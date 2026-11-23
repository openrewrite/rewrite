lexer grammar CFMLLexer;

options {
    caseInsensitive = true;
}

// Whitespace and comments in a tag's expression, outside any brackets, where a call or an index
// may continue on the next line.
channels {
    TAG_HIDDEN
}

// Token types set by actions here, or by the parser when it classifies a statement's first word.
tokens {
    TAG_EXPRESSION_OPEN,
    CFSCRIPT_OPEN,
    HASH_CLOSE,
    BARE_RETURN,
    COMPONENT,
    INTERFACE,
    ABSTRACT,
    FINAL,
    PUBLIC,
    PRIVATE,
    PACKAGE,
    REMOTE,
    STATIC,
    PROPERTY,
    IMPORT,
    TYPE_WORD,
    SCRIPT_TAG_NAME,
    EXPRESSION_TAG_NAME,
    BODY_LBRACE,
    BLOCK_LBRACE,
    CALL_LPAREN,
    INDEX_LBRACKET,
    UNARY_PLUS,
    UNARY_MINUS,
    TIGHT_BANG,
    TIGHT_NOT,
    ATTRIBUTE_WORD
}

@lexer::header {
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
}

@lexer::members {
    public static final Set<String> EXPRESSION_TAGS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            "cfset", "cfif", "cfelseif", "cfreturn")));

    // Tags that never take a body. Others are matched with their end tag, if one follows.
    public static final Set<String> BODYLESS_TAGS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            "cfset", "cfelse", "cfelseif", "cfreturn", "cfargument", "cfproperty", "cfparam", "cfinclude",
            "cfabort", "cfbreak", "cfcontinue", "cfqueryparam", "cfprocparam", "cfprocresult",
            "cfinvokeargument", "cfhttpparam", "cfmailparam", "cflocation", "cfthrow", "cfrethrow", "cfdump",
            "cfsetting", "cfheader", "cfcookie", "cfcontent", "cflog", "cfflush", "cfexit", "cfimport",
            "cfdirectory", "cffile", "cfschedule", "cfhtmlhead", "cfassociate", "cfobject", "cfinput",
            "cfchartdata", "cfzipparam", "cfapplication", "cfcollection", "cfindex", "cfwddx", "cfexecute",
            "cftrace", "cfsleep")));

    // Tags whose bodies evaluate #expressions# in their text.
    public static final Set<String> INTERPOLATING_TAGS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            "cfoutput", "cfquery", "cfmail")));

    // What DEFAULT_MODE is lexing.
    private static final int SCRIPT = 0;
    private static final int CFSCRIPT = 1;
    private static final int TAG_EXPRESSION = 2;
    private static final int POUNDS = 3;

    private static final class Context {
        final int kind;
        int depth;
        // for each open brace, whether it opened a struct literal rather than a block
        final Deque<Boolean> braces = new ArrayDeque<Boolean>();

        Context(int kind) {
            this.kind = kind;
        }
    }

    private final Deque<Context> contexts = new ArrayDeque<Context>();
    private final Deque<String> openTags = new ArrayDeque<String>();
    private int interpolating;
    private String tagName = "";
    private int lastType = -1;
    private boolean structClosed;
    // a line break or a comment since the last token, outside a tag's expression
    private boolean broken;

    /**
     * Lex a script component, which is script from its first character.
     */
    public void script() {
        contexts.push(new Context(SCRIPT));
    }

    /**
     * Lex a template, whose text holds tags.
     */
    public void template() {
        mode(TEMPLATE);
    }

    @Override
    public Token nextToken() {
        Token token = super.nextToken();
        if (token.getChannel() == DEFAULT_TOKEN_CHANNEL) {
            lastType = token.getType();
            broken = false;
        } else if (token.getChannel() == HIDDEN && (token.getType() != WS || token.getText().indexOf('\n') >= 0)) {
            broken = true;
        }
        return token;
    }

    private void enter(int kind) {
        contexts.push(new Context(kind));
        pushMode(DEFAULT_MODE);
    }

    private void exit() {
        contexts.pop();
        popMode();
    }

    private boolean in(int kind) {
        Context context = contexts.peek();
        return context != null && context.kind == kind;
    }

    private boolean outsideBrackets(int kind) {
        Context context = contexts.peek();
        return context != null && context.kind == kind && context.depth == 0;
    }

    private void open(boolean brace) {
        Context context = contexts.peek();
        if (context != null) {
            context.depth++;
            if (brace) {
                context.braces.push(expectsOperand());
            }
        }
    }

    private void close(boolean brace) {
        Context context = contexts.peek();
        if (context != null) {
            if (context.depth > 0) {
                context.depth--;
            }
            if (brace) {
                structClosed = !context.braces.isEmpty() && context.braces.pop();
            }
        }
    }

    // Whether an operand must come next, so that a brace opens a struct literal.
    private boolean expectsOperand() {
        switch (lastType) {
            case ASSIGN: case PLUS_ASSIGN: case MINUS_ASSIGN: case STAR_ASSIGN: case SLASH_ASSIGN: case MOD_ASSIGN:
            case CONCAT_ASSIGN: case COLON: case COMMA: case LPAREN: case CALL_LPAREN: case LBRACKET:
            case INDEX_LBRACKET: case RETURN: case QUESTION: case ELVIS: case HASH: case AMP: case PLUS: case MINUS:
            case UNARY_PLUS: case UNARY_MINUS: case STAR: case SLASH: case PERCENT: case MOD: case BACKSLASH:
            case CARET: case AND: case OR: case NOT: case BANG: case TIGHT_BANG: case TIGHT_NOT: case XOR: case EQV:
            case IMP: case AMPAMP: case PIPEPIPE: case IN: case TAG_EXPRESSION_OPEN: case EQ: case NEQ: case IS:
            case EQUAL: case GT: case GTE: case GE: case LT: case LTE: case LE: case CONTAINS:
            case GREATER_THAN_OR_EQUAL_TO: case LESS_THAN_OR_EQUAL_TO: case GREATER_THAN: case LESS_THAN:
            case DOES_NOT_CONTAIN: case IS_NOT: case NOT_EQUAL: case TEQ: case TNEQ: case EQEQ: case NEQ_SIGN: case LTGT:
            case GTE_SIGN: case LTE_SIGN: case GT_SIGN: case LT_SIGN:
                return true;
            default:
                return false;
        }
    }

    // `//` and `/*` start comments in script and in a block, not in a tag or its #...#.
    private boolean comments() {
        for (Context context : contexts) {
            if (context.kind == SCRIPT || context.kind == CFSCRIPT || !context.braces.isEmpty()) {
                return true;
            }
            if (context.kind == TAG_EXPRESSION) {
                return false;
            }
        }
        return false;
    }

    private void hidden() {
        setChannel(outsideBrackets(TAG_EXPRESSION) ? TAG_HIDDEN : HIDDEN);
    }

    private void startTag() {
        tagName = getText().substring(1).toLowerCase(Locale.ROOT);
        if (EXPRESSION_TAGS.contains(tagName)) {
            setType(TAG_EXPRESSION_OPEN);
            enter(TAG_EXPRESSION);
        } else {
            if ("cfscript".equals(tagName)) {
                setType(CFSCRIPT_OPEN);
            }
            pushMode(TAG);
        }
    }

    // A start tag ended with `>`, once the lexer has left the tag.
    private void startTagEnded() {
        if ("cfscript".equals(tagName)) {
            enter(CFSCRIPT);
        } else if (!BODYLESS_TAGS.contains(tagName)) {
            openTags.push(tagName);
            if (INTERPOLATING_TAGS.contains(tagName)) {
                interpolating++;
            }
        }
    }

    // An end tag closes its start tag and any tags left open inside it, as the parser folds them.
    private void endTag() {
        String name = getText().substring(2).toLowerCase(Locale.ROOT);
        if (openTags.contains(name)) {
            while (true) {
                String open = openTags.pop();
                if (INTERPOLATING_TAGS.contains(open)) {
                    interpolating--;
                }
                if (open.equals(name)) {
                    break;
                }
            }
        }
        pushMode(END_TAG);
    }

    private void endCfscript() {
        exit();
        pushMode(END_TAG);
    }

    private void greaterThan() {
        if (outsideBrackets(TAG_EXPRESSION)) {
            setType(TAG_CLOSE);
            exit();
            startTagEnded();
        }
    }

    // `#` interpolates only in the bodies of <cfoutput>, <cfquery> and <cfmail>. Elsewhere it is text.
    private void templateHash() {
        if (interpolating > 0) {
            setType(HASH);
            enter(POUNDS);
        } else {
            setType(TEXT);
        }
    }

    private void hash() {
        if (outsideBrackets(POUNDS)) {
            setType(HASH_CLOSE);
            exit();
        } else {
            enter(POUNDS);
        }
    }

    private boolean afterMember() {
        return lastType == DOT || lastType == SAFE_DOT || lastType == DOUBLE_COLON;
    }

    // Whether the last token ended an operand, so that what follows is an operator.
    private boolean afterOperandEnd() {
        return lastType == IDENTIFIER || lastType == NUMBER || lastType == TRUE || lastType == FALSE ||
               lastType == NULL || lastType == DQ_CLOSE || lastType == SQ_CLOSE || lastType == HASH_CLOSE ||
               lastType == RPAREN || lastType == RBRACKET || lastType == RBRACE && structClosed;
    }

    // In script, `(` and `[` after an operand on its line call or index it. On a new line, or after a
    // comment, they start a new statement. In a tag's expression the line does not matter.
    private void openParenthesisOrBracket(int postfixType) {
        boolean postfix = !broken && afterOperandEnd();
        open(false);
        if (postfix) {
            setType(postfixType);
        }
    }

    // `+` and `-` after an operand add and subtract, on any line. Elsewhere they are signs.
    private void sign(int unaryType) {
        if (!afterOperandEnd() && lastType != INCR && lastType != DECR) {
            setType(unaryType);
        }
    }

    // A spelled-out operator follows an operand. Anywhere else the word is a name.
    private void operatorWord() {
        if (!afterOperandEnd()) {
            setType(IDENTIFIER);
        }
    }

    // `!` and NOT bind more loosely than comparison, except right after an arithmetic, concatenation
    // or comparison operator or a sign, where they negate only the operand that follows.
    private boolean tight() {
        switch (lastType) {
            case EQ: case NEQ: case IS: case EQUAL: case GT: case GTE: case GE: case LT: case LTE: case LE:
            case CONTAINS: case GREATER_THAN_OR_EQUAL_TO: case LESS_THAN_OR_EQUAL_TO: case GREATER_THAN:
            case LESS_THAN: case DOES_NOT_CONTAIN: case IS_NOT: case NOT_EQUAL: case TEQ: case TNEQ: case EQEQ:
            case NEQ_SIGN: case LTGT: case GTE_SIGN: case LTE_SIGN: case GT_SIGN: case LT_SIGN: case AMP:
            case PLUS: case MINUS: case UNARY_PLUS: case UNARY_MINUS: case PERCENT: case MOD: case BACKSLASH:
            case STAR: case SLASH: case CARET: case INCR: case DECR: case TIGHT_BANG: case TIGHT_NOT:
                return true;
            default:
                return false;
        }
    }

    // A keyword is a name after `.` or `var`, and wherever what follows it says so.
    private void keyword(boolean keyword) {
        if (!keyword || afterMember() || lastType == VAR) {
            setType(IDENTIFIER);
        }
    }

    // Statements are written in script and in blocks. A tag's expression or a #...# holds an
    // expression only, where `<cfreturn Return>` names a variable.
    private boolean statements() {
        Context context = contexts.peek();
        return context == null || context.kind == SCRIPT || context.kind == CFSCRIPT || !context.braces.isEmpty();
    }

    // `break` and `continue` end their statement, perhaps after a label. Elsewhere they name a
    // variable, as in `<cfif NOT continue>`.
    private boolean statementEnds() {
        if (!statements()) {
            return false;
        }
        int i = 1;
        while (la(i) == ' ' || la(i) == '\t') {
            i++;
        }
        if (i > 1 && isIdentStart(la(i))) {
            while (isIdentPart(la(i))) {
                i++;
            }
            while (la(i) == ' ' || la(i) == '\t') {
                i++;
            }
        }
        int next = la(i);
        return next == EOF || next == ';' || next == '}' || next == '\n' || next == '\r' ||
               next == '/' && (la(i + 1) == '/' || la(i + 1) == '*') || next == '<' && (la(i + 1) == '!' || la(i + 1) == '/');
    }

    private int la(int i) {
        return _input.LA(i);
    }

    private static boolean isWhitespace(int c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' || c == 0x0B || c == 0xA0 || c == 0xFEFF;
    }

    private static boolean isIdentStart(int c) {
        return c != EOF && (Character.isLetter(c) || c == '_' || c == '$');
    }

    private static boolean isIdentPart(int c) {
        return c != EOF && (Character.isLetterOrDigit(c) || c == '_' || c == '$');
    }

    private boolean at(int i, String s) {
        for (int k = 0; k < s.length(); k++) {
            if (Character.toLowerCase(la(i + k)) != s.charAt(k)) {
                return false;
            }
        }
        return true;
    }

    private int skipPast(int i, String end) {
        while (la(i) != EOF) {
            if (at(i, end)) {
                return i + end.length();
            }
            i++;
        }
        return i;
    }

    private int skipWhitespace(int i) {
        while (isWhitespace(la(i))) {
            i++;
        }
        return i;
    }

    // From LA index i, past whitespace and comments.
    private int skipTrivia(int i) {
        boolean comments = comments();
        while (true) {
            int c = la(i);
            if (isWhitespace(c)) {
                i++;
            } else if (at(i, "<!---")) {
                i = skipPast(i + 5, "--->");
            } else if (comments && c == '/' && la(i + 1) == '/') {
                while (la(i) != EOF && la(i) != '\n' && la(i) != '\r') {
                    i++;
                }
            } else if (comments && c == '/' && la(i + 1) == '*') {
                i = skipPast(i + 2, "*/");
            } else {
                return i;
            }
        }
    }

    private boolean followedBy(char c) {
        return la(skipTrivia(1)) == c;
    }

    private boolean followedByWord() {
        return isIdentStart(la(skipTrivia(1)));
    }

    // `function name(`, as opposed to `function` as a return type.
    private boolean followedByNameAndParenthesis() {
        int i = skipTrivia(1);
        if (i == 1 || !isIdentStart(la(i))) {
            return false;
        }
        while (isIdentPart(la(i))) {
            i++;
        }
        return la(skipTrivia(i)) == '(';
    }

    // `return.x` and `break = 1` use the keyword as a name.
    private boolean continuesAsExpression() {
        int j = skipWhitespace(1);
        int next = la(j);
        return next == '.' && !Character.isDigit(la(j + 1)) || next == '=' && la(j + 1) != '=';
    }

    private boolean attributeAt(int i) {
        if (!isIdentStart(la(i))) {
            return false;
        }
        while (isIdentPart(la(i)) || la(i) == '-' || la(i) == ':') {
            i++;
        }
        int j = skipWhitespace(i);
        return la(j) == '=' && la(j + 1) != '=' && la(j + 1) != '>';
    }

    private void returnKeyword() {
        if (afterMember() || lastType == VAR || !statements() || continuesAsExpression()) {
            setType(IDENTIFIER);
            return;
        }
        int i = skipTrivia(1);
        int next = la(i);
        if (next == EOF || next == ';' || next == '}' || at(i, "</cfscript")) {
            setType(BARE_RETURN);
        }
    }

    // `throw(...)` is a call and `throw message="x";` the throw tag. Either starts with a name.
    private void throwKeyword() {
        int j = skipWhitespace(1);
        int next = la(j);
        keyword(statements() && !(next == '(' || next == ';' || next == '}' || j > 1 && attributeAt(j)));
    }

    private boolean followedByTypeName() {
        int j = skipWhitespace(1);
        return j > 1 && (isIdentStart(la(j)) || la(j) == '"' || la(j) == '\'');
    }

    // `case` starts a case label unless it is used as a name.
    private boolean caseLabel() {
        int next = la(skipWhitespace(1));
        return !(next == '=' && la(skipWhitespace(1) + 1) != '=' || next == '.' || next == ',' || next == ')' ||
                 next == ']' || next == '}' || next == ';' || next == ':');
    }

    private boolean phraseEnds() {
        return !isIdentPart(la(1)) && afterOperandEnd();
    }

    private boolean safeNavigation() {
        return isIdentPart(la(skipTrivia(1)));
    }
}

