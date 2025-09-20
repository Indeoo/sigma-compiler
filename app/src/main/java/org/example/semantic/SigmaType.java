package org.example.semantic;

/**
 * Represents types in the Sigma language
 */
public enum SigmaType {
    INT("int"),
    DOUBLE("double"),
    STRING("String"),
    BOOLEAN("boolean"),
    VOID("void"),
    UNKNOWN("unknown"),
    NULL("null");

    private final String typeName;

    SigmaType(String typeName) {
        this.typeName = typeName;
    }

    public String getTypeName() {
        return typeName;
    }

    /**
     * Convert string representation to SigmaType
     */
    public static SigmaType fromString(String typeStr) {
        for (SigmaType type : values()) {
            if (type.typeName.equals(typeStr)) {
                return type;
            }
        }
        return UNKNOWN;
    }

    /**
     * Check if this type is compatible with another type for assignment
     */
    public boolean isCompatibleWith(SigmaType other) {
        if (this == other) return true;
        if (this == NULL) return other != INT && other != DOUBLE && other != BOOLEAN;
        if (other == NULL) return this != INT && this != DOUBLE && this != BOOLEAN;

        // Allow implicit conversions
        if (this == INT && other == DOUBLE) return true;
        if (this == DOUBLE && other == INT) return true;

        return false;
    }

    /**
     * Get the result type of a binary operation
     */
    public static SigmaType getBinaryOperationResultType(SigmaType left, SigmaType right, String operator) {
        // Arithmetic operations
        if (operator.matches("[+\\-*/%]")) {
            if (left == STRING || right == STRING) {
                if (operator.equals("+")) return STRING; // String concatenation
                return UNKNOWN; // Invalid operation
            }
            if (left == DOUBLE || right == DOUBLE) return DOUBLE;
            if (left == INT && right == INT) return INT;
            return UNKNOWN;
        }

        // Comparison operations
        if (operator.matches("(<|<=|>|>=|==|!=)")) {
            if (left.isCompatibleWith(right) || right.isCompatibleWith(left)) {
                return BOOLEAN;
            }
            return UNKNOWN;
        }

        // Logical operations
        if (operator.matches("(&&|\\|\\|)")) {
            if (left == BOOLEAN && right == BOOLEAN) return BOOLEAN;
            return UNKNOWN;
        }

        return UNKNOWN;
    }

    @Override
    public String toString() {
        return typeName;
    }
}