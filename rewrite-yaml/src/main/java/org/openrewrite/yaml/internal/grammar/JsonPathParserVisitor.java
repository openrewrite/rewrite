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
package org.openrewrite.yaml.internal.grammar;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link JsonPathParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface JsonPathParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link JsonPathParser#jsonPath}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJsonPath(JsonPathParser.JsonPathContext ctx);
	/**
	 * Visit a parse tree produced by {@link JsonPathParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(JsonPathParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link JsonPathParser#dotOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDotOperator(JsonPathParser.DotOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link JsonPathParser#recursiveDecent}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRecursiveDecent(JsonPathParser.RecursiveDecentContext ctx);
	/**
	 * Visit a parse tree produced by {@link JsonPathParser#bracketOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBracketOperator(JsonPathParser.BracketOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link JsonPathParser#filter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilter(JsonPathParser.FilterContext ctx);
	/**
	 * Visit a parse tree produced by {@link JsonPathParser#filterExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilterExpression(JsonPathParser.FilterExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link JsonPathParser#binaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBinaryExpression(JsonPathParser.BinaryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link JsonPathParser#containsExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContainsExpression(JsonPathParser.ContainsExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link JsonPathParser#regexExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRegexExpression(JsonPathParser.RegexExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link JsonPathParser#unaryExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryExpression(JsonPathParser.UnaryExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link JsonPathParser#literalExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteralExpression(JsonPathParser.LiteralExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link JsonPathParser#property}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProperty(JsonPathParser.PropertyContext ctx);
	/**
	 * Visit a parse tree produced by {@link JsonPathParser#wildcard}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWildcard(JsonPathParser.WildcardContext ctx);
	/**
	 * Visit a parse tree produced by {@link JsonPathParser#slice}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSlice(JsonPathParser.SliceContext ctx);
	/**
	 * Visit a parse tree produced by {@link JsonPathParser#start}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStart(JsonPathParser.StartContext ctx);
	/**
	 * Visit a parse tree produced by {@link JsonPathParser#end}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnd(JsonPathParser.EndContext ctx);
	/**
	 * Visit a parse tree produced by {@link JsonPathParser#indexes}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndexes(JsonPathParser.IndexesContext ctx);
}