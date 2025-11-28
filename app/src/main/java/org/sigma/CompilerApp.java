package org.sigma;

import org.sigma.ir.RPNGenerator;
import org.sigma.ir.RPNProgram;
import org.sigma.lexer.SigmaLexerWrapper;
import org.sigma.lexer.SigmaToken;
import org.sigma.semantics.SemanticAnalyzer;
import org.sigma.semantics.SemanticResult;
import org.sigma.syntax.parser.ParseResult;
import org.sigma.syntax.parser.SigmaParserWrapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CompilerApp {

    public static void main(String[] args) throws IOException {
        Path p;
        if (args.length > 0) {
            p = Path.of(args[0]);
        } else {
            // Try multiple possible locations for the default source file
            // This handles both Gradle (cwd = app/) and IntelliJ (cwd = project root)
            p = findSourceFile();
        }

        if (!Files.exists(p)) {
            System.err.println("Demo file not found: " + p.toAbsolutePath());
            System.err.println("Current working directory: " + Path.of("").toAbsolutePath());
            System.exit(2);
        }
        String src = Files.readString(p);

        List<SigmaToken> list_token = run_lexer(src);

        ParseResult parseResult = new SigmaParserWrapper().parse(list_token);

        System.out.println(parseResult);
        System.out.println(parseResult.getAstAsString());

        // Run semantic analysis
        System.out.println("\n" + "=".repeat(70));
        System.out.println("SEMANTIC ANALYSIS");
        System.out.println("=".repeat(70));

        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
        SemanticResult semanticResult = semanticAnalyzer.analyze(parseResult.getAst());

        System.out.println(semanticResult.visualize());

        // Generate RPN intermediate representation
        if (semanticResult.isSuccessful()) {
            System.out.println("\n" + "=".repeat(70));
            System.out.println("RPN INTERMEDIATE REPRESENTATION");
            System.out.println("=".repeat(70));

            RPNGenerator rpnGenerator = new RPNGenerator(semanticResult);
            RPNProgram rpnProgram = rpnGenerator.generate(parseResult.getAst());

            System.out.println(rpnProgram.visualize());
        } else {
            System.out.println("\nSkipping RPN generation due to semantic errors.");
        }
    }

    /**
     * Find the source file by trying multiple possible locations.
     * This handles different working directories (Gradle vs IntelliJ).
     */
    private static Path findSourceFile() {
        // Possible locations to try
        String[] possiblePaths = {
            "src/main/resources/source.groovy",           // From app/ directory (Gradle)
            "app/src/main/resources/source.groovy",       // From project root (IntelliJ)
            "./app/src/main/resources/source.groovy"      // Alternative from project root
        };

        for (String pathStr : possiblePaths) {
            Path p = Path.of(pathStr);
            if (Files.exists(p)) {
                return p;
            }
        }

        // Default to first option if none found (will show error with helpful message)
        return Path.of("src/main/resources/source.groovy");
    }

    private static List<SigmaToken> run_lexer(String src) {
        SigmaLexerWrapper wrapper = new SigmaLexerWrapper();
        List<SigmaToken> tokens = wrapper.tokenize(src);

        for (SigmaToken t : tokens) {
            System.out.printf("%s\t%s\t(line:%d col:%d)\n",
                    t.getType().name(), t.getText(), t.getLine(), t.getCharPositionInLine());
        }

        return tokens;
    }
}
