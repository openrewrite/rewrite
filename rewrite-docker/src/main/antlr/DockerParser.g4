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

    /**
     * Whether the next token stands against the previous one on the logical line, i.e. with nothing but
     * a line continuation between them. Docker joins the lines a continuation holds together, and drops
     * a comment line while doing so, before it reads what they say, so `ENV K\<newline>=v` binds the
     * same key to the same value that `ENV K=v` does.
     */
    private boolean bound() {
        Token previous = _input.LT(-1);
        Token next = _input.LT(1);
        if (previous == null || next == null) {
            return false;
        }
        for (int i = previous.getTokenIndex() + 1; i < next.getTokenIndex(); i++) {
            int type = _input.get(i).getType();
            if (type != LINE_CONTINUATION && type != COMMENT) {
                return false;
            }
        }
        return true;
    }
}

dockerfile
    : parserDirective* globalArgs stage+ EOF
    ;

parserDirective
    : PARSER_DIRECTIVE
    ;

globalArgs
    : argInstruction*
    ;

stage
    : fromInstruction stageInstruction*
    ;

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
    : HEALTHCHECK NONE
    | HEALTHCHECK healthcheckOptions? CMD ( execForm | shellForm )
    ;

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

flags
    : ( flag | fromFlag )+
    ;

flag
    : FLAG
    ;

// FLAG_END is the whitespace that ends the reference, without which this rule would carry on into the
// flags and paths that follow it.
fromFlag
    : FROM_FLAG imageReference? FLAG_END?
    ;

execForm
    : jsonArray
    ;

shellForm
    : shellFormText
    ;

shellFormText
    : textElement+
    ;

heredoc
    : heredocPreamble NEWLINE heredocBody+
    ;

// Docker reads a heredoc from any word of the line, so a marker need not open it: `cat >>/f <<EOF`
// redirects before it says what it is reading.
heredocPreamble
    : textElement* ( HEREDOC_START textElement* )+
    ;

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

// name:tag@digest. The IMAGE_REF lexer mode emits ':' and '@' as tokens only where they separate the
// parts, so a colon inside a quoted string, a variable reference or a port belongs to the part holding it.
imageReference
    : imageName ( COLON tag? )? ( AT digest? )?
    ;

// Nothing but a separator, `AS` or the next instruction can follow a part of a reference, so the quoted
// alternative is only viable when it is the whole part.
imageName
    : quoted
    | textElement+
    ;

tag
    : quoted
    | ( textElement | COLON )+
    ;

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

// The `=` of a pair is written hard against its key, which is what tells the two forms apart: Docker
// reads `LABEL k =v` as the legacy form with a value of `=v`, not as `k` bound to `v`.
labelPair
    : labelKey ( {bound()}? EQUALS value | text )
    ;

labelKey
    : quoted
    | UNQUOTED_TEXT
    ;

portList
    : port+
    ;

port
    : UNQUOTED_TEXT
    | ENV_VAR
    | COMMAND_SUBST
    | BACKTICK_SUBST
    | SPECIAL_VAR
    ;

envPairs
    : envPair+
    ;

// As `labelPair`, except that the legacy form takes the rest of the line.
envPair
    : envKey ( {bound()}? EQUALS value | text )
    ;

envKey
    : UNQUOTED_TEXT
    ;

// The last path is the destination and the ones before it the sources, a split the grammar cannot make
// itself: were the destination its own rule, the sources would have to give up their last element to it.
copyPaths
    : pathArgument pathArgument+
    ;

// A path ends at the next whitespace, so it is a run of adjacent elements, unlike `text` and `value`,
// which span whitespace and so take every element left.
pathArgument
    : quoted
    | textElement ( {adjacent()}? textElement )*
    ;

path
    : text
    ;

// user:group. The USER_SPEC lexer mode emits ':' as a token only where it separates the two.
userSpec
    : user ( COLON group? )?
    | COLON group?
    ;

// As `imageName`, the quoted alternative is only viable when it is the whole name.
user
    : quoted
    | textElement+
    ;

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

// Only a value written as a single quoted string carries a quote style; anywhere else the quotes are
// part of the text. Stated as its own alternative rather than leaving the visitor to count tokens.
quoted
    : DOUBLE_QUOTED_STRING
    | SINGLE_QUOTED_STRING
    ;

// The multi-element alternative comes first, as ANTLR resolves an ambiguity in favour of the first:
// `quoted` would otherwise read `LABEL author "John Doe" of ACME` as a value of `John Doe`.
text
    : textElement textElement+
    | quoted
    | textElement
    ;

// The `=` that binds a pair is not what ends one, so a value written against it may open with an `=` of
// its own: Docker reads `ENV KEY==value` as `KEY` bound to `=value`.
value
    : ( {adjacent()}? EQUALS )* ( valueElement valueElement+ | quoted | valueElement )
    ;

// Everything a `textElement` admits but the `=` itself: a value is written hard against its key, so
// `ENV K=--flag` and `ENV K=[a,b]` are values like any other, and only the separator can end one.
valueElement
    : UNQUOTED_TEXT
    | DOUBLE_QUOTED_STRING
    | SINGLE_QUOTED_STRING
    | ENV_VAR
    | COMMAND_SUBST
    | BACKTICK_SUBST
    | SPECIAL_VAR
    | DOLLAR
    | FLAG
    | DASH_DASH
    | LBRACKET
    | RBRACKET
    | COMMA
    ;

// Outside a value list nothing separates a pair, so `=` is an ordinary character here.
textElement
    : valueElement
    | EQUALS
    ;

