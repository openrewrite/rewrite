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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.npm.Edge.Type.DEV;
import static org.openrewrite.javascript.internal.lock.npm.Edge.Type.OPTIONAL;
import static org.openrewrite.javascript.internal.lock.npm.Edge.Type.PEER;
import static org.openrewrite.javascript.internal.lock.npm.Edge.Type.PEER_OPTIONAL;
import static org.openrewrite.javascript.internal.lock.npm.Edge.Type.PROD;
import static org.openrewrite.javascript.internal.lock.npm.Edge.Type.WORKSPACE;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.arr;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.node;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.obj;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.opts;

/**
 * Port of @npmcli/arborist test/edge.js over real trees in which each edge resolves as upstream's hand-wired mocks do,
 * the snapshot's getters asserted field by field.
 */
class EdgeTest {

    // top (a (aa), b); c is outside the tree, as upstream's top.resolve('c') finds nothing
    private final Node top = node(opts().path("/top").pkg(obj("name", "top", "version", "1.2.3"))
      .child(opts().pkg(obj("name", "a", "version", "1.2.3"))
        .child(opts().pkg(obj("name", "aa", "version", "1.2.3")).build()).build())
      .child(opts().pkg(obj("name", "b", "version", "1.2.3")).build()));
    private final Node a = child(top, "a");
    private final Node aa = child(a, "aa");
    private final Node b = child(top, "b");
    private final Node c = node(opts().path("/elsewhere/node_modules/c").pkg(obj("name", "c", "version", "2.3.4")));

    private static Node child(Node parent, String name) {
        Node child = parent.getChildren().get(name);
        assertThat(child).as(name).isNotNull();
        return child;
    }

    private static void assertEdge(Edge edge, String name, String spec, Edge.Type type, @Nullable String error,
                                   @Nullable Node from, @Nullable Node to) {
        assertThat(edge.getName()).isEqualTo(name);
        assertThat(edge.getSpec()).isEqualTo(spec);
        assertThat(edge.getType()).isEqualTo(type);
        assertThat(edge.getError()).isEqualTo(error);
        assertThat(edge.isValid()).isEqualTo(error == null);
        assertThat(edge.getFrom()).isSameAs(from);
        assertThat(edge.getTo()).isSameAs(to);
        assertThat(edge.isPeer()).isEqualTo(type == PEER || type == PEER_OPTIONAL);
        assertThat(edge.isDev()).isEqualTo(type == DEV);
        assertThat(edge.isOptional()).isEqualTo(type == OPTIONAL || type == PEER_OPTIONAL);
        assertThat(edge.isWorkspace()).isEqualTo(type == WORKSPACE);
        assertThat(edge.isMissing()).isEqualTo(Edge.MISSING.equals(error));
        assertThat(edge.isPeerLocal()).isEqualTo(Edge.PEER_LOCAL.equals(error));
        assertThat(edge.isInvalid()).isEqualTo(Edge.INVALID.equals(error));
        assertThat(edge.isPeerConflicted()).isFalse();
    }

    @Test
    void peerDepAtTopLevelNestingAllowed() {
        assertEdge(new Edge(top, PEER, "a", "1.2.3"), "a", "1.2.3", PEER, null, top, a);
    }

    @Test
    void missingOptionalDepIsFine() {
        assertEdge(new Edge(top, OPTIONAL, "missing", ""), "missing", "", OPTIONAL, null, top, null);
    }

    @Test
    void peerDepBelowTopLevelNestingDisallowed() {
        assertEdge(new Edge(a, PEER, "aa", "1.2.3"), "aa", "1.2.3", PEER, Edge.PEER_LOCAL, a, aa);
    }

    @Test
    void peerDepBelowTopLevelParallelAllowed() {
        assertEdge(new Edge(a, PEER, "b", "1.2.3"), "b", "1.2.3", PEER, null, a, b);
    }

    @Test
    void satisfiedBy() {
        assertThat(new Edge(a, PROD, "b", "1.x").satisfiedBy(b)).as("b satisfies b@1.x").isTrue();
        assertThat(new Edge(a, PROD, "c", "1.x", "2.x", null).satisfiedBy(c)).as("c@2 satisfies spec:1.x, accept:2.x")
          .isTrue();
        assertThat(new Edge(a, PROD, "c", "1.x", "2.x", null).getAccept()).isEqualTo("2.x");
        assertThat(new Edge(a, PROD, "c", "1.x", "", null).satisfiedBy(c)).as("c@2 satisfies spec:1.x, accept:*")
          .isTrue();
        assertThat(new Edge(a, PROD, "c", "1.x").satisfiedBy(b)).as("b does not satisfy edge for c").isFalse();
    }

