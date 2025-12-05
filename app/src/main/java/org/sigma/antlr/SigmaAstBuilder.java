package org.sigma.antlr;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.example.parser.SigmaParser;
import org.sigma.parser.Ast;
import org.sigma.transform.ScriptWrappingTransformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Converts the ANTLR parse tree into the shared Sigma AST used by the RD frontend
 * and downstream compiler stages.
 */
public final class SigmaAstBuilder {

    public Ast.CompilationUnit build(SigmaParser.CompilationUnitContext ctx) {
        Objects.requireNonNull(ctx, "compilationUnit context");
        List<Ast.Statement> statements = new ArrayList<>();
        if (ctx.children != null) {
            for (ParseTree child : ctx.children) {
                if (child instanceof SigmaParser.DeclarationContext) {
                    Ast.Statement stmt = buildDeclaration((SigmaParser.DeclarationContext) child);
                    if (stmt != null) {
                        statements.add(stmt);
                    }
                } else if (child instanceof SigmaParser.StatementContext) {
                    Ast.Statement stmt = buildStatement((SigmaParser.StatementContext) child);
                    if (stmt != null) {
                        statements.add(stmt);
                    }
                }
            }
        }
        Ast.CompilationUnit unit = new Ast.CompilationUnit(statements);
        return ScriptWrappingTransformer.wrap(unit);
    }

    private Ast.Statement buildDeclaration(SigmaParser.DeclarationContext ctx) {
        if (ctx.variableDeclaration() != null) {
            return buildVariableDeclaration(ctx.variableDeclaration(), false);
        }
        if (ctx.constantDeclaration() != null) {
            return buildConstantDeclaration(ctx.constantDeclaration());
        }
        if (ctx.methodDeclaration() != null) {
            return buildMethodDeclaration(ctx.methodDeclaration());
        }
        if (ctx.classDeclaration() != null) {
            return buildClassDeclaration(ctx.classDeclaration());
        }
        return null;
    }

    private Ast.VariableDeclaration buildConstantDeclaration(SigmaParser.ConstantDeclarationContext ctx) {
        String type = ctx.type().getText();
        String name = ctx.IDENTIFIER().getText();
        Ast.Expression init = buildExpression(ctx.expression());
        return new Ast.VariableDeclaration(type, name, init, line(ctx), col(ctx), true);
    }

    private Ast.VariableDeclaration buildVariableDeclaration(SigmaParser.VariableDeclarationContext ctx, boolean constant) {
        if (ctx == null) {
            return null;
        }
        String type = ctx.type().getText();
        String name = ctx.IDENTIFIER().getText();
        Ast.Expression init = ctx.expression() != null ? buildExpression(ctx.expression()) : null;
        return new Ast.VariableDeclaration(type, name, init, line(ctx), col(ctx), constant);
    }

    private Ast.MethodDeclaration buildMethodDeclaration(SigmaParser.MethodDeclarationContext ctx) {
        String returnType = ctx.type().getText();
        String name = ctx.IDENTIFIER().getText();
        List<Ast.Parameter> parameters = new ArrayList<>();
        SigmaParser.ParameterListContext paramsCtx = ctx.parameterList();
        if (paramsCtx != null) {
            for (SigmaParser.ParameterContext parameterContext : paramsCtx.parameter()) {
                String type = parameterContext.type().getText();
                String paramName = parameterContext.IDENTIFIER().getText();
                parameters.add(new Ast.Parameter(type, paramName, line(parameterContext), col(parameterContext)));
            }
        }
        Ast.Block body = buildBlock(ctx.block());
        return new Ast.MethodDeclaration(returnType, name, parameters, body, line(ctx), col(ctx));
    }

    private Ast.ClassDeclaration buildClassDeclaration(SigmaParser.ClassDeclarationContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        List<Ast.Statement> members = new ArrayList<>();
        SigmaParser.ClassBodyContext body = ctx.classBody();
        if (body != null && body.children != null) {
            for (ParseTree child : body.children) {
                if (child instanceof SigmaParser.DeclarationContext) {
                    Ast.Statement member = buildDeclaration((SigmaParser.DeclarationContext) child);
                    if (member != null) {
                        members.add(member);
                    }
                } else if (child instanceof SigmaParser.StatementContext) {
                    Ast.Statement member = buildStatement((SigmaParser.StatementContext) child);
                    if (member != null) {
                        members.add(member);
                    }
                }
            }
        }
        return new Ast.ClassDeclaration(name, members, line(ctx), col(ctx));
    }

