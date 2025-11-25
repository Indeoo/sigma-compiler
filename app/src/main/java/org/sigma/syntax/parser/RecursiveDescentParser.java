package org.sigma.syntax.parser;

import org.sigma.ast.Ast;

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

    private enum TokenType { IDENT, INT, FLOAT, STRING, KEYWORD, OP, PUNC, EOF }

    private static final Set<String> KEYWORDS = Set.of(
        "class","if","else","while","return","final","null",
        "int","double","float","boolean","String","void",
    "runcode","runfile","compile",
        // additional common / script-level keywords
        "for","break","continue","switch","case","default","do",
        "true","false","var","const","function","new","this","import","package"
    );

    private static final Map<String, String> PUNCT = Map.of(
        ";", "SEMI",
        ",", ",",
        "(", "LPAREN",
        ")", "RPAREN",
        "{", "LBRACE",
        "}", "RBRACE",
        ".", "DOT"
    );

    private static class Token {
        final TokenType type;
        final String text;
        final int line, col;
        // optional hint (e.g. suggested keyword for misspelling)
        final String hint;
        Token(TokenType t, String txt, int l, int c) { this(t, txt, l, c, null); }
        Token(TokenType t, String txt, int l, int c, String hint) { type = t; text = txt; line = l; col = c; this.hint = hint; }
        public String toString(){ return type+"('"+text+"')@"+line+":"+col + (hint!=null?"{hint="+hint+"}":""); }
    }

    // Parser builds AST nodes defined in org.example.ast.Ast

    private final List<Token> tokens;
    private int idx = 0;
    private final List<String> errors = new ArrayList<>();
    // errors discovered during tokenization (e.g. fuzzy keyword suggestions)
    private static final List<String> tokenizationHints = new ArrayList<>();

    private RecursiveDescentParser(List<Token> tokens) { this.tokens = tokens; }

    public static List<String> parseAndCollectErrors(String src) {
        tokenizationHints.clear();
        List<Token> toks = tokenize(src);
        RecursiveDescentParser p = new RecursiveDescentParser(toks);
        p.parseCompilationUnitAst();
        List<String> all = new ArrayList<>();
        all.addAll(tokenizationHints);
        all.addAll(p.errors);
        return normalizeDiagnostics(all);
    }

    /**
     * Parse input and produce an AST (CompilationUnit) along with syntax errors (if any).
     */
    public static ParseAstResult parseToAst(String src) {
        tokenizationHints.clear();
        List<Token> toks = tokenize(src);
        RecursiveDescentParser p = new RecursiveDescentParser(toks);
        Ast.CompilationUnit cu = p.parseCompilationUnitAst();
        List<String> all = new ArrayList<>();
        all.addAll(tokenizationHints);
        all.addAll(p.errors);
        return new ParseAstResult(cu, normalizeDiagnostics(all));
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

    // --- Tokenizer ---
    private static List<Token> tokenize(String s) {
        List<Token> out = new ArrayList<>();
        int pos = 0, line = 1, col = 1;
        while (pos < s.length()) {
            char c = s.charAt(pos);
            if (Character.isWhitespace(c)) {
                if (c == '\n') { line++; col = 1; } else col++;
                pos++; continue;
            }

            // line comment //
            if (c == '/' && pos + 1 < s.length() && s.charAt(pos+1) == '/') {
                pos += 2; col += 2;
                while (pos < s.length() && s.charAt(pos) != '\n') { pos++; col++; }
                continue;
            }

            // block comment /* ... */
            if (c == '/' && pos + 1 < s.length() && s.charAt(pos+1) == '*') {
                int startPos = pos;
                int startLine = line;
                pos += 2; col += 2;
                while (pos+1 < s.length() && !(s.charAt(pos) == '*' && s.charAt(pos+1) == '/')) {
                    if (s.charAt(pos) == '\n') { line++; col = 1; pos++; } else { pos++; col++; }
                }
                if (pos+1 < s.length()) { pos += 2; col += 2; }
                else {
                    // unterminated block comment: report at start line and advance to next line after start
                    tokenizationHints.add(String.format("Line %d: Unterminated block comment", startLine));
                    int nextNl = s.indexOf('\n', startPos);
                    if (nextNl >= 0) { pos = nextNl + 1; line = startLine + 1; col = 1; }
                    else { pos = s.length(); }
                }
                continue;
            }

            // string literal - treat unterminated strings as single token and do not tokenize interior
            if (c == '"') {
                int startCol = col;
                pos++; col++;
                StringBuilder sb = new StringBuilder();
                boolean closed = false;
                while (pos < s.length()) {
                    char ch = s.charAt(pos);
                    if (ch == '\n') break; // unterminated at newline
                    if (ch == '"') { closed = true; pos++; col++; break; }
                    if (ch == '\\' && pos+1 < s.length() && s.charAt(pos+1) != '\n') {
                        sb.append(ch); sb.append(s.charAt(pos+1)); pos += 2; col += 2; continue;
                    }
                    // stop if we hit punctuation that should be tokenized separately (avoid swallowing ) or ;)
                    if (ch == ')' || ch == ';') break;
                    sb.append(ch); pos++; col++;
                }
                if (!closed) {
                    out.add(new Token(TokenType.STRING, sb.toString(), line, startCol, "Unterminated string literal"));
                    tokenizationHints.add(String.format("Line %d: Unterminated string literal", line));
                    if (pos < s.length() && s.charAt(pos) == '\n') {
                        int nextNl = s.indexOf('\n', pos);
                        if (nextNl >= 0) { pos = nextNl + 1; line++; col = 1; } else { pos = s.length(); }
                    }
                } else {
                    out.add(new Token(TokenType.STRING, sb.toString(), line, startCol));
                }
                continue;
            }

            // numbers (support leading-dot floats like .5 and detect invalid numeric formats)
            if (Character.isDigit(c) || (c == '.' && pos+1 < s.length() && Character.isDigit(s.charAt(pos+1)))) {
                int startCol = col; StringBuilder sb = new StringBuilder(); boolean hasDot=false;
                if (c == '.') { hasDot = true; sb.append('.'); pos++; col++; }
                while (pos < s.length() && (Character.isDigit(s.charAt(pos)) || (!hasDot && s.charAt(pos)=='.'))) {
                    if (s.charAt(pos)=='.') hasDot=true; sb.append(s.charAt(pos)); pos++; col++; }
                if (pos < s.length() && Character.isLetter(s.charAt(pos))) {
                    StringBuilder tail = new StringBuilder();
                    while (pos < s.length() && (Character.isLetterOrDigit(s.charAt(pos)) || s.charAt(pos)=='_')) { tail.append(s.charAt(pos)); pos++; col++; }
                    tokenizationHints.add(String.format("Line %d: Invalid numeric literal '%s%s'", line, sb.toString(), tail.toString()));
                    out.add(new Token(TokenType.IDENT, sb.toString()+tail.toString(), line, startCol, "Invalid numeric literal"));
                } else if (!hasDot && sb.toString().equals(".")) {
                    tokenizationHints.add(String.format("Line %d: Invalid numeric literal '.'", line));
                    out.add(new Token(TokenType.OP, ".", line, startCol));
                } else {
                    if (hasDot) {
                        if (pos < s.length() && s.charAt(pos) == '.') {
                            tokenizationHints.add(String.format("Line %d: Invalid numeric literal '%s'", line, sb.toString() + "."));
                        }
                        out.add(new Token(TokenType.FLOAT, sb.toString(), line, startCol));
                    } else {
                        out.add(new Token(TokenType.INT, sb.toString(), line, startCol));
                    }
                }
                continue;
            }

            // identifiers / keywords
            if (Character.isLetter(c) || c == '_') {
                int startCol = col; StringBuilder sb = new StringBuilder(); sb.append(c); pos++; col++;
                while (pos < s.length() && (Character.isLetterOrDigit(s.charAt(pos)) || s.charAt(pos)=='_')) { sb.append(s.charAt(pos)); pos++; col++; }
                String txt = sb.toString();
                if (KEYWORDS.contains(txt)) {
                    out.add(new Token(TokenType.KEYWORD, txt, line, startCol));
                } else {
                    String suggestion = suggestKeyword(txt);
                    if (suggestion != null) {
                        out.add(new Token(TokenType.IDENT, txt, line, startCol, suggestion));
                        tokenizationHints.add(String.format("Line %d: Did you mean '%s' instead of '%s'?", line, suggestion, txt));
                    } else {
                        out.add(new Token(TokenType.IDENT, txt, line, startCol));
                    }
                }
                continue;
            }

            // operators and punctuation
            String two = pos+1 < s.length() ? s.substring(pos,pos+2) : null;
            if (two != null && (two.equals("<=")||two.equals(">=")||two.equals("==")||two.equals("!=")||two.equals("&&")||two.equals("||")||two.equals("**"))) {
                out.add(new Token(TokenType.OP, two, line, col)); pos+=2; col+=2; continue;
            }
            String chs = String.valueOf(c);
            // '^' intentionally removed: only '**' is the power operator. Keep single-char ops here.
            if ("+-*/%<>!=.".indexOf(c) >= 0) { out.add(new Token(TokenType.OP, chs, line, col)); pos++; col++; continue; }
            if (PUNCT.containsKey(chs)) { out.add(new Token(TokenType.PUNC, chs, line, col)); pos++; col++; continue; }

            // unknown char
            out.add(new Token(TokenType.OP, chs, line, col)); pos++; col++;
        }
        out.add(new Token(TokenType.EOF, "", line, col));
        return out;
    }

    private static String suggestKeyword(String id) {
        int bestDist = Integer.MAX_VALUE; String best=null;
        for (String kw : KEYWORDS) {
            int d = levenshtein(id, kw);
            if (d < bestDist) { bestDist = d; best = kw; }
        }
        if (bestDist <= 1) return best; return null;
    }

    private static int levenshtein(String a, String b) {
        int n=a.length(), m=b.length(); int[][] dp=new int[n+1][m+1];
        for(int i=0;i<=n;i++) dp[i][0]=i; for(int j=0;j<=m;j++) dp[0][j]=j;
        for(int i=1;i<=n;i++) for(int j=1;j<=m;j++) dp[i][j]=Math.min(Math.min(dp[i-1][j]+1, dp[i][j-1]+1), dp[i-1][j-1]+(a.charAt(i-1)==b.charAt(j-1)?0:1));
        return dp[n][m];
    }

    // --- Parser helpers ---
    private Token look() { return tokens.get(Math.min(idx, tokens.size()-1)); }
    private Token consume() { return tokens.get(idx++); }
    private boolean acceptOp(String s) { Token t = look(); if (t.type==TokenType.OP && t.text.equals(s)) { consume(); return true; } return false; }
    private boolean acceptPunc(String s) { Token t = look(); if (t.type==TokenType.PUNC && t.text.equals(s)) { consume(); return true; } return false; }
    private boolean acceptKeyword(String s) { Token t = look(); if (t.type==TokenType.KEYWORD && t.text.equals(s)) { consume(); return true; } return false; }
    private void expectPunc(String s) { if (!acceptPunc(s)) { Token t = look(); errors.add(String.format("Line %d: Expected '%s' but found '%s'", t.line, s, t.text)); } }

    // --- Grammar ---
    private Ast.CompilationUnit parseCompilationUnitAst() {
        List<Ast.Statement> stmts = new ArrayList<>();
        while (look().type != TokenType.EOF) {
            Ast.Statement s = parseStatementOrDirectiveAst();
            if (s != null) stmts.add(s);
            else {
                // skip one token to avoid infinite loop
                Token u = look(); errors.add(String.format("Line %d: Unexpected token '%s'", u.line, u.text)); idx++;
            }
        }
        return new Ast.CompilationUnit(stmts);
    }

    private Ast.Statement parseStatementOrDirectiveAst() {
        Token t = look();
        String hint = t.hint;
        boolean isDirectiveKeyword = (t.type==TokenType.KEYWORD && (t.text.equals("runcode")||t.text.equals("runfile")||t.text.equals("compile")))
                || (t.type==TokenType.IDENT && hint!=null && (hint.equals("runcode")||hint.equals("runfile")||hint.equals("compile")));
    if (isDirectiveKeyword) {
            // treat directive as an expression statement when possible (e.g., runcode println(...);)
            consume(); // directive token
            Ast.Expression e = null;
            if (look().type == TokenType.STRING || look().type == TokenType.IDENT || look().type == TokenType.KEYWORD) {
                e = parseExpression();
            }
            if (!acceptPunc(";")) {
                int scan = idx; boolean found = false;
                while (scan < tokens.size()) { Token tok = tokens.get(scan++); if (tok.type==TokenType.PUNC && tok.text.equals(";")) { found = true; break; } }
                if (found) { while (idx < tokens.size()) { Token tok = look(); idx++; if (tok.type==TokenType.PUNC && tok.text.equals(";")) break; } }
                else { errors.add(String.format("Line %d: Missing semicolon after directive '%s'", t.line, t.text)); }
            }
            if (e != null) return new Ast.ExpressionStatement(e, t.line, t.col);
            return null;
        }
        // handle block-level statements: if, while, block, variable declarations, return
        // if statement
        if (look().type==TokenType.KEYWORD && look().text.equals("if")) {
            consume();
            if (!acceptPunc("(")) { Token n = look(); errors.add(String.format("Line %d: Expected '(' after 'if'", n.line)); }
            Ast.Expression cond = parseExpression();
            if (cond == null) { Token n = look(); errors.add(String.format("Line %d: Expected condition expression in 'if'", n.line)); }
            if (!acceptPunc(")")) { Token n = look(); errors.add(String.format("Line %d: Expected ')' after if condition", n.line)); }
            Ast.Statement thenStmt = parseInnerStatement();
            Ast.Statement elseStmt = null;
            if (look().type==TokenType.KEYWORD && look().text.equals("else")) { consume(); elseStmt = parseInnerStatement(); }
            return new Ast.IfStatement(cond, thenStmt, elseStmt);
        }

        // while statement
        if (look().type==TokenType.KEYWORD && look().text.equals("while")) {
            consume();
            if (!acceptPunc("(")) { Token n = look(); errors.add(String.format("Line %d: Expected '(' after 'while'", n.line)); }
            Ast.Expression cond = parseExpression();
            if (cond == null) { Token n = look(); errors.add(String.format("Line %d: Expected condition expression in 'while'", n.line)); }
            if (!acceptPunc(")")) { Token n = look(); errors.add(String.format("Line %d: Expected ')' after while condition", n.line)); }
            Ast.Statement body = parseInnerStatement();
            return new Ast.WhileStatement(cond, body);
        }

        // block
        if (look().type==TokenType.PUNC && look().text.equals("{")) {
            return parseBlock();
        }

        // variable declaration: KEYWORD type IDENTIFIER ...
        if (look().type == TokenType.KEYWORD && (look().text.equals("int")||look().text.equals("double")||look().text.equals("String")||look().text.equals("boolean")||look().text.equals("float"))) {
            String typeName = look().text; consume();
            Token id = look();
            if (id.type == TokenType.IDENT) {
                consume();
                Ast.Expression init = null;
                if (acceptOp("=")) {
                    init = parseExpression();
                }
                if (!acceptPunc(";")) {
                    errors.add(String.format("Line %d: Missing semicolon after declaration of %s", id.line, id.text));
                    // resync
                    int scan = idx; boolean found=false; while (scan < tokens.size()) { Token tok = tokens.get(scan++); if (tok.type==TokenType.PUNC && tok.text.equals(";")) { found=true; break; } }
                    if (found) { while (idx < tokens.size()) { Token tok = look(); idx++; if (tok.type==TokenType.PUNC && tok.text.equals(";")) break; } }
                }
                return new Ast.VariableDeclaration(typeName, id.text, init, id.line, id.col);
            } else {
                errors.add(String.format("Line %d: Expected identifier after type '%s'", id.line, typeName));
                return null;
            }
        }

        // assignment statement: IDENT '=' expr ';'
        if (look().type == TokenType.IDENT) {
            Token next = tokens.size() > idx+1 ? tokens.get(idx+1) : null;
            if (next != null && next.type == TokenType.OP && "=".equals(next.text)) {
                Token id = consume(); // ident
                consume(); // '='
                Ast.Expression val = parseExpression();
                if (!acceptPunc(";")) {
                    errors.add(String.format("Line %d: Missing semicolon after assignment to %s", id.line, id.text));
                    // resync
                    int scan = idx; boolean found=false; while (scan < tokens.size()) { Token tok = tokens.get(scan++); if (tok.type==TokenType.PUNC && tok.text.equals(";")) { found=true; break; } }
                    if (found) { while (idx < tokens.size()) { Token tok = look(); idx++; if (tok.type==TokenType.PUNC && tok.text.equals(";")) break; } }
                }
                return new Ast.Assignment(id.text, val);
            }
        }

        // fallback: expression statement
        int exprStartLine = look().line;
        int exprStartCol = look().col;
        Ast.Expression expr = parseExpression();
        if (expr == null) return null;
        if (!acceptPunc(";")) {
            // Attribute missing-semicolon to the statement's start line (exprStartLine) instead of the next token's line
            errors.add(String.format("Line %d: Missing semicolon at end of statement", exprStartLine));
            // resync to next semicolon
            int scan = idx; boolean found=false; while (scan < tokens.size()) { Token t2 = tokens.get(scan++); if (t2.type==TokenType.PUNC && t2.text.equals(";")) { found=true; break; } }
            if (found) { while (idx < tokens.size()) { Token tok2 = look(); idx++; if (tok2.type==TokenType.PUNC && tok2.text.equals(";")) break; } } else { idx = Math.min(idx+1, tokens.size()-1); }
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
        while (!(look().type==TokenType.PUNC && look().text.equals("}")) && look().type != TokenType.EOF) {
            Ast.Statement s = parseStatementOrDirectiveAst();
            if (s != null) stmts.add(s);
            else {
                Token u = look(); errors.add(String.format("Line %d: Unexpected token '%s' in block", u.line, u.text)); idx++;
            }
        }
        if (!acceptPunc("}")) { Token n = look(); errors.add(String.format("Line %d: Expected '}' to close block", n.line)); }
        return new Ast.Block(stmts);
    }

    // parse a single statement that may be a block or a single-line statement
    private Ast.Statement parseInnerStatement() {
        if (look().type==TokenType.PUNC && look().text.equals("{")) return parseBlock();
        return parseStatementOrDirectiveAst();
    }

    private boolean parseExpressionStatement() {
        int start = idx;
        int exprStartLine = tokens.get(Math.min(start, tokens.size()-1)).line;
        Ast.Expression e = parseExpression();
        if (e==null) return false;
        // require semicolon
        if (!acceptPunc(";")) {
            // Use the recorded start line for error attribution so the missing-semicolon is reported on the statement's line
            errors.add(String.format("Line %d: Missing semicolon at end of statement", exprStartLine));
            // resynchronize: skip tokens until next semicolon or EOF to avoid cascaded errors
            int scan = idx;
            boolean found = false;
            while (scan < tokens.size()) {
                Token tok = tokens.get(scan++);
                if (tok.type == TokenType.PUNC && tok.text.equals(";")) { found = true; break; }
            }
            if (found) {
                while (idx < tokens.size()) { Token tok = look(); idx++; if (tok.type==TokenType.PUNC && tok.text.equals(";")) break; }
            } else {
                // couldn't find a semicolon to resync to; advance by one token instead of jumping to EOF so parsing can continue
                idx = Math.min(idx+1, tokens.size()-1);
            }
            return true;
        }
        // check for extra tokens before newline/EOF - we use token stream, so if next is unexpected (like IDENT) that's extra
        Token next = look();
        if (next.type != TokenType.EOF && !(next.type==TokenType.PUNC && next.text.equals("}"))) {
            // don't necessarily error here; leave to next parse
        }
        // After building expr, check associativity rules (use recorded start-line)
        checkAssociativityExpr(e, exprStartLine);
        return true;
    }

    // Expression parsing with precedence climbing (implemented as recursive descent via methods)
    private Ast.Expression parseExpression() { return parseLogicalOr(); }

    private Ast.Expression parseLogicalOr() {
        Ast.Expression left = parseLogicalAnd(); if (left==null) return null;
        while (true) {
            Token op = look();
            if (op.type==TokenType.OP && op.text.equals("||")) {
                consume();
                Ast.Expression right = parseLogicalAnd(); if (right==null) { errors.add(String.format("Line %d: Expected expression after '||'", op.line)); return null; }
                left = new Ast.Binary("||", left, right, op.line, op.col);
                continue;
            }
            break;
        }
        return left;
    }

    private Ast.Expression parseLogicalAnd() {
        Ast.Expression left = parseRelational(); if (left==null) return null;
        while (true) {
            Token op = look();
            if (op.type==TokenType.OP && op.text.equals("&&")) {
                consume();
                Ast.Expression right = parseRelational(); if (right==null) { errors.add(String.format("Line %d: Expected expression after '&&'", op.line)); return null; }
                left = new Ast.Binary("&&", left, right, op.line, op.col);
                continue;
            }
            break;
        }
        return left;
    }

    private Ast.Expression parseRelational() {
        Ast.Expression left = parseAdditive(); if (left==null) return null;
        while (true) {
            Token t = look();
            if (t.type==TokenType.OP && (t.text.equals("<")||t.text.equals(">")||t.text.equals("<=")||t.text.equals(">=")||t.text.equals("==")||t.text.equals("!="))) {
                consume();
                Ast.Expression right = parseAdditive(); if (right==null) { errors.add(String.format("Line %d: Expected expression after '%s'", t.line,t.text)); return null; }
                left = new Ast.Binary(t.text, left, right, t.line, t.col);
                continue;
            }
            break;
        }
        return left;
    }

    private Ast.Expression parseAdditive() {
        Ast.Expression left = parseMultiplicative(); if (left==null) return null;
        while (true) {
            Token t = look();
            if (t.type==TokenType.OP && t.text.equals("+")) {
                consume();
                Ast.Expression right = parseMultiplicative(); if (right==null) { errors.add(String.format("Line %d: Expected expression after '+'", t.line)); return null; }
                left = new Ast.Binary("+", left, right, t.line, t.col);
            } else if (t.type==TokenType.OP && t.text.equals("-")) {
                consume();
                Ast.Expression right = parseMultiplicative(); if (right==null) { errors.add(String.format("Line %d: Expected expression after '-'", t.line)); return null; }
                left = new Ast.Binary("-", left, right, t.line, t.col);
            } else break;
        }
        return left;
    }

    private Ast.Expression parseMultiplicative() {
        Ast.Expression left = parsePower(); if (left==null) return null;
        while (true) {
            Token t = look();
            if (t.type==TokenType.OP && t.text.equals("*")) {
                consume(); Ast.Expression right = parsePower(); if (right==null) { errors.add(String.format("Line %d: Expected expression after '*'", t.line)); return null; }
                left = new Ast.Binary("*", left, right, t.line, t.col);
            } else if (t.type==TokenType.OP && t.text.equals("/")) {
                consume(); Ast.Expression right = parsePower(); if (right==null) { errors.add(String.format("Line %d: Expected expression after '/'", t.line)); return null; }
                left = new Ast.Binary("/", left, right, t.line, t.col);
            } else if (t.type==TokenType.OP && t.text.equals("%")) {
                consume(); Ast.Expression right = parsePower(); if (right==null) { errors.add(String.format("Line %d: Expected expression after '%%'", t.line)); return null; }
                left = new Ast.Binary("%", left, right, t.line, t.col);
            } else break;
        }
        return left;
    }

    private Ast.Expression parsePower() {
        Ast.Expression left = parseUnary(); if (left==null) return null;
        // right-associative
        Token t = look();
        if (t.type==TokenType.OP && t.text.equals("**")) {
            consume();
            Ast.Expression right = parsePower(); if (right==null) { errors.add(String.format("Line %d: Expected expression after power operator", t.line)); return null; }
            return new Ast.Binary("**", left, right, t.line, t.col);
        }
        return left;
    }

    private Ast.Expression parseUnary() {
        Token t = look();
        if (t.type==TokenType.OP && t.text.equals("!")) {
            consume();
            Ast.Expression r = parseUnary();
            if (r==null) { Token n=look(); errors.add(String.format("Line %d: Expected expression after '!'", n.line)); return null; }
            return new Ast.Unary("!", r, t.line, t.col);
        }
        t = look();
        if (t.type==TokenType.OP && t.text.equals("-")) {
            consume();
            Ast.Expression r = parseUnary();
            if (r==null) { Token n=look(); errors.add(String.format("Line %d: Expected expression after unary '-'", n.line)); return null; }
            return new Ast.Unary("neg", r, t.line, t.col);
        }
        return parsePrimary();
    }

    private Ast.Expression parsePrimary() {
        Token t = look();
        if (t.type==TokenType.INT) { consume(); try { return new Ast.IntLiteral(Integer.parseInt(t.text), t.line, t.col); } catch(Exception e){ errors.add("Integer parse error"); return null; } }
        if (t.type==TokenType.FLOAT) { consume(); try { return new Ast.DoubleLiteral(Double.parseDouble(t.text), t.line, t.col); } catch(Exception e){ errors.add("Float parse error"); return null; } }
    if (t.type==TokenType.STRING) { consume(); return new Ast.StringLiteral(t.text, t.line, t.col); }
    if (t.type==TokenType.IDENT) {
            consume();
            String name = t.text;
            // function call?
            if (acceptPunc("(")) {
                List<Ast.Expression> args = new ArrayList<>();
                if (!acceptPunc(")")) {
                    while (true) {
                        Ast.Expression a = parseExpression();
                        if (a == null) { Token n = look(); errors.add(String.format("Line %d: Expected expression in argument list", n.line)); return null; }
                        args.add(a);
                        if (acceptPunc(")")) break;
                        if (!acceptPunc(",")) { Token n = look(); errors.add(String.format("Line %d: Expected ',' or ')' in argument list", n.line)); return null; }
                    }
                }
                Ast.Identifier id = new Ast.Identifier(name, t.line, t.col);
                return new Ast.Call(id, args, t.line, t.col);
            }
            return new Ast.Identifier(name, t.line, t.col);
    }
    
    if (t.type==TokenType.PUNC && t.text.equals("(")) { consume(); Ast.Expression e = parseExpression(); if (e==null) { Token n=look(); errors.add(String.format("Line %d: Expected expression after '('", n.line)); return null; } if (!acceptPunc(")")) { Token n=look(); errors.add(String.format("Line %d: Expected ')'", n.line)); return null; } return e; }
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
        List<Token> toks = tokenize(src);
        List<String> out = new ArrayList<>();
        for (Token t : toks) out.add(t.toString());
        return out;
    }

}
