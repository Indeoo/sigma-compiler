grammar BasicGroovy;

// Parser rules
compilationUnit
    : (declaration | statement)* EOF
    ;

declaration
    : variableDeclaration
    | methodDeclaration
    | classDeclaration
    ;

variableDeclaration
    : type IDENTIFIER ('=' expression)? ';'?
    ;

methodDeclaration
    : type IDENTIFIER '(' parameterList? ')' block
    ;

classDeclaration
    : 'class' IDENTIFIER block
    ;

parameterList
    : parameter (',' parameter)*
    ;

parameter
    : type IDENTIFIER
    ;

statement
    : assignmentStatement
    | expressionStatement
    | ifStatement
    | whileStatement
    | returnStatement
    | block
    ;

assignmentStatement
    : IDENTIFIER '=' expression ';'?
    ;

expressionStatement
    : expression ';'?
    ;

ifStatement
    : 'if' '(' expression ')' statement ('else' statement)?
    ;

whileStatement
    : 'while' '(' expression ')' statement
    ;

returnStatement
    : 'return' expression? ';'?
    ;

block
    : '{' statement* '}'
    ;

expression
    : primary
    | expression '.' IDENTIFIER                    // member access
    | expression '(' argumentList? ')'             // method call
    | expression ('*' | '/' | '%') expression      // multiplicative
    | expression ('+' | '-') expression            // additive
    | expression ('<' | '<=' | '>' | '>=' | '==' | '!=') expression  // relational
    | expression ('&&' | '||') expression          // logical
    | '(' expression ')'                           // parentheses
    | '!' expression                               // logical not
    | '-' expression                               // unary minus
    ;

argumentList
    : expression (',' expression)*
    ;

primary
    : IDENTIFIER
    | literal
    ;

literal
    : INTEGER
    | FLOAT
    | STRING
    | BOOLEAN
    | 'null'
    ;

type
    : IDENTIFIER
    | 'int'
    | 'double'
    | 'String'
    | 'boolean'
    | 'void'
    ;

// Lexer rules
IDENTIFIER : [a-zA-Z_][a-zA-Z_0-9]* ;

INTEGER : [0-9]+ ;

FLOAT : [0-9]+ '.' [0-9]+ ;

STRING : '"' ( ~["\\\r\n] | '\\' . )* '"' ;

BOOLEAN : 'true' | 'false' ;

// Skip whitespace and comments
WS : [ \t\r\n]+ -> skip ;

LINE_COMMENT : '//' ~[\r\n]* -> skip ;

BLOCK_COMMENT : '/*' .*? '*/' -> skip ;