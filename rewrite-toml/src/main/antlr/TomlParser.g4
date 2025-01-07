/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied.  See the License for the
  specific language governing permissions and limitations
  under the License.
*/

// $antlr-format alignTrailingComments true, columnLimit 150, minEmptyLines 1, maxEmptyLinesToKeep 1, reflowComments false, useTab false
// $antlr-format allowShortRulesOnASingleLine false, allowShortBlocksOnASingleLine true, alignSemicolons hanging, alignColons hanging

parser grammar TomlParser;

options {
    tokenVocab = TomlLexer;
}

document
    : expression? (NL expression?)* EOF
    ;

expression
    : keyValue comment?
    | table comment?
    | comment
    ;

comment
    : COMMENT
    ;

keyValue
    : key EQUALS value
    ;

key
    : simpleKey
    | dottedKey
    ;

simpleKey
    : quotedKey
    | unquotedKey
    ;

unquotedKey
    : UNQUOTED_KEY
    ;

quotedKey
    : BASIC_STRING
    | LITERAL_STRING
    ;

dottedKey
    : simpleKey (DOT simpleKey)+
    ;

value
    : string
    | integer
    | floatingPoint
    | bool
    | dateTime
    | array
    | inlineTable
    ;

string
    : BASIC_STRING
    | ML_BASIC_STRING
    | LITERAL_STRING
    | ML_LITERAL_STRING
    ;

integer
    : DEC_INT
    | HEX_INT
    | OCT_INT
    | BIN_INT
    ;

floatingPoint
    : FLOAT
    | INF
    | NAN
    ;

bool
    : BOOLEAN
    ;

dateTime
    : OFFSET_DATE_TIME
    | LOCAL_DATE_TIME
    | LOCAL_DATE
    | LOCAL_TIME
    ;

commentOrNl
    : COMMENT NL
    | NL
    ;

array
    : L_BRACKET commentOrNl* R_BRACKET
    | L_BRACKET commentOrNl* value (COMMA commentOrNl* value COMMA?)* commentOrNl* R_BRACKET
    ;

table
    : standardTable
    | arrayTable
    ;

standardTable
    : L_BRACKET key R_BRACKET (commentOrNl* expression)*
    ;

inlineTable
    : L_BRACE commentOrNl* R_BRACE
    | L_BRACE commentOrNl* keyValue (COMMA commentOrNl* keyValue COMMA?)* commentOrNl* R_BRACE
    ;

arrayTable
    : DOUBLE_L_BRACKET key DOUBLE_R_BRACKET (commentOrNl* expression)*
    ;
