parser grammar TemplateParameterParser;

options { tokenVocab=TemplateParameterLexer; }

matcherPattern
    : typedPattern
    | parameterName
    ;

typedPattern
    : (parameterName COLON)? patternType
    ;

patternType
    : matcherName LPAREN (type)? RPAREN
    ;

type
    :   typeName typeParameter?
    ;

typeParameter
    : LBRACK variance? type RBRACK
    ;

variance
    : WILDCARD Variance
    ;

parameterName
    : Identifier
    ;

typeName
    : FullyQualifiedName
    | Identifier
    ;

matcherName
    :   Identifier
    ;
