// Generated from /Users/jon/Projects/github/openrewrite/rewrite/rewrite-hcl/src/main/antlr/HCLLexer.g4 by ANTLR 4.9.3
package org.openrewrite.hcl.internal.grammar;
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
	static { RuntimeMetaData.checkVersion("4.9.3", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		FOR_BRACE=1, FOR_BRACK=2, IF=3, IN=4, LBRACE=5, RBRACE=6, ASSIGN=7, Identifier=8, 
		WS=9, COMMENT=10, LINE_COMMENT=11, NEWLINE=12, NumericLiteral=13, BooleanLiteral=14, 
		QUOTE=15, NULL=16, HEREDOC_START=17, PLUS=18, AND=19, EQ=20, LT=21, COLON=22, 
		LBRACK=23, LPAREN=24, MINUS=25, OR=26, NEQ=27, GT=28, QUESTION=29, RBRACK=30, 
		RPAREN=31, MUL=32, NOT=33, LEQ=34, DOT=35, DIV=36, GEQ=37, ARROW=38, COMMA=39, 
		MOD=40, ELLIPSIS=41, TILDE=42, TEMPLATE_INTERPOLATION_START=43, TemplateStringLiteral=44, 
		TemplateStringLiteralChar=45, HTemplateLiteral=46, HTemplateLiteralChar=47;
	public static final int
		TEMPLATE=1, HEREDOC_PREAMBLE=2, HEREDOC=3;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE", "TEMPLATE", "HEREDOC_PREAMBLE", "HEREDOC"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"FOR_BRACE", "FOR_BRACK", "IF", "IN", "LBRACE", "RBRACE", "ASSIGN", "StringLiteralChar", 
			"Identifier", "WS", "COMMENT", "LINE_COMMENT", "NEWLINE", "LetterOrDigit", 
			"Letter", "EscapeSequence", "HexDigit", "NumericLiteral", "ExponentPart", 
			"BooleanLiteral", "QUOTE", "NULL", "HEREDOC_START", "PLUS", "AND", "EQ", 
			"LT", "COLON", "LBRACK", "LPAREN", "MINUS", "OR", "NEQ", "GT", "QUESTION", 
			"RBRACK", "RPAREN", "MUL", "NOT", "LEQ", "DOT", "DIV", "GEQ", "ARROW", 
			"COMMA", "MOD", "ELLIPSIS", "TILDE", "TEMPLATE_INTERPOLATION_START", 
			"TemplateStringLiteral", "TemplateStringLiteralChar", "END_QUOTE", "HP_NEWLINE", 
			"HPIdentifier", "H_NEWLINE", "H_TEMPLATE_INTERPOLATION_START", "HTemplateLiteral", 
			"HTemplateLiteralChar"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, "'if'", "'in'", "'{'", "'}'", "'='", null, null, null, 
			null, null, null, null, null, "'null'", null, "'+'", "'&&'", "'=='", 
			"'<'", "':'", "'['", "'('", "'-'", "'||'", "'!='", "'>'", "'?'", "']'", 
			"')'", "'*'", "'!'", "'<='", "'.'", "'/'", "'>='", "'=>'", "','", "'%'", 
			"'...'", "'~'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "FOR_BRACE", "FOR_BRACK", "IF", "IN", "LBRACE", "RBRACE", "ASSIGN", 
			"Identifier", "WS", "COMMENT", "LINE_COMMENT", "NEWLINE", "NumericLiteral", 
			"BooleanLiteral", "QUOTE", "NULL", "HEREDOC_START", "PLUS", "AND", "EQ", 
			"LT", "COLON", "LBRACK", "LPAREN", "MINUS", "OR", "NEQ", "GT", "QUESTION", 
			"RBRACK", "RPAREN", "MUL", "NOT", "LEQ", "DOT", "DIV", "GEQ", "ARROW", 
			"COMMA", "MOD", "ELLIPSIS", "TILDE", "TEMPLATE_INTERPOLATION_START", 
			"TemplateStringLiteral", "TemplateStringLiteralChar", "HTemplateLiteral", 
			"HTemplateLiteralChar"
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
	    private Stack<String> heredocIdentifier = new Stack<String>();


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
		case 4:
			LBRACE_action((RuleContext)_localctx, actionIndex);
			break;
		case 5:
			RBRACE_action((RuleContext)_localctx, actionIndex);
			break;
		case 48:
			TEMPLATE_INTERPOLATION_START_action((RuleContext)_localctx, actionIndex);
			break;
		case 53:
			HPIdentifier_action((RuleContext)_localctx, actionIndex);
			break;
		case 55:
			H_TEMPLATE_INTERPOLATION_START_action((RuleContext)_localctx, actionIndex);
			break;
		case 56:
			HTemplateLiteral_action((RuleContext)_localctx, actionIndex);
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
	private void HPIdentifier_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 3:

			    heredocIdentifier.push(getText());

			break;
		}
	}
	private void H_TEMPLATE_INTERPOLATION_START_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 4:
			leftCurlyStack.push(CurlyType.INTERPOLATION);
			break;
		}
	}
	private void HTemplateLiteral_action(RuleContext _localctx, int actionIndex) {
		switch (actionIndex) {
		case 5:

			  if(!heredocIdentifier.isEmpty() && getText().endsWith(heredocIdentifier.peek())) {
			      setType(Identifier);
			      heredocIdentifier.pop();
			      popMode();
			  }

			break;
		}
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\61\u01b3\b\1\b\1"+
		"\b\1\b\1\4\2\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t"+
		"\t\4\n\t\n\4\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21"+
		"\t\21\4\22\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30"+
		"\t\30\4\31\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37"+
		"\t\37\4 \t \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)"+
		"\4*\t*\4+\t+\4,\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63"+
		"\t\63\4\64\t\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;"+
		"\3\2\3\2\7\2}\n\2\f\2\16\2\u0080\13\2\3\2\3\2\3\2\3\2\3\3\3\3\7\3\u0088"+
		"\n\3\f\3\16\3\u008b\13\3\3\3\3\3\3\3\3\3\3\4\3\4\3\4\3\5\3\5\3\5\3\6\3"+
		"\6\3\6\3\7\3\7\3\7\3\b\3\b\3\t\3\t\5\t\u00a1\n\t\3\n\3\n\3\n\7\n\u00a6"+
		"\n\n\f\n\16\n\u00a9\13\n\3\13\6\13\u00ac\n\13\r\13\16\13\u00ad\3\13\3"+
		"\13\3\f\3\f\3\f\3\f\7\f\u00b6\n\f\f\f\16\f\u00b9\13\f\3\f\3\f\3\f\3\f"+
		"\3\f\3\r\3\r\3\r\5\r\u00c3\n\r\3\r\7\r\u00c6\n\r\f\r\16\r\u00c9\13\r\3"+
		"\r\3\r\3\r\3\r\3\16\3\16\3\16\3\16\3\17\3\17\5\17\u00d5\n\17\3\20\3\20"+
		"\3\20\3\20\5\20\u00db\n\20\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21"+
		"\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\5\21\u00ef\n\21\3\22\3\22"+
		"\3\23\6\23\u00f4\n\23\r\23\16\23\u00f5\3\23\3\23\7\23\u00fa\n\23\f\23"+
		"\16\23\u00fd\13\23\3\23\5\23\u0100\n\23\3\23\6\23\u0103\n\23\r\23\16\23"+
		"\u0104\3\23\3\23\6\23\u0109\n\23\r\23\16\23\u010a\5\23\u010d\n\23\3\24"+
		"\3\24\5\24\u0111\n\24\3\24\6\24\u0114\n\24\r\24\16\24\u0115\3\25\3\25"+
		"\3\25\3\25\3\25\3\25\3\25\3\25\3\25\5\25\u0121\n\25\3\26\3\26\3\26\3\26"+
		"\3\27\3\27\3\27\3\27\3\27\3\30\3\30\3\30\3\30\5\30\u0130\n\30\3\30\3\30"+
		"\3\31\3\31\3\32\3\32\3\32\3\33\3\33\3\33\3\34\3\34\3\35\3\35\3\36\3\36"+
		"\3\37\3\37\3 \3 \3!\3!\3!\3\"\3\"\3\"\3#\3#\3$\3$\3%\3%\3&\3&\3\'\3\'"+
		"\3(\3(\3)\3)\3)\3*\3*\3+\3+\3,\3,\3,\3-\3-\3-\3.\3.\3/\3/\3\60\3\60\3"+
		"\60\3\60\3\61\3\61\3\62\3\62\3\62\3\62\3\62\3\62\3\62\3\63\6\63\u0177"+
		"\n\63\r\63\16\63\u0178\3\64\3\64\3\64\3\64\3\64\3\64\5\64\u0181\n\64\3"+
		"\65\3\65\3\65\3\65\3\65\3\66\3\66\3\66\3\66\3\66\3\67\3\67\3\67\7\67\u0190"+
		"\n\67\f\67\16\67\u0193\13\67\3\67\3\67\3\67\3\67\38\38\38\38\39\39\39"+
		"\39\39\39\39\39\3:\6:\u01a6\n:\r:\16:\u01a7\3:\3:\3;\3;\3;\3;\3;\3;\5"+
		";\u01b2\n;\3\u00b7\2<\6\3\b\4\n\5\f\6\16\7\20\b\22\t\24\2\26\n\30\13\32"+
		"\f\34\r\36\16 \2\"\2$\2&\2(\17*\2,\20.\21\60\22\62\23\64\24\66\258\26"+
		":\27<\30>\31@\32B\33D\34F\35H\36J\37L N!P\"R#T$V%X&Z\'\\(^)`*b+d,f-h."+
		"j/l\2n\2p\2r\2t\2v\60x\61\6\2\3\4\5\20\6\2\f\f\17\17$$&\'\5\2\13\13\16"+
		"\17\"\"\4\2\f\f\17\17\3\2\62;\6\2&&C\\aac|\4\2\2\u0081\ud802\udc01\3\2"+
		"\ud802\udc01\3\2\udc02\ue001\7\2$$^^ppttvv\5\2\62;CHch\4\2GGgg\4\2--/"+
		"/\3\2}}\5\2\f\f\17\17&\'\2\u01cc\2\6\3\2\2\2\2\b\3\2\2\2\2\n\3\2\2\2\2"+
		"\f\3\2\2\2\2\16\3\2\2\2\2\20\3\2\2\2\2\22\3\2\2\2\2\26\3\2\2\2\2\30\3"+
		"\2\2\2\2\32\3\2\2\2\2\34\3\2\2\2\2\36\3\2\2\2\2(\3\2\2\2\2,\3\2\2\2\2"+
		".\3\2\2\2\2\60\3\2\2\2\2\62\3\2\2\2\2\64\3\2\2\2\2\66\3\2\2\2\28\3\2\2"+
		"\2\2:\3\2\2\2\2<\3\2\2\2\2>\3\2\2\2\2@\3\2\2\2\2B\3\2\2\2\2D\3\2\2\2\2"+
		"F\3\2\2\2\2H\3\2\2\2\2J\3\2\2\2\2L\3\2\2\2\2N\3\2\2\2\2P\3\2\2\2\2R\3"+
		"\2\2\2\2T\3\2\2\2\2V\3\2\2\2\2X\3\2\2\2\2Z\3\2\2\2\2\\\3\2\2\2\2^\3\2"+
		"\2\2\2`\3\2\2\2\2b\3\2\2\2\2d\3\2\2\2\3f\3\2\2\2\3h\3\2\2\2\3j\3\2\2\2"+
		"\3l\3\2\2\2\4n\3\2\2\2\4p\3\2\2\2\5r\3\2\2\2\5t\3\2\2\2\5v\3\2\2\2\5x"+
		"\3\2\2\2\6z\3\2\2\2\b\u0085\3\2\2\2\n\u0090\3\2\2\2\f\u0093\3\2\2\2\16"+
		"\u0096\3\2\2\2\20\u0099\3\2\2\2\22\u009c\3\2\2\2\24\u00a0\3\2\2\2\26\u00a2"+
		"\3\2\2\2\30\u00ab\3\2\2\2\32\u00b1\3\2\2\2\34\u00c2\3\2\2\2\36\u00ce\3"+
		"\2\2\2 \u00d4\3\2\2\2\"\u00da\3\2\2\2$\u00ee\3\2\2\2&\u00f0\3\2\2\2(\u010c"+
		"\3\2\2\2*\u010e\3\2\2\2,\u0120\3\2\2\2.\u0122\3\2\2\2\60\u0126\3\2\2\2"+
		"\62\u012b\3\2\2\2\64\u0133\3\2\2\2\66\u0135\3\2\2\28\u0138\3\2\2\2:\u013b"+
		"\3\2\2\2<\u013d\3\2\2\2>\u013f\3\2\2\2@\u0141\3\2\2\2B\u0143\3\2\2\2D"+
		"\u0145\3\2\2\2F\u0148\3\2\2\2H\u014b\3\2\2\2J\u014d\3\2\2\2L\u014f\3\2"+
		"\2\2N\u0151\3\2\2\2P\u0153\3\2\2\2R\u0155\3\2\2\2T\u0157\3\2\2\2V\u015a"+
		"\3\2\2\2X\u015c\3\2\2\2Z\u015e\3\2\2\2\\\u0161\3\2\2\2^\u0164\3\2\2\2"+
		"`\u0166\3\2\2\2b\u0168\3\2\2\2d\u016c\3\2\2\2f\u016e\3\2\2\2h\u0176\3"+
		"\2\2\2j\u0180\3\2\2\2l\u0182\3\2\2\2n\u0187\3\2\2\2p\u018c\3\2\2\2r\u0198"+
		"\3\2\2\2t\u019c\3\2\2\2v\u01a5\3\2\2\2x\u01b1\3\2\2\2z~\7}\2\2{}\5\30"+
		"\13\2|{\3\2\2\2}\u0080\3\2\2\2~|\3\2\2\2~\177\3\2\2\2\177\u0081\3\2\2"+
		"\2\u0080~\3\2\2\2\u0081\u0082\7h\2\2\u0082\u0083\7q\2\2\u0083\u0084\7"+
		"t\2\2\u0084\7\3\2\2\2\u0085\u0089\7]\2\2\u0086\u0088\5\30\13\2\u0087\u0086"+
		"\3\2\2\2\u0088\u008b\3\2\2\2\u0089\u0087\3\2\2\2\u0089\u008a\3\2\2\2\u008a"+
		"\u008c\3\2\2\2\u008b\u0089\3\2\2\2\u008c\u008d\7h\2\2\u008d\u008e\7q\2"+
		"\2\u008e\u008f\7t\2\2\u008f\t\3\2\2\2\u0090\u0091\7k\2\2\u0091\u0092\7"+
		"h\2\2\u0092\13\3\2\2\2\u0093\u0094\7k\2\2\u0094\u0095\7p\2\2\u0095\r\3"+
		"\2\2\2\u0096\u0097\7}\2\2\u0097\u0098\b\6\2\2\u0098\17\3\2\2\2\u0099\u009a"+
		"\7\177\2\2\u009a\u009b\b\7\3\2\u009b\21\3\2\2\2\u009c\u009d\7?\2\2\u009d"+
		"\23\3\2\2\2\u009e\u00a1\n\2\2\2\u009f\u00a1\5$\21\2\u00a0\u009e\3\2\2"+
		"\2\u00a0\u009f\3\2\2\2\u00a1\25\3\2\2\2\u00a2\u00a7\5\"\20\2\u00a3\u00a6"+
		"\5 \17\2\u00a4\u00a6\7/\2\2\u00a5\u00a3\3\2\2\2\u00a5\u00a4\3\2\2\2\u00a6"+
		"\u00a9\3\2\2\2\u00a7\u00a5\3\2\2\2\u00a7\u00a8\3\2\2\2\u00a8\27\3\2\2"+
		"\2\u00a9\u00a7\3\2\2\2\u00aa\u00ac\t\3\2\2\u00ab\u00aa\3\2\2\2\u00ac\u00ad"+
		"\3\2\2\2\u00ad\u00ab\3\2\2\2\u00ad\u00ae\3\2\2\2\u00ae\u00af\3\2\2\2\u00af"+
		"\u00b0\b\13\4\2\u00b0\31\3\2\2\2\u00b1\u00b2\7\61\2\2\u00b2\u00b3\7,\2"+
		"\2\u00b3\u00b7\3\2\2\2\u00b4\u00b6\13\2\2\2\u00b5\u00b4\3\2\2\2\u00b6"+
		"\u00b9\3\2\2\2\u00b7\u00b8\3\2\2\2\u00b7\u00b5\3\2\2\2\u00b8\u00ba\3\2"+
		"\2\2\u00b9\u00b7\3\2\2\2\u00ba\u00bb\7,\2\2\u00bb\u00bc\7\61\2\2\u00bc"+
		"\u00bd\3\2\2\2\u00bd\u00be\b\f\4\2\u00be\33\3\2\2\2\u00bf\u00c0\7\61\2"+
		"\2\u00c0\u00c3\7\61\2\2\u00c1\u00c3\7%\2\2\u00c2\u00bf\3\2\2\2\u00c2\u00c1"+
		"\3\2\2\2\u00c3\u00c7\3\2\2\2\u00c4\u00c6\n\4\2\2\u00c5\u00c4\3\2\2\2\u00c6"+
		"\u00c9\3\2\2\2\u00c7\u00c5\3\2\2\2\u00c7\u00c8\3\2\2\2\u00c8\u00ca\3\2"+
		"\2\2\u00c9\u00c7\3\2\2\2\u00ca\u00cb\7\f\2\2\u00cb\u00cc\3\2\2\2\u00cc"+
		"\u00cd\b\r\4\2\u00cd\35\3\2\2\2\u00ce\u00cf\7\f\2\2\u00cf\u00d0\3\2\2"+
		"\2\u00d0\u00d1\b\16\4\2\u00d1\37\3\2\2\2\u00d2\u00d5\5\"\20\2\u00d3\u00d5"+
		"\t\5\2\2\u00d4\u00d2\3\2\2\2\u00d4\u00d3\3\2\2\2\u00d5!\3\2\2\2\u00d6"+
		"\u00db\t\6\2\2\u00d7\u00db\n\7\2\2\u00d8\u00d9\t\b\2\2\u00d9\u00db\t\t"+
		"\2\2\u00da\u00d6\3\2\2\2\u00da\u00d7\3\2\2\2\u00da\u00d8\3\2\2\2\u00db"+
		"#\3\2\2\2\u00dc\u00dd\7^\2\2\u00dd\u00ef\t\n\2\2\u00de\u00df\7^\2\2\u00df"+
		"\u00e0\5&\22\2\u00e0\u00e1\5&\22\2\u00e1\u00e2\5&\22\2\u00e2\u00e3\5&"+
		"\22\2\u00e3\u00ef\3\2\2\2\u00e4\u00e5\7^\2\2\u00e5\u00e6\5&\22\2\u00e6"+
		"\u00e7\5&\22\2\u00e7\u00e8\5&\22\2\u00e8\u00e9\5&\22\2\u00e9\u00ea\5&"+
		"\22\2\u00ea\u00eb\5&\22\2\u00eb\u00ec\5&\22\2\u00ec\u00ed\5&\22\2\u00ed"+
		"\u00ef\3\2\2\2\u00ee\u00dc\3\2\2\2\u00ee\u00de\3\2\2\2\u00ee\u00e4\3\2"+
		"\2\2\u00ef%\3\2\2\2\u00f0\u00f1\t\13\2\2\u00f1\'\3\2\2\2\u00f2\u00f4\t"+
		"\5\2\2\u00f3\u00f2\3\2\2\2\u00f4\u00f5\3\2\2\2\u00f5\u00f3\3\2\2\2\u00f5"+
		"\u00f6\3\2\2\2\u00f6\u00f7\3\2\2\2\u00f7\u00fb\7\60\2\2\u00f8\u00fa\t"+
		"\5\2\2\u00f9\u00f8\3\2\2\2\u00fa\u00fd\3\2\2\2\u00fb\u00f9\3\2\2\2\u00fb"+
		"\u00fc\3\2\2\2\u00fc\u00ff\3\2\2\2\u00fd\u00fb\3\2\2\2\u00fe\u0100\5*"+
		"\24\2\u00ff\u00fe\3\2\2\2\u00ff\u0100\3\2\2\2\u0100\u010d\3\2\2\2\u0101"+
		"\u0103\t\5\2\2\u0102\u0101\3\2\2\2\u0103\u0104\3\2\2\2\u0104\u0102\3\2"+
		"\2\2\u0104\u0105\3\2\2\2\u0105\u0106\3\2\2\2\u0106\u010d\5*\24\2\u0107"+
		"\u0109\t\5\2\2\u0108\u0107\3\2\2\2\u0109\u010a\3\2\2\2\u010a\u0108\3\2"+
		"\2\2\u010a\u010b\3\2\2\2\u010b\u010d\3\2\2\2\u010c\u00f3\3\2\2\2\u010c"+
		"\u0102\3\2\2\2\u010c\u0108\3\2\2\2\u010d)\3\2\2\2\u010e\u0110\t\f\2\2"+
		"\u010f\u0111\t\r\2\2\u0110\u010f\3\2\2\2\u0110\u0111\3\2\2\2\u0111\u0113"+
		"\3\2\2\2\u0112\u0114\t\5\2\2\u0113\u0112\3\2\2\2\u0114\u0115\3\2\2\2\u0115"+
		"\u0113\3\2\2\2\u0115\u0116\3\2\2\2\u0116+\3\2\2\2\u0117\u0118\7v\2\2\u0118"+
		"\u0119\7t\2\2\u0119\u011a\7w\2\2\u011a\u0121\7g\2\2\u011b\u011c\7h\2\2"+
		"\u011c\u011d\7c\2\2\u011d\u011e\7n\2\2\u011e\u011f\7u\2\2\u011f\u0121"+
		"\7g\2\2\u0120\u0117\3\2\2\2\u0120\u011b\3\2\2\2\u0121-\3\2\2\2\u0122\u0123"+
		"\7$\2\2\u0123\u0124\3\2\2\2\u0124\u0125\b\26\5\2\u0125/\3\2\2\2\u0126"+
		"\u0127\7p\2\2\u0127\u0128\7w\2\2\u0128\u0129\7n\2\2\u0129\u012a\7n\2\2"+
		"\u012a\61\3\2\2\2\u012b\u012c\7>\2\2\u012c\u012d\7>\2\2\u012d\u012f\3"+
		"\2\2\2\u012e\u0130\7/\2\2\u012f\u012e\3\2\2\2\u012f\u0130\3\2\2\2\u0130"+
		"\u0131\3\2\2\2\u0131\u0132\b\30\6\2\u0132\63\3\2\2\2\u0133\u0134\7-\2"+
		"\2\u0134\65\3\2\2\2\u0135\u0136\7(\2\2\u0136\u0137\7(\2\2\u0137\67\3\2"+
		"\2\2\u0138\u0139\7?\2\2\u0139\u013a\7?\2\2\u013a9\3\2\2\2\u013b\u013c"+
		"\7>\2\2\u013c;\3\2\2\2\u013d\u013e\7<\2\2\u013e=\3\2\2\2\u013f\u0140\7"+
		"]\2\2\u0140?\3\2\2\2\u0141\u0142\7*\2\2\u0142A\3\2\2\2\u0143\u0144\7/"+
		"\2\2\u0144C\3\2\2\2\u0145\u0146\7~\2\2\u0146\u0147\7~\2\2\u0147E\3\2\2"+
		"\2\u0148\u0149\7#\2\2\u0149\u014a\7?\2\2\u014aG\3\2\2\2\u014b\u014c\7"+
		"@\2\2\u014cI\3\2\2\2\u014d\u014e\7A\2\2\u014eK\3\2\2\2\u014f\u0150\7_"+
		"\2\2\u0150M\3\2\2\2\u0151\u0152\7+\2\2\u0152O\3\2\2\2\u0153\u0154\7,\2"+
		"\2\u0154Q\3\2\2\2\u0155\u0156\7#\2\2\u0156S\3\2\2\2\u0157\u0158\7>\2\2"+
		"\u0158\u0159\7?\2\2\u0159U\3\2\2\2\u015a\u015b\7\60\2\2\u015bW\3\2\2\2"+
		"\u015c\u015d\7\61\2\2\u015dY\3\2\2\2\u015e\u015f\7@\2\2\u015f\u0160\7"+
		"?\2\2\u0160[\3\2\2\2\u0161\u0162\7?\2\2\u0162\u0163\7@\2\2\u0163]\3\2"+
		"\2\2\u0164\u0165\7.\2\2\u0165_\3\2\2\2\u0166\u0167\7\'\2\2\u0167a\3\2"+
		"\2\2\u0168\u0169\7\60\2\2\u0169\u016a\7\60\2\2\u016a\u016b\7\60\2\2\u016b"+
		"c\3\2\2\2\u016c\u016d\7\u0080\2\2\u016de\3\2\2\2\u016e\u016f\7&\2\2\u016f"+
		"\u0170\7}\2\2\u0170\u0171\3\2\2\2\u0171\u0172\b\62\7\2\u0172\u0173\3\2"+
		"\2\2\u0173\u0174\b\62\b\2\u0174g\3\2\2\2\u0175\u0177\5j\64\2\u0176\u0175"+
		"\3\2\2\2\u0177\u0178\3\2\2\2\u0178\u0176\3\2\2\2\u0178\u0179\3\2\2\2\u0179"+
		"i\3\2\2\2\u017a\u0181\n\2\2\2\u017b\u017c\7&\2\2\u017c\u0181\n\16\2\2"+
		"\u017d\u017e\7\'\2\2\u017e\u0181\n\16\2\2\u017f\u0181\5$\21\2\u0180\u017a"+
		"\3\2\2\2\u0180\u017b\3\2\2\2\u0180\u017d\3\2\2\2\u0180\u017f\3\2\2\2\u0181"+
		"k\3\2\2\2\u0182\u0183\7$\2\2\u0183\u0184\3\2\2\2\u0184\u0185\b\65\t\2"+
		"\u0185\u0186\b\65\n\2\u0186m\3\2\2\2\u0187\u0188\7\f\2\2\u0188\u0189\3"+
		"\2\2\2\u0189\u018a\b\66\13\2\u018a\u018b\b\66\f\2\u018bo\3\2\2\2\u018c"+
		"\u0191\5\"\20\2\u018d\u0190\5 \17\2\u018e\u0190\7/\2\2\u018f\u018d\3\2"+
		"\2\2\u018f\u018e\3\2\2\2\u0190\u0193\3\2\2\2\u0191\u018f\3\2\2\2\u0191"+
		"\u0192\3\2\2\2\u0192\u0194\3\2\2\2\u0193\u0191\3\2\2\2\u0194\u0195\b\67"+
		"\r\2\u0195\u0196\3\2\2\2\u0196\u0197\b\67\16\2\u0197q\3\2\2\2\u0198\u0199"+
		"\7\f\2\2\u0199\u019a\3\2\2\2\u019a\u019b\b8\13\2\u019bs\3\2\2\2\u019c"+
		"\u019d\7&\2\2\u019d\u019e\7}\2\2\u019e\u019f\3\2\2\2\u019f\u01a0\b9\17"+
		"\2\u01a0\u01a1\3\2\2\2\u01a1\u01a2\b9\20\2\u01a2\u01a3\b9\b\2\u01a3u\3"+
		"\2\2\2\u01a4\u01a6\5x;\2\u01a5\u01a4\3\2\2\2\u01a6\u01a7\3\2\2\2\u01a7"+
		"\u01a5\3\2\2\2\u01a7\u01a8\3\2\2\2\u01a8\u01a9\3\2\2\2\u01a9\u01aa\b:"+
		"\21\2\u01aaw\3\2\2\2\u01ab\u01b2\n\17\2\2\u01ac\u01ad\7&\2\2\u01ad\u01b2"+
		"\n\16\2\2\u01ae\u01af\7\'\2\2\u01af\u01b2\n\16\2\2\u01b0\u01b2\5$\21\2"+
		"\u01b1\u01ab\3\2\2\2\u01b1\u01ac\3\2\2\2\u01b1\u01ae\3\2\2\2\u01b1\u01b0"+
		"\3\2\2\2\u01b2y\3\2\2\2\"\2\3\4\5~\u0089\u00a0\u00a5\u00a7\u00ad\u00b7"+
		"\u00c2\u00c7\u00d4\u00da\u00ee\u00f5\u00fb\u00ff\u0104\u010a\u010c\u0110"+
		"\u0115\u0120\u012f\u0178\u0180\u018f\u0191\u01a7\u01b1\22\3\6\2\3\7\3"+
		"\2\3\2\7\3\2\7\4\2\3\62\4\7\2\2\t\21\2\6\2\2\t\16\2\4\5\2\3\67\5\t\n\2"+
		"\39\6\t-\2\3:\7";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}