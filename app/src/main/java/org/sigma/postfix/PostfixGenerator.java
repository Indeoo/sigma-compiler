package org.sigma.postfix;

import org.sigma.parser.Ast;
import org.sigma.semantics.SemanticResult;
import org.sigma.semantics.SigmaType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts the Script.run() AST method into a .postfix program compatible with PSM.py.
 * This backend intentionally supports only primitive statements that appear inside the
 * synthetic Script wrapper (variable declarations, assignments, if/while, expressions).
 */
public class PostfixGenerator {
    private Map<Ast.Expression, SigmaType> expressionTypes = Map.of();
    private Map<String, PostfixFunction> functionsByName = Map.of();
    private Map<Ast.Expression, SigmaType> expressionCoercions = Map.of();

    public PostfixBundle generate(SemanticResult semanticResult) {
        this.expressionTypes = semanticResult.getExpressionTypes();
        this.expressionCoercions = semanticResult.getExpressionCoercions();

        Ast.MethodDeclaration scriptEntry = findScriptEntry(semanticResult.getAst());
        if (scriptEntry == null) {
            throw new IllegalStateException("Script.run() method not found. Did you run ScriptWrappingTransformer?");
        }

        List<Ast.MethodDeclaration> helperMethods = findHelperMethods(semanticResult.getAst(), scriptEntry);
        List<PostfixFunction> functions = buildFunctionMetadata(helperMethods);
        this.functionsByName = new LinkedHashMap<>();
        for (PostfixFunction function : functions) {
            functionsByName.put(function.name(), function);
        }

        GenerationContext mainCtx = GenerationContext.forMain();
        mainCtx.functionEndLabel = mainCtx.newLabel();
        emitBlock(scriptEntry.body, mainCtx);
        mainCtx.defineLabel(mainCtx.functionEndLabel);
        PostfixProgram mainProgram = mainCtx.buildProgram(functions);

        Map<String, PostfixProgram> functionPrograms = new LinkedHashMap<>();
        for (Ast.MethodDeclaration method : helperMethods) {
            String returnType = mapType(method.returnType);
            GenerationContext fnCtx = GenerationContext.forFunction(returnType);
            registerParameters(method, fnCtx);
            emitBlock(method.body, fnCtx);
            fnCtx.ensureFunctionTermination(method.name);
            PostfixProgram fnProgram = fnCtx.buildProgram(functions);
            functionPrograms.put(method.name, fnProgram);
        }

        return new PostfixBundle(mainProgram, functionPrograms, functions);
    }

