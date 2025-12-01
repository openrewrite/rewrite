/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * XPath lexer for a limited subset of XPath expressions.
 * Supports absolute and relative paths, wildcards, predicates,
 * attribute access, and common XPath functions.
 */
lexer grammar XPathLexer;

// Whitespace
WS : [ \t\r\n]+ -> skip ;

// Path separators
SLASH : '/' ;
DOUBLE_SLASH : '//' ;
AXIS_SEP : '::' ;

// Brackets
LBRACKET : '[' ;
RBRACKET : ']' ;
LPAREN : '(' ;
RPAREN : ')' ;

// Operators
AT : '@' ;
DOTDOT : '..' ;  // Must come before DOT for proper lexing
DOT : '.' ;
COMMA : ',' ;
EQUALS : '=' ;
NOT_EQUALS : '!=' ;
LTE : '<=' ;  // Must come before LT for proper lexing
GTE : '>=' ;  // Must come before GT for proper lexing
LT : '<' ;
GT : '>' ;
WILDCARD : '*' ;

// Numbers
NUMBER : [0-9]+ ('.' [0-9]+)? ;

// Logical operators (for predicate conditions)
AND : 'and' ;
OR : 'or' ;

// Node type keywords (must be before NCNAME to take precedence)
// These are the only valid node types per XPath 1.0 spec [38]
TEXT : 'text' ;
COMMENT : 'comment' ;
NODE : 'node' ;
PROCESSING_INSTRUCTION : 'processing-instruction' ;

// String literals
STRING_LITERAL
    : '\'' (~['])* '\''
    | '"' (~["])* '"'
    ;

// NCName (Non-Colonized Name) - XML name without colons
// QName (Qualified Name) - NCName with optional prefix
// QNAME must come before NCNAME to match longer token first
QNAME
    : NCNAME_CHARS ':' NCNAME_CHARS
    ;

NCNAME
    : NCNAME_CHARS
    ;

fragment NCNAME_CHARS
    : NAME_START_CHAR NAME_CHAR*
    ;

fragment NAME_START_CHAR
    : [a-zA-Z_]
    | '\u00C0'..'\u00D6'
    | '\u00D8'..'\u00F6'
    | '\u00F8'..'\u02FF'
    | '\u0370'..'\u037D'
    | '\u037F'..'\u1FFF'
    | '\u200C'..'\u200D'
    | '\u2070'..'\u218F'
    | '\u2C00'..'\u2FEF'
    | '\u3001'..'\uD7FF'
    | '\uF900'..'\uFDCF'
    | '\uFDF0'..'\uFFFD'
    ;

fragment NAME_CHAR
    : NAME_START_CHAR
    | '-'
    | '.'
    | [0-9]
    | '\u00B7'
    | '\u0300'..'\u036F'
    | '\u203F'..'\u2040'
    ;
