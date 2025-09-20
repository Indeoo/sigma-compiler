package org.example.semantic;

import java.util.Stack;

/**
 * Symbol table implementation with nested scopes
 */
public class SymbolTable {

    private Stack<Scope> scopes;
    private Scope globalScope;

    public SymbolTable() {
        this.scopes = new Stack<>();
        this.globalScope = new Scope("global");
        this.scopes.push(globalScope);

        // Add built-in functions and types
        initializeBuiltins();
    }

    /**
     * Initialize built-in functions and symbols
     */
    private void initializeBuiltins() {
        // Built-in println function
        Symbol printlnSymbol = new Symbol("println", SigmaType.VOID, Symbol.SymbolType.METHOD);
        globalScope.define(printlnSymbol);

        // Built-in print function
        Symbol printSymbol = new Symbol("print", SigmaType.VOID, Symbol.SymbolType.METHOD);
        globalScope.define(printSymbol);
    }

    /**
     * Enter a new scope
     */
    public void enterScope(String scopeName) {
        Scope newScope = new Scope(scopeName, getCurrentScope());
        scopes.push(newScope);
    }

    /**
     * Exit the current scope
     */
    public void exitScope() {
        if (scopes.size() > 1) { // Don't remove global scope
            scopes.pop();
        }
    }

    /**
     * Get the current scope
     */
    public Scope getCurrentScope() {
        return scopes.peek();
    }

    /**
     * Get the global scope
     */
    public Scope getGlobalScope() {
        return globalScope;
    }

    /**
     * Define a symbol in the current scope
     */
    public void define(Symbol symbol) {
        getCurrentScope().define(symbol);
    }

    /**
     * Look up a symbol starting from current scope
     */
    public Symbol lookup(String name) {
        return getCurrentScope().lookup(name);
    }

    /**
     * Check if a symbol is defined in the current scope (not parent scopes)
     */
    public boolean isDefinedInCurrentScope(String name) {
        return getCurrentScope().isDefinedInThisScope(name);
    }

    /**
     * Get the current scope depth
     */
    public int getScopeDepth() {
        return scopes.size();
    }

    /**
     * Check if we're in global scope
     */
    public boolean isInGlobalScope() {
        return scopes.size() == 1;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SymbolTable{\n");
        for (int i = 0; i < scopes.size(); i++) {
            sb.append("  ").append("  ".repeat(i)).append(scopes.get(i)).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }
}