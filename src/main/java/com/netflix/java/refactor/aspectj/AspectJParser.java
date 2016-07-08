// Generated from /Users/joschneider/Projects/github/jkschneider/java-source-linter/src/main/antlr4/AspectJParser.g4 by ANTLR 4.2.2
package com.netflix.java.refactor.aspectj;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class AspectJParser extends Parser {
	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		ADVICEEXECUTION=3, THROW=102, AFTER=6, ISSINGLETON=19, SOFT=32, STATIC=96, 
		ANNOTATION_AFTERTHROWING=41, PREINITIALIZATION=28, INTERFACE=86, AND_ASSIGN=151, 
		ANNOTATION=4, BREAK=62, BYTE=63, ELSE=73, ERROR=14, ANNOTATION_CONSTRUCTOR=51, 
		IF=80, ANNOTATION_DECLAREMIXIN=46, CFLOW=11, ENUM=74, SUB=140, ANNOTATION_BEFORE=44, 
		BANG=127, LPAREN=115, DOT=123, PERTARGET=23, THROWING=35, CASE=64, AT=58, 
		LINE_COMMENT=162, StringLiteral=113, ELLIPSIS=159, LBRACK=119, GET=16, 
		ANNOTATION_VALUE=57, PUBLIC=93, THROWS=103, HANDLER=17, NullLiteral=114, 
		DQUOTE=2, PERCFLOW=21, RSHIFT_ASSIGN=156, LBRACE=117, PERTHIS=24, CALL=10, 
		GOTO=81, SET=31, SUB_ASSIGN=148, SEMI=121, CHAR=66, ASSIGN=124, ANNOTATION_INTERFACES=54, 
		RETURNING=30, COMMENT=161, ANNOTATION_FIELD=53, IMPORT=83, BITOR=144, 
		CATCH=65, MUL_ASSIGN=149, DOUBLE=72, PERCFLOWBELOW=22, PROTECTED=92, AROUND=7, 
		INITIALIZATION=18, LONG=87, COMMA=122, BITAND=143, PRECEDENCE=27, PARENTS=20, 
		COMMENT4=175, COMMENT3=171, ANNOTATION_AROUND=42, COMMENT2=168, PRIVATE=91, 
		COMMENT1=164, CONTINUE=69, DIV=142, FloatingPointLiteral=110, LE=132, 
		CharacterLiteral=112, TARGET=34, VOLATILE=107, EXTENDS=75, INSTANCEOF=84, 
		NEW=89, ADD=139, ANNOTATION_METHOD=56, LT=126, CLASS=67, DO=71, FINALLY=77, 
		Identifier=158, LINE_COMMENT3=172, LINE_COMMENT2=169, CONST=68, PACKAGE=90, 
		LINE_COMMENT1=165, OR_ASSIGN=152, TRY=105, PRIVILEGED=29, IntegerLiteral=109, 
		ANNOTATION_TYPE=55, SYNCHRONIZED=100, STATICINITIALIZATION=33, MUL=141, 
		FOR=79, FINAL=76, ANNOTATION_DECLAREPRECEDENCE=49, RPAREN=116, WARNING=36, 
		CARET=145, URSHIFT_ASSIGN=157, BOOLEAN=61, NOTEQUAL=134, RBRACK=120, RBRACE=118, 
		AND=135, THIS=101, SWITCH=99, VOID=106, TRANSIENT=104, INC=137, ANNOTATION_AFTERRETURNING=40, 
		WITHINCODE=38, FLOAT=78, NATIVE=88, ARGS=5, DIV_ASSIGN=150, BooleanLiteral=111, 
		WITHIN=37, ANNOTATION_DEFAULTIMPL=52, CFLOWBELOW=12, ABSTRACT=59, STRICTFP=97, 
		INT=85, QUESTION=129, RETURN=94, WS1=163, LSHIFT_ASSIGN=155, WS3=170, 
		ANNOTATION_ASPECT=43, WS2=167, WS4=174, ADD_ASSIGN=147, POINTCUT=26, WS=160, 
		GE=133, ANNOTATION_DECLAREWARNING=47, SUPER=98, ANNOTATION_DECLAREERROR=48, 
		OR=136, DEC=138, MOD=146, XOR_ASSIGN=153, PERTYPEWITHIN=25, ASSERT=60, 
		EQUAL=131, INVALID3=173, EXECUTION=15, INVALID4=177, DOTDOT=1, IMPLEMENTS=82, 
		COLON=130, LINE_COMMENT4=176, GT=125, INVALID1=166, SHORT=95, BEFORE=9, 
		DECLARE=13, MOD_ASSIGN=154, ANNOTATION_POINTCUT=50, ASPECT=8, WHILE=108, 
		TILDE=128, ANNOTATION_DECLAREPARENTS=45, ANNOTATION_AFTER=39, DEFAULT=70;
	public static final String[] tokenNames = {
		"<INVALID>", "'..'", "'\"'", "'adviceexecution'", "'annotation'", "'args'", 
		"'after'", "'around'", "'aspect'", "'before'", "'call'", "'cflow'", "'cflowbelow'", 
		"'declare'", "'error'", "'execution'", "'get'", "'handler'", "'initialization'", 
		"'issingleton'", "'parents'", "'percflow'", "'percflowbelow'", "'pertarget'", 
		"'perthis'", "'pertypewithin'", "'pointcut'", "'precedence'", "'preinitialization'", 
		"'privileged'", "'returning'", "'set'", "'soft'", "'staticinitialization'", 
		"'target'", "'throwing'", "'warning'", "'within'", "'withincode'", "'After'", 
		"'AfterReturning'", "'AfterThrowing'", "'Around'", "'Aspect'", "'Before'", 
		"'DeclareParents'", "'DeclareMixin'", "'DeclareWarning'", "'DeclareError'", 
		"'DeclarePrecedence'", "'Pointcut'", "'constructor'", "'defaultImpl'", 
		"'field'", "'interfaces'", "'type'", "'method'", "'value'", "'@'", "'abstract'", 
		"'assert'", "'boolean'", "'break'", "'byte'", "'case'", "'catch'", "'char'", 
		"'class'", "'const'", "'continue'", "'default'", "'do'", "'double'", "'else'", 
		"'enum'", "'extends'", "'final'", "'finally'", "'float'", "'for'", "'if'", 
		"'goto'", "'implements'", "'import'", "'instanceof'", "'int'", "'interface'", 
		"'long'", "'native'", "'new'", "'package'", "'private'", "'protected'", 
		"'public'", "'return'", "'short'", "'static'", "'strictfp'", "'super'", 
		"'switch'", "'synchronized'", "'this'", "'throw'", "'throws'", "'transient'", 
		"'try'", "'void'", "'volatile'", "'while'", "IntegerLiteral", "FloatingPointLiteral", 
		"BooleanLiteral", "CharacterLiteral", "StringLiteral", "'null'", "'('", 
		"')'", "'{'", "'}'", "'['", "']'", "';'", "','", "'.'", "'='", "'>'", 
		"'<'", "'!'", "'~'", "'?'", "':'", "'=='", "'<='", "'>='", "'!='", "'&&'", 
		"'||'", "'++'", "'--'", "'+'", "'-'", "'*'", "'/'", "'&'", "'|'", "'^'", 
		"'%'", "'+='", "'-='", "'*='", "'/='", "'&='", "'|='", "'^='", "'%='", 
		"'<<='", "'>>='", "'>>>='", "Identifier", "'...'", "WS", "COMMENT", "LINE_COMMENT", 
		"WS1", "COMMENT1", "LINE_COMMENT1", "INVALID1", "WS2", "COMMENT2", "LINE_COMMENT2", 
		"WS3", "COMMENT3", "LINE_COMMENT3", "INVALID3", "WS4", "COMMENT4", "LINE_COMMENT4", 
		"INVALID4"
	};
	public static final int
		RULE_typeDeclaration = 0, RULE_aspectBody = 1, RULE_classBodyDeclaration = 2, 
		RULE_aspectBodyDeclaration = 3, RULE_memberDeclaration = 4, RULE_annotation = 5, 
		RULE_classPattern = 6, RULE_classPatternList = 7, RULE_aspectDeclaration = 8, 
		RULE_advice = 9, RULE_adviceSpec = 10, RULE_perClause = 11, RULE_pointcutDeclaration = 12, 
		RULE_pointcutExpression = 13, RULE_pointcutPrimitive = 14, RULE_referencePointcut = 15, 
		RULE_interTypeMemberDeclaration = 16, RULE_interTypeDeclaration = 17, 
		RULE_typePattern = 18, RULE_simpleTypePattern = 19, RULE_dottedNamePattern = 20, 
		RULE_optionalParensTypePattern = 21, RULE_fieldPattern = 22, RULE_fieldModifiersPattern = 23, 
		RULE_fieldModifier = 24, RULE_dotOrDotDot = 25, RULE_simpleNamePattern = 26, 
		RULE_methodOrConstructorPattern = 27, RULE_methodPattern = 28, RULE_methodModifiersPattern = 29, 
		RULE_methodModifier = 30, RULE_formalsPattern = 31, RULE_formalsPatternAfterDotDot = 32, 
		RULE_throwsPattern = 33, RULE_typePatternList = 34, RULE_constructorPattern = 35, 
		RULE_constructorModifiersPattern = 36, RULE_constructorModifier = 37, 
		RULE_annotationPattern = 38, RULE_annotationTypePattern = 39, RULE_formalParametersPattern = 40, 
		RULE_typeOrIdentifier = 41, RULE_annotationOrIdentifer = 42, RULE_annotationsOrIdentifiersPattern = 43, 
		RULE_annotationsOrIdentifiersPatternAfterDotDot = 44, RULE_argsPattern = 45, 
		RULE_argsPatternList = 46, RULE_id = 47, RULE_classDeclaration = 48, RULE_typeParameter = 49, 
		RULE_enumDeclaration = 50, RULE_enumConstant = 51, RULE_interfaceDeclaration = 52, 
		RULE_methodDeclaration = 53, RULE_constructorDeclaration = 54, RULE_constantDeclarator = 55, 
		RULE_interfaceMethodDeclaration = 56, RULE_variableDeclaratorId = 57, 
		RULE_enumConstantName = 58, RULE_classOrInterfaceType = 59, RULE_qualifiedName = 60, 
		RULE_elementValuePair = 61, RULE_annotationTypeDeclaration = 62, RULE_annotationMethodRest = 63, 
		RULE_statement = 64, RULE_catchClause = 65, RULE_expression = 66, RULE_primary = 67, 
		RULE_createdName = 68, RULE_innerCreator = 69, RULE_superSuffix = 70, 
		RULE_explicitGenericInvocationSuffix = 71, RULE_compilationUnit = 72, 
		RULE_packageDeclaration = 73, RULE_importDeclaration = 74, RULE_modifier = 75, 
		RULE_classOrInterfaceModifier = 76, RULE_variableModifier = 77, RULE_typeParameters = 78, 
		RULE_typeBound = 79, RULE_enumConstants = 80, RULE_enumBodyDeclarations = 81, 
		RULE_typeList = 82, RULE_classBody = 83, RULE_interfaceBody = 84, RULE_genericMethodDeclaration = 85, 
		RULE_genericConstructorDeclaration = 86, RULE_fieldDeclaration = 87, RULE_interfaceBodyDeclaration = 88, 
		RULE_interfaceMemberDeclaration = 89, RULE_constDeclaration = 90, RULE_genericInterfaceMethodDeclaration = 91, 
		RULE_variableDeclarators = 92, RULE_variableDeclarator = 93, RULE_variableInitializer = 94, 
		RULE_arrayInitializer = 95, RULE_type = 96, RULE_primitiveType = 97, RULE_typeArguments = 98, 
		RULE_typeArgument = 99, RULE_qualifiedNameList = 100, RULE_formalParameters = 101, 
		RULE_formalParameterList = 102, RULE_formalParameter = 103, RULE_lastFormalParameter = 104, 
		RULE_methodBody = 105, RULE_constructorBody = 106, RULE_literal = 107, 
		RULE_annotationName = 108, RULE_elementValuePairs = 109, RULE_elementValue = 110, 
		RULE_elementValueArrayInitializer = 111, RULE_annotationTypeBody = 112, 
		RULE_annotationTypeElementDeclaration = 113, RULE_annotationTypeElementRest = 114, 
		RULE_annotationMethodOrConstantRest = 115, RULE_annotationConstantRest = 116, 
		RULE_defaultValue = 117, RULE_block = 118, RULE_blockStatement = 119, 
		RULE_localVariableDeclarationStatement = 120, RULE_localVariableDeclaration = 121, 
		RULE_catchType = 122, RULE_finallyBlock = 123, RULE_resourceSpecification = 124, 
		RULE_resources = 125, RULE_resource = 126, RULE_switchBlockStatementGroup = 127, 
		RULE_switchLabel = 128, RULE_forControl = 129, RULE_forInit = 130, RULE_enhancedForControl = 131, 
		RULE_forUpdate = 132, RULE_parExpression = 133, RULE_expressionList = 134, 
		RULE_statementExpression = 135, RULE_constantExpression = 136, RULE_creator = 137, 
		RULE_arrayCreatorRest = 138, RULE_classCreatorRest = 139, RULE_explicitGenericInvocation = 140, 
		RULE_nonWildcardTypeArguments = 141, RULE_typeArgumentsOrDiamond = 142, 
		RULE_nonWildcardTypeArgumentsOrDiamond = 143, RULE_arguments = 144;
	public static final String[] ruleNames = {
		"typeDeclaration", "aspectBody", "classBodyDeclaration", "aspectBodyDeclaration", 
		"memberDeclaration", "annotation", "classPattern", "classPatternList", 
		"aspectDeclaration", "advice", "adviceSpec", "perClause", "pointcutDeclaration", 
		"pointcutExpression", "pointcutPrimitive", "referencePointcut", "interTypeMemberDeclaration", 
		"interTypeDeclaration", "typePattern", "simpleTypePattern", "dottedNamePattern", 
		"optionalParensTypePattern", "fieldPattern", "fieldModifiersPattern", 
		"fieldModifier", "dotOrDotDot", "simpleNamePattern", "methodOrConstructorPattern", 
		"methodPattern", "methodModifiersPattern", "methodModifier", "formalsPattern", 
		"formalsPatternAfterDotDot", "throwsPattern", "typePatternList", "constructorPattern", 
		"constructorModifiersPattern", "constructorModifier", "annotationPattern", 
		"annotationTypePattern", "formalParametersPattern", "typeOrIdentifier", 
		"annotationOrIdentifer", "annotationsOrIdentifiersPattern", "annotationsOrIdentifiersPatternAfterDotDot", 
		"argsPattern", "argsPatternList", "id", "classDeclaration", "typeParameter", 
		"enumDeclaration", "enumConstant", "interfaceDeclaration", "methodDeclaration", 
		"constructorDeclaration", "constantDeclarator", "interfaceMethodDeclaration", 
		"variableDeclaratorId", "enumConstantName", "classOrInterfaceType", "qualifiedName", 
		"elementValuePair", "annotationTypeDeclaration", "annotationMethodRest", 
		"statement", "catchClause", "expression", "primary", "createdName", "innerCreator", 
		"superSuffix", "explicitGenericInvocationSuffix", "compilationUnit", "packageDeclaration", 
		"importDeclaration", "modifier", "classOrInterfaceModifier", "variableModifier", 
		"typeParameters", "typeBound", "enumConstants", "enumBodyDeclarations", 
		"typeList", "classBody", "interfaceBody", "genericMethodDeclaration", 
		"genericConstructorDeclaration", "fieldDeclaration", "interfaceBodyDeclaration", 
		"interfaceMemberDeclaration", "constDeclaration", "genericInterfaceMethodDeclaration", 
		"variableDeclarators", "variableDeclarator", "variableInitializer", "arrayInitializer", 
		"type", "primitiveType", "typeArguments", "typeArgument", "qualifiedNameList", 
		"formalParameters", "formalParameterList", "formalParameter", "lastFormalParameter", 
		"methodBody", "constructorBody", "literal", "annotationName", "elementValuePairs", 
		"elementValue", "elementValueArrayInitializer", "annotationTypeBody", 
		"annotationTypeElementDeclaration", "annotationTypeElementRest", "annotationMethodOrConstantRest", 
		"annotationConstantRest", "defaultValue", "block", "blockStatement", "localVariableDeclarationStatement", 
		"localVariableDeclaration", "catchType", "finallyBlock", "resourceSpecification", 
		"resources", "resource", "switchBlockStatementGroup", "switchLabel", "forControl", 
		"forInit", "enhancedForControl", "forUpdate", "parExpression", "expressionList", 
		"statementExpression", "constantExpression", "creator", "arrayCreatorRest", 
		"classCreatorRest", "explicitGenericInvocation", "nonWildcardTypeArguments", 
		"typeArgumentsOrDiamond", "nonWildcardTypeArgumentsOrDiamond", "arguments"
	};

	@Override
	public String getGrammarFileName() { return "AspectJParser.g4"; }

	@Override
	public String[] getTokenNames() { return tokenNames; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public AspectJParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class TypeDeclarationContext extends ParserRuleContext {
		public ClassOrInterfaceModifierContext classOrInterfaceModifier(int i) {
			return getRuleContext(ClassOrInterfaceModifierContext.class,i);
		}
		public EnumDeclarationContext enumDeclaration() {
			return getRuleContext(EnumDeclarationContext.class,0);
		}
		public ClassDeclarationContext classDeclaration() {
			return getRuleContext(ClassDeclarationContext.class,0);
		}
		public AnnotationTypeDeclarationContext annotationTypeDeclaration() {
			return getRuleContext(AnnotationTypeDeclarationContext.class,0);
		}
		public List<ClassOrInterfaceModifierContext> classOrInterfaceModifier() {
			return getRuleContexts(ClassOrInterfaceModifierContext.class);
		}
		public InterfaceDeclarationContext interfaceDeclaration() {
			return getRuleContext(InterfaceDeclarationContext.class,0);
		}
		public AspectDeclarationContext aspectDeclaration() {
			return getRuleContext(AspectDeclarationContext.class,0);
		}
		public TypeDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterTypeDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitTypeDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitTypeDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeDeclarationContext typeDeclaration() throws RecognitionException {
		TypeDeclarationContext _localctx = new TypeDeclarationContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_typeDeclaration);
		int _la;
		try {
			int _alt;
			setState(326);
			switch ( getInterpreter().adaptivePredict(_input,5,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(293);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (ABSTRACT - 58)) | (1L << (FINAL - 58)) | (1L << (PRIVATE - 58)) | (1L << (PROTECTED - 58)) | (1L << (PUBLIC - 58)) | (1L << (STATIC - 58)) | (1L << (STRICTFP - 58)))) != 0)) {
					{
					{
					setState(290); classOrInterfaceModifier();
					}
					}
					setState(295);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(296); classDeclaration();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(300);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (ABSTRACT - 58)) | (1L << (FINAL - 58)) | (1L << (PRIVATE - 58)) | (1L << (PROTECTED - 58)) | (1L << (PUBLIC - 58)) | (1L << (STATIC - 58)) | (1L << (STRICTFP - 58)))) != 0)) {
					{
					{
					setState(297); classOrInterfaceModifier();
					}
					}
					setState(302);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(303); enumDeclaration();
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(307);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (ABSTRACT - 58)) | (1L << (FINAL - 58)) | (1L << (PRIVATE - 58)) | (1L << (PROTECTED - 58)) | (1L << (PUBLIC - 58)) | (1L << (STATIC - 58)) | (1L << (STRICTFP - 58)))) != 0)) {
					{
					{
					setState(304); classOrInterfaceModifier();
					}
					}
					setState(309);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(310); interfaceDeclaration();
				}
				break;

			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(314);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,3,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(311); classOrInterfaceModifier();
						}
						} 
					}
					setState(316);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,3,_ctx);
				}
				setState(317); annotationTypeDeclaration();
				}
				break;

			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(321);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,4,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(318); classOrInterfaceModifier();
						}
						} 
					}
					setState(323);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,4,_ctx);
				}
				setState(324); aspectDeclaration();
				}
				break;

			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(325); match(SEMI);
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

	public static class AspectBodyContext extends ParserRuleContext {
		public AspectBodyDeclarationContext aspectBodyDeclaration(int i) {
			return getRuleContext(AspectBodyDeclarationContext.class,i);
		}
		public List<AspectBodyDeclarationContext> aspectBodyDeclaration() {
			return getRuleContexts(AspectBodyDeclarationContext.class);
		}
		public AspectBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_aspectBody; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterAspectBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitAspectBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitAspectBody(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AspectBodyContext aspectBody() throws RecognitionException {
		AspectBodyContext _localctx = new AspectBodyContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_aspectBody);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(328); match(LBRACE);
			setState(332);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ARGS) | (1L << AFTER) | (1L << AROUND) | (1L << ASPECT) | (1L << BEFORE) | (1L << CALL) | (1L << CFLOW) | (1L << CFLOWBELOW) | (1L << DECLARE) | (1L << ERROR) | (1L << EXECUTION) | (1L << GET) | (1L << HANDLER) | (1L << INITIALIZATION) | (1L << ISSINGLETON) | (1L << PARENTS) | (1L << PERCFLOW) | (1L << PERCFLOWBELOW) | (1L << PERTARGET) | (1L << PERTHIS) | (1L << PERTYPEWITHIN) | (1L << POINTCUT) | (1L << PRECEDENCE) | (1L << PREINITIALIZATION) | (1L << PRIVILEGED) | (1L << RETURNING) | (1L << SET) | (1L << SOFT) | (1L << STATICINITIALIZATION) | (1L << TARGET) | (1L << THROWING) | (1L << WARNING) | (1L << WITHIN) | (1L << WITHINCODE) | (1L << AT) | (1L << ABSTRACT) | (1L << BOOLEAN) | (1L << BYTE))) != 0) || ((((_la - 66)) & ~0x3f) == 0 && ((1L << (_la - 66)) & ((1L << (CHAR - 66)) | (1L << (CLASS - 66)) | (1L << (DOUBLE - 66)) | (1L << (ENUM - 66)) | (1L << (FINAL - 66)) | (1L << (FLOAT - 66)) | (1L << (INT - 66)) | (1L << (INTERFACE - 66)) | (1L << (LONG - 66)) | (1L << (NATIVE - 66)) | (1L << (PRIVATE - 66)) | (1L << (PROTECTED - 66)) | (1L << (PUBLIC - 66)) | (1L << (SHORT - 66)) | (1L << (STATIC - 66)) | (1L << (STRICTFP - 66)) | (1L << (SYNCHRONIZED - 66)) | (1L << (TRANSIENT - 66)) | (1L << (VOID - 66)) | (1L << (VOLATILE - 66)) | (1L << (LBRACE - 66)) | (1L << (SEMI - 66)) | (1L << (LT - 66)))) != 0) || _la==Identifier) {
				{
				{
				setState(329); aspectBodyDeclaration();
				}
				}
				setState(334);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(335); match(RBRACE);
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

	public static class ClassBodyDeclarationContext extends ParserRuleContext {
		public List<ModifierContext> modifier() {
			return getRuleContexts(ModifierContext.class);
		}
		public MemberDeclarationContext memberDeclaration() {
			return getRuleContext(MemberDeclarationContext.class,0);
		}
		public AspectDeclarationContext aspectDeclaration() {
			return getRuleContext(AspectDeclarationContext.class,0);
		}
		public ModifierContext modifier(int i) {
			return getRuleContext(ModifierContext.class,i);
		}
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public ClassBodyDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_classBodyDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterClassBodyDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitClassBodyDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitClassBodyDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ClassBodyDeclarationContext classBodyDeclaration() throws RecognitionException {
		ClassBodyDeclarationContext _localctx = new ClassBodyDeclarationContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_classBodyDeclaration);
		int _la;
		try {
			int _alt;
			setState(351);
			switch ( getInterpreter().adaptivePredict(_input,9,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(337); match(SEMI);
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(339);
				_la = _input.LA(1);
				if (_la==STATIC) {
					{
					setState(338); match(STATIC);
					}
				}

				setState(341); block();
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(345);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,8,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(342); modifier();
						}
						} 
					}
					setState(347);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,8,_ctx);
				}
				setState(348); memberDeclaration();
				}
				break;

			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(349); match(STATIC);
				setState(350); aspectDeclaration();
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

	public static class AspectBodyDeclarationContext extends ParserRuleContext {
		public ClassBodyDeclarationContext classBodyDeclaration() {
			return getRuleContext(ClassBodyDeclarationContext.class,0);
		}
		public AdviceContext advice() {
			return getRuleContext(AdviceContext.class,0);
		}
		public InterTypeMemberDeclarationContext interTypeMemberDeclaration() {
			return getRuleContext(InterTypeMemberDeclarationContext.class,0);
		}
		public InterTypeDeclarationContext interTypeDeclaration() {
			return getRuleContext(InterTypeDeclarationContext.class,0);
		}
		public AspectBodyDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_aspectBodyDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterAspectBodyDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitAspectBodyDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitAspectBodyDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AspectBodyDeclarationContext aspectBodyDeclaration() throws RecognitionException {
		AspectBodyDeclarationContext _localctx = new AspectBodyDeclarationContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_aspectBodyDeclaration);
		try {
			setState(357);
			switch ( getInterpreter().adaptivePredict(_input,10,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(353); classBodyDeclaration();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(354); advice();
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(355); interTypeMemberDeclaration();
				}
				break;

			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(356); interTypeDeclaration();
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

	public static class MemberDeclarationContext extends ParserRuleContext {
		public GenericMethodDeclarationContext genericMethodDeclaration() {
			return getRuleContext(GenericMethodDeclarationContext.class,0);
		}
		public MethodDeclarationContext methodDeclaration() {
			return getRuleContext(MethodDeclarationContext.class,0);
		}
		public EnumDeclarationContext enumDeclaration() {
			return getRuleContext(EnumDeclarationContext.class,0);
		}
		public ClassDeclarationContext classDeclaration() {
			return getRuleContext(ClassDeclarationContext.class,0);
		}
		public AnnotationTypeDeclarationContext annotationTypeDeclaration() {
			return getRuleContext(AnnotationTypeDeclarationContext.class,0);
		}
		public GenericConstructorDeclarationContext genericConstructorDeclaration() {
			return getRuleContext(GenericConstructorDeclarationContext.class,0);
		}
		public PointcutDeclarationContext pointcutDeclaration() {
			return getRuleContext(PointcutDeclarationContext.class,0);
		}
		public InterfaceDeclarationContext interfaceDeclaration() {
			return getRuleContext(InterfaceDeclarationContext.class,0);
		}
		public ConstructorDeclarationContext constructorDeclaration() {
			return getRuleContext(ConstructorDeclarationContext.class,0);
		}
		public FieldDeclarationContext fieldDeclaration() {
			return getRuleContext(FieldDeclarationContext.class,0);
		}
		public MemberDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_memberDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterMemberDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitMemberDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitMemberDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MemberDeclarationContext memberDeclaration() throws RecognitionException {
		MemberDeclarationContext _localctx = new MemberDeclarationContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_memberDeclaration);
		try {
			setState(369);
			switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(359); methodDeclaration();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(360); genericMethodDeclaration();
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(361); fieldDeclaration();
				}
				break;

			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(362); constructorDeclaration();
				}
				break;

			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(363); genericConstructorDeclaration();
				}
				break;

			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(364); interfaceDeclaration();
				}
				break;

			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(365); annotationTypeDeclaration();
				}
				break;

			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(366); classDeclaration();
				}
				break;

			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(367); enumDeclaration();
				}
				break;

			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(368); pointcutDeclaration();
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

	public static class AnnotationContext extends ParserRuleContext {
		public PerClauseContext perClause() {
			return getRuleContext(PerClauseContext.class,0);
		}
		public ClassPatternContext classPattern() {
			return getRuleContext(ClassPatternContext.class,0);
		}
		public ElementValuePairsContext elementValuePairs() {
			return getRuleContext(ElementValuePairsContext.class,0);
		}
		public PointcutExpressionContext pointcutExpression() {
			return getRuleContext(PointcutExpressionContext.class,0);
		}
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public ClassPatternListContext classPatternList() {
			return getRuleContext(ClassPatternListContext.class,0);
		}
		public AnnotationNameContext annotationName() {
			return getRuleContext(AnnotationNameContext.class,0);
		}
		public TypePatternListContext typePatternList() {
			return getRuleContext(TypePatternListContext.class,0);
		}
		public ElementValueContext elementValue() {
			return getRuleContext(ElementValueContext.class,0);
		}
		public TypePatternContext typePattern() {
			return getRuleContext(TypePatternContext.class,0);
		}
		public AnnotationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotation; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterAnnotation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitAnnotation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitAnnotation(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnnotationContext annotation() throws RecognitionException {
		AnnotationContext _localctx = new AnnotationContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_annotation);
		int _la;
		try {
			setState(518);
			switch ( getInterpreter().adaptivePredict(_input,16,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(371); match(AT);
				setState(372); annotationName();
				setState(379);
				_la = _input.LA(1);
				if (_la==LPAREN) {
					{
					setState(373); match(LPAREN);
					setState(376);
					switch ( getInterpreter().adaptivePredict(_input,12,_ctx) ) {
					case 1:
						{
						setState(374); elementValuePairs();
						}
						break;

					case 2:
						{
						setState(375); elementValue();
						}
						break;
					}
					setState(378); match(RPAREN);
					}
				}

				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(381); match(AT);
				setState(382); match(ANNOTATION_AFTER);
				setState(383); match(LPAREN);
				setState(384); match(DQUOTE);
				setState(385); pointcutExpression(0);
				setState(386); match(DQUOTE);
				setState(387); match(RPAREN);
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(389); match(AT);
				setState(390); match(ANNOTATION_AFTERRETURNING);
				setState(391); match(LPAREN);
				setState(392); match(DQUOTE);
				setState(393); pointcutExpression(0);
				setState(394); match(DQUOTE);
				setState(395); match(RPAREN);
				}
				break;

			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(397); match(AT);
				setState(398); match(ANNOTATION_AFTERRETURNING);
				setState(399); match(LPAREN);
				setState(400); match(POINTCUT);
				setState(401); match(ASSIGN);
				setState(402); match(DQUOTE);
				setState(403); pointcutExpression(0);
				setState(404); match(DQUOTE);
				setState(405); match(COMMA);
				setState(406); match(RETURNING);
				setState(407); match(ASSIGN);
				setState(408); match(DQUOTE);
				setState(409); id();
				setState(410); match(DQUOTE);
				setState(411); match(RPAREN);
				}
				break;

			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(413); match(AT);
				setState(414); match(ANNOTATION_AFTERTHROWING);
				setState(415); match(LPAREN);
				setState(416); match(DQUOTE);
				setState(417); pointcutExpression(0);
				setState(418); match(DQUOTE);
				setState(419); match(RPAREN);
				}
				break;

			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(421); match(AT);
				setState(422); match(ANNOTATION_AROUND);
				setState(423); match(LPAREN);
				setState(424); match(DQUOTE);
				setState(425); pointcutExpression(0);
				setState(426); match(DQUOTE);
				setState(427); match(RPAREN);
				}
				break;

			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(429); match(AT);
				setState(430); match(ANNOTATION_ASPECT);
				setState(437);
				_la = _input.LA(1);
				if (_la==LPAREN) {
					{
					setState(431); match(LPAREN);
					setState(432); match(DQUOTE);
					setState(433); perClause();
					setState(434); match(DQUOTE);
					setState(435); match(RPAREN);
					}
				}

				}
				break;

			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(439); match(AT);
				setState(440); match(ANNOTATION_BEFORE);
				setState(441); match(LPAREN);
				setState(442); match(DQUOTE);
				setState(443); pointcutExpression(0);
				setState(444); match(DQUOTE);
				setState(445); match(RPAREN);
				}
				break;

			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(447); match(AT);
				setState(448); match(ANNOTATION_DECLAREERROR);
				setState(449); match(LPAREN);
				setState(450); match(DQUOTE);
				setState(451); pointcutExpression(0);
				setState(452); match(DQUOTE);
				setState(453); match(RPAREN);
				}
				break;

			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(455); match(AT);
				setState(456); match(ANNOTATION_DECLAREMIXIN);
				setState(457); match(LPAREN);
				setState(458); match(ANNOTATION_VALUE);
				setState(459); match(ASSIGN);
				setState(460); match(DQUOTE);
				setState(461); typePattern(0);
				setState(462); match(DQUOTE);
				setState(463); match(COMMA);
				setState(464); match(ANNOTATION_INTERFACES);
				setState(465); match(ASSIGN);
				setState(466); match(LBRACE);
				setState(467); classPatternList();
				setState(468); match(RBRACE);
				setState(469); match(RPAREN);
				}
				break;

			case 11:
				enterOuterAlt(_localctx, 11);
				{
				setState(471); match(AT);
				setState(472); match(ANNOTATION_DECLAREPARENTS);
				setState(473); match(LPAREN);
				setState(474); match(DQUOTE);
				setState(475); typePattern(0);
				setState(476); match(DQUOTE);
				setState(477); match(RPAREN);
				}
				break;

			case 12:
				enterOuterAlt(_localctx, 12);
				{
				setState(479); match(AT);
				setState(480); match(ANNOTATION_DECLAREPARENTS);
				setState(481); match(LPAREN);
				setState(482); match(ANNOTATION_VALUE);
				setState(483); match(ASSIGN);
				setState(484); match(DQUOTE);
				setState(485); typePattern(0);
				setState(486); match(DQUOTE);
				setState(487); match(COMMA);
				setState(488); match(ANNOTATION_DEFAULTIMPL);
				setState(489); match(ASSIGN);
				setState(490); classPattern();
				setState(491); match(RPAREN);
				}
				break;

			case 13:
				enterOuterAlt(_localctx, 13);
				{
				setState(493); match(AT);
				setState(494); match(ANNOTATION_DECLAREPRECEDENCE);
				setState(495); match(LPAREN);
				setState(496); match(DQUOTE);
				setState(497); typePatternList();
				setState(498); match(DQUOTE);
				setState(499); match(RPAREN);
				}
				break;

			case 14:
				enterOuterAlt(_localctx, 14);
				{
				setState(501); match(AT);
				setState(502); match(ANNOTATION_DECLAREWARNING);
				setState(503); match(LPAREN);
				setState(504); match(DQUOTE);
				setState(505); pointcutExpression(0);
				setState(506); match(DQUOTE);
				setState(507); match(RPAREN);
				}
				break;

			case 15:
				enterOuterAlt(_localctx, 15);
				{
				setState(509); match(AT);
				setState(510); match(ANNOTATION_POINTCUT);
				setState(511); match(LPAREN);
				setState(512); match(DQUOTE);
				setState(514);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << DOTDOT) | (1L << ADVICEEXECUTION) | (1L << ARGS) | (1L << AFTER) | (1L << AROUND) | (1L << ASPECT) | (1L << BEFORE) | (1L << CALL) | (1L << CFLOW) | (1L << CFLOWBELOW) | (1L << DECLARE) | (1L << ERROR) | (1L << EXECUTION) | (1L << GET) | (1L << HANDLER) | (1L << INITIALIZATION) | (1L << ISSINGLETON) | (1L << PARENTS) | (1L << PERCFLOW) | (1L << PERCFLOWBELOW) | (1L << PERTARGET) | (1L << PERTHIS) | (1L << PERTYPEWITHIN) | (1L << POINTCUT) | (1L << PRECEDENCE) | (1L << PREINITIALIZATION) | (1L << PRIVILEGED) | (1L << RETURNING) | (1L << SET) | (1L << SOFT) | (1L << STATICINITIALIZATION) | (1L << TARGET) | (1L << THROWING) | (1L << WARNING) | (1L << WITHIN) | (1L << WITHINCODE) | (1L << AT) | (1L << BOOLEAN) | (1L << BYTE))) != 0) || ((((_la - 66)) & ~0x3f) == 0 && ((1L << (_la - 66)) & ((1L << (CHAR - 66)) | (1L << (DOUBLE - 66)) | (1L << (FLOAT - 66)) | (1L << (IF - 66)) | (1L << (INT - 66)) | (1L << (LONG - 66)) | (1L << (SHORT - 66)) | (1L << (THIS - 66)) | (1L << (VOID - 66)) | (1L << (LPAREN - 66)) | (1L << (DOT - 66)) | (1L << (BANG - 66)))) != 0) || _la==MUL || _la==Identifier) {
					{
					setState(513); pointcutExpression(0);
					}
				}

				setState(516); match(DQUOTE);
				setState(517); match(RPAREN);
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

	public static class ClassPatternContext extends ParserRuleContext {
		public IdContext id(int i) {
			return getRuleContext(IdContext.class,i);
		}
		public List<IdContext> id() {
			return getRuleContexts(IdContext.class);
		}
		public ClassPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_classPattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterClassPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitClassPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitClassPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ClassPatternContext classPattern() throws RecognitionException {
		ClassPatternContext _localctx = new ClassPatternContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_classPattern);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(520); id();
			setState(525);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,17,_ctx);
			while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(521); match(DOT);
					setState(522); id();
					}
					} 
				}
				setState(527);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,17,_ctx);
			}
			setState(528); match(DOT);
			setState(529); match(CLASS);
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

	public static class ClassPatternListContext extends ParserRuleContext {
		public ClassPatternContext classPattern(int i) {
			return getRuleContext(ClassPatternContext.class,i);
		}
		public List<ClassPatternContext> classPattern() {
			return getRuleContexts(ClassPatternContext.class);
		}
		public ClassPatternListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_classPatternList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterClassPatternList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitClassPatternList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitClassPatternList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ClassPatternListContext classPatternList() throws RecognitionException {
		ClassPatternListContext _localctx = new ClassPatternListContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_classPatternList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(531); classPattern();
			setState(536);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(532); match(COMMA);
				setState(533); classPattern();
				}
				}
				setState(538);
				_errHandler.sync(this);
				_la = _input.LA(1);
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

	public static class AspectDeclarationContext extends ParserRuleContext {
		public List<ModifierContext> modifier() {
			return getRuleContexts(ModifierContext.class);
		}
		public TypeListContext typeList() {
			return getRuleContext(TypeListContext.class,0);
		}
		public PerClauseContext perClause() {
			return getRuleContext(PerClauseContext.class,0);
		}
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public ModifierContext modifier(int i) {
			return getRuleContext(ModifierContext.class,i);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public AspectBodyContext aspectBody() {
			return getRuleContext(AspectBodyContext.class,0);
		}
		public AspectDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_aspectDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterAspectDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitAspectDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitAspectDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AspectDeclarationContext aspectDeclaration() throws RecognitionException {
		AspectDeclarationContext _localctx = new AspectDeclarationContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_aspectDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(540);
			_la = _input.LA(1);
			if (_la==PRIVILEGED) {
				{
				setState(539); match(PRIVILEGED);
				}
			}

			setState(545);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (ABSTRACT - 58)) | (1L << (FINAL - 58)) | (1L << (NATIVE - 58)) | (1L << (PRIVATE - 58)) | (1L << (PROTECTED - 58)) | (1L << (PUBLIC - 58)) | (1L << (STATIC - 58)) | (1L << (STRICTFP - 58)) | (1L << (SYNCHRONIZED - 58)) | (1L << (TRANSIENT - 58)) | (1L << (VOLATILE - 58)))) != 0)) {
				{
				{
				setState(542); modifier();
				}
				}
				setState(547);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(548); match(ASPECT);
			setState(549); id();
			setState(552);
			_la = _input.LA(1);
			if (_la==EXTENDS) {
				{
				setState(550); match(EXTENDS);
				setState(551); type();
				}
			}

			setState(556);
			_la = _input.LA(1);
			if (_la==IMPLEMENTS) {
				{
				setState(554); match(IMPLEMENTS);
				setState(555); typeList();
				}
			}

			setState(559);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ISSINGLETON) | (1L << PERCFLOW) | (1L << PERCFLOWBELOW) | (1L << PERTARGET) | (1L << PERTHIS) | (1L << PERTYPEWITHIN))) != 0)) {
				{
				setState(558); perClause();
				}
			}

			setState(561); aspectBody();
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

	public static class AdviceContext extends ParserRuleContext {
		public AdviceSpecContext adviceSpec() {
			return getRuleContext(AdviceSpecContext.class,0);
		}
		public MethodBodyContext methodBody() {
			return getRuleContext(MethodBodyContext.class,0);
		}
		public TypeListContext typeList() {
			return getRuleContext(TypeListContext.class,0);
		}
		public PointcutExpressionContext pointcutExpression() {
			return getRuleContext(PointcutExpressionContext.class,0);
		}
		public AdviceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_advice; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterAdvice(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitAdvice(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitAdvice(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AdviceContext advice() throws RecognitionException {
		AdviceContext _localctx = new AdviceContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_advice);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(564);
			_la = _input.LA(1);
			if (_la==STRICTFP) {
				{
				setState(563); match(STRICTFP);
				}
			}

			setState(566); adviceSpec();
			setState(569);
			_la = _input.LA(1);
			if (_la==THROWS) {
				{
				setState(567); match(THROWS);
				setState(568); typeList();
				}
			}

			setState(571); match(COLON);
			setState(572); pointcutExpression(0);
			setState(573); methodBody();
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

	public static class AdviceSpecContext extends ParserRuleContext {
		public FormalParameterContext formalParameter() {
			return getRuleContext(FormalParameterContext.class,0);
		}
		public FormalParametersContext formalParameters() {
			return getRuleContext(FormalParametersContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public AdviceSpecContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_adviceSpec; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterAdviceSpec(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitAdviceSpec(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitAdviceSpec(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AdviceSpecContext adviceSpec() throws RecognitionException {
		AdviceSpecContext _localctx = new AdviceSpecContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_adviceSpec);
		int _la;
		try {
			setState(605);
			switch ( getInterpreter().adaptivePredict(_input,31,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(575); match(BEFORE);
				setState(576); formalParameters();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(577); match(AFTER);
				setState(578); formalParameters();
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(579); match(AFTER);
				setState(580); formalParameters();
				setState(581); match(RETURNING);
				setState(587);
				_la = _input.LA(1);
				if (_la==LPAREN) {
					{
					setState(582); match(LPAREN);
					setState(584);
					_la = _input.LA(1);
					if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ARGS) | (1L << AFTER) | (1L << AROUND) | (1L << ASPECT) | (1L << BEFORE) | (1L << CALL) | (1L << CFLOW) | (1L << CFLOWBELOW) | (1L << DECLARE) | (1L << ERROR) | (1L << EXECUTION) | (1L << GET) | (1L << HANDLER) | (1L << INITIALIZATION) | (1L << ISSINGLETON) | (1L << PARENTS) | (1L << PERCFLOW) | (1L << PERCFLOWBELOW) | (1L << PERTARGET) | (1L << PERTHIS) | (1L << PERTYPEWITHIN) | (1L << POINTCUT) | (1L << PRECEDENCE) | (1L << PREINITIALIZATION) | (1L << PRIVILEGED) | (1L << RETURNING) | (1L << SET) | (1L << SOFT) | (1L << STATICINITIALIZATION) | (1L << TARGET) | (1L << THROWING) | (1L << WARNING) | (1L << WITHIN) | (1L << WITHINCODE) | (1L << AT) | (1L << BOOLEAN) | (1L << BYTE))) != 0) || ((((_la - 66)) & ~0x3f) == 0 && ((1L << (_la - 66)) & ((1L << (CHAR - 66)) | (1L << (DOUBLE - 66)) | (1L << (FINAL - 66)) | (1L << (FLOAT - 66)) | (1L << (INT - 66)) | (1L << (LONG - 66)) | (1L << (SHORT - 66)))) != 0) || _la==Identifier) {
						{
						setState(583); formalParameter();
						}
					}

					setState(586); match(RPAREN);
					}
				}

				}
				break;

			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(589); match(AFTER);
				setState(590); formalParameters();
				setState(591); match(THROWING);
				setState(597);
				_la = _input.LA(1);
				if (_la==LPAREN) {
					{
					setState(592); match(LPAREN);
					setState(594);
					_la = _input.LA(1);
					if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ARGS) | (1L << AFTER) | (1L << AROUND) | (1L << ASPECT) | (1L << BEFORE) | (1L << CALL) | (1L << CFLOW) | (1L << CFLOWBELOW) | (1L << DECLARE) | (1L << ERROR) | (1L << EXECUTION) | (1L << GET) | (1L << HANDLER) | (1L << INITIALIZATION) | (1L << ISSINGLETON) | (1L << PARENTS) | (1L << PERCFLOW) | (1L << PERCFLOWBELOW) | (1L << PERTARGET) | (1L << PERTHIS) | (1L << PERTYPEWITHIN) | (1L << POINTCUT) | (1L << PRECEDENCE) | (1L << PREINITIALIZATION) | (1L << PRIVILEGED) | (1L << RETURNING) | (1L << SET) | (1L << SOFT) | (1L << STATICINITIALIZATION) | (1L << TARGET) | (1L << THROWING) | (1L << WARNING) | (1L << WITHIN) | (1L << WITHINCODE) | (1L << AT) | (1L << BOOLEAN) | (1L << BYTE))) != 0) || ((((_la - 66)) & ~0x3f) == 0 && ((1L << (_la - 66)) & ((1L << (CHAR - 66)) | (1L << (DOUBLE - 66)) | (1L << (FINAL - 66)) | (1L << (FLOAT - 66)) | (1L << (INT - 66)) | (1L << (LONG - 66)) | (1L << (SHORT - 66)))) != 0) || _la==Identifier) {
						{
						setState(593); formalParameter();
						}
					}

					setState(596); match(RPAREN);
					}
				}

				}
				break;

			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(601);
				switch (_input.LA(1)) {
				case ARGS:
				case AFTER:
				case AROUND:
				case ASPECT:
				case BEFORE:
				case CALL:
				case CFLOW:
				case CFLOWBELOW:
				case DECLARE:
				case ERROR:
				case EXECUTION:
				case GET:
				case HANDLER:
				case INITIALIZATION:
				case ISSINGLETON:
				case PARENTS:
				case PERCFLOW:
				case PERCFLOWBELOW:
				case PERTARGET:
				case PERTHIS:
				case PERTYPEWITHIN:
				case POINTCUT:
				case PRECEDENCE:
				case PREINITIALIZATION:
				case PRIVILEGED:
				case RETURNING:
				case SET:
				case SOFT:
				case STATICINITIALIZATION:
				case TARGET:
				case THROWING:
				case WARNING:
				case WITHIN:
				case WITHINCODE:
				case BOOLEAN:
				case BYTE:
				case CHAR:
				case DOUBLE:
				case FLOAT:
				case INT:
				case LONG:
				case SHORT:
				case Identifier:
					{
					setState(599); type();
					}
					break;
				case VOID:
					{
					setState(600); match(VOID);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(603); match(AROUND);
				setState(604); formalParameters();
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

	public static class PerClauseContext extends ParserRuleContext {
		public PointcutExpressionContext pointcutExpression() {
			return getRuleContext(PointcutExpressionContext.class,0);
		}
		public TypePatternContext typePattern() {
			return getRuleContext(TypePatternContext.class,0);
		}
		public PerClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_perClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterPerClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitPerClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitPerClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PerClauseContext perClause() throws RecognitionException {
		PerClauseContext _localctx = new PerClauseContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_perClause);
		try {
			setState(635);
			switch (_input.LA(1)) {
			case PERTARGET:
				enterOuterAlt(_localctx, 1);
				{
				setState(607); match(PERTARGET);
				setState(608); match(LPAREN);
				setState(609); pointcutExpression(0);
				setState(610); match(RPAREN);
				}
				break;
			case PERTHIS:
				enterOuterAlt(_localctx, 2);
				{
				setState(612); match(PERTHIS);
				setState(613); match(LPAREN);
				setState(614); pointcutExpression(0);
				setState(615); match(RPAREN);
				}
				break;
			case PERCFLOW:
				enterOuterAlt(_localctx, 3);
				{
				setState(617); match(PERCFLOW);
				setState(618); match(LPAREN);
				setState(619); pointcutExpression(0);
				setState(620); match(RPAREN);
				}
				break;
			case PERCFLOWBELOW:
				enterOuterAlt(_localctx, 4);
				{
				setState(622); match(PERCFLOWBELOW);
				setState(623); match(LPAREN);
				setState(624); pointcutExpression(0);
				setState(625); match(RPAREN);
				}
				break;
			case PERTYPEWITHIN:
				enterOuterAlt(_localctx, 5);
				{
				setState(627); match(PERTYPEWITHIN);
				setState(628); match(LPAREN);
				setState(629); typePattern(0);
				setState(630); match(RPAREN);
				}
				break;
			case ISSINGLETON:
				enterOuterAlt(_localctx, 6);
				{
				setState(632); match(ISSINGLETON);
				setState(633); match(LPAREN);
				setState(634); match(RPAREN);
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

	public static class PointcutDeclarationContext extends ParserRuleContext {
		public List<ModifierContext> modifier() {
			return getRuleContexts(ModifierContext.class);
		}
		public FormalParametersContext formalParameters() {
			return getRuleContext(FormalParametersContext.class,0);
		}
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public ModifierContext modifier(int i) {
			return getRuleContext(ModifierContext.class,i);
		}
		public PointcutExpressionContext pointcutExpression() {
			return getRuleContext(PointcutExpressionContext.class,0);
		}
		public PointcutDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pointcutDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterPointcutDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitPointcutDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitPointcutDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PointcutDeclarationContext pointcutDeclaration() throws RecognitionException {
		PointcutDeclarationContext _localctx = new PointcutDeclarationContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_pointcutDeclaration);
		int _la;
		try {
			setState(662);
			switch ( getInterpreter().adaptivePredict(_input,35,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(637); match(ABSTRACT);
				setState(641);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (ABSTRACT - 58)) | (1L << (FINAL - 58)) | (1L << (NATIVE - 58)) | (1L << (PRIVATE - 58)) | (1L << (PROTECTED - 58)) | (1L << (PUBLIC - 58)) | (1L << (STATIC - 58)) | (1L << (STRICTFP - 58)) | (1L << (SYNCHRONIZED - 58)) | (1L << (TRANSIENT - 58)) | (1L << (VOLATILE - 58)))) != 0)) {
					{
					{
					setState(638); modifier();
					}
					}
					setState(643);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(644); match(POINTCUT);
				setState(645); id();
				setState(646); formalParameters();
				setState(647); match(SEMI);
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(652);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (ABSTRACT - 58)) | (1L << (FINAL - 58)) | (1L << (NATIVE - 58)) | (1L << (PRIVATE - 58)) | (1L << (PROTECTED - 58)) | (1L << (PUBLIC - 58)) | (1L << (STATIC - 58)) | (1L << (STRICTFP - 58)) | (1L << (SYNCHRONIZED - 58)) | (1L << (TRANSIENT - 58)) | (1L << (VOLATILE - 58)))) != 0)) {
					{
					{
					setState(649); modifier();
					}
					}
					setState(654);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(655); match(POINTCUT);
				setState(656); id();
				setState(657); formalParameters();
				setState(658); match(COLON);
				setState(659); pointcutExpression(0);
				setState(660); match(SEMI);
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

	public static class PointcutExpressionContext extends ParserRuleContext {
		public ReferencePointcutContext referencePointcut() {
			return getRuleContext(ReferencePointcutContext.class,0);
		}
		public PointcutPrimitiveContext pointcutPrimitive() {
			return getRuleContext(PointcutPrimitiveContext.class,0);
		}
		public List<PointcutExpressionContext> pointcutExpression() {
			return getRuleContexts(PointcutExpressionContext.class);
		}
		public PointcutExpressionContext pointcutExpression(int i) {
			return getRuleContext(PointcutExpressionContext.class,i);
		}
		public PointcutExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pointcutExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterPointcutExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitPointcutExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitPointcutExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PointcutExpressionContext pointcutExpression() throws RecognitionException {
		return pointcutExpression(0);
	}

	private PointcutExpressionContext pointcutExpression(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		PointcutExpressionContext _localctx = new PointcutExpressionContext(_ctx, _parentState);
		PointcutExpressionContext _prevctx = _localctx;
		int _startState = 26;
		enterRecursionRule(_localctx, 26, RULE_pointcutExpression, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(675);
			switch ( getInterpreter().adaptivePredict(_input,37,_ctx) ) {
			case 1:
				{
				setState(665); match(BANG);
				setState(666); pointcutExpression(4);
				}
				break;

			case 2:
				{
				setState(669);
				switch ( getInterpreter().adaptivePredict(_input,36,_ctx) ) {
				case 1:
					{
					setState(667); pointcutPrimitive();
					}
					break;

				case 2:
					{
					setState(668); referencePointcut();
					}
					break;
				}
				}
				break;

			case 3:
				{
				setState(671); match(LPAREN);
				setState(672); pointcutExpression(0);
				setState(673); match(RPAREN);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(685);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,39,_ctx);
			while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(683);
					switch ( getInterpreter().adaptivePredict(_input,38,_ctx) ) {
					case 1:
						{
						_localctx = new PointcutExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_pointcutExpression);
						setState(677);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(678); match(AND);
						setState(679); pointcutExpression(3);
						}
						break;

					case 2:
						{
						_localctx = new PointcutExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_pointcutExpression);
						setState(680);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(681); match(OR);
						setState(682); pointcutExpression(2);
						}
						break;
					}
					} 
				}
				setState(687);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,39,_ctx);
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

	public static class PointcutPrimitiveContext extends ParserRuleContext {
		public PointcutPrimitiveContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pointcutPrimitive; }
	 
		public PointcutPrimitiveContext() { }
		public void copyFrom(PointcutPrimitiveContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class InitializationPointcutContext extends PointcutPrimitiveContext {
		public ConstructorPatternContext constructorPattern() {
			return getRuleContext(ConstructorPatternContext.class,0);
		}
		public InitializationPointcutContext(PointcutPrimitiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterInitializationPointcut(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitInitializationPointcut(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitInitializationPointcut(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class StaticInitializationPointcutContext extends PointcutPrimitiveContext {
		public OptionalParensTypePatternContext optionalParensTypePattern() {
			return getRuleContext(OptionalParensTypePatternContext.class,0);
		}
		public StaticInitializationPointcutContext(PointcutPrimitiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterStaticInitializationPointcut(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitStaticInitializationPointcut(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitStaticInitializationPointcut(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CFlowPointcutContext extends PointcutPrimitiveContext {
		public PointcutExpressionContext pointcutExpression() {
			return getRuleContext(PointcutExpressionContext.class,0);
		}
		public CFlowPointcutContext(PointcutPrimitiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterCFlowPointcut(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitCFlowPointcut(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitCFlowPointcut(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AnnotationArgsPointcutContext extends PointcutPrimitiveContext {
		public AnnotationsOrIdentifiersPatternContext annotationsOrIdentifiersPattern() {
			return getRuleContext(AnnotationsOrIdentifiersPatternContext.class,0);
		}
		public AnnotationArgsPointcutContext(PointcutPrimitiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterAnnotationArgsPointcut(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitAnnotationArgsPointcut(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitAnnotationArgsPointcut(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class GetPointcutContext extends PointcutPrimitiveContext {
		public FieldPatternContext fieldPattern() {
			return getRuleContext(FieldPatternContext.class,0);
		}
		public GetPointcutContext(PointcutPrimitiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterGetPointcut(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitGetPointcut(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitGetPointcut(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ExecutionPointcutContext extends PointcutPrimitiveContext {
		public MethodOrConstructorPatternContext methodOrConstructorPattern() {
			return getRuleContext(MethodOrConstructorPatternContext.class,0);
		}
		public ExecutionPointcutContext(PointcutPrimitiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterExecutionPointcut(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitExecutionPointcut(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitExecutionPointcut(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class TargetPointcutContext extends PointcutPrimitiveContext {
		public TypeOrIdentifierContext typeOrIdentifier() {
			return getRuleContext(TypeOrIdentifierContext.class,0);
		}
		public TargetPointcutContext(PointcutPrimitiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterTargetPointcut(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitTargetPointcut(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitTargetPointcut(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AdviceExecutionPointcutContext extends PointcutPrimitiveContext {
		public AdviceExecutionPointcutContext(PointcutPrimitiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterAdviceExecutionPointcut(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitAdviceExecutionPointcut(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitAdviceExecutionPointcut(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AnnotationPointcutContext extends PointcutPrimitiveContext {
		public AnnotationOrIdentiferContext annotationOrIdentifer() {
			return getRuleContext(AnnotationOrIdentiferContext.class,0);
		}
		public AnnotationPointcutContext(PointcutPrimitiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterAnnotationPointcut(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitAnnotationPointcut(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitAnnotationPointcut(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AnnotationTargetPointcutContext extends PointcutPrimitiveContext {
		public AnnotationOrIdentiferContext annotationOrIdentifer() {
			return getRuleContext(AnnotationOrIdentiferContext.class,0);
		}
		public AnnotationTargetPointcutContext(PointcutPrimitiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterAnnotationTargetPointcut(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitAnnotationTargetPointcut(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitAnnotationTargetPointcut(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AnnotationThisPointcutContext extends PointcutPrimitiveContext {
		public AnnotationOrIdentiferContext annotationOrIdentifer() {
			return getRuleContext(AnnotationOrIdentiferContext.class,0);
		}
		public AnnotationThisPointcutContext(PointcutPrimitiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterAnnotationThisPointcut(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitAnnotationThisPointcut(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitAnnotationThisPointcut(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class SetPointcutContext extends PointcutPrimitiveContext {
		public FieldPatternContext fieldPattern() {
			return getRuleContext(FieldPatternContext.class,0);
		}
		public SetPointcutContext(PointcutPrimitiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterSetPointcut(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitSetPointcut(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitSetPointcut(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class WithinCodePointcutContext extends PointcutPrimitiveContext {
		public MethodOrConstructorPatternContext methodOrConstructorPattern() {
			return getRuleContext(MethodOrConstructorPatternContext.class,0);
		}
		public WithinCodePointcutContext(PointcutPrimitiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterWithinCodePointcut(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitWithinCodePointcut(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitWithinCodePointcut(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ArgsPointcutContext extends PointcutPrimitiveContext {
		public ArgsPatternListContext argsPatternList() {
			return getRuleContext(ArgsPatternListContext.class,0);
		}
		public ArgsPointcutContext(PointcutPrimitiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterArgsPointcut(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitArgsPointcut(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitArgsPointcut(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AnnotationWithinPointcutContext extends PointcutPrimitiveContext {
		public AnnotationOrIdentiferContext annotationOrIdentifer() {
			return getRuleContext(AnnotationOrIdentiferContext.class,0);
		}
		public AnnotationWithinPointcutContext(PointcutPrimitiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterAnnotationWithinPointcut(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitAnnotationWithinPointcut(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitAnnotationWithinPointcut(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CallPointcutContext extends PointcutPrimitiveContext {
		public MethodOrConstructorPatternContext methodOrConstructorPattern() {
			return getRuleContext(MethodOrConstructorPatternContext.class,0);
		}
		public CallPointcutContext(PointcutPrimitiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterCallPointcut(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitCallPointcut(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitCallPointcut(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class WithinPointcutContext extends PointcutPrimitiveContext {
		public OptionalParensTypePatternContext optionalParensTypePattern() {
			return getRuleContext(OptionalParensTypePatternContext.class,0);
		}
		public WithinPointcutContext(PointcutPrimitiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterWithinPointcut(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitWithinPointcut(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitWithinPointcut(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AnnotationWithinCodePointcutContext extends PointcutPrimitiveContext {
		public AnnotationOrIdentiferContext annotationOrIdentifer() {
			return getRuleContext(AnnotationOrIdentiferContext.class,0);
		}
		public AnnotationWithinCodePointcutContext(PointcutPrimitiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterAnnotationWithinCodePointcut(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitAnnotationWithinCodePointcut(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitAnnotationWithinCodePointcut(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class IfPointcutContext extends PointcutPrimitiveContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public IfPointcutContext(PointcutPrimitiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterIfPointcut(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitIfPointcut(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitIfPointcut(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PreInitializationPointcutContext extends PointcutPrimitiveContext {
		public ConstructorPatternContext constructorPattern() {
			return getRuleContext(ConstructorPatternContext.class,0);
		}
		public PreInitializationPointcutContext(PointcutPrimitiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterPreInitializationPointcut(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitPreInitializationPointcut(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitPreInitializationPointcut(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CFlowBelowPointcutContext extends PointcutPrimitiveContext {
		public PointcutExpressionContext pointcutExpression() {
			return getRuleContext(PointcutExpressionContext.class,0);
		}
		public CFlowBelowPointcutContext(PointcutPrimitiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterCFlowBelowPointcut(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitCFlowBelowPointcut(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitCFlowBelowPointcut(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ThisPointcutPointcutContext extends PointcutPrimitiveContext {
		public TypeOrIdentifierContext typeOrIdentifier() {
			return getRuleContext(TypeOrIdentifierContext.class,0);
		}
		public ThisPointcutPointcutContext(PointcutPrimitiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterThisPointcutPointcut(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitThisPointcutPointcut(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitThisPointcutPointcut(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class HandlerPointcutContext extends PointcutPrimitiveContext {
		public OptionalParensTypePatternContext optionalParensTypePattern() {
			return getRuleContext(OptionalParensTypePatternContext.class,0);
		}
		public HandlerPointcutContext(PointcutPrimitiveContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterHandlerPointcut(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitHandlerPointcut(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitHandlerPointcut(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PointcutPrimitiveContext pointcutPrimitive() throws RecognitionException {
		PointcutPrimitiveContext _localctx = new PointcutPrimitiveContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_pointcutPrimitive);
		int _la;
		try {
			setState(808);
			switch ( getInterpreter().adaptivePredict(_input,41,_ctx) ) {
			case 1:
				_localctx = new CallPointcutContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(688); match(CALL);
				setState(689); match(LPAREN);
				setState(690); methodOrConstructorPattern();
				setState(691); match(RPAREN);
				}
				break;

			case 2:
				_localctx = new ExecutionPointcutContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(693); match(EXECUTION);
				setState(694); match(LPAREN);
				setState(695); methodOrConstructorPattern();
				setState(696); match(RPAREN);
				}
				break;

			case 3:
				_localctx = new InitializationPointcutContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(698); match(INITIALIZATION);
				setState(699); match(LPAREN);
				setState(700); constructorPattern();
				setState(701); match(RPAREN);
				}
				break;

			case 4:
				_localctx = new PreInitializationPointcutContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(703); match(PREINITIALIZATION);
				setState(704); match(LPAREN);
				setState(705); constructorPattern();
				setState(706); match(RPAREN);
				}
				break;

			case 5:
				_localctx = new StaticInitializationPointcutContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(708); match(STATICINITIALIZATION);
				setState(709); match(LPAREN);
				setState(710); optionalParensTypePattern();
				setState(711); match(RPAREN);
				}
				break;

			case 6:
				_localctx = new GetPointcutContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(713); match(GET);
				setState(714); match(LPAREN);
				setState(715); fieldPattern();
				setState(716); match(RPAREN);
				}
				break;

			case 7:
				_localctx = new SetPointcutContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(718); match(SET);
				setState(719); match(LPAREN);
				setState(720); fieldPattern();
				setState(721); match(RPAREN);
				}
				break;

			case 8:
				_localctx = new HandlerPointcutContext(_localctx);
				enterOuterAlt(_localctx, 8);
				{
				setState(723); match(HANDLER);
				setState(724); match(LPAREN);
				setState(725); optionalParensTypePattern();
				setState(726); match(RPAREN);
				}
				break;

			case 9:
				_localctx = new AdviceExecutionPointcutContext(_localctx);
				enterOuterAlt(_localctx, 9);
				{
				setState(728); match(ADVICEEXECUTION);
				setState(729); match(LPAREN);
				setState(730); match(RPAREN);
				}
				break;

			case 10:
				_localctx = new WithinPointcutContext(_localctx);
				enterOuterAlt(_localctx, 10);
				{
				setState(731); match(WITHIN);
				setState(732); match(LPAREN);
				setState(733); optionalParensTypePattern();
				setState(734); match(RPAREN);
				}
				break;

			case 11:
				_localctx = new WithinCodePointcutContext(_localctx);
				enterOuterAlt(_localctx, 11);
				{
				setState(736); match(WITHINCODE);
				setState(737); match(LPAREN);
				setState(738); methodOrConstructorPattern();
				setState(739); match(RPAREN);
				}
				break;

			case 12:
				_localctx = new CFlowPointcutContext(_localctx);
				enterOuterAlt(_localctx, 12);
				{
				setState(741); match(CFLOW);
				setState(742); match(LPAREN);
				setState(743); pointcutExpression(0);
				setState(744); match(RPAREN);
				}
				break;

			case 13:
				_localctx = new CFlowBelowPointcutContext(_localctx);
				enterOuterAlt(_localctx, 13);
				{
				setState(746); match(CFLOWBELOW);
				setState(747); match(LPAREN);
				setState(748); pointcutExpression(0);
				setState(749); match(RPAREN);
				}
				break;

			case 14:
				_localctx = new IfPointcutContext(_localctx);
				enterOuterAlt(_localctx, 14);
				{
				setState(751); match(IF);
				setState(752); match(LPAREN);
				setState(754);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ARGS) | (1L << AFTER) | (1L << AROUND) | (1L << ASPECT) | (1L << BEFORE) | (1L << CALL) | (1L << CFLOW) | (1L << CFLOWBELOW) | (1L << DECLARE) | (1L << ERROR) | (1L << EXECUTION) | (1L << GET) | (1L << HANDLER) | (1L << INITIALIZATION) | (1L << ISSINGLETON) | (1L << PARENTS) | (1L << PERCFLOW) | (1L << PERCFLOWBELOW) | (1L << PERTARGET) | (1L << PERTHIS) | (1L << PERTYPEWITHIN) | (1L << POINTCUT) | (1L << PRECEDENCE) | (1L << PREINITIALIZATION) | (1L << PRIVILEGED) | (1L << RETURNING) | (1L << SET) | (1L << SOFT) | (1L << STATICINITIALIZATION) | (1L << TARGET) | (1L << THROWING) | (1L << WARNING) | (1L << WITHIN) | (1L << WITHINCODE) | (1L << BOOLEAN) | (1L << BYTE))) != 0) || ((((_la - 66)) & ~0x3f) == 0 && ((1L << (_la - 66)) & ((1L << (CHAR - 66)) | (1L << (DOUBLE - 66)) | (1L << (FLOAT - 66)) | (1L << (INT - 66)) | (1L << (LONG - 66)) | (1L << (NEW - 66)) | (1L << (SHORT - 66)) | (1L << (SUPER - 66)) | (1L << (THIS - 66)) | (1L << (VOID - 66)) | (1L << (IntegerLiteral - 66)) | (1L << (FloatingPointLiteral - 66)) | (1L << (BooleanLiteral - 66)) | (1L << (CharacterLiteral - 66)) | (1L << (StringLiteral - 66)) | (1L << (NullLiteral - 66)) | (1L << (LPAREN - 66)) | (1L << (LT - 66)) | (1L << (BANG - 66)) | (1L << (TILDE - 66)))) != 0) || ((((_la - 137)) & ~0x3f) == 0 && ((1L << (_la - 137)) & ((1L << (INC - 137)) | (1L << (DEC - 137)) | (1L << (ADD - 137)) | (1L << (SUB - 137)) | (1L << (Identifier - 137)))) != 0)) {
					{
					setState(753); expression(0);
					}
				}

				setState(756); match(RPAREN);
				}
				break;

			case 15:
				_localctx = new ThisPointcutPointcutContext(_localctx);
				enterOuterAlt(_localctx, 15);
				{
				setState(757); match(THIS);
				setState(758); match(LPAREN);
				setState(759); typeOrIdentifier();
				setState(760); match(RPAREN);
				}
				break;

			case 16:
				_localctx = new TargetPointcutContext(_localctx);
				enterOuterAlt(_localctx, 16);
				{
				setState(762); match(TARGET);
				setState(763); match(LPAREN);
				setState(764); typeOrIdentifier();
				setState(765); match(RPAREN);
				}
				break;

			case 17:
				_localctx = new ArgsPointcutContext(_localctx);
				enterOuterAlt(_localctx, 17);
				{
				setState(767); match(ARGS);
				setState(768); match(LPAREN);
				setState(769); argsPatternList();
				setState(770); match(RPAREN);
				}
				break;

			case 18:
				_localctx = new AnnotationThisPointcutContext(_localctx);
				enterOuterAlt(_localctx, 18);
				{
				setState(772); match(AT);
				setState(773); match(THIS);
				setState(774); match(LPAREN);
				setState(775); annotationOrIdentifer();
				setState(776); match(RPAREN);
				}
				break;

			case 19:
				_localctx = new AnnotationTargetPointcutContext(_localctx);
				enterOuterAlt(_localctx, 19);
				{
				setState(778); match(AT);
				setState(779); match(TARGET);
				setState(780); match(LPAREN);
				setState(781); annotationOrIdentifer();
				setState(782); match(RPAREN);
				}
				break;

			case 20:
				_localctx = new AnnotationArgsPointcutContext(_localctx);
				enterOuterAlt(_localctx, 20);
				{
				setState(784); match(AT);
				setState(785); match(ARGS);
				setState(786); match(LPAREN);
				setState(787); annotationsOrIdentifiersPattern();
				setState(788); match(RPAREN);
				}
				break;

			case 21:
				_localctx = new AnnotationWithinPointcutContext(_localctx);
				enterOuterAlt(_localctx, 21);
				{
				setState(790); match(AT);
				setState(791); match(WITHIN);
				setState(792); match(LPAREN);
				setState(793); annotationOrIdentifer();
				setState(794); match(RPAREN);
				}
				break;

			case 22:
				_localctx = new AnnotationWithinCodePointcutContext(_localctx);
				enterOuterAlt(_localctx, 22);
				{
				setState(796); match(AT);
				setState(797); match(WITHINCODE);
				setState(798); match(LPAREN);
				setState(799); annotationOrIdentifer();
				setState(800); match(RPAREN);
				}
				break;

			case 23:
				_localctx = new AnnotationPointcutContext(_localctx);
				enterOuterAlt(_localctx, 23);
				{
				setState(802); match(AT);
				setState(803); match(ANNOTATION);
				setState(804); match(LPAREN);
				setState(805); annotationOrIdentifer();
				setState(806); match(RPAREN);
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

	public static class ReferencePointcutContext extends ParserRuleContext {
		public FormalParametersPatternContext formalParametersPattern() {
			return getRuleContext(FormalParametersPatternContext.class,0);
		}
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public TypePatternContext typePattern() {
			return getRuleContext(TypePatternContext.class,0);
		}
		public ReferencePointcutContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_referencePointcut; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterReferencePointcut(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitReferencePointcut(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitReferencePointcut(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ReferencePointcutContext referencePointcut() throws RecognitionException {
		ReferencePointcutContext _localctx = new ReferencePointcutContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_referencePointcut);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(813);
			switch ( getInterpreter().adaptivePredict(_input,42,_ctx) ) {
			case 1:
				{
				setState(810); typePattern(0);
				setState(811); match(DOT);
				}
				break;
			}
			setState(815); id();
			setState(816); formalParametersPattern();
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

	public static class InterTypeMemberDeclarationContext extends ParserRuleContext {
		public List<ModifierContext> modifier() {
			return getRuleContexts(ModifierContext.class);
		}
		public MethodBodyContext methodBody() {
			return getRuleContext(MethodBodyContext.class,0);
		}
		public TypeContext type(int i) {
			return getRuleContext(TypeContext.class,i);
		}
		public TypeListContext typeList() {
			return getRuleContext(TypeListContext.class,0);
		}
		public FormalParametersContext formalParameters() {
			return getRuleContext(FormalParametersContext.class,0);
		}
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public ModifierContext modifier(int i) {
			return getRuleContext(ModifierContext.class,i);
		}
		public List<TypeContext> type() {
			return getRuleContexts(TypeContext.class);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public InterTypeMemberDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interTypeMemberDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterInterTypeMemberDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitInterTypeMemberDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitInterTypeMemberDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InterTypeMemberDeclarationContext interTypeMemberDeclaration() throws RecognitionException {
		InterTypeMemberDeclarationContext _localctx = new InterTypeMemberDeclarationContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_interTypeMemberDeclaration);
		int _la;
		try {
			int _alt;
			setState(900);
			switch ( getInterpreter().adaptivePredict(_input,55,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(821);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (ABSTRACT - 58)) | (1L << (FINAL - 58)) | (1L << (NATIVE - 58)) | (1L << (PRIVATE - 58)) | (1L << (PROTECTED - 58)) | (1L << (PUBLIC - 58)) | (1L << (STATIC - 58)) | (1L << (STRICTFP - 58)) | (1L << (SYNCHRONIZED - 58)) | (1L << (TRANSIENT - 58)) | (1L << (VOLATILE - 58)))) != 0)) {
					{
					{
					setState(818); modifier();
					}
					}
					setState(823);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(826);
				switch (_input.LA(1)) {
				case ARGS:
				case AFTER:
				case AROUND:
				case ASPECT:
				case BEFORE:
				case CALL:
				case CFLOW:
				case CFLOWBELOW:
				case DECLARE:
				case ERROR:
				case EXECUTION:
				case GET:
				case HANDLER:
				case INITIALIZATION:
				case ISSINGLETON:
				case PARENTS:
				case PERCFLOW:
				case PERCFLOWBELOW:
				case PERTARGET:
				case PERTHIS:
				case PERTYPEWITHIN:
				case POINTCUT:
				case PRECEDENCE:
				case PREINITIALIZATION:
				case PRIVILEGED:
				case RETURNING:
				case SET:
				case SOFT:
				case STATICINITIALIZATION:
				case TARGET:
				case THROWING:
				case WARNING:
				case WITHIN:
				case WITHINCODE:
				case BOOLEAN:
				case BYTE:
				case CHAR:
				case DOUBLE:
				case FLOAT:
				case INT:
				case LONG:
				case SHORT:
				case Identifier:
					{
					setState(824); type();
					}
					break;
				case VOID:
					{
					setState(825); match(VOID);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(828); type();
				setState(829); match(DOT);
				setState(830); id();
				setState(831); formalParameters();
				setState(834);
				_la = _input.LA(1);
				if (_la==THROWS) {
					{
					setState(832); match(THROWS);
					setState(833); typeList();
					}
				}

				setState(836); methodBody();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(841);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,46,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(838); modifier();
						}
						} 
					}
					setState(843);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,46,_ctx);
				}
				setState(844); match(ABSTRACT);
				setState(848);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (ABSTRACT - 58)) | (1L << (FINAL - 58)) | (1L << (NATIVE - 58)) | (1L << (PRIVATE - 58)) | (1L << (PROTECTED - 58)) | (1L << (PUBLIC - 58)) | (1L << (STATIC - 58)) | (1L << (STRICTFP - 58)) | (1L << (SYNCHRONIZED - 58)) | (1L << (TRANSIENT - 58)) | (1L << (VOLATILE - 58)))) != 0)) {
					{
					{
					setState(845); modifier();
					}
					}
					setState(850);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(853);
				switch (_input.LA(1)) {
				case ARGS:
				case AFTER:
				case AROUND:
				case ASPECT:
				case BEFORE:
				case CALL:
				case CFLOW:
				case CFLOWBELOW:
				case DECLARE:
				case ERROR:
				case EXECUTION:
				case GET:
				case HANDLER:
				case INITIALIZATION:
				case ISSINGLETON:
				case PARENTS:
				case PERCFLOW:
				case PERCFLOWBELOW:
				case PERTARGET:
				case PERTHIS:
				case PERTYPEWITHIN:
				case POINTCUT:
				case PRECEDENCE:
				case PREINITIALIZATION:
				case PRIVILEGED:
				case RETURNING:
				case SET:
				case SOFT:
				case STATICINITIALIZATION:
				case TARGET:
				case THROWING:
				case WARNING:
				case WITHIN:
				case WITHINCODE:
				case BOOLEAN:
				case BYTE:
				case CHAR:
				case DOUBLE:
				case FLOAT:
				case INT:
				case LONG:
				case SHORT:
				case Identifier:
					{
					setState(851); type();
					}
					break;
				case VOID:
					{
					setState(852); match(VOID);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(855); type();
				setState(856); match(DOT);
				setState(857); id();
				setState(858); formalParameters();
				setState(861);
				_la = _input.LA(1);
				if (_la==THROWS) {
					{
					setState(859); match(THROWS);
					setState(860); typeList();
					}
				}

				setState(863); match(SEMI);
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(868);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (ABSTRACT - 58)) | (1L << (FINAL - 58)) | (1L << (NATIVE - 58)) | (1L << (PRIVATE - 58)) | (1L << (PROTECTED - 58)) | (1L << (PUBLIC - 58)) | (1L << (STATIC - 58)) | (1L << (STRICTFP - 58)) | (1L << (SYNCHRONIZED - 58)) | (1L << (TRANSIENT - 58)) | (1L << (VOLATILE - 58)))) != 0)) {
					{
					{
					setState(865); modifier();
					}
					}
					setState(870);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(871); type();
				setState(872); match(DOT);
				setState(873); match(NEW);
				setState(874); formalParameters();
				setState(877);
				_la = _input.LA(1);
				if (_la==THROWS) {
					{
					setState(875); match(THROWS);
					setState(876); typeList();
					}
				}

				setState(879); methodBody();
				}
				break;

			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(884);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (((((_la - 58)) & ~0x3f) == 0 && ((1L << (_la - 58)) & ((1L << (AT - 58)) | (1L << (ABSTRACT - 58)) | (1L << (FINAL - 58)) | (1L << (NATIVE - 58)) | (1L << (PRIVATE - 58)) | (1L << (PROTECTED - 58)) | (1L << (PUBLIC - 58)) | (1L << (STATIC - 58)) | (1L << (STRICTFP - 58)) | (1L << (SYNCHRONIZED - 58)) | (1L << (TRANSIENT - 58)) | (1L << (VOLATILE - 58)))) != 0)) {
					{
					{
					setState(881); modifier();
					}
					}
					setState(886);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(889);
				switch (_input.LA(1)) {
				case ARGS:
				case AFTER:
				case AROUND:
				case ASPECT:
				case BEFORE:
				case CALL:
				case CFLOW:
				case CFLOWBELOW:
				case DECLARE:
				case ERROR:
				case EXECUTION:
				case GET:
				case HANDLER:
				case INITIALIZATION:
				case ISSINGLETON:
				case PARENTS:
				case PERCFLOW:
				case PERCFLOWBELOW:
				case PERTARGET:
				case PERTHIS:
				case PERTYPEWITHIN:
				case POINTCUT:
				case PRECEDENCE:
				case PREINITIALIZATION:
				case PRIVILEGED:
				case RETURNING:
				case SET:
				case SOFT:
				case STATICINITIALIZATION:
				case TARGET:
				case THROWING:
				case WARNING:
				case WITHIN:
				case WITHINCODE:
				case BOOLEAN:
				case BYTE:
				case CHAR:
				case DOUBLE:
				case FLOAT:
				case INT:
				case LONG:
				case SHORT:
				case Identifier:
					{
					setState(887); type();
					}
					break;
				case VOID:
					{
					setState(888); match(VOID);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(891); type();
				setState(892); match(DOT);
				setState(893); id();
				setState(896);
				_la = _input.LA(1);
				if (_la==ASSIGN) {
					{
					setState(894); match(ASSIGN);
					setState(895); expression(0);
					}
				}

				setState(898); match(SEMI);
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

	public static class InterTypeDeclarationContext extends ParserRuleContext {
		public ConstructorPatternContext constructorPattern() {
			return getRuleContext(ConstructorPatternContext.class,0);
		}
		public AnnotationContext annotation() {
			return getRuleContext(AnnotationContext.class,0);
		}
		public MethodPatternContext methodPattern() {
			return getRuleContext(MethodPatternContext.class,0);
		}
		public TypeListContext typeList() {
			return getRuleContext(TypeListContext.class,0);
		}
		public TerminalNode StringLiteral() { return getToken(AspectJParser.StringLiteral, 0); }
		public PointcutExpressionContext pointcutExpression() {
			return getRuleContext(PointcutExpressionContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TypePatternListContext typePatternList() {
			return getRuleContext(TypePatternListContext.class,0);
		}
		public FieldPatternContext fieldPattern() {
			return getRuleContext(FieldPatternContext.class,0);
		}
		public TypePatternContext typePattern() {
			return getRuleContext(TypePatternContext.class,0);
		}
		public InterTypeDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interTypeDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterInterTypeDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitInterTypeDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitInterTypeDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InterTypeDeclarationContext interTypeDeclaration() throws RecognitionException {
		InterTypeDeclarationContext _localctx = new InterTypeDeclarationContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_interTypeDeclaration);
		try {
			setState(984);
			switch ( getInterpreter().adaptivePredict(_input,56,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(902); match(DECLARE);
				setState(903); match(PARENTS);
				setState(904); match(COLON);
				setState(905); typePattern(0);
				setState(906); match(EXTENDS);
				setState(907); type();
				setState(908); match(SEMI);
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(910); match(DECLARE);
				setState(911); match(PARENTS);
				setState(912); match(COLON);
				setState(913); typePattern(0);
				setState(914); match(IMPLEMENTS);
				setState(915); typeList();
				setState(916); match(SEMI);
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(918); match(DECLARE);
				setState(919); match(WARNING);
				setState(920); match(COLON);
				setState(921); pointcutExpression(0);
				setState(922); match(COLON);
				setState(923); match(StringLiteral);
				setState(924); match(SEMI);
				}
				break;

			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(926); match(DECLARE);
				setState(927); match(ERROR);
				setState(928); match(COLON);
				setState(929); pointcutExpression(0);
				setState(930); match(COLON);
				setState(931); match(StringLiteral);
				setState(932); match(SEMI);
				}
				break;

			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(934); match(DECLARE);
				setState(935); match(SOFT);
				setState(936); match(COLON);
				setState(937); type();
				setState(938); match(COLON);
				setState(939); pointcutExpression(0);
				setState(940); match(SEMI);
				}
				break;

			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(942); match(DECLARE);
				setState(943); match(PRECEDENCE);
				setState(944); match(COLON);
				setState(945); typePatternList();
				setState(946); match(SEMI);
				}
				break;

			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(948); match(DECLARE);
				setState(949); match(AT);
				setState(950); match(ANNOTATION_TYPE);
				setState(951); match(COLON);
				setState(952); typePattern(0);
				setState(953); match(COLON);
				setState(954); annotation();
				setState(955); match(SEMI);
				}
				break;

			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(957); match(DECLARE);
				setState(958); match(AT);
				setState(959); match(ANNOTATION_METHOD);
				setState(960); match(COLON);
				setState(961); methodPattern();
				setState(962); match(COLON);
				setState(963); annotation();
				setState(964); match(SEMI);
				}
				break;

			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(966); match(DECLARE);
				setState(967); match(AT);
				setState(968); match(ANNOTATION_CONSTRUCTOR);
				setState(969); match(COLON);
				setState(970); constructorPattern();
				setState(971); match(COLON);
				setState(972); annotation();
				setState(973); match(SEMI);
				}
				break;

			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(975); match(DECLARE);
				setState(976); match(AT);
				setState(977); match(ANNOTATION_FIELD);
				setState(978); match(COLON);
				setState(979); fieldPattern();
				setState(980); match(COLON);
				setState(981); annotation();
				setState(982); match(SEMI);
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

	public static class TypePatternContext extends ParserRuleContext {
		public SimpleTypePatternContext simpleTypePattern() {
			return getRuleContext(SimpleTypePatternContext.class,0);
		}
		public TypePatternContext typePattern(int i) {
			return getRuleContext(TypePatternContext.class,i);
		}
		public AnnotationPatternContext annotationPattern() {
			return getRuleContext(AnnotationPatternContext.class,0);
		}
		public List<TypePatternContext> typePattern() {
			return getRuleContexts(TypePatternContext.class);
		}
		public TypePatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typePattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterTypePattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitTypePattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitTypePattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypePatternContext typePattern() throws RecognitionException {
		return typePattern(0);
	}

	private TypePatternContext typePattern(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		TypePatternContext _localctx = new TypePatternContext(_ctx, _parentState);
		TypePatternContext _prevctx = _localctx;
		int _startState = 36;
		enterRecursionRule(_localctx, 36, RULE_typePattern, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(997);
			switch (_input.LA(1)) {
			case BANG:
				{
				setState(987); match(BANG);
				setState(988); typePattern(4);
				}
				break;
			case DOTDOT:
			case ARGS:
			case AFTER:
			case AROUND:
			case ASPECT:
			case BEFORE:
			case CALL:
			case CFLOW:
			case CFLOWBELOW:
			case DECLARE:
			case ERROR:
			case EXECUTION:
			case GET:
			case HANDLER:
			case INITIALIZATION:
			case ISSINGLETON:
			case PARENTS:
			case PERCFLOW:
			case PERCFLOWBELOW:
			case PERTARGET:
			case PERTHIS:
			case PERTYPEWITHIN:
			case POINTCUT:
			case PRECEDENCE:
			case PREINITIALIZATION:
			case PRIVILEGED:
			case RETURNING:
			case SET:
			case SOFT:
			case STATICINITIALIZATION:
			case TARGET:
			case THROWING:
			case WARNING:
			case WITHIN:
			case WITHINCODE:
			case BOOLEAN:
			case BYTE:
			case CHAR:
			case DOUBLE:
			case FLOAT:
			case INT:
			case LONG:
			case SHORT:
			case VOID:
			case DOT:
			case MUL:
			case Identifier:
				{
				setState(989); simpleTypePattern();
				}
				break;
			case LPAREN:
				{
				setState(990); match(LPAREN);
				setState(992);
				switch ( getInterpreter().adaptivePredict(_input,57,_ctx) ) {
				case 1:
					{
					setState(991); annotationPattern();
					}
					break;
				}
				setState(994); typePattern(0);
				setState(995); match(RPAREN);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(1007);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,60,_ctx);
			while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(1005);
					switch ( getInterpreter().adaptivePredict(_input,59,_ctx) ) {
					case 1:
						{
						_localctx = new TypePatternContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_typePattern);
						setState(999);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(1000); match(AND);
						setState(1001); typePattern(3);
						}
						break;

					case 2:
						{
						_localctx = new TypePatternContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_typePattern);
						setState(1002);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(1003); match(OR);
						setState(1004); typePattern(2);
						}
						break;
					}
					} 
				}
				setState(1009);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,60,_ctx);
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

	public static class SimpleTypePatternContext extends ParserRuleContext {
		public DottedNamePatternContext dottedNamePattern() {
			return getRuleContext(DottedNamePatternContext.class,0);
		}
		public SimpleTypePatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_simpleTypePattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterSimpleTypePattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitSimpleTypePattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitSimpleTypePattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SimpleTypePatternContext simpleTypePattern() throws RecognitionException {
		SimpleTypePatternContext _localctx = new SimpleTypePatternContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_simpleTypePattern);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1010); dottedNamePattern();
			setState(1012);
			switch ( getInterpreter().adaptivePredict(_input,61,_ctx) ) {
			case 1:
				{
				setState(1011); match(ADD);
				}
				break;
			}
			setState(1018);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,62,_ctx);
			while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1014); match(LBRACK);
					setState(1015); match(RBRACK);
					}
					} 
				}
				setState(1020);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,62,_ctx);
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

	public static class DottedNamePatternContext extends ParserRuleContext {
		public TypeContext type(int i) {
			return getRuleContext(TypeContext.class,i);
		}
		public IdContext id(int i) {
			return getRuleContext(IdContext.class,i);
		}
		public List<IdContext> id() {
			return getRuleContexts(IdContext.class);
		}
		public List<TypeContext> type() {
			return getRuleContexts(TypeContext.class);
		}
		public DottedNamePatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dottedNamePattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterDottedNamePattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitDottedNamePattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitDottedNamePattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DottedNamePatternContext dottedNamePattern() throws RecognitionException {
		DottedNamePatternContext _localctx = new DottedNamePatternContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_dottedNamePattern);
		try {
			int _alt;
			setState(1031);
			switch (_input.LA(1)) {
			case DOTDOT:
			case ARGS:
			case AFTER:
			case AROUND:
			case ASPECT:
			case BEFORE:
			case CALL:
			case CFLOW:
			case CFLOWBELOW:
			case DECLARE:
			case ERROR:
			case EXECUTION:
			case GET:
			case HANDLER:
			case INITIALIZATION:
			case ISSINGLETON:
			case PARENTS:
			case PERCFLOW:
			case PERCFLOWBELOW:
			case PERTARGET:
			case PERTHIS:
			case PERTYPEWITHIN:
			case POINTCUT:
			case PRECEDENCE:
			case PREINITIALIZATION:
			case PRIVILEGED:
			case RETURNING:
			case SET:
			case SOFT:
			case STATICINITIALIZATION:
			case TARGET:
			case THROWING:
			case WARNING:
			case WITHIN:
			case WITHINCODE:
			case BOOLEAN:
			case BYTE:
			case CHAR:
			case DOUBLE:
			case FLOAT:
			case INT:
			case LONG:
			case SHORT:
			case DOT:
			case MUL:
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(1026); 
				_errHandler.sync(this);
				_alt = 1;
				do {
					switch (_alt) {
					case 1:
						{
						setState(1026);
						switch ( getInterpreter().adaptivePredict(_input,63,_ctx) ) {
						case 1:
							{
							setState(1021); type();
							}
							break;

						case 2:
							{
							setState(1022); id();
							}
							break;

						case 3:
							{
							setState(1023); match(MUL);
							}
							break;

						case 4:
							{
							setState(1024); match(DOT);
							}
							break;

						case 5:
							{
							setState(1025); match(DOTDOT);
							}
							break;
						}
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					setState(1028); 
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,64,_ctx);
				} while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER );
				}
				break;
			case VOID:
				enterOuterAlt(_localctx, 2);
				{
				setState(1030); match(VOID);
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

	public static class OptionalParensTypePatternContext extends ParserRuleContext {
		public AnnotationPatternContext annotationPattern() {
			return getRuleContext(AnnotationPatternContext.class,0);
		}
		public TypePatternContext typePattern() {
			return getRuleContext(TypePatternContext.class,0);
		}
		public OptionalParensTypePatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_optionalParensTypePattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterOptionalParensTypePattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitOptionalParensTypePattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitOptionalParensTypePattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OptionalParensTypePatternContext optionalParensTypePattern() throws RecognitionException {
		OptionalParensTypePatternContext _localctx = new OptionalParensTypePatternContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_optionalParensTypePattern);
		try {
			setState(1044);
			switch ( getInterpreter().adaptivePredict(_input,68,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1033); match(LPAREN);
				setState(1035);
				switch ( getInterpreter().adaptivePredict(_input,66,_ctx) ) {
				case 1:
					{
					setState(1034); annotationPattern();
					}
					break;
				}
				setState(1037); typePattern(0);
				setState(1038); match(RPAREN);
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1041);
				switch ( getInterpreter().adaptivePredict(_input,67,_ctx) ) {
				case 1:
					{
					setState(1040); annotationPattern();
					}
					break;
				}
				setState(1043); typePattern(0);
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

	public static class FieldPatternContext extends ParserRuleContext {
		public FieldModifiersPatternContext fieldModifiersPattern() {
			return getRuleContext(FieldModifiersPatternContext.class,0);
		}
		public DotOrDotDotContext dotOrDotDot() {
			return getRuleContext(DotOrDotDotContext.class,0);
		}
		public TypePatternContext typePattern(int i) {
			return getRuleContext(TypePatternContext.class,i);
		}
		public AnnotationPatternContext annotationPattern() {
			return getRuleContext(AnnotationPatternContext.class,0);
		}
		public List<TypePatternContext> typePattern() {
			return getRuleContexts(TypePatternContext.class);
		}
		public SimpleNamePatternContext simpleNamePattern() {
			return getRuleContext(SimpleNamePatternContext.class,0);
		}
		public FieldPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fieldPattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterFieldPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitFieldPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitFieldPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FieldPatternContext fieldPattern() throws RecognitionException {
		FieldPatternContext _localctx = new FieldPatternContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_fieldPattern);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1047);
			switch ( getInterpreter().adaptivePredict(_input,69,_ctx) ) {
			case 1:
				{
				setState(1046); annotationPattern();
				}
				break;
			}
			setState(1050);
			switch ( getInterpreter().adaptivePredict(_input,70,_ctx) ) {
			case 1:
				{
				setState(1049); fieldModifiersPattern();
				}
				break;
			}
			setState(1052); typePattern(0);
			setState(1056);
			switch ( getInterpreter().adaptivePredict(_input,71,_ctx) ) {
			case 1:
				{
				setState(1053); typePattern(0);
				setState(1054); dotOrDotDot();
				}
				break;
			}
			setState(1058); simpleNamePattern();
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

	public static class FieldModifiersPatternContext extends ParserRuleContext {
		public FieldModifierContext fieldModifier() {
			return getRuleContext(FieldModifierContext.class,0);
		}
		public List<FieldModifiersPatternContext> fieldModifiersPattern() {
			return getRuleContexts(FieldModifiersPatternContext.class);
		}
		public FieldModifiersPatternContext fieldModifiersPattern(int i) {
			return getRuleContext(FieldModifiersPatternContext.class,i);
		}
		public FieldModifiersPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fieldModifiersPattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterFieldModifiersPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitFieldModifiersPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitFieldModifiersPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FieldModifiersPatternContext fieldModifiersPattern() throws RecognitionException {
		FieldModifiersPatternContext _localctx = new FieldModifiersPatternContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_fieldModifiersPattern);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1061);
			_la = _input.LA(1);
			if (_la==BANG) {
				{
				setState(1060); match(BANG);
				}
			}

			setState(1063); fieldModifier();
			setState(1067);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,73,_ctx);
			while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1064); fieldModifiersPattern();
					}
					} 
				}
				setState(1069);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,73,_ctx);
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

	public static class FieldModifierContext extends ParserRuleContext {
		public FieldModifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fieldModifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterFieldModifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitFieldModifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitFieldModifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FieldModifierContext fieldModifier() throws RecognitionException {
		FieldModifierContext _localctx = new FieldModifierContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_fieldModifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1070);
			_la = _input.LA(1);
			if ( !(((((_la - 76)) & ~0x3f) == 0 && ((1L << (_la - 76)) & ((1L << (FINAL - 76)) | (1L << (PRIVATE - 76)) | (1L << (PROTECTED - 76)) | (1L << (PUBLIC - 76)) | (1L << (STATIC - 76)) | (1L << (TRANSIENT - 76)))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			consume();
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

	public static class DotOrDotDotContext extends ParserRuleContext {
		public DotOrDotDotContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dotOrDotDot; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterDotOrDotDot(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitDotOrDotDot(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitDotOrDotDot(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DotOrDotDotContext dotOrDotDot() throws RecognitionException {
		DotOrDotDotContext _localctx = new DotOrDotDotContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_dotOrDotDot);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1072);
			_la = _input.LA(1);
			if ( !(_la==DOTDOT || _la==DOT) ) {
			_errHandler.recoverInline(this);
			}
			consume();
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

	public static class SimpleNamePatternContext extends ParserRuleContext {
		public IdContext id(int i) {
			return getRuleContext(IdContext.class,i);
		}
		public List<IdContext> id() {
			return getRuleContexts(IdContext.class);
		}
		public SimpleNamePatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_simpleNamePattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterSimpleNamePattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitSimpleNamePattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitSimpleNamePattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SimpleNamePatternContext simpleNamePattern() throws RecognitionException {
		SimpleNamePatternContext _localctx = new SimpleNamePatternContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_simpleNamePattern);
		int _la;
		try {
			int _alt;
			setState(1097);
			switch (_input.LA(1)) {
			case ARGS:
			case AFTER:
			case AROUND:
			case ASPECT:
			case BEFORE:
			case CALL:
			case CFLOW:
			case CFLOWBELOW:
			case DECLARE:
			case ERROR:
			case EXECUTION:
			case GET:
			case HANDLER:
			case INITIALIZATION:
			case ISSINGLETON:
			case PARENTS:
			case PERCFLOW:
			case PERCFLOWBELOW:
			case PERTARGET:
			case PERTHIS:
			case PERTYPEWITHIN:
			case POINTCUT:
			case PRECEDENCE:
			case PREINITIALIZATION:
			case PRIVILEGED:
			case RETURNING:
			case SET:
			case SOFT:
			case STATICINITIALIZATION:
			case TARGET:
			case THROWING:
			case WARNING:
			case WITHIN:
			case WITHINCODE:
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(1074); id();
				setState(1079);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,74,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1075); match(MUL);
						setState(1076); id();
						}
						} 
					}
					setState(1081);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,74,_ctx);
				}
				setState(1083);
				_la = _input.LA(1);
				if (_la==MUL) {
					{
					setState(1082); match(MUL);
					}
				}

				}
				break;
			case MUL:
				enterOuterAlt(_localctx, 2);
				{
				setState(1085); match(MUL);
				setState(1091);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,76,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1086); id();
						setState(1087); match(MUL);
						}
						} 
					}
					setState(1093);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,76,_ctx);
				}
				setState(1095);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ARGS) | (1L << AFTER) | (1L << AROUND) | (1L << ASPECT) | (1L << BEFORE) | (1L << CALL) | (1L << CFLOW) | (1L << CFLOWBELOW) | (1L << DECLARE) | (1L << ERROR) | (1L << EXECUTION) | (1L << GET) | (1L << HANDLER) | (1L << INITIALIZATION) | (1L << ISSINGLETON) | (1L << PARENTS) | (1L << PERCFLOW) | (1L << PERCFLOWBELOW) | (1L << PERTARGET) | (1L << PERTHIS) | (1L << PERTYPEWITHIN) | (1L << POINTCUT) | (1L << PRECEDENCE) | (1L << PREINITIALIZATION) | (1L << PRIVILEGED) | (1L << RETURNING) | (1L << SET) | (1L << SOFT) | (1L << STATICINITIALIZATION) | (1L << TARGET) | (1L << THROWING) | (1L << WARNING) | (1L << WITHIN) | (1L << WITHINCODE))) != 0) || _la==Identifier) {
					{
					setState(1094); id();
					}
				}

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

	public static class MethodOrConstructorPatternContext extends ParserRuleContext {
		public ConstructorPatternContext constructorPattern() {
			return getRuleContext(ConstructorPatternContext.class,0);
		}
		public MethodPatternContext methodPattern() {
			return getRuleContext(MethodPatternContext.class,0);
		}
		public MethodOrConstructorPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_methodOrConstructorPattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterMethodOrConstructorPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitMethodOrConstructorPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitMethodOrConstructorPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MethodOrConstructorPatternContext methodOrConstructorPattern() throws RecognitionException {
		MethodOrConstructorPatternContext _localctx = new MethodOrConstructorPatternContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_methodOrConstructorPattern);
		try {
			setState(1101);
			switch ( getInterpreter().adaptivePredict(_input,79,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1099); methodPattern();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1100); constructorPattern();
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

	public static class MethodPatternContext extends ParserRuleContext {
		public MethodModifiersPatternContext methodModifiersPattern() {
			return getRuleContext(MethodModifiersPatternContext.class,0);
		}
		public ThrowsPatternContext throwsPattern() {
			return getRuleContext(ThrowsPatternContext.class,0);
		}
		public FormalParametersPatternContext formalParametersPattern() {
			return getRuleContext(FormalParametersPatternContext.class,0);
		}
		public DotOrDotDotContext dotOrDotDot() {
			return getRuleContext(DotOrDotDotContext.class,0);
		}
		public TypePatternContext typePattern(int i) {
			return getRuleContext(TypePatternContext.class,i);
		}
		public AnnotationPatternContext annotationPattern() {
			return getRuleContext(AnnotationPatternContext.class,0);
		}
		public List<TypePatternContext> typePattern() {
			return getRuleContexts(TypePatternContext.class);
		}
		public SimpleNamePatternContext simpleNamePattern() {
			return getRuleContext(SimpleNamePatternContext.class,0);
		}
		public MethodPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_methodPattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterMethodPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitMethodPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitMethodPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MethodPatternContext methodPattern() throws RecognitionException {
		MethodPatternContext _localctx = new MethodPatternContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_methodPattern);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1104);
			switch ( getInterpreter().adaptivePredict(_input,80,_ctx) ) {
			case 1:
				{
				setState(1103); annotationPattern();
				}
				break;
			}
			setState(1107);
			switch ( getInterpreter().adaptivePredict(_input,81,_ctx) ) {
			case 1:
				{
				setState(1106); methodModifiersPattern();
				}
				break;
			}
			setState(1109); typePattern(0);
			setState(1113);
			switch ( getInterpreter().adaptivePredict(_input,82,_ctx) ) {
			case 1:
				{
				setState(1110); typePattern(0);
				setState(1111); dotOrDotDot();
				}
				break;
			}
			setState(1115); simpleNamePattern();
			setState(1116); formalParametersPattern();
			setState(1118);
			_la = _input.LA(1);
			if (_la==THROWS) {
				{
				setState(1117); throwsPattern();
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

	public static class MethodModifiersPatternContext extends ParserRuleContext {
		public List<MethodModifiersPatternContext> methodModifiersPattern() {
			return getRuleContexts(MethodModifiersPatternContext.class);
		}
		public MethodModifiersPatternContext methodModifiersPattern(int i) {
			return getRuleContext(MethodModifiersPatternContext.class,i);
		}
		public MethodModifierContext methodModifier() {
			return getRuleContext(MethodModifierContext.class,0);
		}
		public MethodModifiersPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_methodModifiersPattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterMethodModifiersPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitMethodModifiersPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitMethodModifiersPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MethodModifiersPatternContext methodModifiersPattern() throws RecognitionException {
		MethodModifiersPatternContext _localctx = new MethodModifiersPatternContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_methodModifiersPattern);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1121);
			_la = _input.LA(1);
			if (_la==BANG) {
				{
				setState(1120); match(BANG);
				}
			}

			setState(1123); methodModifier();
			setState(1127);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,85,_ctx);
			while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1124); methodModifiersPattern();
					}
					} 
				}
				setState(1129);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,85,_ctx);
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

	public static class MethodModifierContext extends ParserRuleContext {
		public MethodModifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_methodModifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterMethodModifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitMethodModifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitMethodModifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MethodModifierContext methodModifier() throws RecognitionException {
		MethodModifierContext _localctx = new MethodModifierContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_methodModifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1130);
			_la = _input.LA(1);
			if ( !(((((_la - 76)) & ~0x3f) == 0 && ((1L << (_la - 76)) & ((1L << (FINAL - 76)) | (1L << (PRIVATE - 76)) | (1L << (PROTECTED - 76)) | (1L << (PUBLIC - 76)) | (1L << (STATIC - 76)) | (1L << (SYNCHRONIZED - 76)))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			consume();
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

	public static class FormalsPatternContext extends ParserRuleContext {
		public FormalsPatternContext formalsPattern(int i) {
			return getRuleContext(FormalsPatternContext.class,i);
		}
		public List<FormalsPatternAfterDotDotContext> formalsPatternAfterDotDot() {
			return getRuleContexts(FormalsPatternAfterDotDotContext.class);
		}
		public FormalsPatternAfterDotDotContext formalsPatternAfterDotDot(int i) {
			return getRuleContext(FormalsPatternAfterDotDotContext.class,i);
		}
		public List<FormalsPatternContext> formalsPattern() {
			return getRuleContexts(FormalsPatternContext.class);
		}
		public TypePatternContext typePattern() {
			return getRuleContext(TypePatternContext.class,0);
		}
		public OptionalParensTypePatternContext optionalParensTypePattern() {
			return getRuleContext(OptionalParensTypePatternContext.class,0);
		}
		public FormalsPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_formalsPattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterFormalsPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitFormalsPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitFormalsPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FormalsPatternContext formalsPattern() throws RecognitionException {
		FormalsPatternContext _localctx = new FormalsPatternContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_formalsPattern);
		try {
			int _alt;
			setState(1151);
			switch ( getInterpreter().adaptivePredict(_input,88,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1132); match(DOTDOT);
				setState(1137);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,86,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1133); match(COMMA);
						setState(1134); formalsPatternAfterDotDot();
						}
						} 
					}
					setState(1139);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,86,_ctx);
				}
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1140); optionalParensTypePattern();
				setState(1145);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,87,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1141); match(COMMA);
						setState(1142); formalsPattern();
						}
						} 
					}
					setState(1147);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,87,_ctx);
				}
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1148); typePattern(0);
				setState(1149); match(ELLIPSIS);
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

	public static class FormalsPatternAfterDotDotContext extends ParserRuleContext {
		public List<FormalsPatternAfterDotDotContext> formalsPatternAfterDotDot() {
			return getRuleContexts(FormalsPatternAfterDotDotContext.class);
		}
		public FormalsPatternAfterDotDotContext formalsPatternAfterDotDot(int i) {
			return getRuleContext(FormalsPatternAfterDotDotContext.class,i);
		}
		public TypePatternContext typePattern() {
			return getRuleContext(TypePatternContext.class,0);
		}
		public OptionalParensTypePatternContext optionalParensTypePattern() {
			return getRuleContext(OptionalParensTypePatternContext.class,0);
		}
		public FormalsPatternAfterDotDotContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_formalsPatternAfterDotDot; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterFormalsPatternAfterDotDot(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitFormalsPatternAfterDotDot(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitFormalsPatternAfterDotDot(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FormalsPatternAfterDotDotContext formalsPatternAfterDotDot() throws RecognitionException {
		FormalsPatternAfterDotDotContext _localctx = new FormalsPatternAfterDotDotContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_formalsPatternAfterDotDot);
		try {
			int _alt;
			setState(1164);
			switch ( getInterpreter().adaptivePredict(_input,90,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1153); optionalParensTypePattern();
				setState(1158);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,89,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1154); match(COMMA);
						setState(1155); formalsPatternAfterDotDot();
						}
						} 
					}
					setState(1160);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,89,_ctx);
				}
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1161); typePattern(0);
				setState(1162); match(ELLIPSIS);
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

	public static class ThrowsPatternContext extends ParserRuleContext {
		public TypePatternListContext typePatternList() {
			return getRuleContext(TypePatternListContext.class,0);
		}
		public ThrowsPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_throwsPattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterThrowsPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitThrowsPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitThrowsPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ThrowsPatternContext throwsPattern() throws RecognitionException {
		ThrowsPatternContext _localctx = new ThrowsPatternContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_throwsPattern);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1166); match(THROWS);
			setState(1167); typePatternList();
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

	public static class TypePatternListContext extends ParserRuleContext {
		public TypePatternContext typePattern(int i) {
			return getRuleContext(TypePatternContext.class,i);
		}
		public List<TypePatternContext> typePattern() {
			return getRuleContexts(TypePatternContext.class);
		}
		public TypePatternListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typePatternList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterTypePatternList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitTypePatternList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitTypePatternList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypePatternListContext typePatternList() throws RecognitionException {
		TypePatternListContext _localctx = new TypePatternListContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_typePatternList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1169); typePattern(0);
			setState(1174);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1170); match(COMMA);
				setState(1171); typePattern(0);
				}
				}
				setState(1176);
				_errHandler.sync(this);
				_la = _input.LA(1);
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

	public static class ConstructorPatternContext extends ParserRuleContext {
		public ThrowsPatternContext throwsPattern() {
			return getRuleContext(ThrowsPatternContext.class,0);
		}
		public FormalParametersPatternContext formalParametersPattern() {
			return getRuleContext(FormalParametersPatternContext.class,0);
		}
		public ConstructorModifiersPatternContext constructorModifiersPattern() {
			return getRuleContext(ConstructorModifiersPatternContext.class,0);
		}
		public DotOrDotDotContext dotOrDotDot() {
			return getRuleContext(DotOrDotDotContext.class,0);
		}
		public AnnotationPatternContext annotationPattern() {
			return getRuleContext(AnnotationPatternContext.class,0);
		}
		public TypePatternContext typePattern() {
			return getRuleContext(TypePatternContext.class,0);
		}
		public ConstructorPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constructorPattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterConstructorPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitConstructorPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitConstructorPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstructorPatternContext constructorPattern() throws RecognitionException {
		ConstructorPatternContext _localctx = new ConstructorPatternContext(_ctx, getState());
		enterRule(_localctx, 70, RULE_constructorPattern);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1178);
			switch ( getInterpreter().adaptivePredict(_input,92,_ctx) ) {
			case 1:
				{
				setState(1177); annotationPattern();
				}
				break;
			}
			setState(1181);
			switch ( getInterpreter().adaptivePredict(_input,93,_ctx) ) {
			case 1:
				{
				setState(1180); constructorModifiersPattern();
				}
				break;
			}
			setState(1186);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << DOTDOT) | (1L << ARGS) | (1L << AFTER) | (1L << AROUND) | (1L << ASPECT) | (1L << BEFORE) | (1L << CALL) | (1L << CFLOW) | (1L << CFLOWBELOW) | (1L << DECLARE) | (1L << ERROR) | (1L << EXECUTION) | (1L << GET) | (1L << HANDLER) | (1L << INITIALIZATION) | (1L << ISSINGLETON) | (1L << PARENTS) | (1L << PERCFLOW) | (1L << PERCFLOWBELOW) | (1L << PERTARGET) | (1L << PERTHIS) | (1L << PERTYPEWITHIN) | (1L << POINTCUT) | (1L << PRECEDENCE) | (1L << PREINITIALIZATION) | (1L << PRIVILEGED) | (1L << RETURNING) | (1L << SET) | (1L << SOFT) | (1L << STATICINITIALIZATION) | (1L << TARGET) | (1L << THROWING) | (1L << WARNING) | (1L << WITHIN) | (1L << WITHINCODE) | (1L << BOOLEAN) | (1L << BYTE))) != 0) || ((((_la - 66)) & ~0x3f) == 0 && ((1L << (_la - 66)) & ((1L << (CHAR - 66)) | (1L << (DOUBLE - 66)) | (1L << (FLOAT - 66)) | (1L << (INT - 66)) | (1L << (LONG - 66)) | (1L << (SHORT - 66)) | (1L << (VOID - 66)) | (1L << (LPAREN - 66)) | (1L << (DOT - 66)) | (1L << (BANG - 66)))) != 0) || _la==MUL || _la==Identifier) {
				{
				setState(1183); typePattern(0);
				setState(1184); dotOrDotDot();
				}
			}

			setState(1188); match(NEW);
			setState(1189); formalParametersPattern();
			setState(1191);
			_la = _input.LA(1);
			if (_la==THROWS) {
				{
				setState(1190); throwsPattern();
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

	public static class ConstructorModifiersPatternContext extends ParserRuleContext {
		public ConstructorModifiersPatternContext constructorModifiersPattern(int i) {
			return getRuleContext(ConstructorModifiersPatternContext.class,i);
		}
		public ConstructorModifierContext constructorModifier() {
			return getRuleContext(ConstructorModifierContext.class,0);
		}
		public List<ConstructorModifiersPatternContext> constructorModifiersPattern() {
			return getRuleContexts(ConstructorModifiersPatternContext.class);
		}
		public ConstructorModifiersPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constructorModifiersPattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterConstructorModifiersPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitConstructorModifiersPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitConstructorModifiersPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstructorModifiersPatternContext constructorModifiersPattern() throws RecognitionException {
		ConstructorModifiersPatternContext _localctx = new ConstructorModifiersPatternContext(_ctx, getState());
		enterRule(_localctx, 72, RULE_constructorModifiersPattern);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1194);
			_la = _input.LA(1);
			if (_la==BANG) {
				{
				setState(1193); match(BANG);
				}
			}

			setState(1196); constructorModifier();
			setState(1200);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,97,_ctx);
			while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1197); constructorModifiersPattern();
					}
					} 
				}
				setState(1202);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,97,_ctx);
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

	public static class ConstructorModifierContext extends ParserRuleContext {
		public ConstructorModifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constructorModifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterConstructorModifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitConstructorModifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitConstructorModifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstructorModifierContext constructorModifier() throws RecognitionException {
		ConstructorModifierContext _localctx = new ConstructorModifierContext(_ctx, getState());
		enterRule(_localctx, 74, RULE_constructorModifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1203);
			_la = _input.LA(1);
			if ( !(((((_la - 91)) & ~0x3f) == 0 && ((1L << (_la - 91)) & ((1L << (PRIVATE - 91)) | (1L << (PROTECTED - 91)) | (1L << (PUBLIC - 91)))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			consume();
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

	public static class AnnotationPatternContext extends ParserRuleContext {
		public AnnotationPatternContext annotationPattern(int i) {
			return getRuleContext(AnnotationPatternContext.class,i);
		}
		public AnnotationTypePatternContext annotationTypePattern() {
			return getRuleContext(AnnotationTypePatternContext.class,0);
		}
		public List<AnnotationPatternContext> annotationPattern() {
			return getRuleContexts(AnnotationPatternContext.class);
		}
		public AnnotationPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotationPattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterAnnotationPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitAnnotationPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitAnnotationPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnnotationPatternContext annotationPattern() throws RecognitionException {
		AnnotationPatternContext _localctx = new AnnotationPatternContext(_ctx, getState());
		enterRule(_localctx, 76, RULE_annotationPattern);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1206);
			_la = _input.LA(1);
			if (_la==BANG) {
				{
				setState(1205); match(BANG);
				}
			}

			setState(1208); match(AT);
			setState(1209); annotationTypePattern();
			setState(1213);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,99,_ctx);
			while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1210); annotationPattern();
					}
					} 
				}
				setState(1215);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,99,_ctx);
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

	public static class AnnotationTypePatternContext extends ParserRuleContext {
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public TypePatternContext typePattern() {
			return getRuleContext(TypePatternContext.class,0);
		}
		public AnnotationTypePatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotationTypePattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterAnnotationTypePattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitAnnotationTypePattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitAnnotationTypePattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnnotationTypePatternContext annotationTypePattern() throws RecognitionException {
		AnnotationTypePatternContext _localctx = new AnnotationTypePatternContext(_ctx, getState());
		enterRule(_localctx, 78, RULE_annotationTypePattern);
		try {
			setState(1221);
			switch (_input.LA(1)) {
			case ARGS:
			case AFTER:
			case AROUND:
			case ASPECT:
			case BEFORE:
			case CALL:
			case CFLOW:
			case CFLOWBELOW:
			case DECLARE:
			case ERROR:
			case EXECUTION:
			case GET:
			case HANDLER:
			case INITIALIZATION:
			case ISSINGLETON:
			case PARENTS:
			case PERCFLOW:
			case PERCFLOWBELOW:
			case PERTARGET:
			case PERTHIS:
			case PERTYPEWITHIN:
			case POINTCUT:
			case PRECEDENCE:
			case PREINITIALIZATION:
			case PRIVILEGED:
			case RETURNING:
			case SET:
			case SOFT:
			case STATICINITIALIZATION:
			case TARGET:
			case THROWING:
			case WARNING:
			case WITHIN:
			case WITHINCODE:
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(1216); qualifiedName();
				}
				break;
			case LPAREN:
				enterOuterAlt(_localctx, 2);
				{
				setState(1217); match(LPAREN);
				setState(1218); typePattern(0);
				setState(1219); match(RPAREN);
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

	public static class FormalParametersPatternContext extends ParserRuleContext {
		public FormalsPatternContext formalsPattern() {
			return getRuleContext(FormalsPatternContext.class,0);
		}
		public FormalParametersPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_formalParametersPattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterFormalParametersPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitFormalParametersPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitFormalParametersPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FormalParametersPatternContext formalParametersPattern() throws RecognitionException {
		FormalParametersPatternContext _localctx = new FormalParametersPatternContext(_ctx, getState());
		enterRule(_localctx, 80, RULE_formalParametersPattern);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1223); match(LPAREN);
			setState(1225);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << DOTDOT) | (1L << ARGS) | (1L << AFTER) | (1L << AROUND) | (1L << ASPECT) | (1L << BEFORE) | (1L << CALL) | (1L << CFLOW) | (1L << CFLOWBELOW) | (1L << DECLARE) | (1L << ERROR) | (1L << EXECUTION) | (1L << GET) | (1L << HANDLER) | (1L << INITIALIZATION) | (1L << ISSINGLETON) | (1L << PARENTS) | (1L << PERCFLOW) | (1L << PERCFLOWBELOW) | (1L << PERTARGET) | (1L << PERTHIS) | (1L << PERTYPEWITHIN) | (1L << POINTCUT) | (1L << PRECEDENCE) | (1L << PREINITIALIZATION) | (1L << PRIVILEGED) | (1L << RETURNING) | (1L << SET) | (1L << SOFT) | (1L << STATICINITIALIZATION) | (1L << TARGET) | (1L << THROWING) | (1L << WARNING) | (1L << WITHIN) | (1L << WITHINCODE) | (1L << AT) | (1L << BOOLEAN) | (1L << BYTE))) != 0) || ((((_la - 66)) & ~0x3f) == 0 && ((1L << (_la - 66)) & ((1L << (CHAR - 66)) | (1L << (DOUBLE - 66)) | (1L << (FLOAT - 66)) | (1L << (INT - 66)) | (1L << (LONG - 66)) | (1L << (SHORT - 66)) | (1L << (VOID - 66)) | (1L << (LPAREN - 66)) | (1L << (DOT - 66)) | (1L << (BANG - 66)))) != 0) || _la==MUL || _la==Identifier) {
				{
				setState(1224); formalsPattern();
				}
			}

			setState(1227); match(RPAREN);
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

	public static class TypeOrIdentifierContext extends ParserRuleContext {
		public VariableDeclaratorIdContext variableDeclaratorId() {
			return getRuleContext(VariableDeclaratorIdContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TypeOrIdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeOrIdentifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterTypeOrIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitTypeOrIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitTypeOrIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeOrIdentifierContext typeOrIdentifier() throws RecognitionException {
		TypeOrIdentifierContext _localctx = new TypeOrIdentifierContext(_ctx, getState());
		enterRule(_localctx, 82, RULE_typeOrIdentifier);
		try {
			setState(1231);
			switch ( getInterpreter().adaptivePredict(_input,102,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1229); type();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1230); variableDeclaratorId();
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

	public static class AnnotationOrIdentiferContext extends ParserRuleContext {
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public AnnotationOrIdentiferContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotationOrIdentifer; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterAnnotationOrIdentifer(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitAnnotationOrIdentifer(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitAnnotationOrIdentifer(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnnotationOrIdentiferContext annotationOrIdentifer() throws RecognitionException {
		AnnotationOrIdentiferContext _localctx = new AnnotationOrIdentiferContext(_ctx, getState());
		enterRule(_localctx, 84, RULE_annotationOrIdentifer);
		try {
			setState(1235);
			switch ( getInterpreter().adaptivePredict(_input,103,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1233); qualifiedName();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1234); id();
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

	public static class AnnotationsOrIdentifiersPatternContext extends ParserRuleContext {
		public AnnotationsOrIdentifiersPatternContext annotationsOrIdentifiersPattern(int i) {
			return getRuleContext(AnnotationsOrIdentifiersPatternContext.class,i);
		}
		public AnnotationOrIdentiferContext annotationOrIdentifer() {
			return getRuleContext(AnnotationOrIdentiferContext.class,0);
		}
		public List<AnnotationsOrIdentifiersPatternContext> annotationsOrIdentifiersPattern() {
			return getRuleContexts(AnnotationsOrIdentifiersPatternContext.class);
		}
		public AnnotationsOrIdentifiersPatternAfterDotDotContext annotationsOrIdentifiersPatternAfterDotDot() {
			return getRuleContext(AnnotationsOrIdentifiersPatternAfterDotDotContext.class,0);
		}
		public AnnotationsOrIdentifiersPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotationsOrIdentifiersPattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterAnnotationsOrIdentifiersPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitAnnotationsOrIdentifiersPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitAnnotationsOrIdentifiersPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnnotationsOrIdentifiersPatternContext annotationsOrIdentifiersPattern() throws RecognitionException {
		AnnotationsOrIdentifiersPatternContext _localctx = new AnnotationsOrIdentifiersPatternContext(_ctx, getState());
		enterRule(_localctx, 86, RULE_annotationsOrIdentifiersPattern);
		try {
			int _alt;
			setState(1258);
			switch (_input.LA(1)) {
			case DOTDOT:
				enterOuterAlt(_localctx, 1);
				{
				setState(1237); match(DOTDOT);
				setState(1240);
				switch ( getInterpreter().adaptivePredict(_input,104,_ctx) ) {
				case 1:
					{
					setState(1238); match(COMMA);
					setState(1239); annotationsOrIdentifiersPatternAfterDotDot();
					}
					break;
				}
				}
				break;
			case ARGS:
			case AFTER:
			case AROUND:
			case ASPECT:
			case BEFORE:
			case CALL:
			case CFLOW:
			case CFLOWBELOW:
			case DECLARE:
			case ERROR:
			case EXECUTION:
			case GET:
			case HANDLER:
			case INITIALIZATION:
			case ISSINGLETON:
			case PARENTS:
			case PERCFLOW:
			case PERCFLOWBELOW:
			case PERTARGET:
			case PERTHIS:
			case PERTYPEWITHIN:
			case POINTCUT:
			case PRECEDENCE:
			case PREINITIALIZATION:
			case PRIVILEGED:
			case RETURNING:
			case SET:
			case SOFT:
			case STATICINITIALIZATION:
			case TARGET:
			case THROWING:
			case WARNING:
			case WITHIN:
			case WITHINCODE:
			case Identifier:
				enterOuterAlt(_localctx, 2);
				{
				setState(1242); annotationOrIdentifer();
				setState(1247);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,105,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1243); match(COMMA);
						setState(1244); annotationsOrIdentifiersPattern();
						}
						} 
					}
					setState(1249);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,105,_ctx);
				}
				}
				break;
			case MUL:
				enterOuterAlt(_localctx, 3);
				{
				setState(1250); match(MUL);
				setState(1255);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,106,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1251); match(COMMA);
						setState(1252); annotationsOrIdentifiersPattern();
						}
						} 
					}
					setState(1257);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,106,_ctx);
				}
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

	public static class AnnotationsOrIdentifiersPatternAfterDotDotContext extends ParserRuleContext {
		public AnnotationsOrIdentifiersPatternAfterDotDotContext annotationsOrIdentifiersPatternAfterDotDot(int i) {
			return getRuleContext(AnnotationsOrIdentifiersPatternAfterDotDotContext.class,i);
		}
		public AnnotationOrIdentiferContext annotationOrIdentifer() {
			return getRuleContext(AnnotationOrIdentiferContext.class,0);
		}
		public List<AnnotationsOrIdentifiersPatternAfterDotDotContext> annotationsOrIdentifiersPatternAfterDotDot() {
			return getRuleContexts(AnnotationsOrIdentifiersPatternAfterDotDotContext.class);
		}
		public AnnotationsOrIdentifiersPatternAfterDotDotContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotationsOrIdentifiersPatternAfterDotDot; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterAnnotationsOrIdentifiersPatternAfterDotDot(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitAnnotationsOrIdentifiersPatternAfterDotDot(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitAnnotationsOrIdentifiersPatternAfterDotDot(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnnotationsOrIdentifiersPatternAfterDotDotContext annotationsOrIdentifiersPatternAfterDotDot() throws RecognitionException {
		AnnotationsOrIdentifiersPatternAfterDotDotContext _localctx = new AnnotationsOrIdentifiersPatternAfterDotDotContext(_ctx, getState());
		enterRule(_localctx, 88, RULE_annotationsOrIdentifiersPatternAfterDotDot);
		try {
			int _alt;
			setState(1276);
			switch (_input.LA(1)) {
			case ARGS:
			case AFTER:
			case AROUND:
			case ASPECT:
			case BEFORE:
			case CALL:
			case CFLOW:
			case CFLOWBELOW:
			case DECLARE:
			case ERROR:
			case EXECUTION:
			case GET:
			case HANDLER:
			case INITIALIZATION:
			case ISSINGLETON:
			case PARENTS:
			case PERCFLOW:
			case PERCFLOWBELOW:
			case PERTARGET:
			case PERTHIS:
			case PERTYPEWITHIN:
			case POINTCUT:
			case PRECEDENCE:
			case PREINITIALIZATION:
			case PRIVILEGED:
			case RETURNING:
			case SET:
			case SOFT:
			case STATICINITIALIZATION:
			case TARGET:
			case THROWING:
			case WARNING:
			case WITHIN:
			case WITHINCODE:
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(1260); annotationOrIdentifer();
				setState(1265);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,108,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1261); match(COMMA);
						setState(1262); annotationsOrIdentifiersPatternAfterDotDot();
						}
						} 
					}
					setState(1267);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,108,_ctx);
				}
				}
				break;
			case MUL:
				enterOuterAlt(_localctx, 2);
				{
				setState(1268); match(MUL);
				setState(1273);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,109,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1269); match(COMMA);
						setState(1270); annotationsOrIdentifiersPatternAfterDotDot();
						}
						} 
					}
					setState(1275);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,109,_ctx);
				}
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

	public static class ArgsPatternContext extends ParserRuleContext {
		public TypeOrIdentifierContext typeOrIdentifier() {
			return getRuleContext(TypeOrIdentifierContext.class,0);
		}
		public ArgsPatternContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_argsPattern; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterArgsPattern(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitArgsPattern(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitArgsPattern(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArgsPatternContext argsPattern() throws RecognitionException {
		ArgsPatternContext _localctx = new ArgsPatternContext(_ctx, getState());
		enterRule(_localctx, 90, RULE_argsPattern);
		int _la;
		try {
			setState(1280);
			switch (_input.LA(1)) {
			case ARGS:
			case AFTER:
			case AROUND:
			case ASPECT:
			case BEFORE:
			case CALL:
			case CFLOW:
			case CFLOWBELOW:
			case DECLARE:
			case ERROR:
			case EXECUTION:
			case GET:
			case HANDLER:
			case INITIALIZATION:
			case ISSINGLETON:
			case PARENTS:
			case PERCFLOW:
			case PERCFLOWBELOW:
			case PERTARGET:
			case PERTHIS:
			case PERTYPEWITHIN:
			case POINTCUT:
			case PRECEDENCE:
			case PREINITIALIZATION:
			case PRIVILEGED:
			case RETURNING:
			case SET:
			case SOFT:
			case STATICINITIALIZATION:
			case TARGET:
			case THROWING:
			case WARNING:
			case WITHIN:
			case WITHINCODE:
			case BOOLEAN:
			case BYTE:
			case CHAR:
			case DOUBLE:
			case FLOAT:
			case INT:
			case LONG:
			case SHORT:
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(1278); typeOrIdentifier();
				}
				break;
			case DOTDOT:
			case MUL:
				enterOuterAlt(_localctx, 2);
				{
				setState(1279);
				_la = _input.LA(1);
				if ( !(_la==DOTDOT || _la==MUL) ) {
				_errHandler.recoverInline(this);
				}
				consume();
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

	public static class ArgsPatternListContext extends ParserRuleContext {
		public ArgsPatternContext argsPattern(int i) {
			return getRuleContext(ArgsPatternContext.class,i);
		}
		public List<ArgsPatternContext> argsPattern() {
			return getRuleContexts(ArgsPatternContext.class);
		}
		public ArgsPatternListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_argsPatternList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterArgsPatternList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitArgsPatternList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitArgsPatternList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArgsPatternListContext argsPatternList() throws RecognitionException {
		ArgsPatternListContext _localctx = new ArgsPatternListContext(_ctx, getState());
		enterRule(_localctx, 92, RULE_argsPatternList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1282); argsPattern();
			setState(1287);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1283); match(COMMA);
				setState(1284); argsPattern();
				}
				}
				setState(1289);
				_errHandler.sync(this);
				_la = _input.LA(1);
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

	public static class IdContext extends ParserRuleContext {
		public TerminalNode PREINITIALIZATION() { return getToken(AspectJParser.PREINITIALIZATION, 0); }
		public TerminalNode ASPECT() { return getToken(AspectJParser.ASPECT, 0); }
		public TerminalNode ERROR() { return getToken(AspectJParser.ERROR, 0); }
		public TerminalNode CALL() { return getToken(AspectJParser.CALL, 0); }
		public TerminalNode ISSINGLETON() { return getToken(AspectJParser.ISSINGLETON, 0); }
		public TerminalNode PRECEDENCE() { return getToken(AspectJParser.PRECEDENCE, 0); }
		public TerminalNode PERTHIS() { return getToken(AspectJParser.PERTHIS, 0); }
		public TerminalNode HANDLER() { return getToken(AspectJParser.HANDLER, 0); }
		public TerminalNode WITHIN() { return getToken(AspectJParser.WITHIN, 0); }
		public TerminalNode PERCFLOWBELOW() { return getToken(AspectJParser.PERCFLOWBELOW, 0); }
		public TerminalNode CFLOW() { return getToken(AspectJParser.CFLOW, 0); }
		public TerminalNode SOFT() { return getToken(AspectJParser.SOFT, 0); }
		public TerminalNode TARGET() { return getToken(AspectJParser.TARGET, 0); }
		public TerminalNode AFTER() { return getToken(AspectJParser.AFTER, 0); }
		public TerminalNode AROUND() { return getToken(AspectJParser.AROUND, 0); }
		public TerminalNode PERCFLOW() { return getToken(AspectJParser.PERCFLOW, 0); }
		public TerminalNode PERTYPEWITHIN() { return getToken(AspectJParser.PERTYPEWITHIN, 0); }
		public TerminalNode INITIALIZATION() { return getToken(AspectJParser.INITIALIZATION, 0); }
		public TerminalNode DECLARE() { return getToken(AspectJParser.DECLARE, 0); }
		public TerminalNode BEFORE() { return getToken(AspectJParser.BEFORE, 0); }
		public TerminalNode CFLOWBELOW() { return getToken(AspectJParser.CFLOWBELOW, 0); }
		public TerminalNode STATICINITIALIZATION() { return getToken(AspectJParser.STATICINITIALIZATION, 0); }
		public TerminalNode PARENTS() { return getToken(AspectJParser.PARENTS, 0); }
		public TerminalNode POINTCUT() { return getToken(AspectJParser.POINTCUT, 0); }
		public TerminalNode PRIVILEGED() { return getToken(AspectJParser.PRIVILEGED, 0); }
		public TerminalNode GET() { return getToken(AspectJParser.GET, 0); }
		public TerminalNode THROWING() { return getToken(AspectJParser.THROWING, 0); }
		public TerminalNode PERTARGET() { return getToken(AspectJParser.PERTARGET, 0); }
		public TerminalNode WITHINCODE() { return getToken(AspectJParser.WITHINCODE, 0); }
		public TerminalNode ARGS() { return getToken(AspectJParser.ARGS, 0); }
		public TerminalNode Identifier() { return getToken(AspectJParser.Identifier, 0); }
		public TerminalNode WARNING() { return getToken(AspectJParser.WARNING, 0); }
		public TerminalNode EXECUTION() { return getToken(AspectJParser.EXECUTION, 0); }
		public TerminalNode SET() { return getToken(AspectJParser.SET, 0); }
		public TerminalNode RETURNING() { return getToken(AspectJParser.RETURNING, 0); }
		public IdContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_id; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterId(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitId(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitId(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdContext id() throws RecognitionException {
		IdContext _localctx = new IdContext(_ctx, getState());
		enterRule(_localctx, 94, RULE_id);
		int _la;
		try {
			setState(1292);
			switch (_input.LA(1)) {
			case ARGS:
			case AFTER:
			case AROUND:
			case ASPECT:
			case BEFORE:
			case CALL:
			case CFLOW:
			case CFLOWBELOW:
			case DECLARE:
			case ERROR:
			case EXECUTION:
			case GET:
			case HANDLER:
			case INITIALIZATION:
			case ISSINGLETON:
			case PARENTS:
			case PERCFLOW:
			case PERCFLOWBELOW:
			case PERTARGET:
			case PERTHIS:
			case PERTYPEWITHIN:
			case POINTCUT:
			case PRECEDENCE:
			case PREINITIALIZATION:
			case PRIVILEGED:
			case RETURNING:
			case SET:
			case SOFT:
			case STATICINITIALIZATION:
			case TARGET:
			case THROWING:
			case WARNING:
			case WITHIN:
			case WITHINCODE:
				enterOuterAlt(_localctx, 1);
				{
				setState(1290);
				_la = _input.LA(1);
				if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ARGS) | (1L << AFTER) | (1L << AROUND) | (1L << ASPECT) | (1L << BEFORE) | (1L << CALL) | (1L << CFLOW) | (1L << CFLOWBELOW) | (1L << DECLARE) | (1L << ERROR) | (1L << EXECUTION) | (1L << GET) | (1L << HANDLER) | (1L << INITIALIZATION) | (1L << ISSINGLETON) | (1L << PARENTS) | (1L << PERCFLOW) | (1L << PERCFLOWBELOW) | (1L << PERTARGET) | (1L << PERTHIS) | (1L << PERTYPEWITHIN) | (1L << POINTCUT) | (1L << PRECEDENCE) | (1L << PREINITIALIZATION) | (1L << PRIVILEGED) | (1L << RETURNING) | (1L << SET) | (1L << SOFT) | (1L << STATICINITIALIZATION) | (1L << TARGET) | (1L << THROWING) | (1L << WARNING) | (1L << WITHIN) | (1L << WITHINCODE))) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				consume();
				}
				break;
			case Identifier:
				enterOuterAlt(_localctx, 2);
				{
				setState(1291); match(Identifier);
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

	public static class ClassDeclarationContext extends ParserRuleContext {
		public ClassBodyContext classBody() {
			return getRuleContext(ClassBodyContext.class,0);
		}
		public TypeListContext typeList() {
			return getRuleContext(TypeListContext.class,0);
		}
		public TypeParametersContext typeParameters() {
			return getRuleContext(TypeParametersContext.class,0);
		}
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public ClassDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_classDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterClassDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitClassDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitClassDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ClassDeclarationContext classDeclaration() throws RecognitionException {
		ClassDeclarationContext _localctx = new ClassDeclarationContext(_ctx, getState());
		enterRule(_localctx, 96, RULE_classDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1294); match(CLASS);
			setState(1295); id();
			setState(1297);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(1296); typeParameters();
				}
			}

			setState(1301);
			_la = _input.LA(1);
			if (_la==EXTENDS) {
				{
				setState(1299); match(EXTENDS);
				setState(1300); type();
				}
			}

			setState(1305);
			_la = _input.LA(1);
			if (_la==IMPLEMENTS) {
				{
				setState(1303); match(IMPLEMENTS);
				setState(1304); typeList();
				}
			}

			setState(1307); classBody();
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

	public static class TypeParameterContext extends ParserRuleContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public TypeBoundContext typeBound() {
			return getRuleContext(TypeBoundContext.class,0);
		}
		public TypeParameterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeParameter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterTypeParameter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitTypeParameter(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitTypeParameter(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeParameterContext typeParameter() throws RecognitionException {
		TypeParameterContext _localctx = new TypeParameterContext(_ctx, getState());
		enterRule(_localctx, 98, RULE_typeParameter);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1309); id();
			setState(1312);
			_la = _input.LA(1);
			if (_la==EXTENDS) {
				{
				setState(1310); match(EXTENDS);
				setState(1311); typeBound();
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

	public static class EnumDeclarationContext extends ParserRuleContext {
		public EnumBodyDeclarationsContext enumBodyDeclarations() {
			return getRuleContext(EnumBodyDeclarationsContext.class,0);
		}
		public TypeListContext typeList() {
			return getRuleContext(TypeListContext.class,0);
		}
		public TerminalNode ENUM() { return getToken(AspectJParser.ENUM, 0); }
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public EnumConstantsContext enumConstants() {
			return getRuleContext(EnumConstantsContext.class,0);
		}
		public EnumDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enumDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterEnumDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitEnumDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitEnumDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EnumDeclarationContext enumDeclaration() throws RecognitionException {
		EnumDeclarationContext _localctx = new EnumDeclarationContext(_ctx, getState());
		enterRule(_localctx, 100, RULE_enumDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1314); match(ENUM);
			setState(1315); id();
			setState(1318);
			_la = _input.LA(1);
			if (_la==IMPLEMENTS) {
				{
				setState(1316); match(IMPLEMENTS);
				setState(1317); typeList();
				}
			}

			setState(1320); match(LBRACE);
			setState(1322);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ARGS) | (1L << AFTER) | (1L << AROUND) | (1L << ASPECT) | (1L << BEFORE) | (1L << CALL) | (1L << CFLOW) | (1L << CFLOWBELOW) | (1L << DECLARE) | (1L << ERROR) | (1L << EXECUTION) | (1L << GET) | (1L << HANDLER) | (1L << INITIALIZATION) | (1L << ISSINGLETON) | (1L << PARENTS) | (1L << PERCFLOW) | (1L << PERCFLOWBELOW) | (1L << PERTARGET) | (1L << PERTHIS) | (1L << PERTYPEWITHIN) | (1L << POINTCUT) | (1L << PRECEDENCE) | (1L << PREINITIALIZATION) | (1L << PRIVILEGED) | (1L << RETURNING) | (1L << SET) | (1L << SOFT) | (1L << STATICINITIALIZATION) | (1L << TARGET) | (1L << THROWING) | (1L << WARNING) | (1L << WITHIN) | (1L << WITHINCODE) | (1L << AT))) != 0) || _la==Identifier) {
				{
				setState(1321); enumConstants();
				}
			}

			setState(1325);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(1324); match(COMMA);
				}
			}

			setState(1328);
			_la = _input.LA(1);
			if (_la==SEMI) {
				{
				setState(1327); enumBodyDeclarations();
				}
			}

			setState(1330); match(RBRACE);
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

	public static class EnumConstantContext extends ParserRuleContext {
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public ClassBodyContext classBody() {
			return getRuleContext(ClassBodyContext.class,0);
		}
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public ArgumentsContext arguments() {
			return getRuleContext(ArgumentsContext.class,0);
		}
		public EnumConstantContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enumConstant; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterEnumConstant(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitEnumConstant(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitEnumConstant(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EnumConstantContext enumConstant() throws RecognitionException {
		EnumConstantContext _localctx = new EnumConstantContext(_ctx, getState());
		enterRule(_localctx, 102, RULE_enumConstant);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1335);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT) {
				{
				{
				setState(1332); annotation();
				}
				}
				setState(1337);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1338); id();
			setState(1340);
			_la = _input.LA(1);
			if (_la==LPAREN) {
				{
				setState(1339); arguments();
				}
			}

			setState(1343);
			_la = _input.LA(1);
			if (_la==LBRACE) {
				{
				setState(1342); classBody();
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

	public static class InterfaceDeclarationContext extends ParserRuleContext {
		public InterfaceBodyContext interfaceBody() {
			return getRuleContext(InterfaceBodyContext.class,0);
		}
		public TypeListContext typeList() {
			return getRuleContext(TypeListContext.class,0);
		}
		public TypeParametersContext typeParameters() {
			return getRuleContext(TypeParametersContext.class,0);
		}
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public InterfaceDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interfaceDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterInterfaceDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitInterfaceDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitInterfaceDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InterfaceDeclarationContext interfaceDeclaration() throws RecognitionException {
		InterfaceDeclarationContext _localctx = new InterfaceDeclarationContext(_ctx, getState());
		enterRule(_localctx, 104, RULE_interfaceDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1345); match(INTERFACE);
			setState(1346); id();
			setState(1348);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(1347); typeParameters();
				}
			}

			setState(1352);
			_la = _input.LA(1);
			if (_la==EXTENDS) {
				{
				setState(1350); match(EXTENDS);
				setState(1351); typeList();
				}
			}

			setState(1354); interfaceBody();
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

	public static class MethodDeclarationContext extends ParserRuleContext {
		public MethodBodyContext methodBody() {
			return getRuleContext(MethodBodyContext.class,0);
		}
		public QualifiedNameListContext qualifiedNameList() {
			return getRuleContext(QualifiedNameListContext.class,0);
		}
		public FormalParametersContext formalParameters() {
			return getRuleContext(FormalParametersContext.class,0);
		}
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public MethodDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_methodDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterMethodDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitMethodDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitMethodDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MethodDeclarationContext methodDeclaration() throws RecognitionException {
		MethodDeclarationContext _localctx = new MethodDeclarationContext(_ctx, getState());
		enterRule(_localctx, 106, RULE_methodDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1358);
			switch (_input.LA(1)) {
			case ARGS:
			case AFTER:
			case AROUND:
			case ASPECT:
			case BEFORE:
			case CALL:
			case CFLOW:
			case CFLOWBELOW:
			case DECLARE:
			case ERROR:
			case EXECUTION:
			case GET:
			case HANDLER:
			case INITIALIZATION:
			case ISSINGLETON:
			case PARENTS:
			case PERCFLOW:
			case PERCFLOWBELOW:
			case PERTARGET:
			case PERTHIS:
			case PERTYPEWITHIN:
			case POINTCUT:
			case PRECEDENCE:
			case PREINITIALIZATION:
			case PRIVILEGED:
			case RETURNING:
			case SET:
			case SOFT:
			case STATICINITIALIZATION:
			case TARGET:
			case THROWING:
			case WARNING:
			case WITHIN:
			case WITHINCODE:
			case BOOLEAN:
			case BYTE:
			case CHAR:
			case DOUBLE:
			case FLOAT:
			case INT:
			case LONG:
			case SHORT:
			case Identifier:
				{
				setState(1356); type();
				}
				break;
			case VOID:
				{
				setState(1357); match(VOID);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(1360); id();
			setState(1361); formalParameters();
			setState(1366);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==LBRACK) {
				{
				{
				setState(1362); match(LBRACK);
				setState(1363); match(RBRACK);
				}
				}
				setState(1368);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1371);
			_la = _input.LA(1);
			if (_la==THROWS) {
				{
				setState(1369); match(THROWS);
				setState(1370); qualifiedNameList();
				}
			}

			setState(1375);
			switch (_input.LA(1)) {
			case LBRACE:
				{
				setState(1373); methodBody();
				}
				break;
			case SEMI:
				{
				setState(1374); match(SEMI);
				}
				break;
			default:
				throw new NoViableAltException(this);
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

	public static class ConstructorDeclarationContext extends ParserRuleContext {
		public ConstructorBodyContext constructorBody() {
			return getRuleContext(ConstructorBodyContext.class,0);
		}
		public QualifiedNameListContext qualifiedNameList() {
			return getRuleContext(QualifiedNameListContext.class,0);
		}
		public FormalParametersContext formalParameters() {
			return getRuleContext(FormalParametersContext.class,0);
		}
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public ConstructorDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constructorDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterConstructorDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitConstructorDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitConstructorDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstructorDeclarationContext constructorDeclaration() throws RecognitionException {
		ConstructorDeclarationContext _localctx = new ConstructorDeclarationContext(_ctx, getState());
		enterRule(_localctx, 108, RULE_constructorDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1377); id();
			setState(1378); formalParameters();
			setState(1381);
			_la = _input.LA(1);
			if (_la==THROWS) {
				{
				setState(1379); match(THROWS);
				setState(1380); qualifiedNameList();
				}
			}

			setState(1383); constructorBody();
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

	public static class ConstantDeclaratorContext extends ParserRuleContext {
		public VariableInitializerContext variableInitializer() {
			return getRuleContext(VariableInitializerContext.class,0);
		}
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public ConstantDeclaratorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constantDeclarator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterConstantDeclarator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitConstantDeclarator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitConstantDeclarator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstantDeclaratorContext constantDeclarator() throws RecognitionException {
		ConstantDeclaratorContext _localctx = new ConstantDeclaratorContext(_ctx, getState());
		enterRule(_localctx, 110, RULE_constantDeclarator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1385); id();
			setState(1390);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==LBRACK) {
				{
				{
				setState(1386); match(LBRACK);
				setState(1387); match(RBRACK);
				}
				}
				setState(1392);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1393); match(ASSIGN);
			setState(1394); variableInitializer();
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

	public static class InterfaceMethodDeclarationContext extends ParserRuleContext {
		public QualifiedNameListContext qualifiedNameList() {
			return getRuleContext(QualifiedNameListContext.class,0);
		}
		public FormalParametersContext formalParameters() {
			return getRuleContext(FormalParametersContext.class,0);
		}
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public InterfaceMethodDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interfaceMethodDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterInterfaceMethodDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitInterfaceMethodDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitInterfaceMethodDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InterfaceMethodDeclarationContext interfaceMethodDeclaration() throws RecognitionException {
		InterfaceMethodDeclarationContext _localctx = new InterfaceMethodDeclarationContext(_ctx, getState());
		enterRule(_localctx, 112, RULE_interfaceMethodDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1398);
			switch (_input.LA(1)) {
			case ARGS:
			case AFTER:
			case AROUND:
			case ASPECT:
			case BEFORE:
			case CALL:
			case CFLOW:
			case CFLOWBELOW:
			case DECLARE:
			case ERROR:
			case EXECUTION:
			case GET:
			case HANDLER:
			case INITIALIZATION:
			case ISSINGLETON:
			case PARENTS:
			case PERCFLOW:
			case PERCFLOWBELOW:
			case PERTARGET:
			case PERTHIS:
			case PERTYPEWITHIN:
			case POINTCUT:
			case PRECEDENCE:
			case PREINITIALIZATION:
			case PRIVILEGED:
			case RETURNING:
			case SET:
			case SOFT:
			case STATICINITIALIZATION:
			case TARGET:
			case THROWING:
			case WARNING:
			case WITHIN:
			case WITHINCODE:
			case BOOLEAN:
			case BYTE:
			case CHAR:
			case DOUBLE:
			case FLOAT:
			case INT:
			case LONG:
			case SHORT:
			case Identifier:
				{
				setState(1396); type();
				}
				break;
			case VOID:
				{
				setState(1397); match(VOID);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(1400); id();
			setState(1401); formalParameters();
			setState(1406);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==LBRACK) {
				{
				{
				setState(1402); match(LBRACK);
				setState(1403); match(RBRACK);
				}
				}
				setState(1408);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1411);
			_la = _input.LA(1);
			if (_la==THROWS) {
				{
				setState(1409); match(THROWS);
				setState(1410); qualifiedNameList();
				}
			}

			setState(1413); match(SEMI);
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

	public static class VariableDeclaratorIdContext extends ParserRuleContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public VariableDeclaratorIdContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableDeclaratorId; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterVariableDeclaratorId(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitVariableDeclaratorId(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitVariableDeclaratorId(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableDeclaratorIdContext variableDeclaratorId() throws RecognitionException {
		VariableDeclaratorIdContext _localctx = new VariableDeclaratorIdContext(_ctx, getState());
		enterRule(_localctx, 114, RULE_variableDeclaratorId);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1415); id();
			setState(1420);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==LBRACK) {
				{
				{
				setState(1416); match(LBRACK);
				setState(1417); match(RBRACK);
				}
				}
				setState(1422);
				_errHandler.sync(this);
				_la = _input.LA(1);
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

	public static class EnumConstantNameContext extends ParserRuleContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public EnumConstantNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enumConstantName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterEnumConstantName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitEnumConstantName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitEnumConstantName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EnumConstantNameContext enumConstantName() throws RecognitionException {
		EnumConstantNameContext _localctx = new EnumConstantNameContext(_ctx, getState());
		enterRule(_localctx, 116, RULE_enumConstantName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1423); id();
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

	public static class ClassOrInterfaceTypeContext extends ParserRuleContext {
		public List<TypeArgumentsContext> typeArguments() {
			return getRuleContexts(TypeArgumentsContext.class);
		}
		public IdContext id(int i) {
			return getRuleContext(IdContext.class,i);
		}
		public TypeArgumentsContext typeArguments(int i) {
			return getRuleContext(TypeArgumentsContext.class,i);
		}
		public List<IdContext> id() {
			return getRuleContexts(IdContext.class);
		}
		public ClassOrInterfaceTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_classOrInterfaceType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterClassOrInterfaceType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitClassOrInterfaceType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitClassOrInterfaceType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ClassOrInterfaceTypeContext classOrInterfaceType() throws RecognitionException {
		ClassOrInterfaceTypeContext _localctx = new ClassOrInterfaceTypeContext(_ctx, getState());
		enterRule(_localctx, 118, RULE_classOrInterfaceType);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1425); id();
			setState(1427);
			switch ( getInterpreter().adaptivePredict(_input,137,_ctx) ) {
			case 1:
				{
				setState(1426); typeArguments();
				}
				break;
			}
			setState(1436);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,139,_ctx);
			while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1429); match(DOT);
					setState(1430); id();
					setState(1432);
					switch ( getInterpreter().adaptivePredict(_input,138,_ctx) ) {
					case 1:
						{
						setState(1431); typeArguments();
						}
						break;
					}
					}
					} 
				}
				setState(1438);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,139,_ctx);
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

	public static class QualifiedNameContext extends ParserRuleContext {
		public IdContext id(int i) {
			return getRuleContext(IdContext.class,i);
		}
		public List<IdContext> id() {
			return getRuleContexts(IdContext.class);
		}
		public QualifiedNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_qualifiedName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterQualifiedName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitQualifiedName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitQualifiedName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QualifiedNameContext qualifiedName() throws RecognitionException {
		QualifiedNameContext _localctx = new QualifiedNameContext(_ctx, getState());
		enterRule(_localctx, 120, RULE_qualifiedName);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1439); id();
			setState(1444);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,140,_ctx);
			while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1440); match(DOT);
					setState(1441); id();
					}
					} 
				}
				setState(1446);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,140,_ctx);
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

	public static class ElementValuePairContext extends ParserRuleContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public ElementValueContext elementValue() {
			return getRuleContext(ElementValueContext.class,0);
		}
		public ElementValuePairContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_elementValuePair; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterElementValuePair(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitElementValuePair(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitElementValuePair(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ElementValuePairContext elementValuePair() throws RecognitionException {
		ElementValuePairContext _localctx = new ElementValuePairContext(_ctx, getState());
		enterRule(_localctx, 122, RULE_elementValuePair);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1447); id();
			setState(1448); match(ASSIGN);
			setState(1449); elementValue();
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

	public static class AnnotationTypeDeclarationContext extends ParserRuleContext {
		public AnnotationTypeBodyContext annotationTypeBody() {
			return getRuleContext(AnnotationTypeBodyContext.class,0);
		}
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public AnnotationTypeDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotationTypeDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterAnnotationTypeDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitAnnotationTypeDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitAnnotationTypeDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnnotationTypeDeclarationContext annotationTypeDeclaration() throws RecognitionException {
		AnnotationTypeDeclarationContext _localctx = new AnnotationTypeDeclarationContext(_ctx, getState());
		enterRule(_localctx, 124, RULE_annotationTypeDeclaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1451); match(AT);
			setState(1452); match(INTERFACE);
			setState(1453); id();
			setState(1454); annotationTypeBody();
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

	public static class AnnotationMethodRestContext extends ParserRuleContext {
		public DefaultValueContext defaultValue() {
			return getRuleContext(DefaultValueContext.class,0);
		}
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public AnnotationMethodRestContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotationMethodRest; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterAnnotationMethodRest(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitAnnotationMethodRest(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitAnnotationMethodRest(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnnotationMethodRestContext annotationMethodRest() throws RecognitionException {
		AnnotationMethodRestContext _localctx = new AnnotationMethodRestContext(_ctx, getState());
		enterRule(_localctx, 126, RULE_annotationMethodRest);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1456); id();
			setState(1457); match(LPAREN);
			setState(1458); match(RPAREN);
			setState(1460);
			_la = _input.LA(1);
			if (_la==DEFAULT) {
				{
				setState(1459); defaultValue();
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

	public static class StatementContext extends ParserRuleContext {
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public StatementExpressionContext statementExpression() {
			return getRuleContext(StatementExpressionContext.class,0);
		}
		public StatementContext statement(int i) {
			return getRuleContext(StatementContext.class,i);
		}
		public List<SwitchLabelContext> switchLabel() {
			return getRuleContexts(SwitchLabelContext.class);
		}
		public List<SwitchBlockStatementGroupContext> switchBlockStatementGroup() {
			return getRuleContexts(SwitchBlockStatementGroupContext.class);
		}
		public ParExpressionContext parExpression() {
			return getRuleContext(ParExpressionContext.class,0);
		}
		public List<CatchClauseContext> catchClause() {
			return getRuleContexts(CatchClauseContext.class);
		}
		public CatchClauseContext catchClause(int i) {
			return getRuleContext(CatchClauseContext.class,i);
		}
		public FinallyBlockContext finallyBlock() {
			return getRuleContext(FinallyBlockContext.class,0);
		}
		public SwitchBlockStatementGroupContext switchBlockStatementGroup(int i) {
			return getRuleContext(SwitchBlockStatementGroupContext.class,i);
		}
		public ForControlContext forControl() {
			return getRuleContext(ForControlContext.class,0);
		}
		public TerminalNode ASSERT() { return getToken(AspectJParser.ASSERT, 0); }
		public ResourceSpecificationContext resourceSpecification() {
			return getRuleContext(ResourceSpecificationContext.class,0);
		}
		public List<StatementContext> statement() {
			return getRuleContexts(StatementContext.class);
		}
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public SwitchLabelContext switchLabel(int i) {
			return getRuleContext(SwitchLabelContext.class,i);
		}
		public StatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_statement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StatementContext statement() throws RecognitionException {
		StatementContext _localctx = new StatementContext(_ctx, getState());
		enterRule(_localctx, 128, RULE_statement);
		int _la;
		try {
			int _alt;
			setState(1567);
			switch ( getInterpreter().adaptivePredict(_input,154,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1462); block();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1463); match(ASSERT);
				setState(1464); expression(0);
				setState(1467);
				_la = _input.LA(1);
				if (_la==COLON) {
					{
					setState(1465); match(COLON);
					setState(1466); expression(0);
					}
				}

				setState(1469); match(SEMI);
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1471); match(IF);
				setState(1472); parExpression();
				setState(1473); statement();
				setState(1476);
				switch ( getInterpreter().adaptivePredict(_input,143,_ctx) ) {
				case 1:
					{
					setState(1474); match(ELSE);
					setState(1475); statement();
					}
					break;
				}
				}
				break;

			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1478); match(FOR);
				setState(1479); match(LPAREN);
				setState(1480); forControl();
				setState(1481); match(RPAREN);
				setState(1482); statement();
				}
				break;

			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(1484); match(WHILE);
				setState(1485); parExpression();
				setState(1486); statement();
				}
				break;

			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(1488); match(DO);
				setState(1489); statement();
				setState(1490); match(WHILE);
				setState(1491); parExpression();
				setState(1492); match(SEMI);
				}
				break;

			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(1494); match(TRY);
				setState(1495); block();
				setState(1505);
				switch (_input.LA(1)) {
				case CATCH:
					{
					setState(1497); 
					_errHandler.sync(this);
					_la = _input.LA(1);
					do {
						{
						{
						setState(1496); catchClause();
						}
						}
						setState(1499); 
						_errHandler.sync(this);
						_la = _input.LA(1);
					} while ( _la==CATCH );
					setState(1502);
					_la = _input.LA(1);
					if (_la==FINALLY) {
						{
						setState(1501); finallyBlock();
						}
					}

					}
					break;
				case FINALLY:
					{
					setState(1504); finallyBlock();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				break;

			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(1507); match(TRY);
				setState(1508); resourceSpecification();
				setState(1509); block();
				setState(1513);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==CATCH) {
					{
					{
					setState(1510); catchClause();
					}
					}
					setState(1515);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(1517);
				_la = _input.LA(1);
				if (_la==FINALLY) {
					{
					setState(1516); finallyBlock();
					}
				}

				}
				break;

			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(1519); match(SWITCH);
				setState(1520); parExpression();
				setState(1521); match(LBRACE);
				setState(1525);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,149,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1522); switchBlockStatementGroup();
						}
						} 
					}
					setState(1527);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,149,_ctx);
				}
				setState(1531);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==CASE || _la==DEFAULT) {
					{
					{
					setState(1528); switchLabel();
					}
					}
					setState(1533);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(1534); match(RBRACE);
				}
				break;

			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(1536); match(SYNCHRONIZED);
				setState(1537); parExpression();
				setState(1538); block();
				}
				break;

			case 11:
				enterOuterAlt(_localctx, 11);
				{
				setState(1540); match(RETURN);
				setState(1542);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ARGS) | (1L << AFTER) | (1L << AROUND) | (1L << ASPECT) | (1L << BEFORE) | (1L << CALL) | (1L << CFLOW) | (1L << CFLOWBELOW) | (1L << DECLARE) | (1L << ERROR) | (1L << EXECUTION) | (1L << GET) | (1L << HANDLER) | (1L << INITIALIZATION) | (1L << ISSINGLETON) | (1L << PARENTS) | (1L << PERCFLOW) | (1L << PERCFLOWBELOW) | (1L << PERTARGET) | (1L << PERTHIS) | (1L << PERTYPEWITHIN) | (1L << POINTCUT) | (1L << PRECEDENCE) | (1L << PREINITIALIZATION) | (1L << PRIVILEGED) | (1L << RETURNING) | (1L << SET) | (1L << SOFT) | (1L << STATICINITIALIZATION) | (1L << TARGET) | (1L << THROWING) | (1L << WARNING) | (1L << WITHIN) | (1L << WITHINCODE) | (1L << BOOLEAN) | (1L << BYTE))) != 0) || ((((_la - 66)) & ~0x3f) == 0 && ((1L << (_la - 66)) & ((1L << (CHAR - 66)) | (1L << (DOUBLE - 66)) | (1L << (FLOAT - 66)) | (1L << (INT - 66)) | (1L << (LONG - 66)) | (1L << (NEW - 66)) | (1L << (SHORT - 66)) | (1L << (SUPER - 66)) | (1L << (THIS - 66)) | (1L << (VOID - 66)) | (1L << (IntegerLiteral - 66)) | (1L << (FloatingPointLiteral - 66)) | (1L << (BooleanLiteral - 66)) | (1L << (CharacterLiteral - 66)) | (1L << (StringLiteral - 66)) | (1L << (NullLiteral - 66)) | (1L << (LPAREN - 66)) | (1L << (LT - 66)) | (1L << (BANG - 66)) | (1L << (TILDE - 66)))) != 0) || ((((_la - 137)) & ~0x3f) == 0 && ((1L << (_la - 137)) & ((1L << (INC - 137)) | (1L << (DEC - 137)) | (1L << (ADD - 137)) | (1L << (SUB - 137)) | (1L << (Identifier - 137)))) != 0)) {
					{
					setState(1541); expression(0);
					}
				}

				setState(1544); match(SEMI);
				}
				break;

			case 12:
				enterOuterAlt(_localctx, 12);
				{
				setState(1545); match(THROW);
				setState(1546); expression(0);
				setState(1547); match(SEMI);
				}
				break;

			case 13:
				enterOuterAlt(_localctx, 13);
				{
				setState(1549); match(BREAK);
				setState(1551);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ARGS) | (1L << AFTER) | (1L << AROUND) | (1L << ASPECT) | (1L << BEFORE) | (1L << CALL) | (1L << CFLOW) | (1L << CFLOWBELOW) | (1L << DECLARE) | (1L << ERROR) | (1L << EXECUTION) | (1L << GET) | (1L << HANDLER) | (1L << INITIALIZATION) | (1L << ISSINGLETON) | (1L << PARENTS) | (1L << PERCFLOW) | (1L << PERCFLOWBELOW) | (1L << PERTARGET) | (1L << PERTHIS) | (1L << PERTYPEWITHIN) | (1L << POINTCUT) | (1L << PRECEDENCE) | (1L << PREINITIALIZATION) | (1L << PRIVILEGED) | (1L << RETURNING) | (1L << SET) | (1L << SOFT) | (1L << STATICINITIALIZATION) | (1L << TARGET) | (1L << THROWING) | (1L << WARNING) | (1L << WITHIN) | (1L << WITHINCODE))) != 0) || _la==Identifier) {
					{
					setState(1550); id();
					}
				}

				setState(1553); match(SEMI);
				}
				break;

			case 14:
				enterOuterAlt(_localctx, 14);
				{
				setState(1554); match(CONTINUE);
				setState(1556);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ARGS) | (1L << AFTER) | (1L << AROUND) | (1L << ASPECT) | (1L << BEFORE) | (1L << CALL) | (1L << CFLOW) | (1L << CFLOWBELOW) | (1L << DECLARE) | (1L << ERROR) | (1L << EXECUTION) | (1L << GET) | (1L << HANDLER) | (1L << INITIALIZATION) | (1L << ISSINGLETON) | (1L << PARENTS) | (1L << PERCFLOW) | (1L << PERCFLOWBELOW) | (1L << PERTARGET) | (1L << PERTHIS) | (1L << PERTYPEWITHIN) | (1L << POINTCUT) | (1L << PRECEDENCE) | (1L << PREINITIALIZATION) | (1L << PRIVILEGED) | (1L << RETURNING) | (1L << SET) | (1L << SOFT) | (1L << STATICINITIALIZATION) | (1L << TARGET) | (1L << THROWING) | (1L << WARNING) | (1L << WITHIN) | (1L << WITHINCODE))) != 0) || _la==Identifier) {
					{
					setState(1555); id();
					}
				}

				setState(1558); match(SEMI);
				}
				break;

			case 15:
				enterOuterAlt(_localctx, 15);
				{
				setState(1559); match(SEMI);
				}
				break;

			case 16:
				enterOuterAlt(_localctx, 16);
				{
				setState(1560); statementExpression();
				setState(1561); match(SEMI);
				}
				break;

			case 17:
				enterOuterAlt(_localctx, 17);
				{
				setState(1563); id();
				setState(1564); match(COLON);
				setState(1565); statement();
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

	public static class CatchClauseContext extends ParserRuleContext {
		public CatchTypeContext catchType() {
			return getRuleContext(CatchTypeContext.class,0);
		}
		public VariableModifierContext variableModifier(int i) {
			return getRuleContext(VariableModifierContext.class,i);
		}
		public List<VariableModifierContext> variableModifier() {
			return getRuleContexts(VariableModifierContext.class);
		}
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public CatchClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_catchClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterCatchClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitCatchClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitCatchClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CatchClauseContext catchClause() throws RecognitionException {
		CatchClauseContext _localctx = new CatchClauseContext(_ctx, getState());
		enterRule(_localctx, 130, RULE_catchClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1569); match(CATCH);
			setState(1570); match(LPAREN);
			setState(1574);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==FINAL) {
				{
				{
				setState(1571); variableModifier();
				}
				}
				setState(1576);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1577); catchType();
			setState(1578); id();
			setState(1579); match(RPAREN);
			setState(1580); block();
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

	public static class ExpressionContext extends ParserRuleContext {
		public NonWildcardTypeArgumentsContext nonWildcardTypeArguments() {
			return getRuleContext(NonWildcardTypeArgumentsContext.class,0);
		}
		public ExplicitGenericInvocationContext explicitGenericInvocation() {
			return getRuleContext(ExplicitGenericInvocationContext.class,0);
		}
		public ExpressionListContext expressionList() {
			return getRuleContext(ExpressionListContext.class,0);
		}
		public InnerCreatorContext innerCreator() {
			return getRuleContext(InnerCreatorContext.class,0);
		}
		public SuperSuffixContext superSuffix() {
			return getRuleContext(SuperSuffixContext.class,0);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public PrimaryContext primary() {
			return getRuleContext(PrimaryContext.class,0);
		}
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public CreatorContext creator() {
			return getRuleContext(CreatorContext.class,0);
		}
		public ExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionContext expression() throws RecognitionException {
		return expression(0);
	}

	private ExpressionContext expression(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ExpressionContext _localctx = new ExpressionContext(_ctx, _parentState);
		ExpressionContext _prevctx = _localctx;
		int _startState = 132;
		enterRecursionRule(_localctx, 132, RULE_expression, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1595);
			switch ( getInterpreter().adaptivePredict(_input,156,_ctx) ) {
			case 1:
				{
				setState(1583); match(LPAREN);
				setState(1584); type();
				setState(1585); match(RPAREN);
				setState(1586); expression(17);
				}
				break;

			case 2:
				{
				setState(1588);
				_la = _input.LA(1);
				if ( !(((((_la - 137)) & ~0x3f) == 0 && ((1L << (_la - 137)) & ((1L << (INC - 137)) | (1L << (DEC - 137)) | (1L << (ADD - 137)) | (1L << (SUB - 137)))) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				consume();
				setState(1589); expression(15);
				}
				break;

			case 3:
				{
				setState(1590);
				_la = _input.LA(1);
				if ( !(_la==BANG || _la==TILDE) ) {
				_errHandler.recoverInline(this);
				}
				consume();
				setState(1591); expression(14);
				}
				break;

			case 4:
				{
				setState(1592); primary();
				}
				break;

			case 5:
				{
				setState(1593); match(NEW);
				setState(1594); creator();
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(1682);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,161,_ctx);
			while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(1680);
					switch ( getInterpreter().adaptivePredict(_input,160,_ctx) ) {
					case 1:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1597);
						if (!(precpred(_ctx, 13))) throw new FailedPredicateException(this, "precpred(_ctx, 13)");
						setState(1598);
						_la = _input.LA(1);
						if ( !(((((_la - 141)) & ~0x3f) == 0 && ((1L << (_la - 141)) & ((1L << (MUL - 141)) | (1L << (DIV - 141)) | (1L << (MOD - 141)))) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(1599); expression(14);
						}
						break;

					case 2:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1600);
						if (!(precpred(_ctx, 12))) throw new FailedPredicateException(this, "precpred(_ctx, 12)");
						setState(1601);
						_la = _input.LA(1);
						if ( !(_la==ADD || _la==SUB) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(1602); expression(13);
						}
						break;

					case 3:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1603);
						if (!(precpred(_ctx, 11))) throw new FailedPredicateException(this, "precpred(_ctx, 11)");
						setState(1611);
						switch ( getInterpreter().adaptivePredict(_input,157,_ctx) ) {
						case 1:
							{
							setState(1604); match(LT);
							setState(1605); match(LT);
							}
							break;

						case 2:
							{
							setState(1606); match(GT);
							setState(1607); match(GT);
							setState(1608); match(GT);
							}
							break;

						case 3:
							{
							setState(1609); match(GT);
							setState(1610); match(GT);
							}
							break;
						}
						setState(1613); expression(12);
						}
						break;

					case 4:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1614);
						if (!(precpred(_ctx, 10))) throw new FailedPredicateException(this, "precpred(_ctx, 10)");
						setState(1615);
						_la = _input.LA(1);
						if ( !(((((_la - 125)) & ~0x3f) == 0 && ((1L << (_la - 125)) & ((1L << (GT - 125)) | (1L << (LT - 125)) | (1L << (LE - 125)) | (1L << (GE - 125)))) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(1616); expression(11);
						}
						break;

					case 5:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1617);
						if (!(precpred(_ctx, 8))) throw new FailedPredicateException(this, "precpred(_ctx, 8)");
						setState(1618);
						_la = _input.LA(1);
						if ( !(_la==EQUAL || _la==NOTEQUAL) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(1619); expression(9);
						}
						break;

					case 6:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1620);
						if (!(precpred(_ctx, 7))) throw new FailedPredicateException(this, "precpred(_ctx, 7)");
						setState(1621); match(BITAND);
						setState(1622); expression(8);
						}
						break;

					case 7:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1623);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(1624); match(CARET);
						setState(1625); expression(7);
						}
						break;

					case 8:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1626);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(1627); match(BITOR);
						setState(1628); expression(6);
						}
						break;

					case 9:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1629);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(1630); match(AND);
						setState(1631); expression(5);
						}
						break;

					case 10:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1632);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(1633); match(OR);
						setState(1634); expression(4);
						}
						break;

					case 11:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1635);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(1636); match(QUESTION);
						setState(1637); expression(0);
						setState(1638); match(COLON);
						setState(1639); expression(3);
						}
						break;

					case 12:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1641);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(1642);
						_la = _input.LA(1);
						if ( !(((((_la - 124)) & ~0x3f) == 0 && ((1L << (_la - 124)) & ((1L << (ASSIGN - 124)) | (1L << (ADD_ASSIGN - 124)) | (1L << (SUB_ASSIGN - 124)) | (1L << (MUL_ASSIGN - 124)) | (1L << (DIV_ASSIGN - 124)) | (1L << (AND_ASSIGN - 124)) | (1L << (OR_ASSIGN - 124)) | (1L << (XOR_ASSIGN - 124)) | (1L << (MOD_ASSIGN - 124)) | (1L << (LSHIFT_ASSIGN - 124)) | (1L << (RSHIFT_ASSIGN - 124)) | (1L << (URSHIFT_ASSIGN - 124)))) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(1643); expression(2);
						}
						break;

					case 13:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1644);
						if (!(precpred(_ctx, 25))) throw new FailedPredicateException(this, "precpred(_ctx, 25)");
						setState(1645); match(DOT);
						setState(1646); id();
						}
						break;

					case 14:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1647);
						if (!(precpred(_ctx, 24))) throw new FailedPredicateException(this, "precpred(_ctx, 24)");
						setState(1648); match(DOT);
						setState(1649); match(THIS);
						}
						break;

					case 15:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1650);
						if (!(precpred(_ctx, 23))) throw new FailedPredicateException(this, "precpred(_ctx, 23)");
						setState(1651); match(DOT);
						setState(1652); match(NEW);
						setState(1654);
						_la = _input.LA(1);
						if (_la==LT) {
							{
							setState(1653); nonWildcardTypeArguments();
							}
						}

						setState(1656); innerCreator();
						}
						break;

					case 16:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1657);
						if (!(precpred(_ctx, 22))) throw new FailedPredicateException(this, "precpred(_ctx, 22)");
						setState(1658); match(DOT);
						setState(1659); match(SUPER);
						setState(1660); superSuffix();
						}
						break;

					case 17:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1661);
						if (!(precpred(_ctx, 21))) throw new FailedPredicateException(this, "precpred(_ctx, 21)");
						setState(1662); match(DOT);
						setState(1663); explicitGenericInvocation();
						}
						break;

					case 18:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1664);
						if (!(precpred(_ctx, 20))) throw new FailedPredicateException(this, "precpred(_ctx, 20)");
						setState(1665); match(LBRACK);
						setState(1666); expression(0);
						setState(1667); match(RBRACK);
						}
						break;

					case 19:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1669);
						if (!(precpred(_ctx, 19))) throw new FailedPredicateException(this, "precpred(_ctx, 19)");
						setState(1670); match(LPAREN);
						setState(1672);
						_la = _input.LA(1);
						if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ARGS) | (1L << AFTER) | (1L << AROUND) | (1L << ASPECT) | (1L << BEFORE) | (1L << CALL) | (1L << CFLOW) | (1L << CFLOWBELOW) | (1L << DECLARE) | (1L << ERROR) | (1L << EXECUTION) | (1L << GET) | (1L << HANDLER) | (1L << INITIALIZATION) | (1L << ISSINGLETON) | (1L << PARENTS) | (1L << PERCFLOW) | (1L << PERCFLOWBELOW) | (1L << PERTARGET) | (1L << PERTHIS) | (1L << PERTYPEWITHIN) | (1L << POINTCUT) | (1L << PRECEDENCE) | (1L << PREINITIALIZATION) | (1L << PRIVILEGED) | (1L << RETURNING) | (1L << SET) | (1L << SOFT) | (1L << STATICINITIALIZATION) | (1L << TARGET) | (1L << THROWING) | (1L << WARNING) | (1L << WITHIN) | (1L << WITHINCODE) | (1L << BOOLEAN) | (1L << BYTE))) != 0) || ((((_la - 66)) & ~0x3f) == 0 && ((1L << (_la - 66)) & ((1L << (CHAR - 66)) | (1L << (DOUBLE - 66)) | (1L << (FLOAT - 66)) | (1L << (INT - 66)) | (1L << (LONG - 66)) | (1L << (NEW - 66)) | (1L << (SHORT - 66)) | (1L << (SUPER - 66)) | (1L << (THIS - 66)) | (1L << (VOID - 66)) | (1L << (IntegerLiteral - 66)) | (1L << (FloatingPointLiteral - 66)) | (1L << (BooleanLiteral - 66)) | (1L << (CharacterLiteral - 66)) | (1L << (StringLiteral - 66)) | (1L << (NullLiteral - 66)) | (1L << (LPAREN - 66)) | (1L << (LT - 66)) | (1L << (BANG - 66)) | (1L << (TILDE - 66)))) != 0) || ((((_la - 137)) & ~0x3f) == 0 && ((1L << (_la - 137)) & ((1L << (INC - 137)) | (1L << (DEC - 137)) | (1L << (ADD - 137)) | (1L << (SUB - 137)) | (1L << (Identifier - 137)))) != 0)) {
							{
							setState(1671); expressionList();
							}
						}

						setState(1674); match(RPAREN);
						}
						break;

					case 20:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1675);
						if (!(precpred(_ctx, 16))) throw new FailedPredicateException(this, "precpred(_ctx, 16)");
						setState(1676);
						_la = _input.LA(1);
						if ( !(_la==INC || _la==DEC) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						}
						break;

					case 21:
						{
						_localctx = new ExpressionContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_expression);
						setState(1677);
						if (!(precpred(_ctx, 9))) throw new FailedPredicateException(this, "precpred(_ctx, 9)");
						setState(1678); match(INSTANCEOF);
						setState(1679); type();
						}
						break;
					}
					} 
				}
				setState(1684);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,161,_ctx);
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

	public static class PrimaryContext extends ParserRuleContext {
		public NonWildcardTypeArgumentsContext nonWildcardTypeArguments() {
			return getRuleContext(NonWildcardTypeArgumentsContext.class,0);
		}
		public ExplicitGenericInvocationSuffixContext explicitGenericInvocationSuffix() {
			return getRuleContext(ExplicitGenericInvocationSuffixContext.class,0);
		}
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public ArgumentsContext arguments() {
			return getRuleContext(ArgumentsContext.class,0);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public PrimaryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_primary; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterPrimary(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitPrimary(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitPrimary(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PrimaryContext primary() throws RecognitionException {
		PrimaryContext _localctx = new PrimaryContext(_ctx, getState());
		enterRule(_localctx, 134, RULE_primary);
		try {
			setState(1706);
			switch ( getInterpreter().adaptivePredict(_input,163,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1685); match(LPAREN);
				setState(1686); expression(0);
				setState(1687); match(RPAREN);
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1689); match(THIS);
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1690); match(SUPER);
				}
				break;

			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1691); literal();
				}
				break;

			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(1692); id();
				}
				break;

			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(1693); type();
				setState(1694); match(DOT);
				setState(1695); match(CLASS);
				}
				break;

			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(1697); match(VOID);
				setState(1698); match(DOT);
				setState(1699); match(CLASS);
				}
				break;

			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(1700); nonWildcardTypeArguments();
				setState(1704);
				switch (_input.LA(1)) {
				case ARGS:
				case AFTER:
				case AROUND:
				case ASPECT:
				case BEFORE:
				case CALL:
				case CFLOW:
				case CFLOWBELOW:
				case DECLARE:
				case ERROR:
				case EXECUTION:
				case GET:
				case HANDLER:
				case INITIALIZATION:
				case ISSINGLETON:
				case PARENTS:
				case PERCFLOW:
				case PERCFLOWBELOW:
				case PERTARGET:
				case PERTHIS:
				case PERTYPEWITHIN:
				case POINTCUT:
				case PRECEDENCE:
				case PREINITIALIZATION:
				case PRIVILEGED:
				case RETURNING:
				case SET:
				case SOFT:
				case STATICINITIALIZATION:
				case TARGET:
				case THROWING:
				case WARNING:
				case WITHIN:
				case WITHINCODE:
				case SUPER:
				case Identifier:
					{
					setState(1701); explicitGenericInvocationSuffix();
					}
					break;
				case THIS:
					{
					setState(1702); match(THIS);
					setState(1703); arguments();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
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

	public static class CreatedNameContext extends ParserRuleContext {
		public List<TypeArgumentsOrDiamondContext> typeArgumentsOrDiamond() {
			return getRuleContexts(TypeArgumentsOrDiamondContext.class);
		}
		public IdContext id(int i) {
			return getRuleContext(IdContext.class,i);
		}
		public PrimitiveTypeContext primitiveType() {
			return getRuleContext(PrimitiveTypeContext.class,0);
		}
		public List<IdContext> id() {
			return getRuleContexts(IdContext.class);
		}
		public TypeArgumentsOrDiamondContext typeArgumentsOrDiamond(int i) {
			return getRuleContext(TypeArgumentsOrDiamondContext.class,i);
		}
		public CreatedNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_createdName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterCreatedName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitCreatedName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitCreatedName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CreatedNameContext createdName() throws RecognitionException {
		CreatedNameContext _localctx = new CreatedNameContext(_ctx, getState());
		enterRule(_localctx, 136, RULE_createdName);
		int _la;
		try {
			setState(1723);
			switch (_input.LA(1)) {
			case ARGS:
			case AFTER:
			case AROUND:
			case ASPECT:
			case BEFORE:
			case CALL:
			case CFLOW:
			case CFLOWBELOW:
			case DECLARE:
			case ERROR:
			case EXECUTION:
			case GET:
			case HANDLER:
			case INITIALIZATION:
			case ISSINGLETON:
			case PARENTS:
			case PERCFLOW:
			case PERCFLOWBELOW:
			case PERTARGET:
			case PERTHIS:
			case PERTYPEWITHIN:
			case POINTCUT:
			case PRECEDENCE:
			case PREINITIALIZATION:
			case PRIVILEGED:
			case RETURNING:
			case SET:
			case SOFT:
			case STATICINITIALIZATION:
			case TARGET:
			case THROWING:
			case WARNING:
			case WITHIN:
			case WITHINCODE:
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(1708); id();
				setState(1710);
				_la = _input.LA(1);
				if (_la==LT) {
					{
					setState(1709); typeArgumentsOrDiamond();
					}
				}

				setState(1719);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==DOT) {
					{
					{
					setState(1712); match(DOT);
					setState(1713); id();
					setState(1715);
					_la = _input.LA(1);
					if (_la==LT) {
						{
						setState(1714); typeArgumentsOrDiamond();
						}
					}

					}
					}
					setState(1721);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case BOOLEAN:
			case BYTE:
			case CHAR:
			case DOUBLE:
			case FLOAT:
			case INT:
			case LONG:
			case SHORT:
				enterOuterAlt(_localctx, 2);
				{
				setState(1722); primitiveType();
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

	public static class InnerCreatorContext extends ParserRuleContext {
		public ClassCreatorRestContext classCreatorRest() {
			return getRuleContext(ClassCreatorRestContext.class,0);
		}
		public NonWildcardTypeArgumentsOrDiamondContext nonWildcardTypeArgumentsOrDiamond() {
			return getRuleContext(NonWildcardTypeArgumentsOrDiamondContext.class,0);
		}
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public InnerCreatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_innerCreator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterInnerCreator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitInnerCreator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitInnerCreator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InnerCreatorContext innerCreator() throws RecognitionException {
		InnerCreatorContext _localctx = new InnerCreatorContext(_ctx, getState());
		enterRule(_localctx, 138, RULE_innerCreator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1725); id();
			setState(1727);
			_la = _input.LA(1);
			if (_la==LT) {
				{
				setState(1726); nonWildcardTypeArgumentsOrDiamond();
				}
			}

			setState(1729); classCreatorRest();
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

	public static class SuperSuffixContext extends ParserRuleContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public ArgumentsContext arguments() {
			return getRuleContext(ArgumentsContext.class,0);
		}
		public SuperSuffixContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_superSuffix; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterSuperSuffix(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitSuperSuffix(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitSuperSuffix(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SuperSuffixContext superSuffix() throws RecognitionException {
		SuperSuffixContext _localctx = new SuperSuffixContext(_ctx, getState());
		enterRule(_localctx, 140, RULE_superSuffix);
		try {
			setState(1737);
			switch (_input.LA(1)) {
			case LPAREN:
				enterOuterAlt(_localctx, 1);
				{
				setState(1731); arguments();
				}
				break;
			case DOT:
				enterOuterAlt(_localctx, 2);
				{
				setState(1732); match(DOT);
				setState(1733); id();
				setState(1735);
				switch ( getInterpreter().adaptivePredict(_input,169,_ctx) ) {
				case 1:
					{
					setState(1734); arguments();
					}
					break;
				}
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

	public static class ExplicitGenericInvocationSuffixContext extends ParserRuleContext {
		public SuperSuffixContext superSuffix() {
			return getRuleContext(SuperSuffixContext.class,0);
		}
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public ArgumentsContext arguments() {
			return getRuleContext(ArgumentsContext.class,0);
		}
		public ExplicitGenericInvocationSuffixContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_explicitGenericInvocationSuffix; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterExplicitGenericInvocationSuffix(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitExplicitGenericInvocationSuffix(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitExplicitGenericInvocationSuffix(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExplicitGenericInvocationSuffixContext explicitGenericInvocationSuffix() throws RecognitionException {
		ExplicitGenericInvocationSuffixContext _localctx = new ExplicitGenericInvocationSuffixContext(_ctx, getState());
		enterRule(_localctx, 142, RULE_explicitGenericInvocationSuffix);
		try {
			setState(1744);
			switch (_input.LA(1)) {
			case SUPER:
				enterOuterAlt(_localctx, 1);
				{
				setState(1739); match(SUPER);
				setState(1740); superSuffix();
				}
				break;
			case ARGS:
			case AFTER:
			case AROUND:
			case ASPECT:
			case BEFORE:
			case CALL:
			case CFLOW:
			case CFLOWBELOW:
			case DECLARE:
			case ERROR:
			case EXECUTION:
			case GET:
			case HANDLER:
			case INITIALIZATION:
			case ISSINGLETON:
			case PARENTS:
			case PERCFLOW:
			case PERCFLOWBELOW:
			case PERTARGET:
			case PERTHIS:
			case PERTYPEWITHIN:
			case POINTCUT:
			case PRECEDENCE:
			case PREINITIALIZATION:
			case PRIVILEGED:
			case RETURNING:
			case SET:
			case SOFT:
			case STATICINITIALIZATION:
			case TARGET:
			case THROWING:
			case WARNING:
			case WITHIN:
			case WITHINCODE:
			case Identifier:
				enterOuterAlt(_localctx, 2);
				{
				setState(1741); id();
				setState(1742); arguments();
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

	public static class CompilationUnitContext extends ParserRuleContext {
		public TypeDeclarationContext typeDeclaration(int i) {
			return getRuleContext(TypeDeclarationContext.class,i);
		}
		public ImportDeclarationContext importDeclaration(int i) {
			return getRuleContext(ImportDeclarationContext.class,i);
		}
		public List<ImportDeclarationContext> importDeclaration() {
			return getRuleContexts(ImportDeclarationContext.class);
		}
		public TerminalNode EOF() { return getToken(AspectJParser.EOF, 0); }
		public PackageDeclarationContext packageDeclaration() {
			return getRuleContext(PackageDeclarationContext.class,0);
		}
		public List<TypeDeclarationContext> typeDeclaration() {
			return getRuleContexts(TypeDeclarationContext.class);
		}
		public CompilationUnitContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_compilationUnit; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterCompilationUnit(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitCompilationUnit(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitCompilationUnit(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CompilationUnitContext compilationUnit() throws RecognitionException {
		CompilationUnitContext _localctx = new CompilationUnitContext(_ctx, getState());
		enterRule(_localctx, 144, RULE_compilationUnit);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1747);
			switch ( getInterpreter().adaptivePredict(_input,172,_ctx) ) {
			case 1:
				{
				setState(1746); packageDeclaration();
				}
				break;
			}
			setState(1752);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==IMPORT) {
				{
				{
				setState(1749); importDeclaration();
				}
				}
				setState(1754);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1758);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ASPECT) | (1L << PRIVILEGED) | (1L << AT) | (1L << ABSTRACT))) != 0) || ((((_la - 67)) & ~0x3f) == 0 && ((1L << (_la - 67)) & ((1L << (CLASS - 67)) | (1L << (ENUM - 67)) | (1L << (FINAL - 67)) | (1L << (INTERFACE - 67)) | (1L << (NATIVE - 67)) | (1L << (PRIVATE - 67)) | (1L << (PROTECTED - 67)) | (1L << (PUBLIC - 67)) | (1L << (STATIC - 67)) | (1L << (STRICTFP - 67)) | (1L << (SYNCHRONIZED - 67)) | (1L << (TRANSIENT - 67)) | (1L << (VOLATILE - 67)) | (1L << (SEMI - 67)))) != 0)) {
				{
				{
				setState(1755); typeDeclaration();
				}
				}
				setState(1760);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1761); match(EOF);
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

	public static class PackageDeclarationContext extends ParserRuleContext {
		public List<AnnotationContext> annotation() {
			return getRuleContexts(AnnotationContext.class);
		}
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public AnnotationContext annotation(int i) {
			return getRuleContext(AnnotationContext.class,i);
		}
		public PackageDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_packageDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterPackageDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitPackageDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitPackageDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PackageDeclarationContext packageDeclaration() throws RecognitionException {
		PackageDeclarationContext _localctx = new PackageDeclarationContext(_ctx, getState());
		enterRule(_localctx, 146, RULE_packageDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1766);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT) {
				{
				{
				setState(1763); annotation();
				}
				}
				setState(1768);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1769); match(PACKAGE);
			setState(1770); qualifiedName();
			setState(1771); match(SEMI);
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

	public static class ImportDeclarationContext extends ParserRuleContext {
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public ImportDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_importDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterImportDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitImportDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitImportDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ImportDeclarationContext importDeclaration() throws RecognitionException {
		ImportDeclarationContext _localctx = new ImportDeclarationContext(_ctx, getState());
		enterRule(_localctx, 148, RULE_importDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1773); match(IMPORT);
			setState(1775);
			_la = _input.LA(1);
			if (_la==STATIC) {
				{
				setState(1774); match(STATIC);
				}
			}

			setState(1777); qualifiedName();
			setState(1780);
			_la = _input.LA(1);
			if (_la==DOT) {
				{
				setState(1778); match(DOT);
				setState(1779); match(MUL);
				}
			}

			setState(1782); match(SEMI);
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

	public static class ModifierContext extends ParserRuleContext {
		public ClassOrInterfaceModifierContext classOrInterfaceModifier() {
			return getRuleContext(ClassOrInterfaceModifierContext.class,0);
		}
		public ModifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_modifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterModifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitModifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitModifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ModifierContext modifier() throws RecognitionException {
		ModifierContext _localctx = new ModifierContext(_ctx, getState());
		enterRule(_localctx, 150, RULE_modifier);
		int _la;
		try {
			setState(1786);
			switch (_input.LA(1)) {
			case AT:
			case ABSTRACT:
			case FINAL:
			case PRIVATE:
			case PROTECTED:
			case PUBLIC:
			case STATIC:
			case STRICTFP:
				enterOuterAlt(_localctx, 1);
				{
				setState(1784); classOrInterfaceModifier();
				}
				break;
			case NATIVE:
			case SYNCHRONIZED:
			case TRANSIENT:
			case VOLATILE:
				enterOuterAlt(_localctx, 2);
				{
				setState(1785);
				_la = _input.LA(1);
				if ( !(((((_la - 88)) & ~0x3f) == 0 && ((1L << (_la - 88)) & ((1L << (NATIVE - 88)) | (1L << (SYNCHRONIZED - 88)) | (1L << (TRANSIENT - 88)) | (1L << (VOLATILE - 88)))) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				consume();
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

	public static class ClassOrInterfaceModifierContext extends ParserRuleContext {
		public AnnotationContext annotation() {
			return getRuleContext(AnnotationContext.class,0);
		}
		public ClassOrInterfaceModifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_classOrInterfaceModifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterClassOrInterfaceModifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitClassOrInterfaceModifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitClassOrInterfaceModifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ClassOrInterfaceModifierContext classOrInterfaceModifier() throws RecognitionException {
		ClassOrInterfaceModifierContext _localctx = new ClassOrInterfaceModifierContext(_ctx, getState());
		enterRule(_localctx, 152, RULE_classOrInterfaceModifier);
		int _la;
		try {
			setState(1790);
			switch (_input.LA(1)) {
			case AT:
				enterOuterAlt(_localctx, 1);
				{
				setState(1788); annotation();
				}
				break;
			case ABSTRACT:
			case FINAL:
			case PRIVATE:
			case PROTECTED:
			case PUBLIC:
			case STATIC:
			case STRICTFP:
				enterOuterAlt(_localctx, 2);
				{
				setState(1789);
				_la = _input.LA(1);
				if ( !(((((_la - 59)) & ~0x3f) == 0 && ((1L << (_la - 59)) & ((1L << (ABSTRACT - 59)) | (1L << (FINAL - 59)) | (1L << (PRIVATE - 59)) | (1L << (PROTECTED - 59)) | (1L << (PUBLIC - 59)) | (1L << (STATIC - 59)) | (1L << (STRICTFP - 59)))) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				consume();
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

	public static class VariableModifierContext extends ParserRuleContext {
		public AnnotationContext annotation() {
			return getRuleContext(AnnotationContext.class,0);
		}
		public VariableModifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableModifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterVariableModifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitVariableModifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitVariableModifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableModifierContext variableModifier() throws RecognitionException {
		VariableModifierContext _localctx = new VariableModifierContext(_ctx, getState());
		enterRule(_localctx, 154, RULE_variableModifier);
		try {
			setState(1794);
			switch (_input.LA(1)) {
			case FINAL:
				enterOuterAlt(_localctx, 1);
				{
				setState(1792); match(FINAL);
				}
				break;
			case AT:
				enterOuterAlt(_localctx, 2);
				{
				setState(1793); annotation();
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

	public static class TypeParametersContext extends ParserRuleContext {
		public List<TypeParameterContext> typeParameter() {
			return getRuleContexts(TypeParameterContext.class);
		}
		public TypeParameterContext typeParameter(int i) {
			return getRuleContext(TypeParameterContext.class,i);
		}
		public TypeParametersContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeParameters; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterTypeParameters(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitTypeParameters(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitTypeParameters(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeParametersContext typeParameters() throws RecognitionException {
		TypeParametersContext _localctx = new TypeParametersContext(_ctx, getState());
		enterRule(_localctx, 156, RULE_typeParameters);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1796); match(LT);
			setState(1797); typeParameter();
			setState(1802);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1798); match(COMMA);
				setState(1799); typeParameter();
				}
				}
				setState(1804);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1805); match(GT);
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

	public static class TypeBoundContext extends ParserRuleContext {
		public TypeContext type(int i) {
			return getRuleContext(TypeContext.class,i);
		}
		public List<TypeContext> type() {
			return getRuleContexts(TypeContext.class);
		}
		public TypeBoundContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeBound; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterTypeBound(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitTypeBound(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitTypeBound(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeBoundContext typeBound() throws RecognitionException {
		TypeBoundContext _localctx = new TypeBoundContext(_ctx, getState());
		enterRule(_localctx, 158, RULE_typeBound);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1807); type();
			setState(1812);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==BITAND) {
				{
				{
				setState(1808); match(BITAND);
				setState(1809); type();
				}
				}
				setState(1814);
				_errHandler.sync(this);
				_la = _input.LA(1);
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

	public static class EnumConstantsContext extends ParserRuleContext {
		public List<EnumConstantContext> enumConstant() {
			return getRuleContexts(EnumConstantContext.class);
		}
		public EnumConstantContext enumConstant(int i) {
			return getRuleContext(EnumConstantContext.class,i);
		}
		public EnumConstantsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enumConstants; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterEnumConstants(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitEnumConstants(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitEnumConstants(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EnumConstantsContext enumConstants() throws RecognitionException {
		EnumConstantsContext _localctx = new EnumConstantsContext(_ctx, getState());
		enterRule(_localctx, 160, RULE_enumConstants);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1815); enumConstant();
			setState(1820);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,183,_ctx);
			while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(1816); match(COMMA);
					setState(1817); enumConstant();
					}
					} 
				}
				setState(1822);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,183,_ctx);
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

	public static class EnumBodyDeclarationsContext extends ParserRuleContext {
		public List<ClassBodyDeclarationContext> classBodyDeclaration() {
			return getRuleContexts(ClassBodyDeclarationContext.class);
		}
		public ClassBodyDeclarationContext classBodyDeclaration(int i) {
			return getRuleContext(ClassBodyDeclarationContext.class,i);
		}
		public EnumBodyDeclarationsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enumBodyDeclarations; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterEnumBodyDeclarations(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitEnumBodyDeclarations(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitEnumBodyDeclarations(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EnumBodyDeclarationsContext enumBodyDeclarations() throws RecognitionException {
		EnumBodyDeclarationsContext _localctx = new EnumBodyDeclarationsContext(_ctx, getState());
		enterRule(_localctx, 162, RULE_enumBodyDeclarations);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1823); match(SEMI);
			setState(1827);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ARGS) | (1L << AFTER) | (1L << AROUND) | (1L << ASPECT) | (1L << BEFORE) | (1L << CALL) | (1L << CFLOW) | (1L << CFLOWBELOW) | (1L << DECLARE) | (1L << ERROR) | (1L << EXECUTION) | (1L << GET) | (1L << HANDLER) | (1L << INITIALIZATION) | (1L << ISSINGLETON) | (1L << PARENTS) | (1L << PERCFLOW) | (1L << PERCFLOWBELOW) | (1L << PERTARGET) | (1L << PERTHIS) | (1L << PERTYPEWITHIN) | (1L << POINTCUT) | (1L << PRECEDENCE) | (1L << PREINITIALIZATION) | (1L << PRIVILEGED) | (1L << RETURNING) | (1L << SET) | (1L << SOFT) | (1L << STATICINITIALIZATION) | (1L << TARGET) | (1L << THROWING) | (1L << WARNING) | (1L << WITHIN) | (1L << WITHINCODE) | (1L << AT) | (1L << ABSTRACT) | (1L << BOOLEAN) | (1L << BYTE))) != 0) || ((((_la - 66)) & ~0x3f) == 0 && ((1L << (_la - 66)) & ((1L << (CHAR - 66)) | (1L << (CLASS - 66)) | (1L << (DOUBLE - 66)) | (1L << (ENUM - 66)) | (1L << (FINAL - 66)) | (1L << (FLOAT - 66)) | (1L << (INT - 66)) | (1L << (INTERFACE - 66)) | (1L << (LONG - 66)) | (1L << (NATIVE - 66)) | (1L << (PRIVATE - 66)) | (1L << (PROTECTED - 66)) | (1L << (PUBLIC - 66)) | (1L << (SHORT - 66)) | (1L << (STATIC - 66)) | (1L << (STRICTFP - 66)) | (1L << (SYNCHRONIZED - 66)) | (1L << (TRANSIENT - 66)) | (1L << (VOID - 66)) | (1L << (VOLATILE - 66)) | (1L << (LBRACE - 66)) | (1L << (SEMI - 66)) | (1L << (LT - 66)))) != 0) || _la==Identifier) {
				{
				{
				setState(1824); classBodyDeclaration();
				}
				}
				setState(1829);
				_errHandler.sync(this);
				_la = _input.LA(1);
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

	public static class TypeListContext extends ParserRuleContext {
		public TypeContext type(int i) {
			return getRuleContext(TypeContext.class,i);
		}
		public List<TypeContext> type() {
			return getRuleContexts(TypeContext.class);
		}
		public TypeListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterTypeList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitTypeList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitTypeList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeListContext typeList() throws RecognitionException {
		TypeListContext _localctx = new TypeListContext(_ctx, getState());
		enterRule(_localctx, 164, RULE_typeList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1830); type();
			setState(1835);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1831); match(COMMA);
				setState(1832); type();
				}
				}
				setState(1837);
				_errHandler.sync(this);
				_la = _input.LA(1);
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

	public static class ClassBodyContext extends ParserRuleContext {
		public List<ClassBodyDeclarationContext> classBodyDeclaration() {
			return getRuleContexts(ClassBodyDeclarationContext.class);
		}
		public ClassBodyDeclarationContext classBodyDeclaration(int i) {
			return getRuleContext(ClassBodyDeclarationContext.class,i);
		}
		public ClassBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_classBody; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterClassBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitClassBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitClassBody(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ClassBodyContext classBody() throws RecognitionException {
		ClassBodyContext _localctx = new ClassBodyContext(_ctx, getState());
		enterRule(_localctx, 166, RULE_classBody);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1838); match(LBRACE);
			setState(1842);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ARGS) | (1L << AFTER) | (1L << AROUND) | (1L << ASPECT) | (1L << BEFORE) | (1L << CALL) | (1L << CFLOW) | (1L << CFLOWBELOW) | (1L << DECLARE) | (1L << ERROR) | (1L << EXECUTION) | (1L << GET) | (1L << HANDLER) | (1L << INITIALIZATION) | (1L << ISSINGLETON) | (1L << PARENTS) | (1L << PERCFLOW) | (1L << PERCFLOWBELOW) | (1L << PERTARGET) | (1L << PERTHIS) | (1L << PERTYPEWITHIN) | (1L << POINTCUT) | (1L << PRECEDENCE) | (1L << PREINITIALIZATION) | (1L << PRIVILEGED) | (1L << RETURNING) | (1L << SET) | (1L << SOFT) | (1L << STATICINITIALIZATION) | (1L << TARGET) | (1L << THROWING) | (1L << WARNING) | (1L << WITHIN) | (1L << WITHINCODE) | (1L << AT) | (1L << ABSTRACT) | (1L << BOOLEAN) | (1L << BYTE))) != 0) || ((((_la - 66)) & ~0x3f) == 0 && ((1L << (_la - 66)) & ((1L << (CHAR - 66)) | (1L << (CLASS - 66)) | (1L << (DOUBLE - 66)) | (1L << (ENUM - 66)) | (1L << (FINAL - 66)) | (1L << (FLOAT - 66)) | (1L << (INT - 66)) | (1L << (INTERFACE - 66)) | (1L << (LONG - 66)) | (1L << (NATIVE - 66)) | (1L << (PRIVATE - 66)) | (1L << (PROTECTED - 66)) | (1L << (PUBLIC - 66)) | (1L << (SHORT - 66)) | (1L << (STATIC - 66)) | (1L << (STRICTFP - 66)) | (1L << (SYNCHRONIZED - 66)) | (1L << (TRANSIENT - 66)) | (1L << (VOID - 66)) | (1L << (VOLATILE - 66)) | (1L << (LBRACE - 66)) | (1L << (SEMI - 66)) | (1L << (LT - 66)))) != 0) || _la==Identifier) {
				{
				{
				setState(1839); classBodyDeclaration();
				}
				}
				setState(1844);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1845); match(RBRACE);
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

	public static class InterfaceBodyContext extends ParserRuleContext {
		public List<InterfaceBodyDeclarationContext> interfaceBodyDeclaration() {
			return getRuleContexts(InterfaceBodyDeclarationContext.class);
		}
		public InterfaceBodyDeclarationContext interfaceBodyDeclaration(int i) {
			return getRuleContext(InterfaceBodyDeclarationContext.class,i);
		}
		public InterfaceBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interfaceBody; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterInterfaceBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitInterfaceBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitInterfaceBody(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InterfaceBodyContext interfaceBody() throws RecognitionException {
		InterfaceBodyContext _localctx = new InterfaceBodyContext(_ctx, getState());
		enterRule(_localctx, 168, RULE_interfaceBody);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1847); match(LBRACE);
			setState(1851);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ARGS) | (1L << AFTER) | (1L << AROUND) | (1L << ASPECT) | (1L << BEFORE) | (1L << CALL) | (1L << CFLOW) | (1L << CFLOWBELOW) | (1L << DECLARE) | (1L << ERROR) | (1L << EXECUTION) | (1L << GET) | (1L << HANDLER) | (1L << INITIALIZATION) | (1L << ISSINGLETON) | (1L << PARENTS) | (1L << PERCFLOW) | (1L << PERCFLOWBELOW) | (1L << PERTARGET) | (1L << PERTHIS) | (1L << PERTYPEWITHIN) | (1L << POINTCUT) | (1L << PRECEDENCE) | (1L << PREINITIALIZATION) | (1L << PRIVILEGED) | (1L << RETURNING) | (1L << SET) | (1L << SOFT) | (1L << STATICINITIALIZATION) | (1L << TARGET) | (1L << THROWING) | (1L << WARNING) | (1L << WITHIN) | (1L << WITHINCODE) | (1L << AT) | (1L << ABSTRACT) | (1L << BOOLEAN) | (1L << BYTE))) != 0) || ((((_la - 66)) & ~0x3f) == 0 && ((1L << (_la - 66)) & ((1L << (CHAR - 66)) | (1L << (CLASS - 66)) | (1L << (DOUBLE - 66)) | (1L << (ENUM - 66)) | (1L << (FINAL - 66)) | (1L << (FLOAT - 66)) | (1L << (INT - 66)) | (1L << (INTERFACE - 66)) | (1L << (LONG - 66)) | (1L << (NATIVE - 66)) | (1L << (PRIVATE - 66)) | (1L << (PROTECTED - 66)) | (1L << (PUBLIC - 66)) | (1L << (SHORT - 66)) | (1L << (STATIC - 66)) | (1L << (STRICTFP - 66)) | (1L << (SYNCHRONIZED - 66)) | (1L << (TRANSIENT - 66)) | (1L << (VOID - 66)) | (1L << (VOLATILE - 66)) | (1L << (SEMI - 66)) | (1L << (LT - 66)))) != 0) || _la==Identifier) {
				{
				{
				setState(1848); interfaceBodyDeclaration();
				}
				}
				setState(1853);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1854); match(RBRACE);
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

	public static class GenericMethodDeclarationContext extends ParserRuleContext {
		public MethodDeclarationContext methodDeclaration() {
			return getRuleContext(MethodDeclarationContext.class,0);
		}
		public TypeParametersContext typeParameters() {
			return getRuleContext(TypeParametersContext.class,0);
		}
		public GenericMethodDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_genericMethodDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterGenericMethodDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitGenericMethodDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitGenericMethodDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GenericMethodDeclarationContext genericMethodDeclaration() throws RecognitionException {
		GenericMethodDeclarationContext _localctx = new GenericMethodDeclarationContext(_ctx, getState());
		enterRule(_localctx, 170, RULE_genericMethodDeclaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1856); typeParameters();
			setState(1857); methodDeclaration();
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

	public static class GenericConstructorDeclarationContext extends ParserRuleContext {
		public TypeParametersContext typeParameters() {
			return getRuleContext(TypeParametersContext.class,0);
		}
		public ConstructorDeclarationContext constructorDeclaration() {
			return getRuleContext(ConstructorDeclarationContext.class,0);
		}
		public GenericConstructorDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_genericConstructorDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterGenericConstructorDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitGenericConstructorDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitGenericConstructorDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GenericConstructorDeclarationContext genericConstructorDeclaration() throws RecognitionException {
		GenericConstructorDeclarationContext _localctx = new GenericConstructorDeclarationContext(_ctx, getState());
		enterRule(_localctx, 172, RULE_genericConstructorDeclaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1859); typeParameters();
			setState(1860); constructorDeclaration();
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

	public static class FieldDeclarationContext extends ParserRuleContext {
		public VariableDeclaratorsContext variableDeclarators() {
			return getRuleContext(VariableDeclaratorsContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public FieldDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fieldDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterFieldDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitFieldDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitFieldDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FieldDeclarationContext fieldDeclaration() throws RecognitionException {
		FieldDeclarationContext _localctx = new FieldDeclarationContext(_ctx, getState());
		enterRule(_localctx, 174, RULE_fieldDeclaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1862); type();
			setState(1863); variableDeclarators();
			setState(1864); match(SEMI);
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

	public static class InterfaceBodyDeclarationContext extends ParserRuleContext {
		public List<ModifierContext> modifier() {
			return getRuleContexts(ModifierContext.class);
		}
		public ModifierContext modifier(int i) {
			return getRuleContext(ModifierContext.class,i);
		}
		public InterfaceMemberDeclarationContext interfaceMemberDeclaration() {
			return getRuleContext(InterfaceMemberDeclarationContext.class,0);
		}
		public InterfaceBodyDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interfaceBodyDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterInterfaceBodyDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitInterfaceBodyDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitInterfaceBodyDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InterfaceBodyDeclarationContext interfaceBodyDeclaration() throws RecognitionException {
		InterfaceBodyDeclarationContext _localctx = new InterfaceBodyDeclarationContext(_ctx, getState());
		enterRule(_localctx, 176, RULE_interfaceBodyDeclaration);
		try {
			int _alt;
			setState(1874);
			switch (_input.LA(1)) {
			case ARGS:
			case AFTER:
			case AROUND:
			case ASPECT:
			case BEFORE:
			case CALL:
			case CFLOW:
			case CFLOWBELOW:
			case DECLARE:
			case ERROR:
			case EXECUTION:
			case GET:
			case HANDLER:
			case INITIALIZATION:
			case ISSINGLETON:
			case PARENTS:
			case PERCFLOW:
			case PERCFLOWBELOW:
			case PERTARGET:
			case PERTHIS:
			case PERTYPEWITHIN:
			case POINTCUT:
			case PRECEDENCE:
			case PREINITIALIZATION:
			case PRIVILEGED:
			case RETURNING:
			case SET:
			case SOFT:
			case STATICINITIALIZATION:
			case TARGET:
			case THROWING:
			case WARNING:
			case WITHIN:
			case WITHINCODE:
			case AT:
			case ABSTRACT:
			case BOOLEAN:
			case BYTE:
			case CHAR:
			case CLASS:
			case DOUBLE:
			case ENUM:
			case FINAL:
			case FLOAT:
			case INT:
			case INTERFACE:
			case LONG:
			case NATIVE:
			case PRIVATE:
			case PROTECTED:
			case PUBLIC:
			case SHORT:
			case STATIC:
			case STRICTFP:
			case SYNCHRONIZED:
			case TRANSIENT:
			case VOID:
			case VOLATILE:
			case LT:
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(1869);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,188,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1866); modifier();
						}
						} 
					}
					setState(1871);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,188,_ctx);
				}
				setState(1872); interfaceMemberDeclaration();
				}
				break;
			case SEMI:
				enterOuterAlt(_localctx, 2);
				{
				setState(1873); match(SEMI);
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

	public static class InterfaceMemberDeclarationContext extends ParserRuleContext {
		public EnumDeclarationContext enumDeclaration() {
			return getRuleContext(EnumDeclarationContext.class,0);
		}
		public ClassDeclarationContext classDeclaration() {
			return getRuleContext(ClassDeclarationContext.class,0);
		}
		public GenericInterfaceMethodDeclarationContext genericInterfaceMethodDeclaration() {
			return getRuleContext(GenericInterfaceMethodDeclarationContext.class,0);
		}
		public AnnotationTypeDeclarationContext annotationTypeDeclaration() {
			return getRuleContext(AnnotationTypeDeclarationContext.class,0);
		}
		public InterfaceDeclarationContext interfaceDeclaration() {
			return getRuleContext(InterfaceDeclarationContext.class,0);
		}
		public ConstDeclarationContext constDeclaration() {
			return getRuleContext(ConstDeclarationContext.class,0);
		}
		public InterfaceMethodDeclarationContext interfaceMethodDeclaration() {
			return getRuleContext(InterfaceMethodDeclarationContext.class,0);
		}
		public InterfaceMemberDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interfaceMemberDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterInterfaceMemberDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitInterfaceMemberDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitInterfaceMemberDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InterfaceMemberDeclarationContext interfaceMemberDeclaration() throws RecognitionException {
		InterfaceMemberDeclarationContext _localctx = new InterfaceMemberDeclarationContext(_ctx, getState());
		enterRule(_localctx, 178, RULE_interfaceMemberDeclaration);
		try {
			setState(1883);
			switch ( getInterpreter().adaptivePredict(_input,190,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1876); constDeclaration();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1877); interfaceMethodDeclaration();
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(1878); genericInterfaceMethodDeclaration();
				}
				break;

			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(1879); interfaceDeclaration();
				}
				break;

			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(1880); annotationTypeDeclaration();
				}
				break;

			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(1881); classDeclaration();
				}
				break;

			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(1882); enumDeclaration();
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

	public static class ConstDeclarationContext extends ParserRuleContext {
		public ConstantDeclaratorContext constantDeclarator(int i) {
			return getRuleContext(ConstantDeclaratorContext.class,i);
		}
		public List<ConstantDeclaratorContext> constantDeclarator() {
			return getRuleContexts(ConstantDeclaratorContext.class);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public ConstDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterConstDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitConstDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitConstDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstDeclarationContext constDeclaration() throws RecognitionException {
		ConstDeclarationContext _localctx = new ConstDeclarationContext(_ctx, getState());
		enterRule(_localctx, 180, RULE_constDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1885); type();
			setState(1886); constantDeclarator();
			setState(1891);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1887); match(COMMA);
				setState(1888); constantDeclarator();
				}
				}
				setState(1893);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1894); match(SEMI);
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

	public static class GenericInterfaceMethodDeclarationContext extends ParserRuleContext {
		public TypeParametersContext typeParameters() {
			return getRuleContext(TypeParametersContext.class,0);
		}
		public InterfaceMethodDeclarationContext interfaceMethodDeclaration() {
			return getRuleContext(InterfaceMethodDeclarationContext.class,0);
		}
		public GenericInterfaceMethodDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_genericInterfaceMethodDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterGenericInterfaceMethodDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitGenericInterfaceMethodDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitGenericInterfaceMethodDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GenericInterfaceMethodDeclarationContext genericInterfaceMethodDeclaration() throws RecognitionException {
		GenericInterfaceMethodDeclarationContext _localctx = new GenericInterfaceMethodDeclarationContext(_ctx, getState());
		enterRule(_localctx, 182, RULE_genericInterfaceMethodDeclaration);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1896); typeParameters();
			setState(1897); interfaceMethodDeclaration();
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

	public static class VariableDeclaratorsContext extends ParserRuleContext {
		public List<VariableDeclaratorContext> variableDeclarator() {
			return getRuleContexts(VariableDeclaratorContext.class);
		}
		public VariableDeclaratorContext variableDeclarator(int i) {
			return getRuleContext(VariableDeclaratorContext.class,i);
		}
		public VariableDeclaratorsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableDeclarators; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterVariableDeclarators(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitVariableDeclarators(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitVariableDeclarators(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableDeclaratorsContext variableDeclarators() throws RecognitionException {
		VariableDeclaratorsContext _localctx = new VariableDeclaratorsContext(_ctx, getState());
		enterRule(_localctx, 184, RULE_variableDeclarators);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1899); variableDeclarator();
			setState(1904);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1900); match(COMMA);
				setState(1901); variableDeclarator();
				}
				}
				setState(1906);
				_errHandler.sync(this);
				_la = _input.LA(1);
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

	public static class VariableDeclaratorContext extends ParserRuleContext {
		public VariableInitializerContext variableInitializer() {
			return getRuleContext(VariableInitializerContext.class,0);
		}
		public VariableDeclaratorIdContext variableDeclaratorId() {
			return getRuleContext(VariableDeclaratorIdContext.class,0);
		}
		public VariableDeclaratorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableDeclarator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterVariableDeclarator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitVariableDeclarator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitVariableDeclarator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableDeclaratorContext variableDeclarator() throws RecognitionException {
		VariableDeclaratorContext _localctx = new VariableDeclaratorContext(_ctx, getState());
		enterRule(_localctx, 186, RULE_variableDeclarator);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1907); variableDeclaratorId();
			setState(1910);
			_la = _input.LA(1);
			if (_la==ASSIGN) {
				{
				setState(1908); match(ASSIGN);
				setState(1909); variableInitializer();
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

	public static class VariableInitializerContext extends ParserRuleContext {
		public ArrayInitializerContext arrayInitializer() {
			return getRuleContext(ArrayInitializerContext.class,0);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public VariableInitializerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variableInitializer; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterVariableInitializer(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitVariableInitializer(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitVariableInitializer(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableInitializerContext variableInitializer() throws RecognitionException {
		VariableInitializerContext _localctx = new VariableInitializerContext(_ctx, getState());
		enterRule(_localctx, 188, RULE_variableInitializer);
		try {
			setState(1914);
			switch (_input.LA(1)) {
			case LBRACE:
				enterOuterAlt(_localctx, 1);
				{
				setState(1912); arrayInitializer();
				}
				break;
			case ARGS:
			case AFTER:
			case AROUND:
			case ASPECT:
			case BEFORE:
			case CALL:
			case CFLOW:
			case CFLOWBELOW:
			case DECLARE:
			case ERROR:
			case EXECUTION:
			case GET:
			case HANDLER:
			case INITIALIZATION:
			case ISSINGLETON:
			case PARENTS:
			case PERCFLOW:
			case PERCFLOWBELOW:
			case PERTARGET:
			case PERTHIS:
			case PERTYPEWITHIN:
			case POINTCUT:
			case PRECEDENCE:
			case PREINITIALIZATION:
			case PRIVILEGED:
			case RETURNING:
			case SET:
			case SOFT:
			case STATICINITIALIZATION:
			case TARGET:
			case THROWING:
			case WARNING:
			case WITHIN:
			case WITHINCODE:
			case BOOLEAN:
			case BYTE:
			case CHAR:
			case DOUBLE:
			case FLOAT:
			case INT:
			case LONG:
			case NEW:
			case SHORT:
			case SUPER:
			case THIS:
			case VOID:
			case IntegerLiteral:
			case FloatingPointLiteral:
			case BooleanLiteral:
			case CharacterLiteral:
			case StringLiteral:
			case NullLiteral:
			case LPAREN:
			case LT:
			case BANG:
			case TILDE:
			case INC:
			case DEC:
			case ADD:
			case SUB:
			case Identifier:
				enterOuterAlt(_localctx, 2);
				{
				setState(1913); expression(0);
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

	public static class ArrayInitializerContext extends ParserRuleContext {
		public List<VariableInitializerContext> variableInitializer() {
			return getRuleContexts(VariableInitializerContext.class);
		}
		public VariableInitializerContext variableInitializer(int i) {
			return getRuleContext(VariableInitializerContext.class,i);
		}
		public ArrayInitializerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arrayInitializer; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterArrayInitializer(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitArrayInitializer(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitArrayInitializer(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArrayInitializerContext arrayInitializer() throws RecognitionException {
		ArrayInitializerContext _localctx = new ArrayInitializerContext(_ctx, getState());
		enterRule(_localctx, 190, RULE_arrayInitializer);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(1916); match(LBRACE);
			setState(1928);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ARGS) | (1L << AFTER) | (1L << AROUND) | (1L << ASPECT) | (1L << BEFORE) | (1L << CALL) | (1L << CFLOW) | (1L << CFLOWBELOW) | (1L << DECLARE) | (1L << ERROR) | (1L << EXECUTION) | (1L << GET) | (1L << HANDLER) | (1L << INITIALIZATION) | (1L << ISSINGLETON) | (1L << PARENTS) | (1L << PERCFLOW) | (1L << PERCFLOWBELOW) | (1L << PERTARGET) | (1L << PERTHIS) | (1L << PERTYPEWITHIN) | (1L << POINTCUT) | (1L << PRECEDENCE) | (1L << PREINITIALIZATION) | (1L << PRIVILEGED) | (1L << RETURNING) | (1L << SET) | (1L << SOFT) | (1L << STATICINITIALIZATION) | (1L << TARGET) | (1L << THROWING) | (1L << WARNING) | (1L << WITHIN) | (1L << WITHINCODE) | (1L << BOOLEAN) | (1L << BYTE))) != 0) || ((((_la - 66)) & ~0x3f) == 0 && ((1L << (_la - 66)) & ((1L << (CHAR - 66)) | (1L << (DOUBLE - 66)) | (1L << (FLOAT - 66)) | (1L << (INT - 66)) | (1L << (LONG - 66)) | (1L << (NEW - 66)) | (1L << (SHORT - 66)) | (1L << (SUPER - 66)) | (1L << (THIS - 66)) | (1L << (VOID - 66)) | (1L << (IntegerLiteral - 66)) | (1L << (FloatingPointLiteral - 66)) | (1L << (BooleanLiteral - 66)) | (1L << (CharacterLiteral - 66)) | (1L << (StringLiteral - 66)) | (1L << (NullLiteral - 66)) | (1L << (LPAREN - 66)) | (1L << (LBRACE - 66)) | (1L << (LT - 66)) | (1L << (BANG - 66)) | (1L << (TILDE - 66)))) != 0) || ((((_la - 137)) & ~0x3f) == 0 && ((1L << (_la - 137)) & ((1L << (INC - 137)) | (1L << (DEC - 137)) | (1L << (ADD - 137)) | (1L << (SUB - 137)) | (1L << (Identifier - 137)))) != 0)) {
				{
				setState(1917); variableInitializer();
				setState(1922);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,195,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1918); match(COMMA);
						setState(1919); variableInitializer();
						}
						} 
					}
					setState(1924);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,195,_ctx);
				}
				setState(1926);
				_la = _input.LA(1);
				if (_la==COMMA) {
					{
					setState(1925); match(COMMA);
					}
				}

				}
			}

			setState(1930); match(RBRACE);
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

	public static class TypeContext extends ParserRuleContext {
		public PrimitiveTypeContext primitiveType() {
			return getRuleContext(PrimitiveTypeContext.class,0);
		}
		public ClassOrInterfaceTypeContext classOrInterfaceType() {
			return getRuleContext(ClassOrInterfaceTypeContext.class,0);
		}
		public TypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeContext type() throws RecognitionException {
		TypeContext _localctx = new TypeContext(_ctx, getState());
		enterRule(_localctx, 192, RULE_type);
		try {
			int _alt;
			setState(1948);
			switch (_input.LA(1)) {
			case ARGS:
			case AFTER:
			case AROUND:
			case ASPECT:
			case BEFORE:
			case CALL:
			case CFLOW:
			case CFLOWBELOW:
			case DECLARE:
			case ERROR:
			case EXECUTION:
			case GET:
			case HANDLER:
			case INITIALIZATION:
			case ISSINGLETON:
			case PARENTS:
			case PERCFLOW:
			case PERCFLOWBELOW:
			case PERTARGET:
			case PERTHIS:
			case PERTYPEWITHIN:
			case POINTCUT:
			case PRECEDENCE:
			case PREINITIALIZATION:
			case PRIVILEGED:
			case RETURNING:
			case SET:
			case SOFT:
			case STATICINITIALIZATION:
			case TARGET:
			case THROWING:
			case WARNING:
			case WITHIN:
			case WITHINCODE:
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(1932); classOrInterfaceType();
				setState(1937);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,198,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1933); match(LBRACK);
						setState(1934); match(RBRACK);
						}
						} 
					}
					setState(1939);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,198,_ctx);
				}
				}
				break;
			case BOOLEAN:
			case BYTE:
			case CHAR:
			case DOUBLE:
			case FLOAT:
			case INT:
			case LONG:
			case SHORT:
				enterOuterAlt(_localctx, 2);
				{
				setState(1940); primitiveType();
				setState(1945);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,199,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1941); match(LBRACK);
						setState(1942); match(RBRACK);
						}
						} 
					}
					setState(1947);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,199,_ctx);
				}
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

	public static class PrimitiveTypeContext extends ParserRuleContext {
		public PrimitiveTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_primitiveType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterPrimitiveType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitPrimitiveType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitPrimitiveType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PrimitiveTypeContext primitiveType() throws RecognitionException {
		PrimitiveTypeContext _localctx = new PrimitiveTypeContext(_ctx, getState());
		enterRule(_localctx, 194, RULE_primitiveType);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1950);
			_la = _input.LA(1);
			if ( !(((((_la - 61)) & ~0x3f) == 0 && ((1L << (_la - 61)) & ((1L << (BOOLEAN - 61)) | (1L << (BYTE - 61)) | (1L << (CHAR - 61)) | (1L << (DOUBLE - 61)) | (1L << (FLOAT - 61)) | (1L << (INT - 61)) | (1L << (LONG - 61)) | (1L << (SHORT - 61)))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			consume();
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

	public static class TypeArgumentsContext extends ParserRuleContext {
		public List<TypeArgumentContext> typeArgument() {
			return getRuleContexts(TypeArgumentContext.class);
		}
		public TypeArgumentContext typeArgument(int i) {
			return getRuleContext(TypeArgumentContext.class,i);
		}
		public TypeArgumentsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeArguments; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterTypeArguments(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitTypeArguments(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitTypeArguments(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeArgumentsContext typeArguments() throws RecognitionException {
		TypeArgumentsContext _localctx = new TypeArgumentsContext(_ctx, getState());
		enterRule(_localctx, 196, RULE_typeArguments);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1952); match(LT);
			setState(1953); typeArgument();
			setState(1958);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1954); match(COMMA);
				setState(1955); typeArgument();
				}
				}
				setState(1960);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(1961); match(GT);
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

	public static class TypeArgumentContext extends ParserRuleContext {
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TypeArgumentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeArgument; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterTypeArgument(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitTypeArgument(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitTypeArgument(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeArgumentContext typeArgument() throws RecognitionException {
		TypeArgumentContext _localctx = new TypeArgumentContext(_ctx, getState());
		enterRule(_localctx, 198, RULE_typeArgument);
		int _la;
		try {
			setState(1969);
			switch (_input.LA(1)) {
			case ARGS:
			case AFTER:
			case AROUND:
			case ASPECT:
			case BEFORE:
			case CALL:
			case CFLOW:
			case CFLOWBELOW:
			case DECLARE:
			case ERROR:
			case EXECUTION:
			case GET:
			case HANDLER:
			case INITIALIZATION:
			case ISSINGLETON:
			case PARENTS:
			case PERCFLOW:
			case PERCFLOWBELOW:
			case PERTARGET:
			case PERTHIS:
			case PERTYPEWITHIN:
			case POINTCUT:
			case PRECEDENCE:
			case PREINITIALIZATION:
			case PRIVILEGED:
			case RETURNING:
			case SET:
			case SOFT:
			case STATICINITIALIZATION:
			case TARGET:
			case THROWING:
			case WARNING:
			case WITHIN:
			case WITHINCODE:
			case BOOLEAN:
			case BYTE:
			case CHAR:
			case DOUBLE:
			case FLOAT:
			case INT:
			case LONG:
			case SHORT:
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(1963); type();
				}
				break;
			case QUESTION:
				enterOuterAlt(_localctx, 2);
				{
				setState(1964); match(QUESTION);
				setState(1967);
				_la = _input.LA(1);
				if (_la==EXTENDS || _la==SUPER) {
					{
					setState(1965);
					_la = _input.LA(1);
					if ( !(_la==EXTENDS || _la==SUPER) ) {
					_errHandler.recoverInline(this);
					}
					consume();
					setState(1966); type();
					}
				}

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

	public static class QualifiedNameListContext extends ParserRuleContext {
		public List<QualifiedNameContext> qualifiedName() {
			return getRuleContexts(QualifiedNameContext.class);
		}
		public QualifiedNameContext qualifiedName(int i) {
			return getRuleContext(QualifiedNameContext.class,i);
		}
		public QualifiedNameListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_qualifiedNameList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterQualifiedNameList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitQualifiedNameList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitQualifiedNameList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QualifiedNameListContext qualifiedNameList() throws RecognitionException {
		QualifiedNameListContext _localctx = new QualifiedNameListContext(_ctx, getState());
		enterRule(_localctx, 200, RULE_qualifiedNameList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1971); qualifiedName();
			setState(1976);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(1972); match(COMMA);
				setState(1973); qualifiedName();
				}
				}
				setState(1978);
				_errHandler.sync(this);
				_la = _input.LA(1);
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

	public static class FormalParametersContext extends ParserRuleContext {
		public FormalParameterListContext formalParameterList() {
			return getRuleContext(FormalParameterListContext.class,0);
		}
		public FormalParametersContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_formalParameters; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterFormalParameters(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitFormalParameters(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitFormalParameters(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FormalParametersContext formalParameters() throws RecognitionException {
		FormalParametersContext _localctx = new FormalParametersContext(_ctx, getState());
		enterRule(_localctx, 202, RULE_formalParameters);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(1979); match(LPAREN);
			setState(1981);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ARGS) | (1L << AFTER) | (1L << AROUND) | (1L << ASPECT) | (1L << BEFORE) | (1L << CALL) | (1L << CFLOW) | (1L << CFLOWBELOW) | (1L << DECLARE) | (1L << ERROR) | (1L << EXECUTION) | (1L << GET) | (1L << HANDLER) | (1L << INITIALIZATION) | (1L << ISSINGLETON) | (1L << PARENTS) | (1L << PERCFLOW) | (1L << PERCFLOWBELOW) | (1L << PERTARGET) | (1L << PERTHIS) | (1L << PERTYPEWITHIN) | (1L << POINTCUT) | (1L << PRECEDENCE) | (1L << PREINITIALIZATION) | (1L << PRIVILEGED) | (1L << RETURNING) | (1L << SET) | (1L << SOFT) | (1L << STATICINITIALIZATION) | (1L << TARGET) | (1L << THROWING) | (1L << WARNING) | (1L << WITHIN) | (1L << WITHINCODE) | (1L << AT) | (1L << BOOLEAN) | (1L << BYTE))) != 0) || ((((_la - 66)) & ~0x3f) == 0 && ((1L << (_la - 66)) & ((1L << (CHAR - 66)) | (1L << (DOUBLE - 66)) | (1L << (FINAL - 66)) | (1L << (FLOAT - 66)) | (1L << (INT - 66)) | (1L << (LONG - 66)) | (1L << (SHORT - 66)))) != 0) || _la==Identifier) {
				{
				setState(1980); formalParameterList();
				}
			}

			setState(1983); match(RPAREN);
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

	public static class FormalParameterListContext extends ParserRuleContext {
		public List<FormalParameterContext> formalParameter() {
			return getRuleContexts(FormalParameterContext.class);
		}
		public LastFormalParameterContext lastFormalParameter() {
			return getRuleContext(LastFormalParameterContext.class,0);
		}
		public FormalParameterContext formalParameter(int i) {
			return getRuleContext(FormalParameterContext.class,i);
		}
		public FormalParameterListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_formalParameterList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterFormalParameterList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitFormalParameterList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitFormalParameterList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FormalParameterListContext formalParameterList() throws RecognitionException {
		FormalParameterListContext _localctx = new FormalParameterListContext(_ctx, getState());
		enterRule(_localctx, 204, RULE_formalParameterList);
		int _la;
		try {
			int _alt;
			setState(1998);
			switch ( getInterpreter().adaptivePredict(_input,208,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(1985); formalParameter();
				setState(1990);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,206,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(1986); match(COMMA);
						setState(1987); formalParameter();
						}
						} 
					}
					setState(1992);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,206,_ctx);
				}
				setState(1995);
				_la = _input.LA(1);
				if (_la==COMMA) {
					{
					setState(1993); match(COMMA);
					setState(1994); lastFormalParameter();
					}
				}

				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(1997); lastFormalParameter();
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

	public static class FormalParameterContext extends ParserRuleContext {
		public VariableModifierContext variableModifier(int i) {
			return getRuleContext(VariableModifierContext.class,i);
		}
		public List<VariableModifierContext> variableModifier() {
			return getRuleContexts(VariableModifierContext.class);
		}
		public VariableDeclaratorIdContext variableDeclaratorId() {
			return getRuleContext(VariableDeclaratorIdContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public FormalParameterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_formalParameter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterFormalParameter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitFormalParameter(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitFormalParameter(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FormalParameterContext formalParameter() throws RecognitionException {
		FormalParameterContext _localctx = new FormalParameterContext(_ctx, getState());
		enterRule(_localctx, 206, RULE_formalParameter);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2003);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==FINAL) {
				{
				{
				setState(2000); variableModifier();
				}
				}
				setState(2005);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(2006); type();
			setState(2007); variableDeclaratorId();
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

	public static class LastFormalParameterContext extends ParserRuleContext {
		public VariableModifierContext variableModifier(int i) {
			return getRuleContext(VariableModifierContext.class,i);
		}
		public List<VariableModifierContext> variableModifier() {
			return getRuleContexts(VariableModifierContext.class);
		}
		public VariableDeclaratorIdContext variableDeclaratorId() {
			return getRuleContext(VariableDeclaratorIdContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public LastFormalParameterContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_lastFormalParameter; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterLastFormalParameter(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitLastFormalParameter(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitLastFormalParameter(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LastFormalParameterContext lastFormalParameter() throws RecognitionException {
		LastFormalParameterContext _localctx = new LastFormalParameterContext(_ctx, getState());
		enterRule(_localctx, 208, RULE_lastFormalParameter);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2012);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==FINAL) {
				{
				{
				setState(2009); variableModifier();
				}
				}
				setState(2014);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(2015); type();
			setState(2016); match(ELLIPSIS);
			setState(2017); variableDeclaratorId();
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

	public static class MethodBodyContext extends ParserRuleContext {
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public MethodBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_methodBody; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterMethodBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitMethodBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitMethodBody(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MethodBodyContext methodBody() throws RecognitionException {
		MethodBodyContext _localctx = new MethodBodyContext(_ctx, getState());
		enterRule(_localctx, 210, RULE_methodBody);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2019); block();
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

	public static class ConstructorBodyContext extends ParserRuleContext {
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public ConstructorBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constructorBody; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterConstructorBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitConstructorBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitConstructorBody(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstructorBodyContext constructorBody() throws RecognitionException {
		ConstructorBodyContext _localctx = new ConstructorBodyContext(_ctx, getState());
		enterRule(_localctx, 212, RULE_constructorBody);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2021); block();
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

	public static class LiteralContext extends ParserRuleContext {
		public TerminalNode StringLiteral() { return getToken(AspectJParser.StringLiteral, 0); }
		public TerminalNode IntegerLiteral() { return getToken(AspectJParser.IntegerLiteral, 0); }
		public TerminalNode FloatingPointLiteral() { return getToken(AspectJParser.FloatingPointLiteral, 0); }
		public TerminalNode BooleanLiteral() { return getToken(AspectJParser.BooleanLiteral, 0); }
		public TerminalNode CharacterLiteral() { return getToken(AspectJParser.CharacterLiteral, 0); }
		public LiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_literal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LiteralContext literal() throws RecognitionException {
		LiteralContext _localctx = new LiteralContext(_ctx, getState());
		enterRule(_localctx, 214, RULE_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2023);
			_la = _input.LA(1);
			if ( !(((((_la - 109)) & ~0x3f) == 0 && ((1L << (_la - 109)) & ((1L << (IntegerLiteral - 109)) | (1L << (FloatingPointLiteral - 109)) | (1L << (BooleanLiteral - 109)) | (1L << (CharacterLiteral - 109)) | (1L << (StringLiteral - 109)) | (1L << (NullLiteral - 109)))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			consume();
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

	public static class AnnotationNameContext extends ParserRuleContext {
		public QualifiedNameContext qualifiedName() {
			return getRuleContext(QualifiedNameContext.class,0);
		}
		public AnnotationNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotationName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterAnnotationName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitAnnotationName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitAnnotationName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnnotationNameContext annotationName() throws RecognitionException {
		AnnotationNameContext _localctx = new AnnotationNameContext(_ctx, getState());
		enterRule(_localctx, 216, RULE_annotationName);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2025); qualifiedName();
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

	public static class ElementValuePairsContext extends ParserRuleContext {
		public ElementValuePairContext elementValuePair(int i) {
			return getRuleContext(ElementValuePairContext.class,i);
		}
		public List<ElementValuePairContext> elementValuePair() {
			return getRuleContexts(ElementValuePairContext.class);
		}
		public ElementValuePairsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_elementValuePairs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterElementValuePairs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitElementValuePairs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitElementValuePairs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ElementValuePairsContext elementValuePairs() throws RecognitionException {
		ElementValuePairsContext _localctx = new ElementValuePairsContext(_ctx, getState());
		enterRule(_localctx, 218, RULE_elementValuePairs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2027); elementValuePair();
			setState(2032);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(2028); match(COMMA);
				setState(2029); elementValuePair();
				}
				}
				setState(2034);
				_errHandler.sync(this);
				_la = _input.LA(1);
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

	public static class ElementValueContext extends ParserRuleContext {
		public ElementValueArrayInitializerContext elementValueArrayInitializer() {
			return getRuleContext(ElementValueArrayInitializerContext.class,0);
		}
		public AnnotationContext annotation() {
			return getRuleContext(AnnotationContext.class,0);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public ElementValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_elementValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterElementValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitElementValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitElementValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ElementValueContext elementValue() throws RecognitionException {
		ElementValueContext _localctx = new ElementValueContext(_ctx, getState());
		enterRule(_localctx, 220, RULE_elementValue);
		try {
			setState(2038);
			switch (_input.LA(1)) {
			case ARGS:
			case AFTER:
			case AROUND:
			case ASPECT:
			case BEFORE:
			case CALL:
			case CFLOW:
			case CFLOWBELOW:
			case DECLARE:
			case ERROR:
			case EXECUTION:
			case GET:
			case HANDLER:
			case INITIALIZATION:
			case ISSINGLETON:
			case PARENTS:
			case PERCFLOW:
			case PERCFLOWBELOW:
			case PERTARGET:
			case PERTHIS:
			case PERTYPEWITHIN:
			case POINTCUT:
			case PRECEDENCE:
			case PREINITIALIZATION:
			case PRIVILEGED:
			case RETURNING:
			case SET:
			case SOFT:
			case STATICINITIALIZATION:
			case TARGET:
			case THROWING:
			case WARNING:
			case WITHIN:
			case WITHINCODE:
			case BOOLEAN:
			case BYTE:
			case CHAR:
			case DOUBLE:
			case FLOAT:
			case INT:
			case LONG:
			case NEW:
			case SHORT:
			case SUPER:
			case THIS:
			case VOID:
			case IntegerLiteral:
			case FloatingPointLiteral:
			case BooleanLiteral:
			case CharacterLiteral:
			case StringLiteral:
			case NullLiteral:
			case LPAREN:
			case LT:
			case BANG:
			case TILDE:
			case INC:
			case DEC:
			case ADD:
			case SUB:
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(2035); expression(0);
				}
				break;
			case AT:
				enterOuterAlt(_localctx, 2);
				{
				setState(2036); annotation();
				}
				break;
			case LBRACE:
				enterOuterAlt(_localctx, 3);
				{
				setState(2037); elementValueArrayInitializer();
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

	public static class ElementValueArrayInitializerContext extends ParserRuleContext {
		public ElementValueContext elementValue(int i) {
			return getRuleContext(ElementValueContext.class,i);
		}
		public List<ElementValueContext> elementValue() {
			return getRuleContexts(ElementValueContext.class);
		}
		public ElementValueArrayInitializerContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_elementValueArrayInitializer; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterElementValueArrayInitializer(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitElementValueArrayInitializer(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitElementValueArrayInitializer(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ElementValueArrayInitializerContext elementValueArrayInitializer() throws RecognitionException {
		ElementValueArrayInitializerContext _localctx = new ElementValueArrayInitializerContext(_ctx, getState());
		enterRule(_localctx, 222, RULE_elementValueArrayInitializer);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(2040); match(LBRACE);
			setState(2049);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ARGS) | (1L << AFTER) | (1L << AROUND) | (1L << ASPECT) | (1L << BEFORE) | (1L << CALL) | (1L << CFLOW) | (1L << CFLOWBELOW) | (1L << DECLARE) | (1L << ERROR) | (1L << EXECUTION) | (1L << GET) | (1L << HANDLER) | (1L << INITIALIZATION) | (1L << ISSINGLETON) | (1L << PARENTS) | (1L << PERCFLOW) | (1L << PERCFLOWBELOW) | (1L << PERTARGET) | (1L << PERTHIS) | (1L << PERTYPEWITHIN) | (1L << POINTCUT) | (1L << PRECEDENCE) | (1L << PREINITIALIZATION) | (1L << PRIVILEGED) | (1L << RETURNING) | (1L << SET) | (1L << SOFT) | (1L << STATICINITIALIZATION) | (1L << TARGET) | (1L << THROWING) | (1L << WARNING) | (1L << WITHIN) | (1L << WITHINCODE) | (1L << AT) | (1L << BOOLEAN) | (1L << BYTE))) != 0) || ((((_la - 66)) & ~0x3f) == 0 && ((1L << (_la - 66)) & ((1L << (CHAR - 66)) | (1L << (DOUBLE - 66)) | (1L << (FLOAT - 66)) | (1L << (INT - 66)) | (1L << (LONG - 66)) | (1L << (NEW - 66)) | (1L << (SHORT - 66)) | (1L << (SUPER - 66)) | (1L << (THIS - 66)) | (1L << (VOID - 66)) | (1L << (IntegerLiteral - 66)) | (1L << (FloatingPointLiteral - 66)) | (1L << (BooleanLiteral - 66)) | (1L << (CharacterLiteral - 66)) | (1L << (StringLiteral - 66)) | (1L << (NullLiteral - 66)) | (1L << (LPAREN - 66)) | (1L << (LBRACE - 66)) | (1L << (LT - 66)) | (1L << (BANG - 66)) | (1L << (TILDE - 66)))) != 0) || ((((_la - 137)) & ~0x3f) == 0 && ((1L << (_la - 137)) & ((1L << (INC - 137)) | (1L << (DEC - 137)) | (1L << (ADD - 137)) | (1L << (SUB - 137)) | (1L << (Identifier - 137)))) != 0)) {
				{
				setState(2041); elementValue();
				setState(2046);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,213,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(2042); match(COMMA);
						setState(2043); elementValue();
						}
						} 
					}
					setState(2048);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,213,_ctx);
				}
				}
			}

			setState(2052);
			_la = _input.LA(1);
			if (_la==COMMA) {
				{
				setState(2051); match(COMMA);
				}
			}

			setState(2054); match(RBRACE);
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

	public static class AnnotationTypeBodyContext extends ParserRuleContext {
		public List<AnnotationTypeElementDeclarationContext> annotationTypeElementDeclaration() {
			return getRuleContexts(AnnotationTypeElementDeclarationContext.class);
		}
		public AnnotationTypeElementDeclarationContext annotationTypeElementDeclaration(int i) {
			return getRuleContext(AnnotationTypeElementDeclarationContext.class,i);
		}
		public AnnotationTypeBodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotationTypeBody; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterAnnotationTypeBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitAnnotationTypeBody(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitAnnotationTypeBody(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnnotationTypeBodyContext annotationTypeBody() throws RecognitionException {
		AnnotationTypeBodyContext _localctx = new AnnotationTypeBodyContext(_ctx, getState());
		enterRule(_localctx, 224, RULE_annotationTypeBody);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2056); match(LBRACE);
			setState(2060);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ARGS) | (1L << AFTER) | (1L << AROUND) | (1L << ASPECT) | (1L << BEFORE) | (1L << CALL) | (1L << CFLOW) | (1L << CFLOWBELOW) | (1L << DECLARE) | (1L << ERROR) | (1L << EXECUTION) | (1L << GET) | (1L << HANDLER) | (1L << INITIALIZATION) | (1L << ISSINGLETON) | (1L << PARENTS) | (1L << PERCFLOW) | (1L << PERCFLOWBELOW) | (1L << PERTARGET) | (1L << PERTHIS) | (1L << PERTYPEWITHIN) | (1L << POINTCUT) | (1L << PRECEDENCE) | (1L << PREINITIALIZATION) | (1L << PRIVILEGED) | (1L << RETURNING) | (1L << SET) | (1L << SOFT) | (1L << STATICINITIALIZATION) | (1L << TARGET) | (1L << THROWING) | (1L << WARNING) | (1L << WITHIN) | (1L << WITHINCODE) | (1L << AT) | (1L << ABSTRACT) | (1L << BOOLEAN) | (1L << BYTE))) != 0) || ((((_la - 66)) & ~0x3f) == 0 && ((1L << (_la - 66)) & ((1L << (CHAR - 66)) | (1L << (CLASS - 66)) | (1L << (DOUBLE - 66)) | (1L << (ENUM - 66)) | (1L << (FINAL - 66)) | (1L << (FLOAT - 66)) | (1L << (INT - 66)) | (1L << (INTERFACE - 66)) | (1L << (LONG - 66)) | (1L << (NATIVE - 66)) | (1L << (PRIVATE - 66)) | (1L << (PROTECTED - 66)) | (1L << (PUBLIC - 66)) | (1L << (SHORT - 66)) | (1L << (STATIC - 66)) | (1L << (STRICTFP - 66)) | (1L << (SYNCHRONIZED - 66)) | (1L << (TRANSIENT - 66)) | (1L << (VOLATILE - 66)) | (1L << (SEMI - 66)))) != 0) || _la==Identifier) {
				{
				{
				setState(2057); annotationTypeElementDeclaration();
				}
				}
				setState(2062);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(2063); match(RBRACE);
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

	public static class AnnotationTypeElementDeclarationContext extends ParserRuleContext {
		public List<ModifierContext> modifier() {
			return getRuleContexts(ModifierContext.class);
		}
		public AnnotationTypeElementRestContext annotationTypeElementRest() {
			return getRuleContext(AnnotationTypeElementRestContext.class,0);
		}
		public ModifierContext modifier(int i) {
			return getRuleContext(ModifierContext.class,i);
		}
		public AnnotationTypeElementDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotationTypeElementDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterAnnotationTypeElementDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitAnnotationTypeElementDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitAnnotationTypeElementDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnnotationTypeElementDeclarationContext annotationTypeElementDeclaration() throws RecognitionException {
		AnnotationTypeElementDeclarationContext _localctx = new AnnotationTypeElementDeclarationContext(_ctx, getState());
		enterRule(_localctx, 226, RULE_annotationTypeElementDeclaration);
		try {
			int _alt;
			setState(2073);
			switch (_input.LA(1)) {
			case ARGS:
			case AFTER:
			case AROUND:
			case ASPECT:
			case BEFORE:
			case CALL:
			case CFLOW:
			case CFLOWBELOW:
			case DECLARE:
			case ERROR:
			case EXECUTION:
			case GET:
			case HANDLER:
			case INITIALIZATION:
			case ISSINGLETON:
			case PARENTS:
			case PERCFLOW:
			case PERCFLOWBELOW:
			case PERTARGET:
			case PERTHIS:
			case PERTYPEWITHIN:
			case POINTCUT:
			case PRECEDENCE:
			case PREINITIALIZATION:
			case PRIVILEGED:
			case RETURNING:
			case SET:
			case SOFT:
			case STATICINITIALIZATION:
			case TARGET:
			case THROWING:
			case WARNING:
			case WITHIN:
			case WITHINCODE:
			case AT:
			case ABSTRACT:
			case BOOLEAN:
			case BYTE:
			case CHAR:
			case CLASS:
			case DOUBLE:
			case ENUM:
			case FINAL:
			case FLOAT:
			case INT:
			case INTERFACE:
			case LONG:
			case NATIVE:
			case PRIVATE:
			case PROTECTED:
			case PUBLIC:
			case SHORT:
			case STATIC:
			case STRICTFP:
			case SYNCHRONIZED:
			case TRANSIENT:
			case VOLATILE:
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(2068);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,217,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(2065); modifier();
						}
						} 
					}
					setState(2070);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,217,_ctx);
				}
				setState(2071); annotationTypeElementRest();
				}
				break;
			case SEMI:
				enterOuterAlt(_localctx, 2);
				{
				setState(2072); match(SEMI);
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

	public static class AnnotationTypeElementRestContext extends ParserRuleContext {
		public EnumDeclarationContext enumDeclaration() {
			return getRuleContext(EnumDeclarationContext.class,0);
		}
		public ClassDeclarationContext classDeclaration() {
			return getRuleContext(ClassDeclarationContext.class,0);
		}
		public AnnotationMethodOrConstantRestContext annotationMethodOrConstantRest() {
			return getRuleContext(AnnotationMethodOrConstantRestContext.class,0);
		}
		public AnnotationTypeDeclarationContext annotationTypeDeclaration() {
			return getRuleContext(AnnotationTypeDeclarationContext.class,0);
		}
		public InterfaceDeclarationContext interfaceDeclaration() {
			return getRuleContext(InterfaceDeclarationContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public AnnotationTypeElementRestContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotationTypeElementRest; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterAnnotationTypeElementRest(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitAnnotationTypeElementRest(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitAnnotationTypeElementRest(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnnotationTypeElementRestContext annotationTypeElementRest() throws RecognitionException {
		AnnotationTypeElementRestContext _localctx = new AnnotationTypeElementRestContext(_ctx, getState());
		enterRule(_localctx, 228, RULE_annotationTypeElementRest);
		try {
			setState(2095);
			switch (_input.LA(1)) {
			case ARGS:
			case AFTER:
			case AROUND:
			case ASPECT:
			case BEFORE:
			case CALL:
			case CFLOW:
			case CFLOWBELOW:
			case DECLARE:
			case ERROR:
			case EXECUTION:
			case GET:
			case HANDLER:
			case INITIALIZATION:
			case ISSINGLETON:
			case PARENTS:
			case PERCFLOW:
			case PERCFLOWBELOW:
			case PERTARGET:
			case PERTHIS:
			case PERTYPEWITHIN:
			case POINTCUT:
			case PRECEDENCE:
			case PREINITIALIZATION:
			case PRIVILEGED:
			case RETURNING:
			case SET:
			case SOFT:
			case STATICINITIALIZATION:
			case TARGET:
			case THROWING:
			case WARNING:
			case WITHIN:
			case WITHINCODE:
			case BOOLEAN:
			case BYTE:
			case CHAR:
			case DOUBLE:
			case FLOAT:
			case INT:
			case LONG:
			case SHORT:
			case Identifier:
				enterOuterAlt(_localctx, 1);
				{
				setState(2075); type();
				setState(2076); annotationMethodOrConstantRest();
				setState(2077); match(SEMI);
				}
				break;
			case CLASS:
				enterOuterAlt(_localctx, 2);
				{
				setState(2079); classDeclaration();
				setState(2081);
				switch ( getInterpreter().adaptivePredict(_input,219,_ctx) ) {
				case 1:
					{
					setState(2080); match(SEMI);
					}
					break;
				}
				}
				break;
			case INTERFACE:
				enterOuterAlt(_localctx, 3);
				{
				setState(2083); interfaceDeclaration();
				setState(2085);
				switch ( getInterpreter().adaptivePredict(_input,220,_ctx) ) {
				case 1:
					{
					setState(2084); match(SEMI);
					}
					break;
				}
				}
				break;
			case ENUM:
				enterOuterAlt(_localctx, 4);
				{
				setState(2087); enumDeclaration();
				setState(2089);
				switch ( getInterpreter().adaptivePredict(_input,221,_ctx) ) {
				case 1:
					{
					setState(2088); match(SEMI);
					}
					break;
				}
				}
				break;
			case AT:
				enterOuterAlt(_localctx, 5);
				{
				setState(2091); annotationTypeDeclaration();
				setState(2093);
				switch ( getInterpreter().adaptivePredict(_input,222,_ctx) ) {
				case 1:
					{
					setState(2092); match(SEMI);
					}
					break;
				}
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

	public static class AnnotationMethodOrConstantRestContext extends ParserRuleContext {
		public AnnotationMethodRestContext annotationMethodRest() {
			return getRuleContext(AnnotationMethodRestContext.class,0);
		}
		public AnnotationConstantRestContext annotationConstantRest() {
			return getRuleContext(AnnotationConstantRestContext.class,0);
		}
		public AnnotationMethodOrConstantRestContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotationMethodOrConstantRest; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterAnnotationMethodOrConstantRest(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitAnnotationMethodOrConstantRest(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitAnnotationMethodOrConstantRest(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnnotationMethodOrConstantRestContext annotationMethodOrConstantRest() throws RecognitionException {
		AnnotationMethodOrConstantRestContext _localctx = new AnnotationMethodOrConstantRestContext(_ctx, getState());
		enterRule(_localctx, 230, RULE_annotationMethodOrConstantRest);
		try {
			setState(2099);
			switch ( getInterpreter().adaptivePredict(_input,224,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(2097); annotationMethodRest();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(2098); annotationConstantRest();
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

	public static class AnnotationConstantRestContext extends ParserRuleContext {
		public VariableDeclaratorsContext variableDeclarators() {
			return getRuleContext(VariableDeclaratorsContext.class,0);
		}
		public AnnotationConstantRestContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_annotationConstantRest; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterAnnotationConstantRest(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitAnnotationConstantRest(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitAnnotationConstantRest(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AnnotationConstantRestContext annotationConstantRest() throws RecognitionException {
		AnnotationConstantRestContext _localctx = new AnnotationConstantRestContext(_ctx, getState());
		enterRule(_localctx, 232, RULE_annotationConstantRest);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2101); variableDeclarators();
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

	public static class DefaultValueContext extends ParserRuleContext {
		public ElementValueContext elementValue() {
			return getRuleContext(ElementValueContext.class,0);
		}
		public DefaultValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_defaultValue; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterDefaultValue(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitDefaultValue(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitDefaultValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DefaultValueContext defaultValue() throws RecognitionException {
		DefaultValueContext _localctx = new DefaultValueContext(_ctx, getState());
		enterRule(_localctx, 234, RULE_defaultValue);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2103); match(DEFAULT);
			setState(2104); elementValue();
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

	public static class BlockContext extends ParserRuleContext {
		public List<BlockStatementContext> blockStatement() {
			return getRuleContexts(BlockStatementContext.class);
		}
		public BlockStatementContext blockStatement(int i) {
			return getRuleContext(BlockStatementContext.class,i);
		}
		public BlockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_block; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterBlock(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitBlock(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitBlock(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BlockContext block() throws RecognitionException {
		BlockContext _localctx = new BlockContext(_ctx, getState());
		enterRule(_localctx, 236, RULE_block);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2106); match(LBRACE);
			setState(2110);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ARGS) | (1L << AFTER) | (1L << AROUND) | (1L << ASPECT) | (1L << BEFORE) | (1L << CALL) | (1L << CFLOW) | (1L << CFLOWBELOW) | (1L << DECLARE) | (1L << ERROR) | (1L << EXECUTION) | (1L << GET) | (1L << HANDLER) | (1L << INITIALIZATION) | (1L << ISSINGLETON) | (1L << PARENTS) | (1L << PERCFLOW) | (1L << PERCFLOWBELOW) | (1L << PERTARGET) | (1L << PERTHIS) | (1L << PERTYPEWITHIN) | (1L << POINTCUT) | (1L << PRECEDENCE) | (1L << PREINITIALIZATION) | (1L << PRIVILEGED) | (1L << RETURNING) | (1L << SET) | (1L << SOFT) | (1L << STATICINITIALIZATION) | (1L << TARGET) | (1L << THROWING) | (1L << WARNING) | (1L << WITHIN) | (1L << WITHINCODE) | (1L << AT) | (1L << ABSTRACT) | (1L << ASSERT) | (1L << BOOLEAN) | (1L << BREAK) | (1L << BYTE))) != 0) || ((((_la - 66)) & ~0x3f) == 0 && ((1L << (_la - 66)) & ((1L << (CHAR - 66)) | (1L << (CLASS - 66)) | (1L << (CONTINUE - 66)) | (1L << (DO - 66)) | (1L << (DOUBLE - 66)) | (1L << (ENUM - 66)) | (1L << (FINAL - 66)) | (1L << (FLOAT - 66)) | (1L << (FOR - 66)) | (1L << (IF - 66)) | (1L << (INT - 66)) | (1L << (INTERFACE - 66)) | (1L << (LONG - 66)) | (1L << (NATIVE - 66)) | (1L << (NEW - 66)) | (1L << (PRIVATE - 66)) | (1L << (PROTECTED - 66)) | (1L << (PUBLIC - 66)) | (1L << (RETURN - 66)) | (1L << (SHORT - 66)) | (1L << (STATIC - 66)) | (1L << (STRICTFP - 66)) | (1L << (SUPER - 66)) | (1L << (SWITCH - 66)) | (1L << (SYNCHRONIZED - 66)) | (1L << (THIS - 66)) | (1L << (THROW - 66)) | (1L << (TRANSIENT - 66)) | (1L << (TRY - 66)) | (1L << (VOID - 66)) | (1L << (VOLATILE - 66)) | (1L << (WHILE - 66)) | (1L << (IntegerLiteral - 66)) | (1L << (FloatingPointLiteral - 66)) | (1L << (BooleanLiteral - 66)) | (1L << (CharacterLiteral - 66)) | (1L << (StringLiteral - 66)) | (1L << (NullLiteral - 66)) | (1L << (LPAREN - 66)) | (1L << (LBRACE - 66)) | (1L << (SEMI - 66)) | (1L << (LT - 66)) | (1L << (BANG - 66)) | (1L << (TILDE - 66)))) != 0) || ((((_la - 137)) & ~0x3f) == 0 && ((1L << (_la - 137)) & ((1L << (INC - 137)) | (1L << (DEC - 137)) | (1L << (ADD - 137)) | (1L << (SUB - 137)) | (1L << (Identifier - 137)))) != 0)) {
				{
				{
				setState(2107); blockStatement();
				}
				}
				setState(2112);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(2113); match(RBRACE);
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

	public static class BlockStatementContext extends ParserRuleContext {
		public TypeDeclarationContext typeDeclaration() {
			return getRuleContext(TypeDeclarationContext.class,0);
		}
		public StatementContext statement() {
			return getRuleContext(StatementContext.class,0);
		}
		public LocalVariableDeclarationStatementContext localVariableDeclarationStatement() {
			return getRuleContext(LocalVariableDeclarationStatementContext.class,0);
		}
		public BlockStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_blockStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterBlockStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitBlockStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitBlockStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BlockStatementContext blockStatement() throws RecognitionException {
		BlockStatementContext _localctx = new BlockStatementContext(_ctx, getState());
		enterRule(_localctx, 238, RULE_blockStatement);
		try {
			setState(2118);
			switch ( getInterpreter().adaptivePredict(_input,226,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(2115); localVariableDeclarationStatement();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(2116); statement();
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(2117); typeDeclaration();
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

	public static class LocalVariableDeclarationStatementContext extends ParserRuleContext {
		public LocalVariableDeclarationContext localVariableDeclaration() {
			return getRuleContext(LocalVariableDeclarationContext.class,0);
		}
		public LocalVariableDeclarationStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_localVariableDeclarationStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterLocalVariableDeclarationStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitLocalVariableDeclarationStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitLocalVariableDeclarationStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LocalVariableDeclarationStatementContext localVariableDeclarationStatement() throws RecognitionException {
		LocalVariableDeclarationStatementContext _localctx = new LocalVariableDeclarationStatementContext(_ctx, getState());
		enterRule(_localctx, 240, RULE_localVariableDeclarationStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2120); localVariableDeclaration();
			setState(2121); match(SEMI);
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

	public static class LocalVariableDeclarationContext extends ParserRuleContext {
		public VariableModifierContext variableModifier(int i) {
			return getRuleContext(VariableModifierContext.class,i);
		}
		public List<VariableModifierContext> variableModifier() {
			return getRuleContexts(VariableModifierContext.class);
		}
		public VariableDeclaratorsContext variableDeclarators() {
			return getRuleContext(VariableDeclaratorsContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public LocalVariableDeclarationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_localVariableDeclaration; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterLocalVariableDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitLocalVariableDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitLocalVariableDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LocalVariableDeclarationContext localVariableDeclaration() throws RecognitionException {
		LocalVariableDeclarationContext _localctx = new LocalVariableDeclarationContext(_ctx, getState());
		enterRule(_localctx, 242, RULE_localVariableDeclaration);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2126);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==FINAL) {
				{
				{
				setState(2123); variableModifier();
				}
				}
				setState(2128);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(2129); type();
			setState(2130); variableDeclarators();
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

	public static class CatchTypeContext extends ParserRuleContext {
		public List<QualifiedNameContext> qualifiedName() {
			return getRuleContexts(QualifiedNameContext.class);
		}
		public QualifiedNameContext qualifiedName(int i) {
			return getRuleContext(QualifiedNameContext.class,i);
		}
		public CatchTypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_catchType; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterCatchType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitCatchType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitCatchType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CatchTypeContext catchType() throws RecognitionException {
		CatchTypeContext _localctx = new CatchTypeContext(_ctx, getState());
		enterRule(_localctx, 244, RULE_catchType);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2132); qualifiedName();
			setState(2137);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==BITOR) {
				{
				{
				setState(2133); match(BITOR);
				setState(2134); qualifiedName();
				}
				}
				setState(2139);
				_errHandler.sync(this);
				_la = _input.LA(1);
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

	public static class FinallyBlockContext extends ParserRuleContext {
		public BlockContext block() {
			return getRuleContext(BlockContext.class,0);
		}
		public FinallyBlockContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_finallyBlock; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterFinallyBlock(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitFinallyBlock(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitFinallyBlock(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FinallyBlockContext finallyBlock() throws RecognitionException {
		FinallyBlockContext _localctx = new FinallyBlockContext(_ctx, getState());
		enterRule(_localctx, 246, RULE_finallyBlock);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2140); match(FINALLY);
			setState(2141); block();
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

	public static class ResourceSpecificationContext extends ParserRuleContext {
		public ResourcesContext resources() {
			return getRuleContext(ResourcesContext.class,0);
		}
		public ResourceSpecificationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_resourceSpecification; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterResourceSpecification(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitResourceSpecification(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitResourceSpecification(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ResourceSpecificationContext resourceSpecification() throws RecognitionException {
		ResourceSpecificationContext _localctx = new ResourceSpecificationContext(_ctx, getState());
		enterRule(_localctx, 248, RULE_resourceSpecification);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2143); match(LPAREN);
			setState(2144); resources();
			setState(2146);
			_la = _input.LA(1);
			if (_la==SEMI) {
				{
				setState(2145); match(SEMI);
				}
			}

			setState(2148); match(RPAREN);
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

	public static class ResourcesContext extends ParserRuleContext {
		public ResourceContext resource(int i) {
			return getRuleContext(ResourceContext.class,i);
		}
		public List<ResourceContext> resource() {
			return getRuleContexts(ResourceContext.class);
		}
		public ResourcesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_resources; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterResources(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitResources(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitResources(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ResourcesContext resources() throws RecognitionException {
		ResourcesContext _localctx = new ResourcesContext(_ctx, getState());
		enterRule(_localctx, 250, RULE_resources);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(2150); resource();
			setState(2155);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,230,_ctx);
			while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(2151); match(SEMI);
					setState(2152); resource();
					}
					} 
				}
				setState(2157);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,230,_ctx);
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

	public static class ResourceContext extends ParserRuleContext {
		public VariableModifierContext variableModifier(int i) {
			return getRuleContext(VariableModifierContext.class,i);
		}
		public List<VariableModifierContext> variableModifier() {
			return getRuleContexts(VariableModifierContext.class);
		}
		public ClassOrInterfaceTypeContext classOrInterfaceType() {
			return getRuleContext(ClassOrInterfaceTypeContext.class,0);
		}
		public VariableDeclaratorIdContext variableDeclaratorId() {
			return getRuleContext(VariableDeclaratorIdContext.class,0);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public ResourceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_resource; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterResource(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitResource(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitResource(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ResourceContext resource() throws RecognitionException {
		ResourceContext _localctx = new ResourceContext(_ctx, getState());
		enterRule(_localctx, 252, RULE_resource);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2161);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==FINAL) {
				{
				{
				setState(2158); variableModifier();
				}
				}
				setState(2163);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(2164); classOrInterfaceType();
			setState(2165); variableDeclaratorId();
			setState(2166); match(ASSIGN);
			setState(2167); expression(0);
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

	public static class SwitchBlockStatementGroupContext extends ParserRuleContext {
		public List<BlockStatementContext> blockStatement() {
			return getRuleContexts(BlockStatementContext.class);
		}
		public List<SwitchLabelContext> switchLabel() {
			return getRuleContexts(SwitchLabelContext.class);
		}
		public BlockStatementContext blockStatement(int i) {
			return getRuleContext(BlockStatementContext.class,i);
		}
		public SwitchLabelContext switchLabel(int i) {
			return getRuleContext(SwitchLabelContext.class,i);
		}
		public SwitchBlockStatementGroupContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_switchBlockStatementGroup; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterSwitchBlockStatementGroup(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitSwitchBlockStatementGroup(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitSwitchBlockStatementGroup(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SwitchBlockStatementGroupContext switchBlockStatementGroup() throws RecognitionException {
		SwitchBlockStatementGroupContext _localctx = new SwitchBlockStatementGroupContext(_ctx, getState());
		enterRule(_localctx, 254, RULE_switchBlockStatementGroup);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2170); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(2169); switchLabel();
				}
				}
				setState(2172); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==CASE || _la==DEFAULT );
			setState(2175); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(2174); blockStatement();
				}
				}
				setState(2177); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ARGS) | (1L << AFTER) | (1L << AROUND) | (1L << ASPECT) | (1L << BEFORE) | (1L << CALL) | (1L << CFLOW) | (1L << CFLOWBELOW) | (1L << DECLARE) | (1L << ERROR) | (1L << EXECUTION) | (1L << GET) | (1L << HANDLER) | (1L << INITIALIZATION) | (1L << ISSINGLETON) | (1L << PARENTS) | (1L << PERCFLOW) | (1L << PERCFLOWBELOW) | (1L << PERTARGET) | (1L << PERTHIS) | (1L << PERTYPEWITHIN) | (1L << POINTCUT) | (1L << PRECEDENCE) | (1L << PREINITIALIZATION) | (1L << PRIVILEGED) | (1L << RETURNING) | (1L << SET) | (1L << SOFT) | (1L << STATICINITIALIZATION) | (1L << TARGET) | (1L << THROWING) | (1L << WARNING) | (1L << WITHIN) | (1L << WITHINCODE) | (1L << AT) | (1L << ABSTRACT) | (1L << ASSERT) | (1L << BOOLEAN) | (1L << BREAK) | (1L << BYTE))) != 0) || ((((_la - 66)) & ~0x3f) == 0 && ((1L << (_la - 66)) & ((1L << (CHAR - 66)) | (1L << (CLASS - 66)) | (1L << (CONTINUE - 66)) | (1L << (DO - 66)) | (1L << (DOUBLE - 66)) | (1L << (ENUM - 66)) | (1L << (FINAL - 66)) | (1L << (FLOAT - 66)) | (1L << (FOR - 66)) | (1L << (IF - 66)) | (1L << (INT - 66)) | (1L << (INTERFACE - 66)) | (1L << (LONG - 66)) | (1L << (NATIVE - 66)) | (1L << (NEW - 66)) | (1L << (PRIVATE - 66)) | (1L << (PROTECTED - 66)) | (1L << (PUBLIC - 66)) | (1L << (RETURN - 66)) | (1L << (SHORT - 66)) | (1L << (STATIC - 66)) | (1L << (STRICTFP - 66)) | (1L << (SUPER - 66)) | (1L << (SWITCH - 66)) | (1L << (SYNCHRONIZED - 66)) | (1L << (THIS - 66)) | (1L << (THROW - 66)) | (1L << (TRANSIENT - 66)) | (1L << (TRY - 66)) | (1L << (VOID - 66)) | (1L << (VOLATILE - 66)) | (1L << (WHILE - 66)) | (1L << (IntegerLiteral - 66)) | (1L << (FloatingPointLiteral - 66)) | (1L << (BooleanLiteral - 66)) | (1L << (CharacterLiteral - 66)) | (1L << (StringLiteral - 66)) | (1L << (NullLiteral - 66)) | (1L << (LPAREN - 66)) | (1L << (LBRACE - 66)) | (1L << (SEMI - 66)) | (1L << (LT - 66)) | (1L << (BANG - 66)) | (1L << (TILDE - 66)))) != 0) || ((((_la - 137)) & ~0x3f) == 0 && ((1L << (_la - 137)) & ((1L << (INC - 137)) | (1L << (DEC - 137)) | (1L << (ADD - 137)) | (1L << (SUB - 137)) | (1L << (Identifier - 137)))) != 0) );
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

	public static class SwitchLabelContext extends ParserRuleContext {
		public ConstantExpressionContext constantExpression() {
			return getRuleContext(ConstantExpressionContext.class,0);
		}
		public EnumConstantNameContext enumConstantName() {
			return getRuleContext(EnumConstantNameContext.class,0);
		}
		public SwitchLabelContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_switchLabel; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterSwitchLabel(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitSwitchLabel(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitSwitchLabel(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SwitchLabelContext switchLabel() throws RecognitionException {
		SwitchLabelContext _localctx = new SwitchLabelContext(_ctx, getState());
		enterRule(_localctx, 256, RULE_switchLabel);
		try {
			setState(2189);
			switch ( getInterpreter().adaptivePredict(_input,234,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(2179); match(CASE);
				setState(2180); constantExpression();
				setState(2181); match(COLON);
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(2183); match(CASE);
				setState(2184); enumConstantName();
				setState(2185); match(COLON);
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(2187); match(DEFAULT);
				setState(2188); match(COLON);
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

	public static class ForControlContext extends ParserRuleContext {
		public ForUpdateContext forUpdate() {
			return getRuleContext(ForUpdateContext.class,0);
		}
		public ForInitContext forInit() {
			return getRuleContext(ForInitContext.class,0);
		}
		public EnhancedForControlContext enhancedForControl() {
			return getRuleContext(EnhancedForControlContext.class,0);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public ForControlContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_forControl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterForControl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitForControl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitForControl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ForControlContext forControl() throws RecognitionException {
		ForControlContext _localctx = new ForControlContext(_ctx, getState());
		enterRule(_localctx, 258, RULE_forControl);
		int _la;
		try {
			setState(2203);
			switch ( getInterpreter().adaptivePredict(_input,238,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(2191); enhancedForControl();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(2193);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ARGS) | (1L << AFTER) | (1L << AROUND) | (1L << ASPECT) | (1L << BEFORE) | (1L << CALL) | (1L << CFLOW) | (1L << CFLOWBELOW) | (1L << DECLARE) | (1L << ERROR) | (1L << EXECUTION) | (1L << GET) | (1L << HANDLER) | (1L << INITIALIZATION) | (1L << ISSINGLETON) | (1L << PARENTS) | (1L << PERCFLOW) | (1L << PERCFLOWBELOW) | (1L << PERTARGET) | (1L << PERTHIS) | (1L << PERTYPEWITHIN) | (1L << POINTCUT) | (1L << PRECEDENCE) | (1L << PREINITIALIZATION) | (1L << PRIVILEGED) | (1L << RETURNING) | (1L << SET) | (1L << SOFT) | (1L << STATICINITIALIZATION) | (1L << TARGET) | (1L << THROWING) | (1L << WARNING) | (1L << WITHIN) | (1L << WITHINCODE) | (1L << AT) | (1L << BOOLEAN) | (1L << BYTE))) != 0) || ((((_la - 66)) & ~0x3f) == 0 && ((1L << (_la - 66)) & ((1L << (CHAR - 66)) | (1L << (DOUBLE - 66)) | (1L << (FINAL - 66)) | (1L << (FLOAT - 66)) | (1L << (INT - 66)) | (1L << (LONG - 66)) | (1L << (NEW - 66)) | (1L << (SHORT - 66)) | (1L << (SUPER - 66)) | (1L << (THIS - 66)) | (1L << (VOID - 66)) | (1L << (IntegerLiteral - 66)) | (1L << (FloatingPointLiteral - 66)) | (1L << (BooleanLiteral - 66)) | (1L << (CharacterLiteral - 66)) | (1L << (StringLiteral - 66)) | (1L << (NullLiteral - 66)) | (1L << (LPAREN - 66)) | (1L << (LT - 66)) | (1L << (BANG - 66)) | (1L << (TILDE - 66)))) != 0) || ((((_la - 137)) & ~0x3f) == 0 && ((1L << (_la - 137)) & ((1L << (INC - 137)) | (1L << (DEC - 137)) | (1L << (ADD - 137)) | (1L << (SUB - 137)) | (1L << (Identifier - 137)))) != 0)) {
					{
					setState(2192); forInit();
					}
				}

				setState(2195); match(SEMI);
				setState(2197);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ARGS) | (1L << AFTER) | (1L << AROUND) | (1L << ASPECT) | (1L << BEFORE) | (1L << CALL) | (1L << CFLOW) | (1L << CFLOWBELOW) | (1L << DECLARE) | (1L << ERROR) | (1L << EXECUTION) | (1L << GET) | (1L << HANDLER) | (1L << INITIALIZATION) | (1L << ISSINGLETON) | (1L << PARENTS) | (1L << PERCFLOW) | (1L << PERCFLOWBELOW) | (1L << PERTARGET) | (1L << PERTHIS) | (1L << PERTYPEWITHIN) | (1L << POINTCUT) | (1L << PRECEDENCE) | (1L << PREINITIALIZATION) | (1L << PRIVILEGED) | (1L << RETURNING) | (1L << SET) | (1L << SOFT) | (1L << STATICINITIALIZATION) | (1L << TARGET) | (1L << THROWING) | (1L << WARNING) | (1L << WITHIN) | (1L << WITHINCODE) | (1L << BOOLEAN) | (1L << BYTE))) != 0) || ((((_la - 66)) & ~0x3f) == 0 && ((1L << (_la - 66)) & ((1L << (CHAR - 66)) | (1L << (DOUBLE - 66)) | (1L << (FLOAT - 66)) | (1L << (INT - 66)) | (1L << (LONG - 66)) | (1L << (NEW - 66)) | (1L << (SHORT - 66)) | (1L << (SUPER - 66)) | (1L << (THIS - 66)) | (1L << (VOID - 66)) | (1L << (IntegerLiteral - 66)) | (1L << (FloatingPointLiteral - 66)) | (1L << (BooleanLiteral - 66)) | (1L << (CharacterLiteral - 66)) | (1L << (StringLiteral - 66)) | (1L << (NullLiteral - 66)) | (1L << (LPAREN - 66)) | (1L << (LT - 66)) | (1L << (BANG - 66)) | (1L << (TILDE - 66)))) != 0) || ((((_la - 137)) & ~0x3f) == 0 && ((1L << (_la - 137)) & ((1L << (INC - 137)) | (1L << (DEC - 137)) | (1L << (ADD - 137)) | (1L << (SUB - 137)) | (1L << (Identifier - 137)))) != 0)) {
					{
					setState(2196); expression(0);
					}
				}

				setState(2199); match(SEMI);
				setState(2201);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ARGS) | (1L << AFTER) | (1L << AROUND) | (1L << ASPECT) | (1L << BEFORE) | (1L << CALL) | (1L << CFLOW) | (1L << CFLOWBELOW) | (1L << DECLARE) | (1L << ERROR) | (1L << EXECUTION) | (1L << GET) | (1L << HANDLER) | (1L << INITIALIZATION) | (1L << ISSINGLETON) | (1L << PARENTS) | (1L << PERCFLOW) | (1L << PERCFLOWBELOW) | (1L << PERTARGET) | (1L << PERTHIS) | (1L << PERTYPEWITHIN) | (1L << POINTCUT) | (1L << PRECEDENCE) | (1L << PREINITIALIZATION) | (1L << PRIVILEGED) | (1L << RETURNING) | (1L << SET) | (1L << SOFT) | (1L << STATICINITIALIZATION) | (1L << TARGET) | (1L << THROWING) | (1L << WARNING) | (1L << WITHIN) | (1L << WITHINCODE) | (1L << BOOLEAN) | (1L << BYTE))) != 0) || ((((_la - 66)) & ~0x3f) == 0 && ((1L << (_la - 66)) & ((1L << (CHAR - 66)) | (1L << (DOUBLE - 66)) | (1L << (FLOAT - 66)) | (1L << (INT - 66)) | (1L << (LONG - 66)) | (1L << (NEW - 66)) | (1L << (SHORT - 66)) | (1L << (SUPER - 66)) | (1L << (THIS - 66)) | (1L << (VOID - 66)) | (1L << (IntegerLiteral - 66)) | (1L << (FloatingPointLiteral - 66)) | (1L << (BooleanLiteral - 66)) | (1L << (CharacterLiteral - 66)) | (1L << (StringLiteral - 66)) | (1L << (NullLiteral - 66)) | (1L << (LPAREN - 66)) | (1L << (LT - 66)) | (1L << (BANG - 66)) | (1L << (TILDE - 66)))) != 0) || ((((_la - 137)) & ~0x3f) == 0 && ((1L << (_la - 137)) & ((1L << (INC - 137)) | (1L << (DEC - 137)) | (1L << (ADD - 137)) | (1L << (SUB - 137)) | (1L << (Identifier - 137)))) != 0)) {
					{
					setState(2200); forUpdate();
					}
				}

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

	public static class ForInitContext extends ParserRuleContext {
		public LocalVariableDeclarationContext localVariableDeclaration() {
			return getRuleContext(LocalVariableDeclarationContext.class,0);
		}
		public ExpressionListContext expressionList() {
			return getRuleContext(ExpressionListContext.class,0);
		}
		public ForInitContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_forInit; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterForInit(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitForInit(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitForInit(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ForInitContext forInit() throws RecognitionException {
		ForInitContext _localctx = new ForInitContext(_ctx, getState());
		enterRule(_localctx, 260, RULE_forInit);
		try {
			setState(2207);
			switch ( getInterpreter().adaptivePredict(_input,239,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(2205); localVariableDeclaration();
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(2206); expressionList();
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

	public static class EnhancedForControlContext extends ParserRuleContext {
		public VariableModifierContext variableModifier(int i) {
			return getRuleContext(VariableModifierContext.class,i);
		}
		public List<VariableModifierContext> variableModifier() {
			return getRuleContexts(VariableModifierContext.class);
		}
		public VariableDeclaratorIdContext variableDeclaratorId() {
			return getRuleContext(VariableDeclaratorIdContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public EnhancedForControlContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enhancedForControl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterEnhancedForControl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitEnhancedForControl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitEnhancedForControl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EnhancedForControlContext enhancedForControl() throws RecognitionException {
		EnhancedForControlContext _localctx = new EnhancedForControlContext(_ctx, getState());
		enterRule(_localctx, 262, RULE_enhancedForControl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2212);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AT || _la==FINAL) {
				{
				{
				setState(2209); variableModifier();
				}
				}
				setState(2214);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(2215); type();
			setState(2216); variableDeclaratorId();
			setState(2217); match(COLON);
			setState(2218); expression(0);
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

	public static class ForUpdateContext extends ParserRuleContext {
		public ExpressionListContext expressionList() {
			return getRuleContext(ExpressionListContext.class,0);
		}
		public ForUpdateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_forUpdate; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterForUpdate(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitForUpdate(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitForUpdate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ForUpdateContext forUpdate() throws RecognitionException {
		ForUpdateContext _localctx = new ForUpdateContext(_ctx, getState());
		enterRule(_localctx, 264, RULE_forUpdate);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2220); expressionList();
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

	public static class ParExpressionContext extends ParserRuleContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public ParExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_parExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterParExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitParExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitParExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ParExpressionContext parExpression() throws RecognitionException {
		ParExpressionContext _localctx = new ParExpressionContext(_ctx, getState());
		enterRule(_localctx, 266, RULE_parExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2222); match(LPAREN);
			setState(2223); expression(0);
			setState(2224); match(RPAREN);
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

	public static class ExpressionListContext extends ParserRuleContext {
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ExpressionListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expressionList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterExpressionList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitExpressionList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitExpressionList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExpressionListContext expressionList() throws RecognitionException {
		ExpressionListContext _localctx = new ExpressionListContext(_ctx, getState());
		enterRule(_localctx, 268, RULE_expressionList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2226); expression(0);
			setState(2231);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(2227); match(COMMA);
				setState(2228); expression(0);
				}
				}
				setState(2233);
				_errHandler.sync(this);
				_la = _input.LA(1);
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

	public static class StatementExpressionContext extends ParserRuleContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public StatementExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_statementExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterStatementExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitStatementExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitStatementExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StatementExpressionContext statementExpression() throws RecognitionException {
		StatementExpressionContext _localctx = new StatementExpressionContext(_ctx, getState());
		enterRule(_localctx, 270, RULE_statementExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2234); expression(0);
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

	public static class ConstantExpressionContext extends ParserRuleContext {
		public ExpressionContext expression() {
			return getRuleContext(ExpressionContext.class,0);
		}
		public ConstantExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constantExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterConstantExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitConstantExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitConstantExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstantExpressionContext constantExpression() throws RecognitionException {
		ConstantExpressionContext _localctx = new ConstantExpressionContext(_ctx, getState());
		enterRule(_localctx, 272, RULE_constantExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2236); expression(0);
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

	public static class CreatorContext extends ParserRuleContext {
		public ArrayCreatorRestContext arrayCreatorRest() {
			return getRuleContext(ArrayCreatorRestContext.class,0);
		}
		public NonWildcardTypeArgumentsContext nonWildcardTypeArguments() {
			return getRuleContext(NonWildcardTypeArgumentsContext.class,0);
		}
		public ClassCreatorRestContext classCreatorRest() {
			return getRuleContext(ClassCreatorRestContext.class,0);
		}
		public CreatedNameContext createdName() {
			return getRuleContext(CreatedNameContext.class,0);
		}
		public CreatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_creator; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterCreator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitCreator(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitCreator(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CreatorContext creator() throws RecognitionException {
		CreatorContext _localctx = new CreatorContext(_ctx, getState());
		enterRule(_localctx, 274, RULE_creator);
		try {
			setState(2247);
			switch (_input.LA(1)) {
			case LT:
				enterOuterAlt(_localctx, 1);
				{
				setState(2238); nonWildcardTypeArguments();
				setState(2239); createdName();
				setState(2240); classCreatorRest();
				}
				break;
			case ARGS:
			case AFTER:
			case AROUND:
			case ASPECT:
			case BEFORE:
			case CALL:
			case CFLOW:
			case CFLOWBELOW:
			case DECLARE:
			case ERROR:
			case EXECUTION:
			case GET:
			case HANDLER:
			case INITIALIZATION:
			case ISSINGLETON:
			case PARENTS:
			case PERCFLOW:
			case PERCFLOWBELOW:
			case PERTARGET:
			case PERTHIS:
			case PERTYPEWITHIN:
			case POINTCUT:
			case PRECEDENCE:
			case PREINITIALIZATION:
			case PRIVILEGED:
			case RETURNING:
			case SET:
			case SOFT:
			case STATICINITIALIZATION:
			case TARGET:
			case THROWING:
			case WARNING:
			case WITHIN:
			case WITHINCODE:
			case BOOLEAN:
			case BYTE:
			case CHAR:
			case DOUBLE:
			case FLOAT:
			case INT:
			case LONG:
			case SHORT:
			case Identifier:
				enterOuterAlt(_localctx, 2);
				{
				setState(2242); createdName();
				setState(2245);
				switch (_input.LA(1)) {
				case LBRACK:
					{
					setState(2243); arrayCreatorRest();
					}
					break;
				case LPAREN:
					{
					setState(2244); classCreatorRest();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
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

	public static class ArrayCreatorRestContext extends ParserRuleContext {
		public ArrayInitializerContext arrayInitializer() {
			return getRuleContext(ArrayInitializerContext.class,0);
		}
		public ExpressionContext expression(int i) {
			return getRuleContext(ExpressionContext.class,i);
		}
		public List<ExpressionContext> expression() {
			return getRuleContexts(ExpressionContext.class);
		}
		public ArrayCreatorRestContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arrayCreatorRest; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterArrayCreatorRest(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitArrayCreatorRest(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitArrayCreatorRest(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArrayCreatorRestContext arrayCreatorRest() throws RecognitionException {
		ArrayCreatorRestContext _localctx = new ArrayCreatorRestContext(_ctx, getState());
		enterRule(_localctx, 276, RULE_arrayCreatorRest);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(2249); match(LBRACK);
			setState(2277);
			switch (_input.LA(1)) {
			case RBRACK:
				{
				setState(2250); match(RBRACK);
				setState(2255);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==LBRACK) {
					{
					{
					setState(2251); match(LBRACK);
					setState(2252); match(RBRACK);
					}
					}
					setState(2257);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(2258); arrayInitializer();
				}
				break;
			case ARGS:
			case AFTER:
			case AROUND:
			case ASPECT:
			case BEFORE:
			case CALL:
			case CFLOW:
			case CFLOWBELOW:
			case DECLARE:
			case ERROR:
			case EXECUTION:
			case GET:
			case HANDLER:
			case INITIALIZATION:
			case ISSINGLETON:
			case PARENTS:
			case PERCFLOW:
			case PERCFLOWBELOW:
			case PERTARGET:
			case PERTHIS:
			case PERTYPEWITHIN:
			case POINTCUT:
			case PRECEDENCE:
			case PREINITIALIZATION:
			case PRIVILEGED:
			case RETURNING:
			case SET:
			case SOFT:
			case STATICINITIALIZATION:
			case TARGET:
			case THROWING:
			case WARNING:
			case WITHIN:
			case WITHINCODE:
			case BOOLEAN:
			case BYTE:
			case CHAR:
			case DOUBLE:
			case FLOAT:
			case INT:
			case LONG:
			case NEW:
			case SHORT:
			case SUPER:
			case THIS:
			case VOID:
			case IntegerLiteral:
			case FloatingPointLiteral:
			case BooleanLiteral:
			case CharacterLiteral:
			case StringLiteral:
			case NullLiteral:
			case LPAREN:
			case LT:
			case BANG:
			case TILDE:
			case INC:
			case DEC:
			case ADD:
			case SUB:
			case Identifier:
				{
				setState(2259); expression(0);
				setState(2260); match(RBRACK);
				setState(2267);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,245,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(2261); match(LBRACK);
						setState(2262); expression(0);
						setState(2263); match(RBRACK);
						}
						} 
					}
					setState(2269);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,245,_ctx);
				}
				setState(2274);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,246,_ctx);
				while ( _alt!=2 && _alt!=ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(2270); match(LBRACK);
						setState(2271); match(RBRACK);
						}
						} 
					}
					setState(2276);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,246,_ctx);
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
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

	public static class ClassCreatorRestContext extends ParserRuleContext {
		public ClassBodyContext classBody() {
			return getRuleContext(ClassBodyContext.class,0);
		}
		public ArgumentsContext arguments() {
			return getRuleContext(ArgumentsContext.class,0);
		}
		public ClassCreatorRestContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_classCreatorRest; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterClassCreatorRest(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitClassCreatorRest(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitClassCreatorRest(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ClassCreatorRestContext classCreatorRest() throws RecognitionException {
		ClassCreatorRestContext _localctx = new ClassCreatorRestContext(_ctx, getState());
		enterRule(_localctx, 278, RULE_classCreatorRest);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2279); arguments();
			setState(2281);
			switch ( getInterpreter().adaptivePredict(_input,248,_ctx) ) {
			case 1:
				{
				setState(2280); classBody();
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

	public static class ExplicitGenericInvocationContext extends ParserRuleContext {
		public NonWildcardTypeArgumentsContext nonWildcardTypeArguments() {
			return getRuleContext(NonWildcardTypeArgumentsContext.class,0);
		}
		public ExplicitGenericInvocationSuffixContext explicitGenericInvocationSuffix() {
			return getRuleContext(ExplicitGenericInvocationSuffixContext.class,0);
		}
		public ExplicitGenericInvocationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_explicitGenericInvocation; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterExplicitGenericInvocation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitExplicitGenericInvocation(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitExplicitGenericInvocation(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExplicitGenericInvocationContext explicitGenericInvocation() throws RecognitionException {
		ExplicitGenericInvocationContext _localctx = new ExplicitGenericInvocationContext(_ctx, getState());
		enterRule(_localctx, 280, RULE_explicitGenericInvocation);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2283); nonWildcardTypeArguments();
			setState(2284); explicitGenericInvocationSuffix();
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

	public static class NonWildcardTypeArgumentsContext extends ParserRuleContext {
		public TypeListContext typeList() {
			return getRuleContext(TypeListContext.class,0);
		}
		public NonWildcardTypeArgumentsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nonWildcardTypeArguments; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterNonWildcardTypeArguments(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitNonWildcardTypeArguments(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitNonWildcardTypeArguments(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NonWildcardTypeArgumentsContext nonWildcardTypeArguments() throws RecognitionException {
		NonWildcardTypeArgumentsContext _localctx = new NonWildcardTypeArgumentsContext(_ctx, getState());
		enterRule(_localctx, 282, RULE_nonWildcardTypeArguments);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2286); match(LT);
			setState(2287); typeList();
			setState(2288); match(GT);
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

	public static class TypeArgumentsOrDiamondContext extends ParserRuleContext {
		public TypeArgumentsContext typeArguments() {
			return getRuleContext(TypeArgumentsContext.class,0);
		}
		public TypeArgumentsOrDiamondContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeArgumentsOrDiamond; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterTypeArgumentsOrDiamond(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitTypeArgumentsOrDiamond(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitTypeArgumentsOrDiamond(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeArgumentsOrDiamondContext typeArgumentsOrDiamond() throws RecognitionException {
		TypeArgumentsOrDiamondContext _localctx = new TypeArgumentsOrDiamondContext(_ctx, getState());
		enterRule(_localctx, 284, RULE_typeArgumentsOrDiamond);
		try {
			setState(2293);
			switch ( getInterpreter().adaptivePredict(_input,249,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(2290); match(LT);
				setState(2291); match(GT);
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(2292); typeArguments();
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

	public static class NonWildcardTypeArgumentsOrDiamondContext extends ParserRuleContext {
		public NonWildcardTypeArgumentsContext nonWildcardTypeArguments() {
			return getRuleContext(NonWildcardTypeArgumentsContext.class,0);
		}
		public NonWildcardTypeArgumentsOrDiamondContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nonWildcardTypeArgumentsOrDiamond; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterNonWildcardTypeArgumentsOrDiamond(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitNonWildcardTypeArgumentsOrDiamond(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitNonWildcardTypeArgumentsOrDiamond(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NonWildcardTypeArgumentsOrDiamondContext nonWildcardTypeArgumentsOrDiamond() throws RecognitionException {
		NonWildcardTypeArgumentsOrDiamondContext _localctx = new NonWildcardTypeArgumentsOrDiamondContext(_ctx, getState());
		enterRule(_localctx, 286, RULE_nonWildcardTypeArgumentsOrDiamond);
		try {
			setState(2298);
			switch ( getInterpreter().adaptivePredict(_input,250,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(2295); match(LT);
				setState(2296); match(GT);
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(2297); nonWildcardTypeArguments();
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

	public static class ArgumentsContext extends ParserRuleContext {
		public ExpressionListContext expressionList() {
			return getRuleContext(ExpressionListContext.class,0);
		}
		public ArgumentsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arguments; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).enterArguments(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof AspectJParserListener ) ((AspectJParserListener)listener).exitArguments(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof AspectJParserVisitor ) return ((AspectJParserVisitor<? extends T>)visitor).visitArguments(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArgumentsContext arguments() throws RecognitionException {
		ArgumentsContext _localctx = new ArgumentsContext(_ctx, getState());
		enterRule(_localctx, 288, RULE_arguments);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(2300); match(LPAREN);
			setState(2302);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ARGS) | (1L << AFTER) | (1L << AROUND) | (1L << ASPECT) | (1L << BEFORE) | (1L << CALL) | (1L << CFLOW) | (1L << CFLOWBELOW) | (1L << DECLARE) | (1L << ERROR) | (1L << EXECUTION) | (1L << GET) | (1L << HANDLER) | (1L << INITIALIZATION) | (1L << ISSINGLETON) | (1L << PARENTS) | (1L << PERCFLOW) | (1L << PERCFLOWBELOW) | (1L << PERTARGET) | (1L << PERTHIS) | (1L << PERTYPEWITHIN) | (1L << POINTCUT) | (1L << PRECEDENCE) | (1L << PREINITIALIZATION) | (1L << PRIVILEGED) | (1L << RETURNING) | (1L << SET) | (1L << SOFT) | (1L << STATICINITIALIZATION) | (1L << TARGET) | (1L << THROWING) | (1L << WARNING) | (1L << WITHIN) | (1L << WITHINCODE) | (1L << BOOLEAN) | (1L << BYTE))) != 0) || ((((_la - 66)) & ~0x3f) == 0 && ((1L << (_la - 66)) & ((1L << (CHAR - 66)) | (1L << (DOUBLE - 66)) | (1L << (FLOAT - 66)) | (1L << (INT - 66)) | (1L << (LONG - 66)) | (1L << (NEW - 66)) | (1L << (SHORT - 66)) | (1L << (SUPER - 66)) | (1L << (THIS - 66)) | (1L << (VOID - 66)) | (1L << (IntegerLiteral - 66)) | (1L << (FloatingPointLiteral - 66)) | (1L << (BooleanLiteral - 66)) | (1L << (CharacterLiteral - 66)) | (1L << (StringLiteral - 66)) | (1L << (NullLiteral - 66)) | (1L << (LPAREN - 66)) | (1L << (LT - 66)) | (1L << (BANG - 66)) | (1L << (TILDE - 66)))) != 0) || ((((_la - 137)) & ~0x3f) == 0 && ((1L << (_la - 137)) & ((1L << (INC - 137)) | (1L << (DEC - 137)) | (1L << (ADD - 137)) | (1L << (SUB - 137)) | (1L << (Identifier - 137)))) != 0)) {
				{
				setState(2301); expressionList();
				}
			}

			setState(2304); match(RPAREN);
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
		case 13: return pointcutExpression_sempred((PointcutExpressionContext)_localctx, predIndex);

		case 18: return typePattern_sempred((TypePatternContext)_localctx, predIndex);

		case 66: return expression_sempred((ExpressionContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean expression_sempred(ExpressionContext _localctx, int predIndex) {
		switch (predIndex) {
		case 4: return precpred(_ctx, 13);

		case 5: return precpred(_ctx, 12);

		case 6: return precpred(_ctx, 11);

		case 7: return precpred(_ctx, 10);

		case 8: return precpred(_ctx, 8);

		case 9: return precpred(_ctx, 7);

		case 10: return precpred(_ctx, 6);

		case 11: return precpred(_ctx, 5);

		case 12: return precpred(_ctx, 4);

		case 13: return precpred(_ctx, 3);

		case 14: return precpred(_ctx, 2);

		case 15: return precpred(_ctx, 1);

		case 16: return precpred(_ctx, 25);

		case 17: return precpred(_ctx, 24);

		case 18: return precpred(_ctx, 23);

		case 19: return precpred(_ctx, 22);

		case 20: return precpred(_ctx, 21);

		case 21: return precpred(_ctx, 20);

		case 22: return precpred(_ctx, 19);

		case 23: return precpred(_ctx, 16);

		case 24: return precpred(_ctx, 9);
		}
		return true;
	}
	private boolean pointcutExpression_sempred(PointcutExpressionContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0: return precpred(_ctx, 2);

		case 1: return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean typePattern_sempred(TypePatternContext _localctx, int predIndex) {
		switch (predIndex) {
		case 2: return precpred(_ctx, 2);

		case 3: return precpred(_ctx, 1);
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3\u00b3\u0905\4\2\t"+
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
		"\3\2\7\2\u0126\n\2\f\2\16\2\u0129\13\2\3\2\3\2\7\2\u012d\n\2\f\2\16\2"+
		"\u0130\13\2\3\2\3\2\7\2\u0134\n\2\f\2\16\2\u0137\13\2\3\2\3\2\7\2\u013b"+
		"\n\2\f\2\16\2\u013e\13\2\3\2\3\2\7\2\u0142\n\2\f\2\16\2\u0145\13\2\3\2"+
		"\3\2\5\2\u0149\n\2\3\3\3\3\7\3\u014d\n\3\f\3\16\3\u0150\13\3\3\3\3\3\3"+
		"\4\3\4\5\4\u0156\n\4\3\4\3\4\7\4\u015a\n\4\f\4\16\4\u015d\13\4\3\4\3\4"+
		"\3\4\5\4\u0162\n\4\3\5\3\5\3\5\3\5\5\5\u0168\n\5\3\6\3\6\3\6\3\6\3\6\3"+
		"\6\3\6\3\6\3\6\3\6\5\6\u0174\n\6\3\7\3\7\3\7\3\7\3\7\5\7\u017b\n\7\3\7"+
		"\5\7\u017e\n\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7"+
		"\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3"+
		"\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7"+
		"\3\7\3\7\3\7\3\7\3\7\3\7\3\7\5\7\u01b8\n\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7"+
		"\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3"+
		"\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7"+
		"\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3"+
		"\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\5\7\u0205"+
		"\n\7\3\7\3\7\5\7\u0209\n\7\3\b\3\b\3\b\7\b\u020e\n\b\f\b\16\b\u0211\13"+
		"\b\3\b\3\b\3\b\3\t\3\t\3\t\7\t\u0219\n\t\f\t\16\t\u021c\13\t\3\n\5\n\u021f"+
		"\n\n\3\n\7\n\u0222\n\n\f\n\16\n\u0225\13\n\3\n\3\n\3\n\3\n\5\n\u022b\n"+
		"\n\3\n\3\n\5\n\u022f\n\n\3\n\5\n\u0232\n\n\3\n\3\n\3\13\5\13\u0237\n\13"+
		"\3\13\3\13\3\13\5\13\u023c\n\13\3\13\3\13\3\13\3\13\3\f\3\f\3\f\3\f\3"+
		"\f\3\f\3\f\3\f\3\f\5\f\u024b\n\f\3\f\5\f\u024e\n\f\3\f\3\f\3\f\3\f\3\f"+
		"\5\f\u0255\n\f\3\f\5\f\u0258\n\f\3\f\3\f\5\f\u025c\n\f\3\f\3\f\5\f\u0260"+
		"\n\f\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3"+
		"\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\5\r\u027e\n\r\3\16\3\16"+
		"\7\16\u0282\n\16\f\16\16\16\u0285\13\16\3\16\3\16\3\16\3\16\3\16\3\16"+
		"\7\16\u028d\n\16\f\16\16\16\u0290\13\16\3\16\3\16\3\16\3\16\3\16\3\16"+
		"\3\16\5\16\u0299\n\16\3\17\3\17\3\17\3\17\3\17\5\17\u02a0\n\17\3\17\3"+
		"\17\3\17\3\17\5\17\u02a6\n\17\3\17\3\17\3\17\3\17\3\17\3\17\7\17\u02ae"+
		"\n\17\f\17\16\17\u02b1\13\17\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3"+
		"\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3"+
		"\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3"+
		"\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3"+
		"\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3"+
		"\20\3\20\5\20\u02f5\n\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20"+
		"\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20"+
		"\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20"+
		"\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20\3\20"+
		"\3\20\5\20\u032b\n\20\3\21\3\21\3\21\5\21\u0330\n\21\3\21\3\21\3\21\3"+
		"\22\7\22\u0336\n\22\f\22\16\22\u0339\13\22\3\22\3\22\5\22\u033d\n\22\3"+
		"\22\3\22\3\22\3\22\3\22\3\22\5\22\u0345\n\22\3\22\3\22\3\22\7\22\u034a"+
		"\n\22\f\22\16\22\u034d\13\22\3\22\3\22\7\22\u0351\n\22\f\22\16\22\u0354"+
		"\13\22\3\22\3\22\5\22\u0358\n\22\3\22\3\22\3\22\3\22\3\22\3\22\5\22\u0360"+
		"\n\22\3\22\3\22\3\22\7\22\u0365\n\22\f\22\16\22\u0368\13\22\3\22\3\22"+
		"\3\22\3\22\3\22\3\22\5\22\u0370\n\22\3\22\3\22\3\22\7\22\u0375\n\22\f"+
		"\22\16\22\u0378\13\22\3\22\3\22\5\22\u037c\n\22\3\22\3\22\3\22\3\22\3"+
		"\22\5\22\u0383\n\22\3\22\3\22\5\22\u0387\n\22\3\23\3\23\3\23\3\23\3\23"+
		"\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23"+
		"\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23"+
		"\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23"+
		"\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23"+
		"\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23"+
		"\3\23\3\23\3\23\3\23\3\23\3\23\3\23\5\23\u03db\n\23\3\24\3\24\3\24\3\24"+
		"\3\24\3\24\5\24\u03e3\n\24\3\24\3\24\3\24\5\24\u03e8\n\24\3\24\3\24\3"+
		"\24\3\24\3\24\3\24\7\24\u03f0\n\24\f\24\16\24\u03f3\13\24\3\25\3\25\5"+
		"\25\u03f7\n\25\3\25\3\25\7\25\u03fb\n\25\f\25\16\25\u03fe\13\25\3\26\3"+
		"\26\3\26\3\26\3\26\6\26\u0405\n\26\r\26\16\26\u0406\3\26\5\26\u040a\n"+
		"\26\3\27\3\27\5\27\u040e\n\27\3\27\3\27\3\27\3\27\5\27\u0414\n\27\3\27"+
		"\5\27\u0417\n\27\3\30\5\30\u041a\n\30\3\30\5\30\u041d\n\30\3\30\3\30\3"+
		"\30\3\30\5\30\u0423\n\30\3\30\3\30\3\31\5\31\u0428\n\31\3\31\3\31\7\31"+
		"\u042c\n\31\f\31\16\31\u042f\13\31\3\32\3\32\3\33\3\33\3\34\3\34\3\34"+
		"\7\34\u0438\n\34\f\34\16\34\u043b\13\34\3\34\5\34\u043e\n\34\3\34\3\34"+
		"\3\34\3\34\7\34\u0444\n\34\f\34\16\34\u0447\13\34\3\34\5\34\u044a\n\34"+
		"\5\34\u044c\n\34\3\35\3\35\5\35\u0450\n\35\3\36\5\36\u0453\n\36\3\36\5"+
		"\36\u0456\n\36\3\36\3\36\3\36\3\36\5\36\u045c\n\36\3\36\3\36\3\36\5\36"+
		"\u0461\n\36\3\37\5\37\u0464\n\37\3\37\3\37\7\37\u0468\n\37\f\37\16\37"+
		"\u046b\13\37\3 \3 \3!\3!\3!\7!\u0472\n!\f!\16!\u0475\13!\3!\3!\3!\7!\u047a"+
		"\n!\f!\16!\u047d\13!\3!\3!\3!\5!\u0482\n!\3\"\3\"\3\"\7\"\u0487\n\"\f"+
		"\"\16\"\u048a\13\"\3\"\3\"\3\"\5\"\u048f\n\"\3#\3#\3#\3$\3$\3$\7$\u0497"+
		"\n$\f$\16$\u049a\13$\3%\5%\u049d\n%\3%\5%\u04a0\n%\3%\3%\3%\5%\u04a5\n"+
		"%\3%\3%\3%\5%\u04aa\n%\3&\5&\u04ad\n&\3&\3&\7&\u04b1\n&\f&\16&\u04b4\13"+
		"&\3\'\3\'\3(\5(\u04b9\n(\3(\3(\3(\7(\u04be\n(\f(\16(\u04c1\13(\3)\3)\3"+
		")\3)\3)\5)\u04c8\n)\3*\3*\5*\u04cc\n*\3*\3*\3+\3+\5+\u04d2\n+\3,\3,\5"+
		",\u04d6\n,\3-\3-\3-\5-\u04db\n-\3-\3-\3-\7-\u04e0\n-\f-\16-\u04e3\13-"+
		"\3-\3-\3-\7-\u04e8\n-\f-\16-\u04eb\13-\5-\u04ed\n-\3.\3.\3.\7.\u04f2\n"+
		".\f.\16.\u04f5\13.\3.\3.\3.\7.\u04fa\n.\f.\16.\u04fd\13.\5.\u04ff\n.\3"+
		"/\3/\5/\u0503\n/\3\60\3\60\3\60\7\60\u0508\n\60\f\60\16\60\u050b\13\60"+
		"\3\61\3\61\5\61\u050f\n\61\3\62\3\62\3\62\5\62\u0514\n\62\3\62\3\62\5"+
		"\62\u0518\n\62\3\62\3\62\5\62\u051c\n\62\3\62\3\62\3\63\3\63\3\63\5\63"+
		"\u0523\n\63\3\64\3\64\3\64\3\64\5\64\u0529\n\64\3\64\3\64\5\64\u052d\n"+
		"\64\3\64\5\64\u0530\n\64\3\64\5\64\u0533\n\64\3\64\3\64\3\65\7\65\u0538"+
		"\n\65\f\65\16\65\u053b\13\65\3\65\3\65\5\65\u053f\n\65\3\65\5\65\u0542"+
		"\n\65\3\66\3\66\3\66\5\66\u0547\n\66\3\66\3\66\5\66\u054b\n\66\3\66\3"+
		"\66\3\67\3\67\5\67\u0551\n\67\3\67\3\67\3\67\3\67\7\67\u0557\n\67\f\67"+
		"\16\67\u055a\13\67\3\67\3\67\5\67\u055e\n\67\3\67\3\67\5\67\u0562\n\67"+
		"\38\38\38\38\58\u0568\n8\38\38\39\39\39\79\u056f\n9\f9\169\u0572\139\3"+
		"9\39\39\3:\3:\5:\u0579\n:\3:\3:\3:\3:\7:\u057f\n:\f:\16:\u0582\13:\3:"+
		"\3:\5:\u0586\n:\3:\3:\3;\3;\3;\7;\u058d\n;\f;\16;\u0590\13;\3<\3<\3=\3"+
		"=\5=\u0596\n=\3=\3=\3=\5=\u059b\n=\7=\u059d\n=\f=\16=\u05a0\13=\3>\3>"+
		"\3>\7>\u05a5\n>\f>\16>\u05a8\13>\3?\3?\3?\3?\3@\3@\3@\3@\3@\3A\3A\3A\3"+
		"A\5A\u05b7\nA\3B\3B\3B\3B\3B\5B\u05be\nB\3B\3B\3B\3B\3B\3B\3B\5B\u05c7"+
		"\nB\3B\3B\3B\3B\3B\3B\3B\3B\3B\3B\3B\3B\3B\3B\3B\3B\3B\3B\3B\6B\u05dc"+
		"\nB\rB\16B\u05dd\3B\5B\u05e1\nB\3B\5B\u05e4\nB\3B\3B\3B\3B\7B\u05ea\n"+
		"B\fB\16B\u05ed\13B\3B\5B\u05f0\nB\3B\3B\3B\3B\7B\u05f6\nB\fB\16B\u05f9"+
		"\13B\3B\7B\u05fc\nB\fB\16B\u05ff\13B\3B\3B\3B\3B\3B\3B\3B\3B\5B\u0609"+
		"\nB\3B\3B\3B\3B\3B\3B\3B\5B\u0612\nB\3B\3B\3B\5B\u0617\nB\3B\3B\3B\3B"+
		"\3B\3B\3B\3B\3B\5B\u0622\nB\3C\3C\3C\7C\u0627\nC\fC\16C\u062a\13C\3C\3"+
		"C\3C\3C\3C\3D\3D\3D\3D\3D\3D\3D\3D\3D\3D\3D\3D\3D\5D\u063e\nD\3D\3D\3"+
		"D\3D\3D\3D\3D\3D\3D\3D\3D\3D\3D\3D\5D\u064e\nD\3D\3D\3D\3D\3D\3D\3D\3"+
		"D\3D\3D\3D\3D\3D\3D\3D\3D\3D\3D\3D\3D\3D\3D\3D\3D\3D\3D\3D\3D\3D\3D\3"+
		"D\3D\3D\3D\3D\3D\3D\3D\3D\3D\3D\5D\u0679\nD\3D\3D\3D\3D\3D\3D\3D\3D\3"+
		"D\3D\3D\3D\3D\3D\3D\3D\5D\u068b\nD\3D\3D\3D\3D\3D\3D\7D\u0693\nD\fD\16"+
		"D\u0696\13D\3E\3E\3E\3E\3E\3E\3E\3E\3E\3E\3E\3E\3E\3E\3E\3E\3E\3E\3E\5"+
		"E\u06ab\nE\5E\u06ad\nE\3F\3F\5F\u06b1\nF\3F\3F\3F\5F\u06b6\nF\7F\u06b8"+
		"\nF\fF\16F\u06bb\13F\3F\5F\u06be\nF\3G\3G\5G\u06c2\nG\3G\3G\3H\3H\3H\3"+
		"H\5H\u06ca\nH\5H\u06cc\nH\3I\3I\3I\3I\3I\5I\u06d3\nI\3J\5J\u06d6\nJ\3"+
		"J\7J\u06d9\nJ\fJ\16J\u06dc\13J\3J\7J\u06df\nJ\fJ\16J\u06e2\13J\3J\3J\3"+
		"K\7K\u06e7\nK\fK\16K\u06ea\13K\3K\3K\3K\3K\3L\3L\5L\u06f2\nL\3L\3L\3L"+
		"\5L\u06f7\nL\3L\3L\3M\3M\5M\u06fd\nM\3N\3N\5N\u0701\nN\3O\3O\5O\u0705"+
		"\nO\3P\3P\3P\3P\7P\u070b\nP\fP\16P\u070e\13P\3P\3P\3Q\3Q\3Q\7Q\u0715\n"+
		"Q\fQ\16Q\u0718\13Q\3R\3R\3R\7R\u071d\nR\fR\16R\u0720\13R\3S\3S\7S\u0724"+
		"\nS\fS\16S\u0727\13S\3T\3T\3T\7T\u072c\nT\fT\16T\u072f\13T\3U\3U\7U\u0733"+
		"\nU\fU\16U\u0736\13U\3U\3U\3V\3V\7V\u073c\nV\fV\16V\u073f\13V\3V\3V\3"+
		"W\3W\3W\3X\3X\3X\3Y\3Y\3Y\3Y\3Z\7Z\u074e\nZ\fZ\16Z\u0751\13Z\3Z\3Z\5Z"+
		"\u0755\nZ\3[\3[\3[\3[\3[\3[\3[\5[\u075e\n[\3\\\3\\\3\\\3\\\7\\\u0764\n"+
		"\\\f\\\16\\\u0767\13\\\3\\\3\\\3]\3]\3]\3^\3^\3^\7^\u0771\n^\f^\16^\u0774"+
		"\13^\3_\3_\3_\5_\u0779\n_\3`\3`\5`\u077d\n`\3a\3a\3a\3a\7a\u0783\na\f"+
		"a\16a\u0786\13a\3a\5a\u0789\na\5a\u078b\na\3a\3a\3b\3b\3b\7b\u0792\nb"+
		"\fb\16b\u0795\13b\3b\3b\3b\7b\u079a\nb\fb\16b\u079d\13b\5b\u079f\nb\3"+
		"c\3c\3d\3d\3d\3d\7d\u07a7\nd\fd\16d\u07aa\13d\3d\3d\3e\3e\3e\3e\5e\u07b2"+
		"\ne\5e\u07b4\ne\3f\3f\3f\7f\u07b9\nf\ff\16f\u07bc\13f\3g\3g\5g\u07c0\n"+
		"g\3g\3g\3h\3h\3h\7h\u07c7\nh\fh\16h\u07ca\13h\3h\3h\5h\u07ce\nh\3h\5h"+
		"\u07d1\nh\3i\7i\u07d4\ni\fi\16i\u07d7\13i\3i\3i\3i\3j\7j\u07dd\nj\fj\16"+
		"j\u07e0\13j\3j\3j\3j\3j\3k\3k\3l\3l\3m\3m\3n\3n\3o\3o\3o\7o\u07f1\no\f"+
		"o\16o\u07f4\13o\3p\3p\3p\5p\u07f9\np\3q\3q\3q\3q\7q\u07ff\nq\fq\16q\u0802"+
		"\13q\5q\u0804\nq\3q\5q\u0807\nq\3q\3q\3r\3r\7r\u080d\nr\fr\16r\u0810\13"+
		"r\3r\3r\3s\7s\u0815\ns\fs\16s\u0818\13s\3s\3s\5s\u081c\ns\3t\3t\3t\3t"+
		"\3t\3t\5t\u0824\nt\3t\3t\5t\u0828\nt\3t\3t\5t\u082c\nt\3t\3t\5t\u0830"+
		"\nt\5t\u0832\nt\3u\3u\5u\u0836\nu\3v\3v\3w\3w\3w\3x\3x\7x\u083f\nx\fx"+
		"\16x\u0842\13x\3x\3x\3y\3y\3y\5y\u0849\ny\3z\3z\3z\3{\7{\u084f\n{\f{\16"+
		"{\u0852\13{\3{\3{\3{\3|\3|\3|\7|\u085a\n|\f|\16|\u085d\13|\3}\3}\3}\3"+
		"~\3~\3~\5~\u0865\n~\3~\3~\3\177\3\177\3\177\7\177\u086c\n\177\f\177\16"+
		"\177\u086f\13\177\3\u0080\7\u0080\u0872\n\u0080\f\u0080\16\u0080\u0875"+
		"\13\u0080\3\u0080\3\u0080\3\u0080\3\u0080\3\u0080\3\u0081\6\u0081\u087d"+
		"\n\u0081\r\u0081\16\u0081\u087e\3\u0081\6\u0081\u0882\n\u0081\r\u0081"+
		"\16\u0081\u0883\3\u0082\3\u0082\3\u0082\3\u0082\3\u0082\3\u0082\3\u0082"+
		"\3\u0082\3\u0082\3\u0082\5\u0082\u0890\n\u0082\3\u0083\3\u0083\5\u0083"+
		"\u0894\n\u0083\3\u0083\3\u0083\5\u0083\u0898\n\u0083\3\u0083\3\u0083\5"+
		"\u0083\u089c\n\u0083\5\u0083\u089e\n\u0083\3\u0084\3\u0084\5\u0084\u08a2"+
		"\n\u0084\3\u0085\7\u0085\u08a5\n\u0085\f\u0085\16\u0085\u08a8\13\u0085"+
		"\3\u0085\3\u0085\3\u0085\3\u0085\3\u0085\3\u0086\3\u0086\3\u0087\3\u0087"+
		"\3\u0087\3\u0087\3\u0088\3\u0088\3\u0088\7\u0088\u08b8\n\u0088\f\u0088"+
		"\16\u0088\u08bb\13\u0088\3\u0089\3\u0089\3\u008a\3\u008a\3\u008b\3\u008b"+
		"\3\u008b\3\u008b\3\u008b\3\u008b\3\u008b\5\u008b\u08c8\n\u008b\5\u008b"+
		"\u08ca\n\u008b\3\u008c\3\u008c\3\u008c\3\u008c\7\u008c\u08d0\n\u008c\f"+
		"\u008c\16\u008c\u08d3\13\u008c\3\u008c\3\u008c\3\u008c\3\u008c\3\u008c"+
		"\3\u008c\3\u008c\7\u008c\u08dc\n\u008c\f\u008c\16\u008c\u08df\13\u008c"+
		"\3\u008c\3\u008c\7\u008c\u08e3\n\u008c\f\u008c\16\u008c\u08e6\13\u008c"+
		"\5\u008c\u08e8\n\u008c\3\u008d\3\u008d\5\u008d\u08ec\n\u008d\3\u008e\3"+
		"\u008e\3\u008e\3\u008f\3\u008f\3\u008f\3\u008f\3\u0090\3\u0090\3\u0090"+
		"\5\u0090\u08f8\n\u0090\3\u0091\3\u0091\3\u0091\5\u0091\u08fd\n\u0091\3"+
		"\u0092\3\u0092\5\u0092\u0901\n\u0092\3\u0092\3\u0092\3\u0092\2\5\34&\u0086"+
		"\u0093\2\4\6\b\n\f\16\20\22\24\26\30\32\34\36 \"$&(*,.\60\62\64\668:<"+
		">@BDFHJLNPRTVXZ\\^`bdfhjlnprtvxz|~\u0080\u0082\u0084\u0086\u0088\u008a"+
		"\u008c\u008e\u0090\u0092\u0094\u0096\u0098\u009a\u009c\u009e\u00a0\u00a2"+
		"\u00a4\u00a6\u00a8\u00aa\u00ac\u00ae\u00b0\u00b2\u00b4\u00b6\u00b8\u00ba"+
		"\u00bc\u00be\u00c0\u00c2\u00c4\u00c6\u00c8\u00ca\u00cc\u00ce\u00d0\u00d2"+
		"\u00d4\u00d6\u00d8\u00da\u00dc\u00de\u00e0\u00e2\u00e4\u00e6\u00e8\u00ea"+
		"\u00ec\u00ee\u00f0\u00f2\u00f4\u00f6\u00f8\u00fa\u00fc\u00fe\u0100\u0102"+
		"\u0104\u0106\u0108\u010a\u010c\u010e\u0110\u0112\u0114\u0116\u0118\u011a"+
		"\u011c\u011e\u0120\u0122\2\25\6\2NN]_bbjj\4\2\3\3}}\6\2NN]_bbff\3\2]_"+
		"\4\2\3\3\u008f\u008f\3\2\7(\3\2\u008b\u008e\3\2\u0081\u0082\4\2\u008f"+
		"\u0090\u0094\u0094\3\2\u008d\u008e\4\2\177\u0080\u0086\u0087\4\2\u0085"+
		"\u0085\u0088\u0088\4\2~~\u0095\u009f\3\2\u008b\u008c\6\2ZZffjjmm\6\2="+
		"=NN]_bc\n\2??AADDJJPPWWYYaa\4\2MMdd\3\2ot\u09f1\2\u0148\3\2\2\2\4\u014a"+
		"\3\2\2\2\6\u0161\3\2\2\2\b\u0167\3\2\2\2\n\u0173\3\2\2\2\f\u0208\3\2\2"+
		"\2\16\u020a\3\2\2\2\20\u0215\3\2\2\2\22\u021e\3\2\2\2\24\u0236\3\2\2\2"+
		"\26\u025f\3\2\2\2\30\u027d\3\2\2\2\32\u0298\3\2\2\2\34\u02a5\3\2\2\2\36"+
		"\u032a\3\2\2\2 \u032f\3\2\2\2\"\u0386\3\2\2\2$\u03da\3\2\2\2&\u03e7\3"+
		"\2\2\2(\u03f4\3\2\2\2*\u0409\3\2\2\2,\u0416\3\2\2\2.\u0419\3\2\2\2\60"+
		"\u0427\3\2\2\2\62\u0430\3\2\2\2\64\u0432\3\2\2\2\66\u044b\3\2\2\28\u044f"+
		"\3\2\2\2:\u0452\3\2\2\2<\u0463\3\2\2\2>\u046c\3\2\2\2@\u0481\3\2\2\2B"+
		"\u048e\3\2\2\2D\u0490\3\2\2\2F\u0493\3\2\2\2H\u049c\3\2\2\2J\u04ac\3\2"+
		"\2\2L\u04b5\3\2\2\2N\u04b8\3\2\2\2P\u04c7\3\2\2\2R\u04c9\3\2\2\2T\u04d1"+
		"\3\2\2\2V\u04d5\3\2\2\2X\u04ec\3\2\2\2Z\u04fe\3\2\2\2\\\u0502\3\2\2\2"+
		"^\u0504\3\2\2\2`\u050e\3\2\2\2b\u0510\3\2\2\2d\u051f\3\2\2\2f\u0524\3"+
		"\2\2\2h\u0539\3\2\2\2j\u0543\3\2\2\2l\u0550\3\2\2\2n\u0563\3\2\2\2p\u056b"+
		"\3\2\2\2r\u0578\3\2\2\2t\u0589\3\2\2\2v\u0591\3\2\2\2x\u0593\3\2\2\2z"+
		"\u05a1\3\2\2\2|\u05a9\3\2\2\2~\u05ad\3\2\2\2\u0080\u05b2\3\2\2\2\u0082"+
		"\u0621\3\2\2\2\u0084\u0623\3\2\2\2\u0086\u063d\3\2\2\2\u0088\u06ac\3\2"+
		"\2\2\u008a\u06bd\3\2\2\2\u008c\u06bf\3\2\2\2\u008e\u06cb\3\2\2\2\u0090"+
		"\u06d2\3\2\2\2\u0092\u06d5\3\2\2\2\u0094\u06e8\3\2\2\2\u0096\u06ef\3\2"+
		"\2\2\u0098\u06fc\3\2\2\2\u009a\u0700\3\2\2\2\u009c\u0704\3\2\2\2\u009e"+
		"\u0706\3\2\2\2\u00a0\u0711\3\2\2\2\u00a2\u0719\3\2\2\2\u00a4\u0721\3\2"+
		"\2\2\u00a6\u0728\3\2\2\2\u00a8\u0730\3\2\2\2\u00aa\u0739\3\2\2\2\u00ac"+
		"\u0742\3\2\2\2\u00ae\u0745\3\2\2\2\u00b0\u0748\3\2\2\2\u00b2\u0754\3\2"+
		"\2\2\u00b4\u075d\3\2\2\2\u00b6\u075f\3\2\2\2\u00b8\u076a\3\2\2\2\u00ba"+
		"\u076d\3\2\2\2\u00bc\u0775\3\2\2\2\u00be\u077c\3\2\2\2\u00c0\u077e\3\2"+
		"\2\2\u00c2\u079e\3\2\2\2\u00c4\u07a0\3\2\2\2\u00c6\u07a2\3\2\2\2\u00c8"+
		"\u07b3\3\2\2\2\u00ca\u07b5\3\2\2\2\u00cc\u07bd\3\2\2\2\u00ce\u07d0\3\2"+
		"\2\2\u00d0\u07d5\3\2\2\2\u00d2\u07de\3\2\2\2\u00d4\u07e5\3\2\2\2\u00d6"+
		"\u07e7\3\2\2\2\u00d8\u07e9\3\2\2\2\u00da\u07eb\3\2\2\2\u00dc\u07ed\3\2"+
		"\2\2\u00de\u07f8\3\2\2\2\u00e0\u07fa\3\2\2\2\u00e2\u080a\3\2\2\2\u00e4"+
		"\u081b\3\2\2\2\u00e6\u0831\3\2\2\2\u00e8\u0835\3\2\2\2\u00ea\u0837\3\2"+
		"\2\2\u00ec\u0839\3\2\2\2\u00ee\u083c\3\2\2\2\u00f0\u0848\3\2\2\2\u00f2"+
		"\u084a\3\2\2\2\u00f4\u0850\3\2\2\2\u00f6\u0856\3\2\2\2\u00f8\u085e\3\2"+
		"\2\2\u00fa\u0861\3\2\2\2\u00fc\u0868\3\2\2\2\u00fe\u0873\3\2\2\2\u0100"+
		"\u087c\3\2\2\2\u0102\u088f\3\2\2\2\u0104\u089d\3\2\2\2\u0106\u08a1\3\2"+
		"\2\2\u0108\u08a6\3\2\2\2\u010a\u08ae\3\2\2\2\u010c\u08b0\3\2\2\2\u010e"+
		"\u08b4\3\2\2\2\u0110\u08bc\3\2\2\2\u0112\u08be\3\2\2\2\u0114\u08c9\3\2"+
		"\2\2\u0116\u08cb\3\2\2\2\u0118\u08e9\3\2\2\2\u011a\u08ed\3\2\2\2\u011c"+
		"\u08f0\3\2\2\2\u011e\u08f7\3\2\2\2\u0120\u08fc\3\2\2\2\u0122\u08fe\3\2"+
		"\2\2\u0124\u0126\5\u009aN\2\u0125\u0124\3\2\2\2\u0126\u0129\3\2\2\2\u0127"+
		"\u0125\3\2\2\2\u0127\u0128\3\2\2\2\u0128\u012a\3\2\2\2\u0129\u0127\3\2"+
		"\2\2\u012a\u0149\5b\62\2\u012b\u012d\5\u009aN\2\u012c\u012b\3\2\2\2\u012d"+
		"\u0130\3\2\2\2\u012e\u012c\3\2\2\2\u012e\u012f\3\2\2\2\u012f\u0131\3\2"+
		"\2\2\u0130\u012e\3\2\2\2\u0131\u0149\5f\64\2\u0132\u0134\5\u009aN\2\u0133"+
		"\u0132\3\2\2\2\u0134\u0137\3\2\2\2\u0135\u0133\3\2\2\2\u0135\u0136\3\2"+
		"\2\2\u0136\u0138\3\2\2\2\u0137\u0135\3\2\2\2\u0138\u0149\5j\66\2\u0139"+
		"\u013b\5\u009aN\2\u013a\u0139\3\2\2\2\u013b\u013e\3\2\2\2\u013c\u013a"+
		"\3\2\2\2\u013c\u013d\3\2\2\2\u013d\u013f\3\2\2\2\u013e\u013c\3\2\2\2\u013f"+
		"\u0149\5~@\2\u0140\u0142\5\u009aN\2\u0141\u0140\3\2\2\2\u0142\u0145\3"+
		"\2\2\2\u0143\u0141\3\2\2\2\u0143\u0144\3\2\2\2\u0144\u0146\3\2\2\2\u0145"+
		"\u0143\3\2\2\2\u0146\u0149\5\22\n\2\u0147\u0149\7{\2\2\u0148\u0127\3\2"+
		"\2\2\u0148\u012e\3\2\2\2\u0148\u0135\3\2\2\2\u0148\u013c\3\2\2\2\u0148"+
		"\u0143\3\2\2\2\u0148\u0147\3\2\2\2\u0149\3\3\2\2\2\u014a\u014e\7w\2\2"+
		"\u014b\u014d\5\b\5\2\u014c\u014b\3\2\2\2\u014d\u0150\3\2\2\2\u014e\u014c"+
		"\3\2\2\2\u014e\u014f\3\2\2\2\u014f\u0151\3\2\2\2\u0150\u014e\3\2\2\2\u0151"+
		"\u0152\7x\2\2\u0152\5\3\2\2\2\u0153\u0162\7{\2\2\u0154\u0156\7b\2\2\u0155"+
		"\u0154\3\2\2\2\u0155\u0156\3\2\2\2\u0156\u0157\3\2\2\2\u0157\u0162\5\u00ee"+
		"x\2\u0158\u015a\5\u0098M\2\u0159\u0158\3\2\2\2\u015a\u015d\3\2\2\2\u015b"+
		"\u0159\3\2\2\2\u015b\u015c\3\2\2\2\u015c\u015e\3\2\2\2\u015d\u015b\3\2"+
		"\2\2\u015e\u0162\5\n\6\2\u015f\u0160\7b\2\2\u0160\u0162\5\22\n\2\u0161"+
		"\u0153\3\2\2\2\u0161\u0155\3\2\2\2\u0161\u015b\3\2\2\2\u0161\u015f\3\2"+
		"\2\2\u0162\7\3\2\2\2\u0163\u0168\5\6\4\2\u0164\u0168\5\24\13\2\u0165\u0168"+
		"\5\"\22\2\u0166\u0168\5$\23\2\u0167\u0163\3\2\2\2\u0167\u0164\3\2\2\2"+
		"\u0167\u0165\3\2\2\2\u0167\u0166\3\2\2\2\u0168\t\3\2\2\2\u0169\u0174\5"+
		"l\67\2\u016a\u0174\5\u00acW\2\u016b\u0174\5\u00b0Y\2\u016c\u0174\5n8\2"+
		"\u016d\u0174\5\u00aeX\2\u016e\u0174\5j\66\2\u016f\u0174\5~@\2\u0170\u0174"+
		"\5b\62\2\u0171\u0174\5f\64\2\u0172\u0174\5\32\16\2\u0173\u0169\3\2\2\2"+
		"\u0173\u016a\3\2\2\2\u0173\u016b\3\2\2\2\u0173\u016c\3\2\2\2\u0173\u016d"+
		"\3\2\2\2\u0173\u016e\3\2\2\2\u0173\u016f\3\2\2\2\u0173\u0170\3\2\2\2\u0173"+
		"\u0171\3\2\2\2\u0173\u0172\3\2\2\2\u0174\13\3\2\2\2\u0175\u0176\7<\2\2"+
		"\u0176\u017d\5\u00dan\2\u0177\u017a\7u\2\2\u0178\u017b\5\u00dco\2\u0179"+
		"\u017b\5\u00dep\2\u017a\u0178\3\2\2\2\u017a\u0179\3\2\2\2\u017a\u017b"+
		"\3\2\2\2\u017b\u017c\3\2\2\2\u017c\u017e\7v\2\2\u017d\u0177\3\2\2\2\u017d"+
		"\u017e\3\2\2\2\u017e\u0209\3\2\2\2\u017f\u0180\7<\2\2\u0180\u0181\7)\2"+
		"\2\u0181\u0182\7u\2\2\u0182\u0183\7\4\2\2\u0183\u0184\5\34\17\2\u0184"+
		"\u0185\7\4\2\2\u0185\u0186\7v\2\2\u0186\u0209\3\2\2\2\u0187\u0188\7<\2"+
		"\2\u0188\u0189\7*\2\2\u0189\u018a\7u\2\2\u018a\u018b\7\4\2\2\u018b\u018c"+
		"\5\34\17\2\u018c\u018d\7\4\2\2\u018d\u018e\7v\2\2\u018e\u0209\3\2\2\2"+
		"\u018f\u0190\7<\2\2\u0190\u0191\7*\2\2\u0191\u0192\7u\2\2\u0192\u0193"+
		"\7\34\2\2\u0193\u0194\7~\2\2\u0194\u0195\7\4\2\2\u0195\u0196\5\34\17\2"+
		"\u0196\u0197\7\4\2\2\u0197\u0198\7|\2\2\u0198\u0199\7 \2\2\u0199\u019a"+
		"\7~\2\2\u019a\u019b\7\4\2\2\u019b\u019c\5`\61\2\u019c\u019d\7\4\2\2\u019d"+
		"\u019e\7v\2\2\u019e\u0209\3\2\2\2\u019f\u01a0\7<\2\2\u01a0\u01a1\7+\2"+
		"\2\u01a1\u01a2\7u\2\2\u01a2\u01a3\7\4\2\2\u01a3\u01a4\5\34\17\2\u01a4"+
		"\u01a5\7\4\2\2\u01a5\u01a6\7v\2\2\u01a6\u0209\3\2\2\2\u01a7\u01a8\7<\2"+
		"\2\u01a8\u01a9\7,\2\2\u01a9\u01aa\7u\2\2\u01aa\u01ab\7\4\2\2\u01ab\u01ac"+
		"\5\34\17\2\u01ac\u01ad\7\4\2\2\u01ad\u01ae\7v\2\2\u01ae\u0209\3\2\2\2"+
		"\u01af\u01b0\7<\2\2\u01b0\u01b7\7-\2\2\u01b1\u01b2\7u\2\2\u01b2\u01b3"+
		"\7\4\2\2\u01b3\u01b4\5\30\r\2\u01b4\u01b5\7\4\2\2\u01b5\u01b6\7v\2\2\u01b6"+
		"\u01b8\3\2\2\2\u01b7\u01b1\3\2\2\2\u01b7\u01b8\3\2\2\2\u01b8\u0209\3\2"+
		"\2\2\u01b9\u01ba\7<\2\2\u01ba\u01bb\7.\2\2\u01bb\u01bc\7u\2\2\u01bc\u01bd"+
		"\7\4\2\2\u01bd\u01be\5\34\17\2\u01be\u01bf\7\4\2\2\u01bf\u01c0\7v\2\2"+
		"\u01c0\u0209\3\2\2\2\u01c1\u01c2\7<\2\2\u01c2\u01c3\7\62\2\2\u01c3\u01c4"+
		"\7u\2\2\u01c4\u01c5\7\4\2\2\u01c5\u01c6\5\34\17\2\u01c6\u01c7\7\4\2\2"+
		"\u01c7\u01c8\7v\2\2\u01c8\u0209\3\2\2\2\u01c9\u01ca\7<\2\2\u01ca\u01cb"+
		"\7\60\2\2\u01cb\u01cc\7u\2\2\u01cc\u01cd\7;\2\2\u01cd\u01ce\7~\2\2\u01ce"+
		"\u01cf\7\4\2\2\u01cf\u01d0\5&\24\2\u01d0\u01d1\7\4\2\2\u01d1\u01d2\7|"+
		"\2\2\u01d2\u01d3\78\2\2\u01d3\u01d4\7~\2\2\u01d4\u01d5\7w\2\2\u01d5\u01d6"+
		"\5\20\t\2\u01d6\u01d7\7x\2\2\u01d7\u01d8\7v\2\2\u01d8\u0209\3\2\2\2\u01d9"+
		"\u01da\7<\2\2\u01da\u01db\7/\2\2\u01db\u01dc\7u\2\2\u01dc\u01dd\7\4\2"+
		"\2\u01dd\u01de\5&\24\2\u01de\u01df\7\4\2\2\u01df\u01e0\7v\2\2\u01e0\u0209"+
		"\3\2\2\2\u01e1\u01e2\7<\2\2\u01e2\u01e3\7/\2\2\u01e3\u01e4\7u\2\2\u01e4"+
		"\u01e5\7;\2\2\u01e5\u01e6\7~\2\2\u01e6\u01e7\7\4\2\2\u01e7\u01e8\5&\24"+
		"\2\u01e8\u01e9\7\4\2\2\u01e9\u01ea\7|\2\2\u01ea\u01eb\7\66\2\2\u01eb\u01ec"+
		"\7~\2\2\u01ec\u01ed\5\16\b\2\u01ed\u01ee\7v\2\2\u01ee\u0209\3\2\2\2\u01ef"+
		"\u01f0\7<\2\2\u01f0\u01f1\7\63\2\2\u01f1\u01f2\7u\2\2\u01f2\u01f3\7\4"+
		"\2\2\u01f3\u01f4\5F$\2\u01f4\u01f5\7\4\2\2\u01f5\u01f6\7v\2\2\u01f6\u0209"+
		"\3\2\2\2\u01f7\u01f8\7<\2\2\u01f8\u01f9\7\61\2\2\u01f9\u01fa\7u\2\2\u01fa"+
		"\u01fb\7\4\2\2\u01fb\u01fc\5\34\17\2\u01fc\u01fd\7\4\2\2\u01fd\u01fe\7"+
		"v\2\2\u01fe\u0209\3\2\2\2\u01ff\u0200\7<\2\2\u0200\u0201\7\64\2\2\u0201"+
		"\u0202\7u\2\2\u0202\u0204\7\4\2\2\u0203\u0205\5\34\17\2\u0204\u0203\3"+
		"\2\2\2\u0204\u0205\3\2\2\2\u0205\u0206\3\2\2\2\u0206\u0207\7\4\2\2\u0207"+
		"\u0209\7v\2\2\u0208\u0175\3\2\2\2\u0208\u017f\3\2\2\2\u0208\u0187\3\2"+
		"\2\2\u0208\u018f\3\2\2\2\u0208\u019f\3\2\2\2\u0208\u01a7\3\2\2\2\u0208"+
		"\u01af\3\2\2\2\u0208\u01b9\3\2\2\2\u0208\u01c1\3\2\2\2\u0208\u01c9\3\2"+
		"\2\2\u0208\u01d9\3\2\2\2\u0208\u01e1\3\2\2\2\u0208\u01ef\3\2\2\2\u0208"+
		"\u01f7\3\2\2\2\u0208\u01ff\3\2\2\2\u0209\r\3\2\2\2\u020a\u020f\5`\61\2"+
		"\u020b\u020c\7}\2\2\u020c\u020e\5`\61\2\u020d\u020b\3\2\2\2\u020e\u0211"+
		"\3\2\2\2\u020f\u020d\3\2\2\2\u020f\u0210\3\2\2\2\u0210\u0212\3\2\2\2\u0211"+
		"\u020f\3\2\2\2\u0212\u0213\7}\2\2\u0213\u0214\7E\2\2\u0214\17\3\2\2\2"+
		"\u0215\u021a\5\16\b\2\u0216\u0217\7|\2\2\u0217\u0219\5\16\b\2\u0218\u0216"+
		"\3\2\2\2\u0219\u021c\3\2\2\2\u021a\u0218\3\2\2\2\u021a\u021b\3\2\2\2\u021b"+
		"\21\3\2\2\2\u021c\u021a\3\2\2\2\u021d\u021f\7\37\2\2\u021e\u021d\3\2\2"+
		"\2\u021e\u021f\3\2\2\2\u021f\u0223\3\2\2\2\u0220\u0222\5\u0098M\2\u0221"+
		"\u0220\3\2\2\2\u0222\u0225\3\2\2\2\u0223\u0221\3\2\2\2\u0223\u0224\3\2"+
		"\2\2\u0224\u0226\3\2\2\2\u0225\u0223\3\2\2\2\u0226\u0227\7\n\2\2\u0227"+
		"\u022a\5`\61\2\u0228\u0229\7M\2\2\u0229\u022b\5\u00c2b\2\u022a\u0228\3"+
		"\2\2\2\u022a\u022b\3\2\2\2\u022b\u022e\3\2\2\2\u022c\u022d\7T\2\2\u022d"+
		"\u022f\5\u00a6T\2\u022e\u022c\3\2\2\2\u022e\u022f\3\2\2\2\u022f\u0231"+
		"\3\2\2\2\u0230\u0232\5\30\r\2\u0231\u0230\3\2\2\2\u0231\u0232\3\2\2\2"+
		"\u0232\u0233\3\2\2\2\u0233\u0234\5\4\3\2\u0234\23\3\2\2\2\u0235\u0237"+
		"\7c\2\2\u0236\u0235\3\2\2\2\u0236\u0237\3\2\2\2\u0237\u0238\3\2\2\2\u0238"+
		"\u023b\5\26\f\2\u0239\u023a\7i\2\2\u023a\u023c\5\u00a6T\2\u023b\u0239"+
		"\3\2\2\2\u023b\u023c\3\2\2\2\u023c\u023d\3\2\2\2\u023d\u023e\7\u0084\2"+
		"\2\u023e\u023f\5\34\17\2\u023f\u0240\5\u00d4k\2\u0240\25\3\2\2\2\u0241"+
		"\u0242\7\13\2\2\u0242\u0260\5\u00ccg\2\u0243\u0244\7\b\2\2\u0244\u0260"+
		"\5\u00ccg\2\u0245\u0246\7\b\2\2\u0246\u0247\5\u00ccg\2\u0247\u024d\7 "+
		"\2\2\u0248\u024a\7u\2\2\u0249\u024b\5\u00d0i\2\u024a\u0249\3\2\2\2\u024a"+
		"\u024b\3\2\2\2\u024b\u024c\3\2\2\2\u024c\u024e\7v\2\2\u024d\u0248\3\2"+
		"\2\2\u024d\u024e\3\2\2\2\u024e\u0260\3\2\2\2\u024f\u0250\7\b\2\2\u0250"+
		"\u0251\5\u00ccg\2\u0251\u0257\7%\2\2\u0252\u0254\7u\2\2\u0253\u0255\5"+
		"\u00d0i\2\u0254\u0253\3\2\2\2\u0254\u0255\3\2\2\2\u0255\u0256\3\2\2\2"+
		"\u0256\u0258\7v\2\2\u0257\u0252\3\2\2\2\u0257\u0258\3\2\2\2\u0258\u0260"+
		"\3\2\2\2\u0259\u025c\5\u00c2b\2\u025a\u025c\7l\2\2\u025b\u0259\3\2\2\2"+
		"\u025b\u025a\3\2\2\2\u025c\u025d\3\2\2\2\u025d\u025e\7\t\2\2\u025e\u0260"+
		"\5\u00ccg\2\u025f\u0241\3\2\2\2\u025f\u0243\3\2\2\2\u025f\u0245\3\2\2"+
		"\2\u025f\u024f\3\2\2\2\u025f\u025b\3\2\2\2\u0260\27\3\2\2\2\u0261\u0262"+
		"\7\31\2\2\u0262\u0263\7u\2\2\u0263\u0264\5\34\17\2\u0264\u0265\7v\2\2"+
		"\u0265\u027e\3\2\2\2\u0266\u0267\7\32\2\2\u0267\u0268\7u\2\2\u0268\u0269"+
		"\5\34\17\2\u0269\u026a\7v\2\2\u026a\u027e\3\2\2\2\u026b\u026c\7\27\2\2"+
		"\u026c\u026d\7u\2\2\u026d\u026e\5\34\17\2\u026e\u026f\7v\2\2\u026f\u027e"+
		"\3\2\2\2\u0270\u0271\7\30\2\2\u0271\u0272\7u\2\2\u0272\u0273\5\34\17\2"+
		"\u0273\u0274\7v\2\2\u0274\u027e\3\2\2\2\u0275\u0276\7\33\2\2\u0276\u0277"+
		"\7u\2\2\u0277\u0278\5&\24\2\u0278\u0279\7v\2\2\u0279\u027e\3\2\2\2\u027a"+
		"\u027b\7\25\2\2\u027b\u027c\7u\2\2\u027c\u027e\7v\2\2\u027d\u0261\3\2"+
		"\2\2\u027d\u0266\3\2\2\2\u027d\u026b\3\2\2\2\u027d\u0270\3\2\2\2\u027d"+
		"\u0275\3\2\2\2\u027d\u027a\3\2\2\2\u027e\31\3\2\2\2\u027f\u0283\7=\2\2"+
		"\u0280\u0282\5\u0098M\2\u0281\u0280\3\2\2\2\u0282\u0285\3\2\2\2\u0283"+
		"\u0281\3\2\2\2\u0283\u0284\3\2\2\2\u0284\u0286\3\2\2\2\u0285\u0283\3\2"+
		"\2\2\u0286\u0287\7\34\2\2\u0287\u0288\5`\61\2\u0288\u0289\5\u00ccg\2\u0289"+
		"\u028a\7{\2\2\u028a\u0299\3\2\2\2\u028b\u028d\5\u0098M\2\u028c\u028b\3"+
		"\2\2\2\u028d\u0290\3\2\2\2\u028e\u028c\3\2\2\2\u028e\u028f\3\2\2\2\u028f"+
		"\u0291\3\2\2\2\u0290\u028e\3\2\2\2\u0291\u0292\7\34\2\2\u0292\u0293\5"+
		"`\61\2\u0293\u0294\5\u00ccg\2\u0294\u0295\7\u0084\2\2\u0295\u0296\5\34"+
		"\17\2\u0296\u0297\7{\2\2\u0297\u0299\3\2\2\2\u0298\u027f\3\2\2\2\u0298"+
		"\u028e\3\2\2\2\u0299\33\3\2\2\2\u029a\u029b\b\17\1\2\u029b\u029c\7\u0081"+
		"\2\2\u029c\u02a6\5\34\17\6\u029d\u02a0\5\36\20\2\u029e\u02a0\5 \21\2\u029f"+
		"\u029d\3\2\2\2\u029f\u029e\3\2\2\2\u02a0\u02a6\3\2\2\2\u02a1\u02a2\7u"+
		"\2\2\u02a2\u02a3\5\34\17\2\u02a3\u02a4\7v\2\2\u02a4\u02a6\3\2\2\2\u02a5"+
		"\u029a\3\2\2\2\u02a5\u029f\3\2\2\2\u02a5\u02a1\3\2\2\2\u02a6\u02af\3\2"+
		"\2\2\u02a7\u02a8\f\4\2\2\u02a8\u02a9\7\u0089\2\2\u02a9\u02ae\5\34\17\5"+
		"\u02aa\u02ab\f\3\2\2\u02ab\u02ac\7\u008a\2\2\u02ac\u02ae\5\34\17\4\u02ad"+
		"\u02a7\3\2\2\2\u02ad\u02aa\3\2\2\2\u02ae\u02b1\3\2\2\2\u02af\u02ad\3\2"+
		"\2\2\u02af\u02b0\3\2\2\2\u02b0\35\3\2\2\2\u02b1\u02af\3\2\2\2\u02b2\u02b3"+
		"\7\f\2\2\u02b3\u02b4\7u\2\2\u02b4\u02b5\58\35\2\u02b5\u02b6\7v\2\2\u02b6"+
		"\u032b\3\2\2\2\u02b7\u02b8\7\21\2\2\u02b8\u02b9\7u\2\2\u02b9\u02ba\58"+
		"\35\2\u02ba\u02bb\7v\2\2\u02bb\u032b\3\2\2\2\u02bc\u02bd\7\24\2\2\u02bd"+
		"\u02be\7u\2\2\u02be\u02bf\5H%\2\u02bf\u02c0\7v\2\2\u02c0\u032b\3\2\2\2"+
		"\u02c1\u02c2\7\36\2\2\u02c2\u02c3\7u\2\2\u02c3\u02c4\5H%\2\u02c4\u02c5"+
		"\7v\2\2\u02c5\u032b\3\2\2\2\u02c6\u02c7\7#\2\2\u02c7\u02c8\7u\2\2\u02c8"+
		"\u02c9\5,\27\2\u02c9\u02ca\7v\2\2\u02ca\u032b\3\2\2\2\u02cb\u02cc\7\22"+
		"\2\2\u02cc\u02cd\7u\2\2\u02cd\u02ce\5.\30\2\u02ce\u02cf\7v\2\2\u02cf\u032b"+
		"\3\2\2\2\u02d0\u02d1\7!\2\2\u02d1\u02d2\7u\2\2\u02d2\u02d3\5.\30\2\u02d3"+
		"\u02d4\7v\2\2\u02d4\u032b\3\2\2\2\u02d5\u02d6\7\23\2\2\u02d6\u02d7\7u"+
		"\2\2\u02d7\u02d8\5,\27\2\u02d8\u02d9\7v\2\2\u02d9\u032b\3\2\2\2\u02da"+
		"\u02db\7\5\2\2\u02db\u02dc\7u\2\2\u02dc\u032b\7v\2\2\u02dd\u02de\7\'\2"+
		"\2\u02de\u02df\7u\2\2\u02df\u02e0\5,\27\2\u02e0\u02e1\7v\2\2\u02e1\u032b"+
		"\3\2\2\2\u02e2\u02e3\7(\2\2\u02e3\u02e4\7u\2\2\u02e4\u02e5\58\35\2\u02e5"+
		"\u02e6\7v\2\2\u02e6\u032b\3\2\2\2\u02e7\u02e8\7\r\2\2\u02e8\u02e9\7u\2"+
		"\2\u02e9\u02ea\5\34\17\2\u02ea\u02eb\7v\2\2\u02eb\u032b\3\2\2\2\u02ec"+
		"\u02ed\7\16\2\2\u02ed\u02ee\7u\2\2\u02ee\u02ef\5\34\17\2\u02ef\u02f0\7"+
		"v\2\2\u02f0\u032b\3\2\2\2\u02f1\u02f2\7R\2\2\u02f2\u02f4\7u\2\2\u02f3"+
		"\u02f5\5\u0086D\2\u02f4\u02f3\3\2\2\2\u02f4\u02f5\3\2\2\2\u02f5\u02f6"+
		"\3\2\2\2\u02f6\u032b\7v\2\2\u02f7\u02f8\7g\2\2\u02f8\u02f9\7u\2\2\u02f9"+
		"\u02fa\5T+\2\u02fa\u02fb\7v\2\2\u02fb\u032b\3\2\2\2\u02fc\u02fd\7$\2\2"+
		"\u02fd\u02fe\7u\2\2\u02fe\u02ff\5T+\2\u02ff\u0300\7v\2\2\u0300\u032b\3"+
		"\2\2\2\u0301\u0302\7\7\2\2\u0302\u0303\7u\2\2\u0303\u0304\5^\60\2\u0304"+
		"\u0305\7v\2\2\u0305\u032b\3\2\2\2\u0306\u0307\7<\2\2\u0307\u0308\7g\2"+
		"\2\u0308\u0309\7u\2\2\u0309\u030a\5V,\2\u030a\u030b\7v\2\2\u030b\u032b"+
		"\3\2\2\2\u030c\u030d\7<\2\2\u030d\u030e\7$\2\2\u030e\u030f\7u\2\2\u030f"+
		"\u0310\5V,\2\u0310\u0311\7v\2\2\u0311\u032b\3\2\2\2\u0312\u0313\7<\2\2"+
		"\u0313\u0314\7\7\2\2\u0314\u0315\7u\2\2\u0315\u0316\5X-\2\u0316\u0317"+
		"\7v\2\2\u0317\u032b\3\2\2\2\u0318\u0319\7<\2\2\u0319\u031a\7\'\2\2\u031a"+
		"\u031b\7u\2\2\u031b\u031c\5V,\2\u031c\u031d\7v\2\2\u031d\u032b\3\2\2\2"+
		"\u031e\u031f\7<\2\2\u031f\u0320\7(\2\2\u0320\u0321\7u\2\2\u0321\u0322"+
		"\5V,\2\u0322\u0323\7v\2\2\u0323\u032b\3\2\2\2\u0324\u0325\7<\2\2\u0325"+
		"\u0326\7\6\2\2\u0326\u0327\7u\2\2\u0327\u0328\5V,\2\u0328\u0329\7v\2\2"+
		"\u0329\u032b\3\2\2\2\u032a\u02b2\3\2\2\2\u032a\u02b7\3\2\2\2\u032a\u02bc"+
		"\3\2\2\2\u032a\u02c1\3\2\2\2\u032a\u02c6\3\2\2\2\u032a\u02cb\3\2\2\2\u032a"+
		"\u02d0\3\2\2\2\u032a\u02d5\3\2\2\2\u032a\u02da\3\2\2\2\u032a\u02dd\3\2"+
		"\2\2\u032a\u02e2\3\2\2\2\u032a\u02e7\3\2\2\2\u032a\u02ec\3\2\2\2\u032a"+
		"\u02f1\3\2\2\2\u032a\u02f7\3\2\2\2\u032a\u02fc\3\2\2\2\u032a\u0301\3\2"+
		"\2\2\u032a\u0306\3\2\2\2\u032a\u030c\3\2\2\2\u032a\u0312\3\2\2\2\u032a"+
		"\u0318\3\2\2\2\u032a\u031e\3\2\2\2\u032a\u0324\3\2\2\2\u032b\37\3\2\2"+
		"\2\u032c\u032d\5&\24\2\u032d\u032e\7}\2\2\u032e\u0330\3\2\2\2\u032f\u032c"+
		"\3\2\2\2\u032f\u0330\3\2\2\2\u0330\u0331\3\2\2\2\u0331\u0332\5`\61\2\u0332"+
		"\u0333\5R*\2\u0333!\3\2\2\2\u0334\u0336\5\u0098M\2\u0335\u0334\3\2\2\2"+
		"\u0336\u0339\3\2\2\2\u0337\u0335\3\2\2\2\u0337\u0338\3\2\2\2\u0338\u033c"+
		"\3\2\2\2\u0339\u0337\3\2\2\2\u033a\u033d\5\u00c2b\2\u033b\u033d\7l\2\2"+
		"\u033c\u033a\3\2\2\2\u033c\u033b\3\2\2\2\u033d\u033e\3\2\2\2\u033e\u033f"+
		"\5\u00c2b\2\u033f\u0340\7}\2\2\u0340\u0341\5`\61\2\u0341\u0344\5\u00cc"+
		"g\2\u0342\u0343\7i\2\2\u0343\u0345\5\u00a6T\2\u0344\u0342\3\2\2\2\u0344"+
		"\u0345\3\2\2\2\u0345\u0346\3\2\2\2\u0346\u0347\5\u00d4k\2\u0347\u0387"+
		"\3\2\2\2\u0348\u034a\5\u0098M\2\u0349\u0348\3\2\2\2\u034a\u034d\3\2\2"+
		"\2\u034b\u0349\3\2\2\2\u034b\u034c\3\2\2\2\u034c\u034e\3\2\2\2\u034d\u034b"+
		"\3\2\2\2\u034e\u0352\7=\2\2\u034f\u0351\5\u0098M\2\u0350\u034f\3\2\2\2"+
		"\u0351\u0354\3\2\2\2\u0352\u0350\3\2\2\2\u0352\u0353\3\2\2\2\u0353\u0357"+
		"\3\2\2\2\u0354\u0352\3\2\2\2\u0355\u0358\5\u00c2b\2\u0356\u0358\7l\2\2"+
		"\u0357\u0355\3\2\2\2\u0357\u0356\3\2\2\2\u0358\u0359\3\2\2\2\u0359\u035a"+
		"\5\u00c2b\2\u035a\u035b\7}\2\2\u035b\u035c\5`\61\2\u035c\u035f\5\u00cc"+
		"g\2\u035d\u035e\7i\2\2\u035e\u0360\5\u00a6T\2\u035f\u035d\3\2\2\2\u035f"+
		"\u0360\3\2\2\2\u0360\u0361\3\2\2\2\u0361\u0362\7{\2\2\u0362\u0387\3\2"+
		"\2\2\u0363\u0365\5\u0098M\2\u0364\u0363\3\2\2\2\u0365\u0368\3\2\2\2\u0366"+
		"\u0364\3\2\2\2\u0366\u0367\3\2\2\2\u0367\u0369\3\2\2\2\u0368\u0366\3\2"+
		"\2\2\u0369\u036a\5\u00c2b\2\u036a\u036b\7}\2\2\u036b\u036c\7[\2\2\u036c"+
		"\u036f\5\u00ccg\2\u036d\u036e\7i\2\2\u036e\u0370\5\u00a6T\2\u036f\u036d"+
		"\3\2\2\2\u036f\u0370\3\2\2\2\u0370\u0371\3\2\2\2\u0371\u0372\5\u00d4k"+
		"\2\u0372\u0387\3\2\2\2\u0373\u0375\5\u0098M\2\u0374\u0373\3\2\2\2\u0375"+
		"\u0378\3\2\2\2\u0376\u0374\3\2\2\2\u0376\u0377\3\2\2\2\u0377\u037b\3\2"+
		"\2\2\u0378\u0376\3\2\2\2\u0379\u037c\5\u00c2b\2\u037a\u037c\7l\2\2\u037b"+
		"\u0379\3\2\2\2\u037b\u037a\3\2\2\2\u037c\u037d\3\2\2\2\u037d\u037e\5\u00c2"+
		"b\2\u037e\u037f\7}\2\2\u037f\u0382\5`\61\2\u0380\u0381\7~\2\2\u0381\u0383"+
		"\5\u0086D\2\u0382\u0380\3\2\2\2\u0382\u0383\3\2\2\2\u0383\u0384\3\2\2"+
		"\2\u0384\u0385\7{\2\2\u0385\u0387\3\2\2\2\u0386\u0337\3\2\2\2\u0386\u034b"+
		"\3\2\2\2\u0386\u0366\3\2\2\2\u0386\u0376\3\2\2\2\u0387#\3\2\2\2\u0388"+
		"\u0389\7\17\2\2\u0389\u038a\7\26\2\2\u038a\u038b\7\u0084\2\2\u038b\u038c"+
		"\5&\24\2\u038c\u038d\7M\2\2\u038d\u038e\5\u00c2b\2\u038e\u038f\7{\2\2"+
		"\u038f\u03db\3\2\2\2\u0390\u0391\7\17\2\2\u0391\u0392\7\26\2\2\u0392\u0393"+
		"\7\u0084\2\2\u0393\u0394\5&\24\2\u0394\u0395\7T\2\2\u0395\u0396\5\u00a6"+
		"T\2\u0396\u0397\7{\2\2\u0397\u03db\3\2\2\2\u0398\u0399\7\17\2\2\u0399"+
		"\u039a\7&\2\2\u039a\u039b\7\u0084\2\2\u039b\u039c\5\34\17\2\u039c\u039d"+
		"\7\u0084\2\2\u039d\u039e\7s\2\2\u039e\u039f\7{\2\2\u039f\u03db\3\2\2\2"+
		"\u03a0\u03a1\7\17\2\2\u03a1\u03a2\7\20\2\2\u03a2\u03a3\7\u0084\2\2\u03a3"+
		"\u03a4\5\34\17\2\u03a4\u03a5\7\u0084\2\2\u03a5\u03a6\7s\2\2\u03a6\u03a7"+
		"\7{\2\2\u03a7\u03db\3\2\2\2\u03a8\u03a9\7\17\2\2\u03a9\u03aa\7\"\2\2\u03aa"+
		"\u03ab\7\u0084\2\2\u03ab\u03ac\5\u00c2b\2\u03ac\u03ad\7\u0084\2\2\u03ad"+
		"\u03ae\5\34\17\2\u03ae\u03af\7{\2\2\u03af\u03db\3\2\2\2\u03b0\u03b1\7"+
		"\17\2\2\u03b1\u03b2\7\35\2\2\u03b2\u03b3\7\u0084\2\2\u03b3\u03b4\5F$\2"+
		"\u03b4\u03b5\7{\2\2\u03b5\u03db\3\2\2\2\u03b6\u03b7\7\17\2\2\u03b7\u03b8"+
		"\7<\2\2\u03b8\u03b9\79\2\2\u03b9\u03ba\7\u0084\2\2\u03ba\u03bb\5&\24\2"+
		"\u03bb\u03bc\7\u0084\2\2\u03bc\u03bd\5\f\7\2\u03bd\u03be\7{\2\2\u03be"+
		"\u03db\3\2\2\2\u03bf\u03c0\7\17\2\2\u03c0\u03c1\7<\2\2\u03c1\u03c2\7:"+
		"\2\2\u03c2\u03c3\7\u0084\2\2\u03c3\u03c4\5:\36\2\u03c4\u03c5\7\u0084\2"+
		"\2\u03c5\u03c6\5\f\7\2\u03c6\u03c7\7{\2\2\u03c7\u03db\3\2\2\2\u03c8\u03c9"+
		"\7\17\2\2\u03c9\u03ca\7<\2\2\u03ca\u03cb\7\65\2\2\u03cb\u03cc\7\u0084"+
		"\2\2\u03cc\u03cd\5H%\2\u03cd\u03ce\7\u0084\2\2\u03ce\u03cf\5\f\7\2\u03cf"+
		"\u03d0\7{\2\2\u03d0\u03db\3\2\2\2\u03d1\u03d2\7\17\2\2\u03d2\u03d3\7<"+
		"\2\2\u03d3\u03d4\7\67\2\2\u03d4\u03d5\7\u0084\2\2\u03d5\u03d6\5.\30\2"+
		"\u03d6\u03d7\7\u0084\2\2\u03d7\u03d8\5\f\7\2\u03d8\u03d9\7{\2\2\u03d9"+
		"\u03db\3\2\2\2\u03da\u0388\3\2\2\2\u03da\u0390\3\2\2\2\u03da\u0398\3\2"+
		"\2\2\u03da\u03a0\3\2\2\2\u03da\u03a8\3\2\2\2\u03da\u03b0\3\2\2\2\u03da"+
		"\u03b6\3\2\2\2\u03da\u03bf\3\2\2\2\u03da\u03c8\3\2\2\2\u03da\u03d1\3\2"+
		"\2\2\u03db%\3\2\2\2\u03dc\u03dd\b\24\1\2\u03dd\u03de\7\u0081\2\2\u03de"+
		"\u03e8\5&\24\6\u03df\u03e8\5(\25\2\u03e0\u03e2\7u\2\2\u03e1\u03e3\5N("+
		"\2\u03e2\u03e1\3\2\2\2\u03e2\u03e3\3\2\2\2\u03e3\u03e4\3\2\2\2\u03e4\u03e5"+
		"\5&\24\2\u03e5\u03e6\7v\2\2\u03e6\u03e8\3\2\2\2\u03e7\u03dc\3\2\2\2\u03e7"+
		"\u03df\3\2\2\2\u03e7\u03e0\3\2\2\2\u03e8\u03f1\3\2\2\2\u03e9\u03ea\f\4"+
		"\2\2\u03ea\u03eb\7\u0089\2\2\u03eb\u03f0\5&\24\5\u03ec\u03ed\f\3\2\2\u03ed"+
		"\u03ee\7\u008a\2\2\u03ee\u03f0\5&\24\4\u03ef\u03e9\3\2\2\2\u03ef\u03ec"+
		"\3\2\2\2\u03f0\u03f3\3\2\2\2\u03f1\u03ef\3\2\2\2\u03f1\u03f2\3\2\2\2\u03f2"+
		"\'\3\2\2\2\u03f3\u03f1\3\2\2\2\u03f4\u03f6\5*\26\2\u03f5\u03f7\7\u008d"+
		"\2\2\u03f6\u03f5\3\2\2\2\u03f6\u03f7\3\2\2\2\u03f7\u03fc\3\2\2\2\u03f8"+
		"\u03f9\7y\2\2\u03f9\u03fb\7z\2\2\u03fa\u03f8\3\2\2\2\u03fb\u03fe\3\2\2"+
		"\2\u03fc\u03fa\3\2\2\2\u03fc\u03fd\3\2\2\2\u03fd)\3\2\2\2\u03fe\u03fc"+
		"\3\2\2\2\u03ff\u0405\5\u00c2b\2\u0400\u0405\5`\61\2\u0401\u0405\7\u008f"+
		"\2\2\u0402\u0405\7}\2\2\u0403\u0405\7\3\2\2\u0404\u03ff\3\2\2\2\u0404"+
		"\u0400\3\2\2\2\u0404\u0401\3\2\2\2\u0404\u0402\3\2\2\2\u0404\u0403\3\2"+
		"\2\2\u0405\u0406\3\2\2\2\u0406\u0404\3\2\2\2\u0406\u0407\3\2\2\2\u0407"+
		"\u040a\3\2\2\2\u0408\u040a\7l\2\2\u0409\u0404\3\2\2\2\u0409\u0408\3\2"+
		"\2\2\u040a+\3\2\2\2\u040b\u040d\7u\2\2\u040c\u040e\5N(\2\u040d\u040c\3"+
		"\2\2\2\u040d\u040e\3\2\2\2\u040e\u040f\3\2\2\2\u040f\u0410\5&\24\2\u0410"+
		"\u0411\7v\2\2\u0411\u0417\3\2\2\2\u0412\u0414\5N(\2\u0413\u0412\3\2\2"+
		"\2\u0413\u0414\3\2\2\2\u0414\u0415\3\2\2\2\u0415\u0417\5&\24\2\u0416\u040b"+
		"\3\2\2\2\u0416\u0413\3\2\2\2\u0417-\3\2\2\2\u0418\u041a\5N(\2\u0419\u0418"+
		"\3\2\2\2\u0419\u041a\3\2\2\2\u041a\u041c\3\2\2\2\u041b\u041d\5\60\31\2"+
		"\u041c\u041b\3\2\2\2\u041c\u041d\3\2\2\2\u041d\u041e\3\2\2\2\u041e\u0422"+
		"\5&\24\2\u041f\u0420\5&\24\2\u0420\u0421\5\64\33\2\u0421\u0423\3\2\2\2"+
		"\u0422\u041f\3\2\2\2\u0422\u0423\3\2\2\2\u0423\u0424\3\2\2\2\u0424\u0425"+
		"\5\66\34\2\u0425/\3\2\2\2\u0426\u0428\7\u0081\2\2\u0427\u0426\3\2\2\2"+
		"\u0427\u0428\3\2\2\2\u0428\u0429\3\2\2\2\u0429\u042d\5\62\32\2\u042a\u042c"+
		"\5\60\31\2\u042b\u042a\3\2\2\2\u042c\u042f\3\2\2\2\u042d\u042b\3\2\2\2"+
		"\u042d\u042e\3\2\2\2\u042e\61\3\2\2\2\u042f\u042d\3\2\2\2\u0430\u0431"+
		"\t\2\2\2\u0431\63\3\2\2\2\u0432\u0433\t\3\2\2\u0433\65\3\2\2\2\u0434\u0439"+
		"\5`\61\2\u0435\u0436\7\u008f\2\2\u0436\u0438\5`\61\2\u0437\u0435\3\2\2"+
		"\2\u0438\u043b\3\2\2\2\u0439\u0437\3\2\2\2\u0439\u043a\3\2\2\2\u043a\u043d"+
		"\3\2\2\2\u043b\u0439\3\2\2\2\u043c\u043e\7\u008f\2\2\u043d\u043c\3\2\2"+
		"\2\u043d\u043e\3\2\2\2\u043e\u044c\3\2\2\2\u043f\u0445\7\u008f\2\2\u0440"+
		"\u0441\5`\61\2\u0441\u0442\7\u008f\2\2\u0442\u0444\3\2\2\2\u0443\u0440"+
		"\3\2\2\2\u0444\u0447\3\2\2\2\u0445\u0443\3\2\2\2\u0445\u0446\3\2\2\2\u0446"+
		"\u0449\3\2\2\2\u0447\u0445\3\2\2\2\u0448\u044a\5`\61\2\u0449\u0448\3\2"+
		"\2\2\u0449\u044a\3\2\2\2\u044a\u044c\3\2\2\2\u044b\u0434\3\2\2\2\u044b"+
		"\u043f\3\2\2\2\u044c\67\3\2\2\2\u044d\u0450\5:\36\2\u044e\u0450\5H%\2"+
		"\u044f\u044d\3\2\2\2\u044f\u044e\3\2\2\2\u04509\3\2\2\2\u0451\u0453\5"+
		"N(\2\u0452\u0451\3\2\2\2\u0452\u0453\3\2\2\2\u0453\u0455\3\2\2\2\u0454"+
		"\u0456\5<\37\2\u0455\u0454\3\2\2\2\u0455\u0456\3\2\2\2\u0456\u0457\3\2"+
		"\2\2\u0457\u045b\5&\24\2\u0458\u0459\5&\24\2\u0459\u045a\5\64\33\2\u045a"+
		"\u045c\3\2\2\2\u045b\u0458\3\2\2\2\u045b\u045c\3\2\2\2\u045c\u045d\3\2"+
		"\2\2\u045d\u045e\5\66\34\2\u045e\u0460\5R*\2\u045f\u0461\5D#\2\u0460\u045f"+
		"\3\2\2\2\u0460\u0461\3\2\2\2\u0461;\3\2\2\2\u0462\u0464\7\u0081\2\2\u0463"+
		"\u0462\3\2\2\2\u0463\u0464\3\2\2\2\u0464\u0465\3\2\2\2\u0465\u0469\5>"+
		" \2\u0466\u0468\5<\37\2\u0467\u0466\3\2\2\2\u0468\u046b\3\2\2\2\u0469"+
		"\u0467\3\2\2\2\u0469\u046a\3\2\2\2\u046a=\3\2\2\2\u046b\u0469\3\2\2\2"+
		"\u046c\u046d\t\4\2\2\u046d?\3\2\2\2\u046e\u0473\7\3\2\2\u046f\u0470\7"+
		"|\2\2\u0470\u0472\5B\"\2\u0471\u046f\3\2\2\2\u0472\u0475\3\2\2\2\u0473"+
		"\u0471\3\2\2\2\u0473\u0474\3\2\2\2\u0474\u0482\3\2\2\2\u0475\u0473\3\2"+
		"\2\2\u0476\u047b\5,\27\2\u0477\u0478\7|\2\2\u0478\u047a\5@!\2\u0479\u0477"+
		"\3\2\2\2\u047a\u047d\3\2\2\2\u047b\u0479\3\2\2\2\u047b\u047c\3\2\2\2\u047c"+
		"\u0482\3\2\2\2\u047d\u047b\3\2\2\2\u047e\u047f\5&\24\2\u047f\u0480\7\u00a1"+
		"\2\2\u0480\u0482\3\2\2\2\u0481\u046e\3\2\2\2\u0481\u0476\3\2\2\2\u0481"+
		"\u047e\3\2\2\2\u0482A\3\2\2\2\u0483\u0488\5,\27\2\u0484\u0485\7|\2\2\u0485"+
		"\u0487\5B\"\2\u0486\u0484\3\2\2\2\u0487\u048a\3\2\2\2\u0488\u0486\3\2"+
		"\2\2\u0488\u0489\3\2\2\2\u0489\u048f\3\2\2\2\u048a\u0488\3\2\2\2\u048b"+
		"\u048c\5&\24\2\u048c\u048d\7\u00a1\2\2\u048d\u048f\3\2\2\2\u048e\u0483"+
		"\3\2\2\2\u048e\u048b\3\2\2\2\u048fC\3\2\2\2\u0490\u0491\7i\2\2\u0491\u0492"+
		"\5F$\2\u0492E\3\2\2\2\u0493\u0498\5&\24\2\u0494\u0495\7|\2\2\u0495\u0497"+
		"\5&\24\2\u0496\u0494\3\2\2\2\u0497\u049a\3\2\2\2\u0498\u0496\3\2\2\2\u0498"+
		"\u0499\3\2\2\2\u0499G\3\2\2\2\u049a\u0498\3\2\2\2\u049b\u049d\5N(\2\u049c"+
		"\u049b\3\2\2\2\u049c\u049d\3\2\2\2\u049d\u049f\3\2\2\2\u049e\u04a0\5J"+
		"&\2\u049f\u049e\3\2\2\2\u049f\u04a0\3\2\2\2\u04a0\u04a4\3\2\2\2\u04a1"+
		"\u04a2\5&\24\2\u04a2\u04a3\5\64\33\2\u04a3\u04a5\3\2\2\2\u04a4\u04a1\3"+
		"\2\2\2\u04a4\u04a5\3\2\2\2\u04a5\u04a6\3\2\2\2\u04a6\u04a7\7[\2\2\u04a7"+
		"\u04a9\5R*\2\u04a8\u04aa\5D#\2\u04a9\u04a8\3\2\2\2\u04a9\u04aa\3\2\2\2"+
		"\u04aaI\3\2\2\2\u04ab\u04ad\7\u0081\2\2\u04ac\u04ab\3\2\2\2\u04ac\u04ad"+
		"\3\2\2\2\u04ad\u04ae\3\2\2\2\u04ae\u04b2\5L\'\2\u04af\u04b1\5J&\2\u04b0"+
		"\u04af\3\2\2\2\u04b1\u04b4\3\2\2\2\u04b2\u04b0\3\2\2\2\u04b2\u04b3\3\2"+
		"\2\2\u04b3K\3\2\2\2\u04b4\u04b2\3\2\2\2\u04b5\u04b6\t\5\2\2\u04b6M\3\2"+
		"\2\2\u04b7\u04b9\7\u0081\2\2\u04b8\u04b7\3\2\2\2\u04b8\u04b9\3\2\2\2\u04b9"+
		"\u04ba\3\2\2\2\u04ba\u04bb\7<\2\2\u04bb\u04bf\5P)\2\u04bc\u04be\5N(\2"+
		"\u04bd\u04bc\3\2\2\2\u04be\u04c1\3\2\2\2\u04bf\u04bd\3\2\2\2\u04bf\u04c0"+
		"\3\2\2\2\u04c0O\3\2\2\2\u04c1\u04bf\3\2\2\2\u04c2\u04c8\5z>\2\u04c3\u04c4"+
		"\7u\2\2\u04c4\u04c5\5&\24\2\u04c5\u04c6\7v\2\2\u04c6\u04c8\3\2\2\2\u04c7"+
		"\u04c2\3\2\2\2\u04c7\u04c3\3\2\2\2\u04c8Q\3\2\2\2\u04c9\u04cb\7u\2\2\u04ca"+
		"\u04cc\5@!\2\u04cb\u04ca\3\2\2\2\u04cb\u04cc\3\2\2\2\u04cc\u04cd\3\2\2"+
		"\2\u04cd\u04ce\7v\2\2\u04ceS\3\2\2\2\u04cf\u04d2\5\u00c2b\2\u04d0\u04d2"+
		"\5t;\2\u04d1\u04cf\3\2\2\2\u04d1\u04d0\3\2\2\2\u04d2U\3\2\2\2\u04d3\u04d6"+
		"\5z>\2\u04d4\u04d6\5`\61\2\u04d5\u04d3\3\2\2\2\u04d5\u04d4\3\2\2\2\u04d6"+
		"W\3\2\2\2\u04d7\u04da\7\3\2\2\u04d8\u04d9\7|\2\2\u04d9\u04db\5Z.\2\u04da"+
		"\u04d8\3\2\2\2\u04da\u04db\3\2\2\2\u04db\u04ed\3\2\2\2\u04dc\u04e1\5V"+
		",\2\u04dd\u04de\7|\2\2\u04de\u04e0\5X-\2\u04df\u04dd\3\2\2\2\u04e0\u04e3"+
		"\3\2\2\2\u04e1\u04df\3\2\2\2\u04e1\u04e2\3\2\2\2\u04e2\u04ed\3\2\2\2\u04e3"+
		"\u04e1\3\2\2\2\u04e4\u04e9\7\u008f\2\2\u04e5\u04e6\7|\2\2\u04e6\u04e8"+
		"\5X-\2\u04e7\u04e5\3\2\2\2\u04e8\u04eb\3\2\2\2\u04e9\u04e7\3\2\2\2\u04e9"+
		"\u04ea\3\2\2\2\u04ea\u04ed\3\2\2\2\u04eb\u04e9\3\2\2\2\u04ec\u04d7\3\2"+
		"\2\2\u04ec\u04dc\3\2\2\2\u04ec\u04e4\3\2\2\2\u04edY\3\2\2\2\u04ee\u04f3"+
		"\5V,\2\u04ef\u04f0\7|\2\2\u04f0\u04f2\5Z.\2\u04f1\u04ef\3\2\2\2\u04f2"+
		"\u04f5\3\2\2\2\u04f3\u04f1\3\2\2\2\u04f3\u04f4\3\2\2\2\u04f4\u04ff\3\2"+
		"\2\2\u04f5\u04f3\3\2\2\2\u04f6\u04fb\7\u008f\2\2\u04f7\u04f8\7|\2\2\u04f8"+
		"\u04fa\5Z.\2\u04f9\u04f7\3\2\2\2\u04fa\u04fd\3\2\2\2\u04fb\u04f9\3\2\2"+
		"\2\u04fb\u04fc\3\2\2\2\u04fc\u04ff\3\2\2\2\u04fd\u04fb\3\2\2\2\u04fe\u04ee"+
		"\3\2\2\2\u04fe\u04f6\3\2\2\2\u04ff[\3\2\2\2\u0500\u0503\5T+\2\u0501\u0503"+
		"\t\6\2\2\u0502\u0500\3\2\2\2\u0502\u0501\3\2\2\2\u0503]\3\2\2\2\u0504"+
		"\u0509\5\\/\2\u0505\u0506\7|\2\2\u0506\u0508\5\\/\2\u0507\u0505\3\2\2"+
		"\2\u0508\u050b\3\2\2\2\u0509\u0507\3\2\2\2\u0509\u050a\3\2\2\2\u050a_"+
		"\3\2\2\2\u050b\u0509\3\2\2\2\u050c\u050f\t\7\2\2\u050d\u050f\7\u00a0\2"+
		"\2\u050e\u050c\3\2\2\2\u050e\u050d\3\2\2\2\u050fa\3\2\2\2\u0510\u0511"+
		"\7E\2\2\u0511\u0513\5`\61\2\u0512\u0514\5\u009eP\2\u0513\u0512\3\2\2\2"+
		"\u0513\u0514\3\2\2\2\u0514\u0517\3\2\2\2\u0515\u0516\7M\2\2\u0516\u0518"+
		"\5\u00c2b\2\u0517\u0515\3\2\2\2\u0517\u0518\3\2\2\2\u0518\u051b\3\2\2"+
		"\2\u0519\u051a\7T\2\2\u051a\u051c\5\u00a6T\2\u051b\u0519\3\2\2\2\u051b"+
		"\u051c\3\2\2\2\u051c\u051d\3\2\2\2\u051d\u051e\5\u00a8U\2\u051ec\3\2\2"+
		"\2\u051f\u0522\5`\61\2\u0520\u0521\7M\2\2\u0521\u0523\5\u00a0Q\2\u0522"+
		"\u0520\3\2\2\2\u0522\u0523\3\2\2\2\u0523e\3\2\2\2\u0524\u0525\7L\2\2\u0525"+
		"\u0528\5`\61\2\u0526\u0527\7T\2\2\u0527\u0529\5\u00a6T\2\u0528\u0526\3"+
		"\2\2\2\u0528\u0529\3\2\2\2\u0529\u052a\3\2\2\2\u052a\u052c\7w\2\2\u052b"+
		"\u052d\5\u00a2R\2\u052c\u052b\3\2\2\2\u052c\u052d\3\2\2\2\u052d\u052f"+
		"\3\2\2\2\u052e\u0530\7|\2\2\u052f\u052e\3\2\2\2\u052f\u0530\3\2\2\2\u0530"+
		"\u0532\3\2\2\2\u0531\u0533\5\u00a4S\2\u0532\u0531\3\2\2\2\u0532\u0533"+
		"\3\2\2\2\u0533\u0534\3\2\2\2\u0534\u0535\7x\2\2\u0535g\3\2\2\2\u0536\u0538"+
		"\5\f\7\2\u0537\u0536\3\2\2\2\u0538\u053b\3\2\2\2\u0539\u0537\3\2\2\2\u0539"+
		"\u053a\3\2\2\2\u053a\u053c\3\2\2\2\u053b\u0539\3\2\2\2\u053c\u053e\5`"+
		"\61\2\u053d\u053f\5\u0122\u0092\2\u053e\u053d\3\2\2\2\u053e\u053f\3\2"+
		"\2\2\u053f\u0541\3\2\2\2\u0540\u0542\5\u00a8U\2\u0541\u0540\3\2\2\2\u0541"+
		"\u0542\3\2\2\2\u0542i\3\2\2\2\u0543\u0544\7X\2\2\u0544\u0546\5`\61\2\u0545"+
		"\u0547\5\u009eP\2\u0546\u0545\3\2\2\2\u0546\u0547\3\2\2\2\u0547\u054a"+
		"\3\2\2\2\u0548\u0549\7M\2\2\u0549\u054b\5\u00a6T\2\u054a\u0548\3\2\2\2"+
		"\u054a\u054b\3\2\2\2\u054b\u054c\3\2\2\2\u054c\u054d\5\u00aaV\2\u054d"+
		"k\3\2\2\2\u054e\u0551\5\u00c2b\2\u054f\u0551\7l\2\2\u0550\u054e\3\2\2"+
		"\2\u0550\u054f\3\2\2\2\u0551\u0552\3\2\2\2\u0552\u0553\5`\61\2\u0553\u0558"+
		"\5\u00ccg\2\u0554\u0555\7y\2\2\u0555\u0557\7z\2\2\u0556\u0554\3\2\2\2"+
		"\u0557\u055a\3\2\2\2\u0558\u0556\3\2\2\2\u0558\u0559\3\2\2\2\u0559\u055d"+
		"\3\2\2\2\u055a\u0558\3\2\2\2\u055b\u055c\7i\2\2\u055c\u055e\5\u00caf\2"+
		"\u055d\u055b\3\2\2\2\u055d\u055e\3\2\2\2\u055e\u0561\3\2\2\2\u055f\u0562"+
		"\5\u00d4k\2\u0560\u0562\7{\2\2\u0561\u055f\3\2\2\2\u0561\u0560\3\2\2\2"+
		"\u0562m\3\2\2\2\u0563\u0564\5`\61\2\u0564\u0567\5\u00ccg\2\u0565\u0566"+
		"\7i\2\2\u0566\u0568\5\u00caf\2\u0567\u0565\3\2\2\2\u0567\u0568\3\2\2\2"+
		"\u0568\u0569\3\2\2\2\u0569\u056a\5\u00d6l\2\u056ao\3\2\2\2\u056b\u0570"+
		"\5`\61\2\u056c\u056d\7y\2\2\u056d\u056f\7z\2\2\u056e\u056c\3\2\2\2\u056f"+
		"\u0572\3\2\2\2\u0570\u056e\3\2\2\2\u0570\u0571\3\2\2\2\u0571\u0573\3\2"+
		"\2\2\u0572\u0570\3\2\2\2\u0573\u0574\7~\2\2\u0574\u0575\5\u00be`\2\u0575"+
		"q\3\2\2\2\u0576\u0579\5\u00c2b\2\u0577\u0579\7l\2\2\u0578\u0576\3\2\2"+
		"\2\u0578\u0577\3\2\2\2\u0579\u057a\3\2\2\2\u057a\u057b\5`\61\2\u057b\u0580"+
		"\5\u00ccg\2\u057c\u057d\7y\2\2\u057d\u057f\7z\2\2\u057e\u057c\3\2\2\2"+
		"\u057f\u0582\3\2\2\2\u0580\u057e\3\2\2\2\u0580\u0581\3\2\2\2\u0581\u0585"+
		"\3\2\2\2\u0582\u0580\3\2\2\2\u0583\u0584\7i\2\2\u0584\u0586\5\u00caf\2"+
		"\u0585\u0583\3\2\2\2\u0585\u0586\3\2\2\2\u0586\u0587\3\2\2\2\u0587\u0588"+
		"\7{\2\2\u0588s\3\2\2\2\u0589\u058e\5`\61\2\u058a\u058b\7y\2\2\u058b\u058d"+
		"\7z\2\2\u058c\u058a\3\2\2\2\u058d\u0590\3\2\2\2\u058e\u058c\3\2\2\2\u058e"+
		"\u058f\3\2\2\2\u058fu\3\2\2\2\u0590\u058e\3\2\2\2\u0591\u0592\5`\61\2"+
		"\u0592w\3\2\2\2\u0593\u0595\5`\61\2\u0594\u0596\5\u00c6d\2\u0595\u0594"+
		"\3\2\2\2\u0595\u0596\3\2\2\2\u0596\u059e\3\2\2\2\u0597\u0598\7}\2\2\u0598"+
		"\u059a\5`\61\2\u0599\u059b\5\u00c6d\2\u059a\u0599\3\2\2\2\u059a\u059b"+
		"\3\2\2\2\u059b\u059d\3\2\2\2\u059c\u0597\3\2\2\2\u059d\u05a0\3\2\2\2\u059e"+
		"\u059c\3\2\2\2\u059e\u059f\3\2\2\2\u059fy\3\2\2\2\u05a0\u059e\3\2\2\2"+
		"\u05a1\u05a6\5`\61\2\u05a2\u05a3\7}\2\2\u05a3\u05a5\5`\61\2\u05a4\u05a2"+
		"\3\2\2\2\u05a5\u05a8\3\2\2\2\u05a6\u05a4\3\2\2\2\u05a6\u05a7\3\2\2\2\u05a7"+
		"{\3\2\2\2\u05a8\u05a6\3\2\2\2\u05a9\u05aa\5`\61\2\u05aa\u05ab\7~\2\2\u05ab"+
		"\u05ac\5\u00dep\2\u05ac}\3\2\2\2\u05ad\u05ae\7<\2\2\u05ae\u05af\7X\2\2"+
		"\u05af\u05b0\5`\61\2\u05b0\u05b1\5\u00e2r\2\u05b1\177\3\2\2\2\u05b2\u05b3"+
		"\5`\61\2\u05b3\u05b4\7u\2\2\u05b4\u05b6\7v\2\2\u05b5\u05b7\5\u00ecw\2"+
		"\u05b6\u05b5\3\2\2\2\u05b6\u05b7\3\2\2\2\u05b7\u0081\3\2\2\2\u05b8\u0622"+
		"\5\u00eex\2\u05b9\u05ba\7>\2\2\u05ba\u05bd\5\u0086D\2\u05bb\u05bc\7\u0084"+
		"\2\2\u05bc\u05be\5\u0086D\2\u05bd\u05bb\3\2\2\2\u05bd\u05be\3\2\2\2\u05be"+
		"\u05bf\3\2\2\2\u05bf\u05c0\7{\2\2\u05c0\u0622\3\2\2\2\u05c1\u05c2\7R\2"+
		"\2\u05c2\u05c3\5\u010c\u0087\2\u05c3\u05c6\5\u0082B\2\u05c4\u05c5\7K\2"+
		"\2\u05c5\u05c7\5\u0082B\2\u05c6\u05c4\3\2\2\2\u05c6\u05c7\3\2\2\2\u05c7"+
		"\u0622\3\2\2\2\u05c8\u05c9\7Q\2\2\u05c9\u05ca\7u\2\2\u05ca\u05cb\5\u0104"+
		"\u0083\2\u05cb\u05cc\7v\2\2\u05cc\u05cd\5\u0082B\2\u05cd\u0622\3\2\2\2"+
		"\u05ce\u05cf\7n\2\2\u05cf\u05d0\5\u010c\u0087\2\u05d0\u05d1\5\u0082B\2"+
		"\u05d1\u0622\3\2\2\2\u05d2\u05d3\7I\2\2\u05d3\u05d4\5\u0082B\2\u05d4\u05d5"+
		"\7n\2\2\u05d5\u05d6\5\u010c\u0087\2\u05d6\u05d7\7{\2\2\u05d7\u0622\3\2"+
		"\2\2\u05d8\u05d9\7k\2\2\u05d9\u05e3\5\u00eex\2\u05da\u05dc\5\u0084C\2"+
		"\u05db\u05da\3\2\2\2\u05dc\u05dd\3\2\2\2\u05dd\u05db\3\2\2\2\u05dd\u05de"+
		"\3\2\2\2\u05de\u05e0\3\2\2\2\u05df\u05e1\5\u00f8}\2\u05e0\u05df\3\2\2"+
		"\2\u05e0\u05e1\3\2\2\2\u05e1\u05e4\3\2\2\2\u05e2\u05e4\5\u00f8}\2\u05e3"+
		"\u05db\3\2\2\2\u05e3\u05e2\3\2\2\2\u05e4\u0622\3\2\2\2\u05e5\u05e6\7k"+
		"\2\2\u05e6\u05e7\5\u00fa~\2\u05e7\u05eb\5\u00eex\2\u05e8\u05ea\5\u0084"+
		"C\2\u05e9\u05e8\3\2\2\2\u05ea\u05ed\3\2\2\2\u05eb\u05e9\3\2\2\2\u05eb"+
		"\u05ec\3\2\2\2\u05ec\u05ef\3\2\2\2\u05ed\u05eb\3\2\2\2\u05ee\u05f0\5\u00f8"+
		"}\2\u05ef\u05ee\3\2\2\2\u05ef\u05f0\3\2\2\2\u05f0\u0622\3\2\2\2\u05f1"+
		"\u05f2\7e\2\2\u05f2\u05f3\5\u010c\u0087\2\u05f3\u05f7\7w\2\2\u05f4\u05f6"+
		"\5\u0100\u0081\2\u05f5\u05f4\3\2\2\2\u05f6\u05f9\3\2\2\2\u05f7\u05f5\3"+
		"\2\2\2\u05f7\u05f8\3\2\2\2\u05f8\u05fd\3\2\2\2\u05f9\u05f7\3\2\2\2\u05fa"+
		"\u05fc\5\u0102\u0082\2\u05fb\u05fa\3\2\2\2\u05fc\u05ff\3\2\2\2\u05fd\u05fb"+
		"\3\2\2\2\u05fd\u05fe\3\2\2\2\u05fe\u0600\3\2\2\2\u05ff\u05fd\3\2\2\2\u0600"+
		"\u0601\7x\2\2\u0601\u0622\3\2\2\2\u0602\u0603\7f\2\2\u0603\u0604\5\u010c"+
		"\u0087\2\u0604\u0605\5\u00eex\2\u0605\u0622\3\2\2\2\u0606\u0608\7`\2\2"+
		"\u0607\u0609\5\u0086D\2\u0608\u0607\3\2\2\2\u0608\u0609\3\2\2\2\u0609"+
		"\u060a\3\2\2\2\u060a\u0622\7{\2\2\u060b\u060c\7h\2\2\u060c\u060d\5\u0086"+
		"D\2\u060d\u060e\7{\2\2\u060e\u0622\3\2\2\2\u060f\u0611\7@\2\2\u0610\u0612"+
		"\5`\61\2\u0611\u0610\3\2\2\2\u0611\u0612\3\2\2\2\u0612\u0613\3\2\2\2\u0613"+
		"\u0622\7{\2\2\u0614\u0616\7G\2\2\u0615\u0617\5`\61\2\u0616\u0615\3\2\2"+
		"\2\u0616\u0617\3\2\2\2\u0617\u0618\3\2\2\2\u0618\u0622\7{\2\2\u0619\u0622"+
		"\7{\2\2\u061a\u061b\5\u0110\u0089\2\u061b\u061c\7{\2\2\u061c\u0622\3\2"+
		"\2\2\u061d\u061e\5`\61\2\u061e\u061f\7\u0084\2\2\u061f\u0620\5\u0082B"+
		"\2\u0620\u0622\3\2\2\2\u0621\u05b8\3\2\2\2\u0621\u05b9\3\2\2\2\u0621\u05c1"+
		"\3\2\2\2\u0621\u05c8\3\2\2\2\u0621\u05ce\3\2\2\2\u0621\u05d2\3\2\2\2\u0621"+
		"\u05d8\3\2\2\2\u0621\u05e5\3\2\2\2\u0621\u05f1\3\2\2\2\u0621\u0602\3\2"+
		"\2\2\u0621\u0606\3\2\2\2\u0621\u060b\3\2\2\2\u0621\u060f\3\2\2\2\u0621"+
		"\u0614\3\2\2\2\u0621\u0619\3\2\2\2\u0621\u061a\3\2\2\2\u0621\u061d\3\2"+
		"\2\2\u0622\u0083\3\2\2\2\u0623\u0624\7C\2\2\u0624\u0628\7u\2\2\u0625\u0627"+
		"\5\u009cO\2\u0626\u0625\3\2\2\2\u0627\u062a\3\2\2\2\u0628\u0626\3\2\2"+
		"\2\u0628\u0629\3\2\2\2\u0629\u062b\3\2\2\2\u062a\u0628\3\2\2\2\u062b\u062c"+
		"\5\u00f6|\2\u062c\u062d\5`\61\2\u062d\u062e\7v\2\2\u062e\u062f\5\u00ee"+
		"x\2\u062f\u0085\3\2\2\2\u0630\u0631\bD\1\2\u0631\u0632\7u\2\2\u0632\u0633"+
		"\5\u00c2b\2\u0633\u0634\7v\2\2\u0634\u0635\5\u0086D\23\u0635\u063e\3\2"+
		"\2\2\u0636\u0637\t\b\2\2\u0637\u063e\5\u0086D\21\u0638\u0639\t\t\2\2\u0639"+
		"\u063e\5\u0086D\20\u063a\u063e\5\u0088E\2\u063b\u063c\7[\2\2\u063c\u063e"+
		"\5\u0114\u008b\2\u063d\u0630\3\2\2\2\u063d\u0636\3\2\2\2\u063d\u0638\3"+
		"\2\2\2\u063d\u063a\3\2\2\2\u063d\u063b\3\2\2\2\u063e\u0694\3\2\2\2\u063f"+
		"\u0640\f\17\2\2\u0640\u0641\t\n\2\2\u0641\u0693\5\u0086D\20\u0642\u0643"+
		"\f\16\2\2\u0643\u0644\t\13\2\2\u0644\u0693\5\u0086D\17\u0645\u064d\f\r"+
		"\2\2\u0646\u0647\7\u0080\2\2\u0647\u064e\7\u0080\2\2\u0648\u0649\7\177"+
		"\2\2\u0649\u064a\7\177\2\2\u064a\u064e\7\177\2\2\u064b\u064c\7\177\2\2"+
		"\u064c\u064e\7\177\2\2\u064d\u0646\3\2\2\2\u064d\u0648\3\2\2\2\u064d\u064b"+
		"\3\2\2\2\u064e\u064f\3\2\2\2\u064f\u0693\5\u0086D\16\u0650\u0651\f\f\2"+
		"\2\u0651\u0652\t\f\2\2\u0652\u0693\5\u0086D\r\u0653\u0654\f\n\2\2\u0654"+
		"\u0655\t\r\2\2\u0655\u0693\5\u0086D\13\u0656\u0657\f\t\2\2\u0657\u0658"+
		"\7\u0091\2\2\u0658\u0693\5\u0086D\n\u0659\u065a\f\b\2\2\u065a\u065b\7"+
		"\u0093\2\2\u065b\u0693\5\u0086D\t\u065c\u065d\f\7\2\2\u065d\u065e\7\u0092"+
		"\2\2\u065e\u0693\5\u0086D\b\u065f\u0660\f\6\2\2\u0660\u0661\7\u0089\2"+
		"\2\u0661\u0693\5\u0086D\7\u0662\u0663\f\5\2\2\u0663\u0664\7\u008a\2\2"+
		"\u0664\u0693\5\u0086D\6\u0665\u0666\f\4\2\2\u0666\u0667\7\u0083\2\2\u0667"+
		"\u0668\5\u0086D\2\u0668\u0669\7\u0084\2\2\u0669\u066a\5\u0086D\5\u066a"+
		"\u0693\3\2\2\2\u066b\u066c\f\3\2\2\u066c\u066d\t\16\2\2\u066d\u0693\5"+
		"\u0086D\4\u066e\u066f\f\33\2\2\u066f\u0670\7}\2\2\u0670\u0693\5`\61\2"+
		"\u0671\u0672\f\32\2\2\u0672\u0673\7}\2\2\u0673\u0693\7g\2\2\u0674\u0675"+
		"\f\31\2\2\u0675\u0676\7}\2\2\u0676\u0678\7[\2\2\u0677\u0679\5\u011c\u008f"+
		"\2\u0678\u0677\3\2\2\2\u0678\u0679\3\2\2\2\u0679\u067a\3\2\2\2\u067a\u0693"+
		"\5\u008cG\2\u067b\u067c\f\30\2\2\u067c\u067d\7}\2\2\u067d\u067e\7d\2\2"+
		"\u067e\u0693\5\u008eH\2\u067f\u0680\f\27\2\2\u0680\u0681\7}\2\2\u0681"+
		"\u0693\5\u011a\u008e\2\u0682\u0683\f\26\2\2\u0683\u0684\7y\2\2\u0684\u0685"+
		"\5\u0086D\2\u0685\u0686\7z\2\2\u0686\u0693\3\2\2\2\u0687\u0688\f\25\2"+
		"\2\u0688\u068a\7u\2\2\u0689\u068b\5\u010e\u0088\2\u068a\u0689\3\2\2\2"+
		"\u068a\u068b\3\2\2\2\u068b\u068c\3\2\2\2\u068c\u0693\7v\2\2\u068d\u068e"+
		"\f\22\2\2\u068e\u0693\t\17\2\2\u068f\u0690\f\13\2\2\u0690\u0691\7V\2\2"+
		"\u0691\u0693\5\u00c2b\2\u0692\u063f\3\2\2\2\u0692\u0642\3\2\2\2\u0692"+
		"\u0645\3\2\2\2\u0692\u0650\3\2\2\2\u0692\u0653\3\2\2\2\u0692\u0656\3\2"+
		"\2\2\u0692\u0659\3\2\2\2\u0692\u065c\3\2\2\2\u0692\u065f\3\2\2\2\u0692"+
		"\u0662\3\2\2\2\u0692\u0665\3\2\2\2\u0692\u066b\3\2\2\2\u0692\u066e\3\2"+
		"\2\2\u0692\u0671\3\2\2\2\u0692\u0674\3\2\2\2\u0692\u067b\3\2\2\2\u0692"+
		"\u067f\3\2\2\2\u0692\u0682\3\2\2\2\u0692\u0687\3\2\2\2\u0692\u068d\3\2"+
		"\2\2\u0692\u068f\3\2\2\2\u0693\u0696\3\2\2\2\u0694\u0692\3\2\2\2\u0694"+
		"\u0695\3\2\2\2\u0695\u0087\3\2\2\2\u0696\u0694\3\2\2\2\u0697\u0698\7u"+
		"\2\2\u0698\u0699\5\u0086D\2\u0699\u069a\7v\2\2\u069a\u06ad\3\2\2\2\u069b"+
		"\u06ad\7g\2\2\u069c\u06ad\7d\2\2\u069d\u06ad\5\u00d8m\2\u069e\u06ad\5"+
		"`\61\2\u069f\u06a0\5\u00c2b\2\u06a0\u06a1\7}\2\2\u06a1\u06a2\7E\2\2\u06a2"+
		"\u06ad\3\2\2\2\u06a3\u06a4\7l\2\2\u06a4\u06a5\7}\2\2\u06a5\u06ad\7E\2"+
		"\2\u06a6\u06aa\5\u011c\u008f\2\u06a7\u06ab\5\u0090I\2\u06a8\u06a9\7g\2"+
		"\2\u06a9\u06ab\5\u0122\u0092\2\u06aa\u06a7\3\2\2\2\u06aa\u06a8\3\2\2\2"+
		"\u06ab\u06ad\3\2\2\2\u06ac\u0697\3\2\2\2\u06ac\u069b\3\2\2\2\u06ac\u069c"+
		"\3\2\2\2\u06ac\u069d\3\2\2\2\u06ac\u069e\3\2\2\2\u06ac\u069f\3\2\2\2\u06ac"+
		"\u06a3\3\2\2\2\u06ac\u06a6\3\2\2\2\u06ad\u0089\3\2\2\2\u06ae\u06b0\5`"+
		"\61\2\u06af\u06b1\5\u011e\u0090\2\u06b0\u06af\3\2\2\2\u06b0\u06b1\3\2"+
		"\2\2\u06b1\u06b9\3\2\2\2\u06b2\u06b3\7}\2\2\u06b3\u06b5\5`\61\2\u06b4"+
		"\u06b6\5\u011e\u0090\2\u06b5\u06b4\3\2\2\2\u06b5\u06b6\3\2\2\2\u06b6\u06b8"+
		"\3\2\2\2\u06b7\u06b2\3\2\2\2\u06b8\u06bb\3\2\2\2\u06b9\u06b7\3\2\2\2\u06b9"+
		"\u06ba\3\2\2\2\u06ba\u06be\3\2\2\2\u06bb\u06b9\3\2\2\2\u06bc\u06be\5\u00c4"+
		"c\2\u06bd\u06ae\3\2\2\2\u06bd\u06bc\3\2\2\2\u06be\u008b\3\2\2\2\u06bf"+
		"\u06c1\5`\61\2\u06c0\u06c2\5\u0120\u0091\2\u06c1\u06c0\3\2\2\2\u06c1\u06c2"+
		"\3\2\2\2\u06c2\u06c3\3\2\2\2\u06c3\u06c4\5\u0118\u008d\2\u06c4\u008d\3"+
		"\2\2\2\u06c5\u06cc\5\u0122\u0092\2\u06c6\u06c7\7}\2\2\u06c7\u06c9\5`\61"+
		"\2\u06c8\u06ca\5\u0122\u0092\2\u06c9\u06c8\3\2\2\2\u06c9\u06ca\3\2\2\2"+
		"\u06ca\u06cc\3\2\2\2\u06cb\u06c5\3\2\2\2\u06cb\u06c6\3\2\2\2\u06cc\u008f"+
		"\3\2\2\2\u06cd\u06ce\7d\2\2\u06ce\u06d3\5\u008eH\2\u06cf\u06d0\5`\61\2"+
		"\u06d0\u06d1\5\u0122\u0092\2\u06d1\u06d3\3\2\2\2\u06d2\u06cd\3\2\2\2\u06d2"+
		"\u06cf\3\2\2\2\u06d3\u0091\3\2\2\2\u06d4\u06d6\5\u0094K\2\u06d5\u06d4"+
		"\3\2\2\2\u06d5\u06d6\3\2\2\2\u06d6\u06da\3\2\2\2\u06d7\u06d9\5\u0096L"+
		"\2\u06d8\u06d7\3\2\2\2\u06d9\u06dc\3\2\2\2\u06da\u06d8\3\2\2\2\u06da\u06db"+
		"\3\2\2\2\u06db\u06e0\3\2\2\2\u06dc\u06da\3\2\2\2\u06dd\u06df\5\2\2\2\u06de"+
		"\u06dd\3\2\2\2\u06df\u06e2\3\2\2\2\u06e0\u06de\3\2\2\2\u06e0\u06e1\3\2"+
		"\2\2\u06e1\u06e3\3\2\2\2\u06e2\u06e0\3\2\2\2\u06e3\u06e4\7\2\2\3\u06e4"+
		"\u0093\3\2\2\2\u06e5\u06e7\5\f\7\2\u06e6\u06e5\3\2\2\2\u06e7\u06ea\3\2"+
		"\2\2\u06e8\u06e6\3\2\2\2\u06e8\u06e9\3\2\2\2\u06e9\u06eb\3\2\2\2\u06ea"+
		"\u06e8\3\2\2\2\u06eb\u06ec\7\\\2\2\u06ec\u06ed\5z>\2\u06ed\u06ee\7{\2"+
		"\2\u06ee\u0095\3\2\2\2\u06ef\u06f1\7U\2\2\u06f0\u06f2\7b\2\2\u06f1\u06f0"+
		"\3\2\2\2\u06f1\u06f2\3\2\2\2\u06f2\u06f3\3\2\2\2\u06f3\u06f6\5z>\2\u06f4"+
		"\u06f5\7}\2\2\u06f5\u06f7\7\u008f\2\2\u06f6\u06f4\3\2\2\2\u06f6\u06f7"+
		"\3\2\2\2\u06f7\u06f8\3\2\2\2\u06f8\u06f9\7{\2\2\u06f9\u0097\3\2\2\2\u06fa"+
		"\u06fd\5\u009aN\2\u06fb\u06fd\t\20\2\2\u06fc\u06fa\3\2\2\2\u06fc\u06fb"+
		"\3\2\2\2\u06fd\u0099\3\2\2\2\u06fe\u0701\5\f\7\2\u06ff\u0701\t\21\2\2"+
		"\u0700\u06fe\3\2\2\2\u0700\u06ff\3\2\2\2\u0701\u009b\3\2\2\2\u0702\u0705"+
		"\7N\2\2\u0703\u0705\5\f\7\2\u0704\u0702\3\2\2\2\u0704\u0703\3\2\2\2\u0705"+
		"\u009d\3\2\2\2\u0706\u0707\7\u0080\2\2\u0707\u070c\5d\63\2\u0708\u0709"+
		"\7|\2\2\u0709\u070b\5d\63\2\u070a\u0708\3\2\2\2\u070b\u070e\3\2\2\2\u070c"+
		"\u070a\3\2\2\2\u070c\u070d\3\2\2\2\u070d\u070f\3\2\2\2\u070e\u070c\3\2"+
		"\2\2\u070f\u0710\7\177\2\2\u0710\u009f\3\2\2\2\u0711\u0716\5\u00c2b\2"+
		"\u0712\u0713\7\u0091\2\2\u0713\u0715\5\u00c2b\2\u0714\u0712\3\2\2\2\u0715"+
		"\u0718\3\2\2\2\u0716\u0714\3\2\2\2\u0716\u0717\3\2\2\2\u0717\u00a1\3\2"+
		"\2\2\u0718\u0716\3\2\2\2\u0719\u071e\5h\65\2\u071a\u071b\7|\2\2\u071b"+
		"\u071d\5h\65\2\u071c\u071a\3\2\2\2\u071d\u0720\3\2\2\2\u071e\u071c\3\2"+
		"\2\2\u071e\u071f\3\2\2\2\u071f\u00a3\3\2\2\2\u0720\u071e\3\2\2\2\u0721"+
		"\u0725\7{\2\2\u0722\u0724\5\6\4\2\u0723\u0722\3\2\2\2\u0724\u0727\3\2"+
		"\2\2\u0725\u0723\3\2\2\2\u0725\u0726\3\2\2\2\u0726\u00a5\3\2\2\2\u0727"+
		"\u0725\3\2\2\2\u0728\u072d\5\u00c2b\2\u0729\u072a\7|\2\2\u072a\u072c\5"+
		"\u00c2b\2\u072b\u0729\3\2\2\2\u072c\u072f\3\2\2\2\u072d\u072b\3\2\2\2"+
		"\u072d\u072e\3\2\2\2\u072e\u00a7\3\2\2\2\u072f\u072d\3\2\2\2\u0730\u0734"+
		"\7w\2\2\u0731\u0733\5\6\4\2\u0732\u0731\3\2\2\2\u0733\u0736\3\2\2\2\u0734"+
		"\u0732\3\2\2\2\u0734\u0735\3\2\2\2\u0735\u0737\3\2\2\2\u0736\u0734\3\2"+
		"\2\2\u0737\u0738\7x\2\2\u0738\u00a9\3\2\2\2\u0739\u073d\7w\2\2\u073a\u073c"+
		"\5\u00b2Z\2\u073b\u073a\3\2\2\2\u073c\u073f\3\2\2\2\u073d\u073b\3\2\2"+
		"\2\u073d\u073e\3\2\2\2\u073e\u0740\3\2\2\2\u073f\u073d\3\2\2\2\u0740\u0741"+
		"\7x\2\2\u0741\u00ab\3\2\2\2\u0742\u0743\5\u009eP\2\u0743\u0744\5l\67\2"+
		"\u0744\u00ad\3\2\2\2\u0745\u0746\5\u009eP\2\u0746\u0747\5n8\2\u0747\u00af"+
		"\3\2\2\2\u0748\u0749\5\u00c2b\2\u0749\u074a\5\u00ba^\2\u074a\u074b\7{"+
		"\2\2\u074b\u00b1\3\2\2\2\u074c\u074e\5\u0098M\2\u074d\u074c\3\2\2\2\u074e"+
		"\u0751\3\2\2\2\u074f\u074d\3\2\2\2\u074f\u0750\3\2\2\2\u0750\u0752\3\2"+
		"\2\2\u0751\u074f\3\2\2\2\u0752\u0755\5\u00b4[\2\u0753\u0755\7{\2\2\u0754"+
		"\u074f\3\2\2\2\u0754\u0753\3\2\2\2\u0755\u00b3\3\2\2\2\u0756\u075e\5\u00b6"+
		"\\\2\u0757\u075e\5r:\2\u0758\u075e\5\u00b8]\2\u0759\u075e\5j\66\2\u075a"+
		"\u075e\5~@\2\u075b\u075e\5b\62\2\u075c\u075e\5f\64\2\u075d\u0756\3\2\2"+
		"\2\u075d\u0757\3\2\2\2\u075d\u0758\3\2\2\2\u075d\u0759\3\2\2\2\u075d\u075a"+
		"\3\2\2\2\u075d\u075b\3\2\2\2\u075d\u075c\3\2\2\2\u075e\u00b5\3\2\2\2\u075f"+
		"\u0760\5\u00c2b\2\u0760\u0765\5p9\2\u0761\u0762\7|\2\2\u0762\u0764\5p"+
		"9\2\u0763\u0761\3\2\2\2\u0764\u0767\3\2\2\2\u0765\u0763\3\2\2\2\u0765"+
		"\u0766\3\2\2\2\u0766\u0768\3\2\2\2\u0767\u0765\3\2\2\2\u0768\u0769\7{"+
		"\2\2\u0769\u00b7\3\2\2\2\u076a\u076b\5\u009eP\2\u076b\u076c\5r:\2\u076c"+
		"\u00b9\3\2\2\2\u076d\u0772\5\u00bc_\2\u076e\u076f\7|\2\2\u076f\u0771\5"+
		"\u00bc_\2\u0770\u076e\3\2\2\2\u0771\u0774\3\2\2\2\u0772\u0770\3\2\2\2"+
		"\u0772\u0773\3\2\2\2\u0773\u00bb\3\2\2\2\u0774\u0772\3\2\2\2\u0775\u0778"+
		"\5t;\2\u0776\u0777\7~\2\2\u0777\u0779\5\u00be`\2\u0778\u0776\3\2\2\2\u0778"+
		"\u0779\3\2\2\2\u0779\u00bd\3\2\2\2\u077a\u077d\5\u00c0a\2\u077b\u077d"+
		"\5\u0086D\2\u077c\u077a\3\2\2\2\u077c\u077b\3\2\2\2\u077d\u00bf\3\2\2"+
		"\2\u077e\u078a\7w\2\2\u077f\u0784\5\u00be`\2\u0780\u0781\7|\2\2\u0781"+
		"\u0783\5\u00be`\2\u0782\u0780\3\2\2\2\u0783\u0786\3\2\2\2\u0784\u0782"+
		"\3\2\2\2\u0784\u0785\3\2\2\2\u0785\u0788\3\2\2\2\u0786\u0784\3\2\2\2\u0787"+
		"\u0789\7|\2\2\u0788\u0787\3\2\2\2\u0788\u0789\3\2\2\2\u0789\u078b\3\2"+
		"\2\2\u078a\u077f\3\2\2\2\u078a\u078b\3\2\2\2\u078b\u078c\3\2\2\2\u078c"+
		"\u078d\7x\2\2\u078d\u00c1\3\2\2\2\u078e\u0793\5x=\2\u078f\u0790\7y\2\2"+
		"\u0790\u0792\7z\2\2\u0791\u078f\3\2\2\2\u0792\u0795\3\2\2\2\u0793\u0791"+
		"\3\2\2\2\u0793\u0794\3\2\2\2\u0794\u079f\3\2\2\2\u0795\u0793\3\2\2\2\u0796"+
		"\u079b\5\u00c4c\2\u0797\u0798\7y\2\2\u0798\u079a\7z\2\2\u0799\u0797\3"+
		"\2\2\2\u079a\u079d\3\2\2\2\u079b\u0799\3\2\2\2\u079b\u079c\3\2\2\2\u079c"+
		"\u079f\3\2\2\2\u079d\u079b\3\2\2\2\u079e\u078e\3\2\2\2\u079e\u0796\3\2"+
		"\2\2\u079f\u00c3\3\2\2\2\u07a0\u07a1\t\22\2\2\u07a1\u00c5\3\2\2\2\u07a2"+
		"\u07a3\7\u0080\2\2\u07a3\u07a8\5\u00c8e\2\u07a4\u07a5\7|\2\2\u07a5\u07a7"+
		"\5\u00c8e\2\u07a6\u07a4\3\2\2\2\u07a7\u07aa\3\2\2\2\u07a8\u07a6\3\2\2"+
		"\2\u07a8\u07a9\3\2\2\2\u07a9\u07ab\3\2\2\2\u07aa\u07a8\3\2\2\2\u07ab\u07ac"+
		"\7\177\2\2\u07ac\u00c7\3\2\2\2\u07ad\u07b4\5\u00c2b\2\u07ae\u07b1\7\u0083"+
		"\2\2\u07af\u07b0\t\23\2\2\u07b0\u07b2\5\u00c2b\2\u07b1\u07af\3\2\2\2\u07b1"+
		"\u07b2\3\2\2\2\u07b2\u07b4\3\2\2\2\u07b3\u07ad\3\2\2\2\u07b3\u07ae\3\2"+
		"\2\2\u07b4\u00c9\3\2\2\2\u07b5\u07ba\5z>\2\u07b6\u07b7\7|\2\2\u07b7\u07b9"+
		"\5z>\2\u07b8\u07b6\3\2\2\2\u07b9\u07bc\3\2\2\2\u07ba\u07b8\3\2\2\2\u07ba"+
		"\u07bb\3\2\2\2\u07bb\u00cb\3\2\2\2\u07bc\u07ba\3\2\2\2\u07bd\u07bf\7u"+
		"\2\2\u07be\u07c0\5\u00ceh\2\u07bf\u07be\3\2\2\2\u07bf\u07c0\3\2\2\2\u07c0"+
		"\u07c1\3\2\2\2\u07c1\u07c2\7v\2\2\u07c2\u00cd\3\2\2\2\u07c3\u07c8\5\u00d0"+
		"i\2\u07c4\u07c5\7|\2\2\u07c5\u07c7\5\u00d0i\2\u07c6\u07c4\3\2\2\2\u07c7"+
		"\u07ca\3\2\2\2\u07c8\u07c6\3\2\2\2\u07c8\u07c9\3\2\2\2\u07c9\u07cd\3\2"+
		"\2\2\u07ca\u07c8\3\2\2\2\u07cb\u07cc\7|\2\2\u07cc\u07ce\5\u00d2j\2\u07cd"+
		"\u07cb\3\2\2\2\u07cd\u07ce\3\2\2\2\u07ce\u07d1\3\2\2\2\u07cf\u07d1\5\u00d2"+
		"j\2\u07d0\u07c3\3\2\2\2\u07d0\u07cf\3\2\2\2\u07d1\u00cf\3\2\2\2\u07d2"+
		"\u07d4\5\u009cO\2\u07d3\u07d2\3\2\2\2\u07d4\u07d7\3\2\2\2\u07d5\u07d3"+
		"\3\2\2\2\u07d5\u07d6\3\2\2\2\u07d6\u07d8\3\2\2\2\u07d7\u07d5\3\2\2\2\u07d8"+
		"\u07d9\5\u00c2b\2\u07d9\u07da\5t;\2\u07da\u00d1\3\2\2\2\u07db\u07dd\5"+
		"\u009cO\2\u07dc\u07db\3\2\2\2\u07dd\u07e0\3\2\2\2\u07de\u07dc\3\2\2\2"+
		"\u07de\u07df\3\2\2\2\u07df\u07e1\3\2\2\2\u07e0\u07de\3\2\2\2\u07e1\u07e2"+
		"\5\u00c2b\2\u07e2\u07e3\7\u00a1\2\2\u07e3\u07e4\5t;\2\u07e4\u00d3\3\2"+
		"\2\2\u07e5\u07e6\5\u00eex\2\u07e6\u00d5\3\2\2\2\u07e7\u07e8\5\u00eex\2"+
		"\u07e8\u00d7\3\2\2\2\u07e9\u07ea\t\24\2\2\u07ea\u00d9\3\2\2\2\u07eb\u07ec"+
		"\5z>\2\u07ec\u00db\3\2\2\2\u07ed\u07f2\5|?\2\u07ee\u07ef\7|\2\2\u07ef"+
		"\u07f1\5|?\2\u07f0\u07ee\3\2\2\2\u07f1\u07f4\3\2\2\2\u07f2\u07f0\3\2\2"+
		"\2\u07f2\u07f3\3\2\2\2\u07f3\u00dd\3\2\2\2\u07f4\u07f2\3\2\2\2\u07f5\u07f9"+
		"\5\u0086D\2\u07f6\u07f9\5\f\7\2\u07f7\u07f9\5\u00e0q\2\u07f8\u07f5\3\2"+
		"\2\2\u07f8\u07f6\3\2\2\2\u07f8\u07f7\3\2\2\2\u07f9\u00df\3\2\2\2\u07fa"+
		"\u0803\7w\2\2\u07fb\u0800\5\u00dep\2\u07fc\u07fd\7|\2\2\u07fd\u07ff\5"+
		"\u00dep\2\u07fe\u07fc\3\2\2\2\u07ff\u0802\3\2\2\2\u0800\u07fe\3\2\2\2"+
		"\u0800\u0801\3\2\2\2\u0801\u0804\3\2\2\2\u0802\u0800\3\2\2\2\u0803\u07fb"+
		"\3\2\2\2\u0803\u0804\3\2\2\2\u0804\u0806\3\2\2\2\u0805\u0807\7|\2\2\u0806"+
		"\u0805\3\2\2\2\u0806\u0807\3\2\2\2\u0807\u0808\3\2\2\2\u0808\u0809\7x"+
		"\2\2\u0809\u00e1\3\2\2\2\u080a\u080e\7w\2\2\u080b\u080d\5\u00e4s\2\u080c"+
		"\u080b\3\2\2\2\u080d\u0810\3\2\2\2\u080e\u080c\3\2\2\2\u080e\u080f\3\2"+
		"\2\2\u080f\u0811\3\2\2\2\u0810\u080e\3\2\2\2\u0811\u0812\7x\2\2\u0812"+
		"\u00e3\3\2\2\2\u0813\u0815\5\u0098M\2\u0814\u0813\3\2\2\2\u0815\u0818"+
		"\3\2\2\2\u0816\u0814\3\2\2\2\u0816\u0817\3\2\2\2\u0817\u0819\3\2\2\2\u0818"+
		"\u0816\3\2\2\2\u0819\u081c\5\u00e6t\2\u081a\u081c\7{\2\2\u081b\u0816\3"+
		"\2\2\2\u081b\u081a\3\2\2\2\u081c\u00e5\3\2\2\2\u081d\u081e\5\u00c2b\2"+
		"\u081e\u081f\5\u00e8u\2\u081f\u0820\7{\2\2\u0820\u0832\3\2\2\2\u0821\u0823"+
		"\5b\62\2\u0822\u0824\7{\2\2\u0823\u0822\3\2\2\2\u0823\u0824\3\2\2\2\u0824"+
		"\u0832\3\2\2\2\u0825\u0827\5j\66\2\u0826\u0828\7{\2\2\u0827\u0826\3\2"+
		"\2\2\u0827\u0828\3\2\2\2\u0828\u0832\3\2\2\2\u0829\u082b\5f\64\2\u082a"+
		"\u082c\7{\2\2\u082b\u082a\3\2\2\2\u082b\u082c\3\2\2\2\u082c\u0832\3\2"+
		"\2\2\u082d\u082f\5~@\2\u082e\u0830\7{\2\2\u082f\u082e\3\2\2\2\u082f\u0830"+
		"\3\2\2\2\u0830\u0832\3\2\2\2\u0831\u081d\3\2\2\2\u0831\u0821\3\2\2\2\u0831"+
		"\u0825\3\2\2\2\u0831\u0829\3\2\2\2\u0831\u082d\3\2\2\2\u0832\u00e7\3\2"+
		"\2\2\u0833\u0836\5\u0080A\2\u0834\u0836\5\u00eav\2\u0835\u0833\3\2\2\2"+
		"\u0835\u0834\3\2\2\2\u0836\u00e9\3\2\2\2\u0837\u0838\5\u00ba^\2\u0838"+
		"\u00eb\3\2\2\2\u0839\u083a\7H\2\2\u083a\u083b\5\u00dep\2\u083b\u00ed\3"+
		"\2\2\2\u083c\u0840\7w\2\2\u083d\u083f\5\u00f0y\2\u083e\u083d\3\2\2\2\u083f"+
		"\u0842\3\2\2\2\u0840\u083e\3\2\2\2\u0840\u0841\3\2\2\2\u0841\u0843\3\2"+
		"\2\2\u0842\u0840\3\2\2\2\u0843\u0844\7x\2\2\u0844\u00ef\3\2\2\2\u0845"+
		"\u0849\5\u00f2z\2\u0846\u0849\5\u0082B\2\u0847\u0849\5\2\2\2\u0848\u0845"+
		"\3\2\2\2\u0848\u0846\3\2\2\2\u0848\u0847\3\2\2\2\u0849\u00f1\3\2\2\2\u084a"+
		"\u084b\5\u00f4{\2\u084b\u084c\7{\2\2\u084c\u00f3\3\2\2\2\u084d\u084f\5"+
		"\u009cO\2\u084e\u084d\3\2\2\2\u084f\u0852\3\2\2\2\u0850\u084e\3\2\2\2"+
		"\u0850\u0851\3\2\2\2\u0851\u0853\3\2\2\2\u0852\u0850\3\2\2\2\u0853\u0854"+
		"\5\u00c2b\2\u0854\u0855\5\u00ba^\2\u0855\u00f5\3\2\2\2\u0856\u085b\5z"+
		">\2\u0857\u0858\7\u0092\2\2\u0858\u085a\5z>\2\u0859\u0857\3\2\2\2\u085a"+
		"\u085d\3\2\2\2\u085b\u0859\3\2\2\2\u085b\u085c\3\2\2\2\u085c\u00f7\3\2"+
		"\2\2\u085d\u085b\3\2\2\2\u085e\u085f\7O\2\2\u085f\u0860\5\u00eex\2\u0860"+
		"\u00f9\3\2\2\2\u0861\u0862\7u\2\2\u0862\u0864\5\u00fc\177\2\u0863\u0865"+
		"\7{\2\2\u0864\u0863\3\2\2\2\u0864\u0865\3\2\2\2\u0865\u0866\3\2\2\2\u0866"+
		"\u0867\7v\2\2\u0867\u00fb\3\2\2\2\u0868\u086d\5\u00fe\u0080\2\u0869\u086a"+
		"\7{\2\2\u086a\u086c\5\u00fe\u0080\2\u086b\u0869\3\2\2\2\u086c\u086f\3"+
		"\2\2\2\u086d\u086b\3\2\2\2\u086d\u086e\3\2\2\2\u086e\u00fd\3\2\2\2\u086f"+
		"\u086d\3\2\2\2\u0870\u0872\5\u009cO\2\u0871\u0870\3\2\2\2\u0872\u0875"+
		"\3\2\2\2\u0873\u0871\3\2\2\2\u0873\u0874\3\2\2\2\u0874\u0876\3\2\2\2\u0875"+
		"\u0873\3\2\2\2\u0876\u0877\5x=\2\u0877\u0878\5t;\2\u0878\u0879\7~\2\2"+
		"\u0879\u087a\5\u0086D\2\u087a\u00ff\3\2\2\2\u087b\u087d\5\u0102\u0082"+
		"\2\u087c\u087b\3\2\2\2\u087d\u087e\3\2\2\2\u087e\u087c\3\2\2\2\u087e\u087f"+
		"\3\2\2\2\u087f\u0881\3\2\2\2\u0880\u0882\5\u00f0y\2\u0881\u0880\3\2\2"+
		"\2\u0882\u0883\3\2\2\2\u0883\u0881\3\2\2\2\u0883\u0884\3\2\2\2\u0884\u0101"+
		"\3\2\2\2\u0885\u0886\7B\2\2\u0886\u0887\5\u0112\u008a\2\u0887\u0888\7"+
		"\u0084\2\2\u0888\u0890\3\2\2\2\u0889\u088a\7B\2\2\u088a\u088b\5v<\2\u088b"+
		"\u088c\7\u0084\2\2\u088c\u0890\3\2\2\2\u088d\u088e\7H\2\2\u088e\u0890"+
		"\7\u0084\2\2\u088f\u0885\3\2\2\2\u088f\u0889\3\2\2\2\u088f\u088d\3\2\2"+
		"\2\u0890\u0103\3\2\2\2\u0891\u089e\5\u0108\u0085\2\u0892\u0894\5\u0106"+
		"\u0084\2\u0893\u0892\3\2\2\2\u0893\u0894\3\2\2\2\u0894\u0895\3\2\2\2\u0895"+
		"\u0897\7{\2\2\u0896\u0898\5\u0086D\2\u0897\u0896\3\2\2\2\u0897\u0898\3"+
		"\2\2\2\u0898\u0899\3\2\2\2\u0899\u089b\7{\2\2\u089a\u089c\5\u010a\u0086"+
		"\2\u089b\u089a\3\2\2\2\u089b\u089c\3\2\2\2\u089c\u089e\3\2\2\2\u089d\u0891"+
		"\3\2\2\2\u089d\u0893\3\2\2\2\u089e\u0105\3\2\2\2\u089f\u08a2\5\u00f4{"+
		"\2\u08a0\u08a2\5\u010e\u0088\2\u08a1\u089f\3\2\2\2\u08a1\u08a0\3\2\2\2"+
		"\u08a2\u0107\3\2\2\2\u08a3\u08a5\5\u009cO\2\u08a4\u08a3\3\2\2\2\u08a5"+
		"\u08a8\3\2\2\2\u08a6\u08a4\3\2\2\2\u08a6\u08a7\3\2\2\2\u08a7\u08a9\3\2"+
		"\2\2\u08a8\u08a6\3\2\2\2\u08a9\u08aa\5\u00c2b\2\u08aa\u08ab\5t;\2\u08ab"+
		"\u08ac\7\u0084\2\2\u08ac\u08ad\5\u0086D\2\u08ad\u0109\3\2\2\2\u08ae\u08af"+
		"\5\u010e\u0088\2\u08af\u010b\3\2\2\2\u08b0\u08b1\7u\2\2\u08b1\u08b2\5"+
		"\u0086D\2\u08b2\u08b3\7v\2\2\u08b3\u010d\3\2\2\2\u08b4\u08b9\5\u0086D"+
		"\2\u08b5\u08b6\7|\2\2\u08b6\u08b8\5\u0086D\2\u08b7\u08b5\3\2\2\2\u08b8"+
		"\u08bb\3\2\2\2\u08b9\u08b7\3\2\2\2\u08b9\u08ba\3\2\2\2\u08ba\u010f\3\2"+
		"\2\2\u08bb\u08b9\3\2\2\2\u08bc\u08bd\5\u0086D\2\u08bd\u0111\3\2\2\2\u08be"+
		"\u08bf\5\u0086D\2\u08bf\u0113\3\2\2\2\u08c0\u08c1\5\u011c\u008f\2\u08c1"+
		"\u08c2\5\u008aF\2\u08c2\u08c3\5\u0118\u008d\2\u08c3\u08ca\3\2\2\2\u08c4"+
		"\u08c7\5\u008aF\2\u08c5\u08c8\5\u0116\u008c\2\u08c6\u08c8\5\u0118\u008d"+
		"\2\u08c7\u08c5\3\2\2\2\u08c7\u08c6\3\2\2\2\u08c8\u08ca\3\2\2\2\u08c9\u08c0"+
		"\3\2\2\2\u08c9\u08c4\3\2\2\2\u08ca\u0115\3\2\2\2\u08cb\u08e7\7y\2\2\u08cc"+
		"\u08d1\7z\2\2\u08cd\u08ce\7y\2\2\u08ce\u08d0\7z\2\2\u08cf\u08cd\3\2\2"+
		"\2\u08d0\u08d3\3\2\2\2\u08d1\u08cf\3\2\2\2\u08d1\u08d2\3\2\2\2\u08d2\u08d4"+
		"\3\2\2\2\u08d3\u08d1\3\2\2\2\u08d4\u08e8\5\u00c0a\2\u08d5\u08d6\5\u0086"+
		"D\2\u08d6\u08dd\7z\2\2\u08d7\u08d8\7y\2\2\u08d8\u08d9\5\u0086D\2\u08d9"+
		"\u08da\7z\2\2\u08da\u08dc\3\2\2\2\u08db\u08d7\3\2\2\2\u08dc\u08df\3\2"+
		"\2\2\u08dd\u08db\3\2\2\2\u08dd\u08de\3\2\2\2\u08de\u08e4\3\2\2\2\u08df"+
		"\u08dd\3\2\2\2\u08e0\u08e1\7y\2\2\u08e1\u08e3\7z\2\2\u08e2\u08e0\3\2\2"+
		"\2\u08e3\u08e6\3\2\2\2\u08e4\u08e2\3\2\2\2\u08e4\u08e5\3\2\2\2\u08e5\u08e8"+
		"\3\2\2\2\u08e6\u08e4\3\2\2\2\u08e7\u08cc\3\2\2\2\u08e7\u08d5\3\2\2\2\u08e8"+
		"\u0117\3\2\2\2\u08e9\u08eb\5\u0122\u0092\2\u08ea\u08ec\5\u00a8U\2\u08eb"+
		"\u08ea\3\2\2\2\u08eb\u08ec\3\2\2\2\u08ec\u0119\3\2\2\2\u08ed\u08ee\5\u011c"+
		"\u008f\2\u08ee\u08ef\5\u0090I\2\u08ef\u011b\3\2\2\2\u08f0\u08f1\7\u0080"+
		"\2\2\u08f1\u08f2\5\u00a6T\2\u08f2\u08f3\7\177\2\2\u08f3\u011d\3\2\2\2"+
		"\u08f4\u08f5\7\u0080\2\2\u08f5\u08f8\7\177\2\2\u08f6\u08f8\5\u00c6d\2"+
		"\u08f7\u08f4\3\2\2\2\u08f7\u08f6\3\2\2\2\u08f8\u011f\3\2\2\2\u08f9\u08fa"+
		"\7\u0080\2\2\u08fa\u08fd\7\177\2\2\u08fb\u08fd\5\u011c\u008f\2\u08fc\u08f9"+
		"\3\2\2\2\u08fc\u08fb\3\2\2\2\u08fd\u0121\3\2\2\2\u08fe\u0900\7u\2\2\u08ff"+
		"\u0901\5\u010e\u0088\2\u0900\u08ff\3\2\2\2\u0900\u0901\3\2\2\2\u0901\u0902"+
		"\3\2\2\2\u0902\u0903\7v\2\2\u0903\u0123\3\2\2\2\u00fe\u0127\u012e\u0135"+
		"\u013c\u0143\u0148\u014e\u0155\u015b\u0161\u0167\u0173\u017a\u017d\u01b7"+
		"\u0204\u0208\u020f\u021a\u021e\u0223\u022a\u022e\u0231\u0236\u023b\u024a"+
		"\u024d\u0254\u0257\u025b\u025f\u027d\u0283\u028e\u0298\u029f\u02a5\u02ad"+
		"\u02af\u02f4\u032a\u032f\u0337\u033c\u0344\u034b\u0352\u0357\u035f\u0366"+
		"\u036f\u0376\u037b\u0382\u0386\u03da\u03e2\u03e7\u03ef\u03f1\u03f6\u03fc"+
		"\u0404\u0406\u0409\u040d\u0413\u0416\u0419\u041c\u0422\u0427\u042d\u0439"+
		"\u043d\u0445\u0449\u044b\u044f\u0452\u0455\u045b\u0460\u0463\u0469\u0473"+
		"\u047b\u0481\u0488\u048e\u0498\u049c\u049f\u04a4\u04a9\u04ac\u04b2\u04b8"+
		"\u04bf\u04c7\u04cb\u04d1\u04d5\u04da\u04e1\u04e9\u04ec\u04f3\u04fb\u04fe"+
		"\u0502\u0509\u050e\u0513\u0517\u051b\u0522\u0528\u052c\u052f\u0532\u0539"+
		"\u053e\u0541\u0546\u054a\u0550\u0558\u055d\u0561\u0567\u0570\u0578\u0580"+
		"\u0585\u058e\u0595\u059a\u059e\u05a6\u05b6\u05bd\u05c6\u05dd\u05e0\u05e3"+
		"\u05eb\u05ef\u05f7\u05fd\u0608\u0611\u0616\u0621\u0628\u063d\u064d\u0678"+
		"\u068a\u0692\u0694\u06aa\u06ac\u06b0\u06b5\u06b9\u06bd\u06c1\u06c9\u06cb"+
		"\u06d2\u06d5\u06da\u06e0\u06e8\u06f1\u06f6\u06fc\u0700\u0704\u070c\u0716"+
		"\u071e\u0725\u072d\u0734\u073d\u074f\u0754\u075d\u0765\u0772\u0778\u077c"+
		"\u0784\u0788\u078a\u0793\u079b\u079e\u07a8\u07b1\u07b3\u07ba\u07bf\u07c8"+
		"\u07cd\u07d0\u07d5\u07de\u07f2\u07f8\u0800\u0803\u0806\u080e\u0816\u081b"+
		"\u0823\u0827\u082b\u082f\u0831\u0835\u0840\u0848\u0850\u085b\u0864\u086d"+
		"\u0873\u087e\u0883\u088f\u0893\u0897\u089b\u089d\u08a1\u08a6\u08b9\u08c7"+
		"\u08c9\u08d1\u08dd\u08e4\u08e7\u08eb\u08f7\u08fc\u0900";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}