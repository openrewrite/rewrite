// Generated from /Users/jon/Projects/github/openrewrite/rewrite/rewrite-java/src/main/antlr/MethodSignatureLexer.g4 by ANTLR 4.13.2
package org.openrewrite.java.internal.grammar;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class MethodSignatureLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		CONSTRUCTOR=1, LPAREN=2, RPAREN=3, LBRACK=4, RBRACK=5, COMMA=6, DOT=7, 
		BANG=8, WILDCARD=9, AND=10, OR=11, ELLIPSIS=12, DOTDOT=13, POUND=14, SPACE=15, 
		Identifier=16;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"CONSTRUCTOR", "LPAREN", "RPAREN", "LBRACK", "RBRACK", "COMMA", "DOT", 
			"BANG", "WILDCARD", "AND", "OR", "ELLIPSIS", "DOTDOT", "POUND", "SPACE", 
			"Identifier", "JavaLetter", "JavaLetterOrDigit"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, "'('", "')'", "'['", "']'", "','", null, "'!'", "'*'", "'&&'", 
			"'||'", "'...'", "'..'", "'#'", "' '"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "CONSTRUCTOR", "LPAREN", "RPAREN", "LBRACK", "RBRACK", "COMMA", 
			"DOT", "BANG", "WILDCARD", "AND", "OR", "ELLIPSIS", "DOTDOT", "POUND", 
			"SPACE", "Identifier"
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


	public MethodSignatureLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "MethodSignatureLexer.g4"; }

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
	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 16:
			return JavaLetter_sempred((RuleContext)_localctx, predIndex);
		case 17:
			return JavaLetterOrDigit_sempred((RuleContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean JavaLetter_sempred(RuleContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return Character.isJavaIdentifierStart(_input.LA(-1));
		case 1:
			return Character.isJavaIdentifierStart(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)));
		}
		return true;
	}
	private boolean JavaLetterOrDigit_sempred(RuleContext _localctx, int predIndex) {
		switch (predIndex) {
		case 2:
			return Character.isJavaIdentifierPart(_input.LA(-1));
		case 3:
			return Character.isJavaIdentifierPart(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)));
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u0000\u0010r\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002\u0001"+
		"\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004"+
		"\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007"+
		"\u0007\u0007\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b"+
		"\u0007\u000b\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002"+
		"\u000f\u0007\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001"+
		"\u0000\u0003\u00009\b\u0000\u0001\u0001\u0001\u0001\u0001\u0002\u0001"+
		"\u0002\u0001\u0003\u0001\u0003\u0001\u0004\u0001\u0004\u0001\u0005\u0001"+
		"\u0005\u0001\u0006\u0001\u0006\u0001\u0007\u0001\u0007\u0001\b\u0001\b"+
		"\u0001\t\u0001\t\u0001\t\u0001\n\u0001\n\u0001\n\u0001\u000b\u0001\u000b"+
		"\u0001\u000b\u0001\u000b\u0001\f\u0001\f\u0001\f\u0001\r\u0001\r\u0001"+
		"\u000e\u0001\u000e\u0001\u000f\u0001\u000f\u0005\u000f^\b\u000f\n\u000f"+
		"\f\u000fa\t\u000f\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001"+
		"\u0010\u0001\u0010\u0003\u0010i\b\u0010\u0001\u0011\u0001\u0011\u0001"+
		"\u0011\u0001\u0011\u0001\u0011\u0001\u0011\u0003\u0011q\b\u0011\u0000"+
		"\u0000\u0012\u0001\u0001\u0003\u0002\u0005\u0003\u0007\u0004\t\u0005\u000b"+
		"\u0006\r\u0007\u000f\b\u0011\t\u0013\n\u0015\u000b\u0017\f\u0019\r\u001b"+
		"\u000e\u001d\u000f\u001f\u0010!\u0000#\u0000\u0001\u0000\u0005\u0004\u0000"+
		"$$@Z__az\u0002\u0000\u0000\u007f\u8000\ud800\u8000\udbff\u0001\u0000\u8000"+
		"\ud800\u8000\udbff\u0001\u0000\u8000\udc00\u8000\udfff\u0005\u0000$$0"+
		"9AZ__azu\u0000\u0001\u0001\u0000\u0000\u0000\u0000\u0003\u0001\u0000\u0000"+
		"\u0000\u0000\u0005\u0001\u0000\u0000\u0000\u0000\u0007\u0001\u0000\u0000"+
		"\u0000\u0000\t\u0001\u0000\u0000\u0000\u0000\u000b\u0001\u0000\u0000\u0000"+
		"\u0000\r\u0001\u0000\u0000\u0000\u0000\u000f\u0001\u0000\u0000\u0000\u0000"+
		"\u0011\u0001\u0000\u0000\u0000\u0000\u0013\u0001\u0000\u0000\u0000\u0000"+
		"\u0015\u0001\u0000\u0000\u0000\u0000\u0017\u0001\u0000\u0000\u0000\u0000"+
		"\u0019\u0001\u0000\u0000\u0000\u0000\u001b\u0001\u0000\u0000\u0000\u0000"+
		"\u001d\u0001\u0000\u0000\u0000\u0000\u001f\u0001\u0000\u0000\u0000\u0001"+
		"8\u0001\u0000\u0000\u0000\u0003:\u0001\u0000\u0000\u0000\u0005<\u0001"+
		"\u0000\u0000\u0000\u0007>\u0001\u0000\u0000\u0000\t@\u0001\u0000\u0000"+
		"\u0000\u000bB\u0001\u0000\u0000\u0000\rD\u0001\u0000\u0000\u0000\u000f"+
		"F\u0001\u0000\u0000\u0000\u0011H\u0001\u0000\u0000\u0000\u0013J\u0001"+
		"\u0000\u0000\u0000\u0015M\u0001\u0000\u0000\u0000\u0017P\u0001\u0000\u0000"+
		"\u0000\u0019T\u0001\u0000\u0000\u0000\u001bW\u0001\u0000\u0000\u0000\u001d"+
		"Y\u0001\u0000\u0000\u0000\u001f[\u0001\u0000\u0000\u0000!h\u0001\u0000"+
		"\u0000\u0000#p\u0001\u0000\u0000\u0000%&\u0005<\u0000\u0000&\'\u0005c"+
		"\u0000\u0000\'(\u0005o\u0000\u0000()\u0005n\u0000\u0000)*\u0005s\u0000"+
		"\u0000*+\u0005t\u0000\u0000+,\u0005r\u0000\u0000,-\u0005u\u0000\u0000"+
		"-.\u0005c\u0000\u0000./\u0005t\u0000\u0000/0\u0005o\u0000\u000001\u0005"+
		"r\u0000\u000019\u0005>\u0000\u000023\u0005<\u0000\u000034\u0005i\u0000"+
		"\u000045\u0005n\u0000\u000056\u0005i\u0000\u000067\u0005t\u0000\u0000"+
		"79\u0005>\u0000\u00008%\u0001\u0000\u0000\u000082\u0001\u0000\u0000\u0000"+
		"9\u0002\u0001\u0000\u0000\u0000:;\u0005(\u0000\u0000;\u0004\u0001\u0000"+
		"\u0000\u0000<=\u0005)\u0000\u0000=\u0006\u0001\u0000\u0000\u0000>?\u0005"+
		"[\u0000\u0000?\b\u0001\u0000\u0000\u0000@A\u0005]\u0000\u0000A\n\u0001"+
		"\u0000\u0000\u0000BC\u0005,\u0000\u0000C\f\u0001\u0000\u0000\u0000DE\u0002"+
		"./\u0000E\u000e\u0001\u0000\u0000\u0000FG\u0005!\u0000\u0000G\u0010\u0001"+
		"\u0000\u0000\u0000HI\u0005*\u0000\u0000I\u0012\u0001\u0000\u0000\u0000"+
		"JK\u0005&\u0000\u0000KL\u0005&\u0000\u0000L\u0014\u0001\u0000\u0000\u0000"+
		"MN\u0005|\u0000\u0000NO\u0005|\u0000\u0000O\u0016\u0001\u0000\u0000\u0000"+
		"PQ\u0005.\u0000\u0000QR\u0005.\u0000\u0000RS\u0005.\u0000\u0000S\u0018"+
		"\u0001\u0000\u0000\u0000TU\u0005.\u0000\u0000UV\u0005.\u0000\u0000V\u001a"+
		"\u0001\u0000\u0000\u0000WX\u0005#\u0000\u0000X\u001c\u0001\u0000\u0000"+
		"\u0000YZ\u0005 \u0000\u0000Z\u001e\u0001\u0000\u0000\u0000[_\u0003!\u0010"+
		"\u0000\\^\u0003#\u0011\u0000]\\\u0001\u0000\u0000\u0000^a\u0001\u0000"+
		"\u0000\u0000_]\u0001\u0000\u0000\u0000_`\u0001\u0000\u0000\u0000` \u0001"+
		"\u0000\u0000\u0000a_\u0001\u0000\u0000\u0000bi\u0007\u0000\u0000\u0000"+
		"cd\b\u0001\u0000\u0000di\u0004\u0010\u0000\u0000ef\u0007\u0002\u0000\u0000"+
		"fg\u0007\u0003\u0000\u0000gi\u0004\u0010\u0001\u0000hb\u0001\u0000\u0000"+
		"\u0000hc\u0001\u0000\u0000\u0000he\u0001\u0000\u0000\u0000i\"\u0001\u0000"+
		"\u0000\u0000jq\u0007\u0004\u0000\u0000kl\b\u0001\u0000\u0000lq\u0004\u0011"+
		"\u0002\u0000mn\u0007\u0002\u0000\u0000no\u0007\u0003\u0000\u0000oq\u0004"+
		"\u0011\u0003\u0000pj\u0001\u0000\u0000\u0000pk\u0001\u0000\u0000\u0000"+
		"pm\u0001\u0000\u0000\u0000q$\u0001\u0000\u0000\u0000\u0005\u00008_hp\u0000";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}