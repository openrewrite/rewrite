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
package org.openrewrite.javascript.internal.lock.pnpm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Port of graph-cycles 3.0.0 {@code analyzeGraph}, the cycles it finds (deduplicated by rotation as
 * rotated-array-set 3.0.0 does); pnpm reads nothing else of its result.
 */
final class GraphCycles {

    private GraphCycles() {
    }

    static List<List<String>> cycles(Map<String, List<String>> graph) {
        Map<String, List<String>> graphMap = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : graph.entrySet()) {
            graphMap.put(entry.getKey(), new ArrayList<>(new LinkedHashSet<>(entry.getValue())));
        }
        Set<String> cycleNodes = new HashSet<>();
        List<List<String>> cycles = new ArrayList<>();
        for (Map.Entry<String, List<String>> start : graphMap.entrySet()) {
            List<String> path = new ArrayList<>(Collections.singletonList(start.getKey()));
            Set<String> visited = new HashSet<>();
            boolean createdCycle = false;
            List<String> to = start.getValue();
            int nodeNextIndex = 0;
            while (true) {
                String leaf = path.get(path.size() - 1);
                boolean stop = false;
                if (!createdCycle && cycleNodes.contains(leaf)) {
                    stop = true;
                } else if (visited.contains(leaf)) {
                    List<String> cycle = new ArrayList<>(path.subList(path.indexOf(leaf), path.size() - 1));
                    if (addRotated(cycles, cycle)) {
                        cycleNodes.addAll(cycle);
                    }
                    createdCycle = true;
                    stop = true;
                }
                if (!stop && nodeNextIndex < to.size()) {
                    visited.add(leaf);
                    String nextLeaf = to.get(nodeNextIndex);
                    path.add(nextLeaf);
                    List<String> next = graphMap.get(nextLeaf);
                    to = next == null ? Collections.<String>emptyList() : next;
                    nodeNextIndex = 0;
                    continue;
                }
                boolean done = false;
                while (true) {
                    if (path.size() == 1) {
                        done = true;
                        break;
                    }
                    String lastLeaf = path.remove(path.size() - 1);
                    String node = path.get(path.size() - 1);
                    visited.remove(node);
                    List<String> siblings = graphMap.get(node);
                    to = siblings == null ? Collections.<String>emptyList() : siblings;
                    nodeNextIndex = to.indexOf(lastLeaf) + 1;
                    if (nodeNextIndex < to.size()) {
                        visited.add(node);
                        String nextLeaf = to.get(nodeNextIndex);
                        path.add(nextLeaf);
                        List<String> next = graphMap.get(nextLeaf);
                        to = next == null ? Collections.<String>emptyList() : next;
                        nodeNextIndex = 0;
                        break;
                    }
                }
                if (done) {
                    break;
                }
            }
        }
        return cycles;
    }

    private static boolean addRotated(List<List<String>> cycles, List<String> cycle) {
        for (List<String> existing : cycles) {
            if (rotatedArrayEqual(existing, cycle)) {
                return false;
            }
        }
        cycles.add(cycle);
        return true;
    }

    private static boolean rotatedArrayEqual(List<String> a, List<String> b) {
        if (a.size() != b.size()) {
            return false;
        }
        if (a.isEmpty()) {
            return true;
        }
        int offset = b.indexOf(a.get(0));
        if (offset == -1) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).equals(b.get((offset + i) % b.size()))) {
                return false;
            }
        }
        return true;
    }
}
