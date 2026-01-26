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
// Generated from /home/tim/Documents/workspace/openrewrite/rewrite/rewrite-docker/src/main/antlr/DockerParser.g4 by ANTLR 4.13.2
package org.openrewrite.docker.internal.grammar;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link DockerParser}.
 */
public interface DockerParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link DockerParser#dockerfile}.
	 * @param ctx the parse tree
	 */
	void enterDockerfile(DockerParser.DockerfileContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#dockerfile}.
	 * @param ctx the parse tree
	 */
	void exitDockerfile(DockerParser.DockerfileContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#parserDirective}.
	 * @param ctx the parse tree
	 */
	void enterParserDirective(DockerParser.ParserDirectiveContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#parserDirective}.
	 * @param ctx the parse tree
	 */
	void exitParserDirective(DockerParser.ParserDirectiveContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#globalArgs}.
	 * @param ctx the parse tree
	 */
	void enterGlobalArgs(DockerParser.GlobalArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#globalArgs}.
	 * @param ctx the parse tree
	 */
	void exitGlobalArgs(DockerParser.GlobalArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#stage}.
	 * @param ctx the parse tree
	 */
	void enterStage(DockerParser.StageContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#stage}.
	 * @param ctx the parse tree
	 */
	void exitStage(DockerParser.StageContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#stageInstruction}.
	 * @param ctx the parse tree
	 */
	void enterStageInstruction(DockerParser.StageInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#stageInstruction}.
	 * @param ctx the parse tree
	 */
	void exitStageInstruction(DockerParser.StageInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#instruction}.
	 * @param ctx the parse tree
	 */
	void enterInstruction(DockerParser.InstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#instruction}.
	 * @param ctx the parse tree
	 */
	void exitInstruction(DockerParser.InstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#fromInstruction}.
	 * @param ctx the parse tree
	 */
	void enterFromInstruction(DockerParser.FromInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#fromInstruction}.
	 * @param ctx the parse tree
	 */
	void exitFromInstruction(DockerParser.FromInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#runInstruction}.
	 * @param ctx the parse tree
	 */
	void enterRunInstruction(DockerParser.RunInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#runInstruction}.
	 * @param ctx the parse tree
	 */
	void exitRunInstruction(DockerParser.RunInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#cmdInstruction}.
	 * @param ctx the parse tree
	 */
	void enterCmdInstruction(DockerParser.CmdInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#cmdInstruction}.
	 * @param ctx the parse tree
	 */
	void exitCmdInstruction(DockerParser.CmdInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#labelInstruction}.
	 * @param ctx the parse tree
	 */
	void enterLabelInstruction(DockerParser.LabelInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#labelInstruction}.
	 * @param ctx the parse tree
	 */
	void exitLabelInstruction(DockerParser.LabelInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#exposeInstruction}.
	 * @param ctx the parse tree
	 */
	void enterExposeInstruction(DockerParser.ExposeInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#exposeInstruction}.
	 * @param ctx the parse tree
	 */
	void exitExposeInstruction(DockerParser.ExposeInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#envInstruction}.
	 * @param ctx the parse tree
	 */
	void enterEnvInstruction(DockerParser.EnvInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#envInstruction}.
	 * @param ctx the parse tree
	 */
	void exitEnvInstruction(DockerParser.EnvInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#addInstruction}.
	 * @param ctx the parse tree
	 */
	void enterAddInstruction(DockerParser.AddInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#addInstruction}.
	 * @param ctx the parse tree
	 */
	void exitAddInstruction(DockerParser.AddInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#copyInstruction}.
	 * @param ctx the parse tree
	 */
	void enterCopyInstruction(DockerParser.CopyInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#copyInstruction}.
	 * @param ctx the parse tree
	 */
	void exitCopyInstruction(DockerParser.CopyInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#entrypointInstruction}.
	 * @param ctx the parse tree
	 */
	void enterEntrypointInstruction(DockerParser.EntrypointInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#entrypointInstruction}.
	 * @param ctx the parse tree
	 */
	void exitEntrypointInstruction(DockerParser.EntrypointInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#volumeInstruction}.
	 * @param ctx the parse tree
	 */
	void enterVolumeInstruction(DockerParser.VolumeInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#volumeInstruction}.
	 * @param ctx the parse tree
	 */
	void exitVolumeInstruction(DockerParser.VolumeInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#userInstruction}.
	 * @param ctx the parse tree
	 */
	void enterUserInstruction(DockerParser.UserInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#userInstruction}.
	 * @param ctx the parse tree
	 */
	void exitUserInstruction(DockerParser.UserInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#workdirInstruction}.
	 * @param ctx the parse tree
	 */
	void enterWorkdirInstruction(DockerParser.WorkdirInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#workdirInstruction}.
	 * @param ctx the parse tree
	 */
	void exitWorkdirInstruction(DockerParser.WorkdirInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#argInstruction}.
	 * @param ctx the parse tree
	 */
	void enterArgInstruction(DockerParser.ArgInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#argInstruction}.
	 * @param ctx the parse tree
	 */
	void exitArgInstruction(DockerParser.ArgInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#onbuildInstruction}.
	 * @param ctx the parse tree
	 */
	void enterOnbuildInstruction(DockerParser.OnbuildInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#onbuildInstruction}.
	 * @param ctx the parse tree
	 */
	void exitOnbuildInstruction(DockerParser.OnbuildInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#stopsignalInstruction}.
	 * @param ctx the parse tree
	 */
	void enterStopsignalInstruction(DockerParser.StopsignalInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#stopsignalInstruction}.
	 * @param ctx the parse tree
	 */
	void exitStopsignalInstruction(DockerParser.StopsignalInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#healthcheckInstruction}.
	 * @param ctx the parse tree
	 */
	void enterHealthcheckInstruction(DockerParser.HealthcheckInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#healthcheckInstruction}.
	 * @param ctx the parse tree
	 */
	void exitHealthcheckInstruction(DockerParser.HealthcheckInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#healthcheckOptions}.
	 * @param ctx the parse tree
	 */
	void enterHealthcheckOptions(DockerParser.HealthcheckOptionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#healthcheckOptions}.
	 * @param ctx the parse tree
	 */
	void exitHealthcheckOptions(DockerParser.HealthcheckOptionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#healthcheckOption}.
	 * @param ctx the parse tree
	 */
	void enterHealthcheckOption(DockerParser.HealthcheckOptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#healthcheckOption}.
	 * @param ctx the parse tree
	 */
	void exitHealthcheckOption(DockerParser.HealthcheckOptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#shellInstruction}.
	 * @param ctx the parse tree
	 */
	void enterShellInstruction(DockerParser.ShellInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#shellInstruction}.
	 * @param ctx the parse tree
	 */
	void exitShellInstruction(DockerParser.ShellInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#maintainerInstruction}.
	 * @param ctx the parse tree
	 */
	void enterMaintainerInstruction(DockerParser.MaintainerInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#maintainerInstruction}.
	 * @param ctx the parse tree
	 */
	void exitMaintainerInstruction(DockerParser.MaintainerInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#flags}.
	 * @param ctx the parse tree
	 */
	void enterFlags(DockerParser.FlagsContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#flags}.
	 * @param ctx the parse tree
	 */
	void exitFlags(DockerParser.FlagsContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#flag}.
	 * @param ctx the parse tree
	 */
	void enterFlag(DockerParser.FlagContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#flag}.
	 * @param ctx the parse tree
	 */
	void exitFlag(DockerParser.FlagContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#execForm}.
	 * @param ctx the parse tree
	 */
	void enterExecForm(DockerParser.ExecFormContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#execForm}.
	 * @param ctx the parse tree
	 */
	void exitExecForm(DockerParser.ExecFormContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#shellForm}.
	 * @param ctx the parse tree
	 */
	void enterShellForm(DockerParser.ShellFormContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#shellForm}.
	 * @param ctx the parse tree
	 */
	void exitShellForm(DockerParser.ShellFormContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#shellFormText}.
	 * @param ctx the parse tree
	 */
	void enterShellFormText(DockerParser.ShellFormTextContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#shellFormText}.
	 * @param ctx the parse tree
	 */
	void exitShellFormText(DockerParser.ShellFormTextContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#shellFormTextElement}.
	 * @param ctx the parse tree
	 */
	void enterShellFormTextElement(DockerParser.ShellFormTextElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#shellFormTextElement}.
	 * @param ctx the parse tree
	 */
	void exitShellFormTextElement(DockerParser.ShellFormTextElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#heredoc}.
	 * @param ctx the parse tree
	 */
	void enterHeredoc(DockerParser.HeredocContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#heredoc}.
	 * @param ctx the parse tree
	 */
	void exitHeredoc(DockerParser.HeredocContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#heredocPreamble}.
	 * @param ctx the parse tree
	 */
	void enterHeredocPreamble(DockerParser.HeredocPreambleContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#heredocPreamble}.
	 * @param ctx the parse tree
	 */
	void exitHeredocPreamble(DockerParser.HeredocPreambleContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#preambleElement}.
	 * @param ctx the parse tree
	 */
	void enterPreambleElement(DockerParser.PreambleElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#preambleElement}.
	 * @param ctx the parse tree
	 */
	void exitPreambleElement(DockerParser.PreambleElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#heredocBody}.
	 * @param ctx the parse tree
	 */
	void enterHeredocBody(DockerParser.HeredocBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#heredocBody}.
	 * @param ctx the parse tree
	 */
	void exitHeredocBody(DockerParser.HeredocBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#heredocContent}.
	 * @param ctx the parse tree
	 */
	void enterHeredocContent(DockerParser.HeredocContentContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#heredocContent}.
	 * @param ctx the parse tree
	 */
	void exitHeredocContent(DockerParser.HeredocContentContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#heredocEnd}.
	 * @param ctx the parse tree
	 */
	void enterHeredocEnd(DockerParser.HeredocEndContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#heredocEnd}.
	 * @param ctx the parse tree
	 */
	void exitHeredocEnd(DockerParser.HeredocEndContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#jsonArray}.
	 * @param ctx the parse tree
	 */
	void enterJsonArray(DockerParser.JsonArrayContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#jsonArray}.
	 * @param ctx the parse tree
	 */
	void exitJsonArray(DockerParser.JsonArrayContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#jsonArrayElements}.
	 * @param ctx the parse tree
	 */
	void enterJsonArrayElements(DockerParser.JsonArrayElementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#jsonArrayElements}.
	 * @param ctx the parse tree
	 */
	void exitJsonArrayElements(DockerParser.JsonArrayElementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#jsonString}.
	 * @param ctx the parse tree
	 */
	void enterJsonString(DockerParser.JsonStringContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#jsonString}.
	 * @param ctx the parse tree
	 */
	void exitJsonString(DockerParser.JsonStringContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#imageName}.
	 * @param ctx the parse tree
	 */
	void enterImageName(DockerParser.ImageNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#imageName}.
	 * @param ctx the parse tree
	 */
	void exitImageName(DockerParser.ImageNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#stageName}.
	 * @param ctx the parse tree
	 */
	void enterStageName(DockerParser.StageNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#stageName}.
	 * @param ctx the parse tree
	 */
	void exitStageName(DockerParser.StageNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#labelPairs}.
	 * @param ctx the parse tree
	 */
	void enterLabelPairs(DockerParser.LabelPairsContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#labelPairs}.
	 * @param ctx the parse tree
	 */
	void exitLabelPairs(DockerParser.LabelPairsContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#labelPair}.
	 * @param ctx the parse tree
	 */
	void enterLabelPair(DockerParser.LabelPairContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#labelPair}.
	 * @param ctx the parse tree
	 */
	void exitLabelPair(DockerParser.LabelPairContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#labelKey}.
	 * @param ctx the parse tree
	 */
	void enterLabelKey(DockerParser.LabelKeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#labelKey}.
	 * @param ctx the parse tree
	 */
	void exitLabelKey(DockerParser.LabelKeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#labelValue}.
	 * @param ctx the parse tree
	 */
	void enterLabelValue(DockerParser.LabelValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#labelValue}.
	 * @param ctx the parse tree
	 */
	void exitLabelValue(DockerParser.LabelValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#labelOldValue}.
	 * @param ctx the parse tree
	 */
	void enterLabelOldValue(DockerParser.LabelOldValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#labelOldValue}.
	 * @param ctx the parse tree
	 */
	void exitLabelOldValue(DockerParser.LabelOldValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#labelOldValueElement}.
	 * @param ctx the parse tree
	 */
	void enterLabelOldValueElement(DockerParser.LabelOldValueElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#labelOldValueElement}.
	 * @param ctx the parse tree
	 */
	void exitLabelOldValueElement(DockerParser.LabelOldValueElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#portList}.
	 * @param ctx the parse tree
	 */
	void enterPortList(DockerParser.PortListContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#portList}.
	 * @param ctx the parse tree
	 */
	void exitPortList(DockerParser.PortListContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#port}.
	 * @param ctx the parse tree
	 */
	void enterPort(DockerParser.PortContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#port}.
	 * @param ctx the parse tree
	 */
	void exitPort(DockerParser.PortContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#envPairs}.
	 * @param ctx the parse tree
	 */
	void enterEnvPairs(DockerParser.EnvPairsContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#envPairs}.
	 * @param ctx the parse tree
	 */
	void exitEnvPairs(DockerParser.EnvPairsContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#envPair}.
	 * @param ctx the parse tree
	 */
	void enterEnvPair(DockerParser.EnvPairContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#envPair}.
	 * @param ctx the parse tree
	 */
	void exitEnvPair(DockerParser.EnvPairContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#envKey}.
	 * @param ctx the parse tree
	 */
	void enterEnvKey(DockerParser.EnvKeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#envKey}.
	 * @param ctx the parse tree
	 */
	void exitEnvKey(DockerParser.EnvKeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#envValueEquals}.
	 * @param ctx the parse tree
	 */
	void enterEnvValueEquals(DockerParser.EnvValueEqualsContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#envValueEquals}.
	 * @param ctx the parse tree
	 */
	void exitEnvValueEquals(DockerParser.EnvValueEqualsContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#envValueSpace}.
	 * @param ctx the parse tree
	 */
	void enterEnvValueSpace(DockerParser.EnvValueSpaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#envValueSpace}.
	 * @param ctx the parse tree
	 */
	void exitEnvValueSpace(DockerParser.EnvValueSpaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#envTextEquals}.
	 * @param ctx the parse tree
	 */
	void enterEnvTextEquals(DockerParser.EnvTextEqualsContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#envTextEquals}.
	 * @param ctx the parse tree
	 */
	void exitEnvTextEquals(DockerParser.EnvTextEqualsContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#envTextElementEquals}.
	 * @param ctx the parse tree
	 */
	void enterEnvTextElementEquals(DockerParser.EnvTextElementEqualsContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#envTextElementEquals}.
	 * @param ctx the parse tree
	 */
	void exitEnvTextElementEquals(DockerParser.EnvTextElementEqualsContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#sourceList}.
	 * @param ctx the parse tree
	 */
	void enterSourceList(DockerParser.SourceListContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#sourceList}.
	 * @param ctx the parse tree
	 */
	void exitSourceList(DockerParser.SourceListContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#sourcePath}.
	 * @param ctx the parse tree
	 */
	void enterSourcePath(DockerParser.SourcePathContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#sourcePath}.
	 * @param ctx the parse tree
	 */
	void exitSourcePath(DockerParser.SourcePathContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#destination}.
	 * @param ctx the parse tree
	 */
	void enterDestination(DockerParser.DestinationContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#destination}.
	 * @param ctx the parse tree
	 */
	void exitDestination(DockerParser.DestinationContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#destinationPath}.
	 * @param ctx the parse tree
	 */
	void enterDestinationPath(DockerParser.DestinationPathContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#destinationPath}.
	 * @param ctx the parse tree
	 */
	void exitDestinationPath(DockerParser.DestinationPathContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#path}.
	 * @param ctx the parse tree
	 */
	void enterPath(DockerParser.PathContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#path}.
	 * @param ctx the parse tree
	 */
	void exitPath(DockerParser.PathContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#pathList}.
	 * @param ctx the parse tree
	 */
	void enterPathList(DockerParser.PathListContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#pathList}.
	 * @param ctx the parse tree
	 */
	void exitPathList(DockerParser.PathListContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#volumePath}.
	 * @param ctx the parse tree
	 */
	void enterVolumePath(DockerParser.VolumePathContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#volumePath}.
	 * @param ctx the parse tree
	 */
	void exitVolumePath(DockerParser.VolumePathContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#userSpec}.
	 * @param ctx the parse tree
	 */
	void enterUserSpec(DockerParser.UserSpecContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#userSpec}.
	 * @param ctx the parse tree
	 */
	void exitUserSpec(DockerParser.UserSpecContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#argName}.
	 * @param ctx the parse tree
	 */
	void enterArgName(DockerParser.ArgNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#argName}.
	 * @param ctx the parse tree
	 */
	void exitArgName(DockerParser.ArgNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#argValue}.
	 * @param ctx the parse tree
	 */
	void enterArgValue(DockerParser.ArgValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#argValue}.
	 * @param ctx the parse tree
	 */
	void exitArgValue(DockerParser.ArgValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#signal}.
	 * @param ctx the parse tree
	 */
	void enterSignal(DockerParser.SignalContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#signal}.
	 * @param ctx the parse tree
	 */
	void exitSignal(DockerParser.SignalContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#text}.
	 * @param ctx the parse tree
	 */
	void enterText(DockerParser.TextContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#text}.
	 * @param ctx the parse tree
	 */
	void exitText(DockerParser.TextContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerParser#textElement}.
	 * @param ctx the parse tree
	 */
	void enterTextElement(DockerParser.TextElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerParser#textElement}.
	 * @param ctx the parse tree
	 */
	void exitTextElement(DockerParser.TextElementContext ctx);
}