    private Ast.Block buildBlock(SigmaParser.BlockContext ctx) {
        if (ctx == null) {
            return new Ast.Block(Collections.emptyList());
        }
        List<Ast.Statement> statements = new ArrayList<>();
        if (ctx.children != null) {
            for (ParseTree child : ctx.children) {
                if (child instanceof SigmaParser.DeclarationContext) {
                    Ast.Statement stmt = buildDeclaration((SigmaParser.DeclarationContext) child);
                    if (stmt != null) {
                        statements.add(stmt);
                    }
                } else if (child instanceof SigmaParser.StatementContext) {
                    Ast.Statement stmt = buildStatement((SigmaParser.StatementContext) child);
                    if (stmt != null) {
                        statements.add(stmt);
                    }
                }
            }
        }
        return new Ast.Block(statements);
    }

    private Ast.Statement buildStatement(SigmaParser.StatementContext ctx) {
        if (ctx.assignmentStatement() != null) {
            return buildAssignment(ctx.assignmentStatement());
        }
        if (ctx.expressionStatement() != null) {
            return buildExpressionStatement(ctx.expressionStatement());
        }
        if (ctx.ifStatement() != null) {
            return buildIf(ctx.ifStatement());
        }
        if (ctx.forStatement() != null) {
            return buildForEach(ctx.forStatement());
        }
        if (ctx.whileStatement() != null) {
            return buildWhile(ctx.whileStatement());
        }
        if (ctx.returnStatement() != null) {
            return buildReturn(ctx.returnStatement());
        }
        if (ctx.block() != null) {
            return buildBlock(ctx.block());
        }
        return null;
    }

    private Ast.Statement buildAssignment(SigmaParser.AssignmentStatementContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        Ast.Expression value = buildExpression(ctx.expression());
        return new Ast.Assignment(name, value);
    }

    private Ast.Statement buildExpressionStatement(SigmaParser.ExpressionStatementContext ctx) {
        if (ctx.expression() == null) {
            return null;
        }
        Ast.Expression expr = buildExpression(ctx.expression());
        if (expr instanceof Ast.Call) {
            Ast.Call call = (Ast.Call) expr;
            if (call.target instanceof Ast.Identifier) {
                Ast.Identifier id = (Ast.Identifier) call.target;
                if ("print".equals(id.name) && call.args.size() == 1) {
                    return new Ast.PrintStatement(call.args.get(0), line(ctx), col(ctx));
                }
            }
        }
        return new Ast.ExpressionStatement(expr, line(ctx), col(ctx));
    }

    private Ast.Statement buildIf(SigmaParser.IfStatementContext ctx) {
        Ast.Expression condition = buildExpression(ctx.ifCondition().expression());
        Ast.Statement thenBranch = buildStatement(ctx.statement(0));
        Ast.Statement elseBranch = ctx.statement().size() > 1 ? buildStatement(ctx.statement(1)) : null;
        return new Ast.IfStatement(condition, thenBranch, elseBranch);
    }

    private Ast.Statement buildForEach(SigmaParser.ForStatementContext ctx) {
        SigmaParser.GroovyForClauseContext clause = ctx.forClause().groovyForClause();
        SigmaParser.ForLoopVariableContext varCtx = clause.forLoopVariable();
        String iteratorName;
        String typeName = null;
        if (varCtx.type() != null) {
            typeName = varCtx.type().getText();
            iteratorName = varCtx.IDENTIFIER().getText();
        } else {
            iteratorName = varCtx.IDENTIFIER().getText();
        }
        Ast.Expression iterable = buildExpression(clause.expression());
        Ast.Statement body = buildStatement(ctx.statement());
        return new Ast.ForEachStatement(typeName, iteratorName, iterable, body, line(ctx), col(ctx));
    }

