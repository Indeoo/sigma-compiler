// Generated from /Users/indeoo/Project/Maga/sigma-compiler/app/src/main/antlr/Sigma.g4 by ANTLR 4.13.2
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link SigmaParser}.
 */
public interface SigmaListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link SigmaParser#compilationUnit}.
	 * @param ctx the parse tree
	 */
	void enterCompilationUnit(SigmaParser.CompilationUnitContext ctx);
	/**
	 * Exit a parse tree produced by {@link SigmaParser#compilationUnit}.
	 * @param ctx the parse tree
	 */
	void exitCompilationUnit(SigmaParser.CompilationUnitContext ctx);
	/**
	 * Enter a parse tree produced by {@link SigmaParser#declaration}.
	 * @param ctx the parse tree
	 */
	void enterDeclaration(SigmaParser.DeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SigmaParser#declaration}.
	 * @param ctx the parse tree
	 */
	void exitDeclaration(SigmaParser.DeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SigmaParser#variableDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterVariableDeclaration(SigmaParser.VariableDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SigmaParser#variableDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitVariableDeclaration(SigmaParser.VariableDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SigmaParser#constantDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterConstantDeclaration(SigmaParser.ConstantDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SigmaParser#constantDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitConstantDeclaration(SigmaParser.ConstantDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SigmaParser#methodDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterMethodDeclaration(SigmaParser.MethodDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SigmaParser#methodDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitMethodDeclaration(SigmaParser.MethodDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SigmaParser#classDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterClassDeclaration(SigmaParser.ClassDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link SigmaParser#classDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitClassDeclaration(SigmaParser.ClassDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link SigmaParser#classBody}.
	 * @param ctx the parse tree
	 */
	void enterClassBody(SigmaParser.ClassBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SigmaParser#classBody}.
	 * @param ctx the parse tree
	 */
	void exitClassBody(SigmaParser.ClassBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SigmaParser#parameterList}.
	 * @param ctx the parse tree
	 */
	void enterParameterList(SigmaParser.ParameterListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SigmaParser#parameterList}.
	 * @param ctx the parse tree
	 */
	void exitParameterList(SigmaParser.ParameterListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SigmaParser#parameter}.
	 * @param ctx the parse tree
	 */
	void enterParameter(SigmaParser.ParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link SigmaParser#parameter}.
	 * @param ctx the parse tree
	 */
	void exitParameter(SigmaParser.ParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link SigmaParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(SigmaParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SigmaParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(SigmaParser.StatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SigmaParser#assignmentStatement}.
	 * @param ctx the parse tree
	 */
	void enterAssignmentStatement(SigmaParser.AssignmentStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SigmaParser#assignmentStatement}.
	 * @param ctx the parse tree
	 */
	void exitAssignmentStatement(SigmaParser.AssignmentStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SigmaParser#expressionStatement}.
	 * @param ctx the parse tree
	 */
	void enterExpressionStatement(SigmaParser.ExpressionStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SigmaParser#expressionStatement}.
	 * @param ctx the parse tree
	 */
	void exitExpressionStatement(SigmaParser.ExpressionStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SigmaParser#ifStatement}.
	 * @param ctx the parse tree
	 */
	void enterIfStatement(SigmaParser.IfStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SigmaParser#ifStatement}.
	 * @param ctx the parse tree
	 */
	void exitIfStatement(SigmaParser.IfStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SigmaParser#whileStatement}.
	 * @param ctx the parse tree
	 */
	void enterWhileStatement(SigmaParser.WhileStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SigmaParser#whileStatement}.
	 * @param ctx the parse tree
	 */
	void exitWhileStatement(SigmaParser.WhileStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SigmaParser#returnStatement}.
	 * @param ctx the parse tree
	 */
	void enterReturnStatement(SigmaParser.ReturnStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link SigmaParser#returnStatement}.
	 * @param ctx the parse tree
	 */
	void exitReturnStatement(SigmaParser.ReturnStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link SigmaParser#block}.
	 * @param ctx the parse tree
	 */
	void enterBlock(SigmaParser.BlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link SigmaParser#block}.
	 * @param ctx the parse tree
	 */
	void exitBlock(SigmaParser.BlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link SigmaParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(SigmaParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SigmaParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(SigmaParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SigmaParser#logicalOrExpression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalOrExpression(SigmaParser.LogicalOrExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SigmaParser#logicalOrExpression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalOrExpression(SigmaParser.LogicalOrExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SigmaParser#logicalAndExpression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalAndExpression(SigmaParser.LogicalAndExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SigmaParser#logicalAndExpression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalAndExpression(SigmaParser.LogicalAndExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SigmaParser#relationalExpression}.
	 * @param ctx the parse tree
	 */
	void enterRelationalExpression(SigmaParser.RelationalExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SigmaParser#relationalExpression}.
	 * @param ctx the parse tree
	 */
	void exitRelationalExpression(SigmaParser.RelationalExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SigmaParser#additiveExpression}.
	 * @param ctx the parse tree
	 */
	void enterAdditiveExpression(SigmaParser.AdditiveExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SigmaParser#additiveExpression}.
	 * @param ctx the parse tree
	 */
	void exitAdditiveExpression(SigmaParser.AdditiveExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SigmaParser#multiplicativeExpression}.
	 * @param ctx the parse tree
	 */
	void enterMultiplicativeExpression(SigmaParser.MultiplicativeExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SigmaParser#multiplicativeExpression}.
	 * @param ctx the parse tree
	 */
	void exitMultiplicativeExpression(SigmaParser.MultiplicativeExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SigmaParser#unaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterUnaryExpression(SigmaParser.UnaryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SigmaParser#unaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitUnaryExpression(SigmaParser.UnaryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SigmaParser#postfixExpression}.
	 * @param ctx the parse tree
	 */
	void enterPostfixExpression(SigmaParser.PostfixExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SigmaParser#postfixExpression}.
	 * @param ctx the parse tree
	 */
	void exitPostfixExpression(SigmaParser.PostfixExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SigmaParser#postfixOp}.
	 * @param ctx the parse tree
	 */
	void enterPostfixOp(SigmaParser.PostfixOpContext ctx);
	/**
	 * Exit a parse tree produced by {@link SigmaParser#postfixOp}.
	 * @param ctx the parse tree
	 */
	void exitPostfixOp(SigmaParser.PostfixOpContext ctx);
	/**
	 * Enter a parse tree produced by {@link SigmaParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterPrimaryExpression(SigmaParser.PrimaryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SigmaParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitPrimaryExpression(SigmaParser.PrimaryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SigmaParser#argumentList}.
	 * @param ctx the parse tree
	 */
	void enterArgumentList(SigmaParser.ArgumentListContext ctx);
	/**
	 * Exit a parse tree produced by {@link SigmaParser#argumentList}.
	 * @param ctx the parse tree
	 */
	void exitArgumentList(SigmaParser.ArgumentListContext ctx);
	/**
	 * Enter a parse tree produced by {@link SigmaParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLiteral(SigmaParser.LiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link SigmaParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLiteral(SigmaParser.LiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link SigmaParser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(SigmaParser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SigmaParser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(SigmaParser.TypeContext ctx);
}