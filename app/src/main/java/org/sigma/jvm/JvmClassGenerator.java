package org.sigma.jvm;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.sigma.parser.Ast;
import org.sigma.semantics.SemanticResult;
import org.sigma.semantics.SigmaType;
import org.sigma.semantics.TypeRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Bytecode generator targeting the JVM using ASM.
 * Converts the semantic-checked AST into a {@code Script} class with:
 *  - default constructor
 *  - {@code static void run()} containing top-level statements
 *  - {@code static void main(String[])} delegating to {@code run}
 *  - Additional static methods for every top-level Sigma function
 */
public class JvmClassGenerator implements Opcodes {

    private static final String SCRIPT_CLASS = "Script";
    private static final Type STRING_TYPE = Type.getType(String.class);
    private static final Type STRING_BUILDER_TYPE = Type.getType(StringBuilder.class);

    private SemanticResult semanticResult;
    private Map<Ast.Expression, SigmaType> expressionTypes = Map.of();
    private TypeRegistry typeRegistry;
    private Ast.CompilationUnit compilationUnit;
    private final Map<String, MethodInfo> declaredMethods = new LinkedHashMap<>();
    private final List<Ast.Statement> topLevelStatements = new ArrayList<>();
    private MethodInfo entryRunMethod;
    private boolean hasSyntheticRun;

    public byte[] generate(SemanticResult semanticResult) {
        Objects.requireNonNull(semanticResult, "semanticResult");
        this.semanticResult = semanticResult;
        this.expressionTypes = semanticResult.getExpressionTypes();
        this.compilationUnit = semanticResult.getAst();
        this.typeRegistry = buildTypeRegistry(semanticResult);
        this.declaredMethods.clear();
        this.topLevelStatements.clear();
        this.entryRunMethod = null;
        this.hasSyntheticRun = false;

        collectMethodDeclarations();
        this.entryRunMethod = declaredMethods.get("run");

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(V17, ACC_PUBLIC | ACC_SUPER, SCRIPT_CLASS, null, "java/lang/Object", null);

        emitConstructor(cw);
        emitRunMethod(cw);
        emitUserMethods(cw);
        emitMainMethod(cw);

        cw.visitEnd();
        return cw.toByteArray();
    }

