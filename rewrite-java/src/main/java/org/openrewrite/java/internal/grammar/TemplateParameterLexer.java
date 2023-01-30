/*
 * Copyright 2022 the original author or authors.
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
		LPAREN=1, RPAREN=2, LBRACK=3, RBRACK=4, DOT=5, COMMA=6, SPACE=7, FullyQualifiedName=8, 
		Number=9, Identifier=10;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"LPAREN", "RPAREN", "LBRACK", "RBRACK", "DOT", "COMMA", "SPACE", "FullyQualifiedName", 
			"Number", "Identifier", "JavaLetter", "JavaLetterOrDigit"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'('", "')'", "'['", "']'", "'.'", "','", "' '"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "LPAREN", "RPAREN", "LBRACK", "RBRACK", "DOT", "COMMA", "SPACE", 
			"FullyQualifiedName", "Number", "Identifier"
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
		case 10:
			return JavaLetter_sempred((RuleContext)_localctx, predIndex);
		case 11:
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
		"\u0004\u0000\n\u007f\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002\u0001"+
		"\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004"+
		"\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007"+
		"\u0007\u0007\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b"+
		"\u0007\u000b\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0002"+
		"\u0001\u0002\u0001\u0003\u0001\u0003\u0001\u0004\u0001\u0004\u0001\u0005"+
		"\u0001\u0005\u0001\u0006\u0001\u0006\u0001\u0007\u0001\u0007\u0001\u0007"+
		"\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007"+
		"\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007"+
		"\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007"+
		"\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007"+
		"\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007"+
		"\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007"+
		"\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007"+
		"\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007"+
		"\u0001\u0007\u0001\u0007\u0001\u0007\u0004\u0007^\b\u0007\u000b\u0007"+
		"\f\u0007_\u0003\u0007b\b\u0007\u0001\b\u0004\be\b\b\u000b\b\f\bf\u0001"+
		"\t\u0001\t\u0005\tk\b\t\n\t\f\tn\t\t\u0001\n\u0001\n\u0001\n\u0001\n\u0001"+
		"\n\u0001\n\u0003\nv\b\n\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b"+
		"\u0001\u000b\u0001\u000b\u0003\u000b~\b\u000b\u0000\u0000\f\u0001\u0001"+
		"\u0003\u0002\u0005\u0003\u0007\u0004\t\u0005\u000b\u0006\r\u0007\u000f"+
		"\b\u0011\t\u0013\n\u0015\u0000\u0017\u0000\u0001\u0000\u0006\u0001\u0000"+
		"09\u0004\u0000$$AZ__az\u0002\u0000\u0000\u007f\u8000\ud800\u8000\udbff"+
		"\u0001\u0000\u8000\ud800\u8000\udbff\u0001\u0000\u8000\udc00\u8000\udfff"+
		"\u0005\u0000$$09AZ__az\u008d\u0000\u0001\u0001\u0000\u0000\u0000\u0000"+
		"\u0003\u0001\u0000\u0000\u0000\u0000\u0005\u0001\u0000\u0000\u0000\u0000"+
		"\u0007\u0001\u0000\u0000\u0000\u0000\t\u0001\u0000\u0000\u0000\u0000\u000b"+
		"\u0001\u0000\u0000\u0000\u0000\r\u0001\u0000\u0000\u0000\u0000\u000f\u0001"+
		"\u0000\u0000\u0000\u0000\u0011\u0001\u0000\u0000\u0000\u0000\u0013\u0001"+
		"\u0000\u0000\u0000\u0001\u0019\u0001\u0000\u0000\u0000\u0003\u001b\u0001"+
		"\u0000\u0000\u0000\u0005\u001d\u0001\u0000\u0000\u0000\u0007\u001f\u0001"+
		"\u0000\u0000\u0000\t!\u0001\u0000\u0000\u0000\u000b#\u0001\u0000\u0000"+
		"\u0000\r%\u0001\u0000\u0000\u0000\u000fa\u0001\u0000\u0000\u0000\u0011"+
		"d\u0001\u0000\u0000\u0000\u0013h\u0001\u0000\u0000\u0000\u0015u\u0001"+
		"\u0000\u0000\u0000\u0017}\u0001\u0000\u0000\u0000\u0019\u001a\u0005(\u0000"+
		"\u0000\u001a\u0002\u0001\u0000\u0000\u0000\u001b\u001c\u0005)\u0000\u0000"+
		"\u001c\u0004\u0001\u0000\u0000\u0000\u001d\u001e\u0005[\u0000\u0000\u001e"+
		"\u0006\u0001\u0000\u0000\u0000\u001f \u0005]\u0000\u0000 \b\u0001\u0000"+
		"\u0000\u0000!\"\u0005.\u0000\u0000\"\n\u0001\u0000\u0000\u0000#$\u0005"+
		",\u0000\u0000$\f\u0001\u0000\u0000\u0000%&\u0005 \u0000\u0000&\u000e\u0001"+
		"\u0000\u0000\u0000\'(\u0005b\u0000\u0000()\u0005o\u0000\u0000)*\u0005"+
		"o\u0000\u0000*+\u0005l\u0000\u0000+,\u0005e\u0000\u0000,-\u0005a\u0000"+
		"\u0000-b\u0005n\u0000\u0000./\u0005b\u0000\u0000/0\u0005y\u0000\u0000"+
		"01\u0005t\u0000\u00001b\u0005e\u0000\u000023\u0005c\u0000\u000034\u0005"+
		"h\u0000\u000045\u0005a\u0000\u00005b\u0005r\u0000\u000067\u0005d\u0000"+
		"\u000078\u0005o\u0000\u000089\u0005u\u0000\u00009:\u0005b\u0000\u0000"+
		":;\u0005l\u0000\u0000;b\u0005e\u0000\u0000<=\u0005f\u0000\u0000=>\u0005"+
		"l\u0000\u0000>?\u0005o\u0000\u0000?@\u0005a\u0000\u0000@b\u0005t\u0000"+
		"\u0000AB\u0005i\u0000\u0000BC\u0005n\u0000\u0000Cb\u0005t\u0000\u0000"+
		"DE\u0005l\u0000\u0000EF\u0005o\u0000\u0000FG\u0005n\u0000\u0000Gb\u0005"+
		"g\u0000\u0000HI\u0005s\u0000\u0000IJ\u0005h\u0000\u0000JK\u0005o\u0000"+
		"\u0000KL\u0005r\u0000\u0000Lb\u0005t\u0000\u0000MN\u0005S\u0000\u0000"+
		"NO\u0005t\u0000\u0000OP\u0005r\u0000\u0000PQ\u0005i\u0000\u0000QR\u0005"+
		"n\u0000\u0000Rb\u0005g\u0000\u0000ST\u0005O\u0000\u0000TU\u0005b\u0000"+
		"\u0000UV\u0005j\u0000\u0000VW\u0005e\u0000\u0000WX\u0005c\u0000\u0000"+
		"Xb\u0005t\u0000\u0000Y]\u0003\u0013\t\u0000Z[\u0003\t\u0004\u0000[\\\u0003"+
		"\u0013\t\u0000\\^\u0001\u0000\u0000\u0000]Z\u0001\u0000\u0000\u0000^_"+
		"\u0001\u0000\u0000\u0000_]\u0001\u0000\u0000\u0000_`\u0001\u0000\u0000"+
		"\u0000`b\u0001\u0000\u0000\u0000a\'\u0001\u0000\u0000\u0000a.\u0001\u0000"+
		"\u0000\u0000a2\u0001\u0000\u0000\u0000a6\u0001\u0000\u0000\u0000a<\u0001"+
		"\u0000\u0000\u0000aA\u0001\u0000\u0000\u0000aD\u0001\u0000\u0000\u0000"+
		"aH\u0001\u0000\u0000\u0000aM\u0001\u0000\u0000\u0000aS\u0001\u0000\u0000"+
		"\u0000aY\u0001\u0000\u0000\u0000b\u0010\u0001\u0000\u0000\u0000ce\u0007"+
		"\u0000\u0000\u0000dc\u0001\u0000\u0000\u0000ef\u0001\u0000\u0000\u0000"+
		"fd\u0001\u0000\u0000\u0000fg\u0001\u0000\u0000\u0000g\u0012\u0001\u0000"+
		"\u0000\u0000hl\u0003\u0015\n\u0000ik\u0003\u0017\u000b\u0000ji\u0001\u0000"+
		"\u0000\u0000kn\u0001\u0000\u0000\u0000lj\u0001\u0000\u0000\u0000lm\u0001"+
		"\u0000\u0000\u0000m\u0014\u0001\u0000\u0000\u0000nl\u0001\u0000\u0000"+
		"\u0000ov\u0007\u0001\u0000\u0000pq\b\u0002\u0000\u0000qv\u0004\n\u0000"+
		"\u0000rs\u0007\u0003\u0000\u0000st\u0007\u0004\u0000\u0000tv\u0004\n\u0001"+
		"\u0000uo\u0001\u0000\u0000\u0000up\u0001\u0000\u0000\u0000ur\u0001\u0000"+
		"\u0000\u0000v\u0016\u0001\u0000\u0000\u0000w~\u0007\u0005\u0000\u0000"+
		"xy\b\u0002\u0000\u0000y~\u0004\u000b\u0002\u0000z{\u0007\u0003\u0000\u0000"+
		"{|\u0007\u0004\u0000\u0000|~\u0004\u000b\u0003\u0000}w\u0001\u0000\u0000"+
		"\u0000}x\u0001\u0000\u0000\u0000}z\u0001\u0000\u0000\u0000~\u0018\u0001"+
		"\u0000\u0000\u0000\u0007\u0000_aflu}\u0000";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}