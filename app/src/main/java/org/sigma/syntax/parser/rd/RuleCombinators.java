package org.sigma.syntax.parser.rd;

import org.sigma.lexer.SigmaToken;
import org.sigma.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Utility class providing combinators for building complex grammar rules from simpler ones.
 * Implements common parsing patterns: sequence, choice, optional, repetition.
 */
public class RuleCombinators {

    /**
     * Creates a rule that matches a specific token type.
     */
    public static GrammarRule<SigmaToken> token(TokenType type) {
        return ctx -> ctx.match(type);
    }

    /**
     * Creates a rule that expects a specific token type (reports error if not found).
     */
    public static GrammarRule<SigmaToken> expect(TokenType type, String errorMsg) {
        return ctx -> ctx.expect(type, errorMsg);
    }

    /**
     * Creates a rule that matches one of several alternatives (ordered choice).
     * Tries each alternative in order, returns first successful match.
     */
    @SafeVarargs
    public static <T> GrammarRule<T> choice(GrammarRule<? extends T>... alternatives) {
        return ctx -> {
            for (GrammarRule<? extends T> alt : alternatives) {
                int savedPos = ctx.savePosition();
                T result = alt.parse(ctx);
                if (result != null) {
                    return result;
                }
                ctx.restorePosition(savedPos);
            }
            return null;
        };
    }

    /**
     * Creates a rule that makes another rule optional.
     * Always succeeds, returns null if the rule doesn't match.
     */
    public static <T> GrammarRule<T> optional(GrammarRule<T> rule) {
        return ctx -> {
            int savedPos = ctx.savePosition();
            T result = rule.parse(ctx);
            if (result == null) {
                ctx.restorePosition(savedPos);
            }
            return result; // Can be null
        };
    }

    /**
     * Creates a rule that repeats another rule zero or more times.
     * Always succeeds, returns empty list if no matches.
     */
    public static <T> GrammarRule<List<T>> zeroOrMore(GrammarRule<T> rule) {
        return ctx -> {
            List<T> results = new ArrayList<>();
            while (true) {
                int savedPos = ctx.savePosition();
                T result = rule.parse(ctx);
                if (result == null) {
                    ctx.restorePosition(savedPos);
                    break;
                }
                results.add(result);
            }
            return results;
        };
    }

    /**
     * Creates a rule that repeats another rule one or more times.
     * Fails if at least one match is not found.
     */
    public static <T> GrammarRule<List<T>> oneOrMore(GrammarRule<T> rule) {
        return ctx -> {
            List<T> results = new ArrayList<>();
            T first = rule.parse(ctx);
            if (first == null) {
                return null;
            }
            results.add(first);

            while (true) {
                int savedPos = ctx.savePosition();
                T result = rule.parse(ctx);
                if (result == null) {
                    ctx.restorePosition(savedPos);
                    break;
                }
                results.add(result);
            }
            return results;
        };
    }

    /**
     * Creates a rule for a comma-separated list: element (COMMA element)*
     */
    public static <T> GrammarRule<List<T>> commaSeparated(GrammarRule<T> element) {
        return ctx -> {
            List<T> results = new ArrayList<>();
            T first = element.parse(ctx);
            if (first == null) {
                return results; // Empty list
            }
            results.add(first);

            while (ctx.match(TokenType.COMMA) != null) {
                T next = element.parse(ctx);
                if (next == null) {
                    ctx.error("Expected element after comma");
                    return results;
                }
                results.add(next);
            }
            return results;
        };
    }

    /**
     * Creates a rule for a non-empty comma-separated list (requires at least one element).
     */
    public static <T> GrammarRule<List<T>> commaSeparatedNonEmpty(GrammarRule<T> element) {
        return ctx -> {
            List<T> results = new ArrayList<>();
            T first = element.parse(ctx);
            if (first == null) {
                return null; // Failed - no elements
            }
            results.add(first);

            while (ctx.match(TokenType.COMMA) != null) {
                T next = element.parse(ctx);
                if (next == null) {
                    ctx.error("Expected element after comma");
                    return results;
                }
                results.add(next);
            }
            return results;
        };
    }

    /**
     * Creates a rule that transforms the result of another rule.
     */
    public static <T, R> GrammarRule<R> map(GrammarRule<T> rule, Function<T, R> mapper) {
        return ctx -> {
            T result = rule.parse(ctx);
            if (result == null) {
                return null;
            }
            return mapper.apply(result);
        };
    }

    /**
     * Creates a rule that sequences two rules and combines their results.
     */
    public static <T1, T2, R> GrammarRule<R> seq(
            GrammarRule<T1> rule1,
            GrammarRule<T2> rule2,
            BiFunction<T1, T2, R> combiner) {
        return ctx -> {
            T1 result1 = rule1.parse(ctx);
            if (result1 == null) {
                return null;
            }
            T2 result2 = rule2.parse(ctx);
            if (result2 == null) {
                return null;
            }
            return combiner.apply(result1, result2);
        };
    }

    /**
     * Creates a rule that matches left-associative binary expressions.
     * Handles: operand (operator operand)*
     */
    public static <T> GrammarRule<T> leftAssociative(
            GrammarRule<T> operand,
            GrammarRule<SigmaToken> operator,
            TriFunction<T, SigmaToken, T, T> combiner) {
        return ctx -> {
            T left = operand.parse(ctx);
            if (left == null) {
                return null;
            }

            while (true) {
                int savedPos = ctx.savePosition();
                SigmaToken op = operator.parse(ctx);
                if (op == null) {
                    ctx.restorePosition(savedPos);
                    break;
                }

                T right = operand.parse(ctx);
                if (right == null) {
                    ctx.error("Expected operand after operator '" + op.getText() + "'");
                    return left;
                }

                left = combiner.apply(left, op, right);
            }

            return left;
        };
    }

    /**
     * Creates a rule that matches right-associative binary expressions.
     * Handles: operand (operator operand)?  with right recursion
     */
    public static <T> GrammarRule<T> rightAssociative(
            GrammarRule<T> operand,
            GrammarRule<SigmaToken> operator,
            TriFunction<T, SigmaToken, T, T> combiner) {
        return ctx -> {
            T left = operand.parse(ctx);
            if (left == null) {
                return null;
            }

            int savedPos = ctx.savePosition();
            SigmaToken op = operator.parse(ctx);
            if (op == null) {
                ctx.restorePosition(savedPos);
                return left;
            }

            // Recursive call for right associativity
            GrammarRule<T> recursiveRule = rightAssociative(operand, operator, combiner);
            T right = recursiveRule.parse(ctx);
            if (right == null) {
                ctx.error("Expected operand after operator '" + op.getText() + "'");
                return left;
            }

            return combiner.apply(left, op, right);
        };
    }

    /**
     * Functional interface for combining three values (used in binary expression parsing).
     */
    @FunctionalInterface
    public interface TriFunction<T1, T2, T3, R> {
        R apply(T1 t1, T2 t2, T3 t3);
    }
}
