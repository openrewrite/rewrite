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
package org.openrewrite.javascript.internal.lock.resolve;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.internal.lock.EngineFailure;
import org.openrewrite.javascript.internal.registry.VersionManifest;

import java.util.*;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Pure serializer tests: a {@link ResolutionGraph} built from an in-memory {@link Registry} (no network) is
 * written and byte-compared against the exact {@code bun.lock} (JSONC) shape bun produces — the hoisted flat
 * closure and a directly-declared fork's {@code "parent/name"} nested tuple — plus the fail-loud boundaries.
 * bun keeps engines/license out of its text tuple, so those manifest fields are intentionally dropped.
 */
class BunLockWriterTest {

    @Test
    void flatClosure() {
        FakeRegistry registry = new FakeRegistry()
                .add("a", "1.0.0", singletonMap("b", "^1.0.0"), "MIT", singletonMap("node", ">=8"))
                .add("b", "1.0.0", emptyMap(), "MIT", null);

        ResolutionGraph graph = new NpmGraphBuilder(registry)
                .build(singletonMap("", app(singletonMap("a", "^1.0.0"))));

        assertThat(new BunLockWriter().write(graph, 1, 1)).isEqualTo(
                "{\n" +
                "  \"lockfileVersion\": 1,\n" +
                "  \"configVersion\": 1,\n" +
                "  \"workspaces\": {\n" +
                "    \"\": {\n" +
                "      \"name\": \"app\",\n" +
                "      \"dependencies\": {\n" +
                "        \"a\": \"^1.0.0\",\n" +
                "      },\n" +
                "    },\n" +
                "  },\n" +
                "  \"packages\": {\n" +
                "    \"a\": [\"a@1.0.0\", \"\", { \"dependencies\": { \"b\": \"^1.0.0\" } }, \"sha512-a-1.0.0\"],\n" +
                "\n" +
                "    \"b\": [\"b@1.0.0\", \"\", {}, \"sha512-b-1.0.0\"],\n" +
                "  }\n" +
                "}\n");
    }

    @Test
    void directlyDeclaredForkNestsUnderParent() {
        // root directly depends on shared@2.0.0 (wins the top slot) and on parent, whose shared@1.0.0 nests.
        FakeRegistry registry = new FakeRegistry()
                .add("parent", "1.0.0", singletonMap("shared", "1.0.0"), "MIT", null)
                .add("shared", "1.0.0", emptyMap(), "MIT", null)
                .add("shared", "2.0.0", emptyMap(), "MIT", null);

        Map<String, String> deps = new LinkedHashMap<>();
        deps.put("parent", "^1.0.0");
        deps.put("shared", "2.0.0");
        ResolutionGraph graph = new NpmGraphBuilder(registry).build(singletonMap("", app(deps)));

        assertThat(new BunLockWriter().write(graph, 1, 1)).isEqualTo(
                "{\n" +
                "  \"lockfileVersion\": 1,\n" +
                "  \"configVersion\": 1,\n" +
                "  \"workspaces\": {\n" +
                "    \"\": {\n" +
                "      \"name\": \"app\",\n" +
                "      \"dependencies\": {\n" +
                "        \"parent\": \"^1.0.0\",\n" +
                "        \"shared\": \"2.0.0\",\n" +
                "      },\n" +
                "    },\n" +
                "  },\n" +
                "  \"packages\": {\n" +
                "    \"parent\": [\"parent@1.0.0\", \"\", { \"dependencies\": { \"shared\": \"1.0.0\" } }, \"sha512-parent-1.0.0\"],\n" +
                "\n" +
                "    \"shared\": [\"shared@2.0.0\", \"\", {}, \"sha512-shared-2.0.0\"],\n" +
                "\n" +
                "    \"parent/shared\": [\"shared@1.0.0\", \"\", {}, \"sha512-shared-1.0.0\"],\n" +
                "  }\n" +
                "}\n");
    }

    @Test
    void binBearingManifestDefers() {
        VersionManifest withBin = new VersionManifest("cli", "1.0.0", TextNode.valueOf("MIT"), "MIT",
                null, null, null, null, TextNode.valueOf("cli.js"), null, null, null, null, null, null, null, null,
                null, new VersionManifest.Dist("https://r/cli/-/cli-1.0.0.tgz", null, "sha512-cli"), null, null, null);
        ResolutionGraph graph = new ResolutionGraph(
                singletonList(new ResolutionGraph.Importer("", "app", "1.0.0",
                        singletonMap("dependencies", singletonMap("cli", "^1.0.0")), singletonMap("cli", "1.0.0"))),
                singletonMap("cli@1.0.0", new ResolvedNode(withBin, emptyMap())));

        assertThatExceptionOfType(EngineFailure.class)
                .isThrownBy(() -> new BunLockWriter().write(graph, 1, 1));
    }

