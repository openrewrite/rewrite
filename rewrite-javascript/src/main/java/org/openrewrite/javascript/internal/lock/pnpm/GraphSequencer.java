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

import lombok.Value;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Port of @pnpm/deps.graph-sequencer src/index.ts (pnpm 11.2.2), with @pnpm/workspace.projects-sorter's
 * {@code sequenceGraph}.
 */
final class GraphSequencer {

    private GraphSequencer() {
    }

    @Value
    static class Result<T> {
        boolean safe;
        List<List<T>> chunks;
        List<List<T>> cycles;
    }

    /**
     * projects-sorter {@code sequenceGraph}: each project's dependencies on other projects, sequenced.
     */
    static Result<String> sequenceGraph(Map<String, List<String>> projectsGraph) {
        Set<String> setOfKeys = projectsGraph.keySet();
        Map<String, List<String>> graph = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> project : projectsGraph.entrySet()) {
            List<String> dependencies = new ArrayList<>();
            for (String d : project.getValue()) {
                if (!d.equals(project.getKey()) && setOfKeys.contains(d)) {
                    dependencies.add(d);
                }
            }
            graph.put(project.getKey(), dependencies);
        }
        return graphSequencer(graph, new ArrayList<>(projectsGraph.keySet()));
    }

    static <T> Result<T> graphSequencer(Map<T, List<T>> graph) {
        return graphSequencer(graph, new ArrayList<>(graph.keySet()));
    }

    static <T> Result<T> graphSequencer(Map<T, List<T>> graph, List<T> includedNodes) {
        Map<T, List<T>> reverseGraph = new LinkedHashMap<>();
        for (T key : graph.keySet()) {
            reverseGraph.put(key, new ArrayList<>());
        }
        Set<T> nodes = new LinkedHashSet<>(includedNodes);
        Set<T> visited = new LinkedHashSet<>();
        Map<T, Integer> outDegree = new LinkedHashMap<>();
        for (Map.Entry<T, List<T>> entry : graph.entrySet()) {
            T from = entry.getKey();
            outDegree.put(from, 0);
            for (T to : entry.getValue()) {
                if (nodes.contains(from) && nodes.contains(to)) {
                    outDegree.merge(from, 1, Integer::sum);
                    reverseGraph.get(to).add(from);
                }
            }
            if (!nodes.contains(from)) {
                visited.add(from);
            }
        }

        List<List<T>> chunks = new ArrayList<>();
        List<List<T>> cycles = new ArrayList<>();
        boolean safe = true;
        while (!nodes.isEmpty()) {
            List<T> chunk = new ArrayList<>();
            int minDegree = Integer.MAX_VALUE;
            for (T node : nodes) {
                int degree = outDegree.get(node);
                if (degree == 0) {
                    chunk.add(node);
                }
                minDegree = Math.min(minDegree, degree);
            }
            if (minDegree == 0) {
                for (T node : chunk) {
                    removeNode(node, reverseGraph, outDegree, visited, nodes);
                }
                chunks.add(chunk);
            } else {
                List<T> cycleNodes = new ArrayList<>();
                for (T node : new ArrayList<>(nodes)) {
                    if (!nodes.contains(node)) {
                        continue;
                    }
                    List<T> cycle = findCycle(node, graph, visited);
                    if (!cycle.isEmpty()) {
                        cycles.add(cycle);
                        for (T c : cycle) {
                            removeNode(c, reverseGraph, outDegree, visited, nodes);
                        }
                        cycleNodes.addAll(cycle);
                        if (cycle.size() > 1) {
                            safe = false;
                        }
                    }
                }
                chunks.add(cycleNodes);
            }
        }
        return new Result<>(safe, chunks, cycles);
    }

    private static <T> void removeNode(T node, Map<T, List<T>> reverseGraph, Map<T, Integer> outDegree,
                                       Set<T> visited, Set<T> nodes) {
        for (T from : reverseGraph.get(node)) {
            outDegree.merge(from, -1, Integer::sum);
        }
        visited.add(node);
        nodes.remove(node);
    }

    @Value
    private static class Path<T> {
        T id;
        List<T> cycle;
    }

    private static <T> List<T> findCycle(T startNode, Map<T, List<T>> graph, Set<T> visited) {
        Deque<Path<T>> queue = new ArrayDeque<>();
        List<T> start = new ArrayList<>();
        start.add(startNode);
        queue.add(new Path<>(startNode, start));
        Set<T> cycleVisited = new LinkedHashSet<>();
        List<List<T>> cycles = new ArrayList<>();
        while (!queue.isEmpty()) {
            Path<T> head = queue.poll();
            List<T> cycle = head.getCycle();
            for (T to : graph.get(head.getId())) {
                if (to.equals(startNode)) {
                    cycleVisited.add(to);
                    cycles.add(new ArrayList<>(cycle));
                    continue;
                }
                if (visited.contains(to) || cycleVisited.contains(to)) {
                    continue;
                }
                cycleVisited.add(to);
                List<T> next = new ArrayList<>(cycle);
                next.add(to);
                queue.add(new Path<>(to, next));
            }
        }
        if (cycles.isEmpty()) {
            return new ArrayList<>();
        }
        cycles.sort((a, b) -> b.size() - a.size());
        return cycles.get(0);
    }
}
