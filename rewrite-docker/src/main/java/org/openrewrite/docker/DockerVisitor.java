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
import org.openrewrite.TreeVisitor;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.docker.tree.Space;
import org.openrewrite.internal.ListUtils;

public class DockerVisitor<P> extends TreeVisitor<Docker, P> {

    public Space visitSpace(Space space, P p) {
        return space;
    }

    public Docker visitFile(Docker.File file, P p) {
        Docker.File d = file;
        d = d.withPrefix(visitSpace(d.getPrefix(), p));
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        d = d.withGlobalArgs(ListUtils.map(d.getGlobalArgs(), arg -> (Docker.Arg) visit(arg, p)));
        return d.withStages(ListUtils.map(d.getStages(), stage -> (Docker.Stage) visit(stage, p)));
    }

    public Docker visitStage(Docker.Stage stage, P p) {
        Docker.Stage s = stage;
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.withFrom((Docker.From) visit(s.getFrom(), p));
        return s.withInstructions(ListUtils.map(s.getInstructions(), inst -> (Docker.Instruction) visit(inst, p)));
    }

    public Docker visitFrom(Docker.From from, P p) {
        Docker.From f = from;
        f = f.withPrefix(visitSpace(f.getPrefix(), p));
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        if (f.getFlags() != null) {
            f = f.withFlags(ListUtils.map(f.getFlags(), flag -> (Docker.Flag) visit(flag, p)));
        }
        f = f.withImageName((Docker.Argument) visit(f.getImageName(), p));
        if (f.getTag() != null) {
            f = f.withTag((Docker.Argument) visit(f.getTag(), p));
        }
        if (f.getDigest() != null) {
            f = f.withDigest((Docker.Argument) visit(f.getDigest(), p));
        }
        if (f.getAs() != null) {
            f = f.withAs(visitFromAs(f.getAs(), p));
        }
        return f;
    }

    public Docker.From.@Nullable As visitFromAs(Docker.From.As as, P p) {
        Docker.From.As a = as;
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        return a.withName((Docker.Argument) visit(a.getName(), p));
    }

    public Docker visitRun(Docker.Run run, P p) {
        Docker.Run r = run;
        r = r.withPrefix(visitSpace(r.getPrefix(), p));
        r = r.withMarkers(visitMarkers(r.getMarkers(), p));
        if (r.getFlags() != null) {
            r = r.withFlags(ListUtils.map(r.getFlags(), flag -> (Docker.Flag) visit(flag, p)));
        }
        return r.withCommandLine((Docker.CommandLine) visit(r.getCommandLine(), p));
    }

    public Docker visitAdd(Docker.Add add, P p) {
        Docker.Add a = add;
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        if (a.getFlags() != null) {
            a = a.withFlags(ListUtils.map(a.getFlags(), flag -> (Docker.Flag) visit(flag, p)));
        }
        if (a.getHeredoc() != null) {
            a = a.withHeredoc((Docker.HeredocForm) visit(a.getHeredoc(), p));
        } else if (a.getSources() != null) {
            a = a.withSources(ListUtils.map(a.getSources(), source -> (Docker.Argument) visit(source, p)));
        }
        if (a.getDestination() != null) {
            a = a.withDestination((Docker.Argument) visit(a.getDestination(), p));
        }
        return a;
    }

    public Docker visitCopy(Docker.Copy copy, P p) {
        Docker.Copy c = copy;
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        if (c.getFlags() != null) {
            c = c.withFlags(ListUtils.map(c.getFlags(), flag -> (Docker.Flag) visit(flag, p)));
        }
        if (c.getHeredoc() != null) {
            c = c.withHeredoc((Docker.HeredocForm) visit(c.getHeredoc(), p));
        } else if (c.getSources() != null) {
            c = c.withSources(ListUtils.map(c.getSources(), source -> (Docker.Argument) visit(source, p)));
        }
        if (c.getDestination() != null) {
            c = c.withDestination((Docker.Argument) visit(c.getDestination(), p));
        }
        return c;
    }

    public Docker visitArg(Docker.Arg arg, P p) {
        Docker.Arg a = arg;
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        a = a.withName((Docker.Argument) visit(a.getName(), p));
        if (a.getValue() != null) {
            a = a.withValue((Docker.Argument) visit(a.getValue(), p));
        }
        return a;
    }

