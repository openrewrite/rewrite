// $antlr-format alignTrailingComments true, columnLimit 150, minEmptyLines 1, maxEmptyLinesToKeep 1, reflowComments false, useTab false
// $antlr-format allowShortRulesOnASingleLine false, allowShortBlocksOnASingleLine true, alignSemicolons hanging, alignColons hanging

parser grammar DockerParser;

options {
    tokenVocab = DockerLexer;
}

@members {
    /**
     * Whether the next token follows the previous one with nothing between them. Whitespace is on the
     * hidden channel, so this is how a rule states that its elements are one unbroken run of source.
     */
    private boolean adjacent() {
        Token previous = _input.LT(-1);
        Token next = _input.LT(1);
        return previous != null && next != null && previous.getStopIndex() + 1 == next.getStartIndex();
    }
}

// Root rule
dockerfile
    : parserDirective* globalArgs stage+ EOF
    ;

parserDirective
    : PARSER_DIRECTIVE
    ;

// Global ARG instructions before first FROM
globalArgs
    : argInstruction*
    ;

// A build stage starting with FROM
stage
    : fromInstruction stageInstruction*
    ;

// Instructions allowed within a stage (everything except FROM and global ARG)
stageInstruction
    : runInstruction
    | cmdInstruction
    | labelInstruction
    | exposeInstruction
    | envInstruction
    | addInstruction
    | copyInstruction
    | entrypointInstruction
    | volumeInstruction
    | userInstruction
    | workdirInstruction
    | argInstruction
    | onbuildInstruction
    | stopsignalInstruction
    | healthcheckInstruction
    | shellInstruction
    | maintainerInstruction
    ;

// Legacy: kept for backward compatibility if needed elsewhere
instruction
    : fromInstruction
    | stageInstruction
    ;

fromInstruction
    : FROM flags? imageReference ( AS stageName )?
    ;

runInstruction
    : RUN flags? ( execForm | shellForm | heredoc )
    ;

cmdInstruction
    : CMD ( execForm | shellForm )
    ;

labelInstruction
    : LABEL labelPairs
    ;

exposeInstruction
    : EXPOSE portList
    ;

envInstruction
    : ENV envPairs
    ;

addInstruction
    : ADD flags? ( heredoc | jsonArray | copyPaths )
    ;

copyInstruction
    : COPY flags? ( heredoc | jsonArray | copyPaths )
    ;

entrypointInstruction
    : ENTRYPOINT ( execForm | shellForm )
    ;

volumeInstruction
    : VOLUME ( jsonArray | pathArgument+ )
    ;

userInstruction
    : USER userSpec
    ;

workdirInstruction
    : WORKDIR path
    ;

argInstruction
    : ARG argName ( EQUALS argValue )?
    ;

onbuildInstruction
    : ONBUILD instruction
    ;

stopsignalInstruction
    : STOPSIGNAL signal
    ;

healthcheckInstruction
    : HEALTHCHECK NONE                                    // Disable health checks
    | HEALTHCHECK healthcheckOptions? CMD ( execForm | shellForm )  // Health check command
    ;

// HEALTHCHECK-specific options - uses FLAG token like regular flags
healthcheckOptions
    : healthcheckOption+
    ;

healthcheckOption
    : FLAG
    ;

shellInstruction
    : SHELL jsonArray
    ;

maintainerInstruction
    : MAINTAINER text
    ;

// Common elements
flags
    : ( flag | fromFlag )+
    ;

// Flag token captures entire flag: --name or --name=value
// The lexer handles stopping at whitespace, so no greedy parsing issues
flag
    : FLAG
    ;

// The --from of a COPY or ADD holds the same name:tag@digest reference a FROM does, split by the
// same rule. FLAG_END is the whitespace that ends the reference, without which this rule would
// carry on into the flags and paths that follow it.
fromFlag
    : FROM_FLAG imageReference? FLAG_END?
    ;

execForm
    : jsonArray
    ;

shellForm
    : shellFormText
    ;

// Text in shell form commands
// Note: Instruction keywords (RUN, ADD, COPY, AS, CMD, etc.) become UNQUOTED_TEXT here
// because they are only recognized as keyword tokens in specific contexts.
shellFormText
    : textElement+
    ;

// Unified heredoc structure supporting both single and multiple heredocs
// Single: RUN <<EOF ... EOF or COPY <<EOF /dest ... EOF
// Multi: RUN <<EOF1 cat >file1 && <<EOF2 cat >file2 ... EOF1 ... EOF2
heredoc
    : heredocPreamble NEWLINE heredocBody+
    ;

// Shell command preamble containing heredoc marker(s) and optional shell commands
// For single heredoc: just "<<EOF" or "<<EOF /dest" (for COPY/ADD)
// For multi heredoc: "<<EOF1 cat >file1 && <<EOF2 cat >file2"
// Elements are shell command text and, for COPY/ADD, the destination path.
heredocPreamble
    : HEREDOC_START textElement* ( HEREDOC_START textElement* )*
    ;

// A single heredoc body (content + closing marker)
heredocBody
    : heredocContent heredocEnd
    ;

heredocContent
    : ( NEWLINE | HEREDOC_CONTENT )*
    ;

heredocEnd
    : UNQUOTED_TEXT
    ;

jsonArray
    : LBRACKET jsonArrayElements? RBRACKET
    ;

jsonArrayElements
    : jsonString ( COMMA jsonString )*
    ;

jsonString
    : DOUBLE_QUOTED_STRING
    ;

// name:tag@digest, the reference used by FROM. The IMAGE_REF lexer mode emits ':' and '@' as
// tokens only where they separate the parts, so a colon inside a quoted string, a variable
// reference or a registry port belongs to the part that holds it.
imageReference
    : imageName ( COLON tag? )? ( AT digest? )?
    | COLON tag? ( AT digest? )?
    | AT digest?
    ;

