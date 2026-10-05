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

import com.fasterxml.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.arr;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.fixture;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.json;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.link;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.node;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.obj;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.opts;

/**
 * Port of the cases of @npmcli/arborist test/shrinkwrap.js that concern the metadata a tree keeps
 * (add, get, delete, addEdge, metaFromNode). Expectations upstream keeps in snapshots, or reaches by
 * reading a tree from disk, are arborist's own answers in the shrinkwrap-dumps fixture.
 */
class ShrinkwrapTest {

    private static final JsonNode DUMPS = fixture("shrinkwrap-dumps");

    private static Shrinkwrap lock(String path, Map<String, Object> data) {
        Shrinkwrap meta = new Shrinkwrap(path);
        meta.setData(data);
        return meta;
    }

    @Test
    void throwsWhenAttemptingToAccessDataBeforeLoading() {
        Shrinkwrap unloaded = new Shrinkwrap("/some/path");
        Node node = node(opts().path("/some/path"));
        assertThatThrownBy(() -> unloaded.get("")).hasMessage("run load() before getting or setting data");
        assertThatThrownBy(() -> unloaded.delete("")).hasMessage("run load() before getting or setting data");
        assertThatThrownBy(() -> unloaded.add(node)).hasMessage("run load() before getting or setting data");
    }

    // resolveOptions, for the v2 packages entries; the legacy dependencies come from commit()
    @Test
    void resolveOptions() {
        String url = "https://private.registry.org/deadbeef/registry/-/registry-1.2.3.tgz";
        for (boolean omit : new boolean[]{true, false}) {
            Shrinkwrap meta = new Shrinkwrap("/dir", new Shrinkwrap.ResolveOptions(omit));
            meta.setData(obj("lockfileVersion", 2, "requires", true, "packages", obj(), "dependencies", obj()));
            Node root = node(opts().pkg(obj("name", "root", "dependencies", obj("registry", "^1.0.0",
              "some-other-registry", "^1.0.0", "@scoped/some-other-registry", "^1.0.0", "tar", url)))
              .path("/dir").realpath("/dir").meta(meta));
            Node registry = node(opts().pkg(obj("name", "registry", "version", "1.2.3")).resolved(url)
              .integrity("sha512-registry").parent(root));
            node(opts().pkg(obj("name", "tar", "version", "1.2.3")).resolved(url).integrity("sha512-registry").parent(root));

            assertThat(meta.get("node_modules/registry").get("resolved")).isEqualTo(omit ? null : url);
            assertThat(meta.get("node_modules/tar").get("resolved")).as("not a registry dep").isEqualTo(url);
            assertThat(Shrinkwrap.metaFromNode(registry, "").get("resolved")).as("metaFromNode default").isEqualTo(url);
        }
    }

