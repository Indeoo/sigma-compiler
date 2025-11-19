// Generated from /Users/indeoo/Project/Maga/sigma-compiler/app/src/main/antlr/Sigma.g4 by ANTLR 4.13.2
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link SigmaParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface SigmaVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link SigmaParser#compilationUnit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompilationUnit(SigmaParser.CompilationUnitContext ctx);
	/**
	 * Visit a parse tree produced by {@link SigmaParser#declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclaration(SigmaParser.DeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SigmaParser#variableDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableDeclaration(SigmaParser.VariableDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SigmaParser#constantDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstantDeclaration(SigmaParser.ConstantDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SigmaParser#methodDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethodDeclaration(SigmaParser.MethodDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SigmaParser#classDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassDeclaration(SigmaParser.ClassDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link SigmaParser#classBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassBody(SigmaParser.ClassBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SigmaParser#parameterList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameterList(SigmaParser.ParameterListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SigmaParser#parameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameter(SigmaParser.ParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link SigmaParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(SigmaParser.StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SigmaParser#assignmentStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignmentStatement(SigmaParser.AssignmentStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SigmaParser#expressionStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionStatement(SigmaParser.ExpressionStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SigmaParser#ifStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfStatement(SigmaParser.IfStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SigmaParser#whileStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhileStatement(SigmaParser.WhileStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SigmaParser#returnStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnStatement(SigmaParser.ReturnStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link SigmaParser#block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlock(SigmaParser.BlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link SigmaParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(SigmaParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SigmaParser#logicalOrExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalOrExpression(SigmaParser.LogicalOrExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SigmaParser#logicalAndExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicalAndExpression(SigmaParser.LogicalAndExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SigmaParser#relationalExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelationalExpression(SigmaParser.RelationalExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SigmaParser#additiveExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdditiveExpression(SigmaParser.AdditiveExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SigmaParser#multiplicativeExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplicativeExpression(SigmaParser.MultiplicativeExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SigmaParser#unaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryExpression(SigmaParser.UnaryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SigmaParser#postfixExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPostfixExpression(SigmaParser.PostfixExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SigmaParser#postfixOp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPostfixOp(SigmaParser.PostfixOpContext ctx);
	/**
	 * Visit a parse tree produced by {@link SigmaParser#primaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimaryExpression(SigmaParser.PrimaryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SigmaParser#argumentList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgumentList(SigmaParser.ArgumentListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SigmaParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral(SigmaParser.LiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link SigmaParser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType(SigmaParser.TypeContext ctx);
}