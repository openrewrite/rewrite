/*
 * Copyright 2025 the original author or authors.
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
// Generated from /Users/knut/git/openrewrite/rewrite/rewrite-xml/src/main/antlr/XPathParser.g4 by ANTLR 4.13.2
package org.openrewrite.xml.internal.grammar;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class XPathParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		WS=1, SLASH=2, DOUBLE_SLASH=3, AXIS_SEP=4, LBRACKET=5, RBRACKET=6, LPAREN=7, 
		RPAREN=8, AT=9, DOTDOT=10, DOT=11, COMMA=12, EQUALS=13, NOT_EQUALS=14, 
		LTE=15, GTE=16, LT=17, GT=18, WILDCARD=19, NUMBER=20, AND=21, OR=22, STRING_LITERAL=23, 
		QNAME=24, NCNAME=25;
	public static final int
		RULE_xpathExpression = 0, RULE_filterExpr = 1, RULE_booleanExpr = 2, RULE_comparisonOp = 3, 
		RULE_comparand = 4, RULE_absoluteLocationPath = 5, RULE_relativeLocationPath = 6, 
		RULE_pathSeparator = 7, RULE_step = 8, RULE_axisStep = 9, RULE_axisName = 10, 
		RULE_abbreviatedStep = 11, RULE_nodeTypeTest = 12, RULE_attributeStep = 13, 
		RULE_nodeTest = 14, RULE_predicate = 15, RULE_predicateExpr = 16, RULE_orExpr = 17, 
		RULE_andExpr = 18, RULE_primaryExpr = 19, RULE_predicateValue = 20, RULE_functionCall = 21, 
		RULE_functionArgs = 22, RULE_functionArg = 23, RULE_comparisonArg = 24, 
		RULE_childElementTest = 25, RULE_stringLiteral = 26;
	private static String[] makeRuleNames() {
		return new String[] {
			"xpathExpression", "filterExpr", "booleanExpr", "comparisonOp", "comparand", 
			"absoluteLocationPath", "relativeLocationPath", "pathSeparator", "step", 
			"axisStep", "axisName", "abbreviatedStep", "nodeTypeTest", "attributeStep", 
			"nodeTest", "predicate", "predicateExpr", "orExpr", "andExpr", "primaryExpr", 
			"predicateValue", "functionCall", "functionArgs", "functionArg", "comparisonArg", 
			"childElementTest", "stringLiteral"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, "'/'", "'//'", "'::'", "'['", "']'", "'('", "')'", "'@'", 
			"'..'", "'.'", "','", "'='", "'!='", "'<='", "'>='", "'<'", "'>'", "'*'", 
			null, "'and'", "'or'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "WS", "SLASH", "DOUBLE_SLASH", "AXIS_SEP", "LBRACKET", "RBRACKET", 
			"LPAREN", "RPAREN", "AT", "DOTDOT", "DOT", "COMMA", "EQUALS", "NOT_EQUALS", 
			"LTE", "GTE", "LT", "GT", "WILDCARD", "NUMBER", "AND", "OR", "STRING_LITERAL", 
			"QNAME", "NCNAME"
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
	public String getGrammarFileName() { return "XPathParser.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public XPathParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class XpathExpressionContext extends ParserRuleContext {
		public BooleanExprContext booleanExpr() {
			return getRuleContext(BooleanExprContext.class,0);
		}
		public FilterExprContext filterExpr() {
			return getRuleContext(FilterExprContext.class,0);
		}
		public AbsoluteLocationPathContext absoluteLocationPath() {
			return getRuleContext(AbsoluteLocationPathContext.class,0);
		}
		public RelativeLocationPathContext relativeLocationPath() {
			return getRuleContext(RelativeLocationPathContext.class,0);
		}
		public XpathExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_xpathExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterXpathExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitXpathExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitXpathExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final XpathExpressionContext xpathExpression() throws RecognitionException {
		XpathExpressionContext _localctx = new XpathExpressionContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_xpathExpression);
		try {
			setState(58);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(54);
				booleanExpr();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(55);
				filterExpr();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(56);
				absoluteLocationPath();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(57);
				relativeLocationPath();
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

	@SuppressWarnings("CheckReturnValue")
	public static class FilterExprContext extends ParserRuleContext {
		public TerminalNode LPAREN() { return getToken(XPathParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(XPathParser.RPAREN, 0); }
		public AbsoluteLocationPathContext absoluteLocationPath() {
			return getRuleContext(AbsoluteLocationPathContext.class,0);
		}
		public List<RelativeLocationPathContext> relativeLocationPath() {
			return getRuleContexts(RelativeLocationPathContext.class);
		}
		public RelativeLocationPathContext relativeLocationPath(int i) {
			return getRuleContext(RelativeLocationPathContext.class,i);
		}
		public List<PredicateContext> predicate() {
			return getRuleContexts(PredicateContext.class);
		}
		public PredicateContext predicate(int i) {
			return getRuleContext(PredicateContext.class,i);
		}
		public PathSeparatorContext pathSeparator() {
			return getRuleContext(PathSeparatorContext.class,0);
		}
		public FilterExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_filterExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterFilterExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitFilterExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitFilterExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FilterExprContext filterExpr() throws RecognitionException {
		FilterExprContext _localctx = new FilterExprContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_filterExpr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(60);
			match(LPAREN);
			setState(63);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SLASH:
			case DOUBLE_SLASH:
				{
				setState(61);
				absoluteLocationPath();
				}
				break;
			case AT:
			case DOTDOT:
			case DOT:
			case WILDCARD:
			case QNAME:
			case NCNAME:
				{
				setState(62);
				relativeLocationPath();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(65);
			match(RPAREN);
			setState(67); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(66);
				predicate();
				}
				}
				setState(69); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==LBRACKET );
			setState(74);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SLASH || _la==DOUBLE_SLASH) {
				{
				setState(71);
				pathSeparator();
				setState(72);
				relativeLocationPath();
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

	@SuppressWarnings("CheckReturnValue")
	public static class BooleanExprContext extends ParserRuleContext {
		public FunctionCallContext functionCall() {
			return getRuleContext(FunctionCallContext.class,0);
		}
		public ComparisonOpContext comparisonOp() {
			return getRuleContext(ComparisonOpContext.class,0);
		}
		public ComparandContext comparand() {
			return getRuleContext(ComparandContext.class,0);
		}
		public BooleanExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_booleanExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterBooleanExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitBooleanExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitBooleanExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BooleanExprContext booleanExpr() throws RecognitionException {
		BooleanExprContext _localctx = new BooleanExprContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_booleanExpr);
		try {
			setState(81);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,4,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(76);
				functionCall();
				setState(77);
				comparisonOp();
				setState(78);
				comparand();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(80);
				functionCall();
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

	@SuppressWarnings("CheckReturnValue")
	public static class ComparisonOpContext extends ParserRuleContext {
		public TerminalNode EQUALS() { return getToken(XPathParser.EQUALS, 0); }
		public TerminalNode NOT_EQUALS() { return getToken(XPathParser.NOT_EQUALS, 0); }
		public TerminalNode LT() { return getToken(XPathParser.LT, 0); }
		public TerminalNode GT() { return getToken(XPathParser.GT, 0); }
		public TerminalNode LTE() { return getToken(XPathParser.LTE, 0); }
		public TerminalNode GTE() { return getToken(XPathParser.GTE, 0); }
		public ComparisonOpContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_comparisonOp; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterComparisonOp(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitComparisonOp(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitComparisonOp(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ComparisonOpContext comparisonOp() throws RecognitionException {
		ComparisonOpContext _localctx = new ComparisonOpContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_comparisonOp);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(83);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 516096L) != 0)) ) {
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

	@SuppressWarnings("CheckReturnValue")
	public static class ComparandContext extends ParserRuleContext {
		public StringLiteralContext stringLiteral() {
			return getRuleContext(StringLiteralContext.class,0);
		}
		public TerminalNode NUMBER() { return getToken(XPathParser.NUMBER, 0); }
		public ComparandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_comparand; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterComparand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitComparand(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitComparand(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ComparandContext comparand() throws RecognitionException {
		ComparandContext _localctx = new ComparandContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_comparand);
		try {
			setState(87);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case STRING_LITERAL:
				enterOuterAlt(_localctx, 1);
				{
				setState(85);
				stringLiteral();
				}
				break;
			case NUMBER:
				enterOuterAlt(_localctx, 2);
				{
				setState(86);
				match(NUMBER);
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

	@SuppressWarnings("CheckReturnValue")
	public static class AbsoluteLocationPathContext extends ParserRuleContext {
		public TerminalNode SLASH() { return getToken(XPathParser.SLASH, 0); }
		public RelativeLocationPathContext relativeLocationPath() {
			return getRuleContext(RelativeLocationPathContext.class,0);
		}
		public TerminalNode DOUBLE_SLASH() { return getToken(XPathParser.DOUBLE_SLASH, 0); }
		public AbsoluteLocationPathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_absoluteLocationPath; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterAbsoluteLocationPath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitAbsoluteLocationPath(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitAbsoluteLocationPath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AbsoluteLocationPathContext absoluteLocationPath() throws RecognitionException {
		AbsoluteLocationPathContext _localctx = new AbsoluteLocationPathContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_absoluteLocationPath);
		int _la;
		try {
			setState(95);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SLASH:
				enterOuterAlt(_localctx, 1);
				{
				setState(89);
				match(SLASH);
				setState(91);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 50859520L) != 0)) {
					{
					setState(90);
					relativeLocationPath();
					}
				}

				}
				break;
			case DOUBLE_SLASH:
				enterOuterAlt(_localctx, 2);
				{
				setState(93);
				match(DOUBLE_SLASH);
				setState(94);
				relativeLocationPath();
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

	@SuppressWarnings("CheckReturnValue")
	public static class RelativeLocationPathContext extends ParserRuleContext {
		public List<StepContext> step() {
			return getRuleContexts(StepContext.class);
		}
		public StepContext step(int i) {
			return getRuleContext(StepContext.class,i);
		}
		public List<PathSeparatorContext> pathSeparator() {
			return getRuleContexts(PathSeparatorContext.class);
		}
		public PathSeparatorContext pathSeparator(int i) {
			return getRuleContext(PathSeparatorContext.class,i);
		}
		public RelativeLocationPathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_relativeLocationPath; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterRelativeLocationPath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitRelativeLocationPath(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitRelativeLocationPath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RelativeLocationPathContext relativeLocationPath() throws RecognitionException {
		RelativeLocationPathContext _localctx = new RelativeLocationPathContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_relativeLocationPath);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(97);
			step();
			setState(103);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==SLASH || _la==DOUBLE_SLASH) {
				{
				{
				setState(98);
				pathSeparator();
				setState(99);
				step();
				}
				}
				setState(105);
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

	@SuppressWarnings("CheckReturnValue")
	public static class PathSeparatorContext extends ParserRuleContext {
		public TerminalNode SLASH() { return getToken(XPathParser.SLASH, 0); }
		public TerminalNode DOUBLE_SLASH() { return getToken(XPathParser.DOUBLE_SLASH, 0); }
		public PathSeparatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pathSeparator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterPathSeparator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitPathSeparator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitPathSeparator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PathSeparatorContext pathSeparator() throws RecognitionException {
		PathSeparatorContext _localctx = new PathSeparatorContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_pathSeparator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(106);
			_la = _input.LA(1);
			if ( !(_la==SLASH || _la==DOUBLE_SLASH) ) {
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

	@SuppressWarnings("CheckReturnValue")
	public static class StepContext extends ParserRuleContext {
		public AxisStepContext axisStep() {
			return getRuleContext(AxisStepContext.class,0);
		}
		public List<PredicateContext> predicate() {
			return getRuleContexts(PredicateContext.class);
		}
		public PredicateContext predicate(int i) {
			return getRuleContext(PredicateContext.class,i);
		}
		public NodeTestContext nodeTest() {
			return getRuleContext(NodeTestContext.class,0);
		}
		public AttributeStepContext attributeStep() {
			return getRuleContext(AttributeStepContext.class,0);
		}
		public NodeTypeTestContext nodeTypeTest() {
			return getRuleContext(NodeTypeTestContext.class,0);
		}
		public AbbreviatedStepContext abbreviatedStep() {
			return getRuleContext(AbbreviatedStepContext.class,0);
		}
		public StepContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_step; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterStep(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitStep(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitStep(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StepContext step() throws RecognitionException {
		StepContext _localctx = new StepContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_step);
		int _la;
		try {
			setState(131);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,12,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(108);
				axisStep();
				setState(112);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==LBRACKET) {
					{
					{
					setState(109);
					predicate();
					}
					}
					setState(114);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(115);
				nodeTest();
				setState(119);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==LBRACKET) {
					{
					{
					setState(116);
					predicate();
					}
					}
					setState(121);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(122);
				attributeStep();
				setState(126);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==LBRACKET) {
					{
					{
					setState(123);
					predicate();
					}
					}
					setState(128);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(129);
				nodeTypeTest();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(130);
				abbreviatedStep();
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

	@SuppressWarnings("CheckReturnValue")
	public static class AxisStepContext extends ParserRuleContext {
		public AxisNameContext axisName() {
			return getRuleContext(AxisNameContext.class,0);
		}
		public TerminalNode AXIS_SEP() { return getToken(XPathParser.AXIS_SEP, 0); }
		public NodeTestContext nodeTest() {
			return getRuleContext(NodeTestContext.class,0);
		}
		public AxisStepContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_axisStep; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterAxisStep(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitAxisStep(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitAxisStep(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AxisStepContext axisStep() throws RecognitionException {
		AxisStepContext _localctx = new AxisStepContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_axisStep);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(133);
			axisName();
			setState(134);
			match(AXIS_SEP);
			setState(135);
			nodeTest();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AxisNameContext extends ParserRuleContext {
		public TerminalNode NCNAME() { return getToken(XPathParser.NCNAME, 0); }
		public AxisNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_axisName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterAxisName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitAxisName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitAxisName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AxisNameContext axisName() throws RecognitionException {
		AxisNameContext _localctx = new AxisNameContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_axisName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(137);
			match(NCNAME);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AbbreviatedStepContext extends ParserRuleContext {
		public TerminalNode DOTDOT() { return getToken(XPathParser.DOTDOT, 0); }
		public TerminalNode DOT() { return getToken(XPathParser.DOT, 0); }
		public AbbreviatedStepContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_abbreviatedStep; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterAbbreviatedStep(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitAbbreviatedStep(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitAbbreviatedStep(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AbbreviatedStepContext abbreviatedStep() throws RecognitionException {
		AbbreviatedStepContext _localctx = new AbbreviatedStepContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_abbreviatedStep);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(139);
			_la = _input.LA(1);
			if ( !(_la==DOTDOT || _la==DOT) ) {
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

	@SuppressWarnings("CheckReturnValue")
	public static class NodeTypeTestContext extends ParserRuleContext {
		public TerminalNode NCNAME() { return getToken(XPathParser.NCNAME, 0); }
		public TerminalNode LPAREN() { return getToken(XPathParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(XPathParser.RPAREN, 0); }
		public NodeTypeTestContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nodeTypeTest; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterNodeTypeTest(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitNodeTypeTest(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitNodeTypeTest(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NodeTypeTestContext nodeTypeTest() throws RecognitionException {
		NodeTypeTestContext _localctx = new NodeTypeTestContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_nodeTypeTest);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(141);
			match(NCNAME);
			setState(142);
			match(LPAREN);
			setState(143);
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

	@SuppressWarnings("CheckReturnValue")
	public static class AttributeStepContext extends ParserRuleContext {
		public TerminalNode AT() { return getToken(XPathParser.AT, 0); }
		public TerminalNode QNAME() { return getToken(XPathParser.QNAME, 0); }
		public TerminalNode NCNAME() { return getToken(XPathParser.NCNAME, 0); }
		public TerminalNode WILDCARD() { return getToken(XPathParser.WILDCARD, 0); }
		public AttributeStepContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_attributeStep; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterAttributeStep(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitAttributeStep(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitAttributeStep(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AttributeStepContext attributeStep() throws RecognitionException {
		AttributeStepContext _localctx = new AttributeStepContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_attributeStep);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(145);
			match(AT);
			setState(146);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 50855936L) != 0)) ) {
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

	@SuppressWarnings("CheckReturnValue")
	public static class NodeTestContext extends ParserRuleContext {
		public TerminalNode QNAME() { return getToken(XPathParser.QNAME, 0); }
		public TerminalNode NCNAME() { return getToken(XPathParser.NCNAME, 0); }
		public TerminalNode WILDCARD() { return getToken(XPathParser.WILDCARD, 0); }
		public NodeTestContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nodeTest; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterNodeTest(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitNodeTest(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitNodeTest(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NodeTestContext nodeTest() throws RecognitionException {
		NodeTestContext _localctx = new NodeTestContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_nodeTest);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(148);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 50855936L) != 0)) ) {
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

	@SuppressWarnings("CheckReturnValue")
	public static class PredicateContext extends ParserRuleContext {
		public TerminalNode LBRACKET() { return getToken(XPathParser.LBRACKET, 0); }
		public PredicateExprContext predicateExpr() {
			return getRuleContext(PredicateExprContext.class,0);
		}
		public TerminalNode RBRACKET() { return getToken(XPathParser.RBRACKET, 0); }
		public PredicateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_predicate; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterPredicate(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitPredicate(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitPredicate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PredicateContext predicate() throws RecognitionException {
		PredicateContext _localctx = new PredicateContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_predicate);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(150);
			match(LBRACKET);
			setState(151);
			predicateExpr();
			setState(152);
			match(RBRACKET);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class PredicateExprContext extends ParserRuleContext {
		public OrExprContext orExpr() {
			return getRuleContext(OrExprContext.class,0);
		}
		public PredicateExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_predicateExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterPredicateExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitPredicateExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitPredicateExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PredicateExprContext predicateExpr() throws RecognitionException {
		PredicateExprContext _localctx = new PredicateExprContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_predicateExpr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(154);
			orExpr();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class OrExprContext extends ParserRuleContext {
		public List<AndExprContext> andExpr() {
			return getRuleContexts(AndExprContext.class);
		}
		public AndExprContext andExpr(int i) {
			return getRuleContext(AndExprContext.class,i);
		}
		public List<TerminalNode> OR() { return getTokens(XPathParser.OR); }
		public TerminalNode OR(int i) {
			return getToken(XPathParser.OR, i);
		}
		public OrExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_orExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterOrExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitOrExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitOrExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OrExprContext orExpr() throws RecognitionException {
		OrExprContext _localctx = new OrExprContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_orExpr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(156);
			andExpr();
			setState(161);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==OR) {
				{
				{
				setState(157);
				match(OR);
				setState(158);
				andExpr();
				}
				}
				setState(163);
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

	@SuppressWarnings("CheckReturnValue")
	public static class AndExprContext extends ParserRuleContext {
		public List<PrimaryExprContext> primaryExpr() {
			return getRuleContexts(PrimaryExprContext.class);
		}
		public PrimaryExprContext primaryExpr(int i) {
			return getRuleContext(PrimaryExprContext.class,i);
		}
		public List<TerminalNode> AND() { return getTokens(XPathParser.AND); }
		public TerminalNode AND(int i) {
			return getToken(XPathParser.AND, i);
		}
		public AndExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_andExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterAndExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitAndExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitAndExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AndExprContext andExpr() throws RecognitionException {
		AndExprContext _localctx = new AndExprContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_andExpr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(164);
			primaryExpr();
			setState(169);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AND) {
				{
				{
				setState(165);
				match(AND);
				setState(166);
				primaryExpr();
				}
				}
				setState(171);
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

	@SuppressWarnings("CheckReturnValue")
	public static class PrimaryExprContext extends ParserRuleContext {
		public PredicateValueContext predicateValue() {
			return getRuleContext(PredicateValueContext.class,0);
		}
		public ComparisonOpContext comparisonOp() {
			return getRuleContext(ComparisonOpContext.class,0);
		}
		public ComparandContext comparand() {
			return getRuleContext(ComparandContext.class,0);
		}
		public PrimaryExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_primaryExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterPrimaryExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitPrimaryExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitPrimaryExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PrimaryExprContext primaryExpr() throws RecognitionException {
		PrimaryExprContext _localctx = new PrimaryExprContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_primaryExpr);
		try {
			setState(177);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,15,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(172);
				predicateValue();
				setState(173);
				comparisonOp();
				setState(174);
				comparand();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(176);
				predicateValue();
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

	@SuppressWarnings("CheckReturnValue")
	public static class PredicateValueContext extends ParserRuleContext {
		public FunctionCallContext functionCall() {
			return getRuleContext(FunctionCallContext.class,0);
		}
		public AttributeStepContext attributeStep() {
			return getRuleContext(AttributeStepContext.class,0);
		}
		public RelativeLocationPathContext relativeLocationPath() {
			return getRuleContext(RelativeLocationPathContext.class,0);
		}
		public ChildElementTestContext childElementTest() {
			return getRuleContext(ChildElementTestContext.class,0);
		}
		public TerminalNode NUMBER() { return getToken(XPathParser.NUMBER, 0); }
		public PredicateValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_predicateValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterPredicateValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitPredicateValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitPredicateValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PredicateValueContext predicateValue() throws RecognitionException {
		PredicateValueContext _localctx = new PredicateValueContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_predicateValue);
		try {
			setState(184);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,16,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(179);
				functionCall();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(180);
				attributeStep();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(181);
				relativeLocationPath();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(182);
				childElementTest();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(183);
				match(NUMBER);
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

	@SuppressWarnings("CheckReturnValue")
	public static class FunctionCallContext extends ParserRuleContext {
		public TerminalNode NCNAME() { return getToken(XPathParser.NCNAME, 0); }
		public TerminalNode LPAREN() { return getToken(XPathParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(XPathParser.RPAREN, 0); }
		public FunctionArgsContext functionArgs() {
			return getRuleContext(FunctionArgsContext.class,0);
		}
		public FunctionCallContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionCall; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterFunctionCall(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitFunctionCall(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitFunctionCall(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionCallContext functionCall() throws RecognitionException {
		FunctionCallContext _localctx = new FunctionCallContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_functionCall);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(186);
			match(NCNAME);
			setState(187);
			match(LPAREN);
			setState(189);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 60296716L) != 0)) {
				{
				setState(188);
				functionArgs();
				}
			}

			setState(191);
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

	@SuppressWarnings("CheckReturnValue")
	public static class FunctionArgsContext extends ParserRuleContext {
		public List<FunctionArgContext> functionArg() {
			return getRuleContexts(FunctionArgContext.class);
		}
		public FunctionArgContext functionArg(int i) {
			return getRuleContext(FunctionArgContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(XPathParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(XPathParser.COMMA, i);
		}
		public FunctionArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterFunctionArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitFunctionArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitFunctionArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionArgsContext functionArgs() throws RecognitionException {
		FunctionArgsContext _localctx = new FunctionArgsContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_functionArgs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(193);
			functionArg();
			setState(198);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(194);
				match(COMMA);
				setState(195);
				functionArg();
				}
				}
				setState(200);
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

	@SuppressWarnings("CheckReturnValue")
	public static class FunctionArgContext extends ParserRuleContext {
		public ComparisonArgContext comparisonArg() {
			return getRuleContext(ComparisonArgContext.class,0);
		}
		public AbsoluteLocationPathContext absoluteLocationPath() {
			return getRuleContext(AbsoluteLocationPathContext.class,0);
		}
		public FunctionCallContext functionCall() {
			return getRuleContext(FunctionCallContext.class,0);
		}
		public RelativeLocationPathContext relativeLocationPath() {
			return getRuleContext(RelativeLocationPathContext.class,0);
		}
		public StringLiteralContext stringLiteral() {
			return getRuleContext(StringLiteralContext.class,0);
		}
		public TerminalNode NUMBER() { return getToken(XPathParser.NUMBER, 0); }
		public FunctionArgContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionArg; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterFunctionArg(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitFunctionArg(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitFunctionArg(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionArgContext functionArg() throws RecognitionException {
		FunctionArgContext _localctx = new FunctionArgContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_functionArg);
		try {
			setState(207);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,19,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(201);
				comparisonArg();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(202);
				absoluteLocationPath();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(203);
				functionCall();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(204);
				relativeLocationPath();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(205);
				stringLiteral();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(206);
				match(NUMBER);
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

	@SuppressWarnings("CheckReturnValue")
	public static class ComparisonArgContext extends ParserRuleContext {
		public ComparisonOpContext comparisonOp() {
			return getRuleContext(ComparisonOpContext.class,0);
		}
		public ComparandContext comparand() {
			return getRuleContext(ComparandContext.class,0);
		}
		public FunctionCallContext functionCall() {
			return getRuleContext(FunctionCallContext.class,0);
		}
		public RelativeLocationPathContext relativeLocationPath() {
			return getRuleContext(RelativeLocationPathContext.class,0);
		}
		public AbsoluteLocationPathContext absoluteLocationPath() {
			return getRuleContext(AbsoluteLocationPathContext.class,0);
		}
		public ComparisonArgContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_comparisonArg; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterComparisonArg(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitComparisonArg(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitComparisonArg(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ComparisonArgContext comparisonArg() throws RecognitionException {
		ComparisonArgContext _localctx = new ComparisonArgContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_comparisonArg);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(212);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,20,_ctx) ) {
			case 1:
				{
				setState(209);
				functionCall();
				}
				break;
			case 2:
				{
				setState(210);
				relativeLocationPath();
				}
				break;
			case 3:
				{
				setState(211);
				absoluteLocationPath();
				}
				break;
			}
			setState(214);
			comparisonOp();
			setState(215);
			comparand();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ChildElementTestContext extends ParserRuleContext {
		public TerminalNode QNAME() { return getToken(XPathParser.QNAME, 0); }
		public TerminalNode NCNAME() { return getToken(XPathParser.NCNAME, 0); }
		public TerminalNode WILDCARD() { return getToken(XPathParser.WILDCARD, 0); }
		public ChildElementTestContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_childElementTest; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterChildElementTest(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitChildElementTest(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitChildElementTest(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ChildElementTestContext childElementTest() throws RecognitionException {
		ChildElementTestContext _localctx = new ChildElementTestContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_childElementTest);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(217);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 50855936L) != 0)) ) {
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

	@SuppressWarnings("CheckReturnValue")
	public static class StringLiteralContext extends ParserRuleContext {
		public TerminalNode STRING_LITERAL() { return getToken(XPathParser.STRING_LITERAL, 0); }
		public StringLiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stringLiteral; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterStringLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitStringLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitStringLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StringLiteralContext stringLiteral() throws RecognitionException {
		StringLiteralContext _localctx = new StringLiteralContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_stringLiteral);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(219);
			match(STRING_LITERAL);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\u0004\u0001\u0019\u00de\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001"+
		"\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004"+
		"\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007"+
		"\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b"+
		"\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007"+
		"\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007"+
		"\u0012\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002\u0015\u0007"+
		"\u0015\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017\u0002\u0018\u0007"+
		"\u0018\u0002\u0019\u0007\u0019\u0002\u001a\u0007\u001a\u0001\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0003\u0000;\b\u0000\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0003\u0001@\b\u0001\u0001\u0001\u0001\u0001\u0004"+
		"\u0001D\b\u0001\u000b\u0001\f\u0001E\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0003\u0001K\b\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0003\u0002R\b\u0002\u0001\u0003\u0001\u0003\u0001\u0004"+
		"\u0001\u0004\u0003\u0004X\b\u0004\u0001\u0005\u0001\u0005\u0003\u0005"+
		"\\\b\u0005\u0001\u0005\u0001\u0005\u0003\u0005`\b\u0005\u0001\u0006\u0001"+
		"\u0006\u0001\u0006\u0001\u0006\u0005\u0006f\b\u0006\n\u0006\f\u0006i\t"+
		"\u0006\u0001\u0007\u0001\u0007\u0001\b\u0001\b\u0005\bo\b\b\n\b\f\br\t"+
		"\b\u0001\b\u0001\b\u0005\bv\b\b\n\b\f\by\t\b\u0001\b\u0001\b\u0005\b}"+
		"\b\b\n\b\f\b\u0080\t\b\u0001\b\u0001\b\u0003\b\u0084\b\b\u0001\t\u0001"+
		"\t\u0001\t\u0001\t\u0001\n\u0001\n\u0001\u000b\u0001\u000b\u0001\f\u0001"+
		"\f\u0001\f\u0001\f\u0001\r\u0001\r\u0001\r\u0001\u000e\u0001\u000e\u0001"+
		"\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u0010\u0001\u0010\u0001"+
		"\u0011\u0001\u0011\u0001\u0011\u0005\u0011\u00a0\b\u0011\n\u0011\f\u0011"+
		"\u00a3\t\u0011\u0001\u0012\u0001\u0012\u0001\u0012\u0005\u0012\u00a8\b"+
		"\u0012\n\u0012\f\u0012\u00ab\t\u0012\u0001\u0013\u0001\u0013\u0001\u0013"+
		"\u0001\u0013\u0001\u0013\u0003\u0013\u00b2\b\u0013\u0001\u0014\u0001\u0014"+
		"\u0001\u0014\u0001\u0014\u0001\u0014\u0003\u0014\u00b9\b\u0014\u0001\u0015"+
		"\u0001\u0015\u0001\u0015\u0003\u0015\u00be\b\u0015\u0001\u0015\u0001\u0015"+
		"\u0001\u0016\u0001\u0016\u0001\u0016\u0005\u0016\u00c5\b\u0016\n\u0016"+
		"\f\u0016\u00c8\t\u0016\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017"+
		"\u0001\u0017\u0001\u0017\u0003\u0017\u00d0\b\u0017\u0001\u0018\u0001\u0018"+
		"\u0001\u0018\u0003\u0018\u00d5\b\u0018\u0001\u0018\u0001\u0018\u0001\u0018"+
		"\u0001\u0019\u0001\u0019\u0001\u001a\u0001\u001a\u0001\u001a\u0000\u0000"+
		"\u001b\u0000\u0002\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018"+
		"\u001a\u001c\u001e \"$&(*,.024\u0000\u0004\u0001\u0000\r\u0012\u0001\u0000"+
		"\u0002\u0003\u0001\u0000\n\u000b\u0002\u0000\u0013\u0013\u0018\u0019\u00e4"+
		"\u0000:\u0001\u0000\u0000\u0000\u0002<\u0001\u0000\u0000\u0000\u0004Q"+
		"\u0001\u0000\u0000\u0000\u0006S\u0001\u0000\u0000\u0000\bW\u0001\u0000"+
		"\u0000\u0000\n_\u0001\u0000\u0000\u0000\fa\u0001\u0000\u0000\u0000\u000e"+
		"j\u0001\u0000\u0000\u0000\u0010\u0083\u0001\u0000\u0000\u0000\u0012\u0085"+
		"\u0001\u0000\u0000\u0000\u0014\u0089\u0001\u0000\u0000\u0000\u0016\u008b"+
		"\u0001\u0000\u0000\u0000\u0018\u008d\u0001\u0000\u0000\u0000\u001a\u0091"+
		"\u0001\u0000\u0000\u0000\u001c\u0094\u0001\u0000\u0000\u0000\u001e\u0096"+
		"\u0001\u0000\u0000\u0000 \u009a\u0001\u0000\u0000\u0000\"\u009c\u0001"+
		"\u0000\u0000\u0000$\u00a4\u0001\u0000\u0000\u0000&\u00b1\u0001\u0000\u0000"+
		"\u0000(\u00b8\u0001\u0000\u0000\u0000*\u00ba\u0001\u0000\u0000\u0000,"+
		"\u00c1\u0001\u0000\u0000\u0000.\u00cf\u0001\u0000\u0000\u00000\u00d4\u0001"+
		"\u0000\u0000\u00002\u00d9\u0001\u0000\u0000\u00004\u00db\u0001\u0000\u0000"+
		"\u00006;\u0003\u0004\u0002\u00007;\u0003\u0002\u0001\u00008;\u0003\n\u0005"+
		"\u00009;\u0003\f\u0006\u0000:6\u0001\u0000\u0000\u0000:7\u0001\u0000\u0000"+
		"\u0000:8\u0001\u0000\u0000\u0000:9\u0001\u0000\u0000\u0000;\u0001\u0001"+
		"\u0000\u0000\u0000<?\u0005\u0007\u0000\u0000=@\u0003\n\u0005\u0000>@\u0003"+
		"\f\u0006\u0000?=\u0001\u0000\u0000\u0000?>\u0001\u0000\u0000\u0000@A\u0001"+
		"\u0000\u0000\u0000AC\u0005\b\u0000\u0000BD\u0003\u001e\u000f\u0000CB\u0001"+
		"\u0000\u0000\u0000DE\u0001\u0000\u0000\u0000EC\u0001\u0000\u0000\u0000"+
		"EF\u0001\u0000\u0000\u0000FJ\u0001\u0000\u0000\u0000GH\u0003\u000e\u0007"+
		"\u0000HI\u0003\f\u0006\u0000IK\u0001\u0000\u0000\u0000JG\u0001\u0000\u0000"+
		"\u0000JK\u0001\u0000\u0000\u0000K\u0003\u0001\u0000\u0000\u0000LM\u0003"+
		"*\u0015\u0000MN\u0003\u0006\u0003\u0000NO\u0003\b\u0004\u0000OR\u0001"+
		"\u0000\u0000\u0000PR\u0003*\u0015\u0000QL\u0001\u0000\u0000\u0000QP\u0001"+
		"\u0000\u0000\u0000R\u0005\u0001\u0000\u0000\u0000ST\u0007\u0000\u0000"+
		"\u0000T\u0007\u0001\u0000\u0000\u0000UX\u00034\u001a\u0000VX\u0005\u0014"+
		"\u0000\u0000WU\u0001\u0000\u0000\u0000WV\u0001\u0000\u0000\u0000X\t\u0001"+
		"\u0000\u0000\u0000Y[\u0005\u0002\u0000\u0000Z\\\u0003\f\u0006\u0000[Z"+
		"\u0001\u0000\u0000\u0000[\\\u0001\u0000\u0000\u0000\\`\u0001\u0000\u0000"+
		"\u0000]^\u0005\u0003\u0000\u0000^`\u0003\f\u0006\u0000_Y\u0001\u0000\u0000"+
		"\u0000_]\u0001\u0000\u0000\u0000`\u000b\u0001\u0000\u0000\u0000ag\u0003"+
		"\u0010\b\u0000bc\u0003\u000e\u0007\u0000cd\u0003\u0010\b\u0000df\u0001"+
		"\u0000\u0000\u0000eb\u0001\u0000\u0000\u0000fi\u0001\u0000\u0000\u0000"+
		"ge\u0001\u0000\u0000\u0000gh\u0001\u0000\u0000\u0000h\r\u0001\u0000\u0000"+
		"\u0000ig\u0001\u0000\u0000\u0000jk\u0007\u0001\u0000\u0000k\u000f\u0001"+
		"\u0000\u0000\u0000lp\u0003\u0012\t\u0000mo\u0003\u001e\u000f\u0000nm\u0001"+
		"\u0000\u0000\u0000or\u0001\u0000\u0000\u0000pn\u0001\u0000\u0000\u0000"+
		"pq\u0001\u0000\u0000\u0000q\u0084\u0001\u0000\u0000\u0000rp\u0001\u0000"+
		"\u0000\u0000sw\u0003\u001c\u000e\u0000tv\u0003\u001e\u000f\u0000ut\u0001"+
		"\u0000\u0000\u0000vy\u0001\u0000\u0000\u0000wu\u0001\u0000\u0000\u0000"+
		"wx\u0001\u0000\u0000\u0000x\u0084\u0001\u0000\u0000\u0000yw\u0001\u0000"+
		"\u0000\u0000z~\u0003\u001a\r\u0000{}\u0003\u001e\u000f\u0000|{\u0001\u0000"+
		"\u0000\u0000}\u0080\u0001\u0000\u0000\u0000~|\u0001\u0000\u0000\u0000"+
		"~\u007f\u0001\u0000\u0000\u0000\u007f\u0084\u0001\u0000\u0000\u0000\u0080"+
		"~\u0001\u0000\u0000\u0000\u0081\u0084\u0003\u0018\f\u0000\u0082\u0084"+
		"\u0003\u0016\u000b\u0000\u0083l\u0001\u0000\u0000\u0000\u0083s\u0001\u0000"+
		"\u0000\u0000\u0083z\u0001\u0000\u0000\u0000\u0083\u0081\u0001\u0000\u0000"+
		"\u0000\u0083\u0082\u0001\u0000\u0000\u0000\u0084\u0011\u0001\u0000\u0000"+
		"\u0000\u0085\u0086\u0003\u0014\n\u0000\u0086\u0087\u0005\u0004\u0000\u0000"+
		"\u0087\u0088\u0003\u001c\u000e\u0000\u0088\u0013\u0001\u0000\u0000\u0000"+
		"\u0089\u008a\u0005\u0019\u0000\u0000\u008a\u0015\u0001\u0000\u0000\u0000"+
		"\u008b\u008c\u0007\u0002\u0000\u0000\u008c\u0017\u0001\u0000\u0000\u0000"+
		"\u008d\u008e\u0005\u0019\u0000\u0000\u008e\u008f\u0005\u0007\u0000\u0000"+
		"\u008f\u0090\u0005\b\u0000\u0000\u0090\u0019\u0001\u0000\u0000\u0000\u0091"+
		"\u0092\u0005\t\u0000\u0000\u0092\u0093\u0007\u0003\u0000\u0000\u0093\u001b"+
		"\u0001\u0000\u0000\u0000\u0094\u0095\u0007\u0003\u0000\u0000\u0095\u001d"+
		"\u0001\u0000\u0000\u0000\u0096\u0097\u0005\u0005\u0000\u0000\u0097\u0098"+
		"\u0003 \u0010\u0000\u0098\u0099\u0005\u0006\u0000\u0000\u0099\u001f\u0001"+
		"\u0000\u0000\u0000\u009a\u009b\u0003\"\u0011\u0000\u009b!\u0001\u0000"+
		"\u0000\u0000\u009c\u00a1\u0003$\u0012\u0000\u009d\u009e\u0005\u0016\u0000"+
		"\u0000\u009e\u00a0\u0003$\u0012\u0000\u009f\u009d\u0001\u0000\u0000\u0000"+
		"\u00a0\u00a3\u0001\u0000\u0000\u0000\u00a1\u009f\u0001\u0000\u0000\u0000"+
		"\u00a1\u00a2\u0001\u0000\u0000\u0000\u00a2#\u0001\u0000\u0000\u0000\u00a3"+
		"\u00a1\u0001\u0000\u0000\u0000\u00a4\u00a9\u0003&\u0013\u0000\u00a5\u00a6"+
		"\u0005\u0015\u0000\u0000\u00a6\u00a8\u0003&\u0013\u0000\u00a7\u00a5\u0001"+
		"\u0000\u0000\u0000\u00a8\u00ab\u0001\u0000\u0000\u0000\u00a9\u00a7\u0001"+
		"\u0000\u0000\u0000\u00a9\u00aa\u0001\u0000\u0000\u0000\u00aa%\u0001\u0000"+
		"\u0000\u0000\u00ab\u00a9\u0001\u0000\u0000\u0000\u00ac\u00ad\u0003(\u0014"+
		"\u0000\u00ad\u00ae\u0003\u0006\u0003\u0000\u00ae\u00af\u0003\b\u0004\u0000"+
		"\u00af\u00b2\u0001\u0000\u0000\u0000\u00b0\u00b2\u0003(\u0014\u0000\u00b1"+
		"\u00ac\u0001\u0000\u0000\u0000\u00b1\u00b0\u0001\u0000\u0000\u0000\u00b2"+
		"\'\u0001\u0000\u0000\u0000\u00b3\u00b9\u0003*\u0015\u0000\u00b4\u00b9"+
		"\u0003\u001a\r\u0000\u00b5\u00b9\u0003\f\u0006\u0000\u00b6\u00b9\u0003"+
		"2\u0019\u0000\u00b7\u00b9\u0005\u0014\u0000\u0000\u00b8\u00b3\u0001\u0000"+
		"\u0000\u0000\u00b8\u00b4\u0001\u0000\u0000\u0000\u00b8\u00b5\u0001\u0000"+
		"\u0000\u0000\u00b8\u00b6\u0001\u0000\u0000\u0000\u00b8\u00b7\u0001\u0000"+
		"\u0000\u0000\u00b9)\u0001\u0000\u0000\u0000\u00ba\u00bb\u0005\u0019\u0000"+
		"\u0000\u00bb\u00bd\u0005\u0007\u0000\u0000\u00bc\u00be\u0003,\u0016\u0000"+
		"\u00bd\u00bc\u0001\u0000\u0000\u0000\u00bd\u00be\u0001\u0000\u0000\u0000"+
		"\u00be\u00bf\u0001\u0000\u0000\u0000\u00bf\u00c0\u0005\b\u0000\u0000\u00c0"+
		"+\u0001\u0000\u0000\u0000\u00c1\u00c6\u0003.\u0017\u0000\u00c2\u00c3\u0005"+
		"\f\u0000\u0000\u00c3\u00c5\u0003.\u0017\u0000\u00c4\u00c2\u0001\u0000"+
		"\u0000\u0000\u00c5\u00c8\u0001\u0000\u0000\u0000\u00c6\u00c4\u0001\u0000"+
		"\u0000\u0000\u00c6\u00c7\u0001\u0000\u0000\u0000\u00c7-\u0001\u0000\u0000"+
		"\u0000\u00c8\u00c6\u0001\u0000\u0000\u0000\u00c9\u00d0\u00030\u0018\u0000"+
		"\u00ca\u00d0\u0003\n\u0005\u0000\u00cb\u00d0\u0003*\u0015\u0000\u00cc"+
		"\u00d0\u0003\f\u0006\u0000\u00cd\u00d0\u00034\u001a\u0000\u00ce\u00d0"+
		"\u0005\u0014\u0000\u0000\u00cf\u00c9\u0001\u0000\u0000\u0000\u00cf\u00ca"+
		"\u0001\u0000\u0000\u0000\u00cf\u00cb\u0001\u0000\u0000\u0000\u00cf\u00cc"+
		"\u0001\u0000\u0000\u0000\u00cf\u00cd\u0001\u0000\u0000\u0000\u00cf\u00ce"+
		"\u0001\u0000\u0000\u0000\u00d0/\u0001\u0000\u0000\u0000\u00d1\u00d5\u0003"+
		"*\u0015\u0000\u00d2\u00d5\u0003\f\u0006\u0000\u00d3\u00d5\u0003\n\u0005"+
		"\u0000\u00d4\u00d1\u0001\u0000\u0000\u0000\u00d4\u00d2\u0001\u0000\u0000"+
		"\u0000\u00d4\u00d3\u0001\u0000\u0000\u0000\u00d5\u00d6\u0001\u0000\u0000"+
		"\u0000\u00d6\u00d7\u0003\u0006\u0003\u0000\u00d7\u00d8\u0003\b\u0004\u0000"+
		"\u00d81\u0001\u0000\u0000\u0000\u00d9\u00da\u0007\u0003\u0000\u0000\u00da"+
		"3\u0001\u0000\u0000\u0000\u00db\u00dc\u0005\u0017\u0000\u0000\u00dc5\u0001"+
		"\u0000\u0000\u0000\u0015:?EJQW[_gpw~\u0083\u00a1\u00a9\u00b1\u00b8\u00bd"+
		"\u00c6\u00cf\u00d4";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}