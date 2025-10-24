package org.example.codegen;

import org.antlr.v4.runtime.tree.ParseTree;
import org.example.parser.SigmaBaseVisitor;
import org.example.parser.SigmaParser;
import org.example.semantic.SigmaType;
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

    public SigmaCodeGenerator(SymbolTable symbolTable, String className) {
        this.symbolTable = symbolTable;
        this.className = className;
        this.errors = new ArrayList<>();
        this.variableSlots = new HashMap<>();
        this.variableTypes = new HashMap<>();
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
            String msg = "Code generation error: " + e.toString();
            errors.add(msg);
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

        if (symbol != null) {
            variableTypes.put(varName, symbol.getType());
        }

        // If there's an initialization expression, evaluate it and store the result
        if (ctx.expression() != null) {
            visit(ctx.expression());

            if (symbol != null) {
                switch (symbol.getType()) {
                    case INT:
                        mainMethod.storeLocal(slot, Type.INT_TYPE);
                        break;
                    case DOUBLE:
                        mainMethod.storeLocal(slot, Type.DOUBLE_TYPE);
                        break;
                    case BOOLEAN:
                        mainMethod.storeLocal(slot, Type.BOOLEAN_TYPE);
                        break;
                    case STRING:
                    default:
                        mainMethod.storeLocal(slot, Type.getType(String.class));
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

        Integer slot = variableSlots.get(varName);
        if (slot != null) {
            visit(ctx.expression());

            Symbol symbol = symbolTable.lookup(varName);
            if (symbol != null) {
                switch (symbol.getType()) {
                    case INT:
                        mainMethod.storeLocal(slot, Type.INT_TYPE);
                        break;
                    case DOUBLE:
                        mainMethod.storeLocal(slot, Type.DOUBLE_TYPE);
                        break;
                    case BOOLEAN:
                        mainMethod.storeLocal(slot, Type.BOOLEAN_TYPE);
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
        if (ctx.logicalOrExpression() != null) {
            visit(ctx.logicalOrExpression());
        }
        return null;
    }

    @Override
    public Void visitLogicalOrExpression(SigmaParser.LogicalOrExpressionContext ctx) {
        List<SigmaParser.LogicalAndExpressionContext> operands = ctx.logicalAndExpression();
        if (operands.size() == 1) {
            visit(operands.get(0));
        } else {
            for (int i = 0; i < operands.size(); i++) {
                visit(operands.get(i));
                if (i < operands.size() - 1) {
                    String op = ctx.LOGICAL(i).getText();
                    if ("||".equals(op)) {
                        mainMethod.visitInsn(IOR);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Void visitLogicalAndExpression(SigmaParser.LogicalAndExpressionContext ctx) {
        List<SigmaParser.RelationalExpressionContext> operands = ctx.relationalExpression();
        if (operands.size() == 1) {
            visit(operands.get(0));
        } else {
            for (int i = 0; i < operands.size(); i++) {
                visit(operands.get(i));
                if (i < operands.size() - 1) {
                    String op = ctx.LOGICAL(i).getText();
                    if ("&&".equals(op)) {
                        mainMethod.visitInsn(IAND);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Void visitRelationalExpression(SigmaParser.RelationalExpressionContext ctx) {
        List<SigmaParser.AdditiveExpressionContext> operands = ctx.additiveExpression();
        if (operands.size() == 1) {
            visit(operands.get(0));
        } else {
            for (int i = 0; i < operands.size() - 1; i++) {
                visit(operands.get(i));
                visit(operands.get(i + 1));

                String op = ctx.RELATIONAL(i).getText();

                boolean isDouble = isAdditiveExpressionDouble(operands.get(i)) ||
                                   isAdditiveExpressionDouble(operands.get(i + 1));

                if (isDouble) {
                    mainMethod.visitInsn(DCMPL);
                    Label lTrue = new Label();
                    Label lEnd = new Label();

                    switch (op) {
                        case ">":
                            mainMethod.visitJumpInsn(IFGT, lTrue);
                            break;
                        case "<":
                            mainMethod.visitJumpInsn(IFLT, lTrue);
                            break;
                        case ">=":
                            mainMethod.visitJumpInsn(IFGE, lTrue);
                            break;
                        case "<=":
                            mainMethod.visitJumpInsn(IFLE, lTrue);
                            break;
                        case "==":
                            mainMethod.visitJumpInsn(IFEQ, lTrue);
                            break;
                        case "!=":
                            mainMethod.visitJumpInsn(IFNE, lTrue);
                            break;
                    }

                    mainMethod.push(0);
                    mainMethod.visitJumpInsn(GOTO, lEnd);
                    mainMethod.visitLabel(lTrue);
                    mainMethod.push(1);
                    mainMethod.visitLabel(lEnd);
                } else {
                    Label lTrue = new Label();
                    Label lEnd = new Label();

                    switch (op) {
                        case ">":
                            mainMethod.visitJumpInsn(IF_ICMPGT, lTrue);
                            break;
                        case "<":
                            mainMethod.visitJumpInsn(IF_ICMPLT, lTrue);
                            break;
                        case ">=":
                            mainMethod.visitJumpInsn(IF_ICMPGE, lTrue);
                            break;
                        case "<=":
                            mainMethod.visitJumpInsn(IF_ICMPLE, lTrue);
                            break;
                        case "==":
                            mainMethod.visitJumpInsn(IF_ICMPEQ, lTrue);
                            break;
                        case "!=":
                            mainMethod.visitJumpInsn(IF_ICMPNE, lTrue);
                            break;
                    }

                    mainMethod.push(0);
                    mainMethod.visitJumpInsn(GOTO, lEnd);
                    mainMethod.visitLabel(lTrue);
                    mainMethod.push(1);
                    mainMethod.visitLabel(lEnd);
                }
            }
        }
        return null;
    }

    @Override
    public Void visitAdditiveExpression(SigmaParser.AdditiveExpressionContext ctx) {
        List<SigmaParser.MultiplicativeExpressionContext> operands = ctx.multiplicativeExpression();
        if (operands.size() == 1) {
            visit(operands.get(0));
        } else {
            for (int i = 0; i < operands.size(); i++) {
                visit(operands.get(i));
                if (i < operands.size() - 1) {
                    String op = ctx.ADDITIVE(i).getText();

                    boolean leftIsDouble = isMultiplicativeExpressionDouble(operands.get(i));
                    boolean rightIsDouble = isMultiplicativeExpressionDouble(operands.get(i + 1));

                    if (leftIsDouble || rightIsDouble) {
                        if ("+".equals(op)) {
                            mainMethod.visitInsn(DADD);
                        } else if ("-".equals(op)) {
                            mainMethod.visitInsn(DSUB);
                        }
                    } else {
                        if ("+".equals(op)) {
                            mainMethod.visitInsn(IADD);
                        } else if ("-".equals(op)) {
                            mainMethod.visitInsn(ISUB);
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Void visitMultiplicativeExpression(SigmaParser.MultiplicativeExpressionContext ctx) {
        List<SigmaParser.UnaryExpressionContext> operands = ctx.unaryExpression();
        if (operands.size() == 1) {
            visit(operands.get(0));
        } else {
            for (int i = 0; i < operands.size(); i++) {
                visit(operands.get(i));
                if (i < operands.size() - 1) {
                    String op = ctx.MULTIPLICATIVE(i).getText();

                    boolean leftIsDouble = isUnaryExpressionDouble(operands.get(i));
                    boolean rightIsDouble = isUnaryExpressionDouble(operands.get(i + 1));

                    if (leftIsDouble || rightIsDouble) {
                        switch (op) {
                            case "*":
                                mainMethod.visitInsn(DMUL);
                                break;
                            case "/":
                                mainMethod.visitInsn(DDIV);
                                break;
                            case "%":
                                mainMethod.visitInsn(DREM);
                                break;
                        }
                    } else {
                        switch (op) {
                            case "*":
                                mainMethod.visitInsn(IMUL);
                                break;
                            case "/":
                                mainMethod.visitInsn(IDIV);
                                break;
                            case "%":
                                mainMethod.visitInsn(IREM);
                                break;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Void visitUnaryExpression(SigmaParser.UnaryExpressionContext ctx) {
        if (ctx.NOT() != null) {
            visit(ctx.unaryExpression());
            // Logical NOT: flip 0 to 1 and 1 to 0
            mainMethod.push(1);
            mainMethod.visitInsn(IXOR);
        } else if (ctx.MINUS() != null) {
            visit(ctx.unaryExpression());

            if (isUnaryExpressionDouble(ctx.unaryExpression())) {
                mainMethod.visitInsn(DNEG);
            } else {
                mainMethod.visitInsn(INEG);
            }
        } else if (ctx.postfixExpression() != null) {
            visit(ctx.postfixExpression());
        }
        return null;
    }

    @Override
    public Void visitPostfixExpression(SigmaParser.PostfixExpressionContext ctx) {
        SigmaParser.PrimaryExpressionContext primary = ctx.primaryExpression();
        List<SigmaParser.PostfixOpContext> postfixOps = ctx.postfixOp();

        if (postfixOps.isEmpty()) {
            visit(primary);
        } else {
            // Handle method calls and member access
            String baseName = null;
            if (primary.IDENTIFIER() != null) {
                baseName = primary.IDENTIFIER().getText();
            }

            for (SigmaParser.PostfixOpContext op : postfixOps) {
                if (op.LPAREN() != null) {
                    // Method call
                    if ("println".equals(baseName)) {
                        handlePrintln(op.argumentList());
                    }
                } else if (op.DOT() != null && op.IDENTIFIER() != null) {
                    // Member access - update baseName
                    baseName = op.IDENTIFIER().getText();
                }
            }
        }
        return null;
    }

    private void handlePrintln(SigmaParser.ArgumentListContext argList) {
        if (argList != null && !argList.expression().isEmpty()) {
            SigmaParser.ExpressionContext argExpr = argList.expression().get(0);
            org.example.semantic.SigmaType exprType = inferExpressionType(argExpr);

            mainMethod.getStatic(Type.getType(System.class), "out", Type.getType("Ljava/io/PrintStream;"));
            visit(argExpr);

            switch (exprType) {
                case INT:
                    mainMethod.invokeVirtual(Type.getType("Ljava/io/PrintStream;"), Method.getMethod("void println (int)"));
                    break;
                case DOUBLE:
                    mainMethod.invokeVirtual(Type.getType("Ljava/io/PrintStream;"), Method.getMethod("void println (double)"));
                    break;
                case BOOLEAN:
                    mainMethod.invokeVirtual(Type.getType("Ljava/io/PrintStream;"), Method.getMethod("void println (boolean)"));
                    break;
                case STRING:
                default:
                    mainMethod.invokeVirtual(Type.getType("Ljava/io/PrintStream;"), Method.getMethod("void println (String)"));
                    break;
            }
        } else {
            mainMethod.getStatic(Type.getType(System.class), "out", Type.getType("Ljava/io/PrintStream;"));
            mainMethod.invokeVirtual(Type.getType("Ljava/io/PrintStream;"), Method.getMethod("void println ()"));
        }
    }

    @Override
    public Void visitPrimaryExpression(SigmaParser.PrimaryExpressionContext ctx) {
        if (ctx.IDENTIFIER() != null) {
            String varName = ctx.IDENTIFIER().getText();
            Integer slot = variableSlots.get(varName);

            if (slot != null) {
                org.example.semantic.SigmaType varType = variableTypes.get(varName);
                if (varType != null) {
                    switch (varType) {
                        case INT:
                            mainMethod.loadLocal(slot, Type.INT_TYPE);
                            break;
                        case DOUBLE:
                            mainMethod.loadLocal(slot, Type.DOUBLE_TYPE);
                            break;
                        case BOOLEAN:
                            mainMethod.loadLocal(slot, Type.BOOLEAN_TYPE);
                            break;
                        case STRING:
                        default:
                            mainMethod.loadLocal(slot, Type.getType(String.class));
                            break;
                    }
                }
            } else {
                errors.add("Undefined variable: " + varName);
            }
        } else if (ctx.literal() != null) {
            visit(ctx.literal());
        } else if (ctx.expression() != null) {
            // Parenthesized expression
            visit(ctx.expression());
        }
        return null;
    }

    @Override
    public Void visitLiteral(SigmaParser.LiteralContext ctx) {
        if (ctx.INTEGER() != null) {
            int value = Integer.parseInt(ctx.INTEGER().getText());
            mainMethod.push(value);
        } else if (ctx.FLOAT() != null) {
            double value = Double.parseDouble(ctx.FLOAT().getText());
            mainMethod.push(value);
        } else if (ctx.STRING() != null) {
            String str = ctx.STRING().getText();
            // Remove surrounding quotes
            str = str.substring(1, str.length() - 1);
            // Handle escape sequences
            str = str.replace("\\n", "\n")
                     .replace("\\t", "\t")
                     .replace("\\\"", "\"")
                     .replace("\\\\", "\\");
            mainMethod.push(str);
        } else if (ctx.BOOLEAN() != null) {
            boolean value = Boolean.parseBoolean(ctx.BOOLEAN().getText());
            mainMethod.push(value ? 1 : 0);
        } else if (ctx.NULL() != null) {
            mainMethod.visitInsn(ACONST_NULL);
        }
        return null;
    }

    @Override
    public Void visitIfStatement(SigmaParser.IfStatementContext ctx) {
        visit(ctx.expression());
        Label lElse = new Label();
        Label lEnd = new Label();

        mainMethod.visitJumpInsn(IFEQ, lElse);
        visit(ctx.statement(0));
        mainMethod.visitJumpInsn(GOTO, lEnd);

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
        visit(ctx.expression());
        mainMethod.visitJumpInsn(IFEQ, lEnd);
        visit(ctx.statement());
        mainMethod.visitJumpInsn(GOTO, lStart);
        mainMethod.visitLabel(lEnd);
        return null;
    }

    @Override
    public Void visitBlock(SigmaParser.BlockContext ctx) {
        for (SigmaParser.DeclarationContext decl : ctx.declaration()) {
            visit(decl);
        }
        for (SigmaParser.StatementContext stmt : ctx.statement()) {
            visit(stmt);
        }
        return null;
    }

    // Type inference helper methods

    private org.example.semantic.SigmaType inferExpressionType(SigmaParser.ExpressionContext ctx) {
        if (ctx == null || ctx.logicalOrExpression() == null) {
            return org.example.semantic.SigmaType.UNKNOWN;
        }
        return inferLogicalOrType(ctx.logicalOrExpression());
    }

    private org.example.semantic.SigmaType inferLogicalOrType(SigmaParser.LogicalOrExpressionContext ctx) {
        if (ctx.logicalAndExpression().size() > 1) {
            return org.example.semantic.SigmaType.BOOLEAN;
        }
        return inferLogicalAndType(ctx.logicalAndExpression(0));
    }

    private org.example.semantic.SigmaType inferLogicalAndType(SigmaParser.LogicalAndExpressionContext ctx) {
        if (ctx.relationalExpression().size() > 1) {
            return org.example.semantic.SigmaType.BOOLEAN;
        }
        return inferRelationalType(ctx.relationalExpression(0));
    }

    private org.example.semantic.SigmaType inferRelationalType(SigmaParser.RelationalExpressionContext ctx) {
        if (ctx.additiveExpression().size() > 1) {
            return org.example.semantic.SigmaType.BOOLEAN;
        }
        return inferAdditiveType(ctx.additiveExpression(0));
    }

    private org.example.semantic.SigmaType inferAdditiveType(SigmaParser.AdditiveExpressionContext ctx) {
        boolean anyDouble = false;
        for (SigmaParser.MultiplicativeExpressionContext mult : ctx.multiplicativeExpression()) {
            org.example.semantic.SigmaType t = inferMultiplicativeType(mult);
            if (t == org.example.semantic.SigmaType.DOUBLE || t == SigmaType.DOUBLE) {
                anyDouble = true;
            }
        }
        return anyDouble ? org.example.semantic.SigmaType.DOUBLE : org.example.semantic.SigmaType.INT;
    }

    private org.example.semantic.SigmaType inferMultiplicativeType(SigmaParser.MultiplicativeExpressionContext ctx) {
        boolean anyDouble = false;
        for (SigmaParser.UnaryExpressionContext unary : ctx.unaryExpression()) {
            org.example.semantic.SigmaType t = inferUnaryType(unary);
            if (t == org.example.semantic.SigmaType.DOUBLE || t == SigmaType.DOUBLE) {
                anyDouble = true;
            }
        }
        return anyDouble ? org.example.semantic.SigmaType.DOUBLE : org.example.semantic.SigmaType.INT;
    }

    private org.example.semantic.SigmaType inferUnaryType(SigmaParser.UnaryExpressionContext ctx) {
        if (ctx.NOT() != null) {
            return org.example.semantic.SigmaType.BOOLEAN;
        } else if (ctx.MINUS() != null) {
            return inferUnaryType(ctx.unaryExpression());
        } else if (ctx.postfixExpression() != null) {
            return inferPostfixType(ctx.postfixExpression());
        }
        return org.example.semantic.SigmaType.UNKNOWN;
    }

    private org.example.semantic.SigmaType inferPostfixType(SigmaParser.PostfixExpressionContext ctx) {
        return inferPrimaryType(ctx.primaryExpression());
    }

    private org.example.semantic.SigmaType inferPrimaryType(SigmaParser.PrimaryExpressionContext ctx) {
        if (ctx.IDENTIFIER() != null) {
            String varName = ctx.IDENTIFIER().getText();
            org.example.semantic.SigmaType varType = variableTypes.get(varName);
            if (varType != null) {
                return varType;
            }
            Symbol symbol = symbolTable.lookup(varName);
            if (symbol != null) {
                return symbol.getType();
            }
        } else if (ctx.literal() != null) {
            return inferLiteralType(ctx.literal());
        } else if (ctx.expression() != null) {
            return inferExpressionType(ctx.expression());
        }
        return org.example.semantic.SigmaType.UNKNOWN;
    }

    private org.example.semantic.SigmaType inferLiteralType(SigmaParser.LiteralContext ctx) {
        if (ctx.INTEGER() != null) {
            return org.example.semantic.SigmaType.INT;
        } else if (ctx.FLOAT() != null) {
            return org.example.semantic.SigmaType.DOUBLE;
        } else if (ctx.STRING() != null) {
            return org.example.semantic.SigmaType.STRING;
        } else if (ctx.BOOLEAN() != null) {
            return org.example.semantic.SigmaType.BOOLEAN;
        } else if (ctx.NULL() != null) {
            return org.example.semantic.SigmaType.NULL;
        }
        return org.example.semantic.SigmaType.UNKNOWN;
    }

    private boolean isAdditiveExpressionDouble(SigmaParser.AdditiveExpressionContext ctx) {
        return inferAdditiveType(ctx) == org.example.semantic.SigmaType.DOUBLE;
    }

    private boolean isMultiplicativeExpressionDouble(SigmaParser.MultiplicativeExpressionContext ctx) {
        return inferMultiplicativeType(ctx) == org.example.semantic.SigmaType.DOUBLE;
    }

    private boolean isUnaryExpressionDouble(SigmaParser.UnaryExpressionContext ctx) {
        return inferUnaryType(ctx) == org.example.semantic.SigmaType.DOUBLE;
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