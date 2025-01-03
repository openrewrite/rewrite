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
package org.openrewrite.toml.internal;

import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;
import org.openrewrite.toml.TomlVisitor;
import org.openrewrite.toml.marker.ArrayTable;
import org.openrewrite.toml.marker.InlineTable;
import org.openrewrite.toml.tree.Comment;
import org.openrewrite.toml.tree.Space;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlRightPadded;

import java.util.List;
import java.util.function.UnaryOperator;

public class TomlPrinter<P> extends TomlVisitor<PrintOutputCapture<P>> {

    public Toml visitArray(Toml.Array array, PrintOutputCapture<P> p) {
        beforeSyntax(array, p);
        p.append("[");
        visitRightPadded(array.getPadding().getValues(), ",", p);
        p.append("]");
        afterSyntax(array, p);
        return array;
    }

    public Toml visitDocument(Toml.Document document, PrintOutputCapture<P> p) {
        beforeSyntax(document, p);
        visit(document.getValues(), p);
        visitSpace(document.getEof(), p);
        afterSyntax(document, p);
        return document;
    }

    public Toml visitEmpty(Toml.Empty empty, PrintOutputCapture<P> p) {
        beforeSyntax(empty, p);
        afterSyntax(empty, p);
        return empty;
    }

    public Toml visitIdentifier(Toml.Identifier identifier, PrintOutputCapture<P> p) {
        beforeSyntax(identifier, p);
        p.append(identifier.getSource());
        afterSyntax(identifier, p);
        return identifier;
    }

    public Toml visitKeyValue(Toml.KeyValue keyValue, PrintOutputCapture<P> p) {
        beforeSyntax(keyValue, p);
        visitRightPadded(keyValue.getPadding().getKey(), p);
        p.append("=");
        visit(keyValue.getValue(), p);
        afterSyntax(keyValue, p);
        return keyValue;
    }

    public Toml visitLiteral(Toml.Literal literal, PrintOutputCapture<P> p) {
        beforeSyntax(literal, p);
        p.append(literal.getSource());
        afterSyntax(literal, p);
        return literal;
    }

    @Override
    public Space visitSpace(Space space, PrintOutputCapture<P> p) {
        p.append(space.getWhitespace());
        for (Comment comment : space.getComments()) {
            visitMarkers(comment.getMarkers(), p);
            p.append("#").append(comment.getText()).append(comment.getSuffix());
        }
        return space;
    }

    @Override
    public Toml visitTable(Toml.Table table, PrintOutputCapture<P> p) {
        beforeSyntax(table, p);
        if (table.getMarkers().findFirst(InlineTable.class).isPresent()) {
            p.append("{");
            visitRightPadded(table.getPadding().getValues(), ",", p);
            p.append("}");
        } else if (table.getMarkers().findFirst(ArrayTable.class).isPresent()) {
            p.append("[[");
            visitRightPadded(table.getName(), p);
            p.append("]]");
            visitRightPadded(table.getPadding().getValues(), "", p);
        } else {
            p.append("[");
            visitRightPadded(table.getName(), p);
            p.append("]");
            visitRightPadded(table.getPadding().getValues(), "", p);
        }
        afterSyntax(table, p);
        return table;
    }

    protected void visitRightPadded(List<? extends TomlRightPadded<? extends Toml>> nodes, String suffixBetween, PrintOutputCapture<P> p) {
        for (int i = 0; i < nodes.size(); i++) {
            TomlRightPadded<? extends Toml> node = nodes.get(i);
            visit(node.getElement(), p);
            visitSpace(node.getAfter(), p);
            if (i < nodes.size() - 1) {
                p.append(suffixBetween);
            }
        }
    }

    private static final UnaryOperator<String> TOML_MARKER_WRAPPER =
            out -> "~~" + out + (out.isEmpty() ? "" : "~~") + ">";

    protected void beforeSyntax(Toml t, PrintOutputCapture<P> p) {
        beforeSyntax(t.getPrefix(), t.getMarkers(), p);
    }

    protected void beforeSyntax(Space prefix, Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), TOML_MARKER_WRAPPER));
        }
        visitSpace(prefix, p);
        visitMarkers(markers, p);
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), TOML_MARKER_WRAPPER));
        }
    }

    protected void afterSyntax(Toml t, PrintOutputCapture<P> p) {
        afterSyntax(t.getMarkers(), p);
    }

    protected void afterSyntax(Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().afterSyntax(marker, new Cursor(getCursor(), marker), TOML_MARKER_WRAPPER));
        }
    }
}
