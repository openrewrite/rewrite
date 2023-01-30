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
// Generated from java-escape by ANTLR 4.11.1
package org.openrewrite.yaml.internal.grammar;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class JsonPathParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.11.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		WS=1, UTF_8_BOM=2, MATCHES_REGEX_OPEN=3, LBRACE=4, RBRACE=5, LBRACK=6, 
		RBRACK=7, LPAREN=8, RPAREN=9, AT=10, DOT=11, DOT_DOT=12, ROOT=13, WILDCARD=14, 
		COLON=15, QUESTION=16, CONTAINS=17, Identifier=18, StringLiteral=19, PositiveNumber=20, 
		NegativeNumber=21, NumericLiteral=22, COMMA=23, TICK=24, QUOTE=25, MATCHES=26, 
		LOGICAL_OPERATOR=27, AND=28, OR=29, EQUALITY_OPERATOR=30, EQ=31, NE=32, 
		TRUE=33, FALSE=34, NULL=35, MATCHES_REGEX_CLOSE=36, S=37, REGEX=38;
	public static final int
		RULE_jsonPath = 0, RULE_expression = 1, RULE_dotOperator = 2, RULE_recursiveDecent = 3, 
		RULE_bracketOperator = 4, RULE_filter = 5, RULE_filterExpression = 6, 
		RULE_binaryExpression = 7, RULE_containsExpression = 8, RULE_regexExpression = 9, 
		RULE_unaryExpression = 10, RULE_literalExpression = 11, RULE_property = 12, 
		RULE_wildcard = 13, RULE_slice = 14, RULE_start = 15, RULE_end = 16, RULE_indexes = 17;
	private static String[] makeRuleNames() {
		return new String[] {
			"jsonPath", "expression", "dotOperator", "recursiveDecent", "bracketOperator", 
			"filter", "filterExpression", "binaryExpression", "containsExpression", 
			"regexExpression", "unaryExpression", "literalExpression", "property", 
			"wildcard", "slice", "start", "end", "indexes"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, "'\\uFEFF'", null, "'{'", "'}'", "'['", "']'", "'('", "')'", 
			"'@'", "'.'", "'..'", "'$'", "'*'", "':'", "'?'", "'contains'", null, 
			null, null, null, null, "','", "'''", "'\"'", "'=~'", null, "'&&'", "'||'", 
			null, "'=='", "'!='", "'true'", "'false'", "'null'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "WS", "UTF_8_BOM", "MATCHES_REGEX_OPEN", "LBRACE", "RBRACE", "LBRACK", 
			"RBRACK", "LPAREN", "RPAREN", "AT", "DOT", "DOT_DOT", "ROOT", "WILDCARD", 
			"COLON", "QUESTION", "CONTAINS", "Identifier", "StringLiteral", "PositiveNumber", 
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
	public String getGrammarFileName() { return "java-escape"; }

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

	@SuppressWarnings("CheckReturnValue")
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
			setState(37);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ROOT) {
				{
				setState(36);
				match(ROOT);
				}
			}

			setState(40); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(39);
					expression();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(42); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,1,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
			}
		}
		catch (RecognitionException re) {
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
			setState(48);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DOT:
				enterOuterAlt(_localctx, 1);
				{
				setState(44);
				match(DOT);
				setState(45);
				dotOperator();
				}
				break;
			case DOT_DOT:
				enterOuterAlt(_localctx, 2);
				{
				setState(46);
				recursiveDecent();
				}
				break;
			case LBRACK:
				enterOuterAlt(_localctx, 3);
				{
				setState(47);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(53);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LBRACK:
				enterOuterAlt(_localctx, 1);
				{
				setState(50);
				bracketOperator();
				}
				break;
			case Identifier:
			case StringLiteral:
				enterOuterAlt(_localctx, 2);
				{
				setState(51);
				property();
				}
				break;
			case WILDCARD:
				enterOuterAlt(_localctx, 3);
				{
				setState(52);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(55);
			match(DOT_DOT);
			setState(56);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(58);
			match(LBRACK);
			setState(67);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,5,_ctx) ) {
			case 1:
				{
				setState(59);
				filter();
				}
				break;
			case 2:
				{
				setState(60);
				slice();
				}
				break;
			case 3:
				{
				setState(61);
				indexes();
				}
				break;
			case 4:
				{
				setState(63); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(62);
					property();
					}
					}
					setState(65); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==Identifier || _la==StringLiteral );
				}
				break;
			}
			setState(69);
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

	@SuppressWarnings("CheckReturnValue")
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
			setState(71);
			match(QUESTION);
			setState(72);
			match(LPAREN);
			setState(74); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(73);
				filterExpression();
				}
				}
				setState(76); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( ((_la) & ~0x3f) == 0 && ((1L << _la) & 60137421888L) != 0 );
			setState(78);
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
	public static class FilterExpressionContext extends ParserRuleContext {
		public BinaryExpressionContext binaryExpression() {
			return getRuleContext(BinaryExpressionContext.class,0);
		}
		public RegexExpressionContext regexExpression() {
			return getRuleContext(RegexExpressionContext.class,0);
		}
		public ContainsExpressionContext containsExpression() {
			return getRuleContext(ContainsExpressionContext.class,0);
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
			setState(84);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(80);
				binaryExpression(0);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(81);
				regexExpression();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(82);
				containsExpression();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(83);
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

	@SuppressWarnings("CheckReturnValue")
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
		public List<ContainsExpressionContext> containsExpression() {
			return getRuleContexts(ContainsExpressionContext.class);
		}
		public ContainsExpressionContext containsExpression(int i) {
			return getRuleContext(ContainsExpressionContext.class,i);
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
			setState(119);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,8,_ctx) ) {
			case 1:
				{
				setState(87);
				regexExpression();
				setState(88);
				match(LOGICAL_OPERATOR);
				setState(89);
				regexExpression();
				}
				break;
			case 2:
				{
				setState(91);
				regexExpression();
				setState(92);
				match(LOGICAL_OPERATOR);
				setState(93);
				binaryExpression(7);
				}
				break;
			case 3:
				{
				setState(95);
				regexExpression();
				setState(96);
				match(LOGICAL_OPERATOR);
				setState(97);
				containsExpression();
				}
				break;
			case 4:
				{
				setState(99);
				containsExpression();
				setState(100);
				match(LOGICAL_OPERATOR);
				setState(101);
				containsExpression();
				}
				break;
			case 5:
				{
				setState(103);
				containsExpression();
				setState(104);
				match(LOGICAL_OPERATOR);
				setState(105);
				binaryExpression(4);
				}
				break;
			case 6:
				{
				setState(107);
				containsExpression();
				setState(108);
				match(LOGICAL_OPERATOR);
				setState(109);
				regexExpression();
				}
				break;
			case 7:
				{
				setState(111);
				unaryExpression();
				setState(112);
				match(EQUALITY_OPERATOR);
				setState(113);
				literalExpression();
				}
				break;
			case 8:
				{
				setState(115);
				literalExpression();
				setState(116);
				match(EQUALITY_OPERATOR);
				setState(117);
				unaryExpression();
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(132);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,10,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(130);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,9,_ctx) ) {
					case 1:
						{
						_localctx = new BinaryExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_binaryExpression);
						setState(121);
						if (!(precpred(_ctx, 11))) throw new FailedPredicateException(this, "precpred(_ctx, 11)");
						setState(122);
						match(LOGICAL_OPERATOR);
						setState(123);
						binaryExpression(12);
						}
						break;
					case 2:
						{
						_localctx = new BinaryExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_binaryExpression);
						setState(124);
						if (!(precpred(_ctx, 10))) throw new FailedPredicateException(this, "precpred(_ctx, 10)");
						setState(125);
						match(LOGICAL_OPERATOR);
						setState(126);
						regexExpression();
						}
						break;
					case 3:
						{
						_localctx = new BinaryExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_binaryExpression);
						setState(127);
						if (!(precpred(_ctx, 9))) throw new FailedPredicateException(this, "precpred(_ctx, 9)");
						setState(128);
						match(LOGICAL_OPERATOR);
						setState(129);
						containsExpression();
						}
						break;
					}
					} 
				}
				setState(134);
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

	@SuppressWarnings("CheckReturnValue")
	public static class ContainsExpressionContext extends ParserRuleContext {
		public UnaryExpressionContext unaryExpression() {
			return getRuleContext(UnaryExpressionContext.class,0);
		}
		public TerminalNode CONTAINS() { return getToken(JsonPathParser.CONTAINS, 0); }
		public LiteralExpressionContext literalExpression() {
			return getRuleContext(LiteralExpressionContext.class,0);
		}
		public ContainsExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_containsExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).enterContainsExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathParserListener ) ((JsonPathParserListener)listener).exitContainsExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof JsonPathParserVisitor ) return ((JsonPathParserVisitor<? extends T>)visitor).visitContainsExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ContainsExpressionContext containsExpression() throws RecognitionException {
		ContainsExpressionContext _localctx = new ContainsExpressionContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_containsExpression);
		try {
			setState(143);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LBRACK:
			case AT:
			case DOT:
			case DOT_DOT:
			case ROOT:
				enterOuterAlt(_localctx, 1);
				{
				setState(135);
				unaryExpression();
				setState(136);
				match(CONTAINS);
				setState(137);
				literalExpression();
				}
				break;
			case StringLiteral:
			case PositiveNumber:
			case NegativeNumber:
			case NumericLiteral:
			case TRUE:
			case FALSE:
			case NULL:
				enterOuterAlt(_localctx, 2);
				{
				setState(139);
				literalExpression();
				setState(140);
				match(CONTAINS);
				setState(141);
				unaryExpression();
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
	public static class RegexExpressionContext extends ParserRuleContext {
		public UnaryExpressionContext unaryExpression() {
			return getRuleContext(UnaryExpressionContext.class,0);
		}
		public TerminalNode MATCHES_REGEX_OPEN() { return getToken(JsonPathParser.MATCHES_REGEX_OPEN, 0); }
		public TerminalNode REGEX() { return getToken(JsonPathParser.REGEX, 0); }
		public TerminalNode MATCHES_REGEX_CLOSE() { return getToken(JsonPathParser.MATCHES_REGEX_CLOSE, 0); }
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
		enterRule(_localctx, 18, RULE_regexExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(145);
			unaryExpression();
			setState(146);
			match(MATCHES_REGEX_OPEN);
			setState(147);
			match(REGEX);
			setState(148);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 20, RULE_unaryExpression);
		int _la;
		try {
			setState(162);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case AT:
				enterOuterAlt(_localctx, 1);
				{
				setState(150);
				match(AT);
				setState(159);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,13,_ctx) ) {
				case 1:
					{
					setState(151);
					match(DOT);
					setState(152);
					match(Identifier);
					}
					break;
				case 2:
					{
					setState(154);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==DOT) {
						{
						setState(153);
						match(DOT);
						}
					}

					setState(156);
					match(LBRACK);
					setState(157);
					match(StringLiteral);
					setState(158);
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
				setState(161);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 22, RULE_literalExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(164);
			_la = _input.LA(1);
			if ( !(((_la) & ~0x3f) == 0 && ((1L << _la) & 60137406464L) != 0) ) {
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
		enterRule(_localctx, 24, RULE_property);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(166);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 26, RULE_wildcard);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(168);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 28, RULE_slice);
		int _la;
		try {
			setState(180);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case PositiveNumber:
				enterOuterAlt(_localctx, 1);
				{
				setState(170);
				start();
				setState(171);
				match(COLON);
				setState(173);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==PositiveNumber) {
					{
					setState(172);
					end();
					}
				}

				}
				break;
			case COLON:
				enterOuterAlt(_localctx, 2);
				{
				setState(175);
				match(COLON);
				setState(176);
				match(PositiveNumber);
				}
				break;
			case NegativeNumber:
				enterOuterAlt(_localctx, 3);
				{
				setState(177);
				match(NegativeNumber);
				setState(178);
				match(COLON);
				}
				break;
			case WILDCARD:
				enterOuterAlt(_localctx, 4);
				{
				setState(179);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 30, RULE_start);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(182);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 32, RULE_end);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(184);
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

	@SuppressWarnings("CheckReturnValue")
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
		enterRule(_localctx, 34, RULE_indexes);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(187); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(186);
				match(PositiveNumber);
				}
				}
				setState(189); 
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
			return precpred(_ctx, 11);
		case 1:
			return precpred(_ctx, 10);
		case 2:
			return precpred(_ctx, 9);
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u0001&\u00c0\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002"+
		"\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b\u0002"+
		"\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007\u000f"+
		"\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0001\u0000\u0003\u0000"+
		"&\b\u0000\u0001\u0000\u0004\u0000)\b\u0000\u000b\u0000\f\u0000*\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0003\u00011\b\u0001\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0003\u00026\b\u0002\u0001\u0003\u0001"+
		"\u0003\u0001\u0003\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0004\u0004@\b\u0004\u000b\u0004\f\u0004A\u0003\u0004D\b\u0004"+
		"\u0001\u0004\u0001\u0004\u0001\u0005\u0001\u0005\u0001\u0005\u0004\u0005"+
		"K\b\u0005\u000b\u0005\f\u0005L\u0001\u0005\u0001\u0005\u0001\u0006\u0001"+
		"\u0006\u0001\u0006\u0001\u0006\u0003\u0006U\b\u0006\u0001\u0007\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001"+
		"\u0007\u0001\u0007\u0003\u0007x\b\u0007\u0001\u0007\u0001\u0007\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001"+
		"\u0007\u0005\u0007\u0083\b\u0007\n\u0007\f\u0007\u0086\t\u0007\u0001\b"+
		"\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0003\b\u0090"+
		"\b\b\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\n\u0001\n\u0001\n\u0001"+
		"\n\u0003\n\u009b\b\n\u0001\n\u0001\n\u0001\n\u0003\n\u00a0\b\n\u0001\n"+
		"\u0003\n\u00a3\b\n\u0001\u000b\u0001\u000b\u0001\f\u0001\f\u0001\r\u0001"+
		"\r\u0001\u000e\u0001\u000e\u0001\u000e\u0003\u000e\u00ae\b\u000e\u0001"+
		"\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0003\u000e\u00b5"+
		"\b\u000e\u0001\u000f\u0001\u000f\u0001\u0010\u0001\u0010\u0001\u0011\u0004"+
		"\u0011\u00bc\b\u0011\u000b\u0011\f\u0011\u00bd\u0001\u0011\u0000\u0001"+
		"\u000e\u0012\u0000\u0002\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016"+
		"\u0018\u001a\u001c\u001e \"\u0000\u0002\u0002\u0000\u0013\u0016!#\u0001"+
		"\u0000\u0012\u0013\u00cf\u0000%\u0001\u0000\u0000\u0000\u00020\u0001\u0000"+
		"\u0000\u0000\u00045\u0001\u0000\u0000\u0000\u00067\u0001\u0000\u0000\u0000"+
		"\b:\u0001\u0000\u0000\u0000\nG\u0001\u0000\u0000\u0000\fT\u0001\u0000"+
		"\u0000\u0000\u000ew\u0001\u0000\u0000\u0000\u0010\u008f\u0001\u0000\u0000"+
		"\u0000\u0012\u0091\u0001\u0000\u0000\u0000\u0014\u00a2\u0001\u0000\u0000"+
		"\u0000\u0016\u00a4\u0001\u0000\u0000\u0000\u0018\u00a6\u0001\u0000\u0000"+
		"\u0000\u001a\u00a8\u0001\u0000\u0000\u0000\u001c\u00b4\u0001\u0000\u0000"+
		"\u0000\u001e\u00b6\u0001\u0000\u0000\u0000 \u00b8\u0001\u0000\u0000\u0000"+
		"\"\u00bb\u0001\u0000\u0000\u0000$&\u0005\r\u0000\u0000%$\u0001\u0000\u0000"+
		"\u0000%&\u0001\u0000\u0000\u0000&(\u0001\u0000\u0000\u0000\')\u0003\u0002"+
		"\u0001\u0000(\'\u0001\u0000\u0000\u0000)*\u0001\u0000\u0000\u0000*(\u0001"+
		"\u0000\u0000\u0000*+\u0001\u0000\u0000\u0000+\u0001\u0001\u0000\u0000"+
		"\u0000,-\u0005\u000b\u0000\u0000-1\u0003\u0004\u0002\u0000.1\u0003\u0006"+
		"\u0003\u0000/1\u0003\b\u0004\u00000,\u0001\u0000\u0000\u00000.\u0001\u0000"+
		"\u0000\u00000/\u0001\u0000\u0000\u00001\u0003\u0001\u0000\u0000\u0000"+
		"26\u0003\b\u0004\u000036\u0003\u0018\f\u000046\u0003\u001a\r\u000052\u0001"+
		"\u0000\u0000\u000053\u0001\u0000\u0000\u000054\u0001\u0000\u0000\u0000"+
		"6\u0005\u0001\u0000\u0000\u000078\u0005\f\u0000\u000089\u0003\u0004\u0002"+
		"\u00009\u0007\u0001\u0000\u0000\u0000:C\u0005\u0006\u0000\u0000;D\u0003"+
		"\n\u0005\u0000<D\u0003\u001c\u000e\u0000=D\u0003\"\u0011\u0000>@\u0003"+
		"\u0018\f\u0000?>\u0001\u0000\u0000\u0000@A\u0001\u0000\u0000\u0000A?\u0001"+
		"\u0000\u0000\u0000AB\u0001\u0000\u0000\u0000BD\u0001\u0000\u0000\u0000"+
		"C;\u0001\u0000\u0000\u0000C<\u0001\u0000\u0000\u0000C=\u0001\u0000\u0000"+
		"\u0000C?\u0001\u0000\u0000\u0000DE\u0001\u0000\u0000\u0000EF\u0005\u0007"+
		"\u0000\u0000F\t\u0001\u0000\u0000\u0000GH\u0005\u0010\u0000\u0000HJ\u0005"+
		"\b\u0000\u0000IK\u0003\f\u0006\u0000JI\u0001\u0000\u0000\u0000KL\u0001"+
		"\u0000\u0000\u0000LJ\u0001\u0000\u0000\u0000LM\u0001\u0000\u0000\u0000"+
		"MN\u0001\u0000\u0000\u0000NO\u0005\t\u0000\u0000O\u000b\u0001\u0000\u0000"+
		"\u0000PU\u0003\u000e\u0007\u0000QU\u0003\u0012\t\u0000RU\u0003\u0010\b"+
		"\u0000SU\u0003\u0014\n\u0000TP\u0001\u0000\u0000\u0000TQ\u0001\u0000\u0000"+
		"\u0000TR\u0001\u0000\u0000\u0000TS\u0001\u0000\u0000\u0000U\r\u0001\u0000"+
		"\u0000\u0000VW\u0006\u0007\uffff\uffff\u0000WX\u0003\u0012\t\u0000XY\u0005"+
		"\u001b\u0000\u0000YZ\u0003\u0012\t\u0000Zx\u0001\u0000\u0000\u0000[\\"+
		"\u0003\u0012\t\u0000\\]\u0005\u001b\u0000\u0000]^\u0003\u000e\u0007\u0007"+
		"^x\u0001\u0000\u0000\u0000_`\u0003\u0012\t\u0000`a\u0005\u001b\u0000\u0000"+
		"ab\u0003\u0010\b\u0000bx\u0001\u0000\u0000\u0000cd\u0003\u0010\b\u0000"+
		"de\u0005\u001b\u0000\u0000ef\u0003\u0010\b\u0000fx\u0001\u0000\u0000\u0000"+
		"gh\u0003\u0010\b\u0000hi\u0005\u001b\u0000\u0000ij\u0003\u000e\u0007\u0004"+
		"jx\u0001\u0000\u0000\u0000kl\u0003\u0010\b\u0000lm\u0005\u001b\u0000\u0000"+
		"mn\u0003\u0012\t\u0000nx\u0001\u0000\u0000\u0000op\u0003\u0014\n\u0000"+
		"pq\u0005\u001e\u0000\u0000qr\u0003\u0016\u000b\u0000rx\u0001\u0000\u0000"+
		"\u0000st\u0003\u0016\u000b\u0000tu\u0005\u001e\u0000\u0000uv\u0003\u0014"+
		"\n\u0000vx\u0001\u0000\u0000\u0000wV\u0001\u0000\u0000\u0000w[\u0001\u0000"+
		"\u0000\u0000w_\u0001\u0000\u0000\u0000wc\u0001\u0000\u0000\u0000wg\u0001"+
		"\u0000\u0000\u0000wk\u0001\u0000\u0000\u0000wo\u0001\u0000\u0000\u0000"+
		"ws\u0001\u0000\u0000\u0000x\u0084\u0001\u0000\u0000\u0000yz\n\u000b\u0000"+
		"\u0000z{\u0005\u001b\u0000\u0000{\u0083\u0003\u000e\u0007\f|}\n\n\u0000"+
		"\u0000}~\u0005\u001b\u0000\u0000~\u0083\u0003\u0012\t\u0000\u007f\u0080"+
		"\n\t\u0000\u0000\u0080\u0081\u0005\u001b\u0000\u0000\u0081\u0083\u0003"+
		"\u0010\b\u0000\u0082y\u0001\u0000\u0000\u0000\u0082|\u0001\u0000\u0000"+
		"\u0000\u0082\u007f\u0001\u0000\u0000\u0000\u0083\u0086\u0001\u0000\u0000"+
		"\u0000\u0084\u0082\u0001\u0000\u0000\u0000\u0084\u0085\u0001\u0000\u0000"+
		"\u0000\u0085\u000f\u0001\u0000\u0000\u0000\u0086\u0084\u0001\u0000\u0000"+
		"\u0000\u0087\u0088\u0003\u0014\n\u0000\u0088\u0089\u0005\u0011\u0000\u0000"+
		"\u0089\u008a\u0003\u0016\u000b\u0000\u008a\u0090\u0001\u0000\u0000\u0000"+
		"\u008b\u008c\u0003\u0016\u000b\u0000\u008c\u008d\u0005\u0011\u0000\u0000"+
		"\u008d\u008e\u0003\u0014\n\u0000\u008e\u0090\u0001\u0000\u0000\u0000\u008f"+
		"\u0087\u0001\u0000\u0000\u0000\u008f\u008b\u0001\u0000\u0000\u0000\u0090"+
		"\u0011\u0001\u0000\u0000\u0000\u0091\u0092\u0003\u0014\n\u0000\u0092\u0093"+
		"\u0005\u0003\u0000\u0000\u0093\u0094\u0005&\u0000\u0000\u0094\u0095\u0005"+
		"$\u0000\u0000\u0095\u0013\u0001\u0000\u0000\u0000\u0096\u009f\u0005\n"+
		"\u0000\u0000\u0097\u0098\u0005\u000b\u0000\u0000\u0098\u00a0\u0005\u0012"+
		"\u0000\u0000\u0099\u009b\u0005\u000b\u0000\u0000\u009a\u0099\u0001\u0000"+
		"\u0000\u0000\u009a\u009b\u0001\u0000\u0000\u0000\u009b\u009c\u0001\u0000"+
		"\u0000\u0000\u009c\u009d\u0005\u0006\u0000\u0000\u009d\u009e\u0005\u0013"+
		"\u0000\u0000\u009e\u00a0\u0005\u0007\u0000\u0000\u009f\u0097\u0001\u0000"+
		"\u0000\u0000\u009f\u009a\u0001\u0000\u0000\u0000\u009f\u00a0\u0001\u0000"+
		"\u0000\u0000\u00a0\u00a3\u0001\u0000\u0000\u0000\u00a1\u00a3\u0003\u0000"+
		"\u0000\u0000\u00a2\u0096\u0001\u0000\u0000\u0000\u00a2\u00a1\u0001\u0000"+
		"\u0000\u0000\u00a3\u0015\u0001\u0000\u0000\u0000\u00a4\u00a5\u0007\u0000"+
		"\u0000\u0000\u00a5\u0017\u0001\u0000\u0000\u0000\u00a6\u00a7\u0007\u0001"+
		"\u0000\u0000\u00a7\u0019\u0001\u0000\u0000\u0000\u00a8\u00a9\u0005\u000e"+
		"\u0000\u0000\u00a9\u001b\u0001\u0000\u0000\u0000\u00aa\u00ab\u0003\u001e"+
		"\u000f\u0000\u00ab\u00ad\u0005\u000f\u0000\u0000\u00ac\u00ae\u0003 \u0010"+
		"\u0000\u00ad\u00ac\u0001\u0000\u0000\u0000\u00ad\u00ae\u0001\u0000\u0000"+
		"\u0000\u00ae\u00b5\u0001\u0000\u0000\u0000\u00af\u00b0\u0005\u000f\u0000"+
		"\u0000\u00b0\u00b5\u0005\u0014\u0000\u0000\u00b1\u00b2\u0005\u0015\u0000"+
		"\u0000\u00b2\u00b5\u0005\u000f\u0000\u0000\u00b3\u00b5\u0003\u001a\r\u0000"+
		"\u00b4\u00aa\u0001\u0000\u0000\u0000\u00b4\u00af\u0001\u0000\u0000\u0000"+
		"\u00b4\u00b1\u0001\u0000\u0000\u0000\u00b4\u00b3\u0001\u0000\u0000\u0000"+
		"\u00b5\u001d\u0001\u0000\u0000\u0000\u00b6\u00b7\u0005\u0014\u0000\u0000"+
		"\u00b7\u001f\u0001\u0000\u0000\u0000\u00b8\u00b9\u0005\u0014\u0000\u0000"+
		"\u00b9!\u0001\u0000\u0000\u0000\u00ba\u00bc\u0005\u0014\u0000\u0000\u00bb"+
		"\u00ba\u0001\u0000\u0000\u0000\u00bc\u00bd\u0001\u0000\u0000\u0000\u00bd"+
		"\u00bb\u0001\u0000\u0000\u0000\u00bd\u00be\u0001\u0000\u0000\u0000\u00be"+
		"#\u0001\u0000\u0000\u0000\u0012%*05ACLTw\u0082\u0084\u008f\u009a\u009f"+
		"\u00a2\u00ad\u00b4\u00bd";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}