    public Docker visitEnv(Docker.Env env, P p) {
        Docker.Env e = env;
        e = e.withPrefix(visitSpace(e.getPrefix(), p));
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        return e.withPairs(ListUtils.map(e.getPairs(), pair -> visitEnvPair(pair, p)));
    }

    public Docker.Env.EnvPair visitEnvPair(Docker.Env.EnvPair pair, P p) {
        Docker.Env.EnvPair ep = pair;
        ep = ep.withPrefix(visitSpace(ep.getPrefix(), p));
        ep = ep.withMarkers(visitMarkers(ep.getMarkers(), p));
        ep = ep.withKey((Docker.Argument) visit(ep.getKey(), p));
        return ep.withValue((Docker.Argument) visit(ep.getValue(), p));
    }

    public Docker visitLabel(Docker.Label label, P p) {
        Docker.Label l = label;
        l = l.withPrefix(visitSpace(l.getPrefix(), p));
        l = l.withMarkers(visitMarkers(l.getMarkers(), p));
        return l.withPairs(ListUtils.map(l.getPairs(), pair -> visitLabelPair(pair, p)));
    }

    public Docker.Label.LabelPair visitLabelPair(Docker.Label.LabelPair pair, P p) {
        Docker.Label.LabelPair lp = pair;
        lp = lp.withPrefix(visitSpace(lp.getPrefix(), p));
        lp = lp.withMarkers(visitMarkers(lp.getMarkers(), p));
        lp = lp.withKey((Docker.Argument) visit(lp.getKey(), p));
        return lp.withValue((Docker.Argument) visit(lp.getValue(), p));
    }

    public Docker visitCmd(Docker.Cmd cmd, P p) {
        Docker.Cmd c = cmd;
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        return c.withCommandLine((Docker.CommandLine) visit(c.getCommandLine(), p));
    }

    public Docker visitEntrypoint(Docker.Entrypoint entrypoint, P p) {
        Docker.Entrypoint e = entrypoint;
        e = e.withPrefix(visitSpace(e.getPrefix(), p));
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        return e.withCommandLine((Docker.CommandLine) visit(e.getCommandLine(), p));
    }

    public Docker visitExpose(Docker.Expose expose, P p) {
        Docker.Expose e = expose;
        e = e.withPrefix(visitSpace(e.getPrefix(), p));
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        return e.withPorts(ListUtils.map(e.getPorts(), port -> (Docker.Port) visit(port, p)));
    }

    public Docker visitPort(Docker.Port port, P p) {
        Docker.Port pt = port;
        pt = pt.withPrefix(visitSpace(pt.getPrefix(), p));
        return pt.withMarkers(visitMarkers(pt.getMarkers(), p));
    }

    public Docker visitVolume(Docker.Volume volume, P p) {
        Docker.Volume v = volume;
        v = v.withPrefix(visitSpace(v.getPrefix(), p));
        v = v.withMarkers(visitMarkers(v.getMarkers(), p));
        return v.withValues(ListUtils.map(v.getValues(), value -> (Docker.Argument) visit(value, p)));
    }

    public Docker visitShell(Docker.Shell shell, P p) {
        Docker.Shell s = shell;
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        return s.withArguments(ListUtils.map(s.getArguments(), arg -> (Docker.Argument) visit(arg, p)));
    }

    public Docker visitWorkdir(Docker.Workdir workdir, P p) {
        Docker.Workdir w = workdir;
        w = w.withPrefix(visitSpace(w.getPrefix(), p));
        w = w.withMarkers(visitMarkers(w.getMarkers(), p));
        return w.withPath((Docker.Argument) visit(w.getPath(), p));
    }

    public Docker visitUser(Docker.User user, P p) {
        Docker.User u = user;
        u = u.withPrefix(visitSpace(u.getPrefix(), p));
        u = u.withMarkers(visitMarkers(u.getMarkers(), p));
        u = u.withUser((Docker.Argument) visit(u.getUser(), p));
        if (u.getGroup() != null) {
            u = u.withGroup((Docker.Argument) visit(u.getGroup(), p));
        }
        return u;
    }

    public Docker visitStopsignal(Docker.Stopsignal stopsignal, P p) {
        Docker.Stopsignal s = stopsignal;
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        return s.withSignal((Docker.Argument) visit(s.getSignal(), p));
    }

