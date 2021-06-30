// Generated from /Users/jon/Projects/github/openrewrite/rewrite/rewrite-hcl/src/main/antlr/HCLLexer.g4 by ANTLR 4.8
import java.util.Stack;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class HCLLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.8", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		NumericLiteral=1, BooleanLiteral=2, NULL=3, LBRACE=4, RBRACE=5, PLUS=6, 
		AND=7, EQ=8, LT=9, COLON=10, LBRACK=11, LPAREN=12, MINUS=13, OR=14, NEQ=15, 
		GT=16, QUESTION=17, RBRACK=18, RPAREN=19, MUL=20, NOT=21, LEQ=22, ASSIGN=23, 
		DOT=24, DIV=25, GEQ=26, ARROW=27, COMMA=28, MOD=29, ELLIPSIS=30, TILDE=31, 
		QUOTE=32, FOR=33, IF=34, IN=35, Identifier=36, WS=37, COMMENT=38, LINE_COMMENT=39, 
		TEMPLATE_INTERPOLATION_START=40, QuotedString=41, QuotedStringChar=42;
	public static final int
		TEMPLATE=1;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE", "TEMPLATE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"NumericLiteral", "ExponentPart", "ExponentIndicator", "SignedInteger", 
			"Digits", "Digit", "Sign", "BooleanLiteral", "NULL", "LBRACE", "RBRACE", 
			"PLUS", "AND", "EQ", "LT", "COLON", "LBRACK", "LPAREN", "MINUS", "OR", 
			"NEQ", "GT", "QUESTION", "RBRACK", "RPAREN", "MUL", "NOT", "LEQ", "ASSIGN", 
			"DOT", "DIV", "GEQ", "ARROW", "COMMA", "MOD", "ELLIPSIS", "TILDE", "QUOTE", 
			"FOR", "IF", "IN", "Identifier", "LetterOrDigit", "Letter", "WS", "COMMENT", 
			"LINE_COMMENT", "EscapeSequence", "HexDigit", "TEMPLATE_INTERPOLATION_START", 
			"QuotedString", "QuotedStringChar", "END_QUOTE"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, "'null'", "'{'", "'}'", "'+'", "'&&'", "'=='", "'<'", 
			"':'", "'['", "'('", "'-'", "'||'", "'!='", "'>'", "'?'", "']'", "')'", 
			"'*'", "'!'", "'<='", "'='", "'.'", "'/'", "'>='", "'=>'", "','", "'%'", 
			"'...'", "'~'", null, "'for'", "'if'", "'in'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "NumericLiteral", "BooleanLiteral", "NULL", "LBRACE", "RBRACE", 
			"PLUS", "AND", "EQ", "LT", "COLON", "LBRACK", "LPAREN", "MINUS", "OR", 
			"NEQ", "GT", "QUESTION", "RBRACK", "RPAREN", "MUL", "NOT", "LEQ", "ASSIGN", 
			"DOT", "DIV", "GEQ", "ARROW", "COMMA", "MOD", "ELLIPSIS", "TILDE", "QUOTE", 
			"FOR", "IF", "IN", "Identifier", "WS", "COMMENT", "LINE_COMMENT", "TEMPLATE_INTERPOLATION_START", 
			"QuotedString", "QuotedStringChar"
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


	    private enum CurlyType {
	        INTERPOLATION,
	        OBJECT
	    }

	    private Stack<CurlyType> leftCurlyStack = new Stack<CurlyType>();


	public HCLLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "HCLLexer.g4"; }

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
		case 9:
			LBRACE_action((RuleContext)_localctx, actionIndex);
			break;
		case 10:
			RBRACE_action((RuleContext)_localctx, actionIndex);
			break;
		case 49:
			TEMPLATE_INTERPOLATION_START_action((RuleContext)_localctx, actionIndex);
			break;
		}
	}
	private void LBRACE_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 0:

			    leftCurlyStack.push(CurlyType.OBJECT);

			break;
		}
	}
	private void RBRACE_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 1:

			    if(!leftCurlyStack.isEmpty()) {
			        if(leftCurlyStack.pop() == CurlyType.INTERPOLATION) {
			            popMode();
			        } else {
			            // closing an object, stay in the default mode
			        }
			    }

			break;
		}
	}
	private void TEMPLATE_INTERPOLATION_START_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 2:
			leftCurlyStack.push(CurlyType.INTERPOLATION);
			break;
		}
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2,\u015a\b\1\b\1\4"+
		"\2\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n"+
		"\4\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t"+
		" \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t"+
		"+\4,\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64"+
		"\t\64\4\65\t\65\4\66\t\66\3\2\3\2\3\2\5\2r\n\2\3\2\5\2u\n\2\3\2\3\2\3"+
		"\2\3\2\5\2{\n\2\3\3\3\3\3\3\3\4\3\4\3\5\5\5\u0083\n\5\3\5\3\5\3\6\6\6"+
		"\u0088\n\6\r\6\16\6\u0089\3\7\3\7\3\b\3\b\3\t\3\t\3\t\3\t\3\t\3\t\3\t"+
		"\3\t\3\t\5\t\u0099\n\t\3\n\3\n\3\n\3\n\3\n\3\13\3\13\3\13\3\f\3\f\3\f"+
		"\3\r\3\r\3\16\3\16\3\16\3\17\3\17\3\17\3\20\3\20\3\21\3\21\3\22\3\22\3"+
		"\23\3\23\3\24\3\24\3\25\3\25\3\25\3\26\3\26\3\26\3\27\3\27\3\30\3\30\3"+
		"\31\3\31\3\32\3\32\3\33\3\33\3\34\3\34\3\35\3\35\3\35\3\36\3\36\3\37\3"+
		"\37\3 \3 \3!\3!\3!\3\"\3\"\3\"\3#\3#\3$\3$\3%\3%\3%\3%\3&\3&\3\'\3\'\3"+
		"\'\3\'\3(\3(\3(\3(\3)\3)\3)\3*\3*\3*\3+\3+\3+\3+\3+\3+\7+\u00f7\n+\f+"+
		"\16+\u00fa\13+\5+\u00fc\n+\3,\3,\5,\u0100\n,\3-\3-\3-\3-\5-\u0106\n-\3"+
		".\6.\u0109\n.\r.\16.\u010a\3.\3.\3/\3/\3/\3/\7/\u0113\n/\f/\16/\u0116"+
		"\13/\3/\3/\3/\3\60\3\60\3\60\5\60\u011e\n\60\3\60\7\60\u0121\n\60\f\60"+
		"\16\60\u0124\13\60\3\61\3\61\3\61\3\61\3\61\3\61\3\61\3\61\3\61\3\61\3"+
		"\61\3\61\3\61\3\61\3\61\3\61\3\61\3\61\3\61\3\61\3\61\3\61\3\61\3\61\5"+
		"\61\u013e\n\61\3\62\3\62\3\63\3\63\3\63\3\63\3\63\3\63\3\63\3\64\6\64"+
		"\u014a\n\64\r\64\16\64\u014b\3\65\3\65\3\65\3\65\3\65\3\65\5\65\u0154"+
		"\n\65\3\66\3\66\3\66\3\66\3\66\3\u0114\2\67\4\3\6\2\b\2\n\2\f\2\16\2\20"+
		"\2\22\4\24\5\26\6\30\7\32\b\34\t\36\n \13\"\f$\r&\16(\17*\20,\21.\22\60"+
		"\23\62\24\64\25\66\268\27:\30<\31>\32@\33B\34D\35F\36H\37J L!N\"P#R$T"+
		"%V&X\2Z\2\\\'^(`)b\2d\2f*h+j,l\2\4\2\3\17\4\2GGgg\3\2\62;\4\2--//\6\2"+
		"&&C\\aac|\4\2\2\u0081\ud802\udc01\3\2\ud802\udc01\3\2\udc02\ue001\5\2"+
		"\13\f\16\17\"\"\4\2\f\f\17\17\7\2$$^^ppttvv\5\2\62;CHch\6\2\f\f\17\17"+
		"$$&\'\3\2}}\2\u0169\2\4\3\2\2\2\2\22\3\2\2\2\2\24\3\2\2\2\2\26\3\2\2\2"+
		"\2\30\3\2\2\2\2\32\3\2\2\2\2\34\3\2\2\2\2\36\3\2\2\2\2 \3\2\2\2\2\"\3"+
		"\2\2\2\2$\3\2\2\2\2&\3\2\2\2\2(\3\2\2\2\2*\3\2\2\2\2,\3\2\2\2\2.\3\2\2"+
		"\2\2\60\3\2\2\2\2\62\3\2\2\2\2\64\3\2\2\2\2\66\3\2\2\2\28\3\2\2\2\2:\3"+
		"\2\2\2\2<\3\2\2\2\2>\3\2\2\2\2@\3\2\2\2\2B\3\2\2\2\2D\3\2\2\2\2F\3\2\2"+
		"\2\2H\3\2\2\2\2J\3\2\2\2\2L\3\2\2\2\2N\3\2\2\2\2P\3\2\2\2\2R\3\2\2\2\2"+
		"T\3\2\2\2\2V\3\2\2\2\2\\\3\2\2\2\2^\3\2\2\2\2`\3\2\2\2\3f\3\2\2\2\3h\3"+
		"\2\2\2\3j\3\2\2\2\3l\3\2\2\2\4z\3\2\2\2\6|\3\2\2\2\b\177\3\2\2\2\n\u0082"+
		"\3\2\2\2\f\u0087\3\2\2\2\16\u008b\3\2\2\2\20\u008d\3\2\2\2\22\u0098\3"+
		"\2\2\2\24\u009a\3\2\2\2\26\u009f\3\2\2\2\30\u00a2\3\2\2\2\32\u00a5\3\2"+
		"\2\2\34\u00a7\3\2\2\2\36\u00aa\3\2\2\2 \u00ad\3\2\2\2\"\u00af\3\2\2\2"+
		"$\u00b1\3\2\2\2&\u00b3\3\2\2\2(\u00b5\3\2\2\2*\u00b7\3\2\2\2,\u00ba\3"+
		"\2\2\2.\u00bd\3\2\2\2\60\u00bf\3\2\2\2\62\u00c1\3\2\2\2\64\u00c3\3\2\2"+
		"\2\66\u00c5\3\2\2\28\u00c7\3\2\2\2:\u00c9\3\2\2\2<\u00cc\3\2\2\2>\u00ce"+
		"\3\2\2\2@\u00d0\3\2\2\2B\u00d2\3\2\2\2D\u00d5\3\2\2\2F\u00d8\3\2\2\2H"+
		"\u00da\3\2\2\2J\u00dc\3\2\2\2L\u00e0\3\2\2\2N\u00e2\3\2\2\2P\u00e6\3\2"+
		"\2\2R\u00ea\3\2\2\2T\u00ed\3\2\2\2V\u00fb\3\2\2\2X\u00ff\3\2\2\2Z\u0105"+
		"\3\2\2\2\\\u0108\3\2\2\2^\u010e\3\2\2\2`\u011d\3\2\2\2b\u013d\3\2\2\2"+
		"d\u013f\3\2\2\2f\u0141\3\2\2\2h\u0149\3\2\2\2j\u0153\3\2\2\2l\u0155\3"+
		"\2\2\2no\5\f\6\2oq\7\60\2\2pr\5\f\6\2qp\3\2\2\2qr\3\2\2\2rt\3\2\2\2su"+
		"\5\6\3\2ts\3\2\2\2tu\3\2\2\2u{\3\2\2\2vw\5\f\6\2wx\5\6\3\2x{\3\2\2\2y"+
		"{\5\f\6\2zn\3\2\2\2zv\3\2\2\2zy\3\2\2\2{\5\3\2\2\2|}\5\b\4\2}~\5\n\5\2"+
		"~\7\3\2\2\2\177\u0080\t\2\2\2\u0080\t\3\2\2\2\u0081\u0083\5\20\b\2\u0082"+
		"\u0081\3\2\2\2\u0082\u0083\3\2\2\2\u0083\u0084\3\2\2\2\u0084\u0085\5\f"+
		"\6\2\u0085\13\3\2\2\2\u0086\u0088\5\16\7\2\u0087\u0086\3\2\2\2\u0088\u0089"+
		"\3\2\2\2\u0089\u0087\3\2\2\2\u0089\u008a\3\2\2\2\u008a\r\3\2\2\2\u008b"+
		"\u008c\t\3\2\2\u008c\17\3\2\2\2\u008d\u008e\t\4\2\2\u008e\21\3\2\2\2\u008f"+
		"\u0090\7v\2\2\u0090\u0091\7t\2\2\u0091\u0092\7w\2\2\u0092\u0099\7g\2\2"+
		"\u0093\u0094\7h\2\2\u0094\u0095\7c\2\2\u0095\u0096\7n\2\2\u0096\u0097"+
		"\7u\2\2\u0097\u0099\7g\2\2\u0098\u008f\3\2\2\2\u0098\u0093\3\2\2\2\u0099"+
		"\23\3\2\2\2\u009a\u009b\7p\2\2\u009b\u009c\7w\2\2\u009c\u009d\7n\2\2\u009d"+
		"\u009e\7n\2\2\u009e\25\3\2\2\2\u009f\u00a0\7}\2\2\u00a0\u00a1\b\13\2\2"+
		"\u00a1\27\3\2\2\2\u00a2\u00a3\7\177\2\2\u00a3\u00a4\b\f\3\2\u00a4\31\3"+
		"\2\2\2\u00a5\u00a6\7-\2\2\u00a6\33\3\2\2\2\u00a7\u00a8\7(\2\2\u00a8\u00a9"+
		"\7(\2\2\u00a9\35\3\2\2\2\u00aa\u00ab\7?\2\2\u00ab\u00ac\7?\2\2\u00ac\37"+
		"\3\2\2\2\u00ad\u00ae\7>\2\2\u00ae!\3\2\2\2\u00af\u00b0\7<\2\2\u00b0#\3"+
		"\2\2\2\u00b1\u00b2\7]\2\2\u00b2%\3\2\2\2\u00b3\u00b4\7*\2\2\u00b4\'\3"+
		"\2\2\2\u00b5\u00b6\7/\2\2\u00b6)\3\2\2\2\u00b7\u00b8\7~\2\2\u00b8\u00b9"+
		"\7~\2\2\u00b9+\3\2\2\2\u00ba\u00bb\7#\2\2\u00bb\u00bc\7?\2\2\u00bc-\3"+
		"\2\2\2\u00bd\u00be\7@\2\2\u00be/\3\2\2\2\u00bf\u00c0\7A\2\2\u00c0\61\3"+
		"\2\2\2\u00c1\u00c2\7_\2\2\u00c2\63\3\2\2\2\u00c3\u00c4\7+\2\2\u00c4\65"+
		"\3\2\2\2\u00c5\u00c6\7,\2\2\u00c6\67\3\2\2\2\u00c7\u00c8\7#\2\2\u00c8"+
		"9\3\2\2\2\u00c9\u00ca\7>\2\2\u00ca\u00cb\7?\2\2\u00cb;\3\2\2\2\u00cc\u00cd"+
		"\7?\2\2\u00cd=\3\2\2\2\u00ce\u00cf\7\60\2\2\u00cf?\3\2\2\2\u00d0\u00d1"+
		"\7\61\2\2\u00d1A\3\2\2\2\u00d2\u00d3\7@\2\2\u00d3\u00d4\7?\2\2\u00d4C"+
		"\3\2\2\2\u00d5\u00d6\7?\2\2\u00d6\u00d7\7@\2\2\u00d7E\3\2\2\2\u00d8\u00d9"+
		"\7.\2\2\u00d9G\3\2\2\2\u00da\u00db\7\'\2\2\u00dbI\3\2\2\2\u00dc\u00dd"+
		"\7\60\2\2\u00dd\u00de\7\60\2\2\u00de\u00df\7\60\2\2\u00dfK\3\2\2\2\u00e0"+
		"\u00e1\7\u0080\2\2\u00e1M\3\2\2\2\u00e2\u00e3\7$\2\2\u00e3\u00e4\3\2\2"+
		"\2\u00e4\u00e5\b\'\4\2\u00e5O\3\2\2\2\u00e6\u00e7\7h\2\2\u00e7\u00e8\7"+
		"q\2\2\u00e8\u00e9\7t\2\2\u00e9Q\3\2\2\2\u00ea\u00eb\7k\2\2\u00eb\u00ec"+
		"\7h\2\2\u00ecS\3\2\2\2\u00ed\u00ee\7k\2\2\u00ee\u00ef\7p\2\2\u00efU\3"+
		"\2\2\2\u00f0\u00fc\5P(\2\u00f1\u00fc\5R)\2\u00f2\u00fc\5T*\2\u00f3\u00f8"+
		"\5Z-\2\u00f4\u00f7\5X,\2\u00f5\u00f7\7/\2\2\u00f6\u00f4\3\2\2\2\u00f6"+
		"\u00f5\3\2\2\2\u00f7\u00fa\3\2\2\2\u00f8\u00f6\3\2\2\2\u00f8\u00f9\3\2"+
		"\2\2\u00f9\u00fc\3\2\2\2\u00fa\u00f8\3\2\2\2\u00fb\u00f0\3\2\2\2\u00fb"+
		"\u00f1\3\2\2\2\u00fb\u00f2\3\2\2\2\u00fb\u00f3\3\2\2\2\u00fcW\3\2\2\2"+
		"\u00fd\u0100\5Z-\2\u00fe\u0100\t\3\2\2\u00ff\u00fd\3\2\2\2\u00ff\u00fe"+
		"\3\2\2\2\u0100Y\3\2\2\2\u0101\u0106\t\5\2\2\u0102\u0106\n\6\2\2\u0103"+
		"\u0104\t\7\2\2\u0104\u0106\t\b\2\2\u0105\u0101\3\2\2\2\u0105\u0102\3\2"+
		"\2\2\u0105\u0103\3\2\2\2\u0106[\3\2\2\2\u0107\u0109\t\t\2\2\u0108\u0107"+
		"\3\2\2\2\u0109\u010a\3\2\2\2\u010a\u0108\3\2\2\2\u010a\u010b\3\2\2\2\u010b"+
		"\u010c\3\2\2\2\u010c\u010d\b.\5\2\u010d]\3\2\2\2\u010e\u010f\7\61\2\2"+
		"\u010f\u0110\7,\2\2\u0110\u0114\3\2\2\2\u0111\u0113\13\2\2\2\u0112\u0111"+
		"\3\2\2\2\u0113\u0116\3\2\2\2\u0114\u0115\3\2\2\2\u0114\u0112\3\2\2\2\u0115"+
		"\u0117\3\2\2\2\u0116\u0114\3\2\2\2\u0117\u0118\7,\2\2\u0118\u0119\7\61"+
		"\2\2\u0119_\3\2\2\2\u011a\u011b\7\61\2\2\u011b\u011e\7\61\2\2\u011c\u011e"+
		"\7%\2\2\u011d\u011a\3\2\2\2\u011d\u011c\3\2\2\2\u011e\u0122\3\2\2\2\u011f"+
		"\u0121\n\n\2\2\u0120\u011f\3\2\2\2\u0121\u0124\3\2\2\2\u0122\u0120\3\2"+
		"\2\2\u0122\u0123\3\2\2\2\u0123a\3\2\2\2\u0124\u0122\3\2\2\2\u0125\u0126"+
		"\7^\2\2\u0126\u013e\t\13\2\2\u0127\u0128\7^\2\2\u0128\u0129\5d\62\2\u0129"+
		"\u012a\5d\62\2\u012a\u012b\5d\62\2\u012b\u012c\5d\62\2\u012c\u013e\3\2"+
		"\2\2\u012d\u012e\7^\2\2\u012e\u012f\5d\62\2\u012f\u0130\5d\62\2\u0130"+
		"\u0131\5d\62\2\u0131\u0132\5d\62\2\u0132\u0133\5d\62\2\u0133\u0134\5d"+
		"\62\2\u0134\u0135\5d\62\2\u0135\u0136\5d\62\2\u0136\u013e\3\2\2\2\u0137"+
		"\u0138\7\'\2\2\u0138\u0139\7\'\2\2\u0139\u013e\7}\2\2\u013a\u013b\7&\2"+
		"\2\u013b\u013c\7&\2\2\u013c\u013e\7}\2\2\u013d\u0125\3\2\2\2\u013d\u0127"+
		"\3\2\2\2\u013d\u012d\3\2\2\2\u013d\u0137\3\2\2\2\u013d\u013a\3\2\2\2\u013e"+
		"c\3\2\2\2\u013f\u0140\t\f\2\2\u0140e\3\2\2\2\u0141\u0142\7&\2\2\u0142"+
		"\u0143\7}\2\2\u0143\u0144\3\2\2\2\u0144\u0145\b\63\6\2\u0145\u0146\3\2"+
		"\2\2\u0146\u0147\b\63\7\2\u0147g\3\2\2\2\u0148\u014a\5j\65\2\u0149\u0148"+
		"\3\2\2\2\u014a\u014b\3\2\2\2\u014b\u0149\3\2\2\2\u014b\u014c\3\2\2\2\u014c"+
		"i\3\2\2\2\u014d\u0154\n\r\2\2\u014e\u014f\7&\2\2\u014f\u0154\n\16\2\2"+
		"\u0150\u0151\7\'\2\2\u0151\u0154\n\16\2\2\u0152\u0154\5b\61\2\u0153\u014d"+
		"\3\2\2\2\u0153\u014e\3\2\2\2\u0153\u0150\3\2\2\2\u0153\u0152\3\2\2\2\u0154"+
		"k\3\2\2\2\u0155\u0156\7$\2\2\u0156\u0157\3\2\2\2\u0157\u0158\b\66\b\2"+
		"\u0158\u0159\b\66\t\2\u0159m\3\2\2\2\26\2\3qtz\u0082\u0089\u0098\u00f6"+
		"\u00f8\u00fb\u00ff\u0105\u010a\u0114\u011d\u0122\u013d\u014b\u0153\n\3"+
		"\13\2\3\f\3\7\3\2\b\2\2\3\63\4\7\2\2\t\"\2\6\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}