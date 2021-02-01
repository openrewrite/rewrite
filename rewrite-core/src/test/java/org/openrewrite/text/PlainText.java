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
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreePrinter;
import org.openrewrite.marker.Markers;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class PlainText implements SourceFile, Tree {
    private final UUID id;
    private Markers markers;
    private final String text;

    public PlainText(UUID id,
                     Markers markers,
                     String text) {
        this.id = id;
        this.markers = markers;
        this.text = text;
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
    public <P> String print(TreePrinter<P> printer, P p) {
        return print(p);
    }

    public PlainText withText(String toText) {
        return new PlainText(id, markers, toText);
    }

    @Override
    public <P> String print(P p) {
        return text;
    }
}
