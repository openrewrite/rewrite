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
// Generated from java-escape by ANTLR 4.11.1
package org.openrewrite.toml.internal.grammar;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link TomlParser}.
 */
public interface TomlParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link TomlParser#document}.
	 * @param ctx the parse tree
	 */
	void enterDocument(TomlParser.DocumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#document}.
	 * @param ctx the parse tree
	 */
	void exitDocument(TomlParser.DocumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link TomlParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(TomlParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(TomlParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link TomlParser#comment}.
	 * @param ctx the parse tree
	 */
	void enterComment(TomlParser.CommentContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#comment}.
	 * @param ctx the parse tree
	 */
	void exitComment(TomlParser.CommentContext ctx);
	/**
	 * Enter a parse tree produced by {@link TomlParser#keyValue}.
	 * @param ctx the parse tree
	 */
	void enterKeyValue(TomlParser.KeyValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#keyValue}.
	 * @param ctx the parse tree
	 */
	void exitKeyValue(TomlParser.KeyValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link TomlParser#key}.
	 * @param ctx the parse tree
	 */
	void enterKey(TomlParser.KeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#key}.
	 * @param ctx the parse tree
	 */
	void exitKey(TomlParser.KeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link TomlParser#simpleKey}.
	 * @param ctx the parse tree
	 */
	void enterSimpleKey(TomlParser.SimpleKeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#simpleKey}.
	 * @param ctx the parse tree
	 */
	void exitSimpleKey(TomlParser.SimpleKeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link TomlParser#unquotedKey}.
	 * @param ctx the parse tree
	 */
	void enterUnquotedKey(TomlParser.UnquotedKeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#unquotedKey}.
	 * @param ctx the parse tree
	 */
	void exitUnquotedKey(TomlParser.UnquotedKeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link TomlParser#quotedKey}.
	 * @param ctx the parse tree
	 */
	void enterQuotedKey(TomlParser.QuotedKeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#quotedKey}.
	 * @param ctx the parse tree
	 */
	void exitQuotedKey(TomlParser.QuotedKeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link TomlParser#dottedKey}.
	 * @param ctx the parse tree
	 */
	void enterDottedKey(TomlParser.DottedKeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#dottedKey}.
	 * @param ctx the parse tree
	 */
	void exitDottedKey(TomlParser.DottedKeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link TomlParser#value}.
	 * @param ctx the parse tree
	 */
	void enterValue(TomlParser.ValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#value}.
	 * @param ctx the parse tree
	 */
	void exitValue(TomlParser.ValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link TomlParser#string}.
	 * @param ctx the parse tree
	 */
	void enterString(TomlParser.StringContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#string}.
	 * @param ctx the parse tree
	 */
	void exitString(TomlParser.StringContext ctx);
	/**
	 * Enter a parse tree produced by {@link TomlParser#integer}.
	 * @param ctx the parse tree
	 */
	void enterInteger(TomlParser.IntegerContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#integer}.
	 * @param ctx the parse tree
	 */
	void exitInteger(TomlParser.IntegerContext ctx);
	/**
	 * Enter a parse tree produced by {@link TomlParser#floatingPoint}.
	 * @param ctx the parse tree
	 */
	void enterFloatingPoint(TomlParser.FloatingPointContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#floatingPoint}.
	 * @param ctx the parse tree
	 */
	void exitFloatingPoint(TomlParser.FloatingPointContext ctx);
	/**
	 * Enter a parse tree produced by {@link TomlParser#bool}.
	 * @param ctx the parse tree
	 */
	void enterBool(TomlParser.BoolContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#bool}.
	 * @param ctx the parse tree
	 */
	void exitBool(TomlParser.BoolContext ctx);
	/**
	 * Enter a parse tree produced by {@link TomlParser#dateTime}.
	 * @param ctx the parse tree
	 */
	void enterDateTime(TomlParser.DateTimeContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#dateTime}.
	 * @param ctx the parse tree
	 */
	void exitDateTime(TomlParser.DateTimeContext ctx);
	/**
	 * Enter a parse tree produced by {@link TomlParser#commentOrNl}.
	 * @param ctx the parse tree
	 */
	void enterCommentOrNl(TomlParser.CommentOrNlContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#commentOrNl}.
	 * @param ctx the parse tree
	 */
	void exitCommentOrNl(TomlParser.CommentOrNlContext ctx);
	/**
	 * Enter a parse tree produced by {@link TomlParser#array}.
	 * @param ctx the parse tree
	 */
	void enterArray(TomlParser.ArrayContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#array}.
	 * @param ctx the parse tree
	 */
	void exitArray(TomlParser.ArrayContext ctx);
	/**
	 * Enter a parse tree produced by {@link TomlParser#table}.
	 * @param ctx the parse tree
	 */
	void enterTable(TomlParser.TableContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#table}.
	 * @param ctx the parse tree
	 */
	void exitTable(TomlParser.TableContext ctx);
	/**
	 * Enter a parse tree produced by {@link TomlParser#standardTable}.
	 * @param ctx the parse tree
	 */
	void enterStandardTable(TomlParser.StandardTableContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#standardTable}.
	 * @param ctx the parse tree
	 */
	void exitStandardTable(TomlParser.StandardTableContext ctx);
	/**
	 * Enter a parse tree produced by {@link TomlParser#inlineTable}.
	 * @param ctx the parse tree
	 */
	void enterInlineTable(TomlParser.InlineTableContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#inlineTable}.
	 * @param ctx the parse tree
	 */
	void exitInlineTable(TomlParser.InlineTableContext ctx);
	/**
	 * Enter a parse tree produced by {@link TomlParser#arrayTable}.
	 * @param ctx the parse tree
	 */
	void enterArrayTable(TomlParser.ArrayTableContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#arrayTable}.
	 * @param ctx the parse tree
	 */
	void exitArrayTable(TomlParser.ArrayTableContext ctx);
}