package org.sigma.parser.rd;

import org.sigma.lexer.SigmaToken;
import org.sigma.lexer.TokenType;
import org.sigma.parser.Ast;

import java.util.ArrayList;
import java.util.List;

import static org.sigma.parser.rd.RuleCombinators.*;

/**
 * Pure Recursive Descent Parser for Sigma language without ANTLR dependencies.
 * Grammar rules are defined in code using a combinator-based approach for easy modification.
 * Grammar based on Sigma.g4.
 */
public class SigmaRecursiveDescentParser {

    // ===== Grammar Rules =====
    // Each rule corresponds to a production in Sigma.g4

    /**
     * compilationUnit: (declaration | statement)* EOF
     */
    public GrammarRule<Ast.CompilationUnit> compilationUnit() {
        return ctx -> {
            List<Ast.Statement> statements = new ArrayList<>();

            while (!ctx.isAtEnd()) {
                int startPos = ctx.getPosition();
                Ast.Statement stmt = declarationOrStatement().parse(ctx);
                if (stmt != null) {
                    statements.add(stmt);
                } else {
                    // Error recovery: synchronize to next statement
                    if (!ctx.isAtEnd()) {
                        ctx.error("Expected declaration or statement");
                        ctx.synchronize();

                        // Safety: ensure we made progress to avoid infinite loop
                        if (ctx.getPosition() == startPos) {
                            ctx.consume(); // Force advance
                        }
                    }
                }
            }

            return new Ast.CompilationUnit(statements);
        };
    }

    /**
     * Helper: declaration | statement
     */
    private GrammarRule<Ast.Statement> declarationOrStatement() {
        return choice(declaration(), statement());
    }

    /**
     * declaration: variableDeclaration | constantDeclaration | methodDeclaration | classDeclaration
     */
    private GrammarRule<Ast.Statement> declaration() {
        return ctx -> {
            // Need careful lookahead to distinguish between:
            // - variableDeclaration: type IDENTIFIER (ASSIGN ...)?  SEMI
            // - methodDeclaration:   type IDENTIFIER LPAREN ...
            // - constantDeclaration: FINAL type IDENTIFIER ASSIGN ... SEMI
            // - classDeclaration:    CLASS IDENTIFIER LBRACE ...

            // Try class declaration first (starts with CLASS)
            Ast.Statement decl = classDeclaration().parse(ctx);
            if (decl != null) return decl;

            // Try constant declaration (starts with FINAL)
            decl = constantDeclaration().parse(ctx);
            if (decl != null) return decl;

            // For method vs variable, we need to lookahead
            // If we see: type IDENTIFIER LPAREN -> method
            // Otherwise try variable
            int startPos = ctx.savePosition();

            // Check if it looks like a method: type IDENTIFIER LPAREN
            String typeName = type().parse(ctx);
            if (typeName != null) {
                SigmaToken id = ctx.match(TokenType.IDENTIFIER);
                if (id != null && ctx.check(TokenType.LPAREN)) {
                    // It's a method! Restore and parse as method
                    ctx.restorePosition(startPos);
                    decl = methodDeclaration().parse(ctx);
                    if (decl != null) return decl;
                }
            }

            // Not a method, restore and try variable declaration
            ctx.restorePosition(startPos);
            decl = variableDeclaration().parse(ctx);
            if (decl != null) return decl;

            return null;
        };
    }

    /**
     * constantDeclaration: FINAL type IDENTIFIER ASSIGN expression SEMI
     * Parses but returns as VariableDeclaration for now (until AST supports const)
     */
    private GrammarRule<Ast.Statement> constantDeclaration() {
        return ctx -> {
            int startPos = ctx.savePosition();

            if (ctx.match(TokenType.FINAL) == null) {
                ctx.restorePosition(startPos);
                return null;
            }

            String typeName = type().parse(ctx);
            if (typeName == null) {
                ctx.restorePosition(startPos);
                return null;
            }

            SigmaToken idToken = ctx.match(TokenType.IDENTIFIER);
            if (idToken == null) {
                ctx.restorePosition(startPos);
                return null;
            }

            ctx.expect(TokenType.ASSIGN, "Expected '=' in constant declaration");

            Ast.Expression init = expression().parse(ctx);
            if (init == null) {
                ctx.error("Expected expression after '='");
            }

            ctx.expect(TokenType.SEMI, "Expected ';' after constant declaration");

            // Return as VariableDeclaration (AST doesn't have ConstantDeclaration yet; SemanticAnalyzer
            // treats it as a normal variable declaration for now)
            return new Ast.VariableDeclaration(
                typeName,
                idToken.getText(),
                init,
                idToken.getLine(),
                idToken.getCharPositionInLine()
            );
        };
    }

