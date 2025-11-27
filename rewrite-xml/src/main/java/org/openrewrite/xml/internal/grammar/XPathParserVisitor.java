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
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link XPathParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface XPathParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link XPathParser#xpathExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXpathExpression(XPathParser.XpathExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#booleanExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanExpr(XPathParser.BooleanExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#comparisonOp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComparisonOp(XPathParser.ComparisonOpContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#comparand}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComparand(XPathParser.ComparandContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#absoluteLocationPath}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAbsoluteLocationPath(XPathParser.AbsoluteLocationPathContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#relativeLocationPath}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelativeLocationPath(XPathParser.RelativeLocationPathContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#pathSeparator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPathSeparator(XPathParser.PathSeparatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#step}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStep(XPathParser.StepContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#nodeTypeTest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNodeTypeTest(XPathParser.NodeTypeTestContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#attributeStep}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttributeStep(XPathParser.AttributeStepContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#nodeTest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNodeTest(XPathParser.NodeTestContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#predicate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPredicate(XPathParser.PredicateContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#predicateExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPredicateExpr(XPathParser.PredicateExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#orExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrExpr(XPathParser.OrExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#andExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAndExpr(XPathParser.AndExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#primaryExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimaryExpr(XPathParser.PrimaryExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#functionCall}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionCall(XPathParser.FunctionCallContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#functionArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionArgs(XPathParser.FunctionArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#functionArg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionArg(XPathParser.FunctionArgContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#childElementTest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitChildElementTest(XPathParser.ChildElementTestContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#stringLiteral}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringLiteral(XPathParser.StringLiteralContext ctx);
}