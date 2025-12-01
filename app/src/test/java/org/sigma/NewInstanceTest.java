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
    private final SigmaParserWrapper parser = new SigmaParserWrapper();

    private Ast.CompilationUnit parseUnit(String code) {
        ParseResult parseResult = parser.parse(code);
        assertTrue(parseResult.isSuccessful(), "Parse should succeed");
        assertNotNull(parseResult.getAst());
        return parseResult.getAst();
    }

    private List<Ast.Statement> scriptStatements(Ast.CompilationUnit ast) {
        for (Ast.Statement stmt : ast.statements) {
            if (stmt instanceof Ast.ClassDeclaration) {
                Ast.ClassDeclaration cls = (Ast.ClassDeclaration) stmt;
                if ("Script".equals(cls.name)) {
                    for (Ast.Statement member : cls.members) {
                        if (member instanceof Ast.MethodDeclaration) {
                            Ast.MethodDeclaration method = (Ast.MethodDeclaration) member;
                            if ("run".equals(method.name)) {
                                return method.body.statements;
                            }
                        }
                    }
                }
            }
        }
        return ast.statements;
    }

    @Test
    void testParseNewInstanceNoArgs() {
        Ast.CompilationUnit cu = parseUnit("new Person()");
        List<Ast.Statement> stmts = scriptStatements(cu);
        assertEquals(1, stmts.size());
        Ast.ExpressionStatement stmt = (Ast.ExpressionStatement) stmts.get(0);
        Ast.NewInstance newInst = (Ast.NewInstance) stmt.expr;

        assertEquals("Person", newInst.className);
        assertEquals(0, newInst.args.size());
    }

    @Test
    void testParseNewInstanceWithArgs() {
        Ast.CompilationUnit cu = parseUnit("new Calculator(5, 10)");
        Ast.ExpressionStatement stmt = (Ast.ExpressionStatement) scriptStatements(cu).get(0);
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

        Ast.CompilationUnit cu = parseUnit(source);
        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        SemanticResult result = analyzer.analyze(cu);

        assertTrue(result.getErrors().isEmpty(),
            "Expected no errors, but got: " + result.getErrors());
    }

    @Test
    void testSemanticUndefinedClass() {
        String source = "Person p = new Person();";

        Ast.CompilationUnit cu = parseUnit(source);
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

        Ast.CompilationUnit cu = parseUnit(source);
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

        Ast.CompilationUnit cu = parseUnit(source);
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

        Ast.CompilationUnit cu = parseUnit(source);
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

        Ast.CompilationUnit cu = parseUnit(source);
        Ast.Statement first = scriptStatements(cu).get(0);
        System.out.println("DEBUG first stmt: " + first.getClass());
        Ast.VariableDeclaration varDecl = (Ast.VariableDeclaration) first;
        Ast.NewInstance outer = (Ast.NewInstance) varDecl.init;

        assertEquals("Outer", outer.className);
        assertEquals(1, outer.args.size());

        Ast.NewInstance inner = (Ast.NewInstance) outer.args.get(0);
        assertEquals("Inner", inner.className);
    }
}
