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
// Generated from /Users/yoshi/Development/Repos/openrewrite/rewrite/rewrite-json/src/main/antlr/JSON5.g4 by ANTLR 4.9.3
package org.openrewrite.json.internal.grammar;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class JSON5Lexer extends Lexer {
    static {
        RuntimeMetaData.checkVersion("4.9.3", RuntimeMetaData.VERSION);
    }

    protected static final DFA[] _decisionToDFA;
    protected static final PredictionContextCache _sharedContextCache =
            new PredictionContextCache();
    public static final int
            T__0 = 1, T__1 = 2, T__2 = 3, T__3 = 4, T__4 = 5, T__5 = 6, SINGLE_LINE_COMMENT = 7,
            MULTI_LINE_COMMENT = 8, LITERAL = 9, STRING = 10, NUMBER = 11, NUMERIC_LITERAL = 12,
            SYMBOL = 13, IDENTIFIER = 14, WS = 15, UTF_8_BOM = 16;
    public static String[] channelNames = {
            "DEFAULT_TOKEN_CHANNEL", "HIDDEN"
    };

    public static String[] modeNames = {
            "DEFAULT_MODE"
    };

    private static String[] makeRuleNames() {
        return new String[]{
                "T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "SINGLE_LINE_COMMENT",
                "MULTI_LINE_COMMENT", "LITERAL", "STRING", "DOUBLE_QUOTE_CHAR", "SINGLE_QUOTE_CHAR",
                "ESCAPE_SEQUENCE", "NUMBER", "NUMERIC_LITERAL", "SYMBOL", "HEX", "INT",
                "EXP", "IDENTIFIER", "IDENTIFIER_START", "IDENTIFIER_PART", "UNICODE_SEQUENCE",
                "NEWLINE", "WS", "UTF_8_BOM"
        };
    }
    public static final String[] ruleNames = makeRuleNames();

    private static String[] makeLiteralNames() {
        return new String[]{
                null, "'{'", "','", "'}'", "':'", "'['", "']'", null, null, null, null,
                null, null, null, null, null, "'\uFEFF'"
        };
    }
    private static final String[] _LITERAL_NAMES = makeLiteralNames();

    private static String[] makeSymbolicNames() {
        return new String[]{
                null, null, null, null, null, null, null, "SINGLE_LINE_COMMENT", "MULTI_LINE_COMMENT",
                "LITERAL", "STRING", "NUMBER", "NUMERIC_LITERAL", "SYMBOL", "IDENTIFIER",
                "WS", "UTF_8_BOM"
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


    public JSON5Lexer(CharStream input) {
        super(input);
        _interp = new LexerATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
    }

    @Override
    public String getGrammarFileName() {
        return "JSON5.g4";
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
            "\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\22\u00ff\b\1\4\2" +
                    "\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4" +
                    "\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22" +
                    "\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31" +
                    "\t\31\4\32\t\32\4\33\t\33\3\2\3\2\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7" +
                    "\3\7\3\b\3\b\3\b\3\b\7\bH\n\b\f\b\16\bK\13\b\3\b\3\b\5\bO\n\b\3\b\3\b" +
                    "\3\t\3\t\3\t\3\t\7\tW\n\t\f\t\16\tZ\13\t\3\t\3\t\3\t\3\t\3\t\3\n\3\n\3" +
                    "\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\5\nn\n\n\3\13\3\13\7\13r\n" +
                    "\13\f\13\16\13u\13\13\3\13\3\13\3\13\7\13z\n\13\f\13\16\13}\13\13\3\13" +
                    "\5\13\u0080\n\13\3\f\3\f\5\f\u0084\n\f\3\r\3\r\5\r\u0088\n\r\3\16\3\16" +
                    "\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\5\16\u0094\n\16\3\17\3\17\3\17" +
                    "\7\17\u0099\n\17\f\17\16\17\u009c\13\17\5\17\u009e\n\17\3\17\5\17\u00a1" +
                    "\n\17\3\17\3\17\6\17\u00a5\n\17\r\17\16\17\u00a6\3\17\5\17\u00aa\n\17" +
                    "\3\17\3\17\3\17\6\17\u00af\n\17\r\17\16\17\u00b0\5\17\u00b3\n\17\3\20" +
                    "\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\5\20\u00c0\n\20\3\21" +
                    "\3\21\3\22\3\22\3\23\3\23\3\23\7\23\u00c9\n\23\f\23\16\23\u00cc\13\23" +
                    "\5\23\u00ce\n\23\3\24\3\24\5\24\u00d2\n\24\3\24\7\24\u00d5\n\24\f\24\16" +
                    "\24\u00d8\13\24\3\25\3\25\7\25\u00dc\n\25\f\25\16\25\u00df\13\25\3\26" +
                    "\3\26\3\26\5\26\u00e4\n\26\3\27\3\27\5\27\u00e8\n\27\3\30\3\30\3\30\3" +
                    "\30\3\30\3\30\3\31\3\31\3\31\5\31\u00f3\n\31\3\32\6\32\u00f6\n\32\r\32" +
                    "\16\32\u00f7\3\32\3\32\3\33\3\33\3\33\3\33\4IX\2\34\3\3\5\4\7\5\t\6\13" +
                    "\7\r\b\17\t\21\n\23\13\25\f\27\2\31\2\33\2\35\r\37\16!\17#\2%\2\'\2)\20" +
                    "+\2-\2/\2\61\2\63\21\65\22\3\2\16\6\2\f\f\17\17$$^^\6\2\f\f\17\17))^^" +
                    "\f\2$$))\61\61^^ddhhppttvvxx\16\2\f\f\17\17$$))\62;^^ddhhppttvxzz\3\2" +
                    "\62;\4\2ZZzz\4\2--//\5\2\62;CHch\3\2\63;\4\2GGgg\5\2\f\f\17\17\u202a\u202b" +
                    "\b\2\13\f\17\17\"\"\u00a2\u00a2\u2005\u2005\uff01\uff01\4\u0272\2&\2&" +
                    "\2C\2\\\2a\2a\2c\2|\2\u00ac\2\u00ac\2\u00b7\2\u00b7\2\u00bc\2\u00bc\2" +
                    "\u00c2\2\u00d8\2\u00da\2\u00f8\2\u00fa\2\u02c3\2\u02c8\2\u02d3\2\u02e2" +
                    "\2\u02e6\2\u02ee\2\u02ee\2\u02f0\2\u02f0\2\u0372\2\u0376\2\u0378\2\u0379" +
                    "\2\u037c\2\u037f\2\u0381\2\u0381\2\u0388\2\u0388\2\u038a\2\u038c\2\u038e" +
                    "\2\u038e\2\u0390\2\u03a3\2\u03a5\2\u03f7\2\u03f9\2\u0483\2\u048c\2\u0531" +
                    "\2\u0533\2\u0558\2\u055b\2\u055b\2\u0562\2\u058a\2\u05d2\2\u05ec\2\u05f1" +
                    "\2\u05f4\2\u0622\2\u064c\2\u0670\2\u0671\2\u0673\2\u06d5\2\u06d7\2\u06d7" +
                    "\2\u06e7\2\u06e8\2\u06f0\2\u06f1\2\u06fc\2\u06fe\2\u0701\2\u0701\2\u0712" +
                    "\2\u0712\2\u0714\2\u0731\2\u074f\2\u07a7\2\u07b3\2\u07b3\2\u07cc\2\u07ec" +
                    "\2\u07f6\2\u07f7\2\u07fc\2\u07fc\2\u0802\2\u0817\2\u081c\2\u081c\2\u0826" +
                    "\2\u0826\2\u082a\2\u082a\2\u0842\2\u085a\2\u0862\2\u086c\2\u08a2\2\u08b6" +
                    "\2\u08b8\2\u08c9\2\u0906\2\u093b\2\u093f\2\u093f\2\u0952\2\u0952\2\u095a" +
                    "\2\u0963\2\u0973\2\u0982\2\u0987\2\u098e\2\u0991\2\u0992\2\u0995\2\u09aa" +
                    "\2\u09ac\2\u09b2\2\u09b4\2\u09b4\2\u09b8\2\u09bb\2\u09bf\2\u09bf\2\u09d0" +
                    "\2\u09d0\2\u09de\2\u09df\2\u09e1\2\u09e3\2\u09f2\2\u09f3\2\u09fe\2\u09fe" +
                    "\2\u0a07\2\u0a0c\2\u0a11\2\u0a12\2\u0a15\2\u0a2a\2\u0a2c\2\u0a32\2\u0a34" +
                    "\2\u0a35\2\u0a37\2\u0a38\2\u0a3a\2\u0a3b\2\u0a5b\2\u0a5e\2\u0a60\2\u0a60" +
                    "\2\u0a74\2\u0a76\2\u0a87\2\u0a8f\2\u0a91\2\u0a93\2\u0a95\2\u0aaa\2\u0aac" +
                    "\2\u0ab2\2\u0ab4\2\u0ab5\2\u0ab7\2\u0abb\2\u0abf\2\u0abf\2\u0ad2\2\u0ad2" +
                    "\2\u0ae2\2\u0ae3\2\u0afb\2\u0afb\2\u0b07\2\u0b0e\2\u0b11\2\u0b12\2\u0b15" +
                    "\2\u0b2a\2\u0b2c\2\u0b32\2\u0b34\2\u0b35\2\u0b37\2\u0b3b\2\u0b3f\2\u0b3f" +
                    "\2\u0b5e\2\u0b5f\2\u0b61\2\u0b63\2\u0b73\2\u0b73\2\u0b85\2\u0b85\2\u0b87" +
                    "\2\u0b8c\2\u0b90\2\u0b92\2\u0b94\2\u0b97\2\u0b9b\2\u0b9c\2\u0b9e\2\u0b9e" +
                    "\2\u0ba0\2\u0ba1\2\u0ba5\2\u0ba6\2\u0baa\2\u0bac\2\u0bb0\2\u0bbb\2\u0bd2" +
                    "\2\u0bd2\2\u0c07\2\u0c0e\2\u0c10\2\u0c12\2\u0c14\2\u0c2a\2\u0c2c\2\u0c3b" +
                    "\2\u0c3f\2\u0c3f\2\u0c5a\2\u0c5c\2\u0c62\2\u0c63\2\u0c82\2\u0c82\2\u0c87" +
                    "\2\u0c8e\2\u0c90\2\u0c92\2\u0c94\2\u0caa\2\u0cac\2\u0cb5\2\u0cb7\2\u0cbb" +
                    "\2\u0cbf\2\u0cbf\2\u0ce0\2\u0ce0\2\u0ce2\2\u0ce3\2\u0cf3\2\u0cf4\2\u0d06" +
                    "\2\u0d0e\2\u0d10\2\u0d12\2\u0d14\2\u0d3c\2\u0d3f\2\u0d3f\2\u0d50\2\u0d50" +
                    "\2\u0d56\2\u0d58\2\u0d61\2\u0d63\2\u0d7c\2\u0d81\2\u0d87\2\u0d98\2\u0d9c" +
                    "\2\u0db3\2\u0db5\2\u0dbd\2\u0dbf\2\u0dbf\2\u0dc2\2\u0dc8\2\u0e03\2\u0e32" +
                    "\2\u0e34\2\u0e35\2\u0e42\2\u0e48\2\u0e83\2\u0e84\2\u0e86\2\u0e86\2\u0e88" +
                    "\2\u0e8c\2\u0e8e\2\u0ea5\2\u0ea7\2\u0ea7\2\u0ea9\2\u0eb2\2\u0eb4\2\u0eb5" +
                    "\2\u0ebf\2\u0ebf\2\u0ec2\2\u0ec6\2\u0ec8\2\u0ec8\2\u0ede\2\u0ee1\2\u0f02" +
                    "\2\u0f02\2\u0f42\2\u0f49\2\u0f4b\2\u0f6e\2\u0f8a\2\u0f8e\2\u1002\2\u102c" +
                    "\2\u1041\2\u1041\2\u1052\2\u1057\2\u105c\2\u105f\2\u1063\2\u1063\2\u1067" +
                    "\2\u1068\2\u1070\2\u1072\2\u1077\2\u1083\2\u1090\2\u1090\2\u10a2\2\u10c7" +
                    "\2\u10c9\2\u10c9\2\u10cf\2\u10cf\2\u10d2\2\u10fc\2\u10fe\2\u124a\2\u124c" +
                    "\2\u124f\2\u1252\2\u1258\2\u125a\2\u125a\2\u125c\2\u125f\2\u1262\2\u128a" +
                    "\2\u128c\2\u128f\2\u1292\2\u12b2\2\u12b4\2\u12b7\2\u12ba\2\u12c0\2\u12c2" +
                    "\2\u12c2\2\u12c4\2\u12c7\2\u12ca\2\u12d8\2\u12da\2\u1312\2\u1314\2\u1317" +
                    "\2\u131a\2\u135c\2\u1382\2\u1391\2\u13a2\2\u13f7\2\u13fa\2\u13ff\2\u1403" +
                    "\2\u166e\2\u1671\2\u1681\2\u1683\2\u169c\2\u16a2\2\u16ec\2\u16f3\2\u16fa" +
                    "\2\u1702\2\u170e\2\u1710\2\u1713\2\u1722\2\u1733\2\u1742\2\u1753\2\u1762" +
                    "\2\u176e\2\u1770\2\u1772\2\u1782\2\u17b5\2\u17d9\2\u17d9\2\u17de\2\u17de" +
                    "\2\u1822\2\u187a\2\u1882\2\u1886\2\u1889\2\u18aa\2\u18ac\2\u18ac\2\u18b2" +
                    "\2\u18f7\2\u1902\2\u1920\2\u1952\2\u196f\2\u1972\2\u1976\2\u1982\2\u19ad" +
                    "\2\u19b2\2\u19cb\2\u1a02\2\u1a18\2\u1a22\2\u1a56\2\u1aa9\2\u1aa9\2\u1b07" +
                    "\2\u1b35\2\u1b47\2\u1b4d\2\u1b85\2\u1ba2\2\u1bb0\2\u1bb1\2\u1bbc\2\u1be7" +
                    "\2\u1c02\2\u1c25\2\u1c4f\2\u1c51\2\u1c5c\2\u1c7f\2\u1c82\2\u1c8a\2\u1c92" +
                    "\2\u1cbc\2\u1cbf\2\u1cc1\2\u1ceb\2\u1cee\2\u1cf0\2\u1cf5\2\u1cf7\2\u1cf8" +
                    "\2\u1cfc\2\u1cfc\2\u1d02\2\u1dc1\2\u1e02\2\u1f17\2\u1f1a\2\u1f1f\2\u1f22" +
                    "\2\u1f47\2\u1f4a\2\u1f4f\2\u1f52\2\u1f59\2\u1f5b\2\u1f5b\2\u1f5d\2\u1f5d" +
                    "\2\u1f5f\2\u1f5f\2\u1f61\2\u1f7f\2\u1f82\2\u1fb6\2\u1fb8\2\u1fbe\2\u1fc0" +
                    "\2\u1fc0\2\u1fc4\2\u1fc6\2\u1fc8\2\u1fce\2\u1fd2\2\u1fd5\2\u1fd8\2\u1fdd" +
                    "\2\u1fe2\2\u1fee\2\u1ff4\2\u1ff6\2\u1ff8\2\u1ffe\2\u2073\2\u2073\2\u2081" +
                    "\2\u2081\2\u2092\2\u209e\2\u2104\2\u2104\2\u2109\2\u2109\2\u210c\2\u2115" +
                    "\2\u2117\2\u2117\2\u211b\2\u211f\2\u2126\2\u2126\2\u2128\2\u2128\2\u212a" +
                    "\2\u212a\2\u212c\2\u212f\2\u2131\2\u213b\2\u213e\2\u2141\2\u2147\2\u214b" +
                    "\2\u2150\2\u2150\2\u2185\2\u2186\2\u2c02\2\u2c30\2\u2c32\2\u2c60\2\u2c62" +
                    "\2\u2ce6\2\u2ced\2\u2cf0\2\u2cf4\2\u2cf5\2\u2d02\2\u2d27\2\u2d29\2\u2d29" +
                    "\2\u2d2f\2\u2d2f\2\u2d32\2\u2d69\2\u2d71\2\u2d71\2\u2d82\2\u2d98\2\u2da2" +
                    "\2\u2da8\2\u2daa\2\u2db0\2\u2db2\2\u2db8\2\u2dba\2\u2dc0\2\u2dc2\2\u2dc8" +
                    "\2\u2dca\2\u2dd0\2\u2dd2\2\u2dd8\2\u2dda\2\u2de0\2\u2e31\2\u2e31\2\u3007" +
                    "\2\u3008\2\u3033\2\u3037\2\u303d\2\u303e\2\u3043\2\u3098\2\u309f\2\u30a1" +
                    "\2\u30a3\2\u30fc\2\u30fe\2\u3101\2\u3107\2\u3131\2\u3133\2\u3190\2\u31a2" +
                    "\2\u31c1\2\u31f2\2\u3201\2\u3402\2\u4dc1\2\u4e02\2\u9ffe\2\ua002\2\ua48e" +
                    "\2\ua4d2\2\ua4ff\2\ua502\2\ua60e\2\ua612\2\ua621\2\ua62c\2\ua62d\2\ua642" +
                    "\2\ua670\2\ua681\2\ua69f\2\ua6a2\2\ua6e7\2\ua719\2\ua721\2\ua724\2\ua78a" +
                    "\2\ua78d\2\ua7c1\2\ua7c4\2\ua7cc\2\ua7f7\2\ua803\2\ua805\2\ua807\2\ua809" +
                    "\2\ua80c\2\ua80e\2\ua824\2\ua842\2\ua875\2\ua884\2\ua8b5\2\ua8f4\2\ua8f9" +
                    "\2\ua8fd\2\ua8fd\2\ua8ff\2\ua900\2\ua90c\2\ua927\2\ua932\2\ua948\2\ua962" +
                    "\2\ua97e\2\ua986\2\ua9b4\2\ua9d1\2\ua9d1\2\ua9e2\2\ua9e6\2\ua9e8\2\ua9f1" +
                    "\2\ua9fc\2\uaa00\2\uaa02\2\uaa2a\2\uaa42\2\uaa44\2\uaa46\2\uaa4d\2\uaa62" +
                    "\2\uaa78\2\uaa7c\2\uaa7c\2\uaa80\2\uaab1\2\uaab3\2\uaab3\2\uaab7\2\uaab8" +
                    "\2\uaabb\2\uaabf\2\uaac2\2\uaac2\2\uaac4\2\uaac4\2\uaadd\2\uaadf\2\uaae2" +
                    "\2\uaaec\2\uaaf4\2\uaaf6\2\uab03\2\uab08\2\uab0b\2\uab10\2\uab13\2\uab18" +
                    "\2\uab22\2\uab28\2\uab2a\2\uab30\2\uab32\2\uab5c\2\uab5e\2\uab6b\2\uab72" +
                    "\2\uabe4\2\uac02\2\ud7a5\2\ud7b2\2\ud7c8\2\ud7cd\2\ud7fd\2\uf902\2\ufa6f" +
                    "\2\ufa72\2\ufadb\2\ufb02\2\ufb08\2\ufb15\2\ufb19\2\ufb1f\2\ufb1f\2\ufb21" +
                    "\2\ufb2a\2\ufb2c\2\ufb38\2\ufb3a\2\ufb3e\2\ufb40\2\ufb40\2\ufb42\2\ufb43" +
                    "\2\ufb45\2\ufb46\2\ufb48\2\ufbb3\2\ufbd5\2\ufd3f\2\ufd52\2\ufd91\2\ufd94" +
                    "\2\ufdc9\2\ufdf2\2\ufdfd\2\ufe72\2\ufe76\2\ufe78\2\ufefe\2\uff23\2\uff3c" +
                    "\2\uff43\2\uff5c\2\uff68\2\uffc0\2\uffc4\2\uffc9\2\uffcc\2\uffd1\2\uffd4" +
                    "\2\uffd9\2\uffdc\2\uffde\2\2\3\r\3\17\3(\3*\3<\3>\3?\3A\3O\3R\3_\3\u0082" +
                    "\3\u00fc\3\u0282\3\u029e\3\u02a2\3\u02d2\3\u0302\3\u0321\3\u032f\3\u0342" +
                    "\3\u0344\3\u034b\3\u0352\3\u0377\3\u0382\3\u039f\3\u03a2\3\u03c5\3\u03ca" +
                    "\3\u03d1\3\u0402\3\u049f\3\u04b2\3\u04d5\3\u04da\3\u04fd\3\u0502\3\u0529" +
                    "\3\u0532\3\u0565\3\u0602\3\u0738\3\u0742\3\u0757\3\u0762\3\u0769\3\u0802" +
                    "\3\u0807\3\u080a\3\u080a\3\u080c\3\u0837\3\u0839\3\u083a\3\u083e\3\u083e" +
                    "\3\u0841\3\u0857\3\u0862\3\u0878\3\u0882\3\u08a0\3\u08e2\3\u08f4\3\u08f6" +
                    "\3\u08f7\3\u0902\3\u0917\3\u0922\3\u093b\3\u0982\3\u09b9\3\u09c0\3\u09c1" +
                    "\3\u0a02\3\u0a02\3\u0a12\3\u0a15\3\u0a17\3\u0a19\3\u0a1b\3\u0a37\3\u0a62" +
                    "\3\u0a7e\3\u0a82\3\u0a9e\3\u0ac2\3\u0ac9\3\u0acb\3\u0ae6\3\u0b02\3\u0b37" +
                    "\3\u0b42\3\u0b57\3\u0b62\3\u0b74\3\u0b82\3\u0b93\3\u0c02\3\u0c4a\3\u0c82" +
                    "\3\u0cb4\3\u0cc2\3\u0cf4\3\u0d02\3\u0d25\3\u0e82\3\u0eab\3\u0eb2\3\u0eb3" +
                    "\3\u0f02\3\u0f1e\3\u0f29\3\u0f29\3\u0f32\3\u0f47\3\u0fb2\3\u0fc6\3\u0fe2" +
                    "\3\u0ff8\3\u1005\3\u1039\3\u1085\3\u10b1\3\u10d2\3\u10ea\3\u1105\3\u1128" +
                    "\3\u1146\3\u1146\3\u1149\3\u1149\3\u1152\3\u1174\3\u1178\3\u1178\3\u1185" +
                    "\3\u11b4\3\u11c3\3\u11c6\3\u11dc\3\u11dc\3\u11de\3\u11de\3\u1202\3\u1213" +
                    "\3\u1215\3\u122d\3\u1282\3\u1288\3\u128a\3\u128a\3\u128c\3\u128f\3\u1291" +
                    "\3\u129f\3\u12a1\3\u12aa\3\u12b2\3\u12e0\3\u1307\3\u130e\3\u1311\3\u1312" +
                    "\3\u1315\3\u132a\3\u132c\3\u1332\3\u1334\3\u1335\3\u1337\3\u133b\3\u133f" +
                    "\3\u133f\3\u1352\3\u1352\3\u135f\3\u1363\3\u1402\3\u1436\3\u1449\3\u144c" +
                    "\3\u1461\3\u1463\3\u1482\3\u14b1\3\u14c6\3\u14c7\3\u14c9\3\u14c9\3\u1582" +
                    "\3\u15b0\3\u15da\3\u15dd\3\u1602\3\u1631\3\u1646\3\u1646\3\u1682\3\u16ac" +
                    "\3\u16ba\3\u16ba\3\u1702\3\u171c\3\u1802\3\u182d\3\u18a2\3\u18e1\3\u1901" +
                    "\3\u1908\3\u190b\3\u190b\3\u190e\3\u1915\3\u1917\3\u1918\3\u191a\3\u1931" +
                    "\3\u1941\3\u1941\3\u1943\3\u1943\3\u19a2\3\u19a9\3\u19ac\3\u19d2\3\u19e3" +
                    "\3\u19e3\3\u19e5\3\u19e5\3\u1a02\3\u1a02\3\u1a0d\3\u1a34\3\u1a3c\3\u1a3c" +
                    "\3\u1a52\3\u1a52\3\u1a5e\3\u1a8b\3\u1a9f\3\u1a9f\3\u1ac2\3\u1afa\3\u1c02" +
                    "\3\u1c0a\3\u1c0c\3\u1c30\3\u1c42\3\u1c42\3\u1c74\3\u1c91\3\u1d02\3\u1d08" +
                    "\3\u1d0a\3\u1d0b\3\u1d0d\3\u1d32\3\u1d48\3\u1d48\3\u1d62\3\u1d67\3\u1d69" +
                    "\3\u1d6a\3\u1d6c\3\u1d8b\3\u1d9a\3\u1d9a\3\u1ee2\3\u1ef4\3\u1fb2\3\u1fb2" +
                    "\3\u2002\3\u239b\3\u2482\3\u2545\3\u3002\3\u3430\3\u4402\3\u4648\3\u6802" +
                    "\3\u6a3a\3\u6a42\3\u6a60\3\u6ad2\3\u6aef\3\u6b02\3\u6b31\3\u6b42\3\u6b45" +
                    "\3\u6b65\3\u6b79\3\u6b7f\3\u6b91\3\u6e42\3\u6e81\3\u6f02\3\u6f4c\3\u6f52" +
                    "\3\u6f52\3\u6f95\3\u6fa1\3\u6fe2\3\u6fe3\3\u6fe5\3\u6fe5\3\u7002\3\u87f9" +
                    "\3\u8802\3\u8cd7\3\u8d02\3\u8d0a\3\ub002\3\ub120\3\ub152\3\ub154\3\ub166" +
                    "\3\ub169\3\ub172\3\ub2fd\3\ubc02\3\ubc6c\3\ubc72\3\ubc7e\3\ubc82\3\ubc8a" +
                    "\3\ubc92\3\ubc9b\3\ud402\3\ud456\3\ud458\3\ud49e\3\ud4a0\3\ud4a1\3\ud4a4" +
                    "\3\ud4a4\3\ud4a7\3\ud4a8\3\ud4ab\3\ud4ae\3\ud4b0\3\ud4bb\3\ud4bd\3\ud4bd" +
                    "\3\ud4bf\3\ud4c5\3\ud4c7\3\ud507\3\ud509\3\ud50c\3\ud50f\3\ud516\3\ud518" +
                    "\3\ud51e\3\ud520\3\ud53b\3\ud53d\3\ud540\3\ud542\3\ud546\3\ud548\3\ud548" +
                    "\3\ud54c\3\ud552\3\ud554\3\ud6a7\3\ud6aa\3\ud6c2\3\ud6c4\3\ud6dc\3\ud6de" +
                    "\3\ud6fc\3\ud6fe\3\ud716\3\ud718\3\ud736\3\ud738\3\ud750\3\ud752\3\ud770" +
                    "\3\ud772\3\ud78a\3\ud78c\3\ud7aa\3\ud7ac\3\ud7c4\3\ud7c6\3\ud7cd\3\ue102" +
                    "\3\ue12e\3\ue139\3\ue13f\3\ue150\3\ue150\3\ue2c2\3\ue2ed\3\ue802\3\ue8c6" +
                    "\3\ue902\3\ue945\3\ue94d\3\ue94d\3\uee02\3\uee05\3\uee07\3\uee21\3\uee23" +
                    "\3\uee24\3\uee26\3\uee26\3\uee29\3\uee29\3\uee2b\3\uee34\3\uee36\3\uee39" +
                    "\3\uee3b\3\uee3b\3\uee3d\3\uee3d\3\uee44\3\uee44\3\uee49\3\uee49\3\uee4b" +
                    "\3\uee4b\3\uee4d\3\uee4d\3\uee4f\3\uee51\3\uee53\3\uee54\3\uee56\3\uee56" +
                    "\3\uee59\3\uee59\3\uee5b\3\uee5b\3\uee5d\3\uee5d\3\uee5f\3\uee5f\3\uee61" +
                    "\3\uee61\3\uee63\3\uee64\3\uee66\3\uee66\3\uee69\3\uee6c\3\uee6e\3\uee74" +
                    "\3\uee76\3\uee79\3\uee7b\3\uee7e\3\uee80\3\uee80\3\uee82\3\uee8b\3\uee8d" +
                    "\3\uee9d\3\ueea3\3\ueea5\3\ueea7\3\ueeab\3\ueead\3\ueebd\3\2\4\ua6df\4" +
                    "\ua702\4\ub736\4\ub742\4\ub81f\4\ub822\4\ucea3\4\uceb2\4\uebe2\4\uf802" +
                    "\4\ufa1f\4\2\5\u134c\5\u01a1\2\62\2;\2a\2a\2\u00b4\2\u00b5\2\u00bb\2\u00bb" +
                    "\2\u00be\2\u00c0\2\u0302\2\u0371\2\u0485\2\u048b\2\u0593\2\u05bf\2\u05c1" +
                    "\2\u05c1\2\u05c3\2\u05c4\2\u05c6\2\u05c7\2\u05c9\2\u05c9\2\u0612\2\u061c" +
                    "\2\u064d\2\u066b\2\u0672\2\u0672\2\u06d8\2\u06de\2\u06e1\2\u06e6\2\u06e9" +
                    "\2\u06ea\2\u06ec\2\u06ef\2\u06f2\2\u06fb\2\u0713\2\u0713\2\u0732\2\u074c" +
                    "\2\u07a8\2\u07b2\2\u07c2\2\u07cb\2\u07ed\2\u07f5\2\u07ff\2\u07ff\2\u0818" +
                    "\2\u081b\2\u081d\2\u0825\2\u0827\2\u0829\2\u082b\2\u082f\2\u085b\2\u085d" +
                    "\2\u08d5\2\u08e3\2\u08e5\2\u0905\2\u093c\2\u093e\2\u0940\2\u0951\2\u0953" +
                    "\2\u0959\2\u0964\2\u0965\2\u0968\2\u0971\2\u0983\2\u0985\2\u09be\2\u09be" +
                    "\2\u09c0\2\u09c6\2\u09c9\2\u09ca\2\u09cd\2\u09cf\2\u09d9\2\u09d9\2\u09e4" +
                    "\2\u09e5\2\u09e8\2\u09f1\2\u09f6\2\u09fb\2\u0a00\2\u0a00\2\u0a03\2\u0a05" +
                    "\2\u0a3e\2\u0a3e\2\u0a40\2\u0a44\2\u0a49\2\u0a4a\2\u0a4d\2\u0a4f\2\u0a53" +
                    "\2\u0a53\2\u0a68\2\u0a73\2\u0a77\2\u0a77\2\u0a83\2\u0a85\2\u0abe\2\u0abe" +
                    "\2\u0ac0\2\u0ac7\2\u0ac9\2\u0acb\2\u0acd\2\u0acf\2\u0ae4\2\u0ae5\2\u0ae8" +
                    "\2\u0af1\2\u0afc\2\u0b01\2\u0b03\2\u0b05\2\u0b3e\2\u0b3e\2\u0b40\2\u0b46" +
                    "\2\u0b49\2\u0b4a\2\u0b4d\2\u0b4f\2\u0b57\2\u0b59\2\u0b64\2\u0b65\2\u0b68" +
                    "\2\u0b71\2\u0b74\2\u0b79\2\u0b84\2\u0b84\2\u0bc0\2\u0bc4\2\u0bc8\2\u0bca" +
                    "\2\u0bcc\2\u0bcf\2\u0bd9\2\u0bd9\2\u0be8\2\u0bf4\2\u0c02\2\u0c06\2\u0c40" +
                    "\2\u0c46\2\u0c48\2\u0c4a\2\u0c4c\2\u0c4f\2\u0c57\2\u0c58\2\u0c64\2\u0c65" +
                    "\2\u0c68\2\u0c71\2\u0c7a\2\u0c80\2\u0c83\2\u0c85\2\u0cbe\2\u0cbe\2\u0cc0" +
                    "\2\u0cc6\2\u0cc8\2\u0cca\2\u0ccc\2\u0ccf\2\u0cd7\2\u0cd8\2\u0ce4\2\u0ce5" +
                    "\2\u0ce8\2\u0cf1\2\u0d02\2\u0d05\2\u0d3d\2\u0d3e\2\u0d40\2\u0d46\2\u0d48" +
                    "\2\u0d4a\2\u0d4c\2\u0d4f\2\u0d59\2\u0d60\2\u0d64\2\u0d65\2\u0d68\2\u0d7a" +
                    "\2\u0d83\2\u0d85\2\u0dcc\2\u0dcc\2\u0dd1\2\u0dd6\2\u0dd8\2\u0dd8\2\u0dda" +
                    "\2\u0de1\2\u0de8\2\u0df1\2\u0df4\2\u0df5\2\u0e33\2\u0e33\2\u0e36\2\u0e3c" +
                    "\2\u0e49\2\u0e50\2\u0e52\2\u0e5b\2\u0eb3\2\u0eb3\2\u0eb6\2\u0ebe\2\u0eca" +
                    "\2\u0ecf\2\u0ed2\2\u0edb\2\u0f1a\2\u0f1b\2\u0f22\2\u0f35\2\u0f37\2\u0f37" +
                    "\2\u0f39\2\u0f39\2\u0f3b\2\u0f3b\2\u0f40\2\u0f41\2\u0f73\2\u0f86\2\u0f88" +
                    "\2\u0f89\2\u0f8f\2\u0f99\2\u0f9b\2\u0fbe\2\u0fc8\2\u0fc8\2\u102d\2\u1040" +
                    "\2\u1042\2\u104b\2\u1058\2\u105b\2\u1060\2\u1062\2\u1064\2\u1066\2\u1069" +
                    "\2\u106f\2\u1073\2\u1076\2\u1084\2\u108f\2\u1091\2\u109f\2\u135f\2\u1361" +
                    "\2\u136b\2\u137e\2\u16f0\2\u16f2\2\u1714\2\u1716\2\u1734\2\u1736\2\u1754" +
                    "\2\u1755\2\u1774\2\u1775\2\u17b6\2\u17d5\2\u17df\2\u17df\2\u17e2\2\u17eb" +
                    "\2\u17f2\2\u17fb\2\u180d\2\u180f\2\u1812\2\u181b\2\u1887\2\u1888\2\u18ab" +
                    "\2\u18ab\2\u1922\2\u192d\2\u1932\2\u193d\2\u1948\2\u1951\2\u19d2\2\u19dc" +
                    "\2\u1a19\2\u1a1d\2\u1a57\2\u1a60\2\u1a62\2\u1a7e\2\u1a81\2\u1a8b\2\u1a92" +
                    "\2\u1a9b\2\u1ab2\2\u1ac2\2\u1b02\2\u1b06\2\u1b36\2\u1b46\2\u1b52\2\u1b5b" +
                    "\2\u1b6d\2\u1b75\2\u1b82\2\u1b84\2\u1ba3\2\u1baf\2\u1bb2\2\u1bbb\2\u1be8" +
                    "\2\u1bf5\2\u1c26\2\u1c39\2\u1c42\2\u1c4b\2\u1c52\2\u1c5b\2\u1cd2\2\u1cd4" +
                    "\2\u1cd6\2\u1cea\2\u1cef\2\u1cef\2\u1cf6\2\u1cf6\2\u1cf9\2\u1cfb\2\u1dc2" +
                    "\2\u1dfb\2\u1dfd\2\u1e01\2\u200e\2\u200f\2\u2041\2\u2042\2\u2056\2\u2056" +
                    "\2\u2072\2\u2072\2\u2076\2\u207b\2\u2082\2\u208b\2\u20d2\2\u20f2\2\u2152" +
                    "\2\u2184\2\u2187\2\u218b\2\u2462\2\u249d\2\u24ec\2\u2501\2\u2778\2\u2795" +
                    "\2\u2cf1\2\u2cf3\2\u2cff\2\u2cff\2\u2d81\2\u2d81\2\u2de2\2\u2e01\2\u3009" +
                    "\2\u3009\2\u3023\2\u3031\2\u303a\2\u303c\2\u309b\2\u309c\2\u3194\2\u3197" +
                    "\2\u3222\2\u322b\2\u324a\2\u3251\2\u3253\2\u3261\2\u3282\2\u328b\2\u32b3" +
                    "\2\u32c1\2\ua622\2\ua62b\2\ua671\2\ua674\2\ua676\2\ua67f\2\ua6a0\2\ua6a1" +
                    "\2\ua6e8\2\ua6f3\2\ua804\2\ua804\2\ua808\2\ua808\2\ua80d\2\ua80d\2\ua825" +
                    "\2\ua829\2\ua82e\2\ua82e\2\ua832\2\ua837\2\ua882\2\ua883\2\ua8b6\2\ua8c7" +
                    "\2\ua8d2\2\ua8db\2\ua8e2\2\ua8f3\2\ua901\2\ua90b\2\ua928\2\ua92f\2\ua949" +
                    "\2\ua955\2\ua982\2\ua985\2\ua9b5\2\ua9c2\2\ua9d2\2\ua9db\2\ua9e7\2\ua9e7" +
                    "\2\ua9f2\2\ua9fb\2\uaa2b\2\uaa38\2\uaa45\2\uaa45\2\uaa4e\2\uaa4f\2\uaa52" +
                    "\2\uaa5b\2\uaa7d\2\uaa7f\2\uaab2\2\uaab2\2\uaab4\2\uaab6\2\uaab9\2\uaaba" +
                    "\2\uaac0\2\uaac1\2\uaac3\2\uaac3\2\uaaed\2\uaaf1\2\uaaf7\2\uaaf8\2\uabe5" +
                    "\2\uabec\2\uabee\2\uabef\2\uabf2\2\uabfb\2\ufb20\2\ufb20\2\ufe02\2\ufe11" +
                    "\2\ufe22\2\ufe31\2\ufe35\2\ufe36\2\ufe4f\2\ufe51\2\uff12\2\uff1b\2\uff41" +
                    "\2\uff41\2\u0109\3\u0135\3\u0142\3\u017a\3\u018c\3\u018d\3\u01ff\3\u01ff" +
                    "\3\u02e2\3\u02fd\3\u0322\3\u0325\3\u0343\3\u0343\3\u034c\3\u034c\3\u0378" +
                    "\3\u037c\3\u03d3\3\u03d7\3\u04a2\3\u04ab\3\u085a\3\u0861\3\u087b\3\u0881" +
                    "\3\u08a9\3\u08b1\3\u08fd\3\u0901\3\u0918\3\u091d\3\u09be\3\u09bf\3\u09c2" +
                    "\3\u09d1\3\u09d4\3\u0a01\3\u0a03\3\u0a05\3\u0a07\3\u0a08\3\u0a0e\3\u0a11" +
                    "\3\u0a3a\3\u0a3c\3\u0a41\3\u0a4a\3\u0a7f\3\u0a80\3\u0a9f\3\u0aa1\3\u0ae7" +
                    "\3\u0ae8\3\u0aed\3\u0af1\3\u0b5a\3\u0b61\3\u0b7a\3\u0b81\3\u0bab\3\u0bb1" +
                    "\3\u0cfc\3\u0d01\3\u0d26\3\u0d29\3\u0d32\3\u0d3b\3\u0e62\3\u0e80\3\u0ead" +
                    "\3\u0eae\3\u0f1f\3\u0f28\3\u0f48\3\u0f56\3\u0fc7\3\u0fcd\3\u1002\3\u1004" +
                    "\3\u103a\3\u1048\3\u1054\3\u1071\3\u1081\3\u1084\3\u10b2\3\u10bc\3\u10f2" +
                    "\3\u10fb\3\u1102\3\u1104\3\u1129\3\u1136\3\u1138\3\u1141\3\u1147\3\u1148" +
                    "\3\u1175\3\u1175\3\u1182\3\u1184\3\u11b5\3\u11c2\3\u11cb\3\u11ce\3\u11d0" +
                    "\3\u11db\3\u11e3\3\u11f6\3\u122e\3\u1239\3\u1240\3\u1240\3\u12e1\3\u12ec" +
                    "\3\u12f2\3\u12fb\3\u1302\3\u1305\3\u133d\3\u133e\3\u1340\3\u1346\3\u1349" +
                    "\3\u134a\3\u134d\3\u134f\3\u1359\3\u1359\3\u1364\3\u1365\3\u1368\3\u136e" +
                    "\3\u1372\3\u1376\3\u1437\3\u1448\3\u1452\3\u145b\3\u1460\3\u1460\3\u14b2" +
                    "\3\u14c5\3\u14d2\3\u14db\3\u15b1\3\u15b7\3\u15ba\3\u15c2\3\u15de\3\u15df" +
                    "\3\u1632\3\u1642\3\u1652\3\u165b\3\u16ad\3\u16b9\3\u16c2\3\u16cb\3\u171f" +
                    "\3\u172d\3\u1732\3\u173d\3\u182e\3\u183c\3\u18e2\3\u18f4\3\u1932\3\u1937" +
                    "\3\u1939\3\u193a\3\u193d\3\u1940\3\u1942\3\u1942\3\u1944\3\u1945\3\u1952" +
                    "\3\u195b\3\u19d3\3\u19d9\3\u19dc\3\u19e2\3\u19e6\3\u19e6\3\u1a03\3\u1a0c" +
                    "\3\u1a35\3\u1a3b\3\u1a3d\3\u1a40\3\u1a49\3\u1a49\3\u1a53\3\u1a5d\3\u1a8c" +
                    "\3\u1a9b\3\u1c31\3\u1c38\3\u1c3a\3\u1c41\3\u1c52\3\u1c6e\3\u1c94\3\u1ca9" +
                    "\3\u1cab\3\u1cb8\3\u1d33\3\u1d38\3\u1d3c\3\u1d3c\3\u1d3e\3\u1d3f\3\u1d41" +
                    "\3\u1d47\3\u1d49\3\u1d49\3\u1d52\3\u1d5b\3\u1d8c\3\u1d90\3\u1d92\3\u1d93" +
                    "\3\u1d95\3\u1d99\3\u1da2\3\u1dab\3\u1ef5\3\u1ef8\3\u1fc2\3\u1fd6\3\u2402" +
                    "\3\u2470\3\u6a62\3\u6a6b\3\u6af2\3\u6af6\3\u6b32\3\u6b38\3\u6b52\3\u6b5b" +
                    "\3\u6b5d\3\u6b63\3\u6e82\3\u6e98\3\u6f51\3\u6f51\3\u6f53\3\u6f89\3\u6f91" +
                    "\3\u6f94\3\u6fe6\3\u6fe6\3\u6ff2\3\u6ff3\3\ubc9f\3\ubca0\3\ud167\3\ud16b" +
                    "\3\ud16f\3\ud174\3\ud17d\3\ud184\3\ud187\3\ud18d\3\ud1ac\3\ud1af\3\ud244" +
                    "\3\ud246\3\ud2e2\3\ud2f5\3\ud362\3\ud37a\3\ud7d0\3\ud801\3\uda02\3\uda38" +
                    "\3\uda3d\3\uda6e\3\uda77\3\uda77\3\uda86\3\uda86\3\uda9d\3\udaa1\3\udaa3" +
                    "\3\udab1\3\ue002\3\ue008\3\ue00a\3\ue01a\3\ue01d\3\ue023\3\ue025\3\ue026" +
                    "\3\ue028\3\ue02c\3\ue132\3\ue138\3\ue142\3\ue14b\3\ue2ee\3\ue2fb\3\ue8c9" +
                    "\3\ue8d8\3\ue946\3\ue94c\3\ue952\3\ue95b\3\uec73\3\uecad\3\uecaf\3\uecb1" +
                    "\3\uecb3\3\uecb6\3\ued03\3\ued2f\3\ued31\3\ued3f\3\uf102\3\uf10e\3\ufbf2" +
                    "\3\ufbfb\3\u0102\20\u01f1\20\u0115\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2" +
                    "\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3" +
                    "\2\2\2\2\25\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2)\3\2\2\2\2" +
                    "\63\3\2\2\2\2\65\3\2\2\2\3\67\3\2\2\2\59\3\2\2\2\7;\3\2\2\2\t=\3\2\2\2" +
                    "\13?\3\2\2\2\rA\3\2\2\2\17C\3\2\2\2\21R\3\2\2\2\23m\3\2\2\2\25\177\3\2" +
                    "\2\2\27\u0083\3\2\2\2\31\u0087\3\2\2\2\33\u0089\3\2\2\2\35\u00b2\3\2\2" +
                    "\2\37\u00bf\3\2\2\2!\u00c1\3\2\2\2#\u00c3\3\2\2\2%\u00cd\3\2\2\2\'\u00cf" +
                    "\3\2\2\2)\u00d9\3\2\2\2+\u00e3\3\2\2\2-\u00e7\3\2\2\2/\u00e9\3\2\2\2\61" +
                    "\u00f2\3\2\2\2\63\u00f5\3\2\2\2\65\u00fb\3\2\2\2\678\7}\2\28\4\3\2\2\2" +
                    "9:\7.\2\2:\6\3\2\2\2;<\7\177\2\2<\b\3\2\2\2=>\7<\2\2>\n\3\2\2\2?@\7]\2" +
                    "\2@\f\3\2\2\2AB\7_\2\2B\16\3\2\2\2CD\7\61\2\2DE\7\61\2\2EI\3\2\2\2FH\13" +
                    "\2\2\2GF\3\2\2\2HK\3\2\2\2IJ\3\2\2\2IG\3\2\2\2JN\3\2\2\2KI\3\2\2\2LO\5" +
                    "\61\31\2MO\7\2\2\3NL\3\2\2\2NM\3\2\2\2OP\3\2\2\2PQ\b\b\2\2Q\20\3\2\2\2" +
                    "RS\7\61\2\2ST\7,\2\2TX\3\2\2\2UW\13\2\2\2VU\3\2\2\2WZ\3\2\2\2XY\3\2\2" +
                    "\2XV\3\2\2\2Y[\3\2\2\2ZX\3\2\2\2[\\\7,\2\2\\]\7\61\2\2]^\3\2\2\2^_\b\t" +
                    "\2\2_\22\3\2\2\2`a\7v\2\2ab\7t\2\2bc\7w\2\2cn\7g\2\2de\7h\2\2ef\7c\2\2" +
                    "fg\7n\2\2gh\7u\2\2hn\7g\2\2ij\7p\2\2jk\7w\2\2kl\7n\2\2ln\7n\2\2m`\3\2" +
                    "\2\2md\3\2\2\2mi\3\2\2\2n\24\3\2\2\2os\7$\2\2pr\5\27\f\2qp\3\2\2\2ru\3" +
                    "\2\2\2sq\3\2\2\2st\3\2\2\2tv\3\2\2\2us\3\2\2\2v\u0080\7$\2\2w{\7)\2\2" +
                    "xz\5\31\r\2yx\3\2\2\2z}\3\2\2\2{y\3\2\2\2{|\3\2\2\2|~\3\2\2\2}{\3\2\2" +
                    "\2~\u0080\7)\2\2\177o\3\2\2\2\177w\3\2\2\2\u0080\26\3\2\2\2\u0081\u0084" +
                    "\n\2\2\2\u0082\u0084\5\33\16\2\u0083\u0081\3\2\2\2\u0083\u0082\3\2\2\2" +
                    "\u0084\30\3\2\2\2\u0085\u0088\n\3\2\2\u0086\u0088\5\33\16\2\u0087\u0085" +
                    "\3\2\2\2\u0087\u0086\3\2\2\2\u0088\32\3\2\2\2\u0089\u0093\7^\2\2\u008a" +
                    "\u0094\5\61\31\2\u008b\u0094\5/\30\2\u008c\u0094\t\4\2\2\u008d\u0094\n" +
                    "\5\2\2\u008e\u0094\7\62\2\2\u008f\u0090\7z\2\2\u0090\u0091\5#\22\2\u0091" +
                    "\u0092\5#\22\2\u0092\u0094\3\2\2\2\u0093\u008a\3\2\2\2\u0093\u008b\3\2" +
                    "\2\2\u0093\u008c\3\2\2\2\u0093\u008d\3\2\2\2\u0093\u008e\3\2\2\2\u0093" +
                    "\u008f\3\2\2\2\u0094\34\3\2\2\2\u0095\u009d\5%\23\2\u0096\u009a\7\60\2" +
                    "\2\u0097\u0099\t\6\2\2\u0098\u0097\3\2\2\2\u0099\u009c\3\2\2\2\u009a\u0098" +
                    "\3\2\2\2\u009a\u009b\3\2\2\2\u009b\u009e\3\2\2\2\u009c\u009a\3\2\2\2\u009d" +
                    "\u0096\3\2\2\2\u009d\u009e\3\2\2\2\u009e\u00a0\3\2\2\2\u009f\u00a1\5\'" +
                    "\24\2\u00a0\u009f\3\2\2\2\u00a0\u00a1\3\2\2\2\u00a1\u00b3\3\2\2\2\u00a2" +
                    "\u00a4\7\60\2\2\u00a3\u00a5\t\6\2\2\u00a4\u00a3\3\2\2\2\u00a5\u00a6\3" +
                    "\2\2\2\u00a6\u00a4\3\2\2\2\u00a6\u00a7\3\2\2\2\u00a7\u00a9\3\2\2\2\u00a8" +
                    "\u00aa\5\'\24\2\u00a9\u00a8\3\2\2\2\u00a9\u00aa\3\2\2\2\u00aa\u00b3\3" +
                    "\2\2\2\u00ab\u00ac\7\62\2\2\u00ac\u00ae\t\7\2\2\u00ad\u00af\5#\22\2\u00ae" +
                    "\u00ad\3\2\2\2\u00af\u00b0\3\2\2\2\u00b0\u00ae\3\2\2\2\u00b0\u00b1\3\2" +
                    "\2\2\u00b1\u00b3\3\2\2\2\u00b2\u0095\3\2\2\2\u00b2\u00a2\3\2\2\2\u00b2" +
                    "\u00ab\3\2\2\2\u00b3\36\3\2\2\2\u00b4\u00b5\7K\2\2\u00b5\u00b6\7p\2\2" +
                    "\u00b6\u00b7\7h\2\2\u00b7\u00b8\7k\2\2\u00b8\u00b9\7p\2\2\u00b9\u00ba" +
                    "\7k\2\2\u00ba\u00bb\7v\2\2\u00bb\u00c0\7{\2\2\u00bc\u00bd\7P\2\2\u00bd" +
                    "\u00be\7c\2\2\u00be\u00c0\7P\2\2\u00bf\u00b4\3\2\2\2\u00bf\u00bc\3\2\2" +
                    "\2\u00c0 \3\2\2\2\u00c1\u00c2\t\b\2\2\u00c2\"\3\2\2\2\u00c3\u00c4\t\t" +
                    "\2\2\u00c4$\3\2\2\2\u00c5\u00ce\7\62\2\2\u00c6\u00ca\t\n\2\2\u00c7\u00c9" +
                    "\t\6\2\2\u00c8\u00c7\3\2\2\2\u00c9\u00cc\3\2\2\2\u00ca\u00c8\3\2\2\2\u00ca" +
                    "\u00cb\3\2\2\2\u00cb\u00ce\3\2\2\2\u00cc\u00ca\3\2\2\2\u00cd\u00c5\3\2" +
                    "\2\2\u00cd\u00c6\3\2\2\2\u00ce&\3\2\2\2\u00cf\u00d1\t\13\2\2\u00d0\u00d2" +
                    "\5!\21\2\u00d1\u00d0\3\2\2\2\u00d1\u00d2\3\2\2\2\u00d2\u00d6\3\2\2\2\u00d3" +
                    "\u00d5\t\6\2\2\u00d4\u00d3\3\2\2\2\u00d5\u00d8\3\2\2\2\u00d6\u00d4\3\2" +
                    "\2\2\u00d6\u00d7\3\2\2\2\u00d7(\3\2\2\2\u00d8\u00d6\3\2\2\2\u00d9\u00dd" +
                    "\5+\26\2\u00da\u00dc\5-\27\2\u00db\u00da\3\2\2\2\u00dc\u00df\3\2\2\2\u00dd" +
                    "\u00db\3\2\2\2\u00dd\u00de\3\2\2\2\u00de*\3\2\2\2\u00df\u00dd\3\2\2\2" +
                    "\u00e0\u00e4\t\16\2\2\u00e1\u00e2\7^\2\2\u00e2\u00e4\5/\30\2\u00e3\u00e0" +
                    "\3\2\2\2\u00e3\u00e1\3\2\2\2\u00e4,\3\2\2\2\u00e5\u00e8\5+\26\2\u00e6" +
                    "\u00e8\t\17\2\2\u00e7\u00e5\3\2\2\2\u00e7\u00e6\3\2\2\2\u00e8.\3\2\2\2" +
                    "\u00e9\u00ea\7w\2\2\u00ea\u00eb\5#\22\2\u00eb\u00ec\5#\22\2\u00ec\u00ed" +
                    "\5#\22\2\u00ed\u00ee\5#\22\2\u00ee\60\3\2\2\2\u00ef\u00f0\7\17\2\2\u00f0" +
                    "\u00f3\7\f\2\2\u00f1\u00f3\t\f\2\2\u00f2\u00ef\3\2\2\2\u00f2\u00f1\3\2" +
                    "\2\2\u00f3\62\3\2\2\2\u00f4\u00f6\t\r\2\2\u00f5\u00f4\3\2\2\2\u00f6\u00f7" +
                    "\3\2\2\2\u00f7\u00f5\3\2\2\2\u00f7\u00f8\3\2\2\2\u00f8\u00f9\3\2\2\2\u00f9" +
                    "\u00fa\b\32\2\2\u00fa\64\3\2\2\2\u00fb\u00fc\7\uff01\2\2\u00fc\u00fd\3" +
                    "\2\2\2\u00fd\u00fe\b\33\2\2\u00fe\66\3\2\2\2\36\2INXms{\177\u0083\u0087" +
                    "\u0093\u009a\u009d\u00a0\u00a6\u00a9\u00b0\u00b2\u00bf\u00ca\u00cd\u00d1" +
                    "\u00d6\u00dd\u00e3\u00e7\u00f2\u00f7\3\b\2\2";
    public static final ATN _ATN =
            new ATNDeserializer().deserialize(_serializedATN.toCharArray());

    static {
        _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
        for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
            _decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
        }
    }
}