package org.example.codegen;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
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

    @Override
    public Void visit(ParseTree tree) {
        if (tree == null) return null;
        return super.visit(tree);
    }

    /**
     * Generate bytecode for the given parse tree
     * @param parseTree the AST to compile
     * @return the compiled bytecode as byte array, or null if compilation failed
     */
    public byte[] generateBytecode(ParseTree parseTree) {
        try {
            if (parseTree == null) {
                throw new IllegalArgumentException("generateBytecode called with null parseTree");
            }
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
            // Capture full exception for debugging
            String msg = "Code generation error: " + e.toString();
            errors.add(msg);
            // also append stack trace lines
            for (StackTraceElement el : e.getStackTrace()) {
                errors.add("  at " + el.toString());
            }
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

        // Look up the declared symbol and allocate a local variable slot for it consistently.
        Symbol symbol = symbolTable.lookup(varName);
        int slot;
        if (symbol != null) {
            switch (symbol.getType()) {
                case INT:
                    slot = mainMethod.newLocal(Type.INT_TYPE);
                    break;
                case DOUBLE:
                    slot = mainMethod.newLocal(Type.DOUBLE_TYPE);
                    break;
                case BOOLEAN:
                    // Booleans are represented as ints on the JVM
                    slot = mainMethod.newLocal(Type.BOOLEAN_TYPE);
                    break;
                case STRING:
                default:
                    slot = mainMethod.newLocal(Type.getType(String.class));
                    break;
            }
        } else {
            slot = mainMethod.newLocal(Type.getType(String.class));
        }
        variableSlots.put(varName, slot);

        // Record declared type (if available) so expressionIsInt and loads can use it
        if (symbol != null) {
            variableTypes.put(varName, symbol.getType());
        }
        // Debug: print allocation
        System.out.println("[CODEGEN] allocated slot=" + slot + " for var='" + varName + "' type=" + (symbol != null ? symbol.getType() : "<unknown>"));

        // If there's an initialization expression, evaluate it and store the result
        if (ctx.expression() != null) {
            visit(ctx.expression());

            // Use the same symbol we looked up earlier to decide storage instruction
            if (symbol != null) {
                switch (symbol.getType()) {
                    case INT:
                        // Expect int on stack
                        mainMethod.storeLocal(slot, Type.INT_TYPE);
                        System.out.println("[CODEGEN] storeLocal ISTORE slot=" + slot + " for var='" + varName + "'");
                        break;
                    case DOUBLE:
                        mainMethod.storeLocal(slot, Type.DOUBLE_TYPE);
                        System.out.println("[CODEGEN] storeLocal DSTORE slot=" + slot + " for var='" + varName + "'");
                        break;
                    case BOOLEAN:
                        mainMethod.storeLocal(slot, Type.BOOLEAN_TYPE);
                        System.out.println("[CODEGEN] storeLocal ISTORE(slot as boolean) slot=" + slot + " for var='" + varName + "'");
                        break;
                    case STRING:
                    default:
                        // Store as string/reference
                        mainMethod.storeLocal(slot, Type.getType(String.class));
                        System.out.println("[CODEGEN] storeLocal ASTORE slot=" + slot + " for var='" + varName + "'");
                        break;
                }
            }
        } else {
            // Initialize with default value
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
                        System.out.println("[CODEGEN] storeLocal ISTORE slot=" + slot + " for assign var='" + varName + "'");
                        break;
                    case DOUBLE:
                        mainMethod.storeLocal(slot, Type.DOUBLE_TYPE);
                        System.out.println("[CODEGEN] storeLocal DSTORE slot=" + slot + " for assign var='" + varName + "'");
                        break;
                    case BOOLEAN:
                        mainMethod.storeLocal(slot, Type.BOOLEAN_TYPE);
                        System.out.println("[CODEGEN] storeLocal ISTORE(slot as boolean) slot=" + slot + " for assign var='" + varName + "'");
                        break;
                    case STRING:
                    default:
                        mainMethod.storeLocal(slot, Type.getType(String.class));
                        System.out.println("[CODEGEN] storeLocal ASTORE slot=" + slot + " for assign var='" + varName + "'");
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
        // Be defensive: check child counts and nulls before visiting
        int c = ctx.getChildCount();
        // Handle method calls first: need at least 3 children and a '(' at position 1
        if (c >= 3 && ctx.getChild(1) != null && "(".equals(ctx.getChild(1).getText())) {
            handleMethodCall(ctx);
        }
        // Binary operations including exponentiation (ensure we catch operator forms before primary shortcut)
        else if (c == 3 && ctx.getChild(1) != null && isOperator(ctx.getChild(1).getText())) {
            handleBinaryOperation(ctx);
        }
        // Parentheses: '(' expression ')'
        else if (c == 3 && ctx.getChild(0) != null && "(".equals(ctx.getChild(0).getText())) {
            if (ctx.expression().size() > 0 && ctx.expression(0) != null) visit(ctx.expression(0));
        }
        // Simple primary expression (identifiers, literals)
        else if (ctx.primary() != null) {
            visit(ctx.primary());
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
            // Evaluate the argument into a typed temporary local to preserve evaluation order
            if (ctx.argumentList() != null && ctx.argumentList().expression().size() > 0) {
                SigmaParser.ExpressionContext argExpr = ctx.argumentList().expression(0);
                org.example.semantic.SigmaType exprType = getExpressionSigmaType(argExpr);

                // allocate a typed temp for the evaluated argument
                int tempSlot;
                Type tempType;
                switch (exprType) {
                    case INT:
                        tempType = Type.INT_TYPE;
                        tempSlot = mainMethod.newLocal(tempType);
                        visit(argExpr);
                        mainMethod.storeLocal(tempSlot, tempType);
                        // push System.out then load int arg and call println(int)
                        mainMethod.getStatic(Type.getType(System.class), "out", Type.getType("Ljava/io/PrintStream;"));
                        mainMethod.loadLocal(tempSlot, tempType);
                        mainMethod.invokeVirtual(Type.getType("Ljava/io/PrintStream;"), Method.getMethod("void println (int)"));
                        return;
                    case DOUBLE:
                        tempType = Type.DOUBLE_TYPE;
                        tempSlot = mainMethod.newLocal(tempType);
                        visit(argExpr);
                        // ensure double on stack (if int -> I2D done by expression code)
                        mainMethod.storeLocal(tempSlot, tempType);
                        mainMethod.getStatic(Type.getType(System.class), "out", Type.getType("Ljava/io/PrintStream;"));
                        mainMethod.loadLocal(tempSlot, tempType);
                        mainMethod.invokeVirtual(Type.getType("Ljava/io/PrintStream;"), Method.getMethod("void println (double)"));
                        return;
                    case BOOLEAN:
                        tempType = Type.BOOLEAN_TYPE;
                        tempSlot = mainMethod.newLocal(tempType);
                        visit(argExpr);
                        mainMethod.storeLocal(tempSlot, tempType);
                        mainMethod.getStatic(Type.getType(System.class), "out", Type.getType("Ljava/io/PrintStream;"));
                        mainMethod.loadLocal(tempSlot, tempType);
                        mainMethod.invokeVirtual(Type.getType("Ljava/io/PrintStream;"), Method.getMethod("void println (boolean)"));
                        return;
                    case STRING:
                    default:
                        tempType = Type.getType(String.class);
                        tempSlot = mainMethod.newLocal(tempType);
                        visit(argExpr);
                        mainMethod.storeLocal(tempSlot, tempType);
                        mainMethod.getStatic(Type.getType(System.class), "out", Type.getType("Ljava/io/PrintStream;"));
                        mainMethod.loadLocal(tempSlot, tempType);
                        mainMethod.invokeVirtual(Type.getType("Ljava/io/PrintStream;"), Method.getMethod("void println (String)"));
                        return;
                }
            } else {
                // No argument: println a blank line
                mainMethod.getStatic(Type.getType(System.class), "out", Type.getType("Ljava/io/PrintStream;"));
                mainMethod.invokeVirtual(Type.getType("Ljava/io/PrintStream;"), Method.getMethod("void println ()"));
                return;
            }
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
                // Evaluate both operands to String temporaries to avoid stack type issues
                int leftTemp = mainMethod.newLocal(Type.getType(String.class));
                int rightTemp = mainMethod.newLocal(Type.getType(String.class));

                // Evaluate left -> String (pick correct String.valueOf overload based on operand type)
                if (leftInt) {
                    visit(ctx.expression(0));
                    mainMethod.invokeStatic(Type.getType(String.class), Method.getMethod("String valueOf (int)"));
                } else if (expressionIsBoolean(ctx.expression(0))) {
                    visit(ctx.expression(0));
                    mainMethod.invokeStatic(Type.getType(String.class), Method.getMethod("String valueOf (boolean)"));
                } else if (expressionIsDouble(ctx.expression(0))) {
                    visit(ctx.expression(0));
                    mainMethod.invokeStatic(Type.getType(String.class), Method.getMethod("String valueOf (double)"));
                } else {
                    visit(ctx.expression(0));
                    // ensure object converted to String
                    mainMethod.invokeStatic(Type.getType(String.class), Method.getMethod("String valueOf (Object)"));
                }
                mainMethod.storeLocal(leftTemp, Type.getType(String.class));

                // Evaluate right -> String
                if (rightInt) {
                    visit(ctx.expression(1));
                    mainMethod.invokeStatic(Type.getType(String.class), Method.getMethod("String valueOf (int)"));
                } else if (expressionIsBoolean(ctx.expression(1))) {
                    visit(ctx.expression(1));
                    mainMethod.invokeStatic(Type.getType(String.class), Method.getMethod("String valueOf (boolean)"));
                } else if (expressionIsDouble(ctx.expression(1))) {
                    visit(ctx.expression(1));
                    mainMethod.invokeStatic(Type.getType(String.class), Method.getMethod("String valueOf (double)"));
                } else {
                    visit(ctx.expression(1));
                    mainMethod.invokeStatic(Type.getType(String.class), Method.getMethod("String valueOf (Object)"));
                }
                mainMethod.storeLocal(rightTemp, Type.getType(String.class));

                // Build final String via StringBuilder
                Type sbType = Type.getType(StringBuilder.class);
                mainMethod.newInstance(sbType);
                mainMethod.dup();
                mainMethod.invokeConstructor(sbType, Method.getMethod("void <init> ()"));

                // append left
                mainMethod.loadLocal(leftTemp, Type.getType(String.class));
                mainMethod.invokeVirtual(sbType, Method.getMethod("java.lang.StringBuilder append (String)"));

                // append right
                mainMethod.loadLocal(rightTemp, Type.getType(String.class));
                mainMethod.invokeVirtual(sbType, Method.getMethod("java.lang.StringBuilder append (String)"));

                // toString
                mainMethod.invokeVirtual(sbType, Method.getMethod("String toString ()"));
            }
        } else if ("-".equals(operator)) {
            visit(ctx.expression(0));
            visit(ctx.expression(1));
            mainMethod.visitInsn(ISUB);
        } else if ("^".equals(operator)) {
            // Robust handling for exponentiation '^'
            // Extract left/right nodes (prefer expression children but fall back to raw children)
            ParseTree leftNode = null;
            ParseTree rightNode = null;
            if (ctx.expression() != null && ctx.expression().size() >= 2) {
                leftNode = ctx.expression(0);
                rightNode = ctx.expression(1);
            } else if (ctx.getChildCount() >= 3) {
                leftNode = ctx.getChild(0);
                rightNode = ctx.getChild(2);
            }

            // Infer types for nodes
            org.example.semantic.SigmaType lt = getTypeForNode(leftNode);
            org.example.semantic.SigmaType rt = getTypeForNode(rightNode);

            boolean leftIsInt = (lt == org.example.semantic.SigmaType.INT) || isIntegerLiteralNode(leftNode) || (leftNode instanceof SigmaParser.ExpressionContext && expressionIsInt((SigmaParser.ExpressionContext) leftNode));
            boolean leftIsDouble = (lt == org.example.semantic.SigmaType.DOUBLE) || (leftNode instanceof SigmaParser.ExpressionContext && expressionIsDouble((SigmaParser.ExpressionContext) leftNode));
            boolean rightIsInt = (rt == org.example.semantic.SigmaType.INT) || isIntegerLiteralNode(rightNode) || (rightNode instanceof SigmaParser.ExpressionContext && expressionIsInt((SigmaParser.ExpressionContext) rightNode));
            boolean rightIsDouble = (rt == org.example.semantic.SigmaType.DOUBLE) || (rightNode instanceof SigmaParser.ExpressionContext && expressionIsDouble((SigmaParser.ExpressionContext) rightNode));

            int leftTemp = mainMethod.newLocal(Type.DOUBLE_TYPE);
            int rightTemp = mainMethod.newLocal(Type.DOUBLE_TYPE);

            // Evaluate left
            if (leftIsInt) {
                visit(leftNode);
                mainMethod.visitInsn(I2D);
            } else if (leftIsDouble) {
                visit(leftNode);
            } else {
                // best-effort fallback: coerce to double
                if (leftNode != null) System.out.println("[CODEGEN] warning: treating left operand of ^ as int (fallback): " + leftNode.getText());
                visit(leftNode);
                mainMethod.visitInsn(I2D);
                leftIsInt = true;
            }
            mainMethod.storeLocal(leftTemp, Type.DOUBLE_TYPE);

            // Evaluate right
            if (rightIsInt) {
                visit(rightNode);
                mainMethod.visitInsn(I2D);
            } else if (rightIsDouble) {
                visit(rightNode);
            } else {
                if (rightNode != null) System.out.println("[CODEGEN] warning: treating right operand of ^ as int (fallback): " + rightNode.getText());
                visit(rightNode);
                mainMethod.visitInsn(I2D);
                rightIsInt = true;
            }
            mainMethod.storeLocal(rightTemp, Type.DOUBLE_TYPE);

            // Call Math.pow
            mainMethod.loadLocal(leftTemp, Type.DOUBLE_TYPE);
            mainMethod.loadLocal(rightTemp, Type.DOUBLE_TYPE);
            mainMethod.invokeStatic(Type.getType(Math.class), Method.getMethod("double pow (double,double)"));

            // If both operands were ints, cast back to int
            if (leftIsInt && rightIsInt) {
                mainMethod.visitInsn(D2I);
            }
        } else if ("*".equals(operator)) {
            // handle double vs int multiplication
            if (expressionIsDouble(ctx.expression(0)) || expressionIsDouble(ctx.expression(1))) {
                // ensure both on stack as double
                if (expressionIsInt(ctx.expression(0))) { visit(ctx.expression(0)); mainMethod.visitInsn(I2D); } else visit(ctx.expression(0));
                if (expressionIsInt(ctx.expression(1))) { visit(ctx.expression(1)); mainMethod.visitInsn(I2D); } else visit(ctx.expression(1));
                mainMethod.visitInsn(DMUL);
            } else {
                visit(ctx.expression(0));
                visit(ctx.expression(1));
                mainMethod.visitInsn(IMUL);
            }
        } else if ("/".equals(operator)) {
            if (expressionIsDouble(ctx.expression(0)) || expressionIsDouble(ctx.expression(1))) {
                if (expressionIsInt(ctx.expression(0))) { visit(ctx.expression(0)); mainMethod.visitInsn(I2D); } else visit(ctx.expression(0));
                if (expressionIsInt(ctx.expression(1))) { visit(ctx.expression(1)); mainMethod.visitInsn(I2D); } else visit(ctx.expression(1));
                mainMethod.visitInsn(DDIV);
            } else {
                visit(ctx.expression(0));
                visit(ctx.expression(1));
                mainMethod.visitInsn(IDIV);
            }
        } else if ("%".equals(operator)) {
            if (expressionIsDouble(ctx.expression(0)) || expressionIsDouble(ctx.expression(1))) {
                if (expressionIsInt(ctx.expression(0))) { visit(ctx.expression(0)); mainMethod.visitInsn(I2D); } else visit(ctx.expression(0));
                if (expressionIsInt(ctx.expression(1))) { visit(ctx.expression(1)); mainMethod.visitInsn(I2D); } else visit(ctx.expression(1));
                mainMethod.visitInsn(DREM);
            } else {
                visit(ctx.expression(0));
                visit(ctx.expression(1));
                mainMethod.visitInsn(IREM);
            }
        }
        // Comparison operators
        else if (">".equals(operator) || "<".equals(operator) || ">=".equals(operator) || "<=".equals(operator)) {
            boolean eitherDouble = expressionIsDouble(ctx.expression(0)) || expressionIsDouble(ctx.expression(1));
            if (eitherDouble) {
                // ensure both are doubles on stack
                if (expressionIsInt(ctx.expression(0))) { visit(ctx.expression(0)); mainMethod.visitInsn(I2D); } else visit(ctx.expression(0));
                if (expressionIsInt(ctx.expression(1))) { visit(ctx.expression(1)); mainMethod.visitInsn(I2D); } else visit(ctx.expression(1));
                // compare doubles -> pushes int (-1/0/1)
                mainMethod.visitInsn(DCMPL);
                Label lTrue = new Label();
                Label lEnd = new Label();
                if (">".equals(operator)) {
                    mainMethod.visitJumpInsn(IFGT, lTrue);
                } else if ("<".equals(operator)) {
                    mainMethod.visitJumpInsn(IFLT, lTrue);
                } else if (">=".equals(operator)) {
                    mainMethod.visitJumpInsn(IFGE, lTrue);
                } else { // <=
                    mainMethod.visitJumpInsn(IFLE, lTrue);
                }
                // false
                mainMethod.push(0);
                mainMethod.visitJumpInsn(GOTO, lEnd);
                // true
                mainMethod.visitLabel(lTrue);
                mainMethod.push(1);
                mainMethod.visitLabel(lEnd);
            } else {
                // integer comparison using IF_ICMP*
                visit(ctx.expression(0));
                visit(ctx.expression(1));
                Label lTrue = new Label();
                Label lEnd = new Label();
                if (">".equals(operator)) {
                    mainMethod.visitJumpInsn(IF_ICMPGT, lTrue);
                } else if ("<".equals(operator)) {
                    mainMethod.visitJumpInsn(IF_ICMPLT, lTrue);
                } else if (">=".equals(operator)) {
                    mainMethod.visitJumpInsn(IF_ICMPGE, lTrue);
                } else { // <=
                    mainMethod.visitJumpInsn(IF_ICMPLE, lTrue);
                }
                mainMethod.push(0);
                mainMethod.visitJumpInsn(GOTO, lEnd);
                mainMethod.visitLabel(lTrue);
                mainMethod.push(1);
                mainMethod.visitLabel(lEnd);
            }
        } else if ("==".equals(operator) || "!=".equals(operator)) {
            boolean eitherDouble = expressionIsDouble(ctx.expression(0)) || expressionIsDouble(ctx.expression(1));
            if (eitherDouble) {
                if (expressionIsInt(ctx.expression(0))) { visit(ctx.expression(0)); mainMethod.visitInsn(I2D); } else visit(ctx.expression(0));
                if (expressionIsInt(ctx.expression(1))) { visit(ctx.expression(1)); mainMethod.visitInsn(I2D); } else visit(ctx.expression(1));
                mainMethod.visitInsn(DCMPL);
                Label lTrue = new Label();
                Label lEnd = new Label();
                if ("==".equals(operator)) {
                    mainMethod.visitJumpInsn(IFEQ, lTrue);
                } else {
                    mainMethod.visitJumpInsn(IFNE, lTrue);
                }
                mainMethod.push(0);
                mainMethod.visitJumpInsn(GOTO, lEnd);
                mainMethod.visitLabel(lTrue);
                mainMethod.push(1);
                mainMethod.visitLabel(lEnd);
            } else {
                visit(ctx.expression(0));
                visit(ctx.expression(1));
                Label lTrue = new Label();
                Label lEnd = new Label();
                if ("==".equals(operator)) {
                    mainMethod.visitJumpInsn(IF_ICMPEQ, lTrue);
                } else {
                    mainMethod.visitJumpInsn(IF_ICMPNE, lTrue);
                }
                mainMethod.push(0);
                mainMethod.visitJumpInsn(GOTO, lEnd);
                mainMethod.visitLabel(lTrue);
                mainMethod.push(1);
                mainMethod.visitLabel(lEnd);
            }
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
            // treat arithmetic operators (including '^') as integer-preserving when both sides are int
            if ("+-*/%^".contains(op)) {
                return expressionIsInt(expr.expression(0)) && expressionIsInt(expr.expression(1));
            }
        }
        // Parenthesized expression: (expr)
        if (expr.getChildCount() == 3 && expr.getChild(0).getText().equals("(")) {
            return expressionIsInt(expr.expression(0));
        }
        // Unary minus
        if (expr.getChildCount() == 2) {
            String op = expr.getChild(0).getText();
            if ("-".equals(op) || "+".equals(op)) {
                return expressionIsInt(expr.expression(0));
            }
        }
        return false;
    }

    /**
     * Returns true if the expression is an integer literal (possibly parenthesized or with unary +/-).
     */
    private boolean isIntegerLiteral(SigmaParser.ExpressionContext expr) {
        if (expr == null) return false;
        if (expr.primary() != null && expr.primary().literal() != null && expr.primary().literal().INTEGER() != null) return true;
        // Parenthesized
        if (expr.getChildCount() == 3 && expr.getChild(0).getText().equals("(")) {
            return isIntegerLiteral(expr.expression(0));
        }
        // Unary + or -
        if (expr.getChildCount() == 2) {
            String op = expr.getChild(0).getText();
            if ("-".equals(op) || "+".equals(op)) {
                return isIntegerLiteral(expr.expression(0));
            }
        }
        return false;
    }

    private boolean expressionIsDouble(SigmaParser.ExpressionContext expr) {
        if (expr == null) return false;
        if (expr.primary() != null && expr.primary().literal() != null && expr.primary().literal().DOUBLE() != null) return true;
        if (expr.primary() != null && expr.primary().IDENTIFIER() != null) {
            String name = expr.primary().IDENTIFIER().getText();
            org.example.semantic.SigmaType vt = variableTypes.get(name);
            return vt == org.example.semantic.SigmaType.DOUBLE;
        }
        if (expr.getChildCount() == 3 && isOperator(expr.getChild(1).getText())) {
            String op = expr.getChild(1).getText();
            if ("+-*/%".contains(op) || op.equals("^")) {
                return expressionIsDouble(expr.expression(0)) || expressionIsDouble(expr.expression(1));
            }
        }
        if (expr.getChildCount() == 3 && expr.getChild(0).getText().equals("(")) {
            return expressionIsDouble(expr.expression(0));
        }
        return false;
    }

    private boolean expressionIsBoolean(SigmaParser.ExpressionContext expr) {
        if (expr == null) return false;
        // If it's a literal boolean like true/false
        if (expr.primary() != null && expr.primary().literal() != null && expr.primary().literal().BOOLEAN() != null) return true;
        // If operator is a comparison or equality, result is boolean
        if (expr.getChildCount() == 3 && isOperator(expr.getChild(1).getText())) {
            String op = expr.getChild(1).getText();
            if ("<".equals(op) || ">".equals(op) || "<=".equals(op) || ">=".equals(op) || "==".equals(op) || "!=".equals(op)) {
                return true;
            }
        }
        // Parenthesized expression
        if (expr.getChildCount() == 3 && expr.getChild(0).getText().equals("(")) {
            return expressionIsBoolean(expr.expression(0));
        }
        return false;
    }

    /**
     * Infer type for an arbitrary parse node (ExpressionContext, PrimaryContext, LiteralContext, or a raw terminal).
     */
    private org.example.semantic.SigmaType getTypeForNode(ParseTree node) {
        if (node == null) return org.example.semantic.SigmaType.UNKNOWN;
        if (node instanceof SigmaParser.ExpressionContext) {
            return getExpressionSigmaType((SigmaParser.ExpressionContext) node);
        }
        if (node instanceof SigmaParser.PrimaryContext) {
            SigmaParser.PrimaryContext p = (SigmaParser.PrimaryContext) node;
            if (p.literal() != null) {
                if (p.literal().INTEGER() != null) return org.example.semantic.SigmaType.INT;
                if (p.literal().DOUBLE() != null) return org.example.semantic.SigmaType.DOUBLE;
                if (p.literal().BOOLEAN() != null) return org.example.semantic.SigmaType.BOOLEAN;
                if (p.literal().STRING() != null) return org.example.semantic.SigmaType.STRING;
            }
            if (p.IDENTIFIER() != null) {
                String name = p.IDENTIFIER().getText();
                org.example.semantic.SigmaType vt = variableTypes.get(name);
                if (vt != null) return vt;
                Symbol s = symbolTable.lookup(name);
                if (s != null) return s.getType();
            }
        }
        if (node instanceof SigmaParser.LiteralContext) {
            SigmaParser.LiteralContext l = (SigmaParser.LiteralContext) node;
            if (l.INTEGER() != null) return org.example.semantic.SigmaType.INT;
            if (l.DOUBLE() != null) return org.example.semantic.SigmaType.DOUBLE;
            if (l.BOOLEAN() != null) return org.example.semantic.SigmaType.BOOLEAN;
            if (l.STRING() != null) return org.example.semantic.SigmaType.STRING;
        }
        if (node instanceof TerminalNode) {
            String txt = node.getText();
            if (txt.matches("[0-9]+")) return org.example.semantic.SigmaType.INT;
            if (txt.matches("[0-9]*\\.[0-9]+")) return org.example.semantic.SigmaType.DOUBLE;
            if ("true".equals(txt) || "false".equals(txt)) return org.example.semantic.SigmaType.BOOLEAN;
            if (txt.startsWith("\"") && txt.endsWith("\"")) return org.example.semantic.SigmaType.STRING;
        }
        return org.example.semantic.SigmaType.UNKNOWN;
    }

    private boolean isIntegerLiteralNode(ParseTree node) {
        if (node == null) return false;
        if (node instanceof SigmaParser.ExpressionContext) return isIntegerLiteral((SigmaParser.ExpressionContext) node);
        if (node instanceof SigmaParser.PrimaryContext) {
            SigmaParser.PrimaryContext p = (SigmaParser.PrimaryContext) node;
            if (p.literal() != null && p.literal().INTEGER() != null) return true;
        }
        if (node instanceof SigmaParser.LiteralContext) {
            SigmaParser.LiteralContext l = (SigmaParser.LiteralContext) node;
            return l.INTEGER() != null;
        }
        if (node instanceof TerminalNode) {
            String txt = node.getText();
            return txt.matches("[0-9]+");
        }
        return false;
    }

    private boolean isOperator(String text) {
        // include '^' as an operator
        return text.matches("[+\\-*/%\\^.&|<>=!]+");
    }

    @Override
    public Void visitIfStatement(SigmaParser.IfStatementContext ctx) {
        // Evaluate condition (should leave int 0/1 on stack)
        visit(ctx.expression());
        Label lElse = new Label();
        Label lEnd = new Label();
        // If condition == 0 jump to else
        mainMethod.visitJumpInsn(IFEQ, lElse);
        // Then branch
        visit(ctx.statement(0));
        mainMethod.visitJumpInsn(GOTO, lEnd);
        // Else branch (if present)
        mainMethod.visitLabel(lElse);
        if (ctx.statement().size() > 1) {
            visit(ctx.statement(1));
        }
        mainMethod.visitLabel(lEnd);
        return null;
    }

    @Override
    public Void visitWhileStatement(SigmaParser.WhileStatementContext ctx) {
        Label lStart = new Label();
        Label lEnd = new Label();
        mainMethod.visitLabel(lStart);
        // Evaluate condition
        visit(ctx.expression());
        // If condition == 0, exit loop
        mainMethod.visitJumpInsn(IFEQ, lEnd);
        // Loop body
        visit(ctx.statement());
        // Jump back to start
        mainMethod.visitJumpInsn(GOTO, lStart);
        mainMethod.visitLabel(lEnd);
        return null;
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

    // Infer expression type using available symbol/type info. This is a lightweight heuristic used by codegen.
    private org.example.semantic.SigmaType getExpressionSigmaType(SigmaParser.ExpressionContext expr) {
        if (expr == null) return org.example.semantic.SigmaType.UNKNOWN;
        if (expr.primary() != null && expr.primary().literal() != null) {
            if (expr.primary().literal().INTEGER() != null) return org.example.semantic.SigmaType.INT;
            if (expr.primary().literal().DOUBLE() != null) return org.example.semantic.SigmaType.DOUBLE;
            if (expr.primary().literal().BOOLEAN() != null) return org.example.semantic.SigmaType.BOOLEAN;
            if (expr.primary().literal().STRING() != null) return org.example.semantic.SigmaType.STRING;
        }
        if (expr.primary() != null && expr.primary().IDENTIFIER() != null) {
            String name = expr.primary().IDENTIFIER().getText();
            org.example.semantic.SigmaType vt = variableTypes.get(name);
            if (vt != null) return vt;
            Symbol s = symbolTable.lookup(name);
            if (s != null) return s.getType();
        }
        // Binary expression
        if (expr.expression().size() == 2) {
            // If operator is a comparison or equality, result is boolean
            String op = expr.getChild(1).getText();
            if ("<".equals(op) || ">".equals(op) || "<=".equals(op) || ">=".equals(op) || "==".equals(op) || "!=".equals(op)) {
                return org.example.semantic.SigmaType.BOOLEAN;
            }
            org.example.semantic.SigmaType l = getExpressionSigmaType(expr.expression(0));
            org.example.semantic.SigmaType r = getExpressionSigmaType(expr.expression(1));
            if (l == org.example.semantic.SigmaType.DOUBLE || r == org.example.semantic.SigmaType.DOUBLE) return org.example.semantic.SigmaType.DOUBLE;
            if (l == org.example.semantic.SigmaType.INT && r == org.example.semantic.SigmaType.INT) return org.example.semantic.SigmaType.INT;
            return org.example.semantic.SigmaType.UNKNOWN;
        }

        return org.example.semantic.SigmaType.UNKNOWN;
    }

    /**
     * Helper: infer type when left operand is available as primary on the parent expression.
     */
    private org.example.semantic.SigmaType getExpressionSigmaTypeFromPrimary(SigmaParser.ExpressionContext ctx) {
        if (ctx == null) return org.example.semantic.SigmaType.UNKNOWN;
        if (ctx.primary() != null) {
            if (ctx.primary().literal() != null) {
                if (ctx.primary().literal().INTEGER() != null) return org.example.semantic.SigmaType.INT;
                if (ctx.primary().literal().DOUBLE() != null) return org.example.semantic.SigmaType.DOUBLE;
                if (ctx.primary().literal().BOOLEAN() != null) return org.example.semantic.SigmaType.BOOLEAN;
                if (ctx.primary().literal().STRING() != null) return org.example.semantic.SigmaType.STRING;
            }
            if (ctx.primary().IDENTIFIER() != null) {
                String name = ctx.primary().IDENTIFIER().getText();
                org.example.semantic.SigmaType vt = variableTypes.get(name);
                if (vt != null) return vt;
                Symbol s = symbolTable.lookup(name);
                if (s != null) return s.getType();
            }
        }
        return org.example.semantic.SigmaType.UNKNOWN;
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
        } else if (ctx.DOUBLE() != null) {
            String txt = ctx.DOUBLE().getText();
            // strip trailing d/D if present
            if (txt.endsWith("d") || txt.endsWith("D")) txt = txt.substring(0, txt.length()-1);
            double dv = Double.parseDouble(txt);
            mainMethod.push(dv);
        } else if (ctx.BOOLEAN() != null) {
            boolean value = Boolean.parseBoolean(ctx.BOOLEAN().getText());
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