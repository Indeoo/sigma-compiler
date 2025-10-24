package org.example.parser;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.example.error.SigmaErrorListener;
import org.example.lexer.SigmaLexerWrapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Parser for the Sigma programming language.
 * Handles lexical analysis (tokenization) and syntax analysis (parse tree generation).
 */
public class SigmaParserWrapper {

    private final SigmaLexerWrapper lexer = new SigmaLexerWrapper();

    /**
     * Parse Sigma source code string and return a ParseResult
     *
     * @param sourceCode the Sigma source code to parse
     * @return ParseResult containing parse tree and any syntax errors
     */
    public ParseResult parse(String sourceCode) {
        CharStream input = CharStreams.fromString(sourceCode);
        CommonTokenStream tokens = lexer.createLexerTable(input);
        return parse(tokens);
    }

    /**
     * Parse from token stream and return a ParseResult
     *
     * @param tokens the token stream to parse
     * @return ParseResult containing parse tree and any syntax errors
     */
    public ParseResult parse(CommonTokenStream tokens) {
        try {
             SigmaErrorListener errorListener = new SigmaErrorListener();

            // Use the ANTLR-generated parser
            org.example.parser.SigmaParser antlrParser = new org.example.parser.SigmaParser(tokens);
            antlrParser.removeErrorListeners();
            antlrParser.addErrorListener(errorListener);

            // Parse starting from compilation unit
            ParseTree tree = antlrParser.compilationUnit();

            if (errorListener.hasErrors()) {
                return ParseResult.failure(errorListener.getErrors());
            }

            return ParseResult.success(tree);

        } catch (Exception e) {
            return ParseResult.failure(java.util.List.of("Parse error: " + e.getMessage()));
        }
    }

    /**
     * Parse a Sigma source file and return a ParseResult
     *
     * @param filename path to the Sigma source file
     * @return ParseResult containing parse tree and any syntax errors
     */
    public ParseResult parseFile(String filename) {
        try {
            String sourceCode = Files.readString(Paths.get(filename));
            return parse(sourceCode);
        } catch (IOException e) {
            return ParseResult.failure(java.util.List.of("File error: " + e.getMessage()));
        }
    }

    /**
     * Parse source code and return just the parse tree (throws on errors)
     * Useful for testing when you expect parsing to succeed
     *
     * @param sourceCode the Sigma source code to parse
     * @return ParseTree if successful
     * @throws RuntimeException if parsing fails
     */
    public ParseTree parseOrThrow(String sourceCode) {
        ParseResult result = parse(sourceCode);
        if (!result.isSuccessful()) {
            throw new RuntimeException("Parse failed: " + result.getErrorsAsString());
        }
        return result.getParseTree();
    }

    /**
     * Validate syntax of source code without returning parse tree
     *
     * @param sourceCode the Sigma source code to validate
     * @return true if syntax is valid, false otherwise
     */
    public boolean isValidSyntax(String sourceCode) {
        return parse(sourceCode).isSuccessful();
    }

    /**
     * Get syntax errors for source code without building full parse tree
     *
     * @param sourceCode the Sigma source code to check
     * @return list of syntax error messages
     */
    public java.util.List<String> getSyntaxErrors(String sourceCode) {
        return parse(sourceCode).getErrors();
    }
}