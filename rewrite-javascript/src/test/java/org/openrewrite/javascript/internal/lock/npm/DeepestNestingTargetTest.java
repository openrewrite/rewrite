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

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.internal.lock.npm.DeepestNestingTarget.deepestNestingTarget;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.link;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.node;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.obj;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.opts;

/**
 * Port of @npmcli/arborist test/deepest-nesting-target.js.
 */
class DeepestNestingTargetTest {

    private static Node.Options a() {
        return opts().pkg(obj("name", "a", "version", "1.2.3", "dependencies", obj("x", ""),
            "peerDependencies", obj("p", "")))
          .child(opts().pkg(obj("name", "x", "version", "1.2.3")).build())
          .build();
    }

    private static Node.Options b() {
        return opts().pkg(obj("name", "b", "version", "1.2.3", "peerDependencies", obj("p", ""))).build();
    }

    @Test
    void deepestNestingTargets() {
        Node tree = node(opts().path("/some/path")
          .pkg(obj("name", "root", "dependencies", obj("a", "", "b", "", "link", "file:target")))
          .fsChild(opts().path("/some/path/target")
            .pkg(obj("name", "link", "version", "1.2.3", "peerDependencies", obj("p", ""),
              "dependencies", obj("a", "", "b", "", "outerlink", "file:/other/path")))
            .child(a())
            .child(b())
            .build())
          .child(a())
          .child(b()));

        Node a = tree.getChildren().get("a");
        Node x = a.getChildren().get("x");
        Node b = tree.getChildren().get("b");

        Node inner = tree.getInventory().get("target");
        Link innerLink = link(opts().parent(tree).name("link").target(inner));
        assertThat(innerLink.getTarget()).as("gut check").isSameAs(inner);
        assertThat(tree.getChildren().get("link")).as("gut check").isSameAs(innerLink);
        Node innerA = inner.getChildren().get("a");
        Node innerX = innerA.getChildren().get("x");
        Node innerB = inner.getChildren().get("b");

        Node outer = node(opts().path("/other/path")
          .pkg(obj("name", "other", "version", "1.2.3", "peerDependencies", obj("p", ""),
            "dependencies", obj("a", "", "b", "")))
          .child(a())
          .child(b()));
        Node outerA = outer.getChildren().get("a");
        Node outerX = outerA.getChildren().get("x");
        Node outerB = outer.getChildren().get("b");

        Link outerLink = link(opts().parent(inner).name("outerlink").target(outer));
        assertThat(tree.getInventory().get("../../other/path")).as("gut check").isSameAs(outer);
        assertThat(outerLink.getTarget()).as("gut check").isSameAs(outer);
        assertThat(inner.getChildren().get("outerlink")).as("gut check").isSameAs(outerLink);

        assertThat(deepestNestingTarget(tree, "p")).isSameAs(tree);
        assertThat(deepestNestingTarget(a, "p")).isSameAs(tree);
        assertThat(deepestNestingTarget(b, "p")).isSameAs(tree);
        assertThat(deepestNestingTarget(x, "p")).isSameAs(x);
        assertThat(deepestNestingTarget(inner, "p")).isSameAs(tree);
        assertThat(deepestNestingTarget(innerA, "p")).isSameAs(tree);
        assertThat(deepestNestingTarget(innerB, "p")).isSameAs(tree);
        assertThat(deepestNestingTarget(innerX, "p")).isSameAs(innerX);
        assertThat(deepestNestingTarget(outer, "p")).isSameAs(outer);
        assertThat(deepestNestingTarget(outerA, "p")).isSameAs(outer);
        assertThat(deepestNestingTarget(outerB, "p")).isSameAs(outer);
        assertThat(deepestNestingTarget(outerX, "p")).isSameAs(outerX);
    }
}
