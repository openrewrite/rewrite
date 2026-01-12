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
package org.openrewrite.python.rpc;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.rpc.request.RpcRequest;

import java.nio.file.Path;
import java.util.List;

/**
 * RPC request to parse an entire Python project.
 * Discovers and parses all relevant source files.
 */
@Value
class ParseProject implements RpcRequest {
    /**
     * Path to the project directory to parse.
     */
    Path projectPath;

    /**
     * Optional glob patterns to exclude from parsing.
     * If not provided, default exclusions (__pycache__, .venv, etc.) will be used.
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
}
