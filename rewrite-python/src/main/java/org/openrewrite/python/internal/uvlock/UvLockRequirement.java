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
 * A declared-metadata entry in {@code requires-dist} or {@code [package.metadata.requires-dev]}.
 * Name/specifier/marker are stored as uv normalized them; construction of new entries should
 * normalize via {@link UvLockNormalization} before storing.
 */
@Value
@With
@Builder(toBuilder = true)
public class UvLockRequirement {

    String name;

    @Nullable
    List<String> extras;

    @Nullable
    String editable;

    @Nullable
    String marker;

    @Nullable
    String specifier;

    @Nullable
    String index;

    /**
     * Direct-URL source of the requirement (mutually exclusive with {@code specifier}); the
     * download URL without the resolved commit that the package {@code source} carries.
     */
    @Nullable
    String url;

    /**
     * Git source of the requirement, e.g. {@code "https://host/repo?tag=1.2.3"}; the resolved
     * commit lives only on the package {@code source}, not here.
     */
    @Nullable
    String git;

    /**
     * Local directory source of the requirement (a {@code [tool.uv.sources]} path pointing at a
     * directory); uv emits it after {@code marker}, in place of {@code specifier}.
     */
    @Nullable
    String directory;
}
