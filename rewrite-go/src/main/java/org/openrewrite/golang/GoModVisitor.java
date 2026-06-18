/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.golang;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.golang.tree.GoMod;
import org.openrewrite.golang.tree.GoModTree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

/**
 * Java-side visitor for the {@code go.mod} LST, analogous to {@link GolangVisitor}
 * for {@code .go} sources and {@code HclVisitor} for HCL. It traverses the bespoke
 * {@link GoMod} node set (directives, blocks, values) so recipes can inspect and
 * rewrite go.mod entirely in Java; printing still goes through the Go RPC server.
 */
@SuppressWarnings("unused")
public class GoModVisitor<P> extends TreeVisitor<GoModTree, P> {

    @Override
    public boolean isAcceptable(SourceFile sourceFile, P p) {
        return sourceFile instanceof GoMod;
    }

    @Override
    public String getLanguage() {
        return "gomod";
    }

    public GoModTree visitGoMod(GoMod goMod, P p) {
        GoMod g = goMod;
        g = g.withPrefix(visitSpace(g.getPrefix(), p));
        g = g.withMarkers(visitMarkers(g.getMarkers(), p));
        g = g.withStatements(ListUtils.map(g.getStatements(), s -> visitRightPadded(s, p)));
        g = g.withEof(visitSpace(g.getEof(), p));
        return g;
    }

    public GoModTree visitDirective(GoMod.Directive directive, P p) {
        GoMod.Directive d = directive;
        d = d.withPrefix(visitSpace(d.getPrefix(), p));
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        d = d.withValues(ListUtils.map(d.getValues(), v -> (GoMod.Value) visit(v, p)));
        return d;
    }

    public GoModTree visitBlock(GoMod.Block block, P p) {
        GoMod.Block b = block;
        b = b.withPrefix(visitSpace(b.getPrefix(), p));
        b = b.withMarkers(visitMarkers(b.getMarkers(), p));
        b = b.withBeforeLParen(visitSpace(b.getBeforeLParen(), p));
        b = b.withEntries(ListUtils.map(b.getEntries(), e -> visitRightPadded(e, p)));
        b = b.withBeforeRParen(visitSpace(b.getBeforeRParen(), p));
        return b;
    }

    public GoModTree visitValue(GoMod.Value value, P p) {
        GoMod.Value v = value;
        v = v.withPrefix(visitSpace(v.getPrefix(), p));
        v = v.withMarkers(visitMarkers(v.getMarkers(), p));
        return v;
    }

    /**
     * go.mod {@link Space} carries no location taxonomy (unlike {@code J}/HCL), so
     * this is a plain identity hook for subclasses to override.
     */
    public Space visitSpace(Space space, P p) {
        return space;
    }

    public <T extends GoModTree> @Nullable JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right, P p) {
        if (right == null) {
            return null;
        }
        pushCursor(right);
        T t = visitAndCast(right.getElement(), p);
        popCursor();
        if (t == null) {
            return null;
        }
        Space after = visitSpace(right.getAfter(), p);
        Markers markers = visitMarkers(right.getMarkers(), p);
        return after == right.getAfter() && t == right.getElement() && markers == right.getMarkers() ?
                right : new JRightPadded<>(t, after, markers);
    }
}
