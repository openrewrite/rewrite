/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.ruby.internal;

import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;
import org.openrewrite.ruby.RubyVisitor;
import org.openrewrite.ruby.tree.Ruby;

import java.util.List;
import java.util.function.UnaryOperator;

public class RubyPrinter<P> extends RubyVisitor<PrintOutputCapture<P>> {
    private static final UnaryOperator<String> MARKER_WRAPPER =
            out -> "/*~~" + out + (out.isEmpty() ? "" : "~~") + ">*/";

    protected void beforeSyntax(Ruby t, PrintOutputCapture<P> p) {
        beforeSyntax(t.getPrefix(), t.getMarkers(), p);
    }

    protected void beforeSyntax(Space prefix, Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), MARKER_WRAPPER));
        }
        visitSpace(prefix, p);
        visitMarkers(markers, p);
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), MARKER_WRAPPER));
        }
    }

    protected void afterSyntax(Ruby t, PrintOutputCapture<P> p) {
        afterSyntax(t.getMarkers(), p);
    }

    protected void afterSyntax(Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().afterSyntax(marker, new Cursor(getCursor(), marker), MARKER_WRAPPER));
        }
    }

    protected void visitRightPadded(List<? extends JRightPadded<? extends J>> nodes, String suffixBetween, PrintOutputCapture<P> p) {
        for (int i = 0; i < nodes.size(); i++) {
            JRightPadded<? extends J> node = nodes.get(i);
            visit(node.getElement(), p);
            visitSpace(node.getAfter(), p);
            visitMarkers(node.getMarkers(), p);
            if (i < nodes.size() - 1) {
                p.append(suffixBetween);
            }
        }
    }

    protected void visitContainer(String before, @Nullable JContainer<? extends J> container,
                                  String suffixBetween, @Nullable String after, PrintOutputCapture<P> p) {
        if (container == null) {
            return;
        }
        beforeSyntax(container.getBefore(), container.getMarkers(), p);
        p.append(before);
        visitRightPadded(container.getPadding().getElements(), suffixBetween, p);
        afterSyntax(container.getMarkers(), p);
        p.append(after == null ? "" : after);
    }

    @Override
    public Space visitSpace(Space space, PrintOutputCapture<P> p) {
        p.append(space.getWhitespace());
        return space;
    }

    protected void visitLeftPadded(@Nullable String prefix, @Nullable JLeftPadded<? extends Ruby> leftPadded, PrintOutputCapture<P> p) {
        if (leftPadded != null) {
            beforeSyntax(leftPadded.getBefore(), leftPadded.getMarkers(), p);
            if (prefix != null) {
                p.append(prefix);
            }
            visit(leftPadded.getElement(), p);
            afterSyntax(leftPadded.getMarkers(), p);
        }
    }

    protected void visitRightPadded(@Nullable JRightPadded<? extends Ruby> rightPadded, @Nullable String suffix, PrintOutputCapture<P> p) {
        if (rightPadded != null) {
            beforeSyntax(Space.EMPTY, rightPadded.getMarkers(), p);
            visit(rightPadded.getElement(), p);
            afterSyntax(rightPadded.getMarkers(), p);
            visitSpace(rightPadded.getAfter(), p);
            if (suffix != null) {
                p.append(suffix);
            }
        }
    }

    public Ruby visitCompilationUnit(Ruby.CompilationUnit compilationUnit, PrintOutputCapture<P> p) {
        visitSpace(compilationUnit.getPrefix(), p);
        visitMarkers(compilationUnit.getMarkers(), p);
        visitContainer("", compilationUnit.getPadding().getExpressions(), "", "", p);
        return compilationUnit;
    }
}
