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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.arr;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.assertGet;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.assertTree;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.describe;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.emptyLock;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.link;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.node;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.obj;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.opts;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.treeCheck;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.treeDump;

/**
 * Port of @npmcli/arborist test/node.js. Upstream snapshots that print whole trees are compared with
 * arborist's own dump of the same sequence (the tree-dumps fixture), as is the lock metadata a tree carries.
 */
class NodeTest {

    private static Node child(Node parent, String name) {
        Node child = parent.getChildren().get(name);
        assertThat(child).as(name).isNotNull();
        return child;
    }

    private static Map<String, String> workspaces(String... namesAndPaths) {
        Map<String, String> ws = new LinkedHashMap<>();
        for (int i = 0; i < namesAndPaths.length; i += 2) {
            ws.put(namesAndPaths[i], namesAndPaths[i + 1]);
        }
        return ws;
    }

    @Nested
    class BasicInstantiation {

        private final OverrideSet overrides = new OverrideSet(obj("foo", "1"));
        private final Node root = node(opts().pkg(obj("name", "root")).path("/srv/user/projects/root")
          .realpath("/srv/user/projects/root").overrides(overrides));

        @Test
        void loneRoot() {
            assertThat(root.getDepth()).isZero();
            assertThat(root.isTop()).isTrue();
            assertThat(root.isLink()).isFalse();
            assertThat(root.getOverrides()).isSameAs(overrides);
            assertThat(describe(root)).isEqualTo(treeDump("just a lone root node"));
        }

        @Test
        void depFlagsAllSetTrue() {
            assertThat(root.isDummy()).isFalse();
            assertThat(root.isExtraneous()).isTrue();
            assertThat(root.isDev()).isTrue();
            assertThat(root.isDevOptional()).isTrue();
            assertThat(root.isOptional()).isTrue();
            assertThat(root.isPeer()).isTrue();
        }

        @Test
        void dummyNodeDepFlagsAllSetFalse() {
            Node node = node(opts().path("/not/a/real/path").dummy(true));
            assertThat(node.isDummy()).isTrue();
            assertThat(node.isExtraneous()).isFalse();
            assertThat(node.isDev()).isFalse();
            assertThat(node.isDevOptional()).isFalse();
            assertThat(node.isOptional()).isFalse();
            assertThat(node.isPeer()).isFalse();
        }
    }

    @ParameterizedTest(name = "meta: {0}")
    @ValueSource(booleans = {false, true})
    void testingWithDepTree(boolean withMeta) {
        String at = withMeta ? "with meta: " : "";
        Shrinkwrap rootMetadata = withMeta ? emptyLock("/srv/user/projects/root") : null;
        Node root = node(opts()
          .pkg(obj("name", "root", "bundleDependencies", arr("bundled"),
            "dependencies", obj("prod", "1.x", "bundled", "", "missing", ""),
            "devDependencies", obj("dev", "", "overlap", ""),
            "optionalDependencies", obj("optional", "", "overlap", "", "optMissing", "")))
          .realpath("/srv/user/projects/root").path("/srv/user/projects/root").meta(rootMetadata)
          .child(opts()
            .pkg(obj("name", "prod", "version", "1.2.3", "dependencies", obj("meta", ""), "peerDependencies",
            obj("peer", "")))
            .fsChild(opts().realpath("/srv/user/projects/root/node_modules/prod/foo")
              .path("/srv/user/projects/root/node_modules/prod/foo").name("foo")
              .pkg(obj("name", "foo", "version", "1.2.3", "dependencies", obj("meta", ""))).build())
            .resolved("prod").integrity("prod").build()));
        assertThat(root.getRoot()).isSameAs(root);
        Node prod = child(root, "prod");
        assertThat(prod.getFsChildren()).hasSize(1);
        Node foo = prod.getFsChildren().iterator().next();
        assertThat(foo.getFsParent()).isSameAs(prod);
        assertThat(prod.getRoot()).isSameAs(root);
        assertThat(prod.getDepth()).isEqualTo(1);

        Node meta = node(opts()
          .pkg(obj("name", "meta", "version", "1.2.3", "devDependencies", obj("missing", ""),
            "dependencies", obj("bundled", ""), "_resolved", "meta", "_integrity", "meta"))
          .path("/srv/user/projects/root/node_modules/prod/node_modules/meta")
          .realpath("/srv/user/projects/root/node_modules/prod/node_modules/meta")
          .parent(prod));
        assertThat(meta.isDescendantOf(root)).isTrue();
        assertThat(meta.getRoot()).isSameAs(root);

        Node bundled = node(opts().pkg(obj("name", "bundled", "version", "1.2.3", "dependencies", obj("meta", "")))
          .resolved("bundled").integrity("bundled")
          .path("/srv/user/projects/root/node_modules/bundled").realpath("/srv/user/projects/root/node_modules/bundled")
          .parent(root));
        assertThat(meta.isDescendantOf(bundled)).isFalse();
        assertThat(bundled.getRoot()).isSameAs(root);

        Node dev = top(root, "dev", "dev");
        assertThat(dev.getRoot()).isSameAs(root);
        Node opt = top(root, "optional", "opt");
        assertThat(opt.getRoot()).isSameAs(root);
        Node peer = top(root, "peer", "peer");
        assertThat(peer.getRoot()).isSameAs(root);
        Node extraneous = top(root, "extraneous", "extraneous");
        assertThat(extraneous.getRoot()).isSameAs(root);

        assertThat(prod.getTop()).isSameAs(root);
        assertThat(prod.getRoot()).isSameAs(root);
        assertThat(root.isRoot()).isTrue();
        assertThat(prod.isRoot()).isFalse();
        assertThat(extraneous.isExtraneous()).isTrue();
        assertTree(at + "initial load with some deps", root);

        meta.setParent(root);
        assertTree(at + "move meta to top level, update stuff", root);
        assertThat(meta.getRoot()).isSameAs(root);
        assertThat(meta.getParent()).isSameAs(root);
        assertThat(root.getInventory().get(meta.getLocation())).isSameAs(meta);

        Node newMeta = node(opts()
          .pkg(obj("name", "meta", "version", "2.3.4", "peerDependencies", obj("asdf", ""),
            "peerDependenciesMeta", obj("asdf", obj("optional", true))))
          .resolved("newMeta").integrity("newMeta").name("meta").parent(prod));
        assertThat(newMeta.getRoot()).isSameAs(root);
        newMeta.setRoot(prod);
        assertThat(newMeta.getRoot()).as("setting root to non-root crawls up root list").isSameAs(root);
        assertThat(meta.getParent()).isSameAs(root);
        assertThat(newMeta.getParent()).isSameAs(prod);
        assertThat(root.getInventory().get(meta.getLocation())).isSameAs(meta);

        Link metaMeta = link(opts()
          .pkg(obj("name", "metameta", "version", "1.2.3", "_resolved", "metameta", "_integrity", "metameta"))
          .path(newMeta.getPath() + "/node_modules/metameta").realpath(meta.getRealpath()).target(meta));
        assertThat(metaMeta.getRoot()).as("link takes root of target if unspecified").isSameAs(root);
        metaMeta.setParent(newMeta);
        assertThat(metaMeta.getRoot()).isSameAs(root);
        assertThat(meta.getRoot()).isSameAs(root);
        assertThat(meta.getParent()).isSameAs(root);
        assertThat(root.getChildren().get("meta")).isSameAs(meta);
        assertThat(root.getInventory().get(meta.getLocation())).isSameAs(meta);
        assertTree(at + "add new meta under prod", root);

        assertThat(meta.getParent()).as("old meta parent is root before assigning").isSameAs(root);
        newMeta.setParent(root);
        assertThat(meta.getParent()).as("old meta parent removed").isNull();
        assertThat(root.getChildren().get("meta")).isNotSameAs(meta);
        assertTree(at + "move new meta to top level", root);

        newMeta.setParent(root);
        assertTree(at + "move new meta to top level second time (no-op)", root);
        treeCheck(root);

        Node newProd = node(opts().name("prod")
          .pkg(obj("name", "prod", "version", "1.2.3", "dependencies", obj("meta", ""), "peerDependencies",
          obj("peer", "")))
          .path("/some/path").resolved("prod").integrity("prod"));
        assertThat(newProd.canReplace(prod)).as("new prod can replace prod").isTrue();
        int kidCount = prod.getChildren().size();
        newProd.replace(prod);
        assertThat(newProd.getChildren().size()).as("kids moved to newProd").isEqualTo(kidCount);
        assertThat(prod.getChildren().size()).as("kids moved to newProd").isZero();
        assertThat(prod.getRoot()).as("prod excised from tree").isSameAs(prod);
        assertThat(newProd.getRoot()).as("newProd in the tree").isSameAs(root);
        assertThat(newProd.getFsChildren()).as("fsChildren replaced").isEmpty();
        assertThat(prod.getFsChildren()).as("fsChildren go along with fsParent").hasSize(1);
        assertThat(prod.getFsChildren().iterator().next()).as("foo still in old prod fsChildren set").isSameAs(foo);
        assertThat(foo.getFsParent()).as("prod is still foos fsParent").isSameAs(prod);

        Node notProd = node(opts().pkg(obj("name", "notprod", "version", "1.2.3")).path("/some/path"));
        assertThat(notProd.canReplace(newProd)).as("cannot replace with different name").isFalse();

        Node prodV2 = node(opts().name("prod").path("/path/to/prod").pkg(obj("name", "prod", "version", "2.3.4")));
        assertThat(newProd.canReplaceWith(prodV2)).as("cannot replace with 2.x").isFalse();

        Node root2 = node(opts()
          .pkg(obj("name", "root", "bundleDependencies", arr("bundled"),
            "dependencies", obj("prod", "1.x", "bundled", "", "missing", ""),
            "devDependencies", obj("dev", "", "overlap", ""),
            "optionalDependencies", obj("optional", "", "overlap", "", "optMissing", "")))
          .realpath("/srv/user/projects/root").path("/srv/user/projects/root").meta(rootMetadata));
        root.replaceWith(root2);
        assertThat(root2.getRoot()).as("replacing root preserves self-rootedness").isSameAs(root2);
        root.replace(root2);
        if (withMeta) {
            assertTree(at + "roots replaced", root);
        }

        Link prodLink = link(opts().pkg(prod.getPackage()).realpath("/some/other/path/entirely")
          .path("/not/where/it/really/is").name("prod"));
        assertThat(prodLink.canReplace(newProd)).as("link can replace node").isTrue();
        prodLink.replace(newProd);
        if (withMeta) {
            assertTree(at + "link replaced node", root);
        }
        assertThat(newProd.getParent()).as("newProd removed from tree").isNull();
        assertThat(newProd.getRoot()).as("newProd removed from tree").isSameAs(newProd);
        assertThat(prodLink.getPath()).as("replaced link").isEqualTo(newProd.getPath());
        assertThat(newProd.getChildren().size()).as("newProd kids moved over").isZero();
        assertThat(prodLink.getChildren().size()).as("links do not have child nodes").isZero();
        assertThat(prodLink.getTarget().getChildren().size()).as("link target has children").isEqualTo(kidCount);

        assertThat(newProd.canReplace(prodLink)).as("node can replace link").isTrue();
        Node target = prodLink.getTarget();
        newProd.replace(prodLink);
        if (rootMetadata != null) {
            assertTree(at + "node replaced link", root);
            for (String location : root.getInventory().keys()) {
                assertGet(at + location, rootMetadata, location);
            }
            assertThat(rootMetadata.getData()).isEqualTo(NpmTreeSupport.<Object>json(treeDump(at + "final / data")));
        }
        assertThat(prodLink.getParent()).as("prodLink removed from tree").isNull();
        assertThat(prodLink.getTarget()).as("prodLink lost its target").isNull();
        assertThat(prodLink.getRoot()).as("prodLink removed from tree").isSameAs(prodLink);
        assertThat(target.getRoot()).as("prodLinks old target removed from tree").isSameAs(target);
        assertThat(newProd.getPath()).as("replaced node").isEqualTo(prodLink.getPath());
        assertThat(prodLink.getChildren().size()).isZero();
    }

    private static Node top(Node root, String name, String resolved) {
        return node(opts().pkg(obj("name", name, "version", "1.2.3")).resolved(resolved).integrity(resolved)
          .path("/srv/user/projects/root/node_modules/" + name).realpath("/srv/user/projects/root/node_modules/" + name)
          .parent(root));
    }

