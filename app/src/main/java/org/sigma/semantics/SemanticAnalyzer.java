package org.sigma.semantics;

import org.sigma.parser.Ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Semantic analyzer for the Sigma language.
 * Performs type checking, name resolution, and semantic validation.
 *
 * Analysis is performed in two passes:
 * 1. Declaration collection - Register all classes, methods, and global variables
 * 2. Type checking - Resolve names, infer types, and check type compatibility
 */
public class SemanticAnalyzer {
    private final SymbolTable symbolTable;
    private final TypeRegistry typeRegistry;
    private final List<SemanticError> errors;
    private final Map<Ast.Expression, SigmaType> expressionTypes;

    // Track current method's return type for return statement checking
    private SigmaType currentMethodReturnType;

    /**
     * Create a new semantic analyzer
     */
    public SemanticAnalyzer() {
        this.symbolTable = new SymbolTable();
        this.typeRegistry = new TypeRegistry();
        this.errors = new ArrayList<>();
        this.expressionTypes = new HashMap<>();
        this.currentMethodReturnType = null;
    }

    /**
     * Convert symbol table string errors to SemanticError objects
     */
    private void convertSymbolTableErrors(int line, int col) {
        for (String errorMsg : symbolTable.getErrors()) {
            errors.add(new SemanticError(
                SemanticError.SemanticErrorType.DUPLICATE_DECLARATION,
                errorMsg,
                line,
                col
            ));
        }
        symbolTable.clearErrors();
    }

    /**
     * Analyze a compilation unit
     *
     * @param ast The AST to analyze
     * @return Semantic analysis result with symbol table, types, and errors
     */
    public SemanticResult analyze(Ast.CompilationUnit ast) {
        errors.clear();
        expressionTypes.clear();

        // Pass 1: Collect declarations (classes, methods, global variables)
        collectDeclarations(ast);

        // Pass 2: Type checking and name resolution
        checkSemantics(ast);

        return new SemanticResult(ast, symbolTable, errors, expressionTypes);
    }

    // ==================== PASS 1: DECLARATION COLLECTION ====================

    /**
     * Collect all top-level declarations
     */
    private void collectDeclarations(Ast.CompilationUnit ast) {
        for (Ast.Statement stmt : ast.statements) {
            if (stmt instanceof Ast.ClassDeclaration) {
                collectClassDeclaration((Ast.ClassDeclaration) stmt);
            } else if (stmt instanceof Ast.MethodDeclaration) {
                collectMethodDeclaration((Ast.MethodDeclaration) stmt);
            } else if (stmt instanceof Ast.VariableDeclaration) {
                collectVariableDeclaration((Ast.VariableDeclaration) stmt);
            }
        }
    }

    /**
     * Collect a class declaration
     */
    private void collectClassDeclaration(Ast.ClassDeclaration classDecl) {
        // Register class type
        typeRegistry.registerClass(classDecl.name);

        // Define class symbol
        SigmaType classType = typeRegistry.resolve(classDecl.name);
        if (!symbolTable.define(classDecl.name, classType, Symbol.SymbolKind.CLASS,
                                classDecl.line, classDecl.col)) {
            // Duplicate class definition (error already added by symbolTable)
            convertSymbolTableErrors(classDecl.line, classDecl.col);
        }

        // Enter class scope and collect members
        symbolTable.enterScope(Scope.ScopeType.CLASS);

        for (Ast.Statement member : classDecl.members) {
            if (member instanceof Ast.MethodDeclaration) {
                collectMethodDeclaration((Ast.MethodDeclaration) member);
            } else if (member instanceof Ast.VariableDeclaration) {
                // Field declaration
                Ast.VariableDeclaration field = (Ast.VariableDeclaration) member;
                SigmaType fieldType = typeRegistry.resolve(field.typeName);
                if (!symbolTable.define(field.name, fieldType, Symbol.SymbolKind.FIELD,
                                       field.line, field.col)) {
                    convertSymbolTableErrors(field.line, field.col);
                }
            } else if (member instanceof Ast.FieldDeclaration) {
                // Explicit field declaration
                Ast.FieldDeclaration field = (Ast.FieldDeclaration) member;
                SigmaType fieldType = typeRegistry.resolve(field.typeName);
                if (!symbolTable.define(field.name, fieldType, Symbol.SymbolKind.FIELD,
                                       field.line, field.col)) {
                    convertSymbolTableErrors(field.line, field.col);
                }
            }
        }

        symbolTable.exitScope();
    }

