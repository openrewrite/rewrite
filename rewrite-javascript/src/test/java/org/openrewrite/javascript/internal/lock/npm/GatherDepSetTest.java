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

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.obj;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.opts;

/**
 * Port of @npmcli/arborist test/gather-dep-set.js.
 */
class GatherDepSetTest {

    private static final String[][] CHILDREN = {
      {"a", "x", "y"}, {"x", "b"}, {"y", "i"}, {"b", "i", "j"}, {"i", "j", "k"}, {"j", "k"}, {"k"},
      {"c", "r", "s"}, {"r", "s", "t"}, {"s", "t"}, {"t", "missing"}
    };

    private final Node tree;

    GatherDepSetTest() {
        Node.Options.OptionsBuilder root = opts().path("/path/to/tree")
          .pkg(obj("dependencies", obj("a", "", "b", "", "c", "")));
        for (String[] child : CHILDREN) {
            Map<String, Object> deps = obj();
            for (int i = 1; i < child.length; i++) {
                deps.put(child[i], "");
            }
            root.child(opts().pkg(obj("name", child[0], "version", "1.0.0", "dependencies", deps)).build());
        }
        tree = new Node(root.build());
    }

    private Set<Node> gather(String... names) {
        Predicate<Edge> f = edge -> edge.getFrom() != tree && edge.getTo() != tree;
        return GatherDepSet.gatherDepSet(Arrays.stream(names).map(n -> tree.getChildren().get(n)).collect(toList()), f);
    }

    // the names are single lowercase letters, where localeCompare('en') is plain string order
    private static List<String> printSet(Set<Node> set) {
        return set.stream().sorted(Comparator.comparing(Node::getName))
          .map(Node::getLocation).collect(toList());
    }

    @Test
    void setWithA() {
        Set<Node> setA = gather("a");
        assertThat(printSet(setA)).containsExactly("node_modules/a", "node_modules/b", "node_modules/i",
          "node_modules/j", "node_modules/k", "node_modules/x", "node_modules/y");
        assertThat(setA).contains(tree.getChildren().get("b"), tree.getChildren().get("x"))
          .doesNotContain(tree.getChildren().get("c"));
    }

    @Test
    void setWithAAndX() {
        Set<Node> setAX = gather("a", "x");
        assertThat(printSet(setAX)).containsExactly("node_modules/a", "node_modules/b", "node_modules/i",
          "node_modules/j", "node_modules/k", "node_modules/x", "node_modules/y");
        assertThat(setAX).contains(tree.getChildren().get("b")).doesNotContain(tree.getChildren().get("c"));
    }

    @Test
    void setWithB() {
        assertThat(gather("b")).isEmpty();
    }

    @Test
    void setWithCOnly() {
        Set<Node> setC = gather("c");
        assertThat(printSet(setC))
          .containsExactly("node_modules/c", "node_modules/r", "node_modules/s", "node_modules/t");
        assertThat(setC).contains(tree.getChildren().get("c"))
          .doesNotContainAnyElementsOf(Collections.singletonList(tree.getChildren().get("a")))
          .doesNotContain(tree.getChildren().get("b"), tree.getChildren().get("x"));
    }
}
