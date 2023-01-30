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
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link HCLParser}.
 */
public interface HCLParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link HCLParser#configFile}.
	 * @param ctx the parse tree
	 */
	void enterConfigFile(HCLParser.ConfigFileContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#configFile}.
	 * @param ctx the parse tree
	 */
	void exitConfigFile(HCLParser.ConfigFileContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#body}.
	 * @param ctx the parse tree
	 */
	void enterBody(HCLParser.BodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#body}.
	 * @param ctx the parse tree
	 */
	void exitBody(HCLParser.BodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#bodyContent}.
	 * @param ctx the parse tree
	 */
	void enterBodyContent(HCLParser.BodyContentContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#bodyContent}.
	 * @param ctx the parse tree
	 */
	void exitBodyContent(HCLParser.BodyContentContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#attribute}.
	 * @param ctx the parse tree
	 */
	void enterAttribute(HCLParser.AttributeContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#attribute}.
	 * @param ctx the parse tree
	 */
	void exitAttribute(HCLParser.AttributeContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#block}.
	 * @param ctx the parse tree
	 */
	void enterBlock(HCLParser.BlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#block}.
	 * @param ctx the parse tree
	 */
	void exitBlock(HCLParser.BlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#blockLabel}.
	 * @param ctx the parse tree
	 */
	void enterBlockLabel(HCLParser.BlockLabelContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#blockLabel}.
	 * @param ctx the parse tree
	 */
	void exitBlockLabel(HCLParser.BlockLabelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code OperationExpression}
	 * labeled alternative in {@link HCLParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterOperationExpression(HCLParser.OperationExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code OperationExpression}
	 * labeled alternative in {@link HCLParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitOperationExpression(HCLParser.OperationExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ConditionalExpression}
	 * labeled alternative in {@link HCLParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterConditionalExpression(HCLParser.ConditionalExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ConditionalExpression}
	 * labeled alternative in {@link HCLParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitConditionalExpression(HCLParser.ConditionalExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ExpressionTerm}
	 * labeled alternative in {@link HCLParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpressionTerm(HCLParser.ExpressionTermContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ExpressionTerm}
	 * labeled alternative in {@link HCLParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpressionTerm(HCLParser.ExpressionTermContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ParentheticalExpression}
	 * labeled alternative in {@link HCLParser#exprTerm}.
	 * @param ctx the parse tree
	 */
	void enterParentheticalExpression(HCLParser.ParentheticalExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ParentheticalExpression}
	 * labeled alternative in {@link HCLParser#exprTerm}.
	 * @param ctx the parse tree
	 */
	void exitParentheticalExpression(HCLParser.ParentheticalExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code AttributeAccessExpression}
	 * labeled alternative in {@link HCLParser#exprTerm}.
	 * @param ctx the parse tree
	 */
	void enterAttributeAccessExpression(HCLParser.AttributeAccessExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code AttributeAccessExpression}
	 * labeled alternative in {@link HCLParser#exprTerm}.
	 * @param ctx the parse tree
	 */
	void exitAttributeAccessExpression(HCLParser.AttributeAccessExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LiteralExpression}
	 * labeled alternative in {@link HCLParser#exprTerm}.
	 * @param ctx the parse tree
	 */
	void enterLiteralExpression(HCLParser.LiteralExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LiteralExpression}
	 * labeled alternative in {@link HCLParser#exprTerm}.
	 * @param ctx the parse tree
	 */
	void exitLiteralExpression(HCLParser.LiteralExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code TemplateExpression}
	 * labeled alternative in {@link HCLParser#exprTerm}.
	 * @param ctx the parse tree
	 */
	void enterTemplateExpression(HCLParser.TemplateExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code TemplateExpression}
	 * labeled alternative in {@link HCLParser#exprTerm}.
	 * @param ctx the parse tree
	 */
	void exitTemplateExpression(HCLParser.TemplateExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code VariableExpression}
	 * labeled alternative in {@link HCLParser#exprTerm}.
	 * @param ctx the parse tree
	 */
	void enterVariableExpression(HCLParser.VariableExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code VariableExpression}
	 * labeled alternative in {@link HCLParser#exprTerm}.
	 * @param ctx the parse tree
	 */
	void exitVariableExpression(HCLParser.VariableExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code SplatExpression}
	 * labeled alternative in {@link HCLParser#exprTerm}.
	 * @param ctx the parse tree
	 */
	void enterSplatExpression(HCLParser.SplatExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code SplatExpression}
	 * labeled alternative in {@link HCLParser#exprTerm}.
	 * @param ctx the parse tree
	 */
	void exitSplatExpression(HCLParser.SplatExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code IndexAccessExpression}
	 * labeled alternative in {@link HCLParser#exprTerm}.
	 * @param ctx the parse tree
	 */
	void enterIndexAccessExpression(HCLParser.IndexAccessExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code IndexAccessExpression}
	 * labeled alternative in {@link HCLParser#exprTerm}.
	 * @param ctx the parse tree
	 */
	void exitIndexAccessExpression(HCLParser.IndexAccessExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ForExpression}
	 * labeled alternative in {@link HCLParser#exprTerm}.
	 * @param ctx the parse tree
	 */
	void enterForExpression(HCLParser.ForExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ForExpression}
	 * labeled alternative in {@link HCLParser#exprTerm}.
	 * @param ctx the parse tree
	 */
	void exitForExpression(HCLParser.ForExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code FunctionCallExpression}
	 * labeled alternative in {@link HCLParser#exprTerm}.
	 * @param ctx the parse tree
	 */
	void enterFunctionCallExpression(HCLParser.FunctionCallExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code FunctionCallExpression}
	 * labeled alternative in {@link HCLParser#exprTerm}.
	 * @param ctx the parse tree
	 */
	void exitFunctionCallExpression(HCLParser.FunctionCallExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CollectionValueExpression}
	 * labeled alternative in {@link HCLParser#exprTerm}.
	 * @param ctx the parse tree
	 */
	void enterCollectionValueExpression(HCLParser.CollectionValueExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CollectionValueExpression}
	 * labeled alternative in {@link HCLParser#exprTerm}.
	 * @param ctx the parse tree
	 */
	void exitCollectionValueExpression(HCLParser.CollectionValueExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#blockExpr}.
	 * @param ctx the parse tree
	 */
	void enterBlockExpr(HCLParser.BlockExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#blockExpr}.
	 * @param ctx the parse tree
	 */
	void exitBlockExpr(HCLParser.BlockExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#literalValue}.
	 * @param ctx the parse tree
	 */
	void enterLiteralValue(HCLParser.LiteralValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#literalValue}.
	 * @param ctx the parse tree
	 */
	void exitLiteralValue(HCLParser.LiteralValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#collectionValue}.
	 * @param ctx the parse tree
	 */
	void enterCollectionValue(HCLParser.CollectionValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#collectionValue}.
	 * @param ctx the parse tree
	 */
	void exitCollectionValue(HCLParser.CollectionValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#tuple}.
	 * @param ctx the parse tree
	 */
	void enterTuple(HCLParser.TupleContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#tuple}.
	 * @param ctx the parse tree
	 */
	void exitTuple(HCLParser.TupleContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#object}.
	 * @param ctx the parse tree
	 */
	void enterObject(HCLParser.ObjectContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#object}.
	 * @param ctx the parse tree
	 */
	void exitObject(HCLParser.ObjectContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#objectelem}.
	 * @param ctx the parse tree
	 */
	void enterObjectelem(HCLParser.ObjectelemContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#objectelem}.
	 * @param ctx the parse tree
	 */
	void exitObjectelem(HCLParser.ObjectelemContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#forExpr}.
	 * @param ctx the parse tree
	 */
	void enterForExpr(HCLParser.ForExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#forExpr}.
	 * @param ctx the parse tree
	 */
	void exitForExpr(HCLParser.ForExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#forTupleExpr}.
	 * @param ctx the parse tree
	 */
	void enterForTupleExpr(HCLParser.ForTupleExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#forTupleExpr}.
	 * @param ctx the parse tree
	 */
	void exitForTupleExpr(HCLParser.ForTupleExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#forObjectExpr}.
	 * @param ctx the parse tree
	 */
	void enterForObjectExpr(HCLParser.ForObjectExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#forObjectExpr}.
	 * @param ctx the parse tree
	 */
	void exitForObjectExpr(HCLParser.ForObjectExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#forIntro}.
	 * @param ctx the parse tree
	 */
	void enterForIntro(HCLParser.ForIntroContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#forIntro}.
	 * @param ctx the parse tree
	 */
	void exitForIntro(HCLParser.ForIntroContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#forCond}.
	 * @param ctx the parse tree
	 */
	void enterForCond(HCLParser.ForCondContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#forCond}.
	 * @param ctx the parse tree
	 */
	void exitForCond(HCLParser.ForCondContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#variableExpr}.
	 * @param ctx the parse tree
	 */
	void enterVariableExpr(HCLParser.VariableExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#variableExpr}.
	 * @param ctx the parse tree
	 */
	void exitVariableExpr(HCLParser.VariableExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void enterFunctionCall(HCLParser.FunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void exitFunctionCall(HCLParser.FunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#arguments}.
	 * @param ctx the parse tree
	 */
	void enterArguments(HCLParser.ArgumentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#arguments}.
	 * @param ctx the parse tree
	 */
	void exitArguments(HCLParser.ArgumentsContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#index}.
	 * @param ctx the parse tree
	 */
	void enterIndex(HCLParser.IndexContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#index}.
	 * @param ctx the parse tree
	 */
	void exitIndex(HCLParser.IndexContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#getAttr}.
	 * @param ctx the parse tree
	 */
	void enterGetAttr(HCLParser.GetAttrContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#getAttr}.
	 * @param ctx the parse tree
	 */
	void exitGetAttr(HCLParser.GetAttrContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#splat}.
	 * @param ctx the parse tree
	 */
	void enterSplat(HCLParser.SplatContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#splat}.
	 * @param ctx the parse tree
	 */
	void exitSplat(HCLParser.SplatContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#attrSplat}.
	 * @param ctx the parse tree
	 */
	void enterAttrSplat(HCLParser.AttrSplatContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#attrSplat}.
	 * @param ctx the parse tree
	 */
	void exitAttrSplat(HCLParser.AttrSplatContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#fullSplat}.
	 * @param ctx the parse tree
	 */
	void enterFullSplat(HCLParser.FullSplatContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#fullSplat}.
	 * @param ctx the parse tree
	 */
	void exitFullSplat(HCLParser.FullSplatContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#operation}.
	 * @param ctx the parse tree
	 */
	void enterOperation(HCLParser.OperationContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#operation}.
	 * @param ctx the parse tree
	 */
	void exitOperation(HCLParser.OperationContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#unaryOp}.
	 * @param ctx the parse tree
	 */
	void enterUnaryOp(HCLParser.UnaryOpContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#unaryOp}.
	 * @param ctx the parse tree
	 */
	void exitUnaryOp(HCLParser.UnaryOpContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#binaryOp}.
	 * @param ctx the parse tree
	 */
	void enterBinaryOp(HCLParser.BinaryOpContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#binaryOp}.
	 * @param ctx the parse tree
	 */
	void exitBinaryOp(HCLParser.BinaryOpContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#binaryOperator}.
	 * @param ctx the parse tree
	 */
	void enterBinaryOperator(HCLParser.BinaryOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#binaryOperator}.
	 * @param ctx the parse tree
	 */
	void exitBinaryOperator(HCLParser.BinaryOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#compareOperator}.
	 * @param ctx the parse tree
	 */
	void enterCompareOperator(HCLParser.CompareOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#compareOperator}.
	 * @param ctx the parse tree
	 */
	void exitCompareOperator(HCLParser.CompareOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#arithmeticOperator}.
	 * @param ctx the parse tree
	 */
	void enterArithmeticOperator(HCLParser.ArithmeticOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#arithmeticOperator}.
	 * @param ctx the parse tree
	 */
	void exitArithmeticOperator(HCLParser.ArithmeticOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#logicOperator}.
	 * @param ctx the parse tree
	 */
	void enterLogicOperator(HCLParser.LogicOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#logicOperator}.
	 * @param ctx the parse tree
	 */
	void exitLogicOperator(HCLParser.LogicOperatorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Heredoc}
	 * labeled alternative in {@link HCLParser#templateExpr}.
	 * @param ctx the parse tree
	 */
	void enterHeredoc(HCLParser.HeredocContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Heredoc}
	 * labeled alternative in {@link HCLParser#templateExpr}.
	 * @param ctx the parse tree
	 */
	void exitHeredoc(HCLParser.HeredocContext ctx);
	/**
	 * Enter a parse tree produced by the {@code QuotedTemplate}
	 * labeled alternative in {@link HCLParser#templateExpr}.
	 * @param ctx the parse tree
	 */
	void enterQuotedTemplate(HCLParser.QuotedTemplateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code QuotedTemplate}
	 * labeled alternative in {@link HCLParser#templateExpr}.
	 * @param ctx the parse tree
	 */
	void exitQuotedTemplate(HCLParser.QuotedTemplateContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#heredocTemplatePart}.
	 * @param ctx the parse tree
	 */
	void enterHeredocTemplatePart(HCLParser.HeredocTemplatePartContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#heredocTemplatePart}.
	 * @param ctx the parse tree
	 */
	void exitHeredocTemplatePart(HCLParser.HeredocTemplatePartContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#heredocLiteral}.
	 * @param ctx the parse tree
	 */
	void enterHeredocLiteral(HCLParser.HeredocLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#heredocLiteral}.
	 * @param ctx the parse tree
	 */
	void exitHeredocLiteral(HCLParser.HeredocLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#quotedTemplatePart}.
	 * @param ctx the parse tree
	 */
	void enterQuotedTemplatePart(HCLParser.QuotedTemplatePartContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#quotedTemplatePart}.
	 * @param ctx the parse tree
	 */
	void exitQuotedTemplatePart(HCLParser.QuotedTemplatePartContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#stringLiteral}.
	 * @param ctx the parse tree
	 */
	void enterStringLiteral(HCLParser.StringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#stringLiteral}.
	 * @param ctx the parse tree
	 */
	void exitStringLiteral(HCLParser.StringLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link HCLParser#templateInterpolation}.
	 * @param ctx the parse tree
	 */
	void enterTemplateInterpolation(HCLParser.TemplateInterpolationContext ctx);
	/**
	 * Exit a parse tree produced by {@link HCLParser#templateInterpolation}.
	 * @param ctx the parse tree
	 */
	void exitTemplateInterpolation(HCLParser.TemplateInterpolationContext ctx);
}