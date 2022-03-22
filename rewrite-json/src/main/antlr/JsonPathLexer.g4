lexer grammar JsonPathLexer;

WS : [ \t\n\r\u000C]+ -> skip;
UTF_8_BOM : '\uFEFF' -> skip;

MATCHES_REGEX_OPEN : MATCHES [ \t]? TICK -> pushMode(MATCHES_REGEX) ;

// Delimiters
LBRACE : '{';
RBRACE : '}';

LBRACK : '[';
RBRACK : ']';

LPAREN : '(';
RPAREN : ')';

// Operators
AT : '@';
DOT : '.';
DOT_DOT : '..';
ROOT : '$';
WILDCARD : '*';
COLON : ':';
QUESTION : '?';

Identifier
    : [_A-Za-z] [_A-Za-z0-9-]*
    ;

StringLiteral
    : QUOTE (ESCAPE_SEQUENCE | SAFE_CODE_POINT)*? QUOTE
    | TICK (ESCAPE_SEQUENCE | SAFE_CODE_POINT)*? TICK
    ;

PositiveNumber
    : [0-9]+
    ;

NegativeNumber
    : MINUS PositiveNumber
    ;

NumericLiteral
    : MINUS? PositiveNumber EXPONENT_PART?
    ;

COMMA : ',' -> skip;

TICK : '\'' ;
QUOTE: '"' ;

// Operators
MATCHES: '=~';

LOGICAL_OPERATOR: (AND | OR) ;
AND : '&&';
OR : '||';
//NOT : '!';

EQUALITY_OPERATOR: (EQ | NE) ;
EQ : '==';
NE : '!=';

// Note: if possible, distinguish long from int for positive and negative numbers if comparisions are implemented.
//GE : '>=';
//GT : '>';
//LE : '<=';
//LT : '<';

// Operations
//IN : 'in'
//NIN : 'nin'
//SUBSET : 'subset'
//CONTAINS : 'contains'
//SIZE : 'size'

TRUE : 'true';
FALSE : 'false';
NULL : 'null';

fragment
ESCAPE_SEQUENCE
    : '\\' [nrt"\\]
    | '\\' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
    | '\\' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
    ;

fragment
UNICODE
   : 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
   ;

fragment
HEX_DIGIT
    : [0-9a-fA-F]
    ;

fragment
SAFE_CODE_POINT
   : ~["\\\u0000-\u001F]
   ;

fragment
EXPONENT_PART
    : [eE] [+\-]? [0-9]+
    ;

fragment
MINUS
    : '-'
    ;

mode MATCHES_REGEX;
MATCHES_REGEX_CLOSE : TICK -> popMode ;
S : [ \t]+ -> skip ;
REGEX : ~[']+ ;