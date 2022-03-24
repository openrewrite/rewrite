parser grammar JsonPathParser;

options { tokenVocab=JsonPathLexer; }

jsonPath
    : ROOT? expression+
    ;

expression
    : DOT dotOperator
    | recursiveDecent
    | bracketOperator
    ;

dotOperator
    : bracketOperator
    | property
    | wildcard
    ;

recursiveDecent
    : DOT_DOT dotOperator
    ;

bracketOperator
    : LBRACK (filter | slice | indexes | property+) RBRACK
    ;

filter
    : QUESTION LPAREN filterExpression+ RPAREN
    ;

filterExpression
    : binaryExpression
    | regexExpression
    | unaryExpression
    ;

binaryExpression
    : binaryExpression LOGICAL_OPERATOR binaryExpression
    | binaryExpression LOGICAL_OPERATOR regexExpression
    | regexExpression LOGICAL_OPERATOR regexExpression
    | regexExpression LOGICAL_OPERATOR binaryExpression
    | unaryExpression EQUALITY_OPERATOR literalExpression
    | literalExpression EQUALITY_OPERATOR unaryExpression
    ;

// Note: @.name cannot be a StringLiteral.
// DOT property is a compromise to simplify visitors.
regexExpression
    : AT (DOT property | DOT? LBRACK StringLiteral RBRACK) MATCHES_REGEX_OPEN REGEX MATCHES_REGEX_CLOSE
    ;

unaryExpression
    : AT (DOT Identifier | DOT? LBRACK StringLiteral RBRACK)
    | jsonPath
    ;

literalExpression
    : StringLiteral
    | PositiveNumber
    | NegativeNumber
    | NumericLiteral
    | TRUE
    | FALSE
    | NULL
    ;

property
    : StringLiteral
    | Identifier
    ;

// Wildcard is defined to generate a visitor.
wildcard
    : WILDCARD
    ;

slice
    : start COLON end?
    | COLON PositiveNumber
    | NegativeNumber COLON
    | wildcard
    ;

start
    : PositiveNumber
    ;

end
    : PositiveNumber
    ;

indexes
    : PositiveNumber+
    ;
