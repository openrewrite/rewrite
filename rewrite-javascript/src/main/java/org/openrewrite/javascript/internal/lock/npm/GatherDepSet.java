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
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Port of @npmcli/arborist lib/gather-dep-set.js (npm 11.17.0).
 */
final class GatherDepSet {

    private GatherDepSet() {
    }

    static Set<Node> gatherDepSet(Collection<Node> set, Predicate<Edge> edgeFilter) {
        Set<Node> deps = new LinkedHashSet<>(set);

        // a JavaScript Set iterator also visits the members added while it runs
        List<Node> order = new ArrayList<>(deps);
        for (int i = 0; i < order.size(); i++) {
            for (Edge edge : order.get(i).getEdgesOut().values()) {
                if (edge.getTo() != null && edgeFilter.test(edge) && deps.add(edge.getTo())) {
                    order.add(edge.getTo());
                }
            }
        }

        boolean changed = true;
        while (changed && !deps.isEmpty()) {
            changed = false;
            for (Node dep : new ArrayList<>(deps)) {
                for (Edge edge : dep.getEdgesIn()) {
                    if (!deps.contains(edge.getFrom()) && edgeFilter.test(edge)) {
                        changed = true;
                        deps.remove(dep);
                        break;
                    }
                }
            }
        }
        return deps;
    }
}
