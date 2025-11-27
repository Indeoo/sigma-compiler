package org.sigma.syntax.parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenSource;
import org.sigma.lexer.SigmaToken;
import org.sigma.lexer.TokenType;
import org.example.parser.SigmaParser;

import java.util.HashMap;
import java.util.Map;

/**
 * Adapter that wraps SigmaToken to implement ANTLR's Token interface.
 * This allows us to use our custom lexer with ANTLR's generated parser.
 */
public class TokenAdapter implements Token {
    private final SigmaToken sigmaToken;
    private final int antlrType;

    // Map our TokenType enum to ANTLR's generated token type constants
    private static final Map<TokenType, Integer> TOKEN_TYPE_MAP = new HashMap<>();

    static {
        // Keywords
        TOKEN_TYPE_MAP.put(TokenType.CLASS, SigmaParser.CLASS);
        TOKEN_TYPE_MAP.put(TokenType.IF, SigmaParser.IF);
        TOKEN_TYPE_MAP.put(TokenType.ELSE, SigmaParser.ELSE);
        TOKEN_TYPE_MAP.put(TokenType.WHILE, SigmaParser.WHILE);
        TOKEN_TYPE_MAP.put(TokenType.RETURN, SigmaParser.RETURN);
        TOKEN_TYPE_MAP.put(TokenType.FINAL, SigmaParser.FINAL);
        TOKEN_TYPE_MAP.put(TokenType.NULL, SigmaParser.NULL);

        // Primitive types - map to PRIMITIVE_TYPE token in grammar
        TOKEN_TYPE_MAP.put(TokenType.INT, SigmaParser.PRIMITIVE_TYPE);
        TOKEN_TYPE_MAP.put(TokenType.DOUBLE, SigmaParser.PRIMITIVE_TYPE);
        TOKEN_TYPE_MAP.put(TokenType.FLOAT, SigmaParser.PRIMITIVE_TYPE);
        TOKEN_TYPE_MAP.put(TokenType.BOOLEAN, SigmaParser.PRIMITIVE_TYPE);

        // Other types
        TOKEN_TYPE_MAP.put(TokenType.STRING_TYPE, SigmaParser.STRING_TYPE);
        TOKEN_TYPE_MAP.put(TokenType.VOID, SigmaParser.VOID);

        // Boolean literals - map to BOOLEAN token in grammar
        TOKEN_TYPE_MAP.put(TokenType.TRUE, SigmaParser.BOOLEAN);
        TOKEN_TYPE_MAP.put(TokenType.FALSE, SigmaParser.BOOLEAN);

        // Identifiers and literals
        TOKEN_TYPE_MAP.put(TokenType.IDENTIFIER, SigmaParser.IDENTIFIER);
        TOKEN_TYPE_MAP.put(TokenType.INTEGER, SigmaParser.INTEGER);
        TOKEN_TYPE_MAP.put(TokenType.FLOAT_LITERAL, SigmaParser.FLOAT);
        TOKEN_TYPE_MAP.put(TokenType.STRING, SigmaParser.STRING);

        // Operators - map to unified token types in grammar
        TOKEN_TYPE_MAP.put(TokenType.POWER, SigmaParser.POWER);
        TOKEN_TYPE_MAP.put(TokenType.MULT, SigmaParser.MULTIPLICATIVE);
        TOKEN_TYPE_MAP.put(TokenType.DIV, SigmaParser.MULTIPLICATIVE);
        TOKEN_TYPE_MAP.put(TokenType.MOD, SigmaParser.MULTIPLICATIVE);
        TOKEN_TYPE_MAP.put(TokenType.PLUS, SigmaParser.PLUS);
        TOKEN_TYPE_MAP.put(TokenType.MINUS, SigmaParser.MINUS);

        TOKEN_TYPE_MAP.put(TokenType.LT, SigmaParser.RELATIONAL);
        TOKEN_TYPE_MAP.put(TokenType.GT, SigmaParser.RELATIONAL);
        TOKEN_TYPE_MAP.put(TokenType.LE, SigmaParser.RELATIONAL);
        TOKEN_TYPE_MAP.put(TokenType.GE, SigmaParser.RELATIONAL);
        TOKEN_TYPE_MAP.put(TokenType.EQ, SigmaParser.RELATIONAL);
        TOKEN_TYPE_MAP.put(TokenType.NE, SigmaParser.RELATIONAL);

        TOKEN_TYPE_MAP.put(TokenType.AND, SigmaParser.LOGICAL);
        TOKEN_TYPE_MAP.put(TokenType.OR, SigmaParser.LOGICAL);
        TOKEN_TYPE_MAP.put(TokenType.NOT, SigmaParser.NOT);

        // Single & and | are invalid in Sigma (must use && and ||)
        TOKEN_TYPE_MAP.put(TokenType.AMPERSAND, Token.INVALID_TYPE);
        TOKEN_TYPE_MAP.put(TokenType.PIPE, Token.INVALID_TYPE);

        TOKEN_TYPE_MAP.put(TokenType.ASSIGN, SigmaParser.ASSIGN);

        // Delimiters
        TOKEN_TYPE_MAP.put(TokenType.LPAREN, SigmaParser.LPAREN);
        TOKEN_TYPE_MAP.put(TokenType.RPAREN, SigmaParser.RPAREN);
        TOKEN_TYPE_MAP.put(TokenType.LBRACE, SigmaParser.LBRACE);
        TOKEN_TYPE_MAP.put(TokenType.RBRACE, SigmaParser.RBRACE);
        TOKEN_TYPE_MAP.put(TokenType.SEMI, SigmaParser.SEMI);
        TOKEN_TYPE_MAP.put(TokenType.COMMA, SigmaParser.COMMA);
        TOKEN_TYPE_MAP.put(TokenType.DOT, SigmaParser.DOT);

        // EOF
        TOKEN_TYPE_MAP.put(TokenType.EOF, Token.EOF);
    }

    public TokenAdapter(SigmaToken sigmaToken) {
        this.sigmaToken = sigmaToken;

        // Check if token type is mapped
        Integer mappedType = TOKEN_TYPE_MAP.get(sigmaToken.getType());
        if (mappedType == null) {
            throw new IllegalArgumentException(
                "Unmapped token type: " + sigmaToken.getType() +
                " '" + sigmaToken.getText() + "' at line " + sigmaToken.getLine() +
                ", column " + sigmaToken.getColumn()
            );
        }

        this.antlrType = mappedType;
    }

    @Override
    public String getText() {
        return sigmaToken.getText();
    }

    @Override
    public int getType() {
        return antlrType;
    }

    @Override
    public int getLine() {
        return sigmaToken.getLine();
    }

    @Override
    public int getCharPositionInLine() {
        return sigmaToken.getColumn();
    }

    @Override
    public int getChannel() {
        return Token.DEFAULT_CHANNEL;
    }

    @Override
    public int getTokenIndex() {
        return -1;  // Not used in our case
    }

    @Override
    public int getStartIndex() {
        return -1;  // Not tracked by SigmaToken
    }

    @Override
    public int getStopIndex() {
        return -1;  // Not tracked by SigmaToken
    }

    @Override
    public TokenSource getTokenSource() {
        return null;  // Not needed for our use case
    }

    @Override
    public CharStream getInputStream() {
        return null;  // Not needed for our use case
    }
}
