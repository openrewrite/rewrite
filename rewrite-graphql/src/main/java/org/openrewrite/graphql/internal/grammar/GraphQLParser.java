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
// Generated from /Users/kylescully/Repos/rewrite/rewrite-graphql/src/main/antlr/GraphQL.g4 by ANTLR 4.13.2
package org.openrewrite.graphql.internal.grammar;

import java.util.*;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class GraphQLParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, T__14=15, T__15=16, T__16=17, 
		T__17=18, T__18=19, T__19=20, T__20=21, T__21=22, T__22=23, T__23=24, 
		T__24=25, T__25=26, T__26=27, T__27=28, T__28=29, T__29=30, T__30=31, 
		T__31=32, T__32=33, T__33=34, T__34=35, T__35=36, T__36=37, T__37=38, 
		T__38=39, T__39=40, T__40=41, T__41=42, T__42=43, T__43=44, T__44=45, 
		T__45=46, T__46=47, T__47=48, T__48=49, T__49=50, T__50=51, T__51=52, 
		T__52=53, NAME=54, INT=55, FLOAT=56, EXPONENT=57, STRING=58, BLOCK_STRING=59, 
		COMMENT=60, WS=61, COMMA=62, UNICODE_BOM=63;
	public static final int
		RULE_document = 0, RULE_definition = 1, RULE_executableDefinition = 2, 
		RULE_operationDefinition = 3, RULE_operationType = 4, RULE_selectionSet = 5, 
		RULE_selection = 6, RULE_field = 7, RULE_alias = 8, RULE_arguments = 9, 
		RULE_argument = 10, RULE_fragmentSpread = 11, RULE_inlineFragment = 12, 
		RULE_fragmentDefinition = 13, RULE_fragmentName = 14, RULE_typeCondition = 15, 
		RULE_value = 16, RULE_variable = 17, RULE_variableDefinitions = 18, RULE_variableDefinition = 19, 
		RULE_defaultValue = 20, RULE_type = 21, RULE_namedType = 22, RULE_listType = 23, 
		RULE_nonNullType = 24, RULE_directives = 25, RULE_directive = 26, RULE_typeSystemDefinition = 27, 
		RULE_typeSystemExtension = 28, RULE_schemaDefinition = 29, RULE_rootOperationTypeDefinition = 30, 
		RULE_schemaExtension = 31, RULE_typeDefinition = 32, RULE_typeExtension = 33, 
		RULE_scalarTypeDefinition = 34, RULE_scalarTypeExtension = 35, RULE_objectTypeDefinition = 36, 
		RULE_objectTypeExtension = 37, RULE_implementsInterfaces = 38, RULE_fieldsDefinition = 39, 
		RULE_fieldDefinition = 40, RULE_argumentsDefinition = 41, RULE_inputValueDefinition = 42, 
		RULE_interfaceTypeDefinition = 43, RULE_interfaceTypeExtension = 44, RULE_unionTypeDefinition = 45, 
		RULE_unionMemberTypes = 46, RULE_unionTypeExtension = 47, RULE_enumTypeDefinition = 48, 
		RULE_enumValuesDefinition = 49, RULE_enumValueDefinition = 50, RULE_enumTypeExtension = 51, 
		RULE_inputObjectTypeDefinition = 52, RULE_inputFieldsDefinition = 53, 
		RULE_inputObjectTypeExtension = 54, RULE_directiveDefinition = 55, RULE_directiveLocations = 56, 
		RULE_directiveLocation = 57, RULE_executableDirectiveLocation = 58, RULE_typeSystemDirectiveLocation = 59, 
		RULE_name = 60, RULE_description = 61, RULE_booleanValue = 62, RULE_nullValue = 63, 
		RULE_enumValue = 64, RULE_listValue = 65, RULE_objectValue = 66, RULE_objectField = 67, 
		RULE_intValue = 68, RULE_floatValue = 69, RULE_stringValue = 70;
	private static String[] makeRuleNames() {
		return new String[] {
			"document", "definition", "executableDefinition", "operationDefinition", 
			"operationType", "selectionSet", "selection", "field", "alias", "arguments", 
			"argument", "fragmentSpread", "inlineFragment", "fragmentDefinition", 
			"fragmentName", "typeCondition", "value", "variable", "variableDefinitions", 
			"variableDefinition", "defaultValue", "type", "namedType", "listType", 
			"nonNullType", "directives", "directive", "typeSystemDefinition", "typeSystemExtension", 
			"schemaDefinition", "rootOperationTypeDefinition", "schemaExtension", 
			"typeDefinition", "typeExtension", "scalarTypeDefinition", "scalarTypeExtension", 
			"objectTypeDefinition", "objectTypeExtension", "implementsInterfaces", 
			"fieldsDefinition", "fieldDefinition", "argumentsDefinition", "inputValueDefinition", 
			"interfaceTypeDefinition", "interfaceTypeExtension", "unionTypeDefinition", 
			"unionMemberTypes", "unionTypeExtension", "enumTypeDefinition", "enumValuesDefinition", 
			"enumValueDefinition", "enumTypeExtension", "inputObjectTypeDefinition", 
			"inputFieldsDefinition", "inputObjectTypeExtension", "directiveDefinition", 
			"directiveLocations", "directiveLocation", "executableDirectiveLocation", 
			"typeSystemDirectiveLocation", "name", "description", "booleanValue", 
			"nullValue", "enumValue", "listValue", "objectValue", "objectField", 
			"intValue", "floatValue", "stringValue"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'query'", "'mutation'", "'subscription'", "'{'", "'}'", "':'", 
			"'('", "')'", "'...'", "'fragment'", "'on'", "'$'", "'='", "'['", "']'", 
			"'!'", "'@'", "'schema'", "'extend'", "'scalar'", "'type'", "'implements'", 
			"'&'", "'interface'", "'union'", "'|'", "'enum'", "'input'", "'directive'", 
			"'repeatable'", "'QUERY'", "'MUTATION'", "'SUBSCRIPTION'", "'FIELD'", 
			"'FRAGMENT_DEFINITION'", "'FRAGMENT_SPREAD'", "'INLINE_FRAGMENT'", "'VARIABLE_DEFINITION'", 
			"'SCHEMA'", "'SCALAR'", "'OBJECT'", "'FIELD_DEFINITION'", "'ARGUMENT_DEFINITION'", 
			"'INTERFACE'", "'UNION'", "'ENUM'", "'ENUM_VALUE'", "'INPUT_OBJECT'", 
			"'INPUT_FIELD_DEFINITION'", "'true'", "'false'", "'null'", "'extends'", 
			null, null, null, null, null, null, null, null, "','", "'\\uFEFF'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, "NAME", "INT", "FLOAT", "EXPONENT", 
			"STRING", "BLOCK_STRING", "COMMENT", "WS", "COMMA", "UNICODE_BOM"
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

	@Override
	public String getGrammarFileName() { return "GraphQL.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public GraphQLParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DocumentContext extends ParserRuleContext {
		public TerminalNode EOF() { return getToken(GraphQLParser.EOF, 0); }
		public List<DefinitionContext> definition() {
			return getRuleContexts(DefinitionContext.class);
		}
		public DefinitionContext definition(int i) {
			return getRuleContext(DefinitionContext.class,i);
		}
		public DocumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_document; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterDocument(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitDocument(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitDocument(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DocumentContext document() throws RecognitionException {
		DocumentContext _localctx = new DocumentContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_document);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(145);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 864691129448924190L) != 0)) {
				{
				{
				setState(142);
				definition();
				}
				}
				setState(147);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(148);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DefinitionContext extends ParserRuleContext {
		public ExecutableDefinitionContext executableDefinition() {
			return getRuleContext(ExecutableDefinitionContext.class,0);
		}
		public TypeSystemDefinitionContext typeSystemDefinition() {
			return getRuleContext(TypeSystemDefinitionContext.class,0);
		}
		public TypeSystemExtensionContext typeSystemExtension() {
			return getRuleContext(TypeSystemExtensionContext.class,0);
		}
		public DefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_definition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitDefinition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitDefinition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DefinitionContext definition() throws RecognitionException {
		DefinitionContext _localctx = new DefinitionContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_definition);
		try {
			setState(153);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__0:
			case T__1:
			case T__2:
			case T__3:
			case T__9:
				enterOuterAlt(_localctx, 1);
				{
				setState(150);
				executableDefinition();
				}
				break;
			case T__17:
			case T__19:
			case T__20:
			case T__23:
			case T__24:
			case T__26:
			case T__27:
			case T__28:
			case STRING:
			case BLOCK_STRING:
				enterOuterAlt(_localctx, 2);
				{
				setState(151);
				typeSystemDefinition();
				}
				break;
			case T__18:
				enterOuterAlt(_localctx, 3);
				{
				setState(152);
				typeSystemExtension();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExecutableDefinitionContext extends ParserRuleContext {
		public OperationDefinitionContext operationDefinition() {
			return getRuleContext(OperationDefinitionContext.class,0);
		}
		public FragmentDefinitionContext fragmentDefinition() {
			return getRuleContext(FragmentDefinitionContext.class,0);
		}
		public ExecutableDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_executableDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterExecutableDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitExecutableDefinition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitExecutableDefinition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExecutableDefinitionContext executableDefinition() throws RecognitionException {
		ExecutableDefinitionContext _localctx = new ExecutableDefinitionContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_executableDefinition);
		try {
			setState(157);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__0:
			case T__1:
			case T__2:
			case T__3:
				enterOuterAlt(_localctx, 1);
				{
				setState(155);
				operationDefinition();
				}
				break;
			case T__9:
				enterOuterAlt(_localctx, 2);
				{
				setState(156);
				fragmentDefinition();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class OperationDefinitionContext extends ParserRuleContext {
		public OperationTypeContext operationType() {
			return getRuleContext(OperationTypeContext.class,0);
		}
		public SelectionSetContext selectionSet() {
			return getRuleContext(SelectionSetContext.class,0);
		}
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public VariableDefinitionsContext variableDefinitions() {
			return getRuleContext(VariableDefinitionsContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public OperationDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_operationDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterOperationDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitOperationDefinition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitOperationDefinition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OperationDefinitionContext operationDefinition() throws RecognitionException {
		OperationDefinitionContext _localctx = new OperationDefinitionContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_operationDefinition);
		int _la;
		try {
			setState(172);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__0:
			case T__1:
			case T__2:
				enterOuterAlt(_localctx, 1);
				{
				setState(159);
				operationType();
				setState(161);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 34902899183324174L) != 0)) {
					{
					setState(160);
					name();
					}
				}

				setState(164);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__6) {
					{
					setState(163);
					variableDefinitions();
					}
				}

				setState(167);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__16) {
					{
					setState(166);
					directives();
					}
				}

				setState(169);
				selectionSet();
				}
				break;
			case T__3:
				enterOuterAlt(_localctx, 2);
				{
				setState(171);
				selectionSet();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class OperationTypeContext extends ParserRuleContext {
		public OperationTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_operationType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterOperationType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitOperationType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitOperationType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OperationTypeContext operationType() throws RecognitionException {
		OperationTypeContext _localctx = new OperationTypeContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_operationType);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(174);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 14L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SelectionSetContext extends ParserRuleContext {
		public List<SelectionContext> selection() {
			return getRuleContexts(SelectionContext.class);
		}
		public SelectionContext selection(int i) {
			return getRuleContext(SelectionContext.class,i);
		}
		public SelectionSetContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_selectionSet; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterSelectionSet(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitSelectionSet(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitSelectionSet(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SelectionSetContext selectionSet() throws RecognitionException {
		SelectionSetContext _localctx = new SelectionSetContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_selectionSet);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(176);
			match(T__3);
			setState(180);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 34902899183324686L) != 0)) {
				{
				{
				setState(177);
				selection();
				}
				}
				setState(182);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(183);
			match(T__4);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SelectionContext extends ParserRuleContext {
		public FieldContext field() {
			return getRuleContext(FieldContext.class,0);
		}
		public FragmentSpreadContext fragmentSpread() {
			return getRuleContext(FragmentSpreadContext.class,0);
		}
		public InlineFragmentContext inlineFragment() {
			return getRuleContext(InlineFragmentContext.class,0);
		}
		public SelectionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_selection; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterSelection(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitSelection(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitSelection(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SelectionContext selection() throws RecognitionException {
		SelectionContext _localctx = new SelectionContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_selection);
		try {
			setState(188);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,8,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(185);
				field();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(186);
				fragmentSpread();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(187);
				inlineFragment();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FieldContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public AliasContext alias() {
			return getRuleContext(AliasContext.class,0);
		}
		public ArgumentsContext arguments() {
			return getRuleContext(ArgumentsContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public SelectionSetContext selectionSet() {
			return getRuleContext(SelectionSetContext.class,0);
		}
		public FieldContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_field; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterField(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitField(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitField(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FieldContext field() throws RecognitionException {
		FieldContext _localctx = new FieldContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_field);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(191);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,9,_ctx) ) {
			case 1:
				{
				setState(190);
				alias();
				}
				break;
			}
			setState(193);
			name();
			setState(195);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__6) {
				{
				setState(194);
				arguments();
				}
			}

			setState(198);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__16) {
				{
				setState(197);
				directives();
				}
			}

			setState(201);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__3) {
				{
				setState(200);
				selectionSet();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AliasContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public AliasContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_alias; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterAlias(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitAlias(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitAlias(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AliasContext alias() throws RecognitionException {
		AliasContext _localctx = new AliasContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_alias);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(203);
			name();
			setState(204);
			match(T__5);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ArgumentsContext extends ParserRuleContext {
		public List<ArgumentContext> argument() {
			return getRuleContexts(ArgumentContext.class);
		}
		public ArgumentContext argument(int i) {
			return getRuleContext(ArgumentContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(GraphQLParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(GraphQLParser.COMMA, i);
		}
		public ArgumentsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arguments; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterArguments(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitArguments(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitArguments(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArgumentsContext arguments() throws RecognitionException {
		ArgumentsContext _localctx = new ArgumentsContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_arguments);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(206);
			match(T__6);
			setState(217);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 34902899183324174L) != 0)) {
				{
				setState(207);
				argument();
				setState(214);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 4646588917610712078L) != 0)) {
					{
					{
					setState(209);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==COMMA) {
						{
						setState(208);
						match(COMMA);
						}
					}

					setState(211);
					argument();
					}
					}
					setState(216);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(219);
			match(T__7);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ArgumentContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public ValueContext value() {
			return getRuleContext(ValueContext.class,0);
		}
		public ArgumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_argument; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterArgument(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitArgument(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitArgument(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArgumentContext argument() throws RecognitionException {
		ArgumentContext _localctx = new ArgumentContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_argument);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(221);
			name();
			setState(222);
			match(T__5);
			setState(223);
			value();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FragmentSpreadContext extends ParserRuleContext {
		public FragmentNameContext fragmentName() {
			return getRuleContext(FragmentNameContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public FragmentSpreadContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fragmentSpread; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterFragmentSpread(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitFragmentSpread(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitFragmentSpread(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FragmentSpreadContext fragmentSpread() throws RecognitionException {
		FragmentSpreadContext _localctx = new FragmentSpreadContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_fragmentSpread);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(225);
			match(T__8);
			setState(226);
			fragmentName();
			setState(228);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__16) {
				{
				setState(227);
				directives();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class InlineFragmentContext extends ParserRuleContext {
		public SelectionSetContext selectionSet() {
			return getRuleContext(SelectionSetContext.class,0);
		}
		public TypeConditionContext typeCondition() {
			return getRuleContext(TypeConditionContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public InlineFragmentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inlineFragment; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterInlineFragment(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitInlineFragment(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitInlineFragment(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InlineFragmentContext inlineFragment() throws RecognitionException {
		InlineFragmentContext _localctx = new InlineFragmentContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_inlineFragment);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(230);
			match(T__8);
			setState(232);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__10) {
				{
				setState(231);
				typeCondition();
				}
			}

			setState(235);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__16) {
				{
				setState(234);
				directives();
				}
			}

			setState(237);
			selectionSet();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FragmentDefinitionContext extends ParserRuleContext {
		public FragmentNameContext fragmentName() {
			return getRuleContext(FragmentNameContext.class,0);
		}
		public TypeConditionContext typeCondition() {
			return getRuleContext(TypeConditionContext.class,0);
		}
		public SelectionSetContext selectionSet() {
			return getRuleContext(SelectionSetContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public FragmentDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fragmentDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterFragmentDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitFragmentDefinition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitFragmentDefinition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FragmentDefinitionContext fragmentDefinition() throws RecognitionException {
		FragmentDefinitionContext _localctx = new FragmentDefinitionContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_fragmentDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(239);
			match(T__9);
			setState(240);
			fragmentName();
			setState(241);
			typeCondition();
			setState(243);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__16) {
				{
				setState(242);
				directives();
				}
			}

			setState(245);
			selectionSet();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FragmentNameContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public FragmentNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fragmentName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterFragmentName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitFragmentName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitFragmentName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FragmentNameContext fragmentName() throws RecognitionException {
		FragmentNameContext _localctx = new FragmentNameContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_fragmentName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(247);
			name();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeConditionContext extends ParserRuleContext {
		public NamedTypeContext namedType() {
			return getRuleContext(NamedTypeContext.class,0);
		}
		public TypeConditionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeCondition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterTypeCondition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitTypeCondition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitTypeCondition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeConditionContext typeCondition() throws RecognitionException {
		TypeConditionContext _localctx = new TypeConditionContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_typeCondition);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(249);
			match(T__10);
			setState(250);
			namedType();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ValueContext extends ParserRuleContext {
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public IntValueContext intValue() {
			return getRuleContext(IntValueContext.class,0);
		}
		public FloatValueContext floatValue() {
			return getRuleContext(FloatValueContext.class,0);
		}
		public StringValueContext stringValue() {
			return getRuleContext(StringValueContext.class,0);
		}
		public BooleanValueContext booleanValue() {
			return getRuleContext(BooleanValueContext.class,0);
		}
		public NullValueContext nullValue() {
			return getRuleContext(NullValueContext.class,0);
		}
		public EnumValueContext enumValue() {
			return getRuleContext(EnumValueContext.class,0);
		}
		public ListValueContext listValue() {
			return getRuleContext(ListValueContext.class,0);
		}
		public ObjectValueContext objectValue() {
			return getRuleContext(ObjectValueContext.class,0);
		}
		public ValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_value; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ValueContext value() throws RecognitionException {
		ValueContext _localctx = new ValueContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_value);
		try {
			setState(261);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,20,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(252);
				variable();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(253);
				intValue();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(254);
				floatValue();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(255);
				stringValue();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(256);
				booleanValue();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(257);
				nullValue();
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(258);
				enumValue();
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(259);
				listValue();
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(260);
				objectValue();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class VariableContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public VariableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterVariable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitVariable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitVariable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableContext variable() throws RecognitionException {
		VariableContext _localctx = new VariableContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_variable);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(263);
			match(T__11);
			setState(264);
			name();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class VariableDefinitionsContext extends ParserRuleContext {
		public List<VariableDefinitionContext> variableDefinition() {
			return getRuleContexts(VariableDefinitionContext.class);
		}
		public VariableDefinitionContext variableDefinition(int i) {
			return getRuleContext(VariableDefinitionContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(GraphQLParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(GraphQLParser.COMMA, i);
		}
		public VariableDefinitionsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableDefinitions; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterVariableDefinitions(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitVariableDefinitions(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitVariableDefinitions(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableDefinitionsContext variableDefinitions() throws RecognitionException {
		VariableDefinitionsContext _localctx = new VariableDefinitionsContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_variableDefinitions);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(266);
			match(T__6);
			setState(267);
			variableDefinition();
			setState(274);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__11 || _la==COMMA) {
				{
				{
				setState(269);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==COMMA) {
					{
					setState(268);
					match(COMMA);
					}
				}

				setState(271);
				variableDefinition();
				}
				}
				setState(276);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(277);
			match(T__7);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class VariableDefinitionContext extends ParserRuleContext {
		public VariableContext variable() {
			return getRuleContext(VariableContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public DefaultValueContext defaultValue() {
			return getRuleContext(DefaultValueContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public VariableDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterVariableDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitVariableDefinition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitVariableDefinition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableDefinitionContext variableDefinition() throws RecognitionException {
		VariableDefinitionContext _localctx = new VariableDefinitionContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_variableDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(279);
			variable();
			setState(280);
			match(T__5);
			setState(281);
			type();
			setState(283);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__12) {
				{
				setState(282);
				defaultValue();
				}
			}

			setState(286);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__16) {
				{
				setState(285);
				directives();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DefaultValueContext extends ParserRuleContext {
		public ValueContext value() {
			return getRuleContext(ValueContext.class,0);
		}
		public DefaultValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_defaultValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterDefaultValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitDefaultValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitDefaultValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DefaultValueContext defaultValue() throws RecognitionException {
		DefaultValueContext _localctx = new DefaultValueContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_defaultValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(288);
			match(T__12);
			setState(289);
			value();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeContext extends ParserRuleContext {
		public NamedTypeContext namedType() {
			return getRuleContext(NamedTypeContext.class,0);
		}
		public ListTypeContext listType() {
			return getRuleContext(ListTypeContext.class,0);
		}
		public NonNullTypeContext nonNullType() {
			return getRuleContext(NonNullTypeContext.class,0);
		}
		public TypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeContext type() throws RecognitionException {
		TypeContext _localctx = new TypeContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_type);
		try {
			setState(294);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,25,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(291);
				namedType();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(292);
				listType();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(293);
				nonNullType();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class NamedTypeContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public NamedTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_namedType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterNamedType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitNamedType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitNamedType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NamedTypeContext namedType() throws RecognitionException {
		NamedTypeContext _localctx = new NamedTypeContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_namedType);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(296);
			name();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ListTypeContext extends ParserRuleContext {
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public ListTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_listType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterListType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitListType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitListType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ListTypeContext listType() throws RecognitionException {
		ListTypeContext _localctx = new ListTypeContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_listType);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(298);
			match(T__13);
			setState(299);
			type();
			setState(300);
			match(T__14);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class NonNullTypeContext extends ParserRuleContext {
		public NamedTypeContext namedType() {
			return getRuleContext(NamedTypeContext.class,0);
		}
		public ListTypeContext listType() {
			return getRuleContext(ListTypeContext.class,0);
		}
		public NonNullTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nonNullType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterNonNullType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitNonNullType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitNonNullType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NonNullTypeContext nonNullType() throws RecognitionException {
		NonNullTypeContext _localctx = new NonNullTypeContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_nonNullType);
		try {
			setState(308);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__0:
			case T__1:
			case T__2:
			case T__9:
			case T__10:
			case T__17:
			case T__19:
			case T__20:
			case T__21:
			case T__23:
			case T__24:
			case T__26:
			case T__27:
			case T__28:
			case T__29:
			case T__49:
			case T__50:
			case T__51:
			case T__52:
			case NAME:
				enterOuterAlt(_localctx, 1);
				{
				setState(302);
				namedType();
				setState(303);
				match(T__15);
				}
				break;
			case T__13:
				enterOuterAlt(_localctx, 2);
				{
				setState(305);
				listType();
				setState(306);
				match(T__15);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DirectivesContext extends ParserRuleContext {
		public List<DirectiveContext> directive() {
			return getRuleContexts(DirectiveContext.class);
		}
		public DirectiveContext directive(int i) {
			return getRuleContext(DirectiveContext.class,i);
		}
		public DirectivesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_directives; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterDirectives(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitDirectives(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitDirectives(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DirectivesContext directives() throws RecognitionException {
		DirectivesContext _localctx = new DirectivesContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_directives);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(311); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(310);
				directive();
				}
				}
				setState(313); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==T__16 );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DirectiveContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public ArgumentsContext arguments() {
			return getRuleContext(ArgumentsContext.class,0);
		}
		public DirectiveContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_directive; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterDirective(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitDirective(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitDirective(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DirectiveContext directive() throws RecognitionException {
		DirectiveContext _localctx = new DirectiveContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_directive);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(315);
			match(T__16);
			setState(316);
			name();
			setState(318);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__6) {
				{
				setState(317);
				arguments();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeSystemDefinitionContext extends ParserRuleContext {
		public SchemaDefinitionContext schemaDefinition() {
			return getRuleContext(SchemaDefinitionContext.class,0);
		}
		public TypeDefinitionContext typeDefinition() {
			return getRuleContext(TypeDefinitionContext.class,0);
		}
		public DirectiveDefinitionContext directiveDefinition() {
			return getRuleContext(DirectiveDefinitionContext.class,0);
		}
		public TypeSystemDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeSystemDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterTypeSystemDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitTypeSystemDefinition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitTypeSystemDefinition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeSystemDefinitionContext typeSystemDefinition() throws RecognitionException {
		TypeSystemDefinitionContext _localctx = new TypeSystemDefinitionContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_typeSystemDefinition);
		try {
			setState(323);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,29,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(320);
				schemaDefinition();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(321);
				typeDefinition();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(322);
				directiveDefinition();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeSystemExtensionContext extends ParserRuleContext {
		public SchemaExtensionContext schemaExtension() {
			return getRuleContext(SchemaExtensionContext.class,0);
		}
		public TypeExtensionContext typeExtension() {
			return getRuleContext(TypeExtensionContext.class,0);
		}
		public TypeSystemExtensionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeSystemExtension; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterTypeSystemExtension(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitTypeSystemExtension(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitTypeSystemExtension(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeSystemExtensionContext typeSystemExtension() throws RecognitionException {
		TypeSystemExtensionContext _localctx = new TypeSystemExtensionContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_typeSystemExtension);
		try {
			setState(327);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,30,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(325);
				schemaExtension();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(326);
				typeExtension();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SchemaDefinitionContext extends ParserRuleContext {
		public DescriptionContext description() {
			return getRuleContext(DescriptionContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public List<RootOperationTypeDefinitionContext> rootOperationTypeDefinition() {
			return getRuleContexts(RootOperationTypeDefinitionContext.class);
		}
		public RootOperationTypeDefinitionContext rootOperationTypeDefinition(int i) {
			return getRuleContext(RootOperationTypeDefinitionContext.class,i);
		}
		public SchemaDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_schemaDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterSchemaDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitSchemaDefinition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitSchemaDefinition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SchemaDefinitionContext schemaDefinition() throws RecognitionException {
		SchemaDefinitionContext _localctx = new SchemaDefinitionContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_schemaDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(330);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==STRING || _la==BLOCK_STRING) {
				{
				setState(329);
				description();
				}
			}

			setState(332);
			match(T__17);
			setState(334);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__16) {
				{
				setState(333);
				directives();
				}
			}

			setState(336);
			match(T__3);
			setState(338); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(337);
				rootOperationTypeDefinition();
				}
				}
				setState(340); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & 14L) != 0) );
			setState(342);
			match(T__4);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class RootOperationTypeDefinitionContext extends ParserRuleContext {
		public OperationTypeContext operationType() {
			return getRuleContext(OperationTypeContext.class,0);
		}
		public NamedTypeContext namedType() {
			return getRuleContext(NamedTypeContext.class,0);
		}
		public RootOperationTypeDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_rootOperationTypeDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterRootOperationTypeDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitRootOperationTypeDefinition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitRootOperationTypeDefinition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RootOperationTypeDefinitionContext rootOperationTypeDefinition() throws RecognitionException {
		RootOperationTypeDefinitionContext _localctx = new RootOperationTypeDefinitionContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_rootOperationTypeDefinition);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(344);
			operationType();
			setState(345);
			match(T__5);
			setState(346);
			namedType();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SchemaExtensionContext extends ParserRuleContext {
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public List<RootOperationTypeDefinitionContext> rootOperationTypeDefinition() {
			return getRuleContexts(RootOperationTypeDefinitionContext.class);
		}
		public RootOperationTypeDefinitionContext rootOperationTypeDefinition(int i) {
			return getRuleContext(RootOperationTypeDefinitionContext.class,i);
		}
		public SchemaExtensionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_schemaExtension; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterSchemaExtension(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitSchemaExtension(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitSchemaExtension(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SchemaExtensionContext schemaExtension() throws RecognitionException {
		SchemaExtensionContext _localctx = new SchemaExtensionContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_schemaExtension);
		int _la;
		try {
			setState(364);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,36,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(348);
				match(T__18);
				setState(349);
				match(T__17);
				setState(351);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__16) {
					{
					setState(350);
					directives();
					}
				}

				setState(353);
				match(T__3);
				setState(355); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(354);
					rootOperationTypeDefinition();
					}
					}
					setState(357); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & 14L) != 0) );
				setState(359);
				match(T__4);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(361);
				match(T__18);
				setState(362);
				match(T__17);
				setState(363);
				directives();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeDefinitionContext extends ParserRuleContext {
		public ScalarTypeDefinitionContext scalarTypeDefinition() {
			return getRuleContext(ScalarTypeDefinitionContext.class,0);
		}
		public ObjectTypeDefinitionContext objectTypeDefinition() {
			return getRuleContext(ObjectTypeDefinitionContext.class,0);
		}
		public InterfaceTypeDefinitionContext interfaceTypeDefinition() {
			return getRuleContext(InterfaceTypeDefinitionContext.class,0);
		}
		public UnionTypeDefinitionContext unionTypeDefinition() {
			return getRuleContext(UnionTypeDefinitionContext.class,0);
		}
		public EnumTypeDefinitionContext enumTypeDefinition() {
			return getRuleContext(EnumTypeDefinitionContext.class,0);
		}
		public InputObjectTypeDefinitionContext inputObjectTypeDefinition() {
			return getRuleContext(InputObjectTypeDefinitionContext.class,0);
		}
		public TypeDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterTypeDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitTypeDefinition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitTypeDefinition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeDefinitionContext typeDefinition() throws RecognitionException {
		TypeDefinitionContext _localctx = new TypeDefinitionContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_typeDefinition);
		try {
			setState(372);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,37,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(366);
				scalarTypeDefinition();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(367);
				objectTypeDefinition();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(368);
				interfaceTypeDefinition();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(369);
				unionTypeDefinition();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(370);
				enumTypeDefinition();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(371);
				inputObjectTypeDefinition();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeExtensionContext extends ParserRuleContext {
		public ScalarTypeExtensionContext scalarTypeExtension() {
			return getRuleContext(ScalarTypeExtensionContext.class,0);
		}
		public ObjectTypeExtensionContext objectTypeExtension() {
			return getRuleContext(ObjectTypeExtensionContext.class,0);
		}
		public InterfaceTypeExtensionContext interfaceTypeExtension() {
			return getRuleContext(InterfaceTypeExtensionContext.class,0);
		}
		public UnionTypeExtensionContext unionTypeExtension() {
			return getRuleContext(UnionTypeExtensionContext.class,0);
		}
		public EnumTypeExtensionContext enumTypeExtension() {
			return getRuleContext(EnumTypeExtensionContext.class,0);
		}
		public InputObjectTypeExtensionContext inputObjectTypeExtension() {
			return getRuleContext(InputObjectTypeExtensionContext.class,0);
		}
		public TypeExtensionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeExtension; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterTypeExtension(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitTypeExtension(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitTypeExtension(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeExtensionContext typeExtension() throws RecognitionException {
		TypeExtensionContext _localctx = new TypeExtensionContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_typeExtension);
		try {
			setState(380);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,38,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(374);
				scalarTypeExtension();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(375);
				objectTypeExtension();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(376);
				interfaceTypeExtension();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(377);
				unionTypeExtension();
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(378);
				enumTypeExtension();
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(379);
				inputObjectTypeExtension();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ScalarTypeDefinitionContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public DescriptionContext description() {
			return getRuleContext(DescriptionContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public ScalarTypeDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_scalarTypeDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterScalarTypeDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitScalarTypeDefinition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitScalarTypeDefinition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ScalarTypeDefinitionContext scalarTypeDefinition() throws RecognitionException {
		ScalarTypeDefinitionContext _localctx = new ScalarTypeDefinitionContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_scalarTypeDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(383);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==STRING || _la==BLOCK_STRING) {
				{
				setState(382);
				description();
				}
			}

			setState(385);
			match(T__19);
			setState(386);
			name();
			setState(388);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__16) {
				{
				setState(387);
				directives();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ScalarTypeExtensionContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public ScalarTypeExtensionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_scalarTypeExtension; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterScalarTypeExtension(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitScalarTypeExtension(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitScalarTypeExtension(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ScalarTypeExtensionContext scalarTypeExtension() throws RecognitionException {
		ScalarTypeExtensionContext _localctx = new ScalarTypeExtensionContext(_ctx, getState());
		enterRule(_localctx, 70, RULE_scalarTypeExtension);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(390);
			match(T__18);
			setState(391);
			match(T__19);
			setState(392);
			name();
			setState(393);
			directives();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ObjectTypeDefinitionContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public DescriptionContext description() {
			return getRuleContext(DescriptionContext.class,0);
		}
		public ImplementsInterfacesContext implementsInterfaces() {
			return getRuleContext(ImplementsInterfacesContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public FieldsDefinitionContext fieldsDefinition() {
			return getRuleContext(FieldsDefinitionContext.class,0);
		}
		public ObjectTypeDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_objectTypeDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterObjectTypeDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitObjectTypeDefinition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitObjectTypeDefinition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ObjectTypeDefinitionContext objectTypeDefinition() throws RecognitionException {
		ObjectTypeDefinitionContext _localctx = new ObjectTypeDefinitionContext(_ctx, getState());
		enterRule(_localctx, 72, RULE_objectTypeDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(396);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==STRING || _la==BLOCK_STRING) {
				{
				setState(395);
				description();
				}
			}

			setState(398);
			match(T__20);
			setState(399);
			name();
			setState(401);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__21) {
				{
				setState(400);
				implementsInterfaces(0);
				}
			}

			setState(404);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__16) {
				{
				setState(403);
				directives();
				}
			}

			setState(407);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,44,_ctx) ) {
			case 1:
				{
				setState(406);
				fieldsDefinition();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ObjectTypeExtensionContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public FieldsDefinitionContext fieldsDefinition() {
			return getRuleContext(FieldsDefinitionContext.class,0);
		}
		public ImplementsInterfacesContext implementsInterfaces() {
			return getRuleContext(ImplementsInterfacesContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public ObjectTypeExtensionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_objectTypeExtension; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterObjectTypeExtension(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitObjectTypeExtension(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitObjectTypeExtension(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ObjectTypeExtensionContext objectTypeExtension() throws RecognitionException {
		ObjectTypeExtensionContext _localctx = new ObjectTypeExtensionContext(_ctx, getState());
		enterRule(_localctx, 74, RULE_objectTypeExtension);
		int _la;
		try {
			setState(433);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,48,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(409);
				match(T__18);
				setState(410);
				match(T__20);
				setState(411);
				name();
				setState(413);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__21) {
					{
					setState(412);
					implementsInterfaces(0);
					}
				}

				setState(416);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__16) {
					{
					setState(415);
					directives();
					}
				}

				setState(418);
				fieldsDefinition();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(420);
				match(T__18);
				setState(421);
				match(T__20);
				setState(422);
				name();
				setState(424);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__21) {
					{
					setState(423);
					implementsInterfaces(0);
					}
				}

				setState(426);
				directives();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(428);
				match(T__18);
				setState(429);
				match(T__20);
				setState(430);
				name();
				setState(431);
				implementsInterfaces(0);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ImplementsInterfacesContext extends ParserRuleContext {
		public NamedTypeContext namedType() {
			return getRuleContext(NamedTypeContext.class,0);
		}
		public ImplementsInterfacesContext implementsInterfaces() {
			return getRuleContext(ImplementsInterfacesContext.class,0);
		}
		public ImplementsInterfacesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_implementsInterfaces; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterImplementsInterfaces(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitImplementsInterfaces(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitImplementsInterfaces(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ImplementsInterfacesContext implementsInterfaces() throws RecognitionException {
		return implementsInterfaces(0);
	}

	private ImplementsInterfacesContext implementsInterfaces(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ImplementsInterfacesContext _localctx = new ImplementsInterfacesContext(_ctx, _parentState);
		ImplementsInterfacesContext _prevctx = _localctx;
		int _startState = 76;
		enterRecursionRule(_localctx, 76, RULE_implementsInterfaces, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(436);
			match(T__21);
			setState(438);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__22) {
				{
				setState(437);
				match(T__22);
				}
			}

			setState(440);
			namedType();
			}
			_ctx.stop = _input.LT(-1);
			setState(447);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,50,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new ImplementsInterfacesContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_implementsInterfaces);
					setState(442);
					if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
					setState(443);
					match(T__22);
					setState(444);
					namedType();
					}
					} 
				}
				setState(449);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,50,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FieldsDefinitionContext extends ParserRuleContext {
		public List<FieldDefinitionContext> fieldDefinition() {
			return getRuleContexts(FieldDefinitionContext.class);
		}
		public FieldDefinitionContext fieldDefinition(int i) {
			return getRuleContext(FieldDefinitionContext.class,i);
		}
		public FieldsDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fieldsDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterFieldsDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitFieldsDefinition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitFieldsDefinition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FieldsDefinitionContext fieldsDefinition() throws RecognitionException {
		FieldsDefinitionContext _localctx = new FieldsDefinitionContext(_ctx, getState());
		enterRule(_localctx, 78, RULE_fieldsDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(450);
			match(T__3);
			setState(454);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 899594027638459406L) != 0)) {
				{
				{
				setState(451);
				fieldDefinition();
				}
				}
				setState(456);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(457);
			match(T__4);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FieldDefinitionContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public DescriptionContext description() {
			return getRuleContext(DescriptionContext.class,0);
		}
		public ArgumentsDefinitionContext argumentsDefinition() {
			return getRuleContext(ArgumentsDefinitionContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public FieldDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fieldDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterFieldDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitFieldDefinition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitFieldDefinition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FieldDefinitionContext fieldDefinition() throws RecognitionException {
		FieldDefinitionContext _localctx = new FieldDefinitionContext(_ctx, getState());
		enterRule(_localctx, 80, RULE_fieldDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(460);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==STRING || _la==BLOCK_STRING) {
				{
				setState(459);
				description();
				}
			}

			setState(462);
			name();
			setState(464);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__6) {
				{
				setState(463);
				argumentsDefinition();
				}
			}

			setState(466);
			match(T__5);
			setState(467);
			type();
			setState(469);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__16) {
				{
				setState(468);
				directives();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ArgumentsDefinitionContext extends ParserRuleContext {
		public List<InputValueDefinitionContext> inputValueDefinition() {
			return getRuleContexts(InputValueDefinitionContext.class);
		}
		public InputValueDefinitionContext inputValueDefinition(int i) {
			return getRuleContext(InputValueDefinitionContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(GraphQLParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(GraphQLParser.COMMA, i);
		}
		public ArgumentsDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_argumentsDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterArgumentsDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitArgumentsDefinition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitArgumentsDefinition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArgumentsDefinitionContext argumentsDefinition() throws RecognitionException {
		ArgumentsDefinitionContext _localctx = new ArgumentsDefinitionContext(_ctx, getState());
		enterRule(_localctx, 82, RULE_argumentsDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(471);
			match(T__6);
			setState(472);
			inputValueDefinition();
			setState(479);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 5511280046065847310L) != 0)) {
				{
				{
				setState(474);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==COMMA) {
					{
					setState(473);
					match(COMMA);
					}
				}

				setState(476);
				inputValueDefinition();
				}
				}
				setState(481);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(482);
			match(T__7);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class InputValueDefinitionContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public DescriptionContext description() {
			return getRuleContext(DescriptionContext.class,0);
		}
		public DefaultValueContext defaultValue() {
			return getRuleContext(DefaultValueContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public InputValueDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inputValueDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterInputValueDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitInputValueDefinition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitInputValueDefinition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InputValueDefinitionContext inputValueDefinition() throws RecognitionException {
		InputValueDefinitionContext _localctx = new InputValueDefinitionContext(_ctx, getState());
		enterRule(_localctx, 84, RULE_inputValueDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(485);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==STRING || _la==BLOCK_STRING) {
				{
				setState(484);
				description();
				}
			}

			setState(487);
			name();
			setState(488);
			match(T__5);
			setState(489);
			type();
			setState(491);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__12) {
				{
				setState(490);
				defaultValue();
				}
			}

			setState(494);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__16) {
				{
				setState(493);
				directives();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class InterfaceTypeDefinitionContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public DescriptionContext description() {
			return getRuleContext(DescriptionContext.class,0);
		}
		public ImplementsInterfacesContext implementsInterfaces() {
			return getRuleContext(ImplementsInterfacesContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public FieldsDefinitionContext fieldsDefinition() {
			return getRuleContext(FieldsDefinitionContext.class,0);
		}
		public InterfaceTypeDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interfaceTypeDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterInterfaceTypeDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitInterfaceTypeDefinition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitInterfaceTypeDefinition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InterfaceTypeDefinitionContext interfaceTypeDefinition() throws RecognitionException {
		InterfaceTypeDefinitionContext _localctx = new InterfaceTypeDefinitionContext(_ctx, getState());
		enterRule(_localctx, 86, RULE_interfaceTypeDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(497);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==STRING || _la==BLOCK_STRING) {
				{
				setState(496);
				description();
				}
			}

			setState(499);
			match(T__23);
			setState(500);
			name();
			setState(502);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__21) {
				{
				setState(501);
				implementsInterfaces(0);
				}
			}

			setState(505);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__16) {
				{
				setState(504);
				directives();
				}
			}

			setState(508);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,63,_ctx) ) {
			case 1:
				{
				setState(507);
				fieldsDefinition();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class InterfaceTypeExtensionContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public FieldsDefinitionContext fieldsDefinition() {
			return getRuleContext(FieldsDefinitionContext.class,0);
		}
		public ImplementsInterfacesContext implementsInterfaces() {
			return getRuleContext(ImplementsInterfacesContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public InterfaceTypeExtensionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interfaceTypeExtension; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterInterfaceTypeExtension(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitInterfaceTypeExtension(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitInterfaceTypeExtension(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InterfaceTypeExtensionContext interfaceTypeExtension() throws RecognitionException {
		InterfaceTypeExtensionContext _localctx = new InterfaceTypeExtensionContext(_ctx, getState());
		enterRule(_localctx, 88, RULE_interfaceTypeExtension);
		int _la;
		try {
			setState(534);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,67,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(510);
				match(T__18);
				setState(511);
				match(T__23);
				setState(512);
				name();
				setState(514);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__21) {
					{
					setState(513);
					implementsInterfaces(0);
					}
				}

				setState(517);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__16) {
					{
					setState(516);
					directives();
					}
				}

				setState(519);
				fieldsDefinition();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(521);
				match(T__18);
				setState(522);
				match(T__23);
				setState(523);
				name();
				setState(525);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__21) {
					{
					setState(524);
					implementsInterfaces(0);
					}
				}

				setState(527);
				directives();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(529);
				match(T__18);
				setState(530);
				match(T__23);
				setState(531);
				name();
				setState(532);
				implementsInterfaces(0);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class UnionTypeDefinitionContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public DescriptionContext description() {
			return getRuleContext(DescriptionContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public UnionMemberTypesContext unionMemberTypes() {
			return getRuleContext(UnionMemberTypesContext.class,0);
		}
		public UnionTypeDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unionTypeDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterUnionTypeDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitUnionTypeDefinition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitUnionTypeDefinition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UnionTypeDefinitionContext unionTypeDefinition() throws RecognitionException {
		UnionTypeDefinitionContext _localctx = new UnionTypeDefinitionContext(_ctx, getState());
		enterRule(_localctx, 90, RULE_unionTypeDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(537);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==STRING || _la==BLOCK_STRING) {
				{
				setState(536);
				description();
				}
			}

			setState(539);
			match(T__24);
			setState(540);
			name();
			setState(542);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__16) {
				{
				setState(541);
				directives();
				}
			}

			setState(545);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__12) {
				{
				setState(544);
				unionMemberTypes(0);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class UnionMemberTypesContext extends ParserRuleContext {
		public NamedTypeContext namedType() {
			return getRuleContext(NamedTypeContext.class,0);
		}
		public UnionMemberTypesContext unionMemberTypes() {
			return getRuleContext(UnionMemberTypesContext.class,0);
		}
		public UnionMemberTypesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unionMemberTypes; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterUnionMemberTypes(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitUnionMemberTypes(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitUnionMemberTypes(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UnionMemberTypesContext unionMemberTypes() throws RecognitionException {
		return unionMemberTypes(0);
	}

	private UnionMemberTypesContext unionMemberTypes(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		UnionMemberTypesContext _localctx = new UnionMemberTypesContext(_ctx, _parentState);
		UnionMemberTypesContext _prevctx = _localctx;
		int _startState = 92;
		enterRecursionRule(_localctx, 92, RULE_unionMemberTypes, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(548);
			match(T__12);
			setState(550);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__25) {
				{
				setState(549);
				match(T__25);
				}
			}

			setState(552);
			namedType();
			}
			_ctx.stop = _input.LT(-1);
			setState(559);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,72,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new UnionMemberTypesContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_unionMemberTypes);
					setState(554);
					if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
					setState(555);
					match(T__25);
					setState(556);
					namedType();
					}
					} 
				}
				setState(561);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,72,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class UnionTypeExtensionContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public UnionMemberTypesContext unionMemberTypes() {
			return getRuleContext(UnionMemberTypesContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public UnionTypeExtensionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_unionTypeExtension; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterUnionTypeExtension(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitUnionTypeExtension(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitUnionTypeExtension(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UnionTypeExtensionContext unionTypeExtension() throws RecognitionException {
		UnionTypeExtensionContext _localctx = new UnionTypeExtensionContext(_ctx, getState());
		enterRule(_localctx, 94, RULE_unionTypeExtension);
		int _la;
		try {
			setState(575);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,74,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(562);
				match(T__18);
				setState(563);
				match(T__24);
				setState(564);
				name();
				setState(566);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__16) {
					{
					setState(565);
					directives();
					}
				}

				setState(568);
				unionMemberTypes(0);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(570);
				match(T__18);
				setState(571);
				match(T__24);
				setState(572);
				name();
				setState(573);
				directives();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class EnumTypeDefinitionContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public DescriptionContext description() {
			return getRuleContext(DescriptionContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public EnumValuesDefinitionContext enumValuesDefinition() {
			return getRuleContext(EnumValuesDefinitionContext.class,0);
		}
		public EnumTypeDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enumTypeDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterEnumTypeDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitEnumTypeDefinition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitEnumTypeDefinition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EnumTypeDefinitionContext enumTypeDefinition() throws RecognitionException {
		EnumTypeDefinitionContext _localctx = new EnumTypeDefinitionContext(_ctx, getState());
		enterRule(_localctx, 96, RULE_enumTypeDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(578);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==STRING || _la==BLOCK_STRING) {
				{
				setState(577);
				description();
				}
			}

			setState(580);
			match(T__26);
			setState(581);
			name();
			setState(583);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__16) {
				{
				setState(582);
				directives();
				}
			}

			setState(586);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,77,_ctx) ) {
			case 1:
				{
				setState(585);
				enumValuesDefinition();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class EnumValuesDefinitionContext extends ParserRuleContext {
		public List<EnumValueDefinitionContext> enumValueDefinition() {
			return getRuleContexts(EnumValueDefinitionContext.class);
		}
		public EnumValueDefinitionContext enumValueDefinition(int i) {
			return getRuleContext(EnumValueDefinitionContext.class,i);
		}
		public EnumValuesDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enumValuesDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterEnumValuesDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitEnumValuesDefinition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitEnumValuesDefinition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EnumValuesDefinitionContext enumValuesDefinition() throws RecognitionException {
		EnumValuesDefinitionContext _localctx = new EnumValuesDefinitionContext(_ctx, getState());
		enterRule(_localctx, 98, RULE_enumValuesDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(588);
			match(T__3);
			setState(592);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 899594027638459406L) != 0)) {
				{
				{
				setState(589);
				enumValueDefinition();
				}
				}
				setState(594);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(595);
			match(T__4);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class EnumValueDefinitionContext extends ParserRuleContext {
		public EnumValueContext enumValue() {
			return getRuleContext(EnumValueContext.class,0);
		}
		public DescriptionContext description() {
			return getRuleContext(DescriptionContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public EnumValueDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enumValueDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterEnumValueDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitEnumValueDefinition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitEnumValueDefinition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EnumValueDefinitionContext enumValueDefinition() throws RecognitionException {
		EnumValueDefinitionContext _localctx = new EnumValueDefinitionContext(_ctx, getState());
		enterRule(_localctx, 100, RULE_enumValueDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(598);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==STRING || _la==BLOCK_STRING) {
				{
				setState(597);
				description();
				}
			}

			setState(600);
			enumValue();
			setState(602);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__16) {
				{
				setState(601);
				directives();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class EnumTypeExtensionContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public EnumValuesDefinitionContext enumValuesDefinition() {
			return getRuleContext(EnumValuesDefinitionContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public EnumTypeExtensionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enumTypeExtension; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterEnumTypeExtension(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitEnumTypeExtension(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitEnumTypeExtension(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EnumTypeExtensionContext enumTypeExtension() throws RecognitionException {
		EnumTypeExtensionContext _localctx = new EnumTypeExtensionContext(_ctx, getState());
		enterRule(_localctx, 102, RULE_enumTypeExtension);
		int _la;
		try {
			setState(617);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,82,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(604);
				match(T__18);
				setState(605);
				match(T__26);
				setState(606);
				name();
				setState(608);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__16) {
					{
					setState(607);
					directives();
					}
				}

				setState(610);
				enumValuesDefinition();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(612);
				match(T__18);
				setState(613);
				match(T__26);
				setState(614);
				name();
				setState(615);
				directives();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class InputObjectTypeDefinitionContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public DescriptionContext description() {
			return getRuleContext(DescriptionContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public InputFieldsDefinitionContext inputFieldsDefinition() {
			return getRuleContext(InputFieldsDefinitionContext.class,0);
		}
		public InputObjectTypeDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inputObjectTypeDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterInputObjectTypeDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitInputObjectTypeDefinition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitInputObjectTypeDefinition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InputObjectTypeDefinitionContext inputObjectTypeDefinition() throws RecognitionException {
		InputObjectTypeDefinitionContext _localctx = new InputObjectTypeDefinitionContext(_ctx, getState());
		enterRule(_localctx, 104, RULE_inputObjectTypeDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(620);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==STRING || _la==BLOCK_STRING) {
				{
				setState(619);
				description();
				}
			}

			setState(622);
			match(T__27);
			setState(623);
			name();
			setState(625);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__16) {
				{
				setState(624);
				directives();
				}
			}

			setState(628);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,85,_ctx) ) {
			case 1:
				{
				setState(627);
				inputFieldsDefinition();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class InputFieldsDefinitionContext extends ParserRuleContext {
		public List<InputValueDefinitionContext> inputValueDefinition() {
			return getRuleContexts(InputValueDefinitionContext.class);
		}
		public InputValueDefinitionContext inputValueDefinition(int i) {
			return getRuleContext(InputValueDefinitionContext.class,i);
		}
		public InputFieldsDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inputFieldsDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterInputFieldsDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitInputFieldsDefinition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitInputFieldsDefinition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InputFieldsDefinitionContext inputFieldsDefinition() throws RecognitionException {
		InputFieldsDefinitionContext _localctx = new InputFieldsDefinitionContext(_ctx, getState());
		enterRule(_localctx, 106, RULE_inputFieldsDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(630);
			match(T__3);
			setState(634);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 899594027638459406L) != 0)) {
				{
				{
				setState(631);
				inputValueDefinition();
				}
				}
				setState(636);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(637);
			match(T__4);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class InputObjectTypeExtensionContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public InputFieldsDefinitionContext inputFieldsDefinition() {
			return getRuleContext(InputFieldsDefinitionContext.class,0);
		}
		public DirectivesContext directives() {
			return getRuleContext(DirectivesContext.class,0);
		}
		public InputObjectTypeExtensionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inputObjectTypeExtension; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterInputObjectTypeExtension(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitInputObjectTypeExtension(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitInputObjectTypeExtension(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InputObjectTypeExtensionContext inputObjectTypeExtension() throws RecognitionException {
		InputObjectTypeExtensionContext _localctx = new InputObjectTypeExtensionContext(_ctx, getState());
		enterRule(_localctx, 108, RULE_inputObjectTypeExtension);
		int _la;
		try {
			setState(652);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,88,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(639);
				match(T__18);
				setState(640);
				match(T__27);
				setState(641);
				name();
				setState(643);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__16) {
					{
					setState(642);
					directives();
					}
				}

				setState(645);
				inputFieldsDefinition();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(647);
				match(T__18);
				setState(648);
				match(T__27);
				setState(649);
				name();
				setState(650);
				directives();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DirectiveDefinitionContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public DirectiveLocationsContext directiveLocations() {
			return getRuleContext(DirectiveLocationsContext.class,0);
		}
		public DescriptionContext description() {
			return getRuleContext(DescriptionContext.class,0);
		}
		public ArgumentsDefinitionContext argumentsDefinition() {
			return getRuleContext(ArgumentsDefinitionContext.class,0);
		}
		public DirectiveDefinitionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_directiveDefinition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterDirectiveDefinition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitDirectiveDefinition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitDirectiveDefinition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DirectiveDefinitionContext directiveDefinition() throws RecognitionException {
		DirectiveDefinitionContext _localctx = new DirectiveDefinitionContext(_ctx, getState());
		enterRule(_localctx, 110, RULE_directiveDefinition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(655);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==STRING || _la==BLOCK_STRING) {
				{
				setState(654);
				description();
				}
			}

			setState(657);
			match(T__28);
			setState(658);
			match(T__16);
			setState(659);
			name();
			setState(661);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__6) {
				{
				setState(660);
				argumentsDefinition();
				}
			}

			setState(664);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__29) {
				{
				setState(663);
				match(T__29);
				}
			}

			setState(666);
			match(T__10);
			setState(667);
			directiveLocations(0);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DirectiveLocationsContext extends ParserRuleContext {
		public DirectiveLocationContext directiveLocation() {
			return getRuleContext(DirectiveLocationContext.class,0);
		}
		public DirectiveLocationsContext directiveLocations() {
			return getRuleContext(DirectiveLocationsContext.class,0);
		}
		public DirectiveLocationsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_directiveLocations; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterDirectiveLocations(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitDirectiveLocations(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitDirectiveLocations(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DirectiveLocationsContext directiveLocations() throws RecognitionException {
		return directiveLocations(0);
	}

	private DirectiveLocationsContext directiveLocations(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		DirectiveLocationsContext _localctx = new DirectiveLocationsContext(_ctx, _parentState);
		DirectiveLocationsContext _prevctx = _localctx;
		int _startState = 112;
		enterRecursionRule(_localctx, 112, RULE_directiveLocations, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(671);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__25) {
				{
				setState(670);
				match(T__25);
				}
			}

			setState(673);
			directiveLocation();
			}
			_ctx.stop = _input.LT(-1);
			setState(680);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,93,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new DirectiveLocationsContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_directiveLocations);
					setState(675);
					if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
					setState(676);
					match(T__25);
					setState(677);
					directiveLocation();
					}
					} 
				}
				setState(682);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,93,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DirectiveLocationContext extends ParserRuleContext {
		public ExecutableDirectiveLocationContext executableDirectiveLocation() {
			return getRuleContext(ExecutableDirectiveLocationContext.class,0);
		}
		public TypeSystemDirectiveLocationContext typeSystemDirectiveLocation() {
			return getRuleContext(TypeSystemDirectiveLocationContext.class,0);
		}
		public DirectiveLocationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_directiveLocation; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterDirectiveLocation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitDirectiveLocation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitDirectiveLocation(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DirectiveLocationContext directiveLocation() throws RecognitionException {
		DirectiveLocationContext _localctx = new DirectiveLocationContext(_ctx, getState());
		enterRule(_localctx, 114, RULE_directiveLocation);
		try {
			setState(685);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__30:
			case T__31:
			case T__32:
			case T__33:
			case T__34:
			case T__35:
			case T__36:
			case T__37:
				enterOuterAlt(_localctx, 1);
				{
				setState(683);
				executableDirectiveLocation();
				}
				break;
			case T__38:
			case T__39:
			case T__40:
			case T__41:
			case T__42:
			case T__43:
			case T__44:
			case T__45:
			case T__46:
			case T__47:
			case T__48:
				enterOuterAlt(_localctx, 2);
				{
				setState(684);
				typeSystemDirectiveLocation();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExecutableDirectiveLocationContext extends ParserRuleContext {
		public ExecutableDirectiveLocationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_executableDirectiveLocation; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterExecutableDirectiveLocation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitExecutableDirectiveLocation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitExecutableDirectiveLocation(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExecutableDirectiveLocationContext executableDirectiveLocation() throws RecognitionException {
		ExecutableDirectiveLocationContext _localctx = new ExecutableDirectiveLocationContext(_ctx, getState());
		enterRule(_localctx, 116, RULE_executableDirectiveLocation);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(687);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 547608330240L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TypeSystemDirectiveLocationContext extends ParserRuleContext {
		public TypeSystemDirectiveLocationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeSystemDirectiveLocation; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterTypeSystemDirectiveLocation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitTypeSystemDirectiveLocation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitTypeSystemDirectiveLocation(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeSystemDirectiveLocationContext typeSystemDirectiveLocation() throws RecognitionException {
		TypeSystemDirectiveLocationContext _localctx = new TypeSystemDirectiveLocationContext(_ctx, getState());
		enterRule(_localctx, 118, RULE_typeSystemDirectiveLocation);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(689);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 1125350151028736L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class NameContext extends ParserRuleContext {
		public TerminalNode NAME() { return getToken(GraphQLParser.NAME, 0); }
		public NameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_name; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NameContext name() throws RecognitionException {
		NameContext _localctx = new NameContext(_ctx, getState());
		enterRule(_localctx, 120, RULE_name);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(691);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 34902899183324174L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DescriptionContext extends ParserRuleContext {
		public StringValueContext stringValue() {
			return getRuleContext(StringValueContext.class,0);
		}
		public DescriptionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_description; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterDescription(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitDescription(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitDescription(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DescriptionContext description() throws RecognitionException {
		DescriptionContext _localctx = new DescriptionContext(_ctx, getState());
		enterRule(_localctx, 122, RULE_description);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(693);
			stringValue();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class BooleanValueContext extends ParserRuleContext {
		public BooleanValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_booleanValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterBooleanValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitBooleanValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitBooleanValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BooleanValueContext booleanValue() throws RecognitionException {
		BooleanValueContext _localctx = new BooleanValueContext(_ctx, getState());
		enterRule(_localctx, 124, RULE_booleanValue);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(695);
			_la = _input.LA(1);
			if ( !(_la==T__49 || _la==T__50) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class NullValueContext extends ParserRuleContext {
		public NullValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nullValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterNullValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitNullValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitNullValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NullValueContext nullValue() throws RecognitionException {
		NullValueContext _localctx = new NullValueContext(_ctx, getState());
		enterRule(_localctx, 126, RULE_nullValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(697);
			match(T__51);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class EnumValueContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public EnumValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enumValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterEnumValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitEnumValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitEnumValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EnumValueContext enumValue() throws RecognitionException {
		EnumValueContext _localctx = new EnumValueContext(_ctx, getState());
		enterRule(_localctx, 128, RULE_enumValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(699);
			name();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ListValueContext extends ParserRuleContext {
		public List<ValueContext> value() {
			return getRuleContexts(ValueContext.class);
		}
		public ValueContext value(int i) {
			return getRuleContext(ValueContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(GraphQLParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(GraphQLParser.COMMA, i);
		}
		public ListValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_listValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterListValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitListValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitListValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ListValueContext listValue() throws RecognitionException {
		ListValueContext _localctx = new ListValueContext(_ctx, getState());
		enterRule(_localctx, 130, RULE_listValue);
		int _la;
		try {
			setState(716);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,97,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(701);
				match(T__13);
				setState(702);
				match(T__14);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(703);
				match(T__13);
				setState(704);
				value();
				setState(711);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 5619366437122759710L) != 0)) {
					{
					{
					setState(706);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==COMMA) {
						{
						setState(705);
						match(COMMA);
						}
					}

					setState(708);
					value();
					}
					}
					setState(713);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(714);
				match(T__14);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ObjectValueContext extends ParserRuleContext {
		public List<ObjectFieldContext> objectField() {
			return getRuleContexts(ObjectFieldContext.class);
		}
		public ObjectFieldContext objectField(int i) {
			return getRuleContext(ObjectFieldContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(GraphQLParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(GraphQLParser.COMMA, i);
		}
		public ObjectValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_objectValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterObjectValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitObjectValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitObjectValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ObjectValueContext objectValue() throws RecognitionException {
		ObjectValueContext _localctx = new ObjectValueContext(_ctx, getState());
		enterRule(_localctx, 132, RULE_objectValue);
		int _la;
		try {
			setState(733);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,100,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(718);
				match(T__3);
				setState(719);
				match(T__4);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(720);
				match(T__3);
				setState(721);
				objectField();
				setState(728);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 4646588917610712078L) != 0)) {
					{
					{
					setState(723);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==COMMA) {
						{
						setState(722);
						match(COMMA);
						}
					}

					setState(725);
					objectField();
					}
					}
					setState(730);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(731);
				match(T__4);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ObjectFieldContext extends ParserRuleContext {
		public NameContext name() {
			return getRuleContext(NameContext.class,0);
		}
		public ValueContext value() {
			return getRuleContext(ValueContext.class,0);
		}
		public ObjectFieldContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_objectField; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterObjectField(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitObjectField(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitObjectField(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ObjectFieldContext objectField() throws RecognitionException {
		ObjectFieldContext _localctx = new ObjectFieldContext(_ctx, getState());
		enterRule(_localctx, 134, RULE_objectField);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(735);
			name();
			setState(736);
			match(T__5);
			setState(737);
			value();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class IntValueContext extends ParserRuleContext {
		public TerminalNode INT() { return getToken(GraphQLParser.INT, 0); }
		public IntValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_intValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterIntValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitIntValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitIntValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IntValueContext intValue() throws RecognitionException {
		IntValueContext _localctx = new IntValueContext(_ctx, getState());
		enterRule(_localctx, 136, RULE_intValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(739);
			match(INT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FloatValueContext extends ParserRuleContext {
		public TerminalNode FLOAT() { return getToken(GraphQLParser.FLOAT, 0); }
		public FloatValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_floatValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterFloatValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitFloatValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitFloatValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FloatValueContext floatValue() throws RecognitionException {
		FloatValueContext _localctx = new FloatValueContext(_ctx, getState());
		enterRule(_localctx, 138, RULE_floatValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(741);
			match(FLOAT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StringValueContext extends ParserRuleContext {
		public TerminalNode STRING() { return getToken(GraphQLParser.STRING, 0); }
		public TerminalNode BLOCK_STRING() { return getToken(GraphQLParser.BLOCK_STRING, 0); }
		public StringValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stringValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).enterStringValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GraphQLListener ) ((GraphQLListener)listener).exitStringValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GraphQLVisitor ) return ((GraphQLVisitor<? extends T>)visitor).visitStringValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StringValueContext stringValue() throws RecognitionException {
		StringValueContext _localctx = new StringValueContext(_ctx, getState());
		enterRule(_localctx, 140, RULE_stringValue);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(743);
			_la = _input.LA(1);
			if ( !(_la==STRING || _la==BLOCK_STRING) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 38:
			return implementsInterfaces_sempred((ImplementsInterfacesContext)_localctx, predIndex);
		case 46:
			return unionMemberTypes_sempred((UnionMemberTypesContext)_localctx, predIndex);
		case 56:
			return directiveLocations_sempred((DirectiveLocationsContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean implementsInterfaces_sempred(ImplementsInterfacesContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean unionMemberTypes_sempred(UnionMemberTypesContext _localctx, int predIndex) {
		switch (predIndex) {
		case 1:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean directiveLocations_sempred(DirectiveLocationsContext _localctx, int predIndex) {
		switch (predIndex) {
		case 2:
			return precpred(_ctx, 1);
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u0001?\u02ea\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
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
		"7\u00077\u00028\u00078\u00029\u00079\u0002:\u0007:\u0002;\u0007;\u0002"+
		"<\u0007<\u0002=\u0007=\u0002>\u0007>\u0002?\u0007?\u0002@\u0007@\u0002"+
		"A\u0007A\u0002B\u0007B\u0002C\u0007C\u0002D\u0007D\u0002E\u0007E\u0002"+
		"F\u0007F\u0001\u0000\u0005\u0000\u0090\b\u0000\n\u0000\f\u0000\u0093\t"+
		"\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0003"+
		"\u0001\u009a\b\u0001\u0001\u0002\u0001\u0002\u0003\u0002\u009e\b\u0002"+
		"\u0001\u0003\u0001\u0003\u0003\u0003\u00a2\b\u0003\u0001\u0003\u0003\u0003"+
		"\u00a5\b\u0003\u0001\u0003\u0003\u0003\u00a8\b\u0003\u0001\u0003\u0001"+
		"\u0003\u0001\u0003\u0003\u0003\u00ad\b\u0003\u0001\u0004\u0001\u0004\u0001"+
		"\u0005\u0001\u0005\u0005\u0005\u00b3\b\u0005\n\u0005\f\u0005\u00b6\t\u0005"+
		"\u0001\u0005\u0001\u0005\u0001\u0006\u0001\u0006\u0001\u0006\u0003\u0006"+
		"\u00bd\b\u0006\u0001\u0007\u0003\u0007\u00c0\b\u0007\u0001\u0007\u0001"+
		"\u0007\u0003\u0007\u00c4\b\u0007\u0001\u0007\u0003\u0007\u00c7\b\u0007"+
		"\u0001\u0007\u0003\u0007\u00ca\b\u0007\u0001\b\u0001\b\u0001\b\u0001\t"+
		"\u0001\t\u0001\t\u0003\t\u00d2\b\t\u0001\t\u0005\t\u00d5\b\t\n\t\f\t\u00d8"+
		"\t\t\u0003\t\u00da\b\t\u0001\t\u0001\t\u0001\n\u0001\n\u0001\n\u0001\n"+
		"\u0001\u000b\u0001\u000b\u0001\u000b\u0003\u000b\u00e5\b\u000b\u0001\f"+
		"\u0001\f\u0003\f\u00e9\b\f\u0001\f\u0003\f\u00ec\b\f\u0001\f\u0001\f\u0001"+
		"\r\u0001\r\u0001\r\u0001\r\u0003\r\u00f4\b\r\u0001\r\u0001\r\u0001\u000e"+
		"\u0001\u000e\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u0010\u0001\u0010"+
		"\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010"+
		"\u0001\u0010\u0003\u0010\u0106\b\u0010\u0001\u0011\u0001\u0011\u0001\u0011"+
		"\u0001\u0012\u0001\u0012\u0001\u0012\u0003\u0012\u010e\b\u0012\u0001\u0012"+
		"\u0005\u0012\u0111\b\u0012\n\u0012\f\u0012\u0114\t\u0012\u0001\u0012\u0001"+
		"\u0012\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0003\u0013\u011c"+
		"\b\u0013\u0001\u0013\u0003\u0013\u011f\b\u0013\u0001\u0014\u0001\u0014"+
		"\u0001\u0014\u0001\u0015\u0001\u0015\u0001\u0015\u0003\u0015\u0127\b\u0015"+
		"\u0001\u0016\u0001\u0016\u0001\u0017\u0001\u0017\u0001\u0017\u0001\u0017"+
		"\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018\u0001\u0018"+
		"\u0003\u0018\u0135\b\u0018\u0001\u0019\u0004\u0019\u0138\b\u0019\u000b"+
		"\u0019\f\u0019\u0139\u0001\u001a\u0001\u001a\u0001\u001a\u0003\u001a\u013f"+
		"\b\u001a\u0001\u001b\u0001\u001b\u0001\u001b\u0003\u001b\u0144\b\u001b"+
		"\u0001\u001c\u0001\u001c\u0003\u001c\u0148\b\u001c\u0001\u001d\u0003\u001d"+
		"\u014b\b\u001d\u0001\u001d\u0001\u001d\u0003\u001d\u014f\b\u001d\u0001"+
		"\u001d\u0001\u001d\u0004\u001d\u0153\b\u001d\u000b\u001d\f\u001d\u0154"+
		"\u0001\u001d\u0001\u001d\u0001\u001e\u0001\u001e\u0001\u001e\u0001\u001e"+
		"\u0001\u001f\u0001\u001f\u0001\u001f\u0003\u001f\u0160\b\u001f\u0001\u001f"+
		"\u0001\u001f\u0004\u001f\u0164\b\u001f\u000b\u001f\f\u001f\u0165\u0001"+
		"\u001f\u0001\u001f\u0001\u001f\u0001\u001f\u0001\u001f\u0003\u001f\u016d"+
		"\b\u001f\u0001 \u0001 \u0001 \u0001 \u0001 \u0001 \u0003 \u0175\b \u0001"+
		"!\u0001!\u0001!\u0001!\u0001!\u0001!\u0003!\u017d\b!\u0001\"\u0003\"\u0180"+
		"\b\"\u0001\"\u0001\"\u0001\"\u0003\"\u0185\b\"\u0001#\u0001#\u0001#\u0001"+
		"#\u0001#\u0001$\u0003$\u018d\b$\u0001$\u0001$\u0001$\u0003$\u0192\b$\u0001"+
		"$\u0003$\u0195\b$\u0001$\u0003$\u0198\b$\u0001%\u0001%\u0001%\u0001%\u0003"+
		"%\u019e\b%\u0001%\u0003%\u01a1\b%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001"+
		"%\u0003%\u01a9\b%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001%\u0001%\u0003"+
		"%\u01b2\b%\u0001&\u0001&\u0001&\u0003&\u01b7\b&\u0001&\u0001&\u0001&\u0001"+
		"&\u0001&\u0005&\u01be\b&\n&\f&\u01c1\t&\u0001\'\u0001\'\u0005\'\u01c5"+
		"\b\'\n\'\f\'\u01c8\t\'\u0001\'\u0001\'\u0001(\u0003(\u01cd\b(\u0001(\u0001"+
		"(\u0003(\u01d1\b(\u0001(\u0001(\u0001(\u0003(\u01d6\b(\u0001)\u0001)\u0001"+
		")\u0003)\u01db\b)\u0001)\u0005)\u01de\b)\n)\f)\u01e1\t)\u0001)\u0001)"+
		"\u0001*\u0003*\u01e6\b*\u0001*\u0001*\u0001*\u0001*\u0003*\u01ec\b*\u0001"+
		"*\u0003*\u01ef\b*\u0001+\u0003+\u01f2\b+\u0001+\u0001+\u0001+\u0003+\u01f7"+
		"\b+\u0001+\u0003+\u01fa\b+\u0001+\u0003+\u01fd\b+\u0001,\u0001,\u0001"+
		",\u0001,\u0003,\u0203\b,\u0001,\u0003,\u0206\b,\u0001,\u0001,\u0001,\u0001"+
		",\u0001,\u0001,\u0003,\u020e\b,\u0001,\u0001,\u0001,\u0001,\u0001,\u0001"+
		",\u0001,\u0003,\u0217\b,\u0001-\u0003-\u021a\b-\u0001-\u0001-\u0001-\u0003"+
		"-\u021f\b-\u0001-\u0003-\u0222\b-\u0001.\u0001.\u0001.\u0003.\u0227\b"+
		".\u0001.\u0001.\u0001.\u0001.\u0001.\u0005.\u022e\b.\n.\f.\u0231\t.\u0001"+
		"/\u0001/\u0001/\u0001/\u0003/\u0237\b/\u0001/\u0001/\u0001/\u0001/\u0001"+
		"/\u0001/\u0001/\u0003/\u0240\b/\u00010\u00030\u0243\b0\u00010\u00010\u0001"+
		"0\u00030\u0248\b0\u00010\u00030\u024b\b0\u00011\u00011\u00051\u024f\b"+
		"1\n1\f1\u0252\t1\u00011\u00011\u00012\u00032\u0257\b2\u00012\u00012\u0003"+
		"2\u025b\b2\u00013\u00013\u00013\u00013\u00033\u0261\b3\u00013\u00013\u0001"+
		"3\u00013\u00013\u00013\u00013\u00033\u026a\b3\u00014\u00034\u026d\b4\u0001"+
		"4\u00014\u00014\u00034\u0272\b4\u00014\u00034\u0275\b4\u00015\u00015\u0005"+
		"5\u0279\b5\n5\f5\u027c\t5\u00015\u00015\u00016\u00016\u00016\u00016\u0003"+
		"6\u0284\b6\u00016\u00016\u00016\u00016\u00016\u00016\u00016\u00036\u028d"+
		"\b6\u00017\u00037\u0290\b7\u00017\u00017\u00017\u00017\u00037\u0296\b"+
		"7\u00017\u00037\u0299\b7\u00017\u00017\u00017\u00018\u00018\u00038\u02a0"+
		"\b8\u00018\u00018\u00018\u00018\u00018\u00058\u02a7\b8\n8\f8\u02aa\t8"+
		"\u00019\u00019\u00039\u02ae\b9\u0001:\u0001:\u0001;\u0001;\u0001<\u0001"+
		"<\u0001=\u0001=\u0001>\u0001>\u0001?\u0001?\u0001@\u0001@\u0001A\u0001"+
		"A\u0001A\u0001A\u0001A\u0003A\u02c3\bA\u0001A\u0005A\u02c6\bA\nA\fA\u02c9"+
		"\tA\u0001A\u0001A\u0003A\u02cd\bA\u0001B\u0001B\u0001B\u0001B\u0001B\u0003"+
		"B\u02d4\bB\u0001B\u0005B\u02d7\bB\nB\fB\u02da\tB\u0001B\u0001B\u0003B"+
		"\u02de\bB\u0001C\u0001C\u0001C\u0001C\u0001D\u0001D\u0001E\u0001E\u0001"+
		"F\u0001F\u0001F\u0000\u0003L\\pG\u0000\u0002\u0004\u0006\b\n\f\u000e\u0010"+
		"\u0012\u0014\u0016\u0018\u001a\u001c\u001e \"$&(*,.02468:<>@BDFHJLNPR"+
		"TVXZ\\^`bdfhjlnprtvxz|~\u0080\u0082\u0084\u0086\u0088\u008a\u008c\u0000"+
		"\u0006\u0001\u0000\u0001\u0003\u0001\u0000\u001f&\u0001\u0000\'1\u0007"+
		"\u0000\u0001\u0003\n\u000b\u0012\u0012\u0014\u0016\u0018\u0019\u001b\u001e"+
		"26\u0001\u000023\u0001\u0000:;\u031c\u0000\u0091\u0001\u0000\u0000\u0000"+
		"\u0002\u0099\u0001\u0000\u0000\u0000\u0004\u009d\u0001\u0000\u0000\u0000"+
		"\u0006\u00ac\u0001\u0000\u0000\u0000\b\u00ae\u0001\u0000\u0000\u0000\n"+
		"\u00b0\u0001\u0000\u0000\u0000\f\u00bc\u0001\u0000\u0000\u0000\u000e\u00bf"+
		"\u0001\u0000\u0000\u0000\u0010\u00cb\u0001\u0000\u0000\u0000\u0012\u00ce"+
		"\u0001\u0000\u0000\u0000\u0014\u00dd\u0001\u0000\u0000\u0000\u0016\u00e1"+
		"\u0001\u0000\u0000\u0000\u0018\u00e6\u0001\u0000\u0000\u0000\u001a\u00ef"+
		"\u0001\u0000\u0000\u0000\u001c\u00f7\u0001\u0000\u0000\u0000\u001e\u00f9"+
		"\u0001\u0000\u0000\u0000 \u0105\u0001\u0000\u0000\u0000\"\u0107\u0001"+
		"\u0000\u0000\u0000$\u010a\u0001\u0000\u0000\u0000&\u0117\u0001\u0000\u0000"+
		"\u0000(\u0120\u0001\u0000\u0000\u0000*\u0126\u0001\u0000\u0000\u0000,"+
		"\u0128\u0001\u0000\u0000\u0000.\u012a\u0001\u0000\u0000\u00000\u0134\u0001"+
		"\u0000\u0000\u00002\u0137\u0001\u0000\u0000\u00004\u013b\u0001\u0000\u0000"+
		"\u00006\u0143\u0001\u0000\u0000\u00008\u0147\u0001\u0000\u0000\u0000:"+
		"\u014a\u0001\u0000\u0000\u0000<\u0158\u0001\u0000\u0000\u0000>\u016c\u0001"+
		"\u0000\u0000\u0000@\u0174\u0001\u0000\u0000\u0000B\u017c\u0001\u0000\u0000"+
		"\u0000D\u017f\u0001\u0000\u0000\u0000F\u0186\u0001\u0000\u0000\u0000H"+
		"\u018c\u0001\u0000\u0000\u0000J\u01b1\u0001\u0000\u0000\u0000L\u01b3\u0001"+
		"\u0000\u0000\u0000N\u01c2\u0001\u0000\u0000\u0000P\u01cc\u0001\u0000\u0000"+
		"\u0000R\u01d7\u0001\u0000\u0000\u0000T\u01e5\u0001\u0000\u0000\u0000V"+
		"\u01f1\u0001\u0000\u0000\u0000X\u0216\u0001\u0000\u0000\u0000Z\u0219\u0001"+
		"\u0000\u0000\u0000\\\u0223\u0001\u0000\u0000\u0000^\u023f\u0001\u0000"+
		"\u0000\u0000`\u0242\u0001\u0000\u0000\u0000b\u024c\u0001\u0000\u0000\u0000"+
		"d\u0256\u0001\u0000\u0000\u0000f\u0269\u0001\u0000\u0000\u0000h\u026c"+
		"\u0001\u0000\u0000\u0000j\u0276\u0001\u0000\u0000\u0000l\u028c\u0001\u0000"+
		"\u0000\u0000n\u028f\u0001\u0000\u0000\u0000p\u029d\u0001\u0000\u0000\u0000"+
		"r\u02ad\u0001\u0000\u0000\u0000t\u02af\u0001\u0000\u0000\u0000v\u02b1"+
		"\u0001\u0000\u0000\u0000x\u02b3\u0001\u0000\u0000\u0000z\u02b5\u0001\u0000"+
		"\u0000\u0000|\u02b7\u0001\u0000\u0000\u0000~\u02b9\u0001\u0000\u0000\u0000"+
		"\u0080\u02bb\u0001\u0000\u0000\u0000\u0082\u02cc\u0001\u0000\u0000\u0000"+
		"\u0084\u02dd\u0001\u0000\u0000\u0000\u0086\u02df\u0001\u0000\u0000\u0000"+
		"\u0088\u02e3\u0001\u0000\u0000\u0000\u008a\u02e5\u0001\u0000\u0000\u0000"+
		"\u008c\u02e7\u0001\u0000\u0000\u0000\u008e\u0090\u0003\u0002\u0001\u0000"+
		"\u008f\u008e\u0001\u0000\u0000\u0000\u0090\u0093\u0001\u0000\u0000\u0000"+
		"\u0091\u008f\u0001\u0000\u0000\u0000\u0091\u0092\u0001\u0000\u0000\u0000"+
		"\u0092\u0094\u0001\u0000\u0000\u0000\u0093\u0091\u0001\u0000\u0000\u0000"+
		"\u0094\u0095\u0005\u0000\u0000\u0001\u0095\u0001\u0001\u0000\u0000\u0000"+
		"\u0096\u009a\u0003\u0004\u0002\u0000\u0097\u009a\u00036\u001b\u0000\u0098"+
		"\u009a\u00038\u001c\u0000\u0099\u0096\u0001\u0000\u0000\u0000\u0099\u0097"+
		"\u0001\u0000\u0000\u0000\u0099\u0098\u0001\u0000\u0000\u0000\u009a\u0003"+
		"\u0001\u0000\u0000\u0000\u009b\u009e\u0003\u0006\u0003\u0000\u009c\u009e"+
		"\u0003\u001a\r\u0000\u009d\u009b\u0001\u0000\u0000\u0000\u009d\u009c\u0001"+
		"\u0000\u0000\u0000\u009e\u0005\u0001\u0000\u0000\u0000\u009f\u00a1\u0003"+
		"\b\u0004\u0000\u00a0\u00a2\u0003x<\u0000\u00a1\u00a0\u0001\u0000\u0000"+
		"\u0000\u00a1\u00a2\u0001\u0000\u0000\u0000\u00a2\u00a4\u0001\u0000\u0000"+
		"\u0000\u00a3\u00a5\u0003$\u0012\u0000\u00a4\u00a3\u0001\u0000\u0000\u0000"+
		"\u00a4\u00a5\u0001\u0000\u0000\u0000\u00a5\u00a7\u0001\u0000\u0000\u0000"+
		"\u00a6\u00a8\u00032\u0019\u0000\u00a7\u00a6\u0001\u0000\u0000\u0000\u00a7"+
		"\u00a8\u0001\u0000\u0000\u0000\u00a8\u00a9\u0001\u0000\u0000\u0000\u00a9"+
		"\u00aa\u0003\n\u0005\u0000\u00aa\u00ad\u0001\u0000\u0000\u0000\u00ab\u00ad"+
		"\u0003\n\u0005\u0000\u00ac\u009f\u0001\u0000\u0000\u0000\u00ac\u00ab\u0001"+
		"\u0000\u0000\u0000\u00ad\u0007\u0001\u0000\u0000\u0000\u00ae\u00af\u0007"+
		"\u0000\u0000\u0000\u00af\t\u0001\u0000\u0000\u0000\u00b0\u00b4\u0005\u0004"+
		"\u0000\u0000\u00b1\u00b3\u0003\f\u0006\u0000\u00b2\u00b1\u0001\u0000\u0000"+
		"\u0000\u00b3\u00b6\u0001\u0000\u0000\u0000\u00b4\u00b2\u0001\u0000\u0000"+
		"\u0000\u00b4\u00b5\u0001\u0000\u0000\u0000\u00b5\u00b7\u0001\u0000\u0000"+
		"\u0000\u00b6\u00b4\u0001\u0000\u0000\u0000\u00b7\u00b8\u0005\u0005\u0000"+
		"\u0000\u00b8\u000b\u0001\u0000\u0000\u0000\u00b9\u00bd\u0003\u000e\u0007"+
		"\u0000\u00ba\u00bd\u0003\u0016\u000b\u0000\u00bb\u00bd\u0003\u0018\f\u0000"+
		"\u00bc\u00b9\u0001\u0000\u0000\u0000\u00bc\u00ba\u0001\u0000\u0000\u0000"+
		"\u00bc\u00bb\u0001\u0000\u0000\u0000\u00bd\r\u0001\u0000\u0000\u0000\u00be"+
		"\u00c0\u0003\u0010\b\u0000\u00bf\u00be\u0001\u0000\u0000\u0000\u00bf\u00c0"+
		"\u0001\u0000\u0000\u0000\u00c0\u00c1\u0001\u0000\u0000\u0000\u00c1\u00c3"+
		"\u0003x<\u0000\u00c2\u00c4\u0003\u0012\t\u0000\u00c3\u00c2\u0001\u0000"+
		"\u0000\u0000\u00c3\u00c4\u0001\u0000\u0000\u0000\u00c4\u00c6\u0001\u0000"+
		"\u0000\u0000\u00c5\u00c7\u00032\u0019\u0000\u00c6\u00c5\u0001\u0000\u0000"+
		"\u0000\u00c6\u00c7\u0001\u0000\u0000\u0000\u00c7\u00c9\u0001\u0000\u0000"+
		"\u0000\u00c8\u00ca\u0003\n\u0005\u0000\u00c9\u00c8\u0001\u0000\u0000\u0000"+
		"\u00c9\u00ca\u0001\u0000\u0000\u0000\u00ca\u000f\u0001\u0000\u0000\u0000"+
		"\u00cb\u00cc\u0003x<\u0000\u00cc\u00cd\u0005\u0006\u0000\u0000\u00cd\u0011"+
		"\u0001\u0000\u0000\u0000\u00ce\u00d9\u0005\u0007\u0000\u0000\u00cf\u00d6"+
		"\u0003\u0014\n\u0000\u00d0\u00d2\u0005>\u0000\u0000\u00d1\u00d0\u0001"+
		"\u0000\u0000\u0000\u00d1\u00d2\u0001\u0000\u0000\u0000\u00d2\u00d3\u0001"+
		"\u0000\u0000\u0000\u00d3\u00d5\u0003\u0014\n\u0000\u00d4\u00d1\u0001\u0000"+
		"\u0000\u0000\u00d5\u00d8\u0001\u0000\u0000\u0000\u00d6\u00d4\u0001\u0000"+
		"\u0000\u0000\u00d6\u00d7\u0001\u0000\u0000\u0000\u00d7\u00da\u0001\u0000"+
		"\u0000\u0000\u00d8\u00d6\u0001\u0000\u0000\u0000\u00d9\u00cf\u0001\u0000"+
		"\u0000\u0000\u00d9\u00da\u0001\u0000\u0000\u0000\u00da\u00db\u0001\u0000"+
		"\u0000\u0000\u00db\u00dc\u0005\b\u0000\u0000\u00dc\u0013\u0001\u0000\u0000"+
		"\u0000\u00dd\u00de\u0003x<\u0000\u00de\u00df\u0005\u0006\u0000\u0000\u00df"+
		"\u00e0\u0003 \u0010\u0000\u00e0\u0015\u0001\u0000\u0000\u0000\u00e1\u00e2"+
		"\u0005\t\u0000\u0000\u00e2\u00e4\u0003\u001c\u000e\u0000\u00e3\u00e5\u0003"+
		"2\u0019\u0000\u00e4\u00e3\u0001\u0000\u0000\u0000\u00e4\u00e5\u0001\u0000"+
		"\u0000\u0000\u00e5\u0017\u0001\u0000\u0000\u0000\u00e6\u00e8\u0005\t\u0000"+
		"\u0000\u00e7\u00e9\u0003\u001e\u000f\u0000\u00e8\u00e7\u0001\u0000\u0000"+
		"\u0000\u00e8\u00e9\u0001\u0000\u0000\u0000\u00e9\u00eb\u0001\u0000\u0000"+
		"\u0000\u00ea\u00ec\u00032\u0019\u0000\u00eb\u00ea\u0001\u0000\u0000\u0000"+
		"\u00eb\u00ec\u0001\u0000\u0000\u0000\u00ec\u00ed\u0001\u0000\u0000\u0000"+
		"\u00ed\u00ee\u0003\n\u0005\u0000\u00ee\u0019\u0001\u0000\u0000\u0000\u00ef"+
		"\u00f0\u0005\n\u0000\u0000\u00f0\u00f1\u0003\u001c\u000e\u0000\u00f1\u00f3"+
		"\u0003\u001e\u000f\u0000\u00f2\u00f4\u00032\u0019\u0000\u00f3\u00f2\u0001"+
		"\u0000\u0000\u0000\u00f3\u00f4\u0001\u0000\u0000\u0000\u00f4\u00f5\u0001"+
		"\u0000\u0000\u0000\u00f5\u00f6\u0003\n\u0005\u0000\u00f6\u001b\u0001\u0000"+
		"\u0000\u0000\u00f7\u00f8\u0003x<\u0000\u00f8\u001d\u0001\u0000\u0000\u0000"+
		"\u00f9\u00fa\u0005\u000b\u0000\u0000\u00fa\u00fb\u0003,\u0016\u0000\u00fb"+
		"\u001f\u0001\u0000\u0000\u0000\u00fc\u0106\u0003\"\u0011\u0000\u00fd\u0106"+
		"\u0003\u0088D\u0000\u00fe\u0106\u0003\u008aE\u0000\u00ff\u0106\u0003\u008c"+
		"F\u0000\u0100\u0106\u0003|>\u0000\u0101\u0106\u0003~?\u0000\u0102\u0106"+
		"\u0003\u0080@\u0000\u0103\u0106\u0003\u0082A\u0000\u0104\u0106\u0003\u0084"+
		"B\u0000\u0105\u00fc\u0001\u0000\u0000\u0000\u0105\u00fd\u0001\u0000\u0000"+
		"\u0000\u0105\u00fe\u0001\u0000\u0000\u0000\u0105\u00ff\u0001\u0000\u0000"+
		"\u0000\u0105\u0100\u0001\u0000\u0000\u0000\u0105\u0101\u0001\u0000\u0000"+
		"\u0000\u0105\u0102\u0001\u0000\u0000\u0000\u0105\u0103\u0001\u0000\u0000"+
		"\u0000\u0105\u0104\u0001\u0000\u0000\u0000\u0106!\u0001\u0000\u0000\u0000"+
		"\u0107\u0108\u0005\f\u0000\u0000\u0108\u0109\u0003x<\u0000\u0109#\u0001"+
		"\u0000\u0000\u0000\u010a\u010b\u0005\u0007\u0000\u0000\u010b\u0112\u0003"+
		"&\u0013\u0000\u010c\u010e\u0005>\u0000\u0000\u010d\u010c\u0001\u0000\u0000"+
		"\u0000\u010d\u010e\u0001\u0000\u0000\u0000\u010e\u010f\u0001\u0000\u0000"+
		"\u0000\u010f\u0111\u0003&\u0013\u0000\u0110\u010d\u0001\u0000\u0000\u0000"+
		"\u0111\u0114\u0001\u0000\u0000\u0000\u0112\u0110\u0001\u0000\u0000\u0000"+
		"\u0112\u0113\u0001\u0000\u0000\u0000\u0113\u0115\u0001\u0000\u0000\u0000"+
		"\u0114\u0112\u0001\u0000\u0000\u0000\u0115\u0116\u0005\b\u0000\u0000\u0116"+
		"%\u0001\u0000\u0000\u0000\u0117\u0118\u0003\"\u0011\u0000\u0118\u0119"+
		"\u0005\u0006\u0000\u0000\u0119\u011b\u0003*\u0015\u0000\u011a\u011c\u0003"+
		"(\u0014\u0000\u011b\u011a\u0001\u0000\u0000\u0000\u011b\u011c\u0001\u0000"+
		"\u0000\u0000\u011c\u011e\u0001\u0000\u0000\u0000\u011d\u011f\u00032\u0019"+
		"\u0000\u011e\u011d\u0001\u0000\u0000\u0000\u011e\u011f\u0001\u0000\u0000"+
		"\u0000\u011f\'\u0001\u0000\u0000\u0000\u0120\u0121\u0005\r\u0000\u0000"+
		"\u0121\u0122\u0003 \u0010\u0000\u0122)\u0001\u0000\u0000\u0000\u0123\u0127"+
		"\u0003,\u0016\u0000\u0124\u0127\u0003.\u0017\u0000\u0125\u0127\u00030"+
		"\u0018\u0000\u0126\u0123\u0001\u0000\u0000\u0000\u0126\u0124\u0001\u0000"+
		"\u0000\u0000\u0126\u0125\u0001\u0000\u0000\u0000\u0127+\u0001\u0000\u0000"+
		"\u0000\u0128\u0129\u0003x<\u0000\u0129-\u0001\u0000\u0000\u0000\u012a"+
		"\u012b\u0005\u000e\u0000\u0000\u012b\u012c\u0003*\u0015\u0000\u012c\u012d"+
		"\u0005\u000f\u0000\u0000\u012d/\u0001\u0000\u0000\u0000\u012e\u012f\u0003"+
		",\u0016\u0000\u012f\u0130\u0005\u0010\u0000\u0000\u0130\u0135\u0001\u0000"+
		"\u0000\u0000\u0131\u0132\u0003.\u0017\u0000\u0132\u0133\u0005\u0010\u0000"+
		"\u0000\u0133\u0135\u0001\u0000\u0000\u0000\u0134\u012e\u0001\u0000\u0000"+
		"\u0000\u0134\u0131\u0001\u0000\u0000\u0000\u01351\u0001\u0000\u0000\u0000"+
		"\u0136\u0138\u00034\u001a\u0000\u0137\u0136\u0001\u0000\u0000\u0000\u0138"+
		"\u0139\u0001\u0000\u0000\u0000\u0139\u0137\u0001\u0000\u0000\u0000\u0139"+
		"\u013a\u0001\u0000\u0000\u0000\u013a3\u0001\u0000\u0000\u0000\u013b\u013c"+
		"\u0005\u0011\u0000\u0000\u013c\u013e\u0003x<\u0000\u013d\u013f\u0003\u0012"+
		"\t\u0000\u013e\u013d\u0001\u0000\u0000\u0000\u013e\u013f\u0001\u0000\u0000"+
		"\u0000\u013f5\u0001\u0000\u0000\u0000\u0140\u0144\u0003:\u001d\u0000\u0141"+
		"\u0144\u0003@ \u0000\u0142\u0144\u0003n7\u0000\u0143\u0140\u0001\u0000"+
		"\u0000\u0000\u0143\u0141\u0001\u0000\u0000\u0000\u0143\u0142\u0001\u0000"+
		"\u0000\u0000\u01447\u0001\u0000\u0000\u0000\u0145\u0148\u0003>\u001f\u0000"+
		"\u0146\u0148\u0003B!\u0000\u0147\u0145\u0001\u0000\u0000\u0000\u0147\u0146"+
		"\u0001\u0000\u0000\u0000\u01489\u0001\u0000\u0000\u0000\u0149\u014b\u0003"+
		"z=\u0000\u014a\u0149\u0001\u0000\u0000\u0000\u014a\u014b\u0001\u0000\u0000"+
		"\u0000\u014b\u014c\u0001\u0000\u0000\u0000\u014c\u014e\u0005\u0012\u0000"+
		"\u0000\u014d\u014f\u00032\u0019\u0000\u014e\u014d\u0001\u0000\u0000\u0000"+
		"\u014e\u014f\u0001\u0000\u0000\u0000\u014f\u0150\u0001\u0000\u0000\u0000"+
		"\u0150\u0152\u0005\u0004\u0000\u0000\u0151\u0153\u0003<\u001e\u0000\u0152"+
		"\u0151\u0001\u0000\u0000\u0000\u0153\u0154\u0001\u0000\u0000\u0000\u0154"+
		"\u0152\u0001\u0000\u0000\u0000\u0154\u0155\u0001\u0000\u0000\u0000\u0155"+
		"\u0156\u0001\u0000\u0000\u0000\u0156\u0157\u0005\u0005\u0000\u0000\u0157"+
		";\u0001\u0000\u0000\u0000\u0158\u0159\u0003\b\u0004\u0000\u0159\u015a"+
		"\u0005\u0006\u0000\u0000\u015a\u015b\u0003,\u0016\u0000\u015b=\u0001\u0000"+
		"\u0000\u0000\u015c\u015d\u0005\u0013\u0000\u0000\u015d\u015f\u0005\u0012"+
		"\u0000\u0000\u015e\u0160\u00032\u0019\u0000\u015f\u015e\u0001\u0000\u0000"+
		"\u0000\u015f\u0160\u0001\u0000\u0000\u0000\u0160\u0161\u0001\u0000\u0000"+
		"\u0000\u0161\u0163\u0005\u0004\u0000\u0000\u0162\u0164\u0003<\u001e\u0000"+
		"\u0163\u0162\u0001\u0000\u0000\u0000\u0164\u0165\u0001\u0000\u0000\u0000"+
		"\u0165\u0163\u0001\u0000\u0000\u0000\u0165\u0166\u0001\u0000\u0000\u0000"+
		"\u0166\u0167\u0001\u0000\u0000\u0000\u0167\u0168\u0005\u0005\u0000\u0000"+
		"\u0168\u016d\u0001\u0000\u0000\u0000\u0169\u016a\u0005\u0013\u0000\u0000"+
		"\u016a\u016b\u0005\u0012\u0000\u0000\u016b\u016d\u00032\u0019\u0000\u016c"+
		"\u015c\u0001\u0000\u0000\u0000\u016c\u0169\u0001\u0000\u0000\u0000\u016d"+
		"?\u0001\u0000\u0000\u0000\u016e\u0175\u0003D\"\u0000\u016f\u0175\u0003"+
		"H$\u0000\u0170\u0175\u0003V+\u0000\u0171\u0175\u0003Z-\u0000\u0172\u0175"+
		"\u0003`0\u0000\u0173\u0175\u0003h4\u0000\u0174\u016e\u0001\u0000\u0000"+
		"\u0000\u0174\u016f\u0001\u0000\u0000\u0000\u0174\u0170\u0001\u0000\u0000"+
		"\u0000\u0174\u0171\u0001\u0000\u0000\u0000\u0174\u0172\u0001\u0000\u0000"+
		"\u0000\u0174\u0173\u0001\u0000\u0000\u0000\u0175A\u0001\u0000\u0000\u0000"+
		"\u0176\u017d\u0003F#\u0000\u0177\u017d\u0003J%\u0000\u0178\u017d\u0003"+
		"X,\u0000\u0179\u017d\u0003^/\u0000\u017a\u017d\u0003f3\u0000\u017b\u017d"+
		"\u0003l6\u0000\u017c\u0176\u0001\u0000\u0000\u0000\u017c\u0177\u0001\u0000"+
		"\u0000\u0000\u017c\u0178\u0001\u0000\u0000\u0000\u017c\u0179\u0001\u0000"+
		"\u0000\u0000\u017c\u017a\u0001\u0000\u0000\u0000\u017c\u017b\u0001\u0000"+
		"\u0000\u0000\u017dC\u0001\u0000\u0000\u0000\u017e\u0180\u0003z=\u0000"+
		"\u017f\u017e\u0001\u0000\u0000\u0000\u017f\u0180\u0001\u0000\u0000\u0000"+
		"\u0180\u0181\u0001\u0000\u0000\u0000\u0181\u0182\u0005\u0014\u0000\u0000"+
		"\u0182\u0184\u0003x<\u0000\u0183\u0185\u00032\u0019\u0000\u0184\u0183"+
		"\u0001\u0000\u0000\u0000\u0184\u0185\u0001\u0000\u0000\u0000\u0185E\u0001"+
		"\u0000\u0000\u0000\u0186\u0187\u0005\u0013\u0000\u0000\u0187\u0188\u0005"+
		"\u0014\u0000\u0000\u0188\u0189\u0003x<\u0000\u0189\u018a\u00032\u0019"+
		"\u0000\u018aG\u0001\u0000\u0000\u0000\u018b\u018d\u0003z=\u0000\u018c"+
		"\u018b\u0001\u0000\u0000\u0000\u018c\u018d\u0001\u0000\u0000\u0000\u018d"+
		"\u018e\u0001\u0000\u0000\u0000\u018e\u018f\u0005\u0015\u0000\u0000\u018f"+
		"\u0191\u0003x<\u0000\u0190\u0192\u0003L&\u0000\u0191\u0190\u0001\u0000"+
		"\u0000\u0000\u0191\u0192\u0001\u0000\u0000\u0000\u0192\u0194\u0001\u0000"+
		"\u0000\u0000\u0193\u0195\u00032\u0019\u0000\u0194\u0193\u0001\u0000\u0000"+
		"\u0000\u0194\u0195\u0001\u0000\u0000\u0000\u0195\u0197\u0001\u0000\u0000"+
		"\u0000\u0196\u0198\u0003N\'\u0000\u0197\u0196\u0001\u0000\u0000\u0000"+
		"\u0197\u0198\u0001\u0000\u0000\u0000\u0198I\u0001\u0000\u0000\u0000\u0199"+
		"\u019a\u0005\u0013\u0000\u0000\u019a\u019b\u0005\u0015\u0000\u0000\u019b"+
		"\u019d\u0003x<\u0000\u019c\u019e\u0003L&\u0000\u019d\u019c\u0001\u0000"+
		"\u0000\u0000\u019d\u019e\u0001\u0000\u0000\u0000\u019e\u01a0\u0001\u0000"+
		"\u0000\u0000\u019f\u01a1\u00032\u0019\u0000\u01a0\u019f\u0001\u0000\u0000"+
		"\u0000\u01a0\u01a1\u0001\u0000\u0000\u0000\u01a1\u01a2\u0001\u0000\u0000"+
		"\u0000\u01a2\u01a3\u0003N\'\u0000\u01a3\u01b2\u0001\u0000\u0000\u0000"+
		"\u01a4\u01a5\u0005\u0013\u0000\u0000\u01a5\u01a6\u0005\u0015\u0000\u0000"+
		"\u01a6\u01a8\u0003x<\u0000\u01a7\u01a9\u0003L&\u0000\u01a8\u01a7\u0001"+
		"\u0000\u0000\u0000\u01a8\u01a9\u0001\u0000\u0000\u0000\u01a9\u01aa\u0001"+
		"\u0000\u0000\u0000\u01aa\u01ab\u00032\u0019\u0000\u01ab\u01b2\u0001\u0000"+
		"\u0000\u0000\u01ac\u01ad\u0005\u0013\u0000\u0000\u01ad\u01ae\u0005\u0015"+
		"\u0000\u0000\u01ae\u01af\u0003x<\u0000\u01af\u01b0\u0003L&\u0000\u01b0"+
		"\u01b2\u0001\u0000\u0000\u0000\u01b1\u0199\u0001\u0000\u0000\u0000\u01b1"+
		"\u01a4\u0001\u0000\u0000\u0000\u01b1\u01ac\u0001\u0000\u0000\u0000\u01b2"+
		"K\u0001\u0000\u0000\u0000\u01b3\u01b4\u0006&\uffff\uffff\u0000\u01b4\u01b6"+
		"\u0005\u0016\u0000\u0000\u01b5\u01b7\u0005\u0017\u0000\u0000\u01b6\u01b5"+
		"\u0001\u0000\u0000\u0000\u01b6\u01b7\u0001\u0000\u0000\u0000\u01b7\u01b8"+
		"\u0001\u0000\u0000\u0000\u01b8\u01b9\u0003,\u0016\u0000\u01b9\u01bf\u0001"+
		"\u0000\u0000\u0000\u01ba\u01bb\n\u0001\u0000\u0000\u01bb\u01bc\u0005\u0017"+
		"\u0000\u0000\u01bc\u01be\u0003,\u0016\u0000\u01bd\u01ba\u0001\u0000\u0000"+
		"\u0000\u01be\u01c1\u0001\u0000\u0000\u0000\u01bf\u01bd\u0001\u0000\u0000"+
		"\u0000\u01bf\u01c0\u0001\u0000\u0000\u0000\u01c0M\u0001\u0000\u0000\u0000"+
		"\u01c1\u01bf\u0001\u0000\u0000\u0000\u01c2\u01c6\u0005\u0004\u0000\u0000"+
		"\u01c3\u01c5\u0003P(\u0000\u01c4\u01c3\u0001\u0000\u0000\u0000\u01c5\u01c8"+
		"\u0001\u0000\u0000\u0000\u01c6\u01c4\u0001\u0000\u0000\u0000\u01c6\u01c7"+
		"\u0001\u0000\u0000\u0000\u01c7\u01c9\u0001\u0000\u0000\u0000\u01c8\u01c6"+
		"\u0001\u0000\u0000\u0000\u01c9\u01ca\u0005\u0005\u0000\u0000\u01caO\u0001"+
		"\u0000\u0000\u0000\u01cb\u01cd\u0003z=\u0000\u01cc\u01cb\u0001\u0000\u0000"+
		"\u0000\u01cc\u01cd\u0001\u0000\u0000\u0000\u01cd\u01ce\u0001\u0000\u0000"+
		"\u0000\u01ce\u01d0\u0003x<\u0000\u01cf\u01d1\u0003R)\u0000\u01d0\u01cf"+
		"\u0001\u0000\u0000\u0000\u01d0\u01d1\u0001\u0000\u0000\u0000\u01d1\u01d2"+
		"\u0001\u0000\u0000\u0000\u01d2\u01d3\u0005\u0006\u0000\u0000\u01d3\u01d5"+
		"\u0003*\u0015\u0000\u01d4\u01d6\u00032\u0019\u0000\u01d5\u01d4\u0001\u0000"+
		"\u0000\u0000\u01d5\u01d6\u0001\u0000\u0000\u0000\u01d6Q\u0001\u0000\u0000"+
		"\u0000\u01d7\u01d8\u0005\u0007\u0000\u0000\u01d8\u01df\u0003T*\u0000\u01d9"+
		"\u01db\u0005>\u0000\u0000\u01da\u01d9\u0001\u0000\u0000\u0000\u01da\u01db"+
		"\u0001\u0000\u0000\u0000\u01db\u01dc\u0001\u0000\u0000\u0000\u01dc\u01de"+
		"\u0003T*\u0000\u01dd\u01da\u0001\u0000\u0000\u0000\u01de\u01e1\u0001\u0000"+
		"\u0000\u0000\u01df\u01dd\u0001\u0000\u0000\u0000\u01df\u01e0\u0001\u0000"+
		"\u0000\u0000\u01e0\u01e2\u0001\u0000\u0000\u0000\u01e1\u01df\u0001\u0000"+
		"\u0000\u0000\u01e2\u01e3\u0005\b\u0000\u0000\u01e3S\u0001\u0000\u0000"+
		"\u0000\u01e4\u01e6\u0003z=\u0000\u01e5\u01e4\u0001\u0000\u0000\u0000\u01e5"+
		"\u01e6\u0001\u0000\u0000\u0000\u01e6\u01e7\u0001\u0000\u0000\u0000\u01e7"+
		"\u01e8\u0003x<\u0000\u01e8\u01e9\u0005\u0006\u0000\u0000\u01e9\u01eb\u0003"+
		"*\u0015\u0000\u01ea\u01ec\u0003(\u0014\u0000\u01eb\u01ea\u0001\u0000\u0000"+
		"\u0000\u01eb\u01ec\u0001\u0000\u0000\u0000\u01ec\u01ee\u0001\u0000\u0000"+
		"\u0000\u01ed\u01ef\u00032\u0019\u0000\u01ee\u01ed\u0001\u0000\u0000\u0000"+
		"\u01ee\u01ef\u0001\u0000\u0000\u0000\u01efU\u0001\u0000\u0000\u0000\u01f0"+
		"\u01f2\u0003z=\u0000\u01f1\u01f0\u0001\u0000\u0000\u0000\u01f1\u01f2\u0001"+
		"\u0000\u0000\u0000\u01f2\u01f3\u0001\u0000\u0000\u0000\u01f3\u01f4\u0005"+
		"\u0018\u0000\u0000\u01f4\u01f6\u0003x<\u0000\u01f5\u01f7\u0003L&\u0000"+
		"\u01f6\u01f5\u0001\u0000\u0000\u0000\u01f6\u01f7\u0001\u0000\u0000\u0000"+
		"\u01f7\u01f9\u0001\u0000\u0000\u0000\u01f8\u01fa\u00032\u0019\u0000\u01f9"+
		"\u01f8\u0001\u0000\u0000\u0000\u01f9\u01fa\u0001\u0000\u0000\u0000\u01fa"+
		"\u01fc\u0001\u0000\u0000\u0000\u01fb\u01fd\u0003N\'\u0000\u01fc\u01fb"+
		"\u0001\u0000\u0000\u0000\u01fc\u01fd\u0001\u0000\u0000\u0000\u01fdW\u0001"+
		"\u0000\u0000\u0000\u01fe\u01ff\u0005\u0013\u0000\u0000\u01ff\u0200\u0005"+
		"\u0018\u0000\u0000\u0200\u0202\u0003x<\u0000\u0201\u0203\u0003L&\u0000"+
		"\u0202\u0201\u0001\u0000\u0000\u0000\u0202\u0203\u0001\u0000\u0000\u0000"+
		"\u0203\u0205\u0001\u0000\u0000\u0000\u0204\u0206\u00032\u0019\u0000\u0205"+
		"\u0204\u0001\u0000\u0000\u0000\u0205\u0206\u0001\u0000\u0000\u0000\u0206"+
		"\u0207\u0001\u0000\u0000\u0000\u0207\u0208\u0003N\'\u0000\u0208\u0217"+
		"\u0001\u0000\u0000\u0000\u0209\u020a\u0005\u0013\u0000\u0000\u020a\u020b"+
		"\u0005\u0018\u0000\u0000\u020b\u020d\u0003x<\u0000\u020c\u020e\u0003L"+
		"&\u0000\u020d\u020c\u0001\u0000\u0000\u0000\u020d\u020e\u0001\u0000\u0000"+
		"\u0000\u020e\u020f\u0001\u0000\u0000\u0000\u020f\u0210\u00032\u0019\u0000"+
		"\u0210\u0217\u0001\u0000\u0000\u0000\u0211\u0212\u0005\u0013\u0000\u0000"+
		"\u0212\u0213\u0005\u0018\u0000\u0000\u0213\u0214\u0003x<\u0000\u0214\u0215"+
		"\u0003L&\u0000\u0215\u0217\u0001\u0000\u0000\u0000\u0216\u01fe\u0001\u0000"+
		"\u0000\u0000\u0216\u0209\u0001\u0000\u0000\u0000\u0216\u0211\u0001\u0000"+
		"\u0000\u0000\u0217Y\u0001\u0000\u0000\u0000\u0218\u021a\u0003z=\u0000"+
		"\u0219\u0218\u0001\u0000\u0000\u0000\u0219\u021a\u0001\u0000\u0000\u0000"+
		"\u021a\u021b\u0001\u0000\u0000\u0000\u021b\u021c\u0005\u0019\u0000\u0000"+
		"\u021c\u021e\u0003x<\u0000\u021d\u021f\u00032\u0019\u0000\u021e\u021d"+
		"\u0001\u0000\u0000\u0000\u021e\u021f\u0001\u0000\u0000\u0000\u021f\u0221"+
		"\u0001\u0000\u0000\u0000\u0220\u0222\u0003\\.\u0000\u0221\u0220\u0001"+
		"\u0000\u0000\u0000\u0221\u0222\u0001\u0000\u0000\u0000\u0222[\u0001\u0000"+
		"\u0000\u0000\u0223\u0224\u0006.\uffff\uffff\u0000\u0224\u0226\u0005\r"+
		"\u0000\u0000\u0225\u0227\u0005\u001a\u0000\u0000\u0226\u0225\u0001\u0000"+
		"\u0000\u0000\u0226\u0227\u0001\u0000\u0000\u0000\u0227\u0228\u0001\u0000"+
		"\u0000\u0000\u0228\u0229\u0003,\u0016\u0000\u0229\u022f\u0001\u0000\u0000"+
		"\u0000\u022a\u022b\n\u0001\u0000\u0000\u022b\u022c\u0005\u001a\u0000\u0000"+
		"\u022c\u022e\u0003,\u0016\u0000\u022d\u022a\u0001\u0000\u0000\u0000\u022e"+
		"\u0231\u0001\u0000\u0000\u0000\u022f\u022d\u0001\u0000\u0000\u0000\u022f"+
		"\u0230\u0001\u0000\u0000\u0000\u0230]\u0001\u0000\u0000\u0000\u0231\u022f"+
		"\u0001\u0000\u0000\u0000\u0232\u0233\u0005\u0013\u0000\u0000\u0233\u0234"+
		"\u0005\u0019\u0000\u0000\u0234\u0236\u0003x<\u0000\u0235\u0237\u00032"+
		"\u0019\u0000\u0236\u0235\u0001\u0000\u0000\u0000\u0236\u0237\u0001\u0000"+
		"\u0000\u0000\u0237\u0238\u0001\u0000\u0000\u0000\u0238\u0239\u0003\\."+
		"\u0000\u0239\u0240\u0001\u0000\u0000\u0000\u023a\u023b\u0005\u0013\u0000"+
		"\u0000\u023b\u023c\u0005\u0019\u0000\u0000\u023c\u023d\u0003x<\u0000\u023d"+
		"\u023e\u00032\u0019\u0000\u023e\u0240\u0001\u0000\u0000\u0000\u023f\u0232"+
		"\u0001\u0000\u0000\u0000\u023f\u023a\u0001\u0000\u0000\u0000\u0240_\u0001"+
		"\u0000\u0000\u0000\u0241\u0243\u0003z=\u0000\u0242\u0241\u0001\u0000\u0000"+
		"\u0000\u0242\u0243\u0001\u0000\u0000\u0000\u0243\u0244\u0001\u0000\u0000"+
		"\u0000\u0244\u0245\u0005\u001b\u0000\u0000\u0245\u0247\u0003x<\u0000\u0246"+
		"\u0248\u00032\u0019\u0000\u0247\u0246\u0001\u0000\u0000\u0000\u0247\u0248"+
		"\u0001\u0000\u0000\u0000\u0248\u024a\u0001\u0000\u0000\u0000\u0249\u024b"+
		"\u0003b1\u0000\u024a\u0249\u0001\u0000\u0000\u0000\u024a\u024b\u0001\u0000"+
		"\u0000\u0000\u024ba\u0001\u0000\u0000\u0000\u024c\u0250\u0005\u0004\u0000"+
		"\u0000\u024d\u024f\u0003d2\u0000\u024e\u024d\u0001\u0000\u0000\u0000\u024f"+
		"\u0252\u0001\u0000\u0000\u0000\u0250\u024e\u0001\u0000\u0000\u0000\u0250"+
		"\u0251\u0001\u0000\u0000\u0000\u0251\u0253\u0001\u0000\u0000\u0000\u0252"+
		"\u0250\u0001\u0000\u0000\u0000\u0253\u0254\u0005\u0005\u0000\u0000\u0254"+
		"c\u0001\u0000\u0000\u0000\u0255\u0257\u0003z=\u0000\u0256\u0255\u0001"+
		"\u0000\u0000\u0000\u0256\u0257\u0001\u0000\u0000\u0000\u0257\u0258\u0001"+
		"\u0000\u0000\u0000\u0258\u025a\u0003\u0080@\u0000\u0259\u025b\u00032\u0019"+
		"\u0000\u025a\u0259\u0001\u0000\u0000\u0000\u025a\u025b\u0001\u0000\u0000"+
		"\u0000\u025be\u0001\u0000\u0000\u0000\u025c\u025d\u0005\u0013\u0000\u0000"+
		"\u025d\u025e\u0005\u001b\u0000\u0000\u025e\u0260\u0003x<\u0000\u025f\u0261"+
		"\u00032\u0019\u0000\u0260\u025f\u0001\u0000\u0000\u0000\u0260\u0261\u0001"+
		"\u0000\u0000\u0000\u0261\u0262\u0001\u0000\u0000\u0000\u0262\u0263\u0003"+
		"b1\u0000\u0263\u026a\u0001\u0000\u0000\u0000\u0264\u0265\u0005\u0013\u0000"+
		"\u0000\u0265\u0266\u0005\u001b\u0000\u0000\u0266\u0267\u0003x<\u0000\u0267"+
		"\u0268\u00032\u0019\u0000\u0268\u026a\u0001\u0000\u0000\u0000\u0269\u025c"+
		"\u0001\u0000\u0000\u0000\u0269\u0264\u0001\u0000\u0000\u0000\u026ag\u0001"+
		"\u0000\u0000\u0000\u026b\u026d\u0003z=\u0000\u026c\u026b\u0001\u0000\u0000"+
		"\u0000\u026c\u026d\u0001\u0000\u0000\u0000\u026d\u026e\u0001\u0000\u0000"+
		"\u0000\u026e\u026f\u0005\u001c\u0000\u0000\u026f\u0271\u0003x<\u0000\u0270"+
		"\u0272\u00032\u0019\u0000\u0271\u0270\u0001\u0000\u0000\u0000\u0271\u0272"+
		"\u0001\u0000\u0000\u0000\u0272\u0274\u0001\u0000\u0000\u0000\u0273\u0275"+
		"\u0003j5\u0000\u0274\u0273\u0001\u0000\u0000\u0000\u0274\u0275\u0001\u0000"+
		"\u0000\u0000\u0275i\u0001\u0000\u0000\u0000\u0276\u027a\u0005\u0004\u0000"+
		"\u0000\u0277\u0279\u0003T*\u0000\u0278\u0277\u0001\u0000\u0000\u0000\u0279"+
		"\u027c\u0001\u0000\u0000\u0000\u027a\u0278\u0001\u0000\u0000\u0000\u027a"+
		"\u027b\u0001\u0000\u0000\u0000\u027b\u027d\u0001\u0000\u0000\u0000\u027c"+
		"\u027a\u0001\u0000\u0000\u0000\u027d\u027e\u0005\u0005\u0000\u0000\u027e"+
		"k\u0001\u0000\u0000\u0000\u027f\u0280\u0005\u0013\u0000\u0000\u0280\u0281"+
		"\u0005\u001c\u0000\u0000\u0281\u0283\u0003x<\u0000\u0282\u0284\u00032"+
		"\u0019\u0000\u0283\u0282\u0001\u0000\u0000\u0000\u0283\u0284\u0001\u0000"+
		"\u0000\u0000\u0284\u0285\u0001\u0000\u0000\u0000\u0285\u0286\u0003j5\u0000"+
		"\u0286\u028d\u0001\u0000\u0000\u0000\u0287\u0288\u0005\u0013\u0000\u0000"+
		"\u0288\u0289\u0005\u001c\u0000\u0000\u0289\u028a\u0003x<\u0000\u028a\u028b"+
		"\u00032\u0019\u0000\u028b\u028d\u0001\u0000\u0000\u0000\u028c\u027f\u0001"+
		"\u0000\u0000\u0000\u028c\u0287\u0001\u0000\u0000\u0000\u028dm\u0001\u0000"+
		"\u0000\u0000\u028e\u0290\u0003z=\u0000\u028f\u028e\u0001\u0000\u0000\u0000"+
		"\u028f\u0290\u0001\u0000\u0000\u0000\u0290\u0291\u0001\u0000\u0000\u0000"+
		"\u0291\u0292\u0005\u001d\u0000\u0000\u0292\u0293\u0005\u0011\u0000\u0000"+
		"\u0293\u0295\u0003x<\u0000\u0294\u0296\u0003R)\u0000\u0295\u0294\u0001"+
		"\u0000\u0000\u0000\u0295\u0296\u0001\u0000\u0000\u0000\u0296\u0298\u0001"+
		"\u0000\u0000\u0000\u0297\u0299\u0005\u001e\u0000\u0000\u0298\u0297\u0001"+
		"\u0000\u0000\u0000\u0298\u0299\u0001\u0000\u0000\u0000\u0299\u029a\u0001"+
		"\u0000\u0000\u0000\u029a\u029b\u0005\u000b\u0000\u0000\u029b\u029c\u0003"+
		"p8\u0000\u029co\u0001\u0000\u0000\u0000\u029d\u029f\u00068\uffff\uffff"+
		"\u0000\u029e\u02a0\u0005\u001a\u0000\u0000\u029f\u029e\u0001\u0000\u0000"+
		"\u0000\u029f\u02a0\u0001\u0000\u0000\u0000\u02a0\u02a1\u0001\u0000\u0000"+
		"\u0000\u02a1\u02a2\u0003r9\u0000\u02a2\u02a8\u0001\u0000\u0000\u0000\u02a3"+
		"\u02a4\n\u0001\u0000\u0000\u02a4\u02a5\u0005\u001a\u0000\u0000\u02a5\u02a7"+
		"\u0003r9\u0000\u02a6\u02a3\u0001\u0000\u0000\u0000\u02a7\u02aa\u0001\u0000"+
		"\u0000\u0000\u02a8\u02a6\u0001\u0000\u0000\u0000\u02a8\u02a9\u0001\u0000"+
		"\u0000\u0000\u02a9q\u0001\u0000\u0000\u0000\u02aa\u02a8\u0001\u0000\u0000"+
		"\u0000\u02ab\u02ae\u0003t:\u0000\u02ac\u02ae\u0003v;\u0000\u02ad\u02ab"+
		"\u0001\u0000\u0000\u0000\u02ad\u02ac\u0001\u0000\u0000\u0000\u02aes\u0001"+
		"\u0000\u0000\u0000\u02af\u02b0\u0007\u0001\u0000\u0000\u02b0u\u0001\u0000"+
		"\u0000\u0000\u02b1\u02b2\u0007\u0002\u0000\u0000\u02b2w\u0001\u0000\u0000"+
		"\u0000\u02b3\u02b4\u0007\u0003\u0000\u0000\u02b4y\u0001\u0000\u0000\u0000"+
		"\u02b5\u02b6\u0003\u008cF\u0000\u02b6{\u0001\u0000\u0000\u0000\u02b7\u02b8"+
		"\u0007\u0004\u0000\u0000\u02b8}\u0001\u0000\u0000\u0000\u02b9\u02ba\u0005"+
		"4\u0000\u0000\u02ba\u007f\u0001\u0000\u0000\u0000\u02bb\u02bc\u0003x<"+
		"\u0000\u02bc\u0081\u0001\u0000\u0000\u0000\u02bd\u02be\u0005\u000e\u0000"+
		"\u0000\u02be\u02cd\u0005\u000f\u0000\u0000\u02bf\u02c0\u0005\u000e\u0000"+
		"\u0000\u02c0\u02c7\u0003 \u0010\u0000\u02c1\u02c3\u0005>\u0000\u0000\u02c2"+
		"\u02c1\u0001\u0000\u0000\u0000\u02c2\u02c3\u0001\u0000\u0000\u0000\u02c3"+
		"\u02c4\u0001\u0000\u0000\u0000\u02c4\u02c6\u0003 \u0010\u0000\u02c5\u02c2"+
		"\u0001\u0000\u0000\u0000\u02c6\u02c9\u0001\u0000\u0000\u0000\u02c7\u02c5"+
		"\u0001\u0000\u0000\u0000\u02c7\u02c8\u0001\u0000\u0000\u0000\u02c8\u02ca"+
		"\u0001\u0000\u0000\u0000\u02c9\u02c7\u0001\u0000\u0000\u0000\u02ca\u02cb"+
		"\u0005\u000f\u0000\u0000\u02cb\u02cd\u0001\u0000\u0000\u0000\u02cc\u02bd"+
		"\u0001\u0000\u0000\u0000\u02cc\u02bf\u0001\u0000\u0000\u0000\u02cd\u0083"+
		"\u0001\u0000\u0000\u0000\u02ce\u02cf\u0005\u0004\u0000\u0000\u02cf\u02de"+
		"\u0005\u0005\u0000\u0000\u02d0\u02d1\u0005\u0004\u0000\u0000\u02d1\u02d8"+
		"\u0003\u0086C\u0000\u02d2\u02d4\u0005>\u0000\u0000\u02d3\u02d2\u0001\u0000"+
		"\u0000\u0000\u02d3\u02d4\u0001\u0000\u0000\u0000\u02d4\u02d5\u0001\u0000"+
		"\u0000\u0000\u02d5\u02d7\u0003\u0086C\u0000\u02d6\u02d3\u0001\u0000\u0000"+
		"\u0000\u02d7\u02da\u0001\u0000\u0000\u0000\u02d8\u02d6\u0001\u0000\u0000"+
		"\u0000\u02d8\u02d9\u0001\u0000\u0000\u0000\u02d9\u02db\u0001\u0000\u0000"+
		"\u0000\u02da\u02d8\u0001\u0000\u0000\u0000\u02db\u02dc\u0005\u0005\u0000"+
		"\u0000\u02dc\u02de\u0001\u0000\u0000\u0000\u02dd\u02ce\u0001\u0000\u0000"+
		"\u0000\u02dd\u02d0\u0001\u0000\u0000\u0000\u02de\u0085\u0001\u0000\u0000"+
		"\u0000\u02df\u02e0\u0003x<\u0000\u02e0\u02e1\u0005\u0006\u0000\u0000\u02e1"+
		"\u02e2\u0003 \u0010\u0000\u02e2\u0087\u0001\u0000\u0000\u0000\u02e3\u02e4"+
		"\u00057\u0000\u0000\u02e4\u0089\u0001\u0000\u0000\u0000\u02e5\u02e6\u0005"+
		"8\u0000\u0000\u02e6\u008b\u0001\u0000\u0000\u0000\u02e7\u02e8\u0007\u0005"+
		"\u0000\u0000\u02e8\u008d\u0001\u0000\u0000\u0000e\u0091\u0099\u009d\u00a1"+
		"\u00a4\u00a7\u00ac\u00b4\u00bc\u00bf\u00c3\u00c6\u00c9\u00d1\u00d6\u00d9"+
		"\u00e4\u00e8\u00eb\u00f3\u0105\u010d\u0112\u011b\u011e\u0126\u0134\u0139"+
		"\u013e\u0143\u0147\u014a\u014e\u0154\u015f\u0165\u016c\u0174\u017c\u017f"+
		"\u0184\u018c\u0191\u0194\u0197\u019d\u01a0\u01a8\u01b1\u01b6\u01bf\u01c6"+
		"\u01cc\u01d0\u01d5\u01da\u01df\u01e5\u01eb\u01ee\u01f1\u01f6\u01f9\u01fc"+
		"\u0202\u0205\u020d\u0216\u0219\u021e\u0221\u0226\u022f\u0236\u023f\u0242"+
		"\u0247\u024a\u0250\u0256\u025a\u0260\u0269\u026c\u0271\u0274\u027a\u0283"+
		"\u028c\u028f\u0295\u0298\u029f\u02a8\u02ad\u02c2\u02c7\u02cc\u02d3\u02d8"+
		"\u02dd";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}