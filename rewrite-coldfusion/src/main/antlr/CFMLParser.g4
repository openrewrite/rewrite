parser grammar CFMLParser;

options {
    tokenVocab = CFMLLexer;
    contextSuperClass = CompactContext;
}

@parser::header {
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
}

@parser::members {
    // Most nodes have one child. Under the JVM's default compressed references, room for two takes no
    // more memory than room for one.
    public static class CompactContext extends ParserRuleContext {
        public CompactContext() {
        }

        public CompactContext(ParserRuleContext parent, int invokingStateNumber) {
            super(parent, invokingStateNumber);
        }

        @Override
        public <T extends ParseTree> T addAnyChild(T t) {
            if (children == null) {
                children = new ArrayList<>(2);
            }
            return super.addAnyChild(t);
        }
    }

    // Tags written as script statements that take an expression rather than attributes.
    private static final Set<String> EXPRESSION_SCRIPT_TAGS = new HashSet<String>(Arrays.asList(
            "abort", "exit", "include", "param", "pageencoding", "rethrow", "retry", "flush"));

    private static final Set<String> RESERVED = new HashSet<String>(Arrays.asList(
            "if", "else", "for", "while", "do", "switch", "case", "default", "try", "catch", "finally", "return",
            "break", "continue", "throw", "var", "function", "component", "interface", "property", "import", "new",
            "in", "true", "false", "null", "not", "and", "or", "public", "private", "package", "remote", "static",
            "final", "abstract"));

    private static final BitSet WORDS = new BitSet();

    static {
        for (int type : new int[]{IDENTIFIER, TYPE_WORD, SCRIPT_TAG_NAME, EXPRESSION_TAG_NAME, ATTRIBUTE_WORD, AND, OR, NOT, XOR, EQV, IMP, MOD, EQ, NEQ,
                IS, EQUAL, GT, GTE, GE, LT, LTE, LE, CONTAINS, ELSE, ELSEIF, CATCH, FINALLY, IN, TRUE, FALSE, NULL,
                IF, FOR, WHILE, SWITCH, DO, TRY, FUNCTION, VAR, NEW, RETURN, BARE_RETURN, BREAK, CONTINUE, THROW,
                CASE, DEFAULT, REQUIRED, COMPONENT, INTERFACE, ABSTRACT, FINAL, PUBLIC, PRIVATE, PACKAGE, REMOTE,
                STATIC, PROPERTY, IMPORT}) {
            WORDS.set(type);
        }
    }

    private Token lt(int i) {
        return _input.LT(i);
    }

    private int la(int i) {
        return _input.LA(i);
    }

    private static String lower(Token token) {
        return token.getText().toLowerCase(Locale.ROOT);
    }

    private static boolean isWord(Token token) {
        return token.getType() > 0 && WORDS.get(token.getType());
    }

    private void retype(int i, int type) {
        ((WritableToken) lt(i)).setType(type);
    }

    // The whitespace and comments right before LT(i).
    private List<Token> trivia(int i) {
        List<Token> hidden = ((BufferedTokenStream) _input).getHiddenTokensToLeft(lt(i).getTokenIndex());
        return hidden == null ? Collections.<Token>emptyList() : hidden;
    }

    private boolean adjacent(int i) {
        return trivia(i).isEmpty();
    }

    private boolean onlyWhitespaceBefore(int i) {
        for (Token token : trivia(i)) {
            if (token.getType() != WS) {
                return false;
            }
        }
        return true;
    }

    // Whether the whitespace before LT(i), up to any comment, holds no line break.
    private boolean noLineBreakBefore(int i) {
        for (Token token : trivia(i)) {
            if (token.getType() != WS) {
                return true;
            }
            if (token.getText().indexOf('\n') >= 0) {
                return false;
            }
        }
        return true;
    }

    protected boolean noLineBreakBefore() {
        return noLineBreakBefore(1);
    }

    // In script, a call, an index or a postfix operator on the next line, or after a comment, starts a
    // new statement. In a tag's expression it continues the expression.
    protected boolean sameLine() {
        for (Token token : trivia(1)) {
            if (token.getChannel() == Token.HIDDEN_CHANNEL &&
                (token.getType() != WS || token.getText().indexOf('\n') >= 0)) {
                return false;
            }
        }
        return true;
    }

    // `i++ b` is not an increment of i.
    protected boolean operandAdjacent() {
        int next = la(2);
        return adjacent(2) && (isWord(lt(2)) || next == NUMBER || next == LPAREN || next == CALL_LPAREN ||
                               next == DQ_OPEN || next == SQ_OPEN);
    }

    private boolean attributeAt(int i) {
        if (!isWord(lt(i))) {
            return false;
        }
        i++;
        while ((la(i) == MINUS || la(i) == COLON) && adjacent(i) && adjacent(i + 1) &&
               (isWord(lt(i + 1)) || la(i + 1) == NUMBER)) {
            i += 2;
        }
        return la(i) == ASSIGN && onlyWhitespaceBefore(i);
    }

    // `name=value`, as an attribute of a component, function, property or tag written in script.
    protected boolean attributeAhead() {
        return attributeAt(1);
    }

    // `data-name`, a name with dashes or colons in it.
    protected boolean adjacentPair() {
        return (la(1) == MINUS || la(1) == COLON) && adjacent(1) && adjacent(2) && (isWord(lt(2)) || la(2) == NUMBER);
    }

    // An attribute with no value. On a new line it starts the next statement, unless a semicolon
    // follows it as in `property name="x"\n    delegate;`.
    protected boolean bareAttribute() {
        return isWord(lt(1)) && (noLineBreakBefore(1) || la(2) == SEMI && onlyWhitespaceBefore(2));
    }

    // Gives the first word of the attribute ahead, if there is one, a token type of its own, so that
    // a loop over attributes need not look past them to see where its statement ends.
    protected void markAttribute(boolean named) {
        if (la(1) != ATTRIBUTE_WORD && (attributeAhead() || !named && bareAttribute())) {
            retype(1, ATTRIBUTE_WORD);
        } else if (!named && la(1) == COMMA && la(2) != ATTRIBUTE_WORD && attributeAt(2)) {
            // the engines take a comma between the attributes of a tag written as a statement
            retype(2, ATTRIBUTE_WORD);
        }
    }

    // An attribute written after a parameter, on its line: `required string path hint="..."`.
    protected boolean parameterAttribute() {
        for (Token token : trivia(1)) {
            if (token.getType() != WS || token.getText().indexOf('\n') >= 0) {
                return false;
            }
        }
        return attributeAhead();
    }

    // A word of `property string name`, which the property's attributes follow.
    protected boolean propertyWord() {
        return isWord(lt(1)) && !attributeAhead() && noLineBreakBefore(1);
    }

    // `include "header.cfm";` and `param rc.sort = "name";` take an expression.
    private boolean expressionForm() {
        if (!EXPRESSION_SCRIPT_TAGS.contains(lower(lt(1)))) {
            return false;
        }
        int next = la(2);
        if (next == EOF || next == LBRACE || next == SEMI || next == RBRACE || next == CFSCRIPT_END) {
            return false;
        }
        return !attributeAt(2) && noLineBreakBefore(2);
    }

    // The body of a tag written as a statement, `lock name="x" { }`.
    protected void bodyBrace() {
        if (la(1) == LBRACE) {
            retype(1, BODY_LBRACE);
        }
    }

    // A brace that opens a block, where a struct literal could otherwise be read.
    protected void blockBrace() {
        if (la(1) == LBRACE) {
            retype(1, BLOCK_LBRACE);
        }
    }

    // `cfloop(array=items, index="i") { }`, a tag written as a call with a body on its line.
    protected void tagCallBrace(ExpressionContext expression) {
        if (la(1) == LBRACE && noLineBreakBefore(1) && isMethodCall(expression)) {
            retype(1, BODY_LBRACE);
        }
    }

    private static boolean isMethodCall(ParserRuleContext ctx) {
        ParseTree node = ctx;
        while (!(node instanceof PostfixExpressionContext)) {
            if (!(node instanceof ParserRuleContext) || node.getChildCount() != 1) {
                return false;
            }
            node = node.getChild(0);
        }
        PostfixExpressionContext postfix = (PostfixExpressionContext) node;
        List<PostfixOperatorContext> operators = postfix.postfixOperator();
        int last = operators.size() - 1;
        if (last < 0 || operators.get(last).arguments() == null) {
            return false;
        }
        return last == 0 ? postfix.primary() instanceof IdentifierPrimaryContext : operators.get(last - 1).memberName() != null;
    }

    /**
     * Words that start a statement are names to the lexer: `component`, `property` or `lock` may also
     * name variables. At the start of a statement this decides what the first word is, and gives it a
     * token type of its own so that every statement starts with a token only it can start with.
     */
    protected void classifyStatement() {
        Token first = lt(1);
        if (first.getType() == LBRACE) {
            // a statement that starts with a brace is a block
            retype(1, BLOCK_LBRACE);
            return;
        }
        if (first.getType() != IDENTIFIER) {
            return;
        }
        String word = lower(first);
        if (("component".equals(word) || "interface".equals(word) || "abstract".equals(word) || "final".equals(word)) &&
            componentAhead()) {
            return;
        }
        if ("property".equals(word) && isWord(lt(2))) {
            retype(1, PROPERTY);
        } else if ("import".equals(word) && isWord(lt(2))) {
            retype(1, IMPORT);
        } else if ("static".equals(word) && la(2) == LBRACE) {
            retype(1, STATIC);
        } else if (!functionHeader() && scriptTagAhead()) {
            retype(1, expressionForm() ? EXPRESSION_TAG_NAME : SCRIPT_TAG_NAME);
        }
    }

    // `abstract component extends="x" {`.
    private boolean componentAhead() {
        int k = 1;
        while ("abstract".equals(lower(lt(k))) || "final".equals(lower(lt(k)))) {
            k++;
        }
        String keyword = lower(lt(k));
        if (!"component".equals(keyword) && !"interface".equals(keyword) ||
            la(k + 1) != LBRACE && !isWord(lt(k + 1))) {
            return false;
        }
        for (int i = 1; i < k; i++) {
            retype(i, "abstract".equals(lower(lt(i))) ? ABSTRACT : FINAL);
        }
        retype(k, "component".equals(keyword) ? COMPONENT : INTERFACE);
        return true;
    }

    // `public static string function name(`: the modifiers, then the return type.
    private boolean functionHeader() {
        int k = 1;
        int function = -1;
        for (int words = 0; words < 8; words++) {
            if (la(k) == FUNCTION && isWord(lt(k + 1)) && (la(k + 2) == LPAREN || la(k + 2) == CALL_LPAREN)) {
                function = k;
                break;
            }
            if (!isWord(lt(k))) {
                return false;
            }
            k++;
            while (la(k) == DOT && adjacent(k) && isWord(lt(k + 1)) && adjacent(k + 1)) {
                k += 2;
            }
            if ((la(k) == LBRACKET || la(k) == INDEX_LBRACKET) && la(k + 1) == RBRACKET && adjacent(k) && adjacent(k + 1)) {
                k += 2;
            }
            if (adjacent(k)) {
                return false;
            }
        }
        if (function < 0) {
            return false;
        }
        int i = modifiers(1, function);
        if (i < function) {
            int last = i;
            while (la(last + 1) == DOT && isWord(lt(last + 2))) {
                last += 2;
            }
            retype(last, TYPE_WORD);
            // the engines take a return type before the access modifier too, as `boolean public function f()`
            i = last + 1;
            if ((la(i) == LBRACKET || la(i) == INDEX_LBRACKET) && la(i + 1) == RBRACKET) {
                i += 2;
            }
            modifiers(i, function);
        }
        return true;
    }

    // Retypes the modifiers from token i on, before the function keyword at token function, and returns where they end.
    private int modifiers(int i, int function) {
        while (i < function && la(i) == IDENTIFIER) {
            int modifier = modifier(lower(lt(i)));
            if (modifier < 0) {
                break;
            }
            retype(i, modifier);
            i++;
        }
        return i;
    }

    private static int modifier(String word) {
        switch (word) {
            case "public":
                return PUBLIC;
            case "private":
                return PRIVATE;
            case "package":
                return PACKAGE;
            case "remote":
                return REMOTE;
            case "static":
                return STATIC;
            case "final":
                return FINAL;
            case "abstract":
                return ABSTRACT;
            default:
                return -1;
        }
    }

    // `lock name="x" { }`, `abort;`, `include "x.cfm";`: a tag written as a statement.
    private boolean scriptTagAhead() {
        String word = lower(lt(1));
        if ("throw".equals(word)) {
            return la(2) != LPAREN && la(2) != CALL_LPAREN;
        }
        if (RESERVED.contains(word)) {
            return false;
        }
        List<Token> gap = trivia(2);
        for (Token token : gap) {
            if (token.getType() != WS) {
                return false;
            }
        }
        int next = la(2);
        if (next == LBRACE) {
            return true;
        }
        if (EXPRESSION_SCRIPT_TAGS.contains(word)) {
            if (next == SEMI || next == RBRACE || next == DQ_OPEN || next == SQ_OPEN || next == EOF) {
                return true;
            }
            return !gap.isEmpty() && isWord(lt(2)) && gap.get(0).getText().indexOf('\n') < 0;
        }
        return !gap.isEmpty() && attributeAt(2);
    }
}