    /**
     * Collect a method declaration
     */
    private void collectMethodDeclaration(Ast.MethodDeclaration methodDecl) {
        // Resolve return type
        SigmaType returnType = typeRegistry.resolve(methodDecl.returnType);

        // For now, store methods with return type only
        // TODO: Support method overloading by storing parameter types
        if (!symbolTable.define(methodDecl.name, returnType, Symbol.SymbolKind.METHOD,
                               methodDecl.line, methodDecl.col)) {
            convertSymbolTableErrors(methodDecl.line, methodDecl.col);
        }
    }

    /**
     * Collect a variable declaration
     */
    private void collectVariableDeclaration(Ast.VariableDeclaration varDecl) {
        SigmaType varType = typeRegistry.resolve(varDecl.typeName);
        if (!symbolTable.define(varDecl.name, varType, Symbol.SymbolKind.VARIABLE,
                               varDecl.line, varDecl.col)) {
            convertSymbolTableErrors(varDecl.line, varDecl.col);
        }
    }

    // ==================== PASS 2: TYPE CHECKING ====================

    /**
     * Perform type checking and name resolution
     */
    private void checkSemantics(Ast.CompilationUnit ast) {
        for (Ast.Statement stmt : ast.statements) {
            checkStatement(stmt);
        }
    }

    /**
     * Check a statement
     */
    private void checkStatement(Ast.Statement stmt) {
        if (stmt instanceof Ast.VariableDeclaration) {
            checkVariableDeclaration((Ast.VariableDeclaration) stmt);
        } else if (stmt instanceof Ast.ExpressionStatement) {
            checkExpressionStatement((Ast.ExpressionStatement) stmt);
        } else if (stmt instanceof Ast.Assignment) {
            checkAssignment((Ast.Assignment) stmt);
        } else if (stmt instanceof Ast.IfStatement) {
            checkIfStatement((Ast.IfStatement) stmt);
        } else if (stmt instanceof Ast.WhileStatement) {
            checkWhileStatement((Ast.WhileStatement) stmt);
        } else if (stmt instanceof Ast.ReturnStatement) {
            checkReturnStatement((Ast.ReturnStatement) stmt);
        } else if (stmt instanceof Ast.Block) {
            checkBlock((Ast.Block) stmt);
        } else if (stmt instanceof Ast.MethodDeclaration) {
            checkMethodDeclaration((Ast.MethodDeclaration) stmt);
        } else if (stmt instanceof Ast.ClassDeclaration) {
            checkClassDeclaration((Ast.ClassDeclaration) stmt);
        }
    }

    /**
     * Check variable declaration
     */
    private void checkVariableDeclaration(Ast.VariableDeclaration varDecl) {
        SigmaType declaredType = typeRegistry.resolve(varDecl.typeName);

        // Check if type exists
        if (declaredType == TypeRegistry.ERROR) {
            errors.add(new SemanticError(
                SemanticError.SemanticErrorType.UNDEFINED_CLASS,
                "Undefined type: " + varDecl.typeName,
                varDecl.line, varDecl.col
            ));
        }

        // Check initializer if present
        if (varDecl.init != null) {
            SigmaType initType = inferExpressionType(varDecl.init);

            // Check compatibility
            if (!initType.isCompatibleWith(declaredType)) {
                errors.add(new SemanticError(
                    SemanticError.SemanticErrorType.TYPE_MISMATCH,
                    String.format("Cannot assign %s to %s", initType, declaredType),
                    varDecl.line, varDecl.col
                ));
            }
        }
    }

    /**
     * Check expression statement
     */
    private void checkExpressionStatement(Ast.ExpressionStatement exprStmt) {
        inferExpressionType(exprStmt.expr);
    }

