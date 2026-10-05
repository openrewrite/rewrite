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
 * Port of @npmcli/arborist lib/from-path.js (npm 11.17.0).
 */
final class FromPath {

    private FromPath() {
    }

    static @Nullable String fromPath(Node node, @Nullable Edge edge) {
        if (edge != null && edge.getOverrides() != null && edge.getName().equals(edge.getOverrides().getName()) &&
            edge.getOverrides().getValue() != null && !edge.getOverrides().getValue().isEmpty()) {
            if (node.getSourceReference() != null) {
                return node.getSourceReference().getRoot().getRealpath();
            }
            return node.getRoot().getRealpath();
        }

        String resolved = node.getResolved();
        if (resolved != null && !resolved.isEmpty()) {
            Spec spec = Spec.npa(resolved);
            if (spec.getType() == Spec.Type.FILE) {
                return PosixPath.dirname(spec.getFetchSpec());
            }
        }
        return node.getRealpath();
    }
}