// =============================================================================================
// Files
// =============================================================================================

template
    : templateElement* EOF
    ;

scriptComponent
    : paddedStatement* EOF
    ;

templateElement
    : TEXT              # textElement
    | TEMPLATE_COMMENT  # commentElement
    | interpolation     # interpolationElement
    | startTag          # startTagElement
    | cfscriptTag       # cfscriptElement
    | endTag            # endTagElement
    ;

startTag
    : START_TAG_OPEN tagAttribute* (TAG_CLOSE | TAG_SELF_CLOSE)
    | TAG_EXPRESSION_OPEN expression? (TAG_CLOSE | TAG_SELF_CLOSE)
    ;

cfscriptTag
    : CFSCRIPT_OPEN tagAttribute* (TAG_SELF_CLOSE | TAG_CLOSE paddedStatement* CFSCRIPT_END END_TAG_CLOSE)
    ;

endTag
    : END_TAG_OPEN END_TAG_CLOSE
    ;

tagAttribute
    : ATTRIBUTE_NAME (TAG_EQUALS attributeValue)?
    ;

attributeValue
    : stringLiteral
    | (UNQUOTED_TEXT | interpolation)+
    ;

interpolation
    : HASH expression HASH_CLOSE
    ;

// =============================================================================================
// Statements
// =============================================================================================