    private void emitConstructor(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void emitRunMethod(ClassWriter cw) {
        if (entryRunMethod != null) {
            if (!topLevelStatements.isEmpty()) {
                throw new UnsupportedOperationException(
                    "Cannot mix top-level statements with an explicit 'run' method.");
            }
            validateEntryPoint(entryRunMethod);
            hasSyntheticRun = false;
            return;
        }

        hasSyntheticRun = true;
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "run", "()V", null, null);
        LocalScope scope = LocalScope.root(0);
        MethodCompilationContext ctx = new MethodCompilationContext("run", TypeRegistry.VOID, mv, scope);

        mv.visitCode();
        for (Ast.Statement stmt : topLevelStatements) {
            emitStatement(stmt, ctx);
        }
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void emitMainMethod(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
        mv.visitCode();
        String descriptor = "()V";
        MethodInfo runInfo = declaredMethods.get("run");
        if (runInfo != null) {
            validateEntryPoint(runInfo);
            descriptor = runInfo.descriptor();
        }
        mv.visitMethodInsn(INVOKESTATIC, SCRIPT_CLASS, "run", descriptor, false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void emitUserMethods(ClassWriter cw) {
        for (MethodInfo methodInfo : declaredMethods.values()) {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC | ACC_STATIC,
                methodInfo.name(),
                methodInfo.descriptor(),
                null,
                null
            );
            mv.visitCode();

            LocalScope scope = LocalScope.root(0);
            // Pre-register parameters in definition order
            MethodCompilationContext ctx = new MethodCompilationContext(
                methodInfo.name(),
                methodInfo.returnType(),
                mv,
                scope
            );
            for (int i = 0; i < methodInfo.parameterTypes().size(); i++) {
                SigmaType paramType = methodInfo.parameterTypes().get(i);
                Ast.Parameter parameter = methodInfo.declaration().parameters.get(i);
                scope.declare(parameter.name, paramType);
            }

            emitBlock(methodInfo.declaration().body, ctx);

            if (isVoid(methodInfo.returnType())) {
                mv.visitInsn(RETURN);
            } else {
                // If control reaches here, no return was emitted.
                mv.visitTypeInsn(NEW, "java/lang/IllegalStateException");
                mv.visitInsn(DUP);
                mv.visitLdcInsn("Missing return statement in method '" + methodInfo.name() + "'");
                mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalStateException",
                    "<init>", "(Ljava/lang/String;)V", false);
                mv.visitInsn(ATHROW);
            }

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
    }

    private void emitBlock(Ast.Block block, MethodCompilationContext ctx) {
        MethodCompilationContext inner = ctx.withScope(ctx.scope.createChild());
        for (Ast.Statement statement : block.statements) {
            emitStatement(statement, inner);
        }
    }

    private void emitStatement(Ast.Statement statement, MethodCompilationContext ctx) {
        if (statement instanceof Ast.Block) {
            emitBlock((Ast.Block) statement, ctx);
        } else if (statement instanceof Ast.VariableDeclaration) {
            emitVariableDeclaration((Ast.VariableDeclaration) statement, ctx);
        } else if (statement instanceof Ast.Assignment) {
            emitAssignment((Ast.Assignment) statement, ctx);
        } else if (statement instanceof Ast.ExpressionStatement) {
            emitExpressionStatement((Ast.ExpressionStatement) statement, ctx);
        } else if (statement instanceof Ast.PrintStatement) {
            emitPrintStatement((Ast.PrintStatement) statement, ctx);
        } else if (statement instanceof Ast.IfStatement) {
            emitIfStatement((Ast.IfStatement) statement, ctx);
        } else if (statement instanceof Ast.WhileStatement) {
            emitWhileStatement((Ast.WhileStatement) statement, ctx);
        } else if (statement instanceof Ast.ReturnStatement) {
            emitReturnStatement((Ast.ReturnStatement) statement, ctx);
        } else if (statement instanceof Ast.ForEachStatement) {
            emitForEachStatement((Ast.ForEachStatement) statement, ctx);
        } else if (statement instanceof Ast.MethodDeclaration) {
            // Already emitted
        } else {
            throw unsupported(statement, "Unsupported statement: " + statement.getClass().getSimpleName());
        }
    }

    private void emitVariableDeclaration(Ast.VariableDeclaration decl, MethodCompilationContext ctx) {
        SigmaType varType = resolveType(decl.typeName, decl.line, decl.col);
        LocalVariable variable = ctx.scope.declare(decl.name, varType);
        if (decl.init != null) {
            emitExpression(decl.init, ctx);
            coerce(typeOf(decl.init), varType, ctx.mv);
            storeVariable(variable, ctx.mv);
        }
    }

    private void emitAssignment(Ast.Assignment assignment, MethodCompilationContext ctx) {
        LocalVariable variable = ctx.scope.resolve(assignment.name);
        if (variable == null) {
            throw unsupported(assignment, "Undefined variable '" + assignment.name + "' in JVM backend.");
        }
        emitExpression(assignment.value, ctx);
        coerce(typeOf(assignment.value), variable.type, ctx.mv);
        storeVariable(variable, ctx.mv);
    }

    private void emitExpressionStatement(Ast.ExpressionStatement stmt, MethodCompilationContext ctx) {
        SigmaType type = typeOf(stmt.expr);
        emitExpression(stmt.expr, ctx);
        popValueIfNeeded(type, ctx.mv);
    }

    private void emitPrintStatement(Ast.PrintStatement stmt, MethodCompilationContext ctx) {
        MethodVisitor mv = ctx.mv;
        SigmaType valueType = typeOf(stmt.expr);
        mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        emitExpression(stmt.expr, ctx);
        String descriptor;
        if (isString(valueType)) {
            descriptor = "(Ljava/lang/String;)V";
        } else if (isDouble(valueType)) {
            descriptor = "(D)V";
        } else if (isFloat(valueType)) {
            descriptor = "(F)V";
        } else if (isBoolean(valueType)) {
            descriptor = "(Z)V";
        } else if (isInt(valueType)) {
            descriptor = "(I)V";
        } else {
            descriptor = "(Ljava/lang/Object;)V";
        }
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", descriptor, false);
    }

    private void emitIfStatement(Ast.IfStatement stmt, MethodCompilationContext ctx) {
        MethodVisitor mv = ctx.mv;
        Label elseLabel = new Label();
        Label endLabel = new Label();

        emitExpression(stmt.cond, ctx);
        mv.visitJumpInsn(IFEQ, elseLabel);
        emitStatement(stmt.thenBranch, ctx);
        mv.visitJumpInsn(GOTO, endLabel);
        mv.visitLabel(elseLabel);
        if (stmt.elseBranch != null) {
            emitStatement(stmt.elseBranch, ctx);
        }
        mv.visitLabel(endLabel);
    }

    private void emitWhileStatement(Ast.WhileStatement stmt, MethodCompilationContext ctx) {
        MethodVisitor mv = ctx.mv;
        Label loopStart = new Label();
        Label loopEnd = new Label();
        mv.visitLabel(loopStart);
        emitExpression(stmt.cond, ctx);
        mv.visitJumpInsn(IFEQ, loopEnd);
        emitStatement(stmt.body, ctx);
        mv.visitJumpInsn(GOTO, loopStart);
        mv.visitLabel(loopEnd);
    }

    /**
     * Lower for-each (counting form) into while-loop with int iterator and bound.
     * Supported shape: for ([int] i in expr) body; where expr is an int bound.
     */
    private void emitForEachStatement(Ast.ForEachStatement stmt, MethodCompilationContext ctx) {
        MethodVisitor mv = ctx.mv;

        SigmaType iterableType = typeOf(stmt.iterable);
        if (!isInt(iterableType)) {
            throw unsupported(stmt, "JVM backend supports for-each only over int iterable (loop count)");
        }

        SigmaType iteratorType = TypeRegistry.INT;
        if (stmt.typeName != null) {
            iteratorType = resolveType(stmt.typeName, stmt.line, stmt.col);
        }
        if (!isInt(iteratorType)) {
            throw unsupported(stmt, "Iterator type must be int for JVM for-each lowering");
        }

        // Declare iterator and bound in current scope
        LocalVariable iteratorVar = ctx.scope.declare(stmt.iteratorName, iteratorType);
        String boundName = stmt.iteratorName + "$bound$";
        LocalVariable boundVar = ctx.scope.declare(boundName, TypeRegistry.INT);

        // bound = iterableExpr (coerce to int)
        emitExpression(stmt.iterable, ctx);
        coerce(typeOf(stmt.iterable), TypeRegistry.INT, mv);
        mv.visitVarInsn(ISTORE, boundVar.index);

        // iterator = 0
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, iteratorVar.index);

        Label startLabel = new Label();
        Label endLabel = new Label();

        mv.visitLabel(startLabel);
        // if iterator >= bound goto end
        mv.visitVarInsn(ILOAD, iteratorVar.index);
        mv.visitVarInsn(ILOAD, boundVar.index);
        mv.visitJumpInsn(IF_ICMPGE, endLabel);

        // body
        emitStatement(stmt.body, ctx);

        // iterator++
        mv.visitIincInsn(iteratorVar.index, 1);
        mv.visitJumpInsn(GOTO, startLabel);
        mv.visitLabel(endLabel);
    }

    private void emitReturnStatement(Ast.ReturnStatement stmt, MethodCompilationContext ctx) {
        if (isVoid(ctx.returnType)) {
            if (stmt.expr != null) {
                emitExpression(stmt.expr, ctx);
                popValueIfNeeded(typeOf(stmt.expr), ctx.mv);
            }
            ctx.mv.visitInsn(RETURN);
            return;
        }

        if (stmt.expr == null) {
            throw unsupported(stmt, "Missing return expression in non-void method '" + ctx.methodName + "'");
        }

        emitExpression(stmt.expr, ctx);
        coerce(typeOf(stmt.expr), ctx.returnType, ctx.mv);
        ctx.mv.visitInsn(returnOpcode(ctx.returnType));
    }

    private void emitExpression(Ast.Expression expr, MethodCompilationContext ctx) {
        if (expr instanceof Ast.IntLiteral) {
            ctx.mv.visitLdcInsn(((Ast.IntLiteral) expr).value);
        } else if (expr instanceof Ast.DoubleLiteral) {
            ctx.mv.visitLdcInsn(((Ast.DoubleLiteral) expr).value);
        } else if (expr instanceof Ast.StringLiteral) {
            ctx.mv.visitLdcInsn(((Ast.StringLiteral) expr).value);
        } else if (expr instanceof Ast.BooleanLiteral) {
            ctx.mv.visitInsn(((Ast.BooleanLiteral) expr).value ? ICONST_1 : ICONST_0);
        } else if (expr instanceof Ast.NullLiteral) {
            ctx.mv.visitInsn(ACONST_NULL);
        } else if (expr instanceof Ast.Identifier) {
            emitIdentifierLoad((Ast.Identifier) expr, ctx);
        } else if (expr instanceof Ast.Binary) {
            emitBinaryExpression((Ast.Binary) expr, ctx);
        } else if (expr instanceof Ast.Unary) {
            emitUnaryExpression((Ast.Unary) expr, ctx);
        } else if (expr instanceof Ast.Call) {
            emitCallExpression((Ast.Call) expr, ctx);
        } else {
            throw unsupported(expr, "Unsupported expression: " + expr.getClass().getSimpleName());
        }
    }

    private void emitUnaryExpression(Ast.Unary unary, MethodCompilationContext ctx) {
        if ("-".equals(unary.op)) {
            emitExpression(unary.expr, ctx);
            SigmaType targetType = typeOf(unary);
            coerce(typeOf(unary.expr), targetType, ctx.mv);
            if (isDouble(targetType)) {
                ctx.mv.visitInsn(DNEG);
            } else if (isFloat(targetType)) {
                ctx.mv.visitInsn(FNEG);
            } else {
                ctx.mv.visitInsn(INEG);
            }
        } else if ("!".equals(unary.op)) {
            emitExpression(unary.expr, ctx);
            emitBooleanNegation(ctx.mv);
        } else {
            throw unsupported(unary, "Unsupported unary operator: " + unary.op);
        }
    }

    private void emitBooleanNegation(MethodVisitor mv) {
        Label trueLabel = new Label();
        Label endLabel = new Label();
        mv.visitJumpInsn(IFEQ, trueLabel);
        mv.visitInsn(ICONST_0);
        mv.visitJumpInsn(GOTO, endLabel);
        mv.visitLabel(trueLabel);
        mv.visitInsn(ICONST_1);
        mv.visitLabel(endLabel);
    }

    private void emitBinaryExpression(Ast.Binary binary, MethodCompilationContext ctx) {
        switch (binary.op) {
            case "+":
                if (isString(typeOf(binary))) {
                    emitStringConcatenation(binary, ctx);
                } else {
                    emitNumericBinary(binary, ctx, IADD, FADD, DADD);
                }
                break;
            case "-":
                emitNumericBinary(binary, ctx, ISUB, FSUB, DSUB);
                break;
            case "*":
                emitNumericBinary(binary, ctx, IMUL, FMUL, DMUL);
                break;
            case "/":
                emitNumericBinary(binary, ctx, IDIV, FDIV, DDIV);
                break;
            case "%":
                emitNumericBinary(binary, ctx, IREM, FREM, DREM);
                break;
            case "**":
                emitPowerExpression(binary, ctx);
                break;
            case "==":
            case "!=":
                emitEqualityComparison(binary, ctx);
                break;
            case "<":
            case "<=":
            case ">":
            case ">=":
                emitRelationalComparison(binary, ctx);
                break;
            case "&&":
                emitLogicalAnd(binary, ctx);
                break;
            case "||":
                emitLogicalOr(binary, ctx);
                break;
            default:
                throw unsupported(binary, "Unsupported binary operator: " + binary.op);
        }
    }

    private void emitLogicalAnd(Ast.Binary binary, MethodCompilationContext ctx) {
        MethodVisitor mv = ctx.mv;
        Label falseLabel = new Label();
        Label endLabel = new Label();

        emitExpression(binary.left, ctx);
        mv.visitJumpInsn(IFEQ, falseLabel);
        emitExpression(binary.right, ctx);
        mv.visitJumpInsn(IFEQ, falseLabel);
        mv.visitInsn(ICONST_1);
        mv.visitJumpInsn(GOTO, endLabel);

        mv.visitLabel(falseLabel);
        mv.visitInsn(ICONST_0);
        mv.visitLabel(endLabel);
    }

    private void emitLogicalOr(Ast.Binary binary, MethodCompilationContext ctx) {
        MethodVisitor mv = ctx.mv;
        Label trueLabel = new Label();
        Label endLabel = new Label();

        emitExpression(binary.left, ctx);
        mv.visitJumpInsn(IFNE, trueLabel);
        emitExpression(binary.right, ctx);
        mv.visitJumpInsn(IFNE, trueLabel);
        mv.visitInsn(ICONST_0);
        mv.visitJumpInsn(GOTO, endLabel);

        mv.visitLabel(trueLabel);
        mv.visitInsn(ICONST_1);
        mv.visitLabel(endLabel);
    }

    private void emitRelationalComparison(Ast.Binary binary, MethodCompilationContext ctx) {
        MethodVisitor mv = ctx.mv;
        SigmaType leftType = typeOf(binary.left);
        SigmaType rightType = typeOf(binary.right);
        SigmaType common = commonNumericType(leftType, rightType);

        emitExpression(binary.left, ctx);
        coerce(leftType, common, mv);
        emitExpression(binary.right, ctx);
        coerce(rightType, common, mv);

        Label trueLabel = new Label();
        Label endLabel = new Label();

        if (isDouble(common)) {
            mv.visitInsn(DCMPG);
            emitComparisonJump(binary.op, trueLabel, mv);
        } else if (isFloat(common)) {
            mv.visitInsn(FCMPG);
            emitComparisonJump(binary.op, trueLabel, mv);
        } else {
            emitIntComparisonJump(binary.op, trueLabel, mv);
        }

        mv.visitInsn(ICONST_0);
        mv.visitJumpInsn(GOTO, endLabel);
        mv.visitLabel(trueLabel);
        mv.visitInsn(ICONST_1);
        mv.visitLabel(endLabel);
    }

    private void emitEqualityComparison(Ast.Binary binary, MethodCompilationContext ctx) {
        MethodVisitor mv = ctx.mv;
        SigmaType leftType = typeOf(binary.left);
        SigmaType rightType = typeOf(binary.right);
        boolean numeric = isNumeric(leftType) && isNumeric(rightType);
        boolean booleanType = isBoolean(leftType) && isBoolean(rightType);

        if (numeric || booleanType) {
            SigmaType common = numeric ? commonNumericType(leftType, rightType) : TypeRegistry.BOOLEAN;
            emitExpression(binary.left, ctx);
            coerce(leftType, common, mv);
            emitExpression(binary.right, ctx);
            coerce(rightType, common, mv);
            Label trueLabel = new Label();
            Label endLabel = new Label();
            if (isDouble(common)) {
                mv.visitInsn(DCMPG);
                mv.visitJumpInsn(binary.op.equals("==") ? IFEQ : IFNE, trueLabel);
            } else if (isFloat(common)) {
                mv.visitInsn(FCMPG);
                mv.visitJumpInsn(binary.op.equals("==") ? IFEQ : IFNE, trueLabel);
            } else {
                mv.visitJumpInsn(binary.op.equals("==") ? IF_ICMPEQ : IF_ICMPNE, trueLabel);
            }
            mv.visitInsn(ICONST_0);
            mv.visitJumpInsn(GOTO, endLabel);
            mv.visitLabel(trueLabel);
            mv.visitInsn(ICONST_1);
            mv.visitLabel(endLabel);
            return;
        }

        if (!leftType.isReference() || !rightType.isReference()) {
            throw unsupported(binary, "Equality between incompatible types: " +
                leftType + " and " + rightType);
        }

        emitExpression(binary.left, ctx);
        emitExpression(binary.right, ctx);
        mv.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "equals",
            "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
        if (binary.op.equals("!=")) {
            emitBooleanNegation(mv);
        }
    }

    private void emitPowerExpression(Ast.Binary binary, MethodCompilationContext ctx) {
        MethodVisitor mv = ctx.mv;
        emitExpression(binary.left, ctx);
        coerce(typeOf(binary.left), TypeRegistry.DOUBLE, mv);
        emitExpression(binary.right, ctx);
        coerce(typeOf(binary.right), TypeRegistry.DOUBLE, mv);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "pow", "(DD)D", false);
        SigmaType resultType = typeOf(binary);
        if (isInt(resultType)) {
            mv.visitInsn(D2I);
        } else if (isFloat(resultType)) {
            mv.visitInsn(D2F);
        }
    }

