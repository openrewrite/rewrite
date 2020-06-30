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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.With;
import lombok.experimental.FieldDefaults;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.YamlSourceVisitor;
import org.openrewrite.yaml.internal.PrintYaml;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
public interface Yaml extends Serializable, Tree {
    @Override
    default String print() {
        return new PrintYaml().visit(this);
    }

    @Override
    default <R> R accept(SourceVisitor<R> v) {
        return v instanceof YamlSourceVisitor ?
                acceptYaml((YamlSourceVisitor<R>) v) : v.defaultTo(null);
    }

    default <R> R acceptYaml(YamlSourceVisitor<R> v) {
        return v.defaultTo(null);
    }

    @JsonIgnore
    @Override
    default String getTreeType() {
        return "yml";
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Documents implements Yaml, SourceFile {
        @EqualsAndHashCode.Include
        UUID id;

        String sourcePath;

        @With
        Map<Metadata, String> metadata;

        @With
        List<Document> documents;

        @With
        Formatting formatting;

        @Override
        public <R> R acceptYaml(YamlSourceVisitor<R> v) {
            return v.visitDocuments(this);
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
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Sequence implements Block {
        @EqualsAndHashCode.Include
        UUID id;

        List<Entry> entries;

        @With
        Formatting formatting;

        @Override
        public <R> R acceptYaml(YamlSourceVisitor<R> v) {
            return v.visitSequence(this);
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
        }
    }

    interface Block extends Yaml {
    }
}
