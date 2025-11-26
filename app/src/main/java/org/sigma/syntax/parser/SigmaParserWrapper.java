package org.sigma.syntax.parser;

import org.sigma.lexer.SigmaToken;
import org.sigma.lexer.SigmaLexerWrapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Wrapper that adapts the recursive-descent parser to the older ParseResult-style API.
 */
public class SigmaParserWrapper {

    /**
     * Parse from a list of tokens (primary method for unified lexer pipeline).
     */
    public ParseResult parse(List<SigmaToken> tokens) {
        RecursiveDescentParser.ParseAstResult r = RecursiveDescentParser.parseToAst(tokens);
        if (r.errors != null && !r.errors.isEmpty()) {
            return ParseResult.failure(r.errors);
        }
        return ParseResult.success();
    }

    /**
     * Parse from source string (delegates to token-based parsing).
     */
    public ParseResult parse(String sourceCode) {
        SigmaLexerWrapper lexer = new SigmaLexerWrapper();
        List<SigmaToken> tokens = lexer.tokenize(sourceCode);
        return parse(tokens);
    }

    public ParseResult parseFile(String filename) {
        try {
            String src = Files.readString(Paths.get(filename));
            return parse(src);
        } catch (IOException e) {
            return ParseResult.failure(java.util.List.of("File error: " + e.getMessage()));
        }
    }

    public ParseResult parseOrThrow(String sourceCode) {
        ParseResult r = parse(sourceCode);
        if (!r.isSuccessful()) throw new RuntimeException("Parse failed: " + r.getErrorsAsString());
        return r;
    }

    public boolean isValidSyntax(String sourceCode) { return parse(sourceCode).isSuccessful(); }

    public java.util.List<String> getSyntaxErrors(String sourceCode) { return parse(sourceCode).getErrors(); }

}