    @Test
    void printableEdgeShowsOverrides() {
        OverrideSet overrideSet = new OverrideSet(obj("c", "2.x"));
        a.setOverrides(overrideSet);
        Map<String, Object> json = new Edge(a, PROD, "c", "1.x", null, overrideSet.getEdgeRule("c", "1.x")).toJSON();
        assertThat(json).containsOnly(
          entry("name", "c"), entry("spec", "2.x"), entry("type", "prod"), entry("from", "node_modules/a"),
          entry("error", "MISSING"), entry("overridden", "2.x"));

        Map<String, Object> overriddenExplanation = new Edge(a, PROD, "c", "1.x", null,
          overrideSet.getEdgeRule("c", "1.x")).explain();
        assertThat(overriddenExplanation.get("rawSpec")).isEqualTo("1.x");
        assertThat(overriddenExplanation.get("spec")).isEqualTo("2.x");
        assertThat(overriddenExplanation.get("overridden")).isEqualTo(true);

        assertThat(new Edge(a, PROD, "c", "1.x", null, overrideSet.getEdgeRule("c", "1.x")).satisfiedBy(c))
          .as("c@2 satisfies spec:1.x, override:2.x").isTrue();
    }

    @Test
    void printableEdgeDoesNotShowNonApplicableOverride() {
        OverrideSet overrideSetB = new OverrideSet(obj("b", "1.x"));
        a.setOverrides(overrideSetB);
        assertThat(new Edge(a, PROD, "c", "2.x", null, overrideSetB.getEdgeRule("c", "2.x")).toJSON()).containsOnly(
          entry("name", "c"), entry("spec", "2.x"), entry("type", "prod"), entry("from", "node_modules/a"),
          entry("error", "MISSING"));
        assertThat(new Edge(a, PROD, "c", "2.x", null, overrideSetB.getEdgeRule("c", "2.x")).satisfiedBy(c))
          .as("c@2 satisfies spec:1.x, no matching override").isTrue();
    }

    @Test
    void overridesFollowTheFromNodeOnReload() {
        Edge overrideEdge = new Edge(a, PROD, "c", "2.x");
        assertThat(overrideEdge.getOverrides()).as("edge has no overrides").isNull();
        a.setOverrides(new OverrideSet(obj("b", "1.x")));
        assertThat(overrideEdge.getOverrides()).as("edge has no overrides").isNull();
        overrideEdge.reload();
        assertThat(overrideEdge.getOverrides()).as("edge has overrides after reload").isNotNull();
        a.setOverrides(null);
        overrideEdge.reload();
        assertThat(overrideEdge.getOverrides()).as("edge has no overrides after reload").isNull();
    }

    @SuppressWarnings("unchecked")
    @Test
    void referenceOverridesResolveSpecs() {
        Node referenceTop = node(opts().path("/reference-top").loadOverrides(true)
          .pkg(obj("name", "referenceTop", "version", "1.2.3",
            "dependencies", obj("referenceChild", "^1.0.0"),
            "overrides", obj("referenceGrandchild", "$referenceChild")))
          .child(opts().pkg(obj("name", "referenceChild", "version", "1.2.3",
              "dependencies", obj("referenceGrandchild", "^2.0.0")))
            .child(opts().pkg(obj("name", "referenceGrandchild", "version", "1.2.3")).build()).build()));
        Edge referenceGrandchildEdge = child(referenceTop, "referenceChild").getEdgesOut().get("referenceGrandchild");
        assertThat(referenceGrandchildEdge).isNotNull();

        assertThat(referenceGrandchildEdge.getSpec()).as("resolves spec to prod dep").isEqualTo("^1.0.0");
        assertThat(referenceGrandchildEdge.isValid()).as("edge is valid").isTrue();

        Map<String, Object> pkg = referenceTop.getPackage();
        ((Map<String, Object>) pkg.get("dependencies")).remove("referenceChild");
        assertThatThrownBy(referenceGrandchildEdge::getSpec).as("spec getter throws for missing dep")
          .hasMessage("Unable to resolve reference $referenceChild");

        pkg.put("devDependencies", obj("referenceChild", "^2.0.0"));
        assertThat(referenceGrandchildEdge.getSpec()).as("resolves spec for dev dep").isEqualTo("^2.0.0");

        pkg.remove("devDependencies");
        pkg.put("optionalDependencies", obj("referenceChild", "^3.0.0"));
        assertThat(referenceGrandchildEdge.getSpec()).as("resolves spec for optional dep").isEqualTo("^3.0.0");

        pkg.remove("optionalDependencies");
        pkg.put("peerDependencies", obj("referenceChild", "^4.0.0"));
        assertThat(referenceGrandchildEdge.getSpec()).as("resolves spec for peer dep").isEqualTo("^4.0.0");
    }

