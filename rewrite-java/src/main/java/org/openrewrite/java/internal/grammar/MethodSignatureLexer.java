// Generated from /Users/knut/git/openrewrite/rewrite/rewrite-java/src/main/antlr/MethodSignatureLexer.g4 by ANTLR 4.13.2
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
		CONSTRUCTOR=1, JAVASCRIPT_DEFAULT_METHOD=2, LPAREN=3, RPAREN=4, LBRACK=5, 
		RBRACK=6, COMMA=7, DOT=8, BANG=9, WILDCARD=10, AND=11, OR=12, ELLIPSIS=13, 
		DOTDOT=14, POUND=15, SPACE=16, Identifier=17;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"CONSTRUCTOR", "JAVASCRIPT_DEFAULT_METHOD", "LPAREN", "RPAREN", "LBRACK", 
			"RBRACK", "COMMA", "DOT", "BANG", "WILDCARD", "AND", "OR", "ELLIPSIS", 
			"DOTDOT", "POUND", "SPACE", "WHITESPACE", "Identifier", "JavaLetter", 
			"JavaLetterOrDigit"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, "'<default>'", null, null, "'['", "']'", null, null, "'!'", 
			"'*'", "'&&'", "'||'", "'...'", "'..'", "'#'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "CONSTRUCTOR", "JAVASCRIPT_DEFAULT_METHOD", "LPAREN", "RPAREN", 
			"LBRACK", "RBRACK", "COMMA", "DOT", "BANG", "WILDCARD", "AND", "OR", 
			"ELLIPSIS", "DOTDOT", "POUND", "SPACE", "Identifier"
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
		case 18:
			return JavaLetter_sempred((RuleContext)_localctx, predIndex);
		case 19:
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
		"\u0004\u0000\u0011\u00a6\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002"+
		"\u0001\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002"+
		"\u0004\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002"+
		"\u0007\u0007\u0007\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002"+
		"\u000b\u0007\u000b\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e"+
		"\u0002\u000f\u0007\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011"+
		"\u0002\u0012\u0007\u0012\u0002\u0013\u0007\u0013\u0001\u0000\u0001\u0000"+
		"\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000"+
		"\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000"+
		"\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0003\u0000"+
		"=\b\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0002"+
		"\u0005\u0002J\b\u0002\n\u0002\f\u0002M\t\u0002\u0001\u0002\u0001\u0002"+
		"\u0005\u0002Q\b\u0002\n\u0002\f\u0002T\t\u0002\u0001\u0003\u0005\u0003"+
		"W\b\u0003\n\u0003\f\u0003Z\t\u0003\u0001\u0003\u0001\u0003\u0005\u0003"+
		"^\b\u0003\n\u0003\f\u0003a\t\u0003\u0001\u0004\u0001\u0004\u0001\u0005"+
		"\u0001\u0005\u0001\u0006\u0005\u0006h\b\u0006\n\u0006\f\u0006k\t\u0006"+
		"\u0001\u0006\u0001\u0006\u0005\u0006o\b\u0006\n\u0006\f\u0006r\t\u0006"+
		"\u0001\u0007\u0001\u0007\u0001\b\u0001\b\u0001\t\u0001\t\u0001\n\u0001"+
		"\n\u0001\n\u0001\u000b\u0001\u000b\u0001\u000b\u0001\f\u0001\f\u0001\f"+
		"\u0001\f\u0001\r\u0001\r\u0001\r\u0001\u000e\u0001\u000e\u0001\u000f\u0004"+
		"\u000f\u008a\b\u000f\u000b\u000f\f\u000f\u008b\u0001\u0010\u0001\u0010"+
		"\u0001\u0011\u0001\u0011\u0005\u0011\u0092\b\u0011\n\u0011\f\u0011\u0095"+
		"\t\u0011\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001"+
		"\u0012\u0003\u0012\u009d\b\u0012\u0001\u0013\u0001\u0013\u0001\u0013\u0001"+
		"\u0013\u0001\u0013\u0001\u0013\u0003\u0013\u00a5\b\u0013\u0000\u0000\u0014"+
		"\u0001\u0001\u0003\u0002\u0005\u0003\u0007\u0004\t\u0005\u000b\u0006\r"+
		"\u0007\u000f\b\u0011\t\u0013\n\u0015\u000b\u0017\f\u0019\r\u001b\u000e"+
		"\u001d\u000f\u001f\u0010!\u0000#\u0011%\u0000\'\u0000\u0001\u0000\u0006"+
		"\u0003\u0000\t\n\r\r  \u0004\u0000$$@Z__az\u0002\u0000\u0000\u007f\u8000"+
		"\ud800\u8000\udbff\u0001\u0000\u8000\ud800\u8000\udbff\u0001\u0000\u8000"+
		"\udc00\u8000\udfff\u0005\u0000$$09AZ__az\u00af\u0000\u0001\u0001\u0000"+
		"\u0000\u0000\u0000\u0003\u0001\u0000\u0000\u0000\u0000\u0005\u0001\u0000"+
		"\u0000\u0000\u0000\u0007\u0001\u0000\u0000\u0000\u0000\t\u0001\u0000\u0000"+
		"\u0000\u0000\u000b\u0001\u0000\u0000\u0000\u0000\r\u0001\u0000\u0000\u0000"+
		"\u0000\u000f\u0001\u0000\u0000\u0000\u0000\u0011\u0001\u0000\u0000\u0000"+
		"\u0000\u0013\u0001\u0000\u0000\u0000\u0000\u0015\u0001\u0000\u0000\u0000"+
		"\u0000\u0017\u0001\u0000\u0000\u0000\u0000\u0019\u0001\u0000\u0000\u0000"+
		"\u0000\u001b\u0001\u0000\u0000\u0000\u0000\u001d\u0001\u0000\u0000\u0000"+
		"\u0000\u001f\u0001\u0000\u0000\u0000\u0000#\u0001\u0000\u0000\u0000\u0001"+
		"<\u0001\u0000\u0000\u0000\u0003>\u0001\u0000\u0000\u0000\u0005K\u0001"+
		"\u0000\u0000\u0000\u0007X\u0001\u0000\u0000\u0000\tb\u0001\u0000\u0000"+
		"\u0000\u000bd\u0001\u0000\u0000\u0000\ri\u0001\u0000\u0000\u0000\u000f"+
		"s\u0001\u0000\u0000\u0000\u0011u\u0001\u0000\u0000\u0000\u0013w\u0001"+
		"\u0000\u0000\u0000\u0015y\u0001\u0000\u0000\u0000\u0017|\u0001\u0000\u0000"+
		"\u0000\u0019\u007f\u0001\u0000\u0000\u0000\u001b\u0083\u0001\u0000\u0000"+
		"\u0000\u001d\u0086\u0001\u0000\u0000\u0000\u001f\u0089\u0001\u0000\u0000"+
		"\u0000!\u008d\u0001\u0000\u0000\u0000#\u008f\u0001\u0000\u0000\u0000%"+
		"\u009c\u0001\u0000\u0000\u0000\'\u00a4\u0001\u0000\u0000\u0000)*\u0005"+
		"<\u0000\u0000*+\u0005c\u0000\u0000+,\u0005o\u0000\u0000,-\u0005n\u0000"+
		"\u0000-.\u0005s\u0000\u0000./\u0005t\u0000\u0000/0\u0005r\u0000\u0000"+
		"01\u0005u\u0000\u000012\u0005c\u0000\u000023\u0005t\u0000\u000034\u0005"+
		"o\u0000\u000045\u0005r\u0000\u00005=\u0005>\u0000\u000067\u0005<\u0000"+
		"\u000078\u0005i\u0000\u000089\u0005n\u0000\u00009:\u0005i\u0000\u0000"+
		":;\u0005t\u0000\u0000;=\u0005>\u0000\u0000<)\u0001\u0000\u0000\u0000<"+
		"6\u0001\u0000\u0000\u0000=\u0002\u0001\u0000\u0000\u0000>?\u0005<\u0000"+
		"\u0000?@\u0005d\u0000\u0000@A\u0005e\u0000\u0000AB\u0005f\u0000\u0000"+
		"BC\u0005a\u0000\u0000CD\u0005u\u0000\u0000DE\u0005l\u0000\u0000EF\u0005"+
		"t\u0000\u0000FG\u0005>\u0000\u0000G\u0004\u0001\u0000\u0000\u0000HJ\u0003"+
		"!\u0010\u0000IH\u0001\u0000\u0000\u0000JM\u0001\u0000\u0000\u0000KI\u0001"+
		"\u0000\u0000\u0000KL\u0001\u0000\u0000\u0000LN\u0001\u0000\u0000\u0000"+
		"MK\u0001\u0000\u0000\u0000NR\u0005(\u0000\u0000OQ\u0003!\u0010\u0000P"+
		"O\u0001\u0000\u0000\u0000QT\u0001\u0000\u0000\u0000RP\u0001\u0000\u0000"+
		"\u0000RS\u0001\u0000\u0000\u0000S\u0006\u0001\u0000\u0000\u0000TR\u0001"+
		"\u0000\u0000\u0000UW\u0003!\u0010\u0000VU\u0001\u0000\u0000\u0000WZ\u0001"+
		"\u0000\u0000\u0000XV\u0001\u0000\u0000\u0000XY\u0001\u0000\u0000\u0000"+
		"Y[\u0001\u0000\u0000\u0000ZX\u0001\u0000\u0000\u0000[_\u0005)\u0000\u0000"+
		"\\^\u0003!\u0010\u0000]\\\u0001\u0000\u0000\u0000^a\u0001\u0000\u0000"+
		"\u0000_]\u0001\u0000\u0000\u0000_`\u0001\u0000\u0000\u0000`\b\u0001\u0000"+
		"\u0000\u0000a_\u0001\u0000\u0000\u0000bc\u0005[\u0000\u0000c\n\u0001\u0000"+
		"\u0000\u0000de\u0005]\u0000\u0000e\f\u0001\u0000\u0000\u0000fh\u0003!"+
		"\u0010\u0000gf\u0001\u0000\u0000\u0000hk\u0001\u0000\u0000\u0000ig\u0001"+
		"\u0000\u0000\u0000ij\u0001\u0000\u0000\u0000jl\u0001\u0000\u0000\u0000"+
		"ki\u0001\u0000\u0000\u0000lp\u0005,\u0000\u0000mo\u0003!\u0010\u0000n"+
		"m\u0001\u0000\u0000\u0000or\u0001\u0000\u0000\u0000pn\u0001\u0000\u0000"+
		"\u0000pq\u0001\u0000\u0000\u0000q\u000e\u0001\u0000\u0000\u0000rp\u0001"+
		"\u0000\u0000\u0000st\u0002./\u0000t\u0010\u0001\u0000\u0000\u0000uv\u0005"+
		"!\u0000\u0000v\u0012\u0001\u0000\u0000\u0000wx\u0005*\u0000\u0000x\u0014"+
		"\u0001\u0000\u0000\u0000yz\u0005&\u0000\u0000z{\u0005&\u0000\u0000{\u0016"+
		"\u0001\u0000\u0000\u0000|}\u0005|\u0000\u0000}~\u0005|\u0000\u0000~\u0018"+
		"\u0001\u0000\u0000\u0000\u007f\u0080\u0005.\u0000\u0000\u0080\u0081\u0005"+
		".\u0000\u0000\u0081\u0082\u0005.\u0000\u0000\u0082\u001a\u0001\u0000\u0000"+
		"\u0000\u0083\u0084\u0005.\u0000\u0000\u0084\u0085\u0005.\u0000\u0000\u0085"+
		"\u001c\u0001\u0000\u0000\u0000\u0086\u0087\u0005#\u0000\u0000\u0087\u001e"+
		"\u0001\u0000\u0000\u0000\u0088\u008a\u0003!\u0010\u0000\u0089\u0088\u0001"+
		"\u0000\u0000\u0000\u008a\u008b\u0001\u0000\u0000\u0000\u008b\u0089\u0001"+
		"\u0000\u0000\u0000\u008b\u008c\u0001\u0000\u0000\u0000\u008c \u0001\u0000"+
		"\u0000\u0000\u008d\u008e\u0007\u0000\u0000\u0000\u008e\"\u0001\u0000\u0000"+
		"\u0000\u008f\u0093\u0003%\u0012\u0000\u0090\u0092\u0003\'\u0013\u0000"+
		"\u0091\u0090\u0001\u0000\u0000\u0000\u0092\u0095\u0001\u0000\u0000\u0000"+
		"\u0093\u0091\u0001\u0000\u0000\u0000\u0093\u0094\u0001\u0000\u0000\u0000"+
		"\u0094$\u0001\u0000\u0000\u0000\u0095\u0093\u0001\u0000\u0000\u0000\u0096"+
		"\u009d\u0007\u0001\u0000\u0000\u0097\u0098\b\u0002\u0000\u0000\u0098\u009d"+
		"\u0004\u0012\u0000\u0000\u0099\u009a\u0007\u0003\u0000\u0000\u009a\u009b"+
		"\u0007\u0004\u0000\u0000\u009b\u009d\u0004\u0012\u0001\u0000\u009c\u0096"+
		"\u0001\u0000\u0000\u0000\u009c\u0097\u0001\u0000\u0000\u0000\u009c\u0099"+
		"\u0001\u0000\u0000\u0000\u009d&\u0001\u0000\u0000\u0000\u009e\u00a5\u0007"+
		"\u0005\u0000\u0000\u009f\u00a0\b\u0002\u0000\u0000\u00a0\u00a5\u0004\u0013"+
		"\u0002\u0000\u00a1\u00a2\u0007\u0003\u0000\u0000\u00a2\u00a3\u0007\u0004"+
		"\u0000\u0000\u00a3\u00a5\u0004\u0013\u0003\u0000\u00a4\u009e\u0001\u0000"+
		"\u0000\u0000\u00a4\u009f\u0001\u0000\u0000\u0000\u00a4\u00a1\u0001\u0000"+
		"\u0000\u0000\u00a5(\u0001\u0000\u0000\u0000\f\u0000<KRX_ip\u008b\u0093"+
		"\u009c\u00a4\u0000";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}