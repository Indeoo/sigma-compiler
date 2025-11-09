package org.example.error;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight stub error listener used when ANTLR is not present.
 * It provides a simple container for error messages and does not
 * depend on ANTLR runtime types.
 */
public class SigmaErrorListener {
    private final List<String> errors = new ArrayList<>();

    public void add(String msg) { errors.add(msg); }

    public boolean hasErrors() { return !errors.isEmpty(); }

    public List<String> getErrors() { return new ArrayList<>(errors); }

    public void clear() { errors.clear(); }
}