package org.sigma.ir;

import org.junit.jupiter.api.Test;
import org.sigma.semantics.SemanticAnalyzer;
import org.sigma.semantics.SemanticResult;
import org.sigma.syntax.parser.Ast;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RPN IR generation from AST.
 */
public class RPNGeneratorTest {

    @Test
    public void testSimpleLiteral() {
        // int x = 42;
        Ast.IntLiteral literal = new Ast.IntLiteral(42, 1, 0);
        Ast.VariableDeclaration varDecl = new Ast.VariableDeclaration("int", "x", literal, 1, 0);
        List<Ast.Statement> stmts = new ArrayList<>();
        stmts.add(varDecl);
        Ast.CompilationUnit ast = new Ast.CompilationUnit(stmts);

        // Analyze and generate
        SemanticResult semanticResult = new SemanticAnalyzer().analyze(ast);
        assertTrue(semanticResult.isSuccessful(), "Semantic analysis should succeed");

        RPNGenerator generator = new RPNGenerator(semanticResult);
        RPNProgram program = generator.generate(ast);

        // Verify instructions
        List<RPNInstruction> instructions = program.getInstructions();
        assertEquals(3, instructions.size(), "Should have 3 instructions (PUSH, STORE, HALT)");

        assertEquals(RPNOpcode.PUSH, instructions.get(0).getOpcode());
        assertEquals(42, instructions.get(0).getOperand());

        assertEquals(RPNOpcode.STORE, instructions.get(1).getOpcode());
        assertEquals("x", instructions.get(1).getOperand());

        assertEquals(RPNOpcode.HALT, instructions.get(2).getOpcode());
    }

    @Test
    public void testBinaryExpression() {
        // int result = 10 + 5;
        Ast.IntLiteral left = new Ast.IntLiteral(10, 1, 0);
        Ast.IntLiteral right = new Ast.IntLiteral(5, 1, 0);
        Ast.Binary binary = new Ast.Binary("+", left, right, 1, 0);
        Ast.VariableDeclaration varDecl = new Ast.VariableDeclaration("int", "result", binary, 1, 0);
        List<Ast.Statement> stmts = new ArrayList<>();
        stmts.add(varDecl);
        Ast.CompilationUnit ast = new Ast.CompilationUnit(stmts);

        // Analyze and generate
        SemanticResult semanticResult = new SemanticAnalyzer().analyze(ast);
        assertTrue(semanticResult.isSuccessful(), "Semantic analysis should succeed");

        RPNGenerator generator = new RPNGenerator(semanticResult);
        RPNProgram program = generator.generate(ast);

        // Verify RPN order: PUSH 10, PUSH 5, ADD, STORE result, HALT
        List<RPNInstruction> instructions = program.getInstructions();
        assertEquals(5, instructions.size());

        assertEquals(RPNOpcode.PUSH, instructions.get(0).getOpcode());
        assertEquals(10, instructions.get(0).getOperand());

        assertEquals(RPNOpcode.PUSH, instructions.get(1).getOpcode());
        assertEquals(5, instructions.get(1).getOperand());

        assertEquals(RPNOpcode.ADD, instructions.get(2).getOpcode());

        assertEquals(RPNOpcode.STORE, instructions.get(3).getOpcode());
        assertEquals("result", instructions.get(3).getOperand());

        assertEquals(RPNOpcode.HALT, instructions.get(4).getOpcode());
    }

