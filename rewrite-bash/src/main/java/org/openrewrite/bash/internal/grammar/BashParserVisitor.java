/*
 * Copyright 2026 the original author or authors.
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
// Generated from /Users/kevin/conductor/workspaces/moderne-product-management/tripoli/working-set-bash-parser/rewrite/rewrite-bash/src/main/antlr/BashParser.g4 by ANTLR 4.13.2
package org.openrewrite.bash.internal.grammar;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link BashParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface BashParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link BashParser#program}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgram(BashParser.ProgramContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#shebang}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShebang(BashParser.ShebangContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#completeCommands}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompleteCommands(BashParser.CompleteCommandsContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#completeCommand}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompleteCommand(BashParser.CompleteCommandContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitList(BashParser.ListContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#listSep}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitListSep(BashParser.ListSepContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#andOr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAndOr(BashParser.AndOrContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#andOrOp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAndOrOp(BashParser.AndOrOpContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#pipeline}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPipeline(BashParser.PipelineContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#bangOpt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBangOpt(BashParser.BangOptContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#timeOpt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTimeOpt(BashParser.TimeOptContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#pipeSequence}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPipeSequence(BashParser.PipeSequenceContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#pipeOp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPipeOp(BashParser.PipeOpContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#command}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommand(BashParser.CommandContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#simpleCommand}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimpleCommand(BashParser.SimpleCommandContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#cmdPrefix}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCmdPrefix(BashParser.CmdPrefixContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#cmdWord}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCmdWord(BashParser.CmdWordContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#cmdSuffix}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCmdSuffix(BashParser.CmdSuffixContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#compoundCommand}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompoundCommand(BashParser.CompoundCommandContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#braceGroup}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBraceGroup(BashParser.BraceGroupContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#subshell}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubshell(BashParser.SubshellContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#compoundList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompoundList(BashParser.CompoundListContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#ifClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfClause(BashParser.IfClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#elifClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElifClause(BashParser.ElifClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#elseClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElseClause(BashParser.ElseClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#forClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForClause(BashParser.ForClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#forBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForBody(BashParser.ForBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#inClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInClause(BashParser.InClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#cStyleForClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCStyleForClause(BashParser.CStyleForClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#whileClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhileClause(BashParser.WhileClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#untilClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUntilClause(BashParser.UntilClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#doGroup}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDoGroup(BashParser.DoGroupContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#selectClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelectClause(BashParser.SelectClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#caseClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCaseClause(BashParser.CaseClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#caseItem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCaseItem(BashParser.CaseItemContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#caseSeparator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCaseSeparator(BashParser.CaseSeparatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPattern(BashParser.PatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#doubleParenExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDoubleParenExpr(BashParser.DoubleParenExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#arithmeticExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArithmeticExpr(BashParser.ArithmeticExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#arithmeticPart}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArithmeticPart(BashParser.ArithmeticPartContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#doubleBracketExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDoubleBracketExpr(BashParser.DoubleBracketExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#conditionExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConditionExpr(BashParser.ConditionExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#conditionPart}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConditionPart(BashParser.ConditionPartContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#functionDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionDefinition(BashParser.FunctionDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#functionParens}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionParens(BashParser.FunctionParensContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#functionBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionBody(BashParser.FunctionBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#assignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment(BashParser.AssignmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#assignmentValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignmentValue(BashParser.AssignmentValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#arrayElements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArrayElements(BashParser.ArrayElementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#redirectionList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRedirectionList(BashParser.RedirectionListContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#redirection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRedirection(BashParser.RedirectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#redirectionOp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRedirectionOp(BashParser.RedirectionOpContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#heredoc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHeredoc(BashParser.HeredocContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#heredocDelimiter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHeredocDelimiter(BashParser.HeredocDelimiterContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#heredocBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHeredocBody(BashParser.HeredocBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#heredocLine}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHeredocLine(BashParser.HeredocLineContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#word}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWord(BashParser.WordContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#wordPart}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWordPart(BashParser.WordPartContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#doubleQuotedString}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDoubleQuotedString(BashParser.DoubleQuotedStringContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#doubleQuotedPart}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDoubleQuotedPart(BashParser.DoubleQuotedPartContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#commandSubstitution}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommandSubstitution(BashParser.CommandSubstitutionContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#commandSubstitutionContent}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommandSubstitutionContent(BashParser.CommandSubstitutionContentContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#backtickContent}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBacktickContent(BashParser.BacktickContentContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#arithmeticSubstitution}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArithmeticSubstitution(BashParser.ArithmeticSubstitutionContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#processSubstitution}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcessSubstitution(BashParser.ProcessSubstitutionContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#braceExpansionContent}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBraceExpansionContent(BashParser.BraceExpansionContentContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#braceExpansionPart}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBraceExpansionPart(BashParser.BraceExpansionPartContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#separator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSeparator(BashParser.SeparatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#sequentialSep}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSequentialSep(BashParser.SequentialSepContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#newlineList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNewlineList(BashParser.NewlineListContext ctx);
	/**
	 * Visit a parse tree produced by {@link BashParser#linebreak}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLinebreak(BashParser.LinebreakContext ctx);
}