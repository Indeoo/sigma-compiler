package org.example.codegen;

import org.antlr.v4.runtime.tree.ParseTree;
import org.example.parser.SigmaBaseVisitor;
import org.example.parser.SigmaParser;
import org.example.semantic.SymbolTable;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.util.ArrayList;
import java.util.List;

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

    public SigmaCodeGenerator(SymbolTable symbolTable, String className) {
        this.symbolTable = symbolTable;
        this.className = className;
        this.errors = new ArrayList<>();
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
        // For now, skip variable declarations in bytecode
        // In a full implementation, we'd allocate local variables
        if (ctx.expression() != null) {
            visit(ctx.expression());
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
                visit(ctx.argumentList().expression(0));
            } else {
                mainMethod.push(""); // Default empty string
            }

            // Call println
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
            // String concatenation or numeric addition
            visit(ctx.expression(0));
            visit(ctx.expression(1));

            // For simplicity, assume string concatenation
            Type stringType = Type.getType(String.class);
            mainMethod.invokeVirtual(stringType, Method.getMethod("String concat (String)"));
        }
    }

    private boolean isOperator(String text) {
        return text.matches("[+\\-*/%.&|<>=!]+");
    }

    @Override
    public Void visitPrimary(SigmaParser.PrimaryContext ctx) {
        if (ctx.IDENTIFIER() != null) {
            String varName = ctx.IDENTIFIER().getText();
            // For simplicity, push variable name as string
            // In full implementation, we'd load from local variables
            mainMethod.push(varName);
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
            mainMethod.push(String.valueOf(value)); // Convert to string for simplicity
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