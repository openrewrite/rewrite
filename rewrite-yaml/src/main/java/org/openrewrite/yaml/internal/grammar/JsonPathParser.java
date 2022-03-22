/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.yaml.internal.grammar;
// Generated from /Users/yoshi/Development/Repos/openrewrite/rewrite/rewrite-yaml/src/main/antlr/JsonPathParser.g4 by ANTLR 4.9.2

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class JsonPathParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.9.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		WS=1, UTF_8_BOM=2, MATCHES_REGEX_OPEN=3, LBRACE=4, RBRACE=5, LBRACK=6, 
		RBRACK=7, LPAREN=8, RPAREN=9, AT=10, DOT=11, DOT_DOT=12, ROOT=13, WILDCARD=14, 
		COLON=15, QUESTION=16, Identifier=17, StringLiteral=18, PositiveNumber=19, 
		NegativeNumber=20, NumericLiteral=21, COMMA=22, TICK=23, QUOTE=24, MATCHES=25, 
		LOGICAL_OPERATOR=26, AND=27, OR=28, EQUALITY_OPERATOR=29, EQ=30, NE=31, 
		TRUE=32, FALSE=33, NULL=34, MATCHES_REGEX_CLOSE=35, S=36, REGEX=37;
	public static final int
		RULE_jsonPath = 0, RULE_expression = 1, RULE_dotOperator = 2, RULE_recursiveDecent = 3, 
		RULE_bracketOperator = 4, RULE_filter = 5, RULE_filterExpression = 6, 
		RULE_binaryExpression = 7, RULE_regexExpression = 8, RULE_unaryExpression = 9, 
		RULE_literalExpression = 10, RULE_property = 11, RULE_wildcard = 12, RULE_slice = 13, 
		RULE_start = 14, RULE_end = 15, RULE_indexes = 16;
	private static String[] makeRuleNames() {
		return new String[] {
			"jsonPath", "expression", "dotOperator", "recursiveDecent", "bracketOperator", 
			"filter", "filterExpression", "binaryExpression", "regexExpression", 
			"unaryExpression", "literalExpression", "property", "wildcard", "slice", 
			"start", "end", "indexes"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, "'\uFEFF'", null, "'{'", "'}'", "'['", "']'", "'('", "')'", 
			"'@'", "'.'", "'..'", "'$'", "'*'", "':'", "'?'", null, null, null, null, 
			null, "','", "'''", "'\"'", "'=~'", null, "'&&'", "'||'", null, "'=='", 
			"'!='", "'true'", "'false'", "'null'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "WS", "UTF_8_BOM", "MATCHES_REGEX_OPEN", "LBRACE", "RBRACE", "LBRACK", 
			"RBRACK", "LPAREN", "RPAREN", "AT", "DOT", "DOT_DOT", "ROOT", "WILDCARD", 
			"COLON", "QUESTION", "Identifier", "StringLiteral", "PositiveNumber", 
			"NegativeNumber", "NumericLiteral", "COMMA", "TICK", "QUOTE", "MATCHES", 
			"LOGICAL_OPERATOR", "AND", "OR", "EQUALITY_OPERATOR", "EQ", "NE", "TRUE", 
			"FALSE", "NULL", "MATCHES_REGEX_CLOSE", "S", "REGEX"
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
	public String getGrammarFileName() { return "JsonPathParser.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public JsonPathParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	public static class JsonPathContext extends ParserRuleContext {
		public TerminalNode ROOT() { return getToken(JsonPathParser.ROOT, 0); }
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public JsonPathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_jsonPath; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).enterJsonPath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).exitJsonPath(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JsonPathParserVisitor ) return ((JsonPathParserVisitor<? extends T>)visitor).visitJsonPath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final JsonPathContext jsonPath() throws RecognitionException {
		JsonPathContext _localctx = new JsonPathContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_jsonPath);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(35);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ROOT) {
				{
				setState(34);
				match(ROOT);
				}
			}

			setState(38); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(37);
					expression();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(40); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,1,_ctx);
			} while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER );
			}
		}
		catch (RecognitionException re) {
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
		public TerminalNode DOT() { return getToken(JsonPathParser.DOT, 0); }
		public DotOperatorContext dotOperator() {
			return getRuleContext(DotOperatorContext.class,0);
		}
		public RecursiveDecentContext recursiveDecent() {
			return getRuleContext(RecursiveDecentContext.class,0);
		}
		public BracketOperatorContext bracketOperator() {
			return getRuleContext(BracketOperatorContext.class,0);
		}
		public ExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).enterExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).exitExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JsonPathParserVisitor ) return ((JsonPathParserVisitor<? extends T>)visitor).visitExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionContext expression() throws RecognitionException {
		ExpressionContext _localctx = new ExpressionContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_expression);
		try {
			setState(46);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DOT:
				enterOuterAlt(_localctx, 1);
				{
				setState(42);
				match(DOT);
				setState(43);
				dotOperator();
				}
				break;
			case DOT_DOT:
				enterOuterAlt(_localctx, 2);
				{
				setState(44);
				recursiveDecent();
				}
				break;
			case LBRACK:
				enterOuterAlt(_localctx, 3);
				{
				setState(45);
				bracketOperator();
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

	public static class DotOperatorContext extends ParserRuleContext {
		public BracketOperatorContext bracketOperator() {
			return getRuleContext(BracketOperatorContext.class,0);
		}
		public PropertyContext property() {
			return getRuleContext(PropertyContext.class,0);
		}
		public WildcardContext wildcard() {
			return getRuleContext(WildcardContext.class,0);
		}
		public DotOperatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dotOperator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).enterDotOperator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).exitDotOperator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JsonPathParserVisitor ) return ((JsonPathParserVisitor<? extends T>)visitor).visitDotOperator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DotOperatorContext dotOperator() throws RecognitionException {
		DotOperatorContext _localctx = new DotOperatorContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_dotOperator);
		try {
			setState(51);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LBRACK:
				enterOuterAlt(_localctx, 1);
				{
				setState(48);
				bracketOperator();
				}
				break;
			case Identifier:
			case StringLiteral:
				enterOuterAlt(_localctx, 2);
				{
				setState(49);
				property();
				}
				break;
			case WILDCARD:
				enterOuterAlt(_localctx, 3);
				{
				setState(50);
				wildcard();
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

	public static class RecursiveDecentContext extends ParserRuleContext {
		public TerminalNode DOT_DOT() { return getToken(JsonPathParser.DOT_DOT, 0); }
		public DotOperatorContext dotOperator() {
			return getRuleContext(DotOperatorContext.class,0);
		}
		public RecursiveDecentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_recursiveDecent; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).enterRecursiveDecent(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).exitRecursiveDecent(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JsonPathParserVisitor ) return ((JsonPathParserVisitor<? extends T>)visitor).visitRecursiveDecent(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RecursiveDecentContext recursiveDecent() throws RecognitionException {
		RecursiveDecentContext _localctx = new RecursiveDecentContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_recursiveDecent);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(53);
			match(DOT_DOT);
			setState(54);
			dotOperator();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class BracketOperatorContext extends ParserRuleContext {
		public TerminalNode LBRACK() { return getToken(JsonPathParser.LBRACK, 0); }
		public TerminalNode RBRACK() { return getToken(JsonPathParser.RBRACK, 0); }
		public FilterContext filter() {
			return getRuleContext(FilterContext.class,0);
		}
		public SliceContext slice() {
			return getRuleContext(SliceContext.class,0);
		}
		public IndexesContext indexes() {
			return getRuleContext(IndexesContext.class,0);
		}
		public List<PropertyContext> property() {
			return getRuleContexts(PropertyContext.class);
		}
		public PropertyContext property(int i) {
			return getRuleContext(PropertyContext.class,i);
		}
		public BracketOperatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_bracketOperator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).enterBracketOperator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).exitBracketOperator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JsonPathParserVisitor ) return ((JsonPathParserVisitor<? extends T>)visitor).visitBracketOperator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BracketOperatorContext bracketOperator() throws RecognitionException {
		BracketOperatorContext _localctx = new BracketOperatorContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_bracketOperator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(56);
			match(LBRACK);
			setState(65);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,5,_ctx) ) {
			case 1:
				{
				setState(57);
				filter();
				}
				break;
			case 2:
				{
				setState(58);
				slice();
				}
				break;
			case 3:
				{
				setState(59);
				indexes();
				}
				break;
			case 4:
				{
				setState(61); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(60);
					property();
					}
					}
					setState(63); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==Identifier || _la==StringLiteral );
				}
				break;
			}
			setState(67);
			match(RBRACK);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FilterContext extends ParserRuleContext {
		public TerminalNode QUESTION() { return getToken(JsonPathParser.QUESTION, 0); }
		public TerminalNode LPAREN() { return getToken(JsonPathParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(JsonPathParser.RPAREN, 0); }
		public List<FilterExpressionContext> filterExpression() {
			return getRuleContexts(FilterExpressionContext.class);
		}
		public FilterExpressionContext filterExpression(int i) {
			return getRuleContext(FilterExpressionContext.class,i);
		}
		public FilterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_filter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).enterFilter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).exitFilter(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JsonPathParserVisitor ) return ((JsonPathParserVisitor<? extends T>)visitor).visitFilter(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FilterContext filter() throws RecognitionException {
		FilterContext _localctx = new FilterContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_filter);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(69);
			match(QUESTION);
			setState(70);
			match(LPAREN);
			setState(72); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(71);
				filterExpression();
				}
				}
				setState(74); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << LBRACK) | (1L << AT) | (1L << DOT) | (1L << DOT_DOT) | (1L << ROOT) | (1L << StringLiteral) | (1L << PositiveNumber) | (1L << NegativeNumber) | (1L << NumericLiteral) | (1L << TRUE) | (1L << FALSE) | (1L << NULL))) != 0) );
			setState(76);
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

	public static class FilterExpressionContext extends ParserRuleContext {
		public BinaryExpressionContext binaryExpression() {
			return getRuleContext(BinaryExpressionContext.class,0);
		}
		public RegexExpressionContext regexExpression() {
			return getRuleContext(RegexExpressionContext.class,0);
		}
		public UnaryExpressionContext unaryExpression() {
			return getRuleContext(UnaryExpressionContext.class,0);
		}
		public FilterExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_filterExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).enterFilterExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).exitFilterExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JsonPathParserVisitor ) return ((JsonPathParserVisitor<? extends T>)visitor).visitFilterExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FilterExpressionContext filterExpression() throws RecognitionException {
		FilterExpressionContext _localctx = new FilterExpressionContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_filterExpression);
		try {
			setState(81);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(78);
				binaryExpression(0);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(79);
				regexExpression();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(80);
				unaryExpression();
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

	public static class BinaryExpressionContext extends ParserRuleContext {
		public List<RegexExpressionContext> regexExpression() {
			return getRuleContexts(RegexExpressionContext.class);
		}
		public RegexExpressionContext regexExpression(int i) {
			return getRuleContext(RegexExpressionContext.class,i);
		}
		public TerminalNode LOGICAL_OPERATOR() { return getToken(JsonPathParser.LOGICAL_OPERATOR, 0); }
		public List<BinaryExpressionContext> binaryExpression() {
			return getRuleContexts(BinaryExpressionContext.class);
		}
		public BinaryExpressionContext binaryExpression(int i) {
			return getRuleContext(BinaryExpressionContext.class,i);
		}
		public UnaryExpressionContext unaryExpression() {
			return getRuleContext(UnaryExpressionContext.class,0);
		}
		public TerminalNode EQUALITY_OPERATOR() { return getToken(JsonPathParser.EQUALITY_OPERATOR, 0); }
		public LiteralExpressionContext literalExpression() {
			return getRuleContext(LiteralExpressionContext.class,0);
		}
		public BinaryExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_binaryExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).enterBinaryExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).exitBinaryExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JsonPathParserVisitor ) return ((JsonPathParserVisitor<? extends T>)visitor).visitBinaryExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BinaryExpressionContext binaryExpression() throws RecognitionException {
		return binaryExpression(0);
	}

	private BinaryExpressionContext binaryExpression(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		BinaryExpressionContext _localctx = new BinaryExpressionContext(_ctx, _parentState);
		BinaryExpressionContext _prevctx = _localctx;
		int _startState = 14;
		enterRecursionRule(_localctx, 14, RULE_binaryExpression, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(100);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,8,_ctx) ) {
			case 1:
				{
				setState(84);
				regexExpression();
				setState(85);
				match(LOGICAL_OPERATOR);
				setState(86);
				regexExpression();
				}
				break;
			case 2:
				{
				setState(88);
				regexExpression();
				setState(89);
				match(LOGICAL_OPERATOR);
				setState(90);
				binaryExpression(3);
				}
				break;
			case 3:
				{
				setState(92);
				unaryExpression();
				setState(93);
				match(EQUALITY_OPERATOR);
				setState(94);
				literalExpression();
				}
				break;
			case 4:
				{
				setState(96);
				literalExpression();
				setState(97);
				match(EQUALITY_OPERATOR);
				setState(98);
				unaryExpression();
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(110);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,10,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(108);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,9,_ctx) ) {
					case 1:
						{
						_localctx = new BinaryExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_binaryExpression);
						setState(102);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(103);
						match(LOGICAL_OPERATOR);
						setState(104);
						binaryExpression(7);
						}
						break;
					case 2:
						{
						_localctx = new BinaryExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_binaryExpression);
						setState(105);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(106);
						match(LOGICAL_OPERATOR);
						setState(107);
						regexExpression();
						}
						break;
					}
					} 
				}
				setState(112);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,10,_ctx);
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

	public static class RegexExpressionContext extends ParserRuleContext {
		public TerminalNode AT() { return getToken(JsonPathParser.AT, 0); }
		public TerminalNode MATCHES_REGEX_OPEN() { return getToken(JsonPathParser.MATCHES_REGEX_OPEN, 0); }
		public TerminalNode REGEX() { return getToken(JsonPathParser.REGEX, 0); }
		public TerminalNode MATCHES_REGEX_CLOSE() { return getToken(JsonPathParser.MATCHES_REGEX_CLOSE, 0); }
		public TerminalNode DOT() { return getToken(JsonPathParser.DOT, 0); }
		public PropertyContext property() {
			return getRuleContext(PropertyContext.class,0);
		}
		public TerminalNode LBRACK() { return getToken(JsonPathParser.LBRACK, 0); }
		public TerminalNode StringLiteral() { return getToken(JsonPathParser.StringLiteral, 0); }
		public TerminalNode RBRACK() { return getToken(JsonPathParser.RBRACK, 0); }
		public RegexExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_regexExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).enterRegexExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).exitRegexExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JsonPathParserVisitor ) return ((JsonPathParserVisitor<? extends T>)visitor).visitRegexExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RegexExpressionContext regexExpression() throws RecognitionException {
		RegexExpressionContext _localctx = new RegexExpressionContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_regexExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(113);
			match(AT);
			setState(122);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,12,_ctx) ) {
			case 1:
				{
				setState(114);
				match(DOT);
				setState(115);
				property();
				}
				break;
			case 2:
				{
				setState(117);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==DOT) {
					{
					setState(116);
					match(DOT);
					}
				}

				setState(119);
				match(LBRACK);
				setState(120);
				match(StringLiteral);
				setState(121);
				match(RBRACK);
				}
				break;
			}
			setState(124);
			match(MATCHES_REGEX_OPEN);
			setState(125);
			match(REGEX);
			setState(126);
			match(MATCHES_REGEX_CLOSE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class UnaryExpressionContext extends ParserRuleContext {
		public TerminalNode AT() { return getToken(JsonPathParser.AT, 0); }
		public TerminalNode DOT() { return getToken(JsonPathParser.DOT, 0); }
		public TerminalNode Identifier() { return getToken(JsonPathParser.Identifier, 0); }
		public TerminalNode LBRACK() { return getToken(JsonPathParser.LBRACK, 0); }
		public TerminalNode StringLiteral() { return getToken(JsonPathParser.StringLiteral, 0); }
		public TerminalNode RBRACK() { return getToken(JsonPathParser.RBRACK, 0); }
		public JsonPathContext jsonPath() {
			return getRuleContext(JsonPathContext.class,0);
		}
		public UnaryExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unaryExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).enterUnaryExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).exitUnaryExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JsonPathParserVisitor ) return ((JsonPathParserVisitor<? extends T>)visitor).visitUnaryExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UnaryExpressionContext unaryExpression() throws RecognitionException {
		UnaryExpressionContext _localctx = new UnaryExpressionContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_unaryExpression);
		int _la;
		try {
			setState(140);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case AT:
				enterOuterAlt(_localctx, 1);
				{
				setState(128);
				match(AT);
				setState(137);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,14,_ctx) ) {
				case 1:
					{
					setState(129);
					match(DOT);
					setState(130);
					match(Identifier);
					}
					break;
				case 2:
					{
					setState(132);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==DOT) {
						{
						setState(131);
						match(DOT);
						}
					}

					setState(134);
					match(LBRACK);
					setState(135);
					match(StringLiteral);
					setState(136);
					match(RBRACK);
					}
					break;
				}
				}
				break;
			case LBRACK:
			case DOT:
			case DOT_DOT:
			case ROOT:
				enterOuterAlt(_localctx, 2);
				{
				setState(139);
				jsonPath();
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

	public static class LiteralExpressionContext extends ParserRuleContext {
		public TerminalNode StringLiteral() { return getToken(JsonPathParser.StringLiteral, 0); }
		public TerminalNode PositiveNumber() { return getToken(JsonPathParser.PositiveNumber, 0); }
		public TerminalNode NegativeNumber() { return getToken(JsonPathParser.NegativeNumber, 0); }
		public TerminalNode NumericLiteral() { return getToken(JsonPathParser.NumericLiteral, 0); }
		public TerminalNode TRUE() { return getToken(JsonPathParser.TRUE, 0); }
		public TerminalNode FALSE() { return getToken(JsonPathParser.FALSE, 0); }
		public TerminalNode NULL() { return getToken(JsonPathParser.NULL, 0); }
		public LiteralExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_literalExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).enterLiteralExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).exitLiteralExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JsonPathParserVisitor ) return ((JsonPathParserVisitor<? extends T>)visitor).visitLiteralExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LiteralExpressionContext literalExpression() throws RecognitionException {
		LiteralExpressionContext _localctx = new LiteralExpressionContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_literalExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(142);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << StringLiteral) | (1L << PositiveNumber) | (1L << NegativeNumber) | (1L << NumericLiteral) | (1L << TRUE) | (1L << FALSE) | (1L << NULL))) != 0)) ) {
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

	public static class PropertyContext extends ParserRuleContext {
		public TerminalNode StringLiteral() { return getToken(JsonPathParser.StringLiteral, 0); }
		public TerminalNode Identifier() { return getToken(JsonPathParser.Identifier, 0); }
		public PropertyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_property; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).enterProperty(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).exitProperty(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JsonPathParserVisitor ) return ((JsonPathParserVisitor<? extends T>)visitor).visitProperty(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PropertyContext property() throws RecognitionException {
		PropertyContext _localctx = new PropertyContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_property);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(144);
			_la = _input.LA(1);
			if ( !(_la==Identifier || _la==StringLiteral) ) {
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

	public static class WildcardContext extends ParserRuleContext {
		public TerminalNode WILDCARD() { return getToken(JsonPathParser.WILDCARD, 0); }
		public WildcardContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_wildcard; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).enterWildcard(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).exitWildcard(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JsonPathParserVisitor ) return ((JsonPathParserVisitor<? extends T>)visitor).visitWildcard(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WildcardContext wildcard() throws RecognitionException {
		WildcardContext _localctx = new WildcardContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_wildcard);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(146);
			match(WILDCARD);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SliceContext extends ParserRuleContext {
		public StartContext start() {
			return getRuleContext(StartContext.class,0);
		}
		public TerminalNode COLON() { return getToken(JsonPathParser.COLON, 0); }
		public EndContext end() {
			return getRuleContext(EndContext.class,0);
		}
		public TerminalNode PositiveNumber() { return getToken(JsonPathParser.PositiveNumber, 0); }
		public TerminalNode NegativeNumber() { return getToken(JsonPathParser.NegativeNumber, 0); }
		public WildcardContext wildcard() {
			return getRuleContext(WildcardContext.class,0);
		}
		public SliceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_slice; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).enterSlice(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).exitSlice(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JsonPathParserVisitor ) return ((JsonPathParserVisitor<? extends T>)visitor).visitSlice(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SliceContext slice() throws RecognitionException {
		SliceContext _localctx = new SliceContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_slice);
		int _la;
		try {
			setState(158);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case PositiveNumber:
				enterOuterAlt(_localctx, 1);
				{
				setState(148);
				start();
				setState(149);
				match(COLON);
				setState(151);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PositiveNumber) {
					{
					setState(150);
					end();
					}
				}

				}
				break;
			case COLON:
				enterOuterAlt(_localctx, 2);
				{
				setState(153);
				match(COLON);
				setState(154);
				match(PositiveNumber);
				}
				break;
			case NegativeNumber:
				enterOuterAlt(_localctx, 3);
				{
				setState(155);
				match(NegativeNumber);
				setState(156);
				match(COLON);
				}
				break;
			case WILDCARD:
				enterOuterAlt(_localctx, 4);
				{
				setState(157);
				wildcard();
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

	public static class StartContext extends ParserRuleContext {
		public TerminalNode PositiveNumber() { return getToken(JsonPathParser.PositiveNumber, 0); }
		public StartContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_start; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).enterStart(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).exitStart(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JsonPathParserVisitor ) return ((JsonPathParserVisitor<? extends T>)visitor).visitStart(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StartContext start() throws RecognitionException {
		StartContext _localctx = new StartContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_start);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(160);
			match(PositiveNumber);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class EndContext extends ParserRuleContext {
		public TerminalNode PositiveNumber() { return getToken(JsonPathParser.PositiveNumber, 0); }
		public EndContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_end; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).enterEnd(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).exitEnd(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JsonPathParserVisitor ) return ((JsonPathParserVisitor<? extends T>)visitor).visitEnd(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EndContext end() throws RecognitionException {
		EndContext _localctx = new EndContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_end);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(162);
			match(PositiveNumber);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class IndexesContext extends ParserRuleContext {
		public List<TerminalNode> PositiveNumber() { return getTokens(JsonPathParser.PositiveNumber); }
		public TerminalNode PositiveNumber(int i) {
			return getToken(JsonPathParser.PositiveNumber, i);
		}
		public IndexesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_indexes; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).enterIndexes(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).exitIndexes(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JsonPathParserVisitor ) return ((JsonPathParserVisitor<? extends T>)visitor).visitIndexes(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IndexesContext indexes() throws RecognitionException {
		IndexesContext _localctx = new IndexesContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_indexes);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(165); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(164);
				match(PositiveNumber);
				}
				}
				setState(167); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==PositiveNumber );
			}
		}
		catch (RecognitionException re) {
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
		case 7:
			return binaryExpression_sempred((BinaryExpressionContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean binaryExpression_sempred(BinaryExpressionContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 6);
		case 1:
			return precpred(_ctx, 5);
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\'\u00ac\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\3\2\5\2&\n\2\3\2\6\2)\n\2\r\2\16\2*\3\3\3\3\3\3\3\3\5\3\61\n\3\3\4\3"+
		"\4\3\4\5\4\66\n\4\3\5\3\5\3\5\3\6\3\6\3\6\3\6\3\6\6\6@\n\6\r\6\16\6A\5"+
		"\6D\n\6\3\6\3\6\3\7\3\7\3\7\6\7K\n\7\r\7\16\7L\3\7\3\7\3\b\3\b\3\b\5\b"+
		"T\n\b\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t"+
		"\3\t\5\tg\n\t\3\t\3\t\3\t\3\t\3\t\3\t\7\to\n\t\f\t\16\tr\13\t\3\n\3\n"+
		"\3\n\3\n\5\nx\n\n\3\n\3\n\3\n\5\n}\n\n\3\n\3\n\3\n\3\n\3\13\3\13\3\13"+
		"\3\13\5\13\u0087\n\13\3\13\3\13\3\13\5\13\u008c\n\13\3\13\5\13\u008f\n"+
		"\13\3\f\3\f\3\r\3\r\3\16\3\16\3\17\3\17\3\17\5\17\u009a\n\17\3\17\3\17"+
		"\3\17\3\17\3\17\5\17\u00a1\n\17\3\20\3\20\3\21\3\21\3\22\6\22\u00a8\n"+
		"\22\r\22\16\22\u00a9\3\22\2\3\20\23\2\4\6\b\n\f\16\20\22\24\26\30\32\34"+
		"\36 \"\2\4\4\2\24\27\"$\3\2\23\24\2\u00b6\2%\3\2\2\2\4\60\3\2\2\2\6\65"+
		"\3\2\2\2\b\67\3\2\2\2\n:\3\2\2\2\fG\3\2\2\2\16S\3\2\2\2\20f\3\2\2\2\22"+
		"s\3\2\2\2\24\u008e\3\2\2\2\26\u0090\3\2\2\2\30\u0092\3\2\2\2\32\u0094"+
		"\3\2\2\2\34\u00a0\3\2\2\2\36\u00a2\3\2\2\2 \u00a4\3\2\2\2\"\u00a7\3\2"+
		"\2\2$&\7\17\2\2%$\3\2\2\2%&\3\2\2\2&(\3\2\2\2\')\5\4\3\2(\'\3\2\2\2)*"+
		"\3\2\2\2*(\3\2\2\2*+\3\2\2\2+\3\3\2\2\2,-\7\r\2\2-\61\5\6\4\2.\61\5\b"+
		"\5\2/\61\5\n\6\2\60,\3\2\2\2\60.\3\2\2\2\60/\3\2\2\2\61\5\3\2\2\2\62\66"+
		"\5\n\6\2\63\66\5\30\r\2\64\66\5\32\16\2\65\62\3\2\2\2\65\63\3\2\2\2\65"+
		"\64\3\2\2\2\66\7\3\2\2\2\678\7\16\2\289\5\6\4\29\t\3\2\2\2:C\7\b\2\2;"+
		"D\5\f\7\2<D\5\34\17\2=D\5\"\22\2>@\5\30\r\2?>\3\2\2\2@A\3\2\2\2A?\3\2"+
		"\2\2AB\3\2\2\2BD\3\2\2\2C;\3\2\2\2C<\3\2\2\2C=\3\2\2\2C?\3\2\2\2DE\3\2"+
		"\2\2EF\7\t\2\2F\13\3\2\2\2GH\7\22\2\2HJ\7\n\2\2IK\5\16\b\2JI\3\2\2\2K"+
		"L\3\2\2\2LJ\3\2\2\2LM\3\2\2\2MN\3\2\2\2NO\7\13\2\2O\r\3\2\2\2PT\5\20\t"+
		"\2QT\5\22\n\2RT\5\24\13\2SP\3\2\2\2SQ\3\2\2\2SR\3\2\2\2T\17\3\2\2\2UV"+
		"\b\t\1\2VW\5\22\n\2WX\7\34\2\2XY\5\22\n\2Yg\3\2\2\2Z[\5\22\n\2[\\\7\34"+
		"\2\2\\]\5\20\t\5]g\3\2\2\2^_\5\24\13\2_`\7\37\2\2`a\5\26\f\2ag\3\2\2\2"+
		"bc\5\26\f\2cd\7\37\2\2de\5\24\13\2eg\3\2\2\2fU\3\2\2\2fZ\3\2\2\2f^\3\2"+
		"\2\2fb\3\2\2\2gp\3\2\2\2hi\f\b\2\2ij\7\34\2\2jo\5\20\t\tkl\f\7\2\2lm\7"+
		"\34\2\2mo\5\22\n\2nh\3\2\2\2nk\3\2\2\2or\3\2\2\2pn\3\2\2\2pq\3\2\2\2q"+
		"\21\3\2\2\2rp\3\2\2\2s|\7\f\2\2tu\7\r\2\2u}\5\30\r\2vx\7\r\2\2wv\3\2\2"+
		"\2wx\3\2\2\2xy\3\2\2\2yz\7\b\2\2z{\7\24\2\2{}\7\t\2\2|t\3\2\2\2|w\3\2"+
		"\2\2}~\3\2\2\2~\177\7\5\2\2\177\u0080\7\'\2\2\u0080\u0081\7%\2\2\u0081"+
		"\23\3\2\2\2\u0082\u008b\7\f\2\2\u0083\u0084\7\r\2\2\u0084\u008c\7\23\2"+
		"\2\u0085\u0087\7\r\2\2\u0086\u0085\3\2\2\2\u0086\u0087\3\2\2\2\u0087\u0088"+
		"\3\2\2\2\u0088\u0089\7\b\2\2\u0089\u008a\7\24\2\2\u008a\u008c\7\t\2\2"+
		"\u008b\u0083\3\2\2\2\u008b\u0086\3\2\2\2\u008c\u008f\3\2\2\2\u008d\u008f"+
		"\5\2\2\2\u008e\u0082\3\2\2\2\u008e\u008d\3\2\2\2\u008f\25\3\2\2\2\u0090"+
		"\u0091\t\2\2\2\u0091\27\3\2\2\2\u0092\u0093\t\3\2\2\u0093\31\3\2\2\2\u0094"+
		"\u0095\7\20\2\2\u0095\33\3\2\2\2\u0096\u0097\5\36\20\2\u0097\u0099\7\21"+
		"\2\2\u0098\u009a\5 \21\2\u0099\u0098\3\2\2\2\u0099\u009a\3\2\2\2\u009a"+
		"\u00a1\3\2\2\2\u009b\u009c\7\21\2\2\u009c\u00a1\7\25\2\2\u009d\u009e\7"+
		"\26\2\2\u009e\u00a1\7\21\2\2\u009f\u00a1\5\32\16\2\u00a0\u0096\3\2\2\2"+
		"\u00a0\u009b\3\2\2\2\u00a0\u009d\3\2\2\2\u00a0\u009f\3\2\2\2\u00a1\35"+
		"\3\2\2\2\u00a2\u00a3\7\25\2\2\u00a3\37\3\2\2\2\u00a4\u00a5\7\25\2\2\u00a5"+
		"!\3\2\2\2\u00a6\u00a8\7\25\2\2\u00a7\u00a6\3\2\2\2\u00a8\u00a9\3\2\2\2"+
		"\u00a9\u00a7\3\2\2\2\u00a9\u00aa\3\2\2\2\u00aa#\3\2\2\2\25%*\60\65ACL"+
		"Sfnpw|\u0086\u008b\u008e\u0099\u00a0\u00a9";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}