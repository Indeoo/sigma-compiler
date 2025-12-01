package org.sigma.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Custom lexer for the Sigma language based on the gpt.jff finite automaton.
 * Implements a character-based state machine to tokenize Sigma source code.
 */
public class SigmaLexerWrapper {

    // Keyword mapping for post-recognition checking
    private static final Map<String, TokenType> KEYWORDS = Map.ofEntries(
            Map.entry("class", TokenType.CLASS),
            Map.entry("if", TokenType.IF),
            Map.entry("else", TokenType.ELSE),
            Map.entry("for", TokenType.FOR),
            Map.entry("while", TokenType.WHILE),
            Map.entry("return", TokenType.RETURN),
            Map.entry("final", TokenType.FINAL),
            Map.entry("null", TokenType.NULL),
            Map.entry("in", TokenType.IN),
            Map.entry("new", TokenType.NEW),
            Map.entry("int", TokenType.INT),
            Map.entry("double", TokenType.DOUBLE),
            Map.entry("float", TokenType.FLOAT),
            Map.entry("boolean", TokenType.BOOLEAN),
            Map.entry("String", TokenType.STRING_TYPE),
            Map.entry("void", TokenType.VOID),
            Map.entry("true", TokenType.TRUE),
            Map.entry("false", TokenType.FALSE)
    );

    private String source;
    private int position;
    private int line;
    private int column;

    /**
     * Tokenizes the given source string into a list of SigmaTokens.
     * Whitespace and comments are skipped (not included in the returned list).
     *
     * @param source the source code to tokenize
     * @return list of tokens (including EOF token at the end)
     */
    public List<SigmaToken> tokenize(String source) {
        this.source = source != null ? source : "";
        this.position = 0;
        this.line = 1;
        this.column = 0;

        List<SigmaToken> tokens = new ArrayList<>();

        while (!isAtEnd()) {
            skipWhitespaceAndNewlines();
            if (isAtEnd()) break;

            SigmaToken token = nextToken();
            if (token != null) {
                tokens.add(token);
            }
        }

        tokens.add(new SigmaToken(TokenType.EOF, "", line, column));
        return tokens;
    }

    /**
     * Returns the next token from the source.
     * Returns null for comments (which are skipped).
     */
    private SigmaToken nextToken() {
        char c = peek();
        int startLine = line;
        int startCol = column;

        // Dispatch based on first character
        if (isLetter(c) || c == '_') {
            return handleIdentifier();
        }

        if (isDigit(c)) {
            return handleNumber();
        }

        switch (c) {
            case '"':
                return handleString();
            case '/':
                return handleSlash(); // Could be /, //, or /*
            case '<':
                return handleLessOrLessEqual();
            case '>':
                return handleGreaterOrGreaterEqual();
            case '=':
                return handleAssignOrEqual();
            case '!':
                return handleNotOrNotEqual();
            case '&':
                return handleAmpersand();
            case '|':
                return handlePipe();

            // Single-char operators and delimiters
            case '+':
                advance();
                return new SigmaToken(TokenType.PLUS, "+", startLine, startCol);
            case '-':
                advance();
                return new SigmaToken(TokenType.MINUS, "-", startLine, startCol);
            case '*':
                return handleMultOrPower();
            case '%':
                advance();
                return new SigmaToken(TokenType.MOD, "%", startLine, startCol);
            case '(':
                advance();
                return new SigmaToken(TokenType.LPAREN, "(", startLine, startCol);
            case ')':
                advance();
                return new SigmaToken(TokenType.RPAREN, ")", startLine, startCol);
            case '{':
                advance();
                return new SigmaToken(TokenType.LBRACE, "{", startLine, startCol);
            case '}':
                advance();
                return new SigmaToken(TokenType.RBRACE, "}", startLine, startCol);
            case ';':
                advance();
                return new SigmaToken(TokenType.SEMI, ";", startLine, startCol);
            case ',':
                advance();
                return new SigmaToken(TokenType.COMMA, ",", startLine, startCol);
            case '.':
                advance();
                return new SigmaToken(TokenType.DOT, ".", startLine, startCol);

            default:
                advance();
                throw new LexerException("Unexpected character: '" + c + "'", startLine, startCol);
        }
    }

