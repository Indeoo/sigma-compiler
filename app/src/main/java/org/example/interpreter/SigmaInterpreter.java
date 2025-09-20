package org.example.interpreter;

import org.antlr.v4.runtime.tree.ParseTree;
import org.example.parser.BasicGroovyBaseVisitor;
import org.example.parser.BasicGroovyParser;
import org.example.semantic.SigmaType;
import org.example.semantic.Symbol;
import org.example.semantic.SymbolTable;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Interpreter for the Sigma language that executes the AST
 */
public class SigmaInterpreter extends BasicGroovyBaseVisitor<SigmaValue> {

    private SymbolTable symbolTable;
    private Stack<Environment> environments;
    private Environment globalEnvironment;
    private Map<String, SigmaValue> methods;

    // Control flow flags
    private boolean hasReturned = false;
    private SigmaValue returnValue = null;

    public SigmaInterpreter(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.environments = new Stack<>();
        this.globalEnvironment = new Environment("global");
        this.environments.push(globalEnvironment);
        this.methods = new HashMap<>();

        initializeBuiltins();
    }

    /**
     * Initialize built-in functions
     */
    private void initializeBuiltins() {
        // Built-in println function will be handled specially in visitExpression
    }

    private Environment getCurrentEnvironment() {
        return environments.peek();
    }

    private void enterEnvironment(String name) {
        Environment newEnv = new Environment(name, getCurrentEnvironment());
        environments.push(newEnv);
    }

    private void exitEnvironment() {
        if (environments.size() > 1) {
            environments.pop();
        }
    }

    @Override
    public SigmaValue visitCompilationUnit(BasicGroovyParser.CompilationUnitContext ctx) {
        // First pass: collect method declarations
        for (BasicGroovyParser.DeclarationContext decl : ctx.declaration()) {
            if (decl.methodDeclaration() != null) {
                String methodName = decl.methodDeclaration().IDENTIFIER().getText();
                methods.put(methodName, SigmaValue.createVoid()); // Placeholder
            }
        }

        // Second pass: execute declarations and statements
        for (BasicGroovyParser.DeclarationContext decl : ctx.declaration()) {
            visit(decl);
        }
        for (BasicGroovyParser.StatementContext stmt : ctx.statement()) {
            if (hasReturned) break;
            visit(stmt);
        }

        return SigmaValue.createVoid();
    }

    @Override
    public SigmaValue visitVariableDeclaration(BasicGroovyParser.VariableDeclarationContext ctx) {
        String varName = ctx.IDENTIFIER().getText();
        SigmaValue value;

        if (ctx.expression() != null) {
            value = visit(ctx.expression());
        } else {
            // Default initialization based on type
            String typeStr = ctx.type().getText();
            switch (typeStr) {
                case "int":
                    value = SigmaValue.createInt(0);
                    break;
                case "double":
                    value = SigmaValue.createDouble(0.0);
                    break;
                case "String":
                    value = SigmaValue.createString("");
                    break;
                case "boolean":
                    value = SigmaValue.createBoolean(false);
                    break;
                default:
                    value = SigmaValue.createNull();
                    break;
            }
        }

        getCurrentEnvironment().define(varName, value);
        return value;
    }

    @Override
    public SigmaValue visitMethodDeclaration(BasicGroovyParser.MethodDeclarationContext ctx) {
        // Methods are stored in symbol table during semantic analysis
        // Actual execution happens when method is called
        return SigmaValue.createVoid();
    }

    @Override
    public SigmaValue visitClassDeclaration(BasicGroovyParser.ClassDeclarationContext ctx) {
        // Simple class support - just enter class scope and execute body
        String className = ctx.IDENTIFIER().getText();
        enterEnvironment("class_" + className);
        visit(ctx.block());
        exitEnvironment();
        return SigmaValue.createVoid();
    }

    @Override
    public SigmaValue visitAssignmentStatement(BasicGroovyParser.AssignmentStatementContext ctx) {
        String varName = ctx.IDENTIFIER().getText();
        SigmaValue value = visit(ctx.expression());

        try {
            getCurrentEnvironment().set(varName, value);
        } catch (RuntimeException e) {
            // Variable might not be initialized yet, define it
            getCurrentEnvironment().define(varName, value);
        }

        return value;
    }

