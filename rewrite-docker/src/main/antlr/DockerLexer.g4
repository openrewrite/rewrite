// $antlr-format alignTrailingComments true, columnLimit 150, maxEmptyLinesToKeep 1, reflowComments false, useTab false
// $antlr-format allowShortRulesOnASingleLine true, allowShortBlocksOnASingleLine true, minEmptyLines 0, alignSemicolons ownLine
// $antlr-format alignColons trailing, singleLineOverrulesHangingColon true, alignLexerCommands true, alignLabels true, alignTrailers true

lexer grammar DockerLexer;

@lexer::header
{import java.util.LinkedList;
import java.util.Queue;
import org.openrewrite.docker.internal.Heredocs;}

@lexer::members
{
    private Queue<String> heredocIdentifiers = new LinkedList<String>();
    private boolean atLineStart = true;
    private boolean afterHealthcheck = false;
    private boolean copyFlags = false;
    private boolean atFileHead = true;
    private boolean atLineHead = true;
    // Named by an '# escape=' directive, and unlike the flags above not scoped to a line
    private char escapeChar = '\\';

    // Every line-scoped flag is answered here, so a rule added later cannot widen a region by forgetting
    // to close it. emit() runs after the rule's action and commands, so _type is the type the token ended
    // up with and _mode is the mode the next token will be read in.
    @Override
    public Token emit() {
        boolean lineEnded = endsLine();
        atLineStart = lineEnded || atLineStart && continuesLineStart();
        afterHealthcheck = !lineEnded && (_type == HEALTHCHECK || afterHealthcheck && _type != CMD && _type != NONE);
        copyFlags = !lineEnded && (_type == COPY || copyFlags && continuesCopyFlags());
        atFileHead = atFileHead && continuesFileHead();
        atLineHead = beginsLineHead() || atLineHead && _type == WS;
        escapeChar = _type == PARSER_DIRECTIVE ? declaredEscape() : escapeChar;
        return super.emit();
    }

    // In a heredoc body a newline only separates content lines; a parser directive matches its own.
    private boolean endsLine() {
        return _type == PARSER_DIRECTIVE || _type == NEWLINE && _mode != HEREDOC;
    }

    // ONBUILD and HEALTHCHECK each take a second instruction, and a HEALTHCHECK's flags stand between it
    // and the CMD or NONE it takes.
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

    // The flag section of a COPY reaches to the first of its paths, the value of a --from included.
    private boolean continuesCopyFlags() {
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

    // A written line, unlike the logical one atLineStart holds to, ends at a continuation as well.
    private boolean beginsLineHead() {
        return _type == NEWLINE && _mode != HEREDOC || _type == LINE_CONTINUATION || _type == PARSER_DIRECTIVE ||
               _type == FLAG_END && getText().endsWith("\n");
    }

    // Docker gives up on directives at the first comment, blank line or instruction.
    private boolean continuesFileHead() {
        return _type == PARSER_DIRECTIVE || _type == WS;
    }

    // Docker fails the build on anything but '\' or '`', a second escape directive included; we read on
    // with the last value that was one of the two.
    private char declaredEscape() {
        String directive = getText();
        int equals = directive.indexOf('=');
        if (!"escape".equalsIgnoreCase(directive.substring(1, equals).trim())) {
            return escapeChar;
        }
        String value = directive.substring(equals + 1).trim();
        return value.length() == 1 && (value.charAt(0) == '\\' || value.charAt(0) == '`') ? value.charAt(0) : escapeChar;
    }

    // The dash is queued along with the marker because it is what tells the terminator rule whether a
    // line indented with tabs closes this body.
    private void pushHeredocMarker() {
        heredocIdentifiers.add(getText().substring(2));
    }

    private boolean atLineContinuation() {
        for (int i = 1; ; i++) {
            int c = _input.LA(i);
            if (c != ' ' && c != '\t') {
                return c == '\n' || (c == '\r' && _input.LA(i + 1) == '\n');
            }
        }
    }

    private boolean atFromFlag() {
        if (!copyFlags) {
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

// Throughout, a predicate sits behind the character it qualifies rather than at the left edge: one
// reachable without consuming anything stops ANTLR caching the start state of the mode that holds it.
PARSER_DIRECTIVE : '#' {atFileHead}? WS_CHAR* [A-Z_]+ WS_CHAR* '=' WS_CHAR* ~[\r\n]* NEWLINE_CHAR;

COMMENT : '#' {atLineHead}? ~[\r\n]* -> channel(HIDDEN);

// Instructions are keywords only at line start, which is what keeps them apart from shell command text.
FROM       : 'FROM'       { if (!atLineStart) setType(UNQUOTED_TEXT); else pushMode(IMAGE_REF); };
RUN        : 'RUN'        { if (!atLineStart) setType(UNQUOTED_TEXT); };
CMD        : 'CMD'        { if (!atLineStart && !afterHealthcheck) setType(UNQUOTED_TEXT); };
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

HEREDOC_START : '<<' '-'? HEREDOC_NAME { pushHeredocMarker(); } -> pushMode(HEREDOC_PREAMBLE);

// Quoting any part of the delimiter says the body is not to be expanded, and says nothing about the
// name, which is why the quotes come off again where the terminator is compared.
fragment HEREDOC_NAME
    : ( [A-Z_][A-Z0-9_]* | '\'' ~['\r\n]* '\'' | '"' ~["\r\n]* '"' )+
    ;

LINE_CONTINUATION : LINE_CONT -> channel(HIDDEN);

fragment LINE_CONT : ESCAPE WS_CHAR* '\r'? '\n';

fragment ESCAPE     : '\\' {escapeChar == '\\'}? | '`' {escapeChar == '`'}?;
fragment NOT_ESCAPE : '\\' {escapeChar != '\\'}? | '`' {escapeChar != '`'}?;

LBRACKET : '[';
RBRACKET : ']';
COMMA    : ',';

EQUALS     : '=';

// Lexing is maximal munch, so without the predicate this longer token would always beat FROM_FLAG and
// the image reference of a --from would stay unsplit.
FLAG : '--' {!atFromFlag()}? FLAG_BODY;

// The --from of a COPY names an image, so its value is lexed as an image reference.
FROM_FLAG : '--' {atFromFlag()}? 'from=' -> pushMode(FLAG_IMAGE_REF);

fragment FLAG_BODY : [a-z] [a-z0-9_-]* ('=' FLAG_VALUE_PART+)?;
fragment FLAG_VALUE_PART
    : '"' ( '\\' ~[\r\n] | ~["\\\r\n] )* '"'
    | '\'' ~['\r\n]* '\''
    | ~[ \t\r\n"'\\]
    | '\\' ~[\r\n]
    ;

DASH_DASH  : '--';

fragment UNQUOTED_CHAR : ~[ \t\r\n\\"'$[\]=<`];

DOUBLE_QUOTED_STRING : DQ_STRING;

fragment DQ_STRING : '"' ( ESCAPE_SEQUENCE | INLINE_CONTINUATION | NOT_ESCAPE | ~["\\\r\n`] )* '"';
SINGLE_QUOTED_STRING : SQ_STRING;

fragment SQ_STRING : '\'' ( INLINE_CONTINUATION | ~['\r\n] )* '\'';

// Docker drops a comment line while joining the lines a continuation holds together, so one does not
// reach the string that spans it.
fragment INLINE_CONTINUATION : ESCAPE WS_CHAR* [\r\n]+ ( WS_CHAR* '#' ~[\r\n]* [\r\n]+ )*;

fragment ESCAPE_SEQUENCE : ESCAPE ~[\r\n];

// Keeps an escape character that LINE_CONT would match out of the text before it. What it escapes is
// optional because the end of the file can stand there.
fragment TEXT_ESCAPE
    : ESCAPE {!atLineContinuation()}? ~[\r\n]?
    | NOT_ESCAPE
    ;

ENV_VAR : VAR_REF;

fragment VAR_REF : '$' '{' [A-Z_][A-Z0-9_]* ( ':-' | ':+' | ':' )? ~[}]* '}' | '$' [A-Z_][A-Z0-9_]*;

SPECIAL_VAR : SPECIAL_VAR_REF;

fragment SPECIAL_VAR_REF : '$' [!$?#@*0-9];

COMMAND_SUBST : '$(' ( COMMAND_SUBST | ~[()] | '(' COMMAND_SUBST_INNER* ')' )* ')';
fragment COMMAND_SUBST_INNER : COMMAND_SUBST | ~[()];

// Only where the directive leaves the backtick to the shell; where it names one, TEXT_ESCAPE reads it.
BACKTICK_SUBST : '`' {escapeChar != '`'}? ~[ \t\r\n`] ~[`\r\n]* '`';

DOLLAR : '$';

UNQUOTED_TEXT
    : ( ~[-< \t\r\n\\"'$[\]=`] ( UNQUOTED_CHAR | TEXT_ESCAPE )*   // Start with non-hyphen, non-<, non-space
    | '-' ~[- \t\r\n\\"'$[\]=<`] ( UNQUOTED_CHAR | TEXT_ESCAPE )*  // Single hyphen followed by non-hyphen, non-space
    | '-'  // Just a hyphen by itself
    | '<' ~[< \t\r\n\\"'$[\]=`] ( UNQUOTED_CHAR | TEXT_ESCAPE )*  // Single < followed by non-<
    | '<'  // Just a < by itself
    | TEXT_ESCAPE ( UNQUOTED_CHAR | TEXT_ESCAPE )*  // Start with escaped char (e.g., \; in find -exec)
    )
    ;

// Docker reads no strings in the arguments of an instruction, so a quote the end of its line leaves
// open is an ordinary character. Not so of the reference a FROM or a --from names.
UNPAIRED_QUOTE : ["'] -> type(UNQUOTED_TEXT);

WS : WS_CHAR+ -> channel(HIDDEN);

fragment WS_CHAR : [ \t];

NEWLINE : NEWLINE_CHAR+ -> channel(HIDDEN);

fragment NEWLINE_CHAR : [\r\n];

// The image reference of a FROM, left at AS or at the end of the line. Only here are ':' and '@' their
// own tokens, so the parser can split the reference while a colon inside a quoted string, a variable
// reference or a registry port ('host:5000/img') stays part of the image name.
mode IMAGE_REF;

IR_WS                : WS_CHAR+      -> type(WS), channel(HIDDEN);
IR_LINE_CONTINUATION : LINE_CONT     -> type(LINE_CONTINUATION), channel(HIDDEN);
IR_COMMENT           : '#' {atLineHead}? ~[\r\n]* -> type(COMMENT), channel(HIDDEN);
IR_NEWLINE           : NEWLINE_CHAR+ -> type(NEWLINE), channel(HIDDEN), popMode;

COLON : ':';
AT    : '@';

AS : 'AS' -> popMode;

IR_FLAG                 : '--' FLAG_BODY  -> type(FLAG);
IR_DOUBLE_QUOTED_STRING : DQ_STRING       -> type(DOUBLE_QUOTED_STRING);
IR_SINGLE_QUOTED_STRING : SQ_STRING       -> type(SINGLE_QUOTED_STRING);
IR_ENV_VAR              : VAR_REF         -> type(ENV_VAR);
IR_SPECIAL_VAR          : SPECIAL_VAR_REF -> type(SPECIAL_VAR);
IR_DOLLAR               : '$'             -> type(DOLLAR);

// A colon that a '/' follows belongs to a registry port rather than to a tag ('host:5000/img:tag').
IR_UNQUOTED_TEXT : IR_TEXT -> type(UNQUOTED_TEXT);

fragment IR_TEXT       : ( IR_TEXT_CHAR | TEXT_ESCAPE | IR_PORT_COLON )+;
fragment IR_TEXT_CHAR  : ~[:@ \t\r\n\\"'$`];
fragment IR_PORT_COLON : ':' ( IR_TEXT_CHAR | ':' )* '/';

// The image reference carried by the --from of a COPY. As IMAGE_REF, except that it ends at the
// whitespace before the paths that follow, and AS is no keyword because no stage alias appears here.
mode FLAG_IMAGE_REF;

// A token rather than hidden whitespace: popping a mode does not bound a parser rule, so without one
// the `imageName` of `COPY --from=build --link .` would carry on into the flag that follows it.
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

// The user:group of a USER. As IMAGE_REF, minus the '@' and the AS exit.
mode USER_SPEC;

US_WS                : WS_CHAR+     -> type(WS), channel(HIDDEN);
US_LINE_CONTINUATION : LINE_CONT    -> type(LINE_CONTINUATION), channel(HIDDEN);
US_COMMENT           : '#' {atLineHead}? ~[\r\n]* -> type(COMMENT), channel(HIDDEN);
US_NEWLINE           : NEWLINE_CHAR+ -> type(NEWLINE), channel(HIDDEN), popMode;

US_COLON : ':' -> type(COLON);

US_DOUBLE_QUOTED_STRING : DQ_STRING       -> type(DOUBLE_QUOTED_STRING);
US_SINGLE_QUOTED_STRING : SQ_STRING       -> type(SINGLE_QUOTED_STRING);
US_ENV_VAR              : VAR_REF         -> type(ENV_VAR);
US_SPECIAL_VAR          : SPECIAL_VAR_REF -> type(SPECIAL_VAR);
US_DOLLAR               : '$'             -> type(DOLLAR);

US_UNQUOTED_TEXT : ( US_TEXT_CHAR | TEXT_ESCAPE )+ -> type(UNQUOTED_TEXT);

US_UNPAIRED_QUOTE : ["'] -> type(UNQUOTED_TEXT);

fragment US_TEXT_CHAR : ~[: \t\r\n\\"'$`];

// The rest of the line a heredoc marker opens, which may declare further markers of its own.
mode HEREDOC_PREAMBLE;

HP_LINE_CONTINUATION : LINE_CONT -> channel(HIDDEN);

HP_NEWLINE : '\n' -> type(NEWLINE), mode(HEREDOC);

HP_WS : [ \t\r\u000C]+ -> channel(HIDDEN);

HP_HEREDOC_START : '<<' '-'? HEREDOC_NAME { pushHeredocMarker(); } -> type(HEREDOC_START);

// Only the file's escape character is excluded, so that HP_LINE_CONTINUATION matches it.
HP_UNQUOTED_TEXT : ( ( ~[<\\` \t\r\n] | NOT_ESCAPE )+
                   | '<' ~[< \t\r\n] ~[ \t\r\n]*  // single < followed by non-< char
                   | '<'  // standalone <
                   ) -> type(UNQUOTED_TEXT);

// The body of a heredoc, left only once every queued marker has been matched.
mode HEREDOC;

H_NEWLINE : '\r'? '\n' -> type(NEWLINE);

// A '\r' only belongs to the content when it does not terminate the line, so that the closing marker
// of a CRLF heredoc is "EOF" rather than "EOF\r".
HEREDOC_CONTENT : ( ~[\r\n] | '\r' ~[\n] )+
{
  if(!heredocIdentifiers.isEmpty() && Heredocs.closes(heredocIdentifiers.peek(), getText())) {
      setType(UNQUOTED_TEXT);
      heredocIdentifiers.poll();
          if(heredocIdentifiers.isEmpty()) {
          popMode();
      }
  }
};