    private Ast.Statement buildWhile(SigmaParser.WhileStatementContext ctx) {
        Ast.Expression condition = buildExpression(ctx.expression());
        Ast.Statement body = buildStatement(ctx.statement());
        return new Ast.WhileStatement(condition, body);
    }

    private Ast.Statement buildReturn(SigmaParser.ReturnStatementContext ctx) {
        Ast.Expression expr = ctx.expression() != null ? buildExpression(ctx.expression()) : null;
        return new Ast.ReturnStatement(expr, line(ctx), col(ctx));
    }

    private Ast.Expression buildExpression(SigmaParser.ExpressionContext ctx) {
        return buildLogicalOr(ctx.logicalOrExpression());
    }

    private Ast.Expression buildLogicalOr(SigmaParser.LogicalOrExpressionContext ctx) {
        List<SigmaParser.LogicalAndExpressionContext> parts = ctx.logicalAndExpression();
        Ast.Expression result = buildLogicalAnd(parts.get(0));
        for (int i = 1; i < parts.size(); i++) {
            Token token = ctx.LOGICAL(i - 1).getSymbol();
            Ast.Expression right = buildLogicalAnd(parts.get(i));
            result = new Ast.Binary(token.getText(), result, right, line(token), col(token));
        }
        return result;
    }

    private Ast.Expression buildLogicalAnd(SigmaParser.LogicalAndExpressionContext ctx) {
        List<SigmaParser.RelationalExpressionContext> parts = ctx.relationalExpression();
        Ast.Expression result = buildRelational(parts.get(0));
        for (int i = 1; i < parts.size(); i++) {
            Token token = ctx.LOGICAL(i - 1).getSymbol();
            Ast.Expression right = buildRelational(parts.get(i));
            result = new Ast.Binary(token.getText(), result, right, line(token), col(token));
        }
        return result;
    }

    private Ast.Expression buildRelational(SigmaParser.RelationalExpressionContext ctx) {
        List<SigmaParser.AdditiveExpressionContext> parts = ctx.additiveExpression();
        Ast.Expression result = buildAdditive(parts.get(0));
        for (int i = 1; i < parts.size(); i++) {
            Token token = ctx.RELATIONAL(i - 1).getSymbol();
            Ast.Expression right = buildAdditive(parts.get(i));
            result = new Ast.Binary(token.getText(), result, right, line(token), col(token));
        }
        return result;
    }

    private Ast.Expression buildAdditive(SigmaParser.AdditiveExpressionContext ctx) {
        List<SigmaParser.MultiplicativeExpressionContext> parts = ctx.multiplicativeExpression();
        Ast.Expression result = buildMultiplicative(parts.get(0));
        for (int i = 1; i < parts.size(); i++) {
            ParseTree opNode = ctx.getChild(2 * i - 1);
            Token token = opNode instanceof TerminalNode ? ((TerminalNode) opNode).getSymbol() : ctx.start;
            String op = token != null ? token.getText() : opNode.getText();
            Ast.Expression right = buildMultiplicative(parts.get(i));
            result = new Ast.Binary(op, result, right, line(token), col(token));
        }
        return result;
    }

    private Ast.Expression buildMultiplicative(SigmaParser.MultiplicativeExpressionContext ctx) {
        List<SigmaParser.PowerExpressionContext> parts = ctx.powerExpression();
        Ast.Expression result = buildPower(parts.get(0));
        for (int i = 1; i < parts.size(); i++) {
            Token token = ctx.MULTIPLICATIVE(i - 1).getSymbol();
            Ast.Expression right = buildPower(parts.get(i));
            result = new Ast.Binary(token.getText(), result, right, line(token), col(token));
        }
        return result;
    }

    private Ast.Expression buildPower(SigmaParser.PowerExpressionContext ctx) {
        Ast.Expression base = buildUnary(ctx.unaryExpression());
        if (ctx.powerExpression() != null) {
            Ast.Expression exponent = buildPower(ctx.powerExpression());
            Token token = ctx.POWER().getSymbol();
            return new Ast.Binary("**", base, exponent, line(token), col(token));
        }
        return base;
    }

