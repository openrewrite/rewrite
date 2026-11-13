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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Port of @npmcli/arborist lib/peer-entry-sets.js (npm 11.17.0): the peer sets a node is part of, by the
 * edge (a top or non-peer edge) that enters each.
 */
final class PeerEntrySets {

    private PeerEntrySets() {
    }

    static Map<Edge, Set<Node>> peerEntrySets(Node node) {
        // each Set below is iterated as JavaScript does, visiting the members added while it runs
        Set<Node> unionSet = new LinkedHashSet<>();
        List<Node> union = new ArrayList<>();
        add(unionSet, union, node);
        for (int i = 0; i < union.size(); i++) {
            Node n = union.get(i);
            for (Edge edge : n.getEdgesOut().values()) {
                if (edge.isValid() && edge.isPeer() && edge.getTo() != null) {
                    add(unionSet, union, edge.getTo());
                }
            }
            for (Edge edge : n.getEdgesIn()) {
                if (edge.isValid() && edge.isPeer()) {
                    add(unionSet, union, edge.getFrom());
                }
            }
        }

        Map<Edge, Set<Node>> entrySets = new LinkedHashMap<>();
        for (Node peer : union) {
            for (Edge edge : peer.getEdgesIn()) {
                if (!edge.isValid()) {
                    continue;
                }
                if (!edge.isPeer() || edge.getFrom().isTop()) {
                    Set<Node> sub = new LinkedHashSet<>();
                    List<Node> subOrder = new ArrayList<>();
                    add(sub, subOrder, peer);
                    for (int i = 0; i < subOrder.size(); i++) {
                        for (Edge e : subOrder.get(i).getEdgesOut().values()) {
                            if (e.isValid() && e.isPeer() && e.getTo() != null) {
                                add(sub, subOrder, e.getTo());
                            }
                        }
                    }
                    if (sub.contains(node)) {
                        entrySets.put(edge, sub);
                    }
                }
            }
        }
        return entrySets;
    }

    private static void add(Set<Node> set, List<Node> order, Node node) {
        if (set.add(node)) {
            order.add(node);
        }
    }
}
