package org.sigma.lexer;

import java.util.Objects;

/**
 * Immutable representation of a lexical token in the Sigma language.
 * Contains the token type, lexeme (text), and source position information.
 */
public class SigmaToken {
    private final TokenType type;
    private final String text;
    private final int line;      // 1-indexed (ANTLR convention)
    private final int column;    // 0-indexed (ANTLR convention)

    /**
     * Creates a new token.
     *
     * @param type   the token type
     * @param text   the lexeme (actual text from source)
     * @param line   the 1-indexed line number
     * @param column the 0-indexed column position (character position in line)
     */
    public SigmaToken(TokenType type, String text, int line, int column) {
        this.type = Objects.requireNonNull(type, "Token type cannot be null");
        this.text = Objects.requireNonNull(text, "Token text cannot be null");
        this.line = line;
        this.column = column;
    }

    /**
     * Returns the token type.
     */
    public TokenType getType() {
        return type;
    }

    /**
     * Returns the lexeme (actual text from source).
     */
    public String getText() {
        return text;
    }

    /**
     * Returns the 1-indexed line number.
     */
    public int getLine() {
        return line;
    }

    /**
     * Returns the 0-indexed column position.
     * This method name matches ANTLR's convention.
     */
    public int getCharPositionInLine() {
        return column;
    }

    /**
     * Returns the 0-indexed column position.
     * Alias for getCharPositionInLine().
     */
    public int getColumn() {
        return column;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SigmaToken)) return false;
        SigmaToken other = (SigmaToken) obj;
        return type == other.type &&
               text.equals(other.text) &&
               line == other.line &&
               column == other.column;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, text, line, column);
    }

    @Override
    public String toString() {
        return String.format("SigmaToken{type=%s, text='%s', line=%d, column=%d}",
                type, text, line, column);
    }
}
