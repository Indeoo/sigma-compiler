package org.example;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.example.parser.*;
import org.example.semantic.*;
import org.example.interpreter.*;
import org.example.error.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Main compiler class for the Sigma programming language.
 * Integrates ANTLR parser, semantic analysis, and interpreter.
 */
public class SigmaCompiler {

    private boolean hasErrors = false;
    private SigmaErrorListener errorListener;

    public SigmaCompiler() {
        this.errorListener = new SigmaErrorListener();
    }

    /**
     * Compile and run a Sigma program from source code
     */
    public void compileAndRun(String sourceCode) {
        try {
            // Step 1: Lexical analysis
            CharStream input = CharStreams.fromString(sourceCode);
            BasicGroovyLexer lexer = new BasicGroovyLexer(input);
            lexer.removeErrorListeners();
            lexer.addErrorListener(errorListener);

            // Step 2: Syntax analysis
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            BasicGroovyParser parser = new BasicGroovyParser(tokens);
            parser.removeErrorListeners();
            parser.addErrorListener(errorListener);

            ParseTree tree = parser.compilationUnit();

            if (errorListener.hasErrors()) {
                System.err.println("Compilation failed with errors:");
                errorListener.getErrors().forEach(System.err::println);
                return;
            }

            // Step 3: Semantic analysis
            SymbolTable symbolTable = new SymbolTable();
            SemanticAnalyzer analyzer = new SemanticAnalyzer(symbolTable);
            analyzer.visit(tree);

            if (analyzer.hasErrors()) {
                System.err.println("Semantic analysis failed with errors:");
                analyzer.getErrors().forEach(System.err::println);
                return;
            }

            // Step 4: Interpretation/Execution
            SigmaInterpreter interpreter = new SigmaInterpreter(symbolTable);
            interpreter.visit(tree);

        } catch (Exception e) {
            System.err.println("Compilation error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Compile and run a Sigma program from a file
     */
    public void compileAndRunFile(String filename) {
        try {
            String sourceCode = new String(Files.readAllBytes(Paths.get(filename)));
            compileAndRun(sourceCode);
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SigmaCompiler compiler = new SigmaCompiler();

        if (args.length == 0) {
            // Interactive mode - simple example
            System.out.println("Sigma Compiler - Interactive Mode");
            String sampleCode = """
                int x = 10;
                println("Hello, Sigma!");
                """;

            compiler.compileAndRun(sampleCode);
        } else {
            // File mode
            for (String filename : args) {
                System.out.println("Compiling: " + filename);
                compiler.compileAndRunFile(filename);
            }
        }
    }
}