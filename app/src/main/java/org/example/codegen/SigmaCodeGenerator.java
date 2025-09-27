package org.example.codegen;

import org.antlr.v4.runtime.tree.ParseTree;
import org.example.parser.SigmaBaseVisitor;
import org.example.parser.SigmaParser;
import org.example.semantic.SymbolTable;
import org.example.semantic.Symbol;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

/**
 * Code generator that produces JVM bytecode from a Sigma AST.
 * Uses ASM library to generate executable Java bytecode.
 */
public class SigmaCodeGenerator extends SigmaBaseVisitor<Void> {

    private final SymbolTable symbolTable;
    private final String className;
    private ClassWriter classWriter;
    private GeneratorAdapter mainMethod;
    private final List<String> errors;

    // Variable tracking for local variable slots
    private final Map<String, Integer> variableSlots;
    private final Map<String, org.example.semantic.SigmaType> variableTypes;
    private int nextLocalVarSlot;

    public SigmaCodeGenerator(SymbolTable symbolTable, String className) {
        this.symbolTable = symbolTable;
        this.className = className;
        this.errors = new ArrayList<>();
        this.variableSlots = new HashMap<>();
        this.variableTypes = new HashMap<>();
        this.nextLocalVarSlot = 1; // Slot 0 reserved for 'this' in non-static methods, but we're using static main
    }

    /**
     * Generate bytecode for the given parse tree
     * @param parseTree the AST to compile
     * @return the compiled bytecode as byte array, or null if compilation failed
     */
    public byte[] generateBytecode(ParseTree parseTree) {
        try {
            // Initialize class writer
            classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

            // Define the class
            classWriter.visit(V11, ACC_PUBLIC, className, null, "java/lang/Object", null);

            // Generate default constructor
            generateDefaultConstructor();

            // Generate main method
            generateMainMethod();

            // Visit the parse tree to generate code in main method
            visit(parseTree);

            // End main method
            mainMethod.returnValue();
            mainMethod.endMethod();

            // End class
            classWriter.visitEnd();

            return classWriter.toByteArray();

        } catch (Exception e) {
            errors.add("Code generation error: " + e.getMessage());
            return null;
        }
    }

    private void generateDefaultConstructor() {
        MethodVisitor constructor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(ALOAD, 0);
        constructor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitInsn(RETURN);
        constructor.visitMaxs(1, 1);
        constructor.visitEnd();
    }

    private void generateMainMethod() {
        Method mainMethodSignature = Method.getMethod("void main (String[])");
        mainMethod = new GeneratorAdapter(ACC_PUBLIC | ACC_STATIC, mainMethodSignature, null, null, classWriter);
        mainMethod.visitCode();
    }

    @Override
    public Void visitCompilationUnit(SigmaParser.CompilationUnitContext ctx) {
        // Visit all declarations and statements in the compilation unit
        for (SigmaParser.DeclarationContext decl : ctx.declaration()) {
            visit(decl);
        }
        for (SigmaParser.StatementContext stmt : ctx.statement()) {
            visit(stmt);
        }
        return null;
    }

    @Override
    public Void visitStatement(SigmaParser.StatementContext ctx) {
        if (ctx.assignmentStatement() != null) {
            visit(ctx.assignmentStatement());
        } else if (ctx.expressionStatement() != null) {
            visit(ctx.expressionStatement());
        } else if (ctx.ifStatement() != null) {
            visit(ctx.ifStatement());
        } else if (ctx.whileStatement() != null) {
            visit(ctx.whileStatement());
        } else if (ctx.returnStatement() != null) {
            visit(ctx.returnStatement());
        } else if (ctx.block() != null) {
            visit(ctx.block());
        }
        return null;
    }

    @Override
    public Void visitVariableDeclaration(SigmaParser.VariableDeclarationContext ctx) {
        String varName = ctx.IDENTIFIER().getText();

        // Allocate a local variable slot for this variable
        int slot = nextLocalVarSlot++;
        variableSlots.put(varName, slot);

        // Record declared type (if available)
        Symbol symbolForType = symbolTable.lookup(varName);
        if (symbolForType != null) {
            variableTypes.put(varName, symbolForType.getType());
        }

        // If there's an initialization expression, evaluate it and store the result
        if (ctx.expression() != null) {
            visit(ctx.expression());

            // Get the variable type from symbol table to determine storage instruction
            Symbol symbol = symbolTable.lookup(varName);
            if (symbol != null) {
                switch (symbol.getType()) {
                    case INT:
                        // Expect int on stack
                        mainMethod.storeLocal(slot, Type.INT_TYPE);
                        break;
                    case STRING:
                    default:
                        // Store as string
                        mainMethod.storeLocal(slot, Type.getType(String.class));
                        break;
                }
            }
        } else {
            // Initialize with default value
            Symbol symbol = symbolTable.lookup(varName);
            if (symbol != null && symbol.getType() == org.example.semantic.SigmaType.INT) {
                mainMethod.push(0);
                mainMethod.storeLocal(slot, Type.INT_TYPE);
            }
        }

        return null;
    }

