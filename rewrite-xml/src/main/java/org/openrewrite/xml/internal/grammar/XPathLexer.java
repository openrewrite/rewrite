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
		WS=1, SLASH=2, DOUBLE_SLASH=3, LBRACKET=4, RBRACKET=5, LPAREN=6, RPAREN=7, 
		AT=8, DOT=9, COMMA=10, EQUALS=11, NOT_EQUALS=12, LT=13, GT=14, LTE=15, 
		GTE=16, WILDCARD=17, NUMBER=18, AND=19, OR=20, LOCAL_NAME=21, NAMESPACE_URI=22, 
		STRING_LITERAL=23, QNAME=24;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"WS", "SLASH", "DOUBLE_SLASH", "LBRACKET", "RBRACKET", "LPAREN", "RPAREN", 
			"AT", "DOT", "COMMA", "EQUALS", "NOT_EQUALS", "LT", "GT", "LTE", "GTE", 
			"WILDCARD", "NUMBER", "AND", "OR", "LOCAL_NAME", "NAMESPACE_URI", "STRING_LITERAL", 
			"QNAME", "NCNAME", "NAME_START_CHAR", "NAME_CHAR"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, "'/'", "'//'", "'['", "']'", "'('", "')'", "'@'", "'.'", 
			"','", "'='", "'!='", "'<'", "'>'", "'<='", "'>='", "'*'", null, "'and'", 
			"'or'", "'local-name'", "'namespace-uri'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "WS", "SLASH", "DOUBLE_SLASH", "LBRACKET", "RBRACKET", "LPAREN", 
			"RPAREN", "AT", "DOT", "COMMA", "EQUALS", "NOT_EQUALS", "LT", "GT", "LTE", 
			"GTE", "WILDCARD", "NUMBER", "AND", "OR", "LOCAL_NAME", "NAMESPACE_URI", 
			"STRING_LITERAL", "QNAME"
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
		"\u0004\u0000\u0018\u00b4\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002"+
		"\u0001\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002"+
		"\u0004\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002"+
		"\u0007\u0007\u0007\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002"+
		"\u000b\u0007\u000b\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e"+
		"\u0002\u000f\u0007\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011"+
		"\u0002\u0012\u0007\u0012\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014"+
		"\u0002\u0015\u0007\u0015\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017"+
		"\u0002\u0018\u0007\u0018\u0002\u0019\u0007\u0019\u0002\u001a\u0007\u001a"+
		"\u0001\u0000\u0004\u00009\b\u0000\u000b\u0000\f\u0000:\u0001\u0000\u0001"+
		"\u0000\u0001\u0001\u0001\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0001"+
		"\u0003\u0001\u0003\u0001\u0004\u0001\u0004\u0001\u0005\u0001\u0005\u0001"+
		"\u0006\u0001\u0006\u0001\u0007\u0001\u0007\u0001\b\u0001\b\u0001\t\u0001"+
		"\t\u0001\n\u0001\n\u0001\u000b\u0001\u000b\u0001\u000b\u0001\f\u0001\f"+
		"\u0001\r\u0001\r\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000f\u0001"+
		"\u000f\u0001\u000f\u0001\u0010\u0001\u0010\u0001\u0011\u0004\u0011d\b"+
		"\u0011\u000b\u0011\f\u0011e\u0001\u0011\u0001\u0011\u0004\u0011j\b\u0011"+
		"\u000b\u0011\f\u0011k\u0003\u0011n\b\u0011\u0001\u0012\u0001\u0012\u0001"+
		"\u0012\u0001\u0012\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0014\u0001"+
		"\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001"+
		"\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0015\u0001\u0015\u0001"+
		"\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0001"+
		"\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0001"+
		"\u0016\u0001\u0016\u0005\u0016\u0092\b\u0016\n\u0016\f\u0016\u0095\t\u0016"+
		"\u0001\u0016\u0001\u0016\u0001\u0016\u0005\u0016\u009a\b\u0016\n\u0016"+
		"\f\u0016\u009d\t\u0016\u0001\u0016\u0003\u0016\u00a0\b\u0016\u0001\u0017"+
		"\u0001\u0017\u0001\u0017\u0003\u0017\u00a5\b\u0017\u0001\u0018\u0001\u0018"+
		"\u0005\u0018\u00a9\b\u0018\n\u0018\f\u0018\u00ac\t\u0018\u0001\u0019\u0003"+
		"\u0019\u00af\b\u0019\u0001\u001a\u0001\u001a\u0003\u001a\u00b3\b\u001a"+
		"\u0000\u0000\u001b\u0001\u0001\u0003\u0002\u0005\u0003\u0007\u0004\t\u0005"+
		"\u000b\u0006\r\u0007\u000f\b\u0011\t\u0013\n\u0015\u000b\u0017\f\u0019"+
		"\r\u001b\u000e\u001d\u000f\u001f\u0010!\u0011#\u0012%\u0013\'\u0014)\u0015"+
		"+\u0016-\u0017/\u00181\u00003\u00005\u0000\u0001\u0000\u0006\u0003\u0000"+
		"\t\n\r\r  \u0001\u000009\u0001\u0000\'\'\u0001\u0000\"\"\u000e\u0000A"+
		"Z__az\u00c0\u00d6\u00d8\u00f6\u00f8\u02ff\u0370\u037d\u037f\u1fff\u200c"+
		"\u200d\u2070\u218f\u2c00\u2fef\u3001\u8000\ud7ff\u8000\uf900\u8000\ufdcf"+
		"\u8000\ufdf0\u8000\ufffd\u0005\u0000-.09\u00b7\u00b7\u0300\u036f\u203f"+
		"\u2040\u00ba\u0000\u0001\u0001\u0000\u0000\u0000\u0000\u0003\u0001\u0000"+
		"\u0000\u0000\u0000\u0005\u0001\u0000\u0000\u0000\u0000\u0007\u0001\u0000"+
		"\u0000\u0000\u0000\t\u0001\u0000\u0000\u0000\u0000\u000b\u0001\u0000\u0000"+
		"\u0000\u0000\r\u0001\u0000\u0000\u0000\u0000\u000f\u0001\u0000\u0000\u0000"+
		"\u0000\u0011\u0001\u0000\u0000\u0000\u0000\u0013\u0001\u0000\u0000\u0000"+
		"\u0000\u0015\u0001\u0000\u0000\u0000\u0000\u0017\u0001\u0000\u0000\u0000"+
		"\u0000\u0019\u0001\u0000\u0000\u0000\u0000\u001b\u0001\u0000\u0000\u0000"+
		"\u0000\u001d\u0001\u0000\u0000\u0000\u0000\u001f\u0001\u0000\u0000\u0000"+
		"\u0000!\u0001\u0000\u0000\u0000\u0000#\u0001\u0000\u0000\u0000\u0000%"+
		"\u0001\u0000\u0000\u0000\u0000\'\u0001\u0000\u0000\u0000\u0000)\u0001"+
		"\u0000\u0000\u0000\u0000+\u0001\u0000\u0000\u0000\u0000-\u0001\u0000\u0000"+
		"\u0000\u0000/\u0001\u0000\u0000\u0000\u00018\u0001\u0000\u0000\u0000\u0003"+
		">\u0001\u0000\u0000\u0000\u0005@\u0001\u0000\u0000\u0000\u0007C\u0001"+
		"\u0000\u0000\u0000\tE\u0001\u0000\u0000\u0000\u000bG\u0001\u0000\u0000"+
		"\u0000\rI\u0001\u0000\u0000\u0000\u000fK\u0001\u0000\u0000\u0000\u0011"+
		"M\u0001\u0000\u0000\u0000\u0013O\u0001\u0000\u0000\u0000\u0015Q\u0001"+
		"\u0000\u0000\u0000\u0017S\u0001\u0000\u0000\u0000\u0019V\u0001\u0000\u0000"+
		"\u0000\u001bX\u0001\u0000\u0000\u0000\u001dZ\u0001\u0000\u0000\u0000\u001f"+
		"]\u0001\u0000\u0000\u0000!`\u0001\u0000\u0000\u0000#c\u0001\u0000\u0000"+
		"\u0000%o\u0001\u0000\u0000\u0000\'s\u0001\u0000\u0000\u0000)v\u0001\u0000"+
		"\u0000\u0000+\u0081\u0001\u0000\u0000\u0000-\u009f\u0001\u0000\u0000\u0000"+
		"/\u00a1\u0001\u0000\u0000\u00001\u00a6\u0001\u0000\u0000\u00003\u00ae"+
		"\u0001\u0000\u0000\u00005\u00b2\u0001\u0000\u0000\u000079\u0007\u0000"+
		"\u0000\u000087\u0001\u0000\u0000\u00009:\u0001\u0000\u0000\u0000:8\u0001"+
		"\u0000\u0000\u0000:;\u0001\u0000\u0000\u0000;<\u0001\u0000\u0000\u0000"+
		"<=\u0006\u0000\u0000\u0000=\u0002\u0001\u0000\u0000\u0000>?\u0005/\u0000"+
		"\u0000?\u0004\u0001\u0000\u0000\u0000@A\u0005/\u0000\u0000AB\u0005/\u0000"+
		"\u0000B\u0006\u0001\u0000\u0000\u0000CD\u0005[\u0000\u0000D\b\u0001\u0000"+
		"\u0000\u0000EF\u0005]\u0000\u0000F\n\u0001\u0000\u0000\u0000GH\u0005("+
		"\u0000\u0000H\f\u0001\u0000\u0000\u0000IJ\u0005)\u0000\u0000J\u000e\u0001"+
		"\u0000\u0000\u0000KL\u0005@\u0000\u0000L\u0010\u0001\u0000\u0000\u0000"+
		"MN\u0005.\u0000\u0000N\u0012\u0001\u0000\u0000\u0000OP\u0005,\u0000\u0000"+
		"P\u0014\u0001\u0000\u0000\u0000QR\u0005=\u0000\u0000R\u0016\u0001\u0000"+
		"\u0000\u0000ST\u0005!\u0000\u0000TU\u0005=\u0000\u0000U\u0018\u0001\u0000"+
		"\u0000\u0000VW\u0005<\u0000\u0000W\u001a\u0001\u0000\u0000\u0000XY\u0005"+
		">\u0000\u0000Y\u001c\u0001\u0000\u0000\u0000Z[\u0005<\u0000\u0000[\\\u0005"+
		"=\u0000\u0000\\\u001e\u0001\u0000\u0000\u0000]^\u0005>\u0000\u0000^_\u0005"+
		"=\u0000\u0000_ \u0001\u0000\u0000\u0000`a\u0005*\u0000\u0000a\"\u0001"+
		"\u0000\u0000\u0000bd\u0007\u0001\u0000\u0000cb\u0001\u0000\u0000\u0000"+
		"de\u0001\u0000\u0000\u0000ec\u0001\u0000\u0000\u0000ef\u0001\u0000\u0000"+
		"\u0000fm\u0001\u0000\u0000\u0000gi\u0005.\u0000\u0000hj\u0007\u0001\u0000"+
		"\u0000ih\u0001\u0000\u0000\u0000jk\u0001\u0000\u0000\u0000ki\u0001\u0000"+
		"\u0000\u0000kl\u0001\u0000\u0000\u0000ln\u0001\u0000\u0000\u0000mg\u0001"+
		"\u0000\u0000\u0000mn\u0001\u0000\u0000\u0000n$\u0001\u0000\u0000\u0000"+
		"op\u0005a\u0000\u0000pq\u0005n\u0000\u0000qr\u0005d\u0000\u0000r&\u0001"+
		"\u0000\u0000\u0000st\u0005o\u0000\u0000tu\u0005r\u0000\u0000u(\u0001\u0000"+
		"\u0000\u0000vw\u0005l\u0000\u0000wx\u0005o\u0000\u0000xy\u0005c\u0000"+
		"\u0000yz\u0005a\u0000\u0000z{\u0005l\u0000\u0000{|\u0005-\u0000\u0000"+
		"|}\u0005n\u0000\u0000}~\u0005a\u0000\u0000~\u007f\u0005m\u0000\u0000\u007f"+
		"\u0080\u0005e\u0000\u0000\u0080*\u0001\u0000\u0000\u0000\u0081\u0082\u0005"+
		"n\u0000\u0000\u0082\u0083\u0005a\u0000\u0000\u0083\u0084\u0005m\u0000"+
		"\u0000\u0084\u0085\u0005e\u0000\u0000\u0085\u0086\u0005s\u0000\u0000\u0086"+
		"\u0087\u0005p\u0000\u0000\u0087\u0088\u0005a\u0000\u0000\u0088\u0089\u0005"+
		"c\u0000\u0000\u0089\u008a\u0005e\u0000\u0000\u008a\u008b\u0005-\u0000"+
		"\u0000\u008b\u008c\u0005u\u0000\u0000\u008c\u008d\u0005r\u0000\u0000\u008d"+
		"\u008e\u0005i\u0000\u0000\u008e,\u0001\u0000\u0000\u0000\u008f\u0093\u0005"+
		"\'\u0000\u0000\u0090\u0092\b\u0002\u0000\u0000\u0091\u0090\u0001\u0000"+
		"\u0000\u0000\u0092\u0095\u0001\u0000\u0000\u0000\u0093\u0091\u0001\u0000"+
		"\u0000\u0000\u0093\u0094\u0001\u0000\u0000\u0000\u0094\u0096\u0001\u0000"+
		"\u0000\u0000\u0095\u0093\u0001\u0000\u0000\u0000\u0096\u00a0\u0005\'\u0000"+
		"\u0000\u0097\u009b\u0005\"\u0000\u0000\u0098\u009a\b\u0003\u0000\u0000"+
		"\u0099\u0098\u0001\u0000\u0000\u0000\u009a\u009d\u0001\u0000\u0000\u0000"+
		"\u009b\u0099\u0001\u0000\u0000\u0000\u009b\u009c\u0001\u0000\u0000\u0000"+
		"\u009c\u009e\u0001\u0000\u0000\u0000\u009d\u009b\u0001\u0000\u0000\u0000"+
		"\u009e\u00a0\u0005\"\u0000\u0000\u009f\u008f\u0001\u0000\u0000\u0000\u009f"+
		"\u0097\u0001\u0000\u0000\u0000\u00a0.\u0001\u0000\u0000\u0000\u00a1\u00a4"+
		"\u00031\u0018\u0000\u00a2\u00a3\u0005:\u0000\u0000\u00a3\u00a5\u00031"+
		"\u0018\u0000\u00a4\u00a2\u0001\u0000\u0000\u0000\u00a4\u00a5\u0001\u0000"+
		"\u0000\u0000\u00a50\u0001\u0000\u0000\u0000\u00a6\u00aa\u00033\u0019\u0000"+
		"\u00a7\u00a9\u00035\u001a\u0000\u00a8\u00a7\u0001\u0000\u0000\u0000\u00a9"+
		"\u00ac\u0001\u0000\u0000\u0000\u00aa\u00a8\u0001\u0000\u0000\u0000\u00aa"+
		"\u00ab\u0001\u0000\u0000\u0000\u00ab2\u0001\u0000\u0000\u0000\u00ac\u00aa"+
		"\u0001\u0000\u0000\u0000\u00ad\u00af\u0007\u0004\u0000\u0000\u00ae\u00ad"+
		"\u0001\u0000\u0000\u0000\u00af4\u0001\u0000\u0000\u0000\u00b0\u00b3\u0003"+
		"3\u0019\u0000\u00b1\u00b3\u0007\u0005\u0000\u0000\u00b2\u00b0\u0001\u0000"+
		"\u0000\u0000\u00b2\u00b1\u0001\u0000\u0000\u0000\u00b36\u0001\u0000\u0000"+
		"\u0000\f\u0000:ekm\u0093\u009b\u009f\u00a4\u00aa\u00ae\u00b2\u0001\u0006"+
		"\u0000\u0000";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}