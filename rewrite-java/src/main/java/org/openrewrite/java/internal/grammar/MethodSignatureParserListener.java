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
	 * Enter a parse tree produced by {@link MethodSignatureParser#dotDot}.
	 * @param ctx the parse tree
	 */
	void enterDotDot(MethodSignatureParser.DotDotContext ctx);
	/**
	 * Exit a parse tree produced by {@link MethodSignatureParser#dotDot}.
	 * @param ctx the parse tree
	 */
	void exitDotDot(MethodSignatureParser.DotDotContext ctx);
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
	 * Enter a parse tree produced by {@link MethodSignatureParser#optionalParensTypePattern}.
	 * @param ctx the parse tree
	 */
	void enterOptionalParensTypePattern(MethodSignatureParser.OptionalParensTypePatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link MethodSignatureParser#optionalParensTypePattern}.
	 * @param ctx the parse tree
	 */
	void exitOptionalParensTypePattern(MethodSignatureParser.OptionalParensTypePatternContext ctx);
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
	 * Enter a parse tree produced by {@link MethodSignatureParser#simpleNamePattern}.
	 * @param ctx the parse tree
	 */
	void enterSimpleNamePattern(MethodSignatureParser.SimpleNamePatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link MethodSignatureParser#simpleNamePattern}.
	 * @param ctx the parse tree
	 */
	void exitSimpleNamePattern(MethodSignatureParser.SimpleNamePatternContext ctx);
}