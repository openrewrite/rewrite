/*
 * Copyright 2026 the original author or authors.
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
// Generated from rewrite-protobuf/src/main/antlr/Protobuf2Parser.g4 by ANTLR 4.13.2
package org.openrewrite.protobuf.internal.grammar;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class Protobuf2Parser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		SEMI=1, COLON=2, BOOL=3, BYTES=4, DOUBLE=5, EDITION=6, ENUM=7, EXTEND=8,
		EXTENSIONS=9, FIXED32=10, FIXED64=11, FLOAT=12, GROUP=13, IMPORT=14, INT32=15,
		INT64=16, MAP=17, MAX=18, MESSAGE=19, ONEOF=20, OPTION=21, PACKAGE=22,
		PUBLIC=23, REPEATED=24, REQUIRED=25, RESERVED=26, RETURNS=27, RPC=28,
		SERVICE=29, SFIXED32=30, SFIXED64=31, SINT32=32, SINT64=33, STREAM=34,
		STRING=35, SYNTAX=36, TO=37, UINT32=38, UINT64=39, WEAK=40, OPTIONAL=41,
		Ident=42, IntegerLiteral=43, NumericLiteral=44, FloatLiteral=45, BooleanLiteral=46,
		StringLiteral=47, Quote=48, LPAREN=49, RPAREN=50, LBRACE=51, RBRACE=52,
		LBRACK=53, RBRACK=54, LCHEVR=55, RCHEVR=56, COMMA=57, DOT=58, MINUS=59,
		PLUS=60, ASSIGN=61, WS=62, UTF_8_BOM=63, COMMENT=64, LINE_COMMENT=65;
	public static final int
		RULE_proto = 0, RULE_stringLiteral = 1, RULE_identOrReserved = 2, RULE_syntax = 3,
		RULE_edition = 4, RULE_importStatement = 5, RULE_packageStatement = 6,
		RULE_optionName = 7, RULE_option = 8, RULE_optionDef = 9, RULE_optionList = 10,
		RULE_topLevelDef = 11, RULE_ident = 12, RULE_message = 13, RULE_messageField = 14,
		RULE_group = 15, RULE_messageBody = 16, RULE_extend = 17, RULE_enumDefinition = 18,
		RULE_enumBody = 19, RULE_enumField = 20, RULE_service = 21, RULE_serviceBody = 22,
		RULE_rpc = 23, RULE_rpcInOut = 24, RULE_rpcBody = 25, RULE_reserved = 26,
		RULE_extensions = 27, RULE_ranges = 28, RULE_range = 29, RULE_fieldNames = 30,
		RULE_type = 31, RULE_field = 32, RULE_oneOf = 33, RULE_mapField = 34,
		RULE_keyType = 35, RULE_reservedWord = 36, RULE_fullIdent = 37, RULE_emptyStatement = 38,
		RULE_constant = 39;
	private static String[] makeRuleNames() {
		return new String[] {
			"proto", "stringLiteral", "identOrReserved", "syntax", "edition", "importStatement",
			"packageStatement", "optionName", "option", "optionDef", "optionList",
			"topLevelDef", "ident", "message", "messageField", "group", "messageBody",
			"extend", "enumDefinition", "enumBody", "enumField", "service", "serviceBody",
			"rpc", "rpcInOut", "rpcBody", "reserved", "extensions", "ranges", "range",
			"fieldNames", "type", "field", "oneOf", "mapField", "keyType", "reservedWord",
			"fullIdent", "emptyStatement", "constant"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "';'", "':'", "'bool'", "'bytes'", "'double'", "'edition'", "'enum'",
			"'extend'", "'extensions'", "'fixed32'", "'fixed64'", "'float'", "'group'",
			"'import'", "'int32'", "'int64'", "'map'", "'max'", "'message'", "'oneof'",
			"'option'", "'package'", "'public'", "'repeated'", "'required'", "'reserved'",
			"'returns'", "'rpc'", "'service'", "'sfixed32'", "'sfixed64'", "'sint32'",
			"'sint64'", "'stream'", "'string'", "'syntax'", "'to'", "'uint32'", "'uint64'",
			"'weak'", "'optional'", null, null, null, null, null, null, null, "'('",
			"')'", "'{'", "'}'", "'['", "']'", "'<'", "'>'", "','", "'.'", "'-'",
			"'+'", "'='", null, "'\\uFEFF'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "SEMI", "COLON", "BOOL", "BYTES", "DOUBLE", "EDITION", "ENUM",
			"EXTEND", "EXTENSIONS", "FIXED32", "FIXED64", "FLOAT", "GROUP", "IMPORT",
			"INT32", "INT64", "MAP", "MAX", "MESSAGE", "ONEOF", "OPTION", "PACKAGE",
			"PUBLIC", "REPEATED", "REQUIRED", "RESERVED", "RETURNS", "RPC", "SERVICE",
			"SFIXED32", "SFIXED64", "SINT32", "SINT64", "STREAM", "STRING", "SYNTAX",
			"TO", "UINT32", "UINT64", "WEAK", "OPTIONAL", "Ident", "IntegerLiteral",
			"NumericLiteral", "FloatLiteral", "BooleanLiteral", "StringLiteral",
			"Quote", "LPAREN", "RPAREN", "LBRACE", "RBRACE", "LBRACK", "RBRACK",
			"LCHEVR", "RCHEVR", "COMMA", "DOT", "MINUS", "PLUS", "ASSIGN", "WS",
			"UTF_8_BOM", "COMMENT", "LINE_COMMENT"
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
	public String getGrammarFileName() { return "Protobuf2Parser.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public Protobuf2Parser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ProtoContext extends ParserRuleContext {
		public TerminalNode EOF() { return getToken(Protobuf2Parser.EOF, 0); }
		public SyntaxContext syntax() {
			return getRuleContext(SyntaxContext.class,0);
		}
		public EditionContext edition() {
			return getRuleContext(EditionContext.class,0);
		}
		public List<ImportStatementContext> importStatement() {
			return getRuleContexts(ImportStatementContext.class);
		}
		public ImportStatementContext importStatement(int i) {
			return getRuleContext(ImportStatementContext.class,i);
		}
		public List<PackageStatementContext> packageStatement() {
			return getRuleContexts(PackageStatementContext.class);
		}
		public PackageStatementContext packageStatement(int i) {
			return getRuleContext(PackageStatementContext.class,i);
		}
		public List<OptionDefContext> optionDef() {
			return getRuleContexts(OptionDefContext.class);
		}
		public OptionDefContext optionDef(int i) {
			return getRuleContext(OptionDefContext.class,i);
		}
		public List<TopLevelDefContext> topLevelDef() {
			return getRuleContexts(TopLevelDefContext.class);
		}
		public TopLevelDefContext topLevelDef(int i) {
			return getRuleContext(TopLevelDefContext.class,i);
		}
		public List<EmptyStatementContext> emptyStatement() {
			return getRuleContexts(EmptyStatementContext.class);
		}
		public EmptyStatementContext emptyStatement(int i) {
			return getRuleContext(EmptyStatementContext.class,i);
		}
		public ProtoContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_proto; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterProto(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitProto(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitProto(this);
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
			setState(82);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SYNTAX:
				{
				setState(80);
				syntax();
				}
				break;
			case EDITION:
				{
				setState(81);
				edition();
				}
				break;
			case EOF:
			case SEMI:
			case ENUM:
			case EXTEND:
			case IMPORT:
			case MESSAGE:
			case OPTION:
			case PACKAGE:
			case SERVICE:
				break;
			default:
				break;
			}
			setState(91);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 543703426L) != 0)) {
				{
				setState(89);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case IMPORT:
					{
					setState(84);
					importStatement();
					}
					break;
				case PACKAGE:
					{
					setState(85);
					packageStatement();
					}
					break;
				case OPTION:
					{
					setState(86);
					optionDef();
					}
					break;
				case ENUM:
				case EXTEND:
				case MESSAGE:
				case SERVICE:
					{
					setState(87);
					topLevelDef();
					}
					break;
				case SEMI:
					{
					setState(88);
					emptyStatement();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(93);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(94);
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

	@SuppressWarnings("CheckReturnValue")
	public static class StringLiteralContext extends ParserRuleContext {
		public TerminalNode StringLiteral() { return getToken(Protobuf2Parser.StringLiteral, 0); }
		public StringLiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stringLiteral; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterStringLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitStringLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitStringLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StringLiteralContext stringLiteral() throws RecognitionException {
		StringLiteralContext _localctx = new StringLiteralContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_stringLiteral);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(96);
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

	@SuppressWarnings("CheckReturnValue")
	public static class IdentOrReservedContext extends ParserRuleContext {
		public IdentContext ident() {
			return getRuleContext(IdentContext.class,0);
		}
		public ReservedWordContext reservedWord() {
			return getRuleContext(ReservedWordContext.class,0);
		}
		public IdentOrReservedContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_identOrReserved; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterIdentOrReserved(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitIdentOrReserved(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitIdentOrReserved(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdentOrReservedContext identOrReserved() throws RecognitionException {
		IdentOrReservedContext _localctx = new IdentOrReservedContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_identOrReserved);
		try {
			setState(100);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(98);
				ident();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(99);
				reservedWord();
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

	@SuppressWarnings("CheckReturnValue")
	public static class SyntaxContext extends ParserRuleContext {
		public TerminalNode SYNTAX() { return getToken(Protobuf2Parser.SYNTAX, 0); }
		public TerminalNode ASSIGN() { return getToken(Protobuf2Parser.ASSIGN, 0); }
		public StringLiteralContext stringLiteral() {
			return getRuleContext(StringLiteralContext.class,0);
		}
		public TerminalNode SEMI() { return getToken(Protobuf2Parser.SEMI, 0); }
		public SyntaxContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_syntax; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterSyntax(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitSyntax(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitSyntax(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SyntaxContext syntax() throws RecognitionException {
		SyntaxContext _localctx = new SyntaxContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_syntax);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(102);
			match(SYNTAX);
			setState(103);
			match(ASSIGN);
			setState(104);
			stringLiteral();
			setState(105);
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

	@SuppressWarnings("CheckReturnValue")
	public static class EditionContext extends ParserRuleContext {
		public TerminalNode EDITION() { return getToken(Protobuf2Parser.EDITION, 0); }
		public TerminalNode ASSIGN() { return getToken(Protobuf2Parser.ASSIGN, 0); }
		public StringLiteralContext stringLiteral() {
			return getRuleContext(StringLiteralContext.class,0);
		}
		public TerminalNode SEMI() { return getToken(Protobuf2Parser.SEMI, 0); }
		public EditionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_edition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterEdition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitEdition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitEdition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EditionContext edition() throws RecognitionException {
		EditionContext _localctx = new EditionContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_edition);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(107);
			match(EDITION);
			setState(108);
			match(ASSIGN);
			setState(109);
			stringLiteral();
			setState(110);
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

	@SuppressWarnings("CheckReturnValue")
	public static class ImportStatementContext extends ParserRuleContext {
		public TerminalNode IMPORT() { return getToken(Protobuf2Parser.IMPORT, 0); }
		public StringLiteralContext stringLiteral() {
			return getRuleContext(StringLiteralContext.class,0);
		}
		public TerminalNode SEMI() { return getToken(Protobuf2Parser.SEMI, 0); }
		public TerminalNode WEAK() { return getToken(Protobuf2Parser.WEAK, 0); }
		public TerminalNode PUBLIC() { return getToken(Protobuf2Parser.PUBLIC, 0); }
		public ImportStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_importStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterImportStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitImportStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitImportStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ImportStatementContext importStatement() throws RecognitionException {
		ImportStatementContext _localctx = new ImportStatementContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_importStatement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(112);
			match(IMPORT);
			setState(114);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==PUBLIC || _la==WEAK) {
				{
				setState(113);
				_la = _input.LA(1);
				if ( !(_la==PUBLIC || _la==WEAK) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
			}

			setState(116);
			stringLiteral();
			setState(117);
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

	@SuppressWarnings("CheckReturnValue")
	public static class PackageStatementContext extends ParserRuleContext {
		public TerminalNode PACKAGE() { return getToken(Protobuf2Parser.PACKAGE, 0); }
		public FullIdentContext fullIdent() {
			return getRuleContext(FullIdentContext.class,0);
		}
		public TerminalNode SEMI() { return getToken(Protobuf2Parser.SEMI, 0); }
		public PackageStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_packageStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterPackageStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitPackageStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitPackageStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PackageStatementContext packageStatement() throws RecognitionException {
		PackageStatementContext _localctx = new PackageStatementContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_packageStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(119);
			match(PACKAGE);
			setState(120);
			fullIdent();
			setState(121);
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

	@SuppressWarnings("CheckReturnValue")
	public static class OptionNameContext extends ParserRuleContext {
		public IdentContext ident() {
			return getRuleContext(IdentContext.class,0);
		}
		public TerminalNode LPAREN() { return getToken(Protobuf2Parser.LPAREN, 0); }
		public FullIdentContext fullIdent() {
			return getRuleContext(FullIdentContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(Protobuf2Parser.RPAREN, 0); }
		public List<TerminalNode> DOT() { return getTokens(Protobuf2Parser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(Protobuf2Parser.DOT, i);
		}
		public List<IdentOrReservedContext> identOrReserved() {
			return getRuleContexts(IdentOrReservedContext.class);
		}
		public IdentOrReservedContext identOrReserved(int i) {
			return getRuleContext(IdentOrReservedContext.class,i);
		}
		public OptionNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_optionName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterOptionName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitOptionName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitOptionName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OptionNameContext optionName() throws RecognitionException {
		OptionNameContext _localctx = new OptionNameContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_optionName);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(128);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case BOOL:
			case BYTES:
			case DOUBLE:
			case EDITION:
			case ENUM:
			case EXTEND:
			case EXTENSIONS:
			case FIXED32:
			case FIXED64:
			case FLOAT:
			case GROUP:
			case IMPORT:
			case INT32:
			case INT64:
			case MAP:
			case MAX:
			case MESSAGE:
			case ONEOF:
			case OPTION:
			case PACKAGE:
			case PUBLIC:
			case REPEATED:
			case REQUIRED:
			case RESERVED:
			case RETURNS:
			case RPC:
			case SERVICE:
			case SFIXED32:
			case SFIXED64:
			case SINT32:
			case SINT64:
			case STREAM:
			case STRING:
			case SYNTAX:
			case TO:
			case UINT32:
			case UINT64:
			case WEAK:
			case OPTIONAL:
			case Ident:
				{
				setState(123);
				ident();
				}
				break;
			case LPAREN:
				{
				setState(124);
				match(LPAREN);
				setState(125);
				fullIdent();
				setState(126);
				match(RPAREN);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(134);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DOT) {
				{
				{
				setState(130);
				match(DOT);
				setState(131);
				identOrReserved();
				}
				}
				setState(136);
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

	@SuppressWarnings("CheckReturnValue")
	public static class OptionContext extends ParserRuleContext {
		public OptionNameContext optionName() {
			return getRuleContext(OptionNameContext.class,0);
		}
		public TerminalNode ASSIGN() { return getToken(Protobuf2Parser.ASSIGN, 0); }
		public ConstantContext constant() {
			return getRuleContext(ConstantContext.class,0);
		}
		public OptionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_option; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterOption(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitOption(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitOption(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OptionContext option() throws RecognitionException {
		OptionContext _localctx = new OptionContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_option);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(137);
			optionName();
			setState(138);
			match(ASSIGN);
			setState(139);
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

	@SuppressWarnings("CheckReturnValue")
	public static class OptionDefContext extends ParserRuleContext {
		public TerminalNode OPTION() { return getToken(Protobuf2Parser.OPTION, 0); }
		public OptionContext option() {
			return getRuleContext(OptionContext.class,0);
		}
		public TerminalNode SEMI() { return getToken(Protobuf2Parser.SEMI, 0); }
		public OptionDefContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_optionDef; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterOptionDef(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitOptionDef(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitOptionDef(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OptionDefContext optionDef() throws RecognitionException {
		OptionDefContext _localctx = new OptionDefContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_optionDef);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(141);
			match(OPTION);
			setState(142);
			option();
			setState(143);
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

	@SuppressWarnings("CheckReturnValue")
	public static class OptionListContext extends ParserRuleContext {
		public TerminalNode LBRACK() { return getToken(Protobuf2Parser.LBRACK, 0); }
		public List<OptionContext> option() {
			return getRuleContexts(OptionContext.class);
		}
		public OptionContext option(int i) {
			return getRuleContext(OptionContext.class,i);
		}
		public TerminalNode RBRACK() { return getToken(Protobuf2Parser.RBRACK, 0); }
		public List<TerminalNode> COMMA() { return getTokens(Protobuf2Parser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(Protobuf2Parser.COMMA, i);
		}
		public OptionListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_optionList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterOptionList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitOptionList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitOptionList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OptionListContext optionList() throws RecognitionException {
		OptionListContext _localctx = new OptionListContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_optionList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(145);
			match(LBRACK);
			setState(146);
			option();
			setState(151);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(147);
				match(COMMA);
				setState(148);
				option();
				}
				}
				setState(153);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(154);
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

	@SuppressWarnings("CheckReturnValue")
	public static class TopLevelDefContext extends ParserRuleContext {
		public MessageContext message() {
			return getRuleContext(MessageContext.class,0);
		}
		public EnumDefinitionContext enumDefinition() {
			return getRuleContext(EnumDefinitionContext.class,0);
		}
		public ServiceContext service() {
			return getRuleContext(ServiceContext.class,0);
		}
		public ExtendContext extend() {
			return getRuleContext(ExtendContext.class,0);
		}
		public TopLevelDefContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_topLevelDef; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterTopLevelDef(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitTopLevelDef(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitTopLevelDef(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TopLevelDefContext topLevelDef() throws RecognitionException {
		TopLevelDefContext _localctx = new TopLevelDefContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_topLevelDef);
		try {
			setState(160);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case MESSAGE:
				enterOuterAlt(_localctx, 1);
				{
				setState(156);
				message();
				}
				break;
			case ENUM:
				enterOuterAlt(_localctx, 2);
				{
				setState(157);
				enumDefinition();
				}
				break;
			case SERVICE:
				enterOuterAlt(_localctx, 3);
				{
				setState(158);
				service();
				}
				break;
			case EXTEND:
				enterOuterAlt(_localctx, 4);
				{
				setState(159);
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

	@SuppressWarnings("CheckReturnValue")
	public static class IdentContext extends ParserRuleContext {
		public TerminalNode Ident() { return getToken(Protobuf2Parser.Ident, 0); }
		public ReservedWordContext reservedWord() {
			return getRuleContext(ReservedWordContext.class,0);
		}
		public IdentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ident; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterIdent(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitIdent(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitIdent(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdentContext ident() throws RecognitionException {
		IdentContext _localctx = new IdentContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_ident);
		try {
			setState(164);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Ident:
				enterOuterAlt(_localctx, 1);
				{
				setState(162);
				match(Ident);
				}
				break;
			case BOOL:
			case BYTES:
			case DOUBLE:
			case EDITION:
			case ENUM:
			case EXTEND:
			case EXTENSIONS:
			case FIXED32:
			case FIXED64:
			case FLOAT:
			case GROUP:
			case IMPORT:
			case INT32:
			case INT64:
			case MAP:
			case MAX:
			case MESSAGE:
			case ONEOF:
			case OPTION:
			case PACKAGE:
			case PUBLIC:
			case REPEATED:
			case REQUIRED:
			case RESERVED:
			case RETURNS:
			case RPC:
			case SERVICE:
			case SFIXED32:
			case SFIXED64:
			case SINT32:
			case SINT64:
			case STREAM:
			case STRING:
			case SYNTAX:
			case TO:
			case UINT32:
			case UINT64:
			case WEAK:
			case OPTIONAL:
				enterOuterAlt(_localctx, 2);
				{
				setState(163);
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

	@SuppressWarnings("CheckReturnValue")
	public static class MessageContext extends ParserRuleContext {
		public TerminalNode MESSAGE() { return getToken(Protobuf2Parser.MESSAGE, 0); }
		public IdentContext ident() {
			return getRuleContext(IdentContext.class,0);
		}
		public MessageBodyContext messageBody() {
			return getRuleContext(MessageBodyContext.class,0);
		}
		public MessageContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_message; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterMessage(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitMessage(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitMessage(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MessageContext message() throws RecognitionException {
		MessageContext _localctx = new MessageContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_message);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(166);
			match(MESSAGE);
			setState(167);
			ident();
			setState(168);
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

	@SuppressWarnings("CheckReturnValue")
	public static class MessageFieldContext extends ParserRuleContext {
		public FieldContext field() {
			return getRuleContext(FieldContext.class,0);
		}
		public TerminalNode OPTIONAL() { return getToken(Protobuf2Parser.OPTIONAL, 0); }
		public TerminalNode REQUIRED() { return getToken(Protobuf2Parser.REQUIRED, 0); }
		public TerminalNode REPEATED() { return getToken(Protobuf2Parser.REPEATED, 0); }
		public MessageFieldContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_messageField; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterMessageField(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitMessageField(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitMessageField(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MessageFieldContext messageField() throws RecognitionException {
		MessageFieldContext _localctx = new MessageFieldContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_messageField);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(170);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 2199073587200L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(171);
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

	@SuppressWarnings("CheckReturnValue")
	public static class GroupContext extends ParserRuleContext {
		public TerminalNode GROUP() { return getToken(Protobuf2Parser.GROUP, 0); }
		public IdentContext ident() {
			return getRuleContext(IdentContext.class,0);
		}
		public TerminalNode ASSIGN() { return getToken(Protobuf2Parser.ASSIGN, 0); }
		public TerminalNode IntegerLiteral() { return getToken(Protobuf2Parser.IntegerLiteral, 0); }
		public MessageBodyContext messageBody() {
			return getRuleContext(MessageBodyContext.class,0);
		}
		public TerminalNode OPTIONAL() { return getToken(Protobuf2Parser.OPTIONAL, 0); }
		public TerminalNode REQUIRED() { return getToken(Protobuf2Parser.REQUIRED, 0); }
		public TerminalNode REPEATED() { return getToken(Protobuf2Parser.REPEATED, 0); }
		public GroupContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_group; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterGroup(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitGroup(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitGroup(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GroupContext group() throws RecognitionException {
		GroupContext _localctx = new GroupContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_group);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(173);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 2199073587200L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(174);
			match(GROUP);
			setState(175);
			ident();
			setState(176);
			match(ASSIGN);
			setState(177);
			match(IntegerLiteral);
			setState(178);
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

	@SuppressWarnings("CheckReturnValue")
	public static class MessageBodyContext extends ParserRuleContext {
		public TerminalNode LBRACE() { return getToken(Protobuf2Parser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(Protobuf2Parser.RBRACE, 0); }
		public List<MessageFieldContext> messageField() {
			return getRuleContexts(MessageFieldContext.class);
		}
		public MessageFieldContext messageField(int i) {
			return getRuleContext(MessageFieldContext.class,i);
		}
		public List<GroupContext> group() {
			return getRuleContexts(GroupContext.class);
		}
		public GroupContext group(int i) {
			return getRuleContext(GroupContext.class,i);
		}
		public List<EnumDefinitionContext> enumDefinition() {
			return getRuleContexts(EnumDefinitionContext.class);
		}
		public EnumDefinitionContext enumDefinition(int i) {
			return getRuleContext(EnumDefinitionContext.class,i);
		}
		public List<ExtendContext> extend() {
			return getRuleContexts(ExtendContext.class);
		}
		public ExtendContext extend(int i) {
			return getRuleContext(ExtendContext.class,i);
		}
		public List<MessageContext> message() {
			return getRuleContexts(MessageContext.class);
		}
		public MessageContext message(int i) {
			return getRuleContext(MessageContext.class,i);
		}
		public List<OptionDefContext> optionDef() {
			return getRuleContexts(OptionDefContext.class);
		}
		public OptionDefContext optionDef(int i) {
			return getRuleContext(OptionDefContext.class,i);
		}
		public List<OneOfContext> oneOf() {
			return getRuleContexts(OneOfContext.class);
		}
		public OneOfContext oneOf(int i) {
			return getRuleContext(OneOfContext.class,i);
		}
		public List<MapFieldContext> mapField() {
			return getRuleContexts(MapFieldContext.class);
		}
		public MapFieldContext mapField(int i) {
			return getRuleContext(MapFieldContext.class,i);
		}
		public List<ReservedContext> reserved() {
			return getRuleContexts(ReservedContext.class);
		}
		public ReservedContext reserved(int i) {
			return getRuleContext(ReservedContext.class,i);
		}
		public List<ExtensionsContext> extensions() {
			return getRuleContexts(ExtensionsContext.class);
		}
		public ExtensionsContext extensions(int i) {
			return getRuleContext(ExtensionsContext.class,i);
		}
		public List<EmptyStatementContext> emptyStatement() {
			return getRuleContexts(EmptyStatementContext.class);
		}
		public EmptyStatementContext emptyStatement(int i) {
			return getRuleContext(EmptyStatementContext.class,i);
		}
		public MessageBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_messageBody; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterMessageBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitMessageBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitMessageBody(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MessageBodyContext messageBody() throws RecognitionException {
		MessageBodyContext _localctx = new MessageBodyContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_messageBody);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(180);
			match(LBRACE);
			setState(194);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 2199144498050L) != 0)) {
				{
				setState(192);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,10,_ctx) ) {
				case 1:
					{
					setState(181);
					messageField();
					}
					break;
				case 2:
					{
					setState(182);
					group();
					}
					break;
				case 3:
					{
					setState(183);
					enumDefinition();
					}
					break;
				case 4:
					{
					setState(184);
					extend();
					}
					break;
				case 5:
					{
					setState(185);
					message();
					}
					break;
				case 6:
					{
					setState(186);
					optionDef();
					}
					break;
				case 7:
					{
					setState(187);
					oneOf();
					}
					break;
				case 8:
					{
					setState(188);
					mapField();
					}
					break;
				case 9:
					{
					setState(189);
					reserved();
					}
					break;
				case 10:
					{
					setState(190);
					extensions();
					}
					break;
				case 11:
					{
					setState(191);
					emptyStatement();
					}
					break;
				}
				}
				setState(196);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(197);
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

	@SuppressWarnings("CheckReturnValue")
	public static class ExtendContext extends ParserRuleContext {
		public TerminalNode EXTEND() { return getToken(Protobuf2Parser.EXTEND, 0); }
		public FullIdentContext fullIdent() {
			return getRuleContext(FullIdentContext.class,0);
		}
		public TerminalNode LBRACE() { return getToken(Protobuf2Parser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(Protobuf2Parser.RBRACE, 0); }
		public List<MessageFieldContext> messageField() {
			return getRuleContexts(MessageFieldContext.class);
		}
		public MessageFieldContext messageField(int i) {
			return getRuleContext(MessageFieldContext.class,i);
		}
		public List<EmptyStatementContext> emptyStatement() {
			return getRuleContexts(EmptyStatementContext.class);
		}
		public EmptyStatementContext emptyStatement(int i) {
			return getRuleContext(EmptyStatementContext.class,i);
		}
		public ExtendContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_extend; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterExtend(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitExtend(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitExtend(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExtendContext extend() throws RecognitionException {
		ExtendContext _localctx = new ExtendContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_extend);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(199);
			match(EXTEND);
			setState(200);
			fullIdent();
			setState(201);
			match(LBRACE);
			setState(206);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 2199073587202L) != 0)) {
				{
				setState(204);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case REPEATED:
				case REQUIRED:
				case OPTIONAL:
					{
					setState(202);
					messageField();
					}
					break;
				case SEMI:
					{
					setState(203);
					emptyStatement();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(208);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(209);
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

	@SuppressWarnings("CheckReturnValue")
	public static class EnumDefinitionContext extends ParserRuleContext {
		public TerminalNode ENUM() { return getToken(Protobuf2Parser.ENUM, 0); }
		public IdentContext ident() {
			return getRuleContext(IdentContext.class,0);
		}
		public EnumBodyContext enumBody() {
			return getRuleContext(EnumBodyContext.class,0);
		}
		public EnumDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enumDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterEnumDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitEnumDefinition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitEnumDefinition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EnumDefinitionContext enumDefinition() throws RecognitionException {
		EnumDefinitionContext _localctx = new EnumDefinitionContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_enumDefinition);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(211);
			match(ENUM);
			setState(212);
			ident();
			setState(213);
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

	@SuppressWarnings("CheckReturnValue")
	public static class EnumBodyContext extends ParserRuleContext {
		public TerminalNode LBRACE() { return getToken(Protobuf2Parser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(Protobuf2Parser.RBRACE, 0); }
		public List<OptionDefContext> optionDef() {
			return getRuleContexts(OptionDefContext.class);
		}
		public OptionDefContext optionDef(int i) {
			return getRuleContext(OptionDefContext.class,i);
		}
		public List<EnumFieldContext> enumField() {
			return getRuleContexts(EnumFieldContext.class);
		}
		public EnumFieldContext enumField(int i) {
			return getRuleContext(EnumFieldContext.class,i);
		}
		public List<EmptyStatementContext> emptyStatement() {
			return getRuleContexts(EmptyStatementContext.class);
		}
		public EmptyStatementContext emptyStatement(int i) {
			return getRuleContext(EmptyStatementContext.class,i);
		}
		public EnumBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enumBody; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterEnumBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitEnumBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitEnumBody(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EnumBodyContext enumBody() throws RecognitionException {
		EnumBodyContext _localctx = new EnumBodyContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_enumBody);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(215);
			match(LBRACE);
			setState(221);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 8796093022202L) != 0)) {
				{
				setState(219);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,14,_ctx) ) {
				case 1:
					{
					setState(216);
					optionDef();
					}
					break;
				case 2:
					{
					setState(217);
					enumField();
					}
					break;
				case 3:
					{
					setState(218);
					emptyStatement();
					}
					break;
				}
				}
				setState(223);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(224);
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

	@SuppressWarnings("CheckReturnValue")
	public static class EnumFieldContext extends ParserRuleContext {
		public IdentContext ident() {
			return getRuleContext(IdentContext.class,0);
		}
		public TerminalNode ASSIGN() { return getToken(Protobuf2Parser.ASSIGN, 0); }
		public TerminalNode SEMI() { return getToken(Protobuf2Parser.SEMI, 0); }
		public TerminalNode IntegerLiteral() { return getToken(Protobuf2Parser.IntegerLiteral, 0); }
		public TerminalNode NumericLiteral() { return getToken(Protobuf2Parser.NumericLiteral, 0); }
		public OptionListContext optionList() {
			return getRuleContext(OptionListContext.class,0);
		}
		public TerminalNode MINUS() { return getToken(Protobuf2Parser.MINUS, 0); }
		public EnumFieldContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enumField; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterEnumField(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitEnumField(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitEnumField(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EnumFieldContext enumField() throws RecognitionException {
		EnumFieldContext _localctx = new EnumFieldContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_enumField);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(226);
			ident();
			setState(227);
			match(ASSIGN);
			setState(233);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case IntegerLiteral:
			case MINUS:
				{
				setState(229);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MINUS) {
					{
					setState(228);
					match(MINUS);
					}
				}

				setState(231);
				match(IntegerLiteral);
				}
				break;
			case NumericLiteral:
				{
				setState(232);
				match(NumericLiteral);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(236);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LBRACK) {
				{
				setState(235);
				optionList();
				}
			}

			setState(238);
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

	@SuppressWarnings("CheckReturnValue")
	public static class ServiceContext extends ParserRuleContext {
		public TerminalNode SERVICE() { return getToken(Protobuf2Parser.SERVICE, 0); }
		public IdentContext ident() {
			return getRuleContext(IdentContext.class,0);
		}
		public ServiceBodyContext serviceBody() {
			return getRuleContext(ServiceBodyContext.class,0);
		}
		public ServiceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_service; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterService(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitService(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitService(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ServiceContext service() throws RecognitionException {
		ServiceContext _localctx = new ServiceContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_service);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(240);
			match(SERVICE);
			setState(241);
			ident();
			setState(242);
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

	@SuppressWarnings("CheckReturnValue")
	public static class ServiceBodyContext extends ParserRuleContext {
		public TerminalNode LBRACE() { return getToken(Protobuf2Parser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(Protobuf2Parser.RBRACE, 0); }
		public List<OptionDefContext> optionDef() {
			return getRuleContexts(OptionDefContext.class);
		}
		public OptionDefContext optionDef(int i) {
			return getRuleContext(OptionDefContext.class,i);
		}
		public List<RpcContext> rpc() {
			return getRuleContexts(RpcContext.class);
		}
		public RpcContext rpc(int i) {
			return getRuleContext(RpcContext.class,i);
		}
		public List<EmptyStatementContext> emptyStatement() {
			return getRuleContexts(EmptyStatementContext.class);
		}
		public EmptyStatementContext emptyStatement(int i) {
			return getRuleContext(EmptyStatementContext.class,i);
		}
		public ServiceBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_serviceBody; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterServiceBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitServiceBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitServiceBody(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ServiceBodyContext serviceBody() throws RecognitionException {
		ServiceBodyContext _localctx = new ServiceBodyContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_serviceBody);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(244);
			match(LBRACE);
			setState(250);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 270532610L) != 0)) {
				{
				setState(248);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case OPTION:
					{
					setState(245);
					optionDef();
					}
					break;
				case RPC:
					{
					setState(246);
					rpc();
					}
					break;
				case SEMI:
					{
					setState(247);
					emptyStatement();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(252);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(253);
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

	@SuppressWarnings("CheckReturnValue")
	public static class RpcContext extends ParserRuleContext {
		public TerminalNode RPC() { return getToken(Protobuf2Parser.RPC, 0); }
		public IdentContext ident() {
			return getRuleContext(IdentContext.class,0);
		}
		public List<RpcInOutContext> rpcInOut() {
			return getRuleContexts(RpcInOutContext.class);
		}
		public RpcInOutContext rpcInOut(int i) {
			return getRuleContext(RpcInOutContext.class,i);
		}
		public TerminalNode RETURNS() { return getToken(Protobuf2Parser.RETURNS, 0); }
		public RpcBodyContext rpcBody() {
			return getRuleContext(RpcBodyContext.class,0);
		}
		public TerminalNode SEMI() { return getToken(Protobuf2Parser.SEMI, 0); }
		public RpcContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_rpc; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterRpc(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitRpc(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitRpc(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RpcContext rpc() throws RecognitionException {
		RpcContext _localctx = new RpcContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_rpc);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(255);
			match(RPC);
			setState(256);
			ident();
			setState(257);
			rpcInOut();
			setState(258);
			match(RETURNS);
			setState(259);
			rpcInOut();
			setState(262);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LBRACE:
				{
				setState(260);
				rpcBody();
				}
				break;
			case SEMI:
				{
				setState(261);
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

	@SuppressWarnings("CheckReturnValue")
	public static class RpcInOutContext extends ParserRuleContext {
		public FullIdentContext messageType;
		public TerminalNode LPAREN() { return getToken(Protobuf2Parser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(Protobuf2Parser.RPAREN, 0); }
		public FullIdentContext fullIdent() {
			return getRuleContext(FullIdentContext.class,0);
		}
		public TerminalNode STREAM() { return getToken(Protobuf2Parser.STREAM, 0); }
		public RpcInOutContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_rpcInOut; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterRpcInOut(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitRpcInOut(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitRpcInOut(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RpcInOutContext rpcInOut() throws RecognitionException {
		RpcInOutContext _localctx = new RpcInOutContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_rpcInOut);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(264);
			match(LPAREN);
			setState(266);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,22,_ctx) ) {
			case 1:
				{
				setState(265);
				match(STREAM);
				}
				break;
			}
			setState(268);
			((RpcInOutContext)_localctx).messageType = fullIdent();
			setState(269);
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

	@SuppressWarnings("CheckReturnValue")
	public static class RpcBodyContext extends ParserRuleContext {
		public TerminalNode LBRACE() { return getToken(Protobuf2Parser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(Protobuf2Parser.RBRACE, 0); }
		public List<OptionDefContext> optionDef() {
			return getRuleContexts(OptionDefContext.class);
		}
		public OptionDefContext optionDef(int i) {
			return getRuleContext(OptionDefContext.class,i);
		}
		public List<EmptyStatementContext> emptyStatement() {
			return getRuleContexts(EmptyStatementContext.class);
		}
		public EmptyStatementContext emptyStatement(int i) {
			return getRuleContext(EmptyStatementContext.class,i);
		}
		public RpcBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_rpcBody; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterRpcBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitRpcBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitRpcBody(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RpcBodyContext rpcBody() throws RecognitionException {
		RpcBodyContext _localctx = new RpcBodyContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_rpcBody);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(271);
			match(LBRACE);
			setState(276);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==SEMI || _la==OPTION) {
				{
				setState(274);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case OPTION:
					{
					setState(272);
					optionDef();
					}
					break;
				case SEMI:
					{
					setState(273);
					emptyStatement();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(278);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(279);
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

	@SuppressWarnings("CheckReturnValue")
	public static class ReservedContext extends ParserRuleContext {
		public TerminalNode RESERVED() { return getToken(Protobuf2Parser.RESERVED, 0); }
		public TerminalNode SEMI() { return getToken(Protobuf2Parser.SEMI, 0); }
		public RangesContext ranges() {
			return getRuleContext(RangesContext.class,0);
		}
		public FieldNamesContext fieldNames() {
			return getRuleContext(FieldNamesContext.class,0);
		}
		public ReservedContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_reserved; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterReserved(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitReserved(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitReserved(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ReservedContext reserved() throws RecognitionException {
		ReservedContext _localctx = new ReservedContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_reserved);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(281);
			match(RESERVED);
			setState(284);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case IntegerLiteral:
				{
				setState(282);
				ranges();
				}
				break;
			case StringLiteral:
				{
				setState(283);
				fieldNames();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(286);
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

	@SuppressWarnings("CheckReturnValue")
	public static class ExtensionsContext extends ParserRuleContext {
		public TerminalNode EXTENSIONS() { return getToken(Protobuf2Parser.EXTENSIONS, 0); }
		public RangesContext ranges() {
			return getRuleContext(RangesContext.class,0);
		}
		public TerminalNode SEMI() { return getToken(Protobuf2Parser.SEMI, 0); }
		public ExtensionsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_extensions; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterExtensions(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitExtensions(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitExtensions(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExtensionsContext extensions() throws RecognitionException {
		ExtensionsContext _localctx = new ExtensionsContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_extensions);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(288);
			match(EXTENSIONS);
			setState(289);
			ranges();
			setState(290);
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

	@SuppressWarnings("CheckReturnValue")
	public static class RangesContext extends ParserRuleContext {
		public List<RangeContext> range() {
			return getRuleContexts(RangeContext.class);
		}
		public RangeContext range(int i) {
			return getRuleContext(RangeContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(Protobuf2Parser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(Protobuf2Parser.COMMA, i);
		}
		public RangesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ranges; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterRanges(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitRanges(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitRanges(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RangesContext ranges() throws RecognitionException {
		RangesContext _localctx = new RangesContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_ranges);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(292);
			range();
			setState(297);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(293);
				match(COMMA);
				setState(294);
				range();
				}
				}
				setState(299);
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

	@SuppressWarnings("CheckReturnValue")
	public static class RangeContext extends ParserRuleContext {
		public List<TerminalNode> IntegerLiteral() { return getTokens(Protobuf2Parser.IntegerLiteral); }
		public TerminalNode IntegerLiteral(int i) {
			return getToken(Protobuf2Parser.IntegerLiteral, i);
		}
		public TerminalNode TO() { return getToken(Protobuf2Parser.TO, 0); }
		public TerminalNode MAX() { return getToken(Protobuf2Parser.MAX, 0); }
		public RangeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_range; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterRange(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitRange(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitRange(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RangeContext range() throws RecognitionException {
		RangeContext _localctx = new RangeContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_range);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(300);
			match(IntegerLiteral);
			setState(303);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==TO) {
				{
				setState(301);
				match(TO);
				setState(302);
				_la = _input.LA(1);
				if ( !(_la==MAX || _la==IntegerLiteral) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
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

	@SuppressWarnings("CheckReturnValue")
	public static class FieldNamesContext extends ParserRuleContext {
		public List<StringLiteralContext> stringLiteral() {
			return getRuleContexts(StringLiteralContext.class);
		}
		public StringLiteralContext stringLiteral(int i) {
			return getRuleContext(StringLiteralContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(Protobuf2Parser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(Protobuf2Parser.COMMA, i);
		}
		public FieldNamesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fieldNames; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterFieldNames(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitFieldNames(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitFieldNames(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FieldNamesContext fieldNames() throws RecognitionException {
		FieldNamesContext _localctx = new FieldNamesContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_fieldNames);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(305);
			stringLiteral();
			setState(310);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(306);
				match(COMMA);
				setState(307);
				stringLiteral();
				}
				}
				setState(312);
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

	@SuppressWarnings("CheckReturnValue")
	public static class TypeContext extends ParserRuleContext {
		public TypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type; }

		public TypeContext() { }
		public void copyFrom(TypeContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class FullyQualifiedTypeContext extends TypeContext {
		public FullIdentContext fullIdent() {
			return getRuleContext(FullIdentContext.class,0);
		}
		public FullyQualifiedTypeContext(TypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterFullyQualifiedType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitFullyQualifiedType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitFullyQualifiedType(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class PrimitiveTypeContext extends TypeContext {
		public TerminalNode DOUBLE() { return getToken(Protobuf2Parser.DOUBLE, 0); }
		public TerminalNode FLOAT() { return getToken(Protobuf2Parser.FLOAT, 0); }
		public TerminalNode INT32() { return getToken(Protobuf2Parser.INT32, 0); }
		public TerminalNode INT64() { return getToken(Protobuf2Parser.INT64, 0); }
		public TerminalNode UINT32() { return getToken(Protobuf2Parser.UINT32, 0); }
		public TerminalNode UINT64() { return getToken(Protobuf2Parser.UINT64, 0); }
		public TerminalNode SINT32() { return getToken(Protobuf2Parser.SINT32, 0); }
		public TerminalNode SINT64() { return getToken(Protobuf2Parser.SINT64, 0); }
		public TerminalNode FIXED32() { return getToken(Protobuf2Parser.FIXED32, 0); }
		public TerminalNode FIXED64() { return getToken(Protobuf2Parser.FIXED64, 0); }
		public TerminalNode SFIXED32() { return getToken(Protobuf2Parser.SFIXED32, 0); }
		public TerminalNode SFIXED64() { return getToken(Protobuf2Parser.SFIXED64, 0); }
		public TerminalNode BOOL() { return getToken(Protobuf2Parser.BOOL, 0); }
		public TerminalNode STRING() { return getToken(Protobuf2Parser.STRING, 0); }
		public TerminalNode BYTES() { return getToken(Protobuf2Parser.BYTES, 0); }
		public PrimitiveTypeContext(TypeContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterPrimitiveType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitPrimitiveType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitPrimitiveType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeContext type() throws RecognitionException {
		TypeContext _localctx = new TypeContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_type);
		int _la;
		try {
			setState(315);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,29,_ctx) ) {
			case 1:
				_localctx = new PrimitiveTypeContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(313);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 875099692088L) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 2:
				_localctx = new FullyQualifiedTypeContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(314);
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

	@SuppressWarnings("CheckReturnValue")
	public static class FieldContext extends ParserRuleContext {
		public IdentOrReservedContext fieldName;
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TerminalNode ASSIGN() { return getToken(Protobuf2Parser.ASSIGN, 0); }
		public TerminalNode IntegerLiteral() { return getToken(Protobuf2Parser.IntegerLiteral, 0); }
		public TerminalNode SEMI() { return getToken(Protobuf2Parser.SEMI, 0); }
		public IdentOrReservedContext identOrReserved() {
			return getRuleContext(IdentOrReservedContext.class,0);
		}
		public OptionListContext optionList() {
			return getRuleContext(OptionListContext.class,0);
		}
		public FieldContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_field; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterField(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitField(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitField(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FieldContext field() throws RecognitionException {
		FieldContext _localctx = new FieldContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_field);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(317);
			type();
			setState(318);
			((FieldContext)_localctx).fieldName = identOrReserved();
			setState(319);
			match(ASSIGN);
			setState(320);
			match(IntegerLiteral);
			setState(322);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LBRACK) {
				{
				setState(321);
				optionList();
				}
			}

			setState(324);
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

	@SuppressWarnings("CheckReturnValue")
	public static class OneOfContext extends ParserRuleContext {
		public TerminalNode ONEOF() { return getToken(Protobuf2Parser.ONEOF, 0); }
		public IdentContext ident() {
			return getRuleContext(IdentContext.class,0);
		}
		public TerminalNode LBRACE() { return getToken(Protobuf2Parser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(Protobuf2Parser.RBRACE, 0); }
		public List<FieldContext> field() {
			return getRuleContexts(FieldContext.class);
		}
		public FieldContext field(int i) {
			return getRuleContext(FieldContext.class,i);
		}
		public List<EmptyStatementContext> emptyStatement() {
			return getRuleContexts(EmptyStatementContext.class);
		}
		public EmptyStatementContext emptyStatement(int i) {
			return getRuleContext(EmptyStatementContext.class,i);
		}
		public OneOfContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_oneOf; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterOneOf(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitOneOf(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitOneOf(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OneOfContext oneOf() throws RecognitionException {
		OneOfContext _localctx = new OneOfContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_oneOf);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(326);
			match(ONEOF);
			setState(327);
			ident();
			setState(328);
			match(LBRACE);
			setState(333);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 288239172244733946L) != 0)) {
				{
				setState(331);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case BOOL:
				case BYTES:
				case DOUBLE:
				case EDITION:
				case ENUM:
				case EXTEND:
				case EXTENSIONS:
				case FIXED32:
				case FIXED64:
				case FLOAT:
				case GROUP:
				case IMPORT:
				case INT32:
				case INT64:
				case MAP:
				case MAX:
				case MESSAGE:
				case ONEOF:
				case OPTION:
				case PACKAGE:
				case PUBLIC:
				case REPEATED:
				case REQUIRED:
				case RESERVED:
				case RETURNS:
				case RPC:
				case SERVICE:
				case SFIXED32:
				case SFIXED64:
				case SINT32:
				case SINT64:
				case STREAM:
				case STRING:
				case SYNTAX:
				case TO:
				case UINT32:
				case UINT64:
				case WEAK:
				case OPTIONAL:
				case Ident:
				case DOT:
					{
					setState(329);
					field();
					}
					break;
				case SEMI:
					{
					setState(330);
					emptyStatement();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(335);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(336);
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

	@SuppressWarnings("CheckReturnValue")
	public static class MapFieldContext extends ParserRuleContext {
		public TerminalNode MAP() { return getToken(Protobuf2Parser.MAP, 0); }
		public TerminalNode LCHEVR() { return getToken(Protobuf2Parser.LCHEVR, 0); }
		public KeyTypeContext keyType() {
			return getRuleContext(KeyTypeContext.class,0);
		}
		public TerminalNode COMMA() { return getToken(Protobuf2Parser.COMMA, 0); }
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TerminalNode RCHEVR() { return getToken(Protobuf2Parser.RCHEVR, 0); }
		public IdentContext ident() {
			return getRuleContext(IdentContext.class,0);
		}
		public TerminalNode ASSIGN() { return getToken(Protobuf2Parser.ASSIGN, 0); }
		public TerminalNode IntegerLiteral() { return getToken(Protobuf2Parser.IntegerLiteral, 0); }
		public TerminalNode SEMI() { return getToken(Protobuf2Parser.SEMI, 0); }
		public OptionListContext optionList() {
			return getRuleContext(OptionListContext.class,0);
		}
		public MapFieldContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_mapField; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterMapField(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitMapField(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitMapField(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MapFieldContext mapField() throws RecognitionException {
		MapFieldContext _localctx = new MapFieldContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_mapField);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(338);
			match(MAP);
			setState(339);
			match(LCHEVR);
			setState(340);
			keyType();
			setState(341);
			match(COMMA);
			setState(342);
			type();
			setState(343);
			match(RCHEVR);
			setState(344);
			ident();
			setState(345);
			match(ASSIGN);
			setState(346);
			match(IntegerLiteral);
			setState(348);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LBRACK) {
				{
				setState(347);
				optionList();
				}
			}

			setState(350);
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

	@SuppressWarnings("CheckReturnValue")
	public static class KeyTypeContext extends ParserRuleContext {
		public TerminalNode INT32() { return getToken(Protobuf2Parser.INT32, 0); }
		public TerminalNode INT64() { return getToken(Protobuf2Parser.INT64, 0); }
		public TerminalNode UINT32() { return getToken(Protobuf2Parser.UINT32, 0); }
		public TerminalNode UINT64() { return getToken(Protobuf2Parser.UINT64, 0); }
		public TerminalNode SINT32() { return getToken(Protobuf2Parser.SINT32, 0); }
		public TerminalNode SINT64() { return getToken(Protobuf2Parser.SINT64, 0); }
		public TerminalNode FIXED32() { return getToken(Protobuf2Parser.FIXED32, 0); }
		public TerminalNode FIXED64() { return getToken(Protobuf2Parser.FIXED64, 0); }
		public TerminalNode SFIXED32() { return getToken(Protobuf2Parser.SFIXED32, 0); }
		public TerminalNode SFIXED64() { return getToken(Protobuf2Parser.SFIXED64, 0); }
		public TerminalNode BOOL() { return getToken(Protobuf2Parser.BOOL, 0); }
		public TerminalNode STRING() { return getToken(Protobuf2Parser.STRING, 0); }
		public KeyTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_keyType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterKeyType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitKeyType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitKeyType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final KeyTypeContext keyType() throws RecognitionException {
		KeyTypeContext _localctx = new KeyTypeContext(_ctx, getState());
		enterRule(_localctx, 70, RULE_keyType);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(352);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 875099687944L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
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

	@SuppressWarnings("CheckReturnValue")
	public static class ReservedWordContext extends ParserRuleContext {
		public TerminalNode BOOL() { return getToken(Protobuf2Parser.BOOL, 0); }
		public TerminalNode BYTES() { return getToken(Protobuf2Parser.BYTES, 0); }
		public TerminalNode DOUBLE() { return getToken(Protobuf2Parser.DOUBLE, 0); }
		public TerminalNode EDITION() { return getToken(Protobuf2Parser.EDITION, 0); }
		public TerminalNode ENUM() { return getToken(Protobuf2Parser.ENUM, 0); }
		public TerminalNode EXTEND() { return getToken(Protobuf2Parser.EXTEND, 0); }
		public TerminalNode EXTENSIONS() { return getToken(Protobuf2Parser.EXTENSIONS, 0); }
		public TerminalNode FIXED32() { return getToken(Protobuf2Parser.FIXED32, 0); }
		public TerminalNode FIXED64() { return getToken(Protobuf2Parser.FIXED64, 0); }
		public TerminalNode FLOAT() { return getToken(Protobuf2Parser.FLOAT, 0); }
		public TerminalNode GROUP() { return getToken(Protobuf2Parser.GROUP, 0); }
		public TerminalNode IMPORT() { return getToken(Protobuf2Parser.IMPORT, 0); }
		public TerminalNode INT32() { return getToken(Protobuf2Parser.INT32, 0); }
		public TerminalNode INT64() { return getToken(Protobuf2Parser.INT64, 0); }
		public TerminalNode MAP() { return getToken(Protobuf2Parser.MAP, 0); }
		public TerminalNode MAX() { return getToken(Protobuf2Parser.MAX, 0); }
		public TerminalNode MESSAGE() { return getToken(Protobuf2Parser.MESSAGE, 0); }
		public TerminalNode ONEOF() { return getToken(Protobuf2Parser.ONEOF, 0); }
		public TerminalNode OPTION() { return getToken(Protobuf2Parser.OPTION, 0); }
		public TerminalNode OPTIONAL() { return getToken(Protobuf2Parser.OPTIONAL, 0); }
		public TerminalNode PACKAGE() { return getToken(Protobuf2Parser.PACKAGE, 0); }
		public TerminalNode PUBLIC() { return getToken(Protobuf2Parser.PUBLIC, 0); }
		public TerminalNode REPEATED() { return getToken(Protobuf2Parser.REPEATED, 0); }
		public TerminalNode REQUIRED() { return getToken(Protobuf2Parser.REQUIRED, 0); }
		public TerminalNode RESERVED() { return getToken(Protobuf2Parser.RESERVED, 0); }
		public TerminalNode RETURNS() { return getToken(Protobuf2Parser.RETURNS, 0); }
		public TerminalNode RPC() { return getToken(Protobuf2Parser.RPC, 0); }
		public TerminalNode SERVICE() { return getToken(Protobuf2Parser.SERVICE, 0); }
		public TerminalNode SFIXED32() { return getToken(Protobuf2Parser.SFIXED32, 0); }
		public TerminalNode SFIXED64() { return getToken(Protobuf2Parser.SFIXED64, 0); }
		public TerminalNode SINT32() { return getToken(Protobuf2Parser.SINT32, 0); }
		public TerminalNode SINT64() { return getToken(Protobuf2Parser.SINT64, 0); }
		public TerminalNode STREAM() { return getToken(Protobuf2Parser.STREAM, 0); }
		public TerminalNode STRING() { return getToken(Protobuf2Parser.STRING, 0); }
		public TerminalNode SYNTAX() { return getToken(Protobuf2Parser.SYNTAX, 0); }
		public TerminalNode TO() { return getToken(Protobuf2Parser.TO, 0); }
		public TerminalNode UINT32() { return getToken(Protobuf2Parser.UINT32, 0); }
		public TerminalNode UINT64() { return getToken(Protobuf2Parser.UINT64, 0); }
		public TerminalNode WEAK() { return getToken(Protobuf2Parser.WEAK, 0); }
		public ReservedWordContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_reservedWord; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterReservedWord(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitReservedWord(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitReservedWord(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ReservedWordContext reservedWord() throws RecognitionException {
		ReservedWordContext _localctx = new ReservedWordContext(_ctx, getState());
		enterRule(_localctx, 72, RULE_reservedWord);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(354);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 4398046511096L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
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

	@SuppressWarnings("CheckReturnValue")
	public static class FullIdentContext extends ParserRuleContext {
		public List<IdentOrReservedContext> identOrReserved() {
			return getRuleContexts(IdentOrReservedContext.class);
		}
		public IdentOrReservedContext identOrReserved(int i) {
			return getRuleContext(IdentOrReservedContext.class,i);
		}
		public List<TerminalNode> DOT() { return getTokens(Protobuf2Parser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(Protobuf2Parser.DOT, i);
		}
		public FullIdentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fullIdent; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterFullIdent(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitFullIdent(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitFullIdent(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FullIdentContext fullIdent() throws RecognitionException {
		FullIdentContext _localctx = new FullIdentContext(_ctx, getState());
		enterRule(_localctx, 74, RULE_fullIdent);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(357);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==DOT) {
				{
				setState(356);
				match(DOT);
				}
			}

			setState(364);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,35,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(359);
					identOrReserved();
					setState(360);
					match(DOT);
					}
					}
				}
				setState(366);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,35,_ctx);
			}
			setState(367);
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

	@SuppressWarnings("CheckReturnValue")
	public static class EmptyStatementContext extends ParserRuleContext {
		public TerminalNode SEMI() { return getToken(Protobuf2Parser.SEMI, 0); }
		public EmptyStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_emptyStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterEmptyStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitEmptyStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitEmptyStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EmptyStatementContext emptyStatement() throws RecognitionException {
		EmptyStatementContext _localctx = new EmptyStatementContext(_ctx, getState());
		enterRule(_localctx, 76, RULE_emptyStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(369);
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

	@SuppressWarnings("CheckReturnValue")
	public static class ConstantContext extends ParserRuleContext {
		public FullIdentContext fullIdent() {
			return getRuleContext(FullIdentContext.class,0);
		}
		public TerminalNode IntegerLiteral() { return getToken(Protobuf2Parser.IntegerLiteral, 0); }
		public TerminalNode NumericLiteral() { return getToken(Protobuf2Parser.NumericLiteral, 0); }
		public TerminalNode StringLiteral() { return getToken(Protobuf2Parser.StringLiteral, 0); }
		public TerminalNode BooleanLiteral() { return getToken(Protobuf2Parser.BooleanLiteral, 0); }
		public ConstantContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constant; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).enterConstant(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Protobuf2ParserListener ) ((Protobuf2ParserListener)listener).exitConstant(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Protobuf2ParserVisitor ) return ((Protobuf2ParserVisitor<? extends T>)visitor).visitConstant(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstantContext constant() throws RecognitionException {
		ConstantContext _localctx = new ConstantContext(_ctx, getState());
		enterRule(_localctx, 78, RULE_constant);
		try {
			setState(376);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case BOOL:
			case BYTES:
			case DOUBLE:
			case EDITION:
			case ENUM:
			case EXTEND:
			case EXTENSIONS:
			case FIXED32:
			case FIXED64:
			case FLOAT:
			case GROUP:
			case IMPORT:
			case INT32:
			case INT64:
			case MAP:
			case MAX:
			case MESSAGE:
			case ONEOF:
			case OPTION:
			case PACKAGE:
			case PUBLIC:
			case REPEATED:
			case REQUIRED:
			case RESERVED:
			case RETURNS:
			case RPC:
			case SERVICE:
			case SFIXED32:
			case SFIXED64:
			case SINT32:
			case SINT64:
			case STREAM:
			case STRING:
			case SYNTAX:
			case TO:
			case UINT32:
			case UINT64:
			case WEAK:
			case OPTIONAL:
			case Ident:
			case DOT:
				enterOuterAlt(_localctx, 1);
				{
				setState(371);
				fullIdent();
				}
				break;
			case IntegerLiteral:
				enterOuterAlt(_localctx, 2);
				{
				setState(372);
				match(IntegerLiteral);
				}
				break;
			case NumericLiteral:
				enterOuterAlt(_localctx, 3);
				{
				setState(373);
				match(NumericLiteral);
				}
				break;
			case StringLiteral:
				enterOuterAlt(_localctx, 4);
				{
				setState(374);
				match(StringLiteral);
				}
				break;
			case BooleanLiteral:
				enterOuterAlt(_localctx, 5);
				{
				setState(375);
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
		"\u0004\u0001A\u017b\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002"+
		"\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b\u0002"+
		"\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007\u000f"+
		"\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007\u0012"+
		"\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002\u0015\u0007\u0015"+
		"\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017\u0002\u0018\u0007\u0018"+
		"\u0002\u0019\u0007\u0019\u0002\u001a\u0007\u001a\u0002\u001b\u0007\u001b"+
		"\u0002\u001c\u0007\u001c\u0002\u001d\u0007\u001d\u0002\u001e\u0007\u001e"+
		"\u0002\u001f\u0007\u001f\u0002 \u0007 \u0002!\u0007!\u0002\"\u0007\"\u0002"+
		"#\u0007#\u0002$\u0007$\u0002%\u0007%\u0002&\u0007&\u0002\'\u0007\'\u0001"+
		"\u0000\u0001\u0000\u0003\u0000S\b\u0000\u0001\u0000\u0001\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0005\u0000Z\b\u0000\n\u0000\f\u0000]\t"+
		"\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0002\u0001"+
		"\u0002\u0003\u0002e\b\u0002\u0001\u0003\u0001\u0003\u0001\u0003\u0001"+
		"\u0003\u0001\u0003\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0005\u0001\u0005\u0003\u0005s\b\u0005\u0001\u0005\u0001"+
		"\u0005\u0001\u0005\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0003\u0007\u0081"+
		"\b\u0007\u0001\u0007\u0001\u0007\u0005\u0007\u0085\b\u0007\n\u0007\f\u0007"+
		"\u0088\t\u0007\u0001\b\u0001\b\u0001\b\u0001\b\u0001\t\u0001\t\u0001\t"+
		"\u0001\t\u0001\n\u0001\n\u0001\n\u0001\n\u0005\n\u0096\b\n\n\n\f\n\u0099"+
		"\t\n\u0001\n\u0001\n\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0003"+
		"\u000b\u00a1\b\u000b\u0001\f\u0001\f\u0003\f\u00a5\b\f\u0001\r\u0001\r"+
		"\u0001\r\u0001\r\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000f\u0001"+
		"\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001"+
		"\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001"+
		"\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0005"+
		"\u0010\u00c1\b\u0010\n\u0010\f\u0010\u00c4\t\u0010\u0001\u0010\u0001\u0010"+
		"\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0005\u0011"+
		"\u00cd\b\u0011\n\u0011\f\u0011\u00d0\t\u0011\u0001\u0011\u0001\u0011\u0001"+
		"\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0013\u0001\u0013\u0001"+
		"\u0013\u0001\u0013\u0005\u0013\u00dc\b\u0013\n\u0013\f\u0013\u00df\t\u0013"+
		"\u0001\u0013\u0001\u0013\u0001\u0014\u0001\u0014\u0001\u0014\u0003\u0014"+
		"\u00e6\b\u0014\u0001\u0014\u0001\u0014\u0003\u0014\u00ea\b\u0014\u0001"+
		"\u0014\u0003\u0014\u00ed\b\u0014\u0001\u0014\u0001\u0014\u0001\u0015\u0001"+
		"\u0015\u0001\u0015\u0001\u0015\u0001\u0016\u0001\u0016\u0001\u0016\u0001"+
		"\u0016\u0005\u0016\u00f9\b\u0016\n\u0016\f\u0016\u00fc\t\u0016\u0001\u0016"+
		"\u0001\u0016\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017"+
		"\u0001\u0017\u0001\u0017\u0003\u0017\u0107\b\u0017\u0001\u0018\u0001\u0018"+
		"\u0003\u0018\u010b\b\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0019"+
		"\u0001\u0019\u0001\u0019\u0005\u0019\u0113\b\u0019\n\u0019\f\u0019\u0116"+
		"\t\u0019\u0001\u0019\u0001\u0019\u0001\u001a\u0001\u001a\u0001\u001a\u0003"+
		"\u001a\u011d\b\u001a\u0001\u001a\u0001\u001a\u0001\u001b\u0001\u001b\u0001"+
		"\u001b\u0001\u001b\u0001\u001c\u0001\u001c\u0001\u001c\u0005\u001c\u0128"+
		"\b\u001c\n\u001c\f\u001c\u012b\t\u001c\u0001\u001d\u0001\u001d\u0001\u001d"+
		"\u0003\u001d\u0130\b\u001d\u0001\u001e\u0001\u001e\u0001\u001e\u0005\u001e"+
		"\u0135\b\u001e\n\u001e\f\u001e\u0138\t\u001e\u0001\u001f\u0001\u001f\u0003"+
		"\u001f\u013c\b\u001f\u0001 \u0001 \u0001 \u0001 \u0001 \u0003 \u0143\b"+
		" \u0001 \u0001 \u0001!\u0001!\u0001!\u0001!\u0001!\u0005!\u014c\b!\n!"+
		"\f!\u014f\t!\u0001!\u0001!\u0001\"\u0001\"\u0001\"\u0001\"\u0001\"\u0001"+
		"\"\u0001\"\u0001\"\u0001\"\u0001\"\u0003\"\u015d\b\"\u0001\"\u0001\"\u0001"+
		"#\u0001#\u0001$\u0001$\u0001%\u0003%\u0166\b%\u0001%\u0001%\u0001%\u0005"+
		"%\u016b\b%\n%\f%\u016e\t%\u0001%\u0001%\u0001&\u0001&\u0001\'\u0001\'"+
		"\u0001\'\u0001\'\u0001\'\u0003\'\u0179\b\'\u0001\'\u0000\u0000(\u0000"+
		"\u0002\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u001c"+
		"\u001e \"$&(*,.02468:<>@BDFHJLN\u0000\u0006\u0002\u0000\u0017\u0017(("+
		"\u0002\u0000\u0018\u0019))\u0002\u0000\u0012\u0012++\u0006\u0000\u0003"+
		"\u0005\n\f\u000f\u0010\u001e!##&\'\u0006\u0000\u0003\u0003\n\u000b\u000f"+
		"\u0010\u001e!##&\'\u0001\u0000\u0003)\u018b\u0000R\u0001\u0000\u0000\u0000"+
		"\u0002`\u0001\u0000\u0000\u0000\u0004d\u0001\u0000\u0000\u0000\u0006f"+
		"\u0001\u0000\u0000\u0000\bk\u0001\u0000\u0000\u0000\np\u0001\u0000\u0000"+
		"\u0000\fw\u0001\u0000\u0000\u0000\u000e\u0080\u0001\u0000\u0000\u0000"+
		"\u0010\u0089\u0001\u0000\u0000\u0000\u0012\u008d\u0001\u0000\u0000\u0000"+
		"\u0014\u0091\u0001\u0000\u0000\u0000\u0016\u00a0\u0001\u0000\u0000\u0000"+
		"\u0018\u00a4\u0001\u0000\u0000\u0000\u001a\u00a6\u0001\u0000\u0000\u0000"+
		"\u001c\u00aa\u0001\u0000\u0000\u0000\u001e\u00ad\u0001\u0000\u0000\u0000"+
		" \u00b4\u0001\u0000\u0000\u0000\"\u00c7\u0001\u0000\u0000\u0000$\u00d3"+
		"\u0001\u0000\u0000\u0000&\u00d7\u0001\u0000\u0000\u0000(\u00e2\u0001\u0000"+
		"\u0000\u0000*\u00f0\u0001\u0000\u0000\u0000,\u00f4\u0001\u0000\u0000\u0000"+
		".\u00ff\u0001\u0000\u0000\u00000\u0108\u0001\u0000\u0000\u00002\u010f"+
		"\u0001\u0000\u0000\u00004\u0119\u0001\u0000\u0000\u00006\u0120\u0001\u0000"+
		"\u0000\u00008\u0124\u0001\u0000\u0000\u0000:\u012c\u0001\u0000\u0000\u0000"+
		"<\u0131\u0001\u0000\u0000\u0000>\u013b\u0001\u0000\u0000\u0000@\u013d"+
		"\u0001\u0000\u0000\u0000B\u0146\u0001\u0000\u0000\u0000D\u0152\u0001\u0000"+
		"\u0000\u0000F\u0160\u0001\u0000\u0000\u0000H\u0162\u0001\u0000\u0000\u0000"+
		"J\u0165\u0001\u0000\u0000\u0000L\u0171\u0001\u0000\u0000\u0000N\u0178"+
		"\u0001\u0000\u0000\u0000PS\u0003\u0006\u0003\u0000QS\u0003\b\u0004\u0000"+
		"RP\u0001\u0000\u0000\u0000RQ\u0001\u0000\u0000\u0000RS\u0001\u0000\u0000"+
		"\u0000S[\u0001\u0000\u0000\u0000TZ\u0003\n\u0005\u0000UZ\u0003\f\u0006"+
		"\u0000VZ\u0003\u0012\t\u0000WZ\u0003\u0016\u000b\u0000XZ\u0003L&\u0000"+
		"YT\u0001\u0000\u0000\u0000YU\u0001\u0000\u0000\u0000YV\u0001\u0000\u0000"+
		"\u0000YW\u0001\u0000\u0000\u0000YX\u0001\u0000\u0000\u0000Z]\u0001\u0000"+
		"\u0000\u0000[Y\u0001\u0000\u0000\u0000[\\\u0001\u0000\u0000\u0000\\^\u0001"+
		"\u0000\u0000\u0000][\u0001\u0000\u0000\u0000^_\u0005\u0000\u0000\u0001"+
		"_\u0001\u0001\u0000\u0000\u0000`a\u0005/\u0000\u0000a\u0003\u0001\u0000"+
		"\u0000\u0000be\u0003\u0018\f\u0000ce\u0003H$\u0000db\u0001\u0000\u0000"+
		"\u0000dc\u0001\u0000\u0000\u0000e\u0005\u0001\u0000\u0000\u0000fg\u0005"+
		"$\u0000\u0000gh\u0005=\u0000\u0000hi\u0003\u0002\u0001\u0000ij\u0005\u0001"+
		"\u0000\u0000j\u0007\u0001\u0000\u0000\u0000kl\u0005\u0006\u0000\u0000"+
		"lm\u0005=\u0000\u0000mn\u0003\u0002\u0001\u0000no\u0005\u0001\u0000\u0000"+
		"o\t\u0001\u0000\u0000\u0000pr\u0005\u000e\u0000\u0000qs\u0007\u0000\u0000"+
		"\u0000rq\u0001\u0000\u0000\u0000rs\u0001\u0000\u0000\u0000st\u0001\u0000"+
		"\u0000\u0000tu\u0003\u0002\u0001\u0000uv\u0005\u0001\u0000\u0000v\u000b"+
		"\u0001\u0000\u0000\u0000wx\u0005\u0016\u0000\u0000xy\u0003J%\u0000yz\u0005"+
		"\u0001\u0000\u0000z\r\u0001\u0000\u0000\u0000{\u0081\u0003\u0018\f\u0000"+
		"|}\u00051\u0000\u0000}~\u0003J%\u0000~\u007f\u00052\u0000\u0000\u007f"+
		"\u0081\u0001\u0000\u0000\u0000\u0080{\u0001\u0000\u0000\u0000\u0080|\u0001"+
		"\u0000\u0000\u0000\u0081\u0086\u0001\u0000\u0000\u0000\u0082\u0083\u0005"+
		":\u0000\u0000\u0083\u0085\u0003\u0004\u0002\u0000\u0084\u0082\u0001\u0000"+
		"\u0000\u0000\u0085\u0088\u0001\u0000\u0000\u0000\u0086\u0084\u0001\u0000"+
		"\u0000\u0000\u0086\u0087\u0001\u0000\u0000\u0000\u0087\u000f\u0001\u0000"+
		"\u0000\u0000\u0088\u0086\u0001\u0000\u0000\u0000\u0089\u008a\u0003\u000e"+
		"\u0007\u0000\u008a\u008b\u0005=\u0000\u0000\u008b\u008c\u0003N\'\u0000"+
		"\u008c\u0011\u0001\u0000\u0000\u0000\u008d\u008e\u0005\u0015\u0000\u0000"+
		"\u008e\u008f\u0003\u0010\b\u0000\u008f\u0090\u0005\u0001\u0000\u0000\u0090"+
		"\u0013\u0001\u0000\u0000\u0000\u0091\u0092\u00055\u0000\u0000\u0092\u0097"+
		"\u0003\u0010\b\u0000\u0093\u0094\u00059\u0000\u0000\u0094\u0096\u0003"+
		"\u0010\b\u0000\u0095\u0093\u0001\u0000\u0000\u0000\u0096\u0099\u0001\u0000"+
		"\u0000\u0000\u0097\u0095\u0001\u0000\u0000\u0000\u0097\u0098\u0001\u0000"+
		"\u0000\u0000\u0098\u009a\u0001\u0000\u0000\u0000\u0099\u0097\u0001\u0000"+
		"\u0000\u0000\u009a\u009b\u00056\u0000\u0000\u009b\u0015\u0001\u0000\u0000"+
		"\u0000\u009c\u00a1\u0003\u001a\r\u0000\u009d\u00a1\u0003$\u0012\u0000"+
		"\u009e\u00a1\u0003*\u0015\u0000\u009f\u00a1\u0003\"\u0011\u0000\u00a0"+
		"\u009c\u0001\u0000\u0000\u0000\u00a0\u009d\u0001\u0000\u0000\u0000\u00a0"+
		"\u009e\u0001\u0000\u0000\u0000\u00a0\u009f\u0001\u0000\u0000\u0000\u00a1"+
		"\u0017\u0001\u0000\u0000\u0000\u00a2\u00a5\u0005*\u0000\u0000\u00a3\u00a5"+
		"\u0003H$\u0000\u00a4\u00a2\u0001\u0000\u0000\u0000\u00a4\u00a3\u0001\u0000"+
		"\u0000\u0000\u00a5\u0019\u0001\u0000\u0000\u0000\u00a6\u00a7\u0005\u0013"+
		"\u0000\u0000\u00a7\u00a8\u0003\u0018\f\u0000\u00a8\u00a9\u0003 \u0010"+
		"\u0000\u00a9\u001b\u0001\u0000\u0000\u0000\u00aa\u00ab\u0007\u0001\u0000"+
		"\u0000\u00ab\u00ac\u0003@ \u0000\u00ac\u001d\u0001\u0000\u0000\u0000\u00ad"+
		"\u00ae\u0007\u0001\u0000\u0000\u00ae\u00af\u0005\r\u0000\u0000\u00af\u00b0"+
		"\u0003\u0018\f\u0000\u00b0\u00b1\u0005=\u0000\u0000\u00b1\u00b2\u0005"+
		"+\u0000\u0000\u00b2\u00b3\u0003 \u0010\u0000\u00b3\u001f\u0001\u0000\u0000"+
		"\u0000\u00b4\u00c2\u00053\u0000\u0000\u00b5\u00c1\u0003\u001c\u000e\u0000"+
		"\u00b6\u00c1\u0003\u001e\u000f\u0000\u00b7\u00c1\u0003$\u0012\u0000\u00b8"+
		"\u00c1\u0003\"\u0011\u0000\u00b9\u00c1\u0003\u001a\r\u0000\u00ba\u00c1"+
		"\u0003\u0012\t\u0000\u00bb\u00c1\u0003B!\u0000\u00bc\u00c1\u0003D\"\u0000"+
		"\u00bd\u00c1\u00034\u001a\u0000\u00be\u00c1\u00036\u001b\u0000\u00bf\u00c1"+
		"\u0003L&\u0000\u00c0\u00b5\u0001\u0000\u0000\u0000\u00c0\u00b6\u0001\u0000"+
		"\u0000\u0000\u00c0\u00b7\u0001\u0000\u0000\u0000\u00c0\u00b8\u0001\u0000"+
		"\u0000\u0000\u00c0\u00b9\u0001\u0000\u0000\u0000\u00c0\u00ba\u0001\u0000"+
		"\u0000\u0000\u00c0\u00bb\u0001\u0000\u0000\u0000\u00c0\u00bc\u0001\u0000"+
		"\u0000\u0000\u00c0\u00bd\u0001\u0000\u0000\u0000\u00c0\u00be\u0001\u0000"+
		"\u0000\u0000\u00c0\u00bf\u0001\u0000\u0000\u0000\u00c1\u00c4\u0001\u0000"+
		"\u0000\u0000\u00c2\u00c0\u0001\u0000\u0000\u0000\u00c2\u00c3\u0001\u0000"+
		"\u0000\u0000\u00c3\u00c5\u0001\u0000\u0000\u0000\u00c4\u00c2\u0001\u0000"+
		"\u0000\u0000\u00c5\u00c6\u00054\u0000\u0000\u00c6!\u0001\u0000\u0000\u0000"+
		"\u00c7\u00c8\u0005\b\u0000\u0000\u00c8\u00c9\u0003J%\u0000\u00c9\u00ce"+
		"\u00053\u0000\u0000\u00ca\u00cd\u0003\u001c\u000e\u0000\u00cb\u00cd\u0003"+
		"L&\u0000\u00cc\u00ca\u0001\u0000\u0000\u0000\u00cc\u00cb\u0001\u0000\u0000"+
		"\u0000\u00cd\u00d0\u0001\u0000\u0000\u0000\u00ce\u00cc\u0001\u0000\u0000"+
		"\u0000\u00ce\u00cf\u0001\u0000\u0000\u0000\u00cf\u00d1\u0001\u0000\u0000"+
		"\u0000\u00d0\u00ce\u0001\u0000\u0000\u0000\u00d1\u00d2\u00054\u0000\u0000"+
		"\u00d2#\u0001\u0000\u0000\u0000\u00d3\u00d4\u0005\u0007\u0000\u0000\u00d4"+
		"\u00d5\u0003\u0018\f\u0000\u00d5\u00d6\u0003&\u0013\u0000\u00d6%\u0001"+
		"\u0000\u0000\u0000\u00d7\u00dd\u00053\u0000\u0000\u00d8\u00dc\u0003\u0012"+
		"\t\u0000\u00d9\u00dc\u0003(\u0014\u0000\u00da\u00dc\u0003L&\u0000\u00db"+
		"\u00d8\u0001\u0000\u0000\u0000\u00db\u00d9\u0001\u0000\u0000\u0000\u00db"+
		"\u00da\u0001\u0000\u0000\u0000\u00dc\u00df\u0001\u0000\u0000\u0000\u00dd"+
		"\u00db\u0001\u0000\u0000\u0000\u00dd\u00de\u0001\u0000\u0000\u0000\u00de"+
		"\u00e0\u0001\u0000\u0000\u0000\u00df\u00dd\u0001\u0000\u0000\u0000\u00e0"+
		"\u00e1\u00054\u0000\u0000\u00e1\'\u0001\u0000\u0000\u0000\u00e2\u00e3"+
		"\u0003\u0018\f\u0000\u00e3\u00e9\u0005=\u0000\u0000\u00e4\u00e6\u0005"+
		";\u0000\u0000\u00e5\u00e4\u0001\u0000\u0000\u0000\u00e5\u00e6\u0001\u0000"+
		"\u0000\u0000\u00e6\u00e7\u0001\u0000\u0000\u0000\u00e7\u00ea\u0005+\u0000"+
		"\u0000\u00e8\u00ea\u0005,\u0000\u0000\u00e9\u00e5\u0001\u0000\u0000\u0000"+
		"\u00e9\u00e8\u0001\u0000\u0000\u0000\u00ea\u00ec\u0001\u0000\u0000\u0000"+
		"\u00eb\u00ed\u0003\u0014\n\u0000\u00ec\u00eb\u0001\u0000\u0000\u0000\u00ec"+
		"\u00ed\u0001\u0000\u0000\u0000\u00ed\u00ee\u0001\u0000\u0000\u0000\u00ee"+
		"\u00ef\u0005\u0001\u0000\u0000\u00ef)\u0001\u0000\u0000\u0000\u00f0\u00f1"+
		"\u0005\u001d\u0000\u0000\u00f1\u00f2\u0003\u0018\f\u0000\u00f2\u00f3\u0003"+
		",\u0016\u0000\u00f3+\u0001\u0000\u0000\u0000\u00f4\u00fa\u00053\u0000"+
		"\u0000\u00f5\u00f9\u0003\u0012\t\u0000\u00f6\u00f9\u0003.\u0017\u0000"+
		"\u00f7\u00f9\u0003L&\u0000\u00f8\u00f5\u0001\u0000\u0000\u0000\u00f8\u00f6"+
		"\u0001\u0000\u0000\u0000\u00f8\u00f7\u0001\u0000\u0000\u0000\u00f9\u00fc"+
		"\u0001\u0000\u0000\u0000\u00fa\u00f8\u0001\u0000\u0000\u0000\u00fa\u00fb"+
		"\u0001\u0000\u0000\u0000\u00fb\u00fd\u0001\u0000\u0000\u0000\u00fc\u00fa"+
		"\u0001\u0000\u0000\u0000\u00fd\u00fe\u00054\u0000\u0000\u00fe-\u0001\u0000"+
		"\u0000\u0000\u00ff\u0100\u0005\u001c\u0000\u0000\u0100\u0101\u0003\u0018"+
		"\f\u0000\u0101\u0102\u00030\u0018\u0000\u0102\u0103\u0005\u001b\u0000"+
		"\u0000\u0103\u0106\u00030\u0018\u0000\u0104\u0107\u00032\u0019\u0000\u0105"+
		"\u0107\u0005\u0001\u0000\u0000\u0106\u0104\u0001\u0000\u0000\u0000\u0106"+
		"\u0105\u0001\u0000\u0000\u0000\u0107/\u0001\u0000\u0000\u0000\u0108\u010a"+
		"\u00051\u0000\u0000\u0109\u010b\u0005\"\u0000\u0000\u010a\u0109\u0001"+
		"\u0000\u0000\u0000\u010a\u010b\u0001\u0000\u0000\u0000\u010b\u010c\u0001"+
		"\u0000\u0000\u0000\u010c\u010d\u0003J%\u0000\u010d\u010e\u00052\u0000"+
		"\u0000\u010e1\u0001\u0000\u0000\u0000\u010f\u0114\u00053\u0000\u0000\u0110"+
		"\u0113\u0003\u0012\t\u0000\u0111\u0113\u0003L&\u0000\u0112\u0110\u0001"+
		"\u0000\u0000\u0000\u0112\u0111\u0001\u0000\u0000\u0000\u0113\u0116\u0001"+
		"\u0000\u0000\u0000\u0114\u0112\u0001\u0000\u0000\u0000\u0114\u0115\u0001"+
		"\u0000\u0000\u0000\u0115\u0117\u0001\u0000\u0000\u0000\u0116\u0114\u0001"+
		"\u0000\u0000\u0000\u0117\u0118\u00054\u0000\u0000\u01183\u0001\u0000\u0000"+
		"\u0000\u0119\u011c\u0005\u001a\u0000\u0000\u011a\u011d\u00038\u001c\u0000"+
		"\u011b\u011d\u0003<\u001e\u0000\u011c\u011a\u0001\u0000\u0000\u0000\u011c"+
		"\u011b\u0001\u0000\u0000\u0000\u011d\u011e\u0001\u0000\u0000\u0000\u011e"+
		"\u011f\u0005\u0001\u0000\u0000\u011f5\u0001\u0000\u0000\u0000\u0120\u0121"+
		"\u0005\t\u0000\u0000\u0121\u0122\u00038\u001c\u0000\u0122\u0123\u0005"+
		"\u0001\u0000\u0000\u01237\u0001\u0000\u0000\u0000\u0124\u0129\u0003:\u001d"+
		"\u0000\u0125\u0126\u00059\u0000\u0000\u0126\u0128\u0003:\u001d\u0000\u0127"+
		"\u0125\u0001\u0000\u0000\u0000\u0128\u012b\u0001\u0000\u0000\u0000\u0129"+
		"\u0127\u0001\u0000\u0000\u0000\u0129\u012a\u0001\u0000\u0000\u0000\u012a"+
		"9\u0001\u0000\u0000\u0000\u012b\u0129\u0001\u0000\u0000\u0000\u012c\u012f"+
		"\u0005+\u0000\u0000\u012d\u012e\u0005%\u0000\u0000\u012e\u0130\u0007\u0002"+
		"\u0000\u0000\u012f\u012d\u0001\u0000\u0000\u0000\u012f\u0130\u0001\u0000"+
		"\u0000\u0000\u0130;\u0001\u0000\u0000\u0000\u0131\u0136\u0003\u0002\u0001"+
		"\u0000\u0132\u0133\u00059\u0000\u0000\u0133\u0135\u0003\u0002\u0001\u0000"+
		"\u0134\u0132\u0001\u0000\u0000\u0000\u0135\u0138\u0001\u0000\u0000\u0000"+
		"\u0136\u0134\u0001\u0000\u0000\u0000\u0136\u0137\u0001\u0000\u0000\u0000"+
		"\u0137=\u0001\u0000\u0000\u0000\u0138\u0136\u0001\u0000\u0000\u0000\u0139"+
		"\u013c\u0007\u0003\u0000\u0000\u013a\u013c\u0003J%\u0000\u013b\u0139\u0001"+
		"\u0000\u0000\u0000\u013b\u013a\u0001\u0000\u0000\u0000\u013c?\u0001\u0000"+
		"\u0000\u0000\u013d\u013e\u0003>\u001f\u0000\u013e\u013f\u0003\u0004\u0002"+
		"\u0000\u013f\u0140\u0005=\u0000\u0000\u0140\u0142\u0005+\u0000\u0000\u0141"+
		"\u0143\u0003\u0014\n\u0000\u0142\u0141\u0001\u0000\u0000\u0000\u0142\u0143"+
		"\u0001\u0000\u0000\u0000\u0143\u0144\u0001\u0000\u0000\u0000\u0144\u0145"+
		"\u0005\u0001\u0000\u0000\u0145A\u0001\u0000\u0000\u0000\u0146\u0147\u0005"+
		"\u0014\u0000\u0000\u0147\u0148\u0003\u0018\f\u0000\u0148\u014d\u00053"+
		"\u0000\u0000\u0149\u014c\u0003@ \u0000\u014a\u014c\u0003L&\u0000\u014b"+
		"\u0149\u0001\u0000\u0000\u0000\u014b\u014a\u0001\u0000\u0000\u0000\u014c"+
		"\u014f\u0001\u0000\u0000\u0000\u014d\u014b\u0001\u0000\u0000\u0000\u014d"+
		"\u014e\u0001\u0000\u0000\u0000\u014e\u0150\u0001\u0000\u0000\u0000\u014f"+
		"\u014d\u0001\u0000\u0000\u0000\u0150\u0151\u00054\u0000\u0000\u0151C\u0001"+
		"\u0000\u0000\u0000\u0152\u0153\u0005\u0011\u0000\u0000\u0153\u0154\u0005"+
		"7\u0000\u0000\u0154\u0155\u0003F#\u0000\u0155\u0156\u00059\u0000\u0000"+
		"\u0156\u0157\u0003>\u001f\u0000\u0157\u0158\u00058\u0000\u0000\u0158\u0159"+
		"\u0003\u0018\f\u0000\u0159\u015a\u0005=\u0000\u0000\u015a\u015c\u0005"+
		"+\u0000\u0000\u015b\u015d\u0003\u0014\n\u0000\u015c\u015b\u0001\u0000"+
		"\u0000\u0000\u015c\u015d\u0001\u0000\u0000\u0000\u015d\u015e\u0001\u0000"+
		"\u0000\u0000\u015e\u015f\u0005\u0001\u0000\u0000\u015fE\u0001\u0000\u0000"+
		"\u0000\u0160\u0161\u0007\u0004\u0000\u0000\u0161G\u0001\u0000\u0000\u0000"+
		"\u0162\u0163\u0007\u0005\u0000\u0000\u0163I\u0001\u0000\u0000\u0000\u0164"+
		"\u0166\u0005:\u0000\u0000\u0165\u0164\u0001\u0000\u0000\u0000\u0165\u0166"+
		"\u0001\u0000\u0000\u0000\u0166\u016c\u0001\u0000\u0000\u0000\u0167\u0168"+
		"\u0003\u0004\u0002\u0000\u0168\u0169\u0005:\u0000\u0000\u0169\u016b\u0001"+
		"\u0000\u0000\u0000\u016a\u0167\u0001\u0000\u0000\u0000\u016b\u016e\u0001"+
		"\u0000\u0000\u0000\u016c\u016a\u0001\u0000\u0000\u0000\u016c\u016d\u0001"+
		"\u0000\u0000\u0000\u016d\u016f\u0001\u0000\u0000\u0000\u016e\u016c\u0001"+
		"\u0000\u0000\u0000\u016f\u0170\u0003\u0004\u0002\u0000\u0170K\u0001\u0000"+
		"\u0000\u0000\u0171\u0172\u0005\u0001\u0000\u0000\u0172M\u0001\u0000\u0000"+
		"\u0000\u0173\u0179\u0003J%\u0000\u0174\u0179\u0005+\u0000\u0000\u0175"+
		"\u0179\u0005,\u0000\u0000\u0176\u0179\u0005/\u0000\u0000\u0177\u0179\u0005"+
		".\u0000\u0000\u0178\u0173\u0001\u0000\u0000\u0000\u0178\u0174\u0001\u0000"+
		"\u0000\u0000\u0178\u0175\u0001\u0000\u0000\u0000\u0178\u0176\u0001\u0000"+
		"\u0000\u0000\u0178\u0177\u0001\u0000\u0000\u0000\u0179O\u0001\u0000\u0000"+
		"\u0000%RY[dr\u0080\u0086\u0097\u00a0\u00a4\u00c0\u00c2\u00cc\u00ce\u00db"+
		"\u00dd\u00e5\u00e9\u00ec\u00f8\u00fa\u0106\u010a\u0112\u0114\u011c\u0129"+
		"\u012f\u0136\u013b\u0142\u014b\u014d\u015c\u0165\u016c\u0178";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
