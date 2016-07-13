// Generated from /Users/joschneider/Projects/github/jkschneider/java-source-refactor/src/main/antlr4/RefactorMethodSignatureParser.g4 by ANTLR 4.2.2
package com.netflix.java.refactor.aspectj;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class RefactorMethodSignatureParser extends Parser {
	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		ADVICEEXECUTION=3, THROW=102, AFTER=6, ISSINGLETON=19, SOFT=32, STATIC=96, 
		ANNOTATION_AFTERTHROWING=41, PREINITIALIZATION=28, INTERFACE=86, AND_ASSIGN=151, 
		ANNOTATION=4, BREAK=62, BYTE=63, ELSE=73, ERROR=14, ANNOTATION_CONSTRUCTOR=51, 
		IF=80, ANNOTATION_DECLAREMIXIN=46, CFLOW=11, ENUM=74, SUB=140, ANNOTATION_BEFORE=44, 
		BANG=127, LPAREN=115, DOT=123, PERTARGET=23, THROWING=35, CASE=64, AT=58, 
		LINE_COMMENT=162, StringLiteral=113, ELLIPSIS=159, LBRACK=119, GET=16, 
		ANNOTATION_VALUE=57, PUBLIC=93, THROWS=103, HANDLER=17, NullLiteral=114, 
		DQUOTE=2, PERCFLOW=21, RSHIFT_ASSIGN=156, LBRACE=117, PERTHIS=24, CALL=10, 
		GOTO=81, SET=31, SUB_ASSIGN=148, SEMI=121, CHAR=66, ASSIGN=124, ANNOTATION_INTERFACES=54, 
		RETURNING=30, COMMENT=161, ANNOTATION_FIELD=53, IMPORT=83, BITOR=144, 
		CATCH=65, MUL_ASSIGN=149, DOUBLE=72, PERCFLOWBELOW=22, PROTECTED=92, AROUND=7, 
		INITIALIZATION=18, LONG=87, COMMA=122, BITAND=143, PRECEDENCE=27, PARENTS=20, 
		COMMENT4=175, COMMENT3=171, ANNOTATION_AROUND=42, COMMENT2=168, PRIVATE=91, 
		COMMENT1=164, CONTINUE=69, DIV=142, FloatingPointLiteral=110, LE=132, 
		CharacterLiteral=112, TARGET=34, VOLATILE=107, EXTENDS=75, INSTANCEOF=84, 
		NEW=89, ADD=139, ANNOTATION_METHOD=56, LT=126, CLASS=67, DO=71, FINALLY=77, 
		Identifier=158, LINE_COMMENT3=172, LINE_COMMENT2=169, CONST=68, PACKAGE=90, 
		LINE_COMMENT1=165, OR_ASSIGN=152, TRY=105, PRIVILEGED=29, IntegerLiteral=109, 
		ANNOTATION_TYPE=55, SYNCHRONIZED=100, STATICINITIALIZATION=33, MUL=141, 
		FOR=79, FINAL=76, ANNOTATION_DECLAREPRECEDENCE=49, RPAREN=116, WARNING=36, 
		CARET=145, URSHIFT_ASSIGN=157, BOOLEAN=61, NOTEQUAL=134, RBRACK=120, RBRACE=118, 
		AND=135, THIS=101, SWITCH=99, VOID=106, TRANSIENT=104, INC=137, ANNOTATION_AFTERRETURNING=40, 
		WITHINCODE=38, FLOAT=78, NATIVE=88, ARGS=5, DIV_ASSIGN=150, BooleanLiteral=111, 
		WITHIN=37, ANNOTATION_DEFAULTIMPL=52, CFLOWBELOW=12, ABSTRACT=59, STRICTFP=97, 
		INT=85, QUESTION=129, RETURN=94, WS1=163, LSHIFT_ASSIGN=155, WS3=170, 
		ANNOTATION_ASPECT=43, WS2=167, WS4=174, ADD_ASSIGN=147, POINTCUT=26, WS=160, 
		GE=133, ANNOTATION_DECLAREWARNING=47, SUPER=98, ANNOTATION_DECLAREERROR=48, 
		OR=136, DEC=138, MOD=146, XOR_ASSIGN=153, PERTYPEWITHIN=25, ASSERT=60, 
		EQUAL=131, INVALID3=173, EXECUTION=15, INVALID4=177, DOTDOT=1, IMPLEMENTS=82, 
		COLON=130, LINE_COMMENT4=176, GT=125, INVALID1=166, SHORT=95, BEFORE=9, 
		DECLARE=13, MOD_ASSIGN=154, ANNOTATION_POINTCUT=50, ASPECT=8, WHILE=108, 
		TILDE=128, ANNOTATION_DECLAREPARENTS=45, ANNOTATION_AFTER=39, DEFAULT=70;
	public static final String[] tokenNames = {
		"<INVALID>", "'..'", "'\"'", "'adviceexecution'", "'annotation'", "'args'", 
		"'after'", "'around'", "'aspect'", "'before'", "'call'", "'cflow'", "'cflowbelow'", 
		"'declare'", "'error'", "'execution'", "'get'", "'handler'", "'initialization'", 
		"'issingleton'", "'parents'", "'percflow'", "'percflowbelow'", "'pertarget'", 
		"'perthis'", "'pertypewithin'", "'pointcut'", "'precedence'", "'preinitialization'", 
		"'privileged'", "'returning'", "'set'", "'soft'", "'staticinitialization'", 
		"'target'", "'throwing'", "'warning'", "'within'", "'withincode'", "'After'", 
		"'AfterReturning'", "'AfterThrowing'", "'Around'", "'Aspect'", "'Before'", 
		"'DeclareParents'", "'DeclareMixin'", "'DeclareWarning'", "'DeclareError'", 
		"'DeclarePrecedence'", "'Pointcut'", "'constructor'", "'defaultImpl'", 
		"'field'", "'interfaces'", "'type'", "'method'", "'value'", "'@'", "'abstract'", 
		"'assert'", "'boolean'", "'break'", "'byte'", "'case'", "'catch'", "'char'", 
		"'class'", "'const'", "'continue'", "'default'", "'do'", "'double'", "'else'", 
		"'enum'", "'extends'", "'final'", "'finally'", "'float'", "'for'", "'if'", 
		"'goto'", "'implements'", "'import'", "'instanceof'", "'int'", "'interface'", 
		"'long'", "'native'", "'new'", "'package'", "'private'", "'protected'", 
		"'public'", "'return'", "'short'", "'static'", "'strictfp'", "'super'", 
		"'switch'", "'synchronized'", "'this'", "'throw'", "'throws'", "'transient'", 
		"'try'", "'void'", "'volatile'", "'while'", "IntegerLiteral", "FloatingPointLiteral", 
		"BooleanLiteral", "CharacterLiteral", "StringLiteral", "'null'", "'('", 
		"')'", "'{'", "'}'", "'['", "']'", "';'", "','", "'.'", "'='", "'>'", 
		"'<'", "'!'", "'~'", "'?'", "':'", "'=='", "'<='", "'>='", "'!='", "'&&'", 
		"'||'", "'++'", "'--'", "'+'", "'-'", "'*'", "'/'", "'&'", "'|'", "'^'", 
		"'%'", "'+='", "'-='", "'*='", "'/='", "'&='", "'|='", "'^='", "'%='", 
		"'<<='", "'>>='", "'>>>='", "Identifier", "'...'", "WS", "COMMENT", "LINE_COMMENT", 
		"WS1", "COMMENT1", "LINE_COMMENT1", "INVALID1", "WS2", "COMMENT2", "LINE_COMMENT2", 
		"WS3", "COMMENT3", "LINE_COMMENT3", "INVALID3", "WS4", "COMMENT4", "LINE_COMMENT4", 
		"INVALID4"
	};
	public static final int
		RULE_methodPattern = 0, RULE_formalParametersPattern = 1, RULE_formalsPattern = 2, 
		RULE_dotDot = 3, RULE_formalsPatternAfterDotDot = 4, RULE_optionalParensTypePattern = 5, 
		RULE_targetTypePattern = 6, RULE_formalTypePattern = 7, RULE_classNameOrInterface = 8, 
		RULE_simpleNamePattern = 9, RULE_compilationUnit = 10, RULE_packageDeclaration = 11, 
		RULE_importDeclaration = 12, RULE_typeDeclaration = 13, RULE_modifier = 14, 
		RULE_classOrInterfaceModifier = 15, RULE_variableModifier = 16, RULE_classDeclaration = 17, 
		RULE_typeParameters = 18, RULE_typeParameter = 19, RULE_typeBound = 20, 
		RULE_enumDeclaration = 21, RULE_enumConstants = 22, RULE_enumConstant = 23, 
		RULE_enumBodyDeclarations = 24, RULE_interfaceDeclaration = 25, RULE_typeList = 26, 
		RULE_classBody = 27, RULE_interfaceBody = 28, RULE_classBodyDeclaration = 29, 
		RULE_memberDeclaration = 30, RULE_methodDeclaration = 31, RULE_genericMethodDeclaration = 32, 
		RULE_constructorDeclaration = 33, RULE_genericConstructorDeclaration = 34, 
		RULE_fieldDeclaration = 35, RULE_interfaceBodyDeclaration = 36, RULE_interfaceMemberDeclaration = 37, 
		RULE_constDeclaration = 38, RULE_constantDeclarator = 39, RULE_interfaceMethodDeclaration = 40, 
		RULE_genericInterfaceMethodDeclaration = 41, RULE_variableDeclarators = 42, 
		RULE_variableDeclarator = 43, RULE_variableDeclaratorId = 44, RULE_variableInitializer = 45, 
		RULE_arrayInitializer = 46, RULE_enumConstantName = 47, RULE_type = 48, 
		RULE_classOrInterfaceType = 49, RULE_primitiveType = 50, RULE_typeArguments = 51, 
		RULE_typeArgument = 52, RULE_qualifiedNameList = 53, RULE_formalParameters = 54, 
		RULE_formalParameterList = 55, RULE_formalParameter = 56, RULE_lastFormalParameter = 57, 
		RULE_methodBody = 58, RULE_constructorBody = 59, RULE_qualifiedName = 60, 
		RULE_literal = 61, RULE_annotation = 62, RULE_annotationName = 63, RULE_elementValuePairs = 64, 
		RULE_elementValuePair = 65, RULE_elementValue = 66, RULE_elementValueArrayInitializer = 67, 
		RULE_annotationTypeDeclaration = 68, RULE_annotationTypeBody = 69, RULE_annotationTypeElementDeclaration = 70, 
		RULE_annotationTypeElementRest = 71, RULE_annotationMethodOrConstantRest = 72, 
		RULE_annotationMethodRest = 73, RULE_annotationConstantRest = 74, RULE_defaultValue = 75, 
		RULE_block = 76, RULE_blockStatement = 77, RULE_localVariableDeclarationStatement = 78, 
		RULE_localVariableDeclaration = 79, RULE_statement = 80, RULE_catchClause = 81, 
		RULE_catchType = 82, RULE_finallyBlock = 83, RULE_resourceSpecification = 84, 
		RULE_resources = 85, RULE_resource = 86, RULE_switchBlockStatementGroup = 87, 
		RULE_switchLabel = 88, RULE_forControl = 89, RULE_forInit = 90, RULE_enhancedForControl = 91, 
		RULE_forUpdate = 92, RULE_parExpression = 93, RULE_expressionList = 94, 
		RULE_statementExpression = 95, RULE_constantExpression = 96, RULE_expression = 97, 
		RULE_primary = 98, RULE_creator = 99, RULE_createdName = 100, RULE_innerCreator = 101, 
		RULE_arrayCreatorRest = 102, RULE_classCreatorRest = 103, RULE_explicitGenericInvocation = 104, 
		RULE_nonWildcardTypeArguments = 105, RULE_typeArgumentsOrDiamond = 106, 
		RULE_nonWildcardTypeArgumentsOrDiamond = 107, RULE_superSuffix = 108, 
		RULE_explicitGenericInvocationSuffix = 109, RULE_arguments = 110;
	public static final String[] ruleNames = {
		"methodPattern", "formalParametersPattern", "formalsPattern", "dotDot", 
		"formalsPatternAfterDotDot", "optionalParensTypePattern", "targetTypePattern", 
		"formalTypePattern", "classNameOrInterface", "simpleNamePattern", "compilationUnit", 
		"packageDeclaration", "importDeclaration", "typeDeclaration", "modifier", 
		"classOrInterfaceModifier", "variableModifier", "classDeclaration", "typeParameters", 
		"typeParameter", "typeBound", "enumDeclaration", "enumConstants", "enumConstant", 
		"enumBodyDeclarations", "interfaceDeclaration", "typeList", "classBody", 
		"interfaceBody", "classBodyDeclaration", "memberDeclaration", "methodDeclaration", 
		"genericMethodDeclaration", "constructorDeclaration", "genericConstructorDeclaration", 
		"fieldDeclaration", "interfaceBodyDeclaration", "interfaceMemberDeclaration", 
		"constDeclaration", "constantDeclarator", "interfaceMethodDeclaration", 
		"genericInterfaceMethodDeclaration", "variableDeclarators", "variableDeclarator", 
		"variableDeclaratorId", "variableInitializer", "arrayInitializer", "enumConstantName", 
		"type", "classOrInterfaceType", "primitiveType", "typeArguments", "typeArgument", 
		"qualifiedNameList", "formalParameters", "formalParameterList", "formalParameter", 
		"lastFormalParameter", "methodBody", "constructorBody", "qualifiedName", 
		"literal", "annotation", "annotationName", "elementValuePairs", "elementValuePair", 
		"elementValue", "elementValueArrayInitializer", "annotationTypeDeclaration", 
		"annotationTypeBody", "annotationTypeElementDeclaration", "annotationTypeElementRest", 
		"annotationMethodOrConstantRest", "annotationMethodRest", "annotationConstantRest", 
		"defaultValue", "block", "blockStatement", "localVariableDeclarationStatement", 
		"localVariableDeclaration", "statement", "catchClause", "catchType", "finallyBlock", 
		"resourceSpecification", "resources", "resource", "switchBlockStatementGroup", 
		"switchLabel", "forControl", "forInit", "enhancedForControl", "forUpdate", 
		"parExpression", "expressionList", "statementExpression", "constantExpression", 
		"expression", "primary", "creator", "createdName", "innerCreator", "arrayCreatorRest", 
		"classCreatorRest", "explicitGenericInvocation", "nonWildcardTypeArguments", 
		"typeArgumentsOrDiamond", "nonWildcardTypeArgumentsOrDiamond", "superSuffix", 
		"explicitGenericInvocationSuffix", "arguments"
	};

	@Override
	public String getGrammarFileName() { return "RefactorMethodSignatureParser.g4"; }

	@Override
	public String[] getTokenNames() { return tokenNames; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public RefactorMethodSignatureParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class MethodPatternContext extends ParserRuleContext {
		public FormalParametersPatternContext formalParametersPattern() {
			return getRuleContext(FormalParametersPatternContext.class,0);
		}
		public TargetTypePatternContext targetTypePattern() {
			return getRuleContext(TargetTypePatternContext.class,0);
		}
		public SimpleNamePatternContext simpleNamePattern() {
			return getRuleContext(SimpleNamePatternContext.class,0);
		}
		public MethodPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_methodPattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterMethodPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitMethodPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitMethodPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MethodPatternContext methodPattern() throws RecognitionException {
		MethodPatternContext _localctx = new MethodPatternContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_methodPattern);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(222); targetTypePattern(0);
			setState(223); simpleNamePattern();
			setState(224); formalParametersPattern();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FormalParametersPatternContext extends ParserRuleContext {
		public FormalsPatternContext formalsPattern() {
			return getRuleContext(FormalsPatternContext.class,0);
		}
		public FormalParametersPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_formalParametersPattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterFormalParametersPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitFormalParametersPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitFormalParametersPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FormalParametersPatternContext formalParametersPattern() throws RecognitionException {
		FormalParametersPatternContext _localctx = new FormalParametersPatternContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_formalParametersPattern);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(226); match(LPAREN);
			setState(228);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << DOTDOT) | (1L << BOOLEAN) | (1L << BYTE))) != 0) || ((((_la - 66)) & ~0x3f) == 0 && ((1L << (_la - 66)) & ((1L << (CHAR - 66)) | (1L << (DOUBLE - 66)) | (1L << (FLOAT - 66)) | (1L << (INT - 66)) | (1L << (LONG - 66)) | (1L << (SHORT - 66)) | (1L << (LPAREN - 66)) | (1L << (DOT - 66)) | (1L << (BANG - 66)))) != 0) || _la==MUL || _la==Identifier) {
				{
				setState(227); formalsPattern();
				}
			}

			setState(230); match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FormalsPatternContext extends ParserRuleContext {
		public FormalsPatternContext formalsPattern(int i) {
			return getRuleContext(FormalsPatternContext.class,i);
		}
		public List<FormalsPatternAfterDotDotContext> formalsPatternAfterDotDot() {
			return getRuleContexts(FormalsPatternAfterDotDotContext.class);
		}
		public FormalsPatternAfterDotDotContext formalsPatternAfterDotDot(int i) {
			return getRuleContext(FormalsPatternAfterDotDotContext.class,i);
		}
		public TerminalNode ELLIPSIS() { return getToken(RefactorMethodSignatureParser.ELLIPSIS, 0); }
		public DotDotContext dotDot() {
			return getRuleContext(DotDotContext.class,0);
		}
		public FormalTypePatternContext formalTypePattern() {
			return getRuleContext(FormalTypePatternContext.class,0);
		}
		public List<FormalsPatternContext> formalsPattern() {
			return getRuleContexts(FormalsPatternContext.class);
		}
		public OptionalParensTypePatternContext optionalParensTypePattern() {
			return getRuleContext(OptionalParensTypePatternContext.class,0);
		}
		public FormalsPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_formalsPattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterFormalsPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitFormalsPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitFormalsPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FormalsPatternContext formalsPattern() throws RecognitionException {
		FormalsPatternContext _localctx = new FormalsPatternContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_formalsPattern);
		try {
			int _alt;
			setState(251);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(232); dotDot();
				setState(237);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,1,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(233); match(COMMA);
						setState(234); formalsPatternAfterDotDot();
						}
						} 
					}
					setState(239);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,1,_ctx);
				}
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(240); optionalParensTypePattern();
				setState(245);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(241); match(COMMA);
						setState(242); formalsPattern();
						}
						} 
					}
					setState(247);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
				}
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(248); formalTypePattern(0);
				setState(249); match(ELLIPSIS);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DotDotContext extends ParserRuleContext {
		public TerminalNode DOTDOT() { return getToken(RefactorMethodSignatureParser.DOTDOT, 0); }
		public DotDotContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dotDot; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterDotDot(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitDotDot(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitDotDot(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DotDotContext dotDot() throws RecognitionException {
		DotDotContext _localctx = new DotDotContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_dotDot);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(253); match(DOTDOT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FormalsPatternAfterDotDotContext extends ParserRuleContext {
		public List<FormalsPatternAfterDotDotContext> formalsPatternAfterDotDot() {
			return getRuleContexts(FormalsPatternAfterDotDotContext.class);
		}
		public FormalsPatternAfterDotDotContext formalsPatternAfterDotDot(int i) {
			return getRuleContext(FormalsPatternAfterDotDotContext.class,i);
		}
		public TerminalNode ELLIPSIS() { return getToken(RefactorMethodSignatureParser.ELLIPSIS, 0); }
		public FormalTypePatternContext formalTypePattern() {
			return getRuleContext(FormalTypePatternContext.class,0);
		}
		public OptionalParensTypePatternContext optionalParensTypePattern() {
			return getRuleContext(OptionalParensTypePatternContext.class,0);
		}
		public FormalsPatternAfterDotDotContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_formalsPatternAfterDotDot; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterFormalsPatternAfterDotDot(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitFormalsPatternAfterDotDot(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitFormalsPatternAfterDotDot(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FormalsPatternAfterDotDotContext formalsPatternAfterDotDot() throws RecognitionException {
		FormalsPatternAfterDotDotContext _localctx = new FormalsPatternAfterDotDotContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_formalsPatternAfterDotDot);
		try {
			int _alt;
			setState(266);
			switch ( getInterpreter().adaptivePredict(_input,5,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(255); optionalParensTypePattern();
				setState(260);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,4,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(256); match(COMMA);
						setState(257); formalsPatternAfterDotDot();
						}
						} 
					}
					setState(262);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,4,_ctx);
				}
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(263); formalTypePattern(0);
				setState(264); match(ELLIPSIS);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OptionalParensTypePatternContext extends ParserRuleContext {
		public FormalTypePatternContext formalTypePattern() {
			return getRuleContext(FormalTypePatternContext.class,0);
		}
		public OptionalParensTypePatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_optionalParensTypePattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterOptionalParensTypePattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitOptionalParensTypePattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitOptionalParensTypePattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OptionalParensTypePatternContext optionalParensTypePattern() throws RecognitionException {
		OptionalParensTypePatternContext _localctx = new OptionalParensTypePatternContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_optionalParensTypePattern);
		try {
			setState(273);
			switch (_input.LA(1)) {
			case LPAREN:
				enterOuterAlt(_localctx, 1);
				{
				setState(268); match(LPAREN);
				setState(269); formalTypePattern(0);
				setState(270); match(RPAREN);
				}
				break;
			case DOTDOT:
			case BOOLEAN:
			case BYTE:
			case CHAR:
			case DOUBLE:
			case FLOAT:
			case INT:
			case LONG:
			case SHORT:
			case DOT:
			case BANG:
			case MUL:
			case Identifier:
				enterOuterAlt(_localctx, 2);
				{
				setState(272); formalTypePattern(0);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TargetTypePatternContext extends ParserRuleContext {
		public TargetTypePatternContext targetTypePattern(int i) {
			return getRuleContext(TargetTypePatternContext.class,i);
		}
		public ClassNameOrInterfaceContext classNameOrInterface() {
			return getRuleContext(ClassNameOrInterfaceContext.class,0);
		}
		public List<TargetTypePatternContext> targetTypePattern() {
			return getRuleContexts(TargetTypePatternContext.class);
		}
		public TargetTypePatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_targetTypePattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterTargetTypePattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitTargetTypePattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitTargetTypePattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TargetTypePatternContext targetTypePattern() throws RecognitionException {
		return targetTypePattern(0);
	}

	private TargetTypePatternContext targetTypePattern(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		TargetTypePatternContext _localctx = new TargetTypePatternContext(_ctx, _parentState);
		TargetTypePatternContext _prevctx = _localctx;
		int _startState = 12;
		enterRecursionRule(_localctx, 12, RULE_targetTypePattern, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(279);
			switch (_input.LA(1)) {
			case BANG:
				{
				setState(276); match(BANG);
				setState(277); targetTypePattern(3);
				}
				break;
			case DOTDOT:
			case DOT:
			case MUL:
			case Identifier:
				{
				setState(278); classNameOrInterface();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(289);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,9,_ctx);
			while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(287);
					switch ( getInterpreter().adaptivePredict(_input,8,_ctx) ) {
					case 1:
						{
						_localctx = new TargetTypePatternContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_targetTypePattern);
						setState(281);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(282); match(AND);
						setState(283); targetTypePattern(3);
						}
						break;

					case 2:
						{
						_localctx = new TargetTypePatternContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_targetTypePattern);
						setState(284);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(285); match(OR);
						setState(286); targetTypePattern(2);
						}
						break;
					}
					} 
				}
				setState(291);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,9,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class FormalTypePatternContext extends ParserRuleContext {
		public PrimitiveTypeContext primitiveType() {
			return getRuleContext(PrimitiveTypeContext.class,0);
		}
		public FormalTypePatternContext formalTypePattern(int i) {
			return getRuleContext(FormalTypePatternContext.class,i);
		}
		public List<FormalTypePatternContext> formalTypePattern() {
			return getRuleContexts(FormalTypePatternContext.class);
		}
		public ClassNameOrInterfaceContext classNameOrInterface() {
			return getRuleContext(ClassNameOrInterfaceContext.class,0);
		}
		public FormalTypePatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_formalTypePattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterFormalTypePattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitFormalTypePattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitFormalTypePattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FormalTypePatternContext formalTypePattern() throws RecognitionException {
		return formalTypePattern(0);
	}

	private FormalTypePatternContext formalTypePattern(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		FormalTypePatternContext _localctx = new FormalTypePatternContext(_ctx, _parentState);
		FormalTypePatternContext _prevctx = _localctx;
		int _startState = 14;
		enterRecursionRule(_localctx, 14, RULE_formalTypePattern, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(297);
			switch (_input.LA(1)) {
			case BANG:
				{
				setState(293); match(BANG);
				setState(294); formalTypePattern(3);
				}
				break;
			case DOTDOT:
			case DOT:
			case MUL:
			case Identifier:
				{
				setState(295); classNameOrInterface();
				}
				break;
			case BOOLEAN:
			case BYTE:
			case CHAR:
			case DOUBLE:
			case FLOAT:
			case INT:
			case LONG:
			case SHORT:
				{
				setState(296); primitiveType();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(307);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,12,_ctx);
			while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(305);
					switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
					case 1:
						{
						_localctx = new FormalTypePatternContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_formalTypePattern);
						setState(299);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(300); match(AND);
						setState(301); formalTypePattern(3);
						}
						break;

					case 2:
						{
						_localctx = new FormalTypePatternContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_formalTypePattern);
						setState(302);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(303); match(OR);
						setState(304); formalTypePattern(2);
						}
						break;
					}
					} 
				}
				setState(309);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,12,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class ClassNameOrInterfaceContext extends ParserRuleContext {
		public List<TerminalNode> Identifier() { return getTokens(RefactorMethodSignatureParser.Identifier); }
		public List<TerminalNode> DOT() { return getTokens(RefactorMethodSignatureParser.DOT); }
		public TerminalNode Identifier(int i) {
			return getToken(RefactorMethodSignatureParser.Identifier, i);
		}
		public List<TerminalNode> DOTDOT() { return getTokens(RefactorMethodSignatureParser.DOTDOT); }
		public TerminalNode DOTDOT(int i) {
			return getToken(RefactorMethodSignatureParser.DOTDOT, i);
		}
		public TerminalNode DOT(int i) {
			return getToken(RefactorMethodSignatureParser.DOT, i);
		}
		public ClassNameOrInterfaceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_classNameOrInterface; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterClassNameOrInterface(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitClassNameOrInterface(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitClassNameOrInterface(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ClassNameOrInterfaceContext classNameOrInterface() throws RecognitionException {
		ClassNameOrInterfaceContext _localctx = new ClassNameOrInterfaceContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_classNameOrInterface);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(311); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(310);
					_la = _input.LA(1);
					if ( !(_la==DOTDOT || ((((_la - 123)) & ~0x3f) == 0 && ((1L << (_la - 123)) & ((1L << (DOT - 123)) | (1L << (MUL - 123)) | (1L << (Identifier - 123)))) != 0)) ) {
					_errHandler.recoverInline(this);
					}
					consume();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(313); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,13,_ctx);
			} while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SimpleNamePatternContext extends ParserRuleContext {
		public List<TerminalNode> Identifier() { return getTokens(RefactorMethodSignatureParser.Identifier); }
		public TerminalNode Identifier(int i) {
			return getToken(RefactorMethodSignatureParser.Identifier, i);
		}
		public SimpleNamePatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_simpleNamePattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterSimpleNamePattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitSimpleNamePattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitSimpleNamePattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SimpleNamePatternContext simpleNamePattern() throws RecognitionException {
		SimpleNamePatternContext _localctx = new SimpleNamePatternContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_simpleNamePattern);
		int _la;
		try {
			int _alt;
			setState(337);
			switch (_input.LA(1)) {
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(315); match(Identifier);
				setState(320);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,14,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(316); match(MUL);
						setState(317); match(Identifier);
						}
						} 
					}
					setState(322);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,14,_ctx);
				}
				setState(324);
				_la = _input.LA(1);
				if (_la==MUL) {
					{
					setState(323); match(MUL);
					}
				}

				}
				break;
			case MUL:
				enterOuterAlt(_localctx, 2);
				{
				setState(326); match(MUL);
				setState(331);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,16,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(327); match(Identifier);
						setState(328); match(MUL);
						}
						} 
					}
					setState(333);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,16,_ctx);
				}
				setState(335);
				_la = _input.LA(1);
				if (_la==Identifier) {
					{
					setState(334); match(Identifier);
					}
				}

				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CompilationUnitContext extends ParserRuleContext {
		public TypeDeclarationContext typeDeclaration(int i) {
			return getRuleContext(TypeDeclarationContext.class,i);
		}
		public ImportDeclarationContext importDeclaration(int i) {
			return getRuleContext(ImportDeclarationContext.class,i);
		}
		public List<ImportDeclarationContext> importDeclaration() {
			return getRuleContexts(ImportDeclarationContext.class);
		}
		public TerminalNode EOF() { return getToken(RefactorMethodSignatureParser.EOF, 0); }
		public PackageDeclarationContext packageDeclaration() {
			return getRuleContext(PackageDeclarationContext.class,0);
		}
		public List<TypeDeclarationContext> typeDeclaration() {
			return getRuleContexts(TypeDeclarationContext.class);
		}
		public CompilationUnitContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_compilationUnit; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterCompilationUnit(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitCompilationUnit(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitCompilationUnit(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CompilationUnitContext compilationUnit() throws RecognitionException {
		CompilationUnitContext _localctx = new CompilationUnitContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_compilationUnit);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(340);
			switch ( getInterpreter().adaptivePredict(_input,19,_ctx) ) {
			case 1:
				{
				setState(339); packageDeclaration();
				}
				break;
			}
			setState(345);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==IMPORT) {
				{
				{
				setState(342); importDeclaration();
				}
				}
				setState(347);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(351);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (ABSTRACT - 58)) | (1L << (CLASS - 58)) | (1L << (ENUM - 58)) | (1L << (FINAL - 58)) | (1L << (INTERFACE - 58)) | (1L << (PRIVATE - 58)) | (1L << (PROTECTED - 58)) | (1L << (PUBLIC - 58)) | (1L << (STATIC - 58)) | (1L << (STRICTFP - 58)) | (1L << (SEMI - 58)))) != 0)) {
				{
				{
				setState(348); typeDeclaration();
				}
				}
				setState(353);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(354); match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PackageDeclarationContext extends ParserRuleContext {
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public PackageDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_packageDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterPackageDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitPackageDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitPackageDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PackageDeclarationContext packageDeclaration() throws RecognitionException {
		PackageDeclarationContext _localctx = new PackageDeclarationContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_packageDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(359);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT) {
				{
				{
				setState(356); annotation();
				}
				}
				setState(361);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(362); match(PACKAGE);
			setState(363); qualifiedName();
			setState(364); match(SEMI);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ImportDeclarationContext extends ParserRuleContext {
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public ImportDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_importDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterImportDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitImportDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitImportDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ImportDeclarationContext importDeclaration() throws RecognitionException {
		ImportDeclarationContext _localctx = new ImportDeclarationContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_importDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(366); match(IMPORT);
			setState(368);
			_la = _input.LA(1);
			if (_la==STATIC) {
				{
				setState(367); match(STATIC);
				}
			}

			setState(370); qualifiedName();
			setState(373);
			_la = _input.LA(1);
			if (_la==DOT) {
				{
				setState(371); match(DOT);
				setState(372); match(MUL);
				}
			}

			setState(375); match(SEMI);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TypeDeclarationContext extends ParserRuleContext {
		public ClassOrInterfaceModifierContext classOrInterfaceModifier(int i) {
			return getRuleContext(ClassOrInterfaceModifierContext.class,i);
		}
		public EnumDeclarationContext enumDeclaration() {
			return getRuleContext(EnumDeclarationContext.class,0);
		}
		public ClassDeclarationContext classDeclaration() {
			return getRuleContext(ClassDeclarationContext.class,0);
		}
		public AnnotationTypeDeclarationContext annotationTypeDeclaration() {
			return getRuleContext(AnnotationTypeDeclarationContext.class,0);
		}
		public List<ClassOrInterfaceModifierContext> classOrInterfaceModifier() {
			return getRuleContexts(ClassOrInterfaceModifierContext.class);
		}
		public InterfaceDeclarationContext interfaceDeclaration() {
			return getRuleContext(InterfaceDeclarationContext.class,0);
		}
		public TypeDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterTypeDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitTypeDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitTypeDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeDeclarationContext typeDeclaration() throws RecognitionException {
		TypeDeclarationContext _localctx = new TypeDeclarationContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_typeDeclaration);
		int _la;
		try {
			int _alt;
			setState(406);
			switch ( getInterpreter().adaptivePredict(_input,29,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(380);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (ABSTRACT - 58)) | (1L << (FINAL - 58)) | (1L << (PRIVATE - 58)) | (1L << (PROTECTED - 58)) | (1L << (PUBLIC - 58)) | (1L << (STATIC - 58)) | (1L << (STRICTFP - 58)))) != 0)) {
					{
					{
					setState(377); classOrInterfaceModifier();
					}
					}
					setState(382);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(383); classDeclaration();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(387);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (ABSTRACT - 58)) | (1L << (FINAL - 58)) | (1L << (PRIVATE - 58)) | (1L << (PROTECTED - 58)) | (1L << (PUBLIC - 58)) | (1L << (STATIC - 58)) | (1L << (STRICTFP - 58)))) != 0)) {
					{
					{
					setState(384); classOrInterfaceModifier();
					}
					}
					setState(389);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(390); enumDeclaration();
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(394);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (ABSTRACT - 58)) | (1L << (FINAL - 58)) | (1L << (PRIVATE - 58)) | (1L << (PROTECTED - 58)) | (1L << (PUBLIC - 58)) | (1L << (STATIC - 58)) | (1L << (STRICTFP - 58)))) != 0)) {
					{
					{
					setState(391); classOrInterfaceModifier();
					}
					}
					setState(396);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(397); interfaceDeclaration();
				}
				break;

			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(401);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,28,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(398); classOrInterfaceModifier();
						}
						} 
					}
					setState(403);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,28,_ctx);
				}
				setState(404); annotationTypeDeclaration();
				}
				break;

			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(405); match(SEMI);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ModifierContext extends ParserRuleContext {
		public ClassOrInterfaceModifierContext classOrInterfaceModifier() {
			return getRuleContext(ClassOrInterfaceModifierContext.class,0);
		}
		public ModifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_modifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterModifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitModifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitModifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ModifierContext modifier() throws RecognitionException {
		ModifierContext _localctx = new ModifierContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_modifier);
		int _la;
		try {
			setState(410);
			switch (_input.LA(1)) {
			case AT:
			case ABSTRACT:
			case FINAL:
			case PRIVATE:
			case PROTECTED:
			case PUBLIC:
			case STATIC:
			case STRICTFP:
				enterOuterAlt(_localctx, 1);
				{
				setState(408); classOrInterfaceModifier();
				}
				break;
			case NATIVE:
			case SYNCHRONIZED:
			case TRANSIENT:
			case VOLATILE:
				enterOuterAlt(_localctx, 2);
				{
				setState(409);
				_la = _input.LA(1);
				if ( !(((((_la - 88)) & ~0x3f) == 0 && ((1L << (_la - 88)) & ((1L << (NATIVE - 88)) | (1L << (SYNCHRONIZED - 88)) | (1L << (TRANSIENT - 88)) | (1L << (VOLATILE - 88)))) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				consume();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ClassOrInterfaceModifierContext extends ParserRuleContext {
		public AnnotationContext annotation() {
			return getRuleContext(AnnotationContext.class,0);
		}
		public ClassOrInterfaceModifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_classOrInterfaceModifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterClassOrInterfaceModifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitClassOrInterfaceModifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitClassOrInterfaceModifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ClassOrInterfaceModifierContext classOrInterfaceModifier() throws RecognitionException {
		ClassOrInterfaceModifierContext _localctx = new ClassOrInterfaceModifierContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_classOrInterfaceModifier);
		int _la;
		try {
			setState(414);
			switch (_input.LA(1)) {
			case AT:
				enterOuterAlt(_localctx, 1);
				{
				setState(412); annotation();
				}
				break;
			case ABSTRACT:
			case FINAL:
			case PRIVATE:
			case PROTECTED:
			case PUBLIC:
			case STATIC:
			case STRICTFP:
				enterOuterAlt(_localctx, 2);
				{
				setState(413);
				_la = _input.LA(1);
				if ( !(((((_la - 59)) & ~0x3f) == 0 && ((1L << (_la - 59)) & ((1L << (ABSTRACT - 59)) | (1L << (FINAL - 59)) | (1L << (PRIVATE - 59)) | (1L << (PROTECTED - 59)) | (1L << (PUBLIC - 59)) | (1L << (STATIC - 59)) | (1L << (STRICTFP - 59)))) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				consume();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VariableModifierContext extends ParserRuleContext {
		public AnnotationContext annotation() {
			return getRuleContext(AnnotationContext.class,0);
		}
		public VariableModifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableModifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterVariableModifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitVariableModifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitVariableModifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableModifierContext variableModifier() throws RecognitionException {
		VariableModifierContext _localctx = new VariableModifierContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_variableModifier);
		try {
			setState(418);
			switch (_input.LA(1)) {
			case FINAL:
				enterOuterAlt(_localctx, 1);
				{
				setState(416); match(FINAL);
				}
				break;
			case AT:
				enterOuterAlt(_localctx, 2);
				{
				setState(417); annotation();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ClassDeclarationContext extends ParserRuleContext {
		public TerminalNode Identifier() { return getToken(RefactorMethodSignatureParser.Identifier, 0); }
		public ClassBodyContext classBody() {
			return getRuleContext(ClassBodyContext.class,0);
		}
		public TypeListContext typeList() {
			return getRuleContext(TypeListContext.class,0);
		}
		public TypeParametersContext typeParameters() {
			return getRuleContext(TypeParametersContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public ClassDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_classDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterClassDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitClassDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitClassDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ClassDeclarationContext classDeclaration() throws RecognitionException {
		ClassDeclarationContext _localctx = new ClassDeclarationContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_classDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(420); match(CLASS);
			setState(421); match(Identifier);
			setState(423);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(422); typeParameters();
				}
			}

			setState(427);
			_la = _input.LA(1);
			if (_la==EXTENDS) {
				{
				setState(425); match(EXTENDS);
				setState(426); type();
				}
			}

			setState(431);
			_la = _input.LA(1);
			if (_la==IMPLEMENTS) {
				{
				setState(429); match(IMPLEMENTS);
				setState(430); typeList();
				}
			}

			setState(433); classBody();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TypeParametersContext extends ParserRuleContext {
		public List<TypeParameterContext> typeParameter() {
			return getRuleContexts(TypeParameterContext.class);
		}
		public TypeParameterContext typeParameter(int i) {
			return getRuleContext(TypeParameterContext.class,i);
		}
		public TypeParametersContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeParameters; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterTypeParameters(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitTypeParameters(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitTypeParameters(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeParametersContext typeParameters() throws RecognitionException {
		TypeParametersContext _localctx = new TypeParametersContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_typeParameters);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(435); match(LT);
			setState(436); typeParameter();
			setState(441);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(437); match(COMMA);
				setState(438); typeParameter();
				}
				}
				setState(443);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(444); match(GT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TypeParameterContext extends ParserRuleContext {
		public TerminalNode Identifier() { return getToken(RefactorMethodSignatureParser.Identifier, 0); }
		public TypeBoundContext typeBound() {
			return getRuleContext(TypeBoundContext.class,0);
		}
		public TypeParameterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeParameter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterTypeParameter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitTypeParameter(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitTypeParameter(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeParameterContext typeParameter() throws RecognitionException {
		TypeParameterContext _localctx = new TypeParameterContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_typeParameter);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(446); match(Identifier);
			setState(449);
			_la = _input.LA(1);
			if (_la==EXTENDS) {
				{
				setState(447); match(EXTENDS);
				setState(448); typeBound();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TypeBoundContext extends ParserRuleContext {
		public TypeContext type(int i) {
			return getRuleContext(TypeContext.class,i);
		}
		public List<TypeContext> type() {
			return getRuleContexts(TypeContext.class);
		}
		public TypeBoundContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeBound; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterTypeBound(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitTypeBound(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitTypeBound(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeBoundContext typeBound() throws RecognitionException {
		TypeBoundContext _localctx = new TypeBoundContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_typeBound);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(451); type();
			setState(456);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==BITAND) {
				{
				{
				setState(452); match(BITAND);
				setState(453); type();
				}
				}
				setState(458);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class EnumDeclarationContext extends ParserRuleContext {
		public EnumBodyDeclarationsContext enumBodyDeclarations() {
			return getRuleContext(EnumBodyDeclarationsContext.class,0);
		}
		public TerminalNode Identifier() { return getToken(RefactorMethodSignatureParser.Identifier, 0); }
		public TypeListContext typeList() {
			return getRuleContext(TypeListContext.class,0);
		}
		public TerminalNode ENUM() { return getToken(RefactorMethodSignatureParser.ENUM, 0); }
		public EnumConstantsContext enumConstants() {
			return getRuleContext(EnumConstantsContext.class,0);
		}
		public EnumDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enumDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterEnumDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitEnumDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitEnumDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EnumDeclarationContext enumDeclaration() throws RecognitionException {
		EnumDeclarationContext _localctx = new EnumDeclarationContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_enumDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(459); match(ENUM);
			setState(460); match(Identifier);
			setState(463);
			_la = _input.LA(1);
			if (_la==IMPLEMENTS) {
				{
				setState(461); match(IMPLEMENTS);
				setState(462); typeList();
				}
			}

			setState(465); match(LBRACE);
			setState(467);
			_la = _input.LA(1);
			if (_la==AT || _la==Identifier) {
				{
				setState(466); enumConstants();
				}
			}

			setState(470);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(469); match(COMMA);
				}
			}

			setState(473);
			_la = _input.LA(1);
			if (_la==SEMI) {
				{
				setState(472); enumBodyDeclarations();
				}
			}

			setState(475); match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class EnumConstantsContext extends ParserRuleContext {
		public List<EnumConstantContext> enumConstant() {
			return getRuleContexts(EnumConstantContext.class);
		}
		public EnumConstantContext enumConstant(int i) {
			return getRuleContext(EnumConstantContext.class,i);
		}
		public EnumConstantsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enumConstants; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterEnumConstants(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitEnumConstants(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitEnumConstants(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EnumConstantsContext enumConstants() throws RecognitionException {
		EnumConstantsContext _localctx = new EnumConstantsContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_enumConstants);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(477); enumConstant();
			setState(482);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,43,_ctx);
			while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(478); match(COMMA);
					setState(479); enumConstant();
					}
					} 
				}
				setState(484);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,43,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class EnumConstantContext extends ParserRuleContext {
		public TerminalNode Identifier() { return getToken(RefactorMethodSignatureParser.Identifier, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public ClassBodyContext classBody() {
			return getRuleContext(ClassBodyContext.class,0);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public ArgumentsContext arguments() {
			return getRuleContext(ArgumentsContext.class,0);
		}
		public EnumConstantContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enumConstant; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterEnumConstant(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitEnumConstant(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitEnumConstant(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EnumConstantContext enumConstant() throws RecognitionException {
		EnumConstantContext _localctx = new EnumConstantContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_enumConstant);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(488);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT) {
				{
				{
				setState(485); annotation();
				}
				}
				setState(490);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(491); match(Identifier);
			setState(493);
			_la = _input.LA(1);
			if (_la==LPAREN) {
				{
				setState(492); arguments();
				}
			}

			setState(496);
			_la = _input.LA(1);
			if (_la==LBRACE) {
				{
				setState(495); classBody();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class EnumBodyDeclarationsContext extends ParserRuleContext {
		public List<ClassBodyDeclarationContext> classBodyDeclaration() {
			return getRuleContexts(ClassBodyDeclarationContext.class);
		}
		public ClassBodyDeclarationContext classBodyDeclaration(int i) {
			return getRuleContext(ClassBodyDeclarationContext.class,i);
		}
		public EnumBodyDeclarationsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enumBodyDeclarations; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterEnumBodyDeclarations(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitEnumBodyDeclarations(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitEnumBodyDeclarations(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EnumBodyDeclarationsContext enumBodyDeclarations() throws RecognitionException {
		EnumBodyDeclarationsContext _localctx = new EnumBodyDeclarationsContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_enumBodyDeclarations);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(498); match(SEMI);
			setState(502);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (ABSTRACT - 58)) | (1L << (BOOLEAN - 58)) | (1L << (BYTE - 58)) | (1L << (CHAR - 58)) | (1L << (CLASS - 58)) | (1L << (DOUBLE - 58)) | (1L << (ENUM - 58)) | (1L << (FINAL - 58)) | (1L << (FLOAT - 58)) | (1L << (INT - 58)) | (1L << (INTERFACE - 58)) | (1L << (LONG - 58)) | (1L << (NATIVE - 58)) | (1L << (PRIVATE - 58)) | (1L << (PROTECTED - 58)) | (1L << (PUBLIC - 58)) | (1L << (SHORT - 58)) | (1L << (STATIC - 58)) | (1L << (STRICTFP - 58)) | (1L << (SYNCHRONIZED - 58)) | (1L << (TRANSIENT - 58)) | (1L << (VOID - 58)) | (1L << (VOLATILE - 58)) | (1L << (LBRACE - 58)) | (1L << (SEMI - 58)))) != 0) || _la==LT || _la==Identifier) {
				{
				{
				setState(499); classBodyDeclaration();
				}
				}
				setState(504);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InterfaceDeclarationContext extends ParserRuleContext {
		public TerminalNode Identifier() { return getToken(RefactorMethodSignatureParser.Identifier, 0); }
		public InterfaceBodyContext interfaceBody() {
			return getRuleContext(InterfaceBodyContext.class,0);
		}
		public TypeListContext typeList() {
			return getRuleContext(TypeListContext.class,0);
		}
		public TypeParametersContext typeParameters() {
			return getRuleContext(TypeParametersContext.class,0);
		}
		public InterfaceDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interfaceDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterInterfaceDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitInterfaceDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitInterfaceDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InterfaceDeclarationContext interfaceDeclaration() throws RecognitionException {
		InterfaceDeclarationContext _localctx = new InterfaceDeclarationContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_interfaceDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(505); match(INTERFACE);
			setState(506); match(Identifier);
			setState(508);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(507); typeParameters();
				}
			}

			setState(512);
			_la = _input.LA(1);
			if (_la==EXTENDS) {
				{
				setState(510); match(EXTENDS);
				setState(511); typeList();
				}
			}

			setState(514); interfaceBody();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TypeListContext extends ParserRuleContext {
		public TypeContext type(int i) {
			return getRuleContext(TypeContext.class,i);
		}
		public List<TypeContext> type() {
			return getRuleContexts(TypeContext.class);
		}
		public TypeListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterTypeList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitTypeList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitTypeList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeListContext typeList() throws RecognitionException {
		TypeListContext _localctx = new TypeListContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_typeList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(516); type();
			setState(521);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(517); match(COMMA);
				setState(518); type();
				}
				}
				setState(523);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ClassBodyContext extends ParserRuleContext {
		public List<ClassBodyDeclarationContext> classBodyDeclaration() {
			return getRuleContexts(ClassBodyDeclarationContext.class);
		}
		public ClassBodyDeclarationContext classBodyDeclaration(int i) {
			return getRuleContext(ClassBodyDeclarationContext.class,i);
		}
		public ClassBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_classBody; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterClassBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitClassBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitClassBody(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ClassBodyContext classBody() throws RecognitionException {
		ClassBodyContext _localctx = new ClassBodyContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_classBody);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(524); match(LBRACE);
			setState(528);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (ABSTRACT - 58)) | (1L << (BOOLEAN - 58)) | (1L << (BYTE - 58)) | (1L << (CHAR - 58)) | (1L << (CLASS - 58)) | (1L << (DOUBLE - 58)) | (1L << (ENUM - 58)) | (1L << (FINAL - 58)) | (1L << (FLOAT - 58)) | (1L << (INT - 58)) | (1L << (INTERFACE - 58)) | (1L << (LONG - 58)) | (1L << (NATIVE - 58)) | (1L << (PRIVATE - 58)) | (1L << (PROTECTED - 58)) | (1L << (PUBLIC - 58)) | (1L << (SHORT - 58)) | (1L << (STATIC - 58)) | (1L << (STRICTFP - 58)) | (1L << (SYNCHRONIZED - 58)) | (1L << (TRANSIENT - 58)) | (1L << (VOID - 58)) | (1L << (VOLATILE - 58)) | (1L << (LBRACE - 58)) | (1L << (SEMI - 58)))) != 0) || _la==LT || _la==Identifier) {
				{
				{
				setState(525); classBodyDeclaration();
				}
				}
				setState(530);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(531); match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InterfaceBodyContext extends ParserRuleContext {
		public List<InterfaceBodyDeclarationContext> interfaceBodyDeclaration() {
			return getRuleContexts(InterfaceBodyDeclarationContext.class);
		}
		public InterfaceBodyDeclarationContext interfaceBodyDeclaration(int i) {
			return getRuleContext(InterfaceBodyDeclarationContext.class,i);
		}
		public InterfaceBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interfaceBody; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterInterfaceBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitInterfaceBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitInterfaceBody(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InterfaceBodyContext interfaceBody() throws RecognitionException {
		InterfaceBodyContext _localctx = new InterfaceBodyContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_interfaceBody);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(533); match(LBRACE);
			setState(537);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (ABSTRACT - 58)) | (1L << (BOOLEAN - 58)) | (1L << (BYTE - 58)) | (1L << (CHAR - 58)) | (1L << (CLASS - 58)) | (1L << (DOUBLE - 58)) | (1L << (ENUM - 58)) | (1L << (FINAL - 58)) | (1L << (FLOAT - 58)) | (1L << (INT - 58)) | (1L << (INTERFACE - 58)) | (1L << (LONG - 58)) | (1L << (NATIVE - 58)) | (1L << (PRIVATE - 58)) | (1L << (PROTECTED - 58)) | (1L << (PUBLIC - 58)) | (1L << (SHORT - 58)) | (1L << (STATIC - 58)) | (1L << (STRICTFP - 58)) | (1L << (SYNCHRONIZED - 58)) | (1L << (TRANSIENT - 58)) | (1L << (VOID - 58)) | (1L << (VOLATILE - 58)) | (1L << (SEMI - 58)))) != 0) || _la==LT || _la==Identifier) {
				{
				{
				setState(534); interfaceBodyDeclaration();
				}
				}
				setState(539);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(540); match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ClassBodyDeclarationContext extends ParserRuleContext {
		public List<ModifierContext> modifier() {
			return getRuleContexts(ModifierContext.class);
		}
		public MemberDeclarationContext memberDeclaration() {
			return getRuleContext(MemberDeclarationContext.class,0);
		}
		public ModifierContext modifier(int i) {
			return getRuleContext(ModifierContext.class,i);
		}
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public ClassBodyDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_classBodyDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterClassBodyDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitClassBodyDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitClassBodyDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ClassBodyDeclarationContext classBodyDeclaration() throws RecognitionException {
		ClassBodyDeclarationContext _localctx = new ClassBodyDeclarationContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_classBodyDeclaration);
		int _la;
		try {
			int _alt;
			setState(554);
			switch ( getInterpreter().adaptivePredict(_input,55,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(542); match(SEMI);
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(544);
				_la = _input.LA(1);
				if (_la==STATIC) {
					{
					setState(543); match(STATIC);
					}
				}

				setState(546); block();
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(550);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,54,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(547); modifier();
						}
						} 
					}
					setState(552);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,54,_ctx);
				}
				setState(553); memberDeclaration();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class MemberDeclarationContext extends ParserRuleContext {
		public GenericMethodDeclarationContext genericMethodDeclaration() {
			return getRuleContext(GenericMethodDeclarationContext.class,0);
		}
		public MethodDeclarationContext methodDeclaration() {
			return getRuleContext(MethodDeclarationContext.class,0);
		}
		public EnumDeclarationContext enumDeclaration() {
			return getRuleContext(EnumDeclarationContext.class,0);
		}
		public ClassDeclarationContext classDeclaration() {
			return getRuleContext(ClassDeclarationContext.class,0);
		}
		public AnnotationTypeDeclarationContext annotationTypeDeclaration() {
			return getRuleContext(AnnotationTypeDeclarationContext.class,0);
		}
		public GenericConstructorDeclarationContext genericConstructorDeclaration() {
			return getRuleContext(GenericConstructorDeclarationContext.class,0);
		}
		public InterfaceDeclarationContext interfaceDeclaration() {
			return getRuleContext(InterfaceDeclarationContext.class,0);
		}
		public ConstructorDeclarationContext constructorDeclaration() {
			return getRuleContext(ConstructorDeclarationContext.class,0);
		}
		public FieldDeclarationContext fieldDeclaration() {
			return getRuleContext(FieldDeclarationContext.class,0);
		}
		public MemberDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_memberDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterMemberDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitMemberDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitMemberDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MemberDeclarationContext memberDeclaration() throws RecognitionException {
		MemberDeclarationContext _localctx = new MemberDeclarationContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_memberDeclaration);
		try {
			setState(565);
			switch ( getInterpreter().adaptivePredict(_input,56,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(556); methodDeclaration();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(557); genericMethodDeclaration();
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(558); fieldDeclaration();
				}
				break;

			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(559); constructorDeclaration();
				}
				break;

			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(560); genericConstructorDeclaration();
				}
				break;

			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(561); interfaceDeclaration();
				}
				break;

			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(562); annotationTypeDeclaration();
				}
				break;

			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(563); classDeclaration();
				}
				break;

			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(564); enumDeclaration();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class MethodDeclarationContext extends ParserRuleContext {
		public TerminalNode Identifier() { return getToken(RefactorMethodSignatureParser.Identifier, 0); }
		public MethodBodyContext methodBody() {
			return getRuleContext(MethodBodyContext.class,0);
		}
		public QualifiedNameListContext qualifiedNameList() {
			return getRuleContext(QualifiedNameListContext.class,0);
		}
		public FormalParametersContext formalParameters() {
			return getRuleContext(FormalParametersContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public MethodDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_methodDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterMethodDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitMethodDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitMethodDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MethodDeclarationContext methodDeclaration() throws RecognitionException {
		MethodDeclarationContext _localctx = new MethodDeclarationContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_methodDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(569);
			switch (_input.LA(1)) {
			case BOOLEAN:
			case BYTE:
			case CHAR:
			case DOUBLE:
			case FLOAT:
			case INT:
			case LONG:
			case SHORT:
			case Identifier:
				{
				setState(567); type();
				}
				break;
			case VOID:
				{
				setState(568); match(VOID);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(571); match(Identifier);
			setState(572); formalParameters();
			setState(577);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==LBRACK) {
				{
				{
				setState(573); match(LBRACK);
				setState(574); match(RBRACK);
				}
				}
				setState(579);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(582);
			_la = _input.LA(1);
			if (_la==THROWS) {
				{
				setState(580); match(THROWS);
				setState(581); qualifiedNameList();
				}
			}

			setState(586);
			switch (_input.LA(1)) {
			case LBRACE:
				{
				setState(584); methodBody();
				}
				break;
			case SEMI:
				{
				setState(585); match(SEMI);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GenericMethodDeclarationContext extends ParserRuleContext {
		public MethodDeclarationContext methodDeclaration() {
			return getRuleContext(MethodDeclarationContext.class,0);
		}
		public TypeParametersContext typeParameters() {
			return getRuleContext(TypeParametersContext.class,0);
		}
		public GenericMethodDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_genericMethodDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterGenericMethodDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitGenericMethodDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitGenericMethodDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GenericMethodDeclarationContext genericMethodDeclaration() throws RecognitionException {
		GenericMethodDeclarationContext _localctx = new GenericMethodDeclarationContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_genericMethodDeclaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(588); typeParameters();
			setState(589); methodDeclaration();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ConstructorDeclarationContext extends ParserRuleContext {
		public TerminalNode Identifier() { return getToken(RefactorMethodSignatureParser.Identifier, 0); }
		public ConstructorBodyContext constructorBody() {
			return getRuleContext(ConstructorBodyContext.class,0);
		}
		public QualifiedNameListContext qualifiedNameList() {
			return getRuleContext(QualifiedNameListContext.class,0);
		}
		public FormalParametersContext formalParameters() {
			return getRuleContext(FormalParametersContext.class,0);
		}
		public ConstructorDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constructorDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterConstructorDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitConstructorDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitConstructorDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstructorDeclarationContext constructorDeclaration() throws RecognitionException {
		ConstructorDeclarationContext _localctx = new ConstructorDeclarationContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_constructorDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(591); match(Identifier);
			setState(592); formalParameters();
			setState(595);
			_la = _input.LA(1);
			if (_la==THROWS) {
				{
				setState(593); match(THROWS);
				setState(594); qualifiedNameList();
				}
			}

			setState(597); constructorBody();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GenericConstructorDeclarationContext extends ParserRuleContext {
		public TypeParametersContext typeParameters() {
			return getRuleContext(TypeParametersContext.class,0);
		}
		public ConstructorDeclarationContext constructorDeclaration() {
			return getRuleContext(ConstructorDeclarationContext.class,0);
		}
		public GenericConstructorDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_genericConstructorDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterGenericConstructorDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitGenericConstructorDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitGenericConstructorDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GenericConstructorDeclarationContext genericConstructorDeclaration() throws RecognitionException {
		GenericConstructorDeclarationContext _localctx = new GenericConstructorDeclarationContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_genericConstructorDeclaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(599); typeParameters();
			setState(600); constructorDeclaration();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FieldDeclarationContext extends ParserRuleContext {
		public VariableDeclaratorsContext variableDeclarators() {
			return getRuleContext(VariableDeclaratorsContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public FieldDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fieldDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterFieldDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitFieldDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitFieldDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FieldDeclarationContext fieldDeclaration() throws RecognitionException {
		FieldDeclarationContext _localctx = new FieldDeclarationContext(_ctx, getState());
		enterRule(_localctx, 70, RULE_fieldDeclaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(602); type();
			setState(603); variableDeclarators();
			setState(604); match(SEMI);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InterfaceBodyDeclarationContext extends ParserRuleContext {
		public List<ModifierContext> modifier() {
			return getRuleContexts(ModifierContext.class);
		}
		public ModifierContext modifier(int i) {
			return getRuleContext(ModifierContext.class,i);
		}
		public InterfaceMemberDeclarationContext interfaceMemberDeclaration() {
			return getRuleContext(InterfaceMemberDeclarationContext.class,0);
		}
		public InterfaceBodyDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interfaceBodyDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterInterfaceBodyDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitInterfaceBodyDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitInterfaceBodyDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InterfaceBodyDeclarationContext interfaceBodyDeclaration() throws RecognitionException {
		InterfaceBodyDeclarationContext _localctx = new InterfaceBodyDeclarationContext(_ctx, getState());
		enterRule(_localctx, 72, RULE_interfaceBodyDeclaration);
		try {
			int _alt;
			setState(614);
			switch (_input.LA(1)) {
			case AT:
			case ABSTRACT:
			case BOOLEAN:
			case BYTE:
			case CHAR:
			case CLASS:
			case DOUBLE:
			case ENUM:
			case FINAL:
			case FLOAT:
			case INT:
			case INTERFACE:
			case LONG:
			case NATIVE:
			case PRIVATE:
			case PROTECTED:
			case PUBLIC:
			case SHORT:
			case STATIC:
			case STRICTFP:
			case SYNCHRONIZED:
			case TRANSIENT:
			case VOID:
			case VOLATILE:
			case LT:
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(609);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,62,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(606); modifier();
						}
						} 
					}
					setState(611);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,62,_ctx);
				}
				setState(612); interfaceMemberDeclaration();
				}
				break;
			case SEMI:
				enterOuterAlt(_localctx, 2);
				{
				setState(613); match(SEMI);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InterfaceMemberDeclarationContext extends ParserRuleContext {
		public EnumDeclarationContext enumDeclaration() {
			return getRuleContext(EnumDeclarationContext.class,0);
		}
		public ClassDeclarationContext classDeclaration() {
			return getRuleContext(ClassDeclarationContext.class,0);
		}
		public GenericInterfaceMethodDeclarationContext genericInterfaceMethodDeclaration() {
			return getRuleContext(GenericInterfaceMethodDeclarationContext.class,0);
		}
		public AnnotationTypeDeclarationContext annotationTypeDeclaration() {
			return getRuleContext(AnnotationTypeDeclarationContext.class,0);
		}
		public InterfaceDeclarationContext interfaceDeclaration() {
			return getRuleContext(InterfaceDeclarationContext.class,0);
		}
		public ConstDeclarationContext constDeclaration() {
			return getRuleContext(ConstDeclarationContext.class,0);
		}
		public InterfaceMethodDeclarationContext interfaceMethodDeclaration() {
			return getRuleContext(InterfaceMethodDeclarationContext.class,0);
		}
		public InterfaceMemberDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interfaceMemberDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterInterfaceMemberDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitInterfaceMemberDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitInterfaceMemberDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InterfaceMemberDeclarationContext interfaceMemberDeclaration() throws RecognitionException {
		InterfaceMemberDeclarationContext _localctx = new InterfaceMemberDeclarationContext(_ctx, getState());
		enterRule(_localctx, 74, RULE_interfaceMemberDeclaration);
		try {
			setState(623);
			switch ( getInterpreter().adaptivePredict(_input,64,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(616); constDeclaration();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(617); interfaceMethodDeclaration();
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(618); genericInterfaceMethodDeclaration();
				}
				break;

			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(619); interfaceDeclaration();
				}
				break;

			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(620); annotationTypeDeclaration();
				}
				break;

			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(621); classDeclaration();
				}
				break;

			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(622); enumDeclaration();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ConstDeclarationContext extends ParserRuleContext {
		public ConstantDeclaratorContext constantDeclarator(int i) {
			return getRuleContext(ConstantDeclaratorContext.class,i);
		}
		public List<ConstantDeclaratorContext> constantDeclarator() {
			return getRuleContexts(ConstantDeclaratorContext.class);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public ConstDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterConstDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitConstDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitConstDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstDeclarationContext constDeclaration() throws RecognitionException {
		ConstDeclarationContext _localctx = new ConstDeclarationContext(_ctx, getState());
		enterRule(_localctx, 76, RULE_constDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(625); type();
			setState(626); constantDeclarator();
			setState(631);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(627); match(COMMA);
				setState(628); constantDeclarator();
				}
				}
				setState(633);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(634); match(SEMI);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ConstantDeclaratorContext extends ParserRuleContext {
		public TerminalNode Identifier() { return getToken(RefactorMethodSignatureParser.Identifier, 0); }
		public VariableInitializerContext variableInitializer() {
			return getRuleContext(VariableInitializerContext.class,0);
		}
		public ConstantDeclaratorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constantDeclarator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterConstantDeclarator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitConstantDeclarator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitConstantDeclarator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstantDeclaratorContext constantDeclarator() throws RecognitionException {
		ConstantDeclaratorContext _localctx = new ConstantDeclaratorContext(_ctx, getState());
		enterRule(_localctx, 78, RULE_constantDeclarator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(636); match(Identifier);
			setState(641);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==LBRACK) {
				{
				{
				setState(637); match(LBRACK);
				setState(638); match(RBRACK);
				}
				}
				setState(643);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(644); match(ASSIGN);
			setState(645); variableInitializer();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InterfaceMethodDeclarationContext extends ParserRuleContext {
		public TerminalNode Identifier() { return getToken(RefactorMethodSignatureParser.Identifier, 0); }
		public QualifiedNameListContext qualifiedNameList() {
			return getRuleContext(QualifiedNameListContext.class,0);
		}
		public FormalParametersContext formalParameters() {
			return getRuleContext(FormalParametersContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public InterfaceMethodDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interfaceMethodDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterInterfaceMethodDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitInterfaceMethodDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitInterfaceMethodDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InterfaceMethodDeclarationContext interfaceMethodDeclaration() throws RecognitionException {
		InterfaceMethodDeclarationContext _localctx = new InterfaceMethodDeclarationContext(_ctx, getState());
		enterRule(_localctx, 80, RULE_interfaceMethodDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(649);
			switch (_input.LA(1)) {
			case BOOLEAN:
			case BYTE:
			case CHAR:
			case DOUBLE:
			case FLOAT:
			case INT:
			case LONG:
			case SHORT:
			case Identifier:
				{
				setState(647); type();
				}
				break;
			case VOID:
				{
				setState(648); match(VOID);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(651); match(Identifier);
			setState(652); formalParameters();
			setState(657);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==LBRACK) {
				{
				{
				setState(653); match(LBRACK);
				setState(654); match(RBRACK);
				}
				}
				setState(659);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(662);
			_la = _input.LA(1);
			if (_la==THROWS) {
				{
				setState(660); match(THROWS);
				setState(661); qualifiedNameList();
				}
			}

			setState(664); match(SEMI);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class GenericInterfaceMethodDeclarationContext extends ParserRuleContext {
		public TypeParametersContext typeParameters() {
			return getRuleContext(TypeParametersContext.class,0);
		}
		public InterfaceMethodDeclarationContext interfaceMethodDeclaration() {
			return getRuleContext(InterfaceMethodDeclarationContext.class,0);
		}
		public GenericInterfaceMethodDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_genericInterfaceMethodDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterGenericInterfaceMethodDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitGenericInterfaceMethodDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitGenericInterfaceMethodDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GenericInterfaceMethodDeclarationContext genericInterfaceMethodDeclaration() throws RecognitionException {
		GenericInterfaceMethodDeclarationContext _localctx = new GenericInterfaceMethodDeclarationContext(_ctx, getState());
		enterRule(_localctx, 82, RULE_genericInterfaceMethodDeclaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(666); typeParameters();
			setState(667); interfaceMethodDeclaration();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VariableDeclaratorsContext extends ParserRuleContext {
		public List<VariableDeclaratorContext> variableDeclarator() {
			return getRuleContexts(VariableDeclaratorContext.class);
		}
		public VariableDeclaratorContext variableDeclarator(int i) {
			return getRuleContext(VariableDeclaratorContext.class,i);
		}
		public VariableDeclaratorsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableDeclarators; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterVariableDeclarators(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitVariableDeclarators(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitVariableDeclarators(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableDeclaratorsContext variableDeclarators() throws RecognitionException {
		VariableDeclaratorsContext _localctx = new VariableDeclaratorsContext(_ctx, getState());
		enterRule(_localctx, 84, RULE_variableDeclarators);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(669); variableDeclarator();
			setState(674);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(670); match(COMMA);
				setState(671); variableDeclarator();
				}
				}
				setState(676);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VariableDeclaratorContext extends ParserRuleContext {
		public VariableInitializerContext variableInitializer() {
			return getRuleContext(VariableInitializerContext.class,0);
		}
		public VariableDeclaratorIdContext variableDeclaratorId() {
			return getRuleContext(VariableDeclaratorIdContext.class,0);
		}
		public VariableDeclaratorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableDeclarator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterVariableDeclarator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitVariableDeclarator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitVariableDeclarator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableDeclaratorContext variableDeclarator() throws RecognitionException {
		VariableDeclaratorContext _localctx = new VariableDeclaratorContext(_ctx, getState());
		enterRule(_localctx, 86, RULE_variableDeclarator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(677); variableDeclaratorId();
			setState(680);
			_la = _input.LA(1);
			if (_la==ASSIGN) {
				{
				setState(678); match(ASSIGN);
				setState(679); variableInitializer();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VariableDeclaratorIdContext extends ParserRuleContext {
		public TerminalNode Identifier() { return getToken(RefactorMethodSignatureParser.Identifier, 0); }
		public VariableDeclaratorIdContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableDeclaratorId; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterVariableDeclaratorId(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitVariableDeclaratorId(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitVariableDeclaratorId(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableDeclaratorIdContext variableDeclaratorId() throws RecognitionException {
		VariableDeclaratorIdContext _localctx = new VariableDeclaratorIdContext(_ctx, getState());
		enterRule(_localctx, 88, RULE_variableDeclaratorId);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(682); match(Identifier);
			setState(687);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==LBRACK) {
				{
				{
				setState(683); match(LBRACK);
				setState(684); match(RBRACK);
				}
				}
				setState(689);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class VariableInitializerContext extends ParserRuleContext {
		public ArrayInitializerContext arrayInitializer() {
			return getRuleContext(ArrayInitializerContext.class,0);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public VariableInitializerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableInitializer; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterVariableInitializer(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitVariableInitializer(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitVariableInitializer(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableInitializerContext variableInitializer() throws RecognitionException {
		VariableInitializerContext _localctx = new VariableInitializerContext(_ctx, getState());
		enterRule(_localctx, 90, RULE_variableInitializer);
		try {
			setState(692);
			switch (_input.LA(1)) {
			case LBRACE:
				enterOuterAlt(_localctx, 1);
				{
				setState(690); arrayInitializer();
				}
				break;
			case BOOLEAN:
			case BYTE:
			case CHAR:
			case DOUBLE:
			case FLOAT:
			case INT:
			case LONG:
			case NEW:
			case SHORT:
			case SUPER:
			case THIS:
			case VOID:
			case IntegerLiteral:
			case FloatingPointLiteral:
			case BooleanLiteral:
			case CharacterLiteral:
			case StringLiteral:
			case NullLiteral:
			case LPAREN:
			case LT:
			case BANG:
			case TILDE:
			case INC:
			case DEC:
			case ADD:
			case SUB:
			case Identifier:
				enterOuterAlt(_localctx, 2);
				{
				setState(691); expression(0);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ArrayInitializerContext extends ParserRuleContext {
		public List<VariableInitializerContext> variableInitializer() {
			return getRuleContexts(VariableInitializerContext.class);
		}
		public VariableInitializerContext variableInitializer(int i) {
			return getRuleContext(VariableInitializerContext.class,i);
		}
		public ArrayInitializerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arrayInitializer; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterArrayInitializer(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitArrayInitializer(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitArrayInitializer(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArrayInitializerContext arrayInitializer() throws RecognitionException {
		ArrayInitializerContext _localctx = new ArrayInitializerContext(_ctx, getState());
		enterRule(_localctx, 92, RULE_arrayInitializer);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(694); match(LBRACE);
			setState(706);
			_la = _input.LA(1);
			if (((((_la - 61)) & ~0x3f) == 0 && ((1L << (_la - 61)) & ((1L << (BOOLEAN - 61)) | (1L << (BYTE - 61)) | (1L << (CHAR - 61)) | (1L << (DOUBLE - 61)) | (1L << (FLOAT - 61)) | (1L << (INT - 61)) | (1L << (LONG - 61)) | (1L << (NEW - 61)) | (1L << (SHORT - 61)) | (1L << (SUPER - 61)) | (1L << (THIS - 61)) | (1L << (VOID - 61)) | (1L << (IntegerLiteral - 61)) | (1L << (FloatingPointLiteral - 61)) | (1L << (BooleanLiteral - 61)) | (1L << (CharacterLiteral - 61)) | (1L << (StringLiteral - 61)) | (1L << (NullLiteral - 61)) | (1L << (LPAREN - 61)) | (1L << (LBRACE - 61)))) != 0) || ((((_la - 126)) & ~0x3f) == 0 && ((1L << (_la - 126)) & ((1L << (LT - 126)) | (1L << (BANG - 126)) | (1L << (TILDE - 126)) | (1L << (INC - 126)) | (1L << (DEC - 126)) | (1L << (ADD - 126)) | (1L << (SUB - 126)) | (1L << (Identifier - 126)))) != 0)) {
				{
				setState(695); variableInitializer();
				setState(700);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,74,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(696); match(COMMA);
						setState(697); variableInitializer();
						}
						} 
					}
					setState(702);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,74,_ctx);
				}
				setState(704);
				_la = _input.LA(1);
				if (_la==COMMA) {
					{
					setState(703); match(COMMA);
					}
				}

				}
			}

			setState(708); match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class EnumConstantNameContext extends ParserRuleContext {
		public TerminalNode Identifier() { return getToken(RefactorMethodSignatureParser.Identifier, 0); }
		public EnumConstantNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enumConstantName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterEnumConstantName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitEnumConstantName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitEnumConstantName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EnumConstantNameContext enumConstantName() throws RecognitionException {
		EnumConstantNameContext _localctx = new EnumConstantNameContext(_ctx, getState());
		enterRule(_localctx, 94, RULE_enumConstantName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(710); match(Identifier);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TypeContext extends ParserRuleContext {
		public PrimitiveTypeContext primitiveType() {
			return getRuleContext(PrimitiveTypeContext.class,0);
		}
		public ClassOrInterfaceTypeContext classOrInterfaceType() {
			return getRuleContext(ClassOrInterfaceTypeContext.class,0);
		}
		public TypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeContext type() throws RecognitionException {
		TypeContext _localctx = new TypeContext(_ctx, getState());
		enterRule(_localctx, 96, RULE_type);
		try {
			int _alt;
			setState(728);
			switch (_input.LA(1)) {
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(712); classOrInterfaceType();
				setState(717);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,77,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(713); match(LBRACK);
						setState(714); match(RBRACK);
						}
						} 
					}
					setState(719);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,77,_ctx);
				}
				}
				break;
			case BOOLEAN:
			case BYTE:
			case CHAR:
			case DOUBLE:
			case FLOAT:
			case INT:
			case LONG:
			case SHORT:
				enterOuterAlt(_localctx, 2);
				{
				setState(720); primitiveType();
				setState(725);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,78,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(721); match(LBRACK);
						setState(722); match(RBRACK);
						}
						} 
					}
					setState(727);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,78,_ctx);
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ClassOrInterfaceTypeContext extends ParserRuleContext {
		public List<TypeArgumentsContext> typeArguments() {
			return getRuleContexts(TypeArgumentsContext.class);
		}
		public List<TerminalNode> Identifier() { return getTokens(RefactorMethodSignatureParser.Identifier); }
		public TerminalNode Identifier(int i) {
			return getToken(RefactorMethodSignatureParser.Identifier, i);
		}
		public TypeArgumentsContext typeArguments(int i) {
			return getRuleContext(TypeArgumentsContext.class,i);
		}
		public ClassOrInterfaceTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_classOrInterfaceType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterClassOrInterfaceType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitClassOrInterfaceType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitClassOrInterfaceType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ClassOrInterfaceTypeContext classOrInterfaceType() throws RecognitionException {
		ClassOrInterfaceTypeContext _localctx = new ClassOrInterfaceTypeContext(_ctx, getState());
		enterRule(_localctx, 98, RULE_classOrInterfaceType);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(730); match(Identifier);
			setState(732);
			switch ( getInterpreter().adaptivePredict(_input,80,_ctx) ) {
			case 1:
				{
				setState(731); typeArguments();
				}
				break;
			}
			setState(741);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,82,_ctx);
			while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(734); match(DOT);
					setState(735); match(Identifier);
					setState(737);
					switch ( getInterpreter().adaptivePredict(_input,81,_ctx) ) {
					case 1:
						{
						setState(736); typeArguments();
						}
						break;
					}
					}
					} 
				}
				setState(743);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,82,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PrimitiveTypeContext extends ParserRuleContext {
		public PrimitiveTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_primitiveType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterPrimitiveType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitPrimitiveType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitPrimitiveType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PrimitiveTypeContext primitiveType() throws RecognitionException {
		PrimitiveTypeContext _localctx = new PrimitiveTypeContext(_ctx, getState());
		enterRule(_localctx, 100, RULE_primitiveType);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(744);
			_la = _input.LA(1);
			if ( !(((((_la - 61)) & ~0x3f) == 0 && ((1L << (_la - 61)) & ((1L << (BOOLEAN - 61)) | (1L << (BYTE - 61)) | (1L << (CHAR - 61)) | (1L << (DOUBLE - 61)) | (1L << (FLOAT - 61)) | (1L << (INT - 61)) | (1L << (LONG - 61)) | (1L << (SHORT - 61)))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			consume();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TypeArgumentsContext extends ParserRuleContext {
		public List<TypeArgumentContext> typeArgument() {
			return getRuleContexts(TypeArgumentContext.class);
		}
		public TypeArgumentContext typeArgument(int i) {
			return getRuleContext(TypeArgumentContext.class,i);
		}
		public TypeArgumentsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeArguments; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterTypeArguments(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitTypeArguments(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitTypeArguments(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeArgumentsContext typeArguments() throws RecognitionException {
		TypeArgumentsContext _localctx = new TypeArgumentsContext(_ctx, getState());
		enterRule(_localctx, 102, RULE_typeArguments);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(746); match(LT);
			setState(747); typeArgument();
			setState(752);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(748); match(COMMA);
				setState(749); typeArgument();
				}
				}
				setState(754);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(755); match(GT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TypeArgumentContext extends ParserRuleContext {
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TypeArgumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeArgument; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterTypeArgument(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitTypeArgument(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitTypeArgument(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeArgumentContext typeArgument() throws RecognitionException {
		TypeArgumentContext _localctx = new TypeArgumentContext(_ctx, getState());
		enterRule(_localctx, 104, RULE_typeArgument);
		int _la;
		try {
			setState(763);
			switch (_input.LA(1)) {
			case BOOLEAN:
			case BYTE:
			case CHAR:
			case DOUBLE:
			case FLOAT:
			case INT:
			case LONG:
			case SHORT:
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(757); type();
				}
				break;
			case QUESTION:
				enterOuterAlt(_localctx, 2);
				{
				setState(758); match(QUESTION);
				setState(761);
				_la = _input.LA(1);
				if (_la==EXTENDS || _la==SUPER) {
					{
					setState(759);
					_la = _input.LA(1);
					if ( !(_la==EXTENDS || _la==SUPER) ) {
					_errHandler.recoverInline(this);
					}
					consume();
					setState(760); type();
					}
				}

				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class QualifiedNameListContext extends ParserRuleContext {
		public List<QualifiedNameContext> qualifiedName() {
			return getRuleContexts(QualifiedNameContext.class);
		}
		public QualifiedNameContext qualifiedName(int i) {
			return getRuleContext(QualifiedNameContext.class,i);
		}
		public QualifiedNameListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_qualifiedNameList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterQualifiedNameList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitQualifiedNameList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitQualifiedNameList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QualifiedNameListContext qualifiedNameList() throws RecognitionException {
		QualifiedNameListContext _localctx = new QualifiedNameListContext(_ctx, getState());
		enterRule(_localctx, 106, RULE_qualifiedNameList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(765); qualifiedName();
			setState(770);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(766); match(COMMA);
				setState(767); qualifiedName();
				}
				}
				setState(772);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FormalParametersContext extends ParserRuleContext {
		public FormalParameterListContext formalParameterList() {
			return getRuleContext(FormalParameterListContext.class,0);
		}
		public FormalParametersContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_formalParameters; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterFormalParameters(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitFormalParameters(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitFormalParameters(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FormalParametersContext formalParameters() throws RecognitionException {
		FormalParametersContext _localctx = new FormalParametersContext(_ctx, getState());
		enterRule(_localctx, 108, RULE_formalParameters);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(773); match(LPAREN);
			setState(775);
			_la = _input.LA(1);
			if (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (BOOLEAN - 58)) | (1L << (BYTE - 58)) | (1L << (CHAR - 58)) | (1L << (DOUBLE - 58)) | (1L << (FINAL - 58)) | (1L << (FLOAT - 58)) | (1L << (INT - 58)) | (1L << (LONG - 58)) | (1L << (SHORT - 58)))) != 0) || _la==Identifier) {
				{
				setState(774); formalParameterList();
				}
			}

			setState(777); match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FormalParameterListContext extends ParserRuleContext {
		public List<FormalParameterContext> formalParameter() {
			return getRuleContexts(FormalParameterContext.class);
		}
		public LastFormalParameterContext lastFormalParameter() {
			return getRuleContext(LastFormalParameterContext.class,0);
		}
		public FormalParameterContext formalParameter(int i) {
			return getRuleContext(FormalParameterContext.class,i);
		}
		public FormalParameterListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_formalParameterList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterFormalParameterList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitFormalParameterList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitFormalParameterList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FormalParameterListContext formalParameterList() throws RecognitionException {
		FormalParameterListContext _localctx = new FormalParameterListContext(_ctx, getState());
		enterRule(_localctx, 110, RULE_formalParameterList);
		int _la;
		try {
			int _alt;
			setState(792);
			switch ( getInterpreter().adaptivePredict(_input,90,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(779); formalParameter();
				setState(784);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,88,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(780); match(COMMA);
						setState(781); formalParameter();
						}
						} 
					}
					setState(786);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,88,_ctx);
				}
				setState(789);
				_la = _input.LA(1);
				if (_la==COMMA) {
					{
					setState(787); match(COMMA);
					setState(788); lastFormalParameter();
					}
				}

				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(791); lastFormalParameter();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FormalParameterContext extends ParserRuleContext {
		public VariableModifierContext variableModifier(int i) {
			return getRuleContext(VariableModifierContext.class,i);
		}
		public List<VariableModifierContext> variableModifier() {
			return getRuleContexts(VariableModifierContext.class);
		}
		public VariableDeclaratorIdContext variableDeclaratorId() {
			return getRuleContext(VariableDeclaratorIdContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public FormalParameterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_formalParameter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterFormalParameter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitFormalParameter(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitFormalParameter(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FormalParameterContext formalParameter() throws RecognitionException {
		FormalParameterContext _localctx = new FormalParameterContext(_ctx, getState());
		enterRule(_localctx, 112, RULE_formalParameter);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(797);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==FINAL) {
				{
				{
				setState(794); variableModifier();
				}
				}
				setState(799);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(800); type();
			setState(801); variableDeclaratorId();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class LastFormalParameterContext extends ParserRuleContext {
		public VariableModifierContext variableModifier(int i) {
			return getRuleContext(VariableModifierContext.class,i);
		}
		public List<VariableModifierContext> variableModifier() {
			return getRuleContexts(VariableModifierContext.class);
		}
		public VariableDeclaratorIdContext variableDeclaratorId() {
			return getRuleContext(VariableDeclaratorIdContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public LastFormalParameterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_lastFormalParameter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterLastFormalParameter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitLastFormalParameter(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitLastFormalParameter(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LastFormalParameterContext lastFormalParameter() throws RecognitionException {
		LastFormalParameterContext _localctx = new LastFormalParameterContext(_ctx, getState());
		enterRule(_localctx, 114, RULE_lastFormalParameter);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(806);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==FINAL) {
				{
				{
				setState(803); variableModifier();
				}
				}
				setState(808);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(809); type();
			setState(810); match(ELLIPSIS);
			setState(811); variableDeclaratorId();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class MethodBodyContext extends ParserRuleContext {
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public MethodBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_methodBody; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterMethodBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitMethodBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitMethodBody(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MethodBodyContext methodBody() throws RecognitionException {
		MethodBodyContext _localctx = new MethodBodyContext(_ctx, getState());
		enterRule(_localctx, 116, RULE_methodBody);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(813); block();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ConstructorBodyContext extends ParserRuleContext {
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public ConstructorBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constructorBody; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterConstructorBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitConstructorBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitConstructorBody(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstructorBodyContext constructorBody() throws RecognitionException {
		ConstructorBodyContext _localctx = new ConstructorBodyContext(_ctx, getState());
		enterRule(_localctx, 118, RULE_constructorBody);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(815); block();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class QualifiedNameContext extends ParserRuleContext {
		public List<TerminalNode> Identifier() { return getTokens(RefactorMethodSignatureParser.Identifier); }
		public TerminalNode Identifier(int i) {
			return getToken(RefactorMethodSignatureParser.Identifier, i);
		}
		public QualifiedNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_qualifiedName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterQualifiedName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitQualifiedName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitQualifiedName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QualifiedNameContext qualifiedName() throws RecognitionException {
		QualifiedNameContext _localctx = new QualifiedNameContext(_ctx, getState());
		enterRule(_localctx, 120, RULE_qualifiedName);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(817); match(Identifier);
			setState(822);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,93,_ctx);
			while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(818); match(DOT);
					setState(819); match(Identifier);
					}
					} 
				}
				setState(824);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,93,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class LiteralContext extends ParserRuleContext {
		public TerminalNode StringLiteral() { return getToken(RefactorMethodSignatureParser.StringLiteral, 0); }
		public TerminalNode IntegerLiteral() { return getToken(RefactorMethodSignatureParser.IntegerLiteral, 0); }
		public TerminalNode FloatingPointLiteral() { return getToken(RefactorMethodSignatureParser.FloatingPointLiteral, 0); }
		public TerminalNode BooleanLiteral() { return getToken(RefactorMethodSignatureParser.BooleanLiteral, 0); }
		public TerminalNode CharacterLiteral() { return getToken(RefactorMethodSignatureParser.CharacterLiteral, 0); }
		public LiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_literal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LiteralContext literal() throws RecognitionException {
		LiteralContext _localctx = new LiteralContext(_ctx, getState());
		enterRule(_localctx, 122, RULE_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(825);
			_la = _input.LA(1);
			if ( !(((((_la - 109)) & ~0x3f) == 0 && ((1L << (_la - 109)) & ((1L << (IntegerLiteral - 109)) | (1L << (FloatingPointLiteral - 109)) | (1L << (BooleanLiteral - 109)) | (1L << (CharacterLiteral - 109)) | (1L << (StringLiteral - 109)) | (1L << (NullLiteral - 109)))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			consume();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AnnotationContext extends ParserRuleContext {
		public ElementValuePairsContext elementValuePairs() {
			return getRuleContext(ElementValuePairsContext.class,0);
		}
		public AnnotationNameContext annotationName() {
			return getRuleContext(AnnotationNameContext.class,0);
		}
		public ElementValueContext elementValue() {
			return getRuleContext(ElementValueContext.class,0);
		}
		public AnnotationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotation; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterAnnotation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitAnnotation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitAnnotation(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnnotationContext annotation() throws RecognitionException {
		AnnotationContext _localctx = new AnnotationContext(_ctx, getState());
		enterRule(_localctx, 124, RULE_annotation);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(827); match(AT);
			setState(828); annotationName();
			setState(835);
			_la = _input.LA(1);
			if (_la==LPAREN) {
				{
				setState(829); match(LPAREN);
				setState(832);
				switch ( getInterpreter().adaptivePredict(_input,94,_ctx) ) {
				case 1:
					{
					setState(830); elementValuePairs();
					}
					break;

				case 2:
					{
					setState(831); elementValue();
					}
					break;
				}
				setState(834); match(RPAREN);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AnnotationNameContext extends ParserRuleContext {
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public AnnotationNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotationName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterAnnotationName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitAnnotationName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitAnnotationName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnnotationNameContext annotationName() throws RecognitionException {
		AnnotationNameContext _localctx = new AnnotationNameContext(_ctx, getState());
		enterRule(_localctx, 126, RULE_annotationName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(837); qualifiedName();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ElementValuePairsContext extends ParserRuleContext {
		public ElementValuePairContext elementValuePair(int i) {
			return getRuleContext(ElementValuePairContext.class,i);
		}
		public List<ElementValuePairContext> elementValuePair() {
			return getRuleContexts(ElementValuePairContext.class);
		}
		public ElementValuePairsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_elementValuePairs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterElementValuePairs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitElementValuePairs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitElementValuePairs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ElementValuePairsContext elementValuePairs() throws RecognitionException {
		ElementValuePairsContext _localctx = new ElementValuePairsContext(_ctx, getState());
		enterRule(_localctx, 128, RULE_elementValuePairs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(839); elementValuePair();
			setState(844);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(840); match(COMMA);
				setState(841); elementValuePair();
				}
				}
				setState(846);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ElementValuePairContext extends ParserRuleContext {
		public TerminalNode Identifier() { return getToken(RefactorMethodSignatureParser.Identifier, 0); }
		public ElementValueContext elementValue() {
			return getRuleContext(ElementValueContext.class,0);
		}
		public ElementValuePairContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_elementValuePair; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterElementValuePair(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitElementValuePair(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitElementValuePair(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ElementValuePairContext elementValuePair() throws RecognitionException {
		ElementValuePairContext _localctx = new ElementValuePairContext(_ctx, getState());
		enterRule(_localctx, 130, RULE_elementValuePair);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(847); match(Identifier);
			setState(848); match(ASSIGN);
			setState(849); elementValue();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ElementValueContext extends ParserRuleContext {
		public ElementValueArrayInitializerContext elementValueArrayInitializer() {
			return getRuleContext(ElementValueArrayInitializerContext.class,0);
		}
		public AnnotationContext annotation() {
			return getRuleContext(AnnotationContext.class,0);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public ElementValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_elementValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterElementValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitElementValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitElementValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ElementValueContext elementValue() throws RecognitionException {
		ElementValueContext _localctx = new ElementValueContext(_ctx, getState());
		enterRule(_localctx, 132, RULE_elementValue);
		try {
			setState(854);
			switch (_input.LA(1)) {
			case BOOLEAN:
			case BYTE:
			case CHAR:
			case DOUBLE:
			case FLOAT:
			case INT:
			case LONG:
			case NEW:
			case SHORT:
			case SUPER:
			case THIS:
			case VOID:
			case IntegerLiteral:
			case FloatingPointLiteral:
			case BooleanLiteral:
			case CharacterLiteral:
			case StringLiteral:
			case NullLiteral:
			case LPAREN:
			case LT:
			case BANG:
			case TILDE:
			case INC:
			case DEC:
			case ADD:
			case SUB:
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(851); expression(0);
				}
				break;
			case AT:
				enterOuterAlt(_localctx, 2);
				{
				setState(852); annotation();
				}
				break;
			case LBRACE:
				enterOuterAlt(_localctx, 3);
				{
				setState(853); elementValueArrayInitializer();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ElementValueArrayInitializerContext extends ParserRuleContext {
		public ElementValueContext elementValue(int i) {
			return getRuleContext(ElementValueContext.class,i);
		}
		public List<ElementValueContext> elementValue() {
			return getRuleContexts(ElementValueContext.class);
		}
		public ElementValueArrayInitializerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_elementValueArrayInitializer; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterElementValueArrayInitializer(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitElementValueArrayInitializer(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitElementValueArrayInitializer(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ElementValueArrayInitializerContext elementValueArrayInitializer() throws RecognitionException {
		ElementValueArrayInitializerContext _localctx = new ElementValueArrayInitializerContext(_ctx, getState());
		enterRule(_localctx, 134, RULE_elementValueArrayInitializer);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(856); match(LBRACE);
			setState(865);
			_la = _input.LA(1);
			if (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (BOOLEAN - 58)) | (1L << (BYTE - 58)) | (1L << (CHAR - 58)) | (1L << (DOUBLE - 58)) | (1L << (FLOAT - 58)) | (1L << (INT - 58)) | (1L << (LONG - 58)) | (1L << (NEW - 58)) | (1L << (SHORT - 58)) | (1L << (SUPER - 58)) | (1L << (THIS - 58)) | (1L << (VOID - 58)) | (1L << (IntegerLiteral - 58)) | (1L << (FloatingPointLiteral - 58)) | (1L << (BooleanLiteral - 58)) | (1L << (CharacterLiteral - 58)) | (1L << (StringLiteral - 58)) | (1L << (NullLiteral - 58)) | (1L << (LPAREN - 58)) | (1L << (LBRACE - 58)))) != 0) || ((((_la - 126)) & ~0x3f) == 0 && ((1L << (_la - 126)) & ((1L << (LT - 126)) | (1L << (BANG - 126)) | (1L << (TILDE - 126)) | (1L << (INC - 126)) | (1L << (DEC - 126)) | (1L << (ADD - 126)) | (1L << (SUB - 126)) | (1L << (Identifier - 126)))) != 0)) {
				{
				setState(857); elementValue();
				setState(862);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,98,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(858); match(COMMA);
						setState(859); elementValue();
						}
						} 
					}
					setState(864);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,98,_ctx);
				}
				}
			}

			setState(868);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(867); match(COMMA);
				}
			}

			setState(870); match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AnnotationTypeDeclarationContext extends ParserRuleContext {
		public TerminalNode Identifier() { return getToken(RefactorMethodSignatureParser.Identifier, 0); }
		public AnnotationTypeBodyContext annotationTypeBody() {
			return getRuleContext(AnnotationTypeBodyContext.class,0);
		}
		public AnnotationTypeDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotationTypeDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterAnnotationTypeDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitAnnotationTypeDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitAnnotationTypeDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnnotationTypeDeclarationContext annotationTypeDeclaration() throws RecognitionException {
		AnnotationTypeDeclarationContext _localctx = new AnnotationTypeDeclarationContext(_ctx, getState());
		enterRule(_localctx, 136, RULE_annotationTypeDeclaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(872); match(AT);
			setState(873); match(INTERFACE);
			setState(874); match(Identifier);
			setState(875); annotationTypeBody();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AnnotationTypeBodyContext extends ParserRuleContext {
		public List<AnnotationTypeElementDeclarationContext> annotationTypeElementDeclaration() {
			return getRuleContexts(AnnotationTypeElementDeclarationContext.class);
		}
		public AnnotationTypeElementDeclarationContext annotationTypeElementDeclaration(int i) {
			return getRuleContext(AnnotationTypeElementDeclarationContext.class,i);
		}
		public AnnotationTypeBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotationTypeBody; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterAnnotationTypeBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitAnnotationTypeBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitAnnotationTypeBody(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnnotationTypeBodyContext annotationTypeBody() throws RecognitionException {
		AnnotationTypeBodyContext _localctx = new AnnotationTypeBodyContext(_ctx, getState());
		enterRule(_localctx, 138, RULE_annotationTypeBody);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(877); match(LBRACE);
			setState(881);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (ABSTRACT - 58)) | (1L << (BOOLEAN - 58)) | (1L << (BYTE - 58)) | (1L << (CHAR - 58)) | (1L << (CLASS - 58)) | (1L << (DOUBLE - 58)) | (1L << (ENUM - 58)) | (1L << (FINAL - 58)) | (1L << (FLOAT - 58)) | (1L << (INT - 58)) | (1L << (INTERFACE - 58)) | (1L << (LONG - 58)) | (1L << (NATIVE - 58)) | (1L << (PRIVATE - 58)) | (1L << (PROTECTED - 58)) | (1L << (PUBLIC - 58)) | (1L << (SHORT - 58)) | (1L << (STATIC - 58)) | (1L << (STRICTFP - 58)) | (1L << (SYNCHRONIZED - 58)) | (1L << (TRANSIENT - 58)) | (1L << (VOLATILE - 58)) | (1L << (SEMI - 58)))) != 0) || _la==Identifier) {
				{
				{
				setState(878); annotationTypeElementDeclaration();
				}
				}
				setState(883);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(884); match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AnnotationTypeElementDeclarationContext extends ParserRuleContext {
		public List<ModifierContext> modifier() {
			return getRuleContexts(ModifierContext.class);
		}
		public AnnotationTypeElementRestContext annotationTypeElementRest() {
			return getRuleContext(AnnotationTypeElementRestContext.class,0);
		}
		public ModifierContext modifier(int i) {
			return getRuleContext(ModifierContext.class,i);
		}
		public AnnotationTypeElementDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotationTypeElementDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterAnnotationTypeElementDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitAnnotationTypeElementDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitAnnotationTypeElementDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnnotationTypeElementDeclarationContext annotationTypeElementDeclaration() throws RecognitionException {
		AnnotationTypeElementDeclarationContext _localctx = new AnnotationTypeElementDeclarationContext(_ctx, getState());
		enterRule(_localctx, 140, RULE_annotationTypeElementDeclaration);
		try {
			int _alt;
			setState(894);
			switch (_input.LA(1)) {
			case AT:
			case ABSTRACT:
			case BOOLEAN:
			case BYTE:
			case CHAR:
			case CLASS:
			case DOUBLE:
			case ENUM:
			case FINAL:
			case FLOAT:
			case INT:
			case INTERFACE:
			case LONG:
			case NATIVE:
			case PRIVATE:
			case PROTECTED:
			case PUBLIC:
			case SHORT:
			case STATIC:
			case STRICTFP:
			case SYNCHRONIZED:
			case TRANSIENT:
			case VOLATILE:
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(889);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,102,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(886); modifier();
						}
						} 
					}
					setState(891);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,102,_ctx);
				}
				setState(892); annotationTypeElementRest();
				}
				break;
			case SEMI:
				enterOuterAlt(_localctx, 2);
				{
				setState(893); match(SEMI);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AnnotationTypeElementRestContext extends ParserRuleContext {
		public EnumDeclarationContext enumDeclaration() {
			return getRuleContext(EnumDeclarationContext.class,0);
		}
		public ClassDeclarationContext classDeclaration() {
			return getRuleContext(ClassDeclarationContext.class,0);
		}
		public AnnotationMethodOrConstantRestContext annotationMethodOrConstantRest() {
			return getRuleContext(AnnotationMethodOrConstantRestContext.class,0);
		}
		public AnnotationTypeDeclarationContext annotationTypeDeclaration() {
			return getRuleContext(AnnotationTypeDeclarationContext.class,0);
		}
		public InterfaceDeclarationContext interfaceDeclaration() {
			return getRuleContext(InterfaceDeclarationContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public AnnotationTypeElementRestContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotationTypeElementRest; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterAnnotationTypeElementRest(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitAnnotationTypeElementRest(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitAnnotationTypeElementRest(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnnotationTypeElementRestContext annotationTypeElementRest() throws RecognitionException {
		AnnotationTypeElementRestContext _localctx = new AnnotationTypeElementRestContext(_ctx, getState());
		enterRule(_localctx, 142, RULE_annotationTypeElementRest);
		try {
			setState(916);
			switch (_input.LA(1)) {
			case BOOLEAN:
			case BYTE:
			case CHAR:
			case DOUBLE:
			case FLOAT:
			case INT:
			case LONG:
			case SHORT:
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(896); type();
				setState(897); annotationMethodOrConstantRest();
				setState(898); match(SEMI);
				}
				break;
			case CLASS:
				enterOuterAlt(_localctx, 2);
				{
				setState(900); classDeclaration();
				setState(902);
				switch ( getInterpreter().adaptivePredict(_input,104,_ctx) ) {
				case 1:
					{
					setState(901); match(SEMI);
					}
					break;
				}
				}
				break;
			case INTERFACE:
				enterOuterAlt(_localctx, 3);
				{
				setState(904); interfaceDeclaration();
				setState(906);
				switch ( getInterpreter().adaptivePredict(_input,105,_ctx) ) {
				case 1:
					{
					setState(905); match(SEMI);
					}
					break;
				}
				}
				break;
			case ENUM:
				enterOuterAlt(_localctx, 4);
				{
				setState(908); enumDeclaration();
				setState(910);
				switch ( getInterpreter().adaptivePredict(_input,106,_ctx) ) {
				case 1:
					{
					setState(909); match(SEMI);
					}
					break;
				}
				}
				break;
			case AT:
				enterOuterAlt(_localctx, 5);
				{
				setState(912); annotationTypeDeclaration();
				setState(914);
				switch ( getInterpreter().adaptivePredict(_input,107,_ctx) ) {
				case 1:
					{
					setState(913); match(SEMI);
					}
					break;
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AnnotationMethodOrConstantRestContext extends ParserRuleContext {
		public AnnotationMethodRestContext annotationMethodRest() {
			return getRuleContext(AnnotationMethodRestContext.class,0);
		}
		public AnnotationConstantRestContext annotationConstantRest() {
			return getRuleContext(AnnotationConstantRestContext.class,0);
		}
		public AnnotationMethodOrConstantRestContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotationMethodOrConstantRest; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterAnnotationMethodOrConstantRest(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitAnnotationMethodOrConstantRest(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitAnnotationMethodOrConstantRest(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnnotationMethodOrConstantRestContext annotationMethodOrConstantRest() throws RecognitionException {
		AnnotationMethodOrConstantRestContext _localctx = new AnnotationMethodOrConstantRestContext(_ctx, getState());
		enterRule(_localctx, 144, RULE_annotationMethodOrConstantRest);
		try {
			setState(920);
			switch ( getInterpreter().adaptivePredict(_input,109,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(918); annotationMethodRest();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(919); annotationConstantRest();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AnnotationMethodRestContext extends ParserRuleContext {
		public TerminalNode Identifier() { return getToken(RefactorMethodSignatureParser.Identifier, 0); }
		public DefaultValueContext defaultValue() {
			return getRuleContext(DefaultValueContext.class,0);
		}
		public AnnotationMethodRestContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotationMethodRest; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterAnnotationMethodRest(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitAnnotationMethodRest(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitAnnotationMethodRest(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnnotationMethodRestContext annotationMethodRest() throws RecognitionException {
		AnnotationMethodRestContext _localctx = new AnnotationMethodRestContext(_ctx, getState());
		enterRule(_localctx, 146, RULE_annotationMethodRest);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(922); match(Identifier);
			setState(923); match(LPAREN);
			setState(924); match(RPAREN);
			setState(926);
			_la = _input.LA(1);
			if (_la==DEFAULT) {
				{
				setState(925); defaultValue();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class AnnotationConstantRestContext extends ParserRuleContext {
		public VariableDeclaratorsContext variableDeclarators() {
			return getRuleContext(VariableDeclaratorsContext.class,0);
		}
		public AnnotationConstantRestContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotationConstantRest; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterAnnotationConstantRest(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitAnnotationConstantRest(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitAnnotationConstantRest(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnnotationConstantRestContext annotationConstantRest() throws RecognitionException {
		AnnotationConstantRestContext _localctx = new AnnotationConstantRestContext(_ctx, getState());
		enterRule(_localctx, 148, RULE_annotationConstantRest);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(928); variableDeclarators();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DefaultValueContext extends ParserRuleContext {
		public ElementValueContext elementValue() {
			return getRuleContext(ElementValueContext.class,0);
		}
		public DefaultValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_defaultValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterDefaultValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitDefaultValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitDefaultValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DefaultValueContext defaultValue() throws RecognitionException {
		DefaultValueContext _localctx = new DefaultValueContext(_ctx, getState());
		enterRule(_localctx, 150, RULE_defaultValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(930); match(DEFAULT);
			setState(931); elementValue();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class BlockContext extends ParserRuleContext {
		public List<BlockStatementContext> blockStatement() {
			return getRuleContexts(BlockStatementContext.class);
		}
		public BlockStatementContext blockStatement(int i) {
			return getRuleContext(BlockStatementContext.class,i);
		}
		public BlockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_block; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterBlock(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitBlock(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitBlock(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BlockContext block() throws RecognitionException {
		BlockContext _localctx = new BlockContext(_ctx, getState());
		enterRule(_localctx, 152, RULE_block);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(933); match(LBRACE);
			setState(937);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (ABSTRACT - 58)) | (1L << (ASSERT - 58)) | (1L << (BOOLEAN - 58)) | (1L << (BREAK - 58)) | (1L << (BYTE - 58)) | (1L << (CHAR - 58)) | (1L << (CLASS - 58)) | (1L << (CONTINUE - 58)) | (1L << (DO - 58)) | (1L << (DOUBLE - 58)) | (1L << (ENUM - 58)) | (1L << (FINAL - 58)) | (1L << (FLOAT - 58)) | (1L << (FOR - 58)) | (1L << (IF - 58)) | (1L << (INT - 58)) | (1L << (INTERFACE - 58)) | (1L << (LONG - 58)) | (1L << (NEW - 58)) | (1L << (PRIVATE - 58)) | (1L << (PROTECTED - 58)) | (1L << (PUBLIC - 58)) | (1L << (RETURN - 58)) | (1L << (SHORT - 58)) | (1L << (STATIC - 58)) | (1L << (STRICTFP - 58)) | (1L << (SUPER - 58)) | (1L << (SWITCH - 58)) | (1L << (SYNCHRONIZED - 58)) | (1L << (THIS - 58)) | (1L << (THROW - 58)) | (1L << (TRY - 58)) | (1L << (VOID - 58)) | (1L << (WHILE - 58)) | (1L << (IntegerLiteral - 58)) | (1L << (FloatingPointLiteral - 58)) | (1L << (BooleanLiteral - 58)) | (1L << (CharacterLiteral - 58)) | (1L << (StringLiteral - 58)) | (1L << (NullLiteral - 58)) | (1L << (LPAREN - 58)) | (1L << (LBRACE - 58)) | (1L << (SEMI - 58)))) != 0) || ((((_la - 126)) & ~0x3f) == 0 && ((1L << (_la - 126)) & ((1L << (LT - 126)) | (1L << (BANG - 126)) | (1L << (TILDE - 126)) | (1L << (INC - 126)) | (1L << (DEC - 126)) | (1L << (ADD - 126)) | (1L << (SUB - 126)) | (1L << (Identifier - 126)))) != 0)) {
				{
				{
				setState(934); blockStatement();
				}
				}
				setState(939);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(940); match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class BlockStatementContext extends ParserRuleContext {
		public TypeDeclarationContext typeDeclaration() {
			return getRuleContext(TypeDeclarationContext.class,0);
		}
		public StatementContext statement() {
			return getRuleContext(StatementContext.class,0);
		}
		public LocalVariableDeclarationStatementContext localVariableDeclarationStatement() {
			return getRuleContext(LocalVariableDeclarationStatementContext.class,0);
		}
		public BlockStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_blockStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterBlockStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitBlockStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitBlockStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BlockStatementContext blockStatement() throws RecognitionException {
		BlockStatementContext _localctx = new BlockStatementContext(_ctx, getState());
		enterRule(_localctx, 154, RULE_blockStatement);
		try {
			setState(945);
			switch ( getInterpreter().adaptivePredict(_input,112,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(942); localVariableDeclarationStatement();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(943); statement();
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(944); typeDeclaration();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class LocalVariableDeclarationStatementContext extends ParserRuleContext {
		public LocalVariableDeclarationContext localVariableDeclaration() {
			return getRuleContext(LocalVariableDeclarationContext.class,0);
		}
		public LocalVariableDeclarationStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_localVariableDeclarationStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterLocalVariableDeclarationStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitLocalVariableDeclarationStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitLocalVariableDeclarationStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LocalVariableDeclarationStatementContext localVariableDeclarationStatement() throws RecognitionException {
		LocalVariableDeclarationStatementContext _localctx = new LocalVariableDeclarationStatementContext(_ctx, getState());
		enterRule(_localctx, 156, RULE_localVariableDeclarationStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(947); localVariableDeclaration();
			setState(948); match(SEMI);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class LocalVariableDeclarationContext extends ParserRuleContext {
		public VariableModifierContext variableModifier(int i) {
			return getRuleContext(VariableModifierContext.class,i);
		}
		public List<VariableModifierContext> variableModifier() {
			return getRuleContexts(VariableModifierContext.class);
		}
		public VariableDeclaratorsContext variableDeclarators() {
			return getRuleContext(VariableDeclaratorsContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public LocalVariableDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_localVariableDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterLocalVariableDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitLocalVariableDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitLocalVariableDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LocalVariableDeclarationContext localVariableDeclaration() throws RecognitionException {
		LocalVariableDeclarationContext _localctx = new LocalVariableDeclarationContext(_ctx, getState());
		enterRule(_localctx, 158, RULE_localVariableDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(953);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==FINAL) {
				{
				{
				setState(950); variableModifier();
				}
				}
				setState(955);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(956); type();
			setState(957); variableDeclarators();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class StatementContext extends ParserRuleContext {
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public StatementExpressionContext statementExpression() {
			return getRuleContext(StatementExpressionContext.class,0);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public List<SwitchLabelContext> switchLabel() {
			return getRuleContexts(SwitchLabelContext.class);
		}
		public List<SwitchBlockStatementGroupContext> switchBlockStatementGroup() {
			return getRuleContexts(SwitchBlockStatementGroupContext.class);
		}
		public ParExpressionContext parExpression() {
			return getRuleContext(ParExpressionContext.class,0);
		}
		public List<CatchClauseContext> catchClause() {
			return getRuleContexts(CatchClauseContext.class);
		}
		public CatchClauseContext catchClause(int i) {
			return getRuleContext(CatchClauseContext.class,i);
		}
		public TerminalNode Identifier() { return getToken(RefactorMethodSignatureParser.Identifier, 0); }
		public FinallyBlockContext finallyBlock() {
			return getRuleContext(FinallyBlockContext.class,0);
		}
		public SwitchBlockStatementGroupContext switchBlockStatementGroup(int i) {
			return getRuleContext(SwitchBlockStatementGroupContext.class,i);
		}
		public ForControlContext forControl() {
			return getRuleContext(ForControlContext.class,0);
		}
		public TerminalNode ASSERT() { return getToken(RefactorMethodSignatureParser.ASSERT, 0); }
		public ResourceSpecificationContext resourceSpecification() {
			return getRuleContext(ResourceSpecificationContext.class,0);
		}
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public SwitchLabelContext switchLabel(int i) {
			return getRuleContext(SwitchLabelContext.class,i);
		}
		public StatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StatementContext statement() throws RecognitionException {
		StatementContext _localctx = new StatementContext(_ctx, getState());
		enterRule(_localctx, 160, RULE_statement);
		int _la;
		try {
			int _alt;
			setState(1063);
			switch ( getInterpreter().adaptivePredict(_input,126,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(959); block();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(960); match(ASSERT);
				setState(961); expression(0);
				setState(964);
				_la = _input.LA(1);
				if (_la==COLON) {
					{
					setState(962); match(COLON);
					setState(963); expression(0);
					}
				}

				setState(966); match(SEMI);
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(968); match(IF);
				setState(969); parExpression();
				setState(970); statement();
				setState(973);
				switch ( getInterpreter().adaptivePredict(_input,115,_ctx) ) {
				case 1:
					{
					setState(971); match(ELSE);
					setState(972); statement();
					}
					break;
				}
				}
				break;

			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(975); match(FOR);
				setState(976); match(LPAREN);
				setState(977); forControl();
				setState(978); match(RPAREN);
				setState(979); statement();
				}
				break;

			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(981); match(WHILE);
				setState(982); parExpression();
				setState(983); statement();
				}
				break;

			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(985); match(DO);
				setState(986); statement();
				setState(987); match(WHILE);
				setState(988); parExpression();
				setState(989); match(SEMI);
				}
				break;

			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(991); match(TRY);
				setState(992); block();
				setState(1002);
				switch (_input.LA(1)) {
				case CATCH:
					{
					setState(994); 
					_errHandler.sync(this);
					_la = _input.LA(1);
					do {
						{
						{
						setState(993); catchClause();
						}
						}
						setState(996); 
						_errHandler.sync(this);
						_la = _input.LA(1);
					} while ( _la==CATCH );
					setState(999);
					_la = _input.LA(1);
					if (_la==FINALLY) {
						{
						setState(998); finallyBlock();
						}
					}

					}
					break;
				case FINALLY:
					{
					setState(1001); finallyBlock();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				break;

			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(1004); match(TRY);
				setState(1005); resourceSpecification();
				setState(1006); block();
				setState(1010);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==CATCH) {
					{
					{
					setState(1007); catchClause();
					}
					}
					setState(1012);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(1014);
				_la = _input.LA(1);
				if (_la==FINALLY) {
					{
					setState(1013); finallyBlock();
					}
				}

				}
				break;

			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(1016); match(SWITCH);
				setState(1017); parExpression();
				setState(1018); match(LBRACE);
				setState(1022);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,121,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1019); switchBlockStatementGroup();
						}
						} 
					}
					setState(1024);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,121,_ctx);
				}
				setState(1028);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==CASE || _la==DEFAULT) {
					{
					{
					setState(1025); switchLabel();
					}
					}
					setState(1030);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(1031); match(RBRACE);
				}
				break;

			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(1033); match(SYNCHRONIZED);
				setState(1034); parExpression();
				setState(1035); block();
				}
				break;

			case 11:
				enterOuterAlt(_localctx, 11);
				{
				setState(1037); match(RETURN);
				setState(1039);
				_la = _input.LA(1);
				if (((((_la - 61)) & ~0x3f) == 0 && ((1L << (_la - 61)) & ((1L << (BOOLEAN - 61)) | (1L << (BYTE - 61)) | (1L << (CHAR - 61)) | (1L << (DOUBLE - 61)) | (1L << (FLOAT - 61)) | (1L << (INT - 61)) | (1L << (LONG - 61)) | (1L << (NEW - 61)) | (1L << (SHORT - 61)) | (1L << (SUPER - 61)) | (1L << (THIS - 61)) | (1L << (VOID - 61)) | (1L << (IntegerLiteral - 61)) | (1L << (FloatingPointLiteral - 61)) | (1L << (BooleanLiteral - 61)) | (1L << (CharacterLiteral - 61)) | (1L << (StringLiteral - 61)) | (1L << (NullLiteral - 61)) | (1L << (LPAREN - 61)))) != 0) || ((((_la - 126)) & ~0x3f) == 0 && ((1L << (_la - 126)) & ((1L << (LT - 126)) | (1L << (BANG - 126)) | (1L << (TILDE - 126)) | (1L << (INC - 126)) | (1L << (DEC - 126)) | (1L << (ADD - 126)) | (1L << (SUB - 126)) | (1L << (Identifier - 126)))) != 0)) {
					{
					setState(1038); expression(0);
					}
				}

				setState(1041); match(SEMI);
				}
				break;

			case 12:
				enterOuterAlt(_localctx, 12);
				{
				setState(1042); match(THROW);
				setState(1043); expression(0);
				setState(1044); match(SEMI);
				}
				break;

			case 13:
				enterOuterAlt(_localctx, 13);
				{
				setState(1046); match(BREAK);
				setState(1048);
				_la = _input.LA(1);
				if (_la==Identifier) {
					{
					setState(1047); match(Identifier);
					}
				}

				setState(1050); match(SEMI);
				}
				break;

			case 14:
				enterOuterAlt(_localctx, 14);
				{
				setState(1051); match(CONTINUE);
				setState(1053);
				_la = _input.LA(1);
				if (_la==Identifier) {
					{
					setState(1052); match(Identifier);
					}
				}

				setState(1055); match(SEMI);
				}
				break;

			case 15:
				enterOuterAlt(_localctx, 15);
				{
				setState(1056); match(SEMI);
				}
				break;

			case 16:
				enterOuterAlt(_localctx, 16);
				{
				setState(1057); statementExpression();
				setState(1058); match(SEMI);
				}
				break;

			case 17:
				enterOuterAlt(_localctx, 17);
				{
				setState(1060); match(Identifier);
				setState(1061); match(COLON);
				setState(1062); statement();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CatchClauseContext extends ParserRuleContext {
		public CatchTypeContext catchType() {
			return getRuleContext(CatchTypeContext.class,0);
		}
		public TerminalNode Identifier() { return getToken(RefactorMethodSignatureParser.Identifier, 0); }
		public VariableModifierContext variableModifier(int i) {
			return getRuleContext(VariableModifierContext.class,i);
		}
		public List<VariableModifierContext> variableModifier() {
			return getRuleContexts(VariableModifierContext.class);
		}
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public CatchClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_catchClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterCatchClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitCatchClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitCatchClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CatchClauseContext catchClause() throws RecognitionException {
		CatchClauseContext _localctx = new CatchClauseContext(_ctx, getState());
		enterRule(_localctx, 162, RULE_catchClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1065); match(CATCH);
			setState(1066); match(LPAREN);
			setState(1070);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==FINAL) {
				{
				{
				setState(1067); variableModifier();
				}
				}
				setState(1072);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1073); catchType();
			setState(1074); match(Identifier);
			setState(1075); match(RPAREN);
			setState(1076); block();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CatchTypeContext extends ParserRuleContext {
		public List<QualifiedNameContext> qualifiedName() {
			return getRuleContexts(QualifiedNameContext.class);
		}
		public QualifiedNameContext qualifiedName(int i) {
			return getRuleContext(QualifiedNameContext.class,i);
		}
		public CatchTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_catchType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterCatchType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitCatchType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitCatchType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CatchTypeContext catchType() throws RecognitionException {
		CatchTypeContext _localctx = new CatchTypeContext(_ctx, getState());
		enterRule(_localctx, 164, RULE_catchType);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1078); qualifiedName();
			setState(1083);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==BITOR) {
				{
				{
				setState(1079); match(BITOR);
				setState(1080); qualifiedName();
				}
				}
				setState(1085);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FinallyBlockContext extends ParserRuleContext {
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public FinallyBlockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_finallyBlock; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterFinallyBlock(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitFinallyBlock(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitFinallyBlock(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FinallyBlockContext finallyBlock() throws RecognitionException {
		FinallyBlockContext _localctx = new FinallyBlockContext(_ctx, getState());
		enterRule(_localctx, 166, RULE_finallyBlock);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1086); match(FINALLY);
			setState(1087); block();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ResourceSpecificationContext extends ParserRuleContext {
		public ResourcesContext resources() {
			return getRuleContext(ResourcesContext.class,0);
		}
		public ResourceSpecificationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_resourceSpecification; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterResourceSpecification(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitResourceSpecification(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitResourceSpecification(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ResourceSpecificationContext resourceSpecification() throws RecognitionException {
		ResourceSpecificationContext _localctx = new ResourceSpecificationContext(_ctx, getState());
		enterRule(_localctx, 168, RULE_resourceSpecification);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1089); match(LPAREN);
			setState(1090); resources();
			setState(1092);
			_la = _input.LA(1);
			if (_la==SEMI) {
				{
				setState(1091); match(SEMI);
				}
			}

			setState(1094); match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ResourcesContext extends ParserRuleContext {
		public ResourceContext resource(int i) {
			return getRuleContext(ResourceContext.class,i);
		}
		public List<ResourceContext> resource() {
			return getRuleContexts(ResourceContext.class);
		}
		public ResourcesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_resources; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterResources(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitResources(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitResources(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ResourcesContext resources() throws RecognitionException {
		ResourcesContext _localctx = new ResourcesContext(_ctx, getState());
		enterRule(_localctx, 170, RULE_resources);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1096); resource();
			setState(1101);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,130,_ctx);
			while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1097); match(SEMI);
					setState(1098); resource();
					}
					} 
				}
				setState(1103);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,130,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ResourceContext extends ParserRuleContext {
		public VariableModifierContext variableModifier(int i) {
			return getRuleContext(VariableModifierContext.class,i);
		}
		public List<VariableModifierContext> variableModifier() {
			return getRuleContexts(VariableModifierContext.class);
		}
		public ClassOrInterfaceTypeContext classOrInterfaceType() {
			return getRuleContext(ClassOrInterfaceTypeContext.class,0);
		}
		public VariableDeclaratorIdContext variableDeclaratorId() {
			return getRuleContext(VariableDeclaratorIdContext.class,0);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public ResourceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_resource; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterResource(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitResource(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitResource(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ResourceContext resource() throws RecognitionException {
		ResourceContext _localctx = new ResourceContext(_ctx, getState());
		enterRule(_localctx, 172, RULE_resource);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1107);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==FINAL) {
				{
				{
				setState(1104); variableModifier();
				}
				}
				setState(1109);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1110); classOrInterfaceType();
			setState(1111); variableDeclaratorId();
			setState(1112); match(ASSIGN);
			setState(1113); expression(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SwitchBlockStatementGroupContext extends ParserRuleContext {
		public List<BlockStatementContext> blockStatement() {
			return getRuleContexts(BlockStatementContext.class);
		}
		public List<SwitchLabelContext> switchLabel() {
			return getRuleContexts(SwitchLabelContext.class);
		}
		public BlockStatementContext blockStatement(int i) {
			return getRuleContext(BlockStatementContext.class,i);
		}
		public SwitchLabelContext switchLabel(int i) {
			return getRuleContext(SwitchLabelContext.class,i);
		}
		public SwitchBlockStatementGroupContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_switchBlockStatementGroup; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterSwitchBlockStatementGroup(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitSwitchBlockStatementGroup(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitSwitchBlockStatementGroup(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SwitchBlockStatementGroupContext switchBlockStatementGroup() throws RecognitionException {
		SwitchBlockStatementGroupContext _localctx = new SwitchBlockStatementGroupContext(_ctx, getState());
		enterRule(_localctx, 174, RULE_switchBlockStatementGroup);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1116); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(1115); switchLabel();
				}
				}
				setState(1118); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==CASE || _la==DEFAULT );
			setState(1121); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(1120); blockStatement();
				}
				}
				setState(1123); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( ((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (ABSTRACT - 58)) | (1L << (ASSERT - 58)) | (1L << (BOOLEAN - 58)) | (1L << (BREAK - 58)) | (1L << (BYTE - 58)) | (1L << (CHAR - 58)) | (1L << (CLASS - 58)) | (1L << (CONTINUE - 58)) | (1L << (DO - 58)) | (1L << (DOUBLE - 58)) | (1L << (ENUM - 58)) | (1L << (FINAL - 58)) | (1L << (FLOAT - 58)) | (1L << (FOR - 58)) | (1L << (IF - 58)) | (1L << (INT - 58)) | (1L << (INTERFACE - 58)) | (1L << (LONG - 58)) | (1L << (NEW - 58)) | (1L << (PRIVATE - 58)) | (1L << (PROTECTED - 58)) | (1L << (PUBLIC - 58)) | (1L << (RETURN - 58)) | (1L << (SHORT - 58)) | (1L << (STATIC - 58)) | (1L << (STRICTFP - 58)) | (1L << (SUPER - 58)) | (1L << (SWITCH - 58)) | (1L << (SYNCHRONIZED - 58)) | (1L << (THIS - 58)) | (1L << (THROW - 58)) | (1L << (TRY - 58)) | (1L << (VOID - 58)) | (1L << (WHILE - 58)) | (1L << (IntegerLiteral - 58)) | (1L << (FloatingPointLiteral - 58)) | (1L << (BooleanLiteral - 58)) | (1L << (CharacterLiteral - 58)) | (1L << (StringLiteral - 58)) | (1L << (NullLiteral - 58)) | (1L << (LPAREN - 58)) | (1L << (LBRACE - 58)) | (1L << (SEMI - 58)))) != 0) || ((((_la - 126)) & ~0x3f) == 0 && ((1L << (_la - 126)) & ((1L << (LT - 126)) | (1L << (BANG - 126)) | (1L << (TILDE - 126)) | (1L << (INC - 126)) | (1L << (DEC - 126)) | (1L << (ADD - 126)) | (1L << (SUB - 126)) | (1L << (Identifier - 126)))) != 0) );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SwitchLabelContext extends ParserRuleContext {
		public ConstantExpressionContext constantExpression() {
			return getRuleContext(ConstantExpressionContext.class,0);
		}
		public EnumConstantNameContext enumConstantName() {
			return getRuleContext(EnumConstantNameContext.class,0);
		}
		public SwitchLabelContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_switchLabel; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterSwitchLabel(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitSwitchLabel(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitSwitchLabel(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SwitchLabelContext switchLabel() throws RecognitionException {
		SwitchLabelContext _localctx = new SwitchLabelContext(_ctx, getState());
		enterRule(_localctx, 176, RULE_switchLabel);
		try {
			setState(1135);
			switch ( getInterpreter().adaptivePredict(_input,134,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1125); match(CASE);
				setState(1126); constantExpression();
				setState(1127); match(COLON);
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1129); match(CASE);
				setState(1130); enumConstantName();
				setState(1131); match(COLON);
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1133); match(DEFAULT);
				setState(1134); match(COLON);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ForControlContext extends ParserRuleContext {
		public ForUpdateContext forUpdate() {
			return getRuleContext(ForUpdateContext.class,0);
		}
		public ForInitContext forInit() {
			return getRuleContext(ForInitContext.class,0);
		}
		public EnhancedForControlContext enhancedForControl() {
			return getRuleContext(EnhancedForControlContext.class,0);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public ForControlContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_forControl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterForControl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitForControl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitForControl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ForControlContext forControl() throws RecognitionException {
		ForControlContext _localctx = new ForControlContext(_ctx, getState());
		enterRule(_localctx, 178, RULE_forControl);
		int _la;
		try {
			setState(1149);
			switch ( getInterpreter().adaptivePredict(_input,138,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1137); enhancedForControl();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1139);
				_la = _input.LA(1);
				if (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (BOOLEAN - 58)) | (1L << (BYTE - 58)) | (1L << (CHAR - 58)) | (1L << (DOUBLE - 58)) | (1L << (FINAL - 58)) | (1L << (FLOAT - 58)) | (1L << (INT - 58)) | (1L << (LONG - 58)) | (1L << (NEW - 58)) | (1L << (SHORT - 58)) | (1L << (SUPER - 58)) | (1L << (THIS - 58)) | (1L << (VOID - 58)) | (1L << (IntegerLiteral - 58)) | (1L << (FloatingPointLiteral - 58)) | (1L << (BooleanLiteral - 58)) | (1L << (CharacterLiteral - 58)) | (1L << (StringLiteral - 58)) | (1L << (NullLiteral - 58)) | (1L << (LPAREN - 58)))) != 0) || ((((_la - 126)) & ~0x3f) == 0 && ((1L << (_la - 126)) & ((1L << (LT - 126)) | (1L << (BANG - 126)) | (1L << (TILDE - 126)) | (1L << (INC - 126)) | (1L << (DEC - 126)) | (1L << (ADD - 126)) | (1L << (SUB - 126)) | (1L << (Identifier - 126)))) != 0)) {
					{
					setState(1138); forInit();
					}
				}

				setState(1141); match(SEMI);
				setState(1143);
				_la = _input.LA(1);
				if (((((_la - 61)) & ~0x3f) == 0 && ((1L << (_la - 61)) & ((1L << (BOOLEAN - 61)) | (1L << (BYTE - 61)) | (1L << (CHAR - 61)) | (1L << (DOUBLE - 61)) | (1L << (FLOAT - 61)) | (1L << (INT - 61)) | (1L << (LONG - 61)) | (1L << (NEW - 61)) | (1L << (SHORT - 61)) | (1L << (SUPER - 61)) | (1L << (THIS - 61)) | (1L << (VOID - 61)) | (1L << (IntegerLiteral - 61)) | (1L << (FloatingPointLiteral - 61)) | (1L << (BooleanLiteral - 61)) | (1L << (CharacterLiteral - 61)) | (1L << (StringLiteral - 61)) | (1L << (NullLiteral - 61)) | (1L << (LPAREN - 61)))) != 0) || ((((_la - 126)) & ~0x3f) == 0 && ((1L << (_la - 126)) & ((1L << (LT - 126)) | (1L << (BANG - 126)) | (1L << (TILDE - 126)) | (1L << (INC - 126)) | (1L << (DEC - 126)) | (1L << (ADD - 126)) | (1L << (SUB - 126)) | (1L << (Identifier - 126)))) != 0)) {
					{
					setState(1142); expression(0);
					}
				}

				setState(1145); match(SEMI);
				setState(1147);
				_la = _input.LA(1);
				if (((((_la - 61)) & ~0x3f) == 0 && ((1L << (_la - 61)) & ((1L << (BOOLEAN - 61)) | (1L << (BYTE - 61)) | (1L << (CHAR - 61)) | (1L << (DOUBLE - 61)) | (1L << (FLOAT - 61)) | (1L << (INT - 61)) | (1L << (LONG - 61)) | (1L << (NEW - 61)) | (1L << (SHORT - 61)) | (1L << (SUPER - 61)) | (1L << (THIS - 61)) | (1L << (VOID - 61)) | (1L << (IntegerLiteral - 61)) | (1L << (FloatingPointLiteral - 61)) | (1L << (BooleanLiteral - 61)) | (1L << (CharacterLiteral - 61)) | (1L << (StringLiteral - 61)) | (1L << (NullLiteral - 61)) | (1L << (LPAREN - 61)))) != 0) || ((((_la - 126)) & ~0x3f) == 0 && ((1L << (_la - 126)) & ((1L << (LT - 126)) | (1L << (BANG - 126)) | (1L << (TILDE - 126)) | (1L << (INC - 126)) | (1L << (DEC - 126)) | (1L << (ADD - 126)) | (1L << (SUB - 126)) | (1L << (Identifier - 126)))) != 0)) {
					{
					setState(1146); forUpdate();
					}
				}

				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ForInitContext extends ParserRuleContext {
		public LocalVariableDeclarationContext localVariableDeclaration() {
			return getRuleContext(LocalVariableDeclarationContext.class,0);
		}
		public ExpressionListContext expressionList() {
			return getRuleContext(ExpressionListContext.class,0);
		}
		public ForInitContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_forInit; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterForInit(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitForInit(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitForInit(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ForInitContext forInit() throws RecognitionException {
		ForInitContext _localctx = new ForInitContext(_ctx, getState());
		enterRule(_localctx, 180, RULE_forInit);
		try {
			setState(1153);
			switch ( getInterpreter().adaptivePredict(_input,139,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1151); localVariableDeclaration();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1152); expressionList();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class EnhancedForControlContext extends ParserRuleContext {
		public VariableModifierContext variableModifier(int i) {
			return getRuleContext(VariableModifierContext.class,i);
		}
		public List<VariableModifierContext> variableModifier() {
			return getRuleContexts(VariableModifierContext.class);
		}
		public VariableDeclaratorIdContext variableDeclaratorId() {
			return getRuleContext(VariableDeclaratorIdContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public EnhancedForControlContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enhancedForControl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterEnhancedForControl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitEnhancedForControl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitEnhancedForControl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EnhancedForControlContext enhancedForControl() throws RecognitionException {
		EnhancedForControlContext _localctx = new EnhancedForControlContext(_ctx, getState());
		enterRule(_localctx, 182, RULE_enhancedForControl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1158);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==FINAL) {
				{
				{
				setState(1155); variableModifier();
				}
				}
				setState(1160);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1161); type();
			setState(1162); variableDeclaratorId();
			setState(1163); match(COLON);
			setState(1164); expression(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ForUpdateContext extends ParserRuleContext {
		public ExpressionListContext expressionList() {
			return getRuleContext(ExpressionListContext.class,0);
		}
		public ForUpdateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_forUpdate; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterForUpdate(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitForUpdate(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitForUpdate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ForUpdateContext forUpdate() throws RecognitionException {
		ForUpdateContext _localctx = new ForUpdateContext(_ctx, getState());
		enterRule(_localctx, 184, RULE_forUpdate);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1166); expressionList();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ParExpressionContext extends ParserRuleContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public ParExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_parExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterParExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitParExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitParExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ParExpressionContext parExpression() throws RecognitionException {
		ParExpressionContext _localctx = new ParExpressionContext(_ctx, getState());
		enterRule(_localctx, 186, RULE_parExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1168); match(LPAREN);
			setState(1169); expression(0);
			setState(1170); match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExpressionListContext extends ParserRuleContext {
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expressionList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterExpressionList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitExpressionList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitExpressionList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionListContext expressionList() throws RecognitionException {
		ExpressionListContext _localctx = new ExpressionListContext(_ctx, getState());
		enterRule(_localctx, 188, RULE_expressionList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1172); expression(0);
			setState(1177);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1173); match(COMMA);
				setState(1174); expression(0);
				}
				}
				setState(1179);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class StatementExpressionContext extends ParserRuleContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public StatementExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_statementExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterStatementExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitStatementExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitStatementExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StatementExpressionContext statementExpression() throws RecognitionException {
		StatementExpressionContext _localctx = new StatementExpressionContext(_ctx, getState());
		enterRule(_localctx, 190, RULE_statementExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1180); expression(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ConstantExpressionContext extends ParserRuleContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public ConstantExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constantExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterConstantExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitConstantExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitConstantExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstantExpressionContext constantExpression() throws RecognitionException {
		ConstantExpressionContext _localctx = new ConstantExpressionContext(_ctx, getState());
		enterRule(_localctx, 192, RULE_constantExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1182); expression(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExpressionContext extends ParserRuleContext {
		public TerminalNode Identifier() { return getToken(RefactorMethodSignatureParser.Identifier, 0); }
		public NonWildcardTypeArgumentsContext nonWildcardTypeArguments() {
			return getRuleContext(NonWildcardTypeArgumentsContext.class,0);
		}
		public ExplicitGenericInvocationContext explicitGenericInvocation() {
			return getRuleContext(ExplicitGenericInvocationContext.class,0);
		}
		public ExpressionListContext expressionList() {
			return getRuleContext(ExpressionListContext.class,0);
		}
		public InnerCreatorContext innerCreator() {
			return getRuleContext(InnerCreatorContext.class,0);
		}
		public SuperSuffixContext superSuffix() {
			return getRuleContext(SuperSuffixContext.class,0);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public PrimaryContext primary() {
			return getRuleContext(PrimaryContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public CreatorContext creator() {
			return getRuleContext(CreatorContext.class,0);
		}
		public ExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionContext expression() throws RecognitionException {
		return expression(0);
	}

	private ExpressionContext expression(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ExpressionContext _localctx = new ExpressionContext(_ctx, _parentState);
		ExpressionContext _prevctx = _localctx;
		int _startState = 194;
		enterRecursionRule(_localctx, 194, RULE_expression, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1197);
			switch ( getInterpreter().adaptivePredict(_input,142,_ctx) ) {
			case 1:
				{
				setState(1185); match(LPAREN);
				setState(1186); type();
				setState(1187); match(RPAREN);
				setState(1188); expression(17);
				}
				break;

			case 2:
				{
				setState(1190);
				_la = _input.LA(1);
				if ( !(((((_la - 137)) & ~0x3f) == 0 && ((1L << (_la - 137)) & ((1L << (INC - 137)) | (1L << (DEC - 137)) | (1L << (ADD - 137)) | (1L << (SUB - 137)))) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				consume();
				setState(1191); expression(15);
				}
				break;

			case 3:
				{
				setState(1192);
				_la = _input.LA(1);
				if ( !(_la==BANG || _la==TILDE) ) {
				_errHandler.recoverInline(this);
				}
				consume();
				setState(1193); expression(14);
				}
				break;

			case 4:
				{
				setState(1194); primary();
				}
				break;

			case 5:
				{
				setState(1195); match(NEW);
				setState(1196); creator();
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(1284);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,147,_ctx);
			while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(1282);
					switch ( getInterpreter().adaptivePredict(_input,146,_ctx) ) {
					case 1:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1199);
						if (!(precpred(_ctx, 13))) throw new FailedPredicateException(this, "precpred(_ctx, 13)");
						setState(1200);
						_la = _input.LA(1);
						if ( !(((((_la - 141)) & ~0x3f) == 0 && ((1L << (_la - 141)) & ((1L << (MUL - 141)) | (1L << (DIV - 141)) | (1L << (MOD - 141)))) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(1201); expression(14);
						}
						break;

					case 2:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1202);
						if (!(precpred(_ctx, 12))) throw new FailedPredicateException(this, "precpred(_ctx, 12)");
						setState(1203);
						_la = _input.LA(1);
						if ( !(_la==ADD || _la==SUB) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(1204); expression(13);
						}
						break;

					case 3:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1205);
						if (!(precpred(_ctx, 11))) throw new FailedPredicateException(this, "precpred(_ctx, 11)");
						setState(1213);
						switch ( getInterpreter().adaptivePredict(_input,143,_ctx) ) {
						case 1:
							{
							setState(1206); match(LT);
							setState(1207); match(LT);
							}
							break;

						case 2:
							{
							setState(1208); match(GT);
							setState(1209); match(GT);
							setState(1210); match(GT);
							}
							break;

						case 3:
							{
							setState(1211); match(GT);
							setState(1212); match(GT);
							}
							break;
						}
						setState(1215); expression(12);
						}
						break;

					case 4:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1216);
						if (!(precpred(_ctx, 10))) throw new FailedPredicateException(this, "precpred(_ctx, 10)");
						setState(1217);
						_la = _input.LA(1);
						if ( !(((((_la - 125)) & ~0x3f) == 0 && ((1L << (_la - 125)) & ((1L << (GT - 125)) | (1L << (LT - 125)) | (1L << (LE - 125)) | (1L << (GE - 125)))) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(1218); expression(11);
						}
						break;

					case 5:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1219);
						if (!(precpred(_ctx, 8))) throw new FailedPredicateException(this, "precpred(_ctx, 8)");
						setState(1220);
						_la = _input.LA(1);
						if ( !(_la==EQUAL || _la==NOTEQUAL) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(1221); expression(9);
						}
						break;

					case 6:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1222);
						if (!(precpred(_ctx, 7))) throw new FailedPredicateException(this, "precpred(_ctx, 7)");
						setState(1223); match(BITAND);
						setState(1224); expression(8);
						}
						break;

					case 7:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1225);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(1226); match(CARET);
						setState(1227); expression(7);
						}
						break;

					case 8:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1228);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(1229); match(BITOR);
						setState(1230); expression(6);
						}
						break;

					case 9:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1231);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(1232); match(AND);
						setState(1233); expression(5);
						}
						break;

					case 10:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1234);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(1235); match(OR);
						setState(1236); expression(4);
						}
						break;

					case 11:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1237);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(1238); match(QUESTION);
						setState(1239); expression(0);
						setState(1240); match(COLON);
						setState(1241); expression(3);
						}
						break;

					case 12:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1243);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(1244);
						_la = _input.LA(1);
						if ( !(((((_la - 124)) & ~0x3f) == 0 && ((1L << (_la - 124)) & ((1L << (ASSIGN - 124)) | (1L << (ADD_ASSIGN - 124)) | (1L << (SUB_ASSIGN - 124)) | (1L << (MUL_ASSIGN - 124)) | (1L << (DIV_ASSIGN - 124)) | (1L << (AND_ASSIGN - 124)) | (1L << (OR_ASSIGN - 124)) | (1L << (XOR_ASSIGN - 124)) | (1L << (MOD_ASSIGN - 124)) | (1L << (LSHIFT_ASSIGN - 124)) | (1L << (RSHIFT_ASSIGN - 124)) | (1L << (URSHIFT_ASSIGN - 124)))) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(1245); expression(1);
						}
						break;

					case 13:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1246);
						if (!(precpred(_ctx, 25))) throw new FailedPredicateException(this, "precpred(_ctx, 25)");
						setState(1247); match(DOT);
						setState(1248); match(Identifier);
						}
						break;

					case 14:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1249);
						if (!(precpred(_ctx, 24))) throw new FailedPredicateException(this, "precpred(_ctx, 24)");
						setState(1250); match(DOT);
						setState(1251); match(THIS);
						}
						break;

					case 15:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1252);
						if (!(precpred(_ctx, 23))) throw new FailedPredicateException(this, "precpred(_ctx, 23)");
						setState(1253); match(DOT);
						setState(1254); match(NEW);
						setState(1256);
						_la = _input.LA(1);
						if (_la==LT) {
							{
							setState(1255); nonWildcardTypeArguments();
							}
						}

						setState(1258); innerCreator();
						}
						break;

					case 16:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1259);
						if (!(precpred(_ctx, 22))) throw new FailedPredicateException(this, "precpred(_ctx, 22)");
						setState(1260); match(DOT);
						setState(1261); match(SUPER);
						setState(1262); superSuffix();
						}
						break;

					case 17:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1263);
						if (!(precpred(_ctx, 21))) throw new FailedPredicateException(this, "precpred(_ctx, 21)");
						setState(1264); match(DOT);
						setState(1265); explicitGenericInvocation();
						}
						break;

					case 18:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1266);
						if (!(precpred(_ctx, 20))) throw new FailedPredicateException(this, "precpred(_ctx, 20)");
						setState(1267); match(LBRACK);
						setState(1268); expression(0);
						setState(1269); match(RBRACK);
						}
						break;

					case 19:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1271);
						if (!(precpred(_ctx, 19))) throw new FailedPredicateException(this, "precpred(_ctx, 19)");
						setState(1272); match(LPAREN);
						setState(1274);
						_la = _input.LA(1);
						if (((((_la - 61)) & ~0x3f) == 0 && ((1L << (_la - 61)) & ((1L << (BOOLEAN - 61)) | (1L << (BYTE - 61)) | (1L << (CHAR - 61)) | (1L << (DOUBLE - 61)) | (1L << (FLOAT - 61)) | (1L << (INT - 61)) | (1L << (LONG - 61)) | (1L << (NEW - 61)) | (1L << (SHORT - 61)) | (1L << (SUPER - 61)) | (1L << (THIS - 61)) | (1L << (VOID - 61)) | (1L << (IntegerLiteral - 61)) | (1L << (FloatingPointLiteral - 61)) | (1L << (BooleanLiteral - 61)) | (1L << (CharacterLiteral - 61)) | (1L << (StringLiteral - 61)) | (1L << (NullLiteral - 61)) | (1L << (LPAREN - 61)))) != 0) || ((((_la - 126)) & ~0x3f) == 0 && ((1L << (_la - 126)) & ((1L << (LT - 126)) | (1L << (BANG - 126)) | (1L << (TILDE - 126)) | (1L << (INC - 126)) | (1L << (DEC - 126)) | (1L << (ADD - 126)) | (1L << (SUB - 126)) | (1L << (Identifier - 126)))) != 0)) {
							{
							setState(1273); expressionList();
							}
						}

						setState(1276); match(RPAREN);
						}
						break;

					case 20:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1277);
						if (!(precpred(_ctx, 16))) throw new FailedPredicateException(this, "precpred(_ctx, 16)");
						setState(1278);
						_la = _input.LA(1);
						if ( !(_la==INC || _la==DEC) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						}
						break;

					case 21:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1279);
						if (!(precpred(_ctx, 9))) throw new FailedPredicateException(this, "precpred(_ctx, 9)");
						setState(1280); match(INSTANCEOF);
						setState(1281); type();
						}
						break;
					}
					} 
				}
				setState(1286);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,147,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class PrimaryContext extends ParserRuleContext {
		public TerminalNode Identifier() { return getToken(RefactorMethodSignatureParser.Identifier, 0); }
		public NonWildcardTypeArgumentsContext nonWildcardTypeArguments() {
			return getRuleContext(NonWildcardTypeArgumentsContext.class,0);
		}
		public ExplicitGenericInvocationSuffixContext explicitGenericInvocationSuffix() {
			return getRuleContext(ExplicitGenericInvocationSuffixContext.class,0);
		}
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public ArgumentsContext arguments() {
			return getRuleContext(ArgumentsContext.class,0);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public PrimaryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_primary; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterPrimary(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitPrimary(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitPrimary(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PrimaryContext primary() throws RecognitionException {
		PrimaryContext _localctx = new PrimaryContext(_ctx, getState());
		enterRule(_localctx, 196, RULE_primary);
		try {
			setState(1308);
			switch ( getInterpreter().adaptivePredict(_input,149,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1287); match(LPAREN);
				setState(1288); expression(0);
				setState(1289); match(RPAREN);
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1291); match(THIS);
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1292); match(SUPER);
				}
				break;

			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1293); literal();
				}
				break;

			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(1294); match(Identifier);
				}
				break;

			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(1295); type();
				setState(1296); match(DOT);
				setState(1297); match(CLASS);
				}
				break;

			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(1299); match(VOID);
				setState(1300); match(DOT);
				setState(1301); match(CLASS);
				}
				break;

			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(1302); nonWildcardTypeArguments();
				setState(1306);
				switch (_input.LA(1)) {
				case SUPER:
				case Identifier:
					{
					setState(1303); explicitGenericInvocationSuffix();
					}
					break;
				case THIS:
					{
					setState(1304); match(THIS);
					setState(1305); arguments();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CreatorContext extends ParserRuleContext {
		public ArrayCreatorRestContext arrayCreatorRest() {
			return getRuleContext(ArrayCreatorRestContext.class,0);
		}
		public NonWildcardTypeArgumentsContext nonWildcardTypeArguments() {
			return getRuleContext(NonWildcardTypeArgumentsContext.class,0);
		}
		public ClassCreatorRestContext classCreatorRest() {
			return getRuleContext(ClassCreatorRestContext.class,0);
		}
		public CreatedNameContext createdName() {
			return getRuleContext(CreatedNameContext.class,0);
		}
		public CreatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_creator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterCreator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitCreator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitCreator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CreatorContext creator() throws RecognitionException {
		CreatorContext _localctx = new CreatorContext(_ctx, getState());
		enterRule(_localctx, 198, RULE_creator);
		try {
			setState(1319);
			switch (_input.LA(1)) {
			case LT:
				enterOuterAlt(_localctx, 1);
				{
				setState(1310); nonWildcardTypeArguments();
				setState(1311); createdName();
				setState(1312); classCreatorRest();
				}
				break;
			case BOOLEAN:
			case BYTE:
			case CHAR:
			case DOUBLE:
			case FLOAT:
			case INT:
			case LONG:
			case SHORT:
			case Identifier:
				enterOuterAlt(_localctx, 2);
				{
				setState(1314); createdName();
				setState(1317);
				switch (_input.LA(1)) {
				case LBRACK:
					{
					setState(1315); arrayCreatorRest();
					}
					break;
				case LPAREN:
					{
					setState(1316); classCreatorRest();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class CreatedNameContext extends ParserRuleContext {
		public List<TerminalNode> Identifier() { return getTokens(RefactorMethodSignatureParser.Identifier); }
		public TerminalNode Identifier(int i) {
			return getToken(RefactorMethodSignatureParser.Identifier, i);
		}
		public List<TypeArgumentsOrDiamondContext> typeArgumentsOrDiamond() {
			return getRuleContexts(TypeArgumentsOrDiamondContext.class);
		}
		public PrimitiveTypeContext primitiveType() {
			return getRuleContext(PrimitiveTypeContext.class,0);
		}
		public TypeArgumentsOrDiamondContext typeArgumentsOrDiamond(int i) {
			return getRuleContext(TypeArgumentsOrDiamondContext.class,i);
		}
		public CreatedNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_createdName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterCreatedName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitCreatedName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitCreatedName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CreatedNameContext createdName() throws RecognitionException {
		CreatedNameContext _localctx = new CreatedNameContext(_ctx, getState());
		enterRule(_localctx, 200, RULE_createdName);
		int _la;
		try {
			setState(1336);
			switch (_input.LA(1)) {
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(1321); match(Identifier);
				setState(1323);
				_la = _input.LA(1);
				if (_la==LT) {
					{
					setState(1322); typeArgumentsOrDiamond();
					}
				}

				setState(1332);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==DOT) {
					{
					{
					setState(1325); match(DOT);
					setState(1326); match(Identifier);
					setState(1328);
					_la = _input.LA(1);
					if (_la==LT) {
						{
						setState(1327); typeArgumentsOrDiamond();
						}
					}

					}
					}
					setState(1334);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case BOOLEAN:
			case BYTE:
			case CHAR:
			case DOUBLE:
			case FLOAT:
			case INT:
			case LONG:
			case SHORT:
				enterOuterAlt(_localctx, 2);
				{
				setState(1335); primitiveType();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InnerCreatorContext extends ParserRuleContext {
		public TerminalNode Identifier() { return getToken(RefactorMethodSignatureParser.Identifier, 0); }
		public ClassCreatorRestContext classCreatorRest() {
			return getRuleContext(ClassCreatorRestContext.class,0);
		}
		public NonWildcardTypeArgumentsOrDiamondContext nonWildcardTypeArgumentsOrDiamond() {
			return getRuleContext(NonWildcardTypeArgumentsOrDiamondContext.class,0);
		}
		public InnerCreatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_innerCreator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterInnerCreator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitInnerCreator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitInnerCreator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InnerCreatorContext innerCreator() throws RecognitionException {
		InnerCreatorContext _localctx = new InnerCreatorContext(_ctx, getState());
		enterRule(_localctx, 202, RULE_innerCreator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1338); match(Identifier);
			setState(1340);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(1339); nonWildcardTypeArgumentsOrDiamond();
				}
			}

			setState(1342); classCreatorRest();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ArrayCreatorRestContext extends ParserRuleContext {
		public ArrayInitializerContext arrayInitializer() {
			return getRuleContext(ArrayInitializerContext.class,0);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ArrayCreatorRestContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arrayCreatorRest; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterArrayCreatorRest(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitArrayCreatorRest(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitArrayCreatorRest(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArrayCreatorRestContext arrayCreatorRest() throws RecognitionException {
		ArrayCreatorRestContext _localctx = new ArrayCreatorRestContext(_ctx, getState());
		enterRule(_localctx, 204, RULE_arrayCreatorRest);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1344); match(LBRACK);
			setState(1372);
			switch (_input.LA(1)) {
			case RBRACK:
				{
				setState(1345); match(RBRACK);
				setState(1350);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==LBRACK) {
					{
					{
					setState(1346); match(LBRACK);
					setState(1347); match(RBRACK);
					}
					}
					setState(1352);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(1353); arrayInitializer();
				}
				break;
			case BOOLEAN:
			case BYTE:
			case CHAR:
			case DOUBLE:
			case FLOAT:
			case INT:
			case LONG:
			case NEW:
			case SHORT:
			case SUPER:
			case THIS:
			case VOID:
			case IntegerLiteral:
			case FloatingPointLiteral:
			case BooleanLiteral:
			case CharacterLiteral:
			case StringLiteral:
			case NullLiteral:
			case LPAREN:
			case LT:
			case BANG:
			case TILDE:
			case INC:
			case DEC:
			case ADD:
			case SUB:
			case Identifier:
				{
				setState(1354); expression(0);
				setState(1355); match(RBRACK);
				setState(1362);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,158,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1356); match(LBRACK);
						setState(1357); expression(0);
						setState(1358); match(RBRACK);
						}
						} 
					}
					setState(1364);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,158,_ctx);
				}
				setState(1369);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,159,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1365); match(LBRACK);
						setState(1366); match(RBRACK);
						}
						} 
					}
					setState(1371);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,159,_ctx);
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ClassCreatorRestContext extends ParserRuleContext {
		public ClassBodyContext classBody() {
			return getRuleContext(ClassBodyContext.class,0);
		}
		public ArgumentsContext arguments() {
			return getRuleContext(ArgumentsContext.class,0);
		}
		public ClassCreatorRestContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_classCreatorRest; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterClassCreatorRest(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitClassCreatorRest(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitClassCreatorRest(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ClassCreatorRestContext classCreatorRest() throws RecognitionException {
		ClassCreatorRestContext _localctx = new ClassCreatorRestContext(_ctx, getState());
		enterRule(_localctx, 206, RULE_classCreatorRest);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1374); arguments();
			setState(1376);
			switch ( getInterpreter().adaptivePredict(_input,161,_ctx) ) {
			case 1:
				{
				setState(1375); classBody();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExplicitGenericInvocationContext extends ParserRuleContext {
		public NonWildcardTypeArgumentsContext nonWildcardTypeArguments() {
			return getRuleContext(NonWildcardTypeArgumentsContext.class,0);
		}
		public ExplicitGenericInvocationSuffixContext explicitGenericInvocationSuffix() {
			return getRuleContext(ExplicitGenericInvocationSuffixContext.class,0);
		}
		public ExplicitGenericInvocationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_explicitGenericInvocation; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterExplicitGenericInvocation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitExplicitGenericInvocation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitExplicitGenericInvocation(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExplicitGenericInvocationContext explicitGenericInvocation() throws RecognitionException {
		ExplicitGenericInvocationContext _localctx = new ExplicitGenericInvocationContext(_ctx, getState());
		enterRule(_localctx, 208, RULE_explicitGenericInvocation);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1378); nonWildcardTypeArguments();
			setState(1379); explicitGenericInvocationSuffix();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NonWildcardTypeArgumentsContext extends ParserRuleContext {
		public TypeListContext typeList() {
			return getRuleContext(TypeListContext.class,0);
		}
		public NonWildcardTypeArgumentsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nonWildcardTypeArguments; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterNonWildcardTypeArguments(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitNonWildcardTypeArguments(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitNonWildcardTypeArguments(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NonWildcardTypeArgumentsContext nonWildcardTypeArguments() throws RecognitionException {
		NonWildcardTypeArgumentsContext _localctx = new NonWildcardTypeArgumentsContext(_ctx, getState());
		enterRule(_localctx, 210, RULE_nonWildcardTypeArguments);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1381); match(LT);
			setState(1382); typeList();
			setState(1383); match(GT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TypeArgumentsOrDiamondContext extends ParserRuleContext {
		public TypeArgumentsContext typeArguments() {
			return getRuleContext(TypeArgumentsContext.class,0);
		}
		public TypeArgumentsOrDiamondContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeArgumentsOrDiamond; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterTypeArgumentsOrDiamond(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitTypeArgumentsOrDiamond(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitTypeArgumentsOrDiamond(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeArgumentsOrDiamondContext typeArgumentsOrDiamond() throws RecognitionException {
		TypeArgumentsOrDiamondContext _localctx = new TypeArgumentsOrDiamondContext(_ctx, getState());
		enterRule(_localctx, 212, RULE_typeArgumentsOrDiamond);
		try {
			setState(1388);
			switch ( getInterpreter().adaptivePredict(_input,162,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1385); match(LT);
				setState(1386); match(GT);
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1387); typeArguments();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NonWildcardTypeArgumentsOrDiamondContext extends ParserRuleContext {
		public NonWildcardTypeArgumentsContext nonWildcardTypeArguments() {
			return getRuleContext(NonWildcardTypeArgumentsContext.class,0);
		}
		public NonWildcardTypeArgumentsOrDiamondContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nonWildcardTypeArgumentsOrDiamond; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterNonWildcardTypeArgumentsOrDiamond(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitNonWildcardTypeArgumentsOrDiamond(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitNonWildcardTypeArgumentsOrDiamond(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NonWildcardTypeArgumentsOrDiamondContext nonWildcardTypeArgumentsOrDiamond() throws RecognitionException {
		NonWildcardTypeArgumentsOrDiamondContext _localctx = new NonWildcardTypeArgumentsOrDiamondContext(_ctx, getState());
		enterRule(_localctx, 214, RULE_nonWildcardTypeArgumentsOrDiamond);
		try {
			setState(1393);
			switch ( getInterpreter().adaptivePredict(_input,163,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1390); match(LT);
				setState(1391); match(GT);
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1392); nonWildcardTypeArguments();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SuperSuffixContext extends ParserRuleContext {
		public TerminalNode Identifier() { return getToken(RefactorMethodSignatureParser.Identifier, 0); }
		public ArgumentsContext arguments() {
			return getRuleContext(ArgumentsContext.class,0);
		}
		public SuperSuffixContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_superSuffix; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterSuperSuffix(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitSuperSuffix(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitSuperSuffix(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SuperSuffixContext superSuffix() throws RecognitionException {
		SuperSuffixContext _localctx = new SuperSuffixContext(_ctx, getState());
		enterRule(_localctx, 216, RULE_superSuffix);
		try {
			setState(1401);
			switch (_input.LA(1)) {
			case LPAREN:
				enterOuterAlt(_localctx, 1);
				{
				setState(1395); arguments();
				}
				break;
			case DOT:
				enterOuterAlt(_localctx, 2);
				{
				setState(1396); match(DOT);
				setState(1397); match(Identifier);
				setState(1399);
				switch ( getInterpreter().adaptivePredict(_input,164,_ctx) ) {
				case 1:
					{
					setState(1398); arguments();
					}
					break;
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExplicitGenericInvocationSuffixContext extends ParserRuleContext {
		public TerminalNode Identifier() { return getToken(RefactorMethodSignatureParser.Identifier, 0); }
		public SuperSuffixContext superSuffix() {
			return getRuleContext(SuperSuffixContext.class,0);
		}
		public ArgumentsContext arguments() {
			return getRuleContext(ArgumentsContext.class,0);
		}
		public ExplicitGenericInvocationSuffixContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_explicitGenericInvocationSuffix; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterExplicitGenericInvocationSuffix(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitExplicitGenericInvocationSuffix(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitExplicitGenericInvocationSuffix(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExplicitGenericInvocationSuffixContext explicitGenericInvocationSuffix() throws RecognitionException {
		ExplicitGenericInvocationSuffixContext _localctx = new ExplicitGenericInvocationSuffixContext(_ctx, getState());
		enterRule(_localctx, 218, RULE_explicitGenericInvocationSuffix);
		try {
			setState(1407);
			switch (_input.LA(1)) {
			case SUPER:
				enterOuterAlt(_localctx, 1);
				{
				setState(1403); match(SUPER);
				setState(1404); superSuffix();
				}
				break;
			case Identifier:
				enterOuterAlt(_localctx, 2);
				{
				setState(1405); match(Identifier);
				setState(1406); arguments();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ArgumentsContext extends ParserRuleContext {
		public ExpressionListContext expressionList() {
			return getRuleContext(ExpressionListContext.class,0);
		}
		public ArgumentsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arguments; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterArguments(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitArguments(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitArguments(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArgumentsContext arguments() throws RecognitionException {
		ArgumentsContext _localctx = new ArgumentsContext(_ctx, getState());
		enterRule(_localctx, 220, RULE_arguments);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1409); match(LPAREN);
			setState(1411);
			_la = _input.LA(1);
			if (((((_la - 61)) & ~0x3f) == 0 && ((1L << (_la - 61)) & ((1L << (BOOLEAN - 61)) | (1L << (BYTE - 61)) | (1L << (CHAR - 61)) | (1L << (DOUBLE - 61)) | (1L << (FLOAT - 61)) | (1L << (INT - 61)) | (1L << (LONG - 61)) | (1L << (NEW - 61)) | (1L << (SHORT - 61)) | (1L << (SUPER - 61)) | (1L << (THIS - 61)) | (1L << (VOID - 61)) | (1L << (IntegerLiteral - 61)) | (1L << (FloatingPointLiteral - 61)) | (1L << (BooleanLiteral - 61)) | (1L << (CharacterLiteral - 61)) | (1L << (StringLiteral - 61)) | (1L << (NullLiteral - 61)) | (1L << (LPAREN - 61)))) != 0) || ((((_la - 126)) & ~0x3f) == 0 && ((1L << (_la - 126)) & ((1L << (LT - 126)) | (1L << (BANG - 126)) | (1L << (TILDE - 126)) | (1L << (INC - 126)) | (1L << (DEC - 126)) | (1L << (ADD - 126)) | (1L << (SUB - 126)) | (1L << (Identifier - 126)))) != 0)) {
				{
				setState(1410); expressionList();
				}
			}

			setState(1413); match(RPAREN);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 6: return targetTypePattern_sempred((TargetTypePatternContext)_localctx, predIndex);

		case 7: return formalTypePattern_sempred((FormalTypePatternContext)_localctx, predIndex);

		case 97: return expression_sempred((ExpressionContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean expression_sempred(ExpressionContext _localctx, int predIndex) {
		switch (predIndex) {
		case 4: return precpred(_ctx, 13);

		case 5: return precpred(_ctx, 12);

		case 6: return precpred(_ctx, 11);

		case 7: return precpred(_ctx, 10);

		case 8: return precpred(_ctx, 8);

		case 9: return precpred(_ctx, 7);

		case 10: return precpred(_ctx, 6);

		case 11: return precpred(_ctx, 5);

		case 12: return precpred(_ctx, 4);

		case 13: return precpred(_ctx, 3);

		case 14: return precpred(_ctx, 2);

		case 15: return precpred(_ctx, 1);

		case 16: return precpred(_ctx, 25);

		case 17: return precpred(_ctx, 24);

		case 18: return precpred(_ctx, 23);

		case 19: return precpred(_ctx, 22);

		case 20: return precpred(_ctx, 21);

		case 21: return precpred(_ctx, 20);

		case 22: return precpred(_ctx, 19);

		case 23: return precpred(_ctx, 16);

		case 24: return precpred(_ctx, 9);
		}
		return true;
	}
	private boolean formalTypePattern_sempred(FormalTypePatternContext _localctx, int predIndex) {
		switch (predIndex) {
		case 2: return precpred(_ctx, 2);

		case 3: return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean targetTypePattern_sempred(TargetTypePatternContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0: return precpred(_ctx, 2);

		case 1: return precpred(_ctx, 1);
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3\u00b3\u058a\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t+\4"+
		",\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64\t"+
		"\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\4<\t<\4=\t="+
		"\4>\t>\4?\t?\4@\t@\4A\tA\4B\tB\4C\tC\4D\tD\4E\tE\4F\tF\4G\tG\4H\tH\4I"+
		"\tI\4J\tJ\4K\tK\4L\tL\4M\tM\4N\tN\4O\tO\4P\tP\4Q\tQ\4R\tR\4S\tS\4T\tT"+
		"\4U\tU\4V\tV\4W\tW\4X\tX\4Y\tY\4Z\tZ\4[\t[\4\\\t\\\4]\t]\4^\t^\4_\t_\4"+
		"`\t`\4a\ta\4b\tb\4c\tc\4d\td\4e\te\4f\tf\4g\tg\4h\th\4i\ti\4j\tj\4k\t"+
		"k\4l\tl\4m\tm\4n\tn\4o\to\4p\tp\3\2\3\2\3\2\3\2\3\3\3\3\5\3\u00e7\n\3"+
		"\3\3\3\3\3\4\3\4\3\4\7\4\u00ee\n\4\f\4\16\4\u00f1\13\4\3\4\3\4\3\4\7\4"+
		"\u00f6\n\4\f\4\16\4\u00f9\13\4\3\4\3\4\3\4\5\4\u00fe\n\4\3\5\3\5\3\6\3"+
		"\6\3\6\7\6\u0105\n\6\f\6\16\6\u0108\13\6\3\6\3\6\3\6\5\6\u010d\n\6\3\7"+
		"\3\7\3\7\3\7\3\7\5\7\u0114\n\7\3\b\3\b\3\b\3\b\5\b\u011a\n\b\3\b\3\b\3"+
		"\b\3\b\3\b\3\b\7\b\u0122\n\b\f\b\16\b\u0125\13\b\3\t\3\t\3\t\3\t\3\t\5"+
		"\t\u012c\n\t\3\t\3\t\3\t\3\t\3\t\3\t\7\t\u0134\n\t\f\t\16\t\u0137\13\t"+
		"\3\n\6\n\u013a\n\n\r\n\16\n\u013b\3\13\3\13\3\13\7\13\u0141\n\13\f\13"+
		"\16\13\u0144\13\13\3\13\5\13\u0147\n\13\3\13\3\13\3\13\7\13\u014c\n\13"+
		"\f\13\16\13\u014f\13\13\3\13\5\13\u0152\n\13\5\13\u0154\n\13\3\f\5\f\u0157"+
		"\n\f\3\f\7\f\u015a\n\f\f\f\16\f\u015d\13\f\3\f\7\f\u0160\n\f\f\f\16\f"+
		"\u0163\13\f\3\f\3\f\3\r\7\r\u0168\n\r\f\r\16\r\u016b\13\r\3\r\3\r\3\r"+
		"\3\r\3\16\3\16\5\16\u0173\n\16\3\16\3\16\3\16\5\16\u0178\n\16\3\16\3\16"+
		"\3\17\7\17\u017d\n\17\f\17\16\17\u0180\13\17\3\17\3\17\7\17\u0184\n\17"+
		"\f\17\16\17\u0187\13\17\3\17\3\17\7\17\u018b\n\17\f\17\16\17\u018e\13"+
		"\17\3\17\3\17\7\17\u0192\n\17\f\17\16\17\u0195\13\17\3\17\3\17\5\17\u0199"+
		"\n\17\3\20\3\20\5\20\u019d\n\20\3\21\3\21\5\21\u01a1\n\21\3\22\3\22\5"+
		"\22\u01a5\n\22\3\23\3\23\3\23\5\23\u01aa\n\23\3\23\3\23\5\23\u01ae\n\23"+
		"\3\23\3\23\5\23\u01b2\n\23\3\23\3\23\3\24\3\24\3\24\3\24\7\24\u01ba\n"+
		"\24\f\24\16\24\u01bd\13\24\3\24\3\24\3\25\3\25\3\25\5\25\u01c4\n\25\3"+
		"\26\3\26\3\26\7\26\u01c9\n\26\f\26\16\26\u01cc\13\26\3\27\3\27\3\27\3"+
		"\27\5\27\u01d2\n\27\3\27\3\27\5\27\u01d6\n\27\3\27\5\27\u01d9\n\27\3\27"+
		"\5\27\u01dc\n\27\3\27\3\27\3\30\3\30\3\30\7\30\u01e3\n\30\f\30\16\30\u01e6"+
		"\13\30\3\31\7\31\u01e9\n\31\f\31\16\31\u01ec\13\31\3\31\3\31\5\31\u01f0"+
		"\n\31\3\31\5\31\u01f3\n\31\3\32\3\32\7\32\u01f7\n\32\f\32\16\32\u01fa"+
		"\13\32\3\33\3\33\3\33\5\33\u01ff\n\33\3\33\3\33\5\33\u0203\n\33\3\33\3"+
		"\33\3\34\3\34\3\34\7\34\u020a\n\34\f\34\16\34\u020d\13\34\3\35\3\35\7"+
		"\35\u0211\n\35\f\35\16\35\u0214\13\35\3\35\3\35\3\36\3\36\7\36\u021a\n"+
		"\36\f\36\16\36\u021d\13\36\3\36\3\36\3\37\3\37\5\37\u0223\n\37\3\37\3"+
		"\37\7\37\u0227\n\37\f\37\16\37\u022a\13\37\3\37\5\37\u022d\n\37\3 \3 "+
		"\3 \3 \3 \3 \3 \3 \3 \5 \u0238\n \3!\3!\5!\u023c\n!\3!\3!\3!\3!\7!\u0242"+
		"\n!\f!\16!\u0245\13!\3!\3!\5!\u0249\n!\3!\3!\5!\u024d\n!\3\"\3\"\3\"\3"+
		"#\3#\3#\3#\5#\u0256\n#\3#\3#\3$\3$\3$\3%\3%\3%\3%\3&\7&\u0262\n&\f&\16"+
		"&\u0265\13&\3&\3&\5&\u0269\n&\3\'\3\'\3\'\3\'\3\'\3\'\3\'\5\'\u0272\n"+
		"\'\3(\3(\3(\3(\7(\u0278\n(\f(\16(\u027b\13(\3(\3(\3)\3)\3)\7)\u0282\n"+
		")\f)\16)\u0285\13)\3)\3)\3)\3*\3*\5*\u028c\n*\3*\3*\3*\3*\7*\u0292\n*"+
		"\f*\16*\u0295\13*\3*\3*\5*\u0299\n*\3*\3*\3+\3+\3+\3,\3,\3,\7,\u02a3\n"+
		",\f,\16,\u02a6\13,\3-\3-\3-\5-\u02ab\n-\3.\3.\3.\7.\u02b0\n.\f.\16.\u02b3"+
		"\13.\3/\3/\5/\u02b7\n/\3\60\3\60\3\60\3\60\7\60\u02bd\n\60\f\60\16\60"+
		"\u02c0\13\60\3\60\5\60\u02c3\n\60\5\60\u02c5\n\60\3\60\3\60\3\61\3\61"+
		"\3\62\3\62\3\62\7\62\u02ce\n\62\f\62\16\62\u02d1\13\62\3\62\3\62\3\62"+
		"\7\62\u02d6\n\62\f\62\16\62\u02d9\13\62\5\62\u02db\n\62\3\63\3\63\5\63"+
		"\u02df\n\63\3\63\3\63\3\63\5\63\u02e4\n\63\7\63\u02e6\n\63\f\63\16\63"+
		"\u02e9\13\63\3\64\3\64\3\65\3\65\3\65\3\65\7\65\u02f1\n\65\f\65\16\65"+
		"\u02f4\13\65\3\65\3\65\3\66\3\66\3\66\3\66\5\66\u02fc\n\66\5\66\u02fe"+
		"\n\66\3\67\3\67\3\67\7\67\u0303\n\67\f\67\16\67\u0306\13\67\38\38\58\u030a"+
		"\n8\38\38\39\39\39\79\u0311\n9\f9\169\u0314\139\39\39\59\u0318\n9\39\5"+
		"9\u031b\n9\3:\7:\u031e\n:\f:\16:\u0321\13:\3:\3:\3:\3;\7;\u0327\n;\f;"+
		"\16;\u032a\13;\3;\3;\3;\3;\3<\3<\3=\3=\3>\3>\3>\7>\u0337\n>\f>\16>\u033a"+
		"\13>\3?\3?\3@\3@\3@\3@\3@\5@\u0343\n@\3@\5@\u0346\n@\3A\3A\3B\3B\3B\7"+
		"B\u034d\nB\fB\16B\u0350\13B\3C\3C\3C\3C\3D\3D\3D\5D\u0359\nD\3E\3E\3E"+
		"\3E\7E\u035f\nE\fE\16E\u0362\13E\5E\u0364\nE\3E\5E\u0367\nE\3E\3E\3F\3"+
		"F\3F\3F\3F\3G\3G\7G\u0372\nG\fG\16G\u0375\13G\3G\3G\3H\7H\u037a\nH\fH"+
		"\16H\u037d\13H\3H\3H\5H\u0381\nH\3I\3I\3I\3I\3I\3I\5I\u0389\nI\3I\3I\5"+
		"I\u038d\nI\3I\3I\5I\u0391\nI\3I\3I\5I\u0395\nI\5I\u0397\nI\3J\3J\5J\u039b"+
		"\nJ\3K\3K\3K\3K\5K\u03a1\nK\3L\3L\3M\3M\3M\3N\3N\7N\u03aa\nN\fN\16N\u03ad"+
		"\13N\3N\3N\3O\3O\3O\5O\u03b4\nO\3P\3P\3P\3Q\7Q\u03ba\nQ\fQ\16Q\u03bd\13"+
		"Q\3Q\3Q\3Q\3R\3R\3R\3R\3R\5R\u03c7\nR\3R\3R\3R\3R\3R\3R\3R\5R\u03d0\n"+
		"R\3R\3R\3R\3R\3R\3R\3R\3R\3R\3R\3R\3R\3R\3R\3R\3R\3R\3R\3R\6R\u03e5\n"+
		"R\rR\16R\u03e6\3R\5R\u03ea\nR\3R\5R\u03ed\nR\3R\3R\3R\3R\7R\u03f3\nR\f"+
		"R\16R\u03f6\13R\3R\5R\u03f9\nR\3R\3R\3R\3R\7R\u03ff\nR\fR\16R\u0402\13"+
		"R\3R\7R\u0405\nR\fR\16R\u0408\13R\3R\3R\3R\3R\3R\3R\3R\3R\5R\u0412\nR"+
		"\3R\3R\3R\3R\3R\3R\3R\5R\u041b\nR\3R\3R\3R\5R\u0420\nR\3R\3R\3R\3R\3R"+
		"\3R\3R\3R\5R\u042a\nR\3S\3S\3S\7S\u042f\nS\fS\16S\u0432\13S\3S\3S\3S\3"+
		"S\3S\3T\3T\3T\7T\u043c\nT\fT\16T\u043f\13T\3U\3U\3U\3V\3V\3V\5V\u0447"+
		"\nV\3V\3V\3W\3W\3W\7W\u044e\nW\fW\16W\u0451\13W\3X\7X\u0454\nX\fX\16X"+
		"\u0457\13X\3X\3X\3X\3X\3X\3Y\6Y\u045f\nY\rY\16Y\u0460\3Y\6Y\u0464\nY\r"+
		"Y\16Y\u0465\3Z\3Z\3Z\3Z\3Z\3Z\3Z\3Z\3Z\3Z\5Z\u0472\nZ\3[\3[\5[\u0476\n"+
		"[\3[\3[\5[\u047a\n[\3[\3[\5[\u047e\n[\5[\u0480\n[\3\\\3\\\5\\\u0484\n"+
		"\\\3]\7]\u0487\n]\f]\16]\u048a\13]\3]\3]\3]\3]\3]\3^\3^\3_\3_\3_\3_\3"+
		"`\3`\3`\7`\u049a\n`\f`\16`\u049d\13`\3a\3a\3b\3b\3c\3c\3c\3c\3c\3c\3c"+
		"\3c\3c\3c\3c\3c\3c\5c\u04b0\nc\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c"+
		"\3c\5c\u04c0\nc\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c"+
		"\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c"+
		"\5c\u04eb\nc\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\5c\u04fd"+
		"\nc\3c\3c\3c\3c\3c\3c\7c\u0505\nc\fc\16c\u0508\13c\3d\3d\3d\3d\3d\3d\3"+
		"d\3d\3d\3d\3d\3d\3d\3d\3d\3d\3d\3d\3d\5d\u051d\nd\5d\u051f\nd\3e\3e\3"+
		"e\3e\3e\3e\3e\5e\u0528\ne\5e\u052a\ne\3f\3f\5f\u052e\nf\3f\3f\3f\5f\u0533"+
		"\nf\7f\u0535\nf\ff\16f\u0538\13f\3f\5f\u053b\nf\3g\3g\5g\u053f\ng\3g\3"+
		"g\3h\3h\3h\3h\7h\u0547\nh\fh\16h\u054a\13h\3h\3h\3h\3h\3h\3h\3h\7h\u0553"+
		"\nh\fh\16h\u0556\13h\3h\3h\7h\u055a\nh\fh\16h\u055d\13h\5h\u055f\nh\3"+
		"i\3i\5i\u0563\ni\3j\3j\3j\3k\3k\3k\3k\3l\3l\3l\5l\u056f\nl\3m\3m\3m\5"+
		"m\u0574\nm\3n\3n\3n\3n\5n\u057a\nn\5n\u057c\nn\3o\3o\3o\3o\5o\u0582\n"+
		"o\3p\3p\5p\u0586\np\3p\3p\3p\2\5\16\20\u00c4q\2\4\6\b\n\f\16\20\22\24"+
		"\26\30\32\34\36 \"$&(*,.\60\62\64\668:<>@BDFHJLNPRTVXZ\\^`bdfhjlnprtv"+
		"xz|~\u0080\u0082\u0084\u0086\u0088\u008a\u008c\u008e\u0090\u0092\u0094"+
		"\u0096\u0098\u009a\u009c\u009e\u00a0\u00a2\u00a4\u00a6\u00a8\u00aa\u00ac"+
		"\u00ae\u00b0\u00b2\u00b4\u00b6\u00b8\u00ba\u00bc\u00be\u00c0\u00c2\u00c4"+
		"\u00c6\u00c8\u00ca\u00cc\u00ce\u00d0\u00d2\u00d4\u00d6\u00d8\u00da\u00dc"+
		"\u00de\2\20\6\2\3\3}}\u008f\u008f\u00a0\u00a0\6\2ZZffjjmm\6\2==NN]_bc"+
		"\n\2??AADDJJPPWWYYaa\4\2MMdd\3\2ot\3\2\u008b\u008e\3\2\u0081\u0082\4\2"+
		"\u008f\u0090\u0094\u0094\3\2\u008d\u008e\4\2\177\u0080\u0086\u0087\4\2"+
		"\u0085\u0085\u0088\u0088\4\2~~\u0095\u009f\3\2\u008b\u008c\u0607\2\u00e0"+
		"\3\2\2\2\4\u00e4\3\2\2\2\6\u00fd\3\2\2\2\b\u00ff\3\2\2\2\n\u010c\3\2\2"+
		"\2\f\u0113\3\2\2\2\16\u0119\3\2\2\2\20\u012b\3\2\2\2\22\u0139\3\2\2\2"+
		"\24\u0153\3\2\2\2\26\u0156\3\2\2\2\30\u0169\3\2\2\2\32\u0170\3\2\2\2\34"+
		"\u0198\3\2\2\2\36\u019c\3\2\2\2 \u01a0\3\2\2\2\"\u01a4\3\2\2\2$\u01a6"+
		"\3\2\2\2&\u01b5\3\2\2\2(\u01c0\3\2\2\2*\u01c5\3\2\2\2,\u01cd\3\2\2\2."+
		"\u01df\3\2\2\2\60\u01ea\3\2\2\2\62\u01f4\3\2\2\2\64\u01fb\3\2\2\2\66\u0206"+
		"\3\2\2\28\u020e\3\2\2\2:\u0217\3\2\2\2<\u022c\3\2\2\2>\u0237\3\2\2\2@"+
		"\u023b\3\2\2\2B\u024e\3\2\2\2D\u0251\3\2\2\2F\u0259\3\2\2\2H\u025c\3\2"+
		"\2\2J\u0268\3\2\2\2L\u0271\3\2\2\2N\u0273\3\2\2\2P\u027e\3\2\2\2R\u028b"+
		"\3\2\2\2T\u029c\3\2\2\2V\u029f\3\2\2\2X\u02a7\3\2\2\2Z\u02ac\3\2\2\2\\"+
		"\u02b6\3\2\2\2^\u02b8\3\2\2\2`\u02c8\3\2\2\2b\u02da\3\2\2\2d\u02dc\3\2"+
		"\2\2f\u02ea\3\2\2\2h\u02ec\3\2\2\2j\u02fd\3\2\2\2l\u02ff\3\2\2\2n\u0307"+
		"\3\2\2\2p\u031a\3\2\2\2r\u031f\3\2\2\2t\u0328\3\2\2\2v\u032f\3\2\2\2x"+
		"\u0331\3\2\2\2z\u0333\3\2\2\2|\u033b\3\2\2\2~\u033d\3\2\2\2\u0080\u0347"+
		"\3\2\2\2\u0082\u0349\3\2\2\2\u0084\u0351\3\2\2\2\u0086\u0358\3\2\2\2\u0088"+
		"\u035a\3\2\2\2\u008a\u036a\3\2\2\2\u008c\u036f\3\2\2\2\u008e\u0380\3\2"+
		"\2\2\u0090\u0396\3\2\2\2\u0092\u039a\3\2\2\2\u0094\u039c\3\2\2\2\u0096"+
		"\u03a2\3\2\2\2\u0098\u03a4\3\2\2\2\u009a\u03a7\3\2\2\2\u009c\u03b3\3\2"+
		"\2\2\u009e\u03b5\3\2\2\2\u00a0\u03bb\3\2\2\2\u00a2\u0429\3\2\2\2\u00a4"+
		"\u042b\3\2\2\2\u00a6\u0438\3\2\2\2\u00a8\u0440\3\2\2\2\u00aa\u0443\3\2"+
		"\2\2\u00ac\u044a\3\2\2\2\u00ae\u0455\3\2\2\2\u00b0\u045e\3\2\2\2\u00b2"+
		"\u0471\3\2\2\2\u00b4\u047f\3\2\2\2\u00b6\u0483\3\2\2\2\u00b8\u0488\3\2"+
		"\2\2\u00ba\u0490\3\2\2\2\u00bc\u0492\3\2\2\2\u00be\u0496\3\2\2\2\u00c0"+
		"\u049e\3\2\2\2\u00c2\u04a0\3\2\2\2\u00c4\u04af\3\2\2\2\u00c6\u051e\3\2"+
		"\2\2\u00c8\u0529\3\2\2\2\u00ca\u053a\3\2\2\2\u00cc\u053c\3\2\2\2\u00ce"+
		"\u0542\3\2\2\2\u00d0\u0560\3\2\2\2\u00d2\u0564\3\2\2\2\u00d4\u0567\3\2"+
		"\2\2\u00d6\u056e\3\2\2\2\u00d8\u0573\3\2\2\2\u00da\u057b\3\2\2\2\u00dc"+
		"\u0581\3\2\2\2\u00de\u0583\3\2\2\2\u00e0\u00e1\5\16\b\2\u00e1\u00e2\5"+
		"\24\13\2\u00e2\u00e3\5\4\3\2\u00e3\3\3\2\2\2\u00e4\u00e6\7u\2\2\u00e5"+
		"\u00e7\5\6\4\2\u00e6\u00e5\3\2\2\2\u00e6\u00e7\3\2\2\2\u00e7\u00e8\3\2"+
		"\2\2\u00e8\u00e9\7v\2\2\u00e9\5\3\2\2\2\u00ea\u00ef\5\b\5\2\u00eb\u00ec"+
		"\7|\2\2\u00ec\u00ee\5\n\6\2\u00ed\u00eb\3\2\2\2\u00ee\u00f1\3\2\2\2\u00ef"+
		"\u00ed\3\2\2\2\u00ef\u00f0\3\2\2\2\u00f0\u00fe\3\2\2\2\u00f1\u00ef\3\2"+
		"\2\2\u00f2\u00f7\5\f\7\2\u00f3\u00f4\7|\2\2\u00f4\u00f6\5\6\4\2\u00f5"+
		"\u00f3\3\2\2\2\u00f6\u00f9\3\2\2\2\u00f7\u00f5\3\2\2\2\u00f7\u00f8\3\2"+
		"\2\2\u00f8\u00fe\3\2\2\2\u00f9\u00f7\3\2\2\2\u00fa\u00fb\5\20\t\2\u00fb"+
		"\u00fc\7\u00a1\2\2\u00fc\u00fe\3\2\2\2\u00fd\u00ea\3\2\2\2\u00fd\u00f2"+
		"\3\2\2\2\u00fd\u00fa\3\2\2\2\u00fe\7\3\2\2\2\u00ff\u0100\7\3\2\2\u0100"+
		"\t\3\2\2\2\u0101\u0106\5\f\7\2\u0102\u0103\7|\2\2\u0103\u0105\5\n\6\2"+
		"\u0104\u0102\3\2\2\2\u0105\u0108\3\2\2\2\u0106\u0104\3\2\2\2\u0106\u0107"+
		"\3\2\2\2\u0107\u010d\3\2\2\2\u0108\u0106\3\2\2\2\u0109\u010a\5\20\t\2"+
		"\u010a\u010b\7\u00a1\2\2\u010b\u010d\3\2\2\2\u010c\u0101\3\2\2\2\u010c"+
		"\u0109\3\2\2\2\u010d\13\3\2\2\2\u010e\u010f\7u\2\2\u010f\u0110\5\20\t"+
		"\2\u0110\u0111\7v\2\2\u0111\u0114\3\2\2\2\u0112\u0114\5\20\t\2\u0113\u010e"+
		"\3\2\2\2\u0113\u0112\3\2\2\2\u0114\r\3\2\2\2\u0115\u0116\b\b\1\2\u0116"+
		"\u0117\7\u0081\2\2\u0117\u011a\5\16\b\5\u0118\u011a\5\22\n\2\u0119\u0115"+
		"\3\2\2\2\u0119\u0118\3\2\2\2\u011a\u0123\3\2\2\2\u011b\u011c\f\4\2\2\u011c"+
		"\u011d\7\u0089\2\2\u011d\u0122\5\16\b\5\u011e\u011f\f\3\2\2\u011f\u0120"+
		"\7\u008a\2\2\u0120\u0122\5\16\b\4\u0121\u011b\3\2\2\2\u0121\u011e\3\2"+
		"\2\2\u0122\u0125\3\2\2\2\u0123\u0121\3\2\2\2\u0123\u0124\3\2\2\2\u0124"+
		"\17\3\2\2\2\u0125\u0123\3\2\2\2\u0126\u0127\b\t\1\2\u0127\u0128\7\u0081"+
		"\2\2\u0128\u012c\5\20\t\5\u0129\u012c\5\22\n\2\u012a\u012c\5f\64\2\u012b"+
		"\u0126\3\2\2\2\u012b\u0129\3\2\2\2\u012b\u012a\3\2\2\2\u012c\u0135\3\2"+
		"\2\2\u012d\u012e\f\4\2\2\u012e\u012f\7\u0089\2\2\u012f\u0134\5\20\t\5"+
		"\u0130\u0131\f\3\2\2\u0131\u0132\7\u008a\2\2\u0132\u0134\5\20\t\4\u0133"+
		"\u012d\3\2\2\2\u0133\u0130\3\2\2\2\u0134\u0137\3\2\2\2\u0135\u0133\3\2"+
		"\2\2\u0135\u0136\3\2\2\2\u0136\21\3\2\2\2\u0137\u0135\3\2\2\2\u0138\u013a"+
		"\t\2\2\2\u0139\u0138\3\2\2\2\u013a\u013b\3\2\2\2\u013b\u0139\3\2\2\2\u013b"+
		"\u013c\3\2\2\2\u013c\23\3\2\2\2\u013d\u0142\7\u00a0\2\2\u013e\u013f\7"+
		"\u008f\2\2\u013f\u0141\7\u00a0\2\2\u0140\u013e\3\2\2\2\u0141\u0144\3\2"+
		"\2\2\u0142\u0140\3\2\2\2\u0142\u0143\3\2\2\2\u0143\u0146\3\2\2\2\u0144"+
		"\u0142\3\2\2\2\u0145\u0147\7\u008f\2\2\u0146\u0145\3\2\2\2\u0146\u0147"+
		"\3\2\2\2\u0147\u0154\3\2\2\2\u0148\u014d\7\u008f\2\2\u0149\u014a\7\u00a0"+
		"\2\2\u014a\u014c\7\u008f\2\2\u014b\u0149\3\2\2\2\u014c\u014f\3\2\2\2\u014d"+
		"\u014b\3\2\2\2\u014d\u014e\3\2\2\2\u014e\u0151\3\2\2\2\u014f\u014d\3\2"+
		"\2\2\u0150\u0152\7\u00a0\2\2\u0151\u0150\3\2\2\2\u0151\u0152\3\2\2\2\u0152"+
		"\u0154\3\2\2\2\u0153\u013d\3\2\2\2\u0153\u0148\3\2\2\2\u0154\25\3\2\2"+
		"\2\u0155\u0157\5\30\r\2\u0156\u0155\3\2\2\2\u0156\u0157\3\2\2\2\u0157"+
		"\u015b\3\2\2\2\u0158\u015a\5\32\16\2\u0159\u0158\3\2\2\2\u015a\u015d\3"+
		"\2\2\2\u015b\u0159\3\2\2\2\u015b\u015c\3\2\2\2\u015c\u0161\3\2\2\2\u015d"+
		"\u015b\3\2\2\2\u015e\u0160\5\34\17\2\u015f\u015e\3\2\2\2\u0160\u0163\3"+
		"\2\2\2\u0161\u015f\3\2\2\2\u0161\u0162\3\2\2\2\u0162\u0164\3\2\2\2\u0163"+
		"\u0161\3\2\2\2\u0164\u0165\7\2\2\3\u0165\27\3\2\2\2\u0166\u0168\5~@\2"+
		"\u0167\u0166\3\2\2\2\u0168\u016b\3\2\2\2\u0169\u0167\3\2\2\2\u0169\u016a"+
		"\3\2\2\2\u016a\u016c\3\2\2\2\u016b\u0169\3\2\2\2\u016c\u016d\7\\\2\2\u016d"+
		"\u016e\5z>\2\u016e\u016f\7{\2\2\u016f\31\3\2\2\2\u0170\u0172\7U\2\2\u0171"+
		"\u0173\7b\2\2\u0172\u0171\3\2\2\2\u0172\u0173\3\2\2\2\u0173\u0174\3\2"+
		"\2\2\u0174\u0177\5z>\2\u0175\u0176\7}\2\2\u0176\u0178\7\u008f\2\2\u0177"+
		"\u0175\3\2\2\2\u0177\u0178\3\2\2\2\u0178\u0179\3\2\2\2\u0179\u017a\7{"+
		"\2\2\u017a\33\3\2\2\2\u017b\u017d\5 \21\2\u017c\u017b\3\2\2\2\u017d\u0180"+
		"\3\2\2\2\u017e\u017c\3\2\2\2\u017e\u017f\3\2\2\2\u017f\u0181\3\2\2\2\u0180"+
		"\u017e\3\2\2\2\u0181\u0199\5$\23\2\u0182\u0184\5 \21\2\u0183\u0182\3\2"+
		"\2\2\u0184\u0187\3\2\2\2\u0185\u0183\3\2\2\2\u0185\u0186\3\2\2\2\u0186"+
		"\u0188\3\2\2\2\u0187\u0185\3\2\2\2\u0188\u0199\5,\27\2\u0189\u018b\5 "+
		"\21\2\u018a\u0189\3\2\2\2\u018b\u018e\3\2\2\2\u018c\u018a\3\2\2\2\u018c"+
		"\u018d\3\2\2\2\u018d\u018f\3\2\2\2\u018e\u018c\3\2\2\2\u018f\u0199\5\64"+
		"\33\2\u0190\u0192\5 \21\2\u0191\u0190\3\2\2\2\u0192\u0195\3\2\2\2\u0193"+
		"\u0191\3\2\2\2\u0193\u0194\3\2\2\2\u0194\u0196\3\2\2\2\u0195\u0193\3\2"+
		"\2\2\u0196\u0199\5\u008aF\2\u0197\u0199\7{\2\2\u0198\u017e\3\2\2\2\u0198"+
		"\u0185\3\2\2\2\u0198\u018c\3\2\2\2\u0198\u0193\3\2\2\2\u0198\u0197\3\2"+
		"\2\2\u0199\35\3\2\2\2\u019a\u019d\5 \21\2\u019b\u019d\t\3\2\2\u019c\u019a"+
		"\3\2\2\2\u019c\u019b\3\2\2\2\u019d\37\3\2\2\2\u019e\u01a1\5~@\2\u019f"+
		"\u01a1\t\4\2\2\u01a0\u019e\3\2\2\2\u01a0\u019f\3\2\2\2\u01a1!\3\2\2\2"+
		"\u01a2\u01a5\7N\2\2\u01a3\u01a5\5~@\2\u01a4\u01a2\3\2\2\2\u01a4\u01a3"+
		"\3\2\2\2\u01a5#\3\2\2\2\u01a6\u01a7\7E\2\2\u01a7\u01a9\7\u00a0\2\2\u01a8"+
		"\u01aa\5&\24\2\u01a9\u01a8\3\2\2\2\u01a9\u01aa\3\2\2\2\u01aa\u01ad\3\2"+
		"\2\2\u01ab\u01ac\7M\2\2\u01ac\u01ae\5b\62\2\u01ad\u01ab\3\2\2\2\u01ad"+
		"\u01ae\3\2\2\2\u01ae\u01b1\3\2\2\2\u01af\u01b0\7T\2\2\u01b0\u01b2\5\66"+
		"\34\2\u01b1\u01af\3\2\2\2\u01b1\u01b2\3\2\2\2\u01b2\u01b3\3\2\2\2\u01b3"+
		"\u01b4\58\35\2\u01b4%\3\2\2\2\u01b5\u01b6\7\u0080\2\2\u01b6\u01bb\5(\25"+
		"\2\u01b7\u01b8\7|\2\2\u01b8\u01ba\5(\25\2\u01b9\u01b7\3\2\2\2\u01ba\u01bd"+
		"\3\2\2\2\u01bb\u01b9\3\2\2\2\u01bb\u01bc\3\2\2\2\u01bc\u01be\3\2\2\2\u01bd"+
		"\u01bb\3\2\2\2\u01be\u01bf\7\177\2\2\u01bf\'\3\2\2\2\u01c0\u01c3\7\u00a0"+
		"\2\2\u01c1\u01c2\7M\2\2\u01c2\u01c4\5*\26\2\u01c3\u01c1\3\2\2\2\u01c3"+
		"\u01c4\3\2\2\2\u01c4)\3\2\2\2\u01c5\u01ca\5b\62\2\u01c6\u01c7\7\u0091"+
		"\2\2\u01c7\u01c9\5b\62\2\u01c8\u01c6\3\2\2\2\u01c9\u01cc\3\2\2\2\u01ca"+
		"\u01c8\3\2\2\2\u01ca\u01cb\3\2\2\2\u01cb+\3\2\2\2\u01cc\u01ca\3\2\2\2"+
		"\u01cd\u01ce\7L\2\2\u01ce\u01d1\7\u00a0\2\2\u01cf\u01d0\7T\2\2\u01d0\u01d2"+
		"\5\66\34\2\u01d1\u01cf\3\2\2\2\u01d1\u01d2\3\2\2\2\u01d2\u01d3\3\2\2\2"+
		"\u01d3\u01d5\7w\2\2\u01d4\u01d6\5.\30\2\u01d5\u01d4\3\2\2\2\u01d5\u01d6"+
		"\3\2\2\2\u01d6\u01d8\3\2\2\2\u01d7\u01d9\7|\2\2\u01d8\u01d7\3\2\2\2\u01d8"+
		"\u01d9\3\2\2\2\u01d9\u01db\3\2\2\2\u01da\u01dc\5\62\32\2\u01db\u01da\3"+
		"\2\2\2\u01db\u01dc\3\2\2\2\u01dc\u01dd\3\2\2\2\u01dd\u01de\7x\2\2\u01de"+
		"-\3\2\2\2\u01df\u01e4\5\60\31\2\u01e0\u01e1\7|\2\2\u01e1\u01e3\5\60\31"+
		"\2\u01e2\u01e0\3\2\2\2\u01e3\u01e6\3\2\2\2\u01e4\u01e2\3\2\2\2\u01e4\u01e5"+
		"\3\2\2\2\u01e5/\3\2\2\2\u01e6\u01e4\3\2\2\2\u01e7\u01e9\5~@\2\u01e8\u01e7"+
		"\3\2\2\2\u01e9\u01ec\3\2\2\2\u01ea\u01e8\3\2\2\2\u01ea\u01eb\3\2\2\2\u01eb"+
		"\u01ed\3\2\2\2\u01ec\u01ea\3\2\2\2\u01ed\u01ef\7\u00a0\2\2\u01ee\u01f0"+
		"\5\u00dep\2\u01ef\u01ee\3\2\2\2\u01ef\u01f0\3\2\2\2\u01f0\u01f2\3\2\2"+
		"\2\u01f1\u01f3\58\35\2\u01f2\u01f1\3\2\2\2\u01f2\u01f3\3\2\2\2\u01f3\61"+
		"\3\2\2\2\u01f4\u01f8\7{\2\2\u01f5\u01f7\5<\37\2\u01f6\u01f5\3\2\2\2\u01f7"+
		"\u01fa\3\2\2\2\u01f8\u01f6\3\2\2\2\u01f8\u01f9\3\2\2\2\u01f9\63\3\2\2"+
		"\2\u01fa\u01f8\3\2\2\2\u01fb\u01fc\7X\2\2\u01fc\u01fe\7\u00a0\2\2\u01fd"+
		"\u01ff\5&\24\2\u01fe\u01fd\3\2\2\2\u01fe\u01ff\3\2\2\2\u01ff\u0202\3\2"+
		"\2\2\u0200\u0201\7M\2\2\u0201\u0203\5\66\34\2\u0202\u0200\3\2\2\2\u0202"+
		"\u0203\3\2\2\2\u0203\u0204\3\2\2\2\u0204\u0205\5:\36\2\u0205\65\3\2\2"+
		"\2\u0206\u020b\5b\62\2\u0207\u0208\7|\2\2\u0208\u020a\5b\62\2\u0209\u0207"+
		"\3\2\2\2\u020a\u020d\3\2\2\2\u020b\u0209\3\2\2\2\u020b\u020c\3\2\2\2\u020c"+
		"\67\3\2\2\2\u020d\u020b\3\2\2\2\u020e\u0212\7w\2\2\u020f\u0211\5<\37\2"+
		"\u0210\u020f\3\2\2\2\u0211\u0214\3\2\2\2\u0212\u0210\3\2\2\2\u0212\u0213"+
		"\3\2\2\2\u0213\u0215\3\2\2\2\u0214\u0212\3\2\2\2\u0215\u0216\7x\2\2\u0216"+
		"9\3\2\2\2\u0217\u021b\7w\2\2\u0218\u021a\5J&\2\u0219\u0218\3\2\2\2\u021a"+
		"\u021d\3\2\2\2\u021b\u0219\3\2\2\2\u021b\u021c\3\2\2\2\u021c\u021e\3\2"+
		"\2\2\u021d\u021b\3\2\2\2\u021e\u021f\7x\2\2\u021f;\3\2\2\2\u0220\u022d"+
		"\7{\2\2\u0221\u0223\7b\2\2\u0222\u0221\3\2\2\2\u0222\u0223\3\2\2\2\u0223"+
		"\u0224\3\2\2\2\u0224\u022d\5\u009aN\2\u0225\u0227\5\36\20\2\u0226\u0225"+
		"\3\2\2\2\u0227\u022a\3\2\2\2\u0228\u0226\3\2\2\2\u0228\u0229\3\2\2\2\u0229"+
		"\u022b\3\2\2\2\u022a\u0228\3\2\2\2\u022b\u022d\5> \2\u022c\u0220\3\2\2"+
		"\2\u022c\u0222\3\2\2\2\u022c\u0228\3\2\2\2\u022d=\3\2\2\2\u022e\u0238"+
		"\5@!\2\u022f\u0238\5B\"\2\u0230\u0238\5H%\2\u0231\u0238\5D#\2\u0232\u0238"+
		"\5F$\2\u0233\u0238\5\64\33\2\u0234\u0238\5\u008aF\2\u0235\u0238\5$\23"+
		"\2\u0236\u0238\5,\27\2\u0237\u022e\3\2\2\2\u0237\u022f\3\2\2\2\u0237\u0230"+
		"\3\2\2\2\u0237\u0231\3\2\2\2\u0237\u0232\3\2\2\2\u0237\u0233\3\2\2\2\u0237"+
		"\u0234\3\2\2\2\u0237\u0235\3\2\2\2\u0237\u0236\3\2\2\2\u0238?\3\2\2\2"+
		"\u0239\u023c\5b\62\2\u023a\u023c\7l\2\2\u023b\u0239\3\2\2\2\u023b\u023a"+
		"\3\2\2\2\u023c\u023d\3\2\2\2\u023d\u023e\7\u00a0\2\2\u023e\u0243\5n8\2"+
		"\u023f\u0240\7y\2\2\u0240\u0242\7z\2\2\u0241\u023f\3\2\2\2\u0242\u0245"+
		"\3\2\2\2\u0243\u0241\3\2\2\2\u0243\u0244\3\2\2\2\u0244\u0248\3\2\2\2\u0245"+
		"\u0243\3\2\2\2\u0246\u0247\7i\2\2\u0247\u0249\5l\67\2\u0248\u0246\3\2"+
		"\2\2\u0248\u0249\3\2\2\2\u0249\u024c\3\2\2\2\u024a\u024d\5v<\2\u024b\u024d"+
		"\7{\2\2\u024c\u024a\3\2\2\2\u024c\u024b\3\2\2\2\u024dA\3\2\2\2\u024e\u024f"+
		"\5&\24\2\u024f\u0250\5@!\2\u0250C\3\2\2\2\u0251\u0252\7\u00a0\2\2\u0252"+
		"\u0255\5n8\2\u0253\u0254\7i\2\2\u0254\u0256\5l\67\2\u0255\u0253\3\2\2"+
		"\2\u0255\u0256\3\2\2\2\u0256\u0257\3\2\2\2\u0257\u0258\5x=\2\u0258E\3"+
		"\2\2\2\u0259\u025a\5&\24\2\u025a\u025b\5D#\2\u025bG\3\2\2\2\u025c\u025d"+
		"\5b\62\2\u025d\u025e\5V,\2\u025e\u025f\7{\2\2\u025fI\3\2\2\2\u0260\u0262"+
		"\5\36\20\2\u0261\u0260\3\2\2\2\u0262\u0265\3\2\2\2\u0263\u0261\3\2\2\2"+
		"\u0263\u0264\3\2\2\2\u0264\u0266\3\2\2\2\u0265\u0263\3\2\2\2\u0266\u0269"+
		"\5L\'\2\u0267\u0269\7{\2\2\u0268\u0263\3\2\2\2\u0268\u0267\3\2\2\2\u0269"+
		"K\3\2\2\2\u026a\u0272\5N(\2\u026b\u0272\5R*\2\u026c\u0272\5T+\2\u026d"+
		"\u0272\5\64\33\2\u026e\u0272\5\u008aF\2\u026f\u0272\5$\23\2\u0270\u0272"+
		"\5,\27\2\u0271\u026a\3\2\2\2\u0271\u026b\3\2\2\2\u0271\u026c\3\2\2\2\u0271"+
		"\u026d\3\2\2\2\u0271\u026e\3\2\2\2\u0271\u026f\3\2\2\2\u0271\u0270\3\2"+
		"\2\2\u0272M\3\2\2\2\u0273\u0274\5b\62\2\u0274\u0279\5P)\2\u0275\u0276"+
		"\7|\2\2\u0276\u0278\5P)\2\u0277\u0275\3\2\2\2\u0278\u027b\3\2\2\2\u0279"+
		"\u0277\3\2\2\2\u0279\u027a\3\2\2\2\u027a\u027c\3\2\2\2\u027b\u0279\3\2"+
		"\2\2\u027c\u027d\7{\2\2\u027dO\3\2\2\2\u027e\u0283\7\u00a0\2\2\u027f\u0280"+
		"\7y\2\2\u0280\u0282\7z\2\2\u0281\u027f\3\2\2\2\u0282\u0285\3\2\2\2\u0283"+
		"\u0281\3\2\2\2\u0283\u0284\3\2\2\2\u0284\u0286\3\2\2\2\u0285\u0283\3\2"+
		"\2\2\u0286\u0287\7~\2\2\u0287\u0288\5\\/\2\u0288Q\3\2\2\2\u0289\u028c"+
		"\5b\62\2\u028a\u028c\7l\2\2\u028b\u0289\3\2\2\2\u028b\u028a\3\2\2\2\u028c"+
		"\u028d\3\2\2\2\u028d\u028e\7\u00a0\2\2\u028e\u0293\5n8\2\u028f\u0290\7"+
		"y\2\2\u0290\u0292\7z\2\2\u0291\u028f\3\2\2\2\u0292\u0295\3\2\2\2\u0293"+
		"\u0291\3\2\2\2\u0293\u0294\3\2\2\2\u0294\u0298\3\2\2\2\u0295\u0293\3\2"+
		"\2\2\u0296\u0297\7i\2\2\u0297\u0299\5l\67\2\u0298\u0296\3\2\2\2\u0298"+
		"\u0299\3\2\2\2\u0299\u029a\3\2\2\2\u029a\u029b\7{\2\2\u029bS\3\2\2\2\u029c"+
		"\u029d\5&\24\2\u029d\u029e\5R*\2\u029eU\3\2\2\2\u029f\u02a4\5X-\2\u02a0"+
		"\u02a1\7|\2\2\u02a1\u02a3\5X-\2\u02a2\u02a0\3\2\2\2\u02a3\u02a6\3\2\2"+
		"\2\u02a4\u02a2\3\2\2\2\u02a4\u02a5\3\2\2\2\u02a5W\3\2\2\2\u02a6\u02a4"+
		"\3\2\2\2\u02a7\u02aa\5Z.\2\u02a8\u02a9\7~\2\2\u02a9\u02ab\5\\/\2\u02aa"+
		"\u02a8\3\2\2\2\u02aa\u02ab\3\2\2\2\u02abY\3\2\2\2\u02ac\u02b1\7\u00a0"+
		"\2\2\u02ad\u02ae\7y\2\2\u02ae\u02b0\7z\2\2\u02af\u02ad\3\2\2\2\u02b0\u02b3"+
		"\3\2\2\2\u02b1\u02af\3\2\2\2\u02b1\u02b2\3\2\2\2\u02b2[\3\2\2\2\u02b3"+
		"\u02b1\3\2\2\2\u02b4\u02b7\5^\60\2\u02b5\u02b7\5\u00c4c\2\u02b6\u02b4"+
		"\3\2\2\2\u02b6\u02b5\3\2\2\2\u02b7]\3\2\2\2\u02b8\u02c4\7w\2\2\u02b9\u02be"+
		"\5\\/\2\u02ba\u02bb\7|\2\2\u02bb\u02bd\5\\/\2\u02bc\u02ba\3\2\2\2\u02bd"+
		"\u02c0\3\2\2\2\u02be\u02bc\3\2\2\2\u02be\u02bf\3\2\2\2\u02bf\u02c2\3\2"+
		"\2\2\u02c0\u02be\3\2\2\2\u02c1\u02c3\7|\2\2\u02c2\u02c1\3\2\2\2\u02c2"+
		"\u02c3\3\2\2\2\u02c3\u02c5\3\2\2\2\u02c4\u02b9\3\2\2\2\u02c4\u02c5\3\2"+
		"\2\2\u02c5\u02c6\3\2\2\2\u02c6\u02c7\7x\2\2\u02c7_\3\2\2\2\u02c8\u02c9"+
		"\7\u00a0\2\2\u02c9a\3\2\2\2\u02ca\u02cf\5d\63\2\u02cb\u02cc\7y\2\2\u02cc"+
		"\u02ce\7z\2\2\u02cd\u02cb\3\2\2\2\u02ce\u02d1\3\2\2\2\u02cf\u02cd\3\2"+
		"\2\2\u02cf\u02d0\3\2\2\2\u02d0\u02db\3\2\2\2\u02d1\u02cf\3\2\2\2\u02d2"+
		"\u02d7\5f\64\2\u02d3\u02d4\7y\2\2\u02d4\u02d6\7z\2\2\u02d5\u02d3\3\2\2"+
		"\2\u02d6\u02d9\3\2\2\2\u02d7\u02d5\3\2\2\2\u02d7\u02d8\3\2\2\2\u02d8\u02db"+
		"\3\2\2\2\u02d9\u02d7\3\2\2\2\u02da\u02ca\3\2\2\2\u02da\u02d2\3\2\2\2\u02db"+
		"c\3\2\2\2\u02dc\u02de\7\u00a0\2\2\u02dd\u02df\5h\65\2\u02de\u02dd\3\2"+
		"\2\2\u02de\u02df\3\2\2\2\u02df\u02e7\3\2\2\2\u02e0\u02e1\7}\2\2\u02e1"+
		"\u02e3\7\u00a0\2\2\u02e2\u02e4\5h\65\2\u02e3\u02e2\3\2\2\2\u02e3\u02e4"+
		"\3\2\2\2\u02e4\u02e6\3\2\2\2\u02e5\u02e0\3\2\2\2\u02e6\u02e9\3\2\2\2\u02e7"+
		"\u02e5\3\2\2\2\u02e7\u02e8\3\2\2\2\u02e8e\3\2\2\2\u02e9\u02e7\3\2\2\2"+
		"\u02ea\u02eb\t\5\2\2\u02ebg\3\2\2\2\u02ec\u02ed\7\u0080\2\2\u02ed\u02f2"+
		"\5j\66\2\u02ee\u02ef\7|\2\2\u02ef\u02f1\5j\66\2\u02f0\u02ee\3\2\2\2\u02f1"+
		"\u02f4\3\2\2\2\u02f2\u02f0\3\2\2\2\u02f2\u02f3\3\2\2\2\u02f3\u02f5\3\2"+
		"\2\2\u02f4\u02f2\3\2\2\2\u02f5\u02f6\7\177\2\2\u02f6i\3\2\2\2\u02f7\u02fe"+
		"\5b\62\2\u02f8\u02fb\7\u0083\2\2\u02f9\u02fa\t\6\2\2\u02fa\u02fc\5b\62"+
		"\2\u02fb\u02f9\3\2\2\2\u02fb\u02fc\3\2\2\2\u02fc\u02fe\3\2\2\2\u02fd\u02f7"+
		"\3\2\2\2\u02fd\u02f8\3\2\2\2\u02fek\3\2\2\2\u02ff\u0304\5z>\2\u0300\u0301"+
		"\7|\2\2\u0301\u0303\5z>\2\u0302\u0300\3\2\2\2\u0303\u0306\3\2\2\2\u0304"+
		"\u0302\3\2\2\2\u0304\u0305\3\2\2\2\u0305m\3\2\2\2\u0306\u0304\3\2\2\2"+
		"\u0307\u0309\7u\2\2\u0308\u030a\5p9\2\u0309\u0308\3\2\2\2\u0309\u030a"+
		"\3\2\2\2\u030a\u030b\3\2\2\2\u030b\u030c\7v\2\2\u030co\3\2\2\2\u030d\u0312"+
		"\5r:\2\u030e\u030f\7|\2\2\u030f\u0311\5r:\2\u0310\u030e\3\2\2\2\u0311"+
		"\u0314\3\2\2\2\u0312\u0310\3\2\2\2\u0312\u0313\3\2\2\2\u0313\u0317\3\2"+
		"\2\2\u0314\u0312\3\2\2\2\u0315\u0316\7|\2\2\u0316\u0318\5t;\2\u0317\u0315"+
		"\3\2\2\2\u0317\u0318\3\2\2\2\u0318\u031b\3\2\2\2\u0319\u031b\5t;\2\u031a"+
		"\u030d\3\2\2\2\u031a\u0319\3\2\2\2\u031bq\3\2\2\2\u031c\u031e\5\"\22\2"+
		"\u031d\u031c\3\2\2\2\u031e\u0321\3\2\2\2\u031f\u031d\3\2\2\2\u031f\u0320"+
		"\3\2\2\2\u0320\u0322\3\2\2\2\u0321\u031f\3\2\2\2\u0322\u0323\5b\62\2\u0323"+
		"\u0324\5Z.\2\u0324s\3\2\2\2\u0325\u0327\5\"\22\2\u0326\u0325\3\2\2\2\u0327"+
		"\u032a\3\2\2\2\u0328\u0326\3\2\2\2\u0328\u0329\3\2\2\2\u0329\u032b\3\2"+
		"\2\2\u032a\u0328\3\2\2\2\u032b\u032c\5b\62\2\u032c\u032d\7\u00a1\2\2\u032d"+
		"\u032e\5Z.\2\u032eu\3\2\2\2\u032f\u0330\5\u009aN\2\u0330w\3\2\2\2\u0331"+
		"\u0332\5\u009aN\2\u0332y\3\2\2\2\u0333\u0338\7\u00a0\2\2\u0334\u0335\7"+
		"}\2\2\u0335\u0337\7\u00a0\2\2\u0336\u0334\3\2\2\2\u0337\u033a\3\2\2\2"+
		"\u0338\u0336\3\2\2\2\u0338\u0339\3\2\2\2\u0339{\3\2\2\2\u033a\u0338\3"+
		"\2\2\2\u033b\u033c\t\7\2\2\u033c}\3\2\2\2\u033d\u033e\7<\2\2\u033e\u0345"+
		"\5\u0080A\2\u033f\u0342\7u\2\2\u0340\u0343\5\u0082B\2\u0341\u0343\5\u0086"+
		"D\2\u0342\u0340\3\2\2\2\u0342\u0341\3\2\2\2\u0342\u0343\3\2\2\2\u0343"+
		"\u0344\3\2\2\2\u0344\u0346\7v\2\2\u0345\u033f\3\2\2\2\u0345\u0346\3\2"+
		"\2\2\u0346\177\3\2\2\2\u0347\u0348\5z>\2\u0348\u0081\3\2\2\2\u0349\u034e"+
		"\5\u0084C\2\u034a\u034b\7|\2\2\u034b\u034d\5\u0084C\2\u034c\u034a\3\2"+
		"\2\2\u034d\u0350\3\2\2\2\u034e\u034c\3\2\2\2\u034e\u034f\3\2\2\2\u034f"+
		"\u0083\3\2\2\2\u0350\u034e\3\2\2\2\u0351\u0352\7\u00a0\2\2\u0352\u0353"+
		"\7~\2\2\u0353\u0354\5\u0086D\2\u0354\u0085\3\2\2\2\u0355\u0359\5\u00c4"+
		"c\2\u0356\u0359\5~@\2\u0357\u0359\5\u0088E\2\u0358\u0355\3\2\2\2\u0358"+
		"\u0356\3\2\2\2\u0358\u0357\3\2\2\2\u0359\u0087\3\2\2\2\u035a\u0363\7w"+
		"\2\2\u035b\u0360\5\u0086D\2\u035c\u035d\7|\2\2\u035d\u035f\5\u0086D\2"+
		"\u035e\u035c\3\2\2\2\u035f\u0362\3\2\2\2\u0360\u035e\3\2\2\2\u0360\u0361"+
		"\3\2\2\2\u0361\u0364\3\2\2\2\u0362\u0360\3\2\2\2\u0363\u035b\3\2\2\2\u0363"+
		"\u0364\3\2\2\2\u0364\u0366\3\2\2\2\u0365\u0367\7|\2\2\u0366\u0365\3\2"+
		"\2\2\u0366\u0367\3\2\2\2\u0367\u0368\3\2\2\2\u0368\u0369\7x\2\2\u0369"+
		"\u0089\3\2\2\2\u036a\u036b\7<\2\2\u036b\u036c\7X\2\2\u036c\u036d\7\u00a0"+
		"\2\2\u036d\u036e\5\u008cG\2\u036e\u008b\3\2\2\2\u036f\u0373\7w\2\2\u0370"+
		"\u0372\5\u008eH\2\u0371\u0370\3\2\2\2\u0372\u0375\3\2\2\2\u0373\u0371"+
		"\3\2\2\2\u0373\u0374\3\2\2\2\u0374\u0376\3\2\2\2\u0375\u0373\3\2\2\2\u0376"+
		"\u0377\7x\2\2\u0377\u008d\3\2\2\2\u0378\u037a\5\36\20\2\u0379\u0378\3"+
		"\2\2\2\u037a\u037d\3\2\2\2\u037b\u0379\3\2\2\2\u037b\u037c\3\2\2\2\u037c"+
		"\u037e\3\2\2\2\u037d\u037b\3\2\2\2\u037e\u0381\5\u0090I\2\u037f\u0381"+
		"\7{\2\2\u0380\u037b\3\2\2\2\u0380\u037f\3\2\2\2\u0381\u008f\3\2\2\2\u0382"+
		"\u0383\5b\62\2\u0383\u0384\5\u0092J\2\u0384\u0385\7{\2\2\u0385\u0397\3"+
		"\2\2\2\u0386\u0388\5$\23\2\u0387\u0389\7{\2\2\u0388\u0387\3\2\2\2\u0388"+
		"\u0389\3\2\2\2\u0389\u0397\3\2\2\2\u038a\u038c\5\64\33\2\u038b\u038d\7"+
		"{\2\2\u038c\u038b\3\2\2\2\u038c\u038d\3\2\2\2\u038d\u0397\3\2\2\2\u038e"+
		"\u0390\5,\27\2\u038f\u0391\7{\2\2\u0390\u038f\3\2\2\2\u0390\u0391\3\2"+
		"\2\2\u0391\u0397\3\2\2\2\u0392\u0394\5\u008aF\2\u0393\u0395\7{\2\2\u0394"+
		"\u0393\3\2\2\2\u0394\u0395\3\2\2\2\u0395\u0397\3\2\2\2\u0396\u0382\3\2"+
		"\2\2\u0396\u0386\3\2\2\2\u0396\u038a\3\2\2\2\u0396\u038e\3\2\2\2\u0396"+
		"\u0392\3\2\2\2\u0397\u0091\3\2\2\2\u0398\u039b\5\u0094K\2\u0399\u039b"+
		"\5\u0096L\2\u039a\u0398\3\2\2\2\u039a\u0399\3\2\2\2\u039b\u0093\3\2\2"+
		"\2\u039c\u039d\7\u00a0\2\2\u039d\u039e\7u\2\2\u039e\u03a0\7v\2\2\u039f"+
		"\u03a1\5\u0098M\2\u03a0\u039f\3\2\2\2\u03a0\u03a1\3\2\2\2\u03a1\u0095"+
		"\3\2\2\2\u03a2\u03a3\5V,\2\u03a3\u0097\3\2\2\2\u03a4\u03a5\7H\2\2\u03a5"+
		"\u03a6\5\u0086D\2\u03a6\u0099\3\2\2\2\u03a7\u03ab\7w\2\2\u03a8\u03aa\5"+
		"\u009cO\2\u03a9\u03a8\3\2\2\2\u03aa\u03ad\3\2\2\2\u03ab\u03a9\3\2\2\2"+
		"\u03ab\u03ac\3\2\2\2\u03ac\u03ae\3\2\2\2\u03ad\u03ab\3\2\2\2\u03ae\u03af"+
		"\7x\2\2\u03af\u009b\3\2\2\2\u03b0\u03b4\5\u009eP\2\u03b1\u03b4\5\u00a2"+
		"R\2\u03b2\u03b4\5\34\17\2\u03b3\u03b0\3\2\2\2\u03b3\u03b1\3\2\2\2\u03b3"+
		"\u03b2\3\2\2\2\u03b4\u009d\3\2\2\2\u03b5\u03b6\5\u00a0Q\2\u03b6\u03b7"+
		"\7{\2\2\u03b7\u009f\3\2\2\2\u03b8\u03ba\5\"\22\2\u03b9\u03b8\3\2\2\2\u03ba"+
		"\u03bd\3\2\2\2\u03bb\u03b9\3\2\2\2\u03bb\u03bc\3\2\2\2\u03bc\u03be\3\2"+
		"\2\2\u03bd\u03bb\3\2\2\2\u03be\u03bf\5b\62\2\u03bf\u03c0\5V,\2\u03c0\u00a1"+
		"\3\2\2\2\u03c1\u042a\5\u009aN\2\u03c2\u03c3\7>\2\2\u03c3\u03c6\5\u00c4"+
		"c\2\u03c4\u03c5\7\u0084\2\2\u03c5\u03c7\5\u00c4c\2\u03c6\u03c4\3\2\2\2"+
		"\u03c6\u03c7\3\2\2\2\u03c7\u03c8\3\2\2\2\u03c8\u03c9\7{\2\2\u03c9\u042a"+
		"\3\2\2\2\u03ca\u03cb\7R\2\2\u03cb\u03cc\5\u00bc_\2\u03cc\u03cf\5\u00a2"+
		"R\2\u03cd\u03ce\7K\2\2\u03ce\u03d0\5\u00a2R\2\u03cf\u03cd\3\2\2\2\u03cf"+
		"\u03d0\3\2\2\2\u03d0\u042a\3\2\2\2\u03d1\u03d2\7Q\2\2\u03d2\u03d3\7u\2"+
		"\2\u03d3\u03d4\5\u00b4[\2\u03d4\u03d5\7v\2\2\u03d5\u03d6\5\u00a2R\2\u03d6"+
		"\u042a\3\2\2\2\u03d7\u03d8\7n\2\2\u03d8\u03d9\5\u00bc_\2\u03d9\u03da\5"+
		"\u00a2R\2\u03da\u042a\3\2\2\2\u03db\u03dc\7I\2\2\u03dc\u03dd\5\u00a2R"+
		"\2\u03dd\u03de\7n\2\2\u03de\u03df\5\u00bc_\2\u03df\u03e0\7{\2\2\u03e0"+
		"\u042a\3\2\2\2\u03e1\u03e2\7k\2\2\u03e2\u03ec\5\u009aN\2\u03e3\u03e5\5"+
		"\u00a4S\2\u03e4\u03e3\3\2\2\2\u03e5\u03e6\3\2\2\2\u03e6\u03e4\3\2\2\2"+
		"\u03e6\u03e7\3\2\2\2\u03e7\u03e9\3\2\2\2\u03e8\u03ea\5\u00a8U\2\u03e9"+
		"\u03e8\3\2\2\2\u03e9\u03ea\3\2\2\2\u03ea\u03ed\3\2\2\2\u03eb\u03ed\5\u00a8"+
		"U\2\u03ec\u03e4\3\2\2\2\u03ec\u03eb\3\2\2\2\u03ed\u042a\3\2\2\2\u03ee"+
		"\u03ef\7k\2\2\u03ef\u03f0\5\u00aaV\2\u03f0\u03f4\5\u009aN\2\u03f1\u03f3"+
		"\5\u00a4S\2\u03f2\u03f1\3\2\2\2\u03f3\u03f6\3\2\2\2\u03f4\u03f2\3\2\2"+
		"\2\u03f4\u03f5\3\2\2\2\u03f5\u03f8\3\2\2\2\u03f6\u03f4\3\2\2\2\u03f7\u03f9"+
		"\5\u00a8U\2\u03f8\u03f7\3\2\2\2\u03f8\u03f9\3\2\2\2\u03f9\u042a\3\2\2"+
		"\2\u03fa\u03fb\7e\2\2\u03fb\u03fc\5\u00bc_\2\u03fc\u0400\7w\2\2\u03fd"+
		"\u03ff\5\u00b0Y\2\u03fe\u03fd\3\2\2\2\u03ff\u0402\3\2\2\2\u0400\u03fe"+
		"\3\2\2\2\u0400\u0401\3\2\2\2\u0401\u0406\3\2\2\2\u0402\u0400\3\2\2\2\u0403"+
		"\u0405\5\u00b2Z\2\u0404\u0403\3\2\2\2\u0405\u0408\3\2\2\2\u0406\u0404"+
		"\3\2\2\2\u0406\u0407\3\2\2\2\u0407\u0409\3\2\2\2\u0408\u0406\3\2\2\2\u0409"+
		"\u040a\7x\2\2\u040a\u042a\3\2\2\2\u040b\u040c\7f\2\2\u040c\u040d\5\u00bc"+
		"_\2\u040d\u040e\5\u009aN\2\u040e\u042a\3\2\2\2\u040f\u0411\7`\2\2\u0410"+
		"\u0412\5\u00c4c\2\u0411\u0410\3\2\2\2\u0411\u0412\3\2\2\2\u0412\u0413"+
		"\3\2\2\2\u0413\u042a\7{\2\2\u0414\u0415\7h\2\2\u0415\u0416\5\u00c4c\2"+
		"\u0416\u0417\7{\2\2\u0417\u042a\3\2\2\2\u0418\u041a\7@\2\2\u0419\u041b"+
		"\7\u00a0\2\2\u041a\u0419\3\2\2\2\u041a\u041b\3\2\2\2\u041b\u041c\3\2\2"+
		"\2\u041c\u042a\7{\2\2\u041d\u041f\7G\2\2\u041e\u0420\7\u00a0\2\2\u041f"+
		"\u041e\3\2\2\2\u041f\u0420\3\2\2\2\u0420\u0421\3\2\2\2\u0421\u042a\7{"+
		"\2\2\u0422\u042a\7{\2\2\u0423\u0424\5\u00c0a\2\u0424\u0425\7{\2\2\u0425"+
		"\u042a\3\2\2\2\u0426\u0427\7\u00a0\2\2\u0427\u0428\7\u0084\2\2\u0428\u042a"+
		"\5\u00a2R\2\u0429\u03c1\3\2\2\2\u0429\u03c2\3\2\2\2\u0429\u03ca\3\2\2"+
		"\2\u0429\u03d1\3\2\2\2\u0429\u03d7\3\2\2\2\u0429\u03db\3\2\2\2\u0429\u03e1"+
		"\3\2\2\2\u0429\u03ee\3\2\2\2\u0429\u03fa\3\2\2\2\u0429\u040b\3\2\2\2\u0429"+
		"\u040f\3\2\2\2\u0429\u0414\3\2\2\2\u0429\u0418\3\2\2\2\u0429\u041d\3\2"+
		"\2\2\u0429\u0422\3\2\2\2\u0429\u0423\3\2\2\2\u0429\u0426\3\2\2\2\u042a"+
		"\u00a3\3\2\2\2\u042b\u042c\7C\2\2\u042c\u0430\7u\2\2\u042d\u042f\5\"\22"+
		"\2\u042e\u042d\3\2\2\2\u042f\u0432\3\2\2\2\u0430\u042e\3\2\2\2\u0430\u0431"+
		"\3\2\2\2\u0431\u0433\3\2\2\2\u0432\u0430\3\2\2\2\u0433\u0434\5\u00a6T"+
		"\2\u0434\u0435\7\u00a0\2\2\u0435\u0436\7v\2\2\u0436\u0437\5\u009aN\2\u0437"+
		"\u00a5\3\2\2\2\u0438\u043d\5z>\2\u0439\u043a\7\u0092\2\2\u043a\u043c\5"+
		"z>\2\u043b\u0439\3\2\2\2\u043c\u043f\3\2\2\2\u043d\u043b\3\2\2\2\u043d"+
		"\u043e\3\2\2\2\u043e\u00a7\3\2\2\2\u043f\u043d\3\2\2\2\u0440\u0441\7O"+
		"\2\2\u0441\u0442\5\u009aN\2\u0442\u00a9\3\2\2\2\u0443\u0444\7u\2\2\u0444"+
		"\u0446\5\u00acW\2\u0445\u0447\7{\2\2\u0446\u0445\3\2\2\2\u0446\u0447\3"+
		"\2\2\2\u0447\u0448\3\2\2\2\u0448\u0449\7v\2\2\u0449\u00ab\3\2\2\2\u044a"+
		"\u044f\5\u00aeX\2\u044b\u044c\7{\2\2\u044c\u044e\5\u00aeX\2\u044d\u044b"+
		"\3\2\2\2\u044e\u0451\3\2\2\2\u044f\u044d\3\2\2\2\u044f\u0450\3\2\2\2\u0450"+
		"\u00ad\3\2\2\2\u0451\u044f\3\2\2\2\u0452\u0454\5\"\22\2\u0453\u0452\3"+
		"\2\2\2\u0454\u0457\3\2\2\2\u0455\u0453\3\2\2\2\u0455\u0456\3\2\2\2\u0456"+
		"\u0458\3\2\2\2\u0457\u0455\3\2\2\2\u0458\u0459\5d\63\2\u0459\u045a\5Z"+
		".\2\u045a\u045b\7~\2\2\u045b\u045c\5\u00c4c\2\u045c\u00af\3\2\2\2\u045d"+
		"\u045f\5\u00b2Z\2\u045e\u045d\3\2\2\2\u045f\u0460\3\2\2\2\u0460\u045e"+
		"\3\2\2\2\u0460\u0461\3\2\2\2\u0461\u0463\3\2\2\2\u0462\u0464\5\u009cO"+
		"\2\u0463\u0462\3\2\2\2\u0464\u0465\3\2\2\2\u0465\u0463\3\2\2\2\u0465\u0466"+
		"\3\2\2\2\u0466\u00b1\3\2\2\2\u0467\u0468\7B\2\2\u0468\u0469\5\u00c2b\2"+
		"\u0469\u046a\7\u0084\2\2\u046a\u0472\3\2\2\2\u046b\u046c\7B\2\2\u046c"+
		"\u046d\5`\61\2\u046d\u046e\7\u0084\2\2\u046e\u0472\3\2\2\2\u046f\u0470"+
		"\7H\2\2\u0470\u0472\7\u0084\2\2\u0471\u0467\3\2\2\2\u0471\u046b\3\2\2"+
		"\2\u0471\u046f\3\2\2\2\u0472\u00b3\3\2\2\2\u0473\u0480\5\u00b8]\2\u0474"+
		"\u0476\5\u00b6\\\2\u0475\u0474\3\2\2\2\u0475\u0476\3\2\2\2\u0476\u0477"+
		"\3\2\2\2\u0477\u0479\7{\2\2\u0478\u047a\5\u00c4c\2\u0479\u0478\3\2\2\2"+
		"\u0479\u047a\3\2\2\2\u047a\u047b\3\2\2\2\u047b\u047d\7{\2\2\u047c\u047e"+
		"\5\u00ba^\2\u047d\u047c\3\2\2\2\u047d\u047e\3\2\2\2\u047e\u0480\3\2\2"+
		"\2\u047f\u0473\3\2\2\2\u047f\u0475\3\2\2\2\u0480\u00b5\3\2\2\2\u0481\u0484"+
		"\5\u00a0Q\2\u0482\u0484\5\u00be`\2\u0483\u0481\3\2\2\2\u0483\u0482\3\2"+
		"\2\2\u0484\u00b7\3\2\2\2\u0485\u0487\5\"\22\2\u0486\u0485\3\2\2\2\u0487"+
		"\u048a\3\2\2\2\u0488\u0486\3\2\2\2\u0488\u0489\3\2\2\2\u0489\u048b\3\2"+
		"\2\2\u048a\u0488\3\2\2\2\u048b\u048c\5b\62\2\u048c\u048d\5Z.\2\u048d\u048e"+
		"\7\u0084\2\2\u048e\u048f\5\u00c4c\2\u048f\u00b9\3\2\2\2\u0490\u0491\5"+
		"\u00be`\2\u0491\u00bb\3\2\2\2\u0492\u0493\7u\2\2\u0493\u0494\5\u00c4c"+
		"\2\u0494\u0495\7v\2\2\u0495\u00bd\3\2\2\2\u0496\u049b\5\u00c4c\2\u0497"+
		"\u0498\7|\2\2\u0498\u049a\5\u00c4c\2\u0499\u0497\3\2\2\2\u049a\u049d\3"+
		"\2\2\2\u049b\u0499\3\2\2\2\u049b\u049c\3\2\2\2\u049c\u00bf\3\2\2\2\u049d"+
		"\u049b\3\2\2\2\u049e\u049f\5\u00c4c\2\u049f\u00c1\3\2\2\2\u04a0\u04a1"+
		"\5\u00c4c\2\u04a1\u00c3\3\2\2\2\u04a2\u04a3\bc\1\2\u04a3\u04a4\7u\2\2"+
		"\u04a4\u04a5\5b\62\2\u04a5\u04a6\7v\2\2\u04a6\u04a7\5\u00c4c\23\u04a7"+
		"\u04b0\3\2\2\2\u04a8\u04a9\t\b\2\2\u04a9\u04b0\5\u00c4c\21\u04aa\u04ab"+
		"\t\t\2\2\u04ab\u04b0\5\u00c4c\20\u04ac\u04b0\5\u00c6d\2\u04ad\u04ae\7"+
		"[\2\2\u04ae\u04b0\5\u00c8e\2\u04af\u04a2\3\2\2\2\u04af\u04a8\3\2\2\2\u04af"+
		"\u04aa\3\2\2\2\u04af\u04ac\3\2\2\2\u04af\u04ad\3\2\2\2\u04b0\u0506\3\2"+
		"\2\2\u04b1\u04b2\f\17\2\2\u04b2\u04b3\t\n\2\2\u04b3\u0505\5\u00c4c\20"+
		"\u04b4\u04b5\f\16\2\2\u04b5\u04b6\t\13\2\2\u04b6\u0505\5\u00c4c\17\u04b7"+
		"\u04bf\f\r\2\2\u04b8\u04b9\7\u0080\2\2\u04b9\u04c0\7\u0080\2\2\u04ba\u04bb"+
		"\7\177\2\2\u04bb\u04bc\7\177\2\2\u04bc\u04c0\7\177\2\2\u04bd\u04be\7\177"+
		"\2\2\u04be\u04c0\7\177\2\2\u04bf\u04b8\3\2\2\2\u04bf\u04ba\3\2\2\2\u04bf"+
		"\u04bd\3\2\2\2\u04c0\u04c1\3\2\2\2\u04c1\u0505\5\u00c4c\16\u04c2\u04c3"+
		"\f\f\2\2\u04c3\u04c4\t\f\2\2\u04c4\u0505\5\u00c4c\r\u04c5\u04c6\f\n\2"+
		"\2\u04c6\u04c7\t\r\2\2\u04c7\u0505\5\u00c4c\13\u04c8\u04c9\f\t\2\2\u04c9"+
		"\u04ca\7\u0091\2\2\u04ca\u0505\5\u00c4c\n\u04cb\u04cc\f\b\2\2\u04cc\u04cd"+
		"\7\u0093\2\2\u04cd\u0505\5\u00c4c\t\u04ce\u04cf\f\7\2\2\u04cf\u04d0\7"+
		"\u0092\2\2\u04d0\u0505\5\u00c4c\b\u04d1\u04d2\f\6\2\2\u04d2\u04d3\7\u0089"+
		"\2\2\u04d3\u0505\5\u00c4c\7\u04d4\u04d5\f\5\2\2\u04d5\u04d6\7\u008a\2"+
		"\2\u04d6\u0505\5\u00c4c\6\u04d7\u04d8\f\4\2\2\u04d8\u04d9\7\u0083\2\2"+
		"\u04d9\u04da\5\u00c4c\2\u04da\u04db\7\u0084\2\2\u04db\u04dc\5\u00c4c\5"+
		"\u04dc\u0505\3\2\2\2\u04dd\u04de\f\3\2\2\u04de\u04df\t\16\2\2\u04df\u0505"+
		"\5\u00c4c\3\u04e0\u04e1\f\33\2\2\u04e1\u04e2\7}\2\2\u04e2\u0505\7\u00a0"+
		"\2\2\u04e3\u04e4\f\32\2\2\u04e4\u04e5\7}\2\2\u04e5\u0505\7g\2\2\u04e6"+
		"\u04e7\f\31\2\2\u04e7\u04e8\7}\2\2\u04e8\u04ea\7[\2\2\u04e9\u04eb\5\u00d4"+
		"k\2\u04ea\u04e9\3\2\2\2\u04ea\u04eb\3\2\2\2\u04eb\u04ec\3\2\2\2\u04ec"+
		"\u0505\5\u00ccg\2\u04ed\u04ee\f\30\2\2\u04ee\u04ef\7}\2\2\u04ef\u04f0"+
		"\7d\2\2\u04f0\u0505\5\u00dan\2\u04f1\u04f2\f\27\2\2\u04f2\u04f3\7}\2\2"+
		"\u04f3\u0505\5\u00d2j\2\u04f4\u04f5\f\26\2\2\u04f5\u04f6\7y\2\2\u04f6"+
		"\u04f7\5\u00c4c\2\u04f7\u04f8\7z\2\2\u04f8\u0505\3\2\2\2\u04f9\u04fa\f"+
		"\25\2\2\u04fa\u04fc\7u\2\2\u04fb\u04fd\5\u00be`\2\u04fc\u04fb\3\2\2\2"+
		"\u04fc\u04fd\3\2\2\2\u04fd\u04fe\3\2\2\2\u04fe\u0505\7v\2\2\u04ff\u0500"+
		"\f\22\2\2\u0500\u0505\t\17\2\2\u0501\u0502\f\13\2\2\u0502\u0503\7V\2\2"+
		"\u0503\u0505\5b\62\2\u0504\u04b1\3\2\2\2\u0504\u04b4\3\2\2\2\u0504\u04b7"+
		"\3\2\2\2\u0504\u04c2\3\2\2\2\u0504\u04c5\3\2\2\2\u0504\u04c8\3\2\2\2\u0504"+
		"\u04cb\3\2\2\2\u0504\u04ce\3\2\2\2\u0504\u04d1\3\2\2\2\u0504\u04d4\3\2"+
		"\2\2\u0504\u04d7\3\2\2\2\u0504\u04dd\3\2\2\2\u0504\u04e0\3\2\2\2\u0504"+
		"\u04e3\3\2\2\2\u0504\u04e6\3\2\2\2\u0504\u04ed\3\2\2\2\u0504\u04f1\3\2"+
		"\2\2\u0504\u04f4\3\2\2\2\u0504\u04f9\3\2\2\2\u0504\u04ff\3\2\2\2\u0504"+
		"\u0501\3\2\2\2\u0505\u0508\3\2\2\2\u0506\u0504\3\2\2\2\u0506\u0507\3\2"+
		"\2\2\u0507\u00c5\3\2\2\2\u0508\u0506\3\2\2\2\u0509\u050a\7u\2\2\u050a"+
		"\u050b\5\u00c4c\2\u050b\u050c\7v\2\2\u050c\u051f\3\2\2\2\u050d\u051f\7"+
		"g\2\2\u050e\u051f\7d\2\2\u050f\u051f\5|?\2\u0510\u051f\7\u00a0\2\2\u0511"+
		"\u0512\5b\62\2\u0512\u0513\7}\2\2\u0513\u0514\7E\2\2\u0514\u051f\3\2\2"+
		"\2\u0515\u0516\7l\2\2\u0516\u0517\7}\2\2\u0517\u051f\7E\2\2\u0518\u051c"+
		"\5\u00d4k\2\u0519\u051d\5\u00dco\2\u051a\u051b\7g\2\2\u051b\u051d\5\u00de"+
		"p\2\u051c\u0519\3\2\2\2\u051c\u051a\3\2\2\2\u051d\u051f\3\2\2\2\u051e"+
		"\u0509\3\2\2\2\u051e\u050d\3\2\2\2\u051e\u050e\3\2\2\2\u051e\u050f\3\2"+
		"\2\2\u051e\u0510\3\2\2\2\u051e\u0511\3\2\2\2\u051e\u0515\3\2\2\2\u051e"+
		"\u0518\3\2\2\2\u051f\u00c7\3\2\2\2\u0520\u0521\5\u00d4k\2\u0521\u0522"+
		"\5\u00caf\2\u0522\u0523\5\u00d0i\2\u0523\u052a\3\2\2\2\u0524\u0527\5\u00ca"+
		"f\2\u0525\u0528\5\u00ceh\2\u0526\u0528\5\u00d0i\2\u0527\u0525\3\2\2\2"+
		"\u0527\u0526\3\2\2\2\u0528\u052a\3\2\2\2\u0529\u0520\3\2\2\2\u0529\u0524"+
		"\3\2\2\2\u052a\u00c9\3\2\2\2\u052b\u052d\7\u00a0\2\2\u052c\u052e\5\u00d6"+
		"l\2\u052d\u052c\3\2\2\2\u052d\u052e\3\2\2\2\u052e\u0536\3\2\2\2\u052f"+
		"\u0530\7}\2\2\u0530\u0532\7\u00a0\2\2\u0531\u0533\5\u00d6l\2\u0532\u0531"+
		"\3\2\2\2\u0532\u0533\3\2\2\2\u0533\u0535\3\2\2\2\u0534\u052f\3\2\2\2\u0535"+
		"\u0538\3\2\2\2\u0536\u0534\3\2\2\2\u0536\u0537\3\2\2\2\u0537\u053b\3\2"+
		"\2\2\u0538\u0536\3\2\2\2\u0539\u053b\5f\64\2\u053a\u052b\3\2\2\2\u053a"+
		"\u0539\3\2\2\2\u053b\u00cb\3\2\2\2\u053c\u053e\7\u00a0\2\2\u053d\u053f"+
		"\5\u00d8m\2\u053e\u053d\3\2\2\2\u053e\u053f\3\2\2\2\u053f\u0540\3\2\2"+
		"\2\u0540\u0541\5\u00d0i\2\u0541\u00cd\3\2\2\2\u0542\u055e\7y\2\2\u0543"+
		"\u0548\7z\2\2\u0544\u0545\7y\2\2\u0545\u0547\7z\2\2\u0546\u0544\3\2\2"+
		"\2\u0547\u054a\3\2\2\2\u0548\u0546\3\2\2\2\u0548\u0549\3\2\2\2\u0549\u054b"+
		"\3\2\2\2\u054a\u0548\3\2\2\2\u054b\u055f\5^\60\2\u054c\u054d\5\u00c4c"+
		"\2\u054d\u0554\7z\2\2\u054e\u054f\7y\2\2\u054f\u0550\5\u00c4c\2\u0550"+
		"\u0551\7z\2\2\u0551\u0553\3\2\2\2\u0552\u054e\3\2\2\2\u0553\u0556\3\2"+
		"\2\2\u0554\u0552\3\2\2\2\u0554\u0555\3\2\2\2\u0555\u055b\3\2\2\2\u0556"+
		"\u0554\3\2\2\2\u0557\u0558\7y\2\2\u0558\u055a\7z\2\2\u0559\u0557\3\2\2"+
		"\2\u055a\u055d\3\2\2\2\u055b\u0559\3\2\2\2\u055b\u055c\3\2\2\2\u055c\u055f"+
		"\3\2\2\2\u055d\u055b\3\2\2\2\u055e\u0543\3\2\2\2\u055e\u054c\3\2\2\2\u055f"+
		"\u00cf\3\2\2\2\u0560\u0562\5\u00dep\2\u0561\u0563\58\35\2\u0562\u0561"+
		"\3\2\2\2\u0562\u0563\3\2\2\2\u0563\u00d1\3\2\2\2\u0564\u0565\5\u00d4k"+
		"\2\u0565\u0566\5\u00dco\2\u0566\u00d3\3\2\2\2\u0567\u0568\7\u0080\2\2"+
		"\u0568\u0569\5\66\34\2\u0569\u056a\7\177\2\2\u056a\u00d5\3\2\2\2\u056b"+
		"\u056c\7\u0080\2\2\u056c\u056f\7\177\2\2\u056d\u056f\5h\65\2\u056e\u056b"+
		"\3\2\2\2\u056e\u056d\3\2\2\2\u056f\u00d7\3\2\2\2\u0570\u0571\7\u0080\2"+
		"\2\u0571\u0574\7\177\2\2\u0572\u0574\5\u00d4k\2\u0573\u0570\3\2\2\2\u0573"+
		"\u0572\3\2\2\2\u0574\u00d9\3\2\2\2\u0575\u057c\5\u00dep\2\u0576\u0577"+
		"\7}\2\2\u0577\u0579\7\u00a0\2\2\u0578\u057a\5\u00dep\2\u0579\u0578\3\2"+
		"\2\2\u0579\u057a\3\2\2\2\u057a\u057c\3\2\2\2\u057b\u0575\3\2\2\2\u057b"+
		"\u0576\3\2\2\2\u057c\u00db\3\2\2\2\u057d\u057e\7d\2\2\u057e\u0582\5\u00da"+
		"n\2\u057f\u0580\7\u00a0\2\2\u0580\u0582\5\u00dep\2\u0581\u057d\3\2\2\2"+
		"\u0581\u057f\3\2\2\2\u0582\u00dd\3\2\2\2\u0583\u0585\7u\2\2\u0584\u0586"+
		"\5\u00be`\2\u0585\u0584\3\2\2\2\u0585\u0586\3\2\2\2\u0586\u0587\3\2\2"+
		"\2\u0587\u0588\7v\2\2\u0588\u00df\3\2\2\2\u00aa\u00e6\u00ef\u00f7\u00fd"+
		"\u0106\u010c\u0113\u0119\u0121\u0123\u012b\u0133\u0135\u013b\u0142\u0146"+
		"\u014d\u0151\u0153\u0156\u015b\u0161\u0169\u0172\u0177\u017e\u0185\u018c"+
		"\u0193\u0198\u019c\u01a0\u01a4\u01a9\u01ad\u01b1\u01bb\u01c3\u01ca\u01d1"+
		"\u01d5\u01d8\u01db\u01e4\u01ea\u01ef\u01f2\u01f8\u01fe\u0202\u020b\u0212"+
		"\u021b\u0222\u0228\u022c\u0237\u023b\u0243\u0248\u024c\u0255\u0263\u0268"+
		"\u0271\u0279\u0283\u028b\u0293\u0298\u02a4\u02aa\u02b1\u02b6\u02be\u02c2"+
		"\u02c4\u02cf\u02d7\u02da\u02de\u02e3\u02e7\u02f2\u02fb\u02fd\u0304\u0309"+
		"\u0312\u0317\u031a\u031f\u0328\u0338\u0342\u0345\u034e\u0358\u0360\u0363"+
		"\u0366\u0373\u037b\u0380\u0388\u038c\u0390\u0394\u0396\u039a\u03a0\u03ab"+
		"\u03b3\u03bb\u03c6\u03cf\u03e6\u03e9\u03ec\u03f4\u03f8\u0400\u0406\u0411"+
		"\u041a\u041f\u0429\u0430\u043d\u0446\u044f\u0455\u0460\u0465\u0471\u0475"+
		"\u0479\u047d\u047f\u0483\u0488\u049b\u04af\u04bf\u04ea\u04fc\u0504\u0506"+
		"\u051c\u051e\u0527\u0529\u052d\u0532\u0536\u053a\u053e\u0548\u0554\u055b"+
		"\u055e\u0562\u056e\u0573\u0579\u057b\u0581\u0585";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}