// A rule's semantic predicate comes after the token's first character, so that the lexer can cache its first
// step. It caches no step into a state that reaches a predicate.

// =============================================================================================
// Script and expressions, in a script component, a <cfscript> body, a tag's expression and #...#
// =============================================================================================

WS                    : WS_CHAR+ { hidden(); };
LINE_COMMENT          : '//' { comments() }? ~[\r\n]* { hidden(); };
BLOCK_COMMENT         : '/*' { comments() }? .*? '*/' { hidden(); };
SCRIPT_TAG_COMMENT    : NESTED_COMMENT { hidden(); };

CFSCRIPT_END          : '</cfscript' { in(CFSCRIPT) }? { endCfscript(); };

// Spelled-out operators, which may not continue into a longer name.
GREATER_THAN_OR_EQUAL_TO : 'greater' WS_CHAR+ 'than' WS_CHAR+ 'or' WS_CHAR+ 'equal' WS_CHAR+ 'to' { phraseEnds() }?;
LESS_THAN_OR_EQUAL_TO : 'less' WS_CHAR+ 'than' WS_CHAR+ 'or' WS_CHAR+ 'equal' WS_CHAR+ 'to' { phraseEnds() }?;
GREATER_THAN          : 'greater' WS_CHAR+ 'than' { phraseEnds() }?;
LESS_THAN             : 'less' WS_CHAR+ 'than' { phraseEnds() }?;
DOES_NOT_CONTAIN      : 'does' WS_CHAR+ 'not' WS_CHAR+ 'contain' { phraseEnds() }?;
IS_NOT                : 'is' WS_CHAR+ 'not' { phraseEnds() }?;
NOT_EQUAL             : 'not' WS_CHAR+ 'equal' { phraseEnds() }?;

