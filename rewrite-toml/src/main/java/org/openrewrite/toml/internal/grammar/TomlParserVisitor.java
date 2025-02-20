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
// Generated from ~/git/rewrite/rewrite-toml/src/main/antlr/TomlParser.g4 by ANTLR 4.13.2
package org.openrewrite.toml.internal.grammar;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link TomlParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface TomlParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link TomlParser#document}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDocument(TomlParser.DocumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(TomlParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#comment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComment(TomlParser.CommentContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#keyValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKeyValue(TomlParser.KeyValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#key}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKey(TomlParser.KeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#simpleKey}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimpleKey(TomlParser.SimpleKeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#unquotedKey}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnquotedKey(TomlParser.UnquotedKeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#quotedKey}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuotedKey(TomlParser.QuotedKeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#dottedKey}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDottedKey(TomlParser.DottedKeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValue(TomlParser.ValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#string}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitString(TomlParser.StringContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#integer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInteger(TomlParser.IntegerContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#floatingPoint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFloatingPoint(TomlParser.FloatingPointContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#bool}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBool(TomlParser.BoolContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#dateTime}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDateTime(TomlParser.DateTimeContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#commentOrNl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommentOrNl(TomlParser.CommentOrNlContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#array}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray(TomlParser.ArrayContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable(TomlParser.TableContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#standardTable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStandardTable(TomlParser.StandardTableContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#inlineTable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInlineTable(TomlParser.InlineTableContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#arrayTable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayTable(TomlParser.ArrayTableContext ctx);
}
