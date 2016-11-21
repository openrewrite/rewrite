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
package com.netflix.java.refactor.aspectj;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link AnnotationSignatureParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface AnnotationSignatureParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#memberDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMemberDeclaration(@NotNull AnnotationSignatureParser.MemberDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#defaultValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefaultValue(@NotNull AnnotationSignatureParser.DefaultValueContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#annotationTypeElementDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationTypeElementDeclaration(@NotNull AnnotationSignatureParser.AnnotationTypeElementDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType(@NotNull AnnotationSignatureParser.TypeContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#annotationTypeBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationTypeBody(@NotNull AnnotationSignatureParser.AnnotationTypeBodyContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#genericInterfaceMethodDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenericInterfaceMethodDeclaration(@NotNull AnnotationSignatureParser.GenericInterfaceMethodDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#classBodyDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassBodyDeclaration(@NotNull AnnotationSignatureParser.ClassBodyDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlock(@NotNull AnnotationSignatureParser.BlockContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#enumBodyDeclarations}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumBodyDeclarations(@NotNull AnnotationSignatureParser.EnumBodyDeclarationsContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#forUpdate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForUpdate(@NotNull AnnotationSignatureParser.ForUpdateContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#enhancedForControl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnhancedForControl(@NotNull AnnotationSignatureParser.EnhancedForControlContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#annotationConstantRest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationConstantRest(@NotNull AnnotationSignatureParser.AnnotationConstantRestContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#explicitGenericInvocation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExplicitGenericInvocation(@NotNull AnnotationSignatureParser.ExplicitGenericInvocationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#nonWildcardTypeArgumentsOrDiamond}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNonWildcardTypeArgumentsOrDiamond(@NotNull AnnotationSignatureParser.NonWildcardTypeArgumentsOrDiamondContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#expressionList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionList(@NotNull AnnotationSignatureParser.ExpressionListContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#annotationTypeElementRest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationTypeElementRest(@NotNull AnnotationSignatureParser.AnnotationTypeElementRestContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#classOrInterfaceType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassOrInterfaceType(@NotNull AnnotationSignatureParser.ClassOrInterfaceTypeContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#typeBound}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeBound(@NotNull AnnotationSignatureParser.TypeBoundContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#variableDeclaratorId}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableDeclaratorId(@NotNull AnnotationSignatureParser.VariableDeclaratorIdContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimary(@NotNull AnnotationSignatureParser.PrimaryContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#classCreatorRest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassCreatorRest(@NotNull AnnotationSignatureParser.ClassCreatorRestContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#interfaceBodyDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterfaceBodyDeclaration(@NotNull AnnotationSignatureParser.InterfaceBodyDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#typeArguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeArguments(@NotNull AnnotationSignatureParser.TypeArgumentsContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#annotationName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationName(@NotNull AnnotationSignatureParser.AnnotationNameContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#finallyBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFinallyBlock(@NotNull AnnotationSignatureParser.FinallyBlockContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#typeParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeParameters(@NotNull AnnotationSignatureParser.TypeParametersContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#lastFormalParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLastFormalParameter(@NotNull AnnotationSignatureParser.LastFormalParameterContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#constructorBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructorBody(@NotNull AnnotationSignatureParser.ConstructorBodyContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral(@NotNull AnnotationSignatureParser.LiteralContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#annotationMethodOrConstantRest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationMethodOrConstantRest(@NotNull AnnotationSignatureParser.AnnotationMethodOrConstantRestContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#catchClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCatchClause(@NotNull AnnotationSignatureParser.CatchClauseContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#variableDeclarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableDeclarator(@NotNull AnnotationSignatureParser.VariableDeclaratorContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#typeList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeList(@NotNull AnnotationSignatureParser.TypeListContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#enumConstants}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumConstants(@NotNull AnnotationSignatureParser.EnumConstantsContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#classBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassBody(@NotNull AnnotationSignatureParser.ClassBodyContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#createdName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreatedName(@NotNull AnnotationSignatureParser.CreatedNameContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#enumDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumDeclaration(@NotNull AnnotationSignatureParser.EnumDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#formalParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormalParameter(@NotNull AnnotationSignatureParser.FormalParameterContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#parExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParExpression(@NotNull AnnotationSignatureParser.ParExpressionContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#annotation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotation(@NotNull AnnotationSignatureParser.AnnotationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#variableInitializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableInitializer(@NotNull AnnotationSignatureParser.VariableInitializerContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#elementValueArrayInitializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElementValueArrayInitializer(@NotNull AnnotationSignatureParser.ElementValueArrayInitializerContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#creator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreator(@NotNull AnnotationSignatureParser.CreatorContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#arrayCreatorRest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayCreatorRest(@NotNull AnnotationSignatureParser.ArrayCreatorRestContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(@NotNull AnnotationSignatureParser.ExpressionContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#constantExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstantExpression(@NotNull AnnotationSignatureParser.ConstantExpressionContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#qualifiedNameList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualifiedNameList(@NotNull AnnotationSignatureParser.QualifiedNameListContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#constructorDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructorDeclaration(@NotNull AnnotationSignatureParser.ConstructorDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#forControl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForControl(@NotNull AnnotationSignatureParser.ForControlContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#superSuffix}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSuperSuffix(@NotNull AnnotationSignatureParser.SuperSuffixContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#variableDeclarators}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableDeclarators(@NotNull AnnotationSignatureParser.VariableDeclaratorsContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#catchType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCatchType(@NotNull AnnotationSignatureParser.CatchTypeContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#classOrInterfaceModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassOrInterfaceModifier(@NotNull AnnotationSignatureParser.ClassOrInterfaceModifierContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#enumConstantName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumConstantName(@NotNull AnnotationSignatureParser.EnumConstantNameContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#modifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModifier(@NotNull AnnotationSignatureParser.ModifierContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#innerCreator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInnerCreator(@NotNull AnnotationSignatureParser.InnerCreatorContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#explicitGenericInvocationSuffix}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExplicitGenericInvocationSuffix(@NotNull AnnotationSignatureParser.ExplicitGenericInvocationSuffixContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#variableModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableModifier(@NotNull AnnotationSignatureParser.VariableModifierContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#elementValuePair}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElementValuePair(@NotNull AnnotationSignatureParser.ElementValuePairContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#arrayInitializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayInitializer(@NotNull AnnotationSignatureParser.ArrayInitializerContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#elementValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElementValue(@NotNull AnnotationSignatureParser.ElementValueContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#constDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstDeclaration(@NotNull AnnotationSignatureParser.ConstDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#resource}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResource(@NotNull AnnotationSignatureParser.ResourceContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#qualifiedName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualifiedName(@NotNull AnnotationSignatureParser.QualifiedNameContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#resourceSpecification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResourceSpecification(@NotNull AnnotationSignatureParser.ResourceSpecificationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#formalParameterList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormalParameterList(@NotNull AnnotationSignatureParser.FormalParameterListContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#annotationTypeDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationTypeDeclaration(@NotNull AnnotationSignatureParser.AnnotationTypeDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#compilationUnit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompilationUnit(@NotNull AnnotationSignatureParser.CompilationUnitContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#annotationMethodRest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationMethodRest(@NotNull AnnotationSignatureParser.AnnotationMethodRestContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#switchBlockStatementGroup}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitchBlockStatementGroup(@NotNull AnnotationSignatureParser.SwitchBlockStatementGroupContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#typeParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeParameter(@NotNull AnnotationSignatureParser.TypeParameterContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#interfaceBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterfaceBody(@NotNull AnnotationSignatureParser.InterfaceBodyContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#methodDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethodDeclaration(@NotNull AnnotationSignatureParser.MethodDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#methodBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethodBody(@NotNull AnnotationSignatureParser.MethodBodyContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#typeArgument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeArgument(@NotNull AnnotationSignatureParser.TypeArgumentContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#typeDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeDeclaration(@NotNull AnnotationSignatureParser.TypeDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#genericConstructorDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenericConstructorDeclaration(@NotNull AnnotationSignatureParser.GenericConstructorDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#classDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassDeclaration(@NotNull AnnotationSignatureParser.ClassDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#enumConstant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumConstant(@NotNull AnnotationSignatureParser.EnumConstantContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(@NotNull AnnotationSignatureParser.StatementContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#importDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportDeclaration(@NotNull AnnotationSignatureParser.ImportDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#primitiveType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimitiveType(@NotNull AnnotationSignatureParser.PrimitiveTypeContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#interfaceDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterfaceDeclaration(@NotNull AnnotationSignatureParser.InterfaceDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#localVariableDeclarationStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocalVariableDeclarationStatement(@NotNull AnnotationSignatureParser.LocalVariableDeclarationStatementContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#blockStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlockStatement(@NotNull AnnotationSignatureParser.BlockStatementContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#fieldDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFieldDeclaration(@NotNull AnnotationSignatureParser.FieldDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#constantDeclarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstantDeclarator(@NotNull AnnotationSignatureParser.ConstantDeclaratorContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#resources}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResources(@NotNull AnnotationSignatureParser.ResourcesContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#statementExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatementExpression(@NotNull AnnotationSignatureParser.StatementExpressionContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#interfaceMethodDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterfaceMethodDeclaration(@NotNull AnnotationSignatureParser.InterfaceMethodDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#packageDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPackageDeclaration(@NotNull AnnotationSignatureParser.PackageDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#elementValuePairs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElementValuePairs(@NotNull AnnotationSignatureParser.ElementValuePairsContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#localVariableDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocalVariableDeclaration(@NotNull AnnotationSignatureParser.LocalVariableDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#nonWildcardTypeArguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNonWildcardTypeArguments(@NotNull AnnotationSignatureParser.NonWildcardTypeArgumentsContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#interfaceMemberDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterfaceMemberDeclaration(@NotNull AnnotationSignatureParser.InterfaceMemberDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#switchLabel}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitchLabel(@NotNull AnnotationSignatureParser.SwitchLabelContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#forInit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForInit(@NotNull AnnotationSignatureParser.ForInitContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#formalParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormalParameters(@NotNull AnnotationSignatureParser.FormalParametersContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#arguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArguments(@NotNull AnnotationSignatureParser.ArgumentsContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#genericMethodDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenericMethodDeclaration(@NotNull AnnotationSignatureParser.GenericMethodDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#typeArgumentsOrDiamond}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeArgumentsOrDiamond(@NotNull AnnotationSignatureParser.TypeArgumentsOrDiamondContext ctx);
}