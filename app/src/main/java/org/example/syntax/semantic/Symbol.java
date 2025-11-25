package org.example.syntax.semantic;

/**
 * Represents a symbol in the symbol table (variable, method, class, etc.)
 */
public class Symbol {

    public enum SymbolType {
        VARIABLE, METHOD, CLASS, PARAMETER
    }

    private String name;
    private SigmaType type;
    private SymbolType symbolType;
    private Object value;
    private boolean initialized;

    public Symbol(String name, SigmaType type, SymbolType symbolType) {
        this.name = name;
        this.type = type;
        this.symbolType = symbolType;
        this.initialized = false;
    }

    public Symbol(String name, SigmaType type, SymbolType symbolType, Object value) {
        this(name, type, symbolType);
        this.value = value;
        this.initialized = true;
    }

    // Getters and setters
    public String getName() { return name; }
    public SigmaType getType() { return type; }
    public SymbolType getSymbolType() { return symbolType; }
    public Object getValue() { return value; }
    public boolean isInitialized() { return initialized; }

    public void setValue(Object value) {
        this.value = value;
        this.initialized = true;
    }

    public void setType(SigmaType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return String.format("Symbol{name='%s', type=%s, symbolType=%s, initialized=%s}",
                           name, type, symbolType, initialized);
    }
}