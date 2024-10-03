// Generated from java-escape by ANTLR 4.11.1
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
	 * Visit a parse tree produced by {@link TomlParser#key_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKeyValue(TomlParser.Key_valueContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#key}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKey(TomlParser.KeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#simple_key}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimple_key(TomlParser.Simple_keyContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#unquoted_key}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnquoted_key(TomlParser.Unquoted_keyContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#quoted_key}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuoted_key(TomlParser.Quoted_keyContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#dotted_key}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDotted_key(TomlParser.Dotted_keyContext ctx);
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
	 * Visit a parse tree produced by {@link TomlParser#floating_point}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFloating_point(TomlParser.Floating_pointContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#bool_}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBool_(TomlParser.Bool_Context ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#date_time}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDate_time(TomlParser.Date_timeContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#array_}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_(TomlParser.Array_Context ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#array_values}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_values(TomlParser.Array_valuesContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#comment_or_nl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComment_or_nl(TomlParser.Comment_or_nlContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#nl_or_comment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNl_or_comment(TomlParser.Nl_or_commentContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable(TomlParser.TableContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#standard_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStandard_table(TomlParser.Standard_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#inline_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInline_table(TomlParser.Inline_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#inline_table_keyvals}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInline_table_keyvals(TomlParser.Inline_table_keyvalsContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#inline_table_keyvals_non_empty}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInline_table_keyvals_non_empty(TomlParser.Inline_table_keyvals_non_emptyContext ctx);
	/**
	 * Visit a parse tree produced by {@link TomlParser#array_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArray_table(TomlParser.Array_tableContext ctx);
}