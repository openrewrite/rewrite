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
import org.openrewrite.Tree;
import org.openrewrite.TreePrinter;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.json.JsonVisitor;
import org.openrewrite.json.tree.*;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.util.List;

public class JsonPrinter<P> extends JsonVisitor<P> {

    private static final String PRINTER_ACC_KEY = "printed";

    private final TreePrinter<P> treePrinter;

    public JsonPrinter(TreePrinter<P> treePrinter) {
        this.treePrinter = treePrinter;
    }

    @NonNull
    protected StringBuilder getPrinter() {
        StringBuilder acc = getCursor().getRoot().getMessage(PRINTER_ACC_KEY);
        if (acc == null) {
            acc = new StringBuilder();
            getCursor().getRoot().putMessage(PRINTER_ACC_KEY, acc);
        }
        return acc;
    }

    public String print(Json json, P p) {
        setCursor(new Cursor(null, "EPSILON"));
        visit(json, p);
        return getPrinter().toString();
    }

    @Override
    @Nullable
    public Json visit(@Nullable Tree tree, P p) {
        if (tree == null) {
            return defaultValue(null, p);
        }

        StringBuilder printerAcc = getPrinter();
        treePrinter.doBefore(tree, printerAcc, p);
        tree = super.visit(tree, p);
        if (tree != null) {
            treePrinter.doAfter(tree, printerAcc, p);
        }
        return (Json) tree;
    }

    public void visit(@Nullable List<? extends Json> nodes, P p) {
        if (nodes != null) {
            for (Json node : nodes) {
                visit(node, p);
            }
        }
    }

    @Override
    public Json visitArray(Json.Array array, P p) {
        visitSpace(array.getPrefix(), p);
        visitMarkers(array.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append('[');
        visitRightPadded(array.getPadding().getValues(), ",", p);
        acc.append(']');
        return array;
    }

    @Override
    public Json visitDocument(Json.Document document, P p) {
        visitSpace(document.getPrefix(), p);
        visitMarkers(document.getMarkers(), p);
        visit(document.getValue(), p);
        visitSpace(document.getEof(), p);
        return document;
    }

    @Override
    public Json visitEmpty(Json.Empty empty, P p) {
        visitSpace(empty.getPrefix(), p);
        visitMarkers(empty.getMarkers(), p);
        return empty;
    }

    @Override
    public Json visitIdentifier(Json.Identifier ident, P p) {
        StringBuilder acc = getPrinter();
        visitSpace(ident.getPrefix(), p);
        visitMarkers(ident.getMarkers(), p);
        acc.append(ident.getName());
        return ident;
    }

    @Override
    public Json visitLiteral(Json.Literal literal, P p) {
        StringBuilder acc = getPrinter();
        visitSpace(literal.getPrefix(), p);
        visitMarkers(literal.getMarkers(), p);
        acc.append(literal.getSource());
        return literal;
    }

    @Override
    public Json visitMember(Json.Member member, P p) {
        StringBuilder acc = getPrinter();
        visitSpace(member.getPrefix(), p);
        visitMarkers(member.getMarkers(), p);
        visitRightPadded(member.getPadding().getKey(), p);
        acc.append(':');
        visit(member.getValue(), p);
        return member;
    }

    @Override
    public Json visitObject(Json.JsonObject obj, P p) {
        visitSpace(obj.getPrefix(), p);
        visitMarkers(obj.getMarkers(), p);
        StringBuilder acc = getPrinter();
        acc.append('{');
        visitRightPadded(obj.getPadding().getMembers(), ",", p);
        acc.append('}');
        return obj;
    }

    @Override
    public <M extends Marker> M visitMarker(Marker marker, P p) {
        StringBuilder acc = getPrinter();
        treePrinter.doBefore(marker, acc, p);
        acc.append(marker.print(treePrinter, p));
        treePrinter.doAfter(marker, acc, p);
        //noinspection unchecked
        return (M) marker;
    }

    @Override
    public Markers visitMarkers(Markers markers, P p) {
        StringBuilder acc = getPrinter();
        treePrinter.doBefore(markers, acc, p);
        Markers m = super.visitMarkers(markers, p);
        treePrinter.doAfter(markers, acc, p);
        return m;
    }

    public Space visitSpace(Space space, P p) {
        StringBuilder acc = getPrinter();
        acc.append(space.getWhitespace());

        for (Comment comment : space.getComments()) {
            visitMarkers(comment.getMarkers(), p);
            if (comment.isMultiline()) {
                acc.append("/*").append(comment.getText()).append("*/");
            } else {
                acc.append("//").append(comment.getText());
            }
            acc.append(comment.getSuffix());
        }
        return space;
    }

    protected void visitRightPadded(List<? extends JsonRightPadded<? extends Json>> nodes, String suffixBetween, P p) {
        StringBuilder acc = getPrinter();
        for (int i = 0; i < nodes.size(); i++) {
            JsonRightPadded<? extends Json> node = nodes.get(i);
            visit(node.getElement(), p);
            visitSpace(node.getAfter(), p);
            if (i < nodes.size() - 1) {
                acc.append(suffixBetween);
            }
        }
    }
}
