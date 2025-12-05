package org.sigma.parser;

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

    public static class PrintStatement implements Statement {
        public final Expression expr;
        public final int line, col;
        public PrintStatement(Expression expr, int line, int col) {
            this.expr = expr;
            this.line = line;
            this.col = col;
        }
    }

    public static class VariableDeclaration implements Statement {
        public final String typeName; // e.g. "int", "double", "String"
        public final String name;
        public final Expression init; // may be null
        public final int line, col;
        public final boolean isConstant;

        public VariableDeclaration(String typeName, String name, Expression init, int line, int col) {
            this(typeName, name, init, line, col, false);
        }

        public VariableDeclaration(String typeName, String name, Expression init,
                                   int line, int col, boolean isConstant) {
            this.typeName = typeName;
            this.name = name;
            this.init = init;
            this.line = line;
            this.col = col;
            this.isConstant = isConstant;
        }
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

    public static class ForEachStatement implements Statement {
        public final String typeName; // null if implicit
        public final String iteratorName;
        public final Expression iterable;
        public final Statement body;
        public final int line, col;
        public ForEachStatement(String typeName, String iteratorName, Expression iterable,
                                Statement body, int line, int col) {
            this.typeName = typeName;
            this.iteratorName = iteratorName;
            this.iterable = iterable;
            this.body = body;
            this.line = line;
            this.col = col;
        }
        public boolean hasExplicitType() { return typeName != null; }
    }

    public static class ReturnStatement implements Statement {
        public final Expression expr; public final int line, col; public ReturnStatement(Expression e, int line, int col) { this.expr = e; this.line=line; this.col=col; }
    }

    // Parameter for method declarations
    public static class Parameter {
        public final String type;
        public final String name;
        public final int line, col;
        public Parameter(String type, String name, int line, int col) {
            this.type = type;
            this.name = name;
            this.line = line;
            this.col = col;
        }
    }

    // Method declaration
    public static class MethodDeclaration implements Statement {
        public final String returnType;
        public final String name;
        public final List<Parameter> parameters;
        public final Block body;
        public final int line, col;
        public MethodDeclaration(String returnType, String name, List<Parameter> parameters, Block body, int line, int col) {
            this.returnType = returnType;
            this.name = name;
            this.parameters = parameters;
            this.body = body;
            this.line = line;
            this.col = col;
        }
    }

    // Field declaration (for class members)
    public static class FieldDeclaration implements Statement {
        public final String typeName;
        public final String name;
        public final Expression init; // may be null
        public final int line, col;
        public FieldDeclaration(String typeName, String name, Expression init, int line, int col) {
            this.typeName = typeName;
            this.name = name;
            this.init = init;
            this.line = line;
            this.col = col;
        }
    }

    // Class declaration
    public static class ClassDeclaration implements Statement {
        public final String name;
        public final List<Statement> members; // fields and methods
        public final int line, col;
        public ClassDeclaration(String name, List<Statement> members, int line, int col) {
            this.name = name;
            this.members = members;
            this.line = line;
            this.col = col;
        }
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

    // Member access (dot notation)
    public static class MemberAccess implements Expression {
        public final Expression object;
        public final String memberName;
        public final int line, col;
        public MemberAccess(Expression object, String memberName, int line, int col) {
            this.object = object;
            this.memberName = memberName;
            this.line = line;
            this.col = col;
        }
    }

    // Object instantiation with 'new' keyword
    public static class NewInstance implements Expression {
        public final String className;
        public final List<Expression> args;
        public final int line, col;
        public NewInstance(String className, List<Expression> args, int line, int col) {
            this.className = className;
            this.args = args;
            this.line = line;
            this.col = col;
        }
    }

}
