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

import lombok.Builder;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

/**
 * Optional inputs to {@link JavaScriptRewriteRpc#parseProject(Path, ParseProjectOptions, org.openrewrite.ExecutionContext)}.
 * <p>
 * Bundles the optional, independent parsing inputs into one value so the API doesn't grow a long
 * tail of positional parameters (and adjacent same-typed arguments that are easy to transpose). Build
 * with {@link #builder()}; every field is optional and defaults to {@code null}.
 */
@Value
@Builder
public class ParseProjectOptions {

    /**
     * Glob patterns to exclude from parsing. When {@code null}, the parser's default exclusions
     * ({@code node_modules}, {@code dist}, etc.) are used.
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
     * Subset of files to return, identified by their source path relative to {@link #relativeTo} (or the
     * project path when {@code relativeTo} is {@code null}), normalized to forward slashes.
     * <p>
     * When {@code null} the whole project is parsed and returned (the default). When non-{@code null} the
     * parser still loads and type-checks the entire project — so types in the returned files resolve
     * exactly as in a full parse — but only serializes and returns the files in this set. This lets a
     * caller (e.g. a CLI doing in-session incremental re-parsing) refresh just the files that changed
     * while still getting full type context, without paying to serialize every unchanged file.
     * <p>
     * A path that discovery wouldn't have parsed (excluded, not a source file, or missing) simply yields
     * nothing rather than an error.
     */
    @Nullable
    List<String> files;
}
