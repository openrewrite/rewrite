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
    : FROM flags? imageName ( AS stageName )?
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
    : HEALTHCHECK flags? healthcheckCommand
    ;

// HEALTHCHECK body: either NONE or CMD followed by command
// CMD can be a token (when no flags) or UNQUOTED_TEXT (when 'cmd' appears after flags)
healthcheckCommand
    : ( CMD | UNQUOTED_TEXT ) ( execForm | shellForm )?
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

flag
    : DASH_DASH flagName ( EQUALS flagValue )?
    ;

flagName
    : UNQUOTED_TEXT
    // Note: 'from' in --from=builder is UNQUOTED_TEXT (instruction keywords are only recognized at line start)
    | AS    // AS is always a keyword token (not instruction-specific)
    ;

// Flag value parsing: allows patterns like "value" or "type=cache,target=/path"
// Multi-token values are allowed (e.g., "moby-build-$TARGETPLATFORM")
// The visitor handles whitespace detection to stop at argument boundaries
// Note: AS is NOT included to allow FROM --flag=value image AS alias parsing
flagValue
    : flagValueToken+
    ;

// Token types allowed in flag values
flagValueToken
    : UNQUOTED_TEXT
    | DOUBLE_QUOTED_STRING
    | SINGLE_QUOTED_STRING
    | ENV_VAR
    | COMMAND_SUBST
    | BACKTICK_SUBST
    | SPECIAL_VAR
    | EQUALS
    ;

execForm
    : jsonArray
    ;

shellForm
    : shellFormText
    ;

// Text in shell form commands
// Note: Instruction keywords (RUN, ADD, COPY, etc.) are UNQUOTED_TEXT here because
// they are only recognized as instruction tokens at line start.
// AS is still a keyword token and needs explicit handling.
shellFormText
    : shellFormTextElement+
    ;

shellFormTextElement
    : UNQUOTED_TEXT
    | DOUBLE_QUOTED_STRING
    | SINGLE_QUOTED_STRING
    | ENV_VAR
    | COMMAND_SUBST      // Allow $(command) in shell commands
    | BACKTICK_SUBST     // Allow `command` in shell commands
    | SPECIAL_VAR        // Allow $!, $$, $?, etc. in shell commands
    | EQUALS
    | DASH_DASH
    | LBRACKET   // Allow [ in shell commands (e.g., if [ -f file ])
    | RBRACKET   // Allow ] in shell commands
    | COMMA      // Allow , in shell commands
    | AS         // AS is always a keyword token
    ;

heredoc
    : HEREDOC_START path? NEWLINE heredocContent heredocEnd
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

imageName
    : text
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
// AS is still a keyword token
labelKey
    : UNQUOTED_TEXT | DOUBLE_QUOTED_STRING | SINGLE_QUOTED_STRING
    | AS
    ;

labelValue
    : UNQUOTED_TEXT | DOUBLE_QUOTED_STRING | SINGLE_QUOTED_STRING
    ;

// Value in old-style LABEL (rest of line after key)
// Instruction keywords are UNQUOTED_TEXT here (not at line start)
labelOldValue
    : labelOldValueElement+
    ;

labelOldValueElement
    : UNQUOTED_TEXT
    | DOUBLE_QUOTED_STRING
    | SINGLE_QUOTED_STRING
    | ENV_VAR
    | COMMAND_SUBST
    | BACKTICK_SUBST
    | SPECIAL_VAR
    | EQUALS
    | DASH_DASH
    | LBRACKET
    | RBRACKET
    | COMMA
    | AS
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
    | AS
    ;

envPairs
    : envPair+
    ;

envPair
    : envKey EQUALS envValueEquals  // New form: KEY=value (no = in value)
    | envKey envValueSpace           // Old form: KEY value (rest of line, can have =)
    ;

// Env key - instruction keywords become UNQUOTED_TEXT (not at line start)
// AS is still a keyword token
envKey
    : UNQUOTED_TEXT
    | AS
    ;

envValueEquals
    : envTextEquals
    ;

envValueSpace
    : text
    ;

envTextEquals
    : envTextElementEquals+
    ;

envTextElementEquals
    : UNQUOTED_TEXT
    | DOUBLE_QUOTED_STRING
    | SINGLE_QUOTED_STRING
    | ENV_VAR
    | COMMAND_SUBST
    | BACKTICK_SUBST
    | SPECIAL_VAR
    | AS
    // NOTE: EQUALS is explicitly NOT included to allow multiple KEY=value pairs
    ;

sourceList
    : source+
    ;

source
    : path
    ;

destination
    : path
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
    | AS
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

// Generic text element - used for paths, image names, arg values, etc.
// Instruction keywords are UNQUOTED_TEXT (not at line start)
// Note: AS is NOT included here to avoid greediness issues in FROM imageName AS alias
textElement
    : UNQUOTED_TEXT
    | DOUBLE_QUOTED_STRING
    | SINGLE_QUOTED_STRING
    | ENV_VAR
    | COMMAND_SUBST   // Allow $(command) in text
    | BACKTICK_SUBST  // Allow `command` in text
    | SPECIAL_VAR     // Allow $!, $$, $?, etc. in text
    | EQUALS     // Allow = in shell form text (e.g., ENV_VAR=value in RUN commands)
    | DASH_DASH  // Allow -- in shell form text (e.g., --option in shell commands)
    | LBRACKET   // Allow [ in text (e.g., shell test expressions)
    | RBRACKET   // Allow ] in text
    | COMMA      // Allow , in text
    ;

