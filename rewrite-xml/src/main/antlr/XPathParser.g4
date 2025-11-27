/*
 * Copyright 2024 the original author or authors.
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
 * XPath parser for a limited subset of XPath expressions.
 *
 * Supports:
 * - Absolute paths: /root/child
 * - Relative paths: child/grandchild
 * - Descendant-or-self: //element
 * - Wildcards: /root/*
 * - Attribute access: /root/@attr, /root/element/@*
 * - Node type tests: /root/element/text(), /root/comment(), etc.
 * - Predicates with conditions: /root/element[@attr='value']
 * - Child element predicates: /root/element[child='value']
 * - XPath functions: local-name(), namespace-uri(), text(), contains(), etc.
 * - Logical operators in predicates: and, or
 * - Multiple predicates: /root/element[@attr='value'][local-name()='element']
 * - Top-level function expressions: contains(/root/element, 'value')
 * - Boolean expressions: not(contains(...)), string-length(...) > 2
 */
parser grammar XPathParser;

options { tokenVocab=XPathLexer; }

// Entry point for XPath expression
xpathExpression
    : booleanExpr
    | absoluteLocationPath
    | relativeLocationPath
    ;

// Boolean expression (function calls with optional comparison)
booleanExpr
    : functionCall comparisonOp comparand
    | functionCall
    ;

// Comparison operators
comparisonOp
    : EQUALS
    | NOT_EQUALS
    | LT
    | GT
    | LTE
    | GTE
    ;

// Value to compare against
comparand
    : stringLiteral
    | NUMBER
    ;

// Absolute path starting with / or //
absoluteLocationPath
    : SLASH relativeLocationPath?
    | DOUBLE_SLASH relativeLocationPath
    ;

// Relative path (series of steps)
relativeLocationPath
    : step (pathSeparator step)*
    ;

// Path separator between steps
pathSeparator
    : SLASH
    | DOUBLE_SLASH
    ;

// A single step in the path
step
    : nodeTest predicate*
    | attributeStep predicate*
    | nodeTypeTest
    ;

// Node type test - text(), comment(), node(), processing-instruction()
// Validation of which functions are valid node type tests happens at runtime
nodeTypeTest
    : QNAME LPAREN RPAREN
    ;

// Attribute step (@attr or @*)
attributeStep
    : AT (QNAME | WILDCARD)
    ;

// Node test (element name or wildcard)
nodeTest
    : QNAME
    | WILDCARD
    ;

// Predicate in square brackets
predicate
    : LBRACKET predicateExpr RBRACKET
    ;

// Predicate expression (supports and/or)
predicateExpr
    : orExpr
    ;

// OR expression (lowest precedence)
orExpr
    : andExpr (OR andExpr)*
    ;

// AND expression (higher precedence than OR)
andExpr
    : primaryExpr (AND primaryExpr)*
    ;

// Primary expression in a predicate
primaryExpr
    : functionCall EQUALS stringLiteral       // local-name()='value', text()='value'
    | attributeTest EQUALS stringLiteral      // @attr='value' or @*='value'
    | childElementTest EQUALS stringLiteral   // child='value' or *='value'
    ;

// XPath function call - unified for both top-level and predicate use
functionCall
    : LOCAL_NAME LPAREN RPAREN
    | NAMESPACE_URI LPAREN RPAREN
    | QNAME LPAREN functionArgs? RPAREN
    ;

// Function arguments (comma-separated)
functionArgs
    : functionArg (COMMA functionArg)*
    ;

// A single function argument
// Note: functionCall must come before relativeLocationPath
// because both can start with QNAME, but we need to check for '(' to distinguish them
functionArg
    : absoluteLocationPath
    | functionCall
    | relativeLocationPath
    | stringLiteral
    | NUMBER
    ;

// Attribute test in predicate
attributeTest
    : AT (QNAME | WILDCARD)
    ;

// Child element test in predicate
childElementTest
    : QNAME
    | WILDCARD
    ;

// String literal value
stringLiteral
    : STRING_LITERAL
    ;
