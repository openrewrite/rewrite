grammar GraphQL;

@header {
import java.util.*;
}

// Parser rules
document
    : definition* EOF
    ;

definition
    : executableDefinition
    | typeSystemDefinition
    | typeSystemExtension
    ;

executableDefinition
    : operationDefinition
    | fragmentDefinition
    ;

operationDefinition
    : operationType name? variableDefinitions? directives? selectionSet
    | selectionSet
    ;

operationType
    : 'query'
    | 'mutation'
    | 'subscription'
    ;

selectionSet
    : '{' selection* '}'
    ;

selection
    : field
    | fragmentSpread
    | inlineFragment
    ;

field
    : alias? name arguments? directives? selectionSet?
    ;

alias
    : name ':'
    ;

arguments
    : '(' (argument (','? argument)*)? ')'
    ;

argument
    : name ':' value
    ;

fragmentSpread
    : '...' fragmentName directives?
    ;

inlineFragment
    : '...' typeCondition? directives? selectionSet
    ;

fragmentDefinition
    : 'fragment' fragmentName typeCondition directives? selectionSet
    ;

fragmentName
    : name // but not 'on'
    ;

typeCondition
    : 'on' namedType
    ;

value
    : variable
    | intValue
    | floatValue
    | stringValue
    | booleanValue
    | nullValue
    | enumValue
    | listValue
    | objectValue
    ;

variable
    : '$' name
    ;

variableDefinitions
    : '(' variableDefinition (','? variableDefinition)* ')'
    ;

variableDefinition
    : variable ':' type defaultValue? directives?
    ;

defaultValue
    : '=' value
    ;

type
    : namedType
    | listType
    | nonNullType
    ;

namedType
    : name
    ;

listType
    : '[' type ']'
    ;

nonNullType
    : namedType '!'
    | listType '!'
    ;

directives
    : directive+
    ;

directive
    : '@' name arguments?
    ;

// Type System Definition
typeSystemDefinition
    : schemaDefinition
    | typeDefinition
    | directiveDefinition
    ;

typeSystemExtension
    : schemaExtension
    | typeExtension
    ;

schemaDefinition
    : description? 'schema' directives? '{' rootOperationTypeDefinition+ '}'
    ;

rootOperationTypeDefinition
    : operationType ':' namedType
    ;

schemaExtension
    : 'extend' 'schema' directives? '{' rootOperationTypeDefinition+ '}'
    | 'extend' 'schema' directives
    ;

typeDefinition
    : scalarTypeDefinition
    | objectTypeDefinition
    | interfaceTypeDefinition
    | unionTypeDefinition
    | enumTypeDefinition
    | inputObjectTypeDefinition
    ;

typeExtension
    : scalarTypeExtension
    | objectTypeExtension
    | interfaceTypeExtension
    | unionTypeExtension
    | enumTypeExtension
    | inputObjectTypeExtension
    ;

scalarTypeDefinition
    : description? 'scalar' name directives?
    ;

scalarTypeExtension
    : 'extend' 'scalar' name directives
    ;

objectTypeDefinition
    : description? 'type' name implementsInterfaces? directives? fieldsDefinition?
    ;

objectTypeExtension
    : 'extend' 'type' name implementsInterfaces? directives? fieldsDefinition
    | 'extend' 'type' name implementsInterfaces? directives
    | 'extend' 'type' name implementsInterfaces
    ;

implementsInterfaces
    : 'implements' '&'? namedType
    | implementsInterfaces '&' namedType
    ;

fieldsDefinition
    : '{' fieldDefinition* '}'
    ;

fieldDefinition
    : description? name argumentsDefinition? ':' type directives?
    ;

argumentsDefinition
    : '(' inputValueDefinition (','? inputValueDefinition)* ')'
    ;

inputValueDefinition
    : description? name ':' type defaultValue? directives?
    ;

interfaceTypeDefinition
    : description? 'interface' name implementsInterfaces? directives? fieldsDefinition?
    ;

interfaceTypeExtension
    : 'extend' 'interface' name implementsInterfaces? directives? fieldsDefinition
    | 'extend' 'interface' name implementsInterfaces? directives
    | 'extend' 'interface' name implementsInterfaces
    ;

