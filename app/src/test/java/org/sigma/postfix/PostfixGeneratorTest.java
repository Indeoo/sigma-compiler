package org.sigma.postfix;

import org.junit.jupiter.api.Test;
import org.sigma.parser.ParseResult;
import org.sigma.parser.SigmaParserWrapper;
import org.sigma.semantics.SemanticAnalyzer;
import org.sigma.semantics.SemanticResult;

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
}
