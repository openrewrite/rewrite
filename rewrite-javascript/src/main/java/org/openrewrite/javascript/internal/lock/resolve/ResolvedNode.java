/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript.internal.lock.resolve;

import lombok.Value;
import org.openrewrite.javascript.internal.registry.VersionManifest;

import java.util.Map;

/**
 * One resolved package instance in a {@link ResolutionGraph}: its registry {@link VersionManifest} (name,
 * version, declared dependency ranges, dist/integrity, peers, platform metadata) plus the concrete version each
 * of its dependency edges resolved to. Layout (which {@code node_modules} path, which content-addressed key) is
 * derived per package manager by the serializer and is deliberately not stored here.
 */
@Value
public class ResolvedNode {

    VersionManifest manifest;

    /** For each dependency edge of {@link #manifest} (by dependency name), the version it resolved to in the graph. */
    Map<String, String> resolvedEdges;

    public String getName() {
        return manifest.getName();
    }

    public String getVersion() {
        return manifest.getVersion();
    }
}
