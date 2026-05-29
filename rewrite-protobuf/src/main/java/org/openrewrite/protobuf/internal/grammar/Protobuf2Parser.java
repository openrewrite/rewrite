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
		SEMI=1, COLON=2, BOOL=3, BYTES=4, DOUBLE=5, ENUM=6, EXTEND=7, FIXED32=8,
		FIXED64=9, FLOAT=10, GROUP=11, IMPORT=12, INT32=13, INT64=14, MAP=15,
		MESSAGE=16, ONEOF=17, OPTION=18, PACKAGE=19, PUBLIC=20, REPEATED=21, REQUIRED=22,
		RESERVED=23, RETURNS=24, RPC=25, SERVICE=26, SFIXED32=27, SFIXED64=28,
		SINT32=29, SINT64=30, STREAM=31, STRING=32, SYNTAX=33, TO=34, UINT32=35,
		UINT64=36, WEAK=37, OPTIONAL=38, Ident=39, IntegerLiteral=40, NumericLiteral=41,
		FloatLiteral=42, BooleanLiteral=43, StringLiteral=44, Quote=45, LPAREN=46,
		RPAREN=47, LBRACE=48, RBRACE=49, LBRACK=50, RBRACK=51, LCHEVR=52, RCHEVR=53,
		COMMA=54, DOT=55, MINUS=56, PLUS=57, ASSIGN=58, WS=59, UTF_8_BOM=60, COMMENT=61,
		LINE_COMMENT=62;
	public static final int
		RULE_proto = 0, RULE_stringLiteral = 1, RULE_identOrReserved = 2, RULE_syntax = 3,
		RULE_importStatement = 4, RULE_packageStatement = 5, RULE_optionName = 6,
		RULE_option = 7, RULE_optionDef = 8, RULE_optionList = 9, RULE_topLevelDef = 10,
		RULE_ident = 11, RULE_message = 12, RULE_messageField = 13, RULE_group = 14,
		RULE_messageBody = 15, RULE_extend = 16, RULE_enumDefinition = 17, RULE_enumBody = 18,
		RULE_enumField = 19, RULE_service = 20, RULE_serviceBody = 21, RULE_rpc = 22,
		RULE_rpcInOut = 23, RULE_rpcBody = 24, RULE_reserved = 25, RULE_ranges = 26,
		RULE_range = 27, RULE_fieldNames = 28, RULE_type = 29, RULE_field = 30,
		RULE_oneOf = 31, RULE_mapField = 32, RULE_keyType = 33, RULE_reservedWord = 34,
		RULE_fullIdent = 35, RULE_emptyStatement = 36, RULE_constant = 37;
	private static String[] makeRuleNames() {
		return new String[] {
			"proto", "stringLiteral", "identOrReserved", "syntax", "importStatement",
			"packageStatement", "optionName", "option", "optionDef", "optionList",
			"topLevelDef", "ident", "message", "messageField", "group", "messageBody",
			"extend", "enumDefinition", "enumBody", "enumField", "service", "serviceBody",
			"rpc", "rpcInOut", "rpcBody", "reserved", "ranges", "range", "fieldNames",
			"type", "field", "oneOf", "mapField", "keyType", "reservedWord", "fullIdent",
			"emptyStatement", "constant"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "';'", "':'", "'bool'", "'bytes'", "'double'", "'enum'", "'extend'",
			"'fixed32'", "'fixed64'", "'float'", "'group'", "'import'", "'int32'",
			"'int64'", "'map'", "'message'", "'oneof'", "'option'", "'package'",
			"'public'", "'repeated'", "'required'", "'reserved'", "'returns'", "'rpc'",
			"'service'", "'sfixed32'", "'sfixed64'", "'sint32'", "'sint64'", "'stream'",
			"'string'", "'syntax'", "'to'", "'uint32'", "'uint64'", "'weak'", "'optional'",
			null, null, null, null, null, null, null, "'('", "')'", "'{'", "'}'",
			"'['", "']'", "'<'", "'>'", "','", "'.'", "'-'", "'+'", "'='", null,
			"'\\uFEFF'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "SEMI", "COLON", "BOOL", "BYTES", "DOUBLE", "ENUM", "EXTEND", "FIXED32",
			"FIXED64", "FLOAT", "GROUP", "IMPORT", "INT32", "INT64", "MAP", "MESSAGE",
			"ONEOF", "OPTION", "PACKAGE", "PUBLIC", "REPEATED", "REQUIRED", "RESERVED",
			"RETURNS", "RPC", "SERVICE", "SFIXED32", "SFIXED64", "SINT32", "SINT64",
			"STREAM", "STRING", "SYNTAX", "TO", "UINT32", "UINT64", "WEAK", "OPTIONAL",
			"Ident", "IntegerLiteral", "NumericLiteral", "FloatLiteral", "BooleanLiteral",
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
			setState(77);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SYNTAX) {
				{
				setState(76);
				syntax();
				}
			}

			setState(86);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 67965122L) != 0)) {
				{
				setState(84);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case IMPORT:
					{
					setState(79);
					importStatement();
					}
					break;
				case PACKAGE:
					{
					setState(80);
					packageStatement();
					}
					break;
				case OPTION:
					{
					setState(81);
					optionDef();
					}
					break;
				case ENUM:
				case EXTEND:
				case MESSAGE:
				case SERVICE:
					{
					setState(82);
					topLevelDef();
					}
					break;
				case SEMI:
					{
					setState(83);
					emptyStatement();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(88);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(89);
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
			setState(91);
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
			setState(95);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Ident:
				enterOuterAlt(_localctx, 1);
				{
				setState(93);
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
				setState(94);
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
			setState(97);
			match(SYNTAX);
			setState(98);
			match(ASSIGN);
			setState(99);
			stringLiteral();
			setState(100);
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
		enterRule(_localctx, 8, RULE_importStatement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(102);
			match(IMPORT);
			setState(104);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==PUBLIC || _la==WEAK) {
				{
				setState(103);
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

			setState(106);
			stringLiteral();
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
		enterRule(_localctx, 10, RULE_packageStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(109);
			match(PACKAGE);
			setState(110);
			fullIdent();
			setState(111);
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
		enterRule(_localctx, 12, RULE_optionName);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(118);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case Ident:
				{
				setState(113);
				ident();
				}
				break;
			case LPAREN:
				{
				setState(114);
				match(LPAREN);
				setState(115);
				fullIdent();
				setState(116);
				match(RPAREN);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(124);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DOT) {
				{
				{
				setState(120);
				match(DOT);
				setState(121);
				identOrReserved();
				}
				}
				setState(126);
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
		enterRule(_localctx, 14, RULE_option);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(127);
			optionName();
			setState(128);
			match(ASSIGN);
			setState(129);
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
		enterRule(_localctx, 16, RULE_optionDef);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(131);
			match(OPTION);
			setState(132);
			option();
			setState(133);
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
		enterRule(_localctx, 18, RULE_optionList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(135);
			match(LBRACK);
			setState(136);
			option();
			setState(141);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(137);
				match(COMMA);
				setState(138);
				option();
				}
				}
				setState(143);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(144);
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
		enterRule(_localctx, 20, RULE_topLevelDef);
		try {
			setState(150);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case MESSAGE:
				enterOuterAlt(_localctx, 1);
				{
				setState(146);
				message();
				}
				break;
			case ENUM:
				enterOuterAlt(_localctx, 2);
				{
				setState(147);
				enumDefinition();
				}
				break;
			case SERVICE:
				enterOuterAlt(_localctx, 3);
				{
				setState(148);
				service();
				}
				break;
			case EXTEND:
				enterOuterAlt(_localctx, 4);
				{
				setState(149);
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
		enterRule(_localctx, 22, RULE_ident);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(152);
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
		enterRule(_localctx, 24, RULE_message);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(154);
			match(MESSAGE);
			setState(155);
			ident();
			setState(156);
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
		enterRule(_localctx, 26, RULE_messageField);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(158);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 274884198400L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(159);
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
		enterRule(_localctx, 28, RULE_group);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(161);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 274884198400L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(162);
			match(GROUP);
			setState(163);
			ident();
			setState(164);
			match(ASSIGN);
			setState(165);
			match(IntegerLiteral);
			setState(166);
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
		enterRule(_localctx, 30, RULE_messageBody);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(168);
			match(LBRACE);
			setState(181);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 274893078722L) != 0)) {
				{
				setState(179);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,9,_ctx) ) {
				case 1:
					{
					setState(169);
					messageField();
					}
					break;
				case 2:
					{
					setState(170);
					group();
					}
					break;
				case 3:
					{
					setState(171);
					enumDefinition();
					}
					break;
				case 4:
					{
					setState(172);
					extend();
					}
					break;
				case 5:
					{
					setState(173);
					message();
					}
					break;
				case 6:
					{
					setState(174);
					optionDef();
					}
					break;
				case 7:
					{
					setState(175);
					oneOf();
					}
					break;
				case 8:
					{
					setState(176);
					mapField();
					}
					break;
				case 9:
					{
					setState(177);
					reserved();
					}
					break;
				case 10:
					{
					setState(178);
					emptyStatement();
					}
					break;
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
		enterRule(_localctx, 32, RULE_extend);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(186);
			match(EXTEND);
			setState(187);
			fullIdent();
			setState(188);
			match(LBRACE);
			setState(193);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 274884198402L) != 0)) {
				{
				setState(191);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case REPEATED:
				case REQUIRED:
				case OPTIONAL:
					{
					setState(189);
					messageField();
					}
					break;
				case SEMI:
					{
					setState(190);
					emptyStatement();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(195);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(196);
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
		enterRule(_localctx, 34, RULE_enumDefinition);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(198);
			match(ENUM);
			setState(199);
			ident();
			setState(200);
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
		enterRule(_localctx, 36, RULE_enumBody);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(202);
			match(LBRACE);
			setState(208);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 549756076034L) != 0)) {
				{
				setState(206);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case OPTION:
					{
					setState(203);
					optionDef();
					}
					break;
				case Ident:
					{
					setState(204);
					enumField();
					}
					break;
				case SEMI:
					{
					setState(205);
					emptyStatement();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(210);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(211);
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
		public TerminalNode IntegerLiteral() { return getToken(Protobuf2Parser.IntegerLiteral, 0); }
		public TerminalNode SEMI() { return getToken(Protobuf2Parser.SEMI, 0); }
		public TerminalNode MINUS() { return getToken(Protobuf2Parser.MINUS, 0); }
		public OptionListContext optionList() {
			return getRuleContext(OptionListContext.class,0);
		}
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
		enterRule(_localctx, 38, RULE_enumField);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(213);
			ident();
			setState(214);
			match(ASSIGN);
			setState(216);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==MINUS) {
				{
				setState(215);
				match(MINUS);
				}
			}

			setState(218);
			match(IntegerLiteral);
			setState(220);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LBRACK) {
				{
				setState(219);
				optionList();
				}
			}

			setState(222);
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
		enterRule(_localctx, 40, RULE_service);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(224);
			match(SERVICE);
			setState(225);
			ident();
			setState(226);
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
		enterRule(_localctx, 42, RULE_serviceBody);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(228);
			match(LBRACE);
			setState(234);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 33816578L) != 0)) {
				{
				setState(232);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case OPTION:
					{
					setState(229);
					optionDef();
					}
					break;
				case RPC:
					{
					setState(230);
					rpc();
					}
					break;
				case SEMI:
					{
					setState(231);
					emptyStatement();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(236);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(237);
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
		enterRule(_localctx, 44, RULE_rpc);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(239);
			match(RPC);
			setState(240);
			ident();
			setState(241);
			rpcInOut();
			setState(242);
			match(RETURNS);
			setState(243);
			rpcInOut();
			setState(246);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LBRACE:
				{
				setState(244);
				rpcBody();
				}
				break;
			case SEMI:
				{
				setState(245);
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
		enterRule(_localctx, 46, RULE_rpcInOut);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(248);
			match(LPAREN);
			setState(250);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,20,_ctx) ) {
			case 1:
				{
				setState(249);
				match(STREAM);
				}
				break;
			}
			setState(252);
			((RpcInOutContext)_localctx).messageType = fullIdent();
			setState(253);
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
		enterRule(_localctx, 48, RULE_rpcBody);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(255);
			match(LBRACE);
			setState(260);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==SEMI || _la==OPTION) {
				{
				setState(258);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case OPTION:
					{
					setState(256);
					optionDef();
					}
					break;
				case SEMI:
					{
					setState(257);
					emptyStatement();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(262);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(263);
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
		enterRule(_localctx, 50, RULE_reserved);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(265);
			match(RESERVED);
			setState(268);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case IntegerLiteral:
				{
				setState(266);
				ranges();
				}
				break;
			case StringLiteral:
				{
				setState(267);
				fieldNames();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(270);
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
		enterRule(_localctx, 52, RULE_ranges);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(272);
			range();
			setState(277);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(273);
				match(COMMA);
				setState(274);
				range();
				}
				}
				setState(279);
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
		enterRule(_localctx, 54, RULE_range);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(280);
			match(IntegerLiteral);
			setState(283);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==TO) {
				{
				setState(281);
				match(TO);
				setState(282);
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
		enterRule(_localctx, 56, RULE_fieldNames);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(285);
			stringLiteral();
			setState(290);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(286);
				match(COMMA);
				setState(287);
				stringLiteral();
				}
				}
				setState(292);
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
		enterRule(_localctx, 58, RULE_type);
		int _la;
		try {
			setState(295);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,27,_ctx) ) {
			case 1:
				_localctx = new PrimitiveTypeContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(293);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 109387474744L) != 0)) ) {
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
				setState(294);
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
		enterRule(_localctx, 60, RULE_field);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(297);
			type();
			setState(298);
			((FieldContext)_localctx).fieldName = identOrReserved();
			setState(299);
			match(ASSIGN);
			setState(300);
			match(IntegerLiteral);
			setState(302);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LBRACK) {
				{
				setState(301);
				optionList();
				}
			}

			setState(304);
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
		enterRule(_localctx, 62, RULE_oneOf);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(306);
			match(ONEOF);
			setState(307);
			ident();
			setState(308);
			match(LBRACE);
			setState(313);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 36029604440139578L) != 0)) {
				{
				setState(311);
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
					setState(309);
					field();
					}
					break;
				case SEMI:
					{
					setState(310);
					emptyStatement();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(315);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(316);
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
		enterRule(_localctx, 64, RULE_mapField);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(318);
			match(MAP);
			setState(319);
			match(LCHEVR);
			setState(320);
			keyType();
			setState(321);
			match(COMMA);
			setState(322);
			type();
			setState(323);
			match(RCHEVR);
			setState(324);
			ident();
			setState(325);
			match(ASSIGN);
			setState(326);
			match(IntegerLiteral);
			setState(328);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LBRACK) {
				{
				setState(327);
				optionList();
				}
			}

			setState(330);
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
		enterRule(_localctx, 66, RULE_keyType);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(332);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 109387473672L) != 0)) ) {
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
		public TerminalNode MESSAGE() { return getToken(Protobuf2Parser.MESSAGE, 0); }
		public TerminalNode OPTION() { return getToken(Protobuf2Parser.OPTION, 0); }
		public TerminalNode PACKAGE() { return getToken(Protobuf2Parser.PACKAGE, 0); }
		public TerminalNode SERVICE() { return getToken(Protobuf2Parser.SERVICE, 0); }
		public TerminalNode STREAM() { return getToken(Protobuf2Parser.STREAM, 0); }
		public TerminalNode STRING() { return getToken(Protobuf2Parser.STRING, 0); }
		public TerminalNode SYNTAX() { return getToken(Protobuf2Parser.SYNTAX, 0); }
		public TerminalNode WEAK() { return getToken(Protobuf2Parser.WEAK, 0); }
		public TerminalNode RPC() { return getToken(Protobuf2Parser.RPC, 0); }
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
		enterRule(_localctx, 68, RULE_reservedWord);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(334);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 152572854272L) != 0)) ) {
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
		enterRule(_localctx, 70, RULE_fullIdent);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(337);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==DOT) {
				{
				setState(336);
				match(DOT);
				}
			}

			setState(344);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,33,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(339);
					identOrReserved();
					setState(340);
					match(DOT);
					}
					}
				}
				setState(346);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,33,_ctx);
			}
			setState(347);
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
		enterRule(_localctx, 72, RULE_emptyStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(349);
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
		enterRule(_localctx, 74, RULE_constant);
		try {
			setState(356);
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
				setState(351);
				fullIdent();
				}
				break;
			case IntegerLiteral:
				enterOuterAlt(_localctx, 2);
				{
				setState(352);
				match(IntegerLiteral);
				}
				break;
			case NumericLiteral:
				enterOuterAlt(_localctx, 3);
				{
				setState(353);
				match(NumericLiteral);
				}
				break;
			case StringLiteral:
				enterOuterAlt(_localctx, 4);
				{
				setState(354);
				match(StringLiteral);
				}
				break;
			case BooleanLiteral:
				enterOuterAlt(_localctx, 5);
				{
				setState(355);
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
		"\u0004\u0001>\u0167\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
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
		"#\u0007#\u0002$\u0007$\u0002%\u0007%\u0001\u0000\u0003\u0000N\b\u0000"+
		"\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0005\u0000"+
		"U\b\u0000\n\u0000\f\u0000X\t\u0000\u0001\u0000\u0001\u0000\u0001\u0001"+
		"\u0001\u0001\u0001\u0002\u0001\u0002\u0003\u0002`\b\u0002\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0004\u0001\u0004"+
		"\u0003\u0004i\b\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0005"+
		"\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0006\u0001\u0006\u0001\u0006"+
		"\u0001\u0006\u0001\u0006\u0003\u0006w\b\u0006\u0001\u0006\u0001\u0006"+
		"\u0005\u0006{\b\u0006\n\u0006\f\u0006~\t\u0006\u0001\u0007\u0001\u0007"+
		"\u0001\u0007\u0001\u0007\u0001\b\u0001\b\u0001\b\u0001\b\u0001\t\u0001"+
		"\t\u0001\t\u0001\t\u0005\t\u008c\b\t\n\t\f\t\u008f\t\t\u0001\t\u0001\t"+
		"\u0001\n\u0001\n\u0001\n\u0001\n\u0003\n\u0097\b\n\u0001\u000b\u0001\u000b"+
		"\u0001\f\u0001\f\u0001\f\u0001\f\u0001\r\u0001\r\u0001\r\u0001\u000e\u0001"+
		"\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001"+
		"\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001"+
		"\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0005\u000f\u00b4"+
		"\b\u000f\n\u000f\f\u000f\u00b7\t\u000f\u0001\u000f\u0001\u000f\u0001\u0010"+
		"\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0005\u0010\u00c0\b\u0010"+
		"\n\u0010\f\u0010\u00c3\t\u0010\u0001\u0010\u0001\u0010\u0001\u0011\u0001"+
		"\u0011\u0001\u0011\u0001\u0011\u0001\u0012\u0001\u0012\u0001\u0012\u0001"+
		"\u0012\u0005\u0012\u00cf\b\u0012\n\u0012\f\u0012\u00d2\t\u0012\u0001\u0012"+
		"\u0001\u0012\u0001\u0013\u0001\u0013\u0001\u0013\u0003\u0013\u00d9\b\u0013"+
		"\u0001\u0013\u0001\u0013\u0003\u0013\u00dd\b\u0013\u0001\u0013\u0001\u0013"+
		"\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0015\u0001\u0015"+
		"\u0001\u0015\u0001\u0015\u0005\u0015\u00e9\b\u0015\n\u0015\f\u0015\u00ec"+
		"\t\u0015\u0001\u0015\u0001\u0015\u0001\u0016\u0001\u0016\u0001\u0016\u0001"+
		"\u0016\u0001\u0016\u0001\u0016\u0001\u0016\u0003\u0016\u00f7\b\u0016\u0001"+
		"\u0017\u0001\u0017\u0003\u0017\u00fb\b\u0017\u0001\u0017\u0001\u0017\u0001"+
		"\u0017\u0001\u0018\u0001\u0018\u0001\u0018\u0005\u0018\u0103\b\u0018\n"+
		"\u0018\f\u0018\u0106\t\u0018\u0001\u0018\u0001\u0018\u0001\u0019\u0001"+
		"\u0019\u0001\u0019\u0003\u0019\u010d\b\u0019\u0001\u0019\u0001\u0019\u0001"+
		"\u001a\u0001\u001a\u0001\u001a\u0005\u001a\u0114\b\u001a\n\u001a\f\u001a"+
		"\u0117\t\u001a\u0001\u001b\u0001\u001b\u0001\u001b\u0003\u001b\u011c\b"+
		"\u001b\u0001\u001c\u0001\u001c\u0001\u001c\u0005\u001c\u0121\b\u001c\n"+
		"\u001c\f\u001c\u0124\t\u001c\u0001\u001d\u0001\u001d\u0003\u001d\u0128"+
		"\b\u001d\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0003"+
		"\u001e\u012f\b\u001e\u0001\u001e\u0001\u001e\u0001\u001f\u0001\u001f\u0001"+
		"\u001f\u0001\u001f\u0001\u001f\u0005\u001f\u0138\b\u001f\n\u001f\f\u001f"+
		"\u013b\t\u001f\u0001\u001f\u0001\u001f\u0001 \u0001 \u0001 \u0001 \u0001"+
		" \u0001 \u0001 \u0001 \u0001 \u0001 \u0003 \u0149\b \u0001 \u0001 \u0001"+
		"!\u0001!\u0001\"\u0001\"\u0001#\u0003#\u0152\b#\u0001#\u0001#\u0001#\u0005"+
		"#\u0157\b#\n#\f#\u015a\t#\u0001#\u0001#\u0001$\u0001$\u0001%\u0001%\u0001"+
		"%\u0001%\u0001%\u0003%\u0165\b%\u0001%\u0000\u0000&\u0000\u0002\u0004"+
		"\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u001c\u001e \""+
		"$&(*,.02468:<>@BDFHJ\u0000\u0005\u0002\u0000\u0014\u0014%%\u0002\u0000"+
		"\u0015\u0016&&\u0006\u0000\u0003\u0005\b\n\r\u000e\u001b\u001e  #$\u0006"+
		"\u0000\u0003\u0003\b\t\r\u000e\u001b\u001e  #$\u0005\u0000\u0010\u0010"+
		"\u0012\u0013\u0019\u001a\u001f!%%\u0175\u0000M\u0001\u0000\u0000\u0000"+
		"\u0002[\u0001\u0000\u0000\u0000\u0004_\u0001\u0000\u0000\u0000\u0006a"+
		"\u0001\u0000\u0000\u0000\bf\u0001\u0000\u0000\u0000\nm\u0001\u0000\u0000"+
		"\u0000\fv\u0001\u0000\u0000\u0000\u000e\u007f\u0001\u0000\u0000\u0000"+
		"\u0010\u0083\u0001\u0000\u0000\u0000\u0012\u0087\u0001\u0000\u0000\u0000"+
		"\u0014\u0096\u0001\u0000\u0000\u0000\u0016\u0098\u0001\u0000\u0000\u0000"+
		"\u0018\u009a\u0001\u0000\u0000\u0000\u001a\u009e\u0001\u0000\u0000\u0000"+
		"\u001c\u00a1\u0001\u0000\u0000\u0000\u001e\u00a8\u0001\u0000\u0000\u0000"+
		" \u00ba\u0001\u0000\u0000\u0000\"\u00c6\u0001\u0000\u0000\u0000$\u00ca"+
		"\u0001\u0000\u0000\u0000&\u00d5\u0001\u0000\u0000\u0000(\u00e0\u0001\u0000"+
		"\u0000\u0000*\u00e4\u0001\u0000\u0000\u0000,\u00ef\u0001\u0000\u0000\u0000"+
		".\u00f8\u0001\u0000\u0000\u00000\u00ff\u0001\u0000\u0000\u00002\u0109"+
		"\u0001\u0000\u0000\u00004\u0110\u0001\u0000\u0000\u00006\u0118\u0001\u0000"+
		"\u0000\u00008\u011d\u0001\u0000\u0000\u0000:\u0127\u0001\u0000\u0000\u0000"+
		"<\u0129\u0001\u0000\u0000\u0000>\u0132\u0001\u0000\u0000\u0000@\u013e"+
		"\u0001\u0000\u0000\u0000B\u014c\u0001\u0000\u0000\u0000D\u014e\u0001\u0000"+
		"\u0000\u0000F\u0151\u0001\u0000\u0000\u0000H\u015d\u0001\u0000\u0000\u0000"+
		"J\u0164\u0001\u0000\u0000\u0000LN\u0003\u0006\u0003\u0000ML\u0001\u0000"+
		"\u0000\u0000MN\u0001\u0000\u0000\u0000NV\u0001\u0000\u0000\u0000OU\u0003"+
		"\b\u0004\u0000PU\u0003\n\u0005\u0000QU\u0003\u0010\b\u0000RU\u0003\u0014"+
		"\n\u0000SU\u0003H$\u0000TO\u0001\u0000\u0000\u0000TP\u0001\u0000\u0000"+
		"\u0000TQ\u0001\u0000\u0000\u0000TR\u0001\u0000\u0000\u0000TS\u0001\u0000"+
		"\u0000\u0000UX\u0001\u0000\u0000\u0000VT\u0001\u0000\u0000\u0000VW\u0001"+
		"\u0000\u0000\u0000WY\u0001\u0000\u0000\u0000XV\u0001\u0000\u0000\u0000"+
		"YZ\u0005\u0000\u0000\u0001Z\u0001\u0001\u0000\u0000\u0000[\\\u0005,\u0000"+
		"\u0000\\\u0003\u0001\u0000\u0000\u0000]`\u0003\u0016\u000b\u0000^`\u0003"+
		"D\"\u0000_]\u0001\u0000\u0000\u0000_^\u0001\u0000\u0000\u0000`\u0005\u0001"+
		"\u0000\u0000\u0000ab\u0005!\u0000\u0000bc\u0005:\u0000\u0000cd\u0003\u0002"+
		"\u0001\u0000de\u0005\u0001\u0000\u0000e\u0007\u0001\u0000\u0000\u0000"+
		"fh\u0005\f\u0000\u0000gi\u0007\u0000\u0000\u0000hg\u0001\u0000\u0000\u0000"+
		"hi\u0001\u0000\u0000\u0000ij\u0001\u0000\u0000\u0000jk\u0003\u0002\u0001"+
		"\u0000kl\u0005\u0001\u0000\u0000l\t\u0001\u0000\u0000\u0000mn\u0005\u0013"+
		"\u0000\u0000no\u0003F#\u0000op\u0005\u0001\u0000\u0000p\u000b\u0001\u0000"+
		"\u0000\u0000qw\u0003\u0016\u000b\u0000rs\u0005.\u0000\u0000st\u0003F#"+
		"\u0000tu\u0005/\u0000\u0000uw\u0001\u0000\u0000\u0000vq\u0001\u0000\u0000"+
		"\u0000vr\u0001\u0000\u0000\u0000w|\u0001\u0000\u0000\u0000xy\u00057\u0000"+
		"\u0000y{\u0003\u0004\u0002\u0000zx\u0001\u0000\u0000\u0000{~\u0001\u0000"+
		"\u0000\u0000|z\u0001\u0000\u0000\u0000|}\u0001\u0000\u0000\u0000}\r\u0001"+
		"\u0000\u0000\u0000~|\u0001\u0000\u0000\u0000\u007f\u0080\u0003\f\u0006"+
		"\u0000\u0080\u0081\u0005:\u0000\u0000\u0081\u0082\u0003J%\u0000\u0082"+
		"\u000f\u0001\u0000\u0000\u0000\u0083\u0084\u0005\u0012\u0000\u0000\u0084"+
		"\u0085\u0003\u000e\u0007\u0000\u0085\u0086\u0005\u0001\u0000\u0000\u0086"+
		"\u0011\u0001\u0000\u0000\u0000\u0087\u0088\u00052\u0000\u0000\u0088\u008d"+
		"\u0003\u000e\u0007\u0000\u0089\u008a\u00056\u0000\u0000\u008a\u008c\u0003"+
		"\u000e\u0007\u0000\u008b\u0089\u0001\u0000\u0000\u0000\u008c\u008f\u0001"+
		"\u0000\u0000\u0000\u008d\u008b\u0001\u0000\u0000\u0000\u008d\u008e\u0001"+
		"\u0000\u0000\u0000\u008e\u0090\u0001\u0000\u0000\u0000\u008f\u008d\u0001"+
		"\u0000\u0000\u0000\u0090\u0091\u00053\u0000\u0000\u0091\u0013\u0001\u0000"+
		"\u0000\u0000\u0092\u0097\u0003\u0018\f\u0000\u0093\u0097\u0003\"\u0011"+
		"\u0000\u0094\u0097\u0003(\u0014\u0000\u0095\u0097\u0003 \u0010\u0000\u0096"+
		"\u0092\u0001\u0000\u0000\u0000\u0096\u0093\u0001\u0000\u0000\u0000\u0096"+
		"\u0094\u0001\u0000\u0000\u0000\u0096\u0095\u0001\u0000\u0000\u0000\u0097"+
		"\u0015\u0001\u0000\u0000\u0000\u0098\u0099\u0005\'\u0000\u0000\u0099\u0017"+
		"\u0001\u0000\u0000\u0000\u009a\u009b\u0005\u0010\u0000\u0000\u009b\u009c"+
		"\u0003\u0016\u000b\u0000\u009c\u009d\u0003\u001e\u000f\u0000\u009d\u0019"+
		"\u0001\u0000\u0000\u0000\u009e\u009f\u0007\u0001\u0000\u0000\u009f\u00a0"+
		"\u0003<\u001e\u0000\u00a0\u001b\u0001\u0000\u0000\u0000\u00a1\u00a2\u0007"+
		"\u0001\u0000\u0000\u00a2\u00a3\u0005\u000b\u0000\u0000\u00a3\u00a4\u0003"+
		"\u0016\u000b\u0000\u00a4\u00a5\u0005:\u0000\u0000\u00a5\u00a6\u0005(\u0000"+
		"\u0000\u00a6\u00a7\u0003\u001e\u000f\u0000\u00a7\u001d\u0001\u0000\u0000"+
		"\u0000\u00a8\u00b5\u00050\u0000\u0000\u00a9\u00b4\u0003\u001a\r\u0000"+
		"\u00aa\u00b4\u0003\u001c\u000e\u0000\u00ab\u00b4\u0003\"\u0011\u0000\u00ac"+
		"\u00b4\u0003 \u0010\u0000\u00ad\u00b4\u0003\u0018\f\u0000\u00ae\u00b4"+
		"\u0003\u0010\b\u0000\u00af\u00b4\u0003>\u001f\u0000\u00b0\u00b4\u0003"+
		"@ \u0000\u00b1\u00b4\u00032\u0019\u0000\u00b2\u00b4\u0003H$\u0000\u00b3"+
		"\u00a9\u0001\u0000\u0000\u0000\u00b3\u00aa\u0001\u0000\u0000\u0000\u00b3"+
		"\u00ab\u0001\u0000\u0000\u0000\u00b3\u00ac\u0001\u0000\u0000\u0000\u00b3"+
		"\u00ad\u0001\u0000\u0000\u0000\u00b3\u00ae\u0001\u0000\u0000\u0000\u00b3"+
		"\u00af\u0001\u0000\u0000\u0000\u00b3\u00b0\u0001\u0000\u0000\u0000\u00b3"+
		"\u00b1\u0001\u0000\u0000\u0000\u00b3\u00b2\u0001\u0000\u0000\u0000\u00b4"+
		"\u00b7\u0001\u0000\u0000\u0000\u00b5\u00b3\u0001\u0000\u0000\u0000\u00b5"+
		"\u00b6\u0001\u0000\u0000\u0000\u00b6\u00b8\u0001\u0000\u0000\u0000\u00b7"+
		"\u00b5\u0001\u0000\u0000\u0000\u00b8\u00b9\u00051\u0000\u0000\u00b9\u001f"+
		"\u0001\u0000\u0000\u0000\u00ba\u00bb\u0005\u0007\u0000\u0000\u00bb\u00bc"+
		"\u0003F#\u0000\u00bc\u00c1\u00050\u0000\u0000\u00bd\u00c0\u0003\u001a"+
		"\r\u0000\u00be\u00c0\u0003H$\u0000\u00bf\u00bd\u0001\u0000\u0000\u0000"+
		"\u00bf\u00be\u0001\u0000\u0000\u0000\u00c0\u00c3\u0001\u0000\u0000\u0000"+
		"\u00c1\u00bf\u0001\u0000\u0000\u0000\u00c1\u00c2\u0001\u0000\u0000\u0000"+
		"\u00c2\u00c4\u0001\u0000\u0000\u0000\u00c3\u00c1\u0001\u0000\u0000\u0000"+
		"\u00c4\u00c5\u00051\u0000\u0000\u00c5!\u0001\u0000\u0000\u0000\u00c6\u00c7"+
		"\u0005\u0006\u0000\u0000\u00c7\u00c8\u0003\u0016\u000b\u0000\u00c8\u00c9"+
		"\u0003$\u0012\u0000\u00c9#\u0001\u0000\u0000\u0000\u00ca\u00d0\u00050"+
		"\u0000\u0000\u00cb\u00cf\u0003\u0010\b\u0000\u00cc\u00cf\u0003&\u0013"+
		"\u0000\u00cd\u00cf\u0003H$\u0000\u00ce\u00cb\u0001\u0000\u0000\u0000\u00ce"+
		"\u00cc\u0001\u0000\u0000\u0000\u00ce\u00cd\u0001\u0000\u0000\u0000\u00cf"+
		"\u00d2\u0001\u0000\u0000\u0000\u00d0\u00ce\u0001\u0000\u0000\u0000\u00d0"+
		"\u00d1\u0001\u0000\u0000\u0000\u00d1\u00d3\u0001\u0000\u0000\u0000\u00d2"+
		"\u00d0\u0001\u0000\u0000\u0000\u00d3\u00d4\u00051\u0000\u0000\u00d4%\u0001"+
		"\u0000\u0000\u0000\u00d5\u00d6\u0003\u0016\u000b\u0000\u00d6\u00d8\u0005"+
		":\u0000\u0000\u00d7\u00d9\u00058\u0000\u0000\u00d8\u00d7\u0001\u0000\u0000"+
		"\u0000\u00d8\u00d9\u0001\u0000\u0000\u0000\u00d9\u00da\u0001\u0000\u0000"+
		"\u0000\u00da\u00dc\u0005(\u0000\u0000\u00db\u00dd\u0003\u0012\t\u0000"+
		"\u00dc\u00db\u0001\u0000\u0000\u0000\u00dc\u00dd\u0001\u0000\u0000\u0000"+
		"\u00dd\u00de\u0001\u0000\u0000\u0000\u00de\u00df\u0005\u0001\u0000\u0000"+
		"\u00df\'\u0001\u0000\u0000\u0000\u00e0\u00e1\u0005\u001a\u0000\u0000\u00e1"+
		"\u00e2\u0003\u0016\u000b\u0000\u00e2\u00e3\u0003*\u0015\u0000\u00e3)\u0001"+
		"\u0000\u0000\u0000\u00e4\u00ea\u00050\u0000\u0000\u00e5\u00e9\u0003\u0010"+
		"\b\u0000\u00e6\u00e9\u0003,\u0016\u0000\u00e7\u00e9\u0003H$\u0000\u00e8"+
		"\u00e5\u0001\u0000\u0000\u0000\u00e8\u00e6\u0001\u0000\u0000\u0000\u00e8"+
		"\u00e7\u0001\u0000\u0000\u0000\u00e9\u00ec\u0001\u0000\u0000\u0000\u00ea"+
		"\u00e8\u0001\u0000\u0000\u0000\u00ea\u00eb\u0001\u0000\u0000\u0000\u00eb"+
		"\u00ed\u0001\u0000\u0000\u0000\u00ec\u00ea\u0001\u0000\u0000\u0000\u00ed"+
		"\u00ee\u00051\u0000\u0000\u00ee+\u0001\u0000\u0000\u0000\u00ef\u00f0\u0005"+
		"\u0019\u0000\u0000\u00f0\u00f1\u0003\u0016\u000b\u0000\u00f1\u00f2\u0003"+
		".\u0017\u0000\u00f2\u00f3\u0005\u0018\u0000\u0000\u00f3\u00f6\u0003.\u0017"+
		"\u0000\u00f4\u00f7\u00030\u0018\u0000\u00f5\u00f7\u0005\u0001\u0000\u0000"+
		"\u00f6\u00f4\u0001\u0000\u0000\u0000\u00f6\u00f5\u0001\u0000\u0000\u0000"+
		"\u00f7-\u0001\u0000\u0000\u0000\u00f8\u00fa\u0005.\u0000\u0000\u00f9\u00fb"+
		"\u0005\u001f\u0000\u0000\u00fa\u00f9\u0001\u0000\u0000\u0000\u00fa\u00fb"+
		"\u0001\u0000\u0000\u0000\u00fb\u00fc\u0001\u0000\u0000\u0000\u00fc\u00fd"+
		"\u0003F#\u0000\u00fd\u00fe\u0005/\u0000\u0000\u00fe/\u0001\u0000\u0000"+
		"\u0000\u00ff\u0104\u00050\u0000\u0000\u0100\u0103\u0003\u0010\b\u0000"+
		"\u0101\u0103\u0003H$\u0000\u0102\u0100\u0001\u0000\u0000\u0000\u0102\u0101"+
		"\u0001\u0000\u0000\u0000\u0103\u0106\u0001\u0000\u0000\u0000\u0104\u0102"+
		"\u0001\u0000\u0000\u0000\u0104\u0105\u0001\u0000\u0000\u0000\u0105\u0107"+
		"\u0001\u0000\u0000\u0000\u0106\u0104\u0001\u0000\u0000\u0000\u0107\u0108"+
		"\u00051\u0000\u0000\u01081\u0001\u0000\u0000\u0000\u0109\u010c\u0005\u0017"+
		"\u0000\u0000\u010a\u010d\u00034\u001a\u0000\u010b\u010d\u00038\u001c\u0000"+
		"\u010c\u010a\u0001\u0000\u0000\u0000\u010c\u010b\u0001\u0000\u0000\u0000"+
		"\u010d\u010e\u0001\u0000\u0000\u0000\u010e\u010f\u0005\u0001\u0000\u0000"+
		"\u010f3\u0001\u0000\u0000\u0000\u0110\u0115\u00036\u001b\u0000\u0111\u0112"+
		"\u00056\u0000\u0000\u0112\u0114\u00036\u001b\u0000\u0113\u0111\u0001\u0000"+
		"\u0000\u0000\u0114\u0117\u0001\u0000\u0000\u0000\u0115\u0113\u0001\u0000"+
		"\u0000\u0000\u0115\u0116\u0001\u0000\u0000\u0000\u01165\u0001\u0000\u0000"+
		"\u0000\u0117\u0115\u0001\u0000\u0000\u0000\u0118\u011b\u0005(\u0000\u0000"+
		"\u0119\u011a\u0005\"\u0000\u0000\u011a\u011c\u0005(\u0000\u0000\u011b"+
		"\u0119\u0001\u0000\u0000\u0000\u011b\u011c\u0001\u0000\u0000\u0000\u011c"+
		"7\u0001\u0000\u0000\u0000\u011d\u0122\u0003\u0002\u0001\u0000\u011e\u011f"+
		"\u00056\u0000\u0000\u011f\u0121\u0003\u0002\u0001\u0000\u0120\u011e\u0001"+
		"\u0000\u0000\u0000\u0121\u0124\u0001\u0000\u0000\u0000\u0122\u0120\u0001"+
		"\u0000\u0000\u0000\u0122\u0123\u0001\u0000\u0000\u0000\u01239\u0001\u0000"+
		"\u0000\u0000\u0124\u0122\u0001\u0000\u0000\u0000\u0125\u0128\u0007\u0002"+
		"\u0000\u0000\u0126\u0128\u0003F#\u0000\u0127\u0125\u0001\u0000\u0000\u0000"+
		"\u0127\u0126\u0001\u0000\u0000\u0000\u0128;\u0001\u0000\u0000\u0000\u0129"+
		"\u012a\u0003:\u001d\u0000\u012a\u012b\u0003\u0004\u0002\u0000\u012b\u012c"+
		"\u0005:\u0000\u0000\u012c\u012e\u0005(\u0000\u0000\u012d\u012f\u0003\u0012"+
		"\t\u0000\u012e\u012d\u0001\u0000\u0000\u0000\u012e\u012f\u0001\u0000\u0000"+
		"\u0000\u012f\u0130\u0001\u0000\u0000\u0000\u0130\u0131\u0005\u0001\u0000"+
		"\u0000\u0131=\u0001\u0000\u0000\u0000\u0132\u0133\u0005\u0011\u0000\u0000"+
		"\u0133\u0134\u0003\u0016\u000b\u0000\u0134\u0139\u00050\u0000\u0000\u0135"+
		"\u0138\u0003<\u001e\u0000\u0136\u0138\u0003H$\u0000\u0137\u0135\u0001"+
		"\u0000\u0000\u0000\u0137\u0136\u0001\u0000\u0000\u0000\u0138\u013b\u0001"+
		"\u0000\u0000\u0000\u0139\u0137\u0001\u0000\u0000\u0000\u0139\u013a\u0001"+
		"\u0000\u0000\u0000\u013a\u013c\u0001\u0000\u0000\u0000\u013b\u0139\u0001"+
		"\u0000\u0000\u0000\u013c\u013d\u00051\u0000\u0000\u013d?\u0001\u0000\u0000"+
		"\u0000\u013e\u013f\u0005\u000f\u0000\u0000\u013f\u0140\u00054\u0000\u0000"+
		"\u0140\u0141\u0003B!\u0000\u0141\u0142\u00056\u0000\u0000\u0142\u0143"+
		"\u0003:\u001d\u0000\u0143\u0144\u00055\u0000\u0000\u0144\u0145\u0003\u0016"+
		"\u000b\u0000\u0145\u0146\u0005:\u0000\u0000\u0146\u0148\u0005(\u0000\u0000"+
		"\u0147\u0149\u0003\u0012\t\u0000\u0148\u0147\u0001\u0000\u0000\u0000\u0148"+
		"\u0149\u0001\u0000\u0000\u0000\u0149\u014a\u0001\u0000\u0000\u0000\u014a"+
		"\u014b\u0005\u0001\u0000\u0000\u014bA\u0001\u0000\u0000\u0000\u014c\u014d"+
		"\u0007\u0003\u0000\u0000\u014dC\u0001\u0000\u0000\u0000\u014e\u014f\u0007"+
		"\u0004\u0000\u0000\u014fE\u0001\u0000\u0000\u0000\u0150\u0152\u00057\u0000"+
		"\u0000\u0151\u0150\u0001\u0000\u0000\u0000\u0151\u0152\u0001\u0000\u0000"+
		"\u0000\u0152\u0158\u0001\u0000\u0000\u0000\u0153\u0154\u0003\u0004\u0002"+
		"\u0000\u0154\u0155\u00057\u0000\u0000\u0155\u0157\u0001\u0000\u0000\u0000"+
		"\u0156\u0153\u0001\u0000\u0000\u0000\u0157\u015a\u0001\u0000\u0000\u0000"+
		"\u0158\u0156\u0001\u0000\u0000\u0000\u0158\u0159\u0001\u0000\u0000\u0000"+
		"\u0159\u015b\u0001\u0000\u0000\u0000\u015a\u0158\u0001\u0000\u0000\u0000"+
		"\u015b\u015c\u0003\u0004\u0002\u0000\u015cG\u0001\u0000\u0000\u0000\u015d"+
		"\u015e\u0005\u0001\u0000\u0000\u015eI\u0001\u0000\u0000\u0000\u015f\u0165"+
		"\u0003F#\u0000\u0160\u0165\u0005(\u0000\u0000\u0161\u0165\u0005)\u0000"+
		"\u0000\u0162\u0165\u0005,\u0000\u0000\u0163\u0165\u0005+\u0000\u0000\u0164"+
		"\u015f\u0001\u0000\u0000\u0000\u0164\u0160\u0001\u0000\u0000\u0000\u0164"+
		"\u0161\u0001\u0000\u0000\u0000\u0164\u0162\u0001\u0000\u0000\u0000\u0164"+
		"\u0163\u0001\u0000\u0000\u0000\u0165K\u0001\u0000\u0000\u0000#MTV_hv|"+
		"\u008d\u0096\u00b3\u00b5\u00bf\u00c1\u00ce\u00d0\u00d8\u00dc\u00e8\u00ea"+
		"\u00f6\u00fa\u0102\u0104\u010c\u0115\u011b\u0122\u0127\u012e\u0137\u0139"+
		"\u0148\u0151\u0158\u0164";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
