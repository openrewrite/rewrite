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
package org.openrewrite.bash;

import org.jspecify.annotations.Nullable;
import org.openrewrite.TreeVisitor;
import org.openrewrite.bash.tree.Bash;
import org.openrewrite.bash.tree.Space;
import org.openrewrite.internal.ListUtils;

public class BashVisitor<P> extends TreeVisitor<Bash, P> {

    public Space visitSpace(Space space, P p) {
        return space;
    }

    public Bash visitScript(Bash.Script script, P p) {
        Bash.Script s = script;
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        if (s.getShebang() != null) {
            s = s.withShebang((Bash.Shebang) visit(s.getShebang(), p));
        }
        s = s.withStatements(ListUtils.map(s.getStatements(), stmt -> (Bash.Statement) visit(stmt, p)));
        return s;
    }

    public Bash visitShebang(Bash.Shebang shebang, P p) {
        Bash.Shebang s = shebang;
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        return s.withMarkers(visitMarkers(s.getMarkers(), p));
    }

    public Bash visitCommand(Bash.Command command, P p) {
        Bash.Command c = command;
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.withAssignments(ListUtils.map(c.getAssignments(), a -> (Bash.Assignment) visit(a, p)));
        c = c.withArguments(ListUtils.map(c.getArguments(), a -> (Bash.Expression) visit(a, p)));
        return c;
    }

    public Bash visitPipeline(Bash.Pipeline pipeline, P p) {
        Bash.Pipeline pl = pipeline;
        pl = pl.withPrefix(visitSpace(pl.getPrefix(), p));
        pl = pl.withMarkers(visitMarkers(pl.getMarkers(), p));
        pl = pl.withCommands(ListUtils.map(pl.getCommands(), cmd -> (Bash.Statement) visit(cmd, p)));
        pl = pl.withPipeOperators(ListUtils.map(pl.getPipeOperators(), op -> op.withPrefix(visitSpace(op.getPrefix(), p))));
        return pl;
    }

    public Bash visitCommandList(Bash.CommandList commandList, P p) {
        Bash.CommandList cl = commandList;
        cl = cl.withPrefix(visitSpace(cl.getPrefix(), p));
        cl = cl.withMarkers(visitMarkers(cl.getMarkers(), p));
        cl = cl.withCommands(ListUtils.map(cl.getCommands(), cmd -> (Bash.Statement) visit(cmd, p)));
        cl = cl.withOperators(ListUtils.map(cl.getOperators(), op -> op.withPrefix(visitSpace(op.getPrefix(), p))));
        return cl;
    }

    public Bash visitAssignment(Bash.Assignment assignment, P p) {
        Bash.Assignment a = assignment;
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        a = a.withName((Bash.Literal) visit(a.getName(), p));
        if (a.getValue() != null) {
            a = a.withValue((Bash.Expression) visit(a.getValue(), p));
        }
        return a;
    }