    /**
     * Check assignment
     */
    private void checkAssignment(Ast.Assignment assignment) {
        // Look up variable
        Symbol symbol = symbolTable.lookup(assignment.name);
        if (symbol == null) {
            errors.add(new SemanticError(
                SemanticError.SemanticErrorType.UNDEFINED_VARIABLE,
                "Undefined variable: " + assignment.name,
                0, 0 // Assignment doesn't have position info
            ));
            return;
        }

        // Infer value type
        SigmaType valueType = inferExpressionType(assignment.value);

        // Check compatibility
        if (!valueType.isCompatibleWith(symbol.getType())) {
            errors.add(new SemanticError(
                SemanticError.SemanticErrorType.TYPE_MISMATCH,
                String.format("Cannot assign %s to %s", valueType, symbol.getType()),
                0, 0
            ));
        }
    }

    /**
     * Check if statement
     */
    private void checkIfStatement(Ast.IfStatement ifStmt) {
        // Check condition
        SigmaType condType = inferExpressionType(ifStmt.cond);
        if (!condType.equals(TypeRegistry.BOOLEAN) && condType != TypeRegistry.ERROR) {
            errors.add(new SemanticError(
                SemanticError.SemanticErrorType.TYPE_MISMATCH,
                "If condition must be boolean, got: " + condType,
                0, 0
            ));
        }

        // Check branches
        symbolTable.enterScope(Scope.ScopeType.BLOCK);
        checkStatement(ifStmt.thenBranch);
        symbolTable.exitScope();

        if (ifStmt.elseBranch != null) {
            symbolTable.enterScope(Scope.ScopeType.BLOCK);
            checkStatement(ifStmt.elseBranch);
            symbolTable.exitScope();
        }
    }

    /**
     * Check while statement
     */
    private void checkWhileStatement(Ast.WhileStatement whileStmt) {
        // Check condition
        SigmaType condType = inferExpressionType(whileStmt.cond);
        if (!condType.equals(TypeRegistry.BOOLEAN) && condType != TypeRegistry.ERROR) {
            errors.add(new SemanticError(
                SemanticError.SemanticErrorType.TYPE_MISMATCH,
                "While condition must be boolean, got: " + condType,
                0, 0
            ));
        }

        // Check body
        symbolTable.enterScope(Scope.ScopeType.BLOCK);
        checkStatement(whileStmt.body);
        symbolTable.exitScope();
    }

    /**
     * Check return statement
     */
    private void checkReturnStatement(Ast.ReturnStatement returnStmt) {
        if (currentMethodReturnType == null) {
            // Return outside method (will be caught if we enforce method-only returns)
            return;
        }

        SigmaType returnType;
        if (returnStmt.expr == null) {
            returnType = TypeRegistry.VOID;
        } else {
            returnType = inferExpressionType(returnStmt.expr);
        }

        // Check compatibility with method return type
        if (!returnType.isCompatibleWith(currentMethodReturnType)) {
            errors.add(new SemanticError(
                SemanticError.SemanticErrorType.INVALID_RETURN_TYPE,
                String.format("Return type %s doesn't match method return type %s",
                             returnType, currentMethodReturnType),
                returnStmt.line, returnStmt.col
            ));
        }
    }

    /**
     * Check block
     */
    private void checkBlock(Ast.Block block) {
        for (Ast.Statement stmt : block.statements) {
            checkStatement(stmt);
        }
    }

    /**
     * Check method declaration
     */
    private void checkMethodDeclaration(Ast.MethodDeclaration methodDecl) {
        // Resolve return type
        SigmaType returnType = typeRegistry.resolve(methodDecl.returnType);
        if (returnType == TypeRegistry.ERROR) {
            errors.add(new SemanticError(
                SemanticError.SemanticErrorType.UNDEFINED_CLASS,
                "Undefined return type: " + methodDecl.returnType,
                methodDecl.line, methodDecl.col
            ));
        }

        // Enter method scope
        symbolTable.enterScope(Scope.ScopeType.METHOD);
        currentMethodReturnType = returnType;

        // Add parameters to scope
        for (Ast.Parameter param : methodDecl.parameters) {
            SigmaType paramType = typeRegistry.resolve(param.type);
            if (paramType == TypeRegistry.ERROR) {
                errors.add(new SemanticError(
                    SemanticError.SemanticErrorType.UNDEFINED_CLASS,
                    "Undefined parameter type: " + param.type,
                    param.line, param.col
                ));
            }
            if (!symbolTable.define(param.name, paramType, Symbol.SymbolKind.PARAMETER,
                                   param.line, param.col)) {
                convertSymbolTableErrors(param.line, param.col);
            }
        }

        // Check method body
        checkBlock(methodDecl.body);

        currentMethodReturnType = null;
        symbolTable.exitScope();
    }

