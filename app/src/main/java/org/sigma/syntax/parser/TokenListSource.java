package org.sigma.syntax.parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenFactory;
import org.antlr.v4.runtime.TokenSource;

import java.util.List;

/**
 * TokenSource implementation that provides tokens from a pre-existing list.
 * Used to feed our custom SigmaTokens (wrapped as TokenAdapters) into ANTLR's parser.
 */
public class TokenListSource implements TokenSource {
    private final List<Token> tokens;
    private final String sourceName;
    private int index = 0;

    public TokenListSource(List<Token> tokens, String sourceName) {
        this.tokens = tokens;
        this.sourceName = sourceName;
    }

    @Override
    public Token nextToken() {
        if (index >= tokens.size()) {
            return tokens.get(tokens.size() - 1);  // Return EOF token
        }
        return tokens.get(index++);
    }

    @Override
    public int getLine() {
        if (index < tokens.size()) {
            return tokens.get(index).getLine();
        }
        return 0;
    }

    @Override
    public int getCharPositionInLine() {
        if (index < tokens.size()) {
            return tokens.get(index).getCharPositionInLine();
        }
        return 0;
    }

    @Override
    public CharStream getInputStream() {
        return null;  // Not needed
    }

    @Override
    public String getSourceName() {
        return sourceName;
    }

    @Override
    public void setTokenFactory(TokenFactory<?> factory) {
        // Not needed - we provide pre-made tokens
    }

    @Override
    public TokenFactory<?> getTokenFactory() {
        return null;  // Not needed
    }
}
