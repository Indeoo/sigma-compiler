package org.example.lexer;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.example.parser.SigmaParser.*;
import static org.junit.jupiter.api.Assertions.*;

class SigmaLexerWrapperTest {

    private SigmaLexerWrapper lexer;
    private Vocabulary vocabulary;

    @BeforeEach
    void setUp() {
        lexer = new SigmaLexerWrapper();
        vocabulary = lexer.getVocabulary();
    }

    /**
     * Helper method to print token information for debugging
     */
    private void printTokenInfo(Token token) {
        System.out.println("Type: " + vocabulary.getSymbolicName(token.getType()));
        System.out.println("Text: " + token.getText());
        System.out.println("Line: " + token.getLine() + " Column:" + token.getCharPositionInLine());
        System.out.println("---");
    }

    /**
     * Helper method to assert token type and text
     */
    private void assertToken(Token token, int expectedType, String expectedText) {
        assertEquals(expectedType, token.getType(),
            "Expected token type: " + vocabulary.getSymbolicName(expectedType) +
            " but got: " + vocabulary.getSymbolicName(token.getType()));
        assertEquals(expectedText, token.getText());
    }

    @Test
    void testBasicExpression() {
        String sourceCode = "x + 5";
        CommonTokenStream tokens = lexer.createLexerTable(sourceCode);
        List<Token> tokenList = tokens.getTokens();

        System.out.println("=== Testing: " + sourceCode + " ===");
        tokenList.forEach(this::printTokenInfo);

        // Verify token sequence
        assertEquals(4, tokenList.size(), "Expected 4 tokens (IDENTIFIER, ADDITIVE, INTEGER, EOF)");

        assertToken(tokenList.get(0), IDENTIFIER, "x");
        assertToken(tokenList.get(1), ADDITIVE, "+");
        assertToken(tokenList.get(2), INTEGER, "5");
        assertToken(tokenList.get(3), Token.EOF, "<EOF>");
    }

    @Test
    void testKeywords() {
        String sourceCode = "int class if else while return final";
        CommonTokenStream tokens = lexer.createLexerTable(sourceCode);
        List<Token> tokenList = tokens.getTokens();

        System.out.println("=== Testing Keywords ===");
        tokenList.forEach(this::printTokenInfo);

        assertToken(tokenList.get(0), PRIMITIVE_TYPE, "int");
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
        CommonTokenStream tokens = lexer.createLexerTable(sourceCode);
        List<Token> tokenList = tokens.getTokens();

        System.out.println("=== Testing Operators ===");
        tokenList.forEach(this::printTokenInfo);

        assertToken(tokenList.get(0), ADDITIVE, "+");
        assertToken(tokenList.get(1), ADDITIVE, "-");
        assertToken(tokenList.get(2), MULTIPLICATIVE, "*");
        assertToken(tokenList.get(3), MULTIPLICATIVE, "/");
        assertToken(tokenList.get(4), MULTIPLICATIVE, "%");
        assertToken(tokenList.get(5), RELATIONAL, "<");
        assertToken(tokenList.get(6), RELATIONAL, "<=");
        assertToken(tokenList.get(7), RELATIONAL, ">");
        assertToken(tokenList.get(8), RELATIONAL, ">=");
        assertToken(tokenList.get(9), RELATIONAL, "==");
        assertToken(tokenList.get(10), RELATIONAL, "!=");
        assertToken(tokenList.get(11), LOGICAL, "&&");
        assertToken(tokenList.get(12), LOGICAL, "||");
        assertToken(tokenList.get(13), NOT, "!");
        assertToken(tokenList.get(14), ASSIGN, "=");
    }

