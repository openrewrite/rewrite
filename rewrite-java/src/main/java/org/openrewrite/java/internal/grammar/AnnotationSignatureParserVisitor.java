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
// Generated from /Users/jon/Projects/github/openrewrite/rewrite/rewrite-java/src/main/antlr/AnnotationSignatureParser.g4 by ANTLR 4.8
package org.openrewrite.java.internal.grammar;
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
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#annotation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotation(AnnotationSignatureParser.AnnotationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#compilationUnit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompilationUnit(AnnotationSignatureParser.CompilationUnitContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#packageDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPackageDeclaration(AnnotationSignatureParser.PackageDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#importDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportDeclaration(AnnotationSignatureParser.ImportDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#typeDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeDeclaration(AnnotationSignatureParser.TypeDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#modifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModifier(AnnotationSignatureParser.ModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#classOrInterfaceModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassOrInterfaceModifier(AnnotationSignatureParser.ClassOrInterfaceModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#variableModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableModifier(AnnotationSignatureParser.VariableModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#classDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassDeclaration(AnnotationSignatureParser.ClassDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#typeParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeParameters(AnnotationSignatureParser.TypeParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#typeParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeParameter(AnnotationSignatureParser.TypeParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#typeBound}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeBound(AnnotationSignatureParser.TypeBoundContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#enumDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumDeclaration(AnnotationSignatureParser.EnumDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#enumConstants}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumConstants(AnnotationSignatureParser.EnumConstantsContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#enumConstant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumConstant(AnnotationSignatureParser.EnumConstantContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#enumBodyDeclarations}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumBodyDeclarations(AnnotationSignatureParser.EnumBodyDeclarationsContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#interfaceDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterfaceDeclaration(AnnotationSignatureParser.InterfaceDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#typeList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeList(AnnotationSignatureParser.TypeListContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#classBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassBody(AnnotationSignatureParser.ClassBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#interfaceBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterfaceBody(AnnotationSignatureParser.InterfaceBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#classBodyDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassBodyDeclaration(AnnotationSignatureParser.ClassBodyDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#memberDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMemberDeclaration(AnnotationSignatureParser.MemberDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#methodDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethodDeclaration(AnnotationSignatureParser.MethodDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#genericMethodDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenericMethodDeclaration(AnnotationSignatureParser.GenericMethodDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#constructorDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructorDeclaration(AnnotationSignatureParser.ConstructorDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#genericConstructorDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenericConstructorDeclaration(AnnotationSignatureParser.GenericConstructorDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#fieldDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFieldDeclaration(AnnotationSignatureParser.FieldDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#interfaceBodyDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterfaceBodyDeclaration(AnnotationSignatureParser.InterfaceBodyDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#interfaceMemberDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterfaceMemberDeclaration(AnnotationSignatureParser.InterfaceMemberDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#constDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstDeclaration(AnnotationSignatureParser.ConstDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#constantDeclarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstantDeclarator(AnnotationSignatureParser.ConstantDeclaratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#interfaceMethodDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterfaceMethodDeclaration(AnnotationSignatureParser.InterfaceMethodDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#genericInterfaceMethodDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenericInterfaceMethodDeclaration(AnnotationSignatureParser.GenericInterfaceMethodDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#variableDeclarators}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableDeclarators(AnnotationSignatureParser.VariableDeclaratorsContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#variableDeclarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableDeclarator(AnnotationSignatureParser.VariableDeclaratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#variableDeclaratorId}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableDeclaratorId(AnnotationSignatureParser.VariableDeclaratorIdContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#variableInitializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableInitializer(AnnotationSignatureParser.VariableInitializerContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#arrayInitializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayInitializer(AnnotationSignatureParser.ArrayInitializerContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#enumConstantName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumConstantName(AnnotationSignatureParser.EnumConstantNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType(AnnotationSignatureParser.TypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#classOrInterfaceType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassOrInterfaceType(AnnotationSignatureParser.ClassOrInterfaceTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#primitiveType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimitiveType(AnnotationSignatureParser.PrimitiveTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#typeArguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeArguments(AnnotationSignatureParser.TypeArgumentsContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#typeArgument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeArgument(AnnotationSignatureParser.TypeArgumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#qualifiedNameList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualifiedNameList(AnnotationSignatureParser.QualifiedNameListContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#formalParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormalParameters(AnnotationSignatureParser.FormalParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#formalParameterList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormalParameterList(AnnotationSignatureParser.FormalParameterListContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#formalParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormalParameter(AnnotationSignatureParser.FormalParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#lastFormalParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLastFormalParameter(AnnotationSignatureParser.LastFormalParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#methodBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethodBody(AnnotationSignatureParser.MethodBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#constructorBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructorBody(AnnotationSignatureParser.ConstructorBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#qualifiedName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualifiedName(AnnotationSignatureParser.QualifiedNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral(AnnotationSignatureParser.LiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#annotationName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationName(AnnotationSignatureParser.AnnotationNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#elementValuePairs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElementValuePairs(AnnotationSignatureParser.ElementValuePairsContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#elementValuePair}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElementValuePair(AnnotationSignatureParser.ElementValuePairContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#elementValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElementValue(AnnotationSignatureParser.ElementValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#elementValueArrayInitializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElementValueArrayInitializer(AnnotationSignatureParser.ElementValueArrayInitializerContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#annotationTypeDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationTypeDeclaration(AnnotationSignatureParser.AnnotationTypeDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#annotationTypeBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationTypeBody(AnnotationSignatureParser.AnnotationTypeBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#annotationTypeElementDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationTypeElementDeclaration(AnnotationSignatureParser.AnnotationTypeElementDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#annotationTypeElementRest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationTypeElementRest(AnnotationSignatureParser.AnnotationTypeElementRestContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#annotationMethodOrConstantRest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationMethodOrConstantRest(AnnotationSignatureParser.AnnotationMethodOrConstantRestContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#annotationMethodRest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationMethodRest(AnnotationSignatureParser.AnnotationMethodRestContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#annotationConstantRest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationConstantRest(AnnotationSignatureParser.AnnotationConstantRestContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#defaultValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefaultValue(AnnotationSignatureParser.DefaultValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlock(AnnotationSignatureParser.BlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#blockStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlockStatement(AnnotationSignatureParser.BlockStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#localVariableDeclarationStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocalVariableDeclarationStatement(AnnotationSignatureParser.LocalVariableDeclarationStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#localVariableDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocalVariableDeclaration(AnnotationSignatureParser.LocalVariableDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(AnnotationSignatureParser.StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#catchClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCatchClause(AnnotationSignatureParser.CatchClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#catchType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCatchType(AnnotationSignatureParser.CatchTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#finallyBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFinallyBlock(AnnotationSignatureParser.FinallyBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#resourceSpecification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResourceSpecification(AnnotationSignatureParser.ResourceSpecificationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#resources}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResources(AnnotationSignatureParser.ResourcesContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#resource}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResource(AnnotationSignatureParser.ResourceContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#switchBlockStatementGroup}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitchBlockStatementGroup(AnnotationSignatureParser.SwitchBlockStatementGroupContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#switchLabel}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitchLabel(AnnotationSignatureParser.SwitchLabelContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#forControl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForControl(AnnotationSignatureParser.ForControlContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#forInit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForInit(AnnotationSignatureParser.ForInitContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#enhancedForControl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnhancedForControl(AnnotationSignatureParser.EnhancedForControlContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#forUpdate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForUpdate(AnnotationSignatureParser.ForUpdateContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#parExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParExpression(AnnotationSignatureParser.ParExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#expressionList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionList(AnnotationSignatureParser.ExpressionListContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#statementExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatementExpression(AnnotationSignatureParser.StatementExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#constantExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstantExpression(AnnotationSignatureParser.ConstantExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(AnnotationSignatureParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimary(AnnotationSignatureParser.PrimaryContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#creator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreator(AnnotationSignatureParser.CreatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#createdName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreatedName(AnnotationSignatureParser.CreatedNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#innerCreator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInnerCreator(AnnotationSignatureParser.InnerCreatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#arrayCreatorRest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayCreatorRest(AnnotationSignatureParser.ArrayCreatorRestContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#classCreatorRest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassCreatorRest(AnnotationSignatureParser.ClassCreatorRestContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#explicitGenericInvocation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExplicitGenericInvocation(AnnotationSignatureParser.ExplicitGenericInvocationContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#nonWildcardTypeArguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNonWildcardTypeArguments(AnnotationSignatureParser.NonWildcardTypeArgumentsContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#typeArgumentsOrDiamond}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeArgumentsOrDiamond(AnnotationSignatureParser.TypeArgumentsOrDiamondContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#nonWildcardTypeArgumentsOrDiamond}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNonWildcardTypeArgumentsOrDiamond(AnnotationSignatureParser.NonWildcardTypeArgumentsOrDiamondContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#superSuffix}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSuperSuffix(AnnotationSignatureParser.SuperSuffixContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#explicitGenericInvocationSuffix}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExplicitGenericInvocationSuffix(AnnotationSignatureParser.ExplicitGenericInvocationSuffixContext ctx);
	/**
	 * Visit a parse tree produced by {@link AnnotationSignatureParser#arguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArguments(AnnotationSignatureParser.ArgumentsContext ctx);
}