package org.example.lexer;

import java.util.ArrayList;
import java.util.List;

public class Lexer {
    // ---- Token model ----
    public enum TokenType {
        IDENT,            // [A-Za-z_][A-Za-z0-9_]*
        INT,              // [0-9]+
        FLOAT,            // [0-9]+ '.' [0-9]+
        STRING,           // " ... " (з підтримкою \-escape)
        WS,               // пробіли й табуляція
        LINE_COMMENT,     // // ...
        BLOCK_COMMENT,    // /* ... */
        // ПАРНІ/ОДИНОЧНІ ОПЕРАТОРИ
        LE, GE, EQ, NE,   // <= >= == !=
        LT, GT, ASSIGN, NOT, // < > = !
        AND_AND, OR_OR,   // && ||
        PLUS, MINUS, STAR, PERCENT,
        LPAREN, RPAREN, LBRACE, RBRACE,
        SEMI, COMMA, DOT, DIV, // ; , . /
        EOF
    }

    public static final class Token {
        public final TokenType type;
        public final String lexeme;
        public final int line;
        public final int col;
        public Token(TokenType type, String lexeme, int line, int col) {
            this.type = type; this.lexeme = lexeme; this.line = line; this.col = col;
        }
        @Override public String toString() {
            return type + "('" + lexeme + "')@" + line + ":" + col;
        }
    }

    // ---- Input ----
    private final String src;
    private int p = 0, line = 1, col = 1;

    public Lexer(String src) { this.src = src != null ? src : ""; }

    // ---- Helpers ----
    private boolean isAtEnd() { return p >= src.length(); }
    private char peek() { return isAtEnd() ? '\0' : src.charAt(p); }
    private char peekNext() { return (p + 1 >= src.length()) ? '\0' : src.charAt(p + 1); }
    private char advance() {
        char c = peek();
        p++;
        if (c == '\n') { line++; col = 1; } else { col++; }
        return c;
    }
    private boolean match(char expected) {
        if (isAtEnd() || src.charAt(p) != expected) return false;
        p++; col++;
        return true;
    }

