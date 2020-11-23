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
package org.openrewrite.properties.tree;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.With;
import lombok.experimental.FieldDefaults;
import org.openrewrite.*;
import org.openrewrite.properties.PropertiesSourceVisitor;
import org.openrewrite.properties.internal.PrintProperties;

import java.io.Serializable;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
public interface Properties extends Serializable, Tree {
    @Override
    default String print() {
        return new PrintProperties().visit(this);
    }

    @Override
    default <R> R accept(SourceVisitor<R> v) {
        return v instanceof PropertiesSourceVisitor ?
                acceptProperties((PropertiesSourceVisitor<R>) v) : v.defaultTo(null);
    }

    default <R> R acceptProperties(PropertiesSourceVisitor<R> v) {
        return v.defaultTo(null);
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @JsonIgnoreProperties(value = "styles")
    class File implements Properties, SourceFile {
        @EqualsAndHashCode.Include
        UUID id;

        URI sourcePath;

        @With
        Collection<Metadata> metadata;

        @With
        List<Content> content;

        @With
        Formatting formatting;

        @Override
        public Formatting getFormatting() {
            return formatting;
        }

        @Override
        public <R> R acceptProperties(PropertiesSourceVisitor<R> v) {
            return v.visitFile(this);
        }
    }

    interface Content extends Properties {
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Entry implements Content {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        String key;

        @With
        String value;

        @With
        Formatting equalsFormatting;

        @With
        Formatting formatting;

        @Override
        public <R> R acceptProperties(PropertiesSourceVisitor<R> v) {
            return v.visitEntry(this);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Comment implements Content {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        String message;

        @With
        Formatting formatting;
    }
}
