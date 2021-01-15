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

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.With;
import lombok.experimental.FieldDefaults;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreePrinter;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.yaml.YamlVisitor;
import org.openrewrite.yaml.internal.YamlPrinter;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
public interface Yaml extends Serializable, Tree {
    @Override
    default String print() {
        return new YamlPrinter<>(TreePrinter.identity()).visit(this, null);
    }

    default String print(TreePrinter<?> printer) {
        return new YamlPrinter<>((TreePrinter<?>) printer).visit(this, null);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    default String printTrimmed() {
        String print = print();

        int i = 0;
        for (; i < print.toCharArray().length && (print.charAt(i) == '\n' || print.charAt(i) == '\r'); i++) {
        }
        print = print.substring(i);

        return print.isEmpty() || !Character.isWhitespace(print.charAt(0)) ?
                print :
                StringUtils.trimIndent(print.trim());
    }

    @Override
    default <R, P> R accept(TreeVisitor<R, P> v, P p) {
        return v instanceof YamlVisitor ?
                acceptYaml((YamlVisitor<R, P>) v, p) : v.defaultValue(null, p);
    }

    default <R, P> R acceptYaml(YamlVisitor<R, P> v, P p) {
        return v.defaultValue(this, p);
    }

    /**
     * @return A new deep copy of this block with different IDs.
     */
    Yaml copyPaste();

    String getPrefix();

    Yaml withPrefix(String prefix);

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @JsonIgnoreProperties(value = "styles")
    class Documents implements Yaml, SourceFile {
        @EqualsAndHashCode.Include
        UUID id;

        Path sourcePath;

        @With
        List<Document> documents;

        @With
        String prefix;

        @With
        Markers markers;

        @Override
        public <R, P> R acceptYaml(YamlVisitor<R, P> v, P p) {
            return v.visitDocuments(this, p);
        }

        @Override
        public Documents copyPaste() {
            return new Documents(randomId(), sourcePath,
                    documents.stream().map(Document::copyPaste).collect(toList()), prefix, Markers.EMPTY);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Document implements Yaml {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        boolean explicit;

        @With
        List<Block> blocks;

        @Nullable
        @With
        End end;

        @With
        String prefix;

        @With
        Markers markers;

        @Override
        public <R, P> R acceptYaml(YamlVisitor<R, P> v, P p) {
            return v.visitDocument(this, p);
        }

        @Override
        public Document copyPaste() {
            return new Document(randomId(), explicit, blocks.stream().map(Block::copyPaste).collect(toList()),
                    end == null ? null : end.copyPaste(), prefix, Markers.EMPTY);
        }

        /**
         * https://yaml.org/spec/1.1/#c-document-end
         */
        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class End implements Yaml {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            String prefix;

            @With
            Markers markers;

            @Override
            public End copyPaste() {
                return new End(randomId(), prefix, Markers.EMPTY);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Scalar implements Block {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        Style style;

        @With
        String value;

        @With
        String prefix;

        @With
        Markers markers;

        public enum Style {
            DOUBLE_QUOTED,
            SINGLE_QUOTED,
            LITERAL,
            FOLDED,
            PLAIN
        }

        @Override
        public <R, P> R acceptYaml(YamlVisitor<R, P> v, P p) {
            return v.visitScalar(this, p);
        }

        @Override
        public Scalar copyPaste() {
            return new Scalar(randomId(), style, value, prefix, Markers.EMPTY);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Mapping implements Block {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        List<Entry> entries;

        @With
        String prefix;

        @With
        Markers markers;

        @Override
        public <R, P> R acceptYaml(YamlVisitor<R, P> v, P p) {
            return v.visitMapping(this, p);
        }

        @Override
        public Mapping copyPaste() {
            return new Mapping(randomId(), entries.stream().map(Entry::copyPaste).collect(toList()), prefix,
                    Markers.EMPTY);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Entry implements Yaml {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Scalar key;

            @With
            // https://yaml.org/spec/1.2/spec.html#:%20mapping%20value//
            String beforeMappingValueIndicator;

            @With
            Block value;

            @With
            String prefix;

            @With
            Markers markers;

            @Override
            public <R, P> R acceptYaml(YamlVisitor<R, P> v, P p) {
                return v.visitMappingEntry(this, p);
            }

            @Override
            public Entry copyPaste() {
                return new Entry(randomId(), key.copyPaste(), beforeMappingValueIndicator, value.copyPaste(), prefix, Markers.EMPTY);
            }
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Sequence implements Block {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        List<Entry> entries;

        @With
        String prefix;

        @With
        Markers markers;

        @Override
        public <R, P> R acceptYaml(YamlVisitor<R, P> v, P p) {
            return v.visitSequence(this, p);
        }

        @Override
        public Sequence copyPaste() {
            return new Sequence(randomId(), entries.stream().map(Entry::copyPaste).collect(toList()), prefix,
                    Markers.EMPTY);
        }

        @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
        @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
        @Data
        public static class Entry implements Yaml {
            @EqualsAndHashCode.Include
            UUID id;

            @With
            Block block;

            @With
            String prefix;

            @With
            Markers markers;

            @Override
            public <R, P> R acceptYaml(YamlVisitor<R, P> v, P p) {
                return v.visitSequenceEntry(this, p);
            }

            @Override
            public Entry copyPaste() {
                return new Entry(randomId(), block.copyPaste(), prefix, Markers.EMPTY);
            }
        }
    }

    interface Block extends Yaml {
        /**
         * @return A new deep copy of this block with different IDs.
         */
        Block copyPaste();

        Block withPrefix(String prefix);
    }
}
