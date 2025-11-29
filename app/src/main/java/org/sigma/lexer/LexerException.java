package org.sigma.lexer;

/**
 * Exception thrown when the lexer encounters an error during tokenization.
 * Includes position information (line and column) to help with error reporting.
 */
public class LexerException extends RuntimeException {
    private final int line;
    private final int column;

    /**
     * Creates a new lexer exception with a message and position information.
     *
     * @param message the error message
     * @param line    the 1-indexed line number where the error occurred
     * @param column  the 0-indexed column position where the error occurred
     */
    public LexerException(String message, int line, int column) {
        super(String.format("Lexer error at line %d, column %d: %s", line, column, message));
        this.line = line;
        this.column = column;
    }

    /**
     * Returns the 1-indexed line number where the error occurred.
     */
    public int getLine() {
        return line;
    }

    /**
     * Returns the 0-indexed column position where the error occurred.
     */
    public int getColumn() {
        return column;
    }
}
