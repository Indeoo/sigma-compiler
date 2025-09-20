package org.example.semantic;

import org.antlr.v4.runtime.tree.ParseTree;
import org.example.parser.SigmaBaseVisitor;
import org.example.parser.SigmaParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Semantic analyzer for the Sigma language.
 * Performs type checking, scope analysis, and symbol resolution.
 * Returns a SemanticResult containing the symbol table and any errors/warnings.
 */
public class SigmaSemanticAnalyzer extends SigmaBaseVisitor<SigmaType> {

    private SymbolTable symbolTable;
    private List<String> errors;
    private List<String> warnings;
    private SigmaType currentMethodReturnType;

    public SigmaSemanticAnalyzer() {
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.currentMethodReturnType = null;
    }

    /**
     * Analyze a parse tree and return semantic analysis results
     *
     * @param parseTree the parse tree to analyze
     * @return SemanticResult containing symbol table and any errors/warnings
     */
    public SemanticResult analyze(ParseTree parseTree) {
        // Reset state for new analysis
        this.symbolTable = new SymbolTable();
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.currentMethodReturnType = null;

        try {
            // Visit the parse tree to perform semantic analysis
            visit(parseTree);

            if (hasErrors()) {
                return SemanticResult.failure(errors);
            } else if (hasWarnings()) {
                return SemanticResult.successWithWarnings(symbolTable, warnings);
            } else {
                return SemanticResult.success(symbolTable);
            }

        } catch (Exception e) {
            errors.add("Internal error during semantic analysis: " + e.getMessage());
            return SemanticResult.failure(errors);
        }
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }

    private void addError(String message) {
        errors.add(message);
    }

    private void addWarning(String message) {
        warnings.add(message);
    }

    @Override
    public SigmaType visitCompilationUnit(SigmaParser.CompilationUnitContext ctx) {
        for (SigmaParser.DeclarationContext decl : ctx.declaration()) {
            visit(decl);
        }
        for (SigmaParser.StatementContext stmt : ctx.statement()) {
            visit(stmt);
        }
        return SigmaType.VOID;
    }

    @Override
    public SigmaType visitVariableDeclaration(SigmaParser.VariableDeclarationContext ctx) {
        String varName = ctx.IDENTIFIER().getText();
        SigmaType varType = getTypeFromContext(ctx.type());

        // Check if variable is already declared in current scope
        if (symbolTable.isDefinedInCurrentScope(varName)) {
            addError("Variable '" + varName + "' is already declared in this scope");
            return SigmaType.UNKNOWN;
        }

        Symbol symbol = new Symbol(varName, varType, Symbol.SymbolType.VARIABLE);

        // Check if there's an initialization expression
        if (ctx.expression() != null) {
            SigmaType exprType = visit(ctx.expression());
            if (!varType.isCompatibleWith(exprType)) {
                addError("Cannot assign " + exprType + " to variable '" + varName + "' of type " + varType);
            }
            symbol.setValue(null); // Mark as initialized
        } else {
            // Add warning for uninitialized variables in some cases
            if (varType != SigmaType.BOOLEAN) { // Boolean has default false
                addWarning("Variable '" + varName + "' declared but not initialized");
            }
        }

        symbolTable.define(symbol);
        return varType;
    }

    @Override
    public SigmaType visitMethodDeclaration(SigmaParser.MethodDeclarationContext ctx) {
        String methodName = ctx.IDENTIFIER().getText();
        SigmaType returnType = getTypeFromContext(ctx.type());

        // Check if method is already declared in current scope
        if (symbolTable.isDefinedInCurrentScope(methodName)) {
            addError("Method '" + methodName + "' is already declared in this scope");
            return SigmaType.UNKNOWN;
        }

        Symbol methodSymbol = new Symbol(methodName, returnType, Symbol.SymbolType.METHOD);
        symbolTable.define(methodSymbol);

        // Enter method scope
        symbolTable.enterScope("method_" + methodName);
        currentMethodReturnType = returnType;

        // Process parameters
        if (ctx.parameterList() != null) {
            visit(ctx.parameterList());
        }

        // Process method body
        visit(ctx.block());

        // Exit method scope
        symbolTable.exitScope();
        currentMethodReturnType = null;

        return returnType;
    }

    @Override
    public SigmaType visitParameterList(SigmaParser.ParameterListContext ctx) {
        for (SigmaParser.ParameterContext param : ctx.parameter()) {
            visit(param);
        }
        return SigmaType.VOID;
    }

