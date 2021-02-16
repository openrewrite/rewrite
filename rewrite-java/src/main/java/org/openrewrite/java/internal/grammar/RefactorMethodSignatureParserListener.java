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
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link RefactorMethodSignatureParser}.
 */
public interface RefactorMethodSignatureParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#methodPattern}.
	 * @param ctx the parse tree
	 */
	void enterMethodPattern(RefactorMethodSignatureParser.MethodPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#methodPattern}.
	 * @param ctx the parse tree
	 */
	void exitMethodPattern(RefactorMethodSignatureParser.MethodPatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#formalParametersPattern}.
	 * @param ctx the parse tree
	 */
	void enterFormalParametersPattern(RefactorMethodSignatureParser.FormalParametersPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#formalParametersPattern}.
	 * @param ctx the parse tree
	 */
	void exitFormalParametersPattern(RefactorMethodSignatureParser.FormalParametersPatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#formalsPattern}.
	 * @param ctx the parse tree
	 */
	void enterFormalsPattern(RefactorMethodSignatureParser.FormalsPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#formalsPattern}.
	 * @param ctx the parse tree
	 */
	void exitFormalsPattern(RefactorMethodSignatureParser.FormalsPatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#dotDot}.
	 * @param ctx the parse tree
	 */
	void enterDotDot(RefactorMethodSignatureParser.DotDotContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#dotDot}.
	 * @param ctx the parse tree
	 */
	void exitDotDot(RefactorMethodSignatureParser.DotDotContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#formalsPatternAfterDotDot}.
	 * @param ctx the parse tree
	 */
	void enterFormalsPatternAfterDotDot(RefactorMethodSignatureParser.FormalsPatternAfterDotDotContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#formalsPatternAfterDotDot}.
	 * @param ctx the parse tree
	 */
	void exitFormalsPatternAfterDotDot(RefactorMethodSignatureParser.FormalsPatternAfterDotDotContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#optionalParensTypePattern}.
	 * @param ctx the parse tree
	 */
	void enterOptionalParensTypePattern(RefactorMethodSignatureParser.OptionalParensTypePatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#optionalParensTypePattern}.
	 * @param ctx the parse tree
	 */
	void exitOptionalParensTypePattern(RefactorMethodSignatureParser.OptionalParensTypePatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#targetTypePattern}.
	 * @param ctx the parse tree
	 */
	void enterTargetTypePattern(RefactorMethodSignatureParser.TargetTypePatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#targetTypePattern}.
	 * @param ctx the parse tree
	 */
	void exitTargetTypePattern(RefactorMethodSignatureParser.TargetTypePatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#formalTypePattern}.
	 * @param ctx the parse tree
	 */
	void enterFormalTypePattern(RefactorMethodSignatureParser.FormalTypePatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#formalTypePattern}.
	 * @param ctx the parse tree
	 */
	void exitFormalTypePattern(RefactorMethodSignatureParser.FormalTypePatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#classNameOrInterface}.
	 * @param ctx the parse tree
	 */
	void enterClassNameOrInterface(RefactorMethodSignatureParser.ClassNameOrInterfaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#classNameOrInterface}.
	 * @param ctx the parse tree
	 */
	void exitClassNameOrInterface(RefactorMethodSignatureParser.ClassNameOrInterfaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#simpleNamePattern}.
	 * @param ctx the parse tree
	 */
	void enterSimpleNamePattern(RefactorMethodSignatureParser.SimpleNamePatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#simpleNamePattern}.
	 * @param ctx the parse tree
	 */
	void exitSimpleNamePattern(RefactorMethodSignatureParser.SimpleNamePatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#compilationUnit}.
	 * @param ctx the parse tree
	 */
	void enterCompilationUnit(RefactorMethodSignatureParser.CompilationUnitContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#compilationUnit}.
	 * @param ctx the parse tree
	 */
	void exitCompilationUnit(RefactorMethodSignatureParser.CompilationUnitContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#packageDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterPackageDeclaration(RefactorMethodSignatureParser.PackageDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#packageDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitPackageDeclaration(RefactorMethodSignatureParser.PackageDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#importDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterImportDeclaration(RefactorMethodSignatureParser.ImportDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#importDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitImportDeclaration(RefactorMethodSignatureParser.ImportDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#typeDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterTypeDeclaration(RefactorMethodSignatureParser.TypeDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#typeDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitTypeDeclaration(RefactorMethodSignatureParser.TypeDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#modifier}.
	 * @param ctx the parse tree
	 */
	void enterModifier(RefactorMethodSignatureParser.ModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#modifier}.
	 * @param ctx the parse tree
	 */
	void exitModifier(RefactorMethodSignatureParser.ModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#classOrInterfaceModifier}.
	 * @param ctx the parse tree
	 */
	void enterClassOrInterfaceModifier(RefactorMethodSignatureParser.ClassOrInterfaceModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#classOrInterfaceModifier}.
	 * @param ctx the parse tree
	 */
	void exitClassOrInterfaceModifier(RefactorMethodSignatureParser.ClassOrInterfaceModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#variableModifier}.
	 * @param ctx the parse tree
	 */
	void enterVariableModifier(RefactorMethodSignatureParser.VariableModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#variableModifier}.
	 * @param ctx the parse tree
	 */
	void exitVariableModifier(RefactorMethodSignatureParser.VariableModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#classDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterClassDeclaration(RefactorMethodSignatureParser.ClassDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#classDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitClassDeclaration(RefactorMethodSignatureParser.ClassDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#typeParameters}.
	 * @param ctx the parse tree
	 */
	void enterTypeParameters(RefactorMethodSignatureParser.TypeParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#typeParameters}.
	 * @param ctx the parse tree
	 */
	void exitTypeParameters(RefactorMethodSignatureParser.TypeParametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#typeParameter}.
	 * @param ctx the parse tree
	 */
	void enterTypeParameter(RefactorMethodSignatureParser.TypeParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#typeParameter}.
	 * @param ctx the parse tree
	 */
	void exitTypeParameter(RefactorMethodSignatureParser.TypeParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#typeBound}.
	 * @param ctx the parse tree
	 */
	void enterTypeBound(RefactorMethodSignatureParser.TypeBoundContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#typeBound}.
	 * @param ctx the parse tree
	 */
	void exitTypeBound(RefactorMethodSignatureParser.TypeBoundContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#enumDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterEnumDeclaration(RefactorMethodSignatureParser.EnumDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#enumDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitEnumDeclaration(RefactorMethodSignatureParser.EnumDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#enumConstants}.
	 * @param ctx the parse tree
	 */
	void enterEnumConstants(RefactorMethodSignatureParser.EnumConstantsContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#enumConstants}.
	 * @param ctx the parse tree
	 */
	void exitEnumConstants(RefactorMethodSignatureParser.EnumConstantsContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#enumConstant}.
	 * @param ctx the parse tree
	 */
	void enterEnumConstant(RefactorMethodSignatureParser.EnumConstantContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#enumConstant}.
	 * @param ctx the parse tree
	 */
	void exitEnumConstant(RefactorMethodSignatureParser.EnumConstantContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#enumBodyDeclarations}.
	 * @param ctx the parse tree
	 */
	void enterEnumBodyDeclarations(RefactorMethodSignatureParser.EnumBodyDeclarationsContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#enumBodyDeclarations}.
	 * @param ctx the parse tree
	 */
	void exitEnumBodyDeclarations(RefactorMethodSignatureParser.EnumBodyDeclarationsContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#interfaceDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceDeclaration(RefactorMethodSignatureParser.InterfaceDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#interfaceDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceDeclaration(RefactorMethodSignatureParser.InterfaceDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#typeList}.
	 * @param ctx the parse tree
	 */
	void enterTypeList(RefactorMethodSignatureParser.TypeListContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#typeList}.
	 * @param ctx the parse tree
	 */
	void exitTypeList(RefactorMethodSignatureParser.TypeListContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#classBody}.
	 * @param ctx the parse tree
	 */
	void enterClassBody(RefactorMethodSignatureParser.ClassBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#classBody}.
	 * @param ctx the parse tree
	 */
	void exitClassBody(RefactorMethodSignatureParser.ClassBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#interfaceBody}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceBody(RefactorMethodSignatureParser.InterfaceBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#interfaceBody}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceBody(RefactorMethodSignatureParser.InterfaceBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#classBodyDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterClassBodyDeclaration(RefactorMethodSignatureParser.ClassBodyDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#classBodyDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitClassBodyDeclaration(RefactorMethodSignatureParser.ClassBodyDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#memberDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterMemberDeclaration(RefactorMethodSignatureParser.MemberDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#memberDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitMemberDeclaration(RefactorMethodSignatureParser.MemberDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#methodDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterMethodDeclaration(RefactorMethodSignatureParser.MethodDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#methodDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitMethodDeclaration(RefactorMethodSignatureParser.MethodDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#genericMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterGenericMethodDeclaration(RefactorMethodSignatureParser.GenericMethodDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#genericMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitGenericMethodDeclaration(RefactorMethodSignatureParser.GenericMethodDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#constructorDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterConstructorDeclaration(RefactorMethodSignatureParser.ConstructorDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#constructorDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitConstructorDeclaration(RefactorMethodSignatureParser.ConstructorDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#genericConstructorDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterGenericConstructorDeclaration(RefactorMethodSignatureParser.GenericConstructorDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#genericConstructorDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitGenericConstructorDeclaration(RefactorMethodSignatureParser.GenericConstructorDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#fieldDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterFieldDeclaration(RefactorMethodSignatureParser.FieldDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#fieldDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitFieldDeclaration(RefactorMethodSignatureParser.FieldDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#interfaceBodyDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceBodyDeclaration(RefactorMethodSignatureParser.InterfaceBodyDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#interfaceBodyDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceBodyDeclaration(RefactorMethodSignatureParser.InterfaceBodyDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#interfaceMemberDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceMemberDeclaration(RefactorMethodSignatureParser.InterfaceMemberDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#interfaceMemberDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceMemberDeclaration(RefactorMethodSignatureParser.InterfaceMemberDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#constDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterConstDeclaration(RefactorMethodSignatureParser.ConstDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#constDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitConstDeclaration(RefactorMethodSignatureParser.ConstDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#constantDeclarator}.
	 * @param ctx the parse tree
	 */
	void enterConstantDeclarator(RefactorMethodSignatureParser.ConstantDeclaratorContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#constantDeclarator}.
	 * @param ctx the parse tree
	 */
	void exitConstantDeclarator(RefactorMethodSignatureParser.ConstantDeclaratorContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#interfaceMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceMethodDeclaration(RefactorMethodSignatureParser.InterfaceMethodDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#interfaceMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceMethodDeclaration(RefactorMethodSignatureParser.InterfaceMethodDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#genericInterfaceMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterGenericInterfaceMethodDeclaration(RefactorMethodSignatureParser.GenericInterfaceMethodDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#genericInterfaceMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitGenericInterfaceMethodDeclaration(RefactorMethodSignatureParser.GenericInterfaceMethodDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#variableDeclarators}.
	 * @param ctx the parse tree
	 */
	void enterVariableDeclarators(RefactorMethodSignatureParser.VariableDeclaratorsContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#variableDeclarators}.
	 * @param ctx the parse tree
	 */
	void exitVariableDeclarators(RefactorMethodSignatureParser.VariableDeclaratorsContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#variableDeclarator}.
	 * @param ctx the parse tree
	 */
	void enterVariableDeclarator(RefactorMethodSignatureParser.VariableDeclaratorContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#variableDeclarator}.
	 * @param ctx the parse tree
	 */
	void exitVariableDeclarator(RefactorMethodSignatureParser.VariableDeclaratorContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#variableDeclaratorId}.
	 * @param ctx the parse tree
	 */
	void enterVariableDeclaratorId(RefactorMethodSignatureParser.VariableDeclaratorIdContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#variableDeclaratorId}.
	 * @param ctx the parse tree
	 */
	void exitVariableDeclaratorId(RefactorMethodSignatureParser.VariableDeclaratorIdContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#variableInitializer}.
	 * @param ctx the parse tree
	 */
	void enterVariableInitializer(RefactorMethodSignatureParser.VariableInitializerContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#variableInitializer}.
	 * @param ctx the parse tree
	 */
	void exitVariableInitializer(RefactorMethodSignatureParser.VariableInitializerContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#arrayInitializer}.
	 * @param ctx the parse tree
	 */
	void enterArrayInitializer(RefactorMethodSignatureParser.ArrayInitializerContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#arrayInitializer}.
	 * @param ctx the parse tree
	 */
	void exitArrayInitializer(RefactorMethodSignatureParser.ArrayInitializerContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#enumConstantName}.
	 * @param ctx the parse tree
	 */
	void enterEnumConstantName(RefactorMethodSignatureParser.EnumConstantNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#enumConstantName}.
	 * @param ctx the parse tree
	 */
	void exitEnumConstantName(RefactorMethodSignatureParser.EnumConstantNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(RefactorMethodSignatureParser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(RefactorMethodSignatureParser.TypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#classOrInterfaceType}.
	 * @param ctx the parse tree
	 */
	void enterClassOrInterfaceType(RefactorMethodSignatureParser.ClassOrInterfaceTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#classOrInterfaceType}.
	 * @param ctx the parse tree
	 */
	void exitClassOrInterfaceType(RefactorMethodSignatureParser.ClassOrInterfaceTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#primitiveType}.
	 * @param ctx the parse tree
	 */
	void enterPrimitiveType(RefactorMethodSignatureParser.PrimitiveTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#primitiveType}.
	 * @param ctx the parse tree
	 */
	void exitPrimitiveType(RefactorMethodSignatureParser.PrimitiveTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#typeArguments}.
	 * @param ctx the parse tree
	 */
	void enterTypeArguments(RefactorMethodSignatureParser.TypeArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#typeArguments}.
	 * @param ctx the parse tree
	 */
	void exitTypeArguments(RefactorMethodSignatureParser.TypeArgumentsContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#typeArgument}.
	 * @param ctx the parse tree
	 */
	void enterTypeArgument(RefactorMethodSignatureParser.TypeArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#typeArgument}.
	 * @param ctx the parse tree
	 */
	void exitTypeArgument(RefactorMethodSignatureParser.TypeArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#qualifiedNameList}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedNameList(RefactorMethodSignatureParser.QualifiedNameListContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#qualifiedNameList}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedNameList(RefactorMethodSignatureParser.QualifiedNameListContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#formalParameters}.
	 * @param ctx the parse tree
	 */
	void enterFormalParameters(RefactorMethodSignatureParser.FormalParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#formalParameters}.
	 * @param ctx the parse tree
	 */
	void exitFormalParameters(RefactorMethodSignatureParser.FormalParametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#formalParameterList}.
	 * @param ctx the parse tree
	 */
	void enterFormalParameterList(RefactorMethodSignatureParser.FormalParameterListContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#formalParameterList}.
	 * @param ctx the parse tree
	 */
	void exitFormalParameterList(RefactorMethodSignatureParser.FormalParameterListContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#formalParameter}.
	 * @param ctx the parse tree
	 */
	void enterFormalParameter(RefactorMethodSignatureParser.FormalParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#formalParameter}.
	 * @param ctx the parse tree
	 */
	void exitFormalParameter(RefactorMethodSignatureParser.FormalParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#lastFormalParameter}.
	 * @param ctx the parse tree
	 */
	void enterLastFormalParameter(RefactorMethodSignatureParser.LastFormalParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#lastFormalParameter}.
	 * @param ctx the parse tree
	 */
	void exitLastFormalParameter(RefactorMethodSignatureParser.LastFormalParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#methodBody}.
	 * @param ctx the parse tree
	 */
	void enterMethodBody(RefactorMethodSignatureParser.MethodBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#methodBody}.
	 * @param ctx the parse tree
	 */
	void exitMethodBody(RefactorMethodSignatureParser.MethodBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#constructorBody}.
	 * @param ctx the parse tree
	 */
	void enterConstructorBody(RefactorMethodSignatureParser.ConstructorBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#constructorBody}.
	 * @param ctx the parse tree
	 */
	void exitConstructorBody(RefactorMethodSignatureParser.ConstructorBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedName(RefactorMethodSignatureParser.QualifiedNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedName(RefactorMethodSignatureParser.QualifiedNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLiteral(RefactorMethodSignatureParser.LiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLiteral(RefactorMethodSignatureParser.LiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#annotation}.
	 * @param ctx the parse tree
	 */
	void enterAnnotation(RefactorMethodSignatureParser.AnnotationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#annotation}.
	 * @param ctx the parse tree
	 */
	void exitAnnotation(RefactorMethodSignatureParser.AnnotationContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#annotationName}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationName(RefactorMethodSignatureParser.AnnotationNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#annotationName}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationName(RefactorMethodSignatureParser.AnnotationNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#elementValuePairs}.
	 * @param ctx the parse tree
	 */
	void enterElementValuePairs(RefactorMethodSignatureParser.ElementValuePairsContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#elementValuePairs}.
	 * @param ctx the parse tree
	 */
	void exitElementValuePairs(RefactorMethodSignatureParser.ElementValuePairsContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#elementValuePair}.
	 * @param ctx the parse tree
	 */
	void enterElementValuePair(RefactorMethodSignatureParser.ElementValuePairContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#elementValuePair}.
	 * @param ctx the parse tree
	 */
	void exitElementValuePair(RefactorMethodSignatureParser.ElementValuePairContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#elementValue}.
	 * @param ctx the parse tree
	 */
	void enterElementValue(RefactorMethodSignatureParser.ElementValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#elementValue}.
	 * @param ctx the parse tree
	 */
	void exitElementValue(RefactorMethodSignatureParser.ElementValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#elementValueArrayInitializer}.
	 * @param ctx the parse tree
	 */
	void enterElementValueArrayInitializer(RefactorMethodSignatureParser.ElementValueArrayInitializerContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#elementValueArrayInitializer}.
	 * @param ctx the parse tree
	 */
	void exitElementValueArrayInitializer(RefactorMethodSignatureParser.ElementValueArrayInitializerContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#annotationTypeDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationTypeDeclaration(RefactorMethodSignatureParser.AnnotationTypeDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#annotationTypeDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationTypeDeclaration(RefactorMethodSignatureParser.AnnotationTypeDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#annotationTypeBody}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationTypeBody(RefactorMethodSignatureParser.AnnotationTypeBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#annotationTypeBody}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationTypeBody(RefactorMethodSignatureParser.AnnotationTypeBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#annotationTypeElementDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationTypeElementDeclaration(RefactorMethodSignatureParser.AnnotationTypeElementDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#annotationTypeElementDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationTypeElementDeclaration(RefactorMethodSignatureParser.AnnotationTypeElementDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#annotationTypeElementRest}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationTypeElementRest(RefactorMethodSignatureParser.AnnotationTypeElementRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#annotationTypeElementRest}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationTypeElementRest(RefactorMethodSignatureParser.AnnotationTypeElementRestContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#annotationMethodOrConstantRest}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationMethodOrConstantRest(RefactorMethodSignatureParser.AnnotationMethodOrConstantRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#annotationMethodOrConstantRest}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationMethodOrConstantRest(RefactorMethodSignatureParser.AnnotationMethodOrConstantRestContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#annotationMethodRest}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationMethodRest(RefactorMethodSignatureParser.AnnotationMethodRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#annotationMethodRest}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationMethodRest(RefactorMethodSignatureParser.AnnotationMethodRestContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#annotationConstantRest}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationConstantRest(RefactorMethodSignatureParser.AnnotationConstantRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#annotationConstantRest}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationConstantRest(RefactorMethodSignatureParser.AnnotationConstantRestContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#defaultValue}.
	 * @param ctx the parse tree
	 */
	void enterDefaultValue(RefactorMethodSignatureParser.DefaultValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#defaultValue}.
	 * @param ctx the parse tree
	 */
	void exitDefaultValue(RefactorMethodSignatureParser.DefaultValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#block}.
	 * @param ctx the parse tree
	 */
	void enterBlock(RefactorMethodSignatureParser.BlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#block}.
	 * @param ctx the parse tree
	 */
	void exitBlock(RefactorMethodSignatureParser.BlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#blockStatement}.
	 * @param ctx the parse tree
	 */
	void enterBlockStatement(RefactorMethodSignatureParser.BlockStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#blockStatement}.
	 * @param ctx the parse tree
	 */
	void exitBlockStatement(RefactorMethodSignatureParser.BlockStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#localVariableDeclarationStatement}.
	 * @param ctx the parse tree
	 */
	void enterLocalVariableDeclarationStatement(RefactorMethodSignatureParser.LocalVariableDeclarationStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#localVariableDeclarationStatement}.
	 * @param ctx the parse tree
	 */
	void exitLocalVariableDeclarationStatement(RefactorMethodSignatureParser.LocalVariableDeclarationStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#localVariableDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterLocalVariableDeclaration(RefactorMethodSignatureParser.LocalVariableDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#localVariableDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitLocalVariableDeclaration(RefactorMethodSignatureParser.LocalVariableDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(RefactorMethodSignatureParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(RefactorMethodSignatureParser.StatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#catchClause}.
	 * @param ctx the parse tree
	 */
	void enterCatchClause(RefactorMethodSignatureParser.CatchClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#catchClause}.
	 * @param ctx the parse tree
	 */
	void exitCatchClause(RefactorMethodSignatureParser.CatchClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#catchType}.
	 * @param ctx the parse tree
	 */
	void enterCatchType(RefactorMethodSignatureParser.CatchTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#catchType}.
	 * @param ctx the parse tree
	 */
	void exitCatchType(RefactorMethodSignatureParser.CatchTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#finallyBlock}.
	 * @param ctx the parse tree
	 */
	void enterFinallyBlock(RefactorMethodSignatureParser.FinallyBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#finallyBlock}.
	 * @param ctx the parse tree
	 */
	void exitFinallyBlock(RefactorMethodSignatureParser.FinallyBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#resourceSpecification}.
	 * @param ctx the parse tree
	 */
	void enterResourceSpecification(RefactorMethodSignatureParser.ResourceSpecificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#resourceSpecification}.
	 * @param ctx the parse tree
	 */
	void exitResourceSpecification(RefactorMethodSignatureParser.ResourceSpecificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#resources}.
	 * @param ctx the parse tree
	 */
	void enterResources(RefactorMethodSignatureParser.ResourcesContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#resources}.
	 * @param ctx the parse tree
	 */
	void exitResources(RefactorMethodSignatureParser.ResourcesContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#resource}.
	 * @param ctx the parse tree
	 */
	void enterResource(RefactorMethodSignatureParser.ResourceContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#resource}.
	 * @param ctx the parse tree
	 */
	void exitResource(RefactorMethodSignatureParser.ResourceContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#switchBlockStatementGroup}.
	 * @param ctx the parse tree
	 */
	void enterSwitchBlockStatementGroup(RefactorMethodSignatureParser.SwitchBlockStatementGroupContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#switchBlockStatementGroup}.
	 * @param ctx the parse tree
	 */
	void exitSwitchBlockStatementGroup(RefactorMethodSignatureParser.SwitchBlockStatementGroupContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#switchLabel}.
	 * @param ctx the parse tree
	 */
	void enterSwitchLabel(RefactorMethodSignatureParser.SwitchLabelContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#switchLabel}.
	 * @param ctx the parse tree
	 */
	void exitSwitchLabel(RefactorMethodSignatureParser.SwitchLabelContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#forControl}.
	 * @param ctx the parse tree
	 */
	void enterForControl(RefactorMethodSignatureParser.ForControlContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#forControl}.
	 * @param ctx the parse tree
	 */
	void exitForControl(RefactorMethodSignatureParser.ForControlContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#forInit}.
	 * @param ctx the parse tree
	 */
	void enterForInit(RefactorMethodSignatureParser.ForInitContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#forInit}.
	 * @param ctx the parse tree
	 */
	void exitForInit(RefactorMethodSignatureParser.ForInitContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#enhancedForControl}.
	 * @param ctx the parse tree
	 */
	void enterEnhancedForControl(RefactorMethodSignatureParser.EnhancedForControlContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#enhancedForControl}.
	 * @param ctx the parse tree
	 */
	void exitEnhancedForControl(RefactorMethodSignatureParser.EnhancedForControlContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#forUpdate}.
	 * @param ctx the parse tree
	 */
	void enterForUpdate(RefactorMethodSignatureParser.ForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#forUpdate}.
	 * @param ctx the parse tree
	 */
	void exitForUpdate(RefactorMethodSignatureParser.ForUpdateContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#parExpression}.
	 * @param ctx the parse tree
	 */
	void enterParExpression(RefactorMethodSignatureParser.ParExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#parExpression}.
	 * @param ctx the parse tree
	 */
	void exitParExpression(RefactorMethodSignatureParser.ParExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#expressionList}.
	 * @param ctx the parse tree
	 */
	void enterExpressionList(RefactorMethodSignatureParser.ExpressionListContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#expressionList}.
	 * @param ctx the parse tree
	 */
	void exitExpressionList(RefactorMethodSignatureParser.ExpressionListContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#statementExpression}.
	 * @param ctx the parse tree
	 */
	void enterStatementExpression(RefactorMethodSignatureParser.StatementExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#statementExpression}.
	 * @param ctx the parse tree
	 */
	void exitStatementExpression(RefactorMethodSignatureParser.StatementExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#constantExpression}.
	 * @param ctx the parse tree
	 */
	void enterConstantExpression(RefactorMethodSignatureParser.ConstantExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#constantExpression}.
	 * @param ctx the parse tree
	 */
	void exitConstantExpression(RefactorMethodSignatureParser.ConstantExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(RefactorMethodSignatureParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(RefactorMethodSignatureParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#primary}.
	 * @param ctx the parse tree
	 */
	void enterPrimary(RefactorMethodSignatureParser.PrimaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#primary}.
	 * @param ctx the parse tree
	 */
	void exitPrimary(RefactorMethodSignatureParser.PrimaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#creator}.
	 * @param ctx the parse tree
	 */
	void enterCreator(RefactorMethodSignatureParser.CreatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#creator}.
	 * @param ctx the parse tree
	 */
	void exitCreator(RefactorMethodSignatureParser.CreatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#createdName}.
	 * @param ctx the parse tree
	 */
	void enterCreatedName(RefactorMethodSignatureParser.CreatedNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#createdName}.
	 * @param ctx the parse tree
	 */
	void exitCreatedName(RefactorMethodSignatureParser.CreatedNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#innerCreator}.
	 * @param ctx the parse tree
	 */
	void enterInnerCreator(RefactorMethodSignatureParser.InnerCreatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#innerCreator}.
	 * @param ctx the parse tree
	 */
	void exitInnerCreator(RefactorMethodSignatureParser.InnerCreatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#arrayCreatorRest}.
	 * @param ctx the parse tree
	 */
	void enterArrayCreatorRest(RefactorMethodSignatureParser.ArrayCreatorRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#arrayCreatorRest}.
	 * @param ctx the parse tree
	 */
	void exitArrayCreatorRest(RefactorMethodSignatureParser.ArrayCreatorRestContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#classCreatorRest}.
	 * @param ctx the parse tree
	 */
	void enterClassCreatorRest(RefactorMethodSignatureParser.ClassCreatorRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#classCreatorRest}.
	 * @param ctx the parse tree
	 */
	void exitClassCreatorRest(RefactorMethodSignatureParser.ClassCreatorRestContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#explicitGenericInvocation}.
	 * @param ctx the parse tree
	 */
	void enterExplicitGenericInvocation(RefactorMethodSignatureParser.ExplicitGenericInvocationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#explicitGenericInvocation}.
	 * @param ctx the parse tree
	 */
	void exitExplicitGenericInvocation(RefactorMethodSignatureParser.ExplicitGenericInvocationContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#nonWildcardTypeArguments}.
	 * @param ctx the parse tree
	 */
	void enterNonWildcardTypeArguments(RefactorMethodSignatureParser.NonWildcardTypeArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#nonWildcardTypeArguments}.
	 * @param ctx the parse tree
	 */
	void exitNonWildcardTypeArguments(RefactorMethodSignatureParser.NonWildcardTypeArgumentsContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#typeArgumentsOrDiamond}.
	 * @param ctx the parse tree
	 */
	void enterTypeArgumentsOrDiamond(RefactorMethodSignatureParser.TypeArgumentsOrDiamondContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#typeArgumentsOrDiamond}.
	 * @param ctx the parse tree
	 */
	void exitTypeArgumentsOrDiamond(RefactorMethodSignatureParser.TypeArgumentsOrDiamondContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#nonWildcardTypeArgumentsOrDiamond}.
	 * @param ctx the parse tree
	 */
	void enterNonWildcardTypeArgumentsOrDiamond(RefactorMethodSignatureParser.NonWildcardTypeArgumentsOrDiamondContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#nonWildcardTypeArgumentsOrDiamond}.
	 * @param ctx the parse tree
	 */
	void exitNonWildcardTypeArgumentsOrDiamond(RefactorMethodSignatureParser.NonWildcardTypeArgumentsOrDiamondContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#superSuffix}.
	 * @param ctx the parse tree
	 */
	void enterSuperSuffix(RefactorMethodSignatureParser.SuperSuffixContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#superSuffix}.
	 * @param ctx the parse tree
	 */
	void exitSuperSuffix(RefactorMethodSignatureParser.SuperSuffixContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#explicitGenericInvocationSuffix}.
	 * @param ctx the parse tree
	 */
	void enterExplicitGenericInvocationSuffix(RefactorMethodSignatureParser.ExplicitGenericInvocationSuffixContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#explicitGenericInvocationSuffix}.
	 * @param ctx the parse tree
	 */
	void exitExplicitGenericInvocationSuffix(RefactorMethodSignatureParser.ExplicitGenericInvocationSuffixContext ctx);
	/**
	 * Enter a parse tree produced by {@link RefactorMethodSignatureParser#arguments}.
	 * @param ctx the parse tree
	 */
	void enterArguments(RefactorMethodSignatureParser.ArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link RefactorMethodSignatureParser#arguments}.
	 * @param ctx the parse tree
	 */
	void exitArguments(RefactorMethodSignatureParser.ArgumentsContext ctx);
}