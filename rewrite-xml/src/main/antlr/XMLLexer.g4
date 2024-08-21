/*
 [The "BSD licence"]
 Copyright (c) 2013 Terence Parr
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:
 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
    derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

/** XML lexer derived from ANTLR v4 ref guide book example */
lexer grammar XMLLexer;

// Default "mode": Everything OUTSIDE of a tag
COMMENT           :  '<!--' .*? '-->' ;
CDATA             :  '<![CDATA[' .*? ']]>' ;

ParamEntityRef    :  '%' Name ';' ;
EntityRef         :  '&' Name ';' ;
CharRef           :  '&#' DIGIT+ ';'
                  |  '&#x' HEXDIGIT+ ';'
                  ;

SEA_WS            :  (' '|'\t'|'\r'? '\n')+        -> skip ;
UTF_ENCODING_BOM  :  (UTF_8_BOM_CHARS | UTF_8_BOM) -> skip ;

QUESTION_MARK     : '?' ;
SPECIAL_OPEN_XML  :  '<' QUESTION_MARK 'xml'       -> pushMode(INSIDE) ;
OPEN              :  '<'                           -> pushMode(INSIDE) ;

SPECIAL_OPEN      :  '<' QUESTION_MARK Name        -> pushMode(INSIDE_PROCESS_INSTRUCTION) ;

DTD_OPEN          :  '<!'                          -> pushMode(INSIDE_DTD) ;

TEXT              :  ~[<&]+ ;  // match any 16 bit char other than < and &

fragment
UTF_8_BOM_CHARS   : '\u00EF''\u00BB''\u00BF' ; // chars for UTF-8 read from a byte array.

fragment
UTF_8_BOM         : '\uFEFF' ; // UTF-8 char if the source is already a String.

// INSIDE of DTD ------------------------------------------
mode INSIDE_DTD;

DTD_CLOSE        :  '>'    -> popMode ;
DTD_SUBSET_OPEN  :  '['    -> pushMode(INSIDE_DTD_SUBSET) ;
DTD_S            :   S     -> skip ;

DOCTYPE          :  'DOCTYPE' ;

DTD_NAME         :  Name   -> type(Name) ;
DTD_STRING       :  STRING -> type(STRING) ;

// INSIDE of DTD SUBSET -----------------------------------
mode INSIDE_DTD_SUBSET;

DTD_SUBSET_CLOSE    :  ']'            -> popMode ;
MARKUP_OPEN         :  '<!'           -> pushMode(INSIDE_MARKUP) ;
DTS_SUBSET_S        :  S              -> skip ;

DTD_PERef           :  ParamEntityRef -> type(ParamEntityRef) ;
DTD_SUBSET_COMMENT  :  COMMENT        -> type(COMMENT) ;

// INSIDE of MARKUP ---------------------------------------
mode INSIDE_MARKUP;

MARK_UP_CLOSE       :  '>' -> popMode ;
MARKUP_SUBSET_OPEN  :  '[' -> more, pushMode(INSIDE_MARKUP_SUBSET) ;
MARKUP_S            :  S -> skip ;

MARKUP_TEXT         :  ~[>[]+ ;  // match any 16 bit char other than > and [

// INSIDE of MARKUP SUBSET --------------------------------
mode INSIDE_MARKUP_SUBSET;

MARKUP_SUBSET  :  ']' -> popMode ;
TXT            :  .   -> more ;  // Collect all of the markup subset text.

// INSIDE of a Processing instruction ---------------------
mode INSIDE_PROCESS_INSTRUCTION;

PI_SPECIAL_CLOSE  : SPECIAL_CLOSE -> type(SPECIAL_CLOSE), popMode ; // close <?Name...?>
PI_S              : S -> skip ;

// This rule separates the `?` in PI_TEXT from a special close `?>`, and is used to simplify parsing.
PI_QUESTION_MARK  : QUESTION_MARK -> more, pushMode(INSIDE_PI_TEXT);

PI_TEXT           : ~[?]+ ; // match any 16 bit char other than ?

// INSIDE of a processing instruction's text --------------
mode INSIDE_PI_TEXT;
PI_TEXT_SPECIAL_CASE : . -> more, popMode;

// INSIDE of a tag ----------------------------------------
mode INSIDE;

CLOSE          :  '>'                     -> popMode ;
SPECIAL_CLOSE  :  QUESTION_MARK '>'       -> popMode ; // close <?xml...?>
SLASH_CLOSE    :  '/>'                    -> popMode ;
S              :  [ \t\r\n]               -> skip ;

// JSP extension to the XML spec
DIRECTIVE_OPEN : '%@' ;
DIRECTIVE_CLOSE: '%';

SLASH          :  '/' ;
EQUALS         :  '=' ;
STRING         :  '"' ~[<"]* '"'
               |  '\'' ~[<']* '\''
               ;

Name           :  (NameStartChar NameChar*) ;

fragment
HEXDIGIT       :   [a-fA-F0-9] ;

fragment
DIGIT          :   [0-9] ;

fragment
NameChar
    :   NameStartChar
    |   '-' | '_' | '.'
    | DIGIT
    |   '\u00B7'
    |   '\u0300'..'\u036F'
    |   '\u203F'..'\u2040'
    ;

fragment
NameStartChar
    : [_:]
    | [a-zA-Z]
    | '\u00C0'..'\u00D6'
    | '\u00D8'..'\u00F6'
    | '\u00F8'..'\u02FF'
    | '\u0370'..'\u037D'
    | '\u037F'..'\u1FFF'
    | '\u200C'..'\u200D'
    | '\u2070'..'\u218F'
    | '\u3001'..'\uD7FF'
    | '\uF900'..'\uFDCF'
    | '\uFDF0'..'\uFFFD'
    | '\u{10000}'..'\u{EFFFF}'
    ;
