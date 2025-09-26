// Generated from /Users/knut/git/openrewrite/rewrite/rewrite-java/src/main/antlr/MethodSignatureParser.g4 by ANTLR 4.13.2
package org.openrewrite.java.internal.grammar;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link MethodSignatureParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface MethodSignatureParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link MethodSignatureParser#methodPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMethodPattern(MethodSignatureParser.MethodPatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link MethodSignatureParser#formalParametersPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormalParametersPattern(MethodSignatureParser.FormalParametersPatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link MethodSignatureParser#formalsPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormalsPattern(MethodSignatureParser.FormalsPatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link MethodSignatureParser#formalsTail}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormalsTail(MethodSignatureParser.FormalsTailContext ctx);
	/**
	 * Visit a parse tree produced by {@link MethodSignatureParser#formalsPatternAfterDotDot}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormalsPatternAfterDotDot(MethodSignatureParser.FormalsPatternAfterDotDotContext ctx);
	/**
	 * Visit a parse tree produced by {@link MethodSignatureParser#targetTypePattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTargetTypePattern(MethodSignatureParser.TargetTypePatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link MethodSignatureParser#formalTypePattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormalTypePattern(MethodSignatureParser.FormalTypePatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link MethodSignatureParser#classNameOrInterface}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassNameOrInterface(MethodSignatureParser.ClassNameOrInterfaceContext ctx);
	/**
	 * Visit a parse tree produced by {@link MethodSignatureParser#arrayDimensions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayDimensions(MethodSignatureParser.ArrayDimensionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link MethodSignatureParser#simpleNamePattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimpleNamePattern(MethodSignatureParser.SimpleNamePatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link MethodSignatureParser#simpleNamePart}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimpleNamePart(MethodSignatureParser.SimpleNamePartContext ctx);
}