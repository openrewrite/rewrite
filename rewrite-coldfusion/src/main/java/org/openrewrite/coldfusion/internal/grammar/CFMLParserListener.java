/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Generated from rewrite-coldfusion/src/main/antlr/CFMLParser.g4 by ANTLR 4.13.2
package org.openrewrite.coldfusion.internal.grammar;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link CFMLParser}.
 */
public interface CFMLParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link CFMLParser#template}.
	 * @param ctx the parse tree
	 */
	void enterTemplate(CFMLParser.TemplateContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#template}.
	 * @param ctx the parse tree
	 */
	void exitTemplate(CFMLParser.TemplateContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#scriptComponent}.
	 * @param ctx the parse tree
	 */
	void enterScriptComponent(CFMLParser.ScriptComponentContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#scriptComponent}.
	 * @param ctx the parse tree
	 */
	void exitScriptComponent(CFMLParser.ScriptComponentContext ctx);
	/**
	 * Enter a parse tree produced by the {@code textElement}
	 * labeled alternative in {@link CFMLParser#templateElement}.
	 * @param ctx the parse tree
	 */
	void enterTextElement(CFMLParser.TextElementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code textElement}
	 * labeled alternative in {@link CFMLParser#templateElement}.
	 * @param ctx the parse tree
	 */
	void exitTextElement(CFMLParser.TextElementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code commentElement}
	 * labeled alternative in {@link CFMLParser#templateElement}.
	 * @param ctx the parse tree
	 */
	void enterCommentElement(CFMLParser.CommentElementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code commentElement}
	 * labeled alternative in {@link CFMLParser#templateElement}.
	 * @param ctx the parse tree
	 */
	void exitCommentElement(CFMLParser.CommentElementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code interpolationElement}
	 * labeled alternative in {@link CFMLParser#templateElement}.
	 * @param ctx the parse tree
	 */
	void enterInterpolationElement(CFMLParser.InterpolationElementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code interpolationElement}
	 * labeled alternative in {@link CFMLParser#templateElement}.
	 * @param ctx the parse tree
	 */
	void exitInterpolationElement(CFMLParser.InterpolationElementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code startTagElement}
	 * labeled alternative in {@link CFMLParser#templateElement}.
	 * @param ctx the parse tree
	 */
	void enterStartTagElement(CFMLParser.StartTagElementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code startTagElement}
	 * labeled alternative in {@link CFMLParser#templateElement}.
	 * @param ctx the parse tree
	 */
	void exitStartTagElement(CFMLParser.StartTagElementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code cfscriptElement}
	 * labeled alternative in {@link CFMLParser#templateElement}.
	 * @param ctx the parse tree
	 */
	void enterCfscriptElement(CFMLParser.CfscriptElementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code cfscriptElement}
	 * labeled alternative in {@link CFMLParser#templateElement}.
	 * @param ctx the parse tree
	 */
	void exitCfscriptElement(CFMLParser.CfscriptElementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code endTagElement}
	 * labeled alternative in {@link CFMLParser#templateElement}.
	 * @param ctx the parse tree
	 */
	void enterEndTagElement(CFMLParser.EndTagElementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code endTagElement}
	 * labeled alternative in {@link CFMLParser#templateElement}.
	 * @param ctx the parse tree
	 */
	void exitEndTagElement(CFMLParser.EndTagElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#startTag}.
	 * @param ctx the parse tree
	 */
	void enterStartTag(CFMLParser.StartTagContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#startTag}.
	 * @param ctx the parse tree
	 */
	void exitStartTag(CFMLParser.StartTagContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#cfscriptTag}.
	 * @param ctx the parse tree
	 */
	void enterCfscriptTag(CFMLParser.CfscriptTagContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#cfscriptTag}.
	 * @param ctx the parse tree
	 */
	void exitCfscriptTag(CFMLParser.CfscriptTagContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#endTag}.
	 * @param ctx the parse tree
	 */
	void enterEndTag(CFMLParser.EndTagContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#endTag}.
	 * @param ctx the parse tree
	 */
	void exitEndTag(CFMLParser.EndTagContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#tagAttribute}.
	 * @param ctx the parse tree
	 */
	void enterTagAttribute(CFMLParser.TagAttributeContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#tagAttribute}.
	 * @param ctx the parse tree
	 */
	void exitTagAttribute(CFMLParser.TagAttributeContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#attributeValue}.
	 * @param ctx the parse tree
	 */
	void enterAttributeValue(CFMLParser.AttributeValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#attributeValue}.
	 * @param ctx the parse tree
	 */
	void exitAttributeValue(CFMLParser.AttributeValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#interpolation}.
	 * @param ctx the parse tree
	 */
	void enterInterpolation(CFMLParser.InterpolationContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#interpolation}.
	 * @param ctx the parse tree
	 */
	void exitInterpolation(CFMLParser.InterpolationContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#paddedStatement}.
	 * @param ctx the parse tree
	 */
	void enterPaddedStatement(CFMLParser.PaddedStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#paddedStatement}.
	 * @param ctx the parse tree
	 */
	void exitPaddedStatement(CFMLParser.PaddedStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(CFMLParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(CFMLParser.StatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#block}.
	 * @param ctx the parse tree
	 */
	void enterBlock(CFMLParser.BlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#block}.
	 * @param ctx the parse tree
	 */
	void exitBlock(CFMLParser.BlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#bodyBlock}.
	 * @param ctx the parse tree
	 */
	void enterBodyBlock(CFMLParser.BodyBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#bodyBlock}.
	 * @param ctx the parse tree
	 */
	void exitBodyBlock(CFMLParser.BodyBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#ifStatement}.
	 * @param ctx the parse tree
	 */
	void enterIfStatement(CFMLParser.IfStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#ifStatement}.
	 * @param ctx the parse tree
	 */
	void exitIfStatement(CFMLParser.IfStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#ifTail}.
	 * @param ctx the parse tree
	 */
	void enterIfTail(CFMLParser.IfTailContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#ifTail}.
	 * @param ctx the parse tree
	 */
	void exitIfTail(CFMLParser.IfTailContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#elseClause}.
	 * @param ctx the parse tree
	 */
	void enterElseClause(CFMLParser.ElseClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#elseClause}.
	 * @param ctx the parse tree
	 */
	void exitElseClause(CFMLParser.ElseClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#forInStatement}.
	 * @param ctx the parse tree
	 */
	void enterForInStatement(CFMLParser.ForInStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#forInStatement}.
	 * @param ctx the parse tree
	 */
	void exitForInStatement(CFMLParser.ForInStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#forInVariable}.
	 * @param ctx the parse tree
	 */
	void enterForInVariable(CFMLParser.ForInVariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#forInVariable}.
	 * @param ctx the parse tree
	 */
	void exitForInVariable(CFMLParser.ForInVariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#forStatement}.
	 * @param ctx the parse tree
	 */
	void enterForStatement(CFMLParser.ForStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#forStatement}.
	 * @param ctx the parse tree
	 */
	void exitForStatement(CFMLParser.ForStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#forClauses}.
	 * @param ctx the parse tree
	 */
	void enterForClauses(CFMLParser.ForClausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#forClauses}.
	 * @param ctx the parse tree
	 */
	void exitForClauses(CFMLParser.ForClausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#whileStatement}.
	 * @param ctx the parse tree
	 */
	void enterWhileStatement(CFMLParser.WhileStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#whileStatement}.
	 * @param ctx the parse tree
	 */
	void exitWhileStatement(CFMLParser.WhileStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#doWhileStatement}.
	 * @param ctx the parse tree
	 */
	void enterDoWhileStatement(CFMLParser.DoWhileStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#doWhileStatement}.
	 * @param ctx the parse tree
	 */
	void exitDoWhileStatement(CFMLParser.DoWhileStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#switchStatement}.
	 * @param ctx the parse tree
	 */
	void enterSwitchStatement(CFMLParser.SwitchStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#switchStatement}.
	 * @param ctx the parse tree
	 */
	void exitSwitchStatement(CFMLParser.SwitchStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#caseClause}.
	 * @param ctx the parse tree
	 */
	void enterCaseClause(CFMLParser.CaseClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#caseClause}.
	 * @param ctx the parse tree
	 */
	void exitCaseClause(CFMLParser.CaseClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#tryStatement}.
	 * @param ctx the parse tree
	 */
	void enterTryStatement(CFMLParser.TryStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#tryStatement}.
	 * @param ctx the parse tree
	 */
	void exitTryStatement(CFMLParser.TryStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#catchClause}.
	 * @param ctx the parse tree
	 */
	void enterCatchClause(CFMLParser.CatchClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#catchClause}.
	 * @param ctx the parse tree
	 */
	void exitCatchClause(CFMLParser.CatchClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#catchParameter}.
	 * @param ctx the parse tree
	 */
	void enterCatchParameter(CFMLParser.CatchParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#catchParameter}.
	 * @param ctx the parse tree
	 */
	void exitCatchParameter(CFMLParser.CatchParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#catchType}.
	 * @param ctx the parse tree
	 */
	void enterCatchType(CFMLParser.CatchTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#catchType}.
	 * @param ctx the parse tree
	 */
	void exitCatchType(CFMLParser.CatchTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#returnStatement}.
	 * @param ctx the parse tree
	 */
	void enterReturnStatement(CFMLParser.ReturnStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#returnStatement}.
	 * @param ctx the parse tree
	 */
	void exitReturnStatement(CFMLParser.ReturnStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#breakStatement}.
	 * @param ctx the parse tree
	 */
	void enterBreakStatement(CFMLParser.BreakStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#breakStatement}.
	 * @param ctx the parse tree
	 */
	void exitBreakStatement(CFMLParser.BreakStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#continueStatement}.
	 * @param ctx the parse tree
	 */
	void enterContinueStatement(CFMLParser.ContinueStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#continueStatement}.
	 * @param ctx the parse tree
	 */
	void exitContinueStatement(CFMLParser.ContinueStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#throwStatement}.
	 * @param ctx the parse tree
	 */
	void enterThrowStatement(CFMLParser.ThrowStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#throwStatement}.
	 * @param ctx the parse tree
	 */
	void exitThrowStatement(CFMLParser.ThrowStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#componentDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterComponentDeclaration(CFMLParser.ComponentDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#componentDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitComponentDeclaration(CFMLParser.ComponentDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#propertyDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterPropertyDeclaration(CFMLParser.PropertyDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#propertyDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitPropertyDeclaration(CFMLParser.PropertyDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#importStatement}.
	 * @param ctx the parse tree
	 */
	void enterImportStatement(CFMLParser.ImportStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#importStatement}.
	 * @param ctx the parse tree
	 */
	void exitImportStatement(CFMLParser.ImportStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#staticInitializer}.
	 * @param ctx the parse tree
	 */
	void enterStaticInitializer(CFMLParser.StaticInitializerContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#staticInitializer}.
	 * @param ctx the parse tree
	 */
	void exitStaticInitializer(CFMLParser.StaticInitializerContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#scriptTag}.
	 * @param ctx the parse tree
	 */
	void enterScriptTag(CFMLParser.ScriptTagContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#scriptTag}.
	 * @param ctx the parse tree
	 */
	void exitScriptTag(CFMLParser.ScriptTagContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#expressionStatement}.
	 * @param ctx the parse tree
	 */
	void enterExpressionStatement(CFMLParser.ExpressionStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#expressionStatement}.
	 * @param ctx the parse tree
	 */
	void exitExpressionStatement(CFMLParser.ExpressionStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#functionDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterFunctionDeclaration(CFMLParser.FunctionDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#functionDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitFunctionDeclaration(CFMLParser.FunctionDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#functionModifier}.
	 * @param ctx the parse tree
	 */
	void enterFunctionModifier(CFMLParser.FunctionModifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#functionModifier}.
	 * @param ctx the parse tree
	 */
	void exitFunctionModifier(CFMLParser.FunctionModifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#returnType}.
	 * @param ctx the parse tree
	 */
	void enterReturnType(CFMLParser.ReturnTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#returnType}.
	 * @param ctx the parse tree
	 */
	void exitReturnType(CFMLParser.ReturnTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#parameterList}.
	 * @param ctx the parse tree
	 */
	void enterParameterList(CFMLParser.ParameterListContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#parameterList}.
	 * @param ctx the parse tree
	 */
	void exitParameterList(CFMLParser.ParameterListContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#parameters}.
	 * @param ctx the parse tree
	 */
	void enterParameters(CFMLParser.ParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#parameters}.
	 * @param ctx the parse tree
	 */
	void exitParameters(CFMLParser.ParametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#parameter}.
	 * @param ctx the parse tree
	 */
	void enterParameter(CFMLParser.ParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#parameter}.
	 * @param ctx the parse tree
	 */
	void exitParameter(CFMLParser.ParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#parameterType}.
	 * @param ctx the parse tree
	 */
	void enterParameterType(CFMLParser.ParameterTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#parameterType}.
	 * @param ctx the parse tree
	 */
	void exitParameterType(CFMLParser.ParameterTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#headerAttribute}.
	 * @param ctx the parse tree
	 */
	void enterHeaderAttribute(CFMLParser.HeaderAttributeContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#headerAttribute}.
	 * @param ctx the parse tree
	 */
	void exitHeaderAttribute(CFMLParser.HeaderAttributeContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#statementAttribute}.
	 * @param ctx the parse tree
	 */
	void enterStatementAttribute(CFMLParser.StatementAttributeContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#statementAttribute}.
	 * @param ctx the parse tree
	 */
	void exitStatementAttribute(CFMLParser.StatementAttributeContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#namedAttribute}.
	 * @param ctx the parse tree
	 */
	void enterNamedAttribute(CFMLParser.NamedAttributeContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#namedAttribute}.
	 * @param ctx the parse tree
	 */
	void exitNamedAttribute(CFMLParser.NamedAttributeContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#attributeName}.
	 * @param ctx the parse tree
	 */
	void enterAttributeName(CFMLParser.AttributeNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#attributeName}.
	 * @param ctx the parse tree
	 */
	void exitAttributeName(CFMLParser.AttributeNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(CFMLParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(CFMLParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#assignmentOperator}.
	 * @param ctx the parse tree
	 */
	void enterAssignmentOperator(CFMLParser.AssignmentOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#assignmentOperator}.
	 * @param ctx the parse tree
	 */
	void exitAssignmentOperator(CFMLParser.AssignmentOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#ternaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterTernaryExpression(CFMLParser.TernaryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#ternaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitTernaryExpression(CFMLParser.TernaryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#operation}.
	 * @param ctx the parse tree
	 */
	void enterOperation(CFMLParser.OperationContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#operation}.
	 * @param ctx the parse tree
	 */
	void exitOperation(CFMLParser.OperationContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void enterComparisonOperator(CFMLParser.ComparisonOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void exitComparisonOperator(CFMLParser.ComparisonOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#unaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterUnaryExpression(CFMLParser.UnaryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#unaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitUnaryExpression(CFMLParser.UnaryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#postfixExpression}.
	 * @param ctx the parse tree
	 */
	void enterPostfixExpression(CFMLParser.PostfixExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#postfixExpression}.
	 * @param ctx the parse tree
	 */
	void exitPostfixExpression(CFMLParser.PostfixExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#postfixOperator}.
	 * @param ctx the parse tree
	 */
	void enterPostfixOperator(CFMLParser.PostfixOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#postfixOperator}.
	 * @param ctx the parse tree
	 */
	void exitPostfixOperator(CFMLParser.PostfixOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#memberName}.
	 * @param ctx the parse tree
	 */
	void enterMemberName(CFMLParser.MemberNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#memberName}.
	 * @param ctx the parse tree
	 */
	void exitMemberName(CFMLParser.MemberNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#arguments}.
	 * @param ctx the parse tree
	 */
	void enterArguments(CFMLParser.ArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#arguments}.
	 * @param ctx the parse tree
	 */
	void exitArguments(CFMLParser.ArgumentsContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#argumentList}.
	 * @param ctx the parse tree
	 */
	void enterArgumentList(CFMLParser.ArgumentListContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#argumentList}.
	 * @param ctx the parse tree
	 */
	void exitArgumentList(CFMLParser.ArgumentListContext ctx);
	/**
	 * Enter a parse tree produced by the {@code namedArgument}
	 * labeled alternative in {@link CFMLParser#argument}.
	 * @param ctx the parse tree
	 */
	void enterNamedArgument(CFMLParser.NamedArgumentContext ctx);
	/**
	 * Exit a parse tree produced by the {@code namedArgument}
	 * labeled alternative in {@link CFMLParser#argument}.
	 * @param ctx the parse tree
	 */
	void exitNamedArgument(CFMLParser.NamedArgumentContext ctx);
	/**
	 * Enter a parse tree produced by the {@code positionalArgument}
	 * labeled alternative in {@link CFMLParser#argument}.
	 * @param ctx the parse tree
	 */
	void enterPositionalArgument(CFMLParser.PositionalArgumentContext ctx);
	/**
	 * Exit a parse tree produced by the {@code positionalArgument}
	 * labeled alternative in {@link CFMLParser#argument}.
	 * @param ctx the parse tree
	 */
	void exitPositionalArgument(CFMLParser.PositionalArgumentContext ctx);
	/**
	 * Enter a parse tree produced by the {@code numberPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 */
	void enterNumberPrimary(CFMLParser.NumberPrimaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code numberPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 */
	void exitNumberPrimary(CFMLParser.NumberPrimaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code booleanPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 */
	void enterBooleanPrimary(CFMLParser.BooleanPrimaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code booleanPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 */
	void exitBooleanPrimary(CFMLParser.BooleanPrimaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code nullPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 */
	void enterNullPrimary(CFMLParser.NullPrimaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code nullPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 */
	void exitNullPrimary(CFMLParser.NullPrimaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code stringPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 */
	void enterStringPrimary(CFMLParser.StringPrimaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code stringPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 */
	void exitStringPrimary(CFMLParser.StringPrimaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code interpolationPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 */
	void enterInterpolationPrimary(CFMLParser.InterpolationPrimaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code interpolationPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 */
	void exitInterpolationPrimary(CFMLParser.InterpolationPrimaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arrowFunctionPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 */
	void enterArrowFunctionPrimary(CFMLParser.ArrowFunctionPrimaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arrowFunctionPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 */
	void exitArrowFunctionPrimary(CFMLParser.ArrowFunctionPrimaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parenthesizedPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 */
	void enterParenthesizedPrimary(CFMLParser.ParenthesizedPrimaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parenthesizedPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 */
	void exitParenthesizedPrimary(CFMLParser.ParenthesizedPrimaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code emptyBracketPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 */
	void enterEmptyBracketPrimary(CFMLParser.EmptyBracketPrimaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code emptyBracketPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 */
	void exitEmptyBracketPrimary(CFMLParser.EmptyBracketPrimaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code orderedStructPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 */
	void enterOrderedStructPrimary(CFMLParser.OrderedStructPrimaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code orderedStructPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 */
	void exitOrderedStructPrimary(CFMLParser.OrderedStructPrimaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code arrayPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 */
	void enterArrayPrimary(CFMLParser.ArrayPrimaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code arrayPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 */
	void exitArrayPrimary(CFMLParser.ArrayPrimaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code emptyStructPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 */
	void enterEmptyStructPrimary(CFMLParser.EmptyStructPrimaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code emptyStructPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 */
	void exitEmptyStructPrimary(CFMLParser.EmptyStructPrimaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code structPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 */
	void enterStructPrimary(CFMLParser.StructPrimaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code structPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 */
	void exitStructPrimary(CFMLParser.StructPrimaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code closurePrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 */
	void enterClosurePrimary(CFMLParser.ClosurePrimaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code closurePrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 */
	void exitClosurePrimary(CFMLParser.ClosurePrimaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code newPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 */
	void enterNewPrimary(CFMLParser.NewPrimaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code newPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 */
	void exitNewPrimary(CFMLParser.NewPrimaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code varPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 */
	void enterVarPrimary(CFMLParser.VarPrimaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code varPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 */
	void exitVarPrimary(CFMLParser.VarPrimaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code identifierPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 */
	void enterIdentifierPrimary(CFMLParser.IdentifierPrimaryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code identifierPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 */
	void exitIdentifierPrimary(CFMLParser.IdentifierPrimaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#structEntry}.
	 * @param ctx the parse tree
	 */
	void enterStructEntry(CFMLParser.StructEntryContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#structEntry}.
	 * @param ctx the parse tree
	 */
	void exitStructEntry(CFMLParser.StructEntryContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#structKey}.
	 * @param ctx the parse tree
	 */
	void enterStructKey(CFMLParser.StructKeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#structKey}.
	 * @param ctx the parse tree
	 */
	void exitStructKey(CFMLParser.StructKeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#closure}.
	 * @param ctx the parse tree
	 */
	void enterClosure(CFMLParser.ClosureContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#closure}.
	 * @param ctx the parse tree
	 */
	void exitClosure(CFMLParser.ClosureContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#arrowFunction}.
	 * @param ctx the parse tree
	 */
	void enterArrowFunction(CFMLParser.ArrowFunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#arrowFunction}.
	 * @param ctx the parse tree
	 */
	void exitArrowFunction(CFMLParser.ArrowFunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#arrowBody}.
	 * @param ctx the parse tree
	 */
	void enterArrowBody(CFMLParser.ArrowBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#arrowBody}.
	 * @param ctx the parse tree
	 */
	void exitArrowBody(CFMLParser.ArrowBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#typeName}.
	 * @param ctx the parse tree
	 */
	void enterTypeName(CFMLParser.TypeNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#typeName}.
	 * @param ctx the parse tree
	 */
	void exitTypeName(CFMLParser.TypeNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#dottedName}.
	 * @param ctx the parse tree
	 */
	void enterDottedName(CFMLParser.DottedNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#dottedName}.
	 * @param ctx the parse tree
	 */
	void exitDottedName(CFMLParser.DottedNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#stringLiteral}.
	 * @param ctx the parse tree
	 */
	void enterStringLiteral(CFMLParser.StringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#stringLiteral}.
	 * @param ctx the parse tree
	 */
	void exitStringLiteral(CFMLParser.StringLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterIdentifier(CFMLParser.IdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitIdentifier(CFMLParser.IdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link CFMLParser#anyWord}.
	 * @param ctx the parse tree
	 */
	void enterAnyWord(CFMLParser.AnyWordContext ctx);
	/**
	 * Exit a parse tree produced by {@link CFMLParser#anyWord}.
	 * @param ctx the parse tree
	 */
	void exitAnyWord(CFMLParser.AnyWordContext ctx);
}