    /**
     * methodDeclaration: type IDENTIFIER LPAREN parameterList? RPAREN block
     */
    private GrammarRule<Ast.Statement> methodDeclaration() {
        return ctx -> {
            int startPos = ctx.savePosition();

            String returnType = type().parse(ctx);
            if (returnType == null) {
                ctx.restorePosition(startPos);
                return null;
            }

            SigmaToken methodName = ctx.match(TokenType.IDENTIFIER);
            if (methodName == null) {
                ctx.restorePosition(startPos);
                return null;
            }

            if (ctx.match(TokenType.LPAREN) == null) {
                ctx.restorePosition(startPos);
                return null;
            }

            // Parse parameter list
            List<Ast.Parameter> parameters = new ArrayList<>();
            if (!ctx.check(TokenType.RPAREN)) {
                // Parse parameters: type IDENTIFIER (COMMA type IDENTIFIER)*
                do {
                    String paramType = type().parse(ctx);
                    if (paramType == null) {
                        ctx.error("Expected parameter type");
                        break;
                    }
                    SigmaToken paramName = ctx.match(TokenType.IDENTIFIER);
                    if (paramName == null) {
                        ctx.error("Expected parameter name");
                        break;
                    }
                    parameters.add(new Ast.Parameter(
                        paramType,
                        paramName.getText(),
                        paramName.getLine(),
                        paramName.getCharPositionInLine()
                    ));
                } while (ctx.match(TokenType.COMMA) != null);
            }

            ctx.expect(TokenType.RPAREN, "Expected ')' after parameters");

            Ast.Block body = block().parse(ctx);
            if (body == null) {
                ctx.error("Expected method body");
            }

            // Return proper MethodDeclaration node
            return new Ast.MethodDeclaration(
                returnType,
                methodName.getText(),
                parameters,
                body,
                methodName.getLine(),
                methodName.getCharPositionInLine()
            );
        };
    }

    /**
     * classDeclaration: CLASS IDENTIFIER classBody
     * classBody: LBRACE (declaration | statement)* RBRACE
     */
    private GrammarRule<Ast.Statement> classDeclaration() {
        return ctx -> {
            int startPos = ctx.savePosition();

            if (ctx.match(TokenType.CLASS) == null) {
                ctx.restorePosition(startPos);
                return null;
            }

            SigmaToken className = ctx.match(TokenType.IDENTIFIER);
            if (className == null) {
                ctx.error("Expected class name after 'class'");
            }

            // Parse class body
            if (ctx.match(TokenType.LBRACE) == null) {
                ctx.error("Expected '{' after class name");
                return null;
            }

            List<Ast.Statement> members = new ArrayList<>();

            while (!ctx.check(TokenType.RBRACE) && !ctx.isAtEnd()) {
                int memberStartPos = ctx.getPosition();
                Ast.Statement member = declarationOrStatement().parse(ctx);
                if (member != null) {
                    members.add(member);
                } else {
                    ctx.error("Expected declaration or statement in class body");
                    ctx.synchronize();

                    // Safety: ensure we made progress to avoid infinite loop
                    if (ctx.getPosition() == memberStartPos) {
                        ctx.consume(); // Force advance
                    }
                }
            }

            ctx.expect(TokenType.RBRACE, "Expected '}' to close class");

            // Return proper ClassDeclaration node
            return new Ast.ClassDeclaration(
                className != null ? className.getText() : "<error>",
                members,
                className != null ? className.getLine() : 0,
                className != null ? className.getCharPositionInLine() : 0
            );
        };
    }

