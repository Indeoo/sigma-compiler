package org.sigma.lexer;

/**
 * Token types for the Sigma lexer, derived from the gpt.jff finite automaton.
 * Each token type corresponds to a final state in the automaton or a keyword
 * distinguished during identifier recognition.
 */
public enum TokenType {
    // Keywords (distinguished from IDENTIFIER at lexer level)
    CLASS,      // "class"
    IF,         // "if"
    ELSE,       // "else"
    WHILE,      // "while"
    RETURN,     // "return"
    FINAL,      // "final"
    NULL,       // "null"
    NEW,        // "new"

    // Primitive type keywords
    INT,        // "int"
    DOUBLE,     // "double"
    FLOAT,      // "float" (as keyword)
    BOOLEAN,    // "boolean"

    // Other type keywords
    STRING_TYPE,  // "String"
    VOID,         // "void"

    // Boolean literals
    TRUE,       // "true"
    FALSE,      // "false"

    // Identifiers and numeric literals (from gpt.jff final states)
    IDENTIFIER,   // qID_END: letter/_ followed by letter/digit/_
    INTEGER,      // qINT_END: sequence of digits
    FLOAT_LITERAL,   // qFLOAT_END: digits.digits
    STRING,       // qSTR_END: "..." with escape sequences

    // Arithmetic operators
    PLUS,       // +
    MINUS,      // -
    MULT,       // *
    DIV,        // /
    MOD,        // %
    POWER,      // **

    // Relational operators
    LT,         // <
    GT,         // >
    LE,         // <=
    GE,         // >=
    EQ,         // ==
    NE,         // !=

    // Logical operators
    AND,        // &&
    OR,         // ||
    NOT,        // !

    // Assignment
    ASSIGN,     // =

    // Bitwise operators (single & or |, parser will handle as error)
    AMPERSAND,  // &
    PIPE,       // |

    // Delimiters
    LPAREN,     // (
    RPAREN,     // )
    LBRACE,     // {
    RBRACE,     // }
    SEMI,       // ;
    COMMA,      // ,
    DOT,        // .

    // Special
    EOF         // End of file
}