    @Test
    void testLiterals() {
        String sourceCode = "42 3.14 \"hello\" true false null";
        CommonTokenStream tokens = lexer.createLexerTable(sourceCode);
        List<Token> tokenList = tokens.getTokens();

        System.out.println("=== Testing Literals ===");
        tokenList.forEach(this::printTokenInfo);

        assertToken(tokenList.get(0), INTEGER, "42");
        assertToken(tokenList.get(1), FLOAT, "3.14");
        assertToken(tokenList.get(2), STRING, "\"hello\"");
        assertToken(tokenList.get(3), BOOLEAN, "true");
        assertToken(tokenList.get(4), BOOLEAN, "false");
        assertToken(tokenList.get(5), NULL, "null");
    }

    @Test
    void testComplexExpression() {
        String sourceCode = "int result = (x + 5) * 2;";
        CommonTokenStream tokens = lexer.createLexerTable(sourceCode);
        List<Token> tokenList = tokens.getTokens();

        System.out.println("=== Testing Complex Expression ===");
        tokenList.forEach(this::printTokenInfo);

        assertToken(tokenList.get(0), PRIMITIVE_TYPE, "int");
        assertToken(tokenList.get(1), IDENTIFIER, "result");
        assertToken(tokenList.get(2), ASSIGN, "=");
        assertToken(tokenList.get(3), LPAREN, "(");
        assertToken(tokenList.get(4), IDENTIFIER, "x");
        assertToken(tokenList.get(5), ADDITIVE, "+");
        assertToken(tokenList.get(6), INTEGER, "5");
        assertToken(tokenList.get(7), RPAREN, ")");
        assertToken(tokenList.get(8), MULTIPLICATIVE, "*");
        assertToken(tokenList.get(9), INTEGER, "2");
        assertToken(tokenList.get(10), SEMI, ";");
    }

