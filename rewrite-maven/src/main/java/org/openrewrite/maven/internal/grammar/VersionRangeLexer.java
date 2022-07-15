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
// Generated from /Users/yoshi/Development/Repos/openrewrite/rewrite/rewrite-maven/src/main/antlr/VersionRangeLexer.g4 by ANTLR 4.9.3
package org.openrewrite.maven.internal.grammar;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class VersionRangeLexer extends Lexer {
    static {
        RuntimeMetaData.checkVersion("4.9.3", RuntimeMetaData.VERSION);
    }

    protected static final DFA[] _decisionToDFA;
    protected static final PredictionContextCache _sharedContextCache =
            new PredictionContextCache();
    public static final int
            COMMA = 1, PROPERTY_OPEN = 2, PROPERTY_CLOSE = 3, OPEN_RANGE_OPEN = 4, OPEN_RANGE_CLOSE = 5,
            CLOSED_RANGE_OPEN = 6, CLOSED_RANGE_CLOSE = 7, Version = 8, WS = 9;
    public static String[] channelNames = {
            "DEFAULT_TOKEN_CHANNEL", "HIDDEN"
    };

    public static String[] modeNames = {
            "DEFAULT_MODE"
    };

    private static String[] makeRuleNames() {
        return new String[]{
                "COMMA", "PROPERTY_OPEN", "PROPERTY_CLOSE", "OPEN_RANGE_OPEN", "OPEN_RANGE_CLOSE",
                "CLOSED_RANGE_OPEN", "CLOSED_RANGE_CLOSE", "Version", "WS"
        };
    }
    public static final String[] ruleNames = makeRuleNames();

    private static String[] makeLiteralNames() {
        return new String[]{
                null, "','", "'${'", "'}'", "'('", "')'", "'['", "']'"
        };
    }
    private static final String[] _LITERAL_NAMES = makeLiteralNames();

    private static String[] makeSymbolicNames() {
        return new String[]{
                null, "COMMA", "PROPERTY_OPEN", "PROPERTY_CLOSE", "OPEN_RANGE_OPEN",
                "OPEN_RANGE_CLOSE", "CLOSED_RANGE_OPEN", "CLOSED_RANGE_CLOSE", "Version",
                "WS"
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


    public VersionRangeLexer(CharStream input) {
        super(input);
        _interp = new LexerATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
    }

    @Override
    public String getGrammarFileName() {
        return "VersionRangeLexer.g4";
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
            "\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\13\60\b\1\4\2\t\2" +
                    "\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\3\2\3" +
                    "\2\3\3\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3\t\6\t&\n\t\r" +
                    "\t\16\t\'\3\n\6\n+\n\n\r\n\16\n,\3\n\3\n\2\2\13\3\3\5\4\7\5\t\6\13\7\r" +
                    "\b\17\t\21\n\23\13\3\2\4\b\2--/\60\62;C\\aac|\5\2\13\f\16\17\"\"\2\61" +
                    "\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2" +
                    "\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\3\25\3\2\2\2\5\27\3\2\2\2" +
                    "\7\32\3\2\2\2\t\34\3\2\2\2\13\36\3\2\2\2\r \3\2\2\2\17\"\3\2\2\2\21%\3" +
                    "\2\2\2\23*\3\2\2\2\25\26\7.\2\2\26\4\3\2\2\2\27\30\7&\2\2\30\31\7}\2\2" +
                    "\31\6\3\2\2\2\32\33\7\177\2\2\33\b\3\2\2\2\34\35\7*\2\2\35\n\3\2\2\2\36" +
                    "\37\7+\2\2\37\f\3\2\2\2 !\7]\2\2!\16\3\2\2\2\"#\7_\2\2#\20\3\2\2\2$&\t" +
                    "\2\2\2%$\3\2\2\2&\'\3\2\2\2\'%\3\2\2\2\'(\3\2\2\2(\22\3\2\2\2)+\t\3\2" +
                    "\2*)\3\2\2\2+,\3\2\2\2,*\3\2\2\2,-\3\2\2\2-.\3\2\2\2./\b\n\2\2/\24\3\2" +
                    "\2\2\5\2\',\3\b\2\2";
    public static final ATN _ATN =
            new ATNDeserializer().deserialize(_serializedATN.toCharArray());

    static {
        _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
        for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
            _decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
        }
    }
}