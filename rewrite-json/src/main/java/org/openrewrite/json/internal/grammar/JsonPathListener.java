// Generated from /Users/jon/Projects/github/openrewrite/rewrite/rewrite-json/src/main/antlr/JsonPath.g4 by ANTLR 4.9.2
package org.openrewrite.json.internal.grammar;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link JsonPath}.
 */
public interface JsonPathListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link JsonPath#jsonpath}.
	 * @param ctx the parse tree
	 */
	void enterJsonpath(JsonPath.JsonpathContext ctx);
	/**
	 * Exit a parse tree produced by {@link JsonPath#jsonpath}.
	 * @param ctx the parse tree
	 */
	void exitJsonpath(JsonPath.JsonpathContext ctx);
	/**
	 * Enter a parse tree produced by the {@code BracketOperator}
	 * labeled alternative in {@link JsonPath#object}.
	 * @param ctx the parse tree
	 */
	void enterBracketOperator(JsonPath.BracketOperatorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code BracketOperator}
	 * labeled alternative in {@link JsonPath#object}.
	 * @param ctx the parse tree
	 */
	void exitBracketOperator(JsonPath.BracketOperatorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DotOperator}
	 * labeled alternative in {@link JsonPath#object}.
	 * @param ctx the parse tree
	 */
	void enterDotOperator(JsonPath.DotOperatorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DotOperator}
	 * labeled alternative in {@link JsonPath#object}.
	 * @param ctx the parse tree
	 */
	void exitDotOperator(JsonPath.DotOperatorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code RecursiveDescent}
	 * labeled alternative in {@link JsonPath#object}.
	 * @param ctx the parse tree
	 */
	void enterRecursiveDescent(JsonPath.RecursiveDescentContext ctx);
	/**
	 * Exit a parse tree produced by the {@code RecursiveDescent}
	 * labeled alternative in {@link JsonPath#object}.
	 * @param ctx the parse tree
	 */
	void exitRecursiveDescent(JsonPath.RecursiveDescentContext ctx);
	/**
	 * Enter a parse tree produced by the {@code UnionOperator}
	 * labeled alternative in {@link JsonPath#object}.
	 * @param ctx the parse tree
	 */
	void enterUnionOperator(JsonPath.UnionOperatorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code UnionOperator}
	 * labeled alternative in {@link JsonPath#object}.
	 * @param ctx the parse tree
	 */
	void exitUnionOperator(JsonPath.UnionOperatorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code RangeOperator}
	 * labeled alternative in {@link JsonPath#object}.
	 * @param ctx the parse tree
	 */
	void enterRangeOperator(JsonPath.RangeOperatorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code RangeOperator}
	 * labeled alternative in {@link JsonPath#object}.
	 * @param ctx the parse tree
	 */
	void exitRangeOperator(JsonPath.RangeOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link JsonPath#rangeOp}.
	 * @param ctx the parse tree
	 */
	void enterRangeOp(JsonPath.RangeOpContext ctx);
	/**
	 * Exit a parse tree produced by {@link JsonPath#rangeOp}.
	 * @param ctx the parse tree
	 */
	void exitRangeOp(JsonPath.RangeOpContext ctx);
	/**
	 * Enter a parse tree produced by {@link JsonPath#start}.
	 * @param ctx the parse tree
	 */
	void enterStart(JsonPath.StartContext ctx);
	/**
	 * Exit a parse tree produced by {@link JsonPath#start}.
	 * @param ctx the parse tree
	 */
	void exitStart(JsonPath.StartContext ctx);
	/**
	 * Enter a parse tree produced by {@link JsonPath#end}.
	 * @param ctx the parse tree
	 */
	void enterEnd(JsonPath.EndContext ctx);
	/**
	 * Exit a parse tree produced by {@link JsonPath#end}.
	 * @param ctx the parse tree
	 */
	void exitEnd(JsonPath.EndContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ParentheticalExpression}
	 * labeled alternative in {@link JsonPath#expression}.
	 * @param ctx the parse tree
	 */
	void enterParentheticalExpression(JsonPath.ParentheticalExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ParentheticalExpression}
	 * labeled alternative in {@link JsonPath#expression}.
	 * @param ctx the parse tree
	 */
	void exitParentheticalExpression(JsonPath.ParentheticalExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Identifier}
	 * labeled alternative in {@link JsonPath#expression}.
	 * @param ctx the parse tree
	 */
	void enterIdentifier(JsonPath.IdentifierContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Identifier}
	 * labeled alternative in {@link JsonPath#expression}.
	 * @param ctx the parse tree
	 */
	void exitIdentifier(JsonPath.IdentifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code AndExpression}
	 * labeled alternative in {@link JsonPath#expression}.
	 * @param ctx the parse tree
	 */
	void enterAndExpression(JsonPath.AndExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code AndExpression}
	 * labeled alternative in {@link JsonPath#expression}.
	 * @param ctx the parse tree
	 */
	void exitAndExpression(JsonPath.AndExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code PathExpression}
	 * labeled alternative in {@link JsonPath#expression}.
	 * @param ctx the parse tree
	 */
	void enterPathExpression(JsonPath.PathExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code PathExpression}
	 * labeled alternative in {@link JsonPath#expression}.
	 * @param ctx the parse tree
	 */
	void exitPathExpression(JsonPath.PathExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ScopedPathExpression}
	 * labeled alternative in {@link JsonPath#expression}.
	 * @param ctx the parse tree
	 */
	void enterScopedPathExpression(JsonPath.ScopedPathExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ScopedPathExpression}
	 * labeled alternative in {@link JsonPath#expression}.
	 * @param ctx the parse tree
	 */
	void exitScopedPathExpression(JsonPath.ScopedPathExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code BinaryExpression}
	 * labeled alternative in {@link JsonPath#expression}.
	 * @param ctx the parse tree
	 */
	void enterBinaryExpression(JsonPath.BinaryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code BinaryExpression}
	 * labeled alternative in {@link JsonPath#expression}.
	 * @param ctx the parse tree
	 */
	void exitBinaryExpression(JsonPath.BinaryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LiteralExpression}
	 * labeled alternative in {@link JsonPath#expression}.
	 * @param ctx the parse tree
	 */
	void enterLiteralExpression(JsonPath.LiteralExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LiteralExpression}
	 * labeled alternative in {@link JsonPath#expression}.
	 * @param ctx the parse tree
	 */
	void exitLiteralExpression(JsonPath.LiteralExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NotExpression}
	 * labeled alternative in {@link JsonPath#expression}.
	 * @param ctx the parse tree
	 */
	void enterNotExpression(JsonPath.NotExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NotExpression}
	 * labeled alternative in {@link JsonPath#expression}.
	 * @param ctx the parse tree
	 */
	void exitNotExpression(JsonPath.NotExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code WildcardExpression}
	 * labeled alternative in {@link JsonPath#expression}.
	 * @param ctx the parse tree
	 */
	void enterWildcardExpression(JsonPath.WildcardExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code WildcardExpression}
	 * labeled alternative in {@link JsonPath#expression}.
	 * @param ctx the parse tree
	 */
	void exitWildcardExpression(JsonPath.WildcardExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code FilterExpression}
	 * labeled alternative in {@link JsonPath#expression}.
	 * @param ctx the parse tree
	 */
	void enterFilterExpression(JsonPath.FilterExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code FilterExpression}
	 * labeled alternative in {@link JsonPath#expression}.
	 * @param ctx the parse tree
	 */
	void exitFilterExpression(JsonPath.FilterExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code OrExpression}
	 * labeled alternative in {@link JsonPath#expression}.
	 * @param ctx the parse tree
	 */
	void enterOrExpression(JsonPath.OrExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code OrExpression}
	 * labeled alternative in {@link JsonPath#expression}.
	 * @param ctx the parse tree
	 */
	void exitOrExpression(JsonPath.OrExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link JsonPath#litExpression}.
	 * @param ctx the parse tree
	 */
	void enterLitExpression(JsonPath.LitExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link JsonPath#litExpression}.
	 * @param ctx the parse tree
	 */
	void exitLitExpression(JsonPath.LitExpressionContext ctx);
}