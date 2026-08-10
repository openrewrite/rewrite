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
package org.openrewrite.javascript.internal.lock;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.internal.lock.LockEditSet.PackageEdit;
import org.openrewrite.javascript.internal.registry.VersionManifest;

import java.util.*;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for the graph-to-bun.lock diff's placement decisions, driven by an in-memory {@link Registry}. The
 * end-to-end byte contract lives in {@code BunResolveAndPatchLockRegenTest}; these pin the decisions themselves —
 * which fresh tuple sits top-level, which nests as {@code parent/name}, what the lock already decided, and which
 * shapes defer.
 */
class BunLockDiffTest {

    private static final String ROOT_ONLY_LOCK = "{\n" +
            "  \"lockfileVersion\": 1,\n" +
            "  \"configVersion\": 1,\n" +
            "  \"workspaces\": {\n" +
            "    \"\": {\n" +
            "      \"name\": \"t\",\n" +
            "    },\n" +
            "  },\n" +
            "  \"packages\": {},\n" +
            "}\n";

    @Test
    void declaredForkWinnerTakesTopAndOtherNestsUnderParent() {
        // root directly declares shared@2.0.0 (wins the top slot) and parent, whose shared@1.0.0 nests.
        FakeRegistry registry = new FakeRegistry()
                .add("parent", "1.0.0", singletonMap("shared", "1.0.0"))
                .add("shared", "1.0.0", emptyMap())
                .add("shared", "2.0.0", emptyMap());
        Map<String, String> deps = new LinkedHashMap<>();
        deps.put("parent", "^1.0.0");
        deps.put("shared", "2.0.0");

        List<PackageEdit> edits = diff(registry, manifest(deps));

        assertThat(editFor(edits, "parent", "1.0.0").getNestedUnder()).isNull();
        assertThat(editFor(edits, "shared", "2.0.0").getNestedUnder()).isNull();
        assertThat(editFor(edits, "shared", "1.0.0").getNestedUnder()).isEqualTo("parent");
    }

    @Test
    void transitiveOnlyFreshForkDefers() {
        // Neither b version is directly declared and the lock decided nothing, so the top slot is not decidable.
        FakeRegistry registry = new FakeRegistry()
                .add("a", "1.0.0", singletonMap("b", "^1.0.0"))
                .add("c", "1.0.0", singletonMap("b", "^2.0.0"))
                .add("b", "1.9.0", emptyMap())
                .add("b", "2.0.0", emptyMap());
        Map<String, String> deps = new LinkedHashMap<>();
        deps.put("a", "^1.0.0");
        deps.put("c", "^1.0.0");

        assertThatExceptionOfType(EngineFailure.class)
                .isThrownBy(() -> diff(registry, manifest(deps)))
                .withMessageContaining("neither version is directly declared");
    }

    @Test
    void lockDecidedForkNeedsNoEdits() {
        // The fork already sits in the lock exactly as resolved: the lock is ground truth, nothing re-places,
        // and the diff is empty.
        FakeRegistry registry = new FakeRegistry()
                .add("debug", "2.6.9", singletonMap("ms", "2.0.0"))
                .add("ms", "2.0.0", emptyMap())
                .add("ms", "2.1.3", emptyMap());
        Map<String, String> deps = new LinkedHashMap<>();
        deps.put("debug", "2.6.9");
        deps.put("ms", "2.1.3");
        String lock = "{\n" +
                "  \"lockfileVersion\": 1,\n" +
                "  \"configVersion\": 1,\n" +
                "  \"workspaces\": {\n" +
                "    \"\": {\n" +
                "      \"name\": \"t\",\n" +
                "      \"dependencies\": {\n" +
                "        \"debug\": \"2.6.9\",\n" +
                "        \"ms\": \"2.1.3\",\n" +
                "      },\n" +
                "    },\n" +
                "  },\n" +
                "  \"packages\": {\n" +
                "    \"debug\": [\"debug@2.6.9\", \"\", { \"dependencies\": { \"ms\": \"2.0.0\" } }, \"sha512-d\"],\n" +
                "\n" +
                "    \"ms\": [\"ms@2.1.3\", \"\", {}, \"sha512-m3\"],\n" +
                "\n" +
                "    \"debug/ms\": [\"ms@2.0.0\", \"\", {}, \"sha512-m0\"],\n" +
                "  }\n" +
                "}\n";

        ResolutionGraph graph = new NpmGraphBuilder(registry, false).build(singletonMap("", manifest(deps)));
        assertThat(BunLockDiff.diff(graph, lock)).isEmpty();
    }

