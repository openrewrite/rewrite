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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * deps/graph-sequencer/test/index.ts of pnpm 11.2.2; a graph is written {@code a>b,c b> c>b}, each node with its
 * edges.
 */
class GraphSequencerTest {

    private static void assertSequence(String graph, @Nullable String included, boolean safe, String chunks,
                                       String cycles) {
        Map<String, List<String>> g = new LinkedHashMap<>();
        for (String node : graph.split(" ")) {
            String[] parts = node.split(">", -1);
            g.put(parts[0], parts[1].isEmpty() ? new ArrayList<>() : Arrays.asList(parts[1].split(",")));
        }
        GraphSequencer.Result<String> result = included == null ? GraphSequencer.graphSequencer(g) :
          GraphSequencer.graphSequencer(g, Arrays.asList(included.split(",")));
        assertThat(result.isSafe()).as("safe").isEqualTo(safe);
        assertThat(result.getChunks().toString()).as("chunks").isEqualTo(chunks);
        assertThat(result.getCycles().toString()).as("cycles").isEqualTo(cycles);
    }

    @Test
    void graphWithThreeIndependentSelfCycles() {
        assertSequence("a>a b>b c>c", null, true, "[[a, b, c]]", "[[a], [b], [c]]");
    }

    @Test
    void graphWithSelfCycleSequencingASubgraph() {
        assertSequence("a>a b>b c>c", "a,b", true, "[[a, b]]", "[[a], [b]]");
    }

    @Test
    void graphWithTwoSelfCyclesAndAnEdgeLinkingThem() {
        assertSequence("a>b,c b>b c>b,c", null, true, "[[b, c], [a]]", "[[b], [c]]");
    }

    @Test
    void graphWithNodesConnectedSequentiallyWithoutACycle() {
        assertSequence("a>b,c b> c>b", null, true, "[[b], [c], [a]]", "[]");
    }

    @Test
    void graphSequencingASubsetOfThreeNodesIgnoringTwo() {
        assertSequence("a>b,c b> c> d>a e>a,b,c", "a,d,e", true, "[[a], [d, e]]", "[]");
    }

    @Test
    void graphWithNoEdges() {
        assertSequence("a> b> c> d>", null, true, "[[a, b, c, d]]", "[]");
    }

    @Test
    void graphOfIsolatedNodesSequencingASubgraph() {
        assertSequence("a> b> c> d>", "a,b,c", true, "[[a, b, c]]", "[]");
    }

    @Test
    void graphWithMultipleDependenciesOnOneItem() {
        assertSequence("a>d b>d c> d>", null, true, "[[c, d], [a, b]]", "[]");
    }

    @Test
    void graphWithResolvedCycle() {
        assertSequence("a>b b>c c>d d>a", null, false, "[[a, b, c, d]]", "[[a, b, c, d]]");
    }

    @Test
    void graphWithACycleSequencingASubgraphThatAvoidsIt() {
        assertSequence("a>b b>c c>d d>a", "a,b,c", true, "[[c], [b], [a]]", "[]");
    }

    @Test
    void graphWithResolvedCycleWithMultipleUnblockedDeps() {
        assertSequence("a>d b>d c>d d>a", null, false, "[[a, d], [b, c]]", "[[a, d]]");
    }

    @Test
    void graphWithResolvedCycleWithMultipleUnblockedDepsSubgraph() {
        assertSequence("a>d b>d c>d d>a", "a,b,c", true, "[[a, b, c]]", "[]");
    }

    @Test
    void graphWithTwoCycles() {
        assertSequence("a>b b>a c>d d>c", null, false, "[[a, b, c, d]]", "[[a, b], [c, d]]");
    }

    @Test
    void graphWithMultipleCyclesCase1() {
        assertSequence("a>c b>a,d c>b d>c,e e>", null, false, "[[e], [a, c, b], [d]]", "[[a, c, b]]");
    }

    @Test
    void graphWithMultipleCyclesCase2() {
        assertSequence("a>b b>d c> d>b,c", null, false, "[[c], [b, d], [a]]", "[[b, d]]");
    }

    @Test
    void graphWithFullyConnectedSubgraphAndAdditionalConnectedNode() {
        assertSequence("a>b,c,d b>a,c,d c>a,b,d d>a,b,c e>b", null, false, "[[a, b, c, d], [e]]",
          "[[a, b], [c, d]]");
    }

    @Test
    void graphWithFullyConnectedSubgraphCase1() {
        assertSequence("a>b,c,d b>a,c,d c>a,b,d d>a,b,c e>b", "b,e", true, "[[b], [e]]", "[]");
    }

    @Test
    void graphWithFullyConnectedSubgraphCase2() {
        assertSequence("a>b,c,d b>a,c,d c>a,b,d d>a,b,c e>b", "a,b,e", false, "[[a, b], [e]]", "[[a, b]]");
    }

    @Test
    void graphWithTwoSelfCycles() {
        assertSequence("a>b,c b>b c>c", null, true, "[[b, c], [a]]", "[[b], [c]]");
    }

    @Test
    void graphWithTwoSelfCyclesSequencingASubgraph() {
        assertSequence("a>b,c b>b c>c", "b,c", true, "[[b, c]]", "[[b], [c]]");
    }

    @Test
    void graphWithManyNodes() {
        assertSequence("a>b,c b> c> d>a e>a,b,c", null, true, "[[b, c], [a], [d, e]]", "[]");
    }

    @Test
    void graphWithManyNodesSequencingASubgraph() {
        assertSequence("a>b,c b> c> d>a e>a,b,c", "a,d,e", true, "[[a], [d, e]]", "[]");
    }

    @Test
    void graphWithBigCycle() {
        assertSequence("a>b b>a,c c>a,b", null, false, "[[a, b, c]]", "[[a, b, c]]");
    }

    @Test
    void graphWithThreeCycles() {
        assertSequence("a>b b>a,c c>a,b e>f f>e g>g", null, false, "[[a, b, c, e, f, g]]",
          "[[a, b, c], [e, f], [g]]");
    }
}
