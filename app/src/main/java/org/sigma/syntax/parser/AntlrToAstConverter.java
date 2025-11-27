package org.sigma.syntax.parser;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.example.parser.SigmaBaseVisitor;
import org.example.parser.SigmaParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts ANTLR's ParseTree to our custom AST.
 * Extends SigmaBaseVisitor and implements visitor methods for each grammar rule.
 */
public class AntlrToAstConverter extends SigmaBaseVisitor<Object> {

    @Override
    public Ast.CompilationUnit visitCompilationUnit(SigmaParser.CompilationUnitContext ctx) {
        List<Ast.Statement> statements = new ArrayList<>();

        // Visit all declarations and statements
        for (SigmaParser.DeclarationContext declCtx : ctx.declaration()) {
            Object result = visit(declCtx);
            if (result instanceof Ast.Statement) {
                statements.add((Ast.Statement) result);
            }
        }

        for (SigmaParser.StatementContext stmtCtx : ctx.statement()) {
            Object result = visit(stmtCtx);
            if (result instanceof Ast.Statement) {
                statements.add((Ast.Statement) result);
            }
        }

        return new Ast.CompilationUnit(statements);
    }

    @Override
    public Object visitDeclaration(SigmaParser.DeclarationContext ctx) {
        // Declaration is a choice - delegate to the actual declaration type
        if (ctx.variableDeclaration() != null) {
            return visit(ctx.variableDeclaration());
        }
        // Other declaration types not yet implemented in AST
        return null;
    }

    @Override
    public Ast.VariableDeclaration visitVariableDeclaration(SigmaParser.VariableDeclarationContext ctx) {
        // variableDeclaration: type IDENTIFIER (ASSIGN expression)? SEMI
        String typeName = ctx.type().getText();
        Token idToken = ctx.IDENTIFIER().getSymbol();
        String varName = idToken.getText();

        Ast.Expression init = null;
        if (ctx.expression() != null) {
            init = (Ast.Expression) visit(ctx.expression());
        }

        return new Ast.VariableDeclaration(typeName, varName, init,
            idToken.getLine(), idToken.getCharPositionInLine());
    }

    @Override
    public Object visitStatement(SigmaParser.StatementContext ctx) {
        // Statement is a choice - delegate to the actual statement type
        if (ctx.assignmentStatement() != null) {
            return visit(ctx.assignmentStatement());
        } else if (ctx.expressionStatement() != null) {
            return visit(ctx.expressionStatement());
        } else if (ctx.ifStatement() != null) {
            return visit(ctx.ifStatement());
        } else if (ctx.whileStatement() != null) {
            return visit(ctx.whileStatement());
        } else if (ctx.returnStatement() != null) {
            return visit(ctx.returnStatement());
        } else if (ctx.block() != null) {
            return visit(ctx.block());
        }
        return null;
    }

    @Override
    public Ast.Assignment visitAssignmentStatement(SigmaParser.AssignmentStatementContext ctx) {
        // assignmentStatement: IDENTIFIER ASSIGN expression SEMI
        String varName = ctx.IDENTIFIER().getText();
        Ast.Expression value = (Ast.Expression) visit(ctx.expression());
        return new Ast.Assignment(varName, value);
    }

    @Override
    public Ast.ExpressionStatement visitExpressionStatement(SigmaParser.ExpressionStatementContext ctx) {
        // expressionStatement: expression SEMI?
        Ast.Expression expr = (Ast.Expression) visit(ctx.expression());
        Token firstToken = ctx.expression().start;
        return new Ast.ExpressionStatement(expr, firstToken.getLine(), firstToken.getCharPositionInLine());
    }

    @Override
    public Ast.IfStatement visitIfStatement(SigmaParser.IfStatementContext ctx) {
        // ifStatement: IF LPAREN expression RPAREN statement (ELSE statement)?
        Ast.Expression cond = (Ast.Expression) visit(ctx.expression());
        Ast.Statement thenStmt = (Ast.Statement) visit(ctx.statement(0));

        Ast.Statement elseStmt = null;
        if (ctx.statement().size() > 1) {
            elseStmt = (Ast.Statement) visit(ctx.statement(1));
        }

        return new Ast.IfStatement(cond, thenStmt, elseStmt);
    }

    @Override
    public Ast.WhileStatement visitWhileStatement(SigmaParser.WhileStatementContext ctx) {
        // whileStatement: WHILE LPAREN expression RPAREN statement
        Ast.Expression cond = (Ast.Expression) visit(ctx.expression());
        Ast.Statement body = (Ast.Statement) visit(ctx.statement());
        return new Ast.WhileStatement(cond, body);
    }

