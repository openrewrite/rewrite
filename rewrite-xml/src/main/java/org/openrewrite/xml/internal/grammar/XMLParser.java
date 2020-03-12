// Generated from /Users/jon/Projects/github/Netflix-Skunkworks/rewrite/rewrite-xml/src/main/antlr/XMLParser.g4 by ANTLR 4.8
package org.openrewrite.xml.internal.grammar;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class XMLParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.8", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		COMMENT=1, CDATA=2, EntityRef=3, CharRef=4, SEA_WS=5, OPEN=6, SPECIAL_OPEN=7, 
		ELEMENT_OPEN=8, TEXT=9, CLOSE=10, SPECIAL_CLOSE=11, SLASH_CLOSE=12, SLASH=13, 
		SUBSET_OPEN=14, SUBSET_CLOSE=15, EQUALS=16, XML_DECL=17, DOCTYPE=18, STRING=19, 
		Name=20, S=21;
	public static final int
		RULE_document = 0, RULE_prolog = 1, RULE_xmldecl = 2, RULE_misc = 3, RULE_doctypedecl = 4, 
		RULE_intsubset = 5, RULE_externalid = 6, RULE_processinginstruction = 7, 
		RULE_content = 8, RULE_element = 9, RULE_reference = 10, RULE_attribute = 11, 
		RULE_chardata = 12;
	private static String[] makeRuleNames() {
		return new String[] {
			"document", "prolog", "xmldecl", "misc", "doctypedecl", "intsubset", 
			"externalid", "processinginstruction", "content", "element", "reference", 
			"attribute", "chardata"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, null, null, null, "'<'", "'<?'", "'<!'", null, "'>'", 
			"'?>'", "'/>'", "'/'", "'['", "']'", "'='", "'xml'", "'DOCTYPE'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "COMMENT", "CDATA", "EntityRef", "CharRef", "SEA_WS", "OPEN", "SPECIAL_OPEN", 
			"ELEMENT_OPEN", "TEXT", "CLOSE", "SPECIAL_CLOSE", "SLASH_CLOSE", "SLASH", 
			"SUBSET_OPEN", "SUBSET_CLOSE", "EQUALS", "XML_DECL", "DOCTYPE", "STRING", 
			"Name", "S"
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

	public static class DocumentContext extends ParserRuleContext {
		public ElementContext element() {
			return getRuleContext(ElementContext.class,0);
		}
		public PrologContext prolog() {
			return getRuleContext(PrologContext.class,0);
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
			setState(27);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SPECIAL_OPEN) {
				{
				setState(26);
				prolog();
				}
			}

			setState(29);
			element();
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
			enterOuterAlt(_localctx, 1);
			{
			setState(31);
			xmldecl();
			setState(35);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << COMMENT) | (1L << SPECIAL_OPEN) | (1L << ELEMENT_OPEN))) != 0)) {
				{
				{
				setState(32);
				misc();
				}
				}
				setState(37);
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

	public static class XmldeclContext extends ParserRuleContext {
		public TerminalNode SPECIAL_OPEN() { return getToken(XMLParser.SPECIAL_OPEN, 0); }
		public TerminalNode XML_DECL() { return getToken(XMLParser.XML_DECL, 0); }
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
			setState(38);
			match(SPECIAL_OPEN);
			setState(39);
			match(XML_DECL);
			setState(43);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==Name) {
				{
				{
				setState(40);
				attribute();
				}
				}
				setState(45);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(46);
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

	public static class MiscContext extends ParserRuleContext {
		public TerminalNode COMMENT() { return getToken(XMLParser.COMMENT, 0); }
		public DoctypedeclContext doctypedecl() {
			return getRuleContext(DoctypedeclContext.class,0);
		}
		public ProcessinginstructionContext processinginstruction() {
			return getRuleContext(ProcessinginstructionContext.class,0);
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
			setState(51);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case COMMENT:
				enterOuterAlt(_localctx, 1);
				{
				setState(48);
				match(COMMENT);
				}
				break;
			case ELEMENT_OPEN:
				enterOuterAlt(_localctx, 2);
				{
				setState(49);
				doctypedecl();
				}
				break;
			case SPECIAL_OPEN:
				enterOuterAlt(_localctx, 3);
				{
				setState(50);
				processinginstruction();
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

	public static class DoctypedeclContext extends ParserRuleContext {
		public TerminalNode ELEMENT_OPEN() { return getToken(XMLParser.ELEMENT_OPEN, 0); }
		public TerminalNode DOCTYPE() { return getToken(XMLParser.DOCTYPE, 0); }
		public TerminalNode Name() { return getToken(XMLParser.Name, 0); }
		public ExternalidContext externalid() {
			return getRuleContext(ExternalidContext.class,0);
		}
		public TerminalNode CLOSE() { return getToken(XMLParser.CLOSE, 0); }
		public List<TerminalNode> STRING() { return getTokens(XMLParser.STRING); }
		public TerminalNode STRING(int i) {
			return getToken(XMLParser.STRING, i);
		}
		public IntsubsetContext intsubset() {
			return getRuleContext(IntsubsetContext.class,0);
		}
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
			setState(53);
			match(ELEMENT_OPEN);
			setState(54);
			match(DOCTYPE);
			setState(55);
			match(Name);
			setState(56);
			externalid();
			setState(60);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==STRING) {
				{
				{
				setState(57);
				match(STRING);
				}
				}
				setState(62);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(64);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SUBSET_OPEN) {
				{
				setState(63);
				intsubset();
				}
			}

			setState(66);
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

	public static class IntsubsetContext extends ParserRuleContext {
		public TerminalNode SUBSET_OPEN() { return getToken(XMLParser.SUBSET_OPEN, 0); }
		public TerminalNode SUBSET_CLOSE() { return getToken(XMLParser.SUBSET_CLOSE, 0); }
		public List<TerminalNode> STRING() { return getTokens(XMLParser.STRING); }
		public TerminalNode STRING(int i) {
			return getToken(XMLParser.STRING, i);
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
			setState(68);
			match(SUBSET_OPEN);
			setState(72);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==STRING) {
				{
				{
				setState(69);
				match(STRING);
				}
				}
				setState(74);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(75);
			match(SUBSET_CLOSE);
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
		enterRule(_localctx, 12, RULE_externalid);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(78);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==Name) {
				{
				setState(77);
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

	public static class ProcessinginstructionContext extends ParserRuleContext {
		public TerminalNode SPECIAL_OPEN() { return getToken(XMLParser.SPECIAL_OPEN, 0); }
		public TerminalNode Name() { return getToken(XMLParser.Name, 0); }
		public TerminalNode SPECIAL_CLOSE() { return getToken(XMLParser.SPECIAL_CLOSE, 0); }
		public List<AttributeContext> attribute() {
			return getRuleContexts(AttributeContext.class);
		}
		public AttributeContext attribute(int i) {
			return getRuleContext(AttributeContext.class,i);
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
		enterRule(_localctx, 14, RULE_processinginstruction);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(80);
			match(SPECIAL_OPEN);
			setState(81);
			match(Name);
			setState(85);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==Name) {
				{
				{
				setState(82);
				attribute();
				}
				}
				setState(87);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(88);
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

	public static class ContentContext extends ParserRuleContext {
		public ElementContext element() {
			return getRuleContext(ElementContext.class,0);
		}
		public ReferenceContext reference() {
			return getRuleContext(ReferenceContext.class,0);
		}
		public TerminalNode CDATA() { return getToken(XMLParser.CDATA, 0); }
		public TerminalNode COMMENT() { return getToken(XMLParser.COMMENT, 0); }
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
		enterRule(_localctx, 16, RULE_content);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(95);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case OPEN:
				{
				setState(90);
				element();
				}
				break;
			case EntityRef:
			case CharRef:
				{
				setState(91);
				reference();
				}
				break;
			case CDATA:
				{
				setState(92);
				match(CDATA);
				}
				break;
			case COMMENT:
				{
				setState(93);
				match(COMMENT);
				}
				break;
			case SEA_WS:
			case TEXT:
				{
				setState(94);
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
		enterRule(_localctx, 18, RULE_element);
		int _la;
		try {
			int _alt;
			setState(125);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,13,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(97);
				match(OPEN);
				setState(98);
				match(Name);
				setState(102);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==Name) {
					{
					{
					setState(99);
					attribute();
					}
					}
					setState(104);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(105);
				match(CLOSE);
				setState(109);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,11,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(106);
						content();
						}
						} 
					}
					setState(111);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,11,_ctx);
				}
				setState(112);
				match(OPEN);
				setState(113);
				match(SLASH);
				setState(114);
				match(Name);
				setState(115);
				match(CLOSE);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(116);
				match(OPEN);
				setState(117);
				match(Name);
				setState(121);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==Name) {
					{
					{
					setState(118);
					attribute();
					}
					}
					setState(123);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(124);
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
		enterRule(_localctx, 20, RULE_reference);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(127);
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
		enterRule(_localctx, 22, RULE_attribute);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(129);
			match(Name);
			setState(130);
			match(EQUALS);
			setState(131);
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

	public static class ChardataContext extends ParserRuleContext {
		public TerminalNode TEXT() { return getToken(XMLParser.TEXT, 0); }
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
		enterRule(_localctx, 24, RULE_chardata);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(133);
			_la = _input.LA(1);
			if ( !(_la==SEA_WS || _la==TEXT) ) {
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\27\u008a\4\2\t\2"+
		"\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\3\2\5\2\36\n\2\3\2\3\2\3\3\3\3\7\3$\n"+
		"\3\f\3\16\3\'\13\3\3\4\3\4\3\4\7\4,\n\4\f\4\16\4/\13\4\3\4\3\4\3\5\3\5"+
		"\3\5\5\5\66\n\5\3\6\3\6\3\6\3\6\3\6\7\6=\n\6\f\6\16\6@\13\6\3\6\5\6C\n"+
		"\6\3\6\3\6\3\7\3\7\7\7I\n\7\f\7\16\7L\13\7\3\7\3\7\3\b\5\bQ\n\b\3\t\3"+
		"\t\3\t\7\tV\n\t\f\t\16\tY\13\t\3\t\3\t\3\n\3\n\3\n\3\n\3\n\5\nb\n\n\3"+
		"\13\3\13\3\13\7\13g\n\13\f\13\16\13j\13\13\3\13\3\13\7\13n\n\13\f\13\16"+
		"\13q\13\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\7\13z\n\13\f\13\16\13}\13"+
		"\13\3\13\5\13\u0080\n\13\3\f\3\f\3\r\3\r\3\r\3\r\3\16\3\16\3\16\2\2\17"+
		"\2\4\6\b\n\f\16\20\22\24\26\30\32\2\4\3\2\5\6\4\2\7\7\13\13\2\u008e\2"+
		"\35\3\2\2\2\4!\3\2\2\2\6(\3\2\2\2\b\65\3\2\2\2\n\67\3\2\2\2\fF\3\2\2\2"+
		"\16P\3\2\2\2\20R\3\2\2\2\22a\3\2\2\2\24\177\3\2\2\2\26\u0081\3\2\2\2\30"+
		"\u0083\3\2\2\2\32\u0087\3\2\2\2\34\36\5\4\3\2\35\34\3\2\2\2\35\36\3\2"+
		"\2\2\36\37\3\2\2\2\37 \5\24\13\2 \3\3\2\2\2!%\5\6\4\2\"$\5\b\5\2#\"\3"+
		"\2\2\2$\'\3\2\2\2%#\3\2\2\2%&\3\2\2\2&\5\3\2\2\2\'%\3\2\2\2()\7\t\2\2"+
		")-\7\23\2\2*,\5\30\r\2+*\3\2\2\2,/\3\2\2\2-+\3\2\2\2-.\3\2\2\2.\60\3\2"+
		"\2\2/-\3\2\2\2\60\61\7\r\2\2\61\7\3\2\2\2\62\66\7\3\2\2\63\66\5\n\6\2"+
		"\64\66\5\20\t\2\65\62\3\2\2\2\65\63\3\2\2\2\65\64\3\2\2\2\66\t\3\2\2\2"+
		"\678\7\n\2\289\7\24\2\29:\7\26\2\2:>\5\16\b\2;=\7\25\2\2<;\3\2\2\2=@\3"+
		"\2\2\2><\3\2\2\2>?\3\2\2\2?B\3\2\2\2@>\3\2\2\2AC\5\f\7\2BA\3\2\2\2BC\3"+
		"\2\2\2CD\3\2\2\2DE\7\f\2\2E\13\3\2\2\2FJ\7\20\2\2GI\7\25\2\2HG\3\2\2\2"+
		"IL\3\2\2\2JH\3\2\2\2JK\3\2\2\2KM\3\2\2\2LJ\3\2\2\2MN\7\21\2\2N\r\3\2\2"+
		"\2OQ\7\26\2\2PO\3\2\2\2PQ\3\2\2\2Q\17\3\2\2\2RS\7\t\2\2SW\7\26\2\2TV\5"+
		"\30\r\2UT\3\2\2\2VY\3\2\2\2WU\3\2\2\2WX\3\2\2\2XZ\3\2\2\2YW\3\2\2\2Z["+
		"\7\r\2\2[\21\3\2\2\2\\b\5\24\13\2]b\5\26\f\2^b\7\4\2\2_b\7\3\2\2`b\5\32"+
		"\16\2a\\\3\2\2\2a]\3\2\2\2a^\3\2\2\2a_\3\2\2\2a`\3\2\2\2b\23\3\2\2\2c"+
		"d\7\b\2\2dh\7\26\2\2eg\5\30\r\2fe\3\2\2\2gj\3\2\2\2hf\3\2\2\2hi\3\2\2"+
		"\2ik\3\2\2\2jh\3\2\2\2ko\7\f\2\2ln\5\22\n\2ml\3\2\2\2nq\3\2\2\2om\3\2"+
		"\2\2op\3\2\2\2pr\3\2\2\2qo\3\2\2\2rs\7\b\2\2st\7\17\2\2tu\7\26\2\2u\u0080"+
		"\7\f\2\2vw\7\b\2\2w{\7\26\2\2xz\5\30\r\2yx\3\2\2\2z}\3\2\2\2{y\3\2\2\2"+
		"{|\3\2\2\2|~\3\2\2\2}{\3\2\2\2~\u0080\7\16\2\2\177c\3\2\2\2\177v\3\2\2"+
		"\2\u0080\25\3\2\2\2\u0081\u0082\t\2\2\2\u0082\27\3\2\2\2\u0083\u0084\7"+
		"\26\2\2\u0084\u0085\7\22\2\2\u0085\u0086\7\25\2\2\u0086\31\3\2\2\2\u0087"+
		"\u0088\t\3\2\2\u0088\33\3\2\2\2\20\35%-\65>BJPWaho{\177";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}