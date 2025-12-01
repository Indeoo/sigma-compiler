// Put generated parser/lexer classes into the org.example.parser package so
// the rest of the Java code (and tests) can import them.
grammar Sigma;

@header {
    package org.example.parser;
}

// Parser rules (with lowercase names)
compilationUnit
    : (declaration | statement)* EOF
    ;

declaration
    : variableDeclaration
    | constantDeclaration
    | methodDeclaration
    | classDeclaration
    ;

variableDeclaration
    : type IDENTIFIER (ASSIGN expression)? SEMI
    ;

constantDeclaration
    : FINAL type IDENTIFIER ASSIGN expression SEMI
    ;

methodDeclaration
    : type IDENTIFIER LPAREN parameterList? RPAREN block
    ;

classDeclaration
    : CLASS IDENTIFIER classBody
    ;

classBody
    : LBRACE (declaration | statement)* RBRACE
    ;

parameterList
    : parameter (COMMA parameter)*
    ;

parameter
    : type IDENTIFIER
    ;

statement
    : assignmentStatement
    | expressionStatement
    | ifStatement
    | forStatement
    | whileStatement
    | returnStatement
    | block
    ;

assignmentStatement
    : IDENTIFIER ASSIGN expression SEMI
    ;

expressionStatement
    : expression SEMI?
    ;

ifStatement
    : IF ifCondition statement (ELSE statement)?
    ;

ifCondition
    : LPAREN expression RPAREN
    | expression
    ;

forStatement
    : FOR forClause statement
    ;

forClause
    : LPAREN groovyForClause RPAREN
    | groovyForClause
    ;

groovyForClause
    : forLoopVariable IN expression
    ;

forLoopVariable
    : type IDENTIFIER
    | IDENTIFIER
    ;

whileStatement
    : WHILE LPAREN expression RPAREN statement
    ;

returnStatement
    : RETURN expression? SEMI
    ;

block
    : LBRACE (declaration | statement)* RBRACE
    ;

// Expression hierarchy with subtypes
expression
    : logicalOrExpression
    ;

logicalOrExpression
    : logicalAndExpression (LOGICAL logicalAndExpression)*
    ;

logicalAndExpression
    : relationalExpression (LOGICAL relationalExpression)*
    ;

relationalExpression
    : additiveExpression (RELATIONAL additiveExpression)*
    ;

additiveExpression
    : multiplicativeExpression ((PLUS | MINUS) multiplicativeExpression)*
    ;

multiplicativeExpression
    : powerExpression (MULTIPLICATIVE powerExpression)*
    ;

powerExpression
    : unaryExpression (POWER powerExpression)?
    ;

unaryExpression
    : NOT unaryExpression
    | MINUS unaryExpression
    | postfixExpression
    ;

postfixExpression
    : primaryExpression (postfixOp)*
    ;

postfixOp
    : DOT IDENTIFIER                           // member access
    | LPAREN argumentList? RPAREN              // method call
    ;

primaryExpression
    : IDENTIFIER
    | literal
    | LPAREN expression RPAREN
    | creator                      // <--- Add this option
    ;

creator
    : NEW type LPAREN argumentList? RPAREN
    ;

argumentList
    : expression (COMMA expression)*
    ;

literal
    : INTEGER
    | FLOAT
    | STRING
    | BOOLEAN
    | NULL
    ;

type
    : IDENTIFIER
    | PRIMITIVE_TYPE
    | STRING_TYPE
    | VOID
    ;

// Lexer rules (uppercase)

// Keywords
CLASS : 'class' ;
IF : 'if' ;
ELSE : 'else' ;
FOR : 'for' ;
IN : 'in' ;
WHILE : 'while' ;
RETURN : 'return' ;
FINAL : 'final' ;
NULL : 'null' ;

// Type keywords - united
PRIMITIVE_TYPE : 'int' | 'double' | 'float' | 'boolean' ;
STRING_TYPE : 'String' ;
VOID : 'void' ;

// Operators - United by category
// POWER uses '**' only. '^' is intentionally omitted because it has different semantics in some hosts (e.g. Scala).
POWER : '**' ;
MULTIPLICATIVE : '*' | '/' | '%' ;
RELATIONAL : '<' | '<=' | '>' | '>=' | '==' | '!=' ;
LOGICAL : '&&' | '||' ;
NOT : '!' ;
ASSIGN : '=' ;

// Individual operators (for unary and binary operations)
MINUS : '-' ;
PLUS : '+' ;

// Delimiters
LPAREN : '(' ;
RPAREN : ')' ;
LBRACE : '{' ;
RBRACE : '}' ;
SEMI : ';' ;
COMMA : ',' ;
DOT : '.' ;

// Literals
BOOLEAN : 'true' | 'false' ;

// Fragments for letters and digits
fragment LETTER : [a-zA-Z] ;
fragment DIGIT : [0-9] ;

IDENTIFIER : (LETTER | '_') (LETTER | DIGIT | '_')* ;

INTEGER : DIGIT+ ;

FLOAT : DIGIT+ '.' DIGIT+ ;

STRING : '"' ( ~["\\\r\n] | '\\' . )* '"' ;

// Skip whitespace and comments
WS : [ \t\r\n]+ -> skip ;

LINE_COMMENT : '//' ~[\r\n]* -> skip ;

BLOCK_COMMENT : '/*' .*? '*/' -> skip ;

// Add this to your Lexer rules
NEW : 'new' ;
