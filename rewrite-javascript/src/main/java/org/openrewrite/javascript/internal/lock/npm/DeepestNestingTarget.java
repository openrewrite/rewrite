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
package org.openrewrite.javascript.internal.lock.npm;

import org.jspecify.annotations.Nullable;

/**
 * Port of @npmcli/arborist lib/deepest-nesting-target.js (npm 11.17.0).
 */
final class DeepestNestingTarget {

    private DeepestNestingTarget() {
    }

    /**
     * @param name {@code null} where arborist passes none, which no edge is found by.
     */
    static Node deepestNestingTarget(Node start, @Nullable String name) {
        for (Node target = start; ; target = target.getResolveParent()) {
            if (target.isProjectRoot() || target.getResolveParent() == null) {
                return target;
            }
            Edge targetEdge = target.getEdgesOut().get(name);
            if (targetEdge == null || !targetEdge.isPeer()) {
                return target;
            }
        }
    }
}