    private void emitNumericBinary(Ast.Binary binary,
                                   MethodCompilationContext ctx,
                                   int intOpcode,
                                   int floatOpcode,
                                   int doubleOpcode) {
        MethodVisitor mv = ctx.mv;
        SigmaType resultType = typeOf(binary);
        SigmaType leftType = typeOf(binary.left);
        SigmaType rightType = typeOf(binary.right);

        emitExpression(binary.left, ctx);
        coerce(leftType, resultType, mv);
        emitExpression(binary.right, ctx);
        coerce(rightType, resultType, mv);

        if (isDouble(resultType)) {
            mv.visitInsn(doubleOpcode);
        } else if (isFloat(resultType)) {
            mv.visitInsn(floatOpcode);
        } else {
            mv.visitInsn(intOpcode);
        }
    }

    private void emitStringConcatenation(Ast.Binary binary, MethodCompilationContext ctx) {
        MethodVisitor mv = ctx.mv;
        mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);

        appendExpressionToBuilder(binary.left, ctx);
        appendExpressionToBuilder(binary.right, ctx);

        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
    }

    private void appendExpressionToBuilder(Ast.Expression expression, MethodCompilationContext ctx) {
        MethodVisitor mv = ctx.mv;
        SigmaType type = typeOf(expression);
        mv.visitInsn(DUP);
        emitExpression(expression, ctx);

        String descriptor;
        if (isString(type)) {
            descriptor = "(Ljava/lang/String;)Ljava/lang/StringBuilder;";
        } else if (isDouble(type)) {
            descriptor = "(D)Ljava/lang/StringBuilder;";
        } else if (isFloat(type)) {
            descriptor = "(F)Ljava/lang/StringBuilder;";
        } else if (isInt(type)) {
            descriptor = "(I)Ljava/lang/StringBuilder;";
        } else if (isBoolean(type)) {
            descriptor = "(Z)Ljava/lang/StringBuilder;";
        } else {
            descriptor = "(Ljava/lang/Object;)Ljava/lang/StringBuilder;";
        }

        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", descriptor, false);
    }

    private void emitCallExpression(Ast.Call call, MethodCompilationContext ctx) {
        if (!(call.target instanceof Ast.Identifier)) {
            throw unsupported(call, "Only direct function calls are supported.");
        }
        String functionName = ((Ast.Identifier) call.target).name;
        MethodInfo method = declaredMethods.get(functionName);
        if (method == null) {
            throw unsupported(call, "Unknown function '" + functionName + "' for JVM backend.");
        }
        if (call.args.size() != method.parameterTypes().size()) {
            throw unsupported(call, "Argument count mismatch when calling '" + functionName + "'");
        }

        MethodVisitor mv = ctx.mv;
        for (int i = 0; i < call.args.size(); i++) {
            Ast.Expression arg = call.args.get(i);
            SigmaType expectedType = method.parameterTypes().get(i);
            emitExpression(arg, ctx);
            coerce(typeOf(arg), expectedType, mv);
        }

        mv.visitMethodInsn(INVOKESTATIC, SCRIPT_CLASS, method.name(), method.descriptor(), false);
    }

    private void emitIdentifierLoad(Ast.Identifier identifier, MethodCompilationContext ctx) {
        LocalVariable variable = ctx.scope.resolve(identifier.name);
        if (variable == null) {
            throw unsupported(identifier, "Undefined variable '" + identifier.name + "'");
        }
        ctx.mv.visitVarInsn(loadOpcode(variable.type), variable.index);
    }

    private void popValueIfNeeded(SigmaType type, MethodVisitor mv) {
        if (isVoid(type)) {
            return;
        }
        if (type != null && slotWidth(type) == 2) {
            mv.visitInsn(POP2);
        } else {
            mv.visitInsn(POP);
        }
    }

    private void storeVariable(LocalVariable variable, MethodVisitor mv) {
        mv.visitVarInsn(storeOpcode(variable.type), variable.index);
    }

    private void emitComparisonJump(String op, Label trueLabel, MethodVisitor mv) {
        switch (op) {
            case "<":
                mv.visitJumpInsn(IFLT, trueLabel);
                break;
            case "<=":
                mv.visitJumpInsn(IFLE, trueLabel);
                break;
            case ">":
                mv.visitJumpInsn(IFGT, trueLabel);
                break;
            case ">=":
                mv.visitJumpInsn(IFGE, trueLabel);
                break;
            default:
                throw new IllegalStateException("Unexpected comparison operator: " + op);
        }
    }

    private void emitIntComparisonJump(String op, Label trueLabel, MethodVisitor mv) {
        switch (op) {
            case "<":
                mv.visitJumpInsn(IF_ICMPLT, trueLabel);
                break;
            case "<=":
                mv.visitJumpInsn(IF_ICMPLE, trueLabel);
                break;
            case ">":
                mv.visitJumpInsn(IF_ICMPGT, trueLabel);
                break;
            case ">=":
                mv.visitJumpInsn(IF_ICMPGE, trueLabel);
                break;
            default:
                throw new IllegalStateException("Unexpected comparison operator: " + op);
        }
    }

    // ========================= Helper methods =========================

    private void collectMethodDeclarations() {
        for (Ast.Statement stmt : compilationUnit.statements) {
            if (stmt instanceof Ast.MethodDeclaration) {
                registerMethod((Ast.MethodDeclaration) stmt);
            } else if (stmt instanceof Ast.ClassDeclaration) {
                collectClassMembers((Ast.ClassDeclaration) stmt);
            } else {
                topLevelStatements.add(stmt);
            }
        }
    }

    private void collectClassMembers(Ast.ClassDeclaration classDecl) {
        for (Ast.Statement member : classDecl.members) {
            if (member instanceof Ast.MethodDeclaration) {
                registerMethod((Ast.MethodDeclaration) member);
            } else if (member instanceof Ast.VariableDeclaration || member instanceof Ast.FieldDeclaration) {
                throw unsupported(member, "Class fields are not supported by the JVM backend yet.");
            } else {
                throw unsupported(member, "Unsupported class member: " + member.getClass().getSimpleName());
            }
        }
    }

    private void registerMethod(Ast.MethodDeclaration method) {
        SigmaType returnType = resolveType(method.returnType, method.line, method.col);
        List<SigmaType> parameters = new ArrayList<>();
        for (Ast.Parameter parameter : method.parameters) {
            parameters.add(resolveType(parameter.type, parameter.line, parameter.col));
        }
        String descriptor = buildDescriptor(returnType, parameters);
        declaredMethods.put(method.name,
            new MethodInfo(method.name, method, returnType, parameters, descriptor));
    }

    private SigmaType resolveType(String typeName, int line, int col) {
        SigmaType type = typeRegistry.resolve(typeName);
        if (type == TypeRegistry.ERROR) {
            throw new IllegalStateException(
                "Unknown type '" + typeName + "' at line " + line + ":" + col + " for JVM backend");
        }
        return type;
    }

    private SigmaType typeOf(Ast.Expression expression) {
        SigmaType type = expressionTypes.get(expression);
        if (type == null) {
            throw new IllegalStateException("Missing type information for expression: " + expression);
        }
        return type;
    }

    private boolean isVoid(SigmaType type) {
        return type == null || "void".equals(type.getName());
    }

    private boolean isInt(SigmaType type) {
        return type != null && "int".equals(type.getName());
    }

    private boolean isFloat(SigmaType type) {
        return type != null && "float".equals(type.getName());
    }

    private boolean isDouble(SigmaType type) {
        return type != null && "double".equals(type.getName());
    }

    private boolean isString(SigmaType type) {
        return type != null && "String".equals(type.getName());
    }

    private boolean isBoolean(SigmaType type) {
        return type != null && "boolean".equals(type.getName());
    }

    private boolean isNumeric(SigmaType type) {
        return isInt(type) || isFloat(type) || isDouble(type);
    }

    private void coerce(SigmaType from, SigmaType to, MethodVisitor mv) {
        if (from == null || to == null || from.equals(to)) {
            return;
        }
        String fromName = from.getName();
        String toName = to.getName();
        if (fromName.equals("int") && toName.equals("double")) {
            mv.visitInsn(I2D);
            return;
        }
        if (fromName.equals("int") && toName.equals("float")) {
            mv.visitInsn(I2F);
            return;
        }
        if (fromName.equals("float") && toName.equals("double")) {
            mv.visitInsn(F2D);
            return;
        }
        if (fromName.equals("double") && toName.equals("float")) {
            mv.visitInsn(D2F);
            return;
        }
        if (fromName.equals("double") && toName.equals("int")) {
            mv.visitInsn(D2I);
            return;
        }
        if (fromName.equals("float") && toName.equals("int")) {
            mv.visitInsn(F2I);
            return;
        }
        if (toName.equals("String")) {
            // Caller should handle conversion to String through append/println
            return;
        }
        if (from instanceof SigmaType.NullType && to.isReference()) {
            return;
        }
        if (fromName.equals("boolean") && toName.equals("int")) {
            return; // already int on stack
        }
        throw new UnsupportedOperationException("Cannot coerce type " + fromName + " to " + toName);
    }

    private int loadOpcode(SigmaType type) {
        if (isDouble(type)) {
            return DLOAD;
        }
        if (isFloat(type)) {
            return FLOAD;
        }
        if (isInt(type) || isBoolean(type)) {
            return ILOAD;
        }
        return ALOAD;
    }

    private int storeOpcode(SigmaType type) {
        if (isDouble(type)) {
            return DSTORE;
        }
        if (isFloat(type)) {
            return FSTORE;
        }
        if (isInt(type) || isBoolean(type)) {
            return ISTORE;
        }
        return ASTORE;
    }

    private int returnOpcode(SigmaType type) {
        if (isDouble(type)) {
            return DRETURN;
        }
        if (isFloat(type)) {
            return FRETURN;
        }
        if (isInt(type) || isBoolean(type)) {
            return IRETURN;
        }
        return ARETURN;
    }

    private int slotWidth(SigmaType type) {
        return isDouble(type) ? 2 : 1;
    }

    private SigmaType commonNumericType(SigmaType a, SigmaType b) {
        if (isDouble(a) || isDouble(b)) {
            return TypeRegistry.DOUBLE;
        }
        if (isFloat(a) || isFloat(b)) {
            return TypeRegistry.FLOAT;
        }
        return TypeRegistry.INT;
    }

    private String buildDescriptor(SigmaType returnType, List<SigmaType> parameterTypes) {
        Type[] args = new Type[parameterTypes.size()];
        for (int i = 0; i < parameterTypes.size(); i++) {
            args[i] = toAsmType(parameterTypes.get(i));
        }
        Type ret = toAsmType(returnType);
        return Type.getMethodDescriptor(ret, args);
    }

    private Type toAsmType(SigmaType type) {
        if (isVoid(type)) {
            return Type.VOID_TYPE;
        }
        if (isInt(type) || isBoolean(type)) {
            return Type.INT_TYPE;
        }
        if (isFloat(type)) {
            return Type.FLOAT_TYPE;
        }
        if (isDouble(type)) {
            return Type.DOUBLE_TYPE;
        }
        if (isString(type)) {
            return STRING_TYPE;
        }
        // Default to Object reference for other classes
        return Type.getType("L" + type.getName().replace('.', '/') + ";");
    }

    private void validateEntryPoint(MethodInfo methodInfo) {
        if (!isVoid(methodInfo.returnType())) {
            throw new UnsupportedOperationException("Entry method 'run' must have void return type.");
        }
        if (!methodInfo.parameterTypes().isEmpty()) {
            throw new UnsupportedOperationException("Entry method 'run' cannot declare parameters.");
        }
    }

    private RuntimeException unsupported(Object node, String message) {
        return new UnsupportedOperationException(message + " (" + node + ")");
    }

    private TypeRegistry buildTypeRegistry(SemanticResult result) {
        TypeRegistry registry = new TypeRegistry();
        for (String className : result.getClassInfos().keySet()) {
            registry.registerClass(className);
        }
        return registry;
    }

    // ========================= Helper records/classes =========================

    private record MethodInfo(
        String name,
        Ast.MethodDeclaration declaration,
        SigmaType returnType,
        List<SigmaType> parameterTypes,
        String descriptor
    ) {}

    private static final class MethodCompilationContext {
        final String methodName;
        final SigmaType returnType;
        final MethodVisitor mv;
        final LocalScope scope;

        MethodCompilationContext(String methodName, SigmaType returnType,
                                 MethodVisitor mv, LocalScope scope) {
            this.methodName = methodName;
            this.returnType = returnType;
            this.mv = mv;
            this.scope = scope;
        }

        MethodCompilationContext withScope(LocalScope newScope) {
            return new MethodCompilationContext(methodName, returnType, mv, newScope);
        }
    }

    private static final class LocalScope {
        private final LocalScope parent;
        private final SlotAllocator allocator;
        private final Map<String, LocalVariable> locals = new LinkedHashMap<>();

        private LocalScope(LocalScope parent, SlotAllocator allocator) {
            this.parent = parent;
            this.allocator = allocator;
        }

        static LocalScope root(int startIndex) {
            return new LocalScope(null, new SlotAllocator(startIndex));
        }

        LocalScope createChild() {
            return new LocalScope(this, allocator);
        }

        LocalVariable declare(String name, SigmaType type) {
            if (locals.containsKey(name)) {
                throw new IllegalStateException("Variable '" + name + "' already declared in this scope.");
            }
            int slot = allocator.reserve(type);
            LocalVariable variable = new LocalVariable(name, slot, type);
            locals.put(name, variable);
            return variable;
        }

        LocalVariable resolve(String name) {
            LocalVariable local = locals.get(name);
            if (local != null) {
                return local;
            }
            if (parent != null) {
                return parent.resolve(name);
            }
            return null;
        }
    }

    private record LocalVariable(String name, int index, SigmaType type) {}

    private static final class SlotAllocator {
        private int nextIndex;

        SlotAllocator(int start) {
            this.nextIndex = start;
        }

        int reserve(SigmaType type) {
            int slot = nextIndex;
            nextIndex += ("double".equals(type.getName())) ? 2 : 1;
            return slot;
        }
    }
}
