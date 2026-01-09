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
// Generated from /home/tim/Documents/workspace/openrewrite/rewrite/rewrite-docker/src/main/antlr/DockerfileParser.g4 by ANTLR 4.13.2
package org.openrewrite.docker.internal.grammar;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link DockerfileParser}.
 */
public interface DockerfileParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#dockerfile}.
	 * @param ctx the parse tree
	 */
	void enterDockerfile(DockerfileParser.DockerfileContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#dockerfile}.
	 * @param ctx the parse tree
	 */
	void exitDockerfile(DockerfileParser.DockerfileContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#parserDirective}.
	 * @param ctx the parse tree
	 */
	void enterParserDirective(DockerfileParser.ParserDirectiveContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#parserDirective}.
	 * @param ctx the parse tree
	 */
	void exitParserDirective(DockerfileParser.ParserDirectiveContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#globalArgs}.
	 * @param ctx the parse tree
	 */
	void enterGlobalArgs(DockerfileParser.GlobalArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#globalArgs}.
	 * @param ctx the parse tree
	 */
	void exitGlobalArgs(DockerfileParser.GlobalArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#stage}.
	 * @param ctx the parse tree
	 */
	void enterStage(DockerfileParser.StageContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#stage}.
	 * @param ctx the parse tree
	 */
	void exitStage(DockerfileParser.StageContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#stageInstruction}.
	 * @param ctx the parse tree
	 */
	void enterStageInstruction(DockerfileParser.StageInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#stageInstruction}.
	 * @param ctx the parse tree
	 */
	void exitStageInstruction(DockerfileParser.StageInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#instruction}.
	 * @param ctx the parse tree
	 */
	void enterInstruction(DockerfileParser.InstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#instruction}.
	 * @param ctx the parse tree
	 */
	void exitInstruction(DockerfileParser.InstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#fromInstruction}.
	 * @param ctx the parse tree
	 */
	void enterFromInstruction(DockerfileParser.FromInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#fromInstruction}.
	 * @param ctx the parse tree
	 */
	void exitFromInstruction(DockerfileParser.FromInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#runInstruction}.
	 * @param ctx the parse tree
	 */
	void enterRunInstruction(DockerfileParser.RunInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#runInstruction}.
	 * @param ctx the parse tree
	 */
	void exitRunInstruction(DockerfileParser.RunInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#cmdInstruction}.
	 * @param ctx the parse tree
	 */
	void enterCmdInstruction(DockerfileParser.CmdInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#cmdInstruction}.
	 * @param ctx the parse tree
	 */
	void exitCmdInstruction(DockerfileParser.CmdInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#labelInstruction}.
	 * @param ctx the parse tree
	 */
	void enterLabelInstruction(DockerfileParser.LabelInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#labelInstruction}.
	 * @param ctx the parse tree
	 */
	void exitLabelInstruction(DockerfileParser.LabelInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#exposeInstruction}.
	 * @param ctx the parse tree
	 */
	void enterExposeInstruction(DockerfileParser.ExposeInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#exposeInstruction}.
	 * @param ctx the parse tree
	 */
	void exitExposeInstruction(DockerfileParser.ExposeInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#envInstruction}.
	 * @param ctx the parse tree
	 */
	void enterEnvInstruction(DockerfileParser.EnvInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#envInstruction}.
	 * @param ctx the parse tree
	 */
	void exitEnvInstruction(DockerfileParser.EnvInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#addInstruction}.
	 * @param ctx the parse tree
	 */
	void enterAddInstruction(DockerfileParser.AddInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#addInstruction}.
	 * @param ctx the parse tree
	 */
	void exitAddInstruction(DockerfileParser.AddInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#copyInstruction}.
	 * @param ctx the parse tree
	 */
	void enterCopyInstruction(DockerfileParser.CopyInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#copyInstruction}.
	 * @param ctx the parse tree
	 */
	void exitCopyInstruction(DockerfileParser.CopyInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#entrypointInstruction}.
	 * @param ctx the parse tree
	 */
	void enterEntrypointInstruction(DockerfileParser.EntrypointInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#entrypointInstruction}.
	 * @param ctx the parse tree
	 */
	void exitEntrypointInstruction(DockerfileParser.EntrypointInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#volumeInstruction}.
	 * @param ctx the parse tree
	 */
	void enterVolumeInstruction(DockerfileParser.VolumeInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#volumeInstruction}.
	 * @param ctx the parse tree
	 */
	void exitVolumeInstruction(DockerfileParser.VolumeInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#userInstruction}.
	 * @param ctx the parse tree
	 */
	void enterUserInstruction(DockerfileParser.UserInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#userInstruction}.
	 * @param ctx the parse tree
	 */
	void exitUserInstruction(DockerfileParser.UserInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#workdirInstruction}.
	 * @param ctx the parse tree
	 */
	void enterWorkdirInstruction(DockerfileParser.WorkdirInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#workdirInstruction}.
	 * @param ctx the parse tree
	 */
	void exitWorkdirInstruction(DockerfileParser.WorkdirInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#argInstruction}.
	 * @param ctx the parse tree
	 */
	void enterArgInstruction(DockerfileParser.ArgInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#argInstruction}.
	 * @param ctx the parse tree
	 */
	void exitArgInstruction(DockerfileParser.ArgInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#onbuildInstruction}.
	 * @param ctx the parse tree
	 */
	void enterOnbuildInstruction(DockerfileParser.OnbuildInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#onbuildInstruction}.
	 * @param ctx the parse tree
	 */
	void exitOnbuildInstruction(DockerfileParser.OnbuildInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#stopsignalInstruction}.
	 * @param ctx the parse tree
	 */
	void enterStopsignalInstruction(DockerfileParser.StopsignalInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#stopsignalInstruction}.
	 * @param ctx the parse tree
	 */
	void exitStopsignalInstruction(DockerfileParser.StopsignalInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#healthcheckInstruction}.
	 * @param ctx the parse tree
	 */
	void enterHealthcheckInstruction(DockerfileParser.HealthcheckInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#healthcheckInstruction}.
	 * @param ctx the parse tree
	 */
	void exitHealthcheckInstruction(DockerfileParser.HealthcheckInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#shellInstruction}.
	 * @param ctx the parse tree
	 */
	void enterShellInstruction(DockerfileParser.ShellInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#shellInstruction}.
	 * @param ctx the parse tree
	 */
	void exitShellInstruction(DockerfileParser.ShellInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#maintainerInstruction}.
	 * @param ctx the parse tree
	 */
	void enterMaintainerInstruction(DockerfileParser.MaintainerInstructionContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#maintainerInstruction}.
	 * @param ctx the parse tree
	 */
	void exitMaintainerInstruction(DockerfileParser.MaintainerInstructionContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#flags}.
	 * @param ctx the parse tree
	 */
	void enterFlags(DockerfileParser.FlagsContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#flags}.
	 * @param ctx the parse tree
	 */
	void exitFlags(DockerfileParser.FlagsContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#flag}.
	 * @param ctx the parse tree
	 */
	void enterFlag(DockerfileParser.FlagContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#flag}.
	 * @param ctx the parse tree
	 */
	void exitFlag(DockerfileParser.FlagContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#flagName}.
	 * @param ctx the parse tree
	 */
	void enterFlagName(DockerfileParser.FlagNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#flagName}.
	 * @param ctx the parse tree
	 */
	void exitFlagName(DockerfileParser.FlagNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#flagValue}.
	 * @param ctx the parse tree
	 */
	void enterFlagValue(DockerfileParser.FlagValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#flagValue}.
	 * @param ctx the parse tree
	 */
	void exitFlagValue(DockerfileParser.FlagValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#flagValueElement}.
	 * @param ctx the parse tree
	 */
	void enterFlagValueElement(DockerfileParser.FlagValueElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#flagValueElement}.
	 * @param ctx the parse tree
	 */
	void exitFlagValueElement(DockerfileParser.FlagValueElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#execForm}.
	 * @param ctx the parse tree
	 */
	void enterExecForm(DockerfileParser.ExecFormContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#execForm}.
	 * @param ctx the parse tree
	 */
	void exitExecForm(DockerfileParser.ExecFormContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#shellForm}.
	 * @param ctx the parse tree
	 */
	void enterShellForm(DockerfileParser.ShellFormContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#shellForm}.
	 * @param ctx the parse tree
	 */
	void exitShellForm(DockerfileParser.ShellFormContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#heredoc}.
	 * @param ctx the parse tree
	 */
	void enterHeredoc(DockerfileParser.HeredocContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#heredoc}.
	 * @param ctx the parse tree
	 */
	void exitHeredoc(DockerfileParser.HeredocContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#heredocContent}.
	 * @param ctx the parse tree
	 */
	void enterHeredocContent(DockerfileParser.HeredocContentContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#heredocContent}.
	 * @param ctx the parse tree
	 */
	void exitHeredocContent(DockerfileParser.HeredocContentContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#heredocEnd}.
	 * @param ctx the parse tree
	 */
	void enterHeredocEnd(DockerfileParser.HeredocEndContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#heredocEnd}.
	 * @param ctx the parse tree
	 */
	void exitHeredocEnd(DockerfileParser.HeredocEndContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#jsonArray}.
	 * @param ctx the parse tree
	 */
	void enterJsonArray(DockerfileParser.JsonArrayContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#jsonArray}.
	 * @param ctx the parse tree
	 */
	void exitJsonArray(DockerfileParser.JsonArrayContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#jsonArrayElements}.
	 * @param ctx the parse tree
	 */
	void enterJsonArrayElements(DockerfileParser.JsonArrayElementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#jsonArrayElements}.
	 * @param ctx the parse tree
	 */
	void exitJsonArrayElements(DockerfileParser.JsonArrayElementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#jsonString}.
	 * @param ctx the parse tree
	 */
	void enterJsonString(DockerfileParser.JsonStringContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#jsonString}.
	 * @param ctx the parse tree
	 */
	void exitJsonString(DockerfileParser.JsonStringContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#imageName}.
	 * @param ctx the parse tree
	 */
	void enterImageName(DockerfileParser.ImageNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#imageName}.
	 * @param ctx the parse tree
	 */
	void exitImageName(DockerfileParser.ImageNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#stageName}.
	 * @param ctx the parse tree
	 */
	void enterStageName(DockerfileParser.StageNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#stageName}.
	 * @param ctx the parse tree
	 */
	void exitStageName(DockerfileParser.StageNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#labelPairs}.
	 * @param ctx the parse tree
	 */
	void enterLabelPairs(DockerfileParser.LabelPairsContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#labelPairs}.
	 * @param ctx the parse tree
	 */
	void exitLabelPairs(DockerfileParser.LabelPairsContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#labelPair}.
	 * @param ctx the parse tree
	 */
	void enterLabelPair(DockerfileParser.LabelPairContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#labelPair}.
	 * @param ctx the parse tree
	 */
	void exitLabelPair(DockerfileParser.LabelPairContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#labelKey}.
	 * @param ctx the parse tree
	 */
	void enterLabelKey(DockerfileParser.LabelKeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#labelKey}.
	 * @param ctx the parse tree
	 */
	void exitLabelKey(DockerfileParser.LabelKeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#labelValue}.
	 * @param ctx the parse tree
	 */
	void enterLabelValue(DockerfileParser.LabelValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#labelValue}.
	 * @param ctx the parse tree
	 */
	void exitLabelValue(DockerfileParser.LabelValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#portList}.
	 * @param ctx the parse tree
	 */
	void enterPortList(DockerfileParser.PortListContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#portList}.
	 * @param ctx the parse tree
	 */
	void exitPortList(DockerfileParser.PortListContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#port}.
	 * @param ctx the parse tree
	 */
	void enterPort(DockerfileParser.PortContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#port}.
	 * @param ctx the parse tree
	 */
	void exitPort(DockerfileParser.PortContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#envPairs}.
	 * @param ctx the parse tree
	 */
	void enterEnvPairs(DockerfileParser.EnvPairsContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#envPairs}.
	 * @param ctx the parse tree
	 */
	void exitEnvPairs(DockerfileParser.EnvPairsContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#envPair}.
	 * @param ctx the parse tree
	 */
	void enterEnvPair(DockerfileParser.EnvPairContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#envPair}.
	 * @param ctx the parse tree
	 */
	void exitEnvPair(DockerfileParser.EnvPairContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#envKey}.
	 * @param ctx the parse tree
	 */
	void enterEnvKey(DockerfileParser.EnvKeyContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#envKey}.
	 * @param ctx the parse tree
	 */
	void exitEnvKey(DockerfileParser.EnvKeyContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#envValueEquals}.
	 * @param ctx the parse tree
	 */
	void enterEnvValueEquals(DockerfileParser.EnvValueEqualsContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#envValueEquals}.
	 * @param ctx the parse tree
	 */
	void exitEnvValueEquals(DockerfileParser.EnvValueEqualsContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#envValueSpace}.
	 * @param ctx the parse tree
	 */
	void enterEnvValueSpace(DockerfileParser.EnvValueSpaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#envValueSpace}.
	 * @param ctx the parse tree
	 */
	void exitEnvValueSpace(DockerfileParser.EnvValueSpaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#envTextEquals}.
	 * @param ctx the parse tree
	 */
	void enterEnvTextEquals(DockerfileParser.EnvTextEqualsContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#envTextEquals}.
	 * @param ctx the parse tree
	 */
	void exitEnvTextEquals(DockerfileParser.EnvTextEqualsContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#envTextElementEquals}.
	 * @param ctx the parse tree
	 */
	void enterEnvTextElementEquals(DockerfileParser.EnvTextElementEqualsContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#envTextElementEquals}.
	 * @param ctx the parse tree
	 */
	void exitEnvTextElementEquals(DockerfileParser.EnvTextElementEqualsContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#sourceList}.
	 * @param ctx the parse tree
	 */
	void enterSourceList(DockerfileParser.SourceListContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#sourceList}.
	 * @param ctx the parse tree
	 */
	void exitSourceList(DockerfileParser.SourceListContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#source}.
	 * @param ctx the parse tree
	 */
	void enterSource(DockerfileParser.SourceContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#source}.
	 * @param ctx the parse tree
	 */
	void exitSource(DockerfileParser.SourceContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#destination}.
	 * @param ctx the parse tree
	 */
	void enterDestination(DockerfileParser.DestinationContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#destination}.
	 * @param ctx the parse tree
	 */
	void exitDestination(DockerfileParser.DestinationContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#path}.
	 * @param ctx the parse tree
	 */
	void enterPath(DockerfileParser.PathContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#path}.
	 * @param ctx the parse tree
	 */
	void exitPath(DockerfileParser.PathContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#pathList}.
	 * @param ctx the parse tree
	 */
	void enterPathList(DockerfileParser.PathListContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#pathList}.
	 * @param ctx the parse tree
	 */
	void exitPathList(DockerfileParser.PathListContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#volumePath}.
	 * @param ctx the parse tree
	 */
	void enterVolumePath(DockerfileParser.VolumePathContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#volumePath}.
	 * @param ctx the parse tree
	 */
	void exitVolumePath(DockerfileParser.VolumePathContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#userSpec}.
	 * @param ctx the parse tree
	 */
	void enterUserSpec(DockerfileParser.UserSpecContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#userSpec}.
	 * @param ctx the parse tree
	 */
	void exitUserSpec(DockerfileParser.UserSpecContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#argName}.
	 * @param ctx the parse tree
	 */
	void enterArgName(DockerfileParser.ArgNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#argName}.
	 * @param ctx the parse tree
	 */
	void exitArgName(DockerfileParser.ArgNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#argValue}.
	 * @param ctx the parse tree
	 */
	void enterArgValue(DockerfileParser.ArgValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#argValue}.
	 * @param ctx the parse tree
	 */
	void exitArgValue(DockerfileParser.ArgValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#signal}.
	 * @param ctx the parse tree
	 */
	void enterSignal(DockerfileParser.SignalContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#signal}.
	 * @param ctx the parse tree
	 */
	void exitSignal(DockerfileParser.SignalContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#text}.
	 * @param ctx the parse tree
	 */
	void enterText(DockerfileParser.TextContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#text}.
	 * @param ctx the parse tree
	 */
	void exitText(DockerfileParser.TextContext ctx);
	/**
	 * Enter a parse tree produced by {@link DockerfileParser#textElement}.
	 * @param ctx the parse tree
	 */
	void enterTextElement(DockerfileParser.TextElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link DockerfileParser#textElement}.
	 * @param ctx the parse tree
	 */
	void exitTextElement(DockerfileParser.TextElementContext ctx);
}