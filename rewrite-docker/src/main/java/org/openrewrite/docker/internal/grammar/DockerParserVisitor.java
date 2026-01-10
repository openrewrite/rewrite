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
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link DockerParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface DockerParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link DockerParser#dockerfile}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDockerfile(DockerParser.DockerfileContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#parserDirective}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParserDirective(DockerParser.ParserDirectiveContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#globalArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGlobalArgs(DockerParser.GlobalArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#stage}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStage(DockerParser.StageContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#stageInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStageInstruction(DockerParser.StageInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#instruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstruction(DockerParser.InstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#fromInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFromInstruction(DockerParser.FromInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#runInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRunInstruction(DockerParser.RunInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#cmdInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCmdInstruction(DockerParser.CmdInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#labelInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLabelInstruction(DockerParser.LabelInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#exposeInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExposeInstruction(DockerParser.ExposeInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#envInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnvInstruction(DockerParser.EnvInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#addInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAddInstruction(DockerParser.AddInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#copyInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCopyInstruction(DockerParser.CopyInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#entrypointInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEntrypointInstruction(DockerParser.EntrypointInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#volumeInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVolumeInstruction(DockerParser.VolumeInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#userInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUserInstruction(DockerParser.UserInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#workdirInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWorkdirInstruction(DockerParser.WorkdirInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#argInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgInstruction(DockerParser.ArgInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#onbuildInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOnbuildInstruction(DockerParser.OnbuildInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#stopsignalInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStopsignalInstruction(DockerParser.StopsignalInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#healthcheckInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHealthcheckInstruction(DockerParser.HealthcheckInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#shellInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShellInstruction(DockerParser.ShellInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#maintainerInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMaintainerInstruction(DockerParser.MaintainerInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#flags}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFlags(DockerParser.FlagsContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#flag}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFlag(DockerParser.FlagContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#flagName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFlagName(DockerParser.FlagNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#flagValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFlagValue(DockerParser.FlagValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#flagValueElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFlagValueElement(DockerParser.FlagValueElementContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#execForm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecForm(DockerParser.ExecFormContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#shellForm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShellForm(DockerParser.ShellFormContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#heredoc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHeredoc(DockerParser.HeredocContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#heredocContent}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHeredocContent(DockerParser.HeredocContentContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#heredocEnd}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHeredocEnd(DockerParser.HeredocEndContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#jsonArray}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJsonArray(DockerParser.JsonArrayContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#jsonArrayElements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJsonArrayElements(DockerParser.JsonArrayElementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#jsonString}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJsonString(DockerParser.JsonStringContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#imageName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImageName(DockerParser.ImageNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#stageName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStageName(DockerParser.StageNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#labelPairs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLabelPairs(DockerParser.LabelPairsContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#labelPair}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLabelPair(DockerParser.LabelPairContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#labelKey}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLabelKey(DockerParser.LabelKeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#labelValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLabelValue(DockerParser.LabelValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#labelOldValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLabelOldValue(DockerParser.LabelOldValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#labelOldValueElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLabelOldValueElement(DockerParser.LabelOldValueElementContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#portList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPortList(DockerParser.PortListContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#port}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPort(DockerParser.PortContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#envPairs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnvPairs(DockerParser.EnvPairsContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#envPair}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnvPair(DockerParser.EnvPairContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#envKey}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnvKey(DockerParser.EnvKeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#envValueEquals}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnvValueEquals(DockerParser.EnvValueEqualsContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#envValueSpace}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnvValueSpace(DockerParser.EnvValueSpaceContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#envTextEquals}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnvTextEquals(DockerParser.EnvTextEqualsContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#envTextElementEquals}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnvTextElementEquals(DockerParser.EnvTextElementEqualsContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#sourceList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSourceList(DockerParser.SourceListContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#source}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSource(DockerParser.SourceContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#destination}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDestination(DockerParser.DestinationContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#path}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPath(DockerParser.PathContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#pathList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPathList(DockerParser.PathListContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#volumePath}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVolumePath(DockerParser.VolumePathContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#userSpec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUserSpec(DockerParser.UserSpecContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#argName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgName(DockerParser.ArgNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#argValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgValue(DockerParser.ArgValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#signal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSignal(DockerParser.SignalContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#text}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitText(DockerParser.TextContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerParser#textElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTextElement(DockerParser.TextElementContext ctx);
}