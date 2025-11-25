package org.sigma.syntax.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Wrapper that adapts the recursive-descent parser to the older ParseResult-style API.
 */
public class SigmaParserWrapper {

    public ParseResult parse(String sourceCode) {
        RecursiveDescentParser.ParseAstResult r = RecursiveDescentParser.parseToAst(sourceCode);
        if (r.errors != null && !r.errors.isEmpty()) {
            return ParseResult.failure(r.errors);
        }
        return ParseResult.success();
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