    private static Map<String, Object> linkPkg(String name) {
        return obj("name", name, "version", "1.2.3", "funding", "https://example.com/payme",
          "_resolved", "github:isaacs/foobarbaz#aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    }

    @Test
    void constructMetadataFromNodeAndPackageData() {
        String dir = "/home/user/projects/root";
        Shrinkwrap meta = lock(dir, obj("lockfileVersion", 2, "requires", true, "dependencies", obj(), "packages", obj()));
        Node root = node(opts().pkg(obj("name", "root",
            "dependencies", obj("a", "", "link", "", "link2", "", "link3", ""),
            "devDependencies", obj("d", "", "e", "https://foo.com/e.tgz", "devit", ""),
            "optionalDependencies", obj("optin", ""),
            "peerDependencies", obj("peer", ""),
            "scripts", obj("install", "true")))
          .path(dir).realpath(dir).meta(meta));
        Node peer = node(opts().pkg(obj("name", "peer", "version", "1.2.3", "dependencies", obj("peerdep", "")))
          .resolved("https://peer.com/peer.tgz").integrity("sha512-peerpeerpeer").parent(root));
        node(opts().pkg(obj("name", "peerdep", "version", "1.2.3"))
          .resolved("https://peer.com/peerdep.tgz").integrity("sha512-peerdeppeerdep").parent(peer));
        node(opts().pkg(obj("name", "e", "version", "1.2.3", "dependencies", obj("tgz", "", "tgz-pkg-resolved", ""),
            "license", obj("type", "blurb", "url", "http://example.com/")))
          .resolved("https://foo.com/e.tgz").parent(root));
        node(opts().pkg(obj("name", "tgz", "version", "1.2.3", "funding", obj("url", "https://example.com/"),
            "engines", obj("node", ">=10"), "os", arr("any", "!win32"), "cpu", arr("x64"), "libc", arr("glibc")))
          .resolved(dir + "/archives/tarball.tgz").parent(root));
        node(opts().pkg(obj("name", "tgz-pkg-resolved", "version", "1.2.3", "funding", obj("url", "https://example.com/"),
            "_resolved", dir + "/archives/tarball-pkg-resolved.tgz",
            "_integrity", "sha512-tarball/package/resolved/integrity"))
          .parent(root));
        link(opts().path(dir + "/node_modules/link").realpath(dir + "/target").parent(root).pkg(linkPkg("link"))
          .target(node(opts().realpath(dir + "/target").path(dir + "/target").root(root).pkg(linkPkg("link")))));
        link(opts().name("link3").path(dir + "/node_modules/link3").realpath(dir + "/realPkg").parent(root).pkg(linkPkg("link3"))
          .target(node(opts().name("link3").realpath(dir + "/realPkg").path(dir + "/realPkg").root(root).pkg(linkPkg("link3")))));
        node(opts().resolved("https://example.com/a.tgz").integrity("sha512-helloyesthisisdog")
          .pkg(obj("name", "a", "version", "1.2.3")).parent(root));
        node(opts().pkg(obj("name", "optin", "version", "1.2.3", "dependencies", obj("devo", ""))).parent(root));
        node(opts().pkg(obj("name", "devit", "version", "1.2.3", "dependencies", obj("devo", ""))).parent(root));
        node(opts().pkg(obj("name", "devo", "version", "1.2.3")).parent(root));
        Node d = node(opts().pkg(obj("name", "d", "version", "1.2.3", "_shasum", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            "bundleDependencies", arr("bundled"),
            "dependencies", obj("e", "1.2.3", "bundled", "npm:not-bundled-tho@*", "git-dep", "github:foo/bar")))
          .parent(root));
        node(opts().pkg(obj("name", "not-bundled-tho", "version", "1.2.3"))
          .path(d.getPath() + "/node_modules/bundled").name("bundled").parent(d));
        node(opts().pkg(obj("name", "git-dep", "version", "1.2.3")).parent(d)
          .resolved("git+ssh://git@github.com/foo/bar.git#0000000000000000000000000000000000000000"));
        node(opts().path(root.getPath() + "/node_modules/nopkg").parent(root));

        JsonNode flags = DUMPS.get("construct").get("flags");
        assertThat(root.getInventory().size()).isEqualTo(flags.size());
        for (Node n : root.getInventory().values()) {
            String calculated = flags.get(n.getPath()).asText();
            n.setDev(calculated.charAt(0) == '1');
            n.setOptional(calculated.charAt(1) == '1');
            n.setDevOptional(calculated.charAt(2) == '1');
            n.setPeer(calculated.charAt(3) == '1');
            n.setExtraneous(calculated.charAt(4) == '1');
        }

        boolean first = true;
        for (JsonNode get : DUMPS.get("construct").get("gets")) {
            assertThat(meta.get(get.get(1).asText())).as(get.get(0).asText())
              .isEqualTo(NpmTreeSupport.<Object>json(get.get(2).asText()));
            if (first) {
                root.getPackage().put("version", "1.2.3");
                meta.add(root);
                first = false;
            }
        }
    }

    // upstream snapshots commit(); what delete() leaves behind is asserted directly
    @Test
    void handleMissingDependenciesObjectWithoutBorking() {
        Shrinkwrap s = lock("/path/to/root", obj(
          "packages", obj("node_modules/foo", obj(), "node_modules/foo/node_modules/bar", obj()),
          "dependencies", obj("foo", obj("resolved", "http://foo.com", "integrity", "foo integrity"))));
        s.delete("/path/to/root/node_modules/foo/node_modules/bar");
        assertThat(s.getData()).isEqualTo(obj(
          "packages", obj("node_modules/foo", obj()),
          "dependencies", obj("foo", obj("resolved", "http://foo.com", "integrity", "foo integrity"))));
    }

    static Stream<Arguments> yarnLockEntries() {
        List<Arguments> scenarios = new ArrayList<>();
        for (JsonNode s : DUMPS.get("yarn")) {
            scenarios.add(Arguments.of(s.get("name").asText(), s));
        }
        return scenarios.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("yarnLockEntries")
    void yarnLockEntry(String name, JsonNode s) {
        String dir = "/project";
        Shrinkwrap meta = lock(dir, obj("lockfileVersion", 3, "requires", true, "packages", obj(), "dependencies", obj()));
        meta.setYarnLock(new YarnLock().parse(s.get("yarn").asText()));
        Node root = node(opts().path(dir).realpath(dir).meta(meta)
          .pkg(obj("name", "root", "version", "1.0.0", "dependencies", obj("mkdirp", s.get("spec").asText()))));
        if (s.get("link").asBoolean()) {
            link(opts().name("mkdirp").parent(root).realpath(dir + "/mkdirp")
              .target(node(opts().path(dir + "/mkdirp").root(root).pkg(json(s.get("pkg"))))));
        } else {
            node(opts().name("mkdirp").parent(root).pkg(json(s.get("pkg"))));
        }

        List<List<@Nullable String>> decorated = new ArrayList<>();
        for (Node n : root.getInventory().values()) {
            decorated.add(arr3(n.getLocation(), n.getResolved(), n.getIntegrity()));
        }
        assertThat(decorated).isEqualTo(NpmTreeSupport.<Object>json(s.get("nodes")));
        for (JsonNode get : s.get("gets")) {
            assertThat(meta.get(get.get(0).asText())).as(get.get(0).asText())
              .isEqualTo(NpmTreeSupport.<Object>json(get.get(1).asText()));
        }
        assertThat(meta.getYarnLock().fromTree(root).toString()).isEqualTo(s.get("rewritten").asText());
    }

    private static List<@Nullable String> arr3(@Nullable String a, @Nullable String b, @Nullable String c) {
        List<@Nullable String> list = new ArrayList<>();
        list.add(a);
        list.add(b);
        list.add(c);
        return list;
    }

    private static Node fooUnder(Map<String, Object> lockedFoo, Map<String, Object> fooPkg) {
        Shrinkwrap s = lock("/some/path", obj("packages", obj("", obj(), "node_modules/foo", lockedFoo)));
        Node root = node(opts().path("/some/path").meta(s));
        return node(opts().parent(root).name("foo").pkg(fooPkg));
    }

    private static final String FOO_TGZ = "https://registry.npmjs.org/foo/foo-1.2.3.tgz";

    @Test
    void setIntegrityBecauseLocationAndResolvedMatch() {
        Node foo = fooUnder(
          obj("resolved", FOO_TGZ, "version", "1.2.3", "integrity", "sha512-happyhappyjoyjoy"),
          obj("version", "1.2.3", "_resolved", FOO_TGZ));
        assertThat(foo.getIntegrity()).isEqualTo("sha512-happyhappyjoyjoy");
    }

    @Test
    void setIntegrityBecauseLocationMatchesAndNoResolved() {
        Node foo = fooUnder(
          obj("resolved", FOO_TGZ, "version", "1.2.3", "integrity", "sha512-happyhappyjoyjoy"),
          obj("version", "1.2.3"));
        assertThat(foo.getResolved()).isEqualTo("https://registry.npmjs.org/foo/foo-1.2.3.tgz");
        assertThat(foo.getIntegrity()).isEqualTo("sha512-happyhappyjoyjoy");
    }

    @Test
    void setIntegrityButNoResolved() {
        Node foo = fooUnder(obj("version", "1.2.3", "integrity", "sha512-happyhappyjoyjoy"), obj("version", "1.2.3"));
        assertThat(foo.getResolved()).isNull();
        assertThat(foo.getIntegrity()).isEqualTo("sha512-happyhappyjoyjoy");
    }

    private static YarnLock.Entry entry(String integrity, String resolved, String version, String... specs) {
        YarnLock.Entry entry = new YarnLock.Entry(Arrays.asList(specs));
        entry.set("integrity", integrity);
        entry.set("resolved", resolved);
        entry.set("version", version);
        return entry;
    }

    @Test
    void getMetaFromYarnLock() {
        String fooIntegrity = "sha512-freebeerisworththeprice";
        String barIntegrity = "sha512-integrity is allegiance to your truest self";
        YarnLock.Entry fooEntry = entry(fooIntegrity, "https://example.com/foo.tgz", "1.2.3", "foo@1.x", "foo@1.2.x");
        fooEntry.set("optionalDependencies", obj("bar", "2.x"));
        YarnLock.Entry foo2Entry = entry("the second coming of fooo", "https://example.com/foo-2.tgz", "2.3.4", "foo@2.x");
        YarnLock.Entry barEntry = entry(barIntegrity, "file:bar-2.3.4.tgz", "2.3.4", "bar@2.x");
        YarnLock yarnLock = new YarnLock().parse("");
        yarnLock.getEntries().put("foo@1.x", fooEntry);
        yarnLock.getEntries().put("foo@1.2.x", fooEntry);
        yarnLock.getEntries().put("bar@2.x", barEntry);
        yarnLock.getEntries().put("foo@2.x", foo2Entry);

        String dir = "/path/to/root";
        Shrinkwrap meta = lock(dir, obj("lockfileVersion", 2, "packages", obj(), "dependencies", obj(), "requires", true));
        meta.setYarnLock(yarnLock);

        Node tree = node(opts().meta(meta).path(dir).realpath(dir).pkg(obj("name", "root", "version", "4.5.6",
          "dependencies", obj("foo", "1.x"), "devDependencies", obj("bar", "2.x"))));

        Node foo = node(opts().name("foo").parent(tree)
          .pkg(obj("name", "foo", "version", "1.2.3", "optionalDependencies", obj("bar", "2.x"))));
        assertThat(foo.getIntegrity()).as("foo integrity from yarn.lock").isEqualTo(fooIntegrity);
        assertThat(foo.getResolved()).as("foo resolved from yarn.lock").isEqualTo("https://example.com/foo.tgz");

        Node bar = node(opts().name("bar").parent(tree).pkg(obj("name", "bar", "version", "2.3.4")));
        assertThat(bar.getIntegrity()).as("bar integrity from yarn.lock").isEqualTo(barIntegrity);
        assertThat(bar.getResolved()).as("bar resolved from yarn.lock").isEqualTo("file:/path/to/root/bar-2.3.4.tgz");

        bar.setParent(null);

        Node barDiffVersion = node(opts().name("bar").parent(tree)
          .pkg(obj("name", "bar", "description", "witaf", "version", "2.4.5")));
        assertThat(barDiffVersion.getIntegrity()).as("version mismatch, no integrity").isNull();
        assertThat(barDiffVersion.getResolved()).as("version mismatch, no resolved").isNull();

        Node barDiffIntegrity = node(opts().integrity("sha512-a fundamental lack of commitment to ideals").name("bar")
          .parent(tree).pkg(obj("name", "bar", "version", "2.3.4")));
        assertThat(barDiffIntegrity.getIntegrity()).as("integrity not updated from yarn lock")
          .isEqualTo("sha512-a fundamental lack of commitment to ideals");
        assertThat(barDiffIntegrity.getResolved()).as("integrity mismatch, no resolved").isNull();

        Node barDiffResolved = node(opts().resolved("https://x.com/b.tgz").name("bar").parent(tree)
          .pkg(obj("name", "bar", "version", "2.3.4")));
        assertThat(barDiffResolved.getIntegrity()).as("integrity not updated from yarn lock").isNull();
        assertThat(barDiffResolved.getResolved()).as("resolved was not updated from yarn lock").isEqualTo("https://x.com/b.tgz");

        Node barSameIntegrity = node(opts().integrity(barIntegrity).name("bar").parent(tree)
          .pkg(obj("name", "bar", "version", "2.3.4")));
        assertThat(barSameIntegrity.getIntegrity()).as("bar integrity still matches").isEqualTo(barIntegrity);
        assertThat(barSameIntegrity.getResolved()).as("bar resolved from yarn.lock")
          .isEqualTo("file:/path/to/root/bar-2.3.4.tgz");

        Node barSameResolved = node(opts().resolved("file:/path/to/root/bar-2.3.4.tgz").name("bar").parent(tree)
          .pkg(obj("name", "bar", "version", "2.3.4")));
        assertThat(barSameResolved.getIntegrity()).as("bar integrity from yarn.lock").isEqualTo(barIntegrity);
        assertThat(barSameResolved.getResolved()).as("bar resolved still matches").isEqualTo("file:/path/to/root/bar-2.3.4.tgz");

        barEntry.set("resolved", null);
        barEntry.set("integrity", null);
        bar.getPackage().put("description", "new integrity, no resolved");
        bar.setIntegrity("new integrity");
        bar.setResolved(null);
        bar.setParent(tree);
        assertThat(bar.getIntegrity()).as("integrity unchanged by yarn lock").isEqualTo("new integrity");
        assertThat(bar.getResolved()).as("resolved set to null, not in yarn entry").isNull();

        bar.getPackage().put("description", "new resolved, no integrity");
        bar.setParent(null);
        bar.setIntegrity(null);
        bar.setResolved("new resolved");
        bar.setParent(tree);
        assertThat(bar.getIntegrity()).as("integrity set to null, not in yarn entry").isNull();
        assertThat(bar.getResolved()).as("resolved unchanged by yarn lock").isEqualTo("new resolved");

        // upstream passes its package under a key Node does not read, so this foo has none
        Node foo2 = node(opts().name("foo").parent(tree));
        assertThat(foo2.getIntegrity()).as("no integrity, entry invalid").isNull();
        assertThat(foo2.getResolved()).as("no resolved, entry invalid").isNull();
    }

    @Test
    void metadataThatOnlyHasOneOfResolvedOrIntegrity() {
        String dir = "/path/to/root";
        Shrinkwrap meta = lock(dir, obj("name", "root", "version", "4.5.6", "lockfileVersion", 2,
          "packages", obj(
            "node_modules/integrity", obj("integrity", "has integrity no resolved"),
            "node_modules/resolved", obj("resolved", "file:has-resolved-no-integrity.tgz"),
            "node_modules/intalready", obj("integrity", "superceded by node integrity value"),
            "node_modules/resalready", obj("resolved", "superceded by node resolved value")),
          "dependencies", obj()));

        Node tree = node(opts().path(dir).realpath(dir).meta(meta));
        Node integrity = node(opts().name("integrity").parent(tree));
        Node resolved = node(opts().name("resolved").parent(tree));
        Node intalready = node(opts().name("intalready").parent(tree).integrity("pre-existing integrity"));
        Node resalready = node(opts().name("resalready").parent(tree).resolved("pre-existing resolved"));

        assertThat(integrity.getIntegrity()).as("integrity only").isEqualTo("has integrity no resolved");
        assertThat(integrity.getResolved()).as("integrity only").isNull();

        assertThat(resolved.getResolved()).as("resolved only").isEqualTo("file:/path/to/root/has-resolved-no-integrity.tgz");
        assertThat(resolved.getIntegrity()).as("resolved only").isNull();

        assertThat(intalready.getResolved()).as("integrity only, from node settings").isNull();
        assertThat(intalready.getIntegrity()).as("integrity only, from node settings").isEqualTo("pre-existing integrity");

        assertThat(resalready.getResolved()).as("resolved only, from node settings").isEqualTo("pre-existing resolved");
        assertThat(resalready.getIntegrity()).as("resolved only, from node settings").isNull();
    }

    @Test
    void doNotAddMetadataIfVersionsMismatch() {
        Shrinkwrap meta = lock("/home/user/projects/root",
          obj("lockfileVersion", 2, "requires", true, "dependencies", obj(), "packages", obj()));
        Node root = node(opts().path("/home/usr/projects/root").meta(meta)
          .pkg(obj("name", "root", "version", "1.2.3", "dependencies", obj("foo", "1")))
          .child(opts().name("foo").pkg(obj("name", "foo", "version", "1.0.0"))
            .resolved("https://registry.npmjs.org/foo-1.0.0.tgz").integrity("sha512-this is no sha of mine").build()));
        Node oldFoo = root.getChildren().get("foo");
        Node newFoo = node(opts().path(oldFoo.getPath()).root(root).pkg(obj("name", "foo", "version", "1.2.3")));
        assertThat(newFoo.getResolved()).isNull();
        assertThat(newFoo.getIntegrity()).isNull();
    }
}
