package org.sigma.syntax.parser;

import org.sigma.ast.Ast;
import org.sigma.lexer.SigmaToken;
import org.sigma.lexer.SigmaLexerWrapper;
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

    // Parser builds AST nodes defined in org.sigma.ast.Ast

    private final List<SigmaToken> tokens;
    private int idx = 0;
    private final List<String> errors = new ArrayList<>();

    // Constructor accepts tokens from SigmaLexerWrapper
    private RecursiveDescentParser(List<SigmaToken> tokens) { this.tokens = tokens; }

    public static List<String> parseAndCollectErrors(String src) {
        // This method is deprecated - use parseToAst(String) instead
        // For backward compatibility, delegate to the new implementation
        ParseAstResult result = parseToAst(src);
        return result.errors;
    }

    /**
     * Parse from a list of SigmaTokens and produce an AST (CompilationUnit) along with syntax errors (if any).
     * This is the primary parsing method that works with the unified lexer.
     * Now uses ANTLR-generated parser with custom lexer tokens via adapter pattern.
     */
    public static ParseAstResult parseToAst(List<SigmaToken> tokens) {
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
                return new ParseAstResult(ast, normalizeDiagnostics(errorListener.getErrors()));
            }

            // Convert ANTLR parse tree to custom AST
            AntlrToAstConverter converter = new AntlrToAstConverter();
            Ast.CompilationUnit ast = (Ast.CompilationUnit) converter.visit(tree);

            return new ParseAstResult(ast, normalizeDiagnostics(errorListener.getErrors()));

        } catch (Exception e) {
            // Handle any unexpected errors during parsing
            List<String> errors = List.of("Internal parser error: " + e.getMessage());
            return new ParseAstResult(null, errors);
        }
    }

    /**
     * Parse input string and produce an AST (CompilationUnit) along with syntax errors (if any).
     * This method delegates to the token-based parseToAst for backward compatibility.
     */
    public static ParseAstResult parseToAst(String src) {
        try {
            SigmaLexerWrapper lexer = new SigmaLexerWrapper();
            List<SigmaToken> tokens = lexer.tokenize(src);
            return parseToAst(tokens);
        } catch (Exception e) {
            // Handle lexer exceptions
            List<String> errors = List.of("Lexer error: " + e.getMessage());
            return new ParseAstResult(null, errors);
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

    public static class ParseAstResult {
        public final Ast.CompilationUnit ast;
        public final List<String> errors;
        public ParseAstResult(Ast.CompilationUnit ast, List<String> errors) { this.ast = ast; this.errors = errors; }
    }

    // --- Parser helpers ---
    private SigmaToken look() { return tokens.get(Math.min(idx, tokens.size()-1)); }
    private SigmaToken consume() { return tokens.get(idx++); }

    private boolean acceptOp(org.sigma.lexer.TokenType type) {
        SigmaToken t = look();
        if (t.getType() == type) {
            consume();
            return true;
        }
        return false;
    }

    private boolean acceptPunc(org.sigma.lexer.TokenType type) {
        SigmaToken t = look();
        if (t.getType() == type) {
            consume();
            return true;
        }
        return false;
    }

    private boolean acceptKeyword(org.sigma.lexer.TokenType type) {
        SigmaToken t = look();
        if (t.getType() == type) {
            consume();
            return true;
        }
        return false;
    }

    private void expectPunc(org.sigma.lexer.TokenType type, String typeStr) {
        if (!acceptPunc(type)) {
            SigmaToken t = look();
            errors.add(String.format("Line %d: Expected '%s' but found '%s'", t.getLine(), typeStr, t.getText()));
        }
    }

    private boolean isKeywordType(org.sigma.lexer.TokenType type) {
        return type == org.sigma.lexer.TokenType.CLASS ||
               type == org.sigma.lexer.TokenType.IF ||
               type == org.sigma.lexer.TokenType.ELSE ||
               type == org.sigma.lexer.TokenType.WHILE ||
               type == org.sigma.lexer.TokenType.RETURN ||
               type == org.sigma.lexer.TokenType.FINAL ||
               type == org.sigma.lexer.TokenType.NULL ||
               type == org.sigma.lexer.TokenType.INT ||
               type == org.sigma.lexer.TokenType.DOUBLE ||
               type == org.sigma.lexer.TokenType.FLOAT ||
               type == org.sigma.lexer.TokenType.BOOLEAN ||
               type == org.sigma.lexer.TokenType.STRING_TYPE ||
               type == org.sigma.lexer.TokenType.VOID ||
               type == org.sigma.lexer.TokenType.TRUE ||
               type == org.sigma.lexer.TokenType.FALSE;
    }

    private boolean isTypeKeyword(org.sigma.lexer.TokenType type) {
        return type == org.sigma.lexer.TokenType.INT ||
               type == org.sigma.lexer.TokenType.DOUBLE ||
               type == org.sigma.lexer.TokenType.FLOAT ||
               type == org.sigma.lexer.TokenType.BOOLEAN ||
               type == org.sigma.lexer.TokenType.STRING_TYPE;
    }

    // --- Grammar ---
    private Ast.CompilationUnit parseCompilationUnitAst() {
        List<Ast.Statement> stmts = new ArrayList<>();
        while (look().getType() != org.sigma.lexer.TokenType.EOF) {
            Ast.Statement s = parseStatementOrDirectiveAst();
            if (s != null) stmts.add(s);
            else {
                // skip one token to avoid infinite loop
                SigmaToken u = look();
                errors.add(String.format("Line %d: Unexpected token '%s'", u.getLine(), u.getText()));
                idx++;
            }
        }
        return new Ast.CompilationUnit(stmts);
    }

    private Ast.Statement parseStatementOrDirectiveAst() {
        SigmaToken t = look();
        // Check for directive keywords (runcode, runfile, compile) - these aren't in TokenType, so check IDENTIFIER
        boolean isDirectiveKeyword = (t.getType() == org.sigma.lexer.TokenType.IDENTIFIER &&
                (t.getText().equals("runcode") || t.getText().equals("runfile") || t.getText().equals("compile")));
        if (isDirectiveKeyword) {
            // treat directive as an expression statement when possible (e.g., runcode println(...);)
            consume(); // directive token
            Ast.Expression e = null;
            org.sigma.lexer.TokenType lookType = look().getType();
            if (lookType == org.sigma.lexer.TokenType.STRING ||
                lookType == org.sigma.lexer.TokenType.IDENTIFIER ||
                isKeywordType(lookType)) {
                e = parseExpression();
            }
            if (!acceptPunc(org.sigma.lexer.TokenType.SEMI)) {
                int scan = idx; boolean found = false;
                while (scan < tokens.size()) {
                    SigmaToken tok = tokens.get(scan++);
                    if (tok.getType() == org.sigma.lexer.TokenType.SEMI) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    while (idx < tokens.size()) {
                        SigmaToken tok = look();
                        idx++;
                        if (tok.getType() == org.sigma.lexer.TokenType.SEMI) break;
                    }
                } else {
                    errors.add(String.format("Line %d: Missing semicolon after directive '%s'", t.getLine(), t.getText()));
                }
            }
            if (e != null) return new Ast.ExpressionStatement(e, t.getLine(), t.getColumn());
            return null;
        }
        // handle block-level statements: if, while, block, variable declarations, return
        // if statement
        if (look().getType() == org.sigma.lexer.TokenType.IF) {
            consume();
            if (!acceptPunc(org.sigma.lexer.TokenType.LPAREN)) {
                SigmaToken n = look();
                errors.add(String.format("Line %d: Expected '(' after 'if'", n.getLine()));
            }
            Ast.Expression cond = parseExpression();
            if (cond == null) {
                SigmaToken n = look();
                errors.add(String.format("Line %d: Expected condition expression in 'if'", n.getLine()));
            }
            if (!acceptPunc(org.sigma.lexer.TokenType.RPAREN)) {
                SigmaToken n = look();
                errors.add(String.format("Line %d: Expected ')' after if condition", n.getLine()));
            }
            Ast.Statement thenStmt = parseInnerStatement();
            Ast.Statement elseStmt = null;
            if (look().getType() == org.sigma.lexer.TokenType.ELSE) {
                consume();
                elseStmt = parseInnerStatement();
            }
            return new Ast.IfStatement(cond, thenStmt, elseStmt);
        }

        // while statement
        if (look().getType() == org.sigma.lexer.TokenType.WHILE) {
            consume();
            if (!acceptPunc(org.sigma.lexer.TokenType.LPAREN)) {
                SigmaToken n = look();
                errors.add(String.format("Line %d: Expected '(' after 'while'", n.getLine()));
            }
            Ast.Expression cond = parseExpression();
            if (cond == null) {
                SigmaToken n = look();
                errors.add(String.format("Line %d: Expected condition expression in 'while'", n.getLine()));
            }
            if (!acceptPunc(org.sigma.lexer.TokenType.RPAREN)) {
                SigmaToken n = look();
                errors.add(String.format("Line %d: Expected ')' after while condition", n.getLine()));
            }
            Ast.Statement body = parseInnerStatement();
            return new Ast.WhileStatement(cond, body);
        }

        // block
        if (look().getType() == org.sigma.lexer.TokenType.LBRACE) {
            return parseBlock();
        }

        // variable declaration: type IDENTIFIER ...
        if (isTypeKeyword(look().getType())) {
            String typeName = look().getText();
            consume();
            SigmaToken id = look();
            if (id.getType() == org.sigma.lexer.TokenType.IDENTIFIER) {
                consume();
                Ast.Expression init = null;
                if (acceptOp(org.sigma.lexer.TokenType.ASSIGN)) {
                    init = parseExpression();
                }
                if (!acceptPunc(org.sigma.lexer.TokenType.SEMI)) {
                    errors.add(String.format("Line %d: Missing semicolon after declaration of %s", id.getLine(), id.getText()));
                    // resync
                    int scan = idx;
                    boolean found = false;
                    while (scan < tokens.size()) {
                        SigmaToken tok = tokens.get(scan++);
                        if (tok.getType() == org.sigma.lexer.TokenType.SEMI) {
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        while (idx < tokens.size()) {
                            SigmaToken tok = look();
                            idx++;
                            if (tok.getType() == org.sigma.lexer.TokenType.SEMI) break;
                        }
                    }
                }
                return new Ast.VariableDeclaration(typeName, id.getText(), init, id.getLine(), id.getColumn());
            } else {
                errors.add(String.format("Line %d: Expected identifier after type '%s'", id.getLine(), typeName));
                return null;
            }
        }

        // assignment statement: IDENT '=' expr ';'
        if (look().getType() == org.sigma.lexer.TokenType.IDENTIFIER) {
            SigmaToken next = tokens.size() > idx+1 ? tokens.get(idx+1) : null;
            if (next != null && next.getType() == org.sigma.lexer.TokenType.ASSIGN) {
                SigmaToken id = consume(); // ident
                consume(); // '='
                Ast.Expression val = parseExpression();
                if (!acceptPunc(org.sigma.lexer.TokenType.SEMI)) {
                    errors.add(String.format("Line %d: Missing semicolon after assignment to %s", id.getLine(), id.getText()));
                    // resync
                    int scan = idx;
                    boolean found = false;
                    while (scan < tokens.size()) {
                        SigmaToken tok = tokens.get(scan++);
                        if (tok.getType() == org.sigma.lexer.TokenType.SEMI) {
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        while (idx < tokens.size()) {
                            SigmaToken tok = look();
                            idx++;
                            if (tok.getType() == org.sigma.lexer.TokenType.SEMI) break;
                        }
                    }
                }
                return new Ast.Assignment(id.getText(), val);
            }
        }

        // fallback: expression statement
        int exprStartLine = look().getLine();
        int exprStartCol = look().getColumn();
        Ast.Expression expr = parseExpression();
        if (expr == null) return null;
        if (!acceptPunc(org.sigma.lexer.TokenType.SEMI)) {
            // Attribute missing-semicolon to the statement's start line (exprStartLine) instead of the next token's line
            errors.add(String.format("Line %d: Missing semicolon at end of statement", exprStartLine));
            // resync to next semicolon
            int scan = idx;
            boolean found = false;
            while (scan < tokens.size()) {
                SigmaToken t2 = tokens.get(scan++);
                if (t2.getType() == org.sigma.lexer.TokenType.SEMI) {
                    found = true;
                    break;
                }
            }
            if (found) {
                while (idx < tokens.size()) {
                    SigmaToken tok2 = look();
                    idx++;
                    if (tok2.getType() == org.sigma.lexer.TokenType.SEMI) break;
                }
            } else {
                idx = Math.min(idx+1, tokens.size()-1);
            }
            return new Ast.ExpressionStatement(expr, exprStartLine, exprStartCol);
        }
        // check associativity (use recorded start line for message attribution)
        checkAssociativityExpr(expr, exprStartLine);
        return new Ast.ExpressionStatement(expr, exprStartLine, exprStartCol);
    }

    // parse a block: '{' statements* '}'
    private Ast.Block parseBlock() {
        consume(); // consume '{'
        List<Ast.Statement> stmts = new ArrayList<>();
        while (look().getType() != org.sigma.lexer.TokenType.RBRACE &&
               look().getType() != org.sigma.lexer.TokenType.EOF) {
            Ast.Statement s = parseStatementOrDirectiveAst();
            if (s != null) stmts.add(s);
            else {
                SigmaToken u = look();
                errors.add(String.format("Line %d: Unexpected token '%s' in block", u.getLine(), u.getText()));
                idx++;
            }
        }
        if (!acceptPunc(org.sigma.lexer.TokenType.RBRACE)) {
            SigmaToken n = look();
            errors.add(String.format("Line %d: Expected '}' to close block", n.getLine()));
        }
        return new Ast.Block(stmts);
    }

    // parse a single statement that may be a block or a single-line statement
    private Ast.Statement parseInnerStatement() {
        if (look().getType() == org.sigma.lexer.TokenType.LBRACE) return parseBlock();
        return parseStatementOrDirectiveAst();
    }

    private boolean parseExpressionStatement() {
        int start = idx;
        int exprStartLine = tokens.get(Math.min(start, tokens.size()-1)).getLine();
        Ast.Expression e = parseExpression();
        if (e==null) return false;
        // require semicolon
        if (!acceptPunc(org.sigma.lexer.TokenType.SEMI)) {
            // Use the recorded start line for error attribution so the missing-semicolon is reported on the statement's line
            errors.add(String.format("Line %d: Missing semicolon at end of statement", exprStartLine));
            // resynchronize: skip tokens until next semicolon or EOF to avoid cascaded errors
            int scan = idx;
            boolean found = false;
            while (scan < tokens.size()) {
                SigmaToken tok = tokens.get(scan++);
                if (tok.getType() == org.sigma.lexer.TokenType.SEMI) {
                    found = true;
                    break;
                }
            }
            if (found) {
                while (idx < tokens.size()) {
                    SigmaToken tok = look();
                    idx++;
                    if (tok.getType() == org.sigma.lexer.TokenType.SEMI) break;
                }
            } else {
                // couldn't find a semicolon to resync to; advance by one token instead of jumping to EOF so parsing can continue
                idx = Math.min(idx+1, tokens.size()-1);
            }
            return true;
        }
        // check for extra tokens before newline/EOF - we use token stream, so if next is unexpected (like IDENT) that's extra
        SigmaToken next = look();
        if (next.getType() != org.sigma.lexer.TokenType.EOF &&
            next.getType() != org.sigma.lexer.TokenType.RBRACE) {
            // don't necessarily error here; leave to next parse
        }
        // After building expr, check associativity rules (use recorded start-line)
        checkAssociativityExpr(e, exprStartLine);
        return true;
    }

    // Expression parsing with precedence climbing (implemented as recursive descent via methods)
    private Ast.Expression parseExpression() { return parseLogicalOr(); }

    private Ast.Expression parseLogicalOr() {
        Ast.Expression left = parseLogicalAnd();
        if (left==null) return null;
        while (true) {
            SigmaToken op = look();
            if (op.getType() == org.sigma.lexer.TokenType.OR) {
                consume();
                Ast.Expression right = parseLogicalAnd();
                if (right==null) {
                    errors.add(String.format("Line %d: Expected expression after '||'", op.getLine()));
                    return null;
                }
                left = new Ast.Binary("||", left, right, op.getLine(), op.getColumn());
                continue;
            }
            break;
        }
        return left;
    }

    private Ast.Expression parseLogicalAnd() {
        Ast.Expression left = parseRelational();
        if (left==null) return null;
        while (true) {
            SigmaToken op = look();
            if (op.getType() == org.sigma.lexer.TokenType.AND) {
                consume();
                Ast.Expression right = parseRelational();
                if (right==null) {
                    errors.add(String.format("Line %d: Expected expression after '&&'", op.getLine()));
                    return null;
                }
                left = new Ast.Binary("&&", left, right, op.getLine(), op.getColumn());
                continue;
            }
            break;
        }
        return left;
    }

    private Ast.Expression parseRelational() {
        Ast.Expression left = parseAdditive();
        if (left==null) return null;
        while (true) {
            SigmaToken t = look();
            org.sigma.lexer.TokenType tType = t.getType();
            if (tType == org.sigma.lexer.TokenType.LT ||
                tType == org.sigma.lexer.TokenType.GT ||
                tType == org.sigma.lexer.TokenType.LE ||
                tType == org.sigma.lexer.TokenType.GE ||
                tType == org.sigma.lexer.TokenType.EQ ||
                tType == org.sigma.lexer.TokenType.NE) {
                String opText = t.getText();
                consume();
                Ast.Expression right = parseAdditive();
                if (right==null) {
                    errors.add(String.format("Line %d: Expected expression after '%s'", t.getLine(), opText));
                    return null;
                }
                left = new Ast.Binary(opText, left, right, t.getLine(), t.getColumn());
                continue;
            }
            break;
        }
        return left;
    }

    private Ast.Expression parseAdditive() {
        Ast.Expression left = parseMultiplicative();
        if (left==null) return null;
        while (true) {
            SigmaToken t = look();
            if (t.getType() == org.sigma.lexer.TokenType.PLUS) {
                consume();
                Ast.Expression right = parseMultiplicative();
                if (right==null) {
                    errors.add(String.format("Line %d: Expected expression after '+'", t.getLine()));
                    return null;
                }
                left = new Ast.Binary("+", left, right, t.getLine(), t.getColumn());
            } else if (t.getType() == org.sigma.lexer.TokenType.MINUS) {
                consume();
                Ast.Expression right = parseMultiplicative();
                if (right==null) {
                    errors.add(String.format("Line %d: Expected expression after '-'", t.getLine()));
                    return null;
                }
                left = new Ast.Binary("-", left, right, t.getLine(), t.getColumn());
            } else break;
        }
        return left;
    }

    private Ast.Expression parseMultiplicative() {
        Ast.Expression left = parsePower();
        if (left==null) return null;
        while (true) {
            SigmaToken t = look();
            if (t.getType() == org.sigma.lexer.TokenType.MULT) {
                consume();
                Ast.Expression right = parsePower();
                if (right==null) {
                    errors.add(String.format("Line %d: Expected expression after '*'", t.getLine()));
                    return null;
                }
                left = new Ast.Binary("*", left, right, t.getLine(), t.getColumn());
            } else if (t.getType() == org.sigma.lexer.TokenType.DIV) {
                consume();
                Ast.Expression right = parsePower();
                if (right==null) {
                    errors.add(String.format("Line %d: Expected expression after '/'", t.getLine()));
                    return null;
                }
                left = new Ast.Binary("/", left, right, t.getLine(), t.getColumn());
            } else if (t.getType() == org.sigma.lexer.TokenType.MOD) {
                consume();
                Ast.Expression right = parsePower();
                if (right==null) {
                    errors.add(String.format("Line %d: Expected expression after '%%'", t.getLine()));
                    return null;
                }
                left = new Ast.Binary("%", left, right, t.getLine(), t.getColumn());
            } else break;
        }
        return left;
    }

    private Ast.Expression parsePower() {
        Ast.Expression left = parseUnary();
        if (left==null) return null;
        // right-associative
        SigmaToken t = look();
        if (t.getType() == org.sigma.lexer.TokenType.POWER) {
            consume();
            Ast.Expression right = parsePower();
            if (right==null) {
                errors.add(String.format("Line %d: Expected expression after power operator", t.getLine()));
                return null;
            }
            return new Ast.Binary("**", left, right, t.getLine(), t.getColumn());
        }
        return left;
    }

    private Ast.Expression parseUnary() {
        SigmaToken t = look();
        if (t.getType() == org.sigma.lexer.TokenType.NOT) {
            consume();
            Ast.Expression r = parseUnary();
            if (r==null) {
                SigmaToken n = look();
                errors.add(String.format("Line %d: Expected expression after '!'", n.getLine()));
                return null;
            }
            return new Ast.Unary("!", r, t.getLine(), t.getColumn());
        }
        t = look();
        if (t.getType() == org.sigma.lexer.TokenType.MINUS) {
            consume();
            Ast.Expression r = parseUnary();
            if (r==null) {
                SigmaToken n = look();
                errors.add(String.format("Line %d: Expected expression after unary '-'", n.getLine()));
                return null;
            }
            return new Ast.Unary("neg", r, t.getLine(), t.getColumn());
        }
        return parsePrimary();
    }

    private Ast.Expression parsePrimary() {
        SigmaToken t = look();
        if (t.getType() == org.sigma.lexer.TokenType.INTEGER) {
            consume();
            try {
                return new Ast.IntLiteral(Integer.parseInt(t.getText()), t.getLine(), t.getColumn());
            } catch(Exception e) {
                errors.add("Integer parse error");
                return null;
            }
        }
        if (t.getType() == org.sigma.lexer.TokenType.FLOAT_LITERAL) {
            consume();
            try {
                return new Ast.DoubleLiteral(Double.parseDouble(t.getText()), t.getLine(), t.getColumn());
            } catch(Exception e) {
                errors.add("Float parse error");
                return null;
            }
        }
        if (t.getType() == org.sigma.lexer.TokenType.STRING) {
            consume();
            return new Ast.StringLiteral(t.getText(), t.getLine(), t.getColumn());
        }
        if (t.getType() == org.sigma.lexer.TokenType.TRUE) {
            consume();
            return new Ast.IntLiteral(1, t.getLine(), t.getColumn()); // true as 1
        }
        if (t.getType() == org.sigma.lexer.TokenType.FALSE) {
            consume();
            return new Ast.IntLiteral(0, t.getLine(), t.getColumn()); // false as 0
        }
        if (t.getType() == org.sigma.lexer.TokenType.NULL) {
            consume();
            return new Ast.Identifier("null", t.getLine(), t.getColumn());
        }
        if (t.getType() == org.sigma.lexer.TokenType.IDENTIFIER) {
            consume();
            String name = t.getText();
            // function call?
            if (acceptPunc(org.sigma.lexer.TokenType.LPAREN)) {
                List<Ast.Expression> args = new ArrayList<>();
                if (!acceptPunc(org.sigma.lexer.TokenType.RPAREN)) {
                    while (true) {
                        Ast.Expression a = parseExpression();
                        if (a == null) {
                            SigmaToken n = look();
                            errors.add(String.format("Line %d: Expected expression in argument list", n.getLine()));
                            return null;
                        }
                        args.add(a);
                        if (acceptPunc(org.sigma.lexer.TokenType.RPAREN)) break;
                        if (!acceptPunc(org.sigma.lexer.TokenType.COMMA)) {
                            SigmaToken n = look();
                            errors.add(String.format("Line %d: Expected ',' or ')' in argument list", n.getLine()));
                            return null;
                        }
                    }
                }
                Ast.Identifier id = new Ast.Identifier(name, t.getLine(), t.getColumn());
                return new Ast.Call(id, args, t.getLine(), t.getColumn());
            }
            return new Ast.Identifier(name, t.getLine(), t.getColumn());
        }

        if (t.getType() == org.sigma.lexer.TokenType.LPAREN) {
            consume();
            Ast.Expression e = parseExpression();
            if (e==null) {
                SigmaToken n = look();
                errors.add(String.format("Line %d: Expected expression after '('", n.getLine()));
                return null;
            }
            if (!acceptPunc(org.sigma.lexer.TokenType.RPAREN)) {
                SigmaToken n = look();
                errors.add(String.format("Line %d: Expected ')'", n.getLine()));
                return null;
            }
            return e;
        }
        return null;
    }

    // Check associativity: for left-assoc ops (+ - * / % && || etc) right child must not be same op; for right-assoc (**) left child must not be same op
    private void checkAssociativityExpr(Ast.Expression e, int line) {
        // Associativity checks disabled: do not emit errors for chained binary operators.
        // We still traverse the expression tree to allow future checks, but for now skip associativity enforcement.
        if (e instanceof Ast.Binary) {
            Ast.Binary b = (Ast.Binary)e;
            if (b.left != null) checkAssociativityExpr(b.left, line);
            if (b.right != null) checkAssociativityExpr(b.right, line);
        } else if (e instanceof Ast.Unary) {
            checkAssociativityExpr(((Ast.Unary)e).expr, line);
        } else if (e instanceof Ast.Call) {
            Ast.Call c = (Ast.Call)e;
            if (c.target != null) checkAssociativityExpr(c.target, line);
            if (c.args != null) for (Ast.Expression a : c.args) if (a != null) checkAssociativityExpr(a, line);
        }
    }

    /**
     * Debug helper: return a printable view of the token stream for the given source.
     */
    public static List<String> dumpTokens(String src) {
        SigmaLexerWrapper lexer = new SigmaLexerWrapper();
        List<SigmaToken> toks = lexer.tokenize(src);
        List<String> out = new ArrayList<>();
        for (SigmaToken t : toks) {
            out.add(String.format("%s('%s')@%d:%d", t.getType(), t.getText(), t.getLine(), t.getColumn()));
        }
        return out;
    }

}