    /**
     * variableDeclaration: type IDENTIFIER (ASSIGN expression)? SEMI
     */
    private GrammarRule<Ast.VariableDeclaration> variableDeclaration() {
        return ctx -> {
            int startPos = ctx.savePosition();

            String typeName = type().parse(ctx);
            if (typeName == null) {
                ctx.restorePosition(startPos);
                return null;
            }

            SigmaToken idToken = ctx.match(TokenType.IDENTIFIER);
            if (idToken == null) {
                ctx.restorePosition(startPos);
                return null;
            }

            Ast.Expression init = null;
            if (ctx.match(TokenType.ASSIGN) != null) {
                init = expression().parse(ctx);
                if (init == null) {
                    ctx.error("Expected expression after '='");
                }
            }

            SigmaToken semi = ctx.match(TokenType.SEMI);
            if (semi == null) {
                ctx.error("Expected ';' after variable declaration");
            }

            return new Ast.VariableDeclaration(
                typeName,
                idToken.getText(),
                init,
                idToken.getLine(),
                idToken.getCharPositionInLine()
            );
        };
    }

    /**
     * type: IDENTIFIER | PRIMITIVE_TYPE | STRING_TYPE | VOID
     */
    private GrammarRule<String> type() {
        return ctx -> {
            SigmaToken token = ctx.match(
                TokenType.IDENTIFIER,
                TokenType.INT,
                TokenType.DOUBLE,
                TokenType.FLOAT,
                TokenType.BOOLEAN,
                TokenType.STRING_TYPE,
                TokenType.VOID
            );
            return token != null ? token.getText() : null;
        };
    }

    /**
     * statement: assignmentStatement | expressionStatement | ifStatement | forStatement | whileStatement | returnStatement | block
     */
    private GrammarRule<Ast.Statement> statement() {
        return ctx -> {
            // Try each statement type in order
            Ast.Statement stmt;

            stmt = ifStatement().parse(ctx);
            if (stmt != null) return stmt;

            stmt = forStatement().parse(ctx);
            if (stmt != null) return stmt;

            stmt = whileStatement().parse(ctx);
            if (stmt != null) return stmt;

            stmt = returnStatement().parse(ctx);
            if (stmt != null) return stmt;

            stmt = block().parse(ctx);
            if (stmt != null) return stmt;

            // Try assignment statement (must check for IDENTIFIER ASSIGN pattern)
            stmt = assignmentStatement().parse(ctx);
            if (stmt != null) return stmt;

            // Finally try expression statement
            stmt = expressionStatement().parse(ctx);
            if (stmt != null) return stmt;

            return null;
        };
    }

    /**
     * assignmentStatement: IDENTIFIER ASSIGN expression SEMI
     */
    private GrammarRule<Ast.Assignment> assignmentStatement() {
        return ctx -> {
            int startPos = ctx.savePosition();

            SigmaToken id = ctx.match(TokenType.IDENTIFIER);
            if (id == null) {
                ctx.restorePosition(startPos);
                return null;
            }

            SigmaToken assign = ctx.match(TokenType.ASSIGN);
            if (assign == null) {
                ctx.restorePosition(startPos);
                return null;
            }

            Ast.Expression value = expression().parse(ctx);
            if (value == null) {
                ctx.error("Expected expression after '='");
                ctx.synchronize();
                return null;
            }

            SigmaToken semi = ctx.match(TokenType.SEMI);
            if (semi == null) {
                ctx.error("Expected ';' after assignment");
            }

            return new Ast.Assignment(id.getText(), value);
        };
    }

    /**
     * expressionStatement: expression SEMI?
     */
    private GrammarRule<Ast.ExpressionStatement> expressionStatement() {
        return ctx -> {
            Ast.Expression expr = expression().parse(ctx);
            if (expr == null) {
                return null;
            }

            SigmaToken firstToken = ctx.current();
            ctx.match(TokenType.SEMI); // Optional semicolon

            return new Ast.ExpressionStatement(expr, firstToken.getLine(), firstToken.getCharPositionInLine());
        };
    }

