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
// Generated from /Users/yoshi/Development/Repos/openrewrite/rewrite/rewrite-hcl/src/main/antlr/HCLParser.g4 by ANTLR 4.9.3
package org.openrewrite.hcl.internal.grammar;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class HCLParser extends Parser {
    static {
        RuntimeMetaData.checkVersion("4.9.3", RuntimeMetaData.VERSION);
    }

    protected static final DFA[] _decisionToDFA;
    protected static final PredictionContextCache _sharedContextCache =
            new PredictionContextCache();
    public static final int
            FOR_BRACE = 1, FOR_BRACK = 2, IF = 3, IN = 4, LBRACE = 5, RBRACE = 6, ASSIGN = 7, Identifier = 8,
            WS = 9, COMMENT = 10, LINE_COMMENT = 11, NEWLINE = 12, NumericLiteral = 13, BooleanLiteral = 14,
            QUOTE = 15, NULL = 16, HEREDOC_START = 17, PLUS = 18, AND = 19, EQ = 20, LT = 21, COLON = 22,
            LBRACK = 23, LPAREN = 24, MINUS = 25, OR = 26, NEQ = 27, GT = 28, QUESTION = 29, RBRACK = 30,
            RPAREN = 31, MUL = 32, NOT = 33, LEQ = 34, DOT = 35, DIV = 36, GEQ = 37, ARROW = 38, COMMA = 39,
            MOD = 40, ELLIPSIS = 41, TILDE = 42, TEMPLATE_INTERPOLATION_START = 43, TemplateStringLiteral = 44,
            TemplateStringLiteralChar = 45, HTemplateLiteral = 46, HTemplateLiteralChar = 47;
    public static final int
            RULE_configFile = 0, RULE_body = 1, RULE_bodyContent = 2, RULE_attribute = 3,
            RULE_block = 4, RULE_blockLabel = 5, RULE_expression = 6, RULE_exprTerm = 7,
            RULE_blockExpr = 8, RULE_literalValue = 9, RULE_collectionValue = 10,
            RULE_tuple = 11, RULE_object = 12, RULE_objectelem = 13, RULE_forExpr = 14,
            RULE_forTupleExpr = 15, RULE_forObjectExpr = 16, RULE_forIntro = 17, RULE_forCond = 18,
            RULE_variableExpr = 19, RULE_functionCall = 20, RULE_arguments = 21, RULE_index = 22,
            RULE_getAttr = 23, RULE_splat = 24, RULE_attrSplat = 25, RULE_fullSplat = 26,
            RULE_operation = 27, RULE_unaryOp = 28, RULE_binaryOp = 29, RULE_binaryOperator = 30,
            RULE_compareOperator = 31, RULE_arithmeticOperator = 32, RULE_logicOperator = 33,
            RULE_templateExpr = 34, RULE_heredocTemplatePart = 35, RULE_heredocLiteral = 36,
            RULE_quotedTemplatePart = 37, RULE_stringLiteral = 38, RULE_templateInterpolation = 39;

    private static String[] makeRuleNames() {
        return new String[]{
                "configFile", "body", "bodyContent", "attribute", "block", "blockLabel",
                "expression", "exprTerm", "blockExpr", "literalValue", "collectionValue",
                "tuple", "object", "objectelem", "forExpr", "forTupleExpr", "forObjectExpr",
                "forIntro", "forCond", "variableExpr", "functionCall", "arguments", "index",
                "getAttr", "splat", "attrSplat", "fullSplat", "operation", "unaryOp",
                "binaryOp", "binaryOperator", "compareOperator", "arithmeticOperator",
                "logicOperator", "templateExpr", "heredocTemplatePart", "heredocLiteral",
                "quotedTemplatePart", "stringLiteral", "templateInterpolation"
        };
    }
    public static final String[] ruleNames = makeRuleNames();

    private static String[] makeLiteralNames() {
        return new String[]{
                null, null, null, "'if'", "'in'", "'{'", "'}'", "'='", null, null, null,
                null, null, null, null, null, "'null'", null, "'+'", "'&&'", "'=='",
                "'<'", "':'", "'['", "'('", "'-'", "'||'", "'!='", "'>'", "'?'", "']'",
                "')'", "'*'", "'!'", "'<='", "'.'", "'/'", "'>='", "'=>'", "','", "'%'",
                "'...'", "'~'"
        };
    }
    private static final String[] _LITERAL_NAMES = makeLiteralNames();

    private static String[] makeSymbolicNames() {
        return new String[]{
                null, "FOR_BRACE", "FOR_BRACK", "IF", "IN", "LBRACE", "RBRACE", "ASSIGN",
                "Identifier", "WS", "COMMENT", "LINE_COMMENT", "NEWLINE", "NumericLiteral",
                "BooleanLiteral", "QUOTE", "NULL", "HEREDOC_START", "PLUS", "AND", "EQ",
                "LT", "COLON", "LBRACK", "LPAREN", "MINUS", "OR", "NEQ", "GT", "QUESTION",
                "RBRACK", "RPAREN", "MUL", "NOT", "LEQ", "DOT", "DIV", "GEQ", "ARROW",
                "COMMA", "MOD", "ELLIPSIS", "TILDE", "TEMPLATE_INTERPOLATION_START",
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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitConfigFile(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ConfigFileContext configFile() throws RecognitionException {
        ConfigFileContext _localctx = new ConfigFileContext(_ctx, getState());
        enterRule(_localctx, 0, RULE_configFile);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(80);
                body();
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
                setState(85);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == Identifier) {
                    {
                        {
                            setState(82);
                            bodyContent();
                        }
                    }
                    setState(87);
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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitBodyContent(this);
            else return visitor.visitChildren(this);
        }
    }

    public final BodyContentContext bodyContent() throws RecognitionException {
        BodyContentContext _localctx = new BodyContentContext(_ctx, getState());
        enterRule(_localctx, 4, RULE_bodyContent);
        try {
            setState(90);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 1, _ctx)) {
                case 1:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(88);
                    attribute();
                }
                    break;
                case 2:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(89);
                    block();
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

    public static class AttributeContext extends ParserRuleContext {
        public TerminalNode Identifier() {
            return getToken(HCLParser.Identifier, 0);
        }

        public TerminalNode ASSIGN() {
            return getToken(HCLParser.ASSIGN, 0);
        }

        public ExpressionContext expression() {
            return getRuleContext(ExpressionContext.class, 0);
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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitAttribute(this);
            else return visitor.visitChildren(this);
        }
    }

    public final AttributeContext attribute() throws RecognitionException {
        AttributeContext _localctx = new AttributeContext(_ctx, getState());
        enterRule(_localctx, 6, RULE_attribute);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(92);
                match(Identifier);
                setState(93);
                match(ASSIGN);
                setState(94);
                expression(0);
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
                setState(96);
                match(Identifier);
                setState(100);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == Identifier || _la == QUOTE) {
                    {
                        {
                            setState(97);
                            blockLabel();
                        }
                    }
                    setState(102);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
                setState(103);
                blockExpr();
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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitBlockLabel(this);
            else return visitor.visitChildren(this);
        }
    }

    public final BlockLabelContext blockLabel() throws RecognitionException {
        BlockLabelContext _localctx = new BlockLabelContext(_ctx, getState());
        enterRule(_localctx, 10, RULE_blockLabel);
        try {
            setState(110);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case QUOTE:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(105);
                    match(QUOTE);
                    setState(106);
                    stringLiteral();
                    setState(107);
                    match(QUOTE);
                }
                    break;
                case Identifier:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(109);
                    match(Identifier);
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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitOperationExpression(this);
            else return visitor.visitChildren(this);
        }
    }

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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitConditionalExpression(this);
            else return visitor.visitChildren(this);
        }
    }

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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitExpressionTerm(this);
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
        ExpressionContext _prevctx = _localctx;
        int _startState = 12;
        enterRecursionRule(_localctx, 12, RULE_expression, _p);
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(115);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 4, _ctx)) {
                    case 1:
                    {
                        _localctx = new ExpressionTermContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;

                        setState(113);
                        exprTerm(0);
                    }
                        break;
                    case 2:
                    {
                        _localctx = new OperationExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(114);
                        operation();
                    }
                        break;
                }
                _ctx.stop = _input.LT(-1);
                setState(125);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 5, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        if (_parseListeners != null) triggerExitRuleEvent();
                        _prevctx = _localctx;
                        {
                            {
                                _localctx = new ConditionalExpressionContext(new ExpressionContext(_parentctx, _parentState));
                                pushNewRecursionContext(_localctx, _startState, RULE_expression);
                                setState(117);
                                if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
                                setState(118);
                                match(QUESTION);
                                setState(119);
                                expression(0);
                                setState(120);
                                match(COLON);
                                setState(121);
                                expression(2);
                            }
                        }
                    }
                    setState(127);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 5, _ctx);
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
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterParentheticalExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitParentheticalExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitParentheticalExpression(this);
            else return visitor.visitChildren(this);
        }
    }

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
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterAttributeAccessExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitAttributeAccessExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitAttributeAccessExpression(this);
            else return visitor.visitChildren(this);
        }
    }

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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitLiteralExpression(this);
            else return visitor.visitChildren(this);
        }
    }

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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitTemplateExpression(this);
            else return visitor.visitChildren(this);
        }
    }

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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitVariableExpression(this);
            else return visitor.visitChildren(this);
        }
    }

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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitSplatExpression(this);
            else return visitor.visitChildren(this);
        }
    }

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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitIndexAccessExpression(this);
            else return visitor.visitChildren(this);
        }
    }

    public static class BlockExpressionContext extends ExprTermContext {
        public BlockExprContext blockExpr() {
            return getRuleContext(BlockExprContext.class, 0);
        }

        public BlockExpressionContext(ExprTermContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterBlockExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitBlockExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitBlockExpression(this);
            else return visitor.visitChildren(this);
        }
    }

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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitForExpression(this);
            else return visitor.visitChildren(this);
        }
    }

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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitFunctionCallExpression(this);
            else return visitor.visitChildren(this);
        }
    }

    public static class CollectionValueExpressionContext extends ExprTermContext {
        public CollectionValueContext collectionValue() {
            return getRuleContext(CollectionValueContext.class, 0);
        }

        public CollectionValueExpressionContext(ExprTermContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).enterCollectionValueExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof HCLParserListener) ((HCLParserListener) listener).exitCollectionValueExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitCollectionValueExpression(this);
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
        ExprTermContext _prevctx = _localctx;
        int _startState = 14;
        enterRecursionRule(_localctx, 14, RULE_exprTerm, _p);
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(140);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 6, _ctx)) {
                    case 1:
                    {
                        _localctx = new TemplateExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;

                        setState(129);
                        templateExpr();
                    }
                        break;
                    case 2:
                    {
                        _localctx = new LiteralExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(130);
                        literalValue();
                    }
                        break;
                    case 3:
                    {
                        _localctx = new ForExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(131);
                        forExpr();
                    }
                        break;
                    case 4:
                    {
                        _localctx = new CollectionValueExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(132);
                        collectionValue();
                    }
                        break;
                    case 5:
                    {
                        _localctx = new VariableExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(133);
                        variableExpr();
                    }
                        break;
                    case 6:
                    {
                        _localctx = new FunctionCallExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(134);
                        functionCall();
                    }
                        break;
                    case 7:
                    {
                        _localctx = new ParentheticalExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(135);
                        match(LPAREN);
                        setState(136);
                        expression(0);
                        setState(137);
                        match(RPAREN);
                    }
                        break;
                    case 8:
                    {
                        _localctx = new BlockExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(139);
                        blockExpr();
                    }
                        break;
                }
                _ctx.stop = _input.LT(-1);
                setState(150);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 8, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        if (_parseListeners != null) triggerExitRuleEvent();
                        _prevctx = _localctx;
                        {
                            setState(148);
                            _errHandler.sync(this);
                            switch (getInterpreter().adaptivePredict(_input, 7, _ctx)) {
                                case 1:
                                {
                                    _localctx = new IndexAccessExpressionContext(new ExprTermContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_exprTerm);
                                    setState(142);
                                    if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
                                    setState(143);
                                    index();
                                }
                                    break;
                                case 2:
                                {
                                    _localctx = new AttributeAccessExpressionContext(new ExprTermContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_exprTerm);
                                    setState(144);
                                    if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
                                    setState(145);
                                    getAttr();
                                }
                                    break;
                                case 3:
                                {
                                    _localctx = new SplatExpressionContext(new ExprTermContext(_parentctx, _parentState));
                                    pushNewRecursionContext(_localctx, _startState, RULE_exprTerm);
                                    setState(146);
                                    if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
                                    setState(147);
                                    splat();
                                }
                                    break;
                            }
                        }
                    }
                    setState(152);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 8, _ctx);
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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitBlockExpr(this);
            else return visitor.visitChildren(this);
        }
    }

    public final BlockExprContext blockExpr() throws RecognitionException {
        BlockExprContext _localctx = new BlockExprContext(_ctx, getState());
        enterRule(_localctx, 16, RULE_blockExpr);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(153);
                match(LBRACE);
                setState(154);
                body();
                setState(155);
                match(RBRACE);
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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitLiteralValue(this);
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
                setState(157);
                _la = _input.LA(1);
                if (!((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << NumericLiteral) | (1L << BooleanLiteral) | (1L << NULL))) != 0))) {
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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitCollectionValue(this);
            else return visitor.visitChildren(this);
        }
    }

    public final CollectionValueContext collectionValue() throws RecognitionException {
        CollectionValueContext _localctx = new CollectionValueContext(_ctx, getState());
        enterRule(_localctx, 20, RULE_collectionValue);
        try {
            setState(161);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case LBRACK:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(159);
                    tuple();
                }
                    break;
                case LBRACE:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(160);
                    object();
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
                setState(163);
                match(LBRACK);
                setState(175);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << FOR_BRACE) | (1L << FOR_BRACK) | (1L << LBRACE) | (1L << Identifier) | (1L << NumericLiteral) | (1L << BooleanLiteral) | (1L << QUOTE) | (1L << NULL) | (1L << HEREDOC_START) | (1L << LBRACK) | (1L << LPAREN) | (1L << MINUS) | (1L << NOT))) != 0)) {
                    {
                        setState(164);
                        expression(0);
                        setState(169);
                        _errHandler.sync(this);
                        _alt = getInterpreter().adaptivePredict(_input, 10, _ctx);
                        while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                            if (_alt == 1) {
                                {
                                    {
                                        setState(165);
                                        match(COMMA);
                                        setState(166);
                                        expression(0);
                                    }
                                }
                            }
                            setState(171);
                            _errHandler.sync(this);
                            _alt = getInterpreter().adaptivePredict(_input, 10, _ctx);
                        }
                        setState(173);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        if (_la == COMMA) {
                            {
                                setState(172);
                                match(COMMA);
                            }
                        }

                    }
                }

                setState(177);
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

        public List<TerminalNode> COMMA() {
            return getTokens(HCLParser.COMMA);
        }

        public TerminalNode COMMA(int i) {
            return getToken(HCLParser.COMMA, i);
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
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(179);
                match(LBRACE);
                setState(191);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == Identifier || _la == LPAREN) {
                    {
                        setState(180);
                        objectelem();
                        setState(185);
                        _errHandler.sync(this);
                        _alt = getInterpreter().adaptivePredict(_input, 13, _ctx);
                        while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                            if (_alt == 1) {
                                {
                                    {
                                        setState(181);
                                        match(COMMA);
                                        setState(182);
                                        objectelem();
                                    }
                                }
                            }
                            setState(187);
                            _errHandler.sync(this);
                            _alt = getInterpreter().adaptivePredict(_input, 13, _ctx);
                        }
                        setState(189);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        if (_la == COMMA) {
                            {
                                setState(188);
                                match(COMMA);
                            }
                        }

                    }
                }

                setState(193);
                match(RBRACE);
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

    public static class ObjectelemContext extends ParserRuleContext {
        public ExpressionContext expression() {
            return getRuleContext(ExpressionContext.class, 0);
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

        public TerminalNode LPAREN() {
            return getToken(HCLParser.LPAREN, 0);
        }

        public TerminalNode RPAREN() {
            return getToken(HCLParser.RPAREN, 0);
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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitObjectelem(this);
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
                setState(199);
                _errHandler.sync(this);
                switch (_input.LA(1)) {
                    case Identifier:
                    {
                        setState(195);
                        match(Identifier);
                    }
                        break;
                    case LPAREN:
                    {
                        setState(196);
                        match(LPAREN);
                        setState(197);
                        match(Identifier);
                        setState(198);
                        match(RPAREN);
                    }
                        break;
                    default:
                        throw new NoViableAltException(this);
                }
                setState(201);
                _la = _input.LA(1);
                if (!(_la == ASSIGN || _la == COLON)) {
                    _errHandler.recoverInline(this);
                }
                else {
                    if (_input.LA(1) == Token.EOF) matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
                setState(202);
                expression(0);
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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitForExpr(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ForExprContext forExpr() throws RecognitionException {
        ForExprContext _localctx = new ForExprContext(_ctx, getState());
        enterRule(_localctx, 28, RULE_forExpr);
        try {
            setState(206);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case FOR_BRACK:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(204);
                    forTupleExpr();
                }
                    break;
                case FOR_BRACE:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(205);
                    forObjectExpr();
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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitForTupleExpr(this);
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
                setState(208);
                match(FOR_BRACK);
                setState(209);
                forIntro();
                setState(210);
                match(COLON);
                setState(211);
                expression(0);
                setState(213);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == IF) {
                    {
                        setState(212);
                        forCond();
                    }
                }

                setState(215);
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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitForObjectExpr(this);
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
                setState(217);
                match(FOR_BRACE);
                setState(218);
                forIntro();
                setState(219);
                match(COLON);
                setState(220);
                expression(0);
                setState(221);
                match(ARROW);
                setState(222);
                expression(0);
                setState(224);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == ELLIPSIS) {
                    {
                        setState(223);
                        match(ELLIPSIS);
                    }
                }

                setState(227);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == IF) {
                    {
                        setState(226);
                        forCond();
                    }
                }

                setState(229);
                match(RBRACE);
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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitForIntro(this);
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
                setState(231);
                match(Identifier);
                setState(234);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == COMMA) {
                    {
                        setState(232);
                        match(COMMA);
                        setState(233);
                        match(Identifier);
                    }
                }

                setState(236);
                match(IN);
                setState(237);
                expression(0);
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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitForCond(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ForCondContext forCond() throws RecognitionException {
        ForCondContext _localctx = new ForCondContext(_ctx, getState());
        enterRule(_localctx, 36, RULE_forCond);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(239);
                match(IF);
                setState(240);
                expression(0);
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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitVariableExpr(this);
            else return visitor.visitChildren(this);
        }
    }

    public final VariableExprContext variableExpr() throws RecognitionException {
        VariableExprContext _localctx = new VariableExprContext(_ctx, getState());
        enterRule(_localctx, 38, RULE_variableExpr);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(242);
                match(Identifier);
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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitFunctionCall(this);
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
                setState(244);
                match(Identifier);
                setState(245);
                match(LPAREN);
                setState(247);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << FOR_BRACE) | (1L << FOR_BRACK) | (1L << LBRACE) | (1L << Identifier) | (1L << NumericLiteral) | (1L << BooleanLiteral) | (1L << QUOTE) | (1L << NULL) | (1L << HEREDOC_START) | (1L << LBRACK) | (1L << LPAREN) | (1L << MINUS) | (1L << NOT))) != 0)) {
                    {
                        setState(246);
                        arguments();
                    }
                }

                setState(249);
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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitArguments(this);
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
                setState(251);
                expression(0);
                setState(256);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 23, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        {
                            {
                                setState(252);
                                match(COMMA);
                                setState(253);
                                expression(0);
                            }
                        }
                    }
                    setState(258);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 23, _ctx);
                }
                setState(260);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == COMMA || _la == ELLIPSIS) {
                    {
                        setState(259);
                        _la = _input.LA(1);
                        if (!(_la == COMMA || _la == ELLIPSIS)) {
                            _errHandler.recoverInline(this);
                        }
                        else {
                            if (_input.LA(1) == Token.EOF) matchedEOF = true;
                            _errHandler.reportMatch(this);
                            consume();
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
                setState(262);
                match(LBRACK);
                setState(263);
                expression(0);
                setState(264);
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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitGetAttr(this);
            else return visitor.visitChildren(this);
        }
    }

    public final GetAttrContext getAttr() throws RecognitionException {
        GetAttrContext _localctx = new GetAttrContext(_ctx, getState());
        enterRule(_localctx, 46, RULE_getAttr);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(266);
                match(DOT);
                setState(267);
                match(Identifier);
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
        enterRule(_localctx, 48, RULE_splat);
        try {
            setState(271);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case DOT:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(269);
                    attrSplat();
                }
                    break;
                case LBRACK:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(270);
                    fullSplat();
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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitAttrSplat(this);
            else return visitor.visitChildren(this);
        }
    }

    public final AttrSplatContext attrSplat() throws RecognitionException {
        AttrSplatContext _localctx = new AttrSplatContext(_ctx, getState());
        enterRule(_localctx, 50, RULE_attrSplat);
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(273);
                match(DOT);
                setState(274);
                match(MUL);
                setState(278);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 26, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        {
                            {
                                setState(275);
                                getAttr();
                            }
                        }
                    }
                    setState(280);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 26, _ctx);
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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitFullSplat(this);
            else return visitor.visitChildren(this);
        }
    }

    public final FullSplatContext fullSplat() throws RecognitionException {
        FullSplatContext _localctx = new FullSplatContext(_ctx, getState());
        enterRule(_localctx, 52, RULE_fullSplat);
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(281);
                match(LBRACK);
                setState(282);
                match(MUL);
                setState(283);
                match(RBRACK);
                setState(288);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 28, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        {
                            setState(286);
                            _errHandler.sync(this);
                            switch (_input.LA(1)) {
                                case DOT:
                                {
                                    setState(284);
                                    getAttr();
                                }
                                    break;
                                case LBRACK:
                                {
                                    setState(285);
                                    index();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                            }
                        }
                    }
                    setState(290);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 28, _ctx);
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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitOperation(this);
            else return visitor.visitChildren(this);
        }
    }

    public final OperationContext operation() throws RecognitionException {
        OperationContext _localctx = new OperationContext(_ctx, getState());
        enterRule(_localctx, 54, RULE_operation);
        try {
            setState(293);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case MINUS:
                case NOT:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(291);
                    unaryOp();
                }
                    break;
                case FOR_BRACE:
                case FOR_BRACK:
                case LBRACE:
                case Identifier:
                case NumericLiteral:
                case BooleanLiteral:
                case QUOTE:
                case NULL:
                case HEREDOC_START:
                case LBRACK:
                case LPAREN:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(292);
                    binaryOp();
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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitUnaryOp(this);
            else return visitor.visitChildren(this);
        }
    }

    public final UnaryOpContext unaryOp() throws RecognitionException {
        UnaryOpContext _localctx = new UnaryOpContext(_ctx, getState());
        enterRule(_localctx, 56, RULE_unaryOp);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(295);
                _la = _input.LA(1);
                if (!(_la == MINUS || _la == NOT)) {
                    _errHandler.recoverInline(this);
                }
                else {
                    if (_input.LA(1) == Token.EOF) matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
                setState(296);
                exprTerm(0);
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

    public static class BinaryOpContext extends ParserRuleContext {
        public List<ExprTermContext> exprTerm() {
            return getRuleContexts(ExprTermContext.class);
        }

        public ExprTermContext exprTerm(int i) {
            return getRuleContext(ExprTermContext.class, i);
        }

        public BinaryOperatorContext binaryOperator() {
            return getRuleContext(BinaryOperatorContext.class, 0);
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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitBinaryOp(this);
            else return visitor.visitChildren(this);
        }
    }

    public final BinaryOpContext binaryOp() throws RecognitionException {
        BinaryOpContext _localctx = new BinaryOpContext(_ctx, getState());
        enterRule(_localctx, 58, RULE_binaryOp);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(298);
                exprTerm(0);
                setState(299);
                binaryOperator();
                setState(300);
                exprTerm(0);
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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitBinaryOperator(this);
            else return visitor.visitChildren(this);
        }
    }

    public final BinaryOperatorContext binaryOperator() throws RecognitionException {
        BinaryOperatorContext _localctx = new BinaryOperatorContext(_ctx, getState());
        enterRule(_localctx, 60, RULE_binaryOperator);
        try {
            setState(305);
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
                    setState(302);
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
                    setState(303);
                    arithmeticOperator();
                }
                    break;
                case AND:
                case OR:
                    enterOuterAlt(_localctx, 3);
                {
                    setState(304);
                    logicOperator();
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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitCompareOperator(this);
            else return visitor.visitChildren(this);
        }
    }

    public final CompareOperatorContext compareOperator() throws RecognitionException {
        CompareOperatorContext _localctx = new CompareOperatorContext(_ctx, getState());
        enterRule(_localctx, 62, RULE_compareOperator);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(307);
                _la = _input.LA(1);
                if (!((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << EQ) | (1L << LT) | (1L << NEQ) | (1L << GT) | (1L << LEQ) | (1L << GEQ))) != 0))) {
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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitArithmeticOperator(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ArithmeticOperatorContext arithmeticOperator() throws RecognitionException {
        ArithmeticOperatorContext _localctx = new ArithmeticOperatorContext(_ctx, getState());
        enterRule(_localctx, 64, RULE_arithmeticOperator);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(309);
                _la = _input.LA(1);
                if (!((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << PLUS) | (1L << MINUS) | (1L << MUL) | (1L << DIV) | (1L << MOD))) != 0))) {
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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitLogicOperator(this);
            else return visitor.visitChildren(this);
        }
    }

    public final LogicOperatorContext logicOperator() throws RecognitionException {
        LogicOperatorContext _localctx = new LogicOperatorContext(_ctx, getState());
        enterRule(_localctx, 66, RULE_logicOperator);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(311);
                _la = _input.LA(1);
                if (!(_la == AND || _la == OR)) {
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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitQuotedTemplate(this);
            else return visitor.visitChildren(this);
        }
    }

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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitHeredoc(this);
            else return visitor.visitChildren(this);
        }
    }

    public final TemplateExprContext templateExpr() throws RecognitionException {
        TemplateExprContext _localctx = new TemplateExprContext(_ctx, getState());
        enterRule(_localctx, 68, RULE_templateExpr);
        int _la;
        try {
            setState(335);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case HEREDOC_START:
                    _localctx = new HeredocContext(_localctx);
                    enterOuterAlt(_localctx, 1);
                {
                    setState(313);
                    match(HEREDOC_START);
                    setState(314);
                    match(Identifier);
                    setState(322);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    do {
                        {
                            {
                                setState(315);
                                match(NEWLINE);
                                setState(319);
                                _errHandler.sync(this);
                                _la = _input.LA(1);
                                while (_la == TEMPLATE_INTERPOLATION_START || _la == HTemplateLiteral) {
                                    {
                                        {
                                            setState(316);
                                            heredocTemplatePart();
                                        }
                                    }
                                    setState(321);
                                    _errHandler.sync(this);
                                    _la = _input.LA(1);
                                }
                            }
                        }
                        setState(324);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                    } while (_la == NEWLINE);
                    setState(326);
                    match(Identifier);
                }
                    break;
                case QUOTE:
                    _localctx = new QuotedTemplateContext(_localctx);
                    enterOuterAlt(_localctx, 2);
                {
                    setState(327);
                    match(QUOTE);
                    setState(331);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    while (_la == TEMPLATE_INTERPOLATION_START || _la == TemplateStringLiteral) {
                        {
                            {
                                setState(328);
                                quotedTemplatePart();
                            }
                        }
                        setState(333);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                    }
                    setState(334);
                    match(QUOTE);
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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitHeredocTemplatePart(this);
            else return visitor.visitChildren(this);
        }
    }

    public final HeredocTemplatePartContext heredocTemplatePart() throws RecognitionException {
        HeredocTemplatePartContext _localctx = new HeredocTemplatePartContext(_ctx, getState());
        enterRule(_localctx, 70, RULE_heredocTemplatePart);
        try {
            setState(339);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case TEMPLATE_INTERPOLATION_START:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(337);
                    templateInterpolation();
                }
                    break;
                case HTemplateLiteral:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(338);
                    heredocLiteral();
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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitHeredocLiteral(this);
            else return visitor.visitChildren(this);
        }
    }

    public final HeredocLiteralContext heredocLiteral() throws RecognitionException {
        HeredocLiteralContext _localctx = new HeredocLiteralContext(_ctx, getState());
        enterRule(_localctx, 72, RULE_heredocLiteral);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(341);
                match(HTemplateLiteral);
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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitQuotedTemplatePart(this);
            else return visitor.visitChildren(this);
        }
    }

    public final QuotedTemplatePartContext quotedTemplatePart() throws RecognitionException {
        QuotedTemplatePartContext _localctx = new QuotedTemplatePartContext(_ctx, getState());
        enterRule(_localctx, 74, RULE_quotedTemplatePart);
        try {
            setState(345);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case TEMPLATE_INTERPOLATION_START:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(343);
                    templateInterpolation();
                }
                    break;
                case TemplateStringLiteral:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(344);
                    stringLiteral();
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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitStringLiteral(this);
            else return visitor.visitChildren(this);
        }
    }

    public final StringLiteralContext stringLiteral() throws RecognitionException {
        StringLiteralContext _localctx = new StringLiteralContext(_ctx, getState());
        enterRule(_localctx, 76, RULE_stringLiteral);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(347);
                match(TemplateStringLiteral);
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
            if (visitor instanceof HCLParserVisitor) return ((HCLParserVisitor<? extends T>) visitor).visitTemplateInterpolation(this);
            else return visitor.visitChildren(this);
        }
    }

    public final TemplateInterpolationContext templateInterpolation() throws RecognitionException {
        TemplateInterpolationContext _localctx = new TemplateInterpolationContext(_ctx, getState());
        enterRule(_localctx, 78, RULE_templateInterpolation);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(349);
                match(TEMPLATE_INTERPOLATION_START);
                setState(350);
                expression(0);
                setState(351);
                match(RBRACE);
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
        }
        return true;
    }

    public static final String _serializedATN =
            "\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\61\u0164\4\2\t\2" +
                    "\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13" +
                    "\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22" +
                    "\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31" +
                    "\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!" +
                    "\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\3\2\3\2\3\3\7" +
                    "\3V\n\3\f\3\16\3Y\13\3\3\4\3\4\5\4]\n\4\3\5\3\5\3\5\3\5\3\6\3\6\7\6e\n" +
                    "\6\f\6\16\6h\13\6\3\6\3\6\3\7\3\7\3\7\3\7\3\7\5\7q\n\7\3\b\3\b\3\b\5\b" +
                    "v\n\b\3\b\3\b\3\b\3\b\3\b\3\b\7\b~\n\b\f\b\16\b\u0081\13\b\3\t\3\t\3\t" +
                    "\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\5\t\u008f\n\t\3\t\3\t\3\t\3\t\3\t" +
                    "\3\t\7\t\u0097\n\t\f\t\16\t\u009a\13\t\3\n\3\n\3\n\3\n\3\13\3\13\3\f\3" +
                    "\f\5\f\u00a4\n\f\3\r\3\r\3\r\3\r\7\r\u00aa\n\r\f\r\16\r\u00ad\13\r\3\r" +
                    "\5\r\u00b0\n\r\5\r\u00b2\n\r\3\r\3\r\3\16\3\16\3\16\3\16\7\16\u00ba\n" +
                    "\16\f\16\16\16\u00bd\13\16\3\16\5\16\u00c0\n\16\5\16\u00c2\n\16\3\16\3" +
                    "\16\3\17\3\17\3\17\3\17\5\17\u00ca\n\17\3\17\3\17\3\17\3\20\3\20\5\20" +
                    "\u00d1\n\20\3\21\3\21\3\21\3\21\3\21\5\21\u00d8\n\21\3\21\3\21\3\22\3" +
                    "\22\3\22\3\22\3\22\3\22\3\22\5\22\u00e3\n\22\3\22\5\22\u00e6\n\22\3\22" +
                    "\3\22\3\23\3\23\3\23\5\23\u00ed\n\23\3\23\3\23\3\23\3\24\3\24\3\24\3\25" +
                    "\3\25\3\26\3\26\3\26\5\26\u00fa\n\26\3\26\3\26\3\27\3\27\3\27\7\27\u0101" +
                    "\n\27\f\27\16\27\u0104\13\27\3\27\5\27\u0107\n\27\3\30\3\30\3\30\3\30" +
                    "\3\31\3\31\3\31\3\32\3\32\5\32\u0112\n\32\3\33\3\33\3\33\7\33\u0117\n" +
                    "\33\f\33\16\33\u011a\13\33\3\34\3\34\3\34\3\34\3\34\7\34\u0121\n\34\f" +
                    "\34\16\34\u0124\13\34\3\35\3\35\5\35\u0128\n\35\3\36\3\36\3\36\3\37\3" +
                    "\37\3\37\3\37\3 \3 \3 \5 \u0134\n \3!\3!\3\"\3\"\3#\3#\3$\3$\3$\3$\7$" +
                    "\u0140\n$\f$\16$\u0143\13$\6$\u0145\n$\r$\16$\u0146\3$\3$\3$\7$\u014c" +
                    "\n$\f$\16$\u014f\13$\3$\5$\u0152\n$\3%\3%\5%\u0156\n%\3&\3&\3\'\3\'\5" +
                    "\'\u015c\n\'\3(\3(\3)\3)\3)\3)\3)\2\4\16\20*\2\4\6\b\n\f\16\20\22\24\26" +
                    "\30\32\34\36 \"$&(*,.\60\62\64\668:<>@BDFHJLNP\2\t\4\2\17\20\22\22\4\2" +
                    "\t\t\30\30\4\2))++\4\2\33\33##\6\2\26\27\35\36$$\'\'\7\2\24\24\33\33\"" +
                    "\"&&**\4\2\25\25\34\34\2\u0168\2R\3\2\2\2\4W\3\2\2\2\6\\\3\2\2\2\b^\3" +
                    "\2\2\2\nb\3\2\2\2\fp\3\2\2\2\16u\3\2\2\2\20\u008e\3\2\2\2\22\u009b\3\2" +
                    "\2\2\24\u009f\3\2\2\2\26\u00a3\3\2\2\2\30\u00a5\3\2\2\2\32\u00b5\3\2\2" +
                    "\2\34\u00c9\3\2\2\2\36\u00d0\3\2\2\2 \u00d2\3\2\2\2\"\u00db\3\2\2\2$\u00e9" +
                    "\3\2\2\2&\u00f1\3\2\2\2(\u00f4\3\2\2\2*\u00f6\3\2\2\2,\u00fd\3\2\2\2." +
                    "\u0108\3\2\2\2\60\u010c\3\2\2\2\62\u0111\3\2\2\2\64\u0113\3\2\2\2\66\u011b" +
                    "\3\2\2\28\u0127\3\2\2\2:\u0129\3\2\2\2<\u012c\3\2\2\2>\u0133\3\2\2\2@" +
                    "\u0135\3\2\2\2B\u0137\3\2\2\2D\u0139\3\2\2\2F\u0151\3\2\2\2H\u0155\3\2" +
                    "\2\2J\u0157\3\2\2\2L\u015b\3\2\2\2N\u015d\3\2\2\2P\u015f\3\2\2\2RS\5\4" +
                    "\3\2S\3\3\2\2\2TV\5\6\4\2UT\3\2\2\2VY\3\2\2\2WU\3\2\2\2WX\3\2\2\2X\5\3" +
                    "\2\2\2YW\3\2\2\2Z]\5\b\5\2[]\5\n\6\2\\Z\3\2\2\2\\[\3\2\2\2]\7\3\2\2\2" +
                    "^_\7\n\2\2_`\7\t\2\2`a\5\16\b\2a\t\3\2\2\2bf\7\n\2\2ce\5\f\7\2dc\3\2\2" +
                    "\2eh\3\2\2\2fd\3\2\2\2fg\3\2\2\2gi\3\2\2\2hf\3\2\2\2ij\5\22\n\2j\13\3" +
                    "\2\2\2kl\7\21\2\2lm\5N(\2mn\7\21\2\2nq\3\2\2\2oq\7\n\2\2pk\3\2\2\2po\3" +
                    "\2\2\2q\r\3\2\2\2rs\b\b\1\2sv\5\20\t\2tv\58\35\2ur\3\2\2\2ut\3\2\2\2v" +
                    "\177\3\2\2\2wx\f\3\2\2xy\7\37\2\2yz\5\16\b\2z{\7\30\2\2{|\5\16\b\4|~\3" +
                    "\2\2\2}w\3\2\2\2~\u0081\3\2\2\2\177}\3\2\2\2\177\u0080\3\2\2\2\u0080\17" +
                    "\3\2\2\2\u0081\177\3\2\2\2\u0082\u0083\b\t\1\2\u0083\u008f\5F$\2\u0084" +
                    "\u008f\5\24\13\2\u0085\u008f\5\36\20\2\u0086\u008f\5\26\f\2\u0087\u008f" +
                    "\5(\25\2\u0088\u008f\5*\26\2\u0089\u008a\7\32\2\2\u008a\u008b\5\16\b\2" +
                    "\u008b\u008c\7!\2\2\u008c\u008f\3\2\2\2\u008d\u008f\5\22\n\2\u008e\u0082" +
                    "\3\2\2\2\u008e\u0084\3\2\2\2\u008e\u0085\3\2\2\2\u008e\u0086\3\2\2\2\u008e" +
                    "\u0087\3\2\2\2\u008e\u0088\3\2\2\2\u008e\u0089\3\2\2\2\u008e\u008d\3\2" +
                    "\2\2\u008f\u0098\3\2\2\2\u0090\u0091\f\7\2\2\u0091\u0097\5.\30\2\u0092" +
                    "\u0093\f\6\2\2\u0093\u0097\5\60\31\2\u0094\u0095\f\5\2\2\u0095\u0097\5" +
                    "\62\32\2\u0096\u0090\3\2\2\2\u0096\u0092\3\2\2\2\u0096\u0094\3\2\2\2\u0097" +
                    "\u009a\3\2\2\2\u0098\u0096\3\2\2\2\u0098\u0099\3\2\2\2\u0099\21\3\2\2" +
                    "\2\u009a\u0098\3\2\2\2\u009b\u009c\7\7\2\2\u009c\u009d\5\4\3\2\u009d\u009e" +
                    "\7\b\2\2\u009e\23\3\2\2\2\u009f\u00a0\t\2\2\2\u00a0\25\3\2\2\2\u00a1\u00a4" +
                    "\5\30\r\2\u00a2\u00a4\5\32\16\2\u00a3\u00a1\3\2\2\2\u00a3\u00a2\3\2\2" +
                    "\2\u00a4\27\3\2\2\2\u00a5\u00b1\7\31\2\2\u00a6\u00ab\5\16\b\2\u00a7\u00a8" +
                    "\7)\2\2\u00a8\u00aa\5\16\b\2\u00a9\u00a7\3\2\2\2\u00aa\u00ad\3\2\2\2\u00ab" +
                    "\u00a9\3\2\2\2\u00ab\u00ac\3\2\2\2\u00ac\u00af\3\2\2\2\u00ad\u00ab\3\2" +
                    "\2\2\u00ae\u00b0\7)\2\2\u00af\u00ae\3\2\2\2\u00af\u00b0\3\2\2\2\u00b0" +
                    "\u00b2\3\2\2\2\u00b1\u00a6\3\2\2\2\u00b1\u00b2\3\2\2\2\u00b2\u00b3\3\2" +
                    "\2\2\u00b3\u00b4\7 \2\2\u00b4\31\3\2\2\2\u00b5\u00c1\7\7\2\2\u00b6\u00bb" +
                    "\5\34\17\2\u00b7\u00b8\7)\2\2\u00b8\u00ba\5\34\17\2\u00b9\u00b7\3\2\2" +
                    "\2\u00ba\u00bd\3\2\2\2\u00bb\u00b9\3\2\2\2\u00bb\u00bc\3\2\2\2\u00bc\u00bf" +
                    "\3\2\2\2\u00bd\u00bb\3\2\2\2\u00be\u00c0\7)\2\2\u00bf\u00be\3\2\2\2\u00bf" +
                    "\u00c0\3\2\2\2\u00c0\u00c2\3\2\2\2\u00c1\u00b6\3\2\2\2\u00c1\u00c2\3\2" +
                    "\2\2\u00c2\u00c3\3\2\2\2\u00c3\u00c4\7\b\2\2\u00c4\33\3\2\2\2\u00c5\u00ca" +
                    "\7\n\2\2\u00c6\u00c7\7\32\2\2\u00c7\u00c8\7\n\2\2\u00c8\u00ca\7!\2\2\u00c9" +
                    "\u00c5\3\2\2\2\u00c9\u00c6\3\2\2\2\u00ca\u00cb\3\2\2\2\u00cb\u00cc\t\3" +
                    "\2\2\u00cc\u00cd\5\16\b\2\u00cd\35\3\2\2\2\u00ce\u00d1\5 \21\2\u00cf\u00d1" +
                    "\5\"\22\2\u00d0\u00ce\3\2\2\2\u00d0\u00cf\3\2\2\2\u00d1\37\3\2\2\2\u00d2" +
                    "\u00d3\7\4\2\2\u00d3\u00d4\5$\23\2\u00d4\u00d5\7\30\2\2\u00d5\u00d7\5" +
                    "\16\b\2\u00d6\u00d8\5&\24\2\u00d7\u00d6\3\2\2\2\u00d7\u00d8\3\2\2\2\u00d8" +
                    "\u00d9\3\2\2\2\u00d9\u00da\7 \2\2\u00da!\3\2\2\2\u00db\u00dc\7\3\2\2\u00dc" +
                    "\u00dd\5$\23\2\u00dd\u00de\7\30\2\2\u00de\u00df\5\16\b\2\u00df\u00e0\7" +
                    "(\2\2\u00e0\u00e2\5\16\b\2\u00e1\u00e3\7+\2\2\u00e2\u00e1\3\2\2\2\u00e2" +
                    "\u00e3\3\2\2\2\u00e3\u00e5\3\2\2\2\u00e4\u00e6\5&\24\2\u00e5\u00e4\3\2" +
                    "\2\2\u00e5\u00e6\3\2\2\2\u00e6\u00e7\3\2\2\2\u00e7\u00e8\7\b\2\2\u00e8" +
                    "#\3\2\2\2\u00e9\u00ec\7\n\2\2\u00ea\u00eb\7)\2\2\u00eb\u00ed\7\n\2\2\u00ec" +
                    "\u00ea\3\2\2\2\u00ec\u00ed\3\2\2\2\u00ed\u00ee\3\2\2\2\u00ee\u00ef\7\6" +
                    "\2\2\u00ef\u00f0\5\16\b\2\u00f0%\3\2\2\2\u00f1\u00f2\7\5\2\2\u00f2\u00f3" +
                    "\5\16\b\2\u00f3\'\3\2\2\2\u00f4\u00f5\7\n\2\2\u00f5)\3\2\2\2\u00f6\u00f7" +
                    "\7\n\2\2\u00f7\u00f9\7\32\2\2\u00f8\u00fa\5,\27\2\u00f9\u00f8\3\2\2\2" +
                    "\u00f9\u00fa\3\2\2\2\u00fa\u00fb\3\2\2\2\u00fb\u00fc\7!\2\2\u00fc+\3\2" +
                    "\2\2\u00fd\u0102\5\16\b\2\u00fe\u00ff\7)\2\2\u00ff\u0101\5\16\b\2\u0100" +
                    "\u00fe\3\2\2\2\u0101\u0104\3\2\2\2\u0102\u0100\3\2\2\2\u0102\u0103\3\2" +
                    "\2\2\u0103\u0106\3\2\2\2\u0104\u0102\3\2\2\2\u0105\u0107\t\4\2\2\u0106" +
                    "\u0105\3\2\2\2\u0106\u0107\3\2\2\2\u0107-\3\2\2\2\u0108\u0109\7\31\2\2" +
                    "\u0109\u010a\5\16\b\2\u010a\u010b\7 \2\2\u010b/\3\2\2\2\u010c\u010d\7" +
                    "%\2\2\u010d\u010e\7\n\2\2\u010e\61\3\2\2\2\u010f\u0112\5\64\33\2\u0110" +
                    "\u0112\5\66\34\2\u0111\u010f\3\2\2\2\u0111\u0110\3\2\2\2\u0112\63\3\2" +
                    "\2\2\u0113\u0114\7%\2\2\u0114\u0118\7\"\2\2\u0115\u0117\5\60\31\2\u0116" +
                    "\u0115\3\2\2\2\u0117\u011a\3\2\2\2\u0118\u0116\3\2\2\2\u0118\u0119\3\2" +
                    "\2\2\u0119\65\3\2\2\2\u011a\u0118\3\2\2\2\u011b\u011c\7\31\2\2\u011c\u011d" +
                    "\7\"\2\2\u011d\u0122\7 \2\2\u011e\u0121\5\60\31\2\u011f\u0121\5.\30\2" +
                    "\u0120\u011e\3\2\2\2\u0120\u011f\3\2\2\2\u0121\u0124\3\2\2\2\u0122\u0120" +
                    "\3\2\2\2\u0122\u0123\3\2\2\2\u0123\67\3\2\2\2\u0124\u0122\3\2\2\2\u0125" +
                    "\u0128\5:\36\2\u0126\u0128\5<\37\2\u0127\u0125\3\2\2\2\u0127\u0126\3\2" +
                    "\2\2\u01289\3\2\2\2\u0129\u012a\t\5\2\2\u012a\u012b\5\20\t\2\u012b;\3" +
                    "\2\2\2\u012c\u012d\5\20\t\2\u012d\u012e\5> \2\u012e\u012f\5\20\t\2\u012f" +
                    "=\3\2\2\2\u0130\u0134\5@!\2\u0131\u0134\5B\"\2\u0132\u0134\5D#\2\u0133" +
                    "\u0130\3\2\2\2\u0133\u0131\3\2\2\2\u0133\u0132\3\2\2\2\u0134?\3\2\2\2" +
                    "\u0135\u0136\t\6\2\2\u0136A\3\2\2\2\u0137\u0138\t\7\2\2\u0138C\3\2\2\2" +
                    "\u0139\u013a\t\b\2\2\u013aE\3\2\2\2\u013b\u013c\7\23\2\2\u013c\u0144\7" +
                    "\n\2\2\u013d\u0141\7\16\2\2\u013e\u0140\5H%\2\u013f\u013e\3\2\2\2\u0140" +
                    "\u0143\3\2\2\2\u0141\u013f\3\2\2\2\u0141\u0142\3\2\2\2\u0142\u0145\3\2" +
                    "\2\2\u0143\u0141\3\2\2\2\u0144\u013d\3\2\2\2\u0145\u0146\3\2\2\2\u0146" +
                    "\u0144\3\2\2\2\u0146\u0147\3\2\2\2\u0147\u0148\3\2\2\2\u0148\u0152\7\n" +
                    "\2\2\u0149\u014d\7\21\2\2\u014a\u014c\5L\'\2\u014b\u014a\3\2\2\2\u014c" +
                    "\u014f\3\2\2\2\u014d\u014b\3\2\2\2\u014d\u014e\3\2\2\2\u014e\u0150\3\2" +
                    "\2\2\u014f\u014d\3\2\2\2\u0150\u0152\7\21\2\2\u0151\u013b\3\2\2\2\u0151" +
                    "\u0149\3\2\2\2\u0152G\3\2\2\2\u0153\u0156\5P)\2\u0154\u0156\5J&\2\u0155" +
                    "\u0153\3\2\2\2\u0155\u0154\3\2\2\2\u0156I\3\2\2\2\u0157\u0158\7\60\2\2" +
                    "\u0158K\3\2\2\2\u0159\u015c\5P)\2\u015a\u015c\5N(\2\u015b\u0159\3\2\2" +
                    "\2\u015b\u015a\3\2\2\2\u015cM\3\2\2\2\u015d\u015e\7.\2\2\u015eO\3\2\2" +
                    "\2\u015f\u0160\7-\2\2\u0160\u0161\5\16\b\2\u0161\u0162\7\b\2\2\u0162Q" +
                    "\3\2\2\2\'W\\fpu\177\u008e\u0096\u0098\u00a3\u00ab\u00af\u00b1\u00bb\u00bf" +
                    "\u00c1\u00c9\u00d0\u00d7\u00e2\u00e5\u00ec\u00f9\u0102\u0106\u0111\u0118" +
                    "\u0120\u0122\u0127\u0133\u0141\u0146\u014d\u0151\u0155\u015b";
    public static final ATN _ATN =
            new ATNDeserializer().deserialize(_serializedATN.toCharArray());

    static {
        _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
        for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
            _decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
        }
    }
}