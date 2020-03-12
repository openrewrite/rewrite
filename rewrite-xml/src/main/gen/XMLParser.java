// Generated from /Users/jon/Projects/github/Netflix-Skunkworks/rewrite/rewrite-xml/src/main/antlr/XMLParser.g4 by ANTLR 4.8
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
		COMMENT_OPEN=1, COMMENT_CLOSE=2, CDATA_OPEN=3, CDATA_CLOSE=4, EntityRef=5, 
		CharRef=6, SEA_WS=7, OPEN=8, SPECIAL_OPEN=9, ELEMENT_OPEN=10, TEXT=11, 
		CLOSE=12, SPECIAL_CLOSE=13, SLASH_CLOSE=14, SLASH=15, SUBSET_OPEN=16, 
		SUBSET_CLOSE=17, EQUALS=18, XML_DECL=19, DOCTYPE=20, STRING=21, Name=22, 
		S=23;
	public static final int
		RULE_document = 0, RULE_prolog = 1, RULE_xmldecl = 2, RULE_misc = 3, RULE_doctypedecl = 4, 
		RULE_intsubset = 5, RULE_externalid = 6, RULE_processinginstruction = 7, 
		RULE_content = 8, RULE_element = 9, RULE_comment = 10, RULE_cdata = 11, 
		RULE_reference = 12, RULE_attribute = 13, RULE_chardata = 14;
	private static String[] makeRuleNames() {
		return new String[] {
			"document", "prolog", "xmldecl", "misc", "doctypedecl", "intsubset", 
			"externalid", "processinginstruction", "content", "element", "comment", 
			"cdata", "reference", "attribute", "chardata"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'<!--'", "'-->'", "'<![CDATA['", "']]>'", null, null, null, "'<'", 
			"'<?'", "'<!'", null, "'>'", "'?>'", "'/>'", "'/'", "'['", "']'", "'='", 
			"'xml'", "'DOCTYPE'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "COMMENT_OPEN", "COMMENT_CLOSE", "CDATA_OPEN", "CDATA_CLOSE", "EntityRef", 
			"CharRef", "SEA_WS", "OPEN", "SPECIAL_OPEN", "ELEMENT_OPEN", "TEXT", 
			"CLOSE", "SPECIAL_CLOSE", "SLASH_CLOSE", "SLASH", "SUBSET_OPEN", "SUBSET_CLOSE", 
			"EQUALS", "XML_DECL", "DOCTYPE", "STRING", "Name", "S"
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
			setState(31);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SPECIAL_OPEN) {
				{
				setState(30);
				prolog();
				}
			}

			setState(33);
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
			setState(35);
			xmldecl();
			setState(39);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << COMMENT_OPEN) | (1L << SPECIAL_OPEN) | (1L << ELEMENT_OPEN))) != 0)) {
				{
				{
				setState(36);
				misc();
				}
				}
				setState(41);
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
			setState(42);
			match(SPECIAL_OPEN);
			setState(43);
			match(XML_DECL);
			setState(47);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==Name) {
				{
				{
				setState(44);
				attribute();
				}
				}
				setState(49);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(50);
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
		public CommentContext comment() {
			return getRuleContext(CommentContext.class,0);
		}
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
			setState(55);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case COMMENT_OPEN:
				enterOuterAlt(_localctx, 1);
				{
				setState(52);
				comment();
				}
				break;
			case ELEMENT_OPEN:
				enterOuterAlt(_localctx, 2);
				{
				setState(53);
				doctypedecl();
				}
				break;
			case SPECIAL_OPEN:
				enterOuterAlt(_localctx, 3);
				{
				setState(54);
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
			setState(57);
			match(ELEMENT_OPEN);
			setState(58);
			match(DOCTYPE);
			setState(59);
			match(Name);
			setState(60);
			externalid();
			setState(64);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==STRING) {
				{
				{
				setState(61);
				match(STRING);
				}
				}
				setState(66);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(68);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SUBSET_OPEN) {
				{
				setState(67);
				intsubset();
				}
			}

			setState(70);
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
			setState(72);
			match(SUBSET_OPEN);
			setState(76);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==STRING) {
				{
				{
				setState(73);
				match(STRING);
				}
				}
				setState(78);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(79);
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
			setState(82);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==Name) {
				{
				setState(81);
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
			setState(84);
			match(SPECIAL_OPEN);
			setState(85);
			match(Name);
			setState(89);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==Name) {
				{
				{
				setState(86);
				attribute();
				}
				}
				setState(91);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(92);
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
		public List<ChardataContext> chardata() {
			return getRuleContexts(ChardataContext.class);
		}
		public ChardataContext chardata(int i) {
			return getRuleContext(ChardataContext.class,i);
		}
		public List<ElementContext> element() {
			return getRuleContexts(ElementContext.class);
		}
		public ElementContext element(int i) {
			return getRuleContext(ElementContext.class,i);
		}
		public List<ReferenceContext> reference() {
			return getRuleContexts(ReferenceContext.class);
		}
		public ReferenceContext reference(int i) {
			return getRuleContext(ReferenceContext.class,i);
		}
		public List<CdataContext> cdata() {
			return getRuleContexts(CdataContext.class);
		}
		public CdataContext cdata(int i) {
			return getRuleContext(CdataContext.class,i);
		}
		public List<CommentContext> comment() {
			return getRuleContexts(CommentContext.class);
		}
		public CommentContext comment(int i) {
			return getRuleContext(CommentContext.class,i);
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
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(95);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SEA_WS || _la==TEXT) {
				{
				setState(94);
				chardata();
				}
			}

			setState(108);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,12,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(101);
					_errHandler.sync(this);
					switch (_input.LA(1)) {
					case OPEN:
						{
						setState(97);
						element();
						}
						break;
					case EntityRef:
					case CharRef:
						{
						setState(98);
						reference();
						}
						break;
					case CDATA_OPEN:
						{
						setState(99);
						cdata();
						}
						break;
					case COMMENT_OPEN:
						{
						setState(100);
						comment();
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					setState(104);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==SEA_WS || _la==TEXT) {
						{
						setState(103);
						chardata();
						}
					}

					}
					} 
				}
				setState(110);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,12,_ctx);
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
		public ContentContext content() {
			return getRuleContext(ContentContext.class,0);
		}
		public TerminalNode SLASH() { return getToken(XMLParser.SLASH, 0); }
		public List<AttributeContext> attribute() {
			return getRuleContexts(AttributeContext.class);
		}
		public AttributeContext attribute(int i) {
			return getRuleContext(AttributeContext.class,i);
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
			setState(135);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,15,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(111);
				match(OPEN);
				setState(112);
				match(Name);
				setState(116);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==Name) {
					{
					{
					setState(113);
					attribute();
					}
					}
					setState(118);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(119);
				match(CLOSE);
				setState(120);
				content();
				setState(121);
				match(OPEN);
				setState(122);
				match(SLASH);
				setState(123);
				match(Name);
				setState(124);
				match(CLOSE);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(126);
				match(OPEN);
				setState(127);
				match(Name);
				setState(131);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==Name) {
					{
					{
					setState(128);
					attribute();
					}
					}
					setState(133);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(134);
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

	public static class CommentContext extends ParserRuleContext {
		public TerminalNode COMMENT_OPEN() { return getToken(XMLParser.COMMENT_OPEN, 0); }
		public TerminalNode TEXT() { return getToken(XMLParser.TEXT, 0); }
		public TerminalNode COMMENT_CLOSE() { return getToken(XMLParser.COMMENT_CLOSE, 0); }
		public CommentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_comment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).enterComment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).exitComment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XMLParserVisitor ) return ((XMLParserVisitor<? extends T>)visitor).visitComment(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CommentContext comment() throws RecognitionException {
		CommentContext _localctx = new CommentContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_comment);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(137);
			match(COMMENT_OPEN);
			setState(138);
			match(TEXT);
			setState(139);
			match(COMMENT_CLOSE);
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

	public static class CdataContext extends ParserRuleContext {
		public TerminalNode CDATA_OPEN() { return getToken(XMLParser.CDATA_OPEN, 0); }
		public TerminalNode TEXT() { return getToken(XMLParser.TEXT, 0); }
		public TerminalNode CDATA_CLOSE() { return getToken(XMLParser.CDATA_CLOSE, 0); }
		public CdataContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_cdata; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).enterCdata(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XMLParserListener ) ((XMLParserListener)listener).exitCdata(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XMLParserVisitor ) return ((XMLParserVisitor<? extends T>)visitor).visitCdata(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CdataContext cdata() throws RecognitionException {
		CdataContext _localctx = new CdataContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_cdata);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(141);
			match(CDATA_OPEN);
			setState(142);
			match(TEXT);
			setState(143);
			match(CDATA_CLOSE);
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
		enterRule(_localctx, 24, RULE_reference);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(145);
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
		enterRule(_localctx, 26, RULE_attribute);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(147);
			match(Name);
			setState(148);
			match(EQUALS);
			setState(149);
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
		enterRule(_localctx, 28, RULE_chardata);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(151);
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\31\u009c\4\2\t\2"+
		"\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\3\2\5\2\"\n\2\3\2"+
		"\3\2\3\3\3\3\7\3(\n\3\f\3\16\3+\13\3\3\4\3\4\3\4\7\4\60\n\4\f\4\16\4\63"+
		"\13\4\3\4\3\4\3\5\3\5\3\5\5\5:\n\5\3\6\3\6\3\6\3\6\3\6\7\6A\n\6\f\6\16"+
		"\6D\13\6\3\6\5\6G\n\6\3\6\3\6\3\7\3\7\7\7M\n\7\f\7\16\7P\13\7\3\7\3\7"+
		"\3\b\5\bU\n\b\3\t\3\t\3\t\7\tZ\n\t\f\t\16\t]\13\t\3\t\3\t\3\n\5\nb\n\n"+
		"\3\n\3\n\3\n\3\n\5\nh\n\n\3\n\5\nk\n\n\7\nm\n\n\f\n\16\np\13\n\3\13\3"+
		"\13\3\13\7\13u\n\13\f\13\16\13x\13\13\3\13\3\13\3\13\3\13\3\13\3\13\3"+
		"\13\3\13\3\13\3\13\7\13\u0084\n\13\f\13\16\13\u0087\13\13\3\13\5\13\u008a"+
		"\n\13\3\f\3\f\3\f\3\f\3\r\3\r\3\r\3\r\3\16\3\16\3\17\3\17\3\17\3\17\3"+
		"\20\3\20\3\20\2\2\21\2\4\6\b\n\f\16\20\22\24\26\30\32\34\36\2\4\3\2\7"+
		"\b\4\2\t\t\r\r\2\u009f\2!\3\2\2\2\4%\3\2\2\2\6,\3\2\2\2\b9\3\2\2\2\n;"+
		"\3\2\2\2\fJ\3\2\2\2\16T\3\2\2\2\20V\3\2\2\2\22a\3\2\2\2\24\u0089\3\2\2"+
		"\2\26\u008b\3\2\2\2\30\u008f\3\2\2\2\32\u0093\3\2\2\2\34\u0095\3\2\2\2"+
		"\36\u0099\3\2\2\2 \"\5\4\3\2! \3\2\2\2!\"\3\2\2\2\"#\3\2\2\2#$\5\24\13"+
		"\2$\3\3\2\2\2%)\5\6\4\2&(\5\b\5\2\'&\3\2\2\2(+\3\2\2\2)\'\3\2\2\2)*\3"+
		"\2\2\2*\5\3\2\2\2+)\3\2\2\2,-\7\13\2\2-\61\7\25\2\2.\60\5\34\17\2/.\3"+
		"\2\2\2\60\63\3\2\2\2\61/\3\2\2\2\61\62\3\2\2\2\62\64\3\2\2\2\63\61\3\2"+
		"\2\2\64\65\7\17\2\2\65\7\3\2\2\2\66:\5\26\f\2\67:\5\n\6\28:\5\20\t\29"+
		"\66\3\2\2\29\67\3\2\2\298\3\2\2\2:\t\3\2\2\2;<\7\f\2\2<=\7\26\2\2=>\7"+
		"\30\2\2>B\5\16\b\2?A\7\27\2\2@?\3\2\2\2AD\3\2\2\2B@\3\2\2\2BC\3\2\2\2"+
		"CF\3\2\2\2DB\3\2\2\2EG\5\f\7\2FE\3\2\2\2FG\3\2\2\2GH\3\2\2\2HI\7\16\2"+
		"\2I\13\3\2\2\2JN\7\22\2\2KM\7\27\2\2LK\3\2\2\2MP\3\2\2\2NL\3\2\2\2NO\3"+
		"\2\2\2OQ\3\2\2\2PN\3\2\2\2QR\7\23\2\2R\r\3\2\2\2SU\7\30\2\2TS\3\2\2\2"+
		"TU\3\2\2\2U\17\3\2\2\2VW\7\13\2\2W[\7\30\2\2XZ\5\34\17\2YX\3\2\2\2Z]\3"+
		"\2\2\2[Y\3\2\2\2[\\\3\2\2\2\\^\3\2\2\2][\3\2\2\2^_\7\17\2\2_\21\3\2\2"+
		"\2`b\5\36\20\2a`\3\2\2\2ab\3\2\2\2bn\3\2\2\2ch\5\24\13\2dh\5\32\16\2e"+
		"h\5\30\r\2fh\5\26\f\2gc\3\2\2\2gd\3\2\2\2ge\3\2\2\2gf\3\2\2\2hj\3\2\2"+
		"\2ik\5\36\20\2ji\3\2\2\2jk\3\2\2\2km\3\2\2\2lg\3\2\2\2mp\3\2\2\2nl\3\2"+
		"\2\2no\3\2\2\2o\23\3\2\2\2pn\3\2\2\2qr\7\n\2\2rv\7\30\2\2su\5\34\17\2"+
		"ts\3\2\2\2ux\3\2\2\2vt\3\2\2\2vw\3\2\2\2wy\3\2\2\2xv\3\2\2\2yz\7\16\2"+
		"\2z{\5\22\n\2{|\7\n\2\2|}\7\21\2\2}~\7\30\2\2~\177\7\16\2\2\177\u008a"+
		"\3\2\2\2\u0080\u0081\7\n\2\2\u0081\u0085\7\30\2\2\u0082\u0084\5\34\17"+
		"\2\u0083\u0082\3\2\2\2\u0084\u0087\3\2\2\2\u0085\u0083\3\2\2\2\u0085\u0086"+
		"\3\2\2\2\u0086\u0088\3\2\2\2\u0087\u0085\3\2\2\2\u0088\u008a\7\20\2\2"+
		"\u0089q\3\2\2\2\u0089\u0080\3\2\2\2\u008a\25\3\2\2\2\u008b\u008c\7\3\2"+
		"\2\u008c\u008d\7\r\2\2\u008d\u008e\7\4\2\2\u008e\27\3\2\2\2\u008f\u0090"+
		"\7\5\2\2\u0090\u0091\7\r\2\2\u0091\u0092\7\6\2\2\u0092\31\3\2\2\2\u0093"+
		"\u0094\t\2\2\2\u0094\33\3\2\2\2\u0095\u0096\7\30\2\2\u0096\u0097\7\24"+
		"\2\2\u0097\u0098\7\27\2\2\u0098\35\3\2\2\2\u0099\u009a\t\3\2\2\u009a\37"+
		"\3\2\2\2\22!)\619BFNT[agjnv\u0085\u0089";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}