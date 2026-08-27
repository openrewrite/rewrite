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
package org.openrewrite.python.internal.poetrylock;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * One {@code [[package]]} entry. Scalar keys are emitted first in the order name, version,
 * description, optional, python-versions, groups, markers, files, develop; then the sub-tables
 * {@code [package.dependencies]}, {@code [package.extras]}, {@code [package.source]}.
 */
@Value
@With
@Builder(toBuilder = true)
public class PoetryLockPackage {
    String name;
    String version;
    String description;
    boolean optional;
    String pythonVersions;
    List<String> groups;

    /** Package-level environment marker, single-string form (mutually exclusive with {@link #groupMarkers}). */
    @Nullable String marker;

    /** Per-group markers inline-table form {@code markers = { main = "...", dev = "..." }}. */
    @Nullable Map<String, String> groupMarkers;

    List<PoetryLockFile> files;

    /** Present (as {@code develop = true|false}) only for git/directory sources. */
    @Nullable Boolean develop;

    @Nullable List<PoetryLockDependency> dependencies;

    /** {@code [package.extras]}: extra name → PEP 508 dependency specs (poetry-rendered). */
    @Nullable Map<String, List<String>> extras;

    @Nullable PoetryLockSource source;
}
