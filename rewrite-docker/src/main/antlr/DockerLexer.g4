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
    private boolean heredocIdentifierCaptured = false;
    // Track if we're at the start of a logical line (where instructions can appear)
    private boolean atLineStart = true;
    // Track if we're after HEALTHCHECK to recognize CMD/NONE as keywords
    private boolean afterHealthcheck = false;
    // Track if we're in the flag section of a COPY or ADD, where --from carries an image reference
    private boolean copyAddFlags = false;

    // Every flag that is scoped to one logical line is cleared here, so that a mode of its own does
    // not have to remember which of them the newline it matches instead of NEWLINE should reset
    private void resetLine() {
        atLineStart = true;
        afterHealthcheck = false;
        copyAddFlags = false;
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
PARSER_DIRECTIVE : '#' WS_CHAR* [A-Z_]+ WS_CHAR* '=' WS_CHAR* ~[\r\n]* NEWLINE_CHAR { atLineStart = true; };

// Comments (after parser directives) - HIDDEN in main mode
COMMENT : '#' ~[\r\n]* -> channel(HIDDEN);

// Instructions (case-insensitive)
// Instructions are only recognized at line start. Otherwise they become UNQUOTED_TEXT.
// This eliminates ambiguity between instruction keywords and shell command text.
FROM       : 'FROM'       { if (!atLineStart) setType(UNQUOTED_TEXT); else pushMode(IMAGE_REF); atLineStart = false; };
RUN        : 'RUN'        { if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; };
// CMD is a keyword at line start (CMD instruction) or after HEALTHCHECK
CMD        : 'CMD'        { if (!atLineStart && !afterHealthcheck) setType(UNQUOTED_TEXT); atLineStart = false; afterHealthcheck = false; };
// NONE is only a keyword after HEALTHCHECK
NONE       : 'NONE'       { if (!afterHealthcheck) setType(UNQUOTED_TEXT); atLineStart = false; afterHealthcheck = false; };
LABEL      : 'LABEL'      { if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; };
EXPOSE     : 'EXPOSE'     { if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; };
ENV        : 'ENV'        { if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; };
ADD        : 'ADD'        { if (!atLineStart) setType(UNQUOTED_TEXT); else copyAddFlags = true; atLineStart = false; };
COPY       : 'COPY'       { if (!atLineStart) setType(UNQUOTED_TEXT); else copyAddFlags = true; atLineStart = false; };
ENTRYPOINT : 'ENTRYPOINT' { if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; };
VOLUME     : 'VOLUME'     { if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; };
USER       : 'USER'       { if (!atLineStart) setType(UNQUOTED_TEXT); else pushMode(USER_SPEC); atLineStart = false; };
WORKDIR    : 'WORKDIR'    { if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; };
ARG        : 'ARG'        { if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; };
// ONBUILD is special: it keeps atLineStart true so the following instruction is recognized
ONBUILD    : 'ONBUILD'    { if (!atLineStart) setType(UNQUOTED_TEXT); /* atLineStart stays true */ };
STOPSIGNAL : 'STOPSIGNAL' { if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; };
// HEALTHCHECK is special: it keeps atLineStart true and sets afterHealthcheck so CMD/NONE are recognized after flags
HEALTHCHECK: 'HEALTHCHECK'{ if (!atLineStart) setType(UNQUOTED_TEXT); else afterHealthcheck = true; /* atLineStart stays true */ };
SHELL      : 'SHELL'      { if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; };
MAINTAINER : 'MAINTAINER' { if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; };

// Heredoc start - captures <<EOF or <<-EOF including the identifier and switches to HEREDOC_PREAMBLE mode
HEREDOC_START : '<<' '-'? [A-Z_][A-Z0-9_]* {
    // Extract and store the heredoc marker identifier in FIFO order
    String text = getText();
    int prefixLen = text.charAt(2) == '-' ? 3 : 2;
    String marker = text.substring(prefixLen);
    heredocIdentifiers.add(marker);
    heredocIdentifierCaptured = true;
    atLineStart = false;
} -> pushMode(HEREDOC_PREAMBLE);

// Line continuation - HIDDEN in main mode
// Supports both backslash (Linux) and backtick (Windows with # escape=`)
LINE_CONTINUATION : LINE_CONT -> channel(HIDDEN);

fragment LINE_CONT : ('\\' | '`') [ \t]* '\r'? '\n';

// JSON array delimiters (for exec form) - no mode switching, handled in parser
LBRACKET : '[' { atLineStart = false; };
RBRACKET : ']' { atLineStart = false; };
COMMA    : ',' { atLineStart = false; };

// Assignment (used in ENV, ARG, LABEL, etc.)
EQUALS     : '=' { if (!afterHealthcheck) atLineStart = false; };

// Flag with optional value: --name or --name=value
// Captures the entire flag as a single token, stopping at whitespace
// This avoids the greedy flagValue+ parsing issue while keeping shell commands working
// Flag values can contain quoted strings (which may include spaces)
// The predicate excludes this rule where FROM_FLAG applies. Lexing is maximal munch, so without it
// this longer token would always win and the image reference of a --from would stay unsplit. It sits
// after the '--' because a predicate reachable without consuming anything stops the lexer caching the
// start state of the mode that holds it, which costs every token in that mode a closure computation.
FLAG : '--' {!atFromFlag()}? FLAG_BODY { if (!afterHealthcheck) atLineStart = false; };

// The --from of a COPY or ADD names an image, so its value is lexed as an image reference
FROM_FLAG : '--' {atFromFlag()}? 'from=' { atLineStart = false; } -> pushMode(FLAG_IMAGE_REF);

fragment FLAG_BODY : [a-z] [a-z0-9_-]* ('=' FLAG_VALUE_PART+)?;
fragment FLAG_VALUE_PART
    : '"' ( '\\' ~[\r\n] | ~["\\\r\n] )* '"'   // Double-quoted string (with escapes)
    | '\'' ~['\r\n]* '\''                        // Single-quoted string (literal)
    | ~[ \t\r\n"'\\]                             // Unquoted character
    | '\\' ~[\r\n]                               // Escaped character
    ;

// Standalone -- (double dash without flag name) - used in shell commands
DASH_DASH  : '--' { if (!afterHealthcheck) atLineStart = false; };

// Unquoted text fragment (to be used in UNQUOTED_TEXT)
// This matches text that doesn't start with -- or <<
// Note: < is excluded to allow HEREDOC_START (<<) to match
fragment UNQUOTED_CHAR : ~[ \t\r\n\\"'$[\]=<];
fragment ESCAPED_CHAR : '\\' .;

// String literals
// Double-quoted strings support escape sequences, line continuation, and bare newlines
// Backtick followed by whitespace+newline is continuation; standalone backtick is regular char
// Bare newlines are allowed (e.g., comment lines inside PowerShell strings don't need trailing backtick)
DOUBLE_QUOTED_STRING : DQ_STRING { if (!afterHealthcheck) atLineStart = false; };

fragment DQ_STRING : '"' ( ESCAPE_SEQUENCE | INLINE_CONTINUATION | '`' | [\r\n] | ~["\\\r\n`] )* '"';
// Single-quoted strings in shell are literal - no escape processing inside
// But they DO support line continuation (backslash or backtick followed by newline)
// Bare newlines are also allowed for multi-line strings
SINGLE_QUOTED_STRING : SQ_STRING { if (!afterHealthcheck) atLineStart = false; };

fragment SQ_STRING : '\'' ( INLINE_CONTINUATION | [\r\n] | ~['\r\n] )* '\'';

// Inline line continuation (inside strings) - backtick or backslash followed by newline
fragment INLINE_CONTINUATION : ('\\' | '`') [ \t]* [\r\n]+;

fragment ESCAPE_SEQUENCE
    : '\\' ~[\r\n]   // Backslash followed by any char except newline (includes \n, \t, \\, \", Windows paths like \P)
    ;

fragment HEX_DIGIT : [0-9A-F];

// Environment variable reference
ENV_VAR : VAR_REF { atLineStart = false; };

fragment VAR_REF : '$' '{' [A-Z_][A-Z0-9_]* ( ':-' | ':+' | ':' )? ~[}]* '}' | '$' [A-Z_][A-Z0-9_]*;

// Special shell variables ($!, $$, $?, $#, $@, $*, $0-$9)
SPECIAL_VAR : SPECIAL_VAR_REF { atLineStart = false; };

fragment SPECIAL_VAR_REF : '$' [!$?#@*0-9];

// Command substitution $(command) or $((arithmetic))
// Handles nested parentheses by counting them
COMMAND_SUBST : '$(' ( COMMAND_SUBST | ~[()] | '(' COMMAND_SUBST_INNER* ')' )* ')' { atLineStart = false; };
fragment COMMAND_SUBST_INNER : COMMAND_SUBST | ~[()];

// Backtick command substitution `command`
// First char after backtick must NOT be whitespace/newline (which would be line continuation)
// Content cannot span newlines (backtick command substitution doesn't support that)
BACKTICK_SUBST : '`' ~[ \t\r\n`] ~[`\r\n]* '`' { atLineStart = false; };

// Lone dollar sign that doesn't match ENV_VAR, SPECIAL_VAR, or COMMAND_SUBST
// This handles cases like $'hello' (bash ANSI-C quoting) where $ precedes a quote
DOLLAR : '$' { atLineStart = false; };

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
    ) { if (!afterHealthcheck) atLineStart = false; copyAddFlags = false; }
    ;

// Whitespace - HIDDEN in main mode
WS : WS_CHAR+ -> channel(HIDDEN);

fragment WS_CHAR : [ \t];

// Newlines - HIDDEN in main mode, reset state for next line
NEWLINE : NEWLINE_CHAR+ { resetLine(); } -> channel(HIDDEN);

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
IR_NEWLINE           : NEWLINE_CHAR+ { resetLine(); } -> type(NEWLINE), channel(HIDDEN), popMode;

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

// Shared with FLAG_IMAGE_REF, which reads the same reference. ESCAPE_SEQUENCE rather than
// ESCAPED_CHAR because it stops before a newline, which lexing longest-match-first would otherwise
// take into the token and so hide the line continuation that ends the reference.
fragment IR_TEXT       : ( IR_TEXT_CHAR | ESCAPE_SEQUENCE | IR_PORT_COLON )+;
fragment IR_TEXT_CHAR  : ~[:@ \t\r\n\\"'$];
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

FIR_NEWLINE : NEWLINE_CHAR+ { resetLine(); } -> type(NEWLINE), channel(HIDDEN), popMode;

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
US_NEWLINE           : NEWLINE_CHAR+ { resetLine(); } -> type(NEWLINE), channel(HIDDEN), popMode;

US_COLON : ':' -> type(COLON);

US_DOUBLE_QUOTED_STRING : DQ_STRING       -> type(DOUBLE_QUOTED_STRING);
US_SINGLE_QUOTED_STRING : SQ_STRING       -> type(SINGLE_QUOTED_STRING);
US_ENV_VAR              : VAR_REF         -> type(ENV_VAR);
US_SPECIAL_VAR          : SPECIAL_VAR_REF -> type(SPECIAL_VAR);
US_DOLLAR               : '$'             -> type(DOLLAR);

US_UNQUOTED_TEXT : ( US_TEXT_CHAR | ESCAPED_CHAR )+ -> type(UNQUOTED_TEXT);

fragment US_TEXT_CHAR : ~[: \t\r\n\\"'$];

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
          atLineStart = true;  // After heredoc ends, next line is at line start
      }
  }
};

