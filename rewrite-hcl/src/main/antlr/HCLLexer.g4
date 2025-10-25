lexer grammar HCLLexer;

@lexer::header
{import java.util.Stack;}

@lexer::members
{
    private enum CurlyType {
        INTERPOLATION,
        OBJECT
    }

    private Stack<CurlyType> leftCurlyStack = new Stack<CurlyType>();
    private Stack<String> heredocIdentifier = new Stack<String>();
}

FOR_BRACE             : '{' (WS|NEWLINE|COMMENT|LINE_COMMENT)* 'for' WS;
FOR_BRACK             : '[' (WS|NEWLINE|COMMENT|LINE_COMMENT)* 'for' WS;

IF              : 'if';
IN              : 'in';

BooleanLiteral
    : 'true'
    | 'false'
    ;

NULL                            : 'null';

LBRACE                          : '{'
{
    leftCurlyStack.push(CurlyType.OBJECT);
};

RBRACE                          : '}'
{
    if(!leftCurlyStack.isEmpty()) {
        if(leftCurlyStack.pop() == CurlyType.INTERPOLATION) {
            popMode();
        } else {
            // closing an object, stay in the default mode
        }
    }
};

ASSIGN          : '=';

fragment StringLiteralChar
    : ~[\n\r%$"\\]
    | EscapeSequence
    ;

Identifier
    : Letter (LetterOrDigit | '-')*
    ;

// Lexical Elements - Comments and Whitespace
// https://github.com/hashicorp/hcl2/blob/master/hcl/hclsyntax/spec.md#comments-and-whitespace
WS              : [ \t\r\u000C]+                           -> channel(HIDDEN);
COMMENT         : '/*' .*? '*/'                            -> channel(HIDDEN);
LINE_COMMENT    : ('//' | '#') ~[\r\n]* '\r'? ('\n' | EOF) -> channel(HIDDEN);
NEWLINE         : '\n'                                     -> channel(HIDDEN);

fragment LetterOrDigit
    : Letter
    | [0-9]
    ;

fragment Letter
    : [a-zA-Z$_]
    | ~[\u0000-\u007F\uD800-\uDBFF] // covers all characters above 0x7F which are not a surrogate
    | [\uD800-\uDBFF] [\uDC00-\uDFFF] // covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
    ;

// Other fragments for use in template expressions

fragment EscapeSequence
    : '\\' [nrt"\\]
    | '\\' HexDigit HexDigit HexDigit HexDigit
    | '\\' HexDigit HexDigit HexDigit HexDigit HexDigit HexDigit HexDigit HexDigit
    ;

fragment HexDigit
    : [0-9a-fA-F]
    ;

// Lexical Elements - Literals
// https://github.com/hashicorp/hcl2/blob/master/hcl/hclsyntax/spec.md#numeric-literals

NumericLiteral
    : [0-9]+ '.' [0-9]+ ExponentPart?
    | [0-9]+ ExponentPart
    | [0-9]+
    ;

fragment ExponentPart
    : [eE] [+\-]? [0-9]+
    ;

QUOTE                               : '"'       -> pushMode(TEMPLATE);

// Lexical Elements - Operators and Delimiters
// https://github.com/hashicorp/hcl2/blob/master/hcl/hclsyntax/spec.md#operators-and-delimiters

HEREDOC_START                       : '<<' '-'? -> pushMode(HEREDOC_PREAMBLE);

PLUS                            : '+';
AND                             : '&&';
EQ                              : '==';
LT                              : '<';
COLON                           : ':';
LBRACK                          : '[';
LPAREN                          : '(';
MINUS                           : '-';
OR                              : '||';
NEQ                             : '!=';
GT                              : '>';
QUESTION                        : '?';
RBRACK                          : ']';
RPAREN                          : ')';
MUL                             : '*';
NOT                             : '!';
LEQ                             : '<=';
DOT                             : '.';
DIV                             : '/';
GEQ                             : '>=';
ARROW                           : '=>';
COMMA                           : ',';
MOD                             : '%';
ELLIPSIS                        : '...';
TILDE                           : '~';


// ----------------------------------------------------------------------------------------------
mode TEMPLATE;
// ----------------------------------------------------------------------------------------------

TEMPLATE_INTERPOLATION_START    : '${' {leftCurlyStack.push(CurlyType.INTERPOLATION);} -> pushMode(DEFAULT_MODE);
//TEMPLATE_DIRECTIVE_START        : '%{' -> pushMode(DEFAULT_MODE);

TemplateStringLiteral
    : TemplateStringLiteralChar+
    ;

TemplateStringLiteralChar
    : ~[\n\r%$"\\]
    | '$' '$'
    | '$' {_input.LA(1) != '{'}?
    | '%' '%'
    | '%' {_input.LA(1) != '{'}?
    | EscapeSequence
    ;

END_QUOTE           : '"' -> type(QUOTE), popMode;

// ----------------------------------------------------------------------------------------------
mode HEREDOC_PREAMBLE;
// ----------------------------------------------------------------------------------------------
HP_NEWLINE : '\n' -> type(NEWLINE), mode(HEREDOC);
HP_WS              : [ \t\r\u000C]+                           -> channel(HIDDEN);
HP_COMMENT         : '/*' .*? '*/'                            -> channel(HIDDEN);
HP_LINE_COMMENT    : ('//' | '#') ~[\r\n]* '\r'? -> channel(HIDDEN);

HPIdentifier : Letter (LetterOrDigit | '-')* {
    heredocIdentifier.push(getText());
} -> type(Identifier);

// ----------------------------------------------------------------------------------------------
mode HEREDOC;
// ----------------------------------------------------------------------------------------------

H_NEWLINE                         : '\n' -> type(NEWLINE);

H_TEMPLATE_INTERPOLATION_START    : '${' {leftCurlyStack.push(CurlyType.INTERPOLATION);} -> type(TEMPLATE_INTERPOLATION_START), pushMode(DEFAULT_MODE);
//H_TEMPLATE_DIRECTIVE_START        : '%{' -> type(TEMPLATE_DIRECTIVE_START), pushMode(DEFAULT_MODE);

HTemplateLiteral : HTemplateLiteralChar+
{
  if(!heredocIdentifier.isEmpty() && getText().endsWith(heredocIdentifier.peek())) {
      setType(Identifier);
      heredocIdentifier.pop();
      popMode();
  }
};

HTemplateLiteralChar
    : ~[\n\r%$]
    | '$' ~[{]
    | '%' ~[{]
    | EscapeSequence
    ;
