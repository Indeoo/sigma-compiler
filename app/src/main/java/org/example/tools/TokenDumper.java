package org.example.tools;

import org.example.parser.RecursiveDescentParser;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class TokenDumper {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: TokenDumper <file>");
            System.exit(2);
        }
        String src = new String(Files.readAllBytes(Paths.get(args[0])));
        List<String> toks = RecursiveDescentParser.dumpTokens(src);
        System.out.println("Tokens (count=" + toks.size() + "):");
        for (String t : toks) System.out.println(t);
    }
}