    private Ast.Expression buildUnary(SigmaParser.UnaryExpressionContext ctx) {
        if (ctx.NOT() != null) {
            Ast.Expression expr = buildUnary(ctx.unaryExpression());
            Token token = ctx.NOT().getSymbol();
            return new Ast.Unary("!", expr, line(token), col(token));
        }
        if (ctx.MINUS() != null) {
            Ast.Expression expr = buildUnary(ctx.unaryExpression());
            Token token = ctx.MINUS().getSymbol();
            return new Ast.Unary("-", expr, line(token), col(token));
        }
        return buildPostfix(ctx.postfixExpression());
    }

    private Ast.Expression buildPostfix(SigmaParser.PostfixExpressionContext ctx) {
        Ast.Expression expression = buildPrimary(ctx.primaryExpression());
        for (SigmaParser.PostfixOpContext opContext : ctx.postfixOp()) {
            if (opContext.DOT() != null) {
                String member = opContext.IDENTIFIER().getText();
                expression = new Ast.MemberAccess(expression, member, line(opContext), col(opContext));
            } else if (opContext.LPAREN() != null) {
                List<Ast.Expression> args = buildArguments(opContext.argumentList());
                expression = new Ast.Call(expression, args, line(opContext), col(opContext));
            }
        }
        return expression;
    }

    private List<Ast.Expression> buildArguments(SigmaParser.ArgumentListContext ctx) {
        List<Ast.Expression> args = new ArrayList<>();
        if (ctx == null || ctx.expression() == null) {
            return args;
        }
        for (SigmaParser.ExpressionContext expressionContext : ctx.expression()) {
            args.add(buildExpression(expressionContext));
        }
        return args;
    }

    private Ast.Expression buildPrimary(SigmaParser.PrimaryExpressionContext ctx) {
        if (ctx.IDENTIFIER() != null) {
            return new Ast.Identifier(ctx.IDENTIFIER().getText(), line(ctx), col(ctx));
        }
        if (ctx.literal() != null) {
            return buildLiteral(ctx.literal());
        }
        if (ctx.expression() != null) {
            return buildExpression(ctx.expression());
        }
        if (ctx.creator() != null) {
            return buildCreator(ctx.creator());
        }
        return new Ast.NullLiteral();
    }

    private Ast.Expression buildCreator(SigmaParser.CreatorContext ctx) {
        String typeName = ctx.type().getText();
        List<Ast.Expression> args = buildArguments(ctx.argumentList());
        return new Ast.NewInstance(typeName, args, line(ctx), col(ctx));
    }

    private Ast.Expression buildLiteral(SigmaParser.LiteralContext ctx) {
        if (ctx.INTEGER() != null) {
            int value = Integer.parseInt(ctx.INTEGER().getText());
            return new Ast.IntLiteral(value, line(ctx), col(ctx));
        }
        if (ctx.FLOAT() != null) {
            double value = Double.parseDouble(ctx.FLOAT().getText());
            return new Ast.DoubleLiteral(value, line(ctx), col(ctx));
        }
        if (ctx.STRING() != null) {
            return new Ast.StringLiteral(decodeString(ctx.STRING().getText()), line(ctx), col(ctx));
        }
        if (ctx.BOOLEAN() != null) {
            return new Ast.BooleanLiteral(Boolean.parseBoolean(ctx.BOOLEAN().getText()));
        }
        return new Ast.NullLiteral();
    }

    private String decodeString(String raw) {
        if (raw == null || raw.length() < 2) {
            return "";
        }
        String inner = raw.substring(1, raw.length() - 1);
        StringBuilder sb = new StringBuilder(inner.length());
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (c == '\\' && i + 1 < inner.length()) {
                char esc = inner.charAt(++i);
                switch (esc) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    default: sb.append(esc); break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private int line(ParserRuleContext ctx) {
        return ctx != null && ctx.getStart() != null ? ctx.getStart().getLine() : 0;
    }

    private int col(ParserRuleContext ctx) {
        return ctx != null && ctx.getStart() != null ? ctx.getStart().getCharPositionInLine() : 0;
    }

    private int line(Token token) {
        return token != null ? token.getLine() : 0;
    }

    private int col(Token token) {
        return token != null ? token.getCharPositionInLine() : 0;
    }
}
