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
import org.antlr.v4.runtime.atn.LexerATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class MethodSignatureLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.11.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		CONSTRUCTOR=1, LPAREN=2, RPAREN=3, LBRACK=4, RBRACK=5, COMMA=6, DOT=7, 
		BANG=8, WILDCARD=9, AND=10, OR=11, ELLIPSIS=12, DOTDOT=13, POUND=14, SPACE=15, 
		Identifier=16;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"CONSTRUCTOR", "LPAREN", "RPAREN", "LBRACK", "RBRACK", "COMMA", "DOT", 
			"BANG", "WILDCARD", "AND", "OR", "ELLIPSIS", "DOTDOT", "POUND", "SPACE", 
			"Identifier", "JavaLetter", "JavaLetterOrDigit"
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


	public MethodSignatureLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "MethodSignatureLexer.g4"; }

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
		"\u0004\u0000\u0010k\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002\u0001"+
		"\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004"+
		"\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007"+
		"\u0007\u0007\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b"+
		"\u0007\u000b\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002"+
		"\u000f\u0007\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0002\u0001\u0002\u0001"+
		"\u0003\u0001\u0003\u0001\u0004\u0001\u0004\u0001\u0005\u0001\u0005\u0001"+
		"\u0006\u0001\u0006\u0001\u0007\u0001\u0007\u0001\b\u0001\b\u0001\t\u0001"+
		"\t\u0001\t\u0001\n\u0001\n\u0001\n\u0001\u000b\u0001\u000b\u0001\u000b"+
		"\u0001\u000b\u0001\f\u0001\f\u0001\f\u0001\r\u0001\r\u0001\u000e\u0001"+
		"\u000e\u0001\u000f\u0001\u000f\u0005\u000fW\b\u000f\n\u000f\f\u000fZ\t"+
		"\u000f\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001"+
		"\u0010\u0003\u0010b\b\u0010\u0001\u0011\u0001\u0011\u0001\u0011\u0001"+
		"\u0011\u0001\u0011\u0001\u0011\u0003\u0011j\b\u0011\u0000\u0000\u0012"+
		"\u0001\u0001\u0003\u0002\u0005\u0003\u0007\u0004\t\u0005\u000b\u0006\r"+
		"\u0007\u000f\b\u0011\t\u0013\n\u0015\u000b\u0017\f\u0019\r\u001b\u000e"+
		"\u001d\u000f\u001f\u0010!\u0000#\u0000\u0001\u0000\u0005\u0004\u0000$"+
		"$AZ__az\u0002\u0000\u0000\u007f\u8000\ud800\u8000\udbff\u0001\u0000\u8000"+
		"\ud800\u8000\udbff\u0001\u0000\u8000\udc00\u8000\udfff\u0005\u0000$$0"+
		"9AZ__azm\u0000\u0001\u0001\u0000\u0000\u0000\u0000\u0003\u0001\u0000\u0000"+
		"\u0000\u0000\u0005\u0001\u0000\u0000\u0000\u0000\u0007\u0001\u0000\u0000"+
		"\u0000\u0000\t\u0001\u0000\u0000\u0000\u0000\u000b\u0001\u0000\u0000\u0000"+
		"\u0000\r\u0001\u0000\u0000\u0000\u0000\u000f\u0001\u0000\u0000\u0000\u0000"+
		"\u0011\u0001\u0000\u0000\u0000\u0000\u0013\u0001\u0000\u0000\u0000\u0000"+
		"\u0015\u0001\u0000\u0000\u0000\u0000\u0017\u0001\u0000\u0000\u0000\u0000"+
		"\u0019\u0001\u0000\u0000\u0000\u0000\u001b\u0001\u0000\u0000\u0000\u0000"+
		"\u001d\u0001\u0000\u0000\u0000\u0000\u001f\u0001\u0000\u0000\u0000\u0001"+
		"%\u0001\u0000\u0000\u0000\u00033\u0001\u0000\u0000\u0000\u00055\u0001"+
		"\u0000\u0000\u0000\u00077\u0001\u0000\u0000\u0000\t9\u0001\u0000\u0000"+
		"\u0000\u000b;\u0001\u0000\u0000\u0000\r=\u0001\u0000\u0000\u0000\u000f"+
		"?\u0001\u0000\u0000\u0000\u0011A\u0001\u0000\u0000\u0000\u0013C\u0001"+
		"\u0000\u0000\u0000\u0015F\u0001\u0000\u0000\u0000\u0017I\u0001\u0000\u0000"+
		"\u0000\u0019M\u0001\u0000\u0000\u0000\u001bP\u0001\u0000\u0000\u0000\u001d"+
		"R\u0001\u0000\u0000\u0000\u001fT\u0001\u0000\u0000\u0000!a\u0001\u0000"+
		"\u0000\u0000#i\u0001\u0000\u0000\u0000%&\u0005<\u0000\u0000&\'\u0005c"+
		"\u0000\u0000\'(\u0005o\u0000\u0000()\u0005n\u0000\u0000)*\u0005s\u0000"+
		"\u0000*+\u0005t\u0000\u0000+,\u0005r\u0000\u0000,-\u0005u\u0000\u0000"+
		"-.\u0005c\u0000\u0000./\u0005t\u0000\u0000/0\u0005o\u0000\u000001\u0005"+
		"r\u0000\u000012\u0005>\u0000\u00002\u0002\u0001\u0000\u0000\u000034\u0005"+
		"(\u0000\u00004\u0004\u0001\u0000\u0000\u000056\u0005)\u0000\u00006\u0006"+
		"\u0001\u0000\u0000\u000078\u0005[\u0000\u00008\b\u0001\u0000\u0000\u0000"+
		"9:\u0005]\u0000\u0000:\n\u0001\u0000\u0000\u0000;<\u0005,\u0000\u0000"+
		"<\f\u0001\u0000\u0000\u0000=>\u0005.\u0000\u0000>\u000e\u0001\u0000\u0000"+
		"\u0000?@\u0005!\u0000\u0000@\u0010\u0001\u0000\u0000\u0000AB\u0005*\u0000"+
		"\u0000B\u0012\u0001\u0000\u0000\u0000CD\u0005&\u0000\u0000DE\u0005&\u0000"+
		"\u0000E\u0014\u0001\u0000\u0000\u0000FG\u0005|\u0000\u0000GH\u0005|\u0000"+
		"\u0000H\u0016\u0001\u0000\u0000\u0000IJ\u0005.\u0000\u0000JK\u0005.\u0000"+
		"\u0000KL\u0005.\u0000\u0000L\u0018\u0001\u0000\u0000\u0000MN\u0005.\u0000"+
		"\u0000NO\u0005.\u0000\u0000O\u001a\u0001\u0000\u0000\u0000PQ\u0005#\u0000"+
		"\u0000Q\u001c\u0001\u0000\u0000\u0000RS\u0005 \u0000\u0000S\u001e\u0001"+
		"\u0000\u0000\u0000TX\u0003!\u0010\u0000UW\u0003#\u0011\u0000VU\u0001\u0000"+
		"\u0000\u0000WZ\u0001\u0000\u0000\u0000XV\u0001\u0000\u0000\u0000XY\u0001"+
		"\u0000\u0000\u0000Y \u0001\u0000\u0000\u0000ZX\u0001\u0000\u0000\u0000"+
		"[b\u0007\u0000\u0000\u0000\\]\b\u0001\u0000\u0000]b\u0004\u0010\u0000"+
		"\u0000^_\u0007\u0002\u0000\u0000_`\u0007\u0003\u0000\u0000`b\u0004\u0010"+
		"\u0001\u0000a[\u0001\u0000\u0000\u0000a\\\u0001\u0000\u0000\u0000a^\u0001"+
		"\u0000\u0000\u0000b\"\u0001\u0000\u0000\u0000cj\u0007\u0004\u0000\u0000"+
		"de\b\u0001\u0000\u0000ej\u0004\u0011\u0002\u0000fg\u0007\u0002\u0000\u0000"+
		"gh\u0007\u0003\u0000\u0000hj\u0004\u0011\u0003\u0000ic\u0001\u0000\u0000"+
		"\u0000id\u0001\u0000\u0000\u0000if\u0001\u0000\u0000\u0000j$\u0001\u0000"+
		"\u0000\u0000\u0004\u0000Xai\u0000";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}