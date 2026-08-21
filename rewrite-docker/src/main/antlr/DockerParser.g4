// $antlr-format alignTrailingComments true, columnLimit 150, minEmptyLines 1, maxEmptyLinesToKeep 1, reflowComments false, useTab false
// $antlr-format allowShortRulesOnASingleLine false, allowShortBlocksOnASingleLine true, alignSemicolons hanging, alignColons hanging

parser grammar DockerParser;

options {
    tokenVocab = DockerLexer;
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
    : ADD flags? ( heredoc | jsonArray | sourceList destination )
    ;

copyInstruction
    : COPY flags? ( heredoc | jsonArray | sourceList destination )
    ;

entrypointInstruction
    : ENTRYPOINT ( execForm | shellForm )
    ;

volumeInstruction
    : VOLUME ( jsonArray | pathList )
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
    : flag+
    ;

// Flag token captures entire flag: --name or --name=value
// The lexer handles stopping at whitespace, so no greedy parsing issues
flag
    : FLAG
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

imageName
    : textElement+
    ;

// The first colon separates the tag, any later one is part of it
tag
    : ( textElement | COLON )+
    ;

// A digest carries its algorithm as a prefix, as in sha256:abc123
digest
    : ( textElement | COLON | AT )+
    ;

stageName
    : UNQUOTED_TEXT
    ;

labelPairs
    : labelPair+
    ;

labelPair
    : labelKey EQUALS labelValue    // New format: key=value
    | labelKey labelOldValue        // Old format: key value
    ;

// Label key - instruction keywords become UNQUOTED_TEXT since they're not at line start
labelKey
    : UNQUOTED_TEXT | DOUBLE_QUOTED_STRING | SINGLE_QUOTED_STRING
    ;

labelValue
    : valueElement+
    ;

// Value in old-style LABEL (rest of line after key)
// Instruction keywords are UNQUOTED_TEXT here (not at line start)
labelOldValue
    : textElement+
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
    : envKey EQUALS envValueEquals  // New form: KEY=value (no = in value)
    | envKey envValueSpace           // Old form: KEY value (rest of line, can have =)
    ;

// Env key - instruction keywords become UNQUOTED_TEXT (not at line start)
envKey
    : UNQUOTED_TEXT
    ;

envValueEquals
    : valueElement+
    ;

envValueSpace
    : textElement+
    ;

// For COPY/ADD: each sourcePath is a separate source
// The parser will group adjacent tokens without whitespace into single arguments
sourceList
    : sourcePath+
    ;

sourcePath
    : UNQUOTED_TEXT
    | DOUBLE_QUOTED_STRING
    | SINGLE_QUOTED_STRING
    | ENV_VAR
    | COMMAND_SUBST
    | BACKTICK_SUBST
    | SPECIAL_VAR
    ;

// Destination is the last path element
destination
    : destinationPath
    ;

destinationPath
    : UNQUOTED_TEXT
    | DOUBLE_QUOTED_STRING
    | SINGLE_QUOTED_STRING
    | ENV_VAR
    | COMMAND_SUBST
    | BACKTICK_SUBST
    | SPECIAL_VAR
    ;

path
    : text
    ;

pathList
    : volumePath+
    ;

volumePath
    : UNQUOTED_TEXT
    | DOUBLE_QUOTED_STRING
    | SINGLE_QUOTED_STRING
    | ENV_VAR  // Allow environment variables (e.g., VOLUME ${DATA_DIR})
    | COMMAND_SUBST   // Allow $(command)
    | BACKTICK_SUBST  // Allow `command`
    | SPECIAL_VAR     // Allow $!, $$, etc.
    ;

userSpec
    : text
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

text
    : textElement+
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

