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

import org.openrewrite.PrintOutputCapture;
import org.openrewrite.json.JsonVisitor;
import org.openrewrite.json.tree.Comment;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonRightPadded;
import org.openrewrite.json.tree.Space;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.SearchResult;

import java.util.List;

public class JsonPrinter<P> extends JsonVisitor<PrintOutputCapture<P>> {

    @Override
    public Json visitArray(Json.Array array, PrintOutputCapture<P> p) {
        visitSpace(array.getPrefix(), p);
        visitMarkers(array.getMarkers(), p);
        p.out.append('[');
        visitRightPadded(array.getPadding().getValues(), ",", p);
        p.out.append(']');
        return array;
    }

    @Override
    public Json visitDocument(Json.Document document, PrintOutputCapture<P> p) {
        visitSpace(document.getPrefix(), p);
        visitMarkers(document.getMarkers(), p);
        visit(document.getValue(), p);
        visitSpace(document.getEof(), p);
        return document;
    }

    @Override
    public Json visitEmpty(Json.Empty empty, PrintOutputCapture<P> p) {
        visitSpace(empty.getPrefix(), p);
        visitMarkers(empty.getMarkers(), p);
        return empty;
    }

    @Override
    public Json visitIdentifier(Json.Identifier ident, PrintOutputCapture<P> p) {
        visitSpace(ident.getPrefix(), p);
        visitMarkers(ident.getMarkers(), p);
        p.out.append(ident.getName());
        return ident;
    }

    @Override
    public Json visitLiteral(Json.Literal literal, PrintOutputCapture<P> p) {
        visitSpace(literal.getPrefix(), p);
        visitMarkers(literal.getMarkers(), p);
        p.out.append(literal.getSource());
        return literal;
    }

    @Override
    public Json visitMember(Json.Member member, PrintOutputCapture<P> p) {
        visitSpace(member.getPrefix(), p);
        visitMarkers(member.getMarkers(), p);
        visitRightPadded(member.getPadding().getKey(), p);
        p.out.append(':');
        visit(member.getValue(), p);
        return member;
    }

    @Override
    public Json visitObject(Json.JsonObject obj, PrintOutputCapture<P> p) {
        visitSpace(obj.getPrefix(), p);
        visitMarkers(obj.getMarkers(), p);
        p.out.append('{');
        visitRightPadded(obj.getPadding().getMembers(), ",", p);
        p.out.append('}');
        return obj;
    }

    public Space visitSpace(Space space, PrintOutputCapture<P> p) {
        p.out.append(space.getWhitespace());

        for (Comment comment : space.getComments()) {
            visitMarkers(comment.getMarkers(), p);
            if (comment.isMultiline()) {
                p.out.append("/*").append(comment.getText()).append("*/");
            } else {
                p.out.append("//").append(comment.getText());
            }
            p.out.append(comment.getSuffix());
        }
        return space;
    }

    protected void visitRightPadded(List<? extends JsonRightPadded<? extends Json>> nodes, String suffixBetween, PrintOutputCapture<P> p) {
        for (int i = 0; i < nodes.size(); i++) {
            JsonRightPadded<? extends Json> node = nodes.get(i);
            visit(node.getElement(), p);
            visitSpace(node.getAfter(), p);
            if (i < nodes.size() - 1) {
                p.out.append(suffixBetween);
            }
        }
    }

    @Override
    public <M extends Marker> M visitMarker(Marker marker, PrintOutputCapture<P> p) {
        if (marker instanceof SearchResult) {
            String description = ((SearchResult) marker).getDescription();
            p.out.append("/*~~")
                    .append(description == null ? "" : "(" + description + ")~~")
                    .append(">*/");
        }
        //noinspection unchecked
        return (M) marker;
    }
}
