// Generated from /Users/knut/git/openrewrite/rewrite/rewrite-java/src/main/antlr/MethodSignatureParser.g4 by ANTLR 4.13.2
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