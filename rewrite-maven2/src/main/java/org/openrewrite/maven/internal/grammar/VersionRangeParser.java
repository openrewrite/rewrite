// Generated from /Users/jon/Projects/github/openrewrite/rewrite/rewrite-maven2/src/main/antlr/VersionRangeParser.g4 by ANTLR 4.8
package org.openrewrite.maven.internal.grammar;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class VersionRangeParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.8", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		COMMA=1, PROPERTY_OPEN=2, PROPERTY_CLOSE=3, OPEN_RANGE_OPEN=4, OPEN_RANGE_CLOSE=5, 
		CLOSED_RANGE_OPEN=6, CLOSED_RANGE_CLOSE=7, Version=8;
	public static final int
		RULE_requestedVersion = 0, RULE_range = 1, RULE_bounds = 2, RULE_boundedLower = 3, 
		RULE_unboundedLower = 4, RULE_version = 5;
	private static String[] makeRuleNames() {
		return new String[] {
			"requestedVersion", "range", "bounds", "boundedLower", "unboundedLower", 
			"version"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "','", "'${'", "'}'", "'('", "')'", "'['", "']'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "COMMA", "PROPERTY_OPEN", "PROPERTY_CLOSE", "OPEN_RANGE_OPEN", 
			"OPEN_RANGE_CLOSE", "CLOSED_RANGE_OPEN", "CLOSED_RANGE_CLOSE", "Version"
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
	public String getGrammarFileName() { return "VersionRangeParser.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public VersionRangeParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	public static class RequestedVersionContext extends ParserRuleContext {
		public List<RangeContext> range() {
			return getRuleContexts(RangeContext.class);
		}
		public RangeContext range(int i) {
			return getRuleContext(RangeContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(VersionRangeParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(VersionRangeParser.COMMA, i);
		}
		public VersionContext version() {
			return getRuleContext(VersionContext.class,0);
		}
		public RequestedVersionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_requestedVersion; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VersionRangeParserListener ) ((VersionRangeParserListener)listener).enterRequestedVersion(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VersionRangeParserListener ) ((VersionRangeParserListener)listener).exitRequestedVersion(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof VersionRangeParserVisitor ) return ((VersionRangeParserVisitor<? extends T>)visitor).visitRequestedVersion(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RequestedVersionContext requestedVersion() throws RecognitionException {
		RequestedVersionContext _localctx = new RequestedVersionContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_requestedVersion);
		int _la;
		try {
			setState(21);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case OPEN_RANGE_OPEN:
			case CLOSED_RANGE_OPEN:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(12);
				range();
				setState(17);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(13);
					match(COMMA);
					setState(14);
					range();
					}
					}
					setState(19);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				}
				break;
			case PROPERTY_OPEN:
			case Version:
				enterOuterAlt(_localctx, 2);
				{
				setState(20);
				version();
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

	public static class RangeContext extends ParserRuleContext {
		public BoundsContext bounds() {
			return getRuleContext(BoundsContext.class,0);
		}
		public TerminalNode OPEN_RANGE_OPEN() { return getToken(VersionRangeParser.OPEN_RANGE_OPEN, 0); }
		public TerminalNode CLOSED_RANGE_OPEN() { return getToken(VersionRangeParser.CLOSED_RANGE_OPEN, 0); }
		public TerminalNode OPEN_RANGE_CLOSE() { return getToken(VersionRangeParser.OPEN_RANGE_CLOSE, 0); }
		public TerminalNode CLOSED_RANGE_CLOSE() { return getToken(VersionRangeParser.CLOSED_RANGE_CLOSE, 0); }
		public RangeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_range; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VersionRangeParserListener ) ((VersionRangeParserListener)listener).enterRange(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VersionRangeParserListener ) ((VersionRangeParserListener)listener).exitRange(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof VersionRangeParserVisitor ) return ((VersionRangeParserVisitor<? extends T>)visitor).visitRange(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RangeContext range() throws RecognitionException {
		RangeContext _localctx = new RangeContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_range);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(23);
			_la = _input.LA(1);
			if ( !(_la==OPEN_RANGE_OPEN || _la==CLOSED_RANGE_OPEN) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(24);
			bounds();
			setState(25);
			_la = _input.LA(1);
			if ( !(_la==OPEN_RANGE_CLOSE || _la==CLOSED_RANGE_CLOSE) ) {
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

	public static class BoundsContext extends ParserRuleContext {
		public BoundedLowerContext boundedLower() {
			return getRuleContext(BoundedLowerContext.class,0);
		}
		public UnboundedLowerContext unboundedLower() {
			return getRuleContext(UnboundedLowerContext.class,0);
		}
		public BoundsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_bounds; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VersionRangeParserListener ) ((VersionRangeParserListener)listener).enterBounds(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VersionRangeParserListener ) ((VersionRangeParserListener)listener).exitBounds(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof VersionRangeParserVisitor ) return ((VersionRangeParserVisitor<? extends T>)visitor).visitBounds(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BoundsContext bounds() throws RecognitionException {
		BoundsContext _localctx = new BoundsContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_bounds);
		try {
			setState(29);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Version:
				enterOuterAlt(_localctx, 1);
				{
				setState(27);
				boundedLower();
				}
				break;
			case COMMA:
				enterOuterAlt(_localctx, 2);
				{
				setState(28);
				unboundedLower();
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

	public static class BoundedLowerContext extends ParserRuleContext {
		public List<TerminalNode> Version() { return getTokens(VersionRangeParser.Version); }
		public TerminalNode Version(int i) {
			return getToken(VersionRangeParser.Version, i);
		}
		public TerminalNode COMMA() { return getToken(VersionRangeParser.COMMA, 0); }
		public BoundedLowerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_boundedLower; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VersionRangeParserListener ) ((VersionRangeParserListener)listener).enterBoundedLower(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VersionRangeParserListener ) ((VersionRangeParserListener)listener).exitBoundedLower(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof VersionRangeParserVisitor ) return ((VersionRangeParserVisitor<? extends T>)visitor).visitBoundedLower(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BoundedLowerContext boundedLower() throws RecognitionException {
		BoundedLowerContext _localctx = new BoundedLowerContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_boundedLower);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(31);
			match(Version);
			setState(32);
			match(COMMA);
			setState(34);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==Version) {
				{
				setState(33);
				match(Version);
				}
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

	public static class UnboundedLowerContext extends ParserRuleContext {
		public TerminalNode COMMA() { return getToken(VersionRangeParser.COMMA, 0); }
		public TerminalNode Version() { return getToken(VersionRangeParser.Version, 0); }
		public UnboundedLowerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unboundedLower; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VersionRangeParserListener ) ((VersionRangeParserListener)listener).enterUnboundedLower(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VersionRangeParserListener ) ((VersionRangeParserListener)listener).exitUnboundedLower(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof VersionRangeParserVisitor ) return ((VersionRangeParserVisitor<? extends T>)visitor).visitUnboundedLower(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UnboundedLowerContext unboundedLower() throws RecognitionException {
		UnboundedLowerContext _localctx = new UnboundedLowerContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_unboundedLower);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(36);
			match(COMMA);
			setState(38);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==Version) {
				{
				setState(37);
				match(Version);
				}
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

	public static class VersionContext extends ParserRuleContext {
		public TerminalNode Version() { return getToken(VersionRangeParser.Version, 0); }
		public TerminalNode PROPERTY_OPEN() { return getToken(VersionRangeParser.PROPERTY_OPEN, 0); }
		public TerminalNode PROPERTY_CLOSE() { return getToken(VersionRangeParser.PROPERTY_CLOSE, 0); }
		public VersionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_version; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof VersionRangeParserListener ) ((VersionRangeParserListener)listener).enterVersion(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof VersionRangeParserListener ) ((VersionRangeParserListener)listener).exitVersion(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof VersionRangeParserVisitor ) return ((VersionRangeParserVisitor<? extends T>)visitor).visitVersion(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VersionContext version() throws RecognitionException {
		VersionContext _localctx = new VersionContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_version);
		try {
			setState(44);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Version:
				enterOuterAlt(_localctx, 1);
				{
				setState(40);
				match(Version);
				}
				break;
			case PROPERTY_OPEN:
				enterOuterAlt(_localctx, 2);
				{
				{
				setState(41);
				match(PROPERTY_OPEN);
				setState(42);
				match(Version);
				setState(43);
				match(PROPERTY_CLOSE);
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

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\n\61\4\2\t\2\4\3"+
		"\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\3\2\3\2\3\2\7\2\22\n\2\f\2\16\2\25"+
		"\13\2\3\2\5\2\30\n\2\3\3\3\3\3\3\3\3\3\4\3\4\5\4 \n\4\3\5\3\5\3\5\5\5"+
		"%\n\5\3\6\3\6\5\6)\n\6\3\7\3\7\3\7\3\7\5\7/\n\7\3\7\2\2\b\2\4\6\b\n\f"+
		"\2\4\4\2\6\6\b\b\4\2\7\7\t\t\2\60\2\27\3\2\2\2\4\31\3\2\2\2\6\37\3\2\2"+
		"\2\b!\3\2\2\2\n&\3\2\2\2\f.\3\2\2\2\16\23\5\4\3\2\17\20\7\3\2\2\20\22"+
		"\5\4\3\2\21\17\3\2\2\2\22\25\3\2\2\2\23\21\3\2\2\2\23\24\3\2\2\2\24\30"+
		"\3\2\2\2\25\23\3\2\2\2\26\30\5\f\7\2\27\16\3\2\2\2\27\26\3\2\2\2\30\3"+
		"\3\2\2\2\31\32\t\2\2\2\32\33\5\6\4\2\33\34\t\3\2\2\34\5\3\2\2\2\35 \5"+
		"\b\5\2\36 \5\n\6\2\37\35\3\2\2\2\37\36\3\2\2\2 \7\3\2\2\2!\"\7\n\2\2\""+
		"$\7\3\2\2#%\7\n\2\2$#\3\2\2\2$%\3\2\2\2%\t\3\2\2\2&(\7\3\2\2\')\7\n\2"+
		"\2(\'\3\2\2\2()\3\2\2\2)\13\3\2\2\2*/\7\n\2\2+,\7\4\2\2,-\7\n\2\2-/\7"+
		"\5\2\2.*\3\2\2\2.+\3\2\2\2/\r\3\2\2\2\b\23\27\37$(.";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}