    @Test
    void threeVersionForkDefers() {
        FakeRegistry registry = new FakeRegistry()
                .add("a", "1.0.0", singletonMap("b", "1.0.0"))
                .add("c", "1.0.0", singletonMap("b", "2.0.0"))
                .add("d", "1.0.0", singletonMap("b", "3.0.0"))
                .add("b", "1.0.0", emptyMap())
                .add("b", "2.0.0", emptyMap())
                .add("b", "3.0.0", emptyMap());
        Map<String, String> deps = new LinkedHashMap<>();
        deps.put("a", "^1.0.0");
        deps.put("c", "^1.0.0");
        deps.put("d", "^1.0.0");

        assertThatExceptionOfType(EngineFailure.class)
                .isThrownBy(() -> diff(registry, manifest(deps)))
                .withMessageContaining("3 versions");
    }

    @Test
    void multiRequirerNestedCopyDefers() {
        // Two packages require the losing fork version, so its single "parent/name" nest key is ambiguous.
        FakeRegistry registry = new FakeRegistry()
                .add("a", "1.0.0", singletonMap("shared", "1.0.0"))
                .add("c", "1.0.0", singletonMap("shared", "1.0.0"))
                .add("shared", "1.0.0", emptyMap())
                .add("shared", "2.0.0", emptyMap());
        Map<String, String> deps = new LinkedHashMap<>();
        deps.put("a", "^1.0.0");
        deps.put("c", "^1.0.0");
        deps.put("shared", "2.0.0");

        assertThatExceptionOfType(EngineFailure.class)
                .isThrownBy(() -> diff(registry, manifest(deps)))
                .withMessageContaining("multiple packages");
    }

    @Test
    void freshAliasEntryDefers() {
        FakeRegistry registry = new FakeRegistry().add("b", "1.0.0", emptyMap());

        assertThatExceptionOfType(EngineFailure.class)
                .isThrownBy(() -> diff(registry, manifest(singletonMap("myb", "npm:b@^1.0.0"))))
                .withMessageContaining("aliases");
    }

    @Test
    void binBearingManifestDefers() {
        // bun folds bin into the tuple metadata in a shape not byte-verified, so a fresh add carrying one defers.
        VersionManifest withBin = new VersionManifest("cli", "1.0.0", TextNode.valueOf("MIT"), "MIT",
                null, null, null, null, TextNode.valueOf("cli.js"), null, null, null, null, null, null, null, null,
                null, new VersionManifest.Dist("https://r/cli/-/cli-1.0.0.tgz", null, "sha512-cli"), null, null, null);
        ResolutionGraph graph = new ResolutionGraph(
                singletonList(new ResolutionGraph.Importer("", "t", "1.0.0",
                        singletonMap("dependencies", singletonMap("cli", "^1.0.0")), singletonMap("cli", "1.0.0"))),
                singletonMap("cli@1.0.0", new ResolvedNode(withBin, emptyMap())));

        assertThatExceptionOfType(EngineFailure.class)
                .isThrownBy(() -> BunLockDiff.diff(graph, ROOT_ONLY_LOCK))
                .withMessageContaining("bin");
    }

    @Test
    void optionalMetaWithoutPeerDeclarationDefers() {
        // A meta entry that marks a name optional without declaring it a peer is a shape bun has not been
        // byte-verified on, so the diff fails loud rather than guess whether it lands in optionalPeers.
        ObjectNode meta = JsonNodeFactory.instance.objectNode();
        meta.putObject("@types/react").put("optional", true);
        VersionManifest m = new VersionManifest("uses-peer", "1.0.0", null, null,
                null, null, singletonMap("react", ">=17"), meta, null, null, null, null, null, null, null, null, null,
                null, new VersionManifest.Dist("https://r/uses-peer/-/uses-peer-1.0.0.tgz", null, "sha512-peer"), null, null, null);
        ResolutionGraph graph = new ResolutionGraph(
                singletonList(new ResolutionGraph.Importer("", "t", "1.0.0",
                        singletonMap("dependencies", singletonMap("uses-peer", "^1.0.0")), singletonMap("uses-peer", "1.0.0"))),
                singletonMap("uses-peer@1.0.0", new ResolvedNode(m, emptyMap())));

        assertThatExceptionOfType(EngineFailure.class)
                .isThrownBy(() -> BunLockDiff.diff(graph, ROOT_ONLY_LOCK))
                .withMessageContaining("optional");
    }

