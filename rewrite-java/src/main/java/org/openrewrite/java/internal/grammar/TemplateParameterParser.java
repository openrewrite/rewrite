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
// Generated from ~/git/rewrite/rewrite-java/src/main/antlr/TemplateParameterParser.g4 by ANTLR 4.13.2
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
public class TemplateParameterParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		LPAREN=1, RPAREN=2, DOT=3, COLON=4, COMMA=5, LBRACK=6, RBRACK=7, WILDCARD=8,
		LSBRACK=9, RSBRACK=10, Variance=11, FullyQualifiedName=12, Number=13,
		Identifier=14, S=15;
	public static final int
		RULE_matcherPattern = 0, RULE_typedPattern = 1, RULE_patternType = 2,
		RULE_type = 3, RULE_typeParameter = 4, RULE_variance = 5, RULE_typeArray = 6,
		RULE_parameterName = 7, RULE_typeName = 8, RULE_matcherName = 9;
	private static String[] makeRuleNames() {
		return new String[] {
			"matcherPattern", "typedPattern", "patternType", "type", "typeParameter",
			"variance", "typeArray", "parameterName", "typeName", "matcherName"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'('", "')'", "'.'", "':'", "','", "'<'", "'>'", "'?'", "'['",
			"']'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "LPAREN", "RPAREN", "DOT", "COLON", "COMMA", "LBRACK", "RBRACK",
			"WILDCARD", "LSBRACK", "RSBRACK", "Variance", "FullyQualifiedName", "Number",
			"Identifier", "S"
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
	public String getGrammarFileName() { return "TemplateParameterParser.g4"; }

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
			setState(22);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(20);
				typedPattern();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(21);
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
			setState(27);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
			case 1:
				{
				setState(24);
				parameterName();
				setState(25);
				match(COLON);
				}
				break;
			}
			setState(29);
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
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
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
			enterOuterAlt(_localctx, 1);
			{
			setState(31);
			matcherName();
			setState(32);
			match(LPAREN);
			setState(34);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==FullyQualifiedName || _la==Identifier) {
				{
				setState(33);
				type();
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
	public static class TypeContext extends ParserRuleContext {
		public TypeNameContext typeName() {
			return getRuleContext(TypeNameContext.class,0);
		}
		public TerminalNode LBRACK() { return getToken(TemplateParameterParser.LBRACK, 0); }
		public List<TypeParameterContext> typeParameter() {
			return getRuleContexts(TypeParameterContext.class);
		}
		public TypeParameterContext typeParameter(int i) {
			return getRuleContext(TypeParameterContext.class,i);
		}
		public TerminalNode RBRACK() { return getToken(TemplateParameterParser.RBRACK, 0); }
		public List<TypeArrayContext> typeArray() {
			return getRuleContexts(TypeArrayContext.class);
		}
		public TypeArrayContext typeArray(int i) {
			return getRuleContext(TypeArrayContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(TemplateParameterParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(TemplateParameterParser.COMMA, i);
		}
		public TypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParameterParserListener ) ((TemplateParameterParserListener)listener).enterType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParameterParserListener ) ((TemplateParameterParserListener)listener).exitType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParameterParserVisitor ) return ((TemplateParameterParserVisitor<? extends T>)visitor).visitType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeContext type() throws RecognitionException {
		TypeContext _localctx = new TypeContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_type);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(38);
			typeName();
			setState(51);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LBRACK) {
				{
				setState(39);
				match(LBRACK);
				setState(45);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,3,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(40);
						typeParameter();
						setState(41);
						match(COMMA);
						}
						}
					}
					setState(47);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,3,_ctx);
				}
				setState(48);
				typeParameter();
				setState(49);
				match(RBRACK);
				}
			}

			setState(56);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==LSBRACK) {
				{
				{
				setState(53);
				typeArray();
				}
				}
				setState(58);
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
	public static class TypeParameterContext extends ParserRuleContext {
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public VarianceContext variance() {
			return getRuleContext(VarianceContext.class,0);
		}
		public TerminalNode WILDCARD() { return getToken(TemplateParameterParser.WILDCARD, 0); }
		public TypeParameterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeParameter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParameterParserListener ) ((TemplateParameterParserListener)listener).enterTypeParameter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParameterParserListener ) ((TemplateParameterParserListener)listener).exitTypeParameter(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParameterParserVisitor ) return ((TemplateParameterParserVisitor<? extends T>)visitor).visitTypeParameter(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeParameterContext typeParameter() throws RecognitionException {
		TypeParameterContext _localctx = new TypeParameterContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_typeParameter);
		int _la;
		try {
			setState(64);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(60);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==WILDCARD) {
					{
					setState(59);
					variance();
					}
				}

				setState(62);
				type();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(63);
				match(WILDCARD);
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
	public static class VarianceContext extends ParserRuleContext {
		public TerminalNode WILDCARD() { return getToken(TemplateParameterParser.WILDCARD, 0); }
		public TerminalNode Variance() { return getToken(TemplateParameterParser.Variance, 0); }
		public VarianceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variance; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParameterParserListener ) ((TemplateParameterParserListener)listener).enterVariance(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParameterParserListener ) ((TemplateParameterParserListener)listener).exitVariance(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParameterParserVisitor ) return ((TemplateParameterParserVisitor<? extends T>)visitor).visitVariance(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VarianceContext variance() throws RecognitionException {
		VarianceContext _localctx = new VarianceContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_variance);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(66);
			match(WILDCARD);
			setState(67);
			match(Variance);
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
	public static class TypeArrayContext extends ParserRuleContext {
		public TerminalNode LSBRACK() { return getToken(TemplateParameterParser.LSBRACK, 0); }
		public TerminalNode RSBRACK() { return getToken(TemplateParameterParser.RSBRACK, 0); }
		public TypeArrayContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeArray; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParameterParserListener ) ((TemplateParameterParserListener)listener).enterTypeArray(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParameterParserListener ) ((TemplateParameterParserListener)listener).exitTypeArray(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParameterParserVisitor ) return ((TemplateParameterParserVisitor<? extends T>)visitor).visitTypeArray(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeArrayContext typeArray() throws RecognitionException {
		TypeArrayContext _localctx = new TypeArrayContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_typeArray);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(69);
			match(LSBRACK);
			setState(70);
			match(RSBRACK);
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
		enterRule(_localctx, 14, RULE_parameterName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(72);
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
	public static class TypeNameContext extends ParserRuleContext {
		public TerminalNode FullyQualifiedName() { return getToken(TemplateParameterParser.FullyQualifiedName, 0); }
		public TerminalNode Identifier() { return getToken(TemplateParameterParser.Identifier, 0); }
		public TypeNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParameterParserListener ) ((TemplateParameterParserListener)listener).enterTypeName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TemplateParameterParserListener ) ((TemplateParameterParserListener)listener).exitTypeName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TemplateParameterParserVisitor ) return ((TemplateParameterParserVisitor<? extends T>)visitor).visitTypeName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeNameContext typeName() throws RecognitionException {
		TypeNameContext _localctx = new TypeNameContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_typeName);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(74);
			_la = _input.LA(1);
			if ( !(_la==FullyQualifiedName || _la==Identifier) ) {
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
		enterRule(_localctx, 18, RULE_matcherName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(76);
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
		"\u0004\u0001\u000fO\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002"+
		"\b\u0007\b\u0002\t\u0007\t\u0001\u0000\u0001\u0000\u0003\u0000\u0017\b"+
		"\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0003\u0001\u001c\b\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0003\u0002#\b"+
		"\u0002\u0001\u0002\u0001\u0002\u0001\u0003\u0001\u0003\u0001\u0003\u0001"+
		"\u0003\u0001\u0003\u0005\u0003,\b\u0003\n\u0003\f\u0003/\t\u0003\u0001"+
		"\u0003\u0001\u0003\u0001\u0003\u0003\u00034\b\u0003\u0001\u0003\u0005"+
		"\u00037\b\u0003\n\u0003\f\u0003:\t\u0003\u0001\u0004\u0003\u0004=\b\u0004"+
		"\u0001\u0004\u0001\u0004\u0003\u0004A\b\u0004\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0007\u0001\u0007"+
		"\u0001\b\u0001\b\u0001\t\u0001\t\u0001\t\u0000\u0000\n\u0000\u0002\u0004"+
		"\u0006\b\n\f\u000e\u0010\u0012\u0000\u0001\u0002\u0000\f\f\u000e\u000e"+
		"L\u0000\u0016\u0001\u0000\u0000\u0000\u0002\u001b\u0001\u0000\u0000\u0000"+
		"\u0004\u001f\u0001\u0000\u0000\u0000\u0006&\u0001\u0000\u0000\u0000\b"+
		"@\u0001\u0000\u0000\u0000\nB\u0001\u0000\u0000\u0000\fE\u0001\u0000\u0000"+
		"\u0000\u000eH\u0001\u0000\u0000\u0000\u0010J\u0001\u0000\u0000\u0000\u0012"+
		"L\u0001\u0000\u0000\u0000\u0014\u0017\u0003\u0002\u0001\u0000\u0015\u0017"+
		"\u0003\u000e\u0007\u0000\u0016\u0014\u0001\u0000\u0000\u0000\u0016\u0015"+
		"\u0001\u0000\u0000\u0000\u0017\u0001\u0001\u0000\u0000\u0000\u0018\u0019"+
		"\u0003\u000e\u0007\u0000\u0019\u001a\u0005\u0004\u0000\u0000\u001a\u001c"+
		"\u0001\u0000\u0000\u0000\u001b\u0018\u0001\u0000\u0000\u0000\u001b\u001c"+
		"\u0001\u0000\u0000\u0000\u001c\u001d\u0001\u0000\u0000\u0000\u001d\u001e"+
		"\u0003\u0004\u0002\u0000\u001e\u0003\u0001\u0000\u0000\u0000\u001f \u0003"+
		"\u0012\t\u0000 \"\u0005\u0001\u0000\u0000!#\u0003\u0006\u0003\u0000\""+
		"!\u0001\u0000\u0000\u0000\"#\u0001\u0000\u0000\u0000#$\u0001\u0000\u0000"+
		"\u0000$%\u0005\u0002\u0000\u0000%\u0005\u0001\u0000\u0000\u0000&3\u0003"+
		"\u0010\b\u0000\'-\u0005\u0006\u0000\u0000()\u0003\b\u0004\u0000)*\u0005"+
		"\u0005\u0000\u0000*,\u0001\u0000\u0000\u0000+(\u0001\u0000\u0000\u0000"+
		",/\u0001\u0000\u0000\u0000-+\u0001\u0000\u0000\u0000-.\u0001\u0000\u0000"+
		"\u0000.0\u0001\u0000\u0000\u0000/-\u0001\u0000\u0000\u000001\u0003\b\u0004"+
		"\u000012\u0005\u0007\u0000\u000024\u0001\u0000\u0000\u00003\'\u0001\u0000"+
		"\u0000\u000034\u0001\u0000\u0000\u000048\u0001\u0000\u0000\u000057\u0003"+
		"\f\u0006\u000065\u0001\u0000\u0000\u00007:\u0001\u0000\u0000\u000086\u0001"+
		"\u0000\u0000\u000089\u0001\u0000\u0000\u00009\u0007\u0001\u0000\u0000"+
		"\u0000:8\u0001\u0000\u0000\u0000;=\u0003\n\u0005\u0000<;\u0001\u0000\u0000"+
		"\u0000<=\u0001\u0000\u0000\u0000=>\u0001\u0000\u0000\u0000>A\u0003\u0006"+
		"\u0003\u0000?A\u0005\b\u0000\u0000@<\u0001\u0000\u0000\u0000@?\u0001\u0000"+
		"\u0000\u0000A\t\u0001\u0000\u0000\u0000BC\u0005\b\u0000\u0000CD\u0005"+
		"\u000b\u0000\u0000D\u000b\u0001\u0000\u0000\u0000EF\u0005\t\u0000\u0000"+
		"FG\u0005\n\u0000\u0000G\r\u0001\u0000\u0000\u0000HI\u0005\u000e\u0000"+
		"\u0000I\u000f\u0001\u0000\u0000\u0000JK\u0007\u0000\u0000\u0000K\u0011"+
		"\u0001\u0000\u0000\u0000LM\u0005\u000e\u0000\u0000M\u0013\u0001\u0000"+
		"\u0000\u0000\b\u0016\u001b\"-38<@";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
