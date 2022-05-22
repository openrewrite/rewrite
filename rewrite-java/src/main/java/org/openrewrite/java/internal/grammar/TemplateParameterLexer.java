// Generated from /Users/jon/Projects/github/openrewrite/rewrite/rewrite-java/src/main/antlr/TemplateParameterLexer.g4 by ANTLR 4.9.3
package org.openrewrite.java.internal.grammar;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class TemplateParameterLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.9.3", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		LPAREN=1, RPAREN=2, LBRACK=3, RBRACK=4, DOT=5, COMMA=6, SPACE=7, FullyQualifiedName=8, 
		Number=9, Identifier=10;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"LPAREN", "RPAREN", "LBRACK", "RBRACK", "DOT", "COMMA", "SPACE", "FullyQualifiedName", 
			"Number", "Identifier", "JavaLetter", "JavaLetterOrDigit"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'('", "')'", "'['", "']'", "'.'", "','", "' '"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "LPAREN", "RPAREN", "LBRACK", "RBRACK", "DOT", "COMMA", "SPACE", 
			"FullyQualifiedName", "Number", "Identifier"
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


	public TemplateParameterLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "TemplateParameterLexer.g4"; }

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
		case 10:
			return JavaLetter_sempred((RuleContext)_localctx, predIndex);
		case 11:
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\f\u0082\b\1\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\3\2\3\2\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7"+
		"\3\b\3\b\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3"+
		"\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t"+
		"\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3"+
		"\t\3\t\3\t\3\t\7\t`\n\t\f\t\16\tc\13\t\5\te\n\t\3\n\6\nh\n\n\r\n\16\n"+
		"i\3\13\3\13\7\13n\n\13\f\13\16\13q\13\13\3\f\3\f\3\f\3\f\3\f\3\f\5\fy"+
		"\n\f\3\r\3\r\3\r\3\r\3\r\3\r\5\r\u0081\n\r\2\2\16\3\3\5\4\7\5\t\6\13\7"+
		"\r\b\17\t\21\n\23\13\25\f\27\2\31\2\3\2\b\3\2\62;\6\2&&C\\aac|\4\2\2\u0081"+
		"\ud802\udc01\3\2\ud802\udc01\3\2\udc02\ue001\7\2&&\62;C\\aac|\2\u0090"+
		"\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2"+
		"\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\3\33\3\2\2\2"+
		"\5\35\3\2\2\2\7\37\3\2\2\2\t!\3\2\2\2\13#\3\2\2\2\r%\3\2\2\2\17\'\3\2"+
		"\2\2\21d\3\2\2\2\23g\3\2\2\2\25k\3\2\2\2\27x\3\2\2\2\31\u0080\3\2\2\2"+
		"\33\34\7*\2\2\34\4\3\2\2\2\35\36\7+\2\2\36\6\3\2\2\2\37 \7]\2\2 \b\3\2"+
		"\2\2!\"\7_\2\2\"\n\3\2\2\2#$\7\60\2\2$\f\3\2\2\2%&\7.\2\2&\16\3\2\2\2"+
		"\'(\7\"\2\2(\20\3\2\2\2)*\7d\2\2*+\7q\2\2+,\7q\2\2,-\7n\2\2-.\7g\2\2."+
		"/\7c\2\2/e\7p\2\2\60\61\7d\2\2\61\62\7{\2\2\62\63\7v\2\2\63e\7g\2\2\64"+
		"\65\7e\2\2\65\66\7j\2\2\66\67\7c\2\2\67e\7t\2\289\7f\2\29:\7q\2\2:;\7"+
		"w\2\2;<\7d\2\2<=\7n\2\2=e\7g\2\2>?\7h\2\2?@\7n\2\2@A\7q\2\2AB\7c\2\2B"+
		"e\7v\2\2CD\7k\2\2DE\7p\2\2Ee\7v\2\2FG\7n\2\2GH\7q\2\2HI\7p\2\2Ie\7i\2"+
		"\2JK\7u\2\2KL\7j\2\2LM\7q\2\2MN\7t\2\2Ne\7v\2\2OP\7U\2\2PQ\7v\2\2QR\7"+
		"t\2\2RS\7k\2\2ST\7p\2\2Te\7i\2\2UV\7Q\2\2VW\7d\2\2WX\7l\2\2XY\7g\2\2Y"+
		"Z\7e\2\2Ze\7v\2\2[a\5\25\13\2\\]\5\13\6\2]^\5\25\13\2^`\3\2\2\2_\\\3\2"+
		"\2\2`c\3\2\2\2a_\3\2\2\2ab\3\2\2\2be\3\2\2\2ca\3\2\2\2d)\3\2\2\2d\60\3"+
		"\2\2\2d\64\3\2\2\2d8\3\2\2\2d>\3\2\2\2dC\3\2\2\2dF\3\2\2\2dJ\3\2\2\2d"+
		"O\3\2\2\2dU\3\2\2\2d[\3\2\2\2e\22\3\2\2\2fh\t\2\2\2gf\3\2\2\2hi\3\2\2"+
		"\2ig\3\2\2\2ij\3\2\2\2j\24\3\2\2\2ko\5\27\f\2ln\5\31\r\2ml\3\2\2\2nq\3"+
		"\2\2\2om\3\2\2\2op\3\2\2\2p\26\3\2\2\2qo\3\2\2\2ry\t\3\2\2st\n\4\2\2t"+
		"y\6\f\2\2uv\t\5\2\2vw\t\6\2\2wy\6\f\3\2xr\3\2\2\2xs\3\2\2\2xu\3\2\2\2"+
		"y\30\3\2\2\2z\u0081\t\7\2\2{|\n\4\2\2|\u0081\6\r\4\2}~\t\5\2\2~\177\t"+
		"\6\2\2\177\u0081\6\r\5\2\u0080z\3\2\2\2\u0080{\3\2\2\2\u0080}\3\2\2\2"+
		"\u0081\32\3\2\2\2\t\2adiox\u0080\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}