    @Override
    public Void visitAssignmentStatement(SigmaParser.AssignmentStatementContext ctx) {
        String varName = ctx.IDENTIFIER().getText();

        // Look up the variable slot
        Integer slot = variableSlots.get(varName);
        if (slot != null) {
            // Evaluate the expression and store the result
            visit(ctx.expression());

            // Get the variable type from symbol table to determine storage instruction
            Symbol symbol = symbolTable.lookup(varName);
            if (symbol != null) {
                switch (symbol.getType()) {
                    case INT:
                        mainMethod.storeLocal(slot, Type.INT_TYPE);
                        break;
                    case STRING:
                    default:
                        mainMethod.storeLocal(slot, Type.getType(String.class));
                        break;
                }
            }
        } else {
            errors.add("Assignment to undefined variable: " + varName);
        }

        return null;
    }

    @Override
    public Void visitExpressionStatement(SigmaParser.ExpressionStatementContext ctx) {
        visit(ctx.expression());
        return null;
    }

    @Override
    public Void visitExpression(SigmaParser.ExpressionContext ctx) {
        if (ctx.primary() != null) {
            // Simple primary expression
            visit(ctx.primary());
        } else if (ctx.getChildCount() >= 3 && ctx.getChild(1).getText().equals("(")) {
            // Method call: expression '(' argumentList? ')'
            handleMethodCall(ctx);
        } else if (ctx.getChildCount() == 3 && isOperator(ctx.getChild(1).getText())) {
            // Binary operation
            handleBinaryOperation(ctx);
        } else if (ctx.getChildCount() == 3 && ctx.getChild(0).getText().equals("(")) {
            // Parentheses: '(' expression ')'
            visit(ctx.expression(0));
        }
        return null;
    }

    private void handleMethodCall(SigmaParser.ExpressionContext ctx) {
        // Get the method name from the left expression
        SigmaParser.ExpressionContext leftExpr = ctx.expression(0);
        String methodName = null;

        // Extract method name from various possible structures
        if (leftExpr.primary() != null && leftExpr.primary().IDENTIFIER() != null) {
            methodName = leftExpr.primary().IDENTIFIER().getText();
        }

        if ("println".equals(methodName)) {
            // Generate System.out.println call
            mainMethod.getStatic(Type.getType(System.class), "out", Type.getType("Ljava/io/PrintStream;"));

            // Visit the argument if present
            if (ctx.argumentList() != null && ctx.argumentList().expression().size() > 0) {
                SigmaParser.ExpressionContext argExpr = ctx.argumentList().expression(0);
                if (expressionIsInt(argExpr)) {
                    visit(argExpr);
                    mainMethod.invokeVirtual(Type.getType("Ljava/io/PrintStream;"),
                            Method.getMethod("void println (int)"));
                    return;
                }

                // Default: evaluate and print as String
                visit(argExpr);
            } else {
                mainMethod.push(""); // Default empty string
            }

            // Call println(String)
            mainMethod.invokeVirtual(Type.getType("Ljava/io/PrintStream;"),
                    Method.getMethod("void println (String)"));
        } else {
            // For other method calls, just visit the left expression for now
            visit(leftExpr);
        }
    }