    /**
     * Check class declaration
     */
    private void checkClassDeclaration(Ast.ClassDeclaration classDecl) {
        // Enter class scope
        symbolTable.enterScope(Scope.ScopeType.CLASS);

        // Re-add members to scope (already collected in pass 1)
        for (Ast.Statement member : classDecl.members) {
            if (member instanceof Ast.MethodDeclaration) {
                checkMethodDeclaration((Ast.MethodDeclaration) member);
            } else if (member instanceof Ast.VariableDeclaration) {
                checkVariableDeclaration((Ast.VariableDeclaration) member);
            } else if (member instanceof Ast.FieldDeclaration) {
                Ast.FieldDeclaration field = (Ast.FieldDeclaration) member;
                if (field.init != null) {
                    SigmaType fieldType = typeRegistry.resolve(field.typeName);
                    SigmaType initType = inferExpressionType(field.init);
                    if (!initType.isCompatibleWith(fieldType)) {
                        errors.add(new SemanticError(
                            SemanticError.SemanticErrorType.TYPE_MISMATCH,
                            String.format("Cannot assign %s to %s", initType, fieldType),
                            field.line, field.col
                        ));
                    }
                }
            }
        }

        symbolTable.exitScope();
    }

    // ==================== EXPRESSION TYPE INFERENCE ====================

    /**
     * Infer the type of an expression
     */
    private SigmaType inferExpressionType(Ast.Expression expr) {
        SigmaType type;

        if (expr instanceof Ast.IntLiteral) {
            type = TypeRegistry.INT;
        } else if (expr instanceof Ast.DoubleLiteral) {
            type = TypeRegistry.DOUBLE;
        } else if (expr instanceof Ast.StringLiteral) {
            type = TypeRegistry.STRING;
        } else if (expr instanceof Ast.BooleanLiteral) {
            type = TypeRegistry.BOOLEAN;
        } else if (expr instanceof Ast.NullLiteral) {
            type = TypeRegistry.NULL;
        } else if (expr instanceof Ast.Identifier) {
            type = inferIdentifierType((Ast.Identifier) expr);
        } else if (expr instanceof Ast.Binary) {
            type = inferBinaryType((Ast.Binary) expr);
        } else if (expr instanceof Ast.Unary) {
            type = inferUnaryType((Ast.Unary) expr);
        } else if (expr instanceof Ast.Call) {
            type = inferCallType((Ast.Call) expr);
        } else if (expr instanceof Ast.MemberAccess) {
            type = inferMemberAccessType((Ast.MemberAccess) expr);
        } else {
            type = TypeRegistry.ERROR;
        }

        // Store inferred type
        expressionTypes.put(expr, type);
        return type;
    }

    /**
     * Infer identifier type
     */
    private SigmaType inferIdentifierType(Ast.Identifier id) {
        Symbol symbol = symbolTable.lookup(id.name);
        if (symbol == null) {
            errors.add(new SemanticError(
                SemanticError.SemanticErrorType.UNDEFINED_VARIABLE,
                "Undefined variable: " + id.name,
                id.line, id.col
            ));
            return TypeRegistry.ERROR;
        }
        return symbol.getType();
    }

