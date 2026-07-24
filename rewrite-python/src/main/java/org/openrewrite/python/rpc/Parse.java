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
package org.openrewrite.python.rpc;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.rpc.request.RpcRequest;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * RPC request to parse an explicit list of Python files.
 * <p>
 * Unlike {@link ParseProject}, which walks a directory, this request parses exactly the
 * files it is given. The key capability is that {@code ty} (the type resolver) is rooted
 * at {@link #relativeTo} rather than at the files' own directory, so first-party imports
 * resolve against a broader workspace root. This lets a caller parse a handful of files
 * (e.g. the {@code .py} files of a single Bazel target) while cross-package first-party
 * imports still resolve against the monorepo root, without parsing the rest of the tree.
 */
@Value
class Parse implements RpcRequest {

    /**
     * The files to parse, each identified by its (absolute or {@link #relativeTo}-relative) path.
     */
    List<Input> inputs;

    /**
     * Project root that {@code ty} is initialized at, so imports resolve relative to it.
     * When {@code null}, the server infers a root from the input paths.
     */
    @Nullable
    Path relativeTo;

    /**
     * Optional path to a virtual environment with the project's dependencies installed.
     * <p>
     * The caller provisions this environment and forwards its path so the parser can point
     * ty-types at it, allowing supertypes that reach into third-party packages to resolve
     * (e.g. a first-party class extending {@code pydantic.BaseModel}). The parser never
     * provisions dependencies itself. When {@code null}, parsing proceeds without
     * dependency-backed type resolution.
     */
    @Nullable
    Path dependencyPath;

    /**
     * Optional, parser-specific options forwarded to the RPC server (e.g.
     * {@code {"languageLevel": "2.7"}}). The handler interprets keys it recognizes and
     * silently ignores the rest. When {@code null}, the handler falls back to its
     * process-wide defaults.
     */
    @Nullable
    Map<String, String> options;

    /**
     * A single file to parse. Serializes to {@code {"path": "<path>"}}, one of the input
     * shapes accepted by the server's {@code Parse} handler.
     */
    @Value
    static class Input {
        Path path;
    }
}
