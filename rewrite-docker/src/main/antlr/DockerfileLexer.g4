// $antlr-format alignTrailingComments true, columnLimit 150, maxEmptyLinesToKeep 1, reflowComments false, useTab false
// $antlr-format allowShortRulesOnASingleLine true, allowShortBlocksOnASingleLine true, minEmptyLines 0, alignSemicolons ownLine
// $antlr-format alignColons trailing, singleLineOverrulesHangingColon true, alignLexerCommands true, alignLabels true, alignTrailers true

lexer grammar DockerfileLexer;

// Parser directives (must be at the beginning of file)
PARSER_DIRECTIVE : '#' WS_CHAR* [a-zA-Z_]+ WS_CHAR* '=' WS_CHAR* ~[\r\n]* NEWLINE_CHAR;

// Comments (after parser directives)
COMMENT : '#' ~[\r\n]*;

// Instructions (case-insensitive)
FROM       : [Ff][Rr][Oo][Mm];
RUN        : [Rr][Uu][Nn];
CMD        : [Cc][Mm][Dd];
LABEL      : [Ll][Aa][Bb][Ee][Ll];
EXPOSE     : [Ee][Xx][Pp][Oo][Ss][Ee];
ENV        : [Ee][Nn][Vv];
ADD        : [Aa][Dd][Dd];
COPY       : [Cc][Oo][Pp][Yy];
ENTRYPOINT : [Ee][Nn][Tt][Rr][Yy][Pp][Oo][Ii][Nn][Tt];
VOLUME     : [Vv][Oo][Ll][Uu][Mm][Ee];
USER       : [Uu][Ss][Ee][Rr];
WORKDIR    : [Ww][Oo][Rr][Kk][Dd][Ii][Rr];
ARG        : [Aa][Rr][Gg];
ONBUILD    : [Oo][Nn][Bb][Uu][Ii][Ll][Dd];
STOPSIGNAL : [Ss][Tt][Oo][Pp][Ss][Ii][Gg][Nn][Aa][Ll];
HEALTHCHECK: [Hh][Ee][Aa][Ll][Tt][Hh][Cc][Hh][Ee][Cc][Kk];
SHELL      : [Ss][Hh][Ee][Ll][Ll];
MAINTAINER : [Mm][Aa][Ii][Nn][Tt][Aa][Ii][Nn][Ee][Rr];

// Special keywords
AS         : [Aa][Ss];

// Heredoc start - captures <<EOF or <<-EOF (no mode switch, handled by parser)
HEREDOC_START : '<<' '-'? [A-Za-z_][A-Za-z0-9_]*;

// Line continuation
LINE_CONTINUATION : '\\' [ \t]* NEWLINE_CHAR;

// JSON array delimiters (for exec form)
LBRACKET : '[' -> pushMode(JSON_MODE);
RBRACKET : ']';

// Assignment and flags
EQUALS     : '=';
DASH_DASH  : '--';

// Unquoted text fragment (to be used in UNQUOTED_TEXT)
// This matches text that doesn't start with --
fragment UNQUOTED_CHAR : ~[ \t\r\n\\"'$[\]=];
fragment ESCAPED_CHAR : '\\' .;

// String literals
DOUBLE_QUOTED_STRING : '"' ( ESCAPE_SEQUENCE | ~["\\\r\n] )* '"';
SINGLE_QUOTED_STRING : '\'' ( ESCAPE_SEQUENCE | ~['\\\r\n] )* '\'';

fragment ESCAPE_SEQUENCE
    : '\\' [nrt"'\\$]
    | '\\' 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
    | '\\' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
    ;

fragment HEX_DIGIT : [0-9a-fA-F];

// Environment variable reference
ENV_VAR : '$' '{' [a-zA-Z_][a-zA-Z0-9_]* ( ':-' | ':+' | ':' )? ~[}]* '}' | '$' [a-zA-Z_][a-zA-Z0-9_]*;

// Unquoted text (arguments, file paths, etc.)
// This should be after more specific tokens
// Note: comma is NOT excluded here - it's only special in JSON arrays
// We structure this to not match text starting with -- (so DASH_DASH can match first)
UNQUOTED_TEXT
    : ~[- \t\r\n\\"'$[\]=] ( UNQUOTED_CHAR | ESCAPED_CHAR )*   // Start with non-hyphen, non-space
    | '-' ~[- \t\r\n\\"'$[\]=] ( UNQUOTED_CHAR | ESCAPED_CHAR )*  // Single hyphen followed by non-hyphen, non-space
    | '-'  // Just a hyphen by itself
    ;

// Whitespace (preserve for LST)
WS : WS_CHAR+ ;

fragment WS_CHAR : [ \t];

// Newlines (preserve for LST)
NEWLINE : NEWLINE_CHAR+;

fragment NEWLINE_CHAR : [\r\n];

// JSON mode - for parsing JSON arrays in exec form
mode JSON_MODE;

// Comma separator in JSON arrays
JSON_COMMA : ',' ;

// Closing bracket - exit JSON mode
JSON_RBRACKET : ']' -> popMode;

// JSON strings (only double-quoted allowed in JSON)
JSON_STRING : '"' ( ESCAPE_SEQUENCE | ~["\\\r\n] )* '"';

// Whitespace in JSON arrays
JSON_WS : WS_CHAR+ ;
