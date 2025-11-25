package org.sigma.lexer;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Simple lexer demo that uses the ANTLR-generated SigmaLexer (via SigmaLexerWrapper)
 * to dump tokens for a provided source file. Usage:
 *   java org.example.lexer.LexerDemo [path/to/file]
 * If no path is provided, defaults to the repository root file
 * "syntax_error_tests.sigma".
 */
public class LexerDemo {
    public static void main(String[] args) {
        try {
            Path p = args.length > 0 ? Path.of(args[0]) : Path.of(System.getProperty("user.dir"), "syntax_error_tests.sigma");
            if (!Files.exists(p)) {
                System.err.println("Demo file not found: " + p.toAbsolutePath());
                System.exit(2);
            }
            String src = Files.readString(p);

            SigmaLexerWrapper wrapper = new SigmaLexerWrapper();
            CommonTokenStream tokens = wrapper.createLexerTable(src);
            Vocabulary vocab = wrapper.getVocabulary();

            List<Token> tokenList = tokens.getTokens();
            System.out.println("Token dump for: " + p.toAbsolutePath());
            for (Token t : tokenList) {
                String typeName = t.getType() == Token.EOF ? "<EOF>" : vocab.getSymbolicName(t.getType());
                System.out.printf("%s\t%s\t(line:%d col:%d)\n", typeName, t.getText(), t.getLine(), t.getCharPositionInLine());
            }
        } catch (Exception e) {
            System.err.println("LexerDemo failed: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
