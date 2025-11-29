package org.sigma.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Parse result containing the AST and any syntax errors.
 * Used throughout the parsing pipeline.
 */
public class ParseResult {

    private final Ast.CompilationUnit ast;
    private final List<String> errors;
    private final boolean successful;

    public ParseResult(Ast.CompilationUnit ast, List<String> errors) {
        this.ast = ast;
        this.errors = new ArrayList<>(errors == null ? List.of() : errors);
        this.successful = this.errors.isEmpty();
    }

    public static ParseResult failure(List<String> errors) { return new ParseResult(null, errors); }
    public static ParseResult success(Ast.CompilationUnit ast) { return new ParseResult(ast, new ArrayList<>()); }

    public Ast.CompilationUnit getAst() { return ast; }
    public List<String> getErrors() { return new ArrayList<>(errors); }
    public boolean isSuccessful() { return successful; }
    public boolean hasErrors() { return !errors.isEmpty(); }
    public int getErrorCount() { return errors.size(); }

    public String getErrorsAsString() { if (errors.isEmpty()) return "No errors"; return String.join("\n", errors); }

    /**
     * Returns a readable string representation of the AST.
     * If AST is null, returns "No AST (parse failed)".
     */
    public String getAstAsString() {
        if (ast == null) {
            return "No AST (parse failed)";
        }
        StringBuilder sb = new StringBuilder();
        printCompilationUnit(ast, sb, 0);
        return sb.toString();
    }

    private void printCompilationUnit(Ast.CompilationUnit ast, StringBuilder sb, int indent) {
        indent(sb, indent).append("[CompilationUnit]").append('\n');
        for (Ast.Statement stmt : ast.statements) {
            printStatement(stmt, sb, indent + 1);
        }
    }

    private void printStatement(Ast.Statement stmt, StringBuilder sb, int indent) {
        if (stmt instanceof Ast.VariableDeclaration) {
            Ast.VariableDeclaration vd = (Ast.VariableDeclaration) stmt;
            indent(sb, indent).append("[VariableDeclaration]").append('\n');
            indent(sb, indent + 1).append("[Type] ").append(vd.typeName).append('\n');
            indent(sb, indent + 1).append("[Identifier] ").append(vd.name).append('\n');
            if (vd.init != null) {
                indent(sb, indent + 1).append("[Initializer]").append('\n');
                printExpression(vd.init, sb, indent + 2);
            }
            return;
        }
        if (stmt instanceof Ast.Assignment) {
            Ast.Assignment a = (Ast.Assignment) stmt;
            indent(sb, indent).append("[Assignment]").append('\n');
            indent(sb, indent + 1).append("[Identifier] ").append(a.name).append('\n');
            indent(sb, indent + 1).append("[Value]").append('\n');
            printExpression(a.value, sb, indent + 2);
            return;
        }
        if (stmt instanceof Ast.ExpressionStatement) {
            indent(sb, indent).append("[ExpressionStatement]").append('\n');
            printExpression(((Ast.ExpressionStatement) stmt).expr, sb, indent + 1);
            return;
        }
        if (stmt instanceof Ast.IfStatement) {
            Ast.IfStatement ifs = (Ast.IfStatement) stmt;
            indent(sb, indent).append("[IfStatement]").append('\n');
            indent(sb, indent + 1).append("[Condition]").append('\n');
            printExpression(ifs.cond, sb, indent + 2);
            indent(sb, indent + 1).append("[Then]").append('\n');
            printStatement(ifs.thenBranch, sb, indent + 2);
            if (ifs.elseBranch != null) {
                indent(sb, indent + 1).append("[Else]").append('\n');
                printStatement(ifs.elseBranch, sb, indent + 2);
            }
            return;
        }
        if (stmt instanceof Ast.WhileStatement) {
            Ast.WhileStatement ws = (Ast.WhileStatement) stmt;
            indent(sb, indent).append("[WhileStatement]").append('\n');
            indent(sb, indent + 1).append("[Condition]").append('\n');
            printExpression(ws.cond, sb, indent + 2);
            indent(sb, indent + 1).append("[Body]").append('\n');
            printStatement(ws.body, sb, indent + 2);
            return;
        }
        if (stmt instanceof Ast.ReturnStatement) {
            Ast.ReturnStatement rs = (Ast.ReturnStatement) stmt;
            indent(sb, indent).append("[ReturnStatement]").append('\n');
            if (rs.expr != null) {
                printExpression(rs.expr, sb, indent + 1);
            } else {
                indent(sb, indent + 1).append("[void]").append('\n');
            }
            return;
        }
        if (stmt instanceof Ast.Block) {
            indent(sb, indent).append("[Block]").append('\n');
            for (Ast.Statement s : ((Ast.Block) stmt).statements) {
                printStatement(s, sb, indent + 1);
            }
            return;
        }
        if (stmt instanceof Ast.MethodDeclaration) {
            Ast.MethodDeclaration md = (Ast.MethodDeclaration) stmt;
            indent(sb, indent).append("[MethodDeclaration]").append('\n');
            indent(sb, indent + 1).append("[Name] ").append(md.name).append('\n');
            indent(sb, indent + 1).append("[ReturnType] ").append(md.returnType).append('\n');
            indent(sb, indent + 1).append("[Parameters]").append('\n');
            for (Ast.Parameter p : md.parameters) {
                indent(sb, indent + 2).append(p.type).append(" ").append(p.name).append('\n');
            }
            indent(sb, indent + 1).append("[Body]").append('\n');
            printStatement(md.body, sb, indent + 2);
            return;
        }
        if (stmt instanceof Ast.ClassDeclaration) {
            Ast.ClassDeclaration cd = (Ast.ClassDeclaration) stmt;
            indent(sb, indent).append("[ClassDeclaration] ").append(cd.name).append('\n');
            for (Ast.Statement member : cd.members) {
                printStatement(member, sb, indent + 1);
            }
            return;
        }
        if (stmt instanceof Ast.FieldDeclaration) {
            Ast.FieldDeclaration fd = (Ast.FieldDeclaration) stmt;
            indent(sb, indent).append("[FieldDeclaration]").append('\n');
            indent(sb, indent + 1).append("[Type] ").append(fd.typeName).append('\n');
            indent(sb, indent + 1).append("[Identifier] ").append(fd.name).append('\n');
            if (fd.init != null) {
                indent(sb, indent + 1).append("[Initializer]").append('\n');
                printExpression(fd.init, sb, indent + 2);
            }
            return;
        }
        indent(sb, indent).append(stmt.getClass().getSimpleName()).append('\n');
    }

