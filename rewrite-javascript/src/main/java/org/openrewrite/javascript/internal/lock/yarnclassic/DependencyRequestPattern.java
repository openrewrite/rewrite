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
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Port of yarn src/types.js {@code DependencyRequestPattern} (yarn 1.22.22): one request the install or a package
 * makes for a pattern. A root request has no parent names.
 */
@Value
@Builder(toBuilder = true)
class DependencyRequestPattern {
    String pattern;

    String registry;

    boolean optional;

    /**
     * {@code dev}, {@code optional}, {@code resolution}, {@code workspaces} or none.
     */
    @Nullable
    String hint;

    @Nullable
    List<String> parentNames;

    @Nullable
    PackageRequest parentRequest;

    @Nullable
    String workspaceName;

    @Nullable
    String workspaceLoc;
}
