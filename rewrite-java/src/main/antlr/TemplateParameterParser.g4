parser grammar TemplateParameterParser;

options { tokenVocab=TemplateParameterLexer; }

matcherPattern
    :   matcherName LPAREN matcherParameter* RPAREN
    ;

matcherParameter
    :   FullyQualifiedName
    |   Identifier
    |   Number
    ;

matcherName
    :   Identifier
    ;
