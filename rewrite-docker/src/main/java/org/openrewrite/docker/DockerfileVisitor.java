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
import org.openrewrite.TreeVisitor;
import org.openrewrite.docker.tree.Dockerfile;
import org.openrewrite.docker.tree.Space;
import org.openrewrite.internal.ListUtils;

public class DockerfileVisitor<P> extends TreeVisitor<Dockerfile, P> {

    public Space visitSpace(Space space, P p) {
        return space;
    }

    public Dockerfile visitDocument(Dockerfile.Document document, P p) {
        Dockerfile.Document d = document;
        d = d.withPrefix(visitSpace(d.getPrefix(), p));
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        d = d.withGlobalArgs(ListUtils.map(d.getGlobalArgs(), arg -> (Dockerfile.Arg) visit(arg, p)));
        return d.withStages(ListUtils.map(d.getStages(), stage -> (Dockerfile.Stage) visit(stage, p)));
    }

    public Dockerfile visitStage(Dockerfile.Stage stage, P p) {
        Dockerfile.Stage s = stage;
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.withFrom((Dockerfile.From) visit(s.getFrom(), p));
        return s.withInstructions(ListUtils.map(s.getInstructions(), inst -> (Dockerfile.Instruction) visit(inst, p)));
    }

    public Dockerfile visitFrom(Dockerfile.From from, P p) {
        Dockerfile.From f = from;
        f = f.withPrefix(visitSpace(f.getPrefix(), p));
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        if (f.getFlags() != null) {
            f = f.withFlags(ListUtils.map(f.getFlags(), flag -> (Dockerfile.Flag) visit(flag, p)));
        }
        f = f.withImageName((Dockerfile.Argument) visit(f.getImageName(), p));
        if (f.getTag() != null) {
            f = f.withTag((Dockerfile.Argument) visit(f.getTag(), p));
        }
        if (f.getDigest() != null) {
            f = f.withDigest((Dockerfile.Argument) visit(f.getDigest(), p));
        }
        if (f.getAs() != null) {
            f = f.withAs(visitFromAs(f.getAs(), p));
        }
        return f;
    }

    public Dockerfile.From.@Nullable As visitFromAs(Dockerfile.From.As as, P p) {
        Dockerfile.From.As a = as;
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        return a.withName((Dockerfile.Argument) visit(a.getName(), p));
    }

    public Dockerfile visitRun(Dockerfile.Run run, P p) {
        Dockerfile.Run r = run;
        r = r.withPrefix(visitSpace(r.getPrefix(), p));
        r = r.withMarkers(visitMarkers(r.getMarkers(), p));
        if (r.getFlags() != null) {
            r = r.withFlags(ListUtils.map(r.getFlags(), flag -> (Dockerfile.Flag) visit(flag, p)));
        }
        return r.withCommandLine((Dockerfile.CommandLine) visit(r.getCommandLine(), p));
    }

    public Dockerfile visitAdd(Dockerfile.Add add, P p) {
        Dockerfile.Add a = add;
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        if (a.getFlags() != null) {
            a = a.withFlags(ListUtils.map(a.getFlags(), flag -> (Dockerfile.Flag) visit(flag, p)));
        }
        if (a.getHeredoc() != null) {
            a = a.withHeredoc((Dockerfile.HeredocForm) visit(a.getHeredoc(), p));
        } else if (a.getSources() != null) {
            a = a.withSources(ListUtils.map(a.getSources(), source -> (Dockerfile.Argument) visit(source, p)));
        }
        if (a.getDestination() != null) {
            a = a.withDestination((Dockerfile.Argument) visit(a.getDestination(), p));
        }
        return a;
    }

    public Dockerfile visitCopy(Dockerfile.Copy copy, P p) {
        Dockerfile.Copy c = copy;
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        if (c.getFlags() != null) {
            c = c.withFlags(ListUtils.map(c.getFlags(), flag -> (Dockerfile.Flag) visit(flag, p)));
        }
        if (c.getHeredoc() != null) {
            c = c.withHeredoc((Dockerfile.HeredocForm) visit(c.getHeredoc(), p));
        } else if (c.getSources() != null) {
            c = c.withSources(ListUtils.map(c.getSources(), source -> (Dockerfile.Argument) visit(source, p)));
        }
        if (c.getDestination() != null) {
            c = c.withDestination((Dockerfile.Argument) visit(c.getDestination(), p));
        }
        return c;
    }

    public Dockerfile visitArg(Dockerfile.Arg arg, P p) {
        Dockerfile.Arg a = arg;
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        a = a.withName((Dockerfile.Argument) visit(a.getName(), p));
        if (a.getValue() != null) {
            a = a.withValue((Dockerfile.Argument) visit(a.getValue(), p));
        }
        return a;
    }

