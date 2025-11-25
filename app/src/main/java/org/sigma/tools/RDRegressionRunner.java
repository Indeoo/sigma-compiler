package org.sigma.tools;

import org.sigma.syntax.parser.RecursiveDescentParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class RDRegressionRunner {
    public static void main(String[] args) throws Exception {
        Path p = Path.of(args.length>0? args[0] : "syntax_error_tests.sigma");
        if (!Files.exists(p)) {
            System.err.println("Test file not found: " + p.toAbsolutePath());
            System.exit(2);
        }
        String src = Files.readString(p);
        List<String> errs = RecursiveDescentParser.parseAndCollectErrors(src);
        System.out.println("Diagnostics from RD parser:");
        for (String e : errs) System.out.println(e);

        boolean okLine6 = errs.stream().anyMatch(s -> s.contains("Line 6:") && s.toLowerCase().contains("did you mean"));
        boolean okLine18 = errs.stream().anyMatch(s -> s.contains("Line 18:") && s.contains("Missing semicolon"));
        boolean okLine31 = errs.stream().anyMatch(s -> s.contains("Line 31:") && s.toLowerCase().contains("unterminated string"));

        if (okLine6 && okLine18 && okLine31) {
            System.out.println("RD regression checks PASSED");
            System.exit(0);
        } else {
            System.err.println("RD regression checks FAILED");
            if (!okLine6) System.err.println("  - missing expected suggestion on line 6");
            if (!okLine18) System.err.println("  - missing missing-semicolon on line 18");
            if (!okLine31) System.err.println("  - missing unterminated string on line 31");
            System.exit(1);
        }
    }
}
