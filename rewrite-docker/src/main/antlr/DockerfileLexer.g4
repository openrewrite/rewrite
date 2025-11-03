// $antlr-format alignTrailingComments true, columnLimit 150, maxEmptyLinesToKeep 1, reflowComments false, useTab false
// $antlr-format allowShortRulesOnASingleLine true, allowShortBlocksOnASingleLine true, minEmptyLines 0, alignSemicolons ownLine
// $antlr-format alignColons trailing, singleLineOverrulesHangingColon true, alignLexerCommands true, alignLabels true, alignTrailers true

lexer grammar DockerfileLexer;

@lexer::header
{import java.util.Stack;}

@lexer::members
{
    private Stack<String> heredocIdentifier = new Stack<String>();
}

options {
    caseInsensitive = true;
}

// Parser directives (must be at the beginning of file)
PARSER_DIRECTIVE : '#' WS_CHAR* [A-Z_]+ WS_CHAR* '=' WS_CHAR* ~[\r\n]* NEWLINE_CHAR;

// Comments (after parser directives) - HIDDEN in main mode
COMMENT : '#' ~[\r\n]* -> channel(HIDDEN);

// Instructions (case-insensitive)
FROM       : 'FROM';
RUN        : 'RUN';
CMD        : 'CMD';
LABEL      : 'LABEL';
EXPOSE     : 'EXPOSE';
ENV        : 'ENV';
ADD        : 'ADD';
COPY       : 'COPY';
ENTRYPOINT : 'ENTRYPOINT';
VOLUME     : 'VOLUME';
USER       : 'USER';
WORKDIR    : 'WORKDIR';
ARG        : 'ARG';
ONBUILD    : 'ONBUILD';
STOPSIGNAL : 'STOPSIGNAL';
HEALTHCHECK: 'HEALTHCHECK';
SHELL      : 'SHELL';
MAINTAINER : 'MAINTAINER';

// Special keywords
AS         : 'AS';

// Heredoc start - captures <<EOF or <<-EOF and switches to HEREDOC_PREAMBLE mode
HEREDOC_START : '<<' '-'? -> pushMode(HEREDOC_PREAMBLE);

// Line continuation - HIDDEN in main mode
LINE_CONTINUATION : '\\' [ \t]* NEWLINE_CHAR -> channel(HIDDEN);

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

fragment HEX_DIGIT : [0-9A-F];

// Environment variable reference
ENV_VAR : '$' '{' [A-Z_][A-Z0-9_]* ( ':-' | ':+' | ':' )? ~[}]* '}' | '$' [A-Z_][A-Z0-9_]*;

// Unquoted text (arguments, file paths, etc.)
// This should be after more specific tokens
// Note: comma is NOT excluded here - it's only special in JSON arrays
// We structure this to not match text starting with -- (so DASH_DASH can match first)
UNQUOTED_TEXT
    : ~[- \t\r\n\\"'$[\]=] ( UNQUOTED_CHAR | ESCAPED_CHAR )*   // Start with non-hyphen, non-space
    | '-' ~[- \t\r\n\\"'$[\]=] ( UNQUOTED_CHAR | ESCAPED_CHAR )*  // Single hyphen followed by non-hyphen, non-space
    | '-'  // Just a hyphen by itself
    ;

// Whitespace - HIDDEN in main mode
WS : WS_CHAR+ -> channel(HIDDEN);

fragment WS_CHAR : [ \t];

// Newlines - HIDDEN in main mode
NEWLINE : NEWLINE_CHAR+ -> channel(HIDDEN);

fragment NEWLINE_CHAR : [\r\n];

// ----------------------------------------------------------------------------------------------
// JSON mode - for parsing JSON arrays in exec form
// ----------------------------------------------------------------------------------------------
mode JSON_MODE;

// Comma separator in JSON arrays
JSON_COMMA : ',' ;

// Closing bracket - exit JSON mode
JSON_RBRACKET : ']' -> popMode;

// JSON strings (only double-quoted allowed in JSON)
JSON_STRING : '"' ( ESCAPE_SEQUENCE | ~["\\\r\n] )* '"';

// Whitespace in JSON arrays
JSON_WS : WS_CHAR+ -> channel(HIDDEN);

// ----------------------------------------------------------------------------------------------
// HEREDOC_PREAMBLE mode - for parsing the heredoc identifier and optional flags
// ----------------------------------------------------------------------------------------------
mode HEREDOC_PREAMBLE;

HP_NEWLINE : '\n' -> type(NEWLINE), mode(HEREDOC);
HP_WS      : [ \t\r\u000C]+ -> channel(HIDDEN);
HP_COMMENT : '/*' .*? '*/'  -> channel(HIDDEN);
HP_LINE_COMMENT : ('//' | '#') ~[\r\n]* '\r'? -> channel(HIDDEN);

HPIdentifier : [A-Z_][A-Z0-9_]* {
    heredocIdentifier.push(getText());
} -> type(UNQUOTED_TEXT);

// ----------------------------------------------------------------------------------------------
// HEREDOC mode - for parsing heredoc content
// ----------------------------------------------------------------------------------------------
mode HEREDOC;

H_NEWLINE : '\n' -> type(NEWLINE);

HTemplateLiteral : ~[\n]+
{
  if(!heredocIdentifier.isEmpty() && getText().equals(heredocIdentifier.peek())) {
      setType(UNQUOTED_TEXT);
      heredocIdentifier.pop();
      popMode();
  }
};

