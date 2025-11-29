package org.example.lexer;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;

import java.io.IOException;
import java.util.List;

public class LexerDemo {

    public static void main(String[] args) throws IOException {
        SigmaLexerWrapper lexer = new SigmaLexerWrapper();
        String sourceCode = new String(lexer.getClass().getResourceAsStream("/source.scala").readAllBytes());
        CommonTokenStream tokens = lexer.createLexerTable(sourceCode);
        Vocabulary vocabulary = lexer.getVocabulary();

        List<Token> tokenList = tokens.getTokens();

        System.out.println("=== Testing Variable Declaration Code ===");
        tokenList.forEach(
                token -> {
                    System.out.println("Type: " + vocabulary.getSymbolicName(token.getType()));
                    System.out.println("Text: " + token.getText());
                    System.out.println("Line: " + token.getLine() + " Column:" + token.getCharPositionInLine());
                    System.out.println("---");
                }
        );
    }
}
