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
// Generated from /Users/yoshi/Development/Repos/openrewrite/rewrite/rewrite-protobuf/src/main/antlr/Protobuf2Lexer.g4 by ANTLR 4.9.3
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
    static {
        RuntimeMetaData.checkVersion("4.9.3", RuntimeMetaData.VERSION);
    }

    protected static final DFA[] _decisionToDFA;
    protected static final PredictionContextCache _sharedContextCache =
            new PredictionContextCache();
    public static final int
            SEMI = 1, COLON = 2, BOOL = 3, BYTES = 4, DOUBLE = 5, ENUM = 6, EXTEND = 7, FIXED32 = 8,
            FIXED64 = 9, FLOAT = 10, IMPORT = 11, INT32 = 12, INT64 = 13, MAP = 14, MESSAGE = 15,
            ONEOF = 16, OPTION = 17, PACKAGE = 18, PUBLIC = 19, REPEATED = 20, REQUIRED = 21,
            RESERVED = 22, RETURNS = 23, RPC = 24, SERVICE = 25, SFIXED32 = 26, SFIXED64 = 27,
            SINT32 = 28, SINT64 = 29, STREAM = 30, STRING = 31, SYNTAX = 32, TO = 33, UINT32 = 34,
            UINT64 = 35, WEAK = 36, OPTIONAL = 37, Ident = 38, IntegerLiteral = 39, NumericLiteral = 40,
            FloatLiteral = 41, BooleanLiteral = 42, StringLiteral = 43, Quote = 44, LPAREN = 45,
            RPAREN = 46, LBRACE = 47, RBRACE = 48, LBRACK = 49, RBRACK = 50, LCHEVR = 51, RCHEVR = 52,
            COMMA = 53, DOT = 54, MINUS = 55, PLUS = 56, ASSIGN = 57, WS = 58, UTF_8_BOM = 59, COMMENT = 60,
            LINE_COMMENT = 61;
    public static String[] channelNames = {
            "DEFAULT_TOKEN_CHANNEL", "HIDDEN"
    };

    public static String[] modeNames = {
            "DEFAULT_MODE"
    };

    private static String[] makeRuleNames() {
        return new String[]{
                "SEMI", "COLON", "BOOL", "BYTES", "DOUBLE", "ENUM", "EXTEND", "FIXED32",
                "FIXED64", "FLOAT", "IMPORT", "INT32", "INT64", "MAP", "MESSAGE", "ONEOF",
                "OPTION", "PACKAGE", "PUBLIC", "REPEATED", "REQUIRED", "RESERVED", "RETURNS",
                "RPC", "SERVICE", "SFIXED32", "SFIXED64", "SINT32", "SINT64", "STREAM",
                "STRING", "SYNTAX", "TO", "UINT32", "UINT64", "WEAK", "OPTIONAL", "Letter",
                "DecimalDigit", "OctalDigit", "HexDigit", "Ident", "IntegerLiteral",
                "NumericLiteral", "DecimalLiteral", "OctalLiteral", "HexLiteral", "FloatLiteral",
                "Decimals", "Exponent", "BooleanLiteral", "StringLiteral", "CharValue",
                "HexEscape", "OctEscape", "CharEscape", "Quote", "LPAREN", "RPAREN",
                "LBRACE", "RBRACE", "LBRACK", "RBRACK", "LCHEVR", "RCHEVR", "COMMA",
                "DOT", "MINUS", "PLUS", "ASSIGN", "WS", "UTF_8_BOM", "COMMENT", "LINE_COMMENT"
        };
    }
    public static final String[] ruleNames = makeRuleNames();

    private static String[] makeLiteralNames() {
        return new String[]{
                null, "';'", "':'", "'bool'", "'bytes'", "'double'", "'enum'", "'extend'",
                "'fixed32'", "'fixed64'", "'float'", "'import'", "'int32'", "'int64'",
                "'map'", "'message'", "'oneof'", "'option'", "'package'", "'public'",
                "'repeated'", "'required'", "'reserved'", "'returns'", "'rpc'", "'service'",
                "'sfixed32'", "'sfixed64'", "'sint32'", "'sint64'", "'stream'", "'string'",
                "'syntax'", "'to'", "'uint32'", "'uint64'", "'weak'", "'optional'", null,
                null, null, null, null, null, null, "'('", "')'", "'{'", "'}'", "'['",
                "']'", "'<'", "'>'", "','", "'.'", "'-'", "'+'", "'='", null, "'\uFEFF'"
        };
    }
    private static final String[] _LITERAL_NAMES = makeLiteralNames();

    private static String[] makeSymbolicNames() {
        return new String[]{
                null, "SEMI", "COLON", "BOOL", "BYTES", "DOUBLE", "ENUM", "EXTEND", "FIXED32",
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
        _interp = new LexerATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
    }

    @Override
    public String getGrammarFileName() {
        return "Protobuf2Lexer.g4";
    }

    @Override
    public String[] getRuleNames() {
        return ruleNames;
    }

    @Override
    public String getSerializedATN() {
        return _serializedATN;
    }

    @Override
    public String[] getChannelNames() {
        return channelNames;
    }

    @Override
    public String[] getModeNames() {
        return modeNames;
    }

    @Override
    public ATN getATN() {
        return _ATN;
    }

    public static final String _serializedATN =
            "\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2?\u0254\b\1\4\2\t" +
                    "\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13" +
                    "\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22" +
                    "\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31" +
                    "\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!" +
                    "\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t+\4" +
                    ",\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64\t" +
                    "\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\4<\t<\4=\t=" +
                    "\4>\t>\4?\t?\4@\t@\4A\tA\4B\tB\4C\tC\4D\tD\4E\tE\4F\tF\4G\tG\4H\tH\4I" +
                    "\tI\4J\tJ\4K\tK\3\2\3\2\3\3\3\3\3\4\3\4\3\4\3\4\3\4\3\5\3\5\3\5\3\5\3" +
                    "\5\3\5\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\7\3\7\3\7\3\7\3\7\3\b\3\b\3\b\3\b" +
                    "\3\b\3\b\3\b\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\n\3\n\3\n\3\n\3\n\3\n\3" +
                    "\n\3\n\3\13\3\13\3\13\3\13\3\13\3\13\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\r\3" +
                    "\r\3\r\3\r\3\r\3\r\3\16\3\16\3\16\3\16\3\16\3\16\3\17\3\17\3\17\3\17\3" +
                    "\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\21\3\21\3\21\3\21\3\21\3\21\3" +
                    "\22\3\22\3\22\3\22\3\22\3\22\3\22\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3" +
                    "\23\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\25\3\25\3\25\3\25\3\25\3\25\3" +
                    "\25\3\25\3\25\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\27\3\27\3" +
                    "\27\3\27\3\27\3\27\3\27\3\27\3\27\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3" +
                    "\30\3\31\3\31\3\31\3\31\3\32\3\32\3\32\3\32\3\32\3\32\3\32\3\32\3\33\3" +
                    "\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\34\3\34\3\34\3\34\3\34\3\34\3" +
                    "\34\3\34\3\34\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\36\3\36\3\36\3\36\3" +
                    "\36\3\36\3\36\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3 \3 \3 \3 \3 \3 \3 " +
                    "\3!\3!\3!\3!\3!\3!\3!\3\"\3\"\3\"\3#\3#\3#\3#\3#\3#\3#\3$\3$\3$\3$\3$" +
                    "\3$\3$\3%\3%\3%\3%\3%\3&\3&\3&\3&\3&\3&\3&\3&\3&\3\'\3\'\3(\3(\3)\3)\3" +
                    "*\3*\3+\3+\3+\7+\u0199\n+\f+\16+\u019c\13+\3,\3,\3,\5,\u01a1\n,\3-\3-" +
                    "\5-\u01a5\n-\3-\3-\5-\u01a9\n-\3.\3.\7.\u01ad\n.\f.\16.\u01b0\13.\3/\3" +
                    "/\7/\u01b4\n/\f/\16/\u01b7\13/\3\60\3\60\3\60\6\60\u01bc\n\60\r\60\16" +
                    "\60\u01bd\3\61\3\61\3\61\5\61\u01c3\n\61\3\61\5\61\u01c6\n\61\3\61\3\61" +
                    "\3\61\3\61\3\61\3\61\5\61\u01ce\n\61\5\61\u01d0\n\61\3\61\3\61\3\61\3" +
                    "\61\3\61\3\61\5\61\u01d8\n\61\3\62\6\62\u01db\n\62\r\62\16\62\u01dc\3" +
                    "\63\3\63\5\63\u01e1\n\63\3\63\3\63\3\64\3\64\3\64\3\64\3\64\3\64\3\64" +
                    "\3\64\3\64\5\64\u01ee\n\64\3\65\3\65\7\65\u01f2\n\65\f\65\16\65\u01f5" +
                    "\13\65\3\65\3\65\3\65\7\65\u01fa\n\65\f\65\16\65\u01fd\13\65\3\65\5\65" +
                    "\u0200\n\65\3\66\3\66\3\66\3\66\5\66\u0206\n\66\3\67\3\67\3\67\3\67\3" +
                    "\67\38\38\38\38\38\39\39\39\3:\3:\3;\3;\3<\3<\3=\3=\3>\3>\3?\3?\3@\3@" +
                    "\3A\3A\3B\3B\3C\3C\3D\3D\3E\3E\3F\3F\3G\3G\3H\6H\u0232\nH\rH\16H\u0233" +
                    "\3H\3H\3I\3I\3I\3I\3J\3J\3J\3J\7J\u0240\nJ\fJ\16J\u0243\13J\3J\3J\3J\3" +
                    "J\3J\3K\3K\3K\3K\7K\u024e\nK\fK\16K\u0251\13K\3K\3K\3\u0241\2L\3\3\5\4" +
                    "\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21!\22" +
                    "#\23%\24\'\25)\26+\27-\30/\31\61\32\63\33\65\34\67\359\36;\37= ?!A\"C" +
                    "#E$G%I&K\'M\2O\2Q\2S\2U(W)Y*[\2]\2_\2a+c\2e\2g,i-k\2m\2o\2q\2s.u/w\60" +
                    "y\61{\62}\63\177\64\u0081\65\u0083\66\u0085\67\u00878\u00899\u008b:\u008d" +
                    ";\u008f<\u0091=\u0093>\u0095?\3\2\17\5\2C\\aac|\3\2\62;\3\2\629\5\2\62" +
                    ";CHch\3\2\63;\4\2ZZzz\4\2GGgg\4\2--//\5\2\2\2\f\f^^\13\2$$))^^cdhhppt" +
                    "tvvxx\4\2$$))\b\2\13\f\17\17\"\"\u00a2\u00a2\u2005\u2005\uff01\uff01\4" +
                    "\2\f\f\17\17\2\u0263\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2" +
                    "\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3" +
                    "\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2" +
                    "\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2" +
                    "\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\2\63\3\2\2\2\2\65\3\2\2\2\2\67\3\2" +
                    "\2\2\29\3\2\2\2\2;\3\2\2\2\2=\3\2\2\2\2?\3\2\2\2\2A\3\2\2\2\2C\3\2\2\2" +
                    "\2E\3\2\2\2\2G\3\2\2\2\2I\3\2\2\2\2K\3\2\2\2\2U\3\2\2\2\2W\3\2\2\2\2Y" +
                    "\3\2\2\2\2a\3\2\2\2\2g\3\2\2\2\2i\3\2\2\2\2s\3\2\2\2\2u\3\2\2\2\2w\3\2" +
                    "\2\2\2y\3\2\2\2\2{\3\2\2\2\2}\3\2\2\2\2\177\3\2\2\2\2\u0081\3\2\2\2\2" +
                    "\u0083\3\2\2\2\2\u0085\3\2\2\2\2\u0087\3\2\2\2\2\u0089\3\2\2\2\2\u008b" +
                    "\3\2\2\2\2\u008d\3\2\2\2\2\u008f\3\2\2\2\2\u0091\3\2\2\2\2\u0093\3\2\2" +
                    "\2\2\u0095\3\2\2\2\3\u0097\3\2\2\2\5\u0099\3\2\2\2\7\u009b\3\2\2\2\t\u00a0" +
                    "\3\2\2\2\13\u00a6\3\2\2\2\r\u00ad\3\2\2\2\17\u00b2\3\2\2\2\21\u00b9\3" +
                    "\2\2\2\23\u00c1\3\2\2\2\25\u00c9\3\2\2\2\27\u00cf\3\2\2\2\31\u00d6\3\2" +
                    "\2\2\33\u00dc\3\2\2\2\35\u00e2\3\2\2\2\37\u00e6\3\2\2\2!\u00ee\3\2\2\2" +
                    "#\u00f4\3\2\2\2%\u00fb\3\2\2\2\'\u0103\3\2\2\2)\u010a\3\2\2\2+\u0113\3" +
                    "\2\2\2-\u011c\3\2\2\2/\u0125\3\2\2\2\61\u012d\3\2\2\2\63\u0131\3\2\2\2" +
                    "\65\u0139\3\2\2\2\67\u0142\3\2\2\29\u014b\3\2\2\2;\u0152\3\2\2\2=\u0159" +
                    "\3\2\2\2?\u0160\3\2\2\2A\u0167\3\2\2\2C\u016e\3\2\2\2E\u0171\3\2\2\2G" +
                    "\u0178\3\2\2\2I\u017f\3\2\2\2K\u0184\3\2\2\2M\u018d\3\2\2\2O\u018f\3\2" +
                    "\2\2Q\u0191\3\2\2\2S\u0193\3\2\2\2U\u0195\3\2\2\2W\u01a0\3\2\2\2Y\u01a4" +
                    "\3\2\2\2[\u01aa\3\2\2\2]\u01b1\3\2\2\2_\u01b8\3\2\2\2a\u01d7\3\2\2\2c" +
                    "\u01da\3\2\2\2e\u01de\3\2\2\2g\u01ed\3\2\2\2i\u01ff\3\2\2\2k\u0205\3\2" +
                    "\2\2m\u0207\3\2\2\2o\u020c\3\2\2\2q\u0211\3\2\2\2s\u0214\3\2\2\2u\u0216" +
                    "\3\2\2\2w\u0218\3\2\2\2y\u021a\3\2\2\2{\u021c\3\2\2\2}\u021e\3\2\2\2\177" +
                    "\u0220\3\2\2\2\u0081\u0222\3\2\2\2\u0083\u0224\3\2\2\2\u0085\u0226\3\2" +
                    "\2\2\u0087\u0228\3\2\2\2\u0089\u022a\3\2\2\2\u008b\u022c\3\2\2\2\u008d" +
                    "\u022e\3\2\2\2\u008f\u0231\3\2\2\2\u0091\u0237\3\2\2\2\u0093\u023b\3\2" +
                    "\2\2\u0095\u0249\3\2\2\2\u0097\u0098\7=\2\2\u0098\4\3\2\2\2\u0099\u009a" +
                    "\7<\2\2\u009a\6\3\2\2\2\u009b\u009c\7d\2\2\u009c\u009d\7q\2\2\u009d\u009e" +
                    "\7q\2\2\u009e\u009f\7n\2\2\u009f\b\3\2\2\2\u00a0\u00a1\7d\2\2\u00a1\u00a2" +
                    "\7{\2\2\u00a2\u00a3\7v\2\2\u00a3\u00a4\7g\2\2\u00a4\u00a5\7u\2\2\u00a5" +
                    "\n\3\2\2\2\u00a6\u00a7\7f\2\2\u00a7\u00a8\7q\2\2\u00a8\u00a9\7w\2\2\u00a9" +
                    "\u00aa\7d\2\2\u00aa\u00ab\7n\2\2\u00ab\u00ac\7g\2\2\u00ac\f\3\2\2\2\u00ad" +
                    "\u00ae\7g\2\2\u00ae\u00af\7p\2\2\u00af\u00b0\7w\2\2\u00b0\u00b1\7o\2\2" +
                    "\u00b1\16\3\2\2\2\u00b2\u00b3\7g\2\2\u00b3\u00b4\7z\2\2\u00b4\u00b5\7" +
                    "v\2\2\u00b5\u00b6\7g\2\2\u00b6\u00b7\7p\2\2\u00b7\u00b8\7f\2\2\u00b8\20" +
                    "\3\2\2\2\u00b9\u00ba\7h\2\2\u00ba\u00bb\7k\2\2\u00bb\u00bc\7z\2\2\u00bc" +
                    "\u00bd\7g\2\2\u00bd\u00be\7f\2\2\u00be\u00bf\7\65\2\2\u00bf\u00c0\7\64" +
                    "\2\2\u00c0\22\3\2\2\2\u00c1\u00c2\7h\2\2\u00c2\u00c3\7k\2\2\u00c3\u00c4" +
                    "\7z\2\2\u00c4\u00c5\7g\2\2\u00c5\u00c6\7f\2\2\u00c6\u00c7\78\2\2\u00c7" +
                    "\u00c8\7\66\2\2\u00c8\24\3\2\2\2\u00c9\u00ca\7h\2\2\u00ca\u00cb\7n\2\2" +
                    "\u00cb\u00cc\7q\2\2\u00cc\u00cd\7c\2\2\u00cd\u00ce\7v\2\2\u00ce\26\3\2" +
                    "\2\2\u00cf\u00d0\7k\2\2\u00d0\u00d1\7o\2\2\u00d1\u00d2\7r\2\2\u00d2\u00d3" +
                    "\7q\2\2\u00d3\u00d4\7t\2\2\u00d4\u00d5\7v\2\2\u00d5\30\3\2\2\2\u00d6\u00d7" +
                    "\7k\2\2\u00d7\u00d8\7p\2\2\u00d8\u00d9\7v\2\2\u00d9\u00da\7\65\2\2\u00da" +
                    "\u00db\7\64\2\2\u00db\32\3\2\2\2\u00dc\u00dd\7k\2\2\u00dd\u00de\7p\2\2" +
                    "\u00de\u00df\7v\2\2\u00df\u00e0\78\2\2\u00e0\u00e1\7\66\2\2\u00e1\34\3" +
                    "\2\2\2\u00e2\u00e3\7o\2\2\u00e3\u00e4\7c\2\2\u00e4\u00e5\7r\2\2\u00e5" +
                    "\36\3\2\2\2\u00e6\u00e7\7o\2\2\u00e7\u00e8\7g\2\2\u00e8\u00e9\7u\2\2\u00e9" +
                    "\u00ea\7u\2\2\u00ea\u00eb\7c\2\2\u00eb\u00ec\7i\2\2\u00ec\u00ed\7g\2\2" +
                    "\u00ed \3\2\2\2\u00ee\u00ef\7q\2\2\u00ef\u00f0\7p\2\2\u00f0\u00f1\7g\2" +
                    "\2\u00f1\u00f2\7q\2\2\u00f2\u00f3\7h\2\2\u00f3\"\3\2\2\2\u00f4\u00f5\7" +
                    "q\2\2\u00f5\u00f6\7r\2\2\u00f6\u00f7\7v\2\2\u00f7\u00f8\7k\2\2\u00f8\u00f9" +
                    "\7q\2\2\u00f9\u00fa\7p\2\2\u00fa$\3\2\2\2\u00fb\u00fc\7r\2\2\u00fc\u00fd" +
                    "\7c\2\2\u00fd\u00fe\7e\2\2\u00fe\u00ff\7m\2\2\u00ff\u0100\7c\2\2\u0100" +
                    "\u0101\7i\2\2\u0101\u0102\7g\2\2\u0102&\3\2\2\2\u0103\u0104\7r\2\2\u0104" +
                    "\u0105\7w\2\2\u0105\u0106\7d\2\2\u0106\u0107\7n\2\2\u0107\u0108\7k\2\2" +
                    "\u0108\u0109\7e\2\2\u0109(\3\2\2\2\u010a\u010b\7t\2\2\u010b\u010c\7g\2" +
                    "\2\u010c\u010d\7r\2\2\u010d\u010e\7g\2\2\u010e\u010f\7c\2\2\u010f\u0110" +
                    "\7v\2\2\u0110\u0111\7g\2\2\u0111\u0112\7f\2\2\u0112*\3\2\2\2\u0113\u0114" +
                    "\7t\2\2\u0114\u0115\7g\2\2\u0115\u0116\7s\2\2\u0116\u0117\7w\2\2\u0117" +
                    "\u0118\7k\2\2\u0118\u0119\7t\2\2\u0119\u011a\7g\2\2\u011a\u011b\7f\2\2" +
                    "\u011b,\3\2\2\2\u011c\u011d\7t\2\2\u011d\u011e\7g\2\2\u011e\u011f\7u\2" +
                    "\2\u011f\u0120\7g\2\2\u0120\u0121\7t\2\2\u0121\u0122\7x\2\2\u0122\u0123" +
                    "\7g\2\2\u0123\u0124\7f\2\2\u0124.\3\2\2\2\u0125\u0126\7t\2\2\u0126\u0127" +
                    "\7g\2\2\u0127\u0128\7v\2\2\u0128\u0129\7w\2\2\u0129\u012a\7t\2\2\u012a" +
                    "\u012b\7p\2\2\u012b\u012c\7u\2\2\u012c\60\3\2\2\2\u012d\u012e\7t\2\2\u012e" +
                    "\u012f\7r\2\2\u012f\u0130\7e\2\2\u0130\62\3\2\2\2\u0131\u0132\7u\2\2\u0132" +
                    "\u0133\7g\2\2\u0133\u0134\7t\2\2\u0134\u0135\7x\2\2\u0135\u0136\7k\2\2" +
                    "\u0136\u0137\7e\2\2\u0137\u0138\7g\2\2\u0138\64\3\2\2\2\u0139\u013a\7" +
                    "u\2\2\u013a\u013b\7h\2\2\u013b\u013c\7k\2\2\u013c\u013d\7z\2\2\u013d\u013e" +
                    "\7g\2\2\u013e\u013f\7f\2\2\u013f\u0140\7\65\2\2\u0140\u0141\7\64\2\2\u0141" +
                    "\66\3\2\2\2\u0142\u0143\7u\2\2\u0143\u0144\7h\2\2\u0144\u0145\7k\2\2\u0145" +
                    "\u0146\7z\2\2\u0146\u0147\7g\2\2\u0147\u0148\7f\2\2\u0148\u0149\78\2\2" +
                    "\u0149\u014a\7\66\2\2\u014a8\3\2\2\2\u014b\u014c\7u\2\2\u014c\u014d\7" +
                    "k\2\2\u014d\u014e\7p\2\2\u014e\u014f\7v\2\2\u014f\u0150\7\65\2\2\u0150" +
                    "\u0151\7\64\2\2\u0151:\3\2\2\2\u0152\u0153\7u\2\2\u0153\u0154\7k\2\2\u0154" +
                    "\u0155\7p\2\2\u0155\u0156\7v\2\2\u0156\u0157\78\2\2\u0157\u0158\7\66\2" +
                    "\2\u0158<\3\2\2\2\u0159\u015a\7u\2\2\u015a\u015b\7v\2\2\u015b\u015c\7" +
                    "t\2\2\u015c\u015d\7g\2\2\u015d\u015e\7c\2\2\u015e\u015f\7o\2\2\u015f>" +
                    "\3\2\2\2\u0160\u0161\7u\2\2\u0161\u0162\7v\2\2\u0162\u0163\7t\2\2\u0163" +
                    "\u0164\7k\2\2\u0164\u0165\7p\2\2\u0165\u0166\7i\2\2\u0166@\3\2\2\2\u0167" +
                    "\u0168\7u\2\2\u0168\u0169\7{\2\2\u0169\u016a\7p\2\2\u016a\u016b\7v\2\2" +
                    "\u016b\u016c\7c\2\2\u016c\u016d\7z\2\2\u016dB\3\2\2\2\u016e\u016f\7v\2" +
                    "\2\u016f\u0170\7q\2\2\u0170D\3\2\2\2\u0171\u0172\7w\2\2\u0172\u0173\7" +
                    "k\2\2\u0173\u0174\7p\2\2\u0174\u0175\7v\2\2\u0175\u0176\7\65\2\2\u0176" +
                    "\u0177\7\64\2\2\u0177F\3\2\2\2\u0178\u0179\7w\2\2\u0179\u017a\7k\2\2\u017a" +
                    "\u017b\7p\2\2\u017b\u017c\7v\2\2\u017c\u017d\78\2\2\u017d\u017e\7\66\2" +
                    "\2\u017eH\3\2\2\2\u017f\u0180\7y\2\2\u0180\u0181\7g\2\2\u0181\u0182\7" +
                    "c\2\2\u0182\u0183\7m\2\2\u0183J\3\2\2\2\u0184\u0185\7q\2\2\u0185\u0186" +
                    "\7r\2\2\u0186\u0187\7v\2\2\u0187\u0188\7k\2\2\u0188\u0189\7q\2\2\u0189" +
                    "\u018a\7p\2\2\u018a\u018b\7c\2\2\u018b\u018c\7n\2\2\u018cL\3\2\2\2\u018d" +
                    "\u018e\t\2\2\2\u018eN\3\2\2\2\u018f\u0190\t\3\2\2\u0190P\3\2\2\2\u0191" +
                    "\u0192\t\4\2\2\u0192R\3\2\2\2\u0193\u0194\t\5\2\2\u0194T\3\2\2\2\u0195" +
                    "\u019a\5M\'\2\u0196\u0199\5M\'\2\u0197\u0199\5O(\2\u0198\u0196\3\2\2\2" +
                    "\u0198\u0197\3\2\2\2\u0199\u019c\3\2\2\2\u019a\u0198\3\2\2\2\u019a\u019b" +
                    "\3\2\2\2\u019bV\3\2\2\2\u019c\u019a\3\2\2\2\u019d\u01a1\5[.\2\u019e\u01a1" +
                    "\5]/\2\u019f\u01a1\5_\60\2\u01a0\u019d\3\2\2\2\u01a0\u019e\3\2\2\2\u01a0" +
                    "\u019f\3\2\2\2\u01a1X\3\2\2\2\u01a2\u01a5\5\u0089E\2\u01a3\u01a5\5\u008b" +
                    "F\2\u01a4\u01a2\3\2\2\2\u01a4\u01a3\3\2\2\2\u01a4\u01a5\3\2\2\2\u01a5" +
                    "\u01a8\3\2\2\2\u01a6\u01a9\5W,\2\u01a7\u01a9\5a\61\2\u01a8\u01a6\3\2\2" +
                    "\2\u01a8\u01a7\3\2\2\2\u01a9Z\3\2\2\2\u01aa\u01ae\t\6\2\2\u01ab\u01ad" +
                    "\5O(\2\u01ac\u01ab\3\2\2\2\u01ad\u01b0\3\2\2\2\u01ae\u01ac\3\2\2\2\u01ae" +
                    "\u01af\3\2\2\2\u01af\\\3\2\2\2\u01b0\u01ae\3\2\2\2\u01b1\u01b5\7\62\2" +
                    "\2\u01b2\u01b4\5Q)\2\u01b3\u01b2\3\2\2\2\u01b4\u01b7\3\2\2\2\u01b5\u01b3" +
                    "\3\2\2\2\u01b5\u01b6\3\2\2\2\u01b6^\3\2\2\2\u01b7\u01b5\3\2\2\2\u01b8" +
                    "\u01b9\7\62\2\2\u01b9\u01bb\t\7\2\2\u01ba\u01bc\5S*\2\u01bb\u01ba\3\2" +
                    "\2\2\u01bc\u01bd\3\2\2\2\u01bd\u01bb\3\2\2\2\u01bd\u01be\3\2\2\2\u01be" +
                    "`\3\2\2\2\u01bf\u01c0\5c\62\2\u01c0\u01c2\7\60\2\2\u01c1\u01c3\5c\62\2" +
                    "\u01c2\u01c1\3\2\2\2\u01c2\u01c3\3\2\2\2\u01c3\u01c5\3\2\2\2\u01c4\u01c6" +
                    "\5e\63\2\u01c5\u01c4\3\2\2\2\u01c5\u01c6\3\2\2\2\u01c6\u01d0\3\2\2\2\u01c7" +
                    "\u01c8\5c\62\2\u01c8\u01c9\5e\63\2\u01c9\u01d0\3\2\2\2\u01ca\u01cb\7\60" +
                    "\2\2\u01cb\u01cd\5c\62\2\u01cc\u01ce\5e\63\2\u01cd\u01cc\3\2\2\2\u01cd" +
                    "\u01ce\3\2\2\2\u01ce\u01d0\3\2\2\2\u01cf\u01bf\3\2\2\2\u01cf\u01c7\3\2" +
                    "\2\2\u01cf\u01ca\3\2\2\2\u01d0\u01d8\3\2\2\2\u01d1\u01d2\7k\2\2\u01d2" +
                    "\u01d3\7p\2\2\u01d3\u01d8\7h\2\2\u01d4\u01d5\7p\2\2\u01d5\u01d6\7c\2\2" +
                    "\u01d6\u01d8\7p\2\2\u01d7\u01cf\3\2\2\2\u01d7\u01d1\3\2\2\2\u01d7\u01d4" +
                    "\3\2\2\2\u01d8b\3\2\2\2\u01d9\u01db\5O(\2\u01da\u01d9\3\2\2\2\u01db\u01dc" +
                    "\3\2\2\2\u01dc\u01da\3\2\2\2\u01dc\u01dd\3\2\2\2\u01ddd\3\2\2\2\u01de" +
                    "\u01e0\t\b\2\2\u01df\u01e1\t\t\2\2\u01e0\u01df\3\2\2\2\u01e0\u01e1\3\2" +
                    "\2\2\u01e1\u01e2\3\2\2\2\u01e2\u01e3\5c\62\2\u01e3f\3\2\2\2\u01e4\u01e5" +
                    "\7v\2\2\u01e5\u01e6\7t\2\2\u01e6\u01e7\7w\2\2\u01e7\u01ee\7g\2\2\u01e8" +
                    "\u01e9\7h\2\2\u01e9\u01ea\7c\2\2\u01ea\u01eb\7n\2\2\u01eb\u01ec\7u\2\2" +
                    "\u01ec\u01ee\7g\2\2\u01ed\u01e4\3\2\2\2\u01ed\u01e8\3\2\2\2\u01eeh\3\2" +
                    "\2\2\u01ef\u01f3\7)\2\2\u01f0\u01f2\5k\66\2\u01f1\u01f0\3\2\2\2\u01f2" +
                    "\u01f5\3\2\2\2\u01f3\u01f1\3\2\2\2\u01f3\u01f4\3\2\2\2\u01f4\u01f6\3\2" +
                    "\2\2\u01f5\u01f3\3\2\2\2\u01f6\u0200\7)\2\2\u01f7\u01fb\7$\2\2\u01f8\u01fa" +
                    "\5k\66\2\u01f9\u01f8\3\2\2\2\u01fa\u01fd\3\2\2\2\u01fb\u01f9\3\2\2\2\u01fb" +
                    "\u01fc\3\2\2\2\u01fc\u01fe\3\2\2\2\u01fd\u01fb\3\2\2\2\u01fe\u0200\7$" +
                    "\2\2\u01ff\u01ef\3\2\2\2\u01ff\u01f7\3\2\2\2\u0200j\3\2\2\2\u0201\u0206" +
                    "\5m\67\2\u0202\u0206\5o8\2\u0203\u0206\5q9\2\u0204\u0206\n\n\2\2\u0205" +
                    "\u0201\3\2\2\2\u0205\u0202\3\2\2\2\u0205\u0203\3\2\2\2\u0205\u0204\3\2" +
                    "\2\2\u0206l\3\2\2\2\u0207\u0208\7^\2\2\u0208\u0209\t\7\2\2\u0209\u020a" +
                    "\5S*\2\u020a\u020b\5S*\2\u020bn\3\2\2\2\u020c\u020d\7^\2\2\u020d\u020e" +
                    "\5Q)\2\u020e\u020f\5Q)\2\u020f\u0210\5Q)\2\u0210p\3\2\2\2\u0211\u0212" +
                    "\7^\2\2\u0212\u0213\t\13\2\2\u0213r\3\2\2\2\u0214\u0215\t\f\2\2\u0215" +
                    "t\3\2\2\2\u0216\u0217\7*\2\2\u0217v\3\2\2\2\u0218\u0219\7+\2\2\u0219x" +
                    "\3\2\2\2\u021a\u021b\7}\2\2\u021bz\3\2\2\2\u021c\u021d\7\177\2\2\u021d" +
                    "|\3\2\2\2\u021e\u021f\7]\2\2\u021f~\3\2\2\2\u0220\u0221\7_\2\2\u0221\u0080" +
                    "\3\2\2\2\u0222\u0223\7>\2\2\u0223\u0082\3\2\2\2\u0224\u0225\7@\2\2\u0225" +
                    "\u0084\3\2\2\2\u0226\u0227\7.\2\2\u0227\u0086\3\2\2\2\u0228\u0229\7\60" +
                    "\2\2\u0229\u0088\3\2\2\2\u022a\u022b\7/\2\2\u022b\u008a\3\2\2\2\u022c" +
                    "\u022d\7-\2\2\u022d\u008c\3\2\2\2\u022e\u022f\7?\2\2\u022f\u008e\3\2\2" +
                    "\2\u0230\u0232\t\r\2\2\u0231\u0230\3\2\2\2\u0232\u0233\3\2\2\2\u0233\u0231" +
                    "\3\2\2\2\u0233\u0234\3\2\2\2\u0234\u0235\3\2\2\2\u0235\u0236\bH\2\2\u0236" +
                    "\u0090\3\2\2\2\u0237\u0238\7\uff01\2\2\u0238\u0239\3\2\2\2\u0239\u023a" +
                    "\bI\2\2\u023a\u0092\3\2\2\2\u023b\u023c\7\61\2\2\u023c\u023d\7,\2\2\u023d" +
                    "\u0241\3\2\2\2\u023e\u0240\13\2\2\2\u023f\u023e\3\2\2\2\u0240\u0243\3" +
                    "\2\2\2\u0241\u0242\3\2\2\2\u0241\u023f\3\2\2\2\u0242\u0244\3\2\2\2\u0243" +
                    "\u0241\3\2\2\2\u0244\u0245\7,\2\2\u0245\u0246\7\61\2\2\u0246\u0247\3\2" +
                    "\2\2\u0247\u0248\bJ\2\2\u0248\u0094\3\2\2\2\u0249\u024a\7\61\2\2\u024a" +
                    "\u024b\7\61\2\2\u024b\u024f\3\2\2\2\u024c\u024e\n\16\2\2\u024d\u024c\3" +
                    "\2\2\2\u024e\u0251\3\2\2\2\u024f\u024d\3\2\2\2\u024f\u0250\3\2\2\2\u0250" +
                    "\u0252\3\2\2\2\u0251\u024f\3\2\2\2\u0252\u0253\bK\2\2\u0253\u0096\3\2" +
                    "\2\2\32\2\u0198\u019a\u01a0\u01a4\u01a8\u01ae\u01b5\u01bd\u01c2\u01c5" +
                    "\u01cd\u01cf\u01d7\u01dc\u01e0\u01ed\u01f3\u01fb\u01ff\u0205\u0233\u0241" +
                    "\u024f\3\b\2\2";
    public static final ATN _ATN =
            new ATNDeserializer().deserialize(_serializedATN.toCharArray());

    static {
        _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
        for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
            _decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
        }
    }
}