    private void handleBinaryOperation(SigmaParser.ExpressionContext ctx) {
        String operator = ctx.getChild(1).getText();

        if ("+".equals(operator)) {
            // If both operands are ints, perform integer add; otherwise perform string concatenation
            boolean leftInt = expressionIsInt(ctx.expression(0));
            boolean rightInt = expressionIsInt(ctx.expression(1));
            if (leftInt && rightInt) {
                visit(ctx.expression(0));
                visit(ctx.expression(1));
                mainMethod.visitInsn(IADD);
            } else {
                // String concatenation via StringBuilder
                Type sbType = Type.getType(StringBuilder.class);
                mainMethod.newInstance(sbType);
                mainMethod.dup();
                mainMethod.invokeConstructor(sbType, Method.getMethod("void <init> ()"));

                // append left
                if (leftInt) {
                    visit(ctx.expression(0));
                    mainMethod.invokeVirtual(sbType, Method.getMethod("java.lang.StringBuilder append (int)"));
                } else {
                    visit(ctx.expression(0));
                    mainMethod.invokeVirtual(sbType, Method.getMethod("java.lang.StringBuilder append (String)"));
                }

                // append right
                if (rightInt) {
                    visit(ctx.expression(1));
                    mainMethod.invokeVirtual(sbType, Method.getMethod("java.lang.StringBuilder append (int)"));
                } else {
                    visit(ctx.expression(1));
                    mainMethod.invokeVirtual(sbType, Method.getMethod("java.lang.StringBuilder append (String)"));
                }

                // toString
                mainMethod.invokeVirtual(sbType, Method.getMethod("String toString ()"));
            }
        } else if ("-".equals(operator)) {
            visit(ctx.expression(0));
            visit(ctx.expression(1));
            mainMethod.visitInsn(ISUB);
        } else if ("*".equals(operator)) {
            visit(ctx.expression(0));
            visit(ctx.expression(1));
            mainMethod.visitInsn(IMUL);
        } else if ("/".equals(operator)) {
            visit(ctx.expression(0));
            visit(ctx.expression(1));
            mainMethod.visitInsn(IDIV);
        } else if ("%".equals(operator)) {
            visit(ctx.expression(0));
            visit(ctx.expression(1));
            mainMethod.visitInsn(IREM);
        }
    }

    /**
     * Heuristic to determine whether an expression will push an int on the stack.
     * Handles integer literals, identifier ints, and simple binary arithmetic expressions.
     */
    private boolean expressionIsInt(SigmaParser.ExpressionContext expr) {
        if (expr == null) return false;
        // Check for literal via primary -> literal
        if (expr.primary() != null && expr.primary().literal() != null && expr.primary().literal().INTEGER() != null) return true;
        if (expr.primary() != null && expr.primary().IDENTIFIER() != null) {
            String name = expr.primary().IDENTIFIER().getText();
            org.example.semantic.SigmaType vt = variableTypes.get(name);
            return vt == org.example.semantic.SigmaType.INT;
        }
        // Binary arithmetic
        if (expr.getChildCount() == 3 && isOperator(expr.getChild(1).getText())) {
            String op = expr.getChild(1).getText();
            if ("+-*/%".contains(op)) {
                return expressionIsInt(expr.expression(0)) && expressionIsInt(expr.expression(1));
            }
        }
        return false;
    }

    private boolean isOperator(String text) {
        return text.matches("[+\\-*/%.&|<>=!]+");
    }

    @Override
    public Void visitPrimary(SigmaParser.PrimaryContext ctx) {
        if (ctx.IDENTIFIER() != null) {
            String varName = ctx.IDENTIFIER().getText();

            // Look up the variable in our local variable tracking
            Integer slot = variableSlots.get(varName);
            if (slot != null) {
                // Load the variable from its local variable slot
                Symbol symbol = symbolTable.lookup(varName);
                if (symbol != null && symbol.getType() == org.example.semantic.SigmaType.INT) {
                    // Load primitive int
                    mainMethod.loadLocal(slot, Type.INT_TYPE);
                } else {
                    // Load as string
                    mainMethod.loadLocal(slot, Type.getType(String.class));
                }
            } else {
                // Variable not found in local variables, push as literal string
                // This handles method names like "println"
                mainMethod.push(varName);
            }
        } else if (ctx.literal() != null) {
            visit(ctx.literal());
        }
        return null;
    }

    @Override
    public Void visitLiteral(SigmaParser.LiteralContext ctx) {
        if (ctx.STRING() != null) {
            String value = ctx.STRING().getText();
            // Remove quotes
            value = value.substring(1, value.length() - 1);
            mainMethod.push(value);
        } else if (ctx.INTEGER() != null) {
            int value = Integer.parseInt(ctx.INTEGER().getText());
            mainMethod.push(value); // push primitive int
        } else if (ctx.BOOLEAN() != null) {
            String value = ctx.BOOLEAN().getText();
            mainMethod.push(value);
        } else if (ctx.getText().equals("null")) {
            mainMethod.push("null");
        }
        return null;
    }

    /**
     * Get any errors that occurred during code generation
     */
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    /**
     * Check if code generation was successful
     */
    public boolean isSuccessful() {
        return errors.isEmpty();
    }
}