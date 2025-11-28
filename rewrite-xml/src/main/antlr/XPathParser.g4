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
 * - Positional predicates: /root/element[1], /root/element[last()]
 * - Parenthesized expressions with predicates: (/root/element)[1], (/root/a)[last()]
 * - XPath functions: local-name(), namespace-uri(), text(), contains(), position(), last(), etc.
 * - Logical operators in predicates: and, or
 * - Multiple predicates: /root/element[@attr='value'][local-name()='element']
 * - Top-level function expressions: contains(/root/element, 'value')
 * - Boolean expressions: not(contains(...)), string-length(...) > 2
 * - Abbreviated syntax: . (self), .. (parent)
 * - Parent axis: parent::node(), parent::element
 */
parser grammar XPathParser;

options { tokenVocab=XPathLexer; }

// Entry point for XPath expression
xpathExpression
    : booleanExpr
    | filterExpr
    | absoluteLocationPath
    | relativeLocationPath
    ;

// Filter expression - parenthesized path with predicates and optional trailing path: (/root/a)[1]/child
filterExpr
    : LPAREN (absoluteLocationPath | relativeLocationPath) RPAREN predicate+ (pathSeparator relativeLocationPath)?
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
    : axisStep predicate*
    | nodeTest predicate*
    | attributeStep predicate*
    | nodeTypeTest
    | abbreviatedStep
    ;

// Axis step - explicit axis like parent::node()
axisStep
    : axisName AXIS_SEP nodeTest
    ;

// Supported axis names (NCName - no namespace prefix)
axisName
    : NCNAME  // parent, ancestor, self, child, etc. - validated at runtime
    ;

// Abbreviated step - . or ..
abbreviatedStep
    : DOTDOT    // parent::node()
    | DOT       // self::node()
    ;

// Node type test - text(), comment(), node(), processing-instruction()
// Validation of which functions are valid node type tests happens at runtime
nodeTypeTest
    : NCNAME LPAREN RPAREN
    ;

// Attribute step (@attr, @ns:attr, or @*)
attributeStep
    : AT (QNAME | NCNAME | WILDCARD)
    ;

// Node test (element name, ns:element, or wildcard)
nodeTest
    : QNAME
    | NCNAME
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
    : predicateValue comparisonOp comparand            // any value expression with comparison
    | predicateValue                                   // standalone value (last(), position(), number, boolean)
    ;

// A value-producing expression in a predicate
predicateValue
    : functionCall                                     // local-name(), last(), position(), contains(), etc.
    | attributeStep                                    // @attr, @*
    | relativeLocationPath                             // bar/baz/text()
    | childElementTest                                 // child, *
    | NUMBER                                           // positional predicate [1], [2], etc.
    ;

// XPath function call - unified for both top-level and predicate use
// Function names are NCNames (no namespace prefix in standard XPath 1.0)
functionCall
    : LOCAL_NAME LPAREN RPAREN
    | NAMESPACE_URI LPAREN RPAREN
    | NCNAME LPAREN functionArgs? RPAREN
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

// Child element test in predicate (element name, ns:element, or wildcard)
childElementTest
    : QNAME
    | NCNAME
    | WILDCARD
    ;

// String literal value
stringLiteral
    : STRING_LITERAL
    ;
