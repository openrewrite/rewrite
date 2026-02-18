// Bash parser grammar for OpenRewrite
// Designed for lossless round-tripping of bash scripts

parser grammar BashParser;

options {
    tokenVocab = BashLexer;
}

@members {
    private boolean noWhitespaceBefore() {
        Token prev = _input.LT(-1);
        Token next = _input.LT(1);
        if (prev == null || next == null) return false;
        int gapStart = prev.getStopIndex() + 1;
        int gapEnd = next.getStartIndex();
        if (gapStart >= gapEnd) return true;
        CharStream input = _input.getTokenSource().getInputStream();
        String gap = input.getText(new org.antlr.v4.runtime.misc.Interval(gapStart, gapEnd - 1));
        for (int i = 0; i < gap.length(); i++) {
            char c = gap.charAt(i);
            if (c == ' ' || c == '\t') return false;
        }
        return true;
    }
}

// ==========================================
// Root rule
// ==========================================
program
    : shebang? linebreak completeCommands? linebreak EOF
    ;

shebang
    : SHEBANG linebreak
    ;

// ==========================================
// Command lists
// ==========================================
completeCommands
    : completeCommand ( newlineList completeCommand )*
    ;

completeCommand
    : list separator?
    ;

// A list of pipelines separated by ; or &
list
    : andOr ( listSep andOr )*
    ;

listSep
    : SEMI linebreak
    | AMP linebreak
    ;

// && and || chains (equal precedence in command lists)
andOr
    : pipeline ( andOrOp linebreak pipeline )*
    ;

andOrOp
    : AND
    | OR
    ;

// ==========================================
// Pipelines
// ==========================================
pipeline
    : bangOpt? timeOpt? pipeSequence
    ;

bangOpt
    : BANG
    ;

timeOpt
    : TIME
    ;

pipeSequence
    : command ( pipeOp linebreak command )*
    ;

pipeOp
    : PIPE
    | PIPE_AND
    ;

// ==========================================
// Commands
// ==========================================
command
    : compoundCommand redirectionList?
    | functionDefinition
    | simpleCommand
    ;

// ==========================================
// Simple commands
// ==========================================
simpleCommand
    : cmdPrefix cmdWord? cmdSuffix?
    | cmdWord cmdSuffix?
    ;

cmdPrefix
    : ( assignment | redirection )+
    ;

cmdWord
    : word
    ;

cmdSuffix
    : ( assignment | word | redirection )+
    ;

// ==========================================
// Compound commands
// ==========================================
compoundCommand
    : braceGroup
    | subshell
    | ifClause
    | forClause
    | cStyleForClause
    | whileClause
    | untilClause
    | caseClause
    | selectClause
    | doubleParenExpr
    | doubleBracketExpr
    ;

// { list; }
braceGroup
    : LBRACE compoundList RBRACE
    ;

// (list)
subshell
    : LPAREN compoundList RPAREN
    ;

compoundList
    : linebreak completeCommands linebreak
    ;

// ==========================================
// If clause
// ==========================================
ifClause
    : IF compoundList THEN compoundList elifClause* elseClause? FI
    ;

elifClause
    : ELIF compoundList THEN compoundList
    ;

elseClause
    : ELSE compoundList
    ;

// ==========================================
// For clause
// ==========================================
forClause
    : FOR WORD forBody
    ;

forBody
    : inClause doGroup
    | doGroup
    ;

inClause
    : linebreak IN word* sequentialSep
    ;

// C-style for loop: for ((init; cond; update))
cStyleForClause
    : FOR DOUBLE_LPAREN arithmeticExpr? SEMI arithmeticExpr? SEMI arithmeticExpr? DOUBLE_RPAREN sequentialSep? doGroup
    ;

// ==========================================
// While/Until clause
// ==========================================
whileClause
    : WHILE compoundList doGroup
    ;

untilClause
    : UNTIL compoundList doGroup
    ;

doGroup
    : DO compoundList DONE
    ;

// ==========================================
// Select clause (similar to for)
// ==========================================
selectClause
    : SELECT WORD inClause doGroup
    ;

