package org.sigma.syntax.parser;

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
        sb.append("CompilationUnit {\n");
        for (int i = 0; i < ast.statements.size(); i++) {
            sb.append("  ").append(statementToString(ast.statements.get(i), 1));
            if (i < ast.statements.size() - 1) {
                sb.append("\n");
            }
        }
        sb.append("\n}");
        return sb.toString();
    }

    private String statementToString(Ast.Statement stmt, int indent) {
        String indentStr = "  ".repeat(indent);

        if (stmt instanceof Ast.VariableDeclaration) {
            Ast.VariableDeclaration vd = (Ast.VariableDeclaration) stmt;
            String init = vd.init != null ? " = " + exprToString(vd.init) : "";
            return String.format("VariableDeclaration(%s %s%s)", vd.typeName, vd.name, init);
        }

        if (stmt instanceof Ast.Assignment) {
            Ast.Assignment a = (Ast.Assignment) stmt;
            return String.format("Assignment(%s = %s)", a.name, exprToString(a.value));
        }

        if (stmt instanceof Ast.ExpressionStatement) {
            Ast.ExpressionStatement es = (Ast.ExpressionStatement) stmt;
            return "ExpressionStatement(" + exprToString(es.expr) + ")";
        }

        if (stmt instanceof Ast.IfStatement) {
            Ast.IfStatement ifs = (Ast.IfStatement) stmt;
            StringBuilder sb = new StringBuilder();
            sb.append("IfStatement {\n");
            sb.append(indentStr).append("  condition: ").append(exprToString(ifs.cond)).append("\n");
            sb.append(indentStr).append("  then: ").append(statementToString(ifs.thenBranch, indent + 1));
            if (ifs.elseBranch != null) {
                sb.append("\n").append(indentStr).append("  else: ").append(statementToString(ifs.elseBranch, indent + 1));
            }
            sb.append("\n").append(indentStr).append("}");
            return sb.toString();
        }

        if (stmt instanceof Ast.WhileStatement) {
            Ast.WhileStatement ws = (Ast.WhileStatement) stmt;
            StringBuilder sb = new StringBuilder();
            sb.append("WhileStatement {\n");
            sb.append(indentStr).append("  condition: ").append(exprToString(ws.cond)).append("\n");
            sb.append(indentStr).append("  body: ").append(statementToString(ws.body, indent + 1));
            sb.append("\n").append(indentStr).append("}");
            return sb.toString();
        }

        if (stmt instanceof Ast.ReturnStatement) {
            Ast.ReturnStatement rs = (Ast.ReturnStatement) stmt;
            String expr = rs.expr != null ? exprToString(rs.expr) : "void";
            return "ReturnStatement(" + expr + ")";
        }

        if (stmt instanceof Ast.Block) {
            Ast.Block b = (Ast.Block) stmt;
            StringBuilder sb = new StringBuilder();
            sb.append("Block {\n");
            for (Ast.Statement s : b.statements) {
                sb.append(indentStr).append("  ").append(statementToString(s, indent + 1)).append("\n");
            }
            sb.append(indentStr).append("}");
            return sb.toString();
        }

        if (stmt instanceof Ast.MethodDeclaration) {
            Ast.MethodDeclaration md = (Ast.MethodDeclaration) stmt;
            StringBuilder sb = new StringBuilder();

            // Method signature
            sb.append("MethodDeclaration(")
              .append(md.returnType).append(" ")
              .append(md.name).append("(");

            // Parameters
            for (int i = 0; i < md.parameters.size(); i++) {
                Ast.Parameter p = md.parameters.get(i);
                sb.append(p.type).append(" ").append(p.name);
                if (i < md.parameters.size() - 1) sb.append(", ");
            }
            sb.append(")) {\n");

            // Body
            sb.append(indentStr).append("  ")
              .append(statementToString(md.body, indent + 1));
            sb.append("\n").append(indentStr).append("}");

            return sb.toString();
        }

        if (stmt instanceof Ast.ClassDeclaration) {
            Ast.ClassDeclaration cd = (Ast.ClassDeclaration) stmt;
            StringBuilder sb = new StringBuilder();

            sb.append("ClassDeclaration(").append(cd.name).append(") {\n");

            // Members (fields and methods)
            for (Ast.Statement member : cd.members) {
                sb.append(indentStr).append("  ")
                  .append(statementToString(member, indent + 1))
                  .append("\n");
            }

            sb.append(indentStr).append("}");
            return sb.toString();
        }

        if (stmt instanceof Ast.FieldDeclaration) {
            Ast.FieldDeclaration fd = (Ast.FieldDeclaration) stmt;
            String init = fd.init != null ? " = " + exprToString(fd.init) : "";
            return String.format("FieldDeclaration(%s %s%s)",
                                 fd.typeName, fd.name, init);
        }

        return stmt.getClass().getSimpleName();
    }

    private String exprToString(Ast.Expression expr) {
        if (expr instanceof Ast.IntLiteral) {
            return String.valueOf(((Ast.IntLiteral) expr).value);
        }
        if (expr instanceof Ast.DoubleLiteral) {
            return String.valueOf(((Ast.DoubleLiteral) expr).value);
        }
        if (expr instanceof Ast.StringLiteral) {
            return ((Ast.StringLiteral) expr).value;
        }
        if (expr instanceof Ast.BooleanLiteral) {
            return String.valueOf(((Ast.BooleanLiteral) expr).value);
        }
        if (expr instanceof Ast.NullLiteral) {
            return "null";
        }
        if (expr instanceof Ast.Identifier) {
            return ((Ast.Identifier) expr).name;
        }
        if (expr instanceof Ast.Binary) {
            Ast.Binary b = (Ast.Binary) expr;
            return String.format("(%s %s %s)", exprToString(b.left), b.op, exprToString(b.right));
        }
        if (expr instanceof Ast.Unary) {
            Ast.Unary u = (Ast.Unary) expr;
            String op = u.op.equals("neg") ? "-" : u.op;
            return String.format("(%s%s)", op, exprToString(u.expr));
        }
        if (expr instanceof Ast.Call) {
            Ast.Call c = (Ast.Call) expr;
            StringBuilder sb = new StringBuilder();
            sb.append(exprToString(c.target)).append("(");
            for (int i = 0; i < c.args.size(); i++) {
                sb.append(exprToString(c.args.get(i)));
                if (i < c.args.size() - 1) sb.append(", ");
            }
            sb.append(")");
            return sb.toString();
        }
        if (expr instanceof Ast.MemberAccess) {
            Ast.MemberAccess ma = (Ast.MemberAccess) expr;
            return String.format("%s.%s", exprToString(ma.object), ma.memberName);
        }
        return expr.getClass().getSimpleName();
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