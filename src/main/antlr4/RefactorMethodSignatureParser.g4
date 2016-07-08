parser grammar RefactorMethodSignatureParser;

options { tokenVocab=AspectJLexer; }
	
import JavaParser;
	
methodPattern
	:	typePattern simpleNamePattern formalParametersPattern
	;
	
methodModifiersPattern
	:	'!'? methodModifier methodModifiersPattern*
	;
		
methodModifier
	:	(	'public'
		|	'private'
		|	'protected'
		|	'static'
		|	'synchronized'
		|	'final'
		)
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
	  		
typePatternList
	:	typePattern (',' typePattern)*
	;

typePattern
	:	simpleTypePattern
	|	'!' typePattern 
//	|	'(' annotationPattern? typePattern ')'
	|	typePattern '&&' typePattern 
  	|	typePattern '||' typePattern
  	;
  	  	
simpleTypePattern
	:	dottedNamePattern '+'? ('[' ']')*
  	;
  	
dottedNamePattern
	:	(type | Identifier | '*' | '.' | '..')+
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