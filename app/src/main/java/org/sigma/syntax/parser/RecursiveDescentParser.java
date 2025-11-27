package org.sigma.syntax.parser;

import org.sigma.lexer.SigmaToken;
import org.example.parser.SigmaParser;

import java.util.*;

/**
 * Improved recursive-descent parser for Sigma (syntax-only checks).
 * - Tokenizes input
 * - Parses statements and expressions with correct precedence and associativity
 * - Detects and reports:
 *   * misspelled keywords (fuzzy match)
 *   * incomplete arithmetic (e.g. "2 +")
 *   * extra tokens / unexpected tokens
 *   * missing semicolons
 *   * operator associativity violations (checks shape of AST for chains)
 */
public class RecursiveDescentParser {

    // Parser builds AST nodes defined in org.sigma.syntax.parser.Ast

    /**
     * Parse from a list of SigmaTokens and produce an AST (CompilationUnit) along with syntax errors (if any).
     * This is the primary parsing method that works with the unified lexer.
     * Now uses ANTLR-generated parser with custom lexer tokens via adapter pattern.
     */
    public static ParseResult parseToAst(List<SigmaToken> tokens) {
        try {
            // Convert SigmaTokens to ANTLR tokens using TokenAdapter
            List<org.antlr.v4.runtime.Token> antlrTokens = tokens.stream()
                .map(TokenAdapter::new)
                .collect(java.util.stream.Collectors.toList());

            // Create TokenSource and TokenStream for ANTLR parser
            TokenListSource tokenSource = new TokenListSource(antlrTokens, "Sigma");
            org.antlr.v4.runtime.CommonTokenStream tokenStream =
                new org.antlr.v4.runtime.CommonTokenStream(tokenSource);

            // Create ANTLR parser
            SigmaParser parser = new SigmaParser(tokenStream);

            // Remove default error listeners and add custom one
            parser.removeErrorListeners();
            CustomErrorListener errorListener = new CustomErrorListener();
            parser.addErrorListener(errorListener);

            // Parse to get ANTLR parse tree
            org.antlr.v4.runtime.tree.ParseTree tree = parser.compilationUnit();

            // Check if parsing succeeded
            if (errorListener.hasErrors()) {
                // Parsing had errors - try to convert if tree exists, otherwise return null AST
                Ast.CompilationUnit ast = null;
                if (tree != null) {
                    try {
                        AntlrToAstConverter converter = new AntlrToAstConverter();
                        ast = (Ast.CompilationUnit) converter.visit(tree);
                    } catch (Exception e) {
                        // AST conversion failed - return null AST with errors
                    }
                }
                return new ParseResult(ast, normalizeDiagnostics(errorListener.getErrors()));
            }

            // Convert ANTLR parse tree to custom AST
            AntlrToAstConverter converter = new AntlrToAstConverter();
            Ast.CompilationUnit ast = (Ast.CompilationUnit) converter.visit(tree);

            return new ParseResult(ast, normalizeDiagnostics(errorListener.getErrors()));

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
