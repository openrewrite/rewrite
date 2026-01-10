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
    ;

execForm
    : jsonArray
    ;

shellForm
    : text
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
    : LBRACKET jsonArrayElements? JSON_RBRACKET
    ;

jsonArrayElements
    : jsonString ( JSON_COMMA jsonString )*
    ;

jsonString
    : JSON_STRING
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
    | labelKey labelOldValue        // Old format: key value (rest of line, can contain keywords)
    ;

labelKey
    : UNQUOTED_TEXT | DOUBLE_QUOTED_STRING | SINGLE_QUOTED_STRING
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
    | EQUALS
    | DASH_DASH
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
    | EQUALS  // Allow = in shell form text (e.g., ENV_VAR=value in RUN commands)
    | DASH_DASH  // Allow -- in shell form text (e.g., --option in shell commands)
    ;

