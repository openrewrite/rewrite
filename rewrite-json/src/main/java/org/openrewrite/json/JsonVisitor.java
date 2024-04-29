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
package org.openrewrite.json;

import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonRightPadded;
import org.openrewrite.json.tree.JsonValue;
import org.openrewrite.json.tree.Space;

public class JsonVisitor<P> extends TreeVisitor<Json, P> {

    @Override
    public boolean isAcceptable(SourceFile sourceFile, P p) {
        return sourceFile instanceof Json.Document;
    }

    @Override
    public String getLanguage() {
        return "json";
    }

    public Json visitArray(Json.Array array, P p) {
        Json.Array a = array;
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        a = a.withValues(ListUtils.map(a.getValues(), v -> (JsonValue) visit(v, p)));
        return a;
    }

    public Json visitDocument(Json.Document document, P p) {
        Json.Document d = document;
        d = d.withPrefix(visitSpace(d.getPrefix(), p));
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        d = d.withValue((JsonValue) visit(d.getValue(), p));
        d = d.withEof(visitSpace(d.getEof(), p));
        return d;
    }

    public Json visitEmpty(Json.Empty empty, P p) {
        Json.Empty e = empty;
        e = e.withPrefix(visitSpace(e.getPrefix(), p));
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        return e;
    }

    public Json visitIdentifier(Json.Identifier identifier, P p) {
        Json.Identifier i = identifier;
        i = i.withPrefix(visitSpace(i.getPrefix(), p));
        i = i.withMarkers(visitMarkers(i.getMarkers(), p));
        return i;
    }

    public Json visitLiteral(Json.Literal literal, P p) {
        Json.Literal l = literal;
        l = l.withPrefix(visitSpace(l.getPrefix(), p));
        l = l.withMarkers(visitMarkers(l.getMarkers(), p));
        return l;
    }

    public Json visitMember(Json.Member member, P p) {
        Json.Member m = member;
        m = m.withPrefix(visitSpace(m.getPrefix(), p));
        m = m.withMarkers(visitMarkers(m.getMarkers(), p));
        m = m.getPadding().withKey(visitRightPadded(m.getPadding().getKey(), p));
        m = m.withValue((JsonValue) visit(m.getValue(), p));
        return m;
    }

    public Json visitObject(Json.JsonObject obj, P p) {
        Json.JsonObject o = obj;
        o = o.withPrefix(visitSpace(o.getPrefix(), p));
        o = o.withMarkers(visitMarkers(o.getMarkers(), p));
        o = o.withMembers(ListUtils.map(o.getMembers(), m -> visit(m, p)));
        return o;
    }

    public Space visitSpace(Space space, P p) {
        return space;
    }

    @SuppressWarnings("ConstantConditions")
    public <T extends Json> JsonRightPadded<T> visitRightPadded(@Nullable JsonRightPadded<T> right, P p) {
        if (right == null) {
            //noinspection ConstantConditions
            return null;
        }

        setCursor(new Cursor(getCursor(), right));

        T t = right.getElement();
        if (t instanceof Json) {
            //noinspection unchecked
            t = (T) visit((Json) right.getElement(), p);
        }

        setCursor(getCursor().getParent());
        if (t == null) {
            //noinspection ConstantConditions
            return null;
        }
        Space after = visitSpace(right.getAfter(), p);
        return (after == right.getAfter() && t == right.getElement()) ? right : new JsonRightPadded<>(t, after, right.getMarkers());
    }
}
