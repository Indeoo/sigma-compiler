package org.sigma;

import org.sigma.syntax.parser.ParseResult;
import org.sigma.syntax.semantic.SemanticResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of compiling a Sigma program.
 * Contains the actual compiled JVM bytecode that can be executed by the JVM.
 */
public class CompilationResult {

    public enum Phase {
        PARSING, SEMANTIC_ANALYSIS, CODE_GENERATION
    }

    private final ParseResult parseResult;
    private final SemanticResult semanticResult;
    private final byte[] bytecode;
    private final String className;
    private final Phase failedPhase;
    private final boolean successful;
    private final List<String> compilationErrors;

    public CompilationResult(ParseResult parseResult, SemanticResult semanticResult,
                           byte[] bytecode, String className, List<String> compilationErrors) {
        this.parseResult = parseResult;
        this.semanticResult = semanticResult;
        this.bytecode = bytecode;
        this.className = className;
        this.compilationErrors = new ArrayList<>(compilationErrors);

        // Determine which phase failed and overall success
        if (parseResult != null && !parseResult.isSuccessful()) {
            this.failedPhase = Phase.PARSING;
            this.successful = false;
        } else if (semanticResult != null && !semanticResult.isSuccessful()) {
            this.failedPhase = Phase.SEMANTIC_ANALYSIS;
            this.successful = false;
        } else if (bytecode == null) {
            this.failedPhase = Phase.CODE_GENERATION;
            this.successful = false;
        } else {
            this.failedPhase = null;
            this.successful = true;
        }
    }

    public static CompilationResult parseFailure(ParseResult parseResult) {
        return new CompilationResult(parseResult, null, null, null, new ArrayList<>());
    }

    public static CompilationResult semanticFailure(ParseResult parseResult, SemanticResult semanticResult) {
        return new CompilationResult(parseResult, semanticResult, null, null, new ArrayList<>());
    }

    public static CompilationResult codeGenerationFailure(ParseResult parseResult, SemanticResult semanticResult,
                                                        List<String> codeGenErrors) {
        return new CompilationResult(parseResult, semanticResult, null, null, codeGenErrors);
    }

    public static CompilationResult success(ParseResult parseResult, SemanticResult semanticResult,
                                          byte[] bytecode, String className) {
        return new CompilationResult(parseResult, semanticResult, bytecode, className, new ArrayList<>());
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
     * @return the compiled bytecode, or null if compilation failed
     */
    public byte[] getBytecode() {
        return bytecode != null ? bytecode.clone() : null;
    }

    /**
     * @return the class name of the compiled program
     */
    public String getClassName() {
        return className;
    }

    /**
     * @return list of code generation errors
     */
    public List<String> getCompilationErrors() {
        return new ArrayList<>(compilationErrors);
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

        if (!compilationErrors.isEmpty()) {
            allErrors.addAll(compilationErrors);
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