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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.assertTree;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.emptyLock;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.link;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.node;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.obj;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.opts;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.treeDump;

/**
 * Port of @npmcli/arborist test/link.js. The snapshots are compared with arborist's own dump of the
 * same trees and lock metadata.
 */
class LinkTest {

    @Test
    void instantiate() {
        Shrinkwrap meta = emptyLock("/srv/user/projects/some/kind/of/path");
        Node root = node(opts().pkg(obj("name", "root")).path("/srv/user/projects/some/kind/of/path").meta(meta));
        Link l1 = link(opts().pkg(obj("name", "root")).path("/srv/user/some/other/path")
          .realpath("/srv/user/projects/some/kind/of/path").meta(meta));
        assertTree("instantiate without providing target", l1);
        assertThat(String.valueOf(meta.getTree() == l1.getTarget()))
          .isEqualTo(treeDump("instantiate without providing target / tree"));
        assertThat(l1.isLink()).isTrue();
        assertThat(l1.getChildren().size()).isZero();
        l1.getChildren().set("asdf", node(opts().path("/asdf")));
        assertThat(l1.getChildren().size()).as("children still empty after setting value").isZero();

        Link l2 = link(opts().path("/srv/user/some/other/path").target(root));
        assertTree("instantiate with target specified", l2.getRoot());
    }

    @Test
    void mustProvideRealpath() {
        assertThatThrownBy(() -> link(opts().path("/x"))).hasMessage("must provide realpath for Link node");
    }

    @Test
    void resolvedIsRelpathToRealpath() {
        Link resolver = link(opts().path("/x/y/z").realpath("/x/z/y/a/b/c"));
        assertThat(resolver.getResolved()).isEqualTo("file:../z/y/a/b/c");
        resolver.setPath(null);
        assertThat(resolver.getResolved()).as("link resolved depends on path").isNull();
        resolver.setPath("/x/z/y/a/b/d");
        assertThat(resolver.getResolved()).as("updates when path changes").isEqualTo("file:c");
    }

    @Test
    void linkTargetSetter() {
        Link link = link(opts().path("/path/to/link").realpath("/node-a")
          .pkg(obj("name", "node-a", "version", "1.2.3")));
        Node oldTarget = link.getTarget();
        assertThat(oldTarget).isNotNull();
        assertThat(oldTarget.getLinksIn()).as("target takes note of link").contains(link);
        assertThat(link.getPackage()).as("link has same package as target").isSameAs(oldTarget.getPackage());

        Node newTarget = node(opts().path("/node-b").realpath("/node-b")
          .pkg(obj("name", "node-b", "version", "1.2.3")));
        link.setTarget(newTarget);
        assertThat(oldTarget.getLinksIn()).as("old target has no links in now").isEmpty();
        assertThat(link.getTarget()).isSameAs(newTarget);
        assertThat(newTarget.getLinksIn()).as("new target notes the link").contains(link);
        assertThat(link.getPackage()).as("link package is new target package").isSameAs(newTarget.getPackage());

        link.setTarget(null);
        assertThat(link.getTarget()).isNull();
        assertThat(link.getPackage()).as("no package without link").isEqualTo(new LinkedHashMap<>());
        assertThat(oldTarget.getLinksIn()).isEmpty();
        assertThat(newTarget.getLinksIn()).isEmpty();

        link.setTarget(null);
        assertThat(link.getTarget()).isNull();
        assertThat(link.getPackage()).isEqualTo(new LinkedHashMap<>());
    }

    @Nested
    class GetRootFromVariousPlaces {

        private final Node root = node(opts().path("/path/to/root"));

        @Test
        void getFromRoot() {
            Link fromRoot = link(opts().pkg(obj("name", "from-root")).path("/path/to/root/from-root")
              .realpath("/path/to/root/from-root-target").root(root));
            assertThat(fromRoot.getRoot()).isSameAs(root);
            assertThat(fromRoot.getFsParent()).isSameAs(root);
            assertThat(fromRoot.getParent()).isNull();
            assertThat(fromRoot.getTarget().getRoot()).isSameAs(root);
            assertThat(fromRoot.getTarget().getFsParent()).isSameAs(root);
            assertThat(fromRoot.getTarget().getParent()).isNull();
        }

