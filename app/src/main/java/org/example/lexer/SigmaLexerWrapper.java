package org.example.lexer;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Vocabulary;
import org.example.error.SigmaErrorListener;

import static org.example.parser.SigmaLexer.VOCABULARY;

public class SigmaLexerWrapper {

    public Vocabulary getVocabulary() {
        return VOCABULARY;
    }

    public CommonTokenStream createLexerTable(CharStream sourceCode) {
        SigmaErrorListener errorListener = new SigmaErrorListener();

        org.example.parser.SigmaLexer lexer = new org.example.parser.SigmaLexer(sourceCode);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill(); // Force lexer to tokenize all input

        return tokens;
    }
}
