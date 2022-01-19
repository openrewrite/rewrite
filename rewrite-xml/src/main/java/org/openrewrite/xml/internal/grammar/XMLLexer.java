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
package org.openrewrite.xml.internal.grammar;

// Generated from /Users/pat/dev/openrewrite/rewrite/rewrite-xml/src/main/antlr/XMLLexer.g4 by ANTLR 4.9.2

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
			PI_S=23, PI_TEXT=24, CLOSE=25, SPECIAL_CLOSE=26, SLASH_CLOSE=27, S=28,
			SLASH=29, EQUALS=30, STRING=31, Name=32;
	public static final int
			INSIDE_DTD=1, INSIDE_DTD_SUBSET=2, INSIDE_MARKUP=3, INSIDE_MARKUP_SUBSET=4,
			INSIDE_PROCESS_INSTRUCTION=5, INSIDE=6;
	public static String[] channelNames = {
			"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
			"DEFAULT_MODE", "INSIDE_DTD", "INSIDE_DTD_SUBSET", "INSIDE_MARKUP", "INSIDE_MARKUP_SUBSET",
			"INSIDE_PROCESS_INSTRUCTION", "INSIDE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
				"COMMENT", "CDATA", "ParamEntityRef", "EntityRef", "CharRef", "SEA_WS",
				"SPECIAL_OPEN_XML", "OPEN", "SPECIAL_OPEN", "DTD_OPEN", "TEXT", "DTD_CLOSE",
				"DTD_SUBSET_OPEN", "DTD_S", "DOCTYPE", "DTD_NAME", "DTD_STRING", "DTD_SUBSET_CLOSE",
				"MARKUP_OPEN", "DTS_SUBSET_S", "DTD_PERef", "DTD_SUBSET_COMMENT", "MARK_UP_CLOSE",
				"MARKUP_SUBSET_OPEN", "MARKUP_S", "MARKUP_TEXT", "MARKUP_SUBSET", "TXT",
				"PI_SPECIAL_CLOSE", "PI_S", "PI_TEXT", "CLOSE", "SPECIAL_CLOSE", "SLASH_CLOSE",
				"S", "SLASH", "EQUALS", "STRING", "Name", "HEXDIGIT", "DIGIT", "NameChar",
				"NameStartChar"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
				null, null, null, null, null, null, null, null, "'<'", null, null, null,
				null, null, null, "'DOCTYPE'", null, null, null, null, null, null, null,
				null, null, null, "'?>'", "'/>'", null, "'/'", "'='"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
				null, "COMMENT", "CDATA", "ParamEntityRef", "EntityRef", "CharRef", "SEA_WS",
				"SPECIAL_OPEN_XML", "OPEN", "SPECIAL_OPEN", "DTD_OPEN", "TEXT", "DTD_CLOSE",
				"DTD_SUBSET_OPEN", "DTD_S", "DOCTYPE", "DTD_SUBSET_CLOSE", "MARKUP_OPEN",
				"DTS_SUBSET_S", "MARK_UP_CLOSE", "MARKUP_S", "MARKUP_TEXT", "MARKUP_SUBSET",
				"PI_S", "PI_TEXT", "CLOSE", "SPECIAL_CLOSE", "SLASH_CLOSE", "S", "SLASH",
				"EQUALS", "STRING", "Name"
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
			"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\"\u0161\b\1\b\1\b"+
					"\1\b\1\b\1\b\1\b\1\4\2\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b"+
					"\t\b\4\t\t\t\4\n\t\n\4\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20"+
					"\t\20\4\21\t\21\4\22\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27"+
					"\t\27\4\30\t\30\4\31\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36"+
					"\t\36\4\37\t\37\4 \t \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4"+
					"(\t(\4)\t)\4*\t*\4+\t+\4,\t,\3\2\3\2\3\2\3\2\3\2\3\2\7\2f\n\2\f\2\16\2"+
					"i\13\2\3\2\3\2\3\2\3\2\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\7\3"+
					"z\n\3\f\3\16\3}\13\3\3\3\3\3\3\3\3\3\3\4\3\4\3\4\3\4\3\5\3\5\3\5\3\5\3"+
					"\6\3\6\3\6\3\6\6\6\u008f\n\6\r\6\16\6\u0090\3\6\3\6\3\6\3\6\3\6\3\6\3"+
					"\6\6\6\u009a\n\6\r\6\16\6\u009b\3\6\3\6\5\6\u00a0\n\6\3\7\3\7\5\7\u00a4"+
					"\n\7\3\7\3\7\6\7\u00a8\n\7\r\7\16\7\u00a9\3\7\3\7\3\b\3\b\3\b\3\b\3\b"+
					"\3\b\3\b\3\b\3\b\3\b\3\t\3\t\3\t\3\t\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\13"+
					"\3\13\3\13\3\13\3\13\3\f\6\f\u00c9\n\f\r\f\16\f\u00ca\3\r\3\r\3\r\3\r"+
					"\3\16\3\16\3\16\3\16\3\17\3\17\3\17\3\17\3\20\3\20\3\20\3\20\3\20\3\20"+
					"\3\20\3\20\3\21\3\21\3\21\3\21\3\22\3\22\3\22\3\22\3\23\3\23\3\23\3\23"+
					"\3\24\3\24\3\24\3\24\3\24\3\25\3\25\3\25\3\25\3\26\3\26\3\26\3\26\3\27"+
					"\3\27\3\27\3\27\3\30\3\30\3\30\3\30\3\31\3\31\3\31\3\31\3\31\3\32\3\32"+
					"\3\32\3\32\3\33\6\33\u010c\n\33\r\33\16\33\u010d\3\34\3\34\3\34\3\34\3"+
					"\35\3\35\3\35\3\35\3\36\3\36\3\36\3\36\3\36\3\37\3\37\3\37\3\37\3 \6 "+
					"\u0122\n \r \16 \u0123\3!\3!\3!\3!\3\"\3\"\3\"\3\"\3\"\3#\3#\3#\3#\3#"+
					"\3$\3$\3$\3$\3%\3%\3&\3&\3\'\3\'\7\'\u013e\n\'\f\'\16\'\u0141\13\'\3\'"+
					"\3\'\3\'\7\'\u0146\n\'\f\'\16\'\u0149\13\'\3\'\5\'\u014c\n\'\3(\3(\7("+
					"\u0150\n(\f(\16(\u0153\13(\3)\3)\3*\3*\3+\3+\3+\3+\5+\u015d\n+\3,\5,\u0160"+
					"\n,\4g{\2-\t\3\13\4\r\5\17\6\21\7\23\b\25\t\27\n\31\13\33\f\35\r\37\16"+
					"!\17#\20%\21\'\2)\2+\22-\23/\24\61\2\63\2\65\25\67\29\26;\27=\30?\2A\2"+
					"C\31E\32G\33I\34K\35M\36O\37Q S!U\"W\2Y\2[\2]\2\t\2\3\4\5\6\7\b\r\4\2"+
					"\13\13\"\"\4\2((>>\4\2@@]]\3\2@A\5\2\13\f\17\17\"\"\4\2$$>>\4\2))>>\5"+
					"\2\62;CHch\3\2\62;\4\2/\60aa\5\2\u00b9\u00b9\u0302\u0371\u2041\u2042\3"+
					"\21\2<\2<\2C\2\\\2a\2a\2c\2|\2\u00c2\2\u00d8\2\u00da\2\u00f8\2\u00fa\2"+
					"\u0301\2\u0372\2\u037f\2\u0381\2\u2001\2\u200e\2\u200f\2\u2072\2\u2191"+
					"\2\u3003\2\ud801\2\uf902\2\ufdd1\2\ufdf2\2\uffff\2\2\3\1\20\u0169\2\t"+
					"\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2"+
					"\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\3"+
					"\37\3\2\2\2\3!\3\2\2\2\3#\3\2\2\2\3%\3\2\2\2\3\'\3\2\2\2\3)\3\2\2\2\4"+
					"+\3\2\2\2\4-\3\2\2\2\4/\3\2\2\2\4\61\3\2\2\2\4\63\3\2\2\2\5\65\3\2\2\2"+
					"\5\67\3\2\2\2\59\3\2\2\2\5;\3\2\2\2\6=\3\2\2\2\6?\3\2\2\2\7A\3\2\2\2\7"+
					"C\3\2\2\2\7E\3\2\2\2\bG\3\2\2\2\bI\3\2\2\2\bK\3\2\2\2\bM\3\2\2\2\bO\3"+
					"\2\2\2\bQ\3\2\2\2\bS\3\2\2\2\bU\3\2\2\2\t_\3\2\2\2\13n\3\2\2\2\r\u0082"+
					"\3\2\2\2\17\u0086\3\2\2\2\21\u009f\3\2\2\2\23\u00a7\3\2\2\2\25\u00ad\3"+
					"\2\2\2\27\u00b7\3\2\2\2\31\u00bb\3\2\2\2\33\u00c2\3\2\2\2\35\u00c8\3\2"+
					"\2\2\37\u00cc\3\2\2\2!\u00d0\3\2\2\2#\u00d4\3\2\2\2%\u00d8\3\2\2\2\'\u00e0"+
					"\3\2\2\2)\u00e4\3\2\2\2+\u00e8\3\2\2\2-\u00ec\3\2\2\2/\u00f1\3\2\2\2\61"+
					"\u00f5\3\2\2\2\63\u00f9\3\2\2\2\65\u00fd\3\2\2\2\67\u0101\3\2\2\29\u0106"+
					"\3\2\2\2;\u010b\3\2\2\2=\u010f\3\2\2\2?\u0113\3\2\2\2A\u0117\3\2\2\2C"+
					"\u011c\3\2\2\2E\u0121\3\2\2\2G\u0125\3\2\2\2I\u0129\3\2\2\2K\u012e\3\2"+
					"\2\2M\u0133\3\2\2\2O\u0137\3\2\2\2Q\u0139\3\2\2\2S\u014b\3\2\2\2U\u014d"+
					"\3\2\2\2W\u0154\3\2\2\2Y\u0156\3\2\2\2[\u015c\3\2\2\2]\u015f\3\2\2\2_"+
					"`\7>\2\2`a\7#\2\2ab\7/\2\2bc\7/\2\2cg\3\2\2\2df\13\2\2\2ed\3\2\2\2fi\3"+
					"\2\2\2gh\3\2\2\2ge\3\2\2\2hj\3\2\2\2ig\3\2\2\2jk\7/\2\2kl\7/\2\2lm\7@"+
					"\2\2m\n\3\2\2\2no\7>\2\2op\7#\2\2pq\7]\2\2qr\7E\2\2rs\7F\2\2st\7C\2\2"+
					"tu\7V\2\2uv\7C\2\2vw\7]\2\2w{\3\2\2\2xz\13\2\2\2yx\3\2\2\2z}\3\2\2\2{"+
					"|\3\2\2\2{y\3\2\2\2|~\3\2\2\2}{\3\2\2\2~\177\7_\2\2\177\u0080\7_\2\2\u0080"+
					"\u0081\7@\2\2\u0081\f\3\2\2\2\u0082\u0083\7\'\2\2\u0083\u0084\5U(\2\u0084"+
					"\u0085\7=\2\2\u0085\16\3\2\2\2\u0086\u0087\7(\2\2\u0087\u0088\5U(\2\u0088"+
					"\u0089\7=\2\2\u0089\20\3\2\2\2\u008a\u008b\7(\2\2\u008b\u008c\7%\2\2\u008c"+
					"\u008e\3\2\2\2\u008d\u008f\5Y*\2\u008e\u008d\3\2\2\2\u008f\u0090\3\2\2"+
					"\2\u0090\u008e\3\2\2\2\u0090\u0091\3\2\2\2\u0091\u0092\3\2\2\2\u0092\u0093"+
					"\7=\2\2\u0093\u00a0\3\2\2\2\u0094\u0095\7(\2\2\u0095\u0096\7%\2\2\u0096"+
					"\u0097\7z\2\2\u0097\u0099\3\2\2\2\u0098\u009a\5W)\2\u0099\u0098\3\2\2"+
					"\2\u009a\u009b\3\2\2\2\u009b\u0099\3\2\2\2\u009b\u009c\3\2\2\2\u009c\u009d"+
					"\3\2\2\2\u009d\u009e\7=\2\2\u009e\u00a0\3\2\2\2\u009f\u008a\3\2\2\2\u009f"+
					"\u0094\3\2\2\2\u00a0\22\3\2\2\2\u00a1\u00a8\t\2\2\2\u00a2\u00a4\7\17\2"+
					"\2\u00a3\u00a2\3\2\2\2\u00a3\u00a4\3\2\2\2\u00a4\u00a5\3\2\2\2\u00a5\u00a8"+
					"\7\f\2\2\u00a6\u00a8\7\uff01\2\2\u00a7\u00a1\3\2\2\2\u00a7\u00a3\3\2\2"+
					"\2\u00a7\u00a6\3\2\2\2\u00a8\u00a9\3\2\2\2\u00a9\u00a7\3\2\2\2\u00a9\u00aa"+
					"\3\2\2\2\u00aa\u00ab\3\2\2\2\u00ab\u00ac\b\7\2\2\u00ac\24\3\2\2\2\u00ad"+
					"\u00ae\7>\2\2\u00ae\u00af\7A\2\2\u00af\u00b0\7z\2\2\u00b0\u00b1\7o\2\2"+
					"\u00b1\u00b2\7n\2\2\u00b2\u00b3\3\2\2\2\u00b3\u00b4\5M$\2\u00b4\u00b5"+
					"\3\2\2\2\u00b5\u00b6\b\b\3\2\u00b6\26\3\2\2\2\u00b7\u00b8\7>\2\2\u00b8"+
					"\u00b9\3\2\2\2\u00b9\u00ba\b\t\3\2\u00ba\30\3\2\2\2\u00bb\u00bc\7>\2\2"+
					"\u00bc\u00bd\7A\2\2\u00bd\u00be\3\2\2\2\u00be\u00bf\5U(\2\u00bf\u00c0"+
					"\3\2\2\2\u00c0\u00c1\b\n\4\2\u00c1\32\3\2\2\2\u00c2\u00c3\7>\2\2\u00c3"+
					"\u00c4\7#\2\2\u00c4\u00c5\3\2\2\2\u00c5\u00c6\b\13\5\2\u00c6\34\3\2\2"+
					"\2\u00c7\u00c9\n\3\2\2\u00c8\u00c7\3\2\2\2\u00c9\u00ca\3\2\2\2\u00ca\u00c8"+
					"\3\2\2\2\u00ca\u00cb\3\2\2\2\u00cb\36\3\2\2\2\u00cc\u00cd\7@\2\2\u00cd"+
					"\u00ce\3\2\2\2\u00ce\u00cf\b\r\6\2\u00cf \3\2\2\2\u00d0\u00d1\7]\2\2\u00d1"+
					"\u00d2\3\2\2\2\u00d2\u00d3\b\16\7\2\u00d3\"\3\2\2\2\u00d4\u00d5\5M$\2"+
					"\u00d5\u00d6\3\2\2\2\u00d6\u00d7\b\17\2\2\u00d7$\3\2\2\2\u00d8\u00d9\7"+
					"F\2\2\u00d9\u00da\7Q\2\2\u00da\u00db\7E\2\2\u00db\u00dc\7V\2\2\u00dc\u00dd"+
					"\7[\2\2\u00dd\u00de\7R\2\2\u00de\u00df\7G\2\2\u00df&\3\2\2\2\u00e0\u00e1"+
					"\5U(\2\u00e1\u00e2\3\2\2\2\u00e2\u00e3\b\21\b\2\u00e3(\3\2\2\2\u00e4\u00e5"+
					"\5S\'\2\u00e5\u00e6\3\2\2\2\u00e6\u00e7\b\22\t\2\u00e7*\3\2\2\2\u00e8"+
					"\u00e9\7_\2\2\u00e9\u00ea\3\2\2\2\u00ea\u00eb\b\23\6\2\u00eb,\3\2\2\2"+
					"\u00ec\u00ed\7>\2\2\u00ed\u00ee\7#\2\2\u00ee\u00ef\3\2\2\2\u00ef\u00f0"+
					"\b\24\n\2\u00f0.\3\2\2\2\u00f1\u00f2\5M$\2\u00f2\u00f3\3\2\2\2\u00f3\u00f4"+
					"\b\25\2\2\u00f4\60\3\2\2\2\u00f5\u00f6\5\r\4\2\u00f6\u00f7\3\2\2\2\u00f7"+
					"\u00f8\b\26\13\2\u00f8\62\3\2\2\2\u00f9\u00fa\5\t\2\2\u00fa\u00fb\3\2"+
					"\2\2\u00fb\u00fc\b\27\f\2\u00fc\64\3\2\2\2\u00fd\u00fe\7@\2\2\u00fe\u00ff"+
					"\3\2\2\2\u00ff\u0100\b\30\6\2\u0100\66\3\2\2\2\u0101\u0102\7]\2\2\u0102"+
					"\u0103\3\2\2\2\u0103\u0104\b\31\r\2\u0104\u0105\b\31\16\2\u01058\3\2\2"+
					"\2\u0106\u0107\5M$\2\u0107\u0108\3\2\2\2\u0108\u0109\b\32\2\2\u0109:\3"+
					"\2\2\2\u010a\u010c\n\4\2\2\u010b\u010a\3\2\2\2\u010c\u010d\3\2\2\2\u010d"+
					"\u010b\3\2\2\2\u010d\u010e\3\2\2\2\u010e<\3\2\2\2\u010f\u0110\7_\2\2\u0110"+
					"\u0111\3\2\2\2\u0111\u0112\b\34\6\2\u0112>\3\2\2\2\u0113\u0114\13\2\2"+
					"\2\u0114\u0115\3\2\2\2\u0115\u0116\b\35\r\2\u0116@\3\2\2\2\u0117\u0118"+
					"\5I\"\2\u0118\u0119\3\2\2\2\u0119\u011a\b\36\17\2\u011a\u011b\b\36\6\2"+
					"\u011bB\3\2\2\2\u011c\u011d\5M$\2\u011d\u011e\3\2\2\2\u011e\u011f\b\37"+
					"\2\2\u011fD\3\2\2\2\u0120\u0122\n\5\2\2\u0121\u0120\3\2\2\2\u0122\u0123"+
					"\3\2\2\2\u0123\u0121\3\2\2\2\u0123\u0124\3\2\2\2\u0124F\3\2\2\2\u0125"+
					"\u0126\7@\2\2\u0126\u0127\3\2\2\2\u0127\u0128\b!\6\2\u0128H\3\2\2\2\u0129"+
					"\u012a\7A\2\2\u012a\u012b\7@\2\2\u012b\u012c\3\2\2\2\u012c\u012d\b\"\6"+
					"\2\u012dJ\3\2\2\2\u012e\u012f\7\61\2\2\u012f\u0130\7@\2\2\u0130\u0131"+
					"\3\2\2\2\u0131\u0132\b#\6\2\u0132L\3\2\2\2\u0133\u0134\t\6\2\2\u0134\u0135"+
					"\3\2\2\2\u0135\u0136\b$\2\2\u0136N\3\2\2\2\u0137\u0138\7\61\2\2\u0138"+
					"P\3\2\2\2\u0139\u013a\7?\2\2\u013aR\3\2\2\2\u013b\u013f\7$\2\2\u013c\u013e"+
					"\n\7\2\2\u013d\u013c\3\2\2\2\u013e\u0141\3\2\2\2\u013f\u013d\3\2\2\2\u013f"+
					"\u0140\3\2\2\2\u0140\u0142\3\2\2\2\u0141\u013f\3\2\2\2\u0142\u014c\7$"+
					"\2\2\u0143\u0147\7)\2\2\u0144\u0146\n\b\2\2\u0145\u0144\3\2\2\2\u0146"+
					"\u0149\3\2\2\2\u0147\u0145\3\2\2\2\u0147\u0148\3\2\2\2\u0148\u014a\3\2"+
					"\2\2\u0149\u0147\3\2\2\2\u014a\u014c\7)\2\2\u014b\u013b\3\2\2\2\u014b"+
					"\u0143\3\2\2\2\u014cT\3\2\2\2\u014d\u0151\5],\2\u014e\u0150\5[+\2\u014f"+
					"\u014e\3\2\2\2\u0150\u0153\3\2\2\2\u0151\u014f\3\2\2\2\u0151\u0152\3\2"+
					"\2\2\u0152V\3\2\2\2\u0153\u0151\3\2\2\2\u0154\u0155\t\t\2\2\u0155X\3\2"+
					"\2\2\u0156\u0157\t\n\2\2\u0157Z\3\2\2\2\u0158\u015d\5],\2\u0159\u015d"+
					"\t\13\2\2\u015a\u015d\5Y*\2\u015b\u015d\t\f\2\2\u015c\u0158\3\2\2\2\u015c"+
					"\u0159\3\2\2\2\u015c\u015a\3\2\2\2\u015c\u015b\3\2\2\2\u015d\\\3\2\2\2"+
					"\u015e\u0160\t\r\2\2\u015f\u015e\3\2\2\2\u0160^\3\2\2\2\32\2\3\4\5\6\7"+
					"\bg{\u0090\u009b\u009f\u00a3\u00a7\u00a9\u00ca\u010d\u0123\u013f\u0147"+
					"\u014b\u0151\u015c\u015f\20\b\2\2\7\b\2\7\7\2\7\3\2\6\2\2\7\4\2\t\"\2"+
					"\t!\2\7\5\2\t\5\2\t\3\2\5\2\2\7\6\2\t\34\2";
	public static final ATN _ATN =
			new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
