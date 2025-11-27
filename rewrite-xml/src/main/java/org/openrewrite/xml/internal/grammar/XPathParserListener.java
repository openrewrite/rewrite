/*
 * Copyright 2025 the original author or authors.
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
// Generated from /Users/knut/git/openrewrite/rewrite/rewrite-xml/src/main/antlr/XPathParser.g4 by ANTLR 4.13.2
package org.openrewrite.xml.internal.grammar;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link XPathParser}.
 */
public interface XPathParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link XPathParser#xpathExpression}.
	 * @param ctx the parse tree
	 */
	void enterXpathExpression(XPathParser.XpathExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#xpathExpression}.
	 * @param ctx the parse tree
	 */
	void exitXpathExpression(XPathParser.XpathExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#booleanExpr}.
	 * @param ctx the parse tree
	 */
	void enterBooleanExpr(XPathParser.BooleanExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#booleanExpr}.
	 * @param ctx the parse tree
	 */
	void exitBooleanExpr(XPathParser.BooleanExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#comparisonOp}.
	 * @param ctx the parse tree
	 */
	void enterComparisonOp(XPathParser.ComparisonOpContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#comparisonOp}.
	 * @param ctx the parse tree
	 */
	void exitComparisonOp(XPathParser.ComparisonOpContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#comparand}.
	 * @param ctx the parse tree
	 */
	void enterComparand(XPathParser.ComparandContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#comparand}.
	 * @param ctx the parse tree
	 */
	void exitComparand(XPathParser.ComparandContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#absoluteLocationPath}.
	 * @param ctx the parse tree
	 */
	void enterAbsoluteLocationPath(XPathParser.AbsoluteLocationPathContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#absoluteLocationPath}.
	 * @param ctx the parse tree
	 */
	void exitAbsoluteLocationPath(XPathParser.AbsoluteLocationPathContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#relativeLocationPath}.
	 * @param ctx the parse tree
	 */
	void enterRelativeLocationPath(XPathParser.RelativeLocationPathContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#relativeLocationPath}.
	 * @param ctx the parse tree
	 */
	void exitRelativeLocationPath(XPathParser.RelativeLocationPathContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#pathSeparator}.
	 * @param ctx the parse tree
	 */
	void enterPathSeparator(XPathParser.PathSeparatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#pathSeparator}.
	 * @param ctx the parse tree
	 */
	void exitPathSeparator(XPathParser.PathSeparatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#step}.
	 * @param ctx the parse tree
	 */
	void enterStep(XPathParser.StepContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#step}.
	 * @param ctx the parse tree
	 */
	void exitStep(XPathParser.StepContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#nodeTypeTest}.
	 * @param ctx the parse tree
	 */
	void enterNodeTypeTest(XPathParser.NodeTypeTestContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#nodeTypeTest}.
	 * @param ctx the parse tree
	 */
	void exitNodeTypeTest(XPathParser.NodeTypeTestContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#attributeStep}.
	 * @param ctx the parse tree
	 */
	void enterAttributeStep(XPathParser.AttributeStepContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#attributeStep}.
	 * @param ctx the parse tree
	 */
	void exitAttributeStep(XPathParser.AttributeStepContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#nodeTest}.
	 * @param ctx the parse tree
	 */
	void enterNodeTest(XPathParser.NodeTestContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#nodeTest}.
	 * @param ctx the parse tree
	 */
	void exitNodeTest(XPathParser.NodeTestContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterPredicate(XPathParser.PredicateContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitPredicate(XPathParser.PredicateContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#predicateExpr}.
	 * @param ctx the parse tree
	 */
	void enterPredicateExpr(XPathParser.PredicateExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#predicateExpr}.
	 * @param ctx the parse tree
	 */
	void exitPredicateExpr(XPathParser.PredicateExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#orExpr}.
	 * @param ctx the parse tree
	 */
	void enterOrExpr(XPathParser.OrExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#orExpr}.
	 * @param ctx the parse tree
	 */
	void exitOrExpr(XPathParser.OrExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#andExpr}.
	 * @param ctx the parse tree
	 */
	void enterAndExpr(XPathParser.AndExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#andExpr}.
	 * @param ctx the parse tree
	 */
	void exitAndExpr(XPathParser.AndExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#primaryExpr}.
	 * @param ctx the parse tree
	 */
	void enterPrimaryExpr(XPathParser.PrimaryExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#primaryExpr}.
	 * @param ctx the parse tree
	 */
	void exitPrimaryExpr(XPathParser.PrimaryExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void enterFunctionCall(XPathParser.FunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void exitFunctionCall(XPathParser.FunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#functionArgs}.
	 * @param ctx the parse tree
	 */
	void enterFunctionArgs(XPathParser.FunctionArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#functionArgs}.
	 * @param ctx the parse tree
	 */
	void exitFunctionArgs(XPathParser.FunctionArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#functionArg}.
	 * @param ctx the parse tree
	 */
	void enterFunctionArg(XPathParser.FunctionArgContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#functionArg}.
	 * @param ctx the parse tree
	 */
	void exitFunctionArg(XPathParser.FunctionArgContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#childElementTest}.
	 * @param ctx the parse tree
	 */
	void enterChildElementTest(XPathParser.ChildElementTestContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#childElementTest}.
	 * @param ctx the parse tree
	 */
	void exitChildElementTest(XPathParser.ChildElementTestContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#stringLiteral}.
	 * @param ctx the parse tree
	 */
	void enterStringLiteral(XPathParser.StringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#stringLiteral}.
	 * @param ctx the parse tree
	 */
	void exitStringLiteral(XPathParser.StringLiteralContext ctx);
}