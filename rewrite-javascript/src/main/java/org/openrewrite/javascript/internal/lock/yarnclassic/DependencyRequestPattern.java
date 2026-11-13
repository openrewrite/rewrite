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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Port of yarn src/types.js {@code DependencyRequestPattern} (yarn 1.22.22), equal only to itself as in yarn's sets.
 */
@Getter
@Builder(toBuilder = true)
final class DependencyRequestPattern {
    private final String pattern;

    private final String registry;

    private final boolean optional;

    /**
     * {@code dev}, {@code optional}, {@code resolution}, {@code workspaces} or none.
     */
    private final @Nullable String hint;

    private final @Nullable List<String> parentNames;

    private final @Nullable PackageRequest parentRequest;

    private final @Nullable String workspaceName;

    private final @Nullable String workspaceLoc;
}
