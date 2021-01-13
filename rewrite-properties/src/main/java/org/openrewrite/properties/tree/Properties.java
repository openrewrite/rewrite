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
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreePrinter;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.Markers;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.internal.PropertiesPrinter;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
public interface Properties extends Serializable, Tree {

    default String print(TreePrinter<?> printer) {
        return new PropertiesPrinter<>((TreePrinter<?>)printer).visit(this, null);
    }

    @Override
    default String print() {
        return new PropertiesPrinter<>(TreePrinter.identity()).visit(this, null);
    }

    @Override
    default <R, P> R accept(TreeVisitor<R, P> v, P p) {
        return v instanceof PropertiesVisitor ?
                acceptProperties((PropertiesVisitor<R, P>) v, p) : v.defaultValue(null, p);
    }

    default <R, P> R acceptProperties(PropertiesVisitor<R, P> v, P p) {
        return v.defaultValue(this, p);
    }

    String getPrefix();

    Properties withPrefix(String prefix);

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @JsonIgnoreProperties(value = "styles")
    class File implements Properties, SourceFile {
        @EqualsAndHashCode.Include
        UUID id;

        Path sourcePath;

        @With
        List<Content> content;

        @With
        String eof;

        @With
        String prefix;

        @With
        Markers markers;

        @Override
        public <R, P> R acceptProperties(PropertiesVisitor<R, P> v, P p) {
            return v.visitFile(this, p);
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
        String beforeEquals;

        @With
        Value value;

        @With
        String prefix;

        @With
        Markers markers;

        @Override
        public <R, P> R acceptProperties(PropertiesVisitor<R, P> v, P p) {
            return v.visitEntry(this, p);
        }
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Value {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        String text;

        @With
        String prefix;
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
        String prefix;

        @With
        Markers markers;
    }
}
