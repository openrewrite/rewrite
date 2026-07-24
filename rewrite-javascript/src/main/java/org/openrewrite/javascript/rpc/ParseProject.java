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
 * RPC request to parse an entire JavaScript/TypeScript project.
 * Discovers and parses all relevant source files, package.json files, and lock files.
 */
@Value
class ParseProject implements RpcRequest {
    /**
     * Path to the project directory to parse.
     */
    Path projectPath;

    /**
     * Optional glob patterns to exclude from parsing.
     * If not provided, default exclusions (node_modules, dist, etc.) will be used.
     */
    @Nullable
    List<String> exclusions;

    /**
     * Optional path to make source file paths relative to.
     * If not specified, paths are relative to projectPath.
     * Use this when parsing a subdirectory but wanting paths relative to the repository root.
     */
    @Nullable
    Path relativeTo;

    /**
     * The incremental "changed files" channel (see {@code ParseProjectOptions#files}): the subset of source
     * files to serialize and return, relative to {@link #relativeTo} (or {@link #projectPath} when
     * {@code relativeTo} is {@code null}), normalized to forward slashes.
     * <p>
     * {@code null} = a full parse of the whole project (the default, original behavior). Non-{@code null} =
     * the server still loads and type-checks the entire project for full type context, but returns only this
     * set. The caller decides full vs. incremental and escalates to a full parse on configuration-input
     * changes; the server never auto-upgrades.
     */
    @Nullable
    List<String> files;

    /**
     * Forward-compatibility carrier for additional, as-yet-undefined parsing options — reserved, today
     * always {@code null} and ignored by the server. Frozen into the wire shape now so a future addition
     * (e.g. stateful-session control or true-incremental re-parsing hints) needs no second breaking change
     * to the request type.
     */
    @Nullable
    Map<String, Object> options;

    ParseProject(Path projectPath) {
        this(projectPath, null, null, null, null);
    }

    ParseProject(Path projectPath, @Nullable List<String> exclusions) {
        this(projectPath, exclusions, null, null, null);
    }

    ParseProject(Path projectPath, @Nullable List<String> exclusions, @Nullable Path relativeTo) {
        this(projectPath, exclusions, relativeTo, null, null);
    }

    ParseProject(Path projectPath, @Nullable List<String> exclusions, @Nullable Path relativeTo, @Nullable List<String> files) {
        this(projectPath, exclusions, relativeTo, files, null);
    }

    ParseProject(Path projectPath, @Nullable List<String> exclusions, @Nullable Path relativeTo, @Nullable List<String> files, @Nullable Map<String, Object> options) {
        this.projectPath = projectPath;
        this.exclusions = exclusions;
        this.relativeTo = relativeTo;
        this.files = files;
        this.options = options;
    }
}
