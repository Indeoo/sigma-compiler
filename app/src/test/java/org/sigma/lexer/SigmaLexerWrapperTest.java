package org.sigma.lexer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.sigma.lexer.TokenType.*;
import static org.junit.jupiter.api.Assertions.*;

class SigmaLexerWrapperTest {

    private SigmaLexerWrapper lexer;

    @BeforeEach
    void setUp() {
        lexer = new SigmaLexerWrapper();
    }

    /**
     * Helper method to print token information for debugging
     */
    private void printTokenInfo(SigmaToken token) {
        System.out.println("Type: " + token.getType().name());
        System.out.println("Text: " + token.getText());
        System.out.println("Line: " + token.getLine() + " Column:" + token.getCharPositionInLine());
        System.out.println("---");
    }

    /**
     * Helper method to assert token type and text
     */
    private void assertToken(SigmaToken token, TokenType expectedType, String expectedText) {
        assertEquals(expectedType, token.getType(),
            "Expected token type: " + expectedType.name() +
            " but got: " + token.getType().name());
        assertEquals(expectedText, token.getText());
    }

    @Test
    void testBasicExpression() {
        String sourceCode = "x + 5";
        List<SigmaToken> tokenList = lexer.tokenize(sourceCode);

        System.out.println("=== Testing: " + sourceCode + " ===");
        tokenList.forEach(this::printTokenInfo);

        // Verify token sequence
        assertEquals(4, tokenList.size(), "Expected 4 tokens (IDENTIFIER, PLUS, INTEGER, EOF)");

        assertToken(tokenList.get(0), IDENTIFIER, "x");
        assertToken(tokenList.get(1), PLUS, "+");
        assertToken(tokenList.get(2), INTEGER, "5");
        assertToken(tokenList.get(3), EOF, "");
    }

    @Test
    void testKeywords() {
        String sourceCode = "int class if else while return final";
        List<SigmaToken> tokenList = lexer.tokenize(sourceCode);

        System.out.println("=== Testing Keywords ===");
        tokenList.forEach(this::printTokenInfo);

        assertToken(tokenList.get(0), INT, "int");
        assertToken(tokenList.get(1), CLASS, "class");
        assertToken(tokenList.get(2), IF, "if");
        assertToken(tokenList.get(3), ELSE, "else");
        assertToken(tokenList.get(4), WHILE, "while");
        assertToken(tokenList.get(5), RETURN, "return");
        assertToken(tokenList.get(6), FINAL, "final");
    }

    @Test
    void testOperators() {
        String sourceCode = "+ - * / % < <= > >= == != && || ! =";
        List<SigmaToken> tokenList = lexer.tokenize(sourceCode);

        System.out.println("=== Testing Operators ===");
        tokenList.forEach(this::printTokenInfo);

        assertToken(tokenList.get(0), PLUS, "+");
        assertToken(tokenList.get(1), MINUS, "-");
        assertToken(tokenList.get(2), MULT, "*");
        assertToken(tokenList.get(3), DIV, "/");
        assertToken(tokenList.get(4), MOD, "%");
        assertToken(tokenList.get(5), LT, "<");
        assertToken(tokenList.get(6), LE, "<=");
        assertToken(tokenList.get(7), GT, ">");
        assertToken(tokenList.get(8), GE, ">=");
        assertToken(tokenList.get(9), EQ, "==");
        assertToken(tokenList.get(10), NE, "!=");
        assertToken(tokenList.get(11), AND, "&&");
        assertToken(tokenList.get(12), OR, "||");
        assertToken(tokenList.get(13), NOT, "!");
        assertToken(tokenList.get(14), ASSIGN, "=");
    }

    @Test
    void testLiterals() {
        String sourceCode = "42 3.14 \"hello\" true false null";
        List<SigmaToken> tokenList = lexer.tokenize(sourceCode);

        System.out.println("=== Testing Literals ===");
        tokenList.forEach(this::printTokenInfo);

        assertToken(tokenList.get(0), INTEGER, "42");
        assertToken(tokenList.get(1), FLOAT_LITERAL, "3.14");
        assertToken(tokenList.get(2), STRING, "\"hello\"");
        assertToken(tokenList.get(3), TRUE, "true");
        assertToken(tokenList.get(4), FALSE, "false");
        assertToken(tokenList.get(5), NULL, "null");
    }