    @Test
    void addingTwoChildrenThatBothHaveLinksIntoThem() {
        Node root = node(opts().path("/path/to/root"));
        Link o1 = link(opts().parent(root).realpath("/path/m/node_modules/n/o1").name("o1"));
        Node t1 = node(opts().path(o1.getRealpath()).root(root));
        Link o2 = link(opts().parent(root).realpath("/path/m/node_modules/n/o2").name("o2"));
        Node t2 = node(opts().path(o2.getRealpath()).root(root));
        Node m = node(opts().path("/path/m").dummy(true).root(root));
        assertThat(o1.getTarget()).isSameAs(t1);
        assertThat(o2.getTarget()).isSameAs(t2);
        assertThat(t1.getLinksIn()).contains(o1);
        assertThat(t2.getLinksIn()).contains(o2);
        assertThat(t1.getFsParent()).isSameAs(m);
        assertThat(t2.getFsParent()).isSameAs(m);
    }

    @Test
    void edgeCasesForBranchCoverage() {
        Node noPkg = node(opts().realpath("/srv/user/projects/root").path("/srv/user/projects/root"));
        assertThat(noPkg.getPackage()).as("default package is empty object").isEmpty();
        assertThat(noPkg.getName()).isEqualTo("root");

        Node noPath = node(opts().realpath("/srv/user/projects/root"));
        assertThat(noPath.getName()).as("pathless gets named for realpath").isEqualTo("root");
    }

    @Test
    void tracksTheLoadingErrorEncountered() {
        IllegalStateException error = new IllegalStateException("this is fine");
        Node root = node(opts().pkg(obj("name", "root")).path("/srv/user/projects/root")
          .realpath("/srv/user/projects/root").error(error));
        assertThat(root.getErrors().get(0)).isSameAs(error);
    }

    @Test
    void couldNotDetectNodeName() {
        assertThatThrownBy(() -> node(opts().pkg(obj()))).hasMessage("could not detect node name from path or package");
    }

    @Test
    void loadFromSystemRootPath() {
        assertThat(node(opts().path("/")).getName()).as("ok to have a nameless node in system root").isNull();
    }

    // node.js from npm 7.1.0 lets a node at the filesystem root go without a name; before, any nameless node throws
    @Test
    void namelessNodeInSystemRootThrowsBefore7_1_0() {
        assertThatThrownBy(() -> node(opts().path("/").release(NpmRelease.of("7.0.15"))))
          .hasMessage("could not detect node name from path or package");
        assertThat(node(opts().path("/").release(NpmRelease.of("7.1.0"))).getName()).isNull();
    }

    // load with a virtual filesystem parent; upstream also asserts the throws of arborist's debug mode
    @Test
    void loadWithAVirtualFilesystemParent() {
        Node root = node(opts()
          .pkg(obj("name", "root", "dependencies", obj("a", "", "link", "", "link2", "", "link3", "")))
          .path("/srv/user/projects/root").realpath("/srv/user/projects/root"));
        Node a = node(opts().pkg(obj("name", "a", "version", "1.2.3")).parent(root).name("a"));
        Link link = link(opts().pkg(obj("name", "link", "version", "1.2.3", "dependencies", obj("a", "", "kid", "")))
          .realpath(root.getRealpath() + "/link-target").parent(root));
        assertThat(link.getTarget()).as("link has a target").isNotNull();
        Node linkKid = node(opts().pkg(obj("name", "kid", "dependencies", obj("a", ""))).parent(link.getTarget()));

        Link link2 = link(opts().pkg(obj("name", "link2", "version", "1.2.3", "dependencies", obj("link", "")))
          .realpath(a.getRealpath() + "/node_modules/link2-target").parent(root).fsParent(a));
        assertThat(link2.getTarget().getParent()).as("fsParent=parent sets parent").isSameAs(a);
        assertThat(link2.getTarget().getFsParent()).as("fsParent=parent does not set fsParent").isNull();
        assertThat(link2.getTarget().getResolveParent()).as("resolveParent is parent").isSameAs(a);

        Node target3 = node(opts().name("link3").path(root.getRealpath() + "/packages/link3")
          .realpath(root.getRealpath() + "/packages/link3")
          .pkg(obj("name", "link3", "version", "1.2.3", "dependencies", obj("link2", "")))
          .fsParent(node(opts().path(root.getRealpath() + "/packages").realpath(root.getRealpath() + "/packages")
            .pkg(obj("name", "packages", "version", "2.3.4", "dependencies", obj("link", ""))))));
        Node packages = target3.getFsParent();

        Link link3 = link(opts().pkg(obj("name", "link3", "version", "1.2.3", "dependencies", obj("link2", "")))
          .realpath(root.getRealpath() + "/packages/link3").target(target3).parent(root));
        assertThat(target3.getRoot()).isSameAs(root);
        assertThat(packages.getFsChildren()).isEmpty();

        packages.setRoot(root);
        assertThat(target3.getFsParent().getPath()).isEqualTo(packages.getPath());
        assertThat(packages.getFsChildren()).hasSize(1);
        assertThat(packages.getLocation()).isEqualTo("packages");
        assertThat(target3.getLocation()).isEqualTo("packages/link3");
        assertThat(target3.getRealpath()).isEqualTo(root.getPath() + "/packages/link3");
        assertThat(target3.getFsParent()).isSameAs(packages);
        assertThat(link3.getRealpath()).isEqualTo(target3.getRealpath());
        assertThat(link3.getRoot()).isSameAs(target3.getRoot());
        assertThat(link3.getTarget()).as("before fsParent move").isSameAs(target3);

        for (int i = 0; i < 2; i++) {
            packages.setFsParent(link.getTarget());
            assertThat(packages.getLocation()).isEqualTo("link-target/packages");
            assertThat(target3.getLocation()).isEqualTo("link-target/packages/link3");
            assertThat(target3.getRealpath()).isEqualTo(root.getPath() + "/link-target/packages/link3");
            assertThat(target3.getFsParent()).isSameAs(packages);
            assertThat(packages.getFsChildren()).hasSize(1);
            assertThat(link3.getRealpath()).isEqualTo(target3.getRealpath());
            assertThat(link3.getRoot()).isSameAs(target3.getRoot());
            assertThat(link3.getTarget()).as("after fsParent move").isSameAs(target3);
        }
        assertThat(packages.getPath()).isEqualTo(root.getRealpath() + "/link-target/packages");
        assertThat(target3.getPath()).isEqualTo(root.getRealpath() + "/link-target/packages/link3");
        assertThat(link3.getTarget()).isSameAs(target3);
        assertThat(target3.getFsParent()).isSameAs(packages);
        assertThat(link3.getRealpath()).as("link realpath updated").isEqualTo(target3.getPath());

        Node linkChild = node(opts().path(target3.getPath() + "/linkchild")
          .pkg(obj("name", "linkchild", "version", "1.2.3")));
        linkChild.setFsParent(link3);
        assertThat(linkChild.getFsParent()).isSameAs(link3.getTarget());
        assertThat(linkChild.getRoot()).isSameAs(root);
        assertThat(target3.getFsParent()).isSameAs(packages);

        assertThat(link.getTarget().getFsParent()).isSameAs(root);
        assertThat(link.getTarget().getResolveParent()).as("resolveParent is fsParent").isSameAs(root);
        assertThat(link.getTarget().getEdgesOut().get("a").getError()).isNull();
        assertThat(linkKid.getEdgesOut().get("a").getError()).isNull();
        assertThat(linkKid.getParent()).isSameAs(link.getTarget());

        Node target = link.getTarget();
        link.getTarget().setRoot(null);
        assertThat(link.getTarget()).isNull();
        assertThat(target.getEdgesOut().get("a").getError()).isEqualTo("MISSING");
        assertThat(linkKid.getEdgesOut().get("a").getError()).isEqualTo("MISSING");

        target.setFsParent(link.getRoot());
        assertThat(link.getTarget()).isSameAs(target);
        assertThat(link.getTarget().getFsParent()).isSameAs(root);
        assertThat(link.getTarget().getEdgesOut().get("a").getError()).isNull();
        assertThat(linkKid.getEdgesOut().get("a").getError()).isNull();

        link.getTarget().setFsParent(link2.getTarget());
        assertThat(link.getTarget().getFsParent()).isSameAs(link2.getTarget());
        assertThat(link.getTarget().getEdgesOut().get("a").getError()).isNull();
        assertThat(linkKid.getEdgesOut().get("a").getError()).isNull();

        link.getTarget().setParent(link2.getTarget());
        assertThat(link.getTarget().getFsParent()).as("lost fsParent for parent").isNull();
        assertThat(link.getTarget().getEdgesOut().get("a").getError()).isNull();
        assertThat(linkKid.getEdgesOut().get("a").getError()).isNull();
        assertThat(link.getRealpath()).isEqualTo(link2.getRealpath() + "/node_modules/link-target");

        String linkLoc = link.getLocation();
        Node linkTarget = link.getTarget();
        link.setParent(null);
        assertThat(link.getRoot()).as("removed from parent, removed from root").isSameAs(link);
        assertThat(root.getInventory().get(linkLoc)).as("removed from root inventory").isNull();
        assertThat(link.getInventory().has(link)).as("link added to own inventory").isTrue();
        assertThat(link.getTarget()).as("target left behind when setting root to null").isNull();
        linkTarget.setRoot(link);
        assertThat(link.getTarget()).as("target set once roots match").isSameAs(linkTarget);
        assertThat(link.getInventory().get("")).isSameAs(linkTarget);
        assertThat(root.getEdgesOut().get("link").getError()).isEqualTo("MISSING");

        packages.setFsParent(null);
        assertThat(packages.getRoot()).as("removed from fsParent, removed from root").isSameAs(packages);

        Node aChild = node(opts().parent(a).pkg(obj("name", "achild", "version", "1.2.3")));
        assertThat(aChild.getParent()).isSameAs(a);
        assertThat(a.getChildren().get("achild")).isSameAs(aChild);
        List<Node> underA = root.getInventory().values().stream()
          .filter(n -> n.getPath() != null && n.getPath().startsWith(a.getPath()) && n != a)
          .collect(Collectors.toList());
        String aLoc = a.getLocation();
        Link aLink = link(opts().path(a.getPath()).target(node(opts().path("/some/other/a").pkg(a.getPackage()))));
        aLink.setRoot(root);
        assertThat(root.getInventory().get(aLoc)).isSameAs(aLink);
        assertThat(a.getRoot()).isSameAs(a);
        for (Node n : underA) {
            assertThat(n.getRoot()).as(n.getPath() + " still under old root").isNotSameAs(root);
        }

        Node fsD = node(opts().path(root.getPath() + "/a/b/c/d").pkg(obj("name", "d", "version", "1.2.3")).root(root));
        assertThat(fsD.getFsParent()).as("root should be fsParent").isSameAs(root);
        link(opts().path(root.getPath() + "/a/b")
          .target(node(opts().path("/some/exotic/location").pkg(obj("name", "b", "version", "1.2.3")))).root(root));
        assertThat(fsD.getRoot()).as("fsD removed from root").isNotSameAs(root);

        Link remoteLink = link(opts().parent(root).name("x").realpath("/remote/node_modules/a/node_modules/x"));
        Node remoteTarget = node(opts().path("/remote/node_modules/a/node_modules/x")
          .realpath("/remote/node_modules/a/node_modules/x").pkg(obj("name", "x", "version", "1.2.3")).root(root));
        assertThat(remoteLink.getTarget()).as("automatically found target").isSameAs(remoteTarget);
        assertThat(remoteTarget.getFsParent()).as("remote target has no fsParent").isNull();
        assertThat(remoteTarget.getParent()).as("remote target has no parent").isNull();
        assertThat(root.getTops()).as("remote target in root.tops").contains(remoteTarget);
        Node remoteParent = node(opts().path("/remote/node_modules/a").pkg(obj("name", "a", "version", "1.2.3"))
          .root(root));
        assertThat(remoteParent.getChildren().get("x")).isSameAs(remoteTarget);
        treeCheck(root);
    }

