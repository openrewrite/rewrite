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
    : matcherName LPAREN ((matcherParameter COMMA)* matcherParameter)? RPAREN
    ;

matcherParameter
    :   FullyQualifiedName
    |   Identifier
    |   Number
    ;

parameterName
    : Identifier
    ;

matcherName
    :   Identifier
    ;
