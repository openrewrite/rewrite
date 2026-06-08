/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.javascript.rpc;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.marker.NodeResolutionResult;

import java.util.List;

/**
 * Result of the {@link Prebuild} RPC: the independent projects discovered in the
 * repository (workspace-aware), each with its source sets, config-input watch-set, and
 * parser settings.
 * <p>
 * v1 is read-only: {@link ProjectDescriptor#resolution} is always null.
 */
@Value
public class PrebuildResult {
    List<ProjectDescriptor> projects;

    /**
     * An independent JavaScript/TypeScript project (a package.json root).
     */
    @Value
    public static class ProjectDescriptor {
        /**
         * package.json directory, relative to the request's {@code relativeTo}, "/"-normalized.
         */
        String path;

        /**
         * Detected package manager. Yarn Classic and Berry are kept distinct, consistent
         * with {@link NodeResolutionResult.PackageManager}.
         */
        NodeResolutionResult.PackageManager packageManager;

        /**
         * Watch-set: files whose change forces a full re-parse (package.json, lock file,
         * tsconfig extends chain + references, prettier/jest/vitest config). Relative,
         * "/"-normalized, deduped.
         */
        List<String> configInputs;

        /**
         * Source sets ("main"/"test"). v1 usually has one ("main") shared parser settings;
         * the list is the forward-compat hook for divergent prod/test settings.
         */
        List<SourceSetDescriptor> sourceSets;

        /**
         * Resolved dependency data. Populated only when install ownership moves into the
         * server; always null in v1.
         */
        @Nullable
        Object resolution;
    }

    /**
     * One source set within a project. Modeled as a list element (not a boolean) so
     * divergent prod/test parser settings are additive later.
     */
    @Value
    public static class SourceSetDescriptor {
        /**
         * "main" or "test".
         */
        String name;

        /**
         * Globs (relative to the project path) selecting this set's files, or null.
         */
        @Nullable
        List<String> includes;

        @Nullable
        List<String> excludes;

        ParserSettings parserSettings;
    }

    /**
     * Resolution-relevant compiler configuration the parser needs. v1 carries only the
     * path to the nearest tsconfig; the type stays open for additive fields.
     */
    @Value
    public static class ParserSettings {
        /**
         * Relative, "/"-normalized path to the nearest tsconfig.json, or null.
         */
        @Nullable
        String tsconfigPath;
    }
}
