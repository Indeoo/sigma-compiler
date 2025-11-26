package org.sigma.syntax.parser;

import org.sigma.ast.Ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Parse result containing the AST and any syntax errors.
 * Used throughout the parsing pipeline.
 */
public class ParseResult {

    private final Ast.CompilationUnit ast;
    private final List<String> errors;
    private final boolean successful;

    public ParseResult(Ast.CompilationUnit ast, List<String> errors) {
        this.ast = ast;
        this.errors = new ArrayList<>(errors == null ? List.of() : errors);
        this.successful = this.errors.isEmpty();
    }

    public static ParseResult failure(List<String> errors) { return new ParseResult(null, errors); }
    public static ParseResult success(Ast.CompilationUnit ast) { return new ParseResult(ast, new ArrayList<>()); }

    public Ast.CompilationUnit getAst() { return ast; }
    public List<String> getErrors() { return new ArrayList<>(errors); }
    public boolean isSuccessful() { return successful; }
    public boolean hasErrors() { return !errors.isEmpty(); }
    public int getErrorCount() { return errors.size(); }

    public String getErrorsAsString() { if (errors.isEmpty()) return "No errors"; return String.join("\n", errors); }

    @Override
    public String toString() {
        return "ParseResult{" +
                "ast=" + ast +
                ", errors=" + errors +
                ", successful=" + successful +
                '}';
    }
}