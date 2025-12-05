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

        SigmaInterpreter interpreter = new SigmaInterpreter(System.out);
        interpreter.visit(tree);
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

    public static final class SigmaInterpreter extends SigmaBaseVisitor<Value> {
        private final Deque<Map<String, Variable>> scopes = new ArrayDeque<>();
        private final PrintStream out;
        private boolean returning = false;
        private Value returnValue = Value.VOID;

        public SigmaInterpreter(PrintStream out) {
            this.out = out == null ? System.out : out;
            scopes.push(new LinkedHashMap<>());
        }

        public SigmaInterpreter() {
            this(System.out);
        }

        @Override
        public Value visitCompilationUnit(SigmaParser.CompilationUnitContext ctx) {
            if (ctx == null || ctx.children == null) {
                return Value.VOID;
            }
            for (ParseTree child : ctx.children) {
                visit(child);
                if (returning) {
                    break;
                }
            }
            return returnValue;
        }

        @Override
        public Value visitVariableDeclaration(SigmaParser.VariableDeclarationContext ctx) {
            String name = ctx.IDENTIFIER().getText();
            Value initial = ctx.expression() != null ? visit(ctx.expression()) : Value.NULL;
            defineVariable(name, initial, false, ctx);
            return Value.VOID;
        }

        @Override
        public Value visitConstantDeclaration(SigmaParser.ConstantDeclarationContext ctx) {
            String name = ctx.IDENTIFIER().getText();
            Value initial = visit(ctx.expression());
            defineVariable(name, initial, true, ctx);
            return Value.VOID;
        }

        @Override
        public Value visitAssignmentStatement(SigmaParser.AssignmentStatementContext ctx) {
            String name = ctx.IDENTIFIER().getText();
            Value value = visit(ctx.expression());
            assignVariable(name, value, ctx);
            return Value.VOID;
        }

        @Override
        public Value visitExpressionStatement(SigmaParser.ExpressionStatementContext ctx) {
            if (ctx.expression() != null) {
                visit(ctx.expression());
            }
            return Value.VOID;
        }

        @Override
        public Value visitBlock(SigmaParser.BlockContext ctx) {
            enterScope();
            if (ctx.children != null) {
                for (ParseTree child : ctx.children) {
                    if (child instanceof TerminalNode) {
                        continue;
                    }
                    visit(child);
                    if (returning) {
                        break;
                    }
                }
            }
            exitScope();
            return Value.VOID;
        }

        @Override
        public Value visitIfStatement(SigmaParser.IfStatementContext ctx) {
            Value cond = visit(ctx.ifCondition());
            if (cond.asBoolean(ctx)) {
                visit(ctx.statement(0));
            } else if (ctx.statement().size() > 1) {
                visit(ctx.statement(1));
            }
            return Value.VOID;
        }

        @Override
        public Value visitIfCondition(SigmaParser.IfConditionContext ctx) {
            return visit(ctx.expression());
        }

        @Override
        public Value visitWhileStatement(SigmaParser.WhileStatementContext ctx) {
            while (visit(ctx.expression()).asBoolean(ctx)) {
                visit(ctx.statement());
                if (returning) {
                    break;
                }
            }
            return Value.VOID;
        }

        @Override
        public Value visitReturnStatement(SigmaParser.ReturnStatementContext ctx) {
            returnValue = ctx.expression() != null ? visit(ctx.expression()) : Value.VOID;
            returning = true;
            return returnValue;
        }

        @Override
        public Value visitForStatement(SigmaParser.ForStatementContext ctx) {
            throw runtimeError(ctx, "for/in loops are not supported by the ANTLR interpreter.");
        }

        @Override
        public Value visitMethodDeclaration(SigmaParser.MethodDeclarationContext ctx) {
            // Methods are ignored by the simple interpreter (no user-defined functions yet)
            return Value.VOID;
        }

        @Override
        public Value visitClassDeclaration(SigmaParser.ClassDeclarationContext ctx) {
            // Classes are ignored (interpreter executes script-level statements only)
            return Value.VOID;
        }

        @Override
        public Value visitExpression(SigmaParser.ExpressionContext ctx) {
            return visit(ctx.logicalOrExpression());
        }

        @Override
        public Value visitLogicalOrExpression(SigmaParser.LogicalOrExpressionContext ctx) {
            Value result = visit(ctx.logicalAndExpression(0));
            for (int i = 1; i < ctx.logicalAndExpression().size(); i++) {
                String op = ctx.getChild(2 * i - 1).getText();
                Value right = visit(ctx.logicalAndExpression(i));
                result = applyLogical(op, result, right, ctx);
            }
            return result;
        }

        @Override
        public Value visitLogicalAndExpression(SigmaParser.LogicalAndExpressionContext ctx) {
            Value result = visit(ctx.relationalExpression(0));
            for (int i = 1; i < ctx.relationalExpression().size(); i++) {
                String op = ctx.getChild(2 * i - 1).getText();
                Value right = visit(ctx.relationalExpression(i));
                result = applyLogical(op, result, right, ctx);
            }
            return result;
        }

        @Override
        public Value visitRelationalExpression(SigmaParser.RelationalExpressionContext ctx) {
            Value result = visit(ctx.additiveExpression(0));
            for (int i = 1; i < ctx.additiveExpression().size(); i++) {
                String op = ctx.getChild(2 * i - 1).getText();
                Value right = visit(ctx.additiveExpression(i));
                result = applyRelational(op, result, right, ctx);
            }
            return result;
        }

        @Override
        public Value visitAdditiveExpression(SigmaParser.AdditiveExpressionContext ctx) {
            Value result = visit(ctx.multiplicativeExpression(0));
            for (int i = 1; i < ctx.multiplicativeExpression().size(); i++) {
                String op = ctx.getChild(2 * i - 1).getText();
                Value right = visit(ctx.multiplicativeExpression(i));
                result = applyAdditive(op, result, right, ctx);
            }
            return result;
        }

        @Override
        public Value visitMultiplicativeExpression(SigmaParser.MultiplicativeExpressionContext ctx) {
            Value result = visit(ctx.powerExpression(0));
            for (int i = 1; i < ctx.powerExpression().size(); i++) {
                String op = ctx.getChild(2 * i - 1).getText();
                Value right = visit(ctx.powerExpression(i));
                result = applyMultiplicative(op, result, right, ctx);
            }
            return result;
        }

        @Override
        public Value visitPowerExpression(SigmaParser.PowerExpressionContext ctx) {
            Value base = visit(ctx.unaryExpression());
            if (ctx.powerExpression() != null) {
                Value exponent = visit(ctx.powerExpression());
                return pow(base, exponent, ctx);
            }
            return base;
        }

        @Override
        public Value visitUnaryExpression(SigmaParser.UnaryExpressionContext ctx) {
            if (ctx.NOT() != null) {
                Value value = visit(ctx.unaryExpression());
                return Value.bool(!value.asBoolean(ctx));
            }
            if (ctx.MINUS() != null) {
                Value value = visit(ctx.unaryExpression());
                return negate(value, ctx);
            }
            return visit(ctx.postfixExpression());
        }

        @Override
        public Value visitPostfixExpression(SigmaParser.PostfixExpressionContext ctx) {
            if (ctx.postfixOp().isEmpty()) {
                return visit(ctx.primaryExpression());
            }

            SigmaParser.PostfixOpContext op = ctx.postfixOp(0);
            if (op.LPAREN() != null) {
                String functionName = ctx.primaryExpression().getText();
                List<Value> args = evaluateArguments(op.argumentList());
                if (ctx.postfixOp().size() > 1) {
                    throw runtimeError(ctx, "Chained calls are not supported in the interpreter.");
                }
                return invokeFunction(functionName, args, ctx);
            }

            throw runtimeError(ctx, "Member access is not supported in the interpreter.");
        }

        @Override
        public Value visitPrimaryExpression(SigmaParser.PrimaryExpressionContext ctx) {
            if (ctx.IDENTIFIER() != null) {
                return resolveVariable(ctx.IDENTIFIER().getText(), ctx);
            }
            if (ctx.literal() != null) {
                return visit(ctx.literal());
            }
            if (ctx.expression() != null) {
                return visit(ctx.expression());
            }
            if (ctx.getChildCount() > 0 && ctx.getChild(0).getText().equals("new")) {
                throw runtimeError(ctx, "Object creation is not supported by the interpreter.");
            }
            return Value.VOID;
        }

        @Override
        public Value visitLiteral(SigmaParser.LiteralContext ctx) {
            if (ctx.INTEGER() != null) {
                return Value.intValue(Integer.parseInt(ctx.INTEGER().getText()));
            }
            if (ctx.FLOAT() != null) {
                return Value.doubleValue(Double.parseDouble(ctx.FLOAT().getText()));
            }
            if (ctx.STRING() != null) {
                return Value.stringValue(decodeString(ctx.STRING().getText()));
            }
            if (ctx.BOOLEAN() != null) {
                return Value.bool(Boolean.parseBoolean(ctx.BOOLEAN().getText()));
            }
            return Value.NULL;
        }

        private List<Value> evaluateArguments(SigmaParser.ArgumentListContext ctx) {
            List<Value> values = new ArrayList<>();
            if (ctx == null || ctx.expression() == null) {
                return values;
            }
            for (SigmaParser.ExpressionContext exprCtx : ctx.expression()) {
                values.add(visit(exprCtx));
            }
            return values;
        }

        private Value invokeFunction(String name, List<Value> args, ParserRuleContext ctx) {
            if ("print".equals(name) || "println".equals(name)) {
                String output = args.isEmpty() ? "" : args.get(0).asString();
                out.println(output);
                return Value.VOID;
            }
            throw runtimeError(ctx, "Function '" + name + "' is not supported by the interpreter.");
        }

        private Value applyLogical(String symbol, Value left, Value right, ParserRuleContext ctx) {
            switch (symbol) {
                case "&&":
                    return Value.bool(left.asBoolean(ctx) && right.asBoolean(ctx));
                case "||":
                    return Value.bool(left.asBoolean(ctx) || right.asBoolean(ctx));
                default:
                    throw runtimeError(ctx, "Unsupported logical operator: " + symbol);
            }
        }

        private Value applyRelational(String symbol, Value left, Value right, ParserRuleContext ctx) {
            switch (symbol) {
                case "==":
                    return Value.bool(left.equals(right));
                case "!=":
                    return Value.bool(!left.equals(right));
                case "<":
                case "<=":
                case ">":
                case ">=":
                    return compareNumbers(symbol, left, right, ctx);
                default:
                    throw runtimeError(ctx, "Unsupported relational operator: " + symbol);
            }
        }

        private Value compareNumbers(String symbol, Value left, Value right, ParserRuleContext ctx) {
            double l = left.asDouble(ctx);
            double r = right.asDouble(ctx);
            boolean result;
            switch (symbol) {
                case "<":
                    result = l < r;
                    break;
                case "<=":
                    result = l <= r;
                    break;
                case ">":
                    result = l > r;
                    break;
                case ">=":
                    result = l >= r;
                    break;
                default:
                    throw runtimeError(ctx, "Unsupported comparison operator: " + symbol);
            }
            return Value.bool(result);
        }

        private Value applyAdditive(String symbol, Value left, Value right, ParserRuleContext ctx) {
            if ("+".equals(symbol)) {
                if (left.type == Value.Type.STRING || right.type == Value.Type.STRING) {
                    return Value.stringValue(left.asString() + right.asString());
                }
                if (left.isDouble() || right.isDouble()) {
                    return Value.doubleValue(left.asDouble(ctx) + right.asDouble(ctx));
                }
                return Value.intValue(left.asInt(ctx) + right.asInt(ctx));
            } else if ("-".equals(symbol)) {
                if (left.isDouble() || right.isDouble()) {
                    return Value.doubleValue(left.asDouble(ctx) - right.asDouble(ctx));
                }
                return Value.intValue(left.asInt(ctx) - right.asInt(ctx));
            }
            throw runtimeError(ctx, "Unsupported additive operator: " + symbol);
        }

        private Value applyMultiplicative(String symbol, Value left, Value right, ParserRuleContext ctx) {
            switch (symbol) {
                case "*":
                    if (left.isDouble() || right.isDouble()) {
                        return Value.doubleValue(left.asDouble(ctx) * right.asDouble(ctx));
                    }
                    return Value.intValue(left.asInt(ctx) * right.asInt(ctx));
                case "/":
                    return Value.doubleValue(left.asDouble(ctx) / right.asDouble(ctx));
                case "%":
                    return Value.intValue(left.asInt(ctx) % right.asInt(ctx));
                default:
                    throw runtimeError(ctx, "Unsupported multiplicative operator: " + symbol);
            }
        }

        private Value pow(Value base, Value exponent, ParserRuleContext ctx) {
            double result = Math.pow(base.asDouble(ctx), exponent.asDouble(ctx));
            return Value.doubleValue(result);
        }

        private Value negate(Value value, ParserRuleContext ctx) {
            if (value.isDouble()) {
                return Value.doubleValue(-value.asDouble(ctx));
            }
            if (value.type == Value.Type.INT) {
                return Value.intValue(-value.asInt(ctx));
            }
            throw runtimeError(ctx, "Unary minus requires numeric operand.");
        }

        private void defineVariable(String name, Value value, boolean constant, ParserRuleContext ctx) {
            Map<String, Variable> current = scopes.peek();
            if (current.containsKey(name)) {
                throw runtimeError(ctx, "Variable '" + name + "' is already defined in this scope.");
            }
            current.put(name, new Variable(value, constant));
        }

        private void assignVariable(String name, Value value, ParserRuleContext ctx) {
            Variable variable = resolveRawVariable(name);
            if (variable == null) {
                throw runtimeError(ctx, "Variable '" + name + "' is not defined.");
            }
            if (variable.constant) {
                throw runtimeError(ctx, "Cannot reassign constant '" + name + "'.");
            }
            variable.value = value;
        }

        private Value resolveVariable(String name, ParserRuleContext ctx) {
            Variable variable = resolveRawVariable(name);
            if (variable == null) {
                throw runtimeError(ctx, "Variable '" + name + "' is not defined.");
            }
            return variable.value;
        }

        private Variable resolveRawVariable(String name) {
            for (Map<String, Variable> scope : scopes) {
                if (scope.containsKey(name)) {
                    return scope.get(name);
                }
            }
            return null;
        }

        private void enterScope() {
            scopes.push(new LinkedHashMap<>());
        }

        private void exitScope() {
            scopes.pop();
        }

        private RuntimeException runtimeError(ParserRuleContext ctx, String message) {
            Token token = ctx.getStart();
            String location = token != null
                ? " (line " + token.getLine() + ":" + token.getCharPositionInLine() + ")"
                : "";
            return new RuntimeException(message + location);
        }

        private String decodeString(String raw) {
            if (raw == null || raw.length() < 2) {
                return "";
            }
            String inner = raw.substring(1, raw.length() - 1);
            StringBuilder sb = new StringBuilder(inner.length());
            for (int i = 0; i < inner.length(); i++) {
                char c = inner.charAt(i);
                if (c == '\\' && i + 1 < inner.length()) {
                    char next = inner.charAt(++i);
                    switch (next) {
                        case 'n':
                            sb.append('\n');
                            break;
                        case 't':
                            sb.append('\t');
                            break;
                        case 'r':
                            sb.append('\r');
                            break;
                        case '"':
                            sb.append('"');
                            break;
                        case '\\':
                            sb.append('\\');
                            break;
                        default:
                            sb.append(next);
                            break;
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        /**
         * Exposes variable value for tests.
         */
        public Value getVariableValue(String name) {
            Variable variable = resolveRawVariable(name);
            return variable != null ? variable.value : null;
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
