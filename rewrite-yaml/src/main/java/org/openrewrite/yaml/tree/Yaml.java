/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.yaml.tree;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.yaml.YamlVisitor;
import org.openrewrite.yaml.internal.YamlPrinter;

import java.lang.ref.SoftReference;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;

public interface Yaml extends Tree {

    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) acceptYaml(v.adapt(YamlVisitor.class), p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(YamlVisitor.class);
    }

    default <P> @Nullable Yaml acceptYaml(YamlVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    /**
     * @return A new deep copy of this block with different IDs.
     */
    Yaml copyPaste();

    String getPrefix();

    Yaml withPrefix(String prefix);

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @With
    class Documents implements Yaml, SourceFileWithReferences {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;
        Path sourcePath;

        @Nullable
        FileAttributes fileAttributes;

        @Nullable // for backwards compatibility
        @With(AccessLevel.PRIVATE)
        String charsetName;

        boolean charsetBomMarked;

        @Nullable
        Checksum checksum;

        @Nullable
        String suffix;

        @Override
        public Charset getCharset() {
            return charsetName == null ? StandardCharsets.UTF_8 : Charset.forName(charsetName);
        }

        @Override
        public SourceFile withCharset(Charset charset) {
            return withCharsetName(charset.name());
        }

        List<Document> documents;

        @Override
        public <P> Yaml acceptYaml(YamlVisitor<P> v, P p) {
            return v.visitDocuments(this, p);
        }

        @Override
        public Documents copyPaste() {
            return new Documents(randomId(), Markers.EMPTY,
                    sourcePath, fileAttributes, charsetName, charsetBomMarked, checksum, suffix, documents.stream().map(Document::copyPaste).collect(toList()));
        }

        /**
         * Prefixes will always be on {@link Mapping.Entry}.
         *
         * @return The empty string.
         */
        @Override
        public String getPrefix() {
            return "";
        }

        @Override
        public Documents withPrefix(String prefix) {
            if (!prefix.isEmpty()) {
                throw new UnsupportedOperationException("Yaml.Documents may not have a non-empty prefix");
            }
            return this;
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new YamlPrinter<>();
        }

        @Nullable
        @NonFinal
        transient SoftReference<References> references;

        @Override
        public References getReferences() {
            this.references = build(this.references);
            return Objects.requireNonNull(this.references.get());
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Document implements Yaml {
        @EqualsAndHashCode.Include
        UUID id;

        String prefix;
        Markers markers;
        boolean explicit;
        Block block;
        End end;

        @Override
        public <P> Yaml acceptYaml(YamlVisitor<P> v, P p) {
            return v.visitDocument(this, p);
        }

        @Override
        public Document copyPaste() {
            return new Document(
                    randomId(),
                    prefix,
                    Markers.EMPTY,
                    explicit,
                    block.copyPaste(),
                    end.copyPaste()
            );
        }

        /**
         * <a href="https://yaml.org/spec/1.1/#c-document-end">Document end</a> specification.
         */
        @Value
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @With
        public static class End implements Yaml {
            @EqualsAndHashCode.Include
            UUID id;

            String prefix;
            Markers markers;

            /**
             * Yaml documents may be explicitly ended with "..."
             * When this is set to "true" the "..." will be printed out.
             * When this is set to "false" no "..." will be printed, but a comment at the end of the document still will be
             * @see <a href="https://yaml.org/spec/1.2/spec.html#id2800401">YAML specification</a>
             */
            boolean explicit;

            @Override
            public <P> Yaml acceptYaml(YamlVisitor<P> v, P p) {
                return v.visitDocumentEnd(this, p);
            }

            @Override
            public End copyPaste() {
                return new End(randomId(), prefix, Markers.EMPTY, explicit);
            }
        }
    }

    interface Block extends Yaml {
        /**
         * @return A new deep copy of this block with different IDs.
         */
        @Override
        Block copyPaste();

        @Override
        Block withPrefix(String prefix);
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Scalar implements Block, YamlKey {
        @EqualsAndHashCode.Include
        UUID id;

        String prefix;
        Markers markers;
        Style style;

        @Nullable
        Anchor anchor;

        @Nullable
        Tag tag;

        String value;

        public enum Style {
            DOUBLE_QUOTED,
            SINGLE_QUOTED,
            LITERAL,
            FOLDED,
            PLAIN
        }

        @Override
        public <P> Yaml acceptYaml(YamlVisitor<P> v, P p) {
            return v.visitScalar(this, p);
        }

        @Override
        public Scalar copyPaste() {
            return new Scalar(randomId(), prefix, Markers.EMPTY, style, anchor, tag, value);
        }

        @Override
        public String toString() {
            return "Yaml.Scalar(" + value + ")";
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class Mapping implements Block {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;

        @Nullable
        String openingBracePrefix;

        List<Entry> entries;

        @Nullable
        String closingBracePrefix;

        @Nullable
        Anchor anchor;

        @Nullable
        Tag tag;

        @JsonCreator
        public Mapping(UUID id, Markers markers, @Nullable String openingBracePrefix, List<Entry> entries, @Nullable String closingBracePrefix, @Nullable Anchor anchor, @Nullable Tag tag) {
            this.id = id;
            this.markers = markers;
            this.openingBracePrefix = openingBracePrefix;
            this.entries = entries;
            this.closingBracePrefix = closingBracePrefix;
            this.anchor = anchor;
            this.tag = tag;
        }

        @Override
        public <P> Yaml acceptYaml(YamlVisitor<P> v, P p) {
            return v.visitMapping(this, p);
        }

        @Override
        public Mapping copyPaste() {
            return new Mapping(randomId(), Markers.EMPTY, openingBracePrefix, entries.stream().map(Entry::copyPaste)
                    .collect(toList()), closingBracePrefix, anchor, tag);
        }

        /**
         * Prefixes will always be on {@link Entry}.
         *
         * @return The empty string.
         */
        @Override
        public String getPrefix() {
            return "";
        }

        @Override
        public Mapping withPrefix(String prefix) {
            if (!prefix.isEmpty()) {
                throw new UnsupportedOperationException("Yaml.Mapping may not have a non-empty prefix");
            }
            return this;
        }

        @Value
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @With
        public static class Entry implements Yaml {
            @EqualsAndHashCode.Include
            UUID id;

            String prefix;
            Markers markers;
            YamlKey key;

            // https://yaml.org/spec/1.2/spec.html#:%20mapping%20value//
            String beforeMappingValueIndicator;

            Block value;

            @Override
            public <P> Yaml acceptYaml(YamlVisitor<P> v, P p) {
                return v.visitMappingEntry(this, p);
            }

            @Override
            public Entry copyPaste() {
                return new Entry(randomId(), prefix, Markers.EMPTY, key.copyPaste(),
                        beforeMappingValueIndicator, value.copyPaste());
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @With
    class Sequence implements Block {
        @EqualsAndHashCode.Include
        UUID id;

        Markers markers;

        /**
         * Will contain the whitespace preceding the '[' in an inline sequence like [a, b].
         * Will be null if the sequence is not an inline sequence, e.g.:
         *  - foo
         *  - bar
         */
        @Nullable
        String openingBracketPrefix;

        List<Entry> entries;

        @Nullable
        String closingBracketPrefix;

        @Nullable
        Anchor anchor;

        @Nullable
        Tag tag;

        @JsonCreator
        public Sequence(UUID id, Markers markers, @Nullable String openingBracketPrefix, List<Entry> entries, @Nullable String closingBracketPrefix, @Nullable Anchor anchor, @Nullable Tag tag) {
            this.id = id;
            this.markers = markers;
            this.openingBracketPrefix = openingBracketPrefix;
            this.entries = entries;
            this.closingBracketPrefix = closingBracketPrefix;
            this.anchor = anchor;
            this.tag = tag;
        }

        @Override
        public <P> Yaml acceptYaml(YamlVisitor<P> v, P p) {
            return v.visitSequence(this, p);
        }

        @Override
        public Sequence copyPaste() {
            return new Sequence(randomId(), Markers.EMPTY, openingBracketPrefix,
                    entries.stream().map(Entry::copyPaste).collect(toList()), closingBracketPrefix, anchor, tag);
        }

        @Override
        public String getPrefix() {
            return "";
        }

        @Override
        public Sequence withPrefix(String prefix) {
            if (!prefix.isEmpty()) {
                throw new UnsupportedOperationException("Yaml.Sequence may not have a non-empty prefix");
            }
            return this;
        }

        @Value
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @With
        public static class Entry implements Yaml {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            String prefix;

            @With
            Markers markers;

            @With
            Block block;

            /**
             * Set to true when this entry is part of a sequence like:
             * - 1
             * - 2
             *
             * And false when this entry is part of a sequence like:
             * [1, 2]
             */
            boolean dash;

            /**
             * Holds the whitespace between this entry and its trailing comma, if any.
             *   v
             * [1 , 2]
             * null if there is no trailing comma.
             */
            @Nullable
            @With
            String trailingCommaPrefix;

            @Override
            public <P> Yaml acceptYaml(YamlVisitor<P> v, P p) {
                return v.visitSequenceEntry(this, p);
            }

            @Override
            public Entry copyPaste() {
                return new Entry(randomId(), prefix, Markers.EMPTY,
                        block.copyPaste(), dash, trailingCommaPrefix);
            }
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Alias implements Block, YamlKey {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        String prefix;

        @With
        Markers markers;

        @With
        Anchor anchor;

        @Override
        public <P> Yaml acceptYaml(YamlVisitor<P> v, P p) {
            return v.visitAlias(this, p);
        }

        @Override
        public String getValue() {
            return anchor.key;
        }

        @Override
        public Alias copyPaste() {
            return new Alias(randomId(), prefix, Markers.EMPTY, anchor);
        }

        @Override
        public String toString() {
            return "Yaml.Alias(" + anchor + ")";
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Anchor implements Yaml {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        String prefix;

        @With
        String postfix;

        @With
        Markers markers;

        @With
        String key;

        @Override
        public <P> Yaml acceptYaml(YamlVisitor<P> v, P p) {
            return v.visitAnchor(this, p);
        }

        @Override
        public Anchor copyPaste() {
            return new Anchor(randomId(), prefix, postfix, Markers.EMPTY, key);
        }

        @Override
        public String toString() {
            return "Yaml.Anchor(" + key + ")";
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @With
    class Tag implements Yaml {
        public enum Kind {
            LOCAL {
                @Override
                public String print(String name) {
                    return "!" + name;
                }
            },
            IMPLICIT_GLOBAL {
                @Override
                public String print(String name) {
                    return "!!" + name;
                }
            },
            EXPLICIT_GLOBAL {
                @Override
                public String print(String name) {
                    return "!<" + name + ">";
                }
            };

            public abstract String print(String name);
        }

        @EqualsAndHashCode.Include
        UUID id;

        @With
        String prefix;

        @With
        Markers markers;

        @With
        String name;

        @With
        String suffix;

        @Getter
        Kind kind;

        public Tag(UUID id, String prefix, Markers markers, String name, String suffix, Kind kind) {
            this.id = id;
            this.prefix = prefix;
            this.suffix = suffix;
            this.markers = markers;
            this.name = name;
            this.kind = kind;
        }

        @Override
        public <P> Yaml acceptYaml(YamlVisitor<P> v, P p) {
            return v.visitTag(this, p);
        }

        @Override
        public Tag copyPaste() {
            return new Tag(randomId(), prefix, Markers.EMPTY, name, suffix, kind);
        }

        @Override
        public String toString() {
            return "Yaml.Tag(" + name + ")";
        }
    }
}
