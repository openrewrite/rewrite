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
import java.util.Map;

@Value
@Builder
public class ParseOptions {

    /**
     * Path that returned source paths are made relative to. When {@code null}, the server infers
     * one from the input paths.
     */
    @Nullable
    Path relativeTo;

    /**
     * Root that {@code ty} is initialized at, so imports resolve relative to it and a {@code ty.toml}
     * placed there supplies {@code extra-paths} and {@code python-version}. Defaults to
     * {@link #relativeTo}; set it when that config lives outside the tree the sources are in.
     */
    @Nullable
    Path projectRoot;

    /**
     * Path to a virtual environment with the project's dependencies installed, exported to
     * {@code ty} as {@code VIRTUAL_ENV} so supertypes reaching into third-party packages resolve
     * (e.g. a first-party class extending {@code pydantic.BaseModel}). The caller provisions it;
     * the parser never provisions dependencies itself.
     */
    @Nullable
    Path dependencyPath;

    /**
     * Parser-specific options forwarded to the RPC server (e.g. {@code {"languageLevel": "2.7"}}).
     * Keys the server does not recognize are silently ignored.
     */
    @Nullable
    Map<String, String> options;
}