    @Test
    void testDelimiters() {
        String sourceCode = "( ) { } ; , .";
        CommonTokenStream tokens = lexer.createLexerTable(sourceCode);
        List<Token> tokenList = tokens.getTokens();

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
        CommonTokenStream tokens = lexer.createLexerTable(sourceCode);
        List<Token> tokenList = tokens.getTokens();

        System.out.println("=== Testing Variable Declaration Code ===");
        tokenList.forEach(this::printTokenInfo);

        // int count = 42;
        assertToken(tokenList.get(0), PRIMITIVE_TYPE, "int");
        assertToken(tokenList.get(1), IDENTIFIER, "count");
        assertToken(tokenList.get(2), ASSIGN, "=");
        assertToken(tokenList.get(3), INTEGER, "42");
        assertToken(tokenList.get(4), SEMI, ";");

        // double price = 19.99;
        assertToken(tokenList.get(5), PRIMITIVE_TYPE, "double");
        assertToken(tokenList.get(6), IDENTIFIER, "price");
        assertToken(tokenList.get(7), ASSIGN, "=");
        assertToken(tokenList.get(8), FLOAT, "19.99");
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
        CommonTokenStream tokens = lexer.createLexerTable(sourceCode);
        List<Token> tokenList = tokens.getTokens();

        System.out.println("=== Testing Method Declaration Code ===");
        tokenList.forEach(this::printTokenInfo);

        // int add(int a, int b) {
        assertToken(tokenList.get(0), PRIMITIVE_TYPE, "int");
        assertToken(tokenList.get(1), IDENTIFIER, "add");
        assertToken(tokenList.get(2), LPAREN, "(");
        assertToken(tokenList.get(3), PRIMITIVE_TYPE, "int");
        assertToken(tokenList.get(4), IDENTIFIER, "a");
        assertToken(tokenList.get(5), COMMA, ",");
        assertToken(tokenList.get(6), PRIMITIVE_TYPE, "int");
        assertToken(tokenList.get(7), IDENTIFIER, "b");
        assertToken(tokenList.get(8), RPAREN, ")");
        assertToken(tokenList.get(9), LBRACE, "{");

        // return a + b;
        assertToken(tokenList.get(10), RETURN, "return");
        assertToken(tokenList.get(11), IDENTIFIER, "a");
        assertToken(tokenList.get(12), ADDITIVE, "+");
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
        CommonTokenStream tokens = lexer.createLexerTable(sourceCode);
        List<Token> tokenList = tokens.getTokens();

        System.out.println("=== Testing If-Else Statement Code ===");
        tokenList.forEach(this::printTokenInfo);

        // if (x > 10) {
        assertToken(tokenList.get(0), IF, "if");
        assertToken(tokenList.get(1), LPAREN, "(");
        assertToken(tokenList.get(2), IDENTIFIER, "x");
        assertToken(tokenList.get(3), RELATIONAL, ">");
        assertToken(tokenList.get(4), INTEGER, "10");
        assertToken(tokenList.get(5), RPAREN, ")");
        assertToken(tokenList.get(6), LBRACE, "{");

        // y = x * 2;
        assertToken(tokenList.get(7), IDENTIFIER, "y");
        assertToken(tokenList.get(8), ASSIGN, "=");
        assertToken(tokenList.get(9), IDENTIFIER, "x");
        assertToken(tokenList.get(10), MULTIPLICATIVE, "*");
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
        CommonTokenStream tokens = lexer.createLexerTable(sourceCode);
        List<Token> tokenList = tokens.getTokens();

        System.out.println("=== Testing While Loop Code ===");
        tokenList.forEach(this::printTokenInfo);

        // while (count < 100) {
        assertToken(tokenList.get(0), WHILE, "while");
        assertToken(tokenList.get(1), LPAREN, "(");
        assertToken(tokenList.get(2), IDENTIFIER, "count");
        assertToken(tokenList.get(3), RELATIONAL, "<");
        assertToken(tokenList.get(4), INTEGER, "100");
        assertToken(tokenList.get(5), RPAREN, ")");
        assertToken(tokenList.get(6), LBRACE, "{");

        // count = count + 1;
        assertToken(tokenList.get(7), IDENTIFIER, "count");
        assertToken(tokenList.get(8), ASSIGN, "=");
        assertToken(tokenList.get(9), IDENTIFIER, "count");
        assertToken(tokenList.get(10), ADDITIVE, "+");
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
        CommonTokenStream tokens = lexer.createLexerTable(sourceCode);
        List<Token> tokenList = tokens.getTokens();

        System.out.println("=== Testing Class Declaration Code ===");
        tokenList.forEach(this::printTokenInfo);

        // class Calculator {
        assertToken(tokenList.get(0), CLASS, "class");
        assertToken(tokenList.get(1), IDENTIFIER, "Calculator");
        assertToken(tokenList.get(2), LBRACE, "{");

        // int result;
        assertToken(tokenList.get(3), PRIMITIVE_TYPE, "int");
        assertToken(tokenList.get(4), IDENTIFIER, "result");
        assertToken(tokenList.get(5), SEMI, ";");

        // int multiply(int x, int y) {
        assertToken(tokenList.get(6), PRIMITIVE_TYPE, "int");
        assertToken(tokenList.get(7), IDENTIFIER, "multiply");
        assertToken(tokenList.get(8), LPAREN, "(");
        assertToken(tokenList.get(9), PRIMITIVE_TYPE, "int");
        assertToken(tokenList.get(10), IDENTIFIER, "x");
        assertToken(tokenList.get(11), COMMA, ",");
        assertToken(tokenList.get(12), PRIMITIVE_TYPE, "int");
        assertToken(tokenList.get(13), IDENTIFIER, "y");
        assertToken(tokenList.get(14), RPAREN, ")");
        assertToken(tokenList.get(15), LBRACE, "{");

        // return x * y;
        assertToken(tokenList.get(16), RETURN, "return");
        assertToken(tokenList.get(17), IDENTIFIER, "x");
        assertToken(tokenList.get(18), MULTIPLICATIVE, "*");
        assertToken(tokenList.get(19), IDENTIFIER, "y");
        assertToken(tokenList.get(20), SEMI, ";");
    }
}