    @Test
    void childOfLinkTargetHasPathLikeParent() {
        Node root = node(opts().pkg(obj("name", "root", "dependencies", obj("a", "", "link", "", "link2", "")))
          .path("/srv/user/projects/root").realpath("/srv/user/projects/root"));
        node(opts().pkg(obj("name", "a", "version", "1.2.3")).parent(root).name("a"));
        Link link = link(opts().pkg(obj("name", "link", "version", "1.2.3", "dependencies", obj("a", "", "kid", "")))
          .realpath(root.getRealpath() + "/link-target").parent(root).fsParent(root));
        Node linkKid = node(opts().pkg(obj("name", "kid")).parent(link));
        assertThat(linkKid.getParent()).as("setting link as parent sets target instead").isSameAs(link.getTarget());
        assertThat(linkKid.getPath()).as("child of link target path is realpath").isEqualTo(linkKid.getRealpath());
    }

    @Test
    void changingRoot() {
        Node root = node(opts().pkg(obj("name", "root", "dependencies", obj("a", "", "link", "", "link2", "")))
          .path("/srv/user/projects/root").realpath("/srv/user/projects/root")
          .meta(emptyLock("/srv/user/projects/root")));
        Node a = node(opts().pkg(obj("name", "a", "version", "1.2.3")).parent(root).name("a")
          .resolved("https://example.com/a-1.2.3.tgz").integrity("sha512-asdfasdfasdf"));
        Node b = node(opts().pkg(obj("name", "b", "version", "1.2.3")).parent(a).name("b"));
        Node root2 = node(opts().pkg(obj("name", "root2", "dependencies", obj("a", "", "link", "", "link2", "")))
          .path("/srv/user/projects/root2").realpath("/srv/user/projects/root2")
          .meta(emptyLock("/srv/user/projects/root2")));
        assertThat(a.getRoot()).isSameAs(root);
        assertThat(b.getRoot()).isSameAs(root);
        assertTree("changing root: before, root", root);
        a.setParent(root2);
        assertTree("changing root: after, root", root);
        assertTree("changing root: after, root2", root2);
        for (String location : root2.getInventory().keys()) {
            assertGet("changing root: root2 " + location, root2.getMeta(), location);
        }
        assertGet("changing root: root node_modules/a", root.getMeta(), "node_modules/a");
        assertThat(a.getRoot()).as("root is set when parent is changed").isSameAs(root2);
        assertThat(b.getRoot()).as("root is set on children when parent is changed").isSameAs(root2);
    }

    @Test
    void attemptToAssignParentToSelfOnRootNode() {
        Node root = node(opts().pkg(obj("name", "root")).path("/").realpath("/"));
        root.setFsParent(root);
        root.setParent(root);
        assertThat(root.getParent()).isNull();
        assertThat(root.getFsParent()).isNull();
    }

    @Test
    void bundledDependenciesLogic() {
        Node root = node(opts()
          .pkg(obj("name", "root", "dependencies", obj("a", "", "b", "", "d", "", "e", "", "f", ""),
          "bundleDependencies", arr("a"))).path("/path/to/root").realpath("/path/to/root"));
        Node a = node(opts().pkg(obj("name", "a", "version", "1.2.3", "dependencies", obj("b", "", "aa", "")))
          .parent(root));
        Node aa = node(opts().pkg(obj("name", "aa", "version", "1.2.3")).parent(a));
        Node b = node(opts().pkg(obj("name", "b", "version", "1.2.3", "dependencies", obj("c", ""))).parent(root));
        Node c = node(opts().pkg(obj("name", "c", "version", "1.2.3", "dependencies", obj("cc", ""))).parent(root));
        node(opts().pkg(obj("name", "cc", "version", "1.2.3", "dependencies", obj("d", ""))).parent(c));
        Node d = node(opts().pkg(obj("name", "d", "version", "1.2.3")).parent(root));
        Node e = node(opts().pkg(obj("name", "e", "version", "1.2.3")).parent(root));
        Node f = node(opts().pkg(obj("name", "f", "version", "1.2.3", "dependencies", obj("fa", "", "fb", ""),
          "bundleDependencies", arr("fb"))).parent(root));
        node(opts().pkg(obj("name", "fa", "version", "1.2.3")).parent(f));
        Node fb = node(opts().pkg(obj("name", "fb", "version", "1.2.3", "dependencies", obj("e", "", "fc", "")))
          .parent(f));
        node(opts().pkg(obj("name", "fc", "version", "1.2.3", "dependencies", obj("fb", ""))).parent(f));

        assertThat(a.isInBundle()).as("bundled dep is bundled").isTrue();
        assertThat(a.isInDepBundle()).as("bundled dep is bundled by root").isFalse();
        assertThat(aa.isInBundle()).as("child of bundled dep is bundled").isTrue();
        assertThat(aa.isInDepBundle()).as("child of dep bundled by root is not dep bundled").isFalse();
        assertThat(b.isInBundle()).as("dep of bundled dep at peer level is bundled").isTrue();
        assertThat(c.isInBundle()).as("metadep of bundled dep at peer level is bundled").isTrue();
        assertThat(d.isInBundle()).as("deduped metadep of bundled metadep is bundled").isTrue();
        assertThat(e.isInBundle()).as("deduped dep of bundled dep of metadep is not bundled").isFalse();
        assertThat(fb.isInBundle()).as("bundled dep of dep is bundled").isTrue();
        assertThat(fb.isInDepBundle()).as("bundled dep of dep is dep bundled (not by root)").isTrue();
    }

    @Test
    void moveFsChildrenWhenMovingToANewFsParentInSameRoot() {
        Node root = node(opts().path("/path/to/root"));
        Node p1 = node(opts().path("/path/to/root/p1").root(root));
        assertThat(p1.getFsParent()).isSameAs(root);
        Node p2 = node(opts().path("/path/to/root/p2").root(root));
        assertThat(p2.getFsParent()).isSameAs(root);
        Node c2 = node(opts().path("/path/to/root/p2/c2").root(root));
        assertThat(c2.getFsParent()).isSameAs(p2);
        p2.setFsParent(p1);
        assertThat(c2.getPath()).isEqualTo("/path/to/root/p1/p2/c2");
    }

    @Test
    void checkIfANodeIsInANodeModulesFolderOrNot() {
        assertThat(node(opts().path("/path/to/foo/node_modules/a").realpath("/path/to/foo/node_modules/a")
          .pkg(obj("name", "a"))).inNodeModules()).as("basic obvious case").isEqualTo("/path/to/foo");
        assertThat(node(opts().path("/path/to/foo/node_modules/a").realpath("/path/to/foo/node_modules/a")
          .pkg(obj("name", "b"))).inNodeModules()).as("based on path name, not pkg name").isEqualTo("/path/to/foo");
        assertThat(node(opts().path("/path/to/foo/node_modules/a/b/c").realpath("/path/to/foo/node_modules/a/b/c")
          .pkg(obj("name", "c"))).inNodeModules()).as("not directly in node_modules").isNull();
        assertThat(node(opts().path("/path/to/foo/node_modules/@c/d").realpath("/path/to/foo/node_modules/@c/d")
          .pkg(obj("name", "@a/b/c/d/e"))).inNodeModules()).as("scoped package in node_modules")
          .isEqualTo("/path/to/foo");
    }

    @Test
    void updateMetadataWhenMovingBetweenLinkedTopOfTreeParents() {
        Node root = node(opts().pkg(obj("name", "root")).path("/srv/user/projects/root")
          .realpath("/srv/user/projects/root")
          .meta(emptyLock("/srv/user/projects/root")));
        Node top1 = node(opts().pkg(obj("name", "top", "version", "1.1.1")).path("/path/to/top1")
          .realpath("/path/to/top1")
          .meta(emptyLock("/path/to/top1")));
        link(opts().name("link1").parent(root).realpath(top1.getPath()).target(top1));
        Node top2 = node(opts().pkg(obj("name", "top", "version", "1.1.1")).path("/path/to/top2")
          .realpath("/path/to/top2")
          .meta(emptyLock("/path/to/top2")));
        Link link2 = link(opts().name("link2").parent(root).realpath(top2.getPath()).target(top2));
        Node child = node(opts().parent(top1)
          .pkg(obj("name", "child", "version", "1.2.3", "dependencies", obj("child2", "2")))
          .resolved("https://child.com/-/child-1.2.3.tgz").integrity("sha512-blortzeyblartzeyfartz"));
        Node child2 = node(opts().parent(child).pkg(obj("name", "child2", "version", "2.3.4"))
          .resolved("https://child.com/-/child-2.3.4.tgz").integrity("sha512-a childs child is a kidkid"));

        assertThat(child.getLocation()).isEqualTo("../../../../path/to/top1/node_modules/child");
        assertThat(child.getRoot()).isSameAs(root);
        assertThat(child.getTop()).isSameAs(top1);
        assertThat(child2.getLocation()).isEqualTo("../../../../path/to/top1/node_modules/child/node_modules/child2");
        assertThat(child2.getRoot()).isSameAs(root);
        assertThat(child2.getTop()).isSameAs(top1);
        assertGet("moving: metadata from root", root.getMeta(), child.getLocation());
        assertGet("moving: metadata from top1", top1.getMeta(), child.getLocation());

        String oldLocation = child.getLocation();
        String oldLocation2 = child2.getLocation();
        child.setParent(link2);
        assertThat(child.getTop()).isSameAs(top2);
        assertThat(child.getParent()).as("parent assigned to link target").isSameAs(top2);
        assertThat(child.getLocation()).isEqualTo("../../../../path/to/top2/node_modules/child");
        assertThat(child2.getTop()).isSameAs(top2);
        assertThat(child2.getParent()).isSameAs(child);
        assertThat(child2.getLocation()).isEqualTo("../../../../path/to/top2/node_modules/child/node_modules/child2");
        assertGet("moving: root metadata updated 1", root.getMeta(), child.getLocation());
        assertGet("moving: root metadata updated 2", root.getMeta(), child2.getLocation());
        assertGet("moving: old location deleted from root", root.getMeta(), oldLocation);
        assertGet("moving: old location deleted from top1", top1.getMeta(), oldLocation);
        assertGet("moving: old location2 deleted from root", root.getMeta(), oldLocation2);
        assertGet("moving: old location2 deleted from top1", top1.getMeta(), oldLocation2);
        assertGet("moving: new top metadata updated 1", top2.getMeta(), child.getLocation());
        assertGet("moving: new top metadata updated 2", top2.getMeta(), child2.getLocation());
        assertTree("moving: final", root);
    }

    @Test
    void settingPackageRefreshesDeps() {
        Node root = node(opts().pkg(obj("dependencies", obj("a", "1"))).path("/path/to/root"));
        Node a = node(opts().pkg(obj("name", "a", "version", "1.2.3")).parent(root));
        assertThat(root.getEdgesOut().get("a").isValid()).as("dep is valid before updating pkg").isTrue();
        root.setPackage(obj("dependencies", obj("a", "2")));
        assertThat(root.getEdgesOut().get("a").isValid()).as("dep is invalid after updating pkg").isFalse();
        a.setPackage(obj("name", "a", "version", "2.3.4"));
        assertThat(root.getEdgesOut().get("a").isValid()).as("dep is valid again after updating dep pkg").isTrue();
    }

    @Test
    void nodesInShrinkwraps() {
        Node root = node(opts().pkg(obj("dependencies", obj("a", ""))).path("/path/to/root")
          .child(opts().name("a")
          .pkg(obj("name", "a", "version", "1.2.3", "dependencies", obj("b", ""), "_hasShrinkwrap", true))
            .child(opts().name("b").pkg(obj("version", "1.2.3", "name", "b", "dependencies", obj("c", "")))
              .child(opts().name("c").pkg(obj("name", "c", "version", "1.2.3")).build()).build()).build()));
        Node a = child(root, "a");
        Node b = child(a, "b");
        Node c = child(b, "c");
        assertThat(a.getHasShrinkwrap()).as("a has a shrinkwrap").isEqualTo(true);
        assertThat(b.isInShrinkwrap()).as("b is in shrinkwrap").isTrue();
        assertThat(c.isInShrinkwrap()).as("c is in shrinkwrap").isTrue();
    }

    @Test
    void hasInstallScript() {
        Node node = node(opts().pkg(obj()).path("/a/b/c"));
        assertThat(node.hasInstallScript()).isFalse();
        node.setPackage(obj("scripts", obj("postinstall", "hello")));
        assertThat(node.hasInstallScript()).isTrue();
        node.setPackage(obj("scripts", obj("nothing", "of interest")));
        assertThat(node.hasInstallScript()).isFalse();
        node.setPackage(obj("hasInstallScript", true));
        assertThat(node.hasInstallScript()).isTrue();
    }

