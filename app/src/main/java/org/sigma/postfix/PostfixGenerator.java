package org.sigma.postfix;

import org.sigma.parser.Ast;
import org.sigma.semantics.SemanticResult;

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

    public PostfixProgram generate(SemanticResult semanticResult) {
        Ast.MethodDeclaration scriptEntry = findScriptEntry(semanticResult.getAst());
        if (scriptEntry == null) {
            throw new IllegalStateException("Script.run() method not found. Did you run ScriptWrappingTransformer?");
        }

        GenerationContext ctx = new GenerationContext();
        ctx.functionEndLabel = ctx.newLabel();
        emitBlock(scriptEntry.body, ctx);
        ctx.defineLabel(ctx.functionEndLabel);

        Map<String, Integer> labelTable = ctx.buildLabelTable();
        return new PostfixProgram(ctx.variables, labelTable, ctx.instructions);
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
            emitExpression(((Ast.ExpressionStatement) stmt).expr, ctx);
            ctx.instructions.add(new PostfixInstruction("POP", "stack_op"));
        } else if (stmt instanceof Ast.PrintStatement) {
            emitExpression(((Ast.PrintStatement) stmt).expr, ctx);
            ctx.instructions.add(new PostfixInstruction("PRINT", "out_op"));
        } else if (stmt instanceof Ast.IfStatement) {
            emitIfStatement((Ast.IfStatement) stmt, ctx);
        } else if (stmt instanceof Ast.WhileStatement) {
            emitWhileStatement((Ast.WhileStatement) stmt, ctx);
        } else if (stmt instanceof Ast.Block) {
            emitBlock((Ast.Block) stmt, ctx);
        } else if (stmt instanceof Ast.ReturnStatement) {
            Ast.ReturnStatement ret = (Ast.ReturnStatement) stmt;
            if (ret.expr != null) {
                emitExpression(ret.expr, ctx);
                ctx.instructions.add(new PostfixInstruction("POP", "stack_op"));
            }
            if (ctx.functionEndLabel != null) {
                ctx.instructions.add(new PostfixInstruction(ctx.functionEndLabel, "label"));
                ctx.instructions.add(new PostfixInstruction("JMP", "jump"));
            }
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

    private void emitAssignment(Ast.Assignment assignment, GenerationContext ctx) {
        ctx.instructions.add(new PostfixInstruction(assignment.name, "l-val"));
        emitExpression(assignment.value, ctx);
        ctx.instructions.add(new PostfixInstruction("=", "assign_op"));
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
        } else {
            throw new UnsupportedOperationException("Unsupported expression for postfix backend: " + expr.getClass().getSimpleName());
        }
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
            default:
                return typeName;
        }
    }

    private static class GenerationContext {
        private final List<PostfixInstruction> instructions = new ArrayList<>();
        private final Map<String, String> variables = new LinkedHashMap<>();
        private final Set<String> definedLabels = new HashSet<>();
        private int labelCounter = 1;
        private String functionEndLabel;

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
    }
}
