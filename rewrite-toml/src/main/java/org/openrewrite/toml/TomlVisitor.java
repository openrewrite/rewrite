/*
 * Copyright 2022 the original author or authors.
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

import org.openrewrite.Cursor;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.toml.tree.Comment;
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

    public Toml visitDocument(Toml.Document document, P p) {
        Toml.Document d = document;
        d = d.withPrefix(visitSpace(d.getPrefix(), p));
        d = d.withMarkers(visitMarkers(d.getMarkers(), p));
        d = d.withEof(visitSpace(d.getEof(), p));
        d = d.withExpressions(ListUtils.map(d.getExpressions(), e -> (Toml.Expression) visitExpression(e, p)));
        return d;
    }

    public Toml visitExpression(Toml.Expression expression, P p) {
        Toml.Expression e = expression;
        e = e.withPrefix(visitSpace(e.getPrefix(), p));
        e = e.withMarkers(visitMarkers(e.getMarkers(), p));
        e = e.withValue((TomlValue) visitValue(e.getValue(), p));
        e = e.withComment(visitComment(e.getComment(), p));
        return e;
    }

    public Comment visitComment(Comment comment, P p) {
        Comment c = comment;
        c = c.withMarkers(visitMarkers(c.getMarkers(), p));
        return c;
    }

    public Toml visitLiteral(Toml.Literal literal, P p) {
        Toml.Literal lv = literal;
        lv = lv.withPrefix(visitSpace(lv.getPrefix(), p));
        lv = lv.withMarkers(visitMarkers(lv.getMarkers(), p));
        return lv;
    }

    public Toml visitArray(Toml.Array array, P p) {
        Toml.Array a = array;
        a = a.withPrefix(visitSpace(a.getPrefix(), p));
        a = a.withMarkers(visitMarkers(a.getMarkers(), p));
        a = a.withValues(ListUtils.map(a.getValues(), v -> (TomlValue) visit(v, p)));
        return a;
    }

    public Toml visitTable(Toml.Table table, P p) {
        Toml.Table t = table;
        t = t.withPrefix(visitSpace(t.getPrefix(), p));
        t = t.withMarkers(visitMarkers(t.getMarkers(), p));
        t = t.withEntries(ListUtils.map(t.getEntries(), e -> visit(e, p)));
        return t;
    }

    public Toml visitKeyValue(Toml.KeyValue keyValue, P p) {
        Toml.KeyValue kv = keyValue;
        kv = kv.withPrefix(visitSpace(kv.getPrefix(), p));
        kv = kv.withMarkers(visitMarkers(kv.getMarkers(), p));
        kv = kv.withKey((Toml.Key) visitKey(kv.getKey(), p));
        kv = kv.withValue((Toml.Literal) visitLiteral(kv.getValue(), p));
        return kv;
    }

    public Toml visitKey(Toml.Key key, P p) {
        Toml.Key k = key;
        k = k.withPrefix(visitSpace(k.getPrefix(), p));
        k = k.withMarkers(visitMarkers(k.getMarkers(), p));
        return k;
    }

    public Toml visitValue(TomlValue value, P p) {
        TomlValue v = value;
        v = v.withPrefix(visitSpace(v.getPrefix(), p));
        v = v.withMarkers(visitMarkers(v.getMarkers(), p));
        return v;
    }

    public Space visitSpace(Space space, P p) {
        return space;
    }

    @SuppressWarnings("ConstantConditions")
    public <T extends Toml> TomlRightPadded<T> visitRightPadded(@Nullable TomlRightPadded<T> right, P p) {
        if (right == null) {
            return null;
        }

        setCursor(new Cursor(getCursor(), right));

        T t = right.getElement();
        if (t instanceof Toml) {
            t = (T) visit(right.getElement(), p);
        }

        setCursor(getCursor().getParent());
        if (t == null) {
            return null;
        }
        Space after = visitSpace(right.getAfter(), p);
        return (after == right.getAfter() && t == right.getElement()) ? right : new TomlRightPadded<>(t, after, right.getMarkers());
    }
}