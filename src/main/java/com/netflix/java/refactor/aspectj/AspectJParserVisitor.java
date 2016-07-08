// Generated from /Users/joschneider/Projects/github/jkschneider/java-source-linter/src/main/antlr4/AspectJParser.g4 by ANTLR 4.2.2
package com.netflix.java.refactor.aspectj;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link AspectJParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface AspectJParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link AspectJParser#classPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassPattern(@NotNull AspectJParser.ClassPatternContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#memberDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMemberDeclaration(@NotNull AspectJParser.MemberDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#GetPointcut}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGetPointcut(@NotNull AspectJParser.GetPointcutContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#defaultValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefaultValue(@NotNull AspectJParser.DefaultValueContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#annotationTypeElementDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationTypeElementDeclaration(@NotNull AspectJParser.AnnotationTypeElementDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#AdviceExecutionPointcut}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdviceExecutionPointcut(@NotNull AspectJParser.AdviceExecutionPointcutContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#AnnotationPointcut}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationPointcut(@NotNull AspectJParser.AnnotationPointcutContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#annotationPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationPattern(@NotNull AspectJParser.AnnotationPatternContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#aspectDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAspectDeclaration(@NotNull AspectJParser.AspectDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#interTypeDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterTypeDeclaration(@NotNull AspectJParser.InterTypeDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType(@NotNull AspectJParser.TypeContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#annotationTypeBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationTypeBody(@NotNull AspectJParser.AnnotationTypeBodyContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#ArgsPointcut}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgsPointcut(@NotNull AspectJParser.ArgsPointcutContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#genericInterfaceMethodDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenericInterfaceMethodDeclaration(@NotNull AspectJParser.GenericInterfaceMethodDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#annotationTypePattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationTypePattern(@NotNull AspectJParser.AnnotationTypePatternContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#classBodyDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassBodyDeclaration(@NotNull AspectJParser.ClassBodyDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#formalsPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormalsPattern(@NotNull AspectJParser.FormalsPatternContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlock(@NotNull AspectJParser.BlockContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#id}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitId(@NotNull AspectJParser.IdContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#enumBodyDeclarations}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumBodyDeclarations(@NotNull AspectJParser.EnumBodyDeclarationsContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#InitializationPointcut}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInitializationPointcut(@NotNull AspectJParser.InitializationPointcutContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#forUpdate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForUpdate(@NotNull AspectJParser.ForUpdateContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#constructorPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructorPattern(@NotNull AspectJParser.ConstructorPatternContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#optionalParensTypePattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOptionalParensTypePattern(@NotNull AspectJParser.OptionalParensTypePatternContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#classPatternList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassPatternList(@NotNull AspectJParser.ClassPatternListContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#enhancedForControl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnhancedForControl(@NotNull AspectJParser.EnhancedForControlContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#annotationConstantRest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationConstantRest(@NotNull AspectJParser.AnnotationConstantRestContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#methodOrConstructorPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethodOrConstructorPattern(@NotNull AspectJParser.MethodOrConstructorPatternContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#perClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPerClause(@NotNull AspectJParser.PerClauseContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#interTypeMemberDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterTypeMemberDeclaration(@NotNull AspectJParser.InterTypeMemberDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#explicitGenericInvocation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExplicitGenericInvocation(@NotNull AspectJParser.ExplicitGenericInvocationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#nonWildcardTypeArgumentsOrDiamond}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNonWildcardTypeArgumentsOrDiamond(@NotNull AspectJParser.NonWildcardTypeArgumentsOrDiamondContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#AnnotationWithinPointcut}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationWithinPointcut(@NotNull AspectJParser.AnnotationWithinPointcutContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#expressionList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionList(@NotNull AspectJParser.ExpressionListContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#formalParametersPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormalParametersPattern(@NotNull AspectJParser.FormalParametersPatternContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#annotationTypeElementRest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationTypeElementRest(@NotNull AspectJParser.AnnotationTypeElementRestContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#classOrInterfaceType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassOrInterfaceType(@NotNull AspectJParser.ClassOrInterfaceTypeContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#typePatternList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypePatternList(@NotNull AspectJParser.TypePatternListContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#variableDeclaratorId}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableDeclaratorId(@NotNull AspectJParser.VariableDeclaratorIdContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#typeBound}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeBound(@NotNull AspectJParser.TypeBoundContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimary(@NotNull AspectJParser.PrimaryContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#classCreatorRest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassCreatorRest(@NotNull AspectJParser.ClassCreatorRestContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#CFlowPointcut}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCFlowPointcut(@NotNull AspectJParser.CFlowPointcutContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#interfaceBodyDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterfaceBodyDeclaration(@NotNull AspectJParser.InterfaceBodyDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#typeArguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeArguments(@NotNull AspectJParser.TypeArgumentsContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#fieldPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFieldPattern(@NotNull AspectJParser.FieldPatternContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#ExecutionPointcut}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecutionPointcut(@NotNull AspectJParser.ExecutionPointcutContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#aspectBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAspectBody(@NotNull AspectJParser.AspectBodyContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#annotationName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationName(@NotNull AspectJParser.AnnotationNameContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#AnnotationTargetPointcut}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationTargetPointcut(@NotNull AspectJParser.AnnotationTargetPointcutContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#finallyBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFinallyBlock(@NotNull AspectJParser.FinallyBlockContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#typeParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeParameters(@NotNull AspectJParser.TypeParametersContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#lastFormalParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLastFormalParameter(@NotNull AspectJParser.LastFormalParameterContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#constructorBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructorBody(@NotNull AspectJParser.ConstructorBodyContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral(@NotNull AspectJParser.LiteralContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#pointcutDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPointcutDeclaration(@NotNull AspectJParser.PointcutDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#WithinCodePointcut}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWithinCodePointcut(@NotNull AspectJParser.WithinCodePointcutContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#annotationMethodOrConstantRest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationMethodOrConstantRest(@NotNull AspectJParser.AnnotationMethodOrConstantRestContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#catchClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCatchClause(@NotNull AspectJParser.CatchClauseContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#variableDeclarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableDeclarator(@NotNull AspectJParser.VariableDeclaratorContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#typeList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeList(@NotNull AspectJParser.TypeListContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#AnnotationWithinCodePointcut}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationWithinCodePointcut(@NotNull AspectJParser.AnnotationWithinCodePointcutContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#enumConstants}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumConstants(@NotNull AspectJParser.EnumConstantsContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#classBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassBody(@NotNull AspectJParser.ClassBodyContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#createdName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreatedName(@NotNull AspectJParser.CreatedNameContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#enumDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumDeclaration(@NotNull AspectJParser.EnumDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#formalParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormalParameter(@NotNull AspectJParser.FormalParameterContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#parExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParExpression(@NotNull AspectJParser.ParExpressionContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#annotation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotation(@NotNull AspectJParser.AnnotationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#AnnotationArgsPointcut}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationArgsPointcut(@NotNull AspectJParser.AnnotationArgsPointcutContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#variableInitializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableInitializer(@NotNull AspectJParser.VariableInitializerContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#elementValueArrayInitializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElementValueArrayInitializer(@NotNull AspectJParser.ElementValueArrayInitializerContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#creator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreator(@NotNull AspectJParser.CreatorContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#arrayCreatorRest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayCreatorRest(@NotNull AspectJParser.ArrayCreatorRestContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#methodModifiersPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethodModifiersPattern(@NotNull AspectJParser.MethodModifiersPatternContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#argsPatternList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgsPatternList(@NotNull AspectJParser.ArgsPatternListContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(@NotNull AspectJParser.ExpressionContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#constantExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstantExpression(@NotNull AspectJParser.ConstantExpressionContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#methodPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethodPattern(@NotNull AspectJParser.MethodPatternContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#qualifiedNameList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualifiedNameList(@NotNull AspectJParser.QualifiedNameListContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#simpleTypePattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimpleTypePattern(@NotNull AspectJParser.SimpleTypePatternContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#constructorDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructorDeclaration(@NotNull AspectJParser.ConstructorDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#superSuffix}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSuperSuffix(@NotNull AspectJParser.SuperSuffixContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#forControl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForControl(@NotNull AspectJParser.ForControlContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#annotationsOrIdentifiersPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationsOrIdentifiersPattern(@NotNull AspectJParser.AnnotationsOrIdentifiersPatternContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#variableDeclarators}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableDeclarators(@NotNull AspectJParser.VariableDeclaratorsContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#CFlowBelowPointcut}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCFlowBelowPointcut(@NotNull AspectJParser.CFlowBelowPointcutContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#catchType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCatchType(@NotNull AspectJParser.CatchTypeContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#constructorModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructorModifier(@NotNull AspectJParser.ConstructorModifierContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#formalsPatternAfterDotDot}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormalsPatternAfterDotDot(@NotNull AspectJParser.FormalsPatternAfterDotDotContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#classOrInterfaceModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassOrInterfaceModifier(@NotNull AspectJParser.ClassOrInterfaceModifierContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#enumConstantName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumConstantName(@NotNull AspectJParser.EnumConstantNameContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#modifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModifier(@NotNull AspectJParser.ModifierContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#innerCreator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInnerCreator(@NotNull AspectJParser.InnerCreatorContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#explicitGenericInvocationSuffix}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExplicitGenericInvocationSuffix(@NotNull AspectJParser.ExplicitGenericInvocationSuffixContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#AnnotationThisPointcut}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationThisPointcut(@NotNull AspectJParser.AnnotationThisPointcutContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#variableModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableModifier(@NotNull AspectJParser.VariableModifierContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#elementValuePair}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElementValuePair(@NotNull AspectJParser.ElementValuePairContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#fieldModifiersPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFieldModifiersPattern(@NotNull AspectJParser.FieldModifiersPatternContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#arrayInitializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayInitializer(@NotNull AspectJParser.ArrayInitializerContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#elementValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElementValue(@NotNull AspectJParser.ElementValueContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#methodModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethodModifier(@NotNull AspectJParser.MethodModifierContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#constDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstDeclaration(@NotNull AspectJParser.ConstDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#resource}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResource(@NotNull AspectJParser.ResourceContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#qualifiedName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualifiedName(@NotNull AspectJParser.QualifiedNameContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#resourceSpecification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResourceSpecification(@NotNull AspectJParser.ResourceSpecificationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#TargetPointcut}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTargetPointcut(@NotNull AspectJParser.TargetPointcutContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#formalParameterList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormalParameterList(@NotNull AspectJParser.FormalParameterListContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#annotationsOrIdentifiersPatternAfterDotDot}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationsOrIdentifiersPatternAfterDotDot(@NotNull AspectJParser.AnnotationsOrIdentifiersPatternAfterDotDotContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#annotationTypeDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationTypeDeclaration(@NotNull AspectJParser.AnnotationTypeDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#annotationOrIdentifer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationOrIdentifer(@NotNull AspectJParser.AnnotationOrIdentiferContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#compilationUnit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompilationUnit(@NotNull AspectJParser.CompilationUnitContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#pointcutExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPointcutExpression(@NotNull AspectJParser.PointcutExpressionContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#annotationMethodRest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationMethodRest(@NotNull AspectJParser.AnnotationMethodRestContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#adviceSpec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdviceSpec(@NotNull AspectJParser.AdviceSpecContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#switchBlockStatementGroup}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitchBlockStatementGroup(@NotNull AspectJParser.SwitchBlockStatementGroupContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#throwsPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitThrowsPattern(@NotNull AspectJParser.ThrowsPatternContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#typeParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeParameter(@NotNull AspectJParser.TypeParameterContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#methodDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethodDeclaration(@NotNull AspectJParser.MethodDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#interfaceBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterfaceBody(@NotNull AspectJParser.InterfaceBodyContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#methodBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethodBody(@NotNull AspectJParser.MethodBodyContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#constructorModifiersPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructorModifiersPattern(@NotNull AspectJParser.ConstructorModifiersPatternContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#typeArgument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeArgument(@NotNull AspectJParser.TypeArgumentContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#advice}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdvice(@NotNull AspectJParser.AdviceContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#referencePointcut}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReferencePointcut(@NotNull AspectJParser.ReferencePointcutContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#typePattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypePattern(@NotNull AspectJParser.TypePatternContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#typeDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeDeclaration(@NotNull AspectJParser.TypeDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#typeOrIdentifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeOrIdentifier(@NotNull AspectJParser.TypeOrIdentifierContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#genericConstructorDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenericConstructorDeclaration(@NotNull AspectJParser.GenericConstructorDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#classDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassDeclaration(@NotNull AspectJParser.ClassDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#SetPointcut}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetPointcut(@NotNull AspectJParser.SetPointcutContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#dottedNamePattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDottedNamePattern(@NotNull AspectJParser.DottedNamePatternContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#enumConstant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnumConstant(@NotNull AspectJParser.EnumConstantContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#IfPointcut}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfPointcut(@NotNull AspectJParser.IfPointcutContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#PreInitializationPointcut}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPreInitializationPointcut(@NotNull AspectJParser.PreInitializationPointcutContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(@NotNull AspectJParser.StatementContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#fieldModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFieldModifier(@NotNull AspectJParser.FieldModifierContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#importDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportDeclaration(@NotNull AspectJParser.ImportDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#primitiveType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimitiveType(@NotNull AspectJParser.PrimitiveTypeContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#StaticInitializationPointcut}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStaticInitializationPointcut(@NotNull AspectJParser.StaticInitializationPointcutContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#dotOrDotDot}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDotOrDotDot(@NotNull AspectJParser.DotOrDotDotContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#interfaceDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterfaceDeclaration(@NotNull AspectJParser.InterfaceDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#localVariableDeclarationStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocalVariableDeclarationStatement(@NotNull AspectJParser.LocalVariableDeclarationStatementContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#blockStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlockStatement(@NotNull AspectJParser.BlockStatementContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#constantDeclarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstantDeclarator(@NotNull AspectJParser.ConstantDeclaratorContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#fieldDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFieldDeclaration(@NotNull AspectJParser.FieldDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#resources}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResources(@NotNull AspectJParser.ResourcesContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#statementExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatementExpression(@NotNull AspectJParser.StatementExpressionContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#aspectBodyDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAspectBodyDeclaration(@NotNull AspectJParser.AspectBodyDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#interfaceMethodDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterfaceMethodDeclaration(@NotNull AspectJParser.InterfaceMethodDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#argsPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgsPattern(@NotNull AspectJParser.ArgsPatternContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#simpleNamePattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimpleNamePattern(@NotNull AspectJParser.SimpleNamePatternContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#packageDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPackageDeclaration(@NotNull AspectJParser.PackageDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#elementValuePairs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElementValuePairs(@NotNull AspectJParser.ElementValuePairsContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#localVariableDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocalVariableDeclaration(@NotNull AspectJParser.LocalVariableDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#CallPointcut}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCallPointcut(@NotNull AspectJParser.CallPointcutContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#WithinPointcut}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWithinPointcut(@NotNull AspectJParser.WithinPointcutContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#nonWildcardTypeArguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNonWildcardTypeArguments(@NotNull AspectJParser.NonWildcardTypeArgumentsContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#interfaceMemberDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterfaceMemberDeclaration(@NotNull AspectJParser.InterfaceMemberDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#switchLabel}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitchLabel(@NotNull AspectJParser.SwitchLabelContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#forInit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForInit(@NotNull AspectJParser.ForInitContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#formalParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormalParameters(@NotNull AspectJParser.FormalParametersContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#arguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArguments(@NotNull AspectJParser.ArgumentsContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#ThisPointcutPointcut}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitThisPointcutPointcut(@NotNull AspectJParser.ThisPointcutPointcutContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#genericMethodDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenericMethodDeclaration(@NotNull AspectJParser.GenericMethodDeclarationContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#typeArgumentsOrDiamond}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeArgumentsOrDiamond(@NotNull AspectJParser.TypeArgumentsOrDiamondContext ctx);

	/**
	 * Visit a parse tree produced by {@link AspectJParser#HandlerPointcut}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHandlerPointcut(@NotNull AspectJParser.HandlerPointcutContext ctx);
}