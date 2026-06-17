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
import org.openrewrite.rpc.request.RpcRequest;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * RPC request to compute the build descriptor for a repository (or partition) root:
 * the independent projects (workspace-aware), their source sets, config-input watch-set,
 * and parser settings. The JavaScript analog of "invoke the build tool".
 * <p>
 * v1 is read-only — no dependency install.
 */
@Value
class Prebuild implements RpcRequest {
    /**
     * Path to the repository (or partition) root to analyze.
     */
    Path repositoryRoot;

    /**
     * Optional glob patterns to exclude from discovery.
     * If not provided, default exclusions (node_modules, dist, etc.) will be used.
     */
    @Nullable
    List<String> exclusions;

    /**
     * Optional base for all returned paths. If not specified, paths are relative to
     * {@link #repositoryRoot}. Use when analyzing a subdirectory but wanting paths
     * relative to the repository root.
     */
    @Nullable
    Path relativeTo;

    /**
     * Forward-compat carrier for future options (e.g. stateful-session hints). Unused in v1.
     */
    @Nullable
    Map<String, Object> options;

    Prebuild(Path repositoryRoot) {
        this(repositoryRoot, null, null, null);
    }

    Prebuild(Path repositoryRoot, @Nullable List<String> exclusions) {
        this(repositoryRoot, exclusions, null, null);
    }

    Prebuild(Path repositoryRoot, @Nullable List<String> exclusions, @Nullable Path relativeTo) {
        this(repositoryRoot, exclusions, relativeTo, null);
    }

    Prebuild(Path repositoryRoot, @Nullable List<String> exclusions, @Nullable Path relativeTo, @Nullable Map<String, Object> options) {
        this.repositoryRoot = repositoryRoot;
        this.exclusions = exclusions;
        this.relativeTo = relativeTo;
        this.options = options;
    }
}
