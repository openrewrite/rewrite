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
     * (e.g., when the first ) closes a command substitution or nested parens).
     */
    private boolean isDoubleParenClose() {
        if (!substTypeStack.isEmpty()) {
            if (parenDepth > 0) {
                // Inside a substitution with nested parens — each ) should be
                // RPAREN to properly close inner parens before )) closes the subst.
                // e.g., $(((1))) → $((  (1)  )) where ))) is RPAREN + DOUBLE_RPAREN
                return false;
            }
            // parenDepth == 0, at the substitution boundary
            // arithmetic $(()) → )) closes it; command $() → ) closes it
            return substTypeStack.peek();
        }
        // Not in substitution context (standalone (( )) or bare )) )
        return true;
    }

    /**
     * Disambiguate $(( as arithmetic vs $( + ( (command substitution + subshell).
     * Called AFTER matching '$((' to decide if this is DOLLAR_DPAREN or should
     * fall through to DOLLAR_LPAREN + LPAREN.
     *
     * Heuristic: after $((, skip whitespace, then:
     * - If first char is digit, $, (, ), +, -, ~, ! → arithmetic
     * - If first char is a letter, check what follows the word:
     *   - If followed by space + another letter/word → likely command (e.g., "echo hello")
     *   - If followed by arithmetic operator or ) → arithmetic (e.g., "x + 1", "x)")
     */
    private boolean isLikelyArithmetic() {
        CharStream input = getInputStream();
        int pos = input.index();
        int size = input.size();

        // Skip whitespace
        while (pos < size) {
            char c = (char) input.getText(
                new org.antlr.v4.runtime.misc.Interval(pos, pos)).charAt(0);
            if (c != ' ' && c != '\t') break;
            pos++;
        }
        if (pos >= size) return true;

        char first = (char) input.getText(
            new org.antlr.v4.runtime.misc.Interval(pos, pos)).charAt(0);

        // Non-letter starts are clearly arithmetic
        if (!Character.isLetter(first) && first != '_') {
            return true;
        }

        // First char is a letter — scan the word
        int wordEnd = pos;
        while (wordEnd < size) {
            char c = (char) input.getText(
                new org.antlr.v4.runtime.misc.Interval(wordEnd, wordEnd)).charAt(0);
            if (!Character.isLetterOrDigit(c) && c != '_') break;
            wordEnd++;
        }
        if (wordEnd >= size) return true;

        char afterWord = (char) input.getText(
            new org.antlr.v4.runtime.misc.Interval(wordEnd, wordEnd)).charAt(0);

        // If word is followed by arithmetic operator or ), it's arithmetic
        if (afterWord == '+' || afterWord == '-' || afterWord == '*' || afterWord == '/'
            || afterWord == '%' || afterWord == '<' || afterWord == '>' || afterWord == '='
            || afterWord == '&' || afterWord == '|' || afterWord == '^' || afterWord == '!'
            || afterWord == '?' || afterWord == ':' || afterWord == ',' || afterWord == ')'
            || afterWord == ']') {
            return true;
        }

        // If word is followed by whitespace, check what comes after
        if (afterWord == ' ' || afterWord == '\t') {
            int next = wordEnd;
            while (next < size) {
                char nc = (char) input.getText(
                    new org.antlr.v4.runtime.misc.Interval(next, next)).charAt(0);
                if (nc != ' ' && nc != '\t') break;
                next++;
            }
            if (next >= size) return true;

            char afterSpace = (char) input.getText(
                new org.antlr.v4.runtime.misc.Interval(next, next)).charAt(0);
            // After whitespace: arithmetic operator or digit = arithmetic.
            // Dollar, paren, and letters are NOT listed here since they appear in
            // both arithmetic and command contexts.
            if (afterSpace == '+' || afterSpace == '-' || afterSpace == '*' || afterSpace == '/'
                || afterSpace == '%' || afterSpace == '<' || afterSpace == '>' || afterSpace == '='
                || afterSpace == '&' || afterSpace == '|' || afterSpace == '^'
                || afterSpace == '?' || afterSpace == ':' || afterSpace == ')' || afterSpace == ']'
                || Character.isDigit(afterSpace)) {
                return true;
            }
            // After whitespace: letter, $, (, or anything else → command substitution
            return false;
        }

        // Default to arithmetic
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
// Semantic predicate: only match $(( as arithmetic if lookahead suggests arithmetic content.
// When the predicate fails, ANTLR falls through to match $( (DOLLAR_LPAREN) + ( (LPAREN).
DOLLAR_DPAREN  : '$((' { isLikelyArithmetic() }? { pushSubst(true); } ;
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

// Backtick command substitution — pushes BACKTICK_CMD mode to prevent
// COMMENT tokens from consuming the closing backtick
BACKTICK : '`' -> pushMode(BACKTICK_CMD) ;

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

DQ_DOLLAR_DPAREN  : '$((' { isLikelyArithmetic() }? { pushSubst(true); } -> type(DOLLAR_DPAREN) ;
DQ_DOLLAR_LPAREN  : '$('  { pushSubst(false); } -> type(DOLLAR_LPAREN) ;
DQ_DOLLAR_LBRACE  : '${'  -> pushMode(BRACE_EXPANSION), type(DOLLAR_LBRACE) ;
DQ_DOLLAR_NAME    : '$' [a-zA-Z_] [a-zA-Z0-9_]* -> type(DOLLAR_NAME) ;
DQ_SPECIAL_VAR    : '$' [?!$#@*0-9\-] -> type(SPECIAL_VAR) ;
DQ_BACKTICK       : '`' -> type(BACKTICK), pushMode(BACKTICK_CMD) ;
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
BE_DOLLAR_DPAREN : '$((' { isLikelyArithmetic() }? { pushSubst(true); } -> type(DOLLAR_DPAREN) ;
BE_DOLLAR_LPAREN : '$(' { pushSubst(false); } -> type(DOLLAR_LPAREN) ;
BE_DOLLAR_SINGLE_QUOTED : '$\'' ( '\\' . | ~['\\] )* '\'' -> type(DOLLAR_SINGLE_QUOTED) ;
BE_DOLLAR_NAME   : '$' [a-zA-Z_] [a-zA-Z0-9_]* -> type(DOLLAR_NAME) ;
BE_SPECIAL_VAR   : '$' [?!$#@*0-9\-] -> type(SPECIAL_VAR) ;
BE_DOLLAR        : '$' -> type(DOLLAR) ;

BE_NAME : [a-zA-Z_] [a-zA-Z0-9_]* ;
BE_NUMBER : [0-9]+ ;
BE_DOUBLE_QUOTE : '"' -> type(DOUBLE_QUOTE), pushMode(DOUBLE_QUOTED) ;
BE_SINGLE_QUOTE : '\'' ( ~['] )* '\'' -> type(SINGLE_QUOTED_STRING) ;

// Text inside brace expansion (default values, patterns, etc.)
BE_TEXT : ( ~[}$"'\\#%/^,@*!:[\]] | '\\' . )+ ;

BE_WS : [ \t]+ -> type(WS) ;

// ==========================================
// Mode: BACKTICK_CMD - inside `...` command substitution
// Captures content as opaque text to prevent COMMENT tokens
// from consuming the closing backtick.
// ==========================================
mode BACKTICK_CMD;

BT_END : '`' -> type(BACKTICK), popMode ;
// Capture everything between backticks as opaque content.
// Handles escaped backticks (\`) as part of content.
BT_CONTENT : ( '\\`' | ~[`] )+ ;
