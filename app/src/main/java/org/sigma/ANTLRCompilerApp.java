package org.sigma;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.example.parser.SigmaBaseVisitor;
import org.example.parser.SigmaLexer;
import org.example.parser.SigmaParser;
import org.sigma.antlr.SigmaAstBuilder;
import org.sigma.parser.Ast;
import org.sigma.parser.ParseResult;
import org.sigma.semantics.SemanticAnalyzer;
import org.sigma.semantics.SemanticResult;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Minimal ANTLR-driven interpreter for the Sigma grammar.
 * Supports variable declarations, assignments, arithmetic/logical expressions,
 * if/else blocks, while loops, and built-in print/println calls.
 */
public final class ANTLRCompilerApp {
    private static final Path DEFAULT_SOURCE = Path.of("app/src/main/resources/source.groovy");

    public static void main(String[] args) throws IOException {
        Path source = args.length > 0 ? Path.of(args[0]) : DEFAULT_SOURCE;
        if (!Files.exists(source)) {
            System.err.println("Source file not found: " + source.toAbsolutePath());
            System.exit(2);
        }

        CharStream input = CharStreams.fromPath(source);
        SigmaLexer lexer = new SigmaLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SigmaParser parser = new SigmaParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new ThrowingErrorListener(source));

        SigmaParser.CompilationUnitContext tree = parser.compilationUnit();

        SigmaAstBuilder builder = new SigmaAstBuilder();
        Ast.CompilationUnit ast = builder.build(tree);

        ParseResult parseResult = ParseResult.success(ast);
        System.out.println("AST-TREE:");
        System.out.println(parseResult.getAstAsString());

        System.out.println("\n" + "=".repeat(70));
        System.out.println("SEMANTIC ANALYSIS");
        System.out.println("=".repeat(70));
        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        SemanticResult semanticResult = analyzer.analyze(ast);
        System.out.println(semanticResult.visualize());

        if (!semanticResult.isSuccessful()) {
            System.exit(1);
        }
    }

    private ANTLRCompilerApp() {
    }

    private static final class ThrowingErrorListener extends BaseErrorListener {
        private final Path source;

        private ThrowingErrorListener(Path source) {
            this.source = source;
        }

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer,
                                Object offendingSymbol,
                                int line,
                                int charPositionInLine,
                                String msg,
                                RecognitionException e) {
            String sourceName = source != null ? source.toAbsolutePath().toString() : "<input>";
            throw new ParseCancellationException(
                "Syntax error in " + sourceName + " at line " + line + ":" + charPositionInLine + " - " + msg);
        }
    }

    public static final class Value {
        enum Type {
            INT, DOUBLE, STRING, BOOL, NULL, VOID
        }

        private static final Value VOID = new Value(Type.VOID, null);
        private static final Value NULL = new Value(Type.NULL, null);
        private static final Value TRUE = new Value(Type.BOOL, true);
        private static final Value FALSE = new Value(Type.BOOL, false);

        private final Type type;
        private final Object raw;

        private Value(Type type, Object raw) {
            this.type = type;
            this.raw = raw;
        }

        static Value intValue(int value) {
            return new Value(Type.INT, value);
        }

        static Value doubleValue(double value) {
            return new Value(Type.DOUBLE, value);
        }

        static Value stringValue(String value) {
            return new Value(Type.STRING, value);
        }

        static Value bool(boolean value) {
            return value ? TRUE : FALSE;
        }

        boolean isDouble() {
            return type == Type.DOUBLE;
        }

        boolean isNumeric() {
            return type == Type.INT || type == Type.DOUBLE;
        }

        int asInt(ParserRuleContext ctx) {
            if (type == Type.INT) {
                return (Integer) raw;
            }
            if (type == Type.DOUBLE) {
                return ((Double) raw).intValue();
            }
            throw typeError("integer", ctx);
        }

        double asDouble(ParserRuleContext ctx) {
            if (type == Type.DOUBLE) {
                return (Double) raw;
            }
            if (type == Type.INT) {
                return ((Integer) raw).doubleValue();
            }
            throw typeError("numeric", ctx);
        }

        boolean asBoolean(ParserRuleContext ctx) {
            if (type == Type.BOOL) {
                return (Boolean) raw;
            }
            throw typeError("boolean", ctx);
        }

        String asString() {
            if (type == Type.STRING) {
                return (String) raw;
            }
            if (type == Type.INT) {
                return raw.toString();
            }
            if (type == Type.DOUBLE) {
                return raw.toString();
            }
            if (type == Type.BOOL) {
                return raw.toString();
            }
            return "null";
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Value)) {
                return false;
            }
            Value other = (Value) obj;
            if (type == Type.DOUBLE || other.type == Type.DOUBLE) {
                return Double.compare(asDoubleInternal(), other.asDoubleInternal()) == 0;
            }
            if (type == Type.INT && other.type == Type.INT) {
                return Objects.equals(raw, other.raw);
            }
            return type == other.type && Objects.equals(raw, other.raw);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, raw);
        }

        private double asDoubleInternal() {
            if (type == Type.DOUBLE) {
                return (Double) raw;
            }
            if (type == Type.INT) {
                return ((Integer) raw).doubleValue();
            }
            throw new IllegalStateException("Expected numeric value for comparison.");
        }

        private RuntimeException typeError(String expected, ParserRuleContext ctx) {
            if (ctx == null || ctx.getStart() == null) {
                return new RuntimeException("Expected " + expected + " value.");
            }
            Token token = ctx.getStart();
            return new RuntimeException(
                "Expected " + expected + " value at line " + token.getLine() + ":" + token.getCharPositionInLine());
        }
    }

    private static final class Variable {
        private Value value;
        private final boolean constant;

        private Variable(Value value, boolean constant) {
            this.value = value;
            this.constant = constant;
        }
    }
}