        @Test
        void getFromFsParent() {
            Link fromFsParent = link(opts().pkg(obj("name", "from-fs-parent")).path("/path/to/root/from-fs-parent")
              .realpath("/path/to/root/from-root-fs-parent").fsParent(root));
            assertThat(fromFsParent.getRoot()).isSameAs(root);
            assertThat(fromFsParent.getFsParent()).isSameAs(root);
            assertThat(fromFsParent.getParent()).isNull();
            assertThat(fromFsParent.getTarget().getRoot()).isSameAs(root);
            assertThat(fromFsParent.getTarget().getFsParent()).isSameAs(root);
            assertThat(fromFsParent.getTarget().getParent()).isNull();
        }

        @Test
        void getFromParent() {
            Link fromParent = link(opts().pkg(obj("name", "from-parent")).parent(root)
              .realpath("/path/to/root/from-root-parent"));
            assertThat(fromParent.getRoot()).isSameAs(root);
            assertThat(fromParent.getFsParent()).isNull();
            assertThat(fromParent.getParent()).isSameAs(root);
            assertThat(fromParent.getTarget().getRoot()).isSameAs(root);
            assertThat(fromParent.getTarget().getFsParent()).isSameAs(root);
            assertThat(fromParent.getTarget().getParent()).isNull();
        }
    }

    @Test
    void temporaryLinkNodePendingAttachmentToATree() {
        Node root = node(opts().path("/path/to/node"));
        Link link = link(opts().name("foo").realpath("/this/will/change"));
        Node target = node(opts().path("/path/to/node/foo"));
        assertThat(link.getRoot()).isSameAs(link);
        assertThat(target.getRoot()).isSameAs(target);
        assertThat(link.getRealpath()).isEqualTo("/this/will/change");
        link.setTarget(target);
        assertThat(target.getRoot()).isSameAs(link);
        assertThat(link.getRealpath()).isEqualTo(target.getPath());
        assertThat(link.getPath()).isNull();
        link.setParent(root);
        assertThat(link.getRealpath()).isEqualTo(target.getPath());
        assertThat(link.getPath()).isEqualTo("/path/to/node/node_modules/foo");
        assertThat(link.getRoot()).isSameAs(root);
        assertThat(target.getRoot()).isSameAs(root);
        assertThat(target.getFsParent()).isSameAs(root);

        Link link2 = link(opts().name("bar").realpath("/this/will/change"));
        Node target2 = node(opts().name("bar"));
        link2.setTarget(target2);
        assertThat(target2.getPath()).isEqualTo("/this/will/change");
        link2.setTarget(null);
        target2.setPath("/path/to/node/bar");
        target2.setRealpath("/path/to/node/bar");
        link2.setTarget(target2);
        assertThat(link2.getRealpath()).isEqualTo(target2.getPath());
        link2.setParent(root);
        assertThat(target2.getFsParent()).isSameAs(root);
    }

    @Test
    void linkGetsVersionFromTarget() {
        Link link = link(opts().realpath("/some/real/path").path("/other/path"));
        assertThat(link.getVersion()).isEqualTo("");
        link.setTarget(null);
        link.setPackage(obj("name", "bar", "version", "2.3.4"));
        assertThat(link.getVersion()).isEqualTo("2.3.4");
        link.setPackage(obj());
        assertThat(link.getVersion()).isEqualTo("");
        node(opts().pkg(obj("name", "foo", "version", "1.2.3")).path("/some/real/path").root(link));
        assertThat(link.getVersion()).isEqualTo("1.2.3");
    }

    // link.js, from npm 7.1.1 (arborist 2.0.0): a link reads its version from its target; before, from the package it
    // took when the target was set, which a later change of the target's package leaves as it was
    @Test
    void linkKeepsVersionItTookFromTargetBefore711() {
        for (String version : new String[]{"7.1.0", "7.1.1"}) {
            NpmRelease release = NpmRelease.of(version);
            Node root = node(opts().release(release).path("/path/to/root").pkg(obj("name", "root")));
            Node target = node(opts().release(release).path("/path/to/root/packages/b").root(root)
              .pkg(obj("name", "b", "version", "1.0.0")));
            Link link = link(opts().release(release).path("/path/to/root/node_modules/b")
              .realpath("/path/to/root/packages/b").target(target).parent(root));
            target.setPackage(obj("name", "b", "version", "2.0.0"));
            assertThat(link.getVersion()).as("npm %s", version).isEqualTo("7.1.0".equals(version) ? "1.0.0" : "2.0.0");
        }
    }