    @Override
    public SigmaType visitParameter(SigmaParser.ParameterContext ctx) {
        String paramName = ctx.IDENTIFIER().getText();
        SigmaType paramType = getTypeFromContext(ctx.type());

        if (symbolTable.isDefinedInCurrentScope(paramName)) {
            addError("Parameter '" + paramName + "' is already declared");
            return SigmaType.UNKNOWN;
        }

        Symbol paramSymbol = new Symbol(paramName, paramType, Symbol.SymbolType.PARAMETER);
        paramSymbol.setValue(null); // Parameters are considered initialized
        symbolTable.define(paramSymbol);

        return paramType;
    }

    @Override
    public SigmaType visitClassDeclaration(SigmaParser.ClassDeclarationContext ctx) {
        String className = ctx.IDENTIFIER().getText();

        if (symbolTable.isDefinedInCurrentScope(className)) {
            addError("Class '" + className + "' is already declared in this scope");
            return SigmaType.UNKNOWN;
        }

        Symbol classSymbol = new Symbol(className, SigmaType.UNKNOWN, Symbol.SymbolType.CLASS);
        symbolTable.define(classSymbol);

        // Enter class scope
        symbolTable.enterScope("class_" + className);
        visit(ctx.classBody());
        symbolTable.exitScope();

        return SigmaType.UNKNOWN;
    }

    @Override
    public SigmaType visitClassBody(SigmaParser.ClassBodyContext ctx) {
        // Visit all declarations and statements in the class body
        for (SigmaParser.DeclarationContext decl : ctx.declaration()) {
            visit(decl);
        }
        for (SigmaParser.StatementContext stmt : ctx.statement()) {
            visit(stmt);
        }
        return SigmaType.UNKNOWN;
    }

    @Override
    public SigmaType visitAssignmentStatement(SigmaParser.AssignmentStatementContext ctx) {
        String varName = ctx.IDENTIFIER().getText();
        Symbol symbol = symbolTable.lookup(varName);

        if (symbol == null) {
            addError("Undefined variable '" + varName + "'");
            return SigmaType.UNKNOWN;
        }

        if (symbol.getSymbolType() != Symbol.SymbolType.VARIABLE &&
            symbol.getSymbolType() != Symbol.SymbolType.PARAMETER) {
            addError("Cannot assign to '" + varName + "' - it is not a variable");
            return SigmaType.UNKNOWN;
        }

        SigmaType exprType = visit(ctx.expression());
        if (!symbol.getType().isCompatibleWith(exprType)) {
            addError("Cannot assign " + exprType + " to variable '" + varName + "' of type " + symbol.getType());
        }

        return symbol.getType();
    }

    @Override
    public SigmaType visitIfStatement(SigmaParser.IfStatementContext ctx) {
        SigmaType conditionType = visit(ctx.expression());
        if (conditionType != SigmaType.BOOLEAN && conditionType != SigmaType.UNKNOWN) {
            addError("If condition must be boolean, got " + conditionType);
        }

        symbolTable.enterScope("if");
        visit(ctx.statement(0)); // then branch

        if (ctx.statement().size() > 1) { // else branch
            symbolTable.exitScope();
            symbolTable.enterScope("else");
            visit(ctx.statement(1));
        }

        symbolTable.exitScope();
        return SigmaType.VOID;
    }

    @Override
    public SigmaType visitWhileStatement(SigmaParser.WhileStatementContext ctx) {
        SigmaType conditionType = visit(ctx.expression());
        if (conditionType != SigmaType.BOOLEAN && conditionType != SigmaType.UNKNOWN) {
            addError("While condition must be boolean, got " + conditionType);
        }

        symbolTable.enterScope("while");
        visit(ctx.statement());
        symbolTable.exitScope();

        return SigmaType.VOID;
    }

    @Override
    public SigmaType visitReturnStatement(SigmaParser.ReturnStatementContext ctx) {
        if (currentMethodReturnType == null) {
            addError("Return statement outside of method");
            return SigmaType.UNKNOWN;
        }

        if (ctx.expression() != null) {
            SigmaType returnType = visit(ctx.expression());
            if (!currentMethodReturnType.isCompatibleWith(returnType)) {
                addError("Cannot return " + returnType + " from method expecting " + currentMethodReturnType);
            }
            return returnType;
        } else {
            if (currentMethodReturnType != SigmaType.VOID) {
                addError("Method expecting " + currentMethodReturnType + " must return a value");
            }
            return SigmaType.VOID;
        }
    }

