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
import org.openrewrite.golang.tree.GoSum;
import org.openrewrite.golang.tree.GoSumTree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

/**
 * Java-side visitor for the {@code go.sum} LST, analogous to {@link GoModVisitor}
 * for {@code go.mod}. It traverses the bespoke {@link GoSum} node set (the flat
 * list of hash lines) so recipes can inspect and rewrite go.sum entirely in Java;
 * printing still goes through the Go RPC server.
 */
@SuppressWarnings("unused")
public class GoSumVisitor<P> extends TreeVisitor<GoSumTree, P> {

    @Override
    public boolean isAcceptable(SourceFile sourceFile, P p) {
        return sourceFile instanceof GoSum;
    }

    @Override
    public String getLanguage() {
        return "gosum";
    }

    public GoSumTree visitGoSum(GoSum goSum, P p) {
        GoSum g = goSum;
        g = g.withPrefix(visitSpace(g.getPrefix(), p));
        g = g.withMarkers(visitMarkers(g.getMarkers(), p));
        g = g.withLines(ListUtils.map(g.getLines(), l -> visitRightPadded(l, p)));
        g = g.withEof(visitSpace(g.getEof(), p));
        return g;
    }

    public GoSumTree visitLine(GoSum.Line line, P p) {
        GoSum.Line l = line;
        l = l.withPrefix(visitSpace(l.getPrefix(), p));
        l = l.withMarkers(visitMarkers(l.getMarkers(), p));
        return l;
    }

    /**
     * go.sum {@link Space} carries no location taxonomy (unlike {@code J}), so
     * this is a plain identity hook for subclasses to override.
     */
    public Space visitSpace(Space space, P p) {
        return space;
    }

    public <T extends GoSumTree> @Nullable JRightPadded<T> visitRightPadded(@Nullable JRightPadded<T> right, P p) {
        if (right == null) {
            return null;
        }
        setCursor(new Cursor(getCursor(), right));
        T t = visitAndCast(right.getElement(), p);
        setCursor(getCursor().getParent());
        if (t == null) {
            return null;
        }
        Space after = visitSpace(right.getAfter(), p);
        Markers markers = visitMarkers(right.getMarkers(), p);
        return after == right.getAfter() && t == right.getElement() && markers == right.getMarkers() ?
                right : new JRightPadded<>(t, after, markers);
    }
}
