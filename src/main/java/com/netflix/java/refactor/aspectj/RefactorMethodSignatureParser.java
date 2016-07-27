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
		THROW=45, STATIC=39, INTERFACE=29, AND_ASSIGN=94, BREAK=5, BYTE=6, ELSE=16, 
		IF=23, ENUM=17, SUB=83, BANG=70, LPAREN=58, DOT=66, CASE=7, AT=102, LINE_COMMENT=106, 
		StringLiteral=56, ELLIPSIS=103, LBRACK=62, PUBLIC=36, THROWS=46, NullLiteral=57, 
		RSHIFT_ASSIGN=99, LBRACE=60, GOTO=24, SUB_ASSIGN=91, SEMI=64, CHAR=9, 
		ASSIGN=67, COMMENT=105, IMPORT=26, BITOR=87, CATCH=8, MUL_ASSIGN=92, DOUBLE=15, 
		PROTECTED=35, LONG=30, COMMA=65, BITAND=86, PRIVATE=34, CONTINUE=12, DIV=85, 
		FloatingPointLiteral=53, LE=75, CharacterLiteral=55, VOLATILE=50, EXTENDS=18, 
		INSTANCEOF=27, NEW=32, ADD=82, LT=69, CLASS=10, DO=14, FINALLY=20, Identifier=101, 
		CONST=11, PACKAGE=33, OR_ASSIGN=95, TRY=48, IntegerLiteral=52, SYNCHRONIZED=43, 
		MUL=84, FOR=22, FINAL=19, RPAREN=59, CARET=88, URSHIFT_ASSIGN=100, BOOLEAN=4, 
		NOTEQUAL=77, RBRACK=63, RBRACE=61, AND=78, THIS=44, SWITCH=42, VOID=49, 
		TRANSIENT=47, INC=80, FLOAT=21, NATIVE=31, DIV_ASSIGN=93, BooleanLiteral=54, 
		ABSTRACT=2, STRICTFP=40, INT=28, QUESTION=72, RETURN=37, LSHIFT_ASSIGN=98, 
		ADD_ASSIGN=90, WS=104, GE=76, SUPER=41, OR=79, DEC=81, MOD=89, XOR_ASSIGN=96, 
		ASSERT=3, EQUAL=74, DOTDOT=1, IMPLEMENTS=25, COLON=73, GT=68, SHORT=38, 
		MOD_ASSIGN=97, WHILE=51, TILDE=71, DEFAULT=13;
	public static final String[] tokenNames = {
		"<INVALID>", "'..'", "'abstract'", "'assert'", "'boolean'", "'break'", 
		"'byte'", "'case'", "'catch'", "'char'", "'class'", "'const'", "'continue'", 
		"'default'", "'do'", "'double'", "'else'", "'enum'", "'extends'", "'final'", 
		"'finally'", "'float'", "'for'", "'if'", "'goto'", "'implements'", "'import'", 
		"'instanceof'", "'int'", "'interface'", "'long'", "'native'", "'new'", 
		"'package'", "'private'", "'protected'", "'public'", "'return'", "'short'", 
		"'static'", "'strictfp'", "'super'", "'switch'", "'synchronized'", "'this'", 
		"'throw'", "'throws'", "'transient'", "'try'", "'void'", "'volatile'", 
		"'while'", "IntegerLiteral", "FloatingPointLiteral", "BooleanLiteral", 
		"CharacterLiteral", "StringLiteral", "'null'", "'('", "')'", "'{'", "'}'", 
		"'['", "']'", "';'", "','", "'.'", "'='", "'>'", "'<'", "'!'", "'~'", 
		"'?'", "':'", "'=='", "'<='", "'>='", "'!='", "'&&'", "'||'", "'++'", 
		"'--'", "'+'", "'-'", "'*'", "'/'", "'&'", "'|'", "'^'", "'%'", "'+='", 
		"'-='", "'*='", "'/='", "'&='", "'|='", "'^='", "'%='", "'<<='", "'>>='", 
		"'>>>='", "Identifier", "'@'", "'...'", "WS", "COMMENT", "LINE_COMMENT"
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
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << DOTDOT) | (1L << BOOLEAN) | (1L << BYTE) | (1L << CHAR) | (1L << DOUBLE) | (1L << FLOAT) | (1L << INT) | (1L << LONG) | (1L << SHORT) | (1L << LPAREN))) != 0) || ((((_la - 66)) & ~0x3f) == 0 && ((1L << (_la - 66)) & ((1L << (DOT - 66)) | (1L << (BANG - 66)) | (1L << (MUL - 66)) | (1L << (Identifier - 66)))) != 0)) {
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
		public TerminalNode Identifier(int i) {
			return getToken(RefactorMethodSignatureParser.Identifier, i);
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
					if ( !(_la==DOTDOT || ((((_la - 66)) & ~0x3f) == 0 && ((1L << (_la - 66)) & ((1L << (DOT - 66)) | (1L << (MUL - 66)) | (1L << (Identifier - 66)))) != 0)) ) {
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
			setState(319);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,14,_ctx);
			while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(315); match(LBRACK);
					setState(316); match(RBRACK);
					}
					} 
				}
				setState(321);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,14,_ctx);
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
			setState(344);
			switch (_input.LA(1)) {
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(322); match(Identifier);
				setState(327);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,15,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(323); match(MUL);
						setState(324); match(Identifier);
						}
						} 
					}
					setState(329);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,15,_ctx);
				}
				setState(331);
				_la = _input.LA(1);
				if (_la==MUL) {
					{
					setState(330); match(MUL);
					}
				}

				}
				break;
			case MUL:
				enterOuterAlt(_localctx, 2);
				{
				setState(333); match(MUL);
				setState(338);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,17,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(334); match(Identifier);
						setState(335); match(MUL);
						}
						} 
					}
					setState(340);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,17,_ctx);
				}
				setState(342);
				_la = _input.LA(1);
				if (_la==Identifier) {
					{
					setState(341); match(Identifier);
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
			setState(347);
			switch ( getInterpreter().adaptivePredict(_input,20,_ctx) ) {
			case 1:
				{
				setState(346); packageDeclaration();
				}
				break;
			}
			setState(352);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==IMPORT) {
				{
				{
				setState(349); importDeclaration();
				}
				}
				setState(354);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(358);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ABSTRACT) | (1L << CLASS) | (1L << ENUM) | (1L << FINAL) | (1L << INTERFACE) | (1L << PRIVATE) | (1L << PROTECTED) | (1L << PUBLIC) | (1L << STATIC) | (1L << STRICTFP))) != 0) || _la==SEMI || _la==AT) {
				{
				{
				setState(355); typeDeclaration();
				}
				}
				setState(360);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(361); match(EOF);
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
			setState(366);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT) {
				{
				{
				setState(363); annotation();
				}
				}
				setState(368);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(369); match(PACKAGE);
			setState(370); qualifiedName();
			setState(371); match(SEMI);
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
			setState(373); match(IMPORT);
			setState(375);
			_la = _input.LA(1);
			if (_la==STATIC) {
				{
				setState(374); match(STATIC);
				}
			}

			setState(377); qualifiedName();
			setState(380);
			_la = _input.LA(1);
			if (_la==DOT) {
				{
				setState(378); match(DOT);
				setState(379); match(MUL);
				}
			}

			setState(382); match(SEMI);
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
			setState(413);
			switch ( getInterpreter().adaptivePredict(_input,30,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(387);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ABSTRACT) | (1L << FINAL) | (1L << PRIVATE) | (1L << PROTECTED) | (1L << PUBLIC) | (1L << STATIC) | (1L << STRICTFP))) != 0) || _la==AT) {
					{
					{
					setState(384); classOrInterfaceModifier();
					}
					}
					setState(389);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(390); classDeclaration();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(394);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ABSTRACT) | (1L << FINAL) | (1L << PRIVATE) | (1L << PROTECTED) | (1L << PUBLIC) | (1L << STATIC) | (1L << STRICTFP))) != 0) || _la==AT) {
					{
					{
					setState(391); classOrInterfaceModifier();
					}
					}
					setState(396);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(397); enumDeclaration();
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(401);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ABSTRACT) | (1L << FINAL) | (1L << PRIVATE) | (1L << PROTECTED) | (1L << PUBLIC) | (1L << STATIC) | (1L << STRICTFP))) != 0) || _la==AT) {
					{
					{
					setState(398); classOrInterfaceModifier();
					}
					}
					setState(403);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(404); interfaceDeclaration();
				}
				break;

			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(408);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,29,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(405); classOrInterfaceModifier();
						}
						} 
					}
					setState(410);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,29,_ctx);
				}
				setState(411); annotationTypeDeclaration();
				}
				break;

			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(412); match(SEMI);
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
			setState(417);
			switch (_input.LA(1)) {
			case ABSTRACT:
			case FINAL:
			case PRIVATE:
			case PROTECTED:
			case PUBLIC:
			case STATIC:
			case STRICTFP:
			case AT:
				enterOuterAlt(_localctx, 1);
				{
				setState(415); classOrInterfaceModifier();
				}
				break;
			case NATIVE:
			case SYNCHRONIZED:
			case TRANSIENT:
			case VOLATILE:
				enterOuterAlt(_localctx, 2);
				{
				setState(416);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << NATIVE) | (1L << SYNCHRONIZED) | (1L << TRANSIENT) | (1L << VOLATILE))) != 0)) ) {
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
			setState(421);
			switch (_input.LA(1)) {
			case AT:
				enterOuterAlt(_localctx, 1);
				{
				setState(419); annotation();
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
				setState(420);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ABSTRACT) | (1L << FINAL) | (1L << PRIVATE) | (1L << PROTECTED) | (1L << PUBLIC) | (1L << STATIC) | (1L << STRICTFP))) != 0)) ) {
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
			setState(425);
			switch (_input.LA(1)) {
			case FINAL:
				enterOuterAlt(_localctx, 1);
				{
				setState(423); match(FINAL);
				}
				break;
			case AT:
				enterOuterAlt(_localctx, 2);
				{
				setState(424); annotation();
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
			setState(427); match(CLASS);
			setState(428); match(Identifier);
			setState(430);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(429); typeParameters();
				}
			}

			setState(434);
			_la = _input.LA(1);
			if (_la==EXTENDS) {
				{
				setState(432); match(EXTENDS);
				setState(433); type();
				}
			}

			setState(438);
			_la = _input.LA(1);
			if (_la==IMPLEMENTS) {
				{
				setState(436); match(IMPLEMENTS);
				setState(437); typeList();
				}
			}

			setState(440); classBody();
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
			setState(442); match(LT);
			setState(443); typeParameter();
			setState(448);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(444); match(COMMA);
				setState(445); typeParameter();
				}
				}
				setState(450);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(451); match(GT);
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
			setState(453); match(Identifier);
			setState(456);
			_la = _input.LA(1);
			if (_la==EXTENDS) {
				{
				setState(454); match(EXTENDS);
				setState(455); typeBound();
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
			setState(458); type();
			setState(463);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==BITAND) {
				{
				{
				setState(459); match(BITAND);
				setState(460); type();
				}
				}
				setState(465);
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
			setState(466); match(ENUM);
			setState(467); match(Identifier);
			setState(470);
			_la = _input.LA(1);
			if (_la==IMPLEMENTS) {
				{
				setState(468); match(IMPLEMENTS);
				setState(469); typeList();
				}
			}

			setState(472); match(LBRACE);
			setState(474);
			_la = _input.LA(1);
			if (_la==Identifier || _la==AT) {
				{
				setState(473); enumConstants();
				}
			}

			setState(477);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(476); match(COMMA);
				}
			}

			setState(480);
			_la = _input.LA(1);
			if (_la==SEMI) {
				{
				setState(479); enumBodyDeclarations();
				}
			}

			setState(482); match(RBRACE);
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
			setState(484); enumConstant();
			setState(489);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,44,_ctx);
			while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(485); match(COMMA);
					setState(486); enumConstant();
					}
					} 
				}
				setState(491);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,44,_ctx);
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
			setState(495);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT) {
				{
				{
				setState(492); annotation();
				}
				}
				setState(497);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(498); match(Identifier);
			setState(500);
			_la = _input.LA(1);
			if (_la==LPAREN) {
				{
				setState(499); arguments();
				}
			}

			setState(503);
			_la = _input.LA(1);
			if (_la==LBRACE) {
				{
				setState(502); classBody();
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
			setState(505); match(SEMI);
			setState(509);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ABSTRACT) | (1L << BOOLEAN) | (1L << BYTE) | (1L << CHAR) | (1L << CLASS) | (1L << DOUBLE) | (1L << ENUM) | (1L << FINAL) | (1L << FLOAT) | (1L << INT) | (1L << INTERFACE) | (1L << LONG) | (1L << NATIVE) | (1L << PRIVATE) | (1L << PROTECTED) | (1L << PUBLIC) | (1L << SHORT) | (1L << STATIC) | (1L << STRICTFP) | (1L << SYNCHRONIZED) | (1L << TRANSIENT) | (1L << VOID) | (1L << VOLATILE) | (1L << LBRACE))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (SEMI - 64)) | (1L << (LT - 64)) | (1L << (Identifier - 64)) | (1L << (AT - 64)))) != 0)) {
				{
				{
				setState(506); classBodyDeclaration();
				}
				}
				setState(511);
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
			setState(512); match(INTERFACE);
			setState(513); match(Identifier);
			setState(515);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(514); typeParameters();
				}
			}

			setState(519);
			_la = _input.LA(1);
			if (_la==EXTENDS) {
				{
				setState(517); match(EXTENDS);
				setState(518); typeList();
				}
			}

			setState(521); interfaceBody();
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
			setState(523); type();
			setState(528);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(524); match(COMMA);
				setState(525); type();
				}
				}
				setState(530);
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
			setState(531); match(LBRACE);
			setState(535);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ABSTRACT) | (1L << BOOLEAN) | (1L << BYTE) | (1L << CHAR) | (1L << CLASS) | (1L << DOUBLE) | (1L << ENUM) | (1L << FINAL) | (1L << FLOAT) | (1L << INT) | (1L << INTERFACE) | (1L << LONG) | (1L << NATIVE) | (1L << PRIVATE) | (1L << PROTECTED) | (1L << PUBLIC) | (1L << SHORT) | (1L << STATIC) | (1L << STRICTFP) | (1L << SYNCHRONIZED) | (1L << TRANSIENT) | (1L << VOID) | (1L << VOLATILE) | (1L << LBRACE))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (SEMI - 64)) | (1L << (LT - 64)) | (1L << (Identifier - 64)) | (1L << (AT - 64)))) != 0)) {
				{
				{
				setState(532); classBodyDeclaration();
				}
				}
				setState(537);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(538); match(RBRACE);
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
			setState(540); match(LBRACE);
			setState(544);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ABSTRACT) | (1L << BOOLEAN) | (1L << BYTE) | (1L << CHAR) | (1L << CLASS) | (1L << DOUBLE) | (1L << ENUM) | (1L << FINAL) | (1L << FLOAT) | (1L << INT) | (1L << INTERFACE) | (1L << LONG) | (1L << NATIVE) | (1L << PRIVATE) | (1L << PROTECTED) | (1L << PUBLIC) | (1L << SHORT) | (1L << STATIC) | (1L << STRICTFP) | (1L << SYNCHRONIZED) | (1L << TRANSIENT) | (1L << VOID) | (1L << VOLATILE))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (SEMI - 64)) | (1L << (LT - 64)) | (1L << (Identifier - 64)) | (1L << (AT - 64)))) != 0)) {
				{
				{
				setState(541); interfaceBodyDeclaration();
				}
				}
				setState(546);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(547); match(RBRACE);
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
			setState(561);
			switch ( getInterpreter().adaptivePredict(_input,56,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(549); match(SEMI);
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(551);
				_la = _input.LA(1);
				if (_la==STATIC) {
					{
					setState(550); match(STATIC);
					}
				}

				setState(553); block();
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(557);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,55,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(554); modifier();
						}
						} 
					}
					setState(559);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,55,_ctx);
				}
				setState(560); memberDeclaration();
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
			setState(572);
			switch ( getInterpreter().adaptivePredict(_input,57,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(563); methodDeclaration();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(564); genericMethodDeclaration();
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(565); fieldDeclaration();
				}
				break;

			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(566); constructorDeclaration();
				}
				break;

			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(567); genericConstructorDeclaration();
				}
				break;

			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(568); interfaceDeclaration();
				}
				break;

			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(569); annotationTypeDeclaration();
				}
				break;

			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(570); classDeclaration();
				}
				break;

			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(571); enumDeclaration();
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
			setState(576);
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
				setState(574); type();
				}
				break;
			case VOID:
				{
				setState(575); match(VOID);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(578); match(Identifier);
			setState(579); formalParameters();
			setState(584);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==LBRACK) {
				{
				{
				setState(580); match(LBRACK);
				setState(581); match(RBRACK);
				}
				}
				setState(586);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(589);
			_la = _input.LA(1);
			if (_la==THROWS) {
				{
				setState(587); match(THROWS);
				setState(588); qualifiedNameList();
				}
			}

			setState(593);
			switch (_input.LA(1)) {
			case LBRACE:
				{
				setState(591); methodBody();
				}
				break;
			case SEMI:
				{
				setState(592); match(SEMI);
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
			setState(595); typeParameters();
			setState(596); methodDeclaration();
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
			setState(598); match(Identifier);
			setState(599); formalParameters();
			setState(602);
			_la = _input.LA(1);
			if (_la==THROWS) {
				{
				setState(600); match(THROWS);
				setState(601); qualifiedNameList();
				}
			}

			setState(604); constructorBody();
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
			setState(606); typeParameters();
			setState(607); constructorDeclaration();
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
			setState(609); type();
			setState(610); variableDeclarators();
			setState(611); match(SEMI);
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
			setState(621);
			switch (_input.LA(1)) {
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
			case AT:
				enterOuterAlt(_localctx, 1);
				{
				setState(616);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,63,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(613); modifier();
						}
						} 
					}
					setState(618);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,63,_ctx);
				}
				setState(619); interfaceMemberDeclaration();
				}
				break;
			case SEMI:
				enterOuterAlt(_localctx, 2);
				{
				setState(620); match(SEMI);
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
			setState(630);
			switch ( getInterpreter().adaptivePredict(_input,65,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(623); constDeclaration();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(624); interfaceMethodDeclaration();
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(625); genericInterfaceMethodDeclaration();
				}
				break;

			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(626); interfaceDeclaration();
				}
				break;

			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(627); annotationTypeDeclaration();
				}
				break;

			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(628); classDeclaration();
				}
				break;

			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(629); enumDeclaration();
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
			setState(632); type();
			setState(633); constantDeclarator();
			setState(638);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(634); match(COMMA);
				setState(635); constantDeclarator();
				}
				}
				setState(640);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(641); match(SEMI);
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
			setState(643); match(Identifier);
			setState(648);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==LBRACK) {
				{
				{
				setState(644); match(LBRACK);
				setState(645); match(RBRACK);
				}
				}
				setState(650);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(651); match(ASSIGN);
			setState(652); variableInitializer();
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
			setState(656);
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
				setState(654); type();
				}
				break;
			case VOID:
				{
				setState(655); match(VOID);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(658); match(Identifier);
			setState(659); formalParameters();
			setState(664);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==LBRACK) {
				{
				{
				setState(660); match(LBRACK);
				setState(661); match(RBRACK);
				}
				}
				setState(666);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(669);
			_la = _input.LA(1);
			if (_la==THROWS) {
				{
				setState(667); match(THROWS);
				setState(668); qualifiedNameList();
				}
			}

			setState(671); match(SEMI);
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
			setState(673); typeParameters();
			setState(674); interfaceMethodDeclaration();
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
			setState(676); variableDeclarator();
			setState(681);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(677); match(COMMA);
				setState(678); variableDeclarator();
				}
				}
				setState(683);
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
			setState(684); variableDeclaratorId();
			setState(687);
			_la = _input.LA(1);
			if (_la==ASSIGN) {
				{
				setState(685); match(ASSIGN);
				setState(686); variableInitializer();
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
			setState(689); match(Identifier);
			setState(694);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==LBRACK) {
				{
				{
				setState(690); match(LBRACK);
				setState(691); match(RBRACK);
				}
				}
				setState(696);
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
			setState(699);
			switch (_input.LA(1)) {
			case LBRACE:
				enterOuterAlt(_localctx, 1);
				{
				setState(697); arrayInitializer();
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
				setState(698); expression(0);
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
			setState(701); match(LBRACE);
			setState(713);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << BOOLEAN) | (1L << BYTE) | (1L << CHAR) | (1L << DOUBLE) | (1L << FLOAT) | (1L << INT) | (1L << LONG) | (1L << NEW) | (1L << SHORT) | (1L << SUPER) | (1L << THIS) | (1L << VOID) | (1L << IntegerLiteral) | (1L << FloatingPointLiteral) | (1L << BooleanLiteral) | (1L << CharacterLiteral) | (1L << StringLiteral) | (1L << NullLiteral) | (1L << LPAREN) | (1L << LBRACE))) != 0) || ((((_la - 69)) & ~0x3f) == 0 && ((1L << (_la - 69)) & ((1L << (LT - 69)) | (1L << (BANG - 69)) | (1L << (TILDE - 69)) | (1L << (INC - 69)) | (1L << (DEC - 69)) | (1L << (ADD - 69)) | (1L << (SUB - 69)) | (1L << (Identifier - 69)))) != 0)) {
				{
				setState(702); variableInitializer();
				setState(707);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,75,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(703); match(COMMA);
						setState(704); variableInitializer();
						}
						} 
					}
					setState(709);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,75,_ctx);
				}
				setState(711);
				_la = _input.LA(1);
				if (_la==COMMA) {
					{
					setState(710); match(COMMA);
					}
				}

				}
			}

			setState(715); match(RBRACE);
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
			setState(717); match(Identifier);
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
			setState(735);
			switch (_input.LA(1)) {
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(719); classOrInterfaceType();
				setState(724);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,78,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(720); match(LBRACK);
						setState(721); match(RBRACK);
						}
						} 
					}
					setState(726);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,78,_ctx);
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
				setState(727); primitiveType();
				setState(732);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,79,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(728); match(LBRACK);
						setState(729); match(RBRACK);
						}
						} 
					}
					setState(734);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,79,_ctx);
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
			setState(737); match(Identifier);
			setState(739);
			switch ( getInterpreter().adaptivePredict(_input,81,_ctx) ) {
			case 1:
				{
				setState(738); typeArguments();
				}
				break;
			}
			setState(748);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,83,_ctx);
			while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(741); match(DOT);
					setState(742); match(Identifier);
					setState(744);
					switch ( getInterpreter().adaptivePredict(_input,82,_ctx) ) {
					case 1:
						{
						setState(743); typeArguments();
						}
						break;
					}
					}
					} 
				}
				setState(750);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,83,_ctx);
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
			setState(751);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << BOOLEAN) | (1L << BYTE) | (1L << CHAR) | (1L << DOUBLE) | (1L << FLOAT) | (1L << INT) | (1L << LONG) | (1L << SHORT))) != 0)) ) {
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
			setState(753); match(LT);
			setState(754); typeArgument();
			setState(759);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(755); match(COMMA);
				setState(756); typeArgument();
				}
				}
				setState(761);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(762); match(GT);
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
			setState(770);
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
				setState(764); type();
				}
				break;
			case QUESTION:
				enterOuterAlt(_localctx, 2);
				{
				setState(765); match(QUESTION);
				setState(768);
				_la = _input.LA(1);
				if (_la==EXTENDS || _la==SUPER) {
					{
					setState(766);
					_la = _input.LA(1);
					if ( !(_la==EXTENDS || _la==SUPER) ) {
					_errHandler.recoverInline(this);
					}
					consume();
					setState(767); type();
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
			setState(772); qualifiedName();
			setState(777);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(773); match(COMMA);
				setState(774); qualifiedName();
				}
				}
				setState(779);
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
			setState(780); match(LPAREN);
			setState(782);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << BOOLEAN) | (1L << BYTE) | (1L << CHAR) | (1L << DOUBLE) | (1L << FINAL) | (1L << FLOAT) | (1L << INT) | (1L << LONG) | (1L << SHORT))) != 0) || _la==Identifier || _la==AT) {
				{
				setState(781); formalParameterList();
				}
			}

			setState(784); match(RPAREN);
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
			setState(799);
			switch ( getInterpreter().adaptivePredict(_input,91,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(786); formalParameter();
				setState(791);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,89,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(787); match(COMMA);
						setState(788); formalParameter();
						}
						} 
					}
					setState(793);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,89,_ctx);
				}
				setState(796);
				_la = _input.LA(1);
				if (_la==COMMA) {
					{
					setState(794); match(COMMA);
					setState(795); lastFormalParameter();
					}
				}

				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(798); lastFormalParameter();
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
			setState(804);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==FINAL || _la==AT) {
				{
				{
				setState(801); variableModifier();
				}
				}
				setState(806);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(807); type();
			setState(808); variableDeclaratorId();
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
			setState(813);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==FINAL || _la==AT) {
				{
				{
				setState(810); variableModifier();
				}
				}
				setState(815);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(816); type();
			setState(817); match(ELLIPSIS);
			setState(818); variableDeclaratorId();
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
			setState(820); block();
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
			setState(822); block();
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
			setState(824); match(Identifier);
			setState(829);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,94,_ctx);
			while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(825); match(DOT);
					setState(826); match(Identifier);
					}
					} 
				}
				setState(831);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,94,_ctx);
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
			setState(832);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << IntegerLiteral) | (1L << FloatingPointLiteral) | (1L << BooleanLiteral) | (1L << CharacterLiteral) | (1L << StringLiteral) | (1L << NullLiteral))) != 0)) ) {
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
			setState(834); match(AT);
			setState(835); annotationName();
			setState(842);
			_la = _input.LA(1);
			if (_la==LPAREN) {
				{
				setState(836); match(LPAREN);
				setState(839);
				switch ( getInterpreter().adaptivePredict(_input,95,_ctx) ) {
				case 1:
					{
					setState(837); elementValuePairs();
					}
					break;

				case 2:
					{
					setState(838); elementValue();
					}
					break;
				}
				setState(841); match(RPAREN);
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
			setState(844); qualifiedName();
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
			setState(846); elementValuePair();
			setState(851);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(847); match(COMMA);
				setState(848); elementValuePair();
				}
				}
				setState(853);
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
			setState(854); match(Identifier);
			setState(855); match(ASSIGN);
			setState(856); elementValue();
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
			setState(861);
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
				setState(858); expression(0);
				}
				break;
			case AT:
				enterOuterAlt(_localctx, 2);
				{
				setState(859); annotation();
				}
				break;
			case LBRACE:
				enterOuterAlt(_localctx, 3);
				{
				setState(860); elementValueArrayInitializer();
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
			setState(863); match(LBRACE);
			setState(872);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << BOOLEAN) | (1L << BYTE) | (1L << CHAR) | (1L << DOUBLE) | (1L << FLOAT) | (1L << INT) | (1L << LONG) | (1L << NEW) | (1L << SHORT) | (1L << SUPER) | (1L << THIS) | (1L << VOID) | (1L << IntegerLiteral) | (1L << FloatingPointLiteral) | (1L << BooleanLiteral) | (1L << CharacterLiteral) | (1L << StringLiteral) | (1L << NullLiteral) | (1L << LPAREN) | (1L << LBRACE))) != 0) || ((((_la - 69)) & ~0x3f) == 0 && ((1L << (_la - 69)) & ((1L << (LT - 69)) | (1L << (BANG - 69)) | (1L << (TILDE - 69)) | (1L << (INC - 69)) | (1L << (DEC - 69)) | (1L << (ADD - 69)) | (1L << (SUB - 69)) | (1L << (Identifier - 69)) | (1L << (AT - 69)))) != 0)) {
				{
				setState(864); elementValue();
				setState(869);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,99,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(865); match(COMMA);
						setState(866); elementValue();
						}
						} 
					}
					setState(871);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,99,_ctx);
				}
				}
			}

			setState(875);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(874); match(COMMA);
				}
			}

			setState(877); match(RBRACE);
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
			setState(879); match(AT);
			setState(880); match(INTERFACE);
			setState(881); match(Identifier);
			setState(882); annotationTypeBody();
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
			setState(884); match(LBRACE);
			setState(888);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ABSTRACT) | (1L << BOOLEAN) | (1L << BYTE) | (1L << CHAR) | (1L << CLASS) | (1L << DOUBLE) | (1L << ENUM) | (1L << FINAL) | (1L << FLOAT) | (1L << INT) | (1L << INTERFACE) | (1L << LONG) | (1L << NATIVE) | (1L << PRIVATE) | (1L << PROTECTED) | (1L << PUBLIC) | (1L << SHORT) | (1L << STATIC) | (1L << STRICTFP) | (1L << SYNCHRONIZED) | (1L << TRANSIENT) | (1L << VOLATILE))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (SEMI - 64)) | (1L << (Identifier - 64)) | (1L << (AT - 64)))) != 0)) {
				{
				{
				setState(885); annotationTypeElementDeclaration();
				}
				}
				setState(890);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(891); match(RBRACE);
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
			setState(901);
			switch (_input.LA(1)) {
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
			case AT:
				enterOuterAlt(_localctx, 1);
				{
				setState(896);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,103,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(893); modifier();
						}
						} 
					}
					setState(898);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,103,_ctx);
				}
				setState(899); annotationTypeElementRest();
				}
				break;
			case SEMI:
				enterOuterAlt(_localctx, 2);
				{
				setState(900); match(SEMI);
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
			setState(923);
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
				setState(903); type();
				setState(904); annotationMethodOrConstantRest();
				setState(905); match(SEMI);
				}
				break;
			case CLASS:
				enterOuterAlt(_localctx, 2);
				{
				setState(907); classDeclaration();
				setState(909);
				switch ( getInterpreter().adaptivePredict(_input,105,_ctx) ) {
				case 1:
					{
					setState(908); match(SEMI);
					}
					break;
				}
				}
				break;
			case INTERFACE:
				enterOuterAlt(_localctx, 3);
				{
				setState(911); interfaceDeclaration();
				setState(913);
				switch ( getInterpreter().adaptivePredict(_input,106,_ctx) ) {
				case 1:
					{
					setState(912); match(SEMI);
					}
					break;
				}
				}
				break;
			case ENUM:
				enterOuterAlt(_localctx, 4);
				{
				setState(915); enumDeclaration();
				setState(917);
				switch ( getInterpreter().adaptivePredict(_input,107,_ctx) ) {
				case 1:
					{
					setState(916); match(SEMI);
					}
					break;
				}
				}
				break;
			case AT:
				enterOuterAlt(_localctx, 5);
				{
				setState(919); annotationTypeDeclaration();
				setState(921);
				switch ( getInterpreter().adaptivePredict(_input,108,_ctx) ) {
				case 1:
					{
					setState(920); match(SEMI);
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
			setState(927);
			switch ( getInterpreter().adaptivePredict(_input,110,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(925); annotationMethodRest();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(926); annotationConstantRest();
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
			setState(929); match(Identifier);
			setState(930); match(LPAREN);
			setState(931); match(RPAREN);
			setState(933);
			_la = _input.LA(1);
			if (_la==DEFAULT) {
				{
				setState(932); defaultValue();
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
			setState(935); variableDeclarators();
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
			setState(937); match(DEFAULT);
			setState(938); elementValue();
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
			setState(940); match(LBRACE);
			setState(944);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ABSTRACT) | (1L << ASSERT) | (1L << BOOLEAN) | (1L << BREAK) | (1L << BYTE) | (1L << CHAR) | (1L << CLASS) | (1L << CONTINUE) | (1L << DO) | (1L << DOUBLE) | (1L << ENUM) | (1L << FINAL) | (1L << FLOAT) | (1L << FOR) | (1L << IF) | (1L << INT) | (1L << INTERFACE) | (1L << LONG) | (1L << NEW) | (1L << PRIVATE) | (1L << PROTECTED) | (1L << PUBLIC) | (1L << RETURN) | (1L << SHORT) | (1L << STATIC) | (1L << STRICTFP) | (1L << SUPER) | (1L << SWITCH) | (1L << SYNCHRONIZED) | (1L << THIS) | (1L << THROW) | (1L << TRY) | (1L << VOID) | (1L << WHILE) | (1L << IntegerLiteral) | (1L << FloatingPointLiteral) | (1L << BooleanLiteral) | (1L << CharacterLiteral) | (1L << StringLiteral) | (1L << NullLiteral) | (1L << LPAREN) | (1L << LBRACE))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (SEMI - 64)) | (1L << (LT - 64)) | (1L << (BANG - 64)) | (1L << (TILDE - 64)) | (1L << (INC - 64)) | (1L << (DEC - 64)) | (1L << (ADD - 64)) | (1L << (SUB - 64)) | (1L << (Identifier - 64)) | (1L << (AT - 64)))) != 0)) {
				{
				{
				setState(941); blockStatement();
				}
				}
				setState(946);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(947); match(RBRACE);
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
			setState(952);
			switch ( getInterpreter().adaptivePredict(_input,113,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(949); localVariableDeclarationStatement();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(950); statement();
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(951); typeDeclaration();
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
			setState(954); localVariableDeclaration();
			setState(955); match(SEMI);
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
			setState(960);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==FINAL || _la==AT) {
				{
				{
				setState(957); variableModifier();
				}
				}
				setState(962);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(963); type();
			setState(964); variableDeclarators();
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
			setState(1070);
			switch ( getInterpreter().adaptivePredict(_input,127,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(966); block();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(967); match(ASSERT);
				setState(968); expression(0);
				setState(971);
				_la = _input.LA(1);
				if (_la==COLON) {
					{
					setState(969); match(COLON);
					setState(970); expression(0);
					}
				}

				setState(973); match(SEMI);
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(975); match(IF);
				setState(976); parExpression();
				setState(977); statement();
				setState(980);
				switch ( getInterpreter().adaptivePredict(_input,116,_ctx) ) {
				case 1:
					{
					setState(978); match(ELSE);
					setState(979); statement();
					}
					break;
				}
				}
				break;

			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(982); match(FOR);
				setState(983); match(LPAREN);
				setState(984); forControl();
				setState(985); match(RPAREN);
				setState(986); statement();
				}
				break;

			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(988); match(WHILE);
				setState(989); parExpression();
				setState(990); statement();
				}
				break;

			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(992); match(DO);
				setState(993); statement();
				setState(994); match(WHILE);
				setState(995); parExpression();
				setState(996); match(SEMI);
				}
				break;

			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(998); match(TRY);
				setState(999); block();
				setState(1009);
				switch (_input.LA(1)) {
				case CATCH:
					{
					setState(1001); 
					_errHandler.sync(this);
					_la = _input.LA(1);
					do {
						{
						{
						setState(1000); catchClause();
						}
						}
						setState(1003); 
						_errHandler.sync(this);
						_la = _input.LA(1);
					} while ( _la==CATCH );
					setState(1006);
					_la = _input.LA(1);
					if (_la==FINALLY) {
						{
						setState(1005); finallyBlock();
						}
					}

					}
					break;
				case FINALLY:
					{
					setState(1008); finallyBlock();
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
				setState(1011); match(TRY);
				setState(1012); resourceSpecification();
				setState(1013); block();
				setState(1017);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==CATCH) {
					{
					{
					setState(1014); catchClause();
					}
					}
					setState(1019);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(1021);
				_la = _input.LA(1);
				if (_la==FINALLY) {
					{
					setState(1020); finallyBlock();
					}
				}

				}
				break;

			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(1023); match(SWITCH);
				setState(1024); parExpression();
				setState(1025); match(LBRACE);
				setState(1029);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,122,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1026); switchBlockStatementGroup();
						}
						} 
					}
					setState(1031);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,122,_ctx);
				}
				setState(1035);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==CASE || _la==DEFAULT) {
					{
					{
					setState(1032); switchLabel();
					}
					}
					setState(1037);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(1038); match(RBRACE);
				}
				break;

			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(1040); match(SYNCHRONIZED);
				setState(1041); parExpression();
				setState(1042); block();
				}
				break;

			case 11:
				enterOuterAlt(_localctx, 11);
				{
				setState(1044); match(RETURN);
				setState(1046);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << BOOLEAN) | (1L << BYTE) | (1L << CHAR) | (1L << DOUBLE) | (1L << FLOAT) | (1L << INT) | (1L << LONG) | (1L << NEW) | (1L << SHORT) | (1L << SUPER) | (1L << THIS) | (1L << VOID) | (1L << IntegerLiteral) | (1L << FloatingPointLiteral) | (1L << BooleanLiteral) | (1L << CharacterLiteral) | (1L << StringLiteral) | (1L << NullLiteral) | (1L << LPAREN))) != 0) || ((((_la - 69)) & ~0x3f) == 0 && ((1L << (_la - 69)) & ((1L << (LT - 69)) | (1L << (BANG - 69)) | (1L << (TILDE - 69)) | (1L << (INC - 69)) | (1L << (DEC - 69)) | (1L << (ADD - 69)) | (1L << (SUB - 69)) | (1L << (Identifier - 69)))) != 0)) {
					{
					setState(1045); expression(0);
					}
				}

				setState(1048); match(SEMI);
				}
				break;

			case 12:
				enterOuterAlt(_localctx, 12);
				{
				setState(1049); match(THROW);
				setState(1050); expression(0);
				setState(1051); match(SEMI);
				}
				break;

			case 13:
				enterOuterAlt(_localctx, 13);
				{
				setState(1053); match(BREAK);
				setState(1055);
				_la = _input.LA(1);
				if (_la==Identifier) {
					{
					setState(1054); match(Identifier);
					}
				}

				setState(1057); match(SEMI);
				}
				break;

			case 14:
				enterOuterAlt(_localctx, 14);
				{
				setState(1058); match(CONTINUE);
				setState(1060);
				_la = _input.LA(1);
				if (_la==Identifier) {
					{
					setState(1059); match(Identifier);
					}
				}

				setState(1062); match(SEMI);
				}
				break;

			case 15:
				enterOuterAlt(_localctx, 15);
				{
				setState(1063); match(SEMI);
				}
				break;

			case 16:
				enterOuterAlt(_localctx, 16);
				{
				setState(1064); statementExpression();
				setState(1065); match(SEMI);
				}
				break;

			case 17:
				enterOuterAlt(_localctx, 17);
				{
				setState(1067); match(Identifier);
				setState(1068); match(COLON);
				setState(1069); statement();
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
			setState(1072); match(CATCH);
			setState(1073); match(LPAREN);
			setState(1077);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==FINAL || _la==AT) {
				{
				{
				setState(1074); variableModifier();
				}
				}
				setState(1079);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1080); catchType();
			setState(1081); match(Identifier);
			setState(1082); match(RPAREN);
			setState(1083); block();
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
			setState(1085); qualifiedName();
			setState(1090);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==BITOR) {
				{
				{
				setState(1086); match(BITOR);
				setState(1087); qualifiedName();
				}
				}
				setState(1092);
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
			setState(1093); match(FINALLY);
			setState(1094); block();
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
			setState(1096); match(LPAREN);
			setState(1097); resources();
			setState(1099);
			_la = _input.LA(1);
			if (_la==SEMI) {
				{
				setState(1098); match(SEMI);
				}
			}

			setState(1101); match(RPAREN);
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
			setState(1103); resource();
			setState(1108);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,131,_ctx);
			while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1104); match(SEMI);
					setState(1105); resource();
					}
					} 
				}
				setState(1110);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,131,_ctx);
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
			setState(1114);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==FINAL || _la==AT) {
				{
				{
				setState(1111); variableModifier();
				}
				}
				setState(1116);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1117); classOrInterfaceType();
			setState(1118); variableDeclaratorId();
			setState(1119); match(ASSIGN);
			setState(1120); expression(0);
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
			setState(1123); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(1122); switchLabel();
				}
				}
				setState(1125); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==CASE || _la==DEFAULT );
			setState(1128); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(1127); blockStatement();
				}
				}
				setState(1130); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ABSTRACT) | (1L << ASSERT) | (1L << BOOLEAN) | (1L << BREAK) | (1L << BYTE) | (1L << CHAR) | (1L << CLASS) | (1L << CONTINUE) | (1L << DO) | (1L << DOUBLE) | (1L << ENUM) | (1L << FINAL) | (1L << FLOAT) | (1L << FOR) | (1L << IF) | (1L << INT) | (1L << INTERFACE) | (1L << LONG) | (1L << NEW) | (1L << PRIVATE) | (1L << PROTECTED) | (1L << PUBLIC) | (1L << RETURN) | (1L << SHORT) | (1L << STATIC) | (1L << STRICTFP) | (1L << SUPER) | (1L << SWITCH) | (1L << SYNCHRONIZED) | (1L << THIS) | (1L << THROW) | (1L << TRY) | (1L << VOID) | (1L << WHILE) | (1L << IntegerLiteral) | (1L << FloatingPointLiteral) | (1L << BooleanLiteral) | (1L << CharacterLiteral) | (1L << StringLiteral) | (1L << NullLiteral) | (1L << LPAREN) | (1L << LBRACE))) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & ((1L << (SEMI - 64)) | (1L << (LT - 64)) | (1L << (BANG - 64)) | (1L << (TILDE - 64)) | (1L << (INC - 64)) | (1L << (DEC - 64)) | (1L << (ADD - 64)) | (1L << (SUB - 64)) | (1L << (Identifier - 64)) | (1L << (AT - 64)))) != 0) );
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
			setState(1142);
			switch ( getInterpreter().adaptivePredict(_input,135,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1132); match(CASE);
				setState(1133); constantExpression();
				setState(1134); match(COLON);
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1136); match(CASE);
				setState(1137); enumConstantName();
				setState(1138); match(COLON);
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1140); match(DEFAULT);
				setState(1141); match(COLON);
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
			setState(1156);
			switch ( getInterpreter().adaptivePredict(_input,139,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1144); enhancedForControl();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1146);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << BOOLEAN) | (1L << BYTE) | (1L << CHAR) | (1L << DOUBLE) | (1L << FINAL) | (1L << FLOAT) | (1L << INT) | (1L << LONG) | (1L << NEW) | (1L << SHORT) | (1L << SUPER) | (1L << THIS) | (1L << VOID) | (1L << IntegerLiteral) | (1L << FloatingPointLiteral) | (1L << BooleanLiteral) | (1L << CharacterLiteral) | (1L << StringLiteral) | (1L << NullLiteral) | (1L << LPAREN))) != 0) || ((((_la - 69)) & ~0x3f) == 0 && ((1L << (_la - 69)) & ((1L << (LT - 69)) | (1L << (BANG - 69)) | (1L << (TILDE - 69)) | (1L << (INC - 69)) | (1L << (DEC - 69)) | (1L << (ADD - 69)) | (1L << (SUB - 69)) | (1L << (Identifier - 69)) | (1L << (AT - 69)))) != 0)) {
					{
					setState(1145); forInit();
					}
				}

				setState(1148); match(SEMI);
				setState(1150);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << BOOLEAN) | (1L << BYTE) | (1L << CHAR) | (1L << DOUBLE) | (1L << FLOAT) | (1L << INT) | (1L << LONG) | (1L << NEW) | (1L << SHORT) | (1L << SUPER) | (1L << THIS) | (1L << VOID) | (1L << IntegerLiteral) | (1L << FloatingPointLiteral) | (1L << BooleanLiteral) | (1L << CharacterLiteral) | (1L << StringLiteral) | (1L << NullLiteral) | (1L << LPAREN))) != 0) || ((((_la - 69)) & ~0x3f) == 0 && ((1L << (_la - 69)) & ((1L << (LT - 69)) | (1L << (BANG - 69)) | (1L << (TILDE - 69)) | (1L << (INC - 69)) | (1L << (DEC - 69)) | (1L << (ADD - 69)) | (1L << (SUB - 69)) | (1L << (Identifier - 69)))) != 0)) {
					{
					setState(1149); expression(0);
					}
				}

				setState(1152); match(SEMI);
				setState(1154);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << BOOLEAN) | (1L << BYTE) | (1L << CHAR) | (1L << DOUBLE) | (1L << FLOAT) | (1L << INT) | (1L << LONG) | (1L << NEW) | (1L << SHORT) | (1L << SUPER) | (1L << THIS) | (1L << VOID) | (1L << IntegerLiteral) | (1L << FloatingPointLiteral) | (1L << BooleanLiteral) | (1L << CharacterLiteral) | (1L << StringLiteral) | (1L << NullLiteral) | (1L << LPAREN))) != 0) || ((((_la - 69)) & ~0x3f) == 0 && ((1L << (_la - 69)) & ((1L << (LT - 69)) | (1L << (BANG - 69)) | (1L << (TILDE - 69)) | (1L << (INC - 69)) | (1L << (DEC - 69)) | (1L << (ADD - 69)) | (1L << (SUB - 69)) | (1L << (Identifier - 69)))) != 0)) {
					{
					setState(1153); forUpdate();
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
			setState(1160);
			switch ( getInterpreter().adaptivePredict(_input,140,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1158); localVariableDeclaration();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1159); expressionList();
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
			setState(1165);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==FINAL || _la==AT) {
				{
				{
				setState(1162); variableModifier();
				}
				}
				setState(1167);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1168); type();
			setState(1169); variableDeclaratorId();
			setState(1170); match(COLON);
			setState(1171); expression(0);
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
			setState(1173); expressionList();
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
			setState(1175); match(LPAREN);
			setState(1176); expression(0);
			setState(1177); match(RPAREN);
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
			setState(1179); expression(0);
			setState(1184);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1180); match(COMMA);
				setState(1181); expression(0);
				}
				}
				setState(1186);
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
			setState(1187); expression(0);
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
			setState(1189); expression(0);
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
			setState(1204);
			switch ( getInterpreter().adaptivePredict(_input,143,_ctx) ) {
			case 1:
				{
				setState(1192); match(LPAREN);
				setState(1193); type();
				setState(1194); match(RPAREN);
				setState(1195); expression(17);
				}
				break;

			case 2:
				{
				setState(1197);
				_la = _input.LA(1);
				if ( !(((((_la - 80)) & ~0x3f) == 0 && ((1L << (_la - 80)) & ((1L << (INC - 80)) | (1L << (DEC - 80)) | (1L << (ADD - 80)) | (1L << (SUB - 80)))) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				consume();
				setState(1198); expression(15);
				}
				break;

			case 3:
				{
				setState(1199);
				_la = _input.LA(1);
				if ( !(_la==BANG || _la==TILDE) ) {
				_errHandler.recoverInline(this);
				}
				consume();
				setState(1200); expression(14);
				}
				break;

			case 4:
				{
				setState(1201); primary();
				}
				break;

			case 5:
				{
				setState(1202); match(NEW);
				setState(1203); creator();
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(1291);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,148,_ctx);
			while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(1289);
					switch ( getInterpreter().adaptivePredict(_input,147,_ctx) ) {
					case 1:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1206);
						if (!(precpred(_ctx, 13))) throw new FailedPredicateException(this, "precpred(_ctx, 13)");
						setState(1207);
						_la = _input.LA(1);
						if ( !(((((_la - 84)) & ~0x3f) == 0 && ((1L << (_la - 84)) & ((1L << (MUL - 84)) | (1L << (DIV - 84)) | (1L << (MOD - 84)))) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(1208); expression(14);
						}
						break;

					case 2:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1209);
						if (!(precpred(_ctx, 12))) throw new FailedPredicateException(this, "precpred(_ctx, 12)");
						setState(1210);
						_la = _input.LA(1);
						if ( !(_la==ADD || _la==SUB) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(1211); expression(13);
						}
						break;

					case 3:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1212);
						if (!(precpred(_ctx, 11))) throw new FailedPredicateException(this, "precpred(_ctx, 11)");
						setState(1220);
						switch ( getInterpreter().adaptivePredict(_input,144,_ctx) ) {
						case 1:
							{
							setState(1213); match(LT);
							setState(1214); match(LT);
							}
							break;

						case 2:
							{
							setState(1215); match(GT);
							setState(1216); match(GT);
							setState(1217); match(GT);
							}
							break;

						case 3:
							{
							setState(1218); match(GT);
							setState(1219); match(GT);
							}
							break;
						}
						setState(1222); expression(12);
						}
						break;

					case 4:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1223);
						if (!(precpred(_ctx, 10))) throw new FailedPredicateException(this, "precpred(_ctx, 10)");
						setState(1224);
						_la = _input.LA(1);
						if ( !(((((_la - 68)) & ~0x3f) == 0 && ((1L << (_la - 68)) & ((1L << (GT - 68)) | (1L << (LT - 68)) | (1L << (LE - 68)) | (1L << (GE - 68)))) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(1225); expression(11);
						}
						break;

					case 5:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1226);
						if (!(precpred(_ctx, 8))) throw new FailedPredicateException(this, "precpred(_ctx, 8)");
						setState(1227);
						_la = _input.LA(1);
						if ( !(_la==EQUAL || _la==NOTEQUAL) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(1228); expression(9);
						}
						break;

					case 6:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1229);
						if (!(precpred(_ctx, 7))) throw new FailedPredicateException(this, "precpred(_ctx, 7)");
						setState(1230); match(BITAND);
						setState(1231); expression(8);
						}
						break;

					case 7:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1232);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(1233); match(CARET);
						setState(1234); expression(7);
						}
						break;

					case 8:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1235);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(1236); match(BITOR);
						setState(1237); expression(6);
						}
						break;

					case 9:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1238);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(1239); match(AND);
						setState(1240); expression(5);
						}
						break;

					case 10:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1241);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(1242); match(OR);
						setState(1243); expression(4);
						}
						break;

					case 11:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1244);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(1245); match(QUESTION);
						setState(1246); expression(0);
						setState(1247); match(COLON);
						setState(1248); expression(3);
						}
						break;

					case 12:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1250);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(1251);
						_la = _input.LA(1);
						if ( !(((((_la - 67)) & ~0x3f) == 0 && ((1L << (_la - 67)) & ((1L << (ASSIGN - 67)) | (1L << (ADD_ASSIGN - 67)) | (1L << (SUB_ASSIGN - 67)) | (1L << (MUL_ASSIGN - 67)) | (1L << (DIV_ASSIGN - 67)) | (1L << (AND_ASSIGN - 67)) | (1L << (OR_ASSIGN - 67)) | (1L << (XOR_ASSIGN - 67)) | (1L << (MOD_ASSIGN - 67)) | (1L << (LSHIFT_ASSIGN - 67)) | (1L << (RSHIFT_ASSIGN - 67)) | (1L << (URSHIFT_ASSIGN - 67)))) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(1252); expression(1);
						}
						break;

					case 13:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1253);
						if (!(precpred(_ctx, 25))) throw new FailedPredicateException(this, "precpred(_ctx, 25)");
						setState(1254); match(DOT);
						setState(1255); match(Identifier);
						}
						break;

					case 14:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1256);
						if (!(precpred(_ctx, 24))) throw new FailedPredicateException(this, "precpred(_ctx, 24)");
						setState(1257); match(DOT);
						setState(1258); match(THIS);
						}
						break;

					case 15:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1259);
						if (!(precpred(_ctx, 23))) throw new FailedPredicateException(this, "precpred(_ctx, 23)");
						setState(1260); match(DOT);
						setState(1261); match(NEW);
						setState(1263);
						_la = _input.LA(1);
						if (_la==LT) {
							{
							setState(1262); nonWildcardTypeArguments();
							}
						}

						setState(1265); innerCreator();
						}
						break;

					case 16:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1266);
						if (!(precpred(_ctx, 22))) throw new FailedPredicateException(this, "precpred(_ctx, 22)");
						setState(1267); match(DOT);
						setState(1268); match(SUPER);
						setState(1269); superSuffix();
						}
						break;

					case 17:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1270);
						if (!(precpred(_ctx, 21))) throw new FailedPredicateException(this, "precpred(_ctx, 21)");
						setState(1271); match(DOT);
						setState(1272); explicitGenericInvocation();
						}
						break;

					case 18:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1273);
						if (!(precpred(_ctx, 20))) throw new FailedPredicateException(this, "precpred(_ctx, 20)");
						setState(1274); match(LBRACK);
						setState(1275); expression(0);
						setState(1276); match(RBRACK);
						}
						break;

					case 19:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1278);
						if (!(precpred(_ctx, 19))) throw new FailedPredicateException(this, "precpred(_ctx, 19)");
						setState(1279); match(LPAREN);
						setState(1281);
						_la = _input.LA(1);
						if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << BOOLEAN) | (1L << BYTE) | (1L << CHAR) | (1L << DOUBLE) | (1L << FLOAT) | (1L << INT) | (1L << LONG) | (1L << NEW) | (1L << SHORT) | (1L << SUPER) | (1L << THIS) | (1L << VOID) | (1L << IntegerLiteral) | (1L << FloatingPointLiteral) | (1L << BooleanLiteral) | (1L << CharacterLiteral) | (1L << StringLiteral) | (1L << NullLiteral) | (1L << LPAREN))) != 0) || ((((_la - 69)) & ~0x3f) == 0 && ((1L << (_la - 69)) & ((1L << (LT - 69)) | (1L << (BANG - 69)) | (1L << (TILDE - 69)) | (1L << (INC - 69)) | (1L << (DEC - 69)) | (1L << (ADD - 69)) | (1L << (SUB - 69)) | (1L << (Identifier - 69)))) != 0)) {
							{
							setState(1280); expressionList();
							}
						}

						setState(1283); match(RPAREN);
						}
						break;

					case 20:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1284);
						if (!(precpred(_ctx, 16))) throw new FailedPredicateException(this, "precpred(_ctx, 16)");
						setState(1285);
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
						setState(1286);
						if (!(precpred(_ctx, 9))) throw new FailedPredicateException(this, "precpred(_ctx, 9)");
						setState(1287); match(INSTANCEOF);
						setState(1288); type();
						}
						break;
					}
					} 
				}
				setState(1293);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,148,_ctx);
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
			setState(1315);
			switch ( getInterpreter().adaptivePredict(_input,150,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1294); match(LPAREN);
				setState(1295); expression(0);
				setState(1296); match(RPAREN);
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1298); match(THIS);
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1299); match(SUPER);
				}
				break;

			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1300); literal();
				}
				break;

			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(1301); match(Identifier);
				}
				break;

			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(1302); type();
				setState(1303); match(DOT);
				setState(1304); match(CLASS);
				}
				break;

			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(1306); match(VOID);
				setState(1307); match(DOT);
				setState(1308); match(CLASS);
				}
				break;

			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(1309); nonWildcardTypeArguments();
				setState(1313);
				switch (_input.LA(1)) {
				case SUPER:
				case Identifier:
					{
					setState(1310); explicitGenericInvocationSuffix();
					}
					break;
				case THIS:
					{
					setState(1311); match(THIS);
					setState(1312); arguments();
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
			setState(1326);
			switch (_input.LA(1)) {
			case LT:
				enterOuterAlt(_localctx, 1);
				{
				setState(1317); nonWildcardTypeArguments();
				setState(1318); createdName();
				setState(1319); classCreatorRest();
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
				setState(1321); createdName();
				setState(1324);
				switch (_input.LA(1)) {
				case LBRACK:
					{
					setState(1322); arrayCreatorRest();
					}
					break;
				case LPAREN:
					{
					setState(1323); classCreatorRest();
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
			setState(1343);
			switch (_input.LA(1)) {
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(1328); match(Identifier);
				setState(1330);
				_la = _input.LA(1);
				if (_la==LT) {
					{
					setState(1329); typeArgumentsOrDiamond();
					}
				}

				setState(1339);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==DOT) {
					{
					{
					setState(1332); match(DOT);
					setState(1333); match(Identifier);
					setState(1335);
					_la = _input.LA(1);
					if (_la==LT) {
						{
						setState(1334); typeArgumentsOrDiamond();
						}
					}

					}
					}
					setState(1341);
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
				setState(1342); primitiveType();
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
			setState(1345); match(Identifier);
			setState(1347);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(1346); nonWildcardTypeArgumentsOrDiamond();
				}
			}

			setState(1349); classCreatorRest();
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
			setState(1351); match(LBRACK);
			setState(1379);
			switch (_input.LA(1)) {
			case RBRACK:
				{
				setState(1352); match(RBRACK);
				setState(1357);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==LBRACK) {
					{
					{
					setState(1353); match(LBRACK);
					setState(1354); match(RBRACK);
					}
					}
					setState(1359);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(1360); arrayInitializer();
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
				setState(1361); expression(0);
				setState(1362); match(RBRACK);
				setState(1369);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,159,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1363); match(LBRACK);
						setState(1364); expression(0);
						setState(1365); match(RBRACK);
						}
						} 
					}
					setState(1371);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,159,_ctx);
				}
				setState(1376);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,160,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1372); match(LBRACK);
						setState(1373); match(RBRACK);
						}
						} 
					}
					setState(1378);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,160,_ctx);
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
			setState(1381); arguments();
			setState(1383);
			switch ( getInterpreter().adaptivePredict(_input,162,_ctx) ) {
			case 1:
				{
				setState(1382); classBody();
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
			setState(1385); nonWildcardTypeArguments();
			setState(1386); explicitGenericInvocationSuffix();
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
			setState(1388); match(LT);
			setState(1389); typeList();
			setState(1390); match(GT);
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
			setState(1395);
			switch ( getInterpreter().adaptivePredict(_input,163,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1392); match(LT);
				setState(1393); match(GT);
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1394); typeArguments();
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
			setState(1400);
			switch ( getInterpreter().adaptivePredict(_input,164,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1397); match(LT);
				setState(1398); match(GT);
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1399); nonWildcardTypeArguments();
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
			setState(1408);
			switch (_input.LA(1)) {
			case LPAREN:
				enterOuterAlt(_localctx, 1);
				{
				setState(1402); arguments();
				}
				break;
			case DOT:
				enterOuterAlt(_localctx, 2);
				{
				setState(1403); match(DOT);
				setState(1404); match(Identifier);
				setState(1406);
				switch ( getInterpreter().adaptivePredict(_input,165,_ctx) ) {
				case 1:
					{
					setState(1405); arguments();
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
			setState(1414);
			switch (_input.LA(1)) {
			case SUPER:
				enterOuterAlt(_localctx, 1);
				{
				setState(1410); match(SUPER);
				setState(1411); superSuffix();
				}
				break;
			case Identifier:
				enterOuterAlt(_localctx, 2);
				{
				setState(1412); match(Identifier);
				setState(1413); arguments();
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
			setState(1416); match(LPAREN);
			setState(1418);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << BOOLEAN) | (1L << BYTE) | (1L << CHAR) | (1L << DOUBLE) | (1L << FLOAT) | (1L << INT) | (1L << LONG) | (1L << NEW) | (1L << SHORT) | (1L << SUPER) | (1L << THIS) | (1L << VOID) | (1L << IntegerLiteral) | (1L << FloatingPointLiteral) | (1L << BooleanLiteral) | (1L << CharacterLiteral) | (1L << StringLiteral) | (1L << NullLiteral) | (1L << LPAREN))) != 0) || ((((_la - 69)) & ~0x3f) == 0 && ((1L << (_la - 69)) & ((1L << (LT - 69)) | (1L << (BANG - 69)) | (1L << (TILDE - 69)) | (1L << (INC - 69)) | (1L << (DEC - 69)) | (1L << (ADD - 69)) | (1L << (SUB - 69)) | (1L << (Identifier - 69)))) != 0)) {
				{
				setState(1417); expressionList();
				}
			}

			setState(1420); match(RPAREN);
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
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3l\u0591\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
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
		"\3\n\6\n\u013a\n\n\r\n\16\n\u013b\3\n\3\n\7\n\u0140\n\n\f\n\16\n\u0143"+
		"\13\n\3\13\3\13\3\13\7\13\u0148\n\13\f\13\16\13\u014b\13\13\3\13\5\13"+
		"\u014e\n\13\3\13\3\13\3\13\7\13\u0153\n\13\f\13\16\13\u0156\13\13\3\13"+
		"\5\13\u0159\n\13\5\13\u015b\n\13\3\f\5\f\u015e\n\f\3\f\7\f\u0161\n\f\f"+
		"\f\16\f\u0164\13\f\3\f\7\f\u0167\n\f\f\f\16\f\u016a\13\f\3\f\3\f\3\r\7"+
		"\r\u016f\n\r\f\r\16\r\u0172\13\r\3\r\3\r\3\r\3\r\3\16\3\16\5\16\u017a"+
		"\n\16\3\16\3\16\3\16\5\16\u017f\n\16\3\16\3\16\3\17\7\17\u0184\n\17\f"+
		"\17\16\17\u0187\13\17\3\17\3\17\7\17\u018b\n\17\f\17\16\17\u018e\13\17"+
		"\3\17\3\17\7\17\u0192\n\17\f\17\16\17\u0195\13\17\3\17\3\17\7\17\u0199"+
		"\n\17\f\17\16\17\u019c\13\17\3\17\3\17\5\17\u01a0\n\17\3\20\3\20\5\20"+
		"\u01a4\n\20\3\21\3\21\5\21\u01a8\n\21\3\22\3\22\5\22\u01ac\n\22\3\23\3"+
		"\23\3\23\5\23\u01b1\n\23\3\23\3\23\5\23\u01b5\n\23\3\23\3\23\5\23\u01b9"+
		"\n\23\3\23\3\23\3\24\3\24\3\24\3\24\7\24\u01c1\n\24\f\24\16\24\u01c4\13"+
		"\24\3\24\3\24\3\25\3\25\3\25\5\25\u01cb\n\25\3\26\3\26\3\26\7\26\u01d0"+
		"\n\26\f\26\16\26\u01d3\13\26\3\27\3\27\3\27\3\27\5\27\u01d9\n\27\3\27"+
		"\3\27\5\27\u01dd\n\27\3\27\5\27\u01e0\n\27\3\27\5\27\u01e3\n\27\3\27\3"+
		"\27\3\30\3\30\3\30\7\30\u01ea\n\30\f\30\16\30\u01ed\13\30\3\31\7\31\u01f0"+
		"\n\31\f\31\16\31\u01f3\13\31\3\31\3\31\5\31\u01f7\n\31\3\31\5\31\u01fa"+
		"\n\31\3\32\3\32\7\32\u01fe\n\32\f\32\16\32\u0201\13\32\3\33\3\33\3\33"+
		"\5\33\u0206\n\33\3\33\3\33\5\33\u020a\n\33\3\33\3\33\3\34\3\34\3\34\7"+
		"\34\u0211\n\34\f\34\16\34\u0214\13\34\3\35\3\35\7\35\u0218\n\35\f\35\16"+
		"\35\u021b\13\35\3\35\3\35\3\36\3\36\7\36\u0221\n\36\f\36\16\36\u0224\13"+
		"\36\3\36\3\36\3\37\3\37\5\37\u022a\n\37\3\37\3\37\7\37\u022e\n\37\f\37"+
		"\16\37\u0231\13\37\3\37\5\37\u0234\n\37\3 \3 \3 \3 \3 \3 \3 \3 \3 \5 "+
		"\u023f\n \3!\3!\5!\u0243\n!\3!\3!\3!\3!\7!\u0249\n!\f!\16!\u024c\13!\3"+
		"!\3!\5!\u0250\n!\3!\3!\5!\u0254\n!\3\"\3\"\3\"\3#\3#\3#\3#\5#\u025d\n"+
		"#\3#\3#\3$\3$\3$\3%\3%\3%\3%\3&\7&\u0269\n&\f&\16&\u026c\13&\3&\3&\5&"+
		"\u0270\n&\3\'\3\'\3\'\3\'\3\'\3\'\3\'\5\'\u0279\n\'\3(\3(\3(\3(\7(\u027f"+
		"\n(\f(\16(\u0282\13(\3(\3(\3)\3)\3)\7)\u0289\n)\f)\16)\u028c\13)\3)\3"+
		")\3)\3*\3*\5*\u0293\n*\3*\3*\3*\3*\7*\u0299\n*\f*\16*\u029c\13*\3*\3*"+
		"\5*\u02a0\n*\3*\3*\3+\3+\3+\3,\3,\3,\7,\u02aa\n,\f,\16,\u02ad\13,\3-\3"+
		"-\3-\5-\u02b2\n-\3.\3.\3.\7.\u02b7\n.\f.\16.\u02ba\13.\3/\3/\5/\u02be"+
		"\n/\3\60\3\60\3\60\3\60\7\60\u02c4\n\60\f\60\16\60\u02c7\13\60\3\60\5"+
		"\60\u02ca\n\60\5\60\u02cc\n\60\3\60\3\60\3\61\3\61\3\62\3\62\3\62\7\62"+
		"\u02d5\n\62\f\62\16\62\u02d8\13\62\3\62\3\62\3\62\7\62\u02dd\n\62\f\62"+
		"\16\62\u02e0\13\62\5\62\u02e2\n\62\3\63\3\63\5\63\u02e6\n\63\3\63\3\63"+
		"\3\63\5\63\u02eb\n\63\7\63\u02ed\n\63\f\63\16\63\u02f0\13\63\3\64\3\64"+
		"\3\65\3\65\3\65\3\65\7\65\u02f8\n\65\f\65\16\65\u02fb\13\65\3\65\3\65"+
		"\3\66\3\66\3\66\3\66\5\66\u0303\n\66\5\66\u0305\n\66\3\67\3\67\3\67\7"+
		"\67\u030a\n\67\f\67\16\67\u030d\13\67\38\38\58\u0311\n8\38\38\39\39\3"+
		"9\79\u0318\n9\f9\169\u031b\139\39\39\59\u031f\n9\39\59\u0322\n9\3:\7:"+
		"\u0325\n:\f:\16:\u0328\13:\3:\3:\3:\3;\7;\u032e\n;\f;\16;\u0331\13;\3"+
		";\3;\3;\3;\3<\3<\3=\3=\3>\3>\3>\7>\u033e\n>\f>\16>\u0341\13>\3?\3?\3@"+
		"\3@\3@\3@\3@\5@\u034a\n@\3@\5@\u034d\n@\3A\3A\3B\3B\3B\7B\u0354\nB\fB"+
		"\16B\u0357\13B\3C\3C\3C\3C\3D\3D\3D\5D\u0360\nD\3E\3E\3E\3E\7E\u0366\n"+
		"E\fE\16E\u0369\13E\5E\u036b\nE\3E\5E\u036e\nE\3E\3E\3F\3F\3F\3F\3F\3G"+
		"\3G\7G\u0379\nG\fG\16G\u037c\13G\3G\3G\3H\7H\u0381\nH\fH\16H\u0384\13"+
		"H\3H\3H\5H\u0388\nH\3I\3I\3I\3I\3I\3I\5I\u0390\nI\3I\3I\5I\u0394\nI\3"+
		"I\3I\5I\u0398\nI\3I\3I\5I\u039c\nI\5I\u039e\nI\3J\3J\5J\u03a2\nJ\3K\3"+
		"K\3K\3K\5K\u03a8\nK\3L\3L\3M\3M\3M\3N\3N\7N\u03b1\nN\fN\16N\u03b4\13N"+
		"\3N\3N\3O\3O\3O\5O\u03bb\nO\3P\3P\3P\3Q\7Q\u03c1\nQ\fQ\16Q\u03c4\13Q\3"+
		"Q\3Q\3Q\3R\3R\3R\3R\3R\5R\u03ce\nR\3R\3R\3R\3R\3R\3R\3R\5R\u03d7\nR\3"+
		"R\3R\3R\3R\3R\3R\3R\3R\3R\3R\3R\3R\3R\3R\3R\3R\3R\3R\3R\6R\u03ec\nR\r"+
		"R\16R\u03ed\3R\5R\u03f1\nR\3R\5R\u03f4\nR\3R\3R\3R\3R\7R\u03fa\nR\fR\16"+
		"R\u03fd\13R\3R\5R\u0400\nR\3R\3R\3R\3R\7R\u0406\nR\fR\16R\u0409\13R\3"+
		"R\7R\u040c\nR\fR\16R\u040f\13R\3R\3R\3R\3R\3R\3R\3R\3R\5R\u0419\nR\3R"+
		"\3R\3R\3R\3R\3R\3R\5R\u0422\nR\3R\3R\3R\5R\u0427\nR\3R\3R\3R\3R\3R\3R"+
		"\3R\3R\5R\u0431\nR\3S\3S\3S\7S\u0436\nS\fS\16S\u0439\13S\3S\3S\3S\3S\3"+
		"S\3T\3T\3T\7T\u0443\nT\fT\16T\u0446\13T\3U\3U\3U\3V\3V\3V\5V\u044e\nV"+
		"\3V\3V\3W\3W\3W\7W\u0455\nW\fW\16W\u0458\13W\3X\7X\u045b\nX\fX\16X\u045e"+
		"\13X\3X\3X\3X\3X\3X\3Y\6Y\u0466\nY\rY\16Y\u0467\3Y\6Y\u046b\nY\rY\16Y"+
		"\u046c\3Z\3Z\3Z\3Z\3Z\3Z\3Z\3Z\3Z\3Z\5Z\u0479\nZ\3[\3[\5[\u047d\n[\3["+
		"\3[\5[\u0481\n[\3[\3[\5[\u0485\n[\5[\u0487\n[\3\\\3\\\5\\\u048b\n\\\3"+
		"]\7]\u048e\n]\f]\16]\u0491\13]\3]\3]\3]\3]\3]\3^\3^\3_\3_\3_\3_\3`\3`"+
		"\3`\7`\u04a1\n`\f`\16`\u04a4\13`\3a\3a\3b\3b\3c\3c\3c\3c\3c\3c\3c\3c\3"+
		"c\3c\3c\3c\3c\5c\u04b7\nc\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\5"+
		"c\u04c7\nc\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3"+
		"c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\5c\u04f2"+
		"\nc\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\5c\u0504\nc\3c\3c"+
		"\3c\3c\3c\3c\7c\u050c\nc\fc\16c\u050f\13c\3d\3d\3d\3d\3d\3d\3d\3d\3d\3"+
		"d\3d\3d\3d\3d\3d\3d\3d\3d\3d\5d\u0524\nd\5d\u0526\nd\3e\3e\3e\3e\3e\3"+
		"e\3e\5e\u052f\ne\5e\u0531\ne\3f\3f\5f\u0535\nf\3f\3f\3f\5f\u053a\nf\7"+
		"f\u053c\nf\ff\16f\u053f\13f\3f\5f\u0542\nf\3g\3g\5g\u0546\ng\3g\3g\3h"+
		"\3h\3h\3h\7h\u054e\nh\fh\16h\u0551\13h\3h\3h\3h\3h\3h\3h\3h\7h\u055a\n"+
		"h\fh\16h\u055d\13h\3h\3h\7h\u0561\nh\fh\16h\u0564\13h\5h\u0566\nh\3i\3"+
		"i\5i\u056a\ni\3j\3j\3j\3k\3k\3k\3k\3l\3l\3l\5l\u0576\nl\3m\3m\3m\5m\u057b"+
		"\nm\3n\3n\3n\3n\5n\u0581\nn\5n\u0583\nn\3o\3o\3o\3o\5o\u0589\no\3p\3p"+
		"\5p\u058d\np\3p\3p\3p\2\5\16\20\u00c4q\2\4\6\b\n\f\16\20\22\24\26\30\32"+
		"\34\36 \"$&(*,.\60\62\64\668:<>@BDFHJLNPRTVXZ\\^`bdfhjlnprtvxz|~\u0080"+
		"\u0082\u0084\u0086\u0088\u008a\u008c\u008e\u0090\u0092\u0094\u0096\u0098"+
		"\u009a\u009c\u009e\u00a0\u00a2\u00a4\u00a6\u00a8\u00aa\u00ac\u00ae\u00b0"+
		"\u00b2\u00b4\u00b6\u00b8\u00ba\u00bc\u00be\u00c0\u00c2\u00c4\u00c6\u00c8"+
		"\u00ca\u00cc\u00ce\u00d0\u00d2\u00d4\u00d6\u00d8\u00da\u00dc\u00de\2\20"+
		"\6\2\3\3DDVVgg\6\2!!--\61\61\64\64\6\2\4\4\25\25$&)*\n\2\6\6\b\b\13\13"+
		"\21\21\27\27\36\36  ((\4\2\24\24++\3\2\66;\3\2RU\3\2HI\4\2VW[[\3\2TU\4"+
		"\2FGMN\4\2LLOO\4\2EE\\f\3\2RS\u060f\2\u00e0\3\2\2\2\4\u00e4\3\2\2\2\6"+
		"\u00fd\3\2\2\2\b\u00ff\3\2\2\2\n\u010c\3\2\2\2\f\u0113\3\2\2\2\16\u0119"+
		"\3\2\2\2\20\u012b\3\2\2\2\22\u0139\3\2\2\2\24\u015a\3\2\2\2\26\u015d\3"+
		"\2\2\2\30\u0170\3\2\2\2\32\u0177\3\2\2\2\34\u019f\3\2\2\2\36\u01a3\3\2"+
		"\2\2 \u01a7\3\2\2\2\"\u01ab\3\2\2\2$\u01ad\3\2\2\2&\u01bc\3\2\2\2(\u01c7"+
		"\3\2\2\2*\u01cc\3\2\2\2,\u01d4\3\2\2\2.\u01e6\3\2\2\2\60\u01f1\3\2\2\2"+
		"\62\u01fb\3\2\2\2\64\u0202\3\2\2\2\66\u020d\3\2\2\28\u0215\3\2\2\2:\u021e"+
		"\3\2\2\2<\u0233\3\2\2\2>\u023e\3\2\2\2@\u0242\3\2\2\2B\u0255\3\2\2\2D"+
		"\u0258\3\2\2\2F\u0260\3\2\2\2H\u0263\3\2\2\2J\u026f\3\2\2\2L\u0278\3\2"+
		"\2\2N\u027a\3\2\2\2P\u0285\3\2\2\2R\u0292\3\2\2\2T\u02a3\3\2\2\2V\u02a6"+
		"\3\2\2\2X\u02ae\3\2\2\2Z\u02b3\3\2\2\2\\\u02bd\3\2\2\2^\u02bf\3\2\2\2"+
		"`\u02cf\3\2\2\2b\u02e1\3\2\2\2d\u02e3\3\2\2\2f\u02f1\3\2\2\2h\u02f3\3"+
		"\2\2\2j\u0304\3\2\2\2l\u0306\3\2\2\2n\u030e\3\2\2\2p\u0321\3\2\2\2r\u0326"+
		"\3\2\2\2t\u032f\3\2\2\2v\u0336\3\2\2\2x\u0338\3\2\2\2z\u033a\3\2\2\2|"+
		"\u0342\3\2\2\2~\u0344\3\2\2\2\u0080\u034e\3\2\2\2\u0082\u0350\3\2\2\2"+
		"\u0084\u0358\3\2\2\2\u0086\u035f\3\2\2\2\u0088\u0361\3\2\2\2\u008a\u0371"+
		"\3\2\2\2\u008c\u0376\3\2\2\2\u008e\u0387\3\2\2\2\u0090\u039d\3\2\2\2\u0092"+
		"\u03a1\3\2\2\2\u0094\u03a3\3\2\2\2\u0096\u03a9\3\2\2\2\u0098\u03ab\3\2"+
		"\2\2\u009a\u03ae\3\2\2\2\u009c\u03ba\3\2\2\2\u009e\u03bc\3\2\2\2\u00a0"+
		"\u03c2\3\2\2\2\u00a2\u0430\3\2\2\2\u00a4\u0432\3\2\2\2\u00a6\u043f\3\2"+
		"\2\2\u00a8\u0447\3\2\2\2\u00aa\u044a\3\2\2\2\u00ac\u0451\3\2\2\2\u00ae"+
		"\u045c\3\2\2\2\u00b0\u0465\3\2\2\2\u00b2\u0478\3\2\2\2\u00b4\u0486\3\2"+
		"\2\2\u00b6\u048a\3\2\2\2\u00b8\u048f\3\2\2\2\u00ba\u0497\3\2\2\2\u00bc"+
		"\u0499\3\2\2\2\u00be\u049d\3\2\2\2\u00c0\u04a5\3\2\2\2\u00c2\u04a7\3\2"+
		"\2\2\u00c4\u04b6\3\2\2\2\u00c6\u0525\3\2\2\2\u00c8\u0530\3\2\2\2\u00ca"+
		"\u0541\3\2\2\2\u00cc\u0543\3\2\2\2\u00ce\u0549\3\2\2\2\u00d0\u0567\3\2"+
		"\2\2\u00d2\u056b\3\2\2\2\u00d4\u056e\3\2\2\2\u00d6\u0575\3\2\2\2\u00d8"+
		"\u057a\3\2\2\2\u00da\u0582\3\2\2\2\u00dc\u0588\3\2\2\2\u00de\u058a\3\2"+
		"\2\2\u00e0\u00e1\5\16\b\2\u00e1\u00e2\5\24\13\2\u00e2\u00e3\5\4\3\2\u00e3"+
		"\3\3\2\2\2\u00e4\u00e6\7<\2\2\u00e5\u00e7\5\6\4\2\u00e6\u00e5\3\2\2\2"+
		"\u00e6\u00e7\3\2\2\2\u00e7\u00e8\3\2\2\2\u00e8\u00e9\7=\2\2\u00e9\5\3"+
		"\2\2\2\u00ea\u00ef\5\b\5\2\u00eb\u00ec\7C\2\2\u00ec\u00ee\5\n\6\2\u00ed"+
		"\u00eb\3\2\2\2\u00ee\u00f1\3\2\2\2\u00ef\u00ed\3\2\2\2\u00ef\u00f0\3\2"+
		"\2\2\u00f0\u00fe\3\2\2\2\u00f1\u00ef\3\2\2\2\u00f2\u00f7\5\f\7\2\u00f3"+
		"\u00f4\7C\2\2\u00f4\u00f6\5\6\4\2\u00f5\u00f3\3\2\2\2\u00f6\u00f9\3\2"+
		"\2\2\u00f7\u00f5\3\2\2\2\u00f7\u00f8\3\2\2\2\u00f8\u00fe\3\2\2\2\u00f9"+
		"\u00f7\3\2\2\2\u00fa\u00fb\5\20\t\2\u00fb\u00fc\7i\2\2\u00fc\u00fe\3\2"+
		"\2\2\u00fd\u00ea\3\2\2\2\u00fd\u00f2\3\2\2\2\u00fd\u00fa\3\2\2\2\u00fe"+
		"\7\3\2\2\2\u00ff\u0100\7\3\2\2\u0100\t\3\2\2\2\u0101\u0106\5\f\7\2\u0102"+
		"\u0103\7C\2\2\u0103\u0105\5\n\6\2\u0104\u0102\3\2\2\2\u0105\u0108\3\2"+
		"\2\2\u0106\u0104\3\2\2\2\u0106\u0107\3\2\2\2\u0107\u010d\3\2\2\2\u0108"+
		"\u0106\3\2\2\2\u0109\u010a\5\20\t\2\u010a\u010b\7i\2\2\u010b\u010d\3\2"+
		"\2\2\u010c\u0101\3\2\2\2\u010c\u0109\3\2\2\2\u010d\13\3\2\2\2\u010e\u010f"+
		"\7<\2\2\u010f\u0110\5\20\t\2\u0110\u0111\7=\2\2\u0111\u0114\3\2\2\2\u0112"+
		"\u0114\5\20\t\2\u0113\u010e\3\2\2\2\u0113\u0112\3\2\2\2\u0114\r\3\2\2"+
		"\2\u0115\u0116\b\b\1\2\u0116\u0117\7H\2\2\u0117\u011a\5\16\b\5\u0118\u011a"+
		"\5\22\n\2\u0119\u0115\3\2\2\2\u0119\u0118\3\2\2\2\u011a\u0123\3\2\2\2"+
		"\u011b\u011c\f\4\2\2\u011c\u011d\7P\2\2\u011d\u0122\5\16\b\5\u011e\u011f"+
		"\f\3\2\2\u011f\u0120\7Q\2\2\u0120\u0122\5\16\b\4\u0121\u011b\3\2\2\2\u0121"+
		"\u011e\3\2\2\2\u0122\u0125\3\2\2\2\u0123\u0121\3\2\2\2\u0123\u0124\3\2"+
		"\2\2\u0124\17\3\2\2\2\u0125\u0123\3\2\2\2\u0126\u0127\b\t\1\2\u0127\u0128"+
		"\7H\2\2\u0128\u012c\5\20\t\5\u0129\u012c\5\22\n\2\u012a\u012c\5f\64\2"+
		"\u012b\u0126\3\2\2\2\u012b\u0129\3\2\2\2\u012b\u012a\3\2\2\2\u012c\u0135"+
		"\3\2\2\2\u012d\u012e\f\4\2\2\u012e\u012f\7P\2\2\u012f\u0134\5\20\t\5\u0130"+
		"\u0131\f\3\2\2\u0131\u0132\7Q\2\2\u0132\u0134\5\20\t\4\u0133\u012d\3\2"+
		"\2\2\u0133\u0130\3\2\2\2\u0134\u0137\3\2\2\2\u0135\u0133\3\2\2\2\u0135"+
		"\u0136\3\2\2\2\u0136\21\3\2\2\2\u0137\u0135\3\2\2\2\u0138\u013a\t\2\2"+
		"\2\u0139\u0138\3\2\2\2\u013a\u013b\3\2\2\2\u013b\u0139\3\2\2\2\u013b\u013c"+
		"\3\2\2\2\u013c\u0141\3\2\2\2\u013d\u013e\7@\2\2\u013e\u0140\7A\2\2\u013f"+
		"\u013d\3\2\2\2\u0140\u0143\3\2\2\2\u0141\u013f\3\2\2\2\u0141\u0142\3\2"+
		"\2\2\u0142\23\3\2\2\2\u0143\u0141\3\2\2\2\u0144\u0149\7g\2\2\u0145\u0146"+
		"\7V\2\2\u0146\u0148\7g\2\2\u0147\u0145\3\2\2\2\u0148\u014b\3\2\2\2\u0149"+
		"\u0147\3\2\2\2\u0149\u014a\3\2\2\2\u014a\u014d\3\2\2\2\u014b\u0149\3\2"+
		"\2\2\u014c\u014e\7V\2\2\u014d\u014c\3\2\2\2\u014d\u014e\3\2\2\2\u014e"+
		"\u015b\3\2\2\2\u014f\u0154\7V\2\2\u0150\u0151\7g\2\2\u0151\u0153\7V\2"+
		"\2\u0152\u0150\3\2\2\2\u0153\u0156\3\2\2\2\u0154\u0152\3\2\2\2\u0154\u0155"+
		"\3\2\2\2\u0155\u0158\3\2\2\2\u0156\u0154\3\2\2\2\u0157\u0159\7g\2\2\u0158"+
		"\u0157\3\2\2\2\u0158\u0159\3\2\2\2\u0159\u015b\3\2\2\2\u015a\u0144\3\2"+
		"\2\2\u015a\u014f\3\2\2\2\u015b\25\3\2\2\2\u015c\u015e\5\30\r\2\u015d\u015c"+
		"\3\2\2\2\u015d\u015e\3\2\2\2\u015e\u0162\3\2\2\2\u015f\u0161\5\32\16\2"+
		"\u0160\u015f\3\2\2\2\u0161\u0164\3\2\2\2\u0162\u0160\3\2\2\2\u0162\u0163"+
		"\3\2\2\2\u0163\u0168\3\2\2\2\u0164\u0162\3\2\2\2\u0165\u0167\5\34\17\2"+
		"\u0166\u0165\3\2\2\2\u0167\u016a\3\2\2\2\u0168\u0166\3\2\2\2\u0168\u0169"+
		"\3\2\2\2\u0169\u016b\3\2\2\2\u016a\u0168\3\2\2\2\u016b\u016c\7\2\2\3\u016c"+
		"\27\3\2\2\2\u016d\u016f\5~@\2\u016e\u016d\3\2\2\2\u016f\u0172\3\2\2\2"+
		"\u0170\u016e\3\2\2\2\u0170\u0171\3\2\2\2\u0171\u0173\3\2\2\2\u0172\u0170"+
		"\3\2\2\2\u0173\u0174\7#\2\2\u0174\u0175\5z>\2\u0175\u0176\7B\2\2\u0176"+
		"\31\3\2\2\2\u0177\u0179\7\34\2\2\u0178\u017a\7)\2\2\u0179\u0178\3\2\2"+
		"\2\u0179\u017a\3\2\2\2\u017a\u017b\3\2\2\2\u017b\u017e\5z>\2\u017c\u017d"+
		"\7D\2\2\u017d\u017f\7V\2\2\u017e\u017c\3\2\2\2\u017e\u017f\3\2\2\2\u017f"+
		"\u0180\3\2\2\2\u0180\u0181\7B\2\2\u0181\33\3\2\2\2\u0182\u0184\5 \21\2"+
		"\u0183\u0182\3\2\2\2\u0184\u0187\3\2\2\2\u0185\u0183\3\2\2\2\u0185\u0186"+
		"\3\2\2\2\u0186\u0188\3\2\2\2\u0187\u0185\3\2\2\2\u0188\u01a0\5$\23\2\u0189"+
		"\u018b\5 \21\2\u018a\u0189\3\2\2\2\u018b\u018e\3\2\2\2\u018c\u018a\3\2"+
		"\2\2\u018c\u018d\3\2\2\2\u018d\u018f\3\2\2\2\u018e\u018c\3\2\2\2\u018f"+
		"\u01a0\5,\27\2\u0190\u0192\5 \21\2\u0191\u0190\3\2\2\2\u0192\u0195\3\2"+
		"\2\2\u0193\u0191\3\2\2\2\u0193\u0194\3\2\2\2\u0194\u0196\3\2\2\2\u0195"+
		"\u0193\3\2\2\2\u0196\u01a0\5\64\33\2\u0197\u0199\5 \21\2\u0198\u0197\3"+
		"\2\2\2\u0199\u019c\3\2\2\2\u019a\u0198\3\2\2\2\u019a\u019b\3\2\2\2\u019b"+
		"\u019d\3\2\2\2\u019c\u019a\3\2\2\2\u019d\u01a0\5\u008aF\2\u019e\u01a0"+
		"\7B\2\2\u019f\u0185\3\2\2\2\u019f\u018c\3\2\2\2\u019f\u0193\3\2\2\2\u019f"+
		"\u019a\3\2\2\2\u019f\u019e\3\2\2\2\u01a0\35\3\2\2\2\u01a1\u01a4\5 \21"+
		"\2\u01a2\u01a4\t\3\2\2\u01a3\u01a1\3\2\2\2\u01a3\u01a2\3\2\2\2\u01a4\37"+
		"\3\2\2\2\u01a5\u01a8\5~@\2\u01a6\u01a8\t\4\2\2\u01a7\u01a5\3\2\2\2\u01a7"+
		"\u01a6\3\2\2\2\u01a8!\3\2\2\2\u01a9\u01ac\7\25\2\2\u01aa\u01ac\5~@\2\u01ab"+
		"\u01a9\3\2\2\2\u01ab\u01aa\3\2\2\2\u01ac#\3\2\2\2\u01ad\u01ae\7\f\2\2"+
		"\u01ae\u01b0\7g\2\2\u01af\u01b1\5&\24\2\u01b0\u01af\3\2\2\2\u01b0\u01b1"+
		"\3\2\2\2\u01b1\u01b4\3\2\2\2\u01b2\u01b3\7\24\2\2\u01b3\u01b5\5b\62\2"+
		"\u01b4\u01b2\3\2\2\2\u01b4\u01b5\3\2\2\2\u01b5\u01b8\3\2\2\2\u01b6\u01b7"+
		"\7\33\2\2\u01b7\u01b9\5\66\34\2\u01b8\u01b6\3\2\2\2\u01b8\u01b9\3\2\2"+
		"\2\u01b9\u01ba\3\2\2\2\u01ba\u01bb\58\35\2\u01bb%\3\2\2\2\u01bc\u01bd"+
		"\7G\2\2\u01bd\u01c2\5(\25\2\u01be\u01bf\7C\2\2\u01bf\u01c1\5(\25\2\u01c0"+
		"\u01be\3\2\2\2\u01c1\u01c4\3\2\2\2\u01c2\u01c0\3\2\2\2\u01c2\u01c3\3\2"+
		"\2\2\u01c3\u01c5\3\2\2\2\u01c4\u01c2\3\2\2\2\u01c5\u01c6\7F\2\2\u01c6"+
		"\'\3\2\2\2\u01c7\u01ca\7g\2\2\u01c8\u01c9\7\24\2\2\u01c9\u01cb\5*\26\2"+
		"\u01ca\u01c8\3\2\2\2\u01ca\u01cb\3\2\2\2\u01cb)\3\2\2\2\u01cc\u01d1\5"+
		"b\62\2\u01cd\u01ce\7X\2\2\u01ce\u01d0\5b\62\2\u01cf\u01cd\3\2\2\2\u01d0"+
		"\u01d3\3\2\2\2\u01d1\u01cf\3\2\2\2\u01d1\u01d2\3\2\2\2\u01d2+\3\2\2\2"+
		"\u01d3\u01d1\3\2\2\2\u01d4\u01d5\7\23\2\2\u01d5\u01d8\7g\2\2\u01d6\u01d7"+
		"\7\33\2\2\u01d7\u01d9\5\66\34\2\u01d8\u01d6\3\2\2\2\u01d8\u01d9\3\2\2"+
		"\2\u01d9\u01da\3\2\2\2\u01da\u01dc\7>\2\2\u01db\u01dd\5.\30\2\u01dc\u01db"+
		"\3\2\2\2\u01dc\u01dd\3\2\2\2\u01dd\u01df\3\2\2\2\u01de\u01e0\7C\2\2\u01df"+
		"\u01de\3\2\2\2\u01df\u01e0\3\2\2\2\u01e0\u01e2\3\2\2\2\u01e1\u01e3\5\62"+
		"\32\2\u01e2\u01e1\3\2\2\2\u01e2\u01e3\3\2\2\2\u01e3\u01e4\3\2\2\2\u01e4"+
		"\u01e5\7?\2\2\u01e5-\3\2\2\2\u01e6\u01eb\5\60\31\2\u01e7\u01e8\7C\2\2"+
		"\u01e8\u01ea\5\60\31\2\u01e9\u01e7\3\2\2\2\u01ea\u01ed\3\2\2\2\u01eb\u01e9"+
		"\3\2\2\2\u01eb\u01ec\3\2\2\2\u01ec/\3\2\2\2\u01ed\u01eb\3\2\2\2\u01ee"+
		"\u01f0\5~@\2\u01ef\u01ee\3\2\2\2\u01f0\u01f3\3\2\2\2\u01f1\u01ef\3\2\2"+
		"\2\u01f1\u01f2\3\2\2\2\u01f2\u01f4\3\2\2\2\u01f3\u01f1\3\2\2\2\u01f4\u01f6"+
		"\7g\2\2\u01f5\u01f7\5\u00dep\2\u01f6\u01f5\3\2\2\2\u01f6\u01f7\3\2\2\2"+
		"\u01f7\u01f9\3\2\2\2\u01f8\u01fa\58\35\2\u01f9\u01f8\3\2\2\2\u01f9\u01fa"+
		"\3\2\2\2\u01fa\61\3\2\2\2\u01fb\u01ff\7B\2\2\u01fc\u01fe\5<\37\2\u01fd"+
		"\u01fc\3\2\2\2\u01fe\u0201\3\2\2\2\u01ff\u01fd\3\2\2\2\u01ff\u0200\3\2"+
		"\2\2\u0200\63\3\2\2\2\u0201\u01ff\3\2\2\2\u0202\u0203\7\37\2\2\u0203\u0205"+
		"\7g\2\2\u0204\u0206\5&\24\2\u0205\u0204\3\2\2\2\u0205\u0206\3\2\2\2\u0206"+
		"\u0209\3\2\2\2\u0207\u0208\7\24\2\2\u0208\u020a\5\66\34\2\u0209\u0207"+
		"\3\2\2\2\u0209\u020a\3\2\2\2\u020a\u020b\3\2\2\2\u020b\u020c\5:\36\2\u020c"+
		"\65\3\2\2\2\u020d\u0212\5b\62\2\u020e\u020f\7C\2\2\u020f\u0211\5b\62\2"+
		"\u0210\u020e\3\2\2\2\u0211\u0214\3\2\2\2\u0212\u0210\3\2\2\2\u0212\u0213"+
		"\3\2\2\2\u0213\67\3\2\2\2\u0214\u0212\3\2\2\2\u0215\u0219\7>\2\2\u0216"+
		"\u0218\5<\37\2\u0217\u0216\3\2\2\2\u0218\u021b\3\2\2\2\u0219\u0217\3\2"+
		"\2\2\u0219\u021a\3\2\2\2\u021a\u021c\3\2\2\2\u021b\u0219\3\2\2\2\u021c"+
		"\u021d\7?\2\2\u021d9\3\2\2\2\u021e\u0222\7>\2\2\u021f\u0221\5J&\2\u0220"+
		"\u021f\3\2\2\2\u0221\u0224\3\2\2\2\u0222\u0220\3\2\2\2\u0222\u0223\3\2"+
		"\2\2\u0223\u0225\3\2\2\2\u0224\u0222\3\2\2\2\u0225\u0226\7?\2\2\u0226"+
		";\3\2\2\2\u0227\u0234\7B\2\2\u0228\u022a\7)\2\2\u0229\u0228\3\2\2\2\u0229"+
		"\u022a\3\2\2\2\u022a\u022b\3\2\2\2\u022b\u0234\5\u009aN\2\u022c\u022e"+
		"\5\36\20\2\u022d\u022c\3\2\2\2\u022e\u0231\3\2\2\2\u022f\u022d\3\2\2\2"+
		"\u022f\u0230\3\2\2\2\u0230\u0232\3\2\2\2\u0231\u022f\3\2\2\2\u0232\u0234"+
		"\5> \2\u0233\u0227\3\2\2\2\u0233\u0229\3\2\2\2\u0233\u022f\3\2\2\2\u0234"+
		"=\3\2\2\2\u0235\u023f\5@!\2\u0236\u023f\5B\"\2\u0237\u023f\5H%\2\u0238"+
		"\u023f\5D#\2\u0239\u023f\5F$\2\u023a\u023f\5\64\33\2\u023b\u023f\5\u008a"+
		"F\2\u023c\u023f\5$\23\2\u023d\u023f\5,\27\2\u023e\u0235\3\2\2\2\u023e"+
		"\u0236\3\2\2\2\u023e\u0237\3\2\2\2\u023e\u0238\3\2\2\2\u023e\u0239\3\2"+
		"\2\2\u023e\u023a\3\2\2\2\u023e\u023b\3\2\2\2\u023e\u023c\3\2\2\2\u023e"+
		"\u023d\3\2\2\2\u023f?\3\2\2\2\u0240\u0243\5b\62\2\u0241\u0243\7\63\2\2"+
		"\u0242\u0240\3\2\2\2\u0242\u0241\3\2\2\2\u0243\u0244\3\2\2\2\u0244\u0245"+
		"\7g\2\2\u0245\u024a\5n8\2\u0246\u0247\7@\2\2\u0247\u0249\7A\2\2\u0248"+
		"\u0246\3\2\2\2\u0249\u024c\3\2\2\2\u024a\u0248\3\2\2\2\u024a\u024b\3\2"+
		"\2\2\u024b\u024f\3\2\2\2\u024c\u024a\3\2\2\2\u024d\u024e\7\60\2\2\u024e"+
		"\u0250\5l\67\2\u024f\u024d\3\2\2\2\u024f\u0250\3\2\2\2\u0250\u0253\3\2"+
		"\2\2\u0251\u0254\5v<\2\u0252\u0254\7B\2\2\u0253\u0251\3\2\2\2\u0253\u0252"+
		"\3\2\2\2\u0254A\3\2\2\2\u0255\u0256\5&\24\2\u0256\u0257\5@!\2\u0257C\3"+
		"\2\2\2\u0258\u0259\7g\2\2\u0259\u025c\5n8\2\u025a\u025b\7\60\2\2\u025b"+
		"\u025d\5l\67\2\u025c\u025a\3\2\2\2\u025c\u025d\3\2\2\2\u025d\u025e\3\2"+
		"\2\2\u025e\u025f\5x=\2\u025fE\3\2\2\2\u0260\u0261\5&\24\2\u0261\u0262"+
		"\5D#\2\u0262G\3\2\2\2\u0263\u0264\5b\62\2\u0264\u0265\5V,\2\u0265\u0266"+
		"\7B\2\2\u0266I\3\2\2\2\u0267\u0269\5\36\20\2\u0268\u0267\3\2\2\2\u0269"+
		"\u026c\3\2\2\2\u026a\u0268\3\2\2\2\u026a\u026b\3\2\2\2\u026b\u026d\3\2"+
		"\2\2\u026c\u026a\3\2\2\2\u026d\u0270\5L\'\2\u026e\u0270\7B\2\2\u026f\u026a"+
		"\3\2\2\2\u026f\u026e\3\2\2\2\u0270K\3\2\2\2\u0271\u0279\5N(\2\u0272\u0279"+
		"\5R*\2\u0273\u0279\5T+\2\u0274\u0279\5\64\33\2\u0275\u0279\5\u008aF\2"+
		"\u0276\u0279\5$\23\2\u0277\u0279\5,\27\2\u0278\u0271\3\2\2\2\u0278\u0272"+
		"\3\2\2\2\u0278\u0273\3\2\2\2\u0278\u0274\3\2\2\2\u0278\u0275\3\2\2\2\u0278"+
		"\u0276\3\2\2\2\u0278\u0277\3\2\2\2\u0279M\3\2\2\2\u027a\u027b\5b\62\2"+
		"\u027b\u0280\5P)\2\u027c\u027d\7C\2\2\u027d\u027f\5P)\2\u027e\u027c\3"+
		"\2\2\2\u027f\u0282\3\2\2\2\u0280\u027e\3\2\2\2\u0280\u0281\3\2\2\2\u0281"+
		"\u0283\3\2\2\2\u0282\u0280\3\2\2\2\u0283\u0284\7B\2\2\u0284O\3\2\2\2\u0285"+
		"\u028a\7g\2\2\u0286\u0287\7@\2\2\u0287\u0289\7A\2\2\u0288\u0286\3\2\2"+
		"\2\u0289\u028c\3\2\2\2\u028a\u0288\3\2\2\2\u028a\u028b\3\2\2\2\u028b\u028d"+
		"\3\2\2\2\u028c\u028a\3\2\2\2\u028d\u028e\7E\2\2\u028e\u028f\5\\/\2\u028f"+
		"Q\3\2\2\2\u0290\u0293\5b\62\2\u0291\u0293\7\63\2\2\u0292\u0290\3\2\2\2"+
		"\u0292\u0291\3\2\2\2\u0293\u0294\3\2\2\2\u0294\u0295\7g\2\2\u0295\u029a"+
		"\5n8\2\u0296\u0297\7@\2\2\u0297\u0299\7A\2\2\u0298\u0296\3\2\2\2\u0299"+
		"\u029c\3\2\2\2\u029a\u0298\3\2\2\2\u029a\u029b\3\2\2\2\u029b\u029f\3\2"+
		"\2\2\u029c\u029a\3\2\2\2\u029d\u029e\7\60\2\2\u029e\u02a0\5l\67\2\u029f"+
		"\u029d\3\2\2\2\u029f\u02a0\3\2\2\2\u02a0\u02a1\3\2\2\2\u02a1\u02a2\7B"+
		"\2\2\u02a2S\3\2\2\2\u02a3\u02a4\5&\24\2\u02a4\u02a5\5R*\2\u02a5U\3\2\2"+
		"\2\u02a6\u02ab\5X-\2\u02a7\u02a8\7C\2\2\u02a8\u02aa\5X-\2\u02a9\u02a7"+
		"\3\2\2\2\u02aa\u02ad\3\2\2\2\u02ab\u02a9\3\2\2\2\u02ab\u02ac\3\2\2\2\u02ac"+
		"W\3\2\2\2\u02ad\u02ab\3\2\2\2\u02ae\u02b1\5Z.\2\u02af\u02b0\7E\2\2\u02b0"+
		"\u02b2\5\\/\2\u02b1\u02af\3\2\2\2\u02b1\u02b2\3\2\2\2\u02b2Y\3\2\2\2\u02b3"+
		"\u02b8\7g\2\2\u02b4\u02b5\7@\2\2\u02b5\u02b7\7A\2\2\u02b6\u02b4\3\2\2"+
		"\2\u02b7\u02ba\3\2\2\2\u02b8\u02b6\3\2\2\2\u02b8\u02b9\3\2\2\2\u02b9["+
		"\3\2\2\2\u02ba\u02b8\3\2\2\2\u02bb\u02be\5^\60\2\u02bc\u02be\5\u00c4c"+
		"\2\u02bd\u02bb\3\2\2\2\u02bd\u02bc\3\2\2\2\u02be]\3\2\2\2\u02bf\u02cb"+
		"\7>\2\2\u02c0\u02c5\5\\/\2\u02c1\u02c2\7C\2\2\u02c2\u02c4\5\\/\2\u02c3"+
		"\u02c1\3\2\2\2\u02c4\u02c7\3\2\2\2\u02c5\u02c3\3\2\2\2\u02c5\u02c6\3\2"+
		"\2\2\u02c6\u02c9\3\2\2\2\u02c7\u02c5\3\2\2\2\u02c8\u02ca\7C\2\2\u02c9"+
		"\u02c8\3\2\2\2\u02c9\u02ca\3\2\2\2\u02ca\u02cc\3\2\2\2\u02cb\u02c0\3\2"+
		"\2\2\u02cb\u02cc\3\2\2\2\u02cc\u02cd\3\2\2\2\u02cd\u02ce\7?\2\2\u02ce"+
		"_\3\2\2\2\u02cf\u02d0\7g\2\2\u02d0a\3\2\2\2\u02d1\u02d6\5d\63\2\u02d2"+
		"\u02d3\7@\2\2\u02d3\u02d5\7A\2\2\u02d4\u02d2\3\2\2\2\u02d5\u02d8\3\2\2"+
		"\2\u02d6\u02d4\3\2\2\2\u02d6\u02d7\3\2\2\2\u02d7\u02e2\3\2\2\2\u02d8\u02d6"+
		"\3\2\2\2\u02d9\u02de\5f\64\2\u02da\u02db\7@\2\2\u02db\u02dd\7A\2\2\u02dc"+
		"\u02da\3\2\2\2\u02dd\u02e0\3\2\2\2\u02de\u02dc\3\2\2\2\u02de\u02df\3\2"+
		"\2\2\u02df\u02e2\3\2\2\2\u02e0\u02de\3\2\2\2\u02e1\u02d1\3\2\2\2\u02e1"+
		"\u02d9\3\2\2\2\u02e2c\3\2\2\2\u02e3\u02e5\7g\2\2\u02e4\u02e6\5h\65\2\u02e5"+
		"\u02e4\3\2\2\2\u02e5\u02e6\3\2\2\2\u02e6\u02ee\3\2\2\2\u02e7\u02e8\7D"+
		"\2\2\u02e8\u02ea\7g\2\2\u02e9\u02eb\5h\65\2\u02ea\u02e9\3\2\2\2\u02ea"+
		"\u02eb\3\2\2\2\u02eb\u02ed\3\2\2\2\u02ec\u02e7\3\2\2\2\u02ed\u02f0\3\2"+
		"\2\2\u02ee\u02ec\3\2\2\2\u02ee\u02ef\3\2\2\2\u02efe\3\2\2\2\u02f0\u02ee"+
		"\3\2\2\2\u02f1\u02f2\t\5\2\2\u02f2g\3\2\2\2\u02f3\u02f4\7G\2\2\u02f4\u02f9"+
		"\5j\66\2\u02f5\u02f6\7C\2\2\u02f6\u02f8\5j\66\2\u02f7\u02f5\3\2\2\2\u02f8"+
		"\u02fb\3\2\2\2\u02f9\u02f7\3\2\2\2\u02f9\u02fa\3\2\2\2\u02fa\u02fc\3\2"+
		"\2\2\u02fb\u02f9\3\2\2\2\u02fc\u02fd\7F\2\2\u02fdi\3\2\2\2\u02fe\u0305"+
		"\5b\62\2\u02ff\u0302\7J\2\2\u0300\u0301\t\6\2\2\u0301\u0303\5b\62\2\u0302"+
		"\u0300\3\2\2\2\u0302\u0303\3\2\2\2\u0303\u0305\3\2\2\2\u0304\u02fe\3\2"+
		"\2\2\u0304\u02ff\3\2\2\2\u0305k\3\2\2\2\u0306\u030b\5z>\2\u0307\u0308"+
		"\7C\2\2\u0308\u030a\5z>\2\u0309\u0307\3\2\2\2\u030a\u030d\3\2\2\2\u030b"+
		"\u0309\3\2\2\2\u030b\u030c\3\2\2\2\u030cm\3\2\2\2\u030d\u030b\3\2\2\2"+
		"\u030e\u0310\7<\2\2\u030f\u0311\5p9\2\u0310\u030f\3\2\2\2\u0310\u0311"+
		"\3\2\2\2\u0311\u0312\3\2\2\2\u0312\u0313\7=\2\2\u0313o\3\2\2\2\u0314\u0319"+
		"\5r:\2\u0315\u0316\7C\2\2\u0316\u0318\5r:\2\u0317\u0315\3\2\2\2\u0318"+
		"\u031b\3\2\2\2\u0319\u0317\3\2\2\2\u0319\u031a\3\2\2\2\u031a\u031e\3\2"+
		"\2\2\u031b\u0319\3\2\2\2\u031c\u031d\7C\2\2\u031d\u031f\5t;\2\u031e\u031c"+
		"\3\2\2\2\u031e\u031f\3\2\2\2\u031f\u0322\3\2\2\2\u0320\u0322\5t;\2\u0321"+
		"\u0314\3\2\2\2\u0321\u0320\3\2\2\2\u0322q\3\2\2\2\u0323\u0325\5\"\22\2"+
		"\u0324\u0323\3\2\2\2\u0325\u0328\3\2\2\2\u0326\u0324\3\2\2\2\u0326\u0327"+
		"\3\2\2\2\u0327\u0329\3\2\2\2\u0328\u0326\3\2\2\2\u0329\u032a\5b\62\2\u032a"+
		"\u032b\5Z.\2\u032bs\3\2\2\2\u032c\u032e\5\"\22\2\u032d\u032c\3\2\2\2\u032e"+
		"\u0331\3\2\2\2\u032f\u032d\3\2\2\2\u032f\u0330\3\2\2\2\u0330\u0332\3\2"+
		"\2\2\u0331\u032f\3\2\2\2\u0332\u0333\5b\62\2\u0333\u0334\7i\2\2\u0334"+
		"\u0335\5Z.\2\u0335u\3\2\2\2\u0336\u0337\5\u009aN\2\u0337w\3\2\2\2\u0338"+
		"\u0339\5\u009aN\2\u0339y\3\2\2\2\u033a\u033f\7g\2\2\u033b\u033c\7D\2\2"+
		"\u033c\u033e\7g\2\2\u033d\u033b\3\2\2\2\u033e\u0341\3\2\2\2\u033f\u033d"+
		"\3\2\2\2\u033f\u0340\3\2\2\2\u0340{\3\2\2\2\u0341\u033f\3\2\2\2\u0342"+
		"\u0343\t\7\2\2\u0343}\3\2\2\2\u0344\u0345\7h\2\2\u0345\u034c\5\u0080A"+
		"\2\u0346\u0349\7<\2\2\u0347\u034a\5\u0082B\2\u0348\u034a\5\u0086D\2\u0349"+
		"\u0347\3\2\2\2\u0349\u0348\3\2\2\2\u0349\u034a\3\2\2\2\u034a\u034b\3\2"+
		"\2\2\u034b\u034d\7=\2\2\u034c\u0346\3\2\2\2\u034c\u034d\3\2\2\2\u034d"+
		"\177\3\2\2\2\u034e\u034f\5z>\2\u034f\u0081\3\2\2\2\u0350\u0355\5\u0084"+
		"C\2\u0351\u0352\7C\2\2\u0352\u0354\5\u0084C\2\u0353\u0351\3\2\2\2\u0354"+
		"\u0357\3\2\2\2\u0355\u0353\3\2\2\2\u0355\u0356\3\2\2\2\u0356\u0083\3\2"+
		"\2\2\u0357\u0355\3\2\2\2\u0358\u0359\7g\2\2\u0359\u035a\7E\2\2\u035a\u035b"+
		"\5\u0086D\2\u035b\u0085\3\2\2\2\u035c\u0360\5\u00c4c\2\u035d\u0360\5~"+
		"@\2\u035e\u0360\5\u0088E\2\u035f\u035c\3\2\2\2\u035f\u035d\3\2\2\2\u035f"+
		"\u035e\3\2\2\2\u0360\u0087\3\2\2\2\u0361\u036a\7>\2\2\u0362\u0367\5\u0086"+
		"D\2\u0363\u0364\7C\2\2\u0364\u0366\5\u0086D\2\u0365\u0363\3\2\2\2\u0366"+
		"\u0369\3\2\2\2\u0367\u0365\3\2\2\2\u0367\u0368\3\2\2\2\u0368\u036b\3\2"+
		"\2\2\u0369\u0367\3\2\2\2\u036a\u0362\3\2\2\2\u036a\u036b\3\2\2\2\u036b"+
		"\u036d\3\2\2\2\u036c\u036e\7C\2\2\u036d\u036c\3\2\2\2\u036d\u036e\3\2"+
		"\2\2\u036e\u036f\3\2\2\2\u036f\u0370\7?\2\2\u0370\u0089\3\2\2\2\u0371"+
		"\u0372\7h\2\2\u0372\u0373\7\37\2\2\u0373\u0374\7g\2\2\u0374\u0375\5\u008c"+
		"G\2\u0375\u008b\3\2\2\2\u0376\u037a\7>\2\2\u0377\u0379\5\u008eH\2\u0378"+
		"\u0377\3\2\2\2\u0379\u037c\3\2\2\2\u037a\u0378\3\2\2\2\u037a\u037b\3\2"+
		"\2\2\u037b\u037d\3\2\2\2\u037c\u037a\3\2\2\2\u037d\u037e\7?\2\2\u037e"+
		"\u008d\3\2\2\2\u037f\u0381\5\36\20\2\u0380\u037f\3\2\2\2\u0381\u0384\3"+
		"\2\2\2\u0382\u0380\3\2\2\2\u0382\u0383\3\2\2\2\u0383\u0385\3\2\2\2\u0384"+
		"\u0382\3\2\2\2\u0385\u0388\5\u0090I\2\u0386\u0388\7B\2\2\u0387\u0382\3"+
		"\2\2\2\u0387\u0386\3\2\2\2\u0388\u008f\3\2\2\2\u0389\u038a\5b\62\2\u038a"+
		"\u038b\5\u0092J\2\u038b\u038c\7B\2\2\u038c\u039e\3\2\2\2\u038d\u038f\5"+
		"$\23\2\u038e\u0390\7B\2\2\u038f\u038e\3\2\2\2\u038f\u0390\3\2\2\2\u0390"+
		"\u039e\3\2\2\2\u0391\u0393\5\64\33\2\u0392\u0394\7B\2\2\u0393\u0392\3"+
		"\2\2\2\u0393\u0394\3\2\2\2\u0394\u039e\3\2\2\2\u0395\u0397\5,\27\2\u0396"+
		"\u0398\7B\2\2\u0397\u0396\3\2\2\2\u0397\u0398\3\2\2\2\u0398\u039e\3\2"+
		"\2\2\u0399\u039b\5\u008aF\2\u039a\u039c\7B\2\2\u039b\u039a\3\2\2\2\u039b"+
		"\u039c\3\2\2\2\u039c\u039e\3\2\2\2\u039d\u0389\3\2\2\2\u039d\u038d\3\2"+
		"\2\2\u039d\u0391\3\2\2\2\u039d\u0395\3\2\2\2\u039d\u0399\3\2\2\2\u039e"+
		"\u0091\3\2\2\2\u039f\u03a2\5\u0094K\2\u03a0\u03a2\5\u0096L\2\u03a1\u039f"+
		"\3\2\2\2\u03a1\u03a0\3\2\2\2\u03a2\u0093\3\2\2\2\u03a3\u03a4\7g\2\2\u03a4"+
		"\u03a5\7<\2\2\u03a5\u03a7\7=\2\2\u03a6\u03a8\5\u0098M\2\u03a7\u03a6\3"+
		"\2\2\2\u03a7\u03a8\3\2\2\2\u03a8\u0095\3\2\2\2\u03a9\u03aa\5V,\2\u03aa"+
		"\u0097\3\2\2\2\u03ab\u03ac\7\17\2\2\u03ac\u03ad\5\u0086D\2\u03ad\u0099"+
		"\3\2\2\2\u03ae\u03b2\7>\2\2\u03af\u03b1\5\u009cO\2\u03b0\u03af\3\2\2\2"+
		"\u03b1\u03b4\3\2\2\2\u03b2\u03b0\3\2\2\2\u03b2\u03b3\3\2\2\2\u03b3\u03b5"+
		"\3\2\2\2\u03b4\u03b2\3\2\2\2\u03b5\u03b6\7?\2\2\u03b6\u009b\3\2\2\2\u03b7"+
		"\u03bb\5\u009eP\2\u03b8\u03bb\5\u00a2R\2\u03b9\u03bb\5\34\17\2\u03ba\u03b7"+
		"\3\2\2\2\u03ba\u03b8\3\2\2\2\u03ba\u03b9\3\2\2\2\u03bb\u009d\3\2\2\2\u03bc"+
		"\u03bd\5\u00a0Q\2\u03bd\u03be\7B\2\2\u03be\u009f\3\2\2\2\u03bf\u03c1\5"+
		"\"\22\2\u03c0\u03bf\3\2\2\2\u03c1\u03c4\3\2\2\2\u03c2\u03c0\3\2\2\2\u03c2"+
		"\u03c3\3\2\2\2\u03c3\u03c5\3\2\2\2\u03c4\u03c2\3\2\2\2\u03c5\u03c6\5b"+
		"\62\2\u03c6\u03c7\5V,\2\u03c7\u00a1\3\2\2\2\u03c8\u0431\5\u009aN\2\u03c9"+
		"\u03ca\7\5\2\2\u03ca\u03cd\5\u00c4c\2\u03cb\u03cc\7K\2\2\u03cc\u03ce\5"+
		"\u00c4c\2\u03cd\u03cb\3\2\2\2\u03cd\u03ce\3\2\2\2\u03ce\u03cf\3\2\2\2"+
		"\u03cf\u03d0\7B\2\2\u03d0\u0431\3\2\2\2\u03d1\u03d2\7\31\2\2\u03d2\u03d3"+
		"\5\u00bc_\2\u03d3\u03d6\5\u00a2R\2\u03d4\u03d5\7\22\2\2\u03d5\u03d7\5"+
		"\u00a2R\2\u03d6\u03d4\3\2\2\2\u03d6\u03d7\3\2\2\2\u03d7\u0431\3\2\2\2"+
		"\u03d8\u03d9\7\30\2\2\u03d9\u03da\7<\2\2\u03da\u03db\5\u00b4[\2\u03db"+
		"\u03dc\7=\2\2\u03dc\u03dd\5\u00a2R\2\u03dd\u0431\3\2\2\2\u03de\u03df\7"+
		"\65\2\2\u03df\u03e0\5\u00bc_\2\u03e0\u03e1\5\u00a2R\2\u03e1\u0431\3\2"+
		"\2\2\u03e2\u03e3\7\20\2\2\u03e3\u03e4\5\u00a2R\2\u03e4\u03e5\7\65\2\2"+
		"\u03e5\u03e6\5\u00bc_\2\u03e6\u03e7\7B\2\2\u03e7\u0431\3\2\2\2\u03e8\u03e9"+
		"\7\62\2\2\u03e9\u03f3\5\u009aN\2\u03ea\u03ec\5\u00a4S\2\u03eb\u03ea\3"+
		"\2\2\2\u03ec\u03ed\3\2\2\2\u03ed\u03eb\3\2\2\2\u03ed\u03ee\3\2\2\2\u03ee"+
		"\u03f0\3\2\2\2\u03ef\u03f1\5\u00a8U\2\u03f0\u03ef\3\2\2\2\u03f0\u03f1"+
		"\3\2\2\2\u03f1\u03f4\3\2\2\2\u03f2\u03f4\5\u00a8U\2\u03f3\u03eb\3\2\2"+
		"\2\u03f3\u03f2\3\2\2\2\u03f4\u0431\3\2\2\2\u03f5\u03f6\7\62\2\2\u03f6"+
		"\u03f7\5\u00aaV\2\u03f7\u03fb\5\u009aN\2\u03f8\u03fa\5\u00a4S\2\u03f9"+
		"\u03f8\3\2\2\2\u03fa\u03fd\3\2\2\2\u03fb\u03f9\3\2\2\2\u03fb\u03fc\3\2"+
		"\2\2\u03fc\u03ff\3\2\2\2\u03fd\u03fb\3\2\2\2\u03fe\u0400\5\u00a8U\2\u03ff"+
		"\u03fe\3\2\2\2\u03ff\u0400\3\2\2\2\u0400\u0431\3\2\2\2\u0401\u0402\7,"+
		"\2\2\u0402\u0403\5\u00bc_\2\u0403\u0407\7>\2\2\u0404\u0406\5\u00b0Y\2"+
		"\u0405\u0404\3\2\2\2\u0406\u0409\3\2\2\2\u0407\u0405\3\2\2\2\u0407\u0408"+
		"\3\2\2\2\u0408\u040d\3\2\2\2\u0409\u0407\3\2\2\2\u040a\u040c\5\u00b2Z"+
		"\2\u040b\u040a\3\2\2\2\u040c\u040f\3\2\2\2\u040d\u040b\3\2\2\2\u040d\u040e"+
		"\3\2\2\2\u040e\u0410\3\2\2\2\u040f\u040d\3\2\2\2\u0410\u0411\7?\2\2\u0411"+
		"\u0431\3\2\2\2\u0412\u0413\7-\2\2\u0413\u0414\5\u00bc_\2\u0414\u0415\5"+
		"\u009aN\2\u0415\u0431\3\2\2\2\u0416\u0418\7\'\2\2\u0417\u0419\5\u00c4"+
		"c\2\u0418\u0417\3\2\2\2\u0418\u0419\3\2\2\2\u0419\u041a\3\2\2\2\u041a"+
		"\u0431\7B\2\2\u041b\u041c\7/\2\2\u041c\u041d\5\u00c4c\2\u041d\u041e\7"+
		"B\2\2\u041e\u0431\3\2\2\2\u041f\u0421\7\7\2\2\u0420\u0422\7g\2\2\u0421"+
		"\u0420\3\2\2\2\u0421\u0422\3\2\2\2\u0422\u0423\3\2\2\2\u0423\u0431\7B"+
		"\2\2\u0424\u0426\7\16\2\2\u0425\u0427\7g\2\2\u0426\u0425\3\2\2\2\u0426"+
		"\u0427\3\2\2\2\u0427\u0428\3\2\2\2\u0428\u0431\7B\2\2\u0429\u0431\7B\2"+
		"\2\u042a\u042b\5\u00c0a\2\u042b\u042c\7B\2\2\u042c\u0431\3\2\2\2\u042d"+
		"\u042e\7g\2\2\u042e\u042f\7K\2\2\u042f\u0431\5\u00a2R\2\u0430\u03c8\3"+
		"\2\2\2\u0430\u03c9\3\2\2\2\u0430\u03d1\3\2\2\2\u0430\u03d8\3\2\2\2\u0430"+
		"\u03de\3\2\2\2\u0430\u03e2\3\2\2\2\u0430\u03e8\3\2\2\2\u0430\u03f5\3\2"+
		"\2\2\u0430\u0401\3\2\2\2\u0430\u0412\3\2\2\2\u0430\u0416\3\2\2\2\u0430"+
		"\u041b\3\2\2\2\u0430\u041f\3\2\2\2\u0430\u0424\3\2\2\2\u0430\u0429\3\2"+
		"\2\2\u0430\u042a\3\2\2\2\u0430\u042d\3\2\2\2\u0431\u00a3\3\2\2\2\u0432"+
		"\u0433\7\n\2\2\u0433\u0437\7<\2\2\u0434\u0436\5\"\22\2\u0435\u0434\3\2"+
		"\2\2\u0436\u0439\3\2\2\2\u0437\u0435\3\2\2\2\u0437\u0438\3\2\2\2\u0438"+
		"\u043a\3\2\2\2\u0439\u0437\3\2\2\2\u043a\u043b\5\u00a6T\2\u043b\u043c"+
		"\7g\2\2\u043c\u043d\7=\2\2\u043d\u043e\5\u009aN\2\u043e\u00a5\3\2\2\2"+
		"\u043f\u0444\5z>\2\u0440\u0441\7Y\2\2\u0441\u0443\5z>\2\u0442\u0440\3"+
		"\2\2\2\u0443\u0446\3\2\2\2\u0444\u0442\3\2\2\2\u0444\u0445\3\2\2\2\u0445"+
		"\u00a7\3\2\2\2\u0446\u0444\3\2\2\2\u0447\u0448\7\26\2\2\u0448\u0449\5"+
		"\u009aN\2\u0449\u00a9\3\2\2\2\u044a\u044b\7<\2\2\u044b\u044d\5\u00acW"+
		"\2\u044c\u044e\7B\2\2\u044d\u044c\3\2\2\2\u044d\u044e\3\2\2\2\u044e\u044f"+
		"\3\2\2\2\u044f\u0450\7=\2\2\u0450\u00ab\3\2\2\2\u0451\u0456\5\u00aeX\2"+
		"\u0452\u0453\7B\2\2\u0453\u0455\5\u00aeX\2\u0454\u0452\3\2\2\2\u0455\u0458"+
		"\3\2\2\2\u0456\u0454\3\2\2\2\u0456\u0457\3\2\2\2\u0457\u00ad\3\2\2\2\u0458"+
		"\u0456\3\2\2\2\u0459\u045b\5\"\22\2\u045a\u0459\3\2\2\2\u045b\u045e\3"+
		"\2\2\2\u045c\u045a\3\2\2\2\u045c\u045d\3\2\2\2\u045d\u045f\3\2\2\2\u045e"+
		"\u045c\3\2\2\2\u045f\u0460\5d\63\2\u0460\u0461\5Z.\2\u0461\u0462\7E\2"+
		"\2\u0462\u0463\5\u00c4c\2\u0463\u00af\3\2\2\2\u0464\u0466\5\u00b2Z\2\u0465"+
		"\u0464\3\2\2\2\u0466\u0467\3\2\2\2\u0467\u0465\3\2\2\2\u0467\u0468\3\2"+
		"\2\2\u0468\u046a\3\2\2\2\u0469\u046b\5\u009cO\2\u046a\u0469\3\2\2\2\u046b"+
		"\u046c\3\2\2\2\u046c\u046a\3\2\2\2\u046c\u046d\3\2\2\2\u046d\u00b1\3\2"+
		"\2\2\u046e\u046f\7\t\2\2\u046f\u0470\5\u00c2b\2\u0470\u0471\7K\2\2\u0471"+
		"\u0479\3\2\2\2\u0472\u0473\7\t\2\2\u0473\u0474\5`\61\2\u0474\u0475\7K"+
		"\2\2\u0475\u0479\3\2\2\2\u0476\u0477\7\17\2\2\u0477\u0479\7K\2\2\u0478"+
		"\u046e\3\2\2\2\u0478\u0472\3\2\2\2\u0478\u0476\3\2\2\2\u0479\u00b3\3\2"+
		"\2\2\u047a\u0487\5\u00b8]\2\u047b\u047d\5\u00b6\\\2\u047c\u047b\3\2\2"+
		"\2\u047c\u047d\3\2\2\2\u047d\u047e\3\2\2\2\u047e\u0480\7B\2\2\u047f\u0481"+
		"\5\u00c4c\2\u0480\u047f\3\2\2\2\u0480\u0481\3\2\2\2\u0481\u0482\3\2\2"+
		"\2\u0482\u0484\7B\2\2\u0483\u0485\5\u00ba^\2\u0484\u0483\3\2\2\2\u0484"+
		"\u0485\3\2\2\2\u0485\u0487\3\2\2\2\u0486\u047a\3\2\2\2\u0486\u047c\3\2"+
		"\2\2\u0487\u00b5\3\2\2\2\u0488\u048b\5\u00a0Q\2\u0489\u048b\5\u00be`\2"+
		"\u048a\u0488\3\2\2\2\u048a\u0489\3\2\2\2\u048b\u00b7\3\2\2\2\u048c\u048e"+
		"\5\"\22\2\u048d\u048c\3\2\2\2\u048e\u0491\3\2\2\2\u048f\u048d\3\2\2\2"+
		"\u048f\u0490\3\2\2\2\u0490\u0492\3\2\2\2\u0491\u048f\3\2\2\2\u0492\u0493"+
		"\5b\62\2\u0493\u0494\5Z.\2\u0494\u0495\7K\2\2\u0495\u0496\5\u00c4c\2\u0496"+
		"\u00b9\3\2\2\2\u0497\u0498\5\u00be`\2\u0498\u00bb\3\2\2\2\u0499\u049a"+
		"\7<\2\2\u049a\u049b\5\u00c4c\2\u049b\u049c\7=\2\2\u049c\u00bd\3\2\2\2"+
		"\u049d\u04a2\5\u00c4c\2\u049e\u049f\7C\2\2\u049f\u04a1\5\u00c4c\2\u04a0"+
		"\u049e\3\2\2\2\u04a1\u04a4\3\2\2\2\u04a2\u04a0\3\2\2\2\u04a2\u04a3\3\2"+
		"\2\2\u04a3\u00bf\3\2\2\2\u04a4\u04a2\3\2\2\2\u04a5\u04a6\5\u00c4c\2\u04a6"+
		"\u00c1\3\2\2\2\u04a7\u04a8\5\u00c4c\2\u04a8\u00c3\3\2\2\2\u04a9\u04aa"+
		"\bc\1\2\u04aa\u04ab\7<\2\2\u04ab\u04ac\5b\62\2\u04ac\u04ad\7=\2\2\u04ad"+
		"\u04ae\5\u00c4c\23\u04ae\u04b7\3\2\2\2\u04af\u04b0\t\b\2\2\u04b0\u04b7"+
		"\5\u00c4c\21\u04b1\u04b2\t\t\2\2\u04b2\u04b7\5\u00c4c\20\u04b3\u04b7\5"+
		"\u00c6d\2\u04b4\u04b5\7\"\2\2\u04b5\u04b7\5\u00c8e\2\u04b6\u04a9\3\2\2"+
		"\2\u04b6\u04af\3\2\2\2\u04b6\u04b1\3\2\2\2\u04b6\u04b3\3\2\2\2\u04b6\u04b4"+
		"\3\2\2\2\u04b7\u050d\3\2\2\2\u04b8\u04b9\f\17\2\2\u04b9\u04ba\t\n\2\2"+
		"\u04ba\u050c\5\u00c4c\20\u04bb\u04bc\f\16\2\2\u04bc\u04bd\t\13\2\2\u04bd"+
		"\u050c\5\u00c4c\17\u04be\u04c6\f\r\2\2\u04bf\u04c0\7G\2\2\u04c0\u04c7"+
		"\7G\2\2\u04c1\u04c2\7F\2\2\u04c2\u04c3\7F\2\2\u04c3\u04c7\7F\2\2\u04c4"+
		"\u04c5\7F\2\2\u04c5\u04c7\7F\2\2\u04c6\u04bf\3\2\2\2\u04c6\u04c1\3\2\2"+
		"\2\u04c6\u04c4\3\2\2\2\u04c7\u04c8\3\2\2\2\u04c8\u050c\5\u00c4c\16\u04c9"+
		"\u04ca\f\f\2\2\u04ca\u04cb\t\f\2\2\u04cb\u050c\5\u00c4c\r\u04cc\u04cd"+
		"\f\n\2\2\u04cd\u04ce\t\r\2\2\u04ce\u050c\5\u00c4c\13\u04cf\u04d0\f\t\2"+
		"\2\u04d0\u04d1\7X\2\2\u04d1\u050c\5\u00c4c\n\u04d2\u04d3\f\b\2\2\u04d3"+
		"\u04d4\7Z\2\2\u04d4\u050c\5\u00c4c\t\u04d5\u04d6\f\7\2\2\u04d6\u04d7\7"+
		"Y\2\2\u04d7\u050c\5\u00c4c\b\u04d8\u04d9\f\6\2\2\u04d9\u04da\7P\2\2\u04da"+
		"\u050c\5\u00c4c\7\u04db\u04dc\f\5\2\2\u04dc\u04dd\7Q\2\2\u04dd\u050c\5"+
		"\u00c4c\6\u04de\u04df\f\4\2\2\u04df\u04e0\7J\2\2\u04e0\u04e1\5\u00c4c"+
		"\2\u04e1\u04e2\7K\2\2\u04e2\u04e3\5\u00c4c\5\u04e3\u050c\3\2\2\2\u04e4"+
		"\u04e5\f\3\2\2\u04e5\u04e6\t\16\2\2\u04e6\u050c\5\u00c4c\3\u04e7\u04e8"+
		"\f\33\2\2\u04e8\u04e9\7D\2\2\u04e9\u050c\7g\2\2\u04ea\u04eb\f\32\2\2\u04eb"+
		"\u04ec\7D\2\2\u04ec\u050c\7.\2\2\u04ed\u04ee\f\31\2\2\u04ee\u04ef\7D\2"+
		"\2\u04ef\u04f1\7\"\2\2\u04f0\u04f2\5\u00d4k\2\u04f1\u04f0\3\2\2\2\u04f1"+
		"\u04f2\3\2\2\2\u04f2\u04f3\3\2\2\2\u04f3\u050c\5\u00ccg\2\u04f4\u04f5"+
		"\f\30\2\2\u04f5\u04f6\7D\2\2\u04f6\u04f7\7+\2\2\u04f7\u050c\5\u00dan\2"+
		"\u04f8\u04f9\f\27\2\2\u04f9\u04fa\7D\2\2\u04fa\u050c\5\u00d2j\2\u04fb"+
		"\u04fc\f\26\2\2\u04fc\u04fd\7@\2\2\u04fd\u04fe\5\u00c4c\2\u04fe\u04ff"+
		"\7A\2\2\u04ff\u050c\3\2\2\2\u0500\u0501\f\25\2\2\u0501\u0503\7<\2\2\u0502"+
		"\u0504\5\u00be`\2\u0503\u0502\3\2\2\2\u0503\u0504\3\2\2\2\u0504\u0505"+
		"\3\2\2\2\u0505\u050c\7=\2\2\u0506\u0507\f\22\2\2\u0507\u050c\t\17\2\2"+
		"\u0508\u0509\f\13\2\2\u0509\u050a\7\35\2\2\u050a\u050c\5b\62\2\u050b\u04b8"+
		"\3\2\2\2\u050b\u04bb\3\2\2\2\u050b\u04be\3\2\2\2\u050b\u04c9\3\2\2\2\u050b"+
		"\u04cc\3\2\2\2\u050b\u04cf\3\2\2\2\u050b\u04d2\3\2\2\2\u050b\u04d5\3\2"+
		"\2\2\u050b\u04d8\3\2\2\2\u050b\u04db\3\2\2\2\u050b\u04de\3\2\2\2\u050b"+
		"\u04e4\3\2\2\2\u050b\u04e7\3\2\2\2\u050b\u04ea\3\2\2\2\u050b\u04ed\3\2"+
		"\2\2\u050b\u04f4\3\2\2\2\u050b\u04f8\3\2\2\2\u050b\u04fb\3\2\2\2\u050b"+
		"\u0500\3\2\2\2\u050b\u0506\3\2\2\2\u050b\u0508\3\2\2\2\u050c\u050f\3\2"+
		"\2\2\u050d\u050b\3\2\2\2\u050d\u050e\3\2\2\2\u050e\u00c5\3\2\2\2\u050f"+
		"\u050d\3\2\2\2\u0510\u0511\7<\2\2\u0511\u0512\5\u00c4c\2\u0512\u0513\7"+
		"=\2\2\u0513\u0526\3\2\2\2\u0514\u0526\7.\2\2\u0515\u0526\7+\2\2\u0516"+
		"\u0526\5|?\2\u0517\u0526\7g\2\2\u0518\u0519\5b\62\2\u0519\u051a\7D\2\2"+
		"\u051a\u051b\7\f\2\2\u051b\u0526\3\2\2\2\u051c\u051d\7\63\2\2\u051d\u051e"+
		"\7D\2\2\u051e\u0526\7\f\2\2\u051f\u0523\5\u00d4k\2\u0520\u0524\5\u00dc"+
		"o\2\u0521\u0522\7.\2\2\u0522\u0524\5\u00dep\2\u0523\u0520\3\2\2\2\u0523"+
		"\u0521\3\2\2\2\u0524\u0526\3\2\2\2\u0525\u0510\3\2\2\2\u0525\u0514\3\2"+
		"\2\2\u0525\u0515\3\2\2\2\u0525\u0516\3\2\2\2\u0525\u0517\3\2\2\2\u0525"+
		"\u0518\3\2\2\2\u0525\u051c\3\2\2\2\u0525\u051f\3\2\2\2\u0526\u00c7\3\2"+
		"\2\2\u0527\u0528\5\u00d4k\2\u0528\u0529\5\u00caf\2\u0529\u052a\5\u00d0"+
		"i\2\u052a\u0531\3\2\2\2\u052b\u052e\5\u00caf\2\u052c\u052f\5\u00ceh\2"+
		"\u052d\u052f\5\u00d0i\2\u052e\u052c\3\2\2\2\u052e\u052d\3\2\2\2\u052f"+
		"\u0531\3\2\2\2\u0530\u0527\3\2\2\2\u0530\u052b\3\2\2\2\u0531\u00c9\3\2"+
		"\2\2\u0532\u0534\7g\2\2\u0533\u0535\5\u00d6l\2\u0534\u0533\3\2\2\2\u0534"+
		"\u0535\3\2\2\2\u0535\u053d\3\2\2\2\u0536\u0537\7D\2\2\u0537\u0539\7g\2"+
		"\2\u0538\u053a\5\u00d6l\2\u0539\u0538\3\2\2\2\u0539\u053a\3\2\2\2\u053a"+
		"\u053c\3\2\2\2\u053b\u0536\3\2\2\2\u053c\u053f\3\2\2\2\u053d\u053b\3\2"+
		"\2\2\u053d\u053e\3\2\2\2\u053e\u0542\3\2\2\2\u053f\u053d\3\2\2\2\u0540"+
		"\u0542\5f\64\2\u0541\u0532\3\2\2\2\u0541\u0540\3\2\2\2\u0542\u00cb\3\2"+
		"\2\2\u0543\u0545\7g\2\2\u0544\u0546\5\u00d8m\2\u0545\u0544\3\2\2\2\u0545"+
		"\u0546\3\2\2\2\u0546\u0547\3\2\2\2\u0547\u0548\5\u00d0i\2\u0548\u00cd"+
		"\3\2\2\2\u0549\u0565\7@\2\2\u054a\u054f\7A\2\2\u054b\u054c\7@\2\2\u054c"+
		"\u054e\7A\2\2\u054d\u054b\3\2\2\2\u054e\u0551\3\2\2\2\u054f\u054d\3\2"+
		"\2\2\u054f\u0550\3\2\2\2\u0550\u0552\3\2\2\2\u0551\u054f\3\2\2\2\u0552"+
		"\u0566\5^\60\2\u0553\u0554\5\u00c4c\2\u0554\u055b\7A\2\2\u0555\u0556\7"+
		"@\2\2\u0556\u0557\5\u00c4c\2\u0557\u0558\7A\2\2\u0558\u055a\3\2\2\2\u0559"+
		"\u0555\3\2\2\2\u055a\u055d\3\2\2\2\u055b\u0559\3\2\2\2\u055b\u055c\3\2"+
		"\2\2\u055c\u0562\3\2\2\2\u055d\u055b\3\2\2\2\u055e\u055f\7@\2\2\u055f"+
		"\u0561\7A\2\2\u0560\u055e\3\2\2\2\u0561\u0564\3\2\2\2\u0562\u0560\3\2"+
		"\2\2\u0562\u0563\3\2\2\2\u0563\u0566\3\2\2\2\u0564\u0562\3\2\2\2\u0565"+
		"\u054a\3\2\2\2\u0565\u0553\3\2\2\2\u0566\u00cf\3\2\2\2\u0567\u0569\5\u00de"+
		"p\2\u0568\u056a\58\35\2\u0569\u0568\3\2\2\2\u0569\u056a\3\2\2\2\u056a"+
		"\u00d1\3\2\2\2\u056b\u056c\5\u00d4k\2\u056c\u056d\5\u00dco\2\u056d\u00d3"+
		"\3\2\2\2\u056e\u056f\7G\2\2\u056f\u0570\5\66\34\2\u0570\u0571\7F\2\2\u0571"+
		"\u00d5\3\2\2\2\u0572\u0573\7G\2\2\u0573\u0576\7F\2\2\u0574\u0576\5h\65"+
		"\2\u0575\u0572\3\2\2\2\u0575\u0574\3\2\2\2\u0576\u00d7\3\2\2\2\u0577\u0578"+
		"\7G\2\2\u0578\u057b\7F\2\2\u0579\u057b\5\u00d4k\2\u057a\u0577\3\2\2\2"+
		"\u057a\u0579\3\2\2\2\u057b\u00d9\3\2\2\2\u057c\u0583\5\u00dep\2\u057d"+
		"\u057e\7D\2\2\u057e\u0580\7g\2\2\u057f\u0581\5\u00dep\2\u0580\u057f\3"+
		"\2\2\2\u0580\u0581\3\2\2\2\u0581\u0583\3\2\2\2\u0582\u057c\3\2\2\2\u0582"+
		"\u057d\3\2\2\2\u0583\u00db\3\2\2\2\u0584\u0585\7+\2\2\u0585\u0589\5\u00da"+
		"n\2\u0586\u0587\7g\2\2\u0587\u0589\5\u00dep\2\u0588\u0584\3\2\2\2\u0588"+
		"\u0586\3\2\2\2\u0589\u00dd\3\2\2\2\u058a\u058c\7<\2\2\u058b\u058d\5\u00be"+
		"`\2\u058c\u058b\3\2\2\2\u058c\u058d\3\2\2\2\u058d\u058e\3\2\2\2\u058e"+
		"\u058f\7=\2\2\u058f\u00df\3\2\2\2\u00ab\u00e6\u00ef\u00f7\u00fd\u0106"+
		"\u010c\u0113\u0119\u0121\u0123\u012b\u0133\u0135\u013b\u0141\u0149\u014d"+
		"\u0154\u0158\u015a\u015d\u0162\u0168\u0170\u0179\u017e\u0185\u018c\u0193"+
		"\u019a\u019f\u01a3\u01a7\u01ab\u01b0\u01b4\u01b8\u01c2\u01ca\u01d1\u01d8"+
		"\u01dc\u01df\u01e2\u01eb\u01f1\u01f6\u01f9\u01ff\u0205\u0209\u0212\u0219"+
		"\u0222\u0229\u022f\u0233\u023e\u0242\u024a\u024f\u0253\u025c\u026a\u026f"+
		"\u0278\u0280\u028a\u0292\u029a\u029f\u02ab\u02b1\u02b8\u02bd\u02c5\u02c9"+
		"\u02cb\u02d6\u02de\u02e1\u02e5\u02ea\u02ee\u02f9\u0302\u0304\u030b\u0310"+
		"\u0319\u031e\u0321\u0326\u032f\u033f\u0349\u034c\u0355\u035f\u0367\u036a"+
		"\u036d\u037a\u0382\u0387\u038f\u0393\u0397\u039b\u039d\u03a1\u03a7\u03b2"+
		"\u03ba\u03c2\u03cd\u03d6\u03ed\u03f0\u03f3\u03fb\u03ff\u0407\u040d\u0418"+
		"\u0421\u0426\u0430\u0437\u0444\u044d\u0456\u045c\u0467\u046c\u0478\u047c"+
		"\u0480\u0484\u0486\u048a\u048f\u04a2\u04b6\u04c6\u04f1\u0503\u050b\u050d"+
		"\u0523\u0525\u052e\u0530\u0534\u0539\u053d\u0541\u0545\u054f\u055b\u0562"+
		"\u0565\u0569\u0575\u057a\u0580\u0582\u0588\u058c";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}