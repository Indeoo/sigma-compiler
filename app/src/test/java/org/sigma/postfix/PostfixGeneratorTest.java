package org.sigma.postfix;

import org.junit.jupiter.api.Test;
import org.sigma.parser.ParseResult;
import org.sigma.parser.SigmaParserWrapper;
import org.sigma.semantics.SemanticAnalyzer;
import org.sigma.semantics.SemanticResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostfixGeneratorTest {

    @Test
    void generatesFunctionCallModule() {
        String code = """
            int add(int a, int b) {
                return a + b;
            }

            print(add(1, 2));
            """;

        SigmaParserWrapper parser = new SigmaParserWrapper();
        ParseResult parseResult = parser.parse(code);
        assertTrue(parseResult.isSuccessful(), parseResult::getErrorsAsString);
        assertNotNull(parseResult.getAst());

        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        SemanticResult semanticResult = analyzer.analyze(parseResult.getAst());
        assertTrue(semanticResult.isSuccessful(), semanticResult::getErrorsAsString);

        PostfixGenerator generator = new PostfixGenerator();
        PostfixBundle bundle = generator.generate(semanticResult);

        assertEquals(1, bundle.getFunctions().size());
        assertTrue(bundle.getFunctionPrograms().containsKey("add"));

        PostfixProgram mainProgram = bundle.getMainProgram();
        boolean hasCall = mainProgram.getInstructions().stream()
            .anyMatch(instr -> "CALL".equals(instr.tokenType()) && "add".equals(instr.lexeme()));
        assertTrue(hasCall, "Main program should call add()");

        PostfixProgram addProgram = bundle.getFunctionPrograms().get("add");
        boolean hasReturn = addProgram.getInstructions().stream()
            .anyMatch(instr -> "RET".equals(instr.lexeme()));
        assertTrue(hasReturn, "Function module must end with RET");
    }

    @Test
    void insertsConversionsFromSemanticCoercions() {
        String code = """
            double k = 2.5;
            int i = 2;
            double result = k * i;
            int y = 2 ** 3;
            print(result);
            print(y);
            """;

        SigmaParserWrapper parser = new SigmaParserWrapper();
        ParseResult parseResult = parser.parse(code);
        assertTrue(parseResult.isSuccessful(), parseResult::getErrorsAsString);

        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        SemanticResult semanticResult = analyzer.analyze(parseResult.getAst());
        assertTrue(semanticResult.isSuccessful(), semanticResult::getErrorsAsString);

        PostfixBundle bundle = new PostfixGenerator().generate(semanticResult);
        List<PostfixInstruction> instructions = bundle.getMainProgram().getInstructions();

        long i2f = instructions.stream()
            .filter(instr -> "i2f".equals(instr.lexeme()) && "conv".equals(instr.tokenType()))
            .count();
        long f2i = instructions.stream()
            .filter(instr -> "f2i".equals(instr.lexeme()) && "conv".equals(instr.tokenType()))
            .count();
        boolean hasPow = instructions.stream()
            .anyMatch(instr -> "^".equals(instr.lexeme()) && "pow_op".equals(instr.tokenType()));

        assertTrue(hasPow, "Pow operator should be emitted");
        assertTrue(i2f >= 1, () -> "Expected at least one i2f conversion, got " + i2f);
        assertTrue(f2i >= 1, () -> "Expected at least one f2i conversion, got " + f2i);
    }
}
