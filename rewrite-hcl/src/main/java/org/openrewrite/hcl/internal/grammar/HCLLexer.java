// Generated from java-escape by ANTLR 4.11.1
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

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class HCLLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.11.1", RuntimeMetaData.VERSION); }

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
	@Override
	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 50:
			return TemplateStringLiteralChar_sempred((RuleContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean TemplateStringLiteralChar_sempred(RuleContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return _input.LA(1) != '{';
		case 1:
			return _input.LA(1) != '{';
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u0000/\u01c2\u0006\uffff\uffff\u0006\uffff\uffff\u0006\uffff\uffff"+
		"\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
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
		"7\u00077\u00028\u00078\u00029\u00079\u0001\u0000\u0001\u0000\u0001\u0000"+
		"\u0001\u0000\u0001\u0000\u0005\u0000~\b\u0000\n\u0000\f\u0000\u0081\t"+
		"\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001"+
		"\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0005"+
		"\u0001\u008e\b\u0001\n\u0001\f\u0001\u0091\t\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0004\u0001\u0004"+
		"\u0001\u0004\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0006\u0001\u0006"+
		"\u0001\u0007\u0001\u0007\u0003\u0007\u00a9\b\u0007\u0001\b\u0001\b\u0001"+
		"\b\u0005\b\u00ae\b\b\n\b\f\b\u00b1\t\b\u0001\t\u0004\t\u00b4\b\t\u000b"+
		"\t\f\t\u00b5\u0001\t\u0001\t\u0001\n\u0001\n\u0001\n\u0001\n\u0005\n\u00be"+
		"\b\n\n\n\f\n\u00c1\t\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\u000b"+
		"\u0001\u000b\u0001\u000b\u0003\u000b\u00cb\b\u000b\u0001\u000b\u0005\u000b"+
		"\u00ce\b\u000b\n\u000b\f\u000b\u00d1\t\u000b\u0001\u000b\u0003\u000b\u00d4"+
		"\b\u000b\u0001\u000b\u0003\u000b\u00d7\b\u000b\u0001\u000b\u0001\u000b"+
		"\u0001\f\u0001\f\u0001\f\u0001\f\u0001\r\u0001\r\u0003\r\u00e1\b\r\u0001"+
		"\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0003\u000e\u00e7\b\u000e\u0001"+
		"\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001"+
		"\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001"+
		"\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0003"+
		"\u000f\u00fb\b\u000f\u0001\u0010\u0001\u0010\u0001\u0011\u0004\u0011\u0100"+
		"\b\u0011\u000b\u0011\f\u0011\u0101\u0001\u0011\u0001\u0011\u0004\u0011"+
		"\u0106\b\u0011\u000b\u0011\f\u0011\u0107\u0001\u0011\u0003\u0011\u010b"+
		"\b\u0011\u0001\u0011\u0004\u0011\u010e\b\u0011\u000b\u0011\f\u0011\u010f"+
		"\u0001\u0011\u0001\u0011\u0004\u0011\u0114\b\u0011\u000b\u0011\f\u0011"+
		"\u0115\u0003\u0011\u0118\b\u0011\u0001\u0012\u0001\u0012\u0003\u0012\u011c"+
		"\b\u0012\u0001\u0012\u0004\u0012\u011f\b\u0012\u000b\u0012\f\u0012\u0120"+
		"\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013"+
		"\u0001\u0013\u0001\u0013\u0001\u0013\u0003\u0013\u012c\b\u0013\u0001\u0014"+
		"\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0015\u0001\u0015\u0001\u0015"+
		"\u0001\u0015\u0001\u0015\u0001\u0016\u0001\u0016\u0001\u0016\u0001\u0016"+
		"\u0003\u0016\u013b\b\u0016\u0001\u0016\u0001\u0016\u0001\u0017\u0001\u0017"+
		"\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0019\u0001\u0019\u0001\u0019"+
		"\u0001\u001a\u0001\u001a\u0001\u001b\u0001\u001b\u0001\u001c\u0001\u001c"+
		"\u0001\u001d\u0001\u001d\u0001\u001e\u0001\u001e\u0001\u001f\u0001\u001f"+
		"\u0001\u001f\u0001 \u0001 \u0001 \u0001!\u0001!\u0001\"\u0001\"\u0001"+
		"#\u0001#\u0001$\u0001$\u0001%\u0001%\u0001&\u0001&\u0001\'\u0001\'\u0001"+
		"\'\u0001(\u0001(\u0001)\u0001)\u0001*\u0001*\u0001*\u0001+\u0001+\u0001"+
		"+\u0001,\u0001,\u0001-\u0001-\u0001.\u0001.\u0001.\u0001.\u0001/\u0001"+
		"/\u00010\u00010\u00010\u00010\u00010\u00010\u00010\u00011\u00041\u0182"+
		"\b1\u000b1\f1\u0183\u00012\u00012\u00012\u00012\u00012\u00012\u00012\u0001"+
		"2\u00012\u00012\u00032\u0190\b2\u00013\u00013\u00013\u00013\u00013\u0001"+
		"4\u00014\u00014\u00014\u00014\u00015\u00015\u00015\u00055\u019f\b5\n5"+
		"\f5\u01a2\t5\u00015\u00015\u00015\u00015\u00016\u00016\u00016\u00016\u0001"+
		"7\u00017\u00017\u00017\u00017\u00017\u00017\u00017\u00018\u00048\u01b5"+
		"\b8\u000b8\f8\u01b6\u00018\u00018\u00019\u00019\u00019\u00019\u00019\u0001"+
		"9\u00039\u01c1\b9\u0001\u00bf\u0000:\u0004\u0001\u0006\u0002\b\u0003\n"+
		"\u0004\f\u0005\u000e\u0006\u0010\u0007\u0012\u0000\u0014\b\u0016\t\u0018"+
		"\n\u001a\u000b\u001c\f\u001e\u0000 \u0000\"\u0000$\u0000&\r(\u0000*\u000e"+
		",\u000f.\u00100\u00112\u00124\u00136\u00148\u0015:\u0016<\u0017>\u0018"+
		"@\u0019B\u001aD\u001bF\u001cH\u001dJ\u001eL\u001fN P!R\"T#V$X%Z&\\\'^"+
		"(`)b*d+f,h-j\u0000l\u0000n\u0000p\u0000r\u0000t.v/\u0004\u0000\u0001\u0002"+
		"\u0003\u000f\u0004\u0000\n\n\r\r\"\"$%\u0003\u0000\t\t\f\r  \u0002\u0000"+
		"\n\n\r\r\u0001\u0001\n\n\u0001\u000009\u0004\u0000$$AZ__az\u0002\u0000"+
		"\u0000\u007f\u8000\ud800\u8000\udbff\u0001\u0000\u8000\ud800\u8000\udbff"+
		"\u0001\u0000\u8000\udc00\u8000\udfff\u0005\u0000\"\"\\\\nnrrtt\u0003\u0000"+
		"09AFaf\u0002\u0000EEee\u0002\u0000++--\u0003\u0000\n\n\r\r$%\u0001\u0000"+
		"{{\u01e4\u0000\u0004\u0001\u0000\u0000\u0000\u0000\u0006\u0001\u0000\u0000"+
		"\u0000\u0000\b\u0001\u0000\u0000\u0000\u0000\n\u0001\u0000\u0000\u0000"+
		"\u0000\f\u0001\u0000\u0000\u0000\u0000\u000e\u0001\u0000\u0000\u0000\u0000"+
		"\u0010\u0001\u0000\u0000\u0000\u0000\u0014\u0001\u0000\u0000\u0000\u0000"+
		"\u0016\u0001\u0000\u0000\u0000\u0000\u0018\u0001\u0000\u0000\u0000\u0000"+
		"\u001a\u0001\u0000\u0000\u0000\u0000\u001c\u0001\u0000\u0000\u0000\u0000"+
		"&\u0001\u0000\u0000\u0000\u0000*\u0001\u0000\u0000\u0000\u0000,\u0001"+
		"\u0000\u0000\u0000\u0000.\u0001\u0000\u0000\u0000\u00000\u0001\u0000\u0000"+
		"\u0000\u00002\u0001\u0000\u0000\u0000\u00004\u0001\u0000\u0000\u0000\u0000"+
		"6\u0001\u0000\u0000\u0000\u00008\u0001\u0000\u0000\u0000\u0000:\u0001"+
		"\u0000\u0000\u0000\u0000<\u0001\u0000\u0000\u0000\u0000>\u0001\u0000\u0000"+
		"\u0000\u0000@\u0001\u0000\u0000\u0000\u0000B\u0001\u0000\u0000\u0000\u0000"+
		"D\u0001\u0000\u0000\u0000\u0000F\u0001\u0000\u0000\u0000\u0000H\u0001"+
		"\u0000\u0000\u0000\u0000J\u0001\u0000\u0000\u0000\u0000L\u0001\u0000\u0000"+
		"\u0000\u0000N\u0001\u0000\u0000\u0000\u0000P\u0001\u0000\u0000\u0000\u0000"+
		"R\u0001\u0000\u0000\u0000\u0000T\u0001\u0000\u0000\u0000\u0000V\u0001"+
		"\u0000\u0000\u0000\u0000X\u0001\u0000\u0000\u0000\u0000Z\u0001\u0000\u0000"+
		"\u0000\u0000\\\u0001\u0000\u0000\u0000\u0000^\u0001\u0000\u0000\u0000"+
		"\u0000`\u0001\u0000\u0000\u0000\u0000b\u0001\u0000\u0000\u0000\u0001d"+
		"\u0001\u0000\u0000\u0000\u0001f\u0001\u0000\u0000\u0000\u0001h\u0001\u0000"+
		"\u0000\u0000\u0001j\u0001\u0000\u0000\u0000\u0002l\u0001\u0000\u0000\u0000"+
		"\u0002n\u0001\u0000\u0000\u0000\u0003p\u0001\u0000\u0000\u0000\u0003r"+
		"\u0001\u0000\u0000\u0000\u0003t\u0001\u0000\u0000\u0000\u0003v\u0001\u0000"+
		"\u0000\u0000\u0004x\u0001\u0000\u0000\u0000\u0006\u0088\u0001\u0000\u0000"+
		"\u0000\b\u0098\u0001\u0000\u0000\u0000\n\u009b\u0001\u0000\u0000\u0000"+
		"\f\u009e\u0001\u0000\u0000\u0000\u000e\u00a1\u0001\u0000\u0000\u0000\u0010"+
		"\u00a4\u0001\u0000\u0000\u0000\u0012\u00a8\u0001\u0000\u0000\u0000\u0014"+
		"\u00aa\u0001\u0000\u0000\u0000\u0016\u00b3\u0001\u0000\u0000\u0000\u0018"+
		"\u00b9\u0001\u0000\u0000\u0000\u001a\u00ca\u0001\u0000\u0000\u0000\u001c"+
		"\u00da\u0001\u0000\u0000\u0000\u001e\u00e0\u0001\u0000\u0000\u0000 \u00e6"+
		"\u0001\u0000\u0000\u0000\"\u00fa\u0001\u0000\u0000\u0000$\u00fc\u0001"+
		"\u0000\u0000\u0000&\u0117\u0001\u0000\u0000\u0000(\u0119\u0001\u0000\u0000"+
		"\u0000*\u012b\u0001\u0000\u0000\u0000,\u012d\u0001\u0000\u0000\u0000."+
		"\u0131\u0001\u0000\u0000\u00000\u0136\u0001\u0000\u0000\u00002\u013e\u0001"+
		"\u0000\u0000\u00004\u0140\u0001\u0000\u0000\u00006\u0143\u0001\u0000\u0000"+
		"\u00008\u0146\u0001\u0000\u0000\u0000:\u0148\u0001\u0000\u0000\u0000<"+
		"\u014a\u0001\u0000\u0000\u0000>\u014c\u0001\u0000\u0000\u0000@\u014e\u0001"+
		"\u0000\u0000\u0000B\u0150\u0001\u0000\u0000\u0000D\u0153\u0001\u0000\u0000"+
		"\u0000F\u0156\u0001\u0000\u0000\u0000H\u0158\u0001\u0000\u0000\u0000J"+
		"\u015a\u0001\u0000\u0000\u0000L\u015c\u0001\u0000\u0000\u0000N\u015e\u0001"+
		"\u0000\u0000\u0000P\u0160\u0001\u0000\u0000\u0000R\u0162\u0001\u0000\u0000"+
		"\u0000T\u0165\u0001\u0000\u0000\u0000V\u0167\u0001\u0000\u0000\u0000X"+
		"\u0169\u0001\u0000\u0000\u0000Z\u016c\u0001\u0000\u0000\u0000\\\u016f"+
		"\u0001\u0000\u0000\u0000^\u0171\u0001\u0000\u0000\u0000`\u0173\u0001\u0000"+
		"\u0000\u0000b\u0177\u0001\u0000\u0000\u0000d\u0179\u0001\u0000\u0000\u0000"+
		"f\u0181\u0001\u0000\u0000\u0000h\u018f\u0001\u0000\u0000\u0000j\u0191"+
		"\u0001\u0000\u0000\u0000l\u0196\u0001\u0000\u0000\u0000n\u019b\u0001\u0000"+
		"\u0000\u0000p\u01a7\u0001\u0000\u0000\u0000r\u01ab\u0001\u0000\u0000\u0000"+
		"t\u01b4\u0001\u0000\u0000\u0000v\u01c0\u0001\u0000\u0000\u0000x\u007f"+
		"\u0005{\u0000\u0000y~\u0003\u0016\t\u0000z~\u0003\u001c\f\u0000{~\u0003"+
		"\u0018\n\u0000|~\u0003\u001a\u000b\u0000}y\u0001\u0000\u0000\u0000}z\u0001"+
		"\u0000\u0000\u0000}{\u0001\u0000\u0000\u0000}|\u0001\u0000\u0000\u0000"+
		"~\u0081\u0001\u0000\u0000\u0000\u007f}\u0001\u0000\u0000\u0000\u007f\u0080"+
		"\u0001\u0000\u0000\u0000\u0080\u0082\u0001\u0000\u0000\u0000\u0081\u007f"+
		"\u0001\u0000\u0000\u0000\u0082\u0083\u0005f\u0000\u0000\u0083\u0084\u0005"+
		"o\u0000\u0000\u0084\u0085\u0005r\u0000\u0000\u0085\u0086\u0001\u0000\u0000"+
		"\u0000\u0086\u0087\u0003\u0016\t\u0000\u0087\u0005\u0001\u0000\u0000\u0000"+
		"\u0088\u008f\u0005[\u0000\u0000\u0089\u008e\u0003\u0016\t\u0000\u008a"+
		"\u008e\u0003\u001c\f\u0000\u008b\u008e\u0003\u0018\n\u0000\u008c\u008e"+
		"\u0003\u001a\u000b\u0000\u008d\u0089\u0001\u0000\u0000\u0000\u008d\u008a"+
		"\u0001\u0000\u0000\u0000\u008d\u008b\u0001\u0000\u0000\u0000\u008d\u008c"+
		"\u0001\u0000\u0000\u0000\u008e\u0091\u0001\u0000\u0000\u0000\u008f\u008d"+
		"\u0001\u0000\u0000\u0000\u008f\u0090\u0001\u0000\u0000\u0000\u0090\u0092"+
		"\u0001\u0000\u0000\u0000\u0091\u008f\u0001\u0000\u0000\u0000\u0092\u0093"+
		"\u0005f\u0000\u0000\u0093\u0094\u0005o\u0000\u0000\u0094\u0095\u0005r"+
		"\u0000\u0000\u0095\u0096\u0001\u0000\u0000\u0000\u0096\u0097\u0003\u0016"+
		"\t\u0000\u0097\u0007\u0001\u0000\u0000\u0000\u0098\u0099\u0005i\u0000"+
		"\u0000\u0099\u009a\u0005f\u0000\u0000\u009a\t\u0001\u0000\u0000\u0000"+
		"\u009b\u009c\u0005i\u0000\u0000\u009c\u009d\u0005n\u0000\u0000\u009d\u000b"+
		"\u0001\u0000\u0000\u0000\u009e\u009f\u0005{\u0000\u0000\u009f\u00a0\u0006"+
		"\u0004\u0000\u0000\u00a0\r\u0001\u0000\u0000\u0000\u00a1\u00a2\u0005}"+
		"\u0000\u0000\u00a2\u00a3\u0006\u0005\u0001\u0000\u00a3\u000f\u0001\u0000"+
		"\u0000\u0000\u00a4\u00a5\u0005=\u0000\u0000\u00a5\u0011\u0001\u0000\u0000"+
		"\u0000\u00a6\u00a9\b\u0000\u0000\u0000\u00a7\u00a9\u0003\"\u000f\u0000"+
		"\u00a8\u00a6\u0001\u0000\u0000\u0000\u00a8\u00a7\u0001\u0000\u0000\u0000"+
		"\u00a9\u0013\u0001\u0000\u0000\u0000\u00aa\u00af\u0003 \u000e\u0000\u00ab"+
		"\u00ae\u0003\u001e\r\u0000\u00ac\u00ae\u0005-\u0000\u0000\u00ad\u00ab"+
		"\u0001\u0000\u0000\u0000\u00ad\u00ac\u0001\u0000\u0000\u0000\u00ae\u00b1"+
		"\u0001\u0000\u0000\u0000\u00af\u00ad\u0001\u0000\u0000\u0000\u00af\u00b0"+
		"\u0001\u0000\u0000\u0000\u00b0\u0015\u0001\u0000\u0000\u0000\u00b1\u00af"+
		"\u0001\u0000\u0000\u0000\u00b2\u00b4\u0007\u0001\u0000\u0000\u00b3\u00b2"+
		"\u0001\u0000\u0000\u0000\u00b4\u00b5\u0001\u0000\u0000\u0000\u00b5\u00b3"+
		"\u0001\u0000\u0000\u0000\u00b5\u00b6\u0001\u0000\u0000\u0000\u00b6\u00b7"+
		"\u0001\u0000\u0000\u0000\u00b7\u00b8\u0006\t\u0002\u0000\u00b8\u0017\u0001"+
		"\u0000\u0000\u0000\u00b9\u00ba\u0005/\u0000\u0000\u00ba\u00bb\u0005*\u0000"+
		"\u0000\u00bb\u00bf\u0001\u0000\u0000\u0000\u00bc\u00be\t\u0000\u0000\u0000"+
		"\u00bd\u00bc\u0001\u0000\u0000\u0000\u00be\u00c1\u0001\u0000\u0000\u0000"+
		"\u00bf\u00c0\u0001\u0000\u0000\u0000\u00bf\u00bd\u0001\u0000\u0000\u0000"+
		"\u00c0\u00c2\u0001\u0000\u0000\u0000\u00c1\u00bf\u0001\u0000\u0000\u0000"+
		"\u00c2\u00c3\u0005*\u0000\u0000\u00c3\u00c4\u0005/\u0000\u0000\u00c4\u00c5"+
		"\u0001\u0000\u0000\u0000\u00c5\u00c6\u0006\n\u0002\u0000\u00c6\u0019\u0001"+
		"\u0000\u0000\u0000\u00c7\u00c8\u0005/\u0000\u0000\u00c8\u00cb\u0005/\u0000"+
		"\u0000\u00c9\u00cb\u0005#\u0000\u0000\u00ca\u00c7\u0001\u0000\u0000\u0000"+
		"\u00ca\u00c9\u0001\u0000\u0000\u0000\u00cb\u00cf\u0001\u0000\u0000\u0000"+
		"\u00cc\u00ce\b\u0002\u0000\u0000\u00cd\u00cc\u0001\u0000\u0000\u0000\u00ce"+
		"\u00d1\u0001\u0000\u0000\u0000\u00cf\u00cd\u0001\u0000\u0000\u0000\u00cf"+
		"\u00d0\u0001\u0000\u0000\u0000\u00d0\u00d3\u0001\u0000\u0000\u0000\u00d1"+
		"\u00cf\u0001\u0000\u0000\u0000\u00d2\u00d4\u0005\r\u0000\u0000\u00d3\u00d2"+
		"\u0001\u0000\u0000\u0000\u00d3\u00d4\u0001\u0000\u0000\u0000\u00d4\u00d6"+
		"\u0001\u0000\u0000\u0000\u00d5\u00d7\u0007\u0003\u0000\u0000\u00d6\u00d5"+
		"\u0001\u0000\u0000\u0000\u00d7\u00d8\u0001\u0000\u0000\u0000\u00d8\u00d9"+
		"\u0006\u000b\u0002\u0000\u00d9\u001b\u0001\u0000\u0000\u0000\u00da\u00db"+
		"\u0005\n\u0000\u0000\u00db\u00dc\u0001\u0000\u0000\u0000\u00dc\u00dd\u0006"+
		"\f\u0002\u0000\u00dd\u001d\u0001\u0000\u0000\u0000\u00de\u00e1\u0003 "+
		"\u000e\u0000\u00df\u00e1\u0007\u0004\u0000\u0000\u00e0\u00de\u0001\u0000"+
		"\u0000\u0000\u00e0\u00df\u0001\u0000\u0000\u0000\u00e1\u001f\u0001\u0000"+
		"\u0000\u0000\u00e2\u00e7\u0007\u0005\u0000\u0000\u00e3\u00e7\b\u0006\u0000"+
		"\u0000\u00e4\u00e5\u0007\u0007\u0000\u0000\u00e5\u00e7\u0007\b\u0000\u0000"+
		"\u00e6\u00e2\u0001\u0000\u0000\u0000\u00e6\u00e3\u0001\u0000\u0000\u0000"+
		"\u00e6\u00e4\u0001\u0000\u0000\u0000\u00e7!\u0001\u0000\u0000\u0000\u00e8"+
		"\u00e9\u0005\\\u0000\u0000\u00e9\u00fb\u0007\t\u0000\u0000\u00ea\u00eb"+
		"\u0005\\\u0000\u0000\u00eb\u00ec\u0003$\u0010\u0000\u00ec\u00ed\u0003"+
		"$\u0010\u0000\u00ed\u00ee\u0003$\u0010\u0000\u00ee\u00ef\u0003$\u0010"+
		"\u0000\u00ef\u00fb\u0001\u0000\u0000\u0000\u00f0\u00f1\u0005\\\u0000\u0000"+
		"\u00f1\u00f2\u0003$\u0010\u0000\u00f2\u00f3\u0003$\u0010\u0000\u00f3\u00f4"+
		"\u0003$\u0010\u0000\u00f4\u00f5\u0003$\u0010\u0000\u00f5\u00f6\u0003$"+
		"\u0010\u0000\u00f6\u00f7\u0003$\u0010\u0000\u00f7\u00f8\u0003$\u0010\u0000"+
		"\u00f8\u00f9\u0003$\u0010\u0000\u00f9\u00fb\u0001\u0000\u0000\u0000\u00fa"+
		"\u00e8\u0001\u0000\u0000\u0000\u00fa\u00ea\u0001\u0000\u0000\u0000\u00fa"+
		"\u00f0\u0001\u0000\u0000\u0000\u00fb#\u0001\u0000\u0000\u0000\u00fc\u00fd"+
		"\u0007\n\u0000\u0000\u00fd%\u0001\u0000\u0000\u0000\u00fe\u0100\u0007"+
		"\u0004\u0000\u0000\u00ff\u00fe\u0001\u0000\u0000\u0000\u0100\u0101\u0001"+
		"\u0000\u0000\u0000\u0101\u00ff\u0001\u0000\u0000\u0000\u0101\u0102\u0001"+
		"\u0000\u0000\u0000\u0102\u0103\u0001\u0000\u0000\u0000\u0103\u0105\u0005"+
		".\u0000\u0000\u0104\u0106\u0007\u0004\u0000\u0000\u0105\u0104\u0001\u0000"+
		"\u0000\u0000\u0106\u0107\u0001\u0000\u0000\u0000\u0107\u0105\u0001\u0000"+
		"\u0000\u0000\u0107\u0108\u0001\u0000\u0000\u0000\u0108\u010a\u0001\u0000"+
		"\u0000\u0000\u0109\u010b\u0003(\u0012\u0000\u010a\u0109\u0001\u0000\u0000"+
		"\u0000\u010a\u010b\u0001\u0000\u0000\u0000\u010b\u0118\u0001\u0000\u0000"+
		"\u0000\u010c\u010e\u0007\u0004\u0000\u0000\u010d\u010c\u0001\u0000\u0000"+
		"\u0000\u010e\u010f\u0001\u0000\u0000\u0000\u010f\u010d\u0001\u0000\u0000"+
		"\u0000\u010f\u0110\u0001\u0000\u0000\u0000\u0110\u0111\u0001\u0000\u0000"+
		"\u0000\u0111\u0118\u0003(\u0012\u0000\u0112\u0114\u0007\u0004\u0000\u0000"+
		"\u0113\u0112\u0001\u0000\u0000\u0000\u0114\u0115\u0001\u0000\u0000\u0000"+
		"\u0115\u0113\u0001\u0000\u0000\u0000\u0115\u0116\u0001\u0000\u0000\u0000"+
		"\u0116\u0118\u0001\u0000\u0000\u0000\u0117\u00ff\u0001\u0000\u0000\u0000"+
		"\u0117\u010d\u0001\u0000\u0000\u0000\u0117\u0113\u0001\u0000\u0000\u0000"+
		"\u0118\'\u0001\u0000\u0000\u0000\u0119\u011b\u0007\u000b\u0000\u0000\u011a"+
		"\u011c\u0007\f\u0000\u0000\u011b\u011a\u0001\u0000\u0000\u0000\u011b\u011c"+
		"\u0001\u0000\u0000\u0000\u011c\u011e\u0001\u0000\u0000\u0000\u011d\u011f"+
		"\u0007\u0004\u0000\u0000\u011e\u011d\u0001\u0000\u0000\u0000\u011f\u0120"+
		"\u0001\u0000\u0000\u0000\u0120\u011e\u0001\u0000\u0000\u0000\u0120\u0121"+
		"\u0001\u0000\u0000\u0000\u0121)\u0001\u0000\u0000\u0000\u0122\u0123\u0005"+
		"t\u0000\u0000\u0123\u0124\u0005r\u0000\u0000\u0124\u0125\u0005u\u0000"+
		"\u0000\u0125\u012c\u0005e\u0000\u0000\u0126\u0127\u0005f\u0000\u0000\u0127"+
		"\u0128\u0005a\u0000\u0000\u0128\u0129\u0005l\u0000\u0000\u0129\u012a\u0005"+
		"s\u0000\u0000\u012a\u012c\u0005e\u0000\u0000\u012b\u0122\u0001\u0000\u0000"+
		"\u0000\u012b\u0126\u0001\u0000\u0000\u0000\u012c+\u0001\u0000\u0000\u0000"+
		"\u012d\u012e\u0005\"\u0000\u0000\u012e\u012f\u0001\u0000\u0000\u0000\u012f"+
		"\u0130\u0006\u0014\u0003\u0000\u0130-\u0001\u0000\u0000\u0000\u0131\u0132"+
		"\u0005n\u0000\u0000\u0132\u0133\u0005u\u0000\u0000\u0133\u0134\u0005l"+
		"\u0000\u0000\u0134\u0135\u0005l\u0000\u0000\u0135/\u0001\u0000\u0000\u0000"+
		"\u0136\u0137\u0005<\u0000\u0000\u0137\u0138\u0005<\u0000\u0000\u0138\u013a"+
		"\u0001\u0000\u0000\u0000\u0139\u013b\u0005-\u0000\u0000\u013a\u0139\u0001"+
		"\u0000\u0000\u0000\u013a\u013b\u0001\u0000\u0000\u0000\u013b\u013c\u0001"+
		"\u0000\u0000\u0000\u013c\u013d\u0006\u0016\u0004\u0000\u013d1\u0001\u0000"+
		"\u0000\u0000\u013e\u013f\u0005+\u0000\u0000\u013f3\u0001\u0000\u0000\u0000"+
		"\u0140\u0141\u0005&\u0000\u0000\u0141\u0142\u0005&\u0000\u0000\u01425"+
		"\u0001\u0000\u0000\u0000\u0143\u0144\u0005=\u0000\u0000\u0144\u0145\u0005"+
		"=\u0000\u0000\u01457\u0001\u0000\u0000\u0000\u0146\u0147\u0005<\u0000"+
		"\u0000\u01479\u0001\u0000\u0000\u0000\u0148\u0149\u0005:\u0000\u0000\u0149"+
		";\u0001\u0000\u0000\u0000\u014a\u014b\u0005[\u0000\u0000\u014b=\u0001"+
		"\u0000\u0000\u0000\u014c\u014d\u0005(\u0000\u0000\u014d?\u0001\u0000\u0000"+
		"\u0000\u014e\u014f\u0005-\u0000\u0000\u014fA\u0001\u0000\u0000\u0000\u0150"+
		"\u0151\u0005|\u0000\u0000\u0151\u0152\u0005|\u0000\u0000\u0152C\u0001"+
		"\u0000\u0000\u0000\u0153\u0154\u0005!\u0000\u0000\u0154\u0155\u0005=\u0000"+
		"\u0000\u0155E\u0001\u0000\u0000\u0000\u0156\u0157\u0005>\u0000\u0000\u0157"+
		"G\u0001\u0000\u0000\u0000\u0158\u0159\u0005?\u0000\u0000\u0159I\u0001"+
		"\u0000\u0000\u0000\u015a\u015b\u0005]\u0000\u0000\u015bK\u0001\u0000\u0000"+
		"\u0000\u015c\u015d\u0005)\u0000\u0000\u015dM\u0001\u0000\u0000\u0000\u015e"+
		"\u015f\u0005*\u0000\u0000\u015fO\u0001\u0000\u0000\u0000\u0160\u0161\u0005"+
		"!\u0000\u0000\u0161Q\u0001\u0000\u0000\u0000\u0162\u0163\u0005<\u0000"+
		"\u0000\u0163\u0164\u0005=\u0000\u0000\u0164S\u0001\u0000\u0000\u0000\u0165"+
		"\u0166\u0005.\u0000\u0000\u0166U\u0001\u0000\u0000\u0000\u0167\u0168\u0005"+
		"/\u0000\u0000\u0168W\u0001\u0000\u0000\u0000\u0169\u016a\u0005>\u0000"+
		"\u0000\u016a\u016b\u0005=\u0000\u0000\u016bY\u0001\u0000\u0000\u0000\u016c"+
		"\u016d\u0005=\u0000\u0000\u016d\u016e\u0005>\u0000\u0000\u016e[\u0001"+
		"\u0000\u0000\u0000\u016f\u0170\u0005,\u0000\u0000\u0170]\u0001\u0000\u0000"+
		"\u0000\u0171\u0172\u0005%\u0000\u0000\u0172_\u0001\u0000\u0000\u0000\u0173"+
		"\u0174\u0005.\u0000\u0000\u0174\u0175\u0005.\u0000\u0000\u0175\u0176\u0005"+
		".\u0000\u0000\u0176a\u0001\u0000\u0000\u0000\u0177\u0178\u0005~\u0000"+
		"\u0000\u0178c\u0001\u0000\u0000\u0000\u0179\u017a\u0005$\u0000\u0000\u017a"+
		"\u017b\u0005{\u0000\u0000\u017b\u017c\u0001\u0000\u0000\u0000\u017c\u017d"+
		"\u00060\u0005\u0000\u017d\u017e\u0001\u0000\u0000\u0000\u017e\u017f\u0006"+
		"0\u0006\u0000\u017fe\u0001\u0000\u0000\u0000\u0180\u0182\u0003h2\u0000"+
		"\u0181\u0180\u0001\u0000\u0000\u0000\u0182\u0183\u0001\u0000\u0000\u0000"+
		"\u0183\u0181\u0001\u0000\u0000\u0000\u0183\u0184\u0001\u0000\u0000\u0000"+
		"\u0184g\u0001\u0000\u0000\u0000\u0185\u0190\b\u0000\u0000\u0000\u0186"+
		"\u0187\u0005$\u0000\u0000\u0187\u0190\u0005$\u0000\u0000\u0188\u0189\u0005"+
		"$\u0000\u0000\u0189\u0190\u00042\u0000\u0000\u018a\u018b\u0005%\u0000"+
		"\u0000\u018b\u0190\u0005%\u0000\u0000\u018c\u018d\u0005%\u0000\u0000\u018d"+
		"\u0190\u00042\u0001\u0000\u018e\u0190\u0003\"\u000f\u0000\u018f\u0185"+
		"\u0001\u0000\u0000\u0000\u018f\u0186\u0001\u0000\u0000\u0000\u018f\u0188"+
		"\u0001\u0000\u0000\u0000\u018f\u018a\u0001\u0000\u0000\u0000\u018f\u018c"+
		"\u0001\u0000\u0000\u0000\u018f\u018e\u0001\u0000\u0000\u0000\u0190i\u0001"+
		"\u0000\u0000\u0000\u0191\u0192\u0005\"\u0000\u0000\u0192\u0193\u0001\u0000"+
		"\u0000\u0000\u0193\u0194\u00063\u0007\u0000\u0194\u0195\u00063\b\u0000"+
		"\u0195k\u0001\u0000\u0000\u0000\u0196\u0197\u0005\n\u0000\u0000\u0197"+
		"\u0198\u0001\u0000\u0000\u0000\u0198\u0199\u00064\t\u0000\u0199\u019a"+
		"\u00064\n\u0000\u019am\u0001\u0000\u0000\u0000\u019b\u01a0\u0003 \u000e"+
		"\u0000\u019c\u019f\u0003\u001e\r\u0000\u019d\u019f\u0005-\u0000\u0000"+
		"\u019e\u019c\u0001\u0000\u0000\u0000\u019e\u019d\u0001\u0000\u0000\u0000"+
		"\u019f\u01a2\u0001\u0000\u0000\u0000\u01a0\u019e\u0001\u0000\u0000\u0000"+
		"\u01a0\u01a1\u0001\u0000\u0000\u0000\u01a1\u01a3\u0001\u0000\u0000\u0000"+
		"\u01a2\u01a0\u0001\u0000\u0000\u0000\u01a3\u01a4\u00065\u000b\u0000\u01a4"+
		"\u01a5\u0001\u0000\u0000\u0000\u01a5\u01a6\u00065\f\u0000\u01a6o\u0001"+
		"\u0000\u0000\u0000\u01a7\u01a8\u0005\n\u0000\u0000\u01a8\u01a9\u0001\u0000"+
		"\u0000\u0000\u01a9\u01aa\u00066\t\u0000\u01aaq\u0001\u0000\u0000\u0000"+
		"\u01ab\u01ac\u0005$\u0000\u0000\u01ac\u01ad\u0005{\u0000\u0000\u01ad\u01ae"+
		"\u0001\u0000\u0000\u0000\u01ae\u01af\u00067\r\u0000\u01af\u01b0\u0001"+
		"\u0000\u0000\u0000\u01b0\u01b1\u00067\u000e\u0000\u01b1\u01b2\u00067\u0006"+
		"\u0000\u01b2s\u0001\u0000\u0000\u0000\u01b3\u01b5\u0003v9\u0000\u01b4"+
		"\u01b3\u0001\u0000\u0000\u0000\u01b5\u01b6\u0001\u0000\u0000\u0000\u01b6"+
		"\u01b4\u0001\u0000\u0000\u0000\u01b6\u01b7\u0001\u0000\u0000\u0000\u01b7"+
		"\u01b8\u0001\u0000\u0000\u0000\u01b8\u01b9\u00068\u000f\u0000\u01b9u\u0001"+
		"\u0000\u0000\u0000\u01ba\u01c1\b\r\u0000\u0000\u01bb\u01bc\u0005$\u0000"+
		"\u0000\u01bc\u01c1\b\u000e\u0000\u0000\u01bd\u01be\u0005%\u0000\u0000"+
		"\u01be\u01c1\b\u000e\u0000\u0000\u01bf\u01c1\u0003\"\u000f\u0000\u01c0"+
		"\u01ba\u0001\u0000\u0000\u0000\u01c0\u01bb\u0001\u0000\u0000\u0000\u01c0"+
		"\u01bd\u0001\u0000\u0000\u0000\u01c0\u01bf\u0001\u0000\u0000\u0000\u01c1"+
		"w\u0001\u0000\u0000\u0000$\u0000\u0001\u0002\u0003}\u007f\u008d\u008f"+
		"\u00a8\u00ad\u00af\u00b5\u00bf\u00ca\u00cf\u00d3\u00d6\u00e0\u00e6\u00fa"+
		"\u0101\u0107\u010a\u010f\u0115\u0117\u011b\u0120\u012b\u013a\u0183\u018f"+
		"\u019e\u01a0\u01b6\u01c0\u0010\u0001\u0004\u0000\u0001\u0005\u0001\u0000"+
		"\u0001\u0000\u0005\u0001\u0000\u0005\u0002\u0000\u00010\u0002\u0005\u0000"+
		"\u0000\u0007\u000f\u0000\u0004\u0000\u0000\u0007\f\u0000\u0002\u0003\u0000"+
		"\u00015\u0003\u0007\b\u0000\u00017\u0004\u0007+\u0000\u00018\u0005";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}