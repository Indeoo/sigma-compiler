package org.example.error;

import org.antlr.v4.runtime.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom error listener for better error reporting in the Sigma compiler
 */
public class SigmaErrorListener extends BaseErrorListener {

    private List<String> errors = new ArrayList<>();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                           int line, int charPositionInLine, String msg, RecognitionException e) {

        String errorMsg = String.format("Line %d:%d - %s", line, charPositionInLine, msg);

        // Provide more user-friendly error messages
        if (msg.contains("missing")) {
            if (msg.contains("'}'")) {
                errorMsg = String.format("Line %d:%d - Missing closing brace '}'", line, charPositionInLine);
            } else if (msg.contains("';'")) {
                errorMsg = String.format("Line %d:%d - Missing semicolon", line, charPositionInLine);
            }
        } else if (msg.contains("extraneous input")) {
            errorMsg = String.format("Line %d:%d - Unexpected token", line, charPositionInLine);
        } else if (msg.contains("no viable alternative")) {
            errorMsg = String.format("Line %d:%d - Syntax error: invalid expression", line, charPositionInLine);
        }

        errors.add(errorMsg);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    public void clearErrors() {
        errors.clear();
    }
}