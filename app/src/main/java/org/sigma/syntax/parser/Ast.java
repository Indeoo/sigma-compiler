package org.sigma.syntax.parser;

import java.util.List;

// Minimal AST types used by the RD frontend and the RD code generator
public class Ast {

    public static class CompilationUnit {
        public final List<Statement> statements;
        public CompilationUnit(List<Statement> statements) { this.statements = statements; }
    }

    // Statements
    public interface Statement {}

    public static class ExpressionStatement implements Statement {
        public final Expression expr;
        public final int line, col;
        public ExpressionStatement(Expression e, int line, int col) { this.expr = e; this.line = line; this.col = col; }
    }

    public static class VariableDeclaration implements Statement {
        public final String typeName; // e.g. "int", "double", "String"
        public final String name;
        public final Expression init; // may be null
        public final int line, col;
        public VariableDeclaration(String typeName, String name, Expression init, int line, int col) { this.typeName = typeName; this.name = name; this.init = init; this.line = line; this.col = col; }
    }

    public static class Assignment implements Statement {
        public final String name;
        public final Expression value;
        public Assignment(String name, Expression value) { this.name = name; this.value = value; }
    }

    public static class Block implements Statement {
        public final List<Statement> statements;
        public Block(List<Statement> statements) { this.statements = statements; }
    }

    public static class IfStatement implements Statement {
        public final Expression cond; public final Statement thenBranch; public final Statement elseBranch;
        public IfStatement(Expression cond, Statement thenBranch, Statement elseBranch) { this.cond = cond; this.thenBranch = thenBranch; this.elseBranch = elseBranch; }
    }

    public static class WhileStatement implements Statement {
        public final Expression cond; public final Statement body;
        public WhileStatement(Expression cond, Statement body) { this.cond = cond; this.body = body; }
    }

    public static class ReturnStatement implements Statement {
        public final Expression expr; public final int line, col; public ReturnStatement(Expression e, int line, int col) { this.expr = e; this.line=line; this.col=col; }
    }

    // Expressions
    public interface Expression {}

    public static class IntLiteral implements Expression { public final int value; public final int line, col; public IntLiteral(int v, int line, int col){value=v; this.line=line; this.col=col;} }
    public static class DoubleLiteral implements Expression { public final double value; public final int line, col; public DoubleLiteral(double v, int line, int col){value=v; this.line=line; this.col=col;} }
    public static class StringLiteral implements Expression { public final String value; public final int line, col; public StringLiteral(String v, int line, int col){value=v; this.line=line; this.col=col;} }
    public static class BooleanLiteral implements Expression { public final boolean value; public BooleanLiteral(boolean v){value=v;} }
    public static class NullLiteral implements Expression { public NullLiteral(){} }
    public static class Identifier implements Expression { public final String name; public final int line, col; public Identifier(String n, int line, int col){name=n; this.line=line; this.col=col;} }

    public static class Binary implements Expression { public final String op; public final Expression left, right; public final int line, col; public Binary(String op, Expression l, Expression r, int line, int col){this.op=op;this.left=l;this.right=r; this.line=line; this.col=col;} }
    public static class Unary implements Expression { public final String op; public final Expression expr; public final int line, col; public Unary(String op, Expression e, int line, int col){this.op=op;this.expr=e; this.line=line; this.col=col;} }
    public static class Call implements Expression { public final Expression target; public final List<Expression> args; public final int line, col; public Call(Expression target, List<Expression> args, int line, int col){this.target=target;this.args=args; this.line=line; this.col=col;} }

}
