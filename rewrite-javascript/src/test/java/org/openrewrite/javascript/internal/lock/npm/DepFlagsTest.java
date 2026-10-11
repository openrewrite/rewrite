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
import org.openrewrite.javascript.internal.lock.PosixPath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.assertTree;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.link;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.node;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.obj;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.opts;

/**
 * Port of @npmcli/arborist test/calc-dep-flags.js; its tree snapshots are in the tree-dumps fixture.
 */
class DepFlagsTest {

    private static void assertFlags(Node node, boolean extraneous, boolean dev, boolean optional, boolean devOptional,
                                    boolean peer) {
        assertThat(node.isExtraneous()).as(node.getName() + " extraneous").isEqualTo(extraneous);
        assertThat(node.isDev()).as(node.getName() + " dev").isEqualTo(dev);
        assertThat(node.isOptional()).as(node.getName() + " optional").isEqualTo(optional);
        assertThat(node.isDevOptional()).as(node.getName() + " devOptional").isEqualTo(devOptional);
        assertThat(node.isPeer()).as(node.getName() + " peer").isEqualTo(peer);
    }

    @Test
    void flagStuff() {
        Node root = node(opts().path("/x").realpath("/x").pkg(obj(
          "dependencies", obj("prod", ""),
          "devDependencies", obj("dev", ""),
          "optionalDependencies", obj("optional", ""),
          "peerDependencies", obj("peer", "", "peeroptional", ""),
          "peerDependenciesMeta", obj("peeroptional", obj("optional", true)))));
        Node optional = node(opts().pkg(obj("name", "optional", "version", "1.2.3", "dependencies", obj("devoptional",
          "", "missing", ""))).parent(root));
        Node devoptional = node(opts().pkg(obj("name", "devoptional", "version", "1.2.3")).parent(root));
        Node extraneous = node(opts().pkg(obj("name", "extraneous")).parent(root));
        Node peer = node(opts().pkg(obj("name", "peer", "version", "1.2.3", "dependencies", obj("peerdep", "")))
          .parent(root));
        Node peerdep = node(opts().pkg(obj("name", "peerdep", "version", "1.2.3")).parent(root));
        Node prod = node(opts().pkg(obj("name", "prod", "version", "1.2.3", "dependencies", obj("proddep", ""),
          "peerDependencies", obj("metapeer", ""))).parent(root));
        Node metapeer = node(opts().pkg(obj("name", "metapeer", "version", "1.2.3", "dependencies", obj("metapeerdep",
          ""))).parent(root));
        Node metapeerdep = node(opts().pkg(obj("name", "metapeerdep", "version", "1.2.3")).parent(root));
        Node proddep = node(opts().pkg(obj("name", "proddep", "version", "1.2.3", "dependencies", obj("proddep", "")))
          .parent(root));
        Node dev = node(opts().pkg(obj("name", "dev", "version", "1.2.3", "dependencies", obj("devdep", "")))
          .parent(root));
        Node devdep = node(opts().pkg(obj("name", "devdep", "version", "1.2.3",
          "dependencies", obj("proddep", "", "linky", "", "devoptional", ""),
          "optionalDependencies", obj("devandoptional", ""))).parent(root));
        Node devandoptional = node(opts().pkg(obj("name", "devandoptional", "version", "1.2.3")).parent(root));
        Link linky = link(opts().pkg(obj("name", "linky", "version", "1.2.3", "dependencies", obj("linklink", "")))
          .realpath("/x/y/z").parent(devdep));
        Link linkylinky = link(opts().pkg(obj("name", "linklink", "version", "1.2.3")).realpath("/l/i/n/k/link")
          .parent(linky.getTarget()));
        Node peeroptional = node(opts().pkg(obj("name", "peeroptional", "version", "1.2.3", "dependencies",
                                                obj("optional", ""))).parent(root));

        DepFlags.calcDepFlags(root);

        assertFlags(optional, false, false, true, false, false);
        assertFlags(devoptional, false, false, false, true, false);
        assertThat(extraneous.isExtraneous()).isTrue();
        assertFlags(peer, false, false, false, false, true);
        assertFlags(peerdep, false, false, false, false, true);
        assertFlags(prod, false, false, false, false, false);
        assertFlags(metapeer, false, false, false, false, true);
        assertFlags(metapeerdep, false, false, false, false, true);
        assertFlags(proddep, false, false, false, false, false);
        assertFlags(dev, false, true, false, false, false);
        assertFlags(devdep, false, true, false, false, false);
        assertFlags(devandoptional, false, true, true, false, false);
        assertFlags(linky, false, true, false, false, false);
        assertFlags(linkylinky, false, true, false, false, false);
        assertFlags(peeroptional, true, false, true, false, true);

        assertTree("flag stuff", root);
    }