// ==========================================
// Case clause
// ==========================================
caseClause
    : CASE word linebreak IN linebreak caseItem* ESAC
    ;

caseItem
    : LPAREN? pattern RPAREN linebreak compoundList? caseSeparator linebreak
    | LPAREN? pattern RPAREN linebreak
    ;

caseSeparator
    : DSEMI
    | SEMI_AND
    | DSEMI_AND
    ;

pattern
    : word ( PIPE word )*
    ;

// ==========================================
// Double paren (arithmetic)
// ==========================================
doubleParenExpr
    : DOUBLE_LPAREN arithmeticExpr DOUBLE_RPAREN
    ;

// Arithmetic expression - capture tokens loosely
arithmeticExpr
    : arithmeticPart+
    ;

arithmeticPart
    : WORD
    | NUMBER
    | DOLLAR_NAME
    | SPECIAL_VAR
    | DOLLAR_LBRACE braceExpansionContent RBRACE
    | DOLLAR_LPAREN commandSubstitutionContent RPAREN
    | DOLLAR_DPAREN arithmeticExpr DOUBLE_RPAREN
    | LPAREN arithmeticExpr RPAREN
    | EQUALS
    | PLUS_EQUALS
    | BANG
    | LESS
    | GREAT
    | DLESS       // << left-shift in arithmetic
    | DGREAT      // >> right-shift in arithmetic
    | STAR
    | QUESTION
    | AND
    | OR
    | AMP
    | PIPE
    | COLON
    | SEMI
    | LBRACKET
    | RBRACKET
    | TILDE
    | DOLLAR
    ;

// ==========================================
// Double bracket (conditional expression)
// ==========================================
doubleBracketExpr
    : DOUBLE_LBRACKET conditionExpr DOUBLE_RBRACKET
    ;

// Condition expression tokens - captured loosely for lossless round-trip
conditionExpr
    : conditionPart+
    ;

conditionPart
    : word
    | LESS
    | GREAT
    | BANG
    | EQUALS
    | AND
    | OR
    | PIPE
    | AMP
    | SEMI
    | LBRACE
    | RBRACE
    | DOLLAR
    | DOUBLE_LBRACKET conditionExpr DOUBLE_RBRACKET  // nested [[ ]] for POSIX character classes
    | DOUBLE_LPAREN    // \( in regex lexes as (( when escaped
    | DOUBLE_RPAREN    // \) in regex
    | NEWLINE
    | COMMENT
    | LPAREN conditionExpr RPAREN
    ;

// ==========================================
// Single bracket test
// ==========================================
// [ is a command called 'test', so parsed as simpleCommand
// with ] as the last argument. No special grammar needed.

// ==========================================
// Function definition
// ==========================================
functionDefinition
    : FUNCTION WORD functionParens? linebreak functionBody
    | WORD functionParens linebreak functionBody
    ;

functionParens
    : LPAREN RPAREN
    ;

functionBody
    : compoundCommand redirectionList?
    ;

// ==========================================
// Assignments
// ==========================================
assignment
    : WORD ( EQUALS | PLUS_EQUALS ) assignmentValue?
    ;

assignmentValue
    : word
    | LPAREN arrayElements? RPAREN
    ;

arrayElements
    : linebreak ( word linebreak )+
    ;

// ==========================================
// Redirections
// ==========================================
redirectionList
    : redirection+
    ;

redirection
    : NUMBER? redirectionOp word
    | NUMBER? HERESTRING word
    | NUMBER? heredoc
    ;

redirectionOp
    : LESS
    | GREAT
    | DGREAT
    | LESSAND
    | GREATAND
    | LESSGREAT
    | CLOBBER
    ;

// ==========================================
// Here documents
// ==========================================
heredoc
    : ( DLESS | DLESSDASH ) heredocDelimiter ~NEWLINE* NEWLINE heredocBody
    ;

heredocDelimiter
    : WORD
    | SINGLE_QUOTED_STRING
    | DOUBLE_QUOTE DQ_TEXT? DOUBLE_QUOTE
    ;

heredocBody
    : heredocLine+
    ;

heredocLine
    : ~(NEWLINE)* NEWLINE
    ;

