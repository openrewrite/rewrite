/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Generated from /Users/ben/src/stash.netflix.com/dev/java-source-refactor/src/main/antlr4/AnnotationSignatureParser.g4 by ANTLR 4.2.2
package com.netflix.rewrite.aspectj;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link AnnotationSignatureParser}.
 */
public interface AnnotationSignatureParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#memberDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterMemberDeclaration(@NotNull AnnotationSignatureParser.MemberDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#memberDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitMemberDeclaration(@NotNull AnnotationSignatureParser.MemberDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#defaultValue}.
	 * @param ctx the parse tree
	 */
	void enterDefaultValue(@NotNull AnnotationSignatureParser.DefaultValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#defaultValue}.
	 * @param ctx the parse tree
	 */
	void exitDefaultValue(@NotNull AnnotationSignatureParser.DefaultValueContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#annotationTypeElementDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationTypeElementDeclaration(@NotNull AnnotationSignatureParser.AnnotationTypeElementDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#annotationTypeElementDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationTypeElementDeclaration(@NotNull AnnotationSignatureParser.AnnotationTypeElementDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(@NotNull AnnotationSignatureParser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(@NotNull AnnotationSignatureParser.TypeContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#annotationTypeBody}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationTypeBody(@NotNull AnnotationSignatureParser.AnnotationTypeBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#annotationTypeBody}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationTypeBody(@NotNull AnnotationSignatureParser.AnnotationTypeBodyContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#genericInterfaceMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterGenericInterfaceMethodDeclaration(@NotNull AnnotationSignatureParser.GenericInterfaceMethodDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#genericInterfaceMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitGenericInterfaceMethodDeclaration(@NotNull AnnotationSignatureParser.GenericInterfaceMethodDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#classBodyDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterClassBodyDeclaration(@NotNull AnnotationSignatureParser.ClassBodyDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#classBodyDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitClassBodyDeclaration(@NotNull AnnotationSignatureParser.ClassBodyDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#block}.
	 * @param ctx the parse tree
	 */
	void enterBlock(@NotNull AnnotationSignatureParser.BlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#block}.
	 * @param ctx the parse tree
	 */
	void exitBlock(@NotNull AnnotationSignatureParser.BlockContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#enumBodyDeclarations}.
	 * @param ctx the parse tree
	 */
	void enterEnumBodyDeclarations(@NotNull AnnotationSignatureParser.EnumBodyDeclarationsContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#enumBodyDeclarations}.
	 * @param ctx the parse tree
	 */
	void exitEnumBodyDeclarations(@NotNull AnnotationSignatureParser.EnumBodyDeclarationsContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#forUpdate}.
	 * @param ctx the parse tree
	 */
	void enterForUpdate(@NotNull AnnotationSignatureParser.ForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#forUpdate}.
	 * @param ctx the parse tree
	 */
	void exitForUpdate(@NotNull AnnotationSignatureParser.ForUpdateContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#enhancedForControl}.
	 * @param ctx the parse tree
	 */
	void enterEnhancedForControl(@NotNull AnnotationSignatureParser.EnhancedForControlContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#enhancedForControl}.
	 * @param ctx the parse tree
	 */
	void exitEnhancedForControl(@NotNull AnnotationSignatureParser.EnhancedForControlContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#annotationConstantRest}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationConstantRest(@NotNull AnnotationSignatureParser.AnnotationConstantRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#annotationConstantRest}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationConstantRest(@NotNull AnnotationSignatureParser.AnnotationConstantRestContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#explicitGenericInvocation}.
	 * @param ctx the parse tree
	 */
	void enterExplicitGenericInvocation(@NotNull AnnotationSignatureParser.ExplicitGenericInvocationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#explicitGenericInvocation}.
	 * @param ctx the parse tree
	 */
	void exitExplicitGenericInvocation(@NotNull AnnotationSignatureParser.ExplicitGenericInvocationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#nonWildcardTypeArgumentsOrDiamond}.
	 * @param ctx the parse tree
	 */
	void enterNonWildcardTypeArgumentsOrDiamond(@NotNull AnnotationSignatureParser.NonWildcardTypeArgumentsOrDiamondContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#nonWildcardTypeArgumentsOrDiamond}.
	 * @param ctx the parse tree
	 */
	void exitNonWildcardTypeArgumentsOrDiamond(@NotNull AnnotationSignatureParser.NonWildcardTypeArgumentsOrDiamondContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#expressionList}.
	 * @param ctx the parse tree
	 */
	void enterExpressionList(@NotNull AnnotationSignatureParser.ExpressionListContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#expressionList}.
	 * @param ctx the parse tree
	 */
	void exitExpressionList(@NotNull AnnotationSignatureParser.ExpressionListContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#annotationTypeElementRest}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationTypeElementRest(@NotNull AnnotationSignatureParser.AnnotationTypeElementRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#annotationTypeElementRest}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationTypeElementRest(@NotNull AnnotationSignatureParser.AnnotationTypeElementRestContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#classOrInterfaceType}.
	 * @param ctx the parse tree
	 */
	void enterClassOrInterfaceType(@NotNull AnnotationSignatureParser.ClassOrInterfaceTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#classOrInterfaceType}.
	 * @param ctx the parse tree
	 */
	void exitClassOrInterfaceType(@NotNull AnnotationSignatureParser.ClassOrInterfaceTypeContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#typeBound}.
	 * @param ctx the parse tree
	 */
	void enterTypeBound(@NotNull AnnotationSignatureParser.TypeBoundContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#typeBound}.
	 * @param ctx the parse tree
	 */
	void exitTypeBound(@NotNull AnnotationSignatureParser.TypeBoundContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#variableDeclaratorId}.
	 * @param ctx the parse tree
	 */
	void enterVariableDeclaratorId(@NotNull AnnotationSignatureParser.VariableDeclaratorIdContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#variableDeclaratorId}.
	 * @param ctx the parse tree
	 */
	void exitVariableDeclaratorId(@NotNull AnnotationSignatureParser.VariableDeclaratorIdContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#primary}.
	 * @param ctx the parse tree
	 */
	void enterPrimary(@NotNull AnnotationSignatureParser.PrimaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#primary}.
	 * @param ctx the parse tree
	 */
	void exitPrimary(@NotNull AnnotationSignatureParser.PrimaryContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#classCreatorRest}.
	 * @param ctx the parse tree
	 */
	void enterClassCreatorRest(@NotNull AnnotationSignatureParser.ClassCreatorRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#classCreatorRest}.
	 * @param ctx the parse tree
	 */
	void exitClassCreatorRest(@NotNull AnnotationSignatureParser.ClassCreatorRestContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#interfaceBodyDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceBodyDeclaration(@NotNull AnnotationSignatureParser.InterfaceBodyDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#interfaceBodyDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceBodyDeclaration(@NotNull AnnotationSignatureParser.InterfaceBodyDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#typeArguments}.
	 * @param ctx the parse tree
	 */
	void enterTypeArguments(@NotNull AnnotationSignatureParser.TypeArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#typeArguments}.
	 * @param ctx the parse tree
	 */
	void exitTypeArguments(@NotNull AnnotationSignatureParser.TypeArgumentsContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#annotationName}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationName(@NotNull AnnotationSignatureParser.AnnotationNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#annotationName}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationName(@NotNull AnnotationSignatureParser.AnnotationNameContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#finallyBlock}.
	 * @param ctx the parse tree
	 */
	void enterFinallyBlock(@NotNull AnnotationSignatureParser.FinallyBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#finallyBlock}.
	 * @param ctx the parse tree
	 */
	void exitFinallyBlock(@NotNull AnnotationSignatureParser.FinallyBlockContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#typeParameters}.
	 * @param ctx the parse tree
	 */
	void enterTypeParameters(@NotNull AnnotationSignatureParser.TypeParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#typeParameters}.
	 * @param ctx the parse tree
	 */
	void exitTypeParameters(@NotNull AnnotationSignatureParser.TypeParametersContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#lastFormalParameter}.
	 * @param ctx the parse tree
	 */
	void enterLastFormalParameter(@NotNull AnnotationSignatureParser.LastFormalParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#lastFormalParameter}.
	 * @param ctx the parse tree
	 */
	void exitLastFormalParameter(@NotNull AnnotationSignatureParser.LastFormalParameterContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#constructorBody}.
	 * @param ctx the parse tree
	 */
	void enterConstructorBody(@NotNull AnnotationSignatureParser.ConstructorBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#constructorBody}.
	 * @param ctx the parse tree
	 */
	void exitConstructorBody(@NotNull AnnotationSignatureParser.ConstructorBodyContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLiteral(@NotNull AnnotationSignatureParser.LiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLiteral(@NotNull AnnotationSignatureParser.LiteralContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#annotationMethodOrConstantRest}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationMethodOrConstantRest(@NotNull AnnotationSignatureParser.AnnotationMethodOrConstantRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#annotationMethodOrConstantRest}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationMethodOrConstantRest(@NotNull AnnotationSignatureParser.AnnotationMethodOrConstantRestContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#catchClause}.
	 * @param ctx the parse tree
	 */
	void enterCatchClause(@NotNull AnnotationSignatureParser.CatchClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#catchClause}.
	 * @param ctx the parse tree
	 */
	void exitCatchClause(@NotNull AnnotationSignatureParser.CatchClauseContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#variableDeclarator}.
	 * @param ctx the parse tree
	 */
	void enterVariableDeclarator(@NotNull AnnotationSignatureParser.VariableDeclaratorContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#variableDeclarator}.
	 * @param ctx the parse tree
	 */
	void exitVariableDeclarator(@NotNull AnnotationSignatureParser.VariableDeclaratorContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#typeList}.
	 * @param ctx the parse tree
	 */
	void enterTypeList(@NotNull AnnotationSignatureParser.TypeListContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#typeList}.
	 * @param ctx the parse tree
	 */
	void exitTypeList(@NotNull AnnotationSignatureParser.TypeListContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#enumConstants}.
	 * @param ctx the parse tree
	 */
	void enterEnumConstants(@NotNull AnnotationSignatureParser.EnumConstantsContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#enumConstants}.
	 * @param ctx the parse tree
	 */
	void exitEnumConstants(@NotNull AnnotationSignatureParser.EnumConstantsContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#classBody}.
	 * @param ctx the parse tree
	 */
	void enterClassBody(@NotNull AnnotationSignatureParser.ClassBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#classBody}.
	 * @param ctx the parse tree
	 */
	void exitClassBody(@NotNull AnnotationSignatureParser.ClassBodyContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#createdName}.
	 * @param ctx the parse tree
	 */
	void enterCreatedName(@NotNull AnnotationSignatureParser.CreatedNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#createdName}.
	 * @param ctx the parse tree
	 */
	void exitCreatedName(@NotNull AnnotationSignatureParser.CreatedNameContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#enumDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterEnumDeclaration(@NotNull AnnotationSignatureParser.EnumDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#enumDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitEnumDeclaration(@NotNull AnnotationSignatureParser.EnumDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#formalParameter}.
	 * @param ctx the parse tree
	 */
	void enterFormalParameter(@NotNull AnnotationSignatureParser.FormalParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#formalParameter}.
	 * @param ctx the parse tree
	 */
	void exitFormalParameter(@NotNull AnnotationSignatureParser.FormalParameterContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#parExpression}.
	 * @param ctx the parse tree
	 */
	void enterParExpression(@NotNull AnnotationSignatureParser.ParExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#parExpression}.
	 * @param ctx the parse tree
	 */
	void exitParExpression(@NotNull AnnotationSignatureParser.ParExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#annotation}.
	 * @param ctx the parse tree
	 */
	void enterAnnotation(@NotNull AnnotationSignatureParser.AnnotationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#annotation}.
	 * @param ctx the parse tree
	 */
	void exitAnnotation(@NotNull AnnotationSignatureParser.AnnotationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#variableInitializer}.
	 * @param ctx the parse tree
	 */
	void enterVariableInitializer(@NotNull AnnotationSignatureParser.VariableInitializerContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#variableInitializer}.
	 * @param ctx the parse tree
	 */
	void exitVariableInitializer(@NotNull AnnotationSignatureParser.VariableInitializerContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#elementValueArrayInitializer}.
	 * @param ctx the parse tree
	 */
	void enterElementValueArrayInitializer(@NotNull AnnotationSignatureParser.ElementValueArrayInitializerContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#elementValueArrayInitializer}.
	 * @param ctx the parse tree
	 */
	void exitElementValueArrayInitializer(@NotNull AnnotationSignatureParser.ElementValueArrayInitializerContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#creator}.
	 * @param ctx the parse tree
	 */
	void enterCreator(@NotNull AnnotationSignatureParser.CreatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#creator}.
	 * @param ctx the parse tree
	 */
	void exitCreator(@NotNull AnnotationSignatureParser.CreatorContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#arrayCreatorRest}.
	 * @param ctx the parse tree
	 */
	void enterArrayCreatorRest(@NotNull AnnotationSignatureParser.ArrayCreatorRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#arrayCreatorRest}.
	 * @param ctx the parse tree
	 */
	void exitArrayCreatorRest(@NotNull AnnotationSignatureParser.ArrayCreatorRestContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(@NotNull AnnotationSignatureParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(@NotNull AnnotationSignatureParser.ExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#constantExpression}.
	 * @param ctx the parse tree
	 */
	void enterConstantExpression(@NotNull AnnotationSignatureParser.ConstantExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#constantExpression}.
	 * @param ctx the parse tree
	 */
	void exitConstantExpression(@NotNull AnnotationSignatureParser.ConstantExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#qualifiedNameList}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedNameList(@NotNull AnnotationSignatureParser.QualifiedNameListContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#qualifiedNameList}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedNameList(@NotNull AnnotationSignatureParser.QualifiedNameListContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#constructorDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterConstructorDeclaration(@NotNull AnnotationSignatureParser.ConstructorDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#constructorDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitConstructorDeclaration(@NotNull AnnotationSignatureParser.ConstructorDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#forControl}.
	 * @param ctx the parse tree
	 */
	void enterForControl(@NotNull AnnotationSignatureParser.ForControlContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#forControl}.
	 * @param ctx the parse tree
	 */
	void exitForControl(@NotNull AnnotationSignatureParser.ForControlContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#superSuffix}.
	 * @param ctx the parse tree
	 */
	void enterSuperSuffix(@NotNull AnnotationSignatureParser.SuperSuffixContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#superSuffix}.
	 * @param ctx the parse tree
	 */
	void exitSuperSuffix(@NotNull AnnotationSignatureParser.SuperSuffixContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#variableDeclarators}.
	 * @param ctx the parse tree
	 */
	void enterVariableDeclarators(@NotNull AnnotationSignatureParser.VariableDeclaratorsContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#variableDeclarators}.
	 * @param ctx the parse tree
	 */
	void exitVariableDeclarators(@NotNull AnnotationSignatureParser.VariableDeclaratorsContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#catchType}.
	 * @param ctx the parse tree
	 */
	void enterCatchType(@NotNull AnnotationSignatureParser.CatchTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#catchType}.
	 * @param ctx the parse tree
	 */
	void exitCatchType(@NotNull AnnotationSignatureParser.CatchTypeContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#classOrInterfaceModifier}.
	 * @param ctx the parse tree
	 */
	void enterClassOrInterfaceModifier(@NotNull AnnotationSignatureParser.ClassOrInterfaceModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#classOrInterfaceModifier}.
	 * @param ctx the parse tree
	 */
	void exitClassOrInterfaceModifier(@NotNull AnnotationSignatureParser.ClassOrInterfaceModifierContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#enumConstantName}.
	 * @param ctx the parse tree
	 */
	void enterEnumConstantName(@NotNull AnnotationSignatureParser.EnumConstantNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#enumConstantName}.
	 * @param ctx the parse tree
	 */
	void exitEnumConstantName(@NotNull AnnotationSignatureParser.EnumConstantNameContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#modifier}.
	 * @param ctx the parse tree
	 */
	void enterModifier(@NotNull AnnotationSignatureParser.ModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#modifier}.
	 * @param ctx the parse tree
	 */
	void exitModifier(@NotNull AnnotationSignatureParser.ModifierContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#innerCreator}.
	 * @param ctx the parse tree
	 */
	void enterInnerCreator(@NotNull AnnotationSignatureParser.InnerCreatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#innerCreator}.
	 * @param ctx the parse tree
	 */
	void exitInnerCreator(@NotNull AnnotationSignatureParser.InnerCreatorContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#explicitGenericInvocationSuffix}.
	 * @param ctx the parse tree
	 */
	void enterExplicitGenericInvocationSuffix(@NotNull AnnotationSignatureParser.ExplicitGenericInvocationSuffixContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#explicitGenericInvocationSuffix}.
	 * @param ctx the parse tree
	 */
	void exitExplicitGenericInvocationSuffix(@NotNull AnnotationSignatureParser.ExplicitGenericInvocationSuffixContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#variableModifier}.
	 * @param ctx the parse tree
	 */
	void enterVariableModifier(@NotNull AnnotationSignatureParser.VariableModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#variableModifier}.
	 * @param ctx the parse tree
	 */
	void exitVariableModifier(@NotNull AnnotationSignatureParser.VariableModifierContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#elementValuePair}.
	 * @param ctx the parse tree
	 */
	void enterElementValuePair(@NotNull AnnotationSignatureParser.ElementValuePairContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#elementValuePair}.
	 * @param ctx the parse tree
	 */
	void exitElementValuePair(@NotNull AnnotationSignatureParser.ElementValuePairContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#arrayInitializer}.
	 * @param ctx the parse tree
	 */
	void enterArrayInitializer(@NotNull AnnotationSignatureParser.ArrayInitializerContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#arrayInitializer}.
	 * @param ctx the parse tree
	 */
	void exitArrayInitializer(@NotNull AnnotationSignatureParser.ArrayInitializerContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#elementValue}.
	 * @param ctx the parse tree
	 */
	void enterElementValue(@NotNull AnnotationSignatureParser.ElementValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#elementValue}.
	 * @param ctx the parse tree
	 */
	void exitElementValue(@NotNull AnnotationSignatureParser.ElementValueContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#constDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterConstDeclaration(@NotNull AnnotationSignatureParser.ConstDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#constDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitConstDeclaration(@NotNull AnnotationSignatureParser.ConstDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#resource}.
	 * @param ctx the parse tree
	 */
	void enterResource(@NotNull AnnotationSignatureParser.ResourceContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#resource}.
	 * @param ctx the parse tree
	 */
	void exitResource(@NotNull AnnotationSignatureParser.ResourceContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedName(@NotNull AnnotationSignatureParser.QualifiedNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedName(@NotNull AnnotationSignatureParser.QualifiedNameContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#resourceSpecification}.
	 * @param ctx the parse tree
	 */
	void enterResourceSpecification(@NotNull AnnotationSignatureParser.ResourceSpecificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#resourceSpecification}.
	 * @param ctx the parse tree
	 */
	void exitResourceSpecification(@NotNull AnnotationSignatureParser.ResourceSpecificationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#formalParameterList}.
	 * @param ctx the parse tree
	 */
	void enterFormalParameterList(@NotNull AnnotationSignatureParser.FormalParameterListContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#formalParameterList}.
	 * @param ctx the parse tree
	 */
	void exitFormalParameterList(@NotNull AnnotationSignatureParser.FormalParameterListContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#annotationTypeDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationTypeDeclaration(@NotNull AnnotationSignatureParser.AnnotationTypeDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#annotationTypeDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationTypeDeclaration(@NotNull AnnotationSignatureParser.AnnotationTypeDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#compilationUnit}.
	 * @param ctx the parse tree
	 */
	void enterCompilationUnit(@NotNull AnnotationSignatureParser.CompilationUnitContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#compilationUnit}.
	 * @param ctx the parse tree
	 */
	void exitCompilationUnit(@NotNull AnnotationSignatureParser.CompilationUnitContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#annotationMethodRest}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationMethodRest(@NotNull AnnotationSignatureParser.AnnotationMethodRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#annotationMethodRest}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationMethodRest(@NotNull AnnotationSignatureParser.AnnotationMethodRestContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#switchBlockStatementGroup}.
	 * @param ctx the parse tree
	 */
	void enterSwitchBlockStatementGroup(@NotNull AnnotationSignatureParser.SwitchBlockStatementGroupContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#switchBlockStatementGroup}.
	 * @param ctx the parse tree
	 */
	void exitSwitchBlockStatementGroup(@NotNull AnnotationSignatureParser.SwitchBlockStatementGroupContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#typeParameter}.
	 * @param ctx the parse tree
	 */
	void enterTypeParameter(@NotNull AnnotationSignatureParser.TypeParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#typeParameter}.
	 * @param ctx the parse tree
	 */
	void exitTypeParameter(@NotNull AnnotationSignatureParser.TypeParameterContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#interfaceBody}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceBody(@NotNull AnnotationSignatureParser.InterfaceBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#interfaceBody}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceBody(@NotNull AnnotationSignatureParser.InterfaceBodyContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#methodDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterMethodDeclaration(@NotNull AnnotationSignatureParser.MethodDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#methodDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitMethodDeclaration(@NotNull AnnotationSignatureParser.MethodDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#methodBody}.
	 * @param ctx the parse tree
	 */
	void enterMethodBody(@NotNull AnnotationSignatureParser.MethodBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#methodBody}.
	 * @param ctx the parse tree
	 */
	void exitMethodBody(@NotNull AnnotationSignatureParser.MethodBodyContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#typeArgument}.
	 * @param ctx the parse tree
	 */
	void enterTypeArgument(@NotNull AnnotationSignatureParser.TypeArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#typeArgument}.
	 * @param ctx the parse tree
	 */
	void exitTypeArgument(@NotNull AnnotationSignatureParser.TypeArgumentContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#typeDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterTypeDeclaration(@NotNull AnnotationSignatureParser.TypeDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#typeDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitTypeDeclaration(@NotNull AnnotationSignatureParser.TypeDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#genericConstructorDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterGenericConstructorDeclaration(@NotNull AnnotationSignatureParser.GenericConstructorDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#genericConstructorDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitGenericConstructorDeclaration(@NotNull AnnotationSignatureParser.GenericConstructorDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#classDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterClassDeclaration(@NotNull AnnotationSignatureParser.ClassDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#classDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitClassDeclaration(@NotNull AnnotationSignatureParser.ClassDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#enumConstant}.
	 * @param ctx the parse tree
	 */
	void enterEnumConstant(@NotNull AnnotationSignatureParser.EnumConstantContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#enumConstant}.
	 * @param ctx the parse tree
	 */
	void exitEnumConstant(@NotNull AnnotationSignatureParser.EnumConstantContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(@NotNull AnnotationSignatureParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(@NotNull AnnotationSignatureParser.StatementContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#importDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterImportDeclaration(@NotNull AnnotationSignatureParser.ImportDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#importDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitImportDeclaration(@NotNull AnnotationSignatureParser.ImportDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#primitiveType}.
	 * @param ctx the parse tree
	 */
	void enterPrimitiveType(@NotNull AnnotationSignatureParser.PrimitiveTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#primitiveType}.
	 * @param ctx the parse tree
	 */
	void exitPrimitiveType(@NotNull AnnotationSignatureParser.PrimitiveTypeContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#interfaceDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceDeclaration(@NotNull AnnotationSignatureParser.InterfaceDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#interfaceDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceDeclaration(@NotNull AnnotationSignatureParser.InterfaceDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#localVariableDeclarationStatement}.
	 * @param ctx the parse tree
	 */
	void enterLocalVariableDeclarationStatement(@NotNull AnnotationSignatureParser.LocalVariableDeclarationStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#localVariableDeclarationStatement}.
	 * @param ctx the parse tree
	 */
	void exitLocalVariableDeclarationStatement(@NotNull AnnotationSignatureParser.LocalVariableDeclarationStatementContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#blockStatement}.
	 * @param ctx the parse tree
	 */
	void enterBlockStatement(@NotNull AnnotationSignatureParser.BlockStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#blockStatement}.
	 * @param ctx the parse tree
	 */
	void exitBlockStatement(@NotNull AnnotationSignatureParser.BlockStatementContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#fieldDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterFieldDeclaration(@NotNull AnnotationSignatureParser.FieldDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#fieldDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitFieldDeclaration(@NotNull AnnotationSignatureParser.FieldDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#constantDeclarator}.
	 * @param ctx the parse tree
	 */
	void enterConstantDeclarator(@NotNull AnnotationSignatureParser.ConstantDeclaratorContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#constantDeclarator}.
	 * @param ctx the parse tree
	 */
	void exitConstantDeclarator(@NotNull AnnotationSignatureParser.ConstantDeclaratorContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#resources}.
	 * @param ctx the parse tree
	 */
	void enterResources(@NotNull AnnotationSignatureParser.ResourcesContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#resources}.
	 * @param ctx the parse tree
	 */
	void exitResources(@NotNull AnnotationSignatureParser.ResourcesContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#statementExpression}.
	 * @param ctx the parse tree
	 */
	void enterStatementExpression(@NotNull AnnotationSignatureParser.StatementExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#statementExpression}.
	 * @param ctx the parse tree
	 */
	void exitStatementExpression(@NotNull AnnotationSignatureParser.StatementExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#interfaceMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceMethodDeclaration(@NotNull AnnotationSignatureParser.InterfaceMethodDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#interfaceMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceMethodDeclaration(@NotNull AnnotationSignatureParser.InterfaceMethodDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#packageDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterPackageDeclaration(@NotNull AnnotationSignatureParser.PackageDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#packageDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitPackageDeclaration(@NotNull AnnotationSignatureParser.PackageDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#elementValuePairs}.
	 * @param ctx the parse tree
	 */
	void enterElementValuePairs(@NotNull AnnotationSignatureParser.ElementValuePairsContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#elementValuePairs}.
	 * @param ctx the parse tree
	 */
	void exitElementValuePairs(@NotNull AnnotationSignatureParser.ElementValuePairsContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#localVariableDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterLocalVariableDeclaration(@NotNull AnnotationSignatureParser.LocalVariableDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#localVariableDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitLocalVariableDeclaration(@NotNull AnnotationSignatureParser.LocalVariableDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#nonWildcardTypeArguments}.
	 * @param ctx the parse tree
	 */
	void enterNonWildcardTypeArguments(@NotNull AnnotationSignatureParser.NonWildcardTypeArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#nonWildcardTypeArguments}.
	 * @param ctx the parse tree
	 */
	void exitNonWildcardTypeArguments(@NotNull AnnotationSignatureParser.NonWildcardTypeArgumentsContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#interfaceMemberDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceMemberDeclaration(@NotNull AnnotationSignatureParser.InterfaceMemberDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#interfaceMemberDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceMemberDeclaration(@NotNull AnnotationSignatureParser.InterfaceMemberDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#switchLabel}.
	 * @param ctx the parse tree
	 */
	void enterSwitchLabel(@NotNull AnnotationSignatureParser.SwitchLabelContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#switchLabel}.
	 * @param ctx the parse tree
	 */
	void exitSwitchLabel(@NotNull AnnotationSignatureParser.SwitchLabelContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#forInit}.
	 * @param ctx the parse tree
	 */
	void enterForInit(@NotNull AnnotationSignatureParser.ForInitContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#forInit}.
	 * @param ctx the parse tree
	 */
	void exitForInit(@NotNull AnnotationSignatureParser.ForInitContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#formalParameters}.
	 * @param ctx the parse tree
	 */
	void enterFormalParameters(@NotNull AnnotationSignatureParser.FormalParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#formalParameters}.
	 * @param ctx the parse tree
	 */
	void exitFormalParameters(@NotNull AnnotationSignatureParser.FormalParametersContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#arguments}.
	 * @param ctx the parse tree
	 */
	void enterArguments(@NotNull AnnotationSignatureParser.ArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#arguments}.
	 * @param ctx the parse tree
	 */
	void exitArguments(@NotNull AnnotationSignatureParser.ArgumentsContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#genericMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterGenericMethodDeclaration(@NotNull AnnotationSignatureParser.GenericMethodDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#genericMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitGenericMethodDeclaration(@NotNull AnnotationSignatureParser.GenericMethodDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#typeArgumentsOrDiamond}.
	 * @param ctx the parse tree
	 */
	void enterTypeArgumentsOrDiamond(@NotNull AnnotationSignatureParser.TypeArgumentsOrDiamondContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#typeArgumentsOrDiamond}.
	 * @param ctx the parse tree
	 */
	void exitTypeArgumentsOrDiamond(@NotNull AnnotationSignatureParser.TypeArgumentsOrDiamondContext ctx);
}