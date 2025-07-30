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
// Generated from ~/git/rewrite/rewrite-java/src/main/antlr/MethodSignatureParser.g4 by ANTLR 4.13.2
package org.openrewrite.java.internal.grammar;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.ParserATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class MethodSignatureParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		CONSTRUCTOR=1, LPAREN=2, RPAREN=3, LBRACK=4, RBRACK=5, COMMA=6, DOT=7,
		BANG=8, WILDCARD=9, AND=10, OR=11, ELLIPSIS=12, DOTDOT=13, POUND=14, SPACE=15,
		Identifier=16;
	public static final int
		RULE_methodPattern = 0, RULE_formalParametersPattern = 1, RULE_formalsPattern = 2,
		RULE_wildcard = 3, RULE_dotDot = 4, RULE_formalsPatternAfterDotDot = 5,
		RULE_optionalParensTypePattern = 6, RULE_targetTypePattern = 7, RULE_formalTypePattern = 8,
		RULE_classNameOrInterface = 9, RULE_simpleNamePattern = 10;
	private static String[] makeRuleNames() {
		return new String[] {
			"methodPattern", "formalParametersPattern", "formalsPattern", "wildcard",
			"dotDot", "formalsPatternAfterDotDot", "optionalParensTypePattern", "targetTypePattern",
			"formalTypePattern", "classNameOrInterface", "simpleNamePattern"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, "'('", "')'", "'['", "']'", "','", "'.'", "'!'", "'*'", "'&&'",
			"'||'", "'...'", "'..'", "'#'", "' '"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "CONSTRUCTOR", "LPAREN", "RPAREN", "LBRACK", "RBRACK", "COMMA",
			"DOT", "BANG", "WILDCARD", "AND", "OR", "ELLIPSIS", "DOTDOT", "POUND",
			"SPACE", "Identifier"
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
	public String getGrammarFileName() { return "MethodSignatureParser.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public MethodSignatureParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
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
		public TerminalNode DOT() { return getToken(MethodSignatureParser.DOT, 0); }
		public TerminalNode POUND() { return getToken(MethodSignatureParser.POUND, 0); }
		public List<TerminalNode> SPACE() { return getTokens(MethodSignatureParser.SPACE); }
		public TerminalNode SPACE(int i) {
			return getToken(MethodSignatureParser.SPACE, i);
		}
		public MethodPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_methodPattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MethodSignatureParserListener ) ((MethodSignatureParserListener)listener).enterMethodPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MethodSignatureParserListener ) ((MethodSignatureParserListener)listener).exitMethodPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof MethodSignatureParserVisitor) return ((MethodSignatureParserVisitor<? extends T>) visitor).visitMethodPattern(this);
            return visitor.visitChildren(this);
        }
	}

	public final MethodPatternContext methodPattern() throws RecognitionException {
		MethodPatternContext _localctx = new MethodPatternContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_methodPattern);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(22);
			targetTypePattern(0);
			setState(31);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case CONSTRUCTOR:
			case WILDCARD:
			case SPACE:
			case Identifier:
				{
				setState(26);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==SPACE) {
					{
					{
					setState(23);
					match(SPACE);
					}
					}
					setState(28);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case DOT:
				{
				setState(29);
				match(DOT);
				}
				break;
			case POUND:
				{
				setState(30);
				match(POUND);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(33);
			simpleNamePattern();
			setState(34);
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

	@SuppressWarnings("CheckReturnValue")
	public static class FormalParametersPatternContext extends ParserRuleContext {
		public TerminalNode LPAREN() { return getToken(MethodSignatureParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(MethodSignatureParser.RPAREN, 0); }
		public FormalsPatternContext formalsPattern() {
			return getRuleContext(FormalsPatternContext.class,0);
		}
		public FormalParametersPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_formalParametersPattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MethodSignatureParserListener ) ((MethodSignatureParserListener)listener).enterFormalParametersPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MethodSignatureParserListener ) ((MethodSignatureParserListener)listener).exitFormalParametersPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof MethodSignatureParserVisitor) return ((MethodSignatureParserVisitor<? extends T>) visitor).visitFormalParametersPattern(this);
            return visitor.visitChildren(this);
        }
	}

	public final FormalParametersPatternContext formalParametersPattern() throws RecognitionException {
		FormalParametersPatternContext _localctx = new FormalParametersPatternContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_formalParametersPattern);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(36);
			match(LPAREN);
			setState(38);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 74628L) != 0)) {
				{
				setState(37);
				formalsPattern();
				}
			}

			setState(40);
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
	public static class FormalsPatternContext extends ParserRuleContext {
		public DotDotContext dotDot() {
			return getRuleContext(DotDotContext.class,0);
		}
		public List<TerminalNode> COMMA() { return getTokens(MethodSignatureParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(MethodSignatureParser.COMMA, i);
		}
		public List<FormalsPatternAfterDotDotContext> formalsPatternAfterDotDot() {
			return getRuleContexts(FormalsPatternAfterDotDotContext.class);
		}
		public FormalsPatternAfterDotDotContext formalsPatternAfterDotDot(int i) {
			return getRuleContext(FormalsPatternAfterDotDotContext.class,i);
		}
		public List<TerminalNode> SPACE() { return getTokens(MethodSignatureParser.SPACE); }
		public TerminalNode SPACE(int i) {
			return getToken(MethodSignatureParser.SPACE, i);
		}
		public WildcardContext wildcard() {
			return getRuleContext(WildcardContext.class,0);
		}
		public List<FormalsPatternContext> formalsPattern() {
			return getRuleContexts(FormalsPatternContext.class);
		}
		public FormalsPatternContext formalsPattern(int i) {
			return getRuleContext(FormalsPatternContext.class,i);
		}
		public OptionalParensTypePatternContext optionalParensTypePattern() {
			return getRuleContext(OptionalParensTypePatternContext.class,0);
		}
		public FormalTypePatternContext formalTypePattern() {
			return getRuleContext(FormalTypePatternContext.class,0);
		}
		public TerminalNode ELLIPSIS() { return getToken(MethodSignatureParser.ELLIPSIS, 0); }
		public FormalsPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_formalsPattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MethodSignatureParserListener ) ((MethodSignatureParserListener)listener).enterFormalsPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MethodSignatureParserListener ) ((MethodSignatureParserListener)listener).exitFormalsPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof MethodSignatureParserVisitor) return ((MethodSignatureParserVisitor<? extends T>) visitor).visitFormalsPattern(this);
            return visitor.visitChildren(this);
        }
	}

	public final FormalsPatternContext formalsPattern() throws RecognitionException {
		FormalsPatternContext _localctx = new FormalsPatternContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_formalsPattern);
		int _la;
		try {
			int _alt;
			setState(87);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,9,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(42);
				dotDot();
				setState(53);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,4,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(43);
						match(COMMA);
						setState(47);
						_errHandler.sync(this);
						_la = _input.LA(1);
						while (_la==SPACE) {
							{
							{
							setState(44);
							match(SPACE);
							}
							}
							setState(49);
							_errHandler.sync(this);
							_la = _input.LA(1);
						}
						setState(50);
						formalsPatternAfterDotDot();
						}
						}
					}
					setState(55);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,4,_ctx);
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(56);
				wildcard();
				setState(67);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,6,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(57);
						match(COMMA);
						setState(61);
						_errHandler.sync(this);
						_la = _input.LA(1);
						while (_la==SPACE) {
							{
							{
							setState(58);
							match(SPACE);
							}
							}
							setState(63);
							_errHandler.sync(this);
							_la = _input.LA(1);
						}
						setState(64);
						formalsPattern();
						}
						}
					}
					setState(69);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,6,_ctx);
				}
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(70);
				optionalParensTypePattern();
				setState(81);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,8,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(71);
						match(COMMA);
						setState(75);
						_errHandler.sync(this);
						_la = _input.LA(1);
						while (_la==SPACE) {
							{
							{
							setState(72);
							match(SPACE);
							}
							}
							setState(77);
							_errHandler.sync(this);
							_la = _input.LA(1);
						}
						setState(78);
						formalsPattern();
						}
						}
					}
					setState(83);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,8,_ctx);
				}
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(84);
				formalTypePattern(0);
				setState(85);
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

	@SuppressWarnings("CheckReturnValue")
	public static class WildcardContext extends ParserRuleContext {
		public TerminalNode WILDCARD() { return getToken(MethodSignatureParser.WILDCARD, 0); }
		public WildcardContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_wildcard; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MethodSignatureParserListener ) ((MethodSignatureParserListener)listener).enterWildcard(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MethodSignatureParserListener ) ((MethodSignatureParserListener)listener).exitWildcard(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof MethodSignatureParserVisitor) return ((MethodSignatureParserVisitor<? extends T>) visitor).visitWildcard(this);
            return visitor.visitChildren(this);
        }
	}

	public final WildcardContext wildcard() throws RecognitionException {
		WildcardContext _localctx = new WildcardContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_wildcard);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(89);
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
	public static class DotDotContext extends ParserRuleContext {
		public TerminalNode DOTDOT() { return getToken(MethodSignatureParser.DOTDOT, 0); }
		public DotDotContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dotDot; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MethodSignatureParserListener ) ((MethodSignatureParserListener)listener).enterDotDot(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MethodSignatureParserListener ) ((MethodSignatureParserListener)listener).exitDotDot(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof MethodSignatureParserVisitor) return ((MethodSignatureParserVisitor<? extends T>) visitor).visitDotDot(this);
            return visitor.visitChildren(this);
        }
	}

	public final DotDotContext dotDot() throws RecognitionException {
		DotDotContext _localctx = new DotDotContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_dotDot);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(91);
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

	@SuppressWarnings("CheckReturnValue")
	public static class FormalsPatternAfterDotDotContext extends ParserRuleContext {
		public OptionalParensTypePatternContext optionalParensTypePattern() {
			return getRuleContext(OptionalParensTypePatternContext.class,0);
		}
		public List<TerminalNode> COMMA() { return getTokens(MethodSignatureParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(MethodSignatureParser.COMMA, i);
		}
		public List<FormalsPatternAfterDotDotContext> formalsPatternAfterDotDot() {
			return getRuleContexts(FormalsPatternAfterDotDotContext.class);
		}
		public FormalsPatternAfterDotDotContext formalsPatternAfterDotDot(int i) {
			return getRuleContext(FormalsPatternAfterDotDotContext.class,i);
		}
		public List<TerminalNode> SPACE() { return getTokens(MethodSignatureParser.SPACE); }
		public TerminalNode SPACE(int i) {
			return getToken(MethodSignatureParser.SPACE, i);
		}
		public FormalTypePatternContext formalTypePattern() {
			return getRuleContext(FormalTypePatternContext.class,0);
		}
		public TerminalNode ELLIPSIS() { return getToken(MethodSignatureParser.ELLIPSIS, 0); }
		public FormalsPatternAfterDotDotContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_formalsPatternAfterDotDot; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MethodSignatureParserListener ) ((MethodSignatureParserListener)listener).enterFormalsPatternAfterDotDot(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MethodSignatureParserListener ) ((MethodSignatureParserListener)listener).exitFormalsPatternAfterDotDot(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof MethodSignatureParserVisitor) return ((MethodSignatureParserVisitor<? extends T>) visitor).visitFormalsPatternAfterDotDot(this);
            return visitor.visitChildren(this);
        }
	}

	public final FormalsPatternAfterDotDotContext formalsPatternAfterDotDot() throws RecognitionException {
		FormalsPatternAfterDotDotContext _localctx = new FormalsPatternAfterDotDotContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_formalsPatternAfterDotDot);
		int _la;
		try {
			int _alt;
			setState(110);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,12,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(93);
				optionalParensTypePattern();
				setState(104);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,11,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(94);
						match(COMMA);
						setState(98);
						_errHandler.sync(this);
						_la = _input.LA(1);
						while (_la==SPACE) {
							{
							{
							setState(95);
							match(SPACE);
							}
							}
							setState(100);
							_errHandler.sync(this);
							_la = _input.LA(1);
						}
						setState(101);
						formalsPatternAfterDotDot();
						}
						}
					}
					setState(106);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,11,_ctx);
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(107);
				formalTypePattern(0);
				setState(108);
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

	@SuppressWarnings("CheckReturnValue")
	public static class OptionalParensTypePatternContext extends ParserRuleContext {
		public TerminalNode LPAREN() { return getToken(MethodSignatureParser.LPAREN, 0); }
		public FormalTypePatternContext formalTypePattern() {
			return getRuleContext(FormalTypePatternContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(MethodSignatureParser.RPAREN, 0); }
		public OptionalParensTypePatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_optionalParensTypePattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MethodSignatureParserListener ) ((MethodSignatureParserListener)listener).enterOptionalParensTypePattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MethodSignatureParserListener ) ((MethodSignatureParserListener)listener).exitOptionalParensTypePattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof MethodSignatureParserVisitor) return ((MethodSignatureParserVisitor<? extends T>) visitor).visitOptionalParensTypePattern(this);
            return visitor.visitChildren(this);
        }
	}

	public final OptionalParensTypePatternContext optionalParensTypePattern() throws RecognitionException {
		OptionalParensTypePatternContext _localctx = new OptionalParensTypePatternContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_optionalParensTypePattern);
		try {
			setState(117);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LPAREN:
				enterOuterAlt(_localctx, 1);
				{
				setState(112);
				match(LPAREN);
				setState(113);
				formalTypePattern(0);
				setState(114);
				match(RPAREN);
				}
				break;
			case DOT:
			case BANG:
			case WILDCARD:
			case DOTDOT:
			case Identifier:
				enterOuterAlt(_localctx, 2);
				{
				setState(116);
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

	@SuppressWarnings("CheckReturnValue")
	public static class TargetTypePatternContext extends ParserRuleContext {
		public ClassNameOrInterfaceContext classNameOrInterface() {
			return getRuleContext(ClassNameOrInterfaceContext.class,0);
		}
		public TerminalNode BANG() { return getToken(MethodSignatureParser.BANG, 0); }
		public List<TargetTypePatternContext> targetTypePattern() {
			return getRuleContexts(TargetTypePatternContext.class);
		}
		public TargetTypePatternContext targetTypePattern(int i) {
			return getRuleContext(TargetTypePatternContext.class,i);
		}
		public TerminalNode AND() { return getToken(MethodSignatureParser.AND, 0); }
		public TerminalNode OR() { return getToken(MethodSignatureParser.OR, 0); }
		public TargetTypePatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_targetTypePattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MethodSignatureParserListener ) ((MethodSignatureParserListener)listener).enterTargetTypePattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MethodSignatureParserListener ) ((MethodSignatureParserListener)listener).exitTargetTypePattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof MethodSignatureParserVisitor) return ((MethodSignatureParserVisitor<? extends T>) visitor).visitTargetTypePattern(this);
            return visitor.visitChildren(this);
        }
	}

	public final TargetTypePatternContext targetTypePattern() throws RecognitionException {
		return targetTypePattern(0);
	}

	private TargetTypePatternContext targetTypePattern(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		TargetTypePatternContext _localctx = new TargetTypePatternContext(_ctx, _parentState);
		int _startState = 14;
		enterRecursionRule(_localctx, 14, RULE_targetTypePattern, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(123);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DOT:
			case WILDCARD:
			case DOTDOT:
			case Identifier:
				{
				setState(120);
				classNameOrInterface();
				}
				break;
			case BANG:
				{
				setState(121);
				match(BANG);
				setState(122);
				targetTypePattern(3);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(133);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,16,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					{
					setState(131);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,15,_ctx) ) {
					case 1:
						{
						_localctx = new TargetTypePatternContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_targetTypePattern);
						setState(125);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(126);
						match(AND);
						setState(127);
						targetTypePattern(3);
						}
						break;
					case 2:
						{
						_localctx = new TargetTypePatternContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_targetTypePattern);
						setState(128);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(129);
						match(OR);
						setState(130);
						targetTypePattern(2);
						}
						break;
					}
					}
				}
				setState(135);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,16,_ctx);
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
	public static class FormalTypePatternContext extends ParserRuleContext {
		public ClassNameOrInterfaceContext classNameOrInterface() {
			return getRuleContext(ClassNameOrInterfaceContext.class,0);
		}
		public TerminalNode BANG() { return getToken(MethodSignatureParser.BANG, 0); }
		public List<FormalTypePatternContext> formalTypePattern() {
			return getRuleContexts(FormalTypePatternContext.class);
		}
		public FormalTypePatternContext formalTypePattern(int i) {
			return getRuleContext(FormalTypePatternContext.class,i);
		}
		public TerminalNode AND() { return getToken(MethodSignatureParser.AND, 0); }
		public TerminalNode OR() { return getToken(MethodSignatureParser.OR, 0); }
		public FormalTypePatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_formalTypePattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MethodSignatureParserListener ) ((MethodSignatureParserListener)listener).enterFormalTypePattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MethodSignatureParserListener ) ((MethodSignatureParserListener)listener).exitFormalTypePattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof MethodSignatureParserVisitor) return ((MethodSignatureParserVisitor<? extends T>) visitor).visitFormalTypePattern(this);
            return visitor.visitChildren(this);
        }
	}

	public final FormalTypePatternContext formalTypePattern() throws RecognitionException {
		return formalTypePattern(0);
	}

	private FormalTypePatternContext formalTypePattern(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		FormalTypePatternContext _localctx = new FormalTypePatternContext(_ctx, _parentState);
		int _startState = 16;
		enterRecursionRule(_localctx, 16, RULE_formalTypePattern, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(140);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DOT:
			case WILDCARD:
			case DOTDOT:
			case Identifier:
				{
				setState(137);
				classNameOrInterface();
				}
				break;
			case BANG:
				{
				setState(138);
				match(BANG);
				setState(139);
				formalTypePattern(3);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(150);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,19,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					{
					setState(148);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,18,_ctx) ) {
					case 1:
						{
						_localctx = new FormalTypePatternContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_formalTypePattern);
						setState(142);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(143);
						match(AND);
						setState(144);
						formalTypePattern(3);
						}
						break;
					case 2:
						{
						_localctx = new FormalTypePatternContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_formalTypePattern);
						setState(145);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(146);
						match(OR);
						setState(147);
						formalTypePattern(2);
						}
						break;
					}
					}
				}
				setState(152);
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
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ClassNameOrInterfaceContext extends ParserRuleContext {
		public List<TerminalNode> LBRACK() { return getTokens(MethodSignatureParser.LBRACK); }
		public TerminalNode LBRACK(int i) {
			return getToken(MethodSignatureParser.LBRACK, i);
		}
		public List<TerminalNode> RBRACK() { return getTokens(MethodSignatureParser.RBRACK); }
		public TerminalNode RBRACK(int i) {
			return getToken(MethodSignatureParser.RBRACK, i);
		}
		public List<TerminalNode> Identifier() { return getTokens(MethodSignatureParser.Identifier); }
		public TerminalNode Identifier(int i) {
			return getToken(MethodSignatureParser.Identifier, i);
		}
		public List<TerminalNode> WILDCARD() { return getTokens(MethodSignatureParser.WILDCARD); }
		public TerminalNode WILDCARD(int i) {
			return getToken(MethodSignatureParser.WILDCARD, i);
		}
		public List<TerminalNode> DOT() { return getTokens(MethodSignatureParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(MethodSignatureParser.DOT, i);
		}
		public List<TerminalNode> DOTDOT() { return getTokens(MethodSignatureParser.DOTDOT); }
		public TerminalNode DOTDOT(int i) {
			return getToken(MethodSignatureParser.DOTDOT, i);
		}
		public ClassNameOrInterfaceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_classNameOrInterface; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MethodSignatureParserListener ) ((MethodSignatureParserListener)listener).enterClassNameOrInterface(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MethodSignatureParserListener ) ((MethodSignatureParserListener)listener).exitClassNameOrInterface(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof MethodSignatureParserVisitor) return ((MethodSignatureParserVisitor<? extends T>) visitor).visitClassNameOrInterface(this);
            return visitor.visitChildren(this);
        }
	}

	public final ClassNameOrInterfaceContext classNameOrInterface() throws RecognitionException {
		ClassNameOrInterfaceContext _localctx = new ClassNameOrInterfaceContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_classNameOrInterface);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(154);
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(153);
					_la = _input.LA(1);
					if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 74368L) != 0)) ) {
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
				setState(156);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,20,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
			setState(162);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,21,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(158);
					match(LBRACK);
					setState(159);
					match(RBRACK);
					}
					}
				}
				setState(164);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,21,_ctx);
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
	public static class SimpleNamePatternContext extends ParserRuleContext {
		public List<TerminalNode> Identifier() { return getTokens(MethodSignatureParser.Identifier); }
		public TerminalNode Identifier(int i) {
			return getToken(MethodSignatureParser.Identifier, i);
		}
		public List<TerminalNode> WILDCARD() { return getTokens(MethodSignatureParser.WILDCARD); }
		public TerminalNode WILDCARD(int i) {
			return getToken(MethodSignatureParser.WILDCARD, i);
		}
		public TerminalNode CONSTRUCTOR() { return getToken(MethodSignatureParser.CONSTRUCTOR, 0); }
		public SimpleNamePatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_simpleNamePattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MethodSignatureParserListener ) ((MethodSignatureParserListener)listener).enterSimpleNamePattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MethodSignatureParserListener ) ((MethodSignatureParserListener)listener).exitSimpleNamePattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof MethodSignatureParserVisitor) return ((MethodSignatureParserVisitor<? extends T>) visitor).visitSimpleNamePattern(this);
            return visitor.visitChildren(this);
        }
	}

	public final SimpleNamePatternContext simpleNamePattern() throws RecognitionException {
		SimpleNamePatternContext _localctx = new SimpleNamePatternContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_simpleNamePattern);
		int _la;
		try {
			int _alt;
			setState(188);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(165);
				match(Identifier);
				setState(170);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,22,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(166);
						match(WILDCARD);
						setState(167);
						match(Identifier);
						}
						}
					}
					setState(172);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,22,_ctx);
				}
				setState(174);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WILDCARD) {
					{
					setState(173);
					match(WILDCARD);
					}
				}

				}
				break;
			case WILDCARD:
				enterOuterAlt(_localctx, 2);
				{
				setState(176);
				match(WILDCARD);
				setState(181);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,24,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(177);
						match(Identifier);
						setState(178);
						match(WILDCARD);
						}
						}
					}
					setState(183);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,24,_ctx);
				}
				setState(185);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Identifier) {
					{
					setState(184);
					match(Identifier);
					}
				}

				}
				break;
			case CONSTRUCTOR:
				enterOuterAlt(_localctx, 3);
				{
				setState(187);
				match(CONSTRUCTOR);
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

    @Override
    public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 7:
			return targetTypePattern_sempred((TargetTypePatternContext)_localctx, predIndex);
		case 8:
			return formalTypePattern_sempred((FormalTypePatternContext)_localctx, predIndex);
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

	public static final String _serializedATN =
		"\u0004\u0001\u0010\u00bf\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001"+
		"\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004"+
		"\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007"+
		"\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0001\u0000\u0001\u0000"+
		"\u0005\u0000\u0019\b\u0000\n\u0000\f\u0000\u001c\t\u0000\u0001\u0000\u0001"+
		"\u0000\u0003\u0000 \b\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001"+
		"\u0001\u0001\u0001\u0003\u0001\'\b\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0005\u0002.\b\u0002\n\u0002\f\u00021\t"+
		"\u0002\u0001\u0002\u0005\u00024\b\u0002\n\u0002\f\u00027\t\u0002\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0005\u0002<\b\u0002\n\u0002\f\u0002?\t"+
		"\u0002\u0001\u0002\u0005\u0002B\b\u0002\n\u0002\f\u0002E\t\u0002\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0005\u0002J\b\u0002\n\u0002\f\u0002M\t"+
		"\u0002\u0001\u0002\u0005\u0002P\b\u0002\n\u0002\f\u0002S\t\u0002\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0003\u0002X\b\u0002\u0001\u0003\u0001"+
		"\u0003\u0001\u0004\u0001\u0004\u0001\u0005\u0001\u0005\u0001\u0005\u0005"+
		"\u0005a\b\u0005\n\u0005\f\u0005d\t\u0005\u0001\u0005\u0005\u0005g\b\u0005"+
		"\n\u0005\f\u0005j\t\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0003\u0005"+
		"o\b\u0005\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006"+
		"\u0003\u0006v\b\u0006\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007"+
		"\u0003\u0007|\b\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007"+
		"\u0001\u0007\u0001\u0007\u0005\u0007\u0084\b\u0007\n\u0007\f\u0007\u0087"+
		"\t\u0007\u0001\b\u0001\b\u0001\b\u0001\b\u0003\b\u008d\b\b\u0001\b\u0001"+
		"\b\u0001\b\u0001\b\u0001\b\u0001\b\u0005\b\u0095\b\b\n\b\f\b\u0098\t\b"+
		"\u0001\t\u0004\t\u009b\b\t\u000b\t\f\t\u009c\u0001\t\u0001\t\u0005\t\u00a1"+
		"\b\t\n\t\f\t\u00a4\t\t\u0001\n\u0001\n\u0001\n\u0005\n\u00a9\b\n\n\n\f"+
		"\n\u00ac\t\n\u0001\n\u0003\n\u00af\b\n\u0001\n\u0001\n\u0001\n\u0005\n"+
		"\u00b4\b\n\n\n\f\n\u00b7\t\n\u0001\n\u0003\n\u00ba\b\n\u0001\n\u0003\n"+
		"\u00bd\b\n\u0001\n\u0000\u0002\u000e\u0010\u000b\u0000\u0002\u0004\u0006"+
		"\b\n\f\u000e\u0010\u0012\u0014\u0000\u0001\u0004\u0000\u0007\u0007\t\t"+
		"\r\r\u0010\u0010\u00d2\u0000\u0016\u0001\u0000\u0000\u0000\u0002$\u0001"+
		"\u0000\u0000\u0000\u0004W\u0001\u0000\u0000\u0000\u0006Y\u0001\u0000\u0000"+
		"\u0000\b[\u0001\u0000\u0000\u0000\nn\u0001\u0000\u0000\u0000\fu\u0001"+
		"\u0000\u0000\u0000\u000e{\u0001\u0000\u0000\u0000\u0010\u008c\u0001\u0000"+
		"\u0000\u0000\u0012\u009a\u0001\u0000\u0000\u0000\u0014\u00bc\u0001\u0000"+
		"\u0000\u0000\u0016\u001f\u0003\u000e\u0007\u0000\u0017\u0019\u0005\u000f"+
		"\u0000\u0000\u0018\u0017\u0001\u0000\u0000\u0000\u0019\u001c\u0001\u0000"+
		"\u0000\u0000\u001a\u0018\u0001\u0000\u0000\u0000\u001a\u001b\u0001\u0000"+
		"\u0000\u0000\u001b \u0001\u0000\u0000\u0000\u001c\u001a\u0001\u0000\u0000"+
		"\u0000\u001d \u0005\u0007\u0000\u0000\u001e \u0005\u000e\u0000\u0000\u001f"+
		"\u001a\u0001\u0000\u0000\u0000\u001f\u001d\u0001\u0000\u0000\u0000\u001f"+
		"\u001e\u0001\u0000\u0000\u0000 !\u0001\u0000\u0000\u0000!\"\u0003\u0014"+
		"\n\u0000\"#\u0003\u0002\u0001\u0000#\u0001\u0001\u0000\u0000\u0000$&\u0005"+
		"\u0002\u0000\u0000%\'\u0003\u0004\u0002\u0000&%\u0001\u0000\u0000\u0000"+
		"&\'\u0001\u0000\u0000\u0000\'(\u0001\u0000\u0000\u0000()\u0005\u0003\u0000"+
		"\u0000)\u0003\u0001\u0000\u0000\u0000*5\u0003\b\u0004\u0000+/\u0005\u0006"+
		"\u0000\u0000,.\u0005\u000f\u0000\u0000-,\u0001\u0000\u0000\u0000.1\u0001"+
		"\u0000\u0000\u0000/-\u0001\u0000\u0000\u0000/0\u0001\u0000\u0000\u0000"+
		"02\u0001\u0000\u0000\u00001/\u0001\u0000\u0000\u000024\u0003\n\u0005\u0000"+
		"3+\u0001\u0000\u0000\u000047\u0001\u0000\u0000\u000053\u0001\u0000\u0000"+
		"\u000056\u0001\u0000\u0000\u00006X\u0001\u0000\u0000\u000075\u0001\u0000"+
		"\u0000\u00008C\u0003\u0006\u0003\u00009=\u0005\u0006\u0000\u0000:<\u0005"+
		"\u000f\u0000\u0000;:\u0001\u0000\u0000\u0000<?\u0001\u0000\u0000\u0000"+
		"=;\u0001\u0000\u0000\u0000=>\u0001\u0000\u0000\u0000>@\u0001\u0000\u0000"+
		"\u0000?=\u0001\u0000\u0000\u0000@B\u0003\u0004\u0002\u0000A9\u0001\u0000"+
		"\u0000\u0000BE\u0001\u0000\u0000\u0000CA\u0001\u0000\u0000\u0000CD\u0001"+
		"\u0000\u0000\u0000DX\u0001\u0000\u0000\u0000EC\u0001\u0000\u0000\u0000"+
		"FQ\u0003\f\u0006\u0000GK\u0005\u0006\u0000\u0000HJ\u0005\u000f\u0000\u0000"+
		"IH\u0001\u0000\u0000\u0000JM\u0001\u0000\u0000\u0000KI\u0001\u0000\u0000"+
		"\u0000KL\u0001\u0000\u0000\u0000LN\u0001\u0000\u0000\u0000MK\u0001\u0000"+
		"\u0000\u0000NP\u0003\u0004\u0002\u0000OG\u0001\u0000\u0000\u0000PS\u0001"+
		"\u0000\u0000\u0000QO\u0001\u0000\u0000\u0000QR\u0001\u0000\u0000\u0000"+
		"RX\u0001\u0000\u0000\u0000SQ\u0001\u0000\u0000\u0000TU\u0003\u0010\b\u0000"+
		"UV\u0005\f\u0000\u0000VX\u0001\u0000\u0000\u0000W*\u0001\u0000\u0000\u0000"+
		"W8\u0001\u0000\u0000\u0000WF\u0001\u0000\u0000\u0000WT\u0001\u0000\u0000"+
		"\u0000X\u0005\u0001\u0000\u0000\u0000YZ\u0005\t\u0000\u0000Z\u0007\u0001"+
		"\u0000\u0000\u0000[\\\u0005\r\u0000\u0000\\\t\u0001\u0000\u0000\u0000"+
		"]h\u0003\f\u0006\u0000^b\u0005\u0006\u0000\u0000_a\u0005\u000f\u0000\u0000"+
		"`_\u0001\u0000\u0000\u0000ad\u0001\u0000\u0000\u0000b`\u0001\u0000\u0000"+
		"\u0000bc\u0001\u0000\u0000\u0000ce\u0001\u0000\u0000\u0000db\u0001\u0000"+
		"\u0000\u0000eg\u0003\n\u0005\u0000f^\u0001\u0000\u0000\u0000gj\u0001\u0000"+
		"\u0000\u0000hf\u0001\u0000\u0000\u0000hi\u0001\u0000\u0000\u0000io\u0001"+
		"\u0000\u0000\u0000jh\u0001\u0000\u0000\u0000kl\u0003\u0010\b\u0000lm\u0005"+
		"\f\u0000\u0000mo\u0001\u0000\u0000\u0000n]\u0001\u0000\u0000\u0000nk\u0001"+
		"\u0000\u0000\u0000o\u000b\u0001\u0000\u0000\u0000pq\u0005\u0002\u0000"+
		"\u0000qr\u0003\u0010\b\u0000rs\u0005\u0003\u0000\u0000sv\u0001\u0000\u0000"+
		"\u0000tv\u0003\u0010\b\u0000up\u0001\u0000\u0000\u0000ut\u0001\u0000\u0000"+
		"\u0000v\r\u0001\u0000\u0000\u0000wx\u0006\u0007\uffff\uffff\u0000x|\u0003"+
		"\u0012\t\u0000yz\u0005\b\u0000\u0000z|\u0003\u000e\u0007\u0003{w\u0001"+
		"\u0000\u0000\u0000{y\u0001\u0000\u0000\u0000|\u0085\u0001\u0000\u0000"+
		"\u0000}~\n\u0002\u0000\u0000~\u007f\u0005\n\u0000\u0000\u007f\u0084\u0003"+
		"\u000e\u0007\u0003\u0080\u0081\n\u0001\u0000\u0000\u0081\u0082\u0005\u000b"+
		"\u0000\u0000\u0082\u0084\u0003\u000e\u0007\u0002\u0083}\u0001\u0000\u0000"+
		"\u0000\u0083\u0080\u0001\u0000\u0000\u0000\u0084\u0087\u0001\u0000\u0000"+
		"\u0000\u0085\u0083\u0001\u0000\u0000\u0000\u0085\u0086\u0001\u0000\u0000"+
		"\u0000\u0086\u000f\u0001\u0000\u0000\u0000\u0087\u0085\u0001\u0000\u0000"+
		"\u0000\u0088\u0089\u0006\b\uffff\uffff\u0000\u0089\u008d\u0003\u0012\t"+
		"\u0000\u008a\u008b\u0005\b\u0000\u0000\u008b\u008d\u0003\u0010\b\u0003"+
		"\u008c\u0088\u0001\u0000\u0000\u0000\u008c\u008a\u0001\u0000\u0000\u0000"+
		"\u008d\u0096\u0001\u0000\u0000\u0000\u008e\u008f\n\u0002\u0000\u0000\u008f"+
		"\u0090\u0005\n\u0000\u0000\u0090\u0095\u0003\u0010\b\u0003\u0091\u0092"+
		"\n\u0001\u0000\u0000\u0092\u0093\u0005\u000b\u0000\u0000\u0093\u0095\u0003"+
		"\u0010\b\u0002\u0094\u008e\u0001\u0000\u0000\u0000\u0094\u0091\u0001\u0000"+
		"\u0000\u0000\u0095\u0098\u0001\u0000\u0000\u0000\u0096\u0094\u0001\u0000"+
		"\u0000\u0000\u0096\u0097\u0001\u0000\u0000\u0000\u0097\u0011\u0001\u0000"+
		"\u0000\u0000\u0098\u0096\u0001\u0000\u0000\u0000\u0099\u009b\u0007\u0000"+
		"\u0000\u0000\u009a\u0099\u0001\u0000\u0000\u0000\u009b\u009c\u0001\u0000"+
		"\u0000\u0000\u009c\u009a\u0001\u0000\u0000\u0000\u009c\u009d\u0001\u0000"+
		"\u0000\u0000\u009d\u00a2\u0001\u0000\u0000\u0000\u009e\u009f\u0005\u0004"+
		"\u0000\u0000\u009f\u00a1\u0005\u0005\u0000\u0000\u00a0\u009e\u0001\u0000"+
		"\u0000\u0000\u00a1\u00a4\u0001\u0000\u0000\u0000\u00a2\u00a0\u0001\u0000"+
		"\u0000\u0000\u00a2\u00a3\u0001\u0000\u0000\u0000\u00a3\u0013\u0001\u0000"+
		"\u0000\u0000\u00a4\u00a2\u0001\u0000\u0000\u0000\u00a5\u00aa\u0005\u0010"+
		"\u0000\u0000\u00a6\u00a7\u0005\t\u0000\u0000\u00a7\u00a9\u0005\u0010\u0000"+
		"\u0000\u00a8\u00a6\u0001\u0000\u0000\u0000\u00a9\u00ac\u0001\u0000\u0000"+
		"\u0000\u00aa\u00a8\u0001\u0000\u0000\u0000\u00aa\u00ab\u0001\u0000\u0000"+
		"\u0000\u00ab\u00ae\u0001\u0000\u0000\u0000\u00ac\u00aa\u0001\u0000\u0000"+
		"\u0000\u00ad\u00af\u0005\t\u0000\u0000\u00ae\u00ad\u0001\u0000\u0000\u0000"+
		"\u00ae\u00af\u0001\u0000\u0000\u0000\u00af\u00bd\u0001\u0000\u0000\u0000"+
		"\u00b0\u00b5\u0005\t\u0000\u0000\u00b1\u00b2\u0005\u0010\u0000\u0000\u00b2"+
		"\u00b4\u0005\t\u0000\u0000\u00b3\u00b1\u0001\u0000\u0000\u0000\u00b4\u00b7"+
		"\u0001\u0000\u0000\u0000\u00b5\u00b3\u0001\u0000\u0000\u0000\u00b5\u00b6"+
		"\u0001\u0000\u0000\u0000\u00b6\u00b9\u0001\u0000\u0000\u0000\u00b7\u00b5"+
		"\u0001\u0000\u0000\u0000\u00b8\u00ba\u0005\u0010\u0000\u0000\u00b9\u00b8"+
		"\u0001\u0000\u0000\u0000\u00b9\u00ba\u0001\u0000\u0000\u0000\u00ba\u00bd"+
		"\u0001\u0000\u0000\u0000\u00bb\u00bd\u0005\u0001\u0000\u0000\u00bc\u00a5"+
		"\u0001\u0000\u0000\u0000\u00bc\u00b0\u0001\u0000\u0000\u0000\u00bc\u00bb"+
		"\u0001\u0000\u0000\u0000\u00bd\u0015\u0001\u0000\u0000\u0000\u001b\u001a"+
		"\u001f&/5=CKQWbhnu{\u0083\u0085\u008c\u0094\u0096\u009c\u00a2\u00aa\u00ae"+
		"\u00b5\u00b9\u00bc";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
