package org.sigma.semantics;

/**
 * Represents a symbol in the symbol table.
 * A symbol is a named entity (variable, parameter, method, class, field) with a type.
 */
public class Symbol {
    private final String name;
    private final SigmaType type;
    private final SymbolKind kind;
    private final int definitionLine;
    private final int definitionCol;

    /**
     * Kinds of symbols that can be defined
     */
    public enum SymbolKind {
        VARIABLE,   // Local variable
        PARAMETER,  // Method/function parameter
        METHOD,     // Method/function
        CLASS,      // Class declaration
        FIELD       // Class field/member
    }

    /**
     * Create a new symbol
     *
     * @param name The name of the symbol
     * @param type The type of the symbol
     * @param kind The kind of symbol (variable, method, etc.)
     * @param definitionLine The line where this symbol was defined
     * @param definitionCol The column where this symbol was defined
     */
    public Symbol(String name, SigmaType type, SymbolKind kind, int definitionLine, int definitionCol) {
        this.name = name;
        this.type = type;
        this.kind = kind;
        this.definitionLine = definitionLine;
        this.definitionCol = definitionCol;
    }

    /**
     * Create a symbol without position information
     */
    public Symbol(String name, SigmaType type, SymbolKind kind) {
        this(name, type, kind, 0, 0);
    }

    /**
     * Get the symbol name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the symbol type
     */
    public SigmaType getType() {
        return type;
    }

    /**
     * Get the symbol kind
     */
    public SymbolKind getKind() {
        return kind;
    }

    /**
     * Get the line where this symbol was defined
     */
    public int getDefinitionLine() {
        return definitionLine;
    }

    /**
     * Get the column where this symbol was defined
     */
    public int getDefinitionCol() {
        return definitionCol;
    }

    /**
     * Check if this is a variable symbol
     */
    public boolean isVariable() {
        return kind == SymbolKind.VARIABLE;
    }

    /**
     * Check if this is a parameter symbol
     */
    public boolean isParameter() {
        return kind == SymbolKind.PARAMETER;
    }

    /**
     * Check if this is a method symbol
     */
    public boolean isMethod() {
        return kind == SymbolKind.METHOD;
    }

    /**
     * Check if this is a class symbol
     */
    public boolean isClass() {
        return kind == SymbolKind.CLASS;
    }

    /**
     * Check if this is a field symbol
     */
    public boolean isField() {
        return kind == SymbolKind.FIELD;
    }

    @Override
    public String toString() {
        return "Symbol{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", kind=" + kind +
                ", line=" + definitionLine +
                ", col=" + definitionCol +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Symbol)) return false;
        Symbol other = (Symbol) obj;
        return this.name.equals(other.name) &&
               this.type.equals(other.type) &&
               this.kind == other.kind;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + kind.hashCode();
        return result;
    }
}
