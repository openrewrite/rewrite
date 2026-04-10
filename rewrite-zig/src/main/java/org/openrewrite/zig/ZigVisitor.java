/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.zig;

import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.zig.tree.Zig;

public class ZigVisitor<P> extends JavaVisitor<P> {

    @Override
    public boolean isAcceptable(SourceFile sourceFile, P p) {
        return sourceFile instanceof Zig.CompilationUnit;
    }

    @Override
    public String getLanguage() {
        return "zig";
    }

    // ---------------------------------------------------------------
    // Zig-specific visit methods
    // ---------------------------------------------------------------

    public J visitZigCompilationUnit(Zig.CompilationUnit cu, P p) {
        Zig.CompilationUnit c = cu;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.COMPILATION_UNIT_PREFIX, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.getPadding().withImports(ListUtils.map(c.getPadding().getImports(),
                el -> visitRightPadded(el, JRightPadded.Location.IMPORT, p)));
        c = c.getPadding().withStatements(visitStatements(c.getPadding().getStatements(), p));
        c = c.withEof(visitSpace(c.getEof(), Space.Location.COMPILATION_UNIT_EOF, p));
        return c;
    }

    private <T extends Statement> java.util.List<JRightPadded<T>> visitStatements(
            java.util.List<JRightPadded<T>> statements, P p) {
        java.util.List<JRightPadded<T>> s = statements;
        for (int i = 0; i < s.size(); i++) {
            JRightPadded<T> rp = s.get(i);
            @SuppressWarnings("unchecked")
            T elem = (T) visitAndCast(rp.getElement(), p);
            if (elem != rp.getElement()) {
                if (s == statements) {
                    s = new java.util.ArrayList<>(statements);
                }
                s.set(i, rp.withElement(elem));
            }
        }
        return s;
    }

    public J visitComptime(Zig.Comptime comptime, P p) {
        Zig.Comptime c = comptime;
        c = c.withPrefix(visitSpace(c.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        c = c.withExpression((Expression) visitAndCast(c.getExpression(), p));
        return c;
    }

    public J visitDefer(Zig.Defer defer, P p) {
        Zig.Defer d = defer;
        d = d.withPrefix(visitSpace(d.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        if (d.getPayload() != null) {
            d = d.withPayload((Zig.Payload) visitAndCast(d.getPayload(), p));
        }
        d = d.withExpression((Expression) visitAndCast(d.getExpression(), p));
        return d;
    }

    public J visitTestDecl(Zig.TestDecl testDecl, P p) {
        Zig.TestDecl t = testDecl;
        t = t.withPrefix(visitSpace(t.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        t = t.withName((J.Literal) visitAndCast(t.getName(), p));
        t = t.withBody((J.Block) visitAndCast(t.getBody(), p));
        return t;
    }

    public J visitBuiltinCall(Zig.BuiltinCall builtinCall, P p) {
        Zig.BuiltinCall b = builtinCall;
        b = b.withPrefix(visitSpace(b.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        b = b.withMarkers(visitMarkers(b.getMarkers(), p));
        b = b.withName((J.Identifier) visitAndCast(b.getName(), p));
        b = b.getPadding().withArguments(visitContainer(b.getPadding().getArguments(), JContainer.Location.LANGUAGE_EXTENSION, p));
        return b;
    }

    public J visitPayload(Zig.Payload payload, P p) {
        Zig.Payload pl = payload;
        pl = pl.withPrefix(visitSpace(pl.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        pl = pl.withMarkers(visitMarkers(pl.getMarkers(), p));
        pl = pl.getPadding().withNames(visitContainer(pl.getPadding().getNames(), JContainer.Location.LANGUAGE_EXTENSION, p));
        return pl;
    }

    public J visitErrorUnion(Zig.ErrorUnion errorUnion, P p) {
        Zig.ErrorUnion e = errorUnion;
        e = e.withPrefix(visitSpace(e.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        if (e.getErrorType() != null) {
            e = e.withErrorType((Expression) visitAndCast(e.getErrorType(), p));
        }
        e = e.getPadding().withValueType(visitLeftPadded(e.getPadding().getValueType(), JLeftPadded.Location.LANGUAGE_EXTENSION, p));
        return e;
    }

    public J visitOptional(Zig.Optional optional, P p) {
        Zig.Optional o = optional;
        o = o.withPrefix(visitSpace(o.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        o = o.withMarkers(visitMarkers(o.getMarkers(), p));
        o = o.withValueType((Expression) visitAndCast(o.getValueType(), p));
        return o;
    }

    public J visitSlice(Zig.Slice slice, P p) {
        Zig.Slice s = slice;
        s = s.withPrefix(visitSpace(s.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        s = s.withMarkers(visitMarkers(s.getMarkers(), p));
        s = s.withTarget((Expression) visitAndCast(s.getTarget(), p));
        s = s.withOpenBracket(visitSpace(s.getOpenBracket(), Space.Location.LANGUAGE_EXTENSION, p));
        if (s.getPadding().getStart() != null) {
            s = s.getPadding().withStart(visitRightPadded(s.getPadding().getStart(), JRightPadded.Location.LANGUAGE_EXTENSION, p));
        }
        if (s.getEnd() != null) {
            s = s.withEnd((Expression) visitAndCast(s.getEnd(), p));
        }
        s = s.withCloseBracket(visitSpace(s.getCloseBracket(), Space.Location.LANGUAGE_EXTENSION, p));
        return s;
    }

    public J visitSwitchProng(Zig.SwitchProng switchProng, P p) {
        Zig.SwitchProng sp = switchProng;
        sp = sp.withPrefix(visitSpace(sp.getPrefix(), Space.Location.LANGUAGE_EXTENSION, p));
        sp = sp.withMarkers(visitMarkers(sp.getMarkers(), p));
        sp = sp.getPadding().withCases(visitContainer(sp.getPadding().getCases(), JContainer.Location.LANGUAGE_EXTENSION, p));
        if (sp.getPayload() != null) {
            sp = sp.withPayload((Zig.Payload) visitAndCast(sp.getPayload(), p));
        }
        sp = sp.getPadding().withArrow(visitLeftPadded(sp.getPadding().getArrow(), JLeftPadded.Location.LANGUAGE_EXTENSION, p));
        return sp;
    }
}
