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
// Generated from /home/tim/Documents/workspace/openrewrite/rewrite/rewrite-docker/src/main/antlr/DockerParser.g4 by ANTLR 4.13.2
package org.openrewrite.docker.internal.grammar;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class DockerParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		PARSER_DIRECTIVE=1, COMMENT=2, FROM=3, RUN=4, CMD=5, NONE=6, LABEL=7, 
		EXPOSE=8, ENV=9, ADD=10, COPY=11, ENTRYPOINT=12, VOLUME=13, USER=14, WORKDIR=15, 
		ARG=16, ONBUILD=17, STOPSIGNAL=18, HEALTHCHECK=19, SHELL=20, MAINTAINER=21, 
		AS=22, HEREDOC_START=23, LINE_CONTINUATION=24, LBRACKET=25, RBRACKET=26, 
		COMMA=27, EQUALS=28, FLAG=29, DASH_DASH=30, DOUBLE_QUOTED_STRING=31, SINGLE_QUOTED_STRING=32, 
		ENV_VAR=33, SPECIAL_VAR=34, COMMAND_SUBST=35, BACKTICK_SUBST=36, UNQUOTED_TEXT=37, 
		WS=38, NEWLINE=39, HP_LINE_CONTINUATION=40, HP_WS=41, HP_COMMENT=42, HP_LINE_COMMENT=43, 
		HEREDOC_CONTENT=44, H_NEWLINE=45;
	public static final int
		RULE_dockerfile = 0, RULE_parserDirective = 1, RULE_globalArgs = 2, RULE_stage = 3, 
		RULE_stageInstruction = 4, RULE_instruction = 5, RULE_fromInstruction = 6, 
		RULE_runInstruction = 7, RULE_cmdInstruction = 8, RULE_labelInstruction = 9, 
		RULE_exposeInstruction = 10, RULE_envInstruction = 11, RULE_addInstruction = 12, 
		RULE_copyInstruction = 13, RULE_entrypointInstruction = 14, RULE_volumeInstruction = 15, 
		RULE_userInstruction = 16, RULE_workdirInstruction = 17, RULE_argInstruction = 18, 
		RULE_onbuildInstruction = 19, RULE_stopsignalInstruction = 20, RULE_healthcheckInstruction = 21, 
		RULE_healthcheckOptions = 22, RULE_healthcheckOption = 23, RULE_shellInstruction = 24, 
		RULE_maintainerInstruction = 25, RULE_flags = 26, RULE_flag = 27, RULE_execForm = 28, 
		RULE_shellForm = 29, RULE_shellFormText = 30, RULE_shellFormTextElement = 31, 
		RULE_heredoc = 32, RULE_heredocPreamble = 33, RULE_preambleElement = 34, 
		RULE_heredocBody = 35, RULE_heredocContent = 36, RULE_heredocEnd = 37, 
		RULE_jsonArray = 38, RULE_jsonArrayElements = 39, RULE_jsonString = 40, 
		RULE_imageName = 41, RULE_stageName = 42, RULE_labelPairs = 43, RULE_labelPair = 44, 
		RULE_labelKey = 45, RULE_labelValue = 46, RULE_labelOldValue = 47, RULE_labelOldValueElement = 48, 
		RULE_portList = 49, RULE_port = 50, RULE_envPairs = 51, RULE_envPair = 52, 
		RULE_envKey = 53, RULE_envValueEquals = 54, RULE_envValueSpace = 55, RULE_envTextEquals = 56, 
		RULE_envTextElementEquals = 57, RULE_sourceList = 58, RULE_sourcePath = 59, 
		RULE_destination = 60, RULE_destinationPath = 61, RULE_path = 62, RULE_pathList = 63, 
		RULE_volumePath = 64, RULE_userSpec = 65, RULE_argName = 66, RULE_argValue = 67, 
		RULE_signal = 68, RULE_text = 69, RULE_textElement = 70;
	private static String[] makeRuleNames() {
		return new String[] {
			"dockerfile", "parserDirective", "globalArgs", "stage", "stageInstruction", 
			"instruction", "fromInstruction", "runInstruction", "cmdInstruction", 
			"labelInstruction", "exposeInstruction", "envInstruction", "addInstruction", 
			"copyInstruction", "entrypointInstruction", "volumeInstruction", "userInstruction", 
			"workdirInstruction", "argInstruction", "onbuildInstruction", "stopsignalInstruction", 
			"healthcheckInstruction", "healthcheckOptions", "healthcheckOption", 
			"shellInstruction", "maintainerInstruction", "flags", "flag", "execForm", 
			"shellForm", "shellFormText", "shellFormTextElement", "heredoc", "heredocPreamble", 
			"preambleElement", "heredocBody", "heredocContent", "heredocEnd", "jsonArray", 
			"jsonArrayElements", "jsonString", "imageName", "stageName", "labelPairs", 
			"labelPair", "labelKey", "labelValue", "labelOldValue", "labelOldValueElement", 
			"portList", "port", "envPairs", "envPair", "envKey", "envValueEquals", 
			"envValueSpace", "envTextEquals", "envTextElementEquals", "sourceList", 
			"sourcePath", "destination", "destinationPath", "path", "pathList", "volumePath", 
			"userSpec", "argName", "argValue", "signal", "text", "textElement"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, "'FROM'", "'RUN'", "'CMD'", "'NONE'", "'LABEL'", "'EXPOSE'", 
			"'ENV'", "'ADD'", "'COPY'", "'ENTRYPOINT'", "'VOLUME'", "'USER'", "'WORKDIR'", 
			"'ARG'", "'ONBUILD'", "'STOPSIGNAL'", "'HEALTHCHECK'", "'SHELL'", "'MAINTAINER'", 
			"'AS'", null, null, "'['", "']'", "','", "'='", null, "'--'", null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			"'\\n'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "PARSER_DIRECTIVE", "COMMENT", "FROM", "RUN", "CMD", "NONE", "LABEL", 
			"EXPOSE", "ENV", "ADD", "COPY", "ENTRYPOINT", "VOLUME", "USER", "WORKDIR", 
			"ARG", "ONBUILD", "STOPSIGNAL", "HEALTHCHECK", "SHELL", "MAINTAINER", 
			"AS", "HEREDOC_START", "LINE_CONTINUATION", "LBRACKET", "RBRACKET", "COMMA", 
			"EQUALS", "FLAG", "DASH_DASH", "DOUBLE_QUOTED_STRING", "SINGLE_QUOTED_STRING", 
			"ENV_VAR", "SPECIAL_VAR", "COMMAND_SUBST", "BACKTICK_SUBST", "UNQUOTED_TEXT", 
			"WS", "NEWLINE", "HP_LINE_CONTINUATION", "HP_WS", "HP_COMMENT", "HP_LINE_COMMENT", 
			"HEREDOC_CONTENT", "H_NEWLINE"
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
	public String getGrammarFileName() { return "DockerParser.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public DockerParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DockerfileContext extends ParserRuleContext {
		public GlobalArgsContext globalArgs() {
			return getRuleContext(GlobalArgsContext.class,0);
		}
		public TerminalNode EOF() { return getToken(DockerParser.EOF, 0); }
		public List<ParserDirectiveContext> parserDirective() {
			return getRuleContexts(ParserDirectiveContext.class);
		}
		public ParserDirectiveContext parserDirective(int i) {
			return getRuleContext(ParserDirectiveContext.class,i);
		}
		public List<StageContext> stage() {
			return getRuleContexts(StageContext.class);
		}
		public StageContext stage(int i) {
			return getRuleContext(StageContext.class,i);
		}
		public DockerfileContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dockerfile; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterDockerfile(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitDockerfile(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitDockerfile(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DockerfileContext dockerfile() throws RecognitionException {
		DockerfileContext _localctx = new DockerfileContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_dockerfile);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(145);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==PARSER_DIRECTIVE) {
				{
				{
				setState(142);
				parserDirective();
				}
				}
				setState(147);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(148);
			globalArgs();
			setState(150); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(149);
				stage();
				}
				}
				setState(152); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==FROM );
			setState(154);
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
	public static class ParserDirectiveContext extends ParserRuleContext {
		public TerminalNode PARSER_DIRECTIVE() { return getToken(DockerParser.PARSER_DIRECTIVE, 0); }
		public ParserDirectiveContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_parserDirective; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterParserDirective(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitParserDirective(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitParserDirective(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ParserDirectiveContext parserDirective() throws RecognitionException {
		ParserDirectiveContext _localctx = new ParserDirectiveContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_parserDirective);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(156);
			match(PARSER_DIRECTIVE);
			}
		}
		catch (RecognitionException re) {
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
	public static class GlobalArgsContext extends ParserRuleContext {
		public List<ArgInstructionContext> argInstruction() {
			return getRuleContexts(ArgInstructionContext.class);
		}
		public ArgInstructionContext argInstruction(int i) {
			return getRuleContext(ArgInstructionContext.class,i);
		}
		public GlobalArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_globalArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterGlobalArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitGlobalArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitGlobalArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GlobalArgsContext globalArgs() throws RecognitionException {
		GlobalArgsContext _localctx = new GlobalArgsContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_globalArgs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(161);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ARG) {
				{
				{
				setState(158);
				argInstruction();
				}
				}
				setState(163);
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
	public static class StageContext extends ParserRuleContext {
		public FromInstructionContext fromInstruction() {
			return getRuleContext(FromInstructionContext.class,0);
		}
		public List<StageInstructionContext> stageInstruction() {
			return getRuleContexts(StageInstructionContext.class);
		}
		public StageInstructionContext stageInstruction(int i) {
			return getRuleContext(StageInstructionContext.class,i);
		}
		public StageContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stage; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterStage(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitStage(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitStage(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StageContext stage() throws RecognitionException {
		StageContext _localctx = new StageContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_stage);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(164);
			fromInstruction();
			setState(168);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 4194224L) != 0)) {
				{
				{
				setState(165);
				stageInstruction();
				}
				}
				setState(170);
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
	public static class StageInstructionContext extends ParserRuleContext {
		public RunInstructionContext runInstruction() {
			return getRuleContext(RunInstructionContext.class,0);
		}
		public CmdInstructionContext cmdInstruction() {
			return getRuleContext(CmdInstructionContext.class,0);
		}
		public LabelInstructionContext labelInstruction() {
			return getRuleContext(LabelInstructionContext.class,0);
		}
		public ExposeInstructionContext exposeInstruction() {
			return getRuleContext(ExposeInstructionContext.class,0);
		}
		public EnvInstructionContext envInstruction() {
			return getRuleContext(EnvInstructionContext.class,0);
		}
		public AddInstructionContext addInstruction() {
			return getRuleContext(AddInstructionContext.class,0);
		}
		public CopyInstructionContext copyInstruction() {
			return getRuleContext(CopyInstructionContext.class,0);
		}
		public EntrypointInstructionContext entrypointInstruction() {
			return getRuleContext(EntrypointInstructionContext.class,0);
		}
		public VolumeInstructionContext volumeInstruction() {
			return getRuleContext(VolumeInstructionContext.class,0);
		}
		public UserInstructionContext userInstruction() {
			return getRuleContext(UserInstructionContext.class,0);
		}
		public WorkdirInstructionContext workdirInstruction() {
			return getRuleContext(WorkdirInstructionContext.class,0);
		}
		public ArgInstructionContext argInstruction() {
			return getRuleContext(ArgInstructionContext.class,0);
		}
		public OnbuildInstructionContext onbuildInstruction() {
			return getRuleContext(OnbuildInstructionContext.class,0);
		}
		public StopsignalInstructionContext stopsignalInstruction() {
			return getRuleContext(StopsignalInstructionContext.class,0);
		}
		public HealthcheckInstructionContext healthcheckInstruction() {
			return getRuleContext(HealthcheckInstructionContext.class,0);
		}
		public ShellInstructionContext shellInstruction() {
			return getRuleContext(ShellInstructionContext.class,0);
		}
		public MaintainerInstructionContext maintainerInstruction() {
			return getRuleContext(MaintainerInstructionContext.class,0);
		}
		public StageInstructionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stageInstruction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterStageInstruction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitStageInstruction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitStageInstruction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StageInstructionContext stageInstruction() throws RecognitionException {
		StageInstructionContext _localctx = new StageInstructionContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_stageInstruction);
		try {
			setState(188);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case RUN:
				enterOuterAlt(_localctx, 1);
				{
				setState(171);
				runInstruction();
				}
				break;
			case CMD:
				enterOuterAlt(_localctx, 2);
				{
				setState(172);
				cmdInstruction();
				}
				break;
			case LABEL:
				enterOuterAlt(_localctx, 3);
				{
				setState(173);
				labelInstruction();
				}
				break;
			case EXPOSE:
				enterOuterAlt(_localctx, 4);
				{
				setState(174);
				exposeInstruction();
				}
				break;
			case ENV:
				enterOuterAlt(_localctx, 5);
				{
				setState(175);
				envInstruction();
				}
				break;
			case ADD:
				enterOuterAlt(_localctx, 6);
				{
				setState(176);
				addInstruction();
				}
				break;
			case COPY:
				enterOuterAlt(_localctx, 7);
				{
				setState(177);
				copyInstruction();
				}
				break;
			case ENTRYPOINT:
				enterOuterAlt(_localctx, 8);
				{
				setState(178);
				entrypointInstruction();
				}
				break;
			case VOLUME:
				enterOuterAlt(_localctx, 9);
				{
				setState(179);
				volumeInstruction();
				}
				break;
			case USER:
				enterOuterAlt(_localctx, 10);
				{
				setState(180);
				userInstruction();
				}
				break;
			case WORKDIR:
				enterOuterAlt(_localctx, 11);
				{
				setState(181);
				workdirInstruction();
				}
				break;
			case ARG:
				enterOuterAlt(_localctx, 12);
				{
				setState(182);
				argInstruction();
				}
				break;
			case ONBUILD:
				enterOuterAlt(_localctx, 13);
				{
				setState(183);
				onbuildInstruction();
				}
				break;
			case STOPSIGNAL:
				enterOuterAlt(_localctx, 14);
				{
				setState(184);
				stopsignalInstruction();
				}
				break;
			case HEALTHCHECK:
				enterOuterAlt(_localctx, 15);
				{
				setState(185);
				healthcheckInstruction();
				}
				break;
			case SHELL:
				enterOuterAlt(_localctx, 16);
				{
				setState(186);
				shellInstruction();
				}
				break;
			case MAINTAINER:
				enterOuterAlt(_localctx, 17);
				{
				setState(187);
				maintainerInstruction();
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
	public static class InstructionContext extends ParserRuleContext {
		public FromInstructionContext fromInstruction() {
			return getRuleContext(FromInstructionContext.class,0);
		}
		public StageInstructionContext stageInstruction() {
			return getRuleContext(StageInstructionContext.class,0);
		}
		public InstructionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_instruction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterInstruction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitInstruction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitInstruction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InstructionContext instruction() throws RecognitionException {
		InstructionContext _localctx = new InstructionContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_instruction);
		try {
			setState(192);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case FROM:
				enterOuterAlt(_localctx, 1);
				{
				setState(190);
				fromInstruction();
				}
				break;
			case RUN:
			case CMD:
			case LABEL:
			case EXPOSE:
			case ENV:
			case ADD:
			case COPY:
			case ENTRYPOINT:
			case VOLUME:
			case USER:
			case WORKDIR:
			case ARG:
			case ONBUILD:
			case STOPSIGNAL:
			case HEALTHCHECK:
			case SHELL:
			case MAINTAINER:
				enterOuterAlt(_localctx, 2);
				{
				setState(191);
				stageInstruction();
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
	public static class FromInstructionContext extends ParserRuleContext {
		public TerminalNode FROM() { return getToken(DockerParser.FROM, 0); }
		public ImageNameContext imageName() {
			return getRuleContext(ImageNameContext.class,0);
		}
		public FlagsContext flags() {
			return getRuleContext(FlagsContext.class,0);
		}
		public TerminalNode AS() { return getToken(DockerParser.AS, 0); }
		public StageNameContext stageName() {
			return getRuleContext(StageNameContext.class,0);
		}
		public FromInstructionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fromInstruction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterFromInstruction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitFromInstruction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitFromInstruction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FromInstructionContext fromInstruction() throws RecognitionException {
		FromInstructionContext _localctx = new FromInstructionContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_fromInstruction);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(194);
			match(FROM);
			setState(196);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,6,_ctx) ) {
			case 1:
				{
				setState(195);
				flags();
				}
				break;
			}
			setState(198);
			imageName();
			setState(201);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==AS) {
				{
				setState(199);
				match(AS);
				setState(200);
				stageName();
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
	public static class RunInstructionContext extends ParserRuleContext {
		public TerminalNode RUN() { return getToken(DockerParser.RUN, 0); }
		public ExecFormContext execForm() {
			return getRuleContext(ExecFormContext.class,0);
		}
		public ShellFormContext shellForm() {
			return getRuleContext(ShellFormContext.class,0);
		}
		public HeredocContext heredoc() {
			return getRuleContext(HeredocContext.class,0);
		}
		public FlagsContext flags() {
			return getRuleContext(FlagsContext.class,0);
		}
		public RunInstructionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_runInstruction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterRunInstruction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitRunInstruction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitRunInstruction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RunInstructionContext runInstruction() throws RecognitionException {
		RunInstructionContext _localctx = new RunInstructionContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_runInstruction);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(203);
			match(RUN);
			setState(205);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,8,_ctx) ) {
			case 1:
				{
				setState(204);
				flags();
				}
				break;
			}
			setState(210);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,9,_ctx) ) {
			case 1:
				{
				setState(207);
				execForm();
				}
				break;
			case 2:
				{
				setState(208);
				shellForm();
				}
				break;
			case 3:
				{
				setState(209);
				heredoc();
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
	public static class CmdInstructionContext extends ParserRuleContext {
		public TerminalNode CMD() { return getToken(DockerParser.CMD, 0); }
		public ExecFormContext execForm() {
			return getRuleContext(ExecFormContext.class,0);
		}
		public ShellFormContext shellForm() {
			return getRuleContext(ShellFormContext.class,0);
		}
		public CmdInstructionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_cmdInstruction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterCmdInstruction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitCmdInstruction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitCmdInstruction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CmdInstructionContext cmdInstruction() throws RecognitionException {
		CmdInstructionContext _localctx = new CmdInstructionContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_cmdInstruction);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(212);
			match(CMD);
			setState(215);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,10,_ctx) ) {
			case 1:
				{
				setState(213);
				execForm();
				}
				break;
			case 2:
				{
				setState(214);
				shellForm();
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
	public static class LabelInstructionContext extends ParserRuleContext {
		public TerminalNode LABEL() { return getToken(DockerParser.LABEL, 0); }
		public LabelPairsContext labelPairs() {
			return getRuleContext(LabelPairsContext.class,0);
		}
		public LabelInstructionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_labelInstruction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterLabelInstruction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitLabelInstruction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitLabelInstruction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LabelInstructionContext labelInstruction() throws RecognitionException {
		LabelInstructionContext _localctx = new LabelInstructionContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_labelInstruction);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(217);
			match(LABEL);
			setState(218);
			labelPairs();
			}
		}
		catch (RecognitionException re) {
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
	public static class ExposeInstructionContext extends ParserRuleContext {
		public TerminalNode EXPOSE() { return getToken(DockerParser.EXPOSE, 0); }
		public PortListContext portList() {
			return getRuleContext(PortListContext.class,0);
		}
		public ExposeInstructionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_exposeInstruction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterExposeInstruction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitExposeInstruction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitExposeInstruction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExposeInstructionContext exposeInstruction() throws RecognitionException {
		ExposeInstructionContext _localctx = new ExposeInstructionContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_exposeInstruction);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(220);
			match(EXPOSE);
			setState(221);
			portList();
			}
		}
		catch (RecognitionException re) {
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
	public static class EnvInstructionContext extends ParserRuleContext {
		public TerminalNode ENV() { return getToken(DockerParser.ENV, 0); }
		public EnvPairsContext envPairs() {
			return getRuleContext(EnvPairsContext.class,0);
		}
		public EnvInstructionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_envInstruction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterEnvInstruction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitEnvInstruction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitEnvInstruction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EnvInstructionContext envInstruction() throws RecognitionException {
		EnvInstructionContext _localctx = new EnvInstructionContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_envInstruction);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(223);
			match(ENV);
			setState(224);
			envPairs();
			}
		}
		catch (RecognitionException re) {
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
	public static class AddInstructionContext extends ParserRuleContext {
		public TerminalNode ADD() { return getToken(DockerParser.ADD, 0); }
		public HeredocContext heredoc() {
			return getRuleContext(HeredocContext.class,0);
		}
		public JsonArrayContext jsonArray() {
			return getRuleContext(JsonArrayContext.class,0);
		}
		public SourceListContext sourceList() {
			return getRuleContext(SourceListContext.class,0);
		}
		public DestinationContext destination() {
			return getRuleContext(DestinationContext.class,0);
		}
		public FlagsContext flags() {
			return getRuleContext(FlagsContext.class,0);
		}
		public AddInstructionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_addInstruction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterAddInstruction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitAddInstruction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitAddInstruction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AddInstructionContext addInstruction() throws RecognitionException {
		AddInstructionContext _localctx = new AddInstructionContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_addInstruction);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(226);
			match(ADD);
			setState(228);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==FLAG) {
				{
				setState(227);
				flags();
				}
			}

			setState(235);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case HEREDOC_START:
				{
				setState(230);
				heredoc();
				}
				break;
			case LBRACKET:
				{
				setState(231);
				jsonArray();
				}
				break;
			case DOUBLE_QUOTED_STRING:
			case SINGLE_QUOTED_STRING:
			case ENV_VAR:
			case SPECIAL_VAR:
			case COMMAND_SUBST:
			case BACKTICK_SUBST:
			case UNQUOTED_TEXT:
				{
				setState(232);
				sourceList();
				setState(233);
				destination();
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
	public static class CopyInstructionContext extends ParserRuleContext {
		public TerminalNode COPY() { return getToken(DockerParser.COPY, 0); }
		public HeredocContext heredoc() {
			return getRuleContext(HeredocContext.class,0);
		}
		public JsonArrayContext jsonArray() {
			return getRuleContext(JsonArrayContext.class,0);
		}
		public SourceListContext sourceList() {
			return getRuleContext(SourceListContext.class,0);
		}
		public DestinationContext destination() {
			return getRuleContext(DestinationContext.class,0);
		}
		public FlagsContext flags() {
			return getRuleContext(FlagsContext.class,0);
		}
		public CopyInstructionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_copyInstruction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterCopyInstruction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitCopyInstruction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitCopyInstruction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CopyInstructionContext copyInstruction() throws RecognitionException {
		CopyInstructionContext _localctx = new CopyInstructionContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_copyInstruction);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(237);
			match(COPY);
			setState(239);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==FLAG) {
				{
				setState(238);
				flags();
				}
			}

			setState(246);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case HEREDOC_START:
				{
				setState(241);
				heredoc();
				}
				break;
			case LBRACKET:
				{
				setState(242);
				jsonArray();
				}
				break;
			case DOUBLE_QUOTED_STRING:
			case SINGLE_QUOTED_STRING:
			case ENV_VAR:
			case SPECIAL_VAR:
			case COMMAND_SUBST:
			case BACKTICK_SUBST:
			case UNQUOTED_TEXT:
				{
				setState(243);
				sourceList();
				setState(244);
				destination();
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
	public static class EntrypointInstructionContext extends ParserRuleContext {
		public TerminalNode ENTRYPOINT() { return getToken(DockerParser.ENTRYPOINT, 0); }
		public ExecFormContext execForm() {
			return getRuleContext(ExecFormContext.class,0);
		}
		public ShellFormContext shellForm() {
			return getRuleContext(ShellFormContext.class,0);
		}
		public EntrypointInstructionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_entrypointInstruction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterEntrypointInstruction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitEntrypointInstruction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitEntrypointInstruction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EntrypointInstructionContext entrypointInstruction() throws RecognitionException {
		EntrypointInstructionContext _localctx = new EntrypointInstructionContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_entrypointInstruction);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(248);
			match(ENTRYPOINT);
			setState(251);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,15,_ctx) ) {
			case 1:
				{
				setState(249);
				execForm();
				}
				break;
			case 2:
				{
				setState(250);
				shellForm();
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
	public static class VolumeInstructionContext extends ParserRuleContext {
		public TerminalNode VOLUME() { return getToken(DockerParser.VOLUME, 0); }
		public JsonArrayContext jsonArray() {
			return getRuleContext(JsonArrayContext.class,0);
		}
		public PathListContext pathList() {
			return getRuleContext(PathListContext.class,0);
		}
		public VolumeInstructionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_volumeInstruction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterVolumeInstruction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitVolumeInstruction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitVolumeInstruction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VolumeInstructionContext volumeInstruction() throws RecognitionException {
		VolumeInstructionContext _localctx = new VolumeInstructionContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_volumeInstruction);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(253);
			match(VOLUME);
			setState(256);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LBRACKET:
				{
				setState(254);
				jsonArray();
				}
				break;
			case DOUBLE_QUOTED_STRING:
			case SINGLE_QUOTED_STRING:
			case ENV_VAR:
			case SPECIAL_VAR:
			case COMMAND_SUBST:
			case BACKTICK_SUBST:
			case UNQUOTED_TEXT:
				{
				setState(255);
				pathList();
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
	public static class UserInstructionContext extends ParserRuleContext {
		public TerminalNode USER() { return getToken(DockerParser.USER, 0); }
		public UserSpecContext userSpec() {
			return getRuleContext(UserSpecContext.class,0);
		}
		public UserInstructionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_userInstruction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterUserInstruction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitUserInstruction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitUserInstruction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UserInstructionContext userInstruction() throws RecognitionException {
		UserInstructionContext _localctx = new UserInstructionContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_userInstruction);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(258);
			match(USER);
			setState(259);
			userSpec();
			}
		}
		catch (RecognitionException re) {
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
	public static class WorkdirInstructionContext extends ParserRuleContext {
		public TerminalNode WORKDIR() { return getToken(DockerParser.WORKDIR, 0); }
		public PathContext path() {
			return getRuleContext(PathContext.class,0);
		}
		public WorkdirInstructionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_workdirInstruction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterWorkdirInstruction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitWorkdirInstruction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitWorkdirInstruction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WorkdirInstructionContext workdirInstruction() throws RecognitionException {
		WorkdirInstructionContext _localctx = new WorkdirInstructionContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_workdirInstruction);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(261);
			match(WORKDIR);
			setState(262);
			path();
			}
		}
		catch (RecognitionException re) {
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
	public static class ArgInstructionContext extends ParserRuleContext {
		public TerminalNode ARG() { return getToken(DockerParser.ARG, 0); }
		public ArgNameContext argName() {
			return getRuleContext(ArgNameContext.class,0);
		}
		public TerminalNode EQUALS() { return getToken(DockerParser.EQUALS, 0); }
		public ArgValueContext argValue() {
			return getRuleContext(ArgValueContext.class,0);
		}
		public ArgInstructionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_argInstruction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterArgInstruction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitArgInstruction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitArgInstruction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArgInstructionContext argInstruction() throws RecognitionException {
		ArgInstructionContext _localctx = new ArgInstructionContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_argInstruction);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(264);
			match(ARG);
			setState(265);
			argName();
			setState(268);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==EQUALS) {
				{
				setState(266);
				match(EQUALS);
				setState(267);
				argValue();
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
	public static class OnbuildInstructionContext extends ParserRuleContext {
		public TerminalNode ONBUILD() { return getToken(DockerParser.ONBUILD, 0); }
		public InstructionContext instruction() {
			return getRuleContext(InstructionContext.class,0);
		}
		public OnbuildInstructionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_onbuildInstruction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterOnbuildInstruction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitOnbuildInstruction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitOnbuildInstruction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OnbuildInstructionContext onbuildInstruction() throws RecognitionException {
		OnbuildInstructionContext _localctx = new OnbuildInstructionContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_onbuildInstruction);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(270);
			match(ONBUILD);
			setState(271);
			instruction();
			}
		}
		catch (RecognitionException re) {
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
	public static class StopsignalInstructionContext extends ParserRuleContext {
		public TerminalNode STOPSIGNAL() { return getToken(DockerParser.STOPSIGNAL, 0); }
		public SignalContext signal() {
			return getRuleContext(SignalContext.class,0);
		}
		public StopsignalInstructionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stopsignalInstruction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterStopsignalInstruction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitStopsignalInstruction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitStopsignalInstruction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StopsignalInstructionContext stopsignalInstruction() throws RecognitionException {
		StopsignalInstructionContext _localctx = new StopsignalInstructionContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_stopsignalInstruction);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(273);
			match(STOPSIGNAL);
			setState(274);
			signal();
			}
		}
		catch (RecognitionException re) {
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
	public static class HealthcheckInstructionContext extends ParserRuleContext {
		public TerminalNode HEALTHCHECK() { return getToken(DockerParser.HEALTHCHECK, 0); }
		public TerminalNode NONE() { return getToken(DockerParser.NONE, 0); }
		public TerminalNode CMD() { return getToken(DockerParser.CMD, 0); }
		public ExecFormContext execForm() {
			return getRuleContext(ExecFormContext.class,0);
		}
		public ShellFormContext shellForm() {
			return getRuleContext(ShellFormContext.class,0);
		}
		public HealthcheckOptionsContext healthcheckOptions() {
			return getRuleContext(HealthcheckOptionsContext.class,0);
		}
		public HealthcheckInstructionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_healthcheckInstruction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterHealthcheckInstruction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitHealthcheckInstruction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitHealthcheckInstruction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HealthcheckInstructionContext healthcheckInstruction() throws RecognitionException {
		HealthcheckInstructionContext _localctx = new HealthcheckInstructionContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_healthcheckInstruction);
		int _la;
		try {
			setState(287);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,20,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(276);
				match(HEALTHCHECK);
				setState(277);
				match(NONE);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(278);
				match(HEALTHCHECK);
				setState(280);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==FLAG) {
					{
					setState(279);
					healthcheckOptions();
					}
				}

				setState(282);
				match(CMD);
				setState(285);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,19,_ctx) ) {
				case 1:
					{
					setState(283);
					execForm();
					}
					break;
				case 2:
					{
					setState(284);
					shellForm();
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
	public static class HealthcheckOptionsContext extends ParserRuleContext {
		public List<HealthcheckOptionContext> healthcheckOption() {
			return getRuleContexts(HealthcheckOptionContext.class);
		}
		public HealthcheckOptionContext healthcheckOption(int i) {
			return getRuleContext(HealthcheckOptionContext.class,i);
		}
		public HealthcheckOptionsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_healthcheckOptions; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterHealthcheckOptions(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitHealthcheckOptions(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitHealthcheckOptions(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HealthcheckOptionsContext healthcheckOptions() throws RecognitionException {
		HealthcheckOptionsContext _localctx = new HealthcheckOptionsContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_healthcheckOptions);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(290); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(289);
				healthcheckOption();
				}
				}
				setState(292); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==FLAG );
			}
		}
		catch (RecognitionException re) {
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
	public static class HealthcheckOptionContext extends ParserRuleContext {
		public TerminalNode FLAG() { return getToken(DockerParser.FLAG, 0); }
		public HealthcheckOptionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_healthcheckOption; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterHealthcheckOption(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitHealthcheckOption(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitHealthcheckOption(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HealthcheckOptionContext healthcheckOption() throws RecognitionException {
		HealthcheckOptionContext _localctx = new HealthcheckOptionContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_healthcheckOption);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(294);
			match(FLAG);
			}
		}
		catch (RecognitionException re) {
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
	public static class ShellInstructionContext extends ParserRuleContext {
		public TerminalNode SHELL() { return getToken(DockerParser.SHELL, 0); }
		public JsonArrayContext jsonArray() {
			return getRuleContext(JsonArrayContext.class,0);
		}
		public ShellInstructionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_shellInstruction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterShellInstruction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitShellInstruction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitShellInstruction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ShellInstructionContext shellInstruction() throws RecognitionException {
		ShellInstructionContext _localctx = new ShellInstructionContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_shellInstruction);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(296);
			match(SHELL);
			setState(297);
			jsonArray();
			}
		}
		catch (RecognitionException re) {
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
	public static class MaintainerInstructionContext extends ParserRuleContext {
		public TerminalNode MAINTAINER() { return getToken(DockerParser.MAINTAINER, 0); }
		public TextContext text() {
			return getRuleContext(TextContext.class,0);
		}
		public MaintainerInstructionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_maintainerInstruction; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterMaintainerInstruction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitMaintainerInstruction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitMaintainerInstruction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MaintainerInstructionContext maintainerInstruction() throws RecognitionException {
		MaintainerInstructionContext _localctx = new MaintainerInstructionContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_maintainerInstruction);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(299);
			match(MAINTAINER);
			setState(300);
			text();
			}
		}
		catch (RecognitionException re) {
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
	public static class FlagsContext extends ParserRuleContext {
		public List<FlagContext> flag() {
			return getRuleContexts(FlagContext.class);
		}
		public FlagContext flag(int i) {
			return getRuleContext(FlagContext.class,i);
		}
		public FlagsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_flags; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterFlags(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitFlags(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitFlags(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FlagsContext flags() throws RecognitionException {
		FlagsContext _localctx = new FlagsContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_flags);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(303); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(302);
					flag();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(305); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,22,_ctx);
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
	public static class FlagContext extends ParserRuleContext {
		public TerminalNode FLAG() { return getToken(DockerParser.FLAG, 0); }
		public FlagContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_flag; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterFlag(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitFlag(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitFlag(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FlagContext flag() throws RecognitionException {
		FlagContext _localctx = new FlagContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_flag);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(307);
			match(FLAG);
			}
		}
		catch (RecognitionException re) {
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
	public static class ExecFormContext extends ParserRuleContext {
		public JsonArrayContext jsonArray() {
			return getRuleContext(JsonArrayContext.class,0);
		}
		public ExecFormContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_execForm; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterExecForm(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitExecForm(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitExecForm(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExecFormContext execForm() throws RecognitionException {
		ExecFormContext _localctx = new ExecFormContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_execForm);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(309);
			jsonArray();
			}
		}
		catch (RecognitionException re) {
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
	public static class ShellFormContext extends ParserRuleContext {
		public ShellFormTextContext shellFormText() {
			return getRuleContext(ShellFormTextContext.class,0);
		}
		public ShellFormContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_shellForm; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterShellForm(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitShellForm(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitShellForm(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ShellFormContext shellForm() throws RecognitionException {
		ShellFormContext _localctx = new ShellFormContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_shellForm);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(311);
			shellFormText();
			}
		}
		catch (RecognitionException re) {
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
	public static class ShellFormTextContext extends ParserRuleContext {
		public List<ShellFormTextElementContext> shellFormTextElement() {
			return getRuleContexts(ShellFormTextElementContext.class);
		}
		public ShellFormTextElementContext shellFormTextElement(int i) {
			return getRuleContext(ShellFormTextElementContext.class,i);
		}
		public ShellFormTextContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_shellFormText; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterShellFormText(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitShellFormText(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitShellFormText(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ShellFormTextContext shellFormText() throws RecognitionException {
		ShellFormTextContext _localctx = new ShellFormTextContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_shellFormText);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(314); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(313);
				shellFormTextElement();
				}
				}
				setState(316); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & 274844352512L) != 0) );
			}
		}
		catch (RecognitionException re) {
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
	public static class ShellFormTextElementContext extends ParserRuleContext {
		public TerminalNode UNQUOTED_TEXT() { return getToken(DockerParser.UNQUOTED_TEXT, 0); }
		public TerminalNode DOUBLE_QUOTED_STRING() { return getToken(DockerParser.DOUBLE_QUOTED_STRING, 0); }
		public TerminalNode SINGLE_QUOTED_STRING() { return getToken(DockerParser.SINGLE_QUOTED_STRING, 0); }
		public TerminalNode ENV_VAR() { return getToken(DockerParser.ENV_VAR, 0); }
		public TerminalNode COMMAND_SUBST() { return getToken(DockerParser.COMMAND_SUBST, 0); }
		public TerminalNode BACKTICK_SUBST() { return getToken(DockerParser.BACKTICK_SUBST, 0); }
		public TerminalNode SPECIAL_VAR() { return getToken(DockerParser.SPECIAL_VAR, 0); }
		public TerminalNode EQUALS() { return getToken(DockerParser.EQUALS, 0); }
		public TerminalNode FLAG() { return getToken(DockerParser.FLAG, 0); }
		public TerminalNode DASH_DASH() { return getToken(DockerParser.DASH_DASH, 0); }
		public TerminalNode LBRACKET() { return getToken(DockerParser.LBRACKET, 0); }
		public TerminalNode RBRACKET() { return getToken(DockerParser.RBRACKET, 0); }
		public TerminalNode COMMA() { return getToken(DockerParser.COMMA, 0); }
		public ShellFormTextElementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_shellFormTextElement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterShellFormTextElement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitShellFormTextElement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitShellFormTextElement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ShellFormTextElementContext shellFormTextElement() throws RecognitionException {
		ShellFormTextElementContext _localctx = new ShellFormTextElementContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_shellFormTextElement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(318);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 274844352512L) != 0)) ) {
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
		public HeredocPreambleContext heredocPreamble() {
			return getRuleContext(HeredocPreambleContext.class,0);
		}
		public TerminalNode NEWLINE() { return getToken(DockerParser.NEWLINE, 0); }
		public List<HeredocBodyContext> heredocBody() {
			return getRuleContexts(HeredocBodyContext.class);
		}
		public HeredocBodyContext heredocBody(int i) {
			return getRuleContext(HeredocBodyContext.class,i);
		}
		public HeredocContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_heredoc; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterHeredoc(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitHeredoc(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitHeredoc(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HeredocContext heredoc() throws RecognitionException {
		HeredocContext _localctx = new HeredocContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_heredoc);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(320);
			heredocPreamble();
			setState(321);
			match(NEWLINE);
			setState(323); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(322);
				heredocBody();
				}
				}
				setState(325); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & 18279380811776L) != 0) );
			}
		}
		catch (RecognitionException re) {
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
	public static class HeredocPreambleContext extends ParserRuleContext {
		public List<TerminalNode> HEREDOC_START() { return getTokens(DockerParser.HEREDOC_START); }
		public TerminalNode HEREDOC_START(int i) {
			return getToken(DockerParser.HEREDOC_START, i);
		}
		public List<PreambleElementContext> preambleElement() {
			return getRuleContexts(PreambleElementContext.class);
		}
		public PreambleElementContext preambleElement(int i) {
			return getRuleContext(PreambleElementContext.class,i);
		}
		public HeredocPreambleContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_heredocPreamble; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterHeredocPreamble(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitHeredocPreamble(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitHeredocPreamble(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HeredocPreambleContext heredocPreamble() throws RecognitionException {
		HeredocPreambleContext _localctx = new HeredocPreambleContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_heredocPreamble);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(327);
			match(HEREDOC_START);
			setState(331);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 274844352512L) != 0)) {
				{
				{
				setState(328);
				preambleElement();
				}
				}
				setState(333);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(343);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==HEREDOC_START) {
				{
				{
				setState(334);
				match(HEREDOC_START);
				setState(338);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 274844352512L) != 0)) {
					{
					{
					setState(335);
					preambleElement();
					}
					}
					setState(340);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				}
				setState(345);
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
	public static class PreambleElementContext extends ParserRuleContext {
		public TerminalNode UNQUOTED_TEXT() { return getToken(DockerParser.UNQUOTED_TEXT, 0); }
		public TerminalNode DOUBLE_QUOTED_STRING() { return getToken(DockerParser.DOUBLE_QUOTED_STRING, 0); }
		public TerminalNode SINGLE_QUOTED_STRING() { return getToken(DockerParser.SINGLE_QUOTED_STRING, 0); }
		public TerminalNode ENV_VAR() { return getToken(DockerParser.ENV_VAR, 0); }
		public TerminalNode COMMAND_SUBST() { return getToken(DockerParser.COMMAND_SUBST, 0); }
		public TerminalNode BACKTICK_SUBST() { return getToken(DockerParser.BACKTICK_SUBST, 0); }
		public TerminalNode SPECIAL_VAR() { return getToken(DockerParser.SPECIAL_VAR, 0); }
		public TerminalNode EQUALS() { return getToken(DockerParser.EQUALS, 0); }
		public TerminalNode FLAG() { return getToken(DockerParser.FLAG, 0); }
		public TerminalNode DASH_DASH() { return getToken(DockerParser.DASH_DASH, 0); }
		public TerminalNode LBRACKET() { return getToken(DockerParser.LBRACKET, 0); }
		public TerminalNode RBRACKET() { return getToken(DockerParser.RBRACKET, 0); }
		public TerminalNode COMMA() { return getToken(DockerParser.COMMA, 0); }
		public PreambleElementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_preambleElement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterPreambleElement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitPreambleElement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitPreambleElement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PreambleElementContext preambleElement() throws RecognitionException {
		PreambleElementContext _localctx = new PreambleElementContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_preambleElement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(346);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 274844352512L) != 0)) ) {
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
	public static class HeredocBodyContext extends ParserRuleContext {
		public HeredocContentContext heredocContent() {
			return getRuleContext(HeredocContentContext.class,0);
		}
		public HeredocEndContext heredocEnd() {
			return getRuleContext(HeredocEndContext.class,0);
		}
		public HeredocBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_heredocBody; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterHeredocBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitHeredocBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitHeredocBody(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HeredocBodyContext heredocBody() throws RecognitionException {
		HeredocBodyContext _localctx = new HeredocBodyContext(_ctx, getState());
		enterRule(_localctx, 70, RULE_heredocBody);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(348);
			heredocContent();
			setState(349);
			heredocEnd();
			}
		}
		catch (RecognitionException re) {
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
	public static class HeredocContentContext extends ParserRuleContext {
		public List<TerminalNode> NEWLINE() { return getTokens(DockerParser.NEWLINE); }
		public TerminalNode NEWLINE(int i) {
			return getToken(DockerParser.NEWLINE, i);
		}
		public List<TerminalNode> HEREDOC_CONTENT() { return getTokens(DockerParser.HEREDOC_CONTENT); }
		public TerminalNode HEREDOC_CONTENT(int i) {
			return getToken(DockerParser.HEREDOC_CONTENT, i);
		}
		public HeredocContentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_heredocContent; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterHeredocContent(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitHeredocContent(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitHeredocContent(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HeredocContentContext heredocContent() throws RecognitionException {
		HeredocContentContext _localctx = new HeredocContentContext(_ctx, getState());
		enterRule(_localctx, 72, RULE_heredocContent);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(354);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==NEWLINE || _la==HEREDOC_CONTENT) {
				{
				{
				setState(351);
				_la = _input.LA(1);
				if ( !(_la==NEWLINE || _la==HEREDOC_CONTENT) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				}
				setState(356);
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
	public static class HeredocEndContext extends ParserRuleContext {
		public TerminalNode UNQUOTED_TEXT() { return getToken(DockerParser.UNQUOTED_TEXT, 0); }
		public HeredocEndContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_heredocEnd; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterHeredocEnd(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitHeredocEnd(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitHeredocEnd(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HeredocEndContext heredocEnd() throws RecognitionException {
		HeredocEndContext _localctx = new HeredocEndContext(_ctx, getState());
		enterRule(_localctx, 74, RULE_heredocEnd);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(357);
			match(UNQUOTED_TEXT);
			}
		}
		catch (RecognitionException re) {
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
	public static class JsonArrayContext extends ParserRuleContext {
		public TerminalNode LBRACKET() { return getToken(DockerParser.LBRACKET, 0); }
		public TerminalNode RBRACKET() { return getToken(DockerParser.RBRACKET, 0); }
		public JsonArrayElementsContext jsonArrayElements() {
			return getRuleContext(JsonArrayElementsContext.class,0);
		}
		public JsonArrayContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_jsonArray; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterJsonArray(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitJsonArray(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitJsonArray(this);
			else return visitor.visitChildren(this);
		}
	}

	public final JsonArrayContext jsonArray() throws RecognitionException {
		JsonArrayContext _localctx = new JsonArrayContext(_ctx, getState());
		enterRule(_localctx, 76, RULE_jsonArray);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(359);
			match(LBRACKET);
			setState(361);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==DOUBLE_QUOTED_STRING) {
				{
				setState(360);
				jsonArrayElements();
				}
			}

			setState(363);
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
	public static class JsonArrayElementsContext extends ParserRuleContext {
		public List<JsonStringContext> jsonString() {
			return getRuleContexts(JsonStringContext.class);
		}
		public JsonStringContext jsonString(int i) {
			return getRuleContext(JsonStringContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(DockerParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(DockerParser.COMMA, i);
		}
		public JsonArrayElementsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_jsonArrayElements; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterJsonArrayElements(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitJsonArrayElements(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitJsonArrayElements(this);
			else return visitor.visitChildren(this);
		}
	}

	public final JsonArrayElementsContext jsonArrayElements() throws RecognitionException {
		JsonArrayElementsContext _localctx = new JsonArrayElementsContext(_ctx, getState());
		enterRule(_localctx, 78, RULE_jsonArrayElements);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(365);
			jsonString();
			setState(370);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(366);
				match(COMMA);
				setState(367);
				jsonString();
				}
				}
				setState(372);
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
	public static class JsonStringContext extends ParserRuleContext {
		public TerminalNode DOUBLE_QUOTED_STRING() { return getToken(DockerParser.DOUBLE_QUOTED_STRING, 0); }
		public JsonStringContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_jsonString; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterJsonString(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitJsonString(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitJsonString(this);
			else return visitor.visitChildren(this);
		}
	}

	public final JsonStringContext jsonString() throws RecognitionException {
		JsonStringContext _localctx = new JsonStringContext(_ctx, getState());
		enterRule(_localctx, 80, RULE_jsonString);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(373);
			match(DOUBLE_QUOTED_STRING);
			}
		}
		catch (RecognitionException re) {
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
	public static class ImageNameContext extends ParserRuleContext {
		public TextContext text() {
			return getRuleContext(TextContext.class,0);
		}
		public ImageNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_imageName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterImageName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitImageName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitImageName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ImageNameContext imageName() throws RecognitionException {
		ImageNameContext _localctx = new ImageNameContext(_ctx, getState());
		enterRule(_localctx, 82, RULE_imageName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(375);
			text();
			}
		}
		catch (RecognitionException re) {
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
	public static class StageNameContext extends ParserRuleContext {
		public TerminalNode UNQUOTED_TEXT() { return getToken(DockerParser.UNQUOTED_TEXT, 0); }
		public StageNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stageName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterStageName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitStageName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitStageName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StageNameContext stageName() throws RecognitionException {
		StageNameContext _localctx = new StageNameContext(_ctx, getState());
		enterRule(_localctx, 84, RULE_stageName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(377);
			match(UNQUOTED_TEXT);
			}
		}
		catch (RecognitionException re) {
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
	public static class LabelPairsContext extends ParserRuleContext {
		public List<LabelPairContext> labelPair() {
			return getRuleContexts(LabelPairContext.class);
		}
		public LabelPairContext labelPair(int i) {
			return getRuleContext(LabelPairContext.class,i);
		}
		public LabelPairsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_labelPairs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterLabelPairs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitLabelPairs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitLabelPairs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LabelPairsContext labelPairs() throws RecognitionException {
		LabelPairsContext _localctx = new LabelPairsContext(_ctx, getState());
		enterRule(_localctx, 86, RULE_labelPairs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(380); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(379);
				labelPair();
				}
				}
				setState(382); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & 143881404416L) != 0) );
			}
		}
		catch (RecognitionException re) {
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
	public static class LabelPairContext extends ParserRuleContext {
		public LabelKeyContext labelKey() {
			return getRuleContext(LabelKeyContext.class,0);
		}
		public TerminalNode EQUALS() { return getToken(DockerParser.EQUALS, 0); }
		public LabelValueContext labelValue() {
			return getRuleContext(LabelValueContext.class,0);
		}
		public LabelOldValueContext labelOldValue() {
			return getRuleContext(LabelOldValueContext.class,0);
		}
		public LabelPairContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_labelPair; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterLabelPair(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitLabelPair(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitLabelPair(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LabelPairContext labelPair() throws RecognitionException {
		LabelPairContext _localctx = new LabelPairContext(_ctx, getState());
		enterRule(_localctx, 88, RULE_labelPair);
		try {
			setState(391);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,32,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(384);
				labelKey();
				setState(385);
				match(EQUALS);
				setState(386);
				labelValue();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(388);
				labelKey();
				setState(389);
				labelOldValue();
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
	public static class LabelKeyContext extends ParserRuleContext {
		public TerminalNode UNQUOTED_TEXT() { return getToken(DockerParser.UNQUOTED_TEXT, 0); }
		public TerminalNode DOUBLE_QUOTED_STRING() { return getToken(DockerParser.DOUBLE_QUOTED_STRING, 0); }
		public TerminalNode SINGLE_QUOTED_STRING() { return getToken(DockerParser.SINGLE_QUOTED_STRING, 0); }
		public LabelKeyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_labelKey; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterLabelKey(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitLabelKey(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitLabelKey(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LabelKeyContext labelKey() throws RecognitionException {
		LabelKeyContext _localctx = new LabelKeyContext(_ctx, getState());
		enterRule(_localctx, 90, RULE_labelKey);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(393);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 143881404416L) != 0)) ) {
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
	public static class LabelValueContext extends ParserRuleContext {
		public TerminalNode UNQUOTED_TEXT() { return getToken(DockerParser.UNQUOTED_TEXT, 0); }
		public TerminalNode DOUBLE_QUOTED_STRING() { return getToken(DockerParser.DOUBLE_QUOTED_STRING, 0); }
		public TerminalNode SINGLE_QUOTED_STRING() { return getToken(DockerParser.SINGLE_QUOTED_STRING, 0); }
		public LabelValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_labelValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterLabelValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitLabelValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitLabelValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LabelValueContext labelValue() throws RecognitionException {
		LabelValueContext _localctx = new LabelValueContext(_ctx, getState());
		enterRule(_localctx, 92, RULE_labelValue);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(395);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 143881404416L) != 0)) ) {
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
	public static class LabelOldValueContext extends ParserRuleContext {
		public List<LabelOldValueElementContext> labelOldValueElement() {
			return getRuleContexts(LabelOldValueElementContext.class);
		}
		public LabelOldValueElementContext labelOldValueElement(int i) {
			return getRuleContext(LabelOldValueElementContext.class,i);
		}
		public LabelOldValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_labelOldValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterLabelOldValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitLabelOldValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitLabelOldValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LabelOldValueContext labelOldValue() throws RecognitionException {
		LabelOldValueContext _localctx = new LabelOldValueContext(_ctx, getState());
		enterRule(_localctx, 94, RULE_labelOldValue);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(398); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(397);
					labelOldValueElement();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(400); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,33,_ctx);
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
	public static class LabelOldValueElementContext extends ParserRuleContext {
		public TerminalNode UNQUOTED_TEXT() { return getToken(DockerParser.UNQUOTED_TEXT, 0); }
		public TerminalNode DOUBLE_QUOTED_STRING() { return getToken(DockerParser.DOUBLE_QUOTED_STRING, 0); }
		public TerminalNode SINGLE_QUOTED_STRING() { return getToken(DockerParser.SINGLE_QUOTED_STRING, 0); }
		public TerminalNode ENV_VAR() { return getToken(DockerParser.ENV_VAR, 0); }
		public TerminalNode COMMAND_SUBST() { return getToken(DockerParser.COMMAND_SUBST, 0); }
		public TerminalNode BACKTICK_SUBST() { return getToken(DockerParser.BACKTICK_SUBST, 0); }
		public TerminalNode SPECIAL_VAR() { return getToken(DockerParser.SPECIAL_VAR, 0); }
		public TerminalNode EQUALS() { return getToken(DockerParser.EQUALS, 0); }
		public TerminalNode FLAG() { return getToken(DockerParser.FLAG, 0); }
		public TerminalNode DASH_DASH() { return getToken(DockerParser.DASH_DASH, 0); }
		public TerminalNode LBRACKET() { return getToken(DockerParser.LBRACKET, 0); }
		public TerminalNode RBRACKET() { return getToken(DockerParser.RBRACKET, 0); }
		public TerminalNode COMMA() { return getToken(DockerParser.COMMA, 0); }
		public LabelOldValueElementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_labelOldValueElement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterLabelOldValueElement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitLabelOldValueElement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitLabelOldValueElement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LabelOldValueElementContext labelOldValueElement() throws RecognitionException {
		LabelOldValueElementContext _localctx = new LabelOldValueElementContext(_ctx, getState());
		enterRule(_localctx, 96, RULE_labelOldValueElement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(402);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 274844352512L) != 0)) ) {
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
	public static class PortListContext extends ParserRuleContext {
		public List<PortContext> port() {
			return getRuleContexts(PortContext.class);
		}
		public PortContext port(int i) {
			return getRuleContext(PortContext.class,i);
		}
		public PortListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_portList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterPortList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitPortList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitPortList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PortListContext portList() throws RecognitionException {
		PortListContext _localctx = new PortListContext(_ctx, getState());
		enterRule(_localctx, 98, RULE_portList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(405); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(404);
				port();
				}
				}
				setState(407); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & 266287972352L) != 0) );
			}
		}
		catch (RecognitionException re) {
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
	public static class PortContext extends ParserRuleContext {
		public TerminalNode UNQUOTED_TEXT() { return getToken(DockerParser.UNQUOTED_TEXT, 0); }
		public TerminalNode ENV_VAR() { return getToken(DockerParser.ENV_VAR, 0); }
		public TerminalNode COMMAND_SUBST() { return getToken(DockerParser.COMMAND_SUBST, 0); }
		public TerminalNode BACKTICK_SUBST() { return getToken(DockerParser.BACKTICK_SUBST, 0); }
		public TerminalNode SPECIAL_VAR() { return getToken(DockerParser.SPECIAL_VAR, 0); }
		public PortContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_port; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterPort(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitPort(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitPort(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PortContext port() throws RecognitionException {
		PortContext _localctx = new PortContext(_ctx, getState());
		enterRule(_localctx, 100, RULE_port);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(409);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 266287972352L) != 0)) ) {
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
	public static class EnvPairsContext extends ParserRuleContext {
		public List<EnvPairContext> envPair() {
			return getRuleContexts(EnvPairContext.class);
		}
		public EnvPairContext envPair(int i) {
			return getRuleContext(EnvPairContext.class,i);
		}
		public EnvPairsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_envPairs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterEnvPairs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitEnvPairs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitEnvPairs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EnvPairsContext envPairs() throws RecognitionException {
		EnvPairsContext _localctx = new EnvPairsContext(_ctx, getState());
		enterRule(_localctx, 102, RULE_envPairs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(412); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(411);
				envPair();
				}
				}
				setState(414); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==UNQUOTED_TEXT );
			}
		}
		catch (RecognitionException re) {
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
	public static class EnvPairContext extends ParserRuleContext {
		public EnvKeyContext envKey() {
			return getRuleContext(EnvKeyContext.class,0);
		}
		public TerminalNode EQUALS() { return getToken(DockerParser.EQUALS, 0); }
		public EnvValueEqualsContext envValueEquals() {
			return getRuleContext(EnvValueEqualsContext.class,0);
		}
		public EnvValueSpaceContext envValueSpace() {
			return getRuleContext(EnvValueSpaceContext.class,0);
		}
		public EnvPairContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_envPair; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterEnvPair(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitEnvPair(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitEnvPair(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EnvPairContext envPair() throws RecognitionException {
		EnvPairContext _localctx = new EnvPairContext(_ctx, getState());
		enterRule(_localctx, 104, RULE_envPair);
		try {
			setState(423);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,36,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(416);
				envKey();
				setState(417);
				match(EQUALS);
				setState(418);
				envValueEquals();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(420);
				envKey();
				setState(421);
				envValueSpace();
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
	public static class EnvKeyContext extends ParserRuleContext {
		public TerminalNode UNQUOTED_TEXT() { return getToken(DockerParser.UNQUOTED_TEXT, 0); }
		public EnvKeyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_envKey; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterEnvKey(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitEnvKey(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitEnvKey(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EnvKeyContext envKey() throws RecognitionException {
		EnvKeyContext _localctx = new EnvKeyContext(_ctx, getState());
		enterRule(_localctx, 106, RULE_envKey);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(425);
			match(UNQUOTED_TEXT);
			}
		}
		catch (RecognitionException re) {
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
	public static class EnvValueEqualsContext extends ParserRuleContext {
		public EnvTextEqualsContext envTextEquals() {
			return getRuleContext(EnvTextEqualsContext.class,0);
		}
		public EnvValueEqualsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_envValueEquals; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterEnvValueEquals(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitEnvValueEquals(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitEnvValueEquals(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EnvValueEqualsContext envValueEquals() throws RecognitionException {
		EnvValueEqualsContext _localctx = new EnvValueEqualsContext(_ctx, getState());
		enterRule(_localctx, 108, RULE_envValueEquals);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(427);
			envTextEquals();
			}
		}
		catch (RecognitionException re) {
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
	public static class EnvValueSpaceContext extends ParserRuleContext {
		public TextContext text() {
			return getRuleContext(TextContext.class,0);
		}
		public EnvValueSpaceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_envValueSpace; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterEnvValueSpace(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitEnvValueSpace(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitEnvValueSpace(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EnvValueSpaceContext envValueSpace() throws RecognitionException {
		EnvValueSpaceContext _localctx = new EnvValueSpaceContext(_ctx, getState());
		enterRule(_localctx, 110, RULE_envValueSpace);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(429);
			text();
			}
		}
		catch (RecognitionException re) {
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
	public static class EnvTextEqualsContext extends ParserRuleContext {
		public List<EnvTextElementEqualsContext> envTextElementEquals() {
			return getRuleContexts(EnvTextElementEqualsContext.class);
		}
		public EnvTextElementEqualsContext envTextElementEquals(int i) {
			return getRuleContext(EnvTextElementEqualsContext.class,i);
		}
		public EnvTextEqualsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_envTextEquals; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterEnvTextEquals(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitEnvTextEquals(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitEnvTextEquals(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EnvTextEqualsContext envTextEquals() throws RecognitionException {
		EnvTextEqualsContext _localctx = new EnvTextEqualsContext(_ctx, getState());
		enterRule(_localctx, 112, RULE_envTextEquals);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(432); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(431);
					envTextElementEquals();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(434); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,37,_ctx);
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
	public static class EnvTextElementEqualsContext extends ParserRuleContext {
		public TerminalNode UNQUOTED_TEXT() { return getToken(DockerParser.UNQUOTED_TEXT, 0); }
		public TerminalNode DOUBLE_QUOTED_STRING() { return getToken(DockerParser.DOUBLE_QUOTED_STRING, 0); }
		public TerminalNode SINGLE_QUOTED_STRING() { return getToken(DockerParser.SINGLE_QUOTED_STRING, 0); }
		public TerminalNode ENV_VAR() { return getToken(DockerParser.ENV_VAR, 0); }
		public TerminalNode COMMAND_SUBST() { return getToken(DockerParser.COMMAND_SUBST, 0); }
		public TerminalNode BACKTICK_SUBST() { return getToken(DockerParser.BACKTICK_SUBST, 0); }
		public TerminalNode SPECIAL_VAR() { return getToken(DockerParser.SPECIAL_VAR, 0); }
		public EnvTextElementEqualsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_envTextElementEquals; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterEnvTextElementEquals(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitEnvTextElementEquals(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitEnvTextElementEquals(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EnvTextElementEqualsContext envTextElementEquals() throws RecognitionException {
		EnvTextElementEqualsContext _localctx = new EnvTextElementEqualsContext(_ctx, getState());
		enterRule(_localctx, 114, RULE_envTextElementEquals);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(436);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 272730423296L) != 0)) ) {
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
	public static class SourceListContext extends ParserRuleContext {
		public List<SourcePathContext> sourcePath() {
			return getRuleContexts(SourcePathContext.class);
		}
		public SourcePathContext sourcePath(int i) {
			return getRuleContext(SourcePathContext.class,i);
		}
		public SourceListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_sourceList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterSourceList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitSourceList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitSourceList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SourceListContext sourceList() throws RecognitionException {
		SourceListContext _localctx = new SourceListContext(_ctx, getState());
		enterRule(_localctx, 116, RULE_sourceList);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(439); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(438);
					sourcePath();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(441); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,38,_ctx);
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
	public static class SourcePathContext extends ParserRuleContext {
		public TerminalNode UNQUOTED_TEXT() { return getToken(DockerParser.UNQUOTED_TEXT, 0); }
		public TerminalNode DOUBLE_QUOTED_STRING() { return getToken(DockerParser.DOUBLE_QUOTED_STRING, 0); }
		public TerminalNode SINGLE_QUOTED_STRING() { return getToken(DockerParser.SINGLE_QUOTED_STRING, 0); }
		public TerminalNode ENV_VAR() { return getToken(DockerParser.ENV_VAR, 0); }
		public TerminalNode COMMAND_SUBST() { return getToken(DockerParser.COMMAND_SUBST, 0); }
		public TerminalNode BACKTICK_SUBST() { return getToken(DockerParser.BACKTICK_SUBST, 0); }
		public TerminalNode SPECIAL_VAR() { return getToken(DockerParser.SPECIAL_VAR, 0); }
		public SourcePathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_sourcePath; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterSourcePath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitSourcePath(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitSourcePath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SourcePathContext sourcePath() throws RecognitionException {
		SourcePathContext _localctx = new SourcePathContext(_ctx, getState());
		enterRule(_localctx, 118, RULE_sourcePath);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(443);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 272730423296L) != 0)) ) {
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
	public static class DestinationContext extends ParserRuleContext {
		public DestinationPathContext destinationPath() {
			return getRuleContext(DestinationPathContext.class,0);
		}
		public DestinationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_destination; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterDestination(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitDestination(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitDestination(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DestinationContext destination() throws RecognitionException {
		DestinationContext _localctx = new DestinationContext(_ctx, getState());
		enterRule(_localctx, 120, RULE_destination);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(445);
			destinationPath();
			}
		}
		catch (RecognitionException re) {
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
	public static class DestinationPathContext extends ParserRuleContext {
		public TerminalNode UNQUOTED_TEXT() { return getToken(DockerParser.UNQUOTED_TEXT, 0); }
		public TerminalNode DOUBLE_QUOTED_STRING() { return getToken(DockerParser.DOUBLE_QUOTED_STRING, 0); }
		public TerminalNode SINGLE_QUOTED_STRING() { return getToken(DockerParser.SINGLE_QUOTED_STRING, 0); }
		public TerminalNode ENV_VAR() { return getToken(DockerParser.ENV_VAR, 0); }
		public TerminalNode COMMAND_SUBST() { return getToken(DockerParser.COMMAND_SUBST, 0); }
		public TerminalNode BACKTICK_SUBST() { return getToken(DockerParser.BACKTICK_SUBST, 0); }
		public TerminalNode SPECIAL_VAR() { return getToken(DockerParser.SPECIAL_VAR, 0); }
		public DestinationPathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_destinationPath; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterDestinationPath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitDestinationPath(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitDestinationPath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DestinationPathContext destinationPath() throws RecognitionException {
		DestinationPathContext _localctx = new DestinationPathContext(_ctx, getState());
		enterRule(_localctx, 122, RULE_destinationPath);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(447);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 272730423296L) != 0)) ) {
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
	public static class PathContext extends ParserRuleContext {
		public TextContext text() {
			return getRuleContext(TextContext.class,0);
		}
		public PathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_path; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterPath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitPath(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitPath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PathContext path() throws RecognitionException {
		PathContext _localctx = new PathContext(_ctx, getState());
		enterRule(_localctx, 124, RULE_path);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(449);
			text();
			}
		}
		catch (RecognitionException re) {
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
	public static class PathListContext extends ParserRuleContext {
		public List<VolumePathContext> volumePath() {
			return getRuleContexts(VolumePathContext.class);
		}
		public VolumePathContext volumePath(int i) {
			return getRuleContext(VolumePathContext.class,i);
		}
		public PathListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pathList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterPathList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitPathList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitPathList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PathListContext pathList() throws RecognitionException {
		PathListContext _localctx = new PathListContext(_ctx, getState());
		enterRule(_localctx, 126, RULE_pathList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(452); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(451);
				volumePath();
				}
				}
				setState(454); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & 272730423296L) != 0) );
			}
		}
		catch (RecognitionException re) {
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
	public static class VolumePathContext extends ParserRuleContext {
		public TerminalNode UNQUOTED_TEXT() { return getToken(DockerParser.UNQUOTED_TEXT, 0); }
		public TerminalNode DOUBLE_QUOTED_STRING() { return getToken(DockerParser.DOUBLE_QUOTED_STRING, 0); }
		public TerminalNode SINGLE_QUOTED_STRING() { return getToken(DockerParser.SINGLE_QUOTED_STRING, 0); }
		public TerminalNode ENV_VAR() { return getToken(DockerParser.ENV_VAR, 0); }
		public TerminalNode COMMAND_SUBST() { return getToken(DockerParser.COMMAND_SUBST, 0); }
		public TerminalNode BACKTICK_SUBST() { return getToken(DockerParser.BACKTICK_SUBST, 0); }
		public TerminalNode SPECIAL_VAR() { return getToken(DockerParser.SPECIAL_VAR, 0); }
		public VolumePathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_volumePath; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterVolumePath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitVolumePath(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitVolumePath(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VolumePathContext volumePath() throws RecognitionException {
		VolumePathContext _localctx = new VolumePathContext(_ctx, getState());
		enterRule(_localctx, 128, RULE_volumePath);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(456);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 272730423296L) != 0)) ) {
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
	public static class UserSpecContext extends ParserRuleContext {
		public TextContext text() {
			return getRuleContext(TextContext.class,0);
		}
		public UserSpecContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_userSpec; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterUserSpec(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitUserSpec(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitUserSpec(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UserSpecContext userSpec() throws RecognitionException {
		UserSpecContext _localctx = new UserSpecContext(_ctx, getState());
		enterRule(_localctx, 130, RULE_userSpec);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(458);
			text();
			}
		}
		catch (RecognitionException re) {
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
	public static class ArgNameContext extends ParserRuleContext {
		public TerminalNode UNQUOTED_TEXT() { return getToken(DockerParser.UNQUOTED_TEXT, 0); }
		public ArgNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_argName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterArgName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitArgName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitArgName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArgNameContext argName() throws RecognitionException {
		ArgNameContext _localctx = new ArgNameContext(_ctx, getState());
		enterRule(_localctx, 132, RULE_argName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(460);
			match(UNQUOTED_TEXT);
			}
		}
		catch (RecognitionException re) {
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
	public static class ArgValueContext extends ParserRuleContext {
		public TextContext text() {
			return getRuleContext(TextContext.class,0);
		}
		public ArgValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_argValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterArgValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitArgValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitArgValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArgValueContext argValue() throws RecognitionException {
		ArgValueContext _localctx = new ArgValueContext(_ctx, getState());
		enterRule(_localctx, 134, RULE_argValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(462);
			text();
			}
		}
		catch (RecognitionException re) {
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
	public static class SignalContext extends ParserRuleContext {
		public TerminalNode UNQUOTED_TEXT() { return getToken(DockerParser.UNQUOTED_TEXT, 0); }
		public SignalContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_signal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterSignal(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitSignal(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitSignal(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SignalContext signal() throws RecognitionException {
		SignalContext _localctx = new SignalContext(_ctx, getState());
		enterRule(_localctx, 136, RULE_signal);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(464);
			match(UNQUOTED_TEXT);
			}
		}
		catch (RecognitionException re) {
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
	public static class TextContext extends ParserRuleContext {
		public List<TextElementContext> textElement() {
			return getRuleContexts(TextElementContext.class);
		}
		public TextElementContext textElement(int i) {
			return getRuleContext(TextElementContext.class,i);
		}
		public TextContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_text; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterText(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitText(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitText(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TextContext text() throws RecognitionException {
		TextContext _localctx = new TextContext(_ctx, getState());
		enterRule(_localctx, 138, RULE_text);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(467); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(466);
					textElement();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(469); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,40,_ctx);
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
	public static class TextElementContext extends ParserRuleContext {
		public TerminalNode UNQUOTED_TEXT() { return getToken(DockerParser.UNQUOTED_TEXT, 0); }
		public TerminalNode DOUBLE_QUOTED_STRING() { return getToken(DockerParser.DOUBLE_QUOTED_STRING, 0); }
		public TerminalNode SINGLE_QUOTED_STRING() { return getToken(DockerParser.SINGLE_QUOTED_STRING, 0); }
		public TerminalNode ENV_VAR() { return getToken(DockerParser.ENV_VAR, 0); }
		public TerminalNode COMMAND_SUBST() { return getToken(DockerParser.COMMAND_SUBST, 0); }
		public TerminalNode BACKTICK_SUBST() { return getToken(DockerParser.BACKTICK_SUBST, 0); }
		public TerminalNode SPECIAL_VAR() { return getToken(DockerParser.SPECIAL_VAR, 0); }
		public TerminalNode EQUALS() { return getToken(DockerParser.EQUALS, 0); }
		public TerminalNode FLAG() { return getToken(DockerParser.FLAG, 0); }
		public TerminalNode DASH_DASH() { return getToken(DockerParser.DASH_DASH, 0); }
		public TerminalNode LBRACKET() { return getToken(DockerParser.LBRACKET, 0); }
		public TerminalNode RBRACKET() { return getToken(DockerParser.RBRACKET, 0); }
		public TerminalNode COMMA() { return getToken(DockerParser.COMMA, 0); }
		public TextElementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_textElement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterTextElement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitTextElement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitTextElement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TextElementContext textElement() throws RecognitionException {
		TextElementContext _localctx = new TextElementContext(_ctx, getState());
		enterRule(_localctx, 140, RULE_textElement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(471);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 274844352512L) != 0)) ) {
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
		"\u0004\u0001-\u01da\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
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
		"A\u0007A\u0002B\u0007B\u0002C\u0007C\u0002D\u0007D\u0002E\u0007E\u0002"+
		"F\u0007F\u0001\u0000\u0005\u0000\u0090\b\u0000\n\u0000\f\u0000\u0093\t"+
		"\u0000\u0001\u0000\u0001\u0000\u0004\u0000\u0097\b\u0000\u000b\u0000\f"+
		"\u0000\u0098\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0002"+
		"\u0005\u0002\u00a0\b\u0002\n\u0002\f\u0002\u00a3\t\u0002\u0001\u0003\u0001"+
		"\u0003\u0005\u0003\u00a7\b\u0003\n\u0003\f\u0003\u00aa\t\u0003\u0001\u0004"+
		"\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004"+
		"\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004"+
		"\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0003\u0004\u00bd\b\u0004"+
		"\u0001\u0005\u0001\u0005\u0003\u0005\u00c1\b\u0005\u0001\u0006\u0001\u0006"+
		"\u0003\u0006\u00c5\b\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0003\u0006"+
		"\u00ca\b\u0006\u0001\u0007\u0001\u0007\u0003\u0007\u00ce\b\u0007\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0003\u0007\u00d3\b\u0007\u0001\b\u0001"+
		"\b\u0001\b\u0003\b\u00d8\b\b\u0001\t\u0001\t\u0001\t\u0001\n\u0001\n\u0001"+
		"\n\u0001\u000b\u0001\u000b\u0001\u000b\u0001\f\u0001\f\u0003\f\u00e5\b"+
		"\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0003\f\u00ec\b\f\u0001\r\u0001"+
		"\r\u0003\r\u00f0\b\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0003\r\u00f7"+
		"\b\r\u0001\u000e\u0001\u000e\u0001\u000e\u0003\u000e\u00fc\b\u000e\u0001"+
		"\u000f\u0001\u000f\u0001\u000f\u0003\u000f\u0101\b\u000f\u0001\u0010\u0001"+
		"\u0010\u0001\u0010\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0012\u0001"+
		"\u0012\u0001\u0012\u0001\u0012\u0003\u0012\u010d\b\u0012\u0001\u0013\u0001"+
		"\u0013\u0001\u0013\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0015\u0001"+
		"\u0015\u0001\u0015\u0001\u0015\u0003\u0015\u0119\b\u0015\u0001\u0015\u0001"+
		"\u0015\u0001\u0015\u0003\u0015\u011e\b\u0015\u0003\u0015\u0120\b\u0015"+
		"\u0001\u0016\u0004\u0016\u0123\b\u0016\u000b\u0016\f\u0016\u0124\u0001"+
		"\u0017\u0001\u0017\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0019\u0001"+
		"\u0019\u0001\u0019\u0001\u001a\u0004\u001a\u0130\b\u001a\u000b\u001a\f"+
		"\u001a\u0131\u0001\u001b\u0001\u001b\u0001\u001c\u0001\u001c\u0001\u001d"+
		"\u0001\u001d\u0001\u001e\u0004\u001e\u013b\b\u001e\u000b\u001e\f\u001e"+
		"\u013c\u0001\u001f\u0001\u001f\u0001 \u0001 \u0001 \u0004 \u0144\b \u000b"+
		" \f \u0145\u0001!\u0001!\u0005!\u014a\b!\n!\f!\u014d\t!\u0001!\u0001!"+
		"\u0005!\u0151\b!\n!\f!\u0154\t!\u0005!\u0156\b!\n!\f!\u0159\t!\u0001\""+
		"\u0001\"\u0001#\u0001#\u0001#\u0001$\u0005$\u0161\b$\n$\f$\u0164\t$\u0001"+
		"%\u0001%\u0001&\u0001&\u0003&\u016a\b&\u0001&\u0001&\u0001\'\u0001\'\u0001"+
		"\'\u0005\'\u0171\b\'\n\'\f\'\u0174\t\'\u0001(\u0001(\u0001)\u0001)\u0001"+
		"*\u0001*\u0001+\u0004+\u017d\b+\u000b+\f+\u017e\u0001,\u0001,\u0001,\u0001"+
		",\u0001,\u0001,\u0001,\u0003,\u0188\b,\u0001-\u0001-\u0001.\u0001.\u0001"+
		"/\u0004/\u018f\b/\u000b/\f/\u0190\u00010\u00010\u00011\u00041\u0196\b"+
		"1\u000b1\f1\u0197\u00012\u00012\u00013\u00043\u019d\b3\u000b3\f3\u019e"+
		"\u00014\u00014\u00014\u00014\u00014\u00014\u00014\u00034\u01a8\b4\u0001"+
		"5\u00015\u00016\u00016\u00017\u00017\u00018\u00048\u01b1\b8\u000b8\f8"+
		"\u01b2\u00019\u00019\u0001:\u0004:\u01b8\b:\u000b:\f:\u01b9\u0001;\u0001"+
		";\u0001<\u0001<\u0001=\u0001=\u0001>\u0001>\u0001?\u0004?\u01c5\b?\u000b"+
		"?\f?\u01c6\u0001@\u0001@\u0001A\u0001A\u0001B\u0001B\u0001C\u0001C\u0001"+
		"D\u0001D\u0001E\u0004E\u01d4\bE\u000bE\fE\u01d5\u0001F\u0001F\u0001F\u0000"+
		"\u0000G\u0000\u0002\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018"+
		"\u001a\u001c\u001e \"$&(*,.02468:<>@BDFHJLNPRTVXZ\\^`bdfhjlnprtvxz|~\u0080"+
		"\u0082\u0084\u0086\u0088\u008a\u008c\u0000\u0005\u0001\u0000\u0019%\u0002"+
		"\u0000\'\',,\u0002\u0000\u001f %%\u0001\u0000!%\u0001\u0000\u001f%\u01cd"+
		"\u0000\u0091\u0001\u0000\u0000\u0000\u0002\u009c\u0001\u0000\u0000\u0000"+
		"\u0004\u00a1\u0001\u0000\u0000\u0000\u0006\u00a4\u0001\u0000\u0000\u0000"+
		"\b\u00bc\u0001\u0000\u0000\u0000\n\u00c0\u0001\u0000\u0000\u0000\f\u00c2"+
		"\u0001\u0000\u0000\u0000\u000e\u00cb\u0001\u0000\u0000\u0000\u0010\u00d4"+
		"\u0001\u0000\u0000\u0000\u0012\u00d9\u0001\u0000\u0000\u0000\u0014\u00dc"+
		"\u0001\u0000\u0000\u0000\u0016\u00df\u0001\u0000\u0000\u0000\u0018\u00e2"+
		"\u0001\u0000\u0000\u0000\u001a\u00ed\u0001\u0000\u0000\u0000\u001c\u00f8"+
		"\u0001\u0000\u0000\u0000\u001e\u00fd\u0001\u0000\u0000\u0000 \u0102\u0001"+
		"\u0000\u0000\u0000\"\u0105\u0001\u0000\u0000\u0000$\u0108\u0001\u0000"+
		"\u0000\u0000&\u010e\u0001\u0000\u0000\u0000(\u0111\u0001\u0000\u0000\u0000"+
		"*\u011f\u0001\u0000\u0000\u0000,\u0122\u0001\u0000\u0000\u0000.\u0126"+
		"\u0001\u0000\u0000\u00000\u0128\u0001\u0000\u0000\u00002\u012b\u0001\u0000"+
		"\u0000\u00004\u012f\u0001\u0000\u0000\u00006\u0133\u0001\u0000\u0000\u0000"+
		"8\u0135\u0001\u0000\u0000\u0000:\u0137\u0001\u0000\u0000\u0000<\u013a"+
		"\u0001\u0000\u0000\u0000>\u013e\u0001\u0000\u0000\u0000@\u0140\u0001\u0000"+
		"\u0000\u0000B\u0147\u0001\u0000\u0000\u0000D\u015a\u0001\u0000\u0000\u0000"+
		"F\u015c\u0001\u0000\u0000\u0000H\u0162\u0001\u0000\u0000\u0000J\u0165"+
		"\u0001\u0000\u0000\u0000L\u0167\u0001\u0000\u0000\u0000N\u016d\u0001\u0000"+
		"\u0000\u0000P\u0175\u0001\u0000\u0000\u0000R\u0177\u0001\u0000\u0000\u0000"+
		"T\u0179\u0001\u0000\u0000\u0000V\u017c\u0001\u0000\u0000\u0000X\u0187"+
		"\u0001\u0000\u0000\u0000Z\u0189\u0001\u0000\u0000\u0000\\\u018b\u0001"+
		"\u0000\u0000\u0000^\u018e\u0001\u0000\u0000\u0000`\u0192\u0001\u0000\u0000"+
		"\u0000b\u0195\u0001\u0000\u0000\u0000d\u0199\u0001\u0000\u0000\u0000f"+
		"\u019c\u0001\u0000\u0000\u0000h\u01a7\u0001\u0000\u0000\u0000j\u01a9\u0001"+
		"\u0000\u0000\u0000l\u01ab\u0001\u0000\u0000\u0000n\u01ad\u0001\u0000\u0000"+
		"\u0000p\u01b0\u0001\u0000\u0000\u0000r\u01b4\u0001\u0000\u0000\u0000t"+
		"\u01b7\u0001\u0000\u0000\u0000v\u01bb\u0001\u0000\u0000\u0000x\u01bd\u0001"+
		"\u0000\u0000\u0000z\u01bf\u0001\u0000\u0000\u0000|\u01c1\u0001\u0000\u0000"+
		"\u0000~\u01c4\u0001\u0000\u0000\u0000\u0080\u01c8\u0001\u0000\u0000\u0000"+
		"\u0082\u01ca\u0001\u0000\u0000\u0000\u0084\u01cc\u0001\u0000\u0000\u0000"+
		"\u0086\u01ce\u0001\u0000\u0000\u0000\u0088\u01d0\u0001\u0000\u0000\u0000"+
		"\u008a\u01d3\u0001\u0000\u0000\u0000\u008c\u01d7\u0001\u0000\u0000\u0000"+
		"\u008e\u0090\u0003\u0002\u0001\u0000\u008f\u008e\u0001\u0000\u0000\u0000"+
		"\u0090\u0093\u0001\u0000\u0000\u0000\u0091\u008f\u0001\u0000\u0000\u0000"+
		"\u0091\u0092\u0001\u0000\u0000\u0000\u0092\u0094\u0001\u0000\u0000\u0000"+
		"\u0093\u0091\u0001\u0000\u0000\u0000\u0094\u0096\u0003\u0004\u0002\u0000"+
		"\u0095\u0097\u0003\u0006\u0003\u0000\u0096\u0095\u0001\u0000\u0000\u0000"+
		"\u0097\u0098\u0001\u0000\u0000\u0000\u0098\u0096\u0001\u0000\u0000\u0000"+
		"\u0098\u0099\u0001\u0000\u0000\u0000\u0099\u009a\u0001\u0000\u0000\u0000"+
		"\u009a\u009b\u0005\u0000\u0000\u0001\u009b\u0001\u0001\u0000\u0000\u0000"+
		"\u009c\u009d\u0005\u0001\u0000\u0000\u009d\u0003\u0001\u0000\u0000\u0000"+
		"\u009e\u00a0\u0003$\u0012\u0000\u009f\u009e\u0001\u0000\u0000\u0000\u00a0"+
		"\u00a3\u0001\u0000\u0000\u0000\u00a1\u009f\u0001\u0000\u0000\u0000\u00a1"+
		"\u00a2\u0001\u0000\u0000\u0000\u00a2\u0005\u0001\u0000\u0000\u0000\u00a3"+
		"\u00a1\u0001\u0000\u0000\u0000\u00a4\u00a8\u0003\f\u0006\u0000\u00a5\u00a7"+
		"\u0003\b\u0004\u0000\u00a6\u00a5\u0001\u0000\u0000\u0000\u00a7\u00aa\u0001"+
		"\u0000\u0000\u0000\u00a8\u00a6\u0001\u0000\u0000\u0000\u00a8\u00a9\u0001"+
		"\u0000\u0000\u0000\u00a9\u0007\u0001\u0000\u0000\u0000\u00aa\u00a8\u0001"+
		"\u0000\u0000\u0000\u00ab\u00bd\u0003\u000e\u0007\u0000\u00ac\u00bd\u0003"+
		"\u0010\b\u0000\u00ad\u00bd\u0003\u0012\t\u0000\u00ae\u00bd\u0003\u0014"+
		"\n\u0000\u00af\u00bd\u0003\u0016\u000b\u0000\u00b0\u00bd\u0003\u0018\f"+
		"\u0000\u00b1\u00bd\u0003\u001a\r\u0000\u00b2\u00bd\u0003\u001c\u000e\u0000"+
		"\u00b3\u00bd\u0003\u001e\u000f\u0000\u00b4\u00bd\u0003 \u0010\u0000\u00b5"+
		"\u00bd\u0003\"\u0011\u0000\u00b6\u00bd\u0003$\u0012\u0000\u00b7\u00bd"+
		"\u0003&\u0013\u0000\u00b8\u00bd\u0003(\u0014\u0000\u00b9\u00bd\u0003*"+
		"\u0015\u0000\u00ba\u00bd\u00030\u0018\u0000\u00bb\u00bd\u00032\u0019\u0000"+
		"\u00bc\u00ab\u0001\u0000\u0000\u0000\u00bc\u00ac\u0001\u0000\u0000\u0000"+
		"\u00bc\u00ad\u0001\u0000\u0000\u0000\u00bc\u00ae\u0001\u0000\u0000\u0000"+
		"\u00bc\u00af\u0001\u0000\u0000\u0000\u00bc\u00b0\u0001\u0000\u0000\u0000"+
		"\u00bc\u00b1\u0001\u0000\u0000\u0000\u00bc\u00b2\u0001\u0000\u0000\u0000"+
		"\u00bc\u00b3\u0001\u0000\u0000\u0000\u00bc\u00b4\u0001\u0000\u0000\u0000"+
		"\u00bc\u00b5\u0001\u0000\u0000\u0000\u00bc\u00b6\u0001\u0000\u0000\u0000"+
		"\u00bc\u00b7\u0001\u0000\u0000\u0000\u00bc\u00b8\u0001\u0000\u0000\u0000"+
		"\u00bc\u00b9\u0001\u0000\u0000\u0000\u00bc\u00ba\u0001\u0000\u0000\u0000"+
		"\u00bc\u00bb\u0001\u0000\u0000\u0000\u00bd\t\u0001\u0000\u0000\u0000\u00be"+
		"\u00c1\u0003\f\u0006\u0000\u00bf\u00c1\u0003\b\u0004\u0000\u00c0\u00be"+
		"\u0001\u0000\u0000\u0000\u00c0\u00bf\u0001\u0000\u0000\u0000\u00c1\u000b"+
		"\u0001\u0000\u0000\u0000\u00c2\u00c4\u0005\u0003\u0000\u0000\u00c3\u00c5"+
		"\u00034\u001a\u0000\u00c4\u00c3\u0001\u0000\u0000\u0000\u00c4\u00c5\u0001"+
		"\u0000\u0000\u0000\u00c5\u00c6\u0001\u0000\u0000\u0000\u00c6\u00c9\u0003"+
		"R)\u0000\u00c7\u00c8\u0005\u0016\u0000\u0000\u00c8\u00ca\u0003T*\u0000"+
		"\u00c9\u00c7\u0001\u0000\u0000\u0000\u00c9\u00ca\u0001\u0000\u0000\u0000"+
		"\u00ca\r\u0001\u0000\u0000\u0000\u00cb\u00cd\u0005\u0004\u0000\u0000\u00cc"+
		"\u00ce\u00034\u001a\u0000\u00cd\u00cc\u0001\u0000\u0000\u0000\u00cd\u00ce"+
		"\u0001\u0000\u0000\u0000\u00ce\u00d2\u0001\u0000\u0000\u0000\u00cf\u00d3"+
		"\u00038\u001c\u0000\u00d0\u00d3\u0003:\u001d\u0000\u00d1\u00d3\u0003@"+
		" \u0000\u00d2\u00cf\u0001\u0000\u0000\u0000\u00d2\u00d0\u0001\u0000\u0000"+
		"\u0000\u00d2\u00d1\u0001\u0000\u0000\u0000\u00d3\u000f\u0001\u0000\u0000"+
		"\u0000\u00d4\u00d7\u0005\u0005\u0000\u0000\u00d5\u00d8\u00038\u001c\u0000"+
		"\u00d6\u00d8\u0003:\u001d\u0000\u00d7\u00d5\u0001\u0000\u0000\u0000\u00d7"+
		"\u00d6\u0001\u0000\u0000\u0000\u00d8\u0011\u0001\u0000\u0000\u0000\u00d9"+
		"\u00da\u0005\u0007\u0000\u0000\u00da\u00db\u0003V+\u0000\u00db\u0013\u0001"+
		"\u0000\u0000\u0000\u00dc\u00dd\u0005\b\u0000\u0000\u00dd\u00de\u0003b"+
		"1\u0000\u00de\u0015\u0001\u0000\u0000\u0000\u00df\u00e0\u0005\t\u0000"+
		"\u0000\u00e0\u00e1\u0003f3\u0000\u00e1\u0017\u0001\u0000\u0000\u0000\u00e2"+
		"\u00e4\u0005\n\u0000\u0000\u00e3\u00e5\u00034\u001a\u0000\u00e4\u00e3"+
		"\u0001\u0000\u0000\u0000\u00e4\u00e5\u0001\u0000\u0000\u0000\u00e5\u00eb"+
		"\u0001\u0000\u0000\u0000\u00e6\u00ec\u0003@ \u0000\u00e7\u00ec\u0003L"+
		"&\u0000\u00e8\u00e9\u0003t:\u0000\u00e9\u00ea\u0003x<\u0000\u00ea\u00ec"+
		"\u0001\u0000\u0000\u0000\u00eb\u00e6\u0001\u0000\u0000\u0000\u00eb\u00e7"+
		"\u0001\u0000\u0000\u0000\u00eb\u00e8\u0001\u0000\u0000\u0000\u00ec\u0019"+
		"\u0001\u0000\u0000\u0000\u00ed\u00ef\u0005\u000b\u0000\u0000\u00ee\u00f0"+
		"\u00034\u001a\u0000\u00ef\u00ee\u0001\u0000\u0000\u0000\u00ef\u00f0\u0001"+
		"\u0000\u0000\u0000\u00f0\u00f6\u0001\u0000\u0000\u0000\u00f1\u00f7\u0003"+
		"@ \u0000\u00f2\u00f7\u0003L&\u0000\u00f3\u00f4\u0003t:\u0000\u00f4\u00f5"+
		"\u0003x<\u0000\u00f5\u00f7\u0001\u0000\u0000\u0000\u00f6\u00f1\u0001\u0000"+
		"\u0000\u0000\u00f6\u00f2\u0001\u0000\u0000\u0000\u00f6\u00f3\u0001\u0000"+
		"\u0000\u0000\u00f7\u001b\u0001\u0000\u0000\u0000\u00f8\u00fb\u0005\f\u0000"+
		"\u0000\u00f9\u00fc\u00038\u001c\u0000\u00fa\u00fc\u0003:\u001d\u0000\u00fb"+
		"\u00f9\u0001\u0000\u0000\u0000\u00fb\u00fa\u0001\u0000\u0000\u0000\u00fc"+
		"\u001d\u0001\u0000\u0000\u0000\u00fd\u0100\u0005\r\u0000\u0000\u00fe\u0101"+
		"\u0003L&\u0000\u00ff\u0101\u0003~?\u0000\u0100\u00fe\u0001\u0000\u0000"+
		"\u0000\u0100\u00ff\u0001\u0000\u0000\u0000\u0101\u001f\u0001\u0000\u0000"+
		"\u0000\u0102\u0103\u0005\u000e\u0000\u0000\u0103\u0104\u0003\u0082A\u0000"+
		"\u0104!\u0001\u0000\u0000\u0000\u0105\u0106\u0005\u000f\u0000\u0000\u0106"+
		"\u0107\u0003|>\u0000\u0107#\u0001\u0000\u0000\u0000\u0108\u0109\u0005"+
		"\u0010\u0000\u0000\u0109\u010c\u0003\u0084B\u0000\u010a\u010b\u0005\u001c"+
		"\u0000\u0000\u010b\u010d\u0003\u0086C\u0000\u010c\u010a\u0001\u0000\u0000"+
		"\u0000\u010c\u010d\u0001\u0000\u0000\u0000\u010d%\u0001\u0000\u0000\u0000"+
		"\u010e\u010f\u0005\u0011\u0000\u0000\u010f\u0110\u0003\n\u0005\u0000\u0110"+
		"\'\u0001\u0000\u0000\u0000\u0111\u0112\u0005\u0012\u0000\u0000\u0112\u0113"+
		"\u0003\u0088D\u0000\u0113)\u0001\u0000\u0000\u0000\u0114\u0115\u0005\u0013"+
		"\u0000\u0000\u0115\u0120\u0005\u0006\u0000\u0000\u0116\u0118\u0005\u0013"+
		"\u0000\u0000\u0117\u0119\u0003,\u0016\u0000\u0118\u0117\u0001\u0000\u0000"+
		"\u0000\u0118\u0119\u0001\u0000\u0000\u0000\u0119\u011a\u0001\u0000\u0000"+
		"\u0000\u011a\u011d\u0005\u0005\u0000\u0000\u011b\u011e\u00038\u001c\u0000"+
		"\u011c\u011e\u0003:\u001d\u0000\u011d\u011b\u0001\u0000\u0000\u0000\u011d"+
		"\u011c\u0001\u0000\u0000\u0000\u011e\u0120\u0001\u0000\u0000\u0000\u011f"+
		"\u0114\u0001\u0000\u0000\u0000\u011f\u0116\u0001\u0000\u0000\u0000\u0120"+
		"+\u0001\u0000\u0000\u0000\u0121\u0123\u0003.\u0017\u0000\u0122\u0121\u0001"+
		"\u0000\u0000\u0000\u0123\u0124\u0001\u0000\u0000\u0000\u0124\u0122\u0001"+
		"\u0000\u0000\u0000\u0124\u0125\u0001\u0000\u0000\u0000\u0125-\u0001\u0000"+
		"\u0000\u0000\u0126\u0127\u0005\u001d\u0000\u0000\u0127/\u0001\u0000\u0000"+
		"\u0000\u0128\u0129\u0005\u0014\u0000\u0000\u0129\u012a\u0003L&\u0000\u012a"+
		"1\u0001\u0000\u0000\u0000\u012b\u012c\u0005\u0015\u0000\u0000\u012c\u012d"+
		"\u0003\u008aE\u0000\u012d3\u0001\u0000\u0000\u0000\u012e\u0130\u00036"+
		"\u001b\u0000\u012f\u012e\u0001\u0000\u0000\u0000\u0130\u0131\u0001\u0000"+
		"\u0000\u0000\u0131\u012f\u0001\u0000\u0000\u0000\u0131\u0132\u0001\u0000"+
		"\u0000\u0000\u01325\u0001\u0000\u0000\u0000\u0133\u0134\u0005\u001d\u0000"+
		"\u0000\u01347\u0001\u0000\u0000\u0000\u0135\u0136\u0003L&\u0000\u0136"+
		"9\u0001\u0000\u0000\u0000\u0137\u0138\u0003<\u001e\u0000\u0138;\u0001"+
		"\u0000\u0000\u0000\u0139\u013b\u0003>\u001f\u0000\u013a\u0139\u0001\u0000"+
		"\u0000\u0000\u013b\u013c\u0001\u0000\u0000\u0000\u013c\u013a\u0001\u0000"+
		"\u0000\u0000\u013c\u013d\u0001\u0000\u0000\u0000\u013d=\u0001\u0000\u0000"+
		"\u0000\u013e\u013f\u0007\u0000\u0000\u0000\u013f?\u0001\u0000\u0000\u0000"+
		"\u0140\u0141\u0003B!\u0000\u0141\u0143\u0005\'\u0000\u0000\u0142\u0144"+
		"\u0003F#\u0000\u0143\u0142\u0001\u0000\u0000\u0000\u0144\u0145\u0001\u0000"+
		"\u0000\u0000\u0145\u0143\u0001\u0000\u0000\u0000\u0145\u0146\u0001\u0000"+
		"\u0000\u0000\u0146A\u0001\u0000\u0000\u0000\u0147\u014b\u0005\u0017\u0000"+
		"\u0000\u0148\u014a\u0003D\"\u0000\u0149\u0148\u0001\u0000\u0000\u0000"+
		"\u014a\u014d\u0001\u0000\u0000\u0000\u014b\u0149\u0001\u0000\u0000\u0000"+
		"\u014b\u014c\u0001\u0000\u0000\u0000\u014c\u0157\u0001\u0000\u0000\u0000"+
		"\u014d\u014b\u0001\u0000\u0000\u0000\u014e\u0152\u0005\u0017\u0000\u0000"+
		"\u014f\u0151\u0003D\"\u0000\u0150\u014f\u0001\u0000\u0000\u0000\u0151"+
		"\u0154\u0001\u0000\u0000\u0000\u0152\u0150\u0001\u0000\u0000\u0000\u0152"+
		"\u0153\u0001\u0000\u0000\u0000\u0153\u0156\u0001\u0000\u0000\u0000\u0154"+
		"\u0152\u0001\u0000\u0000\u0000\u0155\u014e\u0001\u0000\u0000\u0000\u0156"+
		"\u0159\u0001\u0000\u0000\u0000\u0157\u0155\u0001\u0000\u0000\u0000\u0157"+
		"\u0158\u0001\u0000\u0000\u0000\u0158C\u0001\u0000\u0000\u0000\u0159\u0157"+
		"\u0001\u0000\u0000\u0000\u015a\u015b\u0007\u0000\u0000\u0000\u015bE\u0001"+
		"\u0000\u0000\u0000\u015c\u015d\u0003H$\u0000\u015d\u015e\u0003J%\u0000"+
		"\u015eG\u0001\u0000\u0000\u0000\u015f\u0161\u0007\u0001\u0000\u0000\u0160"+
		"\u015f\u0001\u0000\u0000\u0000\u0161\u0164\u0001\u0000\u0000\u0000\u0162"+
		"\u0160\u0001\u0000\u0000\u0000\u0162\u0163\u0001\u0000\u0000\u0000\u0163"+
		"I\u0001\u0000\u0000\u0000\u0164\u0162\u0001\u0000\u0000\u0000\u0165\u0166"+
		"\u0005%\u0000\u0000\u0166K\u0001\u0000\u0000\u0000\u0167\u0169\u0005\u0019"+
		"\u0000\u0000\u0168\u016a\u0003N\'\u0000\u0169\u0168\u0001\u0000\u0000"+
		"\u0000\u0169\u016a\u0001\u0000\u0000\u0000\u016a\u016b\u0001\u0000\u0000"+
		"\u0000\u016b\u016c\u0005\u001a\u0000\u0000\u016cM\u0001\u0000\u0000\u0000"+
		"\u016d\u0172\u0003P(\u0000\u016e\u016f\u0005\u001b\u0000\u0000\u016f\u0171"+
		"\u0003P(\u0000\u0170\u016e\u0001\u0000\u0000\u0000\u0171\u0174\u0001\u0000"+
		"\u0000\u0000\u0172\u0170\u0001\u0000\u0000\u0000\u0172\u0173\u0001\u0000"+
		"\u0000\u0000\u0173O\u0001\u0000\u0000\u0000\u0174\u0172\u0001\u0000\u0000"+
		"\u0000\u0175\u0176\u0005\u001f\u0000\u0000\u0176Q\u0001\u0000\u0000\u0000"+
		"\u0177\u0178\u0003\u008aE\u0000\u0178S\u0001\u0000\u0000\u0000\u0179\u017a"+
		"\u0005%\u0000\u0000\u017aU\u0001\u0000\u0000\u0000\u017b\u017d\u0003X"+
		",\u0000\u017c\u017b\u0001\u0000\u0000\u0000\u017d\u017e\u0001\u0000\u0000"+
		"\u0000\u017e\u017c\u0001\u0000\u0000\u0000\u017e\u017f\u0001\u0000\u0000"+
		"\u0000\u017fW\u0001\u0000\u0000\u0000\u0180\u0181\u0003Z-\u0000\u0181"+
		"\u0182\u0005\u001c\u0000\u0000\u0182\u0183\u0003\\.\u0000\u0183\u0188"+
		"\u0001\u0000\u0000\u0000\u0184\u0185\u0003Z-\u0000\u0185\u0186\u0003^"+
		"/\u0000\u0186\u0188\u0001\u0000\u0000\u0000\u0187\u0180\u0001\u0000\u0000"+
		"\u0000\u0187\u0184\u0001\u0000\u0000\u0000\u0188Y\u0001\u0000\u0000\u0000"+
		"\u0189\u018a\u0007\u0002\u0000\u0000\u018a[\u0001\u0000\u0000\u0000\u018b"+
		"\u018c\u0007\u0002\u0000\u0000\u018c]\u0001\u0000\u0000\u0000\u018d\u018f"+
		"\u0003`0\u0000\u018e\u018d\u0001\u0000\u0000\u0000\u018f\u0190\u0001\u0000"+
		"\u0000\u0000\u0190\u018e\u0001\u0000\u0000\u0000\u0190\u0191\u0001\u0000"+
		"\u0000\u0000\u0191_\u0001\u0000\u0000\u0000\u0192\u0193\u0007\u0000\u0000"+
		"\u0000\u0193a\u0001\u0000\u0000\u0000\u0194\u0196\u0003d2\u0000\u0195"+
		"\u0194\u0001\u0000\u0000\u0000\u0196\u0197\u0001\u0000\u0000\u0000\u0197"+
		"\u0195\u0001\u0000\u0000\u0000\u0197\u0198\u0001\u0000\u0000\u0000\u0198"+
		"c\u0001\u0000\u0000\u0000\u0199\u019a\u0007\u0003\u0000\u0000\u019ae\u0001"+
		"\u0000\u0000\u0000\u019b\u019d\u0003h4\u0000\u019c\u019b\u0001\u0000\u0000"+
		"\u0000\u019d\u019e\u0001\u0000\u0000\u0000\u019e\u019c\u0001\u0000\u0000"+
		"\u0000\u019e\u019f\u0001\u0000\u0000\u0000\u019fg\u0001\u0000\u0000\u0000"+
		"\u01a0\u01a1\u0003j5\u0000\u01a1\u01a2\u0005\u001c\u0000\u0000\u01a2\u01a3"+
		"\u0003l6\u0000\u01a3\u01a8\u0001\u0000\u0000\u0000\u01a4\u01a5\u0003j"+
		"5\u0000\u01a5\u01a6\u0003n7\u0000\u01a6\u01a8\u0001\u0000\u0000\u0000"+
		"\u01a7\u01a0\u0001\u0000\u0000\u0000\u01a7\u01a4\u0001\u0000\u0000\u0000"+
		"\u01a8i\u0001\u0000\u0000\u0000\u01a9\u01aa\u0005%\u0000\u0000\u01aak"+
		"\u0001\u0000\u0000\u0000\u01ab\u01ac\u0003p8\u0000\u01acm\u0001\u0000"+
		"\u0000\u0000\u01ad\u01ae\u0003\u008aE\u0000\u01aeo\u0001\u0000\u0000\u0000"+
		"\u01af\u01b1\u0003r9\u0000\u01b0\u01af\u0001\u0000\u0000\u0000\u01b1\u01b2"+
		"\u0001\u0000\u0000\u0000\u01b2\u01b0\u0001\u0000\u0000\u0000\u01b2\u01b3"+
		"\u0001\u0000\u0000\u0000\u01b3q\u0001\u0000\u0000\u0000\u01b4\u01b5\u0007"+
		"\u0004\u0000\u0000\u01b5s\u0001\u0000\u0000\u0000\u01b6\u01b8\u0003v;"+
		"\u0000\u01b7\u01b6\u0001\u0000\u0000\u0000\u01b8\u01b9\u0001\u0000\u0000"+
		"\u0000\u01b9\u01b7\u0001\u0000\u0000\u0000\u01b9\u01ba\u0001\u0000\u0000"+
		"\u0000\u01bau\u0001\u0000\u0000\u0000\u01bb\u01bc\u0007\u0004\u0000\u0000"+
		"\u01bcw\u0001\u0000\u0000\u0000\u01bd\u01be\u0003z=\u0000\u01bey\u0001"+
		"\u0000\u0000\u0000\u01bf\u01c0\u0007\u0004\u0000\u0000\u01c0{\u0001\u0000"+
		"\u0000\u0000\u01c1\u01c2\u0003\u008aE\u0000\u01c2}\u0001\u0000\u0000\u0000"+
		"\u01c3\u01c5\u0003\u0080@\u0000\u01c4\u01c3\u0001\u0000\u0000\u0000\u01c5"+
		"\u01c6\u0001\u0000\u0000\u0000\u01c6\u01c4\u0001\u0000\u0000\u0000\u01c6"+
		"\u01c7\u0001\u0000\u0000\u0000\u01c7\u007f\u0001\u0000\u0000\u0000\u01c8"+
		"\u01c9\u0007\u0004\u0000\u0000\u01c9\u0081\u0001\u0000\u0000\u0000\u01ca"+
		"\u01cb\u0003\u008aE\u0000\u01cb\u0083\u0001\u0000\u0000\u0000\u01cc\u01cd"+
		"\u0005%\u0000\u0000\u01cd\u0085\u0001\u0000\u0000\u0000\u01ce\u01cf\u0003"+
		"\u008aE\u0000\u01cf\u0087\u0001\u0000\u0000\u0000\u01d0\u01d1\u0005%\u0000"+
		"\u0000\u01d1\u0089\u0001\u0000\u0000\u0000\u01d2\u01d4\u0003\u008cF\u0000"+
		"\u01d3\u01d2\u0001\u0000\u0000\u0000\u01d4\u01d5\u0001\u0000\u0000\u0000"+
		"\u01d5\u01d3\u0001\u0000\u0000\u0000\u01d5\u01d6\u0001\u0000\u0000\u0000"+
		"\u01d6\u008b\u0001\u0000\u0000\u0000\u01d7\u01d8\u0007\u0000\u0000\u0000"+
		"\u01d8\u008d\u0001\u0000\u0000\u0000)\u0091\u0098\u00a1\u00a8\u00bc\u00c0"+
		"\u00c4\u00c9\u00cd\u00d2\u00d7\u00e4\u00eb\u00ef\u00f6\u00fb\u0100\u010c"+
		"\u0118\u011d\u011f\u0124\u0131\u013c\u0145\u014b\u0152\u0157\u0162\u0169"+
		"\u0172\u017e\u0187\u0190\u0197\u019e\u01a7\u01b2\u01b9\u01c6\u01d5";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}