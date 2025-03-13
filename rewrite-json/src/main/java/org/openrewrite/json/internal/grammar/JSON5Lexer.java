/*
 * Copyright 2025 the original author or authors.
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
// Generated from ~/git/rewrite/rewrite-json/src/main/antlr/JSON5.g4 by ANTLR 4.13.2
package org.openrewrite.json.internal.grammar;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class JSON5Lexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, SINGLE_LINE_COMMENT=7, 
		MULTI_LINE_COMMENT=8, LITERAL=9, STRING=10, NUMBER=11, NUMERIC_LITERAL=12, 
		SYMBOL=13, IDENTIFIER=14, WS=15, UTF_8_BOM=16;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "SINGLE_LINE_COMMENT", 
			"MULTI_LINE_COMMENT", "LITERAL", "STRING", "DOUBLE_QUOTE_CHAR", "SINGLE_QUOTE_CHAR", 
			"ESCAPE_SEQUENCE", "NUMBER", "NUMERIC_LITERAL", "SYMBOL", "HEX", "INT", 
			"EXP", "IDENTIFIER", "IDENTIFIER_START", "IDENTIFIER_PART", "UNICODE_SEQUENCE", 
			"NEWLINE", "WS", "UTF_8_BOM"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'{'", "','", "'}'", "':'", "'['", "']'", null, null, null, null, 
			null, null, null, null, null, "'\\uFEFF'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, "SINGLE_LINE_COMMENT", "MULTI_LINE_COMMENT", 
			"LITERAL", "STRING", "NUMBER", "NUMERIC_LITERAL", "SYMBOL", "IDENTIFIER", 
			"WS", "UTF_8_BOM"
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


	public JSON5Lexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "JSON5.g4"; }

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
		"\u0004\u0000\u0010\u00fd\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002"+
		"\u0001\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002"+
		"\u0004\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002"+
		"\u0007\u0007\u0007\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002"+
		"\u000b\u0007\u000b\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e"+
		"\u0002\u000f\u0007\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011"+
		"\u0002\u0012\u0007\u0012\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014"+
		"\u0002\u0015\u0007\u0015\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017"+
		"\u0002\u0018\u0007\u0018\u0002\u0019\u0007\u0019\u0001\u0000\u0001\u0000"+
		"\u0001\u0001\u0001\u0001\u0001\u0002\u0001\u0002\u0001\u0003\u0001\u0003"+
		"\u0001\u0004\u0001\u0004\u0001\u0005\u0001\u0005\u0001\u0006\u0001\u0006"+
		"\u0001\u0006\u0001\u0006\u0005\u0006F\b\u0006\n\u0006\f\u0006I\t\u0006"+
		"\u0001\u0006\u0001\u0006\u0003\u0006M\b\u0006\u0001\u0006\u0001\u0006"+
		"\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0005\u0007U\b\u0007"+
		"\n\u0007\f\u0007X\t\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007"+
		"\u0001\u0007\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001"+
		"\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0003\bl\b\b\u0001\t\u0001"+
		"\t\u0005\tp\b\t\n\t\f\ts\t\t\u0001\t\u0001\t\u0001\t\u0005\tx\b\t\n\t"+
		"\f\t{\t\t\u0001\t\u0003\t~\b\t\u0001\n\u0001\n\u0003\n\u0082\b\n\u0001"+
		"\u000b\u0001\u000b\u0003\u000b\u0086\b\u000b\u0001\f\u0001\f\u0001\f\u0001"+
		"\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0003\f\u0092\b\f\u0001"+
		"\r\u0001\r\u0001\r\u0005\r\u0097\b\r\n\r\f\r\u009a\t\r\u0003\r\u009c\b"+
		"\r\u0001\r\u0003\r\u009f\b\r\u0001\r\u0001\r\u0004\r\u00a3\b\r\u000b\r"+
		"\f\r\u00a4\u0001\r\u0003\r\u00a8\b\r\u0001\r\u0001\r\u0001\r\u0004\r\u00ad"+
		"\b\r\u000b\r\f\r\u00ae\u0003\r\u00b1\b\r\u0001\u000e\u0001\u000e\u0001"+
		"\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001"+
		"\u000e\u0001\u000e\u0001\u000e\u0003\u000e\u00be\b\u000e\u0001\u000f\u0001"+
		"\u000f\u0001\u0010\u0001\u0010\u0001\u0011\u0001\u0011\u0001\u0011\u0005"+
		"\u0011\u00c7\b\u0011\n\u0011\f\u0011\u00ca\t\u0011\u0003\u0011\u00cc\b"+
		"\u0011\u0001\u0012\u0001\u0012\u0003\u0012\u00d0\b\u0012\u0001\u0012\u0005"+
		"\u0012\u00d3\b\u0012\n\u0012\f\u0012\u00d6\t\u0012\u0001\u0013\u0001\u0013"+
		"\u0005\u0013\u00da\b\u0013\n\u0013\f\u0013\u00dd\t\u0013\u0001\u0014\u0001"+
		"\u0014\u0001\u0014\u0003\u0014\u00e2\b\u0014\u0001\u0015\u0001\u0015\u0003"+
		"\u0015\u00e6\b\u0015\u0001\u0016\u0001\u0016\u0001\u0016\u0001\u0016\u0001"+
		"\u0016\u0001\u0016\u0001\u0017\u0001\u0017\u0001\u0017\u0003\u0017\u00f1"+
		"\b\u0017\u0001\u0018\u0004\u0018\u00f4\b\u0018\u000b\u0018\f\u0018\u00f5"+
		"\u0001\u0018\u0001\u0018\u0001\u0019\u0001\u0019\u0001\u0019\u0001\u0019"+
		"\u0002GV\u0000\u001a\u0001\u0001\u0003\u0002\u0005\u0003\u0007\u0004\t"+
		"\u0005\u000b\u0006\r\u0007\u000f\b\u0011\t\u0013\n\u0015\u0000\u0017\u0000"+
		"\u0019\u0000\u001b\u000b\u001d\f\u001f\r!\u0000#\u0000%\u0000\'\u000e"+
		")\u0000+\u0000-\u0000/\u00001\u000f3\u0010\u0001\u0000\u000e\u0004\u0000"+
		"\n\n\r\r\"\"\\\\\u0004\u0000\n\n\r\r\'\'\\\\\n\u0000\"\"\'\'//\\\\bbf"+
		"fnnrrttvv\f\u0000\n\n\r\r\"\"\'\'09\\\\bbffnnrrtvxx\u0001\u000009\u0002"+
		"\u0000XXxx\u0002\u0000++--\u0003\u000009AFaf\u0001\u000019\u0002\u0000"+
		"EEee\u0295\u0000$$AZ__az\u00aa\u00aa\u00b5\u00b5\u00ba\u00ba\u00c0\u00d6"+
		"\u00d8\u00f6\u00f8\u02c1\u02c6\u02d1\u02e0\u02e4\u02ec\u02ec\u02ee\u02ee"+
		"\u0370\u0374\u0376\u0377\u037a\u037d\u037f\u037f\u0386\u0386\u0388\u038a"+
		"\u038c\u038c\u038e\u03a1\u03a3\u03f5\u03f7\u0481\u048a\u052f\u0531\u0556"+
		"\u0559\u0559\u0560\u0588\u05d0\u05ea\u05ef\u05f2\u0620\u064a\u066e\u066f"+
		"\u0671\u06d3\u06d5\u06d5\u06e5\u06e6\u06ee\u06ef\u06fa\u06fc\u06ff\u06ff"+
		"\u0710\u0710\u0712\u072f\u074d\u07a5\u07b1\u07b1\u07ca\u07ea\u07f4\u07f5"+
		"\u07fa\u07fa\u0800\u0815\u081a\u081a\u0824\u0824\u0828\u0828\u0840\u0858"+
		"\u0860\u086a\u0870\u0887\u0889\u088e\u08a0\u08c9\u0904\u0939\u093d\u093d"+
		"\u0950\u0950\u0958\u0961\u0971\u0980\u0985\u098c\u098f\u0990\u0993\u09a8"+
		"\u09aa\u09b0\u09b2\u09b2\u09b6\u09b9\u09bd\u09bd\u09ce\u09ce\u09dc\u09dd"+
		"\u09df\u09e1\u09f0\u09f1\u09fc\u09fc\u0a05\u0a0a\u0a0f\u0a10\u0a13\u0a28"+
		"\u0a2a\u0a30\u0a32\u0a33\u0a35\u0a36\u0a38\u0a39\u0a59\u0a5c\u0a5e\u0a5e"+
		"\u0a72\u0a74\u0a85\u0a8d\u0a8f\u0a91\u0a93\u0aa8\u0aaa\u0ab0\u0ab2\u0ab3"+
		"\u0ab5\u0ab9\u0abd\u0abd\u0ad0\u0ad0\u0ae0\u0ae1\u0af9\u0af9\u0b05\u0b0c"+
		"\u0b0f\u0b10\u0b13\u0b28\u0b2a\u0b30\u0b32\u0b33\u0b35\u0b39\u0b3d\u0b3d"+
		"\u0b5c\u0b5d\u0b5f\u0b61\u0b71\u0b71\u0b83\u0b83\u0b85\u0b8a\u0b8e\u0b90"+
		"\u0b92\u0b95\u0b99\u0b9a\u0b9c\u0b9c\u0b9e\u0b9f\u0ba3\u0ba4\u0ba8\u0baa"+
		"\u0bae\u0bb9\u0bd0\u0bd0\u0c05\u0c0c\u0c0e\u0c10\u0c12\u0c28\u0c2a\u0c39"+
		"\u0c3d\u0c3d\u0c58\u0c5a\u0c5d\u0c5d\u0c60\u0c61\u0c80\u0c80\u0c85\u0c8c"+
		"\u0c8e\u0c90\u0c92\u0ca8\u0caa\u0cb3\u0cb5\u0cb9\u0cbd\u0cbd\u0cdd\u0cde"+
		"\u0ce0\u0ce1\u0cf1\u0cf2\u0d04\u0d0c\u0d0e\u0d10\u0d12\u0d3a\u0d3d\u0d3d"+
		"\u0d4e\u0d4e\u0d54\u0d56\u0d5f\u0d61\u0d7a\u0d7f\u0d85\u0d96\u0d9a\u0db1"+
		"\u0db3\u0dbb\u0dbd\u0dbd\u0dc0\u0dc6\u0e01\u0e30\u0e32\u0e33\u0e40\u0e46"+
		"\u0e81\u0e82\u0e84\u0e84\u0e86\u0e8a\u0e8c\u0ea3\u0ea5\u0ea5\u0ea7\u0eb0"+
		"\u0eb2\u0eb3\u0ebd\u0ebd\u0ec0\u0ec4\u0ec6\u0ec6\u0edc\u0edf\u0f00\u0f00"+
		"\u0f40\u0f47\u0f49\u0f6c\u0f88\u0f8c\u1000\u102a\u103f\u103f\u1050\u1055"+
		"\u105a\u105d\u1061\u1061\u1065\u1066\u106e\u1070\u1075\u1081\u108e\u108e"+
		"\u10a0\u10c5\u10c7\u10c7\u10cd\u10cd\u10d0\u10fa\u10fc\u1248\u124a\u124d"+
		"\u1250\u1256\u1258\u1258\u125a\u125d\u1260\u1288\u128a\u128d\u1290\u12b0"+
		"\u12b2\u12b5\u12b8\u12be\u12c0\u12c0\u12c2\u12c5\u12c8\u12d6\u12d8\u1310"+
		"\u1312\u1315\u1318\u135a\u1380\u138f\u13a0\u13f5\u13f8\u13fd\u1401\u166c"+
		"\u166f\u167f\u1681\u169a\u16a0\u16ea\u16f1\u16f8\u1700\u1711\u171f\u1731"+
		"\u1740\u1751\u1760\u176c\u176e\u1770\u1780\u17b3\u17d7\u17d7\u17dc\u17dc"+
		"\u1820\u1878\u1880\u1884\u1887\u18a8\u18aa\u18aa\u18b0\u18f5\u1900\u191e"+
		"\u1950\u196d\u1970\u1974\u1980\u19ab\u19b0\u19c9\u1a00\u1a16\u1a20\u1a54"+
		"\u1aa7\u1aa7\u1b05\u1b33\u1b45\u1b4c\u1b83\u1ba0\u1bae\u1baf\u1bba\u1be5"+
		"\u1c00\u1c23\u1c4d\u1c4f\u1c5a\u1c7d\u1c80\u1c88\u1c90\u1cba\u1cbd\u1cbf"+
		"\u1ce9\u1cec\u1cee\u1cf3\u1cf5\u1cf6\u1cfa\u1cfa\u1d00\u1dbf\u1e00\u1f15"+
		"\u1f18\u1f1d\u1f20\u1f45\u1f48\u1f4d\u1f50\u1f57\u1f59\u1f59\u1f5b\u1f5b"+
		"\u1f5d\u1f5d\u1f5f\u1f7d\u1f80\u1fb4\u1fb6\u1fbc\u1fbe\u1fbe\u1fc2\u1fc4"+
		"\u1fc6\u1fcc\u1fd0\u1fd3\u1fd6\u1fdb\u1fe0\u1fec\u1ff2\u1ff4\u1ff6\u1ffc"+
		"\u2071\u2071\u207f\u207f\u2090\u209c\u2102\u2102\u2107\u2107\u210a\u2113"+
		"\u2115\u2115\u2119\u211d\u2124\u2124\u2126\u2126\u2128\u2128\u212a\u212d"+
		"\u212f\u2139\u213c\u213f\u2145\u2149\u214e\u214e\u2183\u2184\u2c00\u2ce4"+
		"\u2ceb\u2cee\u2cf2\u2cf3\u2d00\u2d25\u2d27\u2d27\u2d2d\u2d2d\u2d30\u2d67"+
		"\u2d6f\u2d6f\u2d80\u2d96\u2da0\u2da6\u2da8\u2dae\u2db0\u2db6\u2db8\u2dbe"+
		"\u2dc0\u2dc6\u2dc8\u2dce\u2dd0\u2dd6\u2dd8\u2dde\u2e2f\u2e2f\u3005\u3006"+
		"\u3031\u3035\u303b\u303c\u3041\u3096\u309d\u309f\u30a1\u30fa\u30fc\u30ff"+
		"\u3105\u312f\u3131\u318e\u31a0\u31bf\u31f0\u31ff\u3400\u4dbf\u4e00\u8000"+
		"\ua48c\u8000\ua4d0\u8000\ua4fd\u8000\ua500\u8000\ua60c\u8000\ua610\u8000"+
		"\ua61f\u8000\ua62a\u8000\ua62b\u8000\ua640\u8000\ua66e\u8000\ua67f\u8000"+
		"\ua69d\u8000\ua6a0\u8000\ua6e5\u8000\ua717\u8000\ua71f\u8000\ua722\u8000"+
		"\ua788\u8000\ua78b\u8000\ua7ca\u8000\ua7d0\u8000\ua7d1\u8000\ua7d3\u8000"+
		"\ua7d3\u8000\ua7d5\u8000\ua7d9\u8000\ua7f2\u8000\ua801\u8000\ua803\u8000"+
		"\ua805\u8000\ua807\u8000\ua80a\u8000\ua80c\u8000\ua822\u8000\ua840\u8000"+
		"\ua873\u8000\ua882\u8000\ua8b3\u8000\ua8f2\u8000\ua8f7\u8000\ua8fb\u8000"+
		"\ua8fb\u8000\ua8fd\u8000\ua8fe\u8000\ua90a\u8000\ua925\u8000\ua930\u8000"+
		"\ua946\u8000\ua960\u8000\ua97c\u8000\ua984\u8000\ua9b2\u8000\ua9cf\u8000"+
		"\ua9cf\u8000\ua9e0\u8000\ua9e4\u8000\ua9e6\u8000\ua9ef\u8000\ua9fa\u8000"+
		"\ua9fe\u8000\uaa00\u8000\uaa28\u8000\uaa40\u8000\uaa42\u8000\uaa44\u8000"+
		"\uaa4b\u8000\uaa60\u8000\uaa76\u8000\uaa7a\u8000\uaa7a\u8000\uaa7e\u8000"+
		"\uaaaf\u8000\uaab1\u8000\uaab1\u8000\uaab5\u8000\uaab6\u8000\uaab9\u8000"+
		"\uaabd\u8000\uaac0\u8000\uaac0\u8000\uaac2\u8000\uaac2\u8000\uaadb\u8000"+
		"\uaadd\u8000\uaae0\u8000\uaaea\u8000\uaaf2\u8000\uaaf4\u8000\uab01\u8000"+
		"\uab06\u8000\uab09\u8000\uab0e\u8000\uab11\u8000\uab16\u8000\uab20\u8000"+
		"\uab26\u8000\uab28\u8000\uab2e\u8000\uab30\u8000\uab5a\u8000\uab5c\u8000"+
		"\uab69\u8000\uab70\u8000\uabe2\u8000\uac00\u8000\ud7a3\u8000\ud7b0\u8000"+
		"\ud7c6\u8000\ud7cb\u8000\ud7fb\u8000\uf900\u8000\ufa6d\u8000\ufa70\u8000"+
		"\ufad9\u8000\ufb00\u8000\ufb06\u8000\ufb13\u8000\ufb17\u8000\ufb1d\u8000"+
		"\ufb1d\u8000\ufb1f\u8000\ufb28\u8000\ufb2a\u8000\ufb36\u8000\ufb38\u8000"+
		"\ufb3c\u8000\ufb3e\u8000\ufb3e\u8000\ufb40\u8000\ufb41\u8000\ufb43\u8000"+
		"\ufb44\u8000\ufb46\u8000\ufbb1\u8000\ufbd3\u8000\ufd3d\u8000\ufd50\u8000"+
		"\ufd8f\u8000\ufd92\u8000\ufdc7\u8000\ufdf0\u8000\ufdfb\u8000\ufe70\u8000"+
		"\ufe74\u8000\ufe76\u8000\ufefc\u8000\uff21\u8000\uff3a\u8000\uff41\u8000"+
		"\uff5a\u8000\uff66\u8000\uffbe\u8000\uffc2\u8000\uffc7\u8000\uffca\u8000"+
		"\uffcf\u8000\uffd2\u8000\uffd7\u8000\uffda\u8000\uffdc\u8001\u0000\u8001"+
		"\u000b\u8001\r\u8001&\u8001(\u8001:\u8001<\u8001=\u8001?\u8001M\u8001"+
		"P\u8001]\u8001\u0080\u8001\u00fa\u8001\u0280\u8001\u029c\u8001\u02a0\u8001"+
		"\u02d0\u8001\u0300\u8001\u031f\u8001\u032d\u8001\u0340\u8001\u0342\u8001"+
		"\u0349\u8001\u0350\u8001\u0375\u8001\u0380\u8001\u039d\u8001\u03a0\u8001"+
		"\u03c3\u8001\u03c8\u8001\u03cf\u8001\u0400\u8001\u049d\u8001\u04b0\u8001"+
		"\u04d3\u8001\u04d8\u8001\u04fb\u8001\u0500\u8001\u0527\u8001\u0530\u8001"+
		"\u0563\u8001\u0570\u8001\u057a\u8001\u057c\u8001\u058a\u8001\u058c\u8001"+
		"\u0592\u8001\u0594\u8001\u0595\u8001\u0597\u8001\u05a1\u8001\u05a3\u8001"+
		"\u05b1\u8001\u05b3\u8001\u05b9\u8001\u05bb\u8001\u05bc\u8001\u0600\u8001"+
		"\u0736\u8001\u0740\u8001\u0755\u8001\u0760\u8001\u0767\u8001\u0780\u8001"+
		"\u0785\u8001\u0787\u8001\u07b0\u8001\u07b2\u8001\u07ba\u8001\u0800\u8001"+
		"\u0805\u8001\u0808\u8001\u0808\u8001\u080a\u8001\u0835\u8001\u0837\u8001"+
		"\u0838\u8001\u083c\u8001\u083c\u8001\u083f\u8001\u0855\u8001\u0860\u8001"+
		"\u0876\u8001\u0880\u8001\u089e\u8001\u08e0\u8001\u08f2\u8001\u08f4\u8001"+
		"\u08f5\u8001\u0900\u8001\u0915\u8001\u0920\u8001\u0939\u8001\u0980\u8001"+
		"\u09b7\u8001\u09be\u8001\u09bf\u8001\u0a00\u8001\u0a00\u8001\u0a10\u8001"+
		"\u0a13\u8001\u0a15\u8001\u0a17\u8001\u0a19\u8001\u0a35\u8001\u0a60\u8001"+
		"\u0a7c\u8001\u0a80\u8001\u0a9c\u8001\u0ac0\u8001\u0ac7\u8001\u0ac9\u8001"+
		"\u0ae4\u8001\u0b00\u8001\u0b35\u8001\u0b40\u8001\u0b55\u8001\u0b60\u8001"+
		"\u0b72\u8001\u0b80\u8001\u0b91\u8001\u0c00\u8001\u0c48\u8001\u0c80\u8001"+
		"\u0cb2\u8001\u0cc0\u8001\u0cf2\u8001\u0d00\u8001\u0d23\u8001\u0e80\u8001"+
		"\u0ea9\u8001\u0eb0\u8001\u0eb1\u8001\u0f00\u8001\u0f1c\u8001\u0f27\u8001"+
		"\u0f27\u8001\u0f30\u8001\u0f45\u8001\u0f70\u8001\u0f81\u8001\u0fb0\u8001"+
		"\u0fc4\u8001\u0fe0\u8001\u0ff6\u8001\u1003\u8001\u1037\u8001\u1071\u8001"+
		"\u1072\u8001\u1075\u8001\u1075\u8001\u1083\u8001\u10af\u8001\u10d0\u8001"+
		"\u10e8\u8001\u1103\u8001\u1126\u8001\u1144\u8001\u1144\u8001\u1147\u8001"+
		"\u1147\u8001\u1150\u8001\u1172\u8001\u1176\u8001\u1176\u8001\u1183\u8001"+
		"\u11b2\u8001\u11c1\u8001\u11c4\u8001\u11da\u8001\u11da\u8001\u11dc\u8001"+
		"\u11dc\u8001\u1200\u8001\u1211\u8001\u1213\u8001\u122b\u8001\u123f\u8001"+
		"\u1240\u8001\u1280\u8001\u1286\u8001\u1288\u8001\u1288\u8001\u128a\u8001"+
		"\u128d\u8001\u128f\u8001\u129d\u8001\u129f\u8001\u12a8\u8001\u12b0\u8001"+
		"\u12de\u8001\u1305\u8001\u130c\u8001\u130f\u8001\u1310\u8001\u1313\u8001"+
		"\u1328\u8001\u132a\u8001\u1330\u8001\u1332\u8001\u1333\u8001\u1335\u8001"+
		"\u1339\u8001\u133d\u8001\u133d\u8001\u1350\u8001\u1350\u8001\u135d\u8001"+
		"\u1361\u8001\u1400\u8001\u1434\u8001\u1447\u8001\u144a\u8001\u145f\u8001"+
		"\u1461\u8001\u1480\u8001\u14af\u8001\u14c4\u8001\u14c5\u8001\u14c7\u8001"+
		"\u14c7\u8001\u1580\u8001\u15ae\u8001\u15d8\u8001\u15db\u8001\u1600\u8001"+
		"\u162f\u8001\u1644\u8001\u1644\u8001\u1680\u8001\u16aa\u8001\u16b8\u8001"+
		"\u16b8\u8001\u1700\u8001\u171a\u8001\u1740\u8001\u1746\u8001\u1800\u8001"+
		"\u182b\u8001\u18a0\u8001\u18df\u8001\u18ff\u8001\u1906\u8001\u1909\u8001"+
		"\u1909\u8001\u190c\u8001\u1913\u8001\u1915\u8001\u1916\u8001\u1918\u8001"+
		"\u192f\u8001\u193f\u8001\u193f\u8001\u1941\u8001\u1941\u8001\u19a0\u8001"+
		"\u19a7\u8001\u19aa\u8001\u19d0\u8001\u19e1\u8001\u19e1\u8001\u19e3\u8001"+
		"\u19e3\u8001\u1a00\u8001\u1a00\u8001\u1a0b\u8001\u1a32\u8001\u1a3a\u8001"+
		"\u1a3a\u8001\u1a50\u8001\u1a50\u8001\u1a5c\u8001\u1a89\u8001\u1a9d\u8001"+
		"\u1a9d\u8001\u1ab0\u8001\u1af8\u8001\u1c00\u8001\u1c08\u8001\u1c0a\u8001"+
		"\u1c2e\u8001\u1c40\u8001\u1c40\u8001\u1c72\u8001\u1c8f\u8001\u1d00\u8001"+
		"\u1d06\u8001\u1d08\u8001\u1d09\u8001\u1d0b\u8001\u1d30\u8001\u1d46\u8001"+
		"\u1d46\u8001\u1d60\u8001\u1d65\u8001\u1d67\u8001\u1d68\u8001\u1d6a\u8001"+
		"\u1d89\u8001\u1d98\u8001\u1d98\u8001\u1ee0\u8001\u1ef2\u8001\u1f02\u8001"+
		"\u1f02\u8001\u1f04\u8001\u1f10\u8001\u1f12\u8001\u1f33\u8001\u1fb0\u8001"+
		"\u1fb0\u8001\u2000\u8001\u2399\u8001\u2480\u8001\u2543\u8001\u2f90\u8001"+
		"\u2ff0\u8001\u3000\u8001\u342f\u8001\u3441\u8001\u3446\u8001\u4400\u8001"+
		"\u4646\u8001\u6800\u8001\u6a38\u8001\u6a40\u8001\u6a5e\u8001\u6a70\u8001"+
		"\u6abe\u8001\u6ad0\u8001\u6aed\u8001\u6b00\u8001\u6b2f\u8001\u6b40\u8001"+
		"\u6b43\u8001\u6b63\u8001\u6b77\u8001\u6b7d\u8001\u6b8f\u8001\u6e40\u8001"+
		"\u6e7f\u8001\u6f00\u8001\u6f4a\u8001\u6f50\u8001\u6f50\u8001\u6f93\u8001"+
		"\u6f9f\u8001\u6fe0\u8001\u6fe1\u8001\u6fe3\u8001\u6fe3\u8001\u7000\u8001"+
		"\u87f7\u8001\u8800\u8001\u8cd5\u8001\u8d00\u8001\u8d08\u8001\uaff0\u8001"+
		"\uaff3\u8001\uaff5\u8001\uaffb\u8001\uaffd\u8001\uaffe\u8001\ub000\u8001"+
		"\ub122\u8001\ub132\u8001\ub132\u8001\ub150\u8001\ub152\u8001\ub155\u8001"+
		"\ub155\u8001\ub164\u8001\ub167\u8001\ub170\u8001\ub2fb\u8001\ubc00\u8001"+
		"\ubc6a\u8001\ubc70\u8001\ubc7c\u8001\ubc80\u8001\ubc88\u8001\ubc90\u8001"+
		"\ubc99\u8001\ud400\u8001\ud454\u8001\ud456\u8001\ud49c\u8001\ud49e\u8001"+
		"\ud49f\u8001\ud4a2\u8001\ud4a2\u8001\ud4a5\u8001\ud4a6\u8001\ud4a9\u8001"+
		"\ud4ac\u8001\ud4ae\u8001\ud4b9\u8001\ud4bb\u8001\ud4bb\u8001\ud4bd\u8001"+
		"\ud4c3\u8001\ud4c5\u8001\ud505\u8001\ud507\u8001\ud50a\u8001\ud50d\u8001"+
		"\ud514\u8001\ud516\u8001\ud51c\u8001\ud51e\u8001\ud539\u8001\ud53b\u8001"+
		"\ud53e\u8001\ud540\u8001\ud544\u8001\ud546\u8001\ud546\u8001\ud54a\u8001"+
		"\ud550\u8001\ud552\u8001\ud6a5\u8001\ud6a8\u8001\ud6c0\u8001\ud6c2\u8001"+
		"\ud6da\u8001\ud6dc\u8001\ud6fa\u8001\ud6fc\u8001\ud714\u8001\ud716\u8001"+
		"\ud734\u8001\ud736\u8001\ud74e\u8001\ud750\u8001\ud76e\u8001\ud770\u8001"+
		"\ud788\u8001\ud78a\u8001\ud7a8\u8001\ud7aa\u8001\ud7c2\u8001\ud7c4\u8001"+
		"\ud7cb\u8001\udf00\u8001\udf1e\u8001\udf25\u8001\udf2a\u8001\ue030\u8001"+
		"\ue06d\u8001\ue100\u8001\ue12c\u8001\ue137\u8001\ue13d\u8001\ue14e\u8001"+
		"\ue14e\u8001\ue290\u8001\ue2ad\u8001\ue2c0\u8001\ue2eb\u8001\ue4d0\u8001"+
		"\ue4eb\u8001\ue7e0\u8001\ue7e6\u8001\ue7e8\u8001\ue7eb\u8001\ue7ed\u8001"+
		"\ue7ee\u8001\ue7f0\u8001\ue7fe\u8001\ue800\u8001\ue8c4\u8001\ue900\u8001"+
		"\ue943\u8001\ue94b\u8001\ue94b\u8001\uee00\u8001\uee03\u8001\uee05\u8001"+
		"\uee1f\u8001\uee21\u8001\uee22\u8001\uee24\u8001\uee24\u8001\uee27\u8001"+
		"\uee27\u8001\uee29\u8001\uee32\u8001\uee34\u8001\uee37\u8001\uee39\u8001"+
		"\uee39\u8001\uee3b\u8001\uee3b\u8001\uee42\u8001\uee42\u8001\uee47\u8001"+
		"\uee47\u8001\uee49\u8001\uee49\u8001\uee4b\u8001\uee4b\u8001\uee4d\u8001"+
		"\uee4f\u8001\uee51\u8001\uee52\u8001\uee54\u8001\uee54\u8001\uee57\u8001"+
		"\uee57\u8001\uee59\u8001\uee59\u8001\uee5b\u8001\uee5b\u8001\uee5d\u8001"+
		"\uee5d\u8001\uee5f\u8001\uee5f\u8001\uee61\u8001\uee62\u8001\uee64\u8001"+
		"\uee64\u8001\uee67\u8001\uee6a\u8001\uee6c\u8001\uee72\u8001\uee74\u8001"+
		"\uee77\u8001\uee79\u8001\uee7c\u8001\uee7e\u8001\uee7e\u8001\uee80\u8001"+
		"\uee89\u8001\uee8b\u8001\uee9b\u8001\ueea1\u8001\ueea3\u8001\ueea5\u8001"+
		"\ueea9\u8001\ueeab\u8001\ueebb\u8002\u0000\u8002\ua6df\u8002\ua700\u8002"+
		"\ub739\u8002\ub740\u8002\ub81d\u8002\ub820\u8002\ucea1\u8002\uceb0\u8002"+
		"\uebe0\u8002\uf800\u8002\ufa1d\u8003\u0000\u8003\u134a\u8003\u1350\u8003"+
		"\u23af\u01b4\u000009__\u00b2\u00b3\u00b9\u00b9\u00bc\u00be\u0300\u036f"+
		"\u0483\u0489\u0591\u05bd\u05bf\u05bf\u05c1\u05c2\u05c4\u05c5\u05c7\u05c7"+
		"\u0610\u061a\u064b\u0669\u0670\u0670\u06d6\u06dc\u06df\u06e4\u06e7\u06e8"+
		"\u06ea\u06ed\u06f0\u06f9\u0711\u0711\u0730\u074a\u07a6\u07b0\u07c0\u07c9"+
		"\u07eb\u07f3\u07fd\u07fd\u0816\u0819\u081b\u0823\u0825\u0827\u0829\u082d"+
		"\u0859\u085b\u0898\u089f\u08ca\u08e1\u08e3\u0903\u093a\u093c\u093e\u094f"+
		"\u0951\u0957\u0962\u0963\u0966\u096f\u0981\u0983\u09bc\u09bc\u09be\u09c4"+
		"\u09c7\u09c8\u09cb\u09cd\u09d7\u09d7\u09e2\u09e3\u09e6\u09ef\u09f4\u09f9"+
		"\u09fe\u09fe\u0a01\u0a03\u0a3c\u0a3c\u0a3e\u0a42\u0a47\u0a48\u0a4b\u0a4d"+
		"\u0a51\u0a51\u0a66\u0a71\u0a75\u0a75\u0a81\u0a83\u0abc\u0abc\u0abe\u0ac5"+
		"\u0ac7\u0ac9\u0acb\u0acd\u0ae2\u0ae3\u0ae6\u0aef\u0afa\u0aff\u0b01\u0b03"+
		"\u0b3c\u0b3c\u0b3e\u0b44\u0b47\u0b48\u0b4b\u0b4d\u0b55\u0b57\u0b62\u0b63"+
		"\u0b66\u0b6f\u0b72\u0b77\u0b82\u0b82\u0bbe\u0bc2\u0bc6\u0bc8\u0bca\u0bcd"+
		"\u0bd7\u0bd7\u0be6\u0bf2\u0c00\u0c04\u0c3c\u0c3c\u0c3e\u0c44\u0c46\u0c48"+
		"\u0c4a\u0c4d\u0c55\u0c56\u0c62\u0c63\u0c66\u0c6f\u0c78\u0c7e\u0c81\u0c83"+
		"\u0cbc\u0cbc\u0cbe\u0cc4\u0cc6\u0cc8\u0cca\u0ccd\u0cd5\u0cd6\u0ce2\u0ce3"+
		"\u0ce6\u0cef\u0cf3\u0cf3\u0d00\u0d03\u0d3b\u0d3c\u0d3e\u0d44\u0d46\u0d48"+
		"\u0d4a\u0d4d\u0d57\u0d5e\u0d62\u0d63\u0d66\u0d78\u0d81\u0d83\u0dca\u0dca"+
		"\u0dcf\u0dd4\u0dd6\u0dd6\u0dd8\u0ddf\u0de6\u0def\u0df2\u0df3\u0e31\u0e31"+
		"\u0e34\u0e3a\u0e47\u0e4e\u0e50\u0e59\u0eb1\u0eb1\u0eb4\u0ebc\u0ec8\u0ece"+
		"\u0ed0\u0ed9\u0f18\u0f19\u0f20\u0f33\u0f35\u0f35\u0f37\u0f37\u0f39\u0f39"+
		"\u0f3e\u0f3f\u0f71\u0f84\u0f86\u0f87\u0f8d\u0f97\u0f99\u0fbc\u0fc6\u0fc6"+
		"\u102b\u103e\u1040\u1049\u1056\u1059\u105e\u1060\u1062\u1064\u1067\u106d"+
		"\u1071\u1074\u1082\u108d\u108f\u109d\u135d\u135f\u1369\u137c\u16ee\u16f0"+
		"\u1712\u1715\u1732\u1734\u1752\u1753\u1772\u1773\u17b4\u17d3\u17dd\u17dd"+
		"\u17e0\u17e9\u17f0\u17f9\u180b\u180d\u180f\u1819\u1885\u1886\u18a9\u18a9"+
		"\u1920\u192b\u1930\u193b\u1946\u194f\u19d0\u19da\u1a17\u1a1b\u1a55\u1a5e"+
		"\u1a60\u1a7c\u1a7f\u1a89\u1a90\u1a99\u1ab0\u1ace\u1b00\u1b04\u1b34\u1b44"+
		"\u1b50\u1b59\u1b6b\u1b73\u1b80\u1b82\u1ba1\u1bad\u1bb0\u1bb9\u1be6\u1bf3"+
		"\u1c24\u1c37\u1c40\u1c49\u1c50\u1c59\u1cd0\u1cd2\u1cd4\u1ce8\u1ced\u1ced"+
		"\u1cf4\u1cf4\u1cf7\u1cf9\u1dc0\u1dff\u200c\u200d\u203f\u2040\u2054\u2054"+
		"\u2070\u2070\u2074\u2079\u2080\u2089\u20d0\u20f0\u2150\u2182\u2185\u2189"+
		"\u2460\u249b\u24ea\u24ff\u2776\u2793\u2cef\u2cf1\u2cfd\u2cfd\u2d7f\u2d7f"+
		"\u2de0\u2dff\u3007\u3007\u3021\u302f\u3038\u303a\u3099\u309a\u3192\u3195"+
		"\u3220\u3229\u3248\u324f\u3251\u325f\u3280\u3289\u32b1\u32bf\u8000\ua620"+
		"\u8000\ua629\u8000\ua66f\u8000\ua672\u8000\ua674\u8000\ua67d\u8000\ua69e"+
		"\u8000\ua69f\u8000\ua6e6\u8000\ua6f1\u8000\ua802\u8000\ua802\u8000\ua806"+
		"\u8000\ua806\u8000\ua80b\u8000\ua80b\u8000\ua823\u8000\ua827\u8000\ua82c"+
		"\u8000\ua82c\u8000\ua830\u8000\ua835\u8000\ua880\u8000\ua881\u8000\ua8b4"+
		"\u8000\ua8c5\u8000\ua8d0\u8000\ua8d9\u8000\ua8e0\u8000\ua8f1\u8000\ua8ff"+
		"\u8000\ua909\u8000\ua926\u8000\ua92d\u8000\ua947\u8000\ua953\u8000\ua980"+
		"\u8000\ua983\u8000\ua9b3\u8000\ua9c0\u8000\ua9d0\u8000\ua9d9\u8000\ua9e5"+
		"\u8000\ua9e5\u8000\ua9f0\u8000\ua9f9\u8000\uaa29\u8000\uaa36\u8000\uaa43"+
		"\u8000\uaa43\u8000\uaa4c\u8000\uaa4d\u8000\uaa50\u8000\uaa59\u8000\uaa7b"+
		"\u8000\uaa7d\u8000\uaab0\u8000\uaab0\u8000\uaab2\u8000\uaab4\u8000\uaab7"+
		"\u8000\uaab8\u8000\uaabe\u8000\uaabf\u8000\uaac1\u8000\uaac1\u8000\uaaeb"+
		"\u8000\uaaef\u8000\uaaf5\u8000\uaaf6\u8000\uabe3\u8000\uabea\u8000\uabec"+
		"\u8000\uabed\u8000\uabf0\u8000\uabf9\u8000\ufb1e\u8000\ufb1e\u8000\ufe00"+
		"\u8000\ufe0f\u8000\ufe20\u8000\ufe2f\u8000\ufe33\u8000\ufe34\u8000\ufe4d"+
		"\u8000\ufe4f\u8000\uff10\u8000\uff19\u8000\uff3f\u8000\uff3f\u8001\u0107"+
		"\u8001\u0133\u8001\u0140\u8001\u0178\u8001\u018a\u8001\u018b\u8001\u01fd"+
		"\u8001\u01fd\u8001\u02e0\u8001\u02fb\u8001\u0320\u8001\u0323\u8001\u0341"+
		"\u8001\u0341\u8001\u034a\u8001\u034a\u8001\u0376\u8001\u037a\u8001\u03d1"+
		"\u8001\u03d5\u8001\u04a0\u8001\u04a9\u8001\u0858\u8001\u085f\u8001\u0879"+
		"\u8001\u087f\u8001\u08a7\u8001\u08af\u8001\u08fb\u8001\u08ff\u8001\u0916"+
		"\u8001\u091b\u8001\u09bc\u8001\u09bd\u8001\u09c0\u8001\u09cf\u8001\u09d2"+
		"\u8001\u09ff\u8001\u0a01\u8001\u0a03\u8001\u0a05\u8001\u0a06\u8001\u0a0c"+
		"\u8001\u0a0f\u8001\u0a38\u8001\u0a3a\u8001\u0a3f\u8001\u0a48\u8001\u0a7d"+
		"\u8001\u0a7e\u8001\u0a9d\u8001\u0a9f\u8001\u0ae5\u8001\u0ae6\u8001\u0aeb"+
		"\u8001\u0aef\u8001\u0b58\u8001\u0b5f\u8001\u0b78\u8001\u0b7f\u8001\u0ba9"+
		"\u8001\u0baf\u8001\u0cfa\u8001\u0cff\u8001\u0d24\u8001\u0d27\u8001\u0d30"+
		"\u8001\u0d39\u8001\u0e60\u8001\u0e7e\u8001\u0eab\u8001\u0eac\u8001\u0efd"+
		"\u8001\u0eff\u8001\u0f1d\u8001\u0f26\u8001\u0f46\u8001\u0f54\u8001\u0f82"+
		"\u8001\u0f85\u8001\u0fc5\u8001\u0fcb\u8001\u1000\u8001\u1002\u8001\u1038"+
		"\u8001\u1046\u8001\u1052\u8001\u1070\u8001\u1073\u8001\u1074\u8001\u107f"+
		"\u8001\u1082\u8001\u10b0\u8001\u10ba\u8001\u10c2\u8001\u10c2\u8001\u10f0"+
		"\u8001\u10f9\u8001\u1100\u8001\u1102\u8001\u1127\u8001\u1134\u8001\u1136"+
		"\u8001\u113f\u8001\u1145\u8001\u1146\u8001\u1173\u8001\u1173\u8001\u1180"+
		"\u8001\u1182\u8001\u11b3\u8001\u11c0\u8001\u11c9\u8001\u11cc\u8001\u11ce"+
		"\u8001\u11d9\u8001\u11e1\u8001\u11f4\u8001\u122c\u8001\u1237\u8001\u123e"+
		"\u8001\u123e\u8001\u1241\u8001\u1241\u8001\u12df\u8001\u12ea\u8001\u12f0"+
		"\u8001\u12f9\u8001\u1300\u8001\u1303\u8001\u133b\u8001\u133c\u8001\u133e"+
		"\u8001\u1344\u8001\u1347\u8001\u1348\u8001\u134b\u8001\u134d\u8001\u1357"+
		"\u8001\u1357\u8001\u1362\u8001\u1363\u8001\u1366\u8001\u136c\u8001\u1370"+
		"\u8001\u1374\u8001\u1435\u8001\u1446\u8001\u1450\u8001\u1459\u8001\u145e"+
		"\u8001\u145e\u8001\u14b0\u8001\u14c3\u8001\u14d0\u8001\u14d9\u8001\u15af"+
		"\u8001\u15b5\u8001\u15b8\u8001\u15c0\u8001\u15dc\u8001\u15dd\u8001\u1630"+
		"\u8001\u1640\u8001\u1650\u8001\u1659\u8001\u16ab\u8001\u16b7\u8001\u16c0"+
		"\u8001\u16c9\u8001\u171d\u8001\u172b\u8001\u1730\u8001\u173b\u8001\u182c"+
		"\u8001\u183a\u8001\u18e0\u8001\u18f2\u8001\u1930\u8001\u1935\u8001\u1937"+
		"\u8001\u1938\u8001\u193b\u8001\u193e\u8001\u1940\u8001\u1940\u8001\u1942"+
		"\u8001\u1943\u8001\u1950\u8001\u1959\u8001\u19d1\u8001\u19d7\u8001\u19da"+
		"\u8001\u19e0\u8001\u19e4\u8001\u19e4\u8001\u1a01\u8001\u1a0a\u8001\u1a33"+
		"\u8001\u1a39\u8001\u1a3b\u8001\u1a3e\u8001\u1a47\u8001\u1a47\u8001\u1a51"+
		"\u8001\u1a5b\u8001\u1a8a\u8001\u1a99\u8001\u1c2f\u8001\u1c36\u8001\u1c38"+
		"\u8001\u1c3f\u8001\u1c50\u8001\u1c6c\u8001\u1c92\u8001\u1ca7\u8001\u1ca9"+
		"\u8001\u1cb6\u8001\u1d31\u8001\u1d36\u8001\u1d3a\u8001\u1d3a\u8001\u1d3c"+
		"\u8001\u1d3d\u8001\u1d3f\u8001\u1d45\u8001\u1d47\u8001\u1d47\u8001\u1d50"+
		"\u8001\u1d59\u8001\u1d8a\u8001\u1d8e\u8001\u1d90\u8001\u1d91\u8001\u1d93"+
		"\u8001\u1d97\u8001\u1da0\u8001\u1da9\u8001\u1ef3\u8001\u1ef6\u8001\u1f00"+
		"\u8001\u1f01\u8001\u1f03\u8001\u1f03\u8001\u1f34\u8001\u1f3a\u8001\u1f3e"+
		"\u8001\u1f42\u8001\u1f50\u8001\u1f59\u8001\u1fc0\u8001\u1fd4\u8001\u2400"+
		"\u8001\u246e\u8001\u3440\u8001\u3440\u8001\u3447\u8001\u3455\u8001\u6a60"+
		"\u8001\u6a69\u8001\u6ac0\u8001\u6ac9\u8001\u6af0\u8001\u6af4\u8001\u6b30"+
		"\u8001\u6b36\u8001\u6b50\u8001\u6b59\u8001\u6b5b\u8001\u6b61\u8001\u6e80"+
		"\u8001\u6e96\u8001\u6f4f\u8001\u6f4f\u8001\u6f51\u8001\u6f87\u8001\u6f8f"+
		"\u8001\u6f92\u8001\u6fe4\u8001\u6fe4\u8001\u6ff0\u8001\u6ff1\u8001\ubc9d"+
		"\u8001\ubc9e\u8001\ucf00\u8001\ucf2d\u8001\ucf30\u8001\ucf46\u8001\ud165"+
		"\u8001\ud169\u8001\ud16d\u8001\ud172\u8001\ud17b\u8001\ud182\u8001\ud185"+
		"\u8001\ud18b\u8001\ud1aa\u8001\ud1ad\u8001\ud242\u8001\ud244\u8001\ud2c0"+
		"\u8001\ud2d3\u8001\ud2e0\u8001\ud2f3\u8001\ud360\u8001\ud378\u8001\ud7ce"+
		"\u8001\ud7ff\u8001\uda00\u8001\uda36\u8001\uda3b\u8001\uda6c\u8001\uda75"+
		"\u8001\uda75\u8001\uda84\u8001\uda84\u8001\uda9b\u8001\uda9f\u8001\udaa1"+
		"\u8001\udaaf\u8001\ue000\u8001\ue006\u8001\ue008\u8001\ue018\u8001\ue01b"+
		"\u8001\ue021\u8001\ue023\u8001\ue024\u8001\ue026\u8001\ue02a\u8001\ue08f"+
		"\u8001\ue08f\u8001\ue130\u8001\ue136\u8001\ue140\u8001\ue149\u8001\ue2ae"+
		"\u8001\ue2ae\u8001\ue2ec\u8001\ue2f9\u8001\ue4ec\u8001\ue4f9\u8001\ue8c7"+
		"\u8001\ue8d6\u8001\ue944\u8001\ue94a\u8001\ue950\u8001\ue959\u8001\uec71"+
		"\u8001\uecab\u8001\uecad\u8001\uecaf\u8001\uecb1\u8001\uecb4\u8001\ued01"+
		"\u8001\ued2d\u8001\ued2f\u8001\ued3d\u8001\uf100\u8001\uf10c\u8001\ufbf0"+
		"\u8001\ufbf9\u800e\u0100\u800e\u01ef\u0003\u0000\n\n\r\r\u2028\u2029\u0006"+
		"\u0000\t\n\r\r  \u00a0\u00a0\u2003\u2003\u8000\ufeff\u8000\ufeff\u0113"+
		"\u0000\u0001\u0001\u0000\u0000\u0000\u0000\u0003\u0001\u0000\u0000\u0000"+
		"\u0000\u0005\u0001\u0000\u0000\u0000\u0000\u0007\u0001\u0000\u0000\u0000"+
		"\u0000\t\u0001\u0000\u0000\u0000\u0000\u000b\u0001\u0000\u0000\u0000\u0000"+
		"\r\u0001\u0000\u0000\u0000\u0000\u000f\u0001\u0000\u0000\u0000\u0000\u0011"+
		"\u0001\u0000\u0000\u0000\u0000\u0013\u0001\u0000\u0000\u0000\u0000\u001b"+
		"\u0001\u0000\u0000\u0000\u0000\u001d\u0001\u0000\u0000\u0000\u0000\u001f"+
		"\u0001\u0000\u0000\u0000\u0000\'\u0001\u0000\u0000\u0000\u00001\u0001"+
		"\u0000\u0000\u0000\u00003\u0001\u0000\u0000\u0000\u00015\u0001\u0000\u0000"+
		"\u0000\u00037\u0001\u0000\u0000\u0000\u00059\u0001\u0000\u0000\u0000\u0007"+
		";\u0001\u0000\u0000\u0000\t=\u0001\u0000\u0000\u0000\u000b?\u0001\u0000"+
		"\u0000\u0000\rA\u0001\u0000\u0000\u0000\u000fP\u0001\u0000\u0000\u0000"+
		"\u0011k\u0001\u0000\u0000\u0000\u0013}\u0001\u0000\u0000\u0000\u0015\u0081"+
		"\u0001\u0000\u0000\u0000\u0017\u0085\u0001\u0000\u0000\u0000\u0019\u0087"+
		"\u0001\u0000\u0000\u0000\u001b\u00b0\u0001\u0000\u0000\u0000\u001d\u00bd"+
		"\u0001\u0000\u0000\u0000\u001f\u00bf\u0001\u0000\u0000\u0000!\u00c1\u0001"+
		"\u0000\u0000\u0000#\u00cb\u0001\u0000\u0000\u0000%\u00cd\u0001\u0000\u0000"+
		"\u0000\'\u00d7\u0001\u0000\u0000\u0000)\u00e1\u0001\u0000\u0000\u0000"+
		"+\u00e5\u0001\u0000\u0000\u0000-\u00e7\u0001\u0000\u0000\u0000/\u00f0"+
		"\u0001\u0000\u0000\u00001\u00f3\u0001\u0000\u0000\u00003\u00f9\u0001\u0000"+
		"\u0000\u000056\u0005{\u0000\u00006\u0002\u0001\u0000\u0000\u000078\u0005"+
		",\u0000\u00008\u0004\u0001\u0000\u0000\u00009:\u0005}\u0000\u0000:\u0006"+
		"\u0001\u0000\u0000\u0000;<\u0005:\u0000\u0000<\b\u0001\u0000\u0000\u0000"+
		"=>\u0005[\u0000\u0000>\n\u0001\u0000\u0000\u0000?@\u0005]\u0000\u0000"+
		"@\f\u0001\u0000\u0000\u0000AB\u0005/\u0000\u0000BC\u0005/\u0000\u0000"+
		"CG\u0001\u0000\u0000\u0000DF\t\u0000\u0000\u0000ED\u0001\u0000\u0000\u0000"+
		"FI\u0001\u0000\u0000\u0000GH\u0001\u0000\u0000\u0000GE\u0001\u0000\u0000"+
		"\u0000HL\u0001\u0000\u0000\u0000IG\u0001\u0000\u0000\u0000JM\u0003/\u0017"+
		"\u0000KM\u0005\u0000\u0000\u0001LJ\u0001\u0000\u0000\u0000LK\u0001\u0000"+
		"\u0000\u0000MN\u0001\u0000\u0000\u0000NO\u0006\u0006\u0000\u0000O\u000e"+
		"\u0001\u0000\u0000\u0000PQ\u0005/\u0000\u0000QR\u0005*\u0000\u0000RV\u0001"+
		"\u0000\u0000\u0000SU\t\u0000\u0000\u0000TS\u0001\u0000\u0000\u0000UX\u0001"+
		"\u0000\u0000\u0000VW\u0001\u0000\u0000\u0000VT\u0001\u0000\u0000\u0000"+
		"WY\u0001\u0000\u0000\u0000XV\u0001\u0000\u0000\u0000YZ\u0005*\u0000\u0000"+
		"Z[\u0005/\u0000\u0000[\\\u0001\u0000\u0000\u0000\\]\u0006\u0007\u0000"+
		"\u0000]\u0010\u0001\u0000\u0000\u0000^_\u0005t\u0000\u0000_`\u0005r\u0000"+
		"\u0000`a\u0005u\u0000\u0000al\u0005e\u0000\u0000bc\u0005f\u0000\u0000"+
		"cd\u0005a\u0000\u0000de\u0005l\u0000\u0000ef\u0005s\u0000\u0000fl\u0005"+
		"e\u0000\u0000gh\u0005n\u0000\u0000hi\u0005u\u0000\u0000ij\u0005l\u0000"+
		"\u0000jl\u0005l\u0000\u0000k^\u0001\u0000\u0000\u0000kb\u0001\u0000\u0000"+
		"\u0000kg\u0001\u0000\u0000\u0000l\u0012\u0001\u0000\u0000\u0000mq\u0005"+
		"\"\u0000\u0000np\u0003\u0015\n\u0000on\u0001\u0000\u0000\u0000ps\u0001"+
		"\u0000\u0000\u0000qo\u0001\u0000\u0000\u0000qr\u0001\u0000\u0000\u0000"+
		"rt\u0001\u0000\u0000\u0000sq\u0001\u0000\u0000\u0000t~\u0005\"\u0000\u0000"+
		"uy\u0005\'\u0000\u0000vx\u0003\u0017\u000b\u0000wv\u0001\u0000\u0000\u0000"+
		"x{\u0001\u0000\u0000\u0000yw\u0001\u0000\u0000\u0000yz\u0001\u0000\u0000"+
		"\u0000z|\u0001\u0000\u0000\u0000{y\u0001\u0000\u0000\u0000|~\u0005\'\u0000"+
		"\u0000}m\u0001\u0000\u0000\u0000}u\u0001\u0000\u0000\u0000~\u0014\u0001"+
		"\u0000\u0000\u0000\u007f\u0082\b\u0000\u0000\u0000\u0080\u0082\u0003\u0019"+
		"\f\u0000\u0081\u007f\u0001\u0000\u0000\u0000\u0081\u0080\u0001\u0000\u0000"+
		"\u0000\u0082\u0016\u0001\u0000\u0000\u0000\u0083\u0086\b\u0001\u0000\u0000"+
		"\u0084\u0086\u0003\u0019\f\u0000\u0085\u0083\u0001\u0000\u0000\u0000\u0085"+
		"\u0084\u0001\u0000\u0000\u0000\u0086\u0018\u0001\u0000\u0000\u0000\u0087"+
		"\u0091\u0005\\\u0000\u0000\u0088\u0092\u0003/\u0017\u0000\u0089\u0092"+
		"\u0003-\u0016\u0000\u008a\u0092\u0007\u0002\u0000\u0000\u008b\u0092\b"+
		"\u0003\u0000\u0000\u008c\u0092\u00050\u0000\u0000\u008d\u008e\u0005x\u0000"+
		"\u0000\u008e\u008f\u0003!\u0010\u0000\u008f\u0090\u0003!\u0010\u0000\u0090"+
		"\u0092\u0001\u0000\u0000\u0000\u0091\u0088\u0001\u0000\u0000\u0000\u0091"+
		"\u0089\u0001\u0000\u0000\u0000\u0091\u008a\u0001\u0000\u0000\u0000\u0091"+
		"\u008b\u0001\u0000\u0000\u0000\u0091\u008c\u0001\u0000\u0000\u0000\u0091"+
		"\u008d\u0001\u0000\u0000\u0000\u0092\u001a\u0001\u0000\u0000\u0000\u0093"+
		"\u009b\u0003#\u0011\u0000\u0094\u0098\u0005.\u0000\u0000\u0095\u0097\u0007"+
		"\u0004\u0000\u0000\u0096\u0095\u0001\u0000\u0000\u0000\u0097\u009a\u0001"+
		"\u0000\u0000\u0000\u0098\u0096\u0001\u0000\u0000\u0000\u0098\u0099\u0001"+
		"\u0000\u0000\u0000\u0099\u009c\u0001\u0000\u0000\u0000\u009a\u0098\u0001"+
		"\u0000\u0000\u0000\u009b\u0094\u0001\u0000\u0000\u0000\u009b\u009c\u0001"+
		"\u0000\u0000\u0000\u009c\u009e\u0001\u0000\u0000\u0000\u009d\u009f\u0003"+
		"%\u0012\u0000\u009e\u009d\u0001\u0000\u0000\u0000\u009e\u009f\u0001\u0000"+
		"\u0000\u0000\u009f\u00b1\u0001\u0000\u0000\u0000\u00a0\u00a2\u0005.\u0000"+
		"\u0000\u00a1\u00a3\u0007\u0004\u0000\u0000\u00a2\u00a1\u0001\u0000\u0000"+
		"\u0000\u00a3\u00a4\u0001\u0000\u0000\u0000\u00a4\u00a2\u0001\u0000\u0000"+
		"\u0000\u00a4\u00a5\u0001\u0000\u0000\u0000\u00a5\u00a7\u0001\u0000\u0000"+
		"\u0000\u00a6\u00a8\u0003%\u0012\u0000\u00a7\u00a6\u0001\u0000\u0000\u0000"+
		"\u00a7\u00a8\u0001\u0000\u0000\u0000\u00a8\u00b1\u0001\u0000\u0000\u0000"+
		"\u00a9\u00aa\u00050\u0000\u0000\u00aa\u00ac\u0007\u0005\u0000\u0000\u00ab"+
		"\u00ad\u0003!\u0010\u0000\u00ac\u00ab\u0001\u0000\u0000\u0000\u00ad\u00ae"+
		"\u0001\u0000\u0000\u0000\u00ae\u00ac\u0001\u0000\u0000\u0000\u00ae\u00af"+
		"\u0001\u0000\u0000\u0000\u00af\u00b1\u0001\u0000\u0000\u0000\u00b0\u0093"+
		"\u0001\u0000\u0000\u0000\u00b0\u00a0\u0001\u0000\u0000\u0000\u00b0\u00a9"+
		"\u0001\u0000\u0000\u0000\u00b1\u001c\u0001\u0000\u0000\u0000\u00b2\u00b3"+
		"\u0005I\u0000\u0000\u00b3\u00b4\u0005n\u0000\u0000\u00b4\u00b5\u0005f"+
		"\u0000\u0000\u00b5\u00b6\u0005i\u0000\u0000\u00b6\u00b7\u0005n\u0000\u0000"+
		"\u00b7\u00b8\u0005i\u0000\u0000\u00b8\u00b9\u0005t\u0000\u0000\u00b9\u00be"+
		"\u0005y\u0000\u0000\u00ba\u00bb\u0005N\u0000\u0000\u00bb\u00bc\u0005a"+
		"\u0000\u0000\u00bc\u00be\u0005N\u0000\u0000\u00bd\u00b2\u0001\u0000\u0000"+
		"\u0000\u00bd\u00ba\u0001\u0000\u0000\u0000\u00be\u001e\u0001\u0000\u0000"+
		"\u0000\u00bf\u00c0\u0007\u0006\u0000\u0000\u00c0 \u0001\u0000\u0000\u0000"+
		"\u00c1\u00c2\u0007\u0007\u0000\u0000\u00c2\"\u0001\u0000\u0000\u0000\u00c3"+
		"\u00cc\u00050\u0000\u0000\u00c4\u00c8\u0007\b\u0000\u0000\u00c5\u00c7"+
		"\u0007\u0004\u0000\u0000\u00c6\u00c5\u0001\u0000\u0000\u0000\u00c7\u00ca"+
		"\u0001\u0000\u0000\u0000\u00c8\u00c6\u0001\u0000\u0000\u0000\u00c8\u00c9"+
		"\u0001\u0000\u0000\u0000\u00c9\u00cc\u0001\u0000\u0000\u0000\u00ca\u00c8"+
		"\u0001\u0000\u0000\u0000\u00cb\u00c3\u0001\u0000\u0000\u0000\u00cb\u00c4"+
		"\u0001\u0000\u0000\u0000\u00cc$\u0001\u0000\u0000\u0000\u00cd\u00cf\u0007"+
		"\t\u0000\u0000\u00ce\u00d0\u0003\u001f\u000f\u0000\u00cf\u00ce\u0001\u0000"+
		"\u0000\u0000\u00cf\u00d0\u0001\u0000\u0000\u0000\u00d0\u00d4\u0001\u0000"+
		"\u0000\u0000\u00d1\u00d3\u0007\u0004\u0000\u0000\u00d2\u00d1\u0001\u0000"+
		"\u0000\u0000\u00d3\u00d6\u0001\u0000\u0000\u0000\u00d4\u00d2\u0001\u0000"+
		"\u0000\u0000\u00d4\u00d5\u0001\u0000\u0000\u0000\u00d5&\u0001\u0000\u0000"+
		"\u0000\u00d6\u00d4\u0001\u0000\u0000\u0000\u00d7\u00db\u0003)\u0014\u0000"+
		"\u00d8\u00da\u0003+\u0015\u0000\u00d9\u00d8\u0001\u0000\u0000\u0000\u00da"+
		"\u00dd\u0001\u0000\u0000\u0000\u00db\u00d9\u0001\u0000\u0000\u0000\u00db"+
		"\u00dc\u0001\u0000\u0000\u0000\u00dc(\u0001\u0000\u0000\u0000\u00dd\u00db"+
		"\u0001\u0000\u0000\u0000\u00de\u00e2\u0007\n\u0000\u0000\u00df\u00e0\u0005"+
		"\\\u0000\u0000\u00e0\u00e2\u0003-\u0016\u0000\u00e1\u00de\u0001\u0000"+
		"\u0000\u0000\u00e1\u00df\u0001\u0000\u0000\u0000\u00e2*\u0001\u0000\u0000"+
		"\u0000\u00e3\u00e6\u0003)\u0014\u0000\u00e4\u00e6\u0007\u000b\u0000\u0000"+
		"\u00e5\u00e3\u0001\u0000\u0000\u0000\u00e5\u00e4\u0001\u0000\u0000\u0000"+
		"\u00e6,\u0001\u0000\u0000\u0000\u00e7\u00e8\u0005u\u0000\u0000\u00e8\u00e9"+
		"\u0003!\u0010\u0000\u00e9\u00ea\u0003!\u0010\u0000\u00ea\u00eb\u0003!"+
		"\u0010\u0000\u00eb\u00ec\u0003!\u0010\u0000\u00ec.\u0001\u0000\u0000\u0000"+
		"\u00ed\u00ee\u0005\r\u0000\u0000\u00ee\u00f1\u0005\n\u0000\u0000\u00ef"+
		"\u00f1\u0007\f\u0000\u0000\u00f0\u00ed\u0001\u0000\u0000\u0000\u00f0\u00ef"+
		"\u0001\u0000\u0000\u0000\u00f10\u0001\u0000\u0000\u0000\u00f2\u00f4\u0007"+
		"\r\u0000\u0000\u00f3\u00f2\u0001\u0000\u0000\u0000\u00f4\u00f5\u0001\u0000"+
		"\u0000\u0000\u00f5\u00f3\u0001\u0000\u0000\u0000\u00f5\u00f6\u0001\u0000"+
		"\u0000\u0000\u00f6\u00f7\u0001\u0000\u0000\u0000\u00f7\u00f8\u0006\u0018"+
		"\u0000\u0000\u00f82\u0001\u0000\u0000\u0000\u00f9\u00fa\u0005\u8000\ufeff"+
		"\u0000\u0000\u00fa\u00fb\u0001\u0000\u0000\u0000\u00fb\u00fc\u0006\u0019"+
		"\u0000\u0000\u00fc4\u0001\u0000\u0000\u0000\u001c\u0000GLVkqy}\u0081\u0085"+
		"\u0091\u0098\u009b\u009e\u00a4\u00a7\u00ae\u00b0\u00bd\u00c8\u00cb\u00cf"+
		"\u00d4\u00db\u00e1\u00e5\u00f0\u00f5\u0001\u0006\u0000\u0000";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
