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

import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.internal.lock.LockEditSet.PackageEdit;
import org.openrewrite.javascript.internal.registry.VersionManifest;

import java.util.*;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for the graph-to-lock diff's placement decisions, driven by an in-memory {@link Registry}. The
 * end-to-end byte contract lives in {@code NpmResolveAndPatchLockRegenTest}; these pin the decisions themselves —
 * which fresh node hoists, which nests, what the lock already decided, and which shapes defer.
 */
class NpmLockDiffTest {

    private static final String ROOT_ONLY_LOCK = "{\n" +
            "  \"name\": \"t\",\n" +
            "  \"version\": \"1.0.0\",\n" +
            "  \"lockfileVersion\": 3,\n" +
            "  \"requires\": true,\n" +
            "  \"packages\": {\n" +
            "    \"\": {\n" +
            "      \"name\": \"t\",\n" +
            "      \"version\": \"1.0.0\"\n" +
            "    }\n" +
            "  }\n" +
            "}\n";

    @Test
    void freshTransitiveForkNestsUnderItsRequirer() {
        // a needs b@^1 and c needs b@^2, neither declared: the alphabetically-first requirer's version hoists
        // (npm's node_modules path order) and the other nests under its requirer.
        FakeRegistry registry = new FakeRegistry()
                .add("a", "1.0.0", singletonMap("b", "^1.0.0"))
                .add("c", "1.0.0", singletonMap("b", "^2.0.0"))
                .add("b", "1.9.0", emptyMap())
                .add("b", "2.0.0", emptyMap());

        List<PackageEdit> edits = diff(registry, "{\"name\":\"t\",\"version\":\"1.0.0\"," +
                "\"dependencies\":{\"a\":\"^1.0.0\",\"c\":\"^1.0.0\"}}");

        assertThat(editFor(edits, "b", "1.9.0").getNestedUnder()).isNull();
        assertThat(editFor(edits, "b", "2.0.0").getNestedUnder()).isEqualTo("c");
    }

    @Test
    void freshForkWinnerIndependentOfDeclarationOrder() {
        // The same closure declared in reverse order picks the same winner: placement follows npm's sorted
        // node_modules paths, not the manifest's declaration order.
        FakeRegistry registry = new FakeRegistry()
                .add("a", "1.0.0", singletonMap("b", "^1.0.0"))
                .add("c", "1.0.0", singletonMap("b", "^2.0.0"))
                .add("b", "1.9.0", emptyMap())
                .add("b", "2.0.0", emptyMap());

        List<PackageEdit> edits = diff(registry, "{\"name\":\"t\",\"version\":\"1.0.0\"," +
                "\"dependencies\":{\"c\":\"^1.0.0\",\"a\":\"^1.0.0\"}}");

        assertThat(editFor(edits, "b", "1.9.0").getNestedUnder()).isNull();
        assertThat(editFor(edits, "b", "2.0.0").getNestedUnder()).isEqualTo("c");
    }

    @Test
    void threeVersionFreshForkDefers() {
        FakeRegistry registry = new FakeRegistry()
                .add("a", "1.0.0", singletonMap("b", "1.0.0"))
                .add("c", "1.0.0", singletonMap("b", "2.0.0"))
                .add("d", "1.0.0", singletonMap("b", "3.0.0"))
                .add("b", "1.0.0", emptyMap())
                .add("b", "2.0.0", emptyMap())
                .add("b", "3.0.0", emptyMap());

        assertThatExceptionOfType(EngineFailure.class).isThrownBy(() ->
                diff(registry, "{\"name\":\"t\",\"version\":\"1.0.0\"," +
                        "\"dependencies\":{\"a\":\"^1.0.0\",\"c\":\"^1.0.0\",\"d\":\"^1.0.0\"}}"))
                .withMessageContaining("forks into 3 versions");
    }

    @Test
    void collationAmbiguousFreshForkRequirersDefer() {
        // The requirer names first differ at digits, where compareTo and npm's localeCompare may disagree; the
        // hoisted winner is not provable, so the fork defers.
        FakeRegistry registry = new FakeRegistry()
                .add("p1", "1.0.0", singletonMap("b", "^1.0.0"))
                .add("p2", "1.0.0", singletonMap("b", "^2.0.0"))
                .add("b", "1.9.0", emptyMap())
                .add("b", "2.0.0", emptyMap());

        assertThatExceptionOfType(EngineFailure.class).isThrownBy(() ->
                diff(registry, "{\"name\":\"t\",\"version\":\"1.0.0\"," +
                        "\"dependencies\":{\"p1\":\"^1.0.0\",\"p2\":\"^1.0.0\"}}"))
                .withMessageContaining("collation-ambiguous");
    }

