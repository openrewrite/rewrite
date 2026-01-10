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
package org.openrewrite.docker.internal;

import org.openrewrite.PrintOutputCapture;
import org.openrewrite.docker.DockerVisitor;
import org.openrewrite.docker.tree.Comment;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.docker.tree.Space;
import org.openrewrite.marker.Marker;

public class DockerPrinter<P> extends DockerVisitor<PrintOutputCapture<P>> {

    @Override
    public Space visitSpace(Space space, PrintOutputCapture<P> p) {
        for (Comment comment : space.getComments()) {
            p.append(comment.getPrefix());
            p.append(comment.getText());
        }
        p.append(space.getWhitespace());
        return space;
    }

    @Override
    public Docker visitFile(Docker.File file, PrintOutputCapture<P> p) {
        beforeSyntax(file, p);

        // Print global ARG instructions
        for (Docker.Arg arg : file.getGlobalArgs()) {
            visit(arg, p);
        }

        // Print build stages
        for (Docker.Stage stage : file.getStages()) {
            visit(stage, p);
        }

        visitSpace(file.getEof(), p);
        afterSyntax(file, p);
        return file;
    }

    @Override
    public Docker visitStage(Docker.Stage stage, PrintOutputCapture<P> p) {
        beforeSyntax(stage, p);

        // Print FROM instruction
        visit(stage.getFrom(), p);

        // Print stage instructions
        for (Docker.Instruction instruction : stage.getInstructions()) {
            visit(instruction, p);
        }

        afterSyntax(stage, p);
        return stage;
    }

    @Override
    public Docker visitFrom(Docker.From from, PrintOutputCapture<P> p) {
        beforeSyntax(from, p);
        p.append(from.getKeyword());
        if (from.getFlags() != null) {
            for (Docker.Flag flag : from.getFlags()) {
                visit(flag, p);
            }
        }
        visit(from.getImageName(), p);
        if (from.getTag() != null) {
            p.append(":");
            visit(from.getTag(), p);
        } else if (from.getDigest() != null) {
            p.append("@");
            visit(from.getDigest(), p);
        }
        if (from.getAs() != null) {
            visitSpace(from.getAs().getPrefix(), p);
            p.append(from.getAs().getKeyword());
            visit(from.getAs().getName(), p);
        }
        afterSyntax(from, p);
        return from;
    }

    @Override
    public Docker visitRun(Docker.Run run, PrintOutputCapture<P> p) {
        beforeSyntax(run, p);
        p.append(run.getKeyword());
        if (run.getFlags() != null) {
            for (Docker.Flag flag : run.getFlags()) {
                visit(flag, p);
            }
        }
        visit(run.getCommandLine(), p);
        afterSyntax(run, p);
        return run;
    }

    @Override
    public Docker visitAdd(Docker.Add add, PrintOutputCapture<P> p) {
        beforeSyntax(add, p);
        p.append(add.getKeyword());
        if (add.getFlags() != null) {
            for (Docker.Flag flag : add.getFlags()) {
                visit(flag, p);
            }
        }
        if (add.getHeredoc() != null) {
            visit(add.getHeredoc(), p);
        } else if (add.getExecForm() != null) {
            visit(add.getExecForm(), p);
        } else if (add.getSources() != null) {
            for (Docker.Argument source : add.getSources()) {
                visit(source, p);
            }
            visit(add.getDestination(), p);
        }
        afterSyntax(add, p);
        return add;
    }

    @Override
    public Docker visitCopy(Docker.Copy copy, PrintOutputCapture<P> p) {
        beforeSyntax(copy, p);
        p.append(copy.getKeyword());
        if (copy.getFlags() != null) {
            for (Docker.Flag flag : copy.getFlags()) {
                visit(flag, p);
            }
        }
        if (copy.getHeredoc() != null) {
            visit(copy.getHeredoc(), p);
        } else if (copy.getExecForm() != null) {
            visit(copy.getExecForm(), p);
        } else if (copy.getSources() != null) {
            for (Docker.Argument source : copy.getSources()) {
                visit(source, p);
            }
            visit(copy.getDestination(), p);
        }
        afterSyntax(copy, p);
        return copy;
    }

    @Override
    public Docker visitArg(Docker.Arg arg, PrintOutputCapture<P> p) {
        beforeSyntax(arg, p);
        p.append(arg.getKeyword());
        visit(arg.getName(), p);
        if (arg.getValue() != null) {
            p.append("=");
            visit(arg.getValue(), p);
        }
        afterSyntax(arg, p);
        return arg;
    }

    @Override
    public Docker visitEnv(Docker.Env env, PrintOutputCapture<P> p) {
        beforeSyntax(env, p);
        p.append(env.getKeyword());
        for (Docker.Env.EnvPair pair : env.getPairs()) {
            visitSpace(pair.getPrefix(), p);
            visit(pair.getKey(), p);
            if (pair.isHasEquals()) {
                p.append("=");
            }
            visit(pair.getValue(), p);
        }
        afterSyntax(env, p);
        return env;
    }

