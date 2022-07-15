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
// Generated from /Users/jon/Projects/github/openrewrite/rewrite/rewrite-java/src/main/antlr/MethodSignatureParser.g4 by ANTLR 4.9.3
package org.openrewrite.java.internal.grammar;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class MethodSignatureParser extends Parser {
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
    public static final int
            RULE_methodPattern = 0, RULE_formalParametersPattern = 1, RULE_formalsPattern = 2,
            RULE_dotDot = 3, RULE_formalsPatternAfterDotDot = 4, RULE_optionalParensTypePattern = 5,
            RULE_targetTypePattern = 6, RULE_formalTypePattern = 7, RULE_classNameOrInterface = 8,
            RULE_simpleNamePattern = 9;

    private static String[] makeRuleNames() {
        return new String[]{
                "methodPattern", "formalParametersPattern", "formalsPattern", "dotDot",
                "formalsPatternAfterDotDot", "optionalParensTypePattern", "targetTypePattern",
                "formalTypePattern", "classNameOrInterface", "simpleNamePattern"
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

    @Override
    public String getGrammarFileName() {
        return "MethodSignatureParser.g4";
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

    public MethodSignatureParser(TokenStream input) {
        super(input);
        _interp = new ParserATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
    }

    public static class MethodPatternContext extends ParserRuleContext {
        public TargetTypePatternContext targetTypePattern() {
            return getRuleContext(TargetTypePatternContext.class, 0);
        }

        public SimpleNamePatternContext simpleNamePattern() {
            return getRuleContext(SimpleNamePatternContext.class, 0);
        }

        public FormalParametersPatternContext formalParametersPattern() {
            return getRuleContext(FormalParametersPatternContext.class, 0);
        }

        public TerminalNode DOT() {
            return getToken(MethodSignatureParser.DOT, 0);
        }

        public TerminalNode POUND() {
            return getToken(MethodSignatureParser.POUND, 0);
        }

        public List<TerminalNode> SPACE() {
            return getTokens(MethodSignatureParser.SPACE);
        }

        public TerminalNode SPACE(int i) {
            return getToken(MethodSignatureParser.SPACE, i);
        }

        public MethodPatternContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_methodPattern;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof MethodSignatureParserListener) ((MethodSignatureParserListener) listener).enterMethodPattern(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof MethodSignatureParserListener) ((MethodSignatureParserListener) listener).exitMethodPattern(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof MethodSignatureParserVisitor) return ((MethodSignatureParserVisitor<? extends T>) visitor).visitMethodPattern(this);
            else return visitor.visitChildren(this);
        }
    }

    public final MethodPatternContext methodPattern() throws RecognitionException {
        MethodPatternContext _localctx = new MethodPatternContext(_ctx, getState());
        enterRule(_localctx, 0, RULE_methodPattern);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(20);
                targetTypePattern(0);
                setState(29);
                _errHandler.sync(this);
                switch (_input.LA(1)) {
                    case CONSTRUCTOR:
                    case WILDCARD:
                    case SPACE:
                    case Identifier:
                    {
                        setState(24);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while (_la == SPACE) {
                            {
                                {
                                    setState(21);
                                    match(SPACE);
                                }
                            }
                            setState(26);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                    }
                        break;
                    case DOT:
                    {
                        setState(27);
                        match(DOT);
                    }
                        break;
                    case POUND:
                    {
                        setState(28);
                        match(POUND);
                    }
                        break;
                    default:
                        throw new NoViableAltException(this);
                }
                setState(31);
                simpleNamePattern();
                setState(32);
                formalParametersPattern();
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

    public static class FormalParametersPatternContext extends ParserRuleContext {
        public TerminalNode LPAREN() {
            return getToken(MethodSignatureParser.LPAREN, 0);
        }

        public TerminalNode RPAREN() {
            return getToken(MethodSignatureParser.RPAREN, 0);
        }

        public FormalsPatternContext formalsPattern() {
            return getRuleContext(FormalsPatternContext.class, 0);
        }

        public FormalParametersPatternContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_formalParametersPattern;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof MethodSignatureParserListener) ((MethodSignatureParserListener) listener).enterFormalParametersPattern(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof MethodSignatureParserListener) ((MethodSignatureParserListener) listener).exitFormalParametersPattern(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof MethodSignatureParserVisitor) return ((MethodSignatureParserVisitor<? extends T>) visitor).visitFormalParametersPattern(this);
            else return visitor.visitChildren(this);
        }
    }

    public final FormalParametersPatternContext formalParametersPattern() throws RecognitionException {
        FormalParametersPatternContext _localctx = new FormalParametersPatternContext(_ctx, getState());
        enterRule(_localctx, 2, RULE_formalParametersPattern);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(34);
                match(LPAREN);
                setState(36);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << LPAREN) | (1L << DOT) | (1L << BANG) | (1L << WILDCARD) | (1L << DOTDOT) | (1L << Identifier))) != 0)) {
                    {
                        setState(35);
                        formalsPattern();
                    }
                }

                setState(38);
                match(RPAREN);
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

    public static class FormalsPatternContext extends ParserRuleContext {
        public DotDotContext dotDot() {
            return getRuleContext(DotDotContext.class, 0);
        }

        public List<TerminalNode> COMMA() {
            return getTokens(MethodSignatureParser.COMMA);
        }

        public TerminalNode COMMA(int i) {
            return getToken(MethodSignatureParser.COMMA, i);
        }

        public List<FormalsPatternAfterDotDotContext> formalsPatternAfterDotDot() {
            return getRuleContexts(FormalsPatternAfterDotDotContext.class);
        }

        public FormalsPatternAfterDotDotContext formalsPatternAfterDotDot(int i) {
            return getRuleContext(FormalsPatternAfterDotDotContext.class, i);
        }

        public List<TerminalNode> SPACE() {
            return getTokens(MethodSignatureParser.SPACE);
        }

        public TerminalNode SPACE(int i) {
            return getToken(MethodSignatureParser.SPACE, i);
        }

        public OptionalParensTypePatternContext optionalParensTypePattern() {
            return getRuleContext(OptionalParensTypePatternContext.class, 0);
        }

        public List<FormalsPatternContext> formalsPattern() {
            return getRuleContexts(FormalsPatternContext.class);
        }

        public FormalsPatternContext formalsPattern(int i) {
            return getRuleContext(FormalsPatternContext.class, i);
        }

        public FormalTypePatternContext formalTypePattern() {
            return getRuleContext(FormalTypePatternContext.class, 0);
        }

        public TerminalNode ELLIPSIS() {
            return getToken(MethodSignatureParser.ELLIPSIS, 0);
        }

        public FormalsPatternContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_formalsPattern;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof MethodSignatureParserListener) ((MethodSignatureParserListener) listener).enterFormalsPattern(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof MethodSignatureParserListener) ((MethodSignatureParserListener) listener).exitFormalsPattern(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof MethodSignatureParserVisitor) return ((MethodSignatureParserVisitor<? extends T>) visitor).visitFormalsPattern(this);
            else return visitor.visitChildren(this);
        }
    }

    public final FormalsPatternContext formalsPattern() throws RecognitionException {
        FormalsPatternContext _localctx = new FormalsPatternContext(_ctx, getState());
        enterRule(_localctx, 4, RULE_formalsPattern);
        int _la;
        try {
            int _alt;
            setState(71);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 7, _ctx)) {
                case 1:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(40);
                    dotDot();
                    setState(51);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 4, _ctx);
                    while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                        if (_alt == 1) {
                            {
                                {
                                    setState(41);
                                    match(COMMA);
                                    setState(45);
                                    _errHandler.sync(this);
                                    _la = _input.LA(1);
                                    while (_la == SPACE) {
                                        {
                                            {
                                                setState(42);
                                                match(SPACE);
                                            }
                                        }
                                        setState(47);
                                        _errHandler.sync(this);
                                        _la = _input.LA(1);
                                    }
                                    setState(48);
                                    formalsPatternAfterDotDot();
                                }
                            }
                        }
                        setState(53);
                        _errHandler.sync(this);
                        _alt = getInterpreter().adaptivePredict(_input, 4, _ctx);
                    }
                }
                    break;
                case 2:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(54);
                    optionalParensTypePattern();
                    setState(65);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 6, _ctx);
                    while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                        if (_alt == 1) {
                            {
                                {
                                    setState(55);
                                    match(COMMA);
                                    setState(59);
                                    _errHandler.sync(this);
                                    _la = _input.LA(1);
                                    while (_la == SPACE) {
                                        {
                                            {
                                                setState(56);
                                                match(SPACE);
                                            }
                                        }
                                        setState(61);
                                        _errHandler.sync(this);
                                        _la = _input.LA(1);
                                    }
                                    setState(62);
                                    formalsPattern();
                                }
                            }
                        }
                        setState(67);
                        _errHandler.sync(this);
                        _alt = getInterpreter().adaptivePredict(_input, 6, _ctx);
                    }
                }
                    break;
                case 3:
                    enterOuterAlt(_localctx, 3);
                {
                    setState(68);
                    formalTypePattern(0);
                    setState(69);
                    match(ELLIPSIS);
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

    public static class DotDotContext extends ParserRuleContext {
        public TerminalNode DOTDOT() {
            return getToken(MethodSignatureParser.DOTDOT, 0);
        }

        public DotDotContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_dotDot;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof MethodSignatureParserListener) ((MethodSignatureParserListener) listener).enterDotDot(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof MethodSignatureParserListener) ((MethodSignatureParserListener) listener).exitDotDot(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof MethodSignatureParserVisitor) return ((MethodSignatureParserVisitor<? extends T>) visitor).visitDotDot(this);
            else return visitor.visitChildren(this);
        }
    }

    public final DotDotContext dotDot() throws RecognitionException {
        DotDotContext _localctx = new DotDotContext(_ctx, getState());
        enterRule(_localctx, 6, RULE_dotDot);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(73);
                match(DOTDOT);
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

    public static class FormalsPatternAfterDotDotContext extends ParserRuleContext {
        public OptionalParensTypePatternContext optionalParensTypePattern() {
            return getRuleContext(OptionalParensTypePatternContext.class, 0);
        }

        public List<TerminalNode> COMMA() {
            return getTokens(MethodSignatureParser.COMMA);
        }

        public TerminalNode COMMA(int i) {
            return getToken(MethodSignatureParser.COMMA, i);
        }

        public List<FormalsPatternAfterDotDotContext> formalsPatternAfterDotDot() {
            return getRuleContexts(FormalsPatternAfterDotDotContext.class);
        }

        public FormalsPatternAfterDotDotContext formalsPatternAfterDotDot(int i) {
            return getRuleContext(FormalsPatternAfterDotDotContext.class, i);
        }

        public List<TerminalNode> SPACE() {
            return getTokens(MethodSignatureParser.SPACE);
        }

        public TerminalNode SPACE(int i) {
            return getToken(MethodSignatureParser.SPACE, i);
        }

        public FormalTypePatternContext formalTypePattern() {
            return getRuleContext(FormalTypePatternContext.class, 0);
        }

        public TerminalNode ELLIPSIS() {
            return getToken(MethodSignatureParser.ELLIPSIS, 0);
        }

        public FormalsPatternAfterDotDotContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_formalsPatternAfterDotDot;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof MethodSignatureParserListener) ((MethodSignatureParserListener) listener).enterFormalsPatternAfterDotDot(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof MethodSignatureParserListener) ((MethodSignatureParserListener) listener).exitFormalsPatternAfterDotDot(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof MethodSignatureParserVisitor) return ((MethodSignatureParserVisitor<? extends T>) visitor).visitFormalsPatternAfterDotDot(this);
            else return visitor.visitChildren(this);
        }
    }

    public final FormalsPatternAfterDotDotContext formalsPatternAfterDotDot() throws RecognitionException {
        FormalsPatternAfterDotDotContext _localctx = new FormalsPatternAfterDotDotContext(_ctx, getState());
        enterRule(_localctx, 8, RULE_formalsPatternAfterDotDot);
        int _la;
        try {
            int _alt;
            setState(92);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 10, _ctx)) {
                case 1:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(75);
                    optionalParensTypePattern();
                    setState(86);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 9, _ctx);
                    while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                        if (_alt == 1) {
                            {
                                {
                                    setState(76);
                                    match(COMMA);
                                    setState(80);
                                    _errHandler.sync(this);
                                    _la = _input.LA(1);
                                    while (_la == SPACE) {
                                        {
                                            {
                                                setState(77);
                                                match(SPACE);
                                            }
                                        }
                                        setState(82);
                                        _errHandler.sync(this);
                                        _la = _input.LA(1);
                                    }
                                    setState(83);
                                    formalsPatternAfterDotDot();
                                }
                            }
                        }
                        setState(88);
                        _errHandler.sync(this);
                        _alt = getInterpreter().adaptivePredict(_input, 9, _ctx);
                    }
                }
                    break;
                case 2:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(89);
                    formalTypePattern(0);
                    setState(90);
                    match(ELLIPSIS);
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

    public static class OptionalParensTypePatternContext extends ParserRuleContext {
        public TerminalNode LPAREN() {
            return getToken(MethodSignatureParser.LPAREN, 0);
        }

        public FormalTypePatternContext formalTypePattern() {
            return getRuleContext(FormalTypePatternContext.class, 0);
        }

        public TerminalNode RPAREN() {
            return getToken(MethodSignatureParser.RPAREN, 0);
        }

        public OptionalParensTypePatternContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_optionalParensTypePattern;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof MethodSignatureParserListener) ((MethodSignatureParserListener) listener).enterOptionalParensTypePattern(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof MethodSignatureParserListener) ((MethodSignatureParserListener) listener).exitOptionalParensTypePattern(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof MethodSignatureParserVisitor) return ((MethodSignatureParserVisitor<? extends T>) visitor).visitOptionalParensTypePattern(this);
            else return visitor.visitChildren(this);
        }
    }

    public final OptionalParensTypePatternContext optionalParensTypePattern() throws RecognitionException {
        OptionalParensTypePatternContext _localctx = new OptionalParensTypePatternContext(_ctx, getState());
        enterRule(_localctx, 10, RULE_optionalParensTypePattern);
        try {
            setState(99);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case LPAREN:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(94);
                    match(LPAREN);
                    setState(95);
                    formalTypePattern(0);
                    setState(96);
                    match(RPAREN);
                }
                    break;
                case DOT:
                case BANG:
                case WILDCARD:
                case DOTDOT:
                case Identifier:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(98);
                    formalTypePattern(0);
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

    public static class TargetTypePatternContext extends ParserRuleContext {
        public ClassNameOrInterfaceContext classNameOrInterface() {
            return getRuleContext(ClassNameOrInterfaceContext.class, 0);
        }

        public TerminalNode BANG() {
            return getToken(MethodSignatureParser.BANG, 0);
        }

        public List<TargetTypePatternContext> targetTypePattern() {
            return getRuleContexts(TargetTypePatternContext.class);
        }

        public TargetTypePatternContext targetTypePattern(int i) {
            return getRuleContext(TargetTypePatternContext.class, i);
        }

        public TerminalNode AND() {
            return getToken(MethodSignatureParser.AND, 0);
        }

        public TerminalNode OR() {
            return getToken(MethodSignatureParser.OR, 0);
        }

        public TargetTypePatternContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_targetTypePattern;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof MethodSignatureParserListener) ((MethodSignatureParserListener) listener).enterTargetTypePattern(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof MethodSignatureParserListener) ((MethodSignatureParserListener) listener).exitTargetTypePattern(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof MethodSignatureParserVisitor) return ((MethodSignatureParserVisitor<? extends T>) visitor).visitTargetTypePattern(this);
            else return visitor.visitChildren(this);
        }
    }

    public final TargetTypePatternContext targetTypePattern() throws RecognitionException {
        return targetTypePattern(0);
    }

    private TargetTypePatternContext targetTypePattern(int _p) throws RecognitionException {
        ParserRuleContext _parentctx = _ctx;
        int _parentState = getState();
        TargetTypePatternContext _localctx = new TargetTypePatternContext(_ctx, _parentState);
        TargetTypePatternContext _prevctx = _localctx;
        int _startState = 12;
        enterRecursionRule(_localctx, 12, RULE_targetTypePattern, _p);
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(105);
                _errHandler.sync(this);
                switch (_input.LA(1)) {
                    case DOT:
                    case WILDCARD:
                    case DOTDOT:
                    case Identifier:
                    {
                        setState(102);
                        classNameOrInterface();
                    }
                        break;
                    case BANG:
                    {
                        setState(103);
                        match(BANG);
                        setState(104);
                        targetTypePattern(3);
                    }
                        break;
                    default:
                        throw new NoViableAltException(this);
                }
                _ctx.stop = _input.LT(-1);
                setState(115);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 14, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        if (_parseListeners != null) triggerExitRuleEvent();
                        _prevctx = _localctx;
                        {
                            setState(113);
                            _errHandler.sync(this);
                            switch (getInterpreter().adaptivePredict(_input, 13, _ctx)) {
                                case 1:
                                {
                                    _localctx = new TargetTypePatternContext(_parentctx, _parentState);
                                    pushNewRecursionContext(_localctx, _startState, RULE_targetTypePattern);
                                    setState(107);
                                    if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
                                    setState(108);
                                    match(AND);
                                    setState(109);
                                    targetTypePattern(3);
                                }
                                    break;
                                case 2:
                                {
                                    _localctx = new TargetTypePatternContext(_parentctx, _parentState);
                                    pushNewRecursionContext(_localctx, _startState, RULE_targetTypePattern);
                                    setState(110);
                                    if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
                                    setState(111);
                                    match(OR);
                                    setState(112);
                                    targetTypePattern(2);
                                }
                                    break;
                            }
                        }
                    }
                    setState(117);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 14, _ctx);
                }
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        }
        finally {
            unrollRecursionContexts(_parentctx);
        }
        return _localctx;
    }

    public static class FormalTypePatternContext extends ParserRuleContext {
        public ClassNameOrInterfaceContext classNameOrInterface() {
            return getRuleContext(ClassNameOrInterfaceContext.class, 0);
        }

        public TerminalNode BANG() {
            return getToken(MethodSignatureParser.BANG, 0);
        }

        public List<FormalTypePatternContext> formalTypePattern() {
            return getRuleContexts(FormalTypePatternContext.class);
        }

        public FormalTypePatternContext formalTypePattern(int i) {
            return getRuleContext(FormalTypePatternContext.class, i);
        }

        public TerminalNode AND() {
            return getToken(MethodSignatureParser.AND, 0);
        }

        public TerminalNode OR() {
            return getToken(MethodSignatureParser.OR, 0);
        }

        public FormalTypePatternContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_formalTypePattern;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof MethodSignatureParserListener) ((MethodSignatureParserListener) listener).enterFormalTypePattern(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof MethodSignatureParserListener) ((MethodSignatureParserListener) listener).exitFormalTypePattern(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof MethodSignatureParserVisitor) return ((MethodSignatureParserVisitor<? extends T>) visitor).visitFormalTypePattern(this);
            else return visitor.visitChildren(this);
        }
    }

    public final FormalTypePatternContext formalTypePattern() throws RecognitionException {
        return formalTypePattern(0);
    }

    private FormalTypePatternContext formalTypePattern(int _p) throws RecognitionException {
        ParserRuleContext _parentctx = _ctx;
        int _parentState = getState();
        FormalTypePatternContext _localctx = new FormalTypePatternContext(_ctx, _parentState);
        FormalTypePatternContext _prevctx = _localctx;
        int _startState = 14;
        enterRecursionRule(_localctx, 14, RULE_formalTypePattern, _p);
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(122);
                _errHandler.sync(this);
                switch (_input.LA(1)) {
                    case DOT:
                    case WILDCARD:
                    case DOTDOT:
                    case Identifier:
                    {
                        setState(119);
                        classNameOrInterface();
                    }
                        break;
                    case BANG:
                    {
                        setState(120);
                        match(BANG);
                        setState(121);
                        formalTypePattern(3);
                    }
                        break;
                    default:
                        throw new NoViableAltException(this);
                }
                _ctx.stop = _input.LT(-1);
                setState(132);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 17, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        if (_parseListeners != null) triggerExitRuleEvent();
                        _prevctx = _localctx;
                        {
                            setState(130);
                            _errHandler.sync(this);
                            switch (getInterpreter().adaptivePredict(_input, 16, _ctx)) {
                                case 1:
                                {
                                    _localctx = new FormalTypePatternContext(_parentctx, _parentState);
                                    pushNewRecursionContext(_localctx, _startState, RULE_formalTypePattern);
                                    setState(124);
                                    if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
                                    setState(125);
                                    match(AND);
                                    setState(126);
                                    formalTypePattern(3);
                                }
                                    break;
                                case 2:
                                {
                                    _localctx = new FormalTypePatternContext(_parentctx, _parentState);
                                    pushNewRecursionContext(_localctx, _startState, RULE_formalTypePattern);
                                    setState(127);
                                    if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
                                    setState(128);
                                    match(OR);
                                    setState(129);
                                    formalTypePattern(2);
                                }
                                    break;
                            }
                        }
                    }
                    setState(134);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 17, _ctx);
                }
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        }
        finally {
            unrollRecursionContexts(_parentctx);
        }
        return _localctx;
    }

    public static class ClassNameOrInterfaceContext extends ParserRuleContext {
        public List<TerminalNode> LBRACK() {
            return getTokens(MethodSignatureParser.LBRACK);
        }

        public TerminalNode LBRACK(int i) {
            return getToken(MethodSignatureParser.LBRACK, i);
        }

        public List<TerminalNode> RBRACK() {
            return getTokens(MethodSignatureParser.RBRACK);
        }

        public TerminalNode RBRACK(int i) {
            return getToken(MethodSignatureParser.RBRACK, i);
        }

        public List<TerminalNode> Identifier() {
            return getTokens(MethodSignatureParser.Identifier);
        }

        public TerminalNode Identifier(int i) {
            return getToken(MethodSignatureParser.Identifier, i);
        }

        public List<TerminalNode> WILDCARD() {
            return getTokens(MethodSignatureParser.WILDCARD);
        }

        public TerminalNode WILDCARD(int i) {
            return getToken(MethodSignatureParser.WILDCARD, i);
        }

        public List<TerminalNode> DOT() {
            return getTokens(MethodSignatureParser.DOT);
        }

        public TerminalNode DOT(int i) {
            return getToken(MethodSignatureParser.DOT, i);
        }

        public List<TerminalNode> DOTDOT() {
            return getTokens(MethodSignatureParser.DOTDOT);
        }

        public TerminalNode DOTDOT(int i) {
            return getToken(MethodSignatureParser.DOTDOT, i);
        }

        public ClassNameOrInterfaceContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_classNameOrInterface;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof MethodSignatureParserListener) ((MethodSignatureParserListener) listener).enterClassNameOrInterface(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof MethodSignatureParserListener) ((MethodSignatureParserListener) listener).exitClassNameOrInterface(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof MethodSignatureParserVisitor) return ((MethodSignatureParserVisitor<? extends T>) visitor).visitClassNameOrInterface(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ClassNameOrInterfaceContext classNameOrInterface() throws RecognitionException {
        ClassNameOrInterfaceContext _localctx = new ClassNameOrInterfaceContext(_ctx, getState());
        enterRule(_localctx, 16, RULE_classNameOrInterface);
        int _la;
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(136);
                _errHandler.sync(this);
                _alt = 1;
                do {
                    switch (_alt) {
                        case 1:
                        {
                            {
                                setState(135);
                                _la = _input.LA(1);
                                if (!((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << DOT) | (1L << WILDCARD) | (1L << DOTDOT) | (1L << Identifier))) != 0))) {
                                    _errHandler.recoverInline(this);
                                }
                                else {
                                    if (_input.LA(1) == Token.EOF) matchedEOF = true;
                                    _errHandler.reportMatch(this);
                                    consume();
                                }
                            }
                        }
                            break;
                        default:
                            throw new NoViableAltException(this);
                    }
                    setState(138);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 18, _ctx);
                } while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER);
                setState(144);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 19, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        {
                            {
                                setState(140);
                                match(LBRACK);
                                setState(141);
                                match(RBRACK);
                            }
                        }
                    }
                    setState(146);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 19, _ctx);
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

    public static class SimpleNamePatternContext extends ParserRuleContext {
        public List<TerminalNode> Identifier() {
            return getTokens(MethodSignatureParser.Identifier);
        }

        public TerminalNode Identifier(int i) {
            return getToken(MethodSignatureParser.Identifier, i);
        }

        public List<TerminalNode> WILDCARD() {
            return getTokens(MethodSignatureParser.WILDCARD);
        }

        public TerminalNode WILDCARD(int i) {
            return getToken(MethodSignatureParser.WILDCARD, i);
        }

        public TerminalNode CONSTRUCTOR() {
            return getToken(MethodSignatureParser.CONSTRUCTOR, 0);
        }

        public SimpleNamePatternContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_simpleNamePattern;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof MethodSignatureParserListener) ((MethodSignatureParserListener) listener).enterSimpleNamePattern(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof MethodSignatureParserListener) ((MethodSignatureParserListener) listener).exitSimpleNamePattern(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof MethodSignatureParserVisitor) return ((MethodSignatureParserVisitor<? extends T>) visitor).visitSimpleNamePattern(this);
            else return visitor.visitChildren(this);
        }
    }

    public final SimpleNamePatternContext simpleNamePattern() throws RecognitionException {
        SimpleNamePatternContext _localctx = new SimpleNamePatternContext(_ctx, getState());
        enterRule(_localctx, 18, RULE_simpleNamePattern);
        int _la;
        try {
            int _alt;
            setState(170);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case Identifier:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(147);
                    match(Identifier);
                    setState(152);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 20, _ctx);
                    while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                        if (_alt == 1) {
                            {
                                {
                                    setState(148);
                                    match(WILDCARD);
                                    setState(149);
                                    match(Identifier);
                                }
                            }
                        }
                        setState(154);
                        _errHandler.sync(this);
                        _alt = getInterpreter().adaptivePredict(_input, 20, _ctx);
                    }
                    setState(156);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if (_la == WILDCARD) {
                        {
                            setState(155);
                            match(WILDCARD);
                        }
                    }

                }
                    break;
                case WILDCARD:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(158);
                    match(WILDCARD);
                    setState(163);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 22, _ctx);
                    while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                        if (_alt == 1) {
                            {
                                {
                                    setState(159);
                                    match(Identifier);
                                    setState(160);
                                    match(WILDCARD);
                                }
                            }
                        }
                        setState(165);
                        _errHandler.sync(this);
                        _alt = getInterpreter().adaptivePredict(_input, 22, _ctx);
                    }
                    setState(167);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if (_la == Identifier) {
                        {
                            setState(166);
                            match(Identifier);
                        }
                    }

                }
                    break;
                case CONSTRUCTOR:
                    enterOuterAlt(_localctx, 3);
                {
                    setState(169);
                    match(CONSTRUCTOR);
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

    public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
        switch (ruleIndex) {
            case 6:
                return targetTypePattern_sempred((TargetTypePatternContext) _localctx, predIndex);
            case 7:
                return formalTypePattern_sempred((FormalTypePatternContext) _localctx, predIndex);
        }
        return true;
    }

    private boolean targetTypePattern_sempred(TargetTypePatternContext _localctx, int predIndex) {
        switch (predIndex) {
            case 0:
                return precpred(_ctx, 2);
            case 1:
                return precpred(_ctx, 1);
        }
        return true;
    }

    private boolean formalTypePattern_sempred(FormalTypePatternContext _localctx, int predIndex) {
        switch (predIndex) {
            case 2:
                return precpred(_ctx, 2);
            case 3:
                return precpred(_ctx, 1);
        }
        return true;
    }

    public static final String _serializedATN =
            "\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\22\u00af\4\2\t\2" +
                    "\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13" +
                    "\t\13\3\2\3\2\7\2\31\n\2\f\2\16\2\34\13\2\3\2\3\2\5\2 \n\2\3\2\3\2\3\2" +
                    "\3\3\3\3\5\3\'\n\3\3\3\3\3\3\4\3\4\3\4\7\4.\n\4\f\4\16\4\61\13\4\3\4\7" +
                    "\4\64\n\4\f\4\16\4\67\13\4\3\4\3\4\3\4\7\4<\n\4\f\4\16\4?\13\4\3\4\7\4" +
                    "B\n\4\f\4\16\4E\13\4\3\4\3\4\3\4\5\4J\n\4\3\5\3\5\3\6\3\6\3\6\7\6Q\n\6" +
                    "\f\6\16\6T\13\6\3\6\7\6W\n\6\f\6\16\6Z\13\6\3\6\3\6\3\6\5\6_\n\6\3\7\3" +
                    "\7\3\7\3\7\3\7\5\7f\n\7\3\b\3\b\3\b\3\b\5\bl\n\b\3\b\3\b\3\b\3\b\3\b\3" +
                    "\b\7\bt\n\b\f\b\16\bw\13\b\3\t\3\t\3\t\3\t\5\t}\n\t\3\t\3\t\3\t\3\t\3" +
                    "\t\3\t\7\t\u0085\n\t\f\t\16\t\u0088\13\t\3\n\6\n\u008b\n\n\r\n\16\n\u008c" +
                    "\3\n\3\n\7\n\u0091\n\n\f\n\16\n\u0094\13\n\3\13\3\13\3\13\7\13\u0099\n" +
                    "\13\f\13\16\13\u009c\13\13\3\13\5\13\u009f\n\13\3\13\3\13\3\13\7\13\u00a4" +
                    "\n\13\f\13\16\13\u00a7\13\13\3\13\5\13\u00aa\n\13\3\13\5\13\u00ad\n\13" +
                    "\3\13\2\4\16\20\f\2\4\6\b\n\f\16\20\22\24\2\3\6\2\t\t\13\13\17\17\22\22" +
                    "\2\u00c0\2\26\3\2\2\2\4$\3\2\2\2\6I\3\2\2\2\bK\3\2\2\2\n^\3\2\2\2\fe\3" +
                    "\2\2\2\16k\3\2\2\2\20|\3\2\2\2\22\u008a\3\2\2\2\24\u00ac\3\2\2\2\26\37" +
                    "\5\16\b\2\27\31\7\21\2\2\30\27\3\2\2\2\31\34\3\2\2\2\32\30\3\2\2\2\32" +
                    "\33\3\2\2\2\33 \3\2\2\2\34\32\3\2\2\2\35 \7\t\2\2\36 \7\20\2\2\37\32\3" +
                    "\2\2\2\37\35\3\2\2\2\37\36\3\2\2\2 !\3\2\2\2!\"\5\24\13\2\"#\5\4\3\2#" +
                    "\3\3\2\2\2$&\7\4\2\2%\'\5\6\4\2&%\3\2\2\2&\'\3\2\2\2\'(\3\2\2\2()\7\5" +
                    "\2\2)\5\3\2\2\2*\65\5\b\5\2+/\7\b\2\2,.\7\21\2\2-,\3\2\2\2.\61\3\2\2\2" +
                    "/-\3\2\2\2/\60\3\2\2\2\60\62\3\2\2\2\61/\3\2\2\2\62\64\5\n\6\2\63+\3\2" +
                    "\2\2\64\67\3\2\2\2\65\63\3\2\2\2\65\66\3\2\2\2\66J\3\2\2\2\67\65\3\2\2" +
                    "\28C\5\f\7\29=\7\b\2\2:<\7\21\2\2;:\3\2\2\2<?\3\2\2\2=;\3\2\2\2=>\3\2" +
                    "\2\2>@\3\2\2\2?=\3\2\2\2@B\5\6\4\2A9\3\2\2\2BE\3\2\2\2CA\3\2\2\2CD\3\2" +
                    "\2\2DJ\3\2\2\2EC\3\2\2\2FG\5\20\t\2GH\7\16\2\2HJ\3\2\2\2I*\3\2\2\2I8\3" +
                    "\2\2\2IF\3\2\2\2J\7\3\2\2\2KL\7\17\2\2L\t\3\2\2\2MX\5\f\7\2NR\7\b\2\2" +
                    "OQ\7\21\2\2PO\3\2\2\2QT\3\2\2\2RP\3\2\2\2RS\3\2\2\2SU\3\2\2\2TR\3\2\2" +
                    "\2UW\5\n\6\2VN\3\2\2\2WZ\3\2\2\2XV\3\2\2\2XY\3\2\2\2Y_\3\2\2\2ZX\3\2\2" +
                    "\2[\\\5\20\t\2\\]\7\16\2\2]_\3\2\2\2^M\3\2\2\2^[\3\2\2\2_\13\3\2\2\2`" +
                    "a\7\4\2\2ab\5\20\t\2bc\7\5\2\2cf\3\2\2\2df\5\20\t\2e`\3\2\2\2ed\3\2\2" +
                    "\2f\r\3\2\2\2gh\b\b\1\2hl\5\22\n\2ij\7\n\2\2jl\5\16\b\5kg\3\2\2\2ki\3" +
                    "\2\2\2lu\3\2\2\2mn\f\4\2\2no\7\f\2\2ot\5\16\b\5pq\f\3\2\2qr\7\r\2\2rt" +
                    "\5\16\b\4sm\3\2\2\2sp\3\2\2\2tw\3\2\2\2us\3\2\2\2uv\3\2\2\2v\17\3\2\2" +
                    "\2wu\3\2\2\2xy\b\t\1\2y}\5\22\n\2z{\7\n\2\2{}\5\20\t\5|x\3\2\2\2|z\3\2" +
                    "\2\2}\u0086\3\2\2\2~\177\f\4\2\2\177\u0080\7\f\2\2\u0080\u0085\5\20\t" +
                    "\5\u0081\u0082\f\3\2\2\u0082\u0083\7\r\2\2\u0083\u0085\5\20\t\4\u0084" +
                    "~\3\2\2\2\u0084\u0081\3\2\2\2\u0085\u0088\3\2\2\2\u0086\u0084\3\2\2\2" +
                    "\u0086\u0087\3\2\2\2\u0087\21\3\2\2\2\u0088\u0086\3\2\2\2\u0089\u008b" +
                    "\t\2\2\2\u008a\u0089\3\2\2\2\u008b\u008c\3\2\2\2\u008c\u008a\3\2\2\2\u008c" +
                    "\u008d\3\2\2\2\u008d\u0092\3\2\2\2\u008e\u008f\7\6\2\2\u008f\u0091\7\7" +
                    "\2\2\u0090\u008e\3\2\2\2\u0091\u0094\3\2\2\2\u0092\u0090\3\2\2\2\u0092" +
                    "\u0093\3\2\2\2\u0093\23\3\2\2\2\u0094\u0092\3\2\2\2\u0095\u009a\7\22\2" +
                    "\2\u0096\u0097\7\13\2\2\u0097\u0099\7\22\2\2\u0098\u0096\3\2\2\2\u0099" +
                    "\u009c\3\2\2\2\u009a\u0098\3\2\2\2\u009a\u009b\3\2\2\2\u009b\u009e\3\2" +
                    "\2\2\u009c\u009a\3\2\2\2\u009d\u009f\7\13\2\2\u009e\u009d\3\2\2\2\u009e" +
                    "\u009f\3\2\2\2\u009f\u00ad\3\2\2\2\u00a0\u00a5\7\13\2\2\u00a1\u00a2\7" +
                    "\22\2\2\u00a2\u00a4\7\13\2\2\u00a3\u00a1\3\2\2\2\u00a4\u00a7\3\2\2\2\u00a5" +
                    "\u00a3\3\2\2\2\u00a5\u00a6\3\2\2\2\u00a6\u00a9\3\2\2\2\u00a7\u00a5\3\2" +
                    "\2\2\u00a8\u00aa\7\22\2\2\u00a9\u00a8\3\2\2\2\u00a9\u00aa\3\2\2\2\u00aa" +
                    "\u00ad\3\2\2\2\u00ab\u00ad\7\3\2\2\u00ac\u0095\3\2\2\2\u00ac\u00a0\3\2" +
                    "\2\2\u00ac\u00ab\3\2\2\2\u00ad\25\3\2\2\2\33\32\37&/\65=CIRX^eksu|\u0084" +
                    "\u0086\u008c\u0092\u009a\u009e\u00a5\u00a9\u00ac";
    public static final ATN _ATN =
            new ATNDeserializer().deserialize(_serializedATN.toCharArray());

    static {
        _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
        for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
            _decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
        }
    }
}