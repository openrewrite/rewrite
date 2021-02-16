parser grammar RefactorMethodSignatureParser;

options { tokenVocab=AspectJLexer; }
	
import JavaParser;

methodPattern
	:	targetTypePattern (SPACE* | DOT | POUND) simpleNamePattern formalParametersPattern
	;

formalParametersPattern
	:	'(' formalsPattern? ')'
	;

formalsPattern
	:	dotDot (',' SPACE* formalsPatternAfterDotDot)* 
	|	optionalParensTypePattern (',' SPACE* formalsPattern)* 
	|	formalTypePattern '...'
	;

dotDot
    :   '..'
    ;
	                 
formalsPatternAfterDotDot
	:	optionalParensTypePattern (',' SPACE* formalsPatternAfterDotDot)* 
	|	formalTypePattern '...'
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
    :	(Identifier | '*' | '.' | '..')+ ('[' ']')*
    ;
	
simpleNamePattern
    :	Identifier ('*' Identifier)* '*'?
    |	'*' (Identifier '*')* Identifier?
    ;