    public Docker visitOnbuild(Docker.Onbuild onbuild, P p) {
        Docker.Onbuild o = onbuild;
        o = o.withPrefix(visitSpace(o.getPrefix(), p));
        o = o.withMarkers(visitMarkers(o.getMarkers(), p));
        return o.withInstruction((Docker.Instruction) visit(o.getInstruction(), p));
    }

    public Docker visitHealthcheck(Docker.Healthcheck healthcheck, P p) {
        Docker.Healthcheck h = healthcheck;
        h = h.withPrefix(visitSpace(h.getPrefix(), p));
        h = h.withMarkers(visitMarkers(h.getMarkers(), p));
        if (h.getFlags() != null) {
            h = h.withFlags(ListUtils.map(h.getFlags(), flag -> (Docker.Flag) visit(flag, p)));
        }
        if (h.getCmd() != null) {
            h = h.withCmd((Docker.Cmd) visit(h.getCmd(), p));
        }
        return h;
    }

    public Docker visitMaintainer(Docker.Maintainer maintainer, P p) {
        Docker.Maintainer m = maintainer;
        m = m.withPrefix(visitSpace(m.getPrefix(), p));
        m = m.withMarkers(visitMarkers(m.getMarkers(), p));
        return m.withText((Docker.Argument) visit(m.getText(), p));
    }

    public Docker visitCommandLine(Docker.CommandLine commandLine, P p) {
        Docker.CommandLine cl = commandLine;
        cl = cl.withPrefix(visitSpace(cl.getPrefix(), p));
        cl = cl.withMarkers(visitMarkers(cl.getMarkers(), p));
        return cl.withForm((Docker.CommandForm) visit(cl.getForm(), p));
    }

    public Docker visitShellForm(Docker.ShellForm shellForm, P p) {
        Docker.ShellForm sf = shellForm;
        sf = sf.withPrefix(visitSpace(sf.getPrefix(), p));
        sf = sf.withMarkers(visitMarkers(sf.getMarkers(), p));
        return sf.withArguments(ListUtils.map(sf.getArguments(), arg -> (Docker.Argument) visit(arg, p)));
    }

    public Docker visitExecForm(Docker.ExecForm execForm, P p) {
        Docker.ExecForm ef = execForm;
        ef = ef.withPrefix(visitSpace(ef.getPrefix(), p));
        ef = ef.withMarkers(visitMarkers(ef.getMarkers(), p));
        return ef.withArguments(ListUtils.map(ef.getArguments(), arg -> (Docker.Argument) visit(arg, p)));
    }

    public Docker visitHeredocForm(Docker.HeredocForm heredocForm, P p) {
        Docker.HeredocForm hf = heredocForm;
        hf = hf.withPrefix(visitSpace(hf.getPrefix(), p));
        hf = hf.withMarkers(visitMarkers(hf.getMarkers(), p));
        if (hf.getDestination() != null) {
            hf = hf.withDestination((Docker.Argument) visit(hf.getDestination(), p));
        }
        return hf;
    }

    public Docker visitFlag(Docker.Flag flag, P p) {
        Docker.Flag f = flag;
        f = f.withPrefix(visitSpace(f.getPrefix(), p));
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        if (f.getValue() != null) {
            f = f.withValue((Docker.Argument) visit(f.getValue(), p));
        }
        return f;
    }

    public Docker visitArgument(Docker.Argument argument, P p) {
        Docker.Argument a = argument;
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        return a.withContents(ListUtils.map(a.getContents(), content -> (Docker.ArgumentContent) visit(content, p)));
    }

    public Docker visitPlainText(Docker.PlainText plainText, P p) {
        Docker.PlainText pt = plainText;
        pt = pt.withPrefix(visitSpace(pt.getPrefix(), p));
        return pt.withMarkers(visitMarkers(pt.getMarkers(), p));
    }

    public Docker visitQuotedString(Docker.QuotedString quotedString, P p) {
        Docker.QuotedString qs = quotedString;
        qs = qs.withPrefix(visitSpace(qs.getPrefix(), p));
        return qs.withMarkers(visitMarkers(qs.getMarkers(), p));
    }

    public Docker visitEnvironmentVariable(Docker.EnvironmentVariable environmentVariable, P p) {
        Docker.EnvironmentVariable ev = environmentVariable;
        ev = ev.withPrefix(visitSpace(ev.getPrefix(), p));
        return ev.withMarkers(visitMarkers(ev.getMarkers(), p));
    }
}
