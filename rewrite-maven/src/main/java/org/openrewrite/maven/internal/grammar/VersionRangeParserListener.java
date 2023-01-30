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
package org.openrewrite.maven.internal.grammar;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link VersionRangeParser}.
 */
public interface VersionRangeParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link VersionRangeParser#versionRequirement}.
	 * @param ctx the parse tree
	 */
	void enterVersionRequirement(VersionRangeParser.VersionRequirementContext ctx);
	/**
	 * Exit a parse tree produced by {@link VersionRangeParser#versionRequirement}.
	 * @param ctx the parse tree
	 */
	void exitVersionRequirement(VersionRangeParser.VersionRequirementContext ctx);
	/**
	 * Enter a parse tree produced by {@link VersionRangeParser#range}.
	 * @param ctx the parse tree
	 */
	void enterRange(VersionRangeParser.RangeContext ctx);
	/**
	 * Exit a parse tree produced by {@link VersionRangeParser#range}.
	 * @param ctx the parse tree
	 */
	void exitRange(VersionRangeParser.RangeContext ctx);
	/**
	 * Enter a parse tree produced by {@link VersionRangeParser#bounds}.
	 * @param ctx the parse tree
	 */
	void enterBounds(VersionRangeParser.BoundsContext ctx);
	/**
	 * Exit a parse tree produced by {@link VersionRangeParser#bounds}.
	 * @param ctx the parse tree
	 */
	void exitBounds(VersionRangeParser.BoundsContext ctx);
	/**
	 * Enter a parse tree produced by {@link VersionRangeParser#exactly}.
	 * @param ctx the parse tree
	 */
	void enterExactly(VersionRangeParser.ExactlyContext ctx);
	/**
	 * Exit a parse tree produced by {@link VersionRangeParser#exactly}.
	 * @param ctx the parse tree
	 */
	void exitExactly(VersionRangeParser.ExactlyContext ctx);
	/**
	 * Enter a parse tree produced by {@link VersionRangeParser#boundedLower}.
	 * @param ctx the parse tree
	 */
	void enterBoundedLower(VersionRangeParser.BoundedLowerContext ctx);
	/**
	 * Exit a parse tree produced by {@link VersionRangeParser#boundedLower}.
	 * @param ctx the parse tree
	 */
	void exitBoundedLower(VersionRangeParser.BoundedLowerContext ctx);
	/**
	 * Enter a parse tree produced by {@link VersionRangeParser#unboundedLower}.
	 * @param ctx the parse tree
	 */
	void enterUnboundedLower(VersionRangeParser.UnboundedLowerContext ctx);
	/**
	 * Exit a parse tree produced by {@link VersionRangeParser#unboundedLower}.
	 * @param ctx the parse tree
	 */
	void exitUnboundedLower(VersionRangeParser.UnboundedLowerContext ctx);
	/**
	 * Enter a parse tree produced by {@link VersionRangeParser#version}.
	 * @param ctx the parse tree
	 */
	void enterVersion(VersionRangeParser.VersionContext ctx);
	/**
	 * Exit a parse tree produced by {@link VersionRangeParser#version}.
	 * @param ctx the parse tree
	 */
	void exitVersion(VersionRangeParser.VersionContext ctx);
}