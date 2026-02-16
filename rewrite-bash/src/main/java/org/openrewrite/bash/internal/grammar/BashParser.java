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
// Generated from /Users/kevin/conductor/workspaces/moderne-product-management/tripoli/working-set-bash-parser/rewrite/rewrite-bash/src/main/antlr/BashParser.g4 by ANTLR 4.13.2
package org.openrewrite.bash.internal.grammar;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class BashParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		SHEBANG=1, LINE_CONTINUATION=2, NEWLINE=3, COMMENT=4, IF=5, THEN=6, ELSE=7, 
		ELIF=8, FI=9, FOR=10, WHILE=11, UNTIL=12, DO=13, DONE=14, CASE=15, ESAC=16, 
		IN=17, FUNCTION=18, SELECT=19, COPROC=20, TIME=21, DOUBLE_LBRACKET=22, 
		DOUBLE_RBRACKET=23, DOUBLE_LPAREN=24, DOUBLE_RPAREN=25, DOLLAR_DPAREN=26, 
		DOLLAR_LPAREN=27, DOLLAR_LBRACE=28, HERESTRING=29, DLESSDASH=30, DLESS=31, 
		DGREAT=32, LESSAND=33, GREATAND=34, LESSGREAT=35, CLOBBER=36, PROC_SUBST_IN=37, 
		PROC_SUBST_OUT=38, LESS=39, GREAT=40, AND=41, OR=42, PIPE_AND=43, PIPE=44, 
		DSEMI_AND=45, DSEMI=46, SEMI_AND=47, SEMI=48, AMP=49, PLUS_EQUALS=50, 
		EQUALS=51, LPAREN=52, RPAREN=53, LBRACE=54, RBRACE=55, LBRACKET=56, RBRACKET=57, 
		BANG=58, COLON=59, BACKTICK=60, SPECIAL_VAR=61, DOLLAR_NAME=62, DOLLAR_SINGLE_QUOTED=63, 
		SINGLE_QUOTED_STRING=64, DOUBLE_QUOTE=65, NUMBER=66, WORD=67, DOLLAR=68, 
		STAR=69, QUESTION=70, TILDE=71, WS=72, ANY_CHAR=73, DQ_ESCAPE=74, DQ_TEXT=75, 
		BE_COLON_DASH=76, BE_COLON_EQUALS=77, BE_COLON_PLUS=78, BE_COLON_QUEST=79, 
		BE_DHASH=80, BE_HASH=81, BE_DPERCENT=82, BE_PERCENT=83, BE_DSLASH=84, 
		BE_SLASH=85, BE_DCARET=86, BE_CARET=87, BE_DCOMMA=88, BE_COMMA=89, BE_AT=90, 
		BE_STAR=91, BE_BANG=92, BE_COLON=93, BE_LBRACKET=94, BE_RBRACKET=95, BE_NAME=96, 
		BE_NUMBER=97, BE_TEXT=98;
	public static final int
		RULE_program = 0, RULE_shebang = 1, RULE_completeCommands = 2, RULE_completeCommand = 3, 
		RULE_list = 4, RULE_listSeparator = 5, RULE_andOr = 6, RULE_pipeline = 7, 
		RULE_bangOpt = 8, RULE_timeOpt = 9, RULE_pipeSequence = 10, RULE_pipeOp = 11, 
		RULE_command = 12, RULE_simpleCommand = 13, RULE_cmdPrefix = 14, RULE_cmdWord = 15, 
		RULE_cmdSuffix = 16, RULE_compoundCommand = 17, RULE_braceGroup = 18, 
		RULE_subshell = 19, RULE_compoundList = 20, RULE_ifClause = 21, RULE_elifClause = 22, 
		RULE_elseClause = 23, RULE_forClause = 24, RULE_forBody = 25, RULE_inClause = 26, 
		RULE_cStyleForClause = 27, RULE_whileClause = 28, RULE_untilClause = 29, 
		RULE_doGroup = 30, RULE_caseClause = 31, RULE_caseItem = 32, RULE_caseSeparator = 33, 
		RULE_pattern = 34, RULE_doubleParenExpr = 35, RULE_arithmeticExpr = 36, 
		RULE_arithmeticPart = 37, RULE_doubleBracketExpr = 38, RULE_conditionExpr = 39, 
		RULE_conditionPart = 40, RULE_functionDefinition = 41, RULE_functionParens = 42, 
		RULE_functionBody = 43, RULE_assignment = 44, RULE_assignmentValue = 45, 
		RULE_arrayElements = 46, RULE_redirectionList = 47, RULE_redirection = 48, 
		RULE_redirectionOp = 49, RULE_heredoc = 50, RULE_heredocDelimiter = 51, 
		RULE_heredocBody = 52, RULE_heredocLine = 53, RULE_word = 54, RULE_wordPart = 55, 
		RULE_doubleQuotedString = 56, RULE_doubleQuotedPart = 57, RULE_commandSubstitution = 58, 
		RULE_commandSubstitutionContent = 59, RULE_backtickContent = 60, RULE_arithmeticSubstitution = 61, 
		RULE_processSubstitution = 62, RULE_braceExpansionContent = 63, RULE_braceExpansionPart = 64, 
		RULE_separator = 65, RULE_sequentialSep = 66, RULE_newlineList = 67, RULE_linebreak = 68;
	private static String[] makeRuleNames() {
		return new String[] {
			"program", "shebang", "completeCommands", "completeCommand", "list", 
			"listSeparator", "andOr", "pipeline", "bangOpt", "timeOpt", "pipeSequence", 
			"pipeOp", "command", "simpleCommand", "cmdPrefix", "cmdWord", "cmdSuffix", 
			"compoundCommand", "braceGroup", "subshell", "compoundList", "ifClause", 
			"elifClause", "elseClause", "forClause", "forBody", "inClause", "cStyleForClause", 
			"whileClause", "untilClause", "doGroup", "caseClause", "caseItem", "caseSeparator", 
			"pattern", "doubleParenExpr", "arithmeticExpr", "arithmeticPart", "doubleBracketExpr", 
			"conditionExpr", "conditionPart", "functionDefinition", "functionParens", 
			"functionBody", "assignment", "assignmentValue", "arrayElements", "redirectionList", 
			"redirection", "redirectionOp", "heredoc", "heredocDelimiter", "heredocBody", 
			"heredocLine", "word", "wordPart", "doubleQuotedString", "doubleQuotedPart", 
			"commandSubstitution", "commandSubstitutionContent", "backtickContent", 
			"arithmeticSubstitution", "processSubstitution", "braceExpansionContent", 
			"braceExpansionPart", "separator", "sequentialSep", "newlineList", "linebreak"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, null, null, "'if'", "'then'", "'else'", "'elif'", "'fi'", 
			"'for'", "'while'", "'until'", "'do'", "'done'", "'case'", "'esac'", 
			"'in'", "'function'", "'select'", "'coproc'", "'time'", "'[['", "']]'", 
			"'(('", null, "'$(('", "'$('", "'${'", "'<<<'", "'<<-'", "'<<'", "'>>'", 
			"'<&'", "'>&'", "'<>'", "'>|'", "'<('", "'>('", "'<'", "'>'", "'&&'", 
			"'||'", "'|&'", "'|'", "';;&'", "';;'", "';&'", "';'", "'&'", "'+='", 
			"'='", "'('", "')'", "'{'", null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, "'?'", "'~'", null, null, 
			null, null, "':-'", "':='", "':+'", "':?'", "'##'", "'#'", "'%%'", "'%'", 
			"'//'", "'/'", "'^^'", "'^'", "',,'", "','", "'@'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "SHEBANG", "LINE_CONTINUATION", "NEWLINE", "COMMENT", "IF", "THEN", 
			"ELSE", "ELIF", "FI", "FOR", "WHILE", "UNTIL", "DO", "DONE", "CASE", 
			"ESAC", "IN", "FUNCTION", "SELECT", "COPROC", "TIME", "DOUBLE_LBRACKET", 
			"DOUBLE_RBRACKET", "DOUBLE_LPAREN", "DOUBLE_RPAREN", "DOLLAR_DPAREN", 
			"DOLLAR_LPAREN", "DOLLAR_LBRACE", "HERESTRING", "DLESSDASH", "DLESS", 
			"DGREAT", "LESSAND", "GREATAND", "LESSGREAT", "CLOBBER", "PROC_SUBST_IN", 
			"PROC_SUBST_OUT", "LESS", "GREAT", "AND", "OR", "PIPE_AND", "PIPE", "DSEMI_AND", 
			"DSEMI", "SEMI_AND", "SEMI", "AMP", "PLUS_EQUALS", "EQUALS", "LPAREN", 
			"RPAREN", "LBRACE", "RBRACE", "LBRACKET", "RBRACKET", "BANG", "COLON", 
			"BACKTICK", "SPECIAL_VAR", "DOLLAR_NAME", "DOLLAR_SINGLE_QUOTED", "SINGLE_QUOTED_STRING", 
			"DOUBLE_QUOTE", "NUMBER", "WORD", "DOLLAR", "STAR", "QUESTION", "TILDE", 
			"WS", "ANY_CHAR", "DQ_ESCAPE", "DQ_TEXT", "BE_COLON_DASH", "BE_COLON_EQUALS", 
			"BE_COLON_PLUS", "BE_COLON_QUEST", "BE_DHASH", "BE_HASH", "BE_DPERCENT", 
			"BE_PERCENT", "BE_DSLASH", "BE_SLASH", "BE_DCARET", "BE_CARET", "BE_DCOMMA", 
			"BE_COMMA", "BE_AT", "BE_STAR", "BE_BANG", "BE_COLON", "BE_LBRACKET", 
			"BE_RBRACKET", "BE_NAME", "BE_NUMBER", "BE_TEXT"
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
	public String getGrammarFileName() { return "BashParser.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }


	    private boolean noWhitespaceBefore() {
	        Token prev = _input.LT(-1);
	        Token next = _input.LT(1);
	        if (prev == null || next == null) return false;
	        int gapStart = prev.getStopIndex() + 1;
	        int gapEnd = next.getStartIndex();
	        if (gapStart >= gapEnd) return true;
	        CharStream input = _input.getTokenSource().getInputStream();
	        String gap = input.getText(new org.antlr.v4.runtime.misc.Interval(gapStart, gapEnd - 1));
	        for (int i = 0; i < gap.length(); i++) {
	            char c = gap.charAt(i);
	            if (c == ' ' || c == '\t') return false;
	        }
	        return true;
	    }

	public BashParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ProgramContext extends ParserRuleContext {
		public List<LinebreakContext> linebreak() {
			return getRuleContexts(LinebreakContext.class);
		}
		public LinebreakContext linebreak(int i) {
			return getRuleContext(LinebreakContext.class,i);
		}
		public TerminalNode EOF() { return getToken(BashParser.EOF, 0); }
		public ShebangContext shebang() {
			return getRuleContext(ShebangContext.class,0);
		}
		public CompleteCommandsContext completeCommands() {
			return getRuleContext(CompleteCommandsContext.class,0);
		}
		public ProgramContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_program; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterProgram(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitProgram(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitProgram(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ProgramContext program() throws RecognitionException {
		ProgramContext _localctx = new ProgramContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_program);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(139);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SHEBANG) {
				{
				setState(138);
				shebang();
				}
			}

			setState(141);
			linebreak();
			setState(143);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -46159697199235104L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 255L) != 0)) {
				{
				setState(142);
				completeCommands();
				}
			}

			setState(145);
			linebreak();
			setState(146);
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
	public static class ShebangContext extends ParserRuleContext {
		public TerminalNode SHEBANG() { return getToken(BashParser.SHEBANG, 0); }
		public LinebreakContext linebreak() {
			return getRuleContext(LinebreakContext.class,0);
		}
		public ShebangContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_shebang; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterShebang(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitShebang(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitShebang(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ShebangContext shebang() throws RecognitionException {
		ShebangContext _localctx = new ShebangContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_shebang);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(148);
			match(SHEBANG);
			setState(149);
			linebreak();
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
	public static class CompleteCommandsContext extends ParserRuleContext {
		public List<CompleteCommandContext> completeCommand() {
			return getRuleContexts(CompleteCommandContext.class);
		}
		public CompleteCommandContext completeCommand(int i) {
			return getRuleContext(CompleteCommandContext.class,i);
		}
		public List<NewlineListContext> newlineList() {
			return getRuleContexts(NewlineListContext.class);
		}
		public NewlineListContext newlineList(int i) {
			return getRuleContext(NewlineListContext.class,i);
		}
		public CompleteCommandsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_completeCommands; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterCompleteCommands(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitCompleteCommands(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitCompleteCommands(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CompleteCommandsContext completeCommands() throws RecognitionException {
		CompleteCommandsContext _localctx = new CompleteCommandsContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_completeCommands);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(151);
			completeCommand();
			setState(157);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(152);
					newlineList();
					setState(153);
					completeCommand();
					}
					} 
				}
				setState(159);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
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
	public static class CompleteCommandContext extends ParserRuleContext {
		public ListContext list() {
			return getRuleContext(ListContext.class,0);
		}
		public SeparatorContext separator() {
			return getRuleContext(SeparatorContext.class,0);
		}
		public CompleteCommandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_completeCommand; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterCompleteCommand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitCompleteCommand(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitCompleteCommand(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CompleteCommandContext completeCommand() throws RecognitionException {
		CompleteCommandContext _localctx = new CompleteCommandContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_completeCommand);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(160);
			list();
			setState(162);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SEMI || _la==AMP) {
				{
				setState(161);
				separator();
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
	public static class ListContext extends ParserRuleContext {
		public List<AndOrContext> andOr() {
			return getRuleContexts(AndOrContext.class);
		}
		public AndOrContext andOr(int i) {
			return getRuleContext(AndOrContext.class,i);
		}
		public List<ListSeparatorContext> listSeparator() {
			return getRuleContexts(ListSeparatorContext.class);
		}
		public ListSeparatorContext listSeparator(int i) {
			return getRuleContext(ListSeparatorContext.class,i);
		}
		public ListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ListContext list() throws RecognitionException {
		ListContext _localctx = new ListContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_list);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(164);
			andOr();
			setState(170);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,4,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(165);
					listSeparator();
					setState(166);
					andOr();
					}
					} 
				}
				setState(172);
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
	public static class ListSeparatorContext extends ParserRuleContext {
		public TerminalNode AND() { return getToken(BashParser.AND, 0); }
		public LinebreakContext linebreak() {
			return getRuleContext(LinebreakContext.class,0);
		}
		public TerminalNode OR() { return getToken(BashParser.OR, 0); }
		public TerminalNode SEMI() { return getToken(BashParser.SEMI, 0); }
		public TerminalNode AMP() { return getToken(BashParser.AMP, 0); }
		public ListSeparatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_listSeparator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterListSeparator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitListSeparator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitListSeparator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ListSeparatorContext listSeparator() throws RecognitionException {
		ListSeparatorContext _localctx = new ListSeparatorContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_listSeparator);
		try {
			setState(181);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case AND:
				enterOuterAlt(_localctx, 1);
				{
				setState(173);
				match(AND);
				setState(174);
				linebreak();
				}
				break;
			case OR:
				enterOuterAlt(_localctx, 2);
				{
				setState(175);
				match(OR);
				setState(176);
				linebreak();
				}
				break;
			case SEMI:
				enterOuterAlt(_localctx, 3);
				{
				setState(177);
				match(SEMI);
				setState(178);
				linebreak();
				}
				break;
			case AMP:
				enterOuterAlt(_localctx, 4);
				{
				setState(179);
				match(AMP);
				setState(180);
				linebreak();
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
	public static class AndOrContext extends ParserRuleContext {
		public PipelineContext pipeline() {
			return getRuleContext(PipelineContext.class,0);
		}
		public AndOrContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_andOr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterAndOr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitAndOr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitAndOr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AndOrContext andOr() throws RecognitionException {
		AndOrContext _localctx = new AndOrContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_andOr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(183);
			pipeline();
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
	public static class PipelineContext extends ParserRuleContext {
		public PipeSequenceContext pipeSequence() {
			return getRuleContext(PipeSequenceContext.class,0);
		}
		public BangOptContext bangOpt() {
			return getRuleContext(BangOptContext.class,0);
		}
		public TimeOptContext timeOpt() {
			return getRuleContext(TimeOptContext.class,0);
		}
		public PipelineContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pipeline; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterPipeline(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitPipeline(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitPipeline(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PipelineContext pipeline() throws RecognitionException {
		PipelineContext _localctx = new PipelineContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_pipeline);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(186);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,6,_ctx) ) {
			case 1:
				{
				setState(185);
				bangOpt();
				}
				break;
			}
			setState(189);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
			case 1:
				{
				setState(188);
				timeOpt();
				}
				break;
			}
			setState(191);
			pipeSequence();
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
	public static class BangOptContext extends ParserRuleContext {
		public TerminalNode BANG() { return getToken(BashParser.BANG, 0); }
		public BangOptContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_bangOpt; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterBangOpt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitBangOpt(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitBangOpt(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BangOptContext bangOpt() throws RecognitionException {
		BangOptContext _localctx = new BangOptContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_bangOpt);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(193);
			match(BANG);
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
	public static class TimeOptContext extends ParserRuleContext {
		public TerminalNode TIME() { return getToken(BashParser.TIME, 0); }
		public TimeOptContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_timeOpt; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterTimeOpt(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitTimeOpt(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitTimeOpt(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TimeOptContext timeOpt() throws RecognitionException {
		TimeOptContext _localctx = new TimeOptContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_timeOpt);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(195);
			match(TIME);
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
	public static class PipeSequenceContext extends ParserRuleContext {
		public List<CommandContext> command() {
			return getRuleContexts(CommandContext.class);
		}
		public CommandContext command(int i) {
			return getRuleContext(CommandContext.class,i);
		}
		public List<PipeOpContext> pipeOp() {
			return getRuleContexts(PipeOpContext.class);
		}
		public PipeOpContext pipeOp(int i) {
			return getRuleContext(PipeOpContext.class,i);
		}
		public List<LinebreakContext> linebreak() {
			return getRuleContexts(LinebreakContext.class);
		}
		public LinebreakContext linebreak(int i) {
			return getRuleContext(LinebreakContext.class,i);
		}
		public PipeSequenceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pipeSequence; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterPipeSequence(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitPipeSequence(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitPipeSequence(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PipeSequenceContext pipeSequence() throws RecognitionException {
		PipeSequenceContext _localctx = new PipeSequenceContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_pipeSequence);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(197);
			command();
			setState(204);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==PIPE_AND || _la==PIPE) {
				{
				{
				setState(198);
				pipeOp();
				setState(199);
				linebreak();
				setState(200);
				command();
				}
				}
				setState(206);
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
	public static class PipeOpContext extends ParserRuleContext {
		public TerminalNode PIPE() { return getToken(BashParser.PIPE, 0); }
		public TerminalNode PIPE_AND() { return getToken(BashParser.PIPE_AND, 0); }
		public PipeOpContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pipeOp; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterPipeOp(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitPipeOp(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitPipeOp(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PipeOpContext pipeOp() throws RecognitionException {
		PipeOpContext _localctx = new PipeOpContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_pipeOp);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(207);
			_la = _input.LA(1);
			if ( !(_la==PIPE_AND || _la==PIPE) ) {
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
	public static class CommandContext extends ParserRuleContext {
		public CompoundCommandContext compoundCommand() {
			return getRuleContext(CompoundCommandContext.class,0);
		}
		public RedirectionListContext redirectionList() {
			return getRuleContext(RedirectionListContext.class,0);
		}
		public FunctionDefinitionContext functionDefinition() {
			return getRuleContext(FunctionDefinitionContext.class,0);
		}
		public SimpleCommandContext simpleCommand() {
			return getRuleContext(SimpleCommandContext.class,0);
		}
		public CommandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_command; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterCommand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitCommand(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitCommand(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CommandContext command() throws RecognitionException {
		CommandContext _localctx = new CommandContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_command);
		int _la;
		try {
			setState(215);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,10,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(209);
				compoundCommand();
				setState(211);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (((((_la - 29)) & ~0x3f) == 0 && ((1L << (_la - 29)) & 137438956799L) != 0)) {
					{
					setState(210);
					redirectionList();
					}
				}

				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(213);
				functionDefinition();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(214);
				simpleCommand();
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
	public static class SimpleCommandContext extends ParserRuleContext {
		public CmdPrefixContext cmdPrefix() {
			return getRuleContext(CmdPrefixContext.class,0);
		}
		public CmdWordContext cmdWord() {
			return getRuleContext(CmdWordContext.class,0);
		}
		public CmdSuffixContext cmdSuffix() {
			return getRuleContext(CmdSuffixContext.class,0);
		}
		public SimpleCommandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_simpleCommand; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterSimpleCommand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitSimpleCommand(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitSimpleCommand(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SimpleCommandContext simpleCommand() throws RecognitionException {
		SimpleCommandContext _localctx = new SimpleCommandContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_simpleCommand);
		try {
			setState(228);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,14,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(217);
				cmdPrefix();
				setState(219);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
				case 1:
					{
					setState(218);
					cmdWord();
					}
					break;
				}
				setState(222);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,12,_ctx) ) {
				case 1:
					{
					setState(221);
					cmdSuffix();
					}
					break;
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(224);
				cmdWord();
				setState(226);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,13,_ctx) ) {
				case 1:
					{
					setState(225);
					cmdSuffix();
					}
					break;
				}
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
	public static class CmdPrefixContext extends ParserRuleContext {
		public List<AssignmentContext> assignment() {
			return getRuleContexts(AssignmentContext.class);
		}
		public AssignmentContext assignment(int i) {
			return getRuleContext(AssignmentContext.class,i);
		}
		public List<RedirectionContext> redirection() {
			return getRuleContexts(RedirectionContext.class);
		}
		public RedirectionContext redirection(int i) {
			return getRuleContext(RedirectionContext.class,i);
		}
		public CmdPrefixContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_cmdPrefix; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterCmdPrefix(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitCmdPrefix(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitCmdPrefix(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CmdPrefixContext cmdPrefix() throws RecognitionException {
		CmdPrefixContext _localctx = new CmdPrefixContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_cmdPrefix);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(232); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					setState(232);
					_errHandler.sync(this);
					switch (_input.LA(1)) {
					case WORD:
						{
						setState(230);
						assignment();
						}
						break;
					case HERESTRING:
					case DLESSDASH:
					case DLESS:
					case DGREAT:
					case LESSAND:
					case GREATAND:
					case LESSGREAT:
					case CLOBBER:
					case LESS:
					case GREAT:
					case NUMBER:
						{
						setState(231);
						redirection();
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(234); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,16,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
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
	public static class CmdWordContext extends ParserRuleContext {
		public WordContext word() {
			return getRuleContext(WordContext.class,0);
		}
		public CmdWordContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_cmdWord; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterCmdWord(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitCmdWord(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitCmdWord(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CmdWordContext cmdWord() throws RecognitionException {
		CmdWordContext _localctx = new CmdWordContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_cmdWord);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(236);
			word();
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
	public static class CmdSuffixContext extends ParserRuleContext {
		public List<AssignmentContext> assignment() {
			return getRuleContexts(AssignmentContext.class);
		}
		public AssignmentContext assignment(int i) {
			return getRuleContext(AssignmentContext.class,i);
		}
		public List<WordContext> word() {
			return getRuleContexts(WordContext.class);
		}
		public WordContext word(int i) {
			return getRuleContext(WordContext.class,i);
		}
		public List<RedirectionContext> redirection() {
			return getRuleContexts(RedirectionContext.class);
		}
		public RedirectionContext redirection(int i) {
			return getRuleContext(RedirectionContext.class,i);
		}
		public CmdSuffixContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_cmdSuffix; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterCmdSuffix(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitCmdSuffix(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitCmdSuffix(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CmdSuffixContext cmdSuffix() throws RecognitionException {
		CmdSuffixContext _localctx = new CmdSuffixContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_cmdSuffix);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(241); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					setState(241);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,17,_ctx) ) {
					case 1:
						{
						setState(238);
						assignment();
						}
						break;
					case 2:
						{
						setState(239);
						word();
						}
						break;
					case 3:
						{
						setState(240);
						redirection();
						}
						break;
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(243); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,18,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
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
	public static class CompoundCommandContext extends ParserRuleContext {
		public BraceGroupContext braceGroup() {
			return getRuleContext(BraceGroupContext.class,0);
		}
		public SubshellContext subshell() {
			return getRuleContext(SubshellContext.class,0);
		}
		public IfClauseContext ifClause() {
			return getRuleContext(IfClauseContext.class,0);
		}
		public ForClauseContext forClause() {
			return getRuleContext(ForClauseContext.class,0);
		}
		public CStyleForClauseContext cStyleForClause() {
			return getRuleContext(CStyleForClauseContext.class,0);
		}
		public WhileClauseContext whileClause() {
			return getRuleContext(WhileClauseContext.class,0);
		}
		public UntilClauseContext untilClause() {
			return getRuleContext(UntilClauseContext.class,0);
		}
		public CaseClauseContext caseClause() {
			return getRuleContext(CaseClauseContext.class,0);
		}
		public DoubleParenExprContext doubleParenExpr() {
			return getRuleContext(DoubleParenExprContext.class,0);
		}
		public DoubleBracketExprContext doubleBracketExpr() {
			return getRuleContext(DoubleBracketExprContext.class,0);
		}
		public CompoundCommandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_compoundCommand; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterCompoundCommand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitCompoundCommand(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitCompoundCommand(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CompoundCommandContext compoundCommand() throws RecognitionException {
		CompoundCommandContext _localctx = new CompoundCommandContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_compoundCommand);
		try {
			setState(255);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,19,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(245);
				braceGroup();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(246);
				subshell();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(247);
				ifClause();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(248);
				forClause();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(249);
				cStyleForClause();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(250);
				whileClause();
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(251);
				untilClause();
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(252);
				caseClause();
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(253);
				doubleParenExpr();
				}
				break;
			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(254);
				doubleBracketExpr();
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
	public static class BraceGroupContext extends ParserRuleContext {
		public TerminalNode LBRACE() { return getToken(BashParser.LBRACE, 0); }
		public CompoundListContext compoundList() {
			return getRuleContext(CompoundListContext.class,0);
		}
		public TerminalNode RBRACE() { return getToken(BashParser.RBRACE, 0); }
		public BraceGroupContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_braceGroup; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterBraceGroup(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitBraceGroup(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitBraceGroup(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BraceGroupContext braceGroup() throws RecognitionException {
		BraceGroupContext _localctx = new BraceGroupContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_braceGroup);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(257);
			match(LBRACE);
			setState(258);
			compoundList();
			setState(259);
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
	public static class SubshellContext extends ParserRuleContext {
		public TerminalNode LPAREN() { return getToken(BashParser.LPAREN, 0); }
		public CompoundListContext compoundList() {
			return getRuleContext(CompoundListContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(BashParser.RPAREN, 0); }
		public SubshellContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_subshell; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterSubshell(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitSubshell(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitSubshell(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SubshellContext subshell() throws RecognitionException {
		SubshellContext _localctx = new SubshellContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_subshell);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(261);
			match(LPAREN);
			setState(262);
			compoundList();
			setState(263);
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
	public static class CompoundListContext extends ParserRuleContext {
		public List<LinebreakContext> linebreak() {
			return getRuleContexts(LinebreakContext.class);
		}
		public LinebreakContext linebreak(int i) {
			return getRuleContext(LinebreakContext.class,i);
		}
		public CompleteCommandsContext completeCommands() {
			return getRuleContext(CompleteCommandsContext.class,0);
		}
		public CompoundListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_compoundList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterCompoundList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitCompoundList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitCompoundList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CompoundListContext compoundList() throws RecognitionException {
		CompoundListContext _localctx = new CompoundListContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_compoundList);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(265);
			linebreak();
			setState(266);
			completeCommands();
			setState(267);
			linebreak();
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
	public static class IfClauseContext extends ParserRuleContext {
		public TerminalNode IF() { return getToken(BashParser.IF, 0); }
		public List<CompoundListContext> compoundList() {
			return getRuleContexts(CompoundListContext.class);
		}
		public CompoundListContext compoundList(int i) {
			return getRuleContext(CompoundListContext.class,i);
		}
		public TerminalNode THEN() { return getToken(BashParser.THEN, 0); }
		public TerminalNode FI() { return getToken(BashParser.FI, 0); }
		public List<ElifClauseContext> elifClause() {
			return getRuleContexts(ElifClauseContext.class);
		}
		public ElifClauseContext elifClause(int i) {
			return getRuleContext(ElifClauseContext.class,i);
		}
		public ElseClauseContext elseClause() {
			return getRuleContext(ElseClauseContext.class,0);
		}
		public IfClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ifClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterIfClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitIfClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitIfClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IfClauseContext ifClause() throws RecognitionException {
		IfClauseContext _localctx = new IfClauseContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_ifClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(269);
			match(IF);
			setState(270);
			compoundList();
			setState(271);
			match(THEN);
			setState(272);
			compoundList();
			setState(276);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ELIF) {
				{
				{
				setState(273);
				elifClause();
				}
				}
				setState(278);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(280);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ELSE) {
				{
				setState(279);
				elseClause();
				}
			}

			setState(282);
			match(FI);
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
	public static class ElifClauseContext extends ParserRuleContext {
		public TerminalNode ELIF() { return getToken(BashParser.ELIF, 0); }
		public List<CompoundListContext> compoundList() {
			return getRuleContexts(CompoundListContext.class);
		}
		public CompoundListContext compoundList(int i) {
			return getRuleContext(CompoundListContext.class,i);
		}
		public TerminalNode THEN() { return getToken(BashParser.THEN, 0); }
		public ElifClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_elifClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterElifClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitElifClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitElifClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ElifClauseContext elifClause() throws RecognitionException {
		ElifClauseContext _localctx = new ElifClauseContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_elifClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(284);
			match(ELIF);
			setState(285);
			compoundList();
			setState(286);
			match(THEN);
			setState(287);
			compoundList();
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
	public static class ElseClauseContext extends ParserRuleContext {
		public TerminalNode ELSE() { return getToken(BashParser.ELSE, 0); }
		public CompoundListContext compoundList() {
			return getRuleContext(CompoundListContext.class,0);
		}
		public ElseClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_elseClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterElseClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitElseClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitElseClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ElseClauseContext elseClause() throws RecognitionException {
		ElseClauseContext _localctx = new ElseClauseContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_elseClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(289);
			match(ELSE);
			setState(290);
			compoundList();
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
	public static class ForClauseContext extends ParserRuleContext {
		public TerminalNode FOR() { return getToken(BashParser.FOR, 0); }
		public TerminalNode WORD() { return getToken(BashParser.WORD, 0); }
		public ForBodyContext forBody() {
			return getRuleContext(ForBodyContext.class,0);
		}
		public ForClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_forClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterForClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitForClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitForClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ForClauseContext forClause() throws RecognitionException {
		ForClauseContext _localctx = new ForClauseContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_forClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(292);
			match(FOR);
			setState(293);
			match(WORD);
			setState(294);
			forBody();
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
	public static class ForBodyContext extends ParserRuleContext {
		public InClauseContext inClause() {
			return getRuleContext(InClauseContext.class,0);
		}
		public DoGroupContext doGroup() {
			return getRuleContext(DoGroupContext.class,0);
		}
		public ForBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_forBody; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterForBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitForBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitForBody(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ForBodyContext forBody() throws RecognitionException {
		ForBodyContext _localctx = new ForBodyContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_forBody);
		try {
			setState(300);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case NEWLINE:
			case COMMENT:
			case IN:
				enterOuterAlt(_localctx, 1);
				{
				setState(296);
				inClause();
				setState(297);
				doGroup();
				}
				break;
			case DO:
				enterOuterAlt(_localctx, 2);
				{
				setState(299);
				doGroup();
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
	public static class InClauseContext extends ParserRuleContext {
		public LinebreakContext linebreak() {
			return getRuleContext(LinebreakContext.class,0);
		}
		public TerminalNode IN() { return getToken(BashParser.IN, 0); }
		public SequentialSepContext sequentialSep() {
			return getRuleContext(SequentialSepContext.class,0);
		}
		public List<WordContext> word() {
			return getRuleContexts(WordContext.class);
		}
		public WordContext word(int i) {
			return getRuleContext(WordContext.class,i);
		}
		public InClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterInClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitInClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitInClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InClauseContext inClause() throws RecognitionException {
		InClauseContext _localctx = new InClauseContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_inClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(302);
			linebreak();
			setState(303);
			match(IN);
			setState(307);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & -50665083017101344L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 255L) != 0)) {
				{
				{
				setState(304);
				word();
				}
				}
				setState(309);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(310);
			sequentialSep();
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
	public static class CStyleForClauseContext extends ParserRuleContext {
		public TerminalNode FOR() { return getToken(BashParser.FOR, 0); }
		public TerminalNode DOUBLE_LPAREN() { return getToken(BashParser.DOUBLE_LPAREN, 0); }
		public List<TerminalNode> SEMI() { return getTokens(BashParser.SEMI); }
		public TerminalNode SEMI(int i) {
			return getToken(BashParser.SEMI, i);
		}
		public TerminalNode DOUBLE_RPAREN() { return getToken(BashParser.DOUBLE_RPAREN, 0); }
		public DoGroupContext doGroup() {
			return getRuleContext(DoGroupContext.class,0);
		}
		public List<ArithmeticExprContext> arithmeticExpr() {
			return getRuleContexts(ArithmeticExprContext.class);
		}
		public ArithmeticExprContext arithmeticExpr(int i) {
			return getRuleContext(ArithmeticExprContext.class,i);
		}
		public SequentialSepContext sequentialSep() {
			return getRuleContext(SequentialSepContext.class,0);
		}
		public CStyleForClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_cStyleForClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterCStyleForClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitCStyleForClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitCStyleForClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CStyleForClauseContext cStyleForClause() throws RecognitionException {
		CStyleForClauseContext _localctx = new CStyleForClauseContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_cStyleForClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(312);
			match(FOR);
			setState(313);
			match(DOUBLE_LPAREN);
			setState(315);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,24,_ctx) ) {
			case 1:
				{
				setState(314);
				arithmeticExpr();
				}
				break;
			}
			setState(317);
			match(SEMI);
			setState(319);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,25,_ctx) ) {
			case 1:
				{
				setState(318);
				arithmeticExpr();
				}
				break;
			}
			setState(321);
			match(SEMI);
			setState(323);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((((_la - 26)) & ~0x3f) == 0 && ((1L << (_la - 26)) & 69388548300807L) != 0)) {
				{
				setState(322);
				arithmeticExpr();
				}
			}

			setState(325);
			match(DOUBLE_RPAREN);
			setState(327);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 281474976710680L) != 0)) {
				{
				setState(326);
				sequentialSep();
				}
			}

			setState(329);
			doGroup();
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
	public static class WhileClauseContext extends ParserRuleContext {
		public TerminalNode WHILE() { return getToken(BashParser.WHILE, 0); }
		public CompoundListContext compoundList() {
			return getRuleContext(CompoundListContext.class,0);
		}
		public DoGroupContext doGroup() {
			return getRuleContext(DoGroupContext.class,0);
		}
		public WhileClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_whileClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterWhileClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitWhileClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitWhileClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WhileClauseContext whileClause() throws RecognitionException {
		WhileClauseContext _localctx = new WhileClauseContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_whileClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(331);
			match(WHILE);
			setState(332);
			compoundList();
			setState(333);
			doGroup();
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
	public static class UntilClauseContext extends ParserRuleContext {
		public TerminalNode UNTIL() { return getToken(BashParser.UNTIL, 0); }
		public CompoundListContext compoundList() {
			return getRuleContext(CompoundListContext.class,0);
		}
		public DoGroupContext doGroup() {
			return getRuleContext(DoGroupContext.class,0);
		}
		public UntilClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_untilClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterUntilClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitUntilClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitUntilClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UntilClauseContext untilClause() throws RecognitionException {
		UntilClauseContext _localctx = new UntilClauseContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_untilClause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(335);
			match(UNTIL);
			setState(336);
			compoundList();
			setState(337);
			doGroup();
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
	public static class DoGroupContext extends ParserRuleContext {
		public TerminalNode DO() { return getToken(BashParser.DO, 0); }
		public CompoundListContext compoundList() {
			return getRuleContext(CompoundListContext.class,0);
		}
		public TerminalNode DONE() { return getToken(BashParser.DONE, 0); }
		public DoGroupContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_doGroup; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterDoGroup(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitDoGroup(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitDoGroup(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DoGroupContext doGroup() throws RecognitionException {
		DoGroupContext _localctx = new DoGroupContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_doGroup);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(339);
			match(DO);
			setState(340);
			compoundList();
			setState(341);
			match(DONE);
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
	public static class CaseClauseContext extends ParserRuleContext {
		public TerminalNode CASE() { return getToken(BashParser.CASE, 0); }
		public WordContext word() {
			return getRuleContext(WordContext.class,0);
		}
		public List<LinebreakContext> linebreak() {
			return getRuleContexts(LinebreakContext.class);
		}
		public LinebreakContext linebreak(int i) {
			return getRuleContext(LinebreakContext.class,i);
		}
		public TerminalNode IN() { return getToken(BashParser.IN, 0); }
		public TerminalNode ESAC() { return getToken(BashParser.ESAC, 0); }
		public List<CaseItemContext> caseItem() {
			return getRuleContexts(CaseItemContext.class);
		}
		public CaseItemContext caseItem(int i) {
			return getRuleContext(CaseItemContext.class,i);
		}
		public CaseClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_caseClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterCaseClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitCaseClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitCaseClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CaseClauseContext caseClause() throws RecognitionException {
		CaseClauseContext _localctx = new CaseClauseContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_caseClause);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(343);
			match(CASE);
			setState(344);
			word();
			setState(345);
			linebreak();
			setState(346);
			match(IN);
			setState(347);
			linebreak();
			setState(351);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,28,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(348);
					caseItem();
					}
					} 
				}
				setState(353);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,28,_ctx);
			}
			setState(354);
			match(ESAC);
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
	public static class CaseItemContext extends ParserRuleContext {
		public PatternContext pattern() {
			return getRuleContext(PatternContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(BashParser.RPAREN, 0); }
		public List<LinebreakContext> linebreak() {
			return getRuleContexts(LinebreakContext.class);
		}
		public LinebreakContext linebreak(int i) {
			return getRuleContext(LinebreakContext.class,i);
		}
		public CaseSeparatorContext caseSeparator() {
			return getRuleContext(CaseSeparatorContext.class,0);
		}
		public TerminalNode LPAREN() { return getToken(BashParser.LPAREN, 0); }
		public CompoundListContext compoundList() {
			return getRuleContext(CompoundListContext.class,0);
		}
		public CaseItemContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_caseItem; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterCaseItem(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitCaseItem(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitCaseItem(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CaseItemContext caseItem() throws RecognitionException {
		CaseItemContext _localctx = new CaseItemContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_caseItem);
		int _la;
		try {
			setState(375);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,32,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(357);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LPAREN) {
					{
					setState(356);
					match(LPAREN);
					}
				}

				setState(359);
				pattern();
				setState(360);
				match(RPAREN);
				setState(361);
				linebreak();
				setState(363);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -46159697199235080L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 255L) != 0)) {
					{
					setState(362);
					compoundList();
					}
				}

				setState(365);
				caseSeparator();
				setState(366);
				linebreak();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(369);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LPAREN) {
					{
					setState(368);
					match(LPAREN);
					}
				}

				setState(371);
				pattern();
				setState(372);
				match(RPAREN);
				setState(373);
				linebreak();
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
	public static class CaseSeparatorContext extends ParserRuleContext {
		public TerminalNode DSEMI() { return getToken(BashParser.DSEMI, 0); }
		public TerminalNode SEMI_AND() { return getToken(BashParser.SEMI_AND, 0); }
		public TerminalNode DSEMI_AND() { return getToken(BashParser.DSEMI_AND, 0); }
		public CaseSeparatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_caseSeparator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterCaseSeparator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitCaseSeparator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitCaseSeparator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CaseSeparatorContext caseSeparator() throws RecognitionException {
		CaseSeparatorContext _localctx = new CaseSeparatorContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_caseSeparator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(377);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 246290604621824L) != 0)) ) {
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
	public static class PatternContext extends ParserRuleContext {
		public List<WordContext> word() {
			return getRuleContexts(WordContext.class);
		}
		public WordContext word(int i) {
			return getRuleContext(WordContext.class,i);
		}
		public List<TerminalNode> PIPE() { return getTokens(BashParser.PIPE); }
		public TerminalNode PIPE(int i) {
			return getToken(BashParser.PIPE, i);
		}
		public PatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PatternContext pattern() throws RecognitionException {
		PatternContext _localctx = new PatternContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_pattern);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(379);
			word();
			setState(384);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==PIPE) {
				{
				{
				setState(380);
				match(PIPE);
				setState(381);
				word();
				}
				}
				setState(386);
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
	public static class DoubleParenExprContext extends ParserRuleContext {
		public TerminalNode DOUBLE_LPAREN() { return getToken(BashParser.DOUBLE_LPAREN, 0); }
		public ArithmeticExprContext arithmeticExpr() {
			return getRuleContext(ArithmeticExprContext.class,0);
		}
		public TerminalNode DOUBLE_RPAREN() { return getToken(BashParser.DOUBLE_RPAREN, 0); }
		public DoubleParenExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_doubleParenExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterDoubleParenExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitDoubleParenExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitDoubleParenExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DoubleParenExprContext doubleParenExpr() throws RecognitionException {
		DoubleParenExprContext _localctx = new DoubleParenExprContext(_ctx, getState());
		enterRule(_localctx, 70, RULE_doubleParenExpr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(387);
			match(DOUBLE_LPAREN);
			setState(388);
			arithmeticExpr();
			setState(389);
			match(DOUBLE_RPAREN);
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
	public static class ArithmeticExprContext extends ParserRuleContext {
		public List<ArithmeticPartContext> arithmeticPart() {
			return getRuleContexts(ArithmeticPartContext.class);
		}
		public ArithmeticPartContext arithmeticPart(int i) {
			return getRuleContext(ArithmeticPartContext.class,i);
		}
		public ArithmeticExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arithmeticExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterArithmeticExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitArithmeticExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitArithmeticExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArithmeticExprContext arithmeticExpr() throws RecognitionException {
		ArithmeticExprContext _localctx = new ArithmeticExprContext(_ctx, getState());
		enterRule(_localctx, 72, RULE_arithmeticExpr);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(392); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(391);
					arithmeticPart();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(394); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,34,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
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
	public static class ArithmeticPartContext extends ParserRuleContext {
		public TerminalNode WORD() { return getToken(BashParser.WORD, 0); }
		public TerminalNode NUMBER() { return getToken(BashParser.NUMBER, 0); }
		public TerminalNode DOLLAR_NAME() { return getToken(BashParser.DOLLAR_NAME, 0); }
		public TerminalNode SPECIAL_VAR() { return getToken(BashParser.SPECIAL_VAR, 0); }
		public TerminalNode DOLLAR_LBRACE() { return getToken(BashParser.DOLLAR_LBRACE, 0); }
		public BraceExpansionContentContext braceExpansionContent() {
			return getRuleContext(BraceExpansionContentContext.class,0);
		}
		public TerminalNode RBRACE() { return getToken(BashParser.RBRACE, 0); }
		public TerminalNode DOLLAR_LPAREN() { return getToken(BashParser.DOLLAR_LPAREN, 0); }
		public CommandSubstitutionContentContext commandSubstitutionContent() {
			return getRuleContext(CommandSubstitutionContentContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(BashParser.RPAREN, 0); }
		public TerminalNode DOLLAR_DPAREN() { return getToken(BashParser.DOLLAR_DPAREN, 0); }
		public ArithmeticExprContext arithmeticExpr() {
			return getRuleContext(ArithmeticExprContext.class,0);
		}
		public TerminalNode DOUBLE_RPAREN() { return getToken(BashParser.DOUBLE_RPAREN, 0); }
		public TerminalNode LPAREN() { return getToken(BashParser.LPAREN, 0); }
		public TerminalNode EQUALS() { return getToken(BashParser.EQUALS, 0); }
		public TerminalNode PLUS_EQUALS() { return getToken(BashParser.PLUS_EQUALS, 0); }
		public TerminalNode BANG() { return getToken(BashParser.BANG, 0); }
		public TerminalNode LESS() { return getToken(BashParser.LESS, 0); }
		public TerminalNode GREAT() { return getToken(BashParser.GREAT, 0); }
		public TerminalNode STAR() { return getToken(BashParser.STAR, 0); }
		public TerminalNode QUESTION() { return getToken(BashParser.QUESTION, 0); }
		public TerminalNode AND() { return getToken(BashParser.AND, 0); }
		public TerminalNode OR() { return getToken(BashParser.OR, 0); }
		public TerminalNode AMP() { return getToken(BashParser.AMP, 0); }
		public TerminalNode PIPE() { return getToken(BashParser.PIPE, 0); }
		public TerminalNode COLON() { return getToken(BashParser.COLON, 0); }
		public TerminalNode SEMI() { return getToken(BashParser.SEMI, 0); }
		public TerminalNode LBRACKET() { return getToken(BashParser.LBRACKET, 0); }
		public TerminalNode RBRACKET() { return getToken(BashParser.RBRACKET, 0); }
		public TerminalNode TILDE() { return getToken(BashParser.TILDE, 0); }
		public TerminalNode DOLLAR() { return getToken(BashParser.DOLLAR, 0); }
		public ArithmeticPartContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arithmeticPart; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterArithmeticPart(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitArithmeticPart(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitArithmeticPart(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArithmeticPartContext arithmeticPart() throws RecognitionException {
		ArithmeticPartContext _localctx = new ArithmeticPartContext(_ctx, getState());
		enterRule(_localctx, 74, RULE_arithmeticPart);
		try {
			setState(433);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case WORD:
				enterOuterAlt(_localctx, 1);
				{
				setState(396);
				match(WORD);
				}
				break;
			case NUMBER:
				enterOuterAlt(_localctx, 2);
				{
				setState(397);
				match(NUMBER);
				}
				break;
			case DOLLAR_NAME:
				enterOuterAlt(_localctx, 3);
				{
				setState(398);
				match(DOLLAR_NAME);
				}
				break;
			case SPECIAL_VAR:
				enterOuterAlt(_localctx, 4);
				{
				setState(399);
				match(SPECIAL_VAR);
				}
				break;
			case DOLLAR_LBRACE:
				enterOuterAlt(_localctx, 5);
				{
				setState(400);
				match(DOLLAR_LBRACE);
				setState(401);
				braceExpansionContent();
				setState(402);
				match(RBRACE);
				}
				break;
			case DOLLAR_LPAREN:
				enterOuterAlt(_localctx, 6);
				{
				setState(404);
				match(DOLLAR_LPAREN);
				setState(405);
				commandSubstitutionContent();
				setState(406);
				match(RPAREN);
				}
				break;
			case DOLLAR_DPAREN:
				enterOuterAlt(_localctx, 7);
				{
				setState(408);
				match(DOLLAR_DPAREN);
				setState(409);
				arithmeticExpr();
				setState(410);
				match(DOUBLE_RPAREN);
				}
				break;
			case LPAREN:
				enterOuterAlt(_localctx, 8);
				{
				setState(412);
				match(LPAREN);
				setState(413);
				arithmeticExpr();
				setState(414);
				match(RPAREN);
				}
				break;
			case EQUALS:
				enterOuterAlt(_localctx, 9);
				{
				setState(416);
				match(EQUALS);
				}
				break;
			case PLUS_EQUALS:
				enterOuterAlt(_localctx, 10);
				{
				setState(417);
				match(PLUS_EQUALS);
				}
				break;
			case BANG:
				enterOuterAlt(_localctx, 11);
				{
				setState(418);
				match(BANG);
				}
				break;
			case LESS:
				enterOuterAlt(_localctx, 12);
				{
				setState(419);
				match(LESS);
				}
				break;
			case GREAT:
				enterOuterAlt(_localctx, 13);
				{
				setState(420);
				match(GREAT);
				}
				break;
			case STAR:
				enterOuterAlt(_localctx, 14);
				{
				setState(421);
				match(STAR);
				}
				break;
			case QUESTION:
				enterOuterAlt(_localctx, 15);
				{
				setState(422);
				match(QUESTION);
				}
				break;
			case AND:
				enterOuterAlt(_localctx, 16);
				{
				setState(423);
				match(AND);
				}
				break;
			case OR:
				enterOuterAlt(_localctx, 17);
				{
				setState(424);
				match(OR);
				}
				break;
			case AMP:
				enterOuterAlt(_localctx, 18);
				{
				setState(425);
				match(AMP);
				}
				break;
			case PIPE:
				enterOuterAlt(_localctx, 19);
				{
				setState(426);
				match(PIPE);
				}
				break;
			case COLON:
				enterOuterAlt(_localctx, 20);
				{
				setState(427);
				match(COLON);
				}
				break;
			case SEMI:
				enterOuterAlt(_localctx, 21);
				{
				setState(428);
				match(SEMI);
				}
				break;
			case LBRACKET:
				enterOuterAlt(_localctx, 22);
				{
				setState(429);
				match(LBRACKET);
				}
				break;
			case RBRACKET:
				enterOuterAlt(_localctx, 23);
				{
				setState(430);
				match(RBRACKET);
				}
				break;
			case TILDE:
				enterOuterAlt(_localctx, 24);
				{
				setState(431);
				match(TILDE);
				}
				break;
			case DOLLAR:
				enterOuterAlt(_localctx, 25);
				{
				setState(432);
				match(DOLLAR);
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
	public static class DoubleBracketExprContext extends ParserRuleContext {
		public TerminalNode DOUBLE_LBRACKET() { return getToken(BashParser.DOUBLE_LBRACKET, 0); }
		public ConditionExprContext conditionExpr() {
			return getRuleContext(ConditionExprContext.class,0);
		}
		public TerminalNode DOUBLE_RBRACKET() { return getToken(BashParser.DOUBLE_RBRACKET, 0); }
		public DoubleBracketExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_doubleBracketExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterDoubleBracketExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitDoubleBracketExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitDoubleBracketExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DoubleBracketExprContext doubleBracketExpr() throws RecognitionException {
		DoubleBracketExprContext _localctx = new DoubleBracketExprContext(_ctx, getState());
		enterRule(_localctx, 76, RULE_doubleBracketExpr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(435);
			match(DOUBLE_LBRACKET);
			setState(436);
			conditionExpr();
			setState(437);
			match(DOUBLE_RBRACKET);
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
	public static class ConditionExprContext extends ParserRuleContext {
		public List<ConditionPartContext> conditionPart() {
			return getRuleContexts(ConditionPartContext.class);
		}
		public ConditionPartContext conditionPart(int i) {
			return getRuleContext(ConditionPartContext.class,i);
		}
		public ConditionExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_conditionExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterConditionExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitConditionExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitConditionExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConditionExprContext conditionExpr() throws RecognitionException {
		ConditionExprContext _localctx = new ConditionExprContext(_ctx, getState());
		enterRule(_localctx, 78, RULE_conditionExpr);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(440); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(439);
					conditionPart();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(442); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,36,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
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
	public static class ConditionPartContext extends ParserRuleContext {
		public WordContext word() {
			return getRuleContext(WordContext.class,0);
		}
		public TerminalNode LESS() { return getToken(BashParser.LESS, 0); }
		public TerminalNode GREAT() { return getToken(BashParser.GREAT, 0); }
		public TerminalNode BANG() { return getToken(BashParser.BANG, 0); }
		public TerminalNode EQUALS() { return getToken(BashParser.EQUALS, 0); }
		public TerminalNode AND() { return getToken(BashParser.AND, 0); }
		public TerminalNode OR() { return getToken(BashParser.OR, 0); }
		public TerminalNode PIPE() { return getToken(BashParser.PIPE, 0); }
		public TerminalNode AMP() { return getToken(BashParser.AMP, 0); }
		public TerminalNode SEMI() { return getToken(BashParser.SEMI, 0); }
		public TerminalNode LBRACE() { return getToken(BashParser.LBRACE, 0); }
		public TerminalNode RBRACE() { return getToken(BashParser.RBRACE, 0); }
		public TerminalNode DOLLAR() { return getToken(BashParser.DOLLAR, 0); }
		public TerminalNode DOUBLE_LBRACKET() { return getToken(BashParser.DOUBLE_LBRACKET, 0); }
		public TerminalNode DOUBLE_RBRACKET() { return getToken(BashParser.DOUBLE_RBRACKET, 0); }
		public TerminalNode DOUBLE_LPAREN() { return getToken(BashParser.DOUBLE_LPAREN, 0); }
		public TerminalNode DOUBLE_RPAREN() { return getToken(BashParser.DOUBLE_RPAREN, 0); }
		public TerminalNode NEWLINE() { return getToken(BashParser.NEWLINE, 0); }
		public TerminalNode COMMENT() { return getToken(BashParser.COMMENT, 0); }
		public TerminalNode LPAREN() { return getToken(BashParser.LPAREN, 0); }
		public ConditionExprContext conditionExpr() {
			return getRuleContext(ConditionExprContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(BashParser.RPAREN, 0); }
		public ConditionPartContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_conditionPart; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterConditionPart(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitConditionPart(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitConditionPart(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConditionPartContext conditionPart() throws RecognitionException {
		ConditionPartContext _localctx = new ConditionPartContext(_ctx, getState());
		enterRule(_localctx, 80, RULE_conditionPart);
		try {
			setState(467);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,37,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(444);
				word();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(445);
				match(LESS);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(446);
				match(GREAT);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(447);
				match(BANG);
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(448);
				match(EQUALS);
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(449);
				match(AND);
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(450);
				match(OR);
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(451);
				match(PIPE);
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(452);
				match(AMP);
				}
				break;
			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(453);
				match(SEMI);
				}
				break;
			case 11:
				enterOuterAlt(_localctx, 11);
				{
				setState(454);
				match(LBRACE);
				}
				break;
			case 12:
				enterOuterAlt(_localctx, 12);
				{
				setState(455);
				match(RBRACE);
				}
				break;
			case 13:
				enterOuterAlt(_localctx, 13);
				{
				setState(456);
				match(DOLLAR);
				}
				break;
			case 14:
				enterOuterAlt(_localctx, 14);
				{
				setState(457);
				match(DOUBLE_LBRACKET);
				}
				break;
			case 15:
				enterOuterAlt(_localctx, 15);
				{
				setState(458);
				match(DOUBLE_RBRACKET);
				}
				break;
			case 16:
				enterOuterAlt(_localctx, 16);
				{
				setState(459);
				match(DOUBLE_LPAREN);
				}
				break;
			case 17:
				enterOuterAlt(_localctx, 17);
				{
				setState(460);
				match(DOUBLE_RPAREN);
				}
				break;
			case 18:
				enterOuterAlt(_localctx, 18);
				{
				setState(461);
				match(NEWLINE);
				}
				break;
			case 19:
				enterOuterAlt(_localctx, 19);
				{
				setState(462);
				match(COMMENT);
				}
				break;
			case 20:
				enterOuterAlt(_localctx, 20);
				{
				setState(463);
				match(LPAREN);
				setState(464);
				conditionExpr();
				setState(465);
				match(RPAREN);
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
	public static class FunctionDefinitionContext extends ParserRuleContext {
		public TerminalNode FUNCTION() { return getToken(BashParser.FUNCTION, 0); }
		public TerminalNode WORD() { return getToken(BashParser.WORD, 0); }
		public LinebreakContext linebreak() {
			return getRuleContext(LinebreakContext.class,0);
		}
		public FunctionBodyContext functionBody() {
			return getRuleContext(FunctionBodyContext.class,0);
		}
		public FunctionParensContext functionParens() {
			return getRuleContext(FunctionParensContext.class,0);
		}
		public FunctionDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterFunctionDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitFunctionDefinition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitFunctionDefinition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionDefinitionContext functionDefinition() throws RecognitionException {
		FunctionDefinitionContext _localctx = new FunctionDefinitionContext(_ctx, getState());
		enterRule(_localctx, 82, RULE_functionDefinition);
		try {
			setState(482);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case FUNCTION:
				enterOuterAlt(_localctx, 1);
				{
				setState(469);
				match(FUNCTION);
				setState(470);
				match(WORD);
				setState(472);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,38,_ctx) ) {
				case 1:
					{
					setState(471);
					functionParens();
					}
					break;
				}
				setState(474);
				linebreak();
				setState(475);
				functionBody();
				}
				break;
			case WORD:
				enterOuterAlt(_localctx, 2);
				{
				setState(477);
				match(WORD);
				setState(478);
				functionParens();
				setState(479);
				linebreak();
				setState(480);
				functionBody();
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
	public static class FunctionParensContext extends ParserRuleContext {
		public TerminalNode LPAREN() { return getToken(BashParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(BashParser.RPAREN, 0); }
		public FunctionParensContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionParens; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterFunctionParens(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitFunctionParens(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitFunctionParens(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionParensContext functionParens() throws RecognitionException {
		FunctionParensContext _localctx = new FunctionParensContext(_ctx, getState());
		enterRule(_localctx, 84, RULE_functionParens);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(484);
			match(LPAREN);
			setState(485);
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
	public static class FunctionBodyContext extends ParserRuleContext {
		public CompoundCommandContext compoundCommand() {
			return getRuleContext(CompoundCommandContext.class,0);
		}
		public RedirectionListContext redirectionList() {
			return getRuleContext(RedirectionListContext.class,0);
		}
		public FunctionBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functionBody; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterFunctionBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitFunctionBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitFunctionBody(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunctionBodyContext functionBody() throws RecognitionException {
		FunctionBodyContext _localctx = new FunctionBodyContext(_ctx, getState());
		enterRule(_localctx, 86, RULE_functionBody);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(487);
			compoundCommand();
			setState(489);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((((_la - 29)) & ~0x3f) == 0 && ((1L << (_la - 29)) & 137438956799L) != 0)) {
				{
				setState(488);
				redirectionList();
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
	public static class AssignmentContext extends ParserRuleContext {
		public TerminalNode WORD() { return getToken(BashParser.WORD, 0); }
		public TerminalNode EQUALS() { return getToken(BashParser.EQUALS, 0); }
		public TerminalNode PLUS_EQUALS() { return getToken(BashParser.PLUS_EQUALS, 0); }
		public AssignmentValueContext assignmentValue() {
			return getRuleContext(AssignmentValueContext.class,0);
		}
		public AssignmentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_assignment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterAssignment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitAssignment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitAssignment(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AssignmentContext assignment() throws RecognitionException {
		AssignmentContext _localctx = new AssignmentContext(_ctx, getState());
		enterRule(_localctx, 88, RULE_assignment);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(491);
			match(WORD);
			setState(492);
			_la = _input.LA(1);
			if ( !(_la==PLUS_EQUALS || _la==EQUALS) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(494);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,41,_ctx) ) {
			case 1:
				{
				setState(493);
				assignmentValue();
				}
				break;
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
	public static class AssignmentValueContext extends ParserRuleContext {
		public WordContext word() {
			return getRuleContext(WordContext.class,0);
		}
		public TerminalNode LPAREN() { return getToken(BashParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(BashParser.RPAREN, 0); }
		public ArrayElementsContext arrayElements() {
			return getRuleContext(ArrayElementsContext.class,0);
		}
		public AssignmentValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_assignmentValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterAssignmentValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitAssignmentValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitAssignmentValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AssignmentValueContext assignmentValue() throws RecognitionException {
		AssignmentValueContext _localctx = new AssignmentValueContext(_ctx, getState());
		enterRule(_localctx, 90, RULE_assignmentValue);
		int _la;
		try {
			setState(502);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case IF:
			case THEN:
			case ELSE:
			case ELIF:
			case FI:
			case FOR:
			case WHILE:
			case UNTIL:
			case DO:
			case DONE:
			case CASE:
			case ESAC:
			case IN:
			case FUNCTION:
			case SELECT:
			case COPROC:
			case TIME:
			case DOLLAR_DPAREN:
			case DOLLAR_LPAREN:
			case DOLLAR_LBRACE:
			case PROC_SUBST_IN:
			case PROC_SUBST_OUT:
			case PLUS_EQUALS:
			case EQUALS:
			case LBRACE:
			case LBRACKET:
			case RBRACKET:
			case BANG:
			case COLON:
			case BACKTICK:
			case SPECIAL_VAR:
			case DOLLAR_NAME:
			case DOLLAR_SINGLE_QUOTED:
			case SINGLE_QUOTED_STRING:
			case DOUBLE_QUOTE:
			case NUMBER:
			case WORD:
			case DOLLAR:
			case STAR:
			case QUESTION:
			case TILDE:
				enterOuterAlt(_localctx, 1);
				{
				setState(496);
				word();
				}
				break;
			case LPAREN:
				enterOuterAlt(_localctx, 2);
				{
				setState(497);
				match(LPAREN);
				setState(499);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -50665083017101320L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 255L) != 0)) {
					{
					setState(498);
					arrayElements();
					}
				}

				setState(501);
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
	public static class ArrayElementsContext extends ParserRuleContext {
		public List<LinebreakContext> linebreak() {
			return getRuleContexts(LinebreakContext.class);
		}
		public LinebreakContext linebreak(int i) {
			return getRuleContext(LinebreakContext.class,i);
		}
		public List<WordContext> word() {
			return getRuleContexts(WordContext.class);
		}
		public WordContext word(int i) {
			return getRuleContext(WordContext.class,i);
		}
		public ArrayElementsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arrayElements; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterArrayElements(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitArrayElements(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitArrayElements(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArrayElementsContext arrayElements() throws RecognitionException {
		ArrayElementsContext _localctx = new ArrayElementsContext(_ctx, getState());
		enterRule(_localctx, 92, RULE_arrayElements);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(504);
			linebreak();
			setState(508); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(505);
				word();
				setState(506);
				linebreak();
				}
				}
				setState(510); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & -50665083017101344L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 255L) != 0) );
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
	public static class RedirectionListContext extends ParserRuleContext {
		public List<RedirectionContext> redirection() {
			return getRuleContexts(RedirectionContext.class);
		}
		public RedirectionContext redirection(int i) {
			return getRuleContext(RedirectionContext.class,i);
		}
		public RedirectionListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_redirectionList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterRedirectionList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitRedirectionList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitRedirectionList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RedirectionListContext redirectionList() throws RecognitionException {
		RedirectionListContext _localctx = new RedirectionListContext(_ctx, getState());
		enterRule(_localctx, 94, RULE_redirectionList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(513); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(512);
				redirection();
				}
				}
				setState(515); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( ((((_la - 29)) & ~0x3f) == 0 && ((1L << (_la - 29)) & 137438956799L) != 0) );
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
	public static class RedirectionContext extends ParserRuleContext {
		public RedirectionOpContext redirectionOp() {
			return getRuleContext(RedirectionOpContext.class,0);
		}
		public WordContext word() {
			return getRuleContext(WordContext.class,0);
		}
		public TerminalNode NUMBER() { return getToken(BashParser.NUMBER, 0); }
		public TerminalNode HERESTRING() { return getToken(BashParser.HERESTRING, 0); }
		public HeredocContext heredoc() {
			return getRuleContext(HeredocContext.class,0);
		}
		public RedirectionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_redirection; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterRedirection(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitRedirection(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitRedirection(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RedirectionContext redirection() throws RecognitionException {
		RedirectionContext _localctx = new RedirectionContext(_ctx, getState());
		enterRule(_localctx, 96, RULE_redirection);
		int _la;
		try {
			setState(532);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,49,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(518);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NUMBER) {
					{
					setState(517);
					match(NUMBER);
					}
				}

				setState(520);
				redirectionOp();
				setState(521);
				word();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(524);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NUMBER) {
					{
					setState(523);
					match(NUMBER);
					}
				}

				setState(526);
				match(HERESTRING);
				setState(527);
				word();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(529);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==NUMBER) {
					{
					setState(528);
					match(NUMBER);
					}
				}

				setState(531);
				heredoc();
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
	public static class RedirectionOpContext extends ParserRuleContext {
		public TerminalNode LESS() { return getToken(BashParser.LESS, 0); }
		public TerminalNode GREAT() { return getToken(BashParser.GREAT, 0); }
		public TerminalNode DGREAT() { return getToken(BashParser.DGREAT, 0); }
		public TerminalNode LESSAND() { return getToken(BashParser.LESSAND, 0); }
		public TerminalNode GREATAND() { return getToken(BashParser.GREATAND, 0); }
		public TerminalNode LESSGREAT() { return getToken(BashParser.LESSGREAT, 0); }
		public TerminalNode CLOBBER() { return getToken(BashParser.CLOBBER, 0); }
		public RedirectionOpContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_redirectionOp; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterRedirectionOp(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitRedirectionOp(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitRedirectionOp(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RedirectionOpContext redirectionOp() throws RecognitionException {
		RedirectionOpContext _localctx = new RedirectionOpContext(_ctx, getState());
		enterRule(_localctx, 98, RULE_redirectionOp);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(534);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 1782411427840L) != 0)) ) {
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
	public static class HeredocContext extends ParserRuleContext {
		public HeredocDelimiterContext heredocDelimiter() {
			return getRuleContext(HeredocDelimiterContext.class,0);
		}
		public TerminalNode NEWLINE() { return getToken(BashParser.NEWLINE, 0); }
		public HeredocBodyContext heredocBody() {
			return getRuleContext(HeredocBodyContext.class,0);
		}
		public TerminalNode DLESS() { return getToken(BashParser.DLESS, 0); }
		public TerminalNode DLESSDASH() { return getToken(BashParser.DLESSDASH, 0); }
		public HeredocContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_heredoc; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterHeredoc(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitHeredoc(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitHeredoc(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HeredocContext heredoc() throws RecognitionException {
		HeredocContext _localctx = new HeredocContext(_ctx, getState());
		enterRule(_localctx, 100, RULE_heredoc);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(536);
			_la = _input.LA(1);
			if ( !(_la==DLESSDASH || _la==DLESS) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(537);
			heredocDelimiter();
			setState(538);
			match(NEWLINE);
			setState(539);
			heredocBody();
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
	public static class HeredocDelimiterContext extends ParserRuleContext {
		public TerminalNode WORD() { return getToken(BashParser.WORD, 0); }
		public TerminalNode SINGLE_QUOTED_STRING() { return getToken(BashParser.SINGLE_QUOTED_STRING, 0); }
		public List<TerminalNode> DOUBLE_QUOTE() { return getTokens(BashParser.DOUBLE_QUOTE); }
		public TerminalNode DOUBLE_QUOTE(int i) {
			return getToken(BashParser.DOUBLE_QUOTE, i);
		}
		public TerminalNode DQ_TEXT() { return getToken(BashParser.DQ_TEXT, 0); }
		public HeredocDelimiterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_heredocDelimiter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterHeredocDelimiter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitHeredocDelimiter(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitHeredocDelimiter(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HeredocDelimiterContext heredocDelimiter() throws RecognitionException {
		HeredocDelimiterContext _localctx = new HeredocDelimiterContext(_ctx, getState());
		enterRule(_localctx, 102, RULE_heredocDelimiter);
		int _la;
		try {
			setState(548);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case WORD:
				enterOuterAlt(_localctx, 1);
				{
				setState(541);
				match(WORD);
				}
				break;
			case SINGLE_QUOTED_STRING:
				enterOuterAlt(_localctx, 2);
				{
				setState(542);
				match(SINGLE_QUOTED_STRING);
				}
				break;
			case DOUBLE_QUOTE:
				enterOuterAlt(_localctx, 3);
				{
				setState(543);
				match(DOUBLE_QUOTE);
				setState(545);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==DQ_TEXT) {
					{
					setState(544);
					match(DQ_TEXT);
					}
				}

				setState(547);
				match(DOUBLE_QUOTE);
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
	public static class HeredocBodyContext extends ParserRuleContext {
		public List<HeredocLineContext> heredocLine() {
			return getRuleContexts(HeredocLineContext.class);
		}
		public HeredocLineContext heredocLine(int i) {
			return getRuleContext(HeredocLineContext.class,i);
		}
		public HeredocBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_heredocBody; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterHeredocBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitHeredocBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitHeredocBody(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HeredocBodyContext heredocBody() throws RecognitionException {
		HeredocBodyContext _localctx = new HeredocBodyContext(_ctx, getState());
		enterRule(_localctx, 104, RULE_heredocBody);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(551); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(550);
					heredocLine();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(553); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,52,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
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
	public static class HeredocLineContext extends ParserRuleContext {
		public List<TerminalNode> NEWLINE() { return getTokens(BashParser.NEWLINE); }
		public TerminalNode NEWLINE(int i) {
			return getToken(BashParser.NEWLINE, i);
		}
		public HeredocLineContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_heredocLine; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterHeredocLine(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitHeredocLine(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitHeredocLine(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HeredocLineContext heredocLine() throws RecognitionException {
		HeredocLineContext _localctx = new HeredocLineContext(_ctx, getState());
		enterRule(_localctx, 106, RULE_heredocLine);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(558);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & -10L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 34359738367L) != 0)) {
				{
				{
				setState(555);
				_la = _input.LA(1);
				if ( _la <= 0 || (_la==NEWLINE) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				}
				setState(560);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(561);
			match(NEWLINE);
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
	public static class WordContext extends ParserRuleContext {
		public List<WordPartContext> wordPart() {
			return getRuleContexts(WordPartContext.class);
		}
		public WordPartContext wordPart(int i) {
			return getRuleContext(WordPartContext.class,i);
		}
		public WordContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_word; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterWord(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitWord(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitWord(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WordContext word() throws RecognitionException {
		WordContext _localctx = new WordContext(_ctx, getState());
		enterRule(_localctx, 108, RULE_word);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(563);
			wordPart();
			setState(568);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,54,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(564);
					if (!(noWhitespaceBefore())) throw new FailedPredicateException(this, "noWhitespaceBefore()");
					setState(565);
					wordPart();
					}
					} 
				}
				setState(570);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,54,_ctx);
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
	public static class WordPartContext extends ParserRuleContext {
		public TerminalNode WORD() { return getToken(BashParser.WORD, 0); }
		public TerminalNode NUMBER() { return getToken(BashParser.NUMBER, 0); }
		public TerminalNode SINGLE_QUOTED_STRING() { return getToken(BashParser.SINGLE_QUOTED_STRING, 0); }
		public TerminalNode DOLLAR_SINGLE_QUOTED() { return getToken(BashParser.DOLLAR_SINGLE_QUOTED, 0); }
		public DoubleQuotedStringContext doubleQuotedString() {
			return getRuleContext(DoubleQuotedStringContext.class,0);
		}
		public TerminalNode DOLLAR_NAME() { return getToken(BashParser.DOLLAR_NAME, 0); }
		public TerminalNode SPECIAL_VAR() { return getToken(BashParser.SPECIAL_VAR, 0); }
		public TerminalNode DOLLAR_LBRACE() { return getToken(BashParser.DOLLAR_LBRACE, 0); }
		public BraceExpansionContentContext braceExpansionContent() {
			return getRuleContext(BraceExpansionContentContext.class,0);
		}
		public TerminalNode RBRACE() { return getToken(BashParser.RBRACE, 0); }
		public CommandSubstitutionContext commandSubstitution() {
			return getRuleContext(CommandSubstitutionContext.class,0);
		}
		public List<TerminalNode> BACKTICK() { return getTokens(BashParser.BACKTICK); }
		public TerminalNode BACKTICK(int i) {
			return getToken(BashParser.BACKTICK, i);
		}
		public BacktickContentContext backtickContent() {
			return getRuleContext(BacktickContentContext.class,0);
		}
		public ArithmeticSubstitutionContext arithmeticSubstitution() {
			return getRuleContext(ArithmeticSubstitutionContext.class,0);
		}
		public ProcessSubstitutionContext processSubstitution() {
			return getRuleContext(ProcessSubstitutionContext.class,0);
		}
		public TerminalNode STAR() { return getToken(BashParser.STAR, 0); }
		public TerminalNode QUESTION() { return getToken(BashParser.QUESTION, 0); }
		public TerminalNode TILDE() { return getToken(BashParser.TILDE, 0); }
		public TerminalNode COLON() { return getToken(BashParser.COLON, 0); }
		public TerminalNode DOLLAR() { return getToken(BashParser.DOLLAR, 0); }
		public TerminalNode LBRACKET() { return getToken(BashParser.LBRACKET, 0); }
		public TerminalNode RBRACKET() { return getToken(BashParser.RBRACKET, 0); }
		public TerminalNode BANG() { return getToken(BashParser.BANG, 0); }
		public TerminalNode EQUALS() { return getToken(BashParser.EQUALS, 0); }
		public TerminalNode PLUS_EQUALS() { return getToken(BashParser.PLUS_EQUALS, 0); }
		public TerminalNode LBRACE() { return getToken(BashParser.LBRACE, 0); }
		public List<WordPartContext> wordPart() {
			return getRuleContexts(WordPartContext.class);
		}
		public WordPartContext wordPart(int i) {
			return getRuleContext(WordPartContext.class,i);
		}
		public TerminalNode IF() { return getToken(BashParser.IF, 0); }
		public TerminalNode THEN() { return getToken(BashParser.THEN, 0); }
		public TerminalNode ELSE() { return getToken(BashParser.ELSE, 0); }
		public TerminalNode ELIF() { return getToken(BashParser.ELIF, 0); }
		public TerminalNode FI() { return getToken(BashParser.FI, 0); }
		public TerminalNode FOR() { return getToken(BashParser.FOR, 0); }
		public TerminalNode WHILE() { return getToken(BashParser.WHILE, 0); }
		public TerminalNode UNTIL() { return getToken(BashParser.UNTIL, 0); }
		public TerminalNode DO() { return getToken(BashParser.DO, 0); }
		public TerminalNode DONE() { return getToken(BashParser.DONE, 0); }
		public TerminalNode CASE() { return getToken(BashParser.CASE, 0); }
		public TerminalNode ESAC() { return getToken(BashParser.ESAC, 0); }
		public TerminalNode IN() { return getToken(BashParser.IN, 0); }
		public TerminalNode FUNCTION() { return getToken(BashParser.FUNCTION, 0); }
		public TerminalNode SELECT() { return getToken(BashParser.SELECT, 0); }
		public TerminalNode COPROC() { return getToken(BashParser.COPROC, 0); }
		public TerminalNode TIME() { return getToken(BashParser.TIME, 0); }
		public WordPartContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_wordPart; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterWordPart(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitWordPart(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitWordPart(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WordPartContext wordPart() throws RecognitionException {
		WordPartContext _localctx = new WordPartContext(_ctx, getState());
		enterRule(_localctx, 110, RULE_wordPart);
		int _la;
		try {
			setState(624);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case WORD:
				enterOuterAlt(_localctx, 1);
				{
				setState(571);
				match(WORD);
				}
				break;
			case NUMBER:
				enterOuterAlt(_localctx, 2);
				{
				setState(572);
				match(NUMBER);
				}
				break;
			case SINGLE_QUOTED_STRING:
				enterOuterAlt(_localctx, 3);
				{
				setState(573);
				match(SINGLE_QUOTED_STRING);
				}
				break;
			case DOLLAR_SINGLE_QUOTED:
				enterOuterAlt(_localctx, 4);
				{
				setState(574);
				match(DOLLAR_SINGLE_QUOTED);
				}
				break;
			case DOUBLE_QUOTE:
				enterOuterAlt(_localctx, 5);
				{
				setState(575);
				doubleQuotedString();
				}
				break;
			case DOLLAR_NAME:
				enterOuterAlt(_localctx, 6);
				{
				setState(576);
				match(DOLLAR_NAME);
				}
				break;
			case SPECIAL_VAR:
				enterOuterAlt(_localctx, 7);
				{
				setState(577);
				match(SPECIAL_VAR);
				}
				break;
			case DOLLAR_LBRACE:
				enterOuterAlt(_localctx, 8);
				{
				setState(578);
				match(DOLLAR_LBRACE);
				setState(579);
				braceExpansionContent();
				setState(580);
				match(RBRACE);
				}
				break;
			case DOLLAR_LPAREN:
				enterOuterAlt(_localctx, 9);
				{
				setState(582);
				commandSubstitution();
				}
				break;
			case BACKTICK:
				enterOuterAlt(_localctx, 10);
				{
				setState(583);
				match(BACKTICK);
				setState(584);
				backtickContent();
				setState(585);
				match(BACKTICK);
				}
				break;
			case DOLLAR_DPAREN:
				enterOuterAlt(_localctx, 11);
				{
				setState(587);
				arithmeticSubstitution();
				}
				break;
			case PROC_SUBST_IN:
			case PROC_SUBST_OUT:
				enterOuterAlt(_localctx, 12);
				{
				setState(588);
				processSubstitution();
				}
				break;
			case STAR:
				enterOuterAlt(_localctx, 13);
				{
				setState(589);
				match(STAR);
				}
				break;
			case QUESTION:
				enterOuterAlt(_localctx, 14);
				{
				setState(590);
				match(QUESTION);
				}
				break;
			case TILDE:
				enterOuterAlt(_localctx, 15);
				{
				setState(591);
				match(TILDE);
				}
				break;
			case COLON:
				enterOuterAlt(_localctx, 16);
				{
				setState(592);
				match(COLON);
				}
				break;
			case DOLLAR:
				enterOuterAlt(_localctx, 17);
				{
				setState(593);
				match(DOLLAR);
				}
				break;
			case LBRACKET:
				enterOuterAlt(_localctx, 18);
				{
				setState(594);
				match(LBRACKET);
				}
				break;
			case RBRACKET:
				enterOuterAlt(_localctx, 19);
				{
				setState(595);
				match(RBRACKET);
				}
				break;
			case BANG:
				enterOuterAlt(_localctx, 20);
				{
				setState(596);
				match(BANG);
				}
				break;
			case EQUALS:
				enterOuterAlt(_localctx, 21);
				{
				setState(597);
				match(EQUALS);
				}
				break;
			case PLUS_EQUALS:
				enterOuterAlt(_localctx, 22);
				{
				setState(598);
				match(PLUS_EQUALS);
				}
				break;
			case LBRACE:
				enterOuterAlt(_localctx, 23);
				{
				setState(599);
				match(LBRACE);
				setState(603);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & -50665083017101344L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 255L) != 0)) {
					{
					{
					setState(600);
					wordPart();
					}
					}
					setState(605);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(606);
				match(RBRACE);
				}
				break;
			case IF:
				enterOuterAlt(_localctx, 24);
				{
				setState(607);
				match(IF);
				}
				break;
			case THEN:
				enterOuterAlt(_localctx, 25);
				{
				setState(608);
				match(THEN);
				}
				break;
			case ELSE:
				enterOuterAlt(_localctx, 26);
				{
				setState(609);
				match(ELSE);
				}
				break;
			case ELIF:
				enterOuterAlt(_localctx, 27);
				{
				setState(610);
				match(ELIF);
				}
				break;
			case FI:
				enterOuterAlt(_localctx, 28);
				{
				setState(611);
				match(FI);
				}
				break;
			case FOR:
				enterOuterAlt(_localctx, 29);
				{
				setState(612);
				match(FOR);
				}
				break;
			case WHILE:
				enterOuterAlt(_localctx, 30);
				{
				setState(613);
				match(WHILE);
				}
				break;
			case UNTIL:
				enterOuterAlt(_localctx, 31);
				{
				setState(614);
				match(UNTIL);
				}
				break;
			case DO:
				enterOuterAlt(_localctx, 32);
				{
				setState(615);
				match(DO);
				}
				break;
			case DONE:
				enterOuterAlt(_localctx, 33);
				{
				setState(616);
				match(DONE);
				}
				break;
			case CASE:
				enterOuterAlt(_localctx, 34);
				{
				setState(617);
				match(CASE);
				}
				break;
			case ESAC:
				enterOuterAlt(_localctx, 35);
				{
				setState(618);
				match(ESAC);
				}
				break;
			case IN:
				enterOuterAlt(_localctx, 36);
				{
				setState(619);
				match(IN);
				}
				break;
			case FUNCTION:
				enterOuterAlt(_localctx, 37);
				{
				setState(620);
				match(FUNCTION);
				}
				break;
			case SELECT:
				enterOuterAlt(_localctx, 38);
				{
				setState(621);
				match(SELECT);
				}
				break;
			case COPROC:
				enterOuterAlt(_localctx, 39);
				{
				setState(622);
				match(COPROC);
				}
				break;
			case TIME:
				enterOuterAlt(_localctx, 40);
				{
				setState(623);
				match(TIME);
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
	public static class DoubleQuotedStringContext extends ParserRuleContext {
		public List<TerminalNode> DOUBLE_QUOTE() { return getTokens(BashParser.DOUBLE_QUOTE); }
		public TerminalNode DOUBLE_QUOTE(int i) {
			return getToken(BashParser.DOUBLE_QUOTE, i);
		}
		public List<DoubleQuotedPartContext> doubleQuotedPart() {
			return getRuleContexts(DoubleQuotedPartContext.class);
		}
		public DoubleQuotedPartContext doubleQuotedPart(int i) {
			return getRuleContext(DoubleQuotedPartContext.class,i);
		}
		public DoubleQuotedStringContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_doubleQuotedString; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterDoubleQuotedString(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitDoubleQuotedString(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitDoubleQuotedString(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DoubleQuotedStringContext doubleQuotedString() throws RecognitionException {
		DoubleQuotedStringContext _localctx = new DoubleQuotedStringContext(_ctx, getState());
		enterRule(_localctx, 112, RULE_doubleQuotedString);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(626);
			match(DOUBLE_QUOTE);
			setState(630);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 26)) & ~0x3f) == 0 && ((1L << (_la - 26)) & 844545189216263L) != 0)) {
				{
				{
				setState(627);
				doubleQuotedPart();
				}
				}
				setState(632);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(633);
			match(DOUBLE_QUOTE);
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
	public static class DoubleQuotedPartContext extends ParserRuleContext {
		public TerminalNode DQ_TEXT() { return getToken(BashParser.DQ_TEXT, 0); }
		public TerminalNode DQ_ESCAPE() { return getToken(BashParser.DQ_ESCAPE, 0); }
		public TerminalNode DOLLAR_NAME() { return getToken(BashParser.DOLLAR_NAME, 0); }
		public TerminalNode SPECIAL_VAR() { return getToken(BashParser.SPECIAL_VAR, 0); }
		public TerminalNode DOLLAR_LBRACE() { return getToken(BashParser.DOLLAR_LBRACE, 0); }
		public BraceExpansionContentContext braceExpansionContent() {
			return getRuleContext(BraceExpansionContentContext.class,0);
		}
		public TerminalNode RBRACE() { return getToken(BashParser.RBRACE, 0); }
		public CommandSubstitutionContext commandSubstitution() {
			return getRuleContext(CommandSubstitutionContext.class,0);
		}
		public ArithmeticSubstitutionContext arithmeticSubstitution() {
			return getRuleContext(ArithmeticSubstitutionContext.class,0);
		}
		public List<TerminalNode> BACKTICK() { return getTokens(BashParser.BACKTICK); }
		public TerminalNode BACKTICK(int i) {
			return getToken(BashParser.BACKTICK, i);
		}
		public BacktickContentContext backtickContent() {
			return getRuleContext(BacktickContentContext.class,0);
		}
		public DoubleQuotedPartContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_doubleQuotedPart; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterDoubleQuotedPart(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitDoubleQuotedPart(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitDoubleQuotedPart(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DoubleQuotedPartContext doubleQuotedPart() throws RecognitionException {
		DoubleQuotedPartContext _localctx = new DoubleQuotedPartContext(_ctx, getState());
		enterRule(_localctx, 114, RULE_doubleQuotedPart);
		try {
			setState(649);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DQ_TEXT:
				enterOuterAlt(_localctx, 1);
				{
				setState(635);
				match(DQ_TEXT);
				}
				break;
			case DQ_ESCAPE:
				enterOuterAlt(_localctx, 2);
				{
				setState(636);
				match(DQ_ESCAPE);
				}
				break;
			case DOLLAR_NAME:
				enterOuterAlt(_localctx, 3);
				{
				setState(637);
				match(DOLLAR_NAME);
				}
				break;
			case SPECIAL_VAR:
				enterOuterAlt(_localctx, 4);
				{
				setState(638);
				match(SPECIAL_VAR);
				}
				break;
			case DOLLAR_LBRACE:
				enterOuterAlt(_localctx, 5);
				{
				setState(639);
				match(DOLLAR_LBRACE);
				setState(640);
				braceExpansionContent();
				setState(641);
				match(RBRACE);
				}
				break;
			case DOLLAR_LPAREN:
				enterOuterAlt(_localctx, 6);
				{
				setState(643);
				commandSubstitution();
				}
				break;
			case DOLLAR_DPAREN:
				enterOuterAlt(_localctx, 7);
				{
				setState(644);
				arithmeticSubstitution();
				}
				break;
			case BACKTICK:
				enterOuterAlt(_localctx, 8);
				{
				setState(645);
				match(BACKTICK);
				setState(646);
				backtickContent();
				setState(647);
				match(BACKTICK);
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
	public static class CommandSubstitutionContext extends ParserRuleContext {
		public TerminalNode DOLLAR_LPAREN() { return getToken(BashParser.DOLLAR_LPAREN, 0); }
		public CommandSubstitutionContentContext commandSubstitutionContent() {
			return getRuleContext(CommandSubstitutionContentContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(BashParser.RPAREN, 0); }
		public CommandSubstitutionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_commandSubstitution; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterCommandSubstitution(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitCommandSubstitution(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitCommandSubstitution(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CommandSubstitutionContext commandSubstitution() throws RecognitionException {
		CommandSubstitutionContext _localctx = new CommandSubstitutionContext(_ctx, getState());
		enterRule(_localctx, 116, RULE_commandSubstitution);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(651);
			match(DOLLAR_LPAREN);
			setState(652);
			commandSubstitutionContent();
			setState(653);
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
	public static class CommandSubstitutionContentContext extends ParserRuleContext {
		public LinebreakContext linebreak() {
			return getRuleContext(LinebreakContext.class,0);
		}
		public CompleteCommandsContext completeCommands() {
			return getRuleContext(CompleteCommandsContext.class,0);
		}
		public CommandSubstitutionContentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_commandSubstitutionContent; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterCommandSubstitutionContent(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitCommandSubstitutionContent(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitCommandSubstitutionContent(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CommandSubstitutionContentContext commandSubstitutionContent() throws RecognitionException {
		CommandSubstitutionContentContext _localctx = new CommandSubstitutionContentContext(_ctx, getState());
		enterRule(_localctx, 118, RULE_commandSubstitutionContent);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(656);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -46159697199235104L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 255L) != 0)) {
				{
				setState(655);
				completeCommands();
				}
			}

			setState(658);
			linebreak();
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
	public static class BacktickContentContext extends ParserRuleContext {
		public List<TerminalNode> BACKTICK() { return getTokens(BashParser.BACKTICK); }
		public TerminalNode BACKTICK(int i) {
			return getToken(BashParser.BACKTICK, i);
		}
		public BacktickContentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_backtickContent; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterBacktickContent(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitBacktickContent(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitBacktickContent(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BacktickContentContext backtickContent() throws RecognitionException {
		BacktickContentContext _localctx = new BacktickContentContext(_ctx, getState());
		enterRule(_localctx, 120, RULE_backtickContent);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(663);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & -1152921504606846978L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 34359738367L) != 0)) {
				{
				{
				setState(660);
				_la = _input.LA(1);
				if ( _la <= 0 || (_la==BACKTICK) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				}
				setState(665);
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
	public static class ArithmeticSubstitutionContext extends ParserRuleContext {
		public TerminalNode DOLLAR_DPAREN() { return getToken(BashParser.DOLLAR_DPAREN, 0); }
		public ArithmeticExprContext arithmeticExpr() {
			return getRuleContext(ArithmeticExprContext.class,0);
		}
		public TerminalNode DOUBLE_RPAREN() { return getToken(BashParser.DOUBLE_RPAREN, 0); }
		public ArithmeticSubstitutionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arithmeticSubstitution; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterArithmeticSubstitution(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitArithmeticSubstitution(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitArithmeticSubstitution(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArithmeticSubstitutionContext arithmeticSubstitution() throws RecognitionException {
		ArithmeticSubstitutionContext _localctx = new ArithmeticSubstitutionContext(_ctx, getState());
		enterRule(_localctx, 122, RULE_arithmeticSubstitution);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(666);
			match(DOLLAR_DPAREN);
			setState(667);
			arithmeticExpr();
			setState(668);
			match(DOUBLE_RPAREN);
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
	public static class ProcessSubstitutionContext extends ParserRuleContext {
		public TerminalNode PROC_SUBST_IN() { return getToken(BashParser.PROC_SUBST_IN, 0); }
		public CommandSubstitutionContentContext commandSubstitutionContent() {
			return getRuleContext(CommandSubstitutionContentContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(BashParser.RPAREN, 0); }
		public TerminalNode PROC_SUBST_OUT() { return getToken(BashParser.PROC_SUBST_OUT, 0); }
		public ProcessSubstitutionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_processSubstitution; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterProcessSubstitution(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitProcessSubstitution(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitProcessSubstitution(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ProcessSubstitutionContext processSubstitution() throws RecognitionException {
		ProcessSubstitutionContext _localctx = new ProcessSubstitutionContext(_ctx, getState());
		enterRule(_localctx, 124, RULE_processSubstitution);
		try {
			setState(678);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case PROC_SUBST_IN:
				enterOuterAlt(_localctx, 1);
				{
				setState(670);
				match(PROC_SUBST_IN);
				setState(671);
				commandSubstitutionContent();
				setState(672);
				match(RPAREN);
				}
				break;
			case PROC_SUBST_OUT:
				enterOuterAlt(_localctx, 2);
				{
				setState(674);
				match(PROC_SUBST_OUT);
				setState(675);
				commandSubstitutionContent();
				setState(676);
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
	public static class BraceExpansionContentContext extends ParserRuleContext {
		public List<BraceExpansionPartContext> braceExpansionPart() {
			return getRuleContexts(BraceExpansionPartContext.class);
		}
		public BraceExpansionPartContext braceExpansionPart(int i) {
			return getRuleContext(BraceExpansionPartContext.class,i);
		}
		public BraceExpansionContentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_braceExpansionContent; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterBraceExpansionContent(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitBraceExpansionContent(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitBraceExpansionContent(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BraceExpansionContentContext braceExpansionContent() throws RecognitionException {
		BraceExpansionContentContext _localctx = new BraceExpansionContentContext(_ctx, getState());
		enterRule(_localctx, 126, RULE_braceExpansionContent);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(683);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 6917529028110843904L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 34359734531L) != 0)) {
				{
				{
				setState(680);
				braceExpansionPart();
				}
				}
				setState(685);
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
	public static class BraceExpansionPartContext extends ParserRuleContext {
		public TerminalNode BE_NAME() { return getToken(BashParser.BE_NAME, 0); }
		public TerminalNode BE_NUMBER() { return getToken(BashParser.BE_NUMBER, 0); }
		public TerminalNode BE_TEXT() { return getToken(BashParser.BE_TEXT, 0); }
		public TerminalNode BE_COLON_DASH() { return getToken(BashParser.BE_COLON_DASH, 0); }
		public TerminalNode BE_COLON_EQUALS() { return getToken(BashParser.BE_COLON_EQUALS, 0); }
		public TerminalNode BE_COLON_PLUS() { return getToken(BashParser.BE_COLON_PLUS, 0); }
		public TerminalNode BE_COLON_QUEST() { return getToken(BashParser.BE_COLON_QUEST, 0); }
		public TerminalNode BE_HASH() { return getToken(BashParser.BE_HASH, 0); }
		public TerminalNode BE_DHASH() { return getToken(BashParser.BE_DHASH, 0); }
		public TerminalNode BE_PERCENT() { return getToken(BashParser.BE_PERCENT, 0); }
		public TerminalNode BE_DPERCENT() { return getToken(BashParser.BE_DPERCENT, 0); }
		public TerminalNode BE_SLASH() { return getToken(BashParser.BE_SLASH, 0); }
		public TerminalNode BE_DSLASH() { return getToken(BashParser.BE_DSLASH, 0); }
		public TerminalNode BE_CARET() { return getToken(BashParser.BE_CARET, 0); }
		public TerminalNode BE_DCARET() { return getToken(BashParser.BE_DCARET, 0); }
		public TerminalNode BE_COMMA() { return getToken(BashParser.BE_COMMA, 0); }
		public TerminalNode BE_DCOMMA() { return getToken(BashParser.BE_DCOMMA, 0); }
		public TerminalNode BE_AT() { return getToken(BashParser.BE_AT, 0); }
		public TerminalNode BE_STAR() { return getToken(BashParser.BE_STAR, 0); }
		public TerminalNode BE_BANG() { return getToken(BashParser.BE_BANG, 0); }
		public TerminalNode BE_COLON() { return getToken(BashParser.BE_COLON, 0); }
		public TerminalNode BE_LBRACKET() { return getToken(BashParser.BE_LBRACKET, 0); }
		public TerminalNode BE_RBRACKET() { return getToken(BashParser.BE_RBRACKET, 0); }
		public TerminalNode DOLLAR_LBRACE() { return getToken(BashParser.DOLLAR_LBRACE, 0); }
		public BraceExpansionContentContext braceExpansionContent() {
			return getRuleContext(BraceExpansionContentContext.class,0);
		}
		public TerminalNode RBRACE() { return getToken(BashParser.RBRACE, 0); }
		public TerminalNode DOLLAR_LPAREN() { return getToken(BashParser.DOLLAR_LPAREN, 0); }
		public CommandSubstitutionContentContext commandSubstitutionContent() {
			return getRuleContext(CommandSubstitutionContentContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(BashParser.RPAREN, 0); }
		public TerminalNode DOLLAR_DPAREN() { return getToken(BashParser.DOLLAR_DPAREN, 0); }
		public ArithmeticExprContext arithmeticExpr() {
			return getRuleContext(ArithmeticExprContext.class,0);
		}
		public TerminalNode DOUBLE_RPAREN() { return getToken(BashParser.DOUBLE_RPAREN, 0); }
		public TerminalNode DOLLAR_NAME() { return getToken(BashParser.DOLLAR_NAME, 0); }
		public TerminalNode SPECIAL_VAR() { return getToken(BashParser.SPECIAL_VAR, 0); }
		public List<TerminalNode> DOUBLE_QUOTE() { return getTokens(BashParser.DOUBLE_QUOTE); }
		public TerminalNode DOUBLE_QUOTE(int i) {
			return getToken(BashParser.DOUBLE_QUOTE, i);
		}
		public List<DoubleQuotedPartContext> doubleQuotedPart() {
			return getRuleContexts(DoubleQuotedPartContext.class);
		}
		public DoubleQuotedPartContext doubleQuotedPart(int i) {
			return getRuleContext(DoubleQuotedPartContext.class,i);
		}
		public TerminalNode SINGLE_QUOTED_STRING() { return getToken(BashParser.SINGLE_QUOTED_STRING, 0); }
		public TerminalNode WS() { return getToken(BashParser.WS, 0); }
		public BraceExpansionPartContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_braceExpansionPart; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterBraceExpansionPart(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitBraceExpansionPart(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitBraceExpansionPart(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BraceExpansionPartContext braceExpansionPart() throws RecognitionException {
		BraceExpansionPartContext _localctx = new BraceExpansionPartContext(_ctx, getState());
		enterRule(_localctx, 128, RULE_braceExpansionPart);
		int _la;
		try {
			setState(733);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case BE_NAME:
				enterOuterAlt(_localctx, 1);
				{
				setState(686);
				match(BE_NAME);
				}
				break;
			case BE_NUMBER:
				enterOuterAlt(_localctx, 2);
				{
				setState(687);
				match(BE_NUMBER);
				}
				break;
			case BE_TEXT:
				enterOuterAlt(_localctx, 3);
				{
				setState(688);
				match(BE_TEXT);
				}
				break;
			case BE_COLON_DASH:
				enterOuterAlt(_localctx, 4);
				{
				setState(689);
				match(BE_COLON_DASH);
				}
				break;
			case BE_COLON_EQUALS:
				enterOuterAlt(_localctx, 5);
				{
				setState(690);
				match(BE_COLON_EQUALS);
				}
				break;
			case BE_COLON_PLUS:
				enterOuterAlt(_localctx, 6);
				{
				setState(691);
				match(BE_COLON_PLUS);
				}
				break;
			case BE_COLON_QUEST:
				enterOuterAlt(_localctx, 7);
				{
				setState(692);
				match(BE_COLON_QUEST);
				}
				break;
			case BE_HASH:
				enterOuterAlt(_localctx, 8);
				{
				setState(693);
				match(BE_HASH);
				}
				break;
			case BE_DHASH:
				enterOuterAlt(_localctx, 9);
				{
				setState(694);
				match(BE_DHASH);
				}
				break;
			case BE_PERCENT:
				enterOuterAlt(_localctx, 10);
				{
				setState(695);
				match(BE_PERCENT);
				}
				break;
			case BE_DPERCENT:
				enterOuterAlt(_localctx, 11);
				{
				setState(696);
				match(BE_DPERCENT);
				}
				break;
			case BE_SLASH:
				enterOuterAlt(_localctx, 12);
				{
				setState(697);
				match(BE_SLASH);
				}
				break;
			case BE_DSLASH:
				enterOuterAlt(_localctx, 13);
				{
				setState(698);
				match(BE_DSLASH);
				}
				break;
			case BE_CARET:
				enterOuterAlt(_localctx, 14);
				{
				setState(699);
				match(BE_CARET);
				}
				break;
			case BE_DCARET:
				enterOuterAlt(_localctx, 15);
				{
				setState(700);
				match(BE_DCARET);
				}
				break;
			case BE_COMMA:
				enterOuterAlt(_localctx, 16);
				{
				setState(701);
				match(BE_COMMA);
				}
				break;
			case BE_DCOMMA:
				enterOuterAlt(_localctx, 17);
				{
				setState(702);
				match(BE_DCOMMA);
				}
				break;
			case BE_AT:
				enterOuterAlt(_localctx, 18);
				{
				setState(703);
				match(BE_AT);
				}
				break;
			case BE_STAR:
				enterOuterAlt(_localctx, 19);
				{
				setState(704);
				match(BE_STAR);
				}
				break;
			case BE_BANG:
				enterOuterAlt(_localctx, 20);
				{
				setState(705);
				match(BE_BANG);
				}
				break;
			case BE_COLON:
				enterOuterAlt(_localctx, 21);
				{
				setState(706);
				match(BE_COLON);
				}
				break;
			case BE_LBRACKET:
				enterOuterAlt(_localctx, 22);
				{
				setState(707);
				match(BE_LBRACKET);
				}
				break;
			case BE_RBRACKET:
				enterOuterAlt(_localctx, 23);
				{
				setState(708);
				match(BE_RBRACKET);
				}
				break;
			case DOLLAR_LBRACE:
				enterOuterAlt(_localctx, 24);
				{
				setState(709);
				match(DOLLAR_LBRACE);
				setState(710);
				braceExpansionContent();
				setState(711);
				match(RBRACE);
				}
				break;
			case DOLLAR_LPAREN:
				enterOuterAlt(_localctx, 25);
				{
				setState(713);
				match(DOLLAR_LPAREN);
				setState(714);
				commandSubstitutionContent();
				setState(715);
				match(RPAREN);
				}
				break;
			case DOLLAR_DPAREN:
				enterOuterAlt(_localctx, 26);
				{
				setState(717);
				match(DOLLAR_DPAREN);
				setState(718);
				arithmeticExpr();
				setState(719);
				match(DOUBLE_RPAREN);
				}
				break;
			case DOLLAR_NAME:
				enterOuterAlt(_localctx, 27);
				{
				setState(721);
				match(DOLLAR_NAME);
				}
				break;
			case SPECIAL_VAR:
				enterOuterAlt(_localctx, 28);
				{
				setState(722);
				match(SPECIAL_VAR);
				}
				break;
			case DOUBLE_QUOTE:
				enterOuterAlt(_localctx, 29);
				{
				setState(723);
				match(DOUBLE_QUOTE);
				setState(727);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (((((_la - 26)) & ~0x3f) == 0 && ((1L << (_la - 26)) & 844545189216263L) != 0)) {
					{
					{
					setState(724);
					doubleQuotedPart();
					}
					}
					setState(729);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(730);
				match(DOUBLE_QUOTE);
				}
				break;
			case SINGLE_QUOTED_STRING:
				enterOuterAlt(_localctx, 30);
				{
				setState(731);
				match(SINGLE_QUOTED_STRING);
				}
				break;
			case WS:
				enterOuterAlt(_localctx, 31);
				{
				setState(732);
				match(WS);
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
	public static class SeparatorContext extends ParserRuleContext {
		public TerminalNode SEMI() { return getToken(BashParser.SEMI, 0); }
		public LinebreakContext linebreak() {
			return getRuleContext(LinebreakContext.class,0);
		}
		public TerminalNode AMP() { return getToken(BashParser.AMP, 0); }
		public SeparatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_separator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterSeparator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitSeparator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitSeparator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SeparatorContext separator() throws RecognitionException {
		SeparatorContext _localctx = new SeparatorContext(_ctx, getState());
		enterRule(_localctx, 130, RULE_separator);
		try {
			setState(739);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SEMI:
				enterOuterAlt(_localctx, 1);
				{
				setState(735);
				match(SEMI);
				setState(736);
				linebreak();
				}
				break;
			case AMP:
				enterOuterAlt(_localctx, 2);
				{
				setState(737);
				match(AMP);
				setState(738);
				linebreak();
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
	public static class SequentialSepContext extends ParserRuleContext {
		public TerminalNode SEMI() { return getToken(BashParser.SEMI, 0); }
		public LinebreakContext linebreak() {
			return getRuleContext(LinebreakContext.class,0);
		}
		public NewlineListContext newlineList() {
			return getRuleContext(NewlineListContext.class,0);
		}
		public SequentialSepContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_sequentialSep; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterSequentialSep(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitSequentialSep(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitSequentialSep(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SequentialSepContext sequentialSep() throws RecognitionException {
		SequentialSepContext _localctx = new SequentialSepContext(_ctx, getState());
		enterRule(_localctx, 132, RULE_sequentialSep);
		try {
			setState(744);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SEMI:
				enterOuterAlt(_localctx, 1);
				{
				setState(741);
				match(SEMI);
				setState(742);
				linebreak();
				}
				break;
			case NEWLINE:
			case COMMENT:
				enterOuterAlt(_localctx, 2);
				{
				setState(743);
				newlineList();
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
	public static class NewlineListContext extends ParserRuleContext {
		public List<TerminalNode> NEWLINE() { return getTokens(BashParser.NEWLINE); }
		public TerminalNode NEWLINE(int i) {
			return getToken(BashParser.NEWLINE, i);
		}
		public List<TerminalNode> COMMENT() { return getTokens(BashParser.COMMENT); }
		public TerminalNode COMMENT(int i) {
			return getToken(BashParser.COMMENT, i);
		}
		public NewlineListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_newlineList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterNewlineList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitNewlineList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitNewlineList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NewlineListContext newlineList() throws RecognitionException {
		NewlineListContext _localctx = new NewlineListContext(_ctx, getState());
		enterRule(_localctx, 134, RULE_newlineList);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(751); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					setState(751);
					_errHandler.sync(this);
					switch (_input.LA(1)) {
					case NEWLINE:
						{
						setState(746);
						match(NEWLINE);
						}
						break;
					case COMMENT:
						{
						setState(747);
						match(COMMENT);
						setState(749);
						_errHandler.sync(this);
						switch ( getInterpreter().adaptivePredict(_input,67,_ctx) ) {
						case 1:
							{
							setState(748);
							match(NEWLINE);
							}
							break;
						}
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(753); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,69,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
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
	public static class LinebreakContext extends ParserRuleContext {
		public NewlineListContext newlineList() {
			return getRuleContext(NewlineListContext.class,0);
		}
		public LinebreakContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_linebreak; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).enterLinebreak(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof BashParserListener ) ((BashParserListener)listener).exitLinebreak(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof BashParserVisitor ) return ((BashParserVisitor<? extends T>)visitor).visitLinebreak(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LinebreakContext linebreak() throws RecognitionException {
		LinebreakContext _localctx = new LinebreakContext(_ctx, getState());
		enterRule(_localctx, 136, RULE_linebreak);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(756);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,70,_ctx) ) {
			case 1:
				{
				setState(755);
				newlineList();
				}
				break;
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

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 54:
			return word_sempred((WordContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean word_sempred(WordContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return noWhitespaceBefore();
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u0001b\u02f7\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
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
		"#\u0007#\u0002$\u0007$\u0002%\u0007%\u0002&\u0007&\u0002\'\u0007\'\u0002"+
		"(\u0007(\u0002)\u0007)\u0002*\u0007*\u0002+\u0007+\u0002,\u0007,\u0002"+
		"-\u0007-\u0002.\u0007.\u0002/\u0007/\u00020\u00070\u00021\u00071\u0002"+
		"2\u00072\u00023\u00073\u00024\u00074\u00025\u00075\u00026\u00076\u0002"+
		"7\u00077\u00028\u00078\u00029\u00079\u0002:\u0007:\u0002;\u0007;\u0002"+
		"<\u0007<\u0002=\u0007=\u0002>\u0007>\u0002?\u0007?\u0002@\u0007@\u0002"+
		"A\u0007A\u0002B\u0007B\u0002C\u0007C\u0002D\u0007D\u0001\u0000\u0003\u0000"+
		"\u008c\b\u0000\u0001\u0000\u0001\u0000\u0003\u0000\u0090\b\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0005\u0002\u009c\b\u0002\n"+
		"\u0002\f\u0002\u009f\t\u0002\u0001\u0003\u0001\u0003\u0003\u0003\u00a3"+
		"\b\u0003\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0005\u0004\u00a9"+
		"\b\u0004\n\u0004\f\u0004\u00ac\t\u0004\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0003\u0005"+
		"\u00b6\b\u0005\u0001\u0006\u0001\u0006\u0001\u0007\u0003\u0007\u00bb\b"+
		"\u0007\u0001\u0007\u0003\u0007\u00be\b\u0007\u0001\u0007\u0001\u0007\u0001"+
		"\b\u0001\b\u0001\t\u0001\t\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0005"+
		"\n\u00cb\b\n\n\n\f\n\u00ce\t\n\u0001\u000b\u0001\u000b\u0001\f\u0001\f"+
		"\u0003\f\u00d4\b\f\u0001\f\u0001\f\u0003\f\u00d8\b\f\u0001\r\u0001\r\u0003"+
		"\r\u00dc\b\r\u0001\r\u0003\r\u00df\b\r\u0001\r\u0001\r\u0003\r\u00e3\b"+
		"\r\u0003\r\u00e5\b\r\u0001\u000e\u0001\u000e\u0004\u000e\u00e9\b\u000e"+
		"\u000b\u000e\f\u000e\u00ea\u0001\u000f\u0001\u000f\u0001\u0010\u0001\u0010"+
		"\u0001\u0010\u0004\u0010\u00f2\b\u0010\u000b\u0010\f\u0010\u00f3\u0001"+
		"\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001"+
		"\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0003\u0011\u0100\b\u0011\u0001"+
		"\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0013\u0001\u0013\u0001"+
		"\u0013\u0001\u0013\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001"+
		"\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0005\u0015\u0113"+
		"\b\u0015\n\u0015\f\u0015\u0116\t\u0015\u0001\u0015\u0003\u0015\u0119\b"+
		"\u0015\u0001\u0015\u0001\u0015\u0001\u0016\u0001\u0016\u0001\u0016\u0001"+
		"\u0016\u0001\u0016\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0018\u0001"+
		"\u0018\u0001\u0018\u0001\u0018\u0001\u0019\u0001\u0019\u0001\u0019\u0001"+
		"\u0019\u0003\u0019\u012d\b\u0019\u0001\u001a\u0001\u001a\u0001\u001a\u0005"+
		"\u001a\u0132\b\u001a\n\u001a\f\u001a\u0135\t\u001a\u0001\u001a\u0001\u001a"+
		"\u0001\u001b\u0001\u001b\u0001\u001b\u0003\u001b\u013c\b\u001b\u0001\u001b"+
		"\u0001\u001b\u0003\u001b\u0140\b\u001b\u0001\u001b\u0001\u001b\u0003\u001b"+
		"\u0144\b\u001b\u0001\u001b\u0001\u001b\u0003\u001b\u0148\b\u001b\u0001"+
		"\u001b\u0001\u001b\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001"+
		"\u001d\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001e\u0001\u001e\u0001"+
		"\u001e\u0001\u001e\u0001\u001f\u0001\u001f\u0001\u001f\u0001\u001f\u0001"+
		"\u001f\u0001\u001f\u0005\u001f\u015e\b\u001f\n\u001f\f\u001f\u0161\t\u001f"+
		"\u0001\u001f\u0001\u001f\u0001 \u0003 \u0166\b \u0001 \u0001 \u0001 \u0001"+
		" \u0003 \u016c\b \u0001 \u0001 \u0001 \u0001 \u0003 \u0172\b \u0001 \u0001"+
		" \u0001 \u0001 \u0003 \u0178\b \u0001!\u0001!\u0001\"\u0001\"\u0001\""+
		"\u0005\"\u017f\b\"\n\"\f\"\u0182\t\"\u0001#\u0001#\u0001#\u0001#\u0001"+
		"$\u0004$\u0189\b$\u000b$\f$\u018a\u0001%\u0001%\u0001%\u0001%\u0001%\u0001"+
		"%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001"+
		"%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001"+
		"%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001"+
		"%\u0001%\u0003%\u01b2\b%\u0001&\u0001&\u0001&\u0001&\u0001\'\u0004\'\u01b9"+
		"\b\'\u000b\'\f\'\u01ba\u0001(\u0001(\u0001(\u0001(\u0001(\u0001(\u0001"+
		"(\u0001(\u0001(\u0001(\u0001(\u0001(\u0001(\u0001(\u0001(\u0001(\u0001"+
		"(\u0001(\u0001(\u0001(\u0001(\u0001(\u0001(\u0003(\u01d4\b(\u0001)\u0001"+
		")\u0001)\u0003)\u01d9\b)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001)\u0001"+
		")\u0001)\u0003)\u01e3\b)\u0001*\u0001*\u0001*\u0001+\u0001+\u0003+\u01ea"+
		"\b+\u0001,\u0001,\u0001,\u0003,\u01ef\b,\u0001-\u0001-\u0001-\u0003-\u01f4"+
		"\b-\u0001-\u0003-\u01f7\b-\u0001.\u0001.\u0001.\u0001.\u0004.\u01fd\b"+
		".\u000b.\f.\u01fe\u0001/\u0004/\u0202\b/\u000b/\f/\u0203\u00010\u0003"+
		"0\u0207\b0\u00010\u00010\u00010\u00010\u00030\u020d\b0\u00010\u00010\u0001"+
		"0\u00030\u0212\b0\u00010\u00030\u0215\b0\u00011\u00011\u00012\u00012\u0001"+
		"2\u00012\u00012\u00013\u00013\u00013\u00013\u00033\u0222\b3\u00013\u0003"+
		"3\u0225\b3\u00014\u00044\u0228\b4\u000b4\f4\u0229\u00015\u00055\u022d"+
		"\b5\n5\f5\u0230\t5\u00015\u00015\u00016\u00016\u00016\u00056\u0237\b6"+
		"\n6\f6\u023a\t6\u00017\u00017\u00017\u00017\u00017\u00017\u00017\u0001"+
		"7\u00017\u00017\u00017\u00017\u00017\u00017\u00017\u00017\u00017\u0001"+
		"7\u00017\u00017\u00017\u00017\u00017\u00017\u00017\u00017\u00017\u0001"+
		"7\u00017\u00017\u00057\u025a\b7\n7\f7\u025d\t7\u00017\u00017\u00017\u0001"+
		"7\u00017\u00017\u00017\u00017\u00017\u00017\u00017\u00017\u00017\u0001"+
		"7\u00017\u00017\u00017\u00017\u00037\u0271\b7\u00018\u00018\u00058\u0275"+
		"\b8\n8\f8\u0278\t8\u00018\u00018\u00019\u00019\u00019\u00019\u00019\u0001"+
		"9\u00019\u00019\u00019\u00019\u00019\u00019\u00019\u00019\u00039\u028a"+
		"\b9\u0001:\u0001:\u0001:\u0001:\u0001;\u0003;\u0291\b;\u0001;\u0001;\u0001"+
		"<\u0005<\u0296\b<\n<\f<\u0299\t<\u0001=\u0001=\u0001=\u0001=\u0001>\u0001"+
		">\u0001>\u0001>\u0001>\u0001>\u0001>\u0001>\u0003>\u02a7\b>\u0001?\u0005"+
		"?\u02aa\b?\n?\f?\u02ad\t?\u0001@\u0001@\u0001@\u0001@\u0001@\u0001@\u0001"+
		"@\u0001@\u0001@\u0001@\u0001@\u0001@\u0001@\u0001@\u0001@\u0001@\u0001"+
		"@\u0001@\u0001@\u0001@\u0001@\u0001@\u0001@\u0001@\u0001@\u0001@\u0001"+
		"@\u0001@\u0001@\u0001@\u0001@\u0001@\u0001@\u0001@\u0001@\u0001@\u0001"+
		"@\u0001@\u0001@\u0005@\u02d6\b@\n@\f@\u02d9\t@\u0001@\u0001@\u0001@\u0003"+
		"@\u02de\b@\u0001A\u0001A\u0001A\u0001A\u0003A\u02e4\bA\u0001B\u0001B\u0001"+
		"B\u0003B\u02e9\bB\u0001C\u0001C\u0001C\u0003C\u02ee\bC\u0004C\u02f0\b"+
		"C\u000bC\fC\u02f1\u0001D\u0003D\u02f5\bD\u0001D\u0000\u0000E\u0000\u0002"+
		"\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u001c\u001e"+
		" \"$&(*,.02468:<>@BDFHJLNPRTVXZ\\^`bdfhjlnprtvxz|~\u0080\u0082\u0084\u0086"+
		"\u0088\u0000\u0007\u0001\u0000+,\u0001\u0000-/\u0001\u000023\u0002\u0000"+
		" $\'(\u0001\u0000\u001e\u001f\u0001\u0000\u0003\u0003\u0001\u0000<<\u0378"+
		"\u0000\u008b\u0001\u0000\u0000\u0000\u0002\u0094\u0001\u0000\u0000\u0000"+
		"\u0004\u0097\u0001\u0000\u0000\u0000\u0006\u00a0\u0001\u0000\u0000\u0000"+
		"\b\u00a4\u0001\u0000\u0000\u0000\n\u00b5\u0001\u0000\u0000\u0000\f\u00b7"+
		"\u0001\u0000\u0000\u0000\u000e\u00ba\u0001\u0000\u0000\u0000\u0010\u00c1"+
		"\u0001\u0000\u0000\u0000\u0012\u00c3\u0001\u0000\u0000\u0000\u0014\u00c5"+
		"\u0001\u0000\u0000\u0000\u0016\u00cf\u0001\u0000\u0000\u0000\u0018\u00d7"+
		"\u0001\u0000\u0000\u0000\u001a\u00e4\u0001\u0000\u0000\u0000\u001c\u00e8"+
		"\u0001\u0000\u0000\u0000\u001e\u00ec\u0001\u0000\u0000\u0000 \u00f1\u0001"+
		"\u0000\u0000\u0000\"\u00ff\u0001\u0000\u0000\u0000$\u0101\u0001\u0000"+
		"\u0000\u0000&\u0105\u0001\u0000\u0000\u0000(\u0109\u0001\u0000\u0000\u0000"+
		"*\u010d\u0001\u0000\u0000\u0000,\u011c\u0001\u0000\u0000\u0000.\u0121"+
		"\u0001\u0000\u0000\u00000\u0124\u0001\u0000\u0000\u00002\u012c\u0001\u0000"+
		"\u0000\u00004\u012e\u0001\u0000\u0000\u00006\u0138\u0001\u0000\u0000\u0000"+
		"8\u014b\u0001\u0000\u0000\u0000:\u014f\u0001\u0000\u0000\u0000<\u0153"+
		"\u0001\u0000\u0000\u0000>\u0157\u0001\u0000\u0000\u0000@\u0177\u0001\u0000"+
		"\u0000\u0000B\u0179\u0001\u0000\u0000\u0000D\u017b\u0001\u0000\u0000\u0000"+
		"F\u0183\u0001\u0000\u0000\u0000H\u0188\u0001\u0000\u0000\u0000J\u01b1"+
		"\u0001\u0000\u0000\u0000L\u01b3\u0001\u0000\u0000\u0000N\u01b8\u0001\u0000"+
		"\u0000\u0000P\u01d3\u0001\u0000\u0000\u0000R\u01e2\u0001\u0000\u0000\u0000"+
		"T\u01e4\u0001\u0000\u0000\u0000V\u01e7\u0001\u0000\u0000\u0000X\u01eb"+
		"\u0001\u0000\u0000\u0000Z\u01f6\u0001\u0000\u0000\u0000\\\u01f8\u0001"+
		"\u0000\u0000\u0000^\u0201\u0001\u0000\u0000\u0000`\u0214\u0001\u0000\u0000"+
		"\u0000b\u0216\u0001\u0000\u0000\u0000d\u0218\u0001\u0000\u0000\u0000f"+
		"\u0224\u0001\u0000\u0000\u0000h\u0227\u0001\u0000\u0000\u0000j\u022e\u0001"+
		"\u0000\u0000\u0000l\u0233\u0001\u0000\u0000\u0000n\u0270\u0001\u0000\u0000"+
		"\u0000p\u0272\u0001\u0000\u0000\u0000r\u0289\u0001\u0000\u0000\u0000t"+
		"\u028b\u0001\u0000\u0000\u0000v\u0290\u0001\u0000\u0000\u0000x\u0297\u0001"+
		"\u0000\u0000\u0000z\u029a\u0001\u0000\u0000\u0000|\u02a6\u0001\u0000\u0000"+
		"\u0000~\u02ab\u0001\u0000\u0000\u0000\u0080\u02dd\u0001\u0000\u0000\u0000"+
		"\u0082\u02e3\u0001\u0000\u0000\u0000\u0084\u02e8\u0001\u0000\u0000\u0000"+
		"\u0086\u02ef\u0001\u0000\u0000\u0000\u0088\u02f4\u0001\u0000\u0000\u0000"+
		"\u008a\u008c\u0003\u0002\u0001\u0000\u008b\u008a\u0001\u0000\u0000\u0000"+
		"\u008b\u008c\u0001\u0000\u0000\u0000\u008c\u008d\u0001\u0000\u0000\u0000"+
		"\u008d\u008f\u0003\u0088D\u0000\u008e\u0090\u0003\u0004\u0002\u0000\u008f"+
		"\u008e\u0001\u0000\u0000\u0000\u008f\u0090\u0001\u0000\u0000\u0000\u0090"+
		"\u0091\u0001\u0000\u0000\u0000\u0091\u0092\u0003\u0088D\u0000\u0092\u0093"+
		"\u0005\u0000\u0000\u0001\u0093\u0001\u0001\u0000\u0000\u0000\u0094\u0095"+
		"\u0005\u0001\u0000\u0000\u0095\u0096\u0003\u0088D\u0000\u0096\u0003\u0001"+
		"\u0000\u0000\u0000\u0097\u009d\u0003\u0006\u0003\u0000\u0098\u0099\u0003"+
		"\u0086C\u0000\u0099\u009a\u0003\u0006\u0003\u0000\u009a\u009c\u0001\u0000"+
		"\u0000\u0000\u009b\u0098\u0001\u0000\u0000\u0000\u009c\u009f\u0001\u0000"+
		"\u0000\u0000\u009d\u009b\u0001\u0000\u0000\u0000\u009d\u009e\u0001\u0000"+
		"\u0000\u0000\u009e\u0005\u0001\u0000\u0000\u0000\u009f\u009d\u0001\u0000"+
		"\u0000\u0000\u00a0\u00a2\u0003\b\u0004\u0000\u00a1\u00a3\u0003\u0082A"+
		"\u0000\u00a2\u00a1\u0001\u0000\u0000\u0000\u00a2\u00a3\u0001\u0000\u0000"+
		"\u0000\u00a3\u0007\u0001\u0000\u0000\u0000\u00a4\u00aa\u0003\f\u0006\u0000"+
		"\u00a5\u00a6\u0003\n\u0005\u0000\u00a6\u00a7\u0003\f\u0006\u0000\u00a7"+
		"\u00a9\u0001\u0000\u0000\u0000\u00a8\u00a5\u0001\u0000\u0000\u0000\u00a9"+
		"\u00ac\u0001\u0000\u0000\u0000\u00aa\u00a8\u0001\u0000\u0000\u0000\u00aa"+
		"\u00ab\u0001\u0000\u0000\u0000\u00ab\t\u0001\u0000\u0000\u0000\u00ac\u00aa"+
		"\u0001\u0000\u0000\u0000\u00ad\u00ae\u0005)\u0000\u0000\u00ae\u00b6\u0003"+
		"\u0088D\u0000\u00af\u00b0\u0005*\u0000\u0000\u00b0\u00b6\u0003\u0088D"+
		"\u0000\u00b1\u00b2\u00050\u0000\u0000\u00b2\u00b6\u0003\u0088D\u0000\u00b3"+
		"\u00b4\u00051\u0000\u0000\u00b4\u00b6\u0003\u0088D\u0000\u00b5\u00ad\u0001"+
		"\u0000\u0000\u0000\u00b5\u00af\u0001\u0000\u0000\u0000\u00b5\u00b1\u0001"+
		"\u0000\u0000\u0000\u00b5\u00b3\u0001\u0000\u0000\u0000\u00b6\u000b\u0001"+
		"\u0000\u0000\u0000\u00b7\u00b8\u0003\u000e\u0007\u0000\u00b8\r\u0001\u0000"+
		"\u0000\u0000\u00b9\u00bb\u0003\u0010\b\u0000\u00ba\u00b9\u0001\u0000\u0000"+
		"\u0000\u00ba\u00bb\u0001\u0000\u0000\u0000\u00bb\u00bd\u0001\u0000\u0000"+
		"\u0000\u00bc\u00be\u0003\u0012\t\u0000\u00bd\u00bc\u0001\u0000\u0000\u0000"+
		"\u00bd\u00be\u0001\u0000\u0000\u0000\u00be\u00bf\u0001\u0000\u0000\u0000"+
		"\u00bf\u00c0\u0003\u0014\n\u0000\u00c0\u000f\u0001\u0000\u0000\u0000\u00c1"+
		"\u00c2\u0005:\u0000\u0000\u00c2\u0011\u0001\u0000\u0000\u0000\u00c3\u00c4"+
		"\u0005\u0015\u0000\u0000\u00c4\u0013\u0001\u0000\u0000\u0000\u00c5\u00cc"+
		"\u0003\u0018\f\u0000\u00c6\u00c7\u0003\u0016\u000b\u0000\u00c7\u00c8\u0003"+
		"\u0088D\u0000\u00c8\u00c9\u0003\u0018\f\u0000\u00c9\u00cb\u0001\u0000"+
		"\u0000\u0000\u00ca\u00c6\u0001\u0000\u0000\u0000\u00cb\u00ce\u0001\u0000"+
		"\u0000\u0000\u00cc\u00ca\u0001\u0000\u0000\u0000\u00cc\u00cd\u0001\u0000"+
		"\u0000\u0000\u00cd\u0015\u0001\u0000\u0000\u0000\u00ce\u00cc\u0001\u0000"+
		"\u0000\u0000\u00cf\u00d0\u0007\u0000\u0000\u0000\u00d0\u0017\u0001\u0000"+
		"\u0000\u0000\u00d1\u00d3\u0003\"\u0011\u0000\u00d2\u00d4\u0003^/\u0000"+
		"\u00d3\u00d2\u0001\u0000\u0000\u0000\u00d3\u00d4\u0001\u0000\u0000\u0000"+
		"\u00d4\u00d8\u0001\u0000\u0000\u0000\u00d5\u00d8\u0003R)\u0000\u00d6\u00d8"+
		"\u0003\u001a\r\u0000\u00d7\u00d1\u0001\u0000\u0000\u0000\u00d7\u00d5\u0001"+
		"\u0000\u0000\u0000\u00d7\u00d6\u0001\u0000\u0000\u0000\u00d8\u0019\u0001"+
		"\u0000\u0000\u0000\u00d9\u00db\u0003\u001c\u000e\u0000\u00da\u00dc\u0003"+
		"\u001e\u000f\u0000\u00db\u00da\u0001\u0000\u0000\u0000\u00db\u00dc\u0001"+
		"\u0000\u0000\u0000\u00dc\u00de\u0001\u0000\u0000\u0000\u00dd\u00df\u0003"+
		" \u0010\u0000\u00de\u00dd\u0001\u0000\u0000\u0000\u00de\u00df\u0001\u0000"+
		"\u0000\u0000\u00df\u00e5\u0001\u0000\u0000\u0000\u00e0\u00e2\u0003\u001e"+
		"\u000f\u0000\u00e1\u00e3\u0003 \u0010\u0000\u00e2\u00e1\u0001\u0000\u0000"+
		"\u0000\u00e2\u00e3\u0001\u0000\u0000\u0000\u00e3\u00e5\u0001\u0000\u0000"+
		"\u0000\u00e4\u00d9\u0001\u0000\u0000\u0000\u00e4\u00e0\u0001\u0000\u0000"+
		"\u0000\u00e5\u001b\u0001\u0000\u0000\u0000\u00e6\u00e9\u0003X,\u0000\u00e7"+
		"\u00e9\u0003`0\u0000\u00e8\u00e6\u0001\u0000\u0000\u0000\u00e8\u00e7\u0001"+
		"\u0000\u0000\u0000\u00e9\u00ea\u0001\u0000\u0000\u0000\u00ea\u00e8\u0001"+
		"\u0000\u0000\u0000\u00ea\u00eb\u0001\u0000\u0000\u0000\u00eb\u001d\u0001"+
		"\u0000\u0000\u0000\u00ec\u00ed\u0003l6\u0000\u00ed\u001f\u0001\u0000\u0000"+
		"\u0000\u00ee\u00f2\u0003X,\u0000\u00ef\u00f2\u0003l6\u0000\u00f0\u00f2"+
		"\u0003`0\u0000\u00f1\u00ee\u0001\u0000\u0000\u0000\u00f1\u00ef\u0001\u0000"+
		"\u0000\u0000\u00f1\u00f0\u0001\u0000\u0000\u0000\u00f2\u00f3\u0001\u0000"+
		"\u0000\u0000\u00f3\u00f1\u0001\u0000\u0000\u0000\u00f3\u00f4\u0001\u0000"+
		"\u0000\u0000\u00f4!\u0001\u0000\u0000\u0000\u00f5\u0100\u0003$\u0012\u0000"+
		"\u00f6\u0100\u0003&\u0013\u0000\u00f7\u0100\u0003*\u0015\u0000\u00f8\u0100"+
		"\u00030\u0018\u0000\u00f9\u0100\u00036\u001b\u0000\u00fa\u0100\u00038"+
		"\u001c\u0000\u00fb\u0100\u0003:\u001d\u0000\u00fc\u0100\u0003>\u001f\u0000"+
		"\u00fd\u0100\u0003F#\u0000\u00fe\u0100\u0003L&\u0000\u00ff\u00f5\u0001"+
		"\u0000\u0000\u0000\u00ff\u00f6\u0001\u0000\u0000\u0000\u00ff\u00f7\u0001"+
		"\u0000\u0000\u0000\u00ff\u00f8\u0001\u0000\u0000\u0000\u00ff\u00f9\u0001"+
		"\u0000\u0000\u0000\u00ff\u00fa\u0001\u0000\u0000\u0000\u00ff\u00fb\u0001"+
		"\u0000\u0000\u0000\u00ff\u00fc\u0001\u0000\u0000\u0000\u00ff\u00fd\u0001"+
		"\u0000\u0000\u0000\u00ff\u00fe\u0001\u0000\u0000\u0000\u0100#\u0001\u0000"+
		"\u0000\u0000\u0101\u0102\u00056\u0000\u0000\u0102\u0103\u0003(\u0014\u0000"+
		"\u0103\u0104\u00057\u0000\u0000\u0104%\u0001\u0000\u0000\u0000\u0105\u0106"+
		"\u00054\u0000\u0000\u0106\u0107\u0003(\u0014\u0000\u0107\u0108\u00055"+
		"\u0000\u0000\u0108\'\u0001\u0000\u0000\u0000\u0109\u010a\u0003\u0088D"+
		"\u0000\u010a\u010b\u0003\u0004\u0002\u0000\u010b\u010c\u0003\u0088D\u0000"+
		"\u010c)\u0001\u0000\u0000\u0000\u010d\u010e\u0005\u0005\u0000\u0000\u010e"+
		"\u010f\u0003(\u0014\u0000\u010f\u0110\u0005\u0006\u0000\u0000\u0110\u0114"+
		"\u0003(\u0014\u0000\u0111\u0113\u0003,\u0016\u0000\u0112\u0111\u0001\u0000"+
		"\u0000\u0000\u0113\u0116\u0001\u0000\u0000\u0000\u0114\u0112\u0001\u0000"+
		"\u0000\u0000\u0114\u0115\u0001\u0000\u0000\u0000\u0115\u0118\u0001\u0000"+
		"\u0000\u0000\u0116\u0114\u0001\u0000\u0000\u0000\u0117\u0119\u0003.\u0017"+
		"\u0000\u0118\u0117\u0001\u0000\u0000\u0000\u0118\u0119\u0001\u0000\u0000"+
		"\u0000\u0119\u011a\u0001\u0000\u0000\u0000\u011a\u011b\u0005\t\u0000\u0000"+
		"\u011b+\u0001\u0000\u0000\u0000\u011c\u011d\u0005\b\u0000\u0000\u011d"+
		"\u011e\u0003(\u0014\u0000\u011e\u011f\u0005\u0006\u0000\u0000\u011f\u0120"+
		"\u0003(\u0014\u0000\u0120-\u0001\u0000\u0000\u0000\u0121\u0122\u0005\u0007"+
		"\u0000\u0000\u0122\u0123\u0003(\u0014\u0000\u0123/\u0001\u0000\u0000\u0000"+
		"\u0124\u0125\u0005\n\u0000\u0000\u0125\u0126\u0005C\u0000\u0000\u0126"+
		"\u0127\u00032\u0019\u0000\u01271\u0001\u0000\u0000\u0000\u0128\u0129\u0003"+
		"4\u001a\u0000\u0129\u012a\u0003<\u001e\u0000\u012a\u012d\u0001\u0000\u0000"+
		"\u0000\u012b\u012d\u0003<\u001e\u0000\u012c\u0128\u0001\u0000\u0000\u0000"+
		"\u012c\u012b\u0001\u0000\u0000\u0000\u012d3\u0001\u0000\u0000\u0000\u012e"+
		"\u012f\u0003\u0088D\u0000\u012f\u0133\u0005\u0011\u0000\u0000\u0130\u0132"+
		"\u0003l6\u0000\u0131\u0130\u0001\u0000\u0000\u0000\u0132\u0135\u0001\u0000"+
		"\u0000\u0000\u0133\u0131\u0001\u0000\u0000\u0000\u0133\u0134\u0001\u0000"+
		"\u0000\u0000\u0134\u0136\u0001\u0000\u0000\u0000\u0135\u0133\u0001\u0000"+
		"\u0000\u0000\u0136\u0137\u0003\u0084B\u0000\u01375\u0001\u0000\u0000\u0000"+
		"\u0138\u0139\u0005\n\u0000\u0000\u0139\u013b\u0005\u0018\u0000\u0000\u013a"+
		"\u013c\u0003H$\u0000\u013b\u013a\u0001\u0000\u0000\u0000\u013b\u013c\u0001"+
		"\u0000\u0000\u0000\u013c\u013d\u0001\u0000\u0000\u0000\u013d\u013f\u0005"+
		"0\u0000\u0000\u013e\u0140\u0003H$\u0000\u013f\u013e\u0001\u0000\u0000"+
		"\u0000\u013f\u0140\u0001\u0000\u0000\u0000\u0140\u0141\u0001\u0000\u0000"+
		"\u0000\u0141\u0143\u00050\u0000\u0000\u0142\u0144\u0003H$\u0000\u0143"+
		"\u0142\u0001\u0000\u0000\u0000\u0143\u0144\u0001\u0000\u0000\u0000\u0144"+
		"\u0145\u0001\u0000\u0000\u0000\u0145\u0147\u0005\u0019\u0000\u0000\u0146"+
		"\u0148\u0003\u0084B\u0000\u0147\u0146\u0001\u0000\u0000\u0000\u0147\u0148"+
		"\u0001\u0000\u0000\u0000\u0148\u0149\u0001\u0000\u0000\u0000\u0149\u014a"+
		"\u0003<\u001e\u0000\u014a7\u0001\u0000\u0000\u0000\u014b\u014c\u0005\u000b"+
		"\u0000\u0000\u014c\u014d\u0003(\u0014\u0000\u014d\u014e\u0003<\u001e\u0000"+
		"\u014e9\u0001\u0000\u0000\u0000\u014f\u0150\u0005\f\u0000\u0000\u0150"+
		"\u0151\u0003(\u0014\u0000\u0151\u0152\u0003<\u001e\u0000\u0152;\u0001"+
		"\u0000\u0000\u0000\u0153\u0154\u0005\r\u0000\u0000\u0154\u0155\u0003("+
		"\u0014\u0000\u0155\u0156\u0005\u000e\u0000\u0000\u0156=\u0001\u0000\u0000"+
		"\u0000\u0157\u0158\u0005\u000f\u0000\u0000\u0158\u0159\u0003l6\u0000\u0159"+
		"\u015a\u0003\u0088D\u0000\u015a\u015b\u0005\u0011\u0000\u0000\u015b\u015f"+
		"\u0003\u0088D\u0000\u015c\u015e\u0003@ \u0000\u015d\u015c\u0001\u0000"+
		"\u0000\u0000\u015e\u0161\u0001\u0000\u0000\u0000\u015f\u015d\u0001\u0000"+
		"\u0000\u0000\u015f\u0160\u0001\u0000\u0000\u0000\u0160\u0162\u0001\u0000"+
		"\u0000\u0000\u0161\u015f\u0001\u0000\u0000\u0000\u0162\u0163\u0005\u0010"+
		"\u0000\u0000\u0163?\u0001\u0000\u0000\u0000\u0164\u0166\u00054\u0000\u0000"+
		"\u0165\u0164\u0001\u0000\u0000\u0000\u0165\u0166\u0001\u0000\u0000\u0000"+
		"\u0166\u0167\u0001\u0000\u0000\u0000\u0167\u0168\u0003D\"\u0000\u0168"+
		"\u0169\u00055\u0000\u0000\u0169\u016b\u0003\u0088D\u0000\u016a\u016c\u0003"+
		"(\u0014\u0000\u016b\u016a\u0001\u0000\u0000\u0000\u016b\u016c\u0001\u0000"+
		"\u0000\u0000\u016c\u016d\u0001\u0000\u0000\u0000\u016d\u016e\u0003B!\u0000"+
		"\u016e\u016f\u0003\u0088D\u0000\u016f\u0178\u0001\u0000\u0000\u0000\u0170"+
		"\u0172\u00054\u0000\u0000\u0171\u0170\u0001\u0000\u0000\u0000\u0171\u0172"+
		"\u0001\u0000\u0000\u0000\u0172\u0173\u0001\u0000\u0000\u0000\u0173\u0174"+
		"\u0003D\"\u0000\u0174\u0175\u00055\u0000\u0000\u0175\u0176\u0003\u0088"+
		"D\u0000\u0176\u0178\u0001\u0000\u0000\u0000\u0177\u0165\u0001\u0000\u0000"+
		"\u0000\u0177\u0171\u0001\u0000\u0000\u0000\u0178A\u0001\u0000\u0000\u0000"+
		"\u0179\u017a\u0007\u0001\u0000\u0000\u017aC\u0001\u0000\u0000\u0000\u017b"+
		"\u0180\u0003l6\u0000\u017c\u017d\u0005,\u0000\u0000\u017d\u017f\u0003"+
		"l6\u0000\u017e\u017c\u0001\u0000\u0000\u0000\u017f\u0182\u0001\u0000\u0000"+
		"\u0000\u0180\u017e\u0001\u0000\u0000\u0000\u0180\u0181\u0001\u0000\u0000"+
		"\u0000\u0181E\u0001\u0000\u0000\u0000\u0182\u0180\u0001\u0000\u0000\u0000"+
		"\u0183\u0184\u0005\u0018\u0000\u0000\u0184\u0185\u0003H$\u0000\u0185\u0186"+
		"\u0005\u0019\u0000\u0000\u0186G\u0001\u0000\u0000\u0000\u0187\u0189\u0003"+
		"J%\u0000\u0188\u0187\u0001\u0000\u0000\u0000\u0189\u018a\u0001\u0000\u0000"+
		"\u0000\u018a\u0188\u0001\u0000\u0000\u0000\u018a\u018b\u0001\u0000\u0000"+
		"\u0000\u018bI\u0001\u0000\u0000\u0000\u018c\u01b2\u0005C\u0000\u0000\u018d"+
		"\u01b2\u0005B\u0000\u0000\u018e\u01b2\u0005>\u0000\u0000\u018f\u01b2\u0005"+
		"=\u0000\u0000\u0190\u0191\u0005\u001c\u0000\u0000\u0191\u0192\u0003~?"+
		"\u0000\u0192\u0193\u00057\u0000\u0000\u0193\u01b2\u0001\u0000\u0000\u0000"+
		"\u0194\u0195\u0005\u001b\u0000\u0000\u0195\u0196\u0003v;\u0000\u0196\u0197"+
		"\u00055\u0000\u0000\u0197\u01b2\u0001\u0000\u0000\u0000\u0198\u0199\u0005"+
		"\u001a\u0000\u0000\u0199\u019a\u0003H$\u0000\u019a\u019b\u0005\u0019\u0000"+
		"\u0000\u019b\u01b2\u0001\u0000\u0000\u0000\u019c\u019d\u00054\u0000\u0000"+
		"\u019d\u019e\u0003H$\u0000\u019e\u019f\u00055\u0000\u0000\u019f\u01b2"+
		"\u0001\u0000\u0000\u0000\u01a0\u01b2\u00053\u0000\u0000\u01a1\u01b2\u0005"+
		"2\u0000\u0000\u01a2\u01b2\u0005:\u0000\u0000\u01a3\u01b2\u0005\'\u0000"+
		"\u0000\u01a4\u01b2\u0005(\u0000\u0000\u01a5\u01b2\u0005E\u0000\u0000\u01a6"+
		"\u01b2\u0005F\u0000\u0000\u01a7\u01b2\u0005)\u0000\u0000\u01a8\u01b2\u0005"+
		"*\u0000\u0000\u01a9\u01b2\u00051\u0000\u0000\u01aa\u01b2\u0005,\u0000"+
		"\u0000\u01ab\u01b2\u0005;\u0000\u0000\u01ac\u01b2\u00050\u0000\u0000\u01ad"+
		"\u01b2\u00058\u0000\u0000\u01ae\u01b2\u00059\u0000\u0000\u01af\u01b2\u0005"+
		"G\u0000\u0000\u01b0\u01b2\u0005D\u0000\u0000\u01b1\u018c\u0001\u0000\u0000"+
		"\u0000\u01b1\u018d\u0001\u0000\u0000\u0000\u01b1\u018e\u0001\u0000\u0000"+
		"\u0000\u01b1\u018f\u0001\u0000\u0000\u0000\u01b1\u0190\u0001\u0000\u0000"+
		"\u0000\u01b1\u0194\u0001\u0000\u0000\u0000\u01b1\u0198\u0001\u0000\u0000"+
		"\u0000\u01b1\u019c\u0001\u0000\u0000\u0000\u01b1\u01a0\u0001\u0000\u0000"+
		"\u0000\u01b1\u01a1\u0001\u0000\u0000\u0000\u01b1\u01a2\u0001\u0000\u0000"+
		"\u0000\u01b1\u01a3\u0001\u0000\u0000\u0000\u01b1\u01a4\u0001\u0000\u0000"+
		"\u0000\u01b1\u01a5\u0001\u0000\u0000\u0000\u01b1\u01a6\u0001\u0000\u0000"+
		"\u0000\u01b1\u01a7\u0001\u0000\u0000\u0000\u01b1\u01a8\u0001\u0000\u0000"+
		"\u0000\u01b1\u01a9\u0001\u0000\u0000\u0000\u01b1\u01aa\u0001\u0000\u0000"+
		"\u0000\u01b1\u01ab\u0001\u0000\u0000\u0000\u01b1\u01ac\u0001\u0000\u0000"+
		"\u0000\u01b1\u01ad\u0001\u0000\u0000\u0000\u01b1\u01ae\u0001\u0000\u0000"+
		"\u0000\u01b1\u01af\u0001\u0000\u0000\u0000\u01b1\u01b0\u0001\u0000\u0000"+
		"\u0000\u01b2K\u0001\u0000\u0000\u0000\u01b3\u01b4\u0005\u0016\u0000\u0000"+
		"\u01b4\u01b5\u0003N\'\u0000\u01b5\u01b6\u0005\u0017\u0000\u0000\u01b6"+
		"M\u0001\u0000\u0000\u0000\u01b7\u01b9\u0003P(\u0000\u01b8\u01b7\u0001"+
		"\u0000\u0000\u0000\u01b9\u01ba\u0001\u0000\u0000\u0000\u01ba\u01b8\u0001"+
		"\u0000\u0000\u0000\u01ba\u01bb\u0001\u0000\u0000\u0000\u01bbO\u0001\u0000"+
		"\u0000\u0000\u01bc\u01d4\u0003l6\u0000\u01bd\u01d4\u0005\'\u0000\u0000"+
		"\u01be\u01d4\u0005(\u0000\u0000\u01bf\u01d4\u0005:\u0000\u0000\u01c0\u01d4"+
		"\u00053\u0000\u0000\u01c1\u01d4\u0005)\u0000\u0000\u01c2\u01d4\u0005*"+
		"\u0000\u0000\u01c3\u01d4\u0005,\u0000\u0000\u01c4\u01d4\u00051\u0000\u0000"+
		"\u01c5\u01d4\u00050\u0000\u0000\u01c6\u01d4\u00056\u0000\u0000\u01c7\u01d4"+
		"\u00057\u0000\u0000\u01c8\u01d4\u0005D\u0000\u0000\u01c9\u01d4\u0005\u0016"+
		"\u0000\u0000\u01ca\u01d4\u0005\u0017\u0000\u0000\u01cb\u01d4\u0005\u0018"+
		"\u0000\u0000\u01cc\u01d4\u0005\u0019\u0000\u0000\u01cd\u01d4\u0005\u0003"+
		"\u0000\u0000\u01ce\u01d4\u0005\u0004\u0000\u0000\u01cf\u01d0\u00054\u0000"+
		"\u0000\u01d0\u01d1\u0003N\'\u0000\u01d1\u01d2\u00055\u0000\u0000\u01d2"+
		"\u01d4\u0001\u0000\u0000\u0000\u01d3\u01bc\u0001\u0000\u0000\u0000\u01d3"+
		"\u01bd\u0001\u0000\u0000\u0000\u01d3\u01be\u0001\u0000\u0000\u0000\u01d3"+
		"\u01bf\u0001\u0000\u0000\u0000\u01d3\u01c0\u0001\u0000\u0000\u0000\u01d3"+
		"\u01c1\u0001\u0000\u0000\u0000\u01d3\u01c2\u0001\u0000\u0000\u0000\u01d3"+
		"\u01c3\u0001\u0000\u0000\u0000\u01d3\u01c4\u0001\u0000\u0000\u0000\u01d3"+
		"\u01c5\u0001\u0000\u0000\u0000\u01d3\u01c6\u0001\u0000\u0000\u0000\u01d3"+
		"\u01c7\u0001\u0000\u0000\u0000\u01d3\u01c8\u0001\u0000\u0000\u0000\u01d3"+
		"\u01c9\u0001\u0000\u0000\u0000\u01d3\u01ca\u0001\u0000\u0000\u0000\u01d3"+
		"\u01cb\u0001\u0000\u0000\u0000\u01d3\u01cc\u0001\u0000\u0000\u0000\u01d3"+
		"\u01cd\u0001\u0000\u0000\u0000\u01d3\u01ce\u0001\u0000\u0000\u0000\u01d3"+
		"\u01cf\u0001\u0000\u0000\u0000\u01d4Q\u0001\u0000\u0000\u0000\u01d5\u01d6"+
		"\u0005\u0012\u0000\u0000\u01d6\u01d8\u0005C\u0000\u0000\u01d7\u01d9\u0003"+
		"T*\u0000\u01d8\u01d7\u0001\u0000\u0000\u0000\u01d8\u01d9\u0001\u0000\u0000"+
		"\u0000\u01d9\u01da\u0001\u0000\u0000\u0000\u01da\u01db\u0003\u0088D\u0000"+
		"\u01db\u01dc\u0003V+\u0000\u01dc\u01e3\u0001\u0000\u0000\u0000\u01dd\u01de"+
		"\u0005C\u0000\u0000\u01de\u01df\u0003T*\u0000\u01df\u01e0\u0003\u0088"+
		"D\u0000\u01e0\u01e1\u0003V+\u0000\u01e1\u01e3\u0001\u0000\u0000\u0000"+
		"\u01e2\u01d5\u0001\u0000\u0000\u0000\u01e2\u01dd\u0001\u0000\u0000\u0000"+
		"\u01e3S\u0001\u0000\u0000\u0000\u01e4\u01e5\u00054\u0000\u0000\u01e5\u01e6"+
		"\u00055\u0000\u0000\u01e6U\u0001\u0000\u0000\u0000\u01e7\u01e9\u0003\""+
		"\u0011\u0000\u01e8\u01ea\u0003^/\u0000\u01e9\u01e8\u0001\u0000\u0000\u0000"+
		"\u01e9\u01ea\u0001\u0000\u0000\u0000\u01eaW\u0001\u0000\u0000\u0000\u01eb"+
		"\u01ec\u0005C\u0000\u0000\u01ec\u01ee\u0007\u0002\u0000\u0000\u01ed\u01ef"+
		"\u0003Z-\u0000\u01ee\u01ed\u0001\u0000\u0000\u0000\u01ee\u01ef\u0001\u0000"+
		"\u0000\u0000\u01efY\u0001\u0000\u0000\u0000\u01f0\u01f7\u0003l6\u0000"+
		"\u01f1\u01f3\u00054\u0000\u0000\u01f2\u01f4\u0003\\.\u0000\u01f3\u01f2"+
		"\u0001\u0000\u0000\u0000\u01f3\u01f4\u0001\u0000\u0000\u0000\u01f4\u01f5"+
		"\u0001\u0000\u0000\u0000\u01f5\u01f7\u00055\u0000\u0000\u01f6\u01f0\u0001"+
		"\u0000\u0000\u0000\u01f6\u01f1\u0001\u0000\u0000\u0000\u01f7[\u0001\u0000"+
		"\u0000\u0000\u01f8\u01fc\u0003\u0088D\u0000\u01f9\u01fa\u0003l6\u0000"+
		"\u01fa\u01fb\u0003\u0088D\u0000\u01fb\u01fd\u0001\u0000\u0000\u0000\u01fc"+
		"\u01f9\u0001\u0000\u0000\u0000\u01fd\u01fe\u0001\u0000\u0000\u0000\u01fe"+
		"\u01fc\u0001\u0000\u0000\u0000\u01fe\u01ff\u0001\u0000\u0000\u0000\u01ff"+
		"]\u0001\u0000\u0000\u0000\u0200\u0202\u0003`0\u0000\u0201\u0200\u0001"+
		"\u0000\u0000\u0000\u0202\u0203\u0001\u0000\u0000\u0000\u0203\u0201\u0001"+
		"\u0000\u0000\u0000\u0203\u0204\u0001\u0000\u0000\u0000\u0204_\u0001\u0000"+
		"\u0000\u0000\u0205\u0207\u0005B\u0000\u0000\u0206\u0205\u0001\u0000\u0000"+
		"\u0000\u0206\u0207\u0001\u0000\u0000\u0000\u0207\u0208\u0001\u0000\u0000"+
		"\u0000\u0208\u0209\u0003b1\u0000\u0209\u020a\u0003l6\u0000\u020a\u0215"+
		"\u0001\u0000\u0000\u0000\u020b\u020d\u0005B\u0000\u0000\u020c\u020b\u0001"+
		"\u0000\u0000\u0000\u020c\u020d\u0001\u0000\u0000\u0000\u020d\u020e\u0001"+
		"\u0000\u0000\u0000\u020e\u020f\u0005\u001d\u0000\u0000\u020f\u0215\u0003"+
		"l6\u0000\u0210\u0212\u0005B\u0000\u0000\u0211\u0210\u0001\u0000\u0000"+
		"\u0000\u0211\u0212\u0001\u0000\u0000\u0000\u0212\u0213\u0001\u0000\u0000"+
		"\u0000\u0213\u0215\u0003d2\u0000\u0214\u0206\u0001\u0000\u0000\u0000\u0214"+
		"\u020c\u0001\u0000\u0000\u0000\u0214\u0211\u0001\u0000\u0000\u0000\u0215"+
		"a\u0001\u0000\u0000\u0000\u0216\u0217\u0007\u0003\u0000\u0000\u0217c\u0001"+
		"\u0000\u0000\u0000\u0218\u0219\u0007\u0004\u0000\u0000\u0219\u021a\u0003"+
		"f3\u0000\u021a\u021b\u0005\u0003\u0000\u0000\u021b\u021c\u0003h4\u0000"+
		"\u021ce\u0001\u0000\u0000\u0000\u021d\u0225\u0005C\u0000\u0000\u021e\u0225"+
		"\u0005@\u0000\u0000\u021f\u0221\u0005A\u0000\u0000\u0220\u0222\u0005K"+
		"\u0000\u0000\u0221\u0220\u0001\u0000\u0000\u0000\u0221\u0222\u0001\u0000"+
		"\u0000\u0000\u0222\u0223\u0001\u0000\u0000\u0000\u0223\u0225\u0005A\u0000"+
		"\u0000\u0224\u021d\u0001\u0000\u0000\u0000\u0224\u021e\u0001\u0000\u0000"+
		"\u0000\u0224\u021f\u0001\u0000\u0000\u0000\u0225g\u0001\u0000\u0000\u0000"+
		"\u0226\u0228\u0003j5\u0000\u0227\u0226\u0001\u0000\u0000\u0000\u0228\u0229"+
		"\u0001\u0000\u0000\u0000\u0229\u0227\u0001\u0000\u0000\u0000\u0229\u022a"+
		"\u0001\u0000\u0000\u0000\u022ai\u0001\u0000\u0000\u0000\u022b\u022d\b"+
		"\u0005\u0000\u0000\u022c\u022b\u0001\u0000\u0000\u0000\u022d\u0230\u0001"+
		"\u0000\u0000\u0000\u022e\u022c\u0001\u0000\u0000\u0000\u022e\u022f\u0001"+
		"\u0000\u0000\u0000\u022f\u0231\u0001\u0000\u0000\u0000\u0230\u022e\u0001"+
		"\u0000\u0000\u0000\u0231\u0232\u0005\u0003\u0000\u0000\u0232k\u0001\u0000"+
		"\u0000\u0000\u0233\u0238\u0003n7\u0000\u0234\u0235\u00046\u0000\u0000"+
		"\u0235\u0237\u0003n7\u0000\u0236\u0234\u0001\u0000\u0000\u0000\u0237\u023a"+
		"\u0001\u0000\u0000\u0000\u0238\u0236\u0001\u0000\u0000\u0000\u0238\u0239"+
		"\u0001\u0000\u0000\u0000\u0239m\u0001\u0000\u0000\u0000\u023a\u0238\u0001"+
		"\u0000\u0000\u0000\u023b\u0271\u0005C\u0000\u0000\u023c\u0271\u0005B\u0000"+
		"\u0000\u023d\u0271\u0005@\u0000\u0000\u023e\u0271\u0005?\u0000\u0000\u023f"+
		"\u0271\u0003p8\u0000\u0240\u0271\u0005>\u0000\u0000\u0241\u0271\u0005"+
		"=\u0000\u0000\u0242\u0243\u0005\u001c\u0000\u0000\u0243\u0244\u0003~?"+
		"\u0000\u0244\u0245\u00057\u0000\u0000\u0245\u0271\u0001\u0000\u0000\u0000"+
		"\u0246\u0271\u0003t:\u0000\u0247\u0248\u0005<\u0000\u0000\u0248\u0249"+
		"\u0003x<\u0000\u0249\u024a\u0005<\u0000\u0000\u024a\u0271\u0001\u0000"+
		"\u0000\u0000\u024b\u0271\u0003z=\u0000\u024c\u0271\u0003|>\u0000\u024d"+
		"\u0271\u0005E\u0000\u0000\u024e\u0271\u0005F\u0000\u0000\u024f\u0271\u0005"+
		"G\u0000\u0000\u0250\u0271\u0005;\u0000\u0000\u0251\u0271\u0005D\u0000"+
		"\u0000\u0252\u0271\u00058\u0000\u0000\u0253\u0271\u00059\u0000\u0000\u0254"+
		"\u0271\u0005:\u0000\u0000\u0255\u0271\u00053\u0000\u0000\u0256\u0271\u0005"+
		"2\u0000\u0000\u0257\u025b\u00056\u0000\u0000\u0258\u025a\u0003n7\u0000"+
		"\u0259\u0258\u0001\u0000\u0000\u0000\u025a\u025d\u0001\u0000\u0000\u0000"+
		"\u025b\u0259\u0001\u0000\u0000\u0000\u025b\u025c\u0001\u0000\u0000\u0000"+
		"\u025c\u025e\u0001\u0000\u0000\u0000\u025d\u025b\u0001\u0000\u0000\u0000"+
		"\u025e\u0271\u00057\u0000\u0000\u025f\u0271\u0005\u0005\u0000\u0000\u0260"+
		"\u0271\u0005\u0006\u0000\u0000\u0261\u0271\u0005\u0007\u0000\u0000\u0262"+
		"\u0271\u0005\b\u0000\u0000\u0263\u0271\u0005\t\u0000\u0000\u0264\u0271"+
		"\u0005\n\u0000\u0000\u0265\u0271\u0005\u000b\u0000\u0000\u0266\u0271\u0005"+
		"\f\u0000\u0000\u0267\u0271\u0005\r\u0000\u0000\u0268\u0271\u0005\u000e"+
		"\u0000\u0000\u0269\u0271\u0005\u000f\u0000\u0000\u026a\u0271\u0005\u0010"+
		"\u0000\u0000\u026b\u0271\u0005\u0011\u0000\u0000\u026c\u0271\u0005\u0012"+
		"\u0000\u0000\u026d\u0271\u0005\u0013\u0000\u0000\u026e\u0271\u0005\u0014"+
		"\u0000\u0000\u026f\u0271\u0005\u0015\u0000\u0000\u0270\u023b\u0001\u0000"+
		"\u0000\u0000\u0270\u023c\u0001\u0000\u0000\u0000\u0270\u023d\u0001\u0000"+
		"\u0000\u0000\u0270\u023e\u0001\u0000\u0000\u0000\u0270\u023f\u0001\u0000"+
		"\u0000\u0000\u0270\u0240\u0001\u0000\u0000\u0000\u0270\u0241\u0001\u0000"+
		"\u0000\u0000\u0270\u0242\u0001\u0000\u0000\u0000\u0270\u0246\u0001\u0000"+
		"\u0000\u0000\u0270\u0247\u0001\u0000\u0000\u0000\u0270\u024b\u0001\u0000"+
		"\u0000\u0000\u0270\u024c\u0001\u0000\u0000\u0000\u0270\u024d\u0001\u0000"+
		"\u0000\u0000\u0270\u024e\u0001\u0000\u0000\u0000\u0270\u024f\u0001\u0000"+
		"\u0000\u0000\u0270\u0250\u0001\u0000\u0000\u0000\u0270\u0251\u0001\u0000"+
		"\u0000\u0000\u0270\u0252\u0001\u0000\u0000\u0000\u0270\u0253\u0001\u0000"+
		"\u0000\u0000\u0270\u0254\u0001\u0000\u0000\u0000\u0270\u0255\u0001\u0000"+
		"\u0000\u0000\u0270\u0256\u0001\u0000\u0000\u0000\u0270\u0257\u0001\u0000"+
		"\u0000\u0000\u0270\u025f\u0001\u0000\u0000\u0000\u0270\u0260\u0001\u0000"+
		"\u0000\u0000\u0270\u0261\u0001\u0000\u0000\u0000\u0270\u0262\u0001\u0000"+
		"\u0000\u0000\u0270\u0263\u0001\u0000\u0000\u0000\u0270\u0264\u0001\u0000"+
		"\u0000\u0000\u0270\u0265\u0001\u0000\u0000\u0000\u0270\u0266\u0001\u0000"+
		"\u0000\u0000\u0270\u0267\u0001\u0000\u0000\u0000\u0270\u0268\u0001\u0000"+
		"\u0000\u0000\u0270\u0269\u0001\u0000\u0000\u0000\u0270\u026a\u0001\u0000"+
		"\u0000\u0000\u0270\u026b\u0001\u0000\u0000\u0000\u0270\u026c\u0001\u0000"+
		"\u0000\u0000\u0270\u026d\u0001\u0000\u0000\u0000\u0270\u026e\u0001\u0000"+
		"\u0000\u0000\u0270\u026f\u0001\u0000\u0000\u0000\u0271o\u0001\u0000\u0000"+
		"\u0000\u0272\u0276\u0005A\u0000\u0000\u0273\u0275\u0003r9\u0000\u0274"+
		"\u0273\u0001\u0000\u0000\u0000\u0275\u0278\u0001\u0000\u0000\u0000\u0276"+
		"\u0274\u0001\u0000\u0000\u0000\u0276\u0277\u0001\u0000\u0000\u0000\u0277"+
		"\u0279\u0001\u0000\u0000\u0000\u0278\u0276\u0001\u0000\u0000\u0000\u0279"+
		"\u027a\u0005A\u0000\u0000\u027aq\u0001\u0000\u0000\u0000\u027b\u028a\u0005"+
		"K\u0000\u0000\u027c\u028a\u0005J\u0000\u0000\u027d\u028a\u0005>\u0000"+
		"\u0000\u027e\u028a\u0005=\u0000\u0000\u027f\u0280\u0005\u001c\u0000\u0000"+
		"\u0280\u0281\u0003~?\u0000\u0281\u0282\u00057\u0000\u0000\u0282\u028a"+
		"\u0001\u0000\u0000\u0000\u0283\u028a\u0003t:\u0000\u0284\u028a\u0003z"+
		"=\u0000\u0285\u0286\u0005<\u0000\u0000\u0286\u0287\u0003x<\u0000\u0287"+
		"\u0288\u0005<\u0000\u0000\u0288\u028a\u0001\u0000\u0000\u0000\u0289\u027b"+
		"\u0001\u0000\u0000\u0000\u0289\u027c\u0001\u0000\u0000\u0000\u0289\u027d"+
		"\u0001\u0000\u0000\u0000\u0289\u027e\u0001\u0000\u0000\u0000\u0289\u027f"+
		"\u0001\u0000\u0000\u0000\u0289\u0283\u0001\u0000\u0000\u0000\u0289\u0284"+
		"\u0001\u0000\u0000\u0000\u0289\u0285\u0001\u0000\u0000\u0000\u028as\u0001"+
		"\u0000\u0000\u0000\u028b\u028c\u0005\u001b\u0000\u0000\u028c\u028d\u0003"+
		"v;\u0000\u028d\u028e\u00055\u0000\u0000\u028eu\u0001\u0000\u0000\u0000"+
		"\u028f\u0291\u0003\u0004\u0002\u0000\u0290\u028f\u0001\u0000\u0000\u0000"+
		"\u0290\u0291\u0001\u0000\u0000\u0000\u0291\u0292\u0001\u0000\u0000\u0000"+
		"\u0292\u0293\u0003\u0088D\u0000\u0293w\u0001\u0000\u0000\u0000\u0294\u0296"+
		"\b\u0006\u0000\u0000\u0295\u0294\u0001\u0000\u0000\u0000\u0296\u0299\u0001"+
		"\u0000\u0000\u0000\u0297\u0295\u0001\u0000\u0000\u0000\u0297\u0298\u0001"+
		"\u0000\u0000\u0000\u0298y\u0001\u0000\u0000\u0000\u0299\u0297\u0001\u0000"+
		"\u0000\u0000\u029a\u029b\u0005\u001a\u0000\u0000\u029b\u029c\u0003H$\u0000"+
		"\u029c\u029d\u0005\u0019\u0000\u0000\u029d{\u0001\u0000\u0000\u0000\u029e"+
		"\u029f\u0005%\u0000\u0000\u029f\u02a0\u0003v;\u0000\u02a0\u02a1\u0005"+
		"5\u0000\u0000\u02a1\u02a7\u0001\u0000\u0000\u0000\u02a2\u02a3\u0005&\u0000"+
		"\u0000\u02a3\u02a4\u0003v;\u0000\u02a4\u02a5\u00055\u0000\u0000\u02a5"+
		"\u02a7\u0001\u0000\u0000\u0000\u02a6\u029e\u0001\u0000\u0000\u0000\u02a6"+
		"\u02a2\u0001\u0000\u0000\u0000\u02a7}\u0001\u0000\u0000\u0000\u02a8\u02aa"+
		"\u0003\u0080@\u0000\u02a9\u02a8\u0001\u0000\u0000\u0000\u02aa\u02ad\u0001"+
		"\u0000\u0000\u0000\u02ab\u02a9\u0001\u0000\u0000\u0000\u02ab\u02ac\u0001"+
		"\u0000\u0000\u0000\u02ac\u007f\u0001\u0000\u0000\u0000\u02ad\u02ab\u0001"+
		"\u0000\u0000\u0000\u02ae\u02de\u0005`\u0000\u0000\u02af\u02de\u0005a\u0000"+
		"\u0000\u02b0\u02de\u0005b\u0000\u0000\u02b1\u02de\u0005L\u0000\u0000\u02b2"+
		"\u02de\u0005M\u0000\u0000\u02b3\u02de\u0005N\u0000\u0000\u02b4\u02de\u0005"+
		"O\u0000\u0000\u02b5\u02de\u0005Q\u0000\u0000\u02b6\u02de\u0005P\u0000"+
		"\u0000\u02b7\u02de\u0005S\u0000\u0000\u02b8\u02de\u0005R\u0000\u0000\u02b9"+
		"\u02de\u0005U\u0000\u0000\u02ba\u02de\u0005T\u0000\u0000\u02bb\u02de\u0005"+
		"W\u0000\u0000\u02bc\u02de\u0005V\u0000\u0000\u02bd\u02de\u0005Y\u0000"+
		"\u0000\u02be\u02de\u0005X\u0000\u0000\u02bf\u02de\u0005Z\u0000\u0000\u02c0"+
		"\u02de\u0005[\u0000\u0000\u02c1\u02de\u0005\\\u0000\u0000\u02c2\u02de"+
		"\u0005]\u0000\u0000\u02c3\u02de\u0005^\u0000\u0000\u02c4\u02de\u0005_"+
		"\u0000\u0000\u02c5\u02c6\u0005\u001c\u0000\u0000\u02c6\u02c7\u0003~?\u0000"+
		"\u02c7\u02c8\u00057\u0000\u0000\u02c8\u02de\u0001\u0000\u0000\u0000\u02c9"+
		"\u02ca\u0005\u001b\u0000\u0000\u02ca\u02cb\u0003v;\u0000\u02cb\u02cc\u0005"+
		"5\u0000\u0000\u02cc\u02de\u0001\u0000\u0000\u0000\u02cd\u02ce\u0005\u001a"+
		"\u0000\u0000\u02ce\u02cf\u0003H$\u0000\u02cf\u02d0\u0005\u0019\u0000\u0000"+
		"\u02d0\u02de\u0001\u0000\u0000\u0000\u02d1\u02de\u0005>\u0000\u0000\u02d2"+
		"\u02de\u0005=\u0000\u0000\u02d3\u02d7\u0005A\u0000\u0000\u02d4\u02d6\u0003"+
		"r9\u0000\u02d5\u02d4\u0001\u0000\u0000\u0000\u02d6\u02d9\u0001\u0000\u0000"+
		"\u0000\u02d7\u02d5\u0001\u0000\u0000\u0000\u02d7\u02d8\u0001\u0000\u0000"+
		"\u0000\u02d8\u02da\u0001\u0000\u0000\u0000\u02d9\u02d7\u0001\u0000\u0000"+
		"\u0000\u02da\u02de\u0005A\u0000\u0000\u02db\u02de\u0005@\u0000\u0000\u02dc"+
		"\u02de\u0005H\u0000\u0000\u02dd\u02ae\u0001\u0000\u0000\u0000\u02dd\u02af"+
		"\u0001\u0000\u0000\u0000\u02dd\u02b0\u0001\u0000\u0000\u0000\u02dd\u02b1"+
		"\u0001\u0000\u0000\u0000\u02dd\u02b2\u0001\u0000\u0000\u0000\u02dd\u02b3"+
		"\u0001\u0000\u0000\u0000\u02dd\u02b4\u0001\u0000\u0000\u0000\u02dd\u02b5"+
		"\u0001\u0000\u0000\u0000\u02dd\u02b6\u0001\u0000\u0000\u0000\u02dd\u02b7"+
		"\u0001\u0000\u0000\u0000\u02dd\u02b8\u0001\u0000\u0000\u0000\u02dd\u02b9"+
		"\u0001\u0000\u0000\u0000\u02dd\u02ba\u0001\u0000\u0000\u0000\u02dd\u02bb"+
		"\u0001\u0000\u0000\u0000\u02dd\u02bc\u0001\u0000\u0000\u0000\u02dd\u02bd"+
		"\u0001\u0000\u0000\u0000\u02dd\u02be\u0001\u0000\u0000\u0000\u02dd\u02bf"+
		"\u0001\u0000\u0000\u0000\u02dd\u02c0\u0001\u0000\u0000\u0000\u02dd\u02c1"+
		"\u0001\u0000\u0000\u0000\u02dd\u02c2\u0001\u0000\u0000\u0000\u02dd\u02c3"+
		"\u0001\u0000\u0000\u0000\u02dd\u02c4\u0001\u0000\u0000\u0000\u02dd\u02c5"+
		"\u0001\u0000\u0000\u0000\u02dd\u02c9\u0001\u0000\u0000\u0000\u02dd\u02cd"+
		"\u0001\u0000\u0000\u0000\u02dd\u02d1\u0001\u0000\u0000\u0000\u02dd\u02d2"+
		"\u0001\u0000\u0000\u0000\u02dd\u02d3\u0001\u0000\u0000\u0000\u02dd\u02db"+
		"\u0001\u0000\u0000\u0000\u02dd\u02dc\u0001\u0000\u0000\u0000\u02de\u0081"+
		"\u0001\u0000\u0000\u0000\u02df\u02e0\u00050\u0000\u0000\u02e0\u02e4\u0003"+
		"\u0088D\u0000\u02e1\u02e2\u00051\u0000\u0000\u02e2\u02e4\u0003\u0088D"+
		"\u0000\u02e3\u02df\u0001\u0000\u0000\u0000\u02e3\u02e1\u0001\u0000\u0000"+
		"\u0000\u02e4\u0083\u0001\u0000\u0000\u0000\u02e5\u02e6\u00050\u0000\u0000"+
		"\u02e6\u02e9\u0003\u0088D\u0000\u02e7\u02e9\u0003\u0086C\u0000\u02e8\u02e5"+
		"\u0001\u0000\u0000\u0000\u02e8\u02e7\u0001\u0000\u0000\u0000\u02e9\u0085"+
		"\u0001\u0000\u0000\u0000\u02ea\u02f0\u0005\u0003\u0000\u0000\u02eb\u02ed"+
		"\u0005\u0004\u0000\u0000\u02ec\u02ee\u0005\u0003\u0000\u0000\u02ed\u02ec"+
		"\u0001\u0000\u0000\u0000\u02ed\u02ee\u0001\u0000\u0000\u0000\u02ee\u02f0"+
		"\u0001\u0000\u0000\u0000\u02ef\u02ea\u0001\u0000\u0000\u0000\u02ef\u02eb"+
		"\u0001\u0000\u0000\u0000\u02f0\u02f1\u0001\u0000\u0000\u0000\u02f1\u02ef"+
		"\u0001\u0000\u0000\u0000\u02f1\u02f2\u0001\u0000\u0000\u0000\u02f2\u0087"+
		"\u0001\u0000\u0000\u0000\u02f3\u02f5\u0003\u0086C\u0000\u02f4\u02f3\u0001"+
		"\u0000\u0000\u0000\u02f4\u02f5\u0001\u0000\u0000\u0000\u02f5\u0089\u0001"+
		"\u0000\u0000\u0000G\u008b\u008f\u009d\u00a2\u00aa\u00b5\u00ba\u00bd\u00cc"+
		"\u00d3\u00d7\u00db\u00de\u00e2\u00e4\u00e8\u00ea\u00f1\u00f3\u00ff\u0114"+
		"\u0118\u012c\u0133\u013b\u013f\u0143\u0147\u015f\u0165\u016b\u0171\u0177"+
		"\u0180\u018a\u01b1\u01ba\u01d3\u01d8\u01e2\u01e9\u01ee\u01f3\u01f6\u01fe"+
		"\u0203\u0206\u020c\u0211\u0214\u0221\u0224\u0229\u022e\u0238\u025b\u0270"+
		"\u0276\u0289\u0290\u0297\u02a6\u02ab\u02d7\u02dd\u02e3\u02e8\u02ed\u02ef"+
		"\u02f1\u02f4";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}