    public Dockerfile visitEnv(Dockerfile.Env env, P p) {
        Dockerfile.Env e = env;
        e = e.withPrefix(visitSpace(e.getPrefix(), p));
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        return e.withPairs(ListUtils.map(e.getPairs(), pair -> visitEnvPair(pair, p)));
    }

    public Dockerfile.Env.EnvPair visitEnvPair(Dockerfile.Env.EnvPair pair, P p) {
        Dockerfile.Env.EnvPair ep = pair;
        ep = ep.withPrefix(visitSpace(ep.getPrefix(), p));
        ep = ep.withMarkers(visitMarkers(ep.getMarkers(), p));
        ep = ep.withKey((Dockerfile.Argument) visit(ep.getKey(), p));
        return ep.withValue((Dockerfile.Argument) visit(ep.getValue(), p));
    }

    public Dockerfile visitLabel(Dockerfile.Label label, P p) {
        Dockerfile.Label l = label;
        l = l.withPrefix(visitSpace(l.getPrefix(), p));
        l = l.withMarkers(visitMarkers(l.getMarkers(), p));
        return l.withPairs(ListUtils.map(l.getPairs(), pair -> visitLabelPair(pair, p)));
    }

    public Dockerfile.Label.LabelPair visitLabelPair(Dockerfile.Label.LabelPair pair, P p) {
        Dockerfile.Label.LabelPair lp = pair;
        lp = lp.withPrefix(visitSpace(lp.getPrefix(), p));
        lp = lp.withMarkers(visitMarkers(lp.getMarkers(), p));
        lp = lp.withKey((Dockerfile.Argument) visit(lp.getKey(), p));
        return lp.withValue((Dockerfile.Argument) visit(lp.getValue(), p));
    }

    public Dockerfile visitCmd(Dockerfile.Cmd cmd, P p) {
        Dockerfile.Cmd c = cmd;
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        return c.withCommandLine((Dockerfile.CommandLine) visit(c.getCommandLine(), p));
    }

    public Dockerfile visitEntrypoint(Dockerfile.Entrypoint entrypoint, P p) {
        Dockerfile.Entrypoint e = entrypoint;
        e = e.withPrefix(visitSpace(e.getPrefix(), p));
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        return e.withCommandLine((Dockerfile.CommandLine) visit(e.getCommandLine(), p));
    }

    public Dockerfile visitExpose(Dockerfile.Expose expose, P p) {
        Dockerfile.Expose e = expose;
        e = e.withPrefix(visitSpace(e.getPrefix(), p));
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        return e.withPorts(ListUtils.map(e.getPorts(), port -> (Dockerfile.Argument) visit(port, p)));
    }

    public Dockerfile visitVolume(Dockerfile.Volume volume, P p) {
        Dockerfile.Volume v = volume;
        v = v.withPrefix(visitSpace(v.getPrefix(), p));
        v = v.withMarkers(visitMarkers(v.getMarkers(), p));
        return v.withValues(ListUtils.map(v.getValues(), value -> (Dockerfile.Argument) visit(value, p)));
    }

    public Dockerfile visitShell(Dockerfile.Shell shell, P p) {
        Dockerfile.Shell s = shell;
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        return s.withArguments(ListUtils.map(s.getArguments(), arg -> (Dockerfile.Argument) visit(arg, p)));
    }

    public Dockerfile visitWorkdir(Dockerfile.Workdir workdir, P p) {
        Dockerfile.Workdir w = workdir;
        w = w.withPrefix(visitSpace(w.getPrefix(), p));
        w = w.withMarkers(visitMarkers(w.getMarkers(), p));
        return w.withPath((Dockerfile.Argument) visit(w.getPath(), p));
    }

    public Dockerfile visitUser(Dockerfile.User user, P p) {
        Dockerfile.User u = user;
        u = u.withPrefix(visitSpace(u.getPrefix(), p));
        u = u.withMarkers(visitMarkers(u.getMarkers(), p));
        u = u.withUser((Dockerfile.Argument) visit(u.getUser(), p));
        if (u.getGroup() != null) {
            u = u.withGroup((Dockerfile.Argument) visit(u.getGroup(), p));
        }
        return u;
    }

    public Dockerfile visitStopsignal(Dockerfile.Stopsignal stopsignal, P p) {
        Dockerfile.Stopsignal s = stopsignal;
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        return s.withSignal((Dockerfile.Argument) visit(s.getSignal(), p));
    }

