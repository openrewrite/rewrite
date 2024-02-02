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
    : typeName (LBRACK (typeParameter COMMA)* typeParameter RBRACK)?
    ;

typeParameter
    : variance? type
    | WILDCARD
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
