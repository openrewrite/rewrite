parser grammar AnnotationSignatureParser;

options { tokenVocab=AspectJLexer; }

import JavaParser;

annotation
    :	'@' annotationName ( '(' ( elementValuePairs | elementValue )? ')' )?
    ;

