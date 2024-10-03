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
	 * Enter a parse tree produced by {@link TomlParser#key_value}.
	 * @param ctx the parse tree
	 */
	void enterKey_value(TomlParser.Key_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#key_value}.
	 * @param ctx the parse tree
	 */
	void exitKey_value(TomlParser.Key_valueContext ctx);
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
	 * Enter a parse tree produced by {@link TomlParser#simple_key}.
	 * @param ctx the parse tree
	 */
	void enterSimple_key(TomlParser.Simple_keyContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#simple_key}.
	 * @param ctx the parse tree
	 */
	void exitSimple_key(TomlParser.Simple_keyContext ctx);
	/**
	 * Enter a parse tree produced by {@link TomlParser#unquoted_key}.
	 * @param ctx the parse tree
	 */
	void enterUnquoted_key(TomlParser.Unquoted_keyContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#unquoted_key}.
	 * @param ctx the parse tree
	 */
	void exitUnquoted_key(TomlParser.Unquoted_keyContext ctx);
	/**
	 * Enter a parse tree produced by {@link TomlParser#quoted_key}.
	 * @param ctx the parse tree
	 */
	void enterQuoted_key(TomlParser.Quoted_keyContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#quoted_key}.
	 * @param ctx the parse tree
	 */
	void exitQuoted_key(TomlParser.Quoted_keyContext ctx);
	/**
	 * Enter a parse tree produced by {@link TomlParser#dotted_key}.
	 * @param ctx the parse tree
	 */
	void enterDotted_key(TomlParser.Dotted_keyContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#dotted_key}.
	 * @param ctx the parse tree
	 */
	void exitDotted_key(TomlParser.Dotted_keyContext ctx);
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
	 * Enter a parse tree produced by {@link TomlParser#floating_point}.
	 * @param ctx the parse tree
	 */
	void enterFloating_point(TomlParser.Floating_pointContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#floating_point}.
	 * @param ctx the parse tree
	 */
	void exitFloating_point(TomlParser.Floating_pointContext ctx);
	/**
	 * Enter a parse tree produced by {@link TomlParser#bool_}.
	 * @param ctx the parse tree
	 */
	void enterBool_(TomlParser.Bool_Context ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#bool_}.
	 * @param ctx the parse tree
	 */
	void exitBool_(TomlParser.Bool_Context ctx);
	/**
	 * Enter a parse tree produced by {@link TomlParser#date_time}.
	 * @param ctx the parse tree
	 */
	void enterDate_time(TomlParser.Date_timeContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#date_time}.
	 * @param ctx the parse tree
	 */
	void exitDate_time(TomlParser.Date_timeContext ctx);
	/**
	 * Enter a parse tree produced by {@link TomlParser#array_}.
	 * @param ctx the parse tree
	 */
	void enterArray_(TomlParser.Array_Context ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#array_}.
	 * @param ctx the parse tree
	 */
	void exitArray_(TomlParser.Array_Context ctx);
	/**
	 * Enter a parse tree produced by {@link TomlParser#array_values}.
	 * @param ctx the parse tree
	 */
	void enterArray_values(TomlParser.Array_valuesContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#array_values}.
	 * @param ctx the parse tree
	 */
	void exitArray_values(TomlParser.Array_valuesContext ctx);
	/**
	 * Enter a parse tree produced by {@link TomlParser#comment_or_nl}.
	 * @param ctx the parse tree
	 */
	void enterComment_or_nl(TomlParser.Comment_or_nlContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#comment_or_nl}.
	 * @param ctx the parse tree
	 */
	void exitComment_or_nl(TomlParser.Comment_or_nlContext ctx);
	/**
	 * Enter a parse tree produced by {@link TomlParser#nl_or_comment}.
	 * @param ctx the parse tree
	 */
	void enterNl_or_comment(TomlParser.Nl_or_commentContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#nl_or_comment}.
	 * @param ctx the parse tree
	 */
	void exitNl_or_comment(TomlParser.Nl_or_commentContext ctx);
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
	 * Enter a parse tree produced by {@link TomlParser#standard_table}.
	 * @param ctx the parse tree
	 */
	void enterStandard_table(TomlParser.Standard_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#standard_table}.
	 * @param ctx the parse tree
	 */
	void exitStandard_table(TomlParser.Standard_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link TomlParser#inline_table}.
	 * @param ctx the parse tree
	 */
	void enterInline_table(TomlParser.Inline_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#inline_table}.
	 * @param ctx the parse tree
	 */
	void exitInline_table(TomlParser.Inline_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link TomlParser#inline_table_keyvals}.
	 * @param ctx the parse tree
	 */
	void enterInline_table_keyvals(TomlParser.Inline_table_keyvalsContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#inline_table_keyvals}.
	 * @param ctx the parse tree
	 */
	void exitInline_table_keyvals(TomlParser.Inline_table_keyvalsContext ctx);
	/**
	 * Enter a parse tree produced by {@link TomlParser#inline_table_keyvals_non_empty}.
	 * @param ctx the parse tree
	 */
	void enterInline_table_keyvals_non_empty(TomlParser.Inline_table_keyvals_non_emptyContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#inline_table_keyvals_non_empty}.
	 * @param ctx the parse tree
	 */
	void exitInline_table_keyvals_non_empty(TomlParser.Inline_table_keyvals_non_emptyContext ctx);
	/**
	 * Enter a parse tree produced by {@link TomlParser#array_table}.
	 * @param ctx the parse tree
	 */
	void enterArray_table(TomlParser.Array_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link TomlParser#array_table}.
	 * @param ctx the parse tree
	 */
	void exitArray_table(TomlParser.Array_tableContext ctx);
}