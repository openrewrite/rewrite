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
	 * Visit a parse tree produced by {@link XPathParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr(XPathParser.ExprContext ctx);
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
	 * Visit a parse tree produced by {@link XPathParser#equalityExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqualityExpr(XPathParser.EqualityExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#relationalExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelationalExpr(XPathParser.RelationalExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#unaryExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryExpr(XPathParser.UnaryExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#unionExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnionExpr(XPathParser.UnionExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#pathExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPathExpr(XPathParser.PathExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#functionCallExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionCallExpr(XPathParser.FunctionCallExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#bracketedExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBracketedExpr(XPathParser.BracketedExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#literalOrNumber}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteralOrNumber(XPathParser.LiteralOrNumberContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#filterExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilterExpr(XPathParser.FilterExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#primaryExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimaryExpr(XPathParser.PrimaryExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#locationPath}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocationPath(XPathParser.LocationPathContext ctx);
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
	 * Visit a parse tree produced by {@link XPathParser#axisSpecifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAxisSpecifier(XPathParser.AxisSpecifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#axisName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAxisName(XPathParser.AxisNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#abbreviatedStep}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAbbreviatedStep(XPathParser.AbbreviatedStepContext ctx);
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
	 * Visit a parse tree produced by {@link XPathParser#nameTest}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNameTest(XPathParser.NameTestContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#nodeType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNodeType(XPathParser.NodeTypeContext ctx);
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
	 * Visit a parse tree produced by {@link XPathParser#functionCall}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionCall(XPathParser.FunctionCallContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#functionName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionName(XPathParser.FunctionNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#argument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgument(XPathParser.ArgumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link XPathParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral(XPathParser.LiteralContext ctx);
}
