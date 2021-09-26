parser grammar JsonPath;

options { tokenVocab=JsonPathLexer; }

jsonpath
    : ROOT? object+
    ;

object
    : LBRACK expression RBRACK                                  #BracketOperator
    | DOT expression                                            #DotOperator
    | DOT_DOT expression                                        #RecursiveDescent
    | LBRACK expression (COMMA expression) * RBRACK             #UnionOperator
    | rangeOp                                                   #RangeOperator
    ;

rangeOp
    : LBRACK start COLON end? RBRACK
    | LBRACK COLON end? RBRACK
    ;

start
    : NumericLiteral
    ;

end
    : NumericLiteral
    ;

expression
    : expression AND expression                                             #AndExpression
//    | expression OR expression                                              #OrExpression
//    | NOT expression                                                        #NotExpression
    | QUESTION LPAREN expression RPAREN                                     #FilterExpression
    | LPAREN expression RPAREN                                              #ParentheticalExpression
    | litExpression                                                         #LiteralExpression
    | Identifier                                                            #Identifier
    | expression ( EQ | NE | MATCHES ) expression       #BinaryExpression
    | jsonpath                                                              #PathExpression
    | WILDCARD                                                              #WildcardExpression
    | AT object+                                                            #ScopedPathExpression
    ;

litExpression
    : StringLiteral
    | NumericLiteral
    | TRUE
    | FALSE
    | NULL
    ;
