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
 * {@link JsonPathParser}.
 */
public interface JsonPathParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link JsonPathParser#jsonPath}.
	 * @param ctx the parse tree
	 */
	void enterJsonPath(JsonPathParser.JsonPathContext ctx);
	/**
	 * Exit a parse tree produced by {@link JsonPathParser#jsonPath}.
	 * @param ctx the parse tree
	 */
	void exitJsonPath(JsonPathParser.JsonPathContext ctx);
	/**
	 * Enter a parse tree produced by {@link JsonPathParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(JsonPathParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link JsonPathParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(JsonPathParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link JsonPathParser#dotOperator}.
	 * @param ctx the parse tree
	 */
	void enterDotOperator(JsonPathParser.DotOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link JsonPathParser#dotOperator}.
	 * @param ctx the parse tree
	 */
	void exitDotOperator(JsonPathParser.DotOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link JsonPathParser#recursiveDecent}.
	 * @param ctx the parse tree
	 */
	void enterRecursiveDecent(JsonPathParser.RecursiveDecentContext ctx);
	/**
	 * Exit a parse tree produced by {@link JsonPathParser#recursiveDecent}.
	 * @param ctx the parse tree
	 */
	void exitRecursiveDecent(JsonPathParser.RecursiveDecentContext ctx);
	/**
	 * Enter a parse tree produced by {@link JsonPathParser#bracketOperator}.
	 * @param ctx the parse tree
	 */
	void enterBracketOperator(JsonPathParser.BracketOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link JsonPathParser#bracketOperator}.
	 * @param ctx the parse tree
	 */
	void exitBracketOperator(JsonPathParser.BracketOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link JsonPathParser#filter}.
	 * @param ctx the parse tree
	 */
	void enterFilter(JsonPathParser.FilterContext ctx);
	/**
	 * Exit a parse tree produced by {@link JsonPathParser#filter}.
	 * @param ctx the parse tree
	 */
	void exitFilter(JsonPathParser.FilterContext ctx);
	/**
	 * Enter a parse tree produced by {@link JsonPathParser#filterExpression}.
	 * @param ctx the parse tree
	 */
	void enterFilterExpression(JsonPathParser.FilterExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link JsonPathParser#filterExpression}.
	 * @param ctx the parse tree
	 */
	void exitFilterExpression(JsonPathParser.FilterExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link JsonPathParser#binaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterBinaryExpression(JsonPathParser.BinaryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link JsonPathParser#binaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitBinaryExpression(JsonPathParser.BinaryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link JsonPathParser#containsExpression}.
	 * @param ctx the parse tree
	 */
	void enterContainsExpression(JsonPathParser.ContainsExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link JsonPathParser#containsExpression}.
	 * @param ctx the parse tree
	 */
	void exitContainsExpression(JsonPathParser.ContainsExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link JsonPathParser#regexExpression}.
	 * @param ctx the parse tree
	 */
	void enterRegexExpression(JsonPathParser.RegexExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link JsonPathParser#regexExpression}.
	 * @param ctx the parse tree
	 */
	void exitRegexExpression(JsonPathParser.RegexExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link JsonPathParser#unaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterUnaryExpression(JsonPathParser.UnaryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link JsonPathParser#unaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitUnaryExpression(JsonPathParser.UnaryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link JsonPathParser#literalExpression}.
	 * @param ctx the parse tree
	 */
	void enterLiteralExpression(JsonPathParser.LiteralExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link JsonPathParser#literalExpression}.
	 * @param ctx the parse tree
	 */
	void exitLiteralExpression(JsonPathParser.LiteralExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link JsonPathParser#property}.
	 * @param ctx the parse tree
	 */
	void enterProperty(JsonPathParser.PropertyContext ctx);
	/**
	 * Exit a parse tree produced by {@link JsonPathParser#property}.
	 * @param ctx the parse tree
	 */
	void exitProperty(JsonPathParser.PropertyContext ctx);
	/**
	 * Enter a parse tree produced by {@link JsonPathParser#wildcard}.
	 * @param ctx the parse tree
	 */
	void enterWildcard(JsonPathParser.WildcardContext ctx);
	/**
	 * Exit a parse tree produced by {@link JsonPathParser#wildcard}.
	 * @param ctx the parse tree
	 */
	void exitWildcard(JsonPathParser.WildcardContext ctx);
	/**
	 * Enter a parse tree produced by {@link JsonPathParser#slice}.
	 * @param ctx the parse tree
	 */
	void enterSlice(JsonPathParser.SliceContext ctx);
	/**
	 * Exit a parse tree produced by {@link JsonPathParser#slice}.
	 * @param ctx the parse tree
	 */
	void exitSlice(JsonPathParser.SliceContext ctx);
	/**
	 * Enter a parse tree produced by {@link JsonPathParser#start}.
	 * @param ctx the parse tree
	 */
	void enterStart(JsonPathParser.StartContext ctx);
	/**
	 * Exit a parse tree produced by {@link JsonPathParser#start}.
	 * @param ctx the parse tree
	 */
	void exitStart(JsonPathParser.StartContext ctx);
	/**
	 * Enter a parse tree produced by {@link JsonPathParser#end}.
	 * @param ctx the parse tree
	 */
	void enterEnd(JsonPathParser.EndContext ctx);
	/**
	 * Exit a parse tree produced by {@link JsonPathParser#end}.
	 * @param ctx the parse tree
	 */
	void exitEnd(JsonPathParser.EndContext ctx);
	/**
	 * Enter a parse tree produced by {@link JsonPathParser#indexes}.
	 * @param ctx the parse tree
	 */
	void enterIndexes(JsonPathParser.IndexesContext ctx);
	/**
	 * Exit a parse tree produced by {@link JsonPathParser#indexes}.
	 * @param ctx the parse tree
	 */
	void exitIndexes(JsonPathParser.IndexesContext ctx);
}