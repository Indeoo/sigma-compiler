package org.sigma.syntax.parser.rd;

import org.sigma.lexer.SigmaToken;
import org.sigma.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;

/**
 * Context for recursive descent parsing.
 * Tracks current position in token stream, manages lookahead, and collects errors.
 */
public class ParserContext {
    private final List<SigmaToken> tokens;
    private int position;
    private final List<String> errors;

    public ParserContext(List<SigmaToken> tokens) {
        this.tokens = tokens;
        this.position = 0;
        this.errors = new ArrayList<>();
    }

    /**
     * Returns the current token without advancing.
     */
    public SigmaToken current() {
        if (position >= tokens.size()) {
            return tokens.get(tokens.size() - 1); // Return EOF token
        }
        return tokens.get(position);
    }

    /**
     * Returns the token at offset from current position without advancing.
     */
    public SigmaToken lookahead(int offset) {
        int pos = position + offset;
        if (pos >= tokens.size()) {
            return tokens.get(tokens.size() - 1); // Return EOF token
        }
        if (pos < 0) {
            return tokens.get(0);
        }
        return tokens.get(pos);
    }

    /**
     * Consumes and returns the current token, advancing position.
     */
    public SigmaToken consume() {
        SigmaToken token = current();
        if (position < tokens.size() - 1) { // Don't advance past EOF
            position++;
        }
        return token;
    }

    /**
     * Checks if current token matches the given type.
     */
    public boolean check(TokenType type) {
        return current().getType() == type;
    }

    /**
     * Checks if current token matches any of the given types.
     */
    public boolean check(TokenType... types) {
        TokenType currentType = current().getType();
        for (TokenType type : types) {
            if (currentType == type) {
                return true;
            }
        }
        return false;
    }

    /**
     * If current token matches type, consume and return it. Otherwise return null.
     */
    public SigmaToken match(TokenType type) {
        if (check(type)) {
            return consume();
        }
        return null;
    }

    /**
     * If current token matches any type, consume and return it. Otherwise return null.
     */
    public SigmaToken match(TokenType... types) {
        if (check(types)) {
            return consume();
        }
        return null;
    }

    /**
     * Expects the current token to be of given type. Consumes it or reports error.
     */
    public SigmaToken expect(TokenType type, String errorMessage) {
        if (check(type)) {
            return consume();
        }
        error(errorMessage);
        return null;
    }

    /**
     * Reports a parse error at current position.
     */
    public void error(String message) {
        SigmaToken token = current();
        errors.add(String.format("Line %d: %s", token.getLine(), message));
    }

    /**
     * Reports a parse error at specific token.
     */
    public void error(SigmaToken token, String message) {
        errors.add(String.format("Line %d: %s", token.getLine(), message));
    }

    /**
     * Saves current position for backtracking.
     */
    public int savePosition() {
        return position;
    }

    /**
     * Restores position for backtracking.
     */
    public void restorePosition(int savedPosition) {
        this.position = savedPosition;
    }

    /**
     * Checks if we're at end of input.
     */
    public boolean isAtEnd() {
        return current().getType() == TokenType.EOF;
    }

    /**
     * Returns all collected errors.
     */
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    /**
     * Checks if any errors were collected.
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Returns current position in token stream.
     */
    public int getPosition() {
        return position;
    }

    /**
     * Synchronizes parser to next statement boundary after error.
     * Advances until we find a semicolon, closing brace, or EOF.
     */
    public void synchronize() {
        while (!isAtEnd()) {
            TokenType type = current().getType();

            // Stop at statement terminators
            if (type == TokenType.SEMI || type == TokenType.RBRACE) {
                return;
            }

            // Stop before statement/declaration keywords
            if (type == TokenType.CLASS || type == TokenType.IF ||
                type == TokenType.WHILE || type == TokenType.RETURN ||
                type == TokenType.INT || type == TokenType.DOUBLE ||
                type == TokenType.FLOAT || type == TokenType.BOOLEAN ||
                type == TokenType.STRING_TYPE || type == TokenType.VOID ||
                type == TokenType.FINAL) {
                return;
            }

            consume();
        }
    }
}
