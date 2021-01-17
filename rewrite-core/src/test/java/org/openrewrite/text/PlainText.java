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
package org.openrewrite.text;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreePrinter;
import org.openrewrite.marker.Markers;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class PlainText implements SourceFile, Tree {
    private final UUID id;
    private final String text;
    private Markers markers;

    @JsonCreator
    public PlainText(@JsonProperty("id") UUID id,
                     @JsonProperty("text") String text,
                     @JsonProperty("markers") Markers markers) {
        this.id = id;
        this.text = text;
        this.markers = markers;
    }

    @Override
    public Path getSourcePath() {
        return Paths.get("text.txt");
    }

    @Override
    public Markers getMarkers() {
        return markers;
    }

    @SuppressWarnings("unchecked")
    @Override
    public PlainText withMarkers(Markers markers) {
        this.markers = markers;
        return this;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public String print(TreePrinter<?> printer) {
        return print();
    }

    public PlainText withText(String toText) {
        return new PlainText(id, toText, markers);
    }

    @Override
    public String print() {
        return text;
    }
}
