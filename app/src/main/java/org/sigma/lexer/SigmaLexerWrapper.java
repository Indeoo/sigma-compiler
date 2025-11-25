package org.sigma.lexer;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Vocabulary;
import org.sigma.parser.SigmaLexer;

/**
 * Small wrapper around the ANTLR-generated SigmaLexer to provide a
 * compatible API for the tests and demo. The wrapper constructs a
 * lexer from a string and returns a populated CommonTokenStream.
 */
public class SigmaLexerWrapper {

    /**
     * Create a CommonTokenStream filled from the provided source string.
     * The stream will contain tokens produced by the generated SigmaLexer.
     */
    public CommonTokenStream createLexerTable(String source) {
        CharStream cs = CharStreams.fromString(source == null ? "" : source);
        SigmaLexer lexer = new SigmaLexer(cs);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill();
        return tokens;
    }

    /**
     * Return the vocabulary exposed by the generated SigmaLexer.
     * Tests call this to resolve token symbolic names for nicer assertions.
     */
    public Vocabulary getVocabulary() {
        // Create a temporary lexer instance to access the vocabulary
        SigmaLexer lexer = new SigmaLexer(CharStreams.fromString(""));
        return lexer.getVocabulary();
    }
}
