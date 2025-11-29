package org.example.parser;

import org.antlr.v4.runtime.tree.ParseTree;
import java.util.ArrayList;
import java.util.List;

/**
 * Result of parsing a Sigma program.
 * Encapsulates the parse tree and any syntax errors found during parsing.
 */
public class ParseResult {

    private final ParseTree parseTree;
    private final List<String> errors;
    private final boolean successful;

    public ParseResult(ParseTree parseTree, List<String> errors) {
        this.parseTree = parseTree;
        this.errors = new ArrayList<>(errors);
        this.successful = errors.isEmpty();
    }

    public ParseResult(ParseTree parseTree) {
        this(parseTree, new ArrayList<>());
    }

    public static ParseResult failure(List<String> errors) {
        return new ParseResult(null, errors);
    }

    public static ParseResult success(ParseTree parseTree) {
        return new ParseResult(parseTree);
    }

    /**
     * @return the parse tree, or null if parsing failed
     */
    public ParseTree getParseTree() {
        return parseTree;
    }

    /**
     * @return list of syntax errors found during parsing
     */
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    /**
     * @return true if parsing was successful (no syntax errors)
     */
    public boolean isSuccessful() {
        return successful;
    }

    /**
     * @return true if there were syntax errors during parsing
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * @return number of syntax errors found
     */
    public int getErrorCount() {
        return errors.size();
    }

    /**
     * @return a formatted string of all errors
     */
    public String getErrorsAsString() {
        if (errors.isEmpty()) {
            return "No errors";
        }
        return String.join("\n", errors);
    }

    @Override
    public String toString() {
        return String.format("ParseResult{successful=%s, errors=%d, hasParseTree=%s}",
                           successful, errors.size(), parseTree != null);
    }
}