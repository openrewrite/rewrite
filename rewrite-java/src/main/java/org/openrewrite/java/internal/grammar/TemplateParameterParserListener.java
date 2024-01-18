// Generated from java-escape by ANTLR 4.11.1
package org.openrewrite.java.internal.grammar;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link TemplateParameterParser}.
 */
public interface TemplateParameterParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link TemplateParameterParser#matcherPattern}.
	 * @param ctx the parse tree
	 */
	void enterMatcherPattern(TemplateParameterParser.MatcherPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateParameterParser#matcherPattern}.
	 * @param ctx the parse tree
	 */
	void exitMatcherPattern(TemplateParameterParser.MatcherPatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateParameterParser#typedPattern}.
	 * @param ctx the parse tree
	 */
	void enterTypedPattern(TemplateParameterParser.TypedPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateParameterParser#typedPattern}.
	 * @param ctx the parse tree
	 */
	void exitTypedPattern(TemplateParameterParser.TypedPatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateParameterParser#patternType}.
	 * @param ctx the parse tree
	 */
	void enterPatternType(TemplateParameterParser.PatternTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateParameterParser#patternType}.
	 * @param ctx the parse tree
	 */
	void exitPatternType(TemplateParameterParser.PatternTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateParameterParser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(TemplateParameterParser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateParameterParser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(TemplateParameterParser.TypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateParameterParser#typeParameter}.
	 * @param ctx the parse tree
	 */
	void enterTypeParameter(TemplateParameterParser.TypeParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateParameterParser#typeParameter}.
	 * @param ctx the parse tree
	 */
	void exitTypeParameter(TemplateParameterParser.TypeParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateParameterParser#variance}.
	 * @param ctx the parse tree
	 */
	void enterVariance(TemplateParameterParser.VarianceContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateParameterParser#variance}.
	 * @param ctx the parse tree
	 */
	void exitVariance(TemplateParameterParser.VarianceContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateParameterParser#parameterName}.
	 * @param ctx the parse tree
	 */
	void enterParameterName(TemplateParameterParser.ParameterNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateParameterParser#parameterName}.
	 * @param ctx the parse tree
	 */
	void exitParameterName(TemplateParameterParser.ParameterNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateParameterParser#typeName}.
	 * @param ctx the parse tree
	 */
	void enterTypeName(TemplateParameterParser.TypeNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateParameterParser#typeName}.
	 * @param ctx the parse tree
	 */
	void exitTypeName(TemplateParameterParser.TypeNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link TemplateParameterParser#matcherName}.
	 * @param ctx the parse tree
	 */
	void enterMatcherName(TemplateParameterParser.MatcherNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateParameterParser#matcherName}.
	 * @param ctx the parse tree
	 */
	void exitMatcherName(TemplateParameterParser.MatcherNameContext ctx);
}