package org.sigma.syntax.semantic;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of semantic analysis of a Sigma program.
 * Encapsulates the symbol table and any semantic errors found during analysis.
 */
public class SemanticResult {

    private final SymbolTable symbolTable;
    private final List<String> errors;
    private final List<String> warnings;
    private final boolean successful;

    public SemanticResult(SymbolTable symbolTable, List<String> errors, List<String> warnings) {
        this.symbolTable = symbolTable;
        this.errors = new ArrayList<>(errors);
        this.warnings = new ArrayList<>(warnings);
        this.successful = errors.isEmpty();
    }

    public SemanticResult(SymbolTable symbolTable, List<String> errors) {
        this(symbolTable, errors, new ArrayList<>());
    }

    public SemanticResult(SymbolTable symbolTable) {
        this(symbolTable, new ArrayList<>(), new ArrayList<>());
    }

    public static SemanticResult failure(List<String> errors) {
        return new SemanticResult(null, errors);
    }

    public static SemanticResult success(SymbolTable symbolTable) {
        return new SemanticResult(symbolTable);
    }

    public static SemanticResult successWithWarnings(SymbolTable symbolTable, List<String> warnings) {
        return new SemanticResult(symbolTable, new ArrayList<>(), warnings);
    }

    /**
     * @return the symbol table generated during analysis, or null if analysis failed
     */
    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    /**
     * @return list of semantic errors found during analysis
     */
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    /**
     * @return list of warnings found during analysis
     */
    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }

    /**
     * @return true if semantic analysis was successful (no errors)
     */
    public boolean isSuccessful() {
        return successful;
    }

    /**
     * @return true if there were semantic errors during analysis
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * @return true if there were warnings during analysis
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /**
     * @return number of semantic errors found
     */
    public int getErrorCount() {
        return errors.size();
    }

    /**
     * @return number of warnings found
     */
    public int getWarningCount() {
        return warnings.size();
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

    /**
     * @return a formatted string of all warnings
     */
    public String getWarningsAsString() {
        if (warnings.isEmpty()) {
            return "No warnings";
        }
        return String.join("\n", warnings);
    }

    /**
     * @return a formatted string of all errors and warnings
     */
    public String getAllMessagesAsString() {
        List<String> allMessages = new ArrayList<>();

        if (!errors.isEmpty()) {
            allMessages.add("Errors:");
            allMessages.addAll(errors);
        }

        if (!warnings.isEmpty()) {
            if (!allMessages.isEmpty()) {
                allMessages.add("");
            }
            allMessages.add("Warnings:");
            allMessages.addAll(warnings);
        }

        if (allMessages.isEmpty()) {
            return "No errors or warnings";
        }

        return String.join("\n", allMessages);
    }

    @Override
    public String toString() {
        return String.format("SemanticResult{successful=%s, errors=%d, warnings=%d, hasSymbolTable=%s}",
                           successful, errors.size(), warnings.size(), symbolTable != null);
    }
}