    // ========== Identifier and Keyword Handling ==========

    private SigmaToken handleIdentifier() {
        int startLine = line;
        int startCol = column;
        StringBuilder sb = new StringBuilder();

        // First char: LETTER or _
        sb.append(advance());

        // Continue: LETTER, DIGIT, or _
        while (!isAtEnd() && (isLetter(peek()) || isDigit(peek()) || peek() == '_')) {
            sb.append(advance());
        }

        String text = sb.toString();
        // Check if it's a keyword
        TokenType type = KEYWORDS.getOrDefault(text, TokenType.IDENTIFIER);

        return new SigmaToken(type, text, startLine, startCol);
    }

    // ========== Number Handling ==========

    private SigmaToken handleNumber() {
        int startLine = line;
        int startCol = column;
        StringBuilder sb = new StringBuilder();

        // Consume integer part
        while (!isAtEnd() && isDigit(peek())) {
            sb.append(advance());
        }

        // Check for FLOAT: need '.' followed by digit
        if (!isAtEnd() && peek() == '.') {
            char nextChar = peekNext();
            if (nextChar != '\0' && isDigit(nextChar)) {
                // It's a float
                sb.append(advance()); // consume '.'

                // Consume fractional digits
                while (!isAtEnd() && isDigit(peek())) {
                    sb.append(advance());
                }

                return new SigmaToken(TokenType.FLOAT_LITERAL, sb.toString(), startLine, startCol);
            }
        }

        // Otherwise it's an integer
        return new SigmaToken(TokenType.INTEGER, sb.toString(), startLine, startCol);
    }

    // ========== String Handling ==========

    private SigmaToken handleString() {
        int startLine = line;
        int startCol = column;

        StringBuilder sb = new StringBuilder();
        sb.append(advance()); // consume opening "

        while (!isAtEnd() && peek() != '"') {
            if (peek() == '\\') {
                sb.append(advance()); // append \
                if (isAtEnd()) {
                    throw new LexerException("Unterminated string", startLine, startCol);
                }
                char escChar = peek();
                if (!isEscChar(escChar)) {
                    throw new LexerException("Invalid escape sequence: \\" + escChar, line, column);
                }
                sb.append(advance()); // append escape char
            } else if (peek() == '\n' || peek() == '\r') {
                throw new LexerException("Unterminated string (newline in string)", line, column);
            } else {
                sb.append(advance());
            }
        }

        if (isAtEnd()) {
            throw new LexerException("Unterminated string", startLine, startCol);
        }

        sb.append(advance()); // consume closing "

        return new SigmaToken(TokenType.STRING, sb.toString(), startLine, startCol);
    }

    // ========== Slash Handling (/, //, /*) ==========

    private SigmaToken handleSlash() {
        int startLine = line;
        int startCol = column;

        advance(); // consume '/'

        if (!isAtEnd() && peek() == '/') {
            // Line comment - skip it
            skipLineComment();
            return null; // Indicate that token should be skipped
        } else if (!isAtEnd() && peek() == '*') {
            // Block comment - skip it
            skipBlockComment(startLine, startCol);
            return null; // Indicate that token should be skipped
        } else {
            // Just division operator
            return new SigmaToken(TokenType.DIV, "/", startLine, startCol);
        }
    }

    private void skipLineComment() {
        advance(); // consume second '/'

        // Continue until newline or EOF
        while (!isAtEnd() && peek() != '\n' && peek() != '\r') {
            advance();
        }
    }

    private void skipBlockComment(int startLine, int startCol) {
        advance(); // consume '*'

        // Continue until */
        while (!isAtEnd()) {
            if (peek() == '*' && peekNext() == '/') {
                advance(); // consume *
                advance(); // consume /
                return;
            }
            advance();
        }

        throw new LexerException("Unterminated block comment", startLine, startCol);
    }

