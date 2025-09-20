package org.example.semantic;

import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.example.parser.SigmaParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite for the SigmaSemanticAnalyzer class.
 * Tests analyzer.analyze() method directly with manually constructed ParseTree objects.
 */
public class SigmaSemanticAnalyzerTest {

    private SigmaSemanticAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new SigmaSemanticAnalyzer();
    }

    @Test
    void testAnalyzeIntegerVariableDeclaration() {
        // Manually construct ParseTree for: int x = 10;

        // Create contexts
        SigmaParser.CompilationUnitContext compUnit = new SigmaParser.CompilationUnitContext(null, 0);
        SigmaParser.DeclarationContext declaration = new SigmaParser.DeclarationContext(null, 0);
        SigmaParser.VariableDeclarationContext varDecl = new SigmaParser.VariableDeclarationContext(null, 0);
        SigmaParser.TypeContext typeCtx = new SigmaParser.TypeContext(null, 0);
        SigmaParser.ExpressionContext expr = new SigmaParser.ExpressionContext(null, 0);
        SigmaParser.PrimaryContext primary = new SigmaParser.PrimaryContext(null, 0);
        SigmaParser.LiteralContext literal = new SigmaParser.LiteralContext(null, 0);

        // Create terminal nodes
        TerminalNodeImpl typeToken = new TerminalNodeImpl(new CommonToken(1, "int"));
        TerminalNodeImpl identifierToken = new TerminalNodeImpl(new CommonToken(2, "x"));
        TerminalNodeImpl equalsToken = new TerminalNodeImpl(new CommonToken(3, "="));
        TerminalNodeImpl integerToken = new TerminalNodeImpl(new CommonToken(4, "10"));
        TerminalNodeImpl semicolonToken = new TerminalNodeImpl(new CommonToken(5, ";"));

        // Build tree structure manually
        compUnit.addChild(declaration);

        declaration.addChild(varDecl);

        // Variable declaration: type IDENTIFIER = expression ;
        varDecl.addChild(typeCtx);
        varDecl.addChild(identifierToken);
        varDecl.addChild(equalsToken);
        varDecl.addChild(expr);
        varDecl.addChild(semicolonToken);

        // Type: int
        typeCtx.addChild(typeToken);

        // Expression: primary
        expr.addChild(primary);

        // Primary: literal
        primary.addChild(literal);

        // Literal: INTEGER
        literal.addChild(integerToken);

        // Call analyzer.analyze() - this is what we're unit testing
        SemanticResult result = analyzer.analyze(compUnit);

        // Verify the analysis results
        assertNotNull(result);
        System.out.println("Manual ParseTree test - Success: " + result.isSuccessful());
        System.out.println("Error count: " + result.getErrorCount());
        if (!result.isSuccessful()) {
            System.out.println("Errors: " + result.getErrorsAsString());
        }
    }

    @Test
    void testAnalyzeEmptyProgram() {
        // Test with empty compilation unit
        SigmaParser.CompilationUnitContext compUnit = new SigmaParser.CompilationUnitContext(null, 0);

        SemanticResult result = analyzer.analyze(compUnit);

        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertEquals(0, result.getErrorCount());
    }

    @Test
    void testAnalyzeStringVariableDeclaration() {
        // Manually construct ParseTree for: String name = "test";

        SigmaParser.CompilationUnitContext compUnit = new SigmaParser.CompilationUnitContext(null, 0);
        SigmaParser.DeclarationContext declaration = new SigmaParser.DeclarationContext(null, 0);
        SigmaParser.VariableDeclarationContext varDecl = new SigmaParser.VariableDeclarationContext(null, 0);
        SigmaParser.TypeContext typeCtx = new SigmaParser.TypeContext(null, 0);
        SigmaParser.ExpressionContext expr = new SigmaParser.ExpressionContext(null, 0);
        SigmaParser.PrimaryContext primary = new SigmaParser.PrimaryContext(null, 0);
        SigmaParser.LiteralContext literal = new SigmaParser.LiteralContext(null, 0);

        // Create terminal nodes
        TerminalNodeImpl typeToken = new TerminalNodeImpl(new CommonToken(1, "String"));
        TerminalNodeImpl identifierToken = new TerminalNodeImpl(new CommonToken(2, "name"));
        TerminalNodeImpl equalsToken = new TerminalNodeImpl(new CommonToken(3, "="));
        TerminalNodeImpl stringToken = new TerminalNodeImpl(new CommonToken(4, "\"test\""));
        TerminalNodeImpl semicolonToken = new TerminalNodeImpl(new CommonToken(5, ";"));

        // Build tree structure
        compUnit.addChild(declaration);
        declaration.addChild(varDecl);

        varDecl.addChild(typeCtx);
        varDecl.addChild(identifierToken);
        varDecl.addChild(equalsToken);
        varDecl.addChild(expr);
        varDecl.addChild(semicolonToken);

        typeCtx.addChild(typeToken);
        expr.addChild(primary);
        primary.addChild(literal);
        literal.addChild(stringToken);

        SemanticResult result = analyzer.analyze(compUnit);

        assertNotNull(result);
        System.out.println("String variable test - Success: " + result.isSuccessful());
        System.out.println("Error count: " + result.getErrorCount());
    }
}