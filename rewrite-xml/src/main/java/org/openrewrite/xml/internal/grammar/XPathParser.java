/*
 * Copyright 2026 the original author or authors.
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
// Generated from rewrite-xml/src/main/antlr/XPathParser.g4 by ANTLR 4.13.2
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
		LTE=15, GTE=16, LT=17, GT=18, WILDCARD=19, NUMBER=20, AND=21, OR=22, TEXT=23,
		COMMENT=24, NODE=25, PROCESSING_INSTRUCTION=26, STRING_LITERAL=27, QNAME=28,
		NCNAME=29;
	public static final int
		RULE_xpathExpression = 0, RULE_expr = 1, RULE_orExpr = 2, RULE_andExpr = 3,
		RULE_equalityExpr = 4, RULE_relationalExpr = 5, RULE_unaryExpr = 6, RULE_unionExpr = 7,
		RULE_pathExpr = 8, RULE_functionCallExpr = 9, RULE_bracketedExpr = 10,
		RULE_literalOrNumber = 11, RULE_filterExpr = 12, RULE_primaryExpr = 13,
		RULE_locationPath = 14, RULE_absoluteLocationPath = 15, RULE_relativeLocationPath = 16,
		RULE_pathSeparator = 17, RULE_step = 18, RULE_axisSpecifier = 19, RULE_axisName = 20,
		RULE_abbreviatedStep = 21, RULE_attributeStep = 22, RULE_nodeTest = 23,
		RULE_nameTest = 24, RULE_nodeType = 25, RULE_predicate = 26, RULE_predicateExpr = 27,
		RULE_functionCall = 28, RULE_functionName = 29, RULE_argument = 30, RULE_literal = 31;
	private static String[] makeRuleNames() {
		return new String[] {
			"xpathExpression", "expr", "orExpr", "andExpr", "equalityExpr", "relationalExpr",
			"unaryExpr", "unionExpr", "pathExpr", "functionCallExpr", "bracketedExpr",
			"literalOrNumber", "filterExpr", "primaryExpr", "locationPath", "absoluteLocationPath",
			"relativeLocationPath", "pathSeparator", "step", "axisSpecifier", "axisName",
			"abbreviatedStep", "attributeStep", "nodeTest", "nameTest", "nodeType",
			"predicate", "predicateExpr", "functionCall", "functionName", "argument",
			"literal"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, "'/'", "'//'", "'::'", "'['", "']'", "'('", "')'", "'@'",
			"'..'", "'.'", "','", "'='", "'!='", "'<='", "'>='", "'<'", "'>'", "'*'",
			null, "'and'", "'or'", "'text'", "'comment'", "'node'", "'processing-instruction'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "WS", "SLASH", "DOUBLE_SLASH", "AXIS_SEP", "LBRACKET", "RBRACKET",
			"LPAREN", "RPAREN", "AT", "DOTDOT", "DOT", "COMMA", "EQUALS", "NOT_EQUALS",
			"LTE", "GTE", "LT", "GT", "WILDCARD", "NUMBER", "AND", "OR", "TEXT",
			"COMMENT", "NODE", "PROCESSING_INSTRUCTION", "STRING_LITERAL", "QNAME",
			"NCNAME"
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
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
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
			enterOuterAlt(_localctx, 1);
			{
			setState(64);
			expr();
			}
		}
		catch (RecognitionException re) {
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
	public static class ExprContext extends ParserRuleContext {
		public OrExprContext orExpr() {
			return getRuleContext(OrExprContext.class,0);
		}
		public ExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExprContext expr() throws RecognitionException {
		ExprContext _localctx = new ExprContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_expr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(66);
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
		enterRule(_localctx, 4, RULE_orExpr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(68);
			andExpr();
			setState(73);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==OR) {
				{
				{
				setState(69);
				match(OR);
				setState(70);
				andExpr();
				}
				}
				setState(75);
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
		public List<EqualityExprContext> equalityExpr() {
			return getRuleContexts(EqualityExprContext.class);
		}
		public EqualityExprContext equalityExpr(int i) {
			return getRuleContext(EqualityExprContext.class,i);
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
		enterRule(_localctx, 6, RULE_andExpr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(76);
			equalityExpr();
			setState(81);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AND) {
				{
				{
				setState(77);
				match(AND);
				setState(78);
				equalityExpr();
				}
				}
				setState(83);
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
	public static class EqualityExprContext extends ParserRuleContext {
		public List<RelationalExprContext> relationalExpr() {
			return getRuleContexts(RelationalExprContext.class);
		}
		public RelationalExprContext relationalExpr(int i) {
			return getRuleContext(RelationalExprContext.class,i);
		}
		public List<TerminalNode> EQUALS() { return getTokens(XPathParser.EQUALS); }
		public TerminalNode EQUALS(int i) {
			return getToken(XPathParser.EQUALS, i);
		}
		public List<TerminalNode> NOT_EQUALS() { return getTokens(XPathParser.NOT_EQUALS); }
		public TerminalNode NOT_EQUALS(int i) {
			return getToken(XPathParser.NOT_EQUALS, i);
		}
		public EqualityExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_equalityExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterEqualityExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitEqualityExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitEqualityExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EqualityExprContext equalityExpr() throws RecognitionException {
		EqualityExprContext _localctx = new EqualityExprContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_equalityExpr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(84);
			relationalExpr();
			setState(89);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==EQUALS || _la==NOT_EQUALS) {
				{
				{
				setState(85);
				_la = _input.LA(1);
				if ( !(_la==EQUALS || _la==NOT_EQUALS) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(86);
				relationalExpr();
				}
				}
				setState(91);
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
	public static class RelationalExprContext extends ParserRuleContext {
		public List<UnaryExprContext> unaryExpr() {
			return getRuleContexts(UnaryExprContext.class);
		}
		public UnaryExprContext unaryExpr(int i) {
			return getRuleContext(UnaryExprContext.class,i);
		}
		public List<TerminalNode> LT() { return getTokens(XPathParser.LT); }
		public TerminalNode LT(int i) {
			return getToken(XPathParser.LT, i);
		}
		public List<TerminalNode> GT() { return getTokens(XPathParser.GT); }
		public TerminalNode GT(int i) {
			return getToken(XPathParser.GT, i);
		}
		public List<TerminalNode> LTE() { return getTokens(XPathParser.LTE); }
		public TerminalNode LTE(int i) {
			return getToken(XPathParser.LTE, i);
		}
		public List<TerminalNode> GTE() { return getTokens(XPathParser.GTE); }
		public TerminalNode GTE(int i) {
			return getToken(XPathParser.GTE, i);
		}
		public RelationalExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_relationalExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterRelationalExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitRelationalExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitRelationalExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RelationalExprContext relationalExpr() throws RecognitionException {
		RelationalExprContext _localctx = new RelationalExprContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_relationalExpr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(92);
			unaryExpr();
			setState(97);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 491520L) != 0)) {
				{
				{
				setState(93);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 491520L) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(94);
				unaryExpr();
				}
				}
				setState(99);
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
	public static class UnaryExprContext extends ParserRuleContext {
		public UnionExprContext unionExpr() {
			return getRuleContext(UnionExprContext.class,0);
		}
		public UnaryExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unaryExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterUnaryExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitUnaryExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitUnaryExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UnaryExprContext unaryExpr() throws RecognitionException {
		UnaryExprContext _localctx = new UnaryExprContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_unaryExpr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(100);
			unionExpr();
			}
		}
		catch (RecognitionException re) {
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
	public static class UnionExprContext extends ParserRuleContext {
		public PathExprContext pathExpr() {
			return getRuleContext(PathExprContext.class,0);
		}
		public UnionExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unionExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterUnionExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitUnionExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitUnionExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UnionExprContext unionExpr() throws RecognitionException {
		UnionExprContext _localctx = new UnionExprContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_unionExpr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(102);
			pathExpr();
			}
		}
		catch (RecognitionException re) {
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
	public static class PathExprContext extends ParserRuleContext {
		public FunctionCallExprContext functionCallExpr() {
			return getRuleContext(FunctionCallExprContext.class,0);
		}
		public PathSeparatorContext pathSeparator() {
			return getRuleContext(PathSeparatorContext.class,0);
		}
		public RelativeLocationPathContext relativeLocationPath() {
			return getRuleContext(RelativeLocationPathContext.class,0);
		}
		public BracketedExprContext bracketedExpr() {
			return getRuleContext(BracketedExprContext.class,0);
		}
		public LiteralOrNumberContext literalOrNumber() {
			return getRuleContext(LiteralOrNumberContext.class,0);
		}
		public LocationPathContext locationPath() {
			return getRuleContext(LocationPathContext.class,0);
		}
		public PathExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pathExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterPathExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitPathExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitPathExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PathExprContext pathExpr() throws RecognitionException {
		PathExprContext _localctx = new PathExprContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_pathExpr);
		int _la;
		try {
			setState(123);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(104);
				functionCallExpr();
				setState(108);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==SLASH || _la==DOUBLE_SLASH) {
					{
					setState(105);
					pathSeparator();
					setState(106);
					relativeLocationPath();
					}
				}

				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(110);
				bracketedExpr();
				setState(114);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==SLASH || _la==DOUBLE_SLASH) {
					{
					setState(111);
					pathSeparator();
					setState(112);
					relativeLocationPath();
					}
				}

				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(116);
				literalOrNumber();
				setState(120);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==SLASH || _la==DOUBLE_SLASH) {
					{
					setState(117);
					pathSeparator();
					setState(118);
					relativeLocationPath();
					}
				}

				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(122);
				locationPath();
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
	public static class FunctionCallExprContext extends ParserRuleContext {
		public FunctionCallContext functionCall() {
			return getRuleContext(FunctionCallContext.class,0);
		}
		public List<PredicateContext> predicate() {
			return getRuleContexts(PredicateContext.class);
		}
		public PredicateContext predicate(int i) {
			return getRuleContext(PredicateContext.class,i);
		}
		public FunctionCallExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionCallExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterFunctionCallExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitFunctionCallExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitFunctionCallExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionCallExprContext functionCallExpr() throws RecognitionException {
		FunctionCallExprContext _localctx = new FunctionCallExprContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_functionCallExpr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(125);
			functionCall();
			setState(129);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==LBRACKET) {
				{
				{
				setState(126);
				predicate();
				}
				}
				setState(131);
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
	public static class BracketedExprContext extends ParserRuleContext {
		public TerminalNode LPAREN() { return getToken(XPathParser.LPAREN, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(XPathParser.RPAREN, 0); }
		public List<PredicateContext> predicate() {
			return getRuleContexts(PredicateContext.class);
		}
		public PredicateContext predicate(int i) {
			return getRuleContext(PredicateContext.class,i);
		}
		public BracketedExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_bracketedExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterBracketedExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitBracketedExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitBracketedExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BracketedExprContext bracketedExpr() throws RecognitionException {
		BracketedExprContext _localctx = new BracketedExprContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_bracketedExpr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(132);
			match(LPAREN);
			setState(133);
			expr();
			setState(134);
			match(RPAREN);
			setState(138);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==LBRACKET) {
				{
				{
				setState(135);
				predicate();
				}
				}
				setState(140);
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
	public static class LiteralOrNumberContext extends ParserRuleContext {
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public List<PredicateContext> predicate() {
			return getRuleContexts(PredicateContext.class);
		}
		public PredicateContext predicate(int i) {
			return getRuleContext(PredicateContext.class,i);
		}
		public TerminalNode NUMBER() { return getToken(XPathParser.NUMBER, 0); }
		public LiteralOrNumberContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_literalOrNumber; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterLiteralOrNumber(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitLiteralOrNumber(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitLiteralOrNumber(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LiteralOrNumberContext literalOrNumber() throws RecognitionException {
		LiteralOrNumberContext _localctx = new LiteralOrNumberContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_literalOrNumber);
		int _la;
		try {
			setState(155);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case STRING_LITERAL:
				enterOuterAlt(_localctx, 1);
				{
				setState(141);
				literal();
				setState(145);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==LBRACKET) {
					{
					{
					setState(142);
					predicate();
					}
					}
					setState(147);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case NUMBER:
				enterOuterAlt(_localctx, 2);
				{
				setState(148);
				match(NUMBER);
				setState(152);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==LBRACKET) {
					{
					{
					setState(149);
					predicate();
					}
					}
					setState(154);
					_errHandler.sync(this);
					_la = _input.LA(1);
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

	@SuppressWarnings("CheckReturnValue")
	public static class FilterExprContext extends ParserRuleContext {
		public FunctionCallExprContext functionCallExpr() {
			return getRuleContext(FunctionCallExprContext.class,0);
		}
		public BracketedExprContext bracketedExpr() {
			return getRuleContext(BracketedExprContext.class,0);
		}
		public LiteralOrNumberContext literalOrNumber() {
			return getRuleContext(LiteralOrNumberContext.class,0);
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
		enterRule(_localctx, 24, RULE_filterExpr);
		try {
			setState(160);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TEXT:
			case COMMENT:
			case NODE:
			case PROCESSING_INSTRUCTION:
			case NCNAME:
				enterOuterAlt(_localctx, 1);
				{
				setState(157);
				functionCallExpr();
				}
				break;
			case LPAREN:
				enterOuterAlt(_localctx, 2);
				{
				setState(158);
				bracketedExpr();
				}
				break;
			case NUMBER:
			case STRING_LITERAL:
				enterOuterAlt(_localctx, 3);
				{
				setState(159);
				literalOrNumber();
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
	public static class PrimaryExprContext extends ParserRuleContext {
		public TerminalNode LPAREN() { return getToken(XPathParser.LPAREN, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(XPathParser.RPAREN, 0); }
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public TerminalNode NUMBER() { return getToken(XPathParser.NUMBER, 0); }
		public FunctionCallContext functionCall() {
			return getRuleContext(FunctionCallContext.class,0);
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
		enterRule(_localctx, 26, RULE_primaryExpr);
		try {
			setState(169);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LPAREN:
				enterOuterAlt(_localctx, 1);
				{
				setState(162);
				match(LPAREN);
				setState(163);
				expr();
				setState(164);
				match(RPAREN);
				}
				break;
			case STRING_LITERAL:
				enterOuterAlt(_localctx, 2);
				{
				setState(166);
				literal();
				}
				break;
			case NUMBER:
				enterOuterAlt(_localctx, 3);
				{
				setState(167);
				match(NUMBER);
				}
				break;
			case TEXT:
			case COMMENT:
			case NODE:
			case PROCESSING_INSTRUCTION:
			case NCNAME:
				enterOuterAlt(_localctx, 4);
				{
				setState(168);
				functionCall();
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
	public static class LocationPathContext extends ParserRuleContext {
		public AbsoluteLocationPathContext absoluteLocationPath() {
			return getRuleContext(AbsoluteLocationPathContext.class,0);
		}
		public RelativeLocationPathContext relativeLocationPath() {
			return getRuleContext(RelativeLocationPathContext.class,0);
		}
		public LocationPathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_locationPath; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterLocationPath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitLocationPath(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitLocationPath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LocationPathContext locationPath() throws RecognitionException {
		LocationPathContext _localctx = new LocationPathContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_locationPath);
		try {
			setState(173);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SLASH:
			case DOUBLE_SLASH:
				enterOuterAlt(_localctx, 1);
				{
				setState(171);
				absoluteLocationPath();
				}
				break;
			case AT:
			case DOTDOT:
			case DOT:
			case WILDCARD:
			case TEXT:
			case COMMENT:
			case NODE:
			case PROCESSING_INSTRUCTION:
			case QNAME:
			case NCNAME:
				enterOuterAlt(_localctx, 2);
				{
				setState(172);
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
		enterRule(_localctx, 30, RULE_absoluteLocationPath);
		int _la;
		try {
			setState(181);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SLASH:
				enterOuterAlt(_localctx, 1);
				{
				setState(175);
				match(SLASH);
				setState(177);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 931663360L) != 0)) {
					{
					setState(176);
					relativeLocationPath();
					}
				}

				}
				break;
			case DOUBLE_SLASH:
				enterOuterAlt(_localctx, 2);
				{
				setState(179);
				match(DOUBLE_SLASH);
				setState(180);
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
		enterRule(_localctx, 32, RULE_relativeLocationPath);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(183);
			step();
			setState(189);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==SLASH || _la==DOUBLE_SLASH) {
				{
				{
				setState(184);
				pathSeparator();
				setState(185);
				step();
				}
				}
				setState(191);
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
		enterRule(_localctx, 34, RULE_pathSeparator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(192);
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
		public NodeTestContext nodeTest() {
			return getRuleContext(NodeTestContext.class,0);
		}
		public AxisSpecifierContext axisSpecifier() {
			return getRuleContext(AxisSpecifierContext.class,0);
		}
		public List<PredicateContext> predicate() {
			return getRuleContexts(PredicateContext.class);
		}
		public PredicateContext predicate(int i) {
			return getRuleContext(PredicateContext.class,i);
		}
		public AttributeStepContext attributeStep() {
			return getRuleContext(AttributeStepContext.class,0);
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
		enterRule(_localctx, 36, RULE_step);
		int _la;
		try {
			setState(212);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case WILDCARD:
			case TEXT:
			case COMMENT:
			case NODE:
			case PROCESSING_INSTRUCTION:
			case QNAME:
			case NCNAME:
				enterOuterAlt(_localctx, 1);
				{
				setState(195);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,19,_ctx) ) {
				case 1:
					{
					setState(194);
					axisSpecifier();
					}
					break;
				}
				setState(197);
				nodeTest();
				setState(201);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==LBRACKET) {
					{
					{
					setState(198);
					predicate();
					}
					}
					setState(203);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case AT:
				enterOuterAlt(_localctx, 2);
				{
				setState(204);
				attributeStep();
				setState(208);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==LBRACKET) {
					{
					{
					setState(205);
					predicate();
					}
					}
					setState(210);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case DOTDOT:
			case DOT:
				enterOuterAlt(_localctx, 3);
				{
				setState(211);
				abbreviatedStep();
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
	public static class AxisSpecifierContext extends ParserRuleContext {
		public AxisNameContext axisName() {
			return getRuleContext(AxisNameContext.class,0);
		}
		public TerminalNode AXIS_SEP() { return getToken(XPathParser.AXIS_SEP, 0); }
		public AxisSpecifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_axisSpecifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterAxisSpecifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitAxisSpecifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitAxisSpecifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AxisSpecifierContext axisSpecifier() throws RecognitionException {
		AxisSpecifierContext _localctx = new AxisSpecifierContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_axisSpecifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(214);
			axisName();
			setState(215);
			match(AXIS_SEP);
			}
		}
		catch (RecognitionException re) {
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
		enterRule(_localctx, 40, RULE_axisName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(217);
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
		enterRule(_localctx, 42, RULE_abbreviatedStep);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(219);
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
		enterRule(_localctx, 44, RULE_attributeStep);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(221);
			match(AT);
			setState(222);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 805830656L) != 0)) ) {
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
		public NameTestContext nameTest() {
			return getRuleContext(NameTestContext.class,0);
		}
		public NodeTypeContext nodeType() {
			return getRuleContext(NodeTypeContext.class,0);
		}
		public TerminalNode LPAREN() { return getToken(XPathParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(XPathParser.RPAREN, 0); }
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
		enterRule(_localctx, 46, RULE_nodeTest);
		try {
			setState(229);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case WILDCARD:
			case QNAME:
			case NCNAME:
				enterOuterAlt(_localctx, 1);
				{
				setState(224);
				nameTest();
				}
				break;
			case TEXT:
			case COMMENT:
			case NODE:
			case PROCESSING_INSTRUCTION:
				enterOuterAlt(_localctx, 2);
				{
				setState(225);
				nodeType();
				setState(226);
				match(LPAREN);
				setState(227);
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
	public static class NameTestContext extends ParserRuleContext {
		public TerminalNode WILDCARD() { return getToken(XPathParser.WILDCARD, 0); }
		public TerminalNode QNAME() { return getToken(XPathParser.QNAME, 0); }
		public TerminalNode NCNAME() { return getToken(XPathParser.NCNAME, 0); }
		public NameTestContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nameTest; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterNameTest(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitNameTest(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitNameTest(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NameTestContext nameTest() throws RecognitionException {
		NameTestContext _localctx = new NameTestContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_nameTest);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(231);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 805830656L) != 0)) ) {
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
	public static class NodeTypeContext extends ParserRuleContext {
		public TerminalNode TEXT() { return getToken(XPathParser.TEXT, 0); }
		public TerminalNode COMMENT() { return getToken(XPathParser.COMMENT, 0); }
		public TerminalNode NODE() { return getToken(XPathParser.NODE, 0); }
		public TerminalNode PROCESSING_INSTRUCTION() { return getToken(XPathParser.PROCESSING_INSTRUCTION, 0); }
		public NodeTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nodeType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterNodeType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitNodeType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitNodeType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NodeTypeContext nodeType() throws RecognitionException {
		NodeTypeContext _localctx = new NodeTypeContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_nodeType);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(233);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 125829120L) != 0)) ) {
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
		enterRule(_localctx, 52, RULE_predicate);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(235);
			match(LBRACKET);
			setState(236);
			predicateExpr();
			setState(237);
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
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
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
		enterRule(_localctx, 54, RULE_predicateExpr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(239);
			expr();
			}
		}
		catch (RecognitionException re) {
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
		public FunctionNameContext functionName() {
			return getRuleContext(FunctionNameContext.class,0);
		}
		public TerminalNode LPAREN() { return getToken(XPathParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(XPathParser.RPAREN, 0); }
		public List<ArgumentContext> argument() {
			return getRuleContexts(ArgumentContext.class);
		}
		public ArgumentContext argument(int i) {
			return getRuleContext(ArgumentContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(XPathParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(XPathParser.COMMA, i);
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
		enterRule(_localctx, 56, RULE_functionCall);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(241);
			functionName();
			setState(242);
			match(LPAREN);
			setState(251);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 1066929804L) != 0)) {
				{
				setState(243);
				argument();
				setState(248);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(244);
					match(COMMA);
					setState(245);
					argument();
					}
					}
					setState(250);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(253);
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
	public static class FunctionNameContext extends ParserRuleContext {
		public TerminalNode NCNAME() { return getToken(XPathParser.NCNAME, 0); }
		public TerminalNode TEXT() { return getToken(XPathParser.TEXT, 0); }
		public TerminalNode COMMENT() { return getToken(XPathParser.COMMENT, 0); }
		public TerminalNode NODE() { return getToken(XPathParser.NODE, 0); }
		public TerminalNode PROCESSING_INSTRUCTION() { return getToken(XPathParser.PROCESSING_INSTRUCTION, 0); }
		public FunctionNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterFunctionName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitFunctionName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitFunctionName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionNameContext functionName() throws RecognitionException {
		FunctionNameContext _localctx = new FunctionNameContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_functionName);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(255);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 662700032L) != 0)) ) {
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
	public static class ArgumentContext extends ParserRuleContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public ArgumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_argument; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterArgument(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitArgument(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitArgument(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArgumentContext argument() throws RecognitionException {
		ArgumentContext _localctx = new ArgumentContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_argument);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(257);
			expr();
			}
		}
		catch (RecognitionException re) {
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
	public static class LiteralContext extends ParserRuleContext {
		public TerminalNode STRING_LITERAL() { return getToken(XPathParser.STRING_LITERAL, 0); }
		public LiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_literal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LiteralContext literal() throws RecognitionException {
		LiteralContext _localctx = new LiteralContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_literal);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(259);
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
		"\u0004\u0001\u001d\u0106\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001"+
		"\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004"+
		"\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007"+
		"\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b"+
		"\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007"+
		"\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007"+
		"\u0012\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002\u0015\u0007"+
		"\u0015\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017\u0002\u0018\u0007"+
		"\u0018\u0002\u0019\u0007\u0019\u0002\u001a\u0007\u001a\u0002\u001b\u0007"+
		"\u001b\u0002\u001c\u0007\u001c\u0002\u001d\u0007\u001d\u0002\u001e\u0007"+
		"\u001e\u0002\u001f\u0007\u001f\u0001\u0000\u0001\u0000\u0001\u0001\u0001"+
		"\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0005\u0002H\b\u0002\n\u0002"+
		"\f\u0002K\t\u0002\u0001\u0003\u0001\u0003\u0001\u0003\u0005\u0003P\b\u0003"+
		"\n\u0003\f\u0003S\t\u0003\u0001\u0004\u0001\u0004\u0001\u0004\u0005\u0004"+
		"X\b\u0004\n\u0004\f\u0004[\t\u0004\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0005\u0005`\b\u0005\n\u0005\f\u0005c\t\u0005\u0001\u0006\u0001\u0006"+
		"\u0001\u0007\u0001\u0007\u0001\b\u0001\b\u0001\b\u0001\b\u0003\bm\b\b"+
		"\u0001\b\u0001\b\u0001\b\u0001\b\u0003\bs\b\b\u0001\b\u0001\b\u0001\b"+
		"\u0001\b\u0003\by\b\b\u0001\b\u0003\b|\b\b\u0001\t\u0001\t\u0005\t\u0080"+
		"\b\t\n\t\f\t\u0083\t\t\u0001\n\u0001\n\u0001\n\u0001\n\u0005\n\u0089\b"+
		"\n\n\n\f\n\u008c\t\n\u0001\u000b\u0001\u000b\u0005\u000b\u0090\b\u000b"+
		"\n\u000b\f\u000b\u0093\t\u000b\u0001\u000b\u0001\u000b\u0005\u000b\u0097"+
		"\b\u000b\n\u000b\f\u000b\u009a\t\u000b\u0003\u000b\u009c\b\u000b\u0001"+
		"\f\u0001\f\u0001\f\u0003\f\u00a1\b\f\u0001\r\u0001\r\u0001\r\u0001\r\u0001"+
		"\r\u0001\r\u0001\r\u0003\r\u00aa\b\r\u0001\u000e\u0001\u000e\u0003\u000e"+
		"\u00ae\b\u000e\u0001\u000f\u0001\u000f\u0003\u000f\u00b2\b\u000f\u0001"+
		"\u000f\u0001\u000f\u0003\u000f\u00b6\b\u000f\u0001\u0010\u0001\u0010\u0001"+
		"\u0010\u0001\u0010\u0005\u0010\u00bc\b\u0010\n\u0010\f\u0010\u00bf\t\u0010"+
		"\u0001\u0011\u0001\u0011\u0001\u0012\u0003\u0012\u00c4\b\u0012\u0001\u0012"+
		"\u0001\u0012\u0005\u0012\u00c8\b\u0012\n\u0012\f\u0012\u00cb\t\u0012\u0001"+
		"\u0012\u0001\u0012\u0005\u0012\u00cf\b\u0012\n\u0012\f\u0012\u00d2\t\u0012"+
		"\u0001\u0012\u0003\u0012\u00d5\b\u0012\u0001\u0013\u0001\u0013\u0001\u0013"+
		"\u0001\u0014\u0001\u0014\u0001\u0015\u0001\u0015\u0001\u0016\u0001\u0016"+
		"\u0001\u0016\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017"+
		"\u0003\u0017\u00e6\b\u0017\u0001\u0018\u0001\u0018\u0001\u0019\u0001\u0019"+
		"\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001a\u0001\u001b\u0001\u001b"+
		"\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0005\u001c"+
		"\u00f7\b\u001c\n\u001c\f\u001c\u00fa\t\u001c\u0003\u001c\u00fc\b\u001c"+
		"\u0001\u001c\u0001\u001c\u0001\u001d\u0001\u001d\u0001\u001e\u0001\u001e"+
		"\u0001\u001f\u0001\u001f\u0001\u001f\u0000\u0000 \u0000\u0002\u0004\u0006"+
		"\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u001c\u001e \"$&(*,."+
		"02468:<>\u0000\u0007\u0001\u0000\r\u000e\u0001\u0000\u000f\u0012\u0001"+
		"\u0000\u0002\u0003\u0001\u0000\n\u000b\u0002\u0000\u0013\u0013\u001c\u001d"+
		"\u0001\u0000\u0017\u001a\u0002\u0000\u0017\u001a\u001d\u001d\u0105\u0000"+
		"@\u0001\u0000\u0000\u0000\u0002B\u0001\u0000\u0000\u0000\u0004D\u0001"+
		"\u0000\u0000\u0000\u0006L\u0001\u0000\u0000\u0000\bT\u0001\u0000\u0000"+
		"\u0000\n\\\u0001\u0000\u0000\u0000\fd\u0001\u0000\u0000\u0000\u000ef\u0001"+
		"\u0000\u0000\u0000\u0010{\u0001\u0000\u0000\u0000\u0012}\u0001\u0000\u0000"+
		"\u0000\u0014\u0084\u0001\u0000\u0000\u0000\u0016\u009b\u0001\u0000\u0000"+
		"\u0000\u0018\u00a0\u0001\u0000\u0000\u0000\u001a\u00a9\u0001\u0000\u0000"+
		"\u0000\u001c\u00ad\u0001\u0000\u0000\u0000\u001e\u00b5\u0001\u0000\u0000"+
		"\u0000 \u00b7\u0001\u0000\u0000\u0000\"\u00c0\u0001\u0000\u0000\u0000"+
		"$\u00d4\u0001\u0000\u0000\u0000&\u00d6\u0001\u0000\u0000\u0000(\u00d9"+
		"\u0001\u0000\u0000\u0000*\u00db\u0001\u0000\u0000\u0000,\u00dd\u0001\u0000"+
		"\u0000\u0000.\u00e5\u0001\u0000\u0000\u00000\u00e7\u0001\u0000\u0000\u0000"+
		"2\u00e9\u0001\u0000\u0000\u00004\u00eb\u0001\u0000\u0000\u00006\u00ef"+
		"\u0001\u0000\u0000\u00008\u00f1\u0001\u0000\u0000\u0000:\u00ff\u0001\u0000"+
		"\u0000\u0000<\u0101\u0001\u0000\u0000\u0000>\u0103\u0001\u0000\u0000\u0000"+
		"@A\u0003\u0002\u0001\u0000A\u0001\u0001\u0000\u0000\u0000BC\u0003\u0004"+
		"\u0002\u0000C\u0003\u0001\u0000\u0000\u0000DI\u0003\u0006\u0003\u0000"+
		"EF\u0005\u0016\u0000\u0000FH\u0003\u0006\u0003\u0000GE\u0001\u0000\u0000"+
		"\u0000HK\u0001\u0000\u0000\u0000IG\u0001\u0000\u0000\u0000IJ\u0001\u0000"+
		"\u0000\u0000J\u0005\u0001\u0000\u0000\u0000KI\u0001\u0000\u0000\u0000"+
		"LQ\u0003\b\u0004\u0000MN\u0005\u0015\u0000\u0000NP\u0003\b\u0004\u0000"+
		"OM\u0001\u0000\u0000\u0000PS\u0001\u0000\u0000\u0000QO\u0001\u0000\u0000"+
		"\u0000QR\u0001\u0000\u0000\u0000R\u0007\u0001\u0000\u0000\u0000SQ\u0001"+
		"\u0000\u0000\u0000TY\u0003\n\u0005\u0000UV\u0007\u0000\u0000\u0000VX\u0003"+
		"\n\u0005\u0000WU\u0001\u0000\u0000\u0000X[\u0001\u0000\u0000\u0000YW\u0001"+
		"\u0000\u0000\u0000YZ\u0001\u0000\u0000\u0000Z\t\u0001\u0000\u0000\u0000"+
		"[Y\u0001\u0000\u0000\u0000\\a\u0003\f\u0006\u0000]^\u0007\u0001\u0000"+
		"\u0000^`\u0003\f\u0006\u0000_]\u0001\u0000\u0000\u0000`c\u0001\u0000\u0000"+
		"\u0000a_\u0001\u0000\u0000\u0000ab\u0001\u0000\u0000\u0000b\u000b\u0001"+
		"\u0000\u0000\u0000ca\u0001\u0000\u0000\u0000de\u0003\u000e\u0007\u0000"+
		"e\r\u0001\u0000\u0000\u0000fg\u0003\u0010\b\u0000g\u000f\u0001\u0000\u0000"+
		"\u0000hl\u0003\u0012\t\u0000ij\u0003\"\u0011\u0000jk\u0003 \u0010\u0000"+
		"km\u0001\u0000\u0000\u0000li\u0001\u0000\u0000\u0000lm\u0001\u0000\u0000"+
		"\u0000m|\u0001\u0000\u0000\u0000nr\u0003\u0014\n\u0000op\u0003\"\u0011"+
		"\u0000pq\u0003 \u0010\u0000qs\u0001\u0000\u0000\u0000ro\u0001\u0000\u0000"+
		"\u0000rs\u0001\u0000\u0000\u0000s|\u0001\u0000\u0000\u0000tx\u0003\u0016"+
		"\u000b\u0000uv\u0003\"\u0011\u0000vw\u0003 \u0010\u0000wy\u0001\u0000"+
		"\u0000\u0000xu\u0001\u0000\u0000\u0000xy\u0001\u0000\u0000\u0000y|\u0001"+
		"\u0000\u0000\u0000z|\u0003\u001c\u000e\u0000{h\u0001\u0000\u0000\u0000"+
		"{n\u0001\u0000\u0000\u0000{t\u0001\u0000\u0000\u0000{z\u0001\u0000\u0000"+
		"\u0000|\u0011\u0001\u0000\u0000\u0000}\u0081\u00038\u001c\u0000~\u0080"+
		"\u00034\u001a\u0000\u007f~\u0001\u0000\u0000\u0000\u0080\u0083\u0001\u0000"+
		"\u0000\u0000\u0081\u007f\u0001\u0000\u0000\u0000\u0081\u0082\u0001\u0000"+
		"\u0000\u0000\u0082\u0013\u0001\u0000\u0000\u0000\u0083\u0081\u0001\u0000"+
		"\u0000\u0000\u0084\u0085\u0005\u0007\u0000\u0000\u0085\u0086\u0003\u0002"+
		"\u0001\u0000\u0086\u008a\u0005\b\u0000\u0000\u0087\u0089\u00034\u001a"+
		"\u0000\u0088\u0087\u0001\u0000\u0000\u0000\u0089\u008c\u0001\u0000\u0000"+
		"\u0000\u008a\u0088\u0001\u0000\u0000\u0000\u008a\u008b\u0001\u0000\u0000"+
		"\u0000\u008b\u0015\u0001\u0000\u0000\u0000\u008c\u008a\u0001\u0000\u0000"+
		"\u0000\u008d\u0091\u0003>\u001f\u0000\u008e\u0090\u00034\u001a\u0000\u008f"+
		"\u008e\u0001\u0000\u0000\u0000\u0090\u0093\u0001\u0000\u0000\u0000\u0091"+
		"\u008f\u0001\u0000\u0000\u0000\u0091\u0092\u0001\u0000\u0000\u0000\u0092"+
		"\u009c\u0001\u0000\u0000\u0000\u0093\u0091\u0001\u0000\u0000\u0000\u0094"+
		"\u0098\u0005\u0014\u0000\u0000\u0095\u0097\u00034\u001a\u0000\u0096\u0095"+
		"\u0001\u0000\u0000\u0000\u0097\u009a\u0001\u0000\u0000\u0000\u0098\u0096"+
		"\u0001\u0000\u0000\u0000\u0098\u0099\u0001\u0000\u0000\u0000\u0099\u009c"+
		"\u0001\u0000\u0000\u0000\u009a\u0098\u0001\u0000\u0000\u0000\u009b\u008d"+
		"\u0001\u0000\u0000\u0000\u009b\u0094\u0001\u0000\u0000\u0000\u009c\u0017"+
		"\u0001\u0000\u0000\u0000\u009d\u00a1\u0003\u0012\t\u0000\u009e\u00a1\u0003"+
		"\u0014\n\u0000\u009f\u00a1\u0003\u0016\u000b\u0000\u00a0\u009d\u0001\u0000"+
		"\u0000\u0000\u00a0\u009e\u0001\u0000\u0000\u0000\u00a0\u009f\u0001\u0000"+
		"\u0000\u0000\u00a1\u0019\u0001\u0000\u0000\u0000\u00a2\u00a3\u0005\u0007"+
		"\u0000\u0000\u00a3\u00a4\u0003\u0002\u0001\u0000\u00a4\u00a5\u0005\b\u0000"+
		"\u0000\u00a5\u00aa\u0001\u0000\u0000\u0000\u00a6\u00aa\u0003>\u001f\u0000"+
		"\u00a7\u00aa\u0005\u0014\u0000\u0000\u00a8\u00aa\u00038\u001c\u0000\u00a9"+
		"\u00a2\u0001\u0000\u0000\u0000\u00a9\u00a6\u0001\u0000\u0000\u0000\u00a9"+
		"\u00a7\u0001\u0000\u0000\u0000\u00a9\u00a8\u0001\u0000\u0000\u0000\u00aa"+
		"\u001b\u0001\u0000\u0000\u0000\u00ab\u00ae\u0003\u001e\u000f\u0000\u00ac"+
		"\u00ae\u0003 \u0010\u0000\u00ad\u00ab\u0001\u0000\u0000\u0000\u00ad\u00ac"+
		"\u0001\u0000\u0000\u0000\u00ae\u001d\u0001\u0000\u0000\u0000\u00af\u00b1"+
		"\u0005\u0002\u0000\u0000\u00b0\u00b2\u0003 \u0010\u0000\u00b1\u00b0\u0001"+
		"\u0000\u0000\u0000\u00b1\u00b2\u0001\u0000\u0000\u0000\u00b2\u00b6\u0001"+
		"\u0000\u0000\u0000\u00b3\u00b4\u0005\u0003\u0000\u0000\u00b4\u00b6\u0003"+
		" \u0010\u0000\u00b5\u00af\u0001\u0000\u0000\u0000\u00b5\u00b3\u0001\u0000"+
		"\u0000\u0000\u00b6\u001f\u0001\u0000\u0000\u0000\u00b7\u00bd\u0003$\u0012"+
		"\u0000\u00b8\u00b9\u0003\"\u0011\u0000\u00b9\u00ba\u0003$\u0012\u0000"+
		"\u00ba\u00bc\u0001\u0000\u0000\u0000\u00bb\u00b8\u0001\u0000\u0000\u0000"+
		"\u00bc\u00bf\u0001\u0000\u0000\u0000\u00bd\u00bb\u0001\u0000\u0000\u0000"+
		"\u00bd\u00be\u0001\u0000\u0000\u0000\u00be!\u0001\u0000\u0000\u0000\u00bf"+
		"\u00bd\u0001\u0000\u0000\u0000\u00c0\u00c1\u0007\u0002\u0000\u0000\u00c1"+
		"#\u0001\u0000\u0000\u0000\u00c2\u00c4\u0003&\u0013\u0000\u00c3\u00c2\u0001"+
		"\u0000\u0000\u0000\u00c3\u00c4\u0001\u0000\u0000\u0000\u00c4\u00c5\u0001"+
		"\u0000\u0000\u0000\u00c5\u00c9\u0003.\u0017\u0000\u00c6\u00c8\u00034\u001a"+
		"\u0000\u00c7\u00c6\u0001\u0000\u0000\u0000\u00c8\u00cb\u0001\u0000\u0000"+
		"\u0000\u00c9\u00c7\u0001\u0000\u0000\u0000\u00c9\u00ca\u0001\u0000\u0000"+
		"\u0000\u00ca\u00d5\u0001\u0000\u0000\u0000\u00cb\u00c9\u0001\u0000\u0000"+
		"\u0000\u00cc\u00d0\u0003,\u0016\u0000\u00cd\u00cf\u00034\u001a\u0000\u00ce"+
		"\u00cd\u0001\u0000\u0000\u0000\u00cf\u00d2\u0001\u0000\u0000\u0000\u00d0"+
		"\u00ce\u0001\u0000\u0000\u0000\u00d0\u00d1\u0001\u0000\u0000\u0000\u00d1"+
		"\u00d5\u0001\u0000\u0000\u0000\u00d2\u00d0\u0001\u0000\u0000\u0000\u00d3"+
		"\u00d5\u0003*\u0015\u0000\u00d4\u00c3\u0001\u0000\u0000\u0000\u00d4\u00cc"+
		"\u0001\u0000\u0000\u0000\u00d4\u00d3\u0001\u0000\u0000\u0000\u00d5%\u0001"+
		"\u0000\u0000\u0000\u00d6\u00d7\u0003(\u0014\u0000\u00d7\u00d8\u0005\u0004"+
		"\u0000\u0000\u00d8\'\u0001\u0000\u0000\u0000\u00d9\u00da\u0005\u001d\u0000"+
		"\u0000\u00da)\u0001\u0000\u0000\u0000\u00db\u00dc\u0007\u0003\u0000\u0000"+
		"\u00dc+\u0001\u0000\u0000\u0000\u00dd\u00de\u0005\t\u0000\u0000\u00de"+
		"\u00df\u0007\u0004\u0000\u0000\u00df-\u0001\u0000\u0000\u0000\u00e0\u00e6"+
		"\u00030\u0018\u0000\u00e1\u00e2\u00032\u0019\u0000\u00e2\u00e3\u0005\u0007"+
		"\u0000\u0000\u00e3\u00e4\u0005\b\u0000\u0000\u00e4\u00e6\u0001\u0000\u0000"+
		"\u0000\u00e5\u00e0\u0001\u0000\u0000\u0000\u00e5\u00e1\u0001\u0000\u0000"+
		"\u0000\u00e6/\u0001\u0000\u0000\u0000\u00e7\u00e8\u0007\u0004\u0000\u0000"+
		"\u00e81\u0001\u0000\u0000\u0000\u00e9\u00ea\u0007\u0005\u0000\u0000\u00ea"+
		"3\u0001\u0000\u0000\u0000\u00eb\u00ec\u0005\u0005\u0000\u0000\u00ec\u00ed"+
		"\u00036\u001b\u0000\u00ed\u00ee\u0005\u0006\u0000\u0000\u00ee5\u0001\u0000"+
		"\u0000\u0000\u00ef\u00f0\u0003\u0002\u0001\u0000\u00f07\u0001\u0000\u0000"+
		"\u0000\u00f1\u00f2\u0003:\u001d\u0000\u00f2\u00fb\u0005\u0007\u0000\u0000"+
		"\u00f3\u00f8\u0003<\u001e\u0000\u00f4\u00f5\u0005\f\u0000\u0000\u00f5"+
		"\u00f7\u0003<\u001e\u0000\u00f6\u00f4\u0001\u0000\u0000\u0000\u00f7\u00fa"+
		"\u0001\u0000\u0000\u0000\u00f8\u00f6\u0001\u0000\u0000\u0000\u00f8\u00f9"+
		"\u0001\u0000\u0000\u0000\u00f9\u00fc\u0001\u0000\u0000\u0000\u00fa\u00f8"+
		"\u0001\u0000\u0000\u0000\u00fb\u00f3\u0001\u0000\u0000\u0000\u00fb\u00fc"+
		"\u0001\u0000\u0000\u0000\u00fc\u00fd\u0001\u0000\u0000\u0000\u00fd\u00fe"+
		"\u0005\b\u0000\u0000\u00fe9\u0001\u0000\u0000\u0000\u00ff\u0100\u0007"+
		"\u0006\u0000\u0000\u0100;\u0001\u0000\u0000\u0000\u0101\u0102\u0003\u0002"+
		"\u0001\u0000\u0102=\u0001\u0000\u0000\u0000\u0103\u0104\u0005\u001b\u0000"+
		"\u0000\u0104?\u0001\u0000\u0000\u0000\u001aIQYalrx{\u0081\u008a\u0091"+
		"\u0098\u009b\u00a0\u00a9\u00ad\u00b1\u00b5\u00bd\u00c3\u00c9\u00d0\u00d4"+
		"\u00e5\u00f8\u00fb";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