    @Test
    void testComplexExpression() {
        String sourceCode = "int result = (x + 5) * 2;";
        List<SigmaToken> tokenList = lexer.tokenize(sourceCode);

        System.out.println("=== Testing Complex Expression ===");
        tokenList.forEach(this::printTokenInfo);

        assertToken(tokenList.get(0), INT, "int");
        assertToken(tokenList.get(1), IDENTIFIER, "result");
        assertToken(tokenList.get(2), ASSIGN, "=");
        assertToken(tokenList.get(3), LPAREN, "(");
        assertToken(tokenList.get(4), IDENTIFIER, "x");
        assertToken(tokenList.get(5), PLUS, "+");
        assertToken(tokenList.get(6), INTEGER, "5");
        assertToken(tokenList.get(7), RPAREN, ")");
        assertToken(tokenList.get(8), MULT, "*");
        assertToken(tokenList.get(9), INTEGER, "2");
        assertToken(tokenList.get(10), SEMI, ";");
    }

    @Test
    void testDelimiters() {
        String sourceCode = "( ) { } ; , .";
        List<SigmaToken> tokenList = lexer.tokenize(sourceCode);

        System.out.println("=== Testing Delimiters ===");
        tokenList.forEach(this::printTokenInfo);

        assertToken(tokenList.get(0), LPAREN, "(");
        assertToken(tokenList.get(1), RPAREN, ")");
        assertToken(tokenList.get(2), LBRACE, "{");
        assertToken(tokenList.get(3), RBRACE, "}");
        assertToken(tokenList.get(4), SEMI, ";");
        assertToken(tokenList.get(5), COMMA, ",");
        assertToken(tokenList.get(6), DOT, ".");
    }

    // Real code test cases

    @Test
    void testVariableDeclarationCode() {
        String sourceCode = """
            int count = 42;
            double price = 19.99;
            String message = "Hello World";
            boolean isValid = true;
            """;
        List<SigmaToken> tokenList = lexer.tokenize(sourceCode);

        System.out.println("=== Testing Variable Declaration Code ===");
        tokenList.forEach(this::printTokenInfo);

        // int count = 42;
        assertToken(tokenList.get(0), INT, "int");
        assertToken(tokenList.get(1), IDENTIFIER, "count");
        assertToken(tokenList.get(2), ASSIGN, "=");
        assertToken(tokenList.get(3), INTEGER, "42");
        assertToken(tokenList.get(4), SEMI, ";");

        // double price = 19.99;
        assertToken(tokenList.get(5), DOUBLE, "double");
        assertToken(tokenList.get(6), IDENTIFIER, "price");
        assertToken(tokenList.get(7), ASSIGN, "=");
        assertToken(tokenList.get(8), FLOAT_LITERAL, "19.99");
        assertToken(tokenList.get(9), SEMI, ";");

        // String message = "Hello World";
        assertToken(tokenList.get(10), STRING_TYPE, "String");
        assertToken(tokenList.get(11), IDENTIFIER, "message");
        assertToken(tokenList.get(12), ASSIGN, "=");
        assertToken(tokenList.get(13), STRING, "\"Hello World\"");
        assertToken(tokenList.get(14), SEMI, ";");
    }

    @Test
    void testMethodDeclarationCode() {
        String sourceCode = """
            int add(int a, int b) {
                return a + b;
            }
            """;
        List<SigmaToken> tokenList = lexer.tokenize(sourceCode);

        System.out.println("=== Testing Method Declaration Code ===");
        tokenList.forEach(this::printTokenInfo);

        // int add(int a, int b) {
        assertToken(tokenList.get(0), INT, "int");
        assertToken(tokenList.get(1), IDENTIFIER, "add");
        assertToken(tokenList.get(2), LPAREN, "(");
        assertToken(tokenList.get(3), INT, "int");
        assertToken(tokenList.get(4), IDENTIFIER, "a");
        assertToken(tokenList.get(5), COMMA, ",");
        assertToken(tokenList.get(6), INT, "int");
        assertToken(tokenList.get(7), IDENTIFIER, "b");
        assertToken(tokenList.get(8), RPAREN, ")");
        assertToken(tokenList.get(9), LBRACE, "{");

        // return a + b;
        assertToken(tokenList.get(10), RETURN, "return");
        assertToken(tokenList.get(11), IDENTIFIER, "a");
        assertToken(tokenList.get(12), PLUS, "+");
        assertToken(tokenList.get(13), IDENTIFIER, "b");
        assertToken(tokenList.get(14), SEMI, ";");

        // }
        assertToken(tokenList.get(15), RBRACE, "}");
    }

