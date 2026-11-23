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
package org.openrewrite.python.internal.index;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.python.PythonPackageIndex;

/**
 * A package index as uv models it: a {@link PythonPackageIndex} plus uv's
 * {@code default}/{@code explicit}/{@code format = "flat"} flags.
 * <p>
 * {@link UvIndexDiscovery} returns indexes in uv's consultation order: implicit
 * indexes first (highest priority first), then the default index, then any
 * {@code find-links} flat locations. Explicit indexes keep their defined position
 * but are only eligible for packages pinned to them via {@code [tool.uv.sources]};
 * use {@link #usableFor(String)} to filter.
 */
@Value
public class UvIndex {
    PythonPackageIndex index;

    /**
     * Whether the index name was declared in configuration (as opposed to synthesized
     * from the URL). Only declared names participate in {@code [tool.uv.sources]} pinning.
     */
    boolean named;

    /**
     * uv's default index: consulted last, replaces pypi.org when declared.
     */
    boolean defaultIndex;

    /**
     * Only used for packages pinned to this index via {@code [tool.uv.sources]}.
     */
    boolean explicit;

    /**
     * A {@code --find-links}-style flat listing (local directory or HTML page of
     * artifact links) rather than a Simple Repository API index; query it with
     * {@link FlatIndexClient} instead of {@link SimpleIndexClient}.
     */
    boolean flat;

    /**
     * Whether this index may serve the given package, where {@code pinnedIndexName}
     * is the index name the package selects via {@code [tool.uv.sources]}, or null
     * when the package is unpinned. A pinned package resolves exclusively from its
     * pinned index; an unpinned package never resolves from an explicit index.
     */
    public boolean usableFor(@Nullable String pinnedIndexName) {
        if (pinnedIndexName != null) {
            return named && pinnedIndexName.equals(index.getName());
        }
        return !explicit;
    }
}
