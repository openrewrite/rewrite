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

/** XML parser derived from ANTLR v4 ref guide book example */
parser grammar XMLParser;

options { tokenVocab=XMLLexer; }

document
    :   UTF_ENCODING_BOM? prolog element?
    ;

prolog
    :   xmldecl? misc* jspdirective*
    ;

xmldecl
    :   SPECIAL_OPEN_XML attribute* SPECIAL_CLOSE
    ;

misc
    :   (COMMENT | doctypedecl | processinginstruction | jspdeclaration | jspcomment)
    ;

doctypedecl
    :   DTD_OPEN DOCTYPE Name externalid STRING* (DTD_SUBSET_OPEN intsubset DTD_SUBSET_CLOSE)? DTD_CLOSE
    ;

intsubset
    :   (markupdecl | declSep)* ;

markupdecl
    :   (MARKUP_OPEN MARKUP_TEXT? MARKUP_SUBSET* MARKUP_TEXT? MARK_UP_CLOSE)
    |   processinginstruction
    |   COMMENT
    ;

declSep
    :   ParamEntityRef
    ;

externalid
    :   Name?
    ;

processinginstruction
    :   SPECIAL_OPEN PI_TEXT+ SPECIAL_CLOSE
    ;

content
    :   (element | reference | processinginstruction | CDATA | COMMENT |
         jspscriptlet | jspexpression | jspdeclaration | jspcomment | chardata) ;

element
    :   OPEN Name attribute* CLOSE content* OPEN '/' Name CLOSE
    |   OPEN Name attribute* '/>'
    ;

jspdirective
    :   OPEN DIRECTIVE_OPEN Name attribute* DIRECTIVE_CLOSE CLOSE
    ;

jspscriptlet
    :   JSP_SCRIPTLET
    ;

jspexpression
    :   JSP_EXPRESSION
    ;

jspdeclaration
    :   JSP_DECLARATION
    ;

jspcomment
    :   JSP_COMMENT
    ;

reference
    :   (EntityRef | CharRef) ;

attribute
    :   Name '=' STRING ; // Our STRING is AttValue in spec

chardata
    :   (TEXT | QUESTION_MARK | SEA_WS) ;
