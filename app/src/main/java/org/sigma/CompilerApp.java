package org.sigma;

import org.sigma.lexer.SigmaLexerWrapper;
import org.sigma.lexer.SigmaToken;
import org.sigma.syntax.parser.ParseResult;
import org.sigma.syntax.parser.SigmaParserWrapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CompilerApp {

    public static void main(String[] args) throws IOException {
        Path p = args.length > 0 ? Path.of(args[0]) : Path.of("app/src/main/resources", "source.groovy");
        if (!Files.exists(p)) {
            System.err.println("Demo file not found: " + p.toAbsolutePath());
            System.exit(2);
        }
        String src = Files.readString(p);

        List<SigmaToken> list_token = run_lexer(src);

        ParseResult parseResult = new SigmaParserWrapper().parse(list_token);

        System.out.println(parseResult);
        System.out.println(parseResult.getAstAsString());
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