    @Test
    public void testComplexExpression() {
        // int result = 10 * 5 + 3;
        // RPN should be: PUSH 10, PUSH 5, MUL, PUSH 3, ADD, STORE result
        Ast.IntLiteral ten = new Ast.IntLiteral(10, 1, 0);
        Ast.IntLiteral five = new Ast.IntLiteral(5, 1, 0);
        Ast.IntLiteral three = new Ast.IntLiteral(3, 1, 0);
        Ast.Binary mul = new Ast.Binary("*", ten, five, 1, 0);
        Ast.Binary add = new Ast.Binary("+", mul, three, 1, 0);
        Ast.VariableDeclaration varDecl = new Ast.VariableDeclaration("int", "result", add, 1, 0);
        List<Ast.Statement> stmts = new ArrayList<>();
        stmts.add(varDecl);
        Ast.CompilationUnit ast = new Ast.CompilationUnit(stmts);

        // Analyze and generate
        SemanticResult semanticResult = new SemanticAnalyzer().analyze(ast);
        assertTrue(semanticResult.isSuccessful(), "Semantic analysis should succeed");

        RPNGenerator generator = new RPNGenerator(semanticResult);
        RPNProgram program = generator.generate(ast);

        // Verify RPN order
        List<RPNInstruction> instructions = program.getInstructions();
        assertEquals(7, instructions.size());

        assertEquals(RPNOpcode.PUSH, instructions.get(0).getOpcode());
        assertEquals(10, instructions.get(0).getOperand());

        assertEquals(RPNOpcode.PUSH, instructions.get(1).getOpcode());
        assertEquals(5, instructions.get(1).getOperand());

        assertEquals(RPNOpcode.MUL, instructions.get(2).getOpcode());

        assertEquals(RPNOpcode.PUSH, instructions.get(3).getOpcode());
        assertEquals(3, instructions.get(3).getOperand());

        assertEquals(RPNOpcode.ADD, instructions.get(4).getOpcode());

        assertEquals(RPNOpcode.STORE, instructions.get(5).getOpcode());
        assertEquals("result", instructions.get(5).getOperand());

        assertEquals(RPNOpcode.HALT, instructions.get(6).getOpcode());
    }

    @Test
    public void testUnaryExpression() {
        // int x = -42;
        Ast.IntLiteral literal = new Ast.IntLiteral(42, 1, 0);
        Ast.Unary unary = new Ast.Unary("neg", literal, 1, 0);
        Ast.VariableDeclaration varDecl = new Ast.VariableDeclaration("int", "x", unary, 1, 0);
        List<Ast.Statement> stmts = new ArrayList<>();
        stmts.add(varDecl);
        Ast.CompilationUnit ast = new Ast.CompilationUnit(stmts);

        // Analyze and generate
        SemanticResult semanticResult = new SemanticAnalyzer().analyze(ast);
        assertTrue(semanticResult.isSuccessful(), "Semantic analysis should succeed");

        RPNGenerator generator = new RPNGenerator(semanticResult);
        RPNProgram program = generator.generate(ast);

        // Verify: PUSH 42, NEG, STORE x, HALT
        List<RPNInstruction> instructions = program.getInstructions();
        assertEquals(4, instructions.size());

        assertEquals(RPNOpcode.PUSH, instructions.get(0).getOpcode());
        assertEquals(42, instructions.get(0).getOperand());

        assertEquals(RPNOpcode.NEG, instructions.get(1).getOpcode());

        assertEquals(RPNOpcode.STORE, instructions.get(2).getOpcode());
        assertEquals("x", instructions.get(2).getOperand());

        assertEquals(RPNOpcode.HALT, instructions.get(3).getOpcode());
    }