    @Override
    public SigmaType visitBlock(SigmaParser.BlockContext ctx) {
        symbolTable.enterScope("block");

        // Visit all declarations in the block
        for (SigmaParser.DeclarationContext decl : ctx.declaration()) {
            visit(decl);
        }

        // Visit all statements in the block
        for (SigmaParser.StatementContext stmt : ctx.statement()) {
            visit(stmt);
        }

        symbolTable.exitScope();
        return SigmaType.VOID;
    }

    @Override
    public SigmaType visitExpression(SigmaParser.ExpressionContext ctx) {
        if (ctx.primary() != null) {
            return visit(ctx.primary());
        }

        // Binary operations
        if (ctx.expression().size() == 2) {
            SigmaType leftType = visit(ctx.expression(0));
            SigmaType rightType = visit(ctx.expression(1));
            String operator = getOperatorFromContext(ctx);

            SigmaType resultType = SigmaType.getBinaryOperationResultType(leftType, rightType, operator);
            if (resultType == SigmaType.UNKNOWN) {
                addError("Invalid operation " + leftType + " " + operator + " " + rightType);
            }
            return resultType;
        }

        // Unary operations
        if (ctx.expression().size() == 1) {
            SigmaType operandType = visit(ctx.expression(0));
            String operator = getOperatorFromContext(ctx);

            if (operator.equals("!") && operandType != SigmaType.BOOLEAN && operandType != SigmaType.UNKNOWN) {
                addError("Logical NOT can only be applied to boolean, got " + operandType);
                return SigmaType.UNKNOWN;
            } else if (operator.equals("-") && operandType != SigmaType.INT &&
                      operandType != SigmaType.DOUBLE && operandType != SigmaType.UNKNOWN) {
                addError("Unary minus can only be applied to numbers, got " + operandType);
                return SigmaType.UNKNOWN;
            }

            return operandType;
        }

        // Method call
        if (ctx.IDENTIFIER() != null) {
            String methodName = ctx.IDENTIFIER().getText();

            // Handle System.out.println calls specially
            if (methodName.equals("println") || methodName.equals("print")) {
                // Visit arguments if present
                if (ctx.argumentList() != null) {
                    visit(ctx.argumentList());
                }
                return SigmaType.VOID;
            }

            Symbol methodSymbol = symbolTable.lookup(methodName);

            if (methodSymbol == null) {
                addError("Undefined method '" + methodName + "'");
                return SigmaType.UNKNOWN;
            }

            if (methodSymbol.getSymbolType() != Symbol.SymbolType.METHOD) {
                addError("'" + methodName + "' is not a method");
                return SigmaType.UNKNOWN;
            }

            // Visit arguments if present
            if (ctx.argumentList() != null) {
                visit(ctx.argumentList());
            }

            return methodSymbol.getType();
        }

        return SigmaType.UNKNOWN;
    }

    @Override
    public SigmaType visitPrimary(SigmaParser.PrimaryContext ctx) {
        if (ctx.IDENTIFIER() != null) {
            String varName = ctx.IDENTIFIER().getText();

            // Handle special built-in identifiers
            if (varName.equals("System")) {
                return SigmaType.UNKNOWN; // System object, we'll handle it in method calls
            }

            Symbol symbol = symbolTable.lookup(varName);

            if (symbol == null) {
                addError("Undefined identifier '" + varName + "'");
                return SigmaType.UNKNOWN;
            }

            return symbol.getType();
        }

        if (ctx.literal() != null) {
            return visit(ctx.literal());
        }

        return SigmaType.UNKNOWN;
    }

    @Override
    public SigmaType visitLiteral(SigmaParser.LiteralContext ctx) {
        if (ctx.INTEGER() != null) {
            return SigmaType.INT;
        } else if (ctx.FLOAT() != null) {
            return SigmaType.DOUBLE;
        } else if (ctx.STRING() != null) {
            return SigmaType.STRING;
        } else if (ctx.BOOLEAN() != null) {
            return SigmaType.BOOLEAN;
        } else if (ctx.getText().equals("null")) {
            return SigmaType.NULL;
        }
        return SigmaType.UNKNOWN;
    }

    // Helper methods
    private SigmaType getTypeFromContext(SigmaParser.TypeContext ctx) {
        return SigmaType.fromString(ctx.getText());
    }

    private String getOperatorFromContext(SigmaParser.ExpressionContext ctx) {
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            String text = child.getText();
            if (text.matches("[+\\-*/!<>=&|%]+")) {
                return text;
            }
        }
        return "";
    }
}