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
package org.openrewrite.javascript.internal.lock;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * The proven-safe edits a {@link LockPatcher} applies to a lock: it rewrites only the named entries and
 * leaves every other byte untouched, without re-resolving (the engine already ran the closure-safe proof).
 */
@Value
public class LockEditSet {

    /** The raw existing lock, exactly as captured — the patcher's byte-preservation baseline. */
    String existingLockContent;

    /** For failure attribution and workspace-member disambiguation. */
    Path lockPath;

    PackageManager packageManager;

    /** For patchers that re-pin an importer's declared constraint. */
    String editedPackageJson;

    List<PackageEdit> edits;

    /** A single package moving to {@code newVersion}, or a removal when {@code newVersion == null}. */
    @Value
    @Builder(toBuilder = true)
    public static class PackageEdit {
        String name;

        String oldVersion;

        /** The resolved target version, or {@code null} for a removal. */
        @Nullable
        String newVersion;

        /** Format-specific resolved locator (a tarball URL, yarn {@code url#sha1}, …); omitted for pnpm's default registry. */
        @Nullable
        String newResolved;

        /** {@code dist.integrity} (SRI) for the new version, when the format records it. */
        @Nullable
        String newIntegrity;

        /** Yarn Berry's {@code <cacheKey>/<hash>} checksum for the new version, derived from its tarball. */
        @Nullable
        String newBerryChecksum;

        /** The new version's {@code dependencies} (name → constraint). */
        @Nullable
        Map<String, String> newDependencies;

        /** The new version's {@code optionalDependencies}. */
        @Nullable
        Map<String, String> newOptionalDependencies;

        /** Non-layout entry deltas the proof patches in place rather than failing loud. */
        @Nullable
        EntryMetadata metadata;

        /** Pre-edit constraint: the descriptor the entry is keyed by before the edit. */
        @Nullable
        String oldConstraint;

        /** New declared range for a berry forced-move: the requirer's new constraint that re-heads the moved entry's descriptor. */
        @Nullable
        String newConstraint;

        /** pnpm's peer-suffixed form of {@link #newVersion} ({@code version(peer@version)…}) for snapshot keys and references. */
        @Nullable
        String newVersionRef;

        /** The declared scope this dependency was matched in (e.g. {@code dependencies}, {@code devDependencies}). */
        String scope;

        /** The workspace member directory owning this dependency, or {@code null} for the root importer. */
        @Nullable
        String importerDir;

        /** What kind of edit this is; the patcher dispatches on it. A removal is a {@code BUMP} with {@code newVersion == null}. */
        @Builder.Default
        Kind kind = Kind.BUMP;

        /** The bump dropped one or more edges: drop them from this entry, then GC whatever they leave unreachable. */
        boolean prunesOrphans;

        /**
         * ADD is a brand-new dependency. FORCED_MOVE is a transitive a bump pushes to a new version; CONTENT_FORK is
         * pnpm's non-nesting fork; PROMOTION reuses an already-installed transitive.
         */
        public enum Kind { BUMP, ADD, PROMOTION, FORCED_MOVE, CONTENT_FORK }
    }

    /**
     * The metadata a bump patches cleanly without reshaping the tree (engines/license/deprecated/bin, plus a leaf
     * add's serialized metadata). Only changed fields are non-null; the engine has already normalized them.
     */
    @Value
    @Builder(toBuilder = true)
    public static class EntryMetadata {
        @Nullable
        Map<String, String> engines;

        @Nullable
        String license;

        @Nullable
        String deprecated;

        /** {@code bin} object ({@code {name: path}}). */
        @Nullable
        JsonNode bin;

        @Nullable
        List<String> os;

        @Nullable
        List<String> cpu;

        @Nullable
        List<String> libc;

        @Nullable
        Boolean hasInstallScript;

        /** A closure member's {@code peerDependencies} map, copied verbatim (object group). */
        @Nullable
        Map<String, String> peerDependencies;

        /** Raw {@code peerDependenciesMeta} node ({@code {name: {optional: bool}}}), copied verbatim (object group). */
        @Nullable
        JsonNode peerDependenciesMeta;

        /** Entry flag members ({@code dev}/{@code optional}/{@code devOptional}/{@code peer}); written iff {@code TRUE}. */
        @Nullable
        Boolean dev;

        @Nullable
        Boolean optional;

        @Nullable
        Boolean devOptional;

        @Nullable
        Boolean peer;
    }
}
