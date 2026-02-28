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
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link BashParser}.
 */
public interface BashParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link BashParser#program}.
	 * @param ctx the parse tree
	 */
	void enterProgram(BashParser.ProgramContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#program}.
	 * @param ctx the parse tree
	 */
	void exitProgram(BashParser.ProgramContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#shebang}.
	 * @param ctx the parse tree
	 */
	void enterShebang(BashParser.ShebangContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#shebang}.
	 * @param ctx the parse tree
	 */
	void exitShebang(BashParser.ShebangContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#completeCommands}.
	 * @param ctx the parse tree
	 */
	void enterCompleteCommands(BashParser.CompleteCommandsContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#completeCommands}.
	 * @param ctx the parse tree
	 */
	void exitCompleteCommands(BashParser.CompleteCommandsContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#completeCommand}.
	 * @param ctx the parse tree
	 */
	void enterCompleteCommand(BashParser.CompleteCommandContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#completeCommand}.
	 * @param ctx the parse tree
	 */
	void exitCompleteCommand(BashParser.CompleteCommandContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#list}.
	 * @param ctx the parse tree
	 */
	void enterList(BashParser.ListContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#list}.
	 * @param ctx the parse tree
	 */
	void exitList(BashParser.ListContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#listSep}.
	 * @param ctx the parse tree
	 */
	void enterListSep(BashParser.ListSepContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#listSep}.
	 * @param ctx the parse tree
	 */
	void exitListSep(BashParser.ListSepContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#andOr}.
	 * @param ctx the parse tree
	 */
	void enterAndOr(BashParser.AndOrContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#andOr}.
	 * @param ctx the parse tree
	 */
	void exitAndOr(BashParser.AndOrContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#andOrOp}.
	 * @param ctx the parse tree
	 */
	void enterAndOrOp(BashParser.AndOrOpContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#andOrOp}.
	 * @param ctx the parse tree
	 */
	void exitAndOrOp(BashParser.AndOrOpContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#pipeline}.
	 * @param ctx the parse tree
	 */
	void enterPipeline(BashParser.PipelineContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#pipeline}.
	 * @param ctx the parse tree
	 */
	void exitPipeline(BashParser.PipelineContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#bangOpt}.
	 * @param ctx the parse tree
	 */
	void enterBangOpt(BashParser.BangOptContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#bangOpt}.
	 * @param ctx the parse tree
	 */
	void exitBangOpt(BashParser.BangOptContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#timeOpt}.
	 * @param ctx the parse tree
	 */
	void enterTimeOpt(BashParser.TimeOptContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#timeOpt}.
	 * @param ctx the parse tree
	 */
	void exitTimeOpt(BashParser.TimeOptContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#pipeSequence}.
	 * @param ctx the parse tree
	 */
	void enterPipeSequence(BashParser.PipeSequenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#pipeSequence}.
	 * @param ctx the parse tree
	 */
	void exitPipeSequence(BashParser.PipeSequenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#pipeOp}.
	 * @param ctx the parse tree
	 */
	void enterPipeOp(BashParser.PipeOpContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#pipeOp}.
	 * @param ctx the parse tree
	 */
	void exitPipeOp(BashParser.PipeOpContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#command}.
	 * @param ctx the parse tree
	 */
	void enterCommand(BashParser.CommandContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#command}.
	 * @param ctx the parse tree
	 */
	void exitCommand(BashParser.CommandContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#simpleCommand}.
	 * @param ctx the parse tree
	 */
	void enterSimpleCommand(BashParser.SimpleCommandContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#simpleCommand}.
	 * @param ctx the parse tree
	 */
	void exitSimpleCommand(BashParser.SimpleCommandContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#cmdPrefix}.
	 * @param ctx the parse tree
	 */
	void enterCmdPrefix(BashParser.CmdPrefixContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#cmdPrefix}.
	 * @param ctx the parse tree
	 */
	void exitCmdPrefix(BashParser.CmdPrefixContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#cmdWord}.
	 * @param ctx the parse tree
	 */
	void enterCmdWord(BashParser.CmdWordContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#cmdWord}.
	 * @param ctx the parse tree
	 */
	void exitCmdWord(BashParser.CmdWordContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#cmdSuffix}.
	 * @param ctx the parse tree
	 */
	void enterCmdSuffix(BashParser.CmdSuffixContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#cmdSuffix}.
	 * @param ctx the parse tree
	 */
	void exitCmdSuffix(BashParser.CmdSuffixContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#compoundCommand}.
	 * @param ctx the parse tree
	 */
	void enterCompoundCommand(BashParser.CompoundCommandContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#compoundCommand}.
	 * @param ctx the parse tree
	 */
	void exitCompoundCommand(BashParser.CompoundCommandContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#braceGroup}.
	 * @param ctx the parse tree
	 */
	void enterBraceGroup(BashParser.BraceGroupContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#braceGroup}.
	 * @param ctx the parse tree
	 */
	void exitBraceGroup(BashParser.BraceGroupContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#subshell}.
	 * @param ctx the parse tree
	 */
	void enterSubshell(BashParser.SubshellContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#subshell}.
	 * @param ctx the parse tree
	 */
	void exitSubshell(BashParser.SubshellContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#compoundList}.
	 * @param ctx the parse tree
	 */
	void enterCompoundList(BashParser.CompoundListContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#compoundList}.
	 * @param ctx the parse tree
	 */
	void exitCompoundList(BashParser.CompoundListContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#ifClause}.
	 * @param ctx the parse tree
	 */
	void enterIfClause(BashParser.IfClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#ifClause}.
	 * @param ctx the parse tree
	 */
	void exitIfClause(BashParser.IfClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#elifClause}.
	 * @param ctx the parse tree
	 */
	void enterElifClause(BashParser.ElifClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#elifClause}.
	 * @param ctx the parse tree
	 */
	void exitElifClause(BashParser.ElifClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#elseClause}.
	 * @param ctx the parse tree
	 */
	void enterElseClause(BashParser.ElseClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#elseClause}.
	 * @param ctx the parse tree
	 */
	void exitElseClause(BashParser.ElseClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#forClause}.
	 * @param ctx the parse tree
	 */
	void enterForClause(BashParser.ForClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#forClause}.
	 * @param ctx the parse tree
	 */
	void exitForClause(BashParser.ForClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#forBody}.
	 * @param ctx the parse tree
	 */
	void enterForBody(BashParser.ForBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#forBody}.
	 * @param ctx the parse tree
	 */
	void exitForBody(BashParser.ForBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#inClause}.
	 * @param ctx the parse tree
	 */
	void enterInClause(BashParser.InClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#inClause}.
	 * @param ctx the parse tree
	 */
	void exitInClause(BashParser.InClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#cStyleForClause}.
	 * @param ctx the parse tree
	 */
	void enterCStyleForClause(BashParser.CStyleForClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#cStyleForClause}.
	 * @param ctx the parse tree
	 */
	void exitCStyleForClause(BashParser.CStyleForClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#whileClause}.
	 * @param ctx the parse tree
	 */
	void enterWhileClause(BashParser.WhileClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#whileClause}.
	 * @param ctx the parse tree
	 */
	void exitWhileClause(BashParser.WhileClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#untilClause}.
	 * @param ctx the parse tree
	 */
	void enterUntilClause(BashParser.UntilClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#untilClause}.
	 * @param ctx the parse tree
	 */
	void exitUntilClause(BashParser.UntilClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#doGroup}.
	 * @param ctx the parse tree
	 */
	void enterDoGroup(BashParser.DoGroupContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#doGroup}.
	 * @param ctx the parse tree
	 */
	void exitDoGroup(BashParser.DoGroupContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#selectClause}.
	 * @param ctx the parse tree
	 */
	void enterSelectClause(BashParser.SelectClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#selectClause}.
	 * @param ctx the parse tree
	 */
	void exitSelectClause(BashParser.SelectClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#caseClause}.
	 * @param ctx the parse tree
	 */
	void enterCaseClause(BashParser.CaseClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#caseClause}.
	 * @param ctx the parse tree
	 */
	void exitCaseClause(BashParser.CaseClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#caseItem}.
	 * @param ctx the parse tree
	 */
	void enterCaseItem(BashParser.CaseItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#caseItem}.
	 * @param ctx the parse tree
	 */
	void exitCaseItem(BashParser.CaseItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#caseSeparator}.
	 * @param ctx the parse tree
	 */
	void enterCaseSeparator(BashParser.CaseSeparatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#caseSeparator}.
	 * @param ctx the parse tree
	 */
	void exitCaseSeparator(BashParser.CaseSeparatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#pattern}.
	 * @param ctx the parse tree
	 */
	void enterPattern(BashParser.PatternContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#pattern}.
	 * @param ctx the parse tree
	 */
	void exitPattern(BashParser.PatternContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#doubleParenExpr}.
	 * @param ctx the parse tree
	 */
	void enterDoubleParenExpr(BashParser.DoubleParenExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#doubleParenExpr}.
	 * @param ctx the parse tree
	 */
	void exitDoubleParenExpr(BashParser.DoubleParenExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#arithmeticExpr}.
	 * @param ctx the parse tree
	 */
	void enterArithmeticExpr(BashParser.ArithmeticExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#arithmeticExpr}.
	 * @param ctx the parse tree
	 */
	void exitArithmeticExpr(BashParser.ArithmeticExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#arithmeticPart}.
	 * @param ctx the parse tree
	 */
	void enterArithmeticPart(BashParser.ArithmeticPartContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#arithmeticPart}.
	 * @param ctx the parse tree
	 */
	void exitArithmeticPart(BashParser.ArithmeticPartContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#doubleBracketExpr}.
	 * @param ctx the parse tree
	 */
	void enterDoubleBracketExpr(BashParser.DoubleBracketExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#doubleBracketExpr}.
	 * @param ctx the parse tree
	 */
	void exitDoubleBracketExpr(BashParser.DoubleBracketExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#conditionExpr}.
	 * @param ctx the parse tree
	 */
	void enterConditionExpr(BashParser.ConditionExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#conditionExpr}.
	 * @param ctx the parse tree
	 */
	void exitConditionExpr(BashParser.ConditionExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#conditionPart}.
	 * @param ctx the parse tree
	 */
	void enterConditionPart(BashParser.ConditionPartContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#conditionPart}.
	 * @param ctx the parse tree
	 */
	void exitConditionPart(BashParser.ConditionPartContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#functionDefinition}.
	 * @param ctx the parse tree
	 */
	void enterFunctionDefinition(BashParser.FunctionDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#functionDefinition}.
	 * @param ctx the parse tree
	 */
	void exitFunctionDefinition(BashParser.FunctionDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#functionParens}.
	 * @param ctx the parse tree
	 */
	void enterFunctionParens(BashParser.FunctionParensContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#functionParens}.
	 * @param ctx the parse tree
	 */
	void exitFunctionParens(BashParser.FunctionParensContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#functionBody}.
	 * @param ctx the parse tree
	 */
	void enterFunctionBody(BashParser.FunctionBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#functionBody}.
	 * @param ctx the parse tree
	 */
	void exitFunctionBody(BashParser.FunctionBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#assignment}.
	 * @param ctx the parse tree
	 */
	void enterAssignment(BashParser.AssignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#assignment}.
	 * @param ctx the parse tree
	 */
	void exitAssignment(BashParser.AssignmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#assignmentValue}.
	 * @param ctx the parse tree
	 */
	void enterAssignmentValue(BashParser.AssignmentValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#assignmentValue}.
	 * @param ctx the parse tree
	 */
	void exitAssignmentValue(BashParser.AssignmentValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#arrayElements}.
	 * @param ctx the parse tree
	 */
	void enterArrayElements(BashParser.ArrayElementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#arrayElements}.
	 * @param ctx the parse tree
	 */
	void exitArrayElements(BashParser.ArrayElementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#redirectionList}.
	 * @param ctx the parse tree
	 */
	void enterRedirectionList(BashParser.RedirectionListContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#redirectionList}.
	 * @param ctx the parse tree
	 */
	void exitRedirectionList(BashParser.RedirectionListContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#redirection}.
	 * @param ctx the parse tree
	 */
	void enterRedirection(BashParser.RedirectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#redirection}.
	 * @param ctx the parse tree
	 */
	void exitRedirection(BashParser.RedirectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#redirectionOp}.
	 * @param ctx the parse tree
	 */
	void enterRedirectionOp(BashParser.RedirectionOpContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#redirectionOp}.
	 * @param ctx the parse tree
	 */
	void exitRedirectionOp(BashParser.RedirectionOpContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#heredoc}.
	 * @param ctx the parse tree
	 */
	void enterHeredoc(BashParser.HeredocContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#heredoc}.
	 * @param ctx the parse tree
	 */
	void exitHeredoc(BashParser.HeredocContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#heredocDelimiter}.
	 * @param ctx the parse tree
	 */
	void enterHeredocDelimiter(BashParser.HeredocDelimiterContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#heredocDelimiter}.
	 * @param ctx the parse tree
	 */
	void exitHeredocDelimiter(BashParser.HeredocDelimiterContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#heredocBody}.
	 * @param ctx the parse tree
	 */
	void enterHeredocBody(BashParser.HeredocBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#heredocBody}.
	 * @param ctx the parse tree
	 */
	void exitHeredocBody(BashParser.HeredocBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#heredocLine}.
	 * @param ctx the parse tree
	 */
	void enterHeredocLine(BashParser.HeredocLineContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#heredocLine}.
	 * @param ctx the parse tree
	 */
	void exitHeredocLine(BashParser.HeredocLineContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#word}.
	 * @param ctx the parse tree
	 */
	void enterWord(BashParser.WordContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#word}.
	 * @param ctx the parse tree
	 */
	void exitWord(BashParser.WordContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#wordPart}.
	 * @param ctx the parse tree
	 */
	void enterWordPart(BashParser.WordPartContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#wordPart}.
	 * @param ctx the parse tree
	 */
	void exitWordPart(BashParser.WordPartContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#doubleQuotedString}.
	 * @param ctx the parse tree
	 */
	void enterDoubleQuotedString(BashParser.DoubleQuotedStringContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#doubleQuotedString}.
	 * @param ctx the parse tree
	 */
	void exitDoubleQuotedString(BashParser.DoubleQuotedStringContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#doubleQuotedPart}.
	 * @param ctx the parse tree
	 */
	void enterDoubleQuotedPart(BashParser.DoubleQuotedPartContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#doubleQuotedPart}.
	 * @param ctx the parse tree
	 */
	void exitDoubleQuotedPart(BashParser.DoubleQuotedPartContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#commandSubstitution}.
	 * @param ctx the parse tree
	 */
	void enterCommandSubstitution(BashParser.CommandSubstitutionContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#commandSubstitution}.
	 * @param ctx the parse tree
	 */
	void exitCommandSubstitution(BashParser.CommandSubstitutionContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#commandSubstitutionContent}.
	 * @param ctx the parse tree
	 */
	void enterCommandSubstitutionContent(BashParser.CommandSubstitutionContentContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#commandSubstitutionContent}.
	 * @param ctx the parse tree
	 */
	void exitCommandSubstitutionContent(BashParser.CommandSubstitutionContentContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#backtickContent}.
	 * @param ctx the parse tree
	 */
	void enterBacktickContent(BashParser.BacktickContentContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#backtickContent}.
	 * @param ctx the parse tree
	 */
	void exitBacktickContent(BashParser.BacktickContentContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#arithmeticSubstitution}.
	 * @param ctx the parse tree
	 */
	void enterArithmeticSubstitution(BashParser.ArithmeticSubstitutionContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#arithmeticSubstitution}.
	 * @param ctx the parse tree
	 */
	void exitArithmeticSubstitution(BashParser.ArithmeticSubstitutionContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#processSubstitution}.
	 * @param ctx the parse tree
	 */
	void enterProcessSubstitution(BashParser.ProcessSubstitutionContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#processSubstitution}.
	 * @param ctx the parse tree
	 */
	void exitProcessSubstitution(BashParser.ProcessSubstitutionContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#braceExpansionContent}.
	 * @param ctx the parse tree
	 */
	void enterBraceExpansionContent(BashParser.BraceExpansionContentContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#braceExpansionContent}.
	 * @param ctx the parse tree
	 */
	void exitBraceExpansionContent(BashParser.BraceExpansionContentContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#braceExpansionPart}.
	 * @param ctx the parse tree
	 */
	void enterBraceExpansionPart(BashParser.BraceExpansionPartContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#braceExpansionPart}.
	 * @param ctx the parse tree
	 */
	void exitBraceExpansionPart(BashParser.BraceExpansionPartContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#separator}.
	 * @param ctx the parse tree
	 */
	void enterSeparator(BashParser.SeparatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#separator}.
	 * @param ctx the parse tree
	 */
	void exitSeparator(BashParser.SeparatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#sequentialSep}.
	 * @param ctx the parse tree
	 */
	void enterSequentialSep(BashParser.SequentialSepContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#sequentialSep}.
	 * @param ctx the parse tree
	 */
	void exitSequentialSep(BashParser.SequentialSepContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#newlineList}.
	 * @param ctx the parse tree
	 */
	void enterNewlineList(BashParser.NewlineListContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#newlineList}.
	 * @param ctx the parse tree
	 */
	void exitNewlineList(BashParser.NewlineListContext ctx);
	/**
	 * Enter a parse tree produced by {@link BashParser#linebreak}.
	 * @param ctx the parse tree
	 */
	void enterLinebreak(BashParser.LinebreakContext ctx);
	/**
	 * Exit a parse tree produced by {@link BashParser#linebreak}.
	 * @param ctx the parse tree
	 */
	void exitLinebreak(BashParser.LinebreakContext ctx);
}