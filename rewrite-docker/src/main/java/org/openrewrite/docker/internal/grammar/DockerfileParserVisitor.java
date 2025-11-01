/*
 * Copyright 2025 the original author or authors.
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
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link DockerfileParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface DockerfileParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#dockerfile}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDockerfile(DockerfileParser.DockerfileContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#parserDirective}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParserDirective(DockerfileParser.ParserDirectiveContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#globalArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGlobalArgs(DockerfileParser.GlobalArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#stage}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStage(DockerfileParser.StageContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#stageInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStageInstruction(DockerfileParser.StageInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#instruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstruction(DockerfileParser.InstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#fromInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFromInstruction(DockerfileParser.FromInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#runInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRunInstruction(DockerfileParser.RunInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#cmdInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCmdInstruction(DockerfileParser.CmdInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#labelInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLabelInstruction(DockerfileParser.LabelInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#exposeInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExposeInstruction(DockerfileParser.ExposeInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#envInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnvInstruction(DockerfileParser.EnvInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#addInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAddInstruction(DockerfileParser.AddInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#copyInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCopyInstruction(DockerfileParser.CopyInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#entrypointInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEntrypointInstruction(DockerfileParser.EntrypointInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#volumeInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVolumeInstruction(DockerfileParser.VolumeInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#userInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUserInstruction(DockerfileParser.UserInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#workdirInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWorkdirInstruction(DockerfileParser.WorkdirInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#argInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgInstruction(DockerfileParser.ArgInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#onbuildInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOnbuildInstruction(DockerfileParser.OnbuildInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#stopsignalInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStopsignalInstruction(DockerfileParser.StopsignalInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#healthcheckInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHealthcheckInstruction(DockerfileParser.HealthcheckInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#shellInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShellInstruction(DockerfileParser.ShellInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#maintainerInstruction}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMaintainerInstruction(DockerfileParser.MaintainerInstructionContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#flags}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFlags(DockerfileParser.FlagsContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#flag}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFlag(DockerfileParser.FlagContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#flagName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFlagName(DockerfileParser.FlagNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#flagValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFlagValue(DockerfileParser.FlagValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#flagValueElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFlagValueElement(DockerfileParser.FlagValueElementContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#execForm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecForm(DockerfileParser.ExecFormContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#shellForm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShellForm(DockerfileParser.ShellFormContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#heredoc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHeredoc(DockerfileParser.HeredocContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#heredocContent}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHeredocContent(DockerfileParser.HeredocContentContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#heredocEnd}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHeredocEnd(DockerfileParser.HeredocEndContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#jsonArray}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJsonArray(DockerfileParser.JsonArrayContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#jsonArrayElements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJsonArrayElements(DockerfileParser.JsonArrayElementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#jsonString}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJsonString(DockerfileParser.JsonStringContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#imageName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImageName(DockerfileParser.ImageNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#stageName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStageName(DockerfileParser.StageNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#labelPairs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLabelPairs(DockerfileParser.LabelPairsContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#labelPair}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLabelPair(DockerfileParser.LabelPairContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#labelKey}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLabelKey(DockerfileParser.LabelKeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#labelValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLabelValue(DockerfileParser.LabelValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#portList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPortList(DockerfileParser.PortListContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#port}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPort(DockerfileParser.PortContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#envPairs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnvPairs(DockerfileParser.EnvPairsContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#envPair}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnvPair(DockerfileParser.EnvPairContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#envKey}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnvKey(DockerfileParser.EnvKeyContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#envValueEquals}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnvValueEquals(DockerfileParser.EnvValueEqualsContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#envValueSpace}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnvValueSpace(DockerfileParser.EnvValueSpaceContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#envTextEquals}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnvTextEquals(DockerfileParser.EnvTextEqualsContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#envTextElementEquals}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnvTextElementEquals(DockerfileParser.EnvTextElementEqualsContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#sourceList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSourceList(DockerfileParser.SourceListContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#source}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSource(DockerfileParser.SourceContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#destination}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDestination(DockerfileParser.DestinationContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#path}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPath(DockerfileParser.PathContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#pathList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPathList(DockerfileParser.PathListContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#volumePath}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVolumePath(DockerfileParser.VolumePathContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#userSpec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUserSpec(DockerfileParser.UserSpecContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#argName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgName(DockerfileParser.ArgNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#argValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgValue(DockerfileParser.ArgValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#signal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSignal(DockerfileParser.SignalContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#text}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitText(DockerfileParser.TextContext ctx);
	/**
	 * Visit a parse tree produced by {@link DockerfileParser#textElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTextElement(DockerfileParser.TextElementContext ctx);
}