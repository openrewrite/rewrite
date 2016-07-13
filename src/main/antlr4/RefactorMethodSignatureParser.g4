parser grammar RefactorMethodSignatureParser;

options { tokenVocab=AspectJLexer; }
	
import JavaParser;
	
methodPattern
	:	targetTypePattern simpleNamePattern formalParametersPattern
	;

formalParametersPattern
	:	'(' formalsPattern? ')'
	;

formalsPattern
	:	dotDot (',' formalsPatternAfterDotDot)* 
	|	optionalParensTypePattern (',' formalsPattern)* 
	|	formalTypePattern ELLIPSIS
	;
	
dotDot
    :   DOTDOT
    ;
	                 
formalsPatternAfterDotDot
	:	optionalParensTypePattern (',' formalsPatternAfterDotDot)* 
	|	formalTypePattern ELLIPSIS
	;

optionalParensTypePattern
    :	'(' formalTypePattern ')'
    |	formalTypePattern
    ;

targetTypePattern
    :   classNameOrInterface
	|	'!' targetTypePattern 
	|	targetTypePattern '&&' targetTypePattern 
  	|	targetTypePattern '||' targetTypePattern
  	;
  	
formalTypePattern
	:	classNameOrInterface
	|	primitiveType
    |	'!' formalTypePattern 
    |	formalTypePattern '&&' formalTypePattern 
    |	formalTypePattern '||' formalTypePattern
	;

classNameOrInterface
    :	(Identifier | '*' | DOT | DOTDOT)+
    ;
	
simpleNamePattern
    :	Identifier ('*' Identifier)* '*'?
    |	'*' (Identifier '*')* Identifier?
    ;
