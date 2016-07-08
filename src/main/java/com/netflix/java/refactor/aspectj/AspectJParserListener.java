// Generated from /Users/joschneider/Projects/github/jkschneider/java-source-linter/src/main/antlr4/AspectJParser.g4 by ANTLR 4.2.2
package com.netflix.java.refactor.aspectj;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link AspectJParser}.
 */
public interface AspectJParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link AspectJParser#classPattern}.
	 * @param ctx the parse tree
	 */
	void enterClassPattern(@NotNull AspectJParser.ClassPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#classPattern}.
	 * @param ctx the parse tree
	 */
	void exitClassPattern(@NotNull AspectJParser.ClassPatternContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#memberDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterMemberDeclaration(@NotNull AspectJParser.MemberDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#memberDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitMemberDeclaration(@NotNull AspectJParser.MemberDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#GetPointcut}.
	 * @param ctx the parse tree
	 */
	void enterGetPointcut(@NotNull AspectJParser.GetPointcutContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#GetPointcut}.
	 * @param ctx the parse tree
	 */
	void exitGetPointcut(@NotNull AspectJParser.GetPointcutContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#defaultValue}.
	 * @param ctx the parse tree
	 */
	void enterDefaultValue(@NotNull AspectJParser.DefaultValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#defaultValue}.
	 * @param ctx the parse tree
	 */
	void exitDefaultValue(@NotNull AspectJParser.DefaultValueContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#annotationTypeElementDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationTypeElementDeclaration(@NotNull AspectJParser.AnnotationTypeElementDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#annotationTypeElementDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationTypeElementDeclaration(@NotNull AspectJParser.AnnotationTypeElementDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#AdviceExecutionPointcut}.
	 * @param ctx the parse tree
	 */
	void enterAdviceExecutionPointcut(@NotNull AspectJParser.AdviceExecutionPointcutContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#AdviceExecutionPointcut}.
	 * @param ctx the parse tree
	 */
	void exitAdviceExecutionPointcut(@NotNull AspectJParser.AdviceExecutionPointcutContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#AnnotationPointcut}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationPointcut(@NotNull AspectJParser.AnnotationPointcutContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#AnnotationPointcut}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationPointcut(@NotNull AspectJParser.AnnotationPointcutContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#annotationPattern}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationPattern(@NotNull AspectJParser.AnnotationPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#annotationPattern}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationPattern(@NotNull AspectJParser.AnnotationPatternContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#aspectDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterAspectDeclaration(@NotNull AspectJParser.AspectDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#aspectDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitAspectDeclaration(@NotNull AspectJParser.AspectDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#interTypeDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterInterTypeDeclaration(@NotNull AspectJParser.InterTypeDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#interTypeDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitInterTypeDeclaration(@NotNull AspectJParser.InterTypeDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(@NotNull AspectJParser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(@NotNull AspectJParser.TypeContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#annotationTypeBody}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationTypeBody(@NotNull AspectJParser.AnnotationTypeBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#annotationTypeBody}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationTypeBody(@NotNull AspectJParser.AnnotationTypeBodyContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#ArgsPointcut}.
	 * @param ctx the parse tree
	 */
	void enterArgsPointcut(@NotNull AspectJParser.ArgsPointcutContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#ArgsPointcut}.
	 * @param ctx the parse tree
	 */
	void exitArgsPointcut(@NotNull AspectJParser.ArgsPointcutContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#genericInterfaceMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterGenericInterfaceMethodDeclaration(@NotNull AspectJParser.GenericInterfaceMethodDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#genericInterfaceMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitGenericInterfaceMethodDeclaration(@NotNull AspectJParser.GenericInterfaceMethodDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#annotationTypePattern}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationTypePattern(@NotNull AspectJParser.AnnotationTypePatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#annotationTypePattern}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationTypePattern(@NotNull AspectJParser.AnnotationTypePatternContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#classBodyDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterClassBodyDeclaration(@NotNull AspectJParser.ClassBodyDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#classBodyDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitClassBodyDeclaration(@NotNull AspectJParser.ClassBodyDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#formalsPattern}.
	 * @param ctx the parse tree
	 */
	void enterFormalsPattern(@NotNull AspectJParser.FormalsPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#formalsPattern}.
	 * @param ctx the parse tree
	 */
	void exitFormalsPattern(@NotNull AspectJParser.FormalsPatternContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#block}.
	 * @param ctx the parse tree
	 */
	void enterBlock(@NotNull AspectJParser.BlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#block}.
	 * @param ctx the parse tree
	 */
	void exitBlock(@NotNull AspectJParser.BlockContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#id}.
	 * @param ctx the parse tree
	 */
	void enterId(@NotNull AspectJParser.IdContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#id}.
	 * @param ctx the parse tree
	 */
	void exitId(@NotNull AspectJParser.IdContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#enumBodyDeclarations}.
	 * @param ctx the parse tree
	 */
	void enterEnumBodyDeclarations(@NotNull AspectJParser.EnumBodyDeclarationsContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#enumBodyDeclarations}.
	 * @param ctx the parse tree
	 */
	void exitEnumBodyDeclarations(@NotNull AspectJParser.EnumBodyDeclarationsContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#InitializationPointcut}.
	 * @param ctx the parse tree
	 */
	void enterInitializationPointcut(@NotNull AspectJParser.InitializationPointcutContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#InitializationPointcut}.
	 * @param ctx the parse tree
	 */
	void exitInitializationPointcut(@NotNull AspectJParser.InitializationPointcutContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#forUpdate}.
	 * @param ctx the parse tree
	 */
	void enterForUpdate(@NotNull AspectJParser.ForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#forUpdate}.
	 * @param ctx the parse tree
	 */
	void exitForUpdate(@NotNull AspectJParser.ForUpdateContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#constructorPattern}.
	 * @param ctx the parse tree
	 */
	void enterConstructorPattern(@NotNull AspectJParser.ConstructorPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#constructorPattern}.
	 * @param ctx the parse tree
	 */
	void exitConstructorPattern(@NotNull AspectJParser.ConstructorPatternContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#optionalParensTypePattern}.
	 * @param ctx the parse tree
	 */
	void enterOptionalParensTypePattern(@NotNull AspectJParser.OptionalParensTypePatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#optionalParensTypePattern}.
	 * @param ctx the parse tree
	 */
	void exitOptionalParensTypePattern(@NotNull AspectJParser.OptionalParensTypePatternContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#classPatternList}.
	 * @param ctx the parse tree
	 */
	void enterClassPatternList(@NotNull AspectJParser.ClassPatternListContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#classPatternList}.
	 * @param ctx the parse tree
	 */
	void exitClassPatternList(@NotNull AspectJParser.ClassPatternListContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#enhancedForControl}.
	 * @param ctx the parse tree
	 */
	void enterEnhancedForControl(@NotNull AspectJParser.EnhancedForControlContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#enhancedForControl}.
	 * @param ctx the parse tree
	 */
	void exitEnhancedForControl(@NotNull AspectJParser.EnhancedForControlContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#annotationConstantRest}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationConstantRest(@NotNull AspectJParser.AnnotationConstantRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#annotationConstantRest}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationConstantRest(@NotNull AspectJParser.AnnotationConstantRestContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#methodOrConstructorPattern}.
	 * @param ctx the parse tree
	 */
	void enterMethodOrConstructorPattern(@NotNull AspectJParser.MethodOrConstructorPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#methodOrConstructorPattern}.
	 * @param ctx the parse tree
	 */
	void exitMethodOrConstructorPattern(@NotNull AspectJParser.MethodOrConstructorPatternContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#perClause}.
	 * @param ctx the parse tree
	 */
	void enterPerClause(@NotNull AspectJParser.PerClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#perClause}.
	 * @param ctx the parse tree
	 */
	void exitPerClause(@NotNull AspectJParser.PerClauseContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#interTypeMemberDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterInterTypeMemberDeclaration(@NotNull AspectJParser.InterTypeMemberDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#interTypeMemberDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitInterTypeMemberDeclaration(@NotNull AspectJParser.InterTypeMemberDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#explicitGenericInvocation}.
	 * @param ctx the parse tree
	 */
	void enterExplicitGenericInvocation(@NotNull AspectJParser.ExplicitGenericInvocationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#explicitGenericInvocation}.
	 * @param ctx the parse tree
	 */
	void exitExplicitGenericInvocation(@NotNull AspectJParser.ExplicitGenericInvocationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#nonWildcardTypeArgumentsOrDiamond}.
	 * @param ctx the parse tree
	 */
	void enterNonWildcardTypeArgumentsOrDiamond(@NotNull AspectJParser.NonWildcardTypeArgumentsOrDiamondContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#nonWildcardTypeArgumentsOrDiamond}.
	 * @param ctx the parse tree
	 */
	void exitNonWildcardTypeArgumentsOrDiamond(@NotNull AspectJParser.NonWildcardTypeArgumentsOrDiamondContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#AnnotationWithinPointcut}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationWithinPointcut(@NotNull AspectJParser.AnnotationWithinPointcutContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#AnnotationWithinPointcut}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationWithinPointcut(@NotNull AspectJParser.AnnotationWithinPointcutContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#expressionList}.
	 * @param ctx the parse tree
	 */
	void enterExpressionList(@NotNull AspectJParser.ExpressionListContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#expressionList}.
	 * @param ctx the parse tree
	 */
	void exitExpressionList(@NotNull AspectJParser.ExpressionListContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#formalParametersPattern}.
	 * @param ctx the parse tree
	 */
	void enterFormalParametersPattern(@NotNull AspectJParser.FormalParametersPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#formalParametersPattern}.
	 * @param ctx the parse tree
	 */
	void exitFormalParametersPattern(@NotNull AspectJParser.FormalParametersPatternContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#annotationTypeElementRest}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationTypeElementRest(@NotNull AspectJParser.AnnotationTypeElementRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#annotationTypeElementRest}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationTypeElementRest(@NotNull AspectJParser.AnnotationTypeElementRestContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#classOrInterfaceType}.
	 * @param ctx the parse tree
	 */
	void enterClassOrInterfaceType(@NotNull AspectJParser.ClassOrInterfaceTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#classOrInterfaceType}.
	 * @param ctx the parse tree
	 */
	void exitClassOrInterfaceType(@NotNull AspectJParser.ClassOrInterfaceTypeContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#typePatternList}.
	 * @param ctx the parse tree
	 */
	void enterTypePatternList(@NotNull AspectJParser.TypePatternListContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#typePatternList}.
	 * @param ctx the parse tree
	 */
	void exitTypePatternList(@NotNull AspectJParser.TypePatternListContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#variableDeclaratorId}.
	 * @param ctx the parse tree
	 */
	void enterVariableDeclaratorId(@NotNull AspectJParser.VariableDeclaratorIdContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#variableDeclaratorId}.
	 * @param ctx the parse tree
	 */
	void exitVariableDeclaratorId(@NotNull AspectJParser.VariableDeclaratorIdContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#typeBound}.
	 * @param ctx the parse tree
	 */
	void enterTypeBound(@NotNull AspectJParser.TypeBoundContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#typeBound}.
	 * @param ctx the parse tree
	 */
	void exitTypeBound(@NotNull AspectJParser.TypeBoundContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#primary}.
	 * @param ctx the parse tree
	 */
	void enterPrimary(@NotNull AspectJParser.PrimaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#primary}.
	 * @param ctx the parse tree
	 */
	void exitPrimary(@NotNull AspectJParser.PrimaryContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#classCreatorRest}.
	 * @param ctx the parse tree
	 */
	void enterClassCreatorRest(@NotNull AspectJParser.ClassCreatorRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#classCreatorRest}.
	 * @param ctx the parse tree
	 */
	void exitClassCreatorRest(@NotNull AspectJParser.ClassCreatorRestContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#CFlowPointcut}.
	 * @param ctx the parse tree
	 */
	void enterCFlowPointcut(@NotNull AspectJParser.CFlowPointcutContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#CFlowPointcut}.
	 * @param ctx the parse tree
	 */
	void exitCFlowPointcut(@NotNull AspectJParser.CFlowPointcutContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#interfaceBodyDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceBodyDeclaration(@NotNull AspectJParser.InterfaceBodyDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#interfaceBodyDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceBodyDeclaration(@NotNull AspectJParser.InterfaceBodyDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#typeArguments}.
	 * @param ctx the parse tree
	 */
	void enterTypeArguments(@NotNull AspectJParser.TypeArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#typeArguments}.
	 * @param ctx the parse tree
	 */
	void exitTypeArguments(@NotNull AspectJParser.TypeArgumentsContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#fieldPattern}.
	 * @param ctx the parse tree
	 */
	void enterFieldPattern(@NotNull AspectJParser.FieldPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#fieldPattern}.
	 * @param ctx the parse tree
	 */
	void exitFieldPattern(@NotNull AspectJParser.FieldPatternContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#ExecutionPointcut}.
	 * @param ctx the parse tree
	 */
	void enterExecutionPointcut(@NotNull AspectJParser.ExecutionPointcutContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#ExecutionPointcut}.
	 * @param ctx the parse tree
	 */
	void exitExecutionPointcut(@NotNull AspectJParser.ExecutionPointcutContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#aspectBody}.
	 * @param ctx the parse tree
	 */
	void enterAspectBody(@NotNull AspectJParser.AspectBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#aspectBody}.
	 * @param ctx the parse tree
	 */
	void exitAspectBody(@NotNull AspectJParser.AspectBodyContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#annotationName}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationName(@NotNull AspectJParser.AnnotationNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#annotationName}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationName(@NotNull AspectJParser.AnnotationNameContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#AnnotationTargetPointcut}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationTargetPointcut(@NotNull AspectJParser.AnnotationTargetPointcutContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#AnnotationTargetPointcut}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationTargetPointcut(@NotNull AspectJParser.AnnotationTargetPointcutContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#finallyBlock}.
	 * @param ctx the parse tree
	 */
	void enterFinallyBlock(@NotNull AspectJParser.FinallyBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#finallyBlock}.
	 * @param ctx the parse tree
	 */
	void exitFinallyBlock(@NotNull AspectJParser.FinallyBlockContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#typeParameters}.
	 * @param ctx the parse tree
	 */
	void enterTypeParameters(@NotNull AspectJParser.TypeParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#typeParameters}.
	 * @param ctx the parse tree
	 */
	void exitTypeParameters(@NotNull AspectJParser.TypeParametersContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#lastFormalParameter}.
	 * @param ctx the parse tree
	 */
	void enterLastFormalParameter(@NotNull AspectJParser.LastFormalParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#lastFormalParameter}.
	 * @param ctx the parse tree
	 */
	void exitLastFormalParameter(@NotNull AspectJParser.LastFormalParameterContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#constructorBody}.
	 * @param ctx the parse tree
	 */
	void enterConstructorBody(@NotNull AspectJParser.ConstructorBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#constructorBody}.
	 * @param ctx the parse tree
	 */
	void exitConstructorBody(@NotNull AspectJParser.ConstructorBodyContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLiteral(@NotNull AspectJParser.LiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLiteral(@NotNull AspectJParser.LiteralContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#pointcutDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterPointcutDeclaration(@NotNull AspectJParser.PointcutDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#pointcutDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitPointcutDeclaration(@NotNull AspectJParser.PointcutDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#WithinCodePointcut}.
	 * @param ctx the parse tree
	 */
	void enterWithinCodePointcut(@NotNull AspectJParser.WithinCodePointcutContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#WithinCodePointcut}.
	 * @param ctx the parse tree
	 */
	void exitWithinCodePointcut(@NotNull AspectJParser.WithinCodePointcutContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#annotationMethodOrConstantRest}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationMethodOrConstantRest(@NotNull AspectJParser.AnnotationMethodOrConstantRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#annotationMethodOrConstantRest}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationMethodOrConstantRest(@NotNull AspectJParser.AnnotationMethodOrConstantRestContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#catchClause}.
	 * @param ctx the parse tree
	 */
	void enterCatchClause(@NotNull AspectJParser.CatchClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#catchClause}.
	 * @param ctx the parse tree
	 */
	void exitCatchClause(@NotNull AspectJParser.CatchClauseContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#variableDeclarator}.
	 * @param ctx the parse tree
	 */
	void enterVariableDeclarator(@NotNull AspectJParser.VariableDeclaratorContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#variableDeclarator}.
	 * @param ctx the parse tree
	 */
	void exitVariableDeclarator(@NotNull AspectJParser.VariableDeclaratorContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#typeList}.
	 * @param ctx the parse tree
	 */
	void enterTypeList(@NotNull AspectJParser.TypeListContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#typeList}.
	 * @param ctx the parse tree
	 */
	void exitTypeList(@NotNull AspectJParser.TypeListContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#AnnotationWithinCodePointcut}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationWithinCodePointcut(@NotNull AspectJParser.AnnotationWithinCodePointcutContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#AnnotationWithinCodePointcut}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationWithinCodePointcut(@NotNull AspectJParser.AnnotationWithinCodePointcutContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#enumConstants}.
	 * @param ctx the parse tree
	 */
	void enterEnumConstants(@NotNull AspectJParser.EnumConstantsContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#enumConstants}.
	 * @param ctx the parse tree
	 */
	void exitEnumConstants(@NotNull AspectJParser.EnumConstantsContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#classBody}.
	 * @param ctx the parse tree
	 */
	void enterClassBody(@NotNull AspectJParser.ClassBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#classBody}.
	 * @param ctx the parse tree
	 */
	void exitClassBody(@NotNull AspectJParser.ClassBodyContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#createdName}.
	 * @param ctx the parse tree
	 */
	void enterCreatedName(@NotNull AspectJParser.CreatedNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#createdName}.
	 * @param ctx the parse tree
	 */
	void exitCreatedName(@NotNull AspectJParser.CreatedNameContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#enumDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterEnumDeclaration(@NotNull AspectJParser.EnumDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#enumDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitEnumDeclaration(@NotNull AspectJParser.EnumDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#formalParameter}.
	 * @param ctx the parse tree
	 */
	void enterFormalParameter(@NotNull AspectJParser.FormalParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#formalParameter}.
	 * @param ctx the parse tree
	 */
	void exitFormalParameter(@NotNull AspectJParser.FormalParameterContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#parExpression}.
	 * @param ctx the parse tree
	 */
	void enterParExpression(@NotNull AspectJParser.ParExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#parExpression}.
	 * @param ctx the parse tree
	 */
	void exitParExpression(@NotNull AspectJParser.ParExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#annotation}.
	 * @param ctx the parse tree
	 */
	void enterAnnotation(@NotNull AspectJParser.AnnotationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#annotation}.
	 * @param ctx the parse tree
	 */
	void exitAnnotation(@NotNull AspectJParser.AnnotationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#AnnotationArgsPointcut}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationArgsPointcut(@NotNull AspectJParser.AnnotationArgsPointcutContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#AnnotationArgsPointcut}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationArgsPointcut(@NotNull AspectJParser.AnnotationArgsPointcutContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#variableInitializer}.
	 * @param ctx the parse tree
	 */
	void enterVariableInitializer(@NotNull AspectJParser.VariableInitializerContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#variableInitializer}.
	 * @param ctx the parse tree
	 */
	void exitVariableInitializer(@NotNull AspectJParser.VariableInitializerContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#elementValueArrayInitializer}.
	 * @param ctx the parse tree
	 */
	void enterElementValueArrayInitializer(@NotNull AspectJParser.ElementValueArrayInitializerContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#elementValueArrayInitializer}.
	 * @param ctx the parse tree
	 */
	void exitElementValueArrayInitializer(@NotNull AspectJParser.ElementValueArrayInitializerContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#creator}.
	 * @param ctx the parse tree
	 */
	void enterCreator(@NotNull AspectJParser.CreatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#creator}.
	 * @param ctx the parse tree
	 */
	void exitCreator(@NotNull AspectJParser.CreatorContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#arrayCreatorRest}.
	 * @param ctx the parse tree
	 */
	void enterArrayCreatorRest(@NotNull AspectJParser.ArrayCreatorRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#arrayCreatorRest}.
	 * @param ctx the parse tree
	 */
	void exitArrayCreatorRest(@NotNull AspectJParser.ArrayCreatorRestContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#methodModifiersPattern}.
	 * @param ctx the parse tree
	 */
	void enterMethodModifiersPattern(@NotNull AspectJParser.MethodModifiersPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#methodModifiersPattern}.
	 * @param ctx the parse tree
	 */
	void exitMethodModifiersPattern(@NotNull AspectJParser.MethodModifiersPatternContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#argsPatternList}.
	 * @param ctx the parse tree
	 */
	void enterArgsPatternList(@NotNull AspectJParser.ArgsPatternListContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#argsPatternList}.
	 * @param ctx the parse tree
	 */
	void exitArgsPatternList(@NotNull AspectJParser.ArgsPatternListContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(@NotNull AspectJParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(@NotNull AspectJParser.ExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#constantExpression}.
	 * @param ctx the parse tree
	 */
	void enterConstantExpression(@NotNull AspectJParser.ConstantExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#constantExpression}.
	 * @param ctx the parse tree
	 */
	void exitConstantExpression(@NotNull AspectJParser.ConstantExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#methodPattern}.
	 * @param ctx the parse tree
	 */
	void enterMethodPattern(@NotNull AspectJParser.MethodPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#methodPattern}.
	 * @param ctx the parse tree
	 */
	void exitMethodPattern(@NotNull AspectJParser.MethodPatternContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#qualifiedNameList}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedNameList(@NotNull AspectJParser.QualifiedNameListContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#qualifiedNameList}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedNameList(@NotNull AspectJParser.QualifiedNameListContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#simpleTypePattern}.
	 * @param ctx the parse tree
	 */
	void enterSimpleTypePattern(@NotNull AspectJParser.SimpleTypePatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#simpleTypePattern}.
	 * @param ctx the parse tree
	 */
	void exitSimpleTypePattern(@NotNull AspectJParser.SimpleTypePatternContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#constructorDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterConstructorDeclaration(@NotNull AspectJParser.ConstructorDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#constructorDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitConstructorDeclaration(@NotNull AspectJParser.ConstructorDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#superSuffix}.
	 * @param ctx the parse tree
	 */
	void enterSuperSuffix(@NotNull AspectJParser.SuperSuffixContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#superSuffix}.
	 * @param ctx the parse tree
	 */
	void exitSuperSuffix(@NotNull AspectJParser.SuperSuffixContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#forControl}.
	 * @param ctx the parse tree
	 */
	void enterForControl(@NotNull AspectJParser.ForControlContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#forControl}.
	 * @param ctx the parse tree
	 */
	void exitForControl(@NotNull AspectJParser.ForControlContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#annotationsOrIdentifiersPattern}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationsOrIdentifiersPattern(@NotNull AspectJParser.AnnotationsOrIdentifiersPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#annotationsOrIdentifiersPattern}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationsOrIdentifiersPattern(@NotNull AspectJParser.AnnotationsOrIdentifiersPatternContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#variableDeclarators}.
	 * @param ctx the parse tree
	 */
	void enterVariableDeclarators(@NotNull AspectJParser.VariableDeclaratorsContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#variableDeclarators}.
	 * @param ctx the parse tree
	 */
	void exitVariableDeclarators(@NotNull AspectJParser.VariableDeclaratorsContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#CFlowBelowPointcut}.
	 * @param ctx the parse tree
	 */
	void enterCFlowBelowPointcut(@NotNull AspectJParser.CFlowBelowPointcutContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#CFlowBelowPointcut}.
	 * @param ctx the parse tree
	 */
	void exitCFlowBelowPointcut(@NotNull AspectJParser.CFlowBelowPointcutContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#catchType}.
	 * @param ctx the parse tree
	 */
	void enterCatchType(@NotNull AspectJParser.CatchTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#catchType}.
	 * @param ctx the parse tree
	 */
	void exitCatchType(@NotNull AspectJParser.CatchTypeContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#constructorModifier}.
	 * @param ctx the parse tree
	 */
	void enterConstructorModifier(@NotNull AspectJParser.ConstructorModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#constructorModifier}.
	 * @param ctx the parse tree
	 */
	void exitConstructorModifier(@NotNull AspectJParser.ConstructorModifierContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#formalsPatternAfterDotDot}.
	 * @param ctx the parse tree
	 */
	void enterFormalsPatternAfterDotDot(@NotNull AspectJParser.FormalsPatternAfterDotDotContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#formalsPatternAfterDotDot}.
	 * @param ctx the parse tree
	 */
	void exitFormalsPatternAfterDotDot(@NotNull AspectJParser.FormalsPatternAfterDotDotContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#classOrInterfaceModifier}.
	 * @param ctx the parse tree
	 */
	void enterClassOrInterfaceModifier(@NotNull AspectJParser.ClassOrInterfaceModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#classOrInterfaceModifier}.
	 * @param ctx the parse tree
	 */
	void exitClassOrInterfaceModifier(@NotNull AspectJParser.ClassOrInterfaceModifierContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#enumConstantName}.
	 * @param ctx the parse tree
	 */
	void enterEnumConstantName(@NotNull AspectJParser.EnumConstantNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#enumConstantName}.
	 * @param ctx the parse tree
	 */
	void exitEnumConstantName(@NotNull AspectJParser.EnumConstantNameContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#modifier}.
	 * @param ctx the parse tree
	 */
	void enterModifier(@NotNull AspectJParser.ModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#modifier}.
	 * @param ctx the parse tree
	 */
	void exitModifier(@NotNull AspectJParser.ModifierContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#innerCreator}.
	 * @param ctx the parse tree
	 */
	void enterInnerCreator(@NotNull AspectJParser.InnerCreatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#innerCreator}.
	 * @param ctx the parse tree
	 */
	void exitInnerCreator(@NotNull AspectJParser.InnerCreatorContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#explicitGenericInvocationSuffix}.
	 * @param ctx the parse tree
	 */
	void enterExplicitGenericInvocationSuffix(@NotNull AspectJParser.ExplicitGenericInvocationSuffixContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#explicitGenericInvocationSuffix}.
	 * @param ctx the parse tree
	 */
	void exitExplicitGenericInvocationSuffix(@NotNull AspectJParser.ExplicitGenericInvocationSuffixContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#AnnotationThisPointcut}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationThisPointcut(@NotNull AspectJParser.AnnotationThisPointcutContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#AnnotationThisPointcut}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationThisPointcut(@NotNull AspectJParser.AnnotationThisPointcutContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#variableModifier}.
	 * @param ctx the parse tree
	 */
	void enterVariableModifier(@NotNull AspectJParser.VariableModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#variableModifier}.
	 * @param ctx the parse tree
	 */
	void exitVariableModifier(@NotNull AspectJParser.VariableModifierContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#elementValuePair}.
	 * @param ctx the parse tree
	 */
	void enterElementValuePair(@NotNull AspectJParser.ElementValuePairContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#elementValuePair}.
	 * @param ctx the parse tree
	 */
	void exitElementValuePair(@NotNull AspectJParser.ElementValuePairContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#fieldModifiersPattern}.
	 * @param ctx the parse tree
	 */
	void enterFieldModifiersPattern(@NotNull AspectJParser.FieldModifiersPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#fieldModifiersPattern}.
	 * @param ctx the parse tree
	 */
	void exitFieldModifiersPattern(@NotNull AspectJParser.FieldModifiersPatternContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#arrayInitializer}.
	 * @param ctx the parse tree
	 */
	void enterArrayInitializer(@NotNull AspectJParser.ArrayInitializerContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#arrayInitializer}.
	 * @param ctx the parse tree
	 */
	void exitArrayInitializer(@NotNull AspectJParser.ArrayInitializerContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#elementValue}.
	 * @param ctx the parse tree
	 */
	void enterElementValue(@NotNull AspectJParser.ElementValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#elementValue}.
	 * @param ctx the parse tree
	 */
	void exitElementValue(@NotNull AspectJParser.ElementValueContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#methodModifier}.
	 * @param ctx the parse tree
	 */
	void enterMethodModifier(@NotNull AspectJParser.MethodModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#methodModifier}.
	 * @param ctx the parse tree
	 */
	void exitMethodModifier(@NotNull AspectJParser.MethodModifierContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#constDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterConstDeclaration(@NotNull AspectJParser.ConstDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#constDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitConstDeclaration(@NotNull AspectJParser.ConstDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#resource}.
	 * @param ctx the parse tree
	 */
	void enterResource(@NotNull AspectJParser.ResourceContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#resource}.
	 * @param ctx the parse tree
	 */
	void exitResource(@NotNull AspectJParser.ResourceContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void enterQualifiedName(@NotNull AspectJParser.QualifiedNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#qualifiedName}.
	 * @param ctx the parse tree
	 */
	void exitQualifiedName(@NotNull AspectJParser.QualifiedNameContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#resourceSpecification}.
	 * @param ctx the parse tree
	 */
	void enterResourceSpecification(@NotNull AspectJParser.ResourceSpecificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#resourceSpecification}.
	 * @param ctx the parse tree
	 */
	void exitResourceSpecification(@NotNull AspectJParser.ResourceSpecificationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#TargetPointcut}.
	 * @param ctx the parse tree
	 */
	void enterTargetPointcut(@NotNull AspectJParser.TargetPointcutContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#TargetPointcut}.
	 * @param ctx the parse tree
	 */
	void exitTargetPointcut(@NotNull AspectJParser.TargetPointcutContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#formalParameterList}.
	 * @param ctx the parse tree
	 */
	void enterFormalParameterList(@NotNull AspectJParser.FormalParameterListContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#formalParameterList}.
	 * @param ctx the parse tree
	 */
	void exitFormalParameterList(@NotNull AspectJParser.FormalParameterListContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#annotationsOrIdentifiersPatternAfterDotDot}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationsOrIdentifiersPatternAfterDotDot(@NotNull AspectJParser.AnnotationsOrIdentifiersPatternAfterDotDotContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#annotationsOrIdentifiersPatternAfterDotDot}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationsOrIdentifiersPatternAfterDotDot(@NotNull AspectJParser.AnnotationsOrIdentifiersPatternAfterDotDotContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#annotationTypeDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationTypeDeclaration(@NotNull AspectJParser.AnnotationTypeDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#annotationTypeDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationTypeDeclaration(@NotNull AspectJParser.AnnotationTypeDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#annotationOrIdentifer}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationOrIdentifer(@NotNull AspectJParser.AnnotationOrIdentiferContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#annotationOrIdentifer}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationOrIdentifer(@NotNull AspectJParser.AnnotationOrIdentiferContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#compilationUnit}.
	 * @param ctx the parse tree
	 */
	void enterCompilationUnit(@NotNull AspectJParser.CompilationUnitContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#compilationUnit}.
	 * @param ctx the parse tree
	 */
	void exitCompilationUnit(@NotNull AspectJParser.CompilationUnitContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#pointcutExpression}.
	 * @param ctx the parse tree
	 */
	void enterPointcutExpression(@NotNull AspectJParser.PointcutExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#pointcutExpression}.
	 * @param ctx the parse tree
	 */
	void exitPointcutExpression(@NotNull AspectJParser.PointcutExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#annotationMethodRest}.
	 * @param ctx the parse tree
	 */
	void enterAnnotationMethodRest(@NotNull AspectJParser.AnnotationMethodRestContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#annotationMethodRest}.
	 * @param ctx the parse tree
	 */
	void exitAnnotationMethodRest(@NotNull AspectJParser.AnnotationMethodRestContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#adviceSpec}.
	 * @param ctx the parse tree
	 */
	void enterAdviceSpec(@NotNull AspectJParser.AdviceSpecContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#adviceSpec}.
	 * @param ctx the parse tree
	 */
	void exitAdviceSpec(@NotNull AspectJParser.AdviceSpecContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#switchBlockStatementGroup}.
	 * @param ctx the parse tree
	 */
	void enterSwitchBlockStatementGroup(@NotNull AspectJParser.SwitchBlockStatementGroupContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#switchBlockStatementGroup}.
	 * @param ctx the parse tree
	 */
	void exitSwitchBlockStatementGroup(@NotNull AspectJParser.SwitchBlockStatementGroupContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#throwsPattern}.
	 * @param ctx the parse tree
	 */
	void enterThrowsPattern(@NotNull AspectJParser.ThrowsPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#throwsPattern}.
	 * @param ctx the parse tree
	 */
	void exitThrowsPattern(@NotNull AspectJParser.ThrowsPatternContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#typeParameter}.
	 * @param ctx the parse tree
	 */
	void enterTypeParameter(@NotNull AspectJParser.TypeParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#typeParameter}.
	 * @param ctx the parse tree
	 */
	void exitTypeParameter(@NotNull AspectJParser.TypeParameterContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#methodDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterMethodDeclaration(@NotNull AspectJParser.MethodDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#methodDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitMethodDeclaration(@NotNull AspectJParser.MethodDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#interfaceBody}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceBody(@NotNull AspectJParser.InterfaceBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#interfaceBody}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceBody(@NotNull AspectJParser.InterfaceBodyContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#methodBody}.
	 * @param ctx the parse tree
	 */
	void enterMethodBody(@NotNull AspectJParser.MethodBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#methodBody}.
	 * @param ctx the parse tree
	 */
	void exitMethodBody(@NotNull AspectJParser.MethodBodyContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#constructorModifiersPattern}.
	 * @param ctx the parse tree
	 */
	void enterConstructorModifiersPattern(@NotNull AspectJParser.ConstructorModifiersPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#constructorModifiersPattern}.
	 * @param ctx the parse tree
	 */
	void exitConstructorModifiersPattern(@NotNull AspectJParser.ConstructorModifiersPatternContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#typeArgument}.
	 * @param ctx the parse tree
	 */
	void enterTypeArgument(@NotNull AspectJParser.TypeArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#typeArgument}.
	 * @param ctx the parse tree
	 */
	void exitTypeArgument(@NotNull AspectJParser.TypeArgumentContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#advice}.
	 * @param ctx the parse tree
	 */
	void enterAdvice(@NotNull AspectJParser.AdviceContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#advice}.
	 * @param ctx the parse tree
	 */
	void exitAdvice(@NotNull AspectJParser.AdviceContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#referencePointcut}.
	 * @param ctx the parse tree
	 */
	void enterReferencePointcut(@NotNull AspectJParser.ReferencePointcutContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#referencePointcut}.
	 * @param ctx the parse tree
	 */
	void exitReferencePointcut(@NotNull AspectJParser.ReferencePointcutContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#typePattern}.
	 * @param ctx the parse tree
	 */
	void enterTypePattern(@NotNull AspectJParser.TypePatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#typePattern}.
	 * @param ctx the parse tree
	 */
	void exitTypePattern(@NotNull AspectJParser.TypePatternContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#typeDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterTypeDeclaration(@NotNull AspectJParser.TypeDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#typeDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitTypeDeclaration(@NotNull AspectJParser.TypeDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#typeOrIdentifier}.
	 * @param ctx the parse tree
	 */
	void enterTypeOrIdentifier(@NotNull AspectJParser.TypeOrIdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#typeOrIdentifier}.
	 * @param ctx the parse tree
	 */
	void exitTypeOrIdentifier(@NotNull AspectJParser.TypeOrIdentifierContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#genericConstructorDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterGenericConstructorDeclaration(@NotNull AspectJParser.GenericConstructorDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#genericConstructorDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitGenericConstructorDeclaration(@NotNull AspectJParser.GenericConstructorDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#classDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterClassDeclaration(@NotNull AspectJParser.ClassDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#classDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitClassDeclaration(@NotNull AspectJParser.ClassDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#SetPointcut}.
	 * @param ctx the parse tree
	 */
	void enterSetPointcut(@NotNull AspectJParser.SetPointcutContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#SetPointcut}.
	 * @param ctx the parse tree
	 */
	void exitSetPointcut(@NotNull AspectJParser.SetPointcutContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#dottedNamePattern}.
	 * @param ctx the parse tree
	 */
	void enterDottedNamePattern(@NotNull AspectJParser.DottedNamePatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#dottedNamePattern}.
	 * @param ctx the parse tree
	 */
	void exitDottedNamePattern(@NotNull AspectJParser.DottedNamePatternContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#enumConstant}.
	 * @param ctx the parse tree
	 */
	void enterEnumConstant(@NotNull AspectJParser.EnumConstantContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#enumConstant}.
	 * @param ctx the parse tree
	 */
	void exitEnumConstant(@NotNull AspectJParser.EnumConstantContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#IfPointcut}.
	 * @param ctx the parse tree
	 */
	void enterIfPointcut(@NotNull AspectJParser.IfPointcutContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#IfPointcut}.
	 * @param ctx the parse tree
	 */
	void exitIfPointcut(@NotNull AspectJParser.IfPointcutContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#PreInitializationPointcut}.
	 * @param ctx the parse tree
	 */
	void enterPreInitializationPointcut(@NotNull AspectJParser.PreInitializationPointcutContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#PreInitializationPointcut}.
	 * @param ctx the parse tree
	 */
	void exitPreInitializationPointcut(@NotNull AspectJParser.PreInitializationPointcutContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(@NotNull AspectJParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(@NotNull AspectJParser.StatementContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#fieldModifier}.
	 * @param ctx the parse tree
	 */
	void enterFieldModifier(@NotNull AspectJParser.FieldModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#fieldModifier}.
	 * @param ctx the parse tree
	 */
	void exitFieldModifier(@NotNull AspectJParser.FieldModifierContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#importDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterImportDeclaration(@NotNull AspectJParser.ImportDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#importDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitImportDeclaration(@NotNull AspectJParser.ImportDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#primitiveType}.
	 * @param ctx the parse tree
	 */
	void enterPrimitiveType(@NotNull AspectJParser.PrimitiveTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#primitiveType}.
	 * @param ctx the parse tree
	 */
	void exitPrimitiveType(@NotNull AspectJParser.PrimitiveTypeContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#StaticInitializationPointcut}.
	 * @param ctx the parse tree
	 */
	void enterStaticInitializationPointcut(@NotNull AspectJParser.StaticInitializationPointcutContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#StaticInitializationPointcut}.
	 * @param ctx the parse tree
	 */
	void exitStaticInitializationPointcut(@NotNull AspectJParser.StaticInitializationPointcutContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#dotOrDotDot}.
	 * @param ctx the parse tree
	 */
	void enterDotOrDotDot(@NotNull AspectJParser.DotOrDotDotContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#dotOrDotDot}.
	 * @param ctx the parse tree
	 */
	void exitDotOrDotDot(@NotNull AspectJParser.DotOrDotDotContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#interfaceDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceDeclaration(@NotNull AspectJParser.InterfaceDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#interfaceDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceDeclaration(@NotNull AspectJParser.InterfaceDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#localVariableDeclarationStatement}.
	 * @param ctx the parse tree
	 */
	void enterLocalVariableDeclarationStatement(@NotNull AspectJParser.LocalVariableDeclarationStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#localVariableDeclarationStatement}.
	 * @param ctx the parse tree
	 */
	void exitLocalVariableDeclarationStatement(@NotNull AspectJParser.LocalVariableDeclarationStatementContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#blockStatement}.
	 * @param ctx the parse tree
	 */
	void enterBlockStatement(@NotNull AspectJParser.BlockStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#blockStatement}.
	 * @param ctx the parse tree
	 */
	void exitBlockStatement(@NotNull AspectJParser.BlockStatementContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#constantDeclarator}.
	 * @param ctx the parse tree
	 */
	void enterConstantDeclarator(@NotNull AspectJParser.ConstantDeclaratorContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#constantDeclarator}.
	 * @param ctx the parse tree
	 */
	void exitConstantDeclarator(@NotNull AspectJParser.ConstantDeclaratorContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#fieldDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterFieldDeclaration(@NotNull AspectJParser.FieldDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#fieldDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitFieldDeclaration(@NotNull AspectJParser.FieldDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#resources}.
	 * @param ctx the parse tree
	 */
	void enterResources(@NotNull AspectJParser.ResourcesContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#resources}.
	 * @param ctx the parse tree
	 */
	void exitResources(@NotNull AspectJParser.ResourcesContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#statementExpression}.
	 * @param ctx the parse tree
	 */
	void enterStatementExpression(@NotNull AspectJParser.StatementExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#statementExpression}.
	 * @param ctx the parse tree
	 */
	void exitStatementExpression(@NotNull AspectJParser.StatementExpressionContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#aspectBodyDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterAspectBodyDeclaration(@NotNull AspectJParser.AspectBodyDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#aspectBodyDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitAspectBodyDeclaration(@NotNull AspectJParser.AspectBodyDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#interfaceMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceMethodDeclaration(@NotNull AspectJParser.InterfaceMethodDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#interfaceMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceMethodDeclaration(@NotNull AspectJParser.InterfaceMethodDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#argsPattern}.
	 * @param ctx the parse tree
	 */
	void enterArgsPattern(@NotNull AspectJParser.ArgsPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#argsPattern}.
	 * @param ctx the parse tree
	 */
	void exitArgsPattern(@NotNull AspectJParser.ArgsPatternContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#simpleNamePattern}.
	 * @param ctx the parse tree
	 */
	void enterSimpleNamePattern(@NotNull AspectJParser.SimpleNamePatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#simpleNamePattern}.
	 * @param ctx the parse tree
	 */
	void exitSimpleNamePattern(@NotNull AspectJParser.SimpleNamePatternContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#packageDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterPackageDeclaration(@NotNull AspectJParser.PackageDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#packageDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitPackageDeclaration(@NotNull AspectJParser.PackageDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#elementValuePairs}.
	 * @param ctx the parse tree
	 */
	void enterElementValuePairs(@NotNull AspectJParser.ElementValuePairsContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#elementValuePairs}.
	 * @param ctx the parse tree
	 */
	void exitElementValuePairs(@NotNull AspectJParser.ElementValuePairsContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#localVariableDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterLocalVariableDeclaration(@NotNull AspectJParser.LocalVariableDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#localVariableDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitLocalVariableDeclaration(@NotNull AspectJParser.LocalVariableDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#CallPointcut}.
	 * @param ctx the parse tree
	 */
	void enterCallPointcut(@NotNull AspectJParser.CallPointcutContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#CallPointcut}.
	 * @param ctx the parse tree
	 */
	void exitCallPointcut(@NotNull AspectJParser.CallPointcutContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#WithinPointcut}.
	 * @param ctx the parse tree
	 */
	void enterWithinPointcut(@NotNull AspectJParser.WithinPointcutContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#WithinPointcut}.
	 * @param ctx the parse tree
	 */
	void exitWithinPointcut(@NotNull AspectJParser.WithinPointcutContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#nonWildcardTypeArguments}.
	 * @param ctx the parse tree
	 */
	void enterNonWildcardTypeArguments(@NotNull AspectJParser.NonWildcardTypeArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#nonWildcardTypeArguments}.
	 * @param ctx the parse tree
	 */
	void exitNonWildcardTypeArguments(@NotNull AspectJParser.NonWildcardTypeArgumentsContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#interfaceMemberDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterInterfaceMemberDeclaration(@NotNull AspectJParser.InterfaceMemberDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#interfaceMemberDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitInterfaceMemberDeclaration(@NotNull AspectJParser.InterfaceMemberDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#switchLabel}.
	 * @param ctx the parse tree
	 */
	void enterSwitchLabel(@NotNull AspectJParser.SwitchLabelContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#switchLabel}.
	 * @param ctx the parse tree
	 */
	void exitSwitchLabel(@NotNull AspectJParser.SwitchLabelContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#forInit}.
	 * @param ctx the parse tree
	 */
	void enterForInit(@NotNull AspectJParser.ForInitContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#forInit}.
	 * @param ctx the parse tree
	 */
	void exitForInit(@NotNull AspectJParser.ForInitContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#formalParameters}.
	 * @param ctx the parse tree
	 */
	void enterFormalParameters(@NotNull AspectJParser.FormalParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#formalParameters}.
	 * @param ctx the parse tree
	 */
	void exitFormalParameters(@NotNull AspectJParser.FormalParametersContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#arguments}.
	 * @param ctx the parse tree
	 */
	void enterArguments(@NotNull AspectJParser.ArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#arguments}.
	 * @param ctx the parse tree
	 */
	void exitArguments(@NotNull AspectJParser.ArgumentsContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#ThisPointcutPointcut}.
	 * @param ctx the parse tree
	 */
	void enterThisPointcutPointcut(@NotNull AspectJParser.ThisPointcutPointcutContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#ThisPointcutPointcut}.
	 * @param ctx the parse tree
	 */
	void exitThisPointcutPointcut(@NotNull AspectJParser.ThisPointcutPointcutContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#genericMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterGenericMethodDeclaration(@NotNull AspectJParser.GenericMethodDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#genericMethodDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitGenericMethodDeclaration(@NotNull AspectJParser.GenericMethodDeclarationContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#typeArgumentsOrDiamond}.
	 * @param ctx the parse tree
	 */
	void enterTypeArgumentsOrDiamond(@NotNull AspectJParser.TypeArgumentsOrDiamondContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#typeArgumentsOrDiamond}.
	 * @param ctx the parse tree
	 */
	void exitTypeArgumentsOrDiamond(@NotNull AspectJParser.TypeArgumentsOrDiamondContext ctx);

	/**
	 * Enter a parse tree produced by {@link AspectJParser#HandlerPointcut}.
	 * @param ctx the parse tree
	 */
	void enterHandlerPointcut(@NotNull AspectJParser.HandlerPointcutContext ctx);
	/**
	 * Exit a parse tree produced by {@link AspectJParser#HandlerPointcut}.
	 * @param ctx the parse tree
	 */
	void exitHandlerPointcut(@NotNull AspectJParser.HandlerPointcutContext ctx);
}