    /**
     * ifStatement: IF ifCondition statement (ELSE statement)?
     */
    private GrammarRule<Ast.IfStatement> ifStatement() {
        return ctx -> {
            SigmaToken ifToken = ctx.match(TokenType.IF);
            if (ifToken == null) {
                return null;
            }

            boolean hasParens = ctx.match(TokenType.LPAREN) != null;

            Ast.Expression cond = expression().parse(ctx);
            if (cond == null) {
                ctx.error("Expected condition expression");
            }

            if (hasParens) {
                ctx.expect(TokenType.RPAREN, "Expected ')' after if condition");
            }

            Ast.Statement thenBranch = statement().parse(ctx);
            if (thenBranch == null) {
                ctx.error("Expected statement after if condition");
            }

            Ast.Statement elseBranch = null;
            if (ctx.match(TokenType.ELSE) != null) {
                elseBranch = statement().parse(ctx);
                if (elseBranch == null) {
                    ctx.error("Expected statement after 'else'");
                }
            }

            return new Ast.IfStatement(cond, thenBranch, elseBranch);
        };
    }

    /**
     * forStatement: FOR forClause statement
     */
    private GrammarRule<Ast.ForEachStatement> forStatement() {
        return ctx -> {
            SigmaToken forToken = ctx.match(TokenType.FOR);
            if (forToken == null) {
                return null;
            }

            boolean hasParens = ctx.match(TokenType.LPAREN) != null;

            String typeName = null;
            SigmaToken iteratorToken = null;

            int clauseStart = ctx.savePosition();
            String possibleType = type().parse(ctx);
            if (possibleType != null) {
                SigmaToken maybeIterator = ctx.match(TokenType.IDENTIFIER);
                if (maybeIterator != null) {
                    typeName = possibleType;
                    iteratorToken = maybeIterator;
                } else {
                    ctx.restorePosition(clauseStart);
                }
            } else {
                ctx.restorePosition(clauseStart);
            }

            if (iteratorToken == null) {
                SigmaToken idToken = ctx.match(TokenType.IDENTIFIER);
                if (idToken == null) {
                    ctx.error("Expected iterator variable in for-loop");
                    return null;
                }
                iteratorToken = idToken;
            }

            ctx.expect(TokenType.IN, "Expected 'in' in for-loop");

            Ast.Expression iterableExpr = expression().parse(ctx);
            if (iterableExpr == null) {
                ctx.error("Expected iterable expression after 'in'");
            }

            if (hasParens) {
                ctx.expect(TokenType.RPAREN, "Expected ')' after for clause");
            }

            Ast.Statement body = statement().parse(ctx);
            if (body == null) {
                ctx.error("Expected statement after for-loop");
            }

            return new Ast.ForEachStatement(
                typeName,
                iteratorToken.getText(),
                iterableExpr,
                body,
                iteratorToken.getLine(),
                iteratorToken.getCharPositionInLine()
            );
        };
    }

    /**
     * whileStatement: WHILE LPAREN expression RPAREN statement
     */
    private GrammarRule<Ast.WhileStatement> whileStatement() {
        return ctx -> {
            if (ctx.match(TokenType.WHILE) == null) {
                return null;
            }

            ctx.expect(TokenType.LPAREN, "Expected '(' after 'while'");

            Ast.Expression cond = expression().parse(ctx);
            if (cond == null) {
                ctx.error("Expected condition expression");
            }

            ctx.expect(TokenType.RPAREN, "Expected ')' after while condition");

            Ast.Statement body = statement().parse(ctx);
            if (body == null) {
                ctx.error("Expected statement after while condition");
            }

            return new Ast.WhileStatement(cond, body);
        };
    }

