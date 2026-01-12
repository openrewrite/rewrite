// $antlr-format alignTrailingComments true, columnLimit 150, maxEmptyLinesToKeep 1, reflowComments false, useTab false
// $antlr-format allowShortRulesOnASingleLine true, allowShortBlocksOnASingleLine true, minEmptyLines 0, alignSemicolons ownLine
// $antlr-format alignColons trailing, singleLineOverrulesHangingColon true, alignLexerCommands true, alignLabels true, alignTrailers true

lexer grammar DockerLexer;

@lexer::header
{import java.util.Stack;}

@lexer::members
{
    private Stack<String> heredocIdentifier = new Stack<String>();
    private boolean heredocIdentifierCaptured = false;
    // Track if we're at the start of a logical line (where instructions can appear)
    private boolean atLineStart = true;
    // Track if we're after FROM to recognize AS as a keyword (for stage aliasing)
    private boolean afterFrom = false;
    // Track if we're after HEALTHCHECK to recognize CMD/NONE as keywords
    private boolean afterHealthcheck = false;
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
FROM       : 'FROM'       { if (!atLineStart) setType(UNQUOTED_TEXT); else afterFrom = true; atLineStart = false; };
RUN        : 'RUN'        { if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; };
// CMD is a keyword at line start (CMD instruction) or after HEALTHCHECK
CMD        : 'CMD'        { if (!atLineStart && !afterHealthcheck) setType(UNQUOTED_TEXT); atLineStart = false; afterHealthcheck = false; };
// NONE is only a keyword after HEALTHCHECK
NONE       : 'NONE'       { if (!afterHealthcheck) setType(UNQUOTED_TEXT); atLineStart = false; afterHealthcheck = false; };
LABEL      : 'LABEL'      { if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; };
EXPOSE     : 'EXPOSE'     { if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; };
ENV        : 'ENV'        { if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; };
ADD        : 'ADD'        { if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; };
COPY       : 'COPY'       { if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; };
ENTRYPOINT : 'ENTRYPOINT' { if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; };
VOLUME     : 'VOLUME'     { if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; };
USER       : 'USER'       { if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; };
WORKDIR    : 'WORKDIR'    { if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; };
ARG        : 'ARG'        { if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; };
// ONBUILD is special: it keeps atLineStart true so the following instruction is recognized
ONBUILD    : 'ONBUILD'    { if (!atLineStart) setType(UNQUOTED_TEXT); /* atLineStart stays true */ };
STOPSIGNAL : 'STOPSIGNAL' { if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; };
// HEALTHCHECK is special: it keeps atLineStart true and sets afterHealthcheck so CMD/NONE are recognized after flags
HEALTHCHECK: 'HEALTHCHECK'{ if (!atLineStart) setType(UNQUOTED_TEXT); else afterHealthcheck = true; /* atLineStart stays true */ };
SHELL      : 'SHELL'      { if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; };
MAINTAINER : 'MAINTAINER' { if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; };

// AS is only a keyword after FROM (for stage aliasing)
AS         : 'AS' { if (!afterFrom) setType(UNQUOTED_TEXT); atLineStart = false; afterFrom = false; };

// Heredoc start - captures <<EOF or <<-EOF including the identifier and switches to HEREDOC_PREAMBLE mode
HEREDOC_START : '<<' '-'? [A-Z_][A-Z0-9_]* {
    // Extract and store the heredoc marker identifier
    String text = getText();
    int prefixLen = text.charAt(2) == '-' ? 3 : 2;
    String marker = text.substring(prefixLen);
    heredocIdentifier.push(marker);
    heredocIdentifierCaptured = true;
    atLineStart = false;
} -> pushMode(HEREDOC_PREAMBLE);

// Line continuation - HIDDEN in main mode
// Supports both backslash (Linux) and backtick (Windows with # escape=`)
LINE_CONTINUATION : ('\\' | '`') [ \t]* NEWLINE_CHAR -> channel(HIDDEN);

// JSON array delimiters (for exec form) - no mode switching, handled in parser
LBRACKET : '[' { atLineStart = false; };
RBRACKET : ']' { atLineStart = false; };
COMMA    : ',' { atLineStart = false; };

// Assignment (used in ENV, ARG, LABEL, etc.)
EQUALS     : '=' { if (!afterHealthcheck) atLineStart = false; };

// Flag with optional value: --name or --name=value
// Captures the entire flag as a single token, stopping at whitespace
// This avoids the greedy flagValue+ parsing issue while keeping shell commands working
FLAG : '--' [a-zA-Z] [a-zA-Z0-9_-]* ('=' ~[ \t\r\n]+)? { if (!afterHealthcheck) atLineStart = false; };

// Standalone -- (double dash without flag name) - used in shell commands
DASH_DASH  : '--' { if (!afterHealthcheck) atLineStart = false; };

// Unquoted text fragment (to be used in UNQUOTED_TEXT)
// This matches text that doesn't start with -- or <<
// Note: < is excluded to allow HEREDOC_START (<<) to match
fragment UNQUOTED_CHAR : ~[ \t\r\n\\"'$[\]=<];
fragment ESCAPED_CHAR : '\\' .;

// String literals
// Double-quoted strings support escape sequences and line continuation (backslash or backtick)
// Backtick followed by whitespace+newline is continuation; standalone backtick is regular char
DOUBLE_QUOTED_STRING : '"' ( ESCAPE_SEQUENCE | INLINE_CONTINUATION | '`' | ~["\\\r\n`] )* '"' { if (!afterHealthcheck) atLineStart = false; };
// Single-quoted strings in shell are literal - no escape processing inside
// But they DO support line continuation (backslash or backtick followed by newline)
SINGLE_QUOTED_STRING : '\'' ( INLINE_CONTINUATION | ~['\r\n] )* '\'' { if (!afterHealthcheck) atLineStart = false; };

// Inline line continuation (inside strings) - backtick or backslash followed by newline
fragment INLINE_CONTINUATION : ('\\' | '`') [ \t]* [\r\n]+;

fragment ESCAPE_SEQUENCE
    : '\\' ~[\r\n]   // Backslash followed by any char except newline (includes \n, \t, \\, \", Windows paths like \P)
    ;

fragment HEX_DIGIT : [0-9A-F];

// Environment variable reference
ENV_VAR : ('$' '{' [A-Z_][A-Z0-9_]* ( ':-' | ':+' | ':' )? ~[}]* '}' | '$' [A-Z_][A-Z0-9_]*) { atLineStart = false; };

// Special shell variables ($!, $$, $?, $#, $@, $*, $0-$9)
SPECIAL_VAR : '$' [!$?#@*0-9] { atLineStart = false; };

// Command substitution $(command) or $((arithmetic))
// Handles nested parentheses by counting them
COMMAND_SUBST : '$(' ( COMMAND_SUBST | ~[()] | '(' COMMAND_SUBST_INNER* ')' )* ')' { atLineStart = false; };
fragment COMMAND_SUBST_INNER : COMMAND_SUBST | ~[()];

// Backtick command substitution `command`
// First char after backtick must NOT be whitespace/newline (which would be line continuation)
// Content cannot span newlines (backtick command substitution doesn't support that)
BACKTICK_SUBST : '`' ~[ \t\r\n`] ~[`\r\n]* '`' { atLineStart = false; };

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
    ) { if (!afterHealthcheck) atLineStart = false; }
    ;

// Whitespace - HIDDEN in main mode
WS : WS_CHAR+ -> channel(HIDDEN);

fragment WS_CHAR : [ \t];

// Newlines - HIDDEN in main mode, reset state for next line
NEWLINE : NEWLINE_CHAR+ { atLineStart = true; afterFrom = false; afterHealthcheck = false; } -> channel(HIDDEN);

fragment NEWLINE_CHAR : [\r\n];

// ----------------------------------------------------------------------------------------------
// HEREDOC_PREAMBLE mode - for parsing optional destination path after heredoc marker
// The heredoc identifier (e.g., EOF) is already captured in HEREDOC_START
// ----------------------------------------------------------------------------------------------
mode HEREDOC_PREAMBLE;

HP_NEWLINE : '\n' -> type(NEWLINE), mode(HEREDOC);
HP_WS      : [ \t\r\u000C]+ -> channel(HIDDEN);
HP_COMMENT : '/*' .*? '*/'  -> channel(HIDDEN);
HP_LINE_COMMENT : ('//' | '#') ~[\r\n]* '\r'? -> channel(HIDDEN);

// Any text on the heredoc line after the marker (destination paths, interpreter names, etc.)
HP_UNQUOTED_TEXT : ~[ \t\r\n]+ -> type(UNQUOTED_TEXT);

// ----------------------------------------------------------------------------------------------
// HEREDOC mode - for parsing heredoc content
// ----------------------------------------------------------------------------------------------
mode HEREDOC;

H_NEWLINE : '\n' -> type(NEWLINE);

// Match heredoc content lines - emit as HEREDOC_CONTENT unless it's the ending identifier
HEREDOC_CONTENT : ~[\n]+
{
  if(!heredocIdentifier.isEmpty() && getText().equals(heredocIdentifier.peek())) {
      setType(UNQUOTED_TEXT);
      heredocIdentifier.pop();
      popMode();
      atLineStart = true;  // After heredoc ends, next line is at line start
  }
};

