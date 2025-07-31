parser grammar MethodSignatureParser;

options { tokenVocab=MethodSignatureLexer; }

methodPattern
    :    targetTypePattern (SPACE* | DOT | POUND) simpleNamePattern formalParametersPattern
    ;

formalParametersPattern
    :    LPAREN formalsPattern? RPAREN
    ;

formalsPattern
    :    dotDot (COMMA SPACE* formalsPatternAfterDotDot)* 
    |    wildcard (COMMA SPACE* formalsPattern)*
    |    optionalParensTypePattern (COMMA SPACE* formalsPattern)*
    |    formalTypePattern ELLIPSIS
    ;

wildcard
    :   WILDCARD
    ;

dotDot
    :   DOTDOT
    ;

formalsPatternAfterDotDot
    :    optionalParensTypePattern (COMMA SPACE* formalsPatternAfterDotDot)* 
    |    formalTypePattern ELLIPSIS
    ;

optionalParensTypePattern
    :    LPAREN formalTypePattern RPAREN
    |    formalTypePattern
    ;

targetTypePattern
    :   classNameOrInterface
    |    BANG targetTypePattern
    |    targetTypePattern AND targetTypePattern 
    |    targetTypePattern OR targetTypePattern
    ;

formalTypePattern
    :    classNameOrInterface
    |    BANG formalTypePattern
    |    formalTypePattern AND formalTypePattern 
    |    formalTypePattern OR formalTypePattern
    ;

classNameOrInterface
    :    (Identifier | WILDCARD | DOT | DOTDOT)+ (LBRACK RBRACK)*
    ;

simpleNamePattern
    :    Identifier (WILDCARD Identifier)* WILDCARD?
    |    WILDCARD (Identifier WILDCARD)* Identifier?
    |    CONSTRUCTOR
    ;