    /**
     * Infer binary expression type
     */
    private SigmaType inferBinaryType(Ast.Binary binary) {
        SigmaType leftType = inferExpressionType(binary.left);
        SigmaType rightType = inferExpressionType(binary.right);

        // Comparison operators return boolean
        if (binary.op.equals("<") || binary.op.equals("<=") ||
            binary.op.equals(">") || binary.op.equals(">=") ||
            binary.op.equals("==") || binary.op.equals("!=")) {
            return TypeRegistry.BOOLEAN;
        }

        // Logical operators
        if (binary.op.equals("&&") || binary.op.equals("||")) {
            if (!leftType.equals(TypeRegistry.BOOLEAN) || !rightType.equals(TypeRegistry.BOOLEAN)) {
                errors.add(new SemanticError(
                    SemanticError.SemanticErrorType.INVALID_BINARY_OP,
                    String.format("Logical operator requires boolean operands, got %s and %s",
                                 leftType, rightType),
                    binary.line, binary.col
                ));
                return TypeRegistry.ERROR;
            }
            return TypeRegistry.BOOLEAN;
        }

        // Arithmetic operators
        if (binary.op.equals("+") || binary.op.equals("-") ||
            binary.op.equals("*") || binary.op.equals("/") ||
            binary.op.equals("%") || binary.op.equals("**")) {

            // String concatenation
            if (binary.op.equals("+") &&
                (leftType.equals(TypeRegistry.STRING) || rightType.equals(TypeRegistry.STRING))) {
                return TypeRegistry.STRING;
            }

            // Numeric operations - use widest type
            if (leftType.isPrimitive() && rightType.isPrimitive()) {
                if (leftType.equals(TypeRegistry.DOUBLE) || rightType.equals(TypeRegistry.DOUBLE)) {
                    return TypeRegistry.DOUBLE;
                }
                if (leftType.equals(TypeRegistry.FLOAT) || rightType.equals(TypeRegistry.FLOAT)) {
                    return TypeRegistry.FLOAT;
                }
                return TypeRegistry.INT;
            }

            errors.add(new SemanticError(
                SemanticError.SemanticErrorType.INVALID_BINARY_OP,
                String.format("Invalid operand types for %s: %s and %s",
                             binary.op, leftType, rightType),
                binary.line, binary.col
            ));
            return TypeRegistry.ERROR;
        }

        return TypeRegistry.ERROR;
    }

    /**
     * Infer unary expression type
     */
    private SigmaType inferUnaryType(Ast.Unary unary) {
        SigmaType exprType = inferExpressionType(unary.expr);

        if (unary.op.equals("!")) {
            if (!exprType.equals(TypeRegistry.BOOLEAN)) {
                errors.add(new SemanticError(
                    SemanticError.SemanticErrorType.INVALID_UNARY_OP,
                    "Logical NOT requires boolean operand, got: " + exprType,
                    unary.line, unary.col
                ));
                return TypeRegistry.ERROR;
            }
            return TypeRegistry.BOOLEAN;
        }

        if (unary.op.equals("-") || unary.op.equals("neg")) {
            if (!exprType.isPrimitive() || exprType.equals(TypeRegistry.BOOLEAN)) {
                errors.add(new SemanticError(
                    SemanticError.SemanticErrorType.INVALID_UNARY_OP,
                    "Unary minus requires numeric operand, got: " + exprType,
                    unary.line, unary.col
                ));
                return TypeRegistry.ERROR;
            }
            return exprType;
        }

        return TypeRegistry.ERROR;
    }

    /**
     * Infer call expression type
     */
    private SigmaType inferCallType(Ast.Call call) {
        // For now, just look up method/function name
        if (call.target instanceof Ast.Identifier) {
            Ast.Identifier id = (Ast.Identifier) call.target;
            Symbol symbol = symbolTable.lookup(id.name);
            if (symbol == null) {
                errors.add(new SemanticError(
                    SemanticError.SemanticErrorType.UNDEFINED_METHOD,
                    "Undefined method: " + id.name,
                    id.line, id.col
                ));
                return TypeRegistry.ERROR;
            }

            if (!symbol.isMethod()) {
                errors.add(new SemanticError(
                    SemanticError.SemanticErrorType.INVALID_CALL,
                    id.name + " is not a method",
                    id.line, id.col
                ));
                return TypeRegistry.ERROR;
            }

            // Return the method's return type
            return symbol.getType();
        } else if (call.target instanceof Ast.MemberAccess) {
            // Method call on object
            inferExpressionType(call.target);
            // For now, return ERROR (would need class member lookup)
            return TypeRegistry.ERROR;
        }

        return TypeRegistry.ERROR;
    }

    /**
     * Infer member access type
     */
    private SigmaType inferMemberAccessType(Ast.MemberAccess memberAccess) {
        SigmaType objectType = inferExpressionType(memberAccess.object);

        // For now, just return ERROR
        // TODO: Implement class member lookup
        if (!objectType.isReference()) {
            errors.add(new SemanticError(
                SemanticError.SemanticErrorType.INVALID_MEMBER_ACCESS,
                "Cannot access member on non-class type: " + objectType,
                memberAccess.line, memberAccess.col
            ));
        }

        return TypeRegistry.ERROR;
    }
}
