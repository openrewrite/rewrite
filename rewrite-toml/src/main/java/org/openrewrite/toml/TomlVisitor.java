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
package org.openrewrite.toml;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.toml.tree.Space;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlRightPadded;
import org.openrewrite.toml.tree.TomlValue;

public class TomlVisitor<P> extends TreeVisitor<Toml, P> {

    @Override
    public boolean isAcceptable(SourceFile sourceFile, P p) {
        return sourceFile instanceof Toml.Document;
    }

    @Override
    public String getLanguage() {
        return "toml";
    }

    public Toml visitArray(Toml.Array array, P p) {
        Toml.Array a = array;
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        return a.withValues(ListUtils.map(a.getValues(), v -> visit(v, p)));
    }

    public Toml visitDocument(Toml.Document document, P p) {
        Toml.Document d = document;
        d = d.withPrefix(visitSpace(d.getPrefix(), p));
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        d = d.withValues(ListUtils.map(d.getValues(), v -> (TomlValue) visit(v, p)));
        return d.withEof(visitSpace(d.getEof(), p));
    }

    public Toml visitEmpty(Toml.Empty empty, P p) {
        Toml.Empty e = empty;
        e = e.withPrefix(visitSpace(e.getPrefix(), p));
        return e.withMarkers(visitMarkers(e.getMarkers(), p));
    }

    public Toml visitIdentifier(Toml.Identifier identifier, P p) {
        Toml.Identifier i = identifier;
        i = i.withPrefix(visitSpace(i.getPrefix(), p));
        return i.withMarkers(visitMarkers(i.getMarkers(), p));
    }

    public Toml visitKeyValue(Toml.KeyValue keyValue, P p) {
        Toml.KeyValue kv = keyValue;
        kv = kv.withPrefix(visitSpace(kv.getPrefix(), p));
        kv = kv.withMarkers(visitMarkers(kv.getMarkers(), p));
        kv = kv.getPadding().withKey(visitRightPadded(kv.getPadding().getKey(), p));
        return kv.withValue(visit(kv.getValue(), p));
    }

    public Toml visitLiteral(Toml.Literal literal, P p) {
        Toml.Literal l = literal;
        l = l.withPrefix(visitSpace(l.getPrefix(), p));
        return l.withMarkers(visitMarkers(l.getMarkers(), p));
    }

    public Space visitSpace(Space space, P p) {
        return space;
    }

    public Toml visitTable(Toml.Table table, P p) {
        Toml.Table t = table;
        t = t.withPrefix(visitSpace(t.getPrefix(), p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        return t.withValues(ListUtils.map(t.getValues(), v -> visit(v, p)));
    }

    public <T> @Nullable TomlRightPadded<T> visitRightPadded(@Nullable TomlRightPadded<T> right, P p) {
        if (right == null) {
            //noinspection ConstantConditions
            return null;
        }

        setCursor(new Cursor(getCursor(), right));

        T t = right.getElement();
        if (t instanceof Toml) {
            //noinspection unchecked
            t = visitAndCast((Toml) right.getElement(), p);
        }

        setCursor(getCursor().getParent());
        if (t == null) {
            //noinspection ConstantConditions
            return null;
        }

        Space after = visitSpace(right.getAfter(), p);
        return (after == right.getAfter() && t == right.getElement()) ? right : new TomlRightPadded<>(t, after, right.getMarkers());
    }
}
