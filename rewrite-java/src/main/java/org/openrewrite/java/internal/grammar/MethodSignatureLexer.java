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
// Generated from /Users/jon/Projects/github/openrewrite/rewrite/rewrite-java/src/main/antlr/MethodSignatureLexer.g4 by ANTLR 4.13.2
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
public class MethodSignatureLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		CONSTRUCTOR=1, JAVASCRIPT_DEFAULT_METHOD=2, LPAREN=3, RPAREN=4, LBRACK=5, 
		RBRACK=6, COMMA=7, DOT=8, BANG=9, WILDCARD=10, AND=11, OR=12, ELLIPSIS=13, 
		DOTDOT=14, POUND=15, SPACE=16, Identifier=17;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"CONSTRUCTOR", "JAVASCRIPT_DEFAULT_METHOD", "LPAREN", "RPAREN", "LBRACK", 
			"RBRACK", "COMMA", "DOT", "BANG", "WILDCARD", "AND", "OR", "ELLIPSIS", 
			"DOTDOT", "POUND", "SPACE", "Identifier", "JavaLetter", "JavaLetterOrDigit"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, "'<default>'", "'('", "')'", "'['", "']'", "','", null, "'!'", 
			"'*'", "'&&'", "'||'", "'...'", "'..'", "'#'", "' '"
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
		case 17:
			return JavaLetter_sempred((RuleContext)_localctx, predIndex);
		case 18:
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
		"\u0004\u0000\u0011~\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002\u0001"+
		"\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004"+
		"\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007"+
		"\u0007\u0007\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b"+
		"\u0007\u000b\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002"+
		"\u000f\u0007\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002"+
		"\u0012\u0007\u0012\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0003\u0000;\b\u0000\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0002\u0001\u0002\u0001\u0003\u0001"+
		"\u0003\u0001\u0004\u0001\u0004\u0001\u0005\u0001\u0005\u0001\u0006\u0001"+
		"\u0006\u0001\u0007\u0001\u0007\u0001\b\u0001\b\u0001\t\u0001\t\u0001\n"+
		"\u0001\n\u0001\n\u0001\u000b\u0001\u000b\u0001\u000b\u0001\f\u0001\f\u0001"+
		"\f\u0001\f\u0001\r\u0001\r\u0001\r\u0001\u000e\u0001\u000e\u0001\u000f"+
		"\u0001\u000f\u0001\u0010\u0001\u0010\u0005\u0010j\b\u0010\n\u0010\f\u0010"+
		"m\t\u0010\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011"+
		"\u0001\u0011\u0003\u0011u\b\u0011\u0001\u0012\u0001\u0012\u0001\u0012"+
		"\u0001\u0012\u0001\u0012\u0001\u0012\u0003\u0012}\b\u0012\u0000\u0000"+
		"\u0013\u0001\u0001\u0003\u0002\u0005\u0003\u0007\u0004\t\u0005\u000b\u0006"+
		"\r\u0007\u000f\b\u0011\t\u0013\n\u0015\u000b\u0017\f\u0019\r\u001b\u000e"+
		"\u001d\u000f\u001f\u0010!\u0011#\u0000%\u0000\u0001\u0000\u0005\u0004"+
		"\u0000$$@Z__az\u0002\u0000\u0000\u007f\u8000\ud800\u8000\udbff\u0001\u0000"+
		"\u8000\ud800\u8000\udbff\u0001\u0000\u8000\udc00\u8000\udfff\u0005\u0000"+
		"$$09AZ__az\u0081\u0000\u0001\u0001\u0000\u0000\u0000\u0000\u0003\u0001"+
		"\u0000\u0000\u0000\u0000\u0005\u0001\u0000\u0000\u0000\u0000\u0007\u0001"+
		"\u0000\u0000\u0000\u0000\t\u0001\u0000\u0000\u0000\u0000\u000b\u0001\u0000"+
		"\u0000\u0000\u0000\r\u0001\u0000\u0000\u0000\u0000\u000f\u0001\u0000\u0000"+
		"\u0000\u0000\u0011\u0001\u0000\u0000\u0000\u0000\u0013\u0001\u0000\u0000"+
		"\u0000\u0000\u0015\u0001\u0000\u0000\u0000\u0000\u0017\u0001\u0000\u0000"+
		"\u0000\u0000\u0019\u0001\u0000\u0000\u0000\u0000\u001b\u0001\u0000\u0000"+
		"\u0000\u0000\u001d\u0001\u0000\u0000\u0000\u0000\u001f\u0001\u0000\u0000"+
		"\u0000\u0000!\u0001\u0000\u0000\u0000\u0001:\u0001\u0000\u0000\u0000\u0003"+
		"<\u0001\u0000\u0000\u0000\u0005F\u0001\u0000\u0000\u0000\u0007H\u0001"+
		"\u0000\u0000\u0000\tJ\u0001\u0000\u0000\u0000\u000bL\u0001\u0000\u0000"+
		"\u0000\rN\u0001\u0000\u0000\u0000\u000fP\u0001\u0000\u0000\u0000\u0011"+
		"R\u0001\u0000\u0000\u0000\u0013T\u0001\u0000\u0000\u0000\u0015V\u0001"+
		"\u0000\u0000\u0000\u0017Y\u0001\u0000\u0000\u0000\u0019\\\u0001\u0000"+
		"\u0000\u0000\u001b`\u0001\u0000\u0000\u0000\u001dc\u0001\u0000\u0000\u0000"+
		"\u001fe\u0001\u0000\u0000\u0000!g\u0001\u0000\u0000\u0000#t\u0001\u0000"+
		"\u0000\u0000%|\u0001\u0000\u0000\u0000\'(\u0005<\u0000\u0000()\u0005c"+
		"\u0000\u0000)*\u0005o\u0000\u0000*+\u0005n\u0000\u0000+,\u0005s\u0000"+
		"\u0000,-\u0005t\u0000\u0000-.\u0005r\u0000\u0000./\u0005u\u0000\u0000"+
		"/0\u0005c\u0000\u000001\u0005t\u0000\u000012\u0005o\u0000\u000023\u0005"+
		"r\u0000\u00003;\u0005>\u0000\u000045\u0005<\u0000\u000056\u0005i\u0000"+
		"\u000067\u0005n\u0000\u000078\u0005i\u0000\u000089\u0005t\u0000\u0000"+
		"9;\u0005>\u0000\u0000:\'\u0001\u0000\u0000\u0000:4\u0001\u0000\u0000\u0000"+
		";\u0002\u0001\u0000\u0000\u0000<=\u0005<\u0000\u0000=>\u0005d\u0000\u0000"+
		">?\u0005e\u0000\u0000?@\u0005f\u0000\u0000@A\u0005a\u0000\u0000AB\u0005"+
		"u\u0000\u0000BC\u0005l\u0000\u0000CD\u0005t\u0000\u0000DE\u0005>\u0000"+
		"\u0000E\u0004\u0001\u0000\u0000\u0000FG\u0005(\u0000\u0000G\u0006\u0001"+
		"\u0000\u0000\u0000HI\u0005)\u0000\u0000I\b\u0001\u0000\u0000\u0000JK\u0005"+
		"[\u0000\u0000K\n\u0001\u0000\u0000\u0000LM\u0005]\u0000\u0000M\f\u0001"+
		"\u0000\u0000\u0000NO\u0005,\u0000\u0000O\u000e\u0001\u0000\u0000\u0000"+
		"PQ\u0002./\u0000Q\u0010\u0001\u0000\u0000\u0000RS\u0005!\u0000\u0000S"+
		"\u0012\u0001\u0000\u0000\u0000TU\u0005*\u0000\u0000U\u0014\u0001\u0000"+
		"\u0000\u0000VW\u0005&\u0000\u0000WX\u0005&\u0000\u0000X\u0016\u0001\u0000"+
		"\u0000\u0000YZ\u0005|\u0000\u0000Z[\u0005|\u0000\u0000[\u0018\u0001\u0000"+
		"\u0000\u0000\\]\u0005.\u0000\u0000]^\u0005.\u0000\u0000^_\u0005.\u0000"+
		"\u0000_\u001a\u0001\u0000\u0000\u0000`a\u0005.\u0000\u0000ab\u0005.\u0000"+
		"\u0000b\u001c\u0001\u0000\u0000\u0000cd\u0005#\u0000\u0000d\u001e\u0001"+
		"\u0000\u0000\u0000ef\u0005 \u0000\u0000f \u0001\u0000\u0000\u0000gk\u0003"+
		"#\u0011\u0000hj\u0003%\u0012\u0000ih\u0001\u0000\u0000\u0000jm\u0001\u0000"+
		"\u0000\u0000ki\u0001\u0000\u0000\u0000kl\u0001\u0000\u0000\u0000l\"\u0001"+
		"\u0000\u0000\u0000mk\u0001\u0000\u0000\u0000nu\u0007\u0000\u0000\u0000"+
		"op\b\u0001\u0000\u0000pu\u0004\u0011\u0000\u0000qr\u0007\u0002\u0000\u0000"+
		"rs\u0007\u0003\u0000\u0000su\u0004\u0011\u0001\u0000tn\u0001\u0000\u0000"+
		"\u0000to\u0001\u0000\u0000\u0000tq\u0001\u0000\u0000\u0000u$\u0001\u0000"+
		"\u0000\u0000v}\u0007\u0004\u0000\u0000wx\b\u0001\u0000\u0000x}\u0004\u0012"+
		"\u0002\u0000yz\u0007\u0002\u0000\u0000z{\u0007\u0003\u0000\u0000{}\u0004"+
		"\u0012\u0003\u0000|v\u0001\u0000\u0000\u0000|w\u0001\u0000\u0000\u0000"+
		"|y\u0001\u0000\u0000\u0000}&\u0001\u0000\u0000\u0000\u0005\u0000:kt|\u0000";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}