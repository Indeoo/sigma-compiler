package org.sigma.syntax.parser;

import org.sigma.lexer.SigmaToken;
import org.sigma.syntax.parser.rd.SigmaRecursiveDescentParser;

import java.util.*;

/**
 * Recursive descent parser for Sigma (syntax-only checks).
 * Pure Java implementation without ANTLR dependencies.
 * - Tokenizes input
 * - Parses statements and expressions with correct precedence and associativity
 * - Detects and reports syntax errors
 * - Grammar rules defined in code for easy modification
 */
public class RecursiveDescentParser {

    // Parser builds AST nodes defined in org.sigma.syntax.parser.Ast

    /**
     * Parse from a list of SigmaTokens and produce an AST (CompilationUnit) along with syntax errors (if any).
     * This is the primary parsing method that works with the unified lexer.
     * Uses pure recursive descent parsing without ANTLR.
     */
    public static ParseResult parseToAst(List<SigmaToken> tokens) {
        try {
            SigmaRecursiveDescentParser parser = new SigmaRecursiveDescentParser();
            SigmaRecursiveDescentParser.ParseResultWithContext result = parser.parseWithContext(tokens);

            List<String> normalizedErrors = normalizeDiagnostics(result.errors);

            // Return null AST if there are errors (for compatibility with tests)
            Ast.CompilationUnit ast = normalizedErrors.isEmpty() ? result.ast : null;

            return new ParseResult(ast, normalizedErrors);

        } catch (Exception e) {
            // Handle any unexpected errors during parsing
            List<String> errors = List.of("Internal parser error: " + e.getMessage());
            return new ParseResult(null, errors);
        }
    }

    // Normalize and deduplicate diagnostics: ensure they all start with "Line N: ", trim whitespace,
    // remove exact duplicates and return sorted by line/message.
    private static List<String> normalizeDiagnostics(List<String> raw) {
        if (raw == null) return Collections.emptyList();
        Map<Integer, LinkedHashSet<String>> byLine = new TreeMap<>();
        for (String r : raw) {
            if (r == null) continue;
            String s = r.trim();
            int line = lineFromMessage(s);
            String msg = s;
            // if message already has a leading "Line N: ", strip it for normalization
            if (s.startsWith("Line ")) {
                int colon = s.indexOf(':');
                if (colon > 0 && colon+1 < s.length()) msg = s.substring(colon+1).trim();
            }
            if (!byLine.containsKey(line)) byLine.put(line, new LinkedHashSet<>());
            byLine.get(line).add(msg);
        }
        List<String> out = new ArrayList<>();
        for (Map.Entry<Integer, LinkedHashSet<String>> e : byLine.entrySet()) {
            int ln = e.getKey();
            for (String m : e.getValue()) {
                out.add(String.format("Line %d: %s", ln==Integer.MAX_VALUE? -1 : ln, m));
            }
        }
        return out;
    }

    private static int lineFromMessage(String msg) {
        if (msg == null) return Integer.MAX_VALUE;
        String prefix = "Line ";
        if (msg.startsWith(prefix)) {
            int colon = msg.indexOf(':');
            if (colon > prefix.length()) {
                String num = msg.substring(prefix.length(), colon).trim();
                try { return Integer.parseInt(num); } catch (NumberFormatException e) { return Integer.MAX_VALUE; }
            }
        }
        return Integer.MAX_VALUE;
    }

}
