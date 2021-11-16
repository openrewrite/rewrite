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
				"MARKUP_OPEN", "DTS_SUBSET_S", "DTD_SUBSET_COMMENT", "MARK_UP_CLOSE",
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
			"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2 \u0152\b\1\b\1\b"+
					"\1\b\1\b\1\b\1\4\2\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b"+
					"\4\t\t\t\4\n\t\n\4\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t"+
					"\20\4\21\t\21\4\22\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t"+
					"\27\4\30\t\30\4\31\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t"+
					"\36\4\37\t\37\4 \t \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t"+
					"(\3\2\3\2\3\2\3\2\3\2\3\2\7\2]\n\2\f\2\16\2`\13\2\3\2\3\2\3\2\3\2\3\3"+
					"\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\7\3q\n\3\f\3\16\3t\13\3\3\3\3"+
					"\3\3\3\3\3\3\4\3\4\3\4\3\4\3\5\3\5\3\5\3\5\3\6\3\6\3\6\3\6\6\6\u0086\n"+
					"\6\r\6\16\6\u0087\3\6\3\6\3\6\3\6\3\6\3\6\3\6\6\6\u0091\n\6\r\6\16\6\u0092"+
					"\3\6\3\6\5\6\u0097\n\6\3\7\3\7\5\7\u009b\n\7\3\7\6\7\u009e\n\7\r\7\16"+
					"\7\u009f\3\7\3\7\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3"+
					"\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\5\b\u00b9\n\b\3\b\3\b\3\t\3\t\3\t\3\t\3"+
					"\n\3\n\3\n\3\n\3\n\3\13\3\13\3\13\3\13\3\13\3\f\6\f\u00cc\n\f\r\f\16\f"+
					"\u00cd\3\r\3\r\3\r\3\r\3\16\3\16\3\16\3\16\3\17\3\17\3\17\3\17\3\20\3"+
					"\20\3\20\3\20\3\20\3\20\3\20\3\20\3\21\3\21\3\21\3\21\3\22\3\22\3\22\3"+
					"\22\3\23\3\23\3\23\3\23\3\24\3\24\3\24\3\24\3\24\3\25\3\25\3\25\3\25\3"+
					"\26\3\26\3\26\3\26\3\27\3\27\3\27\3\27\3\30\3\30\3\30\3\30\3\30\3\31\3"+
					"\31\3\31\3\31\3\32\6\32\u010b\n\32\r\32\16\32\u010c\3\33\3\33\3\33\3\33"+
					"\3\34\3\34\3\34\3\34\3\35\3\35\3\35\3\35\3\36\3\36\3\36\3\36\3\36\3\37"+
					"\3\37\3\37\3\37\3\37\3 \3 \3 \3 \3!\3!\3\"\3\"\3#\3#\7#\u012f\n#\f#\16"+
					"#\u0132\13#\3#\3#\3#\7#\u0137\n#\f#\16#\u013a\13#\3#\5#\u013d\n#\3$\3"+
					"$\7$\u0141\n$\f$\16$\u0144\13$\3%\3%\3&\3&\3\'\3\'\3\'\3\'\5\'\u014e\n"+
					"\'\3(\5(\u0151\n(\4^r\2)\b\3\n\4\f\5\16\6\20\7\22\b\24\t\26\n\30\13\32"+
					"\f\34\r\36\16 \17\"\20$\21&\2(\2*\22,\23.\24\60\2\62\25\64\2\66\268\27"+
					":\30<\2>\31@\32B\33D\34F\35H\36J\37L N\2P\2R\2T\2\b\2\3\4\5\6\7\f\4\2"+
					"\13\13\"\"\4\2((>>\4\2@@]]\5\2\13\f\17\17\"\"\4\2$$>>\4\2))>>\5\2\62;"+
					"CHch\3\2\62;\4\2/\60aa\5\2\u00b9\u00b9\u0302\u0371\u2041\u2042\3\21\2"+
					"<\2<\2C\2\\\2a\2a\2c\2|\2\u00c2\2\u00d8\2\u00da\2\u00f8\2\u00fa\2\u0301"+
					"\2\u0372\2\u037f\2\u0381\2\u2001\2\u200e\2\u200f\2\u2072\2\u2191\2\u3003"+
					"\2\ud801\2\uf902\2\ufdd1\2\ufdf2\2\uffff\2\2\3\1\20\u015a\2\b\3\2\2\2"+
					"\2\n\3\2\2\2\2\f\3\2\2\2\2\16\3\2\2\2\2\20\3\2\2\2\2\22\3\2\2\2\2\24\3"+
					"\2\2\2\2\26\3\2\2\2\2\30\3\2\2\2\2\32\3\2\2\2\2\34\3\2\2\2\3\36\3\2\2"+
					"\2\3 \3\2\2\2\3\"\3\2\2\2\3$\3\2\2\2\3&\3\2\2\2\3(\3\2\2\2\4*\3\2\2\2"+
					"\4,\3\2\2\2\4.\3\2\2\2\4\60\3\2\2\2\5\62\3\2\2\2\5\64\3\2\2\2\5\66\3\2"+
					"\2\2\58\3\2\2\2\6:\3\2\2\2\6<\3\2\2\2\7>\3\2\2\2\7@\3\2\2\2\7B\3\2\2\2"+
					"\7D\3\2\2\2\7F\3\2\2\2\7H\3\2\2\2\7J\3\2\2\2\7L\3\2\2\2\bV\3\2\2\2\ne"+
					"\3\2\2\2\fy\3\2\2\2\16}\3\2\2\2\20\u0096\3\2\2\2\22\u009d\3\2\2\2\24\u00b8"+
					"\3\2\2\2\26\u00bc\3\2\2\2\30\u00c0\3\2\2\2\32\u00c5\3\2\2\2\34\u00cb\3"+
					"\2\2\2\36\u00cf\3\2\2\2 \u00d3\3\2\2\2\"\u00d7\3\2\2\2$\u00db\3\2\2\2"+
					"&\u00e3\3\2\2\2(\u00e7\3\2\2\2*\u00eb\3\2\2\2,\u00ef\3\2\2\2.\u00f4\3"+
					"\2\2\2\60\u00f8\3\2\2\2\62\u00fc\3\2\2\2\64\u0100\3\2\2\2\66\u0105\3\2"+
					"\2\28\u010a\3\2\2\2:\u010e\3\2\2\2<\u0112\3\2\2\2>\u0116\3\2\2\2@\u011a"+
					"\3\2\2\2B\u011f\3\2\2\2D\u0124\3\2\2\2F\u0128\3\2\2\2H\u012a\3\2\2\2J"+
					"\u013c\3\2\2\2L\u013e\3\2\2\2N\u0145\3\2\2\2P\u0147\3\2\2\2R\u014d\3\2"+
					"\2\2T\u0150\3\2\2\2VW\7>\2\2WX\7#\2\2XY\7/\2\2YZ\7/\2\2Z^\3\2\2\2[]\13"+
					"\2\2\2\\[\3\2\2\2]`\3\2\2\2^_\3\2\2\2^\\\3\2\2\2_a\3\2\2\2`^\3\2\2\2a"+
					"b\7/\2\2bc\7/\2\2cd\7@\2\2d\t\3\2\2\2ef\7>\2\2fg\7#\2\2gh\7]\2\2hi\7E"+
					"\2\2ij\7F\2\2jk\7C\2\2kl\7V\2\2lm\7C\2\2mn\7]\2\2nr\3\2\2\2oq\13\2\2\2"+
					"po\3\2\2\2qt\3\2\2\2rs\3\2\2\2rp\3\2\2\2su\3\2\2\2tr\3\2\2\2uv\7_\2\2"+
					"vw\7_\2\2wx\7@\2\2x\13\3\2\2\2yz\7\'\2\2z{\5L$\2{|\7=\2\2|\r\3\2\2\2}"+
					"~\7(\2\2~\177\5L$\2\177\u0080\7=\2\2\u0080\17\3\2\2\2\u0081\u0082\7(\2"+
					"\2\u0082\u0083\7%\2\2\u0083\u0085\3\2\2\2\u0084\u0086\5P&\2\u0085\u0084"+
					"\3\2\2\2\u0086\u0087\3\2\2\2\u0087\u0085\3\2\2\2\u0087\u0088\3\2\2\2\u0088"+
					"\u0089\3\2\2\2\u0089\u008a\7=\2\2\u008a\u0097\3\2\2\2\u008b\u008c\7(\2"+
					"\2\u008c\u008d\7%\2\2\u008d\u008e\7z\2\2\u008e\u0090\3\2\2\2\u008f\u0091"+
					"\5N%\2\u0090\u008f\3\2\2\2\u0091\u0092\3\2\2\2\u0092\u0090\3\2\2\2\u0092"+
					"\u0093\3\2\2\2\u0093\u0094\3\2\2\2\u0094\u0095\7=\2\2\u0095\u0097\3\2"+
					"\2\2\u0096\u0081\3\2\2\2\u0096\u008b\3\2\2\2\u0097\21\3\2\2\2\u0098\u009e"+
					"\t\2\2\2\u0099\u009b\7\17\2\2\u009a\u0099\3\2\2\2\u009a\u009b\3\2\2\2"+
					"\u009b\u009c\3\2\2\2\u009c\u009e\7\f\2\2\u009d\u0098\3\2\2\2\u009d\u009a"+
					"\3\2\2\2\u009e\u009f\3\2\2\2\u009f\u009d\3\2\2\2\u009f\u00a0\3\2\2\2\u00a0"+
					"\u00a1\3\2\2\2\u00a1\u00a2\b\7\2\2\u00a2\23\3\2\2\2\u00a3\u00a4\7>\2\2"+
					"\u00a4\u00a5\7A\2\2\u00a5\u00a6\7z\2\2\u00a6\u00a7\7o\2\2\u00a7\u00a8"+
					"\7n\2\2\u00a8\u00a9\7/\2\2\u00a9\u00aa\7u\2\2\u00aa\u00ab\7v\2\2\u00ab"+
					"\u00ac\7{\2\2\u00ac\u00ad\7n\2\2\u00ad\u00ae\7g\2\2\u00ae\u00af\7u\2\2"+
					"\u00af\u00b0\7j\2\2\u00b0\u00b1\7g\2\2\u00b1\u00b2\7g\2\2\u00b2\u00b9"+
					"\7v\2\2\u00b3\u00b4\7>\2\2\u00b4\u00b5\7A\2\2\u00b5\u00b6\7z\2\2\u00b6"+
					"\u00b7\7o\2\2\u00b7\u00b9\7n\2\2\u00b8\u00a3\3\2\2\2\u00b8\u00b3\3\2\2"+
					"\2\u00b9\u00ba\3\2\2\2\u00ba\u00bb\b\b\3\2\u00bb\25\3\2\2\2\u00bc\u00bd"+
					"\7>\2\2\u00bd\u00be\3\2\2\2\u00be\u00bf\b\t\3\2\u00bf\27\3\2\2\2\u00c0"+
					"\u00c1\7>\2\2\u00c1\u00c2\7A\2\2\u00c2\u00c3\3\2\2\2\u00c3\u00c4\b\n\3"+
					"\2\u00c4\31\3\2\2\2\u00c5\u00c6\7>\2\2\u00c6\u00c7\7#\2\2\u00c7\u00c8"+
					"\3\2\2\2\u00c8\u00c9\b\13\4\2\u00c9\33\3\2\2\2\u00ca\u00cc\n\3\2\2\u00cb"+
					"\u00ca\3\2\2\2\u00cc\u00cd\3\2\2\2\u00cd\u00cb\3\2\2\2\u00cd\u00ce\3\2"+
					"\2\2\u00ce\35\3\2\2\2\u00cf\u00d0\7@\2\2\u00d0\u00d1\3\2\2\2\u00d1\u00d2"+
					"\b\r\5\2\u00d2\37\3\2\2\2\u00d3\u00d4\7]\2\2\u00d4\u00d5\3\2\2\2\u00d5"+
					"\u00d6\b\16\6\2\u00d6!\3\2\2\2\u00d7\u00d8\5D \2\u00d8\u00d9\3\2\2\2\u00d9"+
					"\u00da\b\17\2\2\u00da#\3\2\2\2\u00db\u00dc\7F\2\2\u00dc\u00dd\7Q\2\2\u00dd"+
					"\u00de\7E\2\2\u00de\u00df\7V\2\2\u00df\u00e0\7[\2\2\u00e0\u00e1\7R\2\2"+
					"\u00e1\u00e2\7G\2\2\u00e2%\3\2\2\2\u00e3\u00e4\5L$\2\u00e4\u00e5\3\2\2"+
					"\2\u00e5\u00e6\b\21\7\2\u00e6\'\3\2\2\2\u00e7\u00e8\5J#\2\u00e8\u00e9"+
					"\3\2\2\2\u00e9\u00ea\b\22\b\2\u00ea)\3\2\2\2\u00eb\u00ec\7_\2\2\u00ec"+
					"\u00ed\3\2\2\2\u00ed\u00ee\b\23\5\2\u00ee+\3\2\2\2\u00ef\u00f0\7>\2\2"+
					"\u00f0\u00f1\7#\2\2\u00f1\u00f2\3\2\2\2\u00f2\u00f3\b\24\t\2\u00f3-\3"+
					"\2\2\2\u00f4\u00f5\5D \2\u00f5\u00f6\3\2\2\2\u00f6\u00f7\b\25\2\2\u00f7"+
					"/\3\2\2\2\u00f8\u00f9\5\b\2\2\u00f9\u00fa\3\2\2\2\u00fa\u00fb\b\26\n\2"+
					"\u00fb\61\3\2\2\2\u00fc\u00fd\7@\2\2\u00fd\u00fe\3\2\2\2\u00fe\u00ff\b"+
					"\27\5\2\u00ff\63\3\2\2\2\u0100\u0101\7]\2\2\u0101\u0102\3\2\2\2\u0102"+
					"\u0103\b\30\13\2\u0103\u0104\b\30\f\2\u0104\65\3\2\2\2\u0105\u0106\5D"+
					" \2\u0106\u0107\3\2\2\2\u0107\u0108\b\31\2\2\u0108\67\3\2\2\2\u0109\u010b"+
					"\n\4\2\2\u010a\u0109\3\2\2\2\u010b\u010c\3\2\2\2\u010c\u010a\3\2\2\2\u010c"+
					"\u010d\3\2\2\2\u010d9\3\2\2\2\u010e\u010f\7_\2\2\u010f\u0110\3\2\2\2\u0110"+
					"\u0111\b\33\5\2\u0111;\3\2\2\2\u0112\u0113\13\2\2\2\u0113\u0114\3\2\2"+
					"\2\u0114\u0115\b\34\13\2\u0115=\3\2\2\2\u0116\u0117\7@\2\2\u0117\u0118"+
					"\3\2\2\2\u0118\u0119\b\35\5\2\u0119?\3\2\2\2\u011a\u011b\7A\2\2\u011b"+
					"\u011c\7@\2\2\u011c\u011d\3\2\2\2\u011d\u011e\b\36\5\2\u011eA\3\2\2\2"+
					"\u011f\u0120\7\61\2\2\u0120\u0121\7@\2\2\u0121\u0122\3\2\2\2\u0122\u0123"+
					"\b\37\5\2\u0123C\3\2\2\2\u0124\u0125\t\5\2\2\u0125\u0126\3\2\2\2\u0126"+
					"\u0127\b \2\2\u0127E\3\2\2\2\u0128\u0129\7\61\2\2\u0129G\3\2\2\2\u012a"+
					"\u012b\7?\2\2\u012bI\3\2\2\2\u012c\u0130\7$\2\2\u012d\u012f\n\6\2\2\u012e"+
					"\u012d\3\2\2\2\u012f\u0132\3\2\2\2\u0130\u012e\3\2\2\2\u0130\u0131\3\2"+
					"\2\2\u0131\u0133\3\2\2\2\u0132\u0130\3\2\2\2\u0133\u013d\7$\2\2\u0134"+
					"\u0138\7)\2\2\u0135\u0137\n\7\2\2\u0136\u0135\3\2\2\2\u0137\u013a\3\2"+
					"\2\2\u0138\u0136\3\2\2\2\u0138\u0139\3\2\2\2\u0139\u013b\3\2\2\2\u013a"+
					"\u0138\3\2\2\2\u013b\u013d\7)\2\2\u013c\u012c\3\2\2\2\u013c\u0134\3\2"+
					"\2\2\u013dK\3\2\2\2\u013e\u0142\5T(\2\u013f\u0141\5R\'\2\u0140\u013f\3"+
					"\2\2\2\u0141\u0144\3\2\2\2\u0142\u0140\3\2\2\2\u0142\u0143\3\2\2\2\u0143"+
					"M\3\2\2\2\u0144\u0142\3\2\2\2\u0145\u0146\t\b\2\2\u0146O\3\2\2\2\u0147"+
					"\u0148\t\t\2\2\u0148Q\3\2\2\2\u0149\u014e\5T(\2\u014a\u014e\t\n\2\2\u014b"+
					"\u014e\5P&\2\u014c\u014e\t\13\2\2\u014d\u0149\3\2\2\2\u014d\u014a\3\2"+
					"\2\2\u014d\u014b\3\2\2\2\u014d\u014c\3\2\2\2\u014eS\3\2\2\2\u014f\u0151"+
					"\t\f\2\2\u0150\u014f\3\2\2\2\u0151U\3\2\2\2\31\2\3\4\5\6\7^r\u0087\u0092"+
					"\u0096\u009a\u009d\u009f\u00b8\u00cd\u010c\u0130\u0138\u013c\u0142\u014d"+
					"\u0150\r\b\2\2\7\7\2\7\3\2\6\2\2\7\4\2\t \2\t\37\2\7\5\2\t\3\2\5\2\2\7"+
					"\6\2";
	public static final ATN _ATN =
			new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}