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

import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;

/**
 * One inline table in a top-level {@code conflicts} set: {@code { package = "...", extra = "..." }}
 * or {@code { package = "...", group = "..." }}, declaring one member of a mutually-exclusive group.
 * Exactly one of {@code extra}/{@code group} is present.
 */
@Value
@With
public class UvLockConflictItem {

    String packageName;

    @Nullable
    String extra;

    @Nullable
    String group;
}
