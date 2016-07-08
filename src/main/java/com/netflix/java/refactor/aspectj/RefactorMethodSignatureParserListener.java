// Generated from /Users/joschneider/Projects/github/jkschneider/java-source-linter/src/main/antlr4/RefactorMethodSignatureParser.g4 by ANTLR 4.2.2
package com.netflix.java.refactor.aspectj;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link RefactorMethodSignatureParser}.
 */
public interface RefactorMethodSignatureParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#memberDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterMemberDeclaration(@NotNull RefactorMethodSignatureParser.MemberDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#memberDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitMemberDeclaration(@NotNull RefactorMethodSignatureParser.MemberDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#defaultValue}.
	 * @param ctx the parse tree
	 */
	void enterDefaultValue(@NotNull RefactorMethodSignatureParser.DefaultValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#defaultValue}.
	 * @param ctx the parse tree
	 */
	void exitDefaultValue(@NotNull RefactorMethodSignatureParser.DefaultValueContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#annotationTypeElementDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationTypeElementDeclaration(@NotNull RefactorMethodSignatureParser.AnnotationTypeElementDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#annotationTypeElementDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationTypeElementDeclaration(@NotNull RefactorMethodSignatureParser.AnnotationTypeElementDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(@NotNull RefactorMethodSignatureParser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(@NotNull RefactorMethodSignatureParser.TypeContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#annotationTypeBody}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationTypeBody(@NotNull RefactorMethodSignatureParser.AnnotationTypeBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#annotationTypeBody}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationTypeBody(@NotNull RefactorMethodSignatureParser.AnnotationTypeBodyContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#genericInterfaceMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterGenericInterfaceMethodDeclaration(@NotNull RefactorMethodSignatureParser.GenericInterfaceMethodDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#genericInterfaceMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitGenericInterfaceMethodDeclaration(@NotNull RefactorMethodSignatureParser.GenericInterfaceMethodDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#classBodyDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterClassBodyDeclaration(@NotNull RefactorMethodSignatureParser.ClassBodyDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#classBodyDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitClassBodyDeclaration(@NotNull RefactorMethodSignatureParser.ClassBodyDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#formalsPattern}.
	 * @param ctx the parse tree
	 */
	void enterFormalsPattern(@NotNull RefactorMethodSignatureParser.FormalsPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#formalsPattern}.
	 * @param ctx the parse tree
	 */
	void exitFormalsPattern(@NotNull RefactorMethodSignatureParser.FormalsPatternContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#block}.
	 * @param ctx the parse tree
	 */
	void enterBlock(@NotNull RefactorMethodSignatureParser.BlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#block}.
	 * @param ctx the parse tree
	 */
	void exitBlock(@NotNull RefactorMethodSignatureParser.BlockContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#enumBodyDeclarations}.
	 * @param ctx the parse tree
	 */
	void enterEnumBodyDeclarations(@NotNull RefactorMethodSignatureParser.EnumBodyDeclarationsContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#enumBodyDeclarations}.
	 * @param ctx the parse tree
	 */
	void exitEnumBodyDeclarations(@NotNull RefactorMethodSignatureParser.EnumBodyDeclarationsContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#forUpdate}.
	 * @param ctx the parse tree
	 */
	void enterForUpdate(@NotNull RefactorMethodSignatureParser.ForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#forUpdate}.
	 * @param ctx the parse tree
	 */
	void exitForUpdate(@NotNull RefactorMethodSignatureParser.ForUpdateContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#optionalParensTypePattern}.
	 * @param ctx the parse tree
	 */
	void enterOptionalParensTypePattern(@NotNull RefactorMethodSignatureParser.OptionalParensTypePatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#optionalParensTypePattern}.
	 * @param ctx the parse tree
	 */
	void exitOptionalParensTypePattern(@NotNull RefactorMethodSignatureParser.OptionalParensTypePatternContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#enhancedForControl}.
	 * @param ctx the parse tree
	 */
	void enterEnhancedForControl(@NotNull RefactorMethodSignatureParser.EnhancedForControlContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#enhancedForControl}.
	 * @param ctx the parse tree
	 */
	void exitEnhancedForControl(@NotNull RefactorMethodSignatureParser.EnhancedForControlContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#annotationConstantRest}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationConstantRest(@NotNull RefactorMethodSignatureParser.AnnotationConstantRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#annotationConstantRest}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationConstantRest(@NotNull RefactorMethodSignatureParser.AnnotationConstantRestContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#explicitGenericInvocation}.
	 * @param ctx the parse tree
	 */
	void enterExplicitGenericInvocation(@NotNull RefactorMethodSignatureParser.ExplicitGenericInvocationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#explicitGenericInvocation}.
	 * @param ctx the parse tree
	 */
	void exitExplicitGenericInvocation(@NotNull RefactorMethodSignatureParser.ExplicitGenericInvocationContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#nonWildcardTypeArgumentsOrDiamond}.
	 * @param ctx the parse tree
	 */
	void enterNonWildcardTypeArgumentsOrDiamond(@NotNull RefactorMethodSignatureParser.NonWildcardTypeArgumentsOrDiamondContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#nonWildcardTypeArgumentsOrDiamond}.
	 * @param ctx the parse tree
	 */
	void exitNonWildcardTypeArgumentsOrDiamond(@NotNull RefactorMethodSignatureParser.NonWildcardTypeArgumentsOrDiamondContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#expressionList}.
	 * @param ctx the parse tree
	 */
	void enterExpressionList(@NotNull RefactorMethodSignatureParser.ExpressionListContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#expressionList}.
	 * @param ctx the parse tree
	 */
	void exitExpressionList(@NotNull RefactorMethodSignatureParser.ExpressionListContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#formalParametersPattern}.
	 * @param ctx the parse tree
	 */
	void enterFormalParametersPattern(@NotNull RefactorMethodSignatureParser.FormalParametersPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#formalParametersPattern}.
	 * @param ctx the parse tree
	 */
	void exitFormalParametersPattern(@NotNull RefactorMethodSignatureParser.FormalParametersPatternContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#annotationTypeElementRest}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationTypeElementRest(@NotNull RefactorMethodSignatureParser.AnnotationTypeElementRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#annotationTypeElementRest}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationTypeElementRest(@NotNull RefactorMethodSignatureParser.AnnotationTypeElementRestContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#classOrInterfaceType}.
	 * @param ctx the parse tree
	 */
	void enterClassOrInterfaceType(@NotNull RefactorMethodSignatureParser.ClassOrInterfaceTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#classOrInterfaceType}.
	 * @param ctx the parse tree
	 */
	void exitClassOrInterfaceType(@NotNull RefactorMethodSignatureParser.ClassOrInterfaceTypeContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#typePatternList}.
	 * @param ctx the parse tree
	 */
	void enterTypePatternList(@NotNull RefactorMethodSignatureParser.TypePatternListContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#typePatternList}.
	 * @param ctx the parse tree
	 */
	void exitTypePatternList(@NotNull RefactorMethodSignatureParser.TypePatternListContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#typeBound}.
	 * @param ctx the parse tree
	 */
	void enterTypeBound(@NotNull RefactorMethodSignatureParser.TypeBoundContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#typeBound}.
	 * @param ctx the parse tree
	 */
	void exitTypeBound(@NotNull RefactorMethodSignatureParser.TypeBoundContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#variableDeclaratorId}.
	 * @param ctx the parse tree
	 */
	void enterVariableDeclaratorId(@NotNull RefactorMethodSignatureParser.VariableDeclaratorIdContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#variableDeclaratorId}.
	 * @param ctx the parse tree
	 */
	void exitVariableDeclaratorId(@NotNull RefactorMethodSignatureParser.VariableDeclaratorIdContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#primary}.
	 * @param ctx the parse tree
	 */
	void enterPrimary(@NotNull RefactorMethodSignatureParser.PrimaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#primary}.
	 * @param ctx the parse tree
	 */
	void exitPrimary(@NotNull RefactorMethodSignatureParser.PrimaryContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#classCreatorRest}.
	 * @param ctx the parse tree
	 */
	void enterClassCreatorRest(@NotNull RefactorMethodSignatureParser.ClassCreatorRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#classCreatorRest}.
	 * @param ctx the parse tree
	 */
	void exitClassCreatorRest(@NotNull RefactorMethodSignatureParser.ClassCreatorRestContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#interfaceBodyDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceBodyDeclaration(@NotNull RefactorMethodSignatureParser.InterfaceBodyDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#interfaceBodyDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceBodyDeclaration(@NotNull RefactorMethodSignatureParser.InterfaceBodyDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#typeArguments}.
	 * @param ctx the parse tree
	 */
	void enterTypeArguments(@NotNull RefactorMethodSignatureParser.TypeArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#typeArguments}.
	 * @param ctx the parse tree
	 */
	void exitTypeArguments(@NotNull RefactorMethodSignatureParser.TypeArgumentsContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#annotationName}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationName(@NotNull RefactorMethodSignatureParser.AnnotationNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#annotationName}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationName(@NotNull RefactorMethodSignatureParser.AnnotationNameContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#finallyBlock}.
	 * @param ctx the parse tree
	 */
	void enterFinallyBlock(@NotNull RefactorMethodSignatureParser.FinallyBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#finallyBlock}.
	 * @param ctx the parse tree
	 */
	void exitFinallyBlock(@NotNull RefactorMethodSignatureParser.FinallyBlockContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#typeParameters}.
	 * @param ctx the parse tree
	 */
	void enterTypeParameters(@NotNull RefactorMethodSignatureParser.TypeParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#typeParameters}.
	 * @param ctx the parse tree
	 */
	void exitTypeParameters(@NotNull RefactorMethodSignatureParser.TypeParametersContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#lastFormalParameter}.
	 * @param ctx the parse tree
	 */
	void enterLastFormalParameter(@NotNull RefactorMethodSignatureParser.LastFormalParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#lastFormalParameter}.
	 * @param ctx the parse tree
	 */
	void exitLastFormalParameter(@NotNull RefactorMethodSignatureParser.LastFormalParameterContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#constructorBody}.
	 * @param ctx the parse tree
	 */
	void enterConstructorBody(@NotNull RefactorMethodSignatureParser.ConstructorBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#constructorBody}.
	 * @param ctx the parse tree
	 */
	void exitConstructorBody(@NotNull RefactorMethodSignatureParser.ConstructorBodyContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLiteral(@NotNull RefactorMethodSignatureParser.LiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLiteral(@NotNull RefactorMethodSignatureParser.LiteralContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#annotationMethodOrConstantRest}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationMethodOrConstantRest(@NotNull RefactorMethodSignatureParser.AnnotationMethodOrConstantRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#annotationMethodOrConstantRest}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationMethodOrConstantRest(@NotNull RefactorMethodSignatureParser.AnnotationMethodOrConstantRestContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#catchClause}.
	 * @param ctx the parse tree
	 */
	void enterCatchClause(@NotNull RefactorMethodSignatureParser.CatchClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#catchClause}.
	 * @param ctx the parse tree
	 */
	void exitCatchClause(@NotNull RefactorMethodSignatureParser.CatchClauseContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#variableDeclarator}.
	 * @param ctx the parse tree
	 */
	void enterVariableDeclarator(@NotNull RefactorMethodSignatureParser.VariableDeclaratorContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#variableDeclarator}.
	 * @param ctx the parse tree
	 */
	void exitVariableDeclarator(@NotNull RefactorMethodSignatureParser.VariableDeclaratorContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#typeList}.
	 * @param ctx the parse tree
	 */
	void enterTypeList(@NotNull RefactorMethodSignatureParser.TypeListContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#typeList}.
	 * @param ctx the parse tree
	 */
	void exitTypeList(@NotNull RefactorMethodSignatureParser.TypeListContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#enumConstants}.
	 * @param ctx the parse tree
	 */
	void enterEnumConstants(@NotNull RefactorMethodSignatureParser.EnumConstantsContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#enumConstants}.
	 * @param ctx the parse tree
	 */
	void exitEnumConstants(@NotNull RefactorMethodSignatureParser.EnumConstantsContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#classBody}.
	 * @param ctx the parse tree
	 */
	void enterClassBody(@NotNull RefactorMethodSignatureParser.ClassBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#classBody}.
	 * @param ctx the parse tree
	 */
	void exitClassBody(@NotNull RefactorMethodSignatureParser.ClassBodyContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#createdName}.
	 * @param ctx the parse tree
	 */
	void enterCreatedName(@NotNull RefactorMethodSignatureParser.CreatedNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#createdName}.
	 * @param ctx the parse tree
	 */
	void exitCreatedName(@NotNull RefactorMethodSignatureParser.CreatedNameContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#enumDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterEnumDeclaration(@NotNull RefactorMethodSignatureParser.EnumDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#enumDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitEnumDeclaration(@NotNull RefactorMethodSignatureParser.EnumDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#formalParameter}.
	 * @param ctx the parse tree
	 */
	void enterFormalParameter(@NotNull RefactorMethodSignatureParser.FormalParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#formalParameter}.
	 * @param ctx the parse tree
	 */
	void exitFormalParameter(@NotNull RefactorMethodSignatureParser.FormalParameterContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#parExpression}.
	 * @param ctx the parse tree
	 */
	void enterParExpression(@NotNull RefactorMethodSignatureParser.ParExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#parExpression}.
	 * @param ctx the parse tree
	 */
	void exitParExpression(@NotNull RefactorMethodSignatureParser.ParExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#annotation}.
	 * @param ctx the parse tree
	 */
	void enterAnnotation(@NotNull RefactorMethodSignatureParser.AnnotationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#annotation}.
	 * @param ctx the parse tree
	 */
	void exitAnnotation(@NotNull RefactorMethodSignatureParser.AnnotationContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#variableInitializer}.
	 * @param ctx the parse tree
	 */
	void enterVariableInitializer(@NotNull RefactorMethodSignatureParser.VariableInitializerContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#variableInitializer}.
	 * @param ctx the parse tree
	 */
	void exitVariableInitializer(@NotNull RefactorMethodSignatureParser.VariableInitializerContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#elementValueArrayInitializer}.
	 * @param ctx the parse tree
	 */
	void enterElementValueArrayInitializer(@NotNull RefactorMethodSignatureParser.ElementValueArrayInitializerContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#elementValueArrayInitializer}.
	 * @param ctx the parse tree
	 */
	void exitElementValueArrayInitializer(@NotNull RefactorMethodSignatureParser.ElementValueArrayInitializerContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#creator}.
	 * @param ctx the parse tree
	 */
	void enterCreator(@NotNull RefactorMethodSignatureParser.CreatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#creator}.
	 * @param ctx the parse tree
	 */
	void exitCreator(@NotNull RefactorMethodSignatureParser.CreatorContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#arrayCreatorRest}.
	 * @param ctx the parse tree
	 */
	void enterArrayCreatorRest(@NotNull RefactorMethodSignatureParser.ArrayCreatorRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#arrayCreatorRest}.
	 * @param ctx the parse tree
	 */
	void exitArrayCreatorRest(@NotNull RefactorMethodSignatureParser.ArrayCreatorRestContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#methodModifiersPattern}.
	 * @param ctx the parse tree
	 */
	void enterMethodModifiersPattern(@NotNull RefactorMethodSignatureParser.MethodModifiersPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#methodModifiersPattern}.
	 * @param ctx the parse tree
	 */
	void exitMethodModifiersPattern(@NotNull RefactorMethodSignatureParser.MethodModifiersPatternContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(@NotNull RefactorMethodSignatureParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(@NotNull RefactorMethodSignatureParser.ExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#constantExpression}.
	 * @param ctx the parse tree
	 */
	void enterConstantExpression(@NotNull RefactorMethodSignatureParser.ConstantExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#constantExpression}.
	 * @param ctx the parse tree
	 */
	void exitConstantExpression(@NotNull RefactorMethodSignatureParser.ConstantExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#methodPattern}.
	 * @param ctx the parse tree
	 */
	void enterMethodPattern(@NotNull RefactorMethodSignatureParser.MethodPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#methodPattern}.
	 * @param ctx the parse tree
	 */
	void exitMethodPattern(@NotNull RefactorMethodSignatureParser.MethodPatternContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#qualifiedNameList}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedNameList(@NotNull RefactorMethodSignatureParser.QualifiedNameListContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#qualifiedNameList}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedNameList(@NotNull RefactorMethodSignatureParser.QualifiedNameListContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#simpleTypePattern}.
	 * @param ctx the parse tree
	 */
	void enterSimpleTypePattern(@NotNull RefactorMethodSignatureParser.SimpleTypePatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#simpleTypePattern}.
	 * @param ctx the parse tree
	 */
	void exitSimpleTypePattern(@NotNull RefactorMethodSignatureParser.SimpleTypePatternContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#constructorDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterConstructorDeclaration(@NotNull RefactorMethodSignatureParser.ConstructorDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#constructorDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitConstructorDeclaration(@NotNull RefactorMethodSignatureParser.ConstructorDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#forControl}.
	 * @param ctx the parse tree
	 */
	void enterForControl(@NotNull RefactorMethodSignatureParser.ForControlContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#forControl}.
	 * @param ctx the parse tree
	 */
	void exitForControl(@NotNull RefactorMethodSignatureParser.ForControlContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#superSuffix}.
	 * @param ctx the parse tree
	 */
	void enterSuperSuffix(@NotNull RefactorMethodSignatureParser.SuperSuffixContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#superSuffix}.
	 * @param ctx the parse tree
	 */
	void exitSuperSuffix(@NotNull RefactorMethodSignatureParser.SuperSuffixContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#variableDeclarators}.
	 * @param ctx the parse tree
	 */
	void enterVariableDeclarators(@NotNull RefactorMethodSignatureParser.VariableDeclaratorsContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#variableDeclarators}.
	 * @param ctx the parse tree
	 */
	void exitVariableDeclarators(@NotNull RefactorMethodSignatureParser.VariableDeclaratorsContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#catchType}.
	 * @param ctx the parse tree
	 */
	void enterCatchType(@NotNull RefactorMethodSignatureParser.CatchTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#catchType}.
	 * @param ctx the parse tree
	 */
	void exitCatchType(@NotNull RefactorMethodSignatureParser.CatchTypeContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#formalsPatternAfterDotDot}.
	 * @param ctx the parse tree
	 */
	void enterFormalsPatternAfterDotDot(@NotNull RefactorMethodSignatureParser.FormalsPatternAfterDotDotContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#formalsPatternAfterDotDot}.
	 * @param ctx the parse tree
	 */
	void exitFormalsPatternAfterDotDot(@NotNull RefactorMethodSignatureParser.FormalsPatternAfterDotDotContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#classOrInterfaceModifier}.
	 * @param ctx the parse tree
	 */
	void enterClassOrInterfaceModifier(@NotNull RefactorMethodSignatureParser.ClassOrInterfaceModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#classOrInterfaceModifier}.
	 * @param ctx the parse tree
	 */
	void exitClassOrInterfaceModifier(@NotNull RefactorMethodSignatureParser.ClassOrInterfaceModifierContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#enumConstantName}.
	 * @param ctx the parse tree
	 */
	void enterEnumConstantName(@NotNull RefactorMethodSignatureParser.EnumConstantNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#enumConstantName}.
	 * @param ctx the parse tree
	 */
	void exitEnumConstantName(@NotNull RefactorMethodSignatureParser.EnumConstantNameContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#modifier}.
	 * @param ctx the parse tree
	 */
	void enterModifier(@NotNull RefactorMethodSignatureParser.ModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#modifier}.
	 * @param ctx the parse tree
	 */
	void exitModifier(@NotNull RefactorMethodSignatureParser.ModifierContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#innerCreator}.
	 * @param ctx the parse tree
	 */
	void enterInnerCreator(@NotNull RefactorMethodSignatureParser.InnerCreatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#innerCreator}.
	 * @param ctx the parse tree
	 */
	void exitInnerCreator(@NotNull RefactorMethodSignatureParser.InnerCreatorContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#explicitGenericInvocationSuffix}.
	 * @param ctx the parse tree
	 */
	void enterExplicitGenericInvocationSuffix(@NotNull RefactorMethodSignatureParser.ExplicitGenericInvocationSuffixContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#explicitGenericInvocationSuffix}.
	 * @param ctx the parse tree
	 */
	void exitExplicitGenericInvocationSuffix(@NotNull RefactorMethodSignatureParser.ExplicitGenericInvocationSuffixContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#variableModifier}.
	 * @param ctx the parse tree
	 */
	void enterVariableModifier(@NotNull RefactorMethodSignatureParser.VariableModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#variableModifier}.
	 * @param ctx the parse tree
	 */
	void exitVariableModifier(@NotNull RefactorMethodSignatureParser.VariableModifierContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#elementValuePair}.
	 * @param ctx the parse tree
	 */
	void enterElementValuePair(@NotNull RefactorMethodSignatureParser.ElementValuePairContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#elementValuePair}.
	 * @param ctx the parse tree
	 */
	void exitElementValuePair(@NotNull RefactorMethodSignatureParser.ElementValuePairContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#arrayInitializer}.
	 * @param ctx the parse tree
	 */
	void enterArrayInitializer(@NotNull RefactorMethodSignatureParser.ArrayInitializerContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#arrayInitializer}.
	 * @param ctx the parse tree
	 */
	void exitArrayInitializer(@NotNull RefactorMethodSignatureParser.ArrayInitializerContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#elementValue}.
	 * @param ctx the parse tree
	 */
	void enterElementValue(@NotNull RefactorMethodSignatureParser.ElementValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#elementValue}.
	 * @param ctx the parse tree
	 */
	void exitElementValue(@NotNull RefactorMethodSignatureParser.ElementValueContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#methodModifier}.
	 * @param ctx the parse tree
	 */
	void enterMethodModifier(@NotNull RefactorMethodSignatureParser.MethodModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#methodModifier}.
	 * @param ctx the parse tree
	 */
	void exitMethodModifier(@NotNull RefactorMethodSignatureParser.MethodModifierContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#constDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterConstDeclaration(@NotNull RefactorMethodSignatureParser.ConstDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#constDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitConstDeclaration(@NotNull RefactorMethodSignatureParser.ConstDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#resource}.
	 * @param ctx the parse tree
	 */
	void enterResource(@NotNull RefactorMethodSignatureParser.ResourceContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#resource}.
	 * @param ctx the parse tree
	 */
	void exitResource(@NotNull RefactorMethodSignatureParser.ResourceContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedName(@NotNull RefactorMethodSignatureParser.QualifiedNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedName(@NotNull RefactorMethodSignatureParser.QualifiedNameContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#resourceSpecification}.
	 * @param ctx the parse tree
	 */
	void enterResourceSpecification(@NotNull RefactorMethodSignatureParser.ResourceSpecificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#resourceSpecification}.
	 * @param ctx the parse tree
	 */
	void exitResourceSpecification(@NotNull RefactorMethodSignatureParser.ResourceSpecificationContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#formalParameterList}.
	 * @param ctx the parse tree
	 */
	void enterFormalParameterList(@NotNull RefactorMethodSignatureParser.FormalParameterListContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#formalParameterList}.
	 * @param ctx the parse tree
	 */
	void exitFormalParameterList(@NotNull RefactorMethodSignatureParser.FormalParameterListContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#annotationTypeDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationTypeDeclaration(@NotNull RefactorMethodSignatureParser.AnnotationTypeDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#annotationTypeDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationTypeDeclaration(@NotNull RefactorMethodSignatureParser.AnnotationTypeDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#compilationUnit}.
	 * @param ctx the parse tree
	 */
	void enterCompilationUnit(@NotNull RefactorMethodSignatureParser.CompilationUnitContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#compilationUnit}.
	 * @param ctx the parse tree
	 */
	void exitCompilationUnit(@NotNull RefactorMethodSignatureParser.CompilationUnitContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#annotationMethodRest}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationMethodRest(@NotNull RefactorMethodSignatureParser.AnnotationMethodRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#annotationMethodRest}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationMethodRest(@NotNull RefactorMethodSignatureParser.AnnotationMethodRestContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#switchBlockStatementGroup}.
	 * @param ctx the parse tree
	 */
	void enterSwitchBlockStatementGroup(@NotNull RefactorMethodSignatureParser.SwitchBlockStatementGroupContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#switchBlockStatementGroup}.
	 * @param ctx the parse tree
	 */
	void exitSwitchBlockStatementGroup(@NotNull RefactorMethodSignatureParser.SwitchBlockStatementGroupContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#typeParameter}.
	 * @param ctx the parse tree
	 */
	void enterTypeParameter(@NotNull RefactorMethodSignatureParser.TypeParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#typeParameter}.
	 * @param ctx the parse tree
	 */
	void exitTypeParameter(@NotNull RefactorMethodSignatureParser.TypeParameterContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#interfaceBody}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceBody(@NotNull RefactorMethodSignatureParser.InterfaceBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#interfaceBody}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceBody(@NotNull RefactorMethodSignatureParser.InterfaceBodyContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#methodDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterMethodDeclaration(@NotNull RefactorMethodSignatureParser.MethodDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#methodDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitMethodDeclaration(@NotNull RefactorMethodSignatureParser.MethodDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#methodBody}.
	 * @param ctx the parse tree
	 */
	void enterMethodBody(@NotNull RefactorMethodSignatureParser.MethodBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#methodBody}.
	 * @param ctx the parse tree
	 */
	void exitMethodBody(@NotNull RefactorMethodSignatureParser.MethodBodyContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#typeArgument}.
	 * @param ctx the parse tree
	 */
	void enterTypeArgument(@NotNull RefactorMethodSignatureParser.TypeArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#typeArgument}.
	 * @param ctx the parse tree
	 */
	void exitTypeArgument(@NotNull RefactorMethodSignatureParser.TypeArgumentContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#typePattern}.
	 * @param ctx the parse tree
	 */
	void enterTypePattern(@NotNull RefactorMethodSignatureParser.TypePatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#typePattern}.
	 * @param ctx the parse tree
	 */
	void exitTypePattern(@NotNull RefactorMethodSignatureParser.TypePatternContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#typeDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterTypeDeclaration(@NotNull RefactorMethodSignatureParser.TypeDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#typeDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitTypeDeclaration(@NotNull RefactorMethodSignatureParser.TypeDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#genericConstructorDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterGenericConstructorDeclaration(@NotNull RefactorMethodSignatureParser.GenericConstructorDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#genericConstructorDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitGenericConstructorDeclaration(@NotNull RefactorMethodSignatureParser.GenericConstructorDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#classDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterClassDeclaration(@NotNull RefactorMethodSignatureParser.ClassDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#classDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitClassDeclaration(@NotNull RefactorMethodSignatureParser.ClassDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#dottedNamePattern}.
	 * @param ctx the parse tree
	 */
	void enterDottedNamePattern(@NotNull RefactorMethodSignatureParser.DottedNamePatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#dottedNamePattern}.
	 * @param ctx the parse tree
	 */
	void exitDottedNamePattern(@NotNull RefactorMethodSignatureParser.DottedNamePatternContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#enumConstant}.
	 * @param ctx the parse tree
	 */
	void enterEnumConstant(@NotNull RefactorMethodSignatureParser.EnumConstantContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#enumConstant}.
	 * @param ctx the parse tree
	 */
	void exitEnumConstant(@NotNull RefactorMethodSignatureParser.EnumConstantContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(@NotNull RefactorMethodSignatureParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(@NotNull RefactorMethodSignatureParser.StatementContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#importDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterImportDeclaration(@NotNull RefactorMethodSignatureParser.ImportDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#importDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitImportDeclaration(@NotNull RefactorMethodSignatureParser.ImportDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#primitiveType}.
	 * @param ctx the parse tree
	 */
	void enterPrimitiveType(@NotNull RefactorMethodSignatureParser.PrimitiveTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#primitiveType}.
	 * @param ctx the parse tree
	 */
	void exitPrimitiveType(@NotNull RefactorMethodSignatureParser.PrimitiveTypeContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#interfaceDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceDeclaration(@NotNull RefactorMethodSignatureParser.InterfaceDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#interfaceDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceDeclaration(@NotNull RefactorMethodSignatureParser.InterfaceDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#localVariableDeclarationStatement}.
	 * @param ctx the parse tree
	 */
	void enterLocalVariableDeclarationStatement(@NotNull RefactorMethodSignatureParser.LocalVariableDeclarationStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#localVariableDeclarationStatement}.
	 * @param ctx the parse tree
	 */
	void exitLocalVariableDeclarationStatement(@NotNull RefactorMethodSignatureParser.LocalVariableDeclarationStatementContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#blockStatement}.
	 * @param ctx the parse tree
	 */
	void enterBlockStatement(@NotNull RefactorMethodSignatureParser.BlockStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#blockStatement}.
	 * @param ctx the parse tree
	 */
	void exitBlockStatement(@NotNull RefactorMethodSignatureParser.BlockStatementContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#fieldDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterFieldDeclaration(@NotNull RefactorMethodSignatureParser.FieldDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#fieldDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitFieldDeclaration(@NotNull RefactorMethodSignatureParser.FieldDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#constantDeclarator}.
	 * @param ctx the parse tree
	 */
	void enterConstantDeclarator(@NotNull RefactorMethodSignatureParser.ConstantDeclaratorContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#constantDeclarator}.
	 * @param ctx the parse tree
	 */
	void exitConstantDeclarator(@NotNull RefactorMethodSignatureParser.ConstantDeclaratorContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#resources}.
	 * @param ctx the parse tree
	 */
	void enterResources(@NotNull RefactorMethodSignatureParser.ResourcesContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#resources}.
	 * @param ctx the parse tree
	 */
	void exitResources(@NotNull RefactorMethodSignatureParser.ResourcesContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#statementExpression}.
	 * @param ctx the parse tree
	 */
	void enterStatementExpression(@NotNull RefactorMethodSignatureParser.StatementExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#statementExpression}.
	 * @param ctx the parse tree
	 */
	void exitStatementExpression(@NotNull RefactorMethodSignatureParser.StatementExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#interfaceMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceMethodDeclaration(@NotNull RefactorMethodSignatureParser.InterfaceMethodDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#interfaceMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceMethodDeclaration(@NotNull RefactorMethodSignatureParser.InterfaceMethodDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#simpleNamePattern}.
	 * @param ctx the parse tree
	 */
	void enterSimpleNamePattern(@NotNull RefactorMethodSignatureParser.SimpleNamePatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#simpleNamePattern}.
	 * @param ctx the parse tree
	 */
	void exitSimpleNamePattern(@NotNull RefactorMethodSignatureParser.SimpleNamePatternContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#packageDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterPackageDeclaration(@NotNull RefactorMethodSignatureParser.PackageDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#packageDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitPackageDeclaration(@NotNull RefactorMethodSignatureParser.PackageDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#elementValuePairs}.
	 * @param ctx the parse tree
	 */
	void enterElementValuePairs(@NotNull RefactorMethodSignatureParser.ElementValuePairsContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#elementValuePairs}.
	 * @param ctx the parse tree
	 */
	void exitElementValuePairs(@NotNull RefactorMethodSignatureParser.ElementValuePairsContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#localVariableDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterLocalVariableDeclaration(@NotNull RefactorMethodSignatureParser.LocalVariableDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#localVariableDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitLocalVariableDeclaration(@NotNull RefactorMethodSignatureParser.LocalVariableDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#nonWildcardTypeArguments}.
	 * @param ctx the parse tree
	 */
	void enterNonWildcardTypeArguments(@NotNull RefactorMethodSignatureParser.NonWildcardTypeArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#nonWildcardTypeArguments}.
	 * @param ctx the parse tree
	 */
	void exitNonWildcardTypeArguments(@NotNull RefactorMethodSignatureParser.NonWildcardTypeArgumentsContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#interfaceMemberDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceMemberDeclaration(@NotNull RefactorMethodSignatureParser.InterfaceMemberDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#interfaceMemberDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceMemberDeclaration(@NotNull RefactorMethodSignatureParser.InterfaceMemberDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#switchLabel}.
	 * @param ctx the parse tree
	 */
	void enterSwitchLabel(@NotNull RefactorMethodSignatureParser.SwitchLabelContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#switchLabel}.
	 * @param ctx the parse tree
	 */
	void exitSwitchLabel(@NotNull RefactorMethodSignatureParser.SwitchLabelContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#forInit}.
	 * @param ctx the parse tree
	 */
	void enterForInit(@NotNull RefactorMethodSignatureParser.ForInitContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#forInit}.
	 * @param ctx the parse tree
	 */
	void exitForInit(@NotNull RefactorMethodSignatureParser.ForInitContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#formalParameters}.
	 * @param ctx the parse tree
	 */
	void enterFormalParameters(@NotNull RefactorMethodSignatureParser.FormalParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#formalParameters}.
	 * @param ctx the parse tree
	 */
	void exitFormalParameters(@NotNull RefactorMethodSignatureParser.FormalParametersContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#arguments}.
	 * @param ctx the parse tree
	 */
	void enterArguments(@NotNull RefactorMethodSignatureParser.ArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#arguments}.
	 * @param ctx the parse tree
	 */
	void exitArguments(@NotNull RefactorMethodSignatureParser.ArgumentsContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#genericMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterGenericMethodDeclaration(@NotNull RefactorMethodSignatureParser.GenericMethodDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#genericMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitGenericMethodDeclaration(@NotNull RefactorMethodSignatureParser.GenericMethodDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#typeArgumentsOrDiamond}.
	 * @param ctx the parse tree
	 */
	void enterTypeArgumentsOrDiamond(@NotNull RefactorMethodSignatureParser.TypeArgumentsOrDiamondContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#typeArgumentsOrDiamond}.
	 * @param ctx the parse tree
	 */
	void exitTypeArgumentsOrDiamond(@NotNull RefactorMethodSignatureParser.TypeArgumentsOrDiamondContext ctx);
}