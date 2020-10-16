// Generated from /Users/jon/Projects/github/openrewrite/rewrite/rewrite-maven2/src/main/antlr/VersionRangeParser.g4 by ANTLR 4.8
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
	 * Visit a parse tree produced by {@link VersionRangeParser#requestedVersion}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRequestedVersion(VersionRangeParser.RequestedVersionContext ctx);
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