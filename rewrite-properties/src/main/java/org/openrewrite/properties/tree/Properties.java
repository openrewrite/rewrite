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
package org.openrewrite.properties.tree;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.With;
import lombok.experimental.FieldDefaults;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.internal.PropertiesPrinter;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public interface Properties extends Tree {

    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) acceptProperties((PropertiesVisitor<P>) v, p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v instanceof PropertiesVisitor;
    }

    @Nullable
    default <P> Properties acceptProperties(PropertiesVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    String getPrefix();

    Properties withPrefix(String prefix);

    <T extends Properties> T withMarkers(Markers markers);

    Markers getMarkers();

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    @JsonIgnoreProperties(value = "styles")
    class File implements Properties, SourceFile {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        String prefix;

        @With
        Markers markers;

        @With
        Path sourcePath;

        @With
        List<Content> content;

        @With
        String eof;

        @Override
        public <P> Properties acceptProperties(PropertiesVisitor<P> v, P p) {
            return v.visitFile(this, p);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new PropertiesPrinter<>();
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
        String prefix;

        @With
        Markers markers;

        @With
        String key;

        @With
        String beforeEquals;

        @With
        Value value;

        @Override
        public <P> Properties acceptProperties(PropertiesVisitor<P> v, P p) {
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
        String prefix;

        @With
        Markers markers;

        @With
        String text;
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
    @Data
    class Comment implements Content {
        @EqualsAndHashCode.Include
        UUID id;

        @With
        String prefix;

        @With
        Markers markers;

        @With
        String message;

        @Override
        public <P> Properties acceptProperties(PropertiesVisitor<P> v, P p) {
            return v.visitComment(this, p);
        }
    }
}
