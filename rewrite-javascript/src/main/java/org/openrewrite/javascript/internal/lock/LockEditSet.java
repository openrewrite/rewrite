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

        /** Format-specific resolved locator (npm tarball URL, yarn {@code url#sha1}, …); omitted for pnpm's default registry. */
        @Nullable
        String newResolved;

        /** {@code dist.integrity} (SRI) for the new version, when the format records it. */
        @Nullable
        String newIntegrity;

        /** {@code dist.shasum} (sha1) — yarn-classic records this in its {@code resolved} suffix. */
        @Nullable
        String newShasum;

        /** Yarn Berry's {@code <cacheKey>/<hash>} checksum for the new version, derived from its tarball. */
        @Nullable
        String newBerryChecksum;

        /** The new version's {@code dependencies} (name → constraint). */
        @Nullable
        Map<String, String> newDependencies;

        /** The new version's {@code optionalDependencies}. */
        @Nullable
        Map<String, String> newOptionalDependencies;

        /** Non-layout deltas the proof writes through rather than failing loud. */
        @Nullable
        WriteThroughMetadata writeThroughMetadata;

        /** Pre-edit constraint, so yarn-classic can split a moving selector out of a merged header rather than renaming it. */
        @Nullable
        String oldConstraint;

        /** New declared range for a berry forced-move: the requirer's new constraint that re-heads the moved entry's descriptor. */
        @Nullable
        String newConstraint;

        /** The declared scope this dependency was matched in (e.g. {@code dependencies}, {@code devDependencies}). */
        String scope;

        /** The workspace member directory owning this dependency, or {@code null} for the root importer. */
        @Nullable
        String importerDir;

        /** What kind of edit this is; the patcher dispatches on it. A removal is a {@code BUMP} with {@code newVersion == null}. */
        @Builder.Default
        Kind kind = Kind.BUMP;

        /** The dependent a nested copy sits under; set for an {@code ADD}'s nested variant and for {@code REVERSE_NEST}. */
        @Nullable
        String nestedUnder;

        /** The bump dropped one or more edges: drop them from this entry, then GC whatever they leave unreachable. */
        boolean prunesOrphans;

        /** A {@code PROMOTION} of a dev-only transitive now production-reachable clears {@code "dev": true} (leaf only). */
        boolean clearDev;

        /** An add-during-bump: this bump's entry gains new dependency edges whose subtrees are placed as fresh ADDs. */
        boolean addsDependencyEdges;

        /**
         * ADD is a brand-new dependency; ADD with a {@link #nestedUnder} is a fresh nested add. FORCED_MOVE is a
         * transitive a bump pushes to a new version; CONTENT_FORK is pnpm's non-nesting fork; PROMOTION reuses an
         * already-installed transitive; REVERSE_NEST relocates a pre-edit entry under a dependent.
         */
        public enum Kind { BUMP, ADD, PROMOTION, FORCED_MOVE, CONTENT_FORK, REVERSE_NEST }
    }

    /**
     * The metadata a bump patches cleanly without reshaping the tree (engines/license/deprecated/bin/funding, plus a
     * leaf add's serialized metadata). Only changed fields are non-null; the engine has already normalized them.
     */
    @Value
    @Builder(toBuilder = true)
    public static class WriteThroughMetadata {
        @Nullable
        Map<String, String> engines;

        /** Distinguishes an engines removal ({@link #engines} null but changed) from no change. */
        boolean enginesChanged;

        @Nullable
        String license;

        @Nullable
        String deprecated;

        /** {@code bin} object ({@code {name: path}}); the engine fails loud on the string form npm would normalize. */
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

        /** {@code funding}, normalized to npm's object form ({@code {url: ...}}). */
        @Nullable
        JsonNode funding;

        /** Distinguishes a funding removal ({@link #funding} null but changed) from no change. */
        boolean fundingChanged;

        /** A closure member's {@code peerDependencies} map, copied verbatim (object group). */
        @Nullable
        Map<String, String> peerDependencies;

        /** A bump rewrote {@code peerDependencies} (add/widen), so the patcher replaces the entry's field in place. */
        boolean peerDependenciesChanged;

        /** Raw {@code peerDependenciesMeta} node ({@code {name: {optional: bool}}}), copied verbatim (object group). */
        @Nullable
        JsonNode peerDependenciesMeta;
    }
}