// ==========================================
// Words (arguments, values)
// ==========================================
word
    : wordPart ({noWhitespaceBefore()}? wordPart)*
    ;

wordPart
    : WORD
    | NUMBER
    | SINGLE_QUOTED_STRING
    | DOLLAR_SINGLE_QUOTED
    | doubleQuotedString
    | DOLLAR_NAME
    | SPECIAL_VAR
    | DOLLAR_LBRACE braceExpansionContent RBRACE
    | commandSubstitution
    | BACKTICK backtickContent BACKTICK
    | arithmeticSubstitution
    | processSubstitution
    | STAR
    | QUESTION
    | TILDE
    | COLON
    | DOLLAR
    | LBRACKET
    | RBRACKET
    | BANG
    | EQUALS
    | PLUS_EQUALS
    | LBRACE wordPart* RBRACE  // brace expansion: {1..10}, {a,b,c}, {} (find -exec)
    // Keywords when used as regular words (not in command position)
    | IF | THEN | ELSE | ELIF | FI
    | FOR | WHILE | UNTIL | DO | DONE
    | CASE | ESAC | IN
    | FUNCTION | SELECT | COPROC | TIME
    ;

doubleQuotedString
    : DOUBLE_QUOTE doubleQuotedPart* DOUBLE_QUOTE
    ;

doubleQuotedPart
    : DQ_TEXT
    | DQ_ESCAPE
    | DOLLAR_NAME
    | SPECIAL_VAR
    | DOLLAR_LBRACE braceExpansionContent RBRACE
    | commandSubstitution
    | arithmeticSubstitution
    | BACKTICK backtickContent BACKTICK
    ;

// ==========================================
// Command substitution: $(...)
// ==========================================
commandSubstitution
    : DOLLAR_LPAREN commandSubstitutionContent RPAREN
    ;

commandSubstitutionContent
    : linebreak completeCommands? linebreak
    ;

// Backtick command substitution: `...`
backtickContent
    : ( ~(BACKTICK) )*
    ;

// ==========================================
// Arithmetic substitution: $((...))
// ==========================================
arithmeticSubstitution
    : DOLLAR_DPAREN arithmeticExpr DOUBLE_RPAREN
    ;

// ==========================================
// Process substitution: <(...) or >(...)
// ==========================================
processSubstitution
    : PROC_SUBST_IN commandSubstitutionContent RPAREN
    | PROC_SUBST_OUT commandSubstitutionContent RPAREN
    ;

// ==========================================
// Brace expansion content: inside ${...}
// ==========================================
braceExpansionContent
    : braceExpansionPart*
    ;

braceExpansionPart
    : BE_NAME
    | BE_NUMBER
    | BE_TEXT
    | BE_COLON_DASH
    | BE_COLON_EQUALS
    | BE_COLON_PLUS
    | BE_COLON_QUEST
    | BE_HASH
    | BE_DHASH
    | BE_PERCENT
    | BE_DPERCENT
    | BE_SLASH
    | BE_DSLASH
    | BE_CARET
    | BE_DCARET
    | BE_COMMA
    | BE_DCOMMA
    | BE_AT
    | BE_STAR
    | BE_BANG
    | BE_COLON
    | BE_LBRACKET
    | BE_RBRACKET
    | DOLLAR_LBRACE braceExpansionContent RBRACE
    | DOLLAR_LPAREN commandSubstitutionContent RPAREN
    | DOLLAR_DPAREN arithmeticExpr DOUBLE_RPAREN
    | DOLLAR_NAME
    | SPECIAL_VAR
    | DOLLAR_SINGLE_QUOTED
    | DOLLAR
    | DOUBLE_QUOTE doubleQuotedPart* DOUBLE_QUOTE
    | SINGLE_QUOTED_STRING
    | WS
    ;

// ==========================================
// Separators and whitespace handling
// ==========================================
separator
    : SEMI linebreak
    | AMP linebreak
    ;

sequentialSep
    : SEMI linebreak
    | newlineList
    ;

newlineList
    : ( NEWLINE | COMMENT NEWLINE? )+
    ;

linebreak
    : newlineList?
    ;