    @Test
    void legacyPeerDependencies() {
        Node root = node(opts().pkg(obj("name", "root", "peerDependencies", obj("foo", "1.x")))
          .path("/srv/user/projects/root").realpath("/srv/user/projects/root").legacyPeerDeps(true));
        Node foo = node(opts().pkg(obj("name", "foo", "version", "1.2.3")).path("/srv/user/projects/root/foo")
          .realpath("/srv/user/projects/root/foo").legacyPeerDeps(true).parent(root));
        assertThat(root.getChildren().get("foo")).as("should be a children").isSameAs(foo);
        assertThat(root.getEdgesOut().size()).as("should have no edges out").isZero();
    }

    @Test
    void setWorkspaces() {
        Node root = node(opts().pkg(obj("name", "workspaces_root")).path("/srv/user/projects/workspaces_root")
          .realpath("/srv/user/projects/workspaces_root"));
        Link link = link(opts().parent(root).target(node(opts().path("/srv/user/projects/workspaces_root/foo")
          .pkg(obj("name", "foo", "version", "1.2.3")))));
        Link unknown = link(opts().parent(root).target(node(opts().path("/srv/user/projects/workspaces_root/unknown")
          .pkg(obj("name", "unknown", "version", "1.2.3")))));
        root.setWorkspaces(workspaces("foo", "/srv/user/projects/workspaces_root/foo",
          "bar", "/srv/user/projects/workspaces_root/bar"));

        assertThat(describe(root)).isEqualTo(treeDump("should setup edges out for each workspace"));
        assertThat(link.isWorkspace()).as("link node reports isWorkspace true").isTrue();
        assertThat(link.getTarget().isWorkspace()).as("target reports isWorkspace true").isTrue();
        assertThat(root.isWorkspace()).as("root is not a workspace").isFalse();
        assertThat(unknown.isWorkspace()).as("unknown node is not a workspace").isFalse();
    }

    @Test
    void getWorkspaces() {
        Node root = node(opts().pkg(obj("name", "workspaces_root")).path("/srv/user/projects/workspaces_root")
          .realpath("/srv/user/projects/workspaces_root"));
        assertThat(root.getWorkspaces()).as("should default to null when no workspaces defined").isNull();
        Map<String, String> ws = new LinkedHashMap<>();
        root.setWorkspaces(ws);
        assertThat(root.getWorkspaces()).isSameAs(ws);
    }

    @Test
    void replaceWorkspaces() {
        Node root = node(opts().pkg(obj("name", "workspaces_root")).path("/srv/user/projects/workspaces_root")
          .realpath("/srv/user/projects/workspaces_root"));
        root.setWorkspaces(workspaces("foo", "/srv/user/projects/workspaces_root/foo",
          "bar", "/srv/user/projects/workspaces_root/bar"));
        Map<String, String> ws = new LinkedHashMap<>();
        root.setWorkspaces(ws);
        assertThat(root.getWorkspaces()).as("should remove previously set workspaces").isSameAs(ws);
    }

    @Test
    void replaceWorkspacesKeepingExistingEdgesOut() {
        Node root = node(opts().pkg(obj("name", "workspaces_root")).path("/srv/user/projects/workspaces_root")
          .realpath("/srv/user/projects/workspaces_root"));
        root.setWorkspaces(workspaces("foo", "/srv/user/projects/workspaces_root/foo"));
        Map<String, String> ws = workspaces("foo", "/srv/user/projects/workspaces_root/foo",
          "bar", "/srv/user/projects/workspaces_root/bar");
        root.setWorkspaces(ws);
        assertThat(root.getWorkspaces()).as("should keep existing edges out").isSameAs(ws);
    }

    @Test
    void doNotRelyOnLegacyResolvedForFileNodes() {
        Node old = node(opts()
          .pkg(obj("_resolved", "file:/x/y/z/blorg.tgz", "_where", "/why/did/i/think/this/was/a/good/idea"))
          .path("/some/completely/different/path"));
        assertThat(old.getResolved()).isNull();
        Node notOld = node(opts().pkg(obj("_resolved", "file:/x/y/z/blorg.tgz"))
          .path("/some/completely/different/path"));
        assertThat(notOld.getResolved()).isEqualTo("file:/x/y/z/blorg.tgz");
    }

    @Test
    void reparentingKeepsChildrenInRootInventory() {
        Node root = node(opts().path("/some/path"));
        Node nested = node(opts().fsParent(root).path("/some/path/node_modules/parent/node_modules/nested"));
        Node fsNested = node(opts().fsParent(nested).path("/some/path/node_modules/parent/node_modules/nested/x"));
        Node kid = node(opts().name("kid").parent(nested));
        assertThat(root.getInventory().has(kid)).isTrue();
        assertThat(root.getInventory().has(fsNested)).isTrue();

        Node parent = node(opts().name("parent").parent(root));
        assertThat(nested.getParent()).isSameAs(parent);
        assertThat(root.getInventory().has(kid)).isTrue();
        assertThat(root.getInventory().has(fsNested)).isTrue();
    }

    @Test
    void reloadingNamedEdgesShouldRefreshEdgesIn() {
        Node root = node(opts().path("/some/path").pkg(obj("dependencies", obj("x", "1"))));
        assertEdge(root, "x", "1", Edge.MISSING, null);

        Node x1 = node(opts().pkg(obj("name", "x", "version", "1.0.0", "dependencies", obj("y", "1"))).parent(root));
        assertEdge(root, "x", "1", null, x1);
        assertEdge(x1, "y", "1", Edge.MISSING, null);

        Node y1 = node(opts().pkg(obj("name", "y", "version", "1.0.0", "dependencies", obj("x", "2"))).parent(root));
        assertEdge(root, "x", "1", null, x1);
        assertEdge(x1, "y", "1", null, y1);
        assertEdge(y1, "x", "2", Edge.INVALID, x1);

        Node y1x2 = node(opts().pkg(obj("name", "x", "version", "2.0.0", "dependencies", obj("y", "2"))).parent(y1));
        assertEdge(root, "x", "1", null, x1);
        assertEdge(x1, "y", "1", null, y1);
        assertEdge(y1, "x", "2", null, y1x2);
        assertEdge(y1x2, "y", "2", Edge.INVALID, y1);

        Node y1y2 = node(opts().pkg(obj("name", "y", "version", "2.0.0", "dependencies", obj("x", "1"))).parent(y1));
        assertEdge(root, "x", "1", null, x1);
        assertEdge(x1, "y", "1", null, y1);
        assertEdge(y1, "x", "2", null, y1x2);
        assertEdge(y1x2, "y", "2", null, y1y2);
        assertEdge(y1y2, "x", "1", Edge.INVALID, y1x2);

        Node y1y2x1 = node(opts().pkg(obj("name", "x", "version", "1.0.0", "dependencies", obj("y", "1")))
          .parent(y1y2));
        assertEdge(root, "x", "1", null, x1);
        assertEdge(x1, "y", "1", null, y1);
        assertEdge(y1, "x", "2", null, y1x2);
        assertEdge(y1x2, "y", "2", null, y1y2);
        assertEdge(y1y2, "x", "1", null, y1y2x1);
        assertEdge(y1y2x1, "y", "1", Edge.INVALID, y1y2);

        Node y1y2y1 = node(opts().pkg(obj("name", "y", "version", "1.0.0", "dependencies", obj("x", "2")))
          .parent(y1y2));
        assertEdge(root, "x", "1", null, x1);
        assertEdge(x1, "y", "1", null, y1);
        assertEdge(y1, "x", "2", null, y1x2);
        assertEdge(y1x2, "y", "2", null, y1y2);
        assertEdge(y1y2, "x", "1", null, y1y2x1);
        assertEdge(y1y2x1, "y", "1", null, y1y2y1);
        assertEdge(y1y2y1, "x", "2", Edge.INVALID, y1y2x1);
    }

    private static void assertEdge(Node from, String name, String spec, @Nullable String error, @Nullable Node to) {
        Edge edge = from.getEdgesOut().get(name);
        assertThat(edge).isNotNull();
        assertThat(edge.getSpec()).isEqualTo(spec);
        if (Edge.MISSING.equals(error)) {
            assertThat(edge.isMissing()).isTrue();
        } else {
            assertThat(edge.isInvalid()).isEqualTo(Edge.INVALID.equals(error));
            assertThat(edge.getTo()).isSameAs(to);
        }
    }

    @Test
    void detectThatTwoNodesAreTheSameThing() {
        Node x = node(opts().path("/x"));
        check(x, x, true);

        Node p = node(opts().path("/foo"));
        check(node(opts().name("a").parent(p).integrity("sha512-xyz")),
          node(opts().name("b").parent(p).integrity("sha512-xyz")), false);

        Node root = node(opts().path("/root"));
        Node target = node(opts().root(root).path("/foo").pkg(obj("name", "x", "version", "1.2.3")));
        check(link(opts().root(root).path("/a/x").target(target)), link(opts().root(root).path("/b/x").target(target)),
          true);

        check(node(opts().path("/foo").pkg(obj("name", "x", "version", "1.2.3"))),
          node(opts().path("/foo").pkg(obj("name", "x", "version", "1.2.3"))), true);
        check(node(opts().path("/a/x").pkg(obj("name", "x", "version", "1.2.3"))),
          node(opts().path("/b/x").pkg(obj("name", "x", "version", "1.2.3"))), false);

        {
            Node r = node(opts().path("/x"));
            String integrity = "sha512-xyzabc";
            Node a = node(opts().parent(r).name("x").integrity(integrity));
            Node b = node(opts().parent(a).name("x").integrity(integrity));
            assertThat(a.getIntegrity()).isEqualTo(integrity);
            assertThat(a.getIntegrity()).isEqualTo(b.getIntegrity());
            check(a, b, true);
        }
        {
            Node r = node(opts().path("/x"));
            Map<String, Object> pkg = obj("name", "x", "version", "1.2.3");
            String resolved = "https://registry.npmjs.org/x/-/x-1.2.3.tgz";
            Node a = node(opts().parent(r).pkg(pkg).integrity("sha512-xyzabc").resolved(resolved));
            Node b = node(opts().parent(a).pkg(pkg).integrity("sha512-foobar").resolved(resolved));
            assertThat(a.getIntegrity()).isEqualTo("sha512-xyzabc");
            assertThat(b.getIntegrity()).isEqualTo("sha512-foobar");
            check(a, b, false);
        }
        {
            Node r = node(opts().path("/x"));
            String resolved = "https://registry.npmjs.org/x/-/x-1.2.3.tgz";
            Node a = node(opts().parent(r).pkg(obj("name", "x", "version", "1.2.3-a")).resolved(resolved));
            Node b = node(opts().parent(a).pkg(obj("name", "x", "version", "1.2.3-b")).resolved(resolved));
            check(a, b, true);
        }
        {
            Node r = node(opts().path("/x"));
            Node a = node(opts().parent(r).pkg(obj("name", "x", "version", "1.2.3")));
            Node b = node(opts().parent(a).pkg(obj("name", "x", "version", "1.2.3")));
            check(a, b, true);
        }
        {
            Node r = node(opts().path("/x"));
            Node a = node(opts().parent(r).pkg(obj("name", "x", "version", "1.2.3-a")));
            Node b = node(opts().parent(a).pkg(obj("name", "x", "version", "1.2.3-b")));
            check(a, b, false);
        }
    }

    private static void check(Node a, Node b, boolean expect) {
        assertThat(a.matches(b)).isEqualTo(expect);
        if (a != b) {
            assertThat(b.matches(a)).isEqualTo(expect);
        }
    }

    @Test
    void nodeSatisfiesRequested() {
        Node node = node(opts().path("/some/path/to/foo").resolved("https://registry.npmjs.org/foo/-/foo-1.2.3.tgz")
          .pkg(obj("name", "foo", "version", "1.2.3")));
        assertThat(node.satisfies("foo")).isTrue();
        assertThat(node.satisfies("foo@1")).isTrue();
        assertThat(node.satisfies("https://registry.npmjs.org/foo/-/foo-1.2.3.tgz")).isTrue();
        assertThat(node.satisfies("foo@2")).isFalse();
        assertThat(node.satisfies("bar")).isFalse();
        assertThat(node.satisfies("https://registry.npmjs.org/foo/-/foo-1.2.5.tgz")).isFalse();
        node.setResolved("git+ssh://git@github.com/org/foo.git#decafbad1100facefaceface");
        assertThat(node.satisfies("https://registry.npmjs.org/foo/-/foo-1.2.3.tgz")).isFalse();
        assertThat(node.satisfies("org/foo")).isTrue();
        assertThat(node.satisfies("github:org/foo")).isTrue();
    }

