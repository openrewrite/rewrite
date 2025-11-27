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
// Generated from /Users/knut/git/openrewrite/rewrite/rewrite-xml/src/main/antlr/XPathParser.g4 by ANTLR 4.13.2
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
public class XPathParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		WS=1, SLASH=2, DOUBLE_SLASH=3, LBRACKET=4, RBRACKET=5, LPAREN=6, RPAREN=7, 
		AT=8, DOT=9, COMMA=10, EQUALS=11, NOT_EQUALS=12, LTE=13, GTE=14, LT=15, 
		GT=16, WILDCARD=17, NUMBER=18, AND=19, OR=20, LOCAL_NAME=21, NAMESPACE_URI=22, 
		STRING_LITERAL=23, QNAME=24;
	public static final int
		RULE_xpathExpression = 0, RULE_booleanExpr = 1, RULE_comparisonOp = 2, 
		RULE_comparand = 3, RULE_absoluteLocationPath = 4, RULE_relativeLocationPath = 5, 
		RULE_pathSeparator = 6, RULE_step = 7, RULE_nodeTypeTest = 8, RULE_attributeStep = 9, 
		RULE_nodeTest = 10, RULE_predicate = 11, RULE_predicateExpr = 12, RULE_orExpr = 13, 
		RULE_andExpr = 14, RULE_primaryExpr = 15, RULE_functionCall = 16, RULE_functionArgs = 17, 
		RULE_functionArg = 18, RULE_childElementTest = 19, RULE_stringLiteral = 20;
	private static String[] makeRuleNames() {
		return new String[] {
			"xpathExpression", "booleanExpr", "comparisonOp", "comparand", "absoluteLocationPath", 
			"relativeLocationPath", "pathSeparator", "step", "nodeTypeTest", "attributeStep", 
			"nodeTest", "predicate", "predicateExpr", "orExpr", "andExpr", "primaryExpr", 
			"functionCall", "functionArgs", "functionArg", "childElementTest", "stringLiteral"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, "'/'", "'//'", "'['", "']'", "'('", "')'", "'@'", "'.'", 
			"','", "'='", "'!='", "'<='", "'>='", "'<'", "'>'", "'*'", null, "'and'", 
			"'or'", "'local-name'", "'namespace-uri'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "WS", "SLASH", "DOUBLE_SLASH", "LBRACKET", "RBRACKET", "LPAREN", 
			"RPAREN", "AT", "DOT", "COMMA", "EQUALS", "NOT_EQUALS", "LTE", "GTE", 
			"LT", "GT", "WILDCARD", "NUMBER", "AND", "OR", "LOCAL_NAME", "NAMESPACE_URI", 
			"STRING_LITERAL", "QNAME"
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
	public String getGrammarFileName() { return "XPathParser.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public XPathParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class XpathExpressionContext extends ParserRuleContext {
		public BooleanExprContext booleanExpr() {
			return getRuleContext(BooleanExprContext.class,0);
		}
		public AbsoluteLocationPathContext absoluteLocationPath() {
			return getRuleContext(AbsoluteLocationPathContext.class,0);
		}
		public RelativeLocationPathContext relativeLocationPath() {
			return getRuleContext(RelativeLocationPathContext.class,0);
		}
		public XpathExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_xpathExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterXpathExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitXpathExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitXpathExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final XpathExpressionContext xpathExpression() throws RecognitionException {
		XpathExpressionContext _localctx = new XpathExpressionContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_xpathExpression);
		try {
			setState(45);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(42);
				booleanExpr();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(43);
				absoluteLocationPath();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(44);
				relativeLocationPath();
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
	public static class BooleanExprContext extends ParserRuleContext {
		public FunctionCallContext functionCall() {
			return getRuleContext(FunctionCallContext.class,0);
		}
		public ComparisonOpContext comparisonOp() {
			return getRuleContext(ComparisonOpContext.class,0);
		}
		public ComparandContext comparand() {
			return getRuleContext(ComparandContext.class,0);
		}
		public BooleanExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_booleanExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterBooleanExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitBooleanExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitBooleanExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BooleanExprContext booleanExpr() throws RecognitionException {
		BooleanExprContext _localctx = new BooleanExprContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_booleanExpr);
		try {
			setState(52);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(47);
				functionCall();
				setState(48);
				comparisonOp();
				setState(49);
				comparand();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(51);
				functionCall();
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
	public static class ComparisonOpContext extends ParserRuleContext {
		public TerminalNode EQUALS() { return getToken(XPathParser.EQUALS, 0); }
		public TerminalNode NOT_EQUALS() { return getToken(XPathParser.NOT_EQUALS, 0); }
		public TerminalNode LT() { return getToken(XPathParser.LT, 0); }
		public TerminalNode GT() { return getToken(XPathParser.GT, 0); }
		public TerminalNode LTE() { return getToken(XPathParser.LTE, 0); }
		public TerminalNode GTE() { return getToken(XPathParser.GTE, 0); }
		public ComparisonOpContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_comparisonOp; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterComparisonOp(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitComparisonOp(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitComparisonOp(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ComparisonOpContext comparisonOp() throws RecognitionException {
		ComparisonOpContext _localctx = new ComparisonOpContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_comparisonOp);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(54);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 129024L) != 0)) ) {
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
	public static class ComparandContext extends ParserRuleContext {
		public StringLiteralContext stringLiteral() {
			return getRuleContext(StringLiteralContext.class,0);
		}
		public TerminalNode NUMBER() { return getToken(XPathParser.NUMBER, 0); }
		public ComparandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_comparand; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterComparand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitComparand(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitComparand(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ComparandContext comparand() throws RecognitionException {
		ComparandContext _localctx = new ComparandContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_comparand);
		try {
			setState(58);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case STRING_LITERAL:
				enterOuterAlt(_localctx, 1);
				{
				setState(56);
				stringLiteral();
				}
				break;
			case NUMBER:
				enterOuterAlt(_localctx, 2);
				{
				setState(57);
				match(NUMBER);
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
	public static class AbsoluteLocationPathContext extends ParserRuleContext {
		public TerminalNode SLASH() { return getToken(XPathParser.SLASH, 0); }
		public RelativeLocationPathContext relativeLocationPath() {
			return getRuleContext(RelativeLocationPathContext.class,0);
		}
		public TerminalNode DOUBLE_SLASH() { return getToken(XPathParser.DOUBLE_SLASH, 0); }
		public AbsoluteLocationPathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_absoluteLocationPath; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterAbsoluteLocationPath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitAbsoluteLocationPath(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitAbsoluteLocationPath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AbsoluteLocationPathContext absoluteLocationPath() throws RecognitionException {
		AbsoluteLocationPathContext _localctx = new AbsoluteLocationPathContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_absoluteLocationPath);
		int _la;
		try {
			setState(66);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SLASH:
				enterOuterAlt(_localctx, 1);
				{
				setState(60);
				match(SLASH);
				setState(62);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 16908544L) != 0)) {
					{
					setState(61);
					relativeLocationPath();
					}
				}

				}
				break;
			case DOUBLE_SLASH:
				enterOuterAlt(_localctx, 2);
				{
				setState(64);
				match(DOUBLE_SLASH);
				setState(65);
				relativeLocationPath();
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
	public static class RelativeLocationPathContext extends ParserRuleContext {
		public List<StepContext> step() {
			return getRuleContexts(StepContext.class);
		}
		public StepContext step(int i) {
			return getRuleContext(StepContext.class,i);
		}
		public List<PathSeparatorContext> pathSeparator() {
			return getRuleContexts(PathSeparatorContext.class);
		}
		public PathSeparatorContext pathSeparator(int i) {
			return getRuleContext(PathSeparatorContext.class,i);
		}
		public RelativeLocationPathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_relativeLocationPath; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterRelativeLocationPath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitRelativeLocationPath(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitRelativeLocationPath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RelativeLocationPathContext relativeLocationPath() throws RecognitionException {
		RelativeLocationPathContext _localctx = new RelativeLocationPathContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_relativeLocationPath);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(68);
			step();
			setState(74);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==SLASH || _la==DOUBLE_SLASH) {
				{
				{
				setState(69);
				pathSeparator();
				setState(70);
				step();
				}
				}
				setState(76);
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
	public static class PathSeparatorContext extends ParserRuleContext {
		public TerminalNode SLASH() { return getToken(XPathParser.SLASH, 0); }
		public TerminalNode DOUBLE_SLASH() { return getToken(XPathParser.DOUBLE_SLASH, 0); }
		public PathSeparatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pathSeparator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterPathSeparator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitPathSeparator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitPathSeparator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PathSeparatorContext pathSeparator() throws RecognitionException {
		PathSeparatorContext _localctx = new PathSeparatorContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_pathSeparator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(77);
			_la = _input.LA(1);
			if ( !(_la==SLASH || _la==DOUBLE_SLASH) ) {
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
	public static class StepContext extends ParserRuleContext {
		public NodeTestContext nodeTest() {
			return getRuleContext(NodeTestContext.class,0);
		}
		public List<PredicateContext> predicate() {
			return getRuleContexts(PredicateContext.class);
		}
		public PredicateContext predicate(int i) {
			return getRuleContext(PredicateContext.class,i);
		}
		public AttributeStepContext attributeStep() {
			return getRuleContext(AttributeStepContext.class,0);
		}
		public NodeTypeTestContext nodeTypeTest() {
			return getRuleContext(NodeTypeTestContext.class,0);
		}
		public StepContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_step; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterStep(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitStep(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitStep(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StepContext step() throws RecognitionException {
		StepContext _localctx = new StepContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_step);
		int _la;
		try {
			setState(94);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,8,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(79);
				nodeTest();
				setState(83);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==LBRACKET) {
					{
					{
					setState(80);
					predicate();
					}
					}
					setState(85);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(86);
				attributeStep();
				setState(90);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==LBRACKET) {
					{
					{
					setState(87);
					predicate();
					}
					}
					setState(92);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(93);
				nodeTypeTest();
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
	public static class NodeTypeTestContext extends ParserRuleContext {
		public TerminalNode QNAME() { return getToken(XPathParser.QNAME, 0); }
		public TerminalNode LPAREN() { return getToken(XPathParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(XPathParser.RPAREN, 0); }
		public NodeTypeTestContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nodeTypeTest; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterNodeTypeTest(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitNodeTypeTest(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitNodeTypeTest(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NodeTypeTestContext nodeTypeTest() throws RecognitionException {
		NodeTypeTestContext _localctx = new NodeTypeTestContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_nodeTypeTest);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(96);
			match(QNAME);
			setState(97);
			match(LPAREN);
			setState(98);
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
	public static class AttributeStepContext extends ParserRuleContext {
		public TerminalNode AT() { return getToken(XPathParser.AT, 0); }
		public TerminalNode QNAME() { return getToken(XPathParser.QNAME, 0); }
		public TerminalNode WILDCARD() { return getToken(XPathParser.WILDCARD, 0); }
		public AttributeStepContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_attributeStep; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterAttributeStep(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitAttributeStep(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitAttributeStep(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AttributeStepContext attributeStep() throws RecognitionException {
		AttributeStepContext _localctx = new AttributeStepContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_attributeStep);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(100);
			match(AT);
			setState(101);
			_la = _input.LA(1);
			if ( !(_la==WILDCARD || _la==QNAME) ) {
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
	public static class NodeTestContext extends ParserRuleContext {
		public TerminalNode QNAME() { return getToken(XPathParser.QNAME, 0); }
		public TerminalNode WILDCARD() { return getToken(XPathParser.WILDCARD, 0); }
		public NodeTestContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nodeTest; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterNodeTest(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitNodeTest(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitNodeTest(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NodeTestContext nodeTest() throws RecognitionException {
		NodeTestContext _localctx = new NodeTestContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_nodeTest);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(103);
			_la = _input.LA(1);
			if ( !(_la==WILDCARD || _la==QNAME) ) {
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
	public static class PredicateContext extends ParserRuleContext {
		public TerminalNode LBRACKET() { return getToken(XPathParser.LBRACKET, 0); }
		public PredicateExprContext predicateExpr() {
			return getRuleContext(PredicateExprContext.class,0);
		}
		public TerminalNode RBRACKET() { return getToken(XPathParser.RBRACKET, 0); }
		public PredicateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_predicate; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterPredicate(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitPredicate(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitPredicate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PredicateContext predicate() throws RecognitionException {
		PredicateContext _localctx = new PredicateContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_predicate);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(105);
			match(LBRACKET);
			setState(106);
			predicateExpr();
			setState(107);
			match(RBRACKET);
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
	public static class PredicateExprContext extends ParserRuleContext {
		public OrExprContext orExpr() {
			return getRuleContext(OrExprContext.class,0);
		}
		public PredicateExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_predicateExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterPredicateExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitPredicateExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitPredicateExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PredicateExprContext predicateExpr() throws RecognitionException {
		PredicateExprContext _localctx = new PredicateExprContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_predicateExpr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(109);
			orExpr();
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
	public static class OrExprContext extends ParserRuleContext {
		public List<AndExprContext> andExpr() {
			return getRuleContexts(AndExprContext.class);
		}
		public AndExprContext andExpr(int i) {
			return getRuleContext(AndExprContext.class,i);
		}
		public List<TerminalNode> OR() { return getTokens(XPathParser.OR); }
		public TerminalNode OR(int i) {
			return getToken(XPathParser.OR, i);
		}
		public OrExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_orExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterOrExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitOrExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitOrExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OrExprContext orExpr() throws RecognitionException {
		OrExprContext _localctx = new OrExprContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_orExpr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(111);
			andExpr();
			setState(116);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==OR) {
				{
				{
				setState(112);
				match(OR);
				setState(113);
				andExpr();
				}
				}
				setState(118);
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
	public static class AndExprContext extends ParserRuleContext {
		public List<PrimaryExprContext> primaryExpr() {
			return getRuleContexts(PrimaryExprContext.class);
		}
		public PrimaryExprContext primaryExpr(int i) {
			return getRuleContext(PrimaryExprContext.class,i);
		}
		public List<TerminalNode> AND() { return getTokens(XPathParser.AND); }
		public TerminalNode AND(int i) {
			return getToken(XPathParser.AND, i);
		}
		public AndExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_andExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterAndExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitAndExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitAndExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AndExprContext andExpr() throws RecognitionException {
		AndExprContext _localctx = new AndExprContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_andExpr);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(119);
			primaryExpr();
			setState(124);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AND) {
				{
				{
				setState(120);
				match(AND);
				setState(121);
				primaryExpr();
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
	public static class PrimaryExprContext extends ParserRuleContext {
		public FunctionCallContext functionCall() {
			return getRuleContext(FunctionCallContext.class,0);
		}
		public TerminalNode EQUALS() { return getToken(XPathParser.EQUALS, 0); }
		public StringLiteralContext stringLiteral() {
			return getRuleContext(StringLiteralContext.class,0);
		}
		public AttributeStepContext attributeStep() {
			return getRuleContext(AttributeStepContext.class,0);
		}
		public RelativeLocationPathContext relativeLocationPath() {
			return getRuleContext(RelativeLocationPathContext.class,0);
		}
		public ChildElementTestContext childElementTest() {
			return getRuleContext(ChildElementTestContext.class,0);
		}
		public PrimaryExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_primaryExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterPrimaryExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitPrimaryExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitPrimaryExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PrimaryExprContext primaryExpr() throws RecognitionException {
		PrimaryExprContext _localctx = new PrimaryExprContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_primaryExpr);
		try {
			setState(143);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(127);
				functionCall();
				setState(128);
				match(EQUALS);
				setState(129);
				stringLiteral();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(131);
				attributeStep();
				setState(132);
				match(EQUALS);
				setState(133);
				stringLiteral();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(135);
				relativeLocationPath();
				setState(136);
				match(EQUALS);
				setState(137);
				stringLiteral();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(139);
				childElementTest();
				setState(140);
				match(EQUALS);
				setState(141);
				stringLiteral();
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
	public static class FunctionCallContext extends ParserRuleContext {
		public TerminalNode LOCAL_NAME() { return getToken(XPathParser.LOCAL_NAME, 0); }
		public TerminalNode LPAREN() { return getToken(XPathParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(XPathParser.RPAREN, 0); }
		public TerminalNode NAMESPACE_URI() { return getToken(XPathParser.NAMESPACE_URI, 0); }
		public TerminalNode QNAME() { return getToken(XPathParser.QNAME, 0); }
		public FunctionArgsContext functionArgs() {
			return getRuleContext(FunctionArgsContext.class,0);
		}
		public FunctionCallContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionCall; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterFunctionCall(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitFunctionCall(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitFunctionCall(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionCallContext functionCall() throws RecognitionException {
		FunctionCallContext _localctx = new FunctionCallContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_functionCall);
		int _la;
		try {
			setState(157);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LOCAL_NAME:
				enterOuterAlt(_localctx, 1);
				{
				setState(145);
				match(LOCAL_NAME);
				setState(146);
				match(LPAREN);
				setState(147);
				match(RPAREN);
				}
				break;
			case NAMESPACE_URI:
				enterOuterAlt(_localctx, 2);
				{
				setState(148);
				match(NAMESPACE_URI);
				setState(149);
				match(LPAREN);
				setState(150);
				match(RPAREN);
				}
				break;
			case QNAME:
				enterOuterAlt(_localctx, 3);
				{
				setState(151);
				match(QNAME);
				setState(152);
				match(LPAREN);
				setState(154);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 31850764L) != 0)) {
					{
					setState(153);
					functionArgs();
					}
				}

				setState(156);
				match(RPAREN);
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
	public static class FunctionArgsContext extends ParserRuleContext {
		public List<FunctionArgContext> functionArg() {
			return getRuleContexts(FunctionArgContext.class);
		}
		public FunctionArgContext functionArg(int i) {
			return getRuleContext(FunctionArgContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(XPathParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(XPathParser.COMMA, i);
		}
		public FunctionArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterFunctionArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitFunctionArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitFunctionArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionArgsContext functionArgs() throws RecognitionException {
		FunctionArgsContext _localctx = new FunctionArgsContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_functionArgs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(159);
			functionArg();
			setState(164);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(160);
				match(COMMA);
				setState(161);
				functionArg();
				}
				}
				setState(166);
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
	public static class FunctionArgContext extends ParserRuleContext {
		public AbsoluteLocationPathContext absoluteLocationPath() {
			return getRuleContext(AbsoluteLocationPathContext.class,0);
		}
		public FunctionCallContext functionCall() {
			return getRuleContext(FunctionCallContext.class,0);
		}
		public RelativeLocationPathContext relativeLocationPath() {
			return getRuleContext(RelativeLocationPathContext.class,0);
		}
		public StringLiteralContext stringLiteral() {
			return getRuleContext(StringLiteralContext.class,0);
		}
		public TerminalNode NUMBER() { return getToken(XPathParser.NUMBER, 0); }
		public FunctionArgContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionArg; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterFunctionArg(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitFunctionArg(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitFunctionArg(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionArgContext functionArg() throws RecognitionException {
		FunctionArgContext _localctx = new FunctionArgContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_functionArg);
		try {
			setState(172);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,15,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(167);
				absoluteLocationPath();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(168);
				functionCall();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(169);
				relativeLocationPath();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(170);
				stringLiteral();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(171);
				match(NUMBER);
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
	public static class ChildElementTestContext extends ParserRuleContext {
		public TerminalNode QNAME() { return getToken(XPathParser.QNAME, 0); }
		public TerminalNode WILDCARD() { return getToken(XPathParser.WILDCARD, 0); }
		public ChildElementTestContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_childElementTest; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterChildElementTest(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitChildElementTest(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitChildElementTest(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ChildElementTestContext childElementTest() throws RecognitionException {
		ChildElementTestContext _localctx = new ChildElementTestContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_childElementTest);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(174);
			_la = _input.LA(1);
			if ( !(_la==WILDCARD || _la==QNAME) ) {
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
	public static class StringLiteralContext extends ParserRuleContext {
		public TerminalNode STRING_LITERAL() { return getToken(XPathParser.STRING_LITERAL, 0); }
		public StringLiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stringLiteral; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).enterStringLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof XPathParserListener ) ((XPathParserListener)listener).exitStringLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof XPathParserVisitor ) return ((XPathParserVisitor<? extends T>)visitor).visitStringLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StringLiteralContext stringLiteral() throws RecognitionException {
		StringLiteralContext _localctx = new StringLiteralContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_stringLiteral);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(176);
			match(STRING_LITERAL);
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
		"\u0004\u0001\u0018\u00b3\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001"+
		"\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004"+
		"\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007"+
		"\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b"+
		"\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007"+
		"\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007"+
		"\u0012\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0001\u0000\u0001"+
		"\u0000\u0001\u0000\u0003\u0000.\b\u0000\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0003\u00015\b\u0001\u0001\u0002\u0001"+
		"\u0002\u0001\u0003\u0001\u0003\u0003\u0003;\b\u0003\u0001\u0004\u0001"+
		"\u0004\u0003\u0004?\b\u0004\u0001\u0004\u0001\u0004\u0003\u0004C\b\u0004"+
		"\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0005\u0005I\b\u0005"+
		"\n\u0005\f\u0005L\t\u0005\u0001\u0006\u0001\u0006\u0001\u0007\u0001\u0007"+
		"\u0005\u0007R\b\u0007\n\u0007\f\u0007U\t\u0007\u0001\u0007\u0001\u0007"+
		"\u0005\u0007Y\b\u0007\n\u0007\f\u0007\\\t\u0007\u0001\u0007\u0003\u0007"+
		"_\b\u0007\u0001\b\u0001\b\u0001\b\u0001\b\u0001\t\u0001\t\u0001\t\u0001"+
		"\n\u0001\n\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\f\u0001"+
		"\f\u0001\r\u0001\r\u0001\r\u0005\rs\b\r\n\r\f\rv\t\r\u0001\u000e\u0001"+
		"\u000e\u0001\u000e\u0005\u000e{\b\u000e\n\u000e\f\u000e~\t\u000e\u0001"+
		"\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001"+
		"\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001"+
		"\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0003\u000f\u0090\b\u000f\u0001"+
		"\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001"+
		"\u0010\u0001\u0010\u0001\u0010\u0003\u0010\u009b\b\u0010\u0001\u0010\u0003"+
		"\u0010\u009e\b\u0010\u0001\u0011\u0001\u0011\u0001\u0011\u0005\u0011\u00a3"+
		"\b\u0011\n\u0011\f\u0011\u00a6\t\u0011\u0001\u0012\u0001\u0012\u0001\u0012"+
		"\u0001\u0012\u0001\u0012\u0003\u0012\u00ad\b\u0012\u0001\u0013\u0001\u0013"+
		"\u0001\u0014\u0001\u0014\u0001\u0014\u0000\u0000\u0015\u0000\u0002\u0004"+
		"\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u001c\u001e \""+
		"$&(\u0000\u0003\u0001\u0000\u000b\u0010\u0001\u0000\u0002\u0003\u0002"+
		"\u0000\u0011\u0011\u0018\u0018\u00b5\u0000-\u0001\u0000\u0000\u0000\u0002"+
		"4\u0001\u0000\u0000\u0000\u00046\u0001\u0000\u0000\u0000\u0006:\u0001"+
		"\u0000\u0000\u0000\bB\u0001\u0000\u0000\u0000\nD\u0001\u0000\u0000\u0000"+
		"\fM\u0001\u0000\u0000\u0000\u000e^\u0001\u0000\u0000\u0000\u0010`\u0001"+
		"\u0000\u0000\u0000\u0012d\u0001\u0000\u0000\u0000\u0014g\u0001\u0000\u0000"+
		"\u0000\u0016i\u0001\u0000\u0000\u0000\u0018m\u0001\u0000\u0000\u0000\u001a"+
		"o\u0001\u0000\u0000\u0000\u001cw\u0001\u0000\u0000\u0000\u001e\u008f\u0001"+
		"\u0000\u0000\u0000 \u009d\u0001\u0000\u0000\u0000\"\u009f\u0001\u0000"+
		"\u0000\u0000$\u00ac\u0001\u0000\u0000\u0000&\u00ae\u0001\u0000\u0000\u0000"+
		"(\u00b0\u0001\u0000\u0000\u0000*.\u0003\u0002\u0001\u0000+.\u0003\b\u0004"+
		"\u0000,.\u0003\n\u0005\u0000-*\u0001\u0000\u0000\u0000-+\u0001\u0000\u0000"+
		"\u0000-,\u0001\u0000\u0000\u0000.\u0001\u0001\u0000\u0000\u0000/0\u0003"+
		" \u0010\u000001\u0003\u0004\u0002\u000012\u0003\u0006\u0003\u000025\u0001"+
		"\u0000\u0000\u000035\u0003 \u0010\u00004/\u0001\u0000\u0000\u000043\u0001"+
		"\u0000\u0000\u00005\u0003\u0001\u0000\u0000\u000067\u0007\u0000\u0000"+
		"\u00007\u0005\u0001\u0000\u0000\u00008;\u0003(\u0014\u00009;\u0005\u0012"+
		"\u0000\u0000:8\u0001\u0000\u0000\u0000:9\u0001\u0000\u0000\u0000;\u0007"+
		"\u0001\u0000\u0000\u0000<>\u0005\u0002\u0000\u0000=?\u0003\n\u0005\u0000"+
		">=\u0001\u0000\u0000\u0000>?\u0001\u0000\u0000\u0000?C\u0001\u0000\u0000"+
		"\u0000@A\u0005\u0003\u0000\u0000AC\u0003\n\u0005\u0000B<\u0001\u0000\u0000"+
		"\u0000B@\u0001\u0000\u0000\u0000C\t\u0001\u0000\u0000\u0000DJ\u0003\u000e"+
		"\u0007\u0000EF\u0003\f\u0006\u0000FG\u0003\u000e\u0007\u0000GI\u0001\u0000"+
		"\u0000\u0000HE\u0001\u0000\u0000\u0000IL\u0001\u0000\u0000\u0000JH\u0001"+
		"\u0000\u0000\u0000JK\u0001\u0000\u0000\u0000K\u000b\u0001\u0000\u0000"+
		"\u0000LJ\u0001\u0000\u0000\u0000MN\u0007\u0001\u0000\u0000N\r\u0001\u0000"+
		"\u0000\u0000OS\u0003\u0014\n\u0000PR\u0003\u0016\u000b\u0000QP\u0001\u0000"+
		"\u0000\u0000RU\u0001\u0000\u0000\u0000SQ\u0001\u0000\u0000\u0000ST\u0001"+
		"\u0000\u0000\u0000T_\u0001\u0000\u0000\u0000US\u0001\u0000\u0000\u0000"+
		"VZ\u0003\u0012\t\u0000WY\u0003\u0016\u000b\u0000XW\u0001\u0000\u0000\u0000"+
		"Y\\\u0001\u0000\u0000\u0000ZX\u0001\u0000\u0000\u0000Z[\u0001\u0000\u0000"+
		"\u0000[_\u0001\u0000\u0000\u0000\\Z\u0001\u0000\u0000\u0000]_\u0003\u0010"+
		"\b\u0000^O\u0001\u0000\u0000\u0000^V\u0001\u0000\u0000\u0000^]\u0001\u0000"+
		"\u0000\u0000_\u000f\u0001\u0000\u0000\u0000`a\u0005\u0018\u0000\u0000"+
		"ab\u0005\u0006\u0000\u0000bc\u0005\u0007\u0000\u0000c\u0011\u0001\u0000"+
		"\u0000\u0000de\u0005\b\u0000\u0000ef\u0007\u0002\u0000\u0000f\u0013\u0001"+
		"\u0000\u0000\u0000gh\u0007\u0002\u0000\u0000h\u0015\u0001\u0000\u0000"+
		"\u0000ij\u0005\u0004\u0000\u0000jk\u0003\u0018\f\u0000kl\u0005\u0005\u0000"+
		"\u0000l\u0017\u0001\u0000\u0000\u0000mn\u0003\u001a\r\u0000n\u0019\u0001"+
		"\u0000\u0000\u0000ot\u0003\u001c\u000e\u0000pq\u0005\u0014\u0000\u0000"+
		"qs\u0003\u001c\u000e\u0000rp\u0001\u0000\u0000\u0000sv\u0001\u0000\u0000"+
		"\u0000tr\u0001\u0000\u0000\u0000tu\u0001\u0000\u0000\u0000u\u001b\u0001"+
		"\u0000\u0000\u0000vt\u0001\u0000\u0000\u0000w|\u0003\u001e\u000f\u0000"+
		"xy\u0005\u0013\u0000\u0000y{\u0003\u001e\u000f\u0000zx\u0001\u0000\u0000"+
		"\u0000{~\u0001\u0000\u0000\u0000|z\u0001\u0000\u0000\u0000|}\u0001\u0000"+
		"\u0000\u0000}\u001d\u0001\u0000\u0000\u0000~|\u0001\u0000\u0000\u0000"+
		"\u007f\u0080\u0003 \u0010\u0000\u0080\u0081\u0005\u000b\u0000\u0000\u0081"+
		"\u0082\u0003(\u0014\u0000\u0082\u0090\u0001\u0000\u0000\u0000\u0083\u0084"+
		"\u0003\u0012\t\u0000\u0084\u0085\u0005\u000b\u0000\u0000\u0085\u0086\u0003"+
		"(\u0014\u0000\u0086\u0090\u0001\u0000\u0000\u0000\u0087\u0088\u0003\n"+
		"\u0005\u0000\u0088\u0089\u0005\u000b\u0000\u0000\u0089\u008a\u0003(\u0014"+
		"\u0000\u008a\u0090\u0001\u0000\u0000\u0000\u008b\u008c\u0003&\u0013\u0000"+
		"\u008c\u008d\u0005\u000b\u0000\u0000\u008d\u008e\u0003(\u0014\u0000\u008e"+
		"\u0090\u0001\u0000\u0000\u0000\u008f\u007f\u0001\u0000\u0000\u0000\u008f"+
		"\u0083\u0001\u0000\u0000\u0000\u008f\u0087\u0001\u0000\u0000\u0000\u008f"+
		"\u008b\u0001\u0000\u0000\u0000\u0090\u001f\u0001\u0000\u0000\u0000\u0091"+
		"\u0092\u0005\u0015\u0000\u0000\u0092\u0093\u0005\u0006\u0000\u0000\u0093"+
		"\u009e\u0005\u0007\u0000\u0000\u0094\u0095\u0005\u0016\u0000\u0000\u0095"+
		"\u0096\u0005\u0006\u0000\u0000\u0096\u009e\u0005\u0007\u0000\u0000\u0097"+
		"\u0098\u0005\u0018\u0000\u0000\u0098\u009a\u0005\u0006\u0000\u0000\u0099"+
		"\u009b\u0003\"\u0011\u0000\u009a\u0099\u0001\u0000\u0000\u0000\u009a\u009b"+
		"\u0001\u0000\u0000\u0000\u009b\u009c\u0001\u0000\u0000\u0000\u009c\u009e"+
		"\u0005\u0007\u0000\u0000\u009d\u0091\u0001\u0000\u0000\u0000\u009d\u0094"+
		"\u0001\u0000\u0000\u0000\u009d\u0097\u0001\u0000\u0000\u0000\u009e!\u0001"+
		"\u0000\u0000\u0000\u009f\u00a4\u0003$\u0012\u0000\u00a0\u00a1\u0005\n"+
		"\u0000\u0000\u00a1\u00a3\u0003$\u0012\u0000\u00a2\u00a0\u0001\u0000\u0000"+
		"\u0000\u00a3\u00a6\u0001\u0000\u0000\u0000\u00a4\u00a2\u0001\u0000\u0000"+
		"\u0000\u00a4\u00a5\u0001\u0000\u0000\u0000\u00a5#\u0001\u0000\u0000\u0000"+
		"\u00a6\u00a4\u0001\u0000\u0000\u0000\u00a7\u00ad\u0003\b\u0004\u0000\u00a8"+
		"\u00ad\u0003 \u0010\u0000\u00a9\u00ad\u0003\n\u0005\u0000\u00aa\u00ad"+
		"\u0003(\u0014\u0000\u00ab\u00ad\u0005\u0012\u0000\u0000\u00ac\u00a7\u0001"+
		"\u0000\u0000\u0000\u00ac\u00a8\u0001\u0000\u0000\u0000\u00ac\u00a9\u0001"+
		"\u0000\u0000\u0000\u00ac\u00aa\u0001\u0000\u0000\u0000\u00ac\u00ab\u0001"+
		"\u0000\u0000\u0000\u00ad%\u0001\u0000\u0000\u0000\u00ae\u00af\u0007\u0002"+
		"\u0000\u0000\u00af\'\u0001\u0000\u0000\u0000\u00b0\u00b1\u0005\u0017\u0000"+
		"\u0000\u00b1)\u0001\u0000\u0000\u0000\u0010-4:>BJSZ^t|\u008f\u009a\u009d"+
		"\u00a4\u00ac";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}