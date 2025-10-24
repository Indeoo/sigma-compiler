package org.example.lexer;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.junit.jupiter.api.Test;

class SigmaLexerWrapperTest {

    @Test
    void testLexer() {
        // Create a lexer
        String sourceCode = "x + 5";

        SigmaLexerWrapper sigmaLexerWrapper = new SigmaLexerWrapper();

        CommonTokenStream tokens = sigmaLexerWrapper.createLexerTable(sourceCode);

        for (Token token : tokens.getTokens()) {
            System.out.println("Type: " + sigmaLexerWrapper.getVocabulary().getSymbolicName(token.getType()));
            System.out.println("Text: " + token.getText());
            System.out.println("Line: " + token.getLine());
            System.out.println("Column: " + token.getCharPositionInLine());
            System.out.println("---");
        }
    }
}