    @Test
    void edgeOverridesAreDroppedWhenTheFromNodeHasNone() {
        Node badOverride = node(opts().path("/bad/node_modules/c").pkg(obj("name", "c", "version", "2.3.4"))
          .overrides(new OverrideSet(obj("b", "1.x"))));
        assertThat(new Edge(a, PROD, "c", "2.x", null, new OverrideSet(obj("b", "1.x"))).satisfiedBy(badOverride))
          .as("arborist answers true; upstream's two-argument t.not holds for either answer")
          .isTrue();
    }

    @Test
    void overrideSpecTakesPriority() {
        OverrideSet overrides = new OverrideSet(obj("c", "1.x"));
        a.setOverrides(overrides);
        Edge overriddenEdge = new Edge(a, PROD, "c", "2.x", null, overrides.getEdgeRule("c", "2.x"));
        assertThat(overriddenEdge.getSpec()).isEqualTo("1.x");
        assertThat(overriddenEdge.getRawSpec()).isEqualTo("2.x");
    }

    @Test
    void clobberedAndClobberingEdges() {
        Edge old = new Edge(a, PEER, "b", "1.2.3");
        Edge neu = new Edge(a, PROD, "b", "*");
        assertEdge(old, "b", "1.2.3", PEER, Edge.DETACHED, null, null);
        assertEdge(neu, "b", "*", PROD, null, a, b);
    }

    @Test
    void invalidDependency() {
        Edge abInvalid = new Edge(a, OPTIONAL, "b", "1.69.420-nice");
        assertEdge(abInvalid, "b", "1.69.420-nice", OPTIONAL, Edge.INVALID, a, b);
        b.getPackage().put("version", "1.69.420-nice");
        abInvalid.reload(true);
        assertThat(abInvalid.isValid()).as("valid after updating target and hard reload").isTrue();
    }

    @Test
    void movingReloadsTheEdge() {
        // b holds its own aa, so the edge from under b resolves as upstream's mock of b does
        node(opts().pkg(obj("name", "aa", "version", "1.2.3")).parent(b));
        Node bb = node(opts().name("bb").pkg(obj("name", "bb", "version", "1.2.3")).parent(b));
        Node bAa = child(b, "aa");
        Edge moving = new Edge(bb, PROD, "aa", "");
        Map<String, Object> explBeforeMove = moving.explain();
        assertEdge(moving, "aa", "", PROD, null, bb, bAa);

        bb.setParent(a);
        moving.reload();
        Map<String, Object> explAfterMove = moving.explain();
        assertThat(explAfterMove).as("moving reloads the explanation").isNotSameAs(explBeforeMove);
        assertEdge(moving, "aa", "", PROD, null, bb, aa);

        bb.setParent(top);
        moving.reload();
        assertEdge(moving, "aa", "", PROD, Edge.MISSING, bb, null);

        new Edge(bb, DEV, "aa", "*");
        assertEdge(moving, "aa", "", PROD, Edge.DETACHED, null, null);
    }

    @Test
    void constructorRejectsBadArguments() {
        assertThatThrownBy(() -> new Edge(top, PROD, "yoinks", null)).hasMessage("must provide string spec");
        assertThatThrownBy(() -> node(opts().path("/accept").pkg(obj(
          "dependencies", obj("yoinks", "1.x"), "acceptDependencies", obj("yoinks", obj("yoinks", "2.2.3"))))))
          .hasMessage("accept field must be a string if provided");
        assertThatThrownBy(() -> new Edge(top, PROD, null, "*")).hasMessage("must provide dependency name");
        assertThatThrownBy(() -> new Edge(null, PROD, "foo", "")).hasMessage("must provide \"from\" node");
        assertThatThrownBy(() -> new Edge(null, WORKSPACE, "foo", "*")).hasMessage("workspace edges must be a symlink");
    }

