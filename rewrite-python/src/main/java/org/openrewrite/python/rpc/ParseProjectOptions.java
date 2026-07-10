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

import lombok.Builder;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

/**
 * Optional inputs to {@link PythonRewriteRpc#parseProject(Path, ParseProjectOptions, org.openrewrite.ExecutionContext)}.
 * <p>
 * Bundles the optional, independent parsing inputs into one value so the API doesn't grow a long
 * tail of positional parameters (and adjacent same-typed arguments that are easy to transpose). Build
 * with {@code builder()}; every field is optional and defaults to {@code null}.
 */
@Value
@Builder
public class ParseProjectOptions {

    /**
     * Glob patterns to exclude from parsing. When {@code null}, the parser's default exclusions
     * ({@code __pycache__}, {@code .venv}, etc.) are used.
     */
    @Nullable
    List<String> exclusions;

    /**
     * Path to make source file paths relative to. When {@code null}, paths are relative to the project
     * path. Use this when parsing a subdirectory but wanting paths relative to the repository root.
     */
    @Nullable
    Path relativeTo;

    /**
     * Path to a virtual environment with the project's dependencies installed.
     * <p>
     * The caller (e.g. a CLI build step) provisions this environment and forwards its path so the
     * parser can point ty-types at it, allowing supertypes that reach into third-party packages to
     * resolve (e.g. a first-party class extending {@code pydantic.BaseModel}). The parser never
     * provisions dependencies itself; when {@code null}, parsing proceeds without dependency-backed
     * type resolution.
     */
    @Nullable
    Path dependencyPath;
}
