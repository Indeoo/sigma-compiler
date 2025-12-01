package org.sigma.semantics;

/**
 * Represents a semantic error found during analysis.
 * Contains error type, message, and location information.
 */
public class SemanticError {
    private final SemanticErrorType type;
    private final String message;
    private final int line;
    private final int col;

    /**
     * Types of semantic errors
     */
    public enum SemanticErrorType {
        UNDEFINED_VARIABLE,      // Variable used but not declared
        UNDEFINED_METHOD,        // Method called but not declared
        UNDEFINED_CLASS,         // Class referenced but not declared
        UNDEFINED_FIELD,         // Field accessed but not declared
        TYPE_MISMATCH,           // Type incompatibility (assignment, operation, etc.)
        DUPLICATE_DECLARATION,   // Symbol declared multiple times in same scope
        INVALID_RETURN_TYPE,     // Return type doesn't match method signature
        UNINITIALIZED_VARIABLE,  // Variable used before initialization
        INVALID_BINARY_OP,       // Invalid types for binary operator
        INVALID_UNARY_OP,        // Invalid types for unary operator
        INVALID_CALL,            // Invalid function/method call
        INVALID_MEMBER_ACCESS,   // Invalid member access on non-class type
        INVALID_CONSTRUCTOR_CALL,// Invalid constructor call
        VOID_EXPRESSION,         // Void used in expression context
        CONSTANT_REASSIGNMENT,   // Attempt to reassign a constant
        CONSTANT_WITHOUT_INITIALIZER // Constant declaration missing initializer
    }

    /**
     * Create a semantic error
     *
     * @param type The type of error
     * @param message The error message
     * @param line The line where the error occurred
     * @param col The column where the error occurred
     */
    public SemanticError(SemanticErrorType type, String message, int line, int col) {
        this.type = type;
        this.message = message;
        this.line = line;
        this.col = col;
    }

    /**
     * Get the error type
     */
    public SemanticErrorType getType() {
        return type;
    }

    /**
     * Get the error message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Get the line number
     */
    public int getLine() {
        return line;
    }

    /**
     * Get the column number
     */
    public int getCol() {
        return col;
    }

    /**
     * Format error as a string with location
     */
    public String format() {
        return String.format("Line %d:%d: [%s] %s", line, col, type, message);
    }

    @Override
    public String toString() {
        return format();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SemanticError)) return false;
        SemanticError other = (SemanticError) obj;
        return this.type == other.type &&
               this.message.equals(other.message) &&
               this.line == other.line &&
               this.col == other.col;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + message.hashCode();
        result = 31 * result + line;
        result = 31 * result + col;
        return result;
    }
}
