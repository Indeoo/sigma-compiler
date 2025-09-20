package org.example;

import org.example.parser.*;
import org.example.runner.SigmaRunner;
import org.example.semantic.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Main compiler class for the Sigma programming language.
 * Orchestrates the compilation pipeline: parsing → semantic analysis → interpretation.
 */
public class SigmaCompiler {

    private final SigmaParser parser;
    private final SigmaSemanticAnalyzer semanticAnalyzer;

    public SigmaCompiler() {
        this.parser = new SigmaParser();
        this.semanticAnalyzer = new SigmaSemanticAnalyzer();
    }

    /**
     * Compile a Sigma program from source code and return the result
     *
     * @param sourceCode the Sigma source code to compile
     * @return CompilationResult containing all compilation phase results
     */
    public CompilationResult compile(String sourceCode) {
        try {
            // Step 1: Parsing (lexical + syntax analysis)
            ParseResult parseResult = parser.parse(sourceCode);
            if (!parseResult.isSuccessful()) {
                return CompilationResult.parseFailure(parseResult);
            }

            // Step 2: Semantic analysis
            SemanticResult semanticResult = semanticAnalyzer.analyze(parseResult.getParseTree());
            if (!semanticResult.isSuccessful()) {
                return CompilationResult.semanticFailure(parseResult, semanticResult);
            }

            // Compilation successful - no execution yet
            return CompilationResult.success(parseResult, semanticResult);

        } catch (Exception e) {
            List<String> executionErrors = List.of("Compilation error: " + e.getMessage());
            return CompilationResult.executionFailure(null, null, executionErrors);
        }
    }


    /**
     * Compile a Sigma program from a file and return the result
     *
     * @param filename path to the Sigma source file
     * @return CompilationResult containing all compilation phase results
     */
    public CompilationResult compileFile(String filename) {
        try {
            String sourceCode = Files.readString(Paths.get(filename));
            return compile(sourceCode);
        } catch (IOException e) {
            List<String> errors = List.of("File error: " + e.getMessage());
            return CompilationResult.executionFailure(null, null, errors);
        }
    }


    /**
     * Get the parser instance (for testing)
     */
    public SigmaParser getParser() {
        return parser;
    }

    /**
     * Get the semantic analyzer instance (for testing)
     */
    public SigmaSemanticAnalyzer getSemanticAnalyzer() {
        return semanticAnalyzer;
    }

    public static void main(String[] args) {
        SigmaCompiler compiler = new SigmaCompiler();
        SigmaRunner runner = new SigmaRunner();

        if (args.length == 0) {
            // Interactive mode - simple example
            System.out.println("Sigma Compiler - Interactive Mode");
            String sampleCode = """
                int x = 10;
                println("Hello, Sigma!");
                """;

            // Step 1: Compile
            CompilationResult result = compiler.compile(sampleCode);

            // Step 2: Handle compilation result
            if (!result.isSuccessful()) {
                System.err.println("Compilation failed:");
                System.err.println(result.getAllMessagesAsString());
                return;
            }

            // Step 3: Execute (separate responsibility)
            try {
                runner.run(result);
            } catch (Exception e) {
                System.err.println("Execution failed: " + e.getMessage());
            }

        } else {
            // File mode
            for (String filename : args) {
                System.out.println("Compiling: " + filename);

                // Step 1: Compile file
                CompilationResult result = compiler.compileFile(filename);

                // Step 2: Handle compilation result
                if (!result.isSuccessful()) {
                    System.err.println("Compilation of " + filename + " failed:");
                    System.err.println(result.getAllMessagesAsString());
                    continue; // Try next file
                }

                // Step 3: Execute (separate responsibility)
                System.out.println("Executing: " + filename);
                try {
                    runner.run(result);
                } catch (Exception e) {
                    System.err.println("Execution of " + filename + " failed: " + e.getMessage());
                }
            }
        }
    }
}