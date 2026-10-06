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
import java.util.Collections;
import java.util.LinkedHashMap;
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
 * Port of @npmcli/arborist test/shrinkwrap.js. Expectations upstream keeps in snapshots, or reaches by
 * reading a tree or a fixture directory from disk, are arborist's own answers in the shrinkwrap-dumps and
 * load-virtual-dumps fixtures.
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
            Shrinkwrap meta = new Shrinkwrap("/dir", NpmOptions.builder().omitLockfileRegistryResolved(omit).build());
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

    private static final JsonNode LOAD_DUMPS = VirtualTreeLoaderTest.DUMPS;

    static Stream<Arguments> loadCases() {
        List<Arguments> out = new ArrayList<>();
        for (JsonNode c : LOAD_DUMPS.get("shrinkwrap")) {
            out.add(Arguments.of(c.get("name").asText(), c));
        }
        return out.stream();
    }

    private static Shrinkwrap loaded(JsonNode c) {
        VirtualTreeLoader.ProjectFiles files = VirtualTreeLoaderTest.files(c.get("inputs").asText());
        NpmOptions options = c.get("lockfileVersion").isNull() ? NpmOptions.DEFAULT :
          NpmOptions.builder().lockfileVersion(c.get("lockfileVersion").asInt()).build();
        return new Shrinkwrap(VirtualTreeLoaderTest.path(c.get("inputs").asText()), options)
          .load(files.getLock(), files.getYarnLock(), files.getManifests().get(""));
    }

    private static String canonical(@Nullable Object data) {
        return JsonStringifyNice.stringify(data, Collections.emptyList(), 0);
    }

    private static @Nullable Object jsValue(@Nullable Object value) {
        return value == Js.UNDEFINED ? null : value;
    }

    // test/shrinkwrap.js's load, lockfileVersion, reset, format and commit cases, over arborist's fixtures
    @ParameterizedTest(name = "{0}")
    @MethodSource("loadCases")
    void loadsAndWritesAsArboristDoes(String name, JsonNode c) {
        Shrinkwrap s = loaded(c);
        JsonNode l = c.get("loaded");
        assertThat(canonical(jsValue(s.getLockfileVersion()))).as("lockfileVersion").isEqualTo(canonical(json(l.get("lockfileVersion"))));
        assertThat(canonical(jsValue(s.getOriginalLockfileVersion()))).as("originalLockfileVersion")
          .isEqualTo(canonical(json(l.get("originalLockfileVersion"))));
        if (l.has("loadedFromDisk")) {
            assertThat(s.isLoadedFromDisk()).as("loadedFromDisk").isEqualTo(l.get("loadedFromDisk").asBoolean());
            assertThat(s.isAncientLockfile()).as("ancientLockfile").isEqualTo(l.get("ancientLockfile").asBoolean());
            assertThat(s.getLoadingError() != null).as("loadingError").isEqualTo(!l.get("loadingError").isNull());
            assertThat(canonical(s.getIndent())).as("indent").isEqualTo(canonical(json(l.get("indent"))));
            assertThat(s.getNewline()).as("newline").isEqualTo(l.get("newline").asText());
            if (l.has("yarnEntries")) {
                assertThat(s.getYarnLock() == null ? null : s.getYarnLock().getEntries().size()).as("yarn entries")
                  .isEqualTo(l.get("yarnEntries").isNull() ? null : l.get("yarnEntries").asInt());
            }
        }
        if (l.has("data")) {
            assertThat(canonical(s.getData())).as("data").isEqualTo(canonical(json(l.get("data").asText())));
        }
        assertThat(s.toString()).as("toString").isEqualTo(c.get("string").asText());
        if (c.has("stringUnformatted")) {
            assertThat(s.toString(false)).as("format: false").isEqualTo(c.get("stringUnformatted").asText());
            assertThat(s.toString("\t")).as("format: tab").isEqualTo(c.get("stringTab").asText());
        }
        if (c.has("reset")) {
            Shrinkwrap r = c.get("name").asText().startsWith("lockfileVersion ") ?
              new Shrinkwrap(VirtualTreeLoaderTest.path(c.get("inputs").asText()), c.get("lockfileVersion").isNull() ? NpmOptions.DEFAULT :
                NpmOptions.builder().lockfileVersion(c.get("lockfileVersion").asInt()).build()) : s;
            r.reset();
            JsonNode reset = c.get("reset");
            assertThat(canonical(r.getData())).as("reset data").isEqualTo(canonical(json(reset.get("data").asText())));
            assertThat(canonical(jsValue(r.getLockfileVersion()))).as("reset lockfileVersion")
              .isEqualTo(canonical(json(reset.get("lockfileVersion"))));
            assertThat(canonical(jsValue(r.getOriginalLockfileVersion()))).as("reset originalLockfileVersion")
              .isEqualTo(canonical(json(reset.get("originalLockfileVersion"))));
        }
    }

    @Test
    void shrinkwrapKeyOrder() {
        assertThat(Shrinkwrap.KEY_ORDER).containsExactly("name", "version", "lockfileVersion", "resolved", "integrity",
          "requires", "packages", "dependencies");
    }

    @Test
    void loadAndChangeLockfileVersion() {
        VirtualTreeLoader.ProjectFiles files = VirtualTreeLoaderTest.files("install-types");
        String path = VirtualTreeLoaderTest.path("install-types");
        Shrinkwrap vDefault = new Shrinkwrap(path).load(files.getLock(), null, files.getManifests().get(""));
        Shrinkwrap v1 = new Shrinkwrap(path, NpmOptions.builder().lockfileVersion(1).build()).load(files.getLock(), null, files.getManifests().get(""));
        Shrinkwrap v2 = new Shrinkwrap(path, NpmOptions.builder().lockfileVersion(2).build()).load(files.getLock(), null, files.getManifests().get(""));
        Shrinkwrap v3 = new Shrinkwrap(path, NpmOptions.builder().lockfileVersion(3).build()).load(files.getLock(), null, files.getManifests().get(""));
        assertThat(canonical(vDefault.getData())).as("default is same as version 3").isEqualTo(canonical(v3.getData()));
        Map<String, Object> v1Data = v1.commit();
        Map<String, Object> v2Data = v2.commit();
        Map<String, Object> v3Data = v3.commit();
        Map<String, Object> merged = new LinkedHashMap<>(v1Data);
        merged.putAll(v3Data);
        merged.put("lockfileVersion", 2);
        assertThat(canonical(v2Data)).as("v2 is superset of v1 and v3").isEqualTo(canonical(merged));
        assertThat(v1Data).as("v1 data does not have packages").doesNotContainKey("packages");
        assertThat(v3Data).as("v3 data does not have dependencies").doesNotContainKey("dependencies");
    }

    @Test
    void loadingInEmptyDirGetsEmptyLockfile() {
        String dir = "/fixtures/empty";
        Shrinkwrap sw = new Shrinkwrap(dir).load(null, null, null);
        assertThat(canonical(sw.getData())).isEqualTo(canonical(obj("lockfileVersion", 3, "requires", true,
          "dependencies", obj(), "packages", obj())));
        assertThat(sw.isLoadedFromDisk()).isFalse();
        Node root = node(opts().path(dir).realpath(dir));
        root.unsetDepFlags();
        sw.add(root);
        assertThat(canonical(sw.commit())).isEqualTo(canonical(obj("name", "empty", "lockfileVersion", 3, "requires", true,
          "packages", obj())));
    }

    @Test
    void lookUpFromLocksAndSuch() {
        VirtualTreeLoader.ProjectFiles files = VirtualTreeLoaderTest.files("install-types");
        Shrinkwrap m = new Shrinkwrap(VirtualTreeLoaderTest.path("install-types")).load(files.getLock(), null, files.getManifests().get(""));
        assertThat(canonical(m.get(""))).as("root metadata").isEqualTo(canonical(obj("name", "a", "version", "1.2.3",
          "dependencies", obj("abbrev", "^1.1.1", "full-git-url", "git+https://github.com/isaacs/abbrev-js.git",
            "ghshort", "github:isaacs/abbrev-js", "old", "npm:abbrev@^1.0.3", "pinned", "npm:abbrev@^1.1.1",
            "reg", "npm:abbrev@^1.1.1", "remote", "https://registry.npmjs.org/abbrev/-/abbrev-1.1.1.tgz",
            "symlink", "file:./abbrev-link-target", "tarball", "file:abbrev-1.1.1.tgz", "bundler", "1.2.3"))));
        assertThat(m.isLoadedFromDisk()).isTrue();
        assertThat(m.get("package/not/found")).as("not found").isEmpty();
        assertThat(m.get("node_modules/old/node_modules/notfound")).as("fall off the dep tree").isEmpty();
    }

    @Test
    void loadAResolutionFromYarnLockIfWeDoNotHaveOurOwn() {
        String sha = "sha512-N2REVrJ/X/jGPfit2d7zea2J1pf7EAR5chIUcfHffAZ7gmlam5U65sAm76+o4ntQbSRdTjYf7qZz3chuHlwXEA==";
        String yarn = "\n# THIS IS AN AUTOGENERATED FILE. DO NOT EDIT THIS FILE DIRECTLY.\n# yarn lockfile v1\n\n" +
                      "mkdirp@^1.0.2:\n  version \"1.0.2\"\n  resolved \"https://registry.yarnpkg.com/mkdirp/-/mkdirp-1.0.2.tgz#5ccd93437619ca7050b538573fc918327eba98fb\"\n  integrity " + sha + "\n\n" +
                      "mcdork@^1.0.0:\n  version \"1.0.2\"\n  resolved \"https://registry.npmjs.org/mcdork/-/mcdork-1.0.2.tgz#5ccd93437619ca7050b538573fc918327eba98fb\"\n  integrity " + sha + "\n\n" +
                      "otherreg@^1.0.0:\n  version \"1.0.2\"\n  resolved \"https://otherreg.registry.npm/othrreg/-/othrreg-1.0.2.tgz#5ccd93437619ca7050b538573fc918327eba98fb\"\n  integrity " + sha + "\n\n" +
                      "brokenempty@^1.2.3:\n  version \"1.2.3\"\n\n" +
                      "regalias@xyz:\n  version \"1.0.2\"\n  resolved \"https://registry.yarnpkg.com/mkdirp/-/mkdirp-1.0.2.tgz#5ccd93437619ca7050b538573fc918327eba98fb\"\n  integrity " + sha + "\n";
        Shrinkwrap s = new Shrinkwrap("/path").load(null, yarn, "{\"dependencies\":{\"mkdirp\":\"^1.0.2\"}}");
        Map<String, Object> opt = new LinkedHashMap<>();
        assertThat(s.checkYarnLock("mkdirp@^1.0.2", opt).getRaw()).isEqualTo("mkdirp@1.0.2");
        assertThat(opt).isEqualTo(obj("resolved", "https://registry.npmjs.org/mkdirp/-/mkdirp-1.0.2.tgz", "integrity", sha));
        opt = new LinkedHashMap<>();
        assertThat(s.checkYarnLock("mcdork@^1.0.0", opt).getRaw()).isEqualTo("mcdork@1.0.2");
        assertThat(opt).isEqualTo(obj("resolved", "https://registry.npmjs.org/mcdork/-/mcdork-1.0.2.tgz", "integrity", sha));
        opt = new LinkedHashMap<>();
        assertThat(s.checkYarnLock("notfound@^1.0.0", opt).getRaw()).isEqualTo("notfound@^1.0.0");
        assertThat(opt).isEmpty();
        opt = new LinkedHashMap<>();
        assertThat(s.checkYarnLock("otherreg@^1.0.0", opt).getRaw()).isEqualTo("otherreg@https://otherreg.registry.npm/othrreg/-/othrreg-1.0.2.tgz");
        assertThat(opt).isEqualTo(obj("resolved", "https://otherreg.registry.npm/othrreg/-/othrreg-1.0.2.tgz", "integrity", sha));
        opt = new LinkedHashMap<>();
        assertThat(s.checkYarnLock("brokenempty@^1.2.3", opt).getRaw()).isEqualTo("brokenempty@^1.2.3");
        assertThat(opt).isEmpty();
        opt = new LinkedHashMap<>();
        assertThat(s.checkYarnLock("regalias@xyz", opt).getRaw()).isEqualTo("regalias@npm:mkdirp@1.0.2");
        assertThat(opt).isEqualTo(obj("resolved", "https://registry.npmjs.org/mkdirp/-/mkdirp-1.0.2.tgz", "integrity", sha));
        assertThat(s.checkYarnLock("mkdirp", new LinkedHashMap<>()).getRaw()).isEqualTo("mkdirp");
        s.setLoadedFromDisk(true);
        opt = new LinkedHashMap<>();
        assertThat(s.checkYarnLock("regalias@xyz", opt).getRaw()).isEqualTo("regalias@xyz");
        assertThat(opt).isEmpty();
        s.setLoadedFromDisk(false);
        YarnLock yarnLock = s.getYarnLock();
        s.setYarnLock(null);
        opt = new LinkedHashMap<>();
        assertThat(s.checkYarnLock("regalias@xyz", opt).getRaw()).isEqualTo("regalias@xyz");
        assertThat(opt).isEmpty();
        s.setYarnLock(yarnLock);
    }

    @Test
    void ignoreYarnLockFileParseErrors() {
        Shrinkwrap s = new Shrinkwrap("/dir").load(null, "this is not a yarn lock file!", null);
        assertThat(s.getYarnLock()).isNotNull();
        assertThat(s.getYarnLock().getEntries()).isEmpty();
    }

    @Test
    void skipInertNodesInCommit() {
        String path = "/proxy";
        Shrinkwrap meta = lock(path, obj("lockfileVersion", 3, "packages", obj()));
        Node root = node(opts().pkg(obj("name", "proxy-registry-repro", "version", "1.0.0", "devDependencies", obj("esbuild", "^0.27.0")))
          .path(path).realpath(path));
        Node esbuild = node(opts().pkg(obj("name", "esbuild", "version", "0.27.7",
          "optionalDependencies", obj("@esbuild/linux-x64", "0.27.7", "@esbuild/aix-ppc64", "0.27.7"))).name("esbuild").parent(root));
        esbuild.setDev(true);
        Node validDep = node(opts().pkg(obj("name", "@esbuild/linux-x64", "version", "0.27.7", "os", arr("linux"), "cpu", arr("x64")))
          .name("@esbuild/linux-x64").parent(root));
        validDep.setOptional(true);
        validDep.setDev(true);
        Node brokenDep = node(opts().pkg(obj("name", "@esbuild/aix-ppc64")).name("@esbuild/aix-ppc64").parent(esbuild));
        brokenDep.setOptional(true);
        brokenDep.setDev(true);
        brokenDep.setExtraneous(false);
        brokenDep.setInert(true);
        Node fileDep = node(opts().pkg(obj("name", "my-local-optional")).name("my-local-optional").parent(root)
          .resolved("file:../my-local-optional"));
        fileDep.setOptional(true);
        fileDep.setExtraneous(false);
        Node localDep = node(opts().pkg(obj("name", "my-disk-optional")).name("my-disk-optional").parent(root));
        localDep.setOptional(true);
        localDep.setExtraneous(false);

        meta.add(root);
        Map<String, Object> packages = NpmTreeSupport.json(canonical(meta.commit().get("packages")));
        assertThat(packages).as("valid optional dep is included").containsKey("node_modules/@esbuild/linux-x64");
        assertThat(((Map<?, ?>) packages.get("node_modules/@esbuild/linux-x64")).get("version")).isEqualTo("0.27.7");
        assertThat(packages).as("inert optional dep is excluded from lockfile").doesNotContainKey("node_modules/esbuild/node_modules/@esbuild/aix-ppc64");
        assertThat(packages).as("file: optional dep without version is preserved").containsKey("node_modules/my-local-optional");
        assertThat(packages).as("local optional dep without version is preserved").containsKey("node_modules/my-disk-optional");
    }

    @Test
    void shrinkwrapWhereRootIsALinkNode() {
        Shrinkwrap meta = new Shrinkwrap("/actual/project/path");
        meta.reset();
        Node root = link(opts().path("/some/link/path").realpath("/actual/project/path").meta(meta));
        node(opts().root(root).path("/actual/project/path").pkg(obj("name", "path", "version", "1.2.3", "dependencies", obj("kid", "")))
          .child(opts().pkg(obj("name", "kid", "version", "1.2.3")).build()));
        assertThat(canonical(root.getMeta().commit())).isEqualTo(canonical(obj(
          "lockfileVersion", 3, "requires", true,
          "packages", obj("", obj("version", "1.2.3", "dependencies", obj("kid", ""), "extraneous", true),
            "node_modules/kid", obj("version", "1.2.3", "extraneous", true)),
          "name", "path", "version", "1.2.3", "extraneous", true)));
    }

    @Test
    void writeBackWithPendingChanges() {
        VirtualTreeLoader.ProjectFiles files = VirtualTreeLoaderTest.files("install-types");
        String fixture = VirtualTreeLoaderTest.path("install-types");
        Shrinkwrap s = new Shrinkwrap(fixture, NpmOptions.builder().lockfileVersion(2).build())
          .load(files.getLock(), null, files.getManifests().get(""));
        Node node = node(opts().path(fixture + "/node_modules/newthing").pkg(obj("name", "newthing", "version", "1.2.3")));
        s.add(node);
        String preCommit = canonical(s.getData());
        Map<String, Object> postCommit = s.commit();
        assertThat(canonical(postCommit)).as("committing changes the data").isNotEqualTo(preCommit);
        s.delete(node.getLocation());
        s.add(node);
        s.toString();
        assertThat(canonical(s.getData())).as("committed changes to data").isEqualTo(canonical(postCommit));
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