    /**
     * returnStatement: RETURN expression? SEMI
     */
    private GrammarRule<Ast.ReturnStatement> returnStatement() {
        return ctx -> {
            SigmaToken returnToken = ctx.match(TokenType.RETURN);
            if (returnToken == null) {
                return null;
            }

            Ast.Expression expr = null;
            if (!ctx.check(TokenType.SEMI)) {
                expr = expression().parse(ctx);
            }

            ctx.expect(TokenType.SEMI, "Expected ';' after return statement");

            return new Ast.ReturnStatement(expr, returnToken.getLine(), returnToken.getCharPositionInLine());
        };
    }

    /**
     * block: LBRACE (declaration | statement)* RBRACE
     */
    private GrammarRule<Ast.Block> block() {
        return ctx -> {
            if (ctx.match(TokenType.LBRACE) == null) {
                return null;
            }

            List<Ast.Statement> statements = new ArrayList<>();

            while (!ctx.check(TokenType.RBRACE) && !ctx.isAtEnd()) {
                int startPos = ctx.getPosition();
                Ast.Statement stmt = declarationOrStatement().parse(ctx);
                if (stmt != null) {
                    statements.add(stmt);
                } else {
                    ctx.error("Expected declaration or statement");
                    ctx.synchronize();

                    // Safety: ensure we made progress to avoid infinite loop
                    if (ctx.getPosition() == startPos) {
                        ctx.consume(); // Force advance
                    }
                }
            }

            ctx.expect(TokenType.RBRACE, "Expected '}' to close block");

            return new Ast.Block(statements);
        };
    }

    // ===== Expression Grammar =====

    /**
     * expression: logicalOrExpression
     */
    private GrammarRule<Ast.Expression> expression() {
        return logicalOrExpression();
    }

    /**
     * logicalOrExpression: logicalAndExpression (LOGICAL['||'] logicalAndExpression)*
     */
    private GrammarRule<Ast.Expression> logicalOrExpression() {
        return leftAssociative(
            logicalAndExpression(),
            ctx -> {
                SigmaToken token = ctx.current();
                if (token.getType() == TokenType.OR) {
                    return ctx.consume();
                }
                return null;
            },
            (left, op, right) -> new Ast.Binary(
                op.getText(), left, right,
                op.getLine(), op.getCharPositionInLine()
            )
        );
    }

    /**
     * logicalAndExpression: relationalExpression (LOGICAL['&&'] relationalExpression)*
     */
    private GrammarRule<Ast.Expression> logicalAndExpression() {
        return leftAssociative(
            relationalExpression(),
            ctx -> {
                SigmaToken token = ctx.current();
                if (token.getType() == TokenType.AND) {
                    return ctx.consume();
                }
                return null;
            },
            (left, op, right) -> new Ast.Binary(
                op.getText(), left, right,
                op.getLine(), op.getCharPositionInLine()
            )
        );
    }

    /**
     * relationalExpression: additiveExpression (RELATIONAL additiveExpression)*
     */
    private GrammarRule<Ast.Expression> relationalExpression() {
        return leftAssociative(
            additiveExpression(),
            ctx -> ctx.match(TokenType.LT, TokenType.LE, TokenType.GT, TokenType.GE, TokenType.EQ, TokenType.NE),
            (left, op, right) -> new Ast.Binary(
                op.getText(), left, right,
                op.getLine(), op.getCharPositionInLine()
            )
        );
    }

    /**
     * additiveExpression: multiplicativeExpression ((PLUS | MINUS) multiplicativeExpression)*
     */
    private GrammarRule<Ast.Expression> additiveExpression() {
        return leftAssociative(
            multiplicativeExpression(),
            ctx -> ctx.match(TokenType.PLUS, TokenType.MINUS),
            (left, op, right) -> new Ast.Binary(
                op.getText(), left, right,
                op.getLine(), op.getCharPositionInLine()
            )
        );
    }

    /**
     * multiplicativeExpression: powerExpression (MULTIPLICATIVE powerExpression)*
     */
    private GrammarRule<Ast.Expression> multiplicativeExpression() {
        return leftAssociative(
            powerExpression(),
            ctx -> ctx.match(TokenType.MULT, TokenType.DIV, TokenType.MOD),
            (left, op, right) -> new Ast.Binary(
                op.getText(), left, right,
                op.getLine(), op.getCharPositionInLine()
            )
        );
    }

