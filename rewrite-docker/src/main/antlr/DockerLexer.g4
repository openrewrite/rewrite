// $antlr-format alignTrailingComments true, columnLimit 150, maxEmptyLinesToKeep 1, reflowComments false, useTab false
// $antlr-format allowShortRulesOnASingleLine true, allowShortBlocksOnASingleLine true, minEmptyLines 0, alignSemicolons ownLine
// $antlr-format alignColons trailing, singleLineOverrulesHangingColon true, alignLexerCommands true, alignLabels true, alignTrailers true

lexer grammar DockerLexer;

@lexer::header
{import java.util.LinkedList;
import java.util.Queue;}

@lexer::members
{
    // Use a queue (FIFO) for heredoc markers so they are matched in order of declaration
    private Queue<String> heredocIdentifiers = new LinkedList<String>();
    // Whether an instruction keyword is recognized here, i.e. at the start of a logical line
    private boolean atLineStart = true;
    // Whether the CMD or NONE that a HEALTHCHECK takes is still to come
    private boolean afterHealthcheck = false;
    // Whether the flags of a COPY or ADD are still being read, where --from carries an image reference
    private boolean copyAddFlags = false;

    // Each flag above holds over a region of one logical line, so each is one question about the token
    // just matched: does that token still belong to the region, or is it the first one past its end?
    // Asking it here, once per flag, is what stops a rule added later from silently widening a region
    // by forgetting to close it - a rule that is not named below ends every region it is not part of.
    // emit() runs after the matched rule's action and commands, so _type is the type the token ends up
    // with (an instruction off line start has become UNQUOTED_TEXT) and _mode is the mode after any
    // push or pop, which is the mode the next token will be read in.
    @Override
    public Token emit() {
        boolean lineEnded = endsLine();
        atLineStart = lineEnded || atLineStart && continuesLineStart();
        afterHealthcheck = !lineEnded && (_type == HEALTHCHECK || afterHealthcheck && _type != CMD && _type != NONE);
        copyAddFlags = !lineEnded && (_type == COPY || _type == ADD || copyAddFlags && continuesCopyAddFlags());
        return super.emit();
    }

    // A newline ends the logical line, except in the body of a heredoc, where it only separates content
    // lines. A parser directive ends one too, because it matches its own trailing newline.
    private boolean endsLine() {
        return _type == PARSER_DIRECTIVE || _type == NEWLINE && _mode != HEREDOC;
    }

    // Nothing hidden stands between the start of a line and the instruction on it. ONBUILD and
    // HEALTHCHECK each take a second instruction, and the flags of a HEALTHCHECK and their values stand
    // between it and the CMD or NONE it takes, so neither ends the position an instruction is read in.
    private boolean continuesLineStart() {
        switch (_type) {
            case WS: case LINE_CONTINUATION: case COMMENT:
            case ONBUILD: case HEALTHCHECK:
                return true;
            case FLAG: case DASH_DASH: case EQUALS:
            case DOUBLE_QUOTED_STRING: case SINGLE_QUOTED_STRING: case UNQUOTED_TEXT:
                return afterHealthcheck;
            default:
                return false;
        }
    }

    // The flag section of a COPY or ADD reaches to the first of its paths. FLAG_IMAGE_REF holds the
    // value of a --from, which is part of the section rather than the end of it.
    private boolean continuesCopyAddFlags() {
        if (_mode == FLAG_IMAGE_REF) {
            return true;
        }
        switch (_type) {
            case FLAG: case FROM_FLAG: case FLAG_END:
            case WS: case LINE_CONTINUATION: case COMMENT:
                return true;
            default:
                return false;
        }
    }

    private boolean atLineContinuation() {
        for (int i = 1; ; i++) {
            int c = _input.LA(i);
            if (c != ' ' && c != '\t') {
                return c == '\n' || (c == '\r' && _input.LA(i + 1) == '\n');
            }
        }
    }

    // Whether what follows the '--' that both flag rules begin with is the '--from=' of a COPY or ADD
    private boolean atFromFlag() {
        if (!copyAddFlags) {
            return false;
        }
        String fromFlag = "from=";
        for (int i = 0; i < fromFlag.length(); i++) {
            int c = _input.LA(i + 1);
            if (c == -1 || Character.toLowerCase(c) != fromFlag.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}

options {
    caseInsensitive = true;
}

// Parser directives (must be at the beginning of file)
// After a parser directive, we're at line start (it consumes the newline)
PARSER_DIRECTIVE : '#' WS_CHAR* [A-Z_]+ WS_CHAR* '=' WS_CHAR* ~[\r\n]* NEWLINE_CHAR;

// Comments (after parser directives) - HIDDEN in main mode
COMMENT : '#' ~[\r\n]* -> channel(HIDDEN);

// Instructions (case-insensitive)
// Instructions are only recognized at line start. Otherwise they become UNQUOTED_TEXT.
// This eliminates ambiguity between instruction keywords and shell command text.
FROM       : 'FROM'       { if (!atLineStart) setType(UNQUOTED_TEXT); else pushMode(IMAGE_REF); };
RUN        : 'RUN'        { if (!atLineStart) setType(UNQUOTED_TEXT); };
// CMD is a keyword at line start (CMD instruction) or after HEALTHCHECK
CMD        : 'CMD'        { if (!atLineStart && !afterHealthcheck) setType(UNQUOTED_TEXT); };
// NONE is only a keyword after HEALTHCHECK
NONE       : 'NONE'       { if (!afterHealthcheck) setType(UNQUOTED_TEXT); };
LABEL      : 'LABEL'      { if (!atLineStart) setType(UNQUOTED_TEXT); };
EXPOSE     : 'EXPOSE'     { if (!atLineStart) setType(UNQUOTED_TEXT); };
ENV        : 'ENV'        { if (!atLineStart) setType(UNQUOTED_TEXT); };
ADD        : 'ADD'        { if (!atLineStart) setType(UNQUOTED_TEXT); };
COPY       : 'COPY'       { if (!atLineStart) setType(UNQUOTED_TEXT); };
ENTRYPOINT : 'ENTRYPOINT' { if (!atLineStart) setType(UNQUOTED_TEXT); };
VOLUME     : 'VOLUME'     { if (!atLineStart) setType(UNQUOTED_TEXT); };
USER       : 'USER'       { if (!atLineStart) setType(UNQUOTED_TEXT); else pushMode(USER_SPEC); };
WORKDIR    : 'WORKDIR'    { if (!atLineStart) setType(UNQUOTED_TEXT); };
ARG        : 'ARG'        { if (!atLineStart) setType(UNQUOTED_TEXT); };
ONBUILD    : 'ONBUILD'    { if (!atLineStart) setType(UNQUOTED_TEXT); };
STOPSIGNAL : 'STOPSIGNAL' { if (!atLineStart) setType(UNQUOTED_TEXT); };
HEALTHCHECK: 'HEALTHCHECK'{ if (!atLineStart) setType(UNQUOTED_TEXT); };
SHELL      : 'SHELL'      { if (!atLineStart) setType(UNQUOTED_TEXT); };
MAINTAINER : 'MAINTAINER' { if (!atLineStart) setType(UNQUOTED_TEXT); };

// Heredoc start - captures <<EOF or <<-EOF including the identifier and switches to HEREDOC_PREAMBLE mode
HEREDOC_START : '<<' '-'? [A-Z_][A-Z0-9_]* {
    // Extract and store the heredoc marker identifier in FIFO order
    String text = getText();
    int prefixLen = text.charAt(2) == '-' ? 3 : 2;
    String marker = text.substring(prefixLen);
    heredocIdentifiers.add(marker);
} -> pushMode(HEREDOC_PREAMBLE);

// Line continuation - HIDDEN in main mode
// Supports both backslash (Linux) and backtick (Windows with # escape=`)
LINE_CONTINUATION : LINE_CONT -> channel(HIDDEN);

fragment LINE_CONT : ('\\' | '`') [ \t]* '\r'? '\n';

// JSON array delimiters (for exec form) - no mode switching, handled in parser
LBRACKET : '[';
RBRACKET : ']';
COMMA    : ',';

// Assignment (used in ENV, ARG, LABEL, etc.)
EQUALS     : '=';

// Flag with optional value: --name or --name=value
// Captures the entire flag as a single token, stopping at whitespace
// This avoids the greedy flagValue+ parsing issue while keeping shell commands working
// Flag values can contain quoted strings (which may include spaces)
// The predicate excludes this rule where FROM_FLAG applies. Lexing is maximal munch, so without it
// this longer token would always win and the image reference of a --from would stay unsplit. It sits
// after the '--' because a predicate reachable without consuming anything stops the lexer caching the
// start state of the mode that holds it, which costs every token in that mode a closure computation.
FLAG : '--' {!atFromFlag()}? FLAG_BODY;

// The --from of a COPY or ADD names an image, so its value is lexed as an image reference
FROM_FLAG : '--' {atFromFlag()}? 'from=' -> pushMode(FLAG_IMAGE_REF);

fragment FLAG_BODY : [a-z] [a-z0-9_-]* ('=' FLAG_VALUE_PART+)?;
fragment FLAG_VALUE_PART
    : '"' ( '\\' ~[\r\n] | ~["\\\r\n] )* '"'   // Double-quoted string (with escapes)
    | '\'' ~['\r\n]* '\''                        // Single-quoted string (literal)
    | ~[ \t\r\n"'\\]                             // Unquoted character
    | '\\' ~[\r\n]                               // Escaped character
    ;

// Standalone -- (double dash without flag name) - used in shell commands
DASH_DASH  : '--';

// Unquoted text fragment (to be used in UNQUOTED_TEXT)
// This matches text that doesn't start with -- or <<
// Note: < is excluded to allow HEREDOC_START (<<) to match
fragment UNQUOTED_CHAR : ~[ \t\r\n\\"'$[\]=<];
fragment ESCAPED_CHAR : '\\' .;

// String literals
// Double-quoted strings support escape sequences, line continuation, and bare newlines
// Backtick followed by whitespace+newline is continuation; standalone backtick is regular char
// Bare newlines are allowed (e.g., comment lines inside PowerShell strings don't need trailing backtick)
DOUBLE_QUOTED_STRING : DQ_STRING;

fragment DQ_STRING : '"' ( ESCAPE_SEQUENCE | INLINE_CONTINUATION | '`' | [\r\n] | ~["\\\r\n`] )* '"';
// Single-quoted strings in shell are literal - no escape processing inside
// But they DO support line continuation (backslash or backtick followed by newline)
// Bare newlines are also allowed for multi-line strings
SINGLE_QUOTED_STRING : SQ_STRING;

fragment SQ_STRING : '\'' ( INLINE_CONTINUATION | [\r\n] | ~['\r\n] )* '\'';

// Inline line continuation (inside strings) - backtick or backslash followed by newline
fragment INLINE_CONTINUATION : ('\\' | '`') [ \t]* [\r\n]+;

fragment ESCAPE_SEQUENCE
    : '\\' ~[\r\n]   // Backslash followed by any char except newline (includes \n, \t, \\, \", Windows paths like \P)
    ;

// An escape character that LINE_CONT would match, longest-match-first would otherwise carry into the
// text before it. The predicate sits behind the character it qualifies: one reachable without
// consuming anything would stop ANTLR caching the start state of every mode this fragment reaches.
fragment TEXT_ESCAPE
    : '\\' {!atLineContinuation()}? ~[\r\n]
    | '`' {!atLineContinuation()}?
    ;

fragment HEX_DIGIT : [0-9A-F];

// Environment variable reference
ENV_VAR : VAR_REF;

fragment VAR_REF : '$' '{' [A-Z_][A-Z0-9_]* ( ':-' | ':+' | ':' )? ~[}]* '}' | '$' [A-Z_][A-Z0-9_]*;

// Special shell variables ($!, $$, $?, $#, $@, $*, $0-$9)
SPECIAL_VAR : SPECIAL_VAR_REF;

fragment SPECIAL_VAR_REF : '$' [!$?#@*0-9];

// Command substitution $(command) or $((arithmetic))
// Handles nested parentheses by counting them
COMMAND_SUBST : '$(' ( COMMAND_SUBST | ~[()] | '(' COMMAND_SUBST_INNER* ')' )* ')';
fragment COMMAND_SUBST_INNER : COMMAND_SUBST | ~[()];

// Backtick command substitution `command`
// First char after backtick must NOT be whitespace/newline (which would be line continuation)
// Content cannot span newlines (backtick command substitution doesn't support that)
BACKTICK_SUBST : '`' ~[ \t\r\n`] ~[`\r\n]* '`';

// Lone dollar sign that doesn't match ENV_VAR, SPECIAL_VAR, or COMMAND_SUBST
// This handles cases like $'hello' (bash ANSI-C quoting) where $ precedes a quote
DOLLAR : '$';

// Unquoted text (arguments, file paths, etc.)
// This should be after more specific tokens
// Note: comma is NOT excluded here - it's only special in JSON arrays
// We structure this to not match text starting with -- (so DASH_DASH can match first)
// Also exclude < from starting char to allow HEREDOC_START (<<) to match
UNQUOTED_TEXT
    : ( ~[-< \t\r\n\\"'$[\]=] ( UNQUOTED_CHAR | ESCAPED_CHAR )*   // Start with non-hyphen, non-<, non-space
    | '-' ~[- \t\r\n\\"'$[\]=<] ( UNQUOTED_CHAR | ESCAPED_CHAR )*  // Single hyphen followed by non-hyphen, non-space
    | '-'  // Just a hyphen by itself
    | '<' ~[< \t\r\n\\"'$[\]=] ( UNQUOTED_CHAR | ESCAPED_CHAR )*  // Single < followed by non-<
    | '<'  // Just a < by itself
    | ESCAPED_CHAR ( UNQUOTED_CHAR | ESCAPED_CHAR )*  // Start with escaped char (e.g., \; in find -exec)
    )
    ;

// Whitespace - HIDDEN in main mode
WS : WS_CHAR+ -> channel(HIDDEN);

fragment WS_CHAR : [ \t];

// Newlines - HIDDEN in main mode
NEWLINE : NEWLINE_CHAR+ -> channel(HIDDEN);

fragment NEWLINE_CHAR : [\r\n];

// ----------------------------------------------------------------------------------------------
// IMAGE_REF mode - the image reference of a FROM instruction, i.e. name:tag@digest
// Entered from the FROM keyword and left at AS or at the end of the line. Only here are ':' and '@'
// their own tokens, so the parser can split the reference while a colon inside a quoted string, a
// variable reference or a registry port ('host:5000/img') stays part of the image name.
// ----------------------------------------------------------------------------------------------
mode IMAGE_REF;

IR_WS                : WS_CHAR+      -> type(WS), channel(HIDDEN);
IR_LINE_CONTINUATION : LINE_CONT     -> type(LINE_CONTINUATION), channel(HIDDEN);
IR_COMMENT           : '#' ~[\r\n]*  -> type(COMMENT), channel(HIDDEN);
IR_NEWLINE           : NEWLINE_CHAR+ -> type(NEWLINE), channel(HIDDEN), popMode;

COLON : ':';
AT    : '@';

// AS ends the reference; the stage name that follows it is ordinary text
AS : 'AS' -> popMode;

IR_FLAG                 : '--' FLAG_BODY  -> type(FLAG);
IR_DOUBLE_QUOTED_STRING : DQ_STRING       -> type(DOUBLE_QUOTED_STRING);
IR_SINGLE_QUOTED_STRING : SQ_STRING       -> type(SINGLE_QUOTED_STRING);
IR_ENV_VAR              : VAR_REF         -> type(ENV_VAR);
IR_SPECIAL_VAR          : SPECIAL_VAR_REF -> type(SPECIAL_VAR);
IR_DOLLAR               : '$'             -> type(DOLLAR);

// Text of one part of the reference. A colon that a '/' follows belongs to a registry port rather
// than to a tag ('host:5000/img:tag'), so it stays inside the token.
IR_UNQUOTED_TEXT : IR_TEXT -> type(UNQUOTED_TEXT);

// Shared with FLAG_IMAGE_REF, which reads the same reference.
fragment IR_TEXT       : ( IR_TEXT_CHAR | TEXT_ESCAPE | IR_PORT_COLON )+;
fragment IR_TEXT_CHAR  : ~[:@ \t\r\n\\"'$`];
fragment IR_PORT_COLON : ':' ( IR_TEXT_CHAR | ':' )* '/';

// ----------------------------------------------------------------------------------------------
// FLAG_IMAGE_REF mode - the image reference carried by the --from flag of a COPY or ADD
// As IMAGE_REF, except that the reference ends at the whitespace before the paths that follow it
// rather than at the end of the line, and AS is not a keyword because no stage alias can appear here.
// ----------------------------------------------------------------------------------------------
mode FLAG_IMAGE_REF;

// The end of the reference is a token of its own rather than hidden whitespace: popping a mode does
// not bound a parser rule, so without a token to name the `imageName` of `COPY --from=build --link .`
// would carry on into the flag that follows it.
FLAG_END : ( WS_CHAR+ | LINE_CONT ) -> popMode;

FIR_NEWLINE : NEWLINE_CHAR+ -> type(NEWLINE), channel(HIDDEN), popMode;

FIR_COLON : ':' -> type(COLON);
FIR_AT    : '@' -> type(AT);

FIR_DOUBLE_QUOTED_STRING : DQ_STRING       -> type(DOUBLE_QUOTED_STRING);
FIR_SINGLE_QUOTED_STRING : SQ_STRING       -> type(SINGLE_QUOTED_STRING);
FIR_ENV_VAR              : VAR_REF         -> type(ENV_VAR);
FIR_SPECIAL_VAR          : SPECIAL_VAR_REF -> type(SPECIAL_VAR);
FIR_DOLLAR               : '$'             -> type(DOLLAR);

FIR_UNQUOTED_TEXT : IR_TEXT -> type(UNQUOTED_TEXT);

// ----------------------------------------------------------------------------------------------
// USER_SPEC mode - the user:group of a USER instruction
// Entered from the USER keyword and left at the end of the line. As IMAGE_REF, minus the '@' and the
// AS exit: only here is ':' its own token, so the parser can split the specification while a colon
// inside a quoted string or a variable reference stays part of the name that holds it.
// ----------------------------------------------------------------------------------------------
mode USER_SPEC;

US_WS                : WS_CHAR+     -> type(WS), channel(HIDDEN);
US_LINE_CONTINUATION : LINE_CONT    -> type(LINE_CONTINUATION), channel(HIDDEN);
US_COMMENT           : '#' ~[\r\n]* -> type(COMMENT), channel(HIDDEN);
US_NEWLINE           : NEWLINE_CHAR+ -> type(NEWLINE), channel(HIDDEN), popMode;

US_COLON : ':' -> type(COLON);

US_DOUBLE_QUOTED_STRING : DQ_STRING       -> type(DOUBLE_QUOTED_STRING);
US_SINGLE_QUOTED_STRING : SQ_STRING       -> type(SINGLE_QUOTED_STRING);
US_ENV_VAR              : VAR_REF         -> type(ENV_VAR);
US_SPECIAL_VAR          : SPECIAL_VAR_REF -> type(SPECIAL_VAR);
US_DOLLAR               : '$'             -> type(DOLLAR);

US_UNQUOTED_TEXT : ( US_TEXT_CHAR | TEXT_ESCAPE )+ -> type(UNQUOTED_TEXT);

fragment US_TEXT_CHAR : ~[: \t\r\n\\"'$`];

// ----------------------------------------------------------------------------------------------
// HEREDOC_PREAMBLE mode - for parsing shell command preamble after heredoc marker(s)
// The heredoc identifier (e.g., EOF) is already captured in HEREDOC_START
// This mode handles the shell command text including additional heredoc markers for multi-heredoc.
// ----------------------------------------------------------------------------------------------
mode HEREDOC_PREAMBLE;

// Line continuation in preamble - stay in HEREDOC_PREAMBLE mode
HP_LINE_CONTINUATION : LINE_CONT -> channel(HIDDEN);

// Newline without continuation - transition to HEREDOC mode for body content
HP_NEWLINE : '\n' -> type(NEWLINE), mode(HEREDOC);

HP_WS      : [ \t\r\u000C]+ -> channel(HIDDEN);
HP_COMMENT : '/*' .*? '*/'  -> channel(HIDDEN);
HP_LINE_COMMENT : ('//' | '#') ~[\r\n]* '\r'? -> channel(HIDDEN);

// Additional heredoc marker in preamble (for multi-heredoc support)
HP_HEREDOC_START : '<<' '-'? [A-Z_][A-Z0-9_]* {
    // Extract and store the heredoc marker identifier in FIFO order
    String text = getText();
    int prefixLen = text.charAt(2) == '-' ? 3 : 2;
    String marker = text.substring(prefixLen);
    heredocIdentifiers.add(marker);
} -> type(HEREDOC_START);

// Any text on the heredoc line after the marker (destination paths, interpreter names, shell commands, etc.)
// Exclude < to allow HP_HEREDOC_START to match <<
// Exclude \ and ` to allow HP_LINE_CONTINUATION to match
HP_UNQUOTED_TEXT : ( ~[<\\` \t\r\n]+
                   | '<' ~[< \t\r\n] ~[ \t\r\n]*  // single < followed by non-< char
                   | '<'  // standalone <
                   ) -> type(UNQUOTED_TEXT);

// ----------------------------------------------------------------------------------------------
// HEREDOC mode - for parsing heredoc content
// Supports multiple heredocs by only popping mode when all markers have been matched.
// ----------------------------------------------------------------------------------------------
mode HEREDOC;

H_NEWLINE : '\n' -> type(NEWLINE);

// Match heredoc content lines - emit as HEREDOC_CONTENT unless it's an ending identifier
// For multi-heredoc, we only popMode when the queue is empty (all markers matched in FIFO order)
HEREDOC_CONTENT : ~[\n]+
{
  if(!heredocIdentifiers.isEmpty() && getText().equals(heredocIdentifiers.peek())) {
      setType(UNQUOTED_TEXT);
      heredocIdentifiers.poll();  // Remove from front of queue (FIFO)
      // Only pop mode when all heredoc markers have been matched
      if(heredocIdentifiers.isEmpty()) {
          popMode();
      }
  }
};

