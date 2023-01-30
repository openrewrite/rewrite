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
package org.openrewrite.java.internal.grammar;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class TemplateParameterParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.11.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		LPAREN=1, RPAREN=2, LBRACK=3, RBRACK=4, DOT=5, COMMA=6, SPACE=7, FullyQualifiedName=8, 
		Number=9, Identifier=10;
	public static final int
		RULE_matcherPattern = 0, RULE_matcherParameter = 1, RULE_matcherName = 2;
	private static String[] makeRuleNames() {
		return new String[] {
			"matcherPattern", "matcherParameter", "matcherName"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'('", "')'", "'['", "']'", "'.'", "','", "' '"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "LPAREN", "RPAREN", "LBRACK", "RBRACK", "DOT", "COMMA", "SPACE", 
			"FullyQualifiedName", "Number", "Identifier"
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

	public TemplateParameterParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MatcherPatternContext extends ParserRuleContext {
		public MatcherNameContext matcherName() {
			return getRuleContext(MatcherNameContext.class,0);
		}
		public TerminalNode LPAREN() { return getToken(TemplateParameterParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(TemplateParameterParser.RPAREN, 0); }
		public List<MatcherParameterContext> matcherParameter() {
			return getRuleContexts(MatcherParameterContext.class);
		}
		public MatcherParameterContext matcherParameter(int i) {
			return getRuleContext(MatcherParameterContext.class,i);
		}
		public MatcherPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_matcherPattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParameterParserListener ) ((TemplateParameterParserListener)listener).enterMatcherPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParameterParserListener ) ((TemplateParameterParserListener)listener).exitMatcherPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParameterParserVisitor ) return ((TemplateParameterParserVisitor<? extends T>)visitor).visitMatcherPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MatcherPatternContext matcherPattern() throws RecognitionException {
		MatcherPatternContext _localctx = new MatcherPatternContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_matcherPattern);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(6);
			matcherName();
			setState(7);
			match(LPAREN);
			setState(11);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((_la) & ~0x3f) == 0 && ((1L << _la) & 1792L) != 0) {
				{
				{
				setState(8);
				matcherParameter();
				}
				}
				setState(13);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(14);
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
	public static class MatcherParameterContext extends ParserRuleContext {
		public TerminalNode FullyQualifiedName() { return getToken(TemplateParameterParser.FullyQualifiedName, 0); }
		public TerminalNode Identifier() { return getToken(TemplateParameterParser.Identifier, 0); }
		public TerminalNode Number() { return getToken(TemplateParameterParser.Number, 0); }
		public MatcherParameterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_matcherParameter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParameterParserListener ) ((TemplateParameterParserListener)listener).enterMatcherParameter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParameterParserListener ) ((TemplateParameterParserListener)listener).exitMatcherParameter(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParameterParserVisitor ) return ((TemplateParameterParserVisitor<? extends T>)visitor).visitMatcherParameter(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MatcherParameterContext matcherParameter() throws RecognitionException {
		MatcherParameterContext _localctx = new MatcherParameterContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_matcherParameter);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(16);
			_la = _input.LA(1);
			if ( !(((_la) & ~0x3f) == 0 && ((1L << _la) & 1792L) != 0) ) {
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
	public static class MatcherNameContext extends ParserRuleContext {
		public TerminalNode Identifier() { return getToken(TemplateParameterParser.Identifier, 0); }
		public MatcherNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_matcherName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParameterParserListener ) ((TemplateParameterParserListener)listener).enterMatcherName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParameterParserListener ) ((TemplateParameterParserListener)listener).exitMatcherName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParameterParserVisitor ) return ((TemplateParameterParserVisitor<? extends T>)visitor).visitMatcherName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MatcherNameContext matcherName() throws RecognitionException {
		MatcherNameContext _localctx = new MatcherNameContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_matcherName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(18);
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

	public static final String _serializedATN =
		"\u0004\u0001\n\u0015\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0001\u0000\u0001\u0000\u0001\u0000\u0005\u0000\n\b"+
		"\u0000\n\u0000\f\u0000\r\t\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001"+
		"\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0000\u0000\u0003\u0000\u0002"+
		"\u0004\u0000\u0001\u0001\u0000\b\n\u0012\u0000\u0006\u0001\u0000\u0000"+
		"\u0000\u0002\u0010\u0001\u0000\u0000\u0000\u0004\u0012\u0001\u0000\u0000"+
		"\u0000\u0006\u0007\u0003\u0004\u0002\u0000\u0007\u000b\u0005\u0001\u0000"+
		"\u0000\b\n\u0003\u0002\u0001\u0000\t\b\u0001\u0000\u0000\u0000\n\r\u0001"+
		"\u0000\u0000\u0000\u000b\t\u0001\u0000\u0000\u0000\u000b\f\u0001\u0000"+
		"\u0000\u0000\f\u000e\u0001\u0000\u0000\u0000\r\u000b\u0001\u0000\u0000"+
		"\u0000\u000e\u000f\u0005\u0002\u0000\u0000\u000f\u0001\u0001\u0000\u0000"+
		"\u0000\u0010\u0011\u0007\u0000\u0000\u0000\u0011\u0003\u0001\u0000\u0000"+
		"\u0000\u0012\u0013\u0005\n\u0000\u0000\u0013\u0005\u0001\u0000\u0000\u0000"+
		"\u0001\u000b";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}