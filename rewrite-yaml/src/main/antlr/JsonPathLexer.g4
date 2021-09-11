lexer grammar JsonPathLexer;

StringLiteral
   : '"' (EscapeSequence | SAFECODEPOINT)* '"'
   | '\'' (EscapeSequence | SAFECODEPOINT)* '\''
   ;

fragment EscapeSequence
    : '\\' [nrt"\\]
    | '\\' HexDigit HexDigit HexDigit HexDigit
    | '\\' HexDigit HexDigit HexDigit HexDigit HexDigit HexDigit HexDigit HexDigit
    ;

fragment UNICODE
   : 'u' HexDigit HexDigit HexDigit HexDigit
   ;

fragment HexDigit
    : [0-9a-fA-F]
    ;

fragment SAFECODEPOINT
   : ~ ["\\\u0000-\u001F]
   ;

NumericLiteral
    : '-'? [0-9]+ ExponentPart?
    ;

fragment ExponentPart
    : [eE] [+\-]? [0-9]+
    ;

AT : '@';
DOT_DOT : '..';
DOT : '.';
ROOT : '$';
WILDCARD : '*';

AND : 'and';
EQ : '==';
GE : '>=';
GT : '>';
LE : '<=';
LT : '<';
NE : '!=';
NOT : 'not';
OR : 'or';

TRUE : 'true';
FALSE : 'false';
NULL : 'null';

LBRACE : '{';
RBRACE : '}';
LBRACK : '[';
RBRACK : ']';
COLON : ':';
COMMA : ',';
LPAREN : '(';
RPAREN : ')';
QUESTION : '?';

Identifier
   : [_A-Za-z] [_A-Za-z0-9-]*
   ;

WS : [ \t\n\r\u000C] + -> skip;