paddedStatement
    : { classifyStatement(); } (functionDeclaration | statement SEMI? | SEMI)
    ;

statement
    : block
    | ifStatement
    | forInStatement
    | forStatement
    | whileStatement
    | doWhileStatement
    | switchStatement
    | tryStatement
    | returnStatement
    | breakStatement
    | continueStatement
    | throwStatement
    | componentDeclaration
    | propertyDeclaration
    | importStatement
    | staticInitializer
    | scriptTag
    | expressionStatement
    ;

block
    : (LBRACE | BLOCK_LBRACE) paddedStatement* RBRACE
    ;

bodyBlock
    : BODY_LBRACE paddedStatement* RBRACE
    ;

ifStatement
    : IF ifTail
    ;

// An else is matched in one place only, so that SLL prediction sees at once that it may belong to
// this if or to an enclosing one.
ifTail
    : LPAREN expression RPAREN paddedStatement elseClause?
    ;

elseClause
    : ELSE paddedStatement
    | ELSEIF ifTail
    ;

forInStatement
    : FOR LPAREN forInVariable IN expression RPAREN paddedStatement
    ;

forInVariable
    : VAR anyWord
    | anyWord (DOT anyWord)*
    ;

forStatement
    : FOR LPAREN forClauses SEMI expression? SEMI forClauses RPAREN paddedStatement
    ;

