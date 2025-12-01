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
// Generated from /Users/knut/git/openrewrite/rewrite/rewrite-xml/src/main/antlr/XPathLexer.g4 by ANTLR 4.13.2
package org.openrewrite.xml.internal.grammar;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class XPathLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		WS=1, SLASH=2, DOUBLE_SLASH=3, AXIS_SEP=4, LBRACKET=5, RBRACKET=6, LPAREN=7, 
		RPAREN=8, AT=9, DOTDOT=10, DOT=11, COMMA=12, EQUALS=13, NOT_EQUALS=14, 
		LTE=15, GTE=16, LT=17, GT=18, WILDCARD=19, NUMBER=20, AND=21, OR=22, STRING_LITERAL=23, 
		QNAME=24, NCNAME=25;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"WS", "SLASH", "DOUBLE_SLASH", "AXIS_SEP", "LBRACKET", "RBRACKET", "LPAREN", 
			"RPAREN", "AT", "DOTDOT", "DOT", "COMMA", "EQUALS", "NOT_EQUALS", "LTE", 
			"GTE", "LT", "GT", "WILDCARD", "NUMBER", "AND", "OR", "STRING_LITERAL", 
			"QNAME", "NCNAME", "NCNAME_CHARS", "NAME_START_CHAR", "NAME_CHAR"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, "'/'", "'//'", "'::'", "'['", "']'", "'('", "')'", "'@'", 
			"'..'", "'.'", "','", "'='", "'!='", "'<='", "'>='", "'<'", "'>'", "'*'", 
			null, "'and'", "'or'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "WS", "SLASH", "DOUBLE_SLASH", "AXIS_SEP", "LBRACKET", "RBRACKET", 
			"LPAREN", "RPAREN", "AT", "DOTDOT", "DOT", "COMMA", "EQUALS", "NOT_EQUALS", 
			"LTE", "GTE", "LT", "GT", "WILDCARD", "NUMBER", "AND", "OR", "STRING_LITERAL", 
			"QNAME", "NCNAME"
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


	public XPathLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "XPathLexer.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\u0004\u0000\u0019\u00a4\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002"+
		"\u0001\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002"+
		"\u0004\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002"+
		"\u0007\u0007\u0007\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002"+
		"\u000b\u0007\u000b\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e"+
		"\u0002\u000f\u0007\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011"+
		"\u0002\u0012\u0007\u0012\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014"+
		"\u0002\u0015\u0007\u0015\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017"+
		"\u0002\u0018\u0007\u0018\u0002\u0019\u0007\u0019\u0002\u001a\u0007\u001a"+
		"\u0002\u001b\u0007\u001b\u0001\u0000\u0004\u0000;\b\u0000\u000b\u0000"+
		"\f\u0000<\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0004"+
		"\u0001\u0004\u0001\u0005\u0001\u0005\u0001\u0006\u0001\u0006\u0001\u0007"+
		"\u0001\u0007\u0001\b\u0001\b\u0001\t\u0001\t\u0001\t\u0001\n\u0001\n\u0001"+
		"\u000b\u0001\u000b\u0001\f\u0001\f\u0001\r\u0001\r\u0001\r\u0001\u000e"+
		"\u0001\u000e\u0001\u000e\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u0010"+
		"\u0001\u0010\u0001\u0011\u0001\u0011\u0001\u0012\u0001\u0012\u0001\u0013"+
		"\u0004\u0013l\b\u0013\u000b\u0013\f\u0013m\u0001\u0013\u0001\u0013\u0004"+
		"\u0013r\b\u0013\u000b\u0013\f\u0013s\u0003\u0013v\b\u0013\u0001\u0014"+
		"\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0015\u0001\u0015\u0001\u0015"+
		"\u0001\u0016\u0001\u0016\u0005\u0016\u0081\b\u0016\n\u0016\f\u0016\u0084"+
		"\t\u0016\u0001\u0016\u0001\u0016\u0001\u0016\u0005\u0016\u0089\b\u0016"+
		"\n\u0016\f\u0016\u008c\t\u0016\u0001\u0016\u0003\u0016\u008f\b\u0016\u0001"+
		"\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0018\u0001\u0018\u0001"+
		"\u0019\u0001\u0019\u0005\u0019\u0099\b\u0019\n\u0019\f\u0019\u009c\t\u0019"+
		"\u0001\u001a\u0003\u001a\u009f\b\u001a\u0001\u001b\u0001\u001b\u0003\u001b"+
		"\u00a3\b\u001b\u0000\u0000\u001c\u0001\u0001\u0003\u0002\u0005\u0003\u0007"+
		"\u0004\t\u0005\u000b\u0006\r\u0007\u000f\b\u0011\t\u0013\n\u0015\u000b"+
		"\u0017\f\u0019\r\u001b\u000e\u001d\u000f\u001f\u0010!\u0011#\u0012%\u0013"+
		"\'\u0014)\u0015+\u0016-\u0017/\u00181\u00193\u00005\u00007\u0000\u0001"+
		"\u0000\u0006\u0003\u0000\t\n\r\r  \u0001\u000009\u0001\u0000\'\'\u0001"+
		"\u0000\"\"\u000e\u0000AZ__az\u00c0\u00d6\u00d8\u00f6\u00f8\u02ff\u0370"+
		"\u037d\u037f\u1fff\u200c\u200d\u2070\u218f\u2c00\u2fef\u3001\u8000\ud7ff"+
		"\u8000\uf900\u8000\ufdcf\u8000\ufdf0\u8000\ufffd\u0005\u0000-.09\u00b7"+
		"\u00b7\u0300\u036f\u203f\u2040\u00a9\u0000\u0001\u0001\u0000\u0000\u0000"+
		"\u0000\u0003\u0001\u0000\u0000\u0000\u0000\u0005\u0001\u0000\u0000\u0000"+
		"\u0000\u0007\u0001\u0000\u0000\u0000\u0000\t\u0001\u0000\u0000\u0000\u0000"+
		"\u000b\u0001\u0000\u0000\u0000\u0000\r\u0001\u0000\u0000\u0000\u0000\u000f"+
		"\u0001\u0000\u0000\u0000\u0000\u0011\u0001\u0000\u0000\u0000\u0000\u0013"+
		"\u0001\u0000\u0000\u0000\u0000\u0015\u0001\u0000\u0000\u0000\u0000\u0017"+
		"\u0001\u0000\u0000\u0000\u0000\u0019\u0001\u0000\u0000\u0000\u0000\u001b"+
		"\u0001\u0000\u0000\u0000\u0000\u001d\u0001\u0000\u0000\u0000\u0000\u001f"+
		"\u0001\u0000\u0000\u0000\u0000!\u0001\u0000\u0000\u0000\u0000#\u0001\u0000"+
		"\u0000\u0000\u0000%\u0001\u0000\u0000\u0000\u0000\'\u0001\u0000\u0000"+
		"\u0000\u0000)\u0001\u0000\u0000\u0000\u0000+\u0001\u0000\u0000\u0000\u0000"+
		"-\u0001\u0000\u0000\u0000\u0000/\u0001\u0000\u0000\u0000\u00001\u0001"+
		"\u0000\u0000\u0000\u0001:\u0001\u0000\u0000\u0000\u0003@\u0001\u0000\u0000"+
		"\u0000\u0005B\u0001\u0000\u0000\u0000\u0007E\u0001\u0000\u0000\u0000\t"+
		"H\u0001\u0000\u0000\u0000\u000bJ\u0001\u0000\u0000\u0000\rL\u0001\u0000"+
		"\u0000\u0000\u000fN\u0001\u0000\u0000\u0000\u0011P\u0001\u0000\u0000\u0000"+
		"\u0013R\u0001\u0000\u0000\u0000\u0015U\u0001\u0000\u0000\u0000\u0017W"+
		"\u0001\u0000\u0000\u0000\u0019Y\u0001\u0000\u0000\u0000\u001b[\u0001\u0000"+
		"\u0000\u0000\u001d^\u0001\u0000\u0000\u0000\u001fa\u0001\u0000\u0000\u0000"+
		"!d\u0001\u0000\u0000\u0000#f\u0001\u0000\u0000\u0000%h\u0001\u0000\u0000"+
		"\u0000\'k\u0001\u0000\u0000\u0000)w\u0001\u0000\u0000\u0000+{\u0001\u0000"+
		"\u0000\u0000-\u008e\u0001\u0000\u0000\u0000/\u0090\u0001\u0000\u0000\u0000"+
		"1\u0094\u0001\u0000\u0000\u00003\u0096\u0001\u0000\u0000\u00005\u009e"+
		"\u0001\u0000\u0000\u00007\u00a2\u0001\u0000\u0000\u00009;\u0007\u0000"+
		"\u0000\u0000:9\u0001\u0000\u0000\u0000;<\u0001\u0000\u0000\u0000<:\u0001"+
		"\u0000\u0000\u0000<=\u0001\u0000\u0000\u0000=>\u0001\u0000\u0000\u0000"+
		">?\u0006\u0000\u0000\u0000?\u0002\u0001\u0000\u0000\u0000@A\u0005/\u0000"+
		"\u0000A\u0004\u0001\u0000\u0000\u0000BC\u0005/\u0000\u0000CD\u0005/\u0000"+
		"\u0000D\u0006\u0001\u0000\u0000\u0000EF\u0005:\u0000\u0000FG\u0005:\u0000"+
		"\u0000G\b\u0001\u0000\u0000\u0000HI\u0005[\u0000\u0000I\n\u0001\u0000"+
		"\u0000\u0000JK\u0005]\u0000\u0000K\f\u0001\u0000\u0000\u0000LM\u0005("+
		"\u0000\u0000M\u000e\u0001\u0000\u0000\u0000NO\u0005)\u0000\u0000O\u0010"+
		"\u0001\u0000\u0000\u0000PQ\u0005@\u0000\u0000Q\u0012\u0001\u0000\u0000"+
		"\u0000RS\u0005.\u0000\u0000ST\u0005.\u0000\u0000T\u0014\u0001\u0000\u0000"+
		"\u0000UV\u0005.\u0000\u0000V\u0016\u0001\u0000\u0000\u0000WX\u0005,\u0000"+
		"\u0000X\u0018\u0001\u0000\u0000\u0000YZ\u0005=\u0000\u0000Z\u001a\u0001"+
		"\u0000\u0000\u0000[\\\u0005!\u0000\u0000\\]\u0005=\u0000\u0000]\u001c"+
		"\u0001\u0000\u0000\u0000^_\u0005<\u0000\u0000_`\u0005=\u0000\u0000`\u001e"+
		"\u0001\u0000\u0000\u0000ab\u0005>\u0000\u0000bc\u0005=\u0000\u0000c \u0001"+
		"\u0000\u0000\u0000de\u0005<\u0000\u0000e\"\u0001\u0000\u0000\u0000fg\u0005"+
		">\u0000\u0000g$\u0001\u0000\u0000\u0000hi\u0005*\u0000\u0000i&\u0001\u0000"+
		"\u0000\u0000jl\u0007\u0001\u0000\u0000kj\u0001\u0000\u0000\u0000lm\u0001"+
		"\u0000\u0000\u0000mk\u0001\u0000\u0000\u0000mn\u0001\u0000\u0000\u0000"+
		"nu\u0001\u0000\u0000\u0000oq\u0005.\u0000\u0000pr\u0007\u0001\u0000\u0000"+
		"qp\u0001\u0000\u0000\u0000rs\u0001\u0000\u0000\u0000sq\u0001\u0000\u0000"+
		"\u0000st\u0001\u0000\u0000\u0000tv\u0001\u0000\u0000\u0000uo\u0001\u0000"+
		"\u0000\u0000uv\u0001\u0000\u0000\u0000v(\u0001\u0000\u0000\u0000wx\u0005"+
		"a\u0000\u0000xy\u0005n\u0000\u0000yz\u0005d\u0000\u0000z*\u0001\u0000"+
		"\u0000\u0000{|\u0005o\u0000\u0000|}\u0005r\u0000\u0000},\u0001\u0000\u0000"+
		"\u0000~\u0082\u0005\'\u0000\u0000\u007f\u0081\b\u0002\u0000\u0000\u0080"+
		"\u007f\u0001\u0000\u0000\u0000\u0081\u0084\u0001\u0000\u0000\u0000\u0082"+
		"\u0080\u0001\u0000\u0000\u0000\u0082\u0083\u0001\u0000\u0000\u0000\u0083"+
		"\u0085\u0001\u0000\u0000\u0000\u0084\u0082\u0001\u0000\u0000\u0000\u0085"+
		"\u008f\u0005\'\u0000\u0000\u0086\u008a\u0005\"\u0000\u0000\u0087\u0089"+
		"\b\u0003\u0000\u0000\u0088\u0087\u0001\u0000\u0000\u0000\u0089\u008c\u0001"+
		"\u0000\u0000\u0000\u008a\u0088\u0001\u0000\u0000\u0000\u008a\u008b\u0001"+
		"\u0000\u0000\u0000\u008b\u008d\u0001\u0000\u0000\u0000\u008c\u008a\u0001"+
		"\u0000\u0000\u0000\u008d\u008f\u0005\"\u0000\u0000\u008e~\u0001\u0000"+
		"\u0000\u0000\u008e\u0086\u0001\u0000\u0000\u0000\u008f.\u0001\u0000\u0000"+
		"\u0000\u0090\u0091\u00033\u0019\u0000\u0091\u0092\u0005:\u0000\u0000\u0092"+
		"\u0093\u00033\u0019\u0000\u00930\u0001\u0000\u0000\u0000\u0094\u0095\u0003"+
		"3\u0019\u0000\u00952\u0001\u0000\u0000\u0000\u0096\u009a\u00035\u001a"+
		"\u0000\u0097\u0099\u00037\u001b\u0000\u0098\u0097\u0001\u0000\u0000\u0000"+
		"\u0099\u009c\u0001\u0000\u0000\u0000\u009a\u0098\u0001\u0000\u0000\u0000"+
		"\u009a\u009b\u0001\u0000\u0000\u0000\u009b4\u0001\u0000\u0000\u0000\u009c"+
		"\u009a\u0001\u0000\u0000\u0000\u009d\u009f\u0007\u0004\u0000\u0000\u009e"+
		"\u009d\u0001\u0000\u0000\u0000\u009f6\u0001\u0000\u0000\u0000\u00a0\u00a3"+
		"\u00035\u001a\u0000\u00a1\u00a3\u0007\u0005\u0000\u0000\u00a2\u00a0\u0001"+
		"\u0000\u0000\u0000\u00a2\u00a1\u0001\u0000\u0000\u0000\u00a38\u0001\u0000"+
		"\u0000\u0000\u000b\u0000<msu\u0082\u008a\u008e\u009a\u009e\u00a2\u0001"+
		"\u0006\u0000\u0000";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}