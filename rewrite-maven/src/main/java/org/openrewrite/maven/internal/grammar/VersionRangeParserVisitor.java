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
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link VersionRangeParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface VersionRangeParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link VersionRangeParser#versionRequirement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVersionRequirement(VersionRangeParser.VersionRequirementContext ctx);
	/**
	 * Visit a parse tree produced by {@link VersionRangeParser#range}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRange(VersionRangeParser.RangeContext ctx);
	/**
	 * Visit a parse tree produced by {@link VersionRangeParser#bounds}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBounds(VersionRangeParser.BoundsContext ctx);
	/**
	 * Visit a parse tree produced by {@link VersionRangeParser#exactly}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExactly(VersionRangeParser.ExactlyContext ctx);
	/**
	 * Visit a parse tree produced by {@link VersionRangeParser#boundedLower}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBoundedLower(VersionRangeParser.BoundedLowerContext ctx);
	/**
	 * Visit a parse tree produced by {@link VersionRangeParser#unboundedLower}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnboundedLower(VersionRangeParser.UnboundedLowerContext ctx);
	/**
	 * Visit a parse tree produced by {@link VersionRangeParser#version}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVersion(VersionRangeParser.VersionContext ctx);
}