    @Override
    public Docker visitLabel(Docker.Label label, PrintOutputCapture<P> p) {
        beforeSyntax(label, p);
        p.append(label.getKeyword());
        for (Docker.Label.LabelPair pair : label.getPairs()) {
            visitSpace(pair.getPrefix(), p);
            visit(pair.getKey(), p);
            if (pair.isHasEquals()) {
                p.append("=");
            }
            visit(pair.getValue(), p);
        }
        afterSyntax(label, p);
        return label;
    }

    @Override
    public Docker visitCmd(Docker.Cmd cmd, PrintOutputCapture<P> p) {
        beforeSyntax(cmd, p);
        p.append(cmd.getKeyword());
        visit(cmd.getCommandLine(), p);
        afterSyntax(cmd, p);
        return cmd;
    }

    @Override
    public Docker visitEntrypoint(Docker.Entrypoint entrypoint, PrintOutputCapture<P> p) {
        beforeSyntax(entrypoint, p);
        p.append(entrypoint.getKeyword());
        visit(entrypoint.getCommandLine(), p);
        afterSyntax(entrypoint, p);
        return entrypoint;
    }

    @Override
    public Docker visitExpose(Docker.Expose expose, PrintOutputCapture<P> p) {
        beforeSyntax(expose, p);
        p.append(expose.getKeyword());
        for (Docker.Port port : expose.getPorts()) {
            visit(port, p);
        }
        afterSyntax(expose, p);
        return expose;
    }

    @Override
    public Docker visitPort(Docker.Port port, PrintOutputCapture<P> p) {
        beforeSyntax(port, p);
        p.append(port.getText());
        afterSyntax(port, p);
        return port;
    }

    @Override
    public Docker visitVolume(Docker.Volume volume, PrintOutputCapture<P> p) {
        beforeSyntax(volume, p);
        p.append(volume.getKeyword());
        if (volume.isJsonForm()) {
            // Print the space before [ and the bracket
            // Note: we assume a single space before [ for now (TODO: capture this properly)
            p.append(" [");
            for (int i = 0; i < volume.getValues().size(); i++) {
                Docker.Argument arg = volume.getValues().get(i);
                // Print the argument with its prefix (includes space after [ or after ,)
                visit(arg, p);
                // Print comma after this element if not last
                if (i < volume.getValues().size() - 1) {
                    p.append(",");
                }
            }
            visitSpace(volume.getClosingBracketPrefix(), p);
            p.append("]");
        } else {
            for (Docker.Argument value : volume.getValues()) {
                visit(value, p);
            }
        }
        afterSyntax(volume, p);
        return volume;
    }

    @Override
    public Docker visitShell(Docker.Shell shell, PrintOutputCapture<P> p) {
        beforeSyntax(shell, p);
        p.append(shell.getKeyword());
        // Print the space before [ and the bracket
        // Note: we assume a single space before [ for now (TODO: capture this properly)
        p.append(" [");
        for (int i = 0; i < shell.getArguments().size(); i++) {
            Docker.Argument arg = shell.getArguments().get(i);
            // Print the argument with its prefix (includes space after [ or after ,)
            visit(arg, p);
            // Print comma after this element if not last
            if (i < shell.getArguments().size() - 1) {
                p.append(",");
            }
        }
        visitSpace(shell.getClosingBracketPrefix(), p);
        p.append("]");
        afterSyntax(shell, p);
        return shell;
    }

    @Override
    public Docker visitWorkdir(Docker.Workdir workdir, PrintOutputCapture<P> p) {
        beforeSyntax(workdir, p);
        p.append(workdir.getKeyword());
        visit(workdir.getPath(), p);
        afterSyntax(workdir, p);
        return workdir;
    }

    @Override
    public Docker visitUser(Docker.User user, PrintOutputCapture<P> p) {
        beforeSyntax(user, p);
        p.append(user.getKeyword());
        visit(user.getUser(), p);
        if (user.getGroup() != null) {
            p.append(":");
            visit(user.getGroup(), p);
        }
        afterSyntax(user, p);
        return user;
    }

    @Override
    public Docker visitStopsignal(Docker.Stopsignal stopsignal, PrintOutputCapture<P> p) {
        beforeSyntax(stopsignal, p);
        p.append(stopsignal.getKeyword());
        visit(stopsignal.getSignal(), p);
        afterSyntax(stopsignal, p);
        return stopsignal;
    }

    @Override
    public Docker visitOnbuild(Docker.Onbuild onbuild, PrintOutputCapture<P> p) {
        beforeSyntax(onbuild, p);
        p.append(onbuild.getKeyword());
        visit(onbuild.getInstruction(), p);
        afterSyntax(onbuild, p);
        return onbuild;
    }

