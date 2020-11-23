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
// Generated from /Users/jon/Projects/github/openrewrite/rewrite/rewrite-xml/src/main/antlr/XMLLexer.g4 by ANTLR 4.8
package org.openrewrite.xml.internal.grammar;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class XMLLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.8", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		COMMENT=1, CDATA=2, EntityRef=3, CharRef=4, SEA_WS=5, SPECIAL_OPEN_XML=6, 
		OPEN=7, SPECIAL_OPEN=8, ELEMENT_OPEN=9, TEXT=10, CLOSE=11, SPECIAL_CLOSE=12, 
		SLASH_CLOSE=13, SLASH=14, SUBSET_OPEN=15, SUBSET_CLOSE=16, EQUALS=17, 
		DOCTYPE=18, STRING=19, Name=20, S=21;
	public static final int
		INSIDE=1;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE", "INSIDE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"COMMENT", "CDATA", "EntityRef", "CharRef", "SEA_WS", "SPECIAL_OPEN_XML", 
			"OPEN", "SPECIAL_OPEN", "ELEMENT_OPEN", "TEXT", "CLOSE", "SPECIAL_CLOSE", 
			"SLASH_CLOSE", "SLASH", "SUBSET_OPEN", "SUBSET_CLOSE", "EQUALS", "DOCTYPE", 
			"STRING", "Name", "S", "HEXDIGIT", "DIGIT", "NameChar", "NameStartChar"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, null, null, null, null, "'<'", "'<?'", "'<!'", null, 
			"'>'", "'?>'", "'/>'", "'/'", "'['", "']'", "'='", "'DOCTYPE'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "COMMENT", "CDATA", "EntityRef", "CharRef", "SEA_WS", "SPECIAL_OPEN_XML", 
			"OPEN", "SPECIAL_OPEN", "ELEMENT_OPEN", "TEXT", "CLOSE", "SPECIAL_CLOSE", 
			"SLASH_CLOSE", "SLASH", "SUBSET_OPEN", "SUBSET_CLOSE", "EQUALS", "DOCTYPE", 
			"STRING", "Name", "S"
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


	public XMLLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "XMLLexer.g4"; }

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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\27\u00f3\b\1\b\1"+
		"\4\2\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t"+
		"\n\4\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4"+
		"\22\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4"+
		"\31\t\31\4\32\t\32\3\2\3\2\3\2\3\2\3\2\3\2\7\2=\n\2\f\2\16\2@\13\2\3\2"+
		"\3\2\3\2\3\2\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\7\3Q\n\3\f\3"+
		"\16\3T\13\3\3\3\3\3\3\3\3\3\3\4\3\4\3\4\3\4\3\5\3\5\3\5\3\5\6\5b\n\5\r"+
		"\5\16\5c\3\5\3\5\3\5\3\5\3\5\3\5\3\5\6\5m\n\5\r\5\16\5n\3\5\3\5\5\5s\n"+
		"\5\3\6\3\6\5\6w\n\6\3\6\6\6z\n\6\r\6\16\6{\3\6\3\6\3\7\3\7\3\7\3\7\3\7"+
		"\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\5\7\u0095"+
		"\n\7\3\7\3\7\3\b\3\b\3\b\3\b\3\t\3\t\3\t\3\t\3\t\3\n\3\n\3\n\3\n\3\n\3"+
		"\13\6\13\u00a8\n\13\r\13\16\13\u00a9\3\f\3\f\3\f\3\f\3\r\3\r\3\r\3\r\3"+
		"\r\3\16\3\16\3\16\3\16\3\16\3\17\3\17\3\20\3\20\3\21\3\21\3\22\3\22\3"+
		"\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\24\3\24\7\24\u00cc\n\24\f\24"+
		"\16\24\u00cf\13\24\3\24\3\24\3\24\7\24\u00d4\n\24\f\24\16\24\u00d7\13"+
		"\24\3\24\5\24\u00da\n\24\3\25\3\25\7\25\u00de\n\25\f\25\16\25\u00e1\13"+
		"\25\3\26\3\26\3\26\3\26\3\27\3\27\3\30\3\30\3\31\3\31\3\31\3\31\5\31\u00ef"+
		"\n\31\3\32\5\32\u00f2\n\32\4>R\2\33\4\3\6\4\b\5\n\6\f\7\16\b\20\t\22\n"+
		"\24\13\26\f\30\r\32\16\34\17\36\20 \21\"\22$\23&\24(\25*\26,\27.\2\60"+
		"\2\62\2\64\2\4\2\3\f\4\2\13\13\"\"\4\2((>>\4\2$$>>\4\2))>>\5\2\13\f\17"+
		"\17\"\"\5\2\62;CHch\3\2\62;\4\2/\60aa\5\2\u00b9\u00b9\u0302\u0371\u2041"+
		"\u2042\13\2<<C\\aac|\u2072\u2191\u2c02\u2ff1\u3003\ud801\uf902\ufdd1\ufdf2"+
		"\uffff\2\u00fe\2\4\3\2\2\2\2\6\3\2\2\2\2\b\3\2\2\2\2\n\3\2\2\2\2\f\3\2"+
		"\2\2\2\16\3\2\2\2\2\20\3\2\2\2\2\22\3\2\2\2\2\24\3\2\2\2\2\26\3\2\2\2"+
		"\3\30\3\2\2\2\3\32\3\2\2\2\3\34\3\2\2\2\3\36\3\2\2\2\3 \3\2\2\2\3\"\3"+
		"\2\2\2\3$\3\2\2\2\3&\3\2\2\2\3(\3\2\2\2\3*\3\2\2\2\3,\3\2\2\2\4\66\3\2"+
		"\2\2\6E\3\2\2\2\bY\3\2\2\2\nr\3\2\2\2\fy\3\2\2\2\16\u0094\3\2\2\2\20\u0098"+
		"\3\2\2\2\22\u009c\3\2\2\2\24\u00a1\3\2\2\2\26\u00a7\3\2\2\2\30\u00ab\3"+
		"\2\2\2\32\u00af\3\2\2\2\34\u00b4\3\2\2\2\36\u00b9\3\2\2\2 \u00bb\3\2\2"+
		"\2\"\u00bd\3\2\2\2$\u00bf\3\2\2\2&\u00c1\3\2\2\2(\u00d9\3\2\2\2*\u00db"+
		"\3\2\2\2,\u00e2\3\2\2\2.\u00e6\3\2\2\2\60\u00e8\3\2\2\2\62\u00ee\3\2\2"+
		"\2\64\u00f1\3\2\2\2\66\67\7>\2\2\678\7#\2\289\7/\2\29:\7/\2\2:>\3\2\2"+
		"\2;=\13\2\2\2<;\3\2\2\2=@\3\2\2\2>?\3\2\2\2><\3\2\2\2?A\3\2\2\2@>\3\2"+
		"\2\2AB\7/\2\2BC\7/\2\2CD\7@\2\2D\5\3\2\2\2EF\7>\2\2FG\7#\2\2GH\7]\2\2"+
		"HI\7E\2\2IJ\7F\2\2JK\7C\2\2KL\7V\2\2LM\7C\2\2MN\7]\2\2NR\3\2\2\2OQ\13"+
		"\2\2\2PO\3\2\2\2QT\3\2\2\2RS\3\2\2\2RP\3\2\2\2SU\3\2\2\2TR\3\2\2\2UV\7"+
		"_\2\2VW\7_\2\2WX\7@\2\2X\7\3\2\2\2YZ\7(\2\2Z[\5*\25\2[\\\7=\2\2\\\t\3"+
		"\2\2\2]^\7(\2\2^_\7%\2\2_a\3\2\2\2`b\5\60\30\2a`\3\2\2\2bc\3\2\2\2ca\3"+
		"\2\2\2cd\3\2\2\2de\3\2\2\2ef\7=\2\2fs\3\2\2\2gh\7(\2\2hi\7%\2\2ij\7z\2"+
		"\2jl\3\2\2\2km\5.\27\2lk\3\2\2\2mn\3\2\2\2nl\3\2\2\2no\3\2\2\2op\3\2\2"+
		"\2pq\7=\2\2qs\3\2\2\2r]\3\2\2\2rg\3\2\2\2s\13\3\2\2\2tz\t\2\2\2uw\7\17"+
		"\2\2vu\3\2\2\2vw\3\2\2\2wx\3\2\2\2xz\7\f\2\2yt\3\2\2\2yv\3\2\2\2z{\3\2"+
		"\2\2{y\3\2\2\2{|\3\2\2\2|}\3\2\2\2}~\b\6\2\2~\r\3\2\2\2\177\u0080\7>\2"+
		"\2\u0080\u0081\7A\2\2\u0081\u0082\7z\2\2\u0082\u0083\7o\2\2\u0083\u0084"+
		"\7n\2\2\u0084\u0085\7/\2\2\u0085\u0086\7u\2\2\u0086\u0087\7v\2\2\u0087"+
		"\u0088\7{\2\2\u0088\u0089\7n\2\2\u0089\u008a\7g\2\2\u008a\u008b\7u\2\2"+
		"\u008b\u008c\7j\2\2\u008c\u008d\7g\2\2\u008d\u008e\7g\2\2\u008e\u0095"+
		"\7v\2\2\u008f\u0090\7>\2\2\u0090\u0091\7A\2\2\u0091\u0092\7z\2\2\u0092"+
		"\u0093\7o\2\2\u0093\u0095\7n\2\2\u0094\177\3\2\2\2\u0094\u008f\3\2\2\2"+
		"\u0095\u0096\3\2\2\2\u0096\u0097\b\7\3\2\u0097\17\3\2\2\2\u0098\u0099"+
		"\7>\2\2\u0099\u009a\3\2\2\2\u009a\u009b\b\b\3\2\u009b\21\3\2\2\2\u009c"+
		"\u009d\7>\2\2\u009d\u009e\7A\2\2\u009e\u009f\3\2\2\2\u009f\u00a0\b\t\3"+
		"\2\u00a0\23\3\2\2\2\u00a1\u00a2\7>\2\2\u00a2\u00a3\7#\2\2\u00a3\u00a4"+
		"\3\2\2\2\u00a4\u00a5\b\n\3\2\u00a5\25\3\2\2\2\u00a6\u00a8\n\3\2\2\u00a7"+
		"\u00a6\3\2\2\2\u00a8\u00a9\3\2\2\2\u00a9\u00a7\3\2\2\2\u00a9\u00aa\3\2"+
		"\2\2\u00aa\27\3\2\2\2\u00ab\u00ac\7@\2\2\u00ac\u00ad\3\2\2\2\u00ad\u00ae"+
		"\b\f\4\2\u00ae\31\3\2\2\2\u00af\u00b0\7A\2\2\u00b0\u00b1\7@\2\2\u00b1"+
		"\u00b2\3\2\2\2\u00b2\u00b3\b\r\4\2\u00b3\33\3\2\2\2\u00b4\u00b5\7\61\2"+
		"\2\u00b5\u00b6\7@\2\2\u00b6\u00b7\3\2\2\2\u00b7\u00b8\b\16\4\2\u00b8\35"+
		"\3\2\2\2\u00b9\u00ba\7\61\2\2\u00ba\37\3\2\2\2\u00bb\u00bc\7]\2\2\u00bc"+
		"!\3\2\2\2\u00bd\u00be\7_\2\2\u00be#\3\2\2\2\u00bf\u00c0\7?\2\2\u00c0%"+
		"\3\2\2\2\u00c1\u00c2\7F\2\2\u00c2\u00c3\7Q\2\2\u00c3\u00c4\7E\2\2\u00c4"+
		"\u00c5\7V\2\2\u00c5\u00c6\7[\2\2\u00c6\u00c7\7R\2\2\u00c7\u00c8\7G\2\2"+
		"\u00c8\'\3\2\2\2\u00c9\u00cd\7$\2\2\u00ca\u00cc\n\4\2\2\u00cb\u00ca\3"+
		"\2\2\2\u00cc\u00cf\3\2\2\2\u00cd\u00cb\3\2\2\2\u00cd\u00ce\3\2\2\2\u00ce"+
		"\u00d0\3\2\2\2\u00cf\u00cd\3\2\2\2\u00d0\u00da\7$\2\2\u00d1\u00d5\7)\2"+
		"\2\u00d2\u00d4\n\5\2\2\u00d3\u00d2\3\2\2\2\u00d4\u00d7\3\2\2\2\u00d5\u00d3"+
		"\3\2\2\2\u00d5\u00d6\3\2\2\2\u00d6\u00d8\3\2\2\2\u00d7\u00d5\3\2\2\2\u00d8"+
		"\u00da\7)\2\2\u00d9\u00c9\3\2\2\2\u00d9\u00d1\3\2\2\2\u00da)\3\2\2\2\u00db"+
		"\u00df\5\64\32\2\u00dc\u00de\5\62\31\2\u00dd\u00dc\3\2\2\2\u00de\u00e1"+
		"\3\2\2\2\u00df\u00dd\3\2\2\2\u00df\u00e0\3\2\2\2\u00e0+\3\2\2\2\u00e1"+
		"\u00df\3\2\2\2\u00e2\u00e3\t\6\2\2\u00e3\u00e4\3\2\2\2\u00e4\u00e5\b\26"+
		"\2\2\u00e5-\3\2\2\2\u00e6\u00e7\t\7\2\2\u00e7/\3\2\2\2\u00e8\u00e9\t\b"+
		"\2\2\u00e9\61\3\2\2\2\u00ea\u00ef\5\64\32\2\u00eb\u00ef\t\t\2\2\u00ec"+
		"\u00ef\5\60\30\2\u00ed\u00ef\t\n\2\2\u00ee\u00ea\3\2\2\2\u00ee\u00eb\3"+
		"\2\2\2\u00ee\u00ec\3\2\2\2\u00ee\u00ed\3\2\2\2\u00ef\63\3\2\2\2\u00f0"+
		"\u00f2\t\13\2\2\u00f1\u00f0\3\2\2\2\u00f2\65\3\2\2\2\24\2\3>Rcnrvy{\u0094"+
		"\u00a9\u00cd\u00d5\u00d9\u00df\u00ee\u00f1\5\b\2\2\7\3\2\6\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}