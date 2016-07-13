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
	|	formalTypePattern '...'
	;
	
dotDot
    :   '..'
    ;
	                 
formalsPatternAfterDotDot
	:	optionalParensTypePattern (',' formalsPatternAfterDotDot)* 
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
