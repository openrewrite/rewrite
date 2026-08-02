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

import lombok.Getter;
import org.openrewrite.javascript.internal.registry.VersionManifest;

import java.util.Collections;
import java.util.Map;

/**
 * One resolved package instance in a {@link ResolutionGraph}: its registry {@link VersionManifest} (name,
 * version, declared dependency ranges, dist/integrity, peers, platform metadata) plus the concrete version each
 * of its dependency edges resolved to. Layout (which {@code node_modules} path, which content-addressed key) is
 * derived per package manager by the serializer and is deliberately not stored here.
 * <p>
 * The {@code dev}/{@code optional}/{@code devOptional} flags carry npm's reachability classification (a node is
 * {@code dev} when every path from the root reaches it through a dev edge, {@code optional} likewise through an
 * optional edge, {@code devOptional} when every path is dev-or-optional but the two overlap). They are computed
 * once by the graph builder and consumed by the serializers that mark them (npm on each entry, pnpm on the
 * snapshot); the yarn/bun serializers, which record dev/optional only as importer scopes, ignore them.
 */
@Getter
public final class ResolvedNode {

    private final VersionManifest manifest;

    /**
     * For each dependency edge of {@link #manifest} (by dependency name), the version it resolved to. Includes
     * both regular and optional dependency edges, since both are placed in the tree; the optionality of an edge
     * is recovered from {@code manifest.getOptionalDependencies()}.
     */
    private final Map<String, String> resolvedEdges;

    private final boolean dev;
    private final boolean optional;
    private final boolean devOptional;

    public ResolvedNode(VersionManifest manifest, Map<String, String> resolvedEdges) {
        this(manifest, resolvedEdges, false, false, false);
    }

    public ResolvedNode(VersionManifest manifest, Map<String, String> resolvedEdges,
                        boolean dev, boolean optional, boolean devOptional) {
        this.manifest = manifest;
        this.resolvedEdges = Collections.unmodifiableMap(resolvedEdges);
        this.dev = dev;
        this.optional = optional;
        this.devOptional = devOptional;
    }

    public String getName() {
        return manifest.getName();
    }

    public String getVersion() {
        return manifest.getVersion();
    }
}
