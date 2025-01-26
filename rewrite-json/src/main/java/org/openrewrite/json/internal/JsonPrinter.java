/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.json.internal;

import org.openrewrite.Cursor;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.json.JsonVisitor;
import org.openrewrite.json.tree.Comment;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonRightPadded;
import org.openrewrite.json.tree.Space;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.util.List;
import java.util.function.UnaryOperator;

public class JsonPrinter<P> extends JsonVisitor<PrintOutputCapture<P>> {

    @Override
    public Json visitArray(Json.Array array, PrintOutputCapture<P> p) {
        beforeSyntax(array, p);
        p.append('[');
        visitRightPadded(array.getPadding().getValues(), ",", p);
        p.append(']');
        afterSyntax(array, p);
        return array;
    }

    @Override
    public Json visitDocument(Json.Document document, PrintOutputCapture<P> p) {
        beforeSyntax(document, p);
        visit(document.getValue(), p);
        visitSpace(document.getEof(), p);
        afterSyntax(document, p);
        return document;
    }

    @Override
    public Json visitEmpty(Json.Empty empty, PrintOutputCapture<P> p) {
        beforeSyntax(empty, p);
        afterSyntax(empty, p);
        return empty;
    }

    @Override
    public Json visitIdentifier(Json.Identifier ident, PrintOutputCapture<P> p) {
        beforeSyntax(ident, p);
        p.append(ident.getName());
        afterSyntax(ident, p);
        return ident;
    }

    @Override
    public Json visitLiteral(Json.Literal literal, PrintOutputCapture<P> p) {
        beforeSyntax(literal, p);
        p.append(literal.getSource());
        afterSyntax(literal, p);
        return literal;
    }

    @Override
    public Json visitMember(Json.Member member, PrintOutputCapture<P> p) {
        beforeSyntax(member, p);
        visitRightPadded(member.getPadding().getKey(), p);
        p.append(':');
        visit(member.getValue(), p);
        afterSyntax(member, p);
        return member;
    }

    @Override
    public Json visitObject(Json.JsonObject obj, PrintOutputCapture<P> p) {
        beforeSyntax(obj, p);
        p.append('{');
        visitRightPadded(obj.getPadding().getMembers(), ",", p);
        p.append('}');
        afterSyntax(obj, p);
        return obj;
    }

    @Override
    public Space visitSpace(Space space, PrintOutputCapture<P> p) {
        p.append(space.getWhitespace());

        for (Comment comment : space.getComments()) {
            visitMarkers(comment.getMarkers(), p);
            if (comment.isMultiline()) {
                p.append("/*").append(comment.getText()).append("*/");
            } else {
                p.append("//").append(comment.getText());
            }
            p.append(comment.getSuffix());
        }
        return space;
    }

    protected void visitRightPadded(List<? extends JsonRightPadded<? extends Json>> nodes, String suffixBetween, PrintOutputCapture<P> p) {
        for (int i = 0; i < nodes.size(); i++) {
            JsonRightPadded<? extends Json> node = nodes.get(i);
            visit(node.getElement(), p);
            visitSpace(node.getAfter(), p);
            if (i < nodes.size() - 1) {
                p.append(suffixBetween);
            }
        }
    }

    private static final UnaryOperator<String> JSON_MARKER_WRAPPER =
            out -> "/*~~" + out + (out.isEmpty() ? "" : "~~") + ">*/";

    private void beforeSyntax(Json j, PrintOutputCapture<P> p) {
        beforeSyntax(j.getPrefix(), j.getMarkers(), p);
    }

    private void beforeSyntax(Space prefix, Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforePrefix(marker, new Cursor(getCursor(), marker), JSON_MARKER_WRAPPER));
        }
        visitSpace(prefix, p);
        visitMarkers(markers, p);
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().beforeSyntax(marker, new Cursor(getCursor(), marker), JSON_MARKER_WRAPPER));
        }
    }

    private void afterSyntax(Json j, PrintOutputCapture<P> p) {
        afterSyntax(j.getMarkers(), p);
    }

    private void afterSyntax(Markers markers, PrintOutputCapture<P> p) {
        for (Marker marker : markers.getMarkers()) {
            p.append(p.getMarkerPrinter().afterSyntax(marker, new Cursor(getCursor(), marker), JSON_MARKER_WRAPPER));
        }
    }
}