    // link.js, from npm 7.1.1 (arborist 2.0.0): a link made without a parent takes its target's root and is placed by
    // it; before, it is its own root, and roots only a target that is its own root
    @Test
    void linkTakesRootOfTargetFrom711() {
        for (String version : new String[]{"7.1.0", "7.1.1"}) {
            NpmRelease release = NpmRelease.of(version);
            boolean roots = "7.1.1".equals(version);
            Node root = node(opts().release(release).path("/path/to/root").pkg(obj("name", "root")));
            Node target = node(opts().release(release).path("/path/to/root/packages/b").root(root)
              .pkg(obj("name", "b", "version", "1.0.0")));
            Link link = link(opts().release(release).path("/path/to/root/node_modules/b")
              .realpath("/path/to/root/packages/b").target(target));
            assertThat(link.getRoot()).as("npm %s", version).isSameAs(roots ? root : link);
            assertThat(link.getParent()).as("npm %s", version).isSameAs(roots ? root : null);
            assertThat(target.getRoot()).isSameAs(root);
            assertThat(target.getLinksIn()).containsExactly(link);

            if (!roots) {
                // arborist 1: the target a link makes lives in the link's tree, and follows the link into another
                Link loose = link(opts().release(release).path("/elsewhere/node_modules/c").realpath("/elsewhere/c"));
                Node made = Objects.requireNonNull(loose.getTarget());
                assertThat(loose.getRoot()).isSameAs(loose);
                assertThat(made.getRoot()).isSameAs(loose);
                assertThat(made.getLinksIn()).containsExactly(loose);
                loose.setParent(root);
                assertThat(loose.getRoot()).isSameAs(root);
                assertThat(made.getRoot()).isSameAs(root);
                assertThat(loose.getTarget()).isSameAs(made);
            }
        }
    }

    @Test
    void recalculateOutEdgesOverridesForwardsOverridesToTarget() {
        Node root = node(opts().path("/path/to/root").loadOverrides(true).pkg(obj("name", "root",
          "dependencies", obj("foo", "1.0.0"), "overrides", obj("bar", "2.0.0"))));
        Node target = node(opts().path("/path/to/store/foo").root(root)
          .pkg(obj("name", "foo", "version", "1.0.0", "dependencies", obj("bar", "1.0.0"))));
        Link link = link(opts().pkg(obj("name", "foo", "version", "1.0.0")).path("/path/to/root/node_modules/foo")
          .realpath("/path/to/store/foo").target(target).parent(root));

        assertThat(root.getOverrides()).as("root has overrides").isNotNull();
        assertThat(link.getOverrides()).as("link received overrides from root edge").isNotNull();
        assertThat(link.getTarget().getOverrides()).as("target received overrides forwarded from link").isNotNull();

        Edge barEdge = link.getTarget().getEdgesOut().get("bar");
        assertThat(barEdge).as("target has edge to bar").isNotNull();
        assertThat(barEdge.getOverrides()).as("bar edge has overrides").isNotNull();
        assertThat(barEdge.getSpec()).isEqualTo("2.0.0");
        assertThat(barEdge.getRawSpec()).isEqualTo("1.0.0");

        link.setTarget(null);
        assertThatCode(link::recalculateOutEdgesOverrides).as("no-op when target is null").doesNotThrowAnyException();
    }

