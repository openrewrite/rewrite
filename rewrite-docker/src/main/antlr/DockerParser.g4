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
    : HEALTHCHECK ( UNQUOTED_TEXT | flags? cmdInstruction )
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
    | FROM  // Allow 'from' as flag name (e.g., --from=builder in COPY)
    | AS    // Allow 'as' as flag name
    ;

flagValue
    : flagValueElement+
    ;

flagValueElement
    : UNQUOTED_TEXT | EQUALS | DOUBLE_QUOTED_STRING | SINGLE_QUOTED_STRING | ENV_VAR
    | COMMAND_SUBST | BACKTICK_SUBST
    ;

execForm
    : jsonArray
    ;

shellForm
    : shellFormText
    ;

// Text that can contain keywords (used in shell form where --shell, --user etc. are valid)
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
    | EQUALS
    | DASH_DASH
    | LBRACKET   // Allow [ in shell commands (e.g., if [ -f file ])
    | RBRACKET   // Allow ] in shell commands
    | COMMA      // Allow , in shell commands
    | shellSafeKeyword  // Allow certain keywords in shell commands (e.g., RUN useradd --shell /bin/false)
    ;

// Keywords that can safely appear in shell form text
// Only non-instruction-starting keywords: SHELL (--shell flag), USER (--user flag), AS (--as flag)
shellSafeKeyword
    : SHELL | USER | AS
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
    : labelKeyWithKeyword EQUALS labelValue    // New format: key=value (allows keyword keys like RUN)
    | labelKey labelOldValue                   // Old format: key value (no instruction keyword keys)
    ;

// Label key that allows all keywords (safe because EQUALS follows)
labelKeyWithKeyword
    : UNQUOTED_TEXT | DOUBLE_QUOTED_STRING | SINGLE_QUOTED_STRING
    | MAINTAINER | FROM | RUN | CMD | LABEL | EXPOSE | ENV | ADD | COPY | ENTRYPOINT
    | VOLUME | USER | WORKDIR | ARG | ONBUILD | STOPSIGNAL | HEALTHCHECK | SHELL | AS
    ;

// Label key for old format (only allows MAINTAINER, not instruction-starting keywords)
labelKey
    : UNQUOTED_TEXT | DOUBLE_QUOTED_STRING | SINGLE_QUOTED_STRING
    | MAINTAINER
    ;

labelValue
    : UNQUOTED_TEXT | DOUBLE_QUOTED_STRING | SINGLE_QUOTED_STRING
    ;

// Value in old-style LABEL (can contain instruction keywords like "run")
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
    | EQUALS
    | DASH_DASH
    | LBRACKET
    | RBRACKET
    | COMMA
    // Old-style LABEL values can contain instruction keywords
    | FROM | RUN | CMD | LABEL | EXPOSE | ENV | ADD | COPY | ENTRYPOINT
    | VOLUME | USER | WORKDIR | ARG | ONBUILD | STOPSIGNAL | HEALTHCHECK | SHELL | MAINTAINER
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
    ;

envPairs
    : envPair+
    ;

envPair
    : envKey EQUALS envValueEquals  // New form: KEY=value (no = in value)
    | envKey envValueSpace           // Old form: KEY value (rest of line, can have =)
    ;

envKey
    : UNQUOTED_TEXT
    | envSafeKeyword  // Allow certain keywords as env keys (e.g., ENV SHELL /bin/bash)
    ;

// Keywords that are safe to use as ENV variable names (not instruction-starting keywords)
envSafeKeyword
    : SHELL | USER | AS
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

textElement
    : UNQUOTED_TEXT
    | DOUBLE_QUOTED_STRING
    | SINGLE_QUOTED_STRING
    | ENV_VAR
    | COMMAND_SUBST   // Allow $(command) in text
    | BACKTICK_SUBST  // Allow `command` in text
    | EQUALS     // Allow = in shell form text (e.g., ENV_VAR=value in RUN commands)
    | DASH_DASH  // Allow -- in shell form text (e.g., --option in shell commands)
    | LBRACKET   // Allow [ in text (e.g., shell test expressions)
    | RBRACKET   // Allow ] in text
    | COMMA      // Allow , in text
    ;