    @Override
    public Docker visitHealthcheck(Docker.Healthcheck healthcheck, PrintOutputCapture<P> p) {
        beforeSyntax(healthcheck, p);
        p.append(healthcheck.getKeyword());
        if (healthcheck.isNone()) {
            // Need to print the space and NONE keyword
            if (healthcheck.getCmd() != null) {
                visitSpace(healthcheck.getCmd().getPrefix(), p);
            }
            p.append("NONE");
        } else {
            if (healthcheck.getFlags() != null) {
                for (Docker.Flag flag : healthcheck.getFlags()) {
                    visit(flag, p);
                }
            }
            if (healthcheck.getCmd() != null) {
                visit(healthcheck.getCmd(), p);
            }
        }
        afterSyntax(healthcheck, p);
        return healthcheck;
    }

    @Override
    public Docker visitMaintainer(Docker.Maintainer maintainer, PrintOutputCapture<P> p) {
        beforeSyntax(maintainer, p);
        p.append(maintainer.getKeyword());
        visit(maintainer.getText(), p);
        afterSyntax(maintainer, p);
        return maintainer;
    }

    @Override
    public Docker visitCommandLine(Docker.CommandLine commandLine, PrintOutputCapture<P> p) {
        beforeSyntax(commandLine, p);
        visit(commandLine.getForm(), p);
        afterSyntax(commandLine, p);
        return commandLine;
    }

    @Override
    public Docker visitShellForm(Docker.ShellForm shellForm, PrintOutputCapture<P> p) {
        beforeSyntax(shellForm, p);
        for (Docker.Literal literal : shellForm.getArguments()) {
            visit(literal, p);
        }
        afterSyntax(shellForm, p);
        return shellForm;
    }

    @Override
    public Docker visitExecForm(Docker.ExecForm execForm, PrintOutputCapture<P> p) {
        beforeSyntax(execForm, p);
        p.append("[");
        for (Docker.Literal literal : execForm.getArguments()) {
            visit(literal, p);
        }
        visitSpace(execForm.getClosingBracketPrefix(), p);
        p.append("]");
        afterSyntax(execForm, p);
        return execForm;
    }

    @Override
    public Docker visitHeredocForm(Docker.HeredocForm heredocForm, PrintOutputCapture<P> p) {
        beforeSyntax(heredocForm, p);
        p.append(heredocForm.getOpening());
        if (heredocForm.getDestination() != null) {
            visit(heredocForm.getDestination(), p);
        }
        for (String contentLine : heredocForm.getContentLines()) {
            p.append(contentLine);
        }
        p.append(heredocForm.getClosing());
        afterSyntax(heredocForm, p);
        return heredocForm;
    }

    @Override
    public Docker visitFlag(Docker.Flag flag, PrintOutputCapture<P> p) {
        beforeSyntax(flag, p);
        p.append("--").append(flag.getName());
        if (flag.getValue() != null) {
            p.append("=");
            visit(flag.getValue(), p);
        }
        afterSyntax(flag, p);
        return flag;
    }

    @Override
    public Docker visitArgument(Docker.Argument argument, PrintOutputCapture<P> p) {
        beforeSyntax(argument, p);
        for (Docker.ArgumentContent content : argument.getContents()) {
            visit(content, p);
        }
        afterSyntax(argument, p);
        return argument;
    }

    @Override
    public Docker visitLiteral(Docker.Literal literal, PrintOutputCapture<P> p) {
        beforeSyntax(literal, p);
        if (literal.getQuoteStyle() != null) {
            char quote = literal.getQuoteStyle() == Docker.Literal.QuoteStyle.DOUBLE ? '"' : '\'';
            p.append(quote).append(literal.getText()).append(quote);
        } else {
            p.append(literal.getText());
        }
        afterSyntax(literal, p);
        return literal;
    }

    @Override
    public Docker visitEnvironmentVariable(Docker.EnvironmentVariable environmentVariable, PrintOutputCapture<P> p) {
        beforeSyntax(environmentVariable, p);
        if (environmentVariable.isBraced()) {
            p.append("${").append(environmentVariable.getName()).append("}");
        } else {
            p.append("$").append(environmentVariable.getName());
        }
        afterSyntax(environmentVariable, p);
        return environmentVariable;
    }

    private static final java.util.function.UnaryOperator<String> DOCKERFILE_MARKER_WRAPPER =
            out -> "~~" + out + (out.isEmpty() ? "" : "~~") + ">";

    private void beforeSyntax(Docker d, PrintOutputCapture<P> p) {
        beforeSyntax(d.getPrefix(), d.getMarkers(), p);
    }

    private void beforeSyntax(Space prefix, org.openrewrite.marker.Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforePrefix(marker, new org.openrewrite.Cursor(getCursor(), marker), DOCKERFILE_MARKER_WRAPPER));
        }
        visitSpace(prefix, p);
        visitMarkers(markers, p);
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforeSyntax(marker, new org.openrewrite.Cursor(getCursor(), marker), DOCKERFILE_MARKER_WRAPPER));
        }
    }

    private void afterSyntax(Docker d, PrintOutputCapture<P> p) {
        afterSyntax(d.getMarkers(), p);
    }

    private void afterSyntax(org.openrewrite.marker.Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().afterSyntax(marker, new org.openrewrite.Cursor(getCursor(), marker), DOCKERFILE_MARKER_WRAPPER));
        }
    }
}