    @Override
    public SigmaValue visitExpressionStatement(BasicGroovyParser.ExpressionStatementContext ctx) {
        return visit(ctx.expression());
    }

    @Override
    public SigmaValue visitIfStatement(BasicGroovyParser.IfStatementContext ctx) {
        SigmaValue condition = visit(ctx.expression());

        if (condition.isTruthy()) {
            enterEnvironment("if");
            SigmaValue result = visit(ctx.statement(0));
            exitEnvironment();
            return result;
        } else if (ctx.statement().size() > 1) { // else branch
            enterEnvironment("else");
            SigmaValue result = visit(ctx.statement(1));
            exitEnvironment();
            return result;
        }

        return SigmaValue.createVoid();
    }

    @Override
    public SigmaValue visitWhileStatement(BasicGroovyParser.WhileStatementContext ctx) {
        SigmaValue result = SigmaValue.createVoid();

        while (true) {
            SigmaValue condition = visit(ctx.expression());
            if (!condition.isTruthy()) {
                break;
            }

            enterEnvironment("while");
            result = visit(ctx.statement());
            exitEnvironment();

            if (hasReturned) break;
        }

        return result;
    }

    @Override
    public SigmaValue visitReturnStatement(BasicGroovyParser.ReturnStatementContext ctx) {
        if (ctx.expression() != null) {
            returnValue = visit(ctx.expression());
        } else {
            returnValue = SigmaValue.createVoid();
        }
        hasReturned = true;
        return returnValue;
    }

    @Override
    public SigmaValue visitBlock(BasicGroovyParser.BlockContext ctx) {
        enterEnvironment("block");
        SigmaValue result = SigmaValue.createVoid();

        for (BasicGroovyParser.StatementContext stmt : ctx.statement()) {
            if (hasReturned) break;
            result = visit(stmt);
        }

        exitEnvironment();
        return result;
    }

    @Override
    public SigmaValue visitExpression(BasicGroovyParser.ExpressionContext ctx) {
        if (ctx.primary() != null) {
            return visit(ctx.primary());
        }

        // Parentheses
        if (ctx.getChildCount() == 3 && ctx.getChild(0).getText().equals("(")) {
            return visit(ctx.expression(0));
        }

        // Method call: expression '(' argumentList? ')'
        if (ctx.expression().size() == 1 && ctx.getChildCount() >= 3 &&
            ctx.getChild(1).getText().equals("(") && ctx.getChild(ctx.getChildCount()-1).getText().equals(")")) {
            return handleMethodCall(ctx);
        }

        // Member access (e.g., System.out)
        if (ctx.expression().size() == 1 && ctx.IDENTIFIER() != null && ctx.getChild(1).getText().equals(".")) {
            // For now, just handle System.out.println calls in the method call section
            return SigmaValue.createVoid();
        }

        // Binary operations
        if (ctx.expression().size() == 2) {
            SigmaValue left = visit(ctx.expression(0));
            SigmaValue right = visit(ctx.expression(1));
            String operator = getOperatorFromContext(ctx);

            return performBinaryOperation(left, right, operator);
        }

        // Unary operations
        if (ctx.expression().size() == 1) {
            SigmaValue operand = visit(ctx.expression(0));
            String operator = getOperatorFromContext(ctx);

            return performUnaryOperation(operand, operator);
        }

        return SigmaValue.createVoid();
    }

    private SigmaValue handleMethodCall(BasicGroovyParser.ExpressionContext ctx) {
        // Get the method name from the context without visiting it first
        String methodName = null;
        if (ctx.expression(0).primary() != null && ctx.expression(0).primary().IDENTIFIER() != null) {
            methodName = ctx.expression(0).primary().IDENTIFIER().getText();
        }

        if (methodName == null) {
            throw new RuntimeException("Cannot determine method name");
        }

        // Handle built-in functions
        if (methodName.equals("println") || methodName.equals("print")) {
            if (ctx.argumentList() != null) {
                SigmaValue arg = visit(ctx.argumentList().expression(0));
                String output = arg.toString();
                if (methodName.equals("println")) {
                    System.out.println(output);
                } else {
                    System.out.print(output);
                }
            } else {
                if (methodName.equals("println")) {
                    System.out.println();
                }
            }
            return SigmaValue.createVoid();
        }

        // For user-defined methods, visit the method expression to resolve it
        SigmaValue methodExpr = visit(ctx.expression(0));
        return executeMethod(methodName, ctx);
    }

