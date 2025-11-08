package org.example;

import org.example.codegen.SigmaCodeGeneratorRD;
import org.example.lexer.*;
import org.example.parser.RecursiveDescentParser;
import org.example.semantic.RDSemanticAnalyzer;
import org.example.semantic.*;
import org.example.ast.Ast;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Main compiler class for the Sigma programming language.
 * Orchestrates the compilation pipeline: parsing → semantic analysis → code generation.
 */
public class SigmaCompiler {

    private final RDSemanticAnalyzer semanticAnalyzer;

    public SigmaCompiler() {
        this.semanticAnalyzer = new RDSemanticAnalyzer();
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
            // Step 1: Parse to RD AST
            RecursiveDescentParser.ParseAstResult rd = RecursiveDescentParser.parseToAst(sourceCode);
            if (rd.errors != null && !rd.errors.isEmpty()) {
                // wrap tokenization/parse errors into ParseResult and return
                org.example.parser.ParseResult pr = org.example.parser.ParseResult.failure(rd.errors);
                return CompilationResult.parseFailure(pr);
            }

            // Step 2: Semantic analysis (RD)
            SemanticResult semanticResult = semanticAnalyzer.analyze(rd.ast);
            if (!semanticResult.isSuccessful()) {
                return CompilationResult.semanticFailure(null, semanticResult);
            }

            // Step 3: Code generation (RD)
            SigmaCodeGeneratorRD codeGenerator = new SigmaCodeGeneratorRD(semanticResult.getSymbolTable(), className);
            byte[] bytecode = codeGenerator.generateBytecode(rd.ast);
            if (!codeGenerator.isSuccessful()) {
                return CompilationResult.codeGenerationFailure(null, semanticResult, codeGenerator.getErrors());
            }

            // Build successful CompilationResult using null ParseResult (we used RD parser)
            return CompilationResult.success(null, semanticResult, bytecode, className);

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


    // Parser and semantic analyzer accessors removed (ANTLR replaced by RD pipeline)

}