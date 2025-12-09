package org.sigma;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.sigma.antlr.SigmaAstBuilder;
import org.sigma.antlr.SigmaLexer;
import org.sigma.antlr.SigmaParser;
import org.sigma.parser.Ast;
import org.sigma.parser.ParseResult;
import org.sigma.semantics.SemanticAnalyzer;
import org.sigma.semantics.SemanticResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.sigma.CompilerApp.emitJvm;

/**
 * Minimal ANTLR-driven interpreter for the Sigma grammar.
 * Supports variable declarations, assignments, arithmetic/logical expressions,
 * if/else blocks, while loops, and built-in print/println calls.
 */
public final class ANTLRCompilerApp {
    private static final Path DEFAULT_SOURCE = Path.of("app/src/main/resources/source_antlr.groovy");

    public static void main(String[] args) throws IOException {
        Path source = args.length > 0 ? Path.of(args[0]) : DEFAULT_SOURCE;
        if (!Files.exists(source)) {
            System.err.println("Source file not found: " + source.toAbsolutePath());
            System.exit(2);
        }

        CharStream input = CharStreams.fromPath(source);
        SigmaLexer lexer = new SigmaLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SigmaParser parser = new SigmaParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new ThrowingErrorListener(source));

        SigmaParser.CompilationUnitContext tree = parser.compilationUnit();

        SigmaAstBuilder builder = new SigmaAstBuilder();
        Ast.CompilationUnit ast = builder.build(tree);

        ParseResult parseResult = ParseResult.success(ast);
        System.out.println("AST-TREE:");
        System.out.println(parseResult.getAstAsString());

        System.out.println("\n" + "=".repeat(70));
        System.out.println("SEMANTIC ANALYSIS");
        System.out.println("=".repeat(70));
        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        SemanticResult semanticResult = analyzer.analyze(ast);
        System.out.println(semanticResult.visualize());

        if (!semanticResult.isSuccessful()) {
            System.exit(1);
        }

        // Generate Postfix IR for PSM.py
        if (semanticResult.isSuccessful()) {
            System.out.println("\n" + "=".repeat(70));
            System.out.println("JVM BYTECODE OUTPUT");
            System.out.println("=".repeat(70));
            emitJvm(semanticResult);
        } else {
            System.out.println("\nSkipping postfix generation due to semantic errors.");
        }
    }

    private ANTLRCompilerApp() {
    }

    private static final class ThrowingErrorListener extends BaseErrorListener {
        private final Path source;

        private ThrowingErrorListener(Path source) {
            this.source = source;
        }

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer,
                                Object offendingSymbol,
                                int line,
                                int charPositionInLine,
                                String msg,
                                RecognitionException e) {
            String sourceName = source != null ? source.toAbsolutePath().toString() : "<input>";
            throw new ParseCancellationException(
                "Syntax error in " + sourceName + " at line " + line + ":" + charPositionInLine + " - " + msg);
        }
    }
}
