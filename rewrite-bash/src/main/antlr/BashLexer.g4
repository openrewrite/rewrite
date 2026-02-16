// Bash lexer grammar for OpenRewrite
// Designed to preserve all whitespace and comments for lossless round-tripping
// Whitespace goes to HIDDEN channel; the parser visitor reconstructs it from token positions.

lexer grammar BashLexer;

@lexer::header
{import java.util.LinkedList;
import java.util.Queue;
import java.util.ArrayDeque;
import java.util.Deque;}

@lexer::members
{
    // Track heredoc markers in FIFO order
    private Queue<String> heredocMarkers = new LinkedList<>();
    private boolean heredocStripTabs = false;

    // Track command/arithmetic substitution nesting for mode transitions.
    // When $( or $(( is encountered (in any mode), we push DEFAULT_MODE
    // and track the substitution type and parenthesis depth so we know
    // when ) or )) should pop the mode.
    // Each entry: false = command substitution $(), true = arithmetic $(())
    private Deque<Boolean> substTypeStack = new ArrayDeque<>();
    private Deque<Integer> parenDepthStack = new ArrayDeque<>();
    private int parenDepth = 0;

    private void pushSubst(boolean isArithmetic) {
        substTypeStack.push(isArithmetic);
        parenDepthStack.push(parenDepth);
        parenDepth = 0;
        pushMode(DEFAULT_MODE);
    }

    private boolean tryPopSubst(boolean isDoubleClose) {
        if (parenDepth == 0 && !substTypeStack.isEmpty()) {
            boolean isArith = substTypeStack.peek();
            if (isArith == isDoubleClose) {
                substTypeStack.pop();
                parenDepth = parenDepthStack.pop();
                popMode();
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true when )) should be treated as a single DOUBLE_RPAREN token.
     * Returns false when )) should be split into two RPAREN tokens
     * (e.g., when the first ) closes a command substitution).
     */
    private boolean isDoubleParenClose() {
        if (parenDepth == 0 && !substTypeStack.isEmpty()) {
            // We're at the boundary of a substitution
            boolean isArith = substTypeStack.peek();
            if (!isArith) {
                // In a command substitution $() - first ) closes it, second ) is separate
                return false;
            }
            // In arithmetic substitution $(()) - )) closes it as a unit
            return true;
        }
        // Not in substitution context, or nested parens - treat as ))
        return true;
    }
}

// ==========================================
// Shebang (must be first line)
// ==========================================
SHEBANG : '#!' ~[\r\n]* { getCharPositionInLine() - getText().length() == 0 }? ;

// ==========================================
// Line continuation (backslash-newline is whitespace)
// ==========================================
LINE_CONTINUATION : '\\' '\r'? '\n' -> channel(HIDDEN) ;

// ==========================================
// Newline (significant in bash - command separator)
// ==========================================
NEWLINE : '\r'? '\n' ;

// ==========================================
// Comments
// ==========================================
COMMENT : '#' ~[\r\n]* ;

// ==========================================
// Keywords
// The parser's `word` rule includes all keywords as alternatives,
// so keywords are only treated specially in keyword positions.
// ==========================================
IF       : 'if' ;
THEN     : 'then' ;
ELSE     : 'else' ;
ELIF     : 'elif' ;
FI       : 'fi' ;
FOR      : 'for' ;
WHILE    : 'while' ;
UNTIL    : 'until' ;
DO       : 'do' ;
DONE     : 'done' ;
CASE     : 'case' ;
ESAC     : 'esac' ;
IN       : 'in' ;
FUNCTION : 'function' ;
SELECT   : 'select' ;
COPROC   : 'coproc' ;
TIME     : 'time' ;

// ==========================================
// Multi-character operators (longest match first)
// ==========================================

// Compound command delimiters (2-char before 1-char)
DOUBLE_LBRACKET : '[[' ;
DOUBLE_RBRACKET : ']]' ;
DOUBLE_LPAREN   : '((' { parenDepth += 2; } ;
DOUBLE_RPAREN   : '))' { isDoubleParenClose() }? { if (!tryPopSubst(true)) { if (parenDepth >= 2) parenDepth -= 2; } } ;

// Command substitution (3-char before 2-char)
DOLLAR_DPAREN  : '$((' { pushSubst(true); } ;
DOLLAR_LPAREN  : '$(' { pushSubst(false); } ;
DOLLAR_LBRACE  : '${' -> pushMode(BRACE_EXPANSION) ;

// Redirection (3-char before 2-char before 1-char)
HERESTRING : '<<<' ;
DLESSDASH  : '<<-' ;
DLESS      : '<<' ;
DGREAT     : '>>' ;
LESSAND    : '<&' ;
GREATAND   : '>&' ;
LESSGREAT  : '<>' ;
CLOBBER    : '>|' ;

// Process substitution (2-char)
PROC_SUBST_IN  : '<(' { parenDepth++; } ;
PROC_SUBST_OUT : '>(' { parenDepth++; } ;

// Single-char redirection
LESS       : '<' ;
GREAT      : '>' ;

// List/pipe operators (2-char before 1-char)
AND       : '&&' ;
OR        : '||' ;
PIPE_AND  : '|&' ;
PIPE      : '|' ;
DSEMI_AND : ';;&' ;
DSEMI     : ';;' ;
SEMI_AND  : ';&' ;
SEMI      : ';' ;
AMP       : '&' ;

// Assignment operators (2-char before 1-char)
PLUS_EQUALS : '+=' ;
EQUALS      : '=' ;

// Single-char delimiters
LPAREN   : '(' { parenDepth++; } ;
RPAREN   : ')' { if (!tryPopSubst(false)) { if (parenDepth > 0) parenDepth--; } } ;
LBRACE   : '{' ;
RBRACE   : '}' ;
LBRACKET : '[' ;
RBRACKET : ']' ;
BANG     : '!' ;
COLON    : ':' ;

