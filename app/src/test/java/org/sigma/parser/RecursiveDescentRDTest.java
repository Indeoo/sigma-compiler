package org.sigma.parser;

import org.sigma.syntax.parser.ParseResult;
import org.sigma.syntax.parser.RecursiveDescentParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RecursiveDescentRDTest {

    @Test
    void rdParserProducesExpectedDiagnosticsForTestFile() throws IOException {
        Path p = Path.of("syntax_error_tests.sigma");
        assertTrue(Files.exists(p), "test input file must exist at project root: syntax_error_tests.sigma");
        String src = Files.readString(p);
        ParseResult result = RecursiveDescentParser.parseToAst(src);
        List<String> errs = result.getErrors();

        // Basic sanity checks: some important diagnostics should be present
        boolean hasLine6 = errs.stream().anyMatch(s -> s.contains("Line 6:") && s.toLowerCase().contains("did you mean") );
        boolean hasLine18MissingSemi = errs.stream().anyMatch(s -> s.contains("Line 18:") && s.contains("Missing semicolon"));
        boolean hasLine31Unterminated = errs.stream().anyMatch(s -> s.contains("Line 31:") && s.toLowerCase().contains("unterminated string"));

        assertTrue(hasLine6, "Expected suggestion hint on line 6 (typo directive)");
        assertTrue(hasLine18MissingSemi, "Expected missing-semicolon diagnostic on line 18");
        assertTrue(hasLine31Unterminated, "Expected unterminated string diagnostic on line 31");

        // Also assert that at least one diagnostic exists overall
        assertFalse(errs.isEmpty(), "Expected parser to report at least one diagnostic for the test file");
    }
}
