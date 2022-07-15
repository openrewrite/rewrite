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
// Generated from /Users/jon/Projects/github/openrewrite/rewrite/rewrite-java/src/main/antlr/AnnotationSignatureLexer.g4 by ANTLR 4.9.3
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
    static {
        RuntimeMetaData.checkVersion("4.9.3", RuntimeMetaData.VERSION);
    }

    protected static final DFA[] _decisionToDFA;
    protected static final PredictionContextCache _sharedContextCache =
            new PredictionContextCache();
    public static final int
            IntegerLiteral = 1, FloatingPointLiteral = 2, BooleanLiteral = 3, CharacterLiteral = 4,
            StringLiteral = 5, LPAREN = 6, RPAREN = 7, LBRACK = 8, RBRACK = 9, COMMA = 10, DOT = 11,
            ASSIGN = 12, COLON = 13, ADD = 14, SUB = 15, AND = 16, OR = 17, AT = 18, ELLIPSIS = 19,
            DOTDOT = 20, SPACE = 21, Identifier = 22;
    public static String[] channelNames = {
            "DEFAULT_TOKEN_CHANNEL", "HIDDEN"
    };

    public static String[] modeNames = {
            "DEFAULT_MODE"
    };

    private static String[] makeRuleNames() {
        return new String[]{
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
                "OctalEscape", "UnicodeEscape", "ZeroToThree", "LPAREN", "RPAREN", "LBRACK",
                "RBRACK", "COMMA", "DOT", "ASSIGN", "COLON", "ADD", "SUB", "AND", "OR",
                "AT", "ELLIPSIS", "DOTDOT", "SPACE", "Identifier", "JavaLetter", "JavaLetterOrDigit"
        };
    }
    public static final String[] ruleNames = makeRuleNames();

    private static String[] makeLiteralNames() {
        return new String[]{
                null, null, null, null, null, null, "'('", "')'", "'['", "']'", "','",
                "'.'", "'='", "':'", "'+'", "'-'", "'&&'", "'||'", "'@'", "'...'", "'..'",
                "' '"
        };
    }
    private static final String[] _LITERAL_NAMES = makeLiteralNames();

    private static String[] makeSymbolicNames() {
        return new String[]{
                null, "IntegerLiteral", "FloatingPointLiteral", "BooleanLiteral", "CharacterLiteral",
                "StringLiteral", "LPAREN", "RPAREN", "LBRACK", "RBRACK", "COMMA", "DOT",
                "ASSIGN", "COLON", "ADD", "SUB", "AND", "OR", "AT", "ELLIPSIS", "DOTDOT",
                "SPACE", "Identifier"
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
        _interp = new LexerATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
    }

    @Override
    public String getGrammarFileName() {
        return "AnnotationSignatureLexer.g4";
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

    @Override
    public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
        switch (ruleIndex) {
            case 62:
                return JavaLetter_sempred((RuleContext) _localctx, predIndex);
            case 63:
                return JavaLetterOrDigit_sempred((RuleContext) _localctx, predIndex);
        }
        return true;
    }

    private boolean JavaLetter_sempred(RuleContext _localctx, int predIndex) {
        switch (predIndex) {
            case 0:
                return Character.isJavaIdentifierStart(_input.LA(-1));
            case 1:
                return Character.isJavaIdentifierStart(Character.toCodePoint((char) _input.LA(-2), (char) _input.LA(-1)));
        }
        return true;
    }

    private boolean JavaLetterOrDigit_sempred(RuleContext _localctx, int predIndex) {
        switch (predIndex) {
            case 2:
                return Character.isJavaIdentifierPart(_input.LA(-1));
            case 3:
                return Character.isJavaIdentifierPart(Character.toCodePoint((char) _input.LA(-2), (char) _input.LA(-1)));
        }
        return true;
    }

    public static final String _serializedATN =
            "\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\30\u01c5\b\1\4\2" +
                    "\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4" +
                    "\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22" +
                    "\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31" +
                    "\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t" +
                    " \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t" +
                    "+\4,\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64" +
                    "\t\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\4<\t<\4=\t" +
                    "=\4>\t>\4?\t?\4@\t@\4A\tA\3\2\3\2\3\2\3\2\5\2\u0088\n\2\3\3\3\3\5\3\u008c" +
                    "\n\3\3\4\3\4\5\4\u0090\n\4\3\5\3\5\5\5\u0094\n\5\3\6\3\6\5\6\u0098\n\6" +
                    "\3\7\3\7\3\b\3\b\3\b\5\b\u009f\n\b\3\b\3\b\3\b\5\b\u00a4\n\b\5\b\u00a6" +
                    "\n\b\3\t\3\t\7\t\u00aa\n\t\f\t\16\t\u00ad\13\t\3\t\5\t\u00b0\n\t\3\n\3" +
                    "\n\5\n\u00b4\n\n\3\13\3\13\3\f\3\f\5\f\u00ba\n\f\3\r\6\r\u00bd\n\r\r\r" +
                    "\16\r\u00be\3\16\3\16\3\16\3\16\3\17\3\17\7\17\u00c7\n\17\f\17\16\17\u00ca" +
                    "\13\17\3\17\5\17\u00cd\n\17\3\20\3\20\3\21\3\21\5\21\u00d3\n\21\3\22\3" +
                    "\22\5\22\u00d7\n\22\3\22\3\22\3\23\3\23\7\23\u00dd\n\23\f\23\16\23\u00e0" +
                    "\13\23\3\23\5\23\u00e3\n\23\3\24\3\24\3\25\3\25\5\25\u00e9\n\25\3\26\3" +
                    "\26\3\26\3\26\3\27\3\27\7\27\u00f1\n\27\f\27\16\27\u00f4\13\27\3\27\5" +
                    "\27\u00f7\n\27\3\30\3\30\3\31\3\31\5\31\u00fd\n\31\3\32\3\32\5\32\u0101" +
                    "\n\32\3\33\3\33\3\33\5\33\u0106\n\33\3\33\5\33\u0109\n\33\3\33\5\33\u010c" +
                    "\n\33\3\33\3\33\3\33\5\33\u0111\n\33\3\33\5\33\u0114\n\33\3\33\3\33\3" +
                    "\33\5\33\u0119\n\33\3\33\3\33\3\33\5\33\u011e\n\33\3\34\3\34\3\34\3\35" +
                    "\3\35\3\36\5\36\u0126\n\36\3\36\3\36\3\37\3\37\3 \3 \3!\3!\3!\5!\u0131" +
                    "\n!\3\"\3\"\5\"\u0135\n\"\3\"\3\"\3\"\5\"\u013a\n\"\3\"\3\"\5\"\u013e" +
                    "\n\"\3#\3#\3#\3$\3$\3%\3%\3%\3%\3%\3%\3%\3%\3%\5%\u014e\n%\3&\3&\3&\3" +
                    "&\3&\3&\3&\3&\5&\u0158\n&\3\'\3\'\3(\3(\5(\u015e\n(\3(\3(\3)\6)\u0163" +
                    "\n)\r)\16)\u0164\3*\3*\5*\u0169\n*\3+\3+\3+\3+\5+\u016f\n+\3,\3,\3,\3" +
                    ",\3,\3,\3,\3,\3,\3,\3,\5,\u017c\n,\3-\3-\3-\3-\3-\3-\3-\3.\3.\3/\3/\3" +
                    "\60\3\60\3\61\3\61\3\62\3\62\3\63\3\63\3\64\3\64\3\65\3\65\3\66\3\66\3" +
                    "\67\3\67\38\38\39\39\39\3:\3:\3:\3;\3;\3<\3<\3<\3<\3=\3=\3=\3>\3>\3?\3" +
                    "?\3?\7?\u01af\n?\f?\16?\u01b2\13?\5?\u01b4\n?\3@\3@\3@\3@\3@\3@\5@\u01bc" +
                    "\n@\3A\3A\3A\3A\3A\3A\5A\u01c4\nA\2\2B\3\3\5\2\7\2\t\2\13\2\r\2\17\2\21" +
                    "\2\23\2\25\2\27\2\31\2\33\2\35\2\37\2!\2#\2%\2\'\2)\2+\2-\2/\2\61\2\63" +
                    "\4\65\2\67\29\2;\2=\2?\2A\2C\2E\2G\2I\5K\6M\2O\7Q\2S\2U\2W\2Y\2[\2]\b" +
                    "_\ta\nc\13e\fg\ri\16k\17m\20o\21q\22s\23u\24w\25y\26{\27}\30\177\2\u0081" +
                    "\2\3\2\26\4\2NNnn\3\2\63;\4\2ZZzz\5\2\62;CHch\3\2\629\4\2DDdd\3\2\62\63" +
                    "\4\2GGgg\4\2--//\6\2FFHHffhh\4\2RRrr\4\2))^^\4\2$$^^\n\2$$))^^ddhhppt" +
                    "tvv\3\2\62\65\6\2&&C\\aac|\4\2\2\u0081\ud802\udc01\3\2\ud802\udc01\3\2" +
                    "\udc02\ue001\b\2&&,,\62;C\\aac|\2\u01d1\2\3\3\2\2\2\2\63\3\2\2\2\2I\3" +
                    "\2\2\2\2K\3\2\2\2\2O\3\2\2\2\2]\3\2\2\2\2_\3\2\2\2\2a\3\2\2\2\2c\3\2\2" +
                    "\2\2e\3\2\2\2\2g\3\2\2\2\2i\3\2\2\2\2k\3\2\2\2\2m\3\2\2\2\2o\3\2\2\2\2" +
                    "q\3\2\2\2\2s\3\2\2\2\2u\3\2\2\2\2w\3\2\2\2\2y\3\2\2\2\2{\3\2\2\2\2}\3" +
                    "\2\2\2\3\u0087\3\2\2\2\5\u0089\3\2\2\2\7\u008d\3\2\2\2\t\u0091\3\2\2\2" +
                    "\13\u0095\3\2\2\2\r\u0099\3\2\2\2\17\u00a5\3\2\2\2\21\u00a7\3\2\2\2\23" +
                    "\u00b3\3\2\2\2\25\u00b5\3\2\2\2\27\u00b9\3\2\2\2\31\u00bc\3\2\2\2\33\u00c0" +
                    "\3\2\2\2\35\u00c4\3\2\2\2\37\u00ce\3\2\2\2!\u00d2\3\2\2\2#\u00d4\3\2\2" +
                    "\2%\u00da\3\2\2\2\'\u00e4\3\2\2\2)\u00e8\3\2\2\2+\u00ea\3\2\2\2-\u00ee" +
                    "\3\2\2\2/\u00f8\3\2\2\2\61\u00fc\3\2\2\2\63\u0100\3\2\2\2\65\u011d\3\2" +
                    "\2\2\67\u011f\3\2\2\29\u0122\3\2\2\2;\u0125\3\2\2\2=\u0129\3\2\2\2?\u012b" +
                    "\3\2\2\2A\u012d\3\2\2\2C\u013d\3\2\2\2E\u013f\3\2\2\2G\u0142\3\2\2\2I" +
                    "\u014d\3\2\2\2K\u0157\3\2\2\2M\u0159\3\2\2\2O\u015b\3\2\2\2Q\u0162\3\2" +
                    "\2\2S\u0168\3\2\2\2U\u016e\3\2\2\2W\u017b\3\2\2\2Y\u017d\3\2\2\2[\u0184" +
                    "\3\2\2\2]\u0186\3\2\2\2_\u0188\3\2\2\2a\u018a\3\2\2\2c\u018c\3\2\2\2e" +
                    "\u018e\3\2\2\2g\u0190\3\2\2\2i\u0192\3\2\2\2k\u0194\3\2\2\2m\u0196\3\2" +
                    "\2\2o\u0198\3\2\2\2q\u019a\3\2\2\2s\u019d\3\2\2\2u\u01a0\3\2\2\2w\u01a2" +
                    "\3\2\2\2y\u01a6\3\2\2\2{\u01a9\3\2\2\2}\u01b3\3\2\2\2\177\u01bb\3\2\2" +
                    "\2\u0081\u01c3\3\2\2\2\u0083\u0088\5\5\3\2\u0084\u0088\5\7\4\2\u0085\u0088" +
                    "\5\t\5\2\u0086\u0088\5\13\6\2\u0087\u0083\3\2\2\2\u0087\u0084\3\2\2\2" +
                    "\u0087\u0085\3\2\2\2\u0087\u0086\3\2\2\2\u0088\4\3\2\2\2\u0089\u008b\5" +
                    "\17\b\2\u008a\u008c\5\r\7\2\u008b\u008a\3\2\2\2\u008b\u008c\3\2\2\2\u008c" +
                    "\6\3\2\2\2\u008d\u008f\5\33\16\2\u008e\u0090\5\r\7\2\u008f\u008e\3\2\2" +
                    "\2\u008f\u0090\3\2\2\2\u0090\b\3\2\2\2\u0091\u0093\5#\22\2\u0092\u0094" +
                    "\5\r\7\2\u0093\u0092\3\2\2\2\u0093\u0094\3\2\2\2\u0094\n\3\2\2\2\u0095" +
                    "\u0097\5+\26\2\u0096\u0098\5\r\7\2\u0097\u0096\3\2\2\2\u0097\u0098\3\2" +
                    "\2\2\u0098\f\3\2\2\2\u0099\u009a\t\2\2\2\u009a\16\3\2\2\2\u009b\u00a6" +
                    "\7\62\2\2\u009c\u00a3\5\25\13\2\u009d\u009f\5\21\t\2\u009e\u009d\3\2\2" +
                    "\2\u009e\u009f\3\2\2\2\u009f\u00a4\3\2\2\2\u00a0\u00a1\5\31\r\2\u00a1" +
                    "\u00a2\5\21\t\2\u00a2\u00a4\3\2\2\2\u00a3\u009e\3\2\2\2\u00a3\u00a0\3" +
                    "\2\2\2\u00a4\u00a6\3\2\2\2\u00a5\u009b\3\2\2\2\u00a5\u009c\3\2\2\2\u00a6" +
                    "\20\3\2\2\2\u00a7\u00af\5\23\n\2\u00a8\u00aa\5\27\f\2\u00a9\u00a8\3\2" +
                    "\2\2\u00aa\u00ad\3\2\2\2\u00ab\u00a9\3\2\2\2\u00ab\u00ac\3\2\2\2\u00ac" +
                    "\u00ae\3\2\2\2\u00ad\u00ab\3\2\2\2\u00ae\u00b0\5\23\n\2\u00af\u00ab\3" +
                    "\2\2\2\u00af\u00b0\3\2\2\2\u00b0\22\3\2\2\2\u00b1\u00b4\7\62\2\2\u00b2" +
                    "\u00b4\5\25\13\2\u00b3\u00b1\3\2\2\2\u00b3\u00b2\3\2\2\2\u00b4\24\3\2" +
                    "\2\2\u00b5\u00b6\t\3\2\2\u00b6\26\3\2\2\2\u00b7\u00ba\5\23\n\2\u00b8\u00ba" +
                    "\7a\2\2\u00b9\u00b7\3\2\2\2\u00b9\u00b8\3\2\2\2\u00ba\30\3\2\2\2\u00bb" +
                    "\u00bd\7a\2\2\u00bc\u00bb\3\2\2\2\u00bd\u00be\3\2\2\2\u00be\u00bc\3\2" +
                    "\2\2\u00be\u00bf\3\2\2\2\u00bf\32\3\2\2\2\u00c0\u00c1\7\62\2\2\u00c1\u00c2" +
                    "\t\4\2\2\u00c2\u00c3\5\35\17\2\u00c3\34\3\2\2\2\u00c4\u00cc\5\37\20\2" +
                    "\u00c5\u00c7\5!\21\2\u00c6\u00c5\3\2\2\2\u00c7\u00ca\3\2\2\2\u00c8\u00c6" +
                    "\3\2\2\2\u00c8\u00c9\3\2\2\2\u00c9\u00cb\3\2\2\2\u00ca\u00c8\3\2\2\2\u00cb" +
                    "\u00cd\5\37\20\2\u00cc\u00c8\3\2\2\2\u00cc\u00cd\3\2\2\2\u00cd\36\3\2" +
                    "\2\2\u00ce\u00cf\t\5\2\2\u00cf \3\2\2\2\u00d0\u00d3\5\37\20\2\u00d1\u00d3" +
                    "\7a\2\2\u00d2\u00d0\3\2\2\2\u00d2\u00d1\3\2\2\2\u00d3\"\3\2\2\2\u00d4" +
                    "\u00d6\7\62\2\2\u00d5\u00d7\5\31\r\2\u00d6\u00d5\3\2\2\2\u00d6\u00d7\3" +
                    "\2\2\2\u00d7\u00d8\3\2\2\2\u00d8\u00d9\5%\23\2\u00d9$\3\2\2\2\u00da\u00e2" +
                    "\5\'\24\2\u00db\u00dd\5)\25\2\u00dc\u00db\3\2\2\2\u00dd\u00e0\3\2\2\2" +
                    "\u00de\u00dc\3\2\2\2\u00de\u00df\3\2\2\2\u00df\u00e1\3\2\2\2\u00e0\u00de" +
                    "\3\2\2\2\u00e1\u00e3\5\'\24\2\u00e2\u00de\3\2\2\2\u00e2\u00e3\3\2\2\2" +
                    "\u00e3&\3\2\2\2\u00e4\u00e5\t\6\2\2\u00e5(\3\2\2\2\u00e6\u00e9\5\'\24" +
                    "\2\u00e7\u00e9\7a\2\2\u00e8\u00e6\3\2\2\2\u00e8\u00e7\3\2\2\2\u00e9*\3" +
                    "\2\2\2\u00ea\u00eb\7\62\2\2\u00eb\u00ec\t\7\2\2\u00ec\u00ed\5-\27\2\u00ed" +
                    ",\3\2\2\2\u00ee\u00f6\5/\30\2\u00ef\u00f1\5\61\31\2\u00f0\u00ef\3\2\2" +
                    "\2\u00f1\u00f4\3\2\2\2\u00f2\u00f0\3\2\2\2\u00f2\u00f3\3\2\2\2\u00f3\u00f5" +
                    "\3\2\2\2\u00f4\u00f2\3\2\2\2\u00f5\u00f7\5/\30\2\u00f6\u00f2\3\2\2\2\u00f6" +
                    "\u00f7\3\2\2\2\u00f7.\3\2\2\2\u00f8\u00f9\t\b\2\2\u00f9\60\3\2\2\2\u00fa" +
                    "\u00fd\5/\30\2\u00fb\u00fd\7a\2\2\u00fc\u00fa\3\2\2\2\u00fc\u00fb\3\2" +
                    "\2\2\u00fd\62\3\2\2\2\u00fe\u0101\5\65\33\2\u00ff\u0101\5A!\2\u0100\u00fe" +
                    "\3\2\2\2\u0100\u00ff\3\2\2\2\u0101\64\3\2\2\2\u0102\u0103\5\21\t\2\u0103" +
                    "\u0105\7\60\2\2\u0104\u0106\5\21\t\2\u0105\u0104\3\2\2\2\u0105\u0106\3" +
                    "\2\2\2\u0106\u0108\3\2\2\2\u0107\u0109\5\67\34\2\u0108\u0107\3\2\2\2\u0108" +
                    "\u0109\3\2\2\2\u0109\u010b\3\2\2\2\u010a\u010c\5? \2\u010b\u010a\3\2\2" +
                    "\2\u010b\u010c\3\2\2\2\u010c\u011e\3\2\2\2\u010d\u010e\7\60\2\2\u010e" +
                    "\u0110\5\21\t\2\u010f\u0111\5\67\34\2\u0110\u010f\3\2\2\2\u0110\u0111" +
                    "\3\2\2\2\u0111\u0113\3\2\2\2\u0112\u0114\5? \2\u0113\u0112\3\2\2\2\u0113" +
                    "\u0114\3\2\2\2\u0114\u011e\3\2\2\2\u0115\u0116\5\21\t\2\u0116\u0118\5" +
                    "\67\34\2\u0117\u0119\5? \2\u0118\u0117\3\2\2\2\u0118\u0119\3\2\2\2\u0119" +
                    "\u011e\3\2\2\2\u011a\u011b\5\21\t\2\u011b\u011c\5? \2\u011c\u011e\3\2" +
                    "\2\2\u011d\u0102\3\2\2\2\u011d\u010d\3\2\2\2\u011d\u0115\3\2\2\2\u011d" +
                    "\u011a\3\2\2\2\u011e\66\3\2\2\2\u011f\u0120\59\35\2\u0120\u0121\5;\36" +
                    "\2\u01218\3\2\2\2\u0122\u0123\t\t\2\2\u0123:\3\2\2\2\u0124\u0126\5=\37" +
                    "\2\u0125\u0124\3\2\2\2\u0125\u0126\3\2\2\2\u0126\u0127\3\2\2\2\u0127\u0128" +
                    "\5\21\t\2\u0128<\3\2\2\2\u0129\u012a\t\n\2\2\u012a>\3\2\2\2\u012b\u012c" +
                    "\t\13\2\2\u012c@\3\2\2\2\u012d\u012e\5C\"\2\u012e\u0130\5E#\2\u012f\u0131" +
                    "\5? \2\u0130\u012f\3\2\2\2\u0130\u0131\3\2\2\2\u0131B\3\2\2\2\u0132\u0134" +
                    "\5\33\16\2\u0133\u0135\7\60\2\2\u0134\u0133\3\2\2\2\u0134\u0135\3\2\2" +
                    "\2\u0135\u013e\3\2\2\2\u0136\u0137\7\62\2\2\u0137\u0139\t\4\2\2\u0138" +
                    "\u013a\5\35\17\2\u0139\u0138\3\2\2\2\u0139\u013a\3\2\2\2\u013a\u013b\3" +
                    "\2\2\2\u013b\u013c\7\60\2\2\u013c\u013e\5\35\17\2\u013d\u0132\3\2\2\2" +
                    "\u013d\u0136\3\2\2\2\u013eD\3\2\2\2\u013f\u0140\5G$\2\u0140\u0141\5;\36" +
                    "\2\u0141F\3\2\2\2\u0142\u0143\t\f\2\2\u0143H\3\2\2\2\u0144\u0145\7v\2" +
                    "\2\u0145\u0146\7t\2\2\u0146\u0147\7w\2\2\u0147\u014e\7g\2\2\u0148\u0149" +
                    "\7h\2\2\u0149\u014a\7c\2\2\u014a\u014b\7n\2\2\u014b\u014c\7u\2\2\u014c" +
                    "\u014e\7g\2\2\u014d\u0144\3\2\2\2\u014d\u0148\3\2\2\2\u014eJ\3\2\2\2\u014f" +
                    "\u0150\7)\2\2\u0150\u0151\5M\'\2\u0151\u0152\7)\2\2\u0152\u0158\3\2\2" +
                    "\2\u0153\u0154\7)\2\2\u0154\u0155\5U+\2\u0155\u0156\7)\2\2\u0156\u0158" +
                    "\3\2\2\2\u0157\u014f\3\2\2\2\u0157\u0153\3\2\2\2\u0158L\3\2\2\2\u0159" +
                    "\u015a\n\r\2\2\u015aN\3\2\2\2\u015b\u015d\7$\2\2\u015c\u015e\5Q)\2\u015d" +
                    "\u015c\3\2\2\2\u015d\u015e\3\2\2\2\u015e\u015f\3\2\2\2\u015f\u0160\7$" +
                    "\2\2\u0160P\3\2\2\2\u0161\u0163\5S*\2\u0162\u0161\3\2\2\2\u0163\u0164" +
                    "\3\2\2\2\u0164\u0162\3\2\2\2\u0164\u0165\3\2\2\2\u0165R\3\2\2\2\u0166" +
                    "\u0169\n\16\2\2\u0167\u0169\5U+\2\u0168\u0166\3\2\2\2\u0168\u0167\3\2" +
                    "\2\2\u0169T\3\2\2\2\u016a\u016b\7^\2\2\u016b\u016f\t\17\2\2\u016c\u016f" +
                    "\5W,\2\u016d\u016f\5Y-\2\u016e\u016a\3\2\2\2\u016e\u016c\3\2\2\2\u016e" +
                    "\u016d\3\2\2\2\u016fV\3\2\2\2\u0170\u0171\7^\2\2\u0171\u017c\5\'\24\2" +
                    "\u0172\u0173\7^\2\2\u0173\u0174\5\'\24\2\u0174\u0175\5\'\24\2\u0175\u017c" +
                    "\3\2\2\2\u0176\u0177\7^\2\2\u0177\u0178\5[.\2\u0178\u0179\5\'\24\2\u0179" +
                    "\u017a\5\'\24\2\u017a\u017c\3\2\2\2\u017b\u0170\3\2\2\2\u017b\u0172\3" +
                    "\2\2\2\u017b\u0176\3\2\2\2\u017cX\3\2\2\2\u017d\u017e\7^\2\2\u017e\u017f" +
                    "\7w\2\2\u017f\u0180\5\37\20\2\u0180\u0181\5\37\20\2\u0181\u0182\5\37\20" +
                    "\2\u0182\u0183\5\37\20\2\u0183Z\3\2\2\2\u0184\u0185\t\20\2\2\u0185\\\3" +
                    "\2\2\2\u0186\u0187\7*\2\2\u0187^\3\2\2\2\u0188\u0189\7+\2\2\u0189`\3\2" +
                    "\2\2\u018a\u018b\7]\2\2\u018bb\3\2\2\2\u018c\u018d\7_\2\2\u018dd\3\2\2" +
                    "\2\u018e\u018f\7.\2\2\u018ff\3\2\2\2\u0190\u0191\7\60\2\2\u0191h\3\2\2" +
                    "\2\u0192\u0193\7?\2\2\u0193j\3\2\2\2\u0194\u0195\7<\2\2\u0195l\3\2\2\2" +
                    "\u0196\u0197\7-\2\2\u0197n\3\2\2\2\u0198\u0199\7/\2\2\u0199p\3\2\2\2\u019a" +
                    "\u019b\7(\2\2\u019b\u019c\7(\2\2\u019cr\3\2\2\2\u019d\u019e\7~\2\2\u019e" +
                    "\u019f\7~\2\2\u019ft\3\2\2\2\u01a0\u01a1\7B\2\2\u01a1v\3\2\2\2\u01a2\u01a3" +
                    "\7\60\2\2\u01a3\u01a4\7\60\2\2\u01a4\u01a5\7\60\2\2\u01a5x\3\2\2\2\u01a6" +
                    "\u01a7\7\60\2\2\u01a7\u01a8\7\60\2\2\u01a8z\3\2\2\2\u01a9\u01aa\7\"\2" +
                    "\2\u01aa|\3\2\2\2\u01ab\u01b4\7,\2\2\u01ac\u01b0\5\177@\2\u01ad\u01af" +
                    "\5\u0081A\2\u01ae\u01ad\3\2\2\2\u01af\u01b2\3\2\2\2\u01b0\u01ae\3\2\2" +
                    "\2\u01b0\u01b1\3\2\2\2\u01b1\u01b4\3\2\2\2\u01b2\u01b0\3\2\2\2\u01b3\u01ab" +
                    "\3\2\2\2\u01b3\u01ac\3\2\2\2\u01b4~\3\2\2\2\u01b5\u01bc\t\21\2\2\u01b6" +
                    "\u01b7\n\22\2\2\u01b7\u01bc\6@\2\2\u01b8\u01b9\t\23\2\2\u01b9\u01ba\t" +
                    "\24\2\2\u01ba\u01bc\6@\3\2\u01bb\u01b5\3\2\2\2\u01bb\u01b6\3\2\2\2\u01bb" +
                    "\u01b8\3\2\2\2\u01bc\u0080\3\2\2\2\u01bd\u01c4\t\25\2\2\u01be\u01bf\n" +
                    "\22\2\2\u01bf\u01c4\6A\4\2\u01c0\u01c1\t\23\2\2\u01c1\u01c2\t\24\2\2\u01c2" +
                    "\u01c4\6A\5\2\u01c3\u01bd\3\2\2\2\u01c3\u01be\3\2\2\2\u01c3\u01c0\3\2" +
                    "\2\2\u01c4\u0082\3\2\2\2\62\2\u0087\u008b\u008f\u0093\u0097\u009e\u00a3" +
                    "\u00a5\u00ab\u00af\u00b3\u00b9\u00be\u00c8\u00cc\u00d2\u00d6\u00de\u00e2" +
                    "\u00e8\u00f2\u00f6\u00fc\u0100\u0105\u0108\u010b\u0110\u0113\u0118\u011d" +
                    "\u0125\u0130\u0134\u0139\u013d\u014d\u0157\u015d\u0164\u0168\u016e\u017b" +
                    "\u01b0\u01b3\u01bb\u01c3\2";
    public static final ATN _ATN =
            new ATNDeserializer().deserialize(_serializedATN.toCharArray());

    static {
        _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
        for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
            _decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
        }
    }
}