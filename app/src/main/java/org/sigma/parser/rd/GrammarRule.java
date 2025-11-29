package org.sigma.parser.rd;

/**
 * Functional interface representing a grammar rule in the recursive descent parser.
 * A grammar rule attempts to parse input and returns the result.
 *
 * @param <T> the type of AST node this rule produces
 */
@FunctionalInterface
public interface GrammarRule<T> {
    /**
     * Attempts to parse using this rule.
     *
     * @param ctx the parser context
     * @return the parsed result, or null if parsing failed
     */
    T parse(ParserContext ctx);
}
