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
import org.openrewrite.python.DependencyWorkspace;

import java.nio.file.Path;
import java.util.List;

/**
 * Optional inputs to {@link PythonRewriteRpc#parseProject(Path, ParseProjectOptions, org.openrewrite.ExecutionContext)}.
 * <p>
 * Bundles the optional, independent parsing inputs into one value so the API doesn't grow a long
 * tail of positional parameters (and adjacent same-typed arguments that are easy to transpose). Build
 * with {@code builder()}; every field is optional and defaults to {@code null}, except that
 * {@link #provisionDependencies} defaults to {@code true}.
 */
@Value
@Builder
public class ParseProjectOptions {

    /**
     * Glob patterns matched against directory names, not paths. These extend the parser's built-in
     * exclusions ({@code __pycache__}, {@code .venv}, etc.) rather than replacing them.
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
     * Path to a virtual environment with the project's dependencies installed, typically the one
     * {@link DependencyWorkspace#provision} returned. ty-types reads it, so that supertypes reaching
     * into third-party packages resolve (e.g. a first-party class extending {@code pydantic.BaseModel}),
     * and the dependency markers resolve against it. When {@code null}, the parse provisions one itself
     * unless {@link #provisionDependencies} is off.
     */
    @Nullable
    Path dependencyPath;

    /**
     * Whether a parse given no {@link #dependencyPath} installs the project's dependencies itself. A host
     * that provisions the env, and may fail to, turns it off, so that the parse installs nothing.
     */
    @Builder.Default
    boolean provisionDependencies = true;
}