    @Test
    void nodePkgid() {
        Node parent = node(opts().path("/some/path"));
        assertThat(parent.getPkgid()).isEqualTo("path@");
        parent.setPackage(obj("name", "parent"));
        assertThat(parent.getPkgid()).isEqualTo("parent@");
        parent.setPackage(obj("name", "parent", "version", "1.2.3"));
        assertThat(parent.getPkgid()).isEqualTo("parent@1.2.3");

        Node n = node(opts().path("/some/path/node_modules/foo").parent(parent));
        assertThat(n.getPkgid()).isEqualTo("foo@");
        n.setPackage(obj("name", "foo", "version", "1.2.3"));
        assertThat(n.getPkgid()).isEqualTo("foo@1.2.3");
        n.setPackage(obj("name", "bar", "version", "1.2.3"));
        assertThat(n.getPkgid()).isEqualTo("foo@npm:bar@1.2.3");
        // `const { name = '', version = '' } = this.package` defaults undefined only: null renders as null
        n.setPackage(obj("name", null, "version", null));
        assertThat(n.getPkgid()).isEqualTo("foo@null");
        n.setPackage(obj("name", 5, "version", 1e21));
        assertThat(n.getPkgid()).isEqualTo("foo@npm:5@1e+21");
    }

    @Test
    void nodeVersion() {
        Node n = node(opts().path("/some/path"));
        assertThat(n.getVersion()).isEqualTo("");
        n.getPackage().put("version", "1.2.3");
        assertThat(n.getVersion()).isEqualTo("1.2.3");
    }

    @Test
    void explainYourself() {
        Node n = node(opts().path("/some/path").pkg(obj("dependencies", obj("x", "1", "y", "2"))));
        assertThat(n.explain()).isEqualTo(obj("location", "/some/path"));
        assertThat(n.explain()).as("caches result").isSameAs(n.explain());
        Node x = node(opts().parent(n).pkg(obj("name", "x", "version", "1.2.3")));
        assertThat(x.explain()).isEqualTo(why("x", "1.2.3", "node_modules/x", false,
          dependent("prod", "x", "1", n.explain())));

        Node virtual = node(opts().path("/virtual-root").sourceReference(x));
        assertThat(virtual.explain()).isEqualTo(x.explain());

        Node y = node(opts().parent(n).pkg(obj("name", "y", "version", "2.3.4", "dependencies", obj("z", "3")))
          .child(opts().pkg(obj("name", "z", "version", "3.4.5", "dependencies", obj("a", "4")))
            .child(opts().pkg(obj("name", "a", "version", "4.5.6", "dependencies", obj())).build()).build()));
        Node z = child(y, "z");
        Node a = child(z, "a");
        assertThat(y.explain())
          .isEqualTo(why("y", "2.3.4", "node_modules/y", false, dependent("prod", "y", "2", n.explain())));
        assertThat(z.explain()).isEqualTo(why("z", "3.4.5", "node_modules/y/node_modules/z", false,
          dependent("prod", "z", "3", y.explain())));
        assertThat(a.explain()).isEqualTo(why("a", "4.5.6", "node_modules/y/node_modules/z/node_modules/a", false,
          dependent("prod", "a", "4", z.explain())));

        y.setPackage(withDependency(y.getPackage(), "b", "1.2.3"));
        a.setPackage(withDependency(a.getPackage(), "b", "1.2.3"));
        Node b = node(opts().parent(n).pkg(obj("name", "b", "version", "9.9.9")));
        assertThat(b.explain()).isEqualTo(why("b", "9.9.9", "node_modules/b", false));
        b.setPackage(new LinkedHashMap<>(b.getPackage()));
        n.setPackage(withDependency(n.getPackage(), "b", "1.2.3"));
        assertThat(b.explain()).isEqualTo(why("b", "9.9.9", "node_modules/b", false,
          obj("type", "prod", "name", "b", "spec", "1.2.3", "error", "INVALID", "from", n.explain())));

        b.setPackage(new LinkedHashMap<>(b.getPackage()));
        Node otherNode = node(opts().pkg(withDependency(n.getPackage(), "b", "9")).path("/virtual-root")
          .child(opts().pkg(new LinkedHashMap<>(b.getPackage())).build()));
        assertThat(b.explain(otherNode.getEdgesOut().get("b"))).isEqualTo(why("b", "9.9.9", "node_modules/b", false,
          dependent("prod", "b", "9", obj("location", "/virtual-root"))));

        Node cycle = node(opts().path("/cy/cle").pkg(obj("name", "cycle-root", "dependencies", obj("c", "1")))
          .child(opts().pkg(obj("name", "a", "version", "1.1.1", "dependencies", obj("b", "1"))).build())
          .child(opts().pkg(obj("name", "b", "version", "1.1.1", "dependencies", obj("a", "1"))).build())
          .child(opts().pkg(obj("name", "c", "version", "1.1.1", "dependencies", obj("a", "1"))).build()));
        assertThat(child(cycle, "b").explain()).isEqualTo(why("b", "1.1.1", "node_modules/b", false,
          dependent("prod", "b", "1", why("a", "1.1.1", "node_modules/a", false,
            dependent("prod", "a", "1", obj("name", "b", "version", "1.1.1")),
            dependent("prod", "a", "1", why("c", "1.1.1", "node_modules/c", false,
              dependent("prod", "c", "1", obj("location", "/cy/cle"))))))));

        Node actual = child(node(opts().path("/project").pkg(obj("dependencies", obj("a", "1")))
          .child(opts().pkg(obj("name", "a", "version", "1.2.3", "dependencies", obj("b", "1"))).build())
          .child(opts().pkg(obj("name", "b", "version", "1.2.3", "dependencies", obj("c", "1", "d", "1"))).build())),
          "b");
        Node virtualB = node(opts().path("/virtual-root").pkg(new LinkedHashMap<>(actual.getPackage()))
          .sourceReference(actual)
          .child(opts().pkg(obj("name", "c", "version", "1.2.3", "dependencies", obj("d", "1"))).build())
          .child(opts().pkg(obj("name", "d", "version", "1.2.3")).build()));
        Edge edge = child(virtualB, "c").getEdgesOut().get("d");
        Map<String, Object> whileInstalling = obj("name", "b", "version", "1.2.3", "path", "/project/node_modules/b");
        Map<String, Object> d = obj("name", "d", "version", "1.2.3", "whileInstalling", whileInstalling,
          "location", "node_modules/d", "isWorkspace", false, "dependents", arr(dependent("prod", "d", "1",
            obj("name", "c", "version", "1.2.3", "whileInstalling", whileInstalling, "location", "node_modules/c",
              "isWorkspace", false, "dependents", arr(dependent("prod", "c", "1",
                why("b", "1.2.3", "node_modules/b", false, dependent("prod", "b", "1",
                  why("a", "1.2.3", "node_modules/a", false,
                    dependent("prod", "a", "1", obj("location", "/project")))))))))));
        assertThat(child(virtualB, "d").explain(edge)).isEqualTo(d);

        Node badParent = node(opts().path("/bad/nodes"));
        IllegalStateException badNode = new IllegalStateException("bad node");
        Node errNode = node(opts().error(badNode).pkg(obj("name", "bad", "version", "node")).parent(badParent));
        Map<String, Object> errWhy = errNode.explain();
        assertThat(errWhy.get("errors")).isEqualTo(Collections.singletonList(badNode));
        assertThat(errWhy.get("name")).isEqualTo("bad");
        assertThat(errWhy.get("version")).isEqualTo("node");
        assertThat(errWhy.get("package")).isEqualTo(errNode.getPackage());
        Node noPkgDep = node(opts().pkg(obj("noname", "bad", "noversion", "node")).parent(badParent)
          .path("/bad/nodes/node_modules/noname"));
        Map<String, Object> noPkgWhy = noPkgDep.explain();
        assertThat((List<?>) noPkgWhy.get("errors")).singleElement()
          .satisfies(e -> assertThat(((Throwable) e).getMessage()).isEqualTo("invalid package: lacks name and/or " +
                  "version"));
        assertThat(noPkgWhy.get("package")).isEqualTo(obj("noname", "bad", "noversion", "node"));

        Node workspacesRoot = node(opts().path("/some/path")
          .pkg(obj("name", "project-root", "version", "1.0.0", "workspaces", arr("a"))));
        Node ws = node(opts().root(workspacesRoot).path("/some/path/a").pkg(obj("name", "a", "version", "1.0.0")));
        link(opts().name("a").parent(workspacesRoot).target(ws));
        workspacesRoot.setWorkspaces(workspaces("a", "/some/path/a"));
        Map<String, Object> wsWhy = why("a", "1.0.0", "a", true);
        wsWhy.put("linksIn", arr(why("a", "1.0.0", "node_modules/a", true,
          dependent("workspace", "a", "file:/some/path/a", obj("location", "/some/path")))));
        assertThat(ws.explain()).as("should have workspaces properly set up").isEqualTo(wsWhy);
    }

    private static Map<String, Object> why(String name, String version, String location, boolean isWorkspace,
      Object... dependents) {
        return obj("name", name, "version", version, "location", location, "isWorkspace", isWorkspace,
          "dependents", new ArrayList<>(Arrays.asList(dependents)));
    }

