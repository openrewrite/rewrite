parser grammar HCLParser;

options { tokenVocab=HCLLexer; }

// Config file
// https://github.com/hashicorp/hcl2/blob/master/hcl/hclsyntax/spec.md#structural-elements

configFile
    : body
    ;

body
    : bodyContent*
    ;

bodyContent
    : attribute
    | block
    ;

attribute
    : (Identifier | NULL) ASSIGN expression
    ;

block
    : Identifier blockLabel* blockExpr
    ;

blockLabel
    : QUOTE stringLiteral QUOTE
    | Identifier
    ;

// Expressions
// https://github.com/hashicorp/hcl2/blob/master/hcl/hclsyntax/spec.md#expressions

expression
    : exprTerm                                          #ExpressionTerm
    | operation                                         #OperationExpression
    | expression QUESTION expression COLON expression   #ConditionalExpression
    ;

exprTerm
    : templateExpr                  #TemplateExpression
    | literalValue                  #LiteralExpression
    // There is a syntax ambiguity between for expressions and collection values whose
    // first element is a reference to a variable named for.
    // The for expression interpretation has priority.
    | forExpr                       #ForExpression
    | collectionValue               #CollectionValueExpression
    | variableExpr                  #VariableExpression
    | functionCall                  #FunctionCallExpression
    | exprTerm index                #IndexAccessExpression
    | exprTerm getAttr              #AttributeAccessExpression
    | exprTerm legacyIndexAttr      #LegacyIndexAttributeExpression
    | exprTerm splat                #SplatExpression
    | LPAREN expression RPAREN      #ParentheticalExpression
    ;

blockExpr
    : LBRACE body RBRACE
    ;

// Literal Values
// https://github.com/hashicorp/hcl2/blob/master/hcl/hclsyntax/spec.md#literal-values

literalValue
    : NumericLiteral
    | BooleanLiteral
    | NULL
    ;

// Collection Values
// https://github.com/hashicorp/hcl2/blob/master/hcl/hclsyntax/spec.md#collection-values

collectionValue
    : tuple
    | object
    ;

tuple
    : LBRACK (expression (COMMA expression)* COMMA?)? RBRACK
    ;

object
    : LBRACE objectelem* RBRACE
    ;

objectelem
    : (qualifiedIdentifier | NULL | LPAREN qualifiedIdentifier RPAREN | QUOTE quotedTemplatePart* QUOTE | expression) (ASSIGN | COLON) expression COMMA?
    ;

qualifiedIdentifier
    : Identifier (DOT Identifier)*
    ;

// For Expressions
// https://github.com/hashicorp/hcl2/blob/master/hcl/hclsyntax/spec.md#for-expressions

forExpr
    : forTupleExpr
    | forObjectExpr
    ;

forTupleExpr
    : FOR_BRACK forIntro COLON expression forCond? RBRACK
    ;

forObjectExpr
    : FOR_BRACE forIntro COLON expression ARROW expression ELLIPSIS? forCond? RBRACE
    ;

forIntro
    : Identifier (COMMA Identifier)? IN expression
    ;

forCond
    : IF expression
    ;

// Variable Expressions
// https://github.com/hashicorp/hcl2/blob/master/hcl/hclsyntax/spec.md#variables-and-variable-expressions

variableExpr
    : Identifier
    ;

// Functions and Function Calls
// https://github.com/hashicorp/hcl2/blob/master/hcl/hclsyntax/spec.md#functions-and-function-calls

functionCall
    : Identifier LPAREN arguments? RPAREN;

arguments
    : expression (COMMA expression)* (COMMA | ELLIPSIS)?
    ;

// Index Operator
// https://github.com/hashicorp/hcl2/blob/master/hcl/hclsyntax/spec.md#index-operator

index
    : LBRACK expression RBRACK
    ;

// Attribute Access Operator
// https://github.com/hashicorp/hcl2/blob/master/hcl/hclsyntax/spec.md#attribute-access-operator

getAttr
    : DOT Identifier
    ;

// Legacy Index Access Operator
// https://github.com/hashicorp/hcl/blob/923b06b5d6adf61a647101c605f7463a1bd56cbf/hclsyntax/spec.md#L547
// Interestingly not mentioned in hcl2 syntax specification anymore

legacyIndexAttr
    : DOT NumericLiteral
    ;

// Splat Operators
// https://github.com/hashicorp/hcl2/blob/master/hcl/hclsyntax/spec.md#splat-operators

splat
    : attrSplat
    | fullSplat
    ;

attrSplat
    : DOT MUL getAttr*
    ;

fullSplat
    : LBRACK MUL RBRACK (getAttr | index)*;

// Operations
// https://github.com/hashicorp/hcl2/blob/master/hcl/hclsyntax/spec.md#operations

operation
    : unaryOp
    | binaryOp
    ;

unaryOp
    : (MINUS | NOT) exprTerm
    ;

binaryOp
    : (exprTerm | unaryOp) binaryOperator (exprTerm | operation)
    ;

binaryOperator
    : compareOperator
    | arithmeticOperator
    | logicOperator
    ;

compareOperator : (EQ | NEQ | LT | GT | LEQ | GEQ);
arithmeticOperator : PLUS | MINUS | MUL | DIV | MOD;
logicOperator : AND | OR;

// Template Expressions
// https://github.com/hashicorp/hcl2/blob/master/hcl/hclsyntax/spec.md#template-expressions

templateExpr
    : HEREDOC_START Identifier (NEWLINE heredocTemplatePart*)+ Identifier             #Heredoc
    | QUOTE quotedTemplatePart* QUOTE                                                 #QuotedTemplate
    ;

heredocTemplatePart
    : templateInterpolation
    | heredocLiteral
    ;

heredocLiteral
    : HTemplateLiteral
    ;

quotedTemplatePart
    : templateInterpolation
    | stringLiteral
    ;

stringLiteral
    : TemplateStringLiteral
    ;

templateInterpolation
    : TEMPLATE_INTERPOLATION_START expression RBRACE;