    @Override
    public Ast.ReturnStatement visitReturnStatement(SigmaParser.ReturnStatementContext ctx) {
        // returnStatement: RETURN expression? SEMI
        Ast.Expression expr = null;
        if (ctx.expression() != null) {
            expr = (Ast.Expression) visit(ctx.expression());
        }

        Token returnToken = ctx.RETURN().getSymbol();
        return new Ast.ReturnStatement(expr, returnToken.getLine(), returnToken.getCharPositionInLine());
    }

    @Override
    public Ast.Block visitBlock(SigmaParser.BlockContext ctx) {
        // block: LBRACE (declaration | statement)* RBRACE
        List<Ast.Statement> statements = new ArrayList<>();

        for (SigmaParser.DeclarationContext declCtx : ctx.declaration()) {
            Object result = visit(declCtx);
            if (result instanceof Ast.Statement) {
                statements.add((Ast.Statement) result);
            }
        }

        for (SigmaParser.StatementContext stmtCtx : ctx.statement()) {
            Object result = visit(stmtCtx);
            if (result instanceof Ast.Statement) {
                statements.add((Ast.Statement) result);
            }
        }

        return new Ast.Block(statements);
    }

    // Expression visitors

    @Override
    public Object visitExpression(SigmaParser.ExpressionContext ctx) {
        // expression: logicalOrExpression
        return visit(ctx.logicalOrExpression());
    }

    @Override
    public Object visitLogicalOrExpression(SigmaParser.LogicalOrExpressionContext ctx) {
        // logicalOrExpression: logicalAndExpression (LOGICAL logicalAndExpression)*
        Ast.Expression left = (Ast.Expression) visit(ctx.logicalAndExpression(0));

        for (int i = 1; i < ctx.logicalAndExpression().size(); i++) {
            Token op = ctx.LOGICAL(i - 1).getSymbol();
            Ast.Expression right = (Ast.Expression) visit(ctx.logicalAndExpression(i));
            left = new Ast.Binary(op.getText(), left, right, op.getLine(), op.getCharPositionInLine());
        }

        return left;
    }

    @Override
    public Object visitLogicalAndExpression(SigmaParser.LogicalAndExpressionContext ctx) {
        // logicalAndExpression: relationalExpression (LOGICAL relationalExpression)*
        Ast.Expression left = (Ast.Expression) visit(ctx.relationalExpression(0));

        for (int i = 1; i < ctx.relationalExpression().size(); i++) {
            Token op = ctx.LOGICAL(i - 1).getSymbol();
            Ast.Expression right = (Ast.Expression) visit(ctx.relationalExpression(i));
            left = new Ast.Binary(op.getText(), left, right, op.getLine(), op.getCharPositionInLine());
        }

        return left;
    }

    @Override
    public Object visitRelationalExpression(SigmaParser.RelationalExpressionContext ctx) {
        // relationalExpression: additiveExpression (RELATIONAL additiveExpression)*
        Ast.Expression left = (Ast.Expression) visit(ctx.additiveExpression(0));

        for (int i = 1; i < ctx.additiveExpression().size(); i++) {
            Token op = ctx.RELATIONAL(i - 1).getSymbol();
            Ast.Expression right = (Ast.Expression) visit(ctx.additiveExpression(i));
            left = new Ast.Binary(op.getText(), left, right, op.getLine(), op.getCharPositionInLine());
        }

        return left;
    }

    @Override
    public Object visitAdditiveExpression(SigmaParser.AdditiveExpressionContext ctx) {
        // additiveExpression: multiplicativeExpression ((PLUS | MINUS) multiplicativeExpression)*
        Ast.Expression left = (Ast.Expression) visit(ctx.multiplicativeExpression(0));

        // Get all operators in order by checking children
        int opIndex = 0;
        for (int i = 1; i < ctx.multiplicativeExpression().size(); i++) {
            // Find the next operator token in the parse tree
            Token op = null;
            while (opIndex < ctx.getChildCount() && op == null) {
                ParseTree child = ctx.getChild(opIndex);
                if (child instanceof org.antlr.v4.runtime.tree.TerminalNode) {
                    org.antlr.v4.runtime.tree.TerminalNode terminal = (org.antlr.v4.runtime.tree.TerminalNode) child;
                    int tokenType = terminal.getSymbol().getType();
                    if (tokenType == SigmaParser.PLUS || tokenType == SigmaParser.MINUS) {
                        op = terminal.getSymbol();
                    }
                }
                opIndex++;
            }

            Ast.Expression right = (Ast.Expression) visit(ctx.multiplicativeExpression(i));
            left = new Ast.Binary(op.getText(), left, right, op.getLine(), op.getCharPositionInLine());
        }

        return left;
    }

