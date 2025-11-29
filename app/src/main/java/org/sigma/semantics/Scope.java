package org.sigma.semantics;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a lexical scope in the Sigma language.
 * Scopes form a hierarchy with parent-child relationships for nested scopes.
 */
public class Scope {
    private final Scope parent;
    private final Map<String, Symbol> symbols;
    private final ScopeType type;

    /**
     * Types of scopes in the language
     */
    public enum ScopeType {
        GLOBAL,    // Top-level scope
        CLASS,     // Inside a class declaration
        METHOD,    // Inside a method/function
        BLOCK      // Inside a block (if, while, etc.)
    }

    /**
     * Create a new scope with a parent scope
     */
    public Scope(Scope parent, ScopeType type) {
        this.parent = parent;
        this.type = type;
        this.symbols = new HashMap<>();
    }

    /**
     * Create a global scope (no parent)
     */
    public Scope(ScopeType type) {
        this(null, type);
    }

    /**
     * Look up a symbol in this scope or parent scopes.
     * Searches up the parent chain until found or global scope is reached.
     *
     * @param name The name of the symbol to look up
     * @return The symbol if found, null otherwise
     */
    public Symbol lookup(String name) {
        // Check this scope first
        Symbol symbol = symbols.get(name);
        if (symbol != null) {
            return symbol;
        }

        // If not found and we have a parent, search parent
        if (parent != null) {
            return parent.lookup(name);
        }

        // Not found anywhere
        return null;
    }

    /**
     * Look up a symbol only in this scope (not parents).
     * Used for checking duplicate declarations.
     *
     * @param name The name of the symbol to look up
     * @return The symbol if found in this scope, null otherwise
     */
    public Symbol lookupLocal(String name) {
        return symbols.get(name);
    }

    /**
     * Define a new symbol in this scope.
     * Does not check for duplicates - use lookupLocal first to check.
     *
     * @param symbol The symbol to define
     */
    public void define(Symbol symbol) {
        symbols.put(symbol.getName(), symbol);
    }

    /**
     * Check if a symbol is defined in this scope (not parents)
     *
     * @param name The name to check
     * @return true if defined in this scope, false otherwise
     */
    public boolean isDefinedLocal(String name) {
        return symbols.containsKey(name);
    }

    /**
     * Check if a symbol is defined in this scope or any parent scope
     *
     * @param name The name to check
     * @return true if defined anywhere in scope chain, false otherwise
     */
    public boolean isDefined(String name) {
        return lookup(name) != null;
    }

    /**
     * Get the parent scope
     */
    public Scope getParent() {
        return parent;
    }

    /**
     * Get the scope type
     */
    public ScopeType getType() {
        return type;
    }

    /**
     * Get all symbols defined in this scope (not parents)
     */
    public Map<String, Symbol> getSymbols() {
        return new HashMap<>(symbols);
    }

    /**
     * Get the count of symbols in this scope
     */
    public int getSymbolCount() {
        return symbols.size();
    }

    @Override
    public String toString() {
        return "Scope{type=" + type + ", symbols=" + symbols.size() + ", hasParent=" + (parent != null) + "}";
    }
}
