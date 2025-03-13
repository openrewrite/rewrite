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
    : typeName (LBRACK (typeParameter COMMA)* typeParameter RBRACK)? typeArray*
    ;

typeParameter
    : variance? type
    | WILDCARD
    ;

variance
    : WILDCARD Variance
    ;

typeArray
    : LSBRACK RSBRACK
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
