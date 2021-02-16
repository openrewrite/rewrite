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
// Generated from /Users/jon/Projects/github/openrewrite/rewrite/rewrite-java/src/main/antlr/RefactorMethodSignatureParser.g4 by ANTLR 4.8
package org.openrewrite.java.internal.grammar;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link RefactorMethodSignatureParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface RefactorMethodSignatureParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#methodPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethodPattern(RefactorMethodSignatureParser.MethodPatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#formalParametersPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormalParametersPattern(RefactorMethodSignatureParser.FormalParametersPatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#formalsPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormalsPattern(RefactorMethodSignatureParser.FormalsPatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#dotDot}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDotDot(RefactorMethodSignatureParser.DotDotContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#formalsPatternAfterDotDot}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormalsPatternAfterDotDot(RefactorMethodSignatureParser.FormalsPatternAfterDotDotContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#optionalParensTypePattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOptionalParensTypePattern(RefactorMethodSignatureParser.OptionalParensTypePatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#targetTypePattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTargetTypePattern(RefactorMethodSignatureParser.TargetTypePatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#formalTypePattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormalTypePattern(RefactorMethodSignatureParser.FormalTypePatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#classNameOrInterface}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassNameOrInterface(RefactorMethodSignatureParser.ClassNameOrInterfaceContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#simpleNamePattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimpleNamePattern(RefactorMethodSignatureParser.SimpleNamePatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#compilationUnit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompilationUnit(RefactorMethodSignatureParser.CompilationUnitContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#packageDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPackageDeclaration(RefactorMethodSignatureParser.PackageDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#importDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportDeclaration(RefactorMethodSignatureParser.ImportDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#typeDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeDeclaration(RefactorMethodSignatureParser.TypeDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#modifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModifier(RefactorMethodSignatureParser.ModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#classOrInterfaceModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassOrInterfaceModifier(RefactorMethodSignatureParser.ClassOrInterfaceModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#variableModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableModifier(RefactorMethodSignatureParser.VariableModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#classDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassDeclaration(RefactorMethodSignatureParser.ClassDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#typeParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeParameters(RefactorMethodSignatureParser.TypeParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#typeParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeParameter(RefactorMethodSignatureParser.TypeParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#typeBound}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeBound(RefactorMethodSignatureParser.TypeBoundContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#enumDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumDeclaration(RefactorMethodSignatureParser.EnumDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#enumConstants}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumConstants(RefactorMethodSignatureParser.EnumConstantsContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#enumConstant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumConstant(RefactorMethodSignatureParser.EnumConstantContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#enumBodyDeclarations}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumBodyDeclarations(RefactorMethodSignatureParser.EnumBodyDeclarationsContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#interfaceDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterfaceDeclaration(RefactorMethodSignatureParser.InterfaceDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#typeList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeList(RefactorMethodSignatureParser.TypeListContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#classBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassBody(RefactorMethodSignatureParser.ClassBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#interfaceBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterfaceBody(RefactorMethodSignatureParser.InterfaceBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#classBodyDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassBodyDeclaration(RefactorMethodSignatureParser.ClassBodyDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#memberDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMemberDeclaration(RefactorMethodSignatureParser.MemberDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#methodDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethodDeclaration(RefactorMethodSignatureParser.MethodDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#genericMethodDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenericMethodDeclaration(RefactorMethodSignatureParser.GenericMethodDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#constructorDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructorDeclaration(RefactorMethodSignatureParser.ConstructorDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#genericConstructorDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenericConstructorDeclaration(RefactorMethodSignatureParser.GenericConstructorDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#fieldDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFieldDeclaration(RefactorMethodSignatureParser.FieldDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#interfaceBodyDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterfaceBodyDeclaration(RefactorMethodSignatureParser.InterfaceBodyDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#interfaceMemberDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterfaceMemberDeclaration(RefactorMethodSignatureParser.InterfaceMemberDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#constDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstDeclaration(RefactorMethodSignatureParser.ConstDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#constantDeclarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstantDeclarator(RefactorMethodSignatureParser.ConstantDeclaratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#interfaceMethodDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterfaceMethodDeclaration(RefactorMethodSignatureParser.InterfaceMethodDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#genericInterfaceMethodDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenericInterfaceMethodDeclaration(RefactorMethodSignatureParser.GenericInterfaceMethodDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#variableDeclarators}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableDeclarators(RefactorMethodSignatureParser.VariableDeclaratorsContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#variableDeclarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableDeclarator(RefactorMethodSignatureParser.VariableDeclaratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#variableDeclaratorId}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableDeclaratorId(RefactorMethodSignatureParser.VariableDeclaratorIdContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#variableInitializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableInitializer(RefactorMethodSignatureParser.VariableInitializerContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#arrayInitializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayInitializer(RefactorMethodSignatureParser.ArrayInitializerContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#enumConstantName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumConstantName(RefactorMethodSignatureParser.EnumConstantNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType(RefactorMethodSignatureParser.TypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#classOrInterfaceType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassOrInterfaceType(RefactorMethodSignatureParser.ClassOrInterfaceTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#primitiveType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimitiveType(RefactorMethodSignatureParser.PrimitiveTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#typeArguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeArguments(RefactorMethodSignatureParser.TypeArgumentsContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#typeArgument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeArgument(RefactorMethodSignatureParser.TypeArgumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#qualifiedNameList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualifiedNameList(RefactorMethodSignatureParser.QualifiedNameListContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#formalParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormalParameters(RefactorMethodSignatureParser.FormalParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#formalParameterList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormalParameterList(RefactorMethodSignatureParser.FormalParameterListContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#formalParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormalParameter(RefactorMethodSignatureParser.FormalParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#lastFormalParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLastFormalParameter(RefactorMethodSignatureParser.LastFormalParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#methodBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethodBody(RefactorMethodSignatureParser.MethodBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#constructorBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructorBody(RefactorMethodSignatureParser.ConstructorBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#qualifiedName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualifiedName(RefactorMethodSignatureParser.QualifiedNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral(RefactorMethodSignatureParser.LiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#annotation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotation(RefactorMethodSignatureParser.AnnotationContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#annotationName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationName(RefactorMethodSignatureParser.AnnotationNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#elementValuePairs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElementValuePairs(RefactorMethodSignatureParser.ElementValuePairsContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#elementValuePair}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElementValuePair(RefactorMethodSignatureParser.ElementValuePairContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#elementValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElementValue(RefactorMethodSignatureParser.ElementValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#elementValueArrayInitializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElementValueArrayInitializer(RefactorMethodSignatureParser.ElementValueArrayInitializerContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#annotationTypeDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationTypeDeclaration(RefactorMethodSignatureParser.AnnotationTypeDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#annotationTypeBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationTypeBody(RefactorMethodSignatureParser.AnnotationTypeBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#annotationTypeElementDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationTypeElementDeclaration(RefactorMethodSignatureParser.AnnotationTypeElementDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#annotationTypeElementRest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationTypeElementRest(RefactorMethodSignatureParser.AnnotationTypeElementRestContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#annotationMethodOrConstantRest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationMethodOrConstantRest(RefactorMethodSignatureParser.AnnotationMethodOrConstantRestContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#annotationMethodRest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationMethodRest(RefactorMethodSignatureParser.AnnotationMethodRestContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#annotationConstantRest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationConstantRest(RefactorMethodSignatureParser.AnnotationConstantRestContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#defaultValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefaultValue(RefactorMethodSignatureParser.DefaultValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlock(RefactorMethodSignatureParser.BlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#blockStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlockStatement(RefactorMethodSignatureParser.BlockStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#localVariableDeclarationStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocalVariableDeclarationStatement(RefactorMethodSignatureParser.LocalVariableDeclarationStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#localVariableDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocalVariableDeclaration(RefactorMethodSignatureParser.LocalVariableDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(RefactorMethodSignatureParser.StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#catchClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCatchClause(RefactorMethodSignatureParser.CatchClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#catchType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCatchType(RefactorMethodSignatureParser.CatchTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#finallyBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFinallyBlock(RefactorMethodSignatureParser.FinallyBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#resourceSpecification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResourceSpecification(RefactorMethodSignatureParser.ResourceSpecificationContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#resources}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResources(RefactorMethodSignatureParser.ResourcesContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#resource}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResource(RefactorMethodSignatureParser.ResourceContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#switchBlockStatementGroup}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitchBlockStatementGroup(RefactorMethodSignatureParser.SwitchBlockStatementGroupContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#switchLabel}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitchLabel(RefactorMethodSignatureParser.SwitchLabelContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#forControl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForControl(RefactorMethodSignatureParser.ForControlContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#forInit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForInit(RefactorMethodSignatureParser.ForInitContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#enhancedForControl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnhancedForControl(RefactorMethodSignatureParser.EnhancedForControlContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#forUpdate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForUpdate(RefactorMethodSignatureParser.ForUpdateContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#parExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParExpression(RefactorMethodSignatureParser.ParExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#expressionList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionList(RefactorMethodSignatureParser.ExpressionListContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#statementExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatementExpression(RefactorMethodSignatureParser.StatementExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#constantExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstantExpression(RefactorMethodSignatureParser.ConstantExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(RefactorMethodSignatureParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimary(RefactorMethodSignatureParser.PrimaryContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#creator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreator(RefactorMethodSignatureParser.CreatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#createdName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreatedName(RefactorMethodSignatureParser.CreatedNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#innerCreator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInnerCreator(RefactorMethodSignatureParser.InnerCreatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#arrayCreatorRest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayCreatorRest(RefactorMethodSignatureParser.ArrayCreatorRestContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#classCreatorRest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassCreatorRest(RefactorMethodSignatureParser.ClassCreatorRestContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#explicitGenericInvocation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExplicitGenericInvocation(RefactorMethodSignatureParser.ExplicitGenericInvocationContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#nonWildcardTypeArguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNonWildcardTypeArguments(RefactorMethodSignatureParser.NonWildcardTypeArgumentsContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#typeArgumentsOrDiamond}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeArgumentsOrDiamond(RefactorMethodSignatureParser.TypeArgumentsOrDiamondContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#nonWildcardTypeArgumentsOrDiamond}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNonWildcardTypeArgumentsOrDiamond(RefactorMethodSignatureParser.NonWildcardTypeArgumentsOrDiamondContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#superSuffix}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSuperSuffix(RefactorMethodSignatureParser.SuperSuffixContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#explicitGenericInvocationSuffix}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExplicitGenericInvocationSuffix(RefactorMethodSignatureParser.ExplicitGenericInvocationSuffixContext ctx);
	/**
	 * Visit a parse tree produced by {@link RefactorMethodSignatureParser#arguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArguments(RefactorMethodSignatureParser.ArgumentsContext ctx);
}