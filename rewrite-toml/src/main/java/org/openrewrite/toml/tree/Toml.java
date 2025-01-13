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
package org.openrewrite.toml.tree;

import lombok.*;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.toml.TomlVisitor;
import org.openrewrite.toml.internal.TomlPrinter;

import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
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

    <J extends Toml> J withPrefix(Space prefix);

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Array implements Toml {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @With
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        List<TomlRightPadded<Toml>> values;

        public List<Toml> getValues() {
            return TomlRightPadded.getElements(values);
        }

        public Array withValues(List<Toml> values) {
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
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Array t;

            public List<TomlRightPadded<Toml>> getValues() {
                return t.values;
            }

            public Array withValues(List<TomlRightPadded<Toml>> values) {
                return t.values == values ? t : new Array(t.id, t.prefix, t.markers, values);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Document implements Toml, SourceFile {
        @EqualsAndHashCode.Include
        UUID id;

        Path sourcePath;
        Space prefix;
        Markers markers;

        @With(AccessLevel.PRIVATE)
        String charsetName;

        boolean charsetBomMarked;
        Checksum checksum;
        FileAttributes fileAttributes;

        @Override
        public Charset getCharset() {
            return Charset.forName(charsetName);
        }

        @Override
        public SourceFile withCharset(Charset charset) {
            return withCharsetName(charset.name());
        }

        List<TomlValue> values;
        Space eof;

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
    class Empty implements Toml {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;

        @Override
        public <P> Toml acceptToml(TomlVisitor<P> v, P p) {
            return v.visitEmpty(this, p);
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Identifier implements TomlKey {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;
        Markers markers;
        String source;
        String name;

        @Override
        public <P> Toml acceptToml(TomlVisitor<P> v, P p) {
            return v.visitIdentifier(this, p);
        }

        @Override
        public String toString() {
            return "Identifier{prefix=" + prefix + ", name=" + name + "}";
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @With
    class KeyValue implements TomlValue {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;

        Markers markers;

        TomlRightPadded<TomlKey> key;

        public TomlKey getKey() {
            return key.getElement();
        }

        public KeyValue withKey(TomlKey key) {
            return getPadding().withKey(TomlRightPadded.withElement(this.key, key));
        }

        Toml value;

        @Override
        public <P> Toml acceptToml(TomlVisitor<P> v, P p) {
            return v.visitKeyValue(this, p);
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
            private final KeyValue t;

            public TomlRightPadded<TomlKey> getKey() {
                return t.key;
            }

            public KeyValue withKey(TomlRightPadded<TomlKey> key) {
                return t.key == key ? t : new KeyValue(t.id, t.prefix, t.markers, key, t.value);
            }
        }

        @Override
        public String toString() {
            return "KeyValue{prefix=" + prefix + ", key=" + key.getElement() + '}';
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

        TomlType.Primitive type;

        String source;

        Object value;

        @Override
        public <P> Toml acceptToml(TomlVisitor<P> v, P p) {
            return v.visitLiteral(this, p);
        }

        @Override
        public String toString() {
            return "Literal{prefix=" + prefix + ", source=" + source + "}";
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Table implements TomlValue {
        @Nullable
        @NonFinal
        transient WeakReference<Padding> padding;

        @EqualsAndHashCode.Include
        @With
        UUID id;

        @With
        Space prefix;

        @With
        Markers markers;

        @With
        TomlRightPadded<Toml.Identifier> name;

        List<TomlRightPadded<Toml>> values;

        public List<Toml> getValues() {
            return TomlRightPadded.getElements(values);
        }

        public Table withValues(List<Toml> values) {
            return getPadding().withValues(TomlRightPadded.withElements(this.values, values));
        }

        @Override
        public <P> Toml acceptToml(TomlVisitor<P> v, P p) {
            return v.visitTable(this, p);
        }

        public Table.Padding getPadding() {
            Table.Padding p;
            if (this.padding == null) {
                p = new Table.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Table.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @RequiredArgsConstructor
        public static class Padding {
            private final Table t;

            public List<TomlRightPadded<Toml>> getValues() {
                return t.values;
            }

            public Table withValues(List<TomlRightPadded<Toml>> values) {
                return t.values == values ? t : new Table(t.id, t.prefix, t.markers, t.name, values);
            }
        }
    }
}
