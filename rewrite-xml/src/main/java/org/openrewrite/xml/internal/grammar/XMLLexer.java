/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.xml.internal.grammar;

// Generated from /Users/yoshi/Development/Repos/openrewrite/rewrite/rewrite-xml/src/main/antlr/XMLLexer.g4 by ANTLR 4.9.2
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class XMLLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.9.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
			new PredictionContextCache();
	public static final int
			COMMENT=1, CDATA=2, ParamEntityRef=3, EntityRef=4, CharRef=5, SEA_WS=6,
			SPECIAL_OPEN_XML=7, OPEN=8, SPECIAL_OPEN=9, DTD_OPEN=10, TEXT=11, DTD_CLOSE=12,
			DTD_SUBSET_OPEN=13, DTD_S=14, DOCTYPE=15, DTD_SUBSET_CLOSE=16, MARKUP_OPEN=17,
			DTS_SUBSET_S=18, MARK_UP_CLOSE=19, MARKUP_S=20, MARKUP_TEXT=21, MARKUP_SUBSET=22,
			CLOSE=23, SPECIAL_CLOSE=24, SLASH_CLOSE=25, S=26, SLASH=27, EQUALS=28,
			STRING=29, Name=30;
	public static final int
			INSIDE_DTD=1, INSIDE_DTD_SUBSET=2, INSIDE_MARKUP=3, INSIDE_MARKUP_SUBSET=4,
			INSIDE=5;
	public static String[] channelNames = {
			"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
			"DEFAULT_MODE", "INSIDE_DTD", "INSIDE_DTD_SUBSET", "INSIDE_MARKUP", "INSIDE_MARKUP_SUBSET",
			"INSIDE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
				"COMMENT", "CDATA", "ParamEntityRef", "EntityRef", "CharRef", "SEA_WS",
				"SPECIAL_OPEN_XML", "OPEN", "SPECIAL_OPEN", "DTD_OPEN", "TEXT", "DTD_CLOSE",
				"DTD_SUBSET_OPEN", "DTD_S", "DOCTYPE", "DTD_NAME", "DTD_STRING", "DTD_SUBSET_CLOSE",
				"MARKUP_OPEN", "DTS_SUBSET_S", "DTD_PERef", "DTD_SUBSET_COMMENT", "MARK_UP_CLOSE",
				"MARKUP_SUBSET_OPEN", "MARKUP_S", "MARKUP_TEXT", "MARKUP_SUBSET", "TXT",
				"CLOSE", "SPECIAL_CLOSE", "SLASH_CLOSE", "S", "SLASH", "EQUALS", "STRING",
				"Name", "HEXDIGIT", "DIGIT", "NameChar", "NameStartChar"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
				null, null, null, null, null, null, null, null, "'<'", "'<?'", null,
				null, null, null, null, "'DOCTYPE'", null, null, null, null, null, null,
				null, null, "'?>'", "'/>'", null, "'/'", "'='"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
				null, "COMMENT", "CDATA", "ParamEntityRef", "EntityRef", "CharRef", "SEA_WS",
				"SPECIAL_OPEN_XML", "OPEN", "SPECIAL_OPEN", "DTD_OPEN", "TEXT", "DTD_CLOSE",
				"DTD_SUBSET_OPEN", "DTD_S", "DOCTYPE", "DTD_SUBSET_CLOSE", "MARKUP_OPEN",
				"DTS_SUBSET_S", "MARK_UP_CLOSE", "MARKUP_S", "MARKUP_TEXT", "MARKUP_SUBSET",
				"CLOSE", "SPECIAL_CLOSE", "SLASH_CLOSE", "S", "SLASH", "EQUALS", "STRING",
				"Name"
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
			"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2 \u0158\b\1\b\1\b"+
					"\1\b\1\b\1\b\1\4\2\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b"+
					"\4\t\t\t\4\n\t\n\4\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t"+
					"\20\4\21\t\21\4\22\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t"+
					"\27\4\30\t\30\4\31\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t"+
					"\36\4\37\t\37\4 \t \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t"+
					"(\4)\t)\3\2\3\2\3\2\3\2\3\2\3\2\7\2_\n\2\f\2\16\2b\13\2\3\2\3\2\3\2\3"+
					"\2\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\7\3s\n\3\f\3\16\3v\13\3"+
					"\3\3\3\3\3\3\3\3\3\4\3\4\3\4\3\4\3\5\3\5\3\5\3\5\3\6\3\6\3\6\3\6\6\6\u0088"+
					"\n\6\r\6\16\6\u0089\3\6\3\6\3\6\3\6\3\6\3\6\3\6\6\6\u0093\n\6\r\6\16\6"+
					"\u0094\3\6\3\6\5\6\u0099\n\6\3\7\3\7\5\7\u009d\n\7\3\7\6\7\u00a0\n\7\r"+
					"\7\16\7\u00a1\3\7\3\7\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b"+
					"\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\5\b\u00bb\n\b\3\b\3\b\3\t\3\t\3\t"+
					"\3\t\3\n\3\n\3\n\3\n\3\n\3\13\3\13\3\13\3\13\3\13\3\f\6\f\u00ce\n\f\r"+
					"\f\16\f\u00cf\3\r\3\r\3\r\3\r\3\16\3\16\3\16\3\16\3\17\3\17\3\17\3\17"+
					"\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\21\3\21\3\21\3\21\3\22\3\22"+
					"\3\22\3\22\3\23\3\23\3\23\3\23\3\24\3\24\3\24\3\24\3\24\3\25\3\25\3\25"+
					"\3\25\3\26\3\26\3\26\3\26\3\27\3\27\3\27\3\27\3\30\3\30\3\30\3\30\3\31"+
					"\3\31\3\31\3\31\3\31\3\32\3\32\3\32\3\32\3\33\6\33\u0111\n\33\r\33\16"+
					"\33\u0112\3\34\3\34\3\34\3\34\3\35\3\35\3\35\3\35\3\36\3\36\3\36\3\36"+
					"\3\37\3\37\3\37\3\37\3\37\3 \3 \3 \3 \3 \3!\3!\3!\3!\3\"\3\"\3#\3#\3$"+
					"\3$\7$\u0135\n$\f$\16$\u0138\13$\3$\3$\3$\7$\u013d\n$\f$\16$\u0140\13"+
					"$\3$\5$\u0143\n$\3%\3%\7%\u0147\n%\f%\16%\u014a\13%\3&\3&\3\'\3\'\3(\3"+
					"(\3(\3(\5(\u0154\n(\3)\5)\u0157\n)\4`t\2*\b\3\n\4\f\5\16\6\20\7\22\b\24"+
					"\t\26\n\30\13\32\f\34\r\36\16 \17\"\20$\21&\2(\2*\22,\23.\24\60\2\62\2"+
					"\64\25\66\28\26:\27<\30>\2@\31B\32D\33F\34H\35J\36L\37N P\2R\2T\2V\2\b"+
					"\2\3\4\5\6\7\f\4\2\13\13\"\"\4\2((>>\4\2@@]]\5\2\13\f\17\17\"\"\4\2$$"+
					">>\4\2))>>\5\2\62;CHch\3\2\62;\4\2/\60aa\5\2\u00b9\u00b9\u0302\u0371\u2041"+
					"\u2042\3\21\2<\2<\2C\2\\\2a\2a\2c\2|\2\u00c2\2\u00d8\2\u00da\2\u00f8\2"+
					"\u00fa\2\u0301\2\u0372\2\u037f\2\u0381\2\u2001\2\u200e\2\u200f\2\u2072"+
					"\2\u2191\2\u3003\2\ud801\2\uf902\2\ufdd1\2\ufdf2\2\uffff\2\2\3\1\20\u0160"+
					"\2\b\3\2\2\2\2\n\3\2\2\2\2\f\3\2\2\2\2\16\3\2\2\2\2\20\3\2\2\2\2\22\3"+
					"\2\2\2\2\24\3\2\2\2\2\26\3\2\2\2\2\30\3\2\2\2\2\32\3\2\2\2\2\34\3\2\2"+
					"\2\3\36\3\2\2\2\3 \3\2\2\2\3\"\3\2\2\2\3$\3\2\2\2\3&\3\2\2\2\3(\3\2\2"+
					"\2\4*\3\2\2\2\4,\3\2\2\2\4.\3\2\2\2\4\60\3\2\2\2\4\62\3\2\2\2\5\64\3\2"+
					"\2\2\5\66\3\2\2\2\58\3\2\2\2\5:\3\2\2\2\6<\3\2\2\2\6>\3\2\2\2\7@\3\2\2"+
					"\2\7B\3\2\2\2\7D\3\2\2\2\7F\3\2\2\2\7H\3\2\2\2\7J\3\2\2\2\7L\3\2\2\2\7"+
					"N\3\2\2\2\bX\3\2\2\2\ng\3\2\2\2\f{\3\2\2\2\16\177\3\2\2\2\20\u0098\3\2"+
					"\2\2\22\u009f\3\2\2\2\24\u00ba\3\2\2\2\26\u00be\3\2\2\2\30\u00c2\3\2\2"+
					"\2\32\u00c7\3\2\2\2\34\u00cd\3\2\2\2\36\u00d1\3\2\2\2 \u00d5\3\2\2\2\""+
					"\u00d9\3\2\2\2$\u00dd\3\2\2\2&\u00e5\3\2\2\2(\u00e9\3\2\2\2*\u00ed\3\2"+
					"\2\2,\u00f1\3\2\2\2.\u00f6\3\2\2\2\60\u00fa\3\2\2\2\62\u00fe\3\2\2\2\64"+
					"\u0102\3\2\2\2\66\u0106\3\2\2\28\u010b\3\2\2\2:\u0110\3\2\2\2<\u0114\3"+
					"\2\2\2>\u0118\3\2\2\2@\u011c\3\2\2\2B\u0120\3\2\2\2D\u0125\3\2\2\2F\u012a"+
					"\3\2\2\2H\u012e\3\2\2\2J\u0130\3\2\2\2L\u0142\3\2\2\2N\u0144\3\2\2\2P"+
					"\u014b\3\2\2\2R\u014d\3\2\2\2T\u0153\3\2\2\2V\u0156\3\2\2\2XY\7>\2\2Y"+
					"Z\7#\2\2Z[\7/\2\2[\\\7/\2\2\\`\3\2\2\2]_\13\2\2\2^]\3\2\2\2_b\3\2\2\2"+
					"`a\3\2\2\2`^\3\2\2\2ac\3\2\2\2b`\3\2\2\2cd\7/\2\2de\7/\2\2ef\7@\2\2f\t"+
					"\3\2\2\2gh\7>\2\2hi\7#\2\2ij\7]\2\2jk\7E\2\2kl\7F\2\2lm\7C\2\2mn\7V\2"+
					"\2no\7C\2\2op\7]\2\2pt\3\2\2\2qs\13\2\2\2rq\3\2\2\2sv\3\2\2\2tu\3\2\2"+
					"\2tr\3\2\2\2uw\3\2\2\2vt\3\2\2\2wx\7_\2\2xy\7_\2\2yz\7@\2\2z\13\3\2\2"+
					"\2{|\7\'\2\2|}\5N%\2}~\7=\2\2~\r\3\2\2\2\177\u0080\7(\2\2\u0080\u0081"+
					"\5N%\2\u0081\u0082\7=\2\2\u0082\17\3\2\2\2\u0083\u0084\7(\2\2\u0084\u0085"+
					"\7%\2\2\u0085\u0087\3\2\2\2\u0086\u0088\5R\'\2\u0087\u0086\3\2\2\2\u0088"+
					"\u0089\3\2\2\2\u0089\u0087\3\2\2\2\u0089\u008a\3\2\2\2\u008a\u008b\3\2"+
					"\2\2\u008b\u008c\7=\2\2\u008c\u0099\3\2\2\2\u008d\u008e\7(\2\2\u008e\u008f"+
					"\7%\2\2\u008f\u0090\7z\2\2\u0090\u0092\3\2\2\2\u0091\u0093\5P&\2\u0092"+
					"\u0091\3\2\2\2\u0093\u0094\3\2\2\2\u0094\u0092\3\2\2\2\u0094\u0095\3\2"+
					"\2\2\u0095\u0096\3\2\2\2\u0096\u0097\7=\2\2\u0097\u0099\3\2\2\2\u0098"+
					"\u0083\3\2\2\2\u0098\u008d\3\2\2\2\u0099\21\3\2\2\2\u009a\u00a0\t\2\2"+
					"\2\u009b\u009d\7\17\2\2\u009c\u009b\3\2\2\2\u009c\u009d\3\2\2\2\u009d"+
					"\u009e\3\2\2\2\u009e\u00a0\7\f\2\2\u009f\u009a\3\2\2\2\u009f\u009c\3\2"+
					"\2\2\u00a0\u00a1\3\2\2\2\u00a1\u009f\3\2\2\2\u00a1\u00a2\3\2\2\2\u00a2"+
					"\u00a3\3\2\2\2\u00a3\u00a4\b\7\2\2\u00a4\23\3\2\2\2\u00a5\u00a6\7>\2\2"+
					"\u00a6\u00a7\7A\2\2\u00a7\u00a8\7z\2\2\u00a8\u00a9\7o\2\2\u00a9\u00aa"+
					"\7n\2\2\u00aa\u00ab\7/\2\2\u00ab\u00ac\7u\2\2\u00ac\u00ad\7v\2\2\u00ad"+
					"\u00ae\7{\2\2\u00ae\u00af\7n\2\2\u00af\u00b0\7g\2\2\u00b0\u00b1\7u\2\2"+
					"\u00b1\u00b2\7j\2\2\u00b2\u00b3\7g\2\2\u00b3\u00b4\7g\2\2\u00b4\u00bb"+
					"\7v\2\2\u00b5\u00b6\7>\2\2\u00b6\u00b7\7A\2\2\u00b7\u00b8\7z\2\2\u00b8"+
					"\u00b9\7o\2\2\u00b9\u00bb\7n\2\2\u00ba\u00a5\3\2\2\2\u00ba\u00b5\3\2\2"+
					"\2\u00bb\u00bc\3\2\2\2\u00bc\u00bd\b\b\3\2\u00bd\25\3\2\2\2\u00be\u00bf"+
					"\7>\2\2\u00bf\u00c0\3\2\2\2\u00c0\u00c1\b\t\3\2\u00c1\27\3\2\2\2\u00c2"+
					"\u00c3\7>\2\2\u00c3\u00c4\7A\2\2\u00c4\u00c5\3\2\2\2\u00c5\u00c6\b\n\3"+
					"\2\u00c6\31\3\2\2\2\u00c7\u00c8\7>\2\2\u00c8\u00c9\7#\2\2\u00c9\u00ca"+
					"\3\2\2\2\u00ca\u00cb\b\13\4\2\u00cb\33\3\2\2\2\u00cc\u00ce\n\3\2\2\u00cd"+
					"\u00cc\3\2\2\2\u00ce\u00cf\3\2\2\2\u00cf\u00cd\3\2\2\2\u00cf\u00d0\3\2"+
					"\2\2\u00d0\35\3\2\2\2\u00d1\u00d2\7@\2\2\u00d2\u00d3\3\2\2\2\u00d3\u00d4"+
					"\b\r\5\2\u00d4\37\3\2\2\2\u00d5\u00d6\7]\2\2\u00d6\u00d7\3\2\2\2\u00d7"+
					"\u00d8\b\16\6\2\u00d8!\3\2\2\2\u00d9\u00da\5F!\2\u00da\u00db\3\2\2\2\u00db"+
					"\u00dc\b\17\2\2\u00dc#\3\2\2\2\u00dd\u00de\7F\2\2\u00de\u00df\7Q\2\2\u00df"+
					"\u00e0\7E\2\2\u00e0\u00e1\7V\2\2\u00e1\u00e2\7[\2\2\u00e2\u00e3\7R\2\2"+
					"\u00e3\u00e4\7G\2\2\u00e4%\3\2\2\2\u00e5\u00e6\5N%\2\u00e6\u00e7\3\2\2"+
					"\2\u00e7\u00e8\b\21\7\2\u00e8\'\3\2\2\2\u00e9\u00ea\5L$\2\u00ea\u00eb"+
					"\3\2\2\2\u00eb\u00ec\b\22\b\2\u00ec)\3\2\2\2\u00ed\u00ee\7_\2\2\u00ee"+
					"\u00ef\3\2\2\2\u00ef\u00f0\b\23\5\2\u00f0+\3\2\2\2\u00f1\u00f2\7>\2\2"+
					"\u00f2\u00f3\7#\2\2\u00f3\u00f4\3\2\2\2\u00f4\u00f5\b\24\t\2\u00f5-\3"+
					"\2\2\2\u00f6\u00f7\5F!\2\u00f7\u00f8\3\2\2\2\u00f8\u00f9\b\25\2\2\u00f9"+
					"/\3\2\2\2\u00fa\u00fb\5\f\4\2\u00fb\u00fc\3\2\2\2\u00fc\u00fd\b\26\n\2"+
					"\u00fd\61\3\2\2\2\u00fe\u00ff\5\b\2\2\u00ff\u0100\3\2\2\2\u0100\u0101"+
					"\b\27\13\2\u0101\63\3\2\2\2\u0102\u0103\7@\2\2\u0103\u0104\3\2\2\2\u0104"+
					"\u0105\b\30\5\2\u0105\65\3\2\2\2\u0106\u0107\7]\2\2\u0107\u0108\3\2\2"+
					"\2\u0108\u0109\b\31\f\2\u0109\u010a\b\31\r\2\u010a\67\3\2\2\2\u010b\u010c"+
					"\5F!\2\u010c\u010d\3\2\2\2\u010d\u010e\b\32\2\2\u010e9\3\2\2\2\u010f\u0111"+
					"\n\4\2\2\u0110\u010f\3\2\2\2\u0111\u0112\3\2\2\2\u0112\u0110\3\2\2\2\u0112"+
					"\u0113\3\2\2\2\u0113;\3\2\2\2\u0114\u0115\7_\2\2\u0115\u0116\3\2\2\2\u0116"+
					"\u0117\b\34\5\2\u0117=\3\2\2\2\u0118\u0119\13\2\2\2\u0119\u011a\3\2\2"+
					"\2\u011a\u011b\b\35\f\2\u011b?\3\2\2\2\u011c\u011d\7@\2\2\u011d\u011e"+
					"\3\2\2\2\u011e\u011f\b\36\5\2\u011fA\3\2\2\2\u0120\u0121\7A\2\2\u0121"+
					"\u0122\7@\2\2\u0122\u0123\3\2\2\2\u0123\u0124\b\37\5\2\u0124C\3\2\2\2"+
					"\u0125\u0126\7\61\2\2\u0126\u0127\7@\2\2\u0127\u0128\3\2\2\2\u0128\u0129"+
					"\b \5\2\u0129E\3\2\2\2\u012a\u012b\t\5\2\2\u012b\u012c\3\2\2\2\u012c\u012d"+
					"\b!\2\2\u012dG\3\2\2\2\u012e\u012f\7\61\2\2\u012fI\3\2\2\2\u0130\u0131"+
					"\7?\2\2\u0131K\3\2\2\2\u0132\u0136\7$\2\2\u0133\u0135\n\6\2\2\u0134\u0133"+
					"\3\2\2\2\u0135\u0138\3\2\2\2\u0136\u0134\3\2\2\2\u0136\u0137\3\2\2\2\u0137"+
					"\u0139\3\2\2\2\u0138\u0136\3\2\2\2\u0139\u0143\7$\2\2\u013a\u013e\7)\2"+
					"\2\u013b\u013d\n\7\2\2\u013c\u013b\3\2\2\2\u013d\u0140\3\2\2\2\u013e\u013c"+
					"\3\2\2\2\u013e\u013f\3\2\2\2\u013f\u0141\3\2\2\2\u0140\u013e\3\2\2\2\u0141"+
					"\u0143\7)\2\2\u0142\u0132\3\2\2\2\u0142\u013a\3\2\2\2\u0143M\3\2\2\2\u0144"+
					"\u0148\5V)\2\u0145\u0147\5T(\2\u0146\u0145\3\2\2\2\u0147\u014a\3\2\2\2"+
					"\u0148\u0146\3\2\2\2\u0148\u0149\3\2\2\2\u0149O\3\2\2\2\u014a\u0148\3"+
					"\2\2\2\u014b\u014c\t\b\2\2\u014cQ\3\2\2\2\u014d\u014e\t\t\2\2\u014eS\3"+
					"\2\2\2\u014f\u0154\5V)\2\u0150\u0154\t\n\2\2\u0151\u0154\5R\'\2\u0152"+
					"\u0154\t\13\2\2\u0153\u014f\3\2\2\2\u0153\u0150\3\2\2\2\u0153\u0151\3"+
					"\2\2\2\u0153\u0152\3\2\2\2\u0154U\3\2\2\2\u0155\u0157\t\f\2\2\u0156\u0155"+
					"\3\2\2\2\u0157W\3\2\2\2\31\2\3\4\5\6\7`t\u0089\u0094\u0098\u009c\u009f"+
					"\u00a1\u00ba\u00cf\u0112\u0136\u013e\u0142\u0148\u0153\u0156\16\b\2\2"+
					"\7\7\2\7\3\2\6\2\2\7\4\2\t \2\t\37\2\7\5\2\t\5\2\t\3\2\5\2\2\7\6\2";
	public static final ATN _ATN =
			new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}