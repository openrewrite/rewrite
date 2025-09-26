parser grammar MethodSignatureParser;

options { tokenVocab=MethodSignatureLexer; }

methodPattern
    :    targetTypePattern (SPACE+ | POUND) simpleNamePattern SPACE* formalParametersPattern
    ;

formalParametersPattern
    :    LPAREN formalsPattern? RPAREN
    ;

formalsPattern
    :    DOTDOT formalsPatternAfterDotDot?
    |    formalTypePattern (ELLIPSIS | formalsTail?)
    ;

formalsTail
    :    COMMA formalsPattern
    ;

formalsPatternAfterDotDot
    :    COMMA formalTypePattern (ELLIPSIS | formalsPatternAfterDotDot?)
    ;

targetTypePattern
    :   classNameOrInterface
    |    BANG classNameOrInterface
    ;

formalTypePattern
    :    classNameOrInterface
    |    BANG classNameOrInterface
    ;

classNameOrInterface
    :    (Identifier | WILDCARD) (DOT | DOTDOT | Identifier | WILDCARD)* arrayDimensions?
    ;

arrayDimensions
    :    LBRACK RBRACK arrayDimensions?
    ;

simpleNamePattern
    :    simpleNamePart+
    |    JAVASCRIPT_DEFAULT_METHOD
    |    CONSTRUCTOR
    ;

simpleNamePart
    :    Identifier
    |    WILDCARD
    ;