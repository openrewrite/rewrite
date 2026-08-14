/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.gradle.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.Cursor;
import org.openrewrite.marker.Marker;
import org.openrewrite.maven.tree.GroupArtifact;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * A snapshot of which libraries declared in a
 * {@code org.openrewrite.gradle.trait.GradleVersionCatalog} originally shared each
 * {@code versionRef(...)} declaration, taken before any recipe mutates the catalog.
 * <p>
 * Attached to the version catalog's own root AST node, so downstream recipes can tell whether
 * two separately-requested version bumps actually target the same underlying
 * {@code version(...)} declaration.
 */
@Value
@With
public class GradleVersionCatalogVersionReferences implements Marker {
    UUID id;

    /**
     * Keyed by a shared {@code version(...)} declaration's own alias. Only references actually
     * resolved through by at least one library are recorded.
     */
    Map<String, SharedReference> sharedReferencesByAlias;

    @Override
    public String print(Cursor cursor, UnaryOperator<String> commentWrapper, boolean verbose) {
        return verbose ? commentWrapper.apply("(" + this + ")") : "";
    }

    @Override
    public String toString() {
        return sharedReferencesByAlias.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "->" + e.getValue())
                .collect(Collectors.joining(", "));
    }

    /**
     * The version value a shared reference held when the snapshot was taken, together with the
     * group:artifact of every library that originally resolved its version through it.
     */
    @Value
    public static class SharedReference {
        String version;
        List<GroupArtifact> groupArtifacts;

        @Override
        public String toString() {
            return version + "@" + groupArtifacts.stream()
                    .map(ga -> ga.getGroupId() + ":" + ga.getArtifactId())
                    .collect(Collectors.joining(", ", "[", "]"));
        }
    }
}
