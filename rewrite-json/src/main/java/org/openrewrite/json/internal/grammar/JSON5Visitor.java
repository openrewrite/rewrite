// Generated from /Users/knut/git/openrewrite/rewrite/rewrite-json/src/main/antlr/JSON5.g4 by ANTLR 4.13.2
package org.openrewrite.json.internal.grammar;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link JSON5Parser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface JSON5Visitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link JSON5Parser#json5}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJson5(JSON5Parser.Json5Context ctx);
	/**
	 * Visit a parse tree produced by {@link JSON5Parser#obj}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObj(JSON5Parser.ObjContext ctx);
	/**
	 * Visit a parse tree produced by {@link JSON5Parser#member}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMember(JSON5Parser.MemberContext ctx);
	/**
	 * Visit a parse tree produced by {@link JSON5Parser#key}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKey(JSON5Parser.KeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link JSON5Parser#value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValue(JSON5Parser.ValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link JSON5Parser#arr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArr(JSON5Parser.ArrContext ctx);
	/**
	 * Visit a parse tree produced by {@link JSON5Parser#number}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumber(JSON5Parser.NumberContext ctx);
}