    @Test
    public void testVariableReference() {
        // int x = 10;
        // int y = x;
        Ast.IntLiteral literal = new Ast.IntLiteral(10, 1, 0);
        Ast.VariableDeclaration varDeclX = new Ast.VariableDeclaration("int", "x", literal, 1, 0);
        Ast.Identifier xRef = new Ast.Identifier("x", 2, 0);
        Ast.VariableDeclaration varDeclY = new Ast.VariableDeclaration("int", "y", xRef, 2, 0);
        List<Ast.Statement> stmts = new ArrayList<>();
        stmts.add(varDeclX);
        stmts.add(varDeclY);
        Ast.CompilationUnit ast = new Ast.CompilationUnit(stmts);

        // Analyze and generate
        SemanticResult semanticResult = new SemanticAnalyzer().analyze(ast);
        assertTrue(semanticResult.isSuccessful(), "Semantic analysis should succeed");

        RPNGenerator generator = new RPNGenerator(semanticResult);
        RPNProgram program = generator.generate(ast);

        // Verify: PUSH 10, STORE x, LOAD x, STORE y, HALT
        List<RPNInstruction> instructions = program.getInstructions();
        assertEquals(5, instructions.size());

        assertEquals(RPNOpcode.PUSH, instructions.get(0).getOpcode());
        assertEquals(RPNOpcode.STORE, instructions.get(1).getOpcode());
        assertEquals("x", instructions.get(1).getOperand());

        assertEquals(RPNOpcode.LOAD, instructions.get(2).getOpcode());
        assertEquals("x", instructions.get(2).getOperand());

        assertEquals(RPNOpcode.STORE, instructions.get(3).getOpcode());
        assertEquals("y", instructions.get(3).getOperand());

        assertEquals(RPNOpcode.HALT, instructions.get(4).getOpcode());
    }

    @Test
    public void testAssignment() {
        // int x = 10;
        // x = 20;
        Ast.IntLiteral literal = new Ast.IntLiteral(10, 1, 0);
        Ast.VariableDeclaration varDecl = new Ast.VariableDeclaration("int", "x", literal, 1, 0);
        Ast.IntLiteral newValue = new Ast.IntLiteral(20, 2, 0);
        Ast.Assignment assign = new Ast.Assignment("x", newValue);
        List<Ast.Statement> stmts = new ArrayList<>();
        stmts.add(varDecl);
        stmts.add(assign);
        Ast.CompilationUnit ast = new Ast.CompilationUnit(stmts);

        // Analyze and generate
        SemanticResult semanticResult = new SemanticAnalyzer().analyze(ast);
        assertTrue(semanticResult.isSuccessful(), "Semantic analysis should succeed");

        RPNGenerator generator = new RPNGenerator(semanticResult);
        RPNProgram program = generator.generate(ast);

        // Verify: PUSH 10, STORE x, PUSH 20, STORE x, HALT
        List<RPNInstruction> instructions = program.getInstructions();
        assertEquals(5, instructions.size());

        assertEquals(RPNOpcode.PUSH, instructions.get(0).getOpcode());
        assertEquals(10, instructions.get(0).getOperand());

        assertEquals(RPNOpcode.STORE, instructions.get(1).getOpcode());
        assertEquals("x", instructions.get(1).getOperand());

        assertEquals(RPNOpcode.PUSH, instructions.get(2).getOpcode());
        assertEquals(20, instructions.get(2).getOperand());

        assertEquals(RPNOpcode.STORE, instructions.get(3).getOpcode());
        assertEquals("x", instructions.get(3).getOperand());

        assertEquals(RPNOpcode.HALT, instructions.get(4).getOpcode());
    }

    @Test
    public void testProgramVisualization() {
        // int x = 5 + 3;
        Ast.IntLiteral left = new Ast.IntLiteral(5, 1, 0);
        Ast.IntLiteral right = new Ast.IntLiteral(3, 1, 0);
        Ast.Binary binary = new Ast.Binary("+", left, right, 1, 0);
        Ast.VariableDeclaration varDecl = new Ast.VariableDeclaration("int", "x", binary, 1, 0);
        List<Ast.Statement> stmts = new ArrayList<>();
        stmts.add(varDecl);
        Ast.CompilationUnit ast = new Ast.CompilationUnit(stmts);

        // Analyze and generate
        SemanticResult semanticResult = new SemanticAnalyzer().analyze(ast);
        RPNGenerator generator = new RPNGenerator(semanticResult);
        RPNProgram program = generator.generate(ast);

        // Test visualization
        String visualization = program.visualize();
        assertNotNull(visualization);
        assertTrue(visualization.contains("RPN Program:"));
        assertTrue(visualization.contains("PUSH"));
        assertTrue(visualization.contains("ADD"));
        assertTrue(visualization.contains("STORE"));
        assertTrue(visualization.contains("HALT"));
    }
}