    @Nested
    class ConvenienceTypeGetterFlags {

        @Test
        void flags() {
            assertThat(new Edge(a, WORKSPACE, "foo", "file:bar/baz").isWorkspace()).isTrue();
            assertThat(new Edge(a, PROD, "foo", "*").isProd()).isTrue();
            assertThat(new Edge(a, DEV, "foo", "*").isDev()).isTrue();
            assertThat(new Edge(a, OPTIONAL, "foo", "*").isOptional()).isTrue();
            assertThat(new Edge(a, PEER, "foo", "*").isPeer()).isTrue();
            assertThat(new Edge(a, PEER_OPTIONAL, "foo", "*").isPeer()).isTrue();
            assertThat(new Edge(a, PEER_OPTIONAL, "foo", "*").isOptional()).isTrue();
        }

        @Test
        void explanation() {
            Edge explainEdge = new Edge(a, PEER_OPTIONAL, "foo", "*");
            Map<String, Object> expl = explainEdge.explain(Collections.emptyList());
            assertThat(explainEdge.explain()).isSameAs(expl);
            assertThat(expl).containsOnly(entry("type", "peerOptional"), entry("name", "foo"), entry("spec", "*"),
              entry("from", a.explain()));
            explainEdge.detach();
            assertThat(explainEdge.explain()).isNotSameAs(expl)
              .containsOnly(entry("type", "peerOptional"), entry("name", "foo"), entry("spec", "*"),
              entry("error", "DETACHED"));
        }
    }

    @Test
    void printableRepresentation() {
        Edge printable = new Edge(top, PROD, "b", "1.2.3");
        assertThat(printable.toJSON()).containsOnly(entry("name", "b"), entry("spec", "1.2.3"), entry("type", "prod"),
          entry("from", ""), entry("to", "node_modules/b"));
        printable.setPeerConflicted(true);
        assertThat(printable.toJSON()).containsOnly(entry("name", "b"), entry("spec", "1.2.3"), entry("type", "prod"),
          entry("from", ""), entry("to", "node_modules/b"), entry("peerConflicted", true));

        Node nothingResolves = node(opts().path("/alone").pkg(obj("name", "alone", "version", "1.2.3")));
        assertThat(new Edge(nothingResolves, DEV, "b", "1.2.3").toJSON()).containsOnly(entry("name", "b"),
          entry("spec", "1.2.3"), entry("type", "dev"), entry("from", ""), entry("error", "MISSING"));
    }

    @Test
    void bundledDependency() {
        Node bundleParent = node(opts().pkg(obj("name", "bundle-parent", "version", "5.6.7",
          "bundleDependencies", arr("bundle-child"))).parent(top));
        Node bundleChild = node(opts().pkg(obj("name", "bundle-child", "version", "1.2.3")).parent(top));
        Edge bundledEdge = new Edge(bundleParent, PROD, "bundle-child", "1.2.3");

        assertThat(bundledEdge.satisfiedBy(bundleChild)).as("bundled dependency").isTrue();
        assertThat(bundledEdge.getFrom().getPackage().get("bundleDependencies")).isEqualTo(arr("bundle-child"));
        assertThat(bundledEdge.getName()).isEqualTo("bundle-child");
        assertThat(bundledEdge.isBundled()).isTrue();
        assertThat(bundledEdge.explain()).containsOnly(entry("type", "prod"), entry("name", "bundle-child"),
          entry("spec", "1.2.3"), entry("bundled", true), entry("from", bundleParent.explain()));
    }