    @Test
    void lockDecidedForkNeedsNoProofAndNoEdits() {
        // The fork already sits in the lock exactly as resolved: the lock is ground truth, nothing re-places,
        // and no requirer-order proof is needed — the diff is empty.
        FakeRegistry registry = new FakeRegistry()
                .add("a", "1.0.0", singletonMap("b", "^1.0.0"))
                .add("b", "1.0.0", emptyMap())
                .add("b", "2.0.0", emptyMap());
        String lock = "{\n" +
                "  \"name\": \"t\",\n" +
                "  \"version\": \"1.0.0\",\n" +
                "  \"lockfileVersion\": 3,\n" +
                "  \"requires\": true,\n" +
                "  \"packages\": {\n" +
                "    \"\": {\n" +
                "      \"name\": \"t\",\n" +
                "      \"version\": \"1.0.0\",\n" +
                "      \"dependencies\": {\n" +
                "        \"a\": \"^1.0.0\",\n" +
                "        \"b\": \"^2.0.0\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"node_modules/a\": {\"version\": \"1.0.0\"},\n" +
                "    \"node_modules/a/node_modules/b\": {\"version\": \"1.0.0\"},\n" +
                "    \"node_modules/b\": {\"version\": \"2.0.0\"}\n" +
                "  }\n" +
                "}\n";

        ResolutionGraph graph = new NpmGraphBuilder(registry).build(singletonMap("",
                "{\"name\":\"t\",\"version\":\"1.0.0\",\"dependencies\":{\"a\":\"^1.0.0\",\"b\":\"^2.0.0\"}}"));
        assertThat(NpmLockDiff.diff(graph, lock)).isEmpty();
    }

    @Test
    void freshForkMemberWithAutoInstalledPeerDefers() {
        // A fresh fork member's peer provider must be dependency-reached and top-level; an auto-installed
        // provider reshapes the layout, so the fork defers.
        FakeRegistry registry = new FakeRegistry()
                .add("a", "1.0.0", singletonMap("b", "^1.0.0"))
                .add("c", "1.0.0", singletonMap("b", "^2.0.0"))
                .add("b", "1.9.0", emptyMap())
                .add("react", "18.2.0", emptyMap());
        registry.addWithPeers("b", "2.0.0", emptyMap(), singletonMap("react", ">=17"));

        assertThatExceptionOfType(EngineFailure.class).isThrownBy(() ->
                diff(registry, "{\"name\":\"t\",\"version\":\"1.0.0\"," +
                        "\"dependencies\":{\"a\":\"^1.0.0\",\"c\":\"^1.0.0\"}}"))
                .withMessageContaining("auto-installed");
    }

    @Test
    void freshAliasEntryDefers() {
        // An in-place alias bump patches fine (the entry keeps its name field), but a brand-new alias entry
        // needs a serialization the patcher does not have yet.
        FakeRegistry registry = new FakeRegistry().add("b", "1.0.0", emptyMap());

        assertThatExceptionOfType(EngineFailure.class).isThrownBy(() ->
                diff(registry, "{\"name\":\"t\",\"version\":\"1.0.0\"," +
                        "\"dependencies\":{\"myb\":\"npm:b@^1.0.0\"}}"))
                .withMessageContaining("alias");
    }

    @Test
    void multiImporterGraphDefers() {
        FakeRegistry registry = new FakeRegistry().add("b", "1.0.0", emptyMap());
        Map<String, String> importers = new LinkedHashMap<>();
        importers.put("", "{\"name\":\"t\",\"version\":\"1.0.0\"}");
        importers.put("packages/m", "{\"name\":\"m\",\"version\":\"1.0.0\",\"dependencies\":{\"b\":\"^1.0.0\"}}");
        ResolutionGraph graph = new NpmGraphBuilder(registry).build(importers);

        assertThatExceptionOfType(EngineFailure.class).isThrownBy(() ->
                NpmLockDiff.diff(graph, ROOT_ONLY_LOCK))
                .withMessageContaining("single root importer");
    }

    // --- helpers ------------------------------------------------------------

    private static List<PackageEdit> diff(Registry registry, String manifest) {
        ResolutionGraph graph = new NpmGraphBuilder(registry, true).build(singletonMap("", manifest));
        return NpmLockDiff.diff(graph, ROOT_ONLY_LOCK);
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
            return put(name, version, deps, null);
        }

        FakeRegistry addWithPeers(String name, String version, Map<String, String> deps, Map<String, String> peers) {
            return put(name, version, deps, peers);
        }

        private FakeRegistry put(String name, String version, Map<String, String> deps,
                                 Map<String, String> peers) {
            versionsByName.computeIfAbsent(name, k -> new TreeSet<>()).add(version);
            VersionManifest.Dist dist = new VersionManifest.Dist(
                    "https://registry.npmjs.org/" + name + "/-/" + name + "-" + version + ".tgz",
                    null, "sha512-" + name + version);
            manifests.put(name + "@" + version, new VersionManifest(name, version, null, null,
                    deps.isEmpty() ? null : deps, null, peers, null, null, null, null, null, null, null,
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
