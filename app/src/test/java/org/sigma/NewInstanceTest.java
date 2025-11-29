package org.sigma;

import org.junit.jupiter.api.Test;
import org.sigma.ir.RPNGenerator;
import org.sigma.ir.RPNInstruction;
import org.sigma.ir.RPNOpcode;
import org.sigma.ir.RPNProgram;
import org.sigma.parser.Ast;
import org.sigma.parser.ParseResult;
import org.sigma.parser.SigmaParserWrapper;
import org.sigma.semantics.SemanticAnalyzer;
import org.sigma.semantics.SemanticError;
import org.sigma.semantics.SemanticResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the new instance (object creation) feature.
 */
public class NewInstanceTest {

    @Test
    void testParseNewInstanceNoArgs() {
        SigmaParserWrapper parser = new SigmaParserWrapper();
        ParseResult parseResult = parser.parse("new Person()");
        Ast.CompilationUnit cu = parseResult.getAst();

        assertEquals(1, cu.statements.size());
        Ast.ExpressionStatement stmt = (Ast.ExpressionStatement) cu.statements.get(0);
        Ast.NewInstance newInst = (Ast.NewInstance) stmt.expr;

        assertEquals("Person", newInst.className);
        assertEquals(0, newInst.args.size());
    }

    @Test
    void testParseNewInstanceWithArgs() {
        SigmaParserWrapper parser = new SigmaParserWrapper();
        ParseResult parseResult = parser.parse("new Calculator(5, 10)");
        Ast.CompilationUnit cu = parseResult.getAst();

        Ast.ExpressionStatement stmt = (Ast.ExpressionStatement) cu.statements.get(0);
        Ast.NewInstance newInst = (Ast.NewInstance) stmt.expr;

        assertEquals("Calculator", newInst.className);
        assertEquals(2, newInst.args.size());
    }

    @Test
    void testSemanticValidClass() {
        String source = """
            class Person {}
            Person p = new Person();
            """;

        SigmaParserWrapper parser = new SigmaParserWrapper();
        ParseResult parseResult = parser.parse(source);
        Ast.CompilationUnit cu = parseResult.getAst();

        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        SemanticResult result = analyzer.analyze(cu);

        assertTrue(result.getErrors().isEmpty(),
            "Expected no errors, but got: " + result.getErrors());
    }

    @Test
    void testSemanticUndefinedClass() {
        String source = "Person p = new Person();";

        SigmaParserWrapper parser = new SigmaParserWrapper();
        ParseResult parseResult = parser.parse(source);
        Ast.CompilationUnit cu = parseResult.getAst();

        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        SemanticResult result = analyzer.analyze(cu);

        // Should get at least one UNDEFINED_CLASS error
        assertFalse(result.getErrors().isEmpty());
        boolean hasUndefinedClassError = result.getErrors().stream()
            .anyMatch(error -> error.getType() == SemanticError.SemanticErrorType.UNDEFINED_CLASS);
        assertTrue(hasUndefinedClassError, "Expected UNDEFINED_CLASS error");
    }

    @Test
    void testSemanticPrimitiveType() {
        String source = "int x = new int();";

        SigmaParserWrapper parser = new SigmaParserWrapper();
        ParseResult parseResult = parser.parse(source);
        Ast.CompilationUnit cu = parseResult.getAst();

        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        SemanticResult result = analyzer.analyze(cu);

        assertEquals(1, result.getErrors().size());
        assertEquals(SemanticError.SemanticErrorType.INVALID_CONSTRUCTOR_CALL,
            result.getErrors().get(0).getType());
    }

    @Test
    void testRPNGenerationNewInstance() {
        String source = """
            class Person {}
            Person p = new Person();
            """;

        SigmaParserWrapper parser = new SigmaParserWrapper();
        ParseResult parseResult = parser.parse(source);
        Ast.CompilationUnit cu = parseResult.getAst();

        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        SemanticResult semanticResult = analyzer.analyze(cu);
        assertTrue(semanticResult.getErrors().isEmpty());

        RPNGenerator generator = new RPNGenerator(semanticResult);
        RPNProgram program = generator.generate(cu);

        List<RPNInstruction> instrs = program.getInstructions();

        // Find the NEW, DUP, INVOKESPECIAL sequence
        boolean foundNew = false;
        boolean foundDup = false;
        boolean foundInvokespecial = false;

        for (RPNInstruction instr : instrs) {
            if (instr.getOpcode() == RPNOpcode.NEW) {
                foundNew = true;
                assertEquals("Person", instr.getOperand());
            }
            if (instr.getOpcode() == RPNOpcode.DUP) {
                foundDup = true;
            }
            if (instr.getOpcode() == RPNOpcode.INVOKESPECIAL) {
                foundInvokespecial = true;
            }
        }

        assertTrue(foundNew, "Expected to find NEW instruction");
        assertTrue(foundDup, "Expected to find DUP instruction");
        assertTrue(foundInvokespecial, "Expected to find INVOKESPECIAL instruction");
    }

    @Test
    void testNewInstanceInAssignment() {
        String source = """
            class Calculator {}
            Calculator c = new Calculator();
            """;

        SigmaParserWrapper parser = new SigmaParserWrapper();
        ParseResult parseResult = parser.parse(source);
        Ast.CompilationUnit cu = parseResult.getAst();

        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        SemanticResult result = analyzer.analyze(cu);

        assertTrue(result.getErrors().isEmpty(),
            "Expected no errors for valid class instantiation in assignment");
    }

    @Test
    void testNestedConstructorCalls() {
        String source = """
            class Inner {}
            class Outer {}
            Outer o = new Outer(new Inner());
            """;

        SigmaParserWrapper parser = new SigmaParserWrapper();
        ParseResult parseResult = parser.parse(source);
        Ast.CompilationUnit cu = parseResult.getAst();

        // Verify parsing
        Ast.VariableDeclaration varDecl = (Ast.VariableDeclaration) cu.statements.get(2);
        Ast.NewInstance outer = (Ast.NewInstance) varDecl.init;

        assertEquals("Outer", outer.className);
        assertEquals(1, outer.args.size());

        Ast.NewInstance inner = (Ast.NewInstance) outer.args.get(0);
        assertEquals("Inner", inner.className);
    }
}