    @Test
    void overrideReferencesFindTheCorrectRoot() {
        OverrideSet overrides = new OverrideSet(obj("foo", "$foo"));
        Node root = node(opts().path("/root").overrides(overrides)
          .pkg(obj("name", "root", "version", "1.2.3", "dependencies", obj("foo", "^1.0.0"), "overrides",
          obj("foo", "$foo")))
          .child(opts().pkg(obj("name", "foo", "version", "1.2.3", "dependencies", obj()))
            .child(opts().pkg(obj("name", "bar", "version", "1.2.3", "dependencies", obj("foo", "^2.0.0"))).build())
            .build()));
        Node bar = child(child(root, "foo"), "bar");

        // a virtual root standing in for bar, holding its own copy of foo, as the ideal tree builder makes one
        Node virtualBar = node(opts().path("/virtual-root").sourceReference(bar).overrides(overrides)
          .child(opts().pkg(obj("name", "foo", "version", "1.2.3", "dependencies", obj())).build()));

        Edge edge = new Edge(virtualBar, PROD, "foo", "^2.0.0", null, overrides.getEdgeRule("foo", "^2.0.0"));
        assertThat(edge.getSpec()).isEqualTo("^1.0.0");
        assertThat(edge.isValid()).as("edge is valid").isTrue();
    }

    @Test
    void shrinkwrappedAndBundledDepsAreNotOverriddenAndRemainValid() {
        Node root = node(opts().path("/root").loadOverrides(true)
          .pkg(obj("name", "root", "version", "1.2.3", "dependencies", obj("foo", "^1.0.0"), "overrides",
          obj("bar", "^2.0.0")))
          .child(opts().hasShrinkwrap(true)
          .pkg(obj("name", "foo", "version", "1.2.3", "dependencies", obj("bar", "^1.0.0")))
            .child(opts().pkg(obj("name", "bar", "version", "1.2.3", "dependencies", obj())).build()).build()));
        Node foo = child(root, "foo");
        Edge edge = foo.getEdgesOut().get("bar");
        assertThat(edge).isNotNull();
        assertThat(edge.getSpec()).isEqualTo("^2.0.0");
        assertThat(child(foo, "bar").isInShrinkwrap()).isTrue();
        assertThat(edge.isValid()).as("edge is valid").isTrue();
    }

    @Test
    void overrideFallbackToLocalWhenRootMissingDependency() {
        Node root = node(opts().path("/root").pkg(obj()));
        Node localFrom = node(opts().pkg(obj("name", "local", "devDependencies", obj("foo", "^1.2.3"))).parent(root)
          .overrides(new OverrideSet(obj("foo", "$foo"))));
        Edge edge = new Edge(localFrom, PROD, "foo", "1.x");
        assertThat(edge.getSpec()).as("should fall back to local package version from devDependencies")
          .isEqualTo("^1.2.3");
    }

    @Test
    void edgeWithOverridesShouldNotCrashWhenTargetHasNoOverrides() {
        Node root = node(opts().path("/root").loadOverrides(true)
          .pkg(obj("name", "root", "version", "1.0.0", "overrides", obj("react", "^18.3.1", "react-dom", "^18.3.1")))
          .child(opts().pkg(obj("name", "dep", "version", "1.0.0", "dependencies", obj("some-lib", "*"))).build()));
        Node dep = child(root, "dep");
        Edge edge = new Edge(root, PROD, "dep", "^1.0.0", null, root.getOverrides());
        dep.setOverrides(null);
        edge.reload(true);
        assertThat(dep.getEdgesOut().size()).isPositive();
        assertThat(edge.getError()).as("edge should be valid (no error)").isNull();
        assertThat(edge.isValid()).isTrue();
    }

    // not an upstream case: arborist 11.17.0 builds this node, and only reading the edge's spec throws
    @Test
    void emptySpecUnderAnUnresolvableReferenceOverride() {
        Node root = node(opts().path("/r").loadOverrides(true).pkg(obj("name", "root", "version", "2.0.0",
          "dependencies", obj("d", "1", "a", "*"), "overrides", obj("a", "$zz"))));
        Node d = node(opts().parent(root).name("d")
          .pkg(obj("name", "d", "version", "1.0.0", "dependencies", obj("a", ""))));
        assertThat(d.getLocation()).isEqualTo("node_modules/d");
        Edge a = d.getEdgesOut().get("a");
        assertThat(a.getOverrides().getValue()).isEqualTo("$zz");
        assertThatThrownBy(a::getSpec).hasMessage("Unable to resolve reference $zz");
    }

    private static Map.Entry<String, Object> entry(String key, Object value) {
        return new SimpleEntry<>(key, value);
    }
}
