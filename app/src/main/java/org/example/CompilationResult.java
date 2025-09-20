package org.example;

import org.example.parser.ParseResult;
import org.example.semantic.SemanticResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of compiling a Sigma program.
 * Encapsulates the results from all compilation phases: parsing, semantic analysis, and execution.
 */
public class CompilationResult {

    public enum Phase {
        PARSING, SEMANTIC_ANALYSIS, EXECUTION
    }

    private final ParseResult parseResult;
    private final SemanticResult semanticResult;
    private final boolean executionSuccessful;
    private final List<String> executionErrors;
    private final Phase failedPhase;
    private final boolean successful;

    public CompilationResult(ParseResult parseResult, SemanticResult semanticResult,
                           boolean executionSuccessful, List<String> executionErrors) {
        this.parseResult = parseResult;
        this.semanticResult = semanticResult;
        this.executionSuccessful = executionSuccessful;
        this.executionErrors = new ArrayList<>(executionErrors);

        // Determine which phase failed and overall success
        if (!parseResult.isSuccessful()) {
            this.failedPhase = Phase.PARSING;
            this.successful = false;
        } else if (!semanticResult.isSuccessful()) {
            this.failedPhase = Phase.SEMANTIC_ANALYSIS;
            this.successful = false;
        } else if (!executionSuccessful) {
            this.failedPhase = Phase.EXECUTION;
            this.successful = false;
        } else {
            this.failedPhase = null;
            this.successful = true;
        }
    }

    public static CompilationResult parseFailure(ParseResult parseResult) {
        return new CompilationResult(parseResult, null, false, new ArrayList<>());
    }

    public static CompilationResult semanticFailure(ParseResult parseResult, SemanticResult semanticResult) {
        return new CompilationResult(parseResult, semanticResult, false, new ArrayList<>());
    }

    public static CompilationResult executionFailure(ParseResult parseResult, SemanticResult semanticResult,
                                                   List<String> executionErrors) {
        return new CompilationResult(parseResult, semanticResult, false, executionErrors);
    }

    public static CompilationResult success(ParseResult parseResult, SemanticResult semanticResult) {
        return new CompilationResult(parseResult, semanticResult, true, new ArrayList<>());
    }

    /**
     * @return the parse result, or null if parsing was not attempted
     */
    public ParseResult getParseResult() {
        return parseResult;
    }

    /**
     * @return the semantic analysis result, or null if semantic analysis was not attempted
     */
    public SemanticResult getSemanticResult() {
        return semanticResult;
    }

    /**
     * @return true if execution was successful
     */
    public boolean isExecutionSuccessful() {
        return executionSuccessful;
    }

    /**
     * @return list of execution errors
     */
    public List<String> getExecutionErrors() {
        return new ArrayList<>(executionErrors);
    }

    /**
     * @return the phase where compilation failed, or null if successful
     */
    public Phase getFailedPhase() {
        return failedPhase;
    }

    /**
     * @return true if the entire compilation was successful
     */
    public boolean isSuccessful() {
        return successful;
    }

    /**
     * @return true if there were any errors in any phase
     */
    public boolean hasErrors() {
        return !successful;
    }

    /**
     * @return true if there were any warnings in any phase
     */
    public boolean hasWarnings() {
        return semanticResult != null && semanticResult.hasWarnings();
    }

    /**
     * @return all errors from all phases
     */
    public List<String> getAllErrors() {
        List<String> allErrors = new ArrayList<>();

        if (parseResult != null && !parseResult.isSuccessful()) {
            allErrors.addAll(parseResult.getErrors());
        }

        if (semanticResult != null && !semanticResult.isSuccessful()) {
            allErrors.addAll(semanticResult.getErrors());
        }

        if (!executionSuccessful) {
            allErrors.addAll(executionErrors);
        }

        return allErrors;
    }

    /**
     * @return all warnings from all phases
     */
    public List<String> getAllWarnings() {
        List<String> allWarnings = new ArrayList<>();

        if (semanticResult != null && semanticResult.hasWarnings()) {
            allWarnings.addAll(semanticResult.getWarnings());
        }

        return allWarnings;
    }

    /**
     * @return a formatted string of all errors and warnings
     */
    public String getAllMessagesAsString() {
        List<String> allMessages = new ArrayList<>();

        List<String> errors = getAllErrors();
        List<String> warnings = getAllWarnings();

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
            return "Compilation successful with no errors or warnings";
        }

        return String.join("\n", allMessages);
    }

    /**
     * @return a summary of the compilation result
     */
    public String getSummary() {
        if (successful) {
            if (hasWarnings()) {
                return String.format("Compilation successful with %d warning(s)", getAllWarnings().size());
            } else {
                return "Compilation successful";
            }
        } else {
            return String.format("Compilation failed in %s phase with %d error(s)",
                               failedPhase.name().toLowerCase(), getAllErrors().size());
        }
    }

    @Override
    public String toString() {
        return String.format("CompilationResult{successful=%s, failedPhase=%s, totalErrors=%d, totalWarnings=%d}",
                           successful, failedPhase, getAllErrors().size(), getAllWarnings().size());
    }
}