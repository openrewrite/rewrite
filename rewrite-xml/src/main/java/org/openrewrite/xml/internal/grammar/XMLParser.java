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
// Generated from rewrite-xml/src/main/antlr/XMLParser.g4 by ANTLR 4.13.2
package org.openrewrite.xml.internal.grammar;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class XMLParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		WS=1, COMMENT=2, CDATA=3, ParamEntityRef=4, EntityRef=5, CharRef=6, SEA_WS=7,
		UTF_ENCODING_BOM=8, QUESTION_MARK=9, SPECIAL_OPEN_XML=10, OPEN=11, SPECIAL_OPEN=12,
		DTD_OPEN=13, JSP_COMMENT=14, JSP_DECLARATION=15, JSP_EXPRESSION=16, JSP_SCRIPTLET=17,
		TEXT=18, DTD_CLOSE=19, DTD_SUBSET_OPEN=20, DTD_S=21, DOCTYPE=22, DTD_SUBSET_CLOSE=23,
		MARKUP_OPEN=24, DTS_SUBSET_S=25, MARK_UP_CLOSE=26, MARKUP_S=27, MARKUP_TEXT=28,
		MARKUP_SUBSET=29, PI_S=30, PI_TEXT=31, CLOSE=32, SPECIAL_CLOSE=33, SLASH_CLOSE=34,
		S=35, DIRECTIVE_OPEN=36, DIRECTIVE_CLOSE=37, SLASH=38, EQUALS=39, STRING=40,
		Name=41;
	public static final int
		RULE_document = 0, RULE_prolog = 1, RULE_xmldecl = 2, RULE_misc = 3, RULE_doctypedecl = 4,
		RULE_intsubset = 5, RULE_markupdecl = 6, RULE_declSep = 7, RULE_externalid = 8,
		RULE_processinginstruction = 9, RULE_content = 10, RULE_element = 11,
		RULE_jspdirective = 12, RULE_jspscriptlet = 13, RULE_jspexpression = 14,
		RULE_jspdeclaration = 15, RULE_jspcomment = 16, RULE_reference = 17, RULE_attribute = 18,
		RULE_chardata = 19;
	private static String[] makeRuleNames() {
		return new String[] {
			"document", "prolog", "xmldecl", "misc", "doctypedecl", "intsubset",
			"markupdecl", "declSep", "externalid", "processinginstruction", "content",
			"element", "jspdirective", "jspscriptlet", "jspexpression", "jspdeclaration",
			"jspcomment", "reference", "attribute", "chardata"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, "'?'", null, "'<'",
			null, null, null, null, null, null, null, null, null, null, null, null,
			null, null, null, null, null, null, null, null, null, null, "'/>'", null,
			"'%@'", "'%'", "'/'", "'='"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "WS", "COMMENT", "CDATA", "ParamEntityRef", "EntityRef", "CharRef",
			"SEA_WS", "UTF_ENCODING_BOM", "QUESTION_MARK", "SPECIAL_OPEN_XML", "OPEN",
			"SPECIAL_OPEN", "DTD_OPEN", "JSP_COMMENT", "JSP_DECLARATION", "JSP_EXPRESSION",
			"JSP_SCRIPTLET", "TEXT", "DTD_CLOSE", "DTD_SUBSET_OPEN", "DTD_S", "DOCTYPE",
			"DTD_SUBSET_CLOSE", "MARKUP_OPEN", "DTS_SUBSET_S", "MARK_UP_CLOSE", "MARKUP_S",
			"MARKUP_TEXT", "MARKUP_SUBSET", "PI_S", "PI_TEXT", "CLOSE", "SPECIAL_CLOSE",
			"SLASH_CLOSE", "S", "DIRECTIVE_OPEN", "DIRECTIVE_CLOSE", "SLASH", "EQUALS",
			"STRING", "Name"
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
	public String getGrammarFileName() { return "XMLParser.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public XMLParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DocumentContext extends ParserRuleContext {
		public PrologContext prolog() {
			return getRuleContext(PrologContext.class,0);
		}
		public TerminalNode UTF_ENCODING_BOM() { return getToken(XMLParser.UTF_ENCODING_BOM, 0); }
		public ElementContext element() {
			return getRuleContext(ElementContext.class,0);
		}
		public DocumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_document; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).enterDocument(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).exitDocument(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XMLParserVisitor ) return ((XMLParserVisitor<? extends T>)visitor).visitDocument(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DocumentContext document() throws RecognitionException {
		DocumentContext _localctx = new DocumentContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_document);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(41);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==UTF_ENCODING_BOM) {
				{
				setState(40);
				match(UTF_ENCODING_BOM);
				}
			}

			setState(43);
			prolog();
			setState(45);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==OPEN) {
				{
				setState(44);
				element();
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
	public static class PrologContext extends ParserRuleContext {
		public XmldeclContext xmldecl() {
			return getRuleContext(XmldeclContext.class,0);
		}
		public List<MiscContext> misc() {
			return getRuleContexts(MiscContext.class);
		}
		public MiscContext misc(int i) {
			return getRuleContext(MiscContext.class,i);
		}
		public List<JspdirectiveContext> jspdirective() {
			return getRuleContexts(JspdirectiveContext.class);
		}
		public JspdirectiveContext jspdirective(int i) {
			return getRuleContext(JspdirectiveContext.class,i);
		}
		public PrologContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_prolog; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).enterProlog(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).exitProlog(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XMLParserVisitor ) return ((XMLParserVisitor<? extends T>)visitor).visitProlog(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PrologContext prolog() throws RecognitionException {
		PrologContext _localctx = new PrologContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_prolog);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(48);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SPECIAL_OPEN_XML) {
				{
				setState(47);
				xmldecl();
				}
			}

			setState(53);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 61444L) != 0)) {
				{
				{
				setState(50);
				misc();
				}
				}
				setState(55);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(59);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,4,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(56);
					jspdirective();
					}
					}
				}
				setState(61);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,4,_ctx);
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
	public static class XmldeclContext extends ParserRuleContext {
		public TerminalNode SPECIAL_OPEN_XML() { return getToken(XMLParser.SPECIAL_OPEN_XML, 0); }
		public TerminalNode SPECIAL_CLOSE() { return getToken(XMLParser.SPECIAL_CLOSE, 0); }
		public List<AttributeContext> attribute() {
			return getRuleContexts(AttributeContext.class);
		}
		public AttributeContext attribute(int i) {
			return getRuleContext(AttributeContext.class,i);
		}
		public XmldeclContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_xmldecl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).enterXmldecl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).exitXmldecl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XMLParserVisitor ) return ((XMLParserVisitor<? extends T>)visitor).visitXmldecl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final XmldeclContext xmldecl() throws RecognitionException {
		XmldeclContext _localctx = new XmldeclContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_xmldecl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(62);
			match(SPECIAL_OPEN_XML);
			setState(66);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==Name) {
				{
				{
				setState(63);
				attribute();
				}
				}
				setState(68);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(69);
			match(SPECIAL_CLOSE);
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
	public static class MiscContext extends ParserRuleContext {
		public TerminalNode COMMENT() { return getToken(XMLParser.COMMENT, 0); }
		public DoctypedeclContext doctypedecl() {
			return getRuleContext(DoctypedeclContext.class,0);
		}
		public ProcessinginstructionContext processinginstruction() {
			return getRuleContext(ProcessinginstructionContext.class,0);
		}
		public JspdeclarationContext jspdeclaration() {
			return getRuleContext(JspdeclarationContext.class,0);
		}
		public JspcommentContext jspcomment() {
			return getRuleContext(JspcommentContext.class,0);
		}
		public MiscContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_misc; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).enterMisc(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).exitMisc(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XMLParserVisitor ) return ((XMLParserVisitor<? extends T>)visitor).visitMisc(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MiscContext misc() throws RecognitionException {
		MiscContext _localctx = new MiscContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_misc);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(76);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case COMMENT:
				{
				setState(71);
				match(COMMENT);
				}
				break;
			case DTD_OPEN:
				{
				setState(72);
				doctypedecl();
				}
				break;
			case SPECIAL_OPEN:
				{
				setState(73);
				processinginstruction();
				}
				break;
			case JSP_DECLARATION:
				{
				setState(74);
				jspdeclaration();
				}
				break;
			case JSP_COMMENT:
				{
				setState(75);
				jspcomment();
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
	public static class DoctypedeclContext extends ParserRuleContext {
		public TerminalNode DTD_OPEN() { return getToken(XMLParser.DTD_OPEN, 0); }
		public TerminalNode DOCTYPE() { return getToken(XMLParser.DOCTYPE, 0); }
		public TerminalNode Name() { return getToken(XMLParser.Name, 0); }
		public ExternalidContext externalid() {
			return getRuleContext(ExternalidContext.class,0);
		}
		public TerminalNode DTD_CLOSE() { return getToken(XMLParser.DTD_CLOSE, 0); }
		public List<TerminalNode> STRING() { return getTokens(XMLParser.STRING); }
		public TerminalNode STRING(int i) {
			return getToken(XMLParser.STRING, i);
		}
		public TerminalNode DTD_SUBSET_OPEN() { return getToken(XMLParser.DTD_SUBSET_OPEN, 0); }
		public IntsubsetContext intsubset() {
			return getRuleContext(IntsubsetContext.class,0);
		}
		public TerminalNode DTD_SUBSET_CLOSE() { return getToken(XMLParser.DTD_SUBSET_CLOSE, 0); }
		public DoctypedeclContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_doctypedecl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).enterDoctypedecl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).exitDoctypedecl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XMLParserVisitor ) return ((XMLParserVisitor<? extends T>)visitor).visitDoctypedecl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DoctypedeclContext doctypedecl() throws RecognitionException {
		DoctypedeclContext _localctx = new DoctypedeclContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_doctypedecl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(78);
			match(DTD_OPEN);
			setState(79);
			match(DOCTYPE);
			setState(80);
			match(Name);
			setState(81);
			externalid();
			setState(85);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==STRING) {
				{
				{
				setState(82);
				match(STRING);
				}
				}
				setState(87);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(92);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==DTD_SUBSET_OPEN) {
				{
				setState(88);
				match(DTD_SUBSET_OPEN);
				setState(89);
				intsubset();
				setState(90);
				match(DTD_SUBSET_CLOSE);
				}
			}

			setState(94);
			match(DTD_CLOSE);
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
	public static class IntsubsetContext extends ParserRuleContext {
		public List<MarkupdeclContext> markupdecl() {
			return getRuleContexts(MarkupdeclContext.class);
		}
		public MarkupdeclContext markupdecl(int i) {
			return getRuleContext(MarkupdeclContext.class,i);
		}
		public List<DeclSepContext> declSep() {
			return getRuleContexts(DeclSepContext.class);
		}
		public DeclSepContext declSep(int i) {
			return getRuleContext(DeclSepContext.class,i);
		}
		public IntsubsetContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_intsubset; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).enterIntsubset(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).exitIntsubset(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XMLParserVisitor ) return ((XMLParserVisitor<? extends T>)visitor).visitIntsubset(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IntsubsetContext intsubset() throws RecognitionException {
		IntsubsetContext _localctx = new IntsubsetContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_intsubset);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(100);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 16781332L) != 0)) {
				{
				setState(98);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case COMMENT:
				case SPECIAL_OPEN:
				case MARKUP_OPEN:
					{
					setState(96);
					markupdecl();
					}
					break;
				case ParamEntityRef:
					{
					setState(97);
					declSep();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(102);
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
	public static class MarkupdeclContext extends ParserRuleContext {
		public TerminalNode MARKUP_OPEN() { return getToken(XMLParser.MARKUP_OPEN, 0); }
		public TerminalNode MARK_UP_CLOSE() { return getToken(XMLParser.MARK_UP_CLOSE, 0); }
		public List<TerminalNode> MARKUP_TEXT() { return getTokens(XMLParser.MARKUP_TEXT); }
		public TerminalNode MARKUP_TEXT(int i) {
			return getToken(XMLParser.MARKUP_TEXT, i);
		}
		public List<TerminalNode> MARKUP_SUBSET() { return getTokens(XMLParser.MARKUP_SUBSET); }
		public TerminalNode MARKUP_SUBSET(int i) {
			return getToken(XMLParser.MARKUP_SUBSET, i);
		}
		public ProcessinginstructionContext processinginstruction() {
			return getRuleContext(ProcessinginstructionContext.class,0);
		}
		public TerminalNode COMMENT() { return getToken(XMLParser.COMMENT, 0); }
		public MarkupdeclContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_markupdecl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).enterMarkupdecl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).exitMarkupdecl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XMLParserVisitor ) return ((XMLParserVisitor<? extends T>)visitor).visitMarkupdecl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MarkupdeclContext markupdecl() throws RecognitionException {
		MarkupdeclContext _localctx = new MarkupdeclContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_markupdecl);
		int _la;
		try {
			setState(119);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case MARKUP_OPEN:
				enterOuterAlt(_localctx, 1);
				{
				{
				setState(103);
				match(MARKUP_OPEN);
				setState(105);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
				case 1:
					{
					setState(104);
					match(MARKUP_TEXT);
					}
					break;
				}
				setState(110);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==MARKUP_SUBSET) {
					{
					{
					setState(107);
					match(MARKUP_SUBSET);
					}
					}
					setState(112);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(114);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==MARKUP_TEXT) {
					{
					setState(113);
					match(MARKUP_TEXT);
					}
				}

				setState(116);
				match(MARK_UP_CLOSE);
				}
				}
				break;
			case SPECIAL_OPEN:
				enterOuterAlt(_localctx, 2);
				{
				setState(117);
				processinginstruction();
				}
				break;
			case COMMENT:
				enterOuterAlt(_localctx, 3);
				{
				setState(118);
				match(COMMENT);
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
	public static class DeclSepContext extends ParserRuleContext {
		public TerminalNode ParamEntityRef() { return getToken(XMLParser.ParamEntityRef, 0); }
		public DeclSepContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_declSep; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).enterDeclSep(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).exitDeclSep(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XMLParserVisitor ) return ((XMLParserVisitor<? extends T>)visitor).visitDeclSep(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DeclSepContext declSep() throws RecognitionException {
		DeclSepContext _localctx = new DeclSepContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_declSep);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(121);
			match(ParamEntityRef);
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
	public static class ExternalidContext extends ParserRuleContext {
		public TerminalNode Name() { return getToken(XMLParser.Name, 0); }
		public ExternalidContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_externalid; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).enterExternalid(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).exitExternalid(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XMLParserVisitor ) return ((XMLParserVisitor<? extends T>)visitor).visitExternalid(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExternalidContext externalid() throws RecognitionException {
		ExternalidContext _localctx = new ExternalidContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_externalid);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(124);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==Name) {
				{
				setState(123);
				match(Name);
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
	public static class ProcessinginstructionContext extends ParserRuleContext {
		public TerminalNode SPECIAL_OPEN() { return getToken(XMLParser.SPECIAL_OPEN, 0); }
		public TerminalNode SPECIAL_CLOSE() { return getToken(XMLParser.SPECIAL_CLOSE, 0); }
		public List<TerminalNode> PI_TEXT() { return getTokens(XMLParser.PI_TEXT); }
		public TerminalNode PI_TEXT(int i) {
			return getToken(XMLParser.PI_TEXT, i);
		}
		public ProcessinginstructionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_processinginstruction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).enterProcessinginstruction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).exitProcessinginstruction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XMLParserVisitor ) return ((XMLParserVisitor<? extends T>)visitor).visitProcessinginstruction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ProcessinginstructionContext processinginstruction() throws RecognitionException {
		ProcessinginstructionContext _localctx = new ProcessinginstructionContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_processinginstruction);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(126);
			match(SPECIAL_OPEN);
			setState(128);
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(127);
				match(PI_TEXT);
				}
				}
				setState(130);
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==PI_TEXT );
			setState(132);
			match(SPECIAL_CLOSE);
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
	public static class ContentContext extends ParserRuleContext {
		public ElementContext element() {
			return getRuleContext(ElementContext.class,0);
		}
		public ReferenceContext reference() {
			return getRuleContext(ReferenceContext.class,0);
		}
		public ProcessinginstructionContext processinginstruction() {
			return getRuleContext(ProcessinginstructionContext.class,0);
		}
		public TerminalNode CDATA() { return getToken(XMLParser.CDATA, 0); }
		public TerminalNode COMMENT() { return getToken(XMLParser.COMMENT, 0); }
		public JspscriptletContext jspscriptlet() {
			return getRuleContext(JspscriptletContext.class,0);
		}
		public JspexpressionContext jspexpression() {
			return getRuleContext(JspexpressionContext.class,0);
		}
		public JspdeclarationContext jspdeclaration() {
			return getRuleContext(JspdeclarationContext.class,0);
		}
		public JspcommentContext jspcomment() {
			return getRuleContext(JspcommentContext.class,0);
		}
		public ChardataContext chardata() {
			return getRuleContext(ChardataContext.class,0);
		}
		public ContentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_content; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).enterContent(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).exitContent(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XMLParserVisitor ) return ((XMLParserVisitor<? extends T>)visitor).visitContent(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ContentContext content() throws RecognitionException {
		ContentContext _localctx = new ContentContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_content);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(144);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case OPEN:
				{
				setState(134);
				element();
				}
				break;
			case EntityRef:
			case CharRef:
				{
				setState(135);
				reference();
				}
				break;
			case SPECIAL_OPEN:
				{
				setState(136);
				processinginstruction();
				}
				break;
			case CDATA:
				{
				setState(137);
				match(CDATA);
				}
				break;
			case COMMENT:
				{
				setState(138);
				match(COMMENT);
				}
				break;
			case JSP_SCRIPTLET:
				{
				setState(139);
				jspscriptlet();
				}
				break;
			case JSP_EXPRESSION:
				{
				setState(140);
				jspexpression();
				}
				break;
			case JSP_DECLARATION:
				{
				setState(141);
				jspdeclaration();
				}
				break;
			case JSP_COMMENT:
				{
				setState(142);
				jspcomment();
				}
				break;
			case SEA_WS:
			case QUESTION_MARK:
			case TEXT:
				{
				setState(143);
				chardata();
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
	public static class ElementContext extends ParserRuleContext {
		public List<TerminalNode> OPEN() { return getTokens(XMLParser.OPEN); }
		public TerminalNode OPEN(int i) {
			return getToken(XMLParser.OPEN, i);
		}
		public List<TerminalNode> Name() { return getTokens(XMLParser.Name); }
		public TerminalNode Name(int i) {
			return getToken(XMLParser.Name, i);
		}
		public List<TerminalNode> CLOSE() { return getTokens(XMLParser.CLOSE); }
		public TerminalNode CLOSE(int i) {
			return getToken(XMLParser.CLOSE, i);
		}
		public TerminalNode SLASH() { return getToken(XMLParser.SLASH, 0); }
		public List<AttributeContext> attribute() {
			return getRuleContexts(AttributeContext.class);
		}
		public AttributeContext attribute(int i) {
			return getRuleContext(AttributeContext.class,i);
		}
		public List<ContentContext> content() {
			return getRuleContexts(ContentContext.class);
		}
		public ContentContext content(int i) {
			return getRuleContext(ContentContext.class,i);
		}
		public TerminalNode SLASH_CLOSE() { return getToken(XMLParser.SLASH_CLOSE, 0); }
		public ElementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_element; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).enterElement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).exitElement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XMLParserVisitor ) return ((XMLParserVisitor<? extends T>)visitor).visitElement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ElementContext element() throws RecognitionException {
		ElementContext _localctx = new ElementContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_element);
		int _la;
		try {
			int _alt;
			setState(174);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,21,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(146);
				match(OPEN);
				setState(147);
				match(Name);
				setState(151);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==Name) {
					{
					{
					setState(148);
					attribute();
					}
					}
					setState(153);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(154);
				match(CLOSE);
				setState(158);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,19,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(155);
						content();
						}
						}
					}
					setState(160);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,19,_ctx);
				}
				setState(161);
				match(OPEN);
				setState(162);
				match(SLASH);
				setState(163);
				match(Name);
				setState(164);
				match(CLOSE);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(165);
				match(OPEN);
				setState(166);
				match(Name);
				setState(170);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==Name) {
					{
					{
					setState(167);
					attribute();
					}
					}
					setState(172);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(173);
				match(SLASH_CLOSE);
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
	public static class JspdirectiveContext extends ParserRuleContext {
		public TerminalNode OPEN() { return getToken(XMLParser.OPEN, 0); }
		public TerminalNode DIRECTIVE_OPEN() { return getToken(XMLParser.DIRECTIVE_OPEN, 0); }
		public TerminalNode Name() { return getToken(XMLParser.Name, 0); }
		public TerminalNode DIRECTIVE_CLOSE() { return getToken(XMLParser.DIRECTIVE_CLOSE, 0); }
		public TerminalNode CLOSE() { return getToken(XMLParser.CLOSE, 0); }
		public List<AttributeContext> attribute() {
			return getRuleContexts(AttributeContext.class);
		}
		public AttributeContext attribute(int i) {
			return getRuleContext(AttributeContext.class,i);
		}
		public JspdirectiveContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_jspdirective; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).enterJspdirective(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).exitJspdirective(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XMLParserVisitor ) return ((XMLParserVisitor<? extends T>)visitor).visitJspdirective(this);
			else return visitor.visitChildren(this);
		}
	}

	public final JspdirectiveContext jspdirective() throws RecognitionException {
		JspdirectiveContext _localctx = new JspdirectiveContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_jspdirective);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(176);
			match(OPEN);
			setState(177);
			match(DIRECTIVE_OPEN);
			setState(178);
			match(Name);
			setState(182);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==Name) {
				{
				{
				setState(179);
				attribute();
				}
				}
				setState(184);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(185);
			match(DIRECTIVE_CLOSE);
			setState(186);
			match(CLOSE);
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
	public static class JspscriptletContext extends ParserRuleContext {
		public TerminalNode JSP_SCRIPTLET() { return getToken(XMLParser.JSP_SCRIPTLET, 0); }
		public JspscriptletContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_jspscriptlet; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).enterJspscriptlet(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).exitJspscriptlet(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XMLParserVisitor ) return ((XMLParserVisitor<? extends T>)visitor).visitJspscriptlet(this);
			else return visitor.visitChildren(this);
		}
	}

	public final JspscriptletContext jspscriptlet() throws RecognitionException {
		JspscriptletContext _localctx = new JspscriptletContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_jspscriptlet);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(188);
			match(JSP_SCRIPTLET);
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
	public static class JspexpressionContext extends ParserRuleContext {
		public TerminalNode JSP_EXPRESSION() { return getToken(XMLParser.JSP_EXPRESSION, 0); }
		public JspexpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_jspexpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).enterJspexpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).exitJspexpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XMLParserVisitor ) return ((XMLParserVisitor<? extends T>)visitor).visitJspexpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final JspexpressionContext jspexpression() throws RecognitionException {
		JspexpressionContext _localctx = new JspexpressionContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_jspexpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(190);
			match(JSP_EXPRESSION);
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
	public static class JspdeclarationContext extends ParserRuleContext {
		public TerminalNode JSP_DECLARATION() { return getToken(XMLParser.JSP_DECLARATION, 0); }
		public JspdeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_jspdeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).enterJspdeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).exitJspdeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XMLParserVisitor ) return ((XMLParserVisitor<? extends T>)visitor).visitJspdeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final JspdeclarationContext jspdeclaration() throws RecognitionException {
		JspdeclarationContext _localctx = new JspdeclarationContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_jspdeclaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(192);
			match(JSP_DECLARATION);
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
	public static class JspcommentContext extends ParserRuleContext {
		public TerminalNode JSP_COMMENT() { return getToken(XMLParser.JSP_COMMENT, 0); }
		public JspcommentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_jspcomment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).enterJspcomment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).exitJspcomment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XMLParserVisitor ) return ((XMLParserVisitor<? extends T>)visitor).visitJspcomment(this);
			else return visitor.visitChildren(this);
		}
	}

	public final JspcommentContext jspcomment() throws RecognitionException {
		JspcommentContext _localctx = new JspcommentContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_jspcomment);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(194);
			match(JSP_COMMENT);
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
	public static class ReferenceContext extends ParserRuleContext {
		public TerminalNode EntityRef() { return getToken(XMLParser.EntityRef, 0); }
		public TerminalNode CharRef() { return getToken(XMLParser.CharRef, 0); }
		public ReferenceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_reference; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).enterReference(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).exitReference(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XMLParserVisitor ) return ((XMLParserVisitor<? extends T>)visitor).visitReference(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ReferenceContext reference() throws RecognitionException {
		ReferenceContext _localctx = new ReferenceContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_reference);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(196);
			_la = _input.LA(1);
			if ( !(_la==EntityRef || _la==CharRef) ) {
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
	public static class AttributeContext extends ParserRuleContext {
		public TerminalNode Name() { return getToken(XMLParser.Name, 0); }
		public TerminalNode EQUALS() { return getToken(XMLParser.EQUALS, 0); }
		public TerminalNode STRING() { return getToken(XMLParser.STRING, 0); }
		public AttributeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_attribute; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).enterAttribute(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).exitAttribute(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XMLParserVisitor ) return ((XMLParserVisitor<? extends T>)visitor).visitAttribute(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AttributeContext attribute() throws RecognitionException {
		AttributeContext _localctx = new AttributeContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_attribute);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(198);
			match(Name);
			setState(199);
			match(EQUALS);
			setState(200);
			match(STRING);
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
	public static class ChardataContext extends ParserRuleContext {
		public TerminalNode TEXT() { return getToken(XMLParser.TEXT, 0); }
		public TerminalNode QUESTION_MARK() { return getToken(XMLParser.QUESTION_MARK, 0); }
		public TerminalNode SEA_WS() { return getToken(XMLParser.SEA_WS, 0); }
		public ChardataContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_chardata; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).enterChardata(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).exitChardata(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XMLParserVisitor ) return ((XMLParserVisitor<? extends T>)visitor).visitChardata(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ChardataContext chardata() throws RecognitionException {
		ChardataContext _localctx = new ChardataContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_chardata);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(202);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 262784L) != 0)) ) {
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

	public static final String _serializedATN =
		"\u0004\u0001)\u00cd\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002"+
		"\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b\u0002"+
		"\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007\u000f"+
		"\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007\u0012"+
		"\u0002\u0013\u0007\u0013\u0001\u0000\u0003\u0000*\b\u0000\u0001\u0000"+
		"\u0001\u0000\u0003\u0000.\b\u0000\u0001\u0001\u0003\u00011\b\u0001\u0001"+
		"\u0001\u0005\u00014\b\u0001\n\u0001\f\u00017\t\u0001\u0001\u0001\u0005"+
		"\u0001:\b\u0001\n\u0001\f\u0001=\t\u0001\u0001\u0002\u0001\u0002\u0005"+
		"\u0002A\b\u0002\n\u0002\f\u0002D\t\u0002\u0001\u0002\u0001\u0002\u0001"+
		"\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0003\u0003M\b"+
		"\u0003\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0005"+
		"\u0004T\b\u0004\n\u0004\f\u0004W\t\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0003\u0004]\b\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0005\u0001\u0005\u0005\u0005c\b\u0005\n\u0005\f\u0005f\t\u0005\u0001"+
		"\u0006\u0001\u0006\u0003\u0006j\b\u0006\u0001\u0006\u0005\u0006m\b\u0006"+
		"\n\u0006\f\u0006p\t\u0006\u0001\u0006\u0003\u0006s\b\u0006\u0001\u0006"+
		"\u0001\u0006\u0001\u0006\u0003\u0006x\b\u0006\u0001\u0007\u0001\u0007"+
		"\u0001\b\u0003\b}\b\b\u0001\t\u0001\t\u0004\t\u0081\b\t\u000b\t\f\t\u0082"+
		"\u0001\t\u0001\t\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001"+
		"\n\u0001\n\u0001\n\u0001\n\u0003\n\u0091\b\n\u0001\u000b\u0001\u000b\u0001"+
		"\u000b\u0005\u000b\u0096\b\u000b\n\u000b\f\u000b\u0099\t\u000b\u0001\u000b"+
		"\u0001\u000b\u0005\u000b\u009d\b\u000b\n\u000b\f\u000b\u00a0\t\u000b\u0001"+
		"\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001"+
		"\u000b\u0005\u000b\u00a9\b\u000b\n\u000b\f\u000b\u00ac\t\u000b\u0001\u000b"+
		"\u0003\u000b\u00af\b\u000b\u0001\f\u0001\f\u0001\f\u0001\f\u0005\f\u00b5"+
		"\b\f\n\f\f\f\u00b8\t\f\u0001\f\u0001\f\u0001\f\u0001\r\u0001\r\u0001\u000e"+
		"\u0001\u000e\u0001\u000f\u0001\u000f\u0001\u0010\u0001\u0010\u0001\u0011"+
		"\u0001\u0011\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0013"+
		"\u0001\u0013\u0001\u0013\u0000\u0000\u0014\u0000\u0002\u0004\u0006\b\n"+
		"\f\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u001c\u001e \"$&\u0000\u0002"+
		"\u0001\u0000\u0005\u0006\u0003\u0000\u0007\u0007\t\t\u0012\u0012\u00db"+
		"\u0000)\u0001\u0000\u0000\u0000\u00020\u0001\u0000\u0000\u0000\u0004>"+
		"\u0001\u0000\u0000\u0000\u0006L\u0001\u0000\u0000\u0000\bN\u0001\u0000"+
		"\u0000\u0000\nd\u0001\u0000\u0000\u0000\fw\u0001\u0000\u0000\u0000\u000e"+
		"y\u0001\u0000\u0000\u0000\u0010|\u0001\u0000\u0000\u0000\u0012~\u0001"+
		"\u0000\u0000\u0000\u0014\u0090\u0001\u0000\u0000\u0000\u0016\u00ae\u0001"+
		"\u0000\u0000\u0000\u0018\u00b0\u0001\u0000\u0000\u0000\u001a\u00bc\u0001"+
		"\u0000\u0000\u0000\u001c\u00be\u0001\u0000\u0000\u0000\u001e\u00c0\u0001"+
		"\u0000\u0000\u0000 \u00c2\u0001\u0000\u0000\u0000\"\u00c4\u0001\u0000"+
		"\u0000\u0000$\u00c6\u0001\u0000\u0000\u0000&\u00ca\u0001\u0000\u0000\u0000"+
		"(*\u0005\b\u0000\u0000)(\u0001\u0000\u0000\u0000)*\u0001\u0000\u0000\u0000"+
		"*+\u0001\u0000\u0000\u0000+-\u0003\u0002\u0001\u0000,.\u0003\u0016\u000b"+
		"\u0000-,\u0001\u0000\u0000\u0000-.\u0001\u0000\u0000\u0000.\u0001\u0001"+
		"\u0000\u0000\u0000/1\u0003\u0004\u0002\u00000/\u0001\u0000\u0000\u0000"+
		"01\u0001\u0000\u0000\u000015\u0001\u0000\u0000\u000024\u0003\u0006\u0003"+
		"\u000032\u0001\u0000\u0000\u000047\u0001\u0000\u0000\u000053\u0001\u0000"+
		"\u0000\u000056\u0001\u0000\u0000\u00006;\u0001\u0000\u0000\u000075\u0001"+
		"\u0000\u0000\u00008:\u0003\u0018\f\u000098\u0001\u0000\u0000\u0000:=\u0001"+
		"\u0000\u0000\u0000;9\u0001\u0000\u0000\u0000;<\u0001\u0000\u0000\u0000"+
		"<\u0003\u0001\u0000\u0000\u0000=;\u0001\u0000\u0000\u0000>B\u0005\n\u0000"+
		"\u0000?A\u0003$\u0012\u0000@?\u0001\u0000\u0000\u0000AD\u0001\u0000\u0000"+
		"\u0000B@\u0001\u0000\u0000\u0000BC\u0001\u0000\u0000\u0000CE\u0001\u0000"+
		"\u0000\u0000DB\u0001\u0000\u0000\u0000EF\u0005!\u0000\u0000F\u0005\u0001"+
		"\u0000\u0000\u0000GM\u0005\u0002\u0000\u0000HM\u0003\b\u0004\u0000IM\u0003"+
		"\u0012\t\u0000JM\u0003\u001e\u000f\u0000KM\u0003 \u0010\u0000LG\u0001"+
		"\u0000\u0000\u0000LH\u0001\u0000\u0000\u0000LI\u0001\u0000\u0000\u0000"+
		"LJ\u0001\u0000\u0000\u0000LK\u0001\u0000\u0000\u0000M\u0007\u0001\u0000"+
		"\u0000\u0000NO\u0005\r\u0000\u0000OP\u0005\u0016\u0000\u0000PQ\u0005)"+
		"\u0000\u0000QU\u0003\u0010\b\u0000RT\u0005(\u0000\u0000SR\u0001\u0000"+
		"\u0000\u0000TW\u0001\u0000\u0000\u0000US\u0001\u0000\u0000\u0000UV\u0001"+
		"\u0000\u0000\u0000V\\\u0001\u0000\u0000\u0000WU\u0001\u0000\u0000\u0000"+
		"XY\u0005\u0014\u0000\u0000YZ\u0003\n\u0005\u0000Z[\u0005\u0017\u0000\u0000"+
		"[]\u0001\u0000\u0000\u0000\\X\u0001\u0000\u0000\u0000\\]\u0001\u0000\u0000"+
		"\u0000]^\u0001\u0000\u0000\u0000^_\u0005\u0013\u0000\u0000_\t\u0001\u0000"+
		"\u0000\u0000`c\u0003\f\u0006\u0000ac\u0003\u000e\u0007\u0000b`\u0001\u0000"+
		"\u0000\u0000ba\u0001\u0000\u0000\u0000cf\u0001\u0000\u0000\u0000db\u0001"+
		"\u0000\u0000\u0000de\u0001\u0000\u0000\u0000e\u000b\u0001\u0000\u0000"+
		"\u0000fd\u0001\u0000\u0000\u0000gi\u0005\u0018\u0000\u0000hj\u0005\u001c"+
		"\u0000\u0000ih\u0001\u0000\u0000\u0000ij\u0001\u0000\u0000\u0000jn\u0001"+
		"\u0000\u0000\u0000km\u0005\u001d\u0000\u0000lk\u0001\u0000\u0000\u0000"+
		"mp\u0001\u0000\u0000\u0000nl\u0001\u0000\u0000\u0000no\u0001\u0000\u0000"+
		"\u0000or\u0001\u0000\u0000\u0000pn\u0001\u0000\u0000\u0000qs\u0005\u001c"+
		"\u0000\u0000rq\u0001\u0000\u0000\u0000rs\u0001\u0000\u0000\u0000st\u0001"+
		"\u0000\u0000\u0000tx\u0005\u001a\u0000\u0000ux\u0003\u0012\t\u0000vx\u0005"+
		"\u0002\u0000\u0000wg\u0001\u0000\u0000\u0000wu\u0001\u0000\u0000\u0000"+
		"wv\u0001\u0000\u0000\u0000x\r\u0001\u0000\u0000\u0000yz\u0005\u0004\u0000"+
		"\u0000z\u000f\u0001\u0000\u0000\u0000{}\u0005)\u0000\u0000|{\u0001\u0000"+
		"\u0000\u0000|}\u0001\u0000\u0000\u0000}\u0011\u0001\u0000\u0000\u0000"+
		"~\u0080\u0005\f\u0000\u0000\u007f\u0081\u0005\u001f\u0000\u0000\u0080"+
		"\u007f\u0001\u0000\u0000\u0000\u0081\u0082\u0001\u0000\u0000\u0000\u0082"+
		"\u0080\u0001\u0000\u0000\u0000\u0082\u0083\u0001\u0000\u0000\u0000\u0083"+
		"\u0084\u0001\u0000\u0000\u0000\u0084\u0085\u0005!\u0000\u0000\u0085\u0013"+
		"\u0001\u0000\u0000\u0000\u0086\u0091\u0003\u0016\u000b\u0000\u0087\u0091"+
		"\u0003\"\u0011\u0000\u0088\u0091\u0003\u0012\t\u0000\u0089\u0091\u0005"+
		"\u0003\u0000\u0000\u008a\u0091\u0005\u0002\u0000\u0000\u008b\u0091\u0003"+
		"\u001a\r\u0000\u008c\u0091\u0003\u001c\u000e\u0000\u008d\u0091\u0003\u001e"+
		"\u000f\u0000\u008e\u0091\u0003 \u0010\u0000\u008f\u0091\u0003&\u0013\u0000"+
		"\u0090\u0086\u0001\u0000\u0000\u0000\u0090\u0087\u0001\u0000\u0000\u0000"+
		"\u0090\u0088\u0001\u0000\u0000\u0000\u0090\u0089\u0001\u0000\u0000\u0000"+
		"\u0090\u008a\u0001\u0000\u0000\u0000\u0090\u008b\u0001\u0000\u0000\u0000"+
		"\u0090\u008c\u0001\u0000\u0000\u0000\u0090\u008d\u0001\u0000\u0000\u0000"+
		"\u0090\u008e\u0001\u0000\u0000\u0000\u0090\u008f\u0001\u0000\u0000\u0000"+
		"\u0091\u0015\u0001\u0000\u0000\u0000\u0092\u0093\u0005\u000b\u0000\u0000"+
		"\u0093\u0097\u0005)\u0000\u0000\u0094\u0096\u0003$\u0012\u0000\u0095\u0094"+
		"\u0001\u0000\u0000\u0000\u0096\u0099\u0001\u0000\u0000\u0000\u0097\u0095"+
		"\u0001\u0000\u0000\u0000\u0097\u0098\u0001\u0000\u0000\u0000\u0098\u009a"+
		"\u0001\u0000\u0000\u0000\u0099\u0097\u0001\u0000\u0000\u0000\u009a\u009e"+
		"\u0005 \u0000\u0000\u009b\u009d\u0003\u0014\n\u0000\u009c\u009b\u0001"+
		"\u0000\u0000\u0000\u009d\u00a0\u0001\u0000\u0000\u0000\u009e\u009c\u0001"+
		"\u0000\u0000\u0000\u009e\u009f\u0001\u0000\u0000\u0000\u009f\u00a1\u0001"+
		"\u0000\u0000\u0000\u00a0\u009e\u0001\u0000\u0000\u0000\u00a1\u00a2\u0005"+
		"\u000b\u0000\u0000\u00a2\u00a3\u0005&\u0000\u0000\u00a3\u00a4\u0005)\u0000"+
		"\u0000\u00a4\u00af\u0005 \u0000\u0000\u00a5\u00a6\u0005\u000b\u0000\u0000"+
		"\u00a6\u00aa\u0005)\u0000\u0000\u00a7\u00a9\u0003$\u0012\u0000\u00a8\u00a7"+
		"\u0001\u0000\u0000\u0000\u00a9\u00ac\u0001\u0000\u0000\u0000\u00aa\u00a8"+
		"\u0001\u0000\u0000\u0000\u00aa\u00ab\u0001\u0000\u0000\u0000\u00ab\u00ad"+
		"\u0001\u0000\u0000\u0000\u00ac\u00aa\u0001\u0000\u0000\u0000\u00ad\u00af"+
		"\u0005\"\u0000\u0000\u00ae\u0092\u0001\u0000\u0000\u0000\u00ae\u00a5\u0001"+
		"\u0000\u0000\u0000\u00af\u0017\u0001\u0000\u0000\u0000\u00b0\u00b1\u0005"+
		"\u000b\u0000\u0000\u00b1\u00b2\u0005$\u0000\u0000\u00b2\u00b6\u0005)\u0000"+
		"\u0000\u00b3\u00b5\u0003$\u0012\u0000\u00b4\u00b3\u0001\u0000\u0000\u0000"+
		"\u00b5\u00b8\u0001\u0000\u0000\u0000\u00b6\u00b4\u0001\u0000\u0000\u0000"+
		"\u00b6\u00b7\u0001\u0000\u0000\u0000\u00b7\u00b9\u0001\u0000\u0000\u0000"+
		"\u00b8\u00b6\u0001\u0000\u0000\u0000\u00b9\u00ba\u0005%\u0000\u0000\u00ba"+
		"\u00bb\u0005 \u0000\u0000\u00bb\u0019\u0001\u0000\u0000\u0000\u00bc\u00bd"+
		"\u0005\u0011\u0000\u0000\u00bd\u001b\u0001\u0000\u0000\u0000\u00be\u00bf"+
		"\u0005\u0010\u0000\u0000\u00bf\u001d\u0001\u0000\u0000\u0000\u00c0\u00c1"+
		"\u0005\u000f\u0000\u0000\u00c1\u001f\u0001\u0000\u0000\u0000\u00c2\u00c3"+
		"\u0005\u000e\u0000\u0000\u00c3!\u0001\u0000\u0000\u0000\u00c4\u00c5\u0007"+
		"\u0000\u0000\u0000\u00c5#\u0001\u0000\u0000\u0000\u00c6\u00c7\u0005)\u0000"+
		"\u0000\u00c7\u00c8\u0005\'\u0000\u0000\u00c8\u00c9\u0005(\u0000\u0000"+
		"\u00c9%\u0001\u0000\u0000\u0000\u00ca\u00cb\u0007\u0001\u0000\u0000\u00cb"+
		"\'\u0001\u0000\u0000\u0000\u0017)-05;BLU\\bdinrw|\u0082\u0090\u0097\u009e"+
		"\u00aa\u00ae\u00b6";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