// Nothing but a separator, `AS` or the next instruction can follow a part of a reference, so the
// quoted alternative is only viable when it is the whole part.
imageName
    : quoted
    | textElement+
    ;

// The first colon separates the tag, any later one is part of it
tag
    : quoted
    | ( textElement | COLON )+
    ;

// A digest carries its algorithm as a prefix, as in sha256:abc123
digest
    : quoted
    | ( textElement | COLON | AT )+
    ;

stageName
    : UNQUOTED_TEXT
    ;

labelPairs
    : labelPair+
    ;

labelPair
    : labelKey EQUALS value    // New format: key=value
    | labelKey text            // Old format: key value, the rest of the line
    ;

// Label key - instruction keywords become UNQUOTED_TEXT since they're not at line start
labelKey
    : quoted
    | UNQUOTED_TEXT
    ;

portList
    : port+
    ;

port
    : UNQUOTED_TEXT
    | ENV_VAR  // Allow environment variables (e.g., EXPOSE ${PORT})
    | COMMAND_SUBST   // Allow $(command)
    | BACKTICK_SUBST  // Allow `command`
    | SPECIAL_VAR     // Allow $!, $$, etc.
    ;

envPairs
    : envPair+
    ;

envPair
    : envKey EQUALS value  // New form: KEY=value (no = in value)
    | envKey text          // Old form: KEY value (rest of line, can have =)
    ;

// Env key - instruction keywords become UNQUOTED_TEXT (not at line start)
envKey
    : UNQUOTED_TEXT
    ;

// Every path of a COPY/ADD. The last is the destination and the ones before it are the sources, a
// split the grammar cannot make itself: were the destination a rule of its own, the sources would
// have to give up their last element to leave it something to match.
copyPaths
    : pathArgument pathArgument+
    ;

// A path ends at the next whitespace, so it is a run of elements that follow one another with nothing
// between them, unlike `text` and `value`, which span whitespace and so take every element left. Shared
// by the paths of a COPY/ADD and those of a VOLUME.
pathArgument
    : quoted
    | pathElement ( {adjacent()}? pathElement )*
    ;

path
    : text
    ;

// user:group, the specification used by USER. The USER_SPEC lexer mode emits ':' as a token only
// where it separates the two, so a colon inside a quoted string or a variable reference belongs to
// the name that holds it.
userSpec
    : user ( COLON group? )?
    | COLON group?
    ;

// As `imageName`: nothing but the separator or the next instruction can follow a name, so the quoted
// alternative is only viable when it is the whole name.
user
    : quoted
    | textElement+
    ;

// The first colon separates the group, any later one is part of it
group
    : quoted
    | ( textElement | COLON )+
    ;

argName
    : UNQUOTED_TEXT
    ;

argValue
    : text
    ;

signal
    : UNQUOTED_TEXT
    ;

// A value written as a single quoted string. Only such a value carries a quote style; anywhere else
// the quotes are part of the text, which is why the rules that hold a value state this case as an
// alternative of its own rather than leaving the visitor to count tokens.
quoted
    : DOUBLE_QUOTED_STRING
    | SINGLE_QUOTED_STRING
    ;

// The alternative of more than one element comes first, as ANTLR resolves an ambiguity in favour of
// the first alternative: `quoted` would otherwise match `LABEL author "John Doe" of ACME` as a value
// of `John Doe` and leave `of ACME` to a second pair.
text
    : textElement textElement+
    | quoted
    | textElement
    ;

// As `text`, for a value that ends at the next `=` so that `KEY=value` pairs can repeat.
value
    : valueElement valueElement+
    | quoted
    | valueElement
    ;

// An element of a value that ends at the next `=`, so that `KEY=value` pairs can repeat on one
// instruction. Shared by LABEL k=v and ENV K=V.
valueElement
    : UNQUOTED_TEXT
    | DOUBLE_QUOTED_STRING
    | SINGLE_QUOTED_STRING
    | ENV_VAR
    | COMMAND_SUBST   // Allow $(command) in values
    | BACKTICK_SUBST  // Allow `command` in values
    | SPECIAL_VAR     // Allow $!, $$, $?, etc. in values
    | DOLLAR          // Allow lone $ in values (e.g., $'hello' ANSI-C quoting)
    // NOTE: EQUALS is explicitly NOT included to allow multiple key=value pairs
    ;

// An element of a path. Whitespace separates paths, so `=` and `,` are ordinary characters here rather
// than the separators they are in a value list.
pathElement
    : valueElement
    | EQUALS
    | COMMA
    ;

// Generic text element - used for paths, image names, arg values, shell form and heredoc preambles.
// Instruction keywords and contextual keywords (AS, CMD, NONE) become UNQUOTED_TEXT
// when not in their specific contexts.
textElement
    : UNQUOTED_TEXT
    | DOUBLE_QUOTED_STRING
    | SINGLE_QUOTED_STRING
    | ENV_VAR
    | COMMAND_SUBST   // Allow $(command) in text
    | BACKTICK_SUBST  // Allow `command` in text
    | SPECIAL_VAR     // Allow $!, $$, $?, etc. in text
    | DOLLAR          // Allow lone $ in text (e.g., $'hello' ANSI-C quoting)
    | EQUALS     // Allow = in shell form text (e.g., ENV_VAR=value in RUN commands)
    | FLAG       // Allow --option or --option=value in text
    | DASH_DASH  // Allow -- in shell form text (e.g., --option in shell commands)
    | LBRACKET   // Allow [ in text (e.g., shell test expressions)
    | RBRACKET   // Allow ] in text
    | COMMA      // Allow , in text
    ;