forClauses
    : expression? (COMMA expression?)*
    ;

whileStatement
    : WHILE LPAREN expression RPAREN paddedStatement
    ;

doWhileStatement
    : DO block SEMI? WHILE LPAREN expression RPAREN
    ;

switchStatement
    : SWITCH LPAREN expression RPAREN LBRACE caseClause* RBRACE
    ;

caseClause
    : (CASE ternaryExpression | DEFAULT) COLON paddedStatement*
    ;

tryStatement
    : TRY block catchClause* (FINALLY block)?
    ;

catchClause
    : CATCH LPAREN catchParameter RPAREN block
    ;

catchParameter
    : VAR anyWord
    | catchType VAR anyWord
    | catchType anyWord
    | anyWord
    ;

catchType
    : stringLiteral
    | anyWord (DOT anyWord)*
    ;

returnStatement
    : RETURN expression
    | BARE_RETURN
    ;

// A label names the loop to leave, as `loop label="outer"` declares it.
breakStatement
    : BREAK ({ noLineBreakBefore() }? IDENTIFIER)?
    ;

continueStatement
    : CONTINUE ({ noLineBreakBefore() }? IDENTIFIER)?
    ;

throwStatement
    : THROW expression
    ;

componentDeclaration
    : (ABSTRACT | FINAL)* (COMPONENT | INTERFACE) headerAttribute* block
    ;

