parser grammar RefactorMethodSignatureParser;

options { tokenVocab=AspectJLexer; }
	
import JavaParser;
	
methodPattern
	:	typePattern simpleNamePattern formalParametersPattern
	;

formalParametersPattern
	:	'(' formalsPattern? ')'
	;

formalsPattern
	:	'..' (',' formalsPatternAfterDotDot)* 
	|	optionalParensTypePattern (',' formalsPattern)* 
	|	typePattern '...'
	;
	              
formalsPatternAfterDotDot
	:	optionalParensTypePattern (',' formalsPatternAfterDotDot)* 
	|	typePattern '...'
	;

typePattern
    :   dottedNamePattern
	|	'!' typePattern 
	|	typePattern '&&' typePattern 
  	|	typePattern '||' typePattern
  	;
  	
dottedNamePattern
	:	(Identifier | '*' | '.' | '..')+
	|	'void'
	;
	
simpleNamePattern
    :	Identifier ('*' Identifier)* '*'?
    |	'*' (Identifier '*')* Identifier?
    ;
    
optionalParensTypePattern
    :	'(' typePattern ')'
    |	typePattern
    ;