lexer grammar Protobuf2Lexer;

SEMI            : ';';
COLON           : ':';

BOOL            : 'bool';
BYTES           : 'bytes';
DOUBLE          : 'double';
ENUM            : 'enum';
EXTEND          : 'extend';
FIXED32         : 'fixed32';
FIXED64         : 'fixed64';
FLOAT           : 'float';
IMPORT          : 'import';
INT32           : 'int32';
INT64           : 'int64';
MAP             : 'map';
MESSAGE         : 'message';
ONEOF           : 'oneof';
OPTION          : 'option';
PACKAGE         : 'package';
PUBLIC          : 'public';
REPEATED        : 'repeated';
REQUIRED        : 'required';
RESERVED        : 'reserved';
RETURNS         : 'returns';
RPC             : 'rpc';
SERVICE         : 'service';
SFIXED32        : 'sfixed32';
SFIXED64        : 'sfixed64';
SINT32          : 'sint32';
SINT64          : 'sint64';
STREAM          : 'stream';
STRING          : 'string';
SYNTAX          : 'syntax';
TO              : 'to';
UINT32          : 'uint32';
UINT64          : 'uint64';
WEAK            : 'weak';
OPTIONAL        : 'optional';

fragment Letter
  : [A-Za-z_]
  ;

fragment DecimalDigit
  : [0-9]
  ;

fragment OctalDigit
  : [0-7]
  ;

fragment HexDigit
  : [0-9A-Fa-f]
  ;

Ident
  : Letter (Letter | DecimalDigit)*
  ;

IntegerLiteral
  : DecimalLiteral
  | OctalLiteral
  | HexLiteral
  ;

NumericLiteral
  : (MINUS | PLUS)? (IntegerLiteral | FloatLiteral)
  ;

fragment DecimalLiteral
  : [1-9] DecimalDigit*
  ;

fragment OctalLiteral
  : '0' OctalDigit*
  ;

fragment HexLiteral
  : '0' ('x' | 'X') HexDigit+
  ;

FloatLiteral
  : (Decimals '.' Decimals? Exponent? | Decimals Exponent | '.' Decimals Exponent?)
  | 'inf'
  | 'nan'
  ;

fragment Decimals
  : DecimalDigit+
  ;

fragment Exponent
  : ('e' | 'E') ('+' | '-')? Decimals
  ;

BooleanLiteral
  : 'true'
  | 'false'
  ;

StringLiteral
  : '\'' CharValue* '\''
  | '"' CharValue* '"'
  ;

fragment CharValue
  : HexEscape
  | OctEscape
  | CharEscape
  | ~[\u0000\n\\]
  ;

fragment HexEscape
  : '\\' ('x' | 'X') HexDigit HexDigit
  ;

fragment OctEscape
  : '\\' OctalDigit OctalDigit OctalDigit
  ;

fragment CharEscape
  : '\\' [abfnrtv\\'"]
  ;

Quote
  : '\''
  | '"'
  ;

LPAREN          : '(';
RPAREN          : ')';
LBRACE          : '{';
RBRACE          : '}';
LBRACK          : '[';
RBRACK          : ']';
LCHEVR          : '<';
RCHEVR          : '>';
COMMA           : ',';
DOT             : '.';
MINUS           : '-';
PLUS            : '+';

ASSIGN          : '=';

WS
  : [ \t\n\r\u00A0\uFEFF\u2003] + -> skip
  ;

UTF_8_BOM : '\uFEFF' -> skip
  ;

COMMENT
  : '/*' .*? '*/' -> skip
  ;

LINE_COMMENT
  : '//' ~[\r\n]* -> skip
  ;