    public Bash visitIfStatement(Bash.IfStatement ifStmt, P p) {
        Bash.IfStatement i = ifStmt;
        i = i.withPrefix(visitSpace(i.getPrefix(), p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        i = i.withCondition(ListUtils.map(i.getCondition(), s -> (Bash.Statement) visit(s, p)));
        i = i.withThenBody(ListUtils.map(i.getThenBody(), s -> (Bash.Statement) visit(s, p)));
        i = i.withElifs(ListUtils.map(i.getElifs(), e -> (Bash.Elif) visit(e, p)));
        if (i.getElseClause() != null) {
            i = i.withElseClause((Bash.Else) visit(i.getElseClause(), p));
        }
        return i;
    }

    public Bash visitElif(Bash.Elif elif, P p) {
        Bash.Elif e = elif;
        e = e.withPrefix(visitSpace(e.getPrefix(), p));
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        e = e.withCondition(ListUtils.map(e.getCondition(), s -> (Bash.Statement) visit(s, p)));
        e = e.withBody(ListUtils.map(e.getBody(), s -> (Bash.Statement) visit(s, p)));
        return e;
    }

    public Bash visitElse(Bash.Else elseClause, P p) {
        Bash.Else e = elseClause;
        e = e.withPrefix(visitSpace(e.getPrefix(), p));
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        e = e.withBody(ListUtils.map(e.getBody(), s -> (Bash.Statement) visit(s, p)));
        return e;
    }

    public Bash visitForLoop(Bash.ForLoop forLoop, P p) {
        Bash.ForLoop f = forLoop;
        f = f.withPrefix(visitSpace(f.getPrefix(), p));
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        f = f.withBody(ListUtils.map(f.getBody(), s -> (Bash.Statement) visit(s, p)));
        return f;
    }

    public Bash visitCStyleForLoop(Bash.CStyleForLoop forLoop, P p) {
        Bash.CStyleForLoop f = forLoop;
        f = f.withPrefix(visitSpace(f.getPrefix(), p));
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        f = f.withBody(ListUtils.map(f.getBody(), s -> (Bash.Statement) visit(s, p)));
        return f;
    }

    public Bash visitWhileLoop(Bash.WhileLoop whileLoop, P p) {
        Bash.WhileLoop w = whileLoop;
        w = w.withPrefix(visitSpace(w.getPrefix(), p));
        w = w.withMarkers(visitMarkers(w.getMarkers(), p));
        w = w.withCondition(ListUtils.map(w.getCondition(), s -> (Bash.Statement) visit(s, p)));
        w = w.withBody(ListUtils.map(w.getBody(), s -> (Bash.Statement) visit(s, p)));
        return w;
    }

    public Bash visitCaseStatement(Bash.CaseStatement caseStmt, P p) {
        Bash.CaseStatement c = caseStmt;
        c = c.withPrefix(visitSpace(c.getPrefix(), p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.withWord((Bash.Expression) visit(c.getWord(), p));
        c = c.withItems(ListUtils.map(c.getItems(), item -> (Bash.CaseItem) visit(item, p)));
        return c;
    }

    public Bash visitCaseItem(Bash.CaseItem caseItem, P p) {
        Bash.CaseItem ci = caseItem;
        ci = ci.withPrefix(visitSpace(ci.getPrefix(), p));
        ci = ci.withMarkers(visitMarkers(ci.getMarkers(), p));
        ci = ci.withBody(ListUtils.map(ci.getBody(), s -> (Bash.Statement) visit(s, p)));
        return ci;
    }

    public Bash visitFunction(Bash.Function function, P p) {
        Bash.Function f = function;
        f = f.withPrefix(visitSpace(f.getPrefix(), p));
        f = f.withMarkers(visitMarkers(f.getMarkers(), p));
        f = f.withBody((Bash.Statement) visit(f.getBody(), p));
        return f;
    }

    public Bash visitSubshell(Bash.Subshell subshell, P p) {
        Bash.Subshell s = subshell;
        s = s.withPrefix(visitSpace(s.getPrefix(), p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.withBody(ListUtils.map(s.getBody(), stmt -> (Bash.Statement) visit(stmt, p)));
        return s;
    }

    public Bash visitBraceGroup(Bash.BraceGroup braceGroup, P p) {
        Bash.BraceGroup bg = braceGroup;
        bg = bg.withPrefix(visitSpace(bg.getPrefix(), p));
        bg = bg.withMarkers(visitMarkers(bg.getMarkers(), p));
        bg = bg.withBody(ListUtils.map(bg.getBody(), stmt -> (Bash.Statement) visit(stmt, p)));
        return bg;
    }

    public Bash visitRedirect(Bash.Redirect redirect, P p) {
        Bash.Redirect r = redirect;
        r = r.withPrefix(visitSpace(r.getPrefix(), p));
        return r.withMarkers(visitMarkers(r.getMarkers(), p));
    }

    public Bash visitHereDoc(Bash.HereDoc hereDoc, P p) {
        Bash.HereDoc h = hereDoc;
        h = h.withPrefix(visitSpace(h.getPrefix(), p));
        return h.withMarkers(visitMarkers(h.getMarkers(), p));
    }

    public Bash visitHereString(Bash.HereString hereString, P p) {
        Bash.HereString h = hereString;
        h = h.withPrefix(visitSpace(h.getPrefix(), p));
        h = h.withMarkers(visitMarkers(h.getMarkers(), p));
        h = h.withWord((Bash.Expression) visit(h.getWord(), p));
        return h;
    }

    public Bash visitWord(Bash.Word word, P p) {
        Bash.Word w = word;
        w = w.withPrefix(visitSpace(w.getPrefix(), p));
        w = w.withMarkers(visitMarkers(w.getMarkers(), p));
        w = w.withParts(ListUtils.map(w.getParts(), part -> (Bash.Expression) visit(part, p)));
        return w;
    }

    public Bash visitLiteral(Bash.Literal literal, P p) {
        Bash.Literal l = literal;
        l = l.withPrefix(visitSpace(l.getPrefix(), p));
        return l.withMarkers(visitMarkers(l.getMarkers(), p));
    }

    public Bash visitSingleQuoted(Bash.SingleQuoted singleQuoted, P p) {
        Bash.SingleQuoted sq = singleQuoted;
        sq = sq.withPrefix(visitSpace(sq.getPrefix(), p));
        return sq.withMarkers(visitMarkers(sq.getMarkers(), p));
    }

    public Bash visitDoubleQuoted(Bash.DoubleQuoted doubleQuoted, P p) {
        Bash.DoubleQuoted dq = doubleQuoted;
        dq = dq.withPrefix(visitSpace(dq.getPrefix(), p));
        dq = dq.withMarkers(visitMarkers(dq.getMarkers(), p));
        dq = dq.withParts(ListUtils.map(dq.getParts(), part -> (Bash.Expression) visit(part, p)));
        return dq;
    }

    public Bash visitDollarSingleQuoted(Bash.DollarSingleQuoted dsq, P p) {
        Bash.DollarSingleQuoted d = dsq;
        d = d.withPrefix(visitSpace(d.getPrefix(), p));
        return d.withMarkers(visitMarkers(d.getMarkers(), p));
    }

    public Bash visitVariableExpansion(Bash.VariableExpansion variableExpansion, P p) {
        Bash.VariableExpansion ve = variableExpansion;
        ve = ve.withPrefix(visitSpace(ve.getPrefix(), p));
        return ve.withMarkers(visitMarkers(ve.getMarkers(), p));
    }

    public Bash visitCommandSubstitution(Bash.CommandSubstitution commandSubstitution, P p) {
        Bash.CommandSubstitution cs = commandSubstitution;
        cs = cs.withPrefix(visitSpace(cs.getPrefix(), p));
        cs = cs.withMarkers(visitMarkers(cs.getMarkers(), p));
        cs = cs.withBody(ListUtils.map(cs.getBody(), s -> (Bash.Statement) visit(s, p)));
        return cs;
    }

    public Bash visitArithmeticExpansion(Bash.ArithmeticExpansion arithmeticExpansion, P p) {
        Bash.ArithmeticExpansion ae = arithmeticExpansion;
        ae = ae.withPrefix(visitSpace(ae.getPrefix(), p));
        return ae.withMarkers(visitMarkers(ae.getMarkers(), p));
    }

    public Bash visitProcessSubstitution(Bash.ProcessSubstitution processSubstitution, P p) {
        Bash.ProcessSubstitution ps = processSubstitution;
        ps = ps.withPrefix(visitSpace(ps.getPrefix(), p));
        ps = ps.withMarkers(visitMarkers(ps.getMarkers(), p));
        ps = ps.withBody(ListUtils.map(ps.getBody(), s -> (Bash.Statement) visit(s, p)));
        return ps;
    }

    public Bash visitConditionalExpression(Bash.ConditionalExpression condExpr, P p) {
        Bash.ConditionalExpression ce = condExpr;
        ce = ce.withPrefix(visitSpace(ce.getPrefix(), p));
        return ce.withMarkers(visitMarkers(ce.getMarkers(), p));
    }

    public Bash visitRedirected(Bash.Redirected redirected, P p) {
        Bash.Redirected r = redirected;
        r = r.withPrefix(visitSpace(r.getPrefix(), p));
        r = r.withMarkers(visitMarkers(r.getMarkers(), p));
        r = r.withCommand((Bash.Statement) visit(r.getCommand(), p));
        r = r.withRedirections(ListUtils.map(r.getRedirections(), e -> (Bash.Expression) visit(e, p)));
        return r;
    }

    public Bash visitBackground(Bash.Background background, P p) {
        Bash.Background bg = background;
        bg = bg.withPrefix(visitSpace(bg.getPrefix(), p));
        bg = bg.withMarkers(visitMarkers(bg.getMarkers(), p));
        bg = bg.withCommand((Bash.Statement) visit(bg.getCommand(), p));
        bg = bg.withAmpersandPrefix(visitSpace(bg.getAmpersandPrefix(), p));
        return bg;
    }

    public Bash visitArrayLiteral(Bash.ArrayLiteral arrayLiteral, P p) {
        Bash.ArrayLiteral al = arrayLiteral;
        al = al.withPrefix(visitSpace(al.getPrefix(), p));
        al = al.withMarkers(visitMarkers(al.getMarkers(), p));
        al = al.withElements(ListUtils.map(al.getElements(), e -> (Bash.Expression) visit(e, p)));
        return al;
    }
}
