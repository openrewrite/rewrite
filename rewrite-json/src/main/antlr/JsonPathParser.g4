parser grammar JsonPathParser;

options { tokenVocab=JsonPathLexer; }

jsonPath
    : ROOT expression*
    | expression+
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
    | containsExpression
    | unaryExpression
    ;

binaryExpression
    // Binary logical operators.
    : binaryExpression LOGICAL_OPERATOR binaryExpression
    | binaryExpression LOGICAL_OPERATOR regexExpression
    | binaryExpression LOGICAL_OPERATOR containsExpression
    // Regex logical operators.
    | regexExpression LOGICAL_OPERATOR regexExpression
    | regexExpression LOGICAL_OPERATOR binaryExpression
    | regexExpression LOGICAL_OPERATOR containsExpression
    // Contains logical operators.
    | containsExpression LOGICAL_OPERATOR containsExpression
    | containsExpression LOGICAL_OPERATOR binaryExpression
    | containsExpression LOGICAL_OPERATOR regexExpression
    // Equality operators.
    | unaryExpression EQUALITY_OPERATOR literalExpression
    | literalExpression EQUALITY_OPERATOR unaryExpression
    ;

containsExpression
    : unaryExpression CONTAINS literalExpression
    | literalExpression CONTAINS unaryExpression
    ;

regexExpression
    : unaryExpression MATCHES_REGEX_OPEN REGEX MATCHES_REGEX_CLOSE
    ;

unaryExpression
    : AT (DOT Identifier | DOT? LBRACK StringLiteral RBRACK)?
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
