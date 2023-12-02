/*
 * Copyright 2023 the original author or authors.
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
		LPAREN=1, RPAREN=2, DOT=3, COLON=4, COMMA=5, FullyQualifiedName=6, Number=7, 
		Identifier=8, S=9;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"LPAREN", "RPAREN", "DOT", "COLON", "COMMA", "FullyQualifiedName", "Number", 
			"Identifier", "JavaLetter", "JavaLetterOrDigit", "S"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'('", "')'", "'.'", "':'", "','"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "LPAREN", "RPAREN", "DOT", "COLON", "COMMA", "FullyQualifiedName", 
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
		case 8:
			return JavaLetter_sempred((RuleContext)_localctx, predIndex);
		case 9:
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
		"\u0004\u0000\t}\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002\u0001"+
		"\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004"+
		"\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007"+
		"\u0007\u0007\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0001\u0000"+
		"\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0002\u0001\u0002\u0001\u0003"+
		"\u0001\u0003\u0001\u0004\u0001\u0004\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0001\u0005\u0001\u0005\u0004\u0005X\b\u0005\u000b\u0005"+
		"\f\u0005Y\u0003\u0005\\\b\u0005\u0001\u0006\u0004\u0006_\b\u0006\u000b"+
		"\u0006\f\u0006`\u0001\u0007\u0001\u0007\u0005\u0007e\b\u0007\n\u0007\f"+
		"\u0007h\t\u0007\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0003"+
		"\bp\b\b\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0003\tx\b\t\u0001"+
		"\n\u0001\n\u0001\n\u0001\n\u0000\u0000\u000b\u0001\u0001\u0003\u0002\u0005"+
		"\u0003\u0007\u0004\t\u0005\u000b\u0006\r\u0007\u000f\b\u0011\u0000\u0013"+
		"\u0000\u0015\t\u0001\u0000\u0007\u0001\u000009\u0004\u0000$$AZ__az\u0002"+
		"\u0000\u0000\u007f\u8000\ud800\u8000\udbff\u0001\u0000\u8000\ud800\u8000"+
		"\udbff\u0001\u0000\u8000\udc00\u8000\udfff\u0005\u0000$$09AZ__az\u0003"+
		"\u0000\t\n\r\r  \u008b\u0000\u0001\u0001\u0000\u0000\u0000\u0000\u0003"+
		"\u0001\u0000\u0000\u0000\u0000\u0005\u0001\u0000\u0000\u0000\u0000\u0007"+
		"\u0001\u0000\u0000\u0000\u0000\t\u0001\u0000\u0000\u0000\u0000\u000b\u0001"+
		"\u0000\u0000\u0000\u0000\r\u0001\u0000\u0000\u0000\u0000\u000f\u0001\u0000"+
		"\u0000\u0000\u0000\u0015\u0001\u0000\u0000\u0000\u0001\u0017\u0001\u0000"+
		"\u0000\u0000\u0003\u0019\u0001\u0000\u0000\u0000\u0005\u001b\u0001\u0000"+
		"\u0000\u0000\u0007\u001d\u0001\u0000\u0000\u0000\t\u001f\u0001\u0000\u0000"+
		"\u0000\u000b[\u0001\u0000\u0000\u0000\r^\u0001\u0000\u0000\u0000\u000f"+
		"b\u0001\u0000\u0000\u0000\u0011o\u0001\u0000\u0000\u0000\u0013w\u0001"+
		"\u0000\u0000\u0000\u0015y\u0001\u0000\u0000\u0000\u0017\u0018\u0005(\u0000"+
		"\u0000\u0018\u0002\u0001\u0000\u0000\u0000\u0019\u001a\u0005)\u0000\u0000"+
		"\u001a\u0004\u0001\u0000\u0000\u0000\u001b\u001c\u0005.\u0000\u0000\u001c"+
		"\u0006\u0001\u0000\u0000\u0000\u001d\u001e\u0005:\u0000\u0000\u001e\b"+
		"\u0001\u0000\u0000\u0000\u001f \u0005,\u0000\u0000 \n\u0001\u0000\u0000"+
		"\u0000!\"\u0005b\u0000\u0000\"#\u0005o\u0000\u0000#$\u0005o\u0000\u0000"+
		"$%\u0005l\u0000\u0000%&\u0005e\u0000\u0000&\'\u0005a\u0000\u0000\'\\\u0005"+
		"n\u0000\u0000()\u0005b\u0000\u0000)*\u0005y\u0000\u0000*+\u0005t\u0000"+
		"\u0000+\\\u0005e\u0000\u0000,-\u0005c\u0000\u0000-.\u0005h\u0000\u0000"+
		"./\u0005a\u0000\u0000/\\\u0005r\u0000\u000001\u0005d\u0000\u000012\u0005"+
		"o\u0000\u000023\u0005u\u0000\u000034\u0005b\u0000\u000045\u0005l\u0000"+
		"\u00005\\\u0005e\u0000\u000067\u0005f\u0000\u000078\u0005l\u0000\u0000"+
		"89\u0005o\u0000\u00009:\u0005a\u0000\u0000:\\\u0005t\u0000\u0000;<\u0005"+
		"i\u0000\u0000<=\u0005n\u0000\u0000=\\\u0005t\u0000\u0000>?\u0005l\u0000"+
		"\u0000?@\u0005o\u0000\u0000@A\u0005n\u0000\u0000A\\\u0005g\u0000\u0000"+
		"BC\u0005s\u0000\u0000CD\u0005h\u0000\u0000DE\u0005o\u0000\u0000EF\u0005"+
		"r\u0000\u0000F\\\u0005t\u0000\u0000GH\u0005S\u0000\u0000HI\u0005t\u0000"+
		"\u0000IJ\u0005r\u0000\u0000JK\u0005i\u0000\u0000KL\u0005n\u0000\u0000"+
		"L\\\u0005g\u0000\u0000MN\u0005O\u0000\u0000NO\u0005b\u0000\u0000OP\u0005"+
		"j\u0000\u0000PQ\u0005e\u0000\u0000QR\u0005c\u0000\u0000R\\\u0005t\u0000"+
		"\u0000SW\u0003\u000f\u0007\u0000TU\u0003\u0005\u0002\u0000UV\u0003\u000f"+
		"\u0007\u0000VX\u0001\u0000\u0000\u0000WT\u0001\u0000\u0000\u0000XY\u0001"+
		"\u0000\u0000\u0000YW\u0001\u0000\u0000\u0000YZ\u0001\u0000\u0000\u0000"+
		"Z\\\u0001\u0000\u0000\u0000[!\u0001\u0000\u0000\u0000[(\u0001\u0000\u0000"+
		"\u0000[,\u0001\u0000\u0000\u0000[0\u0001\u0000\u0000\u0000[6\u0001\u0000"+
		"\u0000\u0000[;\u0001\u0000\u0000\u0000[>\u0001\u0000\u0000\u0000[B\u0001"+
		"\u0000\u0000\u0000[G\u0001\u0000\u0000\u0000[M\u0001\u0000\u0000\u0000"+
		"[S\u0001\u0000\u0000\u0000\\\f\u0001\u0000\u0000\u0000]_\u0007\u0000\u0000"+
		"\u0000^]\u0001\u0000\u0000\u0000_`\u0001\u0000\u0000\u0000`^\u0001\u0000"+
		"\u0000\u0000`a\u0001\u0000\u0000\u0000a\u000e\u0001\u0000\u0000\u0000"+
		"bf\u0003\u0011\b\u0000ce\u0003\u0013\t\u0000dc\u0001\u0000\u0000\u0000"+
		"eh\u0001\u0000\u0000\u0000fd\u0001\u0000\u0000\u0000fg\u0001\u0000\u0000"+
		"\u0000g\u0010\u0001\u0000\u0000\u0000hf\u0001\u0000\u0000\u0000ip\u0007"+
		"\u0001\u0000\u0000jk\b\u0002\u0000\u0000kp\u0004\b\u0000\u0000lm\u0007"+
		"\u0003\u0000\u0000mn\u0007\u0004\u0000\u0000np\u0004\b\u0001\u0000oi\u0001"+
		"\u0000\u0000\u0000oj\u0001\u0000\u0000\u0000ol\u0001\u0000\u0000\u0000"+
		"p\u0012\u0001\u0000\u0000\u0000qx\u0007\u0005\u0000\u0000rs\b\u0002\u0000"+
		"\u0000sx\u0004\t\u0002\u0000tu\u0007\u0003\u0000\u0000uv\u0007\u0004\u0000"+
		"\u0000vx\u0004\t\u0003\u0000wq\u0001\u0000\u0000\u0000wr\u0001\u0000\u0000"+
		"\u0000wt\u0001\u0000\u0000\u0000x\u0014\u0001\u0000\u0000\u0000yz\u0007"+
		"\u0006\u0000\u0000z{\u0001\u0000\u0000\u0000{|\u0006\n\u0000\u0000|\u0016"+
		"\u0001\u0000\u0000\u0000\u0007\u0000Y[`fow\u0001\u0006\u0000\u0000";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}