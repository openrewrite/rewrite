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
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class TemplateParameterLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.11.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		LPAREN=1, RPAREN=2, DOT=3, COLON=4, COMMA=5, LBRACK=6, RBRACK=7, WILDCARD=8, 
		Variance=9, FullyQualifiedName=10, Number=11, Identifier=12, S=13;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"LPAREN", "RPAREN", "DOT", "COLON", "COMMA", "LBRACK", "RBRACK", "WILDCARD", 
			"Variance", "FullyQualifiedName", "Number", "Identifier", "JavaLetter", 
			"JavaLetterOrDigit", "S"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'('", "')'", "'.'", "':'", "','", "'<'", "'>'", "'?'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "LPAREN", "RPAREN", "DOT", "COLON", "COMMA", "LBRACK", "RBRACK", 
			"WILDCARD", "Variance", "FullyQualifiedName", "Number", "Identifier", 
			"S"
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
		case 12:
			return JavaLetter_sempred((RuleContext)_localctx, predIndex);
		case 13:
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
		"\u0004\u0000\r\u0099\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002\u0001"+
		"\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004"+
		"\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007"+
		"\u0007\u0007\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b"+
		"\u0007\u000b\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0001"+
		"\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0002\u0001\u0002\u0001"+
		"\u0003\u0001\u0003\u0001\u0004\u0001\u0004\u0001\u0005\u0001\u0005\u0001"+
		"\u0006\u0001\u0006\u0001\u0007\u0001\u0007\u0001\b\u0001\b\u0001\b\u0001"+
		"\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0003"+
		"\b<\b\b\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001"+
		"\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001"+
		"\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001"+
		"\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001"+
		"\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001"+
		"\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001"+
		"\t\u0001\t\u0004\tt\b\t\u000b\t\f\tu\u0003\tx\b\t\u0001\n\u0004\n{\b\n"+
		"\u000b\n\f\n|\u0001\u000b\u0001\u000b\u0005\u000b\u0081\b\u000b\n\u000b"+
		"\f\u000b\u0084\t\u000b\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f"+
		"\u0003\f\u008c\b\f\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0003"+
		"\r\u0094\b\r\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0000\u0000"+
		"\u000f\u0001\u0001\u0003\u0002\u0005\u0003\u0007\u0004\t\u0005\u000b\u0006"+
		"\r\u0007\u000f\b\u0011\t\u0013\n\u0015\u000b\u0017\f\u0019\u0000\u001b"+
		"\u0000\u001d\r\u0001\u0000\u0007\u0001\u000009\u0004\u0000$$AZ__az\u0002"+
		"\u0000\u0000\u007f\u8000\ud800\u8000\udbff\u0001\u0000\u8000\ud800\u8000"+
		"\udbff\u0001\u0000\u8000\udc00\u8000\udfff\u0005\u0000$$09AZ__az\u0003"+
		"\u0000\t\n\r\r  \u00a8\u0000\u0001\u0001\u0000\u0000\u0000\u0000\u0003"+
		"\u0001\u0000\u0000\u0000\u0000\u0005\u0001\u0000\u0000\u0000\u0000\u0007"+
		"\u0001\u0000\u0000\u0000\u0000\t\u0001\u0000\u0000\u0000\u0000\u000b\u0001"+
		"\u0000\u0000\u0000\u0000\r\u0001\u0000\u0000\u0000\u0000\u000f\u0001\u0000"+
		"\u0000\u0000\u0000\u0011\u0001\u0000\u0000\u0000\u0000\u0013\u0001\u0000"+
		"\u0000\u0000\u0000\u0015\u0001\u0000\u0000\u0000\u0000\u0017\u0001\u0000"+
		"\u0000\u0000\u0000\u001d\u0001\u0000\u0000\u0000\u0001\u001f\u0001\u0000"+
		"\u0000\u0000\u0003!\u0001\u0000\u0000\u0000\u0005#\u0001\u0000\u0000\u0000"+
		"\u0007%\u0001\u0000\u0000\u0000\t\'\u0001\u0000\u0000\u0000\u000b)\u0001"+
		"\u0000\u0000\u0000\r+\u0001\u0000\u0000\u0000\u000f-\u0001\u0000\u0000"+
		"\u0000\u0011;\u0001\u0000\u0000\u0000\u0013w\u0001\u0000\u0000\u0000\u0015"+
		"z\u0001\u0000\u0000\u0000\u0017~\u0001\u0000\u0000\u0000\u0019\u008b\u0001"+
		"\u0000\u0000\u0000\u001b\u0093\u0001\u0000\u0000\u0000\u001d\u0095\u0001"+
		"\u0000\u0000\u0000\u001f \u0005(\u0000\u0000 \u0002\u0001\u0000\u0000"+
		"\u0000!\"\u0005)\u0000\u0000\"\u0004\u0001\u0000\u0000\u0000#$\u0005."+
		"\u0000\u0000$\u0006\u0001\u0000\u0000\u0000%&\u0005:\u0000\u0000&\b\u0001"+
		"\u0000\u0000\u0000\'(\u0005,\u0000\u0000(\n\u0001\u0000\u0000\u0000)*"+
		"\u0005<\u0000\u0000*\f\u0001\u0000\u0000\u0000+,\u0005>\u0000\u0000,\u000e"+
		"\u0001\u0000\u0000\u0000-.\u0005?\u0000\u0000.\u0010\u0001\u0000\u0000"+
		"\u0000/0\u0005e\u0000\u000001\u0005x\u0000\u000012\u0005t\u0000\u0000"+
		"23\u0005e\u0000\u000034\u0005n\u0000\u000045\u0005d\u0000\u00005<\u0005"+
		"s\u0000\u000067\u0005s\u0000\u000078\u0005u\u0000\u000089\u0005p\u0000"+
		"\u00009:\u0005e\u0000\u0000:<\u0005r\u0000\u0000;/\u0001\u0000\u0000\u0000"+
		";6\u0001\u0000\u0000\u0000<\u0012\u0001\u0000\u0000\u0000=>\u0005b\u0000"+
		"\u0000>?\u0005o\u0000\u0000?@\u0005o\u0000\u0000@A\u0005l\u0000\u0000"+
		"AB\u0005e\u0000\u0000BC\u0005a\u0000\u0000Cx\u0005n\u0000\u0000DE\u0005"+
		"b\u0000\u0000EF\u0005y\u0000\u0000FG\u0005t\u0000\u0000Gx\u0005e\u0000"+
		"\u0000HI\u0005c\u0000\u0000IJ\u0005h\u0000\u0000JK\u0005a\u0000\u0000"+
		"Kx\u0005r\u0000\u0000LM\u0005d\u0000\u0000MN\u0005o\u0000\u0000NO\u0005"+
		"u\u0000\u0000OP\u0005b\u0000\u0000PQ\u0005l\u0000\u0000Qx\u0005e\u0000"+
		"\u0000RS\u0005f\u0000\u0000ST\u0005l\u0000\u0000TU\u0005o\u0000\u0000"+
		"UV\u0005a\u0000\u0000Vx\u0005t\u0000\u0000WX\u0005i\u0000\u0000XY\u0005"+
		"n\u0000\u0000Yx\u0005t\u0000\u0000Z[\u0005l\u0000\u0000[\\\u0005o\u0000"+
		"\u0000\\]\u0005n\u0000\u0000]x\u0005g\u0000\u0000^_\u0005s\u0000\u0000"+
		"_`\u0005h\u0000\u0000`a\u0005o\u0000\u0000ab\u0005r\u0000\u0000bx\u0005"+
		"t\u0000\u0000cd\u0005S\u0000\u0000de\u0005t\u0000\u0000ef\u0005r\u0000"+
		"\u0000fg\u0005i\u0000\u0000gh\u0005n\u0000\u0000hx\u0005g\u0000\u0000"+
		"ij\u0005O\u0000\u0000jk\u0005b\u0000\u0000kl\u0005j\u0000\u0000lm\u0005"+
		"e\u0000\u0000mn\u0005c\u0000\u0000nx\u0005t\u0000\u0000os\u0003\u0017"+
		"\u000b\u0000pq\u0003\u0005\u0002\u0000qr\u0003\u0017\u000b\u0000rt\u0001"+
		"\u0000\u0000\u0000sp\u0001\u0000\u0000\u0000tu\u0001\u0000\u0000\u0000"+
		"us\u0001\u0000\u0000\u0000uv\u0001\u0000\u0000\u0000vx\u0001\u0000\u0000"+
		"\u0000w=\u0001\u0000\u0000\u0000wD\u0001\u0000\u0000\u0000wH\u0001\u0000"+
		"\u0000\u0000wL\u0001\u0000\u0000\u0000wR\u0001\u0000\u0000\u0000wW\u0001"+
		"\u0000\u0000\u0000wZ\u0001\u0000\u0000\u0000w^\u0001\u0000\u0000\u0000"+
		"wc\u0001\u0000\u0000\u0000wi\u0001\u0000\u0000\u0000wo\u0001\u0000\u0000"+
		"\u0000x\u0014\u0001\u0000\u0000\u0000y{\u0007\u0000\u0000\u0000zy\u0001"+
		"\u0000\u0000\u0000{|\u0001\u0000\u0000\u0000|z\u0001\u0000\u0000\u0000"+
		"|}\u0001\u0000\u0000\u0000}\u0016\u0001\u0000\u0000\u0000~\u0082\u0003"+
		"\u0019\f\u0000\u007f\u0081\u0003\u001b\r\u0000\u0080\u007f\u0001\u0000"+
		"\u0000\u0000\u0081\u0084\u0001\u0000\u0000\u0000\u0082\u0080\u0001\u0000"+
		"\u0000\u0000\u0082\u0083\u0001\u0000\u0000\u0000\u0083\u0018\u0001\u0000"+
		"\u0000\u0000\u0084\u0082\u0001\u0000\u0000\u0000\u0085\u008c\u0007\u0001"+
		"\u0000\u0000\u0086\u0087\b\u0002\u0000\u0000\u0087\u008c\u0004\f\u0000"+
		"\u0000\u0088\u0089\u0007\u0003\u0000\u0000\u0089\u008a\u0007\u0004\u0000"+
		"\u0000\u008a\u008c\u0004\f\u0001\u0000\u008b\u0085\u0001\u0000\u0000\u0000"+
		"\u008b\u0086\u0001\u0000\u0000\u0000\u008b\u0088\u0001\u0000\u0000\u0000"+
		"\u008c\u001a\u0001\u0000\u0000\u0000\u008d\u0094\u0007\u0005\u0000\u0000"+
		"\u008e\u008f\b\u0002\u0000\u0000\u008f\u0094\u0004\r\u0002\u0000\u0090"+
		"\u0091\u0007\u0003\u0000\u0000\u0091\u0092\u0007\u0004\u0000\u0000\u0092"+
		"\u0094\u0004\r\u0003\u0000\u0093\u008d\u0001\u0000\u0000\u0000\u0093\u008e"+
		"\u0001\u0000\u0000\u0000\u0093\u0090\u0001\u0000\u0000\u0000\u0094\u001c"+
		"\u0001\u0000\u0000\u0000\u0095\u0096\u0007\u0006\u0000\u0000\u0096\u0097"+
		"\u0001\u0000\u0000\u0000\u0097\u0098\u0006\u000e\u0000\u0000\u0098\u001e"+
		"\u0001\u0000\u0000\u0000\b\u0000;uw|\u0082\u008b\u0093\u0001\u0006\u0000"+
		"\u0000";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}