    public Dockerfile visitOnbuild(Dockerfile.Onbuild onbuild, P p) {
        Dockerfile.Onbuild o = onbuild;
        o = o.withPrefix(visitSpace(o.getPrefix(), p));
        o = o.withMarkers(visitMarkers(o.getMarkers(), p));
        return o.withInstruction((Dockerfile.Instruction) visit(o.getInstruction(), p));
    }

    public Dockerfile visitHealthcheck(Dockerfile.Healthcheck healthcheck, P p) {
        Dockerfile.Healthcheck h = healthcheck;
        h = h.withPrefix(visitSpace(h.getPrefix(), p));
        h = h.withMarkers(visitMarkers(h.getMarkers(), p));
        if (h.getFlags() != null) {
            h = h.withFlags(ListUtils.map(h.getFlags(), flag -> (Dockerfile.Flag) visit(flag, p)));
        }
        if (h.getCmd() != null) {
            h = h.withCmd((Dockerfile.Cmd) visit(h.getCmd(), p));
        }
        return h;
    }

    public Dockerfile visitMaintainer(Dockerfile.Maintainer maintainer, P p) {
        Dockerfile.Maintainer m = maintainer;
        m = m.withPrefix(visitSpace(m.getPrefix(), p));
        m = m.withMarkers(visitMarkers(m.getMarkers(), p));
        return m.withText((Dockerfile.Argument) visit(m.getText(), p));
    }

    public Dockerfile visitCommandLine(Dockerfile.CommandLine commandLine, P p) {
        Dockerfile.CommandLine cl = commandLine;
        cl = cl.withPrefix(visitSpace(cl.getPrefix(), p));
        cl = cl.withMarkers(visitMarkers(cl.getMarkers(), p));
        return cl.withForm((Dockerfile.CommandForm) visit(cl.getForm(), p));
    }

    public Dockerfile visitShellForm(Dockerfile.ShellForm shellForm, P p) {
        Dockerfile.ShellForm sf = shellForm;
        sf = sf.withPrefix(visitSpace(sf.getPrefix(), p));
        sf = sf.withMarkers(visitMarkers(sf.getMarkers(), p));
        return sf.withArguments(ListUtils.map(sf.getArguments(), arg -> (Dockerfile.Argument) visit(arg, p)));
    }

    public Dockerfile visitExecForm(Dockerfile.ExecForm execForm, P p) {
        Dockerfile.ExecForm ef = execForm;
        ef = ef.withPrefix(visitSpace(ef.getPrefix(), p));
        ef = ef.withMarkers(visitMarkers(ef.getMarkers(), p));
        return ef.withArguments(ListUtils.map(ef.getArguments(), arg -> (Dockerfile.Argument) visit(arg, p)));
    }

    public Dockerfile visitHeredocForm(Dockerfile.HeredocForm heredocForm, P p) {
        Dockerfile.HeredocForm hf = heredocForm;
        hf = hf.withPrefix(visitSpace(hf.getPrefix(), p));
        hf = hf.withMarkers(visitMarkers(hf.getMarkers(), p));
        if (hf.getDestination() != null) {
            hf = hf.withDestination((Dockerfile.Argument) visit(hf.getDestination(), p));
        }
        return hf;
    }

    public Dockerfile visitFlag(Dockerfile.Flag flag, P p) {
        Dockerfile.Flag f = flag;
        f = f.withPrefix(visitSpace(f.getPrefix(), p));
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        if (f.getValue() != null) {
            f = f.withValue((Dockerfile.Argument) visit(f.getValue(), p));
        }
        return f;
    }

    public Dockerfile visitArgument(Dockerfile.Argument argument, P p) {
        Dockerfile.Argument a = argument;
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        return a.withContents(ListUtils.map(a.getContents(), content -> (Dockerfile.ArgumentContent) visit(content, p)));
    }

    public Dockerfile visitPlainText(Dockerfile.PlainText plainText, P p) {
        Dockerfile.PlainText pt = plainText;
        pt = pt.withPrefix(visitSpace(pt.getPrefix(), p));
        return pt.withMarkers(visitMarkers(pt.getMarkers(), p));
    }

    public Dockerfile visitQuotedString(Dockerfile.QuotedString quotedString, P p) {
        Dockerfile.QuotedString qs = quotedString;
        qs = qs.withPrefix(visitSpace(qs.getPrefix(), p));
        return qs.withMarkers(visitMarkers(qs.getMarkers(), p));
    }

    public Dockerfile visitEnvironmentVariable(Dockerfile.EnvironmentVariable environmentVariable, P p) {
        Dockerfile.EnvironmentVariable ev = environmentVariable;
        ev = ev.withPrefix(visitSpace(ev.getPrefix(), p));
        return ev.withMarkers(visitMarkers(ev.getMarkers(), p));
    }
}
