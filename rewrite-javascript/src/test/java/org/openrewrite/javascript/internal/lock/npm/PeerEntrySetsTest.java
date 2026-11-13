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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.node;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.obj;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.opts;

/**
 * Port of @npmcli/arborist test/peer-entry-sets.js; the expectations are its snapshots.
 */
class PeerEntrySetsTest {

    @Test
    void basicTest() {
        String version = "1.0.0";
        Node a = node(opts().path("/some/path")
          .pkg(obj("name", "a", "version", version, "dependencies", obj("b", "", "c", "", "d", "")))
          .child(opts().pkg(obj("name", "b", "version", version, "peerDependencies", obj("d", ""))).build())
          .child(opts().pkg(obj("name", "c", "version", version, "peerDependencies", obj("f", "", "h", ""))).build())
          .child(opts().pkg(obj("name", "d", "version", version, "peerDependencies", obj("e", ""))).build())
          .child(opts().pkg(obj("name", "e", "version", version, "peerDependencies", obj("f", ""))).build())
          .child(opts().pkg(obj("name", "f", "version", version, "peerDependencies", obj("g", ""))).build())
          .child(opts().pkg(obj("name", "g", "version", version, "dependencies", obj("c", "2"),
            "peerDependencies", obj("f", ""))).build())
          .child(opts().pkg(obj("name", "h", "version", version, "peerDependencies", obj("g", "", "d", "2")))
            .build()));

        String bSet = "b d e f g";
        String cSet = "c f h g";
        String dSet = "d e f g";
        assertThat(format(a, "b")).containsExactly(entry("b", bSet));
        assertThat(format(a, "c")).containsExactly(entry("c", cSet));
        assertThat(format(a, "d")).containsExactly(entry("d", dSet), entry("b", bSet));
        assertThat(format(a, "e")).containsExactly(entry("d", dSet), entry("b", bSet));
        assertThat(format(a, "f")).containsExactly(entry("c", cSet), entry("d", dSet), entry("b", bSet));
        assertThat(format(a, "g")).containsExactly(entry("c", cSet), entry("d", dSet), entry("b", bSet));
        assertThat(format(a, "h")).containsExactly(entry("c", cSet));
    }

    // the test's formatSnapshot: each entry edge as [from, type, name, spec], with its nodes as [location, version]
    private static List<String> format(Node root, String name) {
        List<String> entries = new ArrayList<>();
        for (Map.Entry<Edge, Set<Node>> entry : PeerEntrySets.peerEntrySets(root.getChildren().get(name)).entrySet()) {
            Edge edge = entry.getKey();
            List<String> nodes = new ArrayList<>();
            for (Node n : entry.getValue()) {
                nodes.add(n.getLocation() + " " + n.getVersion());
            }
            entries.add("[" + edge.getFrom().getLocation() + ", " + edge.getType() + ", " + edge.getName() + ", " +
                        edge.getSpec() + "] => " + nodes);
        }
        return entries;
    }

    private static String entry(String edgeName, String nodeNames) {
        List<String> nodes = new ArrayList<>();
        for (String n : nodeNames.split(" ")) {
            nodes.add("node_modules/" + n + " 1.0.0");
        }
        return "[, prod, " + edgeName + ", ] => " + nodes;
    }
}
