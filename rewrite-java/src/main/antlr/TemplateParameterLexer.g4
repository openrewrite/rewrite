lexer grammar TemplateParameterLexer;

LPAREN          : '(';
RPAREN          : ')';
DOT             : '.';
COLON           : ':';
COMMA           : ',';
LBRACK          : '<';
RBRACK          : '>';
WILDCARD        : '?';

Variance
    :   'extends'
    |   'super'
    ;

FullyQualifiedName
    :   'boolean'
    |   'byte'
    |   'char'
    |   'double'
    |   'float'
    |   'int'
    |   'long'
    |   'short'
    |   'String'
    |   'Object'
    |   Identifier (DOT Identifier)+
    ;

Number
    :   [0-9]+
    ;

Identifier
    :   JavaLetter JavaLetterOrDigit*
    ;

fragment
JavaLetter
    :   [a-zA-Z$_] // these are the "java letters" below 0x7F
    |   // covers all characters above 0x7F which are not a surrogate
        ~[\u0000-\u007F\uD800-\uDBFF]
        {Character.isJavaIdentifierStart(_input.LA(-1))}?
    |   // covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
        [\uD800-\uDBFF] [\uDC00-\uDFFF]
        {Character.isJavaIdentifierStart(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)))}?
    ;

fragment
JavaLetterOrDigit
    :   [a-zA-Z0-9$_] // these are the "java letters or digits" below 0x7F
    |   // covers all characters above 0x7F which are not a surrogate
        ~[\u0000-\u007F\uD800-\uDBFF]
        {Character.isJavaIdentifierPart(_input.LA(-1))}?
    |   // covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
        [\uD800-\uDBFF] [\uDC00-\uDFFF]
        {Character.isJavaIdentifierPart(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)))}?
    ;

S   :  [ \t\r\n] -> skip ;
