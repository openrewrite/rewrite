/*
 * Copyright 2024 the original author or authors.
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

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class MethodSignatureParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.11.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		CONSTRUCTOR=1, LPAREN=2, RPAREN=3, LBRACK=4, RBRACK=5, COMMA=6, DOT=7, 
		BANG=8, WILDCARD=9, AND=10, OR=11, ELLIPSIS=12, DOTDOT=13, POUND=14, SPACE=15, 
		Identifier=16;
	public static final int
		RULE_methodPattern = 0, RULE_formalParametersPattern = 1, RULE_formalsPattern = 2, 
		RULE_dotDot = 3, RULE_formalsPatternAfterDotDot = 4, RULE_optionalParensTypePattern = 5, 
		RULE_targetTypePattern = 6, RULE_formalTypePattern = 7, RULE_classNameOrInterface = 8, 
		RULE_simpleNamePattern = 9;
	private static String[] makeRuleNames() {
		return new String[] {
			"methodPattern", "formalParametersPattern", "formalsPattern", "dotDot", 
			"formalsPatternAfterDotDot", "optionalParensTypePattern", "targetTypePattern", 
			"formalTypePattern", "classNameOrInterface", "simpleNamePattern"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'<constructor>'", "'('", "')'", "'['", "']'", "','", "'.'", "'!'", 
			"'*'", "'&&'", "'||'", "'...'", "'..'", "'#'", "' '"
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
	public String getGrammarFileName() { return "java-escape"; }

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
			if ( visitor instanceof MethodSignatureParserVisitor ) return ((MethodSignatureParserVisitor<? extends T>)visitor).visitMethodPattern(this);
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
			setState(20);
			targetTypePattern(0);
			setState(29);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case CONSTRUCTOR:
			case WILDCARD:
			case SPACE:
			case Identifier:
				{
				setState(24);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==SPACE) {
					{
					{
					setState(21);
					match(SPACE);
					}
					}
					setState(26);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case DOT:
				{
				setState(27);
				match(DOT);
				}
				break;
			case POUND:
				{
				setState(28);
				match(POUND);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(31);
			simpleNamePattern();
			setState(32);
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
			if ( visitor instanceof MethodSignatureParserVisitor ) return ((MethodSignatureParserVisitor<? extends T>)visitor).visitFormalParametersPattern(this);
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
			setState(34);
			match(LPAREN);
			setState(36);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((_la) & ~0x3f) == 0 && ((1L << _la) & 74628L) != 0) {
				{
				setState(35);
				formalsPattern();
				}
			}

			setState(38);
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
			if ( visitor instanceof MethodSignatureParserVisitor ) return ((MethodSignatureParserVisitor<? extends T>)visitor).visitFormalsPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FormalsPatternContext formalsPattern() throws RecognitionException {
		FormalsPatternContext _localctx = new FormalsPatternContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_formalsPattern);
		int _la;
		try {
			int _alt;
			setState(71);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(40);
				dotDot();
				setState(51);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,4,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(41);
						match(COMMA);
						setState(45);
						_errHandler.sync(this);
						_la = _input.LA(1);
						while (_la==SPACE) {
							{
							{
							setState(42);
							match(SPACE);
							}
							}
							setState(47);
							_errHandler.sync(this);
							_la = _input.LA(1);
						}
						setState(48);
						formalsPatternAfterDotDot();
						}
						} 
					}
					setState(53);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,4,_ctx);
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(54);
				optionalParensTypePattern();
				setState(65);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,6,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(55);
						match(COMMA);
						setState(59);
						_errHandler.sync(this);
						_la = _input.LA(1);
						while (_la==SPACE) {
							{
							{
							setState(56);
							match(SPACE);
							}
							}
							setState(61);
							_errHandler.sync(this);
							_la = _input.LA(1);
						}
						setState(62);
						formalsPattern();
						}
						} 
					}
					setState(67);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,6,_ctx);
				}
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(68);
				formalTypePattern(0);
				setState(69);
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
			if ( visitor instanceof MethodSignatureParserVisitor ) return ((MethodSignatureParserVisitor<? extends T>)visitor).visitDotDot(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DotDotContext dotDot() throws RecognitionException {
		DotDotContext _localctx = new DotDotContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_dotDot);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(73);
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
			if ( visitor instanceof MethodSignatureParserVisitor ) return ((MethodSignatureParserVisitor<? extends T>)visitor).visitFormalsPatternAfterDotDot(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FormalsPatternAfterDotDotContext formalsPatternAfterDotDot() throws RecognitionException {
		FormalsPatternAfterDotDotContext _localctx = new FormalsPatternAfterDotDotContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_formalsPatternAfterDotDot);
		int _la;
		try {
			int _alt;
			setState(92);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,10,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(75);
				optionalParensTypePattern();
				setState(86);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,9,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(76);
						match(COMMA);
						setState(80);
						_errHandler.sync(this);
						_la = _input.LA(1);
						while (_la==SPACE) {
							{
							{
							setState(77);
							match(SPACE);
							}
							}
							setState(82);
							_errHandler.sync(this);
							_la = _input.LA(1);
						}
						setState(83);
						formalsPatternAfterDotDot();
						}
						} 
					}
					setState(88);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,9,_ctx);
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(89);
				formalTypePattern(0);
				setState(90);
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
			if ( visitor instanceof MethodSignatureParserVisitor ) return ((MethodSignatureParserVisitor<? extends T>)visitor).visitOptionalParensTypePattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OptionalParensTypePatternContext optionalParensTypePattern() throws RecognitionException {
		OptionalParensTypePatternContext _localctx = new OptionalParensTypePatternContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_optionalParensTypePattern);
		try {
			setState(99);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LPAREN:
				enterOuterAlt(_localctx, 1);
				{
				setState(94);
				match(LPAREN);
				setState(95);
				formalTypePattern(0);
				setState(96);
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
				setState(98);
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
			if ( visitor instanceof MethodSignatureParserVisitor ) return ((MethodSignatureParserVisitor<? extends T>)visitor).visitTargetTypePattern(this);
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
			setState(105);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DOT:
			case WILDCARD:
			case DOTDOT:
			case Identifier:
				{
				setState(102);
				classNameOrInterface();
				}
				break;
			case BANG:
				{
				setState(103);
				match(BANG);
				setState(104);
				targetTypePattern(3);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(115);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,14,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(113);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,13,_ctx) ) {
					case 1:
						{
						_localctx = new TargetTypePatternContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_targetTypePattern);
						setState(107);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(108);
						match(AND);
						setState(109);
						targetTypePattern(3);
						}
						break;
					case 2:
						{
						_localctx = new TargetTypePatternContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_targetTypePattern);
						setState(110);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(111);
						match(OR);
						setState(112);
						targetTypePattern(2);
						}
						break;
					}
					} 
				}
				setState(117);
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
			if ( visitor instanceof MethodSignatureParserVisitor ) return ((MethodSignatureParserVisitor<? extends T>)visitor).visitFormalTypePattern(this);
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
			setState(122);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DOT:
			case WILDCARD:
			case DOTDOT:
			case Identifier:
				{
				setState(119);
				classNameOrInterface();
				}
				break;
			case BANG:
				{
				setState(120);
				match(BANG);
				setState(121);
				formalTypePattern(3);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(132);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,17,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(130);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,16,_ctx) ) {
					case 1:
						{
						_localctx = new FormalTypePatternContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_formalTypePattern);
						setState(124);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(125);
						match(AND);
						setState(126);
						formalTypePattern(3);
						}
						break;
					case 2:
						{
						_localctx = new FormalTypePatternContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_formalTypePattern);
						setState(127);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(128);
						match(OR);
						setState(129);
						formalTypePattern(2);
						}
						break;
					}
					} 
				}
				setState(134);
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
			if ( visitor instanceof MethodSignatureParserVisitor ) return ((MethodSignatureParserVisitor<? extends T>)visitor).visitClassNameOrInterface(this);
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
			setState(136); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(135);
					_la = _input.LA(1);
					if ( !(((_la) & ~0x3f) == 0 && ((1L << _la) & 74368L) != 0) ) {
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
				setState(138); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,18,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
			setState(144);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,19,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(140);
					match(LBRACK);
					setState(141);
					match(RBRACK);
					}
					} 
				}
				setState(146);
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
			if ( visitor instanceof MethodSignatureParserVisitor ) return ((MethodSignatureParserVisitor<? extends T>)visitor).visitSimpleNamePattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SimpleNamePatternContext simpleNamePattern() throws RecognitionException {
		SimpleNamePatternContext _localctx = new SimpleNamePatternContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_simpleNamePattern);
		int _la;
		try {
			int _alt;
			setState(170);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(147);
				match(Identifier);
				setState(152);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,20,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(148);
						match(WILDCARD);
						setState(149);
						match(Identifier);
						}
						} 
					}
					setState(154);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,20,_ctx);
				}
				setState(156);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WILDCARD) {
					{
					setState(155);
					match(WILDCARD);
					}
				}

				}
				break;
			case WILDCARD:
				enterOuterAlt(_localctx, 2);
				{
				setState(158);
				match(WILDCARD);
				setState(163);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,22,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(159);
						match(Identifier);
						setState(160);
						match(WILDCARD);
						}
						} 
					}
					setState(165);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,22,_ctx);
				}
				setState(167);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==Identifier) {
					{
					setState(166);
					match(Identifier);
					}
				}

				}
				break;
			case CONSTRUCTOR:
				enterOuterAlt(_localctx, 3);
				{
				setState(169);
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

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 6:
			return targetTypePattern_sempred((TargetTypePatternContext)_localctx, predIndex);
		case 7:
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
		"\u0004\u0001\u0010\u00ad\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001"+
		"\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004"+
		"\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007"+
		"\u0002\b\u0007\b\u0002\t\u0007\t\u0001\u0000\u0001\u0000\u0005\u0000\u0017"+
		"\b\u0000\n\u0000\f\u0000\u001a\t\u0000\u0001\u0000\u0001\u0000\u0003\u0000"+
		"\u001e\b\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001"+
		"\u0003\u0001%\b\u0001\u0001\u0001\u0001\u0001\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0005\u0002,\b\u0002\n\u0002\f\u0002/\t\u0002\u0001\u0002"+
		"\u0005\u00022\b\u0002\n\u0002\f\u00025\t\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0005\u0002:\b\u0002\n\u0002\f\u0002=\t\u0002\u0001\u0002"+
		"\u0005\u0002@\b\u0002\n\u0002\f\u0002C\t\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0003\u0002H\b\u0002\u0001\u0003\u0001\u0003\u0001\u0004"+
		"\u0001\u0004\u0001\u0004\u0005\u0004O\b\u0004\n\u0004\f\u0004R\t\u0004"+
		"\u0001\u0004\u0005\u0004U\b\u0004\n\u0004\f\u0004X\t\u0004\u0001\u0004"+
		"\u0001\u0004\u0001\u0004\u0003\u0004]\b\u0004\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0001\u0005\u0001\u0005\u0003\u0005d\b\u0005\u0001\u0006"+
		"\u0001\u0006\u0001\u0006\u0001\u0006\u0003\u0006j\b\u0006\u0001\u0006"+
		"\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0005\u0006"+
		"r\b\u0006\n\u0006\f\u0006u\t\u0006\u0001\u0007\u0001\u0007\u0001\u0007"+
		"\u0001\u0007\u0003\u0007{\b\u0007\u0001\u0007\u0001\u0007\u0001\u0007"+
		"\u0001\u0007\u0001\u0007\u0001\u0007\u0005\u0007\u0083\b\u0007\n\u0007"+
		"\f\u0007\u0086\t\u0007\u0001\b\u0004\b\u0089\b\b\u000b\b\f\b\u008a\u0001"+
		"\b\u0001\b\u0005\b\u008f\b\b\n\b\f\b\u0092\t\b\u0001\t\u0001\t\u0001\t"+
		"\u0005\t\u0097\b\t\n\t\f\t\u009a\t\t\u0001\t\u0003\t\u009d\b\t\u0001\t"+
		"\u0001\t\u0001\t\u0005\t\u00a2\b\t\n\t\f\t\u00a5\t\t\u0001\t\u0003\t\u00a8"+
		"\b\t\u0001\t\u0003\t\u00ab\b\t\u0001\t\u0000\u0002\f\u000e\n\u0000\u0002"+
		"\u0004\u0006\b\n\f\u000e\u0010\u0012\u0000\u0001\u0004\u0000\u0007\u0007"+
		"\t\t\r\r\u0010\u0010\u00be\u0000\u0014\u0001\u0000\u0000\u0000\u0002\""+
		"\u0001\u0000\u0000\u0000\u0004G\u0001\u0000\u0000\u0000\u0006I\u0001\u0000"+
		"\u0000\u0000\b\\\u0001\u0000\u0000\u0000\nc\u0001\u0000\u0000\u0000\f"+
		"i\u0001\u0000\u0000\u0000\u000ez\u0001\u0000\u0000\u0000\u0010\u0088\u0001"+
		"\u0000\u0000\u0000\u0012\u00aa\u0001\u0000\u0000\u0000\u0014\u001d\u0003"+
		"\f\u0006\u0000\u0015\u0017\u0005\u000f\u0000\u0000\u0016\u0015\u0001\u0000"+
		"\u0000\u0000\u0017\u001a\u0001\u0000\u0000\u0000\u0018\u0016\u0001\u0000"+
		"\u0000\u0000\u0018\u0019\u0001\u0000\u0000\u0000\u0019\u001e\u0001\u0000"+
		"\u0000\u0000\u001a\u0018\u0001\u0000\u0000\u0000\u001b\u001e\u0005\u0007"+
		"\u0000\u0000\u001c\u001e\u0005\u000e\u0000\u0000\u001d\u0018\u0001\u0000"+
		"\u0000\u0000\u001d\u001b\u0001\u0000\u0000\u0000\u001d\u001c\u0001\u0000"+
		"\u0000\u0000\u001e\u001f\u0001\u0000\u0000\u0000\u001f \u0003\u0012\t"+
		"\u0000 !\u0003\u0002\u0001\u0000!\u0001\u0001\u0000\u0000\u0000\"$\u0005"+
		"\u0002\u0000\u0000#%\u0003\u0004\u0002\u0000$#\u0001\u0000\u0000\u0000"+
		"$%\u0001\u0000\u0000\u0000%&\u0001\u0000\u0000\u0000&\'\u0005\u0003\u0000"+
		"\u0000\'\u0003\u0001\u0000\u0000\u0000(3\u0003\u0006\u0003\u0000)-\u0005"+
		"\u0006\u0000\u0000*,\u0005\u000f\u0000\u0000+*\u0001\u0000\u0000\u0000"+
		",/\u0001\u0000\u0000\u0000-+\u0001\u0000\u0000\u0000-.\u0001\u0000\u0000"+
		"\u0000.0\u0001\u0000\u0000\u0000/-\u0001\u0000\u0000\u000002\u0003\b\u0004"+
		"\u00001)\u0001\u0000\u0000\u000025\u0001\u0000\u0000\u000031\u0001\u0000"+
		"\u0000\u000034\u0001\u0000\u0000\u00004H\u0001\u0000\u0000\u000053\u0001"+
		"\u0000\u0000\u00006A\u0003\n\u0005\u00007;\u0005\u0006\u0000\u00008:\u0005"+
		"\u000f\u0000\u000098\u0001\u0000\u0000\u0000:=\u0001\u0000\u0000\u0000"+
		";9\u0001\u0000\u0000\u0000;<\u0001\u0000\u0000\u0000<>\u0001\u0000\u0000"+
		"\u0000=;\u0001\u0000\u0000\u0000>@\u0003\u0004\u0002\u0000?7\u0001\u0000"+
		"\u0000\u0000@C\u0001\u0000\u0000\u0000A?\u0001\u0000\u0000\u0000AB\u0001"+
		"\u0000\u0000\u0000BH\u0001\u0000\u0000\u0000CA\u0001\u0000\u0000\u0000"+
		"DE\u0003\u000e\u0007\u0000EF\u0005\f\u0000\u0000FH\u0001\u0000\u0000\u0000"+
		"G(\u0001\u0000\u0000\u0000G6\u0001\u0000\u0000\u0000GD\u0001\u0000\u0000"+
		"\u0000H\u0005\u0001\u0000\u0000\u0000IJ\u0005\r\u0000\u0000J\u0007\u0001"+
		"\u0000\u0000\u0000KV\u0003\n\u0005\u0000LP\u0005\u0006\u0000\u0000MO\u0005"+
		"\u000f\u0000\u0000NM\u0001\u0000\u0000\u0000OR\u0001\u0000\u0000\u0000"+
		"PN\u0001\u0000\u0000\u0000PQ\u0001\u0000\u0000\u0000QS\u0001\u0000\u0000"+
		"\u0000RP\u0001\u0000\u0000\u0000SU\u0003\b\u0004\u0000TL\u0001\u0000\u0000"+
		"\u0000UX\u0001\u0000\u0000\u0000VT\u0001\u0000\u0000\u0000VW\u0001\u0000"+
		"\u0000\u0000W]\u0001\u0000\u0000\u0000XV\u0001\u0000\u0000\u0000YZ\u0003"+
		"\u000e\u0007\u0000Z[\u0005\f\u0000\u0000[]\u0001\u0000\u0000\u0000\\K"+
		"\u0001\u0000\u0000\u0000\\Y\u0001\u0000\u0000\u0000]\t\u0001\u0000\u0000"+
		"\u0000^_\u0005\u0002\u0000\u0000_`\u0003\u000e\u0007\u0000`a\u0005\u0003"+
		"\u0000\u0000ad\u0001\u0000\u0000\u0000bd\u0003\u000e\u0007\u0000c^\u0001"+
		"\u0000\u0000\u0000cb\u0001\u0000\u0000\u0000d\u000b\u0001\u0000\u0000"+
		"\u0000ef\u0006\u0006\uffff\uffff\u0000fj\u0003\u0010\b\u0000gh\u0005\b"+
		"\u0000\u0000hj\u0003\f\u0006\u0003ie\u0001\u0000\u0000\u0000ig\u0001\u0000"+
		"\u0000\u0000js\u0001\u0000\u0000\u0000kl\n\u0002\u0000\u0000lm\u0005\n"+
		"\u0000\u0000mr\u0003\f\u0006\u0003no\n\u0001\u0000\u0000op\u0005\u000b"+
		"\u0000\u0000pr\u0003\f\u0006\u0002qk\u0001\u0000\u0000\u0000qn\u0001\u0000"+
		"\u0000\u0000ru\u0001\u0000\u0000\u0000sq\u0001\u0000\u0000\u0000st\u0001"+
		"\u0000\u0000\u0000t\r\u0001\u0000\u0000\u0000us\u0001\u0000\u0000\u0000"+
		"vw\u0006\u0007\uffff\uffff\u0000w{\u0003\u0010\b\u0000xy\u0005\b\u0000"+
		"\u0000y{\u0003\u000e\u0007\u0003zv\u0001\u0000\u0000\u0000zx\u0001\u0000"+
		"\u0000\u0000{\u0084\u0001\u0000\u0000\u0000|}\n\u0002\u0000\u0000}~\u0005"+
		"\n\u0000\u0000~\u0083\u0003\u000e\u0007\u0003\u007f\u0080\n\u0001\u0000"+
		"\u0000\u0080\u0081\u0005\u000b\u0000\u0000\u0081\u0083\u0003\u000e\u0007"+
		"\u0002\u0082|\u0001\u0000\u0000\u0000\u0082\u007f\u0001\u0000\u0000\u0000"+
		"\u0083\u0086\u0001\u0000\u0000\u0000\u0084\u0082\u0001\u0000\u0000\u0000"+
		"\u0084\u0085\u0001\u0000\u0000\u0000\u0085\u000f\u0001\u0000\u0000\u0000"+
		"\u0086\u0084\u0001\u0000\u0000\u0000\u0087\u0089\u0007\u0000\u0000\u0000"+
		"\u0088\u0087\u0001\u0000\u0000\u0000\u0089\u008a\u0001\u0000\u0000\u0000"+
		"\u008a\u0088\u0001\u0000\u0000\u0000\u008a\u008b\u0001\u0000\u0000\u0000"+
		"\u008b\u0090\u0001\u0000\u0000\u0000\u008c\u008d\u0005\u0004\u0000\u0000"+
		"\u008d\u008f\u0005\u0005\u0000\u0000\u008e\u008c\u0001\u0000\u0000\u0000"+
		"\u008f\u0092\u0001\u0000\u0000\u0000\u0090\u008e\u0001\u0000\u0000\u0000"+
		"\u0090\u0091\u0001\u0000\u0000\u0000\u0091\u0011\u0001\u0000\u0000\u0000"+
		"\u0092\u0090\u0001\u0000\u0000\u0000\u0093\u0098\u0005\u0010\u0000\u0000"+
		"\u0094\u0095\u0005\t\u0000\u0000\u0095\u0097\u0005\u0010\u0000\u0000\u0096"+
		"\u0094\u0001\u0000\u0000\u0000\u0097\u009a\u0001\u0000\u0000\u0000\u0098"+
		"\u0096\u0001\u0000\u0000\u0000\u0098\u0099\u0001\u0000\u0000\u0000\u0099"+
		"\u009c\u0001\u0000\u0000\u0000\u009a\u0098\u0001\u0000\u0000\u0000\u009b"+
		"\u009d\u0005\t\u0000\u0000\u009c\u009b\u0001\u0000\u0000\u0000\u009c\u009d"+
		"\u0001\u0000\u0000\u0000\u009d\u00ab\u0001\u0000\u0000\u0000\u009e\u00a3"+
		"\u0005\t\u0000\u0000\u009f\u00a0\u0005\u0010\u0000\u0000\u00a0\u00a2\u0005"+
		"\t\u0000\u0000\u00a1\u009f\u0001\u0000\u0000\u0000\u00a2\u00a5\u0001\u0000"+
		"\u0000\u0000\u00a3\u00a1\u0001\u0000\u0000\u0000\u00a3\u00a4\u0001\u0000"+
		"\u0000\u0000\u00a4\u00a7\u0001\u0000\u0000\u0000\u00a5\u00a3\u0001\u0000"+
		"\u0000\u0000\u00a6\u00a8\u0005\u0010\u0000\u0000\u00a7\u00a6\u0001\u0000"+
		"\u0000\u0000\u00a7\u00a8\u0001\u0000\u0000\u0000\u00a8\u00ab\u0001\u0000"+
		"\u0000\u0000\u00a9\u00ab\u0005\u0001\u0000\u0000\u00aa\u0093\u0001\u0000"+
		"\u0000\u0000\u00aa\u009e\u0001\u0000\u0000\u0000\u00aa\u00a9\u0001\u0000"+
		"\u0000\u0000\u00ab\u0013\u0001\u0000\u0000\u0000\u0019\u0018\u001d$-3"+
		";AGPV\\ciqsz\u0082\u0084\u008a\u0090\u0098\u009c\u00a3\u00a7\u00aa";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}