AND                   : 'and' { operatorWord(); };
OR                    : 'or' { operatorWord(); };
NOT                   : 'not' { keyword(true); if (getType() == NOT && tight()) setType(TIGHT_NOT); };
XOR                   : 'xor' { operatorWord(); };
EQV                   : 'eqv' { operatorWord(); };
IMP                   : 'imp' { operatorWord(); };
MOD                   : 'mod' { operatorWord(); };
EQ                    : 'eq' { operatorWord(); };
NEQ                   : 'neq' { operatorWord(); };
IS                    : 'is' { operatorWord(); };
EQUAL                 : 'equal' { operatorWord(); };
GT                    : 'gt' { operatorWord(); };
GTE                   : 'gte' { operatorWord(); };
GE                    : 'ge' { operatorWord(); };
LT                    : 'lt' { operatorWord(); };
LTE                   : 'lte' { operatorWord(); };
LE                    : 'le' { operatorWord(); };
CONTAINS              : 'contains' { operatorWord(); };

// Keywords wherever they are not a member name.
ELSE                  : 'else' { keyword(true); };
ELSEIF                : 'elseif' { keyword(followedBy('(')); };
CATCH                 : 'catch' { keyword(followedBy('(')); };
FINALLY               : 'finally' { keyword(followedBy('{')); };
IN                    : 'in' { keyword(afterOperandEnd()); };
TRUE                  : 'true' { keyword(true); };
FALSE                 : 'false' { keyword(true); };
NULL                  : 'null' { keyword(true); };

