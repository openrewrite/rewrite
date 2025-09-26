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
// Generated from /Users/jon/Projects/github/openrewrite/rewrite/rewrite-java/src/main/antlr/MethodSignatureParser.g4 by ANTLR 4.13.2
package org.openrewrite.java.internal.grammar;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link MethodSignatureParser}.
 */
public interface MethodSignatureParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link MethodSignatureParser#methodPattern}.
	 * @param ctx the parse tree
	 */
	void enterMethodPattern(MethodSignatureParser.MethodPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link MethodSignatureParser#methodPattern}.
	 * @param ctx the parse tree
	 */
	void exitMethodPattern(MethodSignatureParser.MethodPatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link MethodSignatureParser#formalParametersPattern}.
	 * @param ctx the parse tree
	 */
	void enterFormalParametersPattern(MethodSignatureParser.FormalParametersPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link MethodSignatureParser#formalParametersPattern}.
	 * @param ctx the parse tree
	 */
	void exitFormalParametersPattern(MethodSignatureParser.FormalParametersPatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link MethodSignatureParser#formalsPattern}.
	 * @param ctx the parse tree
	 */
	void enterFormalsPattern(MethodSignatureParser.FormalsPatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link MethodSignatureParser#formalsPattern}.
	 * @param ctx the parse tree
	 */
	void exitFormalsPattern(MethodSignatureParser.FormalsPatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link MethodSignatureParser#formalsTail}.
	 * @param ctx the parse tree
	 */
	void enterFormalsTail(MethodSignatureParser.FormalsTailContext ctx);
	/**
	 * Exit a parse tree produced by {@link MethodSignatureParser#formalsTail}.
	 * @param ctx the parse tree
	 */
	void exitFormalsTail(MethodSignatureParser.FormalsTailContext ctx);
	/**
	 * Enter a parse tree produced by {@link MethodSignatureParser#formalsPatternAfterDotDot}.
	 * @param ctx the parse tree
	 */
	void enterFormalsPatternAfterDotDot(MethodSignatureParser.FormalsPatternAfterDotDotContext ctx);
	/**
	 * Exit a parse tree produced by {@link MethodSignatureParser#formalsPatternAfterDotDot}.
	 * @param ctx the parse tree
	 */
	void exitFormalsPatternAfterDotDot(MethodSignatureParser.FormalsPatternAfterDotDotContext ctx);
	/**
	 * Enter a parse tree produced by {@link MethodSignatureParser#targetTypePattern}.
	 * @param ctx the parse tree
	 */
	void enterTargetTypePattern(MethodSignatureParser.TargetTypePatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link MethodSignatureParser#targetTypePattern}.
	 * @param ctx the parse tree
	 */
	void exitTargetTypePattern(MethodSignatureParser.TargetTypePatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link MethodSignatureParser#formalTypePattern}.
	 * @param ctx the parse tree
	 */
	void enterFormalTypePattern(MethodSignatureParser.FormalTypePatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link MethodSignatureParser#formalTypePattern}.
	 * @param ctx the parse tree
	 */
	void exitFormalTypePattern(MethodSignatureParser.FormalTypePatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link MethodSignatureParser#classNameOrInterface}.
	 * @param ctx the parse tree
	 */
	void enterClassNameOrInterface(MethodSignatureParser.ClassNameOrInterfaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link MethodSignatureParser#classNameOrInterface}.
	 * @param ctx the parse tree
	 */
	void exitClassNameOrInterface(MethodSignatureParser.ClassNameOrInterfaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link MethodSignatureParser#arrayDimensions}.
	 * @param ctx the parse tree
	 */
	void enterArrayDimensions(MethodSignatureParser.ArrayDimensionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link MethodSignatureParser#arrayDimensions}.
	 * @param ctx the parse tree
	 */
	void exitArrayDimensions(MethodSignatureParser.ArrayDimensionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link MethodSignatureParser#simpleNamePattern}.
	 * @param ctx the parse tree
	 */
	void enterSimpleNamePattern(MethodSignatureParser.SimpleNamePatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link MethodSignatureParser#simpleNamePattern}.
	 * @param ctx the parse tree
	 */
	void exitSimpleNamePattern(MethodSignatureParser.SimpleNamePatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link MethodSignatureParser#simpleNamePart}.
	 * @param ctx the parse tree
	 */
	void enterSimpleNamePart(MethodSignatureParser.SimpleNamePartContext ctx);
	/**
	 * Exit a parse tree produced by {@link MethodSignatureParser#simpleNamePart}.
	 * @param ctx the parse tree
	 */
	void exitSimpleNamePart(MethodSignatureParser.SimpleNamePartContext ctx);
}