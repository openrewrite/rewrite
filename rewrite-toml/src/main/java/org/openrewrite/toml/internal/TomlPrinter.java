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
package org.openrewrite.toml.internal;

import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;
import org.openrewrite.toml.TomlVisitor;
import org.openrewrite.toml.tree.Space;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlRightPadded;

import java.util.List;
import java.util.function.UnaryOperator;

public class TomlPrinter<P> extends TomlVisitor<PrintOutputCapture<P>> {
    private static final UnaryOperator<String> MARKER_WRAPPER =
            out -> "/*~~" + out + (out.isEmpty() ? "" : "~~") + ">*/";

    protected void beforeSyntax(Toml t, PrintOutputCapture<P> p) {
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

    protected void afterSyntax(Toml t, PrintOutputCapture<P> p) {
        afterSyntax(t.getMarkers(), p);
    }

    protected void afterSyntax(Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().afterSyntax(marker, new Cursor(getCursor(), marker), MARKER_WRAPPER));
        }
    }

    @Override
    public Toml visitDocument(Toml.Document document, PrintOutputCapture<P> p) {
        beforeSyntax(document, p);
        visit(document.getExpressions(), p);
        visitSpace(document.getEof(), p);
        afterSyntax(document, p);
        return document;
    }

    @Override
    public Toml visitExpression(Toml.Expression expression, PrintOutputCapture<P> p) {
        beforeSyntax(expression, p);
        visit(expression.getValue(), p);
        visitComment(expression.getComment(), p);
        afterSyntax(expression, p);
        return expression;
    }

    @Override
    public Toml visitKey(Toml.Key key, PrintOutputCapture<P> p) {
        beforeSyntax(key, p);
        p.append(key.getName());
        afterSyntax(key, p);
        return key;
    }

//    @Override
//    public Toml visitValue(TomlValue value, PrintOutputCapture<P> p) {
//        beforeSyntax(value, p);
//        p.append(value.get());
//        afterSyntax(value, p);
//        return value;
//    }

    @Override
    public Toml visitLiteral(Toml.Literal literal, PrintOutputCapture<P> p) {
        beforeSyntax(literal, p);
        p.append(literal.getSource());
        afterSyntax(literal, p);
        return literal;
    }

    @Override
    public Toml visitArray(Toml.Array array, PrintOutputCapture<P> p) {
        beforeSyntax(array, p);
        p.append('[');
        visitRightPadded(array.getPadding().getValues(), ",", p);
        p.append(']');
        afterSyntax(array, p);
        return array;
    }

    @Override
    public Toml visitTable(Toml.Table obj, PrintOutputCapture<P> p) {
        beforeSyntax(obj, p);
        p.append('{');
        visitRightPadded(obj.getPadding().getEntries(), ",", p);
        p.append('}');
        afterSyntax(obj, p);
        return obj;
    }

    @Override
    public Space visitSpace(Space space, PrintOutputCapture<P> p) {
        p.append(space.getWhitespace());

//        for (Comment comment : space.getComments()) {
//            visitMarkers(comment.getMarkers(), p);
//            if (comment.isMultiline()) {
//                p.append("/*").append(comment.getText()).append("*/");
//            } else {
//                p.append("//").append(comment.getText());
//            }
//            p.append(comment.getSuffix());
//        }
        return space;
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
}
