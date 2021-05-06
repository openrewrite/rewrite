/*
 * Copyright 2020 the original author or authors.
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
// Generated from /Users/jon/Projects/github/openrewrite/rewrite/rewrite-java/src/main/antlr/AnnotationSignatureLexer.g4 by ANTLR 4.8
package org.openrewrite.java.internal.grammar;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class AnnotationSignatureLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.8", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		IntegerLiteral=1, FloatingPointLiteral=2, BooleanLiteral=3, CharacterLiteral=4, 
		StringLiteral=5, NullLiteral=6, Identifier=7, LPAREN=8, RPAREN=9, LBRACK=10, 
		RBRACK=11, COMMA=12, DOT=13, ASSIGN=14, COLON=15, ADD=16, SUB=17, AND=18, 
		OR=19, AT=20, ELLIPSIS=21, DOTDOT=22, SPACE=23;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"IntegerLiteral", "DecimalIntegerLiteral", "HexIntegerLiteral", "OctalIntegerLiteral", 
			"BinaryIntegerLiteral", "IntegerTypeSuffix", "DecimalNumeral", "Digits", 
			"Digit", "NonZeroDigit", "DigitOrUnderscore", "Underscores", "HexNumeral", 
			"HexDigits", "HexDigit", "HexDigitOrUnderscore", "OctalNumeral", "OctalDigits", 
			"OctalDigit", "OctalDigitOrUnderscore", "BinaryNumeral", "BinaryDigits", 
			"BinaryDigit", "BinaryDigitOrUnderscore", "FloatingPointLiteral", "DecimalFloatingPointLiteral", 
			"ExponentPart", "ExponentIndicator", "SignedInteger", "Sign", "FloatTypeSuffix", 
			"HexadecimalFloatingPointLiteral", "HexSignificand", "BinaryExponent", 
			"BinaryExponentIndicator", "BooleanLiteral", "CharacterLiteral", "SingleCharacter", 
			"StringLiteral", "StringCharacters", "StringCharacter", "EscapeSequence", 
			"OctalEscape", "UnicodeEscape", "ZeroToThree", "NullLiteral", "Identifier", 
			"JavaLetter", "JavaLetterOrDigit", "LPAREN", "RPAREN", "LBRACK", "RBRACK", 
			"COMMA", "DOT", "ASSIGN", "COLON", "ADD", "SUB", "AND", "OR", "AT", "ELLIPSIS", 
			"DOTDOT", "SPACE"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, null, null, null, "'null'", null, "'('", "')'", "'['", 
			"']'", "','", "'.'", "'='", "':'", "'+'", "'-'", "'&&'", "'||'", "'@'", 
			"'...'", "'..'", "' '"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "IntegerLiteral", "FloatingPointLiteral", "BooleanLiteral", "CharacterLiteral", 
			"StringLiteral", "NullLiteral", "Identifier", "LPAREN", "RPAREN", "LBRACK", 
			"RBRACK", "COMMA", "DOT", "ASSIGN", "COLON", "ADD", "SUB", "AND", "OR", 
			"AT", "ELLIPSIS", "DOTDOT", "SPACE"
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


	public AnnotationSignatureLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "AnnotationSignatureLexer.g4"; }

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
		case 47:
			return JavaLetter_sempred((RuleContext)_localctx, predIndex);
		case 48:
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\31\u01c9\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t"+
		" \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t"+
		"+\4,\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64"+
		"\t\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\4<\t<\4=\t"+
		"=\4>\t>\4?\t?\4@\t@\4A\tA\4B\tB\3\2\3\2\3\2\3\2\5\2\u008a\n\2\3\3\3\3"+
		"\5\3\u008e\n\3\3\4\3\4\5\4\u0092\n\4\3\5\3\5\5\5\u0096\n\5\3\6\3\6\5\6"+
		"\u009a\n\6\3\7\3\7\3\b\3\b\3\b\5\b\u00a1\n\b\3\b\3\b\3\b\5\b\u00a6\n\b"+
		"\5\b\u00a8\n\b\3\t\3\t\7\t\u00ac\n\t\f\t\16\t\u00af\13\t\3\t\5\t\u00b2"+
		"\n\t\3\n\3\n\5\n\u00b6\n\n\3\13\3\13\3\f\3\f\5\f\u00bc\n\f\3\r\6\r\u00bf"+
		"\n\r\r\r\16\r\u00c0\3\16\3\16\3\16\3\16\3\17\3\17\7\17\u00c9\n\17\f\17"+
		"\16\17\u00cc\13\17\3\17\5\17\u00cf\n\17\3\20\3\20\3\21\3\21\5\21\u00d5"+
		"\n\21\3\22\3\22\5\22\u00d9\n\22\3\22\3\22\3\23\3\23\7\23\u00df\n\23\f"+
		"\23\16\23\u00e2\13\23\3\23\5\23\u00e5\n\23\3\24\3\24\3\25\3\25\5\25\u00eb"+
		"\n\25\3\26\3\26\3\26\3\26\3\27\3\27\7\27\u00f3\n\27\f\27\16\27\u00f6\13"+
		"\27\3\27\5\27\u00f9\n\27\3\30\3\30\3\31\3\31\5\31\u00ff\n\31\3\32\3\32"+
		"\5\32\u0103\n\32\3\33\3\33\3\33\5\33\u0108\n\33\3\33\5\33\u010b\n\33\3"+
		"\33\5\33\u010e\n\33\3\33\3\33\3\33\5\33\u0113\n\33\3\33\5\33\u0116\n\33"+
		"\3\33\3\33\3\33\5\33\u011b\n\33\3\33\3\33\3\33\5\33\u0120\n\33\3\34\3"+
		"\34\3\34\3\35\3\35\3\36\5\36\u0128\n\36\3\36\3\36\3\37\3\37\3 \3 \3!\3"+
		"!\3!\5!\u0133\n!\3\"\3\"\5\"\u0137\n\"\3\"\3\"\3\"\5\"\u013c\n\"\3\"\3"+
		"\"\5\"\u0140\n\"\3#\3#\3#\3$\3$\3%\3%\3%\3%\3%\3%\3%\3%\3%\5%\u0150\n"+
		"%\3&\3&\3&\3&\3&\3&\3&\3&\5&\u015a\n&\3\'\3\'\3(\3(\5(\u0160\n(\3(\3("+
		"\3)\6)\u0165\n)\r)\16)\u0166\3*\3*\5*\u016b\n*\3+\3+\3+\3+\5+\u0171\n"+
		"+\3,\3,\3,\3,\3,\3,\3,\3,\3,\3,\3,\5,\u017e\n,\3-\3-\3-\3-\3-\3-\3-\3"+
		".\3.\3/\3/\3/\3/\3/\3\60\3\60\7\60\u0190\n\60\f\60\16\60\u0193\13\60\3"+
		"\61\3\61\3\61\3\61\3\61\3\61\5\61\u019b\n\61\3\62\3\62\3\62\3\62\3\62"+
		"\3\62\5\62\u01a3\n\62\3\63\3\63\3\64\3\64\3\65\3\65\3\66\3\66\3\67\3\67"+
		"\38\38\39\39\3:\3:\3;\3;\3<\3<\3=\3=\3=\3>\3>\3>\3?\3?\3@\3@\3@\3@\3A"+
		"\3A\3A\3B\3B\2\2C\3\3\5\2\7\2\t\2\13\2\r\2\17\2\21\2\23\2\25\2\27\2\31"+
		"\2\33\2\35\2\37\2!\2#\2%\2\'\2)\2+\2-\2/\2\61\2\63\4\65\2\67\29\2;\2="+
		"\2?\2A\2C\2E\2G\2I\5K\6M\2O\7Q\2S\2U\2W\2Y\2[\2]\b_\ta\2c\2e\ng\13i\f"+
		"k\rm\16o\17q\20s\21u\22w\23y\24{\25}\26\177\27\u0081\30\u0083\31\3\2\26"+
		"\4\2NNnn\3\2\63;\4\2ZZzz\5\2\62;CHch\3\2\629\4\2DDdd\3\2\62\63\4\2GGg"+
		"g\4\2--//\6\2FFHHffhh\4\2RRrr\4\2))^^\4\2$$^^\n\2$$))^^ddhhppttvv\3\2"+
		"\62\65\6\2&&C\\aac|\4\2\2\u0081\ud802\udc01\3\2\ud802\udc01\3\2\udc02"+
		"\ue001\7\2&&\62;C\\aac|\2\u01d4\2\3\3\2\2\2\2\63\3\2\2\2\2I\3\2\2\2\2"+
		"K\3\2\2\2\2O\3\2\2\2\2]\3\2\2\2\2_\3\2\2\2\2e\3\2\2\2\2g\3\2\2\2\2i\3"+
		"\2\2\2\2k\3\2\2\2\2m\3\2\2\2\2o\3\2\2\2\2q\3\2\2\2\2s\3\2\2\2\2u\3\2\2"+
		"\2\2w\3\2\2\2\2y\3\2\2\2\2{\3\2\2\2\2}\3\2\2\2\2\177\3\2\2\2\2\u0081\3"+
		"\2\2\2\2\u0083\3\2\2\2\3\u0089\3\2\2\2\5\u008b\3\2\2\2\7\u008f\3\2\2\2"+
		"\t\u0093\3\2\2\2\13\u0097\3\2\2\2\r\u009b\3\2\2\2\17\u00a7\3\2\2\2\21"+
		"\u00a9\3\2\2\2\23\u00b5\3\2\2\2\25\u00b7\3\2\2\2\27\u00bb\3\2\2\2\31\u00be"+
		"\3\2\2\2\33\u00c2\3\2\2\2\35\u00c6\3\2\2\2\37\u00d0\3\2\2\2!\u00d4\3\2"+
		"\2\2#\u00d6\3\2\2\2%\u00dc\3\2\2\2\'\u00e6\3\2\2\2)\u00ea\3\2\2\2+\u00ec"+
		"\3\2\2\2-\u00f0\3\2\2\2/\u00fa\3\2\2\2\61\u00fe\3\2\2\2\63\u0102\3\2\2"+
		"\2\65\u011f\3\2\2\2\67\u0121\3\2\2\29\u0124\3\2\2\2;\u0127\3\2\2\2=\u012b"+
		"\3\2\2\2?\u012d\3\2\2\2A\u012f\3\2\2\2C\u013f\3\2\2\2E\u0141\3\2\2\2G"+
		"\u0144\3\2\2\2I\u014f\3\2\2\2K\u0159\3\2\2\2M\u015b\3\2\2\2O\u015d\3\2"+
		"\2\2Q\u0164\3\2\2\2S\u016a\3\2\2\2U\u0170\3\2\2\2W\u017d\3\2\2\2Y\u017f"+
		"\3\2\2\2[\u0186\3\2\2\2]\u0188\3\2\2\2_\u018d\3\2\2\2a\u019a\3\2\2\2c"+
		"\u01a2\3\2\2\2e\u01a4\3\2\2\2g\u01a6\3\2\2\2i\u01a8\3\2\2\2k\u01aa\3\2"+
		"\2\2m\u01ac\3\2\2\2o\u01ae\3\2\2\2q\u01b0\3\2\2\2s\u01b2\3\2\2\2u\u01b4"+
		"\3\2\2\2w\u01b6\3\2\2\2y\u01b8\3\2\2\2{\u01bb\3\2\2\2}\u01be\3\2\2\2\177"+
		"\u01c0\3\2\2\2\u0081\u01c4\3\2\2\2\u0083\u01c7\3\2\2\2\u0085\u008a\5\5"+
		"\3\2\u0086\u008a\5\7\4\2\u0087\u008a\5\t\5\2\u0088\u008a\5\13\6\2\u0089"+
		"\u0085\3\2\2\2\u0089\u0086\3\2\2\2\u0089\u0087\3\2\2\2\u0089\u0088\3\2"+
		"\2\2\u008a\4\3\2\2\2\u008b\u008d\5\17\b\2\u008c\u008e\5\r\7\2\u008d\u008c"+
		"\3\2\2\2\u008d\u008e\3\2\2\2\u008e\6\3\2\2\2\u008f\u0091\5\33\16\2\u0090"+
		"\u0092\5\r\7\2\u0091\u0090\3\2\2\2\u0091\u0092\3\2\2\2\u0092\b\3\2\2\2"+
		"\u0093\u0095\5#\22\2\u0094\u0096\5\r\7\2\u0095\u0094\3\2\2\2\u0095\u0096"+
		"\3\2\2\2\u0096\n\3\2\2\2\u0097\u0099\5+\26\2\u0098\u009a\5\r\7\2\u0099"+
		"\u0098\3\2\2\2\u0099\u009a\3\2\2\2\u009a\f\3\2\2\2\u009b\u009c\t\2\2\2"+
		"\u009c\16\3\2\2\2\u009d\u00a8\7\62\2\2\u009e\u00a5\5\25\13\2\u009f\u00a1"+
		"\5\21\t\2\u00a0\u009f\3\2\2\2\u00a0\u00a1\3\2\2\2\u00a1\u00a6\3\2\2\2"+
		"\u00a2\u00a3\5\31\r\2\u00a3\u00a4\5\21\t\2\u00a4\u00a6\3\2\2\2\u00a5\u00a0"+
		"\3\2\2\2\u00a5\u00a2\3\2\2\2\u00a6\u00a8\3\2\2\2\u00a7\u009d\3\2\2\2\u00a7"+
		"\u009e\3\2\2\2\u00a8\20\3\2\2\2\u00a9\u00b1\5\23\n\2\u00aa\u00ac\5\27"+
		"\f\2\u00ab\u00aa\3\2\2\2\u00ac\u00af\3\2\2\2\u00ad\u00ab\3\2\2\2\u00ad"+
		"\u00ae\3\2\2\2\u00ae\u00b0\3\2\2\2\u00af\u00ad\3\2\2\2\u00b0\u00b2\5\23"+
		"\n\2\u00b1\u00ad\3\2\2\2\u00b1\u00b2\3\2\2\2\u00b2\22\3\2\2\2\u00b3\u00b6"+
		"\7\62\2\2\u00b4\u00b6\5\25\13\2\u00b5\u00b3\3\2\2\2\u00b5\u00b4\3\2\2"+
		"\2\u00b6\24\3\2\2\2\u00b7\u00b8\t\3\2\2\u00b8\26\3\2\2\2\u00b9\u00bc\5"+
		"\23\n\2\u00ba\u00bc\7a\2\2\u00bb\u00b9\3\2\2\2\u00bb\u00ba\3\2\2\2\u00bc"+
		"\30\3\2\2\2\u00bd\u00bf\7a\2\2\u00be\u00bd\3\2\2\2\u00bf\u00c0\3\2\2\2"+
		"\u00c0\u00be\3\2\2\2\u00c0\u00c1\3\2\2\2\u00c1\32\3\2\2\2\u00c2\u00c3"+
		"\7\62\2\2\u00c3\u00c4\t\4\2\2\u00c4\u00c5\5\35\17\2\u00c5\34\3\2\2\2\u00c6"+
		"\u00ce\5\37\20\2\u00c7\u00c9\5!\21\2\u00c8\u00c7\3\2\2\2\u00c9\u00cc\3"+
		"\2\2\2\u00ca\u00c8\3\2\2\2\u00ca\u00cb\3\2\2\2\u00cb\u00cd\3\2\2\2\u00cc"+
		"\u00ca\3\2\2\2\u00cd\u00cf\5\37\20\2\u00ce\u00ca\3\2\2\2\u00ce\u00cf\3"+
		"\2\2\2\u00cf\36\3\2\2\2\u00d0\u00d1\t\5\2\2\u00d1 \3\2\2\2\u00d2\u00d5"+
		"\5\37\20\2\u00d3\u00d5\7a\2\2\u00d4\u00d2\3\2\2\2\u00d4\u00d3\3\2\2\2"+
		"\u00d5\"\3\2\2\2\u00d6\u00d8\7\62\2\2\u00d7\u00d9\5\31\r\2\u00d8\u00d7"+
		"\3\2\2\2\u00d8\u00d9\3\2\2\2\u00d9\u00da\3\2\2\2\u00da\u00db\5%\23\2\u00db"+
		"$\3\2\2\2\u00dc\u00e4\5\'\24\2\u00dd\u00df\5)\25\2\u00de\u00dd\3\2\2\2"+
		"\u00df\u00e2\3\2\2\2\u00e0\u00de\3\2\2\2\u00e0\u00e1\3\2\2\2\u00e1\u00e3"+
		"\3\2\2\2\u00e2\u00e0\3\2\2\2\u00e3\u00e5\5\'\24\2\u00e4\u00e0\3\2\2\2"+
		"\u00e4\u00e5\3\2\2\2\u00e5&\3\2\2\2\u00e6\u00e7\t\6\2\2\u00e7(\3\2\2\2"+
		"\u00e8\u00eb\5\'\24\2\u00e9\u00eb\7a\2\2\u00ea\u00e8\3\2\2\2\u00ea\u00e9"+
		"\3\2\2\2\u00eb*\3\2\2\2\u00ec\u00ed\7\62\2\2\u00ed\u00ee\t\7\2\2\u00ee"+
		"\u00ef\5-\27\2\u00ef,\3\2\2\2\u00f0\u00f8\5/\30\2\u00f1\u00f3\5\61\31"+
		"\2\u00f2\u00f1\3\2\2\2\u00f3\u00f6\3\2\2\2\u00f4\u00f2\3\2\2\2\u00f4\u00f5"+
		"\3\2\2\2\u00f5\u00f7\3\2\2\2\u00f6\u00f4\3\2\2\2\u00f7\u00f9\5/\30\2\u00f8"+
		"\u00f4\3\2\2\2\u00f8\u00f9\3\2\2\2\u00f9.\3\2\2\2\u00fa\u00fb\t\b\2\2"+
		"\u00fb\60\3\2\2\2\u00fc\u00ff\5/\30\2\u00fd\u00ff\7a\2\2\u00fe\u00fc\3"+
		"\2\2\2\u00fe\u00fd\3\2\2\2\u00ff\62\3\2\2\2\u0100\u0103\5\65\33\2\u0101"+
		"\u0103\5A!\2\u0102\u0100\3\2\2\2\u0102\u0101\3\2\2\2\u0103\64\3\2\2\2"+
		"\u0104\u0105\5\21\t\2\u0105\u0107\7\60\2\2\u0106\u0108\5\21\t\2\u0107"+
		"\u0106\3\2\2\2\u0107\u0108\3\2\2\2\u0108\u010a\3\2\2\2\u0109\u010b\5\67"+
		"\34\2\u010a\u0109\3\2\2\2\u010a\u010b\3\2\2\2\u010b\u010d\3\2\2\2\u010c"+
		"\u010e\5? \2\u010d\u010c\3\2\2\2\u010d\u010e\3\2\2\2\u010e\u0120\3\2\2"+
		"\2\u010f\u0110\7\60\2\2\u0110\u0112\5\21\t\2\u0111\u0113\5\67\34\2\u0112"+
		"\u0111\3\2\2\2\u0112\u0113\3\2\2\2\u0113\u0115\3\2\2\2\u0114\u0116\5?"+
		" \2\u0115\u0114\3\2\2\2\u0115\u0116\3\2\2\2\u0116\u0120\3\2\2\2\u0117"+
		"\u0118\5\21\t\2\u0118\u011a\5\67\34\2\u0119\u011b\5? \2\u011a\u0119\3"+
		"\2\2\2\u011a\u011b\3\2\2\2\u011b\u0120\3\2\2\2\u011c\u011d\5\21\t\2\u011d"+
		"\u011e\5? \2\u011e\u0120\3\2\2\2\u011f\u0104\3\2\2\2\u011f\u010f\3\2\2"+
		"\2\u011f\u0117\3\2\2\2\u011f\u011c\3\2\2\2\u0120\66\3\2\2\2\u0121\u0122"+
		"\59\35\2\u0122\u0123\5;\36\2\u01238\3\2\2\2\u0124\u0125\t\t\2\2\u0125"+
		":\3\2\2\2\u0126\u0128\5=\37\2\u0127\u0126\3\2\2\2\u0127\u0128\3\2\2\2"+
		"\u0128\u0129\3\2\2\2\u0129\u012a\5\21\t\2\u012a<\3\2\2\2\u012b\u012c\t"+
		"\n\2\2\u012c>\3\2\2\2\u012d\u012e\t\13\2\2\u012e@\3\2\2\2\u012f\u0130"+
		"\5C\"\2\u0130\u0132\5E#\2\u0131\u0133\5? \2\u0132\u0131\3\2\2\2\u0132"+
		"\u0133\3\2\2\2\u0133B\3\2\2\2\u0134\u0136\5\33\16\2\u0135\u0137\7\60\2"+
		"\2\u0136\u0135\3\2\2\2\u0136\u0137\3\2\2\2\u0137\u0140\3\2\2\2\u0138\u0139"+
		"\7\62\2\2\u0139\u013b\t\4\2\2\u013a\u013c\5\35\17\2\u013b\u013a\3\2\2"+
		"\2\u013b\u013c\3\2\2\2\u013c\u013d\3\2\2\2\u013d\u013e\7\60\2\2\u013e"+
		"\u0140\5\35\17\2\u013f\u0134\3\2\2\2\u013f\u0138\3\2\2\2\u0140D\3\2\2"+
		"\2\u0141\u0142\5G$\2\u0142\u0143\5;\36\2\u0143F\3\2\2\2\u0144\u0145\t"+
		"\f\2\2\u0145H\3\2\2\2\u0146\u0147\7v\2\2\u0147\u0148\7t\2\2\u0148\u0149"+
		"\7w\2\2\u0149\u0150\7g\2\2\u014a\u014b\7h\2\2\u014b\u014c\7c\2\2\u014c"+
		"\u014d\7n\2\2\u014d\u014e\7u\2\2\u014e\u0150\7g\2\2\u014f\u0146\3\2\2"+
		"\2\u014f\u014a\3\2\2\2\u0150J\3\2\2\2\u0151\u0152\7)\2\2\u0152\u0153\5"+
		"M\'\2\u0153\u0154\7)\2\2\u0154\u015a\3\2\2\2\u0155\u0156\7)\2\2\u0156"+
		"\u0157\5U+\2\u0157\u0158\7)\2\2\u0158\u015a\3\2\2\2\u0159\u0151\3\2\2"+
		"\2\u0159\u0155\3\2\2\2\u015aL\3\2\2\2\u015b\u015c\n\r\2\2\u015cN\3\2\2"+
		"\2\u015d\u015f\7$\2\2\u015e\u0160\5Q)\2\u015f\u015e\3\2\2\2\u015f\u0160"+
		"\3\2\2\2\u0160\u0161\3\2\2\2\u0161\u0162\7$\2\2\u0162P\3\2\2\2\u0163\u0165"+
		"\5S*\2\u0164\u0163\3\2\2\2\u0165\u0166\3\2\2\2\u0166\u0164\3\2\2\2\u0166"+
		"\u0167\3\2\2\2\u0167R\3\2\2\2\u0168\u016b\n\16\2\2\u0169\u016b\5U+\2\u016a"+
		"\u0168\3\2\2\2\u016a\u0169\3\2\2\2\u016bT\3\2\2\2\u016c\u016d\7^\2\2\u016d"+
		"\u0171\t\17\2\2\u016e\u0171\5W,\2\u016f\u0171\5Y-\2\u0170\u016c\3\2\2"+
		"\2\u0170\u016e\3\2\2\2\u0170\u016f\3\2\2\2\u0171V\3\2\2\2\u0172\u0173"+
		"\7^\2\2\u0173\u017e\5\'\24\2\u0174\u0175\7^\2\2\u0175\u0176\5\'\24\2\u0176"+
		"\u0177\5\'\24\2\u0177\u017e\3\2\2\2\u0178\u0179\7^\2\2\u0179\u017a\5["+
		".\2\u017a\u017b\5\'\24\2\u017b\u017c\5\'\24\2\u017c\u017e\3\2\2\2\u017d"+
		"\u0172\3\2\2\2\u017d\u0174\3\2\2\2\u017d\u0178\3\2\2\2\u017eX\3\2\2\2"+
		"\u017f\u0180\7^\2\2\u0180\u0181\7w\2\2\u0181\u0182\5\37\20\2\u0182\u0183"+
		"\5\37\20\2\u0183\u0184\5\37\20\2\u0184\u0185\5\37\20\2\u0185Z\3\2\2\2"+
		"\u0186\u0187\t\20\2\2\u0187\\\3\2\2\2\u0188\u0189\7p\2\2\u0189\u018a\7"+
		"w\2\2\u018a\u018b\7n\2\2\u018b\u018c\7n\2\2\u018c^\3\2\2\2\u018d\u0191"+
		"\5a\61\2\u018e\u0190\5c\62\2\u018f\u018e\3\2\2\2\u0190\u0193\3\2\2\2\u0191"+
		"\u018f\3\2\2\2\u0191\u0192\3\2\2\2\u0192`\3\2\2\2\u0193\u0191\3\2\2\2"+
		"\u0194\u019b\t\21\2\2\u0195\u0196\n\22\2\2\u0196\u019b\6\61\2\2\u0197"+
		"\u0198\t\23\2\2\u0198\u0199\t\24\2\2\u0199\u019b\6\61\3\2\u019a\u0194"+
		"\3\2\2\2\u019a\u0195\3\2\2\2\u019a\u0197\3\2\2\2\u019bb\3\2\2\2\u019c"+
		"\u01a3\t\25\2\2\u019d\u019e\n\22\2\2\u019e\u01a3\6\62\4\2\u019f\u01a0"+
		"\t\23\2\2\u01a0\u01a1\t\24\2\2\u01a1\u01a3\6\62\5\2\u01a2\u019c\3\2\2"+
		"\2\u01a2\u019d\3\2\2\2\u01a2\u019f\3\2\2\2\u01a3d\3\2\2\2\u01a4\u01a5"+
		"\7*\2\2\u01a5f\3\2\2\2\u01a6\u01a7\7+\2\2\u01a7h\3\2\2\2\u01a8\u01a9\7"+
		"]\2\2\u01a9j\3\2\2\2\u01aa\u01ab\7_\2\2\u01abl\3\2\2\2\u01ac\u01ad\7."+
		"\2\2\u01adn\3\2\2\2\u01ae\u01af\7\60\2\2\u01afp\3\2\2\2\u01b0\u01b1\7"+
		"?\2\2\u01b1r\3\2\2\2\u01b2\u01b3\7<\2\2\u01b3t\3\2\2\2\u01b4\u01b5\7-"+
		"\2\2\u01b5v\3\2\2\2\u01b6\u01b7\7/\2\2\u01b7x\3\2\2\2\u01b8\u01b9\7(\2"+
		"\2\u01b9\u01ba\7(\2\2\u01baz\3\2\2\2\u01bb\u01bc\7~\2\2\u01bc\u01bd\7"+
		"~\2\2\u01bd|\3\2\2\2\u01be\u01bf\7B\2\2\u01bf~\3\2\2\2\u01c0\u01c1\7\60"+
		"\2\2\u01c1\u01c2\7\60\2\2\u01c2\u01c3\7\60\2\2\u01c3\u0080\3\2\2\2\u01c4"+
		"\u01c5\7\60\2\2\u01c5\u01c6\7\60\2\2\u01c6\u0082\3\2\2\2\u01c7\u01c8\7"+
		"\"\2\2\u01c8\u0084\3\2\2\2\61\2\u0089\u008d\u0091\u0095\u0099\u00a0\u00a5"+
		"\u00a7\u00ad\u00b1\u00b5\u00bb\u00c0\u00ca\u00ce\u00d4\u00d8\u00e0\u00e4"+
		"\u00ea\u00f4\u00f8\u00fe\u0102\u0107\u010a\u010d\u0112\u0115\u011a\u011f"+
		"\u0127\u0132\u0136\u013b\u013f\u014f\u0159\u015f\u0166\u016a\u0170\u017d"+
		"\u0191\u019a\u01a2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}