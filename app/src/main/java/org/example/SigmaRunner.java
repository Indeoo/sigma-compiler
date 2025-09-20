package org.example;

import org.antlr.v4.runtime.tree.ParseTree;
import org.example.interpreter.SigmaInterpreter;
import org.example.semantic.SymbolTable;

/**
 * Runner for executing compiled Sigma programs.
 * Responsible only for program execution, not compilation.
 */
public class SigmaRunner {

    /**
     * Execute a successfully compiled Sigma program
     *
     * @param result the compilation result containing parse tree and symbol table
     * @throws IllegalArgumentException if the compilation result is not successful
     * @throws RuntimeException if execution fails
     */
    public void run(CompilationResult result) {
        if (!result.isSuccessful()) {
            throw new IllegalArgumentException("Cannot run failed compilation. Compilation errors: " +
                                             result.getAllMessagesAsString());
        }

        // Display warnings if any
        if (result.hasWarnings()) {
            System.err.println("Compilation warnings:");
            result.getAllWarnings().forEach(warning -> System.err.println("Warning: " + warning));
        }

        try {
            runProgram(result.getParseResult().getParseTree(),
                      result.getSemanticResult().getSymbolTable());
        } catch (Exception e) {
            throw new RuntimeException("Execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Execute a Sigma program given a parse tree and symbol table
     *
     * @param parseTree the parse tree of the program
     * @param symbolTable the symbol table from semantic analysis
     * @throws RuntimeException if execution fails
     */
    public void runProgram(ParseTree parseTree, SymbolTable symbolTable) {
        if (parseTree == null) {
            throw new IllegalArgumentException("Parse tree cannot be null");
        }
        if (symbolTable == null) {
            throw new IllegalArgumentException("Symbol table cannot be null");
        }

        try {
            SigmaInterpreter interpreter = new SigmaInterpreter(symbolTable);
            interpreter.visit(parseTree);
        } catch (Exception e) {
            throw new RuntimeException("Execution error: " + e.getMessage(), e);
        }
    }

    /**
     * Execute a Sigma program from compilation result, handling errors gracefully
     *
     * @param result the compilation result
     * @return true if execution was successful, false if there were errors
     */
    public boolean runSafely(CompilationResult result) {
        try {
            run(result);
            return true;
        } catch (Exception e) {
            System.err.println("Execution failed: " + e.getMessage());
            return false;
        }
    }
}