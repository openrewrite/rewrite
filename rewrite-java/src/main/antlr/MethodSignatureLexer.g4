lexer grammar MethodSignatureLexer;

// §3.11 Separators

CONSTRUCTOR     : '<constructor>' | '<init>';

LPAREN          : '(';
RPAREN          : ')';
LBRACK          : '[';
RBRACK          : ']';
COMMA           : ',';

// The widening of DOT to include '/' allows matching of package separators
// in JavaScript and TypeScript import paths, e.g. @types/lodash..* map(..)
DOT             : '.' | '/';

// §3.12 Operators

BANG            : '!';
WILDCARD        : '*';

AND             : '&&';
OR              : '||';

ELLIPSIS        : '...';

DOTDOT          : '..';
POUND           : '#';
SPACE           : ' ';

Identifier
    :   JavaLetter JavaLetterOrDigit*
    ;

fragment
JavaLetter
    :   [a-zA-Z$_] // these are the "java letters" below 0x7F
    |
        // The widening of JavaLetter to include '@' allows matching package name parts
        // in JavaScript and TypeScript import paths, e.g. @types/lodash..* map(..)
        '@'
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