    @Test
    void scopeMoveDefers() {
        // b moved from dependencies to devDependencies: the workspace mirror would have to move the member.
        FakeRegistry registry = new FakeRegistry().add("b", "1.0.0", emptyMap());
        String lock = "{\n" +
                "  \"lockfileVersion\": 1,\n" +
                "  \"configVersion\": 1,\n" +
                "  \"workspaces\": {\n" +
                "    \"\": {\n" +
                "      \"name\": \"t\",\n" +
                "      \"dependencies\": {\n" +
                "        \"b\": \"^1.0.0\",\n" +
                "      },\n" +
                "    },\n" +
                "  },\n" +
                "  \"packages\": {\n" +
                "    \"b\": [\"b@1.0.0\", \"\", {}, \"sha512-b\"],\n" +
                "  }\n" +
                "}\n";
        ResolutionGraph graph = new NpmGraphBuilder(registry, false).build(singletonMap("",
                "{\"name\":\"t\",\"version\":\"1.0.0\",\"devDependencies\":{\"b\":\"^1.0.0\"}}"));

        assertThatExceptionOfType(EngineFailure.class)
                .isThrownBy(() -> BunLockDiff.diff(graph, lock))
                .withMessageContaining("moved from dependencies to devDependencies");
    }

    @Test
    void multiImporterGraphDefers() {
        FakeRegistry registry = new FakeRegistry().add("b", "1.0.0", emptyMap());
        Map<String, String> importers = new LinkedHashMap<>();
        importers.put("", "{\"name\":\"t\",\"version\":\"1.0.0\"}");
        importers.put("packages/m", "{\"name\":\"m\",\"version\":\"1.0.0\",\"dependencies\":{\"b\":\"^1.0.0\"}}");
        ResolutionGraph graph = new NpmGraphBuilder(registry, false).build(importers);

        assertThatExceptionOfType(EngineFailure.class)
                .isThrownBy(() -> BunLockDiff.diff(graph, ROOT_ONLY_LOCK))
                .withMessageContaining("single root importer");
    }

    // --- helpers ------------------------------------------------------------

    private static List<PackageEdit> diff(Registry registry, String manifest) {
        ResolutionGraph graph = new NpmGraphBuilder(registry, false).build(singletonMap("", manifest));
        return BunLockDiff.diff(graph, ROOT_ONLY_LOCK);
    }

    private static String manifest(Map<String, String> dependencies) {
        StringBuilder deps = new StringBuilder();
        for (Map.Entry<String, String> e : dependencies.entrySet()) {
            if (deps.length() > 0) {
                deps.append(',');
            }
            deps.append('"').append(e.getKey()).append("\":\"").append(e.getValue()).append('"');
        }
        return "{\"name\":\"t\",\"version\":\"1.0.0\",\"dependencies\":{" + deps + "}}";
    }

    private static PackageEdit editFor(List<PackageEdit> edits, String name, String version) {
        return edits.stream()
                .filter(e -> name.equals(e.getName()) && version.equals(e.getNewVersion()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no edit for " + name + "@" + version + " in " + edits));
    }

    private static final class FakeRegistry implements Registry {
        final Map<String, Set<String>> versionsByName = new HashMap<>();
        final Map<String, VersionManifest> manifests = new HashMap<>();

        FakeRegistry add(String name, String version, Map<String, String> deps) {
            versionsByName.computeIfAbsent(name, k -> new TreeSet<>()).add(version);
            VersionManifest.Dist dist = new VersionManifest.Dist(
                    "https://registry.npmjs.org/" + name + "/-/" + name + "-" + version + ".tgz",
                    null, "sha512-" + name + version);
            manifests.put(name + "@" + version, new VersionManifest(name, version, null, null,
                    deps.isEmpty() ? null : deps, null, null, null, null, null, null, null, null, null,
                    null, null, null, null, dist, null, null, null));
            return this;
        }

        @Override
        public Set<String> versions(String name) {
            return versionsByName.getOrDefault(name, Collections.emptySet());
        }

        @Override
        public VersionManifest manifest(String name, String version) {
            return manifests.get(name + "@" + version);
        }
    }
}
