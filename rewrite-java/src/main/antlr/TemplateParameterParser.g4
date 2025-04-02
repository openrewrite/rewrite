parser grammar TemplateParameterParser;

options { tokenVocab=TemplateParameterLexer; }

matcherPattern
    : typedPattern
    | parameterName
    ;

genericPattern
    : genericName (Extends (type AND)* type)?
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
    : WILDCARD Extends
    | WILDCARD Super
    ;

typeArray
    : LSBRACK RSBRACK
    ;

parameterName
    : Identifier
    ;

genericName
    : Identifier
    ;

typeName
    : FullyQualifiedName
    | Identifier
    ;

matcherName
    :   Identifier
    ;
