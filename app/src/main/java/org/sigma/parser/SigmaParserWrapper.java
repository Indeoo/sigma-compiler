package org.sigma.parser;

import org.sigma.lexer.SigmaToken;
import org.sigma.lexer.SigmaLexerWrapper;
import org.sigma.transform.ScriptWrappingTransformer;

import java.util.List;

/**
 * Wrapper that adapts the recursive-descent parser to the older ParseResult-style API.
 */
public class SigmaParserWrapper {

    /**
     * Parse from a list of tokens (primary method for unified lexer pipeline).
     */
    public ParseResult parse(List<SigmaToken> tokens) {
        ParseResult baseResult = RecursiveDescentParser.parseToAst(tokens);
        if (!baseResult.isSuccessful() || baseResult.getAst() == null) {
            return baseResult;
        }

        Ast.CompilationUnit wrapped = ScriptWrappingTransformer.wrap(baseResult.getAst());
        return ParseResult.success(wrapped);
        // # NO TRANSFORMATION
        //return ParseResult.success(baseResult.getAst());
    }

    /**
     * Parse from source string (delegates to token-based parsing).
     */
    public ParseResult parse(String sourceCode) {
        SigmaLexerWrapper lexer = new SigmaLexerWrapper();
        List<SigmaToken> tokens = lexer.tokenize(sourceCode);
        return parse(tokens);
    }

    public ParseResult parseOrThrow(String sourceCode) {
        ParseResult r = parse(sourceCode);
        if (!r.isSuccessful()) throw new RuntimeException("Parse failed: " + r.getErrorsAsString());
        return r;
    }

    public boolean isValidSyntax(String sourceCode) { return parse(sourceCode).isSuccessful(); }

    public java.util.List<String> getSyntaxErrors(String sourceCode) { return parse(sourceCode).getErrors(); }

}
