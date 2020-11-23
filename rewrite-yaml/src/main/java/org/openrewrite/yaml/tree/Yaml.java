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
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.YamlSourceVisitor;
import org.openrewrite.yaml.internal.PrintYaml;

import java.io.Serializable;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
public interface Yaml extends Serializable, Tree {
    @Override
    default String print() {
        return new PrintYaml().visit(this);
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
    default <R> R accept(SourceVisitor<R> v) {
        return v instanceof YamlSourceVisitor ?
                acceptYaml((YamlSourceVisitor<R>) v) : v.defaultTo(null);
    }

    default <R> R acceptYaml(YamlSourceVisitor<R> v) {
        return v.defaultTo(null);
    }

    /**
     * @return A new deep copy of this block with different IDs.
     */
    Yaml copyPaste();

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @JsonIgnoreProperties(value = "styles")
    class Documents implements Yaml, SourceFile {
        @EqualsAndHashCode.Include
        UUID id;

        URI sourcePath;

        @With
        Collection<Metadata> metadata;

        @With
        List<Document> documents;

        @With
        Formatting formatting;

        @Override
        public <R> R acceptYaml(YamlSourceVisitor<R> v) {
            return v.visitDocuments(this);
        }

        @Override
        public Documents copyPaste() {
            return new Documents(randomId(), sourcePath, metadata,
                    documents.stream().map(Document::copyPaste).collect(toList()), formatting);
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
        Formatting formatting;

        @Override
        public <R> R acceptYaml(YamlSourceVisitor<R> v) {
            return v.visitDocument(this);
        }

        @Override
        public Document copyPaste() {
            return new Document(randomId(), explicit, blocks.stream().map(Block::copyPaste).collect(toList()),
                    end == null ? null : end.copyPaste(), formatting);
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
            Formatting formatting;

            @Override
            public End copyPaste() {
                return new End(randomId(), formatting);
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
        Formatting formatting;

        public enum Style {
            DOUBLE_QUOTED,
            SINGLE_QUOTED,
            LITERAL,
            FOLDED,
            PLAIN
        }

        @Override
        public <R> R acceptYaml(YamlSourceVisitor<R> v) {
            return v.visitScalar(this);
        }

        @Override
        public Scalar copyPaste() {
            return new Scalar(randomId(), style, value, formatting);
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
        Formatting formatting;

        @Override
        public <R> R acceptYaml(YamlSourceVisitor<R> v) {
            return v.visitMapping(this);
        }

        @Override
        public Mapping copyPaste() {
            return new Mapping(randomId(), entries.stream().map(Entry::copyPaste).collect(toList()), formatting);
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
            Block value;

            @With
            Formatting formatting;

            @Override
            public <R> R acceptYaml(YamlSourceVisitor<R> v) {
                return v.visitMappingEntry(this);
            }

            @Override
            public Entry copyPaste() {
                return new Entry(randomId(), key.copyPaste(), value.copyPaste(), formatting);
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
        Formatting formatting;

        @Override
        public <R> R acceptYaml(YamlSourceVisitor<R> v) {
            return v.visitSequence(this);
        }

        @Override
        public Sequence copyPaste() {
            return new Sequence(randomId(), entries.stream().map(Entry::copyPaste).collect(toList()), formatting);
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
            Formatting formatting;

            @Override
            public <R> R acceptYaml(YamlSourceVisitor<R> v) {
                return v.visitSequenceEntry(this);
            }

            @Override
            public Entry copyPaste() {
                return new Entry(randomId(), block.copyPaste(), formatting);
            }
        }
    }

    interface Block extends Yaml {
        /**
         * @return A new deep copy of this block with different IDs.
         */
        Block copyPaste();
    }
}
