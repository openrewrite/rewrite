parser grammar Protobuf2Parser;

options { tokenVocab=Protobuf2Lexer; }

proto
  : syntax (importStatement | packageStatement | optionDef | topLevelDef | emptyStatement)* EOF
  ;

stringLiteral
  : StringLiteral
  ;

identOrReserved
  : ident
  | reservedWord
  ;

syntax
  : SYNTAX ASSIGN stringLiteral SEMI
  ;

importStatement
  : IMPORT (WEAK | PUBLIC)? stringLiteral SEMI
  ;

packageStatement
  : PACKAGE fullIdent SEMI
  ;

optionName
  : (ident | LPAREN fullIdent RPAREN ) (DOT identOrReserved)*
  ;

option
  : optionName ASSIGN constant
  ;

optionDef
  : OPTION option SEMI
  ;

optionList
  : (LBRACK option (COMMA option)* RBRACK)
  ;

topLevelDef
  : message
  | enumDefinition
  | service
  | extend
  ;

ident
  : Ident
  ;

message
  : MESSAGE ident messageBody
  ;

messageField
  : (OPTIONAL | REQUIRED | REPEATED) field
  ;

messageBody
  : LBRACE (messageField | enumDefinition | extend | message | optionDef | oneOf | mapField | reserved | emptyStatement)* RBRACE
  ;

extend
  : EXTEND fullIdent LBRACE ( messageField | emptyStatement )* RBRACE
  ;

enumDefinition
  : ENUM ident enumBody
  ;

enumBody
  : LBRACE (optionDef | enumField | emptyStatement)* RBRACE
  ;

enumField
  : ident ASSIGN MINUS? IntegerLiteral optionList? SEMI
  ;

service
  : SERVICE ident serviceBody
  ;

serviceBody
  : LBRACE (optionDef | rpc | emptyStatement)* RBRACE
  ;

rpc
  : RPC ident rpcInOut RETURNS rpcInOut (rpcBody | SEMI)
  ;

rpcInOut
  : LPAREN STREAM? messageType=fullIdent RPAREN
  ;

rpcBody
 : LBRACE (optionDef | emptyStatement)* RBRACE
 ;

reserved
  : RESERVED (ranges | fieldNames) SEMI
  ;

ranges
  : range (COMMA range)*
  ;

range
  : IntegerLiteral (TO IntegerLiteral)?
  ;

fieldNames
  : stringLiteral (COMMA stringLiteral)*
  ;

type
  : (DOUBLE
      | FLOAT
      | INT32
      | INT64
      | UINT32
      | UINT64
      | SINT32
      | SINT64
      | FIXED32
      | FIXED64
      | SFIXED32
      | SFIXED64
      | BOOL
      | STRING
      | BYTES) #PrimitiveType
  | fullIdent #FullyQualifiedType
  ;

field
  : type fieldName=identOrReserved ASSIGN IntegerLiteral optionList? SEMI
  ;

oneOf
  : ONEOF ident LBRACE (field | emptyStatement)* RBRACE
  ;

mapField
  : MAP LCHEVR keyType COMMA type RCHEVR ident ASSIGN IntegerLiteral optionList? SEMI
  ;

keyType
  : INT32
  | INT64
  | UINT32
  | UINT64
  | SINT32
  | SINT64
  | FIXED32
  | FIXED64
  | SFIXED32
  | SFIXED64
  | BOOL
  | STRING
  ;

reservedWord
  : MESSAGE
  | OPTION
  | PACKAGE
  | SERVICE
  | STREAM
  | STRING
  | SYNTAX
  | WEAK
  | RPC
  ;

/*
 Technically a fullIdent can't start with a reserved word, but
 it simplifies the use of the parser if we start with the assumption
 that the proto is well formed to begin with.

 Also, in some cases a fullIdent can begin with a dot and some cases not, but
 we are going to map both of these cases to the same AST element for simplicity.
*/
fullIdent
  : DOT? (identOrReserved DOT)* identOrReserved
  ;

emptyStatement
  : SEMI
  ;

constant
  : fullIdent
  | IntegerLiteral
  | NumericLiteral
  | StringLiteral
  | BooleanLiteral
  ;
