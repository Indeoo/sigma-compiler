package org.sigma.parser;

import org.sigma.lexer.SigmaLexerWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for the SigmaParserWrapper class.
 * Tests lexical analysis, syntax analysis, and error reporting.
 */
public class SigmaParserWrapperTest {

    private SigmaParserWrapper parser;
    private SigmaLexerWrapper lexer;

    @BeforeEach
    void setUp() {
        parser = new SigmaParserWrapper();
        lexer = new SigmaLexerWrapper();
    }

    @Test
    void testValidVariableDeclarations() {
        // Test parsing multiple variable declarations
        String code = "int x = 10; double y = 3.14; String name = \"test\"; boolean flag = true;";

        ParseResult result = parser.parse(code);

        assertTrue(result.isSuccessful());
        assertNotNull(result.getAst());
        assertEquals(0, result.getErrorCount());
    }

    @Test
    void testValidExpressions() {
        String code = """
            int a = 5 + 3;
            int b = a * 2;
            boolean c = a > b;
            String d = "hello" + " world";
            """;

        ParseResult result = parser.parse(code);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getAst());
    }

    @Test
    void testValidControlFlow() {
        String code = """
            int x = 10;
            if (x > 5) {
                println("greater");
            } else {
                println("less or equal");
            }

            while (x > 0) {
                x = x - 1;
            }
            """;

        ParseResult result = parser.parse(code);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getAst());
    }

    @Test
    void testValidMethodDeclaration() {
        String code = """
            int add(int a, int b) {
                return a + b;
            }

            void greet(String name) {
                println("Hello " + name);
            }
            """;

        ParseResult result = parser.parse(code);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getAst());
    }

    @Test
    void testValidClassDeclaration() {
        String code = """
            class Calculator {
                int add(int a, int b) {
                    return a + b;
                }
            }
            """;

        ParseResult result = parser.parse(code);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getAst());
    }

    @Test
    void testValidLiterals() {
        String code = """
            int intVal = 42;
            double doubleVal = 3.14159;
            String stringVal = "hello world";
            boolean boolTrue = true;
            boolean boolFalse = false;
            String nullVal = null;
            """;

        ParseResult result = parser.parse(code);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getAst());
    }

    @Test
    void testValidMethodCalls() {
        String code = """
            println("Hello World");
            int result = add(5, 3);
            greet("Alice");
            """;

        ParseResult result = parser.parse(code);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getAst());
    }

    @Test
    void testPrintStatementParsing() {
        String code = "print(\"Hello\");";

        ParseResult result = parser.parse(code);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getAst());
        assertFalse(result.getAst().statements.isEmpty());
        assertTrue(result.getAst().statements.get(0) instanceof Ast.PrintStatement);
    }

    @Test
    void testSyntaxErrorMissingSemicolon() {
        String code = """
            int x = 10
            int y = 20;
            """;

        ParseResult result = parser.parse(code);
        assertFalse(result.isSuccessful());
        assertNull(result.getAst());
        assertTrue(result.hasErrors());
        assertTrue(result.getErrorCount() > 0);
    }

    @Test
    void testSyntaxErrorMissingBrace() {
        String code = """
            if (x > 5) {
                println("greater");
            // Missing closing brace
            """;

        ParseResult result = parser.parse(code);
        assertFalse(result.isSuccessful());
        assertTrue(result.hasErrors());
    }

    @Test
    void testSyntaxErrorInvalidExpression() {
        String code = """
            int x = 5 + + 3;
            """;

        ParseResult result = parser.parse(code);
        assertFalse(result.isSuccessful());
        assertTrue(result.hasErrors());
    }

    @Test
    void testSyntaxErrorInvalidMethodSignature() {
        String code = """
            int (int a, int b) {
                return a + b;
            }
            """;

        ParseResult result = parser.parse(code);
        assertFalse(result.isSuccessful());
        assertTrue(result.hasErrors());
    }

    @Test
    void testEmptyProgram() {
        String code = "";

        ParseResult result = parser.parse(code);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getAst());
    }

    @Test
    void testOnlyWhitespace() {
        String code = "   \n\t  \n  ";

        ParseResult result = parser.parse(code);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getAst());
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

            int main() {
                int num = 5;
                int fact = factorial(num);
                println("Factorial of " + num + " is " + fact);

                double area = circleArea(2.5);
                println("Circle area: " + area);

                return 0;
            }
            """;

        ParseResult result = parser.parse(code);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getAst());
    }

    @Test
    void testParseOrThrowSuccess() {
        String code = "int x = 5;";

        assertDoesNotThrow(() -> {
            ParseResult result = parser.parseOrThrow(code);
            assertNotNull(result);
            assertNotNull(result.getAst());
        });
    }

    @Test
    void testParseOrThrowFailure() {
        String code = "int x = 5 +;";

        assertThrows(RuntimeException.class, () -> {
            parser.parseOrThrow(code);
        });
    }

    @Test
    void testIsValidSyntax() {
        assertTrue(parser.isValidSyntax("int x = 5;"));
        assertFalse(parser.isValidSyntax("int x = 5 +;"));
    }

    @Test
    void testGetSyntaxErrors() {
        String validCode = "int x = 5;";
        String invalidCode = "int x = 5 +;";

        assertTrue(parser.getSyntaxErrors(validCode).isEmpty());
        assertFalse(parser.getSyntaxErrors(invalidCode).isEmpty());
    }

    @Test
    void testBooleanLiterals() {
        String code = """
            boolean t = true;
            boolean f = false;
            """;

        ParseResult result = parser.parse(code);
        assertTrue(result.isSuccessful(), "Boolean literals should parse correctly");
        assertNotNull(result.getAst());
    }

    @Test
    void testNestedExpressions() {
        String code = """
            int result = ((5 + 3) * 2) - 1;
            boolean complex = (x > 5) && (y < 10) || (z == 0);
            """;

        ParseResult result = parser.parse(code);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getAst());
    }

    @Test
    void testUnaryOperators() {
        String code = """
            int x = -5;
            boolean y = !true;
            """;

        ParseResult result = parser.parse(code);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getAst());
    }

    @Test
    void testStringLiterals() {
        String code = """
            String empty = "";
            String simple = "hello";
            String withSpaces = "hello world";
            String withEscapes = "hello\\nworld\\t!";
            """;

        ParseResult result = parser.parse(code);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getAst());
    }

    @Test
    void testMethodCallsWithArguments() {
        String code = """
            println();
            println("hello");
            int sum = add(5, 3);
            calculate(1, 2, 3, 4);
            """;

        ParseResult result = parser.parse(code);
        assertTrue(result.isSuccessful());
        assertNotNull(result.getAst());
    }
}
