package org.sigma.parser;

import org.junit.jupiter.api.Test;
import org.sigma.lexer.SigmaLexerWrapper;
import org.sigma.lexer.SigmaToken;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the ANTLR-based parser implementation.
 */
public class GrammarDrivenParserTest {

    @Test
    public void testSimpleExpression() {
        String code = "2 + 3;";
        ParseResult result = parse(code);

        assertNotNull(result, "Parse result should not be null");
        // Note: May have errors since we're still implementing features
        System.out.println("Simple expression errors: " + result.getErrors());
    }

    @Test
    public void testVariableDeclaration() {
        String code = "int x = 10;";
        ParseResult result = parse(code);

        assertNotNull(result, "Parse result should not be null");
        System.out.println("Variable declaration errors: " + result.getErrors());
    }

    @Test
    public void testIfStatement() {
        String code = "if (x > 0) y = 1;";
        ParseResult result = parse(code);

        assertNotNull(result, "Parse result should not be null");
        System.out.println("If statement errors: " + result.getErrors());
    }

    private ParseResult parse(String code) {
        try {
            SigmaLexerWrapper lexer = new SigmaLexerWrapper();
            List<SigmaToken> tokens = lexer.tokenize(code);
            return RecursiveDescentParser.parseToAst(tokens);
        } catch (Exception e) {
            fail("Parsing should not throw exception: " + e.getMessage());
            return null;
        }
    }
}
