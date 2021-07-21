/*
 *  Copyright 2021 the original author or authors.
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  https://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

// Generated from /Users/jbrisbin/src/github.com/openrewrite/rewrite/rewrite-yaml/src/main/antlr/JsonPath.g4 by ANTLR 4.9.2
package org.openrewrite.yaml.internal.grammar;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link JsonPath}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface JsonPathVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link JsonPath#jsonpath}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJsonpath(JsonPath.JsonpathContext ctx);
	/**
	 * Visit a parse tree produced by the {@code BracketOperator}
	 * labeled alternative in {@link JsonPath#object}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBracketOperator(JsonPath.BracketOperatorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DotOperator}
	 * labeled alternative in {@link JsonPath#object}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDotOperator(JsonPath.DotOperatorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code RecursiveDescent}
	 * labeled alternative in {@link JsonPath#object}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRecursiveDescent(JsonPath.RecursiveDescentContext ctx);
	/**
	 * Visit a parse tree produced by the {@code UnionOperator}
	 * labeled alternative in {@link JsonPath#object}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnionOperator(JsonPath.UnionOperatorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code RangeOperator}
	 * labeled alternative in {@link JsonPath#object}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRangeOperator(JsonPath.RangeOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link JsonPath#rangeOp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRangeOp(JsonPath.RangeOpContext ctx);
	/**
	 * Visit a parse tree produced by {@link JsonPath#start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStart(JsonPath.StartContext ctx);
	/**
	 * Visit a parse tree produced by {@link JsonPath#end}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnd(JsonPath.EndContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ParentheticalExpression}
	 * labeled alternative in {@link JsonPath#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParentheticalExpression(JsonPath.ParentheticalExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Identifier}
	 * labeled alternative in {@link JsonPath#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifier(JsonPath.IdentifierContext ctx);
	/**
	 * Visit a parse tree produced by the {@code AndExpression}
	 * labeled alternative in {@link JsonPath#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAndExpression(JsonPath.AndExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code PathExpression}
	 * labeled alternative in {@link JsonPath#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPathExpression(JsonPath.PathExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ScopedPathExpression}
	 * labeled alternative in {@link JsonPath#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScopedPathExpression(JsonPath.ScopedPathExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code BinaryExpression}
	 * labeled alternative in {@link JsonPath#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBinaryExpression(JsonPath.BinaryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LiteralExpression}
	 * labeled alternative in {@link JsonPath#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteralExpression(JsonPath.LiteralExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NotExpression}
	 * labeled alternative in {@link JsonPath#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotExpression(JsonPath.NotExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FilterExpression}
	 * labeled alternative in {@link JsonPath#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilterExpression(JsonPath.FilterExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code OrExpression}
	 * labeled alternative in {@link JsonPath#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrExpression(JsonPath.OrExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link JsonPath#litExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLitExpression(JsonPath.LitExpressionContext ctx);
}