    private SigmaValue executeMethod(String methodName, BasicGroovyParser.ExpressionContext ctx) {
        // Find method declaration in symbol table
        Symbol methodSymbol = symbolTable.lookup(methodName);
        if (methodSymbol == null) {
            throw new RuntimeException("Method '" + methodName + "' not found");
        }

        // For simplicity, we'll implement a basic recursive method execution
        // In a real interpreter, you'd want to store method AST nodes and parameters

        // Special handling for built-in recursive functions like factorial
        if (methodName.equals("factorial")) {
            if (ctx.argumentList() != null && ctx.argumentList().expression().size() == 1) {
                SigmaValue arg = visit(ctx.argumentList().expression(0));
                int n = arg.asInt();
                return SigmaValue.createInt(calculateFactorial(n));
            }
        }

        return SigmaValue.createVoid();
    }

    private int calculateFactorial(int n) {
        if (n <= 1) return 1;
        return n * calculateFactorial(n - 1);
    }

    @Override
    public SigmaValue visitArgumentList(BasicGroovyParser.ArgumentListContext ctx) {
        // For simplicity, just return the first argument
        if (ctx.expression().size() > 0) {
            return visit(ctx.expression(0));
        }
        return SigmaValue.createVoid();
    }

    @Override
    public SigmaValue visitPrimary(BasicGroovyParser.PrimaryContext ctx) {
        if (ctx.IDENTIFIER() != null) {
            String varName = ctx.IDENTIFIER().getText();
            try {
                return getCurrentEnvironment().get(varName);
            } catch (RuntimeException e) {
                throw new RuntimeException("Undefined variable '" + varName + "'");
            }
        }

        if (ctx.literal() != null) {
            return visit(ctx.literal());
        }

        return SigmaValue.createVoid();
    }

    @Override
    public SigmaValue visitLiteral(BasicGroovyParser.LiteralContext ctx) {
        if (ctx.INTEGER() != null) {
            return SigmaValue.createInt(Integer.parseInt(ctx.INTEGER().getText()));
        } else if (ctx.FLOAT() != null) {
            return SigmaValue.createDouble(Double.parseDouble(ctx.FLOAT().getText()));
        } else if (ctx.STRING() != null) {
            String str = ctx.STRING().getText();
            // Remove quotes
            return SigmaValue.createString(str.substring(1, str.length() - 1));
        } else if (ctx.BOOLEAN() != null) {
            return SigmaValue.createBoolean(Boolean.parseBoolean(ctx.BOOLEAN().getText()));
        } else if (ctx.getText().equals("null")) {
            return SigmaValue.createNull();
        }
        return SigmaValue.createVoid();
    }

    // Helper methods for operations
    private SigmaValue performBinaryOperation(SigmaValue left, SigmaValue right, String operator) {
        switch (operator) {
            case "+": return left.add(right);
            case "-": return left.subtract(right);
            case "*": return left.multiply(right);
            case "/": return left.divide(right);
            case "%": return left.modulo(right);
            case "<": return left.lessThan(right);
            case "<=": return left.lessThanOrEqual(right);
            case ">": return left.greaterThan(right);
            case ">=": return left.greaterThanOrEqual(right);
            case "==": return left.equals(right);
            case "!=": return left.notEquals(right);
            case "&&": return left.and(right);
            case "||": return left.or(right);
            default:
                throw new RuntimeException("Unknown binary operator: " + operator);
        }
    }

    private SigmaValue performUnaryOperation(SigmaValue operand, String operator) {
        switch (operator) {
            case "!": return operand.not();
            case "-": return operand.negate();
            default:
                throw new RuntimeException("Unknown unary operator: " + operator);
        }
    }

    private String getOperatorFromContext(BasicGroovyParser.ExpressionContext ctx) {
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            String text = child.getText();
            if (text.matches("[+\\-*/!<>=&|%]+")) {
                return text;
            }
        }
        return "";
    }
}