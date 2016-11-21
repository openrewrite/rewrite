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
// Generated from /Users/joschneider/Projects/github/jkschneider/java-source-refactor/src/main/antlr4/JavaParser.g4 by ANTLR 4.2.2
package com.netflix.java.refactor.aspectj;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link JavaParser}.
 */
public interface JavaParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link JavaParser#memberDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterMemberDeclaration(@NotNull JavaParser.MemberDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#memberDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitMemberDeclaration(@NotNull JavaParser.MemberDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#defaultValue}.
	 * @param ctx the parse tree
	 */
	void enterDefaultValue(@NotNull JavaParser.DefaultValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#defaultValue}.
	 * @param ctx the parse tree
	 */
	void exitDefaultValue(@NotNull JavaParser.DefaultValueContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#annotationTypeElementDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationTypeElementDeclaration(@NotNull JavaParser.AnnotationTypeElementDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#annotationTypeElementDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationTypeElementDeclaration(@NotNull JavaParser.AnnotationTypeElementDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(@NotNull JavaParser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(@NotNull JavaParser.TypeContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#annotationTypeBody}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationTypeBody(@NotNull JavaParser.AnnotationTypeBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#annotationTypeBody}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationTypeBody(@NotNull JavaParser.AnnotationTypeBodyContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#genericInterfaceMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterGenericInterfaceMethodDeclaration(@NotNull JavaParser.GenericInterfaceMethodDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#genericInterfaceMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitGenericInterfaceMethodDeclaration(@NotNull JavaParser.GenericInterfaceMethodDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#classBodyDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterClassBodyDeclaration(@NotNull JavaParser.ClassBodyDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#classBodyDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitClassBodyDeclaration(@NotNull JavaParser.ClassBodyDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#block}.
	 * @param ctx the parse tree
	 */
	void enterBlock(@NotNull JavaParser.BlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#block}.
	 * @param ctx the parse tree
	 */
	void exitBlock(@NotNull JavaParser.BlockContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#enumBodyDeclarations}.
	 * @param ctx the parse tree
	 */
	void enterEnumBodyDeclarations(@NotNull JavaParser.EnumBodyDeclarationsContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#enumBodyDeclarations}.
	 * @param ctx the parse tree
	 */
	void exitEnumBodyDeclarations(@NotNull JavaParser.EnumBodyDeclarationsContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#forUpdate}.
	 * @param ctx the parse tree
	 */
	void enterForUpdate(@NotNull JavaParser.ForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#forUpdate}.
	 * @param ctx the parse tree
	 */
	void exitForUpdate(@NotNull JavaParser.ForUpdateContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#enhancedForControl}.
	 * @param ctx the parse tree
	 */
	void enterEnhancedForControl(@NotNull JavaParser.EnhancedForControlContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#enhancedForControl}.
	 * @param ctx the parse tree
	 */
	void exitEnhancedForControl(@NotNull JavaParser.EnhancedForControlContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#annotationConstantRest}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationConstantRest(@NotNull JavaParser.AnnotationConstantRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#annotationConstantRest}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationConstantRest(@NotNull JavaParser.AnnotationConstantRestContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#explicitGenericInvocation}.
	 * @param ctx the parse tree
	 */
	void enterExplicitGenericInvocation(@NotNull JavaParser.ExplicitGenericInvocationContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#explicitGenericInvocation}.
	 * @param ctx the parse tree
	 */
	void exitExplicitGenericInvocation(@NotNull JavaParser.ExplicitGenericInvocationContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#nonWildcardTypeArgumentsOrDiamond}.
	 * @param ctx the parse tree
	 */
	void enterNonWildcardTypeArgumentsOrDiamond(@NotNull JavaParser.NonWildcardTypeArgumentsOrDiamondContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#nonWildcardTypeArgumentsOrDiamond}.
	 * @param ctx the parse tree
	 */
	void exitNonWildcardTypeArgumentsOrDiamond(@NotNull JavaParser.NonWildcardTypeArgumentsOrDiamondContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#expressionList}.
	 * @param ctx the parse tree
	 */
	void enterExpressionList(@NotNull JavaParser.ExpressionListContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#expressionList}.
	 * @param ctx the parse tree
	 */
	void exitExpressionList(@NotNull JavaParser.ExpressionListContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#annotationTypeElementRest}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationTypeElementRest(@NotNull JavaParser.AnnotationTypeElementRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#annotationTypeElementRest}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationTypeElementRest(@NotNull JavaParser.AnnotationTypeElementRestContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#classOrInterfaceType}.
	 * @param ctx the parse tree
	 */
	void enterClassOrInterfaceType(@NotNull JavaParser.ClassOrInterfaceTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#classOrInterfaceType}.
	 * @param ctx the parse tree
	 */
	void exitClassOrInterfaceType(@NotNull JavaParser.ClassOrInterfaceTypeContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#typeBound}.
	 * @param ctx the parse tree
	 */
	void enterTypeBound(@NotNull JavaParser.TypeBoundContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#typeBound}.
	 * @param ctx the parse tree
	 */
	void exitTypeBound(@NotNull JavaParser.TypeBoundContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#variableDeclaratorId}.
	 * @param ctx the parse tree
	 */
	void enterVariableDeclaratorId(@NotNull JavaParser.VariableDeclaratorIdContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#variableDeclaratorId}.
	 * @param ctx the parse tree
	 */
	void exitVariableDeclaratorId(@NotNull JavaParser.VariableDeclaratorIdContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#primary}.
	 * @param ctx the parse tree
	 */
	void enterPrimary(@NotNull JavaParser.PrimaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#primary}.
	 * @param ctx the parse tree
	 */
	void exitPrimary(@NotNull JavaParser.PrimaryContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#classCreatorRest}.
	 * @param ctx the parse tree
	 */
	void enterClassCreatorRest(@NotNull JavaParser.ClassCreatorRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#classCreatorRest}.
	 * @param ctx the parse tree
	 */
	void exitClassCreatorRest(@NotNull JavaParser.ClassCreatorRestContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#interfaceBodyDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceBodyDeclaration(@NotNull JavaParser.InterfaceBodyDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#interfaceBodyDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceBodyDeclaration(@NotNull JavaParser.InterfaceBodyDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#typeArguments}.
	 * @param ctx the parse tree
	 */
	void enterTypeArguments(@NotNull JavaParser.TypeArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#typeArguments}.
	 * @param ctx the parse tree
	 */
	void exitTypeArguments(@NotNull JavaParser.TypeArgumentsContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#annotationName}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationName(@NotNull JavaParser.AnnotationNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#annotationName}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationName(@NotNull JavaParser.AnnotationNameContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#finallyBlock}.
	 * @param ctx the parse tree
	 */
	void enterFinallyBlock(@NotNull JavaParser.FinallyBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#finallyBlock}.
	 * @param ctx the parse tree
	 */
	void exitFinallyBlock(@NotNull JavaParser.FinallyBlockContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#typeParameters}.
	 * @param ctx the parse tree
	 */
	void enterTypeParameters(@NotNull JavaParser.TypeParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#typeParameters}.
	 * @param ctx the parse tree
	 */
	void exitTypeParameters(@NotNull JavaParser.TypeParametersContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#lastFormalParameter}.
	 * @param ctx the parse tree
	 */
	void enterLastFormalParameter(@NotNull JavaParser.LastFormalParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#lastFormalParameter}.
	 * @param ctx the parse tree
	 */
	void exitLastFormalParameter(@NotNull JavaParser.LastFormalParameterContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#constructorBody}.
	 * @param ctx the parse tree
	 */
	void enterConstructorBody(@NotNull JavaParser.ConstructorBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#constructorBody}.
	 * @param ctx the parse tree
	 */
	void exitConstructorBody(@NotNull JavaParser.ConstructorBodyContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLiteral(@NotNull JavaParser.LiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLiteral(@NotNull JavaParser.LiteralContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#annotationMethodOrConstantRest}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationMethodOrConstantRest(@NotNull JavaParser.AnnotationMethodOrConstantRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#annotationMethodOrConstantRest}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationMethodOrConstantRest(@NotNull JavaParser.AnnotationMethodOrConstantRestContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#catchClause}.
	 * @param ctx the parse tree
	 */
	void enterCatchClause(@NotNull JavaParser.CatchClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#catchClause}.
	 * @param ctx the parse tree
	 */
	void exitCatchClause(@NotNull JavaParser.CatchClauseContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#variableDeclarator}.
	 * @param ctx the parse tree
	 */
	void enterVariableDeclarator(@NotNull JavaParser.VariableDeclaratorContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#variableDeclarator}.
	 * @param ctx the parse tree
	 */
	void exitVariableDeclarator(@NotNull JavaParser.VariableDeclaratorContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#typeList}.
	 * @param ctx the parse tree
	 */
	void enterTypeList(@NotNull JavaParser.TypeListContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#typeList}.
	 * @param ctx the parse tree
	 */
	void exitTypeList(@NotNull JavaParser.TypeListContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#enumConstants}.
	 * @param ctx the parse tree
	 */
	void enterEnumConstants(@NotNull JavaParser.EnumConstantsContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#enumConstants}.
	 * @param ctx the parse tree
	 */
	void exitEnumConstants(@NotNull JavaParser.EnumConstantsContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#classBody}.
	 * @param ctx the parse tree
	 */
	void enterClassBody(@NotNull JavaParser.ClassBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#classBody}.
	 * @param ctx the parse tree
	 */
	void exitClassBody(@NotNull JavaParser.ClassBodyContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#createdName}.
	 * @param ctx the parse tree
	 */
	void enterCreatedName(@NotNull JavaParser.CreatedNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#createdName}.
	 * @param ctx the parse tree
	 */
	void exitCreatedName(@NotNull JavaParser.CreatedNameContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#enumDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterEnumDeclaration(@NotNull JavaParser.EnumDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#enumDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitEnumDeclaration(@NotNull JavaParser.EnumDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#formalParameter}.
	 * @param ctx the parse tree
	 */
	void enterFormalParameter(@NotNull JavaParser.FormalParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#formalParameter}.
	 * @param ctx the parse tree
	 */
	void exitFormalParameter(@NotNull JavaParser.FormalParameterContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#parExpression}.
	 * @param ctx the parse tree
	 */
	void enterParExpression(@NotNull JavaParser.ParExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#parExpression}.
	 * @param ctx the parse tree
	 */
	void exitParExpression(@NotNull JavaParser.ParExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#annotation}.
	 * @param ctx the parse tree
	 */
	void enterAnnotation(@NotNull JavaParser.AnnotationContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#annotation}.
	 * @param ctx the parse tree
	 */
	void exitAnnotation(@NotNull JavaParser.AnnotationContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#variableInitializer}.
	 * @param ctx the parse tree
	 */
	void enterVariableInitializer(@NotNull JavaParser.VariableInitializerContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#variableInitializer}.
	 * @param ctx the parse tree
	 */
	void exitVariableInitializer(@NotNull JavaParser.VariableInitializerContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#elementValueArrayInitializer}.
	 * @param ctx the parse tree
	 */
	void enterElementValueArrayInitializer(@NotNull JavaParser.ElementValueArrayInitializerContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#elementValueArrayInitializer}.
	 * @param ctx the parse tree
	 */
	void exitElementValueArrayInitializer(@NotNull JavaParser.ElementValueArrayInitializerContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#creator}.
	 * @param ctx the parse tree
	 */
	void enterCreator(@NotNull JavaParser.CreatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#creator}.
	 * @param ctx the parse tree
	 */
	void exitCreator(@NotNull JavaParser.CreatorContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#arrayCreatorRest}.
	 * @param ctx the parse tree
	 */
	void enterArrayCreatorRest(@NotNull JavaParser.ArrayCreatorRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#arrayCreatorRest}.
	 * @param ctx the parse tree
	 */
	void exitArrayCreatorRest(@NotNull JavaParser.ArrayCreatorRestContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(@NotNull JavaParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(@NotNull JavaParser.ExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#constantExpression}.
	 * @param ctx the parse tree
	 */
	void enterConstantExpression(@NotNull JavaParser.ConstantExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#constantExpression}.
	 * @param ctx the parse tree
	 */
	void exitConstantExpression(@NotNull JavaParser.ConstantExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#qualifiedNameList}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedNameList(@NotNull JavaParser.QualifiedNameListContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#qualifiedNameList}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedNameList(@NotNull JavaParser.QualifiedNameListContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#constructorDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterConstructorDeclaration(@NotNull JavaParser.ConstructorDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#constructorDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitConstructorDeclaration(@NotNull JavaParser.ConstructorDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#forControl}.
	 * @param ctx the parse tree
	 */
	void enterForControl(@NotNull JavaParser.ForControlContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#forControl}.
	 * @param ctx the parse tree
	 */
	void exitForControl(@NotNull JavaParser.ForControlContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#superSuffix}.
	 * @param ctx the parse tree
	 */
	void enterSuperSuffix(@NotNull JavaParser.SuperSuffixContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#superSuffix}.
	 * @param ctx the parse tree
	 */
	void exitSuperSuffix(@NotNull JavaParser.SuperSuffixContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#variableDeclarators}.
	 * @param ctx the parse tree
	 */
	void enterVariableDeclarators(@NotNull JavaParser.VariableDeclaratorsContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#variableDeclarators}.
	 * @param ctx the parse tree
	 */
	void exitVariableDeclarators(@NotNull JavaParser.VariableDeclaratorsContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#catchType}.
	 * @param ctx the parse tree
	 */
	void enterCatchType(@NotNull JavaParser.CatchTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#catchType}.
	 * @param ctx the parse tree
	 */
	void exitCatchType(@NotNull JavaParser.CatchTypeContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#classOrInterfaceModifier}.
	 * @param ctx the parse tree
	 */
	void enterClassOrInterfaceModifier(@NotNull JavaParser.ClassOrInterfaceModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#classOrInterfaceModifier}.
	 * @param ctx the parse tree
	 */
	void exitClassOrInterfaceModifier(@NotNull JavaParser.ClassOrInterfaceModifierContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#enumConstantName}.
	 * @param ctx the parse tree
	 */
	void enterEnumConstantName(@NotNull JavaParser.EnumConstantNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#enumConstantName}.
	 * @param ctx the parse tree
	 */
	void exitEnumConstantName(@NotNull JavaParser.EnumConstantNameContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#modifier}.
	 * @param ctx the parse tree
	 */
	void enterModifier(@NotNull JavaParser.ModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#modifier}.
	 * @param ctx the parse tree
	 */
	void exitModifier(@NotNull JavaParser.ModifierContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#innerCreator}.
	 * @param ctx the parse tree
	 */
	void enterInnerCreator(@NotNull JavaParser.InnerCreatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#innerCreator}.
	 * @param ctx the parse tree
	 */
	void exitInnerCreator(@NotNull JavaParser.InnerCreatorContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#explicitGenericInvocationSuffix}.
	 * @param ctx the parse tree
	 */
	void enterExplicitGenericInvocationSuffix(@NotNull JavaParser.ExplicitGenericInvocationSuffixContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#explicitGenericInvocationSuffix}.
	 * @param ctx the parse tree
	 */
	void exitExplicitGenericInvocationSuffix(@NotNull JavaParser.ExplicitGenericInvocationSuffixContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#variableModifier}.
	 * @param ctx the parse tree
	 */
	void enterVariableModifier(@NotNull JavaParser.VariableModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#variableModifier}.
	 * @param ctx the parse tree
	 */
	void exitVariableModifier(@NotNull JavaParser.VariableModifierContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#elementValuePair}.
	 * @param ctx the parse tree
	 */
	void enterElementValuePair(@NotNull JavaParser.ElementValuePairContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#elementValuePair}.
	 * @param ctx the parse tree
	 */
	void exitElementValuePair(@NotNull JavaParser.ElementValuePairContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#arrayInitializer}.
	 * @param ctx the parse tree
	 */
	void enterArrayInitializer(@NotNull JavaParser.ArrayInitializerContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#arrayInitializer}.
	 * @param ctx the parse tree
	 */
	void exitArrayInitializer(@NotNull JavaParser.ArrayInitializerContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#elementValue}.
	 * @param ctx the parse tree
	 */
	void enterElementValue(@NotNull JavaParser.ElementValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#elementValue}.
	 * @param ctx the parse tree
	 */
	void exitElementValue(@NotNull JavaParser.ElementValueContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#constDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterConstDeclaration(@NotNull JavaParser.ConstDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#constDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitConstDeclaration(@NotNull JavaParser.ConstDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#resource}.
	 * @param ctx the parse tree
	 */
	void enterResource(@NotNull JavaParser.ResourceContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#resource}.
	 * @param ctx the parse tree
	 */
	void exitResource(@NotNull JavaParser.ResourceContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedName(@NotNull JavaParser.QualifiedNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedName(@NotNull JavaParser.QualifiedNameContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#resourceSpecification}.
	 * @param ctx the parse tree
	 */
	void enterResourceSpecification(@NotNull JavaParser.ResourceSpecificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#resourceSpecification}.
	 * @param ctx the parse tree
	 */
	void exitResourceSpecification(@NotNull JavaParser.ResourceSpecificationContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#formalParameterList}.
	 * @param ctx the parse tree
	 */
	void enterFormalParameterList(@NotNull JavaParser.FormalParameterListContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#formalParameterList}.
	 * @param ctx the parse tree
	 */
	void exitFormalParameterList(@NotNull JavaParser.FormalParameterListContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#annotationTypeDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationTypeDeclaration(@NotNull JavaParser.AnnotationTypeDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#annotationTypeDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationTypeDeclaration(@NotNull JavaParser.AnnotationTypeDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#compilationUnit}.
	 * @param ctx the parse tree
	 */
	void enterCompilationUnit(@NotNull JavaParser.CompilationUnitContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#compilationUnit}.
	 * @param ctx the parse tree
	 */
	void exitCompilationUnit(@NotNull JavaParser.CompilationUnitContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#annotationMethodRest}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationMethodRest(@NotNull JavaParser.AnnotationMethodRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#annotationMethodRest}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationMethodRest(@NotNull JavaParser.AnnotationMethodRestContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#switchBlockStatementGroup}.
	 * @param ctx the parse tree
	 */
	void enterSwitchBlockStatementGroup(@NotNull JavaParser.SwitchBlockStatementGroupContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#switchBlockStatementGroup}.
	 * @param ctx the parse tree
	 */
	void exitSwitchBlockStatementGroup(@NotNull JavaParser.SwitchBlockStatementGroupContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#typeParameter}.
	 * @param ctx the parse tree
	 */
	void enterTypeParameter(@NotNull JavaParser.TypeParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#typeParameter}.
	 * @param ctx the parse tree
	 */
	void exitTypeParameter(@NotNull JavaParser.TypeParameterContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#interfaceBody}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceBody(@NotNull JavaParser.InterfaceBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#interfaceBody}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceBody(@NotNull JavaParser.InterfaceBodyContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#methodDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterMethodDeclaration(@NotNull JavaParser.MethodDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#methodDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitMethodDeclaration(@NotNull JavaParser.MethodDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#methodBody}.
	 * @param ctx the parse tree
	 */
	void enterMethodBody(@NotNull JavaParser.MethodBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#methodBody}.
	 * @param ctx the parse tree
	 */
	void exitMethodBody(@NotNull JavaParser.MethodBodyContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#typeArgument}.
	 * @param ctx the parse tree
	 */
	void enterTypeArgument(@NotNull JavaParser.TypeArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#typeArgument}.
	 * @param ctx the parse tree
	 */
	void exitTypeArgument(@NotNull JavaParser.TypeArgumentContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#typeDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterTypeDeclaration(@NotNull JavaParser.TypeDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#typeDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitTypeDeclaration(@NotNull JavaParser.TypeDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#genericConstructorDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterGenericConstructorDeclaration(@NotNull JavaParser.GenericConstructorDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#genericConstructorDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitGenericConstructorDeclaration(@NotNull JavaParser.GenericConstructorDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#classDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterClassDeclaration(@NotNull JavaParser.ClassDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#classDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitClassDeclaration(@NotNull JavaParser.ClassDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#enumConstant}.
	 * @param ctx the parse tree
	 */
	void enterEnumConstant(@NotNull JavaParser.EnumConstantContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#enumConstant}.
	 * @param ctx the parse tree
	 */
	void exitEnumConstant(@NotNull JavaParser.EnumConstantContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(@NotNull JavaParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(@NotNull JavaParser.StatementContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#importDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterImportDeclaration(@NotNull JavaParser.ImportDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#importDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitImportDeclaration(@NotNull JavaParser.ImportDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#primitiveType}.
	 * @param ctx the parse tree
	 */
	void enterPrimitiveType(@NotNull JavaParser.PrimitiveTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#primitiveType}.
	 * @param ctx the parse tree
	 */
	void exitPrimitiveType(@NotNull JavaParser.PrimitiveTypeContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#interfaceDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceDeclaration(@NotNull JavaParser.InterfaceDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#interfaceDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceDeclaration(@NotNull JavaParser.InterfaceDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#localVariableDeclarationStatement}.
	 * @param ctx the parse tree
	 */
	void enterLocalVariableDeclarationStatement(@NotNull JavaParser.LocalVariableDeclarationStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#localVariableDeclarationStatement}.
	 * @param ctx the parse tree
	 */
	void exitLocalVariableDeclarationStatement(@NotNull JavaParser.LocalVariableDeclarationStatementContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#blockStatement}.
	 * @param ctx the parse tree
	 */
	void enterBlockStatement(@NotNull JavaParser.BlockStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#blockStatement}.
	 * @param ctx the parse tree
	 */
	void exitBlockStatement(@NotNull JavaParser.BlockStatementContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#fieldDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterFieldDeclaration(@NotNull JavaParser.FieldDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#fieldDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitFieldDeclaration(@NotNull JavaParser.FieldDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#constantDeclarator}.
	 * @param ctx the parse tree
	 */
	void enterConstantDeclarator(@NotNull JavaParser.ConstantDeclaratorContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#constantDeclarator}.
	 * @param ctx the parse tree
	 */
	void exitConstantDeclarator(@NotNull JavaParser.ConstantDeclaratorContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#resources}.
	 * @param ctx the parse tree
	 */
	void enterResources(@NotNull JavaParser.ResourcesContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#resources}.
	 * @param ctx the parse tree
	 */
	void exitResources(@NotNull JavaParser.ResourcesContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#statementExpression}.
	 * @param ctx the parse tree
	 */
	void enterStatementExpression(@NotNull JavaParser.StatementExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#statementExpression}.
	 * @param ctx the parse tree
	 */
	void exitStatementExpression(@NotNull JavaParser.StatementExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#interfaceMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceMethodDeclaration(@NotNull JavaParser.InterfaceMethodDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#interfaceMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceMethodDeclaration(@NotNull JavaParser.InterfaceMethodDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#packageDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterPackageDeclaration(@NotNull JavaParser.PackageDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#packageDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitPackageDeclaration(@NotNull JavaParser.PackageDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#elementValuePairs}.
	 * @param ctx the parse tree
	 */
	void enterElementValuePairs(@NotNull JavaParser.ElementValuePairsContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#elementValuePairs}.
	 * @param ctx the parse tree
	 */
	void exitElementValuePairs(@NotNull JavaParser.ElementValuePairsContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#localVariableDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterLocalVariableDeclaration(@NotNull JavaParser.LocalVariableDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#localVariableDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitLocalVariableDeclaration(@NotNull JavaParser.LocalVariableDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#nonWildcardTypeArguments}.
	 * @param ctx the parse tree
	 */
	void enterNonWildcardTypeArguments(@NotNull JavaParser.NonWildcardTypeArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#nonWildcardTypeArguments}.
	 * @param ctx the parse tree
	 */
	void exitNonWildcardTypeArguments(@NotNull JavaParser.NonWildcardTypeArgumentsContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#interfaceMemberDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceMemberDeclaration(@NotNull JavaParser.InterfaceMemberDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#interfaceMemberDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceMemberDeclaration(@NotNull JavaParser.InterfaceMemberDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#switchLabel}.
	 * @param ctx the parse tree
	 */
	void enterSwitchLabel(@NotNull JavaParser.SwitchLabelContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#switchLabel}.
	 * @param ctx the parse tree
	 */
	void exitSwitchLabel(@NotNull JavaParser.SwitchLabelContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#forInit}.
	 * @param ctx the parse tree
	 */
	void enterForInit(@NotNull JavaParser.ForInitContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#forInit}.
	 * @param ctx the parse tree
	 */
	void exitForInit(@NotNull JavaParser.ForInitContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#formalParameters}.
	 * @param ctx the parse tree
	 */
	void enterFormalParameters(@NotNull JavaParser.FormalParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#formalParameters}.
	 * @param ctx the parse tree
	 */
	void exitFormalParameters(@NotNull JavaParser.FormalParametersContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#arguments}.
	 * @param ctx the parse tree
	 */
	void enterArguments(@NotNull JavaParser.ArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#arguments}.
	 * @param ctx the parse tree
	 */
	void exitArguments(@NotNull JavaParser.ArgumentsContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#genericMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterGenericMethodDeclaration(@NotNull JavaParser.GenericMethodDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#genericMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitGenericMethodDeclaration(@NotNull JavaParser.GenericMethodDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link JavaParser#typeArgumentsOrDiamond}.
	 * @param ctx the parse tree
	 */
	void enterTypeArgumentsOrDiamond(@NotNull JavaParser.TypeArgumentsOrDiamondContext ctx);
	/**
	 * Exit a parse tree produced by {@link JavaParser#typeArgumentsOrDiamond}.
	 * @param ctx the parse tree
	 */
	void exitTypeArgumentsOrDiamond(@NotNull JavaParser.TypeArgumentsOrDiamondContext ctx);
}