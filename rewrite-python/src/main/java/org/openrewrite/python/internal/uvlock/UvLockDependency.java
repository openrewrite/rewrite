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
package org.openrewrite.python.internal.uvlock;

import lombok.Builder;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A resolved graph edge in a {@code dependencies}, {@code [package.optional-dependencies]}
 * or {@code [package.dev-dependencies]} array. {@code version} and {@code source} are present
 * only when the name is ambiguous in the file (fork duplicates).
 */
@Value
@With
@Builder(toBuilder = true)
public class UvLockDependency {

    String name;

    @Nullable
    String version;

    @Nullable
    UvLockSource source;

    @Nullable
    List<String> extra;

    /**
     * Marker string exactly as uv emitted it (already simplified/normalized by uv).
     */
    @Nullable
    String marker;

    public static UvLockDependency of(String name) {
        return new UvLockDependency(name, null, null, null, null);
    }
}
