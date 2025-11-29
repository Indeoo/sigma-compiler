package org.sigma.lexer;

import java.util.List;

/**
 * Simple standalone test for the custom lexer implementation.
 */
public class LexerTest {
    public static void main(String[] args) {
        SigmaLexerWrapper lexer = new SigmaLexerWrapper();

        // Test 1: Basic expression
        System.out.println("=== Test 1: Basic expression ===");
        testLexer(lexer, "x + 5");

        // Test 2: Keywords
        System.out.println("\n=== Test 2: Keywords ===");
        testLexer(lexer, "int class if else while return final");

        // Test 3: Operators
        System.out.println("\n=== Test 3: Operators ===");
        testLexer(lexer, "+ - * / % < <= > >= == != && || ! =");

        // Test 4: Literals
        System.out.println("\n=== Test 4: Literals ===");
        testLexer(lexer, "42 3.14 \"hello\" true false null");

        // Test 5: Complex expression
        System.out.println("\n=== Test 5: Complex expression ===");
        testLexer(lexer, "int result = (x + 5) * 2;");

        // Test 6: Comments
        System.out.println("\n=== Test 6: Comments (should be skipped) ===");
        testLexer(lexer, "x = 10; // comment\ny = 20; /* block */ z = 30;");

        // Test 7: String with escape sequences
        System.out.println("\n=== Test 7: String with escapes ===");
        testLexer(lexer, "\"Hello\\nWorld\\t!\"");

        // Test 8: Float vs integer with dot
        System.out.println("\n=== Test 8: Number edge cases ===");
        testLexer(lexer, "42 42.5 42.");

        System.out.println("\n=== All tests completed successfully! ===");
    }

    private static void testLexer(SigmaLexerWrapper lexer, String source) {
        System.out.println("Source: " + source);
        try {
            List<SigmaToken> tokens = lexer.tokenize(source);
            System.out.println("Tokens (" + tokens.size() + "):");
            for (SigmaToken token : tokens) {
                System.out.printf("  %-15s %-20s (line:%d col:%d)\n",
                        token.getType().name(),
                        "\"" + token.getText() + "\"",
                        token.getLine(),
                        token.getCharPositionInLine());
            }
        } catch (LexerException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }
}
