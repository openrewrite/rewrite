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
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link AnnotationSignatureParser}.
 */
public interface AnnotationSignatureParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#annotation}.
	 * @param ctx the parse tree
	 */
	void enterAnnotation(AnnotationSignatureParser.AnnotationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#annotation}.
	 * @param ctx the parse tree
	 */
	void exitAnnotation(AnnotationSignatureParser.AnnotationContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#compilationUnit}.
	 * @param ctx the parse tree
	 */
	void enterCompilationUnit(AnnotationSignatureParser.CompilationUnitContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#compilationUnit}.
	 * @param ctx the parse tree
	 */
	void exitCompilationUnit(AnnotationSignatureParser.CompilationUnitContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#packageDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterPackageDeclaration(AnnotationSignatureParser.PackageDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#packageDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitPackageDeclaration(AnnotationSignatureParser.PackageDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#importDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterImportDeclaration(AnnotationSignatureParser.ImportDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#importDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitImportDeclaration(AnnotationSignatureParser.ImportDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#typeDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterTypeDeclaration(AnnotationSignatureParser.TypeDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#typeDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitTypeDeclaration(AnnotationSignatureParser.TypeDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#modifier}.
	 * @param ctx the parse tree
	 */
	void enterModifier(AnnotationSignatureParser.ModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#modifier}.
	 * @param ctx the parse tree
	 */
	void exitModifier(AnnotationSignatureParser.ModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#classOrInterfaceModifier}.
	 * @param ctx the parse tree
	 */
	void enterClassOrInterfaceModifier(AnnotationSignatureParser.ClassOrInterfaceModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#classOrInterfaceModifier}.
	 * @param ctx the parse tree
	 */
	void exitClassOrInterfaceModifier(AnnotationSignatureParser.ClassOrInterfaceModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#variableModifier}.
	 * @param ctx the parse tree
	 */
	void enterVariableModifier(AnnotationSignatureParser.VariableModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#variableModifier}.
	 * @param ctx the parse tree
	 */
	void exitVariableModifier(AnnotationSignatureParser.VariableModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#classDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterClassDeclaration(AnnotationSignatureParser.ClassDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#classDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitClassDeclaration(AnnotationSignatureParser.ClassDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#typeParameters}.
	 * @param ctx the parse tree
	 */
	void enterTypeParameters(AnnotationSignatureParser.TypeParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#typeParameters}.
	 * @param ctx the parse tree
	 */
	void exitTypeParameters(AnnotationSignatureParser.TypeParametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#typeParameter}.
	 * @param ctx the parse tree
	 */
	void enterTypeParameter(AnnotationSignatureParser.TypeParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#typeParameter}.
	 * @param ctx the parse tree
	 */
	void exitTypeParameter(AnnotationSignatureParser.TypeParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#typeBound}.
	 * @param ctx the parse tree
	 */
	void enterTypeBound(AnnotationSignatureParser.TypeBoundContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#typeBound}.
	 * @param ctx the parse tree
	 */
	void exitTypeBound(AnnotationSignatureParser.TypeBoundContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#enumDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterEnumDeclaration(AnnotationSignatureParser.EnumDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#enumDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitEnumDeclaration(AnnotationSignatureParser.EnumDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#enumConstants}.
	 * @param ctx the parse tree
	 */
	void enterEnumConstants(AnnotationSignatureParser.EnumConstantsContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#enumConstants}.
	 * @param ctx the parse tree
	 */
	void exitEnumConstants(AnnotationSignatureParser.EnumConstantsContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#enumConstant}.
	 * @param ctx the parse tree
	 */
	void enterEnumConstant(AnnotationSignatureParser.EnumConstantContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#enumConstant}.
	 * @param ctx the parse tree
	 */
	void exitEnumConstant(AnnotationSignatureParser.EnumConstantContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#enumBodyDeclarations}.
	 * @param ctx the parse tree
	 */
	void enterEnumBodyDeclarations(AnnotationSignatureParser.EnumBodyDeclarationsContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#enumBodyDeclarations}.
	 * @param ctx the parse tree
	 */
	void exitEnumBodyDeclarations(AnnotationSignatureParser.EnumBodyDeclarationsContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#interfaceDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceDeclaration(AnnotationSignatureParser.InterfaceDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#interfaceDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceDeclaration(AnnotationSignatureParser.InterfaceDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#typeList}.
	 * @param ctx the parse tree
	 */
	void enterTypeList(AnnotationSignatureParser.TypeListContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#typeList}.
	 * @param ctx the parse tree
	 */
	void exitTypeList(AnnotationSignatureParser.TypeListContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#classBody}.
	 * @param ctx the parse tree
	 */
	void enterClassBody(AnnotationSignatureParser.ClassBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#classBody}.
	 * @param ctx the parse tree
	 */
	void exitClassBody(AnnotationSignatureParser.ClassBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#interfaceBody}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceBody(AnnotationSignatureParser.InterfaceBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#interfaceBody}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceBody(AnnotationSignatureParser.InterfaceBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#classBodyDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterClassBodyDeclaration(AnnotationSignatureParser.ClassBodyDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#classBodyDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitClassBodyDeclaration(AnnotationSignatureParser.ClassBodyDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#memberDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterMemberDeclaration(AnnotationSignatureParser.MemberDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#memberDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitMemberDeclaration(AnnotationSignatureParser.MemberDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#methodDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterMethodDeclaration(AnnotationSignatureParser.MethodDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#methodDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitMethodDeclaration(AnnotationSignatureParser.MethodDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#genericMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterGenericMethodDeclaration(AnnotationSignatureParser.GenericMethodDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#genericMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitGenericMethodDeclaration(AnnotationSignatureParser.GenericMethodDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#constructorDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterConstructorDeclaration(AnnotationSignatureParser.ConstructorDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#constructorDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitConstructorDeclaration(AnnotationSignatureParser.ConstructorDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#genericConstructorDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterGenericConstructorDeclaration(AnnotationSignatureParser.GenericConstructorDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#genericConstructorDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitGenericConstructorDeclaration(AnnotationSignatureParser.GenericConstructorDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#fieldDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterFieldDeclaration(AnnotationSignatureParser.FieldDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#fieldDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitFieldDeclaration(AnnotationSignatureParser.FieldDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#interfaceBodyDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceBodyDeclaration(AnnotationSignatureParser.InterfaceBodyDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#interfaceBodyDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceBodyDeclaration(AnnotationSignatureParser.InterfaceBodyDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#interfaceMemberDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceMemberDeclaration(AnnotationSignatureParser.InterfaceMemberDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#interfaceMemberDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceMemberDeclaration(AnnotationSignatureParser.InterfaceMemberDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#constDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterConstDeclaration(AnnotationSignatureParser.ConstDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#constDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitConstDeclaration(AnnotationSignatureParser.ConstDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#constantDeclarator}.
	 * @param ctx the parse tree
	 */
	void enterConstantDeclarator(AnnotationSignatureParser.ConstantDeclaratorContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#constantDeclarator}.
	 * @param ctx the parse tree
	 */
	void exitConstantDeclarator(AnnotationSignatureParser.ConstantDeclaratorContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#interfaceMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceMethodDeclaration(AnnotationSignatureParser.InterfaceMethodDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#interfaceMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceMethodDeclaration(AnnotationSignatureParser.InterfaceMethodDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#genericInterfaceMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterGenericInterfaceMethodDeclaration(AnnotationSignatureParser.GenericInterfaceMethodDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#genericInterfaceMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitGenericInterfaceMethodDeclaration(AnnotationSignatureParser.GenericInterfaceMethodDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#variableDeclarators}.
	 * @param ctx the parse tree
	 */
	void enterVariableDeclarators(AnnotationSignatureParser.VariableDeclaratorsContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#variableDeclarators}.
	 * @param ctx the parse tree
	 */
	void exitVariableDeclarators(AnnotationSignatureParser.VariableDeclaratorsContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#variableDeclarator}.
	 * @param ctx the parse tree
	 */
	void enterVariableDeclarator(AnnotationSignatureParser.VariableDeclaratorContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#variableDeclarator}.
	 * @param ctx the parse tree
	 */
	void exitVariableDeclarator(AnnotationSignatureParser.VariableDeclaratorContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#variableDeclaratorId}.
	 * @param ctx the parse tree
	 */
	void enterVariableDeclaratorId(AnnotationSignatureParser.VariableDeclaratorIdContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#variableDeclaratorId}.
	 * @param ctx the parse tree
	 */
	void exitVariableDeclaratorId(AnnotationSignatureParser.VariableDeclaratorIdContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#variableInitializer}.
	 * @param ctx the parse tree
	 */
	void enterVariableInitializer(AnnotationSignatureParser.VariableInitializerContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#variableInitializer}.
	 * @param ctx the parse tree
	 */
	void exitVariableInitializer(AnnotationSignatureParser.VariableInitializerContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#arrayInitializer}.
	 * @param ctx the parse tree
	 */
	void enterArrayInitializer(AnnotationSignatureParser.ArrayInitializerContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#arrayInitializer}.
	 * @param ctx the parse tree
	 */
	void exitArrayInitializer(AnnotationSignatureParser.ArrayInitializerContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#enumConstantName}.
	 * @param ctx the parse tree
	 */
	void enterEnumConstantName(AnnotationSignatureParser.EnumConstantNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#enumConstantName}.
	 * @param ctx the parse tree
	 */
	void exitEnumConstantName(AnnotationSignatureParser.EnumConstantNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(AnnotationSignatureParser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(AnnotationSignatureParser.TypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#classOrInterfaceType}.
	 * @param ctx the parse tree
	 */
	void enterClassOrInterfaceType(AnnotationSignatureParser.ClassOrInterfaceTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#classOrInterfaceType}.
	 * @param ctx the parse tree
	 */
	void exitClassOrInterfaceType(AnnotationSignatureParser.ClassOrInterfaceTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#primitiveType}.
	 * @param ctx the parse tree
	 */
	void enterPrimitiveType(AnnotationSignatureParser.PrimitiveTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#primitiveType}.
	 * @param ctx the parse tree
	 */
	void exitPrimitiveType(AnnotationSignatureParser.PrimitiveTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#typeArguments}.
	 * @param ctx the parse tree
	 */
	void enterTypeArguments(AnnotationSignatureParser.TypeArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#typeArguments}.
	 * @param ctx the parse tree
	 */
	void exitTypeArguments(AnnotationSignatureParser.TypeArgumentsContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#typeArgument}.
	 * @param ctx the parse tree
	 */
	void enterTypeArgument(AnnotationSignatureParser.TypeArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#typeArgument}.
	 * @param ctx the parse tree
	 */
	void exitTypeArgument(AnnotationSignatureParser.TypeArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#qualifiedNameList}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedNameList(AnnotationSignatureParser.QualifiedNameListContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#qualifiedNameList}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedNameList(AnnotationSignatureParser.QualifiedNameListContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#formalParameters}.
	 * @param ctx the parse tree
	 */
	void enterFormalParameters(AnnotationSignatureParser.FormalParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#formalParameters}.
	 * @param ctx the parse tree
	 */
	void exitFormalParameters(AnnotationSignatureParser.FormalParametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#formalParameterList}.
	 * @param ctx the parse tree
	 */
	void enterFormalParameterList(AnnotationSignatureParser.FormalParameterListContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#formalParameterList}.
	 * @param ctx the parse tree
	 */
	void exitFormalParameterList(AnnotationSignatureParser.FormalParameterListContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#formalParameter}.
	 * @param ctx the parse tree
	 */
	void enterFormalParameter(AnnotationSignatureParser.FormalParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#formalParameter}.
	 * @param ctx the parse tree
	 */
	void exitFormalParameter(AnnotationSignatureParser.FormalParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#lastFormalParameter}.
	 * @param ctx the parse tree
	 */
	void enterLastFormalParameter(AnnotationSignatureParser.LastFormalParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#lastFormalParameter}.
	 * @param ctx the parse tree
	 */
	void exitLastFormalParameter(AnnotationSignatureParser.LastFormalParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#methodBody}.
	 * @param ctx the parse tree
	 */
	void enterMethodBody(AnnotationSignatureParser.MethodBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#methodBody}.
	 * @param ctx the parse tree
	 */
	void exitMethodBody(AnnotationSignatureParser.MethodBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#constructorBody}.
	 * @param ctx the parse tree
	 */
	void enterConstructorBody(AnnotationSignatureParser.ConstructorBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#constructorBody}.
	 * @param ctx the parse tree
	 */
	void exitConstructorBody(AnnotationSignatureParser.ConstructorBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedName(AnnotationSignatureParser.QualifiedNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedName(AnnotationSignatureParser.QualifiedNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLiteral(AnnotationSignatureParser.LiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLiteral(AnnotationSignatureParser.LiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#annotationName}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationName(AnnotationSignatureParser.AnnotationNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#annotationName}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationName(AnnotationSignatureParser.AnnotationNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#elementValuePairs}.
	 * @param ctx the parse tree
	 */
	void enterElementValuePairs(AnnotationSignatureParser.ElementValuePairsContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#elementValuePairs}.
	 * @param ctx the parse tree
	 */
	void exitElementValuePairs(AnnotationSignatureParser.ElementValuePairsContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#elementValuePair}.
	 * @param ctx the parse tree
	 */
	void enterElementValuePair(AnnotationSignatureParser.ElementValuePairContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#elementValuePair}.
	 * @param ctx the parse tree
	 */
	void exitElementValuePair(AnnotationSignatureParser.ElementValuePairContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#elementValue}.
	 * @param ctx the parse tree
	 */
	void enterElementValue(AnnotationSignatureParser.ElementValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#elementValue}.
	 * @param ctx the parse tree
	 */
	void exitElementValue(AnnotationSignatureParser.ElementValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#elementValueArrayInitializer}.
	 * @param ctx the parse tree
	 */
	void enterElementValueArrayInitializer(AnnotationSignatureParser.ElementValueArrayInitializerContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#elementValueArrayInitializer}.
	 * @param ctx the parse tree
	 */
	void exitElementValueArrayInitializer(AnnotationSignatureParser.ElementValueArrayInitializerContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#annotationTypeDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationTypeDeclaration(AnnotationSignatureParser.AnnotationTypeDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#annotationTypeDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationTypeDeclaration(AnnotationSignatureParser.AnnotationTypeDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#annotationTypeBody}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationTypeBody(AnnotationSignatureParser.AnnotationTypeBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#annotationTypeBody}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationTypeBody(AnnotationSignatureParser.AnnotationTypeBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#annotationTypeElementDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationTypeElementDeclaration(AnnotationSignatureParser.AnnotationTypeElementDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#annotationTypeElementDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationTypeElementDeclaration(AnnotationSignatureParser.AnnotationTypeElementDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#annotationTypeElementRest}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationTypeElementRest(AnnotationSignatureParser.AnnotationTypeElementRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#annotationTypeElementRest}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationTypeElementRest(AnnotationSignatureParser.AnnotationTypeElementRestContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#annotationMethodOrConstantRest}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationMethodOrConstantRest(AnnotationSignatureParser.AnnotationMethodOrConstantRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#annotationMethodOrConstantRest}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationMethodOrConstantRest(AnnotationSignatureParser.AnnotationMethodOrConstantRestContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#annotationMethodRest}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationMethodRest(AnnotationSignatureParser.AnnotationMethodRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#annotationMethodRest}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationMethodRest(AnnotationSignatureParser.AnnotationMethodRestContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#annotationConstantRest}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationConstantRest(AnnotationSignatureParser.AnnotationConstantRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#annotationConstantRest}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationConstantRest(AnnotationSignatureParser.AnnotationConstantRestContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#defaultValue}.
	 * @param ctx the parse tree
	 */
	void enterDefaultValue(AnnotationSignatureParser.DefaultValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#defaultValue}.
	 * @param ctx the parse tree
	 */
	void exitDefaultValue(AnnotationSignatureParser.DefaultValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#block}.
	 * @param ctx the parse tree
	 */
	void enterBlock(AnnotationSignatureParser.BlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#block}.
	 * @param ctx the parse tree
	 */
	void exitBlock(AnnotationSignatureParser.BlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#blockStatement}.
	 * @param ctx the parse tree
	 */
	void enterBlockStatement(AnnotationSignatureParser.BlockStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#blockStatement}.
	 * @param ctx the parse tree
	 */
	void exitBlockStatement(AnnotationSignatureParser.BlockStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#localVariableDeclarationStatement}.
	 * @param ctx the parse tree
	 */
	void enterLocalVariableDeclarationStatement(AnnotationSignatureParser.LocalVariableDeclarationStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#localVariableDeclarationStatement}.
	 * @param ctx the parse tree
	 */
	void exitLocalVariableDeclarationStatement(AnnotationSignatureParser.LocalVariableDeclarationStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#localVariableDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterLocalVariableDeclaration(AnnotationSignatureParser.LocalVariableDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#localVariableDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitLocalVariableDeclaration(AnnotationSignatureParser.LocalVariableDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(AnnotationSignatureParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(AnnotationSignatureParser.StatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#catchClause}.
	 * @param ctx the parse tree
	 */
	void enterCatchClause(AnnotationSignatureParser.CatchClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#catchClause}.
	 * @param ctx the parse tree
	 */
	void exitCatchClause(AnnotationSignatureParser.CatchClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#catchType}.
	 * @param ctx the parse tree
	 */
	void enterCatchType(AnnotationSignatureParser.CatchTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#catchType}.
	 * @param ctx the parse tree
	 */
	void exitCatchType(AnnotationSignatureParser.CatchTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#finallyBlock}.
	 * @param ctx the parse tree
	 */
	void enterFinallyBlock(AnnotationSignatureParser.FinallyBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#finallyBlock}.
	 * @param ctx the parse tree
	 */
	void exitFinallyBlock(AnnotationSignatureParser.FinallyBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#resourceSpecification}.
	 * @param ctx the parse tree
	 */
	void enterResourceSpecification(AnnotationSignatureParser.ResourceSpecificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#resourceSpecification}.
	 * @param ctx the parse tree
	 */
	void exitResourceSpecification(AnnotationSignatureParser.ResourceSpecificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#resources}.
	 * @param ctx the parse tree
	 */
	void enterResources(AnnotationSignatureParser.ResourcesContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#resources}.
	 * @param ctx the parse tree
	 */
	void exitResources(AnnotationSignatureParser.ResourcesContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#resource}.
	 * @param ctx the parse tree
	 */
	void enterResource(AnnotationSignatureParser.ResourceContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#resource}.
	 * @param ctx the parse tree
	 */
	void exitResource(AnnotationSignatureParser.ResourceContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#switchBlockStatementGroup}.
	 * @param ctx the parse tree
	 */
	void enterSwitchBlockStatementGroup(AnnotationSignatureParser.SwitchBlockStatementGroupContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#switchBlockStatementGroup}.
	 * @param ctx the parse tree
	 */
	void exitSwitchBlockStatementGroup(AnnotationSignatureParser.SwitchBlockStatementGroupContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#switchLabel}.
	 * @param ctx the parse tree
	 */
	void enterSwitchLabel(AnnotationSignatureParser.SwitchLabelContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#switchLabel}.
	 * @param ctx the parse tree
	 */
	void exitSwitchLabel(AnnotationSignatureParser.SwitchLabelContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#forControl}.
	 * @param ctx the parse tree
	 */
	void enterForControl(AnnotationSignatureParser.ForControlContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#forControl}.
	 * @param ctx the parse tree
	 */
	void exitForControl(AnnotationSignatureParser.ForControlContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#forInit}.
	 * @param ctx the parse tree
	 */
	void enterForInit(AnnotationSignatureParser.ForInitContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#forInit}.
	 * @param ctx the parse tree
	 */
	void exitForInit(AnnotationSignatureParser.ForInitContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#enhancedForControl}.
	 * @param ctx the parse tree
	 */
	void enterEnhancedForControl(AnnotationSignatureParser.EnhancedForControlContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#enhancedForControl}.
	 * @param ctx the parse tree
	 */
	void exitEnhancedForControl(AnnotationSignatureParser.EnhancedForControlContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#forUpdate}.
	 * @param ctx the parse tree
	 */
	void enterForUpdate(AnnotationSignatureParser.ForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#forUpdate}.
	 * @param ctx the parse tree
	 */
	void exitForUpdate(AnnotationSignatureParser.ForUpdateContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#parExpression}.
	 * @param ctx the parse tree
	 */
	void enterParExpression(AnnotationSignatureParser.ParExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#parExpression}.
	 * @param ctx the parse tree
	 */
	void exitParExpression(AnnotationSignatureParser.ParExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#expressionList}.
	 * @param ctx the parse tree
	 */
	void enterExpressionList(AnnotationSignatureParser.ExpressionListContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#expressionList}.
	 * @param ctx the parse tree
	 */
	void exitExpressionList(AnnotationSignatureParser.ExpressionListContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#statementExpression}.
	 * @param ctx the parse tree
	 */
	void enterStatementExpression(AnnotationSignatureParser.StatementExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#statementExpression}.
	 * @param ctx the parse tree
	 */
	void exitStatementExpression(AnnotationSignatureParser.StatementExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#constantExpression}.
	 * @param ctx the parse tree
	 */
	void enterConstantExpression(AnnotationSignatureParser.ConstantExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#constantExpression}.
	 * @param ctx the parse tree
	 */
	void exitConstantExpression(AnnotationSignatureParser.ConstantExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(AnnotationSignatureParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(AnnotationSignatureParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#primary}.
	 * @param ctx the parse tree
	 */
	void enterPrimary(AnnotationSignatureParser.PrimaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#primary}.
	 * @param ctx the parse tree
	 */
	void exitPrimary(AnnotationSignatureParser.PrimaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#creator}.
	 * @param ctx the parse tree
	 */
	void enterCreator(AnnotationSignatureParser.CreatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#creator}.
	 * @param ctx the parse tree
	 */
	void exitCreator(AnnotationSignatureParser.CreatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#createdName}.
	 * @param ctx the parse tree
	 */
	void enterCreatedName(AnnotationSignatureParser.CreatedNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#createdName}.
	 * @param ctx the parse tree
	 */
	void exitCreatedName(AnnotationSignatureParser.CreatedNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#innerCreator}.
	 * @param ctx the parse tree
	 */
	void enterInnerCreator(AnnotationSignatureParser.InnerCreatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#innerCreator}.
	 * @param ctx the parse tree
	 */
	void exitInnerCreator(AnnotationSignatureParser.InnerCreatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#arrayCreatorRest}.
	 * @param ctx the parse tree
	 */
	void enterArrayCreatorRest(AnnotationSignatureParser.ArrayCreatorRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#arrayCreatorRest}.
	 * @param ctx the parse tree
	 */
	void exitArrayCreatorRest(AnnotationSignatureParser.ArrayCreatorRestContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#classCreatorRest}.
	 * @param ctx the parse tree
	 */
	void enterClassCreatorRest(AnnotationSignatureParser.ClassCreatorRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#classCreatorRest}.
	 * @param ctx the parse tree
	 */
	void exitClassCreatorRest(AnnotationSignatureParser.ClassCreatorRestContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#explicitGenericInvocation}.
	 * @param ctx the parse tree
	 */
	void enterExplicitGenericInvocation(AnnotationSignatureParser.ExplicitGenericInvocationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#explicitGenericInvocation}.
	 * @param ctx the parse tree
	 */
	void exitExplicitGenericInvocation(AnnotationSignatureParser.ExplicitGenericInvocationContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#nonWildcardTypeArguments}.
	 * @param ctx the parse tree
	 */
	void enterNonWildcardTypeArguments(AnnotationSignatureParser.NonWildcardTypeArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#nonWildcardTypeArguments}.
	 * @param ctx the parse tree
	 */
	void exitNonWildcardTypeArguments(AnnotationSignatureParser.NonWildcardTypeArgumentsContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#typeArgumentsOrDiamond}.
	 * @param ctx the parse tree
	 */
	void enterTypeArgumentsOrDiamond(AnnotationSignatureParser.TypeArgumentsOrDiamondContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#typeArgumentsOrDiamond}.
	 * @param ctx the parse tree
	 */
	void exitTypeArgumentsOrDiamond(AnnotationSignatureParser.TypeArgumentsOrDiamondContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#nonWildcardTypeArgumentsOrDiamond}.
	 * @param ctx the parse tree
	 */
	void enterNonWildcardTypeArgumentsOrDiamond(AnnotationSignatureParser.NonWildcardTypeArgumentsOrDiamondContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#nonWildcardTypeArgumentsOrDiamond}.
	 * @param ctx the parse tree
	 */
	void exitNonWildcardTypeArgumentsOrDiamond(AnnotationSignatureParser.NonWildcardTypeArgumentsOrDiamondContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#superSuffix}.
	 * @param ctx the parse tree
	 */
	void enterSuperSuffix(AnnotationSignatureParser.SuperSuffixContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#superSuffix}.
	 * @param ctx the parse tree
	 */
	void exitSuperSuffix(AnnotationSignatureParser.SuperSuffixContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#explicitGenericInvocationSuffix}.
	 * @param ctx the parse tree
	 */
	void enterExplicitGenericInvocationSuffix(AnnotationSignatureParser.ExplicitGenericInvocationSuffixContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#explicitGenericInvocationSuffix}.
	 * @param ctx the parse tree
	 */
	void exitExplicitGenericInvocationSuffix(AnnotationSignatureParser.ExplicitGenericInvocationSuffixContext ctx);
	/**
	 * Enter a parse tree produced by {@link AnnotationSignatureParser#arguments}.
	 * @param ctx the parse tree
	 */
	void enterArguments(AnnotationSignatureParser.ArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link AnnotationSignatureParser#arguments}.
	 * @param ctx the parse tree
	 */
	void exitArguments(AnnotationSignatureParser.ArgumentsContext ctx);
}