    @Test
    void satisfiedPeerSurfaceEmitted() {
        // uses-peer declares a react peer met by the top-level react; bun copies the peer range verbatim into the
        // consumer's tuple metadata and leaves the provider's tuple untouched (no "peer" flag, unlike npm).
        VersionManifest usesPeer = new VersionManifest("uses-peer", "1.0.0", null, null,
                null, null, singletonMap("react", ">=17"), null, null, null, null, null, null, null, null, null, null,
                null, new VersionManifest.Dist("https://r/uses-peer/-/uses-peer-1.0.0.tgz", null, "sha512-peer"), null, null, null);
        VersionManifest react = new VersionManifest("react", "18.0.0", null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, new VersionManifest.Dist("https://r/react/-/react-18.0.0.tgz", null, "sha512-react"), null, null, null);
        Map<String, ResolvedNode> nodes = new LinkedHashMap<>();
        nodes.put("react@18.0.0", new ResolvedNode(react, emptyMap()));
        nodes.put("uses-peer@1.0.0", new ResolvedNode(usesPeer, emptyMap()));
        Map<String, String> declared = new LinkedHashMap<>();
        declared.put("react", "18.0.0");
        declared.put("uses-peer", "^1.0.0");
        Map<String, String> resolved = new LinkedHashMap<>();
        resolved.put("react", "18.0.0");
        resolved.put("uses-peer", "1.0.0");
        ResolutionGraph graph = new ResolutionGraph(
                singletonList(new ResolutionGraph.Importer("", "app", "1.0.0",
                        singletonMap("dependencies", declared), resolved)),
                nodes);

        assertThat(new BunLockWriter().write(graph, 1, 1)).isEqualTo(
                "{\n" +
                "  \"lockfileVersion\": 1,\n" +
                "  \"configVersion\": 1,\n" +
                "  \"workspaces\": {\n" +
                "    \"\": {\n" +
                "      \"name\": \"app\",\n" +
                "      \"dependencies\": {\n" +
                "        \"react\": \"18.0.0\",\n" +
                "        \"uses-peer\": \"^1.0.0\",\n" +
                "      },\n" +
                "    },\n" +
                "  },\n" +
                "  \"packages\": {\n" +
                "    \"react\": [\"react@18.0.0\", \"\", {}, \"sha512-react\"],\n" +
                "\n" +
                "    \"uses-peer\": [\"uses-peer@1.0.0\", \"\", { \"peerDependencies\": { \"react\": \">=17\" } }, \"sha512-peer\"],\n" +
                "  }\n" +
                "}\n");
    }

    @Test
    void peerMetaFlattensToOptionalPeersSorted() {
        // Mirrors bun's react-redux@9.2.0 shape: both the peerDependencies map and the optionalPeers array are
        // ASCII-sorted regardless of manifest order, and the meta object is flattened to its optional peer names.
        Map<String, String> peers = new LinkedHashMap<>();
        peers.put("react", "^18.0 || ^19");
        peers.put("redux", "^5.0.0");
        peers.put("@types/react", "^18.2.25 || ^19");
        ObjectNode meta = JsonNodeFactory.instance.objectNode();
        meta.putObject("redux").put("optional", true);
        meta.putObject("@types/react").put("optional", true);
        VersionManifest m = new VersionManifest("uses-peer", "1.0.0", null, null,
                singletonMap("use-sync-external-store", "^1.4.0"), null, peers, meta, null, null, null, null, null,
                null, null, null, null, null,
                new VersionManifest.Dist("https://r/uses-peer/-/uses-peer-1.0.0.tgz", null, "sha512-peer"), null, null, null);
        ResolutionGraph graph = new ResolutionGraph(
                singletonList(new ResolutionGraph.Importer("", "app", "1.0.0",
                        singletonMap("dependencies", singletonMap("uses-peer", "^1.0.0")), singletonMap("uses-peer", "1.0.0"))),
                singletonMap("uses-peer@1.0.0", new ResolvedNode(m, emptyMap())));

        assertThat(new BunLockWriter().write(graph, 1, 1)).isEqualTo(
                "{\n" +
                "  \"lockfileVersion\": 1,\n" +
                "  \"configVersion\": 1,\n" +
                "  \"workspaces\": {\n" +
                "    \"\": {\n" +
                "      \"name\": \"app\",\n" +
                "      \"dependencies\": {\n" +
                "        \"uses-peer\": \"^1.0.0\",\n" +
                "      },\n" +
                "    },\n" +
                "  },\n" +
                "  \"packages\": {\n" +
                "    \"uses-peer\": [\"uses-peer@1.0.0\", \"\", { \"dependencies\": { \"use-sync-external-store\": \"^1.4.0\" }, " +
                "\"peerDependencies\": { \"@types/react\": \"^18.2.25 || ^19\", \"react\": \"^18.0 || ^19\", \"redux\": \"^5.0.0\" }, " +
                "\"optionalPeers\": [\"@types/react\", \"redux\"] }, \"sha512-peer\"],\n" +
                "  }\n" +
                "}\n");
    }

