package org.sigma.syntax.parser;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom error listener for ANTLR parser that formats errors in our style.
 * Formats errors as "Line N: message" to match existing error format.
 */
public class CustomErrorListener extends BaseErrorListener {
    private final List<String> errors = new ArrayList<>();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer,
                           Object offendingSymbol,
                           int line,
                           int charPositionInLine,
                           String msg,
                           RecognitionException e) {
        // Format error in our style: "Line N: message"
        errors.add(String.format("Line %d: %s", line, msg));
    }

    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
