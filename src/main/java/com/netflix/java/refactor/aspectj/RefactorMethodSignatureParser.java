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
		RULE_formalsPatternAfterDotDot = 3, RULE_typePattern = 4, RULE_dottedNamePattern = 5, 
		RULE_simpleNamePattern = 6, RULE_optionalParensTypePattern = 7, RULE_compilationUnit = 8, 
		RULE_packageDeclaration = 9, RULE_importDeclaration = 10, RULE_typeDeclaration = 11, 
		RULE_modifier = 12, RULE_classOrInterfaceModifier = 13, RULE_variableModifier = 14, 
		RULE_classDeclaration = 15, RULE_typeParameters = 16, RULE_typeParameter = 17, 
		RULE_typeBound = 18, RULE_enumDeclaration = 19, RULE_enumConstants = 20, 
		RULE_enumConstant = 21, RULE_enumBodyDeclarations = 22, RULE_interfaceDeclaration = 23, 
		RULE_typeList = 24, RULE_classBody = 25, RULE_interfaceBody = 26, RULE_classBodyDeclaration = 27, 
		RULE_memberDeclaration = 28, RULE_methodDeclaration = 29, RULE_genericMethodDeclaration = 30, 
		RULE_constructorDeclaration = 31, RULE_genericConstructorDeclaration = 32, 
		RULE_fieldDeclaration = 33, RULE_interfaceBodyDeclaration = 34, RULE_interfaceMemberDeclaration = 35, 
		RULE_constDeclaration = 36, RULE_constantDeclarator = 37, RULE_interfaceMethodDeclaration = 38, 
		RULE_genericInterfaceMethodDeclaration = 39, RULE_variableDeclarators = 40, 
		RULE_variableDeclarator = 41, RULE_variableDeclaratorId = 42, RULE_variableInitializer = 43, 
		RULE_arrayInitializer = 44, RULE_enumConstantName = 45, RULE_type = 46, 
		RULE_classOrInterfaceType = 47, RULE_primitiveType = 48, RULE_typeArguments = 49, 
		RULE_typeArgument = 50, RULE_qualifiedNameList = 51, RULE_formalParameters = 52, 
		RULE_formalParameterList = 53, RULE_formalParameter = 54, RULE_lastFormalParameter = 55, 
		RULE_methodBody = 56, RULE_constructorBody = 57, RULE_qualifiedName = 58, 
		RULE_literal = 59, RULE_annotation = 60, RULE_annotationName = 61, RULE_elementValuePairs = 62, 
		RULE_elementValuePair = 63, RULE_elementValue = 64, RULE_elementValueArrayInitializer = 65, 
		RULE_annotationTypeDeclaration = 66, RULE_annotationTypeBody = 67, RULE_annotationTypeElementDeclaration = 68, 
		RULE_annotationTypeElementRest = 69, RULE_annotationMethodOrConstantRest = 70, 
		RULE_annotationMethodRest = 71, RULE_annotationConstantRest = 72, RULE_defaultValue = 73, 
		RULE_block = 74, RULE_blockStatement = 75, RULE_localVariableDeclarationStatement = 76, 
		RULE_localVariableDeclaration = 77, RULE_statement = 78, RULE_catchClause = 79, 
		RULE_catchType = 80, RULE_finallyBlock = 81, RULE_resourceSpecification = 82, 
		RULE_resources = 83, RULE_resource = 84, RULE_switchBlockStatementGroup = 85, 
		RULE_switchLabel = 86, RULE_forControl = 87, RULE_forInit = 88, RULE_enhancedForControl = 89, 
		RULE_forUpdate = 90, RULE_parExpression = 91, RULE_expressionList = 92, 
		RULE_statementExpression = 93, RULE_constantExpression = 94, RULE_expression = 95, 
		RULE_primary = 96, RULE_creator = 97, RULE_createdName = 98, RULE_innerCreator = 99, 
		RULE_arrayCreatorRest = 100, RULE_classCreatorRest = 101, RULE_explicitGenericInvocation = 102, 
		RULE_nonWildcardTypeArguments = 103, RULE_typeArgumentsOrDiamond = 104, 
		RULE_nonWildcardTypeArgumentsOrDiamond = 105, RULE_superSuffix = 106, 
		RULE_explicitGenericInvocationSuffix = 107, RULE_arguments = 108;
	public static final String[] ruleNames = {
		"methodPattern", "formalParametersPattern", "formalsPattern", "formalsPatternAfterDotDot", 
		"typePattern", "dottedNamePattern", "simpleNamePattern", "optionalParensTypePattern", 
		"compilationUnit", "packageDeclaration", "importDeclaration", "typeDeclaration", 
		"modifier", "classOrInterfaceModifier", "variableModifier", "classDeclaration", 
		"typeParameters", "typeParameter", "typeBound", "enumDeclaration", "enumConstants", 
		"enumConstant", "enumBodyDeclarations", "interfaceDeclaration", "typeList", 
		"classBody", "interfaceBody", "classBodyDeclaration", "memberDeclaration", 
		"methodDeclaration", "genericMethodDeclaration", "constructorDeclaration", 
		"genericConstructorDeclaration", "fieldDeclaration", "interfaceBodyDeclaration", 
		"interfaceMemberDeclaration", "constDeclaration", "constantDeclarator", 
		"interfaceMethodDeclaration", "genericInterfaceMethodDeclaration", "variableDeclarators", 
		"variableDeclarator", "variableDeclaratorId", "variableInitializer", "arrayInitializer", 
		"enumConstantName", "type", "classOrInterfaceType", "primitiveType", "typeArguments", 
		"typeArgument", "qualifiedNameList", "formalParameters", "formalParameterList", 
		"formalParameter", "lastFormalParameter", "methodBody", "constructorBody", 
		"qualifiedName", "literal", "annotation", "annotationName", "elementValuePairs", 
		"elementValuePair", "elementValue", "elementValueArrayInitializer", "annotationTypeDeclaration", 
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
		public TypePatternContext typePattern() {
			return getRuleContext(TypePatternContext.class,0);
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
			setState(218); typePattern(0);
			setState(219); simpleNamePattern();
			setState(220); formalParametersPattern();
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
			setState(222); match(LPAREN);
			setState(224);
			_la = _input.LA(1);
			if (_la==DOTDOT || ((((_la - 106)) & ~0x3f) == 0 && ((1L << (_la - 106)) & ((1L << (VOID - 106)) | (1L << (LPAREN - 106)) | (1L << (DOT - 106)) | (1L << (BANG - 106)) | (1L << (MUL - 106)) | (1L << (Identifier - 106)))) != 0)) {
				{
				setState(223); formalsPattern();
				}
			}

			setState(226); match(RPAREN);
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
		public List<FormalsPatternContext> formalsPattern() {
			return getRuleContexts(FormalsPatternContext.class);
		}
		public TypePatternContext typePattern() {
			return getRuleContext(TypePatternContext.class,0);
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
			setState(247);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(228); match(DOTDOT);
				setState(233);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,1,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(229); match(COMMA);
						setState(230); formalsPatternAfterDotDot();
						}
						} 
					}
					setState(235);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,1,_ctx);
				}
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(236); optionalParensTypePattern();
				setState(241);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(237); match(COMMA);
						setState(238); formalsPattern();
						}
						} 
					}
					setState(243);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
				}
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(244); typePattern(0);
				setState(245); match(ELLIPSIS);
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

	public static class FormalsPatternAfterDotDotContext extends ParserRuleContext {
		public List<FormalsPatternAfterDotDotContext> formalsPatternAfterDotDot() {
			return getRuleContexts(FormalsPatternAfterDotDotContext.class);
		}
		public FormalsPatternAfterDotDotContext formalsPatternAfterDotDot(int i) {
			return getRuleContext(FormalsPatternAfterDotDotContext.class,i);
		}
		public TypePatternContext typePattern() {
			return getRuleContext(TypePatternContext.class,0);
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
		enterRule(_localctx, 6, RULE_formalsPatternAfterDotDot);
		try {
			int _alt;
			setState(260);
			switch ( getInterpreter().adaptivePredict(_input,5,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(249); optionalParensTypePattern();
				setState(254);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,4,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(250); match(COMMA);
						setState(251); formalsPatternAfterDotDot();
						}
						} 
					}
					setState(256);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,4,_ctx);
				}
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(257); typePattern(0);
				setState(258); match(ELLIPSIS);
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

	public static class TypePatternContext extends ParserRuleContext {
		public DottedNamePatternContext dottedNamePattern() {
			return getRuleContext(DottedNamePatternContext.class,0);
		}
		public TypePatternContext typePattern(int i) {
			return getRuleContext(TypePatternContext.class,i);
		}
		public List<TypePatternContext> typePattern() {
			return getRuleContexts(TypePatternContext.class);
		}
		public TypePatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typePattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterTypePattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitTypePattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitTypePattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypePatternContext typePattern() throws RecognitionException {
		return typePattern(0);
	}

	private TypePatternContext typePattern(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		TypePatternContext _localctx = new TypePatternContext(_ctx, _parentState);
		TypePatternContext _prevctx = _localctx;
		int _startState = 8;
		enterRecursionRule(_localctx, 8, RULE_typePattern, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(266);
			switch (_input.LA(1)) {
			case BANG:
				{
				setState(263); match(BANG);
				setState(264); typePattern(3);
				}
				break;
			case DOTDOT:
			case VOID:
			case DOT:
			case MUL:
			case Identifier:
				{
				setState(265); dottedNamePattern();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(276);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,8,_ctx);
			while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(274);
					switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
					case 1:
						{
						_localctx = new TypePatternContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_typePattern);
						setState(268);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(269); match(AND);
						setState(270); typePattern(3);
						}
						break;

					case 2:
						{
						_localctx = new TypePatternContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_typePattern);
						setState(271);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(272); match(OR);
						setState(273); typePattern(2);
						}
						break;
					}
					} 
				}
				setState(278);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,8,_ctx);
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

	public static class DottedNamePatternContext extends ParserRuleContext {
		public List<TerminalNode> Identifier() { return getTokens(RefactorMethodSignatureParser.Identifier); }
		public TerminalNode Identifier(int i) {
			return getToken(RefactorMethodSignatureParser.Identifier, i);
		}
		public DottedNamePatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dottedNamePattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).enterDottedNamePattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RefactorMethodSignatureParserListener ) ((RefactorMethodSignatureParserListener)listener).exitDottedNamePattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RefactorMethodSignatureParserVisitor ) return ((RefactorMethodSignatureParserVisitor<? extends T>)visitor).visitDottedNamePattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DottedNamePatternContext dottedNamePattern() throws RecognitionException {
		DottedNamePatternContext _localctx = new DottedNamePatternContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_dottedNamePattern);
		int _la;
		try {
			int _alt;
			setState(285);
			switch (_input.LA(1)) {
			case DOTDOT:
			case DOT:
			case MUL:
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(280); 
				_errHandler.sync(this);
				_alt = 1;
				do {
					switch (_alt) {
					case 1:
						{
						{
						setState(279);
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
					setState(282); 
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,9,_ctx);
				} while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER );
				}
				break;
			case VOID:
				enterOuterAlt(_localctx, 2);
				{
				setState(284); match(VOID);
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
		enterRule(_localctx, 12, RULE_simpleNamePattern);
		int _la;
		try {
			int _alt;
			setState(309);
			switch (_input.LA(1)) {
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(287); match(Identifier);
				setState(292);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,11,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(288); match(MUL);
						setState(289); match(Identifier);
						}
						} 
					}
					setState(294);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,11,_ctx);
				}
				setState(296);
				_la = _input.LA(1);
				if (_la==MUL) {
					{
					setState(295); match(MUL);
					}
				}

				}
				break;
			case MUL:
				enterOuterAlt(_localctx, 2);
				{
				setState(298); match(MUL);
				setState(303);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,13,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(299); match(Identifier);
						setState(300); match(MUL);
						}
						} 
					}
					setState(305);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,13,_ctx);
				}
				setState(307);
				_la = _input.LA(1);
				if (_la==Identifier) {
					{
					setState(306); match(Identifier);
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

	public static class OptionalParensTypePatternContext extends ParserRuleContext {
		public TypePatternContext typePattern() {
			return getRuleContext(TypePatternContext.class,0);
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
		enterRule(_localctx, 14, RULE_optionalParensTypePattern);
		try {
			setState(316);
			switch (_input.LA(1)) {
			case LPAREN:
				enterOuterAlt(_localctx, 1);
				{
				setState(311); match(LPAREN);
				setState(312); typePattern(0);
				setState(313); match(RPAREN);
				}
				break;
			case DOTDOT:
			case VOID:
			case DOT:
			case BANG:
			case MUL:
			case Identifier:
				enterOuterAlt(_localctx, 2);
				{
				setState(315); typePattern(0);
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
		enterRule(_localctx, 16, RULE_compilationUnit);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(319);
			switch ( getInterpreter().adaptivePredict(_input,17,_ctx) ) {
			case 1:
				{
				setState(318); packageDeclaration();
				}
				break;
			}
			setState(324);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==IMPORT) {
				{
				{
				setState(321); importDeclaration();
				}
				}
				setState(326);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(330);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (ABSTRACT - 58)) | (1L << (CLASS - 58)) | (1L << (ENUM - 58)) | (1L << (FINAL - 58)) | (1L << (INTERFACE - 58)) | (1L << (PRIVATE - 58)) | (1L << (PROTECTED - 58)) | (1L << (PUBLIC - 58)) | (1L << (STATIC - 58)) | (1L << (STRICTFP - 58)) | (1L << (SEMI - 58)))) != 0)) {
				{
				{
				setState(327); typeDeclaration();
				}
				}
				setState(332);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(333); match(EOF);
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
		enterRule(_localctx, 18, RULE_packageDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(338);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT) {
				{
				{
				setState(335); annotation();
				}
				}
				setState(340);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(341); match(PACKAGE);
			setState(342); qualifiedName();
			setState(343); match(SEMI);
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
		enterRule(_localctx, 20, RULE_importDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(345); match(IMPORT);
			setState(347);
			_la = _input.LA(1);
			if (_la==STATIC) {
				{
				setState(346); match(STATIC);
				}
			}

			setState(349); qualifiedName();
			setState(352);
			_la = _input.LA(1);
			if (_la==DOT) {
				{
				setState(350); match(DOT);
				setState(351); match(MUL);
				}
			}

			setState(354); match(SEMI);
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
		enterRule(_localctx, 22, RULE_typeDeclaration);
		int _la;
		try {
			int _alt;
			setState(385);
			switch ( getInterpreter().adaptivePredict(_input,27,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(359);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (ABSTRACT - 58)) | (1L << (FINAL - 58)) | (1L << (PRIVATE - 58)) | (1L << (PROTECTED - 58)) | (1L << (PUBLIC - 58)) | (1L << (STATIC - 58)) | (1L << (STRICTFP - 58)))) != 0)) {
					{
					{
					setState(356); classOrInterfaceModifier();
					}
					}
					setState(361);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(362); classDeclaration();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(366);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (ABSTRACT - 58)) | (1L << (FINAL - 58)) | (1L << (PRIVATE - 58)) | (1L << (PROTECTED - 58)) | (1L << (PUBLIC - 58)) | (1L << (STATIC - 58)) | (1L << (STRICTFP - 58)))) != 0)) {
					{
					{
					setState(363); classOrInterfaceModifier();
					}
					}
					setState(368);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(369); enumDeclaration();
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(373);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (ABSTRACT - 58)) | (1L << (FINAL - 58)) | (1L << (PRIVATE - 58)) | (1L << (PROTECTED - 58)) | (1L << (PUBLIC - 58)) | (1L << (STATIC - 58)) | (1L << (STRICTFP - 58)))) != 0)) {
					{
					{
					setState(370); classOrInterfaceModifier();
					}
					}
					setState(375);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(376); interfaceDeclaration();
				}
				break;

			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(380);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,26,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(377); classOrInterfaceModifier();
						}
						} 
					}
					setState(382);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,26,_ctx);
				}
				setState(383); annotationTypeDeclaration();
				}
				break;

			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(384); match(SEMI);
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
		enterRule(_localctx, 24, RULE_modifier);
		int _la;
		try {
			setState(389);
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
				setState(387); classOrInterfaceModifier();
				}
				break;
			case NATIVE:
			case SYNCHRONIZED:
			case TRANSIENT:
			case VOLATILE:
				enterOuterAlt(_localctx, 2);
				{
				setState(388);
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
		enterRule(_localctx, 26, RULE_classOrInterfaceModifier);
		int _la;
		try {
			setState(393);
			switch (_input.LA(1)) {
			case AT:
				enterOuterAlt(_localctx, 1);
				{
				setState(391); annotation();
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
				setState(392);
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
		enterRule(_localctx, 28, RULE_variableModifier);
		try {
			setState(397);
			switch (_input.LA(1)) {
			case FINAL:
				enterOuterAlt(_localctx, 1);
				{
				setState(395); match(FINAL);
				}
				break;
			case AT:
				enterOuterAlt(_localctx, 2);
				{
				setState(396); annotation();
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
		enterRule(_localctx, 30, RULE_classDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(399); match(CLASS);
			setState(400); match(Identifier);
			setState(402);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(401); typeParameters();
				}
			}

			setState(406);
			_la = _input.LA(1);
			if (_la==EXTENDS) {
				{
				setState(404); match(EXTENDS);
				setState(405); type();
				}
			}

			setState(410);
			_la = _input.LA(1);
			if (_la==IMPLEMENTS) {
				{
				setState(408); match(IMPLEMENTS);
				setState(409); typeList();
				}
			}

			setState(412); classBody();
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
		enterRule(_localctx, 32, RULE_typeParameters);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(414); match(LT);
			setState(415); typeParameter();
			setState(420);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(416); match(COMMA);
				setState(417); typeParameter();
				}
				}
				setState(422);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(423); match(GT);
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
		enterRule(_localctx, 34, RULE_typeParameter);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(425); match(Identifier);
			setState(428);
			_la = _input.LA(1);
			if (_la==EXTENDS) {
				{
				setState(426); match(EXTENDS);
				setState(427); typeBound();
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
		enterRule(_localctx, 36, RULE_typeBound);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(430); type();
			setState(435);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==BITAND) {
				{
				{
				setState(431); match(BITAND);
				setState(432); type();
				}
				}
				setState(437);
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
		enterRule(_localctx, 38, RULE_enumDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(438); match(ENUM);
			setState(439); match(Identifier);
			setState(442);
			_la = _input.LA(1);
			if (_la==IMPLEMENTS) {
				{
				setState(440); match(IMPLEMENTS);
				setState(441); typeList();
				}
			}

			setState(444); match(LBRACE);
			setState(446);
			_la = _input.LA(1);
			if (_la==AT || _la==Identifier) {
				{
				setState(445); enumConstants();
				}
			}

			setState(449);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(448); match(COMMA);
				}
			}

			setState(452);
			_la = _input.LA(1);
			if (_la==SEMI) {
				{
				setState(451); enumBodyDeclarations();
				}
			}

			setState(454); match(RBRACE);
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
		enterRule(_localctx, 40, RULE_enumConstants);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(456); enumConstant();
			setState(461);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,41,_ctx);
			while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(457); match(COMMA);
					setState(458); enumConstant();
					}
					} 
				}
				setState(463);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,41,_ctx);
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
		enterRule(_localctx, 42, RULE_enumConstant);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(467);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT) {
				{
				{
				setState(464); annotation();
				}
				}
				setState(469);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(470); match(Identifier);
			setState(472);
			_la = _input.LA(1);
			if (_la==LPAREN) {
				{
				setState(471); arguments();
				}
			}

			setState(475);
			_la = _input.LA(1);
			if (_la==LBRACE) {
				{
				setState(474); classBody();
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
		enterRule(_localctx, 44, RULE_enumBodyDeclarations);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(477); match(SEMI);
			setState(481);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (ABSTRACT - 58)) | (1L << (BOOLEAN - 58)) | (1L << (BYTE - 58)) | (1L << (CHAR - 58)) | (1L << (CLASS - 58)) | (1L << (DOUBLE - 58)) | (1L << (ENUM - 58)) | (1L << (FINAL - 58)) | (1L << (FLOAT - 58)) | (1L << (INT - 58)) | (1L << (INTERFACE - 58)) | (1L << (LONG - 58)) | (1L << (NATIVE - 58)) | (1L << (PRIVATE - 58)) | (1L << (PROTECTED - 58)) | (1L << (PUBLIC - 58)) | (1L << (SHORT - 58)) | (1L << (STATIC - 58)) | (1L << (STRICTFP - 58)) | (1L << (SYNCHRONIZED - 58)) | (1L << (TRANSIENT - 58)) | (1L << (VOID - 58)) | (1L << (VOLATILE - 58)) | (1L << (LBRACE - 58)) | (1L << (SEMI - 58)))) != 0) || _la==LT || _la==Identifier) {
				{
				{
				setState(478); classBodyDeclaration();
				}
				}
				setState(483);
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
		enterRule(_localctx, 46, RULE_interfaceDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(484); match(INTERFACE);
			setState(485); match(Identifier);
			setState(487);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(486); typeParameters();
				}
			}

			setState(491);
			_la = _input.LA(1);
			if (_la==EXTENDS) {
				{
				setState(489); match(EXTENDS);
				setState(490); typeList();
				}
			}

			setState(493); interfaceBody();
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
		enterRule(_localctx, 48, RULE_typeList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(495); type();
			setState(500);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(496); match(COMMA);
				setState(497); type();
				}
				}
				setState(502);
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
		enterRule(_localctx, 50, RULE_classBody);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(503); match(LBRACE);
			setState(507);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (ABSTRACT - 58)) | (1L << (BOOLEAN - 58)) | (1L << (BYTE - 58)) | (1L << (CHAR - 58)) | (1L << (CLASS - 58)) | (1L << (DOUBLE - 58)) | (1L << (ENUM - 58)) | (1L << (FINAL - 58)) | (1L << (FLOAT - 58)) | (1L << (INT - 58)) | (1L << (INTERFACE - 58)) | (1L << (LONG - 58)) | (1L << (NATIVE - 58)) | (1L << (PRIVATE - 58)) | (1L << (PROTECTED - 58)) | (1L << (PUBLIC - 58)) | (1L << (SHORT - 58)) | (1L << (STATIC - 58)) | (1L << (STRICTFP - 58)) | (1L << (SYNCHRONIZED - 58)) | (1L << (TRANSIENT - 58)) | (1L << (VOID - 58)) | (1L << (VOLATILE - 58)) | (1L << (LBRACE - 58)) | (1L << (SEMI - 58)))) != 0) || _la==LT || _la==Identifier) {
				{
				{
				setState(504); classBodyDeclaration();
				}
				}
				setState(509);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(510); match(RBRACE);
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
		enterRule(_localctx, 52, RULE_interfaceBody);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(512); match(LBRACE);
			setState(516);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (ABSTRACT - 58)) | (1L << (BOOLEAN - 58)) | (1L << (BYTE - 58)) | (1L << (CHAR - 58)) | (1L << (CLASS - 58)) | (1L << (DOUBLE - 58)) | (1L << (ENUM - 58)) | (1L << (FINAL - 58)) | (1L << (FLOAT - 58)) | (1L << (INT - 58)) | (1L << (INTERFACE - 58)) | (1L << (LONG - 58)) | (1L << (NATIVE - 58)) | (1L << (PRIVATE - 58)) | (1L << (PROTECTED - 58)) | (1L << (PUBLIC - 58)) | (1L << (SHORT - 58)) | (1L << (STATIC - 58)) | (1L << (STRICTFP - 58)) | (1L << (SYNCHRONIZED - 58)) | (1L << (TRANSIENT - 58)) | (1L << (VOID - 58)) | (1L << (VOLATILE - 58)) | (1L << (SEMI - 58)))) != 0) || _la==LT || _la==Identifier) {
				{
				{
				setState(513); interfaceBodyDeclaration();
				}
				}
				setState(518);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(519); match(RBRACE);
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
		enterRule(_localctx, 54, RULE_classBodyDeclaration);
		int _la;
		try {
			int _alt;
			setState(533);
			switch ( getInterpreter().adaptivePredict(_input,53,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(521); match(SEMI);
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(523);
				_la = _input.LA(1);
				if (_la==STATIC) {
					{
					setState(522); match(STATIC);
					}
				}

				setState(525); block();
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(529);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,52,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(526); modifier();
						}
						} 
					}
					setState(531);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,52,_ctx);
				}
				setState(532); memberDeclaration();
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
		enterRule(_localctx, 56, RULE_memberDeclaration);
		try {
			setState(544);
			switch ( getInterpreter().adaptivePredict(_input,54,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(535); methodDeclaration();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(536); genericMethodDeclaration();
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(537); fieldDeclaration();
				}
				break;

			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(538); constructorDeclaration();
				}
				break;

			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(539); genericConstructorDeclaration();
				}
				break;

			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(540); interfaceDeclaration();
				}
				break;

			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(541); annotationTypeDeclaration();
				}
				break;

			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(542); classDeclaration();
				}
				break;

			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(543); enumDeclaration();
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
		enterRule(_localctx, 58, RULE_methodDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(548);
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
				setState(546); type();
				}
				break;
			case VOID:
				{
				setState(547); match(VOID);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(550); match(Identifier);
			setState(551); formalParameters();
			setState(556);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==LBRACK) {
				{
				{
				setState(552); match(LBRACK);
				setState(553); match(RBRACK);
				}
				}
				setState(558);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(561);
			_la = _input.LA(1);
			if (_la==THROWS) {
				{
				setState(559); match(THROWS);
				setState(560); qualifiedNameList();
				}
			}

			setState(565);
			switch (_input.LA(1)) {
			case LBRACE:
				{
				setState(563); methodBody();
				}
				break;
			case SEMI:
				{
				setState(564); match(SEMI);
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
		enterRule(_localctx, 60, RULE_genericMethodDeclaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(567); typeParameters();
			setState(568); methodDeclaration();
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
		enterRule(_localctx, 62, RULE_constructorDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(570); match(Identifier);
			setState(571); formalParameters();
			setState(574);
			_la = _input.LA(1);
			if (_la==THROWS) {
				{
				setState(572); match(THROWS);
				setState(573); qualifiedNameList();
				}
			}

			setState(576); constructorBody();
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
		enterRule(_localctx, 64, RULE_genericConstructorDeclaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(578); typeParameters();
			setState(579); constructorDeclaration();
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
		enterRule(_localctx, 66, RULE_fieldDeclaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(581); type();
			setState(582); variableDeclarators();
			setState(583); match(SEMI);
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
		enterRule(_localctx, 68, RULE_interfaceBodyDeclaration);
		try {
			int _alt;
			setState(593);
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
				setState(588);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,60,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(585); modifier();
						}
						} 
					}
					setState(590);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,60,_ctx);
				}
				setState(591); interfaceMemberDeclaration();
				}
				break;
			case SEMI:
				enterOuterAlt(_localctx, 2);
				{
				setState(592); match(SEMI);
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
		enterRule(_localctx, 70, RULE_interfaceMemberDeclaration);
		try {
			setState(602);
			switch ( getInterpreter().adaptivePredict(_input,62,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(595); constDeclaration();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(596); interfaceMethodDeclaration();
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(597); genericInterfaceMethodDeclaration();
				}
				break;

			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(598); interfaceDeclaration();
				}
				break;

			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(599); annotationTypeDeclaration();
				}
				break;

			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(600); classDeclaration();
				}
				break;

			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(601); enumDeclaration();
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
		enterRule(_localctx, 72, RULE_constDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(604); type();
			setState(605); constantDeclarator();
			setState(610);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(606); match(COMMA);
				setState(607); constantDeclarator();
				}
				}
				setState(612);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(613); match(SEMI);
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
		enterRule(_localctx, 74, RULE_constantDeclarator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(615); match(Identifier);
			setState(620);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==LBRACK) {
				{
				{
				setState(616); match(LBRACK);
				setState(617); match(RBRACK);
				}
				}
				setState(622);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(623); match(ASSIGN);
			setState(624); variableInitializer();
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
		enterRule(_localctx, 76, RULE_interfaceMethodDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(628);
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
				setState(626); type();
				}
				break;
			case VOID:
				{
				setState(627); match(VOID);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(630); match(Identifier);
			setState(631); formalParameters();
			setState(636);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==LBRACK) {
				{
				{
				setState(632); match(LBRACK);
				setState(633); match(RBRACK);
				}
				}
				setState(638);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(641);
			_la = _input.LA(1);
			if (_la==THROWS) {
				{
				setState(639); match(THROWS);
				setState(640); qualifiedNameList();
				}
			}

			setState(643); match(SEMI);
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
		enterRule(_localctx, 78, RULE_genericInterfaceMethodDeclaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(645); typeParameters();
			setState(646); interfaceMethodDeclaration();
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
		enterRule(_localctx, 80, RULE_variableDeclarators);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(648); variableDeclarator();
			setState(653);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(649); match(COMMA);
				setState(650); variableDeclarator();
				}
				}
				setState(655);
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
		enterRule(_localctx, 82, RULE_variableDeclarator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(656); variableDeclaratorId();
			setState(659);
			_la = _input.LA(1);
			if (_la==ASSIGN) {
				{
				setState(657); match(ASSIGN);
				setState(658); variableInitializer();
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
		enterRule(_localctx, 84, RULE_variableDeclaratorId);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(661); match(Identifier);
			setState(666);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==LBRACK) {
				{
				{
				setState(662); match(LBRACK);
				setState(663); match(RBRACK);
				}
				}
				setState(668);
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
		enterRule(_localctx, 86, RULE_variableInitializer);
		try {
			setState(671);
			switch (_input.LA(1)) {
			case LBRACE:
				enterOuterAlt(_localctx, 1);
				{
				setState(669); arrayInitializer();
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
				setState(670); expression(0);
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
		enterRule(_localctx, 88, RULE_arrayInitializer);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(673); match(LBRACE);
			setState(685);
			_la = _input.LA(1);
			if (((((_la - 61)) & ~0x3f) == 0 && ((1L << (_la - 61)) & ((1L << (BOOLEAN - 61)) | (1L << (BYTE - 61)) | (1L << (CHAR - 61)) | (1L << (DOUBLE - 61)) | (1L << (FLOAT - 61)) | (1L << (INT - 61)) | (1L << (LONG - 61)) | (1L << (NEW - 61)) | (1L << (SHORT - 61)) | (1L << (SUPER - 61)) | (1L << (THIS - 61)) | (1L << (VOID - 61)) | (1L << (IntegerLiteral - 61)) | (1L << (FloatingPointLiteral - 61)) | (1L << (BooleanLiteral - 61)) | (1L << (CharacterLiteral - 61)) | (1L << (StringLiteral - 61)) | (1L << (NullLiteral - 61)) | (1L << (LPAREN - 61)) | (1L << (LBRACE - 61)))) != 0) || ((((_la - 126)) & ~0x3f) == 0 && ((1L << (_la - 126)) & ((1L << (LT - 126)) | (1L << (BANG - 126)) | (1L << (TILDE - 126)) | (1L << (INC - 126)) | (1L << (DEC - 126)) | (1L << (ADD - 126)) | (1L << (SUB - 126)) | (1L << (Identifier - 126)))) != 0)) {
				{
				setState(674); variableInitializer();
				setState(679);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,72,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(675); match(COMMA);
						setState(676); variableInitializer();
						}
						} 
					}
					setState(681);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,72,_ctx);
				}
				setState(683);
				_la = _input.LA(1);
				if (_la==COMMA) {
					{
					setState(682); match(COMMA);
					}
				}

				}
			}

			setState(687); match(RBRACE);
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
		enterRule(_localctx, 90, RULE_enumConstantName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(689); match(Identifier);
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
		enterRule(_localctx, 92, RULE_type);
		try {
			int _alt;
			setState(707);
			switch (_input.LA(1)) {
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(691); classOrInterfaceType();
				setState(696);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,75,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(692); match(LBRACK);
						setState(693); match(RBRACK);
						}
						} 
					}
					setState(698);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,75,_ctx);
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
				setState(699); primitiveType();
				setState(704);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,76,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(700); match(LBRACK);
						setState(701); match(RBRACK);
						}
						} 
					}
					setState(706);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,76,_ctx);
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
		enterRule(_localctx, 94, RULE_classOrInterfaceType);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(709); match(Identifier);
			setState(711);
			switch ( getInterpreter().adaptivePredict(_input,78,_ctx) ) {
			case 1:
				{
				setState(710); typeArguments();
				}
				break;
			}
			setState(720);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,80,_ctx);
			while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(713); match(DOT);
					setState(714); match(Identifier);
					setState(716);
					switch ( getInterpreter().adaptivePredict(_input,79,_ctx) ) {
					case 1:
						{
						setState(715); typeArguments();
						}
						break;
					}
					}
					} 
				}
				setState(722);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,80,_ctx);
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
		enterRule(_localctx, 96, RULE_primitiveType);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(723);
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
		enterRule(_localctx, 98, RULE_typeArguments);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(725); match(LT);
			setState(726); typeArgument();
			setState(731);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(727); match(COMMA);
				setState(728); typeArgument();
				}
				}
				setState(733);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(734); match(GT);
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
		enterRule(_localctx, 100, RULE_typeArgument);
		int _la;
		try {
			setState(742);
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
				setState(736); type();
				}
				break;
			case QUESTION:
				enterOuterAlt(_localctx, 2);
				{
				setState(737); match(QUESTION);
				setState(740);
				_la = _input.LA(1);
				if (_la==EXTENDS || _la==SUPER) {
					{
					setState(738);
					_la = _input.LA(1);
					if ( !(_la==EXTENDS || _la==SUPER) ) {
					_errHandler.recoverInline(this);
					}
					consume();
					setState(739); type();
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
		enterRule(_localctx, 102, RULE_qualifiedNameList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(744); qualifiedName();
			setState(749);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(745); match(COMMA);
				setState(746); qualifiedName();
				}
				}
				setState(751);
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
		enterRule(_localctx, 104, RULE_formalParameters);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(752); match(LPAREN);
			setState(754);
			_la = _input.LA(1);
			if (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (BOOLEAN - 58)) | (1L << (BYTE - 58)) | (1L << (CHAR - 58)) | (1L << (DOUBLE - 58)) | (1L << (FINAL - 58)) | (1L << (FLOAT - 58)) | (1L << (INT - 58)) | (1L << (LONG - 58)) | (1L << (SHORT - 58)))) != 0) || _la==Identifier) {
				{
				setState(753); formalParameterList();
				}
			}

			setState(756); match(RPAREN);
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
		enterRule(_localctx, 106, RULE_formalParameterList);
		int _la;
		try {
			int _alt;
			setState(771);
			switch ( getInterpreter().adaptivePredict(_input,88,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(758); formalParameter();
				setState(763);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,86,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(759); match(COMMA);
						setState(760); formalParameter();
						}
						} 
					}
					setState(765);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,86,_ctx);
				}
				setState(768);
				_la = _input.LA(1);
				if (_la==COMMA) {
					{
					setState(766); match(COMMA);
					setState(767); lastFormalParameter();
					}
				}

				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(770); lastFormalParameter();
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
		enterRule(_localctx, 108, RULE_formalParameter);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(776);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==FINAL) {
				{
				{
				setState(773); variableModifier();
				}
				}
				setState(778);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(779); type();
			setState(780); variableDeclaratorId();
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
		enterRule(_localctx, 110, RULE_lastFormalParameter);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(785);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==FINAL) {
				{
				{
				setState(782); variableModifier();
				}
				}
				setState(787);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(788); type();
			setState(789); match(ELLIPSIS);
			setState(790); variableDeclaratorId();
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
		enterRule(_localctx, 112, RULE_methodBody);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(792); block();
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
		enterRule(_localctx, 114, RULE_constructorBody);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(794); block();
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
		enterRule(_localctx, 116, RULE_qualifiedName);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(796); match(Identifier);
			setState(801);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,91,_ctx);
			while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(797); match(DOT);
					setState(798); match(Identifier);
					}
					} 
				}
				setState(803);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,91,_ctx);
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
		enterRule(_localctx, 118, RULE_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(804);
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
		enterRule(_localctx, 120, RULE_annotation);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(806); match(AT);
			setState(807); annotationName();
			setState(814);
			_la = _input.LA(1);
			if (_la==LPAREN) {
				{
				setState(808); match(LPAREN);
				setState(811);
				switch ( getInterpreter().adaptivePredict(_input,92,_ctx) ) {
				case 1:
					{
					setState(809); elementValuePairs();
					}
					break;

				case 2:
					{
					setState(810); elementValue();
					}
					break;
				}
				setState(813); match(RPAREN);
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
		enterRule(_localctx, 122, RULE_annotationName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(816); qualifiedName();
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
		enterRule(_localctx, 124, RULE_elementValuePairs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(818); elementValuePair();
			setState(823);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(819); match(COMMA);
				setState(820); elementValuePair();
				}
				}
				setState(825);
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
		enterRule(_localctx, 126, RULE_elementValuePair);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(826); match(Identifier);
			setState(827); match(ASSIGN);
			setState(828); elementValue();
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
		enterRule(_localctx, 128, RULE_elementValue);
		try {
			setState(833);
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
				setState(830); expression(0);
				}
				break;
			case AT:
				enterOuterAlt(_localctx, 2);
				{
				setState(831); annotation();
				}
				break;
			case LBRACE:
				enterOuterAlt(_localctx, 3);
				{
				setState(832); elementValueArrayInitializer();
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
		enterRule(_localctx, 130, RULE_elementValueArrayInitializer);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(835); match(LBRACE);
			setState(844);
			_la = _input.LA(1);
			if (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (BOOLEAN - 58)) | (1L << (BYTE - 58)) | (1L << (CHAR - 58)) | (1L << (DOUBLE - 58)) | (1L << (FLOAT - 58)) | (1L << (INT - 58)) | (1L << (LONG - 58)) | (1L << (NEW - 58)) | (1L << (SHORT - 58)) | (1L << (SUPER - 58)) | (1L << (THIS - 58)) | (1L << (VOID - 58)) | (1L << (IntegerLiteral - 58)) | (1L << (FloatingPointLiteral - 58)) | (1L << (BooleanLiteral - 58)) | (1L << (CharacterLiteral - 58)) | (1L << (StringLiteral - 58)) | (1L << (NullLiteral - 58)) | (1L << (LPAREN - 58)) | (1L << (LBRACE - 58)))) != 0) || ((((_la - 126)) & ~0x3f) == 0 && ((1L << (_la - 126)) & ((1L << (LT - 126)) | (1L << (BANG - 126)) | (1L << (TILDE - 126)) | (1L << (INC - 126)) | (1L << (DEC - 126)) | (1L << (ADD - 126)) | (1L << (SUB - 126)) | (1L << (Identifier - 126)))) != 0)) {
				{
				setState(836); elementValue();
				setState(841);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,96,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(837); match(COMMA);
						setState(838); elementValue();
						}
						} 
					}
					setState(843);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,96,_ctx);
				}
				}
			}

			setState(847);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(846); match(COMMA);
				}
			}

			setState(849); match(RBRACE);
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
		enterRule(_localctx, 132, RULE_annotationTypeDeclaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(851); match(AT);
			setState(852); match(INTERFACE);
			setState(853); match(Identifier);
			setState(854); annotationTypeBody();
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
		enterRule(_localctx, 134, RULE_annotationTypeBody);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(856); match(LBRACE);
			setState(860);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (ABSTRACT - 58)) | (1L << (BOOLEAN - 58)) | (1L << (BYTE - 58)) | (1L << (CHAR - 58)) | (1L << (CLASS - 58)) | (1L << (DOUBLE - 58)) | (1L << (ENUM - 58)) | (1L << (FINAL - 58)) | (1L << (FLOAT - 58)) | (1L << (INT - 58)) | (1L << (INTERFACE - 58)) | (1L << (LONG - 58)) | (1L << (NATIVE - 58)) | (1L << (PRIVATE - 58)) | (1L << (PROTECTED - 58)) | (1L << (PUBLIC - 58)) | (1L << (SHORT - 58)) | (1L << (STATIC - 58)) | (1L << (STRICTFP - 58)) | (1L << (SYNCHRONIZED - 58)) | (1L << (TRANSIENT - 58)) | (1L << (VOLATILE - 58)) | (1L << (SEMI - 58)))) != 0) || _la==Identifier) {
				{
				{
				setState(857); annotationTypeElementDeclaration();
				}
				}
				setState(862);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(863); match(RBRACE);
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
		enterRule(_localctx, 136, RULE_annotationTypeElementDeclaration);
		try {
			int _alt;
			setState(873);
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
				setState(868);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,100,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(865); modifier();
						}
						} 
					}
					setState(870);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,100,_ctx);
				}
				setState(871); annotationTypeElementRest();
				}
				break;
			case SEMI:
				enterOuterAlt(_localctx, 2);
				{
				setState(872); match(SEMI);
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
		enterRule(_localctx, 138, RULE_annotationTypeElementRest);
		try {
			setState(895);
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
				setState(875); type();
				setState(876); annotationMethodOrConstantRest();
				setState(877); match(SEMI);
				}
				break;
			case CLASS:
				enterOuterAlt(_localctx, 2);
				{
				setState(879); classDeclaration();
				setState(881);
				switch ( getInterpreter().adaptivePredict(_input,102,_ctx) ) {
				case 1:
					{
					setState(880); match(SEMI);
					}
					break;
				}
				}
				break;
			case INTERFACE:
				enterOuterAlt(_localctx, 3);
				{
				setState(883); interfaceDeclaration();
				setState(885);
				switch ( getInterpreter().adaptivePredict(_input,103,_ctx) ) {
				case 1:
					{
					setState(884); match(SEMI);
					}
					break;
				}
				}
				break;
			case ENUM:
				enterOuterAlt(_localctx, 4);
				{
				setState(887); enumDeclaration();
				setState(889);
				switch ( getInterpreter().adaptivePredict(_input,104,_ctx) ) {
				case 1:
					{
					setState(888); match(SEMI);
					}
					break;
				}
				}
				break;
			case AT:
				enterOuterAlt(_localctx, 5);
				{
				setState(891); annotationTypeDeclaration();
				setState(893);
				switch ( getInterpreter().adaptivePredict(_input,105,_ctx) ) {
				case 1:
					{
					setState(892); match(SEMI);
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
		enterRule(_localctx, 140, RULE_annotationMethodOrConstantRest);
		try {
			setState(899);
			switch ( getInterpreter().adaptivePredict(_input,107,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(897); annotationMethodRest();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(898); annotationConstantRest();
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
		enterRule(_localctx, 142, RULE_annotationMethodRest);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(901); match(Identifier);
			setState(902); match(LPAREN);
			setState(903); match(RPAREN);
			setState(905);
			_la = _input.LA(1);
			if (_la==DEFAULT) {
				{
				setState(904); defaultValue();
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
		enterRule(_localctx, 144, RULE_annotationConstantRest);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(907); variableDeclarators();
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
		enterRule(_localctx, 146, RULE_defaultValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(909); match(DEFAULT);
			setState(910); elementValue();
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
		enterRule(_localctx, 148, RULE_block);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(912); match(LBRACE);
			setState(916);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (ABSTRACT - 58)) | (1L << (ASSERT - 58)) | (1L << (BOOLEAN - 58)) | (1L << (BREAK - 58)) | (1L << (BYTE - 58)) | (1L << (CHAR - 58)) | (1L << (CLASS - 58)) | (1L << (CONTINUE - 58)) | (1L << (DO - 58)) | (1L << (DOUBLE - 58)) | (1L << (ENUM - 58)) | (1L << (FINAL - 58)) | (1L << (FLOAT - 58)) | (1L << (FOR - 58)) | (1L << (IF - 58)) | (1L << (INT - 58)) | (1L << (INTERFACE - 58)) | (1L << (LONG - 58)) | (1L << (NEW - 58)) | (1L << (PRIVATE - 58)) | (1L << (PROTECTED - 58)) | (1L << (PUBLIC - 58)) | (1L << (RETURN - 58)) | (1L << (SHORT - 58)) | (1L << (STATIC - 58)) | (1L << (STRICTFP - 58)) | (1L << (SUPER - 58)) | (1L << (SWITCH - 58)) | (1L << (SYNCHRONIZED - 58)) | (1L << (THIS - 58)) | (1L << (THROW - 58)) | (1L << (TRY - 58)) | (1L << (VOID - 58)) | (1L << (WHILE - 58)) | (1L << (IntegerLiteral - 58)) | (1L << (FloatingPointLiteral - 58)) | (1L << (BooleanLiteral - 58)) | (1L << (CharacterLiteral - 58)) | (1L << (StringLiteral - 58)) | (1L << (NullLiteral - 58)) | (1L << (LPAREN - 58)) | (1L << (LBRACE - 58)) | (1L << (SEMI - 58)))) != 0) || ((((_la - 126)) & ~0x3f) == 0 && ((1L << (_la - 126)) & ((1L << (LT - 126)) | (1L << (BANG - 126)) | (1L << (TILDE - 126)) | (1L << (INC - 126)) | (1L << (DEC - 126)) | (1L << (ADD - 126)) | (1L << (SUB - 126)) | (1L << (Identifier - 126)))) != 0)) {
				{
				{
				setState(913); blockStatement();
				}
				}
				setState(918);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(919); match(RBRACE);
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
		enterRule(_localctx, 150, RULE_blockStatement);
		try {
			setState(924);
			switch ( getInterpreter().adaptivePredict(_input,110,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(921); localVariableDeclarationStatement();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(922); statement();
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(923); typeDeclaration();
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
		enterRule(_localctx, 152, RULE_localVariableDeclarationStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(926); localVariableDeclaration();
			setState(927); match(SEMI);
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
		enterRule(_localctx, 154, RULE_localVariableDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(932);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==FINAL) {
				{
				{
				setState(929); variableModifier();
				}
				}
				setState(934);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(935); type();
			setState(936); variableDeclarators();
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
		enterRule(_localctx, 156, RULE_statement);
		int _la;
		try {
			int _alt;
			setState(1042);
			switch ( getInterpreter().adaptivePredict(_input,124,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(938); block();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(939); match(ASSERT);
				setState(940); expression(0);
				setState(943);
				_la = _input.LA(1);
				if (_la==COLON) {
					{
					setState(941); match(COLON);
					setState(942); expression(0);
					}
				}

				setState(945); match(SEMI);
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(947); match(IF);
				setState(948); parExpression();
				setState(949); statement();
				setState(952);
				switch ( getInterpreter().adaptivePredict(_input,113,_ctx) ) {
				case 1:
					{
					setState(950); match(ELSE);
					setState(951); statement();
					}
					break;
				}
				}
				break;

			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(954); match(FOR);
				setState(955); match(LPAREN);
				setState(956); forControl();
				setState(957); match(RPAREN);
				setState(958); statement();
				}
				break;

			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(960); match(WHILE);
				setState(961); parExpression();
				setState(962); statement();
				}
				break;

			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(964); match(DO);
				setState(965); statement();
				setState(966); match(WHILE);
				setState(967); parExpression();
				setState(968); match(SEMI);
				}
				break;

			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(970); match(TRY);
				setState(971); block();
				setState(981);
				switch (_input.LA(1)) {
				case CATCH:
					{
					setState(973); 
					_errHandler.sync(this);
					_la = _input.LA(1);
					do {
						{
						{
						setState(972); catchClause();
						}
						}
						setState(975); 
						_errHandler.sync(this);
						_la = _input.LA(1);
					} while ( _la==CATCH );
					setState(978);
					_la = _input.LA(1);
					if (_la==FINALLY) {
						{
						setState(977); finallyBlock();
						}
					}

					}
					break;
				case FINALLY:
					{
					setState(980); finallyBlock();
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
				setState(983); match(TRY);
				setState(984); resourceSpecification();
				setState(985); block();
				setState(989);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==CATCH) {
					{
					{
					setState(986); catchClause();
					}
					}
					setState(991);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(993);
				_la = _input.LA(1);
				if (_la==FINALLY) {
					{
					setState(992); finallyBlock();
					}
				}

				}
				break;

			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(995); match(SWITCH);
				setState(996); parExpression();
				setState(997); match(LBRACE);
				setState(1001);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,119,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(998); switchBlockStatementGroup();
						}
						} 
					}
					setState(1003);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,119,_ctx);
				}
				setState(1007);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==CASE || _la==DEFAULT) {
					{
					{
					setState(1004); switchLabel();
					}
					}
					setState(1009);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(1010); match(RBRACE);
				}
				break;

			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(1012); match(SYNCHRONIZED);
				setState(1013); parExpression();
				setState(1014); block();
				}
				break;

			case 11:
				enterOuterAlt(_localctx, 11);
				{
				setState(1016); match(RETURN);
				setState(1018);
				_la = _input.LA(1);
				if (((((_la - 61)) & ~0x3f) == 0 && ((1L << (_la - 61)) & ((1L << (BOOLEAN - 61)) | (1L << (BYTE - 61)) | (1L << (CHAR - 61)) | (1L << (DOUBLE - 61)) | (1L << (FLOAT - 61)) | (1L << (INT - 61)) | (1L << (LONG - 61)) | (1L << (NEW - 61)) | (1L << (SHORT - 61)) | (1L << (SUPER - 61)) | (1L << (THIS - 61)) | (1L << (VOID - 61)) | (1L << (IntegerLiteral - 61)) | (1L << (FloatingPointLiteral - 61)) | (1L << (BooleanLiteral - 61)) | (1L << (CharacterLiteral - 61)) | (1L << (StringLiteral - 61)) | (1L << (NullLiteral - 61)) | (1L << (LPAREN - 61)))) != 0) || ((((_la - 126)) & ~0x3f) == 0 && ((1L << (_la - 126)) & ((1L << (LT - 126)) | (1L << (BANG - 126)) | (1L << (TILDE - 126)) | (1L << (INC - 126)) | (1L << (DEC - 126)) | (1L << (ADD - 126)) | (1L << (SUB - 126)) | (1L << (Identifier - 126)))) != 0)) {
					{
					setState(1017); expression(0);
					}
				}

				setState(1020); match(SEMI);
				}
				break;

			case 12:
				enterOuterAlt(_localctx, 12);
				{
				setState(1021); match(THROW);
				setState(1022); expression(0);
				setState(1023); match(SEMI);
				}
				break;

			case 13:
				enterOuterAlt(_localctx, 13);
				{
				setState(1025); match(BREAK);
				setState(1027);
				_la = _input.LA(1);
				if (_la==Identifier) {
					{
					setState(1026); match(Identifier);
					}
				}

				setState(1029); match(SEMI);
				}
				break;

			case 14:
				enterOuterAlt(_localctx, 14);
				{
				setState(1030); match(CONTINUE);
				setState(1032);
				_la = _input.LA(1);
				if (_la==Identifier) {
					{
					setState(1031); match(Identifier);
					}
				}

				setState(1034); match(SEMI);
				}
				break;

			case 15:
				enterOuterAlt(_localctx, 15);
				{
				setState(1035); match(SEMI);
				}
				break;

			case 16:
				enterOuterAlt(_localctx, 16);
				{
				setState(1036); statementExpression();
				setState(1037); match(SEMI);
				}
				break;

			case 17:
				enterOuterAlt(_localctx, 17);
				{
				setState(1039); match(Identifier);
				setState(1040); match(COLON);
				setState(1041); statement();
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
		enterRule(_localctx, 158, RULE_catchClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1044); match(CATCH);
			setState(1045); match(LPAREN);
			setState(1049);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==FINAL) {
				{
				{
				setState(1046); variableModifier();
				}
				}
				setState(1051);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1052); catchType();
			setState(1053); match(Identifier);
			setState(1054); match(RPAREN);
			setState(1055); block();
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
		enterRule(_localctx, 160, RULE_catchType);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1057); qualifiedName();
			setState(1062);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==BITOR) {
				{
				{
				setState(1058); match(BITOR);
				setState(1059); qualifiedName();
				}
				}
				setState(1064);
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
		enterRule(_localctx, 162, RULE_finallyBlock);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1065); match(FINALLY);
			setState(1066); block();
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
		enterRule(_localctx, 164, RULE_resourceSpecification);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1068); match(LPAREN);
			setState(1069); resources();
			setState(1071);
			_la = _input.LA(1);
			if (_la==SEMI) {
				{
				setState(1070); match(SEMI);
				}
			}

			setState(1073); match(RPAREN);
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
		enterRule(_localctx, 166, RULE_resources);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1075); resource();
			setState(1080);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,128,_ctx);
			while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1076); match(SEMI);
					setState(1077); resource();
					}
					} 
				}
				setState(1082);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,128,_ctx);
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
		enterRule(_localctx, 168, RULE_resource);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1086);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==FINAL) {
				{
				{
				setState(1083); variableModifier();
				}
				}
				setState(1088);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1089); classOrInterfaceType();
			setState(1090); variableDeclaratorId();
			setState(1091); match(ASSIGN);
			setState(1092); expression(0);
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
		enterRule(_localctx, 170, RULE_switchBlockStatementGroup);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1095); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(1094); switchLabel();
				}
				}
				setState(1097); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==CASE || _la==DEFAULT );
			setState(1100); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(1099); blockStatement();
				}
				}
				setState(1102); 
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
		enterRule(_localctx, 172, RULE_switchLabel);
		try {
			setState(1114);
			switch ( getInterpreter().adaptivePredict(_input,132,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1104); match(CASE);
				setState(1105); constantExpression();
				setState(1106); match(COLON);
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1108); match(CASE);
				setState(1109); enumConstantName();
				setState(1110); match(COLON);
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1112); match(DEFAULT);
				setState(1113); match(COLON);
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
		enterRule(_localctx, 174, RULE_forControl);
		int _la;
		try {
			setState(1128);
			switch ( getInterpreter().adaptivePredict(_input,136,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1116); enhancedForControl();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1118);
				_la = _input.LA(1);
				if (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (BOOLEAN - 58)) | (1L << (BYTE - 58)) | (1L << (CHAR - 58)) | (1L << (DOUBLE - 58)) | (1L << (FINAL - 58)) | (1L << (FLOAT - 58)) | (1L << (INT - 58)) | (1L << (LONG - 58)) | (1L << (NEW - 58)) | (1L << (SHORT - 58)) | (1L << (SUPER - 58)) | (1L << (THIS - 58)) | (1L << (VOID - 58)) | (1L << (IntegerLiteral - 58)) | (1L << (FloatingPointLiteral - 58)) | (1L << (BooleanLiteral - 58)) | (1L << (CharacterLiteral - 58)) | (1L << (StringLiteral - 58)) | (1L << (NullLiteral - 58)) | (1L << (LPAREN - 58)))) != 0) || ((((_la - 126)) & ~0x3f) == 0 && ((1L << (_la - 126)) & ((1L << (LT - 126)) | (1L << (BANG - 126)) | (1L << (TILDE - 126)) | (1L << (INC - 126)) | (1L << (DEC - 126)) | (1L << (ADD - 126)) | (1L << (SUB - 126)) | (1L << (Identifier - 126)))) != 0)) {
					{
					setState(1117); forInit();
					}
				}

				setState(1120); match(SEMI);
				setState(1122);
				_la = _input.LA(1);
				if (((((_la - 61)) & ~0x3f) == 0 && ((1L << (_la - 61)) & ((1L << (BOOLEAN - 61)) | (1L << (BYTE - 61)) | (1L << (CHAR - 61)) | (1L << (DOUBLE - 61)) | (1L << (FLOAT - 61)) | (1L << (INT - 61)) | (1L << (LONG - 61)) | (1L << (NEW - 61)) | (1L << (SHORT - 61)) | (1L << (SUPER - 61)) | (1L << (THIS - 61)) | (1L << (VOID - 61)) | (1L << (IntegerLiteral - 61)) | (1L << (FloatingPointLiteral - 61)) | (1L << (BooleanLiteral - 61)) | (1L << (CharacterLiteral - 61)) | (1L << (StringLiteral - 61)) | (1L << (NullLiteral - 61)) | (1L << (LPAREN - 61)))) != 0) || ((((_la - 126)) & ~0x3f) == 0 && ((1L << (_la - 126)) & ((1L << (LT - 126)) | (1L << (BANG - 126)) | (1L << (TILDE - 126)) | (1L << (INC - 126)) | (1L << (DEC - 126)) | (1L << (ADD - 126)) | (1L << (SUB - 126)) | (1L << (Identifier - 126)))) != 0)) {
					{
					setState(1121); expression(0);
					}
				}

				setState(1124); match(SEMI);
				setState(1126);
				_la = _input.LA(1);
				if (((((_la - 61)) & ~0x3f) == 0 && ((1L << (_la - 61)) & ((1L << (BOOLEAN - 61)) | (1L << (BYTE - 61)) | (1L << (CHAR - 61)) | (1L << (DOUBLE - 61)) | (1L << (FLOAT - 61)) | (1L << (INT - 61)) | (1L << (LONG - 61)) | (1L << (NEW - 61)) | (1L << (SHORT - 61)) | (1L << (SUPER - 61)) | (1L << (THIS - 61)) | (1L << (VOID - 61)) | (1L << (IntegerLiteral - 61)) | (1L << (FloatingPointLiteral - 61)) | (1L << (BooleanLiteral - 61)) | (1L << (CharacterLiteral - 61)) | (1L << (StringLiteral - 61)) | (1L << (NullLiteral - 61)) | (1L << (LPAREN - 61)))) != 0) || ((((_la - 126)) & ~0x3f) == 0 && ((1L << (_la - 126)) & ((1L << (LT - 126)) | (1L << (BANG - 126)) | (1L << (TILDE - 126)) | (1L << (INC - 126)) | (1L << (DEC - 126)) | (1L << (ADD - 126)) | (1L << (SUB - 126)) | (1L << (Identifier - 126)))) != 0)) {
					{
					setState(1125); forUpdate();
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
		enterRule(_localctx, 176, RULE_forInit);
		try {
			setState(1132);
			switch ( getInterpreter().adaptivePredict(_input,137,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1130); localVariableDeclaration();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1131); expressionList();
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
		enterRule(_localctx, 178, RULE_enhancedForControl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1137);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==FINAL) {
				{
				{
				setState(1134); variableModifier();
				}
				}
				setState(1139);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1140); type();
			setState(1141); variableDeclaratorId();
			setState(1142); match(COLON);
			setState(1143); expression(0);
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
		enterRule(_localctx, 180, RULE_forUpdate);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1145); expressionList();
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
		enterRule(_localctx, 182, RULE_parExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1147); match(LPAREN);
			setState(1148); expression(0);
			setState(1149); match(RPAREN);
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
		enterRule(_localctx, 184, RULE_expressionList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1151); expression(0);
			setState(1156);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1152); match(COMMA);
				setState(1153); expression(0);
				}
				}
				setState(1158);
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
		enterRule(_localctx, 186, RULE_statementExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1159); expression(0);
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
		enterRule(_localctx, 188, RULE_constantExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1161); expression(0);
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
		int _startState = 190;
		enterRecursionRule(_localctx, 190, RULE_expression, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1176);
			switch ( getInterpreter().adaptivePredict(_input,140,_ctx) ) {
			case 1:
				{
				setState(1164); match(LPAREN);
				setState(1165); type();
				setState(1166); match(RPAREN);
				setState(1167); expression(17);
				}
				break;

			case 2:
				{
				setState(1169);
				_la = _input.LA(1);
				if ( !(((((_la - 137)) & ~0x3f) == 0 && ((1L << (_la - 137)) & ((1L << (INC - 137)) | (1L << (DEC - 137)) | (1L << (ADD - 137)) | (1L << (SUB - 137)))) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				consume();
				setState(1170); expression(15);
				}
				break;

			case 3:
				{
				setState(1171);
				_la = _input.LA(1);
				if ( !(_la==BANG || _la==TILDE) ) {
				_errHandler.recoverInline(this);
				}
				consume();
				setState(1172); expression(14);
				}
				break;

			case 4:
				{
				setState(1173); primary();
				}
				break;

			case 5:
				{
				setState(1174); match(NEW);
				setState(1175); creator();
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(1263);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,145,_ctx);
			while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(1261);
					switch ( getInterpreter().adaptivePredict(_input,144,_ctx) ) {
					case 1:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1178);
						if (!(precpred(_ctx, 13))) throw new FailedPredicateException(this, "precpred(_ctx, 13)");
						setState(1179);
						_la = _input.LA(1);
						if ( !(((((_la - 141)) & ~0x3f) == 0 && ((1L << (_la - 141)) & ((1L << (MUL - 141)) | (1L << (DIV - 141)) | (1L << (MOD - 141)))) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(1180); expression(14);
						}
						break;

					case 2:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1181);
						if (!(precpred(_ctx, 12))) throw new FailedPredicateException(this, "precpred(_ctx, 12)");
						setState(1182);
						_la = _input.LA(1);
						if ( !(_la==ADD || _la==SUB) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(1183); expression(13);
						}
						break;

					case 3:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1184);
						if (!(precpred(_ctx, 11))) throw new FailedPredicateException(this, "precpred(_ctx, 11)");
						setState(1192);
						switch ( getInterpreter().adaptivePredict(_input,141,_ctx) ) {
						case 1:
							{
							setState(1185); match(LT);
							setState(1186); match(LT);
							}
							break;

						case 2:
							{
							setState(1187); match(GT);
							setState(1188); match(GT);
							setState(1189); match(GT);
							}
							break;

						case 3:
							{
							setState(1190); match(GT);
							setState(1191); match(GT);
							}
							break;
						}
						setState(1194); expression(12);
						}
						break;

					case 4:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1195);
						if (!(precpred(_ctx, 10))) throw new FailedPredicateException(this, "precpred(_ctx, 10)");
						setState(1196);
						_la = _input.LA(1);
						if ( !(((((_la - 125)) & ~0x3f) == 0 && ((1L << (_la - 125)) & ((1L << (GT - 125)) | (1L << (LT - 125)) | (1L << (LE - 125)) | (1L << (GE - 125)))) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(1197); expression(11);
						}
						break;

					case 5:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1198);
						if (!(precpred(_ctx, 8))) throw new FailedPredicateException(this, "precpred(_ctx, 8)");
						setState(1199);
						_la = _input.LA(1);
						if ( !(_la==EQUAL || _la==NOTEQUAL) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(1200); expression(9);
						}
						break;

					case 6:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1201);
						if (!(precpred(_ctx, 7))) throw new FailedPredicateException(this, "precpred(_ctx, 7)");
						setState(1202); match(BITAND);
						setState(1203); expression(8);
						}
						break;

					case 7:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1204);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(1205); match(CARET);
						setState(1206); expression(7);
						}
						break;

					case 8:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1207);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(1208); match(BITOR);
						setState(1209); expression(6);
						}
						break;

					case 9:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1210);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(1211); match(AND);
						setState(1212); expression(5);
						}
						break;

					case 10:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1213);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(1214); match(OR);
						setState(1215); expression(4);
						}
						break;

					case 11:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1216);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(1217); match(QUESTION);
						setState(1218); expression(0);
						setState(1219); match(COLON);
						setState(1220); expression(3);
						}
						break;

					case 12:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1222);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(1223);
						_la = _input.LA(1);
						if ( !(((((_la - 124)) & ~0x3f) == 0 && ((1L << (_la - 124)) & ((1L << (ASSIGN - 124)) | (1L << (ADD_ASSIGN - 124)) | (1L << (SUB_ASSIGN - 124)) | (1L << (MUL_ASSIGN - 124)) | (1L << (DIV_ASSIGN - 124)) | (1L << (AND_ASSIGN - 124)) | (1L << (OR_ASSIGN - 124)) | (1L << (XOR_ASSIGN - 124)) | (1L << (MOD_ASSIGN - 124)) | (1L << (LSHIFT_ASSIGN - 124)) | (1L << (RSHIFT_ASSIGN - 124)) | (1L << (URSHIFT_ASSIGN - 124)))) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(1224); expression(1);
						}
						break;

					case 13:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1225);
						if (!(precpred(_ctx, 25))) throw new FailedPredicateException(this, "precpred(_ctx, 25)");
						setState(1226); match(DOT);
						setState(1227); match(Identifier);
						}
						break;

					case 14:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1228);
						if (!(precpred(_ctx, 24))) throw new FailedPredicateException(this, "precpred(_ctx, 24)");
						setState(1229); match(DOT);
						setState(1230); match(THIS);
						}
						break;

					case 15:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1231);
						if (!(precpred(_ctx, 23))) throw new FailedPredicateException(this, "precpred(_ctx, 23)");
						setState(1232); match(DOT);
						setState(1233); match(NEW);
						setState(1235);
						_la = _input.LA(1);
						if (_la==LT) {
							{
							setState(1234); nonWildcardTypeArguments();
							}
						}

						setState(1237); innerCreator();
						}
						break;

					case 16:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1238);
						if (!(precpred(_ctx, 22))) throw new FailedPredicateException(this, "precpred(_ctx, 22)");
						setState(1239); match(DOT);
						setState(1240); match(SUPER);
						setState(1241); superSuffix();
						}
						break;

					case 17:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1242);
						if (!(precpred(_ctx, 21))) throw new FailedPredicateException(this, "precpred(_ctx, 21)");
						setState(1243); match(DOT);
						setState(1244); explicitGenericInvocation();
						}
						break;

					case 18:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1245);
						if (!(precpred(_ctx, 20))) throw new FailedPredicateException(this, "precpred(_ctx, 20)");
						setState(1246); match(LBRACK);
						setState(1247); expression(0);
						setState(1248); match(RBRACK);
						}
						break;

					case 19:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1250);
						if (!(precpred(_ctx, 19))) throw new FailedPredicateException(this, "precpred(_ctx, 19)");
						setState(1251); match(LPAREN);
						setState(1253);
						_la = _input.LA(1);
						if (((((_la - 61)) & ~0x3f) == 0 && ((1L << (_la - 61)) & ((1L << (BOOLEAN - 61)) | (1L << (BYTE - 61)) | (1L << (CHAR - 61)) | (1L << (DOUBLE - 61)) | (1L << (FLOAT - 61)) | (1L << (INT - 61)) | (1L << (LONG - 61)) | (1L << (NEW - 61)) | (1L << (SHORT - 61)) | (1L << (SUPER - 61)) | (1L << (THIS - 61)) | (1L << (VOID - 61)) | (1L << (IntegerLiteral - 61)) | (1L << (FloatingPointLiteral - 61)) | (1L << (BooleanLiteral - 61)) | (1L << (CharacterLiteral - 61)) | (1L << (StringLiteral - 61)) | (1L << (NullLiteral - 61)) | (1L << (LPAREN - 61)))) != 0) || ((((_la - 126)) & ~0x3f) == 0 && ((1L << (_la - 126)) & ((1L << (LT - 126)) | (1L << (BANG - 126)) | (1L << (TILDE - 126)) | (1L << (INC - 126)) | (1L << (DEC - 126)) | (1L << (ADD - 126)) | (1L << (SUB - 126)) | (1L << (Identifier - 126)))) != 0)) {
							{
							setState(1252); expressionList();
							}
						}

						setState(1255); match(RPAREN);
						}
						break;

					case 20:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1256);
						if (!(precpred(_ctx, 16))) throw new FailedPredicateException(this, "precpred(_ctx, 16)");
						setState(1257);
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
						setState(1258);
						if (!(precpred(_ctx, 9))) throw new FailedPredicateException(this, "precpred(_ctx, 9)");
						setState(1259); match(INSTANCEOF);
						setState(1260); type();
						}
						break;
					}
					} 
				}
				setState(1265);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,145,_ctx);
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
		enterRule(_localctx, 192, RULE_primary);
		try {
			setState(1287);
			switch ( getInterpreter().adaptivePredict(_input,147,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1266); match(LPAREN);
				setState(1267); expression(0);
				setState(1268); match(RPAREN);
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1270); match(THIS);
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1271); match(SUPER);
				}
				break;

			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1272); literal();
				}
				break;

			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(1273); match(Identifier);
				}
				break;

			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(1274); type();
				setState(1275); match(DOT);
				setState(1276); match(CLASS);
				}
				break;

			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(1278); match(VOID);
				setState(1279); match(DOT);
				setState(1280); match(CLASS);
				}
				break;

			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(1281); nonWildcardTypeArguments();
				setState(1285);
				switch (_input.LA(1)) {
				case SUPER:
				case Identifier:
					{
					setState(1282); explicitGenericInvocationSuffix();
					}
					break;
				case THIS:
					{
					setState(1283); match(THIS);
					setState(1284); arguments();
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
		enterRule(_localctx, 194, RULE_creator);
		try {
			setState(1298);
			switch (_input.LA(1)) {
			case LT:
				enterOuterAlt(_localctx, 1);
				{
				setState(1289); nonWildcardTypeArguments();
				setState(1290); createdName();
				setState(1291); classCreatorRest();
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
				setState(1293); createdName();
				setState(1296);
				switch (_input.LA(1)) {
				case LBRACK:
					{
					setState(1294); arrayCreatorRest();
					}
					break;
				case LPAREN:
					{
					setState(1295); classCreatorRest();
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
		enterRule(_localctx, 196, RULE_createdName);
		int _la;
		try {
			setState(1315);
			switch (_input.LA(1)) {
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(1300); match(Identifier);
				setState(1302);
				_la = _input.LA(1);
				if (_la==LT) {
					{
					setState(1301); typeArgumentsOrDiamond();
					}
				}

				setState(1311);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==DOT) {
					{
					{
					setState(1304); match(DOT);
					setState(1305); match(Identifier);
					setState(1307);
					_la = _input.LA(1);
					if (_la==LT) {
						{
						setState(1306); typeArgumentsOrDiamond();
						}
					}

					}
					}
					setState(1313);
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
				setState(1314); primitiveType();
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
		enterRule(_localctx, 198, RULE_innerCreator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1317); match(Identifier);
			setState(1319);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(1318); nonWildcardTypeArgumentsOrDiamond();
				}
			}

			setState(1321); classCreatorRest();
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
		enterRule(_localctx, 200, RULE_arrayCreatorRest);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1323); match(LBRACK);
			setState(1351);
			switch (_input.LA(1)) {
			case RBRACK:
				{
				setState(1324); match(RBRACK);
				setState(1329);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==LBRACK) {
					{
					{
					setState(1325); match(LBRACK);
					setState(1326); match(RBRACK);
					}
					}
					setState(1331);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(1332); arrayInitializer();
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
				setState(1333); expression(0);
				setState(1334); match(RBRACK);
				setState(1341);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,156,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1335); match(LBRACK);
						setState(1336); expression(0);
						setState(1337); match(RBRACK);
						}
						} 
					}
					setState(1343);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,156,_ctx);
				}
				setState(1348);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,157,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1344); match(LBRACK);
						setState(1345); match(RBRACK);
						}
						} 
					}
					setState(1350);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,157,_ctx);
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
		enterRule(_localctx, 202, RULE_classCreatorRest);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1353); arguments();
			setState(1355);
			switch ( getInterpreter().adaptivePredict(_input,159,_ctx) ) {
			case 1:
				{
				setState(1354); classBody();
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
		enterRule(_localctx, 204, RULE_explicitGenericInvocation);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1357); nonWildcardTypeArguments();
			setState(1358); explicitGenericInvocationSuffix();
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
		enterRule(_localctx, 206, RULE_nonWildcardTypeArguments);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1360); match(LT);
			setState(1361); typeList();
			setState(1362); match(GT);
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
		enterRule(_localctx, 208, RULE_typeArgumentsOrDiamond);
		try {
			setState(1367);
			switch ( getInterpreter().adaptivePredict(_input,160,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1364); match(LT);
				setState(1365); match(GT);
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1366); typeArguments();
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
		enterRule(_localctx, 210, RULE_nonWildcardTypeArgumentsOrDiamond);
		try {
			setState(1372);
			switch ( getInterpreter().adaptivePredict(_input,161,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1369); match(LT);
				setState(1370); match(GT);
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1371); nonWildcardTypeArguments();
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
		enterRule(_localctx, 212, RULE_superSuffix);
		try {
			setState(1380);
			switch (_input.LA(1)) {
			case LPAREN:
				enterOuterAlt(_localctx, 1);
				{
				setState(1374); arguments();
				}
				break;
			case DOT:
				enterOuterAlt(_localctx, 2);
				{
				setState(1375); match(DOT);
				setState(1376); match(Identifier);
				setState(1378);
				switch ( getInterpreter().adaptivePredict(_input,162,_ctx) ) {
				case 1:
					{
					setState(1377); arguments();
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
		enterRule(_localctx, 214, RULE_explicitGenericInvocationSuffix);
		try {
			setState(1386);
			switch (_input.LA(1)) {
			case SUPER:
				enterOuterAlt(_localctx, 1);
				{
				setState(1382); match(SUPER);
				setState(1383); superSuffix();
				}
				break;
			case Identifier:
				enterOuterAlt(_localctx, 2);
				{
				setState(1384); match(Identifier);
				setState(1385); arguments();
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
		enterRule(_localctx, 216, RULE_arguments);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1388); match(LPAREN);
			setState(1390);
			_la = _input.LA(1);
			if (((((_la - 61)) & ~0x3f) == 0 && ((1L << (_la - 61)) & ((1L << (BOOLEAN - 61)) | (1L << (BYTE - 61)) | (1L << (CHAR - 61)) | (1L << (DOUBLE - 61)) | (1L << (FLOAT - 61)) | (1L << (INT - 61)) | (1L << (LONG - 61)) | (1L << (NEW - 61)) | (1L << (SHORT - 61)) | (1L << (SUPER - 61)) | (1L << (THIS - 61)) | (1L << (VOID - 61)) | (1L << (IntegerLiteral - 61)) | (1L << (FloatingPointLiteral - 61)) | (1L << (BooleanLiteral - 61)) | (1L << (CharacterLiteral - 61)) | (1L << (StringLiteral - 61)) | (1L << (NullLiteral - 61)) | (1L << (LPAREN - 61)))) != 0) || ((((_la - 126)) & ~0x3f) == 0 && ((1L << (_la - 126)) & ((1L << (LT - 126)) | (1L << (BANG - 126)) | (1L << (TILDE - 126)) | (1L << (INC - 126)) | (1L << (DEC - 126)) | (1L << (ADD - 126)) | (1L << (SUB - 126)) | (1L << (Identifier - 126)))) != 0)) {
				{
				setState(1389); expressionList();
				}
			}

			setState(1392); match(RPAREN);
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
		case 4: return typePattern_sempred((TypePatternContext)_localctx, predIndex);

		case 95: return expression_sempred((ExpressionContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean expression_sempred(ExpressionContext _localctx, int predIndex) {
		switch (predIndex) {
		case 2: return precpred(_ctx, 13);

		case 3: return precpred(_ctx, 12);

		case 4: return precpred(_ctx, 11);

		case 5: return precpred(_ctx, 10);

		case 6: return precpred(_ctx, 8);

		case 7: return precpred(_ctx, 7);

		case 8: return precpred(_ctx, 6);

		case 9: return precpred(_ctx, 5);

		case 10: return precpred(_ctx, 4);

		case 11: return precpred(_ctx, 3);

		case 12: return precpred(_ctx, 2);

		case 13: return precpred(_ctx, 1);

		case 14: return precpred(_ctx, 25);

		case 15: return precpred(_ctx, 24);

		case 16: return precpred(_ctx, 23);

		case 17: return precpred(_ctx, 22);

		case 18: return precpred(_ctx, 21);

		case 19: return precpred(_ctx, 20);

		case 20: return precpred(_ctx, 19);

		case 21: return precpred(_ctx, 16);

		case 22: return precpred(_ctx, 9);
		}
		return true;
	}
	private boolean typePattern_sempred(TypePatternContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0: return precpred(_ctx, 2);

		case 1: return precpred(_ctx, 1);
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3\u00b3\u0575\4\2\t"+
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
		"k\4l\tl\4m\tm\4n\tn\3\2\3\2\3\2\3\2\3\3\3\3\5\3\u00e3\n\3\3\3\3\3\3\4"+
		"\3\4\3\4\7\4\u00ea\n\4\f\4\16\4\u00ed\13\4\3\4\3\4\3\4\7\4\u00f2\n\4\f"+
		"\4\16\4\u00f5\13\4\3\4\3\4\3\4\5\4\u00fa\n\4\3\5\3\5\3\5\7\5\u00ff\n\5"+
		"\f\5\16\5\u0102\13\5\3\5\3\5\3\5\5\5\u0107\n\5\3\6\3\6\3\6\3\6\5\6\u010d"+
		"\n\6\3\6\3\6\3\6\3\6\3\6\3\6\7\6\u0115\n\6\f\6\16\6\u0118\13\6\3\7\6\7"+
		"\u011b\n\7\r\7\16\7\u011c\3\7\5\7\u0120\n\7\3\b\3\b\3\b\7\b\u0125\n\b"+
		"\f\b\16\b\u0128\13\b\3\b\5\b\u012b\n\b\3\b\3\b\3\b\7\b\u0130\n\b\f\b\16"+
		"\b\u0133\13\b\3\b\5\b\u0136\n\b\5\b\u0138\n\b\3\t\3\t\3\t\3\t\3\t\5\t"+
		"\u013f\n\t\3\n\5\n\u0142\n\n\3\n\7\n\u0145\n\n\f\n\16\n\u0148\13\n\3\n"+
		"\7\n\u014b\n\n\f\n\16\n\u014e\13\n\3\n\3\n\3\13\7\13\u0153\n\13\f\13\16"+
		"\13\u0156\13\13\3\13\3\13\3\13\3\13\3\f\3\f\5\f\u015e\n\f\3\f\3\f\3\f"+
		"\5\f\u0163\n\f\3\f\3\f\3\r\7\r\u0168\n\r\f\r\16\r\u016b\13\r\3\r\3\r\7"+
		"\r\u016f\n\r\f\r\16\r\u0172\13\r\3\r\3\r\7\r\u0176\n\r\f\r\16\r\u0179"+
		"\13\r\3\r\3\r\7\r\u017d\n\r\f\r\16\r\u0180\13\r\3\r\3\r\5\r\u0184\n\r"+
		"\3\16\3\16\5\16\u0188\n\16\3\17\3\17\5\17\u018c\n\17\3\20\3\20\5\20\u0190"+
		"\n\20\3\21\3\21\3\21\5\21\u0195\n\21\3\21\3\21\5\21\u0199\n\21\3\21\3"+
		"\21\5\21\u019d\n\21\3\21\3\21\3\22\3\22\3\22\3\22\7\22\u01a5\n\22\f\22"+
		"\16\22\u01a8\13\22\3\22\3\22\3\23\3\23\3\23\5\23\u01af\n\23\3\24\3\24"+
		"\3\24\7\24\u01b4\n\24\f\24\16\24\u01b7\13\24\3\25\3\25\3\25\3\25\5\25"+
		"\u01bd\n\25\3\25\3\25\5\25\u01c1\n\25\3\25\5\25\u01c4\n\25\3\25\5\25\u01c7"+
		"\n\25\3\25\3\25\3\26\3\26\3\26\7\26\u01ce\n\26\f\26\16\26\u01d1\13\26"+
		"\3\27\7\27\u01d4\n\27\f\27\16\27\u01d7\13\27\3\27\3\27\5\27\u01db\n\27"+
		"\3\27\5\27\u01de\n\27\3\30\3\30\7\30\u01e2\n\30\f\30\16\30\u01e5\13\30"+
		"\3\31\3\31\3\31\5\31\u01ea\n\31\3\31\3\31\5\31\u01ee\n\31\3\31\3\31\3"+
		"\32\3\32\3\32\7\32\u01f5\n\32\f\32\16\32\u01f8\13\32\3\33\3\33\7\33\u01fc"+
		"\n\33\f\33\16\33\u01ff\13\33\3\33\3\33\3\34\3\34\7\34\u0205\n\34\f\34"+
		"\16\34\u0208\13\34\3\34\3\34\3\35\3\35\5\35\u020e\n\35\3\35\3\35\7\35"+
		"\u0212\n\35\f\35\16\35\u0215\13\35\3\35\5\35\u0218\n\35\3\36\3\36\3\36"+
		"\3\36\3\36\3\36\3\36\3\36\3\36\5\36\u0223\n\36\3\37\3\37\5\37\u0227\n"+
		"\37\3\37\3\37\3\37\3\37\7\37\u022d\n\37\f\37\16\37\u0230\13\37\3\37\3"+
		"\37\5\37\u0234\n\37\3\37\3\37\5\37\u0238\n\37\3 \3 \3 \3!\3!\3!\3!\5!"+
		"\u0241\n!\3!\3!\3\"\3\"\3\"\3#\3#\3#\3#\3$\7$\u024d\n$\f$\16$\u0250\13"+
		"$\3$\3$\5$\u0254\n$\3%\3%\3%\3%\3%\3%\3%\5%\u025d\n%\3&\3&\3&\3&\7&\u0263"+
		"\n&\f&\16&\u0266\13&\3&\3&\3\'\3\'\3\'\7\'\u026d\n\'\f\'\16\'\u0270\13"+
		"\'\3\'\3\'\3\'\3(\3(\5(\u0277\n(\3(\3(\3(\3(\7(\u027d\n(\f(\16(\u0280"+
		"\13(\3(\3(\5(\u0284\n(\3(\3(\3)\3)\3)\3*\3*\3*\7*\u028e\n*\f*\16*\u0291"+
		"\13*\3+\3+\3+\5+\u0296\n+\3,\3,\3,\7,\u029b\n,\f,\16,\u029e\13,\3-\3-"+
		"\5-\u02a2\n-\3.\3.\3.\3.\7.\u02a8\n.\f.\16.\u02ab\13.\3.\5.\u02ae\n.\5"+
		".\u02b0\n.\3.\3.\3/\3/\3\60\3\60\3\60\7\60\u02b9\n\60\f\60\16\60\u02bc"+
		"\13\60\3\60\3\60\3\60\7\60\u02c1\n\60\f\60\16\60\u02c4\13\60\5\60\u02c6"+
		"\n\60\3\61\3\61\5\61\u02ca\n\61\3\61\3\61\3\61\5\61\u02cf\n\61\7\61\u02d1"+
		"\n\61\f\61\16\61\u02d4\13\61\3\62\3\62\3\63\3\63\3\63\3\63\7\63\u02dc"+
		"\n\63\f\63\16\63\u02df\13\63\3\63\3\63\3\64\3\64\3\64\3\64\5\64\u02e7"+
		"\n\64\5\64\u02e9\n\64\3\65\3\65\3\65\7\65\u02ee\n\65\f\65\16\65\u02f1"+
		"\13\65\3\66\3\66\5\66\u02f5\n\66\3\66\3\66\3\67\3\67\3\67\7\67\u02fc\n"+
		"\67\f\67\16\67\u02ff\13\67\3\67\3\67\5\67\u0303\n\67\3\67\5\67\u0306\n"+
		"\67\38\78\u0309\n8\f8\168\u030c\138\38\38\38\39\79\u0312\n9\f9\169\u0315"+
		"\139\39\39\39\39\3:\3:\3;\3;\3<\3<\3<\7<\u0322\n<\f<\16<\u0325\13<\3="+
		"\3=\3>\3>\3>\3>\3>\5>\u032e\n>\3>\5>\u0331\n>\3?\3?\3@\3@\3@\7@\u0338"+
		"\n@\f@\16@\u033b\13@\3A\3A\3A\3A\3B\3B\3B\5B\u0344\nB\3C\3C\3C\3C\7C\u034a"+
		"\nC\fC\16C\u034d\13C\5C\u034f\nC\3C\5C\u0352\nC\3C\3C\3D\3D\3D\3D\3D\3"+
		"E\3E\7E\u035d\nE\fE\16E\u0360\13E\3E\3E\3F\7F\u0365\nF\fF\16F\u0368\13"+
		"F\3F\3F\5F\u036c\nF\3G\3G\3G\3G\3G\3G\5G\u0374\nG\3G\3G\5G\u0378\nG\3"+
		"G\3G\5G\u037c\nG\3G\3G\5G\u0380\nG\5G\u0382\nG\3H\3H\5H\u0386\nH\3I\3"+
		"I\3I\3I\5I\u038c\nI\3J\3J\3K\3K\3K\3L\3L\7L\u0395\nL\fL\16L\u0398\13L"+
		"\3L\3L\3M\3M\3M\5M\u039f\nM\3N\3N\3N\3O\7O\u03a5\nO\fO\16O\u03a8\13O\3"+
		"O\3O\3O\3P\3P\3P\3P\3P\5P\u03b2\nP\3P\3P\3P\3P\3P\3P\3P\5P\u03bb\nP\3"+
		"P\3P\3P\3P\3P\3P\3P\3P\3P\3P\3P\3P\3P\3P\3P\3P\3P\3P\3P\6P\u03d0\nP\r"+
		"P\16P\u03d1\3P\5P\u03d5\nP\3P\5P\u03d8\nP\3P\3P\3P\3P\7P\u03de\nP\fP\16"+
		"P\u03e1\13P\3P\5P\u03e4\nP\3P\3P\3P\3P\7P\u03ea\nP\fP\16P\u03ed\13P\3"+
		"P\7P\u03f0\nP\fP\16P\u03f3\13P\3P\3P\3P\3P\3P\3P\3P\3P\5P\u03fd\nP\3P"+
		"\3P\3P\3P\3P\3P\3P\5P\u0406\nP\3P\3P\3P\5P\u040b\nP\3P\3P\3P\3P\3P\3P"+
		"\3P\3P\5P\u0415\nP\3Q\3Q\3Q\7Q\u041a\nQ\fQ\16Q\u041d\13Q\3Q\3Q\3Q\3Q\3"+
		"Q\3R\3R\3R\7R\u0427\nR\fR\16R\u042a\13R\3S\3S\3S\3T\3T\3T\5T\u0432\nT"+
		"\3T\3T\3U\3U\3U\7U\u0439\nU\fU\16U\u043c\13U\3V\7V\u043f\nV\fV\16V\u0442"+
		"\13V\3V\3V\3V\3V\3V\3W\6W\u044a\nW\rW\16W\u044b\3W\6W\u044f\nW\rW\16W"+
		"\u0450\3X\3X\3X\3X\3X\3X\3X\3X\3X\3X\5X\u045d\nX\3Y\3Y\5Y\u0461\nY\3Y"+
		"\3Y\5Y\u0465\nY\3Y\3Y\5Y\u0469\nY\5Y\u046b\nY\3Z\3Z\5Z\u046f\nZ\3[\7["+
		"\u0472\n[\f[\16[\u0475\13[\3[\3[\3[\3[\3[\3\\\3\\\3]\3]\3]\3]\3^\3^\3"+
		"^\7^\u0485\n^\f^\16^\u0488\13^\3_\3_\3`\3`\3a\3a\3a\3a\3a\3a\3a\3a\3a"+
		"\3a\3a\3a\3a\5a\u049b\na\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\5a"+
		"\u04ab\na\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a"+
		"\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\5a\u04d6"+
		"\na\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\3a\5a\u04e8\na\3a\3a"+
		"\3a\3a\3a\3a\7a\u04f0\na\fa\16a\u04f3\13a\3b\3b\3b\3b\3b\3b\3b\3b\3b\3"+
		"b\3b\3b\3b\3b\3b\3b\3b\3b\3b\5b\u0508\nb\5b\u050a\nb\3c\3c\3c\3c\3c\3"+
		"c\3c\5c\u0513\nc\5c\u0515\nc\3d\3d\5d\u0519\nd\3d\3d\3d\5d\u051e\nd\7"+
		"d\u0520\nd\fd\16d\u0523\13d\3d\5d\u0526\nd\3e\3e\5e\u052a\ne\3e\3e\3f"+
		"\3f\3f\3f\7f\u0532\nf\ff\16f\u0535\13f\3f\3f\3f\3f\3f\3f\3f\7f\u053e\n"+
		"f\ff\16f\u0541\13f\3f\3f\7f\u0545\nf\ff\16f\u0548\13f\5f\u054a\nf\3g\3"+
		"g\5g\u054e\ng\3h\3h\3h\3i\3i\3i\3i\3j\3j\3j\5j\u055a\nj\3k\3k\3k\5k\u055f"+
		"\nk\3l\3l\3l\3l\5l\u0565\nl\5l\u0567\nl\3m\3m\3m\3m\5m\u056d\nm\3n\3n"+
		"\5n\u0571\nn\3n\3n\3n\2\4\n\u00c0o\2\4\6\b\n\f\16\20\22\24\26\30\32\34"+
		"\36 \"$&(*,.\60\62\64\668:<>@BDFHJLNPRTVXZ\\^`bdfhjlnprtvxz|~\u0080\u0082"+
		"\u0084\u0086\u0088\u008a\u008c\u008e\u0090\u0092\u0094\u0096\u0098\u009a"+
		"\u009c\u009e\u00a0\u00a2\u00a4\u00a6\u00a8\u00aa\u00ac\u00ae\u00b0\u00b2"+
		"\u00b4\u00b6\u00b8\u00ba\u00bc\u00be\u00c0\u00c2\u00c4\u00c6\u00c8\u00ca"+
		"\u00cc\u00ce\u00d0\u00d2\u00d4\u00d6\u00d8\u00da\2\20\6\2\3\3}}\u008f"+
		"\u008f\u00a0\u00a0\6\2ZZffjjmm\6\2==NN]_bc\n\2??AADDJJPPWWYYaa\4\2MMd"+
		"d\3\2ot\3\2\u008b\u008e\3\2\u0081\u0082\4\2\u008f\u0090\u0094\u0094\3"+
		"\2\u008d\u008e\4\2\177\u0080\u0086\u0087\4\2\u0085\u0085\u0088\u0088\4"+
		"\2~~\u0095\u009f\3\2\u008b\u008c\u05f1\2\u00dc\3\2\2\2\4\u00e0\3\2\2\2"+
		"\6\u00f9\3\2\2\2\b\u0106\3\2\2\2\n\u010c\3\2\2\2\f\u011f\3\2\2\2\16\u0137"+
		"\3\2\2\2\20\u013e\3\2\2\2\22\u0141\3\2\2\2\24\u0154\3\2\2\2\26\u015b\3"+
		"\2\2\2\30\u0183\3\2\2\2\32\u0187\3\2\2\2\34\u018b\3\2\2\2\36\u018f\3\2"+
		"\2\2 \u0191\3\2\2\2\"\u01a0\3\2\2\2$\u01ab\3\2\2\2&\u01b0\3\2\2\2(\u01b8"+
		"\3\2\2\2*\u01ca\3\2\2\2,\u01d5\3\2\2\2.\u01df\3\2\2\2\60\u01e6\3\2\2\2"+
		"\62\u01f1\3\2\2\2\64\u01f9\3\2\2\2\66\u0202\3\2\2\28\u0217\3\2\2\2:\u0222"+
		"\3\2\2\2<\u0226\3\2\2\2>\u0239\3\2\2\2@\u023c\3\2\2\2B\u0244\3\2\2\2D"+
		"\u0247\3\2\2\2F\u0253\3\2\2\2H\u025c\3\2\2\2J\u025e\3\2\2\2L\u0269\3\2"+
		"\2\2N\u0276\3\2\2\2P\u0287\3\2\2\2R\u028a\3\2\2\2T\u0292\3\2\2\2V\u0297"+
		"\3\2\2\2X\u02a1\3\2\2\2Z\u02a3\3\2\2\2\\\u02b3\3\2\2\2^\u02c5\3\2\2\2"+
		"`\u02c7\3\2\2\2b\u02d5\3\2\2\2d\u02d7\3\2\2\2f\u02e8\3\2\2\2h\u02ea\3"+
		"\2\2\2j\u02f2\3\2\2\2l\u0305\3\2\2\2n\u030a\3\2\2\2p\u0313\3\2\2\2r\u031a"+
		"\3\2\2\2t\u031c\3\2\2\2v\u031e\3\2\2\2x\u0326\3\2\2\2z\u0328\3\2\2\2|"+
		"\u0332\3\2\2\2~\u0334\3\2\2\2\u0080\u033c\3\2\2\2\u0082\u0343\3\2\2\2"+
		"\u0084\u0345\3\2\2\2\u0086\u0355\3\2\2\2\u0088\u035a\3\2\2\2\u008a\u036b"+
		"\3\2\2\2\u008c\u0381\3\2\2\2\u008e\u0385\3\2\2\2\u0090\u0387\3\2\2\2\u0092"+
		"\u038d\3\2\2\2\u0094\u038f\3\2\2\2\u0096\u0392\3\2\2\2\u0098\u039e\3\2"+
		"\2\2\u009a\u03a0\3\2\2\2\u009c\u03a6\3\2\2\2\u009e\u0414\3\2\2\2\u00a0"+
		"\u0416\3\2\2\2\u00a2\u0423\3\2\2\2\u00a4\u042b\3\2\2\2\u00a6\u042e\3\2"+
		"\2\2\u00a8\u0435\3\2\2\2\u00aa\u0440\3\2\2\2\u00ac\u0449\3\2\2\2\u00ae"+
		"\u045c\3\2\2\2\u00b0\u046a\3\2\2\2\u00b2\u046e\3\2\2\2\u00b4\u0473\3\2"+
		"\2\2\u00b6\u047b\3\2\2\2\u00b8\u047d\3\2\2\2\u00ba\u0481\3\2\2\2\u00bc"+
		"\u0489\3\2\2\2\u00be\u048b\3\2\2\2\u00c0\u049a\3\2\2\2\u00c2\u0509\3\2"+
		"\2\2\u00c4\u0514\3\2\2\2\u00c6\u0525\3\2\2\2\u00c8\u0527\3\2\2\2\u00ca"+
		"\u052d\3\2\2\2\u00cc\u054b\3\2\2\2\u00ce\u054f\3\2\2\2\u00d0\u0552\3\2"+
		"\2\2\u00d2\u0559\3\2\2\2\u00d4\u055e\3\2\2\2\u00d6\u0566\3\2\2\2\u00d8"+
		"\u056c\3\2\2\2\u00da\u056e\3\2\2\2\u00dc\u00dd\5\n\6\2\u00dd\u00de\5\16"+
		"\b\2\u00de\u00df\5\4\3\2\u00df\3\3\2\2\2\u00e0\u00e2\7u\2\2\u00e1\u00e3"+
		"\5\6\4\2\u00e2\u00e1\3\2\2\2\u00e2\u00e3\3\2\2\2\u00e3\u00e4\3\2\2\2\u00e4"+
		"\u00e5\7v\2\2\u00e5\5\3\2\2\2\u00e6\u00eb\7\3\2\2\u00e7\u00e8\7|\2\2\u00e8"+
		"\u00ea\5\b\5\2\u00e9\u00e7\3\2\2\2\u00ea\u00ed\3\2\2\2\u00eb\u00e9\3\2"+
		"\2\2\u00eb\u00ec\3\2\2\2\u00ec\u00fa\3\2\2\2\u00ed\u00eb\3\2\2\2\u00ee"+
		"\u00f3\5\20\t\2\u00ef\u00f0\7|\2\2\u00f0\u00f2\5\6\4\2\u00f1\u00ef\3\2"+
		"\2\2\u00f2\u00f5\3\2\2\2\u00f3\u00f1\3\2\2\2\u00f3\u00f4\3\2\2\2\u00f4"+
		"\u00fa\3\2\2\2\u00f5\u00f3\3\2\2\2\u00f6\u00f7\5\n\6\2\u00f7\u00f8\7\u00a1"+
		"\2\2\u00f8\u00fa\3\2\2\2\u00f9\u00e6\3\2\2\2\u00f9\u00ee\3\2\2\2\u00f9"+
		"\u00f6\3\2\2\2\u00fa\7\3\2\2\2\u00fb\u0100\5\20\t\2\u00fc\u00fd\7|\2\2"+
		"\u00fd\u00ff\5\b\5\2\u00fe\u00fc\3\2\2\2\u00ff\u0102\3\2\2\2\u0100\u00fe"+
		"\3\2\2\2\u0100\u0101\3\2\2\2\u0101\u0107\3\2\2\2\u0102\u0100\3\2\2\2\u0103"+
		"\u0104\5\n\6\2\u0104\u0105\7\u00a1\2\2\u0105\u0107\3\2\2\2\u0106\u00fb"+
		"\3\2\2\2\u0106\u0103\3\2\2\2\u0107\t\3\2\2\2\u0108\u0109\b\6\1\2\u0109"+
		"\u010a\7\u0081\2\2\u010a\u010d\5\n\6\5\u010b\u010d\5\f\7\2\u010c\u0108"+
		"\3\2\2\2\u010c\u010b\3\2\2\2\u010d\u0116\3\2\2\2\u010e\u010f\f\4\2\2\u010f"+
		"\u0110\7\u0089\2\2\u0110\u0115\5\n\6\5\u0111\u0112\f\3\2\2\u0112\u0113"+
		"\7\u008a\2\2\u0113\u0115\5\n\6\4\u0114\u010e\3\2\2\2\u0114\u0111\3\2\2"+
		"\2\u0115\u0118\3\2\2\2\u0116\u0114\3\2\2\2\u0116\u0117\3\2\2\2\u0117\13"+
		"\3\2\2\2\u0118\u0116\3\2\2\2\u0119\u011b\t\2\2\2\u011a\u0119\3\2\2\2\u011b"+
		"\u011c\3\2\2\2\u011c\u011a\3\2\2\2\u011c\u011d\3\2\2\2\u011d\u0120\3\2"+
		"\2\2\u011e\u0120\7l\2\2\u011f\u011a\3\2\2\2\u011f\u011e\3\2\2\2\u0120"+
		"\r\3\2\2\2\u0121\u0126\7\u00a0\2\2\u0122\u0123\7\u008f\2\2\u0123\u0125"+
		"\7\u00a0\2\2\u0124\u0122\3\2\2\2\u0125\u0128\3\2\2\2\u0126\u0124\3\2\2"+
		"\2\u0126\u0127\3\2\2\2\u0127\u012a\3\2\2\2\u0128\u0126\3\2\2\2\u0129\u012b"+
		"\7\u008f\2\2\u012a\u0129\3\2\2\2\u012a\u012b\3\2\2\2\u012b\u0138\3\2\2"+
		"\2\u012c\u0131\7\u008f\2\2\u012d\u012e\7\u00a0\2\2\u012e\u0130\7\u008f"+
		"\2\2\u012f\u012d\3\2\2\2\u0130\u0133\3\2\2\2\u0131\u012f\3\2\2\2\u0131"+
		"\u0132\3\2\2\2\u0132\u0135\3\2\2\2\u0133\u0131\3\2\2\2\u0134\u0136\7\u00a0"+
		"\2\2\u0135\u0134\3\2\2\2\u0135\u0136\3\2\2\2\u0136\u0138\3\2\2\2\u0137"+
		"\u0121\3\2\2\2\u0137\u012c\3\2\2\2\u0138\17\3\2\2\2\u0139\u013a\7u\2\2"+
		"\u013a\u013b\5\n\6\2\u013b\u013c\7v\2\2\u013c\u013f\3\2\2\2\u013d\u013f"+
		"\5\n\6\2\u013e\u0139\3\2\2\2\u013e\u013d\3\2\2\2\u013f\21\3\2\2\2\u0140"+
		"\u0142\5\24\13\2\u0141\u0140\3\2\2\2\u0141\u0142\3\2\2\2\u0142\u0146\3"+
		"\2\2\2\u0143\u0145\5\26\f\2\u0144\u0143\3\2\2\2\u0145\u0148\3\2\2\2\u0146"+
		"\u0144\3\2\2\2\u0146\u0147\3\2\2\2\u0147\u014c\3\2\2\2\u0148\u0146\3\2"+
		"\2\2\u0149\u014b\5\30\r\2\u014a\u0149\3\2\2\2\u014b\u014e\3\2\2\2\u014c"+
		"\u014a\3\2\2\2\u014c\u014d\3\2\2\2\u014d\u014f\3\2\2\2\u014e\u014c\3\2"+
		"\2\2\u014f\u0150\7\2\2\3\u0150\23\3\2\2\2\u0151\u0153\5z>\2\u0152\u0151"+
		"\3\2\2\2\u0153\u0156\3\2\2\2\u0154\u0152\3\2\2\2\u0154\u0155\3\2\2\2\u0155"+
		"\u0157\3\2\2\2\u0156\u0154\3\2\2\2\u0157\u0158\7\\\2\2\u0158\u0159\5v"+
		"<\2\u0159\u015a\7{\2\2\u015a\25\3\2\2\2\u015b\u015d\7U\2\2\u015c\u015e"+
		"\7b\2\2\u015d\u015c\3\2\2\2\u015d\u015e\3\2\2\2\u015e\u015f\3\2\2\2\u015f"+
		"\u0162\5v<\2\u0160\u0161\7}\2\2\u0161\u0163\7\u008f\2\2\u0162\u0160\3"+
		"\2\2\2\u0162\u0163\3\2\2\2\u0163\u0164\3\2\2\2\u0164\u0165\7{\2\2\u0165"+
		"\27\3\2\2\2\u0166\u0168\5\34\17\2\u0167\u0166\3\2\2\2\u0168\u016b\3\2"+
		"\2\2\u0169\u0167\3\2\2\2\u0169\u016a\3\2\2\2\u016a\u016c\3\2\2\2\u016b"+
		"\u0169\3\2\2\2\u016c\u0184\5 \21\2\u016d\u016f\5\34\17\2\u016e\u016d\3"+
		"\2\2\2\u016f\u0172\3\2\2\2\u0170\u016e\3\2\2\2\u0170\u0171\3\2\2\2\u0171"+
		"\u0173\3\2\2\2\u0172\u0170\3\2\2\2\u0173\u0184\5(\25\2\u0174\u0176\5\34"+
		"\17\2\u0175\u0174\3\2\2\2\u0176\u0179\3\2\2\2\u0177\u0175\3\2\2\2\u0177"+
		"\u0178\3\2\2\2\u0178\u017a\3\2\2\2\u0179\u0177\3\2\2\2\u017a\u0184\5\60"+
		"\31\2\u017b\u017d\5\34\17\2\u017c\u017b\3\2\2\2\u017d\u0180\3\2\2\2\u017e"+
		"\u017c\3\2\2\2\u017e\u017f\3\2\2\2\u017f\u0181\3\2\2\2\u0180\u017e\3\2"+
		"\2\2\u0181\u0184\5\u0086D\2\u0182\u0184\7{\2\2\u0183\u0169\3\2\2\2\u0183"+
		"\u0170\3\2\2\2\u0183\u0177\3\2\2\2\u0183\u017e\3\2\2\2\u0183\u0182\3\2"+
		"\2\2\u0184\31\3\2\2\2\u0185\u0188\5\34\17\2\u0186\u0188\t\3\2\2\u0187"+
		"\u0185\3\2\2\2\u0187\u0186\3\2\2\2\u0188\33\3\2\2\2\u0189\u018c\5z>\2"+
		"\u018a\u018c\t\4\2\2\u018b\u0189\3\2\2\2\u018b\u018a\3\2\2\2\u018c\35"+
		"\3\2\2\2\u018d\u0190\7N\2\2\u018e\u0190\5z>\2\u018f\u018d\3\2\2\2\u018f"+
		"\u018e\3\2\2\2\u0190\37\3\2\2\2\u0191\u0192\7E\2\2\u0192\u0194\7\u00a0"+
		"\2\2\u0193\u0195\5\"\22\2\u0194\u0193\3\2\2\2\u0194\u0195\3\2\2\2\u0195"+
		"\u0198\3\2\2\2\u0196\u0197\7M\2\2\u0197\u0199\5^\60\2\u0198\u0196\3\2"+
		"\2\2\u0198\u0199\3\2\2\2\u0199\u019c\3\2\2\2\u019a\u019b\7T\2\2\u019b"+
		"\u019d\5\62\32\2\u019c\u019a\3\2\2\2\u019c\u019d\3\2\2\2\u019d\u019e\3"+
		"\2\2\2\u019e\u019f\5\64\33\2\u019f!\3\2\2\2\u01a0\u01a1\7\u0080\2\2\u01a1"+
		"\u01a6\5$\23\2\u01a2\u01a3\7|\2\2\u01a3\u01a5\5$\23\2\u01a4\u01a2\3\2"+
		"\2\2\u01a5\u01a8\3\2\2\2\u01a6\u01a4\3\2\2\2\u01a6\u01a7\3\2\2\2\u01a7"+
		"\u01a9\3\2\2\2\u01a8\u01a6\3\2\2\2\u01a9\u01aa\7\177\2\2\u01aa#\3\2\2"+
		"\2\u01ab\u01ae\7\u00a0\2\2\u01ac\u01ad\7M\2\2\u01ad\u01af\5&\24\2\u01ae"+
		"\u01ac\3\2\2\2\u01ae\u01af\3\2\2\2\u01af%\3\2\2\2\u01b0\u01b5\5^\60\2"+
		"\u01b1\u01b2\7\u0091\2\2\u01b2\u01b4\5^\60\2\u01b3\u01b1\3\2\2\2\u01b4"+
		"\u01b7\3\2\2\2\u01b5\u01b3\3\2\2\2\u01b5\u01b6\3\2\2\2\u01b6\'\3\2\2\2"+
		"\u01b7\u01b5\3\2\2\2\u01b8\u01b9\7L\2\2\u01b9\u01bc\7\u00a0\2\2\u01ba"+
		"\u01bb\7T\2\2\u01bb\u01bd\5\62\32\2\u01bc\u01ba\3\2\2\2\u01bc\u01bd\3"+
		"\2\2\2\u01bd\u01be\3\2\2\2\u01be\u01c0\7w\2\2\u01bf\u01c1\5*\26\2\u01c0"+
		"\u01bf\3\2\2\2\u01c0\u01c1\3\2\2\2\u01c1\u01c3\3\2\2\2\u01c2\u01c4\7|"+
		"\2\2\u01c3\u01c2\3\2\2\2\u01c3\u01c4\3\2\2\2\u01c4\u01c6\3\2\2\2\u01c5"+
		"\u01c7\5.\30\2\u01c6\u01c5\3\2\2\2\u01c6\u01c7\3\2\2\2\u01c7\u01c8\3\2"+
		"\2\2\u01c8\u01c9\7x\2\2\u01c9)\3\2\2\2\u01ca\u01cf\5,\27\2\u01cb\u01cc"+
		"\7|\2\2\u01cc\u01ce\5,\27\2\u01cd\u01cb\3\2\2\2\u01ce\u01d1\3\2\2\2\u01cf"+
		"\u01cd\3\2\2\2\u01cf\u01d0\3\2\2\2\u01d0+\3\2\2\2\u01d1\u01cf\3\2\2\2"+
		"\u01d2\u01d4\5z>\2\u01d3\u01d2\3\2\2\2\u01d4\u01d7\3\2\2\2\u01d5\u01d3"+
		"\3\2\2\2\u01d5\u01d6\3\2\2\2\u01d6\u01d8\3\2\2\2\u01d7\u01d5\3\2\2\2\u01d8"+
		"\u01da\7\u00a0\2\2\u01d9\u01db\5\u00dan\2\u01da\u01d9\3\2\2\2\u01da\u01db"+
		"\3\2\2\2\u01db\u01dd\3\2\2\2\u01dc\u01de\5\64\33\2\u01dd\u01dc\3\2\2\2"+
		"\u01dd\u01de\3\2\2\2\u01de-\3\2\2\2\u01df\u01e3\7{\2\2\u01e0\u01e2\58"+
		"\35\2\u01e1\u01e0\3\2\2\2\u01e2\u01e5\3\2\2\2\u01e3\u01e1\3\2\2\2\u01e3"+
		"\u01e4\3\2\2\2\u01e4/\3\2\2\2\u01e5\u01e3\3\2\2\2\u01e6\u01e7\7X\2\2\u01e7"+
		"\u01e9\7\u00a0\2\2\u01e8\u01ea\5\"\22\2\u01e9\u01e8\3\2\2\2\u01e9\u01ea"+
		"\3\2\2\2\u01ea\u01ed\3\2\2\2\u01eb\u01ec\7M\2\2\u01ec\u01ee\5\62\32\2"+
		"\u01ed\u01eb\3\2\2\2\u01ed\u01ee\3\2\2\2\u01ee\u01ef\3\2\2\2\u01ef\u01f0"+
		"\5\66\34\2\u01f0\61\3\2\2\2\u01f1\u01f6\5^\60\2\u01f2\u01f3\7|\2\2\u01f3"+
		"\u01f5\5^\60\2\u01f4\u01f2\3\2\2\2\u01f5\u01f8\3\2\2\2\u01f6\u01f4\3\2"+
		"\2\2\u01f6\u01f7\3\2\2\2\u01f7\63\3\2\2\2\u01f8\u01f6\3\2\2\2\u01f9\u01fd"+
		"\7w\2\2\u01fa\u01fc\58\35\2\u01fb\u01fa\3\2\2\2\u01fc\u01ff\3\2\2\2\u01fd"+
		"\u01fb\3\2\2\2\u01fd\u01fe\3\2\2\2\u01fe\u0200\3\2\2\2\u01ff\u01fd\3\2"+
		"\2\2\u0200\u0201\7x\2\2\u0201\65\3\2\2\2\u0202\u0206\7w\2\2\u0203\u0205"+
		"\5F$\2\u0204\u0203\3\2\2\2\u0205\u0208\3\2\2\2\u0206\u0204\3\2\2\2\u0206"+
		"\u0207\3\2\2\2\u0207\u0209\3\2\2\2\u0208\u0206\3\2\2\2\u0209\u020a\7x"+
		"\2\2\u020a\67\3\2\2\2\u020b\u0218\7{\2\2\u020c\u020e\7b\2\2\u020d\u020c"+
		"\3\2\2\2\u020d\u020e\3\2\2\2\u020e\u020f\3\2\2\2\u020f\u0218\5\u0096L"+
		"\2\u0210\u0212\5\32\16\2\u0211\u0210\3\2\2\2\u0212\u0215\3\2\2\2\u0213"+
		"\u0211\3\2\2\2\u0213\u0214\3\2\2\2\u0214\u0216\3\2\2\2\u0215\u0213\3\2"+
		"\2\2\u0216\u0218\5:\36\2\u0217\u020b\3\2\2\2\u0217\u020d\3\2\2\2\u0217"+
		"\u0213\3\2\2\2\u02189\3\2\2\2\u0219\u0223\5<\37\2\u021a\u0223\5> \2\u021b"+
		"\u0223\5D#\2\u021c\u0223\5@!\2\u021d\u0223\5B\"\2\u021e\u0223\5\60\31"+
		"\2\u021f\u0223\5\u0086D\2\u0220\u0223\5 \21\2\u0221\u0223\5(\25\2\u0222"+
		"\u0219\3\2\2\2\u0222\u021a\3\2\2\2\u0222\u021b\3\2\2\2\u0222\u021c\3\2"+
		"\2\2\u0222\u021d\3\2\2\2\u0222\u021e\3\2\2\2\u0222\u021f\3\2\2\2\u0222"+
		"\u0220\3\2\2\2\u0222\u0221\3\2\2\2\u0223;\3\2\2\2\u0224\u0227\5^\60\2"+
		"\u0225\u0227\7l\2\2\u0226\u0224\3\2\2\2\u0226\u0225\3\2\2\2\u0227\u0228"+
		"\3\2\2\2\u0228\u0229\7\u00a0\2\2\u0229\u022e\5j\66\2\u022a\u022b\7y\2"+
		"\2\u022b\u022d\7z\2\2\u022c\u022a\3\2\2\2\u022d\u0230\3\2\2\2\u022e\u022c"+
		"\3\2\2\2\u022e\u022f\3\2\2\2\u022f\u0233\3\2\2\2\u0230\u022e\3\2\2\2\u0231"+
		"\u0232\7i\2\2\u0232\u0234\5h\65\2\u0233\u0231\3\2\2\2\u0233\u0234\3\2"+
		"\2\2\u0234\u0237\3\2\2\2\u0235\u0238\5r:\2\u0236\u0238\7{\2\2\u0237\u0235"+
		"\3\2\2\2\u0237\u0236\3\2\2\2\u0238=\3\2\2\2\u0239\u023a\5\"\22\2\u023a"+
		"\u023b\5<\37\2\u023b?\3\2\2\2\u023c\u023d\7\u00a0\2\2\u023d\u0240\5j\66"+
		"\2\u023e\u023f\7i\2\2\u023f\u0241\5h\65\2\u0240\u023e\3\2\2\2\u0240\u0241"+
		"\3\2\2\2\u0241\u0242\3\2\2\2\u0242\u0243\5t;\2\u0243A\3\2\2\2\u0244\u0245"+
		"\5\"\22\2\u0245\u0246\5@!\2\u0246C\3\2\2\2\u0247\u0248\5^\60\2\u0248\u0249"+
		"\5R*\2\u0249\u024a\7{\2\2\u024aE\3\2\2\2\u024b\u024d\5\32\16\2\u024c\u024b"+
		"\3\2\2\2\u024d\u0250\3\2\2\2\u024e\u024c\3\2\2\2\u024e\u024f\3\2\2\2\u024f"+
		"\u0251\3\2\2\2\u0250\u024e\3\2\2\2\u0251\u0254\5H%\2\u0252\u0254\7{\2"+
		"\2\u0253\u024e\3\2\2\2\u0253\u0252\3\2\2\2\u0254G\3\2\2\2\u0255\u025d"+
		"\5J&\2\u0256\u025d\5N(\2\u0257\u025d\5P)\2\u0258\u025d\5\60\31\2\u0259"+
		"\u025d\5\u0086D\2\u025a\u025d\5 \21\2\u025b\u025d\5(\25\2\u025c\u0255"+
		"\3\2\2\2\u025c\u0256\3\2\2\2\u025c\u0257\3\2\2\2\u025c\u0258\3\2\2\2\u025c"+
		"\u0259\3\2\2\2\u025c\u025a\3\2\2\2\u025c\u025b\3\2\2\2\u025dI\3\2\2\2"+
		"\u025e\u025f\5^\60\2\u025f\u0264\5L\'\2\u0260\u0261\7|\2\2\u0261\u0263"+
		"\5L\'\2\u0262\u0260\3\2\2\2\u0263\u0266\3\2\2\2\u0264\u0262\3\2\2\2\u0264"+
		"\u0265\3\2\2\2\u0265\u0267\3\2\2\2\u0266\u0264\3\2\2\2\u0267\u0268\7{"+
		"\2\2\u0268K\3\2\2\2\u0269\u026e\7\u00a0\2\2\u026a\u026b\7y\2\2\u026b\u026d"+
		"\7z\2\2\u026c\u026a\3\2\2\2\u026d\u0270\3\2\2\2\u026e\u026c\3\2\2\2\u026e"+
		"\u026f\3\2\2\2\u026f\u0271\3\2\2\2\u0270\u026e\3\2\2\2\u0271\u0272\7~"+
		"\2\2\u0272\u0273\5X-\2\u0273M\3\2\2\2\u0274\u0277\5^\60\2\u0275\u0277"+
		"\7l\2\2\u0276\u0274\3\2\2\2\u0276\u0275\3\2\2\2\u0277\u0278\3\2\2\2\u0278"+
		"\u0279\7\u00a0\2\2\u0279\u027e\5j\66\2\u027a\u027b\7y\2\2\u027b\u027d"+
		"\7z\2\2\u027c\u027a\3\2\2\2\u027d\u0280\3\2\2\2\u027e\u027c\3\2\2\2\u027e"+
		"\u027f\3\2\2\2\u027f\u0283\3\2\2\2\u0280\u027e\3\2\2\2\u0281\u0282\7i"+
		"\2\2\u0282\u0284\5h\65\2\u0283\u0281\3\2\2\2\u0283\u0284\3\2\2\2\u0284"+
		"\u0285\3\2\2\2\u0285\u0286\7{\2\2\u0286O\3\2\2\2\u0287\u0288\5\"\22\2"+
		"\u0288\u0289\5N(\2\u0289Q\3\2\2\2\u028a\u028f\5T+\2\u028b\u028c\7|\2\2"+
		"\u028c\u028e\5T+\2\u028d\u028b\3\2\2\2\u028e\u0291\3\2\2\2\u028f\u028d"+
		"\3\2\2\2\u028f\u0290\3\2\2\2\u0290S\3\2\2\2\u0291\u028f\3\2\2\2\u0292"+
		"\u0295\5V,\2\u0293\u0294\7~\2\2\u0294\u0296\5X-\2\u0295\u0293\3\2\2\2"+
		"\u0295\u0296\3\2\2\2\u0296U\3\2\2\2\u0297\u029c\7\u00a0\2\2\u0298\u0299"+
		"\7y\2\2\u0299\u029b\7z\2\2\u029a\u0298\3\2\2\2\u029b\u029e\3\2\2\2\u029c"+
		"\u029a\3\2\2\2\u029c\u029d\3\2\2\2\u029dW\3\2\2\2\u029e\u029c\3\2\2\2"+
		"\u029f\u02a2\5Z.\2\u02a0\u02a2\5\u00c0a\2\u02a1\u029f\3\2\2\2\u02a1\u02a0"+
		"\3\2\2\2\u02a2Y\3\2\2\2\u02a3\u02af\7w\2\2\u02a4\u02a9\5X-\2\u02a5\u02a6"+
		"\7|\2\2\u02a6\u02a8\5X-\2\u02a7\u02a5\3\2\2\2\u02a8\u02ab\3\2\2\2\u02a9"+
		"\u02a7\3\2\2\2\u02a9\u02aa\3\2\2\2\u02aa\u02ad\3\2\2\2\u02ab\u02a9\3\2"+
		"\2\2\u02ac\u02ae\7|\2\2\u02ad\u02ac\3\2\2\2\u02ad\u02ae\3\2\2\2\u02ae"+
		"\u02b0\3\2\2\2\u02af\u02a4\3\2\2\2\u02af\u02b0\3\2\2\2\u02b0\u02b1\3\2"+
		"\2\2\u02b1\u02b2\7x\2\2\u02b2[\3\2\2\2\u02b3\u02b4\7\u00a0\2\2\u02b4]"+
		"\3\2\2\2\u02b5\u02ba\5`\61\2\u02b6\u02b7\7y\2\2\u02b7\u02b9\7z\2\2\u02b8"+
		"\u02b6\3\2\2\2\u02b9\u02bc\3\2\2\2\u02ba\u02b8\3\2\2\2\u02ba\u02bb\3\2"+
		"\2\2\u02bb\u02c6\3\2\2\2\u02bc\u02ba\3\2\2\2\u02bd\u02c2\5b\62\2\u02be"+
		"\u02bf\7y\2\2\u02bf\u02c1\7z\2\2\u02c0\u02be\3\2\2\2\u02c1\u02c4\3\2\2"+
		"\2\u02c2\u02c0\3\2\2\2\u02c2\u02c3\3\2\2\2\u02c3\u02c6\3\2\2\2\u02c4\u02c2"+
		"\3\2\2\2\u02c5\u02b5\3\2\2\2\u02c5\u02bd\3\2\2\2\u02c6_\3\2\2\2\u02c7"+
		"\u02c9\7\u00a0\2\2\u02c8\u02ca\5d\63\2\u02c9\u02c8\3\2\2\2\u02c9\u02ca"+
		"\3\2\2\2\u02ca\u02d2\3\2\2\2\u02cb\u02cc\7}\2\2\u02cc\u02ce\7\u00a0\2"+
		"\2\u02cd\u02cf\5d\63\2\u02ce\u02cd\3\2\2\2\u02ce\u02cf\3\2\2\2\u02cf\u02d1"+
		"\3\2\2\2\u02d0\u02cb\3\2\2\2\u02d1\u02d4\3\2\2\2\u02d2\u02d0\3\2\2\2\u02d2"+
		"\u02d3\3\2\2\2\u02d3a\3\2\2\2\u02d4\u02d2\3\2\2\2\u02d5\u02d6\t\5\2\2"+
		"\u02d6c\3\2\2\2\u02d7\u02d8\7\u0080\2\2\u02d8\u02dd\5f\64\2\u02d9\u02da"+
		"\7|\2\2\u02da\u02dc\5f\64\2\u02db\u02d9\3\2\2\2\u02dc\u02df\3\2\2\2\u02dd"+
		"\u02db\3\2\2\2\u02dd\u02de\3\2\2\2\u02de\u02e0\3\2\2\2\u02df\u02dd\3\2"+
		"\2\2\u02e0\u02e1\7\177\2\2\u02e1e\3\2\2\2\u02e2\u02e9\5^\60\2\u02e3\u02e6"+
		"\7\u0083\2\2\u02e4\u02e5\t\6\2\2\u02e5\u02e7\5^\60\2\u02e6\u02e4\3\2\2"+
		"\2\u02e6\u02e7\3\2\2\2\u02e7\u02e9\3\2\2\2\u02e8\u02e2\3\2\2\2\u02e8\u02e3"+
		"\3\2\2\2\u02e9g\3\2\2\2\u02ea\u02ef\5v<\2\u02eb\u02ec\7|\2\2\u02ec\u02ee"+
		"\5v<\2\u02ed\u02eb\3\2\2\2\u02ee\u02f1\3\2\2\2\u02ef\u02ed\3\2\2\2\u02ef"+
		"\u02f0\3\2\2\2\u02f0i\3\2\2\2\u02f1\u02ef\3\2\2\2\u02f2\u02f4\7u\2\2\u02f3"+
		"\u02f5\5l\67\2\u02f4\u02f3\3\2\2\2\u02f4\u02f5\3\2\2\2\u02f5\u02f6\3\2"+
		"\2\2\u02f6\u02f7\7v\2\2\u02f7k\3\2\2\2\u02f8\u02fd\5n8\2\u02f9\u02fa\7"+
		"|\2\2\u02fa\u02fc\5n8\2\u02fb\u02f9\3\2\2\2\u02fc\u02ff\3\2\2\2\u02fd"+
		"\u02fb\3\2\2\2\u02fd\u02fe\3\2\2\2\u02fe\u0302\3\2\2\2\u02ff\u02fd\3\2"+
		"\2\2\u0300\u0301\7|\2\2\u0301\u0303\5p9\2\u0302\u0300\3\2\2\2\u0302\u0303"+
		"\3\2\2\2\u0303\u0306\3\2\2\2\u0304\u0306\5p9\2\u0305\u02f8\3\2\2\2\u0305"+
		"\u0304\3\2\2\2\u0306m\3\2\2\2\u0307\u0309\5\36\20\2\u0308\u0307\3\2\2"+
		"\2\u0309\u030c\3\2\2\2\u030a\u0308\3\2\2\2\u030a\u030b\3\2\2\2\u030b\u030d"+
		"\3\2\2\2\u030c\u030a\3\2\2\2\u030d\u030e\5^\60\2\u030e\u030f\5V,\2\u030f"+
		"o\3\2\2\2\u0310\u0312\5\36\20\2\u0311\u0310\3\2\2\2\u0312\u0315\3\2\2"+
		"\2\u0313\u0311\3\2\2\2\u0313\u0314\3\2\2\2\u0314\u0316\3\2\2\2\u0315\u0313"+
		"\3\2\2\2\u0316\u0317\5^\60\2\u0317\u0318\7\u00a1\2\2\u0318\u0319\5V,\2"+
		"\u0319q\3\2\2\2\u031a\u031b\5\u0096L\2\u031bs\3\2\2\2\u031c\u031d\5\u0096"+
		"L\2\u031du\3\2\2\2\u031e\u0323\7\u00a0\2\2\u031f\u0320\7}\2\2\u0320\u0322"+
		"\7\u00a0\2\2\u0321\u031f\3\2\2\2\u0322\u0325\3\2\2\2\u0323\u0321\3\2\2"+
		"\2\u0323\u0324\3\2\2\2\u0324w\3\2\2\2\u0325\u0323\3\2\2\2\u0326\u0327"+
		"\t\7\2\2\u0327y\3\2\2\2\u0328\u0329\7<\2\2\u0329\u0330\5|?\2\u032a\u032d"+
		"\7u\2\2\u032b\u032e\5~@\2\u032c\u032e\5\u0082B\2\u032d\u032b\3\2\2\2\u032d"+
		"\u032c\3\2\2\2\u032d\u032e\3\2\2\2\u032e\u032f\3\2\2\2\u032f\u0331\7v"+
		"\2\2\u0330\u032a\3\2\2\2\u0330\u0331\3\2\2\2\u0331{\3\2\2\2\u0332\u0333"+
		"\5v<\2\u0333}\3\2\2\2\u0334\u0339\5\u0080A\2\u0335\u0336\7|\2\2\u0336"+
		"\u0338\5\u0080A\2\u0337\u0335\3\2\2\2\u0338\u033b\3\2\2\2\u0339\u0337"+
		"\3\2\2\2\u0339\u033a\3\2\2\2\u033a\177\3\2\2\2\u033b\u0339\3\2\2\2\u033c"+
		"\u033d\7\u00a0\2\2\u033d\u033e\7~\2\2\u033e\u033f\5\u0082B\2\u033f\u0081"+
		"\3\2\2\2\u0340\u0344\5\u00c0a\2\u0341\u0344\5z>\2\u0342\u0344\5\u0084"+
		"C\2\u0343\u0340\3\2\2\2\u0343\u0341\3\2\2\2\u0343\u0342\3\2\2\2\u0344"+
		"\u0083\3\2\2\2\u0345\u034e\7w\2\2\u0346\u034b\5\u0082B\2\u0347\u0348\7"+
		"|\2\2\u0348\u034a\5\u0082B\2\u0349\u0347\3\2\2\2\u034a\u034d\3\2\2\2\u034b"+
		"\u0349\3\2\2\2\u034b\u034c\3\2\2\2\u034c\u034f\3\2\2\2\u034d\u034b\3\2"+
		"\2\2\u034e\u0346\3\2\2\2\u034e\u034f\3\2\2\2\u034f\u0351\3\2\2\2\u0350"+
		"\u0352\7|\2\2\u0351\u0350\3\2\2\2\u0351\u0352\3\2\2\2\u0352\u0353\3\2"+
		"\2\2\u0353\u0354\7x\2\2\u0354\u0085\3\2\2\2\u0355\u0356\7<\2\2\u0356\u0357"+
		"\7X\2\2\u0357\u0358\7\u00a0\2\2\u0358\u0359\5\u0088E\2\u0359\u0087\3\2"+
		"\2\2\u035a\u035e\7w\2\2\u035b\u035d\5\u008aF\2\u035c\u035b\3\2\2\2\u035d"+
		"\u0360\3\2\2\2\u035e\u035c\3\2\2\2\u035e\u035f\3\2\2\2\u035f\u0361\3\2"+
		"\2\2\u0360\u035e\3\2\2\2\u0361\u0362\7x\2\2\u0362\u0089\3\2\2\2\u0363"+
		"\u0365\5\32\16\2\u0364\u0363\3\2\2\2\u0365\u0368\3\2\2\2\u0366\u0364\3"+
		"\2\2\2\u0366\u0367\3\2\2\2\u0367\u0369\3\2\2\2\u0368\u0366\3\2\2\2\u0369"+
		"\u036c\5\u008cG\2\u036a\u036c\7{\2\2\u036b\u0366\3\2\2\2\u036b\u036a\3"+
		"\2\2\2\u036c\u008b\3\2\2\2\u036d\u036e\5^\60\2\u036e\u036f\5\u008eH\2"+
		"\u036f\u0370\7{\2\2\u0370\u0382\3\2\2\2\u0371\u0373\5 \21\2\u0372\u0374"+
		"\7{\2\2\u0373\u0372\3\2\2\2\u0373\u0374\3\2\2\2\u0374\u0382\3\2\2\2\u0375"+
		"\u0377\5\60\31\2\u0376\u0378\7{\2\2\u0377\u0376\3\2\2\2\u0377\u0378\3"+
		"\2\2\2\u0378\u0382\3\2\2\2\u0379\u037b\5(\25\2\u037a\u037c\7{\2\2\u037b"+
		"\u037a\3\2\2\2\u037b\u037c\3\2\2\2\u037c\u0382\3\2\2\2\u037d\u037f\5\u0086"+
		"D\2\u037e\u0380\7{\2\2\u037f\u037e\3\2\2\2\u037f\u0380\3\2\2\2\u0380\u0382"+
		"\3\2\2\2\u0381\u036d\3\2\2\2\u0381\u0371\3\2\2\2\u0381\u0375\3\2\2\2\u0381"+
		"\u0379\3\2\2\2\u0381\u037d\3\2\2\2\u0382\u008d\3\2\2\2\u0383\u0386\5\u0090"+
		"I\2\u0384\u0386\5\u0092J\2\u0385\u0383\3\2\2\2\u0385\u0384\3\2\2\2\u0386"+
		"\u008f\3\2\2\2\u0387\u0388\7\u00a0\2\2\u0388\u0389\7u\2\2\u0389\u038b"+
		"\7v\2\2\u038a\u038c\5\u0094K\2\u038b\u038a\3\2\2\2\u038b\u038c\3\2\2\2"+
		"\u038c\u0091\3\2\2\2\u038d\u038e\5R*\2\u038e\u0093\3\2\2\2\u038f\u0390"+
		"\7H\2\2\u0390\u0391\5\u0082B\2\u0391\u0095\3\2\2\2\u0392\u0396\7w\2\2"+
		"\u0393\u0395\5\u0098M\2\u0394\u0393\3\2\2\2\u0395\u0398\3\2\2\2\u0396"+
		"\u0394\3\2\2\2\u0396\u0397\3\2\2\2\u0397\u0399\3\2\2\2\u0398\u0396\3\2"+
		"\2\2\u0399\u039a\7x\2\2\u039a\u0097\3\2\2\2\u039b\u039f\5\u009aN\2\u039c"+
		"\u039f\5\u009eP\2\u039d\u039f\5\30\r\2\u039e\u039b\3\2\2\2\u039e\u039c"+
		"\3\2\2\2\u039e\u039d\3\2\2\2\u039f\u0099\3\2\2\2\u03a0\u03a1\5\u009cO"+
		"\2\u03a1\u03a2\7{\2\2\u03a2\u009b\3\2\2\2\u03a3\u03a5\5\36\20\2\u03a4"+
		"\u03a3\3\2\2\2\u03a5\u03a8\3\2\2\2\u03a6\u03a4\3\2\2\2\u03a6\u03a7\3\2"+
		"\2\2\u03a7\u03a9\3\2\2\2\u03a8\u03a6\3\2\2\2\u03a9\u03aa\5^\60\2\u03aa"+
		"\u03ab\5R*\2\u03ab\u009d\3\2\2\2\u03ac\u0415\5\u0096L\2\u03ad\u03ae\7"+
		">\2\2\u03ae\u03b1\5\u00c0a\2\u03af\u03b0\7\u0084\2\2\u03b0\u03b2\5\u00c0"+
		"a\2\u03b1\u03af\3\2\2\2\u03b1\u03b2\3\2\2\2\u03b2\u03b3\3\2\2\2\u03b3"+
		"\u03b4\7{\2\2\u03b4\u0415\3\2\2\2\u03b5\u03b6\7R\2\2\u03b6\u03b7\5\u00b8"+
		"]\2\u03b7\u03ba\5\u009eP\2\u03b8\u03b9\7K\2\2\u03b9\u03bb\5\u009eP\2\u03ba"+
		"\u03b8\3\2\2\2\u03ba\u03bb\3\2\2\2\u03bb\u0415\3\2\2\2\u03bc\u03bd\7Q"+
		"\2\2\u03bd\u03be\7u\2\2\u03be\u03bf\5\u00b0Y\2\u03bf\u03c0\7v\2\2\u03c0"+
		"\u03c1\5\u009eP\2\u03c1\u0415\3\2\2\2\u03c2\u03c3\7n\2\2\u03c3\u03c4\5"+
		"\u00b8]\2\u03c4\u03c5\5\u009eP\2\u03c5\u0415\3\2\2\2\u03c6\u03c7\7I\2"+
		"\2\u03c7\u03c8\5\u009eP\2\u03c8\u03c9\7n\2\2\u03c9\u03ca\5\u00b8]\2\u03ca"+
		"\u03cb\7{\2\2\u03cb\u0415\3\2\2\2\u03cc\u03cd\7k\2\2\u03cd\u03d7\5\u0096"+
		"L\2\u03ce\u03d0\5\u00a0Q\2\u03cf\u03ce\3\2\2\2\u03d0\u03d1\3\2\2\2\u03d1"+
		"\u03cf\3\2\2\2\u03d1\u03d2\3\2\2\2\u03d2\u03d4\3\2\2\2\u03d3\u03d5\5\u00a4"+
		"S\2\u03d4\u03d3\3\2\2\2\u03d4\u03d5\3\2\2\2\u03d5\u03d8\3\2\2\2\u03d6"+
		"\u03d8\5\u00a4S\2\u03d7\u03cf\3\2\2\2\u03d7\u03d6\3\2\2\2\u03d8\u0415"+
		"\3\2\2\2\u03d9\u03da\7k\2\2\u03da\u03db\5\u00a6T\2\u03db\u03df\5\u0096"+
		"L\2\u03dc\u03de\5\u00a0Q\2\u03dd\u03dc\3\2\2\2\u03de\u03e1\3\2\2\2\u03df"+
		"\u03dd\3\2\2\2\u03df\u03e0\3\2\2\2\u03e0\u03e3\3\2\2\2\u03e1\u03df\3\2"+
		"\2\2\u03e2\u03e4\5\u00a4S\2\u03e3\u03e2\3\2\2\2\u03e3\u03e4\3\2\2\2\u03e4"+
		"\u0415\3\2\2\2\u03e5\u03e6\7e\2\2\u03e6\u03e7\5\u00b8]\2\u03e7\u03eb\7"+
		"w\2\2\u03e8\u03ea\5\u00acW\2\u03e9\u03e8\3\2\2\2\u03ea\u03ed\3\2\2\2\u03eb"+
		"\u03e9\3\2\2\2\u03eb\u03ec\3\2\2\2\u03ec\u03f1\3\2\2\2\u03ed\u03eb\3\2"+
		"\2\2\u03ee\u03f0\5\u00aeX\2\u03ef\u03ee\3\2\2\2\u03f0\u03f3\3\2\2\2\u03f1"+
		"\u03ef\3\2\2\2\u03f1\u03f2\3\2\2\2\u03f2\u03f4\3\2\2\2\u03f3\u03f1\3\2"+
		"\2\2\u03f4\u03f5\7x\2\2\u03f5\u0415\3\2\2\2\u03f6\u03f7\7f\2\2\u03f7\u03f8"+
		"\5\u00b8]\2\u03f8\u03f9\5\u0096L\2\u03f9\u0415\3\2\2\2\u03fa\u03fc\7`"+
		"\2\2\u03fb\u03fd\5\u00c0a\2\u03fc\u03fb\3\2\2\2\u03fc\u03fd\3\2\2\2\u03fd"+
		"\u03fe\3\2\2\2\u03fe\u0415\7{\2\2\u03ff\u0400\7h\2\2\u0400\u0401\5\u00c0"+
		"a\2\u0401\u0402\7{\2\2\u0402\u0415\3\2\2\2\u0403\u0405\7@\2\2\u0404\u0406"+
		"\7\u00a0\2\2\u0405\u0404\3\2\2\2\u0405\u0406\3\2\2\2\u0406\u0407\3\2\2"+
		"\2\u0407\u0415\7{\2\2\u0408\u040a\7G\2\2\u0409\u040b\7\u00a0\2\2\u040a"+
		"\u0409\3\2\2\2\u040a\u040b\3\2\2\2\u040b\u040c\3\2\2\2\u040c\u0415\7{"+
		"\2\2\u040d\u0415\7{\2\2\u040e\u040f\5\u00bc_\2\u040f\u0410\7{\2\2\u0410"+
		"\u0415\3\2\2\2\u0411\u0412\7\u00a0\2\2\u0412\u0413\7\u0084\2\2\u0413\u0415"+
		"\5\u009eP\2\u0414\u03ac\3\2\2\2\u0414\u03ad\3\2\2\2\u0414\u03b5\3\2\2"+
		"\2\u0414\u03bc\3\2\2\2\u0414\u03c2\3\2\2\2\u0414\u03c6\3\2\2\2\u0414\u03cc"+
		"\3\2\2\2\u0414\u03d9\3\2\2\2\u0414\u03e5\3\2\2\2\u0414\u03f6\3\2\2\2\u0414"+
		"\u03fa\3\2\2\2\u0414\u03ff\3\2\2\2\u0414\u0403\3\2\2\2\u0414\u0408\3\2"+
		"\2\2\u0414\u040d\3\2\2\2\u0414\u040e\3\2\2\2\u0414\u0411\3\2\2\2\u0415"+
		"\u009f\3\2\2\2\u0416\u0417\7C\2\2\u0417\u041b\7u\2\2\u0418\u041a\5\36"+
		"\20\2\u0419\u0418\3\2\2\2\u041a\u041d\3\2\2\2\u041b\u0419\3\2\2\2\u041b"+
		"\u041c\3\2\2\2\u041c\u041e\3\2\2\2\u041d\u041b\3\2\2\2\u041e\u041f\5\u00a2"+
		"R\2\u041f\u0420\7\u00a0\2\2\u0420\u0421\7v\2\2\u0421\u0422\5\u0096L\2"+
		"\u0422\u00a1\3\2\2\2\u0423\u0428\5v<\2\u0424\u0425\7\u0092\2\2\u0425\u0427"+
		"\5v<\2\u0426\u0424\3\2\2\2\u0427\u042a\3\2\2\2\u0428\u0426\3\2\2\2\u0428"+
		"\u0429\3\2\2\2\u0429\u00a3\3\2\2\2\u042a\u0428\3\2\2\2\u042b\u042c\7O"+
		"\2\2\u042c\u042d\5\u0096L\2\u042d\u00a5\3\2\2\2\u042e\u042f\7u\2\2\u042f"+
		"\u0431\5\u00a8U\2\u0430\u0432\7{\2\2\u0431\u0430\3\2\2\2\u0431\u0432\3"+
		"\2\2\2\u0432\u0433\3\2\2\2\u0433\u0434\7v\2\2\u0434\u00a7\3\2\2\2\u0435"+
		"\u043a\5\u00aaV\2\u0436\u0437\7{\2\2\u0437\u0439\5\u00aaV\2\u0438\u0436"+
		"\3\2\2\2\u0439\u043c\3\2\2\2\u043a\u0438\3\2\2\2\u043a\u043b\3\2\2\2\u043b"+
		"\u00a9\3\2\2\2\u043c\u043a\3\2\2\2\u043d\u043f\5\36\20\2\u043e\u043d\3"+
		"\2\2\2\u043f\u0442\3\2\2\2\u0440\u043e\3\2\2\2\u0440\u0441\3\2\2\2\u0441"+
		"\u0443\3\2\2\2\u0442\u0440\3\2\2\2\u0443\u0444\5`\61\2\u0444\u0445\5V"+
		",\2\u0445\u0446\7~\2\2\u0446\u0447\5\u00c0a\2\u0447\u00ab\3\2\2\2\u0448"+
		"\u044a\5\u00aeX\2\u0449\u0448\3\2\2\2\u044a\u044b\3\2\2\2\u044b\u0449"+
		"\3\2\2\2\u044b\u044c\3\2\2\2\u044c\u044e\3\2\2\2\u044d\u044f\5\u0098M"+
		"\2\u044e\u044d\3\2\2\2\u044f\u0450\3\2\2\2\u0450\u044e\3\2\2\2\u0450\u0451"+
		"\3\2\2\2\u0451\u00ad\3\2\2\2\u0452\u0453\7B\2\2\u0453\u0454\5\u00be`\2"+
		"\u0454\u0455\7\u0084\2\2\u0455\u045d\3\2\2\2\u0456\u0457\7B\2\2\u0457"+
		"\u0458\5\\/\2\u0458\u0459\7\u0084\2\2\u0459\u045d\3\2\2\2\u045a\u045b"+
		"\7H\2\2\u045b\u045d\7\u0084\2\2\u045c\u0452\3\2\2\2\u045c\u0456\3\2\2"+
		"\2\u045c\u045a\3\2\2\2\u045d\u00af\3\2\2\2\u045e\u046b\5\u00b4[\2\u045f"+
		"\u0461\5\u00b2Z\2\u0460\u045f\3\2\2\2\u0460\u0461\3\2\2\2\u0461\u0462"+
		"\3\2\2\2\u0462\u0464\7{\2\2\u0463\u0465\5\u00c0a\2\u0464\u0463\3\2\2\2"+
		"\u0464\u0465\3\2\2\2\u0465\u0466\3\2\2\2\u0466\u0468\7{\2\2\u0467\u0469"+
		"\5\u00b6\\\2\u0468\u0467\3\2\2\2\u0468\u0469\3\2\2\2\u0469\u046b\3\2\2"+
		"\2\u046a\u045e\3\2\2\2\u046a\u0460\3\2\2\2\u046b\u00b1\3\2\2\2\u046c\u046f"+
		"\5\u009cO\2\u046d\u046f\5\u00ba^\2\u046e\u046c\3\2\2\2\u046e\u046d\3\2"+
		"\2\2\u046f\u00b3\3\2\2\2\u0470\u0472\5\36\20\2\u0471\u0470\3\2\2\2\u0472"+
		"\u0475\3\2\2\2\u0473\u0471\3\2\2\2\u0473\u0474\3\2\2\2\u0474\u0476\3\2"+
		"\2\2\u0475\u0473\3\2\2\2\u0476\u0477\5^\60\2\u0477\u0478\5V,\2\u0478\u0479"+
		"\7\u0084\2\2\u0479\u047a\5\u00c0a\2\u047a\u00b5\3\2\2\2\u047b\u047c\5"+
		"\u00ba^\2\u047c\u00b7\3\2\2\2\u047d\u047e\7u\2\2\u047e\u047f\5\u00c0a"+
		"\2\u047f\u0480\7v\2\2\u0480\u00b9\3\2\2\2\u0481\u0486\5\u00c0a\2\u0482"+
		"\u0483\7|\2\2\u0483\u0485\5\u00c0a\2\u0484\u0482\3\2\2\2\u0485\u0488\3"+
		"\2\2\2\u0486\u0484\3\2\2\2\u0486\u0487\3\2\2\2\u0487\u00bb\3\2\2\2\u0488"+
		"\u0486\3\2\2\2\u0489\u048a\5\u00c0a\2\u048a\u00bd\3\2\2\2\u048b\u048c"+
		"\5\u00c0a\2\u048c\u00bf\3\2\2\2\u048d\u048e\ba\1\2\u048e\u048f\7u\2\2"+
		"\u048f\u0490\5^\60\2\u0490\u0491\7v\2\2\u0491\u0492\5\u00c0a\23\u0492"+
		"\u049b\3\2\2\2\u0493\u0494\t\b\2\2\u0494\u049b\5\u00c0a\21\u0495\u0496"+
		"\t\t\2\2\u0496\u049b\5\u00c0a\20\u0497\u049b\5\u00c2b\2\u0498\u0499\7"+
		"[\2\2\u0499\u049b\5\u00c4c\2\u049a\u048d\3\2\2\2\u049a\u0493\3\2\2\2\u049a"+
		"\u0495\3\2\2\2\u049a\u0497\3\2\2\2\u049a\u0498\3\2\2\2\u049b\u04f1\3\2"+
		"\2\2\u049c\u049d\f\17\2\2\u049d\u049e\t\n\2\2\u049e\u04f0\5\u00c0a\20"+
		"\u049f\u04a0\f\16\2\2\u04a0\u04a1\t\13\2\2\u04a1\u04f0\5\u00c0a\17\u04a2"+
		"\u04aa\f\r\2\2\u04a3\u04a4\7\u0080\2\2\u04a4\u04ab\7\u0080\2\2\u04a5\u04a6"+
		"\7\177\2\2\u04a6\u04a7\7\177\2\2\u04a7\u04ab\7\177\2\2\u04a8\u04a9\7\177"+
		"\2\2\u04a9\u04ab\7\177\2\2\u04aa\u04a3\3\2\2\2\u04aa\u04a5\3\2\2\2\u04aa"+
		"\u04a8\3\2\2\2\u04ab\u04ac\3\2\2\2\u04ac\u04f0\5\u00c0a\16\u04ad\u04ae"+
		"\f\f\2\2\u04ae\u04af\t\f\2\2\u04af\u04f0\5\u00c0a\r\u04b0\u04b1\f\n\2"+
		"\2\u04b1\u04b2\t\r\2\2\u04b2\u04f0\5\u00c0a\13\u04b3\u04b4\f\t\2\2\u04b4"+
		"\u04b5\7\u0091\2\2\u04b5\u04f0\5\u00c0a\n\u04b6\u04b7\f\b\2\2\u04b7\u04b8"+
		"\7\u0093\2\2\u04b8\u04f0\5\u00c0a\t\u04b9\u04ba\f\7\2\2\u04ba\u04bb\7"+
		"\u0092\2\2\u04bb\u04f0\5\u00c0a\b\u04bc\u04bd\f\6\2\2\u04bd\u04be\7\u0089"+
		"\2\2\u04be\u04f0\5\u00c0a\7\u04bf\u04c0\f\5\2\2\u04c0\u04c1\7\u008a\2"+
		"\2\u04c1\u04f0\5\u00c0a\6\u04c2\u04c3\f\4\2\2\u04c3\u04c4\7\u0083\2\2"+
		"\u04c4\u04c5\5\u00c0a\2\u04c5\u04c6\7\u0084\2\2\u04c6\u04c7\5\u00c0a\5"+
		"\u04c7\u04f0\3\2\2\2\u04c8\u04c9\f\3\2\2\u04c9\u04ca\t\16\2\2\u04ca\u04f0"+
		"\5\u00c0a\3\u04cb\u04cc\f\33\2\2\u04cc\u04cd\7}\2\2\u04cd\u04f0\7\u00a0"+
		"\2\2\u04ce\u04cf\f\32\2\2\u04cf\u04d0\7}\2\2\u04d0\u04f0\7g\2\2\u04d1"+
		"\u04d2\f\31\2\2\u04d2\u04d3\7}\2\2\u04d3\u04d5\7[\2\2\u04d4\u04d6\5\u00d0"+
		"i\2\u04d5\u04d4\3\2\2\2\u04d5\u04d6\3\2\2\2\u04d6\u04d7\3\2\2\2\u04d7"+
		"\u04f0\5\u00c8e\2\u04d8\u04d9\f\30\2\2\u04d9\u04da\7}\2\2\u04da\u04db"+
		"\7d\2\2\u04db\u04f0\5\u00d6l\2\u04dc\u04dd\f\27\2\2\u04dd\u04de\7}\2\2"+
		"\u04de\u04f0\5\u00ceh\2\u04df\u04e0\f\26\2\2\u04e0\u04e1\7y\2\2\u04e1"+
		"\u04e2\5\u00c0a\2\u04e2\u04e3\7z\2\2\u04e3\u04f0\3\2\2\2\u04e4\u04e5\f"+
		"\25\2\2\u04e5\u04e7\7u\2\2\u04e6\u04e8\5\u00ba^\2\u04e7\u04e6\3\2\2\2"+
		"\u04e7\u04e8\3\2\2\2\u04e8\u04e9\3\2\2\2\u04e9\u04f0\7v\2\2\u04ea\u04eb"+
		"\f\22\2\2\u04eb\u04f0\t\17\2\2\u04ec\u04ed\f\13\2\2\u04ed\u04ee\7V\2\2"+
		"\u04ee\u04f0\5^\60\2\u04ef\u049c\3\2\2\2\u04ef\u049f\3\2\2\2\u04ef\u04a2"+
		"\3\2\2\2\u04ef\u04ad\3\2\2\2\u04ef\u04b0\3\2\2\2\u04ef\u04b3\3\2\2\2\u04ef"+
		"\u04b6\3\2\2\2\u04ef\u04b9\3\2\2\2\u04ef\u04bc\3\2\2\2\u04ef\u04bf\3\2"+
		"\2\2\u04ef\u04c2\3\2\2\2\u04ef\u04c8\3\2\2\2\u04ef\u04cb\3\2\2\2\u04ef"+
		"\u04ce\3\2\2\2\u04ef\u04d1\3\2\2\2\u04ef\u04d8\3\2\2\2\u04ef\u04dc\3\2"+
		"\2\2\u04ef\u04df\3\2\2\2\u04ef\u04e4\3\2\2\2\u04ef\u04ea\3\2\2\2\u04ef"+
		"\u04ec\3\2\2\2\u04f0\u04f3\3\2\2\2\u04f1\u04ef\3\2\2\2\u04f1\u04f2\3\2"+
		"\2\2\u04f2\u00c1\3\2\2\2\u04f3\u04f1\3\2\2\2\u04f4\u04f5\7u\2\2\u04f5"+
		"\u04f6\5\u00c0a\2\u04f6\u04f7\7v\2\2\u04f7\u050a\3\2\2\2\u04f8\u050a\7"+
		"g\2\2\u04f9\u050a\7d\2\2\u04fa\u050a\5x=\2\u04fb\u050a\7\u00a0\2\2\u04fc"+
		"\u04fd\5^\60\2\u04fd\u04fe\7}\2\2\u04fe\u04ff\7E\2\2\u04ff\u050a\3\2\2"+
		"\2\u0500\u0501\7l\2\2\u0501\u0502\7}\2\2\u0502\u050a\7E\2\2\u0503\u0507"+
		"\5\u00d0i\2\u0504\u0508\5\u00d8m\2\u0505\u0506\7g\2\2\u0506\u0508\5\u00da"+
		"n\2\u0507\u0504\3\2\2\2\u0507\u0505\3\2\2\2\u0508\u050a\3\2\2\2\u0509"+
		"\u04f4\3\2\2\2\u0509\u04f8\3\2\2\2\u0509\u04f9\3\2\2\2\u0509\u04fa\3\2"+
		"\2\2\u0509\u04fb\3\2\2\2\u0509\u04fc\3\2\2\2\u0509\u0500\3\2\2\2\u0509"+
		"\u0503\3\2\2\2\u050a\u00c3\3\2\2\2\u050b\u050c\5\u00d0i\2\u050c\u050d"+
		"\5\u00c6d\2\u050d\u050e\5\u00ccg\2\u050e\u0515\3\2\2\2\u050f\u0512\5\u00c6"+
		"d\2\u0510\u0513\5\u00caf\2\u0511\u0513\5\u00ccg\2\u0512\u0510\3\2\2\2"+
		"\u0512\u0511\3\2\2\2\u0513\u0515\3\2\2\2\u0514\u050b\3\2\2\2\u0514\u050f"+
		"\3\2\2\2\u0515\u00c5\3\2\2\2\u0516\u0518\7\u00a0\2\2\u0517\u0519\5\u00d2"+
		"j\2\u0518\u0517\3\2\2\2\u0518\u0519\3\2\2\2\u0519\u0521\3\2\2\2\u051a"+
		"\u051b\7}\2\2\u051b\u051d\7\u00a0\2\2\u051c\u051e\5\u00d2j\2\u051d\u051c"+
		"\3\2\2\2\u051d\u051e\3\2\2\2\u051e\u0520\3\2\2\2\u051f\u051a\3\2\2\2\u0520"+
		"\u0523\3\2\2\2\u0521\u051f\3\2\2\2\u0521\u0522\3\2\2\2\u0522\u0526\3\2"+
		"\2\2\u0523\u0521\3\2\2\2\u0524\u0526\5b\62\2\u0525\u0516\3\2\2\2\u0525"+
		"\u0524\3\2\2\2\u0526\u00c7\3\2\2\2\u0527\u0529\7\u00a0\2\2\u0528\u052a"+
		"\5\u00d4k\2\u0529\u0528\3\2\2\2\u0529\u052a\3\2\2\2\u052a\u052b\3\2\2"+
		"\2\u052b\u052c\5\u00ccg\2\u052c\u00c9\3\2\2\2\u052d\u0549\7y\2\2\u052e"+
		"\u0533\7z\2\2\u052f\u0530\7y\2\2\u0530\u0532\7z\2\2\u0531\u052f\3\2\2"+
		"\2\u0532\u0535\3\2\2\2\u0533\u0531\3\2\2\2\u0533\u0534\3\2\2\2\u0534\u0536"+
		"\3\2\2\2\u0535\u0533\3\2\2\2\u0536\u054a\5Z.\2\u0537\u0538\5\u00c0a\2"+
		"\u0538\u053f\7z\2\2\u0539\u053a\7y\2\2\u053a\u053b\5\u00c0a\2\u053b\u053c"+
		"\7z\2\2\u053c\u053e\3\2\2\2\u053d\u0539\3\2\2\2\u053e\u0541\3\2\2\2\u053f"+
		"\u053d\3\2\2\2\u053f\u0540\3\2\2\2\u0540\u0546\3\2\2\2\u0541\u053f\3\2"+
		"\2\2\u0542\u0543\7y\2\2\u0543\u0545\7z\2\2\u0544\u0542\3\2\2\2\u0545\u0548"+
		"\3\2\2\2\u0546\u0544\3\2\2\2\u0546\u0547\3\2\2\2\u0547\u054a\3\2\2\2\u0548"+
		"\u0546\3\2\2\2\u0549\u052e\3\2\2\2\u0549\u0537\3\2\2\2\u054a\u00cb\3\2"+
		"\2\2\u054b\u054d\5\u00dan\2\u054c\u054e\5\64\33\2\u054d\u054c\3\2\2\2"+
		"\u054d\u054e\3\2\2\2\u054e\u00cd\3\2\2\2\u054f\u0550\5\u00d0i\2\u0550"+
		"\u0551\5\u00d8m\2\u0551\u00cf\3\2\2\2\u0552\u0553\7\u0080\2\2\u0553\u0554"+
		"\5\62\32\2\u0554\u0555\7\177\2\2\u0555\u00d1\3\2\2\2\u0556\u0557\7\u0080"+
		"\2\2\u0557\u055a\7\177\2\2\u0558\u055a\5d\63\2\u0559\u0556\3\2\2\2\u0559"+
		"\u0558\3\2\2\2\u055a\u00d3\3\2\2\2\u055b\u055c\7\u0080\2\2\u055c\u055f"+
		"\7\177\2\2\u055d\u055f\5\u00d0i\2\u055e\u055b\3\2\2\2\u055e\u055d\3\2"+
		"\2\2\u055f\u00d5\3\2\2\2\u0560\u0567\5\u00dan\2\u0561\u0562\7}\2\2\u0562"+
		"\u0564\7\u00a0\2\2\u0563\u0565\5\u00dan\2\u0564\u0563\3\2\2\2\u0564\u0565"+
		"\3\2\2\2\u0565\u0567\3\2\2\2\u0566\u0560\3\2\2\2\u0566\u0561\3\2\2\2\u0567"+
		"\u00d7\3\2\2\2\u0568\u0569\7d\2\2\u0569\u056d\5\u00d6l\2\u056a\u056b\7"+
		"\u00a0\2\2\u056b\u056d\5\u00dan\2\u056c\u0568\3\2\2\2\u056c\u056a\3\2"+
		"\2\2\u056d\u00d9\3\2\2\2\u056e\u0570\7u\2\2\u056f\u0571\5\u00ba^\2\u0570"+
		"\u056f\3\2\2\2\u0570\u0571\3\2\2\2\u0571\u0572\3\2\2\2\u0572\u0573\7v"+
		"\2\2\u0573\u00db\3\2\2\2\u00a8\u00e2\u00eb\u00f3\u00f9\u0100\u0106\u010c"+
		"\u0114\u0116\u011c\u011f\u0126\u012a\u0131\u0135\u0137\u013e\u0141\u0146"+
		"\u014c\u0154\u015d\u0162\u0169\u0170\u0177\u017e\u0183\u0187\u018b\u018f"+
		"\u0194\u0198\u019c\u01a6\u01ae\u01b5\u01bc\u01c0\u01c3\u01c6\u01cf\u01d5"+
		"\u01da\u01dd\u01e3\u01e9\u01ed\u01f6\u01fd\u0206\u020d\u0213\u0217\u0222"+
		"\u0226\u022e\u0233\u0237\u0240\u024e\u0253\u025c\u0264\u026e\u0276\u027e"+
		"\u0283\u028f\u0295\u029c\u02a1\u02a9\u02ad\u02af\u02ba\u02c2\u02c5\u02c9"+
		"\u02ce\u02d2\u02dd\u02e6\u02e8\u02ef\u02f4\u02fd\u0302\u0305\u030a\u0313"+
		"\u0323\u032d\u0330\u0339\u0343\u034b\u034e\u0351\u035e\u0366\u036b\u0373"+
		"\u0377\u037b\u037f\u0381\u0385\u038b\u0396\u039e\u03a6\u03b1\u03ba\u03d1"+
		"\u03d4\u03d7\u03df\u03e3\u03eb\u03f1\u03fc\u0405\u040a\u0414\u041b\u0428"+
		"\u0431\u043a\u0440\u044b\u0450\u045c\u0460\u0464\u0468\u046a\u046e\u0473"+
		"\u0486\u049a\u04aa\u04d5\u04e7\u04ef\u04f1\u0507\u0509\u0512\u0514\u0518"+
		"\u051d\u0521\u0525\u0529\u0533\u053f\u0546\u0549\u054d\u0559\u055e\u0564"+
		"\u0566\u056c\u0570";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}