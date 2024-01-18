// Generated from java-escape by ANTLR 4.11.1
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
	 * Visit a parse tree produced by {@link TemplateParameterParser#typedPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypedPattern(TemplateParameterParser.TypedPatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link TemplateParameterParser#patternType}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPatternType(TemplateParameterParser.PatternTypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link TemplateParameterParser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType(TemplateParameterParser.TypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link TemplateParameterParser#typeParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeParameter(TemplateParameterParser.TypeParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link TemplateParameterParser#variance}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariance(TemplateParameterParser.VarianceContext ctx);
	/**
	 * Visit a parse tree produced by {@link TemplateParameterParser#parameterName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameterName(TemplateParameterParser.ParameterNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link TemplateParameterParser#typeName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeName(TemplateParameterParser.TypeNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link TemplateParameterParser#matcherName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMatcherName(TemplateParameterParser.MatcherNameContext ctx);
}