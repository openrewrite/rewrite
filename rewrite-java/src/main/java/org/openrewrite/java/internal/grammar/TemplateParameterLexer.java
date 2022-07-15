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
// Generated from /Users/jon/Projects/github/openrewrite/rewrite/rewrite-java/src/main/antlr/TemplateParameterLexer.g4 by ANTLR 4.9.3
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
public class TemplateParameterLexer extends Lexer {
    static {
        RuntimeMetaData.checkVersion("4.9.3", RuntimeMetaData.VERSION);
    }

    protected static final DFA[] _decisionToDFA;
    protected static final PredictionContextCache _sharedContextCache =
            new PredictionContextCache();
    public static final int
            LPAREN = 1, RPAREN = 2, LBRACK = 3, RBRACK = 4, DOT = 5, COMMA = 6, SPACE = 7, FullyQualifiedName = 8,
            Number = 9, Identifier = 10;
    public static String[] channelNames = {
            "DEFAULT_TOKEN_CHANNEL", "HIDDEN"
    };

    public static String[] modeNames = {
            "DEFAULT_MODE"
    };

    private static String[] makeRuleNames() {
        return new String[]{
                "LPAREN", "RPAREN", "LBRACK", "RBRACK", "DOT", "COMMA", "SPACE", "FullyQualifiedName",
                "Number", "Identifier", "JavaLetter", "JavaLetterOrDigit"
        };
    }
    public static final String[] ruleNames = makeRuleNames();

    private static String[] makeLiteralNames() {
        return new String[]{
                null, "'('", "')'", "'['", "']'", "'.'", "','", "' '"
        };
    }
    private static final String[] _LITERAL_NAMES = makeLiteralNames();

    private static String[] makeSymbolicNames() {
        return new String[]{
                null, "LPAREN", "RPAREN", "LBRACK", "RBRACK", "DOT", "COMMA", "SPACE",
                "FullyQualifiedName", "Number", "Identifier"
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


    public TemplateParameterLexer(CharStream input) {
        super(input);
        _interp = new LexerATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
    }

    @Override
    public String getGrammarFileName() {
        return "TemplateParameterLexer.g4";
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
            case 10:
                return JavaLetter_sempred((RuleContext) _localctx, predIndex);
            case 11:
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
            "\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\f\u0081\b\1\4\2\t" +
                    "\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13" +
                    "\t\13\4\f\t\f\4\r\t\r\3\2\3\2\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7" +
                    "\3\b\3\b\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3" +
                    "\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t" +
                    "\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3" +
                    "\t\3\t\3\t\3\t\6\t`\n\t\r\t\16\ta\5\td\n\t\3\n\6\ng\n\n\r\n\16\nh\3\13" +
                    "\3\13\7\13m\n\13\f\13\16\13p\13\13\3\f\3\f\3\f\3\f\3\f\3\f\5\fx\n\f\3" +
                    "\r\3\r\3\r\3\r\3\r\3\r\5\r\u0080\n\r\2\2\16\3\3\5\4\7\5\t\6\13\7\r\b\17" +
                    "\t\21\n\23\13\25\f\27\2\31\2\3\2\b\3\2\62;\6\2&&C\\aac|\4\2\2\u0081\ud802" +
                    "\udc01\3\2\ud802\udc01\3\2\udc02\ue001\7\2&&\62;C\\aac|\2\u008f\2\3\3" +
                    "\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2" +
                    "\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\3\33\3\2\2\2\5\35\3" +
                    "\2\2\2\7\37\3\2\2\2\t!\3\2\2\2\13#\3\2\2\2\r%\3\2\2\2\17\'\3\2\2\2\21" +
                    "c\3\2\2\2\23f\3\2\2\2\25j\3\2\2\2\27w\3\2\2\2\31\177\3\2\2\2\33\34\7*" +
                    "\2\2\34\4\3\2\2\2\35\36\7+\2\2\36\6\3\2\2\2\37 \7]\2\2 \b\3\2\2\2!\"\7" +
                    "_\2\2\"\n\3\2\2\2#$\7\60\2\2$\f\3\2\2\2%&\7.\2\2&\16\3\2\2\2\'(\7\"\2" +
                    "\2(\20\3\2\2\2)*\7d\2\2*+\7q\2\2+,\7q\2\2,-\7n\2\2-.\7g\2\2./\7c\2\2/" +
                    "d\7p\2\2\60\61\7d\2\2\61\62\7{\2\2\62\63\7v\2\2\63d\7g\2\2\64\65\7e\2" +
                    "\2\65\66\7j\2\2\66\67\7c\2\2\67d\7t\2\289\7f\2\29:\7q\2\2:;\7w\2\2;<\7" +
                    "d\2\2<=\7n\2\2=d\7g\2\2>?\7h\2\2?@\7n\2\2@A\7q\2\2AB\7c\2\2Bd\7v\2\2C" +
                    "D\7k\2\2DE\7p\2\2Ed\7v\2\2FG\7n\2\2GH\7q\2\2HI\7p\2\2Id\7i\2\2JK\7u\2" +
                    "\2KL\7j\2\2LM\7q\2\2MN\7t\2\2Nd\7v\2\2OP\7U\2\2PQ\7v\2\2QR\7t\2\2RS\7" +
                    "k\2\2ST\7p\2\2Td\7i\2\2UV\7Q\2\2VW\7d\2\2WX\7l\2\2XY\7g\2\2YZ\7e\2\2Z" +
                    "d\7v\2\2[_\5\25\13\2\\]\5\13\6\2]^\5\25\13\2^`\3\2\2\2_\\\3\2\2\2`a\3" +
                    "\2\2\2a_\3\2\2\2ab\3\2\2\2bd\3\2\2\2c)\3\2\2\2c\60\3\2\2\2c\64\3\2\2\2" +
                    "c8\3\2\2\2c>\3\2\2\2cC\3\2\2\2cF\3\2\2\2cJ\3\2\2\2cO\3\2\2\2cU\3\2\2\2" +
                    "c[\3\2\2\2d\22\3\2\2\2eg\t\2\2\2fe\3\2\2\2gh\3\2\2\2hf\3\2\2\2hi\3\2\2" +
                    "\2i\24\3\2\2\2jn\5\27\f\2km\5\31\r\2lk\3\2\2\2mp\3\2\2\2nl\3\2\2\2no\3" +
                    "\2\2\2o\26\3\2\2\2pn\3\2\2\2qx\t\3\2\2rs\n\4\2\2sx\6\f\2\2tu\t\5\2\2u" +
                    "v\t\6\2\2vx\6\f\3\2wq\3\2\2\2wr\3\2\2\2wt\3\2\2\2x\30\3\2\2\2y\u0080\t" +
                    "\7\2\2z{\n\4\2\2{\u0080\6\r\4\2|}\t\5\2\2}~\t\6\2\2~\u0080\6\r\5\2\177" +
                    "y\3\2\2\2\177z\3\2\2\2\177|\3\2\2\2\u0080\32\3\2\2\2\t\2achnw\177\2";
    public static final ATN _ATN =
            new ATNDeserializer().deserialize(_serializedATN.toCharArray());

    static {
        _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
        for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
            _decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
        }
    }
}