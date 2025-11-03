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
package org.openrewrite.docker;

import org.jspecify.annotations.Nullable;
import org.openrewrite.docker.tree.Dockerfile;

public class DockerfileIsoVisitor<P> extends DockerfileVisitor<P> {

    @Override
    public Dockerfile.Document visitDocument(Dockerfile.Document document, P p) {
        return (Dockerfile.Document) super.visitDocument(document, p);
    }

    @Override
    public Dockerfile.Stage visitStage(Dockerfile.Stage stage, P p) {
        return (Dockerfile.Stage) super.visitStage(stage, p);
    }

    @Override
    public Dockerfile.From visitFrom(Dockerfile.From from, P p) {
        return (Dockerfile.From) super.visitFrom(from, p);
    }

    @Override
    public Dockerfile.From.@Nullable As visitFromAs(Dockerfile.From.As as, P p) {
        return super.visitFromAs(as, p);
    }

    @Override
    public Dockerfile.Run visitRun(Dockerfile.Run run, P p) {
        return (Dockerfile.Run) super.visitRun(run, p);
    }

    @Override
    public Dockerfile.Add visitAdd(Dockerfile.Add add, P p) {
        return (Dockerfile.Add) super.visitAdd(add, p);
    }

    @Override
    public Dockerfile.Copy visitCopy(Dockerfile.Copy copy, P p) {
        return (Dockerfile.Copy) super.visitCopy(copy, p);
    }

    @Override
    public Dockerfile.Arg visitArg(Dockerfile.Arg arg, P p) {
        return (Dockerfile.Arg) super.visitArg(arg, p);
    }

    @Override
    public Dockerfile.Env visitEnv(Dockerfile.Env env, P p) {
        return (Dockerfile.Env) super.visitEnv(env, p);
    }

    @Override
    public Dockerfile.Env.EnvPair visitEnvPair(Dockerfile.Env.EnvPair pair, P p) {
        return super.visitEnvPair(pair, p);
    }

    @Override
    public Dockerfile.Label visitLabel(Dockerfile.Label label, P p) {
        return (Dockerfile.Label) super.visitLabel(label, p);
    }

    @Override
    public Dockerfile.Label.LabelPair visitLabelPair(Dockerfile.Label.LabelPair pair, P p) {
        return super.visitLabelPair(pair, p);
    }

    @Override
    public Dockerfile.Cmd visitCmd(Dockerfile.Cmd cmd, P p) {
        return (Dockerfile.Cmd) super.visitCmd(cmd, p);
    }

    @Override
    public Dockerfile.Entrypoint visitEntrypoint(Dockerfile.Entrypoint entrypoint, P p) {
        return (Dockerfile.Entrypoint) super.visitEntrypoint(entrypoint, p);
    }

    @Override
    public Dockerfile.Expose visitExpose(Dockerfile.Expose expose, P p) {
        return (Dockerfile.Expose) super.visitExpose(expose, p);
    }

    @Override
    public Dockerfile.Volume visitVolume(Dockerfile.Volume volume, P p) {
        return (Dockerfile.Volume) super.visitVolume(volume, p);
    }

    @Override
    public Dockerfile.Shell visitShell(Dockerfile.Shell shell, P p) {
        return (Dockerfile.Shell) super.visitShell(shell, p);
    }

    @Override
    public Dockerfile.Workdir visitWorkdir(Dockerfile.Workdir workdir, P p) {
        return (Dockerfile.Workdir) super.visitWorkdir(workdir, p);
    }

    @Override
    public Dockerfile.User visitUser(Dockerfile.User user, P p) {
        return (Dockerfile.User) super.visitUser(user, p);
    }

    @Override
    public Dockerfile.Stopsignal visitStopsignal(Dockerfile.Stopsignal stopsignal, P p) {
        return (Dockerfile.Stopsignal) super.visitStopsignal(stopsignal, p);
    }

    @Override
    public Dockerfile.Onbuild visitOnbuild(Dockerfile.Onbuild onbuild, P p) {
        return (Dockerfile.Onbuild) super.visitOnbuild(onbuild, p);
    }

    @Override
    public Dockerfile.Healthcheck visitHealthcheck(Dockerfile.Healthcheck healthcheck, P p) {
        return (Dockerfile.Healthcheck) super.visitHealthcheck(healthcheck, p);
    }

    @Override
    public Dockerfile.Maintainer visitMaintainer(Dockerfile.Maintainer maintainer, P p) {
        return (Dockerfile.Maintainer) super.visitMaintainer(maintainer, p);
    }

    @Override
    public Dockerfile.CommandLine visitCommandLine(Dockerfile.CommandLine commandLine, P p) {
        return (Dockerfile.CommandLine) super.visitCommandLine(commandLine, p);
    }

    @Override
    public Dockerfile.ShellForm visitShellForm(Dockerfile.ShellForm shellForm, P p) {
        return (Dockerfile.ShellForm) super.visitShellForm(shellForm, p);
    }

    @Override
    public Dockerfile.ExecForm visitExecForm(Dockerfile.ExecForm execForm, P p) {
        return (Dockerfile.ExecForm) super.visitExecForm(execForm, p);
    }

    @Override
    public Dockerfile.HeredocForm visitHeredocForm(Dockerfile.HeredocForm heredocForm, P p) {
        return (Dockerfile.HeredocForm) super.visitHeredocForm(heredocForm, p);
    }

    @Override
    public Dockerfile.Flag visitFlag(Dockerfile.Flag flag, P p) {
        return (Dockerfile.Flag) super.visitFlag(flag, p);
    }

    @Override
    public Dockerfile.Argument visitArgument(Dockerfile.Argument argument, P p) {
        return (Dockerfile.Argument) super.visitArgument(argument, p);
    }

    @Override
    public Dockerfile.PlainText visitPlainText(Dockerfile.PlainText plainText, P p) {
        return (Dockerfile.PlainText) super.visitPlainText(plainText, p);
    }

    @Override
    public Dockerfile.QuotedString visitQuotedString(Dockerfile.QuotedString quotedString, P p) {
        return (Dockerfile.QuotedString) super.visitQuotedString(quotedString, p);
    }

    @Override
    public Dockerfile.EnvironmentVariable visitEnvironmentVariable(Dockerfile.EnvironmentVariable environmentVariable, P p) {
        return (Dockerfile.EnvironmentVariable) super.visitEnvironmentVariable(environmentVariable, p);
    }
}
