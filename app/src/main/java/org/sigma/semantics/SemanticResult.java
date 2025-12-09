package org.sigma.semantics;

import org.sigma.parser.Ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of semantic analysis.
 * Contains the analyzed AST, symbol table, type information, and any errors.
 */
public class SemanticResult {
    private final Ast.CompilationUnit ast;
    private final SymbolTable symbolTable;
    private final List<SemanticError> errors;
    private final Map<Ast.Expression, SigmaType> expressionTypes;
    private final Map<Ast.Expression, Symbol> resolvedSymbols;
    private final Map<String, ClassInfo> classInfos;
    private final Map<Ast.Expression, SigmaType> expressionCoercions;

    /**
     * Create a semantic analysis result
     *
     * @param ast The analyzed AST
     * @param symbolTable The populated symbol table
     * @param errors List of semantic errors
     * @param expressionTypes Map of expressions to their inferred types
     */
    public SemanticResult(Ast.CompilationUnit ast,
                         SymbolTable symbolTable,
                         List<SemanticError> errors,
                         Map<Ast.Expression, SigmaType> expressionTypes,
                         Map<Ast.Expression, Symbol> resolvedSymbols,
                         Map<String, ClassInfo> classInfos,
                         Map<Ast.Expression, SigmaType> expressionCoercions) {
        this.ast = ast;
        this.symbolTable = symbolTable;
        this.errors = new ArrayList<>(errors);
        this.expressionTypes = new HashMap<>(expressionTypes);
        this.resolvedSymbols = new HashMap<>(resolvedSymbols);
        this.classInfos = new HashMap<>(classInfos);
        this.expressionCoercions = new HashMap<>(expressionCoercions);
    }

    /**
     * Check if semantic analysis was successful (no errors)
     */
    public boolean isSuccessful() {
        return errors.isEmpty();
    }

    /**
     * Get the AST
     */
    public Ast.CompilationUnit getAst() {
        return ast;
    }

    /**
     * Get the symbol table
     */
    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    /**
     * Get all semantic errors
     */
    public List<SemanticError> getErrors() {
        return new ArrayList<>(errors);
    }

    /**
     * Get error count
     */
    public int getErrorCount() {
        return errors.size();
    }

    /**
     * Get the type of an expression
     */
    public SigmaType getExpressionType(Ast.Expression expr) {
        return expressionTypes.get(expr);
    }

    /**
     * Get all expression types
     */
    public Map<Ast.Expression, SigmaType> getExpressionTypes() {
        return new HashMap<>(expressionTypes);
    }

    /**
     * Get coercions inserted during semantic analysis for expressions that need
     * conversion to satisfy type rules.
     */
    public Map<Ast.Expression, SigmaType> getExpressionCoercions() {
        return new HashMap<>(expressionCoercions);
    }

    /**
     * Get resolved symbols for identifier/member expressions.
     */
    public Map<Ast.Expression, Symbol> getResolvedSymbols() {
        return new HashMap<>(resolvedSymbols);
    }

    /**
     * Get class metadata captured during semantic analysis.
     */
    public Map<String, ClassInfo> getClassInfos() {
        return new HashMap<>(classInfos);
    }

    /**
     * Get formatted error messages
     */
    public List<String> getErrorMessages() {
        List<String> messages = new ArrayList<>();
        for (SemanticError error : errors) {
            messages.add(error.format());
        }
        return messages;
    }

    /**
     * Get errors as a formatted string
     */
    public String getErrorsAsString() {
        if (errors.isEmpty()) {
            return "No semantic errors";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Semantic Errors (").append(errors.size()).append("):\n");
        for (SemanticError error : errors) {
            sb.append("  ").append(error.format()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Get a visualization of the semantic analysis results
     */
    public String visualize() {
        StringBuilder sb = new StringBuilder();

        sb.append("=".repeat(70)).append("\n");
        sb.append("SEMANTIC ANALYSIS RESULTS\n");
        sb.append("=".repeat(70)).append("\n\n");

        // Status
        sb.append("Status: ");
        if (isSuccessful()) {
            sb.append("✓ SUCCESS (No errors)\n");
        } else {
            sb.append("✗ FAILED (").append(errors.size()).append(" errors)\n");
        }
        sb.append("\n");

        // Errors
        if (!errors.isEmpty()) {
            sb.append("-".repeat(70)).append("\n");
            sb.append("ERRORS:\n");
            sb.append("-".repeat(70)).append("\n");
            for (SemanticError error : errors) {
                sb.append(error.format()).append("\n");
            }
            sb.append("\n");
        }

        // Symbol Table Summary
        sb.append("-".repeat(70)).append("\n");
        sb.append("SYMBOL TABLE SUMMARY:\n");
        sb.append("-".repeat(70)).append("\n");
        sb.append(visualizeSymbolTable());
        sb.append("\n");

        // Type Information Summary
        sb.append("-".repeat(70)).append("\n");
        sb.append("TYPE INFORMATION:\n");
        sb.append("-".repeat(70)).append("\n");
        sb.append("Total expressions with inferred types: ").append(expressionTypes.size()).append("\n");

        // Group by type
        Map<String, Integer> typeCount = new HashMap<>();
        for (SigmaType type : expressionTypes.values()) {
            String typeName = type.getName();
            typeCount.put(typeName, typeCount.getOrDefault(typeName, 0) + 1);
        }

        if (!typeCount.isEmpty()) {
            sb.append("\nType distribution:\n");
            for (Map.Entry<String, Integer> entry : typeCount.entrySet()) {
                sb.append("  ").append(entry.getKey())
                  .append(": ").append(entry.getValue())
                  .append(" expression(s)\n");
            }
        }

        sb.append("\n").append("=".repeat(70)).append("\n");

        return sb.toString();
    }

    /**
     * Visualize the symbol table structure
     */
    private String visualizeSymbolTable() {
        StringBuilder sb = new StringBuilder();
        Scope current = symbolTable.getCurrentScope();

        // Walk up to global scope
        List<Scope> scopes = new ArrayList<>();
        Scope s = current;
        while (s != null) {
            scopes.add(0, s); // Add to beginning
            s = s.getParent();
        }

        // Display each scope
        for (int i = 0; i < scopes.size(); i++) {
            Scope scope = scopes.get(i);
            String indent = "  ".repeat(i);

            sb.append(indent).append("Scope[").append(scope.getType()).append("]");

            Map<String, Symbol> symbols = scope.getSymbols();
            if (symbols.isEmpty()) {
                sb.append(" (empty)\n");
            } else {
                sb.append(" (").append(symbols.size()).append(" symbols):\n");
                for (Symbol symbol : symbols.values()) {
                    sb.append(indent).append("  - ")
                      .append(symbol.getName())
                      .append(": ")
                      .append(symbol.getType())
                      .append(" (")
                      .append(symbol.getKind())
                      .append(")\n");
                }
            }
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return "SemanticResult{" +
                "successful=" + isSuccessful() +
                ", errors=" + errors.size() +
                ", expressionTypes=" + expressionTypes.size() +
                '}';
    }
}