    private Ast.MethodDeclaration findScriptEntry(Ast.CompilationUnit unit) {
        for (Ast.Statement stmt : unit.statements) {
            if (stmt instanceof Ast.ClassDeclaration) {
                Ast.ClassDeclaration cls = (Ast.ClassDeclaration) stmt;
                if ("Script".equals(cls.name)) {
                    for (Ast.Statement member : cls.members) {
                        if (member instanceof Ast.MethodDeclaration) {
                            Ast.MethodDeclaration method = (Ast.MethodDeclaration) member;
                            if ("run".equals(method.name)) {
                                return method;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private List<Ast.MethodDeclaration> findHelperMethods(Ast.CompilationUnit unit, Ast.MethodDeclaration runMethod) {
        List<Ast.MethodDeclaration> helpers = new ArrayList<>();
        for (Ast.Statement stmt : unit.statements) {
            if (stmt instanceof Ast.ClassDeclaration) {
                Ast.ClassDeclaration cls = (Ast.ClassDeclaration) stmt;
                if ("Script".equals(cls.name)) {
                    for (Ast.Statement member : cls.members) {
                        if (member instanceof Ast.MethodDeclaration) {
                            Ast.MethodDeclaration method = (Ast.MethodDeclaration) member;
                            if (!"run".equals(method.name)) {
                                helpers.add(method);
                            }
                        }
                    }
                }
            }
        }
        return helpers;
    }

    private List<PostfixFunction> buildFunctionMetadata(List<Ast.MethodDeclaration> methods) {
        List<PostfixFunction> functions = new ArrayList<>();
        for (Ast.MethodDeclaration method : methods) {
            String type = mapType(method.returnType);
            functions.add(new PostfixFunction(method.name, type, method.parameters.size()));
        }
        return functions;
    }

    private void registerParameters(Ast.MethodDeclaration method, GenerationContext ctx) {
        for (Ast.Parameter parameter : method.parameters) {
            ctx.registerVariable(parameter.name, mapType(parameter.type));
        }
    }

    private void emitBlock(Ast.Block block, GenerationContext ctx) {
        for (Ast.Statement stmt : block.statements) {
            emitStatement(stmt, ctx);
        }
    }

    private void emitStatement(Ast.Statement stmt, GenerationContext ctx) {
        if (stmt instanceof Ast.VariableDeclaration) {
            emitVariableDeclaration((Ast.VariableDeclaration) stmt, ctx);
        } else if (stmt instanceof Ast.Assignment) {
            emitAssignment((Ast.Assignment) stmt, ctx);
        } else if (stmt instanceof Ast.ExpressionStatement) {
            Ast.Expression expr = ((Ast.ExpressionStatement) stmt).expr;
            emitExpression(expr, ctx);
            if (expressionProducesValue(expr)) {
                ctx.instructions.add(new PostfixInstruction("POP", "stack_op"));
            }
        } else if (stmt instanceof Ast.PrintStatement) {
            emitExpression(((Ast.PrintStatement) stmt).expr, ctx);
            ctx.instructions.add(new PostfixInstruction("PRINT", "out_op"));
        } else if (stmt instanceof Ast.IfStatement) {
            emitIfStatement((Ast.IfStatement) stmt, ctx);
        } else if (stmt instanceof Ast.WhileStatement) {
            emitWhileStatement((Ast.WhileStatement) stmt, ctx);
        } else if (stmt instanceof Ast.ForEachStatement) {
            emitForEach((Ast.ForEachStatement) stmt, ctx);
        } else if (stmt instanceof Ast.Block) {
            emitBlock((Ast.Block) stmt, ctx);
        } else if (stmt instanceof Ast.ReturnStatement) {
            emitReturn((Ast.ReturnStatement) stmt, ctx);
        } else {
            throw new UnsupportedOperationException("Unsupported statement for postfix backend: " + stmt.getClass().getSimpleName());
        }
    }

    private void emitVariableDeclaration(Ast.VariableDeclaration varDecl, GenerationContext ctx) {
        ctx.registerVariable(varDecl.name, mapType(varDecl.typeName));
        if (varDecl.init != null) {
            ctx.instructions.add(new PostfixInstruction(varDecl.name, "l-val"));
            emitExpression(varDecl.init, ctx);
            ctx.instructions.add(new PostfixInstruction("=", "assign_op"));
        }
    }

    private boolean expressionProducesValue(Ast.Expression expr) {
        if (expressionTypes == null) {
            return true;
        }
        SigmaType type = expressionTypes.get(expr);
        return type == null || !"void".equals(type.getName());
    }

    private void emitAssignment(Ast.Assignment assignment, GenerationContext ctx) {
        ctx.instructions.add(new PostfixInstruction(assignment.name, "l-val"));
        emitExpression(assignment.value, ctx);
        ctx.instructions.add(new PostfixInstruction("=", "assign_op"));
    }

    private void emitReturn(Ast.ReturnStatement ret, GenerationContext ctx) {
        if (ctx.isFunction) {
            if (!"void".equals(ctx.returnType)) {
                if (ret.expr == null) {
                    throw new UnsupportedOperationException("Function must return a value of type " + ctx.returnType);
                }
                emitExpression(ret.expr, ctx);
            } else if (ret.expr != null) {
                emitExpression(ret.expr, ctx);
                ctx.instructions.add(new PostfixInstruction("POP", "stack_op"));
            }
            ctx.instructions.add(new PostfixInstruction("RET", "RET"));
            ctx.hasReturn = true;
        } else {
            if (ret.expr != null) {
                emitExpression(ret.expr, ctx);
                ctx.instructions.add(new PostfixInstruction("POP", "stack_op"));
            }
            if (ctx.functionEndLabel != null) {
                ctx.instructions.add(new PostfixInstruction(ctx.functionEndLabel, "label"));
                ctx.instructions.add(new PostfixInstruction("JMP", "jump"));
            }
        }
    }

    private void emitIfStatement(Ast.IfStatement ifStmt, GenerationContext ctx) {
        String elseLabel = ctx.newLabel();
        String endLabel = ctx.newLabel();

        emitExpression(ifStmt.cond, ctx);
        ctx.instructions.add(new PostfixInstruction(elseLabel, "label"));
        ctx.instructions.add(new PostfixInstruction("JF", "jf"));

        emitStatement(ifStmt.thenBranch, ctx);
        ctx.instructions.add(new PostfixInstruction(endLabel, "label"));
        ctx.instructions.add(new PostfixInstruction("JMP", "jump"));

        ctx.defineLabel(elseLabel);
        if (ifStmt.elseBranch != null) {
            emitStatement(ifStmt.elseBranch, ctx);
        }
        ctx.defineLabel(endLabel);
    }

    private void emitWhileStatement(Ast.WhileStatement whileStmt, GenerationContext ctx) {
        String startLabel = ctx.newLabel();
        String endLabel = ctx.newLabel();

        ctx.defineLabel(startLabel);
        emitExpression(whileStmt.cond, ctx);
        ctx.instructions.add(new PostfixInstruction(endLabel, "label"));
        ctx.instructions.add(new PostfixInstruction("JF", "jf"));

        emitStatement(whileStmt.body, ctx);
        ctx.instructions.add(new PostfixInstruction(startLabel, "label"));
        ctx.instructions.add(new PostfixInstruction("JMP", "jump"));
        ctx.defineLabel(endLabel);
    }

    /**
     * Lower a for-each into a counter-based while loop.
     * Semantics currently only supports primitive iterables; here we assume an int bound and iterate 0..bound-1.
     */
    private void emitForEach(Ast.ForEachStatement forStmt, GenerationContext ctx) {
        SigmaType iterableType = expressionTypes.get(forStmt.iterable);
        if (iterableType == null || !"int".equals(iterableType.getName())) {
            throw new UnsupportedOperationException("Postfix backend supports for-each only over int iterable (loop count)");
        }

        String iteratorName = forStmt.iteratorName;
        String boundName = iteratorName + "$bound$" + ctx.labelCounter;

        String iteratorType = forStmt.typeName != null ? mapType(forStmt.typeName) : "int";
        ctx.registerVariable(iteratorName, iteratorType);
        ctx.registerVariable(boundName, "int");

        // bound = iterableExpr
        ctx.instructions.add(new PostfixInstruction(boundName, "l-val"));
        emitExpression(forStmt.iterable, ctx);
        ctx.instructions.add(new PostfixInstruction("=", "assign_op"));

        // iterator = 0
        ctx.instructions.add(new PostfixInstruction(iteratorName, "l-val"));
        ctx.instructions.add(new PostfixInstruction("0", "int"));
        ctx.instructions.add(new PostfixInstruction("=", "assign_op"));

        String startLabel = ctx.newLabel();
        String endLabel = ctx.newLabel();

        ctx.defineLabel(startLabel);
        // condition: iterator < bound
        ctx.instructions.add(new PostfixInstruction(iteratorName, "r-val"));
        ctx.instructions.add(new PostfixInstruction(boundName, "r-val"));
        ctx.instructions.add(new PostfixInstruction("<", "rel_op"));
        ctx.instructions.add(new PostfixInstruction(endLabel, "label"));
        ctx.instructions.add(new PostfixInstruction("JF", "jf"));

        // body
        emitStatement(forStmt.body, ctx);

        // iterator = iterator + 1
        ctx.instructions.add(new PostfixInstruction(iteratorName, "l-val"));
        ctx.instructions.add(new PostfixInstruction(iteratorName, "r-val"));
        ctx.instructions.add(new PostfixInstruction("1", "int"));
        ctx.instructions.add(new PostfixInstruction("+", "math_op"));
        ctx.instructions.add(new PostfixInstruction("=", "assign_op"));

        // jump to start
        ctx.instructions.add(new PostfixInstruction(startLabel, "label"));
        ctx.instructions.add(new PostfixInstruction("JMP", "jump"));
        ctx.defineLabel(endLabel);
    }

    private void emitExpression(Ast.Expression expr, GenerationContext ctx) {
        if (expr instanceof Ast.IntLiteral) {
            ctx.instructions.add(new PostfixInstruction(Integer.toString(((Ast.IntLiteral) expr).value), "int"));
        } else if (expr instanceof Ast.DoubleLiteral) {
            ctx.instructions.add(new PostfixInstruction(Double.toString(((Ast.DoubleLiteral) expr).value), "float"));
        } else if (expr instanceof Ast.BooleanLiteral) {
            ctx.instructions.add(new PostfixInstruction(Boolean.toString(((Ast.BooleanLiteral) expr).value), "bool"));
        } else if (expr instanceof Ast.StringLiteral) {
            ctx.instructions.add(new PostfixInstruction(
                encodeStringLiteral(((Ast.StringLiteral) expr).value),
                "string"));
        } else if (expr instanceof Ast.Identifier) {
            ctx.instructions.add(new PostfixInstruction(((Ast.Identifier) expr).name, "r-val"));
        } else if (expr instanceof Ast.Binary) {
            Ast.Binary binary = (Ast.Binary) expr;
            emitExpression(binary.left, ctx);
            emitExpression(binary.right, ctx);
            ctx.instructions.add(new PostfixInstruction(mapBinaryOperator(binary.op), binaryTokenType(binary.op)));
        } else if (expr instanceof Ast.Unary) {
            Ast.Unary unary = (Ast.Unary) expr;
            emitExpression(unary.expr, ctx);
            ctx.instructions.add(new PostfixInstruction(mapUnaryOperator(unary.op), unaryTokenType(unary.op)));
        } else if (expr instanceof Ast.Call) {
            emitCall((Ast.Call) expr, ctx);
        } else {
            throw new UnsupportedOperationException("Unsupported expression for postfix backend: " + expr.getClass().getSimpleName());
        }
        applyCoercion(expr, ctx);
    }

    private String mapBinaryOperator(String op) {
        switch (op) {
            case "+":
            case "-":
            case "*":
            case "/":
            case "%":
                return op;
            case "**":
                return "^";
            case "<":
            case "<=":
            case ">":
            case ">=":
            case "==":
            case "!=":
                return op;
            case "&&":
                return "AND";
            case "||":
                return "OR";
            default:
                throw new UnsupportedOperationException("Unsupported binary operator: " + op);
        }
    }

    private String binaryTokenType(String op) {
        switch (op) {
            case "+":
            case "-":
            case "*":
            case "/":
            case "%":
                return "math_op";
            case "**":
                return "pow_op";
            case "<":
            case "<=":
            case ">":
            case ">=":
            case "==":
            case "!=":
                return "rel_op";
            case "&&":
            case "||":
                return "bool_op";
            default:
                throw new UnsupportedOperationException("Unsupported binary operator token type: " + op);
        }
    }

    private String mapUnaryOperator(String op) {
        switch (op) {
            case "-":
            case "neg":
                return "NEG";
            case "!":
                return "NOT";
            default:
                throw new UnsupportedOperationException("Unsupported unary operator: " + op);
        }
    }

    private String unaryTokenType(String op) {
        switch (op) {
            case "-":
            case "neg":
                return "math_op";
            case "!":
                return "bool_op";
            default:
                throw new UnsupportedOperationException("Unsupported unary operator token type: " + op);
        }
    }

    private void emitCall(Ast.Call call, GenerationContext ctx) {
        if (!(call.target instanceof Ast.Identifier)) {
            throw new UnsupportedOperationException("Only simple function calls are supported in Postfix backend.");
        }
        Ast.Identifier id = (Ast.Identifier) call.target;
        for (Ast.Expression arg : call.args) {
            emitExpression(arg, ctx);
        }
        if (!functionsByName.containsKey(id.name)) {
            throw new UnsupportedOperationException("Function '" + id.name + "' is not supported by the Postfix backend.");
        }
        ctx.instructions.add(new PostfixInstruction(id.name, "CALL"));
    }

    private String encodeStringLiteral(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 2);
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\n':
                    sb.append("\\n");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        sb.append('"');
        return sb.toString();
    }

    private String mapType(String typeName) {
        switch (typeName) {
            case "int":
                return "int";
            case "double":
            case "float":
                return "float";
            case "boolean":
                return "bool";
            case "String":
                return "string";
            case "void":
                return "void";
            default:
                return typeName;
        }
    }

    private void applyCoercion(Ast.Expression expr, GenerationContext ctx) {
        SigmaType target = expressionCoercions.get(expr);
        if (target == null) {
            return;
        }
        SigmaType source = expressionTypes.get(expr);
        if (source == null || source.equals(target)) {
            return;
        }
        String op = conversionOp(source, target);
        if (op != null) {
            ctx.instructions.add(new PostfixInstruction(op, "conv"));
        }
    }

    private String conversionOp(SigmaType from, SigmaType to) {
        String fromName = from.getName();
        String toName = to.getName();
        if (fromName.equals(toName)) {
            return null;
        }
        if (isInt(fromName) && isFloatLike(toName)) {
            return "i2f";
        }
        if (isFloatLike(fromName) && isInt(toName)) {
            return "f2i";
        }
        if (isInt(fromName) && isString(toName)) {
            return "i2s";
        }
        if (isString(fromName) && isInt(toName)) {
            return "s2i";
        }
        if (isFloatLike(fromName) && isString(toName)) {
            return "f2s";
        }
        if (isString(fromName) && isFloatLike(toName)) {
            return "s2f";
        }
        if (isInt(fromName) && isBoolean(toName)) {
            return "i2b";
        }
        if (isBoolean(fromName) && isInt(toName)) {
            return "b2i";
        }
        return null;
    }

    private boolean isInt(String typeName) {
        return "int".equals(typeName);
    }

    private boolean isFloatLike(String typeName) {
        return "float".equals(typeName) || "double".equals(typeName);
    }

    private boolean isString(String typeName) {
        return "string".equals(typeName) || "String".equals(typeName);
    }

    private boolean isBoolean(String typeName) {
        return "bool".equals(typeName) || "boolean".equals(typeName);
    }

    private static class GenerationContext {
        private final List<PostfixInstruction> instructions = new ArrayList<>();
        private final Map<String, String> variables = new LinkedHashMap<>();
        private final Set<String> definedLabels = new HashSet<>();
        private final boolean isFunction;
        private final String returnType;
        private boolean hasReturn;
        private int labelCounter = 1;
        private String functionEndLabel;

        private GenerationContext(boolean isFunction, String returnType) {
            this.isFunction = isFunction;
            this.returnType = returnType;
        }

        static GenerationContext forMain() {
            return new GenerationContext(false, "void");
        }

        static GenerationContext forFunction(String returnType) {
            return new GenerationContext(true, returnType);
        }

        void registerVariable(String name, String type) {
            variables.putIfAbsent(name, type);
        }

        String newLabel() {
            return "m" + labelCounter++;
        }

        void defineLabel(String label) {
            definedLabels.add(label);
            instructions.add(new PostfixInstruction(label, "label", true));
            instructions.add(new PostfixInstruction(":", "colon"));
        }

        Map<String, Integer> buildLabelTable() {
            Map<String, Integer> table = new LinkedHashMap<>();
            for (int i = 0; i < instructions.size(); i++) {
                PostfixInstruction ins = instructions.get(i);
                if (ins.tokenType().equals("label") && ins.labelDefinition()) {
                    table.put(ins.lexeme(), i);
                }
            }
            return table;
        }

        PostfixProgram buildProgram(List<PostfixFunction> functions) {
            return new PostfixProgram(variables, buildLabelTable(), List.copyOf(instructions), functions);
        }

        void ensureFunctionTermination(String functionName) {
            if (!isFunction) {
                return;
            }
            if ("void".equals(returnType)) {
                if (!hasReturn) {
                    instructions.add(new PostfixInstruction("RET", "RET"));
                }
            } else if (!hasReturn) {
                throw new IllegalStateException("Function '" + functionName + "' must return a value.");
            }
        }
    }
}