    @Test
    void optionalMetaWithoutPeerDeclarationDefers() {
        // A meta entry that marks a name optional without declaring it a peer is a shape bun has not been verified
        // on, so the writer fails loud rather than guess whether it lands in optionalPeers.
        ObjectNode meta = JsonNodeFactory.instance.objectNode();
        meta.putObject("@types/react").put("optional", true);
        VersionManifest m = new VersionManifest("uses-peer", "1.0.0", null, null,
                null, null, singletonMap("react", ">=17"), meta, null, null, null, null, null, null, null, null, null,
                null, new VersionManifest.Dist("https://r/uses-peer/-/uses-peer-1.0.0.tgz", null, "sha512-peer"), null, null, null);
        ResolutionGraph graph = new ResolutionGraph(
                singletonList(new ResolutionGraph.Importer("", "app", "1.0.0",
                        singletonMap("dependencies", singletonMap("uses-peer", "^1.0.0")), singletonMap("uses-peer", "1.0.0"))),
                singletonMap("uses-peer@1.0.0", new ResolvedNode(m, emptyMap())));

        assertThatExceptionOfType(EngineFailure.class)
                .isThrownBy(() -> new BunLockWriter().write(graph, 1, 1));
    }

    @Test
    void workspaceImporterDefers() {
        ResolutionGraph graph = new ResolutionGraph(
                singletonList(new ResolutionGraph.Importer("packages/app", "app", "1.0.0", emptyMap(), emptyMap())),
                emptyMap());
        assertThatExceptionOfType(EngineFailure.class)
                .isThrownBy(() -> new BunLockWriter().write(graph, 1, 1));
    }

    @Test
    void nonBunLockfileVersionDefers() {
        ResolutionGraph graph = new ResolutionGraph(
                singletonList(new ResolutionGraph.Importer("", "app", "1.0.0",
                        singletonMap("dependencies", emptyMap()), emptyMap())),
                emptyMap());
        assertThatExceptionOfType(EngineFailure.class)
                .isThrownBy(() -> new BunLockWriter().write(graph, 2, 1));
    }

    // --- in-memory registry + manifest builders ---------------------------

    private static String app(Map<String, String> dependencies) {
        StringBuilder deps = new StringBuilder();
        for (Map.Entry<String, String> e : dependencies.entrySet()) {
            if (deps.length() > 0) {
                deps.append(',');
            }
            deps.append('"').append(e.getKey()).append("\":\"").append(e.getValue()).append('"');
        }
        return "{\"name\":\"app\",\"version\":\"1.0.0\",\"dependencies\":{" + deps + "}}";
    }

    private static final class FakeRegistry implements Registry {
        final Map<String, Set<String>> versionsByName = new HashMap<>();
        final Map<String, VersionManifest> manifests = new HashMap<>();

        FakeRegistry add(String name, String version, Map<String, String> deps,
                         @Nullable String license, @Nullable Map<String, String> engines) {
            versionsByName.computeIfAbsent(name, k -> new TreeSet<>()).add(version);
            JsonNode lic = license == null ? null : JsonNodeFactory.instance.textNode(license);
            VersionManifest.Dist dist = new VersionManifest.Dist(
                    "https://r/" + name + "/-/" + name + "-" + version + ".tgz", null, "sha512-" + name + "-" + version);
            manifests.put(name + "@" + version, new VersionManifest(name, version, lic, license,
                    deps.isEmpty() ? null : deps, null, null, null, null, engines, null, null, null, null, null,
                    null, null, null, dist, null, null, null));
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
