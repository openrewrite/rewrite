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
// Generated from /Users/jon/Projects/github/openrewrite/rewrite/rewrite-java/src/main/antlr/MethodSignatureLexer.g4 by ANTLR 4.9.3
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
public class MethodSignatureLexer extends Lexer {
    static {
        RuntimeMetaData.checkVersion("4.9.3", RuntimeMetaData.VERSION);
    }

    protected static final DFA[] _decisionToDFA;
    protected static final PredictionContextCache _sharedContextCache =
            new PredictionContextCache();
    public static final int
            CONSTRUCTOR = 1, LPAREN = 2, RPAREN = 3, LBRACK = 4, RBRACK = 5, COMMA = 6, DOT = 7,
            BANG = 8, WILDCARD = 9, AND = 10, OR = 11, ELLIPSIS = 12, DOTDOT = 13, POUND = 14, SPACE = 15,
            Identifier = 16;
    public static String[] channelNames = {
            "DEFAULT_TOKEN_CHANNEL", "HIDDEN"
    };

    public static String[] modeNames = {
            "DEFAULT_MODE"
    };

    private static String[] makeRuleNames() {
        return new String[]{
                "CONSTRUCTOR", "LPAREN", "RPAREN", "LBRACK", "RBRACK", "COMMA", "DOT",
                "BANG", "WILDCARD", "AND", "OR", "ELLIPSIS", "DOTDOT", "POUND", "SPACE",
                "Identifier", "JavaLetter", "JavaLetterOrDigit"
        };
    }
    public static final String[] ruleNames = makeRuleNames();

    private static String[] makeLiteralNames() {
        return new String[]{
                null, "'<constructor>'", "'('", "')'", "'['", "']'", "','", "'.'", "'!'",
                "'*'", "'&&'", "'||'", "'...'", "'..'", "'#'", "' '"
        };
    }
    private static final String[] _LITERAL_NAMES = makeLiteralNames();

    private static String[] makeSymbolicNames() {
        return new String[]{
                null, "CONSTRUCTOR", "LPAREN", "RPAREN", "LBRACK", "RBRACK", "COMMA",
                "DOT", "BANG", "WILDCARD", "AND", "OR", "ELLIPSIS", "DOTDOT", "POUND",
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


    public MethodSignatureLexer(CharStream input) {
        super(input);
        _interp = new LexerATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
    }

    @Override
    public String getGrammarFileName() {
        return "MethodSignatureLexer.g4";
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
            case 16:
                return JavaLetter_sempred((RuleContext) _localctx, predIndex);
            case 17:
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
            "\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\22m\b\1\4\2\t\2\4" +
                    "\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t" +
                    "\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22" +
                    "\4\23\t\23\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\3" +
                    "\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3\t\3\t\3\n\3\n\3\13\3\13" +
                    "\3\13\3\f\3\f\3\f\3\r\3\r\3\r\3\r\3\16\3\16\3\16\3\17\3\17\3\20\3\20\3" +
                    "\21\3\21\7\21Y\n\21\f\21\16\21\\\13\21\3\22\3\22\3\22\3\22\3\22\3\22\5" +
                    "\22d\n\22\3\23\3\23\3\23\3\23\3\23\3\23\5\23l\n\23\2\2\24\3\3\5\4\7\5" +
                    "\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21!\22#\2" +
                    "%\2\3\2\7\6\2&&C\\aac|\4\2\2\u0081\ud802\udc01\3\2\ud802\udc01\3\2\udc02" +
                    "\ue001\7\2&&\62;C\\aac|\2o\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2" +
                    "\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2" +
                    "\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3" +
                    "\2\2\2\2!\3\2\2\2\3\'\3\2\2\2\5\65\3\2\2\2\7\67\3\2\2\2\t9\3\2\2\2\13" +
                    ";\3\2\2\2\r=\3\2\2\2\17?\3\2\2\2\21A\3\2\2\2\23C\3\2\2\2\25E\3\2\2\2\27" +
                    "H\3\2\2\2\31K\3\2\2\2\33O\3\2\2\2\35R\3\2\2\2\37T\3\2\2\2!V\3\2\2\2#c" +
                    "\3\2\2\2%k\3\2\2\2\'(\7>\2\2()\7e\2\2)*\7q\2\2*+\7p\2\2+,\7u\2\2,-\7v" +
                    "\2\2-.\7t\2\2./\7w\2\2/\60\7e\2\2\60\61\7v\2\2\61\62\7q\2\2\62\63\7t\2" +
                    "\2\63\64\7@\2\2\64\4\3\2\2\2\65\66\7*\2\2\66\6\3\2\2\2\678\7+\2\28\b\3" +
                    "\2\2\29:\7]\2\2:\n\3\2\2\2;<\7_\2\2<\f\3\2\2\2=>\7.\2\2>\16\3\2\2\2?@" +
                    "\7\60\2\2@\20\3\2\2\2AB\7#\2\2B\22\3\2\2\2CD\7,\2\2D\24\3\2\2\2EF\7(\2" +
                    "\2FG\7(\2\2G\26\3\2\2\2HI\7~\2\2IJ\7~\2\2J\30\3\2\2\2KL\7\60\2\2LM\7\60" +
                    "\2\2MN\7\60\2\2N\32\3\2\2\2OP\7\60\2\2PQ\7\60\2\2Q\34\3\2\2\2RS\7%\2\2" +
                    "S\36\3\2\2\2TU\7\"\2\2U \3\2\2\2VZ\5#\22\2WY\5%\23\2XW\3\2\2\2Y\\\3\2" +
                    "\2\2ZX\3\2\2\2Z[\3\2\2\2[\"\3\2\2\2\\Z\3\2\2\2]d\t\2\2\2^_\n\3\2\2_d\6" +
                    "\22\2\2`a\t\4\2\2ab\t\5\2\2bd\6\22\3\2c]\3\2\2\2c^\3\2\2\2c`\3\2\2\2d" +
                    "$\3\2\2\2el\t\6\2\2fg\n\3\2\2gl\6\23\4\2hi\t\4\2\2ij\t\5\2\2jl\6\23\5" +
                    "\2ke\3\2\2\2kf\3\2\2\2kh\3\2\2\2l&\3\2\2\2\6\2Zck\2";
    public static final ATN _ATN =
            new ATNDeserializer().deserialize(_serializedATN.toCharArray());

    static {
        _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
        for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
            _decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
        }
    }
}