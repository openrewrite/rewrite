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
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link CFMLParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface CFMLParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link CFMLParser#template}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTemplate(CFMLParser.TemplateContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#scriptComponent}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScriptComponent(CFMLParser.ScriptComponentContext ctx);
	/**
	 * Visit a parse tree produced by the {@code textElement}
	 * labeled alternative in {@link CFMLParser#templateElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTextElement(CFMLParser.TextElementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code commentElement}
	 * labeled alternative in {@link CFMLParser#templateElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommentElement(CFMLParser.CommentElementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code interpolationElement}
	 * labeled alternative in {@link CFMLParser#templateElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterpolationElement(CFMLParser.InterpolationElementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code startTagElement}
	 * labeled alternative in {@link CFMLParser#templateElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStartTagElement(CFMLParser.StartTagElementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code cfscriptElement}
	 * labeled alternative in {@link CFMLParser#templateElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCfscriptElement(CFMLParser.CfscriptElementContext ctx);
	/**
	 * Visit a parse tree produced by the {@code endTagElement}
	 * labeled alternative in {@link CFMLParser#templateElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEndTagElement(CFMLParser.EndTagElementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#startTag}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStartTag(CFMLParser.StartTagContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#cfscriptTag}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCfscriptTag(CFMLParser.CfscriptTagContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#endTag}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEndTag(CFMLParser.EndTagContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#tagAttribute}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTagAttribute(CFMLParser.TagAttributeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#attributeValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttributeValue(CFMLParser.AttributeValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#interpolation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterpolation(CFMLParser.InterpolationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#paddedStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPaddedStatement(CFMLParser.PaddedStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(CFMLParser.StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlock(CFMLParser.BlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#bodyBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBodyBlock(CFMLParser.BodyBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#ifStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfStatement(CFMLParser.IfStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#ifTail}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfTail(CFMLParser.IfTailContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#elseClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElseClause(CFMLParser.ElseClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#forInStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForInStatement(CFMLParser.ForInStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#forInVariable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForInVariable(CFMLParser.ForInVariableContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#forStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForStatement(CFMLParser.ForStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#forClauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForClauses(CFMLParser.ForClausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#whileStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhileStatement(CFMLParser.WhileStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#doWhileStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDoWhileStatement(CFMLParser.DoWhileStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#switchStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitchStatement(CFMLParser.SwitchStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#caseClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCaseClause(CFMLParser.CaseClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#tryStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTryStatement(CFMLParser.TryStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#catchClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCatchClause(CFMLParser.CatchClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#catchParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCatchParameter(CFMLParser.CatchParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#catchType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCatchType(CFMLParser.CatchTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#returnStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnStatement(CFMLParser.ReturnStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#breakStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBreakStatement(CFMLParser.BreakStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#continueStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContinueStatement(CFMLParser.ContinueStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#throwStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitThrowStatement(CFMLParser.ThrowStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#componentDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComponentDeclaration(CFMLParser.ComponentDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#propertyDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPropertyDeclaration(CFMLParser.PropertyDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#importStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportStatement(CFMLParser.ImportStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#staticInitializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStaticInitializer(CFMLParser.StaticInitializerContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#scriptTag}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScriptTag(CFMLParser.ScriptTagContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#expressionStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionStatement(CFMLParser.ExpressionStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#functionDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionDeclaration(CFMLParser.FunctionDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#functionModifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionModifier(CFMLParser.FunctionModifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#returnType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnType(CFMLParser.ReturnTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#parameterList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameterList(CFMLParser.ParameterListContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#parameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameters(CFMLParser.ParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#parameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameter(CFMLParser.ParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#parameterType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameterType(CFMLParser.ParameterTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#headerAttribute}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHeaderAttribute(CFMLParser.HeaderAttributeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#statementAttribute}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatementAttribute(CFMLParser.StatementAttributeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#namedAttribute}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedAttribute(CFMLParser.NamedAttributeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#attributeName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttributeName(CFMLParser.AttributeNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(CFMLParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#assignmentOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignmentOperator(CFMLParser.AssignmentOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#ternaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTernaryExpression(CFMLParser.TernaryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#operation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOperation(CFMLParser.OperationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#comparisonOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComparisonOperator(CFMLParser.ComparisonOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#unaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryExpression(CFMLParser.UnaryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#postfixExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPostfixExpression(CFMLParser.PostfixExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#postfixOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPostfixOperator(CFMLParser.PostfixOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#memberName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMemberName(CFMLParser.MemberNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#arguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArguments(CFMLParser.ArgumentsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#argumentList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgumentList(CFMLParser.ArgumentListContext ctx);
	/**
	 * Visit a parse tree produced by the {@code namedArgument}
	 * labeled alternative in {@link CFMLParser#argument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamedArgument(CFMLParser.NamedArgumentContext ctx);
	/**
	 * Visit a parse tree produced by the {@code positionalArgument}
	 * labeled alternative in {@link CFMLParser#argument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPositionalArgument(CFMLParser.PositionalArgumentContext ctx);
	/**
	 * Visit a parse tree produced by the {@code numberPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumberPrimary(CFMLParser.NumberPrimaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code booleanPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanPrimary(CFMLParser.BooleanPrimaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code nullPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNullPrimary(CFMLParser.NullPrimaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code stringPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringPrimary(CFMLParser.StringPrimaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code interpolationPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterpolationPrimary(CFMLParser.InterpolationPrimaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code arrowFunctionPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrowFunctionPrimary(CFMLParser.ArrowFunctionPrimaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code parenthesizedPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenthesizedPrimary(CFMLParser.ParenthesizedPrimaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code emptyBracketPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEmptyBracketPrimary(CFMLParser.EmptyBracketPrimaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code orderedStructPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrderedStructPrimary(CFMLParser.OrderedStructPrimaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code arrayPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayPrimary(CFMLParser.ArrayPrimaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code emptyStructPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEmptyStructPrimary(CFMLParser.EmptyStructPrimaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code structPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructPrimary(CFMLParser.StructPrimaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code closurePrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClosurePrimary(CFMLParser.ClosurePrimaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code newPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNewPrimary(CFMLParser.NewPrimaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code varPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarPrimary(CFMLParser.VarPrimaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code identifierPrimary}
	 * labeled alternative in {@link CFMLParser#primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifierPrimary(CFMLParser.IdentifierPrimaryContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#structEntry}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructEntry(CFMLParser.StructEntryContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#structKey}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructKey(CFMLParser.StructKeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#closure}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClosure(CFMLParser.ClosureContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#arrowFunction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrowFunction(CFMLParser.ArrowFunctionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#arrowBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrowBody(CFMLParser.ArrowBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#typeName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeName(CFMLParser.TypeNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#dottedName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDottedName(CFMLParser.DottedNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#stringLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringLiteral(CFMLParser.StringLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifier(CFMLParser.IdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link CFMLParser#anyWord}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnyWord(CFMLParser.AnyWordContext ctx);
}