unionTypeDefinition
    : description? 'union' name directives? unionMemberTypes?
    ;

unionMemberTypes
    : '=' '|'? namedType
    | unionMemberTypes '|' namedType
    ;

unionTypeExtension
    : 'extend' 'union' name directives? unionMemberTypes
    | 'extend' 'union' name directives
    ;

enumTypeDefinition
    : description? 'enum' name directives? enumValuesDefinition?
    ;

enumValuesDefinition
    : '{' enumValueDefinition* '}'
    ;

enumValueDefinition
    : description? enumValue directives?
    ;

enumTypeExtension
    : 'extend' 'enum' name directives? enumValuesDefinition
    | 'extend' 'enum' name directives
    ;

inputObjectTypeDefinition
    : description? 'input' name directives? inputFieldsDefinition?
    ;

inputFieldsDefinition
    : '{' inputValueDefinition* '}'
    ;

inputObjectTypeExtension
    : 'extend' 'input' name directives? inputFieldsDefinition
    | 'extend' 'input' name directives
    ;

directiveDefinition
    : description? 'directive' '@' name argumentsDefinition? 'repeatable'? 'on' directiveLocations
    ;

directiveLocations
    : '|'? directiveLocation
    | directiveLocations '|' directiveLocation
    ;

directiveLocation
    : executableDirectiveLocation
    | typeSystemDirectiveLocation
    ;

executableDirectiveLocation
    : 'QUERY'
    | 'MUTATION'
    | 'SUBSCRIPTION'
    | 'FIELD'
    | 'FRAGMENT_DEFINITION'
    | 'FRAGMENT_SPREAD'
    | 'INLINE_FRAGMENT'
    | 'VARIABLE_DEFINITION'
    ;

typeSystemDirectiveLocation
    : 'SCHEMA'
    | 'SCALAR'
    | 'OBJECT'
    | 'FIELD_DEFINITION'
    | 'ARGUMENT_DEFINITION'
    | 'INTERFACE'
    | 'UNION'
    | 'ENUM'
    | 'ENUM_VALUE'
    | 'INPUT_OBJECT'
    | 'INPUT_FIELD_DEFINITION'
    ;

// Common rules
name
    : NAME
    | 'query'
    | 'mutation' 
    | 'subscription'
    | 'fragment'
    | 'on'
    | 'true'
    | 'false'
    | 'null'
    | 'type'
    | 'interface'
    | 'union'
    | 'enum'
    | 'input'
    | 'scalar'
    | 'schema'
    | 'directive'
    | 'extends'
    | 'implements'
    | 'repeatable'
    ;

description
    : stringValue
    ;

// Values
booleanValue
    : 'true'
    | 'false'
    ;

nullValue
    : 'null'
    ;

enumValue
    : name // but not 'true', 'false' or 'null'
    ;

listValue
    : '[' ']'
    | '[' value (','? value)* ']'
    ;

objectValue
    : '{' '}'
    | '{' objectField (','? objectField)* '}'
    ;

objectField
    : name ':' value
    ;

// Lexer rules
NAME
    : [_A-Za-z][_0-9A-Za-z]*
    ;

intValue
    : INT
    ;

INT
    : '-'? '0'
    | '-'? [1-9] [0-9]*
    ;

floatValue
    : FLOAT
    ;

FLOAT
    : INT '.' [0-9]+ EXPONENT?
    | INT EXPONENT
    ;

EXPONENT
    : [eE] [+-]? [0-9]+
    ;

stringValue
    : STRING
    | BLOCK_STRING
    ;

STRING
    : '"' (~["\\\n\r] | ESCAPED_CHAR)* '"'
    ;

BLOCK_STRING
    : '"""' .*? '"""'
    ;

fragment ESCAPED_CHAR
    : '\\' (["\\/bfnrt] | UNICODE)
    ;

fragment UNICODE
    : 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
    ;

fragment HEX_DIGIT
    : [0-9a-fA-F]
    ;

// Comments and Ignored Tokens
COMMENT
    : '#' ~[\r\n]* -> channel(HIDDEN)
    ;

WS
    : [ \t\r\n]+ -> skip
    ;

COMMA
    : ','
    ;

// Skip Unicode BOM
UNICODE_BOM
    : '\uFEFF' -> skip
    ;