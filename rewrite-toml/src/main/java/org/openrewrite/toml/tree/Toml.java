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
package org.openrewrite.toml.tree;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.With;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Checksum;
import org.openrewrite.Cursor;
import org.openrewrite.FileAttributes;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.Markers;
import org.openrewrite.toml.TomlVisitor;
import org.openrewrite.toml.internal.TomlPrinter;

import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public interface Toml extends Tree {

    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) acceptToml(v.adapt(TomlVisitor.class), p);
    }

    default <P> @Nullable Toml acceptToml(TomlVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(TomlVisitor.class);
    }

    Space getPrefix();

    <T extends Toml> T withPrefix(Space prefix);

    @Getter
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    class Document implements Toml, SourceFile {
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Path sourcePath;

        @With
        Space prefix;

        @With
        Markers markers;

        @Nullable // for backwards compatibility
        @With(AccessLevel.PRIVATE)
        String charsetName;

        @With
        boolean charsetBomMarked;

        @With
        @Nullable
        Checksum checksum;

        @With
        @Nullable
        FileAttributes fileAttributes;

        @With
        List<Expression> expressions;

        @With
        Space eof;

        @Override
        public Charset getCharset() {
            return charsetName == null ? StandardCharsets.UTF_8 : Charset.forName(charsetName);
        }

        @Override
        public SourceFile withCharset(Charset charset) {
            return withCharsetName(charset.name());
        }

        @Override
        public <P> Toml acceptToml(TomlVisitor<P> v, P p) {
            return v.visitDocument(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new TomlPrinter<>();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Expression implements TomlValue {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        TomlValue value;
        Comment comment;

        @Override
        public <P> Toml acceptToml(TomlVisitor<P> v, P p) {
            return v.visitExpression(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class KeyValue implements TomlKey, TomlValue {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        Key key;
        Literal value;

        @Override
        public <P> Toml acceptToml(TomlVisitor<P> v, P p) {
            return v.visitKeyValue(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Key implements TomlKey {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String name;

        @Override
        public <P> Toml acceptToml(TomlVisitor<P> v, P p) {
            return v.visitKey(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Literal implements TomlValue {
        @EqualsAndHashCode.Include
        UUID id;
        Space prefix;
        Markers markers;
        String source;
        Object value;

        @Override
        public <P> Toml acceptToml(TomlVisitor<P> v, P p) {
            return v.visitLiteral(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Array implements TomlValue {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @Getter
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @Getter
        @With
        Space prefix;

        @Getter
        @With
        Markers markers;

        List<TomlRightPadded<TomlValue>> values;

        public List<TomlValue> getValues() {
            return TomlRightPadded.getElements(values);
        }

        public Array withValues(List<TomlValue> values) {
            return getPadding().withValues(TomlRightPadded.withElements(this.values, values));
        }

        @Override
        public <P> Toml acceptToml(TomlVisitor<P> v, P p) {
            return v.visitArray(this, p);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.a != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Array a;

            public List<TomlRightPadded<TomlValue>> getValues() {
                return a.values;
            }

            public Array withValues(List<TomlRightPadded<TomlValue>> values) {
                return a.values == values ? a : new Array(a.id, a.prefix, a.markers, values);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Table implements TomlValue {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @Getter
        @EqualsAndHashCode.Include
        UUID id;

        @With
        @Getter
        Space prefix;

        @With
        @Getter
        Markers markers;

        List<TomlRightPadded<Toml>> entries;

        public List<Toml> getEntries() {
            return TomlRightPadded.getElements(entries);
        }

        public Table withEntries(List<Toml> entries) {
            return getPadding().withEntries(TomlRightPadded.withElements(this.entries, entries));
        }

        @Override
        public <P> Toml acceptToml(TomlVisitor<P> v, P p) {
            return v.visitTable(this, p);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Table t;

            public List<TomlRightPadded<Toml>> getEntries() {
                return t.entries;
            }

            public Table withEntries(List<TomlRightPadded<Toml>> entries) {
                return t.entries == entries ? t : new Table(t.id, t.prefix, t.markers, entries);
            }
        }
    }
}