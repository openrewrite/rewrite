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
}