    // ========== Operator Handlers with Lookahead ==========

    private SigmaToken handleLessOrLessEqual() {
        int startLine = line;
        int startCol = column;

        advance(); // consume '<'

        if (!isAtEnd() && peek() == '=') {
            advance(); // consume '='
            return new SigmaToken(TokenType.LE, "<=", startLine, startCol);
        }

        return new SigmaToken(TokenType.LT, "<", startLine, startCol);
    }

    private SigmaToken handleGreaterOrGreaterEqual() {
        int startLine = line;
        int startCol = column;

        advance(); // consume '>'

        if (!isAtEnd() && peek() == '=') {
            advance(); // consume '='
            return new SigmaToken(TokenType.GE, ">=", startLine, startCol);
        }

        return new SigmaToken(TokenType.GT, ">", startLine, startCol);
    }

    private SigmaToken handleAssignOrEqual() {
        int startLine = line;
        int startCol = column;

        advance(); // consume '='

        if (!isAtEnd() && peek() == '=') {
            advance(); // consume second '='
            return new SigmaToken(TokenType.EQ, "==", startLine, startCol);
        }

        return new SigmaToken(TokenType.ASSIGN, "=", startLine, startCol);
    }

    private SigmaToken handleNotOrNotEqual() {
        int startLine = line;
        int startCol = column;

        advance(); // consume '!'

        if (!isAtEnd() && peek() == '=') {
            advance(); // consume '='
            return new SigmaToken(TokenType.NE, "!=", startLine, startCol);
        }

        return new SigmaToken(TokenType.NOT, "!", startLine, startCol);
    }

    private SigmaToken handleAmpersand() {
        int startLine = line;
        int startCol = column;

        advance(); // consume '&'

        if (!isAtEnd() && peek() == '&') {
            advance(); // consume second '&'
            return new SigmaToken(TokenType.AND, "&&", startLine, startCol);
        }

        // Single '&' - create token, let parser handle error
        return new SigmaToken(TokenType.AMPERSAND, "&", startLine, startCol);
    }

    private SigmaToken handlePipe() {
        int startLine = line;
        int startCol = column;

        advance(); // consume '|'

        if (!isAtEnd() && peek() == '|') {
            advance(); // consume second '|'
            return new SigmaToken(TokenType.OR, "||", startLine, startCol);
        }

        // Single '|' - create token, let parser handle error
        return new SigmaToken(TokenType.PIPE, "|", startLine, startCol);
    }

    private SigmaToken handleMultOrPower() {
        int startLine = line;
        int startCol = column;

        advance(); // consume '*'

        if (!isAtEnd() && peek() == '*') {
            advance(); // consume second '*'
            return new SigmaToken(TokenType.POWER, "**", startLine, startCol);
        }

        return new SigmaToken(TokenType.MULT, "*", startLine, startCol);
    }

    // ========== Whitespace Handling ==========

    private void skipWhitespaceAndNewlines() {
        while (!isAtEnd()) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                advance();
            } else {
                break;
            }
        }
    }

    // ========== Character Position Management ==========

    private char advance() {
        if (isAtEnd()) return '\0';

        char c = source.charAt(position);
        position++;

        if (c == '\n') {
            line++;
            column = 0;
        } else {
            column++;
        }

        return c;
    }

    private char peek() {
        return isAtEnd() ? '\0' : source.charAt(position);
    }

    private char peekNext() {
        return (position + 1 >= source.length()) ? '\0' : source.charAt(position + 1);
    }

    private boolean isAtEnd() {
        return position >= source.length();
    }

    // ========== Character Class Checks (from gpt.jff) ==========

    private static boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isEscChar(char c) {
        // ESC_CHAR: n, t, r, ", \
        return c == 'n' || c == 't' || c == 'r' || c == '"' || c == '\\';
    }
}
