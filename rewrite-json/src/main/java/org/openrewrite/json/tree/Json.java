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
package org.openrewrite.json.tree;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.json.JsonVisitor;
import org.openrewrite.json.internal.JsonPrinter;
import org.openrewrite.marker.Markers;

import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public interface Json extends Tree {

    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) acceptJson(v.adapt(JsonVisitor.class), p);
    }

    default <P> @Nullable Json acceptJson(JsonVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(JsonVisitor.class);
    }

    Space getPrefix();

    <J extends Json> J withPrefix(Space prefix);

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Array implements JsonValue {
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

        List<JsonRightPadded<JsonValue>> values;

        public List<JsonValue> getValues() {
            return JsonRightPadded.getElements(values);
        }

        public Array withValues(List<JsonValue> values) {
            return getPadding().withValues(JsonRightPadded.withElements(this.values, values));
        }

        @Override
        public <P> Json acceptJson(JsonVisitor<P> v, P p) {
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

            public List<JsonRightPadded<JsonValue>> getValues() {
                return t.values;
            }

            public Array withValues(List<JsonRightPadded<JsonValue>> values) {
                return t.values == values ? t : new Array(t.id, t.prefix, t.markers, values);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    class Document implements Json, SourceFile {
        @Getter
        @With
        @EqualsAndHashCode.Include
        UUID id;

        @Getter
        @With
        Path sourcePath;

        @Getter
        @With
        Space prefix;

        @Getter
        @With
        Markers markers;

        @Nullable // for backwards compatibility
        @Getter
        @With(AccessLevel.PRIVATE)
        String charsetName;

        @With
        @Getter
        boolean charsetBomMarked;

        @With
        @Getter
        @Nullable
        Checksum checksum;

        @With
        @Getter
        @Nullable
        FileAttributes fileAttributes;

        @Override
        public Charset getCharset() {
            return charsetName == null ? StandardCharsets.UTF_8 : Charset.forName(charsetName);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Json.Document withCharset(Charset charset) {
            return withCharsetName(charset.name());
        }

        @Getter
        @With
        JsonValue value;

        @Getter
        @With
        Space eof;

        @Override
        public <P> Json acceptJson(JsonVisitor<P> v, P p) {
            return v.visitDocument(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new JsonPrinter<>();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Empty implements JsonValue {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;

        Markers markers;

        @Override
        public <P> Json acceptJson(JsonVisitor<P> v, P p) {
            return v.visitEmpty(this, p);
        }

        @Override
        public String toString() {
            return "Empty{prefix=" + prefix + "}";
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Identifier implements JsonKey {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;

        Markers markers;

        String name;

        @Override
        public <P> Json acceptJson(JsonVisitor<P> v, P p) {
            return v.visitIdentifier(this, p);
        }

        @Override
        public String toString() {
            return "Identifier{prefix=" + prefix + ",name=" + name + "}";
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Literal implements JsonValue, JsonKey {
        @EqualsAndHashCode.Include
        UUID id;

        Space prefix;

        Markers markers;

        String source;

        @Nullable // for `null` values
        Object value;

        @Override
        public <P> Json acceptJson(JsonVisitor<P> v, P p) {
            return v.visitLiteral(this, p);
        }

        @Override
        public String toString() {
            return "Literal{prefix=" + prefix + ",source=" + source + "}";
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Member implements Json {
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

        JsonRightPadded<JsonKey> key;

        public JsonKey getKey() {
            return key.getElement();
        }

        public Member withKey(JsonKey key) {
            return getPadding().withKey(this.key.withElement(key));
        }

        @Getter
        @With
        JsonValue value;

        @Override
        public <P> Json acceptJson(JsonVisitor<P> v, P p) {
            return v.visitMember(this, p);
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
            private final Member t;

            public JsonRightPadded<JsonKey> getKey() {
                return t.key;
            }

            public Member withKey(JsonRightPadded<JsonKey> key) {
                return t.key == key ? t : new Member(t.id, t.prefix, t.markers, key, t.value);
            }
        }

        @Override
        public String toString() {
            return "Member{prefix=" + prefix + ",key=" + key.getElement() + "}";
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class JsonObject implements JsonValue {
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

        /**
         * Either {@link Member} or {@link Empty}
         */
        List<JsonRightPadded<Json>> members;

        public List<Json> getMembers() {
            return JsonRightPadded.getElements(members);
        }

        public JsonObject withMembers(List<Json> members) {
            return getPadding().withMembers(JsonRightPadded.withElements(this.members, members));
        }

        @Override
        public <P> Json acceptJson(JsonVisitor<P> v, P p) {
            return v.visitObject(this, p);
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
            private final JsonObject t;

            public List<JsonRightPadded<Json>> getMembers() {
                return t.members;
            }

            public JsonObject withMembers(List<JsonRightPadded<Json>> members) {
                return t.members == members ? t : new JsonObject(t.id, t.prefix, t.markers, members);
            }
        }
    }
}
