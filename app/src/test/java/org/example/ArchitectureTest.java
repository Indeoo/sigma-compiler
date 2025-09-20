package org.example;

import org.example.parser.ParseResult;
import org.example.parser.SigmaParser;
import org.example.semantic.SemanticResult;
import org.example.semantic.SigmaSemanticAnalyzer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify the new separated architecture is working correctly.
 */
public class ArchitectureTest {

    @Test
    void testParserSeparation() {
        SigmaParser parser = new SigmaParser();

        // Test valid code
        ParseResult validResult = parser.parse("int x = 5;");
        assertTrue(validResult.isSuccessful());
        assertNotNull(validResult.getParseTree());
        assertEquals(0, validResult.getErrorCount());

        // Test invalid code
        ParseResult invalidResult = parser.parse("int x = 5 +;");
        assertFalse(invalidResult.isSuccessful());
        assertTrue(invalidResult.getErrorCount() > 0);
    }

    @Test
    void testSemanticAnalyzerSeparation() {
        SigmaParser parser = new SigmaParser();
        SigmaSemanticAnalyzer analyzer = new SigmaSemanticAnalyzer();

        // Parse valid code
        ParseResult parseResult = parser.parse("int x = 10; int y = x + 5;");
        assertTrue(parseResult.isSuccessful());

        // Analyze semantics
        SemanticResult semanticResult = analyzer.analyze(parseResult.getParseTree());
        assertTrue(semanticResult.isSuccessful());
        assertNotNull(semanticResult.getSymbolTable());
        assertEquals(0, semanticResult.getErrorCount());

        // Check symbol table has our variables
        assertNotNull(semanticResult.getSymbolTable().lookup("x"));
        assertNotNull(semanticResult.getSymbolTable().lookup("y"));
    }

    @Test
    void testSemanticAnalyzerErrorDetection() {
        SigmaParser parser = new SigmaParser();
        SigmaSemanticAnalyzer analyzer = new SigmaSemanticAnalyzer();

        // Parse code with semantic error
        ParseResult parseResult = parser.parse("int x = undefinedVariable;");
        assertTrue(parseResult.isSuccessful()); // Should parse fine

        // Analyze semantics - should find error
        SemanticResult semanticResult = analyzer.analyze(parseResult.getParseTree());
        assertFalse(semanticResult.isSuccessful());
        assertTrue(semanticResult.getErrorCount() > 0);
        assertTrue(semanticResult.getErrorsAsString().contains("Undefined"));
    }

    @Test
    void testCompilerIntegration() {
        SigmaCompiler compiler = new SigmaCompiler();

        // Test successful compilation
        CompilationResult successResult = compiler.compile("int x = 5; println(\"x = \" + x);");
        assertTrue(successResult.isSuccessful());
        assertNotNull(successResult.getParseResult());
        assertNotNull(successResult.getSemanticResult());
        assertTrue(successResult.getParseResult().isSuccessful());
        assertTrue(successResult.getSemanticResult().isSuccessful());

        // Test parse failure
        CompilationResult parseFailure = compiler.compile("int x = 5 +;");
        assertFalse(parseFailure.isSuccessful());
        assertEquals(CompilationResult.Phase.PARSING, parseFailure.getFailedPhase());

        // Test semantic failure
        CompilationResult semanticFailure = compiler.compile("int x = undefinedVar;");
        assertFalse(semanticFailure.isSuccessful());
        assertEquals(CompilationResult.Phase.SEMANTIC_ANALYSIS, semanticFailure.getFailedPhase());
    }

    @Test
    void testCompilerComponentAccess() {
        SigmaCompiler compiler = new SigmaCompiler();

        // Verify we can access individual components
        assertNotNull(compiler.getParser());
        assertNotNull(compiler.getSemanticAnalyzer());

        // Test components work independently
        ParseResult parseResult = compiler.getParser().parse("int x = 5;");
        assertTrue(parseResult.isSuccessful());

        SemanticResult semanticResult = compiler.getSemanticAnalyzer().analyze(parseResult.getParseTree());
        assertTrue(semanticResult.isSuccessful());
    }
}