    @Test
    void noReset() {
        Node root = node(opts().path("/some/path").realpath("/some/path").pkg(obj("dependencies", obj("foo", ""))));
        Node foo = node(opts().parent(root).pkg(obj("name", "foo", "version", "1.2.3")));
        root.setOptional(false);
        root.setDev(true);
        root.setExtraneous(false);

        DepFlags.calcDepFlags(root, false);
        assertTree("no reset", root);
        assertThat(root.isDev()).as("root.dev").isTrue();
        assertThat(foo.isDev()).as("foo.dev").isTrue();
        assertThat(root.isOptional()).as("root.optional").isFalse();
        assertThat(foo.isOptional()).as("foo.optional").isFalse();
        assertThat(root.isExtraneous()).as("root.extraneous").isFalse();
        assertThat(foo.isExtraneous()).as("foo.extraneous").isFalse();
    }

    @Test
    void setParentsToNotExtraneousWhenVisiting() {
        Node root = node(opts().path("/some/path").realpath("/some/path").pkg(obj("dependencies",
          obj("baz", "file:node_modules/asdf/node_modules/baz", "foo", "file:bar/foo"))));
        Node bar = node(opts().root(root).path(PosixPath.resolve(root.getPath(), "bar")));
        Node foo = node(opts().root(root).path(PosixPath.resolve(bar.getPath(), "foo")).pkg(obj("name", "foo",
          "version",
          "1.2.3")));
        Node asdf = node(opts().parent(root).pkg(obj("name", "asdf", "version", "1.2.3")));
        Node baz = node(opts().parent(asdf).pkg(obj("name", "baz", "version", "1.2.3")));
        Link fooLink = link(opts().name("foo").target(foo).parent(root).realpath(foo.getPath()));
        Link bazLink = link(opts().name("baz").target(baz).parent(root).realpath(baz.getPath()));

        DepFlags.calcDepFlags(root, true);

        assertThat(root.isExtraneous()).as("root is not extraneous").isFalse();
        assertThat(asdf.isExtraneous()).as("asdf is not extraneous").isFalse();
        assertThat(bar.isExtraneous()).as("bar is not extraneous").isFalse();
        assertThat(baz.isExtraneous()).as("baz is not extraneous").isFalse();
        assertThat(foo.isExtraneous()).as("foo is not extraneous").isFalse();
        assertThat(fooLink.isExtraneous()).as("fooLink is not extraneous").isFalse();
        assertThat(bazLink.isExtraneous()).as("bazLink is not extraneous").isFalse();
        assertTree("set parents to not extraneous when visiting", root);
    }

    @Test
    void checkNullTargetInLink() {
        Link root = link(opts().path("/some/path").realpath("/some/path").pkg(obj("dependencies", obj("foo", ""))));
        assertThat(root.getTarget()).isNull();
        assertThatCode(() -> DepFlags.calcDepFlags(root)).doesNotThrowAnyException();
        assertThatCode(() -> DepFlags.calcDepFlags(root, false)).doesNotThrowAnyException();
    }

    @Test
    void resetDepFlagsSetsEveryFlagOfEveryNode() {
        Node root = node(opts().path("/some/path").realpath("/some/path").pkg(obj("dependencies", obj("foo", ""))));
        Node foo = node(opts().parent(root).pkg(obj("name", "foo", "version", "1.2.3")));
        DepFlags.calcDepFlags(root);
        assertFlags(foo, false, false, false, false, false);
        DepFlags.resetDepFlags(root);
        assertFlags(foo, true, true, true, true, true);
        assertFlags(root, true, true, true, true, true);
    }
}