    @Override
    public Object visitMultiplicativeExpression(SigmaParser.MultiplicativeExpressionContext ctx) {
        // multiplicativeExpression: powerExpression (MULTIPLICATIVE powerExpression)*
        Ast.Expression left = (Ast.Expression) visit(ctx.powerExpression(0));

        for (int i = 1; i < ctx.powerExpression().size(); i++) {
            Token op = ctx.MULTIPLICATIVE(i - 1).getSymbol();
            Ast.Expression right = (Ast.Expression) visit(ctx.powerExpression(i));
            left = new Ast.Binary(op.getText(), left, right, op.getLine(), op.getCharPositionInLine());
        }

        return left;
    }

    @Override
    public Object visitPowerExpression(SigmaParser.PowerExpressionContext ctx) {
        // powerExpression: unaryExpression (POWER powerExpression)?
        Ast.Expression left = (Ast.Expression) visit(ctx.unaryExpression());

        if (ctx.powerExpression() != null) {
            Token op = ctx.POWER().getSymbol();
            Ast.Expression right = (Ast.Expression) visit(ctx.powerExpression());
            return new Ast.Binary(op.getText(), left, right, op.getLine(), op.getCharPositionInLine());
        }

        return left;
    }

    @Override
    public Object visitUnaryExpression(SigmaParser.UnaryExpressionContext ctx) {
        // unaryExpression: NOT unaryExpression | MINUS unaryExpression | postfixExpression
        if (ctx.NOT() != null) {
            Token op = ctx.NOT().getSymbol();
            Ast.Expression expr = (Ast.Expression) visit(ctx.unaryExpression());
            return new Ast.Unary("!", expr, op.getLine(), op.getCharPositionInLine());
        } else if (ctx.MINUS() != null) {
            Token op = ctx.MINUS().getSymbol();
            Ast.Expression expr = (Ast.Expression) visit(ctx.unaryExpression());
            return new Ast.Unary("neg", expr, op.getLine(), op.getCharPositionInLine());
        } else {
            return visit(ctx.postfixExpression());
        }
    }

    @Override
    public Object visitPostfixExpression(SigmaParser.PostfixExpressionContext ctx) {
        // postfixExpression: primaryExpression (postfixOp)*
        Ast.Expression expr = (Ast.Expression) visit(ctx.primaryExpression());

        // Handle postfix operations (method calls, member access)
        for (SigmaParser.PostfixOpContext opCtx : ctx.postfixOp()) {
            if (opCtx.argumentList() != null) {
                // Method call: expr(args)
                List<Ast.Expression> args = new ArrayList<>();
                for (SigmaParser.ExpressionContext argCtx : opCtx.argumentList().expression()) {
                    args.add((Ast.Expression) visit(argCtx));
                }
                Token lparen = opCtx.LPAREN().getSymbol();
                expr = new Ast.Call(expr, args, lparen.getLine(), lparen.getCharPositionInLine());
            }
            // Member access not fully implemented yet in AST
        }

        return expr;
    }

    @Override
    public Object visitPrimaryExpression(SigmaParser.PrimaryExpressionContext ctx) {
        // primaryExpression: IDENTIFIER | literal | LPAREN expression RPAREN
        if (ctx.IDENTIFIER() != null) {
            Token idToken = ctx.IDENTIFIER().getSymbol();
            return new Ast.Identifier(idToken.getText(), idToken.getLine(), idToken.getCharPositionInLine());
        } else if (ctx.literal() != null) {
            return visit(ctx.literal());
        } else if (ctx.expression() != null) {
            // Parenthesized expression
            return visit(ctx.expression());
        }
        return null;
    }

    @Override
    public Object visitLiteral(SigmaParser.LiteralContext ctx) {
        // literal: INTEGER | FLOAT | STRING | BOOLEAN | NULL
        if (ctx.INTEGER() != null) {
            Token token = ctx.INTEGER().getSymbol();
            int value = Integer.parseInt(token.getText());
            return new Ast.IntLiteral(value, token.getLine(), token.getCharPositionInLine());
        } else if (ctx.FLOAT() != null) {
            Token token = ctx.FLOAT().getSymbol();
            double value = Double.parseDouble(token.getText());
            return new Ast.DoubleLiteral(value, token.getLine(), token.getCharPositionInLine());
        } else if (ctx.STRING() != null) {
            Token token = ctx.STRING().getSymbol();
            return new Ast.StringLiteral(token.getText(), token.getLine(), token.getCharPositionInLine());
        } else if (ctx.BOOLEAN() != null) {
            Token token = ctx.BOOLEAN().getSymbol();
            int value = token.getText().equals("true") ? 1 : 0;
            return new Ast.IntLiteral(value, token.getLine(), token.getCharPositionInLine());
        } else if (ctx.NULL() != null) {
            Token token = ctx.NULL().getSymbol();
            return new Ast.Identifier("null", token.getLine(), token.getCharPositionInLine());
        }
        return null;
    }
}
