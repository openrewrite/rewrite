// Generated from /Users/jon/Projects/github/openrewrite/rewrite/rewrite-java/src/main/antlr/TemplateParameterParser.g4 by ANTLR 4.9.2
package org.openrewrite.java.internal.grammar;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link TemplateParameterParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface TemplateParameterParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link TemplateParameterParser#matcherPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMatcherPattern(TemplateParameterParser.MatcherPatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link TemplateParameterParser#matcherParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMatcherParameter(TemplateParameterParser.MatcherParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link TemplateParameterParser#matcherName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMatcherName(TemplateParameterParser.MatcherNameContext ctx);
}