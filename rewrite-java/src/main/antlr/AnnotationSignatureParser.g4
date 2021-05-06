parser grammar AnnotationSignatureParser;

options { tokenVocab=AnnotationSignatureLexer; }

annotation
    :	AT annotationName ( LPAREN ( elementValuePairs | elementValue )? RPAREN )?
    ;

annotationName
    :   qualifiedName ;

qualifiedName
    :   Identifier (DOT Identifier)*
    ;

elementValuePairs
    :   elementValuePair (COMMA elementValuePair)*
    ;

elementValuePair
    :   Identifier ASSIGN elementValue
    ;

elementValue
    :   primary
    ;

primary
    :   literal
    |   type
    ;

type
    :   classOrInterfaceType (LBRACK RBRACK)*
    ;

classOrInterfaceType
    :   Identifier (DOT Identifier)*
    ;

literal
    :   IntegerLiteral
    |   FloatingPointLiteral
    |   CharacterLiteral
    |   StringLiteral
    |   BooleanLiteral
    ;
