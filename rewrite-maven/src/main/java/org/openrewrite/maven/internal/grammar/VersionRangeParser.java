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
// Generated from /Users/yoshi/Development/Repos/openrewrite/rewrite/rewrite-maven/src/main/antlr/VersionRangeParser.g4 by ANTLR 4.9.3
package org.openrewrite.maven.internal.grammar;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class VersionRangeParser extends Parser {
    static {
        RuntimeMetaData.checkVersion("4.9.3", RuntimeMetaData.VERSION);
    }

    protected static final DFA[] _decisionToDFA;
    protected static final PredictionContextCache _sharedContextCache =
            new PredictionContextCache();
    public static final int
            COMMA = 1, PROPERTY_OPEN = 2, PROPERTY_CLOSE = 3, OPEN_RANGE_OPEN = 4, OPEN_RANGE_CLOSE = 5,
            CLOSED_RANGE_OPEN = 6, CLOSED_RANGE_CLOSE = 7, Version = 8, WS = 9;
    public static final int
            RULE_versionRequirement = 0, RULE_range = 1, RULE_bounds = 2, RULE_exactly = 3,
            RULE_boundedLower = 4, RULE_unboundedLower = 5, RULE_version = 6;

    private static String[] makeRuleNames() {
        return new String[]{
                "versionRequirement", "range", "bounds", "exactly", "boundedLower", "unboundedLower",
                "version"
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

    @Override
    public String getGrammarFileName() {
        return "VersionRangeParser.g4";
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
    public ATN getATN() {
        return _ATN;
    }

    public VersionRangeParser(TokenStream input) {
        super(input);
        _interp = new ParserATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
    }

    public static class VersionRequirementContext extends ParserRuleContext {
        public List<RangeContext> range() {
            return getRuleContexts(RangeContext.class);
        }

        public RangeContext range(int i) {
            return getRuleContext(RangeContext.class, i);
        }

        public List<TerminalNode> COMMA() {
            return getTokens(VersionRangeParser.COMMA);
        }

        public TerminalNode COMMA(int i) {
            return getToken(VersionRangeParser.COMMA, i);
        }

        public VersionRequirementContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_versionRequirement;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof VersionRangeParserListener) ((VersionRangeParserListener) listener).enterVersionRequirement(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof VersionRangeParserListener) ((VersionRangeParserListener) listener).exitVersionRequirement(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof VersionRangeParserVisitor) return ((VersionRangeParserVisitor<? extends T>) visitor).visitVersionRequirement(this);
            else return visitor.visitChildren(this);
        }
    }

    public final VersionRequirementContext versionRequirement() throws RecognitionException {
        VersionRequirementContext _localctx = new VersionRequirementContext(_ctx, getState());
        enterRule(_localctx, 0, RULE_versionRequirement);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(14);
                range();
                setState(19);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == COMMA) {
                    {
                        {
                            setState(15);
                            match(COMMA);
                            setState(16);
                            range();
                        }
                    }
                    setState(21);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        }
        finally {
            exitRule();
        }
        return _localctx;
    }

    public static class RangeContext extends ParserRuleContext {
        public BoundsContext bounds() {
            return getRuleContext(BoundsContext.class, 0);
        }

        public TerminalNode OPEN_RANGE_OPEN() {
            return getToken(VersionRangeParser.OPEN_RANGE_OPEN, 0);
        }

        public TerminalNode CLOSED_RANGE_OPEN() {
            return getToken(VersionRangeParser.CLOSED_RANGE_OPEN, 0);
        }

        public TerminalNode OPEN_RANGE_CLOSE() {
            return getToken(VersionRangeParser.OPEN_RANGE_CLOSE, 0);
        }

        public TerminalNode CLOSED_RANGE_CLOSE() {
            return getToken(VersionRangeParser.CLOSED_RANGE_CLOSE, 0);
        }

        public RangeContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_range;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof VersionRangeParserListener) ((VersionRangeParserListener) listener).enterRange(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof VersionRangeParserListener) ((VersionRangeParserListener) listener).exitRange(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof VersionRangeParserVisitor) return ((VersionRangeParserVisitor<? extends T>) visitor).visitRange(this);
            else return visitor.visitChildren(this);
        }
    }

    public final RangeContext range() throws RecognitionException {
        RangeContext _localctx = new RangeContext(_ctx, getState());
        enterRule(_localctx, 2, RULE_range);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(22);
                _la = _input.LA(1);
                if (!(_la == OPEN_RANGE_OPEN || _la == CLOSED_RANGE_OPEN)) {
                    _errHandler.recoverInline(this);
                }
                else {
                    if (_input.LA(1) == Token.EOF) matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
                setState(23);
                bounds();
                setState(24);
                _la = _input.LA(1);
                if (!(_la == OPEN_RANGE_CLOSE || _la == CLOSED_RANGE_CLOSE)) {
                    _errHandler.recoverInline(this);
                }
                else {
                    if (_input.LA(1) == Token.EOF) matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        }
        finally {
            exitRule();
        }
        return _localctx;
    }

    public static class BoundsContext extends ParserRuleContext {
        public BoundedLowerContext boundedLower() {
            return getRuleContext(BoundedLowerContext.class, 0);
        }

        public UnboundedLowerContext unboundedLower() {
            return getRuleContext(UnboundedLowerContext.class, 0);
        }

        public ExactlyContext exactly() {
            return getRuleContext(ExactlyContext.class, 0);
        }

        public BoundsContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_bounds;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof VersionRangeParserListener) ((VersionRangeParserListener) listener).enterBounds(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof VersionRangeParserListener) ((VersionRangeParserListener) listener).exitBounds(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof VersionRangeParserVisitor) return ((VersionRangeParserVisitor<? extends T>) visitor).visitBounds(this);
            else return visitor.visitChildren(this);
        }
    }

    public final BoundsContext bounds() throws RecognitionException {
        BoundsContext _localctx = new BoundsContext(_ctx, getState());
        enterRule(_localctx, 4, RULE_bounds);
        try {
            setState(29);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 1, _ctx)) {
                case 1:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(26);
                    boundedLower();
                }
                    break;
                case 2:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(27);
                    unboundedLower();
                }
                    break;
                case 3:
                    enterOuterAlt(_localctx, 3);
                {
                    setState(28);
                    exactly();
                }
                    break;
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        }
        finally {
            exitRule();
        }
        return _localctx;
    }

    public static class ExactlyContext extends ParserRuleContext {
        public TerminalNode Version() {
            return getToken(VersionRangeParser.Version, 0);
        }

        public ExactlyContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_exactly;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof VersionRangeParserListener) ((VersionRangeParserListener) listener).enterExactly(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof VersionRangeParserListener) ((VersionRangeParserListener) listener).exitExactly(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof VersionRangeParserVisitor) return ((VersionRangeParserVisitor<? extends T>) visitor).visitExactly(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ExactlyContext exactly() throws RecognitionException {
        ExactlyContext _localctx = new ExactlyContext(_ctx, getState());
        enterRule(_localctx, 6, RULE_exactly);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(31);
                match(Version);
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        }
        finally {
            exitRule();
        }
        return _localctx;
    }

    public static class BoundedLowerContext extends ParserRuleContext {
        public List<TerminalNode> Version() {
            return getTokens(VersionRangeParser.Version);
        }

        public TerminalNode Version(int i) {
            return getToken(VersionRangeParser.Version, i);
        }

        public TerminalNode COMMA() {
            return getToken(VersionRangeParser.COMMA, 0);
        }

        public BoundedLowerContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_boundedLower;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof VersionRangeParserListener) ((VersionRangeParserListener) listener).enterBoundedLower(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof VersionRangeParserListener) ((VersionRangeParserListener) listener).exitBoundedLower(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof VersionRangeParserVisitor) return ((VersionRangeParserVisitor<? extends T>) visitor).visitBoundedLower(this);
            else return visitor.visitChildren(this);
        }
    }

    public final BoundedLowerContext boundedLower() throws RecognitionException {
        BoundedLowerContext _localctx = new BoundedLowerContext(_ctx, getState());
        enterRule(_localctx, 8, RULE_boundedLower);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                {
                    setState(33);
                    match(Version);
                    setState(34);
                    match(COMMA);
                    setState(36);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if (_la == Version) {
                        {
                            setState(35);
                            match(Version);
                        }
                    }

                }
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        }
        finally {
            exitRule();
        }
        return _localctx;
    }

    public static class UnboundedLowerContext extends ParserRuleContext {
        public TerminalNode COMMA() {
            return getToken(VersionRangeParser.COMMA, 0);
        }

        public TerminalNode Version() {
            return getToken(VersionRangeParser.Version, 0);
        }

        public UnboundedLowerContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_unboundedLower;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof VersionRangeParserListener) ((VersionRangeParserListener) listener).enterUnboundedLower(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof VersionRangeParserListener) ((VersionRangeParserListener) listener).exitUnboundedLower(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof VersionRangeParserVisitor) return ((VersionRangeParserVisitor<? extends T>) visitor).visitUnboundedLower(this);
            else return visitor.visitChildren(this);
        }
    }

    public final UnboundedLowerContext unboundedLower() throws RecognitionException {
        UnboundedLowerContext _localctx = new UnboundedLowerContext(_ctx, getState());
        enterRule(_localctx, 10, RULE_unboundedLower);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                {
                    setState(38);
                    match(COMMA);
                    setState(40);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if (_la == Version) {
                        {
                            setState(39);
                            match(Version);
                        }
                    }

                }
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        }
        finally {
            exitRule();
        }
        return _localctx;
    }

    public static class VersionContext extends ParserRuleContext {
        public TerminalNode Version() {
            return getToken(VersionRangeParser.Version, 0);
        }

        public TerminalNode PROPERTY_OPEN() {
            return getToken(VersionRangeParser.PROPERTY_OPEN, 0);
        }

        public TerminalNode PROPERTY_CLOSE() {
            return getToken(VersionRangeParser.PROPERTY_CLOSE, 0);
        }

        public VersionContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_version;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof VersionRangeParserListener) ((VersionRangeParserListener) listener).enterVersion(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof VersionRangeParserListener) ((VersionRangeParserListener) listener).exitVersion(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof VersionRangeParserVisitor) return ((VersionRangeParserVisitor<? extends T>) visitor).visitVersion(this);
            else return visitor.visitChildren(this);
        }
    }

    public final VersionContext version() throws RecognitionException {
        VersionContext _localctx = new VersionContext(_ctx, getState());
        enterRule(_localctx, 12, RULE_version);
        try {
            setState(46);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case Version:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(42);
                    match(Version);
                }
                    break;
                case PROPERTY_OPEN:
                    enterOuterAlt(_localctx, 2);
                {
                    {
                        setState(43);
                        match(PROPERTY_OPEN);
                        setState(44);
                        match(Version);
                        setState(45);
                        match(PROPERTY_CLOSE);
                    }
                }
                    break;
                default:
                    throw new NoViableAltException(this);
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        }
        finally {
            exitRule();
        }
        return _localctx;
    }

    public static final String _serializedATN =
            "\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\13\63\4\2\t\2\4\3" +
                    "\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\3\2\3\2\3\2\7\2\24\n\2\f" +
                    "\2\16\2\27\13\2\3\3\3\3\3\3\3\3\3\4\3\4\3\4\5\4 \n\4\3\5\3\5\3\6\3\6\3" +
                    "\6\5\6\'\n\6\3\7\3\7\5\7+\n\7\3\b\3\b\3\b\3\b\5\b\61\n\b\3\b\2\2\t\2\4" +
                    "\6\b\n\f\16\2\4\4\2\6\6\b\b\4\2\7\7\t\t\2\61\2\20\3\2\2\2\4\30\3\2\2\2" +
                    "\6\37\3\2\2\2\b!\3\2\2\2\n#\3\2\2\2\f(\3\2\2\2\16\60\3\2\2\2\20\25\5\4" +
                    "\3\2\21\22\7\3\2\2\22\24\5\4\3\2\23\21\3\2\2\2\24\27\3\2\2\2\25\23\3\2" +
                    "\2\2\25\26\3\2\2\2\26\3\3\2\2\2\27\25\3\2\2\2\30\31\t\2\2\2\31\32\5\6" +
                    "\4\2\32\33\t\3\2\2\33\5\3\2\2\2\34 \5\n\6\2\35 \5\f\7\2\36 \5\b\5\2\37" +
                    "\34\3\2\2\2\37\35\3\2\2\2\37\36\3\2\2\2 \7\3\2\2\2!\"\7\n\2\2\"\t\3\2" +
                    "\2\2#$\7\n\2\2$&\7\3\2\2%\'\7\n\2\2&%\3\2\2\2&\'\3\2\2\2\'\13\3\2\2\2" +
                    "(*\7\3\2\2)+\7\n\2\2*)\3\2\2\2*+\3\2\2\2+\r\3\2\2\2,\61\7\n\2\2-.\7\4" +
                    "\2\2./\7\n\2\2/\61\7\5\2\2\60,\3\2\2\2\60-\3\2\2\2\61\17\3\2\2\2\7\25" +
                    "\37&*\60";
    public static final ATN _ATN =
            new ATNDeserializer().deserialize(_serializedATN.toCharArray());

    static {
        _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
        for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
            _decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
        }
    }
}