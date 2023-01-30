/*
 * Copyright 2022 the original author or authors.
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
// Generated from java-escape by ANTLR 4.11.1
package org.openrewrite.hcl.internal.grammar;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link HCLParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface HCLParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link HCLParser#configFile}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConfigFile(HCLParser.ConfigFileContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBody(HCLParser.BodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#bodyContent}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBodyContent(HCLParser.BodyContentContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#attribute}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttribute(HCLParser.AttributeContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlock(HCLParser.BlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#blockLabel}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlockLabel(HCLParser.BlockLabelContext ctx);
	/**
	 * Visit a parse tree produced by the {@code OperationExpression}
	 * labeled alternative in {@link HCLParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOperationExpression(HCLParser.OperationExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ConditionalExpression}
	 * labeled alternative in {@link HCLParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConditionalExpression(HCLParser.ConditionalExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ExpressionTerm}
	 * labeled alternative in {@link HCLParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressionTerm(HCLParser.ExpressionTermContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ParentheticalExpression}
	 * labeled alternative in {@link HCLParser#exprTerm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParentheticalExpression(HCLParser.ParentheticalExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code AttributeAccessExpression}
	 * labeled alternative in {@link HCLParser#exprTerm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttributeAccessExpression(HCLParser.AttributeAccessExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LiteralExpression}
	 * labeled alternative in {@link HCLParser#exprTerm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteralExpression(HCLParser.LiteralExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TemplateExpression}
	 * labeled alternative in {@link HCLParser#exprTerm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTemplateExpression(HCLParser.TemplateExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code VariableExpression}
	 * labeled alternative in {@link HCLParser#exprTerm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableExpression(HCLParser.VariableExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SplatExpression}
	 * labeled alternative in {@link HCLParser#exprTerm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSplatExpression(HCLParser.SplatExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code IndexAccessExpression}
	 * labeled alternative in {@link HCLParser#exprTerm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndexAccessExpression(HCLParser.IndexAccessExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ForExpression}
	 * labeled alternative in {@link HCLParser#exprTerm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForExpression(HCLParser.ForExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FunctionCallExpression}
	 * labeled alternative in {@link HCLParser#exprTerm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionCallExpression(HCLParser.FunctionCallExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CollectionValueExpression}
	 * labeled alternative in {@link HCLParser#exprTerm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCollectionValueExpression(HCLParser.CollectionValueExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#blockExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlockExpr(HCLParser.BlockExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#literalValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteralValue(HCLParser.LiteralValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#collectionValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCollectionValue(HCLParser.CollectionValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#tuple}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTuple(HCLParser.TupleContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#object}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObject(HCLParser.ObjectContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#objectelem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObjectelem(HCLParser.ObjectelemContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#forExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForExpr(HCLParser.ForExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#forTupleExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForTupleExpr(HCLParser.ForTupleExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#forObjectExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForObjectExpr(HCLParser.ForObjectExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#forIntro}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForIntro(HCLParser.ForIntroContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#forCond}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForCond(HCLParser.ForCondContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#variableExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableExpr(HCLParser.VariableExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#functionCall}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionCall(HCLParser.FunctionCallContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#arguments}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArguments(HCLParser.ArgumentsContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#index}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex(HCLParser.IndexContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#getAttr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGetAttr(HCLParser.GetAttrContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#splat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSplat(HCLParser.SplatContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#attrSplat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttrSplat(HCLParser.AttrSplatContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#fullSplat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFullSplat(HCLParser.FullSplatContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#operation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOperation(HCLParser.OperationContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#unaryOp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryOp(HCLParser.UnaryOpContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#binaryOp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBinaryOp(HCLParser.BinaryOpContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#binaryOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBinaryOperator(HCLParser.BinaryOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#compareOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompareOperator(HCLParser.CompareOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#arithmeticOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArithmeticOperator(HCLParser.ArithmeticOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#logicOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogicOperator(HCLParser.LogicOperatorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Heredoc}
	 * labeled alternative in {@link HCLParser#templateExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHeredoc(HCLParser.HeredocContext ctx);
	/**
	 * Visit a parse tree produced by the {@code QuotedTemplate}
	 * labeled alternative in {@link HCLParser#templateExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuotedTemplate(HCLParser.QuotedTemplateContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#heredocTemplatePart}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHeredocTemplatePart(HCLParser.HeredocTemplatePartContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#heredocLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHeredocLiteral(HCLParser.HeredocLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#quotedTemplatePart}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuotedTemplatePart(HCLParser.QuotedTemplatePartContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#stringLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringLiteral(HCLParser.StringLiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link HCLParser#templateInterpolation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTemplateInterpolation(HCLParser.TemplateInterpolationContext ctx);
}