    private static Map<String, Object> dependent(String type, String name, String spec, Map<String, Object> from) {
        return obj("type", type, "name", name, "spec", spec, "from", from);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> withDependency(Map<String, Object> pkg, String name, String spec) {
        Map<String, Object> copy = new LinkedHashMap<>(pkg);
        Map<String, Object> deps = new LinkedHashMap<>(pkg.get("dependencies") instanceof Map ?
          (Map<String, Object>) pkg.get("dependencies") : Collections.emptyMap());
        deps.put(name, spec);
        copy.put("dependencies", deps);
        return copy;
    }

    @Test
    void guardAgainstSettingPackageToSomethingImproper() {
        assertThat(node(opts().path("/some/path")).getPackage()).isEmpty();
        assertThat(node(opts().path("/some/path").pkg(null)).getPackage()).isEmpty();
        Node p = node(opts().path("/some/path").pkg("hello"));
        assertThat(p.getPackage()).isEmpty();
        p.setPackage("this is not an object");
        assertThat(p.getPackage()).isEmpty();
    }

    @Test
    void clearInventoryWhenChangingRoot() {
        Node r1 = node(opts().path("/root1"));
        Node r2 = node(opts().path("/root1/root2").child(opts().pkg(obj("name", "foo", "version", "1.2.3")).build()));
        Node r3 = node(opts().path("/root1/root3"));
        Node child3 = node(opts().path("/root1/root2/node_modules/foo").pkg(obj("name", "foo", "version", "1.2.3"))
          .root(r3));
        Node child = child(r2, "foo");
        assertThat(r1.getInventory().size()).isEqualTo(1);
        assertThat(r2.getInventory().size()).isEqualTo(2);
        assertThat(r1.getInventory().has(child)).isFalse();
        assertThat(r2.getInventory().has(child)).isTrue();
        r2.setRoot(r1);
        assertThat(r1.getInventory().size()).isEqualTo(3);
        assertThat(r2.getInventory().size()).isZero();
        assertThat(r1.getInventory().has(child)).isTrue();
        assertThat(r2.getInventory().has(child)).isFalse();
        assertThat(r3.getInventory().size()).isEqualTo(2);
        r3.setRoot(r1);
        assertThat(r1.getInventory().size()).isEqualTo(4);
        assertThat(r3.getInventory().size()).isZero();
        assertThat(r1.getInventory().has(child3)).isTrue();
        assertThat(child3.getRoot()).isSameAs(r1);
    }

    @Test
    void createANodeThatDoesntGetAddedToARootUntilLater() {
        Node root = node(opts().path("/path/to/root"));
        Node foo = node(opts().name("foo"));
        assertThat(foo.getRoot()).as("other is self-rooted at first").isSameAs(foo);
        assertThat(foo.getPath()).isNull();
        foo.setParent(root);
        assertThat(foo.getRoot()).isSameAs(root);
        assertThat(foo.getPath()).isEqualTo("/path/to/root/node_modules/foo");
    }

    @Test
    void changingPathToANodeModulesFolderSetsNameIfNecessary() {
        Node node = node(opts().path("/some/random/path"));
        assertThat(node.getName()).isEqualTo("path");
        node.changePath("/path/to/node_modules/foo");
        assertThat(node.getName()).isEqualTo("foo");
        node.changePath("/path/to/node_modules/foo/node_modules/bar");
        assertThat(node.getName()).isEqualTo("bar");
        node.changePath("/path/to/node_modules/foo/node_modules/@bar/baz/node_modules");
        assertThat(node.getName()).isEqualTo("bar");
        node.changePath("/path/to/node_modules/foo/node_modules/@bar/baz");
        assertThat(node.getName()).isEqualTo("@bar/baz");
    }

    @Test
    void isProjectRootShowsIfTheNodeIsTheRootLinkTarget() {
        Link link = link(opts().path("/link").realpath("/actual"));
        Node n = node(opts().path("/actual").root(link));
        assertThat(n.isProjectRoot()).isTrue();
        assertThat(link.isProjectRoot()).isTrue();
        assertThat(link.isRoot()).isTrue();
        assertThat(n.isRoot()).isFalse();
    }

    @Test
    void virtualReferencesToRootNodeHasDevDepEdges() {
        Node root = node(opts().path("/some/project/path").pkg(obj("devDependencies", obj("a", "1"))));
        Node virtualRoot = node(opts().path("/virtual-root").sourceReference(root));
        assertThat(virtualRoot.getEdgesOut().get("a").getType()).isEqualTo(Edge.Type.DEV);
    }

    @Nested
    class DuplicatedDependencies {

        @Test
        void preferProdOverPeer() {
            Node n = node(opts().path("/path/to/project")
              .pkg(obj("dependencies", obj("foo", "1.x"), "peerDependencies", obj("foo", ">=1"))));
            assertThat(n.getEdgesOut().get("foo").getType()).isEqualTo(Edge.Type.PROD);
            assertThat(n.getEdgesOut().get("foo").getSpec()).isEqualTo("1.x");
        }

        @Test
        void preferDevOverPeer() {
            Node n = node(opts().path("/path/to/project")
              .pkg(obj("devDependencies", obj("foo", "1.x"), "peerDependencies", obj("foo", ">=1"))));
            assertThat(n.getEdgesOut().get("foo").getType()).isEqualTo(Edge.Type.DEV);
            assertThat(n.getEdgesOut().get("foo").getSpec()).isEqualTo("1.x");
        }

        @Test
        void preferProdOverDev() {
            Node n = node(opts().path("/path/to/project")
              .pkg(obj("devDependencies", obj("foo", "1.x"), "dependencies", obj("foo", ">=1"))));
            assertThat(n.getEdgesOut().get("foo").getType()).isEqualTo(Edge.Type.DEV);
            assertThat(n.getEdgesOut().get("foo").getSpec()).isEqualTo("1.x");
        }

        @Test
        void ifInAllThreeUseDev() {
            Node n = node(opts().path("/path/to/project").pkg(obj("devDependencies", obj("foo", "1.x"),
              "dependencies", obj("foo", "2"), "peerDependencies", obj("foo", ">=1"))));
            assertThat(n.getEdgesOut().get("foo").getType()).isEqualTo(Edge.Type.DEV);
            assertThat(n.getEdgesOut().get("foo").getSpec()).isEqualTo("1.x");
        }

        @Test
        void preferWorkspaceVersion() {
            Node root = node(opts().pkg(obj("name", "workspaces_root")).path("/srv/user/projects/workspaces_root")
              .realpath("/srv/user/projects/workspaces_root"));
            root.setWorkspaces(workspaces("foo", "/srv/user/projects/workspaces_root/foo"));
            root.setPackage(obj("name", "bar", "version", "1.2.3", "dependencies", obj("foo", "2.3.4")));
            assertThat(root.getEdgesOut().get("foo").getType()).as("keeps workspace edge")
              .isEqualTo(Edge.Type.WORKSPACE);
        }
    }

    @Test
    void canDedupe() {
        Node root = node(opts().path("/path/to/root")
          .pkg(obj("name", "root", "version", "1.2.3", "dependencies",
          obj("bundler", "", "wrapper", "", "a", "1", "b", "2", "c", "3")))
          .child(opts()
          .pkg(obj("name", "bundler", "version", "1.2.3", "bundleDependencies", arr("a"), "dependencies",
          obj("a", "1")))
            .child(opts().pkg(obj("name", "a", "version", "1.2.3")).integrity("a123").build()).build())
          .child(opts()
          .pkg(obj("name", "wrapper", "version", "1.2.3", "dependencies", obj("a", "1"), "_hasShrinkwrap", true))
            .hasShrinkwrap(true)
            .child(opts().pkg(obj("name", "a", "version", "1.2.3")).integrity("a123").build()).build())
          .child(opts().pkg(obj("name", "a", "version", "1.2.3")).integrity("a123").build())
          .child(opts().pkg(obj("name", "c", "version", "3.4.5", "dependencies", obj("a", "1")))
            .child(opts().pkg(obj("name", "a", "version", "1.2.3")).integrity("a123").build()).build())
          .child(opts().pkg(obj("name", "b", "version", "2.3.4", "dependencies", obj("a", "1", "c", "2", "d", "3")))
            .child(opts().pkg(obj("name", "a", "version", "1.9.99")).build())
            .child(opts().pkg(obj("name", "e", "version", "2.3.4")).integrity("y").build())
            .child(opts().pkg(obj("name", "c", "version", "2.3.4", "dependencies", obj("a", "2")))
              .child(opts().pkg(obj("name", "a", "version", "2.3.99", "dependencies", obj("e", "2"))).integrity("a2399")
                .child(opts().pkg(obj("name", "e", "version", "2.0.1")).integrity("x").build()).build()).build())
            .child(opts().pkg(obj("name", "d", "version", "3.4.5", "dependencies", obj("a", "3")))
              .child(opts().pkg(obj("name", "a", "version", "3.4.5")).build()).build()).build())
          .child(opts().pkg(obj("name", "extraneous", "version", "1.2.3")).build()));

        assertThat(root.getInventory().filter(Node::canDedupe).stream().map(Node::getLocation)).as("preferDedupe=false")
          .containsExactly("node_modules/c/node_modules/a", "node_modules/b/node_modules/e",
            "node_modules/b/node_modules/c/node_modules/a/node_modules/e");
        assertThat(root.getInventory().filter(n -> n.canDedupe(true)).stream().map(Node::getLocation))
          .as("preferDedupe=true")
          .containsExactly("node_modules/c/node_modules/a", "node_modules/b/node_modules/a",
          "node_modules/b/node_modules/e",
            "node_modules/b/node_modules/c/node_modules/a/node_modules/e");

        Node top = node(opts().fsParent(root).path(root.getPath() + "/packages/top")
          .pkg(obj("name", "top", "version", "1.2.3", "dependencies", obj("a", "1")))
          .child(opts().pkg(obj("name", "a", "version", "1.0.0")).build()));
        assertThat(child(top, "a").canDedupe()).isTrue();

        assertThat(top.isDescendantOf(root)).isTrue();
        assertThat(top.isFsTop()).isFalse();
        assertThat(top.getFsTop()).isSameAs(root);
        assertThat(child(top, "a").isFsTop()).isTrue();
    }

    @Test
    void canDedupeReadsParentsBeforeNpm7_14_0() {
        NpmRelease release = NpmRelease.of("7.13.0");
        Node root = node(opts().release(release).path("/path/to/root")
          .pkg(obj("name", "root", "version", "1.2.3", "dependencies", obj("a", "1")))
          .child(opts().pkg(obj("name", "a", "version", "1.2.3")).build()));
        Node top = node(opts().release(release).fsParent(root).path(root.getPath() + "/packages/top")
          .pkg(obj("name", "top", "version", "1.2.3", "dependencies", obj("a", "1")))
          .child(opts().pkg(obj("name", "a", "version", "1.0.0")).build()));
        assertThat(child(top, "a").canDedupe()).as("a top in another's folder has no parent").isFalse();
    }

    @Test
    void canDedupeReturnsTrueWhenExplicitRequestIsTrueRegardlessOfOtherConditions() {
        Node root = node(opts().pkg(obj("name", "root", "version", "1.0.0")).path("/root").realpath("/root"));
        Node duplicate = node(opts().pkg(obj("name", "dup", "version", "1.0.0")).parent(root));
        Node node = node(opts().pkg(obj("name", "dup", "version", "2.0.0")).parent(duplicate));
        new Edge(duplicate, Edge.Type.PROD, "dup", "*");
        assertThat(node.getEdgesIn()).isNotEmpty();
        assertThat(node.canDedupe(false, false)).as("without explicit request, dedupe is not allowed").isFalse();
        assertThat(node.canDedupe(false, true)).as("explicit request forces dedupe to return true").isTrue();
    }

    @Test
    void packageNameGetter() {
        Node node = node(opts().pkg(obj("name", "foo")).path("/path/to/bar"));
        assertThat(node.getName()).isEqualTo("bar");
        assertThat(node.getPackageName()).isEqualTo("foo");
    }

    @Test
    void nodeAtRootShouldNotHaveFsParent() {
        Node root = node(opts().path("/some/path"));
        Link link = link(opts().parent(root).name("link").realpath("/"));
        assertThat(link.getTarget().getFsParent()).isNull();
    }

    @Test
    void nodeAncestryIterator() {
        Node root = node(opts().path("/some/path").pkg(obj("name", "root", "version", "1.2.3"))
          .child(opts().pkg(obj("name", "a", "version", "1.2.3"))
            .child(opts().pkg(obj("name", "b", "version", "1.2.3"))
              .child(opts().pkg(obj("name", "c", "version", "1.2.3")).build()).build()).build()));
        Node c = child(child(child(root, "a"), "b"), "c");
        Node d = node(opts().root(root).path(c.getPath() + "/d").pkg(obj("name", "d", "version", "1.2.3"))
          .child(opts().pkg(obj("name", "e", "version", "1.2.3"))
            .child(opts().pkg(obj("name", "f", "version", "1.2.3"))
              .child(opts().pkg(obj("name", "g", "version", "1.2.3")).build()).build()).build()));
        Node g = child(child(child(d, "e"), "f"), "g");
        assertThat(StreamSupport.stream(g.ancestry().spliterator(), false).map(Node::getPackageName))
          .containsExactly("g", "f", "e", "d", "c", "b", "a", "root");
    }

    @Test
    void canReplaceWithIsAlwaysFalseWhenPackageNameDoesNotMatch() {
        Node root = node(opts().path("/some/path")
          .pkg(obj("dependencies", obj("foo", "1.2.3", "alias", "npm:bar@1.2.3")))
          .child(opts().pkg(obj("name", "foo", "version", "1.2.3")).build())
          .child(opts().name("alias").pkg(obj("name", "bar", "version", "1.2.3")).build()));
        Node rep = node(opts().path("/some/path/node_modules/foo").name("foo")
          .pkg(obj("name", "bar", "version", "1.2.3")));
        assertThat(rep.canReplace(child(root, "foo"))).as("cannot replace actual node with an alias").isFalse();
        Node alias = node(opts().path("/some/path/node_modules/alias").name("alias")
          .pkg(obj("name", "bar", "version", "1.2.3")));
        assertThat(alias.canReplace(child(root, "alias"))).as("can replace alias with a different alias to same thing")
          .isTrue();
    }

    @Test
    void canReplaceWithComparesNoPackageNamesBeforeNpm7_20_3() {
        NpmRelease release = NpmRelease.of("7.20.2");
        Node root = node(opts().release(release).path("/some/path")
          .pkg(obj("dependencies", obj("foo", "1.2.3")))
          .child(opts().pkg(obj("name", "foo", "version", "1.2.3")).build()));
        Node rep = node(opts().release(release).path("/some/path/node_modules/foo").name("foo")
          .pkg(obj("name", "bar", "version", "1.2.3")));
        assertThat(rep.canReplace(child(root, "foo"))).as("an alias can replace the actual node").isTrue();
    }

    // node.js canReplaceWith from npm 7.0.11 leaves out the edges into a node from its own dependencies, which go with
    // it; before, b's edge back to a holds a in place
    @Test
    void canReplaceWithCountsItsOwnDependentsBeforeNpm7_0_11() {
        for (String version : new String[]{"7.0.10", "7.0.11"}) {
            NpmRelease release = NpmRelease.of(version);
            Node root = node(opts().release(release).path("/some/path").pkg(obj("dependencies", obj("a", "1 || 2")))
              .child(opts().pkg(obj("name", "a", "version", "1.0.0", "dependencies", obj("b", "1"))).build())
              .child(opts().pkg(obj("name", "b", "version", "1.0.0", "dependencies", obj("a", "1"))).build()));
            Node rep = node(opts().release(release).pkg(obj("name", "a", "version", "2.0.0")));
            assertThat(rep.canReplace(child(root, "a"))).as("npm %s", version).isEqualTo("7.0.11".equals(version));
        }
    }

    @Test
    void canReplaceWhileIgnoringCertainPeerDeps() {
        Node tree = node(opts().path("/some/path").pkg(obj("dependencies", obj("a", "1||2", "b", "")))
          .child(opts().pkg(obj("name", "a", "version", "1.0.0", "peerDependencies", obj("b", "1"))).build())
          .child(opts().pkg(obj("name", "b", "version", "1.0.0", "peerDependencies", obj("a", "1"))).build()));
        Node current = child(tree, "a");
        Node rep = node(opts().path(current.getPath()).pkg(obj("name", "a", "version", "2.0.0")));
        assertThat(rep.canReplace(current)).as("cannot replace because peer dep").isFalse();
        assertThat(rep.canReplace(current, Collections.singletonList("b"))).as("can replace if ignoring the `b` peer")
          .isTrue();
    }

    @Test
    void childrenAreUnicodeNormalizingAndCaseInsensitive() {
        String cafe1 = "café";
        String cafe2 = "café";
        Node tree = node(opts().path("/some/path")
          .child(opts().pkg(obj("name", "A", "version", "1.0.0")).build())
          .child(opts().pkg(obj("name", "a", "version", "2.0.0")).build())
          .child(opts().pkg(obj("name", cafe1, "version", "1.0.0")).build())
          .child(opts().pkg(obj("name", cafe2, "version", "2.0.0")).build()));
        assertThat(tree.getChildren().size()).isEqualTo(2);
        assertThat(tree.getChildren().get("A")).isSameAs(tree.getChildren().get("a"));
        Node a = tree.getChildren().get("a");
        assertThat(a.getVersion()).isEqualTo("2.0.0");
        assertThat(a.getPackageName()).isEqualTo("a");
        assertThat(a.getName()).isEqualTo("a");
        assertThat(tree.getChildren().get(cafe1)).isSameAs(tree.getChildren().get(cafe2));
        Node cafe = tree.getChildren().get(cafe1);
        assertThat(cafe.getVersion()).isEqualTo("2.0.0");
        assertThat(cafe.getPackageName()).isEqualTo(cafe2);
        assertThat(cafe.getName()).isEqualTo(cafe2);
    }

    @Nested
    class Overrides {

        @Test
        void skipsLoadingWhenNoOverridesAreProvided() {
            assertThat(node(opts().loadOverrides(true).path("/some/path").pkg(obj("name", "foo"))).getOverrides())
              .isNull();
        }

        @Test
        void skipsLoadingWhenOverridesAreEmpty() {
            assertThat(node(opts().loadOverrides(true).path("/some/path").pkg(obj("name", "foo", "overrides", obj())))
              .getOverrides()).isNull();
        }

        @Test
        void loadsOverrides() {
            assertThat(node(opts().loadOverrides(true).path("/some/path")
              .pkg(obj("name", "foo", "dependencies", obj("bar", "^1"), "overrides", obj("bar", obj(".", "2.0.0")))))
              .getOverrides()).isNotNull();
        }

        private Node overriddenTree(String bazVersion) {
            return node(opts().loadOverrides(true).path("/some/path")
              .pkg(obj("name", "foo", "dependencies", obj("bar", "^1"), "overrides", obj("baz", "1.0.0")))
              .child(opts().name("bar").pkg(obj("dependencies", obj("baz", "2.0.0")))
                .child(opts().name("baz").pkg(obj("version", bazVersion, "dependencies", obj("buzz", "1.0.0")))
                  .child(opts().name("buzz").pkg(obj()).build()).build()).build()));
        }

        @Test
        void nodeOverriddenIsTrueWhenAnOverrideAppliesToASpecificNode() {
            Node tree = overriddenTree("1.0.0");
            Node bar = tree.getEdgesOut().get("bar").getTo();
            assertThat(bar.isOverridden()).as("bar was not overridden").isFalse();
            Node baz = bar.getEdgesOut().get("baz").getTo();
            assertThat(baz.isOverridden()).as("baz was overridden").isTrue();
            Node buzz = baz.getEdgesOut().get("buzz").getTo();
            assertThat(buzz.isOverridden()).as("buzz was not overridden").isFalse();
        }

        @Test
        void nodeOverriddenIsFalseWhenAnOverrideDoesNotMatchTheNodeVersion() {
            Node tree = overriddenTree("3.0.0");
            Node bar = tree.getEdgesOut().get("bar").getTo();
            assertThat(bar.isOverridden()).isFalse();
            Node baz = bar.getEdgesOut().get("baz").getTo();
            assertThat(baz.isOverridden()).as("baz was not overridden because version mismatch").isFalse();
            Node buzz = baz.getEdgesOut().get("buzz").getTo();
            assertThat(buzz.isOverridden()).isFalse();
        }

        @Test
        void nodeOverriddenReturnsFalseWhenAnIncomingEdgeOverrideEqualsItsSourceOverride() {
            OverrideSet baseOverride = new OverrideSet(obj("foo", "bar"));
            baseOverride.setName("test-package");
            baseOverride.setValue("1.0.0");
            Node from = node(opts().path("/some/path").overrides(baseOverride));
            Node node = node(opts().pkg(obj("name", "test-package", "version", "1.0.0")).parent(from)
              .overrides(baseOverride));
            OverrideSet equalOverride = new OverrideSet(obj("foo", "bar"));
            equalOverride.setName("test-package");
            equalOverride.setValue("1.0.0");
            new Edge(from, Edge.Type.PROD, "test-package", "*").setOverrides(equalOverride);
            assertThat(node.getEdgesIn()).hasSize(1);
            assertThat(node.isOverridden())
              .as("node.overridden returns false when edge.override equals edge.from.override").isFalse();
        }

        @Test
        void assertRootOverridesThrowsWhenADependencyAndOverrideConflict() {
            Node conflictingTree = node(opts().loadOverrides(true).path("/some/path")
              .pkg(obj("name", "foo", "dependencies", obj("bar", "1.x"), "overrides", obj("bar", "2.x")))
              .child(opts().pkg(obj("name", "bar", "version", "1.x")).build()));
            assertThatThrownBy(conflictingTree::assertRootOverrides).isInstanceOf(Node.OverrideConflict.class)
              .satisfies(e -> assertThat(((Node.OverrideConflict) e).getCode()).isEqualTo("EOVERRIDE"));
            assertThatCode(() -> child(conflictingTree, "bar").assertRootOverrides()).as("child does not throw")
              .doesNotThrowAnyException();

            Node safeTree = node(opts().loadOverrides(true).path("/some/path")
              .pkg(obj("name", "foo", "dependencies", obj("bar", "1.x"), "overrides", obj("baz", "2.x")))
              .child(opts().pkg(obj("name", "bar", "version", "1.x")).build()));
            assertThatCode(safeTree::assertRootOverrides).as("non conflicting tree does not throw")
              .doesNotThrowAnyException();
        }

        @Test
        void overridesPropagateToChildrenAndEdges() {
            Node tree = node(opts().loadOverrides(true).path("/some/path")
              .pkg(obj("name", "foo", "dependencies", obj("bar", "^1"), "overrides", obj("bar", obj(".", "2.0.0"))))
              .child(opts().pkg(obj("name", "bar", "version", "1.0.0")).build()));
            assertThat(tree.getOverrides()).as("overrides is defined on root").isNotNull();
            assertThat(tree.getEdgesOut().get("bar").getOverrides()).as("overrides is defined on edgeOut").isNotNull();
            assertThat(child(tree, "bar").getOverrides()).as("overrides is defined on child").isNotNull();
        }

        @Test
        void settingRootReplacesOverrides() {
            Node root = node(opts().path("/some/path").loadOverrides(true)
              .pkg(obj("name", "root", "version", "1.0.0", "dependencies", obj("foo", "^1.0.0"), "overrides",
              obj("bar", "^2.0.0"))));
            Node foo = node(opts().path("/some/path/node_modules/foo")
              .pkg(obj("name", "foo", "version", "1.0.0", "dependencies", obj("bar", "^1.0.0"))));
            Node bar = node(opts().path("/some/path/node_modules/bar").pkg(obj("name", "bar", "version", "2.0.0")));

            assertThat(root.getOverrides()).isNotNull();
            assertThat(foo.getOverrides()).isNull();
            assertThat(bar.getOverrides()).isNull();
            assertThat(root.getEdgesOut().get("foo").isValid()).isFalse();
            assertThat(foo.getEdgesOut().get("bar").isValid()).isFalse();

            bar.setRoot(root);
            assertThat(bar.getOverrides()).as("bar still does not have overrides until connected by a dependency edge")
              .isNull();
            assertThat(foo.getEdgesOut().get("bar").isValid()).isFalse();

            foo.setRoot(root);
            assertThat(foo.getOverrides()).as("foo now has overrides").isNotNull();
            assertThat(root.getEdgesOut().get("foo").isValid()).isTrue();
            assertThat(bar.getOverrides()).as("bar now has overrides after foo is attached").isNotNull();
            assertThat(foo.getEdgesOut().get("bar").isValid()).isTrue();
        }

        @Test
        void canReplaceWithRequiresTheSameOverrides() {
            Node original = node(opts().loadOverrides(true).path("/some/path")
              .pkg(obj("name", "foo", "dependencies", obj("bar", "^1"), "overrides", obj("bar", obj(".", "2.0.0"))))
              .child(opts().pkg(obj("name", "bar", "version", "1.0.0")).build()));
            Node equivalentReplacement = node(opts().loadOverrides(true).path("/some/path")
              .pkg(obj("name", "foo", "dependencies", obj("bar", "^1"), "overrides", obj("bar", obj(".", "2.0.0"))))
              .child(opts().pkg(obj("name", "bar", "version", "1.0.0")).build()));
            assertThat(original.canReplaceWith(equivalentReplacement)).as("different overrides passes").isTrue();

            Node goodReplacement = node(opts().path("/some/path")
              .pkg(obj("name", "foo", "dependencies", obj("bar", "^1"), "overrides", obj("bar", obj(".", "2.0.0"))))
              .child(opts().pkg(obj("name", "bar", "version", "1.0.0")).build()));
            assertThat(original.canReplaceWith(goodReplacement)).as("no overrides fails").isFalse();
            goodReplacement.setOverrides(original.getOverrides());
            assertThat(original.canReplaceWith(goodReplacement)).as("same overrides passes").isTrue();
        }
    }

    @Test
    void nodeWithNoEdgesInIsNotARegistryDep() {
        assertThat(node(opts().path("/foo")).isRegistryDependency()).isFalse();
    }

    @Test
    void nodeWithNonRegistryEdgeInIsNotARegistryDep() {
        Node root = node(opts().path("/some/path").pkg(obj("dependencies", obj("registry", "", "tar", ""))));
        Node node = node(opts().pkg(obj("name", "node", "version", "1.0.0")).parent(root));
        node(opts().pkg(obj("name", "registry", "dependencies", obj("node", "^1.0.0"))).parent(root));
        node(opts().pkg(obj("name", "tar", "dependencies", obj("node", "file:node"))).parent(root));
        assertThat(node.isRegistryDependency()).isFalse();
    }

    @Test
    void nodeWithOnlyRegistryEdgesInARegistryDep() {
        Node root = node(opts().path("/some/path").pkg(obj("dependencies", obj("registry", "", "tar", ""))));
        Node node = node(opts().pkg(obj("name", "node", "version", "1.0.0")).parent(root));
        node(opts().pkg(obj("name", "registry", "dependencies", obj("node", "^1.0.0"))).parent(root));
        assertThat(node.isRegistryDependency()).isTrue();
    }

    @Test
    void canReplaceWithReturnsFalseWhenOverridesDiffer() {
        Node node1 = node(opts().pkg(obj("name", "foo", "dependencies", obj("bar", "^1"))).path("/some/path/foo")
          .realpath("/some/path/foo").overrides(new OverrideSet(obj("foo", "1.0.0"))));
        Node node2 = node(opts().pkg(obj("name", "foo", "dependencies", obj("bar", "^1"))).path("/some/path/foo")
          .realpath("/some/path/foo").overrides(new OverrideSet(obj("foo", "2.0.0"))));
        assertThat(node1.getEdgesOut().size()).isPositive();
        assertThat(node1.canReplaceWith(node2, new HashSet<>())).as("cannot replace when overrides differ").isFalse();
    }

    // upstream adds plain objects to edgesIn; here real edges come from siblings carrying those override sets
    @Test
    void updateOverridesEdgeInRemovedUsesFindSpecificOverrideSetForMultipleEdgesIn() {
        OverrideSet commonOverrides = new OverrideSet(obj("foo", "1.0.0"));
        OverrideSet specificOverrides = new OverrideSet(obj("foo", "1.0.0", "bar", "2.0.0"));
        Node root = node(opts().path("/some/path"));
        Node node = node(opts().pkg(obj("name", "nodeA")).parent(root).overrides(commonOverrides));
        Node common = node(opts().pkg(obj("name", "common")).parent(root).overrides(commonOverrides));
        Node specific = node(opts().pkg(obj("name", "specific")).parent(root).overrides(specificOverrides));
        new Edge(common, Edge.Type.PROD, "nodeA", "*");
        new Edge(specific, Edge.Type.PROD, "nodeA", "*");
        assertThat(node.getEdgesIn()).extracting(Edge::getOverrides)
          .containsExactly(commonOverrides, specificOverrides);
        assertThat(node.getOverrides()).isSameAs(commonOverrides);

        boolean result = node.updateOverridesEdgeInRemoved(commonOverrides);
        assertThat(result).as("updateOverridesEdgeInRemoved returns true when newOverrideSet differs").isTrue();
        assertThat(commonOverrides.isEqual(node.getOverrides())).as("node.overrides is updated").isFalse();
    }

    @Test
    void updateOverridesEdgeInAddedConflictsOnConflictingOverrideSet() {
        OverrideSet overrides8 = new OverrideSet(obj("bat", "1.2.0"));
        OverrideSet overrides9 = new OverrideSet(obj("bat@3.0.0", "1.2.0"));
        Node node = node(opts().pkg(obj("name", "conflict-node")).path("/some/path/conflict-node")
          .realpath("/some/path/conflict-node").overrides(overrides8));
        assertThat(node.updateOverridesEdgeInAdded(overrides9)).as("returns undefined on conflict").isNull();
    }

    @Test
    void updateOverridesEdgeInRemovedCallsRecalculateOutEdgesOverridesWhenNewOverrideSetExists() {
        OverrideSet originalOverrides = new OverrideSet(obj("foo", "1.0.0"));
        OverrideSet specificOverrides = new OverrideSet(obj("foo", "1.0.0", "bar", "2.0.0"));
        Node root = node(opts().path("/some/path"));
        boolean[] recalcCalled = {false};
        Node node = new Node(opts().pkg(obj("name", "test-node")).parent(root).overrides(originalOverrides).build()) {
            @Override
            void recalculateOutEdgesOverrides() {
                recalcCalled[0] = true;
            }
        };
        Node from = node(opts().pkg(obj("name", "from")).parent(root).overrides(specificOverrides));
        new Edge(from, Edge.Type.PROD, "test-node", "*");
        assertThat(node.getEdgesIn()).extracting(Edge::getOverrides).containsExactly(specificOverrides);

        boolean result = node.updateOverridesEdgeInRemoved(originalOverrides);
        assertThat(result).as("returns true when override set changes").isTrue();
        assertThat(recalcCalled[0]).as("recalculateOutEdgesOverrides was called").isTrue();
        assertThat(specificOverrides.isEqual(node.getOverrides()))
          .as("node.overrides updated to the specific override set").isTrue();
    }

    private Node mockDepTree(String mockDepVersion, Map<String, Object> mockDepDependencies, Node.Options grandchild) {
        return node(opts().loadOverrides(true).path("/root")
          .pkg(obj("name", "root", "version", "1.0.0", "dependencies", obj("mockDep", "1.x"), "overrides",
          obj("mockDep", "2.x")))
          .child(opts().name("mockDep").pkg(obj("version", mockDepVersion, "dependencies", mockDepDependencies))
            .child(grandchild).build()));
    }

    @Test
    void shouldPropagateTheNewOverrideSetToTheTargetNode() {
        Node tree = mockDepTree("2.0.0", obj("subDep", "1.0.0"), opts().name("subDep").pkg(obj()).build());
        Edge edge = tree.getEdgesOut().get("mockDep");
        edge.setOverrides(new OverrideSet(obj("mockDep", "1.x")));
        edge.reload();
        assertThat(edge.getOverrides().getValue()).as("Edge override propagates the correct override value from the " +
                                                      "parent")
          .isEqualTo("2.x");
    }

    // upstream replaces edge.satisfiedBy with () => true; the package versions here satisfy the edge outright
    @Nested
    class OverrideConflictDetectionWithSemanticComparison {

        @Test
        void nonConflictingDifferentOverrideSetsShouldBeValid() {
            Node tree = mockDepTree("2.0.0", obj("subDep", "1.0.0"), opts().name("subDep").pkg(obj()).build());
            Edge edge = tree.getEdgesOut().get("mockDep");
            edge.setOverrides(new OverrideSet(obj("@vaadin/react-components", "24.9.2")));
            child(tree, "mockDep").setOverrides(new OverrideSet(obj("@vaadin/react-components-pro", "24.9.2")));
            edge.reload(true);
            assertThat(edge.getError()).as("Edge should be valid with non-conflicting override contexts").isNull();
            assertThat(edge.isValid()).isTrue();
        }

        @Test
        void conflictingOverrideSetsShouldBeDetectedAsInvalid() {
            OverrideSet overridesV1 = new OverrideSet(obj("lodash", "1.x"));
            OverrideSet overridesV4 = new OverrideSet(obj("lodash", "4.x"));
            Node tree = mockDepTree("1.0.0", obj("lodash", "1.0.0"), opts().name("lodash").pkg(obj()).build());
            Edge edge = tree.getEdgesOut().get("mockDep");
            Node mockDep = child(tree, "mockDep");
            edge.setOverrides(overridesV1);
            mockDep.setOverrides(overridesV4);
            edge.reload(true);
            edge.setOverrides(overridesV1);
            mockDep.setOverrides(overridesV4);
            assertThat(edge.getError()).as("Edge should be invalid with conflicting override versions")
              .isEqualTo("INVALID");
            assertThat(edge.isValid()).isFalse();
        }

        @Test
        void referenceOverridesShouldNotCauseFalsePositives() {
            Node tree = mockDepTree("2.0.0", obj("lodash", "4.0.0"), opts().name("lodash").pkg(obj()).build());
            Edge edge = tree.getEdgesOut().get("mockDep");
            edge.setOverrides(new OverrideSet(obj("lodash", "$some-reference")));
            child(tree, "mockDep").setOverrides(new OverrideSet(obj("lodash", "$another-reference")));
            edge.reload(true);
            assertThat(edge.getError()).as("Edge should be valid with reference overrides").isNull();
            assertThat(edge.isValid()).isTrue();
        }
    }

    /**
     * Arborist 1's tree setters (node.js, npm 7.1.0), which arborist 2.0.0 (npm 7.1.1) replaced ({@link LegacyNode}).
     */
    @Nested
    class ArboristOneSetters {

        // from npm 7.1.1 the root setter places a node by its path and lists the root in its own inventory; before, it
        // only roots the node, which the parent setter places
        @Test
        void rootSetterPlacesNodeFrom711() {
            for (String version : new String[]{"7.1.0", "7.1.1"}) {
                NpmRelease release = NpmRelease.of(version);
                boolean places = "7.1.1".equals(version);
                Node root = node(opts().release(release).path("/project")
                  .pkg(obj("name", "root", "dependencies", obj("a", "1"))));
                Node a = node(opts().release(release).path("/project/node_modules/a").root(root)
                  .pkg(obj("name", "a", "version", "1.0.0")));
                assertThat(a.getParent()).as("npm %s", version).isSameAs(places ? root : null);
                assertThat(root.getInventory().get("")).as("npm %s", version).isSameAs(places ? root : null);
                assertThat(root.getInventory().get("node_modules/a")).isSameAs(a);
                assertThat(root.getEdgesOut().get("a").getTo()).as("npm %s", version).isSameAs(places ? a : null);

                a.setParent(root);
                assertThat(root.getChildren().get("a")).isSameAs(a);
                assertThat(root.getEdgesOut().get("a").getTo()).isSameAs(a);
                assertThat(root.getLocation()).isEqualTo("");
                assertThat(a.getLocation()).isEqualTo("node_modules/a");
            }
        }

        // from npm 7.0.10 the fsParent setter ignores a node at the node's own realpath
        @Test
        void fsParentAtOwnRealpathIsIgnoredFrom7010() {
            for (String version : new String[]{"7.0.9", "7.0.10"}) {
                NpmRelease release = NpmRelease.of(version);
                Node root = node(opts().release(release).path("/project").pkg(obj("name", "root")));
                Node other = node(opts().release(release).path("/project/packages/a").root(root)
                  .pkg(obj("name", "other")));
                Node a = node(opts().release(release).path("/project/packages/a").pkg(obj("name", "a")));
                a.setFsParent(other);
                assertThat(a.getFsParent()).as("npm %s", version).isSameAs("7.0.9".equals(version) ? other : null);
            }
        }

        // from npm 7.0.6 replaceWith leaves the children of a node a link replaces where they are; before, they go
        // under the link, which puts them under its target
        @Test
        void childrenOfNodeALinkReplacesStayFrom706() {
            for (String version : new String[]{"7.0.5", "7.0.6"}) {
                NpmRelease release = NpmRelease.of(version);
                Node root = node(opts().release(release).path("/project").pkg(obj("name", "root")));
                Node a = node(opts().release(release).parent(root).pkg(obj("name", "a", "version", "1.0.0")));
                Node c = node(opts().release(release).parent(a).pkg(obj("name", "c", "version", "1.0.0")));
                Node target = node(opts().release(release).path("/project/packages/a").fsParent(root)
                  .pkg(obj("name", "a", "version", "2.0.0")));
                Link link = link(opts().release(release).path("/project/node_modules/a")
                  .realpath("/project/packages/a").target(target));
                link.replace(a);
                assertThat(root.getChildren().get("a")).isSameAs(link);
                assertThat(c.getParent()).as("npm %s", version).isSameAs("7.0.5".equals(version) ? target : a);
            }
        }
    }

    @Nested
    class ShouldOmit {

        private Node dummy() {
            return node(opts().pkg(obj("name", "test")).path("/test").dummy(true));
        }

        @Test
        void devDependencyWithDevOmit() {
            Node node = dummy();
            node.setDev(true);
            assertThat(node.shouldOmit(set("dev"))).isTrue();
            assertThat(node.shouldOmit(set("optional"))).isFalse();
        }

        @Test
        void optionalDependencyWithOptionalOmit() {
            Node node = dummy();
            node.setOptional(true);
            assertThat(node.shouldOmit(set("optional"))).isTrue();
            assertThat(node.shouldOmit(set("dev"))).isFalse();
        }

        @Test
        void peerDependencyWithPeerOmit() {
            Node node = dummy();
            node.setPeer(true);
            assertThat(node.shouldOmit(set("peer"))).isTrue();
            assertThat(node.shouldOmit(set("dev"))).isFalse();
        }

        @Test
        void devOptionalDependency() {
            Node node = dummy();
            node.setDevOptional(true);
            assertThat(node.shouldOmit(set("dev", "optional"))).isTrue();
            assertThat(node.shouldOmit(set("dev"))).isFalse();
            assertThat(node.shouldOmit(set("optional"))).isFalse();
        }

        @Test
        void regularDependency() {
            assertThat(dummy().shouldOmit(set("dev", "optional", "peer"))).isFalse();
        }

        @Test
        void emptyOmitSet() {
            Node node = dummy();
            node.setDev(true);
            assertThat(node.shouldOmit(set())).isFalse();
        }

        private Set<String> set(String... values) {
            return new HashSet<>(Arrays.asList(values));
        }
    }
}
