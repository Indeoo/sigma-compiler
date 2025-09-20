package org.example.semantic;

import org.antlr.v4.runtime.tree.ParseTree;
import org.example.parser.SigmaParser;
import org.example.parser.ParseResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for the SigmaSemanticAnalyzer class.
 * Tests type checking, symbol resolution, scope validation, and error reporting.
 */
public class SigmaSemanticAnalyzerTest {

    private SigmaParser parser;
    private SigmaSemanticAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        parser = new SigmaParser();
        analyzer = new SigmaSemanticAnalyzer();
    }

    private ParseTree parseCode(String code) {
        ParseResult result = parser.parse(code);
        assertTrue(result.isSuccessful(), "Code should parse successfully: " + result.getErrorsAsString());
        return result.getParseTree();
    }

    @Test
    void testValidVariableDeclarations() {
        String code = """
            int x = 10;
            double y = 3.14;
            String name = "test";
            boolean flag = true;
            """;

        ParseTree tree = parseCode(code);
        SemanticResult result = analyzer.analyze(tree);

        assertTrue(result.isSuccessful());
        assertNotNull(result.getSymbolTable());
        assertEquals(0, result.getErrorCount());

        // Check that variables are in symbol table
        SymbolTable symbolTable = result.getSymbolTable();
        assertNotNull(symbolTable.lookup("x"));
        assertNotNull(symbolTable.lookup("y"));
        assertNotNull(symbolTable.lookup("name"));
        assertNotNull(symbolTable.lookup("flag"));
    }

    @Test
    void testTypeCompatibility() {
        String code = """
            int x = 10;
            double y = x;  // int to double is compatible
            int z = 5;
            """;

        ParseTree tree = parseCode(code);
        SemanticResult result = analyzer.analyze(tree);

        assertTrue(result.isSuccessful());
        assertEquals(0, result.getErrorCount());
    }

    @Test
    void testTypeIncompatibility() {
        String code = """
            String x = 10;  // int to String is incompatible
            """;

        ParseTree tree = parseCode(code);
        SemanticResult result = analyzer.analyze(tree);

        assertFalse(result.isSuccessful());
        assertTrue(result.getErrorCount() > 0);
        assertTrue(result.getErrorsAsString().contains("Cannot assign"));
    }

    @Test
    void testUndefinedVariable() {
        String code = """
            int x = undefinedVar;
            """;

        ParseTree tree = parseCode(code);
        SemanticResult result = analyzer.analyze(tree);

        assertFalse(result.isSuccessful());
        assertTrue(result.getErrorCount() > 0);
        assertTrue(result.getErrorsAsString().contains("Undefined identifier"));
    }

    @Test
    void testRedeclaredVariable() {
        String code = """
            int x = 10;
            int x = 20;  // Redeclaration in same scope
            """;

        ParseTree tree = parseCode(code);
        SemanticResult result = analyzer.analyze(tree);

        assertFalse(result.isSuccessful());
        assertTrue(result.getErrorCount() > 0);
        assertTrue(result.getErrorsAsString().contains("already declared"));
    }

    @Test
    void testValidAssignment() {
        String code = """
            int x = 10;
            x = 20;  // Valid assignment
            """;

        ParseTree tree = parseCode(code);
        SemanticResult result = analyzer.analyze(tree);

        assertTrue(result.isSuccessful());
        assertEquals(0, result.getErrorCount());
    }

    @Test
    void testInvalidAssignment() {
        String code = """
            int x = 10;
            x = "string";  // Invalid type assignment
            """;

        ParseTree tree = parseCode(code);
        SemanticResult result = analyzer.analyze(tree);

        assertFalse(result.isSuccessful());
        assertTrue(result.getErrorCount() > 0);
        assertTrue(result.getErrorsAsString().contains("Cannot assign"));
    }

    @Test
    void testValidMethodDeclaration() {
        String code = """
            int add(int a, int b) {
                return a + b;
            }
            """;

        ParseTree tree = parseCode(code);
        SemanticResult result = analyzer.analyze(tree);

        assertTrue(result.isSuccessful());
        assertEquals(0, result.getErrorCount());

        // Check that method is in symbol table
        Symbol addMethod = result.getSymbolTable().lookup("add");
        assertNotNull(addMethod);
        assertEquals(Symbol.SymbolType.METHOD, addMethod.getSymbolType());
        assertEquals(SigmaType.INT, addMethod.getType());
    }

    @Test
    void testMethodRedeclaration() {
        String code = """
            int add(int a, int b) {
                return a + b;
            }
            double add(double x, double y) {  // Redeclaration (no overloading support)
                return x + y;
            }
            """;

        ParseTree tree = parseCode(code);
        SemanticResult result = analyzer.analyze(tree);

        assertFalse(result.isSuccessful());
        assertTrue(result.getErrorCount() > 0);
        assertTrue(result.getErrorsAsString().contains("already declared"));
    }

    @Test
    void testValidReturnType() {
        String code = """
            int getValue() {
                return 42;
            }
            """;

        ParseTree tree = parseCode(code);
        SemanticResult result = analyzer.analyze(tree);

        assertTrue(result.isSuccessful());
        assertEquals(0, result.getErrorCount());
    }

    @Test
    void testInvalidReturnType() {
        String code = """
            int getValue() {
                return "string";  // Wrong return type
            }
            """;

        ParseTree tree = parseCode(code);
        SemanticResult result = analyzer.analyze(tree);

        assertFalse(result.isSuccessful());
        assertTrue(result.getErrorCount() > 0);
        assertTrue(result.getErrorsAsString().contains("Cannot return"));
    }

    @Test
    void testReturnOutsideMethod() {
        String code = """
            int x = 10;
            return x;  // Return outside method
            """;

        ParseTree tree = parseCode(code);
        SemanticResult result = analyzer.analyze(tree);

        assertFalse(result.isSuccessful());
        assertTrue(result.getErrorCount() > 0);
        assertTrue(result.getErrorsAsString().contains("Return statement outside"));
    }

    @Test
    void testValidBinaryOperations() {
        String code = """
            int a = 5;
            int b = 3;
            int sum = a + b;
            int diff = a - b;
            int product = a * b;
            int quotient = a / b;
            boolean greater = a > b;
            boolean equal = a == b;
            """;

        ParseTree tree = parseCode(code);
        SemanticResult result = analyzer.analyze(tree);

        assertTrue(result.isSuccessful());
        assertEquals(0, result.getErrorCount());
    }

    @Test
    void testInvalidBinaryOperations() {
        String code = """
            String s = "hello";
            int n = 5;
            int invalid = s * n;  // String * int is invalid
            """;

        ParseTree tree = parseCode(code);
        SemanticResult result = analyzer.analyze(tree);

        assertFalse(result.isSuccessful());
        assertTrue(result.getErrorCount() > 0);
        assertTrue(result.getErrorsAsString().contains("Invalid operation"));
    }

    @Test
    void testStringConcatenation() {
        String code = """
            String s1 = "hello";
            String s2 = "world";
            String result = s1 + " " + s2;
            """;

        ParseTree tree = parseCode(code);
        SemanticResult result = analyzer.analyze(tree);

        assertTrue(result.isSuccessful());
        assertEquals(0, result.getErrorCount());
    }

    @Test
    void testValidUnaryOperations() {
        String code = """
            int x = -5;
            boolean flag = !true;
            """;

        ParseTree tree = parseCode(code);
        SemanticResult result = analyzer.analyze(tree);

        assertTrue(result.isSuccessful());
        assertEquals(0, result.getErrorCount());
    }

    @Test
    void testInvalidUnaryOperations() {
        String code = """
            String s = "hello";
            String invalid = -s;  // Unary minus on string
            """;

        ParseTree tree = parseCode(code);
        SemanticResult result = analyzer.analyze(tree);

        assertFalse(result.isSuccessful());
        assertTrue(result.getErrorCount() > 0);
        assertTrue(result.getErrorsAsString().contains("Unary minus can only be applied"));
    }

    @Test
    void testValidConditionalStatements() {
        String code = """
            boolean condition = true;
            if (condition) {
                int x = 10;
            }
            """;

        ParseTree tree = parseCode(code);
        SemanticResult result = analyzer.analyze(tree);

        assertTrue(result.isSuccessful());
        assertEquals(0, result.getErrorCount());
    }

    @Test
    void testInvalidConditionalType() {
        String code = """
            int x = 5;
            if (x) {  // int condition instead of boolean
                println("hello");
            }
            """;

        ParseTree tree = parseCode(code);
        SemanticResult result = analyzer.analyze(tree);

        assertFalse(result.isSuccessful());
        assertTrue(result.getErrorCount() > 0);
        assertTrue(result.getErrorsAsString().contains("condition must be boolean"));
    }

    @Test
    void testScopeResolution() {
        String code = """
            int x = 10;  // Global x
            if (true) {
                int y = 20;  // Local y
                x = 15;      // Access global x
            }
            """;

        ParseTree tree = parseCode(code);
        SemanticResult result = analyzer.analyze(tree);

        assertTrue(result.isSuccessful());
        assertEquals(0, result.getErrorCount());
    }

    @Test
    void testScopeIsolation() {
        String code = """
            if (true) {
                int x = 10;
            }
            x = 20;  // x not accessible outside if block
            """;

        ParseTree tree = parseCode(code);
        SemanticResult result = analyzer.analyze(tree);

        assertFalse(result.isSuccessful());
        assertTrue(result.getErrorCount() > 0);
        assertTrue(result.getErrorsAsString().contains("Undefined"));
    }

    @Test
    void testParameterScope() {
        String code = """
            int add(int a, int b) {
                return a + b;  // Parameters accessible in method
            }
            int x = a;  // Parameter not accessible outside method
            """;

        ParseTree tree = parseCode(code);
        SemanticResult result = analyzer.analyze(tree);

        assertFalse(result.isSuccessful());
        assertTrue(result.getErrorCount() > 0);
        assertTrue(result.getErrorsAsString().contains("Undefined"));
    }

    @Test
    void testBuiltinMethodCalls() {
        String code = """
            println("hello");
            print("world");
            """;

        ParseTree tree = parseCode(code);
        SemanticResult result = analyzer.analyze(tree);

        assertTrue(result.isSuccessful());
        assertEquals(0, result.getErrorCount());
    }

    @Test
    void testClassDeclaration() {
        String code = """
            class Calculator {
                int add(int a, int b) {
                    return a + b;
                }
            }
            """;

        ParseTree tree = parseCode(code);
        SemanticResult result = analyzer.analyze(tree);

        assertTrue(result.isSuccessful());
        assertEquals(0, result.getErrorCount());

        // Check that class is in symbol table
        Symbol calculatorClass = result.getSymbolTable().lookup("Calculator");
        assertNotNull(calculatorClass);
        assertEquals(Symbol.SymbolType.CLASS, calculatorClass.getSymbolType());
    }

    @Test
    void testUninitializedVariableWarning() {
        String code = """
            int x;  // Uninitialized variable should generate warning
            String s;
            """;

        ParseTree tree = parseCode(code);
        SemanticResult result = analyzer.analyze(tree);

        // Should succeed but with warnings
        assertTrue(result.isSuccessful());
        assertTrue(result.hasWarnings());
        assertTrue(result.getWarningCount() > 0);
    }

    @Test
    void testComplexValidProgram() {
        String code = """
            int factorial(int n) {
                if (n <= 1) {
                    return 1;
                } else {
                    return n * factorial(n - 1);
                }
            }

            class Calculator {
                double pi = 3.14159;

                double circleArea(double radius) {
                    return pi * radius * radius;
                }
            }

            int x = 5;
            int fact = factorial(x);
            """;

        ParseTree tree = parseCode(code);
        SemanticResult result = analyzer.analyze(tree);

        assertTrue(result.isSuccessful());
        assertEquals(0, result.getErrorCount());

        // Check that all symbols are properly defined
        SymbolTable symbolTable = result.getSymbolTable();
        assertNotNull(symbolTable.lookup("factorial"));
        assertNotNull(symbolTable.lookup("Calculator"));
        assertNotNull(symbolTable.lookup("x"));
        assertNotNull(symbolTable.lookup("fact"));
    }

    @Test
    void testMultipleErrors() {
        String code = """
            int x = undefinedVar;    // Error 1: undefined variable
            String y = 10;           // Error 2: type mismatch
            int x = 20;              // Error 3: redeclaration
            """;

        ParseTree tree = parseCode(code);
        SemanticResult result = analyzer.analyze(tree);

        assertFalse(result.isSuccessful());
        assertTrue(result.getErrorCount() >= 3);
    }

    @Test
    void testNullHandling() {
        String code = """
            String s = null;
            if (s == null) {
                println("s is null");
            }
            """;

        ParseTree tree = parseCode(code);
        SemanticResult result = analyzer.analyze(tree);

        assertTrue(result.isSuccessful());
        assertEquals(0, result.getErrorCount());
    }
}