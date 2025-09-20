package org.example;

import org.example.codegen.SigmaCodeGenerator;
import org.example.parser.*;
import org.example.semantic.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Main compiler class for the Sigma programming language.
 * Orchestrates the compilation pipeline: parsing → semantic analysis → code generation.
 */
public class SigmaCompiler {

    private final SigmaParser parser;
    private final SigmaSemanticAnalyzer semanticAnalyzer;

    public SigmaCompiler() {
        this.parser = new SigmaParser();
        this.semanticAnalyzer = new SigmaSemanticAnalyzer();
    }

    /**
     * Compile a Sigma program from source code to JVM bytecode
     *
     * @param sourceCode the Sigma source code to compile
     * @return CompilationResult containing bytecode or error information
     */
    public CompilationResult compile(String sourceCode) {
        return compile(sourceCode, "SigmaProgram");
    }

    /**
     * Compile a Sigma program from source code to JVM bytecode with custom class name
     *
     * @param sourceCode the Sigma source code to compile
     * @param className the name for the generated class
     * @return CompilationResult containing bytecode or error information
     */
    public CompilationResult compile(String sourceCode, String className) {
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

            // Step 3: Code generation
            SigmaCodeGenerator codeGenerator = new SigmaCodeGenerator(
                semanticResult.getSymbolTable(), className);
            byte[] bytecode = codeGenerator.generateBytecode(parseResult.getParseTree());

            if (!codeGenerator.isSuccessful()) {
                return CompilationResult.codeGenerationFailure(parseResult, semanticResult,
                                                             codeGenerator.getErrors());
            }

            // Compilation successful - return bytecode
            return CompilationResult.success(parseResult, semanticResult, bytecode, className);

        } catch (Exception e) {
            List<String> codeGenErrors = List.of("Compilation error: " + e.getMessage());
            return CompilationResult.codeGenerationFailure(null, null, codeGenErrors);
        }
    }


    /**
     * Compile a Sigma program from a file to JVM bytecode
     *
     * @param filename path to the Sigma source file
     * @return CompilationResult containing bytecode or error information
     */
    public CompilationResult compileFile(String filename) {
        try {
            String sourceCode = Files.readString(Paths.get(filename));
            // Use filename (without extension) as class name
            String className = Paths.get(filename).getFileName().toString();
            if (className.contains(".")) {
                className = className.substring(0, className.lastIndexOf("."));
            }
            className = "Sigma" + className; // Prefix to ensure valid class name
            return compile(sourceCode, className);
        } catch (IOException e) {
            List<String> errors = List.of("File error: " + e.getMessage());
            return CompilationResult.codeGenerationFailure(null, null, errors);
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

}