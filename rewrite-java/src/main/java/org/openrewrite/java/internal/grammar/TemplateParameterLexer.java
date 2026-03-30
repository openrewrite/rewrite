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
// Generated from /Users/jon/Projects/github/openrewrite/rewrite/rewrite-java/src/main/antlr/TemplateParameterLexer.g4 by ANTLR 4.13.2
package org.openrewrite.java.internal.grammar;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class TemplateParameterLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		LPAREN=1, RPAREN=2, DOT=3, COLON=4, COMMA=5, LBRACK=6, RBRACK=7, WILDCARD=8, 
		LSBRACK=9, RSBRACK=10, AND=11, Extends=12, Super=13, FullyQualifiedName=14, 
		Number=15, Identifier=16, S=17;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"LPAREN", "RPAREN", "DOT", "COLON", "COMMA", "LBRACK", "RBRACK", "WILDCARD", 
			"LSBRACK", "RSBRACK", "AND", "Extends", "Super", "FullyQualifiedName", 
			"Number", "Identifier", "JavaLetter", "JavaLetterOrDigit", "S"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'('", "')'", "'.'", "':'", "','", "'<'", "'>'", "'?'", "'['", 
			"']'", "'&'", "'extends'", "'super'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "LPAREN", "RPAREN", "DOT", "COLON", "COMMA", "LBRACK", "RBRACK", 
			"WILDCARD", "LSBRACK", "RSBRACK", "AND", "Extends", "Super", "FullyQualifiedName", 
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


	public TemplateParameterLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "TemplateParameterLexer.g4"; }

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

	@Override
	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 16:
			return JavaLetter_sempred((RuleContext)_localctx, predIndex);
		case 17:
			return JavaLetterOrDigit_sempred((RuleContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean JavaLetter_sempred(RuleContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return Character.isJavaIdentifierStart(_input.LA(-1));
		case 1:
			return Character.isJavaIdentifierStart(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)));
		}
		return true;
	}
	private boolean JavaLetterOrDigit_sempred(RuleContext _localctx, int predIndex) {
		switch (predIndex) {
		case 2:
			return Character.isJavaIdentifierPart(_input.LA(-1));
		case 3:
			return Character.isJavaIdentifierPart(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)));
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u0000\u0011\u00a7\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002"+
		"\u0001\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002"+
		"\u0004\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002"+
		"\u0007\u0007\u0007\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002"+
		"\u000b\u0007\u000b\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e"+
		"\u0002\u000f\u0007\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011"+
		"\u0002\u0012\u0007\u0012\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001"+
		"\u0001\u0002\u0001\u0002\u0001\u0003\u0001\u0003\u0001\u0004\u0001\u0004"+
		"\u0001\u0005\u0001\u0005\u0001\u0006\u0001\u0006\u0001\u0007\u0001\u0007"+
		"\u0001\b\u0001\b\u0001\t\u0001\t\u0001\n\u0001\n\u0001\u000b\u0001\u000b"+
		"\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b"+
		"\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\r\u0001\r\u0001"+
		"\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001"+
		"\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001"+
		"\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001"+
		"\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001"+
		"\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001"+
		"\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0004\r\u0082\b\r\u000b"+
		"\r\f\r\u0083\u0003\r\u0086\b\r\u0001\u000e\u0004\u000e\u0089\b\u000e\u000b"+
		"\u000e\f\u000e\u008a\u0001\u000f\u0001\u000f\u0005\u000f\u008f\b\u000f"+
		"\n\u000f\f\u000f\u0092\t\u000f\u0001\u0010\u0001\u0010\u0001\u0010\u0001"+
		"\u0010\u0001\u0010\u0001\u0010\u0003\u0010\u009a\b\u0010\u0001\u0011\u0001"+
		"\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0003\u0011\u00a2"+
		"\b\u0011\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0000\u0000\u0013"+
		"\u0001\u0001\u0003\u0002\u0005\u0003\u0007\u0004\t\u0005\u000b\u0006\r"+
		"\u0007\u000f\b\u0011\t\u0013\n\u0015\u000b\u0017\f\u0019\r\u001b\u000e"+
		"\u001d\u000f\u001f\u0010!\u0000#\u0000%\u0011\u0001\u0000\u0007\u0001"+
		"\u000009\u0004\u0000$$AZ__az\u0002\u0000\u0000\u007f\u8000\ud800\u8000"+
		"\udbff\u0001\u0000\u8000\ud800\u8000\udbff\u0001\u0000\u8000\udc00\u8000"+
		"\udfff\u0005\u0000$$09AZ__az\u0003\u0000\t\n\r\r  \u00b5\u0000\u0001\u0001"+
		"\u0000\u0000\u0000\u0000\u0003\u0001\u0000\u0000\u0000\u0000\u0005\u0001"+
		"\u0000\u0000\u0000\u0000\u0007\u0001\u0000\u0000\u0000\u0000\t\u0001\u0000"+
		"\u0000\u0000\u0000\u000b\u0001\u0000\u0000\u0000\u0000\r\u0001\u0000\u0000"+
		"\u0000\u0000\u000f\u0001\u0000\u0000\u0000\u0000\u0011\u0001\u0000\u0000"+
		"\u0000\u0000\u0013\u0001\u0000\u0000\u0000\u0000\u0015\u0001\u0000\u0000"+
		"\u0000\u0000\u0017\u0001\u0000\u0000\u0000\u0000\u0019\u0001\u0000\u0000"+
		"\u0000\u0000\u001b\u0001\u0000\u0000\u0000\u0000\u001d\u0001\u0000\u0000"+
		"\u0000\u0000\u001f\u0001\u0000\u0000\u0000\u0000%\u0001\u0000\u0000\u0000"+
		"\u0001\'\u0001\u0000\u0000\u0000\u0003)\u0001\u0000\u0000\u0000\u0005"+
		"+\u0001\u0000\u0000\u0000\u0007-\u0001\u0000\u0000\u0000\t/\u0001\u0000"+
		"\u0000\u0000\u000b1\u0001\u0000\u0000\u0000\r3\u0001\u0000\u0000\u0000"+
		"\u000f5\u0001\u0000\u0000\u0000\u00117\u0001\u0000\u0000\u0000\u00139"+
		"\u0001\u0000\u0000\u0000\u0015;\u0001\u0000\u0000\u0000\u0017=\u0001\u0000"+
		"\u0000\u0000\u0019E\u0001\u0000\u0000\u0000\u001b\u0085\u0001\u0000\u0000"+
		"\u0000\u001d\u0088\u0001\u0000\u0000\u0000\u001f\u008c\u0001\u0000\u0000"+
		"\u0000!\u0099\u0001\u0000\u0000\u0000#\u00a1\u0001\u0000\u0000\u0000%"+
		"\u00a3\u0001\u0000\u0000\u0000\'(\u0005(\u0000\u0000(\u0002\u0001\u0000"+
		"\u0000\u0000)*\u0005)\u0000\u0000*\u0004\u0001\u0000\u0000\u0000+,\u0005"+
		".\u0000\u0000,\u0006\u0001\u0000\u0000\u0000-.\u0005:\u0000\u0000.\b\u0001"+
		"\u0000\u0000\u0000/0\u0005,\u0000\u00000\n\u0001\u0000\u0000\u000012\u0005"+
		"<\u0000\u00002\f\u0001\u0000\u0000\u000034\u0005>\u0000\u00004\u000e\u0001"+
		"\u0000\u0000\u000056\u0005?\u0000\u00006\u0010\u0001\u0000\u0000\u0000"+
		"78\u0005[\u0000\u00008\u0012\u0001\u0000\u0000\u00009:\u0005]\u0000\u0000"+
		":\u0014\u0001\u0000\u0000\u0000;<\u0005&\u0000\u0000<\u0016\u0001\u0000"+
		"\u0000\u0000=>\u0005e\u0000\u0000>?\u0005x\u0000\u0000?@\u0005t\u0000"+
		"\u0000@A\u0005e\u0000\u0000AB\u0005n\u0000\u0000BC\u0005d\u0000\u0000"+
		"CD\u0005s\u0000\u0000D\u0018\u0001\u0000\u0000\u0000EF\u0005s\u0000\u0000"+
		"FG\u0005u\u0000\u0000GH\u0005p\u0000\u0000HI\u0005e\u0000\u0000IJ\u0005"+
		"r\u0000\u0000J\u001a\u0001\u0000\u0000\u0000KL\u0005b\u0000\u0000LM\u0005"+
		"o\u0000\u0000MN\u0005o\u0000\u0000NO\u0005l\u0000\u0000OP\u0005e\u0000"+
		"\u0000PQ\u0005a\u0000\u0000Q\u0086\u0005n\u0000\u0000RS\u0005b\u0000\u0000"+
		"ST\u0005y\u0000\u0000TU\u0005t\u0000\u0000U\u0086\u0005e\u0000\u0000V"+
		"W\u0005c\u0000\u0000WX\u0005h\u0000\u0000XY\u0005a\u0000\u0000Y\u0086"+
		"\u0005r\u0000\u0000Z[\u0005d\u0000\u0000[\\\u0005o\u0000\u0000\\]\u0005"+
		"u\u0000\u0000]^\u0005b\u0000\u0000^_\u0005l\u0000\u0000_\u0086\u0005e"+
		"\u0000\u0000`a\u0005f\u0000\u0000ab\u0005l\u0000\u0000bc\u0005o\u0000"+
		"\u0000cd\u0005a\u0000\u0000d\u0086\u0005t\u0000\u0000ef\u0005i\u0000\u0000"+
		"fg\u0005n\u0000\u0000g\u0086\u0005t\u0000\u0000hi\u0005l\u0000\u0000i"+
		"j\u0005o\u0000\u0000jk\u0005n\u0000\u0000k\u0086\u0005g\u0000\u0000lm"+
		"\u0005s\u0000\u0000mn\u0005h\u0000\u0000no\u0005o\u0000\u0000op\u0005"+
		"r\u0000\u0000p\u0086\u0005t\u0000\u0000qr\u0005S\u0000\u0000rs\u0005t"+
		"\u0000\u0000st\u0005r\u0000\u0000tu\u0005i\u0000\u0000uv\u0005n\u0000"+
		"\u0000v\u0086\u0005g\u0000\u0000wx\u0005O\u0000\u0000xy\u0005b\u0000\u0000"+
		"yz\u0005j\u0000\u0000z{\u0005e\u0000\u0000{|\u0005c\u0000\u0000|\u0086"+
		"\u0005t\u0000\u0000}\u0081\u0003\u001f\u000f\u0000~\u007f\u0003\u0005"+
		"\u0002\u0000\u007f\u0080\u0003\u001f\u000f\u0000\u0080\u0082\u0001\u0000"+
		"\u0000\u0000\u0081~\u0001\u0000\u0000\u0000\u0082\u0083\u0001\u0000\u0000"+
		"\u0000\u0083\u0081\u0001\u0000\u0000\u0000\u0083\u0084\u0001\u0000\u0000"+
		"\u0000\u0084\u0086\u0001\u0000\u0000\u0000\u0085K\u0001\u0000\u0000\u0000"+
		"\u0085R\u0001\u0000\u0000\u0000\u0085V\u0001\u0000\u0000\u0000\u0085Z"+
		"\u0001\u0000\u0000\u0000\u0085`\u0001\u0000\u0000\u0000\u0085e\u0001\u0000"+
		"\u0000\u0000\u0085h\u0001\u0000\u0000\u0000\u0085l\u0001\u0000\u0000\u0000"+
		"\u0085q\u0001\u0000\u0000\u0000\u0085w\u0001\u0000\u0000\u0000\u0085}"+
		"\u0001\u0000\u0000\u0000\u0086\u001c\u0001\u0000\u0000\u0000\u0087\u0089"+
		"\u0007\u0000\u0000\u0000\u0088\u0087\u0001\u0000\u0000\u0000\u0089\u008a"+
		"\u0001\u0000\u0000\u0000\u008a\u0088\u0001\u0000\u0000\u0000\u008a\u008b"+
		"\u0001\u0000\u0000\u0000\u008b\u001e\u0001\u0000\u0000\u0000\u008c\u0090"+
		"\u0003!\u0010\u0000\u008d\u008f\u0003#\u0011\u0000\u008e\u008d\u0001\u0000"+
		"\u0000\u0000\u008f\u0092\u0001\u0000\u0000\u0000\u0090\u008e\u0001\u0000"+
		"\u0000\u0000\u0090\u0091\u0001\u0000\u0000\u0000\u0091 \u0001\u0000\u0000"+
		"\u0000\u0092\u0090\u0001\u0000\u0000\u0000\u0093\u009a\u0007\u0001\u0000"+
		"\u0000\u0094\u0095\b\u0002\u0000\u0000\u0095\u009a\u0004\u0010\u0000\u0000"+
		"\u0096\u0097\u0007\u0003\u0000\u0000\u0097\u0098\u0007\u0004\u0000\u0000"+
		"\u0098\u009a\u0004\u0010\u0001\u0000\u0099\u0093\u0001\u0000\u0000\u0000"+
		"\u0099\u0094\u0001\u0000\u0000\u0000\u0099\u0096\u0001\u0000\u0000\u0000"+
		"\u009a\"\u0001\u0000\u0000\u0000\u009b\u00a2\u0007\u0005\u0000\u0000\u009c"+
		"\u009d\b\u0002\u0000\u0000\u009d\u00a2\u0004\u0011\u0002\u0000\u009e\u009f"+
		"\u0007\u0003\u0000\u0000\u009f\u00a0\u0007\u0004\u0000\u0000\u00a0\u00a2"+
		"\u0004\u0011\u0003\u0000\u00a1\u009b\u0001\u0000\u0000\u0000\u00a1\u009c"+
		"\u0001\u0000\u0000\u0000\u00a1\u009e\u0001\u0000\u0000\u0000\u00a2$\u0001"+
		"\u0000\u0000\u0000\u00a3\u00a4\u0007\u0006\u0000\u0000\u00a4\u00a5\u0001"+
		"\u0000\u0000\u0000\u00a5\u00a6\u0006\u0012\u0000\u0000\u00a6&\u0001\u0000"+
		"\u0000\u0000\u0007\u0000\u0083\u0085\u008a\u0090\u0099\u00a1\u0001\u0006"+
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