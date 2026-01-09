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
package org.openrewrite.docker;

import org.jspecify.annotations.Nullable;
import org.openrewrite.docker.tree.Docker;

public class DockerIsoVisitor<P> extends DockerVisitor<P> {

    @Override
    public Docker.File visitFile(Docker.File file, P p) {
        return (Docker.File) super.visitFile(file, p);
    }

    @Override
    public Docker.Stage visitStage(Docker.Stage stage, P p) {
        return (Docker.Stage) super.visitStage(stage, p);
    }

    @Override
    public Docker.From visitFrom(Docker.From from, P p) {
        return (Docker.From) super.visitFrom(from, p);
    }

    @Override
    public Docker.From.@Nullable As visitFromAs(Docker.From.As as, P p) {
        return super.visitFromAs(as, p);
    }

    @Override
    public Docker.Run visitRun(Docker.Run run, P p) {
        return (Docker.Run) super.visitRun(run, p);
    }

    @Override
    public Docker.Add visitAdd(Docker.Add add, P p) {
        return (Docker.Add) super.visitAdd(add, p);
    }

    @Override
    public Docker.Copy visitCopy(Docker.Copy copy, P p) {
        return (Docker.Copy) super.visitCopy(copy, p);
    }

    @Override
    public Docker.Arg visitArg(Docker.Arg arg, P p) {
        return (Docker.Arg) super.visitArg(arg, p);
    }

    @Override
    public Docker.Env visitEnv(Docker.Env env, P p) {
        return (Docker.Env) super.visitEnv(env, p);
    }

    @Override
    public Docker.Env.EnvPair visitEnvPair(Docker.Env.EnvPair pair, P p) {
        return super.visitEnvPair(pair, p);
    }

    @Override
    public Docker.Label visitLabel(Docker.Label label, P p) {
        return (Docker.Label) super.visitLabel(label, p);
    }

    @Override
    public Docker.Label.LabelPair visitLabelPair(Docker.Label.LabelPair pair, P p) {
        return super.visitLabelPair(pair, p);
    }

    @Override
    public Docker.Cmd visitCmd(Docker.Cmd cmd, P p) {
        return (Docker.Cmd) super.visitCmd(cmd, p);
    }

    @Override
    public Docker.Entrypoint visitEntrypoint(Docker.Entrypoint entrypoint, P p) {
        return (Docker.Entrypoint) super.visitEntrypoint(entrypoint, p);
    }

    @Override
    public Docker.Expose visitExpose(Docker.Expose expose, P p) {
        return (Docker.Expose) super.visitExpose(expose, p);
    }

    @Override
    public Docker.Volume visitVolume(Docker.Volume volume, P p) {
        return (Docker.Volume) super.visitVolume(volume, p);
    }

    @Override
    public Docker.Shell visitShell(Docker.Shell shell, P p) {
        return (Docker.Shell) super.visitShell(shell, p);
    }

    @Override
    public Docker.Workdir visitWorkdir(Docker.Workdir workdir, P p) {
        return (Docker.Workdir) super.visitWorkdir(workdir, p);
    }

    @Override
    public Docker.User visitUser(Docker.User user, P p) {
        return (Docker.User) super.visitUser(user, p);
    }

    @Override
    public Docker.Stopsignal visitStopsignal(Docker.Stopsignal stopsignal, P p) {
        return (Docker.Stopsignal) super.visitStopsignal(stopsignal, p);
    }

    @Override
    public Docker.Onbuild visitOnbuild(Docker.Onbuild onbuild, P p) {
        return (Docker.Onbuild) super.visitOnbuild(onbuild, p);
    }

    @Override
    public Docker.Healthcheck visitHealthcheck(Docker.Healthcheck healthcheck, P p) {
        return (Docker.Healthcheck) super.visitHealthcheck(healthcheck, p);
    }

    @Override
    public Docker.Maintainer visitMaintainer(Docker.Maintainer maintainer, P p) {
        return (Docker.Maintainer) super.visitMaintainer(maintainer, p);
    }

    @Override
    public Docker.CommandLine visitCommandLine(Docker.CommandLine commandLine, P p) {
        return (Docker.CommandLine) super.visitCommandLine(commandLine, p);
    }

    @Override
    public Docker.ShellForm visitShellForm(Docker.ShellForm shellForm, P p) {
        return (Docker.ShellForm) super.visitShellForm(shellForm, p);
    }

    @Override
    public Docker.ExecForm visitExecForm(Docker.ExecForm execForm, P p) {
        return (Docker.ExecForm) super.visitExecForm(execForm, p);
    }

    @Override
    public Docker.HeredocForm visitHeredocForm(Docker.HeredocForm heredocForm, P p) {
        return (Docker.HeredocForm) super.visitHeredocForm(heredocForm, p);
    }

    @Override
    public Docker.Flag visitFlag(Docker.Flag flag, P p) {
        return (Docker.Flag) super.visitFlag(flag, p);
    }

    @Override
    public Docker.Argument visitArgument(Docker.Argument argument, P p) {
        return (Docker.Argument) super.visitArgument(argument, p);
    }

    @Override
    public Docker.PlainText visitPlainText(Docker.PlainText plainText, P p) {
        return (Docker.PlainText) super.visitPlainText(plainText, p);
    }

    @Override
    public Docker.QuotedString visitQuotedString(Docker.QuotedString quotedString, P p) {
        return (Docker.QuotedString) super.visitQuotedString(quotedString, p);
    }

    @Override
    public Docker.EnvironmentVariable visitEnvironmentVariable(Docker.EnvironmentVariable environmentVariable, P p) {
        return (Docker.EnvironmentVariable) super.visitEnvironmentVariable(environmentVariable, p);
    }
}
