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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Port of @npmcli/arborist lib/optional-set.js (npm 11.17.0).
 */
final class OptionalSet {

    private OptionalSet() {
    }

    static Set<Node> optionalSet(Node node) {
        Set<Node> set = new LinkedHashSet<>();
        set.add(node);
        List<Node> walk = new ArrayList<>(set);
        for (int i = 0; i < walk.size(); i++) {
            for (Edge edge : walk.get(i).getEdgesIn()) {
                if (!edge.isOptional() && set.add(edge.getFrom())) {
                    walk.add(edge.getFrom());
                }
            }
        }
        return GatherDepSet.gatherDepSet(set, edge ->
                !set.contains(edge.getTo()) && !(edge.getFrom() != null && edge.getFrom().isInert()));
    }
}
