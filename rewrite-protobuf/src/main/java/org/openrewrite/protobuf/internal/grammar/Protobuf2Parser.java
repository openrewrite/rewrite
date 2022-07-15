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
// Generated from /Users/yoshi/Development/Repos/openrewrite/rewrite/rewrite-protobuf/src/main/antlr/Protobuf2Parser.g4 by ANTLR 4.9.3
package org.openrewrite.protobuf.internal.grammar;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class Protobuf2Parser extends Parser {
    static {
        RuntimeMetaData.checkVersion("4.9.3", RuntimeMetaData.VERSION);
    }

    protected static final DFA[] _decisionToDFA;
    protected static final PredictionContextCache _sharedContextCache =
            new PredictionContextCache();
    public static final int
            SEMI = 1, COLON = 2, BOOL = 3, BYTES = 4, DOUBLE = 5, ENUM = 6, EXTEND = 7, FIXED32 = 8,
            FIXED64 = 9, FLOAT = 10, IMPORT = 11, INT32 = 12, INT64 = 13, MAP = 14, MESSAGE = 15,
            ONEOF = 16, OPTION = 17, PACKAGE = 18, PUBLIC = 19, REPEATED = 20, REQUIRED = 21,
            RESERVED = 22, RETURNS = 23, RPC = 24, SERVICE = 25, SFIXED32 = 26, SFIXED64 = 27,
            SINT32 = 28, SINT64 = 29, STREAM = 30, STRING = 31, SYNTAX = 32, TO = 33, UINT32 = 34,
            UINT64 = 35, WEAK = 36, OPTIONAL = 37, Ident = 38, IntegerLiteral = 39, NumericLiteral = 40,
            FloatLiteral = 41, BooleanLiteral = 42, StringLiteral = 43, Quote = 44, LPAREN = 45,
            RPAREN = 46, LBRACE = 47, RBRACE = 48, LBRACK = 49, RBRACK = 50, LCHEVR = 51, RCHEVR = 52,
            COMMA = 53, DOT = 54, MINUS = 55, PLUS = 56, ASSIGN = 57, WS = 58, UTF_8_BOM = 59, COMMENT = 60,
            LINE_COMMENT = 61;
    public static final int
            RULE_proto = 0, RULE_stringLiteral = 1, RULE_identOrReserved = 2, RULE_syntax = 3,
            RULE_importStatement = 4, RULE_packageStatement = 5, RULE_optionName = 6,
            RULE_option = 7, RULE_optionDef = 8, RULE_optionList = 9, RULE_topLevelDef = 10,
            RULE_ident = 11, RULE_message = 12, RULE_messageField = 13, RULE_messageBody = 14,
            RULE_extend = 15, RULE_enumDefinition = 16, RULE_enumBody = 17, RULE_enumField = 18,
            RULE_service = 19, RULE_serviceBody = 20, RULE_rpc = 21, RULE_rpcInOut = 22,
            RULE_rpcBody = 23, RULE_reserved = 24, RULE_ranges = 25, RULE_range = 26,
            RULE_fieldNames = 27, RULE_type = 28, RULE_field = 29, RULE_oneOf = 30,
            RULE_mapField = 31, RULE_keyType = 32, RULE_reservedWord = 33, RULE_fullIdent = 34,
            RULE_emptyStatement = 35, RULE_constant = 36;

    private static String[] makeRuleNames() {
        return new String[]{
                "proto", "stringLiteral", "identOrReserved", "syntax", "importStatement",
                "packageStatement", "optionName", "option", "optionDef", "optionList",
                "topLevelDef", "ident", "message", "messageField", "messageBody", "extend",
                "enumDefinition", "enumBody", "enumField", "service", "serviceBody",
                "rpc", "rpcInOut", "rpcBody", "reserved", "ranges", "range", "fieldNames",
                "type", "field", "oneOf", "mapField", "keyType", "reservedWord", "fullIdent",
                "emptyStatement", "constant"
        };
    }
    public static final String[] ruleNames = makeRuleNames();

    private static String[] makeLiteralNames() {
        return new String[]{
                null, "';'", "':'", "'bool'", "'bytes'", "'double'", "'enum'", "'extend'",
                "'fixed32'", "'fixed64'", "'float'", "'import'", "'int32'", "'int64'",
                "'map'", "'message'", "'oneof'", "'option'", "'package'", "'public'",
                "'repeated'", "'required'", "'reserved'", "'returns'", "'rpc'", "'service'",
                "'sfixed32'", "'sfixed64'", "'sint32'", "'sint64'", "'stream'", "'string'",
                "'syntax'", "'to'", "'uint32'", "'uint64'", "'weak'", "'optional'", null,
                null, null, null, null, null, null, "'('", "')'", "'{'", "'}'", "'['",
                "']'", "'<'", "'>'", "','", "'.'", "'-'", "'+'", "'='", null, "'\uFEFF'"
        };
    }
    private static final String[] _LITERAL_NAMES = makeLiteralNames();

    private static String[] makeSymbolicNames() {
        return new String[]{
                null, "SEMI", "COLON", "BOOL", "BYTES", "DOUBLE", "ENUM", "EXTEND", "FIXED32",
                "FIXED64", "FLOAT", "IMPORT", "INT32", "INT64", "MAP", "MESSAGE", "ONEOF",
                "OPTION", "PACKAGE", "PUBLIC", "REPEATED", "REQUIRED", "RESERVED", "RETURNS",
                "RPC", "SERVICE", "SFIXED32", "SFIXED64", "SINT32", "SINT64", "STREAM",
                "STRING", "SYNTAX", "TO", "UINT32", "UINT64", "WEAK", "OPTIONAL", "Ident",
                "IntegerLiteral", "NumericLiteral", "FloatLiteral", "BooleanLiteral",
                "StringLiteral", "Quote", "LPAREN", "RPAREN", "LBRACE", "RBRACE", "LBRACK",
                "RBRACK", "LCHEVR", "RCHEVR", "COMMA", "DOT", "MINUS", "PLUS", "ASSIGN",
                "WS", "UTF_8_BOM", "COMMENT", "LINE_COMMENT"
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
        return "Protobuf2Parser.g4";
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

    public Protobuf2Parser(TokenStream input) {
        super(input);
        _interp = new ParserATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
    }

    public static class ProtoContext extends ParserRuleContext {
        public SyntaxContext syntax() {
            return getRuleContext(SyntaxContext.class, 0);
        }

        public TerminalNode EOF() {
            return getToken(Protobuf2Parser.EOF, 0);
        }

        public List<ImportStatementContext> importStatement() {
            return getRuleContexts(ImportStatementContext.class);
        }

        public ImportStatementContext importStatement(int i) {
            return getRuleContext(ImportStatementContext.class, i);
        }

        public List<PackageStatementContext> packageStatement() {
            return getRuleContexts(PackageStatementContext.class);
        }

        public PackageStatementContext packageStatement(int i) {
            return getRuleContext(PackageStatementContext.class, i);
        }

        public List<OptionDefContext> optionDef() {
            return getRuleContexts(OptionDefContext.class);
        }

        public OptionDefContext optionDef(int i) {
            return getRuleContext(OptionDefContext.class, i);
        }

        public List<TopLevelDefContext> topLevelDef() {
            return getRuleContexts(TopLevelDefContext.class);
        }

        public TopLevelDefContext topLevelDef(int i) {
            return getRuleContext(TopLevelDefContext.class, i);
        }

        public List<EmptyStatementContext> emptyStatement() {
            return getRuleContexts(EmptyStatementContext.class);
        }

        public EmptyStatementContext emptyStatement(int i) {
            return getRuleContext(EmptyStatementContext.class, i);
        }

        public ProtoContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_proto;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterProto(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitProto(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitProto(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ProtoContext proto() throws RecognitionException {
        ProtoContext _localctx = new ProtoContext(_ctx, getState());
        enterRule(_localctx, 0, RULE_proto);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(74);
                syntax();
                setState(82);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << SEMI) | (1L << ENUM) | (1L << EXTEND) | (1L << IMPORT) | (1L << MESSAGE) | (1L << OPTION) | (1L << PACKAGE) | (1L << SERVICE))) != 0)) {
                    {
                        setState(80);
                        _errHandler.sync(this);
                        switch (_input.LA(1)) {
                            case IMPORT:
                            {
                                setState(75);
                                importStatement();
                            }
                                break;
                            case PACKAGE:
                            {
                                setState(76);
                                packageStatement();
                            }
                                break;
                            case OPTION:
                            {
                                setState(77);
                                optionDef();
                            }
                                break;
                            case ENUM:
                            case EXTEND:
                            case MESSAGE:
                            case SERVICE:
                            {
                                setState(78);
                                topLevelDef();
                            }
                                break;
                            case SEMI:
                            {
                                setState(79);
                                emptyStatement();
                            }
                                break;
                            default:
                                throw new NoViableAltException(this);
                        }
                    }
                    setState(84);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
                setState(85);
                match(EOF);
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
        public TerminalNode StringLiteral() {
            return getToken(Protobuf2Parser.StringLiteral, 0);
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
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterStringLiteral(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitStringLiteral(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitStringLiteral(this);
            else return visitor.visitChildren(this);
        }
    }

    public final StringLiteralContext stringLiteral() throws RecognitionException {
        StringLiteralContext _localctx = new StringLiteralContext(_ctx, getState());
        enterRule(_localctx, 2, RULE_stringLiteral);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(87);
                match(StringLiteral);
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

    public static class IdentOrReservedContext extends ParserRuleContext {
        public IdentContext ident() {
            return getRuleContext(IdentContext.class, 0);
        }

        public ReservedWordContext reservedWord() {
            return getRuleContext(ReservedWordContext.class, 0);
        }

        public IdentOrReservedContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_identOrReserved;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterIdentOrReserved(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitIdentOrReserved(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitIdentOrReserved(this);
            else return visitor.visitChildren(this);
        }
    }

    public final IdentOrReservedContext identOrReserved() throws RecognitionException {
        IdentOrReservedContext _localctx = new IdentOrReservedContext(_ctx, getState());
        enterRule(_localctx, 4, RULE_identOrReserved);
        try {
            setState(91);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case Ident:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(89);
                    ident();
                }
                    break;
                case MESSAGE:
                case OPTION:
                case PACKAGE:
                case RPC:
                case SERVICE:
                case STREAM:
                case STRING:
                case SYNTAX:
                case WEAK:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(90);
                    reservedWord();
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

    public static class SyntaxContext extends ParserRuleContext {
        public TerminalNode SYNTAX() {
            return getToken(Protobuf2Parser.SYNTAX, 0);
        }

        public TerminalNode ASSIGN() {
            return getToken(Protobuf2Parser.ASSIGN, 0);
        }

        public StringLiteralContext stringLiteral() {
            return getRuleContext(StringLiteralContext.class, 0);
        }

        public TerminalNode SEMI() {
            return getToken(Protobuf2Parser.SEMI, 0);
        }

        public SyntaxContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_syntax;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterSyntax(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitSyntax(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitSyntax(this);
            else return visitor.visitChildren(this);
        }
    }

    public final SyntaxContext syntax() throws RecognitionException {
        SyntaxContext _localctx = new SyntaxContext(_ctx, getState());
        enterRule(_localctx, 6, RULE_syntax);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(93);
                match(SYNTAX);
                setState(94);
                match(ASSIGN);
                setState(95);
                stringLiteral();
                setState(96);
                match(SEMI);
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

    public static class ImportStatementContext extends ParserRuleContext {
        public TerminalNode IMPORT() {
            return getToken(Protobuf2Parser.IMPORT, 0);
        }

        public StringLiteralContext stringLiteral() {
            return getRuleContext(StringLiteralContext.class, 0);
        }

        public TerminalNode SEMI() {
            return getToken(Protobuf2Parser.SEMI, 0);
        }

        public TerminalNode WEAK() {
            return getToken(Protobuf2Parser.WEAK, 0);
        }

        public TerminalNode PUBLIC() {
            return getToken(Protobuf2Parser.PUBLIC, 0);
        }

        public ImportStatementContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_importStatement;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterImportStatement(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitImportStatement(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitImportStatement(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ImportStatementContext importStatement() throws RecognitionException {
        ImportStatementContext _localctx = new ImportStatementContext(_ctx, getState());
        enterRule(_localctx, 8, RULE_importStatement);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(98);
                match(IMPORT);
                setState(100);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == PUBLIC || _la == WEAK) {
                    {
                        setState(99);
                        _la = _input.LA(1);
                        if (!(_la == PUBLIC || _la == WEAK)) {
                            _errHandler.recoverInline(this);
                        }
                        else {
                            if (_input.LA(1) == Token.EOF) matchedEOF = true;
                            _errHandler.reportMatch(this);
                            consume();
                        }
                    }
                }

                setState(102);
                stringLiteral();
                setState(103);
                match(SEMI);
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

    public static class PackageStatementContext extends ParserRuleContext {
        public TerminalNode PACKAGE() {
            return getToken(Protobuf2Parser.PACKAGE, 0);
        }

        public FullIdentContext fullIdent() {
            return getRuleContext(FullIdentContext.class, 0);
        }

        public TerminalNode SEMI() {
            return getToken(Protobuf2Parser.SEMI, 0);
        }

        public PackageStatementContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_packageStatement;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterPackageStatement(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitPackageStatement(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitPackageStatement(this);
            else return visitor.visitChildren(this);
        }
    }

    public final PackageStatementContext packageStatement() throws RecognitionException {
        PackageStatementContext _localctx = new PackageStatementContext(_ctx, getState());
        enterRule(_localctx, 10, RULE_packageStatement);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(105);
                match(PACKAGE);
                setState(106);
                fullIdent();
                setState(107);
                match(SEMI);
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

    public static class OptionNameContext extends ParserRuleContext {
        public IdentContext ident() {
            return getRuleContext(IdentContext.class, 0);
        }

        public TerminalNode LPAREN() {
            return getToken(Protobuf2Parser.LPAREN, 0);
        }

        public FullIdentContext fullIdent() {
            return getRuleContext(FullIdentContext.class, 0);
        }

        public TerminalNode RPAREN() {
            return getToken(Protobuf2Parser.RPAREN, 0);
        }

        public List<TerminalNode> DOT() {
            return getTokens(Protobuf2Parser.DOT);
        }

        public TerminalNode DOT(int i) {
            return getToken(Protobuf2Parser.DOT, i);
        }

        public List<IdentOrReservedContext> identOrReserved() {
            return getRuleContexts(IdentOrReservedContext.class);
        }

        public IdentOrReservedContext identOrReserved(int i) {
            return getRuleContext(IdentOrReservedContext.class, i);
        }

        public OptionNameContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_optionName;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterOptionName(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitOptionName(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitOptionName(this);
            else return visitor.visitChildren(this);
        }
    }

    public final OptionNameContext optionName() throws RecognitionException {
        OptionNameContext _localctx = new OptionNameContext(_ctx, getState());
        enterRule(_localctx, 12, RULE_optionName);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(114);
                _errHandler.sync(this);
                switch (_input.LA(1)) {
                    case Ident:
                    {
                        setState(109);
                        ident();
                    }
                        break;
                    case LPAREN:
                    {
                        setState(110);
                        match(LPAREN);
                        setState(111);
                        fullIdent();
                        setState(112);
                        match(RPAREN);
                    }
                        break;
                    default:
                        throw new NoViableAltException(this);
                }
                setState(120);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == DOT) {
                    {
                        {
                            setState(116);
                            match(DOT);
                            setState(117);
                            identOrReserved();
                        }
                    }
                    setState(122);
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

    public static class OptionContext extends ParserRuleContext {
        public OptionNameContext optionName() {
            return getRuleContext(OptionNameContext.class, 0);
        }

        public TerminalNode ASSIGN() {
            return getToken(Protobuf2Parser.ASSIGN, 0);
        }

        public ConstantContext constant() {
            return getRuleContext(ConstantContext.class, 0);
        }

        public OptionContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_option;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterOption(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitOption(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitOption(this);
            else return visitor.visitChildren(this);
        }
    }

    public final OptionContext option() throws RecognitionException {
        OptionContext _localctx = new OptionContext(_ctx, getState());
        enterRule(_localctx, 14, RULE_option);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(123);
                optionName();
                setState(124);
                match(ASSIGN);
                setState(125);
                constant();
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

    public static class OptionDefContext extends ParserRuleContext {
        public TerminalNode OPTION() {
            return getToken(Protobuf2Parser.OPTION, 0);
        }

        public OptionContext option() {
            return getRuleContext(OptionContext.class, 0);
        }

        public TerminalNode SEMI() {
            return getToken(Protobuf2Parser.SEMI, 0);
        }

        public OptionDefContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_optionDef;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterOptionDef(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitOptionDef(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitOptionDef(this);
            else return visitor.visitChildren(this);
        }
    }

    public final OptionDefContext optionDef() throws RecognitionException {
        OptionDefContext _localctx = new OptionDefContext(_ctx, getState());
        enterRule(_localctx, 16, RULE_optionDef);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(127);
                match(OPTION);
                setState(128);
                option();
                setState(129);
                match(SEMI);
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

    public static class OptionListContext extends ParserRuleContext {
        public TerminalNode LBRACK() {
            return getToken(Protobuf2Parser.LBRACK, 0);
        }

        public List<OptionContext> option() {
            return getRuleContexts(OptionContext.class);
        }

        public OptionContext option(int i) {
            return getRuleContext(OptionContext.class, i);
        }

        public TerminalNode RBRACK() {
            return getToken(Protobuf2Parser.RBRACK, 0);
        }

        public List<TerminalNode> COMMA() {
            return getTokens(Protobuf2Parser.COMMA);
        }

        public TerminalNode COMMA(int i) {
            return getToken(Protobuf2Parser.COMMA, i);
        }

        public OptionListContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_optionList;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterOptionList(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitOptionList(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitOptionList(this);
            else return visitor.visitChildren(this);
        }
    }

    public final OptionListContext optionList() throws RecognitionException {
        OptionListContext _localctx = new OptionListContext(_ctx, getState());
        enterRule(_localctx, 18, RULE_optionList);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                {
                    setState(131);
                    match(LBRACK);
                    setState(132);
                    option();
                    setState(137);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                    while (_la == COMMA) {
                        {
                            {
                                setState(133);
                                match(COMMA);
                                setState(134);
                                option();
                            }
                        }
                        setState(139);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                    }
                    setState(140);
                    match(RBRACK);
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

    public static class TopLevelDefContext extends ParserRuleContext {
        public MessageContext message() {
            return getRuleContext(MessageContext.class, 0);
        }

        public EnumDefinitionContext enumDefinition() {
            return getRuleContext(EnumDefinitionContext.class, 0);
        }

        public ServiceContext service() {
            return getRuleContext(ServiceContext.class, 0);
        }

        public ExtendContext extend() {
            return getRuleContext(ExtendContext.class, 0);
        }

        public TopLevelDefContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_topLevelDef;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterTopLevelDef(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitTopLevelDef(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitTopLevelDef(this);
            else return visitor.visitChildren(this);
        }
    }

    public final TopLevelDefContext topLevelDef() throws RecognitionException {
        TopLevelDefContext _localctx = new TopLevelDefContext(_ctx, getState());
        enterRule(_localctx, 20, RULE_topLevelDef);
        try {
            setState(146);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case MESSAGE:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(142);
                    message();
                }
                    break;
                case ENUM:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(143);
                    enumDefinition();
                }
                    break;
                case SERVICE:
                    enterOuterAlt(_localctx, 3);
                {
                    setState(144);
                    service();
                }
                    break;
                case EXTEND:
                    enterOuterAlt(_localctx, 4);
                {
                    setState(145);
                    extend();
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

    public static class IdentContext extends ParserRuleContext {
        public TerminalNode Ident() {
            return getToken(Protobuf2Parser.Ident, 0);
        }

        public IdentContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_ident;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterIdent(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitIdent(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitIdent(this);
            else return visitor.visitChildren(this);
        }
    }

    public final IdentContext ident() throws RecognitionException {
        IdentContext _localctx = new IdentContext(_ctx, getState());
        enterRule(_localctx, 22, RULE_ident);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(148);
                match(Ident);
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

    public static class MessageContext extends ParserRuleContext {
        public TerminalNode MESSAGE() {
            return getToken(Protobuf2Parser.MESSAGE, 0);
        }

        public IdentContext ident() {
            return getRuleContext(IdentContext.class, 0);
        }

        public MessageBodyContext messageBody() {
            return getRuleContext(MessageBodyContext.class, 0);
        }

        public MessageContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_message;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterMessage(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitMessage(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitMessage(this);
            else return visitor.visitChildren(this);
        }
    }

    public final MessageContext message() throws RecognitionException {
        MessageContext _localctx = new MessageContext(_ctx, getState());
        enterRule(_localctx, 24, RULE_message);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(150);
                match(MESSAGE);
                setState(151);
                ident();
                setState(152);
                messageBody();
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

    public static class MessageFieldContext extends ParserRuleContext {
        public FieldContext field() {
            return getRuleContext(FieldContext.class, 0);
        }

        public TerminalNode OPTIONAL() {
            return getToken(Protobuf2Parser.OPTIONAL, 0);
        }

        public TerminalNode REQUIRED() {
            return getToken(Protobuf2Parser.REQUIRED, 0);
        }

        public TerminalNode REPEATED() {
            return getToken(Protobuf2Parser.REPEATED, 0);
        }

        public MessageFieldContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_messageField;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterMessageField(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitMessageField(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitMessageField(this);
            else return visitor.visitChildren(this);
        }
    }

    public final MessageFieldContext messageField() throws RecognitionException {
        MessageFieldContext _localctx = new MessageFieldContext(_ctx, getState());
        enterRule(_localctx, 26, RULE_messageField);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(154);
                _la = _input.LA(1);
                if (!((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << REPEATED) | (1L << REQUIRED) | (1L << OPTIONAL))) != 0))) {
                    _errHandler.recoverInline(this);
                }
                else {
                    if (_input.LA(1) == Token.EOF) matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
                setState(155);
                field();
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

    public static class MessageBodyContext extends ParserRuleContext {
        public TerminalNode LBRACE() {
            return getToken(Protobuf2Parser.LBRACE, 0);
        }

        public TerminalNode RBRACE() {
            return getToken(Protobuf2Parser.RBRACE, 0);
        }

        public List<MessageFieldContext> messageField() {
            return getRuleContexts(MessageFieldContext.class);
        }

        public MessageFieldContext messageField(int i) {
            return getRuleContext(MessageFieldContext.class, i);
        }

        public List<EnumDefinitionContext> enumDefinition() {
            return getRuleContexts(EnumDefinitionContext.class);
        }

        public EnumDefinitionContext enumDefinition(int i) {
            return getRuleContext(EnumDefinitionContext.class, i);
        }

        public List<ExtendContext> extend() {
            return getRuleContexts(ExtendContext.class);
        }

        public ExtendContext extend(int i) {
            return getRuleContext(ExtendContext.class, i);
        }

        public List<MessageContext> message() {
            return getRuleContexts(MessageContext.class);
        }

        public MessageContext message(int i) {
            return getRuleContext(MessageContext.class, i);
        }

        public List<OptionDefContext> optionDef() {
            return getRuleContexts(OptionDefContext.class);
        }

        public OptionDefContext optionDef(int i) {
            return getRuleContext(OptionDefContext.class, i);
        }

        public List<OneOfContext> oneOf() {
            return getRuleContexts(OneOfContext.class);
        }

        public OneOfContext oneOf(int i) {
            return getRuleContext(OneOfContext.class, i);
        }

        public List<MapFieldContext> mapField() {
            return getRuleContexts(MapFieldContext.class);
        }

        public MapFieldContext mapField(int i) {
            return getRuleContext(MapFieldContext.class, i);
        }

        public List<ReservedContext> reserved() {
            return getRuleContexts(ReservedContext.class);
        }

        public ReservedContext reserved(int i) {
            return getRuleContext(ReservedContext.class, i);
        }

        public List<EmptyStatementContext> emptyStatement() {
            return getRuleContexts(EmptyStatementContext.class);
        }

        public EmptyStatementContext emptyStatement(int i) {
            return getRuleContext(EmptyStatementContext.class, i);
        }

        public MessageBodyContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_messageBody;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterMessageBody(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitMessageBody(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitMessageBody(this);
            else return visitor.visitChildren(this);
        }
    }

    public final MessageBodyContext messageBody() throws RecognitionException {
        MessageBodyContext _localctx = new MessageBodyContext(_ctx, getState());
        enterRule(_localctx, 28, RULE_messageBody);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(157);
                match(LBRACE);
                setState(169);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << SEMI) | (1L << ENUM) | (1L << EXTEND) | (1L << MAP) | (1L << MESSAGE) | (1L << ONEOF) | (1L << OPTION) | (1L << REPEATED) | (1L << REQUIRED) | (1L << RESERVED) | (1L << OPTIONAL))) != 0)) {
                    {
                        setState(167);
                        _errHandler.sync(this);
                        switch (_input.LA(1)) {
                            case REPEATED:
                            case REQUIRED:
                            case OPTIONAL:
                            {
                                setState(158);
                                messageField();
                            }
                                break;
                            case ENUM:
                            {
                                setState(159);
                                enumDefinition();
                            }
                                break;
                            case EXTEND:
                            {
                                setState(160);
                                extend();
                            }
                                break;
                            case MESSAGE:
                            {
                                setState(161);
                                message();
                            }
                                break;
                            case OPTION:
                            {
                                setState(162);
                                optionDef();
                            }
                                break;
                            case ONEOF:
                            {
                                setState(163);
                                oneOf();
                            }
                                break;
                            case MAP:
                            {
                                setState(164);
                                mapField();
                            }
                                break;
                            case RESERVED:
                            {
                                setState(165);
                                reserved();
                            }
                                break;
                            case SEMI:
                            {
                                setState(166);
                                emptyStatement();
                            }
                                break;
                            default:
                                throw new NoViableAltException(this);
                        }
                    }
                    setState(171);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
                setState(172);
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

    public static class ExtendContext extends ParserRuleContext {
        public TerminalNode EXTEND() {
            return getToken(Protobuf2Parser.EXTEND, 0);
        }

        public FullIdentContext fullIdent() {
            return getRuleContext(FullIdentContext.class, 0);
        }

        public TerminalNode LBRACE() {
            return getToken(Protobuf2Parser.LBRACE, 0);
        }

        public TerminalNode RBRACE() {
            return getToken(Protobuf2Parser.RBRACE, 0);
        }

        public List<MessageFieldContext> messageField() {
            return getRuleContexts(MessageFieldContext.class);
        }

        public MessageFieldContext messageField(int i) {
            return getRuleContext(MessageFieldContext.class, i);
        }

        public List<EmptyStatementContext> emptyStatement() {
            return getRuleContexts(EmptyStatementContext.class);
        }

        public EmptyStatementContext emptyStatement(int i) {
            return getRuleContext(EmptyStatementContext.class, i);
        }

        public ExtendContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_extend;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterExtend(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitExtend(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitExtend(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ExtendContext extend() throws RecognitionException {
        ExtendContext _localctx = new ExtendContext(_ctx, getState());
        enterRule(_localctx, 30, RULE_extend);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(174);
                match(EXTEND);
                setState(175);
                fullIdent();
                setState(176);
                match(LBRACE);
                setState(181);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << SEMI) | (1L << REPEATED) | (1L << REQUIRED) | (1L << OPTIONAL))) != 0)) {
                    {
                        setState(179);
                        _errHandler.sync(this);
                        switch (_input.LA(1)) {
                            case REPEATED:
                            case REQUIRED:
                            case OPTIONAL:
                            {
                                setState(177);
                                messageField();
                            }
                                break;
                            case SEMI:
                            {
                                setState(178);
                                emptyStatement();
                            }
                                break;
                            default:
                                throw new NoViableAltException(this);
                        }
                    }
                    setState(183);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
                setState(184);
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

    public static class EnumDefinitionContext extends ParserRuleContext {
        public TerminalNode ENUM() {
            return getToken(Protobuf2Parser.ENUM, 0);
        }

        public IdentContext ident() {
            return getRuleContext(IdentContext.class, 0);
        }

        public EnumBodyContext enumBody() {
            return getRuleContext(EnumBodyContext.class, 0);
        }

        public EnumDefinitionContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_enumDefinition;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterEnumDefinition(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitEnumDefinition(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitEnumDefinition(this);
            else return visitor.visitChildren(this);
        }
    }

    public final EnumDefinitionContext enumDefinition() throws RecognitionException {
        EnumDefinitionContext _localctx = new EnumDefinitionContext(_ctx, getState());
        enterRule(_localctx, 32, RULE_enumDefinition);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(186);
                match(ENUM);
                setState(187);
                ident();
                setState(188);
                enumBody();
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

    public static class EnumBodyContext extends ParserRuleContext {
        public TerminalNode LBRACE() {
            return getToken(Protobuf2Parser.LBRACE, 0);
        }

        public TerminalNode RBRACE() {
            return getToken(Protobuf2Parser.RBRACE, 0);
        }

        public List<OptionDefContext> optionDef() {
            return getRuleContexts(OptionDefContext.class);
        }

        public OptionDefContext optionDef(int i) {
            return getRuleContext(OptionDefContext.class, i);
        }

        public List<EnumFieldContext> enumField() {
            return getRuleContexts(EnumFieldContext.class);
        }

        public EnumFieldContext enumField(int i) {
            return getRuleContext(EnumFieldContext.class, i);
        }

        public List<EmptyStatementContext> emptyStatement() {
            return getRuleContexts(EmptyStatementContext.class);
        }

        public EmptyStatementContext emptyStatement(int i) {
            return getRuleContext(EmptyStatementContext.class, i);
        }

        public EnumBodyContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_enumBody;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterEnumBody(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitEnumBody(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitEnumBody(this);
            else return visitor.visitChildren(this);
        }
    }

    public final EnumBodyContext enumBody() throws RecognitionException {
        EnumBodyContext _localctx = new EnumBodyContext(_ctx, getState());
        enterRule(_localctx, 34, RULE_enumBody);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(190);
                match(LBRACE);
                setState(196);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << SEMI) | (1L << OPTION) | (1L << Ident))) != 0)) {
                    {
                        setState(194);
                        _errHandler.sync(this);
                        switch (_input.LA(1)) {
                            case OPTION:
                            {
                                setState(191);
                                optionDef();
                            }
                                break;
                            case Ident:
                            {
                                setState(192);
                                enumField();
                            }
                                break;
                            case SEMI:
                            {
                                setState(193);
                                emptyStatement();
                            }
                                break;
                            default:
                                throw new NoViableAltException(this);
                        }
                    }
                    setState(198);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
                setState(199);
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

    public static class EnumFieldContext extends ParserRuleContext {
        public IdentContext ident() {
            return getRuleContext(IdentContext.class, 0);
        }

        public TerminalNode ASSIGN() {
            return getToken(Protobuf2Parser.ASSIGN, 0);
        }

        public TerminalNode IntegerLiteral() {
            return getToken(Protobuf2Parser.IntegerLiteral, 0);
        }

        public TerminalNode SEMI() {
            return getToken(Protobuf2Parser.SEMI, 0);
        }

        public TerminalNode MINUS() {
            return getToken(Protobuf2Parser.MINUS, 0);
        }

        public OptionListContext optionList() {
            return getRuleContext(OptionListContext.class, 0);
        }

        public EnumFieldContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_enumField;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterEnumField(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitEnumField(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitEnumField(this);
            else return visitor.visitChildren(this);
        }
    }

    public final EnumFieldContext enumField() throws RecognitionException {
        EnumFieldContext _localctx = new EnumFieldContext(_ctx, getState());
        enterRule(_localctx, 36, RULE_enumField);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(201);
                ident();
                setState(202);
                match(ASSIGN);
                setState(204);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == MINUS) {
                    {
                        setState(203);
                        match(MINUS);
                    }
                }

                setState(206);
                match(IntegerLiteral);
                setState(208);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == LBRACK) {
                    {
                        setState(207);
                        optionList();
                    }
                }

                setState(210);
                match(SEMI);
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

    public static class ServiceContext extends ParserRuleContext {
        public TerminalNode SERVICE() {
            return getToken(Protobuf2Parser.SERVICE, 0);
        }

        public IdentContext ident() {
            return getRuleContext(IdentContext.class, 0);
        }

        public ServiceBodyContext serviceBody() {
            return getRuleContext(ServiceBodyContext.class, 0);
        }

        public ServiceContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_service;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterService(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitService(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitService(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ServiceContext service() throws RecognitionException {
        ServiceContext _localctx = new ServiceContext(_ctx, getState());
        enterRule(_localctx, 38, RULE_service);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(212);
                match(SERVICE);
                setState(213);
                ident();
                setState(214);
                serviceBody();
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

    public static class ServiceBodyContext extends ParserRuleContext {
        public TerminalNode LBRACE() {
            return getToken(Protobuf2Parser.LBRACE, 0);
        }

        public TerminalNode RBRACE() {
            return getToken(Protobuf2Parser.RBRACE, 0);
        }

        public List<OptionDefContext> optionDef() {
            return getRuleContexts(OptionDefContext.class);
        }

        public OptionDefContext optionDef(int i) {
            return getRuleContext(OptionDefContext.class, i);
        }

        public List<RpcContext> rpc() {
            return getRuleContexts(RpcContext.class);
        }

        public RpcContext rpc(int i) {
            return getRuleContext(RpcContext.class, i);
        }

        public List<EmptyStatementContext> emptyStatement() {
            return getRuleContexts(EmptyStatementContext.class);
        }

        public EmptyStatementContext emptyStatement(int i) {
            return getRuleContext(EmptyStatementContext.class, i);
        }

        public ServiceBodyContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_serviceBody;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterServiceBody(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitServiceBody(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitServiceBody(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ServiceBodyContext serviceBody() throws RecognitionException {
        ServiceBodyContext _localctx = new ServiceBodyContext(_ctx, getState());
        enterRule(_localctx, 40, RULE_serviceBody);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(216);
                match(LBRACE);
                setState(222);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << SEMI) | (1L << OPTION) | (1L << RPC))) != 0)) {
                    {
                        setState(220);
                        _errHandler.sync(this);
                        switch (_input.LA(1)) {
                            case OPTION:
                            {
                                setState(217);
                                optionDef();
                            }
                                break;
                            case RPC:
                            {
                                setState(218);
                                rpc();
                            }
                                break;
                            case SEMI:
                            {
                                setState(219);
                                emptyStatement();
                            }
                                break;
                            default:
                                throw new NoViableAltException(this);
                        }
                    }
                    setState(224);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
                setState(225);
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

    public static class RpcContext extends ParserRuleContext {
        public TerminalNode RPC() {
            return getToken(Protobuf2Parser.RPC, 0);
        }

        public IdentContext ident() {
            return getRuleContext(IdentContext.class, 0);
        }

        public List<RpcInOutContext> rpcInOut() {
            return getRuleContexts(RpcInOutContext.class);
        }

        public RpcInOutContext rpcInOut(int i) {
            return getRuleContext(RpcInOutContext.class, i);
        }

        public TerminalNode RETURNS() {
            return getToken(Protobuf2Parser.RETURNS, 0);
        }

        public RpcBodyContext rpcBody() {
            return getRuleContext(RpcBodyContext.class, 0);
        }

        public TerminalNode SEMI() {
            return getToken(Protobuf2Parser.SEMI, 0);
        }

        public RpcContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_rpc;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterRpc(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitRpc(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitRpc(this);
            else return visitor.visitChildren(this);
        }
    }

    public final RpcContext rpc() throws RecognitionException {
        RpcContext _localctx = new RpcContext(_ctx, getState());
        enterRule(_localctx, 42, RULE_rpc);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(227);
                match(RPC);
                setState(228);
                ident();
                setState(229);
                rpcInOut();
                setState(230);
                match(RETURNS);
                setState(231);
                rpcInOut();
                setState(234);
                _errHandler.sync(this);
                switch (_input.LA(1)) {
                    case LBRACE:
                    {
                        setState(232);
                        rpcBody();
                    }
                        break;
                    case SEMI:
                    {
                        setState(233);
                        match(SEMI);
                    }
                        break;
                    default:
                        throw new NoViableAltException(this);
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

    public static class RpcInOutContext extends ParserRuleContext {
        public FullIdentContext messageType;

        public TerminalNode LPAREN() {
            return getToken(Protobuf2Parser.LPAREN, 0);
        }

        public TerminalNode RPAREN() {
            return getToken(Protobuf2Parser.RPAREN, 0);
        }

        public FullIdentContext fullIdent() {
            return getRuleContext(FullIdentContext.class, 0);
        }

        public TerminalNode STREAM() {
            return getToken(Protobuf2Parser.STREAM, 0);
        }

        public RpcInOutContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_rpcInOut;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterRpcInOut(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitRpcInOut(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitRpcInOut(this);
            else return visitor.visitChildren(this);
        }
    }

    public final RpcInOutContext rpcInOut() throws RecognitionException {
        RpcInOutContext _localctx = new RpcInOutContext(_ctx, getState());
        enterRule(_localctx, 44, RULE_rpcInOut);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(236);
                match(LPAREN);
                setState(238);
                _errHandler.sync(this);
                switch (getInterpreter().adaptivePredict(_input, 19, _ctx)) {
                    case 1:
                    {
                        setState(237);
                        match(STREAM);
                    }
                        break;
                }
                setState(240);
                ((RpcInOutContext) _localctx).messageType = fullIdent();
                setState(241);
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

    public static class RpcBodyContext extends ParserRuleContext {
        public TerminalNode LBRACE() {
            return getToken(Protobuf2Parser.LBRACE, 0);
        }

        public TerminalNode RBRACE() {
            return getToken(Protobuf2Parser.RBRACE, 0);
        }

        public List<OptionDefContext> optionDef() {
            return getRuleContexts(OptionDefContext.class);
        }

        public OptionDefContext optionDef(int i) {
            return getRuleContext(OptionDefContext.class, i);
        }

        public List<EmptyStatementContext> emptyStatement() {
            return getRuleContexts(EmptyStatementContext.class);
        }

        public EmptyStatementContext emptyStatement(int i) {
            return getRuleContext(EmptyStatementContext.class, i);
        }

        public RpcBodyContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_rpcBody;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterRpcBody(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitRpcBody(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitRpcBody(this);
            else return visitor.visitChildren(this);
        }
    }

    public final RpcBodyContext rpcBody() throws RecognitionException {
        RpcBodyContext _localctx = new RpcBodyContext(_ctx, getState());
        enterRule(_localctx, 46, RULE_rpcBody);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(243);
                match(LBRACE);
                setState(248);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == SEMI || _la == OPTION) {
                    {
                        setState(246);
                        _errHandler.sync(this);
                        switch (_input.LA(1)) {
                            case OPTION:
                            {
                                setState(244);
                                optionDef();
                            }
                                break;
                            case SEMI:
                            {
                                setState(245);
                                emptyStatement();
                            }
                                break;
                            default:
                                throw new NoViableAltException(this);
                        }
                    }
                    setState(250);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
                setState(251);
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

    public static class ReservedContext extends ParserRuleContext {
        public TerminalNode RESERVED() {
            return getToken(Protobuf2Parser.RESERVED, 0);
        }

        public TerminalNode SEMI() {
            return getToken(Protobuf2Parser.SEMI, 0);
        }

        public RangesContext ranges() {
            return getRuleContext(RangesContext.class, 0);
        }

        public FieldNamesContext fieldNames() {
            return getRuleContext(FieldNamesContext.class, 0);
        }

        public ReservedContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_reserved;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterReserved(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitReserved(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitReserved(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ReservedContext reserved() throws RecognitionException {
        ReservedContext _localctx = new ReservedContext(_ctx, getState());
        enterRule(_localctx, 48, RULE_reserved);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(253);
                match(RESERVED);
                setState(256);
                _errHandler.sync(this);
                switch (_input.LA(1)) {
                    case IntegerLiteral:
                    {
                        setState(254);
                        ranges();
                    }
                        break;
                    case StringLiteral:
                    {
                        setState(255);
                        fieldNames();
                    }
                        break;
                    default:
                        throw new NoViableAltException(this);
                }
                setState(258);
                match(SEMI);
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

    public static class RangesContext extends ParserRuleContext {
        public List<RangeContext> range() {
            return getRuleContexts(RangeContext.class);
        }

        public RangeContext range(int i) {
            return getRuleContext(RangeContext.class, i);
        }

        public List<TerminalNode> COMMA() {
            return getTokens(Protobuf2Parser.COMMA);
        }

        public TerminalNode COMMA(int i) {
            return getToken(Protobuf2Parser.COMMA, i);
        }

        public RangesContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_ranges;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterRanges(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitRanges(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitRanges(this);
            else return visitor.visitChildren(this);
        }
    }

    public final RangesContext ranges() throws RecognitionException {
        RangesContext _localctx = new RangesContext(_ctx, getState());
        enterRule(_localctx, 50, RULE_ranges);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(260);
                range();
                setState(265);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == COMMA) {
                    {
                        {
                            setState(261);
                            match(COMMA);
                            setState(262);
                            range();
                        }
                    }
                    setState(267);
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
        public List<TerminalNode> IntegerLiteral() {
            return getTokens(Protobuf2Parser.IntegerLiteral);
        }

        public TerminalNode IntegerLiteral(int i) {
            return getToken(Protobuf2Parser.IntegerLiteral, i);
        }

        public TerminalNode TO() {
            return getToken(Protobuf2Parser.TO, 0);
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
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterRange(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitRange(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitRange(this);
            else return visitor.visitChildren(this);
        }
    }

    public final RangeContext range() throws RecognitionException {
        RangeContext _localctx = new RangeContext(_ctx, getState());
        enterRule(_localctx, 52, RULE_range);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(268);
                match(IntegerLiteral);
                setState(271);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == TO) {
                    {
                        setState(269);
                        match(TO);
                        setState(270);
                        match(IntegerLiteral);
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

    public static class FieldNamesContext extends ParserRuleContext {
        public List<StringLiteralContext> stringLiteral() {
            return getRuleContexts(StringLiteralContext.class);
        }

        public StringLiteralContext stringLiteral(int i) {
            return getRuleContext(StringLiteralContext.class, i);
        }

        public List<TerminalNode> COMMA() {
            return getTokens(Protobuf2Parser.COMMA);
        }

        public TerminalNode COMMA(int i) {
            return getToken(Protobuf2Parser.COMMA, i);
        }

        public FieldNamesContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_fieldNames;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterFieldNames(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitFieldNames(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitFieldNames(this);
            else return visitor.visitChildren(this);
        }
    }

    public final FieldNamesContext fieldNames() throws RecognitionException {
        FieldNamesContext _localctx = new FieldNamesContext(_ctx, getState());
        enterRule(_localctx, 54, RULE_fieldNames);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(273);
                stringLiteral();
                setState(278);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == COMMA) {
                    {
                        {
                            setState(274);
                            match(COMMA);
                            setState(275);
                            stringLiteral();
                        }
                    }
                    setState(280);
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

    public static class TypeContext extends ParserRuleContext {
        public TypeContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_type;
        }

        public TypeContext() {
        }

        public void copyFrom(TypeContext ctx) {
            super.copyFrom(ctx);
        }
    }

    public static class FullyQualifiedTypeContext extends TypeContext {
        public FullIdentContext fullIdent() {
            return getRuleContext(FullIdentContext.class, 0);
        }

        public FullyQualifiedTypeContext(TypeContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterFullyQualifiedType(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitFullyQualifiedType(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitFullyQualifiedType(this);
            else return visitor.visitChildren(this);
        }
    }

    public static class PrimitiveTypeContext extends TypeContext {
        public TerminalNode DOUBLE() {
            return getToken(Protobuf2Parser.DOUBLE, 0);
        }

        public TerminalNode FLOAT() {
            return getToken(Protobuf2Parser.FLOAT, 0);
        }

        public TerminalNode INT32() {
            return getToken(Protobuf2Parser.INT32, 0);
        }

        public TerminalNode INT64() {
            return getToken(Protobuf2Parser.INT64, 0);
        }

        public TerminalNode UINT32() {
            return getToken(Protobuf2Parser.UINT32, 0);
        }

        public TerminalNode UINT64() {
            return getToken(Protobuf2Parser.UINT64, 0);
        }

        public TerminalNode SINT32() {
            return getToken(Protobuf2Parser.SINT32, 0);
        }

        public TerminalNode SINT64() {
            return getToken(Protobuf2Parser.SINT64, 0);
        }

        public TerminalNode FIXED32() {
            return getToken(Protobuf2Parser.FIXED32, 0);
        }

        public TerminalNode FIXED64() {
            return getToken(Protobuf2Parser.FIXED64, 0);
        }

        public TerminalNode SFIXED32() {
            return getToken(Protobuf2Parser.SFIXED32, 0);
        }

        public TerminalNode SFIXED64() {
            return getToken(Protobuf2Parser.SFIXED64, 0);
        }

        public TerminalNode BOOL() {
            return getToken(Protobuf2Parser.BOOL, 0);
        }

        public TerminalNode STRING() {
            return getToken(Protobuf2Parser.STRING, 0);
        }

        public TerminalNode BYTES() {
            return getToken(Protobuf2Parser.BYTES, 0);
        }

        public PrimitiveTypeContext(TypeContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterPrimitiveType(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitPrimitiveType(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitPrimitiveType(this);
            else return visitor.visitChildren(this);
        }
    }

    public final TypeContext type() throws RecognitionException {
        TypeContext _localctx = new TypeContext(_ctx, getState());
        enterRule(_localctx, 56, RULE_type);
        int _la;
        try {
            setState(283);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 26, _ctx)) {
                case 1:
                    _localctx = new PrimitiveTypeContext(_localctx);
                    enterOuterAlt(_localctx, 1);
                {
                    setState(281);
                    _la = _input.LA(1);
                    if (!((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << BOOL) | (1L << BYTES) | (1L << DOUBLE) | (1L << FIXED32) | (1L << FIXED64) | (1L << FLOAT) | (1L << INT32) | (1L << INT64) | (1L << SFIXED32) | (1L << SFIXED64) | (1L << SINT32) | (1L << SINT64) | (1L << STRING) | (1L << UINT32) | (1L << UINT64))) != 0))) {
                        _errHandler.recoverInline(this);
                    }
                    else {
                        if (_input.LA(1) == Token.EOF) matchedEOF = true;
                        _errHandler.reportMatch(this);
                        consume();
                    }
                }
                    break;
                case 2:
                    _localctx = new FullyQualifiedTypeContext(_localctx);
                    enterOuterAlt(_localctx, 2);
                {
                    setState(282);
                    fullIdent();
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

    public static class FieldContext extends ParserRuleContext {
        public IdentOrReservedContext fieldName;

        public TypeContext type() {
            return getRuleContext(TypeContext.class, 0);
        }

        public TerminalNode ASSIGN() {
            return getToken(Protobuf2Parser.ASSIGN, 0);
        }

        public TerminalNode IntegerLiteral() {
            return getToken(Protobuf2Parser.IntegerLiteral, 0);
        }

        public TerminalNode SEMI() {
            return getToken(Protobuf2Parser.SEMI, 0);
        }

        public IdentOrReservedContext identOrReserved() {
            return getRuleContext(IdentOrReservedContext.class, 0);
        }

        public OptionListContext optionList() {
            return getRuleContext(OptionListContext.class, 0);
        }

        public FieldContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_field;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterField(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitField(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitField(this);
            else return visitor.visitChildren(this);
        }
    }

    public final FieldContext field() throws RecognitionException {
        FieldContext _localctx = new FieldContext(_ctx, getState());
        enterRule(_localctx, 58, RULE_field);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(285);
                type();
                setState(286);
                ((FieldContext) _localctx).fieldName = identOrReserved();
                setState(287);
                match(ASSIGN);
                setState(288);
                match(IntegerLiteral);
                setState(290);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == LBRACK) {
                    {
                        setState(289);
                        optionList();
                    }
                }

                setState(292);
                match(SEMI);
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

    public static class OneOfContext extends ParserRuleContext {
        public TerminalNode ONEOF() {
            return getToken(Protobuf2Parser.ONEOF, 0);
        }

        public IdentContext ident() {
            return getRuleContext(IdentContext.class, 0);
        }

        public TerminalNode LBRACE() {
            return getToken(Protobuf2Parser.LBRACE, 0);
        }

        public TerminalNode RBRACE() {
            return getToken(Protobuf2Parser.RBRACE, 0);
        }

        public List<FieldContext> field() {
            return getRuleContexts(FieldContext.class);
        }

        public FieldContext field(int i) {
            return getRuleContext(FieldContext.class, i);
        }

        public List<EmptyStatementContext> emptyStatement() {
            return getRuleContexts(EmptyStatementContext.class);
        }

        public EmptyStatementContext emptyStatement(int i) {
            return getRuleContext(EmptyStatementContext.class, i);
        }

        public OneOfContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_oneOf;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterOneOf(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitOneOf(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitOneOf(this);
            else return visitor.visitChildren(this);
        }
    }

    public final OneOfContext oneOf() throws RecognitionException {
        OneOfContext _localctx = new OneOfContext(_ctx, getState());
        enterRule(_localctx, 60, RULE_oneOf);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(294);
                match(ONEOF);
                setState(295);
                ident();
                setState(296);
                match(LBRACE);
                setState(301);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << SEMI) | (1L << BOOL) | (1L << BYTES) | (1L << DOUBLE) | (1L << FIXED32) | (1L << FIXED64) | (1L << FLOAT) | (1L << INT32) | (1L << INT64) | (1L << MESSAGE) | (1L << OPTION) | (1L << PACKAGE) | (1L << RPC) | (1L << SERVICE) | (1L << SFIXED32) | (1L << SFIXED64) | (1L << SINT32) | (1L << SINT64) | (1L << STREAM) | (1L << STRING) | (1L << SYNTAX) | (1L << UINT32) | (1L << UINT64) | (1L << WEAK) | (1L << Ident) | (1L << DOT))) != 0)) {
                    {
                        setState(299);
                        _errHandler.sync(this);
                        switch (_input.LA(1)) {
                            case BOOL:
                            case BYTES:
                            case DOUBLE:
                            case FIXED32:
                            case FIXED64:
                            case FLOAT:
                            case INT32:
                            case INT64:
                            case MESSAGE:
                            case OPTION:
                            case PACKAGE:
                            case RPC:
                            case SERVICE:
                            case SFIXED32:
                            case SFIXED64:
                            case SINT32:
                            case SINT64:
                            case STREAM:
                            case STRING:
                            case SYNTAX:
                            case UINT32:
                            case UINT64:
                            case WEAK:
                            case Ident:
                            case DOT:
                            {
                                setState(297);
                                field();
                            }
                                break;
                            case SEMI:
                            {
                                setState(298);
                                emptyStatement();
                            }
                                break;
                            default:
                                throw new NoViableAltException(this);
                        }
                    }
                    setState(303);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
                setState(304);
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

    public static class MapFieldContext extends ParserRuleContext {
        public TerminalNode MAP() {
            return getToken(Protobuf2Parser.MAP, 0);
        }

        public TerminalNode LCHEVR() {
            return getToken(Protobuf2Parser.LCHEVR, 0);
        }

        public KeyTypeContext keyType() {
            return getRuleContext(KeyTypeContext.class, 0);
        }

        public TerminalNode COMMA() {
            return getToken(Protobuf2Parser.COMMA, 0);
        }

        public TypeContext type() {
            return getRuleContext(TypeContext.class, 0);
        }

        public TerminalNode RCHEVR() {
            return getToken(Protobuf2Parser.RCHEVR, 0);
        }

        public IdentContext ident() {
            return getRuleContext(IdentContext.class, 0);
        }

        public TerminalNode ASSIGN() {
            return getToken(Protobuf2Parser.ASSIGN, 0);
        }

        public TerminalNode IntegerLiteral() {
            return getToken(Protobuf2Parser.IntegerLiteral, 0);
        }

        public TerminalNode SEMI() {
            return getToken(Protobuf2Parser.SEMI, 0);
        }

        public OptionListContext optionList() {
            return getRuleContext(OptionListContext.class, 0);
        }

        public MapFieldContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_mapField;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterMapField(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitMapField(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitMapField(this);
            else return visitor.visitChildren(this);
        }
    }

    public final MapFieldContext mapField() throws RecognitionException {
        MapFieldContext _localctx = new MapFieldContext(_ctx, getState());
        enterRule(_localctx, 62, RULE_mapField);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(306);
                match(MAP);
                setState(307);
                match(LCHEVR);
                setState(308);
                keyType();
                setState(309);
                match(COMMA);
                setState(310);
                type();
                setState(311);
                match(RCHEVR);
                setState(312);
                ident();
                setState(313);
                match(ASSIGN);
                setState(314);
                match(IntegerLiteral);
                setState(316);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == LBRACK) {
                    {
                        setState(315);
                        optionList();
                    }
                }

                setState(318);
                match(SEMI);
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

    public static class KeyTypeContext extends ParserRuleContext {
        public TerminalNode INT32() {
            return getToken(Protobuf2Parser.INT32, 0);
        }

        public TerminalNode INT64() {
            return getToken(Protobuf2Parser.INT64, 0);
        }

        public TerminalNode UINT32() {
            return getToken(Protobuf2Parser.UINT32, 0);
        }

        public TerminalNode UINT64() {
            return getToken(Protobuf2Parser.UINT64, 0);
        }

        public TerminalNode SINT32() {
            return getToken(Protobuf2Parser.SINT32, 0);
        }

        public TerminalNode SINT64() {
            return getToken(Protobuf2Parser.SINT64, 0);
        }

        public TerminalNode FIXED32() {
            return getToken(Protobuf2Parser.FIXED32, 0);
        }

        public TerminalNode FIXED64() {
            return getToken(Protobuf2Parser.FIXED64, 0);
        }

        public TerminalNode SFIXED32() {
            return getToken(Protobuf2Parser.SFIXED32, 0);
        }

        public TerminalNode SFIXED64() {
            return getToken(Protobuf2Parser.SFIXED64, 0);
        }

        public TerminalNode BOOL() {
            return getToken(Protobuf2Parser.BOOL, 0);
        }

        public TerminalNode STRING() {
            return getToken(Protobuf2Parser.STRING, 0);
        }

        public KeyTypeContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_keyType;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterKeyType(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitKeyType(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitKeyType(this);
            else return visitor.visitChildren(this);
        }
    }

    public final KeyTypeContext keyType() throws RecognitionException {
        KeyTypeContext _localctx = new KeyTypeContext(_ctx, getState());
        enterRule(_localctx, 64, RULE_keyType);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(320);
                _la = _input.LA(1);
                if (!((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << BOOL) | (1L << FIXED32) | (1L << FIXED64) | (1L << INT32) | (1L << INT64) | (1L << SFIXED32) | (1L << SFIXED64) | (1L << SINT32) | (1L << SINT64) | (1L << STRING) | (1L << UINT32) | (1L << UINT64))) != 0))) {
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

    public static class ReservedWordContext extends ParserRuleContext {
        public TerminalNode MESSAGE() {
            return getToken(Protobuf2Parser.MESSAGE, 0);
        }

        public TerminalNode OPTION() {
            return getToken(Protobuf2Parser.OPTION, 0);
        }

        public TerminalNode PACKAGE() {
            return getToken(Protobuf2Parser.PACKAGE, 0);
        }

        public TerminalNode SERVICE() {
            return getToken(Protobuf2Parser.SERVICE, 0);
        }

        public TerminalNode STREAM() {
            return getToken(Protobuf2Parser.STREAM, 0);
        }

        public TerminalNode STRING() {
            return getToken(Protobuf2Parser.STRING, 0);
        }

        public TerminalNode SYNTAX() {
            return getToken(Protobuf2Parser.SYNTAX, 0);
        }

        public TerminalNode WEAK() {
            return getToken(Protobuf2Parser.WEAK, 0);
        }

        public TerminalNode RPC() {
            return getToken(Protobuf2Parser.RPC, 0);
        }

        public ReservedWordContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_reservedWord;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterReservedWord(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitReservedWord(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitReservedWord(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ReservedWordContext reservedWord() throws RecognitionException {
        ReservedWordContext _localctx = new ReservedWordContext(_ctx, getState());
        enterRule(_localctx, 66, RULE_reservedWord);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(322);
                _la = _input.LA(1);
                if (!((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << MESSAGE) | (1L << OPTION) | (1L << PACKAGE) | (1L << RPC) | (1L << SERVICE) | (1L << STREAM) | (1L << STRING) | (1L << SYNTAX) | (1L << WEAK))) != 0))) {
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

    public static class FullIdentContext extends ParserRuleContext {
        public List<IdentOrReservedContext> identOrReserved() {
            return getRuleContexts(IdentOrReservedContext.class);
        }

        public IdentOrReservedContext identOrReserved(int i) {
            return getRuleContext(IdentOrReservedContext.class, i);
        }

        public List<TerminalNode> DOT() {
            return getTokens(Protobuf2Parser.DOT);
        }

        public TerminalNode DOT(int i) {
            return getToken(Protobuf2Parser.DOT, i);
        }

        public FullIdentContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_fullIdent;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterFullIdent(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitFullIdent(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitFullIdent(this);
            else return visitor.visitChildren(this);
        }
    }

    public final FullIdentContext fullIdent() throws RecognitionException {
        FullIdentContext _localctx = new FullIdentContext(_ctx, getState());
        enterRule(_localctx, 68, RULE_fullIdent);
        int _la;
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(325);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la == DOT) {
                    {
                        setState(324);
                        match(DOT);
                    }
                }

                setState(332);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 32, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        {
                            {
                                setState(327);
                                identOrReserved();
                                setState(328);
                                match(DOT);
                            }
                        }
                    }
                    setState(334);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 32, _ctx);
                }
                setState(335);
                identOrReserved();
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

    public static class EmptyStatementContext extends ParserRuleContext {
        public TerminalNode SEMI() {
            return getToken(Protobuf2Parser.SEMI, 0);
        }

        public EmptyStatementContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_emptyStatement;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterEmptyStatement(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitEmptyStatement(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitEmptyStatement(this);
            else return visitor.visitChildren(this);
        }
    }

    public final EmptyStatementContext emptyStatement() throws RecognitionException {
        EmptyStatementContext _localctx = new EmptyStatementContext(_ctx, getState());
        enterRule(_localctx, 70, RULE_emptyStatement);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(337);
                match(SEMI);
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

    public static class ConstantContext extends ParserRuleContext {
        public FullIdentContext fullIdent() {
            return getRuleContext(FullIdentContext.class, 0);
        }

        public TerminalNode IntegerLiteral() {
            return getToken(Protobuf2Parser.IntegerLiteral, 0);
        }

        public TerminalNode NumericLiteral() {
            return getToken(Protobuf2Parser.NumericLiteral, 0);
        }

        public TerminalNode StringLiteral() {
            return getToken(Protobuf2Parser.StringLiteral, 0);
        }

        public TerminalNode BooleanLiteral() {
            return getToken(Protobuf2Parser.BooleanLiteral, 0);
        }

        public ConstantContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_constant;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).enterConstant(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof Protobuf2ParserListener) ((Protobuf2ParserListener) listener).exitConstant(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof Protobuf2ParserVisitor) return ((Protobuf2ParserVisitor<? extends T>) visitor).visitConstant(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ConstantContext constant() throws RecognitionException {
        ConstantContext _localctx = new ConstantContext(_ctx, getState());
        enterRule(_localctx, 72, RULE_constant);
        try {
            setState(344);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
                case MESSAGE:
                case OPTION:
                case PACKAGE:
                case RPC:
                case SERVICE:
                case STREAM:
                case STRING:
                case SYNTAX:
                case WEAK:
                case Ident:
                case DOT:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(339);
                    fullIdent();
                }
                    break;
                case IntegerLiteral:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(340);
                    match(IntegerLiteral);
                }
                    break;
                case NumericLiteral:
                    enterOuterAlt(_localctx, 3);
                {
                    setState(341);
                    match(NumericLiteral);
                }
                    break;
                case StringLiteral:
                    enterOuterAlt(_localctx, 4);
                {
                    setState(342);
                    match(StringLiteral);
                }
                    break;
                case BooleanLiteral:
                    enterOuterAlt(_localctx, 5);
                {
                    setState(343);
                    match(BooleanLiteral);
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
            "\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3?\u015d\4\2\t\2\4" +
                    "\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t" +
                    "\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22" +
                    "\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31" +
                    "\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!" +
                    "\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\3\2\3\2\3\2\3\2\3\2\3\2\7\2S\n\2\f" +
                    "\2\16\2V\13\2\3\2\3\2\3\3\3\3\3\4\3\4\5\4^\n\4\3\5\3\5\3\5\3\5\3\5\3\6" +
                    "\3\6\5\6g\n\6\3\6\3\6\3\6\3\7\3\7\3\7\3\7\3\b\3\b\3\b\3\b\3\b\5\bu\n\b" +
                    "\3\b\3\b\7\by\n\b\f\b\16\b|\13\b\3\t\3\t\3\t\3\t\3\n\3\n\3\n\3\n\3\13" +
                    "\3\13\3\13\3\13\7\13\u008a\n\13\f\13\16\13\u008d\13\13\3\13\3\13\3\f\3" +
                    "\f\3\f\3\f\5\f\u0095\n\f\3\r\3\r\3\16\3\16\3\16\3\16\3\17\3\17\3\17\3" +
                    "\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\7\20\u00aa\n\20\f\20" +
                    "\16\20\u00ad\13\20\3\20\3\20\3\21\3\21\3\21\3\21\3\21\7\21\u00b6\n\21" +
                    "\f\21\16\21\u00b9\13\21\3\21\3\21\3\22\3\22\3\22\3\22\3\23\3\23\3\23\3" +
                    "\23\7\23\u00c5\n\23\f\23\16\23\u00c8\13\23\3\23\3\23\3\24\3\24\3\24\5" +
                    "\24\u00cf\n\24\3\24\3\24\5\24\u00d3\n\24\3\24\3\24\3\25\3\25\3\25\3\25" +
                    "\3\26\3\26\3\26\3\26\7\26\u00df\n\26\f\26\16\26\u00e2\13\26\3\26\3\26" +
                    "\3\27\3\27\3\27\3\27\3\27\3\27\3\27\5\27\u00ed\n\27\3\30\3\30\5\30\u00f1" +
                    "\n\30\3\30\3\30\3\30\3\31\3\31\3\31\7\31\u00f9\n\31\f\31\16\31\u00fc\13" +
                    "\31\3\31\3\31\3\32\3\32\3\32\5\32\u0103\n\32\3\32\3\32\3\33\3\33\3\33" +
                    "\7\33\u010a\n\33\f\33\16\33\u010d\13\33\3\34\3\34\3\34\5\34\u0112\n\34" +
                    "\3\35\3\35\3\35\7\35\u0117\n\35\f\35\16\35\u011a\13\35\3\36\3\36\5\36" +
                    "\u011e\n\36\3\37\3\37\3\37\3\37\3\37\5\37\u0125\n\37\3\37\3\37\3 \3 \3" +
                    " \3 \3 \7 \u012e\n \f \16 \u0131\13 \3 \3 \3!\3!\3!\3!\3!\3!\3!\3!\3!" +
                    "\3!\5!\u013f\n!\3!\3!\3\"\3\"\3#\3#\3$\5$\u0148\n$\3$\3$\3$\7$\u014d\n" +
                    "$\f$\16$\u0150\13$\3$\3$\3%\3%\3&\3&\3&\3&\3&\5&\u015b\n&\3&\2\2\'\2\4" +
                    "\6\b\n\f\16\20\22\24\26\30\32\34\36 \"$&(*,.\60\62\64\668:<>@BDFHJ\2\7" +
                    "\4\2\25\25&&\4\2\26\27\'\'\b\2\5\7\n\f\16\17\34\37!!$%\b\2\5\5\n\13\16" +
                    "\17\34\37!!$%\7\2\21\21\23\24\32\33 \"&&\2\u016a\2L\3\2\2\2\4Y\3\2\2\2" +
                    "\6]\3\2\2\2\b_\3\2\2\2\nd\3\2\2\2\fk\3\2\2\2\16t\3\2\2\2\20}\3\2\2\2\22" +
                    "\u0081\3\2\2\2\24\u0085\3\2\2\2\26\u0094\3\2\2\2\30\u0096\3\2\2\2\32\u0098" +
                    "\3\2\2\2\34\u009c\3\2\2\2\36\u009f\3\2\2\2 \u00b0\3\2\2\2\"\u00bc\3\2" +
                    "\2\2$\u00c0\3\2\2\2&\u00cb\3\2\2\2(\u00d6\3\2\2\2*\u00da\3\2\2\2,\u00e5" +
                    "\3\2\2\2.\u00ee\3\2\2\2\60\u00f5\3\2\2\2\62\u00ff\3\2\2\2\64\u0106\3\2" +
                    "\2\2\66\u010e\3\2\2\28\u0113\3\2\2\2:\u011d\3\2\2\2<\u011f\3\2\2\2>\u0128" +
                    "\3\2\2\2@\u0134\3\2\2\2B\u0142\3\2\2\2D\u0144\3\2\2\2F\u0147\3\2\2\2H" +
                    "\u0153\3\2\2\2J\u015a\3\2\2\2LT\5\b\5\2MS\5\n\6\2NS\5\f\7\2OS\5\22\n\2" +
                    "PS\5\26\f\2QS\5H%\2RM\3\2\2\2RN\3\2\2\2RO\3\2\2\2RP\3\2\2\2RQ\3\2\2\2" +
                    "SV\3\2\2\2TR\3\2\2\2TU\3\2\2\2UW\3\2\2\2VT\3\2\2\2WX\7\2\2\3X\3\3\2\2" +
                    "\2YZ\7-\2\2Z\5\3\2\2\2[^\5\30\r\2\\^\5D#\2][\3\2\2\2]\\\3\2\2\2^\7\3\2" +
                    "\2\2_`\7\"\2\2`a\7;\2\2ab\5\4\3\2bc\7\3\2\2c\t\3\2\2\2df\7\r\2\2eg\t\2" +
                    "\2\2fe\3\2\2\2fg\3\2\2\2gh\3\2\2\2hi\5\4\3\2ij\7\3\2\2j\13\3\2\2\2kl\7" +
                    "\24\2\2lm\5F$\2mn\7\3\2\2n\r\3\2\2\2ou\5\30\r\2pq\7/\2\2qr\5F$\2rs\7\60" +
                    "\2\2su\3\2\2\2to\3\2\2\2tp\3\2\2\2uz\3\2\2\2vw\78\2\2wy\5\6\4\2xv\3\2" +
                    "\2\2y|\3\2\2\2zx\3\2\2\2z{\3\2\2\2{\17\3\2\2\2|z\3\2\2\2}~\5\16\b\2~\177" +
                    "\7;\2\2\177\u0080\5J&\2\u0080\21\3\2\2\2\u0081\u0082\7\23\2\2\u0082\u0083" +
                    "\5\20\t\2\u0083\u0084\7\3\2\2\u0084\23\3\2\2\2\u0085\u0086\7\63\2\2\u0086" +
                    "\u008b\5\20\t\2\u0087\u0088\7\67\2\2\u0088\u008a\5\20\t\2\u0089\u0087" +
                    "\3\2\2\2\u008a\u008d\3\2\2\2\u008b\u0089\3\2\2\2\u008b\u008c\3\2\2\2\u008c" +
                    "\u008e\3\2\2\2\u008d\u008b\3\2\2\2\u008e\u008f\7\64\2\2\u008f\25\3\2\2" +
                    "\2\u0090\u0095\5\32\16\2\u0091\u0095\5\"\22\2\u0092\u0095\5(\25\2\u0093" +
                    "\u0095\5 \21\2\u0094\u0090\3\2\2\2\u0094\u0091\3\2\2\2\u0094\u0092\3\2" +
                    "\2\2\u0094\u0093\3\2\2\2\u0095\27\3\2\2\2\u0096\u0097\7(\2\2\u0097\31" +
                    "\3\2\2\2\u0098\u0099\7\21\2\2\u0099\u009a\5\30\r\2\u009a\u009b\5\36\20" +
                    "\2\u009b\33\3\2\2\2\u009c\u009d\t\3\2\2\u009d\u009e\5<\37\2\u009e\35\3" +
                    "\2\2\2\u009f\u00ab\7\61\2\2\u00a0\u00aa\5\34\17\2\u00a1\u00aa\5\"\22\2" +
                    "\u00a2\u00aa\5 \21\2\u00a3\u00aa\5\32\16\2\u00a4\u00aa\5\22\n\2\u00a5" +
                    "\u00aa\5> \2\u00a6\u00aa\5@!\2\u00a7\u00aa\5\62\32\2\u00a8\u00aa\5H%\2" +
                    "\u00a9\u00a0\3\2\2\2\u00a9\u00a1\3\2\2\2\u00a9\u00a2\3\2\2\2\u00a9\u00a3" +
                    "\3\2\2\2\u00a9\u00a4\3\2\2\2\u00a9\u00a5\3\2\2\2\u00a9\u00a6\3\2\2\2\u00a9" +
                    "\u00a7\3\2\2\2\u00a9\u00a8\3\2\2\2\u00aa\u00ad\3\2\2\2\u00ab\u00a9\3\2" +
                    "\2\2\u00ab\u00ac\3\2\2\2\u00ac\u00ae\3\2\2\2\u00ad\u00ab\3\2\2\2\u00ae" +
                    "\u00af\7\62\2\2\u00af\37\3\2\2\2\u00b0\u00b1\7\t\2\2\u00b1\u00b2\5F$\2" +
                    "\u00b2\u00b7\7\61\2\2\u00b3\u00b6\5\34\17\2\u00b4\u00b6\5H%\2\u00b5\u00b3" +
                    "\3\2\2\2\u00b5\u00b4\3\2\2\2\u00b6\u00b9\3\2\2\2\u00b7\u00b5\3\2\2\2\u00b7" +
                    "\u00b8\3\2\2\2\u00b8\u00ba\3\2\2\2\u00b9\u00b7\3\2\2\2\u00ba\u00bb\7\62" +
                    "\2\2\u00bb!\3\2\2\2\u00bc\u00bd\7\b\2\2\u00bd\u00be\5\30\r\2\u00be\u00bf" +
                    "\5$\23\2\u00bf#\3\2\2\2\u00c0\u00c6\7\61\2\2\u00c1\u00c5\5\22\n\2\u00c2" +
                    "\u00c5\5&\24\2\u00c3\u00c5\5H%\2\u00c4\u00c1\3\2\2\2\u00c4\u00c2\3\2\2" +
                    "\2\u00c4\u00c3\3\2\2\2\u00c5\u00c8\3\2\2\2\u00c6\u00c4\3\2\2\2\u00c6\u00c7" +
                    "\3\2\2\2\u00c7\u00c9\3\2\2\2\u00c8\u00c6\3\2\2\2\u00c9\u00ca\7\62\2\2" +
                    "\u00ca%\3\2\2\2\u00cb\u00cc\5\30\r\2\u00cc\u00ce\7;\2\2\u00cd\u00cf\7" +
                    "9\2\2\u00ce\u00cd\3\2\2\2\u00ce\u00cf\3\2\2\2\u00cf\u00d0\3\2\2\2\u00d0" +
                    "\u00d2\7)\2\2\u00d1\u00d3\5\24\13\2\u00d2\u00d1\3\2\2\2\u00d2\u00d3\3" +
                    "\2\2\2\u00d3\u00d4\3\2\2\2\u00d4\u00d5\7\3\2\2\u00d5\'\3\2\2\2\u00d6\u00d7" +
                    "\7\33\2\2\u00d7\u00d8\5\30\r\2\u00d8\u00d9\5*\26\2\u00d9)\3\2\2\2\u00da" +
                    "\u00e0\7\61\2\2\u00db\u00df\5\22\n\2\u00dc\u00df\5,\27\2\u00dd\u00df\5" +
                    "H%\2\u00de\u00db\3\2\2\2\u00de\u00dc\3\2\2\2\u00de\u00dd\3\2\2\2\u00df" +
                    "\u00e2\3\2\2\2\u00e0\u00de\3\2\2\2\u00e0\u00e1\3\2\2\2\u00e1\u00e3\3\2" +
                    "\2\2\u00e2\u00e0\3\2\2\2\u00e3\u00e4\7\62\2\2\u00e4+\3\2\2\2\u00e5\u00e6" +
                    "\7\32\2\2\u00e6\u00e7\5\30\r\2\u00e7\u00e8\5.\30\2\u00e8\u00e9\7\31\2" +
                    "\2\u00e9\u00ec\5.\30\2\u00ea\u00ed\5\60\31\2\u00eb\u00ed\7\3\2\2\u00ec" +
                    "\u00ea\3\2\2\2\u00ec\u00eb\3\2\2\2\u00ed-\3\2\2\2\u00ee\u00f0\7/\2\2\u00ef" +
                    "\u00f1\7 \2\2\u00f0\u00ef\3\2\2\2\u00f0\u00f1\3\2\2\2\u00f1\u00f2\3\2" +
                    "\2\2\u00f2\u00f3\5F$\2\u00f3\u00f4\7\60\2\2\u00f4/\3\2\2\2\u00f5\u00fa" +
                    "\7\61\2\2\u00f6\u00f9\5\22\n\2\u00f7\u00f9\5H%\2\u00f8\u00f6\3\2\2\2\u00f8" +
                    "\u00f7\3\2\2\2\u00f9\u00fc\3\2\2\2\u00fa\u00f8\3\2\2\2\u00fa\u00fb\3\2" +
                    "\2\2\u00fb\u00fd\3\2\2\2\u00fc\u00fa\3\2\2\2\u00fd\u00fe\7\62\2\2\u00fe" +
                    "\61\3\2\2\2\u00ff\u0102\7\30\2\2\u0100\u0103\5\64\33\2\u0101\u0103\58" +
                    "\35\2\u0102\u0100\3\2\2\2\u0102\u0101\3\2\2\2\u0103\u0104\3\2\2\2\u0104" +
                    "\u0105\7\3\2\2\u0105\63\3\2\2\2\u0106\u010b\5\66\34\2\u0107\u0108\7\67" +
                    "\2\2\u0108\u010a\5\66\34\2\u0109\u0107\3\2\2\2\u010a\u010d\3\2\2\2\u010b" +
                    "\u0109\3\2\2\2\u010b\u010c\3\2\2\2\u010c\65\3\2\2\2\u010d\u010b\3\2\2" +
                    "\2\u010e\u0111\7)\2\2\u010f\u0110\7#\2\2\u0110\u0112\7)\2\2\u0111\u010f" +
                    "\3\2\2\2\u0111\u0112\3\2\2\2\u0112\67\3\2\2\2\u0113\u0118\5\4\3\2\u0114" +
                    "\u0115\7\67\2\2\u0115\u0117\5\4\3\2\u0116\u0114\3\2\2\2\u0117\u011a\3" +
                    "\2\2\2\u0118\u0116\3\2\2\2\u0118\u0119\3\2\2\2\u01199\3\2\2\2\u011a\u0118" +
                    "\3\2\2\2\u011b\u011e\t\4\2\2\u011c\u011e\5F$\2\u011d\u011b\3\2\2\2\u011d" +
                    "\u011c\3\2\2\2\u011e;\3\2\2\2\u011f\u0120\5:\36\2\u0120\u0121\5\6\4\2" +
                    "\u0121\u0122\7;\2\2\u0122\u0124\7)\2\2\u0123\u0125\5\24\13\2\u0124\u0123" +
                    "\3\2\2\2\u0124\u0125\3\2\2\2\u0125\u0126\3\2\2\2\u0126\u0127\7\3\2\2\u0127" +
                    "=\3\2\2\2\u0128\u0129\7\22\2\2\u0129\u012a\5\30\r\2\u012a\u012f\7\61\2" +
                    "\2\u012b\u012e\5<\37\2\u012c\u012e\5H%\2\u012d\u012b\3\2\2\2\u012d\u012c" +
                    "\3\2\2\2\u012e\u0131\3\2\2\2\u012f\u012d\3\2\2\2\u012f\u0130\3\2\2\2\u0130" +
                    "\u0132\3\2\2\2\u0131\u012f\3\2\2\2\u0132\u0133\7\62\2\2\u0133?\3\2\2\2" +
                    "\u0134\u0135\7\20\2\2\u0135\u0136\7\65\2\2\u0136\u0137\5B\"\2\u0137\u0138" +
                    "\7\67\2\2\u0138\u0139\5:\36\2\u0139\u013a\7\66\2\2\u013a\u013b\5\30\r" +
                    "\2\u013b\u013c\7;\2\2\u013c\u013e\7)\2\2\u013d\u013f\5\24\13\2\u013e\u013d" +
                    "\3\2\2\2\u013e\u013f\3\2\2\2\u013f\u0140\3\2\2\2\u0140\u0141\7\3\2\2\u0141" +
                    "A\3\2\2\2\u0142\u0143\t\5\2\2\u0143C\3\2\2\2\u0144\u0145\t\6\2\2\u0145" +
                    "E\3\2\2\2\u0146\u0148\78\2\2\u0147\u0146\3\2\2\2\u0147\u0148\3\2\2\2\u0148" +
                    "\u014e\3\2\2\2\u0149\u014a\5\6\4\2\u014a\u014b\78\2\2\u014b\u014d\3\2" +
                    "\2\2\u014c\u0149\3\2\2\2\u014d\u0150\3\2\2\2\u014e\u014c\3\2\2\2\u014e" +
                    "\u014f\3\2\2\2\u014f\u0151\3\2\2\2\u0150\u014e\3\2\2\2\u0151\u0152\5\6" +
                    "\4\2\u0152G\3\2\2\2\u0153\u0154\7\3\2\2\u0154I\3\2\2\2\u0155\u015b\5F" +
                    "$\2\u0156\u015b\7)\2\2\u0157\u015b\7*\2\2\u0158\u015b\7-\2\2\u0159\u015b" +
                    "\7,\2\2\u015a\u0155\3\2\2\2\u015a\u0156\3\2\2\2\u015a\u0157\3\2\2\2\u015a" +
                    "\u0158\3\2\2\2\u015a\u0159\3\2\2\2\u015bK\3\2\2\2$RT]ftz\u008b\u0094\u00a9" +
                    "\u00ab\u00b5\u00b7\u00c4\u00c6\u00ce\u00d2\u00de\u00e0\u00ec\u00f0\u00f8" +
                    "\u00fa\u0102\u010b\u0111\u0118\u011d\u0124\u012d\u012f\u013e\u0147\u014e" +
                    "\u015a";
    public static final ATN _ATN =
            new ATNDeserializer().deserialize(_serializedATN.toCharArray());

    static {
        _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
        for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
            _decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
        }
    }
}