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

    ParseProject(Path projectPath) {
        this(projectPath, null);
    }

    ParseProject(Path projectPath, @Nullable List<String> exclusions) {
        this.projectPath = projectPath;
        this.exclusions = exclusions;
    }
}
