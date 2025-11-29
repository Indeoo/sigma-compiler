package org.sigma.semantics;

import java.util.ArrayList;
import java.util.List;

/**
 * Symbol table for managing scopes and symbol definitions.
 * Tracks the current scope and provides operations to enter/exit scopes
 * and define/lookup symbols.
 */
public class SymbolTable {
    private Scope currentScope;
    private final List<String> errors;

    /**
     * Create a new symbol table with a global scope
     */
    public SymbolTable() {
        this.currentScope = new Scope(Scope.ScopeType.GLOBAL);
        this.errors = new ArrayList<>();
    }

    /**
     * Enter a new scope (push a new scope onto the stack)
     *
     * @param type The type of scope to enter
     */
    public void enterScope(Scope.ScopeType type) {
        currentScope = new Scope(currentScope, type);
    }

    /**
     * Exit the current scope (pop scope from stack)
     * Cannot exit global scope.
     *
     * @throws IllegalStateException if trying to exit global scope
     */
    public void exitScope() {
        if (currentScope.getParent() == null) {
            throw new IllegalStateException("Cannot exit global scope");
        }
        currentScope = currentScope.getParent();
    }

    /**
     * Define a new symbol in the current scope.
     * Checks for duplicate declarations in the current scope.
     *
     * @param name The name of the symbol
     * @param type The type of the symbol
     * @param kind The kind of symbol
     * @param line The line where defined
     * @param col The column where defined
     * @return true if defined successfully, false if duplicate
     */
    public boolean define(String name, SigmaType type, Symbol.SymbolKind kind, int line, int col) {
        // Check for duplicate in current scope
        if (currentScope.isDefinedLocal(name)) {
            Symbol existing = currentScope.lookupLocal(name);
            errors.add(String.format("Line %d:%d: Symbol '%s' is already defined at line %d:%d",
                    line, col, name, existing.getDefinitionLine(), existing.getDefinitionCol()));
            return false;
        }

        // Define the symbol
        Symbol symbol = new Symbol(name, type, kind, line, col);
        currentScope.define(symbol);
        return true;
    }

    /**
     * Define a symbol without position information
     */
    public boolean define(String name, SigmaType type, Symbol.SymbolKind kind) {
        return define(name, type, kind, 0, 0);
    }

    /**
     * Look up a symbol in the current scope or parent scopes
     *
     * @param name The name to look up
     * @return The symbol if found, null otherwise
     */
    public Symbol lookup(String name) {
        return currentScope.lookup(name);
    }

    /**
     * Look up a symbol only in the current scope
     *
     * @param name The name to look up
     * @return The symbol if found in current scope, null otherwise
     */
    public Symbol lookupLocal(String name) {
        return currentScope.lookupLocal(name);
    }

    /**
     * Check if a symbol is defined in current or parent scopes
     *
     * @param name The name to check
     * @return true if defined, false otherwise
     */
    public boolean isDefined(String name) {
        return currentScope.isDefined(name);
    }

    /**
     * Check if a symbol is defined in the current scope only
     *
     * @param name The name to check
     * @return true if defined locally, false otherwise
     */
    public boolean isDefinedLocal(String name) {
        return currentScope.isDefinedLocal(name);
    }

    /**
     * Get the current scope
     */
    public Scope getCurrentScope() {
        return currentScope;
    }

    /**
     * Get the current scope type
     */
    public Scope.ScopeType getCurrentScopeType() {
        return currentScope.getType();
    }

    /**
     * Get all errors that occurred during symbol table operations
     */
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    /**
     * Check if there are any errors
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Clear all errors
     */
    public void clearErrors() {
        errors.clear();
    }

    /**
     * Get the depth of the current scope (0 = global, 1 = first nested, etc.)
     */
    public int getScopeDepth() {
        int depth = 0;
        Scope scope = currentScope;
        while (scope.getParent() != null) {
            depth++;
            scope = scope.getParent();
        }
        return depth;
    }

    /**
     * Check if we're currently in the global scope
     */
    public boolean isGlobalScope() {
        return currentScope.getType() == Scope.ScopeType.GLOBAL &&
               currentScope.getParent() == null;
    }

    /**
     * Check if we're currently in a method scope
     */
    public boolean isInMethodScope() {
        Scope scope = currentScope;
        while (scope != null) {
            if (scope.getType() == Scope.ScopeType.METHOD) {
                return true;
            }
            scope = scope.getParent();
        }
        return false;
    }

    /**
     * Check if we're currently in a class scope
     */
    public boolean isInClassScope() {
        Scope scope = currentScope;
        while (scope != null) {
            if (scope.getType() == Scope.ScopeType.CLASS) {
                return true;
            }
            scope = scope.getParent();
        }
        return false;
    }

    @Override
    public String toString() {
        return "SymbolTable{" +
                "currentScope=" + currentScope +
                ", depth=" + getScopeDepth() +
                ", errors=" + errors.size() +
                '}';
    }
}
