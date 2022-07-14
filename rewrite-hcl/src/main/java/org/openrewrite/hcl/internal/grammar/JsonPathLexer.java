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
// Generated from /Users/jon/Projects/github/openrewrite/rewrite/rewrite-hcl/src/main/antlr/JsonPathLexer.g4 by ANTLR 4.9.3
package org.openrewrite.hcl.internal.grammar;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class JsonPathLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.9.3", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		WS=1, UTF_8_BOM=2, MATCHES_REGEX_OPEN=3, LBRACE=4, RBRACE=5, LBRACK=6, 
		RBRACK=7, LPAREN=8, RPAREN=9, AT=10, DOT=11, DOT_DOT=12, ROOT=13, WILDCARD=14, 
		COLON=15, QUESTION=16, CONTAINS=17, Identifier=18, StringLiteral=19, PositiveNumber=20, 
		NegativeNumber=21, NumericLiteral=22, COMMA=23, TICK=24, QUOTE=25, MATCHES=26, 
		LOGICAL_OPERATOR=27, AND=28, OR=29, EQUALITY_OPERATOR=30, EQ=31, NE=32, 
		TRUE=33, FALSE=34, NULL=35, MATCHES_REGEX_CLOSE=36, S=37, REGEX=38;
	public static final int
		MATCHES_REGEX=1;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE", "MATCHES_REGEX"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"WS", "UTF_8_BOM", "MATCHES_REGEX_OPEN", "LBRACE", "RBRACE", "LBRACK", 
			"RBRACK", "LPAREN", "RPAREN", "AT", "DOT", "DOT_DOT", "ROOT", "WILDCARD", 
			"COLON", "QUESTION", "CONTAINS", "Identifier", "StringLiteral", "PositiveNumber", 
			"NegativeNumber", "NumericLiteral", "COMMA", "TICK", "QUOTE", "MATCHES", 
			"LOGICAL_OPERATOR", "AND", "OR", "EQUALITY_OPERATOR", "EQ", "NE", "TRUE", 
			"FALSE", "NULL", "ESCAPE_SEQUENCE", "UNICODE", "HEX_DIGIT", "SAFE_CODE_POINT", 
			"EXPONENT_PART", "MINUS", "MATCHES_REGEX_CLOSE", "S", "REGEX"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, "'\uFEFF'", null, "'{'", "'}'", "'['", "']'", "'('", "')'", 
			"'@'", "'.'", "'..'", "'$'", "'*'", "':'", "'?'", "'contains'", null, 
			null, null, null, null, "','", "'''", "'\"'", "'=~'", null, "'&&'", "'||'", 
			null, "'=='", "'!='", "'true'", "'false'", "'null'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "WS", "UTF_8_BOM", "MATCHES_REGEX_OPEN", "LBRACE", "RBRACE", "LBRACK", 
			"RBRACK", "LPAREN", "RPAREN", "AT", "DOT", "DOT_DOT", "ROOT", "WILDCARD", 
			"COLON", "QUESTION", "CONTAINS", "Identifier", "StringLiteral", "PositiveNumber", 
			"NegativeNumber", "NumericLiteral", "COMMA", "TICK", "QUOTE", "MATCHES", 
			"LOGICAL_OPERATOR", "AND", "OR", "EQUALITY_OPERATOR", "EQ", "NE", "TRUE", 
			"FALSE", "NULL", "MATCHES_REGEX_CLOSE", "S", "REGEX"
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


	public JsonPathLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "JsonPathLexer.g4"; }

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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2(\u0127\b\1\b\1\4"+
		"\2\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n"+
		"\4\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t"+
		" \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t"+
		"+\4,\t,\4-\t-\3\2\6\2^\n\2\r\2\16\2_\3\2\3\2\3\3\3\3\3\3\3\3\3\4\3\4\5"+
		"\4j\n\4\3\4\3\4\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3\t\3\t\3\n\3"+
		"\n\3\13\3\13\3\f\3\f\3\r\3\r\3\r\3\16\3\16\3\17\3\17\3\20\3\20\3\21\3"+
		"\21\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\23\3\23\7\23\u0096"+
		"\n\23\f\23\16\23\u0099\13\23\3\24\3\24\3\24\7\24\u009e\n\24\f\24\16\24"+
		"\u00a1\13\24\3\24\3\24\3\24\3\24\3\24\7\24\u00a8\n\24\f\24\16\24\u00ab"+
		"\13\24\3\24\3\24\5\24\u00af\n\24\3\25\6\25\u00b2\n\25\r\25\16\25\u00b3"+
		"\3\26\3\26\3\26\3\27\5\27\u00ba\n\27\3\27\3\27\5\27\u00be\n\27\3\30\3"+
		"\30\3\30\3\30\3\31\3\31\3\32\3\32\3\33\3\33\3\33\3\34\3\34\5\34\u00cd"+
		"\n\34\3\35\3\35\3\35\3\36\3\36\3\36\3\37\3\37\5\37\u00d7\n\37\3 \3 \3"+
		" \3!\3!\3!\3\"\3\"\3\"\3\"\3\"\3#\3#\3#\3#\3#\3#\3$\3$\3$\3$\3$\3%\3%"+
		"\3%\3%\3%\3%\3%\3%\3%\3%\3%\3%\3%\3%\3%\3%\3%\3%\5%\u0101\n%\3&\3&\3&"+
		"\3&\3&\3&\3\'\3\'\3(\3(\3)\3)\5)\u010f\n)\3)\6)\u0112\n)\r)\16)\u0113"+
		"\3*\3*\3+\3+\3+\3+\3,\6,\u011d\n,\r,\16,\u011e\3,\3,\3-\6-\u0124\n-\r"+
		"-\16-\u0125\4\u009f\u00a9\2.\4\3\6\4\b\5\n\6\f\7\16\b\20\t\22\n\24\13"+
		"\26\f\30\r\32\16\34\17\36\20 \21\"\22$\23&\24(\25*\26,\27.\30\60\31\62"+
		"\32\64\33\66\348\35:\36<\37> @!B\"D#F$H%J\2L\2N\2P\2R\2T\2V&X\'Z(\4\2"+
		"\3\r\5\2\13\f\16\17\"\"\4\2\13\13\"\"\5\2C\\aac|\7\2//\62;C\\aac|\3\2"+
		"\62;\7\2$$^^ppttvv\5\2\62;CHch\5\2\2!$$^^\4\2GGgg\4\2--//\3\2))\2\u0132"+
		"\2\4\3\2\2\2\2\6\3\2\2\2\2\b\3\2\2\2\2\n\3\2\2\2\2\f\3\2\2\2\2\16\3\2"+
		"\2\2\2\20\3\2\2\2\2\22\3\2\2\2\2\24\3\2\2\2\2\26\3\2\2\2\2\30\3\2\2\2"+
		"\2\32\3\2\2\2\2\34\3\2\2\2\2\36\3\2\2\2\2 \3\2\2\2\2\"\3\2\2\2\2$\3\2"+
		"\2\2\2&\3\2\2\2\2(\3\2\2\2\2*\3\2\2\2\2,\3\2\2\2\2.\3\2\2\2\2\60\3\2\2"+
		"\2\2\62\3\2\2\2\2\64\3\2\2\2\2\66\3\2\2\2\28\3\2\2\2\2:\3\2\2\2\2<\3\2"+
		"\2\2\2>\3\2\2\2\2@\3\2\2\2\2B\3\2\2\2\2D\3\2\2\2\2F\3\2\2\2\2H\3\2\2\2"+
		"\3V\3\2\2\2\3X\3\2\2\2\3Z\3\2\2\2\4]\3\2\2\2\6c\3\2\2\2\bg\3\2\2\2\no"+
		"\3\2\2\2\fq\3\2\2\2\16s\3\2\2\2\20u\3\2\2\2\22w\3\2\2\2\24y\3\2\2\2\26"+
		"{\3\2\2\2\30}\3\2\2\2\32\177\3\2\2\2\34\u0082\3\2\2\2\36\u0084\3\2\2\2"+
		" \u0086\3\2\2\2\"\u0088\3\2\2\2$\u008a\3\2\2\2&\u0093\3\2\2\2(\u00ae\3"+
		"\2\2\2*\u00b1\3\2\2\2,\u00b5\3\2\2\2.\u00b9\3\2\2\2\60\u00bf\3\2\2\2\62"+
		"\u00c3\3\2\2\2\64\u00c5\3\2\2\2\66\u00c7\3\2\2\28\u00cc\3\2\2\2:\u00ce"+
		"\3\2\2\2<\u00d1\3\2\2\2>\u00d6\3\2\2\2@\u00d8\3\2\2\2B\u00db\3\2\2\2D"+
		"\u00de\3\2\2\2F\u00e3\3\2\2\2H\u00e9\3\2\2\2J\u0100\3\2\2\2L\u0102\3\2"+
		"\2\2N\u0108\3\2\2\2P\u010a\3\2\2\2R\u010c\3\2\2\2T\u0115\3\2\2\2V\u0117"+
		"\3\2\2\2X\u011c\3\2\2\2Z\u0123\3\2\2\2\\^\t\2\2\2]\\\3\2\2\2^_\3\2\2\2"+
		"_]\3\2\2\2_`\3\2\2\2`a\3\2\2\2ab\b\2\2\2b\5\3\2\2\2cd\7\uff01\2\2de\3"+
		"\2\2\2ef\b\3\2\2f\7\3\2\2\2gi\5\66\33\2hj\t\3\2\2ih\3\2\2\2ij\3\2\2\2"+
		"jk\3\2\2\2kl\5\62\31\2lm\3\2\2\2mn\b\4\3\2n\t\3\2\2\2op\7}\2\2p\13\3\2"+
		"\2\2qr\7\177\2\2r\r\3\2\2\2st\7]\2\2t\17\3\2\2\2uv\7_\2\2v\21\3\2\2\2"+
		"wx\7*\2\2x\23\3\2\2\2yz\7+\2\2z\25\3\2\2\2{|\7B\2\2|\27\3\2\2\2}~\7\60"+
		"\2\2~\31\3\2\2\2\177\u0080\7\60\2\2\u0080\u0081\7\60\2\2\u0081\33\3\2"+
		"\2\2\u0082\u0083\7&\2\2\u0083\35\3\2\2\2\u0084\u0085\7,\2\2\u0085\37\3"+
		"\2\2\2\u0086\u0087\7<\2\2\u0087!\3\2\2\2\u0088\u0089\7A\2\2\u0089#\3\2"+
		"\2\2\u008a\u008b\7e\2\2\u008b\u008c\7q\2\2\u008c\u008d\7p\2\2\u008d\u008e"+
		"\7v\2\2\u008e\u008f\7c\2\2\u008f\u0090\7k\2\2\u0090\u0091\7p\2\2\u0091"+
		"\u0092\7u\2\2\u0092%\3\2\2\2\u0093\u0097\t\4\2\2\u0094\u0096\t\5\2\2\u0095"+
		"\u0094\3\2\2\2\u0096\u0099\3\2\2\2\u0097\u0095\3\2\2\2\u0097\u0098\3\2"+
		"\2\2\u0098\'\3\2\2\2\u0099\u0097\3\2\2\2\u009a\u009f\5\64\32\2\u009b\u009e"+
		"\5J%\2\u009c\u009e\5P(\2\u009d\u009b\3\2\2\2\u009d\u009c\3\2\2\2\u009e"+
		"\u00a1\3\2\2\2\u009f\u00a0\3\2\2\2\u009f\u009d\3\2\2\2\u00a0\u00a2\3\2"+
		"\2\2\u00a1\u009f\3\2\2\2\u00a2\u00a3\5\64\32\2\u00a3\u00af\3\2\2\2\u00a4"+
		"\u00a9\5\62\31\2\u00a5\u00a8\5J%\2\u00a6\u00a8\5P(\2\u00a7\u00a5\3\2\2"+
		"\2\u00a7\u00a6\3\2\2\2\u00a8\u00ab\3\2\2\2\u00a9\u00aa\3\2\2\2\u00a9\u00a7"+
		"\3\2\2\2\u00aa\u00ac\3\2\2\2\u00ab\u00a9\3\2\2\2\u00ac\u00ad\5\62\31\2"+
		"\u00ad\u00af\3\2\2\2\u00ae\u009a\3\2\2\2\u00ae\u00a4\3\2\2\2\u00af)\3"+
		"\2\2\2\u00b0\u00b2\t\6\2\2\u00b1\u00b0\3\2\2\2\u00b2\u00b3\3\2\2\2\u00b3"+
		"\u00b1\3\2\2\2\u00b3\u00b4\3\2\2\2\u00b4+\3\2\2\2\u00b5\u00b6\5T*\2\u00b6"+
		"\u00b7\5*\25\2\u00b7-\3\2\2\2\u00b8\u00ba\5T*\2\u00b9\u00b8\3\2\2\2\u00b9"+
		"\u00ba\3\2\2\2\u00ba\u00bb\3\2\2\2\u00bb\u00bd\5*\25\2\u00bc\u00be\5R"+
		")\2\u00bd\u00bc\3\2\2\2\u00bd\u00be\3\2\2\2\u00be/\3\2\2\2\u00bf\u00c0"+
		"\7.\2\2\u00c0\u00c1\3\2\2\2\u00c1\u00c2\b\30\2\2\u00c2\61\3\2\2\2\u00c3"+
		"\u00c4\7)\2\2\u00c4\63\3\2\2\2\u00c5\u00c6\7$\2\2\u00c6\65\3\2\2\2\u00c7"+
		"\u00c8\7?\2\2\u00c8\u00c9\7\u0080\2\2\u00c9\67\3\2\2\2\u00ca\u00cd\5:"+
		"\35\2\u00cb\u00cd\5<\36\2\u00cc\u00ca\3\2\2\2\u00cc\u00cb\3\2\2\2\u00cd"+
		"9\3\2\2\2\u00ce\u00cf\7(\2\2\u00cf\u00d0\7(\2\2\u00d0;\3\2\2\2\u00d1\u00d2"+
		"\7~\2\2\u00d2\u00d3\7~\2\2\u00d3=\3\2\2\2\u00d4\u00d7\5@ \2\u00d5\u00d7"+
		"\5B!\2\u00d6\u00d4\3\2\2\2\u00d6\u00d5\3\2\2\2\u00d7?\3\2\2\2\u00d8\u00d9"+
		"\7?\2\2\u00d9\u00da\7?\2\2\u00daA\3\2\2\2\u00db\u00dc\7#\2\2\u00dc\u00dd"+
		"\7?\2\2\u00ddC\3\2\2\2\u00de\u00df\7v\2\2\u00df\u00e0\7t\2\2\u00e0\u00e1"+
		"\7w\2\2\u00e1\u00e2\7g\2\2\u00e2E\3\2\2\2\u00e3\u00e4\7h\2\2\u00e4\u00e5"+
		"\7c\2\2\u00e5\u00e6\7n\2\2\u00e6\u00e7\7u\2\2\u00e7\u00e8\7g\2\2\u00e8"+
		"G\3\2\2\2\u00e9\u00ea\7p\2\2\u00ea\u00eb\7w\2\2\u00eb\u00ec\7n\2\2\u00ec"+
		"\u00ed\7n\2\2\u00edI\3\2\2\2\u00ee\u00ef\7^\2\2\u00ef\u0101\t\7\2\2\u00f0"+
		"\u00f1\7^\2\2\u00f1\u00f2\5N\'\2\u00f2\u00f3\5N\'\2\u00f3\u00f4\5N\'\2"+
		"\u00f4\u00f5\5N\'\2\u00f5\u0101\3\2\2\2\u00f6\u00f7\7^\2\2\u00f7\u00f8"+
		"\5N\'\2\u00f8\u00f9\5N\'\2\u00f9\u00fa\5N\'\2\u00fa\u00fb\5N\'\2\u00fb"+
		"\u00fc\5N\'\2\u00fc\u00fd\5N\'\2\u00fd\u00fe\5N\'\2\u00fe\u00ff\5N\'\2"+
		"\u00ff\u0101\3\2\2\2\u0100\u00ee\3\2\2\2\u0100\u00f0\3\2\2\2\u0100\u00f6"+
		"\3\2\2\2\u0101K\3\2\2\2\u0102\u0103\7w\2\2\u0103\u0104\5N\'\2\u0104\u0105"+
		"\5N\'\2\u0105\u0106\5N\'\2\u0106\u0107\5N\'\2\u0107M\3\2\2\2\u0108\u0109"+
		"\t\b\2\2\u0109O\3\2\2\2\u010a\u010b\n\t\2\2\u010bQ\3\2\2\2\u010c\u010e"+
		"\t\n\2\2\u010d\u010f\t\13\2\2\u010e\u010d\3\2\2\2\u010e\u010f\3\2\2\2"+
		"\u010f\u0111\3\2\2\2\u0110\u0112\t\6\2\2\u0111\u0110\3\2\2\2\u0112\u0113"+
		"\3\2\2\2\u0113\u0111\3\2\2\2\u0113\u0114\3\2\2\2\u0114S\3\2\2\2\u0115"+
		"\u0116\7/\2\2\u0116U\3\2\2\2\u0117\u0118\5\62\31\2\u0118\u0119\3\2\2\2"+
		"\u0119\u011a\b+\4\2\u011aW\3\2\2\2\u011b\u011d\t\3\2\2\u011c\u011b\3\2"+
		"\2\2\u011d\u011e\3\2\2\2\u011e\u011c\3\2\2\2\u011e\u011f\3\2\2\2\u011f"+
		"\u0120\3\2\2\2\u0120\u0121\b,\2\2\u0121Y\3\2\2\2\u0122\u0124\n\f\2\2\u0123"+
		"\u0122\3\2\2\2\u0124\u0125\3\2\2\2\u0125\u0123\3\2\2\2\u0125\u0126\3\2"+
		"\2\2\u0126[\3\2\2\2\26\2\3_i\u0097\u009d\u009f\u00a7\u00a9\u00ae\u00b3"+
		"\u00b9\u00bd\u00cc\u00d6\u0100\u010e\u0113\u011e\u0125\5\b\2\2\7\3\2\6"+
		"\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}