propertyDeclaration
    : PROPERTY ({ propertyWord() }? dottedName ({ propertyWord() }? anyWord)?)? { markAttribute(false); }
      (statementAttribute { markAttribute(false); })*
    ;

importStatement
    : IMPORT anyWord (DOT (anyWord | STAR))*
    ;

staticInitializer
    : STATIC block
    ;

scriptTag
    : SCRIPT_TAG_NAME { markAttribute(false); } (COMMA? statementAttribute { markAttribute(false); })* { bodyBrace(); } bodyBlock?
    | EXPRESSION_TAG_NAME expression { markAttribute(true); } (statementAttribute { markAttribute(true); })* { bodyBrace(); }
      bodyBlock?
    ;

expressionStatement
    : expression { tagCallBrace($expression.ctx); } bodyBlock?
    ;

// A function without a body ends with a semicolon. Letting it end with nothing would make every
// function body a candidate for a block statement after it, and prediction would scan each one.
functionDeclaration
    : functionModifier* (returnType functionModifier*)? FUNCTION anyWord (LPAREN | CALL_LPAREN) parameters? RPAREN
      headerAttribute* (block SEMI? | SEMI)
    ;

functionModifier
    : PUBLIC
    | PRIVATE
    | PACKAGE
    | REMOTE
    | STATIC
    | FINAL
    | ABSTRACT
    ;

returnType
    : (anyWord DOT)* TYPE_WORD ((LBRACKET | INDEX_LBRACKET) RBRACKET)?
    ;

// The parameters of a closure or an arrow function, which start where an operand would.
parameterList
    : LPAREN parameters? RPAREN
    ;

parameters
    : parameter (COMMA? parameter)* COMMA?
    ;

parameter
    : REQUIRED? parameterType? anyWord (ASSIGN ternaryExpression)? ({ parameterAttribute() }? namedAttribute)*
    ;

parameterType
    : dottedName ((LBRACKET | INDEX_LBRACKET) RBRACKET)?
    ;

// Attributes of a component, function or closure, before its body: any word is one.
headerAttribute
    : attributeName (ASSIGN ternaryExpression)?
    ;

// An attribute of a property or a tag written as a statement, whose first word the parser marks.
statementAttribute
    : ATTRIBUTE_WORD ({ adjacentPair() }? (MINUS | COLON) (anyWord | NUMBER))* (ASSIGN ternaryExpression)?
    ;

namedAttribute
    : attributeName ASSIGN ternaryExpression
    ;

attributeName
    : anyWord ({ adjacentPair() }? (MINUS | COLON) (anyWord | NUMBER))*
    ;

// =============================================================================================
// Expressions
// =============================================================================================

expression
    : ternaryExpression (assignmentOperator expression)?
    ;

assignmentOperator
    : ASSIGN
    | PLUS_ASSIGN
    | MINUS_ASSIGN
    | STAR_ASSIGN
    | SLASH_ASSIGN
    | MOD_ASSIGN
    | CONCAT_ASSIGN
    ;

ternaryExpression
    : operation (ELVIS ternaryExpression | QUESTION ternaryExpression COLON ternaryExpression)?
    ;

// The binary operators and the loose NOT, from the tightest binding to the loosest. NOT binds more loosely
// than comparison in CFML, so NOT a EQ b is NOT (a EQ b).
operation
    : unaryExpression
    | <assoc=right> operation CARET operation
    | operation (STAR | SLASH) operation
    | operation BACKSLASH operation
    | operation (PERCENT | MOD) operation
    | operation (PLUS | MINUS) operation
    | operation AMP operation
    | operation comparisonOperator operation
    | (BANG | NOT) operation
    | operation (AMPAMP | AND) operation
    | operation (PIPEPIPE | OR) operation
    | operation XOR operation
    | operation EQV operation
    | operation IMP operation
    ;

comparisonOperator
    : GREATER_THAN_OR_EQUAL_TO
    | LESS_THAN_OR_EQUAL_TO
    | GREATER_THAN
    | LESS_THAN
    | DOES_NOT_CONTAIN
    | IS_NOT
    | NOT_EQUAL
    | NEQ
    | CONTAINS
    | GTE
    | GE
    | LTE
    | LE
    | GT
    | LT
    | EQ
    | IS
    | EQUAL
    | TEQ
    | TNEQ
    | EQEQ
    | NEQ_SIGN
    | LTGT
    | GTE_SIGN
    | LTE_SIGN
    | GT_SIGN
    | LT_SIGN
    ;

