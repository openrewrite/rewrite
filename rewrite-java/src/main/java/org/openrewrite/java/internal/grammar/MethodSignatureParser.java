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
// Generated from /Users/jon/Projects/github/openrewrite/rewrite/rewrite-java/src/main/antlr/MethodSignatureParser.g4 by ANTLR 4.13.2
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
		CONSTRUCTOR=1, JAVASCRIPT_DEFAULT_METHOD=2, LPAREN=3, RPAREN=4, LBRACK=5, 
		RBRACK=6, COMMA=7, DOT=8, BANG=9, WILDCARD=10, AND=11, OR=12, ELLIPSIS=13, 
		DOTDOT=14, POUND=15, SPACE=16, Identifier=17;
	public static final int
		RULE_methodPattern = 0, RULE_formalParametersPattern = 1, RULE_formalsPattern = 2, 
		RULE_formalsTail = 3, RULE_formalsPatternAfterDotDot = 4, RULE_targetTypePattern = 5, 
		RULE_formalTypePattern = 6, RULE_classNameOrInterface = 7, RULE_arrayDimensions = 8, 
		RULE_simpleNamePattern = 9, RULE_simpleNamePart = 10;
	private static String[] makeRuleNames() {
		return new String[] {
			"methodPattern", "formalParametersPattern", "formalsPattern", "formalsTail", 
			"formalsPatternAfterDotDot", "targetTypePattern", "formalTypePattern", 
			"classNameOrInterface", "arrayDimensions", "simpleNamePattern", "simpleNamePart"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, "'<default>'", null, null, "'['", "']'", null, null, "'!'", 
			"'*'", "'&&'", "'||'", "'...'", "'..'", "'#'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "CONSTRUCTOR", "JAVASCRIPT_DEFAULT_METHOD", "LPAREN", "RPAREN", 
			"LBRACK", "RBRACK", "COMMA", "DOT", "BANG", "WILDCARD", "AND", "OR", 
			"ELLIPSIS", "DOTDOT", "POUND", "SPACE", "Identifier"
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
			setState(22);
			targetTypePattern();
			setState(29);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SPACE:
				{
				setState(24); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(23);
					match(SPACE);
					}
					}
					setState(26); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==SPACE );
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
			setState(35);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==SPACE) {
				{
				{
				setState(32);
				match(SPACE);
				}
				}
				setState(37);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(38);
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
			setState(40);
			match(LPAREN);
			setState(42);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 148992L) != 0)) {
				{
				setState(41);
				formalsPattern();
				}
			}

			setState(44);
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
		public TerminalNode DOTDOT() { return getToken(MethodSignatureParser.DOTDOT, 0); }
		public FormalsPatternAfterDotDotContext formalsPatternAfterDotDot() {
			return getRuleContext(FormalsPatternAfterDotDotContext.class,0);
		}
		public FormalTypePatternContext formalTypePattern() {
			return getRuleContext(FormalTypePatternContext.class,0);
		}
		public TerminalNode ELLIPSIS() { return getToken(MethodSignatureParser.ELLIPSIS, 0); }
		public FormalsTailContext formalsTail() {
			return getRuleContext(FormalsTailContext.class,0);
		}
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
			setState(57);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DOTDOT:
				enterOuterAlt(_localctx, 1);
				{
				setState(46);
				match(DOTDOT);
				setState(48);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==COMMA) {
					{
					setState(47);
					formalsPatternAfterDotDot();
					}
				}

				}
				break;
			case BANG:
			case WILDCARD:
			case Identifier:
				enterOuterAlt(_localctx, 2);
				{
				setState(50);
				formalTypePattern();
				setState(55);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case ELLIPSIS:
					{
					setState(51);
					match(ELLIPSIS);
					}
					break;
				case RPAREN:
				case COMMA:
					{
					setState(53);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==COMMA) {
						{
						setState(52);
						formalsTail();
						}
					}

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

	@SuppressWarnings("CheckReturnValue")
	public static class FormalsTailContext extends ParserRuleContext {
		public TerminalNode COMMA() { return getToken(MethodSignatureParser.COMMA, 0); }
		public FormalsPatternContext formalsPattern() {
			return getRuleContext(FormalsPatternContext.class,0);
		}
		public FormalsTailContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_formalsTail; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MethodSignatureParserListener ) ((MethodSignatureParserListener)listener).enterFormalsTail(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MethodSignatureParserListener ) ((MethodSignatureParserListener)listener).exitFormalsTail(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MethodSignatureParserVisitor ) return ((MethodSignatureParserVisitor<? extends T>)visitor).visitFormalsTail(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FormalsTailContext formalsTail() throws RecognitionException {
		FormalsTailContext _localctx = new FormalsTailContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_formalsTail);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(59);
			match(COMMA);
			setState(60);
			formalsPattern();
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
		public TerminalNode COMMA() { return getToken(MethodSignatureParser.COMMA, 0); }
		public FormalTypePatternContext formalTypePattern() {
			return getRuleContext(FormalTypePatternContext.class,0);
		}
		public TerminalNode ELLIPSIS() { return getToken(MethodSignatureParser.ELLIPSIS, 0); }
		public FormalsPatternAfterDotDotContext formalsPatternAfterDotDot() {
			return getRuleContext(FormalsPatternAfterDotDotContext.class,0);
		}
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
			enterOuterAlt(_localctx, 1);
			{
			setState(62);
			match(COMMA);
			setState(63);
			formalTypePattern();
			setState(68);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ELLIPSIS:
				{
				setState(64);
				match(ELLIPSIS);
				}
				break;
			case RPAREN:
			case COMMA:
				{
				setState(66);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==COMMA) {
					{
					setState(65);
					formalsPatternAfterDotDot();
					}
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

	@SuppressWarnings("CheckReturnValue")
	public static class TargetTypePatternContext extends ParserRuleContext {
		public ClassNameOrInterfaceContext classNameOrInterface() {
			return getRuleContext(ClassNameOrInterfaceContext.class,0);
		}
		public TerminalNode BANG() { return getToken(MethodSignatureParser.BANG, 0); }
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
		TargetTypePatternContext _localctx = new TargetTypePatternContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_targetTypePattern);
		try {
			setState(73);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case WILDCARD:
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(70);
				classNameOrInterface();
				}
				break;
			case BANG:
				enterOuterAlt(_localctx, 2);
				{
				setState(71);
				match(BANG);
				setState(72);
				classNameOrInterface();
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
	public static class FormalTypePatternContext extends ParserRuleContext {
		public ClassNameOrInterfaceContext classNameOrInterface() {
			return getRuleContext(ClassNameOrInterfaceContext.class,0);
		}
		public TerminalNode BANG() { return getToken(MethodSignatureParser.BANG, 0); }
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
		FormalTypePatternContext _localctx = new FormalTypePatternContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_formalTypePattern);
		try {
			setState(78);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case WILDCARD:
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(75);
				classNameOrInterface();
				}
				break;
			case BANG:
				enterOuterAlt(_localctx, 2);
				{
				setState(76);
				match(BANG);
				setState(77);
				classNameOrInterface();
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
	public static class ClassNameOrInterfaceContext extends ParserRuleContext {
		public List<TerminalNode> Identifier() { return getTokens(MethodSignatureParser.Identifier); }
		public TerminalNode Identifier(int i) {
			return getToken(MethodSignatureParser.Identifier, i);
		}
		public List<TerminalNode> WILDCARD() { return getTokens(MethodSignatureParser.WILDCARD); }
		public TerminalNode WILDCARD(int i) {
			return getToken(MethodSignatureParser.WILDCARD, i);
		}
		public ArrayDimensionsContext arrayDimensions() {
			return getRuleContext(ArrayDimensionsContext.class,0);
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
		enterRule(_localctx, 14, RULE_classNameOrInterface);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(80);
			_la = _input.LA(1);
			if ( !(_la==WILDCARD || _la==Identifier) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(84);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 148736L) != 0)) {
				{
				{
				setState(81);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 148736L) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				}
				setState(86);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(88);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LBRACK) {
				{
				setState(87);
				arrayDimensions();
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
	public static class ArrayDimensionsContext extends ParserRuleContext {
		public TerminalNode LBRACK() { return getToken(MethodSignatureParser.LBRACK, 0); }
		public TerminalNode RBRACK() { return getToken(MethodSignatureParser.RBRACK, 0); }
		public ArrayDimensionsContext arrayDimensions() {
			return getRuleContext(ArrayDimensionsContext.class,0);
		}
		public ArrayDimensionsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arrayDimensions; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MethodSignatureParserListener ) ((MethodSignatureParserListener)listener).enterArrayDimensions(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MethodSignatureParserListener ) ((MethodSignatureParserListener)listener).exitArrayDimensions(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MethodSignatureParserVisitor ) return ((MethodSignatureParserVisitor<? extends T>)visitor).visitArrayDimensions(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArrayDimensionsContext arrayDimensions() throws RecognitionException {
		ArrayDimensionsContext _localctx = new ArrayDimensionsContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_arrayDimensions);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(90);
			match(LBRACK);
			setState(91);
			match(RBRACK);
			setState(93);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LBRACK) {
				{
				setState(92);
				arrayDimensions();
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
	public static class SimpleNamePatternContext extends ParserRuleContext {
		public List<SimpleNamePartContext> simpleNamePart() {
			return getRuleContexts(SimpleNamePartContext.class);
		}
		public SimpleNamePartContext simpleNamePart(int i) {
			return getRuleContext(SimpleNamePartContext.class,i);
		}
		public TerminalNode JAVASCRIPT_DEFAULT_METHOD() { return getToken(MethodSignatureParser.JAVASCRIPT_DEFAULT_METHOD, 0); }
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
			setState(102);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case WILDCARD:
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(96); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(95);
					simpleNamePart();
					}
					}
					setState(98); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==WILDCARD || _la==Identifier );
				}
				break;
			case JAVASCRIPT_DEFAULT_METHOD:
				enterOuterAlt(_localctx, 2);
				{
				setState(100);
				match(JAVASCRIPT_DEFAULT_METHOD);
				}
				break;
			case CONSTRUCTOR:
				enterOuterAlt(_localctx, 3);
				{
				setState(101);
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

	@SuppressWarnings("CheckReturnValue")
	public static class SimpleNamePartContext extends ParserRuleContext {
		public TerminalNode Identifier() { return getToken(MethodSignatureParser.Identifier, 0); }
		public TerminalNode WILDCARD() { return getToken(MethodSignatureParser.WILDCARD, 0); }
		public SimpleNamePartContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_simpleNamePart; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof MethodSignatureParserListener ) ((MethodSignatureParserListener)listener).enterSimpleNamePart(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof MethodSignatureParserListener ) ((MethodSignatureParserListener)listener).exitSimpleNamePart(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof MethodSignatureParserVisitor ) return ((MethodSignatureParserVisitor<? extends T>)visitor).visitSimpleNamePart(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SimpleNamePartContext simpleNamePart() throws RecognitionException {
		SimpleNamePartContext _localctx = new SimpleNamePartContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_simpleNamePart);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(104);
			_la = _input.LA(1);
			if ( !(_la==WILDCARD || _la==Identifier) ) {
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

	public static final String _serializedATN =
		"\u0004\u0001\u0011k\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002"+
		"\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0001\u0000\u0001\u0000\u0004"+
		"\u0000\u0019\b\u0000\u000b\u0000\f\u0000\u001a\u0001\u0000\u0003\u0000"+
		"\u001e\b\u0000\u0001\u0000\u0001\u0000\u0005\u0000\"\b\u0000\n\u0000\f"+
		"\u0000%\t\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0003"+
		"\u0001+\b\u0001\u0001\u0001\u0001\u0001\u0001\u0002\u0001\u0002\u0003"+
		"\u00021\b\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0003\u00026\b\u0002"+
		"\u0003\u00028\b\u0002\u0003\u0002:\b\u0002\u0001\u0003\u0001\u0003\u0001"+
		"\u0003\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0003\u0004C\b"+
		"\u0004\u0003\u0004E\b\u0004\u0001\u0005\u0001\u0005\u0001\u0005\u0003"+
		"\u0005J\b\u0005\u0001\u0006\u0001\u0006\u0001\u0006\u0003\u0006O\b\u0006"+
		"\u0001\u0007\u0001\u0007\u0005\u0007S\b\u0007\n\u0007\f\u0007V\t\u0007"+
		"\u0001\u0007\u0003\u0007Y\b\u0007\u0001\b\u0001\b\u0001\b\u0003\b^\b\b"+
		"\u0001\t\u0004\ta\b\t\u000b\t\f\tb\u0001\t\u0001\t\u0003\tg\b\t\u0001"+
		"\n\u0001\n\u0001\n\u0000\u0000\u000b\u0000\u0002\u0004\u0006\b\n\f\u000e"+
		"\u0010\u0012\u0014\u0000\u0002\u0002\u0000\n\n\u0011\u0011\u0004\u0000"+
		"\b\b\n\n\u000e\u000e\u0011\u0011q\u0000\u0016\u0001\u0000\u0000\u0000"+
		"\u0002(\u0001\u0000\u0000\u0000\u00049\u0001\u0000\u0000\u0000\u0006;"+
		"\u0001\u0000\u0000\u0000\b>\u0001\u0000\u0000\u0000\nI\u0001\u0000\u0000"+
		"\u0000\fN\u0001\u0000\u0000\u0000\u000eP\u0001\u0000\u0000\u0000\u0010"+
		"Z\u0001\u0000\u0000\u0000\u0012f\u0001\u0000\u0000\u0000\u0014h\u0001"+
		"\u0000\u0000\u0000\u0016\u001d\u0003\n\u0005\u0000\u0017\u0019\u0005\u0010"+
		"\u0000\u0000\u0018\u0017\u0001\u0000\u0000\u0000\u0019\u001a\u0001\u0000"+
		"\u0000\u0000\u001a\u0018\u0001\u0000\u0000\u0000\u001a\u001b\u0001\u0000"+
		"\u0000\u0000\u001b\u001e\u0001\u0000\u0000\u0000\u001c\u001e\u0005\u000f"+
		"\u0000\u0000\u001d\u0018\u0001\u0000\u0000\u0000\u001d\u001c\u0001\u0000"+
		"\u0000\u0000\u001e\u001f\u0001\u0000\u0000\u0000\u001f#\u0003\u0012\t"+
		"\u0000 \"\u0005\u0010\u0000\u0000! \u0001\u0000\u0000\u0000\"%\u0001\u0000"+
		"\u0000\u0000#!\u0001\u0000\u0000\u0000#$\u0001\u0000\u0000\u0000$&\u0001"+
		"\u0000\u0000\u0000%#\u0001\u0000\u0000\u0000&\'\u0003\u0002\u0001\u0000"+
		"\'\u0001\u0001\u0000\u0000\u0000(*\u0005\u0003\u0000\u0000)+\u0003\u0004"+
		"\u0002\u0000*)\u0001\u0000\u0000\u0000*+\u0001\u0000\u0000\u0000+,\u0001"+
		"\u0000\u0000\u0000,-\u0005\u0004\u0000\u0000-\u0003\u0001\u0000\u0000"+
		"\u0000.0\u0005\u000e\u0000\u0000/1\u0003\b\u0004\u00000/\u0001\u0000\u0000"+
		"\u000001\u0001\u0000\u0000\u00001:\u0001\u0000\u0000\u000027\u0003\f\u0006"+
		"\u000038\u0005\r\u0000\u000046\u0003\u0006\u0003\u000054\u0001\u0000\u0000"+
		"\u000056\u0001\u0000\u0000\u000068\u0001\u0000\u0000\u000073\u0001\u0000"+
		"\u0000\u000075\u0001\u0000\u0000\u00008:\u0001\u0000\u0000\u00009.\u0001"+
		"\u0000\u0000\u000092\u0001\u0000\u0000\u0000:\u0005\u0001\u0000\u0000"+
		"\u0000;<\u0005\u0007\u0000\u0000<=\u0003\u0004\u0002\u0000=\u0007\u0001"+
		"\u0000\u0000\u0000>?\u0005\u0007\u0000\u0000?D\u0003\f\u0006\u0000@E\u0005"+
		"\r\u0000\u0000AC\u0003\b\u0004\u0000BA\u0001\u0000\u0000\u0000BC\u0001"+
		"\u0000\u0000\u0000CE\u0001\u0000\u0000\u0000D@\u0001\u0000\u0000\u0000"+
		"DB\u0001\u0000\u0000\u0000E\t\u0001\u0000\u0000\u0000FJ\u0003\u000e\u0007"+
		"\u0000GH\u0005\t\u0000\u0000HJ\u0003\u000e\u0007\u0000IF\u0001\u0000\u0000"+
		"\u0000IG\u0001\u0000\u0000\u0000J\u000b\u0001\u0000\u0000\u0000KO\u0003"+
		"\u000e\u0007\u0000LM\u0005\t\u0000\u0000MO\u0003\u000e\u0007\u0000NK\u0001"+
		"\u0000\u0000\u0000NL\u0001\u0000\u0000\u0000O\r\u0001\u0000\u0000\u0000"+
		"PT\u0007\u0000\u0000\u0000QS\u0007\u0001\u0000\u0000RQ\u0001\u0000\u0000"+
		"\u0000SV\u0001\u0000\u0000\u0000TR\u0001\u0000\u0000\u0000TU\u0001\u0000"+
		"\u0000\u0000UX\u0001\u0000\u0000\u0000VT\u0001\u0000\u0000\u0000WY\u0003"+
		"\u0010\b\u0000XW\u0001\u0000\u0000\u0000XY\u0001\u0000\u0000\u0000Y\u000f"+
		"\u0001\u0000\u0000\u0000Z[\u0005\u0005\u0000\u0000[]\u0005\u0006\u0000"+
		"\u0000\\^\u0003\u0010\b\u0000]\\\u0001\u0000\u0000\u0000]^\u0001\u0000"+
		"\u0000\u0000^\u0011\u0001\u0000\u0000\u0000_a\u0003\u0014\n\u0000`_\u0001"+
		"\u0000\u0000\u0000ab\u0001\u0000\u0000\u0000b`\u0001\u0000\u0000\u0000"+
		"bc\u0001\u0000\u0000\u0000cg\u0001\u0000\u0000\u0000dg\u0005\u0002\u0000"+
		"\u0000eg\u0005\u0001\u0000\u0000f`\u0001\u0000\u0000\u0000fd\u0001\u0000"+
		"\u0000\u0000fe\u0001\u0000\u0000\u0000g\u0013\u0001\u0000\u0000\u0000"+
		"hi\u0007\u0000\u0000\u0000i\u0015\u0001\u0000\u0000\u0000\u0011\u001a"+
		"\u001d#*0579BDINTX]bf";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}