    private void printExpression(Ast.Expression expr, StringBuilder sb, int indent) {
        if (expr instanceof Ast.IntLiteral) {
            indent(sb, indent).append("[IntLiteral] ").append(((Ast.IntLiteral) expr).value).append('\n');
            return;
        }
        if (expr instanceof Ast.DoubleLiteral) {
            indent(sb, indent).append("[DoubleLiteral] ").append(((Ast.DoubleLiteral) expr).value).append('\n');
            return;
        }
        if (expr instanceof Ast.StringLiteral) {
            indent(sb, indent).append("[StringLiteral] ").append(((Ast.StringLiteral) expr).value).append('\n');
            return;
        }
        if (expr instanceof Ast.BooleanLiteral) {
            indent(sb, indent).append("[BooleanLiteral] ").append(((Ast.BooleanLiteral) expr).value).append('\n');
            return;
        }
        if (expr instanceof Ast.NullLiteral) {
            indent(sb, indent).append("[NullLiteral]").append('\n');
            return;
        }
        if (expr instanceof Ast.Identifier) {
            indent(sb, indent).append("[Identifier] ").append(((Ast.Identifier) expr).name).append('\n');
            return;
        }
        if (expr instanceof Ast.Binary) {
            Ast.Binary b = (Ast.Binary) expr;
            indent(sb, indent).append("[BinaryExpression ").append(b.op).append("]").append('\n');
            printExpression(b.left, sb, indent + 1);
            printExpression(b.right, sb, indent + 1);
            return;
        }
        if (expr instanceof Ast.Unary) {
            Ast.Unary u = (Ast.Unary) expr;
            indent(sb, indent).append("[UnaryExpression ").append(u.op).append("]").append('\n');
            printExpression(u.expr, sb, indent + 1);
            return;
        }
        if (expr instanceof Ast.Call) {
            Ast.Call call = (Ast.Call) expr;
            indent(sb, indent).append("[Call]").append('\n');
            indent(sb, indent + 1).append("[Target]").append('\n');
            printExpression(call.target, sb, indent + 2);
            indent(sb, indent + 1).append("[Args]").append('\n');
            for (Ast.Expression arg : call.args) {
                printExpression(arg, sb, indent + 2);
            }
            return;
        }
        if (expr instanceof Ast.MemberAccess) {
            Ast.MemberAccess ma = (Ast.MemberAccess) expr;
            indent(sb, indent).append("[MemberAccess]").append('\n');
            indent(sb, indent + 1).append("[Object]").append('\n');
            printExpression(ma.object, sb, indent + 2);
            indent(sb, indent + 1).append("[Member] ").append(ma.memberName).append('\n');
            return;
        }
        if (expr instanceof Ast.NewInstance) {
            Ast.NewInstance ni = (Ast.NewInstance) expr;
            indent(sb, indent).append("[NewInstance] ").append(ni.className).append('\n');
            for (Ast.Expression arg : ni.args) {
                printExpression(arg, sb, indent + 1);
            }
            return;
        }
        indent(sb, indent).append(expr.getClass().getSimpleName()).append('\n');
    }

    private StringBuilder indent(StringBuilder sb, int indent) {
        for (int i = 0; i < indent; i++) {
            sb.append("  ");
        }
        return sb;
    }

    @Override
    public String toString() {
        return "ParseResult{" +
                "ast=" + ast +
                ", errors=" + errors +
                ", successful=" + successful +
                '}';
    }
}
