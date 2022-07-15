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
// Generated from /Users/jon/Projects/github/openrewrite/rewrite/rewrite-java/src/main/antlr/AnnotationSignatureParser.g4 by ANTLR 4.9.3
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
public class AnnotationSignatureParser extends Parser {
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
    public static final int
            RULE_annotation = 0, RULE_annotationName = 1, RULE_qualifiedName = 2,
            RULE_elementValuePairs = 3, RULE_elementValuePair = 4, RULE_elementValue = 5,
            RULE_primary = 6, RULE_type = 7, RULE_classOrInterfaceType = 8, RULE_literal = 9;

    private static String[] makeRuleNames() {
        return new String[]{
                "annotation", "annotationName", "qualifiedName", "elementValuePairs",
                "elementValuePair", "elementValue", "primary", "type", "classOrInterfaceType",
                "literal"
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

    @Override
    public String getGrammarFileName() {
        return "AnnotationSignatureParser.g4";
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

    public AnnotationSignatureParser(TokenStream input) {
        super(input);
        _interp = new ParserATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
    }

    public static class AnnotationContext extends ParserRuleContext {
        public TerminalNode AT() {
            return getToken(AnnotationSignatureParser.AT, 0);
        }

        public AnnotationNameContext annotationName() {
            return getRuleContext(AnnotationNameContext.class, 0);
        }

        public TerminalNode LPAREN() {
            return getToken(AnnotationSignatureParser.LPAREN, 0);
        }

        public TerminalNode RPAREN() {
            return getToken(AnnotationSignatureParser.RPAREN, 0);
        }

        public ElementValuePairsContext elementValuePairs() {
            return getRuleContext(ElementValuePairsContext.class, 0);
        }

        public ElementValueContext elementValue() {
            return getRuleContext(ElementValueContext.class, 0);
        }

        public AnnotationContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_annotation;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof AnnotationSignatureParserListener) ((AnnotationSignatureParserListener) listener).enterAnnotation(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof AnnotationSignatureParserListener) ((AnnotationSignatureParserListener) listener).exitAnnotation(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof AnnotationSignatureParserVisitor) return ((AnnotationSignatureParserVisitor<? extends T>) visitor).visitAnnotation(this);
            else return visitor.visitChildren(this);
        }
    }

    public final AnnotationContext annotation() throws RecognitionException {
        AnnotationContext _localctx = new AnnotationContext(_ctx, getState());
        enterRule(_localctx, 0, RULE_annotation);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(20);
                match(AT);
                setState(21);
                annotationName();
                setState(28);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == LPAREN) {
                    {
                        setState(22);
                        match(LPAREN);
                        setState(25);
                        _errHandler.sync(this);
                        switch (getInterpreter().adaptivePredict(_input, 0, _ctx)) {
                            case 1:
                            {
                                setState(23);
                                elementValuePairs();
                            }
                                break;
                            case 2:
                            {
                                setState(24);
                                elementValue();
                            }
                                break;
                        }
                        setState(27);
                        match(RPAREN);
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

    public static class AnnotationNameContext extends ParserRuleContext {
        public QualifiedNameContext qualifiedName() {
            return getRuleContext(QualifiedNameContext.class, 0);
        }

        public AnnotationNameContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_annotationName;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof AnnotationSignatureParserListener) ((AnnotationSignatureParserListener) listener).enterAnnotationName(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof AnnotationSignatureParserListener) ((AnnotationSignatureParserListener) listener).exitAnnotationName(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof AnnotationSignatureParserVisitor) return ((AnnotationSignatureParserVisitor<? extends T>) visitor).visitAnnotationName(this);
            else return visitor.visitChildren(this);
        }
    }

    public final AnnotationNameContext annotationName() throws RecognitionException {
        AnnotationNameContext _localctx = new AnnotationNameContext(_ctx, getState());
        enterRule(_localctx, 2, RULE_annotationName);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(30);
                qualifiedName();
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

    public static class QualifiedNameContext extends ParserRuleContext {
        public List<TerminalNode> Identifier() {
            return getTokens(AnnotationSignatureParser.Identifier);
        }

        public TerminalNode Identifier(int i) {
            return getToken(AnnotationSignatureParser.Identifier, i);
        }

        public List<TerminalNode> DOT() {
            return getTokens(AnnotationSignatureParser.DOT);
        }

        public TerminalNode DOT(int i) {
            return getToken(AnnotationSignatureParser.DOT, i);
        }

        public List<TerminalNode> DOTDOT() {
            return getTokens(AnnotationSignatureParser.DOTDOT);
        }

        public TerminalNode DOTDOT(int i) {
            return getToken(AnnotationSignatureParser.DOTDOT, i);
        }

        public QualifiedNameContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_qualifiedName;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof AnnotationSignatureParserListener) ((AnnotationSignatureParserListener) listener).enterQualifiedName(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof AnnotationSignatureParserListener) ((AnnotationSignatureParserListener) listener).exitQualifiedName(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof AnnotationSignatureParserVisitor) return ((AnnotationSignatureParserVisitor<? extends T>) visitor).visitQualifiedName(this);
            else return visitor.visitChildren(this);
        }
    }

