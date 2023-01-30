/*
 * Copyright 2022 the original author or authors.
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
	 * Enter a parse tree produced by {@link TemplateParameterParser#matcherParameter}.
	 * @param ctx the parse tree
	 */
	void enterMatcherParameter(TemplateParameterParser.MatcherParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link TemplateParameterParser#matcherParameter}.
	 * @param ctx the parse tree
	 */
	void exitMatcherParameter(TemplateParameterParser.MatcherParameterContext ctx);
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