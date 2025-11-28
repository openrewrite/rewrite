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
		LTE=15, GTE=16, LT=17, GT=18, WILDCARD=19, NUMBER=20, AND=21, OR=22, LOCAL_NAME=23, 
		NAMESPACE_URI=24, STRING_LITERAL=25, QNAME=26, NCNAME=27;
	public static final int
		RULE_xpathExpression = 0, RULE_filterExpr = 1, RULE_booleanExpr = 2, RULE_comparisonOp = 3, 
		RULE_comparand = 4, RULE_absoluteLocationPath = 5, RULE_relativeLocationPath = 6, 
		RULE_pathSeparator = 7, RULE_step = 8, RULE_axisStep = 9, RULE_axisName = 10, 
		RULE_abbreviatedStep = 11, RULE_nodeTypeTest = 12, RULE_attributeStep = 13, 
		RULE_nodeTest = 14, RULE_predicate = 15, RULE_predicateExpr = 16, RULE_orExpr = 17, 
		RULE_andExpr = 18, RULE_primaryExpr = 19, RULE_predicateValue = 20, RULE_functionCall = 21, 
		RULE_functionArgs = 22, RULE_functionArg = 23, RULE_childElementTest = 24, 
		RULE_stringLiteral = 25;
	private static String[] makeRuleNames() {
		return new String[] {
			"xpathExpression", "filterExpr", "booleanExpr", "comparisonOp", "comparand", 
			"absoluteLocationPath", "relativeLocationPath", "pathSeparator", "step", 
			"axisStep", "axisName", "abbreviatedStep", "nodeTypeTest", "attributeStep", 
			"nodeTest", "predicate", "predicateExpr", "orExpr", "andExpr", "primaryExpr", 
			"predicateValue", "functionCall", "functionArgs", "functionArg", "childElementTest", 
			"stringLiteral"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, "'/'", "'//'", "'::'", "'['", "']'", "'('", "')'", "'@'", 
			"'..'", "'.'", "','", "'='", "'!='", "'<='", "'>='", "'<'", "'>'", "'*'", 
			null, "'and'", "'or'", "'local-name'", "'namespace-uri'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "WS", "SLASH", "DOUBLE_SLASH", "AXIS_SEP", "LBRACKET", "RBRACKET", 
			"LPAREN", "RPAREN", "AT", "DOTDOT", "DOT", "COMMA", "EQUALS", "NOT_EQUALS", 
			"LTE", "GTE", "LT", "GT", "WILDCARD", "NUMBER", "AND", "OR", "LOCAL_NAME", 
			"NAMESPACE_URI", "STRING_LITERAL", "QNAME", "NCNAME"
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
			setState(56);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(52);
				booleanExpr();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(53);
				filterExpr();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(54);
				absoluteLocationPath();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(55);
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
			setState(58);
			match(LPAREN);
			setState(61);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SLASH:
			case DOUBLE_SLASH:
				{
				setState(59);
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
				setState(60);
				relativeLocationPath();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(63);
			match(RPAREN);
			setState(65); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(64);
				predicate();
				}
				}
				setState(67); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==LBRACKET );
			setState(72);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SLASH || _la==DOUBLE_SLASH) {
				{
				setState(69);
				pathSeparator();
				setState(70);
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
			setState(79);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,4,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(74);
				functionCall();
				setState(75);
				comparisonOp();
				setState(76);
				comparand();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(78);
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
			setState(81);
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
			setState(85);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case STRING_LITERAL:
				enterOuterAlt(_localctx, 1);
				{
				setState(83);
				stringLiteral();
				}
				break;
			case NUMBER:
				enterOuterAlt(_localctx, 2);
				{
				setState(84);
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
			setState(93);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SLASH:
				enterOuterAlt(_localctx, 1);
				{
				setState(87);
				match(SLASH);
				setState(89);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 201854464L) != 0)) {
					{
					setState(88);
					relativeLocationPath();
					}
				}

				}
				break;
			case DOUBLE_SLASH:
				enterOuterAlt(_localctx, 2);
				{
				setState(91);
				match(DOUBLE_SLASH);
				setState(92);
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
			setState(95);
			step();
			setState(101);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==SLASH || _la==DOUBLE_SLASH) {
				{
				{
				setState(96);
				pathSeparator();
				setState(97);
				step();
				}
				}
				setState(103);
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
			setState(104);
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
			setState(129);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,12,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(106);
				axisStep();
				setState(110);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==LBRACKET) {
					{
					{
					setState(107);
					predicate();
					}
					}
					setState(112);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(113);
				nodeTest();
				setState(117);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==LBRACKET) {
					{
					{
					setState(114);
					predicate();
					}
					}
					setState(119);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(120);
				attributeStep();
				setState(124);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==LBRACKET) {
					{
					{
					setState(121);
					predicate();
					}
					}
					setState(126);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(127);
				nodeTypeTest();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(128);
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
			setState(131);
			axisName();
			setState(132);
			match(AXIS_SEP);
			setState(133);
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
			setState(135);
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
			setState(137);
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
			setState(139);
			match(NCNAME);
			setState(140);
			match(LPAREN);
			setState(141);
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
			setState(143);
			match(AT);
			setState(144);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 201850880L) != 0)) ) {
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
			setState(146);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 201850880L) != 0)) ) {
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
			setState(148);
			match(LBRACKET);
			setState(149);
			predicateExpr();
			setState(150);
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
			setState(152);
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
			setState(154);
			andExpr();
			setState(159);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==OR) {
				{
				{
				setState(155);
				match(OR);
				setState(156);
				andExpr();
				}
				}
				setState(161);
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
			setState(162);
			primaryExpr();
			setState(167);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AND) {
				{
				{
				setState(163);
				match(AND);
				setState(164);
				primaryExpr();
				}
				}
				setState(169);
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
			setState(175);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,15,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(170);
				predicateValue();
				setState(171);
				comparisonOp();
				setState(172);
				comparand();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(174);
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
			setState(182);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,16,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(177);
				functionCall();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(178);
				attributeStep();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(179);
				relativeLocationPath();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(180);
				childElementTest();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(181);
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
		public TerminalNode LOCAL_NAME() { return getToken(XPathParser.LOCAL_NAME, 0); }
		public TerminalNode LPAREN() { return getToken(XPathParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(XPathParser.RPAREN, 0); }
		public TerminalNode NAMESPACE_URI() { return getToken(XPathParser.NAMESPACE_URI, 0); }
		public TerminalNode NCNAME() { return getToken(XPathParser.NCNAME, 0); }
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
			setState(196);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LOCAL_NAME:
				enterOuterAlt(_localctx, 1);
				{
				setState(184);
				match(LOCAL_NAME);
				setState(185);
				match(LPAREN);
				setState(186);
				match(RPAREN);
				}
				break;
			case NAMESPACE_URI:
				enterOuterAlt(_localctx, 2);
				{
				setState(187);
				match(NAMESPACE_URI);
				setState(188);
				match(LPAREN);
				setState(189);
				match(RPAREN);
				}
				break;
			case NCNAME:
				enterOuterAlt(_localctx, 3);
				{
				setState(190);
				match(NCNAME);
				setState(191);
				match(LPAREN);
				setState(193);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 261623308L) != 0)) {
					{
					setState(192);
					functionArgs();
					}
				}

				setState(195);
				match(RPAREN);
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
			setState(198);
			functionArg();
			setState(203);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(199);
				match(COMMA);
				setState(200);
				functionArg();
				}
				}
				setState(205);
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
			setState(211);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,20,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(206);
				absoluteLocationPath();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(207);
				functionCall();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(208);
				relativeLocationPath();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(209);
				stringLiteral();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(210);
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
		enterRule(_localctx, 48, RULE_childElementTest);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(213);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 201850880L) != 0)) ) {
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
		enterRule(_localctx, 50, RULE_stringLiteral);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(215);
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
		"\u0004\u0001\u001b\u00da\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001"+
		"\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004"+
		"\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007"+
		"\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b"+
		"\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007"+
		"\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007"+
		"\u0012\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002\u0015\u0007"+
		"\u0015\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017\u0002\u0018\u0007"+
		"\u0018\u0002\u0019\u0007\u0019\u0001\u0000\u0001\u0000\u0001\u0000\u0001"+
		"\u0000\u0003\u00009\b\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0003"+
		"\u0001>\b\u0001\u0001\u0001\u0001\u0001\u0004\u0001B\b\u0001\u000b\u0001"+
		"\f\u0001C\u0001\u0001\u0001\u0001\u0001\u0001\u0003\u0001I\b\u0001\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0003\u0002P\b"+
		"\u0002\u0001\u0003\u0001\u0003\u0001\u0004\u0001\u0004\u0003\u0004V\b"+
		"\u0004\u0001\u0005\u0001\u0005\u0003\u0005Z\b\u0005\u0001\u0005\u0001"+
		"\u0005\u0003\u0005^\b\u0005\u0001\u0006\u0001\u0006\u0001\u0006\u0001"+
		"\u0006\u0005\u0006d\b\u0006\n\u0006\f\u0006g\t\u0006\u0001\u0007\u0001"+
		"\u0007\u0001\b\u0001\b\u0005\bm\b\b\n\b\f\bp\t\b\u0001\b\u0001\b\u0005"+
		"\bt\b\b\n\b\f\bw\t\b\u0001\b\u0001\b\u0005\b{\b\b\n\b\f\b~\t\b\u0001\b"+
		"\u0001\b\u0003\b\u0082\b\b\u0001\t\u0001\t\u0001\t\u0001\t\u0001\n\u0001"+
		"\n\u0001\u000b\u0001\u000b\u0001\f\u0001\f\u0001\f\u0001\f\u0001\r\u0001"+
		"\r\u0001\r\u0001\u000e\u0001\u000e\u0001\u000f\u0001\u000f\u0001\u000f"+
		"\u0001\u000f\u0001\u0010\u0001\u0010\u0001\u0011\u0001\u0011\u0001\u0011"+
		"\u0005\u0011\u009e\b\u0011\n\u0011\f\u0011\u00a1\t\u0011\u0001\u0012\u0001"+
		"\u0012\u0001\u0012\u0005\u0012\u00a6\b\u0012\n\u0012\f\u0012\u00a9\t\u0012"+
		"\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0003\u0013"+
		"\u00b0\b\u0013\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0014"+
		"\u0003\u0014\u00b7\b\u0014\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0015"+
		"\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0003\u0015"+
		"\u00c2\b\u0015\u0001\u0015\u0003\u0015\u00c5\b\u0015\u0001\u0016\u0001"+
		"\u0016\u0001\u0016\u0005\u0016\u00ca\b\u0016\n\u0016\f\u0016\u00cd\t\u0016"+
		"\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0003\u0017"+
		"\u00d4\b\u0017\u0001\u0018\u0001\u0018\u0001\u0019\u0001\u0019\u0001\u0019"+
		"\u0000\u0000\u001a\u0000\u0002\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014"+
		"\u0016\u0018\u001a\u001c\u001e \"$&(*,.02\u0000\u0004\u0001\u0000\r\u0012"+
		"\u0001\u0000\u0002\u0003\u0001\u0000\n\u000b\u0002\u0000\u0013\u0013\u001a"+
		"\u001b\u00e0\u00008\u0001\u0000\u0000\u0000\u0002:\u0001\u0000\u0000\u0000"+
		"\u0004O\u0001\u0000\u0000\u0000\u0006Q\u0001\u0000\u0000\u0000\bU\u0001"+
		"\u0000\u0000\u0000\n]\u0001\u0000\u0000\u0000\f_\u0001\u0000\u0000\u0000"+
		"\u000eh\u0001\u0000\u0000\u0000\u0010\u0081\u0001\u0000\u0000\u0000\u0012"+
		"\u0083\u0001\u0000\u0000\u0000\u0014\u0087\u0001\u0000\u0000\u0000\u0016"+
		"\u0089\u0001\u0000\u0000\u0000\u0018\u008b\u0001\u0000\u0000\u0000\u001a"+
		"\u008f\u0001\u0000\u0000\u0000\u001c\u0092\u0001\u0000\u0000\u0000\u001e"+
		"\u0094\u0001\u0000\u0000\u0000 \u0098\u0001\u0000\u0000\u0000\"\u009a"+
		"\u0001\u0000\u0000\u0000$\u00a2\u0001\u0000\u0000\u0000&\u00af\u0001\u0000"+
		"\u0000\u0000(\u00b6\u0001\u0000\u0000\u0000*\u00c4\u0001\u0000\u0000\u0000"+
		",\u00c6\u0001\u0000\u0000\u0000.\u00d3\u0001\u0000\u0000\u00000\u00d5"+
		"\u0001\u0000\u0000\u00002\u00d7\u0001\u0000\u0000\u000049\u0003\u0004"+
		"\u0002\u000059\u0003\u0002\u0001\u000069\u0003\n\u0005\u000079\u0003\f"+
		"\u0006\u000084\u0001\u0000\u0000\u000085\u0001\u0000\u0000\u000086\u0001"+
		"\u0000\u0000\u000087\u0001\u0000\u0000\u00009\u0001\u0001\u0000\u0000"+
		"\u0000:=\u0005\u0007\u0000\u0000;>\u0003\n\u0005\u0000<>\u0003\f\u0006"+
		"\u0000=;\u0001\u0000\u0000\u0000=<\u0001\u0000\u0000\u0000>?\u0001\u0000"+
		"\u0000\u0000?A\u0005\b\u0000\u0000@B\u0003\u001e\u000f\u0000A@\u0001\u0000"+
		"\u0000\u0000BC\u0001\u0000\u0000\u0000CA\u0001\u0000\u0000\u0000CD\u0001"+
		"\u0000\u0000\u0000DH\u0001\u0000\u0000\u0000EF\u0003\u000e\u0007\u0000"+
		"FG\u0003\f\u0006\u0000GI\u0001\u0000\u0000\u0000HE\u0001\u0000\u0000\u0000"+
		"HI\u0001\u0000\u0000\u0000I\u0003\u0001\u0000\u0000\u0000JK\u0003*\u0015"+
		"\u0000KL\u0003\u0006\u0003\u0000LM\u0003\b\u0004\u0000MP\u0001\u0000\u0000"+
		"\u0000NP\u0003*\u0015\u0000OJ\u0001\u0000\u0000\u0000ON\u0001\u0000\u0000"+
		"\u0000P\u0005\u0001\u0000\u0000\u0000QR\u0007\u0000\u0000\u0000R\u0007"+
		"\u0001\u0000\u0000\u0000SV\u00032\u0019\u0000TV\u0005\u0014\u0000\u0000"+
		"US\u0001\u0000\u0000\u0000UT\u0001\u0000\u0000\u0000V\t\u0001\u0000\u0000"+
		"\u0000WY\u0005\u0002\u0000\u0000XZ\u0003\f\u0006\u0000YX\u0001\u0000\u0000"+
		"\u0000YZ\u0001\u0000\u0000\u0000Z^\u0001\u0000\u0000\u0000[\\\u0005\u0003"+
		"\u0000\u0000\\^\u0003\f\u0006\u0000]W\u0001\u0000\u0000\u0000][\u0001"+
		"\u0000\u0000\u0000^\u000b\u0001\u0000\u0000\u0000_e\u0003\u0010\b\u0000"+
		"`a\u0003\u000e\u0007\u0000ab\u0003\u0010\b\u0000bd\u0001\u0000\u0000\u0000"+
		"c`\u0001\u0000\u0000\u0000dg\u0001\u0000\u0000\u0000ec\u0001\u0000\u0000"+
		"\u0000ef\u0001\u0000\u0000\u0000f\r\u0001\u0000\u0000\u0000ge\u0001\u0000"+
		"\u0000\u0000hi\u0007\u0001\u0000\u0000i\u000f\u0001\u0000\u0000\u0000"+
		"jn\u0003\u0012\t\u0000km\u0003\u001e\u000f\u0000lk\u0001\u0000\u0000\u0000"+
		"mp\u0001\u0000\u0000\u0000nl\u0001\u0000\u0000\u0000no\u0001\u0000\u0000"+
		"\u0000o\u0082\u0001\u0000\u0000\u0000pn\u0001\u0000\u0000\u0000qu\u0003"+
		"\u001c\u000e\u0000rt\u0003\u001e\u000f\u0000sr\u0001\u0000\u0000\u0000"+
		"tw\u0001\u0000\u0000\u0000us\u0001\u0000\u0000\u0000uv\u0001\u0000\u0000"+
		"\u0000v\u0082\u0001\u0000\u0000\u0000wu\u0001\u0000\u0000\u0000x|\u0003"+
		"\u001a\r\u0000y{\u0003\u001e\u000f\u0000zy\u0001\u0000\u0000\u0000{~\u0001"+
		"\u0000\u0000\u0000|z\u0001\u0000\u0000\u0000|}\u0001\u0000\u0000\u0000"+
		"}\u0082\u0001\u0000\u0000\u0000~|\u0001\u0000\u0000\u0000\u007f\u0082"+
		"\u0003\u0018\f\u0000\u0080\u0082\u0003\u0016\u000b\u0000\u0081j\u0001"+
		"\u0000\u0000\u0000\u0081q\u0001\u0000\u0000\u0000\u0081x\u0001\u0000\u0000"+
		"\u0000\u0081\u007f\u0001\u0000\u0000\u0000\u0081\u0080\u0001\u0000\u0000"+
		"\u0000\u0082\u0011\u0001\u0000\u0000\u0000\u0083\u0084\u0003\u0014\n\u0000"+
		"\u0084\u0085\u0005\u0004\u0000\u0000\u0085\u0086\u0003\u001c\u000e\u0000"+
		"\u0086\u0013\u0001\u0000\u0000\u0000\u0087\u0088\u0005\u001b\u0000\u0000"+
		"\u0088\u0015\u0001\u0000\u0000\u0000\u0089\u008a\u0007\u0002\u0000\u0000"+
		"\u008a\u0017\u0001\u0000\u0000\u0000\u008b\u008c\u0005\u001b\u0000\u0000"+
		"\u008c\u008d\u0005\u0007\u0000\u0000\u008d\u008e\u0005\b\u0000\u0000\u008e"+
		"\u0019\u0001\u0000\u0000\u0000\u008f\u0090\u0005\t\u0000\u0000\u0090\u0091"+
		"\u0007\u0003\u0000\u0000\u0091\u001b\u0001\u0000\u0000\u0000\u0092\u0093"+
		"\u0007\u0003\u0000\u0000\u0093\u001d\u0001\u0000\u0000\u0000\u0094\u0095"+
		"\u0005\u0005\u0000\u0000\u0095\u0096\u0003 \u0010\u0000\u0096\u0097\u0005"+
		"\u0006\u0000\u0000\u0097\u001f\u0001\u0000\u0000\u0000\u0098\u0099\u0003"+
		"\"\u0011\u0000\u0099!\u0001\u0000\u0000\u0000\u009a\u009f\u0003$\u0012"+
		"\u0000\u009b\u009c\u0005\u0016\u0000\u0000\u009c\u009e\u0003$\u0012\u0000"+
		"\u009d\u009b\u0001\u0000\u0000\u0000\u009e\u00a1\u0001\u0000\u0000\u0000"+
		"\u009f\u009d\u0001\u0000\u0000\u0000\u009f\u00a0\u0001\u0000\u0000\u0000"+
		"\u00a0#\u0001\u0000\u0000\u0000\u00a1\u009f\u0001\u0000\u0000\u0000\u00a2"+
		"\u00a7\u0003&\u0013\u0000\u00a3\u00a4\u0005\u0015\u0000\u0000\u00a4\u00a6"+
		"\u0003&\u0013\u0000\u00a5\u00a3\u0001\u0000\u0000\u0000\u00a6\u00a9\u0001"+
		"\u0000\u0000\u0000\u00a7\u00a5\u0001\u0000\u0000\u0000\u00a7\u00a8\u0001"+
		"\u0000\u0000\u0000\u00a8%\u0001\u0000\u0000\u0000\u00a9\u00a7\u0001\u0000"+
		"\u0000\u0000\u00aa\u00ab\u0003(\u0014\u0000\u00ab\u00ac\u0003\u0006\u0003"+
		"\u0000\u00ac\u00ad\u0003\b\u0004\u0000\u00ad\u00b0\u0001\u0000\u0000\u0000"+
		"\u00ae\u00b0\u0003(\u0014\u0000\u00af\u00aa\u0001\u0000\u0000\u0000\u00af"+
		"\u00ae\u0001\u0000\u0000\u0000\u00b0\'\u0001\u0000\u0000\u0000\u00b1\u00b7"+
		"\u0003*\u0015\u0000\u00b2\u00b7\u0003\u001a\r\u0000\u00b3\u00b7\u0003"+
		"\f\u0006\u0000\u00b4\u00b7\u00030\u0018\u0000\u00b5\u00b7\u0005\u0014"+
		"\u0000\u0000\u00b6\u00b1\u0001\u0000\u0000\u0000\u00b6\u00b2\u0001\u0000"+
		"\u0000\u0000\u00b6\u00b3\u0001\u0000\u0000\u0000\u00b6\u00b4\u0001\u0000"+
		"\u0000\u0000\u00b6\u00b5\u0001\u0000\u0000\u0000\u00b7)\u0001\u0000\u0000"+
		"\u0000\u00b8\u00b9\u0005\u0017\u0000\u0000\u00b9\u00ba\u0005\u0007\u0000"+
		"\u0000\u00ba\u00c5\u0005\b\u0000\u0000\u00bb\u00bc\u0005\u0018\u0000\u0000"+
		"\u00bc\u00bd\u0005\u0007\u0000\u0000\u00bd\u00c5\u0005\b\u0000\u0000\u00be"+
		"\u00bf\u0005\u001b\u0000\u0000\u00bf\u00c1\u0005\u0007\u0000\u0000\u00c0"+
		"\u00c2\u0003,\u0016\u0000\u00c1\u00c0\u0001\u0000\u0000\u0000\u00c1\u00c2"+
		"\u0001\u0000\u0000\u0000\u00c2\u00c3\u0001\u0000\u0000\u0000\u00c3\u00c5"+
		"\u0005\b\u0000\u0000\u00c4\u00b8\u0001\u0000\u0000\u0000\u00c4\u00bb\u0001"+
		"\u0000\u0000\u0000\u00c4\u00be\u0001\u0000\u0000\u0000\u00c5+\u0001\u0000"+
		"\u0000\u0000\u00c6\u00cb\u0003.\u0017\u0000\u00c7\u00c8\u0005\f\u0000"+
		"\u0000\u00c8\u00ca\u0003.\u0017\u0000\u00c9\u00c7\u0001\u0000\u0000\u0000"+
		"\u00ca\u00cd\u0001\u0000\u0000\u0000\u00cb\u00c9\u0001\u0000\u0000\u0000"+
		"\u00cb\u00cc\u0001\u0000\u0000\u0000\u00cc-\u0001\u0000\u0000\u0000\u00cd"+
		"\u00cb\u0001\u0000\u0000\u0000\u00ce\u00d4\u0003\n\u0005\u0000\u00cf\u00d4"+
		"\u0003*\u0015\u0000\u00d0\u00d4\u0003\f\u0006\u0000\u00d1\u00d4\u0003"+
		"2\u0019\u0000\u00d2\u00d4\u0005\u0014\u0000\u0000\u00d3\u00ce\u0001\u0000"+
		"\u0000\u0000\u00d3\u00cf\u0001\u0000\u0000\u0000\u00d3\u00d0\u0001\u0000"+
		"\u0000\u0000\u00d3\u00d1\u0001\u0000\u0000\u0000\u00d3\u00d2\u0001\u0000"+
		"\u0000\u0000\u00d4/\u0001\u0000\u0000\u0000\u00d5\u00d6\u0007\u0003\u0000"+
		"\u0000\u00d61\u0001\u0000\u0000\u0000\u00d7\u00d8\u0005\u0019\u0000\u0000"+
		"\u00d83\u0001\u0000\u0000\u0000\u00158=CHOUY]enu|\u0081\u009f\u00a7\u00af"+
		"\u00b6\u00c1\u00c4\u00cb\u00d3";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}