    @Test
    void testIfElseStatementCode() {
        String sourceCode = """
            if (x > 10) {
                y = x * 2;
            } else {
                y = x / 2;
            }
            """;
        List<SigmaToken> tokenList = lexer.tokenize(sourceCode);

        System.out.println("=== Testing If-Else Statement Code ===");
        tokenList.forEach(this::printTokenInfo);

        // if (x > 10) {
        assertToken(tokenList.get(0), IF, "if");
        assertToken(tokenList.get(1), LPAREN, "(");
        assertToken(tokenList.get(2), IDENTIFIER, "x");
        assertToken(tokenList.get(3), GT, ">");
        assertToken(tokenList.get(4), INTEGER, "10");
        assertToken(tokenList.get(5), RPAREN, ")");
        assertToken(tokenList.get(6), LBRACE, "{");

        // y = x * 2;
        assertToken(tokenList.get(7), IDENTIFIER, "y");
        assertToken(tokenList.get(8), ASSIGN, "=");
        assertToken(tokenList.get(9), IDENTIFIER, "x");
        assertToken(tokenList.get(10), MULT, "*");
        assertToken(tokenList.get(11), INTEGER, "2");
        assertToken(tokenList.get(12), SEMI, ";");

        // } else {
        assertToken(tokenList.get(13), RBRACE, "}");
        assertToken(tokenList.get(14), ELSE, "else");
        assertToken(tokenList.get(15), LBRACE, "{");
    }

    @Test
    void testWhileLoopCode() {
        String sourceCode = """
            while (count < 100) {
                count = count + 1;
                sum = sum + count;
            }
            // COMMENT
            """;
        List<SigmaToken> tokenList = lexer.tokenize(sourceCode);

        System.out.println("=== Testing While Loop Code ===");
        tokenList.forEach(this::printTokenInfo);

        // while (count < 100) {
        assertToken(tokenList.get(0), WHILE, "while");
        assertToken(tokenList.get(1), LPAREN, "(");
        assertToken(tokenList.get(2), IDENTIFIER, "count");
        assertToken(tokenList.get(3), LT, "<");
        assertToken(tokenList.get(4), INTEGER, "100");
        assertToken(tokenList.get(5), RPAREN, ")");
        assertToken(tokenList.get(6), LBRACE, "{");

        // count = count + 1;
        assertToken(tokenList.get(7), IDENTIFIER, "count");
        assertToken(tokenList.get(8), ASSIGN, "=");
        assertToken(tokenList.get(9), IDENTIFIER, "count");
        assertToken(tokenList.get(10), PLUS, "+");
        assertToken(tokenList.get(11), INTEGER, "1");
        assertToken(tokenList.get(12), SEMI, ";");
    }

    @Test
    void testClassDeclarationCode() {
        String sourceCode = """
            class Calculator {
                int result;

                int multiply(int x, int y) {
                    return x * y;
                }
            }
            """;
        List<SigmaToken> tokenList = lexer.tokenize(sourceCode);

        System.out.println("=== Testing Class Declaration Code ===");
        tokenList.forEach(this::printTokenInfo);

        // class Calculator {
        assertToken(tokenList.get(0), CLASS, "class");
        assertToken(tokenList.get(1), IDENTIFIER, "Calculator");
        assertToken(tokenList.get(2), LBRACE, "{");

        // int result;
        assertToken(tokenList.get(3), INT, "int");
        assertToken(tokenList.get(4), IDENTIFIER, "result");
        assertToken(tokenList.get(5), SEMI, ";");

        // int multiply(int x, int y) {
        assertToken(tokenList.get(6), INT, "int");
        assertToken(tokenList.get(7), IDENTIFIER, "multiply");
        assertToken(tokenList.get(8), LPAREN, "(");
        assertToken(tokenList.get(9), INT, "int");
        assertToken(tokenList.get(10), IDENTIFIER, "x");
        assertToken(tokenList.get(11), COMMA, ",");
        assertToken(tokenList.get(12), INT, "int");
        assertToken(tokenList.get(13), IDENTIFIER, "y");
        assertToken(tokenList.get(14), RPAREN, ")");
        assertToken(tokenList.get(15), LBRACE, "{");

        // return x * y;
        assertToken(tokenList.get(16), RETURN, "return");
        assertToken(tokenList.get(17), IDENTIFIER, "x");
        assertToken(tokenList.get(18), MULT, "*");
        assertToken(tokenList.get(19), IDENTIFIER, "y");
        assertToken(tokenList.get(20), SEMI, ";");
    }
}
