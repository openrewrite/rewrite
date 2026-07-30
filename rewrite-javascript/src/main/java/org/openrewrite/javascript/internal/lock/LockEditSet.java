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
 * The proven-safe set of edits a {@link LockPatcher} applies to a lock file. The engine has already
 * run the closure-safe proof, so a patcher may apply these edits without re-resolving: it only rewrites
 * the named entries and leaves every other byte untouched.
 * <p>
 * Fields that only some formats need are nullable: {@code newShasum}/{@code oldConstraint} are yarn-classic
 * concerns, {@code importerDir} is a workspace concern, and {@code writeThroughMetadata} carries the
 * non-layout-affecting deltas (engines/license/deprecated/bin) the proof lets through instead of failing loud.
 */
@Value
public class LockEditSet {

    /** The raw existing lock content, exactly as captured — the patcher's byte-preservation baseline. */
    String existingLockContent;

    /** Path of the lock file, for failure attribution and workspace-member disambiguation. */
    Path lockPath;

    PackageManager packageManager;

    /** The edited {@code package.json} content, for patchers that re-pin an importer's declared constraint. */
    String editedPackageJson;

    List<PackageEdit> edits;

    /**
     * A single package moving from {@code oldVersion} to {@code newVersion}. A removal is expressed with
     * {@code newVersion == null} (the entry and its byte-exact orphans are dropped by the patcher).
     */
    @Value
    @Builder
    public static class PackageEdit {
        String name;

        String oldVersion;

        /** The resolved target version, or {@code null} for a removal. */
        @Nullable
        String newVersion;

        /** Format-specific resolved locator (npm tarball URL, yarn {@code url#sha1}, …); may be omitted (pnpm default registry). */
        @Nullable
        String newResolved;

        /** {@code dist.integrity} (SRI) for the new version, when the format records it. */
        @Nullable
        String newIntegrity;

        /** {@code dist.shasum} (sha1) for the new version — yarn-classic records this in its {@code resolved} suffix. */
        @Nullable
        String newShasum;

        /** The new version's {@code dependencies} (name → constraint). Unchanged from the old by construction of the proof. */
        @Nullable
        Map<String, String> newDependencies;

        /** The new version's {@code optionalDependencies}. */
        @Nullable
        Map<String, String> newOptionalDependencies;

        /** Non-layout-affecting deltas the proof allows to be written through rather than failing loud. */
        @Nullable
        WriteThroughMetadata writeThroughMetadata;

        /**
         * The pre-edit declared constraint, so yarn-classic can split a moving selector out of a merged header
         * ({@code "a@^1", "a@~1.2":}) instead of renaming the whole header.
         */
        @Nullable
        String oldConstraint;

        /** The declared scope this dependency was matched in (e.g. {@code dependencies}, {@code devDependencies}). */
        String scope;

        /** The workspace member directory owning this dependency, or {@code null} for the root importer. */
        @Nullable
        String importerDir;

        /**
         * A brand-new dependency the recipe added (Phase B). The patcher inserts a new entry rather than
         * rewriting an existing one; {@code oldVersion} is empty. Only a scalar-only leaf is emitted so far.
         */
        boolean added;

        /**
         * A transitive forced to move by a direct-dependency bump whose changed edges no longer satisfy its
         * pinned version (Phase B cascade). pnpm keys by resolved version, so the patcher renames the moved
         * entry and retargets every snapshot reference; other formats update the placement in place.
         */
        boolean forcedMove;

        /**
         * The dependent package a second, nested copy of this package is placed under (Phase B I5). A direct
         * bump takes the top-level slot; a reverse-dependent whose recorded constraint excludes the new version
         * keeps the old version nested at {@code node_modules/<nestedUnder>/node_modules/<name>} (v2 also under
         * the legacy tree). The patcher relocates the pre-edit entry byte-for-byte — {@code oldVersion} names it.
         */
        @Nullable
        String nestedUnder;
    }

    /**
     * The metadata tier a real {@code npm}/{@code pnpm} bump patches cleanly without reshaping the tree, so the
     * engine writes the new values through instead of failing loud. Only fields that actually changed are non-null.
     * For a leaf add (Phase B I1-follow) the object/array fields below carry the leaf's serialized metadata; the
     * engine has already normalized/vetted them, so the patcher renders each value as-is at npm's indentation.
     */
    @Value
    @Builder
    public static class WriteThroughMetadata {
        @Nullable
        Map<String, String> engines;

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

        /** {@code funding} node, already normalized by the engine to npm's object form ({@code {url: ...}}). */
        @Nullable
        JsonNode funding;

        /** A closure member's {@code peerDependencies} map, copied verbatim into the lock entry (object group). */
        @Nullable
        Map<String, String> peerDependencies;

        /** Raw {@code peerDependenciesMeta} node ({@code {name: {optional: bool}}}), copied verbatim (object group). */
        @Nullable
        JsonNode peerDependenciesMeta;
    }
}