    /**
     * powerExpression: unaryExpression (POWER powerExpression)?
     * Power is right-associative: 2**3**4 = 2**(3**4)
     */
    private GrammarRule<Ast.Expression> powerExpression() {
        return rightAssociative(
            unaryExpression(),
            ctx -> ctx.match(TokenType.POWER),
            (left, op, right) -> new Ast.Binary(
                op.getText(), left, right,
                op.getLine(), op.getCharPositionInLine()
            )
        );
    }

    /**
     * unaryExpression: NOT unaryExpression | MINUS unaryExpression | postfixExpression
     */
    private GrammarRule<Ast.Expression> unaryExpression() {
        return ctx -> {
            // Try NOT
            SigmaToken notToken = ctx.match(TokenType.NOT);
            if (notToken != null) {
                Ast.Expression expr = unaryExpression().parse(ctx);
                if (expr == null) {
                    ctx.error("Expected expression after '!'");
                }
                return new Ast.Unary("!", expr, notToken.getLine(), notToken.getCharPositionInLine());
            }

            // Try MINUS (unary negation)
            SigmaToken minusToken = ctx.match(TokenType.MINUS);
            if (minusToken != null) {
                Ast.Expression expr = unaryExpression().parse(ctx);
                if (expr == null) {
                    ctx.error("Expected expression after '-'");
                }
                return new Ast.Unary("neg", expr, minusToken.getLine(), minusToken.getCharPositionInLine());
            }

            // Otherwise postfix expression
            return postfixExpression().parse(ctx);
        };
    }

    /**
     * postfixExpression: primaryExpression (postfixOp)*
     * postfixOp: DOT IDENTIFIER | LPAREN argumentList? RPAREN
     */
    private GrammarRule<Ast.Expression> postfixExpression() {
        return ctx -> {
            Ast.Expression expr = primaryExpression().parse(ctx);
            if (expr == null) {
                return null;
            }

            // Handle postfix operations
            while (true) {
                // Method call: LPAREN argumentList? RPAREN
                if (ctx.check(TokenType.LPAREN)) {
                    SigmaToken lparen = ctx.consume();
                    List<Ast.Expression> args = new ArrayList<>();

                    if (!ctx.check(TokenType.RPAREN)) {
                        List<Ast.Expression> argList = commaSeparated(expression()).parse(ctx);
                        if (argList != null) {
                            args.addAll(argList);
                        }
                    }

                    ctx.expect(TokenType.RPAREN, "Expected ')' after arguments");
                    expr = new Ast.Call(expr, args, lparen.getLine(), lparen.getCharPositionInLine());
                }
                // Member access: DOT IDENTIFIER
                else if (ctx.check(TokenType.DOT)) {
                    SigmaToken dot = ctx.consume(); // consume DOT
                    SigmaToken memberName = ctx.match(TokenType.IDENTIFIER);
                    if (memberName == null) {
                        ctx.error("Expected identifier after '.'");
                        break;
                    }
                    expr = new Ast.MemberAccess(
                        expr,
                        memberName.getText(),
                        dot.getLine(),
                        dot.getCharPositionInLine()
                    );
                }
                else {
                    break;
                }
            }

            return expr;
        };
    }

