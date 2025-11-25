package org.sigma.syntax.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight parse result used by the RD frontend.
 * Carries a list of syntax errors (if any). The parse tree is not represented here.
 */
public class ParseResult {

    private final List<String> errors;
    private final boolean successful;

    public ParseResult(List<String> errors) {
        this.errors = new ArrayList<>(errors == null ? List.of() : errors);
        this.successful = this.errors.isEmpty();
    }

    public static ParseResult failure(List<String> errors) { return new ParseResult(errors); }
    public static ParseResult success() { return new ParseResult(new ArrayList<>()); }

    public List<String> getErrors() { return new ArrayList<>(errors); }
    public boolean isSuccessful() { return successful; }
    public boolean hasErrors() { return !errors.isEmpty(); }
    public int getErrorCount() { return errors.size(); }

    public String getErrorsAsString() { if (errors.isEmpty()) return "No errors"; return String.join("\n", errors); }

    @Override
    public String toString() { return String.format("ParseResult{successful=%s, errors=%d}", successful, errors.size()); }

}