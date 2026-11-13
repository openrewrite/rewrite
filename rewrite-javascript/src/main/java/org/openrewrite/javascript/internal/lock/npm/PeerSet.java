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
 * Port of @npmcli/arborist lib/peer-set.js (npm 7.20.2), which npm 7.20.3 replaced with peer-entry-sets.js: the nodes
 * a node is tied to by valid peer edges, either way, including those with dependents outside the set.
 */
final class PeerSet {

    private PeerSet() {
    }

    static Set<Node> peerSet(Node node) {
        // a JavaScript Set iterator also visits the members added while it runs
        Set<Node> set = new LinkedHashSet<>();
        List<Node> order = new ArrayList<>();
        set.add(node);
        order.add(node);
        for (int i = 0; i < order.size(); i++) {
            Node n = order.get(i);
            for (Edge edge : n.getEdgesOut().values()) {
                if (edge.isValid() && edge.isPeer() && edge.getTo() != null && set.add(edge.getTo())) {
                    order.add(edge.getTo());
                }
            }
            for (Edge edge : n.getEdgesIn()) {
                if (edge.isValid() && edge.isPeer() && set.add(edge.getFrom())) {
                    order.add(edge.getFrom());
                }
            }
        }
        return set;
    }
}
