package org.example.lexer;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.example.parser.SigmaLexer;
import org.junit.jupiter.api.Test;

class LexerTest {

    @Test
    void testLexer() {
        // Create a lexer
        CharStream input = CharStreams.fromString("x + 5");
        SigmaLexer lexer = new SigmaLexer(input);

// Get all tokens
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill(); // Force lexer to tokenize all input

// Iterate through tokens
        for (Token token : tokens.getTokens()) {
            System.out.println("Type: " + lexer.getVocabulary().getSymbolicName(token.getType()));
            System.out.println("Text: " + token.getText());
            System.out.println("Line: " + token.getLine());
            System.out.println("Column: " + token.getCharPositionInLine());
            System.out.println("---");
        }
    }
}
