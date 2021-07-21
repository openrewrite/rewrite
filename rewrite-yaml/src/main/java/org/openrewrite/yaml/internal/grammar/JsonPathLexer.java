/*
 *  Copyright 2021 the original author or authors.
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  https://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

// Generated from /Users/jbrisbin/src/github.com/openrewrite/rewrite/rewrite-yaml/src/main/antlr/JsonPathLexer.g4 by ANTLR 4.9.2
package org.openrewrite.yaml.internal.grammar;
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
	static { RuntimeMetaData.checkVersion("4.9.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		StringLiteral=1, NumericLiteral=2, AT=3, DOT_DOT=4, DOT=5, ROOT=6, WILDCARD=7, 
		AND=8, EQ=9, GE=10, GT=11, LE=12, LT=13, NE=14, NOT=15, OR=16, TRUE=17, 
		FALSE=18, NULL=19, LBRACE=20, RBRACE=21, LBRACK=22, RBRACK=23, COLON=24, 
		COMMA=25, LPAREN=26, RPAREN=27, QUESTION=28, Identifier=29, WS=30;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"StringLiteral", "EscapeSequence", "UNICODE", "HexDigit", "SAFECODEPOINT", 
			"NumericLiteral", "ExponentPart", "AT", "DOT_DOT", "DOT", "ROOT", "WILDCARD", 
			"AND", "EQ", "GE", "GT", "LE", "LT", "NE", "NOT", "OR", "TRUE", "FALSE", 
			"NULL", "LBRACE", "RBRACE", "LBRACK", "RBRACK", "COLON", "COMMA", "LPAREN", 
			"RPAREN", "QUESTION", "Identifier", "WS"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, "'@'", "'..'", "'.'", "'$'", "'*'", "'and'", "'=='", 
			"'>='", "'>'", "'<='", "'<'", "'!='", "'not'", "'or'", "'true'", "'false'", 
			"'null'", "'{'", "'}'", "'['", "']'", "':'", "','", "'('", "')'", "'?'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "StringLiteral", "NumericLiteral", "AT", "DOT_DOT", "DOT", "ROOT", 
			"WILDCARD", "AND", "EQ", "GE", "GT", "LE", "LT", "NE", "NOT", "OR", "TRUE", 
			"FALSE", "NULL", "LBRACE", "RBRACE", "LBRACK", "RBRACK", "COLON", "COMMA", 
			"LPAREN", "RPAREN", "QUESTION", "Identifier", "WS"
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2 \u00e5\b\1\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\3\2\3\2\3\2\7\2M\n\2\f\2\16\2P\13\2\3\2\3\2\3"+
		"\2\3\2\7\2V\n\2\f\2\16\2Y\13\2\3\2\5\2\\\n\2\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\5\3p\n\3\3\4\3\4\3\4\3"+
		"\4\3\4\3\4\3\5\3\5\3\6\3\6\3\7\5\7}\n\7\3\7\6\7\u0080\n\7\r\7\16\7\u0081"+
		"\3\7\5\7\u0085\n\7\3\b\3\b\5\b\u0089\n\b\3\b\6\b\u008c\n\b\r\b\16\b\u008d"+
		"\3\t\3\t\3\n\3\n\3\n\3\13\3\13\3\f\3\f\3\r\3\r\3\16\3\16\3\16\3\16\3\17"+
		"\3\17\3\17\3\20\3\20\3\20\3\21\3\21\3\22\3\22\3\22\3\23\3\23\3\24\3\24"+
		"\3\24\3\25\3\25\3\25\3\25\3\26\3\26\3\26\3\27\3\27\3\27\3\27\3\27\3\30"+
		"\3\30\3\30\3\30\3\30\3\30\3\31\3\31\3\31\3\31\3\31\3\32\3\32\3\33\3\33"+
		"\3\34\3\34\3\35\3\35\3\36\3\36\3\37\3\37\3 \3 \3!\3!\3\"\3\"\3#\3#\7#"+
		"\u00da\n#\f#\16#\u00dd\13#\3$\6$\u00e0\n$\r$\16$\u00e1\3$\3$\2\2%\3\3"+
		"\5\2\7\2\t\2\13\2\r\4\17\2\21\5\23\6\25\7\27\b\31\t\33\n\35\13\37\f!\r"+
		"#\16%\17\'\20)\21+\22-\23/\24\61\25\63\26\65\27\67\309\31;\32=\33?\34"+
		"A\35C\36E\37G \3\2\13\7\2$$^^ppttvv\5\2\62;CHch\5\2\2!$$^^\3\2\62;\4\2"+
		"GGgg\4\2--//\5\2C\\aac|\6\2\62;C\\aac|\5\2\13\f\16\17\"\"\2\u00ed\2\3"+
		"\3\2\2\2\2\r\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2"+
		"\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3"+
		"\2\2\2\2%\3\2\2\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2"+
		"\2\2\2\61\3\2\2\2\2\63\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2"+
		";\3\2\2\2\2=\3\2\2\2\2?\3\2\2\2\2A\3\2\2\2\2C\3\2\2\2\2E\3\2\2\2\2G\3"+
		"\2\2\2\3[\3\2\2\2\5o\3\2\2\2\7q\3\2\2\2\tw\3\2\2\2\13y\3\2\2\2\r|\3\2"+
		"\2\2\17\u0086\3\2\2\2\21\u008f\3\2\2\2\23\u0091\3\2\2\2\25\u0094\3\2\2"+
		"\2\27\u0096\3\2\2\2\31\u0098\3\2\2\2\33\u009a\3\2\2\2\35\u009e\3\2\2\2"+
		"\37\u00a1\3\2\2\2!\u00a4\3\2\2\2#\u00a6\3\2\2\2%\u00a9\3\2\2\2\'\u00ab"+
		"\3\2\2\2)\u00ae\3\2\2\2+\u00b2\3\2\2\2-\u00b5\3\2\2\2/\u00ba\3\2\2\2\61"+
		"\u00c0\3\2\2\2\63\u00c5\3\2\2\2\65\u00c7\3\2\2\2\67\u00c9\3\2\2\29\u00cb"+
		"\3\2\2\2;\u00cd\3\2\2\2=\u00cf\3\2\2\2?\u00d1\3\2\2\2A\u00d3\3\2\2\2C"+
		"\u00d5\3\2\2\2E\u00d7\3\2\2\2G\u00df\3\2\2\2IN\7$\2\2JM\5\5\3\2KM\5\13"+
		"\6\2LJ\3\2\2\2LK\3\2\2\2MP\3\2\2\2NL\3\2\2\2NO\3\2\2\2OQ\3\2\2\2PN\3\2"+
		"\2\2Q\\\7$\2\2RW\7)\2\2SV\5\5\3\2TV\5\13\6\2US\3\2\2\2UT\3\2\2\2VY\3\2"+
		"\2\2WU\3\2\2\2WX\3\2\2\2XZ\3\2\2\2YW\3\2\2\2Z\\\7)\2\2[I\3\2\2\2[R\3\2"+
		"\2\2\\\4\3\2\2\2]^\7^\2\2^p\t\2\2\2_`\7^\2\2`a\5\t\5\2ab\5\t\5\2bc\5\t"+
		"\5\2cd\5\t\5\2dp\3\2\2\2ef\7^\2\2fg\5\t\5\2gh\5\t\5\2hi\5\t\5\2ij\5\t"+
		"\5\2jk\5\t\5\2kl\5\t\5\2lm\5\t\5\2mn\5\t\5\2np\3\2\2\2o]\3\2\2\2o_\3\2"+
		"\2\2oe\3\2\2\2p\6\3\2\2\2qr\7w\2\2rs\5\t\5\2st\5\t\5\2tu\5\t\5\2uv\5\t"+
		"\5\2v\b\3\2\2\2wx\t\3\2\2x\n\3\2\2\2yz\n\4\2\2z\f\3\2\2\2{}\7/\2\2|{\3"+
		"\2\2\2|}\3\2\2\2}\177\3\2\2\2~\u0080\t\5\2\2\177~\3\2\2\2\u0080\u0081"+
		"\3\2\2\2\u0081\177\3\2\2\2\u0081\u0082\3\2\2\2\u0082\u0084\3\2\2\2\u0083"+
		"\u0085\5\17\b\2\u0084\u0083\3\2\2\2\u0084\u0085\3\2\2\2\u0085\16\3\2\2"+
		"\2\u0086\u0088\t\6\2\2\u0087\u0089\t\7\2\2\u0088\u0087\3\2\2\2\u0088\u0089"+
		"\3\2\2\2\u0089\u008b\3\2\2\2\u008a\u008c\t\5\2\2\u008b\u008a\3\2\2\2\u008c"+
		"\u008d\3\2\2\2\u008d\u008b\3\2\2\2\u008d\u008e\3\2\2\2\u008e\20\3\2\2"+
		"\2\u008f\u0090\7B\2\2\u0090\22\3\2\2\2\u0091\u0092\7\60\2\2\u0092\u0093"+
		"\7\60\2\2\u0093\24\3\2\2\2\u0094\u0095\7\60\2\2\u0095\26\3\2\2\2\u0096"+
		"\u0097\7&\2\2\u0097\30\3\2\2\2\u0098\u0099\7,\2\2\u0099\32\3\2\2\2\u009a"+
		"\u009b\7c\2\2\u009b\u009c\7p\2\2\u009c\u009d\7f\2\2\u009d\34\3\2\2\2\u009e"+
		"\u009f\7?\2\2\u009f\u00a0\7?\2\2\u00a0\36\3\2\2\2\u00a1\u00a2\7@\2\2\u00a2"+
		"\u00a3\7?\2\2\u00a3 \3\2\2\2\u00a4\u00a5\7@\2\2\u00a5\"\3\2\2\2\u00a6"+
		"\u00a7\7>\2\2\u00a7\u00a8\7?\2\2\u00a8$\3\2\2\2\u00a9\u00aa\7>\2\2\u00aa"+
		"&\3\2\2\2\u00ab\u00ac\7#\2\2\u00ac\u00ad\7?\2\2\u00ad(\3\2\2\2\u00ae\u00af"+
		"\7p\2\2\u00af\u00b0\7q\2\2\u00b0\u00b1\7v\2\2\u00b1*\3\2\2\2\u00b2\u00b3"+
		"\7q\2\2\u00b3\u00b4\7t\2\2\u00b4,\3\2\2\2\u00b5\u00b6\7v\2\2\u00b6\u00b7"+
		"\7t\2\2\u00b7\u00b8\7w\2\2\u00b8\u00b9\7g\2\2\u00b9.\3\2\2\2\u00ba\u00bb"+
		"\7h\2\2\u00bb\u00bc\7c\2\2\u00bc\u00bd\7n\2\2\u00bd\u00be\7u\2\2\u00be"+
		"\u00bf\7g\2\2\u00bf\60\3\2\2\2\u00c0\u00c1\7p\2\2\u00c1\u00c2\7w\2\2\u00c2"+
		"\u00c3\7n\2\2\u00c3\u00c4\7n\2\2\u00c4\62\3\2\2\2\u00c5\u00c6\7}\2\2\u00c6"+
		"\64\3\2\2\2\u00c7\u00c8\7\177\2\2\u00c8\66\3\2\2\2\u00c9\u00ca\7]\2\2"+
		"\u00ca8\3\2\2\2\u00cb\u00cc\7_\2\2\u00cc:\3\2\2\2\u00cd\u00ce\7<\2\2\u00ce"+
		"<\3\2\2\2\u00cf\u00d0\7.\2\2\u00d0>\3\2\2\2\u00d1\u00d2\7*\2\2\u00d2@"+
		"\3\2\2\2\u00d3\u00d4\7+\2\2\u00d4B\3\2\2\2\u00d5\u00d6\7A\2\2\u00d6D\3"+
		"\2\2\2\u00d7\u00db\t\b\2\2\u00d8\u00da\t\t\2\2\u00d9\u00d8\3\2\2\2\u00da"+
		"\u00dd\3\2\2\2\u00db\u00d9\3\2\2\2\u00db\u00dc\3\2\2\2\u00dcF\3\2\2\2"+
		"\u00dd\u00db\3\2\2\2\u00de\u00e0\t\n\2\2\u00df\u00de\3\2\2\2\u00e0\u00e1"+
		"\3\2\2\2\u00e1\u00df\3\2\2\2\u00e1\u00e2\3\2\2\2\u00e2\u00e3\3\2\2\2\u00e3"+
		"\u00e4\b$\2\2\u00e4H\3\2\2\2\20\2LNUW[o|\u0081\u0084\u0088\u008d\u00db"+
		"\u00e1\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}