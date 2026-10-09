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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.canonical;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.describe;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.fixture;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.jsValue;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.json;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.node;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.obj;
import static org.openrewrite.javascript.internal.lock.npm.NpmTreeSupport.opts;

/**
 * Port of @npmcli/arborist test/arborist/load-virtual.js over its fixtures, with the trees arborist
 * itself loaded from them (the load-virtual-dumps fixture) as the expectation.
 */
class VirtualTreeLoaderTest {

    static final JsonNode DUMPS = fixture("load-virtual-dumps.gz");
    static final String ROOT = "/fixtures";

    static VirtualTreeLoader.ProjectFiles files(String inputs) {
        JsonNode in = DUMPS.get("inputs").get(inputs);
        VirtualTreeLoader.ProjectFiles.ProjectFilesBuilder files = VirtualTreeLoader.ProjectFiles.builder()
          .lock(in.get("lock").isNull() ? null : new LockFile(in.get("lock").asText()))
          .yarnLock(in.get("yarnLock").isNull() ? null : in.get("yarnLock").asText());
        in.get("manifests").fields().forEachRemaining(e -> files.manifest(e.getKey(), e.getValue().asText()));
        return files.build();
    }

    static String path(String inputs) {
        return ROOT + "/" + inputs;
    }

    static Stream<Arguments> cases() {
        List<Arguments> out = new ArrayList<>();
        for (JsonNode c : DUMPS.get("loadVirtual")) {
            if (!c.get("name").asText().contains("(")) {
                out.add(Arguments.of(c.get("name").asText(), c));
            }
        }
        return out.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void loadsTheTreeArboristLoads(String name, JsonNode c) {
        VirtualTreeLoader loader = new VirtualTreeLoader(path(name), NpmOptions.DEFAULT);
        if (c.has("error")) {
            assertThatThrownBy(() -> loader.loadVirtual(files(name))).hasMessage(c.get("error").get("message")
              .asText());
            return;
        }
        Node tree = loader.loadVirtual(files(name));
        assertThat(describe(tree)).as("tree").isEqualTo(c.get("tree").asText());
        assertThat(loader.isFlagsSuspect()).as("flagsSuspect").isEqualTo(c.get("flagsSuspect").asBoolean());
        Shrinkwrap meta = tree.getMeta();
        assertThat(canonical(jsValue(meta.getLockfileVersion()))).as("lockfileVersion")
          .isEqualTo(canonical(json(c.get("lockfileVersion"))));
        assertThat(meta.isLoadedFromDisk()).isEqualTo(c.get("loadedFromDisk").asBoolean());
        assertThat(meta.isAncientLockfile()).isEqualTo(c.get("ancientLockfile").asBoolean());
        assertThat(meta.toString()).as("toString").isEqualTo(c.get("string").asText());
        DepFlags.resetDepFlags(tree);
        DepFlags.calcDepFlags(tree);
        assertThat(describe(tree)).as("tree after flags")
          .isEqualTo(c.get("treeAfterFlags").isNull() ? c.get("tree").asText() : c.get("treeAfterFlags").asText());
        assertThat(meta.toString()).as("toString after flags")
          .isEqualTo(c.get("stringAfterFlags").isNull() ? c.get("string").asText() : c.get("stringAfterFlags")
            .asText());
        if (c.has("yarnLockAfter")) {
            assertThat(meta.getYarnLock().toString()).isEqualTo(c.get("yarnLockAfter").asText());
        }
    }

    @Test
    void sameTreeReused() {
        VirtualTreeLoader loader = new VirtualTreeLoader(path("install-types"), NpmOptions.DEFAULT);
        Node tree = loader.loadVirtual(files("install-types"));
        assertThat(loader.loadVirtual(files("install-types"))).isSameAs(tree);
    }

    private static JsonNode dump(String name) {
        for (JsonNode c : DUMPS.get("loadVirtual")) {
            if (c.get("name").asText().equals(name)) {
                return c;
            }
        }
        throw new IllegalArgumentException(name);
    }

    @Test
    void loadFromFixtureUsingFilesystemRootPath() {
        Node root = node(opts().path("/").pkg(json(files("install-types").getManifests().get(""))));
        VirtualTreeLoader loader = new VirtualTreeLoader("/", NpmOptions.DEFAULT);
        assertThat(loader.loadVirtual(files("install-types"), root)).isSameAs(root);
    }

    @Test
    void loadFromRootThatAlreadyHasShrinkwrap() {
        String name = "tap-and-flow";
        VirtualTreeLoader.ProjectFiles files = files(name);
        Shrinkwrap meta = new Shrinkwrap(path(name), NpmOptions.DEFAULT).load(files);
        Node root = node(opts().path(path(name)).pkg(json(files.getManifests().get(""))).meta(meta));
        VirtualTreeLoader loader = new VirtualTreeLoader(path(name), NpmOptions.DEFAULT);
        assertThat(loader.loadVirtual(files, root)).isSameAs(root);
        JsonNode c = dump("tap-and-flow (root with meta)");
        assertThat(describe(root)).isEqualTo(c.get("tree").asText());
        assertThat(loader.isFlagsSuspect()).isEqualTo(c.get("flagsSuspect").asBoolean());
        assertThat(root.getMeta().toString()).isEqualTo(c.get("string").asText());
    }

    @Test
    void doNotResetFlagsOnSuppliedRootOption() {
        String name = "test-package-with-shrinkwrap";
        Node root = node(opts().path(path(name)).pkg(json(files(name).getManifests().get("")))
          .dev(true).optional(false).peer(false).devOptional(true));
        VirtualTreeLoader loader = new VirtualTreeLoader(path(name), NpmOptions.DEFAULT);
        Node tree = loader.loadVirtual(files(name), root);
        assertThat(tree.isDev()).as("tree is still dev").isTrue();
        assertThat(tree.isDevOptional()).as("tree is still devOptional").isTrue();
        assertThat(tree.isOptional()).as("tree is not optional").isFalse();
        assertThat(tree.isPeer()).as("tree is not peer").isFalse();
        JsonNode c = dump(name + " (root option)");
        assertThat(describe(tree)).isEqualTo(c.get("tree").asText());
        assertThat(loader.isFlagsSuspect()).isTrue();
        assertThat(tree.getMeta().toString()).isEqualTo(c.get("string").asText());
    }

    @Test
    void changedButReusingTheSameRootThatAlreadyHasMeta() {
        String name = "edit-package-json/changed";
        VirtualTreeLoader changeArb = new VirtualTreeLoader(path(name), NpmOptions.DEFAULT);
        Node root = changeArb.loadVirtual(files(name));
        assertThat(changeArb.isFlagsSuspect()).isTrue();
        root.getMeta().setLoadedFromDisk(false);
        VirtualTreeLoader newArb = new VirtualTreeLoader(path(name), NpmOptions.DEFAULT);
        Node tree = newArb.loadVirtual(files(name), root);
        JsonNode c = dump(name + " (re-used root)");
        assertThat(describe(tree)).isEqualTo(c.get("tree").asText());
        assertThat(newArb.isFlagsSuspect()).as("flags not suspect").isFalse();
        assertThat(tree.getMeta().toString()).isEqualTo(c.get("string").asText());
    }

    @Test
    void doNotBundleTheEntireUniverse() {
        Node tree = new VirtualTreeLoader(path("tap-old-lockfile"), NpmOptions.DEFAULT)
          .loadVirtual(files("tap-old-lockfile"));
        List<Object> bundled = new ArrayList<>((List<?>) tree.getChildren().get("tap").getPackage()
          .get("bundleDependencies"));
        bundled.sort(null);
        assertThat(bundled).containsExactly("@types/react", "import-jsx", "ink", "minipass", "signal-exit",
                                            "tap-parser",
          "tap-yaml", "treport", "yaml");
    }

    @Test
    void errorWhenLinkTargetIsMissing() {
        VirtualTreeLoader.ProjectFiles files = VirtualTreeLoader.ProjectFiles.builder()
          .manifest("", "{\"name\":\"root\",\"workspaces\":[\"packages/*\"]}")
          .manifest("packages/some-folder/my-package", "{\"name\":\"@my-scope/my-package\",\"version\":\"1.0.0\"}")
          .lock(new LockFile(JsonStringifyNice.stringify(obj("name", "root", "lockfileVersion", 3, "packages", obj(
            "", obj("workspaces", NpmTreeSupport.arr("packages/*")),
            "node_modules/@my-scope/my-package", obj("resolved", "packages/some-folder/my-package", "link", true))))))
          .build();
        assertThatThrownBy(() -> new VirtualTreeLoader("/project", NpmOptions.DEFAULT).loadVirtual(files))
          .isInstanceOf(VirtualTreeLoader.MissingTargetException.class)
          .hasMessageStartingWith("Missing target in lock file: \"packages/some-folder/my-package\" is referenced by " +
                                  "\"node_modules/@my-scope/my-package\" but does not exist.");
    }

    @Test
    void withoutSubtreeOnlyRootLockfilesWithFileEntriesStillLoad() {
        VirtualTreeLoader.ProjectFiles files = VirtualTreeLoader.ProjectFiles.builder()
          .manifest("",
                    "{\"name\":\"project\",\"version\":\"1.0.0\",\"dependencies\":{\"sibling\":\"file:../sibling\"}}")
          .manifest("../sibling", "{\"name\":\"sibling\",\"version\":\"1.0.0\"}")
          .lock(new LockFile(JsonStringifyNice.stringify(obj("name", "project", "version", "1.0.0", "lockfileVersion",
          3,
            "requires", true,
            "packages", obj(
              "", obj("name", "project", "version", "1.0.0", "dependencies", obj("sibling", "file:../sibling")),
              "../sibling", obj("name", "sibling", "version", "1.0.0"),
              "node_modules/sibling", obj("resolved", "../sibling", "link", true))))))
          .build();
        Node tree = new VirtualTreeLoader("/dir/project", NpmOptions.DEFAULT).loadVirtual(files);
        assertThat(tree.getChildren().get("sibling")).as("external file:../ link still loads").isNotNull();
    }
}