// Keywords when what follows makes them one, and names otherwise.
IF                    : 'if' { keyword(statements() && followedBy('(')); };
FOR                   : 'for' { keyword(statements() && followedBy('(')); };
WHILE                 : 'while' { keyword(statements() && followedBy('(')); };
SWITCH                : 'switch' { keyword(statements() && followedBy('(')); };
DO                    : 'do' { keyword(statements() && followedBy('{')); };
TRY                   : 'try' { keyword(statements() && followedBy('{')); };
FUNCTION              : 'function' { keyword(followedBy('(') || followedByNameAndParenthesis()); };
VAR                   : 'var' { keyword(followedByWord()); };
NEW                   : 'new' { keyword(followedByTypeName()); };
RETURN                : 'return' { returnKeyword(); };
BREAK                 : 'break' { keyword(statementEnds()); };
CONTINUE              : 'continue' { keyword(statementEnds()); };
THROW                 : 'throw' { throwKeyword(); };
CASE                  : 'case' { keyword(caseLabel()); };
DEFAULT               : 'default' { keyword(la(skipWhitespace(1)) == ':'); };
REQUIRED              : 'required' { keyword(followedByWord()); };

IDENTIFIER            : IDENT_START IDENT_PART*;

// After `.`, a name may start with a digit, as in `row.2FactorEnabled` or `row.2`.
DIGIT_NAME            : DIGIT+ (IDENT_START | NON_ASCII_DIGIT) { afterMember() }? IDENT_PART* -> type(IDENTIFIER);

