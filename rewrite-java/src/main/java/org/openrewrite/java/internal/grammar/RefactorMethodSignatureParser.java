/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Generated from /Users/jon/Projects/github/openrewrite/rewrite/rewrite-java/src/main/antlr/RefactorMethodSignatureParser.g4 by ANTLR 4.8
package org.openrewrite.java.internal.grammar;
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
	static { RuntimeMetaData.checkVersion("4.8", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		DOTDOT=1, POUND=2, SPACE=3, ABSTRACT=4, ASSERT=5, BOOLEAN=6, BREAK=7, 
		BYTE=8, CASE=9, CATCH=10, CHAR=11, CLASS=12, CONST=13, CONTINUE=14, DEFAULT=15, 
		DO=16, DOUBLE=17, ELSE=18, ENUM=19, EXTENDS=20, FINAL=21, FINALLY=22, 
		FLOAT=23, FOR=24, IF=25, GOTO=26, IMPLEMENTS=27, IMPORT=28, INSTANCEOF=29, 
		INT=30, INTERFACE=31, LONG=32, NATIVE=33, NEW=34, PACKAGE=35, PRIVATE=36, 
		PROTECTED=37, PUBLIC=38, RETURN=39, SHORT=40, STATIC=41, STRICTFP=42, 
		SUPER=43, SWITCH=44, SYNCHRONIZED=45, THIS=46, THROW=47, THROWS=48, TRANSIENT=49, 
		TRY=50, VOID=51, VOLATILE=52, WHILE=53, IntegerLiteral=54, FloatingPointLiteral=55, 
		BooleanLiteral=56, CharacterLiteral=57, StringLiteral=58, NullLiteral=59, 
		LPAREN=60, RPAREN=61, LBRACE=62, RBRACE=63, LBRACK=64, RBRACK=65, SEMI=66, 
		COMMA=67, DOT=68, ASSIGN=69, GT=70, LT=71, BANG=72, TILDE=73, QUESTION=74, 
		COLON=75, EQUAL=76, LE=77, GE=78, NOTEQUAL=79, AND=80, OR=81, INC=82, 
		DEC=83, ADD=84, SUB=85, MUL=86, DIV=87, BITAND=88, BITOR=89, CARET=90, 
		MOD=91, ADD_ASSIGN=92, SUB_ASSIGN=93, MUL_ASSIGN=94, DIV_ASSIGN=95, AND_ASSIGN=96, 
		OR_ASSIGN=97, XOR_ASSIGN=98, MOD_ASSIGN=99, LSHIFT_ASSIGN=100, RSHIFT_ASSIGN=101, 
		URSHIFT_ASSIGN=102, Identifier=103, AT=104, ELLIPSIS=105, WS=106, COMMENT=107, 
		LINE_COMMENT=108;
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
	private static String[] makeRuleNames() {
		return new String[] {
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
			"localVariableDeclaration", "statement", "catchClause", "catchType", 
			"finallyBlock", "resourceSpecification", "resources", "resource", "switchBlockStatementGroup", 
			"switchLabel", "forControl", "forInit", "enhancedForControl", "forUpdate", 
			"parExpression", "expressionList", "statementExpression", "constantExpression", 
			"expression", "primary", "creator", "createdName", "innerCreator", "arrayCreatorRest", 
			"classCreatorRest", "explicitGenericInvocation", "nonWildcardTypeArguments", 
			"typeArgumentsOrDiamond", "nonWildcardTypeArgumentsOrDiamond", "superSuffix", 
			"explicitGenericInvocationSuffix", "arguments"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'..'", "'#'", "' '", "'abstract'", "'assert'", "'boolean'", "'break'", 
			"'byte'", "'case'", "'catch'", "'char'", "'class'", "'const'", "'continue'", 
			"'default'", "'do'", "'double'", "'else'", "'enum'", "'extends'", "'final'", 
			"'finally'", "'float'", "'for'", "'if'", "'goto'", "'implements'", "'import'", 
			"'instanceof'", "'int'", "'interface'", "'long'", "'native'", "'new'", 
			"'package'", "'private'", "'protected'", "'public'", "'return'", "'short'", 
			"'static'", "'strictfp'", "'super'", "'switch'", "'synchronized'", "'this'", 
			"'throw'", "'throws'", "'transient'", "'try'", "'void'", "'volatile'", 
			"'while'", null, null, null, null, null, "'null'", "'('", "')'", "'{'", 
			"'}'", "'['", "']'", "';'", "','", "'.'", "'='", "'>'", "'<'", "'!'", 
			"'~'", "'?'", "':'", "'=='", "'<='", "'>='", "'!='", "'&&'", "'||'", 
			"'++'", "'--'", "'+'", "'-'", "'*'", "'/'", "'&'", "'|'", "'^'", "'%'", 
			"'+='", "'-='", "'*='", "'/='", "'&='", "'|='", "'^='", "'%='", "'<<='", 
			"'>>='", "'>>>='", null, "'@'", "'...'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "DOTDOT", "POUND", "SPACE", "ABSTRACT", "ASSERT", "BOOLEAN", "BREAK", 
			"BYTE", "CASE", "CATCH", "CHAR", "CLASS", "CONST", "CONTINUE", "DEFAULT", 
			"DO", "DOUBLE", "ELSE", "ENUM", "EXTENDS", "FINAL", "FINALLY", "FLOAT", 
			"FOR", "IF", "GOTO", "IMPLEMENTS", "IMPORT", "INSTANCEOF", "INT", "INTERFACE", 
			"LONG", "NATIVE", "NEW", "PACKAGE", "PRIVATE", "PROTECTED", "PUBLIC", 
			"RETURN", "SHORT", "STATIC", "STRICTFP", "SUPER", "SWITCH", "SYNCHRONIZED", 
			"THIS", "THROW", "THROWS", "TRANSIENT", "TRY", "VOID", "VOLATILE", "WHILE", 
			"IntegerLiteral", "FloatingPointLiteral", "BooleanLiteral", "CharacterLiteral", 
			"StringLiteral", "NullLiteral", "LPAREN", "RPAREN", "LBRACE", "RBRACE", 
			"LBRACK", "RBRACK", "SEMI", "COMMA", "DOT", "ASSIGN", "GT", "LT", "BANG", 
			"TILDE", "QUESTION", "COLON", "EQUAL", "LE", "GE", "NOTEQUAL", "AND", 
			"OR", "INC", "DEC", "ADD", "SUB", "MUL", "DIV", "BITAND", "BITOR", "CARET", 
			"MOD", "ADD_ASSIGN", "SUB_ASSIGN", "MUL_ASSIGN", "DIV_ASSIGN", "AND_ASSIGN", 
			"OR_ASSIGN", "XOR_ASSIGN", "MOD_ASSIGN", "LSHIFT_ASSIGN", "RSHIFT_ASSIGN", 
			"URSHIFT_ASSIGN", "Identifier", "AT", "ELLIPSIS", "WS", "COMMENT", "LINE_COMMENT"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "RefactorMethodSignatureParser.g4"; }

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
		public TargetTypePatternContext targetTypePattern() {
			return getRuleContext(TargetTypePatternContext.class,0);
		}
		public SimpleNamePatternContext simpleNamePattern() {
			return getRuleContext(SimpleNamePatternContext.class,0);
		}
		public FormalParametersPatternContext formalParametersPattern() {
			return getRuleContext(FormalParametersPatternContext.class,0);
		}
		public TerminalNode DOT() { return getToken(RefactorMethodSignatureParser.DOT, 0); }
		public TerminalNode POUND() { return getToken(RefactorMethodSignatureParser.POUND, 0); }
		public List<TerminalNode> SPACE() { return getTokens(RefactorMethodSignatureParser.SPACE); }
		public TerminalNode SPACE(int i) {
			return getToken(RefactorMethodSignatureParser.SPACE, i);
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
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(222);
			targetTypePattern(0);
			setState(231);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SPACE:
			case MUL:
			case Identifier:
				{
				setState(226);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==SPACE) {
					{
					{
					setState(223);
					match(SPACE);
					}
					}
					setState(228);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case DOT:
				{
				setState(229);
				match(DOT);
				}
				break;
			case POUND:
				{
				setState(230);
				match(POUND);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(233);
			simpleNamePattern();
			setState(234);
			formalParametersPattern();
			}
		}
		catch (RecognitionException re) {
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
		public TerminalNode LPAREN() { return getToken(RefactorMethodSignatureParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(RefactorMethodSignatureParser.RPAREN, 0); }
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
			setState(236);
			match(LPAREN);
			setState(238);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << DOTDOT) | (1L << BOOLEAN) | (1L << BYTE) | (1L << CHAR) | (1L << DOUBLE) | (1L << FLOAT) | (1L << INT) | (1L << LONG) | (1L << SHORT) | (1L << LPAREN))) != 0) || ((((_la - 68)) & ~0x3f) == 0 && ((1L << (_la - 68)) & ((1L << (DOT - 68)) | (1L << (BANG - 68)) | (1L << (MUL - 68)) | (1L << (Identifier - 68)))) != 0)) {
				{
				setState(237);
				formalsPattern();
				}
			}

			setState(240);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
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
		public DotDotContext dotDot() {
			return getRuleContext(DotDotContext.class,0);
		}
		public List<TerminalNode> COMMA() { return getTokens(RefactorMethodSignatureParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(RefactorMethodSignatureParser.COMMA, i);
		}
		public List<FormalsPatternAfterDotDotContext> formalsPatternAfterDotDot() {
			return getRuleContexts(FormalsPatternAfterDotDotContext.class);
		}
		public FormalsPatternAfterDotDotContext formalsPatternAfterDotDot(int i) {
			return getRuleContext(FormalsPatternAfterDotDotContext.class,i);
		}
		public List<TerminalNode> SPACE() { return getTokens(RefactorMethodSignatureParser.SPACE); }
		public TerminalNode SPACE(int i) {
			return getToken(RefactorMethodSignatureParser.SPACE, i);
		}
		public OptionalParensTypePatternContext optionalParensTypePattern() {
			return getRuleContext(OptionalParensTypePatternContext.class,0);
		}
		public List<FormalsPatternContext> formalsPattern() {
			return getRuleContexts(FormalsPatternContext.class);
		}
		public FormalsPatternContext formalsPattern(int i) {
			return getRuleContext(FormalsPatternContext.class,i);
		}
		public FormalTypePatternContext formalTypePattern() {
			return getRuleContext(FormalTypePatternContext.class,0);
		}
		public TerminalNode ELLIPSIS() { return getToken(RefactorMethodSignatureParser.ELLIPSIS, 0); }
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
		int _la;
		try {
			int _alt;
			setState(273);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(242);
				dotDot();
				setState(253);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,4,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(243);
						match(COMMA);
						setState(247);
						_errHandler.sync(this);
						_la = _input.LA(1);
						while (_la==SPACE) {
							{
							{
							setState(244);
							match(SPACE);
							}
							}
							setState(249);
							_errHandler.sync(this);
							_la = _input.LA(1);
						}
						setState(250);
						formalsPatternAfterDotDot();
						}
						} 
					}
					setState(255);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,4,_ctx);
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(256);
				optionalParensTypePattern();
				setState(267);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,6,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(257);
						match(COMMA);
						setState(261);
						_errHandler.sync(this);
						_la = _input.LA(1);
						while (_la==SPACE) {
							{
							{
							setState(258);
							match(SPACE);
							}
							}
							setState(263);
							_errHandler.sync(this);
							_la = _input.LA(1);
						}
						setState(264);
						formalsPattern();
						}
						} 
					}
					setState(269);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,6,_ctx);
				}
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(270);
				formalTypePattern(0);
				setState(271);
				match(ELLIPSIS);
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
			setState(275);
			match(DOTDOT);
			}
		}
		catch (RecognitionException re) {
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
		public OptionalParensTypePatternContext optionalParensTypePattern() {
			return getRuleContext(OptionalParensTypePatternContext.class,0);
		}
		public List<TerminalNode> COMMA() { return getTokens(RefactorMethodSignatureParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(RefactorMethodSignatureParser.COMMA, i);
		}
		public List<FormalsPatternAfterDotDotContext> formalsPatternAfterDotDot() {
			return getRuleContexts(FormalsPatternAfterDotDotContext.class);
		}
		public FormalsPatternAfterDotDotContext formalsPatternAfterDotDot(int i) {
			return getRuleContext(FormalsPatternAfterDotDotContext.class,i);
		}
		public List<TerminalNode> SPACE() { return getTokens(RefactorMethodSignatureParser.SPACE); }
		public TerminalNode SPACE(int i) {
			return getToken(RefactorMethodSignatureParser.SPACE, i);
		}
		public FormalTypePatternContext formalTypePattern() {
			return getRuleContext(FormalTypePatternContext.class,0);
		}
		public TerminalNode ELLIPSIS() { return getToken(RefactorMethodSignatureParser.ELLIPSIS, 0); }
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
		int _la;
		try {
			int _alt;
			setState(294);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,10,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(277);
				optionalParensTypePattern();
				setState(288);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,9,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(278);
						match(COMMA);
						setState(282);
						_errHandler.sync(this);
						_la = _input.LA(1);
						while (_la==SPACE) {
							{
							{
							setState(279);
							match(SPACE);
							}
							}
							setState(284);
							_errHandler.sync(this);
							_la = _input.LA(1);
						}
						setState(285);
						formalsPatternAfterDotDot();
						}
						} 
					}
					setState(290);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,9,_ctx);
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(291);
				formalTypePattern(0);
				setState(292);
				match(ELLIPSIS);
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
		public TerminalNode LPAREN() { return getToken(RefactorMethodSignatureParser.LPAREN, 0); }
		public FormalTypePatternContext formalTypePattern() {
			return getRuleContext(FormalTypePatternContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(RefactorMethodSignatureParser.RPAREN, 0); }
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
			setState(301);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LPAREN:
				enterOuterAlt(_localctx, 1);
				{
				setState(296);
				match(LPAREN);
				setState(297);
				formalTypePattern(0);
				setState(298);
				match(RPAREN);
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
				setState(300);
				formalTypePattern(0);
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
		public ClassNameOrInterfaceContext classNameOrInterface() {
			return getRuleContext(ClassNameOrInterfaceContext.class,0);
		}
		public TerminalNode BANG() { return getToken(RefactorMethodSignatureParser.BANG, 0); }
		public List<TargetTypePatternContext> targetTypePattern() {
			return getRuleContexts(TargetTypePatternContext.class);
		}
		public TargetTypePatternContext targetTypePattern(int i) {
			return getRuleContext(TargetTypePatternContext.class,i);
		}
		public TerminalNode AND() { return getToken(RefactorMethodSignatureParser.AND, 0); }
		public TerminalNode OR() { return getToken(RefactorMethodSignatureParser.OR, 0); }
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
			setState(307);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DOTDOT:
			case DOT:
			case MUL:
			case Identifier:
				{
				setState(304);
				classNameOrInterface();
				}
				break;
			case BANG:
				{
				setState(305);
				match(BANG);
				setState(306);
				targetTypePattern(3);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(317);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,14,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(315);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,13,_ctx) ) {
					case 1:
						{
						_localctx = new TargetTypePatternContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_targetTypePattern);
						setState(309);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(310);
						match(AND);
						setState(311);
						targetTypePattern(3);
						}
						break;
					case 2:
						{
						_localctx = new TargetTypePatternContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_targetTypePattern);
						setState(312);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(313);
						match(OR);
						setState(314);
						targetTypePattern(2);
						}
						break;
					}
					} 
				}
				setState(319);
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
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class FormalTypePatternContext extends ParserRuleContext {
		public ClassNameOrInterfaceContext classNameOrInterface() {
			return getRuleContext(ClassNameOrInterfaceContext.class,0);
		}
		public PrimitiveTypeContext primitiveType() {
			return getRuleContext(PrimitiveTypeContext.class,0);
		}
		public TerminalNode BANG() { return getToken(RefactorMethodSignatureParser.BANG, 0); }
		public List<FormalTypePatternContext> formalTypePattern() {
			return getRuleContexts(FormalTypePatternContext.class);
		}
		public FormalTypePatternContext formalTypePattern(int i) {
			return getRuleContext(FormalTypePatternContext.class,i);
		}
		public TerminalNode AND() { return getToken(RefactorMethodSignatureParser.AND, 0); }
		public TerminalNode OR() { return getToken(RefactorMethodSignatureParser.OR, 0); }
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
			setState(325);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DOTDOT:
			case DOT:
			case MUL:
			case Identifier:
				{
				setState(321);
				classNameOrInterface();
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
				setState(322);
				primitiveType();
				}
				break;
			case BANG:
				{
				setState(323);
				match(BANG);
				setState(324);
				formalTypePattern(3);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(335);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,17,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(333);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,16,_ctx) ) {
					case 1:
						{
						_localctx = new FormalTypePatternContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_formalTypePattern);
						setState(327);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(328);
						match(AND);
						setState(329);
						formalTypePattern(3);
						}
						break;
					case 2:
						{
						_localctx = new FormalTypePatternContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_formalTypePattern);
						setState(330);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(331);
						match(OR);
						setState(332);
						formalTypePattern(2);
						}
						break;
					}
					} 
				}
				setState(337);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,17,_ctx);
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
		public List<TerminalNode> LBRACK() { return getTokens(RefactorMethodSignatureParser.LBRACK); }
		public TerminalNode LBRACK(int i) {
			return getToken(RefactorMethodSignatureParser.LBRACK, i);
		}
		public List<TerminalNode> RBRACK() { return getTokens(RefactorMethodSignatureParser.RBRACK); }
		public TerminalNode RBRACK(int i) {
			return getToken(RefactorMethodSignatureParser.RBRACK, i);
		}
		public List<TerminalNode> Identifier() { return getTokens(RefactorMethodSignatureParser.Identifier); }
		public TerminalNode Identifier(int i) {
			return getToken(RefactorMethodSignatureParser.Identifier, i);
		}
		public List<TerminalNode> MUL() { return getTokens(RefactorMethodSignatureParser.MUL); }
		public TerminalNode MUL(int i) {
			return getToken(RefactorMethodSignatureParser.MUL, i);
		}
		public List<TerminalNode> DOT() { return getTokens(RefactorMethodSignatureParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(RefactorMethodSignatureParser.DOT, i);
		}
		public List<TerminalNode> DOTDOT() { return getTokens(RefactorMethodSignatureParser.DOTDOT); }
		public TerminalNode DOTDOT(int i) {
			return getToken(RefactorMethodSignatureParser.DOTDOT, i);
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
			setState(339); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(338);
					_la = _input.LA(1);
					if ( !(_la==DOTDOT || ((((_la - 68)) & ~0x3f) == 0 && ((1L << (_la - 68)) & ((1L << (DOT - 68)) | (1L << (MUL - 68)) | (1L << (Identifier - 68)))) != 0)) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(341); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,18,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
			setState(347);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,19,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(343);
					match(LBRACK);
					setState(344);
					match(RBRACK);
					}
					} 
				}
				setState(349);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,19,_ctx);
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
		public List<TerminalNode> MUL() { return getTokens(RefactorMethodSignatureParser.MUL); }
		public TerminalNode MUL(int i) {
			return getToken(RefactorMethodSignatureParser.MUL, i);
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
			setState(372);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(350);
				match(Identifier);
				setState(355);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,20,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(351);
						match(MUL);
						setState(352);
						match(Identifier);
						}
						} 
					}
					setState(357);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,20,_ctx);
				}
				setState(359);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MUL) {
					{
					setState(358);
					match(MUL);
					}
				}

				}
				break;
			case MUL:
				enterOuterAlt(_localctx, 2);
				{
				setState(361);
				match(MUL);
				setState(366);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,22,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(362);
						match(Identifier);
						setState(363);
						match(MUL);
						}
						} 
					}
					setState(368);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,22,_ctx);
				}
				setState(370);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Identifier) {
					{
					setState(369);
					match(Identifier);
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
		public TerminalNode EOF() { return getToken(RefactorMethodSignatureParser.EOF, 0); }
		public PackageDeclarationContext packageDeclaration() {
			return getRuleContext(PackageDeclarationContext.class,0);
		}
		public List<ImportDeclarationContext> importDeclaration() {
			return getRuleContexts(ImportDeclarationContext.class);
		}
		public ImportDeclarationContext importDeclaration(int i) {
			return getRuleContext(ImportDeclarationContext.class,i);
		}
		public List<TypeDeclarationContext> typeDeclaration() {
			return getRuleContexts(TypeDeclarationContext.class);
		}
		public TypeDeclarationContext typeDeclaration(int i) {
			return getRuleContext(TypeDeclarationContext.class,i);
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
			setState(375);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,25,_ctx) ) {
			case 1:
				{
				setState(374);
				packageDeclaration();
				}
				break;
			}
			setState(380);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==IMPORT) {
				{
				{
				setState(377);
				importDeclaration();
				}
				}
				setState(382);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(386);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ABSTRACT) | (1L << CLASS) | (1L << ENUM) | (1L << FINAL) | (1L << INTERFACE) | (1L << PRIVATE) | (1L << PROTECTED) | (1L << PUBLIC) | (1L << STATIC) | (1L << STRICTFP))) != 0) || _la==SEMI || _la==AT) {
				{
				{
				setState(383);
				typeDeclaration();
				}
				}
				setState(388);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(389);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
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
		public TerminalNode PACKAGE() { return getToken(RefactorMethodSignatureParser.PACKAGE, 0); }
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public TerminalNode SEMI() { return getToken(RefactorMethodSignatureParser.SEMI, 0); }
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
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
			setState(394);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT) {
				{
				{
				setState(391);
				annotation();
				}
				}
				setState(396);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(397);
			match(PACKAGE);
			setState(398);
			qualifiedName();
			setState(399);
			match(SEMI);
			}
		}
		catch (RecognitionException re) {
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
		public TerminalNode IMPORT() { return getToken(RefactorMethodSignatureParser.IMPORT, 0); }
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public TerminalNode SEMI() { return getToken(RefactorMethodSignatureParser.SEMI, 0); }
		public TerminalNode STATIC() { return getToken(RefactorMethodSignatureParser.STATIC, 0); }
		public TerminalNode DOT() { return getToken(RefactorMethodSignatureParser.DOT, 0); }
		public TerminalNode MUL() { return getToken(RefactorMethodSignatureParser.MUL, 0); }
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
			setState(401);
			match(IMPORT);
			setState(403);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==STATIC) {
				{
				setState(402);
				match(STATIC);
				}
			}

			setState(405);
			qualifiedName();
			setState(408);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==DOT) {
				{
				setState(406);
				match(DOT);
				setState(407);
				match(MUL);
				}
			}

			setState(410);
			match(SEMI);
			}
		}
		catch (RecognitionException re) {
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
		public ClassDeclarationContext classDeclaration() {
			return getRuleContext(ClassDeclarationContext.class,0);
		}
		public List<ClassOrInterfaceModifierContext> classOrInterfaceModifier() {
			return getRuleContexts(ClassOrInterfaceModifierContext.class);
		}
		public ClassOrInterfaceModifierContext classOrInterfaceModifier(int i) {
			return getRuleContext(ClassOrInterfaceModifierContext.class,i);
		}
		public EnumDeclarationContext enumDeclaration() {
			return getRuleContext(EnumDeclarationContext.class,0);
		}
		public InterfaceDeclarationContext interfaceDeclaration() {
			return getRuleContext(InterfaceDeclarationContext.class,0);
		}
		public AnnotationTypeDeclarationContext annotationTypeDeclaration() {
			return getRuleContext(AnnotationTypeDeclarationContext.class,0);
		}
		public TerminalNode SEMI() { return getToken(RefactorMethodSignatureParser.SEMI, 0); }
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
			setState(441);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,35,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(415);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ABSTRACT) | (1L << FINAL) | (1L << PRIVATE) | (1L << PROTECTED) | (1L << PUBLIC) | (1L << STATIC) | (1L << STRICTFP))) != 0) || _la==AT) {
					{
					{
					setState(412);
					classOrInterfaceModifier();
					}
					}
					setState(417);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(418);
				classDeclaration();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(422);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ABSTRACT) | (1L << FINAL) | (1L << PRIVATE) | (1L << PROTECTED) | (1L << PUBLIC) | (1L << STATIC) | (1L << STRICTFP))) != 0) || _la==AT) {
					{
					{
					setState(419);
					classOrInterfaceModifier();
					}
					}
					setState(424);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(425);
				enumDeclaration();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(429);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ABSTRACT) | (1L << FINAL) | (1L << PRIVATE) | (1L << PROTECTED) | (1L << PUBLIC) | (1L << STATIC) | (1L << STRICTFP))) != 0) || _la==AT) {
					{
					{
					setState(426);
					classOrInterfaceModifier();
					}
					}
					setState(431);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(432);
				interfaceDeclaration();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(436);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,34,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(433);
						classOrInterfaceModifier();
						}
						} 
					}
					setState(438);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,34,_ctx);
				}
				setState(439);
				annotationTypeDeclaration();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(440);
				match(SEMI);
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
		public TerminalNode NATIVE() { return getToken(RefactorMethodSignatureParser.NATIVE, 0); }
		public TerminalNode SYNCHRONIZED() { return getToken(RefactorMethodSignatureParser.SYNCHRONIZED, 0); }
		public TerminalNode TRANSIENT() { return getToken(RefactorMethodSignatureParser.TRANSIENT, 0); }
		public TerminalNode VOLATILE() { return getToken(RefactorMethodSignatureParser.VOLATILE, 0); }
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
			setState(445);
			_errHandler.sync(this);
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
				setState(443);
				classOrInterfaceModifier();
				}
				break;
			case NATIVE:
			case SYNCHRONIZED:
			case TRANSIENT:
			case VOLATILE:
				enterOuterAlt(_localctx, 2);
				{
				setState(444);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << NATIVE) | (1L << SYNCHRONIZED) | (1L << TRANSIENT) | (1L << VOLATILE))) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
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

	public static class ClassOrInterfaceModifierContext extends ParserRuleContext {
		public AnnotationContext annotation() {
			return getRuleContext(AnnotationContext.class,0);
		}
		public TerminalNode PUBLIC() { return getToken(RefactorMethodSignatureParser.PUBLIC, 0); }
		public TerminalNode PROTECTED() { return getToken(RefactorMethodSignatureParser.PROTECTED, 0); }
		public TerminalNode PRIVATE() { return getToken(RefactorMethodSignatureParser.PRIVATE, 0); }
		public TerminalNode STATIC() { return getToken(RefactorMethodSignatureParser.STATIC, 0); }
		public TerminalNode ABSTRACT() { return getToken(RefactorMethodSignatureParser.ABSTRACT, 0); }
		public TerminalNode FINAL() { return getToken(RefactorMethodSignatureParser.FINAL, 0); }
		public TerminalNode STRICTFP() { return getToken(RefactorMethodSignatureParser.STRICTFP, 0); }
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
			setState(449);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case AT:
				enterOuterAlt(_localctx, 1);
				{
				setState(447);
				annotation();
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
				setState(448);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ABSTRACT) | (1L << FINAL) | (1L << PRIVATE) | (1L << PROTECTED) | (1L << PUBLIC) | (1L << STATIC) | (1L << STRICTFP))) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
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

	public static class VariableModifierContext extends ParserRuleContext {
		public TerminalNode FINAL() { return getToken(RefactorMethodSignatureParser.FINAL, 0); }
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
			setState(453);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case FINAL:
				enterOuterAlt(_localctx, 1);
				{
				setState(451);
				match(FINAL);
				}
				break;
			case AT:
				enterOuterAlt(_localctx, 2);
				{
				setState(452);
				annotation();
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
		public TerminalNode CLASS() { return getToken(RefactorMethodSignatureParser.CLASS, 0); }
		public TerminalNode Identifier() { return getToken(RefactorMethodSignatureParser.Identifier, 0); }
		public ClassBodyContext classBody() {
			return getRuleContext(ClassBodyContext.class,0);
		}
		public TypeParametersContext typeParameters() {
			return getRuleContext(TypeParametersContext.class,0);
		}
		public TerminalNode EXTENDS() { return getToken(RefactorMethodSignatureParser.EXTENDS, 0); }
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TerminalNode IMPLEMENTS() { return getToken(RefactorMethodSignatureParser.IMPLEMENTS, 0); }
		public TypeListContext typeList() {
			return getRuleContext(TypeListContext.class,0);
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
			setState(455);
			match(CLASS);
			setState(456);
			match(Identifier);
			setState(458);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(457);
				typeParameters();
				}
			}

			setState(462);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==EXTENDS) {
				{
				setState(460);
				match(EXTENDS);
				setState(461);
				type();
				}
			}

			setState(466);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==IMPLEMENTS) {
				{
				setState(464);
				match(IMPLEMENTS);
				setState(465);
				typeList();
				}
			}

			setState(468);
			classBody();
			}
		}
		catch (RecognitionException re) {
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
		public TerminalNode LT() { return getToken(RefactorMethodSignatureParser.LT, 0); }
		public List<TypeParameterContext> typeParameter() {
			return getRuleContexts(TypeParameterContext.class);
		}
		public TypeParameterContext typeParameter(int i) {
			return getRuleContext(TypeParameterContext.class,i);
		}
		public TerminalNode GT() { return getToken(RefactorMethodSignatureParser.GT, 0); }
		public List<TerminalNode> COMMA() { return getTokens(RefactorMethodSignatureParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(RefactorMethodSignatureParser.COMMA, i);
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
			setState(470);
			match(LT);
			setState(471);
			typeParameter();
			setState(476);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(472);
				match(COMMA);
				setState(473);
				typeParameter();
				}
				}
				setState(478);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(479);
			match(GT);
			}
		}
		catch (RecognitionException re) {
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
		public TerminalNode EXTENDS() { return getToken(RefactorMethodSignatureParser.EXTENDS, 0); }
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
			setState(481);
			match(Identifier);
			setState(484);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==EXTENDS) {
				{
				setState(482);
				match(EXTENDS);
				setState(483);
				typeBound();
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
		public List<TypeContext> type() {
			return getRuleContexts(TypeContext.class);
		}
		public TypeContext type(int i) {
			return getRuleContext(TypeContext.class,i);
		}
		public List<TerminalNode> BITAND() { return getTokens(RefactorMethodSignatureParser.BITAND); }
		public TerminalNode BITAND(int i) {
			return getToken(RefactorMethodSignatureParser.BITAND, i);
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
			setState(486);
			type();
			setState(491);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==BITAND) {
				{
				{
				setState(487);
				match(BITAND);
				setState(488);
				type();
				}
				}
				setState(493);
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
		public TerminalNode ENUM() { return getToken(RefactorMethodSignatureParser.ENUM, 0); }
		public TerminalNode Identifier() { return getToken(RefactorMethodSignatureParser.Identifier, 0); }
		public TerminalNode LBRACE() { return getToken(RefactorMethodSignatureParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(RefactorMethodSignatureParser.RBRACE, 0); }
		public TerminalNode IMPLEMENTS() { return getToken(RefactorMethodSignatureParser.IMPLEMENTS, 0); }
		public TypeListContext typeList() {
			return getRuleContext(TypeListContext.class,0);
		}
		public EnumConstantsContext enumConstants() {
			return getRuleContext(EnumConstantsContext.class,0);
		}
		public TerminalNode COMMA() { return getToken(RefactorMethodSignatureParser.COMMA, 0); }
		public EnumBodyDeclarationsContext enumBodyDeclarations() {
			return getRuleContext(EnumBodyDeclarationsContext.class,0);
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
			setState(494);
			match(ENUM);
			setState(495);
			match(Identifier);
			setState(498);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==IMPLEMENTS) {
				{
				setState(496);
				match(IMPLEMENTS);
				setState(497);
				typeList();
				}
			}

			setState(500);
			match(LBRACE);
			setState(502);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==Identifier || _la==AT) {
				{
				setState(501);
				enumConstants();
				}
			}

			setState(505);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(504);
				match(COMMA);
				}
			}

			setState(508);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SEMI) {
				{
				setState(507);
				enumBodyDeclarations();
				}
			}

			setState(510);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
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
		public List<TerminalNode> COMMA() { return getTokens(RefactorMethodSignatureParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(RefactorMethodSignatureParser.COMMA, i);
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
			setState(512);
			enumConstant();
			setState(517);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,49,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(513);
					match(COMMA);
					setState(514);
					enumConstant();
					}
					} 
				}
				setState(519);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,49,_ctx);
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
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public ArgumentsContext arguments() {
			return getRuleContext(ArgumentsContext.class,0);
		}
		public ClassBodyContext classBody() {
			return getRuleContext(ClassBodyContext.class,0);
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
			setState(523);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT) {
				{
				{
				setState(520);
				annotation();
				}
				}
				setState(525);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(526);
			match(Identifier);
			setState(528);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LPAREN) {
				{
				setState(527);
				arguments();
				}
			}

			setState(531);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LBRACE) {
				{
				setState(530);
				classBody();
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
		public TerminalNode SEMI() { return getToken(RefactorMethodSignatureParser.SEMI, 0); }
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
			setState(533);
			match(SEMI);
			setState(537);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ABSTRACT) | (1L << BOOLEAN) | (1L << BYTE) | (1L << CHAR) | (1L << CLASS) | (1L << DOUBLE) | (1L << ENUM) | (1L << FINAL) | (1L << FLOAT) | (1L << INT) | (1L << INTERFACE) | (1L << LONG) | (1L << NATIVE) | (1L << PRIVATE) | (1L << PROTECTED) | (1L << PUBLIC) | (1L << SHORT) | (1L << STATIC) | (1L << STRICTFP) | (1L << SYNCHRONIZED) | (1L << TRANSIENT) | (1L << VOID) | (1L << VOLATILE) | (1L << LBRACE))) != 0) || ((((_la - 66)) & ~0x3f) == 0 && ((1L << (_la - 66)) & ((1L << (SEMI - 66)) | (1L << (LT - 66)) | (1L << (Identifier - 66)) | (1L << (AT - 66)))) != 0)) {
				{
				{
				setState(534);
				classBodyDeclaration();
				}
				}
				setState(539);
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
		public TerminalNode INTERFACE() { return getToken(RefactorMethodSignatureParser.INTERFACE, 0); }
		public TerminalNode Identifier() { return getToken(RefactorMethodSignatureParser.Identifier, 0); }
		public InterfaceBodyContext interfaceBody() {
			return getRuleContext(InterfaceBodyContext.class,0);
		}
		public TypeParametersContext typeParameters() {
			return getRuleContext(TypeParametersContext.class,0);
		}
		public TerminalNode EXTENDS() { return getToken(RefactorMethodSignatureParser.EXTENDS, 0); }
		public TypeListContext typeList() {
			return getRuleContext(TypeListContext.class,0);
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
			setState(540);
			match(INTERFACE);
			setState(541);
			match(Identifier);
			setState(543);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(542);
				typeParameters();
				}
			}

			setState(547);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==EXTENDS) {
				{
				setState(545);
				match(EXTENDS);
				setState(546);
				typeList();
				}
			}

			setState(549);
			interfaceBody();
			}
		}
		catch (RecognitionException re) {
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
		public List<TypeContext> type() {
			return getRuleContexts(TypeContext.class);
		}
		public TypeContext type(int i) {
			return getRuleContext(TypeContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(RefactorMethodSignatureParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(RefactorMethodSignatureParser.COMMA, i);
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
			setState(551);
			type();
			setState(556);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(552);
				match(COMMA);
				setState(553);
				type();
				}
				}
				setState(558);
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
		public TerminalNode LBRACE() { return getToken(RefactorMethodSignatureParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(RefactorMethodSignatureParser.RBRACE, 0); }
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
			setState(559);
			match(LBRACE);
			setState(563);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ABSTRACT) | (1L << BOOLEAN) | (1L << BYTE) | (1L << CHAR) | (1L << CLASS) | (1L << DOUBLE) | (1L << ENUM) | (1L << FINAL) | (1L << FLOAT) | (1L << INT) | (1L << INTERFACE) | (1L << LONG) | (1L << NATIVE) | (1L << PRIVATE) | (1L << PROTECTED) | (1L << PUBLIC) | (1L << SHORT) | (1L << STATIC) | (1L << STRICTFP) | (1L << SYNCHRONIZED) | (1L << TRANSIENT) | (1L << VOID) | (1L << VOLATILE) | (1L << LBRACE))) != 0) || ((((_la - 66)) & ~0x3f) == 0 && ((1L << (_la - 66)) & ((1L << (SEMI - 66)) | (1L << (LT - 66)) | (1L << (Identifier - 66)) | (1L << (AT - 66)))) != 0)) {
				{
				{
				setState(560);
				classBodyDeclaration();
				}
				}
				setState(565);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(566);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
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
		public TerminalNode LBRACE() { return getToken(RefactorMethodSignatureParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(RefactorMethodSignatureParser.RBRACE, 0); }
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
			setState(568);
			match(LBRACE);
			setState(572);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ABSTRACT) | (1L << BOOLEAN) | (1L << BYTE) | (1L << CHAR) | (1L << CLASS) | (1L << DOUBLE) | (1L << ENUM) | (1L << FINAL) | (1L << FLOAT) | (1L << INT) | (1L << INTERFACE) | (1L << LONG) | (1L << NATIVE) | (1L << PRIVATE) | (1L << PROTECTED) | (1L << PUBLIC) | (1L << SHORT) | (1L << STATIC) | (1L << STRICTFP) | (1L << SYNCHRONIZED) | (1L << TRANSIENT) | (1L << VOID) | (1L << VOLATILE))) != 0) || ((((_la - 66)) & ~0x3f) == 0 && ((1L << (_la - 66)) & ((1L << (SEMI - 66)) | (1L << (LT - 66)) | (1L << (Identifier - 66)) | (1L << (AT - 66)))) != 0)) {
				{
				{
				setState(569);
				interfaceBodyDeclaration();
				}
				}
				setState(574);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(575);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
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
		public TerminalNode SEMI() { return getToken(RefactorMethodSignatureParser.SEMI, 0); }
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public TerminalNode STATIC() { return getToken(RefactorMethodSignatureParser.STATIC, 0); }
		public MemberDeclarationContext memberDeclaration() {
			return getRuleContext(MemberDeclarationContext.class,0);
		}
		public List<ModifierContext> modifier() {
			return getRuleContexts(ModifierContext.class);
		}
		public ModifierContext modifier(int i) {
			return getRuleContext(ModifierContext.class,i);
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
			setState(589);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,61,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(577);
				match(SEMI);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(579);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==STATIC) {
					{
					setState(578);
					match(STATIC);
					}
				}

				setState(581);
				block();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(585);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,60,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(582);
						modifier();
						}
						} 
					}
					setState(587);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,60,_ctx);
				}
				setState(588);
				memberDeclaration();
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
		public MethodDeclarationContext methodDeclaration() {
			return getRuleContext(MethodDeclarationContext.class,0);
		}
		public GenericMethodDeclarationContext genericMethodDeclaration() {
			return getRuleContext(GenericMethodDeclarationContext.class,0);
		}
		public FieldDeclarationContext fieldDeclaration() {
			return getRuleContext(FieldDeclarationContext.class,0);
		}
		public ConstructorDeclarationContext constructorDeclaration() {
			return getRuleContext(ConstructorDeclarationContext.class,0);
		}
		public GenericConstructorDeclarationContext genericConstructorDeclaration() {
			return getRuleContext(GenericConstructorDeclarationContext.class,0);
		}
		public InterfaceDeclarationContext interfaceDeclaration() {
			return getRuleContext(InterfaceDeclarationContext.class,0);
		}
		public AnnotationTypeDeclarationContext annotationTypeDeclaration() {
			return getRuleContext(AnnotationTypeDeclarationContext.class,0);
		}
		public ClassDeclarationContext classDeclaration() {
			return getRuleContext(ClassDeclarationContext.class,0);
		}
		public EnumDeclarationContext enumDeclaration() {
			return getRuleContext(EnumDeclarationContext.class,0);
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
			setState(600);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,62,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(591);
				methodDeclaration();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(592);
				genericMethodDeclaration();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(593);
				fieldDeclaration();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(594);
				constructorDeclaration();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(595);
				genericConstructorDeclaration();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(596);
				interfaceDeclaration();
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(597);
				annotationTypeDeclaration();
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(598);
				classDeclaration();
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(599);
				enumDeclaration();
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
		public FormalParametersContext formalParameters() {
			return getRuleContext(FormalParametersContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TerminalNode VOID() { return getToken(RefactorMethodSignatureParser.VOID, 0); }
		public MethodBodyContext methodBody() {
			return getRuleContext(MethodBodyContext.class,0);
		}
		public TerminalNode SEMI() { return getToken(RefactorMethodSignatureParser.SEMI, 0); }
		public List<TerminalNode> LBRACK() { return getTokens(RefactorMethodSignatureParser.LBRACK); }
		public TerminalNode LBRACK(int i) {
			return getToken(RefactorMethodSignatureParser.LBRACK, i);
		}
		public List<TerminalNode> RBRACK() { return getTokens(RefactorMethodSignatureParser.RBRACK); }
		public TerminalNode RBRACK(int i) {
			return getToken(RefactorMethodSignatureParser.RBRACK, i);
		}
		public TerminalNode THROWS() { return getToken(RefactorMethodSignatureParser.THROWS, 0); }
		public QualifiedNameListContext qualifiedNameList() {
			return getRuleContext(QualifiedNameListContext.class,0);
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
			setState(604);
			_errHandler.sync(this);
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
				setState(602);
				type();
				}
				break;
			case VOID:
				{
				setState(603);
				match(VOID);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(606);
			match(Identifier);
			setState(607);
			formalParameters();
			setState(612);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==LBRACK) {
				{
				{
				setState(608);
				match(LBRACK);
				setState(609);
				match(RBRACK);
				}
				}
				setState(614);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(617);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==THROWS) {
				{
				setState(615);
				match(THROWS);
				setState(616);
				qualifiedNameList();
				}
			}

			setState(621);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LBRACE:
				{
				setState(619);
				methodBody();
				}
				break;
			case SEMI:
				{
				setState(620);
				match(SEMI);
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
		public TypeParametersContext typeParameters() {
			return getRuleContext(TypeParametersContext.class,0);
		}
		public MethodDeclarationContext methodDeclaration() {
			return getRuleContext(MethodDeclarationContext.class,0);
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
			setState(623);
			typeParameters();
			setState(624);
			methodDeclaration();
			}
		}
		catch (RecognitionException re) {
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
		public FormalParametersContext formalParameters() {
			return getRuleContext(FormalParametersContext.class,0);
		}
		public ConstructorBodyContext constructorBody() {
			return getRuleContext(ConstructorBodyContext.class,0);
		}
		public TerminalNode THROWS() { return getToken(RefactorMethodSignatureParser.THROWS, 0); }
		public QualifiedNameListContext qualifiedNameList() {
			return getRuleContext(QualifiedNameListContext.class,0);
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
			setState(626);
			match(Identifier);
			setState(627);
			formalParameters();
			setState(630);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==THROWS) {
				{
				setState(628);
				match(THROWS);
				setState(629);
				qualifiedNameList();
				}
			}

			setState(632);
			constructorBody();
			}
		}
		catch (RecognitionException re) {
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
			setState(634);
			typeParameters();
			setState(635);
			constructorDeclaration();
			}
		}
		catch (RecognitionException re) {
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
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public VariableDeclaratorsContext variableDeclarators() {
			return getRuleContext(VariableDeclaratorsContext.class,0);
		}
		public TerminalNode SEMI() { return getToken(RefactorMethodSignatureParser.SEMI, 0); }
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
			setState(637);
			type();
			setState(638);
			variableDeclarators();
			setState(639);
			match(SEMI);
			}
		}
		catch (RecognitionException re) {
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
		public InterfaceMemberDeclarationContext interfaceMemberDeclaration() {
			return getRuleContext(InterfaceMemberDeclarationContext.class,0);
		}
		public List<ModifierContext> modifier() {
			return getRuleContexts(ModifierContext.class);
		}
		public ModifierContext modifier(int i) {
			return getRuleContext(ModifierContext.class,i);
		}
		public TerminalNode SEMI() { return getToken(RefactorMethodSignatureParser.SEMI, 0); }
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
			setState(649);
			_errHandler.sync(this);
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
				setState(644);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,68,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(641);
						modifier();
						}
						} 
					}
					setState(646);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,68,_ctx);
				}
				setState(647);
				interfaceMemberDeclaration();
				}
				break;
			case SEMI:
				enterOuterAlt(_localctx, 2);
				{
				setState(648);
				match(SEMI);
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
		public ConstDeclarationContext constDeclaration() {
			return getRuleContext(ConstDeclarationContext.class,0);
		}
		public InterfaceMethodDeclarationContext interfaceMethodDeclaration() {
			return getRuleContext(InterfaceMethodDeclarationContext.class,0);
		}
		public GenericInterfaceMethodDeclarationContext genericInterfaceMethodDeclaration() {
			return getRuleContext(GenericInterfaceMethodDeclarationContext.class,0);
		}
		public InterfaceDeclarationContext interfaceDeclaration() {
			return getRuleContext(InterfaceDeclarationContext.class,0);
		}
		public AnnotationTypeDeclarationContext annotationTypeDeclaration() {
			return getRuleContext(AnnotationTypeDeclarationContext.class,0);
		}
		public ClassDeclarationContext classDeclaration() {
			return getRuleContext(ClassDeclarationContext.class,0);
		}
		public EnumDeclarationContext enumDeclaration() {
			return getRuleContext(EnumDeclarationContext.class,0);
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
			setState(658);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,70,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(651);
				constDeclaration();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(652);
				interfaceMethodDeclaration();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(653);
				genericInterfaceMethodDeclaration();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(654);
				interfaceDeclaration();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(655);
				annotationTypeDeclaration();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(656);
				classDeclaration();
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(657);
				enumDeclaration();
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
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public List<ConstantDeclaratorContext> constantDeclarator() {
			return getRuleContexts(ConstantDeclaratorContext.class);
		}
		public ConstantDeclaratorContext constantDeclarator(int i) {
			return getRuleContext(ConstantDeclaratorContext.class,i);
		}
		public TerminalNode SEMI() { return getToken(RefactorMethodSignatureParser.SEMI, 0); }
		public List<TerminalNode> COMMA() { return getTokens(RefactorMethodSignatureParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(RefactorMethodSignatureParser.COMMA, i);
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
			setState(660);
			type();
			setState(661);
			constantDeclarator();
			setState(666);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(662);
				match(COMMA);
				setState(663);
				constantDeclarator();
				}
				}
				setState(668);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(669);
			match(SEMI);
			}
		}
		catch (RecognitionException re) {
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
		public TerminalNode ASSIGN() { return getToken(RefactorMethodSignatureParser.ASSIGN, 0); }
		public VariableInitializerContext variableInitializer() {
			return getRuleContext(VariableInitializerContext.class,0);
		}
		public List<TerminalNode> LBRACK() { return getTokens(RefactorMethodSignatureParser.LBRACK); }
		public TerminalNode LBRACK(int i) {
			return getToken(RefactorMethodSignatureParser.LBRACK, i);
		}
		public List<TerminalNode> RBRACK() { return getTokens(RefactorMethodSignatureParser.RBRACK); }
		public TerminalNode RBRACK(int i) {
			return getToken(RefactorMethodSignatureParser.RBRACK, i);
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
			setState(671);
			match(Identifier);
			setState(676);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==LBRACK) {
				{
				{
				setState(672);
				match(LBRACK);
				setState(673);
				match(RBRACK);
				}
				}
				setState(678);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(679);
			match(ASSIGN);
			setState(680);
			variableInitializer();
			}
		}
		catch (RecognitionException re) {
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
		public FormalParametersContext formalParameters() {
			return getRuleContext(FormalParametersContext.class,0);
		}
		public TerminalNode SEMI() { return getToken(RefactorMethodSignatureParser.SEMI, 0); }
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TerminalNode VOID() { return getToken(RefactorMethodSignatureParser.VOID, 0); }
		public List<TerminalNode> LBRACK() { return getTokens(RefactorMethodSignatureParser.LBRACK); }
		public TerminalNode LBRACK(int i) {
			return getToken(RefactorMethodSignatureParser.LBRACK, i);
		}
		public List<TerminalNode> RBRACK() { return getTokens(RefactorMethodSignatureParser.RBRACK); }
		public TerminalNode RBRACK(int i) {
			return getToken(RefactorMethodSignatureParser.RBRACK, i);
		}
		public TerminalNode THROWS() { return getToken(RefactorMethodSignatureParser.THROWS, 0); }
		public QualifiedNameListContext qualifiedNameList() {
			return getRuleContext(QualifiedNameListContext.class,0);
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
			setState(684);
			_errHandler.sync(this);
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
				setState(682);
				type();
				}
				break;
			case VOID:
				{
				setState(683);
				match(VOID);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(686);
			match(Identifier);
			setState(687);
			formalParameters();
			setState(692);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==LBRACK) {
				{
				{
				setState(688);
				match(LBRACK);
				setState(689);
				match(RBRACK);
				}
				}
				setState(694);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(697);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==THROWS) {
				{
				setState(695);
				match(THROWS);
				setState(696);
				qualifiedNameList();
				}
			}

			setState(699);
			match(SEMI);
			}
		}
		catch (RecognitionException re) {
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
			setState(701);
			typeParameters();
			setState(702);
			interfaceMethodDeclaration();
			}
		}
		catch (RecognitionException re) {
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
		public List<TerminalNode> COMMA() { return getTokens(RefactorMethodSignatureParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(RefactorMethodSignatureParser.COMMA, i);
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
			setState(704);
			variableDeclarator();
			setState(709);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(705);
				match(COMMA);
				setState(706);
				variableDeclarator();
				}
				}
				setState(711);
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
		public VariableDeclaratorIdContext variableDeclaratorId() {
			return getRuleContext(VariableDeclaratorIdContext.class,0);
		}
		public TerminalNode ASSIGN() { return getToken(RefactorMethodSignatureParser.ASSIGN, 0); }
		public VariableInitializerContext variableInitializer() {
			return getRuleContext(VariableInitializerContext.class,0);
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
			setState(712);
			variableDeclaratorId();
			setState(715);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ASSIGN) {
				{
				setState(713);
				match(ASSIGN);
				setState(714);
				variableInitializer();
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
		public List<TerminalNode> LBRACK() { return getTokens(RefactorMethodSignatureParser.LBRACK); }
		public TerminalNode LBRACK(int i) {
			return getToken(RefactorMethodSignatureParser.LBRACK, i);
		}
		public List<TerminalNode> RBRACK() { return getTokens(RefactorMethodSignatureParser.RBRACK); }
		public TerminalNode RBRACK(int i) {
			return getToken(RefactorMethodSignatureParser.RBRACK, i);
		}
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
			setState(717);
			match(Identifier);
			setState(722);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==LBRACK) {
				{
				{
				setState(718);
				match(LBRACK);
				setState(719);
				match(RBRACK);
				}
				}
				setState(724);
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
			setState(727);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LBRACE:
				enterOuterAlt(_localctx, 1);
				{
				setState(725);
				arrayInitializer();
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
				setState(726);
				expression(0);
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
		public TerminalNode LBRACE() { return getToken(RefactorMethodSignatureParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(RefactorMethodSignatureParser.RBRACE, 0); }
		public List<VariableInitializerContext> variableInitializer() {
			return getRuleContexts(VariableInitializerContext.class);
		}
		public VariableInitializerContext variableInitializer(int i) {
			return getRuleContext(VariableInitializerContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(RefactorMethodSignatureParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(RefactorMethodSignatureParser.COMMA, i);
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
			setState(729);
			match(LBRACE);
			setState(741);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << BOOLEAN) | (1L << BYTE) | (1L << CHAR) | (1L << DOUBLE) | (1L << FLOAT) | (1L << INT) | (1L << LONG) | (1L << NEW) | (1L << SHORT) | (1L << SUPER) | (1L << THIS) | (1L << VOID) | (1L << IntegerLiteral) | (1L << FloatingPointLiteral) | (1L << BooleanLiteral) | (1L << CharacterLiteral) | (1L << StringLiteral) | (1L << NullLiteral) | (1L << LPAREN) | (1L << LBRACE))) != 0) || ((((_la - 71)) & ~0x3f) == 0 && ((1L << (_la - 71)) & ((1L << (LT - 71)) | (1L << (BANG - 71)) | (1L << (TILDE - 71)) | (1L << (INC - 71)) | (1L << (DEC - 71)) | (1L << (ADD - 71)) | (1L << (SUB - 71)) | (1L << (Identifier - 71)))) != 0)) {
				{
				setState(730);
				variableInitializer();
				setState(735);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,80,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(731);
						match(COMMA);
						setState(732);
						variableInitializer();
						}
						} 
					}
					setState(737);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,80,_ctx);
				}
				setState(739);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==COMMA) {
					{
					setState(738);
					match(COMMA);
					}
				}

				}
			}

			setState(743);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
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
			setState(745);
			match(Identifier);
			}
		}
		catch (RecognitionException re) {
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
		public ClassOrInterfaceTypeContext classOrInterfaceType() {
			return getRuleContext(ClassOrInterfaceTypeContext.class,0);
		}
		public List<TerminalNode> LBRACK() { return getTokens(RefactorMethodSignatureParser.LBRACK); }
		public TerminalNode LBRACK(int i) {
			return getToken(RefactorMethodSignatureParser.LBRACK, i);
		}
		public List<TerminalNode> RBRACK() { return getTokens(RefactorMethodSignatureParser.RBRACK); }
		public TerminalNode RBRACK(int i) {
			return getToken(RefactorMethodSignatureParser.RBRACK, i);
		}
		public PrimitiveTypeContext primitiveType() {
			return getRuleContext(PrimitiveTypeContext.class,0);
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
			setState(763);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(747);
				classOrInterfaceType();
				setState(752);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,83,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(748);
						match(LBRACK);
						setState(749);
						match(RBRACK);
						}
						} 
					}
					setState(754);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,83,_ctx);
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
				setState(755);
				primitiveType();
				setState(760);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,84,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(756);
						match(LBRACK);
						setState(757);
						match(RBRACK);
						}
						} 
					}
					setState(762);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,84,_ctx);
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
		public List<TerminalNode> Identifier() { return getTokens(RefactorMethodSignatureParser.Identifier); }
		public TerminalNode Identifier(int i) {
			return getToken(RefactorMethodSignatureParser.Identifier, i);
		}
		public List<TypeArgumentsContext> typeArguments() {
			return getRuleContexts(TypeArgumentsContext.class);
		}
		public TypeArgumentsContext typeArguments(int i) {
			return getRuleContext(TypeArgumentsContext.class,i);
		}
		public List<TerminalNode> DOT() { return getTokens(RefactorMethodSignatureParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(RefactorMethodSignatureParser.DOT, i);
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
			setState(765);
			match(Identifier);
			setState(767);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,86,_ctx) ) {
			case 1:
				{
				setState(766);
				typeArguments();
				}
				break;
			}
			setState(776);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,88,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(769);
					match(DOT);
					setState(770);
					match(Identifier);
					setState(772);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,87,_ctx) ) {
					case 1:
						{
						setState(771);
						typeArguments();
						}
						break;
					}
					}
					} 
				}
				setState(778);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,88,_ctx);
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
		public TerminalNode BOOLEAN() { return getToken(RefactorMethodSignatureParser.BOOLEAN, 0); }
		public TerminalNode CHAR() { return getToken(RefactorMethodSignatureParser.CHAR, 0); }
		public TerminalNode BYTE() { return getToken(RefactorMethodSignatureParser.BYTE, 0); }
		public TerminalNode SHORT() { return getToken(RefactorMethodSignatureParser.SHORT, 0); }
		public TerminalNode INT() { return getToken(RefactorMethodSignatureParser.INT, 0); }
		public TerminalNode LONG() { return getToken(RefactorMethodSignatureParser.LONG, 0); }
		public TerminalNode FLOAT() { return getToken(RefactorMethodSignatureParser.FLOAT, 0); }
		public TerminalNode DOUBLE() { return getToken(RefactorMethodSignatureParser.DOUBLE, 0); }
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
			setState(779);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << BOOLEAN) | (1L << BYTE) | (1L << CHAR) | (1L << DOUBLE) | (1L << FLOAT) | (1L << INT) | (1L << LONG) | (1L << SHORT))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
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

	public static class TypeArgumentsContext extends ParserRuleContext {
		public TerminalNode LT() { return getToken(RefactorMethodSignatureParser.LT, 0); }
		public List<TypeArgumentContext> typeArgument() {
			return getRuleContexts(TypeArgumentContext.class);
		}
		public TypeArgumentContext typeArgument(int i) {
			return getRuleContext(TypeArgumentContext.class,i);
		}
		public TerminalNode GT() { return getToken(RefactorMethodSignatureParser.GT, 0); }
		public List<TerminalNode> COMMA() { return getTokens(RefactorMethodSignatureParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(RefactorMethodSignatureParser.COMMA, i);
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
			setState(781);
			match(LT);
			setState(782);
			typeArgument();
			setState(787);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(783);
				match(COMMA);
				setState(784);
				typeArgument();
				}
				}
				setState(789);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(790);
			match(GT);
			}
		}
		catch (RecognitionException re) {
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
		public TerminalNode QUESTION() { return getToken(RefactorMethodSignatureParser.QUESTION, 0); }
		public TerminalNode EXTENDS() { return getToken(RefactorMethodSignatureParser.EXTENDS, 0); }
		public TerminalNode SUPER() { return getToken(RefactorMethodSignatureParser.SUPER, 0); }
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
			setState(798);
			_errHandler.sync(this);
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
				setState(792);
				type();
				}
				break;
			case QUESTION:
				enterOuterAlt(_localctx, 2);
				{
				setState(793);
				match(QUESTION);
				setState(796);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EXTENDS || _la==SUPER) {
					{
					setState(794);
					_la = _input.LA(1);
					if ( !(_la==EXTENDS || _la==SUPER) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(795);
					type();
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
		public List<TerminalNode> COMMA() { return getTokens(RefactorMethodSignatureParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(RefactorMethodSignatureParser.COMMA, i);
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
			setState(800);
			qualifiedName();
			setState(805);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(801);
				match(COMMA);
				setState(802);
				qualifiedName();
				}
				}
				setState(807);
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
		public TerminalNode LPAREN() { return getToken(RefactorMethodSignatureParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(RefactorMethodSignatureParser.RPAREN, 0); }
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
			setState(808);
			match(LPAREN);
			setState(810);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << BOOLEAN) | (1L << BYTE) | (1L << CHAR) | (1L << DOUBLE) | (1L << FINAL) | (1L << FLOAT) | (1L << INT) | (1L << LONG) | (1L << SHORT))) != 0) || _la==Identifier || _la==AT) {
				{
				setState(809);
				formalParameterList();
				}
			}

			setState(812);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
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
		public FormalParameterContext formalParameter(int i) {
			return getRuleContext(FormalParameterContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(RefactorMethodSignatureParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(RefactorMethodSignatureParser.COMMA, i);
		}
		public LastFormalParameterContext lastFormalParameter() {
			return getRuleContext(LastFormalParameterContext.class,0);
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
			setState(827);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,96,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(814);
				formalParameter();
				setState(819);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,94,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(815);
						match(COMMA);
						setState(816);
						formalParameter();
						}
						} 
					}
					setState(821);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,94,_ctx);
				}
				setState(824);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==COMMA) {
					{
					setState(822);
					match(COMMA);
					setState(823);
					lastFormalParameter();
					}
				}

				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(826);
				lastFormalParameter();
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
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public VariableDeclaratorIdContext variableDeclaratorId() {
			return getRuleContext(VariableDeclaratorIdContext.class,0);
		}
		public List<VariableModifierContext> variableModifier() {
			return getRuleContexts(VariableModifierContext.class);
		}
		public VariableModifierContext variableModifier(int i) {
			return getRuleContext(VariableModifierContext.class,i);
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
			setState(832);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==FINAL || _la==AT) {
				{
				{
				setState(829);
				variableModifier();
				}
				}
				setState(834);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(835);
			type();
			setState(836);
			variableDeclaratorId();
			}
		}
		catch (RecognitionException re) {
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
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TerminalNode ELLIPSIS() { return getToken(RefactorMethodSignatureParser.ELLIPSIS, 0); }
		public VariableDeclaratorIdContext variableDeclaratorId() {
			return getRuleContext(VariableDeclaratorIdContext.class,0);
		}
		public List<VariableModifierContext> variableModifier() {
			return getRuleContexts(VariableModifierContext.class);
		}
		public VariableModifierContext variableModifier(int i) {
			return getRuleContext(VariableModifierContext.class,i);
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
			setState(841);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==FINAL || _la==AT) {
				{
				{
				setState(838);
				variableModifier();
				}
				}
				setState(843);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(844);
			type();
			setState(845);
			match(ELLIPSIS);
			setState(846);
			variableDeclaratorId();
			}
		}
		catch (RecognitionException re) {
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
			setState(848);
			block();
			}
		}
		catch (RecognitionException re) {
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
			setState(850);
			block();
			}
		}
		catch (RecognitionException re) {
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
		public List<TerminalNode> DOT() { return getTokens(RefactorMethodSignatureParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(RefactorMethodSignatureParser.DOT, i);
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
			setState(852);
			match(Identifier);
			setState(857);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,99,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(853);
					match(DOT);
					setState(854);
					match(Identifier);
					}
					} 
				}
				setState(859);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,99,_ctx);
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
		public TerminalNode IntegerLiteral() { return getToken(RefactorMethodSignatureParser.IntegerLiteral, 0); }
		public TerminalNode FloatingPointLiteral() { return getToken(RefactorMethodSignatureParser.FloatingPointLiteral, 0); }
		public TerminalNode CharacterLiteral() { return getToken(RefactorMethodSignatureParser.CharacterLiteral, 0); }
		public TerminalNode StringLiteral() { return getToken(RefactorMethodSignatureParser.StringLiteral, 0); }
		public TerminalNode BooleanLiteral() { return getToken(RefactorMethodSignatureParser.BooleanLiteral, 0); }
		public TerminalNode NullLiteral() { return getToken(RefactorMethodSignatureParser.NullLiteral, 0); }
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
			setState(860);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << IntegerLiteral) | (1L << FloatingPointLiteral) | (1L << BooleanLiteral) | (1L << CharacterLiteral) | (1L << StringLiteral) | (1L << NullLiteral))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
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

	public static class AnnotationContext extends ParserRuleContext {
		public TerminalNode AT() { return getToken(RefactorMethodSignatureParser.AT, 0); }
		public AnnotationNameContext annotationName() {
			return getRuleContext(AnnotationNameContext.class,0);
		}
		public TerminalNode LPAREN() { return getToken(RefactorMethodSignatureParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(RefactorMethodSignatureParser.RPAREN, 0); }
		public ElementValuePairsContext elementValuePairs() {
			return getRuleContext(ElementValuePairsContext.class,0);
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
			setState(862);
			match(AT);
			setState(863);
			annotationName();
			setState(870);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LPAREN) {
				{
				setState(864);
				match(LPAREN);
				setState(867);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,100,_ctx) ) {
				case 1:
					{
					setState(865);
					elementValuePairs();
					}
					break;
				case 2:
					{
					setState(866);
					elementValue();
					}
					break;
				}
				setState(869);
				match(RPAREN);
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
			setState(872);
			qualifiedName();
			}
		}
		catch (RecognitionException re) {
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
		public List<ElementValuePairContext> elementValuePair() {
			return getRuleContexts(ElementValuePairContext.class);
		}
		public ElementValuePairContext elementValuePair(int i) {
			return getRuleContext(ElementValuePairContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(RefactorMethodSignatureParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(RefactorMethodSignatureParser.COMMA, i);
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
			setState(874);
			elementValuePair();
			setState(879);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(875);
				match(COMMA);
				setState(876);
				elementValuePair();
				}
				}
				setState(881);
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
		public TerminalNode ASSIGN() { return getToken(RefactorMethodSignatureParser.ASSIGN, 0); }
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
			setState(882);
			match(Identifier);
			setState(883);
			match(ASSIGN);
			setState(884);
			elementValue();
			}
		}
		catch (RecognitionException re) {
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
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public AnnotationContext annotation() {
			return getRuleContext(AnnotationContext.class,0);
		}
		public ElementValueArrayInitializerContext elementValueArrayInitializer() {
			return getRuleContext(ElementValueArrayInitializerContext.class,0);
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
			setState(889);
			_errHandler.sync(this);
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
				setState(886);
				expression(0);
				}
				break;
			case AT:
				enterOuterAlt(_localctx, 2);
				{
				setState(887);
				annotation();
				}
				break;
			case LBRACE:
				enterOuterAlt(_localctx, 3);
				{
				setState(888);
				elementValueArrayInitializer();
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
		public TerminalNode LBRACE() { return getToken(RefactorMethodSignatureParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(RefactorMethodSignatureParser.RBRACE, 0); }
		public List<ElementValueContext> elementValue() {
			return getRuleContexts(ElementValueContext.class);
		}
		public ElementValueContext elementValue(int i) {
			return getRuleContext(ElementValueContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(RefactorMethodSignatureParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(RefactorMethodSignatureParser.COMMA, i);
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
			setState(891);
			match(LBRACE);
			setState(900);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << BOOLEAN) | (1L << BYTE) | (1L << CHAR) | (1L << DOUBLE) | (1L << FLOAT) | (1L << INT) | (1L << LONG) | (1L << NEW) | (1L << SHORT) | (1L << SUPER) | (1L << THIS) | (1L << VOID) | (1L << IntegerLiteral) | (1L << FloatingPointLiteral) | (1L << BooleanLiteral) | (1L << CharacterLiteral) | (1L << StringLiteral) | (1L << NullLiteral) | (1L << LPAREN) | (1L << LBRACE))) != 0) || ((((_la - 71)) & ~0x3f) == 0 && ((1L << (_la - 71)) & ((1L << (LT - 71)) | (1L << (BANG - 71)) | (1L << (TILDE - 71)) | (1L << (INC - 71)) | (1L << (DEC - 71)) | (1L << (ADD - 71)) | (1L << (SUB - 71)) | (1L << (Identifier - 71)) | (1L << (AT - 71)))) != 0)) {
				{
				setState(892);
				elementValue();
				setState(897);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,104,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(893);
						match(COMMA);
						setState(894);
						elementValue();
						}
						} 
					}
					setState(899);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,104,_ctx);
				}
				}
			}

			setState(903);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(902);
				match(COMMA);
				}
			}

			setState(905);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
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
		public TerminalNode AT() { return getToken(RefactorMethodSignatureParser.AT, 0); }
		public TerminalNode INTERFACE() { return getToken(RefactorMethodSignatureParser.INTERFACE, 0); }
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
			setState(907);
			match(AT);
			setState(908);
			match(INTERFACE);
			setState(909);
			match(Identifier);
			setState(910);
			annotationTypeBody();
			}
		}
		catch (RecognitionException re) {
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
		public TerminalNode LBRACE() { return getToken(RefactorMethodSignatureParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(RefactorMethodSignatureParser.RBRACE, 0); }
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
			setState(912);
			match(LBRACE);
			setState(916);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ABSTRACT) | (1L << BOOLEAN) | (1L << BYTE) | (1L << CHAR) | (1L << CLASS) | (1L << DOUBLE) | (1L << ENUM) | (1L << FINAL) | (1L << FLOAT) | (1L << INT) | (1L << INTERFACE) | (1L << LONG) | (1L << NATIVE) | (1L << PRIVATE) | (1L << PROTECTED) | (1L << PUBLIC) | (1L << SHORT) | (1L << STATIC) | (1L << STRICTFP) | (1L << SYNCHRONIZED) | (1L << TRANSIENT) | (1L << VOLATILE))) != 0) || ((((_la - 66)) & ~0x3f) == 0 && ((1L << (_la - 66)) & ((1L << (SEMI - 66)) | (1L << (Identifier - 66)) | (1L << (AT - 66)))) != 0)) {
				{
				{
				setState(913);
				annotationTypeElementDeclaration();
				}
				}
				setState(918);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(919);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
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
		public AnnotationTypeElementRestContext annotationTypeElementRest() {
			return getRuleContext(AnnotationTypeElementRestContext.class,0);
		}
		public List<ModifierContext> modifier() {
			return getRuleContexts(ModifierContext.class);
		}
		public ModifierContext modifier(int i) {
			return getRuleContext(ModifierContext.class,i);
		}
		public TerminalNode SEMI() { return getToken(RefactorMethodSignatureParser.SEMI, 0); }
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
			setState(929);
			_errHandler.sync(this);
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
				setState(924);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,108,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(921);
						modifier();
						}
						} 
					}
					setState(926);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,108,_ctx);
				}
				setState(927);
				annotationTypeElementRest();
				}
				break;
			case SEMI:
				enterOuterAlt(_localctx, 2);
				{
				setState(928);
				match(SEMI);
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
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public AnnotationMethodOrConstantRestContext annotationMethodOrConstantRest() {
			return getRuleContext(AnnotationMethodOrConstantRestContext.class,0);
		}
		public TerminalNode SEMI() { return getToken(RefactorMethodSignatureParser.SEMI, 0); }
		public ClassDeclarationContext classDeclaration() {
			return getRuleContext(ClassDeclarationContext.class,0);
		}
		public InterfaceDeclarationContext interfaceDeclaration() {
			return getRuleContext(InterfaceDeclarationContext.class,0);
		}
		public EnumDeclarationContext enumDeclaration() {
			return getRuleContext(EnumDeclarationContext.class,0);
		}
		public AnnotationTypeDeclarationContext annotationTypeDeclaration() {
			return getRuleContext(AnnotationTypeDeclarationContext.class,0);
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
			setState(951);
			_errHandler.sync(this);
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
				setState(931);
				type();
				setState(932);
				annotationMethodOrConstantRest();
				setState(933);
				match(SEMI);
				}
				break;
			case CLASS:
				enterOuterAlt(_localctx, 2);
				{
				setState(935);
				classDeclaration();
				setState(937);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,110,_ctx) ) {
				case 1:
					{
					setState(936);
					match(SEMI);
					}
					break;
				}
				}
				break;
			case INTERFACE:
				enterOuterAlt(_localctx, 3);
				{
				setState(939);
				interfaceDeclaration();
				setState(941);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,111,_ctx) ) {
				case 1:
					{
					setState(940);
					match(SEMI);
					}
					break;
				}
				}
				break;
			case ENUM:
				enterOuterAlt(_localctx, 4);
				{
				setState(943);
				enumDeclaration();
				setState(945);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,112,_ctx) ) {
				case 1:
					{
					setState(944);
					match(SEMI);
					}
					break;
				}
				}
				break;
			case AT:
				enterOuterAlt(_localctx, 5);
				{
				setState(947);
				annotationTypeDeclaration();
				setState(949);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,113,_ctx) ) {
				case 1:
					{
					setState(948);
					match(SEMI);
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
			setState(955);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,115,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(953);
				annotationMethodRest();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(954);
				annotationConstantRest();
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
		public TerminalNode LPAREN() { return getToken(RefactorMethodSignatureParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(RefactorMethodSignatureParser.RPAREN, 0); }
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
			setState(957);
			match(Identifier);
			setState(958);
			match(LPAREN);
			setState(959);
			match(RPAREN);
			setState(961);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==DEFAULT) {
				{
				setState(960);
				defaultValue();
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
			setState(963);
			variableDeclarators();
			}
		}
		catch (RecognitionException re) {
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
		public TerminalNode DEFAULT() { return getToken(RefactorMethodSignatureParser.DEFAULT, 0); }
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
			setState(965);
			match(DEFAULT);
			setState(966);
			elementValue();
			}
		}
		catch (RecognitionException re) {
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
		public TerminalNode LBRACE() { return getToken(RefactorMethodSignatureParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(RefactorMethodSignatureParser.RBRACE, 0); }
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
			setState(968);
			match(LBRACE);
			setState(972);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ABSTRACT) | (1L << ASSERT) | (1L << BOOLEAN) | (1L << BREAK) | (1L << BYTE) | (1L << CHAR) | (1L << CLASS) | (1L << CONTINUE) | (1L << DO) | (1L << DOUBLE) | (1L << ENUM) | (1L << FINAL) | (1L << FLOAT) | (1L << FOR) | (1L << IF) | (1L << INT) | (1L << INTERFACE) | (1L << LONG) | (1L << NEW) | (1L << PRIVATE) | (1L << PROTECTED) | (1L << PUBLIC) | (1L << RETURN) | (1L << SHORT) | (1L << STATIC) | (1L << STRICTFP) | (1L << SUPER) | (1L << SWITCH) | (1L << SYNCHRONIZED) | (1L << THIS) | (1L << THROW) | (1L << TRY) | (1L << VOID) | (1L << WHILE) | (1L << IntegerLiteral) | (1L << FloatingPointLiteral) | (1L << BooleanLiteral) | (1L << CharacterLiteral) | (1L << StringLiteral) | (1L << NullLiteral) | (1L << LPAREN) | (1L << LBRACE))) != 0) || ((((_la - 66)) & ~0x3f) == 0 && ((1L << (_la - 66)) & ((1L << (SEMI - 66)) | (1L << (LT - 66)) | (1L << (BANG - 66)) | (1L << (TILDE - 66)) | (1L << (INC - 66)) | (1L << (DEC - 66)) | (1L << (ADD - 66)) | (1L << (SUB - 66)) | (1L << (Identifier - 66)) | (1L << (AT - 66)))) != 0)) {
				{
				{
				setState(969);
				blockStatement();
				}
				}
				setState(974);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(975);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
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
		public LocalVariableDeclarationStatementContext localVariableDeclarationStatement() {
			return getRuleContext(LocalVariableDeclarationStatementContext.class,0);
		}
		public StatementContext statement() {
			return getRuleContext(StatementContext.class,0);
		}
		public TypeDeclarationContext typeDeclaration() {
			return getRuleContext(TypeDeclarationContext.class,0);
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
			setState(980);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,118,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(977);
				localVariableDeclarationStatement();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(978);
				statement();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(979);
				typeDeclaration();
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
		public TerminalNode SEMI() { return getToken(RefactorMethodSignatureParser.SEMI, 0); }
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
			setState(982);
			localVariableDeclaration();
			setState(983);
			match(SEMI);
			}
		}
		catch (RecognitionException re) {
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
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public VariableDeclaratorsContext variableDeclarators() {
			return getRuleContext(VariableDeclaratorsContext.class,0);
		}
		public List<VariableModifierContext> variableModifier() {
			return getRuleContexts(VariableModifierContext.class);
		}
		public VariableModifierContext variableModifier(int i) {
			return getRuleContext(VariableModifierContext.class,i);
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
			setState(988);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==FINAL || _la==AT) {
				{
				{
				setState(985);
				variableModifier();
				}
				}
				setState(990);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(991);
			type();
			setState(992);
			variableDeclarators();
			}
		}
		catch (RecognitionException re) {
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
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public TerminalNode ASSERT() { return getToken(RefactorMethodSignatureParser.ASSERT, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TerminalNode SEMI() { return getToken(RefactorMethodSignatureParser.SEMI, 0); }
		public TerminalNode COLON() { return getToken(RefactorMethodSignatureParser.COLON, 0); }
		public TerminalNode IF() { return getToken(RefactorMethodSignatureParser.IF, 0); }
		public ParExpressionContext parExpression() {
			return getRuleContext(ParExpressionContext.class,0);
		}
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public TerminalNode ELSE() { return getToken(RefactorMethodSignatureParser.ELSE, 0); }
		public TerminalNode FOR() { return getToken(RefactorMethodSignatureParser.FOR, 0); }
		public TerminalNode LPAREN() { return getToken(RefactorMethodSignatureParser.LPAREN, 0); }
		public ForControlContext forControl() {
			return getRuleContext(ForControlContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(RefactorMethodSignatureParser.RPAREN, 0); }
		public TerminalNode WHILE() { return getToken(RefactorMethodSignatureParser.WHILE, 0); }
		public TerminalNode DO() { return getToken(RefactorMethodSignatureParser.DO, 0); }
		public TerminalNode TRY() { return getToken(RefactorMethodSignatureParser.TRY, 0); }
		public FinallyBlockContext finallyBlock() {
			return getRuleContext(FinallyBlockContext.class,0);
		}
		public List<CatchClauseContext> catchClause() {
			return getRuleContexts(CatchClauseContext.class);
		}
		public CatchClauseContext catchClause(int i) {
			return getRuleContext(CatchClauseContext.class,i);
		}
		public ResourceSpecificationContext resourceSpecification() {
			return getRuleContext(ResourceSpecificationContext.class,0);
		}
		public TerminalNode SWITCH() { return getToken(RefactorMethodSignatureParser.SWITCH, 0); }
		public TerminalNode LBRACE() { return getToken(RefactorMethodSignatureParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(RefactorMethodSignatureParser.RBRACE, 0); }
		public List<SwitchBlockStatementGroupContext> switchBlockStatementGroup() {
			return getRuleContexts(SwitchBlockStatementGroupContext.class);
		}
		public SwitchBlockStatementGroupContext switchBlockStatementGroup(int i) {
			return getRuleContext(SwitchBlockStatementGroupContext.class,i);
		}
		public List<SwitchLabelContext> switchLabel() {
			return getRuleContexts(SwitchLabelContext.class);
		}
		public SwitchLabelContext switchLabel(int i) {
			return getRuleContext(SwitchLabelContext.class,i);
		}
		public TerminalNode SYNCHRONIZED() { return getToken(RefactorMethodSignatureParser.SYNCHRONIZED, 0); }
		public TerminalNode RETURN() { return getToken(RefactorMethodSignatureParser.RETURN, 0); }
		public TerminalNode THROW() { return getToken(RefactorMethodSignatureParser.THROW, 0); }
		public TerminalNode BREAK() { return getToken(RefactorMethodSignatureParser.BREAK, 0); }
		public TerminalNode Identifier() { return getToken(RefactorMethodSignatureParser.Identifier, 0); }
		public TerminalNode CONTINUE() { return getToken(RefactorMethodSignatureParser.CONTINUE, 0); }
		public StatementExpressionContext statementExpression() {
			return getRuleContext(StatementExpressionContext.class,0);
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
			setState(1098);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,132,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(994);
				block();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(995);
				match(ASSERT);
				setState(996);
				expression(0);
				setState(999);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==COLON) {
					{
					setState(997);
					match(COLON);
					setState(998);
					expression(0);
					}
				}

				setState(1001);
				match(SEMI);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1003);
				match(IF);
				setState(1004);
				parExpression();
				setState(1005);
				statement();
				setState(1008);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,121,_ctx) ) {
				case 1:
					{
					setState(1006);
					match(ELSE);
					setState(1007);
					statement();
					}
					break;
				}
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1010);
				match(FOR);
				setState(1011);
				match(LPAREN);
				setState(1012);
				forControl();
				setState(1013);
				match(RPAREN);
				setState(1014);
				statement();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(1016);
				match(WHILE);
				setState(1017);
				parExpression();
				setState(1018);
				statement();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(1020);
				match(DO);
				setState(1021);
				statement();
				setState(1022);
				match(WHILE);
				setState(1023);
				parExpression();
				setState(1024);
				match(SEMI);
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(1026);
				match(TRY);
				setState(1027);
				block();
				setState(1037);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case CATCH:
					{
					setState(1029); 
					_errHandler.sync(this);
					_la = _input.LA(1);
					do {
						{
						{
						setState(1028);
						catchClause();
						}
						}
						setState(1031); 
						_errHandler.sync(this);
						_la = _input.LA(1);
					} while ( _la==CATCH );
					setState(1034);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==FINALLY) {
						{
						setState(1033);
						finallyBlock();
						}
					}

					}
					break;
				case FINALLY:
					{
					setState(1036);
					finallyBlock();
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
				setState(1039);
				match(TRY);
				setState(1040);
				resourceSpecification();
				setState(1041);
				block();
				setState(1045);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==CATCH) {
					{
					{
					setState(1042);
					catchClause();
					}
					}
					setState(1047);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(1049);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==FINALLY) {
					{
					setState(1048);
					finallyBlock();
					}
				}

				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(1051);
				match(SWITCH);
				setState(1052);
				parExpression();
				setState(1053);
				match(LBRACE);
				setState(1057);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,127,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1054);
						switchBlockStatementGroup();
						}
						} 
					}
					setState(1059);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,127,_ctx);
				}
				setState(1063);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==CASE || _la==DEFAULT) {
					{
					{
					setState(1060);
					switchLabel();
					}
					}
					setState(1065);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(1066);
				match(RBRACE);
				}
				break;
			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(1068);
				match(SYNCHRONIZED);
				setState(1069);
				parExpression();
				setState(1070);
				block();
				}
				break;
			case 11:
				enterOuterAlt(_localctx, 11);
				{
				setState(1072);
				match(RETURN);
				setState(1074);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << BOOLEAN) | (1L << BYTE) | (1L << CHAR) | (1L << DOUBLE) | (1L << FLOAT) | (1L << INT) | (1L << LONG) | (1L << NEW) | (1L << SHORT) | (1L << SUPER) | (1L << THIS) | (1L << VOID) | (1L << IntegerLiteral) | (1L << FloatingPointLiteral) | (1L << BooleanLiteral) | (1L << CharacterLiteral) | (1L << StringLiteral) | (1L << NullLiteral) | (1L << LPAREN))) != 0) || ((((_la - 71)) & ~0x3f) == 0 && ((1L << (_la - 71)) & ((1L << (LT - 71)) | (1L << (BANG - 71)) | (1L << (TILDE - 71)) | (1L << (INC - 71)) | (1L << (DEC - 71)) | (1L << (ADD - 71)) | (1L << (SUB - 71)) | (1L << (Identifier - 71)))) != 0)) {
					{
					setState(1073);
					expression(0);
					}
				}

				setState(1076);
				match(SEMI);
				}
				break;
			case 12:
				enterOuterAlt(_localctx, 12);
				{
				setState(1077);
				match(THROW);
				setState(1078);
				expression(0);
				setState(1079);
				match(SEMI);
				}
				break;
			case 13:
				enterOuterAlt(_localctx, 13);
				{
				setState(1081);
				match(BREAK);
				setState(1083);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Identifier) {
					{
					setState(1082);
					match(Identifier);
					}
				}

				setState(1085);
				match(SEMI);
				}
				break;
			case 14:
				enterOuterAlt(_localctx, 14);
				{
				setState(1086);
				match(CONTINUE);
				setState(1088);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Identifier) {
					{
					setState(1087);
					match(Identifier);
					}
				}

				setState(1090);
				match(SEMI);
				}
				break;
			case 15:
				enterOuterAlt(_localctx, 15);
				{
				setState(1091);
				match(SEMI);
				}
				break;
			case 16:
				enterOuterAlt(_localctx, 16);
				{
				setState(1092);
				statementExpression();
				setState(1093);
				match(SEMI);
				}
				break;
			case 17:
				enterOuterAlt(_localctx, 17);
				{
				setState(1095);
				match(Identifier);
				setState(1096);
				match(COLON);
				setState(1097);
				statement();
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
		public TerminalNode CATCH() { return getToken(RefactorMethodSignatureParser.CATCH, 0); }
		public TerminalNode LPAREN() { return getToken(RefactorMethodSignatureParser.LPAREN, 0); }
		public CatchTypeContext catchType() {
			return getRuleContext(CatchTypeContext.class,0);
		}
		public TerminalNode Identifier() { return getToken(RefactorMethodSignatureParser.Identifier, 0); }
		public TerminalNode RPAREN() { return getToken(RefactorMethodSignatureParser.RPAREN, 0); }
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public List<VariableModifierContext> variableModifier() {
			return getRuleContexts(VariableModifierContext.class);
		}
		public VariableModifierContext variableModifier(int i) {
			return getRuleContext(VariableModifierContext.class,i);
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
			setState(1100);
			match(CATCH);
			setState(1101);
			match(LPAREN);
			setState(1105);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==FINAL || _la==AT) {
				{
				{
				setState(1102);
				variableModifier();
				}
				}
				setState(1107);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1108);
			catchType();
			setState(1109);
			match(Identifier);
			setState(1110);
			match(RPAREN);
			setState(1111);
			block();
			}
		}
		catch (RecognitionException re) {
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
		public List<TerminalNode> BITOR() { return getTokens(RefactorMethodSignatureParser.BITOR); }
		public TerminalNode BITOR(int i) {
			return getToken(RefactorMethodSignatureParser.BITOR, i);
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
			setState(1113);
			qualifiedName();
			setState(1118);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==BITOR) {
				{
				{
				setState(1114);
				match(BITOR);
				setState(1115);
				qualifiedName();
				}
				}
				setState(1120);
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
		public TerminalNode FINALLY() { return getToken(RefactorMethodSignatureParser.FINALLY, 0); }
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
			setState(1121);
			match(FINALLY);
			setState(1122);
			block();
			}
		}
		catch (RecognitionException re) {
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
		public TerminalNode LPAREN() { return getToken(RefactorMethodSignatureParser.LPAREN, 0); }
		public ResourcesContext resources() {
			return getRuleContext(ResourcesContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(RefactorMethodSignatureParser.RPAREN, 0); }
		public TerminalNode SEMI() { return getToken(RefactorMethodSignatureParser.SEMI, 0); }
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
			setState(1124);
			match(LPAREN);
			setState(1125);
			resources();
			setState(1127);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SEMI) {
				{
				setState(1126);
				match(SEMI);
				}
			}

			setState(1129);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
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
		public List<ResourceContext> resource() {
			return getRuleContexts(ResourceContext.class);
		}
		public ResourceContext resource(int i) {
			return getRuleContext(ResourceContext.class,i);
		}
		public List<TerminalNode> SEMI() { return getTokens(RefactorMethodSignatureParser.SEMI); }
		public TerminalNode SEMI(int i) {
			return getToken(RefactorMethodSignatureParser.SEMI, i);
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
			setState(1131);
			resource();
			setState(1136);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,136,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1132);
					match(SEMI);
					setState(1133);
					resource();
					}
					} 
				}
				setState(1138);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,136,_ctx);
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
		public ClassOrInterfaceTypeContext classOrInterfaceType() {
			return getRuleContext(ClassOrInterfaceTypeContext.class,0);
		}
		public VariableDeclaratorIdContext variableDeclaratorId() {
			return getRuleContext(VariableDeclaratorIdContext.class,0);
		}
		public TerminalNode ASSIGN() { return getToken(RefactorMethodSignatureParser.ASSIGN, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public List<VariableModifierContext> variableModifier() {
			return getRuleContexts(VariableModifierContext.class);
		}
		public VariableModifierContext variableModifier(int i) {
			return getRuleContext(VariableModifierContext.class,i);
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
			setState(1142);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==FINAL || _la==AT) {
				{
				{
				setState(1139);
				variableModifier();
				}
				}
				setState(1144);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1145);
			classOrInterfaceType();
			setState(1146);
			variableDeclaratorId();
			setState(1147);
			match(ASSIGN);
			setState(1148);
			expression(0);
			}
		}
		catch (RecognitionException re) {
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
		public List<SwitchLabelContext> switchLabel() {
			return getRuleContexts(SwitchLabelContext.class);
		}
		public SwitchLabelContext switchLabel(int i) {
			return getRuleContext(SwitchLabelContext.class,i);
		}
		public List<BlockStatementContext> blockStatement() {
			return getRuleContexts(BlockStatementContext.class);
		}
		public BlockStatementContext blockStatement(int i) {
			return getRuleContext(BlockStatementContext.class,i);
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
			setState(1151); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(1150);
				switchLabel();
				}
				}
				setState(1153); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==CASE || _la==DEFAULT );
			setState(1156); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(1155);
				blockStatement();
				}
				}
				setState(1158); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ABSTRACT) | (1L << ASSERT) | (1L << BOOLEAN) | (1L << BREAK) | (1L << BYTE) | (1L << CHAR) | (1L << CLASS) | (1L << CONTINUE) | (1L << DO) | (1L << DOUBLE) | (1L << ENUM) | (1L << FINAL) | (1L << FLOAT) | (1L << FOR) | (1L << IF) | (1L << INT) | (1L << INTERFACE) | (1L << LONG) | (1L << NEW) | (1L << PRIVATE) | (1L << PROTECTED) | (1L << PUBLIC) | (1L << RETURN) | (1L << SHORT) | (1L << STATIC) | (1L << STRICTFP) | (1L << SUPER) | (1L << SWITCH) | (1L << SYNCHRONIZED) | (1L << THIS) | (1L << THROW) | (1L << TRY) | (1L << VOID) | (1L << WHILE) | (1L << IntegerLiteral) | (1L << FloatingPointLiteral) | (1L << BooleanLiteral) | (1L << CharacterLiteral) | (1L << StringLiteral) | (1L << NullLiteral) | (1L << LPAREN) | (1L << LBRACE))) != 0) || ((((_la - 66)) & ~0x3f) == 0 && ((1L << (_la - 66)) & ((1L << (SEMI - 66)) | (1L << (LT - 66)) | (1L << (BANG - 66)) | (1L << (TILDE - 66)) | (1L << (INC - 66)) | (1L << (DEC - 66)) | (1L << (ADD - 66)) | (1L << (SUB - 66)) | (1L << (Identifier - 66)) | (1L << (AT - 66)))) != 0) );
			}
		}
		catch (RecognitionException re) {
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
		public TerminalNode CASE() { return getToken(RefactorMethodSignatureParser.CASE, 0); }
		public ConstantExpressionContext constantExpression() {
			return getRuleContext(ConstantExpressionContext.class,0);
		}
		public TerminalNode COLON() { return getToken(RefactorMethodSignatureParser.COLON, 0); }
		public EnumConstantNameContext enumConstantName() {
			return getRuleContext(EnumConstantNameContext.class,0);
		}
		public TerminalNode DEFAULT() { return getToken(RefactorMethodSignatureParser.DEFAULT, 0); }
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
			setState(1170);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,140,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1160);
				match(CASE);
				setState(1161);
				constantExpression();
				setState(1162);
				match(COLON);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1164);
				match(CASE);
				setState(1165);
				enumConstantName();
				setState(1166);
				match(COLON);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1168);
				match(DEFAULT);
				setState(1169);
				match(COLON);
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
		public EnhancedForControlContext enhancedForControl() {
			return getRuleContext(EnhancedForControlContext.class,0);
		}
		public List<TerminalNode> SEMI() { return getTokens(RefactorMethodSignatureParser.SEMI); }
		public TerminalNode SEMI(int i) {
			return getToken(RefactorMethodSignatureParser.SEMI, i);
		}
		public ForInitContext forInit() {
			return getRuleContext(ForInitContext.class,0);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public ForUpdateContext forUpdate() {
			return getRuleContext(ForUpdateContext.class,0);
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
			setState(1184);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,144,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1172);
				enhancedForControl();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1174);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << BOOLEAN) | (1L << BYTE) | (1L << CHAR) | (1L << DOUBLE) | (1L << FINAL) | (1L << FLOAT) | (1L << INT) | (1L << LONG) | (1L << NEW) | (1L << SHORT) | (1L << SUPER) | (1L << THIS) | (1L << VOID) | (1L << IntegerLiteral) | (1L << FloatingPointLiteral) | (1L << BooleanLiteral) | (1L << CharacterLiteral) | (1L << StringLiteral) | (1L << NullLiteral) | (1L << LPAREN))) != 0) || ((((_la - 71)) & ~0x3f) == 0 && ((1L << (_la - 71)) & ((1L << (LT - 71)) | (1L << (BANG - 71)) | (1L << (TILDE - 71)) | (1L << (INC - 71)) | (1L << (DEC - 71)) | (1L << (ADD - 71)) | (1L << (SUB - 71)) | (1L << (Identifier - 71)) | (1L << (AT - 71)))) != 0)) {
					{
					setState(1173);
					forInit();
					}
				}

				setState(1176);
				match(SEMI);
				setState(1178);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << BOOLEAN) | (1L << BYTE) | (1L << CHAR) | (1L << DOUBLE) | (1L << FLOAT) | (1L << INT) | (1L << LONG) | (1L << NEW) | (1L << SHORT) | (1L << SUPER) | (1L << THIS) | (1L << VOID) | (1L << IntegerLiteral) | (1L << FloatingPointLiteral) | (1L << BooleanLiteral) | (1L << CharacterLiteral) | (1L << StringLiteral) | (1L << NullLiteral) | (1L << LPAREN))) != 0) || ((((_la - 71)) & ~0x3f) == 0 && ((1L << (_la - 71)) & ((1L << (LT - 71)) | (1L << (BANG - 71)) | (1L << (TILDE - 71)) | (1L << (INC - 71)) | (1L << (DEC - 71)) | (1L << (ADD - 71)) | (1L << (SUB - 71)) | (1L << (Identifier - 71)))) != 0)) {
					{
					setState(1177);
					expression(0);
					}
				}

				setState(1180);
				match(SEMI);
				setState(1182);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << BOOLEAN) | (1L << BYTE) | (1L << CHAR) | (1L << DOUBLE) | (1L << FLOAT) | (1L << INT) | (1L << LONG) | (1L << NEW) | (1L << SHORT) | (1L << SUPER) | (1L << THIS) | (1L << VOID) | (1L << IntegerLiteral) | (1L << FloatingPointLiteral) | (1L << BooleanLiteral) | (1L << CharacterLiteral) | (1L << StringLiteral) | (1L << NullLiteral) | (1L << LPAREN))) != 0) || ((((_la - 71)) & ~0x3f) == 0 && ((1L << (_la - 71)) & ((1L << (LT - 71)) | (1L << (BANG - 71)) | (1L << (TILDE - 71)) | (1L << (INC - 71)) | (1L << (DEC - 71)) | (1L << (ADD - 71)) | (1L << (SUB - 71)) | (1L << (Identifier - 71)))) != 0)) {
					{
					setState(1181);
					forUpdate();
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
			setState(1188);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,145,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1186);
				localVariableDeclaration();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1187);
				expressionList();
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
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public VariableDeclaratorIdContext variableDeclaratorId() {
			return getRuleContext(VariableDeclaratorIdContext.class,0);
		}
		public TerminalNode COLON() { return getToken(RefactorMethodSignatureParser.COLON, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public List<VariableModifierContext> variableModifier() {
			return getRuleContexts(VariableModifierContext.class);
		}
		public VariableModifierContext variableModifier(int i) {
			return getRuleContext(VariableModifierContext.class,i);
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
			setState(1193);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==FINAL || _la==AT) {
				{
				{
				setState(1190);
				variableModifier();
				}
				}
				setState(1195);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1196);
			type();
			setState(1197);
			variableDeclaratorId();
			setState(1198);
			match(COLON);
			setState(1199);
			expression(0);
			}
		}
		catch (RecognitionException re) {
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
			setState(1201);
			expressionList();
			}
		}
		catch (RecognitionException re) {
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
		public TerminalNode LPAREN() { return getToken(RefactorMethodSignatureParser.LPAREN, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(RefactorMethodSignatureParser.RPAREN, 0); }
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
			setState(1203);
			match(LPAREN);
			setState(1204);
			expression(0);
			setState(1205);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
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
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(RefactorMethodSignatureParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(RefactorMethodSignatureParser.COMMA, i);
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
			setState(1207);
			expression(0);
			setState(1212);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1208);
				match(COMMA);
				setState(1209);
				expression(0);
				}
				}
				setState(1214);
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
			setState(1215);
			expression(0);
			}
		}
		catch (RecognitionException re) {
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
			setState(1217);
			expression(0);
			}
		}
		catch (RecognitionException re) {
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
		public PrimaryContext primary() {
			return getRuleContext(PrimaryContext.class,0);
		}
		public TerminalNode NEW() { return getToken(RefactorMethodSignatureParser.NEW, 0); }
		public CreatorContext creator() {
			return getRuleContext(CreatorContext.class,0);
		}
		public TerminalNode LPAREN() { return getToken(RefactorMethodSignatureParser.LPAREN, 0); }
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(RefactorMethodSignatureParser.RPAREN, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public TerminalNode ADD() { return getToken(RefactorMethodSignatureParser.ADD, 0); }
		public TerminalNode SUB() { return getToken(RefactorMethodSignatureParser.SUB, 0); }
		public TerminalNode INC() { return getToken(RefactorMethodSignatureParser.INC, 0); }
		public TerminalNode DEC() { return getToken(RefactorMethodSignatureParser.DEC, 0); }
		public TerminalNode TILDE() { return getToken(RefactorMethodSignatureParser.TILDE, 0); }
		public TerminalNode BANG() { return getToken(RefactorMethodSignatureParser.BANG, 0); }
		public TerminalNode MUL() { return getToken(RefactorMethodSignatureParser.MUL, 0); }
		public TerminalNode DIV() { return getToken(RefactorMethodSignatureParser.DIV, 0); }
		public TerminalNode MOD() { return getToken(RefactorMethodSignatureParser.MOD, 0); }
		public List<TerminalNode> LT() { return getTokens(RefactorMethodSignatureParser.LT); }
		public TerminalNode LT(int i) {
			return getToken(RefactorMethodSignatureParser.LT, i);
		}
		public List<TerminalNode> GT() { return getTokens(RefactorMethodSignatureParser.GT); }
		public TerminalNode GT(int i) {
			return getToken(RefactorMethodSignatureParser.GT, i);
		}
		public TerminalNode LE() { return getToken(RefactorMethodSignatureParser.LE, 0); }
		public TerminalNode GE() { return getToken(RefactorMethodSignatureParser.GE, 0); }
		public TerminalNode EQUAL() { return getToken(RefactorMethodSignatureParser.EQUAL, 0); }
		public TerminalNode NOTEQUAL() { return getToken(RefactorMethodSignatureParser.NOTEQUAL, 0); }
		public TerminalNode BITAND() { return getToken(RefactorMethodSignatureParser.BITAND, 0); }
		public TerminalNode CARET() { return getToken(RefactorMethodSignatureParser.CARET, 0); }
		public TerminalNode BITOR() { return getToken(RefactorMethodSignatureParser.BITOR, 0); }
		public TerminalNode AND() { return getToken(RefactorMethodSignatureParser.AND, 0); }
		public TerminalNode OR() { return getToken(RefactorMethodSignatureParser.OR, 0); }
		public TerminalNode QUESTION() { return getToken(RefactorMethodSignatureParser.QUESTION, 0); }
		public TerminalNode COLON() { return getToken(RefactorMethodSignatureParser.COLON, 0); }
		public TerminalNode ASSIGN() { return getToken(RefactorMethodSignatureParser.ASSIGN, 0); }
		public TerminalNode ADD_ASSIGN() { return getToken(RefactorMethodSignatureParser.ADD_ASSIGN, 0); }
		public TerminalNode SUB_ASSIGN() { return getToken(RefactorMethodSignatureParser.SUB_ASSIGN, 0); }
		public TerminalNode MUL_ASSIGN() { return getToken(RefactorMethodSignatureParser.MUL_ASSIGN, 0); }
		public TerminalNode DIV_ASSIGN() { return getToken(RefactorMethodSignatureParser.DIV_ASSIGN, 0); }
		public TerminalNode AND_ASSIGN() { return getToken(RefactorMethodSignatureParser.AND_ASSIGN, 0); }
		public TerminalNode OR_ASSIGN() { return getToken(RefactorMethodSignatureParser.OR_ASSIGN, 0); }
		public TerminalNode XOR_ASSIGN() { return getToken(RefactorMethodSignatureParser.XOR_ASSIGN, 0); }
		public TerminalNode RSHIFT_ASSIGN() { return getToken(RefactorMethodSignatureParser.RSHIFT_ASSIGN, 0); }
		public TerminalNode URSHIFT_ASSIGN() { return getToken(RefactorMethodSignatureParser.URSHIFT_ASSIGN, 0); }
		public TerminalNode LSHIFT_ASSIGN() { return getToken(RefactorMethodSignatureParser.LSHIFT_ASSIGN, 0); }
		public TerminalNode MOD_ASSIGN() { return getToken(RefactorMethodSignatureParser.MOD_ASSIGN, 0); }
		public TerminalNode DOT() { return getToken(RefactorMethodSignatureParser.DOT, 0); }
		public TerminalNode Identifier() { return getToken(RefactorMethodSignatureParser.Identifier, 0); }
		public TerminalNode THIS() { return getToken(RefactorMethodSignatureParser.THIS, 0); }
		public InnerCreatorContext innerCreator() {
			return getRuleContext(InnerCreatorContext.class,0);
		}
		public NonWildcardTypeArgumentsContext nonWildcardTypeArguments() {
			return getRuleContext(NonWildcardTypeArgumentsContext.class,0);
		}
		public TerminalNode SUPER() { return getToken(RefactorMethodSignatureParser.SUPER, 0); }
		public SuperSuffixContext superSuffix() {
			return getRuleContext(SuperSuffixContext.class,0);
		}
		public ExplicitGenericInvocationContext explicitGenericInvocation() {
			return getRuleContext(ExplicitGenericInvocationContext.class,0);
		}
		public TerminalNode LBRACK() { return getToken(RefactorMethodSignatureParser.LBRACK, 0); }
		public TerminalNode RBRACK() { return getToken(RefactorMethodSignatureParser.RBRACK, 0); }
		public ExpressionListContext expressionList() {
			return getRuleContext(ExpressionListContext.class,0);
		}
		public TerminalNode INSTANCEOF() { return getToken(RefactorMethodSignatureParser.INSTANCEOF, 0); }
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
			setState(1232);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,148,_ctx) ) {
			case 1:
				{
				setState(1220);
				primary();
				}
				break;
			case 2:
				{
				setState(1221);
				match(NEW);
				setState(1222);
				creator();
				}
				break;
			case 3:
				{
				setState(1223);
				match(LPAREN);
				setState(1224);
				type();
				setState(1225);
				match(RPAREN);
				setState(1226);
				expression(17);
				}
				break;
			case 4:
				{
				setState(1228);
				_la = _input.LA(1);
				if ( !(((((_la - 82)) & ~0x3f) == 0 && ((1L << (_la - 82)) & ((1L << (INC - 82)) | (1L << (DEC - 82)) | (1L << (ADD - 82)) | (1L << (SUB - 82)))) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(1229);
				expression(15);
				}
				break;
			case 5:
				{
				setState(1230);
				_la = _input.LA(1);
				if ( !(_la==BANG || _la==TILDE) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(1231);
				expression(14);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(1319);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,153,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(1317);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,152,_ctx) ) {
					case 1:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1234);
						if (!(precpred(_ctx, 13))) throw new FailedPredicateException(this, "precpred(_ctx, 13)");
						setState(1235);
						_la = _input.LA(1);
						if ( !(((((_la - 86)) & ~0x3f) == 0 && ((1L << (_la - 86)) & ((1L << (MUL - 86)) | (1L << (DIV - 86)) | (1L << (MOD - 86)))) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(1236);
						expression(14);
						}
						break;
					case 2:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1237);
						if (!(precpred(_ctx, 12))) throw new FailedPredicateException(this, "precpred(_ctx, 12)");
						setState(1238);
						_la = _input.LA(1);
						if ( !(_la==ADD || _la==SUB) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(1239);
						expression(13);
						}
						break;
					case 3:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1240);
						if (!(precpred(_ctx, 11))) throw new FailedPredicateException(this, "precpred(_ctx, 11)");
						setState(1248);
						_errHandler.sync(this);
						switch ( getInterpreter().adaptivePredict(_input,149,_ctx) ) {
						case 1:
							{
							setState(1241);
							match(LT);
							setState(1242);
							match(LT);
							}
							break;
						case 2:
							{
							setState(1243);
							match(GT);
							setState(1244);
							match(GT);
							setState(1245);
							match(GT);
							}
							break;
						case 3:
							{
							setState(1246);
							match(GT);
							setState(1247);
							match(GT);
							}
							break;
						}
						setState(1250);
						expression(12);
						}
						break;
					case 4:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1251);
						if (!(precpred(_ctx, 10))) throw new FailedPredicateException(this, "precpred(_ctx, 10)");
						setState(1252);
						_la = _input.LA(1);
						if ( !(((((_la - 70)) & ~0x3f) == 0 && ((1L << (_la - 70)) & ((1L << (GT - 70)) | (1L << (LT - 70)) | (1L << (LE - 70)) | (1L << (GE - 70)))) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(1253);
						expression(11);
						}
						break;
					case 5:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1254);
						if (!(precpred(_ctx, 8))) throw new FailedPredicateException(this, "precpred(_ctx, 8)");
						setState(1255);
						_la = _input.LA(1);
						if ( !(_la==EQUAL || _la==NOTEQUAL) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(1256);
						expression(9);
						}
						break;
					case 6:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1257);
						if (!(precpred(_ctx, 7))) throw new FailedPredicateException(this, "precpred(_ctx, 7)");
						setState(1258);
						match(BITAND);
						setState(1259);
						expression(8);
						}
						break;
					case 7:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1260);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(1261);
						match(CARET);
						setState(1262);
						expression(7);
						}
						break;
					case 8:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1263);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(1264);
						match(BITOR);
						setState(1265);
						expression(6);
						}
						break;
					case 9:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1266);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(1267);
						match(AND);
						setState(1268);
						expression(5);
						}
						break;
					case 10:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1269);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(1270);
						match(OR);
						setState(1271);
						expression(4);
						}
						break;
					case 11:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1272);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(1273);
						match(QUESTION);
						setState(1274);
						expression(0);
						setState(1275);
						match(COLON);
						setState(1276);
						expression(3);
						}
						break;
					case 12:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1278);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(1279);
						_la = _input.LA(1);
						if ( !(((((_la - 69)) & ~0x3f) == 0 && ((1L << (_la - 69)) & ((1L << (ASSIGN - 69)) | (1L << (ADD_ASSIGN - 69)) | (1L << (SUB_ASSIGN - 69)) | (1L << (MUL_ASSIGN - 69)) | (1L << (DIV_ASSIGN - 69)) | (1L << (AND_ASSIGN - 69)) | (1L << (OR_ASSIGN - 69)) | (1L << (XOR_ASSIGN - 69)) | (1L << (MOD_ASSIGN - 69)) | (1L << (LSHIFT_ASSIGN - 69)) | (1L << (RSHIFT_ASSIGN - 69)) | (1L << (URSHIFT_ASSIGN - 69)))) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(1280);
						expression(1);
						}
						break;
					case 13:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1281);
						if (!(precpred(_ctx, 25))) throw new FailedPredicateException(this, "precpred(_ctx, 25)");
						setState(1282);
						match(DOT);
						setState(1283);
						match(Identifier);
						}
						break;
					case 14:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1284);
						if (!(precpred(_ctx, 24))) throw new FailedPredicateException(this, "precpred(_ctx, 24)");
						setState(1285);
						match(DOT);
						setState(1286);
						match(THIS);
						}
						break;
					case 15:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1287);
						if (!(precpred(_ctx, 23))) throw new FailedPredicateException(this, "precpred(_ctx, 23)");
						setState(1288);
						match(DOT);
						setState(1289);
						match(NEW);
						setState(1291);
						_errHandler.sync(this);
						_la = _input.LA(1);
						if (_la==LT) {
							{
							setState(1290);
							nonWildcardTypeArguments();
							}
						}

						setState(1293);
						innerCreator();
						}
						break;
					case 16:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1294);
						if (!(precpred(_ctx, 22))) throw new FailedPredicateException(this, "precpred(_ctx, 22)");
						setState(1295);
						match(DOT);
						setState(1296);
						match(SUPER);
						setState(1297);
						superSuffix();
						}
						break;
					case 17:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1298);
						if (!(precpred(_ctx, 21))) throw new FailedPredicateException(this, "precpred(_ctx, 21)");
						setState(1299);
						match(DOT);
						setState(1300);
						explicitGenericInvocation();
						}
						break;
					case 18:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1301);
						if (!(precpred(_ctx, 20))) throw new FailedPredicateException(this, "precpred(_ctx, 20)");
						setState(1302);
						match(LBRACK);
						setState(1303);
						expression(0);
						setState(1304);
						match(RBRACK);
						}
						break;
					case 19:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1306);
						if (!(precpred(_ctx, 19))) throw new FailedPredicateException(this, "precpred(_ctx, 19)");
						setState(1307);
						match(LPAREN);
						setState(1309);
						_errHandler.sync(this);
						_la = _input.LA(1);
						if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << BOOLEAN) | (1L << BYTE) | (1L << CHAR) | (1L << DOUBLE) | (1L << FLOAT) | (1L << INT) | (1L << LONG) | (1L << NEW) | (1L << SHORT) | (1L << SUPER) | (1L << THIS) | (1L << VOID) | (1L << IntegerLiteral) | (1L << FloatingPointLiteral) | (1L << BooleanLiteral) | (1L << CharacterLiteral) | (1L << StringLiteral) | (1L << NullLiteral) | (1L << LPAREN))) != 0) || ((((_la - 71)) & ~0x3f) == 0 && ((1L << (_la - 71)) & ((1L << (LT - 71)) | (1L << (BANG - 71)) | (1L << (TILDE - 71)) | (1L << (INC - 71)) | (1L << (DEC - 71)) | (1L << (ADD - 71)) | (1L << (SUB - 71)) | (1L << (Identifier - 71)))) != 0)) {
							{
							setState(1308);
							expressionList();
							}
						}

						setState(1311);
						match(RPAREN);
						}
						break;
					case 20:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1312);
						if (!(precpred(_ctx, 16))) throw new FailedPredicateException(this, "precpred(_ctx, 16)");
						setState(1313);
						_la = _input.LA(1);
						if ( !(_la==INC || _la==DEC) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						}
						break;
					case 21:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1314);
						if (!(precpred(_ctx, 9))) throw new FailedPredicateException(this, "precpred(_ctx, 9)");
						setState(1315);
						match(INSTANCEOF);
						setState(1316);
						type();
						}
						break;
					}
					} 
				}
				setState(1321);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,153,_ctx);
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
		public TerminalNode LPAREN() { return getToken(RefactorMethodSignatureParser.LPAREN, 0); }
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(RefactorMethodSignatureParser.RPAREN, 0); }
		public TerminalNode THIS() { return getToken(RefactorMethodSignatureParser.THIS, 0); }
		public TerminalNode SUPER() { return getToken(RefactorMethodSignatureParser.SUPER, 0); }
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public TerminalNode Identifier() { return getToken(RefactorMethodSignatureParser.Identifier, 0); }
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TerminalNode DOT() { return getToken(RefactorMethodSignatureParser.DOT, 0); }
		public TerminalNode CLASS() { return getToken(RefactorMethodSignatureParser.CLASS, 0); }
		public TerminalNode VOID() { return getToken(RefactorMethodSignatureParser.VOID, 0); }
		public NonWildcardTypeArgumentsContext nonWildcardTypeArguments() {
			return getRuleContext(NonWildcardTypeArgumentsContext.class,0);
		}
		public ExplicitGenericInvocationSuffixContext explicitGenericInvocationSuffix() {
			return getRuleContext(ExplicitGenericInvocationSuffixContext.class,0);
		}
		public ArgumentsContext arguments() {
			return getRuleContext(ArgumentsContext.class,0);
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
			setState(1343);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,155,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1322);
				match(LPAREN);
				setState(1323);
				expression(0);
				setState(1324);
				match(RPAREN);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1326);
				match(THIS);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1327);
				match(SUPER);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1328);
				literal();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(1329);
				match(Identifier);
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(1330);
				type();
				setState(1331);
				match(DOT);
				setState(1332);
				match(CLASS);
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(1334);
				match(VOID);
				setState(1335);
				match(DOT);
				setState(1336);
				match(CLASS);
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(1337);
				nonWildcardTypeArguments();
				setState(1341);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case SUPER:
				case Identifier:
					{
					setState(1338);
					explicitGenericInvocationSuffix();
					}
					break;
				case THIS:
					{
					setState(1339);
					match(THIS);
					setState(1340);
					arguments();
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
		public NonWildcardTypeArgumentsContext nonWildcardTypeArguments() {
			return getRuleContext(NonWildcardTypeArgumentsContext.class,0);
		}
		public CreatedNameContext createdName() {
			return getRuleContext(CreatedNameContext.class,0);
		}
		public ClassCreatorRestContext classCreatorRest() {
			return getRuleContext(ClassCreatorRestContext.class,0);
		}
		public ArrayCreatorRestContext arrayCreatorRest() {
			return getRuleContext(ArrayCreatorRestContext.class,0);
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
			setState(1354);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LT:
				enterOuterAlt(_localctx, 1);
				{
				setState(1345);
				nonWildcardTypeArguments();
				setState(1346);
				createdName();
				setState(1347);
				classCreatorRest();
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
				setState(1349);
				createdName();
				setState(1352);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case LBRACK:
					{
					setState(1350);
					arrayCreatorRest();
					}
					break;
				case LPAREN:
					{
					setState(1351);
					classCreatorRest();
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
		public TypeArgumentsOrDiamondContext typeArgumentsOrDiamond(int i) {
			return getRuleContext(TypeArgumentsOrDiamondContext.class,i);
		}
		public List<TerminalNode> DOT() { return getTokens(RefactorMethodSignatureParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(RefactorMethodSignatureParser.DOT, i);
		}
		public PrimitiveTypeContext primitiveType() {
			return getRuleContext(PrimitiveTypeContext.class,0);
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
			setState(1371);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(1356);
				match(Identifier);
				setState(1358);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LT) {
					{
					setState(1357);
					typeArgumentsOrDiamond();
					}
				}

				setState(1367);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==DOT) {
					{
					{
					setState(1360);
					match(DOT);
					setState(1361);
					match(Identifier);
					setState(1363);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==LT) {
						{
						setState(1362);
						typeArgumentsOrDiamond();
						}
					}

					}
					}
					setState(1369);
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
				setState(1370);
				primitiveType();
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
			setState(1373);
			match(Identifier);
			setState(1375);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(1374);
				nonWildcardTypeArgumentsOrDiamond();
				}
			}

			setState(1377);
			classCreatorRest();
			}
		}
		catch (RecognitionException re) {
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
		public List<TerminalNode> LBRACK() { return getTokens(RefactorMethodSignatureParser.LBRACK); }
		public TerminalNode LBRACK(int i) {
			return getToken(RefactorMethodSignatureParser.LBRACK, i);
		}
		public List<TerminalNode> RBRACK() { return getTokens(RefactorMethodSignatureParser.RBRACK); }
		public TerminalNode RBRACK(int i) {
			return getToken(RefactorMethodSignatureParser.RBRACK, i);
		}
		public ArrayInitializerContext arrayInitializer() {
			return getRuleContext(ArrayInitializerContext.class,0);
		}
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
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
			setState(1379);
			match(LBRACK);
			setState(1407);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case RBRACK:
				{
				setState(1380);
				match(RBRACK);
				setState(1385);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==LBRACK) {
					{
					{
					setState(1381);
					match(LBRACK);
					setState(1382);
					match(RBRACK);
					}
					}
					setState(1387);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(1388);
				arrayInitializer();
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
				setState(1389);
				expression(0);
				setState(1390);
				match(RBRACK);
				setState(1397);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,164,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1391);
						match(LBRACK);
						setState(1392);
						expression(0);
						setState(1393);
						match(RBRACK);
						}
						} 
					}
					setState(1399);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,164,_ctx);
				}
				setState(1404);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,165,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1400);
						match(LBRACK);
						setState(1401);
						match(RBRACK);
						}
						} 
					}
					setState(1406);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,165,_ctx);
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
		public ArgumentsContext arguments() {
			return getRuleContext(ArgumentsContext.class,0);
		}
		public ClassBodyContext classBody() {
			return getRuleContext(ClassBodyContext.class,0);
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
			setState(1409);
			arguments();
			setState(1411);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,167,_ctx) ) {
			case 1:
				{
				setState(1410);
				classBody();
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
			setState(1413);
			nonWildcardTypeArguments();
			setState(1414);
			explicitGenericInvocationSuffix();
			}
		}
		catch (RecognitionException re) {
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
		public TerminalNode LT() { return getToken(RefactorMethodSignatureParser.LT, 0); }
		public TypeListContext typeList() {
			return getRuleContext(TypeListContext.class,0);
		}
		public TerminalNode GT() { return getToken(RefactorMethodSignatureParser.GT, 0); }
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
			setState(1416);
			match(LT);
			setState(1417);
			typeList();
			setState(1418);
			match(GT);
			}
		}
		catch (RecognitionException re) {
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
		public TerminalNode LT() { return getToken(RefactorMethodSignatureParser.LT, 0); }
		public TerminalNode GT() { return getToken(RefactorMethodSignatureParser.GT, 0); }
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
			setState(1423);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,168,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1420);
				match(LT);
				setState(1421);
				match(GT);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1422);
				typeArguments();
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
		public TerminalNode LT() { return getToken(RefactorMethodSignatureParser.LT, 0); }
		public TerminalNode GT() { return getToken(RefactorMethodSignatureParser.GT, 0); }
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
			setState(1428);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,169,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1425);
				match(LT);
				setState(1426);
				match(GT);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1427);
				nonWildcardTypeArguments();
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
		public ArgumentsContext arguments() {
			return getRuleContext(ArgumentsContext.class,0);
		}
		public TerminalNode DOT() { return getToken(RefactorMethodSignatureParser.DOT, 0); }
		public TerminalNode Identifier() { return getToken(RefactorMethodSignatureParser.Identifier, 0); }
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
			setState(1436);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LPAREN:
				enterOuterAlt(_localctx, 1);
				{
				setState(1430);
				arguments();
				}
				break;
			case DOT:
				enterOuterAlt(_localctx, 2);
				{
				setState(1431);
				match(DOT);
				setState(1432);
				match(Identifier);
				setState(1434);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,170,_ctx) ) {
				case 1:
					{
					setState(1433);
					arguments();
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
		public TerminalNode SUPER() { return getToken(RefactorMethodSignatureParser.SUPER, 0); }
		public SuperSuffixContext superSuffix() {
			return getRuleContext(SuperSuffixContext.class,0);
		}
		public TerminalNode Identifier() { return getToken(RefactorMethodSignatureParser.Identifier, 0); }
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
			setState(1442);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SUPER:
				enterOuterAlt(_localctx, 1);
				{
				setState(1438);
				match(SUPER);
				setState(1439);
				superSuffix();
				}
				break;
			case Identifier:
				enterOuterAlt(_localctx, 2);
				{
				setState(1440);
				match(Identifier);
				setState(1441);
				arguments();
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
		public TerminalNode LPAREN() { return getToken(RefactorMethodSignatureParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(RefactorMethodSignatureParser.RPAREN, 0); }
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
			setState(1444);
			match(LPAREN);
			setState(1446);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << BOOLEAN) | (1L << BYTE) | (1L << CHAR) | (1L << DOUBLE) | (1L << FLOAT) | (1L << INT) | (1L << LONG) | (1L << NEW) | (1L << SHORT) | (1L << SUPER) | (1L << THIS) | (1L << VOID) | (1L << IntegerLiteral) | (1L << FloatingPointLiteral) | (1L << BooleanLiteral) | (1L << CharacterLiteral) | (1L << StringLiteral) | (1L << NullLiteral) | (1L << LPAREN))) != 0) || ((((_la - 71)) & ~0x3f) == 0 && ((1L << (_la - 71)) & ((1L << (LT - 71)) | (1L << (BANG - 71)) | (1L << (TILDE - 71)) | (1L << (INC - 71)) | (1L << (DEC - 71)) | (1L << (ADD - 71)) | (1L << (SUB - 71)) | (1L << (Identifier - 71)))) != 0)) {
				{
				setState(1445);
				expressionList();
				}
			}

			setState(1448);
			match(RPAREN);
			}
		}
		catch (RecognitionException re) {
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
		case 6:
			return targetTypePattern_sempred((TargetTypePatternContext)_localctx, predIndex);
		case 7:
			return formalTypePattern_sempred((FormalTypePatternContext)_localctx, predIndex);
		case 97:
			return expression_sempred((ExpressionContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean targetTypePattern_sempred(TargetTypePatternContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 2);
		case 1:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean formalTypePattern_sempred(FormalTypePatternContext _localctx, int predIndex) {
		switch (predIndex) {
		case 2:
			return precpred(_ctx, 2);
		case 3:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean expression_sempred(ExpressionContext _localctx, int predIndex) {
		switch (predIndex) {
		case 4:
			return precpred(_ctx, 13);
		case 5:
			return precpred(_ctx, 12);
		case 6:
			return precpred(_ctx, 11);
		case 7:
			return precpred(_ctx, 10);
		case 8:
			return precpred(_ctx, 8);
		case 9:
			return precpred(_ctx, 7);
		case 10:
			return precpred(_ctx, 6);
		case 11:
			return precpred(_ctx, 5);
		case 12:
			return precpred(_ctx, 4);
		case 13:
			return precpred(_ctx, 3);
		case 14:
			return precpred(_ctx, 2);
		case 15:
			return precpred(_ctx, 1);
		case 16:
			return precpred(_ctx, 25);
		case 17:
			return precpred(_ctx, 24);
		case 18:
			return precpred(_ctx, 23);
		case 19:
			return precpred(_ctx, 22);
		case 20:
			return precpred(_ctx, 21);
		case 21:
			return precpred(_ctx, 20);
		case 22:
			return precpred(_ctx, 19);
		case 23:
			return precpred(_ctx, 16);
		case 24:
			return precpred(_ctx, 9);
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3n\u05ad\4\2\t\2\4"+
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
		"k\4l\tl\4m\tm\4n\tn\4o\to\4p\tp\3\2\3\2\7\2\u00e3\n\2\f\2\16\2\u00e6\13"+
		"\2\3\2\3\2\5\2\u00ea\n\2\3\2\3\2\3\2\3\3\3\3\5\3\u00f1\n\3\3\3\3\3\3\4"+
		"\3\4\3\4\7\4\u00f8\n\4\f\4\16\4\u00fb\13\4\3\4\7\4\u00fe\n\4\f\4\16\4"+
		"\u0101\13\4\3\4\3\4\3\4\7\4\u0106\n\4\f\4\16\4\u0109\13\4\3\4\7\4\u010c"+
		"\n\4\f\4\16\4\u010f\13\4\3\4\3\4\3\4\5\4\u0114\n\4\3\5\3\5\3\6\3\6\3\6"+
		"\7\6\u011b\n\6\f\6\16\6\u011e\13\6\3\6\7\6\u0121\n\6\f\6\16\6\u0124\13"+
		"\6\3\6\3\6\3\6\5\6\u0129\n\6\3\7\3\7\3\7\3\7\3\7\5\7\u0130\n\7\3\b\3\b"+
		"\3\b\3\b\5\b\u0136\n\b\3\b\3\b\3\b\3\b\3\b\3\b\7\b\u013e\n\b\f\b\16\b"+
		"\u0141\13\b\3\t\3\t\3\t\3\t\3\t\5\t\u0148\n\t\3\t\3\t\3\t\3\t\3\t\3\t"+
		"\7\t\u0150\n\t\f\t\16\t\u0153\13\t\3\n\6\n\u0156\n\n\r\n\16\n\u0157\3"+
		"\n\3\n\7\n\u015c\n\n\f\n\16\n\u015f\13\n\3\13\3\13\3\13\7\13\u0164\n\13"+
		"\f\13\16\13\u0167\13\13\3\13\5\13\u016a\n\13\3\13\3\13\3\13\7\13\u016f"+
		"\n\13\f\13\16\13\u0172\13\13\3\13\5\13\u0175\n\13\5\13\u0177\n\13\3\f"+
		"\5\f\u017a\n\f\3\f\7\f\u017d\n\f\f\f\16\f\u0180\13\f\3\f\7\f\u0183\n\f"+
		"\f\f\16\f\u0186\13\f\3\f\3\f\3\r\7\r\u018b\n\r\f\r\16\r\u018e\13\r\3\r"+
		"\3\r\3\r\3\r\3\16\3\16\5\16\u0196\n\16\3\16\3\16\3\16\5\16\u019b\n\16"+
		"\3\16\3\16\3\17\7\17\u01a0\n\17\f\17\16\17\u01a3\13\17\3\17\3\17\7\17"+
		"\u01a7\n\17\f\17\16\17\u01aa\13\17\3\17\3\17\7\17\u01ae\n\17\f\17\16\17"+
		"\u01b1\13\17\3\17\3\17\7\17\u01b5\n\17\f\17\16\17\u01b8\13\17\3\17\3\17"+
		"\5\17\u01bc\n\17\3\20\3\20\5\20\u01c0\n\20\3\21\3\21\5\21\u01c4\n\21\3"+
		"\22\3\22\5\22\u01c8\n\22\3\23\3\23\3\23\5\23\u01cd\n\23\3\23\3\23\5\23"+
		"\u01d1\n\23\3\23\3\23\5\23\u01d5\n\23\3\23\3\23\3\24\3\24\3\24\3\24\7"+
		"\24\u01dd\n\24\f\24\16\24\u01e0\13\24\3\24\3\24\3\25\3\25\3\25\5\25\u01e7"+
		"\n\25\3\26\3\26\3\26\7\26\u01ec\n\26\f\26\16\26\u01ef\13\26\3\27\3\27"+
		"\3\27\3\27\5\27\u01f5\n\27\3\27\3\27\5\27\u01f9\n\27\3\27\5\27\u01fc\n"+
		"\27\3\27\5\27\u01ff\n\27\3\27\3\27\3\30\3\30\3\30\7\30\u0206\n\30\f\30"+
		"\16\30\u0209\13\30\3\31\7\31\u020c\n\31\f\31\16\31\u020f\13\31\3\31\3"+
		"\31\5\31\u0213\n\31\3\31\5\31\u0216\n\31\3\32\3\32\7\32\u021a\n\32\f\32"+
		"\16\32\u021d\13\32\3\33\3\33\3\33\5\33\u0222\n\33\3\33\3\33\5\33\u0226"+
		"\n\33\3\33\3\33\3\34\3\34\3\34\7\34\u022d\n\34\f\34\16\34\u0230\13\34"+
		"\3\35\3\35\7\35\u0234\n\35\f\35\16\35\u0237\13\35\3\35\3\35\3\36\3\36"+
		"\7\36\u023d\n\36\f\36\16\36\u0240\13\36\3\36\3\36\3\37\3\37\5\37\u0246"+
		"\n\37\3\37\3\37\7\37\u024a\n\37\f\37\16\37\u024d\13\37\3\37\5\37\u0250"+
		"\n\37\3 \3 \3 \3 \3 \3 \3 \3 \3 \5 \u025b\n \3!\3!\5!\u025f\n!\3!\3!\3"+
		"!\3!\7!\u0265\n!\f!\16!\u0268\13!\3!\3!\5!\u026c\n!\3!\3!\5!\u0270\n!"+
		"\3\"\3\"\3\"\3#\3#\3#\3#\5#\u0279\n#\3#\3#\3$\3$\3$\3%\3%\3%\3%\3&\7&"+
		"\u0285\n&\f&\16&\u0288\13&\3&\3&\5&\u028c\n&\3\'\3\'\3\'\3\'\3\'\3\'\3"+
		"\'\5\'\u0295\n\'\3(\3(\3(\3(\7(\u029b\n(\f(\16(\u029e\13(\3(\3(\3)\3)"+
		"\3)\7)\u02a5\n)\f)\16)\u02a8\13)\3)\3)\3)\3*\3*\5*\u02af\n*\3*\3*\3*\3"+
		"*\7*\u02b5\n*\f*\16*\u02b8\13*\3*\3*\5*\u02bc\n*\3*\3*\3+\3+\3+\3,\3,"+
		"\3,\7,\u02c6\n,\f,\16,\u02c9\13,\3-\3-\3-\5-\u02ce\n-\3.\3.\3.\7.\u02d3"+
		"\n.\f.\16.\u02d6\13.\3/\3/\5/\u02da\n/\3\60\3\60\3\60\3\60\7\60\u02e0"+
		"\n\60\f\60\16\60\u02e3\13\60\3\60\5\60\u02e6\n\60\5\60\u02e8\n\60\3\60"+
		"\3\60\3\61\3\61\3\62\3\62\3\62\7\62\u02f1\n\62\f\62\16\62\u02f4\13\62"+
		"\3\62\3\62\3\62\7\62\u02f9\n\62\f\62\16\62\u02fc\13\62\5\62\u02fe\n\62"+
		"\3\63\3\63\5\63\u0302\n\63\3\63\3\63\3\63\5\63\u0307\n\63\7\63\u0309\n"+
		"\63\f\63\16\63\u030c\13\63\3\64\3\64\3\65\3\65\3\65\3\65\7\65\u0314\n"+
		"\65\f\65\16\65\u0317\13\65\3\65\3\65\3\66\3\66\3\66\3\66\5\66\u031f\n"+
		"\66\5\66\u0321\n\66\3\67\3\67\3\67\7\67\u0326\n\67\f\67\16\67\u0329\13"+
		"\67\38\38\58\u032d\n8\38\38\39\39\39\79\u0334\n9\f9\169\u0337\139\39\3"+
		"9\59\u033b\n9\39\59\u033e\n9\3:\7:\u0341\n:\f:\16:\u0344\13:\3:\3:\3:"+
		"\3;\7;\u034a\n;\f;\16;\u034d\13;\3;\3;\3;\3;\3<\3<\3=\3=\3>\3>\3>\7>\u035a"+
		"\n>\f>\16>\u035d\13>\3?\3?\3@\3@\3@\3@\3@\5@\u0366\n@\3@\5@\u0369\n@\3"+
		"A\3A\3B\3B\3B\7B\u0370\nB\fB\16B\u0373\13B\3C\3C\3C\3C\3D\3D\3D\5D\u037c"+
		"\nD\3E\3E\3E\3E\7E\u0382\nE\fE\16E\u0385\13E\5E\u0387\nE\3E\5E\u038a\n"+
		"E\3E\3E\3F\3F\3F\3F\3F\3G\3G\7G\u0395\nG\fG\16G\u0398\13G\3G\3G\3H\7H"+
		"\u039d\nH\fH\16H\u03a0\13H\3H\3H\5H\u03a4\nH\3I\3I\3I\3I\3I\3I\5I\u03ac"+
		"\nI\3I\3I\5I\u03b0\nI\3I\3I\5I\u03b4\nI\3I\3I\5I\u03b8\nI\5I\u03ba\nI"+
		"\3J\3J\5J\u03be\nJ\3K\3K\3K\3K\5K\u03c4\nK\3L\3L\3M\3M\3M\3N\3N\7N\u03cd"+
		"\nN\fN\16N\u03d0\13N\3N\3N\3O\3O\3O\5O\u03d7\nO\3P\3P\3P\3Q\7Q\u03dd\n"+
		"Q\fQ\16Q\u03e0\13Q\3Q\3Q\3Q\3R\3R\3R\3R\3R\5R\u03ea\nR\3R\3R\3R\3R\3R"+
		"\3R\3R\5R\u03f3\nR\3R\3R\3R\3R\3R\3R\3R\3R\3R\3R\3R\3R\3R\3R\3R\3R\3R"+
		"\3R\3R\6R\u0408\nR\rR\16R\u0409\3R\5R\u040d\nR\3R\5R\u0410\nR\3R\3R\3"+
		"R\3R\7R\u0416\nR\fR\16R\u0419\13R\3R\5R\u041c\nR\3R\3R\3R\3R\7R\u0422"+
		"\nR\fR\16R\u0425\13R\3R\7R\u0428\nR\fR\16R\u042b\13R\3R\3R\3R\3R\3R\3"+
		"R\3R\3R\5R\u0435\nR\3R\3R\3R\3R\3R\3R\3R\5R\u043e\nR\3R\3R\3R\5R\u0443"+
		"\nR\3R\3R\3R\3R\3R\3R\3R\3R\5R\u044d\nR\3S\3S\3S\7S\u0452\nS\fS\16S\u0455"+
		"\13S\3S\3S\3S\3S\3S\3T\3T\3T\7T\u045f\nT\fT\16T\u0462\13T\3U\3U\3U\3V"+
		"\3V\3V\5V\u046a\nV\3V\3V\3W\3W\3W\7W\u0471\nW\fW\16W\u0474\13W\3X\7X\u0477"+
		"\nX\fX\16X\u047a\13X\3X\3X\3X\3X\3X\3Y\6Y\u0482\nY\rY\16Y\u0483\3Y\6Y"+
		"\u0487\nY\rY\16Y\u0488\3Z\3Z\3Z\3Z\3Z\3Z\3Z\3Z\3Z\3Z\5Z\u0495\nZ\3[\3"+
		"[\5[\u0499\n[\3[\3[\5[\u049d\n[\3[\3[\5[\u04a1\n[\5[\u04a3\n[\3\\\3\\"+
		"\5\\\u04a7\n\\\3]\7]\u04aa\n]\f]\16]\u04ad\13]\3]\3]\3]\3]\3]\3^\3^\3"+
		"_\3_\3_\3_\3`\3`\3`\7`\u04bd\n`\f`\16`\u04c0\13`\3a\3a\3b\3b\3c\3c\3c"+
		"\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\5c\u04d3\nc\3c\3c\3c\3c\3c\3c\3c\3c\3c"+
		"\3c\3c\3c\3c\3c\5c\u04e3\nc\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c"+
		"\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c"+
		"\3c\3c\3c\3c\5c\u050e\nc\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c\3c"+
		"\3c\5c\u0520\nc\3c\3c\3c\3c\3c\3c\7c\u0528\nc\fc\16c\u052b\13c\3d\3d\3"+
		"d\3d\3d\3d\3d\3d\3d\3d\3d\3d\3d\3d\3d\3d\3d\3d\3d\5d\u0540\nd\5d\u0542"+
		"\nd\3e\3e\3e\3e\3e\3e\3e\5e\u054b\ne\5e\u054d\ne\3f\3f\5f\u0551\nf\3f"+
		"\3f\3f\5f\u0556\nf\7f\u0558\nf\ff\16f\u055b\13f\3f\5f\u055e\nf\3g\3g\5"+
		"g\u0562\ng\3g\3g\3h\3h\3h\3h\7h\u056a\nh\fh\16h\u056d\13h\3h\3h\3h\3h"+
		"\3h\3h\3h\7h\u0576\nh\fh\16h\u0579\13h\3h\3h\7h\u057d\nh\fh\16h\u0580"+
		"\13h\5h\u0582\nh\3i\3i\5i\u0586\ni\3j\3j\3j\3k\3k\3k\3k\3l\3l\3l\5l\u0592"+
		"\nl\3m\3m\3m\5m\u0597\nm\3n\3n\3n\3n\5n\u059d\nn\5n\u059f\nn\3o\3o\3o"+
		"\3o\5o\u05a5\no\3p\3p\5p\u05a9\np\3p\3p\3p\2\5\16\20\u00c4q\2\4\6\b\n"+
		"\f\16\20\22\24\26\30\32\34\36 \"$&(*,.\60\62\64\668:<>@BDFHJLNPRTVXZ\\"+
		"^`bdfhjlnprtvxz|~\u0080\u0082\u0084\u0086\u0088\u008a\u008c\u008e\u0090"+
		"\u0092\u0094\u0096\u0098\u009a\u009c\u009e\u00a0\u00a2\u00a4\u00a6\u00a8"+
		"\u00aa\u00ac\u00ae\u00b0\u00b2\u00b4\u00b6\u00b8\u00ba\u00bc\u00be\u00c0"+
		"\u00c2\u00c4\u00c6\u00c8\u00ca\u00cc\u00ce\u00d0\u00d2\u00d4\u00d6\u00d8"+
		"\u00da\u00dc\u00de\2\20\6\2\3\3FFXXii\6\2##//\63\63\66\66\6\2\6\6\27\27"+
		"&(+,\n\2\b\b\n\n\r\r\23\23\31\31  \"\"**\4\2\26\26--\3\28=\3\2TW\3\2J"+
		"K\4\2XY]]\3\2VW\4\2HIOP\4\2NNQQ\4\2GG^h\3\2TU\2\u0631\2\u00e0\3\2\2\2"+
		"\4\u00ee\3\2\2\2\6\u0113\3\2\2\2\b\u0115\3\2\2\2\n\u0128\3\2\2\2\f\u012f"+
		"\3\2\2\2\16\u0135\3\2\2\2\20\u0147\3\2\2\2\22\u0155\3\2\2\2\24\u0176\3"+
		"\2\2\2\26\u0179\3\2\2\2\30\u018c\3\2\2\2\32\u0193\3\2\2\2\34\u01bb\3\2"+
		"\2\2\36\u01bf\3\2\2\2 \u01c3\3\2\2\2\"\u01c7\3\2\2\2$\u01c9\3\2\2\2&\u01d8"+
		"\3\2\2\2(\u01e3\3\2\2\2*\u01e8\3\2\2\2,\u01f0\3\2\2\2.\u0202\3\2\2\2\60"+
		"\u020d\3\2\2\2\62\u0217\3\2\2\2\64\u021e\3\2\2\2\66\u0229\3\2\2\28\u0231"+
		"\3\2\2\2:\u023a\3\2\2\2<\u024f\3\2\2\2>\u025a\3\2\2\2@\u025e\3\2\2\2B"+
		"\u0271\3\2\2\2D\u0274\3\2\2\2F\u027c\3\2\2\2H\u027f\3\2\2\2J\u028b\3\2"+
		"\2\2L\u0294\3\2\2\2N\u0296\3\2\2\2P\u02a1\3\2\2\2R\u02ae\3\2\2\2T\u02bf"+
		"\3\2\2\2V\u02c2\3\2\2\2X\u02ca\3\2\2\2Z\u02cf\3\2\2\2\\\u02d9\3\2\2\2"+
		"^\u02db\3\2\2\2`\u02eb\3\2\2\2b\u02fd\3\2\2\2d\u02ff\3\2\2\2f\u030d\3"+
		"\2\2\2h\u030f\3\2\2\2j\u0320\3\2\2\2l\u0322\3\2\2\2n\u032a\3\2\2\2p\u033d"+
		"\3\2\2\2r\u0342\3\2\2\2t\u034b\3\2\2\2v\u0352\3\2\2\2x\u0354\3\2\2\2z"+
		"\u0356\3\2\2\2|\u035e\3\2\2\2~\u0360\3\2\2\2\u0080\u036a\3\2\2\2\u0082"+
		"\u036c\3\2\2\2\u0084\u0374\3\2\2\2\u0086\u037b\3\2\2\2\u0088\u037d\3\2"+
		"\2\2\u008a\u038d\3\2\2\2\u008c\u0392\3\2\2\2\u008e\u03a3\3\2\2\2\u0090"+
		"\u03b9\3\2\2\2\u0092\u03bd\3\2\2\2\u0094\u03bf\3\2\2\2\u0096\u03c5\3\2"+
		"\2\2\u0098\u03c7\3\2\2\2\u009a\u03ca\3\2\2\2\u009c\u03d6\3\2\2\2\u009e"+
		"\u03d8\3\2\2\2\u00a0\u03de\3\2\2\2\u00a2\u044c\3\2\2\2\u00a4\u044e\3\2"+
		"\2\2\u00a6\u045b\3\2\2\2\u00a8\u0463\3\2\2\2\u00aa\u0466\3\2\2\2\u00ac"+
		"\u046d\3\2\2\2\u00ae\u0478\3\2\2\2\u00b0\u0481\3\2\2\2\u00b2\u0494\3\2"+
		"\2\2\u00b4\u04a2\3\2\2\2\u00b6\u04a6\3\2\2\2\u00b8\u04ab\3\2\2\2\u00ba"+
		"\u04b3\3\2\2\2\u00bc\u04b5\3\2\2\2\u00be\u04b9\3\2\2\2\u00c0\u04c1\3\2"+
		"\2\2\u00c2\u04c3\3\2\2\2\u00c4\u04d2\3\2\2\2\u00c6\u0541\3\2\2\2\u00c8"+
		"\u054c\3\2\2\2\u00ca\u055d\3\2\2\2\u00cc\u055f\3\2\2\2\u00ce\u0565\3\2"+
		"\2\2\u00d0\u0583\3\2\2\2\u00d2\u0587\3\2\2\2\u00d4\u058a\3\2\2\2\u00d6"+
		"\u0591\3\2\2\2\u00d8\u0596\3\2\2\2\u00da\u059e\3\2\2\2\u00dc\u05a4\3\2"+
		"\2\2\u00de\u05a6\3\2\2\2\u00e0\u00e9\5\16\b\2\u00e1\u00e3\7\5\2\2\u00e2"+
		"\u00e1\3\2\2\2\u00e3\u00e6\3\2\2\2\u00e4\u00e2\3\2\2\2\u00e4\u00e5\3\2"+
		"\2\2\u00e5\u00ea\3\2\2\2\u00e6\u00e4\3\2\2\2\u00e7\u00ea\7F\2\2\u00e8"+
		"\u00ea\7\4\2\2\u00e9\u00e4\3\2\2\2\u00e9\u00e7\3\2\2\2\u00e9\u00e8\3\2"+
		"\2\2\u00ea\u00eb\3\2\2\2\u00eb\u00ec\5\24\13\2\u00ec\u00ed\5\4\3\2\u00ed"+
		"\3\3\2\2\2\u00ee\u00f0\7>\2\2\u00ef\u00f1\5\6\4\2\u00f0\u00ef\3\2\2\2"+
		"\u00f0\u00f1\3\2\2\2\u00f1\u00f2\3\2\2\2\u00f2\u00f3\7?\2\2\u00f3\5\3"+
		"\2\2\2\u00f4\u00ff\5\b\5\2\u00f5\u00f9\7E\2\2\u00f6\u00f8\7\5\2\2\u00f7"+
		"\u00f6\3\2\2\2\u00f8\u00fb\3\2\2\2\u00f9\u00f7\3\2\2\2\u00f9\u00fa\3\2"+
		"\2\2\u00fa\u00fc\3\2\2\2\u00fb\u00f9\3\2\2\2\u00fc\u00fe\5\n\6\2\u00fd"+
		"\u00f5\3\2\2\2\u00fe\u0101\3\2\2\2\u00ff\u00fd\3\2\2\2\u00ff\u0100\3\2"+
		"\2\2\u0100\u0114\3\2\2\2\u0101\u00ff\3\2\2\2\u0102\u010d\5\f\7\2\u0103"+
		"\u0107\7E\2\2\u0104\u0106\7\5\2\2\u0105\u0104\3\2\2\2\u0106\u0109\3\2"+
		"\2\2\u0107\u0105\3\2\2\2\u0107\u0108\3\2\2\2\u0108\u010a\3\2\2\2\u0109"+
		"\u0107\3\2\2\2\u010a\u010c\5\6\4\2\u010b\u0103\3\2\2\2\u010c\u010f\3\2"+
		"\2\2\u010d\u010b\3\2\2\2\u010d\u010e\3\2\2\2\u010e\u0114\3\2\2\2\u010f"+
		"\u010d\3\2\2\2\u0110\u0111\5\20\t\2\u0111\u0112\7k\2\2\u0112\u0114\3\2"+
		"\2\2\u0113\u00f4\3\2\2\2\u0113\u0102\3\2\2\2\u0113\u0110\3\2\2\2\u0114"+
		"\7\3\2\2\2\u0115\u0116\7\3\2\2\u0116\t\3\2\2\2\u0117\u0122\5\f\7\2\u0118"+
		"\u011c\7E\2\2\u0119\u011b\7\5\2\2\u011a\u0119\3\2\2\2\u011b\u011e\3\2"+
		"\2\2\u011c\u011a\3\2\2\2\u011c\u011d\3\2\2\2\u011d\u011f\3\2\2\2\u011e"+
		"\u011c\3\2\2\2\u011f\u0121\5\n\6\2\u0120\u0118\3\2\2\2\u0121\u0124\3\2"+
		"\2\2\u0122\u0120\3\2\2\2\u0122\u0123\3\2\2\2\u0123\u0129\3\2\2\2\u0124"+
		"\u0122\3\2\2\2\u0125\u0126\5\20\t\2\u0126\u0127\7k\2\2\u0127\u0129\3\2"+
		"\2\2\u0128\u0117\3\2\2\2\u0128\u0125\3\2\2\2\u0129\13\3\2\2\2\u012a\u012b"+
		"\7>\2\2\u012b\u012c\5\20\t\2\u012c\u012d\7?\2\2\u012d\u0130\3\2\2\2\u012e"+
		"\u0130\5\20\t\2\u012f\u012a\3\2\2\2\u012f\u012e\3\2\2\2\u0130\r\3\2\2"+
		"\2\u0131\u0132\b\b\1\2\u0132\u0136\5\22\n\2\u0133\u0134\7J\2\2\u0134\u0136"+
		"\5\16\b\5\u0135\u0131\3\2\2\2\u0135\u0133\3\2\2\2\u0136\u013f\3\2\2\2"+
		"\u0137\u0138\f\4\2\2\u0138\u0139\7R\2\2\u0139\u013e\5\16\b\5\u013a\u013b"+
		"\f\3\2\2\u013b\u013c\7S\2\2\u013c\u013e\5\16\b\4\u013d\u0137\3\2\2\2\u013d"+
		"\u013a\3\2\2\2\u013e\u0141\3\2\2\2\u013f\u013d\3\2\2\2\u013f\u0140\3\2"+
		"\2\2\u0140\17\3\2\2\2\u0141\u013f\3\2\2\2\u0142\u0143\b\t\1\2\u0143\u0148"+
		"\5\22\n\2\u0144\u0148\5f\64\2\u0145\u0146\7J\2\2\u0146\u0148\5\20\t\5"+
		"\u0147\u0142\3\2\2\2\u0147\u0144\3\2\2\2\u0147\u0145\3\2\2\2\u0148\u0151"+
		"\3\2\2\2\u0149\u014a\f\4\2\2\u014a\u014b\7R\2\2\u014b\u0150\5\20\t\5\u014c"+
		"\u014d\f\3\2\2\u014d\u014e\7S\2\2\u014e\u0150\5\20\t\4\u014f\u0149\3\2"+
		"\2\2\u014f\u014c\3\2\2\2\u0150\u0153\3\2\2\2\u0151\u014f\3\2\2\2\u0151"+
		"\u0152\3\2\2\2\u0152\21\3\2\2\2\u0153\u0151\3\2\2\2\u0154\u0156\t\2\2"+
		"\2\u0155\u0154\3\2\2\2\u0156\u0157\3\2\2\2\u0157\u0155\3\2\2\2\u0157\u0158"+
		"\3\2\2\2\u0158\u015d\3\2\2\2\u0159\u015a\7B\2\2\u015a\u015c\7C\2\2\u015b"+
		"\u0159\3\2\2\2\u015c\u015f\3\2\2\2\u015d\u015b\3\2\2\2\u015d\u015e\3\2"+
		"\2\2\u015e\23\3\2\2\2\u015f\u015d\3\2\2\2\u0160\u0165\7i\2\2\u0161\u0162"+
		"\7X\2\2\u0162\u0164\7i\2\2\u0163\u0161\3\2\2\2\u0164\u0167\3\2\2\2\u0165"+
		"\u0163\3\2\2\2\u0165\u0166\3\2\2\2\u0166\u0169\3\2\2\2\u0167\u0165\3\2"+
		"\2\2\u0168\u016a\7X\2\2\u0169\u0168\3\2\2\2\u0169\u016a\3\2\2\2\u016a"+
		"\u0177\3\2\2\2\u016b\u0170\7X\2\2\u016c\u016d\7i\2\2\u016d\u016f\7X\2"+
		"\2\u016e\u016c\3\2\2\2\u016f\u0172\3\2\2\2\u0170\u016e\3\2\2\2\u0170\u0171"+
		"\3\2\2\2\u0171\u0174\3\2\2\2\u0172\u0170\3\2\2\2\u0173\u0175\7i\2\2\u0174"+
		"\u0173\3\2\2\2\u0174\u0175\3\2\2\2\u0175\u0177\3\2\2\2\u0176\u0160\3\2"+
		"\2\2\u0176\u016b\3\2\2\2\u0177\25\3\2\2\2\u0178\u017a\5\30\r\2\u0179\u0178"+
		"\3\2\2\2\u0179\u017a\3\2\2\2\u017a\u017e\3\2\2\2\u017b\u017d\5\32\16\2"+
		"\u017c\u017b\3\2\2\2\u017d\u0180\3\2\2\2\u017e\u017c\3\2\2\2\u017e\u017f"+
		"\3\2\2\2\u017f\u0184\3\2\2\2\u0180\u017e\3\2\2\2\u0181\u0183\5\34\17\2"+
		"\u0182\u0181\3\2\2\2\u0183\u0186\3\2\2\2\u0184\u0182\3\2\2\2\u0184\u0185"+
		"\3\2\2\2\u0185\u0187\3\2\2\2\u0186\u0184\3\2\2\2\u0187\u0188\7\2\2\3\u0188"+
		"\27\3\2\2\2\u0189\u018b\5~@\2\u018a\u0189\3\2\2\2\u018b\u018e\3\2\2\2"+
		"\u018c\u018a\3\2\2\2\u018c\u018d\3\2\2\2\u018d\u018f\3\2\2\2\u018e\u018c"+
		"\3\2\2\2\u018f\u0190\7%\2\2\u0190\u0191\5z>\2\u0191\u0192\7D\2\2\u0192"+
		"\31\3\2\2\2\u0193\u0195\7\36\2\2\u0194\u0196\7+\2\2\u0195\u0194\3\2\2"+
		"\2\u0195\u0196\3\2\2\2\u0196\u0197\3\2\2\2\u0197\u019a\5z>\2\u0198\u0199"+
		"\7F\2\2\u0199\u019b\7X\2\2\u019a\u0198\3\2\2\2\u019a\u019b\3\2\2\2\u019b"+
		"\u019c\3\2\2\2\u019c\u019d\7D\2\2\u019d\33\3\2\2\2\u019e\u01a0\5 \21\2"+
		"\u019f\u019e\3\2\2\2\u01a0\u01a3\3\2\2\2\u01a1\u019f\3\2\2\2\u01a1\u01a2"+
		"\3\2\2\2\u01a2\u01a4\3\2\2\2\u01a3\u01a1\3\2\2\2\u01a4\u01bc\5$\23\2\u01a5"+
		"\u01a7\5 \21\2\u01a6\u01a5\3\2\2\2\u01a7\u01aa\3\2\2\2\u01a8\u01a6\3\2"+
		"\2\2\u01a8\u01a9\3\2\2\2\u01a9\u01ab\3\2\2\2\u01aa\u01a8\3\2\2\2\u01ab"+
		"\u01bc\5,\27\2\u01ac\u01ae\5 \21\2\u01ad\u01ac\3\2\2\2\u01ae\u01b1\3\2"+
		"\2\2\u01af\u01ad\3\2\2\2\u01af\u01b0\3\2\2\2\u01b0\u01b2\3\2\2\2\u01b1"+
		"\u01af\3\2\2\2\u01b2\u01bc\5\64\33\2\u01b3\u01b5\5 \21\2\u01b4\u01b3\3"+
		"\2\2\2\u01b5\u01b8\3\2\2\2\u01b6\u01b4\3\2\2\2\u01b6\u01b7\3\2\2\2\u01b7"+
		"\u01b9\3\2\2\2\u01b8\u01b6\3\2\2\2\u01b9\u01bc\5\u008aF\2\u01ba\u01bc"+
		"\7D\2\2\u01bb\u01a1\3\2\2\2\u01bb\u01a8\3\2\2\2\u01bb\u01af\3\2\2\2\u01bb"+
		"\u01b6\3\2\2\2\u01bb\u01ba\3\2\2\2\u01bc\35\3\2\2\2\u01bd\u01c0\5 \21"+
		"\2\u01be\u01c0\t\3\2\2\u01bf\u01bd\3\2\2\2\u01bf\u01be\3\2\2\2\u01c0\37"+
		"\3\2\2\2\u01c1\u01c4\5~@\2\u01c2\u01c4\t\4\2\2\u01c3\u01c1\3\2\2\2\u01c3"+
		"\u01c2\3\2\2\2\u01c4!\3\2\2\2\u01c5\u01c8\7\27\2\2\u01c6\u01c8\5~@\2\u01c7"+
		"\u01c5\3\2\2\2\u01c7\u01c6\3\2\2\2\u01c8#\3\2\2\2\u01c9\u01ca\7\16\2\2"+
		"\u01ca\u01cc\7i\2\2\u01cb\u01cd\5&\24\2\u01cc\u01cb\3\2\2\2\u01cc\u01cd"+
		"\3\2\2\2\u01cd\u01d0\3\2\2\2\u01ce\u01cf\7\26\2\2\u01cf\u01d1\5b\62\2"+
		"\u01d0\u01ce\3\2\2\2\u01d0\u01d1\3\2\2\2\u01d1\u01d4\3\2\2\2\u01d2\u01d3"+
		"\7\35\2\2\u01d3\u01d5\5\66\34\2\u01d4\u01d2\3\2\2\2\u01d4\u01d5\3\2\2"+
		"\2\u01d5\u01d6\3\2\2\2\u01d6\u01d7\58\35\2\u01d7%\3\2\2\2\u01d8\u01d9"+
		"\7I\2\2\u01d9\u01de\5(\25\2\u01da\u01db\7E\2\2\u01db\u01dd\5(\25\2\u01dc"+
		"\u01da\3\2\2\2\u01dd\u01e0\3\2\2\2\u01de\u01dc\3\2\2\2\u01de\u01df\3\2"+
		"\2\2\u01df\u01e1\3\2\2\2\u01e0\u01de\3\2\2\2\u01e1\u01e2\7H\2\2\u01e2"+
		"\'\3\2\2\2\u01e3\u01e6\7i\2\2\u01e4\u01e5\7\26\2\2\u01e5\u01e7\5*\26\2"+
		"\u01e6\u01e4\3\2\2\2\u01e6\u01e7\3\2\2\2\u01e7)\3\2\2\2\u01e8\u01ed\5"+
		"b\62\2\u01e9\u01ea\7Z\2\2\u01ea\u01ec\5b\62\2\u01eb\u01e9\3\2\2\2\u01ec"+
		"\u01ef\3\2\2\2\u01ed\u01eb\3\2\2\2\u01ed\u01ee\3\2\2\2\u01ee+\3\2\2\2"+
		"\u01ef\u01ed\3\2\2\2\u01f0\u01f1\7\25\2\2\u01f1\u01f4\7i\2\2\u01f2\u01f3"+
		"\7\35\2\2\u01f3\u01f5\5\66\34\2\u01f4\u01f2\3\2\2\2\u01f4\u01f5\3\2\2"+
		"\2\u01f5\u01f6\3\2\2\2\u01f6\u01f8\7@\2\2\u01f7\u01f9\5.\30\2\u01f8\u01f7"+
		"\3\2\2\2\u01f8\u01f9\3\2\2\2\u01f9\u01fb\3\2\2\2\u01fa\u01fc\7E\2\2\u01fb"+
		"\u01fa\3\2\2\2\u01fb\u01fc\3\2\2\2\u01fc\u01fe\3\2\2\2\u01fd\u01ff\5\62"+
		"\32\2\u01fe\u01fd\3\2\2\2\u01fe\u01ff\3\2\2\2\u01ff\u0200\3\2\2\2\u0200"+
		"\u0201\7A\2\2\u0201-\3\2\2\2\u0202\u0207\5\60\31\2\u0203\u0204\7E\2\2"+
		"\u0204\u0206\5\60\31\2\u0205\u0203\3\2\2\2\u0206\u0209\3\2\2\2\u0207\u0205"+
		"\3\2\2\2\u0207\u0208\3\2\2\2\u0208/\3\2\2\2\u0209\u0207\3\2\2\2\u020a"+
		"\u020c\5~@\2\u020b\u020a\3\2\2\2\u020c\u020f\3\2\2\2\u020d\u020b\3\2\2"+
		"\2\u020d\u020e\3\2\2\2\u020e\u0210\3\2\2\2\u020f\u020d\3\2\2\2\u0210\u0212"+
		"\7i\2\2\u0211\u0213\5\u00dep\2\u0212\u0211\3\2\2\2\u0212\u0213\3\2\2\2"+
		"\u0213\u0215\3\2\2\2\u0214\u0216\58\35\2\u0215\u0214\3\2\2\2\u0215\u0216"+
		"\3\2\2\2\u0216\61\3\2\2\2\u0217\u021b\7D\2\2\u0218\u021a\5<\37\2\u0219"+
		"\u0218\3\2\2\2\u021a\u021d\3\2\2\2\u021b\u0219\3\2\2\2\u021b\u021c\3\2"+
		"\2\2\u021c\63\3\2\2\2\u021d\u021b\3\2\2\2\u021e\u021f\7!\2\2\u021f\u0221"+
		"\7i\2\2\u0220\u0222\5&\24\2\u0221\u0220\3\2\2\2\u0221\u0222\3\2\2\2\u0222"+
		"\u0225\3\2\2\2\u0223\u0224\7\26\2\2\u0224\u0226\5\66\34\2\u0225\u0223"+
		"\3\2\2\2\u0225\u0226\3\2\2\2\u0226\u0227\3\2\2\2\u0227\u0228\5:\36\2\u0228"+
		"\65\3\2\2\2\u0229\u022e\5b\62\2\u022a\u022b\7E\2\2\u022b\u022d\5b\62\2"+
		"\u022c\u022a\3\2\2\2\u022d\u0230\3\2\2\2\u022e\u022c\3\2\2\2\u022e\u022f"+
		"\3\2\2\2\u022f\67\3\2\2\2\u0230\u022e\3\2\2\2\u0231\u0235\7@\2\2\u0232"+
		"\u0234\5<\37\2\u0233\u0232\3\2\2\2\u0234\u0237\3\2\2\2\u0235\u0233\3\2"+
		"\2\2\u0235\u0236\3\2\2\2\u0236\u0238\3\2\2\2\u0237\u0235\3\2\2\2\u0238"+
		"\u0239\7A\2\2\u02399\3\2\2\2\u023a\u023e\7@\2\2\u023b\u023d\5J&\2\u023c"+
		"\u023b\3\2\2\2\u023d\u0240\3\2\2\2\u023e\u023c\3\2\2\2\u023e\u023f\3\2"+
		"\2\2\u023f\u0241\3\2\2\2\u0240\u023e\3\2\2\2\u0241\u0242\7A\2\2\u0242"+
		";\3\2\2\2\u0243\u0250\7D\2\2\u0244\u0246\7+\2\2\u0245\u0244\3\2\2\2\u0245"+
		"\u0246\3\2\2\2\u0246\u0247\3\2\2\2\u0247\u0250\5\u009aN\2\u0248\u024a"+
		"\5\36\20\2\u0249\u0248\3\2\2\2\u024a\u024d\3\2\2\2\u024b\u0249\3\2\2\2"+
		"\u024b\u024c\3\2\2\2\u024c\u024e\3\2\2\2\u024d\u024b\3\2\2\2\u024e\u0250"+
		"\5> \2\u024f\u0243\3\2\2\2\u024f\u0245\3\2\2\2\u024f\u024b\3\2\2\2\u0250"+
		"=\3\2\2\2\u0251\u025b\5@!\2\u0252\u025b\5B\"\2\u0253\u025b\5H%\2\u0254"+
		"\u025b\5D#\2\u0255\u025b\5F$\2\u0256\u025b\5\64\33\2\u0257\u025b\5\u008a"+
		"F\2\u0258\u025b\5$\23\2\u0259\u025b\5,\27\2\u025a\u0251\3\2\2\2\u025a"+
		"\u0252\3\2\2\2\u025a\u0253\3\2\2\2\u025a\u0254\3\2\2\2\u025a\u0255\3\2"+
		"\2\2\u025a\u0256\3\2\2\2\u025a\u0257\3\2\2\2\u025a\u0258\3\2\2\2\u025a"+
		"\u0259\3\2\2\2\u025b?\3\2\2\2\u025c\u025f\5b\62\2\u025d\u025f\7\65\2\2"+
		"\u025e\u025c\3\2\2\2\u025e\u025d\3\2\2\2\u025f\u0260\3\2\2\2\u0260\u0261"+
		"\7i\2\2\u0261\u0266\5n8\2\u0262\u0263\7B\2\2\u0263\u0265\7C\2\2\u0264"+
		"\u0262\3\2\2\2\u0265\u0268\3\2\2\2\u0266\u0264\3\2\2\2\u0266\u0267\3\2"+
		"\2\2\u0267\u026b\3\2\2\2\u0268\u0266\3\2\2\2\u0269\u026a\7\62\2\2\u026a"+
		"\u026c\5l\67\2\u026b\u0269\3\2\2\2\u026b\u026c\3\2\2\2\u026c\u026f\3\2"+
		"\2\2\u026d\u0270\5v<\2\u026e\u0270\7D\2\2\u026f\u026d\3\2\2\2\u026f\u026e"+
		"\3\2\2\2\u0270A\3\2\2\2\u0271\u0272\5&\24\2\u0272\u0273\5@!\2\u0273C\3"+
		"\2\2\2\u0274\u0275\7i\2\2\u0275\u0278\5n8\2\u0276\u0277\7\62\2\2\u0277"+
		"\u0279\5l\67\2\u0278\u0276\3\2\2\2\u0278\u0279\3\2\2\2\u0279\u027a\3\2"+
		"\2\2\u027a\u027b\5x=\2\u027bE\3\2\2\2\u027c\u027d\5&\24\2\u027d\u027e"+
		"\5D#\2\u027eG\3\2\2\2\u027f\u0280\5b\62\2\u0280\u0281\5V,\2\u0281\u0282"+
		"\7D\2\2\u0282I\3\2\2\2\u0283\u0285\5\36\20\2\u0284\u0283\3\2\2\2\u0285"+
		"\u0288\3\2\2\2\u0286\u0284\3\2\2\2\u0286\u0287\3\2\2\2\u0287\u0289\3\2"+
		"\2\2\u0288\u0286\3\2\2\2\u0289\u028c\5L\'\2\u028a\u028c\7D\2\2\u028b\u0286"+
		"\3\2\2\2\u028b\u028a\3\2\2\2\u028cK\3\2\2\2\u028d\u0295\5N(\2\u028e\u0295"+
		"\5R*\2\u028f\u0295\5T+\2\u0290\u0295\5\64\33\2\u0291\u0295\5\u008aF\2"+
		"\u0292\u0295\5$\23\2\u0293\u0295\5,\27\2\u0294\u028d\3\2\2\2\u0294\u028e"+
		"\3\2\2\2\u0294\u028f\3\2\2\2\u0294\u0290\3\2\2\2\u0294\u0291\3\2\2\2\u0294"+
		"\u0292\3\2\2\2\u0294\u0293\3\2\2\2\u0295M\3\2\2\2\u0296\u0297\5b\62\2"+
		"\u0297\u029c\5P)\2\u0298\u0299\7E\2\2\u0299\u029b\5P)\2\u029a\u0298\3"+
		"\2\2\2\u029b\u029e\3\2\2\2\u029c\u029a\3\2\2\2\u029c\u029d\3\2\2\2\u029d"+
		"\u029f\3\2\2\2\u029e\u029c\3\2\2\2\u029f\u02a0\7D\2\2\u02a0O\3\2\2\2\u02a1"+
		"\u02a6\7i\2\2\u02a2\u02a3\7B\2\2\u02a3\u02a5\7C\2\2\u02a4\u02a2\3\2\2"+
		"\2\u02a5\u02a8\3\2\2\2\u02a6\u02a4\3\2\2\2\u02a6\u02a7\3\2\2\2\u02a7\u02a9"+
		"\3\2\2\2\u02a8\u02a6\3\2\2\2\u02a9\u02aa\7G\2\2\u02aa\u02ab\5\\/\2\u02ab"+
		"Q\3\2\2\2\u02ac\u02af\5b\62\2\u02ad\u02af\7\65\2\2\u02ae\u02ac\3\2\2\2"+
		"\u02ae\u02ad\3\2\2\2\u02af\u02b0\3\2\2\2\u02b0\u02b1\7i\2\2\u02b1\u02b6"+
		"\5n8\2\u02b2\u02b3\7B\2\2\u02b3\u02b5\7C\2\2\u02b4\u02b2\3\2\2\2\u02b5"+
		"\u02b8\3\2\2\2\u02b6\u02b4\3\2\2\2\u02b6\u02b7\3\2\2\2\u02b7\u02bb\3\2"+
		"\2\2\u02b8\u02b6\3\2\2\2\u02b9\u02ba\7\62\2\2\u02ba\u02bc\5l\67\2\u02bb"+
		"\u02b9\3\2\2\2\u02bb\u02bc\3\2\2\2\u02bc\u02bd\3\2\2\2\u02bd\u02be\7D"+
		"\2\2\u02beS\3\2\2\2\u02bf\u02c0\5&\24\2\u02c0\u02c1\5R*\2\u02c1U\3\2\2"+
		"\2\u02c2\u02c7\5X-\2\u02c3\u02c4\7E\2\2\u02c4\u02c6\5X-\2\u02c5\u02c3"+
		"\3\2\2\2\u02c6\u02c9\3\2\2\2\u02c7\u02c5\3\2\2\2\u02c7\u02c8\3\2\2\2\u02c8"+
		"W\3\2\2\2\u02c9\u02c7\3\2\2\2\u02ca\u02cd\5Z.\2\u02cb\u02cc\7G\2\2\u02cc"+
		"\u02ce\5\\/\2\u02cd\u02cb\3\2\2\2\u02cd\u02ce\3\2\2\2\u02ceY\3\2\2\2\u02cf"+
		"\u02d4\7i\2\2\u02d0\u02d1\7B\2\2\u02d1\u02d3\7C\2\2\u02d2\u02d0\3\2\2"+
		"\2\u02d3\u02d6\3\2\2\2\u02d4\u02d2\3\2\2\2\u02d4\u02d5\3\2\2\2\u02d5["+
		"\3\2\2\2\u02d6\u02d4\3\2\2\2\u02d7\u02da\5^\60\2\u02d8\u02da\5\u00c4c"+
		"\2\u02d9\u02d7\3\2\2\2\u02d9\u02d8\3\2\2\2\u02da]\3\2\2\2\u02db\u02e7"+
		"\7@\2\2\u02dc\u02e1\5\\/\2\u02dd\u02de\7E\2\2\u02de\u02e0\5\\/\2\u02df"+
		"\u02dd\3\2\2\2\u02e0\u02e3\3\2\2\2\u02e1\u02df\3\2\2\2\u02e1\u02e2\3\2"+
		"\2\2\u02e2\u02e5\3\2\2\2\u02e3\u02e1\3\2\2\2\u02e4\u02e6\7E\2\2\u02e5"+
		"\u02e4\3\2\2\2\u02e5\u02e6\3\2\2\2\u02e6\u02e8\3\2\2\2\u02e7\u02dc\3\2"+
		"\2\2\u02e7\u02e8\3\2\2\2\u02e8\u02e9\3\2\2\2\u02e9\u02ea\7A\2\2\u02ea"+
		"_\3\2\2\2\u02eb\u02ec\7i\2\2\u02eca\3\2\2\2\u02ed\u02f2\5d\63\2\u02ee"+
		"\u02ef\7B\2\2\u02ef\u02f1\7C\2\2\u02f0\u02ee\3\2\2\2\u02f1\u02f4\3\2\2"+
		"\2\u02f2\u02f0\3\2\2\2\u02f2\u02f3\3\2\2\2\u02f3\u02fe\3\2\2\2\u02f4\u02f2"+
		"\3\2\2\2\u02f5\u02fa\5f\64\2\u02f6\u02f7\7B\2\2\u02f7\u02f9\7C\2\2\u02f8"+
		"\u02f6\3\2\2\2\u02f9\u02fc\3\2\2\2\u02fa\u02f8\3\2\2\2\u02fa\u02fb\3\2"+
		"\2\2\u02fb\u02fe\3\2\2\2\u02fc\u02fa\3\2\2\2\u02fd\u02ed\3\2\2\2\u02fd"+
		"\u02f5\3\2\2\2\u02fec\3\2\2\2\u02ff\u0301\7i\2\2\u0300\u0302\5h\65\2\u0301"+
		"\u0300\3\2\2\2\u0301\u0302\3\2\2\2\u0302\u030a\3\2\2\2\u0303\u0304\7F"+
		"\2\2\u0304\u0306\7i\2\2\u0305\u0307\5h\65\2\u0306\u0305\3\2\2\2\u0306"+
		"\u0307\3\2\2\2\u0307\u0309\3\2\2\2\u0308\u0303\3\2\2\2\u0309\u030c\3\2"+
		"\2\2\u030a\u0308\3\2\2\2\u030a\u030b\3\2\2\2\u030be\3\2\2\2\u030c\u030a"+
		"\3\2\2\2\u030d\u030e\t\5\2\2\u030eg\3\2\2\2\u030f\u0310\7I\2\2\u0310\u0315"+
		"\5j\66\2\u0311\u0312\7E\2\2\u0312\u0314\5j\66\2\u0313\u0311\3\2\2\2\u0314"+
		"\u0317\3\2\2\2\u0315\u0313\3\2\2\2\u0315\u0316\3\2\2\2\u0316\u0318\3\2"+
		"\2\2\u0317\u0315\3\2\2\2\u0318\u0319\7H\2\2\u0319i\3\2\2\2\u031a\u0321"+
		"\5b\62\2\u031b\u031e\7L\2\2\u031c\u031d\t\6\2\2\u031d\u031f\5b\62\2\u031e"+
		"\u031c\3\2\2\2\u031e\u031f\3\2\2\2\u031f\u0321\3\2\2\2\u0320\u031a\3\2"+
		"\2\2\u0320\u031b\3\2\2\2\u0321k\3\2\2\2\u0322\u0327\5z>\2\u0323\u0324"+
		"\7E\2\2\u0324\u0326\5z>\2\u0325\u0323\3\2\2\2\u0326\u0329\3\2\2\2\u0327"+
		"\u0325\3\2\2\2\u0327\u0328\3\2\2\2\u0328m\3\2\2\2\u0329\u0327\3\2\2\2"+
		"\u032a\u032c\7>\2\2\u032b\u032d\5p9\2\u032c\u032b\3\2\2\2\u032c\u032d"+
		"\3\2\2\2\u032d\u032e\3\2\2\2\u032e\u032f\7?\2\2\u032fo\3\2\2\2\u0330\u0335"+
		"\5r:\2\u0331\u0332\7E\2\2\u0332\u0334\5r:\2\u0333\u0331\3\2\2\2\u0334"+
		"\u0337\3\2\2\2\u0335\u0333\3\2\2\2\u0335\u0336\3\2\2\2\u0336\u033a\3\2"+
		"\2\2\u0337\u0335\3\2\2\2\u0338\u0339\7E\2\2\u0339\u033b\5t;\2\u033a\u0338"+
		"\3\2\2\2\u033a\u033b\3\2\2\2\u033b\u033e\3\2\2\2\u033c\u033e\5t;\2\u033d"+
		"\u0330\3\2\2\2\u033d\u033c\3\2\2\2\u033eq\3\2\2\2\u033f\u0341\5\"\22\2"+
		"\u0340\u033f\3\2\2\2\u0341\u0344\3\2\2\2\u0342\u0340\3\2\2\2\u0342\u0343"+
		"\3\2\2\2\u0343\u0345\3\2\2\2\u0344\u0342\3\2\2\2\u0345\u0346\5b\62\2\u0346"+
		"\u0347\5Z.\2\u0347s\3\2\2\2\u0348\u034a\5\"\22\2\u0349\u0348\3\2\2\2\u034a"+
		"\u034d\3\2\2\2\u034b\u0349\3\2\2\2\u034b\u034c\3\2\2\2\u034c\u034e\3\2"+
		"\2\2\u034d\u034b\3\2\2\2\u034e\u034f\5b\62\2\u034f\u0350\7k\2\2\u0350"+
		"\u0351\5Z.\2\u0351u\3\2\2\2\u0352\u0353\5\u009aN\2\u0353w\3\2\2\2\u0354"+
		"\u0355\5\u009aN\2\u0355y\3\2\2\2\u0356\u035b\7i\2\2\u0357\u0358\7F\2\2"+
		"\u0358\u035a\7i\2\2\u0359\u0357\3\2\2\2\u035a\u035d\3\2\2\2\u035b\u0359"+
		"\3\2\2\2\u035b\u035c\3\2\2\2\u035c{\3\2\2\2\u035d\u035b\3\2\2\2\u035e"+
		"\u035f\t\7\2\2\u035f}\3\2\2\2\u0360\u0361\7j\2\2\u0361\u0368\5\u0080A"+
		"\2\u0362\u0365\7>\2\2\u0363\u0366\5\u0082B\2\u0364\u0366\5\u0086D\2\u0365"+
		"\u0363\3\2\2\2\u0365\u0364\3\2\2\2\u0365\u0366\3\2\2\2\u0366\u0367\3\2"+
		"\2\2\u0367\u0369\7?\2\2\u0368\u0362\3\2\2\2\u0368\u0369\3\2\2\2\u0369"+
		"\177\3\2\2\2\u036a\u036b\5z>\2\u036b\u0081\3\2\2\2\u036c\u0371\5\u0084"+
		"C\2\u036d\u036e\7E\2\2\u036e\u0370\5\u0084C\2\u036f\u036d\3\2\2\2\u0370"+
		"\u0373\3\2\2\2\u0371\u036f\3\2\2\2\u0371\u0372\3\2\2\2\u0372\u0083\3\2"+
		"\2\2\u0373\u0371\3\2\2\2\u0374\u0375\7i\2\2\u0375\u0376\7G\2\2\u0376\u0377"+
		"\5\u0086D\2\u0377\u0085\3\2\2\2\u0378\u037c\5\u00c4c\2\u0379\u037c\5~"+
		"@\2\u037a\u037c\5\u0088E\2\u037b\u0378\3\2\2\2\u037b\u0379\3\2\2\2\u037b"+
		"\u037a\3\2\2\2\u037c\u0087\3\2\2\2\u037d\u0386\7@\2\2\u037e\u0383\5\u0086"+
		"D\2\u037f\u0380\7E\2\2\u0380\u0382\5\u0086D\2\u0381\u037f\3\2\2\2\u0382"+
		"\u0385\3\2\2\2\u0383\u0381\3\2\2\2\u0383\u0384\3\2\2\2\u0384\u0387\3\2"+
		"\2\2\u0385\u0383\3\2\2\2\u0386\u037e\3\2\2\2\u0386\u0387\3\2\2\2\u0387"+
		"\u0389\3\2\2\2\u0388\u038a\7E\2\2\u0389\u0388\3\2\2\2\u0389\u038a\3\2"+
		"\2\2\u038a\u038b\3\2\2\2\u038b\u038c\7A\2\2\u038c\u0089\3\2\2\2\u038d"+
		"\u038e\7j\2\2\u038e\u038f\7!\2\2\u038f\u0390\7i\2\2\u0390\u0391\5\u008c"+
		"G\2\u0391\u008b\3\2\2\2\u0392\u0396\7@\2\2\u0393\u0395\5\u008eH\2\u0394"+
		"\u0393\3\2\2\2\u0395\u0398\3\2\2\2\u0396\u0394\3\2\2\2\u0396\u0397\3\2"+
		"\2\2\u0397\u0399\3\2\2\2\u0398\u0396\3\2\2\2\u0399\u039a\7A\2\2\u039a"+
		"\u008d\3\2\2\2\u039b\u039d\5\36\20\2\u039c\u039b\3\2\2\2\u039d\u03a0\3"+
		"\2\2\2\u039e\u039c\3\2\2\2\u039e\u039f\3\2\2\2\u039f\u03a1\3\2\2\2\u03a0"+
		"\u039e\3\2\2\2\u03a1\u03a4\5\u0090I\2\u03a2\u03a4\7D\2\2\u03a3\u039e\3"+
		"\2\2\2\u03a3\u03a2\3\2\2\2\u03a4\u008f\3\2\2\2\u03a5\u03a6\5b\62\2\u03a6"+
		"\u03a7\5\u0092J\2\u03a7\u03a8\7D\2\2\u03a8\u03ba\3\2\2\2\u03a9\u03ab\5"+
		"$\23\2\u03aa\u03ac\7D\2\2\u03ab\u03aa\3\2\2\2\u03ab\u03ac\3\2\2\2\u03ac"+
		"\u03ba\3\2\2\2\u03ad\u03af\5\64\33\2\u03ae\u03b0\7D\2\2\u03af\u03ae\3"+
		"\2\2\2\u03af\u03b0\3\2\2\2\u03b0\u03ba\3\2\2\2\u03b1\u03b3\5,\27\2\u03b2"+
		"\u03b4\7D\2\2\u03b3\u03b2\3\2\2\2\u03b3\u03b4\3\2\2\2\u03b4\u03ba\3\2"+
		"\2\2\u03b5\u03b7\5\u008aF\2\u03b6\u03b8\7D\2\2\u03b7\u03b6\3\2\2\2\u03b7"+
		"\u03b8\3\2\2\2\u03b8\u03ba\3\2\2\2\u03b9\u03a5\3\2\2\2\u03b9\u03a9\3\2"+
		"\2\2\u03b9\u03ad\3\2\2\2\u03b9\u03b1\3\2\2\2\u03b9\u03b5\3\2\2\2\u03ba"+
		"\u0091\3\2\2\2\u03bb\u03be\5\u0094K\2\u03bc\u03be\5\u0096L\2\u03bd\u03bb"+
		"\3\2\2\2\u03bd\u03bc\3\2\2\2\u03be\u0093\3\2\2\2\u03bf\u03c0\7i\2\2\u03c0"+
		"\u03c1\7>\2\2\u03c1\u03c3\7?\2\2\u03c2\u03c4\5\u0098M\2\u03c3\u03c2\3"+
		"\2\2\2\u03c3\u03c4\3\2\2\2\u03c4\u0095\3\2\2\2\u03c5\u03c6\5V,\2\u03c6"+
		"\u0097\3\2\2\2\u03c7\u03c8\7\21\2\2\u03c8\u03c9\5\u0086D\2\u03c9\u0099"+
		"\3\2\2\2\u03ca\u03ce\7@\2\2\u03cb\u03cd\5\u009cO\2\u03cc\u03cb\3\2\2\2"+
		"\u03cd\u03d0\3\2\2\2\u03ce\u03cc\3\2\2\2\u03ce\u03cf\3\2\2\2\u03cf\u03d1"+
		"\3\2\2\2\u03d0\u03ce\3\2\2\2\u03d1\u03d2\7A\2\2\u03d2\u009b\3\2\2\2\u03d3"+
		"\u03d7\5\u009eP\2\u03d4\u03d7\5\u00a2R\2\u03d5\u03d7\5\34\17\2\u03d6\u03d3"+
		"\3\2\2\2\u03d6\u03d4\3\2\2\2\u03d6\u03d5\3\2\2\2\u03d7\u009d\3\2\2\2\u03d8"+
		"\u03d9\5\u00a0Q\2\u03d9\u03da\7D\2\2\u03da\u009f\3\2\2\2\u03db\u03dd\5"+
		"\"\22\2\u03dc\u03db\3\2\2\2\u03dd\u03e0\3\2\2\2\u03de\u03dc\3\2\2\2\u03de"+
		"\u03df\3\2\2\2\u03df\u03e1\3\2\2\2\u03e0\u03de\3\2\2\2\u03e1\u03e2\5b"+
		"\62\2\u03e2\u03e3\5V,\2\u03e3\u00a1\3\2\2\2\u03e4\u044d\5\u009aN\2\u03e5"+
		"\u03e6\7\7\2\2\u03e6\u03e9\5\u00c4c\2\u03e7\u03e8\7M\2\2\u03e8\u03ea\5"+
		"\u00c4c\2\u03e9\u03e7\3\2\2\2\u03e9\u03ea\3\2\2\2\u03ea\u03eb\3\2\2\2"+
		"\u03eb\u03ec\7D\2\2\u03ec\u044d\3\2\2\2\u03ed\u03ee\7\33\2\2\u03ee\u03ef"+
		"\5\u00bc_\2\u03ef\u03f2\5\u00a2R\2\u03f0\u03f1\7\24\2\2\u03f1\u03f3\5"+
		"\u00a2R\2\u03f2\u03f0\3\2\2\2\u03f2\u03f3\3\2\2\2\u03f3\u044d\3\2\2\2"+
		"\u03f4\u03f5\7\32\2\2\u03f5\u03f6\7>\2\2\u03f6\u03f7\5\u00b4[\2\u03f7"+
		"\u03f8\7?\2\2\u03f8\u03f9\5\u00a2R\2\u03f9\u044d\3\2\2\2\u03fa\u03fb\7"+
		"\67\2\2\u03fb\u03fc\5\u00bc_\2\u03fc\u03fd\5\u00a2R\2\u03fd\u044d\3\2"+
		"\2\2\u03fe\u03ff\7\22\2\2\u03ff\u0400\5\u00a2R\2\u0400\u0401\7\67\2\2"+
		"\u0401\u0402\5\u00bc_\2\u0402\u0403\7D\2\2\u0403\u044d\3\2\2\2\u0404\u0405"+
		"\7\64\2\2\u0405\u040f\5\u009aN\2\u0406\u0408\5\u00a4S\2\u0407\u0406\3"+
		"\2\2\2\u0408\u0409\3\2\2\2\u0409\u0407\3\2\2\2\u0409\u040a\3\2\2\2\u040a"+
		"\u040c\3\2\2\2\u040b\u040d\5\u00a8U\2\u040c\u040b\3\2\2\2\u040c\u040d"+
		"\3\2\2\2\u040d\u0410\3\2\2\2\u040e\u0410\5\u00a8U\2\u040f\u0407\3\2\2"+
		"\2\u040f\u040e\3\2\2\2\u0410\u044d\3\2\2\2\u0411\u0412\7\64\2\2\u0412"+
		"\u0413\5\u00aaV\2\u0413\u0417\5\u009aN\2\u0414\u0416\5\u00a4S\2\u0415"+
		"\u0414\3\2\2\2\u0416\u0419\3\2\2\2\u0417\u0415\3\2\2\2\u0417\u0418\3\2"+
		"\2\2\u0418\u041b\3\2\2\2\u0419\u0417\3\2\2\2\u041a\u041c\5\u00a8U\2\u041b"+
		"\u041a\3\2\2\2\u041b\u041c\3\2\2\2\u041c\u044d\3\2\2\2\u041d\u041e\7."+
		"\2\2\u041e\u041f\5\u00bc_\2\u041f\u0423\7@\2\2\u0420\u0422\5\u00b0Y\2"+
		"\u0421\u0420\3\2\2\2\u0422\u0425\3\2\2\2\u0423\u0421\3\2\2\2\u0423\u0424"+
		"\3\2\2\2\u0424\u0429\3\2\2\2\u0425\u0423\3\2\2\2\u0426\u0428\5\u00b2Z"+
		"\2\u0427\u0426\3\2\2\2\u0428\u042b\3\2\2\2\u0429\u0427\3\2\2\2\u0429\u042a"+
		"\3\2\2\2\u042a\u042c\3\2\2\2\u042b\u0429\3\2\2\2\u042c\u042d\7A\2\2\u042d"+
		"\u044d\3\2\2\2\u042e\u042f\7/\2\2\u042f\u0430\5\u00bc_\2\u0430\u0431\5"+
		"\u009aN\2\u0431\u044d\3\2\2\2\u0432\u0434\7)\2\2\u0433\u0435\5\u00c4c"+
		"\2\u0434\u0433\3\2\2\2\u0434\u0435\3\2\2\2\u0435\u0436\3\2\2\2\u0436\u044d"+
		"\7D\2\2\u0437\u0438\7\61\2\2\u0438\u0439\5\u00c4c\2\u0439\u043a\7D\2\2"+
		"\u043a\u044d\3\2\2\2\u043b\u043d\7\t\2\2\u043c\u043e\7i\2\2\u043d\u043c"+
		"\3\2\2\2\u043d\u043e\3\2\2\2\u043e\u043f\3\2\2\2\u043f\u044d\7D\2\2\u0440"+
		"\u0442\7\20\2\2\u0441\u0443\7i\2\2\u0442\u0441\3\2\2\2\u0442\u0443\3\2"+
		"\2\2\u0443\u0444\3\2\2\2\u0444\u044d\7D\2\2\u0445\u044d\7D\2\2\u0446\u0447"+
		"\5\u00c0a\2\u0447\u0448\7D\2\2\u0448\u044d\3\2\2\2\u0449\u044a\7i\2\2"+
		"\u044a\u044b\7M\2\2\u044b\u044d\5\u00a2R\2\u044c\u03e4\3\2\2\2\u044c\u03e5"+
		"\3\2\2\2\u044c\u03ed\3\2\2\2\u044c\u03f4\3\2\2\2\u044c\u03fa\3\2\2\2\u044c"+
		"\u03fe\3\2\2\2\u044c\u0404\3\2\2\2\u044c\u0411\3\2\2\2\u044c\u041d\3\2"+
		"\2\2\u044c\u042e\3\2\2\2\u044c\u0432\3\2\2\2\u044c\u0437\3\2\2\2\u044c"+
		"\u043b\3\2\2\2\u044c\u0440\3\2\2\2\u044c\u0445\3\2\2\2\u044c\u0446\3\2"+
		"\2\2\u044c\u0449\3\2\2\2\u044d\u00a3\3\2\2\2\u044e\u044f\7\f\2\2\u044f"+
		"\u0453\7>\2\2\u0450\u0452\5\"\22\2\u0451\u0450\3\2\2\2\u0452\u0455\3\2"+
		"\2\2\u0453\u0451\3\2\2\2\u0453\u0454\3\2\2\2\u0454\u0456\3\2\2\2\u0455"+
		"\u0453\3\2\2\2\u0456\u0457\5\u00a6T\2\u0457\u0458\7i\2\2\u0458\u0459\7"+
		"?\2\2\u0459\u045a\5\u009aN\2\u045a\u00a5\3\2\2\2\u045b\u0460\5z>\2\u045c"+
		"\u045d\7[\2\2\u045d\u045f\5z>\2\u045e\u045c\3\2\2\2\u045f\u0462\3\2\2"+
		"\2\u0460\u045e\3\2\2\2\u0460\u0461\3\2\2\2\u0461\u00a7\3\2\2\2\u0462\u0460"+
		"\3\2\2\2\u0463\u0464\7\30\2\2\u0464\u0465\5\u009aN\2\u0465\u00a9\3\2\2"+
		"\2\u0466\u0467\7>\2\2\u0467\u0469\5\u00acW\2\u0468\u046a\7D\2\2\u0469"+
		"\u0468\3\2\2\2\u0469\u046a\3\2\2\2\u046a\u046b\3\2\2\2\u046b\u046c\7?"+
		"\2\2\u046c\u00ab\3\2\2\2\u046d\u0472\5\u00aeX\2\u046e\u046f\7D\2\2\u046f"+
		"\u0471\5\u00aeX\2\u0470\u046e\3\2\2\2\u0471\u0474\3\2\2\2\u0472\u0470"+
		"\3\2\2\2\u0472\u0473\3\2\2\2\u0473\u00ad\3\2\2\2\u0474\u0472\3\2\2\2\u0475"+
		"\u0477\5\"\22\2\u0476\u0475\3\2\2\2\u0477\u047a\3\2\2\2\u0478\u0476\3"+
		"\2\2\2\u0478\u0479\3\2\2\2\u0479\u047b\3\2\2\2\u047a\u0478\3\2\2\2\u047b"+
		"\u047c\5d\63\2\u047c\u047d\5Z.\2\u047d\u047e\7G\2\2\u047e\u047f\5\u00c4"+
		"c\2\u047f\u00af\3\2\2\2\u0480\u0482\5\u00b2Z\2\u0481\u0480\3\2\2\2\u0482"+
		"\u0483\3\2\2\2\u0483\u0481\3\2\2\2\u0483\u0484\3\2\2\2\u0484\u0486\3\2"+
		"\2\2\u0485\u0487\5\u009cO\2\u0486\u0485\3\2\2\2\u0487\u0488\3\2\2\2\u0488"+
		"\u0486\3\2\2\2\u0488\u0489\3\2\2\2\u0489\u00b1\3\2\2\2\u048a\u048b\7\13"+
		"\2\2\u048b\u048c\5\u00c2b\2\u048c\u048d\7M\2\2\u048d\u0495\3\2\2\2\u048e"+
		"\u048f\7\13\2\2\u048f\u0490\5`\61\2\u0490\u0491\7M\2\2\u0491\u0495\3\2"+
		"\2\2\u0492\u0493\7\21\2\2\u0493\u0495\7M\2\2\u0494\u048a\3\2\2\2\u0494"+
		"\u048e\3\2\2\2\u0494\u0492\3\2\2\2\u0495\u00b3\3\2\2\2\u0496\u04a3\5\u00b8"+
		"]\2\u0497\u0499\5\u00b6\\\2\u0498\u0497\3\2\2\2\u0498\u0499\3\2\2\2\u0499"+
		"\u049a\3\2\2\2\u049a\u049c\7D\2\2\u049b\u049d\5\u00c4c\2\u049c\u049b\3"+
		"\2\2\2\u049c\u049d\3\2\2\2\u049d\u049e\3\2\2\2\u049e\u04a0\7D\2\2\u049f"+
		"\u04a1\5\u00ba^\2\u04a0\u049f\3\2\2\2\u04a0\u04a1\3\2\2\2\u04a1\u04a3"+
		"\3\2\2\2\u04a2\u0496\3\2\2\2\u04a2\u0498\3\2\2\2\u04a3\u00b5\3\2\2\2\u04a4"+
		"\u04a7\5\u00a0Q\2\u04a5\u04a7\5\u00be`\2\u04a6\u04a4\3\2\2\2\u04a6\u04a5"+
		"\3\2\2\2\u04a7\u00b7\3\2\2\2\u04a8\u04aa\5\"\22\2\u04a9\u04a8\3\2\2\2"+
		"\u04aa\u04ad\3\2\2\2\u04ab\u04a9\3\2\2\2\u04ab\u04ac\3\2\2\2\u04ac\u04ae"+
		"\3\2\2\2\u04ad\u04ab\3\2\2\2\u04ae\u04af\5b\62\2\u04af\u04b0\5Z.\2\u04b0"+
		"\u04b1\7M\2\2\u04b1\u04b2\5\u00c4c\2\u04b2\u00b9\3\2\2\2\u04b3\u04b4\5"+
		"\u00be`\2\u04b4\u00bb\3\2\2\2\u04b5\u04b6\7>\2\2\u04b6\u04b7\5\u00c4c"+
		"\2\u04b7\u04b8\7?\2\2\u04b8\u00bd\3\2\2\2\u04b9\u04be\5\u00c4c\2\u04ba"+
		"\u04bb\7E\2\2\u04bb\u04bd\5\u00c4c\2\u04bc\u04ba\3\2\2\2\u04bd\u04c0\3"+
		"\2\2\2\u04be\u04bc\3\2\2\2\u04be\u04bf\3\2\2\2\u04bf\u00bf\3\2\2\2\u04c0"+
		"\u04be\3\2\2\2\u04c1\u04c2\5\u00c4c\2\u04c2\u00c1\3\2\2\2\u04c3\u04c4"+
		"\5\u00c4c\2\u04c4\u00c3\3\2\2\2\u04c5\u04c6\bc\1\2\u04c6\u04d3\5\u00c6"+
		"d\2\u04c7\u04c8\7$\2\2\u04c8\u04d3\5\u00c8e\2\u04c9\u04ca\7>\2\2\u04ca"+
		"\u04cb\5b\62\2\u04cb\u04cc\7?\2\2\u04cc\u04cd\5\u00c4c\23\u04cd\u04d3"+
		"\3\2\2\2\u04ce\u04cf\t\b\2\2\u04cf\u04d3\5\u00c4c\21\u04d0\u04d1\t\t\2"+
		"\2\u04d1\u04d3\5\u00c4c\20\u04d2\u04c5\3\2\2\2\u04d2\u04c7\3\2\2\2\u04d2"+
		"\u04c9\3\2\2\2\u04d2\u04ce\3\2\2\2\u04d2\u04d0\3\2\2\2\u04d3\u0529\3\2"+
		"\2\2\u04d4\u04d5\f\17\2\2\u04d5\u04d6\t\n\2\2\u04d6\u0528\5\u00c4c\20"+
		"\u04d7\u04d8\f\16\2\2\u04d8\u04d9\t\13\2\2\u04d9\u0528\5\u00c4c\17\u04da"+
		"\u04e2\f\r\2\2\u04db\u04dc\7I\2\2\u04dc\u04e3\7I\2\2\u04dd\u04de\7H\2"+
		"\2\u04de\u04df\7H\2\2\u04df\u04e3\7H\2\2\u04e0\u04e1\7H\2\2\u04e1\u04e3"+
		"\7H\2\2\u04e2\u04db\3\2\2\2\u04e2\u04dd\3\2\2\2\u04e2\u04e0\3\2\2\2\u04e3"+
		"\u04e4\3\2\2\2\u04e4\u0528\5\u00c4c\16\u04e5\u04e6\f\f\2\2\u04e6\u04e7"+
		"\t\f\2\2\u04e7\u0528\5\u00c4c\r\u04e8\u04e9\f\n\2\2\u04e9\u04ea\t\r\2"+
		"\2\u04ea\u0528\5\u00c4c\13\u04eb\u04ec\f\t\2\2\u04ec\u04ed\7Z\2\2\u04ed"+
		"\u0528\5\u00c4c\n\u04ee\u04ef\f\b\2\2\u04ef\u04f0\7\\\2\2\u04f0\u0528"+
		"\5\u00c4c\t\u04f1\u04f2\f\7\2\2\u04f2\u04f3\7[\2\2\u04f3\u0528\5\u00c4"+
		"c\b\u04f4\u04f5\f\6\2\2\u04f5\u04f6\7R\2\2\u04f6\u0528\5\u00c4c\7\u04f7"+
		"\u04f8\f\5\2\2\u04f8\u04f9\7S\2\2\u04f9\u0528\5\u00c4c\6\u04fa\u04fb\f"+
		"\4\2\2\u04fb\u04fc\7L\2\2\u04fc\u04fd\5\u00c4c\2\u04fd\u04fe\7M\2\2\u04fe"+
		"\u04ff\5\u00c4c\5\u04ff\u0528\3\2\2\2\u0500\u0501\f\3\2\2\u0501\u0502"+
		"\t\16\2\2\u0502\u0528\5\u00c4c\3\u0503\u0504\f\33\2\2\u0504\u0505\7F\2"+
		"\2\u0505\u0528\7i\2\2\u0506\u0507\f\32\2\2\u0507\u0508\7F\2\2\u0508\u0528"+
		"\7\60\2\2\u0509\u050a\f\31\2\2\u050a\u050b\7F\2\2\u050b\u050d\7$\2\2\u050c"+
		"\u050e\5\u00d4k\2\u050d\u050c\3\2\2\2\u050d\u050e\3\2\2\2\u050e\u050f"+
		"\3\2\2\2\u050f\u0528\5\u00ccg\2\u0510\u0511\f\30\2\2\u0511\u0512\7F\2"+
		"\2\u0512\u0513\7-\2\2\u0513\u0528\5\u00dan\2\u0514\u0515\f\27\2\2\u0515"+
		"\u0516\7F\2\2\u0516\u0528\5\u00d2j\2\u0517\u0518\f\26\2\2\u0518\u0519"+
		"\7B\2\2\u0519\u051a\5\u00c4c\2\u051a\u051b\7C\2\2\u051b\u0528\3\2\2\2"+
		"\u051c\u051d\f\25\2\2\u051d\u051f\7>\2\2\u051e\u0520\5\u00be`\2\u051f"+
		"\u051e\3\2\2\2\u051f\u0520\3\2\2\2\u0520\u0521\3\2\2\2\u0521\u0528\7?"+
		"\2\2\u0522\u0523\f\22\2\2\u0523\u0528\t\17\2\2\u0524\u0525\f\13\2\2\u0525"+
		"\u0526\7\37\2\2\u0526\u0528\5b\62\2\u0527\u04d4\3\2\2\2\u0527\u04d7\3"+
		"\2\2\2\u0527\u04da\3\2\2\2\u0527\u04e5\3\2\2\2\u0527\u04e8\3\2\2\2\u0527"+
		"\u04eb\3\2\2\2\u0527\u04ee\3\2\2\2\u0527\u04f1\3\2\2\2\u0527\u04f4\3\2"+
		"\2\2\u0527\u04f7\3\2\2\2\u0527\u04fa\3\2\2\2\u0527\u0500\3\2\2\2\u0527"+
		"\u0503\3\2\2\2\u0527\u0506\3\2\2\2\u0527\u0509\3\2\2\2\u0527\u0510\3\2"+
		"\2\2\u0527\u0514\3\2\2\2\u0527\u0517\3\2\2\2\u0527\u051c\3\2\2\2\u0527"+
		"\u0522\3\2\2\2\u0527\u0524\3\2\2\2\u0528\u052b\3\2\2\2\u0529\u0527\3\2"+
		"\2\2\u0529\u052a\3\2\2\2\u052a\u00c5\3\2\2\2\u052b\u0529\3\2\2\2\u052c"+
		"\u052d\7>\2\2\u052d\u052e\5\u00c4c\2\u052e\u052f\7?\2\2\u052f\u0542\3"+
		"\2\2\2\u0530\u0542\7\60\2\2\u0531\u0542\7-\2\2\u0532\u0542\5|?\2\u0533"+
		"\u0542\7i\2\2\u0534\u0535\5b\62\2\u0535\u0536\7F\2\2\u0536\u0537\7\16"+
		"\2\2\u0537\u0542\3\2\2\2\u0538\u0539\7\65\2\2\u0539\u053a\7F\2\2\u053a"+
		"\u0542\7\16\2\2\u053b\u053f\5\u00d4k\2\u053c\u0540\5\u00dco\2\u053d\u053e"+
		"\7\60\2\2\u053e\u0540\5\u00dep\2\u053f\u053c\3\2\2\2\u053f\u053d\3\2\2"+
		"\2\u0540\u0542\3\2\2\2\u0541\u052c\3\2\2\2\u0541\u0530\3\2\2\2\u0541\u0531"+
		"\3\2\2\2\u0541\u0532\3\2\2\2\u0541\u0533\3\2\2\2\u0541\u0534\3\2\2\2\u0541"+
		"\u0538\3\2\2\2\u0541\u053b\3\2\2\2\u0542\u00c7\3\2\2\2\u0543\u0544\5\u00d4"+
		"k\2\u0544\u0545\5\u00caf\2\u0545\u0546\5\u00d0i\2\u0546\u054d\3\2\2\2"+
		"\u0547\u054a\5\u00caf\2\u0548\u054b\5\u00ceh\2\u0549\u054b\5\u00d0i\2"+
		"\u054a\u0548\3\2\2\2\u054a\u0549\3\2\2\2\u054b\u054d\3\2\2\2\u054c\u0543"+
		"\3\2\2\2\u054c\u0547\3\2\2\2\u054d\u00c9\3\2\2\2\u054e\u0550\7i\2\2\u054f"+
		"\u0551\5\u00d6l\2\u0550\u054f\3\2\2\2\u0550\u0551\3\2\2\2\u0551\u0559"+
		"\3\2\2\2\u0552\u0553\7F\2\2\u0553\u0555\7i\2\2\u0554\u0556\5\u00d6l\2"+
		"\u0555\u0554\3\2\2\2\u0555\u0556\3\2\2\2\u0556\u0558\3\2\2\2\u0557\u0552"+
		"\3\2\2\2\u0558\u055b\3\2\2\2\u0559\u0557\3\2\2\2\u0559\u055a\3\2\2\2\u055a"+
		"\u055e\3\2\2\2\u055b\u0559\3\2\2\2\u055c\u055e\5f\64\2\u055d\u054e\3\2"+
		"\2\2\u055d\u055c\3\2\2\2\u055e\u00cb\3\2\2\2\u055f\u0561\7i\2\2\u0560"+
		"\u0562\5\u00d8m\2\u0561\u0560\3\2\2\2\u0561\u0562\3\2\2\2\u0562\u0563"+
		"\3\2\2\2\u0563\u0564\5\u00d0i\2\u0564\u00cd\3\2\2\2\u0565\u0581\7B\2\2"+
		"\u0566\u056b\7C\2\2\u0567\u0568\7B\2\2\u0568\u056a\7C\2\2\u0569\u0567"+
		"\3\2\2\2\u056a\u056d\3\2\2\2\u056b\u0569\3\2\2\2\u056b\u056c\3\2\2\2\u056c"+
		"\u056e\3\2\2\2\u056d\u056b\3\2\2\2\u056e\u0582\5^\60\2\u056f\u0570\5\u00c4"+
		"c\2\u0570\u0577\7C\2\2\u0571\u0572\7B\2\2\u0572\u0573\5\u00c4c\2\u0573"+
		"\u0574\7C\2\2\u0574\u0576\3\2\2\2\u0575\u0571\3\2\2\2\u0576\u0579\3\2"+
		"\2\2\u0577\u0575\3\2\2\2\u0577\u0578\3\2\2\2\u0578\u057e\3\2\2\2\u0579"+
		"\u0577\3\2\2\2\u057a\u057b\7B\2\2\u057b\u057d\7C\2\2\u057c\u057a\3\2\2"+
		"\2\u057d\u0580\3\2\2\2\u057e\u057c\3\2\2\2\u057e\u057f\3\2\2\2\u057f\u0582"+
		"\3\2\2\2\u0580\u057e\3\2\2\2\u0581\u0566\3\2\2\2\u0581\u056f\3\2\2\2\u0582"+
		"\u00cf\3\2\2\2\u0583\u0585\5\u00dep\2\u0584\u0586\58\35\2\u0585\u0584"+
		"\3\2\2\2\u0585\u0586\3\2\2\2\u0586\u00d1\3\2\2\2\u0587\u0588\5\u00d4k"+
		"\2\u0588\u0589\5\u00dco\2\u0589\u00d3\3\2\2\2\u058a\u058b\7I\2\2\u058b"+
		"\u058c\5\66\34\2\u058c\u058d\7H\2\2\u058d\u00d5\3\2\2\2\u058e\u058f\7"+
		"I\2\2\u058f\u0592\7H\2\2\u0590\u0592\5h\65\2\u0591\u058e\3\2\2\2\u0591"+
		"\u0590\3\2\2\2\u0592\u00d7\3\2\2\2\u0593\u0594\7I\2\2\u0594\u0597\7H\2"+
		"\2\u0595\u0597\5\u00d4k\2\u0596\u0593\3\2\2\2\u0596\u0595\3\2\2\2\u0597"+
		"\u00d9\3\2\2\2\u0598\u059f\5\u00dep\2\u0599\u059a\7F\2\2\u059a\u059c\7"+
		"i\2\2\u059b\u059d\5\u00dep\2\u059c\u059b\3\2\2\2\u059c\u059d\3\2\2\2\u059d"+
		"\u059f\3\2\2\2\u059e\u0598\3\2\2\2\u059e\u0599\3\2\2\2\u059f\u00db\3\2"+
		"\2\2\u05a0\u05a1\7-\2\2\u05a1\u05a5\5\u00dan\2\u05a2\u05a3\7i\2\2\u05a3"+
		"\u05a5\5\u00dep\2\u05a4\u05a0\3\2\2\2\u05a4\u05a2\3\2\2\2\u05a5\u00dd"+
		"\3\2\2\2\u05a6\u05a8\7>\2\2\u05a7\u05a9\5\u00be`\2\u05a8\u05a7\3\2\2\2"+
		"\u05a8\u05a9\3\2\2\2\u05a9\u05aa\3\2\2\2\u05aa\u05ab\7?\2\2\u05ab\u00df"+
		"\3\2\2\2\u00b0\u00e4\u00e9\u00f0\u00f9\u00ff\u0107\u010d\u0113\u011c\u0122"+
		"\u0128\u012f\u0135\u013d\u013f\u0147\u014f\u0151\u0157\u015d\u0165\u0169"+
		"\u0170\u0174\u0176\u0179\u017e\u0184\u018c\u0195\u019a\u01a1\u01a8\u01af"+
		"\u01b6\u01bb\u01bf\u01c3\u01c7\u01cc\u01d0\u01d4\u01de\u01e6\u01ed\u01f4"+
		"\u01f8\u01fb\u01fe\u0207\u020d\u0212\u0215\u021b\u0221\u0225\u022e\u0235"+
		"\u023e\u0245\u024b\u024f\u025a\u025e\u0266\u026b\u026f\u0278\u0286\u028b"+
		"\u0294\u029c\u02a6\u02ae\u02b6\u02bb\u02c7\u02cd\u02d4\u02d9\u02e1\u02e5"+
		"\u02e7\u02f2\u02fa\u02fd\u0301\u0306\u030a\u0315\u031e\u0320\u0327\u032c"+
		"\u0335\u033a\u033d\u0342\u034b\u035b\u0365\u0368\u0371\u037b\u0383\u0386"+
		"\u0389\u0396\u039e\u03a3\u03ab\u03af\u03b3\u03b7\u03b9\u03bd\u03c3\u03ce"+
		"\u03d6\u03de\u03e9\u03f2\u0409\u040c\u040f\u0417\u041b\u0423\u0429\u0434"+
		"\u043d\u0442\u044c\u0453\u0460\u0469\u0472\u0478\u0483\u0488\u0494\u0498"+
		"\u049c\u04a0\u04a2\u04a6\u04ab\u04be\u04d2\u04e2\u050d\u051f\u0527\u0529"+
		"\u053f\u0541\u054a\u054c\u0550\u0555\u0559\u055d\u0561\u056b\u0577\u057e"+
		"\u0581\u0585\u0591\u0596\u059c\u059e\u05a4\u05a8";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}