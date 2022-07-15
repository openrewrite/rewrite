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
// Generated from /Users/yoshi/Development/Repos/openrewrite/rewrite/rewrite-json/src/main/antlr/JsonPathParser.g4 by ANTLR 4.9.3
package org.openrewrite.json.internal.grammar;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class JsonPathParser extends Parser {
    static {
        RuntimeMetaData.checkVersion("4.9.3", RuntimeMetaData.VERSION);
    }

    protected static final DFA[] _decisionToDFA;
    protected static final PredictionContextCache _sharedContextCache =
            new PredictionContextCache();
    public static final int
            WS = 1, UTF_8_BOM = 2, MATCHES_REGEX_OPEN = 3, LBRACE = 4, RBRACE = 5, LBRACK = 6,
            RBRACK = 7, LPAREN = 8, RPAREN = 9, AT = 10, DOT = 11, DOT_DOT = 12, ROOT = 13, WILDCARD = 14,
            COLON = 15, QUESTION = 16, CONTAINS = 17, Identifier = 18, StringLiteral = 19, PositiveNumber = 20,
            NegativeNumber = 21, NumericLiteral = 22, COMMA = 23, TICK = 24, QUOTE = 25, MATCHES = 26,
            LOGICAL_OPERATOR = 27, AND = 28, OR = 29, EQUALITY_OPERATOR = 30, EQ = 31, NE = 32,
            TRUE = 33, FALSE = 34, NULL = 35, MATCHES_REGEX_CLOSE = 36, S = 37, REGEX = 38;
    public static final int
            RULE_jsonPath = 0, RULE_expression = 1, RULE_dotOperator = 2, RULE_recursiveDecent = 3,
            RULE_bracketOperator = 4, RULE_filter = 5, RULE_filterExpression = 6,
            RULE_binaryExpression = 7, RULE_containsExpression = 8, RULE_regexExpression = 9,
            RULE_unaryExpression = 10, RULE_literalExpression = 11, RULE_property = 12,
            RULE_wildcard = 13, RULE_slice = 14, RULE_start = 15, RULE_end = 16, RULE_indexes = 17;

    private static String[] makeRuleNames() {
        return new String[]{
                "jsonPath", "expression", "dotOperator", "recursiveDecent", "bracketOperator",
                "filter", "filterExpression", "binaryExpression", "containsExpression",
                "regexExpression", "unaryExpression", "literalExpression", "property",
                "wildcard", "slice", "start", "end", "indexes"
        };
    }
    public static final String[] ruleNames = makeRuleNames();

    private static String[] makeLiteralNames() {
        return new String[]{
                null, null, "'\uFEFF'", null, "'{'", "'}'", "'['", "']'", "'('", "')'",
                "'@'", "'.'", "'..'", "'$'", "'*'", "':'", "'?'", "'contains'", null,
                null, null, null, null, "','", "'''", "'\"'", "'=~'", null, "'&&'", "'||'",
                null, "'=='", "'!='", "'true'", "'false'", "'null'"
        };
    }
    private static final String[] _LITERAL_NAMES = makeLiteralNames();

    private static String[] makeSymbolicNames() {
        return new String[]{
                null, "WS", "UTF_8_BOM", "MATCHES_REGEX_OPEN", "LBRACE", "RBRACE", "LBRACK",
                "RBRACK", "LPAREN", "RPAREN", "AT", "DOT", "DOT_DOT", "ROOT", "WILDCARD",
                "COLON", "QUESTION", "CONTAINS", "Identifier", "StringLiteral", "PositiveNumber",
                "NegativeNumber", "NumericLiteral", "COMMA", "TICK", "QUOTE", "MATCHES",
                "LOGICAL_OPERATOR", "AND", "OR", "EQUALITY_OPERATOR", "EQ", "NE", "TRUE",
                "FALSE", "NULL", "MATCHES_REGEX_CLOSE", "S", "REGEX"
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
        return "JsonPathParser.g4";
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

    public JsonPathParser(TokenStream input) {
        super(input);
        _interp = new ParserATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
    }

    public static class JsonPathContext extends ParserRuleContext {
        public TerminalNode ROOT() {
            return getToken(JsonPathParser.ROOT, 0);
        }

        public List<ExpressionContext> expression() {
            return getRuleContexts(ExpressionContext.class);
        }

        public ExpressionContext expression(int i) {
            return getRuleContext(ExpressionContext.class, i);
        }

        public JsonPathContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_jsonPath;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).enterJsonPath(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).exitJsonPath(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof JsonPathParserVisitor) return ((JsonPathParserVisitor<? extends T>) visitor).visitJsonPath(this);
            else return visitor.visitChildren(this);
        }
    }

    public final JsonPathContext jsonPath() throws RecognitionException {
        JsonPathContext _localctx = new JsonPathContext(_ctx, getState());
        enterRule(_localctx, 0, RULE_jsonPath);
        int _la;
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(37);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == ROOT) {
                    {
                        setState(36);
                        match(ROOT);
                    }
                }

                setState(40);
                _errHandler.sync(this);
                _alt = 1;
                do {
                    switch (_alt) {
                        case 1:
                        {
                            {
                                setState(39);
                                expression();
                            }
                        }
                            break;
                        default:
                            throw new NoViableAltException(this);
                    }
                    setState(42);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 1, _ctx);
                } while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER);
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

    public static class ExpressionContext extends ParserRuleContext {
        public TerminalNode DOT() {
            return getToken(JsonPathParser.DOT, 0);
        }

        public DotOperatorContext dotOperator() {
            return getRuleContext(DotOperatorContext.class, 0);
        }

        public RecursiveDecentContext recursiveDecent() {
            return getRuleContext(RecursiveDecentContext.class, 0);
        }

        public BracketOperatorContext bracketOperator() {
            return getRuleContext(BracketOperatorContext.class, 0);
        }

        public ExpressionContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_expression;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).enterExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).exitExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof JsonPathParserVisitor) return ((JsonPathParserVisitor<? extends T>) visitor).visitExpression(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ExpressionContext expression() throws RecognitionException {
        ExpressionContext _localctx = new ExpressionContext(_ctx, getState());
        enterRule(_localctx, 2, RULE_expression);
        try {
            setState(48);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case DOT:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(44);
                    match(DOT);
                    setState(45);
                    dotOperator();
                }
                    break;
                case DOT_DOT:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(46);
                    recursiveDecent();
                }
                    break;
                case LBRACK:
                    enterOuterAlt(_localctx, 3);
                {
                    setState(47);
                    bracketOperator();
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

    public static class DotOperatorContext extends ParserRuleContext {
        public BracketOperatorContext bracketOperator() {
            return getRuleContext(BracketOperatorContext.class, 0);
        }

        public PropertyContext property() {
            return getRuleContext(PropertyContext.class, 0);
        }

        public WildcardContext wildcard() {
            return getRuleContext(WildcardContext.class, 0);
        }

        public DotOperatorContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_dotOperator;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).enterDotOperator(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).exitDotOperator(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof JsonPathParserVisitor) return ((JsonPathParserVisitor<? extends T>) visitor).visitDotOperator(this);
            else return visitor.visitChildren(this);
        }
    }

    public final DotOperatorContext dotOperator() throws RecognitionException {
        DotOperatorContext _localctx = new DotOperatorContext(_ctx, getState());
        enterRule(_localctx, 4, RULE_dotOperator);
        try {
            setState(53);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case LBRACK:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(50);
                    bracketOperator();
                }
                    break;
                case Identifier:
                case StringLiteral:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(51);
                    property();
                }
                    break;
                case WILDCARD:
                    enterOuterAlt(_localctx, 3);
                {
                    setState(52);
                    wildcard();
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

    public static class RecursiveDecentContext extends ParserRuleContext {
        public TerminalNode DOT_DOT() {
            return getToken(JsonPathParser.DOT_DOT, 0);
        }

        public DotOperatorContext dotOperator() {
            return getRuleContext(DotOperatorContext.class, 0);
        }

        public RecursiveDecentContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_recursiveDecent;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).enterRecursiveDecent(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).exitRecursiveDecent(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof JsonPathParserVisitor) return ((JsonPathParserVisitor<? extends T>) visitor).visitRecursiveDecent(this);
            else return visitor.visitChildren(this);
        }
    }

    public final RecursiveDecentContext recursiveDecent() throws RecognitionException {
        RecursiveDecentContext _localctx = new RecursiveDecentContext(_ctx, getState());
        enterRule(_localctx, 6, RULE_recursiveDecent);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(55);
                match(DOT_DOT);
                setState(56);
                dotOperator();
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

    public static class BracketOperatorContext extends ParserRuleContext {
        public TerminalNode LBRACK() {
            return getToken(JsonPathParser.LBRACK, 0);
        }

        public TerminalNode RBRACK() {
            return getToken(JsonPathParser.RBRACK, 0);
        }

        public FilterContext filter() {
            return getRuleContext(FilterContext.class, 0);
        }

        public SliceContext slice() {
            return getRuleContext(SliceContext.class, 0);
        }

        public IndexesContext indexes() {
            return getRuleContext(IndexesContext.class, 0);
        }

        public List<PropertyContext> property() {
            return getRuleContexts(PropertyContext.class);
        }

        public PropertyContext property(int i) {
            return getRuleContext(PropertyContext.class, i);
        }

        public BracketOperatorContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_bracketOperator;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).enterBracketOperator(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).exitBracketOperator(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof JsonPathParserVisitor) return ((JsonPathParserVisitor<? extends T>) visitor).visitBracketOperator(this);
            else return visitor.visitChildren(this);
        }
    }

    public final BracketOperatorContext bracketOperator() throws RecognitionException {
        BracketOperatorContext _localctx = new BracketOperatorContext(_ctx, getState());
        enterRule(_localctx, 8, RULE_bracketOperator);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(58);
                match(LBRACK);
                setState(67);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 5, _ctx)) {
                    case 1:
                    {
                        setState(59);
                        filter();
                    }
                        break;
                    case 2:
                    {
                        setState(60);
                        slice();
                    }
                        break;
                    case 3:
                    {
                        setState(61);
                        indexes();
                    }
                        break;
                    case 4:
                    {
                        setState(63);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        do {
                            {
                                {
                                    setState(62);
                                    property();
                                }
                            }
                            setState(65);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        } while (_la == Identifier || _la == StringLiteral);
                    }
                        break;
                }
                setState(69);
                match(RBRACK);
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

    public static class FilterContext extends ParserRuleContext {
        public TerminalNode QUESTION() {
            return getToken(JsonPathParser.QUESTION, 0);
        }

        public TerminalNode LPAREN() {
            return getToken(JsonPathParser.LPAREN, 0);
        }

        public TerminalNode RPAREN() {
            return getToken(JsonPathParser.RPAREN, 0);
        }

        public List<FilterExpressionContext> filterExpression() {
            return getRuleContexts(FilterExpressionContext.class);
        }

        public FilterExpressionContext filterExpression(int i) {
            return getRuleContext(FilterExpressionContext.class, i);
        }

        public FilterContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_filter;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).enterFilter(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).exitFilter(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof JsonPathParserVisitor) return ((JsonPathParserVisitor<? extends T>) visitor).visitFilter(this);
            else return visitor.visitChildren(this);
        }
    }

    public final FilterContext filter() throws RecognitionException {
        FilterContext _localctx = new FilterContext(_ctx, getState());
        enterRule(_localctx, 10, RULE_filter);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(71);
                match(QUESTION);
                setState(72);
                match(LPAREN);
                setState(74);
                _errHandler.sync(this);
                _la = _input.LA(1);
                do {
                    {
                        {
                            setState(73);
                            filterExpression();
                        }
                    }
                    setState(76);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                } while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << LBRACK) | (1L << AT) | (1L << DOT) | (1L << DOT_DOT) | (1L << ROOT) | (1L << StringLiteral) | (1L << PositiveNumber) | (1L << NegativeNumber) | (1L << NumericLiteral) | (1L << TRUE) | (1L << FALSE) | (1L << NULL))) != 0));
                setState(78);
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

    public static class FilterExpressionContext extends ParserRuleContext {
        public BinaryExpressionContext binaryExpression() {
            return getRuleContext(BinaryExpressionContext.class, 0);
        }

        public RegexExpressionContext regexExpression() {
            return getRuleContext(RegexExpressionContext.class, 0);
        }

        public ContainsExpressionContext containsExpression() {
            return getRuleContext(ContainsExpressionContext.class, 0);
        }

        public UnaryExpressionContext unaryExpression() {
            return getRuleContext(UnaryExpressionContext.class, 0);
        }

        public FilterExpressionContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_filterExpression;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).enterFilterExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).exitFilterExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof JsonPathParserVisitor) return ((JsonPathParserVisitor<? extends T>) visitor).visitFilterExpression(this);
            else return visitor.visitChildren(this);
        }
    }

    public final FilterExpressionContext filterExpression() throws RecognitionException {
        FilterExpressionContext _localctx = new FilterExpressionContext(_ctx, getState());
        enterRule(_localctx, 12, RULE_filterExpression);
        try {
            setState(84);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 7, _ctx)) {
                case 1:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(80);
                    binaryExpression(0);
                }
                    break;
                case 2:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(81);
                    regexExpression();
                }
                    break;
                case 3:
                    enterOuterAlt(_localctx, 3);
                {
                    setState(82);
                    containsExpression();
                }
                    break;
                case 4:
                    enterOuterAlt(_localctx, 4);
                {
                    setState(83);
                    unaryExpression();
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

    public static class BinaryExpressionContext extends ParserRuleContext {
        public List<RegexExpressionContext> regexExpression() {
            return getRuleContexts(RegexExpressionContext.class);
        }

        public RegexExpressionContext regexExpression(int i) {
            return getRuleContext(RegexExpressionContext.class, i);
        }

        public TerminalNode LOGICAL_OPERATOR() {
            return getToken(JsonPathParser.LOGICAL_OPERATOR, 0);
        }

        public List<BinaryExpressionContext> binaryExpression() {
            return getRuleContexts(BinaryExpressionContext.class);
        }

        public BinaryExpressionContext binaryExpression(int i) {
            return getRuleContext(BinaryExpressionContext.class, i);
        }

        public List<ContainsExpressionContext> containsExpression() {
            return getRuleContexts(ContainsExpressionContext.class);
        }

        public ContainsExpressionContext containsExpression(int i) {
            return getRuleContext(ContainsExpressionContext.class, i);
        }

        public UnaryExpressionContext unaryExpression() {
            return getRuleContext(UnaryExpressionContext.class, 0);
        }

        public TerminalNode EQUALITY_OPERATOR() {
            return getToken(JsonPathParser.EQUALITY_OPERATOR, 0);
        }

        public LiteralExpressionContext literalExpression() {
            return getRuleContext(LiteralExpressionContext.class, 0);
        }

        public BinaryExpressionContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_binaryExpression;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).enterBinaryExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).exitBinaryExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof JsonPathParserVisitor) return ((JsonPathParserVisitor<? extends T>) visitor).visitBinaryExpression(this);
            else return visitor.visitChildren(this);
        }
    }

    public final BinaryExpressionContext binaryExpression() throws RecognitionException {
        return binaryExpression(0);
    }

    private BinaryExpressionContext binaryExpression(int _p) throws RecognitionException {
        ParserRuleContext _parentctx = _ctx;
        int _parentState = getState();
        BinaryExpressionContext _localctx = new BinaryExpressionContext(_ctx, _parentState);
        BinaryExpressionContext _prevctx = _localctx;
        int _startState = 14;
        enterRecursionRule(_localctx, 14, RULE_binaryExpression, _p);
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(119);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 8, _ctx)) {
                    case 1:
                    {
                        setState(87);
                        regexExpression();
                        setState(88);
                        match(LOGICAL_OPERATOR);
                        setState(89);
                        regexExpression();
                    }
                        break;
                    case 2:
                    {
                        setState(91);
                        regexExpression();
                        setState(92);
                        match(LOGICAL_OPERATOR);
                        setState(93);
                        binaryExpression(7);
                    }
                        break;
                    case 3:
                    {
                        setState(95);
                        regexExpression();
                        setState(96);
                        match(LOGICAL_OPERATOR);
                        setState(97);
                        containsExpression();
                    }
                        break;
                    case 4:
                    {
                        setState(99);
                        containsExpression();
                        setState(100);
                        match(LOGICAL_OPERATOR);
                        setState(101);
                        containsExpression();
                    }
                        break;
                    case 5:
                    {
                        setState(103);
                        containsExpression();
                        setState(104);
                        match(LOGICAL_OPERATOR);
                        setState(105);
                        binaryExpression(4);
                    }
                        break;
                    case 6:
                    {
                        setState(107);
                        containsExpression();
                        setState(108);
                        match(LOGICAL_OPERATOR);
                        setState(109);
                        regexExpression();
                    }
                        break;
                    case 7:
                    {
                        setState(111);
                        unaryExpression();
                        setState(112);
                        match(EQUALITY_OPERATOR);
                        setState(113);
                        literalExpression();
                    }
                        break;
                    case 8:
                    {
                        setState(115);
                        literalExpression();
                        setState(116);
                        match(EQUALITY_OPERATOR);
                        setState(117);
                        unaryExpression();
                    }
                        break;
                }
                _ctx.stop = _input.LT(-1);
                setState(132);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 10, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        if (_parseListeners != null) triggerExitRuleEvent();
                        _prevctx = _localctx;
                        {
                            setState(130);
                            _errHandler.sync(this);
                            switch (getInterpreter().adaptivePredict(_input, 9, _ctx)) {
                                case 1:
                                {
                                    _localctx = new BinaryExpressionContext(_parentctx, _parentState);
                                    pushNewRecursionContext(_localctx, _startState, RULE_binaryExpression);
                                    setState(121);
                                    if (!(precpred(_ctx, 11))) throw new FailedPredicateException(this, "precpred(_ctx, 11)");
                                    setState(122);
                                    match(LOGICAL_OPERATOR);
                                    setState(123);
                                    binaryExpression(12);
                                }
                                    break;
                                case 2:
                                {
                                    _localctx = new BinaryExpressionContext(_parentctx, _parentState);
                                    pushNewRecursionContext(_localctx, _startState, RULE_binaryExpression);
                                    setState(124);
                                    if (!(precpred(_ctx, 10))) throw new FailedPredicateException(this, "precpred(_ctx, 10)");
                                    setState(125);
                                    match(LOGICAL_OPERATOR);
                                    setState(126);
                                    regexExpression();
                                }
                                    break;
                                case 3:
                                {
                                    _localctx = new BinaryExpressionContext(_parentctx, _parentState);
                                    pushNewRecursionContext(_localctx, _startState, RULE_binaryExpression);
                                    setState(127);
                                    if (!(precpred(_ctx, 9))) throw new FailedPredicateException(this, "precpred(_ctx, 9)");
                                    setState(128);
                                    match(LOGICAL_OPERATOR);
                                    setState(129);
                                    containsExpression();
                                }
                                    break;
                            }
                        }
                    }
                    setState(134);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 10, _ctx);
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

    public static class ContainsExpressionContext extends ParserRuleContext {
        public UnaryExpressionContext unaryExpression() {
            return getRuleContext(UnaryExpressionContext.class, 0);
        }

        public TerminalNode CONTAINS() {
            return getToken(JsonPathParser.CONTAINS, 0);
        }

        public LiteralExpressionContext literalExpression() {
            return getRuleContext(LiteralExpressionContext.class, 0);
        }

        public ContainsExpressionContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_containsExpression;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).enterContainsExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).exitContainsExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof JsonPathParserVisitor) return ((JsonPathParserVisitor<? extends T>) visitor).visitContainsExpression(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ContainsExpressionContext containsExpression() throws RecognitionException {
        ContainsExpressionContext _localctx = new ContainsExpressionContext(_ctx, getState());
        enterRule(_localctx, 16, RULE_containsExpression);
        try {
            setState(143);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case LBRACK:
                case AT:
                case DOT:
                case DOT_DOT:
                case ROOT:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(135);
                    unaryExpression();
                    setState(136);
                    match(CONTAINS);
                    setState(137);
                    literalExpression();
                }
                    break;
                case StringLiteral:
                case PositiveNumber:
                case NegativeNumber:
                case NumericLiteral:
                case TRUE:
                case FALSE:
                case NULL:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(139);
                    literalExpression();
                    setState(140);
                    match(CONTAINS);
                    setState(141);
                    unaryExpression();
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

    public static class RegexExpressionContext extends ParserRuleContext {
        public UnaryExpressionContext unaryExpression() {
            return getRuleContext(UnaryExpressionContext.class, 0);
        }

        public TerminalNode MATCHES_REGEX_OPEN() {
            return getToken(JsonPathParser.MATCHES_REGEX_OPEN, 0);
        }

        public TerminalNode REGEX() {
            return getToken(JsonPathParser.REGEX, 0);
        }

        public TerminalNode MATCHES_REGEX_CLOSE() {
            return getToken(JsonPathParser.MATCHES_REGEX_CLOSE, 0);
        }

        public RegexExpressionContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_regexExpression;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).enterRegexExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).exitRegexExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof JsonPathParserVisitor) return ((JsonPathParserVisitor<? extends T>) visitor).visitRegexExpression(this);
            else return visitor.visitChildren(this);
        }
    }

    public final RegexExpressionContext regexExpression() throws RecognitionException {
        RegexExpressionContext _localctx = new RegexExpressionContext(_ctx, getState());
        enterRule(_localctx, 18, RULE_regexExpression);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(145);
                unaryExpression();
                setState(146);
                match(MATCHES_REGEX_OPEN);
                setState(147);
                match(REGEX);
                setState(148);
                match(MATCHES_REGEX_CLOSE);
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

    public static class UnaryExpressionContext extends ParserRuleContext {
        public TerminalNode AT() {
            return getToken(JsonPathParser.AT, 0);
        }

        public TerminalNode DOT() {
            return getToken(JsonPathParser.DOT, 0);
        }

        public TerminalNode Identifier() {
            return getToken(JsonPathParser.Identifier, 0);
        }

        public TerminalNode LBRACK() {
            return getToken(JsonPathParser.LBRACK, 0);
        }

        public TerminalNode StringLiteral() {
            return getToken(JsonPathParser.StringLiteral, 0);
        }

        public TerminalNode RBRACK() {
            return getToken(JsonPathParser.RBRACK, 0);
        }

        public JsonPathContext jsonPath() {
            return getRuleContext(JsonPathContext.class, 0);
        }

        public UnaryExpressionContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_unaryExpression;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).enterUnaryExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).exitUnaryExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof JsonPathParserVisitor) return ((JsonPathParserVisitor<? extends T>) visitor).visitUnaryExpression(this);
            else return visitor.visitChildren(this);
        }
    }

    public final UnaryExpressionContext unaryExpression() throws RecognitionException {
        UnaryExpressionContext _localctx = new UnaryExpressionContext(_ctx, getState());
        enterRule(_localctx, 20, RULE_unaryExpression);
        int _la;
        try {
            setState(162);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case AT:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(150);
                    match(AT);
                    setState(159);
                    _errHandler.sync(this);
                    switch (getInterpreter().adaptivePredict(_input, 13, _ctx)) {
                        case 1:
                        {
                            setState(151);
                            match(DOT);
                            setState(152);
                            match(Identifier);
                        }
                            break;
                        case 2:
                        {
                            setState(154);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                            if (_la == DOT) {
                                {
                                    setState(153);
                                    match(DOT);
                                }
                            }

                            setState(156);
                            match(LBRACK);
                            setState(157);
                            match(StringLiteral);
                            setState(158);
                            match(RBRACK);
                        }
                            break;
                    }
                }
                    break;
                case LBRACK:
                case DOT:
                case DOT_DOT:
                case ROOT:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(161);
                    jsonPath();
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

    public static class LiteralExpressionContext extends ParserRuleContext {
        public TerminalNode StringLiteral() {
            return getToken(JsonPathParser.StringLiteral, 0);
        }

        public TerminalNode PositiveNumber() {
            return getToken(JsonPathParser.PositiveNumber, 0);
        }

        public TerminalNode NegativeNumber() {
            return getToken(JsonPathParser.NegativeNumber, 0);
        }

        public TerminalNode NumericLiteral() {
            return getToken(JsonPathParser.NumericLiteral, 0);
        }

        public TerminalNode TRUE() {
            return getToken(JsonPathParser.TRUE, 0);
        }

        public TerminalNode FALSE() {
            return getToken(JsonPathParser.FALSE, 0);
        }

        public TerminalNode NULL() {
            return getToken(JsonPathParser.NULL, 0);
        }

        public LiteralExpressionContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_literalExpression;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).enterLiteralExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).exitLiteralExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof JsonPathParserVisitor) return ((JsonPathParserVisitor<? extends T>) visitor).visitLiteralExpression(this);
            else return visitor.visitChildren(this);
        }
    }

    public final LiteralExpressionContext literalExpression() throws RecognitionException {
        LiteralExpressionContext _localctx = new LiteralExpressionContext(_ctx, getState());
        enterRule(_localctx, 22, RULE_literalExpression);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(164);
                _la = _input.LA(1);
                if (!((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << StringLiteral) | (1L << PositiveNumber) | (1L << NegativeNumber) | (1L << NumericLiteral) | (1L << TRUE) | (1L << FALSE) | (1L << NULL))) != 0))) {
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

    public static class PropertyContext extends ParserRuleContext {
        public TerminalNode StringLiteral() {
            return getToken(JsonPathParser.StringLiteral, 0);
        }

        public TerminalNode Identifier() {
            return getToken(JsonPathParser.Identifier, 0);
        }

        public PropertyContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_property;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).enterProperty(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).exitProperty(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof JsonPathParserVisitor) return ((JsonPathParserVisitor<? extends T>) visitor).visitProperty(this);
            else return visitor.visitChildren(this);
        }
    }

    public final PropertyContext property() throws RecognitionException {
        PropertyContext _localctx = new PropertyContext(_ctx, getState());
        enterRule(_localctx, 24, RULE_property);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(166);
                _la = _input.LA(1);
                if (!(_la == Identifier || _la == StringLiteral)) {
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

    public static class WildcardContext extends ParserRuleContext {
        public TerminalNode WILDCARD() {
            return getToken(JsonPathParser.WILDCARD, 0);
        }

        public WildcardContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_wildcard;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).enterWildcard(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).exitWildcard(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof JsonPathParserVisitor) return ((JsonPathParserVisitor<? extends T>) visitor).visitWildcard(this);
            else return visitor.visitChildren(this);
        }
    }

    public final WildcardContext wildcard() throws RecognitionException {
        WildcardContext _localctx = new WildcardContext(_ctx, getState());
        enterRule(_localctx, 26, RULE_wildcard);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(168);
                match(WILDCARD);
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

    public static class SliceContext extends ParserRuleContext {
        public StartContext start() {
            return getRuleContext(StartContext.class, 0);
        }

        public TerminalNode COLON() {
            return getToken(JsonPathParser.COLON, 0);
        }

        public EndContext end() {
            return getRuleContext(EndContext.class, 0);
        }

        public TerminalNode PositiveNumber() {
            return getToken(JsonPathParser.PositiveNumber, 0);
        }

        public TerminalNode NegativeNumber() {
            return getToken(JsonPathParser.NegativeNumber, 0);
        }

        public WildcardContext wildcard() {
            return getRuleContext(WildcardContext.class, 0);
        }

        public SliceContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_slice;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).enterSlice(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).exitSlice(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof JsonPathParserVisitor) return ((JsonPathParserVisitor<? extends T>) visitor).visitSlice(this);
            else return visitor.visitChildren(this);
        }
    }

    public final SliceContext slice() throws RecognitionException {
        SliceContext _localctx = new SliceContext(_ctx, getState());
        enterRule(_localctx, 28, RULE_slice);
        int _la;
        try {
            setState(180);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case PositiveNumber:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(170);
                    start();
                    setState(171);
                    match(COLON);
                    setState(173);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    if (_la == PositiveNumber) {
                        {
                            setState(172);
                            end();
                        }
                    }

                }
                    break;
                case COLON:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(175);
                    match(COLON);
                    setState(176);
                    match(PositiveNumber);
                }
                    break;
                case NegativeNumber:
                    enterOuterAlt(_localctx, 3);
                {
                    setState(177);
                    match(NegativeNumber);
                    setState(178);
                    match(COLON);
                }
                    break;
                case WILDCARD:
                    enterOuterAlt(_localctx, 4);
                {
                    setState(179);
                    wildcard();
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

    public static class StartContext extends ParserRuleContext {
        public TerminalNode PositiveNumber() {
            return getToken(JsonPathParser.PositiveNumber, 0);
        }

        public StartContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_start;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).enterStart(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).exitStart(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof JsonPathParserVisitor) return ((JsonPathParserVisitor<? extends T>) visitor).visitStart(this);
            else return visitor.visitChildren(this);
        }
    }

    public final StartContext start() throws RecognitionException {
        StartContext _localctx = new StartContext(_ctx, getState());
        enterRule(_localctx, 30, RULE_start);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(182);
                match(PositiveNumber);
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

    public static class EndContext extends ParserRuleContext {
        public TerminalNode PositiveNumber() {
            return getToken(JsonPathParser.PositiveNumber, 0);
        }

        public EndContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_end;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).enterEnd(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).exitEnd(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof JsonPathParserVisitor) return ((JsonPathParserVisitor<? extends T>) visitor).visitEnd(this);
            else return visitor.visitChildren(this);
        }
    }

    public final EndContext end() throws RecognitionException {
        EndContext _localctx = new EndContext(_ctx, getState());
        enterRule(_localctx, 32, RULE_end);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(184);
                match(PositiveNumber);
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

    public static class IndexesContext extends ParserRuleContext {
        public List<TerminalNode> PositiveNumber() {
            return getTokens(JsonPathParser.PositiveNumber);
        }

        public TerminalNode PositiveNumber(int i) {
            return getToken(JsonPathParser.PositiveNumber, i);
        }

        public IndexesContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_indexes;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).enterIndexes(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof JsonPathParserListener) ((JsonPathParserListener) listener).exitIndexes(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof JsonPathParserVisitor) return ((JsonPathParserVisitor<? extends T>) visitor).visitIndexes(this);
            else return visitor.visitChildren(this);
        }
    }

    public final IndexesContext indexes() throws RecognitionException {
        IndexesContext _localctx = new IndexesContext(_ctx, getState());
        enterRule(_localctx, 34, RULE_indexes);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(187);
                _errHandler.sync(this);
                _la = _input.LA(1);
                do {
                    {
                        {
                            setState(186);
                            match(PositiveNumber);
                        }
                    }
                    setState(189);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                } while (_la == PositiveNumber);
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
            case 7:
                return binaryExpression_sempred((BinaryExpressionContext) _localctx, predIndex);
        }
        return true;
    }

    private boolean binaryExpression_sempred(BinaryExpressionContext _localctx, int predIndex) {
        switch (predIndex) {
            case 0:
                return precpred(_ctx, 11);
            case 1:
                return precpred(_ctx, 10);
            case 2:
                return precpred(_ctx, 9);
        }
        return true;
    }

    public static final String _serializedATN =
            "\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3(\u00c2\4\2\t\2\4" +
                    "\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t" +
                    "\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22" +
                    "\4\23\t\23\3\2\5\2(\n\2\3\2\6\2+\n\2\r\2\16\2,\3\3\3\3\3\3\3\3\5\3\63" +
                    "\n\3\3\4\3\4\3\4\5\48\n\4\3\5\3\5\3\5\3\6\3\6\3\6\3\6\3\6\6\6B\n\6\r\6" +
                    "\16\6C\5\6F\n\6\3\6\3\6\3\7\3\7\3\7\6\7M\n\7\r\7\16\7N\3\7\3\7\3\b\3\b" +
                    "\3\b\3\b\5\bW\n\b\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t" +
                    "\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3" +
                    "\t\3\t\3\t\5\tz\n\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\7\t\u0085\n\t" +
                    "\f\t\16\t\u0088\13\t\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\5\n\u0092\n\n\3\13" +
                    "\3\13\3\13\3\13\3\13\3\f\3\f\3\f\3\f\5\f\u009d\n\f\3\f\3\f\3\f\5\f\u00a2" +
                    "\n\f\3\f\5\f\u00a5\n\f\3\r\3\r\3\16\3\16\3\17\3\17\3\20\3\20\3\20\5\20" +
                    "\u00b0\n\20\3\20\3\20\3\20\3\20\3\20\5\20\u00b7\n\20\3\21\3\21\3\22\3" +
                    "\22\3\23\6\23\u00be\n\23\r\23\16\23\u00bf\3\23\2\3\20\24\2\4\6\b\n\f\16" +
                    "\20\22\24\26\30\32\34\36 \"$\2\4\4\2\25\30#%\3\2\24\25\2\u00d1\2\'\3\2" +
                    "\2\2\4\62\3\2\2\2\6\67\3\2\2\2\b9\3\2\2\2\n<\3\2\2\2\fI\3\2\2\2\16V\3" +
                    "\2\2\2\20y\3\2\2\2\22\u0091\3\2\2\2\24\u0093\3\2\2\2\26\u00a4\3\2\2\2" +
                    "\30\u00a6\3\2\2\2\32\u00a8\3\2\2\2\34\u00aa\3\2\2\2\36\u00b6\3\2\2\2 " +
                    "\u00b8\3\2\2\2\"\u00ba\3\2\2\2$\u00bd\3\2\2\2&(\7\17\2\2\'&\3\2\2\2\'" +
                    "(\3\2\2\2(*\3\2\2\2)+\5\4\3\2*)\3\2\2\2+,\3\2\2\2,*\3\2\2\2,-\3\2\2\2" +
                    "-\3\3\2\2\2./\7\r\2\2/\63\5\6\4\2\60\63\5\b\5\2\61\63\5\n\6\2\62.\3\2" +
                    "\2\2\62\60\3\2\2\2\62\61\3\2\2\2\63\5\3\2\2\2\648\5\n\6\2\658\5\32\16" +
                    "\2\668\5\34\17\2\67\64\3\2\2\2\67\65\3\2\2\2\67\66\3\2\2\28\7\3\2\2\2" +
                    "9:\7\16\2\2:;\5\6\4\2;\t\3\2\2\2<E\7\b\2\2=F\5\f\7\2>F\5\36\20\2?F\5$" +
                    "\23\2@B\5\32\16\2A@\3\2\2\2BC\3\2\2\2CA\3\2\2\2CD\3\2\2\2DF\3\2\2\2E=" +
                    "\3\2\2\2E>\3\2\2\2E?\3\2\2\2EA\3\2\2\2FG\3\2\2\2GH\7\t\2\2H\13\3\2\2\2" +
                    "IJ\7\22\2\2JL\7\n\2\2KM\5\16\b\2LK\3\2\2\2MN\3\2\2\2NL\3\2\2\2NO\3\2\2" +
                    "\2OP\3\2\2\2PQ\7\13\2\2Q\r\3\2\2\2RW\5\20\t\2SW\5\24\13\2TW\5\22\n\2U" +
                    "W\5\26\f\2VR\3\2\2\2VS\3\2\2\2VT\3\2\2\2VU\3\2\2\2W\17\3\2\2\2XY\b\t\1" +
                    "\2YZ\5\24\13\2Z[\7\35\2\2[\\\5\24\13\2\\z\3\2\2\2]^\5\24\13\2^_\7\35\2" +
                    "\2_`\5\20\t\t`z\3\2\2\2ab\5\24\13\2bc\7\35\2\2cd\5\22\n\2dz\3\2\2\2ef" +
                    "\5\22\n\2fg\7\35\2\2gh\5\22\n\2hz\3\2\2\2ij\5\22\n\2jk\7\35\2\2kl\5\20" +
                    "\t\6lz\3\2\2\2mn\5\22\n\2no\7\35\2\2op\5\24\13\2pz\3\2\2\2qr\5\26\f\2" +
                    "rs\7 \2\2st\5\30\r\2tz\3\2\2\2uv\5\30\r\2vw\7 \2\2wx\5\26\f\2xz\3\2\2" +
                    "\2yX\3\2\2\2y]\3\2\2\2ya\3\2\2\2ye\3\2\2\2yi\3\2\2\2ym\3\2\2\2yq\3\2\2" +
                    "\2yu\3\2\2\2z\u0086\3\2\2\2{|\f\r\2\2|}\7\35\2\2}\u0085\5\20\t\16~\177" +
                    "\f\f\2\2\177\u0080\7\35\2\2\u0080\u0085\5\24\13\2\u0081\u0082\f\13\2\2" +
                    "\u0082\u0083\7\35\2\2\u0083\u0085\5\22\n\2\u0084{\3\2\2\2\u0084~\3\2\2" +
                    "\2\u0084\u0081\3\2\2\2\u0085\u0088\3\2\2\2\u0086\u0084\3\2\2\2\u0086\u0087" +
                    "\3\2\2\2\u0087\21\3\2\2\2\u0088\u0086\3\2\2\2\u0089\u008a\5\26\f\2\u008a" +
                    "\u008b\7\23\2\2\u008b\u008c\5\30\r\2\u008c\u0092\3\2\2\2\u008d\u008e\5" +
                    "\30\r\2\u008e\u008f\7\23\2\2\u008f\u0090\5\26\f\2\u0090\u0092\3\2\2\2" +
                    "\u0091\u0089\3\2\2\2\u0091\u008d\3\2\2\2\u0092\23\3\2\2\2\u0093\u0094" +
                    "\5\26\f\2\u0094\u0095\7\5\2\2\u0095\u0096\7(\2\2\u0096\u0097\7&\2\2\u0097" +
                    "\25\3\2\2\2\u0098\u00a1\7\f\2\2\u0099\u009a\7\r\2\2\u009a\u00a2\7\24\2" +
                    "\2\u009b\u009d\7\r\2\2\u009c\u009b\3\2\2\2\u009c\u009d\3\2\2\2\u009d\u009e" +
                    "\3\2\2\2\u009e\u009f\7\b\2\2\u009f\u00a0\7\25\2\2\u00a0\u00a2\7\t\2\2" +
                    "\u00a1\u0099\3\2\2\2\u00a1\u009c\3\2\2\2\u00a1\u00a2\3\2\2\2\u00a2\u00a5" +
                    "\3\2\2\2\u00a3\u00a5\5\2\2\2\u00a4\u0098\3\2\2\2\u00a4\u00a3\3\2\2\2\u00a5" +
                    "\27\3\2\2\2\u00a6\u00a7\t\2\2\2\u00a7\31\3\2\2\2\u00a8\u00a9\t\3\2\2\u00a9" +
                    "\33\3\2\2\2\u00aa\u00ab\7\20\2\2\u00ab\35\3\2\2\2\u00ac\u00ad\5 \21\2" +
                    "\u00ad\u00af\7\21\2\2\u00ae\u00b0\5\"\22\2\u00af\u00ae\3\2\2\2\u00af\u00b0" +
                    "\3\2\2\2\u00b0\u00b7\3\2\2\2\u00b1\u00b2\7\21\2\2\u00b2\u00b7\7\26\2\2" +
                    "\u00b3\u00b4\7\27\2\2\u00b4\u00b7\7\21\2\2\u00b5\u00b7\5\34\17\2\u00b6" +
                    "\u00ac\3\2\2\2\u00b6\u00b1\3\2\2\2\u00b6\u00b3\3\2\2\2\u00b6\u00b5\3\2" +
                    "\2\2\u00b7\37\3\2\2\2\u00b8\u00b9\7\26\2\2\u00b9!\3\2\2\2\u00ba\u00bb" +
                    "\7\26\2\2\u00bb#\3\2\2\2\u00bc\u00be\7\26\2\2\u00bd\u00bc\3\2\2\2\u00be" +
                    "\u00bf\3\2\2\2\u00bf\u00bd\3\2\2\2\u00bf\u00c0\3\2\2\2\u00c0%\3\2\2\2" +
                    "\24\',\62\67CENVy\u0084\u0086\u0091\u009c\u00a1\u00a4\u00af\u00b6\u00bf";
    public static final ATN _ATN =
            new ATNDeserializer().deserialize(_serializedATN.toCharArray());

    static {
        _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
        for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
            _decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
        }
    }
}