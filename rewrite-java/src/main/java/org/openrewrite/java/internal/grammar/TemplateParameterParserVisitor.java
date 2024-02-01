/*
 * Copyright 2024 the original author or authors.
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