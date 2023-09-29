/*
 * Copyright 2023 the original author or authors.
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
		LPAREN=1, RPAREN=2, DOT=3, COLON=4, COMMA=5, FullyQualifiedName=6, Number=7, 
		Identifier=8, S=9;
	public static final int
		RULE_matcherPattern = 0, RULE_typedPattern = 1, RULE_patternType = 2, 
		RULE_matcherParameter = 3, RULE_parameterName = 4, RULE_matcherName = 5;
	private static String[] makeRuleNames() {
		return new String[] {
			"matcherPattern", "typedPattern", "patternType", "matcherParameter", 
			"parameterName", "matcherName"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'('", "')'", "'.'", "':'", "','"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "LPAREN", "RPAREN", "DOT", "COLON", "COMMA", "FullyQualifiedName", 
			"Number", "Identifier", "S"
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
		public TypedPatternContext typedPattern() {
			return getRuleContext(TypedPatternContext.class,0);
		}
		public ParameterNameContext parameterName() {
			return getRuleContext(ParameterNameContext.class,0);
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
		try {
			setState(14);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(12);
				typedPattern();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(13);
				parameterName();
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
	public static class TypedPatternContext extends ParserRuleContext {
		public PatternTypeContext patternType() {
			return getRuleContext(PatternTypeContext.class,0);
		}
		public ParameterNameContext parameterName() {
			return getRuleContext(ParameterNameContext.class,0);
		}
		public TerminalNode COLON() { return getToken(TemplateParameterParser.COLON, 0); }
		public TypedPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typedPattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParameterParserListener ) ((TemplateParameterParserListener)listener).enterTypedPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParameterParserListener ) ((TemplateParameterParserListener)listener).exitTypedPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParameterParserVisitor ) return ((TemplateParameterParserVisitor<? extends T>)visitor).visitTypedPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypedPatternContext typedPattern() throws RecognitionException {
		TypedPatternContext _localctx = new TypedPatternContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_typedPattern);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(19);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
			case 1:
				{
				setState(16);
				parameterName();
				setState(17);
				match(COLON);
				}
				break;
			}
			setState(21);
			patternType();
			}
		}
		catch (RecognitionException re) {
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
	public static class PatternTypeContext extends ParserRuleContext {
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
		public List<TerminalNode> COMMA() { return getTokens(TemplateParameterParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(TemplateParameterParser.COMMA, i);
		}
		public PatternTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_patternType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParameterParserListener ) ((TemplateParameterParserListener)listener).enterPatternType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParameterParserListener ) ((TemplateParameterParserListener)listener).exitPatternType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParameterParserVisitor ) return ((TemplateParameterParserVisitor<? extends T>)visitor).visitPatternType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PatternTypeContext patternType() throws RecognitionException {
		PatternTypeContext _localctx = new PatternTypeContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_patternType);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(23);
			matcherName();
			setState(24);
			match(LPAREN);
			setState(34);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((_la) & ~0x3f) == 0 && ((1L << _la) & 448L) != 0) {
				{
				setState(30);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(25);
						matcherParameter();
						setState(26);
						match(COMMA);
						}
						} 
					}
					setState(32);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
				}
				setState(33);
				matcherParameter();
				}
			}

			setState(36);
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
		enterRule(_localctx, 6, RULE_matcherParameter);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(38);
			_la = _input.LA(1);
			if ( !(((_la) & ~0x3f) == 0 && ((1L << _la) & 448L) != 0) ) {
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
	public static class ParameterNameContext extends ParserRuleContext {
		public TerminalNode Identifier() { return getToken(TemplateParameterParser.Identifier, 0); }
		public ParameterNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_parameterName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParameterParserListener ) ((TemplateParameterParserListener)listener).enterParameterName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParameterParserListener ) ((TemplateParameterParserListener)listener).exitParameterName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParameterParserVisitor ) return ((TemplateParameterParserVisitor<? extends T>)visitor).visitParameterName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ParameterNameContext parameterName() throws RecognitionException {
		ParameterNameContext _localctx = new ParameterNameContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_parameterName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(40);
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
		enterRule(_localctx, 10, RULE_matcherName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(42);
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
		"\u0004\u0001\t-\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0001\u0000\u0001\u0000\u0003\u0000\u000f\b\u0000\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0003\u0001\u0014\b\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0005"+
		"\u0002\u001d\b\u0002\n\u0002\f\u0002 \t\u0002\u0001\u0002\u0003\u0002"+
		"#\b\u0002\u0001\u0002\u0001\u0002\u0001\u0003\u0001\u0003\u0001\u0004"+
		"\u0001\u0004\u0001\u0005\u0001\u0005\u0001\u0005\u0000\u0000\u0006\u0000"+
		"\u0002\u0004\u0006\b\n\u0000\u0001\u0001\u0000\u0006\b*\u0000\u000e\u0001"+
		"\u0000\u0000\u0000\u0002\u0013\u0001\u0000\u0000\u0000\u0004\u0017\u0001"+
		"\u0000\u0000\u0000\u0006&\u0001\u0000\u0000\u0000\b(\u0001\u0000\u0000"+
		"\u0000\n*\u0001\u0000\u0000\u0000\f\u000f\u0003\u0002\u0001\u0000\r\u000f"+
		"\u0003\b\u0004\u0000\u000e\f\u0001\u0000\u0000\u0000\u000e\r\u0001\u0000"+
		"\u0000\u0000\u000f\u0001\u0001\u0000\u0000\u0000\u0010\u0011\u0003\b\u0004"+
		"\u0000\u0011\u0012\u0005\u0004\u0000\u0000\u0012\u0014\u0001\u0000\u0000"+
		"\u0000\u0013\u0010\u0001\u0000\u0000\u0000\u0013\u0014\u0001\u0000\u0000"+
		"\u0000\u0014\u0015\u0001\u0000\u0000\u0000\u0015\u0016\u0003\u0004\u0002"+
		"\u0000\u0016\u0003\u0001\u0000\u0000\u0000\u0017\u0018\u0003\n\u0005\u0000"+
		"\u0018\"\u0005\u0001\u0000\u0000\u0019\u001a\u0003\u0006\u0003\u0000\u001a"+
		"\u001b\u0005\u0005\u0000\u0000\u001b\u001d\u0001\u0000\u0000\u0000\u001c"+
		"\u0019\u0001\u0000\u0000\u0000\u001d \u0001\u0000\u0000\u0000\u001e\u001c"+
		"\u0001\u0000\u0000\u0000\u001e\u001f\u0001\u0000\u0000\u0000\u001f!\u0001"+
		"\u0000\u0000\u0000 \u001e\u0001\u0000\u0000\u0000!#\u0003\u0006\u0003"+
		"\u0000\"\u001e\u0001\u0000\u0000\u0000\"#\u0001\u0000\u0000\u0000#$\u0001"+
		"\u0000\u0000\u0000$%\u0005\u0002\u0000\u0000%\u0005\u0001\u0000\u0000"+
		"\u0000&\'\u0007\u0000\u0000\u0000\'\u0007\u0001\u0000\u0000\u0000()\u0005"+
		"\b\u0000\u0000)\t\u0001\u0000\u0000\u0000*+\u0005\b\u0000\u0000+\u000b"+
		"\u0001\u0000\u0000\u0000\u0004\u000e\u0013\u001e\"";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}