NUMBER
    : DIGIT+ { if (afterMember()) setType(IDENTIFIER); }
    | DIGIT+ '.' DIGIT+ EXPONENT?
    | DIGIT+ EXPONENT
    | '.' DIGIT { !afterOperandEnd() }? DIGIT* EXPONENT?
    ;

DQ_OPEN               : '"' -> pushMode(DQ_STRING);
SQ_OPEN               : '\'' -> pushMode(SQ_STRING);

TEQ                   : '===';
TNEQ                  : '!==';
EQEQ                  : '==';
NEQ_SIGN              : '!=';
LTGT                  : '<>';
LTE_SIGN              : '<=';
// In a tag's expression, `>` ends the tag and `=` is template text.
GTE_SIGN              : '>=' { !outsideBrackets(TAG_EXPRESSION) }?;
ARROW                 : '=>';
PLUS_ASSIGN           : '+=';
MINUS_ASSIGN          : '-=';
STAR_ASSIGN           : '*=';
SLASH_ASSIGN          : '/=';
MOD_ASSIGN            : '%=';
CONCAT_ASSIGN         : '&=';
INCR                  : '++';
DECR                  : '--';
AMPAMP                : '&&';
PIPEPIPE              : '||';
// also written with whitespace between, `a ? : b`
ELVIS                 : '?' WS_CHAR* ':';
SAFE_DOT              : '?.' { safeNavigation() }?;
DOUBLE_COLON          : '::';
EXPRESSION_SELF_CLOSE : '/>' { outsideBrackets(TAG_EXPRESSION) }? { setType(TAG_SELF_CLOSE); exit(); };
ASSIGN                : '=';
PLUS                  : '+' { sign(UNARY_PLUS); };
MINUS                 : '-' { sign(UNARY_MINUS); };
STAR                  : '*';
SLASH                 : '/';
BACKSLASH             : '\\';
PERCENT               : '%';
CARET                 : '^';
AMP                   : '&';
BANG                  : '!' { if (tight()) setType(TIGHT_BANG); };
QUESTION              : '?';
COLON                 : ':';
DOT                   : '.';
COMMA                 : ',';
SEMI                  : ';';
LT_SIGN               : '<';
GT_SIGN               : '>' { greaterThan(); };
LPAREN                : '(' { openParenthesisOrBracket(CALL_LPAREN); };
RPAREN                : ')' { close(false); };
LBRACKET              : '[' { openParenthesisOrBracket(INDEX_LBRACKET); };
RBRACKET              : ']' { close(false); };
LBRACE                : '{' { open(true); };
RBRACE                : '}' { close(true); };
HASH                  : '#' { hash(); };

