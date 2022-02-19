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
// Generated from /Users/jon/Projects/github/openrewrite/rewrite/rewrite-protobuf/src/main/antlr/Protobuf2Lexer.g4 by ANTLR 4.9.3
package org.openrewrite.protobuf.internal.grammar;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class Protobuf2Lexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.9.3", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		SEMI=1, COLON=2, BOOL=3, BYTES=4, DOUBLE=5, ENUM=6, FIXED32=7, FIXED64=8, 
		FLOAT=9, IMPORT=10, INT32=11, INT64=12, MAP=13, MESSAGE=14, ONEOF=15, 
		OPTION=16, PACKAGE=17, PUBLIC=18, REPEATED=19, REQUIRED=20, RESERVED=21, 
		RETURNS=22, RPC=23, SERVICE=24, SFIXED32=25, SFIXED64=26, SINT32=27, SINT64=28, 
		STREAM=29, STRING=30, SYNTAX=31, TO=32, UINT32=33, UINT64=34, WEAK=35, 
		OPTIONAL=36, Ident=37, IntegerLiteral=38, NumericLiteral=39, FloatLiteral=40, 
		BooleanLiteral=41, StringLiteral=42, Quote=43, LPAREN=44, RPAREN=45, LBRACE=46, 
		RBRACE=47, LBRACK=48, RBRACK=49, LCHEVR=50, RCHEVR=51, COMMA=52, DOT=53, 
		MINUS=54, PLUS=55, ASSIGN=56, WS=57, UTF_8_BOM=58, COMMENT=59, LINE_COMMENT=60;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"SEMI", "COLON", "BOOL", "BYTES", "DOUBLE", "ENUM", "FIXED32", "FIXED64", 
			"FLOAT", "IMPORT", "INT32", "INT64", "MAP", "MESSAGE", "ONEOF", "OPTION", 
			"PACKAGE", "PUBLIC", "REPEATED", "REQUIRED", "RESERVED", "RETURNS", "RPC", 
			"SERVICE", "SFIXED32", "SFIXED64", "SINT32", "SINT64", "STREAM", "STRING", 
			"SYNTAX", "TO", "UINT32", "UINT64", "WEAK", "OPTIONAL", "Letter", "DecimalDigit", 
			"OctalDigit", "HexDigit", "Ident", "IntegerLiteral", "NumericLiteral", 
			"DecimalLiteral", "OctalLiteral", "HexLiteral", "FloatLiteral", "Decimals", 
			"Exponent", "BooleanLiteral", "StringLiteral", "CharValue", "HexEscape", 
			"OctEscape", "CharEscape", "Quote", "LPAREN", "RPAREN", "LBRACE", "RBRACE", 
			"LBRACK", "RBRACK", "LCHEVR", "RCHEVR", "COMMA", "DOT", "MINUS", "PLUS", 
			"ASSIGN", "WS", "UTF_8_BOM", "COMMENT", "LINE_COMMENT"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "';'", "':'", "'bool'", "'bytes'", "'double'", "'enum'", "'fixed32'", 
			"'fixed64'", "'float'", "'import'", "'int32'", "'int64'", "'map'", "'message'", 
			"'oneof'", "'option'", "'package'", "'public'", "'repeated'", "'required'", 
			"'reserved'", "'returns'", "'rpc'", "'service'", "'sfixed32'", "'sfixed64'", 
			"'sint32'", "'sint64'", "'stream'", "'string'", "'syntax'", "'to'", "'uint32'", 
			"'uint64'", "'weak'", "'optional'", null, null, null, null, null, null, 
			null, "'('", "')'", "'{'", "'}'", "'['", "']'", "'<'", "'>'", "','", 
			"'.'", "'-'", "'+'", "'='", null, "'\uFEFF'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "SEMI", "COLON", "BOOL", "BYTES", "DOUBLE", "ENUM", "FIXED32", 
			"FIXED64", "FLOAT", "IMPORT", "INT32", "INT64", "MAP", "MESSAGE", "ONEOF", 
			"OPTION", "PACKAGE", "PUBLIC", "REPEATED", "REQUIRED", "RESERVED", "RETURNS", 
			"RPC", "SERVICE", "SFIXED32", "SFIXED64", "SINT32", "SINT64", "STREAM", 
			"STRING", "SYNTAX", "TO", "UINT32", "UINT64", "WEAK", "OPTIONAL", "Ident", 
			"IntegerLiteral", "NumericLiteral", "FloatLiteral", "BooleanLiteral", 
			"StringLiteral", "Quote", "LPAREN", "RPAREN", "LBRACE", "RBRACE", "LBRACK", 
			"RBRACK", "LCHEVR", "RCHEVR", "COMMA", "DOT", "MINUS", "PLUS", "ASSIGN", 
			"WS", "UTF_8_BOM", "COMMENT", "LINE_COMMENT"
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


	public Protobuf2Lexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "Protobuf2Lexer.g4"; }

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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2>\u024b\b\1\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t+\4"+
		",\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64\t"+
		"\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\4<\t<\4=\t="+
		"\4>\t>\4?\t?\4@\t@\4A\tA\4B\tB\4C\tC\4D\tD\4E\tE\4F\tF\4G\tG\4H\tH\4I"+
		"\tI\4J\tJ\3\2\3\2\3\3\3\3\3\4\3\4\3\4\3\4\3\4\3\5\3\5\3\5\3\5\3\5\3\5"+
		"\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\7\3\7\3\7\3\7\3\7\3\b\3\b\3\b\3\b\3\b\3"+
		"\b\3\b\3\b\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\n\3\n\3\n\3\n\3\n\3\n\3\13"+
		"\3\13\3\13\3\13\3\13\3\13\3\13\3\f\3\f\3\f\3\f\3\f\3\f\3\r\3\r\3\r\3\r"+
		"\3\r\3\r\3\16\3\16\3\16\3\16\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3"+
		"\20\3\20\3\20\3\20\3\20\3\20\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\22\3"+
		"\22\3\22\3\22\3\22\3\22\3\22\3\22\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3"+
		"\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\25\3\25\3\25\3\25\3\25\3"+
		"\25\3\25\3\25\3\25\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\27\3"+
		"\27\3\27\3\27\3\27\3\27\3\27\3\27\3\30\3\30\3\30\3\30\3\31\3\31\3\31\3"+
		"\31\3\31\3\31\3\31\3\31\3\32\3\32\3\32\3\32\3\32\3\32\3\32\3\32\3\32\3"+
		"\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\34\3\34\3\34\3\34\3\34\3"+
		"\34\3\34\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\36\3\36\3\36\3\36\3\36\3"+
		"\36\3\36\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3 \3 \3 \3 \3 \3 \3 \3!\3"+
		"!\3!\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3#\3#\3#\3#\3#\3#\3#\3$\3$\3$\3$\3$\3"+
		"%\3%\3%\3%\3%\3%\3%\3%\3%\3&\3&\3\'\3\'\3(\3(\3)\3)\3*\3*\3*\7*\u0190"+
		"\n*\f*\16*\u0193\13*\3+\3+\3+\5+\u0198\n+\3,\3,\5,\u019c\n,\3,\3,\5,\u01a0"+
		"\n,\3-\3-\7-\u01a4\n-\f-\16-\u01a7\13-\3.\3.\7.\u01ab\n.\f.\16.\u01ae"+
		"\13.\3/\3/\3/\6/\u01b3\n/\r/\16/\u01b4\3\60\3\60\3\60\5\60\u01ba\n\60"+
		"\3\60\5\60\u01bd\n\60\3\60\3\60\3\60\3\60\3\60\3\60\5\60\u01c5\n\60\5"+
		"\60\u01c7\n\60\3\60\3\60\3\60\3\60\3\60\3\60\5\60\u01cf\n\60\3\61\6\61"+
		"\u01d2\n\61\r\61\16\61\u01d3\3\62\3\62\5\62\u01d8\n\62\3\62\3\62\3\63"+
		"\3\63\3\63\3\63\3\63\3\63\3\63\3\63\3\63\5\63\u01e5\n\63\3\64\3\64\7\64"+
		"\u01e9\n\64\f\64\16\64\u01ec\13\64\3\64\3\64\3\64\7\64\u01f1\n\64\f\64"+
		"\16\64\u01f4\13\64\3\64\5\64\u01f7\n\64\3\65\3\65\3\65\3\65\5\65\u01fd"+
		"\n\65\3\66\3\66\3\66\3\66\3\66\3\67\3\67\3\67\3\67\3\67\38\38\38\39\3"+
		"9\3:\3:\3;\3;\3<\3<\3=\3=\3>\3>\3?\3?\3@\3@\3A\3A\3B\3B\3C\3C\3D\3D\3"+
		"E\3E\3F\3F\3G\6G\u0229\nG\rG\16G\u022a\3G\3G\3H\3H\3H\3H\3I\3I\3I\3I\7"+
		"I\u0237\nI\fI\16I\u023a\13I\3I\3I\3I\3I\3I\3J\3J\3J\3J\7J\u0245\nJ\fJ"+
		"\16J\u0248\13J\3J\3J\3\u0238\2K\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23"+
		"\13\25\f\27\r\31\16\33\17\35\20\37\21!\22#\23%\24\'\25)\26+\27-\30/\31"+
		"\61\32\63\33\65\34\67\359\36;\37= ?!A\"C#E$G%I&K\2M\2O\2Q\2S\'U(W)Y\2"+
		"[\2]\2_*a\2c\2e+g,i\2k\2m\2o\2q-s.u/w\60y\61{\62}\63\177\64\u0081\65\u0083"+
		"\66\u0085\67\u00878\u00899\u008b:\u008d;\u008f<\u0091=\u0093>\3\2\17\5"+
		"\2C\\aac|\3\2\62;\3\2\629\5\2\62;CHch\3\2\63;\4\2ZZzz\4\2GGgg\4\2--//"+
		"\5\2\2\2\f\f^^\13\2$$))^^cdhhppttvvxx\4\2$$))\b\2\13\f\17\17\"\"\u00a2"+
		"\u00a2\u2005\u2005\uff01\uff01\4\2\f\f\17\17\2\u025a\2\3\3\2\2\2\2\5\3"+
		"\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2"+
		"\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3"+
		"\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'"+
		"\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\2\63"+
		"\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2;\3\2\2\2\2=\3\2\2\2\2"+
		"?\3\2\2\2\2A\3\2\2\2\2C\3\2\2\2\2E\3\2\2\2\2G\3\2\2\2\2I\3\2\2\2\2S\3"+
		"\2\2\2\2U\3\2\2\2\2W\3\2\2\2\2_\3\2\2\2\2e\3\2\2\2\2g\3\2\2\2\2q\3\2\2"+
		"\2\2s\3\2\2\2\2u\3\2\2\2\2w\3\2\2\2\2y\3\2\2\2\2{\3\2\2\2\2}\3\2\2\2\2"+
		"\177\3\2\2\2\2\u0081\3\2\2\2\2\u0083\3\2\2\2\2\u0085\3\2\2\2\2\u0087\3"+
		"\2\2\2\2\u0089\3\2\2\2\2\u008b\3\2\2\2\2\u008d\3\2\2\2\2\u008f\3\2\2\2"+
		"\2\u0091\3\2\2\2\2\u0093\3\2\2\2\3\u0095\3\2\2\2\5\u0097\3\2\2\2\7\u0099"+
		"\3\2\2\2\t\u009e\3\2\2\2\13\u00a4\3\2\2\2\r\u00ab\3\2\2\2\17\u00b0\3\2"+
		"\2\2\21\u00b8\3\2\2\2\23\u00c0\3\2\2\2\25\u00c6\3\2\2\2\27\u00cd\3\2\2"+
		"\2\31\u00d3\3\2\2\2\33\u00d9\3\2\2\2\35\u00dd\3\2\2\2\37\u00e5\3\2\2\2"+
		"!\u00eb\3\2\2\2#\u00f2\3\2\2\2%\u00fa\3\2\2\2\'\u0101\3\2\2\2)\u010a\3"+
		"\2\2\2+\u0113\3\2\2\2-\u011c\3\2\2\2/\u0124\3\2\2\2\61\u0128\3\2\2\2\63"+
		"\u0130\3\2\2\2\65\u0139\3\2\2\2\67\u0142\3\2\2\29\u0149\3\2\2\2;\u0150"+
		"\3\2\2\2=\u0157\3\2\2\2?\u015e\3\2\2\2A\u0165\3\2\2\2C\u0168\3\2\2\2E"+
		"\u016f\3\2\2\2G\u0176\3\2\2\2I\u017b\3\2\2\2K\u0184\3\2\2\2M\u0186\3\2"+
		"\2\2O\u0188\3\2\2\2Q\u018a\3\2\2\2S\u018c\3\2\2\2U\u0197\3\2\2\2W\u019b"+
		"\3\2\2\2Y\u01a1\3\2\2\2[\u01a8\3\2\2\2]\u01af\3\2\2\2_\u01ce\3\2\2\2a"+
		"\u01d1\3\2\2\2c\u01d5\3\2\2\2e\u01e4\3\2\2\2g\u01f6\3\2\2\2i\u01fc\3\2"+
		"\2\2k\u01fe\3\2\2\2m\u0203\3\2\2\2o\u0208\3\2\2\2q\u020b\3\2\2\2s\u020d"+
		"\3\2\2\2u\u020f\3\2\2\2w\u0211\3\2\2\2y\u0213\3\2\2\2{\u0215\3\2\2\2}"+
		"\u0217\3\2\2\2\177\u0219\3\2\2\2\u0081\u021b\3\2\2\2\u0083\u021d\3\2\2"+
		"\2\u0085\u021f\3\2\2\2\u0087\u0221\3\2\2\2\u0089\u0223\3\2\2\2\u008b\u0225"+
		"\3\2\2\2\u008d\u0228\3\2\2\2\u008f\u022e\3\2\2\2\u0091\u0232\3\2\2\2\u0093"+
		"\u0240\3\2\2\2\u0095\u0096\7=\2\2\u0096\4\3\2\2\2\u0097\u0098\7<\2\2\u0098"+
		"\6\3\2\2\2\u0099\u009a\7d\2\2\u009a\u009b\7q\2\2\u009b\u009c\7q\2\2\u009c"+
		"\u009d\7n\2\2\u009d\b\3\2\2\2\u009e\u009f\7d\2\2\u009f\u00a0\7{\2\2\u00a0"+
		"\u00a1\7v\2\2\u00a1\u00a2\7g\2\2\u00a2\u00a3\7u\2\2\u00a3\n\3\2\2\2\u00a4"+
		"\u00a5\7f\2\2\u00a5\u00a6\7q\2\2\u00a6\u00a7\7w\2\2\u00a7\u00a8\7d\2\2"+
		"\u00a8\u00a9\7n\2\2\u00a9\u00aa\7g\2\2\u00aa\f\3\2\2\2\u00ab\u00ac\7g"+
		"\2\2\u00ac\u00ad\7p\2\2\u00ad\u00ae\7w\2\2\u00ae\u00af\7o\2\2\u00af\16"+
		"\3\2\2\2\u00b0\u00b1\7h\2\2\u00b1\u00b2\7k\2\2\u00b2\u00b3\7z\2\2\u00b3"+
		"\u00b4\7g\2\2\u00b4\u00b5\7f\2\2\u00b5\u00b6\7\65\2\2\u00b6\u00b7\7\64"+
		"\2\2\u00b7\20\3\2\2\2\u00b8\u00b9\7h\2\2\u00b9\u00ba\7k\2\2\u00ba\u00bb"+
		"\7z\2\2\u00bb\u00bc\7g\2\2\u00bc\u00bd\7f\2\2\u00bd\u00be\78\2\2\u00be"+
		"\u00bf\7\66\2\2\u00bf\22\3\2\2\2\u00c0\u00c1\7h\2\2\u00c1\u00c2\7n\2\2"+
		"\u00c2\u00c3\7q\2\2\u00c3\u00c4\7c\2\2\u00c4\u00c5\7v\2\2\u00c5\24\3\2"+
		"\2\2\u00c6\u00c7\7k\2\2\u00c7\u00c8\7o\2\2\u00c8\u00c9\7r\2\2\u00c9\u00ca"+
		"\7q\2\2\u00ca\u00cb\7t\2\2\u00cb\u00cc\7v\2\2\u00cc\26\3\2\2\2\u00cd\u00ce"+
		"\7k\2\2\u00ce\u00cf\7p\2\2\u00cf\u00d0\7v\2\2\u00d0\u00d1\7\65\2\2\u00d1"+
		"\u00d2\7\64\2\2\u00d2\30\3\2\2\2\u00d3\u00d4\7k\2\2\u00d4\u00d5\7p\2\2"+
		"\u00d5\u00d6\7v\2\2\u00d6\u00d7\78\2\2\u00d7\u00d8\7\66\2\2\u00d8\32\3"+
		"\2\2\2\u00d9\u00da\7o\2\2\u00da\u00db\7c\2\2\u00db\u00dc\7r\2\2\u00dc"+
		"\34\3\2\2\2\u00dd\u00de\7o\2\2\u00de\u00df\7g\2\2\u00df\u00e0\7u\2\2\u00e0"+
		"\u00e1\7u\2\2\u00e1\u00e2\7c\2\2\u00e2\u00e3\7i\2\2\u00e3\u00e4\7g\2\2"+
		"\u00e4\36\3\2\2\2\u00e5\u00e6\7q\2\2\u00e6\u00e7\7p\2\2\u00e7\u00e8\7"+
		"g\2\2\u00e8\u00e9\7q\2\2\u00e9\u00ea\7h\2\2\u00ea \3\2\2\2\u00eb\u00ec"+
		"\7q\2\2\u00ec\u00ed\7r\2\2\u00ed\u00ee\7v\2\2\u00ee\u00ef\7k\2\2\u00ef"+
		"\u00f0\7q\2\2\u00f0\u00f1\7p\2\2\u00f1\"\3\2\2\2\u00f2\u00f3\7r\2\2\u00f3"+
		"\u00f4\7c\2\2\u00f4\u00f5\7e\2\2\u00f5\u00f6\7m\2\2\u00f6\u00f7\7c\2\2"+
		"\u00f7\u00f8\7i\2\2\u00f8\u00f9\7g\2\2\u00f9$\3\2\2\2\u00fa\u00fb\7r\2"+
		"\2\u00fb\u00fc\7w\2\2\u00fc\u00fd\7d\2\2\u00fd\u00fe\7n\2\2\u00fe\u00ff"+
		"\7k\2\2\u00ff\u0100\7e\2\2\u0100&\3\2\2\2\u0101\u0102\7t\2\2\u0102\u0103"+
		"\7g\2\2\u0103\u0104\7r\2\2\u0104\u0105\7g\2\2\u0105\u0106\7c\2\2\u0106"+
		"\u0107\7v\2\2\u0107\u0108\7g\2\2\u0108\u0109\7f\2\2\u0109(\3\2\2\2\u010a"+
		"\u010b\7t\2\2\u010b\u010c\7g\2\2\u010c\u010d\7s\2\2\u010d\u010e\7w\2\2"+
		"\u010e\u010f\7k\2\2\u010f\u0110\7t\2\2\u0110\u0111\7g\2\2\u0111\u0112"+
		"\7f\2\2\u0112*\3\2\2\2\u0113\u0114\7t\2\2\u0114\u0115\7g\2\2\u0115\u0116"+
		"\7u\2\2\u0116\u0117\7g\2\2\u0117\u0118\7t\2\2\u0118\u0119\7x\2\2\u0119"+
		"\u011a\7g\2\2\u011a\u011b\7f\2\2\u011b,\3\2\2\2\u011c\u011d\7t\2\2\u011d"+
		"\u011e\7g\2\2\u011e\u011f\7v\2\2\u011f\u0120\7w\2\2\u0120\u0121\7t\2\2"+
		"\u0121\u0122\7p\2\2\u0122\u0123\7u\2\2\u0123.\3\2\2\2\u0124\u0125\7t\2"+
		"\2\u0125\u0126\7r\2\2\u0126\u0127\7e\2\2\u0127\60\3\2\2\2\u0128\u0129"+
		"\7u\2\2\u0129\u012a\7g\2\2\u012a\u012b\7t\2\2\u012b\u012c\7x\2\2\u012c"+
		"\u012d\7k\2\2\u012d\u012e\7e\2\2\u012e\u012f\7g\2\2\u012f\62\3\2\2\2\u0130"+
		"\u0131\7u\2\2\u0131\u0132\7h\2\2\u0132\u0133\7k\2\2\u0133\u0134\7z\2\2"+
		"\u0134\u0135\7g\2\2\u0135\u0136\7f\2\2\u0136\u0137\7\65\2\2\u0137\u0138"+
		"\7\64\2\2\u0138\64\3\2\2\2\u0139\u013a\7u\2\2\u013a\u013b\7h\2\2\u013b"+
		"\u013c\7k\2\2\u013c\u013d\7z\2\2\u013d\u013e\7g\2\2\u013e\u013f\7f\2\2"+
		"\u013f\u0140\78\2\2\u0140\u0141\7\66\2\2\u0141\66\3\2\2\2\u0142\u0143"+
		"\7u\2\2\u0143\u0144\7k\2\2\u0144\u0145\7p\2\2\u0145\u0146\7v\2\2\u0146"+
		"\u0147\7\65\2\2\u0147\u0148\7\64\2\2\u01488\3\2\2\2\u0149\u014a\7u\2\2"+
		"\u014a\u014b\7k\2\2\u014b\u014c\7p\2\2\u014c\u014d\7v\2\2\u014d\u014e"+
		"\78\2\2\u014e\u014f\7\66\2\2\u014f:\3\2\2\2\u0150\u0151\7u\2\2\u0151\u0152"+
		"\7v\2\2\u0152\u0153\7t\2\2\u0153\u0154\7g\2\2\u0154\u0155\7c\2\2\u0155"+
		"\u0156\7o\2\2\u0156<\3\2\2\2\u0157\u0158\7u\2\2\u0158\u0159\7v\2\2\u0159"+
		"\u015a\7t\2\2\u015a\u015b\7k\2\2\u015b\u015c\7p\2\2\u015c\u015d\7i\2\2"+
		"\u015d>\3\2\2\2\u015e\u015f\7u\2\2\u015f\u0160\7{\2\2\u0160\u0161\7p\2"+
		"\2\u0161\u0162\7v\2\2\u0162\u0163\7c\2\2\u0163\u0164\7z\2\2\u0164@\3\2"+
		"\2\2\u0165\u0166\7v\2\2\u0166\u0167\7q\2\2\u0167B\3\2\2\2\u0168\u0169"+
		"\7w\2\2\u0169\u016a\7k\2\2\u016a\u016b\7p\2\2\u016b\u016c\7v\2\2\u016c"+
		"\u016d\7\65\2\2\u016d\u016e\7\64\2\2\u016eD\3\2\2\2\u016f\u0170\7w\2\2"+
		"\u0170\u0171\7k\2\2\u0171\u0172\7p\2\2\u0172\u0173\7v\2\2\u0173\u0174"+
		"\78\2\2\u0174\u0175\7\66\2\2\u0175F\3\2\2\2\u0176\u0177\7y\2\2\u0177\u0178"+
		"\7g\2\2\u0178\u0179\7c\2\2\u0179\u017a\7m\2\2\u017aH\3\2\2\2\u017b\u017c"+
		"\7q\2\2\u017c\u017d\7r\2\2\u017d\u017e\7v\2\2\u017e\u017f\7k\2\2\u017f"+
		"\u0180\7q\2\2\u0180\u0181\7p\2\2\u0181\u0182\7c\2\2\u0182\u0183\7n\2\2"+
		"\u0183J\3\2\2\2\u0184\u0185\t\2\2\2\u0185L\3\2\2\2\u0186\u0187\t\3\2\2"+
		"\u0187N\3\2\2\2\u0188\u0189\t\4\2\2\u0189P\3\2\2\2\u018a\u018b\t\5\2\2"+
		"\u018bR\3\2\2\2\u018c\u0191\5K&\2\u018d\u0190\5K&\2\u018e\u0190\5M\'\2"+
		"\u018f\u018d\3\2\2\2\u018f\u018e\3\2\2\2\u0190\u0193\3\2\2\2\u0191\u018f"+
		"\3\2\2\2\u0191\u0192\3\2\2\2\u0192T\3\2\2\2\u0193\u0191\3\2\2\2\u0194"+
		"\u0198\5Y-\2\u0195\u0198\5[.\2\u0196\u0198\5]/\2\u0197\u0194\3\2\2\2\u0197"+
		"\u0195\3\2\2\2\u0197\u0196\3\2\2\2\u0198V\3\2\2\2\u0199\u019c\5\u0087"+
		"D\2\u019a\u019c\5\u0089E\2\u019b\u0199\3\2\2\2\u019b\u019a\3\2\2\2\u019b"+
		"\u019c\3\2\2\2\u019c\u019f\3\2\2\2\u019d\u01a0\5U+\2\u019e\u01a0\5_\60"+
		"\2\u019f\u019d\3\2\2\2\u019f\u019e\3\2\2\2\u01a0X\3\2\2\2\u01a1\u01a5"+
		"\t\6\2\2\u01a2\u01a4\5M\'\2\u01a3\u01a2\3\2\2\2\u01a4\u01a7\3\2\2\2\u01a5"+
		"\u01a3\3\2\2\2\u01a5\u01a6\3\2\2\2\u01a6Z\3\2\2\2\u01a7\u01a5\3\2\2\2"+
		"\u01a8\u01ac\7\62\2\2\u01a9\u01ab\5O(\2\u01aa\u01a9\3\2\2\2\u01ab\u01ae"+
		"\3\2\2\2\u01ac\u01aa\3\2\2\2\u01ac\u01ad\3\2\2\2\u01ad\\\3\2\2\2\u01ae"+
		"\u01ac\3\2\2\2\u01af\u01b0\7\62\2\2\u01b0\u01b2\t\7\2\2\u01b1\u01b3\5"+
		"Q)\2\u01b2\u01b1\3\2\2\2\u01b3\u01b4\3\2\2\2\u01b4\u01b2\3\2\2\2\u01b4"+
		"\u01b5\3\2\2\2\u01b5^\3\2\2\2\u01b6\u01b7\5a\61\2\u01b7\u01b9\7\60\2\2"+
		"\u01b8\u01ba\5a\61\2\u01b9\u01b8\3\2\2\2\u01b9\u01ba\3\2\2\2\u01ba\u01bc"+
		"\3\2\2\2\u01bb\u01bd\5c\62\2\u01bc\u01bb\3\2\2\2\u01bc\u01bd\3\2\2\2\u01bd"+
		"\u01c7\3\2\2\2\u01be\u01bf\5a\61\2\u01bf\u01c0\5c\62\2\u01c0\u01c7\3\2"+
		"\2\2\u01c1\u01c2\7\60\2\2\u01c2\u01c4\5a\61\2\u01c3\u01c5\5c\62\2\u01c4"+
		"\u01c3\3\2\2\2\u01c4\u01c5\3\2\2\2\u01c5\u01c7\3\2\2\2\u01c6\u01b6\3\2"+
		"\2\2\u01c6\u01be\3\2\2\2\u01c6\u01c1\3\2\2\2\u01c7\u01cf\3\2\2\2\u01c8"+
		"\u01c9\7k\2\2\u01c9\u01ca\7p\2\2\u01ca\u01cf\7h\2\2\u01cb\u01cc\7p\2\2"+
		"\u01cc\u01cd\7c\2\2\u01cd\u01cf\7p\2\2\u01ce\u01c6\3\2\2\2\u01ce\u01c8"+
		"\3\2\2\2\u01ce\u01cb\3\2\2\2\u01cf`\3\2\2\2\u01d0\u01d2\5M\'\2\u01d1\u01d0"+
		"\3\2\2\2\u01d2\u01d3\3\2\2\2\u01d3\u01d1\3\2\2\2\u01d3\u01d4\3\2\2\2\u01d4"+
		"b\3\2\2\2\u01d5\u01d7\t\b\2\2\u01d6\u01d8\t\t\2\2\u01d7\u01d6\3\2\2\2"+
		"\u01d7\u01d8\3\2\2\2\u01d8\u01d9\3\2\2\2\u01d9\u01da\5a\61\2\u01dad\3"+
		"\2\2\2\u01db\u01dc\7v\2\2\u01dc\u01dd\7t\2\2\u01dd\u01de\7w\2\2\u01de"+
		"\u01e5\7g\2\2\u01df\u01e0\7h\2\2\u01e0\u01e1\7c\2\2\u01e1\u01e2\7n\2\2"+
		"\u01e2\u01e3\7u\2\2\u01e3\u01e5\7g\2\2\u01e4\u01db\3\2\2\2\u01e4\u01df"+
		"\3\2\2\2\u01e5f\3\2\2\2\u01e6\u01ea\7)\2\2\u01e7\u01e9\5i\65\2\u01e8\u01e7"+
		"\3\2\2\2\u01e9\u01ec\3\2\2\2\u01ea\u01e8\3\2\2\2\u01ea\u01eb\3\2\2\2\u01eb"+
		"\u01ed\3\2\2\2\u01ec\u01ea\3\2\2\2\u01ed\u01f7\7)\2\2\u01ee\u01f2\7$\2"+
		"\2\u01ef\u01f1\5i\65\2\u01f0\u01ef\3\2\2\2\u01f1\u01f4\3\2\2\2\u01f2\u01f0"+
		"\3\2\2\2\u01f2\u01f3\3\2\2\2\u01f3\u01f5\3\2\2\2\u01f4\u01f2\3\2\2\2\u01f5"+
		"\u01f7\7$\2\2\u01f6\u01e6\3\2\2\2\u01f6\u01ee\3\2\2\2\u01f7h\3\2\2\2\u01f8"+
		"\u01fd\5k\66\2\u01f9\u01fd\5m\67\2\u01fa\u01fd\5o8\2\u01fb\u01fd\n\n\2"+
		"\2\u01fc\u01f8\3\2\2\2\u01fc\u01f9\3\2\2\2\u01fc\u01fa\3\2\2\2\u01fc\u01fb"+
		"\3\2\2\2\u01fdj\3\2\2\2\u01fe\u01ff\7^\2\2\u01ff\u0200\t\7\2\2\u0200\u0201"+
		"\5Q)\2\u0201\u0202\5Q)\2\u0202l\3\2\2\2\u0203\u0204\7^\2\2\u0204\u0205"+
		"\5O(\2\u0205\u0206\5O(\2\u0206\u0207\5O(\2\u0207n\3\2\2\2\u0208\u0209"+
		"\7^\2\2\u0209\u020a\t\13\2\2\u020ap\3\2\2\2\u020b\u020c\t\f\2\2\u020c"+
		"r\3\2\2\2\u020d\u020e\7*\2\2\u020et\3\2\2\2\u020f\u0210\7+\2\2\u0210v"+
		"\3\2\2\2\u0211\u0212\7}\2\2\u0212x\3\2\2\2\u0213\u0214\7\177\2\2\u0214"+
		"z\3\2\2\2\u0215\u0216\7]\2\2\u0216|\3\2\2\2\u0217\u0218\7_\2\2\u0218~"+
		"\3\2\2\2\u0219\u021a\7>\2\2\u021a\u0080\3\2\2\2\u021b\u021c\7@\2\2\u021c"+
		"\u0082\3\2\2\2\u021d\u021e\7.\2\2\u021e\u0084\3\2\2\2\u021f\u0220\7\60"+
		"\2\2\u0220\u0086\3\2\2\2\u0221\u0222\7/\2\2\u0222\u0088\3\2\2\2\u0223"+
		"\u0224\7-\2\2\u0224\u008a\3\2\2\2\u0225\u0226\7?\2\2\u0226\u008c\3\2\2"+
		"\2\u0227\u0229\t\r\2\2\u0228\u0227\3\2\2\2\u0229\u022a\3\2\2\2\u022a\u0228"+
		"\3\2\2\2\u022a\u022b\3\2\2\2\u022b\u022c\3\2\2\2\u022c\u022d\bG\2\2\u022d"+
		"\u008e\3\2\2\2\u022e\u022f\7\uff01\2\2\u022f\u0230\3\2\2\2\u0230\u0231"+
		"\bH\2\2\u0231\u0090\3\2\2\2\u0232\u0233\7\61\2\2\u0233\u0234\7,\2\2\u0234"+
		"\u0238\3\2\2\2\u0235\u0237\13\2\2\2\u0236\u0235\3\2\2\2\u0237\u023a\3"+
		"\2\2\2\u0238\u0239\3\2\2\2\u0238\u0236\3\2\2\2\u0239\u023b\3\2\2\2\u023a"+
		"\u0238\3\2\2\2\u023b\u023c\7,\2\2\u023c\u023d\7\61\2\2\u023d\u023e\3\2"+
		"\2\2\u023e\u023f\bI\2\2\u023f\u0092\3\2\2\2\u0240\u0241\7\61\2\2\u0241"+
		"\u0242\7\61\2\2\u0242\u0246\3\2\2\2\u0243\u0245\n\16\2\2\u0244\u0243\3"+
		"\2\2\2\u0245\u0248\3\2\2\2\u0246\u0244\3\2\2\2\u0246\u0247\3\2\2\2\u0247"+
		"\u0249\3\2\2\2\u0248\u0246\3\2\2\2\u0249\u024a\bJ\2\2\u024a\u0094\3\2"+
		"\2\2\32\2\u018f\u0191\u0197\u019b\u019f\u01a5\u01ac\u01b4\u01b9\u01bc"+
		"\u01c4\u01c6\u01ce\u01d3\u01d7\u01e4\u01ea\u01f2\u01f6\u01fc\u022a\u0238"+
		"\u0246\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}