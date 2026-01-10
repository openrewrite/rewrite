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
// Generated from /home/tim/Documents/workspace/openrewrite/rewrite/rewrite-docker/src/main/antlr/DockerLexer.g4 by ANTLR 4.13.2
package org.openrewrite.docker.internal.grammar;
import java.util.Stack;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class DockerLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		PARSER_DIRECTIVE=1, COMMENT=2, FROM=3, RUN=4, CMD=5, LABEL=6, EXPOSE=7, 
		ENV=8, ADD=9, COPY=10, ENTRYPOINT=11, VOLUME=12, USER=13, WORKDIR=14, 
		ARG=15, ONBUILD=16, STOPSIGNAL=17, HEALTHCHECK=18, SHELL=19, MAINTAINER=20, 
		AS=21, HEREDOC_START=22, LINE_CONTINUATION=23, LBRACKET=24, RBRACKET=25, 
		COMMA=26, EQUALS=27, DASH_DASH=28, DOUBLE_QUOTED_STRING=29, SINGLE_QUOTED_STRING=30, 
		ENV_VAR=31, SPECIAL_VAR=32, COMMAND_SUBST=33, BACKTICK_SUBST=34, UNQUOTED_TEXT=35, 
		WS=36, NEWLINE=37, HP_WS=38, HP_COMMENT=39, HP_LINE_COMMENT=40, HEREDOC_CONTENT=41, 
		H_NEWLINE=42;
	public static final int
		HEREDOC_PREAMBLE=1, HEREDOC=2;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE", "HEREDOC_PREAMBLE", "HEREDOC"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"PARSER_DIRECTIVE", "COMMENT", "FROM", "RUN", "CMD", "LABEL", "EXPOSE", 
			"ENV", "ADD", "COPY", "ENTRYPOINT", "VOLUME", "USER", "WORKDIR", "ARG", 
			"ONBUILD", "STOPSIGNAL", "HEALTHCHECK", "SHELL", "MAINTAINER", "AS", 
			"HEREDOC_START", "LINE_CONTINUATION", "LBRACKET", "RBRACKET", "COMMA", 
			"EQUALS", "DASH_DASH", "UNQUOTED_CHAR", "ESCAPED_CHAR", "DOUBLE_QUOTED_STRING", 
			"SINGLE_QUOTED_STRING", "INLINE_CONTINUATION", "ESCAPE_SEQUENCE", "HEX_DIGIT", 
			"ENV_VAR", "SPECIAL_VAR", "COMMAND_SUBST", "COMMAND_SUBST_INNER", "BACKTICK_SUBST", 
			"UNQUOTED_TEXT", "WS", "WS_CHAR", "NEWLINE", "NEWLINE_CHAR", "HP_NEWLINE", 
			"HP_WS", "HP_COMMENT", "HP_LINE_COMMENT", "HPIdentifier", "HP_UNQUOTED_TEXT", 
			"H_NEWLINE", "HEREDOC_CONTENT"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, "'FROM'", "'RUN'", "'CMD'", "'LABEL'", "'EXPOSE'", 
			"'ENV'", "'ADD'", "'COPY'", "'ENTRYPOINT'", "'VOLUME'", "'USER'", "'WORKDIR'", 
			"'ARG'", "'ONBUILD'", "'STOPSIGNAL'", "'HEALTHCHECK'", "'SHELL'", "'MAINTAINER'", 
			"'AS'", null, null, "'['", "']'", "','", "'='", "'--'", null, null, null, 
			null, null, null, null, null, null, null, null, null, null, "'\\n'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "PARSER_DIRECTIVE", "COMMENT", "FROM", "RUN", "CMD", "LABEL", "EXPOSE", 
			"ENV", "ADD", "COPY", "ENTRYPOINT", "VOLUME", "USER", "WORKDIR", "ARG", 
			"ONBUILD", "STOPSIGNAL", "HEALTHCHECK", "SHELL", "MAINTAINER", "AS", 
			"HEREDOC_START", "LINE_CONTINUATION", "LBRACKET", "RBRACKET", "COMMA", 
			"EQUALS", "DASH_DASH", "DOUBLE_QUOTED_STRING", "SINGLE_QUOTED_STRING", 
			"ENV_VAR", "SPECIAL_VAR", "COMMAND_SUBST", "BACKTICK_SUBST", "UNQUOTED_TEXT", 
			"WS", "NEWLINE", "HP_WS", "HP_COMMENT", "HP_LINE_COMMENT", "HEREDOC_CONTENT", 
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


	    private Stack<String> heredocIdentifier = new Stack<String>();
	    private boolean heredocIdentifierCaptured = false;
	    // Track if we're at the start of a logical line (where instructions can appear)
	    private boolean atLineStart = true;


	public DockerLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "DockerLexer.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	@Override
	public void action(RuleContext _localctx, int ruleIndex, int actionIndex) {
		switch (ruleIndex) {
		case 0:
			PARSER_DIRECTIVE_action((RuleContext)_localctx, actionIndex);
			break;
		case 2:
			FROM_action((RuleContext)_localctx, actionIndex);
			break;
		case 3:
			RUN_action((RuleContext)_localctx, actionIndex);
			break;
		case 4:
			CMD_action((RuleContext)_localctx, actionIndex);
			break;
		case 5:
			LABEL_action((RuleContext)_localctx, actionIndex);
			break;
		case 6:
			EXPOSE_action((RuleContext)_localctx, actionIndex);
			break;
		case 7:
			ENV_action((RuleContext)_localctx, actionIndex);
			break;
		case 8:
			ADD_action((RuleContext)_localctx, actionIndex);
			break;
		case 9:
			COPY_action((RuleContext)_localctx, actionIndex);
			break;
		case 10:
			ENTRYPOINT_action((RuleContext)_localctx, actionIndex);
			break;
		case 11:
			VOLUME_action((RuleContext)_localctx, actionIndex);
			break;
		case 12:
			USER_action((RuleContext)_localctx, actionIndex);
			break;
		case 13:
			WORKDIR_action((RuleContext)_localctx, actionIndex);
			break;
		case 14:
			ARG_action((RuleContext)_localctx, actionIndex);
			break;
		case 15:
			ONBUILD_action((RuleContext)_localctx, actionIndex);
			break;
		case 16:
			STOPSIGNAL_action((RuleContext)_localctx, actionIndex);
			break;
		case 17:
			HEALTHCHECK_action((RuleContext)_localctx, actionIndex);
			break;
		case 18:
			SHELL_action((RuleContext)_localctx, actionIndex);
			break;
		case 19:
			MAINTAINER_action((RuleContext)_localctx, actionIndex);
			break;
		case 20:
			AS_action((RuleContext)_localctx, actionIndex);
			break;
		case 21:
			HEREDOC_START_action((RuleContext)_localctx, actionIndex);
			break;
		case 23:
			LBRACKET_action((RuleContext)_localctx, actionIndex);
			break;
		case 24:
			RBRACKET_action((RuleContext)_localctx, actionIndex);
			break;
		case 25:
			COMMA_action((RuleContext)_localctx, actionIndex);
			break;
		case 26:
			EQUALS_action((RuleContext)_localctx, actionIndex);
			break;
		case 27:
			DASH_DASH_action((RuleContext)_localctx, actionIndex);
			break;
		case 30:
			DOUBLE_QUOTED_STRING_action((RuleContext)_localctx, actionIndex);
			break;
		case 31:
			SINGLE_QUOTED_STRING_action((RuleContext)_localctx, actionIndex);
			break;
		case 35:
			ENV_VAR_action((RuleContext)_localctx, actionIndex);
			break;
		case 36:
			SPECIAL_VAR_action((RuleContext)_localctx, actionIndex);
			break;
		case 37:
			COMMAND_SUBST_action((RuleContext)_localctx, actionIndex);
			break;
		case 39:
			BACKTICK_SUBST_action((RuleContext)_localctx, actionIndex);
			break;
		case 40:
			UNQUOTED_TEXT_action((RuleContext)_localctx, actionIndex);
			break;
		case 43:
			NEWLINE_action((RuleContext)_localctx, actionIndex);
			break;
		case 49:
			HPIdentifier_action((RuleContext)_localctx, actionIndex);
			break;
		case 52:
			HEREDOC_CONTENT_action((RuleContext)_localctx, actionIndex);
			break;
		}
	}
	private void PARSER_DIRECTIVE_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 0:
			 atLineStart = true; 
			break;
		}
	}
	private void FROM_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 1:
			 if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; 
			break;
		}
	}
	private void RUN_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 2:
			 if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; 
			break;
		}
	}
	private void CMD_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 3:
			 if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; 
			break;
		}
	}
	private void LABEL_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 4:
			 if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; 
			break;
		}
	}
	private void EXPOSE_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 5:
			 if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; 
			break;
		}
	}
	private void ENV_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 6:
			 if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; 
			break;
		}
	}
	private void ADD_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 7:
			 if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; 
			break;
		}
	}
	private void COPY_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 8:
			 if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; 
			break;
		}
	}
	private void ENTRYPOINT_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 9:
			 if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; 
			break;
		}
	}
	private void VOLUME_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 10:
			 if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; 
			break;
		}
	}
	private void USER_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 11:
			 if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; 
			break;
		}
	}
	private void WORKDIR_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 12:
			 if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; 
			break;
		}
	}
	private void ARG_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 13:
			 if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; 
			break;
		}
	}
	private void ONBUILD_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 14:
			 if (!atLineStart) setType(UNQUOTED_TEXT); /* atLineStart stays true */ 
			break;
		}
	}
	private void STOPSIGNAL_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 15:
			 if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; 
			break;
		}
	}
	private void HEALTHCHECK_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 16:
			 if (!atLineStart) setType(UNQUOTED_TEXT); /* atLineStart stays true for CMD */ 
			break;
		}
	}
	private void SHELL_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 17:
			 if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; 
			break;
		}
	}
	private void MAINTAINER_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 18:
			 if (!atLineStart) setType(UNQUOTED_TEXT); atLineStart = false; 
			break;
		}
	}
	private void AS_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 19:
			 atLineStart = false; 
			break;
		}
	}
	private void HEREDOC_START_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 20:

			    heredocIdentifierCaptured = false;  // Reset for new heredoc
			    atLineStart = false;

			break;
		}
	}
	private void LBRACKET_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 21:
			 atLineStart = false; 
			break;
		}
	}
	private void RBRACKET_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 22:
			 atLineStart = false; 
			break;
		}
	}
	private void COMMA_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 23:
			 atLineStart = false; 
			break;
		}
	}
	private void EQUALS_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 24:
			 atLineStart = false; 
			break;
		}
	}
	private void DASH_DASH_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 25:
			 atLineStart = false; 
			break;
		}
	}
	private void DOUBLE_QUOTED_STRING_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 26:
			 atLineStart = false; 
			break;
		}
	}
	private void SINGLE_QUOTED_STRING_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 27:
			 atLineStart = false; 
			break;
		}
	}
	private void ENV_VAR_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 28:
			 atLineStart = false; 
			break;
		}
	}
	private void SPECIAL_VAR_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 29:
			 atLineStart = false; 
			break;
		}
	}
	private void COMMAND_SUBST_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 30:
			 atLineStart = false; 
			break;
		}
	}
	private void BACKTICK_SUBST_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 31:
			 atLineStart = false; 
			break;
		}
	}
	private void UNQUOTED_TEXT_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 32:
			 atLineStart = false; 
			break;
		}
	}
	private void NEWLINE_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 33:
			 atLineStart = true; 
			break;
		}
	}
	private void HPIdentifier_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 34:

			    // Only push the first identifier (the heredoc marker), not subsequent text like interpreter names
			    if (!heredocIdentifierCaptured) {
			        heredocIdentifier.push(getText());
			        heredocIdentifierCaptured = true;
			    }

			break;
		}
	}
	private void HEREDOC_CONTENT_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 35:

			  if(!heredocIdentifier.isEmpty() && getText().equals(heredocIdentifier.peek())) {
			      setType(UNQUOTED_TEXT);
			      heredocIdentifier.pop();
			      popMode();
			      atLineStart = true;  // After heredoc ends, next line is at line start
			  }

			break;
		}
	}

	public static final String _serializedATN =
		"\u0004\u0000*\u025d\u0006\uffff\uffff\u0006\uffff\uffff\u0006\uffff\uffff"+
		"\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002\u0002\u0007\u0002"+
		"\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002\u0005\u0007\u0005"+
		"\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002\b\u0007\b\u0002"+
		"\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b\u0002\f\u0007\f\u0002"+
		"\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007\u000f\u0002\u0010"+
		"\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007\u0012\u0002\u0013"+
		"\u0007\u0013\u0002\u0014\u0007\u0014\u0002\u0015\u0007\u0015\u0002\u0016"+
		"\u0007\u0016\u0002\u0017\u0007\u0017\u0002\u0018\u0007\u0018\u0002\u0019"+
		"\u0007\u0019\u0002\u001a\u0007\u001a\u0002\u001b\u0007\u001b\u0002\u001c"+
		"\u0007\u001c\u0002\u001d\u0007\u001d\u0002\u001e\u0007\u001e\u0002\u001f"+
		"\u0007\u001f\u0002 \u0007 \u0002!\u0007!\u0002\"\u0007\"\u0002#\u0007"+
		"#\u0002$\u0007$\u0002%\u0007%\u0002&\u0007&\u0002\'\u0007\'\u0002(\u0007"+
		"(\u0002)\u0007)\u0002*\u0007*\u0002+\u0007+\u0002,\u0007,\u0002-\u0007"+
		"-\u0002.\u0007.\u0002/\u0007/\u00020\u00070\u00021\u00071\u00022\u0007"+
		"2\u00023\u00073\u00024\u00074\u0001\u0000\u0001\u0000\u0005\u0000p\b\u0000"+
		"\n\u0000\f\u0000s\t\u0000\u0001\u0000\u0004\u0000v\b\u0000\u000b\u0000"+
		"\f\u0000w\u0001\u0000\u0005\u0000{\b\u0000\n\u0000\f\u0000~\t\u0000\u0001"+
		"\u0000\u0001\u0000\u0005\u0000\u0082\b\u0000\n\u0000\f\u0000\u0085\t\u0000"+
		"\u0001\u0000\u0005\u0000\u0088\b\u0000\n\u0000\f\u0000\u008b\t\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0005\u0001\u0092"+
		"\b\u0001\n\u0001\f\u0001\u0095\t\u0001\u0001\u0001\u0001\u0001\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004"+
		"\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0001\u0005\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006"+
		"\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0007"+
		"\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\b\u0001"+
		"\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\t\u0001\t\u0001\t\u0001\t\u0001"+
		"\t\u0001\t\u0001\t\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001"+
		"\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\u000b\u0001\u000b"+
		"\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b"+
		"\u0001\u000b\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001"+
		"\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001"+
		"\r\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e"+
		"\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f"+
		"\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u0010\u0001\u0010"+
		"\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010"+
		"\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0011"+
		"\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011"+
		"\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011"+
		"\u0001\u0011\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012"+
		"\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0013\u0001\u0013\u0001\u0013"+
		"\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013"+
		"\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0014\u0001\u0014"+
		"\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0015\u0001\u0015\u0001\u0015"+
		"\u0001\u0015\u0003\u0015\u0140\b\u0015\u0001\u0015\u0001\u0015\u0001\u0015"+
		"\u0001\u0015\u0001\u0016\u0001\u0016\u0005\u0016\u0148\b\u0016\n\u0016"+
		"\f\u0016\u014b\t\u0016\u0001\u0016\u0001\u0016\u0001\u0016\u0001\u0016"+
		"\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0018\u0001\u0018\u0001\u0018"+
		"\u0001\u0019\u0001\u0019\u0001\u0019\u0001\u001a\u0001\u001a\u0001\u001a"+
		"\u0001\u001b\u0001\u001b\u0001\u001b\u0001\u001b\u0001\u001b\u0001\u001c"+
		"\u0001\u001c\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001e\u0001\u001e"+
		"\u0001\u001e\u0001\u001e\u0001\u001e\u0005\u001e\u016c\b\u001e\n\u001e"+
		"\f\u001e\u016f\t\u001e\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001f"+
		"\u0001\u001f\u0001\u001f\u0005\u001f\u0177\b\u001f\n\u001f\f\u001f\u017a"+
		"\t\u001f\u0001\u001f\u0001\u001f\u0001\u001f\u0001 \u0001 \u0005 \u0181"+
		"\b \n \f \u0184\t \u0001 \u0004 \u0187\b \u000b \f \u0188\u0001!\u0001"+
		"!\u0001!\u0001\"\u0001\"\u0001#\u0001#\u0001#\u0001#\u0005#\u0194\b#\n"+
		"#\f#\u0197\t#\u0001#\u0001#\u0001#\u0001#\u0001#\u0003#\u019e\b#\u0001"+
		"#\u0005#\u01a1\b#\n#\f#\u01a4\t#\u0001#\u0001#\u0001#\u0001#\u0005#\u01aa"+
		"\b#\n#\f#\u01ad\t#\u0003#\u01af\b#\u0001#\u0001#\u0001$\u0001$\u0001$"+
		"\u0001$\u0001%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001%\u0005%\u01be"+
		"\b%\n%\f%\u01c1\t%\u0001%\u0005%\u01c4\b%\n%\f%\u01c7\t%\u0001%\u0001"+
		"%\u0001%\u0001&\u0001&\u0003&\u01ce\b&\u0001\'\u0001\'\u0001\'\u0005\'"+
		"\u01d3\b\'\n\'\f\'\u01d6\t\'\u0001\'\u0001\'\u0001\'\u0001(\u0001(\u0001"+
		"(\u0005(\u01de\b(\n(\f(\u01e1\t(\u0001(\u0001(\u0001(\u0001(\u0005(\u01e7"+
		"\b(\n(\f(\u01ea\t(\u0001(\u0001(\u0001(\u0001(\u0001(\u0005(\u01f1\b("+
		"\n(\f(\u01f4\t(\u0001(\u0001(\u0001(\u0001(\u0005(\u01fa\b(\n(\f(\u01fd"+
		"\t(\u0003(\u01ff\b(\u0001(\u0001(\u0001)\u0004)\u0204\b)\u000b)\f)\u0205"+
		"\u0001)\u0001)\u0001*\u0001*\u0001+\u0004+\u020d\b+\u000b+\f+\u020e\u0001"+
		"+\u0001+\u0001+\u0001+\u0001,\u0001,\u0001-\u0001-\u0001-\u0001-\u0001"+
		"-\u0001.\u0004.\u021d\b.\u000b.\f.\u021e\u0001.\u0001.\u0001/\u0001/\u0001"+
		"/\u0001/\u0005/\u0227\b/\n/\f/\u022a\t/\u0001/\u0001/\u0001/\u0001/\u0001"+
		"/\u00010\u00010\u00010\u00030\u0234\b0\u00010\u00050\u0237\b0\n0\f0\u023a"+
		"\t0\u00010\u00030\u023d\b0\u00010\u00010\u00011\u00011\u00051\u0243\b"+
		"1\n1\f1\u0246\t1\u00011\u00011\u00011\u00011\u00012\u00042\u024d\b2\u000b"+
		"2\f2\u024e\u00012\u00012\u00013\u00013\u00013\u00013\u00014\u00044\u0258"+
		"\b4\u000b4\f4\u0259\u00014\u00014\u0001\u0228\u00005\u0003\u0001\u0005"+
		"\u0002\u0007\u0003\t\u0004\u000b\u0005\r\u0006\u000f\u0007\u0011\b\u0013"+
		"\t\u0015\n\u0017\u000b\u0019\f\u001b\r\u001d\u000e\u001f\u000f!\u0010"+
		"#\u0011%\u0012\'\u0013)\u0014+\u0015-\u0016/\u00171\u00183\u00195\u001a"+
		"7\u001b9\u001c;\u0000=\u0000?\u001dA\u001eC\u0000E\u0000G\u0000I\u001f"+
		"K M!O\u0000Q\"S#U$W\u0000Y%[\u0000]\u0000_&a\'c(e\u0000g\u0000i*k)\u0003"+
		"\u0000\u0001\u0002)\u0003\u0000AZ__az\u0002\u0000\n\n\r\r\u0002\u0000"+
		"FFff\u0002\u0000RRrr\u0002\u0000OOoo\u0002\u0000MMmm\u0002\u0000UUuu\u0002"+
		"\u0000NNnn\u0002\u0000CCcc\u0002\u0000DDdd\u0002\u0000LLll\u0002\u0000"+
		"AAaa\u0002\u0000BBbb\u0002\u0000EEee\u0002\u0000XXxx\u0002\u0000PPpp\u0002"+
		"\u0000SSss\u0002\u0000VVvv\u0002\u0000YYyy\u0002\u0000TTtt\u0002\u0000"+
		"IIii\u0002\u0000WWww\u0002\u0000KKkk\u0002\u0000GGgg\u0002\u0000HHhh\u0002"+
		"\u0000\\\\``\u0002\u0000\t\t  \b\u0000\t\n\r\r  \"\"$$\'\'<=[]\u0005\u0000"+
		"\n\n\r\r\"\"\\\\``\u0003\u0000\n\n\r\r\'\'\u0003\u000009AFaf\u0004\u0000"+
		"09AZ__az\u0001\u0000}}\u0005\u0000!!#$**09?@\u0001\u0000()\u0004\u0000"+
		"\t\n\r\r  ``\u0003\u0000\n\n\r\r``\t\u0000\t\n\r\r  \"\"$$\'\'--<=[]\u0003"+
		"\u0000\t\t\f\r  \u0003\u0000\t\n\r\r  \u0001\u0000\n\n\u0286\u0000\u0003"+
		"\u0001\u0000\u0000\u0000\u0000\u0005\u0001\u0000\u0000\u0000\u0000\u0007"+
		"\u0001\u0000\u0000\u0000\u0000\t\u0001\u0000\u0000\u0000\u0000\u000b\u0001"+
		"\u0000\u0000\u0000\u0000\r\u0001\u0000\u0000\u0000\u0000\u000f\u0001\u0000"+
		"\u0000\u0000\u0000\u0011\u0001\u0000\u0000\u0000\u0000\u0013\u0001\u0000"+
		"\u0000\u0000\u0000\u0015\u0001\u0000\u0000\u0000\u0000\u0017\u0001\u0000"+
		"\u0000\u0000\u0000\u0019\u0001\u0000\u0000\u0000\u0000\u001b\u0001\u0000"+
		"\u0000\u0000\u0000\u001d\u0001\u0000\u0000\u0000\u0000\u001f\u0001\u0000"+
		"\u0000\u0000\u0000!\u0001\u0000\u0000\u0000\u0000#\u0001\u0000\u0000\u0000"+
		"\u0000%\u0001\u0000\u0000\u0000\u0000\'\u0001\u0000\u0000\u0000\u0000"+
		")\u0001\u0000\u0000\u0000\u0000+\u0001\u0000\u0000\u0000\u0000-\u0001"+
		"\u0000\u0000\u0000\u0000/\u0001\u0000\u0000\u0000\u00001\u0001\u0000\u0000"+
		"\u0000\u00003\u0001\u0000\u0000\u0000\u00005\u0001\u0000\u0000\u0000\u0000"+
		"7\u0001\u0000\u0000\u0000\u00009\u0001\u0000\u0000\u0000\u0000?\u0001"+
		"\u0000\u0000\u0000\u0000A\u0001\u0000\u0000\u0000\u0000I\u0001\u0000\u0000"+
		"\u0000\u0000K\u0001\u0000\u0000\u0000\u0000M\u0001\u0000\u0000\u0000\u0000"+
		"Q\u0001\u0000\u0000\u0000\u0000S\u0001\u0000\u0000\u0000\u0000U\u0001"+
		"\u0000\u0000\u0000\u0000Y\u0001\u0000\u0000\u0000\u0001]\u0001\u0000\u0000"+
		"\u0000\u0001_\u0001\u0000\u0000\u0000\u0001a\u0001\u0000\u0000\u0000\u0001"+
		"c\u0001\u0000\u0000\u0000\u0001e\u0001\u0000\u0000\u0000\u0001g\u0001"+
		"\u0000\u0000\u0000\u0002i\u0001\u0000\u0000\u0000\u0002k\u0001\u0000\u0000"+
		"\u0000\u0003m\u0001\u0000\u0000\u0000\u0005\u008f\u0001\u0000\u0000\u0000"+
		"\u0007\u0098\u0001\u0000\u0000\u0000\t\u009f\u0001\u0000\u0000\u0000\u000b"+
		"\u00a5\u0001\u0000\u0000\u0000\r\u00ab\u0001\u0000\u0000\u0000\u000f\u00b3"+
		"\u0001\u0000\u0000\u0000\u0011\u00bc\u0001\u0000\u0000\u0000\u0013\u00c2"+
		"\u0001\u0000\u0000\u0000\u0015\u00c8\u0001\u0000\u0000\u0000\u0017\u00cf"+
		"\u0001\u0000\u0000\u0000\u0019\u00dc\u0001\u0000\u0000\u0000\u001b\u00e5"+
		"\u0001\u0000\u0000\u0000\u001d\u00ec\u0001\u0000\u0000\u0000\u001f\u00f6"+
		"\u0001\u0000\u0000\u0000!\u00fc\u0001\u0000\u0000\u0000#\u0106\u0001\u0000"+
		"\u0000\u0000%\u0113\u0001\u0000\u0000\u0000\'\u0121\u0001\u0000\u0000"+
		"\u0000)\u0129\u0001\u0000\u0000\u0000+\u0136\u0001\u0000\u0000\u0000-"+
		"\u013b\u0001\u0000\u0000\u0000/\u0145\u0001\u0000\u0000\u00001\u0150\u0001"+
		"\u0000\u0000\u00003\u0153\u0001\u0000\u0000\u00005\u0156\u0001\u0000\u0000"+
		"\u00007\u0159\u0001\u0000\u0000\u00009\u015c\u0001\u0000\u0000\u0000;"+
		"\u0161\u0001\u0000\u0000\u0000=\u0163\u0001\u0000\u0000\u0000?\u0166\u0001"+
		"\u0000\u0000\u0000A\u0173\u0001\u0000\u0000\u0000C\u017e\u0001\u0000\u0000"+
		"\u0000E\u018a\u0001\u0000\u0000\u0000G\u018d\u0001\u0000\u0000\u0000I"+
		"\u01ae\u0001\u0000\u0000\u0000K\u01b2\u0001\u0000\u0000\u0000M\u01b6\u0001"+
		"\u0000\u0000\u0000O\u01cd\u0001\u0000\u0000\u0000Q\u01cf\u0001\u0000\u0000"+
		"\u0000S\u01fe\u0001\u0000\u0000\u0000U\u0203\u0001\u0000\u0000\u0000W"+
		"\u0209\u0001\u0000\u0000\u0000Y\u020c\u0001\u0000\u0000\u0000[\u0214\u0001"+
		"\u0000\u0000\u0000]\u0216\u0001\u0000\u0000\u0000_\u021c\u0001\u0000\u0000"+
		"\u0000a\u0222\u0001\u0000\u0000\u0000c\u0233\u0001\u0000\u0000\u0000e"+
		"\u0240\u0001\u0000\u0000\u0000g\u024c\u0001\u0000\u0000\u0000i\u0252\u0001"+
		"\u0000\u0000\u0000k\u0257\u0001\u0000\u0000\u0000mq\u0005#\u0000\u0000"+
		"np\u0003W*\u0000on\u0001\u0000\u0000\u0000ps\u0001\u0000\u0000\u0000q"+
		"o\u0001\u0000\u0000\u0000qr\u0001\u0000\u0000\u0000ru\u0001\u0000\u0000"+
		"\u0000sq\u0001\u0000\u0000\u0000tv\u0007\u0000\u0000\u0000ut\u0001\u0000"+
		"\u0000\u0000vw\u0001\u0000\u0000\u0000wu\u0001\u0000\u0000\u0000wx\u0001"+
		"\u0000\u0000\u0000x|\u0001\u0000\u0000\u0000y{\u0003W*\u0000zy\u0001\u0000"+
		"\u0000\u0000{~\u0001\u0000\u0000\u0000|z\u0001\u0000\u0000\u0000|}\u0001"+
		"\u0000\u0000\u0000}\u007f\u0001\u0000\u0000\u0000~|\u0001\u0000\u0000"+
		"\u0000\u007f\u0083\u0005=\u0000\u0000\u0080\u0082\u0003W*\u0000\u0081"+
		"\u0080\u0001\u0000\u0000\u0000\u0082\u0085\u0001\u0000\u0000\u0000\u0083"+
		"\u0081\u0001\u0000\u0000\u0000\u0083\u0084\u0001\u0000\u0000\u0000\u0084"+
		"\u0089\u0001\u0000\u0000\u0000\u0085\u0083\u0001\u0000\u0000\u0000\u0086"+
		"\u0088\b\u0001\u0000\u0000\u0087\u0086\u0001\u0000\u0000\u0000\u0088\u008b"+
		"\u0001\u0000\u0000\u0000\u0089\u0087\u0001\u0000\u0000\u0000\u0089\u008a"+
		"\u0001\u0000\u0000\u0000\u008a\u008c\u0001\u0000\u0000\u0000\u008b\u0089"+
		"\u0001\u0000\u0000\u0000\u008c\u008d\u0003[,\u0000\u008d\u008e\u0006\u0000"+
		"\u0000\u0000\u008e\u0004\u0001\u0000\u0000\u0000\u008f\u0093\u0005#\u0000"+
		"\u0000\u0090\u0092\b\u0001\u0000\u0000\u0091\u0090\u0001\u0000\u0000\u0000"+
		"\u0092\u0095\u0001\u0000\u0000\u0000\u0093\u0091\u0001\u0000\u0000\u0000"+
		"\u0093\u0094\u0001\u0000\u0000\u0000\u0094\u0096\u0001\u0000\u0000\u0000"+
		"\u0095\u0093\u0001\u0000\u0000\u0000\u0096\u0097\u0006\u0001\u0001\u0000"+
		"\u0097\u0006\u0001\u0000\u0000\u0000\u0098\u0099\u0007\u0002\u0000\u0000"+
		"\u0099\u009a\u0007\u0003\u0000\u0000\u009a\u009b\u0007\u0004\u0000\u0000"+
		"\u009b\u009c\u0007\u0005\u0000\u0000\u009c\u009d\u0001\u0000\u0000\u0000"+
		"\u009d\u009e\u0006\u0002\u0002\u0000\u009e\b\u0001\u0000\u0000\u0000\u009f"+
		"\u00a0\u0007\u0003\u0000\u0000\u00a0\u00a1\u0007\u0006\u0000\u0000\u00a1"+
		"\u00a2\u0007\u0007\u0000\u0000\u00a2\u00a3\u0001\u0000\u0000\u0000\u00a3"+
		"\u00a4\u0006\u0003\u0003\u0000\u00a4\n\u0001\u0000\u0000\u0000\u00a5\u00a6"+
		"\u0007\b\u0000\u0000\u00a6\u00a7\u0007\u0005\u0000\u0000\u00a7\u00a8\u0007"+
		"\t\u0000\u0000\u00a8\u00a9\u0001\u0000\u0000\u0000\u00a9\u00aa\u0006\u0004"+
		"\u0004\u0000\u00aa\f\u0001\u0000\u0000\u0000\u00ab\u00ac\u0007\n\u0000"+
		"\u0000\u00ac\u00ad\u0007\u000b\u0000\u0000\u00ad\u00ae\u0007\f\u0000\u0000"+
		"\u00ae\u00af\u0007\r\u0000\u0000\u00af\u00b0\u0007\n\u0000\u0000\u00b0"+
		"\u00b1\u0001\u0000\u0000\u0000\u00b1\u00b2\u0006\u0005\u0005\u0000\u00b2"+
		"\u000e\u0001\u0000\u0000\u0000\u00b3\u00b4\u0007\r\u0000\u0000\u00b4\u00b5"+
		"\u0007\u000e\u0000\u0000\u00b5\u00b6\u0007\u000f\u0000\u0000\u00b6\u00b7"+
		"\u0007\u0004\u0000\u0000\u00b7\u00b8\u0007\u0010\u0000\u0000\u00b8\u00b9"+
		"\u0007\r\u0000\u0000\u00b9\u00ba\u0001\u0000\u0000\u0000\u00ba\u00bb\u0006"+
		"\u0006\u0006\u0000\u00bb\u0010\u0001\u0000\u0000\u0000\u00bc\u00bd\u0007"+
		"\r\u0000\u0000\u00bd\u00be\u0007\u0007\u0000\u0000\u00be\u00bf\u0007\u0011"+
		"\u0000\u0000\u00bf\u00c0\u0001\u0000\u0000\u0000\u00c0\u00c1\u0006\u0007"+
		"\u0007\u0000\u00c1\u0012\u0001\u0000\u0000\u0000\u00c2\u00c3\u0007\u000b"+
		"\u0000\u0000\u00c3\u00c4\u0007\t\u0000\u0000\u00c4\u00c5\u0007\t\u0000"+
		"\u0000\u00c5\u00c6\u0001\u0000\u0000\u0000\u00c6\u00c7\u0006\b\b\u0000"+
		"\u00c7\u0014\u0001\u0000\u0000\u0000\u00c8\u00c9\u0007\b\u0000\u0000\u00c9"+
		"\u00ca\u0007\u0004\u0000\u0000\u00ca\u00cb\u0007\u000f\u0000\u0000\u00cb"+
		"\u00cc\u0007\u0012\u0000\u0000\u00cc\u00cd\u0001\u0000\u0000\u0000\u00cd"+
		"\u00ce\u0006\t\t\u0000\u00ce\u0016\u0001\u0000\u0000\u0000\u00cf\u00d0"+
		"\u0007\r\u0000\u0000\u00d0\u00d1\u0007\u0007\u0000\u0000\u00d1\u00d2\u0007"+
		"\u0013\u0000\u0000\u00d2\u00d3\u0007\u0003\u0000\u0000\u00d3\u00d4\u0007"+
		"\u0012\u0000\u0000\u00d4\u00d5\u0007\u000f\u0000\u0000\u00d5\u00d6\u0007"+
		"\u0004\u0000\u0000\u00d6\u00d7\u0007\u0014\u0000\u0000\u00d7\u00d8\u0007"+
		"\u0007\u0000\u0000\u00d8\u00d9\u0007\u0013\u0000\u0000\u00d9\u00da\u0001"+
		"\u0000\u0000\u0000\u00da\u00db\u0006\n\n\u0000\u00db\u0018\u0001\u0000"+
		"\u0000\u0000\u00dc\u00dd\u0007\u0011\u0000\u0000\u00dd\u00de\u0007\u0004"+
		"\u0000\u0000\u00de\u00df\u0007\n\u0000\u0000\u00df\u00e0\u0007\u0006\u0000"+
		"\u0000\u00e0\u00e1\u0007\u0005\u0000\u0000\u00e1\u00e2\u0007\r\u0000\u0000"+
		"\u00e2\u00e3\u0001\u0000\u0000\u0000\u00e3\u00e4\u0006\u000b\u000b\u0000"+
		"\u00e4\u001a\u0001\u0000\u0000\u0000\u00e5\u00e6\u0007\u0006\u0000\u0000"+
		"\u00e6\u00e7\u0007\u0010\u0000\u0000\u00e7\u00e8\u0007\r\u0000\u0000\u00e8"+
		"\u00e9\u0007\u0003\u0000\u0000\u00e9\u00ea\u0001\u0000\u0000\u0000\u00ea"+
		"\u00eb\u0006\f\f\u0000\u00eb\u001c\u0001\u0000\u0000\u0000\u00ec\u00ed"+
		"\u0007\u0015\u0000\u0000\u00ed\u00ee\u0007\u0004\u0000\u0000\u00ee\u00ef"+
		"\u0007\u0003\u0000\u0000\u00ef\u00f0\u0007\u0016\u0000\u0000\u00f0\u00f1"+
		"\u0007\t\u0000\u0000\u00f1\u00f2\u0007\u0014\u0000\u0000\u00f2\u00f3\u0007"+
		"\u0003\u0000\u0000\u00f3\u00f4\u0001\u0000\u0000\u0000\u00f4\u00f5\u0006"+
		"\r\r\u0000\u00f5\u001e\u0001\u0000\u0000\u0000\u00f6\u00f7\u0007\u000b"+
		"\u0000\u0000\u00f7\u00f8\u0007\u0003\u0000\u0000\u00f8\u00f9\u0007\u0017"+
		"\u0000\u0000\u00f9\u00fa\u0001\u0000\u0000\u0000\u00fa\u00fb\u0006\u000e"+
		"\u000e\u0000\u00fb \u0001\u0000\u0000\u0000\u00fc\u00fd\u0007\u0004\u0000"+
		"\u0000\u00fd\u00fe\u0007\u0007\u0000\u0000\u00fe\u00ff\u0007\f\u0000\u0000"+
		"\u00ff\u0100\u0007\u0006\u0000\u0000\u0100\u0101\u0007\u0014\u0000\u0000"+
		"\u0101\u0102\u0007\n\u0000\u0000\u0102\u0103\u0007\t\u0000\u0000\u0103"+
		"\u0104\u0001\u0000\u0000\u0000\u0104\u0105\u0006\u000f\u000f\u0000\u0105"+
		"\"\u0001\u0000\u0000\u0000\u0106\u0107\u0007\u0010\u0000\u0000\u0107\u0108"+
		"\u0007\u0013\u0000\u0000\u0108\u0109\u0007\u0004\u0000\u0000\u0109\u010a"+
		"\u0007\u000f\u0000\u0000\u010a\u010b\u0007\u0010\u0000\u0000\u010b\u010c"+
		"\u0007\u0014\u0000\u0000\u010c\u010d\u0007\u0017\u0000\u0000\u010d\u010e"+
		"\u0007\u0007\u0000\u0000\u010e\u010f\u0007\u000b\u0000\u0000\u010f\u0110"+
		"\u0007\n\u0000\u0000\u0110\u0111\u0001\u0000\u0000\u0000\u0111\u0112\u0006"+
		"\u0010\u0010\u0000\u0112$\u0001\u0000\u0000\u0000\u0113\u0114\u0007\u0018"+
		"\u0000\u0000\u0114\u0115\u0007\r\u0000\u0000\u0115\u0116\u0007\u000b\u0000"+
		"\u0000\u0116\u0117\u0007\n\u0000\u0000\u0117\u0118\u0007\u0013\u0000\u0000"+
		"\u0118\u0119\u0007\u0018\u0000\u0000\u0119\u011a\u0007\b\u0000\u0000\u011a"+
		"\u011b\u0007\u0018\u0000\u0000\u011b\u011c\u0007\r\u0000\u0000\u011c\u011d"+
		"\u0007\b\u0000\u0000\u011d\u011e\u0007\u0016\u0000\u0000\u011e\u011f\u0001"+
		"\u0000\u0000\u0000\u011f\u0120\u0006\u0011\u0011\u0000\u0120&\u0001\u0000"+
		"\u0000\u0000\u0121\u0122\u0007\u0010\u0000\u0000\u0122\u0123\u0007\u0018"+
		"\u0000\u0000\u0123\u0124\u0007\r\u0000\u0000\u0124\u0125\u0007\n\u0000"+
		"\u0000\u0125\u0126\u0007\n\u0000\u0000\u0126\u0127\u0001\u0000\u0000\u0000"+
		"\u0127\u0128\u0006\u0012\u0012\u0000\u0128(\u0001\u0000\u0000\u0000\u0129"+
		"\u012a\u0007\u0005\u0000\u0000\u012a\u012b\u0007\u000b\u0000\u0000\u012b"+
		"\u012c\u0007\u0014\u0000\u0000\u012c\u012d\u0007\u0007\u0000\u0000\u012d"+
		"\u012e\u0007\u0013\u0000\u0000\u012e\u012f\u0007\u000b\u0000\u0000\u012f"+
		"\u0130\u0007\u0014\u0000\u0000\u0130\u0131\u0007\u0007\u0000\u0000\u0131"+
		"\u0132\u0007\r\u0000\u0000\u0132\u0133\u0007\u0003\u0000\u0000\u0133\u0134"+
		"\u0001\u0000\u0000\u0000\u0134\u0135\u0006\u0013\u0013\u0000\u0135*\u0001"+
		"\u0000\u0000\u0000\u0136\u0137\u0007\u000b\u0000\u0000\u0137\u0138\u0007"+
		"\u0010\u0000\u0000\u0138\u0139\u0001\u0000\u0000\u0000\u0139\u013a\u0006"+
		"\u0014\u0014\u0000\u013a,\u0001\u0000\u0000\u0000\u013b\u013c\u0005<\u0000"+
		"\u0000\u013c\u013d\u0005<\u0000\u0000\u013d\u013f\u0001\u0000\u0000\u0000"+
		"\u013e\u0140\u0005-\u0000\u0000\u013f\u013e\u0001\u0000\u0000\u0000\u013f"+
		"\u0140\u0001\u0000\u0000\u0000\u0140\u0141\u0001\u0000\u0000\u0000\u0141"+
		"\u0142\u0006\u0015\u0015\u0000\u0142\u0143\u0001\u0000\u0000\u0000\u0143"+
		"\u0144\u0006\u0015\u0016\u0000\u0144.\u0001\u0000\u0000\u0000\u0145\u0149"+
		"\u0007\u0019\u0000\u0000\u0146\u0148\u0007\u001a\u0000\u0000\u0147\u0146"+
		"\u0001\u0000\u0000\u0000\u0148\u014b\u0001\u0000\u0000\u0000\u0149\u0147"+
		"\u0001\u0000\u0000\u0000\u0149\u014a\u0001\u0000\u0000\u0000\u014a\u014c"+
		"\u0001\u0000\u0000\u0000\u014b\u0149\u0001\u0000\u0000\u0000\u014c\u014d"+
		"\u0003[,\u0000\u014d\u014e\u0001\u0000\u0000\u0000\u014e\u014f\u0006\u0016"+
		"\u0001\u0000\u014f0\u0001\u0000\u0000\u0000\u0150\u0151\u0005[\u0000\u0000"+
		"\u0151\u0152\u0006\u0017\u0017\u0000\u01522\u0001\u0000\u0000\u0000\u0153"+
		"\u0154\u0005]\u0000\u0000\u0154\u0155\u0006\u0018\u0018\u0000\u01554\u0001"+
		"\u0000\u0000\u0000\u0156\u0157\u0005,\u0000\u0000\u0157\u0158\u0006\u0019"+
		"\u0019\u0000\u01586\u0001\u0000\u0000\u0000\u0159\u015a\u0005=\u0000\u0000"+
		"\u015a\u015b\u0006\u001a\u001a\u0000\u015b8\u0001\u0000\u0000\u0000\u015c"+
		"\u015d\u0005-\u0000\u0000\u015d\u015e\u0005-\u0000\u0000\u015e\u015f\u0001"+
		"\u0000\u0000\u0000\u015f\u0160\u0006\u001b\u001b\u0000\u0160:\u0001\u0000"+
		"\u0000\u0000\u0161\u0162\b\u001b\u0000\u0000\u0162<\u0001\u0000\u0000"+
		"\u0000\u0163\u0164\u0005\\\u0000\u0000\u0164\u0165\t\u0000\u0000\u0000"+
		"\u0165>\u0001\u0000\u0000\u0000\u0166\u016d\u0005\"\u0000\u0000\u0167"+
		"\u016c\u0003E!\u0000\u0168\u016c\u0003C \u0000\u0169\u016c\u0005`\u0000"+
		"\u0000\u016a\u016c\b\u001c\u0000\u0000\u016b\u0167\u0001\u0000\u0000\u0000"+
		"\u016b\u0168\u0001\u0000\u0000\u0000\u016b\u0169\u0001\u0000\u0000\u0000"+
		"\u016b\u016a\u0001\u0000\u0000\u0000\u016c\u016f\u0001\u0000\u0000\u0000"+
		"\u016d\u016b\u0001\u0000\u0000\u0000\u016d\u016e\u0001\u0000\u0000\u0000"+
		"\u016e\u0170\u0001\u0000\u0000\u0000\u016f\u016d\u0001\u0000\u0000\u0000"+
		"\u0170\u0171\u0005\"\u0000\u0000\u0171\u0172\u0006\u001e\u001c\u0000\u0172"+
		"@\u0001\u0000\u0000\u0000\u0173\u0178\u0005\'\u0000\u0000\u0174\u0177"+
		"\u0003C \u0000\u0175\u0177\b\u001d\u0000\u0000\u0176\u0174\u0001\u0000"+
		"\u0000\u0000\u0176\u0175\u0001\u0000\u0000\u0000\u0177\u017a\u0001\u0000"+
		"\u0000\u0000\u0178\u0176\u0001\u0000\u0000\u0000\u0178\u0179\u0001\u0000"+
		"\u0000\u0000\u0179\u017b\u0001\u0000\u0000\u0000\u017a\u0178\u0001\u0000"+
		"\u0000\u0000\u017b\u017c\u0005\'\u0000\u0000\u017c\u017d\u0006\u001f\u001d"+
		"\u0000\u017dB\u0001\u0000\u0000\u0000\u017e\u0182\u0007\u0019\u0000\u0000"+
		"\u017f\u0181\u0007\u001a\u0000\u0000\u0180\u017f\u0001\u0000\u0000\u0000"+
		"\u0181\u0184\u0001\u0000\u0000\u0000\u0182\u0180\u0001\u0000\u0000\u0000"+
		"\u0182\u0183\u0001\u0000\u0000\u0000\u0183\u0186\u0001\u0000\u0000\u0000"+
		"\u0184\u0182\u0001\u0000\u0000\u0000\u0185\u0187\u0007\u0001\u0000\u0000"+
		"\u0186\u0185\u0001\u0000\u0000\u0000\u0187\u0188\u0001\u0000\u0000\u0000"+
		"\u0188\u0186\u0001\u0000\u0000\u0000\u0188\u0189\u0001\u0000\u0000\u0000"+
		"\u0189D\u0001\u0000\u0000\u0000\u018a\u018b\u0005\\\u0000\u0000\u018b"+
		"\u018c\b\u0001\u0000\u0000\u018cF\u0001\u0000\u0000\u0000\u018d\u018e"+
		"\u0007\u001e\u0000\u0000\u018eH\u0001\u0000\u0000\u0000\u018f\u0190\u0005"+
		"$\u0000\u0000\u0190\u0191\u0005{\u0000\u0000\u0191\u0195\u0007\u0000\u0000"+
		"\u0000\u0192\u0194\u0007\u001f\u0000\u0000\u0193\u0192\u0001\u0000\u0000"+
		"\u0000\u0194\u0197\u0001\u0000\u0000\u0000\u0195\u0193\u0001\u0000\u0000"+
		"\u0000\u0195\u0196\u0001\u0000\u0000\u0000\u0196\u019d\u0001\u0000\u0000"+
		"\u0000\u0197\u0195\u0001\u0000\u0000\u0000\u0198\u0199\u0005:\u0000\u0000"+
		"\u0199\u019e\u0005-\u0000\u0000\u019a\u019b\u0005:\u0000\u0000\u019b\u019e"+
		"\u0005+\u0000\u0000\u019c\u019e\u0005:\u0000\u0000\u019d\u0198\u0001\u0000"+
		"\u0000\u0000\u019d\u019a\u0001\u0000\u0000\u0000\u019d\u019c\u0001\u0000"+
		"\u0000\u0000\u019d\u019e\u0001\u0000\u0000\u0000\u019e\u01a2\u0001\u0000"+
		"\u0000\u0000\u019f\u01a1\b \u0000\u0000\u01a0\u019f\u0001\u0000\u0000"+
		"\u0000\u01a1\u01a4\u0001\u0000\u0000\u0000\u01a2\u01a0\u0001\u0000\u0000"+
		"\u0000\u01a2\u01a3\u0001\u0000\u0000\u0000\u01a3\u01a5\u0001\u0000\u0000"+
		"\u0000\u01a4\u01a2\u0001\u0000\u0000\u0000\u01a5\u01af\u0005}\u0000\u0000"+
		"\u01a6\u01a7\u0005$\u0000\u0000\u01a7\u01ab\u0007\u0000\u0000\u0000\u01a8"+
		"\u01aa\u0007\u001f\u0000\u0000\u01a9\u01a8\u0001\u0000\u0000\u0000\u01aa"+
		"\u01ad\u0001\u0000\u0000\u0000\u01ab\u01a9\u0001\u0000\u0000\u0000\u01ab"+
		"\u01ac\u0001\u0000\u0000\u0000\u01ac\u01af\u0001\u0000\u0000\u0000\u01ad"+
		"\u01ab\u0001\u0000\u0000\u0000\u01ae\u018f\u0001\u0000\u0000\u0000\u01ae"+
		"\u01a6\u0001\u0000\u0000\u0000\u01af\u01b0\u0001\u0000\u0000\u0000\u01b0"+
		"\u01b1\u0006#\u001e\u0000\u01b1J\u0001\u0000\u0000\u0000\u01b2\u01b3\u0005"+
		"$\u0000\u0000\u01b3\u01b4\u0007!\u0000\u0000\u01b4\u01b5\u0006$\u001f"+
		"\u0000\u01b5L\u0001\u0000\u0000\u0000\u01b6\u01b7\u0005$\u0000\u0000\u01b7"+
		"\u01b8\u0005(\u0000\u0000\u01b8\u01c5\u0001\u0000\u0000\u0000\u01b9\u01c4"+
		"\u0003M%\u0000\u01ba\u01c4\b\"\u0000\u0000\u01bb\u01bf\u0005(\u0000\u0000"+
		"\u01bc\u01be\u0003O&\u0000\u01bd\u01bc\u0001\u0000\u0000\u0000\u01be\u01c1"+
		"\u0001\u0000\u0000\u0000\u01bf\u01bd\u0001\u0000\u0000\u0000\u01bf\u01c0"+
		"\u0001\u0000\u0000\u0000\u01c0\u01c2\u0001\u0000\u0000\u0000\u01c1\u01bf"+
		"\u0001\u0000\u0000\u0000\u01c2\u01c4\u0005)\u0000\u0000\u01c3\u01b9\u0001"+
		"\u0000\u0000\u0000\u01c3\u01ba\u0001\u0000\u0000\u0000\u01c3\u01bb\u0001"+
		"\u0000\u0000\u0000\u01c4\u01c7\u0001\u0000\u0000\u0000\u01c5\u01c3\u0001"+
		"\u0000\u0000\u0000\u01c5\u01c6\u0001\u0000\u0000\u0000\u01c6\u01c8\u0001"+
		"\u0000\u0000\u0000\u01c7\u01c5\u0001\u0000\u0000\u0000\u01c8\u01c9\u0005"+
		")\u0000\u0000\u01c9\u01ca\u0006% \u0000\u01caN\u0001\u0000\u0000\u0000"+
		"\u01cb\u01ce\u0003M%\u0000\u01cc\u01ce\b\"\u0000\u0000\u01cd\u01cb\u0001"+
		"\u0000\u0000\u0000\u01cd\u01cc\u0001\u0000\u0000\u0000\u01ceP\u0001\u0000"+
		"\u0000\u0000\u01cf\u01d0\u0005`\u0000\u0000\u01d0\u01d4\b#\u0000\u0000"+
		"\u01d1\u01d3\b$\u0000\u0000\u01d2\u01d1\u0001\u0000\u0000\u0000\u01d3"+
		"\u01d6\u0001\u0000\u0000\u0000\u01d4\u01d2\u0001\u0000\u0000\u0000\u01d4"+
		"\u01d5\u0001\u0000\u0000\u0000\u01d5\u01d7\u0001\u0000\u0000\u0000\u01d6"+
		"\u01d4\u0001\u0000\u0000\u0000\u01d7\u01d8\u0005`\u0000\u0000\u01d8\u01d9"+
		"\u0006\'!\u0000\u01d9R\u0001\u0000\u0000\u0000\u01da\u01df\b%\u0000\u0000"+
		"\u01db\u01de\u0003;\u001c\u0000\u01dc\u01de\u0003=\u001d\u0000\u01dd\u01db"+
		"\u0001\u0000\u0000\u0000\u01dd\u01dc\u0001\u0000\u0000\u0000\u01de\u01e1"+
		"\u0001\u0000\u0000\u0000\u01df\u01dd\u0001\u0000\u0000\u0000\u01df\u01e0"+
		"\u0001\u0000\u0000\u0000\u01e0\u01ff\u0001\u0000\u0000\u0000\u01e1\u01df"+
		"\u0001\u0000\u0000\u0000\u01e2\u01e3\u0005-\u0000\u0000\u01e3\u01e8\b"+
		"%\u0000\u0000\u01e4\u01e7\u0003;\u001c\u0000\u01e5\u01e7\u0003=\u001d"+
		"\u0000\u01e6\u01e4\u0001\u0000\u0000\u0000\u01e6\u01e5\u0001\u0000\u0000"+
		"\u0000\u01e7\u01ea\u0001\u0000\u0000\u0000\u01e8\u01e6\u0001\u0000\u0000"+
		"\u0000\u01e8\u01e9\u0001\u0000\u0000\u0000\u01e9\u01ff\u0001\u0000\u0000"+
		"\u0000\u01ea\u01e8\u0001\u0000\u0000\u0000\u01eb\u01ff\u0005-\u0000\u0000"+
		"\u01ec\u01ed\u0005<\u0000\u0000\u01ed\u01f2\b\u001b\u0000\u0000\u01ee"+
		"\u01f1\u0003;\u001c\u0000\u01ef\u01f1\u0003=\u001d\u0000\u01f0\u01ee\u0001"+
		"\u0000\u0000\u0000\u01f0\u01ef\u0001\u0000\u0000\u0000\u01f1\u01f4\u0001"+
		"\u0000\u0000\u0000\u01f2\u01f0\u0001\u0000\u0000\u0000\u01f2\u01f3\u0001"+
		"\u0000\u0000\u0000\u01f3\u01ff\u0001\u0000\u0000\u0000\u01f4\u01f2\u0001"+
		"\u0000\u0000\u0000\u01f5\u01ff\u0005<\u0000\u0000\u01f6\u01fb\u0003=\u001d"+
		"\u0000\u01f7\u01fa\u0003;\u001c\u0000\u01f8\u01fa\u0003=\u001d\u0000\u01f9"+
		"\u01f7\u0001\u0000\u0000\u0000\u01f9\u01f8\u0001\u0000\u0000\u0000\u01fa"+
		"\u01fd\u0001\u0000\u0000\u0000\u01fb\u01f9\u0001\u0000\u0000\u0000\u01fb"+
		"\u01fc\u0001\u0000\u0000\u0000\u01fc\u01ff\u0001\u0000\u0000\u0000\u01fd"+
		"\u01fb\u0001\u0000\u0000\u0000\u01fe\u01da\u0001\u0000\u0000\u0000\u01fe"+
		"\u01e2\u0001\u0000\u0000\u0000\u01fe\u01eb\u0001\u0000\u0000\u0000\u01fe"+
		"\u01ec\u0001\u0000\u0000\u0000\u01fe\u01f5\u0001\u0000\u0000\u0000\u01fe"+
		"\u01f6\u0001\u0000\u0000\u0000\u01ff\u0200\u0001\u0000\u0000\u0000\u0200"+
		"\u0201\u0006(\"\u0000\u0201T\u0001\u0000\u0000\u0000\u0202\u0204\u0003"+
		"W*\u0000\u0203\u0202\u0001\u0000\u0000\u0000\u0204\u0205\u0001\u0000\u0000"+
		"\u0000\u0205\u0203\u0001\u0000\u0000\u0000\u0205\u0206\u0001\u0000\u0000"+
		"\u0000\u0206\u0207\u0001\u0000\u0000\u0000\u0207\u0208\u0006)\u0001\u0000"+
		"\u0208V\u0001\u0000\u0000\u0000\u0209\u020a\u0007\u001a\u0000\u0000\u020a"+
		"X\u0001\u0000\u0000\u0000\u020b\u020d\u0003[,\u0000\u020c\u020b\u0001"+
		"\u0000\u0000\u0000\u020d\u020e\u0001\u0000\u0000\u0000\u020e\u020c\u0001"+
		"\u0000\u0000\u0000\u020e\u020f\u0001\u0000\u0000\u0000\u020f\u0210\u0001"+
		"\u0000\u0000\u0000\u0210\u0211\u0006+#\u0000\u0211\u0212\u0001\u0000\u0000"+
		"\u0000\u0212\u0213\u0006+\u0001\u0000\u0213Z\u0001\u0000\u0000\u0000\u0214"+
		"\u0215\u0007\u0001\u0000\u0000\u0215\\\u0001\u0000\u0000\u0000\u0216\u0217"+
		"\u0005\n\u0000\u0000\u0217\u0218\u0001\u0000\u0000\u0000\u0218\u0219\u0006"+
		"-$\u0000\u0219\u021a\u0006-%\u0000\u021a^\u0001\u0000\u0000\u0000\u021b"+
		"\u021d\u0007&\u0000\u0000\u021c\u021b\u0001\u0000\u0000\u0000\u021d\u021e"+
		"\u0001\u0000\u0000\u0000\u021e\u021c\u0001\u0000\u0000\u0000\u021e\u021f"+
		"\u0001\u0000\u0000\u0000\u021f\u0220\u0001\u0000\u0000\u0000\u0220\u0221"+
		"\u0006.\u0001\u0000\u0221`\u0001\u0000\u0000\u0000\u0222\u0223\u0005/"+
		"\u0000\u0000\u0223\u0224\u0005*\u0000\u0000\u0224\u0228\u0001\u0000\u0000"+
		"\u0000\u0225\u0227\t\u0000\u0000\u0000\u0226\u0225\u0001\u0000\u0000\u0000"+
		"\u0227\u022a\u0001\u0000\u0000\u0000\u0228\u0229\u0001\u0000\u0000\u0000"+
		"\u0228\u0226\u0001\u0000\u0000\u0000\u0229\u022b\u0001\u0000\u0000\u0000"+
		"\u022a\u0228\u0001\u0000\u0000\u0000\u022b\u022c\u0005*\u0000\u0000\u022c"+
		"\u022d\u0005/\u0000\u0000\u022d\u022e\u0001\u0000\u0000\u0000\u022e\u022f"+
		"\u0006/\u0001\u0000\u022fb\u0001\u0000\u0000\u0000\u0230\u0231\u0005/"+
		"\u0000\u0000\u0231\u0234\u0005/\u0000\u0000\u0232\u0234\u0005#\u0000\u0000"+
		"\u0233\u0230\u0001\u0000\u0000\u0000\u0233\u0232\u0001\u0000\u0000\u0000"+
		"\u0234\u0238\u0001\u0000\u0000\u0000\u0235\u0237\b\u0001\u0000\u0000\u0236"+
		"\u0235\u0001\u0000\u0000\u0000\u0237\u023a\u0001\u0000\u0000\u0000\u0238"+
		"\u0236\u0001\u0000\u0000\u0000\u0238\u0239\u0001\u0000\u0000\u0000\u0239"+
		"\u023c\u0001\u0000\u0000\u0000\u023a\u0238\u0001\u0000\u0000\u0000\u023b"+
		"\u023d\u0005\r\u0000\u0000\u023c\u023b\u0001\u0000\u0000\u0000\u023c\u023d"+
		"\u0001\u0000\u0000\u0000\u023d\u023e\u0001\u0000\u0000\u0000\u023e\u023f"+
		"\u00060\u0001\u0000\u023fd\u0001\u0000\u0000\u0000\u0240\u0244\u0007\u0000"+
		"\u0000\u0000\u0241\u0243\u0007\u001f\u0000\u0000\u0242\u0241\u0001\u0000"+
		"\u0000\u0000\u0243\u0246\u0001\u0000\u0000\u0000\u0244\u0242\u0001\u0000"+
		"\u0000\u0000\u0244\u0245\u0001\u0000\u0000\u0000\u0245\u0247\u0001\u0000"+
		"\u0000\u0000\u0246\u0244\u0001\u0000\u0000\u0000\u0247\u0248\u00061&\u0000"+
		"\u0248\u0249\u0001\u0000\u0000\u0000\u0249\u024a\u00061\'\u0000\u024a"+
		"f\u0001\u0000\u0000\u0000\u024b\u024d\b\'\u0000\u0000\u024c\u024b\u0001"+
		"\u0000\u0000\u0000\u024d\u024e\u0001\u0000\u0000\u0000\u024e\u024c\u0001"+
		"\u0000\u0000\u0000\u024e\u024f\u0001\u0000\u0000\u0000\u024f\u0250\u0001"+
		"\u0000\u0000\u0000\u0250\u0251\u00062\'\u0000\u0251h\u0001\u0000\u0000"+
		"\u0000\u0252\u0253\u0005\n\u0000\u0000\u0253\u0254\u0001\u0000\u0000\u0000"+
		"\u0254\u0255\u00063$\u0000\u0255j\u0001\u0000\u0000\u0000\u0256\u0258"+
		"\b(\u0000\u0000\u0257\u0256\u0001\u0000\u0000\u0000\u0258\u0259\u0001"+
		"\u0000\u0000\u0000\u0259\u0257\u0001\u0000\u0000\u0000\u0259\u025a\u0001"+
		"\u0000\u0000\u0000\u025a\u025b\u0001\u0000\u0000\u0000\u025b\u025c\u0006"+
		"4(\u0000\u025cl\u0001\u0000\u0000\u0000.\u0000\u0001\u0002qw|\u0083\u0089"+
		"\u0093\u013f\u0149\u016b\u016d\u0176\u0178\u0182\u0188\u0195\u019d\u01a2"+
		"\u01ab\u01ae\u01bf\u01c3\u01c5\u01cd\u01d4\u01dd\u01df\u01e6\u01e8\u01f0"+
		"\u01f2\u01f9\u01fb\u01fe\u0205\u020e\u021e\u0228\u0233\u0238\u023c\u0244"+
		"\u024e\u0259)\u0001\u0000\u0000\u0000\u0001\u0000\u0001\u0002\u0001\u0001"+
		"\u0003\u0002\u0001\u0004\u0003\u0001\u0005\u0004\u0001\u0006\u0005\u0001"+
		"\u0007\u0006\u0001\b\u0007\u0001\t\b\u0001\n\t\u0001\u000b\n\u0001\f\u000b"+
		"\u0001\r\f\u0001\u000e\r\u0001\u000f\u000e\u0001\u0010\u000f\u0001\u0011"+
		"\u0010\u0001\u0012\u0011\u0001\u0013\u0012\u0001\u0014\u0013\u0001\u0015"+
		"\u0014\u0005\u0001\u0000\u0001\u0017\u0015\u0001\u0018\u0016\u0001\u0019"+
		"\u0017\u0001\u001a\u0018\u0001\u001b\u0019\u0001\u001e\u001a\u0001\u001f"+
		"\u001b\u0001#\u001c\u0001$\u001d\u0001%\u001e\u0001\'\u001f\u0001( \u0001"+
		"+!\u0007%\u0000\u0002\u0002\u0000\u00011\"\u0007#\u0000\u00014#";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}