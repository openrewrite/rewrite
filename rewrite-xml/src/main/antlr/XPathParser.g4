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
 * XPath parser following the XPath 1.0 specification grammar structure.
 * See: https://www.w3.org/TR/1999/REC-xpath-19991116/
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
 * - Logical operators: and, or
 * - Comparison operators: =, !=, <, >, <=, >=
 * - Multiple predicates: /root/element[@attr='value'][local-name()='element']
 * - Abbreviated syntax: . (self), .. (parent)
 * - Axis steps: parent::node(), parent::element
 *
 * Not yet implemented:
 * - Union operator: |
 * - Arithmetic operators: +, -, *, div, mod
 * - Variable references: $var
 */
parser grammar XPathParser;

options { tokenVocab=XPathLexer; }

//==============================================================================
// Entry point
//==============================================================================

// [14] Expr ::= OrExpr
xpathExpression
    : expr
    ;

// Top-level expression - any expression type
expr
    : orExpr
    ;

//==============================================================================
// Expression hierarchy (following XPath 1.0 spec precedence)
//==============================================================================

// [21] OrExpr ::= AndExpr | OrExpr 'or' AndExpr
orExpr
    : andExpr (OR andExpr)*
    ;

// [22] AndExpr ::= EqualityExpr | AndExpr 'and' EqualityExpr
andExpr
    : equalityExpr (AND equalityExpr)*
    ;

// [23] EqualityExpr ::= RelationalExpr | EqualityExpr '=' RelationalExpr | EqualityExpr '!=' RelationalExpr
equalityExpr
    : relationalExpr ((EQUALS | NOT_EQUALS) relationalExpr)*
    ;

// [24] RelationalExpr ::= AdditiveExpr | RelationalExpr '<' AdditiveExpr | ...
// (Skipping AdditiveExpr/MultiplicativeExpr for now - going directly to UnaryExpr)
relationalExpr
    : unaryExpr ((LT | GT | LTE | GTE) unaryExpr)*
    ;

// [27] UnaryExpr ::= UnionExpr | '-' UnaryExpr
// (Skipping '-' for now since we don't have arithmetic)
unaryExpr
    : unionExpr
    ;

// [18] UnionExpr ::= PathExpr | UnionExpr '|' PathExpr
// (Union operator not yet implemented - going directly to PathExpr)
unionExpr
    : pathExpr
    ;

// [19] PathExpr ::= LocationPath | FilterExpr | FilterExpr '/' RelativeLocationPath | FilterExpr '//' RelativeLocationPath
// Restructured to eliminate ambiguity: filterExpr alternatives are made explicit
// The key distinction: function calls require LPAREN after the name, location paths don't
pathExpr
    : functionCallExpr (pathSeparator relativeLocationPath)?  // func() possibly followed by /path
    | bracketedExpr (pathSeparator relativeLocationPath)?     // (expr) possibly followed by /path
    | literalOrNumber (pathSeparator relativeLocationPath)?   // 'string' or 123 possibly followed by /path
    | locationPath                                             // /a/b, a/b, //a, etc.
    ;

// [20] FilterExpr ::= PrimaryExpr | FilterExpr Predicate
// Function call with optional predicates - requires LPAREN after name to disambiguate from locationPath
functionCallExpr
    : functionCall predicate*
    ;

// Bracketed expression with optional predicates
bracketedExpr
    : LPAREN expr RPAREN predicate*
    ;

// Literal or number with optional predicates
literalOrNumber
    : literal predicate*
    | NUMBER predicate*
    ;

// Legacy filterExpr for backward compatibility (used in compileFilterExpr)
filterExpr
    : functionCallExpr
    | bracketedExpr
    | literalOrNumber
    ;

// [15] PrimaryExpr ::= VariableReference | '(' Expr ')' | Literal | Number | FunctionCall
primaryExpr
    : LPAREN expr RPAREN
    | literal
    | NUMBER
    | functionCall
    ;

//==============================================================================
// Location paths
//==============================================================================

// [1] LocationPath ::= RelativeLocationPath | AbsoluteLocationPath
locationPath
    : absoluteLocationPath
    | relativeLocationPath
    ;

// [2] AbsoluteLocationPath ::= '/' RelativeLocationPath? | AbbreviatedAbsoluteLocationPath
absoluteLocationPath
    : SLASH relativeLocationPath?
    | DOUBLE_SLASH relativeLocationPath
    ;

// [3] RelativeLocationPath ::= Step | RelativeLocationPath '/' Step | AbbreviatedRelativeLocationPath
relativeLocationPath
    : step (pathSeparator step)*
    ;

// Path separator between steps
pathSeparator
    : SLASH
    | DOUBLE_SLASH
    ;

//==============================================================================
// Location steps
//==============================================================================

// [4] Step ::= AxisSpecifier NodeTest Predicate* | AbbreviatedStep
// Restructured to eliminate ambiguity: axis specifier is optional prefix
step
    : axisSpecifier? nodeTest predicate*
    | attributeStep predicate*
    | abbreviatedStep
    ;

// [5] AxisSpecifier ::= AxisName '::' | AbbreviatedAxisSpecifier
// Made as a separate rule to allow optional usage in step
axisSpecifier
    : axisName AXIS_SEP
    ;

// [6] AxisName - validated at runtime
axisName
    : NCNAME
    ;

// [12] AbbreviatedStep ::= '.' | '..'
abbreviatedStep
    : DOTDOT
    | DOT
    ;

// [13] AbbreviatedAxisSpecifier ::= '@'?
attributeStep
    : AT (QNAME | NCNAME | WILDCARD)
    ;

//==============================================================================
// Node tests
//==============================================================================

// [7] NodeTest ::= NameTest | NodeType '(' ')' | 'processing-instruction' '(' Literal ')'
nodeTest
    : nameTest
    | nodeType LPAREN RPAREN
    ;

// [37] NameTest ::= '*' | NCName ':' '*' | QName
nameTest
    : WILDCARD
    | QNAME
    | NCNAME
    ;

// [38] NodeType ::= 'comment' | 'text' | 'processing-instruction' | 'node'
// Uses specific tokens to avoid ambiguity with function calls
nodeType
    : TEXT
    | COMMENT
    | NODE
    | PROCESSING_INSTRUCTION
    ;

//==============================================================================
// Predicates
//==============================================================================

// [8] Predicate ::= '[' PredicateExpr ']'
predicate
    : LBRACKET predicateExpr RBRACKET
    ;

// [9] PredicateExpr ::= Expr
predicateExpr
    : expr
    ;

//==============================================================================
// Functions
//==============================================================================

// [16] FunctionCall ::= FunctionName '(' (Argument (',' Argument)*)? ')'
functionCall
    : functionName LPAREN (argument (COMMA argument)*)? RPAREN
    ;

// [35] FunctionName ::= QName - NodeType
// Node type tokens can also be function names (text(), comment(), node(), processing-instruction())
functionName
    : NCNAME
    | TEXT
    | COMMENT
    | NODE
    | PROCESSING_INSTRUCTION
    ;

// [17] Argument ::= Expr
argument
    : expr
    ;

//==============================================================================
// Literals
//==============================================================================

// [29] Literal ::= '"' [^"]* '"' | "'" [^']* "'"
literal
    : STRING_LITERAL
    ;