    public final QualifiedNameContext qualifiedName() throws RecognitionException {
        QualifiedNameContext _localctx = new QualifiedNameContext(_ctx, getState());
        enterRule(_localctx, 4, RULE_qualifiedName);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(32);
                match(Identifier);
                setState(37);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == DOT || _la == DOTDOT) {
                    {
                        {
                            setState(33);
                            _la = _input.LA(1);
                            if (!(_la == DOT || _la == DOTDOT)) {
                                _errHandler.recoverInline(this);
                            }
                            else {
                                if (_input.LA(1) == Token.EOF) matchedEOF = true;
                                _errHandler.reportMatch(this);
                                consume();
                            }
                            setState(34);
                            match(Identifier);
                        }
                    }
                    setState(39);
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

    public static class ElementValuePairsContext extends ParserRuleContext {
        public List<ElementValuePairContext> elementValuePair() {
            return getRuleContexts(ElementValuePairContext.class);
        }

        public ElementValuePairContext elementValuePair(int i) {
            return getRuleContext(ElementValuePairContext.class, i);
        }

        public List<TerminalNode> COMMA() {
            return getTokens(AnnotationSignatureParser.COMMA);
        }

        public TerminalNode COMMA(int i) {
            return getToken(AnnotationSignatureParser.COMMA, i);
        }

        public ElementValuePairsContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_elementValuePairs;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof AnnotationSignatureParserListener) ((AnnotationSignatureParserListener) listener).enterElementValuePairs(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof AnnotationSignatureParserListener) ((AnnotationSignatureParserListener) listener).exitElementValuePairs(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof AnnotationSignatureParserVisitor) return ((AnnotationSignatureParserVisitor<? extends T>) visitor).visitElementValuePairs(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ElementValuePairsContext elementValuePairs() throws RecognitionException {
        ElementValuePairsContext _localctx = new ElementValuePairsContext(_ctx, getState());
        enterRule(_localctx, 6, RULE_elementValuePairs);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(40);
                elementValuePair();
                setState(45);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == COMMA) {
                    {
                        {
                            setState(41);
                            match(COMMA);
                            setState(42);
                            elementValuePair();
                        }
                    }
                    setState(47);
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

    public static class ElementValuePairContext extends ParserRuleContext {
        public TerminalNode Identifier() {
            return getToken(AnnotationSignatureParser.Identifier, 0);
        }

        public TerminalNode ASSIGN() {
            return getToken(AnnotationSignatureParser.ASSIGN, 0);
        }

        public ElementValueContext elementValue() {
            return getRuleContext(ElementValueContext.class, 0);
        }

        public ElementValuePairContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_elementValuePair;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof AnnotationSignatureParserListener) ((AnnotationSignatureParserListener) listener).enterElementValuePair(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof AnnotationSignatureParserListener) ((AnnotationSignatureParserListener) listener).exitElementValuePair(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof AnnotationSignatureParserVisitor) return ((AnnotationSignatureParserVisitor<? extends T>) visitor).visitElementValuePair(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ElementValuePairContext elementValuePair() throws RecognitionException {
        ElementValuePairContext _localctx = new ElementValuePairContext(_ctx, getState());
        enterRule(_localctx, 8, RULE_elementValuePair);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(48);
                match(Identifier);
                setState(49);
                match(ASSIGN);
                setState(50);
                elementValue();
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

    public static class ElementValueContext extends ParserRuleContext {
        public PrimaryContext primary() {
            return getRuleContext(PrimaryContext.class, 0);
        }

        public ElementValueContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_elementValue;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof AnnotationSignatureParserListener) ((AnnotationSignatureParserListener) listener).enterElementValue(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof AnnotationSignatureParserListener) ((AnnotationSignatureParserListener) listener).exitElementValue(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof AnnotationSignatureParserVisitor) return ((AnnotationSignatureParserVisitor<? extends T>) visitor).visitElementValue(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ElementValueContext elementValue() throws RecognitionException {
        ElementValueContext _localctx = new ElementValueContext(_ctx, getState());
        enterRule(_localctx, 10, RULE_elementValue);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(52);
                primary();
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

    public static class PrimaryContext extends ParserRuleContext {
        public LiteralContext literal() {
            return getRuleContext(LiteralContext.class, 0);
        }

        public TypeContext type() {
            return getRuleContext(TypeContext.class, 0);
        }

        public PrimaryContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_primary;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof AnnotationSignatureParserListener) ((AnnotationSignatureParserListener) listener).enterPrimary(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof AnnotationSignatureParserListener) ((AnnotationSignatureParserListener) listener).exitPrimary(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof AnnotationSignatureParserVisitor) return ((AnnotationSignatureParserVisitor<? extends T>) visitor).visitPrimary(this);
            else return visitor.visitChildren(this);
        }
    }

    public final PrimaryContext primary() throws RecognitionException {
        PrimaryContext _localctx = new PrimaryContext(_ctx, getState());
        enterRule(_localctx, 12, RULE_primary);
        try {
            setState(56);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case IntegerLiteral:
                case FloatingPointLiteral:
                case BooleanLiteral:
                case CharacterLiteral:
                case StringLiteral:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(54);
                    literal();
                }
                    break;
                case Identifier:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(55);
                    type();
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

    public static class TypeContext extends ParserRuleContext {
        public ClassOrInterfaceTypeContext classOrInterfaceType() {
            return getRuleContext(ClassOrInterfaceTypeContext.class, 0);
        }

        public List<TerminalNode> LBRACK() {
            return getTokens(AnnotationSignatureParser.LBRACK);
        }

        public TerminalNode LBRACK(int i) {
            return getToken(AnnotationSignatureParser.LBRACK, i);
        }

        public List<TerminalNode> RBRACK() {
            return getTokens(AnnotationSignatureParser.RBRACK);
        }

        public TerminalNode RBRACK(int i) {
            return getToken(AnnotationSignatureParser.RBRACK, i);
        }

        public TypeContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_type;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof AnnotationSignatureParserListener) ((AnnotationSignatureParserListener) listener).enterType(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof AnnotationSignatureParserListener) ((AnnotationSignatureParserListener) listener).exitType(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof AnnotationSignatureParserVisitor) return ((AnnotationSignatureParserVisitor<? extends T>) visitor).visitType(this);
            else return visitor.visitChildren(this);
        }
    }

    public final TypeContext type() throws RecognitionException {
        TypeContext _localctx = new TypeContext(_ctx, getState());
        enterRule(_localctx, 14, RULE_type);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(58);
                classOrInterfaceType();
                setState(63);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == LBRACK) {
                    {
                        {
                            setState(59);
                            match(LBRACK);
                            setState(60);
                            match(RBRACK);
                        }
                    }
                    setState(65);
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

    public static class ClassOrInterfaceTypeContext extends ParserRuleContext {
        public List<TerminalNode> Identifier() {
            return getTokens(AnnotationSignatureParser.Identifier);
        }

        public TerminalNode Identifier(int i) {
            return getToken(AnnotationSignatureParser.Identifier, i);
        }

        public List<TerminalNode> DOT() {
            return getTokens(AnnotationSignatureParser.DOT);
        }

        public TerminalNode DOT(int i) {
            return getToken(AnnotationSignatureParser.DOT, i);
        }

        public List<TerminalNode> DOTDOT() {
            return getTokens(AnnotationSignatureParser.DOTDOT);
        }

        public TerminalNode DOTDOT(int i) {
            return getToken(AnnotationSignatureParser.DOTDOT, i);
        }

        public ClassOrInterfaceTypeContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_classOrInterfaceType;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof AnnotationSignatureParserListener) ((AnnotationSignatureParserListener) listener).enterClassOrInterfaceType(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof AnnotationSignatureParserListener) ((AnnotationSignatureParserListener) listener).exitClassOrInterfaceType(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof AnnotationSignatureParserVisitor) return ((AnnotationSignatureParserVisitor<? extends T>) visitor).visitClassOrInterfaceType(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ClassOrInterfaceTypeContext classOrInterfaceType() throws RecognitionException {
        ClassOrInterfaceTypeContext _localctx = new ClassOrInterfaceTypeContext(_ctx, getState());
        enterRule(_localctx, 16, RULE_classOrInterfaceType);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(66);
                match(Identifier);
                setState(71);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == DOT || _la == DOTDOT) {
                    {
                        {
                            setState(67);
                            _la = _input.LA(1);
                            if (!(_la == DOT || _la == DOTDOT)) {
                                _errHandler.recoverInline(this);
                            }
                            else {
                                if (_input.LA(1) == Token.EOF) matchedEOF = true;
                                _errHandler.reportMatch(this);
                                consume();
                            }
                            setState(68);
                            match(Identifier);
                        }
                    }
                    setState(73);
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

    public static class LiteralContext extends ParserRuleContext {
        public TerminalNode IntegerLiteral() {
            return getToken(AnnotationSignatureParser.IntegerLiteral, 0);
        }

        public TerminalNode FloatingPointLiteral() {
            return getToken(AnnotationSignatureParser.FloatingPointLiteral, 0);
        }

        public TerminalNode CharacterLiteral() {
            return getToken(AnnotationSignatureParser.CharacterLiteral, 0);
        }

        public TerminalNode StringLiteral() {
            return getToken(AnnotationSignatureParser.StringLiteral, 0);
        }

        public TerminalNode BooleanLiteral() {
            return getToken(AnnotationSignatureParser.BooleanLiteral, 0);
        }

        public LiteralContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_literal;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof AnnotationSignatureParserListener) ((AnnotationSignatureParserListener) listener).enterLiteral(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof AnnotationSignatureParserListener) ((AnnotationSignatureParserListener) listener).exitLiteral(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof AnnotationSignatureParserVisitor) return ((AnnotationSignatureParserVisitor<? extends T>) visitor).visitLiteral(this);
            else return visitor.visitChildren(this);
        }
    }

    public final LiteralContext literal() throws RecognitionException {
        LiteralContext _localctx = new LiteralContext(_ctx, getState());
        enterRule(_localctx, 18, RULE_literal);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(74);
                _la = _input.LA(1);
                if (!((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << IntegerLiteral) | (1L << FloatingPointLiteral) | (1L << BooleanLiteral) | (1L << CharacterLiteral) | (1L << StringLiteral))) != 0))) {
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

    public static final String _serializedATN =
            "\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\30O\4\2\t\2\4\3\t" +
                    "\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t\13\3" +
                    "\2\3\2\3\2\3\2\3\2\5\2\34\n\2\3\2\5\2\37\n\2\3\3\3\3\3\4\3\4\3\4\7\4&" +
                    "\n\4\f\4\16\4)\13\4\3\5\3\5\3\5\7\5.\n\5\f\5\16\5\61\13\5\3\6\3\6\3\6" +
                    "\3\6\3\7\3\7\3\b\3\b\5\b;\n\b\3\t\3\t\3\t\7\t@\n\t\f\t\16\tC\13\t\3\n" +
                    "\3\n\3\n\7\nH\n\n\f\n\16\nK\13\n\3\13\3\13\3\13\2\2\f\2\4\6\b\n\f\16\20" +
                    "\22\24\2\4\4\2\r\r\26\26\3\2\3\7\2L\2\26\3\2\2\2\4 \3\2\2\2\6\"\3\2\2" +
                    "\2\b*\3\2\2\2\n\62\3\2\2\2\f\66\3\2\2\2\16:\3\2\2\2\20<\3\2\2\2\22D\3" +
                    "\2\2\2\24L\3\2\2\2\26\27\7\24\2\2\27\36\5\4\3\2\30\33\7\b\2\2\31\34\5" +
                    "\b\5\2\32\34\5\f\7\2\33\31\3\2\2\2\33\32\3\2\2\2\33\34\3\2\2\2\34\35\3" +
                    "\2\2\2\35\37\7\t\2\2\36\30\3\2\2\2\36\37\3\2\2\2\37\3\3\2\2\2 !\5\6\4" +
                    "\2!\5\3\2\2\2\"\'\7\30\2\2#$\t\2\2\2$&\7\30\2\2%#\3\2\2\2&)\3\2\2\2\'" +
                    "%\3\2\2\2\'(\3\2\2\2(\7\3\2\2\2)\'\3\2\2\2*/\5\n\6\2+,\7\f\2\2,.\5\n\6" +
                    "\2-+\3\2\2\2.\61\3\2\2\2/-\3\2\2\2/\60\3\2\2\2\60\t\3\2\2\2\61/\3\2\2" +
                    "\2\62\63\7\30\2\2\63\64\7\16\2\2\64\65\5\f\7\2\65\13\3\2\2\2\66\67\5\16" +
                    "\b\2\67\r\3\2\2\28;\5\24\13\29;\5\20\t\2:8\3\2\2\2:9\3\2\2\2;\17\3\2\2" +
                    "\2<A\5\22\n\2=>\7\n\2\2>@\7\13\2\2?=\3\2\2\2@C\3\2\2\2A?\3\2\2\2AB\3\2" +
                    "\2\2B\21\3\2\2\2CA\3\2\2\2DI\7\30\2\2EF\t\2\2\2FH\7\30\2\2GE\3\2\2\2H" +
                    "K\3\2\2\2IG\3\2\2\2IJ\3\2\2\2J\23\3\2\2\2KI\3\2\2\2LM\t\3\2\2M\25\3\2" +
                    "\2\2\t\33\36\'/:AI";
    public static final ATN _ATN =
            new ATNDeserializer().deserialize(_serializedATN.toCharArray());

    static {
        _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
        for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
            _decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
        }
    }
}