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
		PARSER_DIRECTIVE=1, COMMENT=2, FROM=3, RUN=4, CMD=5, LABEL=6, EXPOSE=7, 
		ENV=8, ADD=9, COPY=10, ENTRYPOINT=11, VOLUME=12, USER=13, WORKDIR=14, 
		ARG=15, ONBUILD=16, STOPSIGNAL=17, HEALTHCHECK=18, SHELL=19, MAINTAINER=20, 
		AS=21, HEREDOC_START=22, LINE_CONTINUATION=23, LBRACKET=24, RBRACKET=25, 
		EQUALS=26, DASH_DASH=27, DOUBLE_QUOTED_STRING=28, SINGLE_QUOTED_STRING=29, 
		ENV_VAR=30, UNQUOTED_TEXT=31, WS=32, NEWLINE=33, JSON_COMMA=34, JSON_RBRACKET=35, 
		JSON_STRING=36, JSON_WS=37, HP_WS=38, HP_COMMENT=39, HP_LINE_COMMENT=40, 
		HTemplateLiteral=41, H_NEWLINE=42;
	public static final int
		RULE_dockerfile = 0, RULE_parserDirective = 1, RULE_globalArgs = 2, RULE_stage = 3, 
		RULE_stageInstruction = 4, RULE_instruction = 5, RULE_fromInstruction = 6, 
		RULE_runInstruction = 7, RULE_cmdInstruction = 8, RULE_labelInstruction = 9, 
		RULE_exposeInstruction = 10, RULE_envInstruction = 11, RULE_addInstruction = 12, 
		RULE_copyInstruction = 13, RULE_entrypointInstruction = 14, RULE_volumeInstruction = 15, 
		RULE_userInstruction = 16, RULE_workdirInstruction = 17, RULE_argInstruction = 18, 
		RULE_onbuildInstruction = 19, RULE_stopsignalInstruction = 20, RULE_healthcheckInstruction = 21, 
		RULE_shellInstruction = 22, RULE_maintainerInstruction = 23, RULE_flags = 24, 
		RULE_flag = 25, RULE_flagName = 26, RULE_flagValue = 27, RULE_flagValueElement = 28, 
		RULE_execForm = 29, RULE_shellForm = 30, RULE_heredoc = 31, RULE_heredocContent = 32, 
		RULE_heredocEnd = 33, RULE_jsonArray = 34, RULE_jsonArrayElements = 35, 
		RULE_jsonString = 36, RULE_imageName = 37, RULE_stageName = 38, RULE_labelPairs = 39, 
		RULE_labelPair = 40, RULE_labelKey = 41, RULE_labelValue = 42, RULE_labelOldValue = 43, 
		RULE_labelOldValueElement = 44, RULE_portList = 45, RULE_port = 46, RULE_envPairs = 47, 
		RULE_envPair = 48, RULE_envKey = 49, RULE_envValueEquals = 50, RULE_envValueSpace = 51, 
		RULE_envTextEquals = 52, RULE_envTextElementEquals = 53, RULE_sourceList = 54, 
		RULE_source = 55, RULE_destination = 56, RULE_path = 57, RULE_pathList = 58, 
		RULE_volumePath = 59, RULE_userSpec = 60, RULE_argName = 61, RULE_argValue = 62, 
		RULE_signal = 63, RULE_text = 64, RULE_textElement = 65;
	private static String[] makeRuleNames() {
		return new String[] {
			"dockerfile", "parserDirective", "globalArgs", "stage", "stageInstruction", 
			"instruction", "fromInstruction", "runInstruction", "cmdInstruction", 
			"labelInstruction", "exposeInstruction", "envInstruction", "addInstruction", 
			"copyInstruction", "entrypointInstruction", "volumeInstruction", "userInstruction", 
			"workdirInstruction", "argInstruction", "onbuildInstruction", "stopsignalInstruction", 
			"healthcheckInstruction", "shellInstruction", "maintainerInstruction", 
			"flags", "flag", "flagName", "flagValue", "flagValueElement", "execForm", 
			"shellForm", "heredoc", "heredocContent", "heredocEnd", "jsonArray", 
			"jsonArrayElements", "jsonString", "imageName", "stageName", "labelPairs", 
			"labelPair", "labelKey", "labelValue", "labelOldValue", "labelOldValueElement", 
			"portList", "port", "envPairs", "envPair", "envKey", "envValueEquals", 
			"envValueSpace", "envTextEquals", "envTextElementEquals", "sourceList", 
			"source", "destination", "path", "pathList", "volumePath", "userSpec", 
			"argName", "argValue", "signal", "text", "textElement"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, "'FROM'", "'RUN'", "'CMD'", "'LABEL'", "'EXPOSE'", 
			"'ENV'", "'ADD'", "'COPY'", "'ENTRYPOINT'", "'VOLUME'", "'USER'", "'WORKDIR'", 
			"'ARG'", "'ONBUILD'", "'STOPSIGNAL'", "'HEALTHCHECK'", "'SHELL'", "'MAINTAINER'", 
			"'AS'", null, null, "'['", null, "'='", "'--'", null, null, null, null, 
			null, null, "','", null, null, null, null, null, null, null, "'\\n'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "PARSER_DIRECTIVE", "COMMENT", "FROM", "RUN", "CMD", "LABEL", "EXPOSE", 
			"ENV", "ADD", "COPY", "ENTRYPOINT", "VOLUME", "USER", "WORKDIR", "ARG", 
			"ONBUILD", "STOPSIGNAL", "HEALTHCHECK", "SHELL", "MAINTAINER", "AS", 
			"HEREDOC_START", "LINE_CONTINUATION", "LBRACKET", "RBRACKET", "EQUALS", 
			"DASH_DASH", "DOUBLE_QUOTED_STRING", "SINGLE_QUOTED_STRING", "ENV_VAR", 
			"UNQUOTED_TEXT", "WS", "NEWLINE", "JSON_COMMA", "JSON_RBRACKET", "JSON_STRING", 
			"JSON_WS", "HP_WS", "HP_COMMENT", "HP_LINE_COMMENT", "HTemplateLiteral", 
			"H_NEWLINE"
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
			setState(135);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==PARSER_DIRECTIVE) {
				{
				{
				setState(132);
				parserDirective();
				}
				}
				setState(137);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(138);
			globalArgs();
			setState(140); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(139);
				stage();
				}
				}
				setState(142); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==FROM );
			setState(144);
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
			setState(146);
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
			setState(151);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ARG) {
				{
				{
				setState(148);
				argInstruction();
				}
				}
				setState(153);
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
			setState(154);
			fromInstruction();
			setState(158);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 2097136L) != 0)) {
				{
				{
				setState(155);
				stageInstruction();
				}
				}
				setState(160);
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
			setState(178);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case RUN:
				enterOuterAlt(_localctx, 1);
				{
				setState(161);
				runInstruction();
				}
				break;
			case CMD:
				enterOuterAlt(_localctx, 2);
				{
				setState(162);
				cmdInstruction();
				}
				break;
			case LABEL:
				enterOuterAlt(_localctx, 3);
				{
				setState(163);
				labelInstruction();
				}
				break;
			case EXPOSE:
				enterOuterAlt(_localctx, 4);
				{
				setState(164);
				exposeInstruction();
				}
				break;
			case ENV:
				enterOuterAlt(_localctx, 5);
				{
				setState(165);
				envInstruction();
				}
				break;
			case ADD:
				enterOuterAlt(_localctx, 6);
				{
				setState(166);
				addInstruction();
				}
				break;
			case COPY:
				enterOuterAlt(_localctx, 7);
				{
				setState(167);
				copyInstruction();
				}
				break;
			case ENTRYPOINT:
				enterOuterAlt(_localctx, 8);
				{
				setState(168);
				entrypointInstruction();
				}
				break;
			case VOLUME:
				enterOuterAlt(_localctx, 9);
				{
				setState(169);
				volumeInstruction();
				}
				break;
			case USER:
				enterOuterAlt(_localctx, 10);
				{
				setState(170);
				userInstruction();
				}
				break;
			case WORKDIR:
				enterOuterAlt(_localctx, 11);
				{
				setState(171);
				workdirInstruction();
				}
				break;
			case ARG:
				enterOuterAlt(_localctx, 12);
				{
				setState(172);
				argInstruction();
				}
				break;
			case ONBUILD:
				enterOuterAlt(_localctx, 13);
				{
				setState(173);
				onbuildInstruction();
				}
				break;
			case STOPSIGNAL:
				enterOuterAlt(_localctx, 14);
				{
				setState(174);
				stopsignalInstruction();
				}
				break;
			case HEALTHCHECK:
				enterOuterAlt(_localctx, 15);
				{
				setState(175);
				healthcheckInstruction();
				}
				break;
			case SHELL:
				enterOuterAlt(_localctx, 16);
				{
				setState(176);
				shellInstruction();
				}
				break;
			case MAINTAINER:
				enterOuterAlt(_localctx, 17);
				{
				setState(177);
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
			setState(182);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case FROM:
				enterOuterAlt(_localctx, 1);
				{
				setState(180);
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
				setState(181);
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
			setState(184);
			match(FROM);
			setState(186);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,6,_ctx) ) {
			case 1:
				{
				setState(185);
				flags();
				}
				break;
			}
			setState(188);
			imageName();
			setState(191);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==AS) {
				{
				setState(189);
				match(AS);
				setState(190);
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
			setState(193);
			match(RUN);
			setState(195);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,8,_ctx) ) {
			case 1:
				{
				setState(194);
				flags();
				}
				break;
			}
			setState(200);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LBRACKET:
				{
				setState(197);
				execForm();
				}
				break;
			case EQUALS:
			case DASH_DASH:
			case DOUBLE_QUOTED_STRING:
			case SINGLE_QUOTED_STRING:
			case ENV_VAR:
			case UNQUOTED_TEXT:
				{
				setState(198);
				shellForm();
				}
				break;
			case HEREDOC_START:
				{
				setState(199);
				heredoc();
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
			setState(202);
			match(CMD);
			setState(205);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LBRACKET:
				{
				setState(203);
				execForm();
				}
				break;
			case EQUALS:
			case DASH_DASH:
			case DOUBLE_QUOTED_STRING:
			case SINGLE_QUOTED_STRING:
			case ENV_VAR:
			case UNQUOTED_TEXT:
				{
				setState(204);
				shellForm();
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
			setState(207);
			match(LABEL);
			setState(208);
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
			setState(210);
			match(EXPOSE);
			setState(211);
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
			setState(213);
			match(ENV);
			setState(214);
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
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(216);
			match(ADD);
			setState(218);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
			case 1:
				{
				setState(217);
				flags();
				}
				break;
			}
			setState(224);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case HEREDOC_START:
				{
				setState(220);
				heredoc();
				}
				break;
			case EQUALS:
			case DASH_DASH:
			case DOUBLE_QUOTED_STRING:
			case SINGLE_QUOTED_STRING:
			case ENV_VAR:
			case UNQUOTED_TEXT:
				{
				setState(221);
				sourceList();
				setState(222);
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
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(226);
			match(COPY);
			setState(228);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,13,_ctx) ) {
			case 1:
				{
				setState(227);
				flags();
				}
				break;
			}
			setState(234);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case HEREDOC_START:
				{
				setState(230);
				heredoc();
				}
				break;
			case EQUALS:
			case DASH_DASH:
			case DOUBLE_QUOTED_STRING:
			case SINGLE_QUOTED_STRING:
			case ENV_VAR:
			case UNQUOTED_TEXT:
				{
				setState(231);
				sourceList();
				setState(232);
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
			setState(236);
			match(ENTRYPOINT);
			setState(239);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LBRACKET:
				{
				setState(237);
				execForm();
				}
				break;
			case EQUALS:
			case DASH_DASH:
			case DOUBLE_QUOTED_STRING:
			case SINGLE_QUOTED_STRING:
			case ENV_VAR:
			case UNQUOTED_TEXT:
				{
				setState(238);
				shellForm();
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
			setState(241);
			match(VOLUME);
			setState(244);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LBRACKET:
				{
				setState(242);
				jsonArray();
				}
				break;
			case DOUBLE_QUOTED_STRING:
			case SINGLE_QUOTED_STRING:
			case UNQUOTED_TEXT:
				{
				setState(243);
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
			setState(246);
			match(USER);
			setState(247);
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
			setState(249);
			match(WORKDIR);
			setState(250);
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
			setState(252);
			match(ARG);
			setState(253);
			argName();
			setState(256);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==EQUALS) {
				{
				setState(254);
				match(EQUALS);
				setState(255);
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
			setState(258);
			match(ONBUILD);
			setState(259);
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
			setState(261);
			match(STOPSIGNAL);
			setState(262);
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
		public TerminalNode UNQUOTED_TEXT() { return getToken(DockerParser.UNQUOTED_TEXT, 0); }
		public CmdInstructionContext cmdInstruction() {
			return getRuleContext(CmdInstructionContext.class,0);
		}
		public FlagsContext flags() {
			return getRuleContext(FlagsContext.class,0);
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
			enterOuterAlt(_localctx, 1);
			{
			setState(264);
			match(HEALTHCHECK);
			setState(270);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case UNQUOTED_TEXT:
				{
				setState(265);
				match(UNQUOTED_TEXT);
				}
				break;
			case CMD:
			case DASH_DASH:
				{
				setState(267);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==DASH_DASH) {
					{
					setState(266);
					flags();
					}
				}

				setState(269);
				cmdInstruction();
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
		enterRule(_localctx, 44, RULE_shellInstruction);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(272);
			match(SHELL);
			setState(273);
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
		enterRule(_localctx, 46, RULE_maintainerInstruction);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(275);
			match(MAINTAINER);
			setState(276);
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
		enterRule(_localctx, 48, RULE_flags);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(279); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(278);
					flag();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(281); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,20,_ctx);
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
		public TerminalNode DASH_DASH() { return getToken(DockerParser.DASH_DASH, 0); }
		public FlagNameContext flagName() {
			return getRuleContext(FlagNameContext.class,0);
		}
		public TerminalNode EQUALS() { return getToken(DockerParser.EQUALS, 0); }
		public FlagValueContext flagValue() {
			return getRuleContext(FlagValueContext.class,0);
		}
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
		enterRule(_localctx, 50, RULE_flag);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(283);
			match(DASH_DASH);
			setState(284);
			flagName();
			setState(287);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,21,_ctx) ) {
			case 1:
				{
				setState(285);
				match(EQUALS);
				setState(286);
				flagValue();
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
	public static class FlagNameContext extends ParserRuleContext {
		public TerminalNode UNQUOTED_TEXT() { return getToken(DockerParser.UNQUOTED_TEXT, 0); }
		public TerminalNode FROM() { return getToken(DockerParser.FROM, 0); }
		public TerminalNode AS() { return getToken(DockerParser.AS, 0); }
		public FlagNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_flagName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterFlagName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitFlagName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitFlagName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FlagNameContext flagName() throws RecognitionException {
		FlagNameContext _localctx = new FlagNameContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_flagName);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(289);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 2149580808L) != 0)) ) {
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
	public static class FlagValueContext extends ParserRuleContext {
		public List<FlagValueElementContext> flagValueElement() {
			return getRuleContexts(FlagValueElementContext.class);
		}
		public FlagValueElementContext flagValueElement(int i) {
			return getRuleContext(FlagValueElementContext.class,i);
		}
		public FlagValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_flagValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterFlagValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitFlagValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitFlagValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FlagValueContext flagValue() throws RecognitionException {
		FlagValueContext _localctx = new FlagValueContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_flagValue);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(292); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(291);
					flagValueElement();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(294); 
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
	public static class FlagValueElementContext extends ParserRuleContext {
		public TerminalNode UNQUOTED_TEXT() { return getToken(DockerParser.UNQUOTED_TEXT, 0); }
		public TerminalNode EQUALS() { return getToken(DockerParser.EQUALS, 0); }
		public TerminalNode DOUBLE_QUOTED_STRING() { return getToken(DockerParser.DOUBLE_QUOTED_STRING, 0); }
		public TerminalNode SINGLE_QUOTED_STRING() { return getToken(DockerParser.SINGLE_QUOTED_STRING, 0); }
		public FlagValueElementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_flagValueElement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterFlagValueElement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitFlagValueElement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitFlagValueElement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FlagValueElementContext flagValueElement() throws RecognitionException {
		FlagValueElementContext _localctx = new FlagValueElementContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_flagValueElement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(296);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 3019898880L) != 0)) ) {
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
		enterRule(_localctx, 58, RULE_execForm);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(298);
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
		public TextContext text() {
			return getRuleContext(TextContext.class,0);
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
		enterRule(_localctx, 60, RULE_shellForm);
		try {
			enterOuterAlt(_localctx, 1);
			{
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
	public static class HeredocContext extends ParserRuleContext {
		public TerminalNode HEREDOC_START() { return getToken(DockerParser.HEREDOC_START, 0); }
		public TerminalNode NEWLINE() { return getToken(DockerParser.NEWLINE, 0); }
		public HeredocContentContext heredocContent() {
			return getRuleContext(HeredocContentContext.class,0);
		}
		public HeredocEndContext heredocEnd() {
			return getRuleContext(HeredocEndContext.class,0);
		}
		public PathContext path() {
			return getRuleContext(PathContext.class,0);
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
		enterRule(_localctx, 62, RULE_heredoc);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(302);
			match(HEREDOC_START);
			setState(304);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 4227858432L) != 0)) {
				{
				setState(303);
				path();
				}
			}

			setState(306);
			match(NEWLINE);
			setState(307);
			heredocContent();
			setState(308);
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
		public List<TextContext> text() {
			return getRuleContexts(TextContext.class);
		}
		public TextContext text(int i) {
			return getRuleContext(TextContext.class,i);
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
		enterRule(_localctx, 64, RULE_heredocContent);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(314);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,25,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					setState(312);
					_errHandler.sync(this);
					switch (_input.LA(1)) {
					case NEWLINE:
						{
						setState(310);
						match(NEWLINE);
						}
						break;
					case EQUALS:
					case DASH_DASH:
					case DOUBLE_QUOTED_STRING:
					case SINGLE_QUOTED_STRING:
					case ENV_VAR:
					case UNQUOTED_TEXT:
						{
						setState(311);
						text();
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					} 
				}
				setState(316);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,25,_ctx);
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
		enterRule(_localctx, 66, RULE_heredocEnd);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(317);
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
		public TerminalNode JSON_RBRACKET() { return getToken(DockerParser.JSON_RBRACKET, 0); }
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
		enterRule(_localctx, 68, RULE_jsonArray);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(319);
			match(LBRACKET);
			setState(321);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==JSON_STRING) {
				{
				setState(320);
				jsonArrayElements();
				}
			}

			setState(323);
			match(JSON_RBRACKET);
			}
		}
		catch (RecognitionException re) {
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
		public List<TerminalNode> JSON_COMMA() { return getTokens(DockerParser.JSON_COMMA); }
		public TerminalNode JSON_COMMA(int i) {
			return getToken(DockerParser.JSON_COMMA, i);
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
		enterRule(_localctx, 70, RULE_jsonArrayElements);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(325);
			jsonString();
			setState(330);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==JSON_COMMA) {
				{
				{
				setState(326);
				match(JSON_COMMA);
				setState(327);
				jsonString();
				}
				}
				setState(332);
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
		public TerminalNode JSON_STRING() { return getToken(DockerParser.JSON_STRING, 0); }
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
		enterRule(_localctx, 72, RULE_jsonString);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(333);
			match(JSON_STRING);
			}
		}
		catch (RecognitionException re) {
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
		enterRule(_localctx, 74, RULE_imageName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(335);
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
		enterRule(_localctx, 76, RULE_stageName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(337);
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
		enterRule(_localctx, 78, RULE_labelPairs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(340); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(339);
				labelPair();
				}
				}
				setState(342); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & 2952790016L) != 0) );
			}
		}
		catch (RecognitionException re) {
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
		enterRule(_localctx, 80, RULE_labelPair);
		try {
			setState(351);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,29,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(344);
				labelKey();
				setState(345);
				match(EQUALS);
				setState(346);
				labelValue();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(348);
				labelKey();
				setState(349);
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
		enterRule(_localctx, 82, RULE_labelKey);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(353);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 2952790016L) != 0)) ) {
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
		enterRule(_localctx, 84, RULE_labelValue);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(355);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 2952790016L) != 0)) ) {
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
		enterRule(_localctx, 86, RULE_labelOldValue);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(358); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(357);
					labelOldValueElement();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(360); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,30,_ctx);
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
		public TerminalNode EQUALS() { return getToken(DockerParser.EQUALS, 0); }
		public TerminalNode DASH_DASH() { return getToken(DockerParser.DASH_DASH, 0); }
		public TerminalNode FROM() { return getToken(DockerParser.FROM, 0); }
		public TerminalNode RUN() { return getToken(DockerParser.RUN, 0); }
		public TerminalNode CMD() { return getToken(DockerParser.CMD, 0); }
		public TerminalNode LABEL() { return getToken(DockerParser.LABEL, 0); }
		public TerminalNode EXPOSE() { return getToken(DockerParser.EXPOSE, 0); }
		public TerminalNode ENV() { return getToken(DockerParser.ENV, 0); }
		public TerminalNode ADD() { return getToken(DockerParser.ADD, 0); }
		public TerminalNode COPY() { return getToken(DockerParser.COPY, 0); }
		public TerminalNode ENTRYPOINT() { return getToken(DockerParser.ENTRYPOINT, 0); }
		public TerminalNode VOLUME() { return getToken(DockerParser.VOLUME, 0); }
		public TerminalNode USER() { return getToken(DockerParser.USER, 0); }
		public TerminalNode WORKDIR() { return getToken(DockerParser.WORKDIR, 0); }
		public TerminalNode ARG() { return getToken(DockerParser.ARG, 0); }
		public TerminalNode ONBUILD() { return getToken(DockerParser.ONBUILD, 0); }
		public TerminalNode STOPSIGNAL() { return getToken(DockerParser.STOPSIGNAL, 0); }
		public TerminalNode HEALTHCHECK() { return getToken(DockerParser.HEALTHCHECK, 0); }
		public TerminalNode SHELL() { return getToken(DockerParser.SHELL, 0); }
		public TerminalNode MAINTAINER() { return getToken(DockerParser.MAINTAINER, 0); }
		public TerminalNode AS() { return getToken(DockerParser.AS, 0); }
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
		enterRule(_localctx, 88, RULE_labelOldValueElement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(362);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 4232052728L) != 0)) ) {
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
		enterRule(_localctx, 90, RULE_portList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(365); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(364);
				port();
				}
				}
				setState(367); 
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
	public static class PortContext extends ParserRuleContext {
		public TerminalNode UNQUOTED_TEXT() { return getToken(DockerParser.UNQUOTED_TEXT, 0); }
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
		enterRule(_localctx, 92, RULE_port);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(369);
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
		enterRule(_localctx, 94, RULE_envPairs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(372); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(371);
				envPair();
				}
				}
				setState(374); 
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
		enterRule(_localctx, 96, RULE_envPair);
		try {
			setState(383);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,33,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(376);
				envKey();
				setState(377);
				match(EQUALS);
				setState(378);
				envValueEquals();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(380);
				envKey();
				setState(381);
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
		enterRule(_localctx, 98, RULE_envKey);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(385);
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
		enterRule(_localctx, 100, RULE_envValueEquals);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(387);
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
		enterRule(_localctx, 102, RULE_envValueSpace);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(389);
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
		enterRule(_localctx, 104, RULE_envTextEquals);
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
					envTextElementEquals();
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
	public static class EnvTextElementEqualsContext extends ParserRuleContext {
		public TerminalNode UNQUOTED_TEXT() { return getToken(DockerParser.UNQUOTED_TEXT, 0); }
		public TerminalNode DOUBLE_QUOTED_STRING() { return getToken(DockerParser.DOUBLE_QUOTED_STRING, 0); }
		public TerminalNode SINGLE_QUOTED_STRING() { return getToken(DockerParser.SINGLE_QUOTED_STRING, 0); }
		public TerminalNode ENV_VAR() { return getToken(DockerParser.ENV_VAR, 0); }
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
		enterRule(_localctx, 106, RULE_envTextElementEquals);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(396);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 4026531840L) != 0)) ) {
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
		public List<SourceContext> source() {
			return getRuleContexts(SourceContext.class);
		}
		public SourceContext source(int i) {
			return getRuleContext(SourceContext.class,i);
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
		enterRule(_localctx, 108, RULE_sourceList);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(399); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(398);
					source();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(401); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,35,_ctx);
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
	public static class SourceContext extends ParserRuleContext {
		public PathContext path() {
			return getRuleContext(PathContext.class,0);
		}
		public SourceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_source; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).enterSource(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DockerParserListener ) ((DockerParserListener)listener).exitSource(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof DockerParserVisitor ) return ((DockerParserVisitor<? extends T>)visitor).visitSource(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SourceContext source() throws RecognitionException {
		SourceContext _localctx = new SourceContext(_ctx, getState());
		enterRule(_localctx, 110, RULE_source);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(403);
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
	public static class DestinationContext extends ParserRuleContext {
		public PathContext path() {
			return getRuleContext(PathContext.class,0);
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
		enterRule(_localctx, 112, RULE_destination);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(405);
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
		enterRule(_localctx, 114, RULE_path);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(407);
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
		enterRule(_localctx, 116, RULE_pathList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(410); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(409);
				volumePath();
				}
				}
				setState(412); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & 2952790016L) != 0) );
			}
		}
		catch (RecognitionException re) {
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
		enterRule(_localctx, 118, RULE_volumePath);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(414);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 2952790016L) != 0)) ) {
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
		enterRule(_localctx, 120, RULE_userSpec);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(416);
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
		enterRule(_localctx, 122, RULE_argName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(418);
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
		enterRule(_localctx, 124, RULE_argValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(420);
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
		enterRule(_localctx, 126, RULE_signal);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(422);
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
		enterRule(_localctx, 128, RULE_text);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(425); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(424);
					textElement();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(427); 
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
	public static class TextElementContext extends ParserRuleContext {
		public TerminalNode UNQUOTED_TEXT() { return getToken(DockerParser.UNQUOTED_TEXT, 0); }
		public TerminalNode DOUBLE_QUOTED_STRING() { return getToken(DockerParser.DOUBLE_QUOTED_STRING, 0); }
		public TerminalNode SINGLE_QUOTED_STRING() { return getToken(DockerParser.SINGLE_QUOTED_STRING, 0); }
		public TerminalNode ENV_VAR() { return getToken(DockerParser.ENV_VAR, 0); }
		public TerminalNode EQUALS() { return getToken(DockerParser.EQUALS, 0); }
		public TerminalNode DASH_DASH() { return getToken(DockerParser.DASH_DASH, 0); }
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
		enterRule(_localctx, 130, RULE_textElement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(429);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 4227858432L) != 0)) ) {
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
		"\u0004\u0001*\u01b0\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
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
		"A\u0007A\u0001\u0000\u0005\u0000\u0086\b\u0000\n\u0000\f\u0000\u0089\t"+
		"\u0000\u0001\u0000\u0001\u0000\u0004\u0000\u008d\b\u0000\u000b\u0000\f"+
		"\u0000\u008e\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0002"+
		"\u0005\u0002\u0096\b\u0002\n\u0002\f\u0002\u0099\t\u0002\u0001\u0003\u0001"+
		"\u0003\u0005\u0003\u009d\b\u0003\n\u0003\f\u0003\u00a0\t\u0003\u0001\u0004"+
		"\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004"+
		"\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004"+
		"\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0003\u0004\u00b3\b\u0004"+
		"\u0001\u0005\u0001\u0005\u0003\u0005\u00b7\b\u0005\u0001\u0006\u0001\u0006"+
		"\u0003\u0006\u00bb\b\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0003\u0006"+
		"\u00c0\b\u0006\u0001\u0007\u0001\u0007\u0003\u0007\u00c4\b\u0007\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0003\u0007\u00c9\b\u0007\u0001\b\u0001"+
		"\b\u0001\b\u0003\b\u00ce\b\b\u0001\t\u0001\t\u0001\t\u0001\n\u0001\n\u0001"+
		"\n\u0001\u000b\u0001\u000b\u0001\u000b\u0001\f\u0001\f\u0003\f\u00db\b"+
		"\f\u0001\f\u0001\f\u0001\f\u0001\f\u0003\f\u00e1\b\f\u0001\r\u0001\r\u0003"+
		"\r\u00e5\b\r\u0001\r\u0001\r\u0001\r\u0001\r\u0003\r\u00eb\b\r\u0001\u000e"+
		"\u0001\u000e\u0001\u000e\u0003\u000e\u00f0\b\u000e\u0001\u000f\u0001\u000f"+
		"\u0001\u000f\u0003\u000f\u00f5\b\u000f\u0001\u0010\u0001\u0010\u0001\u0010"+
		"\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0012\u0001\u0012\u0001\u0012"+
		"\u0001\u0012\u0003\u0012\u0101\b\u0012\u0001\u0013\u0001\u0013\u0001\u0013"+
		"\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0015\u0001\u0015\u0001\u0015"+
		"\u0003\u0015\u010c\b\u0015\u0001\u0015\u0003\u0015\u010f\b\u0015\u0001"+
		"\u0016\u0001\u0016\u0001\u0016\u0001\u0017\u0001\u0017\u0001\u0017\u0001"+
		"\u0018\u0004\u0018\u0118\b\u0018\u000b\u0018\f\u0018\u0119\u0001\u0019"+
		"\u0001\u0019\u0001\u0019\u0001\u0019\u0003\u0019\u0120\b\u0019\u0001\u001a"+
		"\u0001\u001a\u0001\u001b\u0004\u001b\u0125\b\u001b\u000b\u001b\f\u001b"+
		"\u0126\u0001\u001c\u0001\u001c\u0001\u001d\u0001\u001d\u0001\u001e\u0001"+
		"\u001e\u0001\u001f\u0001\u001f\u0003\u001f\u0131\b\u001f\u0001\u001f\u0001"+
		"\u001f\u0001\u001f\u0001\u001f\u0001 \u0001 \u0005 \u0139\b \n \f \u013c"+
		"\t \u0001!\u0001!\u0001\"\u0001\"\u0003\"\u0142\b\"\u0001\"\u0001\"\u0001"+
		"#\u0001#\u0001#\u0005#\u0149\b#\n#\f#\u014c\t#\u0001$\u0001$\u0001%\u0001"+
		"%\u0001&\u0001&\u0001\'\u0004\'\u0155\b\'\u000b\'\f\'\u0156\u0001(\u0001"+
		"(\u0001(\u0001(\u0001(\u0001(\u0001(\u0003(\u0160\b(\u0001)\u0001)\u0001"+
		"*\u0001*\u0001+\u0004+\u0167\b+\u000b+\f+\u0168\u0001,\u0001,\u0001-\u0004"+
		"-\u016e\b-\u000b-\f-\u016f\u0001.\u0001.\u0001/\u0004/\u0175\b/\u000b"+
		"/\f/\u0176\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00030\u0180"+
		"\b0\u00011\u00011\u00012\u00012\u00013\u00013\u00014\u00044\u0189\b4\u000b"+
		"4\f4\u018a\u00015\u00015\u00016\u00046\u0190\b6\u000b6\f6\u0191\u0001"+
		"7\u00017\u00018\u00018\u00019\u00019\u0001:\u0004:\u019b\b:\u000b:\f:"+
		"\u019c\u0001;\u0001;\u0001<\u0001<\u0001=\u0001=\u0001>\u0001>\u0001?"+
		"\u0001?\u0001@\u0004@\u01aa\b@\u000b@\f@\u01ab\u0001A\u0001A\u0001A\u0000"+
		"\u0000B\u0000\u0002\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018"+
		"\u001a\u001c\u001e \"$&(*,.02468:<>@BDFHJLNPRTVXZ\\^`bdfhjlnprtvxz|~\u0080"+
		"\u0082\u0000\u0006\u0003\u0000\u0003\u0003\u0015\u0015\u001f\u001f\u0003"+
		"\u0000\u001a\u001a\u001c\u001d\u001f\u001f\u0002\u0000\u001c\u001d\u001f"+
		"\u001f\u0002\u0000\u0003\u0015\u001a\u001f\u0001\u0000\u001c\u001f\u0001"+
		"\u0000\u001a\u001f\u01a3\u0000\u0087\u0001\u0000\u0000\u0000\u0002\u0092"+
		"\u0001\u0000\u0000\u0000\u0004\u0097\u0001\u0000\u0000\u0000\u0006\u009a"+
		"\u0001\u0000\u0000\u0000\b\u00b2\u0001\u0000\u0000\u0000\n\u00b6\u0001"+
		"\u0000\u0000\u0000\f\u00b8\u0001\u0000\u0000\u0000\u000e\u00c1\u0001\u0000"+
		"\u0000\u0000\u0010\u00ca\u0001\u0000\u0000\u0000\u0012\u00cf\u0001\u0000"+
		"\u0000\u0000\u0014\u00d2\u0001\u0000\u0000\u0000\u0016\u00d5\u0001\u0000"+
		"\u0000\u0000\u0018\u00d8\u0001\u0000\u0000\u0000\u001a\u00e2\u0001\u0000"+
		"\u0000\u0000\u001c\u00ec\u0001\u0000\u0000\u0000\u001e\u00f1\u0001\u0000"+
		"\u0000\u0000 \u00f6\u0001\u0000\u0000\u0000\"\u00f9\u0001\u0000\u0000"+
		"\u0000$\u00fc\u0001\u0000\u0000\u0000&\u0102\u0001\u0000\u0000\u0000("+
		"\u0105\u0001\u0000\u0000\u0000*\u0108\u0001\u0000\u0000\u0000,\u0110\u0001"+
		"\u0000\u0000\u0000.\u0113\u0001\u0000\u0000\u00000\u0117\u0001\u0000\u0000"+
		"\u00002\u011b\u0001\u0000\u0000\u00004\u0121\u0001\u0000\u0000\u00006"+
		"\u0124\u0001\u0000\u0000\u00008\u0128\u0001\u0000\u0000\u0000:\u012a\u0001"+
		"\u0000\u0000\u0000<\u012c\u0001\u0000\u0000\u0000>\u012e\u0001\u0000\u0000"+
		"\u0000@\u013a\u0001\u0000\u0000\u0000B\u013d\u0001\u0000\u0000\u0000D"+
		"\u013f\u0001\u0000\u0000\u0000F\u0145\u0001\u0000\u0000\u0000H\u014d\u0001"+
		"\u0000\u0000\u0000J\u014f\u0001\u0000\u0000\u0000L\u0151\u0001\u0000\u0000"+
		"\u0000N\u0154\u0001\u0000\u0000\u0000P\u015f\u0001\u0000\u0000\u0000R"+
		"\u0161\u0001\u0000\u0000\u0000T\u0163\u0001\u0000\u0000\u0000V\u0166\u0001"+
		"\u0000\u0000\u0000X\u016a\u0001\u0000\u0000\u0000Z\u016d\u0001\u0000\u0000"+
		"\u0000\\\u0171\u0001\u0000\u0000\u0000^\u0174\u0001\u0000\u0000\u0000"+
		"`\u017f\u0001\u0000\u0000\u0000b\u0181\u0001\u0000\u0000\u0000d\u0183"+
		"\u0001\u0000\u0000\u0000f\u0185\u0001\u0000\u0000\u0000h\u0188\u0001\u0000"+
		"\u0000\u0000j\u018c\u0001\u0000\u0000\u0000l\u018f\u0001\u0000\u0000\u0000"+
		"n\u0193\u0001\u0000\u0000\u0000p\u0195\u0001\u0000\u0000\u0000r\u0197"+
		"\u0001\u0000\u0000\u0000t\u019a\u0001\u0000\u0000\u0000v\u019e\u0001\u0000"+
		"\u0000\u0000x\u01a0\u0001\u0000\u0000\u0000z\u01a2\u0001\u0000\u0000\u0000"+
		"|\u01a4\u0001\u0000\u0000\u0000~\u01a6\u0001\u0000\u0000\u0000\u0080\u01a9"+
		"\u0001\u0000\u0000\u0000\u0082\u01ad\u0001\u0000\u0000\u0000\u0084\u0086"+
		"\u0003\u0002\u0001\u0000\u0085\u0084\u0001\u0000\u0000\u0000\u0086\u0089"+
		"\u0001\u0000\u0000\u0000\u0087\u0085\u0001\u0000\u0000\u0000\u0087\u0088"+
		"\u0001\u0000\u0000\u0000\u0088\u008a\u0001\u0000\u0000\u0000\u0089\u0087"+
		"\u0001\u0000\u0000\u0000\u008a\u008c\u0003\u0004\u0002\u0000\u008b\u008d"+
		"\u0003\u0006\u0003\u0000\u008c\u008b\u0001\u0000\u0000\u0000\u008d\u008e"+
		"\u0001\u0000\u0000\u0000\u008e\u008c\u0001\u0000\u0000\u0000\u008e\u008f"+
		"\u0001\u0000\u0000\u0000\u008f\u0090\u0001\u0000\u0000\u0000\u0090\u0091"+
		"\u0005\u0000\u0000\u0001\u0091\u0001\u0001\u0000\u0000\u0000\u0092\u0093"+
		"\u0005\u0001\u0000\u0000\u0093\u0003\u0001\u0000\u0000\u0000\u0094\u0096"+
		"\u0003$\u0012\u0000\u0095\u0094\u0001\u0000\u0000\u0000\u0096\u0099\u0001"+
		"\u0000\u0000\u0000\u0097\u0095\u0001\u0000\u0000\u0000\u0097\u0098\u0001"+
		"\u0000\u0000\u0000\u0098\u0005\u0001\u0000\u0000\u0000\u0099\u0097\u0001"+
		"\u0000\u0000\u0000\u009a\u009e\u0003\f\u0006\u0000\u009b\u009d\u0003\b"+
		"\u0004\u0000\u009c\u009b\u0001\u0000\u0000\u0000\u009d\u00a0\u0001\u0000"+
		"\u0000\u0000\u009e\u009c\u0001\u0000\u0000\u0000\u009e\u009f\u0001\u0000"+
		"\u0000\u0000\u009f\u0007\u0001\u0000\u0000\u0000\u00a0\u009e\u0001\u0000"+
		"\u0000\u0000\u00a1\u00b3\u0003\u000e\u0007\u0000\u00a2\u00b3\u0003\u0010"+
		"\b\u0000\u00a3\u00b3\u0003\u0012\t\u0000\u00a4\u00b3\u0003\u0014\n\u0000"+
		"\u00a5\u00b3\u0003\u0016\u000b\u0000\u00a6\u00b3\u0003\u0018\f\u0000\u00a7"+
		"\u00b3\u0003\u001a\r\u0000\u00a8\u00b3\u0003\u001c\u000e\u0000\u00a9\u00b3"+
		"\u0003\u001e\u000f\u0000\u00aa\u00b3\u0003 \u0010\u0000\u00ab\u00b3\u0003"+
		"\"\u0011\u0000\u00ac\u00b3\u0003$\u0012\u0000\u00ad\u00b3\u0003&\u0013"+
		"\u0000\u00ae\u00b3\u0003(\u0014\u0000\u00af\u00b3\u0003*\u0015\u0000\u00b0"+
		"\u00b3\u0003,\u0016\u0000\u00b1\u00b3\u0003.\u0017\u0000\u00b2\u00a1\u0001"+
		"\u0000\u0000\u0000\u00b2\u00a2\u0001\u0000\u0000\u0000\u00b2\u00a3\u0001"+
		"\u0000\u0000\u0000\u00b2\u00a4\u0001\u0000\u0000\u0000\u00b2\u00a5\u0001"+
		"\u0000\u0000\u0000\u00b2\u00a6\u0001\u0000\u0000\u0000\u00b2\u00a7\u0001"+
		"\u0000\u0000\u0000\u00b2\u00a8\u0001\u0000\u0000\u0000\u00b2\u00a9\u0001"+
		"\u0000\u0000\u0000\u00b2\u00aa\u0001\u0000\u0000\u0000\u00b2\u00ab\u0001"+
		"\u0000\u0000\u0000\u00b2\u00ac\u0001\u0000\u0000\u0000\u00b2\u00ad\u0001"+
		"\u0000\u0000\u0000\u00b2\u00ae\u0001\u0000\u0000\u0000\u00b2\u00af\u0001"+
		"\u0000\u0000\u0000\u00b2\u00b0\u0001\u0000\u0000\u0000\u00b2\u00b1\u0001"+
		"\u0000\u0000\u0000\u00b3\t\u0001\u0000\u0000\u0000\u00b4\u00b7\u0003\f"+
		"\u0006\u0000\u00b5\u00b7\u0003\b\u0004\u0000\u00b6\u00b4\u0001\u0000\u0000"+
		"\u0000\u00b6\u00b5\u0001\u0000\u0000\u0000\u00b7\u000b\u0001\u0000\u0000"+
		"\u0000\u00b8\u00ba\u0005\u0003\u0000\u0000\u00b9\u00bb\u00030\u0018\u0000"+
		"\u00ba\u00b9\u0001\u0000\u0000\u0000\u00ba\u00bb\u0001\u0000\u0000\u0000"+
		"\u00bb\u00bc\u0001\u0000\u0000\u0000\u00bc\u00bf\u0003J%\u0000\u00bd\u00be"+
		"\u0005\u0015\u0000\u0000\u00be\u00c0\u0003L&\u0000\u00bf\u00bd\u0001\u0000"+
		"\u0000\u0000\u00bf\u00c0\u0001\u0000\u0000\u0000\u00c0\r\u0001\u0000\u0000"+
		"\u0000\u00c1\u00c3\u0005\u0004\u0000\u0000\u00c2\u00c4\u00030\u0018\u0000"+
		"\u00c3\u00c2\u0001\u0000\u0000\u0000\u00c3\u00c4\u0001\u0000\u0000\u0000"+
		"\u00c4\u00c8\u0001\u0000\u0000\u0000\u00c5\u00c9\u0003:\u001d\u0000\u00c6"+
		"\u00c9\u0003<\u001e\u0000\u00c7\u00c9\u0003>\u001f\u0000\u00c8\u00c5\u0001"+
		"\u0000\u0000\u0000\u00c8\u00c6\u0001\u0000\u0000\u0000\u00c8\u00c7\u0001"+
		"\u0000\u0000\u0000\u00c9\u000f\u0001\u0000\u0000\u0000\u00ca\u00cd\u0005"+
		"\u0005\u0000\u0000\u00cb\u00ce\u0003:\u001d\u0000\u00cc\u00ce\u0003<\u001e"+
		"\u0000\u00cd\u00cb\u0001\u0000\u0000\u0000\u00cd\u00cc\u0001\u0000\u0000"+
		"\u0000\u00ce\u0011\u0001\u0000\u0000\u0000\u00cf\u00d0\u0005\u0006\u0000"+
		"\u0000\u00d0\u00d1\u0003N\'\u0000\u00d1\u0013\u0001\u0000\u0000\u0000"+
		"\u00d2\u00d3\u0005\u0007\u0000\u0000\u00d3\u00d4\u0003Z-\u0000\u00d4\u0015"+
		"\u0001\u0000\u0000\u0000\u00d5\u00d6\u0005\b\u0000\u0000\u00d6\u00d7\u0003"+
		"^/\u0000\u00d7\u0017\u0001\u0000\u0000\u0000\u00d8\u00da\u0005\t\u0000"+
		"\u0000\u00d9\u00db\u00030\u0018\u0000\u00da\u00d9\u0001\u0000\u0000\u0000"+
		"\u00da\u00db\u0001\u0000\u0000\u0000\u00db\u00e0\u0001\u0000\u0000\u0000"+
		"\u00dc\u00e1\u0003>\u001f\u0000\u00dd\u00de\u0003l6\u0000\u00de\u00df"+
		"\u0003p8\u0000\u00df\u00e1\u0001\u0000\u0000\u0000\u00e0\u00dc\u0001\u0000"+
		"\u0000\u0000\u00e0\u00dd\u0001\u0000\u0000\u0000\u00e1\u0019\u0001\u0000"+
		"\u0000\u0000\u00e2\u00e4\u0005\n\u0000\u0000\u00e3\u00e5\u00030\u0018"+
		"\u0000\u00e4\u00e3\u0001\u0000\u0000\u0000\u00e4\u00e5\u0001\u0000\u0000"+
		"\u0000\u00e5\u00ea\u0001\u0000\u0000\u0000\u00e6\u00eb\u0003>\u001f\u0000"+
		"\u00e7\u00e8\u0003l6\u0000\u00e8\u00e9\u0003p8\u0000\u00e9\u00eb\u0001"+
		"\u0000\u0000\u0000\u00ea\u00e6\u0001\u0000\u0000\u0000\u00ea\u00e7\u0001"+
		"\u0000\u0000\u0000\u00eb\u001b\u0001\u0000\u0000\u0000\u00ec\u00ef\u0005"+
		"\u000b\u0000\u0000\u00ed\u00f0\u0003:\u001d\u0000\u00ee\u00f0\u0003<\u001e"+
		"\u0000\u00ef\u00ed\u0001\u0000\u0000\u0000\u00ef\u00ee\u0001\u0000\u0000"+
		"\u0000\u00f0\u001d\u0001\u0000\u0000\u0000\u00f1\u00f4\u0005\f\u0000\u0000"+
		"\u00f2\u00f5\u0003D\"\u0000\u00f3\u00f5\u0003t:\u0000\u00f4\u00f2\u0001"+
		"\u0000\u0000\u0000\u00f4\u00f3\u0001\u0000\u0000\u0000\u00f5\u001f\u0001"+
		"\u0000\u0000\u0000\u00f6\u00f7\u0005\r\u0000\u0000\u00f7\u00f8\u0003x"+
		"<\u0000\u00f8!\u0001\u0000\u0000\u0000\u00f9\u00fa\u0005\u000e\u0000\u0000"+
		"\u00fa\u00fb\u0003r9\u0000\u00fb#\u0001\u0000\u0000\u0000\u00fc\u00fd"+
		"\u0005\u000f\u0000\u0000\u00fd\u0100\u0003z=\u0000\u00fe\u00ff\u0005\u001a"+
		"\u0000\u0000\u00ff\u0101\u0003|>\u0000\u0100\u00fe\u0001\u0000\u0000\u0000"+
		"\u0100\u0101\u0001\u0000\u0000\u0000\u0101%\u0001\u0000\u0000\u0000\u0102"+
		"\u0103\u0005\u0010\u0000\u0000\u0103\u0104\u0003\n\u0005\u0000\u0104\'"+
		"\u0001\u0000\u0000\u0000\u0105\u0106\u0005\u0011\u0000\u0000\u0106\u0107"+
		"\u0003~?\u0000\u0107)\u0001\u0000\u0000\u0000\u0108\u010e\u0005\u0012"+
		"\u0000\u0000\u0109\u010f\u0005\u001f\u0000\u0000\u010a\u010c\u00030\u0018"+
		"\u0000\u010b\u010a\u0001\u0000\u0000\u0000\u010b\u010c\u0001\u0000\u0000"+
		"\u0000\u010c\u010d\u0001\u0000\u0000\u0000\u010d\u010f\u0003\u0010\b\u0000"+
		"\u010e\u0109\u0001\u0000\u0000\u0000\u010e\u010b\u0001\u0000\u0000\u0000"+
		"\u010f+\u0001\u0000\u0000\u0000\u0110\u0111\u0005\u0013\u0000\u0000\u0111"+
		"\u0112\u0003D\"\u0000\u0112-\u0001\u0000\u0000\u0000\u0113\u0114\u0005"+
		"\u0014\u0000\u0000\u0114\u0115\u0003\u0080@\u0000\u0115/\u0001\u0000\u0000"+
		"\u0000\u0116\u0118\u00032\u0019\u0000\u0117\u0116\u0001\u0000\u0000\u0000"+
		"\u0118\u0119\u0001\u0000\u0000\u0000\u0119\u0117\u0001\u0000\u0000\u0000"+
		"\u0119\u011a\u0001\u0000\u0000\u0000\u011a1\u0001\u0000\u0000\u0000\u011b"+
		"\u011c\u0005\u001b\u0000\u0000\u011c\u011f\u00034\u001a\u0000\u011d\u011e"+
		"\u0005\u001a\u0000\u0000\u011e\u0120\u00036\u001b\u0000\u011f\u011d\u0001"+
		"\u0000\u0000\u0000\u011f\u0120\u0001\u0000\u0000\u0000\u01203\u0001\u0000"+
		"\u0000\u0000\u0121\u0122\u0007\u0000\u0000\u0000\u01225\u0001\u0000\u0000"+
		"\u0000\u0123\u0125\u00038\u001c\u0000\u0124\u0123\u0001\u0000\u0000\u0000"+
		"\u0125\u0126\u0001\u0000\u0000\u0000\u0126\u0124\u0001\u0000\u0000\u0000"+
		"\u0126\u0127\u0001\u0000\u0000\u0000\u01277\u0001\u0000\u0000\u0000\u0128"+
		"\u0129\u0007\u0001\u0000\u0000\u01299\u0001\u0000\u0000\u0000\u012a\u012b"+
		"\u0003D\"\u0000\u012b;\u0001\u0000\u0000\u0000\u012c\u012d\u0003\u0080"+
		"@\u0000\u012d=\u0001\u0000\u0000\u0000\u012e\u0130\u0005\u0016\u0000\u0000"+
		"\u012f\u0131\u0003r9\u0000\u0130\u012f\u0001\u0000\u0000\u0000\u0130\u0131"+
		"\u0001\u0000\u0000\u0000\u0131\u0132\u0001\u0000\u0000\u0000\u0132\u0133"+
		"\u0005!\u0000\u0000\u0133\u0134\u0003@ \u0000\u0134\u0135\u0003B!\u0000"+
		"\u0135?\u0001\u0000\u0000\u0000\u0136\u0139\u0005!\u0000\u0000\u0137\u0139"+
		"\u0003\u0080@\u0000\u0138\u0136\u0001\u0000\u0000\u0000\u0138\u0137\u0001"+
		"\u0000\u0000\u0000\u0139\u013c\u0001\u0000\u0000\u0000\u013a\u0138\u0001"+
		"\u0000\u0000\u0000\u013a\u013b\u0001\u0000\u0000\u0000\u013bA\u0001\u0000"+
		"\u0000\u0000\u013c\u013a\u0001\u0000\u0000\u0000\u013d\u013e\u0005\u001f"+
		"\u0000\u0000\u013eC\u0001\u0000\u0000\u0000\u013f\u0141\u0005\u0018\u0000"+
		"\u0000\u0140\u0142\u0003F#\u0000\u0141\u0140\u0001\u0000\u0000\u0000\u0141"+
		"\u0142\u0001\u0000\u0000\u0000\u0142\u0143\u0001\u0000\u0000\u0000\u0143"+
		"\u0144\u0005#\u0000\u0000\u0144E\u0001\u0000\u0000\u0000\u0145\u014a\u0003"+
		"H$\u0000\u0146\u0147\u0005\"\u0000\u0000\u0147\u0149\u0003H$\u0000\u0148"+
		"\u0146\u0001\u0000\u0000\u0000\u0149\u014c\u0001\u0000\u0000\u0000\u014a"+
		"\u0148\u0001\u0000\u0000\u0000\u014a\u014b\u0001\u0000\u0000\u0000\u014b"+
		"G\u0001\u0000\u0000\u0000\u014c\u014a\u0001\u0000\u0000\u0000\u014d\u014e"+
		"\u0005$\u0000\u0000\u014eI\u0001\u0000\u0000\u0000\u014f\u0150\u0003\u0080"+
		"@\u0000\u0150K\u0001\u0000\u0000\u0000\u0151\u0152\u0005\u001f\u0000\u0000"+
		"\u0152M\u0001\u0000\u0000\u0000\u0153\u0155\u0003P(\u0000\u0154\u0153"+
		"\u0001\u0000\u0000\u0000\u0155\u0156\u0001\u0000\u0000\u0000\u0156\u0154"+
		"\u0001\u0000\u0000\u0000\u0156\u0157\u0001\u0000\u0000\u0000\u0157O\u0001"+
		"\u0000\u0000\u0000\u0158\u0159\u0003R)\u0000\u0159\u015a\u0005\u001a\u0000"+
		"\u0000\u015a\u015b\u0003T*\u0000\u015b\u0160\u0001\u0000\u0000\u0000\u015c"+
		"\u015d\u0003R)\u0000\u015d\u015e\u0003V+\u0000\u015e\u0160\u0001\u0000"+
		"\u0000\u0000\u015f\u0158\u0001\u0000\u0000\u0000\u015f\u015c\u0001\u0000"+
		"\u0000\u0000\u0160Q\u0001\u0000\u0000\u0000\u0161\u0162\u0007\u0002\u0000"+
		"\u0000\u0162S\u0001\u0000\u0000\u0000\u0163\u0164\u0007\u0002\u0000\u0000"+
		"\u0164U\u0001\u0000\u0000\u0000\u0165\u0167\u0003X,\u0000\u0166\u0165"+
		"\u0001\u0000\u0000\u0000\u0167\u0168\u0001\u0000\u0000\u0000\u0168\u0166"+
		"\u0001\u0000\u0000\u0000\u0168\u0169\u0001\u0000\u0000\u0000\u0169W\u0001"+
		"\u0000\u0000\u0000\u016a\u016b\u0007\u0003\u0000\u0000\u016bY\u0001\u0000"+
		"\u0000\u0000\u016c\u016e\u0003\\.\u0000\u016d\u016c\u0001\u0000\u0000"+
		"\u0000\u016e\u016f\u0001\u0000\u0000\u0000\u016f\u016d\u0001\u0000\u0000"+
		"\u0000\u016f\u0170\u0001\u0000\u0000\u0000\u0170[\u0001\u0000\u0000\u0000"+
		"\u0171\u0172\u0005\u001f\u0000\u0000\u0172]\u0001\u0000\u0000\u0000\u0173"+
		"\u0175\u0003`0\u0000\u0174\u0173\u0001\u0000\u0000\u0000\u0175\u0176\u0001"+
		"\u0000\u0000\u0000\u0176\u0174\u0001\u0000\u0000\u0000\u0176\u0177\u0001"+
		"\u0000\u0000\u0000\u0177_\u0001\u0000\u0000\u0000\u0178\u0179\u0003b1"+
		"\u0000\u0179\u017a\u0005\u001a\u0000\u0000\u017a\u017b\u0003d2\u0000\u017b"+
		"\u0180\u0001\u0000\u0000\u0000\u017c\u017d\u0003b1\u0000\u017d\u017e\u0003"+
		"f3\u0000\u017e\u0180\u0001\u0000\u0000\u0000\u017f\u0178\u0001\u0000\u0000"+
		"\u0000\u017f\u017c\u0001\u0000\u0000\u0000\u0180a\u0001\u0000\u0000\u0000"+
		"\u0181\u0182\u0005\u001f\u0000\u0000\u0182c\u0001\u0000\u0000\u0000\u0183"+
		"\u0184\u0003h4\u0000\u0184e\u0001\u0000\u0000\u0000\u0185\u0186\u0003"+
		"\u0080@\u0000\u0186g\u0001\u0000\u0000\u0000\u0187\u0189\u0003j5\u0000"+
		"\u0188\u0187\u0001\u0000\u0000\u0000\u0189\u018a\u0001\u0000\u0000\u0000"+
		"\u018a\u0188\u0001\u0000\u0000\u0000\u018a\u018b\u0001\u0000\u0000\u0000"+
		"\u018bi\u0001\u0000\u0000\u0000\u018c\u018d\u0007\u0004\u0000\u0000\u018d"+
		"k\u0001\u0000\u0000\u0000\u018e\u0190\u0003n7\u0000\u018f\u018e\u0001"+
		"\u0000\u0000\u0000\u0190\u0191\u0001\u0000\u0000\u0000\u0191\u018f\u0001"+
		"\u0000\u0000\u0000\u0191\u0192\u0001\u0000\u0000\u0000\u0192m\u0001\u0000"+
		"\u0000\u0000\u0193\u0194\u0003r9\u0000\u0194o\u0001\u0000\u0000\u0000"+
		"\u0195\u0196\u0003r9\u0000\u0196q\u0001\u0000\u0000\u0000\u0197\u0198"+
		"\u0003\u0080@\u0000\u0198s\u0001\u0000\u0000\u0000\u0199\u019b\u0003v"+
		";\u0000\u019a\u0199\u0001\u0000\u0000\u0000\u019b\u019c\u0001\u0000\u0000"+
		"\u0000\u019c\u019a\u0001\u0000\u0000\u0000\u019c\u019d\u0001\u0000\u0000"+
		"\u0000\u019du\u0001\u0000\u0000\u0000\u019e\u019f\u0007\u0002\u0000\u0000"+
		"\u019fw\u0001\u0000\u0000\u0000\u01a0\u01a1\u0003\u0080@\u0000\u01a1y"+
		"\u0001\u0000\u0000\u0000\u01a2\u01a3\u0005\u001f\u0000\u0000\u01a3{\u0001"+
		"\u0000\u0000\u0000\u01a4\u01a5\u0003\u0080@\u0000\u01a5}\u0001\u0000\u0000"+
		"\u0000\u01a6\u01a7\u0005\u001f\u0000\u0000\u01a7\u007f\u0001\u0000\u0000"+
		"\u0000\u01a8\u01aa\u0003\u0082A\u0000\u01a9\u01a8\u0001\u0000\u0000\u0000"+
		"\u01aa\u01ab\u0001\u0000\u0000\u0000\u01ab\u01a9\u0001\u0000\u0000\u0000"+
		"\u01ab\u01ac\u0001\u0000\u0000\u0000\u01ac\u0081\u0001\u0000\u0000\u0000"+
		"\u01ad\u01ae\u0007\u0005\u0000\u0000\u01ae\u0083\u0001\u0000\u0000\u0000"+
		"&\u0087\u008e\u0097\u009e\u00b2\u00b6\u00ba\u00bf\u00c3\u00c8\u00cd\u00da"+
		"\u00e0\u00e4\u00ea\u00ef\u00f4\u0100\u010b\u010e\u0119\u011f\u0126\u0130"+
		"\u0138\u013a\u0141\u014a\u0156\u015f\u0168\u016f\u0176\u017f\u018a\u0191"+
		"\u019c\u01ab";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}