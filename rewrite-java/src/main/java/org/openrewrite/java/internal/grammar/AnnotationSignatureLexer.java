/*
 * Copyright 2020 the original author or authors.
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
// Generated from /Users/jon/Projects/github/openrewrite/rewrite/rewrite-java/src/main/antlr/AnnotationSignatureLexer.g4 by ANTLR 4.8
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
public class AnnotationSignatureLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.8", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		AT=1, DOTDOT=2, POUND=3, SPACE=4, ABSTRACT=5, ASSERT=6, BOOLEAN=7, BREAK=8, 
		BYTE=9, CASE=10, CATCH=11, CHAR=12, CLASS=13, CONST=14, CONTINUE=15, DEFAULT=16, 
		DO=17, DOUBLE=18, ELSE=19, ENUM=20, EXTENDS=21, FINAL=22, FINALLY=23, 
		FLOAT=24, FOR=25, IF=26, GOTO=27, IMPLEMENTS=28, IMPORT=29, INSTANCEOF=30, 
		INT=31, INTERFACE=32, LONG=33, NATIVE=34, NEW=35, PACKAGE=36, PRIVATE=37, 
		PROTECTED=38, PUBLIC=39, RETURN=40, SHORT=41, STATIC=42, STRICTFP=43, 
		SUPER=44, SWITCH=45, SYNCHRONIZED=46, THIS=47, THROW=48, THROWS=49, TRANSIENT=50, 
		TRY=51, VOID=52, VOLATILE=53, WHILE=54, IntegerLiteral=55, FloatingPointLiteral=56, 
		BooleanLiteral=57, CharacterLiteral=58, StringLiteral=59, NullLiteral=60, 
		LPAREN=61, RPAREN=62, LBRACE=63, RBRACE=64, LBRACK=65, RBRACK=66, SEMI=67, 
		COMMA=68, DOT=69, ASSIGN=70, GT=71, LT=72, BANG=73, TILDE=74, QUESTION=75, 
		COLON=76, EQUAL=77, LE=78, GE=79, NOTEQUAL=80, AND=81, OR=82, INC=83, 
		DEC=84, ADD=85, SUB=86, MUL=87, DIV=88, BITAND=89, BITOR=90, CARET=91, 
		MOD=92, ADD_ASSIGN=93, SUB_ASSIGN=94, MUL_ASSIGN=95, DIV_ASSIGN=96, AND_ASSIGN=97, 
		OR_ASSIGN=98, XOR_ASSIGN=99, MOD_ASSIGN=100, LSHIFT_ASSIGN=101, RSHIFT_ASSIGN=102, 
		URSHIFT_ASSIGN=103, Identifier=104, ELLIPSIS=105, WS=106, COMMENT=107, 
		LINE_COMMENT=108;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"AT", "DOTDOT", "POUND", "SPACE", "ABSTRACT", "ASSERT", "BOOLEAN", "BREAK", 
			"BYTE", "CASE", "CATCH", "CHAR", "CLASS", "CONST", "CONTINUE", "DEFAULT", 
			"DO", "DOUBLE", "ELSE", "ENUM", "EXTENDS", "FINAL", "FINALLY", "FLOAT", 
			"FOR", "IF", "GOTO", "IMPLEMENTS", "IMPORT", "INSTANCEOF", "INT", "INTERFACE", 
			"LONG", "NATIVE", "NEW", "PACKAGE", "PRIVATE", "PROTECTED", "PUBLIC", 
			"RETURN", "SHORT", "STATIC", "STRICTFP", "SUPER", "SWITCH", "SYNCHRONIZED", 
			"THIS", "THROW", "THROWS", "TRANSIENT", "TRY", "VOID", "VOLATILE", "WHILE", 
			"IntegerLiteral", "DecimalIntegerLiteral", "HexIntegerLiteral", "OctalIntegerLiteral", 
			"BinaryIntegerLiteral", "IntegerTypeSuffix", "DecimalNumeral", "Digits", 
			"Digit", "NonZeroDigit", "DigitOrUnderscore", "Underscores", "HexNumeral", 
			"HexDigits", "HexDigit", "HexDigitOrUnderscore", "OctalNumeral", "OctalDigits", 
			"OctalDigit", "OctalDigitOrUnderscore", "BinaryNumeral", "BinaryDigits", 
			"BinaryDigit", "BinaryDigitOrUnderscore", "FloatingPointLiteral", "DecimalFloatingPointLiteral", 
			"ExponentPart", "ExponentIndicator", "SignedInteger", "Sign", "FloatTypeSuffix", 
			"HexadecimalFloatingPointLiteral", "HexSignificand", "BinaryExponent", 
			"BinaryExponentIndicator", "BooleanLiteral", "CharacterLiteral", "SingleCharacter", 
			"StringLiteral", "StringCharacters", "StringCharacter", "EscapeSequence", 
			"OctalEscape", "UnicodeEscape", "ZeroToThree", "NullLiteral", "LPAREN", 
			"RPAREN", "LBRACE", "RBRACE", "LBRACK", "RBRACK", "SEMI", "COMMA", "DOT", 
			"ASSIGN", "GT", "LT", "BANG", "TILDE", "QUESTION", "COLON", "EQUAL", 
			"LE", "GE", "NOTEQUAL", "AND", "OR", "INC", "DEC", "ADD", "SUB", "MUL", 
			"DIV", "BITAND", "BITOR", "CARET", "MOD", "ADD_ASSIGN", "SUB_ASSIGN", 
			"MUL_ASSIGN", "DIV_ASSIGN", "AND_ASSIGN", "OR_ASSIGN", "XOR_ASSIGN", 
			"MOD_ASSIGN", "LSHIFT_ASSIGN", "RSHIFT_ASSIGN", "URSHIFT_ASSIGN", "Identifier", 
			"JavaLetter", "JavaLetterOrDigit", "ELLIPSIS", "WS", "COMMENT", "LINE_COMMENT"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'@'", "'..'", "'#'", "' '", "'abstract'", "'assert'", "'boolean'", 
			"'break'", "'byte'", "'case'", "'catch'", "'char'", "'class'", "'const'", 
			"'continue'", "'default'", "'do'", "'double'", "'else'", "'enum'", "'extends'", 
			"'final'", "'finally'", "'float'", "'for'", "'if'", "'goto'", "'implements'", 
			"'import'", "'instanceof'", "'int'", "'interface'", "'long'", "'native'", 
			"'new'", "'package'", "'private'", "'protected'", "'public'", "'return'", 
			"'short'", "'static'", "'strictfp'", "'super'", "'switch'", "'synchronized'", 
			"'this'", "'throw'", "'throws'", "'transient'", "'try'", "'void'", "'volatile'", 
			"'while'", null, null, null, null, null, "'null'", "'('", "')'", "'{'", 
			"'}'", "'['", "']'", "';'", "','", "'.'", "'='", "'>'", "'<'", "'!'", 
			"'~'", "'?'", "':'", "'=='", "'<='", "'>='", "'!='", "'&&'", "'||'", 
			"'++'", "'--'", "'+'", "'-'", "'*'", "'/'", "'&'", "'|'", "'^'", "'%'", 
			"'+='", "'-='", "'*='", "'/='", "'&='", "'|='", "'^='", "'%='", "'<<='", 
			"'>>='", "'>>>='", null, "'...'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "AT", "DOTDOT", "POUND", "SPACE", "ABSTRACT", "ASSERT", "BOOLEAN", 
			"BREAK", "BYTE", "CASE", "CATCH", "CHAR", "CLASS", "CONST", "CONTINUE", 
			"DEFAULT", "DO", "DOUBLE", "ELSE", "ENUM", "EXTENDS", "FINAL", "FINALLY", 
			"FLOAT", "FOR", "IF", "GOTO", "IMPLEMENTS", "IMPORT", "INSTANCEOF", "INT", 
			"INTERFACE", "LONG", "NATIVE", "NEW", "PACKAGE", "PRIVATE", "PROTECTED", 
			"PUBLIC", "RETURN", "SHORT", "STATIC", "STRICTFP", "SUPER", "SWITCH", 
			"SYNCHRONIZED", "THIS", "THROW", "THROWS", "TRANSIENT", "TRY", "VOID", 
			"VOLATILE", "WHILE", "IntegerLiteral", "FloatingPointLiteral", "BooleanLiteral", 
			"CharacterLiteral", "StringLiteral", "NullLiteral", "LPAREN", "RPAREN", 
			"LBRACE", "RBRACE", "LBRACK", "RBRACK", "SEMI", "COMMA", "DOT", "ASSIGN", 
			"GT", "LT", "BANG", "TILDE", "QUESTION", "COLON", "EQUAL", "LE", "GE", 
			"NOTEQUAL", "AND", "OR", "INC", "DEC", "ADD", "SUB", "MUL", "DIV", "BITAND", 
			"BITOR", "CARET", "MOD", "ADD_ASSIGN", "SUB_ASSIGN", "MUL_ASSIGN", "DIV_ASSIGN", 
			"AND_ASSIGN", "OR_ASSIGN", "XOR_ASSIGN", "MOD_ASSIGN", "LSHIFT_ASSIGN", 
			"RSHIFT_ASSIGN", "URSHIFT_ASSIGN", "Identifier", "ELLIPSIS", "WS", "COMMENT", 
			"LINE_COMMENT"
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


	public AnnotationSignatureLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "AnnotationSignatureLexer.g4"; }

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
		case 144:
			return JavaLetter_sempred((RuleContext)_localctx, predIndex);
		case 145:
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2n\u043b\b\1\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t+\4"+
		",\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64\t"+
		"\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\4<\t<\4=\t="+
		"\4>\t>\4?\t?\4@\t@\4A\tA\4B\tB\4C\tC\4D\tD\4E\tE\4F\tF\4G\tG\4H\tH\4I"+
		"\tI\4J\tJ\4K\tK\4L\tL\4M\tM\4N\tN\4O\tO\4P\tP\4Q\tQ\4R\tR\4S\tS\4T\tT"+
		"\4U\tU\4V\tV\4W\tW\4X\tX\4Y\tY\4Z\tZ\4[\t[\4\\\t\\\4]\t]\4^\t^\4_\t_\4"+
		"`\t`\4a\ta\4b\tb\4c\tc\4d\td\4e\te\4f\tf\4g\tg\4h\th\4i\ti\4j\tj\4k\t"+
		"k\4l\tl\4m\tm\4n\tn\4o\to\4p\tp\4q\tq\4r\tr\4s\ts\4t\tt\4u\tu\4v\tv\4"+
		"w\tw\4x\tx\4y\ty\4z\tz\4{\t{\4|\t|\4}\t}\4~\t~\4\177\t\177\4\u0080\t\u0080"+
		"\4\u0081\t\u0081\4\u0082\t\u0082\4\u0083\t\u0083\4\u0084\t\u0084\4\u0085"+
		"\t\u0085\4\u0086\t\u0086\4\u0087\t\u0087\4\u0088\t\u0088\4\u0089\t\u0089"+
		"\4\u008a\t\u008a\4\u008b\t\u008b\4\u008c\t\u008c\4\u008d\t\u008d\4\u008e"+
		"\t\u008e\4\u008f\t\u008f\4\u0090\t\u0090\4\u0091\t\u0091\4\u0092\t\u0092"+
		"\4\u0093\t\u0093\4\u0094\t\u0094\4\u0095\t\u0095\4\u0096\t\u0096\4\u0097"+
		"\t\u0097\3\2\3\2\3\3\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\6\3\6\3\6\3\6\3"+
		"\6\3\6\3\6\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b"+
		"\3\t\3\t\3\t\3\t\3\t\3\t\3\n\3\n\3\n\3\n\3\n\3\13\3\13\3\13\3\13\3\13"+
		"\3\f\3\f\3\f\3\f\3\f\3\f\3\r\3\r\3\r\3\r\3\r\3\16\3\16\3\16\3\16\3\16"+
		"\3\16\3\17\3\17\3\17\3\17\3\17\3\17\3\20\3\20\3\20\3\20\3\20\3\20\3\20"+
		"\3\20\3\20\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\22\3\22\3\22\3\23"+
		"\3\23\3\23\3\23\3\23\3\23\3\23\3\24\3\24\3\24\3\24\3\24\3\25\3\25\3\25"+
		"\3\25\3\25\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\27\3\27\3\27\3\27"+
		"\3\27\3\27\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\31\3\31\3\31\3\31"+
		"\3\31\3\31\3\32\3\32\3\32\3\32\3\33\3\33\3\33\3\34\3\34\3\34\3\34\3\34"+
		"\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\36\3\36\3\36"+
		"\3\36\3\36\3\36\3\36\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37"+
		"\3\37\3 \3 \3 \3 \3!\3!\3!\3!\3!\3!\3!\3!\3!\3!\3\"\3\"\3\"\3\"\3\"\3"+
		"#\3#\3#\3#\3#\3#\3#\3$\3$\3$\3$\3%\3%\3%\3%\3%\3%\3%\3%\3&\3&\3&\3&\3"+
		"&\3&\3&\3&\3\'\3\'\3\'\3\'\3\'\3\'\3\'\3\'\3\'\3\'\3(\3(\3(\3(\3(\3(\3"+
		"(\3)\3)\3)\3)\3)\3)\3)\3*\3*\3*\3*\3*\3*\3+\3+\3+\3+\3+\3+\3+\3,\3,\3"+
		",\3,\3,\3,\3,\3,\3,\3-\3-\3-\3-\3-\3-\3.\3.\3.\3.\3.\3.\3.\3/\3/\3/\3"+
		"/\3/\3/\3/\3/\3/\3/\3/\3/\3/\3\60\3\60\3\60\3\60\3\60\3\61\3\61\3\61\3"+
		"\61\3\61\3\61\3\62\3\62\3\62\3\62\3\62\3\62\3\62\3\63\3\63\3\63\3\63\3"+
		"\63\3\63\3\63\3\63\3\63\3\63\3\64\3\64\3\64\3\64\3\65\3\65\3\65\3\65\3"+
		"\65\3\66\3\66\3\66\3\66\3\66\3\66\3\66\3\66\3\66\3\67\3\67\3\67\3\67\3"+
		"\67\3\67\38\38\38\38\58\u0290\n8\39\39\59\u0294\n9\3:\3:\5:\u0298\n:\3"+
		";\3;\5;\u029c\n;\3<\3<\5<\u02a0\n<\3=\3=\3>\3>\3>\5>\u02a7\n>\3>\3>\3"+
		">\5>\u02ac\n>\5>\u02ae\n>\3?\3?\7?\u02b2\n?\f?\16?\u02b5\13?\3?\5?\u02b8"+
		"\n?\3@\3@\5@\u02bc\n@\3A\3A\3B\3B\5B\u02c2\nB\3C\6C\u02c5\nC\rC\16C\u02c6"+
		"\3D\3D\3D\3D\3E\3E\7E\u02cf\nE\fE\16E\u02d2\13E\3E\5E\u02d5\nE\3F\3F\3"+
		"G\3G\5G\u02db\nG\3H\3H\5H\u02df\nH\3H\3H\3I\3I\7I\u02e5\nI\fI\16I\u02e8"+
		"\13I\3I\5I\u02eb\nI\3J\3J\3K\3K\5K\u02f1\nK\3L\3L\3L\3L\3M\3M\7M\u02f9"+
		"\nM\fM\16M\u02fc\13M\3M\5M\u02ff\nM\3N\3N\3O\3O\5O\u0305\nO\3P\3P\5P\u0309"+
		"\nP\3Q\3Q\3Q\5Q\u030e\nQ\3Q\5Q\u0311\nQ\3Q\5Q\u0314\nQ\3Q\3Q\3Q\5Q\u0319"+
		"\nQ\3Q\5Q\u031c\nQ\3Q\3Q\3Q\5Q\u0321\nQ\3Q\3Q\3Q\5Q\u0326\nQ\3R\3R\3R"+
		"\3S\3S\3T\5T\u032e\nT\3T\3T\3U\3U\3V\3V\3W\3W\3W\5W\u0339\nW\3X\3X\5X"+
		"\u033d\nX\3X\3X\3X\5X\u0342\nX\3X\3X\5X\u0346\nX\3Y\3Y\3Y\3Z\3Z\3[\3["+
		"\3[\3[\3[\3[\3[\3[\3[\5[\u0356\n[\3\\\3\\\3\\\3\\\3\\\3\\\3\\\3\\\5\\"+
		"\u0360\n\\\3]\3]\3^\3^\5^\u0366\n^\3^\3^\3_\6_\u036b\n_\r_\16_\u036c\3"+
		"`\3`\5`\u0371\n`\3a\3a\3a\3a\5a\u0377\na\3b\3b\3b\3b\3b\3b\3b\3b\3b\3"+
		"b\3b\5b\u0384\nb\3c\3c\3c\3c\3c\3c\3c\3d\3d\3e\3e\3e\3e\3e\3f\3f\3g\3"+
		"g\3h\3h\3i\3i\3j\3j\3k\3k\3l\3l\3m\3m\3n\3n\3o\3o\3p\3p\3q\3q\3r\3r\3"+
		"s\3s\3t\3t\3u\3u\3v\3v\3v\3w\3w\3w\3x\3x\3x\3y\3y\3y\3z\3z\3z\3{\3{\3"+
		"{\3|\3|\3|\3}\3}\3}\3~\3~\3\177\3\177\3\u0080\3\u0080\3\u0081\3\u0081"+
		"\3\u0082\3\u0082\3\u0083\3\u0083\3\u0084\3\u0084\3\u0085\3\u0085\3\u0086"+
		"\3\u0086\3\u0086\3\u0087\3\u0087\3\u0087\3\u0088\3\u0088\3\u0088\3\u0089"+
		"\3\u0089\3\u0089\3\u008a\3\u008a\3\u008a\3\u008b\3\u008b\3\u008b\3\u008c"+
		"\3\u008c\3\u008c\3\u008d\3\u008d\3\u008d\3\u008e\3\u008e\3\u008e\3\u008e"+
		"\3\u008f\3\u008f\3\u008f\3\u008f\3\u0090\3\u0090\3\u0090\3\u0090\3\u0090"+
		"\3\u0091\3\u0091\7\u0091\u0403\n\u0091\f\u0091\16\u0091\u0406\13\u0091"+
		"\3\u0092\3\u0092\3\u0092\3\u0092\3\u0092\3\u0092\5\u0092\u040e\n\u0092"+
		"\3\u0093\3\u0093\3\u0093\3\u0093\3\u0093\3\u0093\5\u0093\u0416\n\u0093"+
		"\3\u0094\3\u0094\3\u0094\3\u0094\3\u0095\6\u0095\u041d\n\u0095\r\u0095"+
		"\16\u0095\u041e\3\u0095\3\u0095\3\u0096\3\u0096\3\u0096\3\u0096\7\u0096"+
		"\u0427\n\u0096\f\u0096\16\u0096\u042a\13\u0096\3\u0096\3\u0096\3\u0096"+
		"\3\u0096\3\u0096\3\u0097\3\u0097\3\u0097\3\u0097\7\u0097\u0435\n\u0097"+
		"\f\u0097\16\u0097\u0438\13\u0097\3\u0097\3\u0097\3\u0428\2\u0098\3\3\5"+
		"\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21"+
		"!\22#\23%\24\'\25)\26+\27-\30/\31\61\32\63\33\65\34\67\359\36;\37= ?!"+
		"A\"C#E$G%I&K\'M(O)Q*S+U,W-Y.[/]\60_\61a\62c\63e\64g\65i\66k\67m8o9q\2"+
		"s\2u\2w\2y\2{\2}\2\177\2\u0081\2\u0083\2\u0085\2\u0087\2\u0089\2\u008b"+
		"\2\u008d\2\u008f\2\u0091\2\u0093\2\u0095\2\u0097\2\u0099\2\u009b\2\u009d"+
		"\2\u009f:\u00a1\2\u00a3\2\u00a5\2\u00a7\2\u00a9\2\u00ab\2\u00ad\2\u00af"+
		"\2\u00b1\2\u00b3\2\u00b5;\u00b7<\u00b9\2\u00bb=\u00bd\2\u00bf\2\u00c1"+
		"\2\u00c3\2\u00c5\2\u00c7\2\u00c9>\u00cb?\u00cd@\u00cfA\u00d1B\u00d3C\u00d5"+
		"D\u00d7E\u00d9F\u00dbG\u00ddH\u00dfI\u00e1J\u00e3K\u00e5L\u00e7M\u00e9"+
		"N\u00ebO\u00edP\u00efQ\u00f1R\u00f3S\u00f5T\u00f7U\u00f9V\u00fbW\u00fd"+
		"X\u00ffY\u0101Z\u0103[\u0105\\\u0107]\u0109^\u010b_\u010d`\u010fa\u0111"+
		"b\u0113c\u0115d\u0117e\u0119f\u011bg\u011dh\u011fi\u0121j\u0123\2\u0125"+
		"\2\u0127k\u0129l\u012bm\u012dn\3\2\30\4\2NNnn\3\2\63;\4\2ZZzz\5\2\62;"+
		"CHch\3\2\629\4\2DDdd\3\2\62\63\4\2GGgg\4\2--//\6\2FFHHffhh\4\2RRrr\4\2"+
		"))^^\4\2$$^^\n\2$$))^^ddhhppttvv\3\2\62\65\6\2&&C\\aac|\4\2\2\u0081\ud802"+
		"\udc01\3\2\ud802\udc01\3\2\udc02\ue001\7\2&&\62;C\\aac|\5\2\13\f\16\17"+
		"\"\"\4\2\f\f\17\17\2\u0449\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2"+
		"\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2"+
		"\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3"+
		"\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2"+
		"\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\2\63\3\2\2\2\2\65\3\2\2\2\2\67"+
		"\3\2\2\2\29\3\2\2\2\2;\3\2\2\2\2=\3\2\2\2\2?\3\2\2\2\2A\3\2\2\2\2C\3\2"+
		"\2\2\2E\3\2\2\2\2G\3\2\2\2\2I\3\2\2\2\2K\3\2\2\2\2M\3\2\2\2\2O\3\2\2\2"+
		"\2Q\3\2\2\2\2S\3\2\2\2\2U\3\2\2\2\2W\3\2\2\2\2Y\3\2\2\2\2[\3\2\2\2\2]"+
		"\3\2\2\2\2_\3\2\2\2\2a\3\2\2\2\2c\3\2\2\2\2e\3\2\2\2\2g\3\2\2\2\2i\3\2"+
		"\2\2\2k\3\2\2\2\2m\3\2\2\2\2o\3\2\2\2\2\u009f\3\2\2\2\2\u00b5\3\2\2\2"+
		"\2\u00b7\3\2\2\2\2\u00bb\3\2\2\2\2\u00c9\3\2\2\2\2\u00cb\3\2\2\2\2\u00cd"+
		"\3\2\2\2\2\u00cf\3\2\2\2\2\u00d1\3\2\2\2\2\u00d3\3\2\2\2\2\u00d5\3\2\2"+
		"\2\2\u00d7\3\2\2\2\2\u00d9\3\2\2\2\2\u00db\3\2\2\2\2\u00dd\3\2\2\2\2\u00df"+
		"\3\2\2\2\2\u00e1\3\2\2\2\2\u00e3\3\2\2\2\2\u00e5\3\2\2\2\2\u00e7\3\2\2"+
		"\2\2\u00e9\3\2\2\2\2\u00eb\3\2\2\2\2\u00ed\3\2\2\2\2\u00ef\3\2\2\2\2\u00f1"+
		"\3\2\2\2\2\u00f3\3\2\2\2\2\u00f5\3\2\2\2\2\u00f7\3\2\2\2\2\u00f9\3\2\2"+
		"\2\2\u00fb\3\2\2\2\2\u00fd\3\2\2\2\2\u00ff\3\2\2\2\2\u0101\3\2\2\2\2\u0103"+
		"\3\2\2\2\2\u0105\3\2\2\2\2\u0107\3\2\2\2\2\u0109\3\2\2\2\2\u010b\3\2\2"+
		"\2\2\u010d\3\2\2\2\2\u010f\3\2\2\2\2\u0111\3\2\2\2\2\u0113\3\2\2\2\2\u0115"+
		"\3\2\2\2\2\u0117\3\2\2\2\2\u0119\3\2\2\2\2\u011b\3\2\2\2\2\u011d\3\2\2"+
		"\2\2\u011f\3\2\2\2\2\u0121\3\2\2\2\2\u0127\3\2\2\2\2\u0129\3\2\2\2\2\u012b"+
		"\3\2\2\2\2\u012d\3\2\2\2\3\u012f\3\2\2\2\5\u0131\3\2\2\2\7\u0134\3\2\2"+
		"\2\t\u0136\3\2\2\2\13\u0138\3\2\2\2\r\u0141\3\2\2\2\17\u0148\3\2\2\2\21"+
		"\u0150\3\2\2\2\23\u0156\3\2\2\2\25\u015b\3\2\2\2\27\u0160\3\2\2\2\31\u0166"+
		"\3\2\2\2\33\u016b\3\2\2\2\35\u0171\3\2\2\2\37\u0177\3\2\2\2!\u0180\3\2"+
		"\2\2#\u0188\3\2\2\2%\u018b\3\2\2\2\'\u0192\3\2\2\2)\u0197\3\2\2\2+\u019c"+
		"\3\2\2\2-\u01a4\3\2\2\2/\u01aa\3\2\2\2\61\u01b2\3\2\2\2\63\u01b8\3\2\2"+
		"\2\65\u01bc\3\2\2\2\67\u01bf\3\2\2\29\u01c4\3\2\2\2;\u01cf\3\2\2\2=\u01d6"+
		"\3\2\2\2?\u01e1\3\2\2\2A\u01e5\3\2\2\2C\u01ef\3\2\2\2E\u01f4\3\2\2\2G"+
		"\u01fb\3\2\2\2I\u01ff\3\2\2\2K\u0207\3\2\2\2M\u020f\3\2\2\2O\u0219\3\2"+
		"\2\2Q\u0220\3\2\2\2S\u0227\3\2\2\2U\u022d\3\2\2\2W\u0234\3\2\2\2Y\u023d"+
		"\3\2\2\2[\u0243\3\2\2\2]\u024a\3\2\2\2_\u0257\3\2\2\2a\u025c\3\2\2\2c"+
		"\u0262\3\2\2\2e\u0269\3\2\2\2g\u0273\3\2\2\2i\u0277\3\2\2\2k\u027c\3\2"+
		"\2\2m\u0285\3\2\2\2o\u028f\3\2\2\2q\u0291\3\2\2\2s\u0295\3\2\2\2u\u0299"+
		"\3\2\2\2w\u029d\3\2\2\2y\u02a1\3\2\2\2{\u02ad\3\2\2\2}\u02af\3\2\2\2\177"+
		"\u02bb\3\2\2\2\u0081\u02bd\3\2\2\2\u0083\u02c1\3\2\2\2\u0085\u02c4\3\2"+
		"\2\2\u0087\u02c8\3\2\2\2\u0089\u02cc\3\2\2\2\u008b\u02d6\3\2\2\2\u008d"+
		"\u02da\3\2\2\2\u008f\u02dc\3\2\2\2\u0091\u02e2\3\2\2\2\u0093\u02ec\3\2"+
		"\2\2\u0095\u02f0\3\2\2\2\u0097\u02f2\3\2\2\2\u0099\u02f6\3\2\2\2\u009b"+
		"\u0300\3\2\2\2\u009d\u0304\3\2\2\2\u009f\u0308\3\2\2\2\u00a1\u0325\3\2"+
		"\2\2\u00a3\u0327\3\2\2\2\u00a5\u032a\3\2\2\2\u00a7\u032d\3\2\2\2\u00a9"+
		"\u0331\3\2\2\2\u00ab\u0333\3\2\2\2\u00ad\u0335\3\2\2\2\u00af\u0345\3\2"+
		"\2\2\u00b1\u0347\3\2\2\2\u00b3\u034a\3\2\2\2\u00b5\u0355\3\2\2\2\u00b7"+
		"\u035f\3\2\2\2\u00b9\u0361\3\2\2\2\u00bb\u0363\3\2\2\2\u00bd\u036a\3\2"+
		"\2\2\u00bf\u0370\3\2\2\2\u00c1\u0376\3\2\2\2\u00c3\u0383\3\2\2\2\u00c5"+
		"\u0385\3\2\2\2\u00c7\u038c\3\2\2\2\u00c9\u038e\3\2\2\2\u00cb\u0393\3\2"+
		"\2\2\u00cd\u0395\3\2\2\2\u00cf\u0397\3\2\2\2\u00d1\u0399\3\2\2\2\u00d3"+
		"\u039b\3\2\2\2\u00d5\u039d\3\2\2\2\u00d7\u039f\3\2\2\2\u00d9\u03a1\3\2"+
		"\2\2\u00db\u03a3\3\2\2\2\u00dd\u03a5\3\2\2\2\u00df\u03a7\3\2\2\2\u00e1"+
		"\u03a9\3\2\2\2\u00e3\u03ab\3\2\2\2\u00e5\u03ad\3\2\2\2\u00e7\u03af\3\2"+
		"\2\2\u00e9\u03b1\3\2\2\2\u00eb\u03b3\3\2\2\2\u00ed\u03b6\3\2\2\2\u00ef"+
		"\u03b9\3\2\2\2\u00f1\u03bc\3\2\2\2\u00f3\u03bf\3\2\2\2\u00f5\u03c2\3\2"+
		"\2\2\u00f7\u03c5\3\2\2\2\u00f9\u03c8\3\2\2\2\u00fb\u03cb\3\2\2\2\u00fd"+
		"\u03cd\3\2\2\2\u00ff\u03cf\3\2\2\2\u0101\u03d1\3\2\2\2\u0103\u03d3\3\2"+
		"\2\2\u0105\u03d5\3\2\2\2\u0107\u03d7\3\2\2\2\u0109\u03d9\3\2\2\2\u010b"+
		"\u03db\3\2\2\2\u010d\u03de\3\2\2\2\u010f\u03e1\3\2\2\2\u0111\u03e4\3\2"+
		"\2\2\u0113\u03e7\3\2\2\2\u0115\u03ea\3\2\2\2\u0117\u03ed\3\2\2\2\u0119"+
		"\u03f0\3\2\2\2\u011b\u03f3\3\2\2\2\u011d\u03f7\3\2\2\2\u011f\u03fb\3\2"+
		"\2\2\u0121\u0400\3\2\2\2\u0123\u040d\3\2\2\2\u0125\u0415\3\2\2\2\u0127"+
		"\u0417\3\2\2\2\u0129\u041c\3\2\2\2\u012b\u0422\3\2\2\2\u012d\u0430\3\2"+
		"\2\2\u012f\u0130\7B\2\2\u0130\4\3\2\2\2\u0131\u0132\7\60\2\2\u0132\u0133"+
		"\7\60\2\2\u0133\6\3\2\2\2\u0134\u0135\7%\2\2\u0135\b\3\2\2\2\u0136\u0137"+
		"\7\"\2\2\u0137\n\3\2\2\2\u0138\u0139\7c\2\2\u0139\u013a\7d\2\2\u013a\u013b"+
		"\7u\2\2\u013b\u013c\7v\2\2\u013c\u013d\7t\2\2\u013d\u013e\7c\2\2\u013e"+
		"\u013f\7e\2\2\u013f\u0140\7v\2\2\u0140\f\3\2\2\2\u0141\u0142\7c\2\2\u0142"+
		"\u0143\7u\2\2\u0143\u0144\7u\2\2\u0144\u0145\7g\2\2\u0145\u0146\7t\2\2"+
		"\u0146\u0147\7v\2\2\u0147\16\3\2\2\2\u0148\u0149\7d\2\2\u0149\u014a\7"+
		"q\2\2\u014a\u014b\7q\2\2\u014b\u014c\7n\2\2\u014c\u014d\7g\2\2\u014d\u014e"+
		"\7c\2\2\u014e\u014f\7p\2\2\u014f\20\3\2\2\2\u0150\u0151\7d\2\2\u0151\u0152"+
		"\7t\2\2\u0152\u0153\7g\2\2\u0153\u0154\7c\2\2\u0154\u0155\7m\2\2\u0155"+
		"\22\3\2\2\2\u0156\u0157\7d\2\2\u0157\u0158\7{\2\2\u0158\u0159\7v\2\2\u0159"+
		"\u015a\7g\2\2\u015a\24\3\2\2\2\u015b\u015c\7e\2\2\u015c\u015d\7c\2\2\u015d"+
		"\u015e\7u\2\2\u015e\u015f\7g\2\2\u015f\26\3\2\2\2\u0160\u0161\7e\2\2\u0161"+
		"\u0162\7c\2\2\u0162\u0163\7v\2\2\u0163\u0164\7e\2\2\u0164\u0165\7j\2\2"+
		"\u0165\30\3\2\2\2\u0166\u0167\7e\2\2\u0167\u0168\7j\2\2\u0168\u0169\7"+
		"c\2\2\u0169\u016a\7t\2\2\u016a\32\3\2\2\2\u016b\u016c\7e\2\2\u016c\u016d"+
		"\7n\2\2\u016d\u016e\7c\2\2\u016e\u016f\7u\2\2\u016f\u0170\7u\2\2\u0170"+
		"\34\3\2\2\2\u0171\u0172\7e\2\2\u0172\u0173\7q\2\2\u0173\u0174\7p\2\2\u0174"+
		"\u0175\7u\2\2\u0175\u0176\7v\2\2\u0176\36\3\2\2\2\u0177\u0178\7e\2\2\u0178"+
		"\u0179\7q\2\2\u0179\u017a\7p\2\2\u017a\u017b\7v\2\2\u017b\u017c\7k\2\2"+
		"\u017c\u017d\7p\2\2\u017d\u017e\7w\2\2\u017e\u017f\7g\2\2\u017f \3\2\2"+
		"\2\u0180\u0181\7f\2\2\u0181\u0182\7g\2\2\u0182\u0183\7h\2\2\u0183\u0184"+
		"\7c\2\2\u0184\u0185\7w\2\2\u0185\u0186\7n\2\2\u0186\u0187\7v\2\2\u0187"+
		"\"\3\2\2\2\u0188\u0189\7f\2\2\u0189\u018a\7q\2\2\u018a$\3\2\2\2\u018b"+
		"\u018c\7f\2\2\u018c\u018d\7q\2\2\u018d\u018e\7w\2\2\u018e\u018f\7d\2\2"+
		"\u018f\u0190\7n\2\2\u0190\u0191\7g\2\2\u0191&\3\2\2\2\u0192\u0193\7g\2"+
		"\2\u0193\u0194\7n\2\2\u0194\u0195\7u\2\2\u0195\u0196\7g\2\2\u0196(\3\2"+
		"\2\2\u0197\u0198\7g\2\2\u0198\u0199\7p\2\2\u0199\u019a\7w\2\2\u019a\u019b"+
		"\7o\2\2\u019b*\3\2\2\2\u019c\u019d\7g\2\2\u019d\u019e\7z\2\2\u019e\u019f"+
		"\7v\2\2\u019f\u01a0\7g\2\2\u01a0\u01a1\7p\2\2\u01a1\u01a2\7f\2\2\u01a2"+
		"\u01a3\7u\2\2\u01a3,\3\2\2\2\u01a4\u01a5\7h\2\2\u01a5\u01a6\7k\2\2\u01a6"+
		"\u01a7\7p\2\2\u01a7\u01a8\7c\2\2\u01a8\u01a9\7n\2\2\u01a9.\3\2\2\2\u01aa"+
		"\u01ab\7h\2\2\u01ab\u01ac\7k\2\2\u01ac\u01ad\7p\2\2\u01ad\u01ae\7c\2\2"+
		"\u01ae\u01af\7n\2\2\u01af\u01b0\7n\2\2\u01b0\u01b1\7{\2\2\u01b1\60\3\2"+
		"\2\2\u01b2\u01b3\7h\2\2\u01b3\u01b4\7n\2\2\u01b4\u01b5\7q\2\2\u01b5\u01b6"+
		"\7c\2\2\u01b6\u01b7\7v\2\2\u01b7\62\3\2\2\2\u01b8\u01b9\7h\2\2\u01b9\u01ba"+
		"\7q\2\2\u01ba\u01bb\7t\2\2\u01bb\64\3\2\2\2\u01bc\u01bd\7k\2\2\u01bd\u01be"+
		"\7h\2\2\u01be\66\3\2\2\2\u01bf\u01c0\7i\2\2\u01c0\u01c1\7q\2\2\u01c1\u01c2"+
		"\7v\2\2\u01c2\u01c3\7q\2\2\u01c38\3\2\2\2\u01c4\u01c5\7k\2\2\u01c5\u01c6"+
		"\7o\2\2\u01c6\u01c7\7r\2\2\u01c7\u01c8\7n\2\2\u01c8\u01c9\7g\2\2\u01c9"+
		"\u01ca\7o\2\2\u01ca\u01cb\7g\2\2\u01cb\u01cc\7p\2\2\u01cc\u01cd\7v\2\2"+
		"\u01cd\u01ce\7u\2\2\u01ce:\3\2\2\2\u01cf\u01d0\7k\2\2\u01d0\u01d1\7o\2"+
		"\2\u01d1\u01d2\7r\2\2\u01d2\u01d3\7q\2\2\u01d3\u01d4\7t\2\2\u01d4\u01d5"+
		"\7v\2\2\u01d5<\3\2\2\2\u01d6\u01d7\7k\2\2\u01d7\u01d8\7p\2\2\u01d8\u01d9"+
		"\7u\2\2\u01d9\u01da\7v\2\2\u01da\u01db\7c\2\2\u01db\u01dc\7p\2\2\u01dc"+
		"\u01dd\7e\2\2\u01dd\u01de\7g\2\2\u01de\u01df\7q\2\2\u01df\u01e0\7h\2\2"+
		"\u01e0>\3\2\2\2\u01e1\u01e2\7k\2\2\u01e2\u01e3\7p\2\2\u01e3\u01e4\7v\2"+
		"\2\u01e4@\3\2\2\2\u01e5\u01e6\7k\2\2\u01e6\u01e7\7p\2\2\u01e7\u01e8\7"+
		"v\2\2\u01e8\u01e9\7g\2\2\u01e9\u01ea\7t\2\2\u01ea\u01eb\7h\2\2\u01eb\u01ec"+
		"\7c\2\2\u01ec\u01ed\7e\2\2\u01ed\u01ee\7g\2\2\u01eeB\3\2\2\2\u01ef\u01f0"+
		"\7n\2\2\u01f0\u01f1\7q\2\2\u01f1\u01f2\7p\2\2\u01f2\u01f3\7i\2\2\u01f3"+
		"D\3\2\2\2\u01f4\u01f5\7p\2\2\u01f5\u01f6\7c\2\2\u01f6\u01f7\7v\2\2\u01f7"+
		"\u01f8\7k\2\2\u01f8\u01f9\7x\2\2\u01f9\u01fa\7g\2\2\u01faF\3\2\2\2\u01fb"+
		"\u01fc\7p\2\2\u01fc\u01fd\7g\2\2\u01fd\u01fe\7y\2\2\u01feH\3\2\2\2\u01ff"+
		"\u0200\7r\2\2\u0200\u0201\7c\2\2\u0201\u0202\7e\2\2\u0202\u0203\7m\2\2"+
		"\u0203\u0204\7c\2\2\u0204\u0205\7i\2\2\u0205\u0206\7g\2\2\u0206J\3\2\2"+
		"\2\u0207\u0208\7r\2\2\u0208\u0209\7t\2\2\u0209\u020a\7k\2\2\u020a\u020b"+
		"\7x\2\2\u020b\u020c\7c\2\2\u020c\u020d\7v\2\2\u020d\u020e\7g\2\2\u020e"+
		"L\3\2\2\2\u020f\u0210\7r\2\2\u0210\u0211\7t\2\2\u0211\u0212\7q\2\2\u0212"+
		"\u0213\7v\2\2\u0213\u0214\7g\2\2\u0214\u0215\7e\2\2\u0215\u0216\7v\2\2"+
		"\u0216\u0217\7g\2\2\u0217\u0218\7f\2\2\u0218N\3\2\2\2\u0219\u021a\7r\2"+
		"\2\u021a\u021b\7w\2\2\u021b\u021c\7d\2\2\u021c\u021d\7n\2\2\u021d\u021e"+
		"\7k\2\2\u021e\u021f\7e\2\2\u021fP\3\2\2\2\u0220\u0221\7t\2\2\u0221\u0222"+
		"\7g\2\2\u0222\u0223\7v\2\2\u0223\u0224\7w\2\2\u0224\u0225\7t\2\2\u0225"+
		"\u0226\7p\2\2\u0226R\3\2\2\2\u0227\u0228\7u\2\2\u0228\u0229\7j\2\2\u0229"+
		"\u022a\7q\2\2\u022a\u022b\7t\2\2\u022b\u022c\7v\2\2\u022cT\3\2\2\2\u022d"+
		"\u022e\7u\2\2\u022e\u022f\7v\2\2\u022f\u0230\7c\2\2\u0230\u0231\7v\2\2"+
		"\u0231\u0232\7k\2\2\u0232\u0233\7e\2\2\u0233V\3\2\2\2\u0234\u0235\7u\2"+
		"\2\u0235\u0236\7v\2\2\u0236\u0237\7t\2\2\u0237\u0238\7k\2\2\u0238\u0239"+
		"\7e\2\2\u0239\u023a\7v\2\2\u023a\u023b\7h\2\2\u023b\u023c\7r\2\2\u023c"+
		"X\3\2\2\2\u023d\u023e\7u\2\2\u023e\u023f\7w\2\2\u023f\u0240\7r\2\2\u0240"+
		"\u0241\7g\2\2\u0241\u0242\7t\2\2\u0242Z\3\2\2\2\u0243\u0244\7u\2\2\u0244"+
		"\u0245\7y\2\2\u0245\u0246\7k\2\2\u0246\u0247\7v\2\2\u0247\u0248\7e\2\2"+
		"\u0248\u0249\7j\2\2\u0249\\\3\2\2\2\u024a\u024b\7u\2\2\u024b\u024c\7{"+
		"\2\2\u024c\u024d\7p\2\2\u024d\u024e\7e\2\2\u024e\u024f\7j\2\2\u024f\u0250"+
		"\7t\2\2\u0250\u0251\7q\2\2\u0251\u0252\7p\2\2\u0252\u0253\7k\2\2\u0253"+
		"\u0254\7|\2\2\u0254\u0255\7g\2\2\u0255\u0256\7f\2\2\u0256^\3\2\2\2\u0257"+
		"\u0258\7v\2\2\u0258\u0259\7j\2\2\u0259\u025a\7k\2\2\u025a\u025b\7u\2\2"+
		"\u025b`\3\2\2\2\u025c\u025d\7v\2\2\u025d\u025e\7j\2\2\u025e\u025f\7t\2"+
		"\2\u025f\u0260\7q\2\2\u0260\u0261\7y\2\2\u0261b\3\2\2\2\u0262\u0263\7"+
		"v\2\2\u0263\u0264\7j\2\2\u0264\u0265\7t\2\2\u0265\u0266\7q\2\2\u0266\u0267"+
		"\7y\2\2\u0267\u0268\7u\2\2\u0268d\3\2\2\2\u0269\u026a\7v\2\2\u026a\u026b"+
		"\7t\2\2\u026b\u026c\7c\2\2\u026c\u026d\7p\2\2\u026d\u026e\7u\2\2\u026e"+
		"\u026f\7k\2\2\u026f\u0270\7g\2\2\u0270\u0271\7p\2\2\u0271\u0272\7v\2\2"+
		"\u0272f\3\2\2\2\u0273\u0274\7v\2\2\u0274\u0275\7t\2\2\u0275\u0276\7{\2"+
		"\2\u0276h\3\2\2\2\u0277\u0278\7x\2\2\u0278\u0279\7q\2\2\u0279\u027a\7"+
		"k\2\2\u027a\u027b\7f\2\2\u027bj\3\2\2\2\u027c\u027d\7x\2\2\u027d\u027e"+
		"\7q\2\2\u027e\u027f\7n\2\2\u027f\u0280\7c\2\2\u0280\u0281\7v\2\2\u0281"+
		"\u0282\7k\2\2\u0282\u0283\7n\2\2\u0283\u0284\7g\2\2\u0284l\3\2\2\2\u0285"+
		"\u0286\7y\2\2\u0286\u0287\7j\2\2\u0287\u0288\7k\2\2\u0288\u0289\7n\2\2"+
		"\u0289\u028a\7g\2\2\u028an\3\2\2\2\u028b\u0290\5q9\2\u028c\u0290\5s:\2"+
		"\u028d\u0290\5u;\2\u028e\u0290\5w<\2\u028f\u028b\3\2\2\2\u028f\u028c\3"+
		"\2\2\2\u028f\u028d\3\2\2\2\u028f\u028e\3\2\2\2\u0290p\3\2\2\2\u0291\u0293"+
		"\5{>\2\u0292\u0294\5y=\2\u0293\u0292\3\2\2\2\u0293\u0294\3\2\2\2\u0294"+
		"r\3\2\2\2\u0295\u0297\5\u0087D\2\u0296\u0298\5y=\2\u0297\u0296\3\2\2\2"+
		"\u0297\u0298\3\2\2\2\u0298t\3\2\2\2\u0299\u029b\5\u008fH\2\u029a\u029c"+
		"\5y=\2\u029b\u029a\3\2\2\2\u029b\u029c\3\2\2\2\u029cv\3\2\2\2\u029d\u029f"+
		"\5\u0097L\2\u029e\u02a0\5y=\2\u029f\u029e\3\2\2\2\u029f\u02a0\3\2\2\2"+
		"\u02a0x\3\2\2\2\u02a1\u02a2\t\2\2\2\u02a2z\3\2\2\2\u02a3\u02ae\7\62\2"+
		"\2\u02a4\u02ab\5\u0081A\2\u02a5\u02a7\5}?\2\u02a6\u02a5\3\2\2\2\u02a6"+
		"\u02a7\3\2\2\2\u02a7\u02ac\3\2\2\2\u02a8\u02a9\5\u0085C\2\u02a9\u02aa"+
		"\5}?\2\u02aa\u02ac\3\2\2\2\u02ab\u02a6\3\2\2\2\u02ab\u02a8\3\2\2\2\u02ac"+
		"\u02ae\3\2\2\2\u02ad\u02a3\3\2\2\2\u02ad\u02a4\3\2\2\2\u02ae|\3\2\2\2"+
		"\u02af\u02b7\5\177@\2\u02b0\u02b2\5\u0083B\2\u02b1\u02b0\3\2\2\2\u02b2"+
		"\u02b5\3\2\2\2\u02b3\u02b1\3\2\2\2\u02b3\u02b4\3\2\2\2\u02b4\u02b6\3\2"+
		"\2\2\u02b5\u02b3\3\2\2\2\u02b6\u02b8\5\177@\2\u02b7\u02b3\3\2\2\2\u02b7"+
		"\u02b8\3\2\2\2\u02b8~\3\2\2\2\u02b9\u02bc\7\62\2\2\u02ba\u02bc\5\u0081"+
		"A\2\u02bb\u02b9\3\2\2\2\u02bb\u02ba\3\2\2\2\u02bc\u0080\3\2\2\2\u02bd"+
		"\u02be\t\3\2\2\u02be\u0082\3\2\2\2\u02bf\u02c2\5\177@\2\u02c0\u02c2\7"+
		"a\2\2\u02c1\u02bf\3\2\2\2\u02c1\u02c0\3\2\2\2\u02c2\u0084\3\2\2\2\u02c3"+
		"\u02c5\7a\2\2\u02c4\u02c3\3\2\2\2\u02c5\u02c6\3\2\2\2\u02c6\u02c4\3\2"+
		"\2\2\u02c6\u02c7\3\2\2\2\u02c7\u0086\3\2\2\2\u02c8\u02c9\7\62\2\2\u02c9"+
		"\u02ca\t\4\2\2\u02ca\u02cb\5\u0089E\2\u02cb\u0088\3\2\2\2\u02cc\u02d4"+
		"\5\u008bF\2\u02cd\u02cf\5\u008dG\2\u02ce\u02cd\3\2\2\2\u02cf\u02d2\3\2"+
		"\2\2\u02d0\u02ce\3\2\2\2\u02d0\u02d1\3\2\2\2\u02d1\u02d3\3\2\2\2\u02d2"+
		"\u02d0\3\2\2\2\u02d3\u02d5\5\u008bF\2\u02d4\u02d0\3\2\2\2\u02d4\u02d5"+
		"\3\2\2\2\u02d5\u008a\3\2\2\2\u02d6\u02d7\t\5\2\2\u02d7\u008c\3\2\2\2\u02d8"+
		"\u02db\5\u008bF\2\u02d9\u02db\7a\2\2\u02da\u02d8\3\2\2\2\u02da\u02d9\3"+
		"\2\2\2\u02db\u008e\3\2\2\2\u02dc\u02de\7\62\2\2\u02dd\u02df\5\u0085C\2"+
		"\u02de\u02dd\3\2\2\2\u02de\u02df\3\2\2\2\u02df\u02e0\3\2\2\2\u02e0\u02e1"+
		"\5\u0091I\2\u02e1\u0090\3\2\2\2\u02e2\u02ea\5\u0093J\2\u02e3\u02e5\5\u0095"+
		"K\2\u02e4\u02e3\3\2\2\2\u02e5\u02e8\3\2\2\2\u02e6\u02e4\3\2\2\2\u02e6"+
		"\u02e7\3\2\2\2\u02e7\u02e9\3\2\2\2\u02e8\u02e6\3\2\2\2\u02e9\u02eb\5\u0093"+
		"J\2\u02ea\u02e6\3\2\2\2\u02ea\u02eb\3\2\2\2\u02eb\u0092\3\2\2\2\u02ec"+
		"\u02ed\t\6\2\2\u02ed\u0094\3\2\2\2\u02ee\u02f1\5\u0093J\2\u02ef\u02f1"+
		"\7a\2\2\u02f0\u02ee\3\2\2\2\u02f0\u02ef\3\2\2\2\u02f1\u0096\3\2\2\2\u02f2"+
		"\u02f3\7\62\2\2\u02f3\u02f4\t\7\2\2\u02f4\u02f5\5\u0099M\2\u02f5\u0098"+
		"\3\2\2\2\u02f6\u02fe\5\u009bN\2\u02f7\u02f9\5\u009dO\2\u02f8\u02f7\3\2"+
		"\2\2\u02f9\u02fc\3\2\2\2\u02fa\u02f8\3\2\2\2\u02fa\u02fb\3\2\2\2\u02fb"+
		"\u02fd\3\2\2\2\u02fc\u02fa\3\2\2\2\u02fd\u02ff\5\u009bN\2\u02fe\u02fa"+
		"\3\2\2\2\u02fe\u02ff\3\2\2\2\u02ff\u009a\3\2\2\2\u0300\u0301\t\b\2\2\u0301"+
		"\u009c\3\2\2\2\u0302\u0305\5\u009bN\2\u0303\u0305\7a\2\2\u0304\u0302\3"+
		"\2\2\2\u0304\u0303\3\2\2\2\u0305\u009e\3\2\2\2\u0306\u0309\5\u00a1Q\2"+
		"\u0307\u0309\5\u00adW\2\u0308\u0306\3\2\2\2\u0308\u0307\3\2\2\2\u0309"+
		"\u00a0\3\2\2\2\u030a\u030b\5}?\2\u030b\u030d\7\60\2\2\u030c\u030e\5}?"+
		"\2\u030d\u030c\3\2\2\2\u030d\u030e\3\2\2\2\u030e\u0310\3\2\2\2\u030f\u0311"+
		"\5\u00a3R\2\u0310\u030f\3\2\2\2\u0310\u0311\3\2\2\2\u0311\u0313\3\2\2"+
		"\2\u0312\u0314\5\u00abV\2\u0313\u0312\3\2\2\2\u0313\u0314\3\2\2\2\u0314"+
		"\u0326\3\2\2\2\u0315\u0316\7\60\2\2\u0316\u0318\5}?\2\u0317\u0319\5\u00a3"+
		"R\2\u0318\u0317\3\2\2\2\u0318\u0319\3\2\2\2\u0319\u031b\3\2\2\2\u031a"+
		"\u031c\5\u00abV\2\u031b\u031a\3\2\2\2\u031b\u031c\3\2\2\2\u031c\u0326"+
		"\3\2\2\2\u031d\u031e\5}?\2\u031e\u0320\5\u00a3R\2\u031f\u0321\5\u00ab"+
		"V\2\u0320\u031f\3\2\2\2\u0320\u0321\3\2\2\2\u0321\u0326\3\2\2\2\u0322"+
		"\u0323\5}?\2\u0323\u0324\5\u00abV\2\u0324\u0326\3\2\2\2\u0325\u030a\3"+
		"\2\2\2\u0325\u0315\3\2\2\2\u0325\u031d\3\2\2\2\u0325\u0322\3\2\2\2\u0326"+
		"\u00a2\3\2\2\2\u0327\u0328\5\u00a5S\2\u0328\u0329\5\u00a7T\2\u0329\u00a4"+
		"\3\2\2\2\u032a\u032b\t\t\2\2\u032b\u00a6\3\2\2\2\u032c\u032e\5\u00a9U"+
		"\2\u032d\u032c\3\2\2\2\u032d\u032e\3\2\2\2\u032e\u032f\3\2\2\2\u032f\u0330"+
		"\5}?\2\u0330\u00a8\3\2\2\2\u0331\u0332\t\n\2\2\u0332\u00aa\3\2\2\2\u0333"+
		"\u0334\t\13\2\2\u0334\u00ac\3\2\2\2\u0335\u0336\5\u00afX\2\u0336\u0338"+
		"\5\u00b1Y\2\u0337\u0339\5\u00abV\2\u0338\u0337\3\2\2\2\u0338\u0339\3\2"+
		"\2\2\u0339\u00ae\3\2\2\2\u033a\u033c\5\u0087D\2\u033b\u033d\7\60\2\2\u033c"+
		"\u033b\3\2\2\2\u033c\u033d\3\2\2\2\u033d\u0346\3\2\2\2\u033e\u033f\7\62"+
		"\2\2\u033f\u0341\t\4\2\2\u0340\u0342\5\u0089E\2\u0341\u0340\3\2\2\2\u0341"+
		"\u0342\3\2\2\2\u0342\u0343\3\2\2\2\u0343\u0344\7\60\2\2\u0344\u0346\5"+
		"\u0089E\2\u0345\u033a\3\2\2\2\u0345\u033e\3\2\2\2\u0346\u00b0\3\2\2\2"+
		"\u0347\u0348\5\u00b3Z\2\u0348\u0349\5\u00a7T\2\u0349\u00b2\3\2\2\2\u034a"+
		"\u034b\t\f\2\2\u034b\u00b4\3\2\2\2\u034c\u034d\7v\2\2\u034d\u034e\7t\2"+
		"\2\u034e\u034f\7w\2\2\u034f\u0356\7g\2\2\u0350\u0351\7h\2\2\u0351\u0352"+
		"\7c\2\2\u0352\u0353\7n\2\2\u0353\u0354\7u\2\2\u0354\u0356\7g\2\2\u0355"+
		"\u034c\3\2\2\2\u0355\u0350\3\2\2\2\u0356\u00b6\3\2\2\2\u0357\u0358\7)"+
		"\2\2\u0358\u0359\5\u00b9]\2\u0359\u035a\7)\2\2\u035a\u0360\3\2\2\2\u035b"+
		"\u035c\7)\2\2\u035c\u035d\5\u00c1a\2\u035d\u035e\7)\2\2\u035e\u0360\3"+
		"\2\2\2\u035f\u0357\3\2\2\2\u035f\u035b\3\2\2\2\u0360\u00b8\3\2\2\2\u0361"+
		"\u0362\n\r\2\2\u0362\u00ba\3\2\2\2\u0363\u0365\7$\2\2\u0364\u0366\5\u00bd"+
		"_\2\u0365\u0364\3\2\2\2\u0365\u0366\3\2\2\2\u0366\u0367\3\2\2\2\u0367"+
		"\u0368\7$\2\2\u0368\u00bc\3\2\2\2\u0369\u036b\5\u00bf`\2\u036a\u0369\3"+
		"\2\2\2\u036b\u036c\3\2\2\2\u036c\u036a\3\2\2\2\u036c\u036d\3\2\2\2\u036d"+
		"\u00be\3\2\2\2\u036e\u0371\n\16\2\2\u036f\u0371\5\u00c1a\2\u0370\u036e"+
		"\3\2\2\2\u0370\u036f\3\2\2\2\u0371\u00c0\3\2\2\2\u0372\u0373\7^\2\2\u0373"+
		"\u0377\t\17\2\2\u0374\u0377\5\u00c3b\2\u0375\u0377\5\u00c5c\2\u0376\u0372"+
		"\3\2\2\2\u0376\u0374\3\2\2\2\u0376\u0375\3\2\2\2\u0377\u00c2\3\2\2\2\u0378"+
		"\u0379\7^\2\2\u0379\u0384\5\u0093J\2\u037a\u037b\7^\2\2\u037b\u037c\5"+
		"\u0093J\2\u037c\u037d\5\u0093J\2\u037d\u0384\3\2\2\2\u037e\u037f\7^\2"+
		"\2\u037f\u0380\5\u00c7d\2\u0380\u0381\5\u0093J\2\u0381\u0382\5\u0093J"+
		"\2\u0382\u0384\3\2\2\2\u0383\u0378\3\2\2\2\u0383\u037a\3\2\2\2\u0383\u037e"+
		"\3\2\2\2\u0384\u00c4\3\2\2\2\u0385\u0386\7^\2\2\u0386\u0387\7w\2\2\u0387"+
		"\u0388\5\u008bF\2\u0388\u0389\5\u008bF\2\u0389\u038a\5\u008bF\2\u038a"+
		"\u038b\5\u008bF\2\u038b\u00c6\3\2\2\2\u038c\u038d\t\20\2\2\u038d\u00c8"+
		"\3\2\2\2\u038e\u038f\7p\2\2\u038f\u0390\7w\2\2\u0390\u0391\7n\2\2\u0391"+
		"\u0392\7n\2\2\u0392\u00ca\3\2\2\2\u0393\u0394\7*\2\2\u0394\u00cc\3\2\2"+
		"\2\u0395\u0396\7+\2\2\u0396\u00ce\3\2\2\2\u0397\u0398\7}\2\2\u0398\u00d0"+
		"\3\2\2\2\u0399\u039a\7\177\2\2\u039a\u00d2\3\2\2\2\u039b\u039c\7]\2\2"+
		"\u039c\u00d4\3\2\2\2\u039d\u039e\7_\2\2\u039e\u00d6\3\2\2\2\u039f\u03a0"+
		"\7=\2\2\u03a0\u00d8\3\2\2\2\u03a1\u03a2\7.\2\2\u03a2\u00da\3\2\2\2\u03a3"+
		"\u03a4\7\60\2\2\u03a4\u00dc\3\2\2\2\u03a5\u03a6\7?\2\2\u03a6\u00de\3\2"+
		"\2\2\u03a7\u03a8\7@\2\2\u03a8\u00e0\3\2\2\2\u03a9\u03aa\7>\2\2\u03aa\u00e2"+
		"\3\2\2\2\u03ab\u03ac\7#\2\2\u03ac\u00e4\3\2\2\2\u03ad\u03ae\7\u0080\2"+
		"\2\u03ae\u00e6\3\2\2\2\u03af\u03b0\7A\2\2\u03b0\u00e8\3\2\2\2\u03b1\u03b2"+
		"\7<\2\2\u03b2\u00ea\3\2\2\2\u03b3\u03b4\7?\2\2\u03b4\u03b5\7?\2\2\u03b5"+
		"\u00ec\3\2\2\2\u03b6\u03b7\7>\2\2\u03b7\u03b8\7?\2\2\u03b8\u00ee\3\2\2"+
		"\2\u03b9\u03ba\7@\2\2\u03ba\u03bb\7?\2\2\u03bb\u00f0\3\2\2\2\u03bc\u03bd"+
		"\7#\2\2\u03bd\u03be\7?\2\2\u03be\u00f2\3\2\2\2\u03bf\u03c0\7(\2\2\u03c0"+
		"\u03c1\7(\2\2\u03c1\u00f4\3\2\2\2\u03c2\u03c3\7~\2\2\u03c3\u03c4\7~\2"+
		"\2\u03c4\u00f6\3\2\2\2\u03c5\u03c6\7-\2\2\u03c6\u03c7\7-\2\2\u03c7\u00f8"+
		"\3\2\2\2\u03c8\u03c9\7/\2\2\u03c9\u03ca\7/\2\2\u03ca\u00fa\3\2\2\2\u03cb"+
		"\u03cc\7-\2\2\u03cc\u00fc\3\2\2\2\u03cd\u03ce\7/\2\2\u03ce\u00fe\3\2\2"+
		"\2\u03cf\u03d0\7,\2\2\u03d0\u0100\3\2\2\2\u03d1\u03d2\7\61\2\2\u03d2\u0102"+
		"\3\2\2\2\u03d3\u03d4\7(\2\2\u03d4\u0104\3\2\2\2\u03d5\u03d6\7~\2\2\u03d6"+
		"\u0106\3\2\2\2\u03d7\u03d8\7`\2\2\u03d8\u0108\3\2\2\2\u03d9\u03da\7\'"+
		"\2\2\u03da\u010a\3\2\2\2\u03db\u03dc\7-\2\2\u03dc\u03dd\7?\2\2\u03dd\u010c"+
		"\3\2\2\2\u03de\u03df\7/\2\2\u03df\u03e0\7?\2\2\u03e0\u010e\3\2\2\2\u03e1"+
		"\u03e2\7,\2\2\u03e2\u03e3\7?\2\2\u03e3\u0110\3\2\2\2\u03e4\u03e5\7\61"+
		"\2\2\u03e5\u03e6\7?\2\2\u03e6\u0112\3\2\2\2\u03e7\u03e8\7(\2\2\u03e8\u03e9"+
		"\7?\2\2\u03e9\u0114\3\2\2\2\u03ea\u03eb\7~\2\2\u03eb\u03ec\7?\2\2\u03ec"+
		"\u0116\3\2\2\2\u03ed\u03ee\7`\2\2\u03ee\u03ef\7?\2\2\u03ef\u0118\3\2\2"+
		"\2\u03f0\u03f1\7\'\2\2\u03f1\u03f2\7?\2\2\u03f2\u011a\3\2\2\2\u03f3\u03f4"+
		"\7>\2\2\u03f4\u03f5\7>\2\2\u03f5\u03f6\7?\2\2\u03f6\u011c\3\2\2\2\u03f7"+
		"\u03f8\7@\2\2\u03f8\u03f9\7@\2\2\u03f9\u03fa\7?\2\2\u03fa\u011e\3\2\2"+
		"\2\u03fb\u03fc\7@\2\2\u03fc\u03fd\7@\2\2\u03fd\u03fe\7@\2\2\u03fe\u03ff"+
		"\7?\2\2\u03ff\u0120\3\2\2\2\u0400\u0404\5\u0123\u0092\2\u0401\u0403\5"+
		"\u0125\u0093\2\u0402\u0401\3\2\2\2\u0403\u0406\3\2\2\2\u0404\u0402\3\2"+
		"\2\2\u0404\u0405\3\2\2\2\u0405\u0122\3\2\2\2\u0406\u0404\3\2\2\2\u0407"+
		"\u040e\t\21\2\2\u0408\u0409\n\22\2\2\u0409\u040e\6\u0092\2\2\u040a\u040b"+
		"\t\23\2\2\u040b\u040c\t\24\2\2\u040c\u040e\6\u0092\3\2\u040d\u0407\3\2"+
		"\2\2\u040d\u0408\3\2\2\2\u040d\u040a\3\2\2\2\u040e\u0124\3\2\2\2\u040f"+
		"\u0416\t\25\2\2\u0410\u0411\n\22\2\2\u0411\u0416\6\u0093\4\2\u0412\u0413"+
		"\t\23\2\2\u0413\u0414\t\24\2\2\u0414\u0416\6\u0093\5\2\u0415\u040f\3\2"+
		"\2\2\u0415\u0410\3\2\2\2\u0415\u0412\3\2\2\2\u0416\u0126\3\2\2\2\u0417"+
		"\u0418\7\60\2\2\u0418\u0419\7\60\2\2\u0419\u041a\7\60\2\2\u041a\u0128"+
		"\3\2\2\2\u041b\u041d\t\26\2\2\u041c\u041b\3\2\2\2\u041d\u041e\3\2\2\2"+
		"\u041e\u041c\3\2\2\2\u041e\u041f\3\2\2\2\u041f\u0420\3\2\2\2\u0420\u0421"+
		"\b\u0095\2\2\u0421\u012a\3\2\2\2\u0422\u0423\7\61\2\2\u0423\u0424\7,\2"+
		"\2\u0424\u0428\3\2\2\2\u0425\u0427\13\2\2\2\u0426\u0425\3\2\2\2\u0427"+
		"\u042a\3\2\2\2\u0428\u0429\3\2\2\2\u0428\u0426\3\2\2\2\u0429\u042b\3\2"+
		"\2\2\u042a\u0428\3\2\2\2\u042b\u042c\7,\2\2\u042c\u042d\7\61\2\2\u042d"+
		"\u042e\3\2\2\2\u042e\u042f\b\u0096\2\2\u042f\u012c\3\2\2\2\u0430\u0431"+
		"\7\61\2\2\u0431\u0432\7\61\2\2\u0432\u0436\3\2\2\2\u0433\u0435\n\27\2"+
		"\2\u0434\u0433\3\2\2\2\u0435\u0438\3\2\2\2\u0436\u0434\3\2\2\2\u0436\u0437"+
		"\3\2\2\2\u0437\u0439\3\2\2\2\u0438\u0436\3\2\2\2\u0439\u043a\b\u0097\2"+
		"\2\u043a\u012e\3\2\2\2\64\2\u028f\u0293\u0297\u029b\u029f\u02a6\u02ab"+
		"\u02ad\u02b3\u02b7\u02bb\u02c1\u02c6\u02d0\u02d4\u02da\u02de\u02e6\u02ea"+
		"\u02f0\u02fa\u02fe\u0304\u0308\u030d\u0310\u0313\u0318\u031b\u0320\u0325"+
		"\u032d\u0338\u033c\u0341\u0345\u0355\u035f\u0365\u036c\u0370\u0376\u0383"+
		"\u0404\u040d\u0415\u041e\u0428\u0436\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}