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
package org.openrewrite.csharp.rpc;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.rpc.request.RpcRequest;

import java.nio.file.Path;
import java.util.List;

/**
 * RPC request to parse an entire C# project.
 * Reads a .csproj file to discover source files, resolve NuGet references,
 * and include source-generator output from obj/ for complete type attribution.
 */
@Value
class ParseProject implements RpcRequest {
    /**
     * Path to the .csproj file to parse.
     */
    Path projectPath;

    /**
     * Optional glob patterns to exclude from parsing.
     * If not provided, default exclusions (bin, obj user code, etc.) will be used.
     */
    @Nullable
    List<String> exclusions;

    /**
     * Optional path to make source file paths relative to.
     * If not specified, paths are relative to the .csproj directory.
     * Use this when parsing a subdirectory but wanting paths relative to the repository root.
     */
    @Nullable
    Path relativeTo;

    ParseProject(Path projectPath) {
        this(projectPath, null, null);
    }

    ParseProject(Path projectPath, @Nullable List<String> exclusions) {
        this(projectPath, exclusions, null);
    }

    ParseProject(Path projectPath, @Nullable List<String> exclusions, @Nullable Path relativeTo) {
        this.projectPath = projectPath;
        this.exclusions = exclusions;
        this.relativeTo = relativeTo;
    }
}