    /**
     * primaryExpression: IDENTIFIER | literal | LPAREN expression RPAREN
     */
    private GrammarRule<Ast.Expression> primaryExpression() {
        return ctx -> {
            // Try identifier
            SigmaToken id = ctx.match(TokenType.IDENTIFIER);
            if (id != null) {
                return new Ast.Identifier(id.getText(), id.getLine(), id.getCharPositionInLine());
            }

            // Try literal
            Ast.Expression lit = literal().parse(ctx);
            if (lit != null) {
                return lit;
            }

            // Try parenthesized expression
            if (ctx.match(TokenType.LPAREN) != null) {
                Ast.Expression expr = expression().parse(ctx);
                if (expr == null) {
                    ctx.error("Expected expression after '('");
                }
                ctx.expect(TokenType.RPAREN, "Expected ')' after expression");
                return expr;
            }

            // Try object creation with 'new'
            if (ctx.match(TokenType.NEW) != null) {
                // Expect class name (could be primitive type or identifier)
                SigmaToken classNameToken = ctx.current();
                String className;

                if (ctx.match(TokenType.IDENTIFIER) != null) {
                    className = classNameToken.getText();
                } else if (ctx.match(TokenType.INT) != null) {
                    className = "int";
                } else if (ctx.match(TokenType.DOUBLE) != null) {
                    className = "double";
                } else if (ctx.match(TokenType.FLOAT) != null) {
                    className = "float";
                } else if (ctx.match(TokenType.BOOLEAN) != null) {
                    className = "boolean";
                } else if (ctx.match(TokenType.STRING_TYPE) != null) {
                    className = "String";
                } else {
                    ctx.error("Expected class name after 'new'");
                    return null;
                }

                // Expect '('
                SigmaToken lparen = ctx.expect(TokenType.LPAREN, "Expected '(' after class name in constructor call");

                // Parse argument list
                List<Ast.Expression> args = new ArrayList<>();
                if (!ctx.check(TokenType.RPAREN)) {
                    List<Ast.Expression> argList = commaSeparated(expression()).parse(ctx);
                    if (argList != null) {
                        args.addAll(argList);
                    }
                }

                // Expect ')'
                ctx.expect(TokenType.RPAREN, "Expected ')' after constructor arguments");

                return new Ast.NewInstance(className, args, lparen.getLine(), lparen.getCharPositionInLine());
            }

            return null;
        };
    }

    /**
     * literal: INTEGER | FLOAT | STRING | BOOLEAN | NULL
     */
    private GrammarRule<Ast.Expression> literal() {
        return ctx -> {
            SigmaToken token = ctx.current();

            switch (token.getType()) {
                case INTEGER:
                    ctx.consume();
                    int intValue = Integer.parseInt(token.getText());
                    return new Ast.IntLiteral(intValue, token.getLine(), token.getCharPositionInLine());

                case FLOAT_LITERAL:
                    ctx.consume();
                    double doubleValue = Double.parseDouble(token.getText());
                    return new Ast.DoubleLiteral(doubleValue, token.getLine(), token.getCharPositionInLine());

                case STRING:
                    ctx.consume();
                    return new Ast.StringLiteral(token.getText(), token.getLine(), token.getCharPositionInLine());

                case TRUE:
                    ctx.consume();
                    return new Ast.BooleanLiteral(true);

                case FALSE:
                    ctx.consume();
                    return new Ast.BooleanLiteral(false);

                case NULL:
                    ctx.consume();
                    return new Ast.Identifier("null", token.getLine(), token.getCharPositionInLine());

                default:
                    return null;
            }
        };
    }

    // ===== Public API =====

    /**
     * Parse a list of tokens into an AST.
     *
     * @param tokens the token list to parse (must include EOF token at end)
     * @return the parsed CompilationUnit, or null if parsing failed
     */
    public Ast.CompilationUnit parse(List<SigmaToken> tokens) {
        ParserContext ctx = new ParserContext(tokens);
        Ast.CompilationUnit result = compilationUnit().parse(ctx);

        if (ctx.hasErrors()) {
            // Return partial AST even with errors (for better error reporting)
            return result;
        }

        return result;
    }

    /**
     * Parse tokens and return both AST and any errors.
     */
    public ParseResultWithContext parseWithContext(List<SigmaToken> tokens) {
        ParserContext ctx = new ParserContext(tokens);
        Ast.CompilationUnit ast = compilationUnit().parse(ctx);
        return new ParseResultWithContext(ast, ctx.getErrors());
    }

    /**
     * Container for parse result with errors.
     */
    public static class ParseResultWithContext {
        public final Ast.CompilationUnit ast;
        public final List<String> errors;

        public ParseResultWithContext(Ast.CompilationUnit ast, List<String> errors) {
            this.ast = ast;
            this.errors = errors;
        }

        public boolean isSuccessful() {
            return errors.isEmpty();
        }
    }
}
