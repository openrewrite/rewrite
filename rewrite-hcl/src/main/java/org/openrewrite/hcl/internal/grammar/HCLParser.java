/*
 * Copyright 2025 the original author or authors.
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
// Generated from ~/git/rewrite/rewrite-hcl/src/main/antlr/HCLParser.g4 by ANTLR 4.13.2
package org.openrewrite.hcl.internal.grammar;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.ParserATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class HCLParser extends Parser {
    static {
        RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION);
    }

    protected static final DFA[] _decisionToDFA;
    protected static final PredictionContextCache _sharedContextCache =
            new PredictionContextCache();
    public static final int
            FOR_BRACE = 1, FOR_BRACK = 2, IF = 3, IN = 4, BooleanLiteral = 5, NULL = 6, LBRACE = 7,
            RBRACE = 8, ASSIGN = 9, Identifier = 10, WS = 11, COMMENT = 12, LINE_COMMENT = 13,
            NEWLINE = 14, NumericLiteral = 15, QUOTE = 16, HEREDOC_START = 17, PLUS = 18, AND = 19,
            EQ = 20, LT = 21, COLON = 22, LBRACK = 23, LPAREN = 24, MINUS = 25, OR = 26, NEQ = 27,
            GT = 28, QUESTION = 29, RBRACK = 30, RPAREN = 31, MUL = 32, NOT = 33, LEQ = 34, DOT = 35,
            DIV = 36, GEQ = 37, ARROW = 38, COMMA = 39, MOD = 40, ELLIPSIS = 41, TILDE = 42, TEMPLATE_INTERPOLATION_START = 43,
            TemplateStringLiteral = 44, TemplateStringLiteralChar = 45, HTemplateLiteral = 46,
            HTemplateLiteralChar = 47;
    public static final int
            RULE_configFile = 0, RULE_body = 1, RULE_bodyContent = 2, RULE_attribute = 3,
            RULE_block = 4, RULE_blockLabel = 5, RULE_expression = 6, RULE_exprTerm = 7,
            RULE_blockExpr = 8, RULE_literalValue = 9, RULE_collectionValue = 10,
            RULE_tuple = 11, RULE_object = 12, RULE_objectelem = 13, RULE_forExpr = 14,
            RULE_forTupleExpr = 15, RULE_forObjectExpr = 16, RULE_forIntro = 17, RULE_forCond = 18,
            RULE_variableExpr = 19, RULE_functionCall = 20, RULE_arguments = 21, RULE_index = 22,
            RULE_getAttr = 23, RULE_legacyIndexAttr = 24, RULE_splat = 25, RULE_attrSplat = 26,
            RULE_fullSplat = 27, RULE_operation = 28, RULE_unaryOp = 29, RULE_binaryOp = 30,
            RULE_binaryOperator = 31, RULE_compareOperator = 32, RULE_arithmeticOperator = 33,
            RULE_logicOperator = 34, RULE_templateExpr = 35, RULE_heredocTemplatePart = 36,
            RULE_heredocLiteral = 37, RULE_quotedTemplatePart = 38, RULE_stringLiteral = 39,
            RULE_templateInterpolation = 40;

    private static String[] makeRuleNames() {
        return new String[]{
                "configFile", "body", "bodyContent", "attribute", "block", "blockLabel",
                "expression", "exprTerm", "blockExpr", "literalValue", "collectionValue",
                "tuple", "object", "objectelem", "forExpr", "forTupleExpr", "forObjectExpr",
                "forIntro", "forCond", "variableExpr", "functionCall", "arguments", "index",
                "getAttr", "legacyIndexAttr", "splat", "attrSplat", "fullSplat", "operation",
                "unaryOp", "binaryOp", "binaryOperator", "compareOperator", "arithmeticOperator",
                "logicOperator", "templateExpr", "heredocTemplatePart", "heredocLiteral",
                "quotedTemplatePart", "stringLiteral", "templateInterpolation"
        };
    }

    public static final String[] ruleNames = makeRuleNames();

    private static String[] makeLiteralNames() {
        return new String[]{
                null, null, null, "'if'", "'in'", null, "'null'", "'{'", "'}'", "'='",
                null, null, null, null, null, null, null, null, "'+'", "'&&'", "'=='",
                "'<'", "':'", "'['", "'('", "'-'", "'||'", "'!='", "'>'", "'?'", "']'",
                "')'", "'*'", "'!'", "'<='", "'.'", "'/'", "'>='", "'=>'", "','", "'%'",
                "'...'", "'~'"
        };
    }

    private static final String[] _LITERAL_NAMES = makeLiteralNames();

    private static String[] makeSymbolicNames() {
        return new String[]{
                null, "FOR_BRACE", "FOR_BRACK", "IF", "IN", "BooleanLiteral", "NULL",
                "LBRACE", "RBRACE", "ASSIGN", "Identifier", "WS", "COMMENT", "LINE_COMMENT",
                "NEWLINE", "NumericLiteral", "QUOTE", "HEREDOC_START", "PLUS", "AND",
                "EQ", "LT", "COLON", "LBRACK", "LPAREN", "MINUS", "OR", "NEQ", "GT",
                "QUESTION", "RBRACK", "RPAREN", "MUL", "NOT", "LEQ", "DOT", "DIV", "GEQ",
                "ARROW", "COMMA", "MOD", "ELLIPSIS", "TILDE", "TEMPLATE_INTERPOLATION_START",
                "TemplateStringLiteral", "TemplateStringLiteralChar", "HTemplateLiteral",
                "HTemplateLiteralChar"
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
        return "HCLParser.g4";
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

    public HCLParser(TokenStream input) {
        super(input);
        _interp = new ParserATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ConfigFileContext extends ParserRuleContext {
        public BodyContext body() {
            return getRuleContext(BodyContext.class, 0);
        }

        public ConfigFileContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_configFile;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterConfigFile(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitConfigFile(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitConfigFile(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ConfigFileContext configFile() throws RecognitionException {
        ConfigFileContext _localctx = new ConfigFileContext(_ctx, getState());
        enterRule(_localctx, 0, RULE_configFile);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(82);
                body();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class BodyContext extends ParserRuleContext {
        public List<BodyContentContext> bodyContent() {
            return getRuleContexts(BodyContentContext.class);
        }

        public BodyContentContext bodyContent(int i) {
            return getRuleContext(BodyContentContext.class, i);
        }

        public BodyContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_body;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterBody(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitBody(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitBody(this);
            else return visitor.visitChildren(this);
        }
    }

    public final BodyContext body() throws RecognitionException {
        BodyContext _localctx = new BodyContext(_ctx, getState());
        enterRule(_localctx, 2, RULE_body);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(87);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == NULL || _la == Identifier) {
                    {
                        {
                            setState(84);
                            bodyContent();
                        }
                    }
                    setState(89);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class BodyContentContext extends ParserRuleContext {
        public AttributeContext attribute() {
            return getRuleContext(AttributeContext.class, 0);
        }

        public BlockContext block() {
            return getRuleContext(BlockContext.class, 0);
        }

        public BodyContentContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_bodyContent;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterBodyContent(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitBodyContent(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitBodyContent(this);
            else return visitor.visitChildren(this);
        }
    }

    public final BodyContentContext bodyContent() throws RecognitionException {
        BodyContentContext _localctx = new BodyContentContext(_ctx, getState());
        enterRule(_localctx, 4, RULE_bodyContent);
        try {
            setState(92);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 1, _ctx)) {
                case 1:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(90);
                    attribute();
                }
                break;
                case 2:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(91);
                    block();
                }
                break;
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class AttributeContext extends ParserRuleContext {
        public TerminalNode ASSIGN() {
            return getToken(HCLParser.ASSIGN, 0);
        }

        public ExpressionContext expression() {
            return getRuleContext(ExpressionContext.class, 0);
        }

        public TerminalNode Identifier() {
            return getToken(HCLParser.Identifier, 0);
        }

        public TerminalNode NULL() {
            return getToken(HCLParser.NULL, 0);
        }

        public AttributeContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_attribute;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterAttribute(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitAttribute(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitAttribute(this);
            else return visitor.visitChildren(this);
        }
    }

    public final AttributeContext attribute() throws RecognitionException {
        AttributeContext _localctx = new AttributeContext(_ctx, getState());
        enterRule(_localctx, 6, RULE_attribute);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(94);
                _la = _input.LA(1);
                if (!(_la == NULL || _la == Identifier)) {
                    _errHandler.recoverInline(this);
                } else {
                    if (_input.LA(1) == Token.EOF) matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
                setState(95);
                match(ASSIGN);
                setState(96);
                expression(0);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class BlockContext extends ParserRuleContext {
        public TerminalNode Identifier() {
            return getToken(HCLParser.Identifier, 0);
        }

        public BlockExprContext blockExpr() {
            return getRuleContext(BlockExprContext.class, 0);
        }

        public List<BlockLabelContext> blockLabel() {
            return getRuleContexts(BlockLabelContext.class);
        }

        public BlockLabelContext blockLabel(int i) {
            return getRuleContext(BlockLabelContext.class, i);
        }

        public BlockContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_block;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterBlock(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitBlock(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitBlock(this);
            else return visitor.visitChildren(this);
        }
    }

    public final BlockContext block() throws RecognitionException {
        BlockContext _localctx = new BlockContext(_ctx, getState());
        enterRule(_localctx, 8, RULE_block);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(98);
                match(Identifier);
                setState(102);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == Identifier || _la == QUOTE) {
                    {
                        {
                            setState(99);
                            blockLabel();
                        }
                    }
                    setState(104);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
                setState(105);
                blockExpr();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class BlockLabelContext extends ParserRuleContext {
        public List<TerminalNode> QUOTE() {
            return getTokens(HCLParser.QUOTE);
        }

        public TerminalNode QUOTE(int i) {
            return getToken(HCLParser.QUOTE, i);
        }

        public StringLiteralContext stringLiteral() {
            return getRuleContext(StringLiteralContext.class, 0);
        }

        public TerminalNode Identifier() {
            return getToken(HCLParser.Identifier, 0);
        }

        public BlockLabelContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_blockLabel;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterBlockLabel(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitBlockLabel(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitBlockLabel(this);
            else return visitor.visitChildren(this);
        }
    }

    public final BlockLabelContext blockLabel() throws RecognitionException {
        BlockLabelContext _localctx = new BlockLabelContext(_ctx, getState());
        enterRule(_localctx, 10, RULE_blockLabel);
        try {
            setState(112);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case QUOTE:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(107);
                    match(QUOTE);
                    setState(108);
                    stringLiteral();
                    setState(109);
                    match(QUOTE);
                }
                break;
                case Identifier:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(111);
                    match(Identifier);
                }
                break;
                default:
                    throw new NoViableAltException(this);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ExpressionContext extends ParserRuleContext {
        public ExpressionContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_expression;
        }

        public ExpressionContext() {
        }

        public void copyFrom(ExpressionContext ctx) {
            super.copyFrom(ctx);
        }
    }

    @SuppressWarnings("CheckReturnValue")
    public static class OperationExpressionContext extends ExpressionContext {
        public OperationContext operation() {
            return getRuleContext(OperationContext.class, 0);
        }

        public OperationExpressionContext(ExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterOperationExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitOperationExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitOperationExpression(this);
            else return visitor.visitChildren(this);
        }
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ConditionalExpressionContext extends ExpressionContext {
        public List<ExpressionContext> expression() {
            return getRuleContexts(ExpressionContext.class);
        }

        public ExpressionContext expression(int i) {
            return getRuleContext(ExpressionContext.class, i);
        }

        public TerminalNode QUESTION() {
            return getToken(HCLParser.QUESTION, 0);
        }

        public TerminalNode COLON() {
            return getToken(HCLParser.COLON, 0);
        }

        public ConditionalExpressionContext(ExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterConditionalExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitConditionalExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitConditionalExpression(this);
            else return visitor.visitChildren(this);
        }
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ExpressionTermContext extends ExpressionContext {
        public ExprTermContext exprTerm() {
            return getRuleContext(ExprTermContext.class, 0);
        }

        public ExpressionTermContext(ExpressionContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterExpressionTerm(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitExpressionTerm(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitExpressionTerm(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ExpressionContext expression() throws RecognitionException {
        return expression(0);
    }

    private ExpressionContext expression(int _p) throws RecognitionException {
        ParserRuleContext _parentctx = _ctx;
        int _parentState = getState();
        ExpressionContext _localctx = new ExpressionContext(_ctx, _parentState);
        int _startState = 12;
        enterRecursionRule(_localctx, 12, RULE_expression, _p);
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(117);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 4, _ctx)) {
                    case 1: {
                        _localctx = new ExpressionTermContext(_localctx);
                        _ctx = _localctx;

                        setState(115);
                        exprTerm(0);
                    }
                    break;
                    case 2: {
                        _localctx = new OperationExpressionContext(_localctx);
                        _ctx = _localctx;
                        setState(116);
                        operation();
                    }
                    break;
                }
                _ctx.stop = _input.LT(-1);
                setState(127);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 5, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        if (_parseListeners != null) triggerExitRuleEvent();
                        {
                            {
                                _localctx = new ConditionalExpressionContext(new ExpressionContext(_parentctx, _parentState));
                                pushNewRecursionContext(_localctx, _startState, RULE_expression);
                                setState(119);
                                if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
                                setState(120);
                                match(QUESTION);
                                setState(121);
                                expression(0);
                                setState(122);
                                match(COLON);
                                setState(123);
                                expression(2);
                            }
                        }
                    }
                    setState(129);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 5, _ctx);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            unrollRecursionContexts(_parentctx);
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ExprTermContext extends ParserRuleContext {
        public ExprTermContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_exprTerm;
        }

        public ExprTermContext() {
        }

        public void copyFrom(ExprTermContext ctx) {
            super.copyFrom(ctx);
        }
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ParentheticalExpressionContext extends ExprTermContext {
        public TerminalNode LPAREN() {
            return getToken(HCLParser.LPAREN, 0);
        }

        public ExpressionContext expression() {
            return getRuleContext(ExpressionContext.class, 0);
        }

        public TerminalNode RPAREN() {
            return getToken(HCLParser.RPAREN, 0);
        }

        public ParentheticalExpressionContext(ExprTermContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener)
                ((HCLParserListener) listener).enterParentheticalExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitParentheticalExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitParentheticalExpression(this);
            else return visitor.visitChildren(this);
        }
    }

    @SuppressWarnings("CheckReturnValue")
    public static class AttributeAccessExpressionContext extends ExprTermContext {
        public ExprTermContext exprTerm() {
            return getRuleContext(ExprTermContext.class, 0);
        }

        public GetAttrContext getAttr() {
            return getRuleContext(GetAttrContext.class, 0);
        }

        public AttributeAccessExpressionContext(ExprTermContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener)
                ((HCLParserListener) listener).enterAttributeAccessExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener)
                ((HCLParserListener) listener).exitAttributeAccessExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitAttributeAccessExpression(this);
            else return visitor.visitChildren(this);
        }
    }

    @SuppressWarnings("CheckReturnValue")
    public static class LiteralExpressionContext extends ExprTermContext {
        public LiteralValueContext literalValue() {
            return getRuleContext(LiteralValueContext.class, 0);
        }

        public LiteralExpressionContext(ExprTermContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterLiteralExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitLiteralExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitLiteralExpression(this);
            else return visitor.visitChildren(this);
        }
    }

    @SuppressWarnings("CheckReturnValue")
    public static class TemplateExpressionContext extends ExprTermContext {
        public TemplateExprContext templateExpr() {
            return getRuleContext(TemplateExprContext.class, 0);
        }

        public TemplateExpressionContext(ExprTermContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterTemplateExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitTemplateExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitTemplateExpression(this);
            else return visitor.visitChildren(this);
        }
    }

    @SuppressWarnings("CheckReturnValue")
    public static class VariableExpressionContext extends ExprTermContext {
        public VariableExprContext variableExpr() {
            return getRuleContext(VariableExprContext.class, 0);
        }

        public VariableExpressionContext(ExprTermContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterVariableExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitVariableExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitVariableExpression(this);
            else return visitor.visitChildren(this);
        }
    }

    @SuppressWarnings("CheckReturnValue")
    public static class SplatExpressionContext extends ExprTermContext {
        public ExprTermContext exprTerm() {
            return getRuleContext(ExprTermContext.class, 0);
        }

        public SplatContext splat() {
            return getRuleContext(SplatContext.class, 0);
        }

        public SplatExpressionContext(ExprTermContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterSplatExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitSplatExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitSplatExpression(this);
            else return visitor.visitChildren(this);
        }
    }

    @SuppressWarnings("CheckReturnValue")
    public static class IndexAccessExpressionContext extends ExprTermContext {
        public ExprTermContext exprTerm() {
            return getRuleContext(ExprTermContext.class, 0);
        }

        public IndexContext index() {
            return getRuleContext(IndexContext.class, 0);
        }

        public IndexAccessExpressionContext(ExprTermContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterIndexAccessExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitIndexAccessExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitIndexAccessExpression(this);
            else return visitor.visitChildren(this);
        }
    }

    @SuppressWarnings("CheckReturnValue")
    public static class LegacyIndexAttributeExpressionContext extends ExprTermContext {
        public ExprTermContext exprTerm() {
            return getRuleContext(ExprTermContext.class, 0);
        }

        public LegacyIndexAttrContext legacyIndexAttr() {
            return getRuleContext(LegacyIndexAttrContext.class, 0);
        }

        public LegacyIndexAttributeExpressionContext(ExprTermContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener)
                ((HCLParserListener) listener).enterLegacyIndexAttributeExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener)
                ((HCLParserListener) listener).exitLegacyIndexAttributeExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitLegacyIndexAttributeExpression(this);
            else return visitor.visitChildren(this);
        }
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ForExpressionContext extends ExprTermContext {
        public ForExprContext forExpr() {
            return getRuleContext(ForExprContext.class, 0);
        }

        public ForExpressionContext(ExprTermContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterForExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitForExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitForExpression(this);
            else return visitor.visitChildren(this);
        }
    }

    @SuppressWarnings("CheckReturnValue")
    public static class FunctionCallExpressionContext extends ExprTermContext {
        public FunctionCallContext functionCall() {
            return getRuleContext(FunctionCallContext.class, 0);
        }

        public FunctionCallExpressionContext(ExprTermContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterFunctionCallExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitFunctionCallExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitFunctionCallExpression(this);
            else return visitor.visitChildren(this);
        }
    }

    @SuppressWarnings("CheckReturnValue")
    public static class CollectionValueExpressionContext extends ExprTermContext {
        public CollectionValueContext collectionValue() {
            return getRuleContext(CollectionValueContext.class, 0);
        }

        public CollectionValueExpressionContext(ExprTermContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener)
                ((HCLParserListener) listener).enterCollectionValueExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener)
                ((HCLParserListener) listener).exitCollectionValueExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitCollectionValueExpression(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ExprTermContext exprTerm() throws RecognitionException {
        return exprTerm(0);
    }

    private ExprTermContext exprTerm(int _p) throws RecognitionException {
        ParserRuleContext _parentctx = _ctx;
        int _parentState = getState();
        ExprTermContext _localctx = new ExprTermContext(_ctx, _parentState);
        int _startState = 14;
        enterRecursionRule(_localctx, 14, RULE_exprTerm, _p);
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(141);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 6, _ctx)) {
                    case 1: {
                        _localctx = new TemplateExpressionContext(_localctx);
                        _ctx = _localctx;

                        setState(131);
                        templateExpr();
                    }
                    break;
                    case 2: {
                        _localctx = new LiteralExpressionContext(_localctx);
                        _ctx = _localctx;
                        setState(132);
                        literalValue();
                    }
                    break;
                    case 3: {
                        _localctx = new ForExpressionContext(_localctx);
                        _ctx = _localctx;
                        setState(133);
                        forExpr();
                    }
                    break;
                    case 4: {
                        _localctx = new CollectionValueExpressionContext(_localctx);
                        _ctx = _localctx;
                        setState(134);
                        collectionValue();
                    }
                    break;
                    case 5: {
                        _localctx = new VariableExpressionContext(_localctx);
                        _ctx = _localctx;
                        setState(135);
                        variableExpr();
                    }
                    break;
                    case 6: {
                        _localctx = new FunctionCallExpressionContext(_localctx);
                        _ctx = _localctx;
                        setState(136);
                        functionCall();
                    }
                    break;
                    case 7: {
                        _localctx = new ParentheticalExpressionContext(_localctx);
                        _ctx = _localctx;
                        setState(137);
                        match(LPAREN);
                        setState(138);
                        expression(0);
                        setState(139);
                        match(RPAREN);
                    }
                    break;
                }
                _ctx.stop = _input.LT(-1);
                setState(153);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 8, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        if (_parseListeners != null) triggerExitRuleEvent();
                        {
                            setState(151);
                            _errHandler.sync(this);
                            switch (getInterpreter().adaptivePredict(_input, 7, _ctx)) {
                                case 1: {
                                    _localctx = new IndexAccessExpressionContext(new ExprTermContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_exprTerm);
                                    setState(143);
                                    if (!(precpred(_ctx, 5)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 5)");
                                    setState(144);
                                    index();
                                }
                                break;
                                case 2: {
                                    _localctx = new AttributeAccessExpressionContext(new ExprTermContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_exprTerm);
                                    setState(145);
                                    if (!(precpred(_ctx, 4)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 4)");
                                    setState(146);
                                    getAttr();
                                }
                                break;
                                case 3: {
                                    _localctx = new LegacyIndexAttributeExpressionContext(new ExprTermContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_exprTerm);
                                    setState(147);
                                    if (!(precpred(_ctx, 3)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 3)");
                                    setState(148);
                                    legacyIndexAttr();
                                }
                                break;
                                case 4: {
                                    _localctx = new SplatExpressionContext(new ExprTermContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_exprTerm);
                                    setState(149);
                                    if (!(precpred(_ctx, 2)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 2)");
                                    setState(150);
                                    splat();
                                }
                                break;
                            }
                        }
                    }
                    setState(155);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 8, _ctx);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            unrollRecursionContexts(_parentctx);
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class BlockExprContext extends ParserRuleContext {
        public TerminalNode LBRACE() {
            return getToken(HCLParser.LBRACE, 0);
        }

        public BodyContext body() {
            return getRuleContext(BodyContext.class, 0);
        }

        public TerminalNode RBRACE() {
            return getToken(HCLParser.RBRACE, 0);
        }

        public BlockExprContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_blockExpr;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterBlockExpr(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitBlockExpr(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitBlockExpr(this);
            else return visitor.visitChildren(this);
        }
    }

    public final BlockExprContext blockExpr() throws RecognitionException {
        BlockExprContext _localctx = new BlockExprContext(_ctx, getState());
        enterRule(_localctx, 16, RULE_blockExpr);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(156);
                match(LBRACE);
                setState(157);
                body();
                setState(158);
                match(RBRACE);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class LiteralValueContext extends ParserRuleContext {
        public TerminalNode NumericLiteral() {
            return getToken(HCLParser.NumericLiteral, 0);
        }

        public TerminalNode BooleanLiteral() {
            return getToken(HCLParser.BooleanLiteral, 0);
        }

        public TerminalNode NULL() {
            return getToken(HCLParser.NULL, 0);
        }

        public LiteralValueContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_literalValue;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterLiteralValue(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitLiteralValue(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitLiteralValue(this);
            else return visitor.visitChildren(this);
        }
    }

    public final LiteralValueContext literalValue() throws RecognitionException {
        LiteralValueContext _localctx = new LiteralValueContext(_ctx, getState());
        enterRule(_localctx, 18, RULE_literalValue);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(160);
                _la = _input.LA(1);
                if (!((((_la) & ~0x3f) == 0 && ((1L << _la) & 32864L) != 0))) {
                    _errHandler.recoverInline(this);
                } else {
                    if (_input.LA(1) == Token.EOF) matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class CollectionValueContext extends ParserRuleContext {
        public TupleContext tuple() {
            return getRuleContext(TupleContext.class, 0);
        }

        public ObjectContext object() {
            return getRuleContext(ObjectContext.class, 0);
        }

        public CollectionValueContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_collectionValue;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterCollectionValue(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitCollectionValue(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitCollectionValue(this);
            else return visitor.visitChildren(this);
        }
    }

    public final CollectionValueContext collectionValue() throws RecognitionException {
        CollectionValueContext _localctx = new CollectionValueContext(_ctx, getState());
        enterRule(_localctx, 20, RULE_collectionValue);
        try {
            setState(164);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case LBRACK:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(162);
                    tuple();
                }
                break;
                case LBRACE:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(163);
                    object();
                }
                break;
                default:
                    throw new NoViableAltException(this);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class TupleContext extends ParserRuleContext {
        public TerminalNode LBRACK() {
            return getToken(HCLParser.LBRACK, 0);
        }

        public TerminalNode RBRACK() {
            return getToken(HCLParser.RBRACK, 0);
        }

        public List<ExpressionContext> expression() {
            return getRuleContexts(ExpressionContext.class);
        }

        public ExpressionContext expression(int i) {
            return getRuleContext(ExpressionContext.class, i);
        }

        public List<TerminalNode> COMMA() {
            return getTokens(HCLParser.COMMA);
        }

        public TerminalNode COMMA(int i) {
            return getToken(HCLParser.COMMA, i);
        }

        public TupleContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_tuple;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterTuple(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitTuple(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitTuple(this);
            else return visitor.visitChildren(this);
        }
    }

    public final TupleContext tuple() throws RecognitionException {
        TupleContext _localctx = new TupleContext(_ctx, getState());
        enterRule(_localctx, 22, RULE_tuple);
        int _la;
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(166);
                match(LBRACK);
                setState(178);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 8648885478L) != 0)) {
                    {
                        setState(167);
                        expression(0);
                        setState(172);
                        _errHandler.sync(this);
                        _alt = getInterpreter().adaptivePredict(_input, 10, _ctx);
                        while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                            if (_alt == 1) {
                                {
                                    {
                                        setState(168);
                                        match(COMMA);
                                        setState(169);
                                        expression(0);
                                    }
                                }
                            }
                            setState(174);
                            _errHandler.sync(this);
                            _alt = getInterpreter().adaptivePredict(_input, 10, _ctx);
                        }
                        setState(176);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        if (_la == COMMA) {
                            {
                                setState(175);
                                match(COMMA);
                            }
                        }

                    }
                }

                setState(180);
                match(RBRACK);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ObjectContext extends ParserRuleContext {
        public TerminalNode LBRACE() {
            return getToken(HCLParser.LBRACE, 0);
        }

        public TerminalNode RBRACE() {
            return getToken(HCLParser.RBRACE, 0);
        }

        public List<ObjectelemContext> objectelem() {
            return getRuleContexts(ObjectelemContext.class);
        }

        public ObjectelemContext objectelem(int i) {
            return getRuleContext(ObjectelemContext.class, i);
        }

        public ObjectContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_object;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterObject(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitObject(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitObject(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ObjectContext object() throws RecognitionException {
        ObjectContext _localctx = new ObjectContext(_ctx, getState());
        enterRule(_localctx, 24, RULE_object);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(182);
                match(LBRACE);
                setState(186);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 8648885478L) != 0)) {
                    {
                        {
                            setState(183);
                            objectelem();
                        }
                    }
                    setState(188);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
                setState(189);
                match(RBRACE);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ObjectelemContext extends ParserRuleContext {
        public List<ExpressionContext> expression() {
            return getRuleContexts(ExpressionContext.class);
        }

        public ExpressionContext expression(int i) {
            return getRuleContext(ExpressionContext.class, i);
        }

        public TerminalNode ASSIGN() {
            return getToken(HCLParser.ASSIGN, 0);
        }

        public TerminalNode COLON() {
            return getToken(HCLParser.COLON, 0);
        }

        public TerminalNode Identifier() {
            return getToken(HCLParser.Identifier, 0);
        }

        public TerminalNode NULL() {
            return getToken(HCLParser.NULL, 0);
        }

        public TerminalNode LPAREN() {
            return getToken(HCLParser.LPAREN, 0);
        }

        public TerminalNode RPAREN() {
            return getToken(HCLParser.RPAREN, 0);
        }

        public List<TerminalNode> QUOTE() {
            return getTokens(HCLParser.QUOTE);
        }

        public TerminalNode QUOTE(int i) {
            return getToken(HCLParser.QUOTE, i);
        }

        public TerminalNode COMMA() {
            return getToken(HCLParser.COMMA, 0);
        }

        public List<QuotedTemplatePartContext> quotedTemplatePart() {
            return getRuleContexts(QuotedTemplatePartContext.class);
        }

        public QuotedTemplatePartContext quotedTemplatePart(int i) {
            return getRuleContext(QuotedTemplatePartContext.class, i);
        }

        public ObjectelemContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_objectelem;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterObjectelem(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitObjectelem(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitObjectelem(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ObjectelemContext objectelem() throws RecognitionException {
        ObjectelemContext _localctx = new ObjectelemContext(_ctx, getState());
        enterRule(_localctx, 26, RULE_objectelem);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(205);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 15, _ctx)) {
                    case 1: {
                        setState(191);
                        match(Identifier);
                    }
                    break;
                    case 2: {
                        setState(192);
                        match(NULL);
                    }
                    break;
                    case 3: {
                        setState(193);
                        match(LPAREN);
                        setState(194);
                        match(Identifier);
                        setState(195);
                        match(RPAREN);
                    }
                    break;
                    case 4: {
                        setState(196);
                        match(QUOTE);
                        setState(200);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while (_la == TEMPLATE_INTERPOLATION_START || _la == TemplateStringLiteral) {
                            {
                                {
                                    setState(197);
                                    quotedTemplatePart();
                                }
                            }
                            setState(202);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(203);
                        match(QUOTE);
                    }
                    break;
                    case 5: {
                        setState(204);
                        expression(0);
                    }
                    break;
                }
                setState(207);
                _la = _input.LA(1);
                if (!(_la == ASSIGN || _la == COLON)) {
                    _errHandler.recoverInline(this);
                } else {
                    if (_input.LA(1) == Token.EOF) matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
                setState(208);
                expression(0);
                setState(210);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == COMMA) {
                    {
                        setState(209);
                        match(COMMA);
                    }
                }

            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ForExprContext extends ParserRuleContext {
        public ForTupleExprContext forTupleExpr() {
            return getRuleContext(ForTupleExprContext.class, 0);
        }

        public ForObjectExprContext forObjectExpr() {
            return getRuleContext(ForObjectExprContext.class, 0);
        }

        public ForExprContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_forExpr;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterForExpr(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitForExpr(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitForExpr(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ForExprContext forExpr() throws RecognitionException {
        ForExprContext _localctx = new ForExprContext(_ctx, getState());
        enterRule(_localctx, 28, RULE_forExpr);
        try {
            setState(214);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case FOR_BRACK:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(212);
                    forTupleExpr();
                }
                break;
                case FOR_BRACE:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(213);
                    forObjectExpr();
                }
                break;
                default:
                    throw new NoViableAltException(this);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ForTupleExprContext extends ParserRuleContext {
        public TerminalNode FOR_BRACK() {
            return getToken(HCLParser.FOR_BRACK, 0);
        }

        public ForIntroContext forIntro() {
            return getRuleContext(ForIntroContext.class, 0);
        }

        public TerminalNode COLON() {
            return getToken(HCLParser.COLON, 0);
        }

        public ExpressionContext expression() {
            return getRuleContext(ExpressionContext.class, 0);
        }

        public TerminalNode RBRACK() {
            return getToken(HCLParser.RBRACK, 0);
        }

        public ForCondContext forCond() {
            return getRuleContext(ForCondContext.class, 0);
        }

        public ForTupleExprContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_forTupleExpr;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterForTupleExpr(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitForTupleExpr(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitForTupleExpr(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ForTupleExprContext forTupleExpr() throws RecognitionException {
        ForTupleExprContext _localctx = new ForTupleExprContext(_ctx, getState());
        enterRule(_localctx, 30, RULE_forTupleExpr);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(216);
                match(FOR_BRACK);
                setState(217);
                forIntro();
                setState(218);
                match(COLON);
                setState(219);
                expression(0);
                setState(221);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == IF) {
                    {
                        setState(220);
                        forCond();
                    }
                }

                setState(223);
                match(RBRACK);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ForObjectExprContext extends ParserRuleContext {
        public TerminalNode FOR_BRACE() {
            return getToken(HCLParser.FOR_BRACE, 0);
        }

        public ForIntroContext forIntro() {
            return getRuleContext(ForIntroContext.class, 0);
        }

        public TerminalNode COLON() {
            return getToken(HCLParser.COLON, 0);
        }

        public List<ExpressionContext> expression() {
            return getRuleContexts(ExpressionContext.class);
        }

        public ExpressionContext expression(int i) {
            return getRuleContext(ExpressionContext.class, i);
        }

        public TerminalNode ARROW() {
            return getToken(HCLParser.ARROW, 0);
        }

        public TerminalNode RBRACE() {
            return getToken(HCLParser.RBRACE, 0);
        }

        public TerminalNode ELLIPSIS() {
            return getToken(HCLParser.ELLIPSIS, 0);
        }

        public ForCondContext forCond() {
            return getRuleContext(ForCondContext.class, 0);
        }

        public ForObjectExprContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_forObjectExpr;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterForObjectExpr(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitForObjectExpr(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitForObjectExpr(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ForObjectExprContext forObjectExpr() throws RecognitionException {
        ForObjectExprContext _localctx = new ForObjectExprContext(_ctx, getState());
        enterRule(_localctx, 32, RULE_forObjectExpr);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(225);
                match(FOR_BRACE);
                setState(226);
                forIntro();
                setState(227);
                match(COLON);
                setState(228);
                expression(0);
                setState(229);
                match(ARROW);
                setState(230);
                expression(0);
                setState(232);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == ELLIPSIS) {
                    {
                        setState(231);
                        match(ELLIPSIS);
                    }
                }

                setState(235);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == IF) {
                    {
                        setState(234);
                        forCond();
                    }
                }

                setState(237);
                match(RBRACE);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ForIntroContext extends ParserRuleContext {
        public List<TerminalNode> Identifier() {
            return getTokens(HCLParser.Identifier);
        }

        public TerminalNode Identifier(int i) {
            return getToken(HCLParser.Identifier, i);
        }

        public TerminalNode IN() {
            return getToken(HCLParser.IN, 0);
        }

        public ExpressionContext expression() {
            return getRuleContext(ExpressionContext.class, 0);
        }

        public TerminalNode COMMA() {
            return getToken(HCLParser.COMMA, 0);
        }

        public ForIntroContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_forIntro;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterForIntro(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitForIntro(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitForIntro(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ForIntroContext forIntro() throws RecognitionException {
        ForIntroContext _localctx = new ForIntroContext(_ctx, getState());
        enterRule(_localctx, 34, RULE_forIntro);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(239);
                match(Identifier);
                setState(242);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == COMMA) {
                    {
                        setState(240);
                        match(COMMA);
                        setState(241);
                        match(Identifier);
                    }
                }

                setState(244);
                match(IN);
                setState(245);
                expression(0);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ForCondContext extends ParserRuleContext {
        public TerminalNode IF() {
            return getToken(HCLParser.IF, 0);
        }

        public ExpressionContext expression() {
            return getRuleContext(ExpressionContext.class, 0);
        }

        public ForCondContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_forCond;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterForCond(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitForCond(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitForCond(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ForCondContext forCond() throws RecognitionException {
        ForCondContext _localctx = new ForCondContext(_ctx, getState());
        enterRule(_localctx, 36, RULE_forCond);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(247);
                match(IF);
                setState(248);
                expression(0);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class VariableExprContext extends ParserRuleContext {
        public TerminalNode Identifier() {
            return getToken(HCLParser.Identifier, 0);
        }

        public VariableExprContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_variableExpr;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterVariableExpr(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitVariableExpr(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitVariableExpr(this);
            else return visitor.visitChildren(this);
        }
    }

    public final VariableExprContext variableExpr() throws RecognitionException {
        VariableExprContext _localctx = new VariableExprContext(_ctx, getState());
        enterRule(_localctx, 38, RULE_variableExpr);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(250);
                match(Identifier);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class FunctionCallContext extends ParserRuleContext {
        public TerminalNode Identifier() {
            return getToken(HCLParser.Identifier, 0);
        }

        public TerminalNode LPAREN() {
            return getToken(HCLParser.LPAREN, 0);
        }

        public TerminalNode RPAREN() {
            return getToken(HCLParser.RPAREN, 0);
        }

        public ArgumentsContext arguments() {
            return getRuleContext(ArgumentsContext.class, 0);
        }

        public FunctionCallContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_functionCall;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterFunctionCall(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitFunctionCall(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitFunctionCall(this);
            else return visitor.visitChildren(this);
        }
    }

    public final FunctionCallContext functionCall() throws RecognitionException {
        FunctionCallContext _localctx = new FunctionCallContext(_ctx, getState());
        enterRule(_localctx, 40, RULE_functionCall);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(252);
                match(Identifier);
                setState(253);
                match(LPAREN);
                setState(255);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 8648885478L) != 0)) {
                    {
                        setState(254);
                        arguments();
                    }
                }

                setState(257);
                match(RPAREN);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ArgumentsContext extends ParserRuleContext {
        public List<ExpressionContext> expression() {
            return getRuleContexts(ExpressionContext.class);
        }

        public ExpressionContext expression(int i) {
            return getRuleContext(ExpressionContext.class, i);
        }

        public List<TerminalNode> COMMA() {
            return getTokens(HCLParser.COMMA);
        }

        public TerminalNode COMMA(int i) {
            return getToken(HCLParser.COMMA, i);
        }

        public TerminalNode ELLIPSIS() {
            return getToken(HCLParser.ELLIPSIS, 0);
        }

        public ArgumentsContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_arguments;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterArguments(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitArguments(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitArguments(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ArgumentsContext arguments() throws RecognitionException {
        ArgumentsContext _localctx = new ArgumentsContext(_ctx, getState());
        enterRule(_localctx, 42, RULE_arguments);
        int _la;
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(259);
                expression(0);
                setState(264);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 23, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        {
                            {
                                setState(260);
                                match(COMMA);
                                setState(261);
                                expression(0);
                            }
                        }
                    }
                    setState(266);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 23, _ctx);
                }
                setState(268);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == COMMA || _la == ELLIPSIS) {
                    {
                        setState(267);
                        _la = _input.LA(1);
                        if (!(_la == COMMA || _la == ELLIPSIS)) {
                            _errHandler.recoverInline(this);
                        } else {
                            if (_input.LA(1) == Token.EOF) matchedEOF = true;
                            _errHandler.reportMatch(this);
                            consume();
                        }
                    }
                }

            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class IndexContext extends ParserRuleContext {
        public TerminalNode LBRACK() {
            return getToken(HCLParser.LBRACK, 0);
        }

        public ExpressionContext expression() {
            return getRuleContext(ExpressionContext.class, 0);
        }

        public TerminalNode RBRACK() {
            return getToken(HCLParser.RBRACK, 0);
        }

        public IndexContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_index;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterIndex(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitIndex(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitIndex(this);
            else return visitor.visitChildren(this);
        }
    }

    public final IndexContext index() throws RecognitionException {
        IndexContext _localctx = new IndexContext(_ctx, getState());
        enterRule(_localctx, 44, RULE_index);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(270);
                match(LBRACK);
                setState(271);
                expression(0);
                setState(272);
                match(RBRACK);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class GetAttrContext extends ParserRuleContext {
        public TerminalNode DOT() {
            return getToken(HCLParser.DOT, 0);
        }

        public TerminalNode Identifier() {
            return getToken(HCLParser.Identifier, 0);
        }

        public GetAttrContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_getAttr;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterGetAttr(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitGetAttr(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitGetAttr(this);
            else return visitor.visitChildren(this);
        }
    }

    public final GetAttrContext getAttr() throws RecognitionException {
        GetAttrContext _localctx = new GetAttrContext(_ctx, getState());
        enterRule(_localctx, 46, RULE_getAttr);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(274);
                match(DOT);
                setState(275);
                match(Identifier);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class LegacyIndexAttrContext extends ParserRuleContext {
        public TerminalNode DOT() {
            return getToken(HCLParser.DOT, 0);
        }

        public TerminalNode NumericLiteral() {
            return getToken(HCLParser.NumericLiteral, 0);
        }

        public LegacyIndexAttrContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_legacyIndexAttr;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterLegacyIndexAttr(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitLegacyIndexAttr(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitLegacyIndexAttr(this);
            else return visitor.visitChildren(this);
        }
    }

    public final LegacyIndexAttrContext legacyIndexAttr() throws RecognitionException {
        LegacyIndexAttrContext _localctx = new LegacyIndexAttrContext(_ctx, getState());
        enterRule(_localctx, 48, RULE_legacyIndexAttr);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(277);
                match(DOT);
                setState(278);
                match(NumericLiteral);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class SplatContext extends ParserRuleContext {
        public AttrSplatContext attrSplat() {
            return getRuleContext(AttrSplatContext.class, 0);
        }

        public FullSplatContext fullSplat() {
            return getRuleContext(FullSplatContext.class, 0);
        }

        public SplatContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_splat;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterSplat(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitSplat(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitSplat(this);
            else return visitor.visitChildren(this);
        }
    }

    public final SplatContext splat() throws RecognitionException {
        SplatContext _localctx = new SplatContext(_ctx, getState());
        enterRule(_localctx, 50, RULE_splat);
        try {
            setState(282);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case DOT:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(280);
                    attrSplat();
                }
                break;
                case LBRACK:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(281);
                    fullSplat();
                }
                break;
                default:
                    throw new NoViableAltException(this);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class AttrSplatContext extends ParserRuleContext {
        public TerminalNode DOT() {
            return getToken(HCLParser.DOT, 0);
        }

        public TerminalNode MUL() {
            return getToken(HCLParser.MUL, 0);
        }

        public List<GetAttrContext> getAttr() {
            return getRuleContexts(GetAttrContext.class);
        }

        public GetAttrContext getAttr(int i) {
            return getRuleContext(GetAttrContext.class, i);
        }

        public AttrSplatContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_attrSplat;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterAttrSplat(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitAttrSplat(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitAttrSplat(this);
            else return visitor.visitChildren(this);
        }
    }

    public final AttrSplatContext attrSplat() throws RecognitionException {
        AttrSplatContext _localctx = new AttrSplatContext(_ctx, getState());
        enterRule(_localctx, 52, RULE_attrSplat);
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(284);
                match(DOT);
                setState(285);
                match(MUL);
                setState(289);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 26, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        {
                            {
                                setState(286);
                                getAttr();
                            }
                        }
                    }
                    setState(291);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 26, _ctx);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class FullSplatContext extends ParserRuleContext {
        public TerminalNode LBRACK() {
            return getToken(HCLParser.LBRACK, 0);
        }

        public TerminalNode MUL() {
            return getToken(HCLParser.MUL, 0);
        }

        public TerminalNode RBRACK() {
            return getToken(HCLParser.RBRACK, 0);
        }

        public List<GetAttrContext> getAttr() {
            return getRuleContexts(GetAttrContext.class);
        }

        public GetAttrContext getAttr(int i) {
            return getRuleContext(GetAttrContext.class, i);
        }

        public List<IndexContext> index() {
            return getRuleContexts(IndexContext.class);
        }

        public IndexContext index(int i) {
            return getRuleContext(IndexContext.class, i);
        }

        public FullSplatContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_fullSplat;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterFullSplat(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitFullSplat(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitFullSplat(this);
            else return visitor.visitChildren(this);
        }
    }

    public final FullSplatContext fullSplat() throws RecognitionException {
        FullSplatContext _localctx = new FullSplatContext(_ctx, getState());
        enterRule(_localctx, 54, RULE_fullSplat);
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(292);
                match(LBRACK);
                setState(293);
                match(MUL);
                setState(294);
                match(RBRACK);
                setState(299);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 28, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        {
                            setState(297);
                            _errHandler.sync(this);
                            switch (_input.LA(1)) {
                                case DOT: {
                                    setState(295);
                                    getAttr();
                                }
                                break;
                                case LBRACK: {
                                    setState(296);
                                    index();
                                }
                                break;
                                default:
                                    throw new NoViableAltException(this);
                            }
                        }
                    }
                    setState(301);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 28, _ctx);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class OperationContext extends ParserRuleContext {
        public UnaryOpContext unaryOp() {
            return getRuleContext(UnaryOpContext.class, 0);
        }

        public BinaryOpContext binaryOp() {
            return getRuleContext(BinaryOpContext.class, 0);
        }

        public OperationContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_operation;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterOperation(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitOperation(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitOperation(this);
            else return visitor.visitChildren(this);
        }
    }

    public final OperationContext operation() throws RecognitionException {
        OperationContext _localctx = new OperationContext(_ctx, getState());
        enterRule(_localctx, 56, RULE_operation);
        try {
            setState(304);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 29, _ctx)) {
                case 1:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(302);
                    unaryOp();
                }
                break;
                case 2:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(303);
                    binaryOp();
                }
                break;
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class UnaryOpContext extends ParserRuleContext {
        public ExprTermContext exprTerm() {
            return getRuleContext(ExprTermContext.class, 0);
        }

        public TerminalNode MINUS() {
            return getToken(HCLParser.MINUS, 0);
        }

        public TerminalNode NOT() {
            return getToken(HCLParser.NOT, 0);
        }

        public UnaryOpContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_unaryOp;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterUnaryOp(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitUnaryOp(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitUnaryOp(this);
            else return visitor.visitChildren(this);
        }
    }

    public final UnaryOpContext unaryOp() throws RecognitionException {
        UnaryOpContext _localctx = new UnaryOpContext(_ctx, getState());
        enterRule(_localctx, 58, RULE_unaryOp);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(306);
                _la = _input.LA(1);
                if (!(_la == MINUS || _la == NOT)) {
                    _errHandler.recoverInline(this);
                } else {
                    if (_input.LA(1) == Token.EOF) matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
                setState(307);
                exprTerm(0);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class BinaryOpContext extends ParserRuleContext {
        public BinaryOperatorContext binaryOperator() {
            return getRuleContext(BinaryOperatorContext.class, 0);
        }

        public List<ExprTermContext> exprTerm() {
            return getRuleContexts(ExprTermContext.class);
        }

        public ExprTermContext exprTerm(int i) {
            return getRuleContext(ExprTermContext.class, i);
        }

        public UnaryOpContext unaryOp() {
            return getRuleContext(UnaryOpContext.class, 0);
        }

        public OperationContext operation() {
            return getRuleContext(OperationContext.class, 0);
        }

        public BinaryOpContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_binaryOp;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterBinaryOp(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitBinaryOp(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitBinaryOp(this);
            else return visitor.visitChildren(this);
        }
    }

    public final BinaryOpContext binaryOp() throws RecognitionException {
        BinaryOpContext _localctx = new BinaryOpContext(_ctx, getState());
        enterRule(_localctx, 60, RULE_binaryOp);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(311);
                _errHandler.sync(this);
                switch (_input.LA(1)) {
                    case FOR_BRACE:
                    case FOR_BRACK:
                    case BooleanLiteral:
                    case NULL:
                    case LBRACE:
                    case Identifier:
                    case NumericLiteral:
                    case QUOTE:
                    case HEREDOC_START:
                    case LBRACK:
                    case LPAREN: {
                        setState(309);
                        exprTerm(0);
                    }
                    break;
                    case MINUS:
                    case NOT: {
                        setState(310);
                        unaryOp();
                    }
                    break;
                    default:
                        throw new NoViableAltException(this);
                }
                setState(313);
                binaryOperator();
                setState(316);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 31, _ctx)) {
                    case 1: {
                        setState(314);
                        exprTerm(0);
                    }
                    break;
                    case 2: {
                        setState(315);
                        operation();
                    }
                    break;
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class BinaryOperatorContext extends ParserRuleContext {
        public CompareOperatorContext compareOperator() {
            return getRuleContext(CompareOperatorContext.class, 0);
        }

        public ArithmeticOperatorContext arithmeticOperator() {
            return getRuleContext(ArithmeticOperatorContext.class, 0);
        }

        public LogicOperatorContext logicOperator() {
            return getRuleContext(LogicOperatorContext.class, 0);
        }

        public BinaryOperatorContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_binaryOperator;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterBinaryOperator(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitBinaryOperator(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitBinaryOperator(this);
            else return visitor.visitChildren(this);
        }
    }

    public final BinaryOperatorContext binaryOperator() throws RecognitionException {
        BinaryOperatorContext _localctx = new BinaryOperatorContext(_ctx, getState());
        enterRule(_localctx, 62, RULE_binaryOperator);
        try {
            setState(321);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case EQ:
                case LT:
                case NEQ:
                case GT:
                case LEQ:
                case GEQ:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(318);
                    compareOperator();
                }
                break;
                case PLUS:
                case MINUS:
                case MUL:
                case DIV:
                case MOD:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(319);
                    arithmeticOperator();
                }
                break;
                case AND:
                case OR:
                    enterOuterAlt(_localctx, 3);
                {
                    setState(320);
                    logicOperator();
                }
                break;
                default:
                    throw new NoViableAltException(this);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class CompareOperatorContext extends ParserRuleContext {
        public TerminalNode EQ() {
            return getToken(HCLParser.EQ, 0);
        }

        public TerminalNode NEQ() {
            return getToken(HCLParser.NEQ, 0);
        }

        public TerminalNode LT() {
            return getToken(HCLParser.LT, 0);
        }

        public TerminalNode GT() {
            return getToken(HCLParser.GT, 0);
        }

        public TerminalNode LEQ() {
            return getToken(HCLParser.LEQ, 0);
        }

        public TerminalNode GEQ() {
            return getToken(HCLParser.GEQ, 0);
        }

        public CompareOperatorContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_compareOperator;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterCompareOperator(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitCompareOperator(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitCompareOperator(this);
            else return visitor.visitChildren(this);
        }
    }

    public final CompareOperatorContext compareOperator() throws RecognitionException {
        CompareOperatorContext _localctx = new CompareOperatorContext(_ctx, getState());
        enterRule(_localctx, 64, RULE_compareOperator);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(323);
                _la = _input.LA(1);
                if (!((((_la) & ~0x3f) == 0 && ((1L << _la) & 155024621568L) != 0))) {
                    _errHandler.recoverInline(this);
                } else {
                    if (_input.LA(1) == Token.EOF) matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ArithmeticOperatorContext extends ParserRuleContext {
        public TerminalNode PLUS() {
            return getToken(HCLParser.PLUS, 0);
        }

        public TerminalNode MINUS() {
            return getToken(HCLParser.MINUS, 0);
        }

        public TerminalNode MUL() {
            return getToken(HCLParser.MUL, 0);
        }

        public TerminalNode DIV() {
            return getToken(HCLParser.DIV, 0);
        }

        public TerminalNode MOD() {
            return getToken(HCLParser.MOD, 0);
        }

        public ArithmeticOperatorContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_arithmeticOperator;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterArithmeticOperator(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitArithmeticOperator(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitArithmeticOperator(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ArithmeticOperatorContext arithmeticOperator() throws RecognitionException {
        ArithmeticOperatorContext _localctx = new ArithmeticOperatorContext(_ctx, getState());
        enterRule(_localctx, 66, RULE_arithmeticOperator);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(325);
                _la = _input.LA(1);
                if (!((((_la) & ~0x3f) == 0 && ((1L << _la) & 1172559888384L) != 0))) {
                    _errHandler.recoverInline(this);
                } else {
                    if (_input.LA(1) == Token.EOF) matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class LogicOperatorContext extends ParserRuleContext {
        public TerminalNode AND() {
            return getToken(HCLParser.AND, 0);
        }

        public TerminalNode OR() {
            return getToken(HCLParser.OR, 0);
        }

        public LogicOperatorContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_logicOperator;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterLogicOperator(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitLogicOperator(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitLogicOperator(this);
            else return visitor.visitChildren(this);
        }
    }

    public final LogicOperatorContext logicOperator() throws RecognitionException {
        LogicOperatorContext _localctx = new LogicOperatorContext(_ctx, getState());
        enterRule(_localctx, 68, RULE_logicOperator);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(327);
                _la = _input.LA(1);
                if (!(_la == AND || _la == OR)) {
                    _errHandler.recoverInline(this);
                } else {
                    if (_input.LA(1) == Token.EOF) matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class TemplateExprContext extends ParserRuleContext {
        public TemplateExprContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_templateExpr;
        }

        public TemplateExprContext() {
        }

        public void copyFrom(TemplateExprContext ctx) {
            super.copyFrom(ctx);
        }
    }

    @SuppressWarnings("CheckReturnValue")
    public static class QuotedTemplateContext extends TemplateExprContext {
        public List<TerminalNode> QUOTE() {
            return getTokens(HCLParser.QUOTE);
        }

        public TerminalNode QUOTE(int i) {
            return getToken(HCLParser.QUOTE, i);
        }

        public List<QuotedTemplatePartContext> quotedTemplatePart() {
            return getRuleContexts(QuotedTemplatePartContext.class);
        }

        public QuotedTemplatePartContext quotedTemplatePart(int i) {
            return getRuleContext(QuotedTemplatePartContext.class, i);
        }

        public QuotedTemplateContext(TemplateExprContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterQuotedTemplate(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitQuotedTemplate(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitQuotedTemplate(this);
            else return visitor.visitChildren(this);
        }
    }

    @SuppressWarnings("CheckReturnValue")
    public static class HeredocContext extends TemplateExprContext {
        public TerminalNode HEREDOC_START() {
            return getToken(HCLParser.HEREDOC_START, 0);
        }

        public List<TerminalNode> Identifier() {
            return getTokens(HCLParser.Identifier);
        }

        public TerminalNode Identifier(int i) {
            return getToken(HCLParser.Identifier, i);
        }

        public List<TerminalNode> NEWLINE() {
            return getTokens(HCLParser.NEWLINE);
        }

        public TerminalNode NEWLINE(int i) {
            return getToken(HCLParser.NEWLINE, i);
        }

        public List<HeredocTemplatePartContext> heredocTemplatePart() {
            return getRuleContexts(HeredocTemplatePartContext.class);
        }

        public HeredocTemplatePartContext heredocTemplatePart(int i) {
            return getRuleContext(HeredocTemplatePartContext.class, i);
        }

        public HeredocContext(TemplateExprContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterHeredoc(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitHeredoc(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitHeredoc(this);
            else return visitor.visitChildren(this);
        }
    }

    public final TemplateExprContext templateExpr() throws RecognitionException {
        TemplateExprContext _localctx = new TemplateExprContext(_ctx, getState());
        enterRule(_localctx, 70, RULE_templateExpr);
        int _la;
        try {
            setState(351);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case HEREDOC_START:
                    _localctx = new HeredocContext(_localctx);
                    enterOuterAlt(_localctx, 1);
                {
                    setState(329);
                    match(HEREDOC_START);
                    setState(330);
                    match(Identifier);
                    setState(338);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    do {
                        {
                            {
                                setState(331);
                                match(NEWLINE);
                                setState(335);
                                _errHandler.sync(this);
                                _la = _input.LA(1);
                                while (_la == TEMPLATE_INTERPOLATION_START || _la == HTemplateLiteral) {
                                    {
                                        {
                                            setState(332);
                                            heredocTemplatePart();
                                        }
                                    }
                                    setState(337);
                                    _errHandler.sync(this);
                                    _la = _input.LA(1);
                                }
                            }
                        }
                        setState(340);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                    } while (_la == NEWLINE);
                    setState(342);
                    match(Identifier);
                }
                break;
                case QUOTE:
                    _localctx = new QuotedTemplateContext(_localctx);
                    enterOuterAlt(_localctx, 2);
                {
                    setState(343);
                    match(QUOTE);
                    setState(347);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    while (_la == TEMPLATE_INTERPOLATION_START || _la == TemplateStringLiteral) {
                        {
                            {
                                setState(344);
                                quotedTemplatePart();
                            }
                        }
                        setState(349);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                    }
                    setState(350);
                    match(QUOTE);
                }
                break;
                default:
                    throw new NoViableAltException(this);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class HeredocTemplatePartContext extends ParserRuleContext {
        public TemplateInterpolationContext templateInterpolation() {
            return getRuleContext(TemplateInterpolationContext.class, 0);
        }

        public HeredocLiteralContext heredocLiteral() {
            return getRuleContext(HeredocLiteralContext.class, 0);
        }

        public HeredocTemplatePartContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_heredocTemplatePart;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterHeredocTemplatePart(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitHeredocTemplatePart(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitHeredocTemplatePart(this);
            else return visitor.visitChildren(this);
        }
    }

    public final HeredocTemplatePartContext heredocTemplatePart() throws RecognitionException {
        HeredocTemplatePartContext _localctx = new HeredocTemplatePartContext(_ctx, getState());
        enterRule(_localctx, 72, RULE_heredocTemplatePart);
        try {
            setState(355);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case TEMPLATE_INTERPOLATION_START:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(353);
                    templateInterpolation();
                }
                break;
                case HTemplateLiteral:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(354);
                    heredocLiteral();
                }
                break;
                default:
                    throw new NoViableAltException(this);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class HeredocLiteralContext extends ParserRuleContext {
        public TerminalNode HTemplateLiteral() {
            return getToken(HCLParser.HTemplateLiteral, 0);
        }

        public HeredocLiteralContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_heredocLiteral;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterHeredocLiteral(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitHeredocLiteral(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitHeredocLiteral(this);
            else return visitor.visitChildren(this);
        }
    }

    public final HeredocLiteralContext heredocLiteral() throws RecognitionException {
        HeredocLiteralContext _localctx = new HeredocLiteralContext(_ctx, getState());
        enterRule(_localctx, 74, RULE_heredocLiteral);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(357);
                match(HTemplateLiteral);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class QuotedTemplatePartContext extends ParserRuleContext {
        public TemplateInterpolationContext templateInterpolation() {
            return getRuleContext(TemplateInterpolationContext.class, 0);
        }

        public StringLiteralContext stringLiteral() {
            return getRuleContext(StringLiteralContext.class, 0);
        }

        public QuotedTemplatePartContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_quotedTemplatePart;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterQuotedTemplatePart(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitQuotedTemplatePart(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitQuotedTemplatePart(this);
            else return visitor.visitChildren(this);
        }
    }

    public final QuotedTemplatePartContext quotedTemplatePart() throws RecognitionException {
        QuotedTemplatePartContext _localctx = new QuotedTemplatePartContext(_ctx, getState());
        enterRule(_localctx, 76, RULE_quotedTemplatePart);
        try {
            setState(361);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case TEMPLATE_INTERPOLATION_START:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(359);
                    templateInterpolation();
                }
                break;
                case TemplateStringLiteral:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(360);
                    stringLiteral();
                }
                break;
                default:
                    throw new NoViableAltException(this);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class StringLiteralContext extends ParserRuleContext {
        public TerminalNode TemplateStringLiteral() {
            return getToken(HCLParser.TemplateStringLiteral, 0);
        }

        public StringLiteralContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_stringLiteral;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterStringLiteral(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitStringLiteral(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitStringLiteral(this);
            else return visitor.visitChildren(this);
        }
    }

    public final StringLiteralContext stringLiteral() throws RecognitionException {
        StringLiteralContext _localctx = new StringLiteralContext(_ctx, getState());
        enterRule(_localctx, 78, RULE_stringLiteral);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(363);
                match(TemplateStringLiteral);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class TemplateInterpolationContext extends ParserRuleContext {
        public TerminalNode TEMPLATE_INTERPOLATION_START() {
            return getToken(HCLParser.TEMPLATE_INTERPOLATION_START, 0);
        }

        public ExpressionContext expression() {
            return getRuleContext(ExpressionContext.class, 0);
        }

        public TerminalNode RBRACE() {
            return getToken(HCLParser.RBRACE, 0);
        }

        public TemplateInterpolationContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_templateInterpolation;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterTemplateInterpolation(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitTemplateInterpolation(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor)
                return ((HCLParserVisitor<? extends T>) visitor).visitTemplateInterpolation(this);
            else return visitor.visitChildren(this);
        }
    }

    public final TemplateInterpolationContext templateInterpolation() throws RecognitionException {
        TemplateInterpolationContext _localctx = new TemplateInterpolationContext(_ctx, getState());
        enterRule(_localctx, 80, RULE_templateInterpolation);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(365);
                match(TEMPLATE_INTERPOLATION_START);
                setState(366);
                expression(0);
                setState(367);
                match(RBRACE);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @Override public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
        switch (ruleIndex) {
            case 6:
                return expression_sempred((ExpressionContext) _localctx, predIndex);
            case 7:
                return exprTerm_sempred((ExprTermContext) _localctx, predIndex);
        }
        return true;
    }

    private boolean expression_sempred(ExpressionContext _localctx, int predIndex) {
        switch (predIndex) {
            case 0:
                return precpred(_ctx, 1);
        }
        return true;
    }

    private boolean exprTerm_sempred(ExprTermContext _localctx, int predIndex) {
        switch (predIndex) {
            case 1:
                return precpred(_ctx, 5);
            case 2:
                return precpred(_ctx, 4);
            case 3:
                return precpred(_ctx, 3);
            case 4:
                return precpred(_ctx, 2);
        }
        return true;
    }

    public static final String _serializedATN =
            "\u0004\u0001/\u0172\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002" +
                    "\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002" +
                    "\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002" +
                    "\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b\u0002" +
                    "\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007\u000f" +
                    "\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007\u0012" +
                    "\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002\u0015\u0007\u0015" +
                    "\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017\u0002\u0018\u0007\u0018" +
                    "\u0002\u0019\u0007\u0019\u0002\u001a\u0007\u001a\u0002\u001b\u0007\u001b" +
                    "\u0002\u001c\u0007\u001c\u0002\u001d\u0007\u001d\u0002\u001e\u0007\u001e" +
                    "\u0002\u001f\u0007\u001f\u0002 \u0007 \u0002!\u0007!\u0002\"\u0007\"\u0002" +
                    "#\u0007#\u0002$\u0007$\u0002%\u0007%\u0002&\u0007&\u0002\'\u0007\'\u0002" +
                    "(\u0007(\u0001\u0000\u0001\u0000\u0001\u0001\u0005\u0001V\b\u0001\n\u0001" +
                    "\f\u0001Y\t\u0001\u0001\u0002\u0001\u0002\u0003\u0002]\b\u0002\u0001\u0003" +
                    "\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0004\u0001\u0004\u0005\u0004" +
                    "e\b\u0004\n\u0004\f\u0004h\t\u0004\u0001\u0004\u0001\u0004\u0001\u0005" +
                    "\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0003\u0005q\b\u0005" +
                    "\u0001\u0006\u0001\u0006\u0001\u0006\u0003\u0006v\b\u0006\u0001\u0006" +
                    "\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0005\u0006" +
                    "~\b\u0006\n\u0006\f\u0006\u0081\t\u0006\u0001\u0007\u0001\u0007\u0001" +
                    "\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001" +
                    "\u0007\u0001\u0007\u0001\u0007\u0003\u0007\u008e\b\u0007\u0001\u0007\u0001" +
                    "\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001" +
                    "\u0007\u0005\u0007\u0098\b\u0007\n\u0007\f\u0007\u009b\t\u0007\u0001\b" +
                    "\u0001\b\u0001\b\u0001\b\u0001\t\u0001\t\u0001\n\u0001\n\u0003\n\u00a5" +
                    "\b\n\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0005\u000b\u00ab" +
                    "\b\u000b\n\u000b\f\u000b\u00ae\t\u000b\u0001\u000b\u0003\u000b\u00b1\b" +
                    "\u000b\u0003\u000b\u00b3\b\u000b\u0001\u000b\u0001\u000b\u0001\f\u0001" +
                    "\f\u0005\f\u00b9\b\f\n\f\f\f\u00bc\t\f\u0001\f\u0001\f\u0001\r\u0001\r" +
                    "\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0005\r\u00c7\b\r\n\r\f\r\u00ca" +
                    "\t\r\u0001\r\u0001\r\u0003\r\u00ce\b\r\u0001\r\u0001\r\u0001\r\u0003\r" +
                    "\u00d3\b\r\u0001\u000e\u0001\u000e\u0003\u000e\u00d7\b\u000e\u0001\u000f" +
                    "\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0003\u000f\u00de\b\u000f" +
                    "\u0001\u000f\u0001\u000f\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010" +
                    "\u0001\u0010\u0001\u0010\u0001\u0010\u0003\u0010\u00e9\b\u0010\u0001\u0010" +
                    "\u0003\u0010\u00ec\b\u0010\u0001\u0010\u0001\u0010\u0001\u0011\u0001\u0011" +
                    "\u0001\u0011\u0003\u0011\u00f3\b\u0011\u0001\u0011\u0001\u0011\u0001\u0011" +
                    "\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0013\u0001\u0013\u0001\u0014" +
                    "\u0001\u0014\u0001\u0014\u0003\u0014\u0100\b\u0014\u0001\u0014\u0001\u0014" +
                    "\u0001\u0015\u0001\u0015\u0001\u0015\u0005\u0015\u0107\b\u0015\n\u0015" +
                    "\f\u0015\u010a\t\u0015\u0001\u0015\u0003\u0015\u010d\b\u0015\u0001\u0016" +
                    "\u0001\u0016\u0001\u0016\u0001\u0016\u0001\u0017\u0001\u0017\u0001\u0017" +
                    "\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0019\u0001\u0019\u0003\u0019" +
                    "\u011b\b\u0019\u0001\u001a\u0001\u001a\u0001\u001a\u0005\u001a\u0120\b" +
                    "\u001a\n\u001a\f\u001a\u0123\t\u001a\u0001\u001b\u0001\u001b\u0001\u001b" +
                    "\u0001\u001b\u0001\u001b\u0005\u001b\u012a\b\u001b\n\u001b\f\u001b\u012d" +
                    "\t\u001b\u0001\u001c\u0001\u001c\u0003\u001c\u0131\b\u001c\u0001\u001d" +
                    "\u0001\u001d\u0001\u001d\u0001\u001e\u0001\u001e\u0003\u001e\u0138\b\u001e" +
                    "\u0001\u001e\u0001\u001e\u0001\u001e\u0003\u001e\u013d\b\u001e\u0001\u001f" +
                    "\u0001\u001f\u0001\u001f\u0003\u001f\u0142\b\u001f\u0001 \u0001 \u0001" +
                    "!\u0001!\u0001\"\u0001\"\u0001#\u0001#\u0001#\u0001#\u0005#\u014e\b#\n" +
                    "#\f#\u0151\t#\u0004#\u0153\b#\u000b#\f#\u0154\u0001#\u0001#\u0001#\u0005" +
                    "#\u015a\b#\n#\f#\u015d\t#\u0001#\u0003#\u0160\b#\u0001$\u0001$\u0003$" +
                    "\u0164\b$\u0001%\u0001%\u0001&\u0001&\u0003&\u016a\b&\u0001\'\u0001\'" +
                    "\u0001(\u0001(\u0001(\u0001(\u0001(\u0000\u0002\f\u000e)\u0000\u0002\u0004" +
                    "\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u001c\u001e \"" +
                    "$&(*,.02468:<>@BDFHJLNP\u0000\b\u0002\u0000\u0006\u0006\n\n\u0002\u0000" +
                    "\u0005\u0006\u000f\u000f\u0002\u0000\t\t\u0016\u0016\u0002\u0000\'\')" +
                    ")\u0002\u0000\u0019\u0019!!\u0004\u0000\u0014\u0015\u001b\u001c\"\"%%" +
                    "\u0005\u0000\u0012\u0012\u0019\u0019  $$((\u0002\u0000\u0013\u0013\u001a" +
                    "\u001a\u017a\u0000R\u0001\u0000\u0000\u0000\u0002W\u0001\u0000\u0000\u0000" +
                    "\u0004\\\u0001\u0000\u0000\u0000\u0006^\u0001\u0000\u0000\u0000\bb\u0001" +
                    "\u0000\u0000\u0000\np\u0001\u0000\u0000\u0000\fu\u0001\u0000\u0000\u0000" +
                    "\u000e\u008d\u0001\u0000\u0000\u0000\u0010\u009c\u0001\u0000\u0000\u0000" +
                    "\u0012\u00a0\u0001\u0000\u0000\u0000\u0014\u00a4\u0001\u0000\u0000\u0000" +
                    "\u0016\u00a6\u0001\u0000\u0000\u0000\u0018\u00b6\u0001\u0000\u0000\u0000" +
                    "\u001a\u00cd\u0001\u0000\u0000\u0000\u001c\u00d6\u0001\u0000\u0000\u0000" +
                    "\u001e\u00d8\u0001\u0000\u0000\u0000 \u00e1\u0001\u0000\u0000\u0000\"" +
                    "\u00ef\u0001\u0000\u0000\u0000$\u00f7\u0001\u0000\u0000\u0000&\u00fa\u0001" +
                    "\u0000\u0000\u0000(\u00fc\u0001\u0000\u0000\u0000*\u0103\u0001\u0000\u0000" +
                    "\u0000,\u010e\u0001\u0000\u0000\u0000.\u0112\u0001\u0000\u0000\u00000" +
                    "\u0115\u0001\u0000\u0000\u00002\u011a\u0001\u0000\u0000\u00004\u011c\u0001" +
                    "\u0000\u0000\u00006\u0124\u0001\u0000\u0000\u00008\u0130\u0001\u0000\u0000" +
                    "\u0000:\u0132\u0001\u0000\u0000\u0000<\u0137\u0001\u0000\u0000\u0000>" +
                    "\u0141\u0001\u0000\u0000\u0000@\u0143\u0001\u0000\u0000\u0000B\u0145\u0001" +
                    "\u0000\u0000\u0000D\u0147\u0001\u0000\u0000\u0000F\u015f\u0001\u0000\u0000" +
                    "\u0000H\u0163\u0001\u0000\u0000\u0000J\u0165\u0001\u0000\u0000\u0000L" +
                    "\u0169\u0001\u0000\u0000\u0000N\u016b\u0001\u0000\u0000\u0000P\u016d\u0001" +
                    "\u0000\u0000\u0000RS\u0003\u0002\u0001\u0000S\u0001\u0001\u0000\u0000" +
                    "\u0000TV\u0003\u0004\u0002\u0000UT\u0001\u0000\u0000\u0000VY\u0001\u0000" +
                    "\u0000\u0000WU\u0001\u0000\u0000\u0000WX\u0001\u0000\u0000\u0000X\u0003" +
                    "\u0001\u0000\u0000\u0000YW\u0001\u0000\u0000\u0000Z]\u0003\u0006\u0003" +
                    "\u0000[]\u0003\b\u0004\u0000\\Z\u0001\u0000\u0000\u0000\\[\u0001\u0000" +
                    "\u0000\u0000]\u0005\u0001\u0000\u0000\u0000^_\u0007\u0000\u0000\u0000" +
                    "_`\u0005\t\u0000\u0000`a\u0003\f\u0006\u0000a\u0007\u0001\u0000\u0000" +
                    "\u0000bf\u0005\n\u0000\u0000ce\u0003\n\u0005\u0000dc\u0001\u0000\u0000" +
                    "\u0000eh\u0001\u0000\u0000\u0000fd\u0001\u0000\u0000\u0000fg\u0001\u0000" +
                    "\u0000\u0000gi\u0001\u0000\u0000\u0000hf\u0001\u0000\u0000\u0000ij\u0003" +
                    "\u0010\b\u0000j\t\u0001\u0000\u0000\u0000kl\u0005\u0010\u0000\u0000lm" +
                    "\u0003N\'\u0000mn\u0005\u0010\u0000\u0000nq\u0001\u0000\u0000\u0000oq" +
                    "\u0005\n\u0000\u0000pk\u0001\u0000\u0000\u0000po\u0001\u0000\u0000\u0000" +
                    "q\u000b\u0001\u0000\u0000\u0000rs\u0006\u0006\uffff\uffff\u0000sv\u0003" +
                    "\u000e\u0007\u0000tv\u00038\u001c\u0000ur\u0001\u0000\u0000\u0000ut\u0001" +
                    "\u0000\u0000\u0000v\u007f\u0001\u0000\u0000\u0000wx\n\u0001\u0000\u0000" +
                    "xy\u0005\u001d\u0000\u0000yz\u0003\f\u0006\u0000z{\u0005\u0016\u0000\u0000" +
                    "{|\u0003\f\u0006\u0002|~\u0001\u0000\u0000\u0000}w\u0001\u0000\u0000\u0000" +
                    "~\u0081\u0001\u0000\u0000\u0000\u007f}\u0001\u0000\u0000\u0000\u007f\u0080" +
                    "\u0001\u0000\u0000\u0000\u0080\r\u0001\u0000\u0000\u0000\u0081\u007f\u0001" +
                    "\u0000\u0000\u0000\u0082\u0083\u0006\u0007\uffff\uffff\u0000\u0083\u008e" +
                    "\u0003F#\u0000\u0084\u008e\u0003\u0012\t\u0000\u0085\u008e\u0003\u001c" +
                    "\u000e\u0000\u0086\u008e\u0003\u0014\n\u0000\u0087\u008e\u0003&\u0013" +
                    "\u0000\u0088\u008e\u0003(\u0014\u0000\u0089\u008a\u0005\u0018\u0000\u0000" +
                    "\u008a\u008b\u0003\f\u0006\u0000\u008b\u008c\u0005\u001f\u0000\u0000\u008c" +
                    "\u008e\u0001\u0000\u0000\u0000\u008d\u0082\u0001\u0000\u0000\u0000\u008d" +
                    "\u0084\u0001\u0000\u0000\u0000\u008d\u0085\u0001\u0000\u0000\u0000\u008d" +
                    "\u0086\u0001\u0000\u0000\u0000\u008d\u0087\u0001\u0000\u0000\u0000\u008d" +
                    "\u0088\u0001\u0000\u0000\u0000\u008d\u0089\u0001\u0000\u0000\u0000\u008e" +
                    "\u0099\u0001\u0000\u0000\u0000\u008f\u0090\n\u0005\u0000\u0000\u0090\u0098" +
                    "\u0003,\u0016\u0000\u0091\u0092\n\u0004\u0000\u0000\u0092\u0098\u0003" +
                    ".\u0017\u0000\u0093\u0094\n\u0003\u0000\u0000\u0094\u0098\u00030\u0018" +
                    "\u0000\u0095\u0096\n\u0002\u0000\u0000\u0096\u0098\u00032\u0019\u0000" +
                    "\u0097\u008f\u0001\u0000\u0000\u0000\u0097\u0091\u0001\u0000\u0000\u0000" +
                    "\u0097\u0093\u0001\u0000\u0000\u0000\u0097\u0095\u0001\u0000\u0000\u0000" +
                    "\u0098\u009b\u0001\u0000\u0000\u0000\u0099\u0097\u0001\u0000\u0000\u0000" +
                    "\u0099\u009a\u0001\u0000\u0000\u0000\u009a\u000f\u0001\u0000\u0000\u0000" +
                    "\u009b\u0099\u0001\u0000\u0000\u0000\u009c\u009d\u0005\u0007\u0000\u0000" +
                    "\u009d\u009e\u0003\u0002\u0001\u0000\u009e\u009f\u0005\b\u0000\u0000\u009f" +
                    "\u0011\u0001\u0000\u0000\u0000\u00a0\u00a1\u0007\u0001\u0000\u0000\u00a1" +
                    "\u0013\u0001\u0000\u0000\u0000\u00a2\u00a5\u0003\u0016\u000b\u0000\u00a3" +
                    "\u00a5\u0003\u0018\f\u0000\u00a4\u00a2\u0001\u0000\u0000\u0000\u00a4\u00a3" +
                    "\u0001\u0000\u0000\u0000\u00a5\u0015\u0001\u0000\u0000\u0000\u00a6\u00b2" +
                    "\u0005\u0017\u0000\u0000\u00a7\u00ac\u0003\f\u0006\u0000\u00a8\u00a9\u0005" +
                    "\'\u0000\u0000\u00a9\u00ab\u0003\f\u0006\u0000\u00aa\u00a8\u0001\u0000" +
                    "\u0000\u0000\u00ab\u00ae\u0001\u0000\u0000\u0000\u00ac\u00aa\u0001\u0000" +
                    "\u0000\u0000\u00ac\u00ad\u0001\u0000\u0000\u0000\u00ad\u00b0\u0001\u0000" +
                    "\u0000\u0000\u00ae\u00ac\u0001\u0000\u0000\u0000\u00af\u00b1\u0005\'\u0000" +
                    "\u0000\u00b0\u00af\u0001\u0000\u0000\u0000\u00b0\u00b1\u0001\u0000\u0000" +
                    "\u0000\u00b1\u00b3\u0001\u0000\u0000\u0000\u00b2\u00a7\u0001\u0000\u0000" +
                    "\u0000\u00b2\u00b3\u0001\u0000\u0000\u0000\u00b3\u00b4\u0001\u0000\u0000" +
                    "\u0000\u00b4\u00b5\u0005\u001e\u0000\u0000\u00b5\u0017\u0001\u0000\u0000" +
                    "\u0000\u00b6\u00ba\u0005\u0007\u0000\u0000\u00b7\u00b9\u0003\u001a\r\u0000" +
                    "\u00b8\u00b7\u0001\u0000\u0000\u0000\u00b9\u00bc\u0001\u0000\u0000\u0000" +
                    "\u00ba\u00b8\u0001\u0000\u0000\u0000\u00ba\u00bb\u0001\u0000\u0000\u0000" +
                    "\u00bb\u00bd\u0001\u0000\u0000\u0000\u00bc\u00ba\u0001\u0000\u0000\u0000" +
                    "\u00bd\u00be\u0005\b\u0000\u0000\u00be\u0019\u0001\u0000\u0000\u0000\u00bf" +
                    "\u00ce\u0005\n\u0000\u0000\u00c0\u00ce\u0005\u0006\u0000\u0000\u00c1\u00c2" +
                    "\u0005\u0018\u0000\u0000\u00c2\u00c3\u0005\n\u0000\u0000\u00c3\u00ce\u0005" +
                    "\u001f\u0000\u0000\u00c4\u00c8\u0005\u0010\u0000\u0000\u00c5\u00c7\u0003" +
                    "L&\u0000\u00c6\u00c5\u0001\u0000\u0000\u0000\u00c7\u00ca\u0001\u0000\u0000" +
                    "\u0000\u00c8\u00c6\u0001\u0000\u0000\u0000\u00c8\u00c9\u0001\u0000\u0000" +
                    "\u0000\u00c9\u00cb\u0001\u0000\u0000\u0000\u00ca\u00c8\u0001\u0000\u0000" +
                    "\u0000\u00cb\u00ce\u0005\u0010\u0000\u0000\u00cc\u00ce\u0003\f\u0006\u0000" +
                    "\u00cd\u00bf\u0001\u0000\u0000\u0000\u00cd\u00c0\u0001\u0000\u0000\u0000" +
                    "\u00cd\u00c1\u0001\u0000\u0000\u0000\u00cd\u00c4\u0001\u0000\u0000\u0000" +
                    "\u00cd\u00cc\u0001\u0000\u0000\u0000\u00ce\u00cf\u0001\u0000\u0000\u0000" +
                    "\u00cf\u00d0\u0007\u0002\u0000\u0000\u00d0\u00d2\u0003\f\u0006\u0000\u00d1" +
                    "\u00d3\u0005\'\u0000\u0000\u00d2\u00d1\u0001\u0000\u0000\u0000\u00d2\u00d3" +
                    "\u0001\u0000\u0000\u0000\u00d3\u001b\u0001\u0000\u0000\u0000\u00d4\u00d7" +
                    "\u0003\u001e\u000f\u0000\u00d5\u00d7\u0003 \u0010\u0000\u00d6\u00d4\u0001" +
                    "\u0000\u0000\u0000\u00d6\u00d5\u0001\u0000\u0000\u0000\u00d7\u001d\u0001" +
                    "\u0000\u0000\u0000\u00d8\u00d9\u0005\u0002\u0000\u0000\u00d9\u00da\u0003" +
                    "\"\u0011\u0000\u00da\u00db\u0005\u0016\u0000\u0000\u00db\u00dd\u0003\f" +
                    "\u0006\u0000\u00dc\u00de\u0003$\u0012\u0000\u00dd\u00dc\u0001\u0000\u0000" +
                    "\u0000\u00dd\u00de\u0001\u0000\u0000\u0000\u00de\u00df\u0001\u0000\u0000" +
                    "\u0000\u00df\u00e0\u0005\u001e\u0000\u0000\u00e0\u001f\u0001\u0000\u0000" +
                    "\u0000\u00e1\u00e2\u0005\u0001\u0000\u0000\u00e2\u00e3\u0003\"\u0011\u0000" +
                    "\u00e3\u00e4\u0005\u0016\u0000\u0000\u00e4\u00e5\u0003\f\u0006\u0000\u00e5" +
                    "\u00e6\u0005&\u0000\u0000\u00e6\u00e8\u0003\f\u0006\u0000\u00e7\u00e9" +
                    "\u0005)\u0000\u0000\u00e8\u00e7\u0001\u0000\u0000\u0000\u00e8\u00e9\u0001" +
                    "\u0000\u0000\u0000\u00e9\u00eb\u0001\u0000\u0000\u0000\u00ea\u00ec\u0003" +
                    "$\u0012\u0000\u00eb\u00ea\u0001\u0000\u0000\u0000\u00eb\u00ec\u0001\u0000" +
                    "\u0000\u0000\u00ec\u00ed\u0001\u0000\u0000\u0000\u00ed\u00ee\u0005\b\u0000" +
                    "\u0000\u00ee!\u0001\u0000\u0000\u0000\u00ef\u00f2\u0005\n\u0000\u0000" +
                    "\u00f0\u00f1\u0005\'\u0000\u0000\u00f1\u00f3\u0005\n\u0000\u0000\u00f2" +
                    "\u00f0\u0001\u0000\u0000\u0000\u00f2\u00f3\u0001\u0000\u0000\u0000\u00f3" +
                    "\u00f4\u0001\u0000\u0000\u0000\u00f4\u00f5\u0005\u0004\u0000\u0000\u00f5" +
                    "\u00f6\u0003\f\u0006\u0000\u00f6#\u0001\u0000\u0000\u0000\u00f7\u00f8" +
                    "\u0005\u0003\u0000\u0000\u00f8\u00f9\u0003\f\u0006\u0000\u00f9%\u0001" +
                    "\u0000\u0000\u0000\u00fa\u00fb\u0005\n\u0000\u0000\u00fb\'\u0001\u0000" +
                    "\u0000\u0000\u00fc\u00fd\u0005\n\u0000\u0000\u00fd\u00ff\u0005\u0018\u0000" +
                    "\u0000\u00fe\u0100\u0003*\u0015\u0000\u00ff\u00fe\u0001\u0000\u0000\u0000" +
                    "\u00ff\u0100\u0001\u0000\u0000\u0000\u0100\u0101\u0001\u0000\u0000\u0000" +
                    "\u0101\u0102\u0005\u001f\u0000\u0000\u0102)\u0001\u0000\u0000\u0000\u0103" +
                    "\u0108\u0003\f\u0006\u0000\u0104\u0105\u0005\'\u0000\u0000\u0105\u0107" +
                    "\u0003\f\u0006\u0000\u0106\u0104\u0001\u0000\u0000\u0000\u0107\u010a\u0001" +
                    "\u0000\u0000\u0000\u0108\u0106\u0001\u0000\u0000\u0000\u0108\u0109\u0001" +
                    "\u0000\u0000\u0000\u0109\u010c\u0001\u0000\u0000\u0000\u010a\u0108\u0001" +
                    "\u0000\u0000\u0000\u010b\u010d\u0007\u0003\u0000\u0000\u010c\u010b\u0001" +
                    "\u0000\u0000\u0000\u010c\u010d\u0001\u0000\u0000\u0000\u010d+\u0001\u0000" +
                    "\u0000\u0000\u010e\u010f\u0005\u0017\u0000\u0000\u010f\u0110\u0003\f\u0006" +
                    "\u0000\u0110\u0111\u0005\u001e\u0000\u0000\u0111-\u0001\u0000\u0000\u0000" +
                    "\u0112\u0113\u0005#\u0000\u0000\u0113\u0114\u0005\n\u0000\u0000\u0114" +
                    "/\u0001\u0000\u0000\u0000\u0115\u0116\u0005#\u0000\u0000\u0116\u0117\u0005" +
                    "\u000f\u0000\u0000\u01171\u0001\u0000\u0000\u0000\u0118\u011b\u00034\u001a" +
                    "\u0000\u0119\u011b\u00036\u001b\u0000\u011a\u0118\u0001\u0000\u0000\u0000" +
                    "\u011a\u0119\u0001\u0000\u0000\u0000\u011b3\u0001\u0000\u0000\u0000\u011c" +
                    "\u011d\u0005#\u0000\u0000\u011d\u0121\u0005 \u0000\u0000\u011e\u0120\u0003" +
                    ".\u0017\u0000\u011f\u011e\u0001\u0000\u0000\u0000\u0120\u0123\u0001\u0000" +
                    "\u0000\u0000\u0121\u011f\u0001\u0000\u0000\u0000\u0121\u0122\u0001\u0000" +
                    "\u0000\u0000\u01225\u0001\u0000\u0000\u0000\u0123\u0121\u0001\u0000\u0000" +
                    "\u0000\u0124\u0125\u0005\u0017\u0000\u0000\u0125\u0126\u0005 \u0000\u0000" +
                    "\u0126\u012b\u0005\u001e\u0000\u0000\u0127\u012a\u0003.\u0017\u0000\u0128" +
                    "\u012a\u0003,\u0016\u0000\u0129\u0127\u0001\u0000\u0000\u0000\u0129\u0128" +
                    "\u0001\u0000\u0000\u0000\u012a\u012d\u0001\u0000\u0000\u0000\u012b\u0129" +
                    "\u0001\u0000\u0000\u0000\u012b\u012c\u0001\u0000\u0000\u0000\u012c7\u0001" +
                    "\u0000\u0000\u0000\u012d\u012b\u0001\u0000\u0000\u0000\u012e\u0131\u0003" +
                    ":\u001d\u0000\u012f\u0131\u0003<\u001e\u0000\u0130\u012e\u0001\u0000\u0000" +
                    "\u0000\u0130\u012f\u0001\u0000\u0000\u0000\u01319\u0001\u0000\u0000\u0000" +
                    "\u0132\u0133\u0007\u0004\u0000\u0000\u0133\u0134\u0003\u000e\u0007\u0000" +
                    "\u0134;\u0001\u0000\u0000\u0000\u0135\u0138\u0003\u000e\u0007\u0000\u0136" +
                    "\u0138\u0003:\u001d\u0000\u0137\u0135\u0001\u0000\u0000\u0000\u0137\u0136" +
                    "\u0001\u0000\u0000\u0000\u0138\u0139\u0001\u0000\u0000\u0000\u0139\u013c" +
                    "\u0003>\u001f\u0000\u013a\u013d\u0003\u000e\u0007\u0000\u013b\u013d\u0003" +
                    "8\u001c\u0000\u013c\u013a\u0001\u0000\u0000\u0000\u013c\u013b\u0001\u0000" +
                    "\u0000\u0000\u013d=\u0001\u0000\u0000\u0000\u013e\u0142\u0003@ \u0000" +
                    "\u013f\u0142\u0003B!\u0000\u0140\u0142\u0003D\"\u0000\u0141\u013e\u0001" +
                    "\u0000\u0000\u0000\u0141\u013f\u0001\u0000\u0000\u0000\u0141\u0140\u0001" +
                    "\u0000\u0000\u0000\u0142?\u0001\u0000\u0000\u0000\u0143\u0144\u0007\u0005" +
                    "\u0000\u0000\u0144A\u0001\u0000\u0000\u0000\u0145\u0146\u0007\u0006\u0000" +
                    "\u0000\u0146C\u0001\u0000\u0000\u0000\u0147\u0148\u0007\u0007\u0000\u0000" +
                    "\u0148E\u0001\u0000\u0000\u0000\u0149\u014a\u0005\u0011\u0000\u0000\u014a" +
                    "\u0152\u0005\n\u0000\u0000\u014b\u014f\u0005\u000e\u0000\u0000\u014c\u014e" +
                    "\u0003H$\u0000\u014d\u014c\u0001\u0000\u0000\u0000\u014e\u0151\u0001\u0000" +
                    "\u0000\u0000\u014f\u014d\u0001\u0000\u0000\u0000\u014f\u0150\u0001\u0000" +
                    "\u0000\u0000\u0150\u0153\u0001\u0000\u0000\u0000\u0151\u014f\u0001\u0000" +
                    "\u0000\u0000\u0152\u014b\u0001\u0000\u0000\u0000\u0153\u0154\u0001\u0000" +
                    "\u0000\u0000\u0154\u0152\u0001\u0000\u0000\u0000\u0154\u0155\u0001\u0000" +
                    "\u0000\u0000\u0155\u0156\u0001\u0000\u0000\u0000\u0156\u0160\u0005\n\u0000" +
                    "\u0000\u0157\u015b\u0005\u0010\u0000\u0000\u0158\u015a\u0003L&\u0000\u0159" +
                    "\u0158\u0001\u0000\u0000\u0000\u015a\u015d\u0001\u0000\u0000\u0000\u015b" +
                    "\u0159\u0001\u0000\u0000\u0000\u015b\u015c\u0001\u0000\u0000\u0000\u015c" +
                    "\u015e\u0001\u0000\u0000\u0000\u015d\u015b\u0001\u0000\u0000\u0000\u015e" +
                    "\u0160\u0005\u0010\u0000\u0000\u015f\u0149\u0001\u0000\u0000\u0000\u015f" +
                    "\u0157\u0001\u0000\u0000\u0000\u0160G\u0001\u0000\u0000\u0000\u0161\u0164" +
                    "\u0003P(\u0000\u0162\u0164\u0003J%\u0000\u0163\u0161\u0001\u0000\u0000" +
                    "\u0000\u0163\u0162\u0001\u0000\u0000\u0000\u0164I\u0001\u0000\u0000\u0000" +
                    "\u0165\u0166\u0005.\u0000\u0000\u0166K\u0001\u0000\u0000\u0000\u0167\u016a" +
                    "\u0003P(\u0000\u0168\u016a\u0003N\'\u0000\u0169\u0167\u0001\u0000\u0000" +
                    "\u0000\u0169\u0168\u0001\u0000\u0000\u0000\u016aM\u0001\u0000\u0000\u0000" +
                    "\u016b\u016c\u0005,\u0000\u0000\u016cO\u0001\u0000\u0000\u0000\u016d\u016e" +
                    "\u0005+\u0000\u0000\u016e\u016f\u0003\f\u0006\u0000\u016f\u0170\u0005" +
                    "\b\u0000\u0000\u0170Q\u0001\u0000\u0000\u0000\'W\\fpu\u007f\u008d\u0097" +
                    "\u0099\u00a4\u00ac\u00b0\u00b2\u00ba\u00c8\u00cd\u00d2\u00d6\u00dd\u00e8" +
                    "\u00eb\u00f2\u00ff\u0108\u010c\u011a\u0121\u0129\u012b\u0130\u0137\u013c" +
                    "\u0141\u014f\u0154\u015b\u015f\u0163\u0169";
    public static final ATN _ATN =
            new ATNDeserializer().deserialize(_serializedATN.toCharArray());

    static {
        _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
        for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
            _decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
        }
    }
}