// =============================================================================================
// Template text, with tags and comments
// =============================================================================================

mode TEMPLATE;

TEMPLATE_COMMENT      : NESTED_COMMENT;
// what remains of a comment with no end, which no rule of the parser accepts
UNTERMINATED_COMMENT  : '<!---';
START_TAG_OPEN        : '<cf' TAG_NAME_CHAR+ { startTag(); };
END_TAG_OPEN          : '</cf' TAG_NAME_CHAR+ { endTag(); };
TEMPLATE_HASH         : '#' { templateHash(); };
TEXT                  : ~[<#]+;
TEXT_HASHES           : '##' -> type(TEXT);
TEXT_LT               : '<' -> type(TEXT);

// =============================================================================================
// A start tag's attributes
// =============================================================================================

mode TAG;

TAG_WS                : WS_CHAR+ -> channel(HIDDEN);
TAG_COMMENT           : NESTED_COMMENT -> channel(HIDDEN);
TAG_CLOSE             : '>' { popMode(); startTagEnded(); };
TAG_SELF_CLOSE        : '/>' -> popMode;
ATTRIBUTE_NAME        : ATTRIBUTE_START ATTRIBUTE_PART*;
TAG_EQUALS            : '=' -> pushMode(ATTRIBUTE_VALUE);

mode ATTRIBUTE_VALUE;

VALUE_WS              : WS_CHAR+ -> channel(HIDDEN);
VALUE_COMMENT         : NESTED_COMMENT -> channel(HIDDEN);
VALUE_DQ              : '"' { popMode(); pushMode(DQ_STRING); } -> type(DQ_OPEN);
VALUE_SQ              : '\'' { popMode(); pushMode(SQ_STRING); } -> type(SQ_OPEN);
VALUE_HASHES          : '##' { popMode(); pushMode(UNQUOTED); } -> type(UNQUOTED_TEXT);
VALUE_HASH            : '#' { popMode(); pushMode(UNQUOTED); enter(POUNDS); } -> type(HASH);
VALUE_TEXT            : UNQUOTED_FIRST UNQUOTED_CHAR* { popMode(); pushMode(UNQUOTED); } -> type(UNQUOTED_TEXT);
VALUE_SLASH           : '/' { popMode(); pushMode(UNQUOTED); } -> type(UNQUOTED_TEXT);

// An unquoted attribute value runs to whitespace or the end of the tag.
mode UNQUOTED;

UNQUOTED_WS           : WS_CHAR+ { popMode(); } -> channel(HIDDEN);
UNQUOTED_TEXT         : UNQUOTED_CHAR+;
UNQUOTED_SLASH        : '/' -> type(UNQUOTED_TEXT);
UNQUOTED_HASHES       : '##' -> type(UNQUOTED_TEXT);
UNQUOTED_HASH         : '#' { enter(POUNDS); } -> type(HASH);
UNQUOTED_CLOSE        : '>' { popMode(); popMode(); startTagEnded(); } -> type(TAG_CLOSE);
UNQUOTED_SELF_CLOSE   : '/>' { popMode(); popMode(); } -> type(TAG_SELF_CLOSE);

mode END_TAG;

END_TAG_WS            : WS_CHAR+ -> channel(HIDDEN);
END_TAG_COMMENT       : NESTED_COMMENT -> channel(HIDDEN);
END_TAG_CLOSE         : '>' -> popMode;

// =============================================================================================
// Strings, where doubled quotes and ## stand for themselves and #...# interpolates
// =============================================================================================

mode DQ_STRING;

DQ_TEXT               : (~["#] | '""' | '##')+;
DQ_HASH               : '#' { enter(POUNDS); } -> type(HASH);
DQ_CLOSE              : '"' -> popMode;

mode SQ_STRING;

SQ_TEXT               : (~['#] | '\'\'' | '##')+;
SQ_HASH               : '#' { enter(POUNDS); } -> type(HASH);
SQ_CLOSE              : '\'' -> popMode;

// =============================================================================================

fragment WS_CHAR        : [ \t\r\n\f\u000B ﻿];
fragment DIGIT          : [0-9];
fragment NON_ASCII_DIGIT : ~[0-9\P{Nd}];
fragment EXPONENT       : 'e' [+-]? DIGIT+;
fragment IDENT_START    : [\p{L}_$];
fragment IDENT_PART     : [\p{L}\p{Nd}_$];
fragment TAG_NAME_CHAR  : [\p{L}\p{Nd}_];
fragment ATTRIBUTE_START : [\p{L}_$];
fragment ATTRIBUTE_PART : [\p{L}\p{Nd}_$:.\-];
// <!--- comments ---> nest.
fragment NESTED_COMMENT : '<!---' (NESTED_COMMENT | .)*? '--->';
fragment UNQUOTED_FIRST : ~[ \t\r\n\f\u000B ﻿>#/"'<];
fragment UNQUOTED_CHAR  : ~[ \t\r\n\f\u000B ﻿>#/];