// Backtick command substitution
BACKTICK : '`' ;

// ==========================================
// Variable references
// ==========================================

// Special variables: $?, $!, $$, $#, $@, $*, $0-$9, $-
SPECIAL_VAR : '$' [?!$#@*0-9\-] ;

// Simple variable reference: $NAME
DOLLAR_NAME : '$' [a-zA-Z_] [a-zA-Z0-9_]* ;

// ==========================================
// Quoting
// ==========================================

// Dollar single-quoted string: $'...' with C-style escapes (before DOLLAR_NAME to avoid conflict)
DOLLAR_SINGLE_QUOTED : '$\'' ( '\\' . | ~['\\] )* '\'' ;

// Single-quoted string: no interpolation, no escapes
SINGLE_QUOTED_STRING : '\'' ( ~['] )* '\'' ;

// Double-quoted string start
DOUBLE_QUOTE : '"' -> pushMode(DOUBLE_QUOTED) ;

// ==========================================
// Unquoted words and text
// ==========================================

// Integer literal for file descriptors in redirections and arithmetic
NUMBER : [0-9]+ ;

// Regular unquoted word (identifiers, paths, etc.)
// Stops at shell metacharacters. Note: + is allowed in WORD (for arithmetic, paths).
// [ and ] are excluded so they become separate LBRACKET/RBRACKET tokens.
WORD : ( ~[ \t\r\n|&;<>(){}$`"'\\!#=:*?~[\]] | '\\' . )+ ;

// Bare $ not followed by a variable/substitution trigger
DOLLAR   : '$' ;

// Glob characters (separate tokens for recipe use)
STAR     : '*' ;
QUESTION : '?' ;
TILDE    : '~' ;

// ==========================================
// Whitespace (HIDDEN channel - reconstructed from token positions)
// ==========================================
WS : [ \t]+ -> channel(HIDDEN) ;

// ==========================================
// Catch-all for any other character
// ==========================================
ANY_CHAR : . ;

// ==========================================
// Mode: DOUBLE_QUOTED - inside double-quoted strings
// ==========================================
mode DOUBLE_QUOTED;

DQ_DOLLAR_DPAREN  : '$((' { pushSubst(true); } -> type(DOLLAR_DPAREN) ;
DQ_DOLLAR_LPAREN  : '$('  { pushSubst(false); } -> type(DOLLAR_LPAREN) ;
DQ_DOLLAR_LBRACE  : '${'  -> pushMode(BRACE_EXPANSION), type(DOLLAR_LBRACE) ;
DQ_DOLLAR_NAME    : '$' [a-zA-Z_] [a-zA-Z0-9_]* -> type(DOLLAR_NAME) ;
DQ_SPECIAL_VAR    : '$' [?!$#@*0-9\-] -> type(SPECIAL_VAR) ;
DQ_BACKTICK       : '`' -> type(BACKTICK) ;
DQ_ESCAPE         : '\\' [\\$`"\n] ;
DQ_END            : '"' -> type(DOUBLE_QUOTE), popMode ;
// Bare $ not followed by a variable/substitution trigger - just text
DQ_DOLLAR         : '$' -> type(DQ_TEXT) ;
DQ_TEXT           : ( ~[$`"\\] | '\\' ~[\\$`"\n] )+ ;

// ==========================================
// Mode: BRACE_EXPANSION - inside ${...}
// ==========================================
mode BRACE_EXPANSION;

BE_RBRACE : '}' -> type(RBRACE), popMode ;

// Operators inside ${...}
BE_COLON_DASH   : ':-' ;
BE_COLON_EQUALS : ':=' ;
BE_COLON_PLUS   : ':+' ;
BE_COLON_QUEST  : ':?' ;
BE_DHASH        : '##' ;
BE_HASH         : '#' ;
BE_DPERCENT     : '%%' ;
BE_PERCENT      : '%' ;
BE_DSLASH       : '//' ;
BE_SLASH        : '/' ;
BE_DCARET       : '^^' ;
BE_CARET        : '^' ;
BE_DCOMMA       : ',,' ;
BE_COMMA        : ',' ;
BE_AT           : '@' ;
BE_STAR         : '*' ;
BE_BANG         : '!' ;
BE_COLON        : ':' ;
BE_LBRACKET     : '[' ;
BE_RBRACKET     : ']' ;

// Nested constructs inside brace expansion
BE_DOLLAR_LBRACE : '${' -> pushMode(BRACE_EXPANSION), type(DOLLAR_LBRACE) ;
BE_DOLLAR_DPAREN : '$((' { pushSubst(true); } -> type(DOLLAR_DPAREN) ;
BE_DOLLAR_LPAREN : '$(' { pushSubst(false); } -> type(DOLLAR_LPAREN) ;
BE_DOLLAR_NAME   : '$' [a-zA-Z_] [a-zA-Z0-9_]* -> type(DOLLAR_NAME) ;
BE_SPECIAL_VAR   : '$' [?!$#@*0-9\-] -> type(SPECIAL_VAR) ;

BE_NAME : [a-zA-Z_] [a-zA-Z0-9_]* ;
BE_NUMBER : [0-9]+ ;
BE_DOUBLE_QUOTE : '"' -> type(DOUBLE_QUOTE), pushMode(DOUBLE_QUOTED) ;
BE_SINGLE_QUOTE : '\'' ( ~['] )* '\'' -> type(SINGLE_QUOTED_STRING) ;

// Text inside brace expansion (default values, patterns, etc.)
BE_TEXT : ( ~[}$"'\\#%/^,@*!:[\]] | '\\' . )+ ;

BE_WS : [ \t]+ -> type(WS) ;
