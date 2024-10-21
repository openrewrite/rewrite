/*
 * Copyright 2024 the original author or authors.
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
// Generated from java-escape by ANTLR 4.11.1
package org.openrewrite.xml.internal.grammar;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.LexerATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class XMLLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.11.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		COMMENT=1, CDATA=2, ParamEntityRef=3, EntityRef=4, CharRef=5, SEA_WS=6, 
		UTF_ENCODING_BOM=7, QUESTION_MARK=8, SPECIAL_OPEN_XML=9, OPEN=10, SPECIAL_OPEN=11, 
		DTD_OPEN=12, TEXT=13, DTD_CLOSE=14, DTD_SUBSET_OPEN=15, DTD_S=16, DOCTYPE=17, 
		DTD_SUBSET_CLOSE=18, MARKUP_OPEN=19, DTS_SUBSET_S=20, MARK_UP_CLOSE=21, 
		MARKUP_S=22, MARKUP_TEXT=23, MARKUP_SUBSET=24, PI_S=25, PI_TEXT=26, CLOSE=27, 
		SPECIAL_CLOSE=28, SLASH_CLOSE=29, S=30, DIRECTIVE_OPEN=31, DIRECTIVE_CLOSE=32, 
		SLASH=33, EQUALS=34, STRING=35, Name=36;
	public static final int
		INSIDE_DTD=1, INSIDE_DTD_SUBSET=2, INSIDE_MARKUP=3, INSIDE_MARKUP_SUBSET=4, 
		INSIDE_PROCESS_INSTRUCTION=5, INSIDE_PI_TEXT=6, INSIDE=7;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE", "INSIDE_DTD", "INSIDE_DTD_SUBSET", "INSIDE_MARKUP", "INSIDE_MARKUP_SUBSET", 
		"INSIDE_PROCESS_INSTRUCTION", "INSIDE_PI_TEXT", "INSIDE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"COMMENT", "CDATA", "ParamEntityRef", "EntityRef", "CharRef", "SEA_WS", 
			"UTF_ENCODING_BOM", "QUESTION_MARK", "SPECIAL_OPEN_XML", "OPEN", "SPECIAL_OPEN", 
			"DTD_OPEN", "TEXT", "UTF_8_BOM_CHARS", "UTF_8_BOM", "DTD_CLOSE", "DTD_SUBSET_OPEN", 
			"DTD_S", "DOCTYPE", "DTD_NAME", "DTD_STRING", "DTD_SUBSET_CLOSE", "MARKUP_OPEN", 
			"DTS_SUBSET_S", "DTD_PERef", "DTD_SUBSET_COMMENT", "MARK_UP_CLOSE", "MARKUP_SUBSET_OPEN", 
			"MARKUP_S", "MARKUP_TEXT", "MARKUP_SUBSET", "TXT", "PI_SPECIAL_CLOSE", 
			"PI_S", "PI_QUESTION_MARK", "PI_TEXT", "PI_TEXT_SPECIAL_CASE", "CLOSE", 
			"SPECIAL_CLOSE", "SLASH_CLOSE", "S", "DIRECTIVE_OPEN", "DIRECTIVE_CLOSE", 
			"SLASH", "EQUALS", "STRING", "Name", "HEXDIGIT", "DIGIT", "NameChar", 
			"NameStartChar"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, "'?'", null, "'<'", null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, "'/>'", null, "'%@'", "'%'", "'/'", "'='"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "COMMENT", "CDATA", "ParamEntityRef", "EntityRef", "CharRef", "SEA_WS", 
			"UTF_ENCODING_BOM", "QUESTION_MARK", "SPECIAL_OPEN_XML", "OPEN", "SPECIAL_OPEN", 
			"DTD_OPEN", "TEXT", "DTD_CLOSE", "DTD_SUBSET_OPEN", "DTD_S", "DOCTYPE", 
			"DTD_SUBSET_CLOSE", "MARKUP_OPEN", "DTS_SUBSET_S", "MARK_UP_CLOSE", "MARKUP_S", 
			"MARKUP_TEXT", "MARKUP_SUBSET", "PI_S", "PI_TEXT", "CLOSE", "SPECIAL_CLOSE", 
			"SLASH_CLOSE", "S", "DIRECTIVE_OPEN", "DIRECTIVE_CLOSE", "SLASH", "EQUALS", 
			"STRING", "Name"
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


	public XMLLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "XMLLexer.g4"; }

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

	public static final String _serializedATN =
		"\u0004\u0000$\u0189\u0006\uffff\uffff\u0006\uffff\uffff\u0006\uffff\uffff"+
		"\u0006\uffff\uffff\u0006\uffff\uffff\u0006\uffff\uffff\u0006\uffff\uffff"+
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
		"2\u00072\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001"+
		"\u0000\u0005\u0000u\b\u0000\n\u0000\f\u0000x\t\u0000\u0001\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0005\u0001\u0089\b\u0001\n\u0001\f\u0001\u008c\t\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0004\u0004\u009e\b\u0004"+
		"\u000b\u0004\f\u0004\u009f\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004"+
		"\u0001\u0004\u0001\u0004\u0001\u0004\u0004\u0004\u00a9\b\u0004\u000b\u0004"+
		"\f\u0004\u00aa\u0001\u0004\u0001\u0004\u0003\u0004\u00af\b\u0004\u0001"+
		"\u0005\u0001\u0005\u0003\u0005\u00b3\b\u0005\u0001\u0005\u0004\u0005\u00b6"+
		"\b\u0005\u000b\u0005\f\u0005\u00b7\u0001\u0005\u0001\u0005\u0001\u0006"+
		"\u0001\u0006\u0003\u0006\u00be\b\u0006\u0001\u0006\u0001\u0006\u0001\u0007"+
		"\u0001\u0007\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001"+
		"\b\u0001\t\u0001\t\u0001\t\u0001\t\u0001\n\u0001\n\u0001\n\u0001\n\u0001"+
		"\n\u0001\n\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b"+
		"\u0001\f\u0004\f\u00dc\b\f\u000b\f\f\f\u00dd\u0001\r\u0001\r\u0001\r\u0001"+
		"\r\u0001\u000e\u0001\u000e\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f"+
		"\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0011\u0001\u0011"+
		"\u0001\u0011\u0001\u0011\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012"+
		"\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0013\u0001\u0013"+
		"\u0001\u0013\u0001\u0013\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0014"+
		"\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0016\u0001\u0016"+
		"\u0001\u0016\u0001\u0016\u0001\u0016\u0001\u0017\u0001\u0017\u0001\u0017"+
		"\u0001\u0017\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0019"+
		"\u0001\u0019\u0001\u0019\u0001\u0019\u0001\u001a\u0001\u001a\u0001\u001a"+
		"\u0001\u001a\u0001\u001b\u0001\u001b\u0001\u001b\u0001\u001b\u0001\u001b"+
		"\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001d\u0004\u001d"+
		"\u0125\b\u001d\u000b\u001d\f\u001d\u0126\u0001\u001e\u0001\u001e\u0001"+
		"\u001e\u0001\u001e\u0001\u001f\u0001\u001f\u0001\u001f\u0001\u001f\u0001"+
		" \u0001 \u0001 \u0001 \u0001 \u0001!\u0001!\u0001!\u0001!\u0001\"\u0001"+
		"\"\u0001\"\u0001\"\u0001\"\u0001#\u0004#\u0140\b#\u000b#\f#\u0141\u0001"+
		"$\u0001$\u0001$\u0001$\u0001$\u0001%\u0001%\u0001%\u0001%\u0001&\u0001"+
		"&\u0001&\u0001&\u0001&\u0001\'\u0001\'\u0001\'\u0001\'\u0001\'\u0001("+
		"\u0001(\u0001(\u0001(\u0001)\u0001)\u0001)\u0001*\u0001*\u0001+\u0001"+
		"+\u0001,\u0001,\u0001-\u0001-\u0005-\u0166\b-\n-\f-\u0169\t-\u0001-\u0001"+
		"-\u0001-\u0005-\u016e\b-\n-\f-\u0171\t-\u0001-\u0003-\u0174\b-\u0001."+
		"\u0001.\u0005.\u0178\b.\n.\f.\u017b\t.\u0001/\u0001/\u00010\u00010\u0001"+
		"1\u00011\u00011\u00011\u00031\u0185\b1\u00012\u00032\u0188\b2\u0002v\u008a"+
		"\u00003\b\u0001\n\u0002\f\u0003\u000e\u0004\u0010\u0005\u0012\u0006\u0014"+
		"\u0007\u0016\b\u0018\t\u001a\n\u001c\u000b\u001e\f \r\"\u0000$\u0000&"+
		"\u000e(\u000f*\u0010,\u0011.\u00000\u00002\u00124\u00136\u00148\u0000"+
		":\u0000<\u0015>\u0000@\u0016B\u0017D\u0018F\u0000H\u0000J\u0019L\u0000"+
		"N\u001aP\u0000R\u001bT\u001cV\u001dX\u001eZ\u001f\\ ^!`\"b#d$f\u0000h"+
		"\u0000j\u0000l\u0000\b\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007"+
		"\u0013\u0002\u0000\t\t  \u0002\u0000&&<<\u0002\u0000DDdd\u0002\u0000O"+
		"Ooo\u0002\u0000CCcc\u0002\u0000TTtt\u0002\u0000YYyy\u0002\u0000PPpp\u0002"+
		"\u0000EEee\u0002\u0000>>[[\u0001\u0000??\u0003\u0000\t\n\r\r  \u0002\u0000"+
		"\"\"<<\u0002\u0000\'\'<<\u0003\u000009AFaf\u0001\u000009\u0002\u0000-"+
		".__\u0003\u0000\u00b7\u00b7\u0300\u036f\u203f\u2040\u000f\u0000::AZ__"+
		"az\u00c0\u00d6\u00d8\u00f6\u00f8\u02ff\u0370\u037d\u037f\u1fff\u200c\u200d"+
		"\u2070\u218f\u3001\u8000\ud7ff\u8000\uf900\u8000\ufdcf\u8000\ufdf0\u8000"+
		"\ufffd\u8001\u0000\u800e\uffff\u018e\u0000\b\u0001\u0000\u0000\u0000\u0000"+
		"\n\u0001\u0000\u0000\u0000\u0000\f\u0001\u0000\u0000\u0000\u0000\u000e"+
		"\u0001\u0000\u0000\u0000\u0000\u0010\u0001\u0000\u0000\u0000\u0000\u0012"+
		"\u0001\u0000\u0000\u0000\u0000\u0014\u0001\u0000\u0000\u0000\u0000\u0016"+
		"\u0001\u0000\u0000\u0000\u0000\u0018\u0001\u0000\u0000\u0000\u0000\u001a"+
		"\u0001\u0000\u0000\u0000\u0000\u001c\u0001\u0000\u0000\u0000\u0000\u001e"+
		"\u0001\u0000\u0000\u0000\u0000 \u0001\u0000\u0000\u0000\u0001&\u0001\u0000"+
		"\u0000\u0000\u0001(\u0001\u0000\u0000\u0000\u0001*\u0001\u0000\u0000\u0000"+
		"\u0001,\u0001\u0000\u0000\u0000\u0001.\u0001\u0000\u0000\u0000\u00010"+
		"\u0001\u0000\u0000\u0000\u00022\u0001\u0000\u0000\u0000\u00024\u0001\u0000"+
		"\u0000\u0000\u00026\u0001\u0000\u0000\u0000\u00028\u0001\u0000\u0000\u0000"+
		"\u0002:\u0001\u0000\u0000\u0000\u0003<\u0001\u0000\u0000\u0000\u0003>"+
		"\u0001\u0000\u0000\u0000\u0003@\u0001\u0000\u0000\u0000\u0003B\u0001\u0000"+
		"\u0000\u0000\u0004D\u0001\u0000\u0000\u0000\u0004F\u0001\u0000\u0000\u0000"+
		"\u0005H\u0001\u0000\u0000\u0000\u0005J\u0001\u0000\u0000\u0000\u0005L"+
		"\u0001\u0000\u0000\u0000\u0005N\u0001\u0000\u0000\u0000\u0006P\u0001\u0000"+
		"\u0000\u0000\u0007R\u0001\u0000\u0000\u0000\u0007T\u0001\u0000\u0000\u0000"+
		"\u0007V\u0001\u0000\u0000\u0000\u0007X\u0001\u0000\u0000\u0000\u0007Z"+
		"\u0001\u0000\u0000\u0000\u0007\\\u0001\u0000\u0000\u0000\u0007^\u0001"+
		"\u0000\u0000\u0000\u0007`\u0001\u0000\u0000\u0000\u0007b\u0001\u0000\u0000"+
		"\u0000\u0007d\u0001\u0000\u0000\u0000\bn\u0001\u0000\u0000\u0000\n}\u0001"+
		"\u0000\u0000\u0000\f\u0091\u0001\u0000\u0000\u0000\u000e\u0095\u0001\u0000"+
		"\u0000\u0000\u0010\u00ae\u0001\u0000\u0000\u0000\u0012\u00b5\u0001\u0000"+
		"\u0000\u0000\u0014\u00bd\u0001\u0000\u0000\u0000\u0016\u00c1\u0001\u0000"+
		"\u0000\u0000\u0018\u00c3\u0001\u0000\u0000\u0000\u001a\u00cb\u0001\u0000"+
		"\u0000\u0000\u001c\u00cf\u0001\u0000\u0000\u0000\u001e\u00d5\u0001\u0000"+
		"\u0000\u0000 \u00db\u0001\u0000\u0000\u0000\"\u00df\u0001\u0000\u0000"+
		"\u0000$\u00e3\u0001\u0000\u0000\u0000&\u00e5\u0001\u0000\u0000\u0000("+
		"\u00e9\u0001\u0000\u0000\u0000*\u00ed\u0001\u0000\u0000\u0000,\u00f1\u0001"+
		"\u0000\u0000\u0000.\u00f9\u0001\u0000\u0000\u00000\u00fd\u0001\u0000\u0000"+
		"\u00002\u0101\u0001\u0000\u0000\u00004\u0105\u0001\u0000\u0000\u00006"+
		"\u010a\u0001\u0000\u0000\u00008\u010e\u0001\u0000\u0000\u0000:\u0112\u0001"+
		"\u0000\u0000\u0000<\u0116\u0001\u0000\u0000\u0000>\u011a\u0001\u0000\u0000"+
		"\u0000@\u011f\u0001\u0000\u0000\u0000B\u0124\u0001\u0000\u0000\u0000D"+
		"\u0128\u0001\u0000\u0000\u0000F\u012c\u0001\u0000\u0000\u0000H\u0130\u0001"+
		"\u0000\u0000\u0000J\u0135\u0001\u0000\u0000\u0000L\u0139\u0001\u0000\u0000"+
		"\u0000N\u013f\u0001\u0000\u0000\u0000P\u0143\u0001\u0000\u0000\u0000R"+
		"\u0148\u0001\u0000\u0000\u0000T\u014c\u0001\u0000\u0000\u0000V\u0151\u0001"+
		"\u0000\u0000\u0000X\u0156\u0001\u0000\u0000\u0000Z\u015a\u0001\u0000\u0000"+
		"\u0000\\\u015d\u0001\u0000\u0000\u0000^\u015f\u0001\u0000\u0000\u0000"+
		"`\u0161\u0001\u0000\u0000\u0000b\u0173\u0001\u0000\u0000\u0000d\u0175"+
		"\u0001\u0000\u0000\u0000f\u017c\u0001\u0000\u0000\u0000h\u017e\u0001\u0000"+
		"\u0000\u0000j\u0184\u0001\u0000\u0000\u0000l\u0187\u0001\u0000\u0000\u0000"+
		"no\u0005<\u0000\u0000op\u0005!\u0000\u0000pq\u0005-\u0000\u0000qr\u0005"+
		"-\u0000\u0000rv\u0001\u0000\u0000\u0000su\t\u0000\u0000\u0000ts\u0001"+
		"\u0000\u0000\u0000ux\u0001\u0000\u0000\u0000vw\u0001\u0000\u0000\u0000"+
		"vt\u0001\u0000\u0000\u0000wy\u0001\u0000\u0000\u0000xv\u0001\u0000\u0000"+
		"\u0000yz\u0005-\u0000\u0000z{\u0005-\u0000\u0000{|\u0005>\u0000\u0000"+
		"|\t\u0001\u0000\u0000\u0000}~\u0005<\u0000\u0000~\u007f\u0005!\u0000\u0000"+
		"\u007f\u0080\u0005[\u0000\u0000\u0080\u0081\u0005C\u0000\u0000\u0081\u0082"+
		"\u0005D\u0000\u0000\u0082\u0083\u0005A\u0000\u0000\u0083\u0084\u0005T"+
		"\u0000\u0000\u0084\u0085\u0005A\u0000\u0000\u0085\u0086\u0005[\u0000\u0000"+
		"\u0086\u008a\u0001\u0000\u0000\u0000\u0087\u0089\t\u0000\u0000\u0000\u0088"+
		"\u0087\u0001\u0000\u0000\u0000\u0089\u008c\u0001\u0000\u0000\u0000\u008a"+
		"\u008b\u0001\u0000\u0000\u0000\u008a\u0088\u0001\u0000\u0000\u0000\u008b"+
		"\u008d\u0001\u0000\u0000\u0000\u008c\u008a\u0001\u0000\u0000\u0000\u008d"+
		"\u008e\u0005]\u0000\u0000\u008e\u008f\u0005]\u0000\u0000\u008f\u0090\u0005"+
		">\u0000\u0000\u0090\u000b\u0001\u0000\u0000\u0000\u0091\u0092\u0005%\u0000"+
		"\u0000\u0092\u0093\u0003d.\u0000\u0093\u0094\u0005;\u0000\u0000\u0094"+
		"\r\u0001\u0000\u0000\u0000\u0095\u0096\u0005&\u0000\u0000\u0096\u0097"+
		"\u0003d.\u0000\u0097\u0098\u0005;\u0000\u0000\u0098\u000f\u0001\u0000"+
		"\u0000\u0000\u0099\u009a\u0005&\u0000\u0000\u009a\u009b\u0005#\u0000\u0000"+
		"\u009b\u009d\u0001\u0000\u0000\u0000\u009c\u009e\u0003h0\u0000\u009d\u009c"+
		"\u0001\u0000\u0000\u0000\u009e\u009f\u0001\u0000\u0000\u0000\u009f\u009d"+
		"\u0001\u0000\u0000\u0000\u009f\u00a0\u0001\u0000\u0000\u0000\u00a0\u00a1"+
		"\u0001\u0000\u0000\u0000\u00a1\u00a2\u0005;\u0000\u0000\u00a2\u00af\u0001"+
		"\u0000\u0000\u0000\u00a3\u00a4\u0005&\u0000\u0000\u00a4\u00a5\u0005#\u0000"+
		"\u0000\u00a5\u00a6\u0005x\u0000\u0000\u00a6\u00a8\u0001\u0000\u0000\u0000"+
		"\u00a7\u00a9\u0003f/\u0000\u00a8\u00a7\u0001\u0000\u0000\u0000\u00a9\u00aa"+
		"\u0001\u0000\u0000\u0000\u00aa\u00a8\u0001\u0000\u0000\u0000\u00aa\u00ab"+
		"\u0001\u0000\u0000\u0000\u00ab\u00ac\u0001\u0000\u0000\u0000\u00ac\u00ad"+
		"\u0005;\u0000\u0000\u00ad\u00af\u0001\u0000\u0000\u0000\u00ae\u0099\u0001"+
		"\u0000\u0000\u0000\u00ae\u00a3\u0001\u0000\u0000\u0000\u00af\u0011\u0001"+
		"\u0000\u0000\u0000\u00b0\u00b6\u0007\u0000\u0000\u0000\u00b1\u00b3\u0005"+
		"\r\u0000\u0000\u00b2\u00b1\u0001\u0000\u0000\u0000\u00b2\u00b3\u0001\u0000"+
		"\u0000\u0000\u00b3\u00b4\u0001\u0000\u0000\u0000\u00b4\u00b6\u0005\n\u0000"+
		"\u0000\u00b5\u00b0\u0001\u0000\u0000\u0000\u00b5\u00b2\u0001\u0000\u0000"+
		"\u0000\u00b6\u00b7\u0001\u0000\u0000\u0000\u00b7\u00b5\u0001\u0000\u0000"+
		"\u0000\u00b7\u00b8\u0001\u0000\u0000\u0000\u00b8\u00b9\u0001\u0000\u0000"+
		"\u0000\u00b9\u00ba\u0006\u0005\u0000\u0000\u00ba\u0013\u0001\u0000\u0000"+
		"\u0000\u00bb\u00be\u0003\"\r\u0000\u00bc\u00be\u0003$\u000e\u0000\u00bd"+
		"\u00bb\u0001\u0000\u0000\u0000\u00bd\u00bc\u0001\u0000\u0000\u0000\u00be"+
		"\u00bf\u0001\u0000\u0000\u0000\u00bf\u00c0\u0006\u0006\u0000\u0000\u00c0"+
		"\u0015\u0001\u0000\u0000\u0000\u00c1\u00c2\u0005?\u0000\u0000\u00c2\u0017"+
		"\u0001\u0000\u0000\u0000\u00c3\u00c4\u0005<\u0000\u0000\u00c4\u00c5\u0003"+
		"\u0016\u0007\u0000\u00c5\u00c6\u0005x\u0000\u0000\u00c6\u00c7\u0005m\u0000"+
		"\u0000\u00c7\u00c8\u0005l\u0000\u0000\u00c8\u00c9\u0001\u0000\u0000\u0000"+
		"\u00c9\u00ca\u0006\b\u0001\u0000\u00ca\u0019\u0001\u0000\u0000\u0000\u00cb"+
		"\u00cc\u0005<\u0000\u0000\u00cc\u00cd\u0001\u0000\u0000\u0000\u00cd\u00ce"+
		"\u0006\t\u0001\u0000\u00ce\u001b\u0001\u0000\u0000\u0000\u00cf\u00d0\u0005"+
		"<\u0000\u0000\u00d0\u00d1\u0003\u0016\u0007\u0000\u00d1\u00d2\u0003d."+
		"\u0000\u00d2\u00d3\u0001\u0000\u0000\u0000\u00d3\u00d4\u0006\n\u0002\u0000"+
		"\u00d4\u001d\u0001\u0000\u0000\u0000\u00d5\u00d6\u0005<\u0000\u0000\u00d6"+
		"\u00d7\u0005!\u0000\u0000\u00d7\u00d8\u0001\u0000\u0000\u0000\u00d8\u00d9"+
		"\u0006\u000b\u0003\u0000\u00d9\u001f\u0001\u0000\u0000\u0000\u00da\u00dc"+
		"\b\u0001\u0000\u0000\u00db\u00da\u0001\u0000\u0000\u0000\u00dc\u00dd\u0001"+
		"\u0000\u0000\u0000\u00dd\u00db\u0001\u0000\u0000\u0000\u00dd\u00de\u0001"+
		"\u0000\u0000\u0000\u00de!\u0001\u0000\u0000\u0000\u00df\u00e0\u0005\u00ef"+
		"\u0000\u0000\u00e0\u00e1\u0005\u00bb\u0000\u0000\u00e1\u00e2\u0005\u00bf"+
		"\u0000\u0000\u00e2#\u0001\u0000\u0000\u0000\u00e3\u00e4\u0005\u8000\ufeff"+
		"\u0000\u0000\u00e4%\u0001\u0000\u0000\u0000\u00e5\u00e6\u0005>\u0000\u0000"+
		"\u00e6\u00e7\u0001\u0000\u0000\u0000\u00e7\u00e8\u0006\u000f\u0004\u0000"+
		"\u00e8\'\u0001\u0000\u0000\u0000\u00e9\u00ea\u0005[\u0000\u0000\u00ea"+
		"\u00eb\u0001\u0000\u0000\u0000\u00eb\u00ec\u0006\u0010\u0005\u0000\u00ec"+
		")\u0001\u0000\u0000\u0000\u00ed\u00ee\u0003X(\u0000\u00ee\u00ef\u0001"+
		"\u0000\u0000\u0000\u00ef\u00f0\u0006\u0011\u0000\u0000\u00f0+\u0001\u0000"+
		"\u0000\u0000\u00f1\u00f2\u0007\u0002\u0000\u0000\u00f2\u00f3\u0007\u0003"+
		"\u0000\u0000\u00f3\u00f4\u0007\u0004\u0000\u0000\u00f4\u00f5\u0007\u0005"+
		"\u0000\u0000\u00f5\u00f6\u0007\u0006\u0000\u0000\u00f6\u00f7\u0007\u0007"+
		"\u0000\u0000\u00f7\u00f8\u0007\b\u0000\u0000\u00f8-\u0001\u0000\u0000"+
		"\u0000\u00f9\u00fa\u0003d.\u0000\u00fa\u00fb\u0001\u0000\u0000\u0000\u00fb"+
		"\u00fc\u0006\u0013\u0006\u0000\u00fc/\u0001\u0000\u0000\u0000\u00fd\u00fe"+
		"\u0003b-\u0000\u00fe\u00ff\u0001\u0000\u0000\u0000\u00ff\u0100\u0006\u0014"+
		"\u0007\u0000\u01001\u0001\u0000\u0000\u0000\u0101\u0102\u0005]\u0000\u0000"+
		"\u0102\u0103\u0001\u0000\u0000\u0000\u0103\u0104\u0006\u0015\u0004\u0000"+
		"\u01043\u0001\u0000\u0000\u0000\u0105\u0106\u0005<\u0000\u0000\u0106\u0107"+
		"\u0005!\u0000\u0000\u0107\u0108\u0001\u0000\u0000\u0000\u0108\u0109\u0006"+
		"\u0016\b\u0000\u01095\u0001\u0000\u0000\u0000\u010a\u010b\u0003X(\u0000"+
		"\u010b\u010c\u0001\u0000\u0000\u0000\u010c\u010d\u0006\u0017\u0000\u0000"+
		"\u010d7\u0001\u0000\u0000\u0000\u010e\u010f\u0003\f\u0002\u0000\u010f"+
		"\u0110\u0001\u0000\u0000\u0000\u0110\u0111\u0006\u0018\t\u0000\u01119"+
		"\u0001\u0000\u0000\u0000\u0112\u0113\u0003\b\u0000\u0000\u0113\u0114\u0001"+
		"\u0000\u0000\u0000\u0114\u0115\u0006\u0019\n\u0000\u0115;\u0001\u0000"+
		"\u0000\u0000\u0116\u0117\u0005>\u0000\u0000\u0117\u0118\u0001\u0000\u0000"+
		"\u0000\u0118\u0119\u0006\u001a\u0004\u0000\u0119=\u0001\u0000\u0000\u0000"+
		"\u011a\u011b\u0005[\u0000\u0000\u011b\u011c\u0001\u0000\u0000\u0000\u011c"+
		"\u011d\u0006\u001b\u000b\u0000\u011d\u011e\u0006\u001b\f\u0000\u011e?"+
		"\u0001\u0000\u0000\u0000\u011f\u0120\u0003X(\u0000\u0120\u0121\u0001\u0000"+
		"\u0000\u0000\u0121\u0122\u0006\u001c\u0000\u0000\u0122A\u0001\u0000\u0000"+
		"\u0000\u0123\u0125\b\t\u0000\u0000\u0124\u0123\u0001\u0000\u0000\u0000"+
		"\u0125\u0126\u0001\u0000\u0000\u0000\u0126\u0124\u0001\u0000\u0000\u0000"+
		"\u0126\u0127\u0001\u0000\u0000\u0000\u0127C\u0001\u0000\u0000\u0000\u0128"+
		"\u0129\u0005]\u0000\u0000\u0129\u012a\u0001\u0000\u0000\u0000\u012a\u012b"+
		"\u0006\u001e\u0004\u0000\u012bE\u0001\u0000\u0000\u0000\u012c\u012d\t"+
		"\u0000\u0000\u0000\u012d\u012e\u0001\u0000\u0000\u0000\u012e\u012f\u0006"+
		"\u001f\u000b\u0000\u012fG\u0001\u0000\u0000\u0000\u0130\u0131\u0003T&"+
		"\u0000\u0131\u0132\u0001\u0000\u0000\u0000\u0132\u0133\u0006 \r\u0000"+
		"\u0133\u0134\u0006 \u0004\u0000\u0134I\u0001\u0000\u0000\u0000\u0135\u0136"+
		"\u0003X(\u0000\u0136\u0137\u0001\u0000\u0000\u0000\u0137\u0138\u0006!"+
		"\u0000\u0000\u0138K\u0001\u0000\u0000\u0000\u0139\u013a\u0003\u0016\u0007"+
		"\u0000\u013a\u013b\u0001\u0000\u0000\u0000\u013b\u013c\u0006\"\u000b\u0000"+
		"\u013c\u013d\u0006\"\u000e\u0000\u013dM\u0001\u0000\u0000\u0000\u013e"+
		"\u0140\b\n\u0000\u0000\u013f\u013e\u0001\u0000\u0000\u0000\u0140\u0141"+
		"\u0001\u0000\u0000\u0000\u0141\u013f\u0001\u0000\u0000\u0000\u0141\u0142"+
		"\u0001\u0000\u0000\u0000\u0142O\u0001\u0000\u0000\u0000\u0143\u0144\t"+
		"\u0000\u0000\u0000\u0144\u0145\u0001\u0000\u0000\u0000\u0145\u0146\u0006"+
		"$\u000b\u0000\u0146\u0147\u0006$\u0004\u0000\u0147Q\u0001\u0000\u0000"+
		"\u0000\u0148\u0149\u0005>\u0000\u0000\u0149\u014a\u0001\u0000\u0000\u0000"+
		"\u014a\u014b\u0006%\u0004\u0000\u014bS\u0001\u0000\u0000\u0000\u014c\u014d"+
		"\u0003\u0016\u0007\u0000\u014d\u014e\u0005>\u0000\u0000\u014e\u014f\u0001"+
		"\u0000\u0000\u0000\u014f\u0150\u0006&\u0004\u0000\u0150U\u0001\u0000\u0000"+
		"\u0000\u0151\u0152\u0005/\u0000\u0000\u0152\u0153\u0005>\u0000\u0000\u0153"+
		"\u0154\u0001\u0000\u0000\u0000\u0154\u0155\u0006\'\u0004\u0000\u0155W"+
		"\u0001\u0000\u0000\u0000\u0156\u0157\u0007\u000b\u0000\u0000\u0157\u0158"+
		"\u0001\u0000\u0000\u0000\u0158\u0159\u0006(\u0000\u0000\u0159Y\u0001\u0000"+
		"\u0000\u0000\u015a\u015b\u0005%\u0000\u0000\u015b\u015c\u0005@\u0000\u0000"+
		"\u015c[\u0001\u0000\u0000\u0000\u015d\u015e\u0005%\u0000\u0000\u015e]"+
		"\u0001\u0000\u0000\u0000\u015f\u0160\u0005/\u0000\u0000\u0160_\u0001\u0000"+
		"\u0000\u0000\u0161\u0162\u0005=\u0000\u0000\u0162a\u0001\u0000\u0000\u0000"+
		"\u0163\u0167\u0005\"\u0000\u0000\u0164\u0166\b\f\u0000\u0000\u0165\u0164"+
		"\u0001\u0000\u0000\u0000\u0166\u0169\u0001\u0000\u0000\u0000\u0167\u0165"+
		"\u0001\u0000\u0000\u0000\u0167\u0168\u0001\u0000\u0000\u0000\u0168\u016a"+
		"\u0001\u0000\u0000\u0000\u0169\u0167\u0001\u0000\u0000\u0000\u016a\u0174"+
		"\u0005\"\u0000\u0000\u016b\u016f\u0005\'\u0000\u0000\u016c\u016e\b\r\u0000"+
		"\u0000\u016d\u016c\u0001\u0000\u0000\u0000\u016e\u0171\u0001\u0000\u0000"+
		"\u0000\u016f\u016d\u0001\u0000\u0000\u0000\u016f\u0170\u0001\u0000\u0000"+
		"\u0000\u0170\u0172\u0001\u0000\u0000\u0000\u0171\u016f\u0001\u0000\u0000"+
		"\u0000\u0172\u0174\u0005\'\u0000\u0000\u0173\u0163\u0001\u0000\u0000\u0000"+
		"\u0173\u016b\u0001\u0000\u0000\u0000\u0174c\u0001\u0000\u0000\u0000\u0175"+
		"\u0179\u0003l2\u0000\u0176\u0178\u0003j1\u0000\u0177\u0176\u0001\u0000"+
		"\u0000\u0000\u0178\u017b\u0001\u0000\u0000\u0000\u0179\u0177\u0001\u0000"+
		"\u0000\u0000\u0179\u017a\u0001\u0000\u0000\u0000\u017ae\u0001\u0000\u0000"+
		"\u0000\u017b\u0179\u0001\u0000\u0000\u0000\u017c\u017d\u0007\u000e\u0000"+
		"\u0000\u017dg\u0001\u0000\u0000\u0000\u017e\u017f\u0007\u000f\u0000\u0000"+
		"\u017fi\u0001\u0000\u0000\u0000\u0180\u0185\u0003l2\u0000\u0181\u0185"+
		"\u0007\u0010\u0000\u0000\u0182\u0185\u0003h0\u0000\u0183\u0185\u0007\u0011"+
		"\u0000\u0000\u0184\u0180\u0001\u0000\u0000\u0000\u0184\u0181\u0001\u0000"+
		"\u0000\u0000\u0184\u0182\u0001\u0000\u0000\u0000\u0184\u0183\u0001\u0000"+
		"\u0000\u0000\u0185k\u0001\u0000\u0000\u0000\u0186\u0188\u0007\u0012\u0000"+
		"\u0000\u0187\u0186\u0001\u0000\u0000\u0000\u0188m\u0001\u0000\u0000\u0000"+
		"\u001a\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007v\u008a\u009f\u00aa"+
		"\u00ae\u00b2\u00b5\u00b7\u00bd\u00dd\u0126\u0141\u0167\u016f\u0173\u0179"+
		"\u0184\u0187\u000f\u0006\u0000\u0000\u0005\u0007\u0000\u0005\u0005\u0000"+
		"\u0005\u0001\u0000\u0004\u0000\u0000\u0005\u0002\u0000\u0007$\u0000\u0007"+
		"#\u0000\u0005\u0003\u0000\u0007\u0003\u0000\u0007\u0001\u0000\u0003\u0000"+
		"\u0000\u0005\u0004\u0000\u0007\u001c\u0000\u0005\u0006\u0000";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