unaryExpression
    : (INCR | DECR | UNARY_MINUS | UNARY_PLUS | TIGHT_BANG | TIGHT_NOT) unaryExpression
    | postfixExpression
    ;

postfixExpression
    : primary postfixOperator*
    ;

// The lexer tells a call's `(` and an index's `[` from those that start a new statement.
postfixOperator
    : (DOT | SAFE_DOT | DOUBLE_COLON) memberName
    | arguments
    | INDEX_LBRACKET expression RBRACKET
    | { sameLine() && !operandAdjacent() }? (INCR | DECR)
    ;

memberName
    : IDENTIFIER
    | NUMBER
    ;

arguments
    : CALL_LPAREN argumentList? RPAREN
    ;

// Named arguments may leave out the comma between them, as in a tag written as a call.
argumentList
    : argument (COMMA argument | { attributeAhead() }? argument)* COMMA?
    ;

argument
    : (anyWord | stringLiteral) (ASSIGN | COLON) ternaryExpression # namedArgument
    | ternaryExpression                                           # positionalArgument
    ;

primary
    : NUMBER                                                                # numberPrimary
    | (TRUE | FALSE)                                                        # booleanPrimary
    | NULL                                                                  # nullPrimary
    | stringLiteral                                                         # stringPrimary
    | interpolation                                                         # interpolationPrimary
    | arrowFunction                                                         # arrowFunctionPrimary
    | LPAREN expression RPAREN                                              # parenthesizedPrimary
    | LBRACKET (COLON | ASSIGN)? RBRACKET                                   # emptyBracketPrimary
    | LBRACKET structEntry (COMMA structEntry)* COMMA? RBRACKET             # orderedStructPrimary
    | LBRACKET ternaryExpression (COMMA ternaryExpression)* COMMA? RBRACKET # arrayPrimary
    | LBRACE RBRACE                                                         # emptyStructPrimary
    | LBRACE structEntry (COMMA structEntry)* COMMA? RBRACE                 # structPrimary
    | closure                                                               # closurePrimary
    | NEW typeName                                                          # newPrimary
    | VAR expression                                                        # varPrimary
    | identifier                                                            # identifierPrimary
    ;

structEntry
    : structKey (COLON | ASSIGN) expression
    ;

structKey
    : stringLiteral
    | (anyWord | NUMBER) (DOT anyWord)*
    ;

closure
    : FUNCTION parameterList headerAttribute* block
    ;

arrowFunction
    : parameterList ARROW { blockBrace(); } arrowBody
    | anyWord ARROW { blockBrace(); } arrowBody
    ;

arrowBody
    : block
    | RETURN expression
    | BARE_RETURN
    | expression
    ;

typeName
    : stringLiteral
    | dottedName
    ;

dottedName
    : anyWord (DOT anyWord)*
    ;

stringLiteral
    : DQ_OPEN (DQ_TEXT | interpolation)* DQ_CLOSE
    | SQ_OPEN (SQ_TEXT | interpolation)* SQ_CLOSE
    ;

// The lexer makes a keyword a name wherever it cannot be the keyword.
identifier
    : IDENTIFIER
    ;

anyWord
    : IDENTIFIER
    | TYPE_WORD
    | SCRIPT_TAG_NAME
    | AND
    | OR
    | NOT
    | XOR
    | EQV
    | IMP
    | MOD
    | EQ
    | NEQ
    | IS
    | EQUAL
    | GT
    | GTE
    | GE
    | LT
    | LTE
    | LE
    | CONTAINS
    | ELSE
    | ELSEIF
    | CATCH
    | FINALLY
    | IN
    | TRUE
    | FALSE
    | NULL
    | IF
    | FOR
    | WHILE
    | SWITCH
    | DO
    | TRY
    | FUNCTION
    | VAR
    | NEW
    | RETURN
    | BARE_RETURN
    | BREAK
    | CONTINUE
    | THROW
    | CASE
    | DEFAULT
    | REQUIRED
    | COMPONENT
    | INTERFACE
    | ABSTRACT
    | FINAL
    | PUBLIC
    | PRIVATE
    | PACKAGE
    | REMOTE
    | STATIC
    | PROPERTY
    | IMPORT
    | EXPRESSION_TAG_NAME
    | ATTRIBUTE_WORD
    ;