    private static boolean isLetter(char c) { return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z'); }
    private static boolean isDigit(char c)  { return (c >= '0' && c <= '9'); }
    private static boolean isIdStart(char c){ return isLetter(c) || c == '_'; }
    private static boolean isIdPart(char c) { return isLetter(c) || isDigit(c) || c == '_'; }

    // ---- Public API ----
    public List<Token> tokenize(boolean includeWhitespaceAndComments) {
        List<Token> out = new ArrayList<>();
        Token t;
        do {
            t = nextToken(includeWhitespaceAndComments);
            if (t != null) out.add(t);
        } while (t == null || t.type != TokenType.EOF);
        return out;
    }

    public Token nextToken(boolean includeWhitespaceAndComments) {
        // Skip nothing here; each class handled explicitly
        if (isAtEnd()) return new Token(TokenType.EOF, "", line, col);

        char c = peek();
        int startLine = line, startCol = col;

        // q0: LETTER or '_': qID (IDENT)
        if (isIdStart(c)) {
            StringBuilder sb = new StringBuilder();
            sb.append(advance()); // first
            while (isIdPart(peek())) sb.append(advance());
            // qID_END on OTHER
            return new Token(TokenType.IDENT, sb.toString(), startLine, startCol);
        }

        // q0: DIGIT -> qINT / qFDOT / qFLOAT
        if (isDigit(c)) {
            StringBuilder sb = new StringBuilder();
            while (isDigit(peek())) sb.append(advance());
            // lookahead for '.'; only FLOAT if next is digit (qFDOT -> qFLOAT on DIGIT)
            if (peek() == '.' && isDigit(peekNext())) {
                sb.append(advance()); // '.'
                while (isDigit(peek())) sb.append(advance());
                return new Token(TokenType.FLOAT, sb.toString(), startLine, startCol);
            }
            // else INT_END
            return new Token(TokenType.INT, sb.toString(), startLine, startCol);
        }

        // q0: STRING '"'
        if (c == '"') {
            advance(); // consume opening "
            StringBuilder sb = new StringBuilder();
            boolean closed = false;
            while (!isAtEnd()) {
                char ch = advance();
                if (ch == '"') { closed = true; break; }
                if (ch == '\\') { // qESC
                    if (isAtEnd()) break;
                    char esc = advance(); // ESC_CHAR
                    sb.append('\\').append(esc);
                } else {
                    sb.append(ch); // STR_CHAR
                }
            }
            if (!closed) {
                throw error(startLine, startCol, "Unterminated string literal");
            }
            return new Token(TokenType.STRING, sb.toString(), startLine, startCol);
        }

        // q0: WS (space or tab)
        if (c == ' ' || c == '\t') {
            StringBuilder sb = new StringBuilder();
            while (peek() == ' ' || peek() == '\t') sb.append(advance());
            if (includeWhitespaceAndComments)
                return new Token(TokenType.WS, sb.toString(), startLine, startCol);
            // skip and return next
            return nextToken(includeWhitespaceAndComments);
        }

        // q0: '/' -> DIV | // line comment | /* block comment */
        if (c == '/') {
            advance();
            if (match('/')) {
                // line comment
                StringBuilder sb = new StringBuilder();
                while (!isAtEnd() && peek() != '\n') sb.append(advance()); // LC_CHAR*
                if (includeWhitespaceAndComments)
                    return new Token(TokenType.LINE_COMMENT, sb.toString(), startLine, startCol);
                return nextToken(includeWhitespaceAndComments);
            } else if (match('*')) {
                // block comment (non-nested per DFA)
                StringBuilder sb = new StringBuilder();
                boolean closed = false;
                while (!isAtEnd()) {
                    char ch = advance(); // BC_NOT_STAR or '*'
                    if (ch == '*') {
                        if (match('/')) { closed = true; break; }  // qBC_STAR '/'
                        else { // stayed in qBC_STAR on '*', or returned to qBLOCK_COMMENT on NOT '/'
                            // leave the '*' in lexeme too
                            sb.append('*');
                        }
                    } else {
                        sb.append(ch);
                    }
                }
                if (!closed) throw error(startLine, startCol, "Unterminated block comment");
                if (includeWhitespaceAndComments)
                    return new Token(TokenType.BLOCK_COMMENT, sb.toString(), startLine, startCol);
                return nextToken(includeWhitespaceAndComments);
            } else {
                return new Token(TokenType.DIV, "/", startLine, startCol);
            }
        }

        // Парні оператори та одиночні символи
        switch (c) {
            case '<': {
                advance();
                if (match('=')) return new Token(TokenType.LE, "<=", startLine, startCol);
                return new Token(TokenType.LT, "<", startLine, startCol);
            }
            case '>': {
                advance();
                if (match('=')) return new Token(TokenType.GE, ">=", startLine, startCol);
                return new Token(TokenType.GT, ">", startLine, startCol);
            }
            case '=': {
                advance();
                if (match('=')) return new Token(TokenType.EQ, "==", startLine, startCol);
                return new Token(TokenType.ASSIGN, "=", startLine, startCol);
            }
            case '!': {
                advance();
                if (match('=')) return new Token(TokenType.NE, "!=", startLine, startCol);
                return new Token(TokenType.NOT, "!", startLine, startCol);
            }
            case '&': {
                advance();
                if (match('&')) return new Token(TokenType.AND_AND, "&&", startLine, startCol);
                // У DFA одинарний '&' не фінальний: вважатимемо помилкою вводу
                throw error(startLine, startCol, "Single '&' not allowed (expected '&&')");
            }
            case '|': {
                advance();
                if (match('|')) return new Token(TokenType.OR_OR, "||", startLine, startCol);
                throw error(startLine, startCol, "Single '|' not allowed (expected '||')");
            }
            case '+': advance(); return new Token(TokenType.PLUS, "+", startLine, startCol);
            case '-': advance(); return new Token(TokenType.MINUS, "-", startLine, startCol);
            case '*': advance(); return new Token(TokenType.STAR, "*", startLine, startCol);
            case '%': advance(); return new Token(TokenType.PERCENT, "%", startLine, startCol);
            case '(': advance(); return new Token(TokenType.LPAREN, "(", startLine, startCol);
            case ')': advance(); return new Token(TokenType.RPAREN, ")", startLine, startCol);
            case '{': advance(); return new Token(TokenType.LBRACE, "{", startLine, startCol);
            case '}': advance(); return new Token(TokenType.RBRACE, "}", startLine, startCol);
            case ';': advance(); return new Token(TokenType.SEMI, ";", startLine, startCol);
            case ',': advance(); return new Token(TokenType.COMMA, ",", startLine, startCol);
            case '.': advance(); return new Token(TokenType.DOT, ".", startLine, startCol);
            case '\n': {
                advance(); // новий рядок не належить WS за DFA (тільки ' ' і '\t'), просто ігноруємо
                return nextToken(includeWhitespaceAndComments);
            }
            default:
                // Будь-що інше — помилка (DFA мав клас OTHER лише для завершення токенів)
                char bad = advance();
                throw error(startLine, startCol, "Unexpected character: '" + printable(bad) + "'");
        }
    }

    private static RuntimeException error(int line, int col, String msg) {
        return new RuntimeException("Lex error at " + line + ":" + col + " - " + msg);
    }

    private static String printable(char c) {
        if (c == '\0') return "\\0";
        if (c == '\t') return "\\t";
        if (c == '\n') return "\\n";
        return String.valueOf(c);
    }

    // ---- Demo ----
    public static void main(String[] args) {
//        String code = String.join("\n",
//                "var a1 = 123;",
//                "var b = 45.67;",
//                "var s = \"hi\\nthere\";",
//                "if (a1 <= b && b != 0) {",
//                "  // line comment",
//                "  /* block",
//                "     comment */",
//                "  a1 = a1 + 1;",
//                "}"
//        );
        String code = """
            inst count = 4s2;
            float price = 19.99;s
            String message = "Hello World";
            boolean isValid = true;
            """;
        Lexer lx = new Lexer(code);
        List<Token> tokens = lx.tokenize(true); // true — включати WS та коментарі; false — пропускати
        for (Token t : tokens) System.out.println(t);
    }
}
