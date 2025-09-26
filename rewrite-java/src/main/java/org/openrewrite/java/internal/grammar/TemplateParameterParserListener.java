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
// Generated from /Users/jon/Projects/github/openrewrite/rewrite/rewrite-java/src/main/antlr/TemplateParameterParser.g4 by ANTLR 4.13.2
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
	 * Enter a parse tree produced by {@link TemplateParameterParser#genericPattern}.
	 * @param ctx the parse tree
	 */
	void enterGenericPattern(TemplateParameterParser.GenericPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateParameterParser#genericPattern}.
	 * @param ctx the parse tree
	 */
	void exitGenericPattern(TemplateParameterParser.GenericPatternContext ctx);
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
	 * Enter a parse tree produced by {@link TemplateParameterParser#typeArray}.
	 * @param ctx the parse tree
	 */
	void enterTypeArray(TemplateParameterParser.TypeArrayContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateParameterParser#typeArray}.
	 * @param ctx the parse tree
	 */
	void exitTypeArray(TemplateParameterParser.TypeArrayContext ctx);
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
	 * Enter a parse tree produced by {@link TemplateParameterParser#genericName}.
	 * @param ctx the parse tree
	 */
	void enterGenericName(TemplateParameterParser.GenericNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateParameterParser#genericName}.
	 * @param ctx the parse tree
	 */
	void exitGenericName(TemplateParameterParser.GenericNameContext ctx);
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