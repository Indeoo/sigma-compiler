package org.sigma.syntax.semantic;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a scope in the symbol table
 */
public class Scope {

    private Map<String, Symbol> symbols;
    private Scope parent;
    private String scopeName;

    public Scope(String scopeName) {
        this.scopeName = scopeName;
        this.symbols = new HashMap<>();
        this.parent = null;
    }

    public Scope(String scopeName, Scope parent) {
        this.scopeName = scopeName;
        this.symbols = new HashMap<>();
        this.parent = parent;
    }

    /**
     * Define a symbol in this scope
     */
    public void define(Symbol symbol) {
        symbols.put(symbol.getName(), symbol);
    }

    /**
     * Look up a symbol starting from this scope and moving up the parent chain
     */
    public Symbol lookup(String name) {
        Symbol symbol = symbols.get(name);
        if (symbol != null) {
            return symbol;
        }

        // Look in parent scope
        if (parent != null) {
            return parent.lookup(name);
        }

        return null;
    }

    /**
     * Check if a symbol is defined in this scope only (not parent scopes)
     */
    public boolean isDefinedInThisScope(String name) {
        return symbols.containsKey(name);
    }

    /**
     * Get all symbols in this scope
     */
    public Map<String, Symbol> getSymbols() {
        return new HashMap<>(symbols);
    }

    public Scope getParent() {
        return parent;
    }

    public String getScopeName() {
        return scopeName;
    }

    @Override
    public String toString() {
        return String.format("Scope{name='%s', symbols=%d, hasParent=%s}",
                           scopeName, symbols.size(), parent != null);
    }
}