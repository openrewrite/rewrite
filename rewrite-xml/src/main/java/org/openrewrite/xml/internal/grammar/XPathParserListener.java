/*
 * Copyright 2026 the original author or authors.
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
// Generated from rewrite-xml/src/main/antlr/XPathParser.g4 by ANTLR 4.13.2
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
	 * Enter a parse tree produced by {@link XPathParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterExpr(XPathParser.ExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitExpr(XPathParser.ExprContext ctx);
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
	 * Enter a parse tree produced by {@link XPathParser#equalityExpr}.
	 * @param ctx the parse tree
	 */
	void enterEqualityExpr(XPathParser.EqualityExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#equalityExpr}.
	 * @param ctx the parse tree
	 */
	void exitEqualityExpr(XPathParser.EqualityExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#relationalExpr}.
	 * @param ctx the parse tree
	 */
	void enterRelationalExpr(XPathParser.RelationalExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#relationalExpr}.
	 * @param ctx the parse tree
	 */
	void exitRelationalExpr(XPathParser.RelationalExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#unaryExpr}.
	 * @param ctx the parse tree
	 */
	void enterUnaryExpr(XPathParser.UnaryExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#unaryExpr}.
	 * @param ctx the parse tree
	 */
	void exitUnaryExpr(XPathParser.UnaryExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#unionExpr}.
	 * @param ctx the parse tree
	 */
	void enterUnionExpr(XPathParser.UnionExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#unionExpr}.
	 * @param ctx the parse tree
	 */
	void exitUnionExpr(XPathParser.UnionExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#pathExpr}.
	 * @param ctx the parse tree
	 */
	void enterPathExpr(XPathParser.PathExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#pathExpr}.
	 * @param ctx the parse tree
	 */
	void exitPathExpr(XPathParser.PathExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#functionCallExpr}.
	 * @param ctx the parse tree
	 */
	void enterFunctionCallExpr(XPathParser.FunctionCallExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#functionCallExpr}.
	 * @param ctx the parse tree
	 */
	void exitFunctionCallExpr(XPathParser.FunctionCallExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#bracketedExpr}.
	 * @param ctx the parse tree
	 */
	void enterBracketedExpr(XPathParser.BracketedExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#bracketedExpr}.
	 * @param ctx the parse tree
	 */
	void exitBracketedExpr(XPathParser.BracketedExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#literalOrNumber}.
	 * @param ctx the parse tree
	 */
	void enterLiteralOrNumber(XPathParser.LiteralOrNumberContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#literalOrNumber}.
	 * @param ctx the parse tree
	 */
	void exitLiteralOrNumber(XPathParser.LiteralOrNumberContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#filterExpr}.
	 * @param ctx the parse tree
	 */
	void enterFilterExpr(XPathParser.FilterExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#filterExpr}.
	 * @param ctx the parse tree
	 */
	void exitFilterExpr(XPathParser.FilterExprContext ctx);
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
	 * Enter a parse tree produced by {@link XPathParser#locationPath}.
	 * @param ctx the parse tree
	 */
	void enterLocationPath(XPathParser.LocationPathContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#locationPath}.
	 * @param ctx the parse tree
	 */
	void exitLocationPath(XPathParser.LocationPathContext ctx);
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
	 * Enter a parse tree produced by {@link XPathParser#axisSpecifier}.
	 * @param ctx the parse tree
	 */
	void enterAxisSpecifier(XPathParser.AxisSpecifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#axisSpecifier}.
	 * @param ctx the parse tree
	 */
	void exitAxisSpecifier(XPathParser.AxisSpecifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#axisName}.
	 * @param ctx the parse tree
	 */
	void enterAxisName(XPathParser.AxisNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#axisName}.
	 * @param ctx the parse tree
	 */
	void exitAxisName(XPathParser.AxisNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#abbreviatedStep}.
	 * @param ctx the parse tree
	 */
	void enterAbbreviatedStep(XPathParser.AbbreviatedStepContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#abbreviatedStep}.
	 * @param ctx the parse tree
	 */
	void exitAbbreviatedStep(XPathParser.AbbreviatedStepContext ctx);
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
	 * Enter a parse tree produced by {@link XPathParser#nameTest}.
	 * @param ctx the parse tree
	 */
	void enterNameTest(XPathParser.NameTestContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#nameTest}.
	 * @param ctx the parse tree
	 */
	void exitNameTest(XPathParser.NameTestContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#nodeType}.
	 * @param ctx the parse tree
	 */
	void enterNodeType(XPathParser.NodeTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#nodeType}.
	 * @param ctx the parse tree
	 */
	void exitNodeType(XPathParser.NodeTypeContext ctx);
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
	 * Enter a parse tree produced by {@link XPathParser#functionName}.
	 * @param ctx the parse tree
	 */
	void enterFunctionName(XPathParser.FunctionNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#functionName}.
	 * @param ctx the parse tree
	 */
	void exitFunctionName(XPathParser.FunctionNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#argument}.
	 * @param ctx the parse tree
	 */
	void enterArgument(XPathParser.ArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#argument}.
	 * @param ctx the parse tree
	 */
	void exitArgument(XPathParser.ArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link XPathParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLiteral(XPathParser.LiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link XPathParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLiteral(XPathParser.LiteralContext ctx);
}