    @Test
    void recalculateOutEdgesOverridesDoesNotForwardWhenNoRuleMatchesATargetDep() {
        Node root = node(opts().path("/path/to/root").loadOverrides(true).pkg(obj("name", "root",
          "dependencies", obj("foo", "1.0.0"), "overrides", obj("bar", "2.0.0"))));
        Node target = node(opts().path("/path/to/store/foo").root(root)
          .pkg(obj("name", "foo", "version", "1.0.0", "dependencies", obj("baz", "1.0.0"))));
        link(opts().pkg(obj("name", "foo", "version", "1.0.0")).path("/path/to/root/node_modules/foo")
          .realpath("/path/to/store/foo").target(target).parent(root));

        assertThat(root.getOverrides()).isNotNull();
        assertThat(target.getOverrides()).as("target.overrides stays undefined when no rule matches a target dep")
          .isNull();
        Edge bazEdge = target.getEdgesOut().get("baz");
        assertThat(bazEdge).isNotNull();
        assertThat(bazEdge.getOverrides()).as("unrelated edge keeps edge.overrides undefined").isNull();
        assertThat(bazEdge.getSpec()).isEqualTo("1.0.0");
    }

    // link.js overrideMatchesSubtree, from npm 11.18.0 (npm/cli#9619): a rule naming a dependency below the target
    @Test
    void recalculateOutEdgesOverridesForwardsWhenARuleAppliesBelowTheTargetFrom1118() {
        for (String version : new String[]{"11.17.0", "11.18.0"}) {
            NpmRelease release = NpmRelease.of(version);
            Node root = node(opts().release(release).path("/path/to/root").loadOverrides(true).pkg(obj("name", "root",
              "dependencies", obj("foo", "1.0.0"), "overrides", obj("bar", "2.0.0"))));
            Node target = node(opts().release(release).path("/path/to/store/foo").root(root)
              .pkg(obj("name", "foo", "version", "1.0.0", "dependencies", obj("baz", "1.0.0"))));
            Node baz = node(opts().release(release).parent(target)
              .pkg(obj("name", "baz", "version", "1.0.0", "dependencies", obj("bar", "1.0.0"))));
            link(opts().release(release).pkg(obj("name", "foo", "version", "1.0.0"))
              .path("/path/to/root/node_modules/foo").realpath("/path/to/store/foo").target(target).parent(root));

            Edge barEdge = baz.getEdgesOut().get("bar");
            if ("11.18.0".equals(version)) {
                assertThat(target.getOverrides()).as("npm %s forwards to the target", version).isNotNull();
                assertThat(barEdge.getSpec()).as("npm %s overrides the transitive edge", version).isEqualTo("2.0.0");
            } else {
                assertThat(target.getOverrides()).as("npm %s forwards nothing", version).isNull();
                assertThat(barEdge.getSpec()).as("npm %s leaves the transitive edge", version).isEqualTo("1.0.0");
            }
        }
    }

    // getEdgeRule matches on name and spec, so from npm 11.18.0 a rule for another version of a declared dependency
    // no longer gives the target the override set (npm/cli#9357)
    @Test
    void recalculateOutEdgesOverridesIgnoresARuleForAnotherVersionFrom1118() {
        for (String version : new String[]{"11.17.0", "11.18.0"}) {
            NpmRelease release = NpmRelease.of(version);
            Node root = node(opts().release(release).path("/path/to/root").loadOverrides(true).pkg(obj("name", "root",
              "dependencies", obj("foo", "1.0.0"), "overrides", obj("bar@2", "3.0.0"))));
            Node target = node(opts().release(release).path("/path/to/store/foo").root(root)
              .pkg(obj("name", "foo", "version", "1.0.0", "dependencies", obj("bar", "1.0.0"))));
            link(opts().release(release).pkg(obj("name", "foo", "version", "1.0.0"))
              .path("/path/to/root/node_modules/foo").realpath("/path/to/store/foo").target(target).parent(root));

            assertThat(target.getOverrides() != null).as("npm %s gives the target the override set", version)
              .isEqualTo("11.17.0".equals(version));
            assertThat(target.getEdgesOut().get("bar").getSpec()).isEqualTo("1.0.0");
        }
    }

    @Test
    void linkToRootPathGetsRootAsTarget() {
        Node root = node(opts().path("/project/root").pkg(obj("name", "root", "dependencies", obj("root", "file:."))));
        Link link = link(opts().parent(root).realpath(root.getPath()).pkg(new LinkedHashMap<>(root.getPackage())));
        assertThat(link.getTarget()).isSameAs(root);
    }
}
