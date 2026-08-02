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

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Pure serializer tests: a {@link ResolutionGraph} built from an in-memory {@link Registry} (no network) is
 * written and byte-compared against the exact {@code package-lock.json} shape npm produces — the hoisted flat
 * closure, the lockfileVersion 2 legacy tree, and a directly-declared fork's nested placement — plus the
 * fail-loud boundaries.
 */
class NpmLockWriterTest {

    @Test
    void flatClosureV3() {
        FakeRegistry registry = new FakeRegistry()
                .add("a", "1.0.0", singletonMap("b", "^1.0.0"), "MIT", singletonMap("node", ">=8"))
                .add("b", "1.0.0", emptyMap(), "MIT", null);

        ResolutionGraph graph = new NpmGraphBuilder(registry)
                .build(singletonMap("", app(singletonMap("a", "^1.0.0"))));

        assertThat(new NpmLockWriter().write(graph, 3)).isEqualTo(
                "{\n" +
                "  \"name\": \"app\",\n" +
                "  \"version\": \"1.0.0\",\n" +
                "  \"lockfileVersion\": 3,\n" +
                "  \"requires\": true,\n" +
                "  \"packages\": {\n" +
                "    \"\": {\n" +
                "      \"name\": \"app\",\n" +
                "      \"version\": \"1.0.0\",\n" +
                "      \"dependencies\": {\n" +
                "        \"a\": \"^1.0.0\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"node_modules/a\": {\n" +
                "      \"version\": \"1.0.0\",\n" +
                "      \"resolved\": \"https://r/a/-/a-1.0.0.tgz\",\n" +
                "      \"integrity\": \"sha512-a-1.0.0\",\n" +
                "      \"license\": \"MIT\",\n" +
                "      \"dependencies\": {\n" +
                "        \"b\": \"^1.0.0\"\n" +
                "      },\n" +
                "      \"engines\": {\n" +
                "        \"node\": \">=8\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"node_modules/b\": {\n" +
                "      \"version\": \"1.0.0\",\n" +
                "      \"resolved\": \"https://r/b/-/b-1.0.0.tgz\",\n" +
                "      \"integrity\": \"sha512-b-1.0.0\",\n" +
                "      \"license\": \"MIT\"\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
    }

    @Test
    void flatClosureV2AddsLegacyTree() {
        FakeRegistry registry = new FakeRegistry()
                .add("a", "1.0.0", singletonMap("b", "^1.0.0"), "MIT", singletonMap("node", ">=8"))
                .add("b", "1.0.0", emptyMap(), "MIT", null);

        ResolutionGraph graph = new NpmGraphBuilder(registry)
                .build(singletonMap("", app(singletonMap("a", "^1.0.0"))));

        String lock = new NpmLockWriter().write(graph, 2);
        assertThat(lock).contains("\"lockfileVersion\": 2,");
        // The legacy `dependencies` tree follows `packages`: minimal entries by bare name, `requires` for deps.
        assertThat(lock).endsWith(
                "  \"dependencies\": {\n" +
                "    \"a\": {\n" +
                "      \"version\": \"1.0.0\",\n" +
                "      \"resolved\": \"https://r/a/-/a-1.0.0.tgz\",\n" +
                "      \"integrity\": \"sha512-a-1.0.0\",\n" +
                "      \"requires\": {\n" +
                "        \"b\": \"^1.0.0\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"b\": {\n" +
                "      \"version\": \"1.0.0\",\n" +
                "      \"resolved\": \"https://r/b/-/b-1.0.0.tgz\",\n" +
                "      \"integrity\": \"sha512-b-1.0.0\"\n" +
                "    }\n" +
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

        String lock = new NpmLockWriter().write(graph, 3);
        assertThat(lock).contains("\"node_modules/parent/node_modules/shared\": {\n" +
                "      \"version\": \"1.0.0\",");
        assertThat(lock).contains("\"node_modules/shared\": {\n" +
                "      \"version\": \"2.0.0\",");
        // parent's own dependency edge records the declared constraint verbatim.
        assertThat(lock).contains("\"node_modules/parent\": {\n" +
                "      \"version\": \"1.0.0\",\n" +
                "      \"resolved\": \"https://r/parent/-/parent-1.0.0.tgz\",\n" +
                "      \"integrity\": \"sha512-parent-1.0.0\",\n" +
                "      \"license\": \"MIT\",\n" +
                "      \"dependencies\": {\n" +
                "        \"shared\": \"1.0.0\"\n" +
                "      }\n" +
                "    }");
    }

    @Test
    void satisfiedPeerFlagsProviderAndRecordsMetaVerbatim() {
        FakeRegistry registry = new FakeRegistry()
                .add("framework", "1.0.0", emptyMap(), "MIT", null);
        // plugin declares a satisfied non-optional peer (framework, also a top-level dep) and an absent optional peer.
        ObjectNode meta = JsonNodeFactory.instance.objectNode();
        meta.putObject("types").put("optional", true);
        Map<String, String> peers = new LinkedHashMap<>();
        peers.put("framework", "^1.0.0");
        peers.put("types", ">=1.0.0");
        registry.versionsByName.computeIfAbsent("plugin", k -> new TreeSet<>()).add("1.0.0");
        registry.manifests.put("plugin@1.0.0", new VersionManifest("plugin", "1.0.0", TextNode.valueOf("MIT"), "MIT",
                null, null, peers, meta, null, null, null, null, null, null, null, null, null, null,
                new VersionManifest.Dist("https://r/plugin/-/plugin-1.0.0.tgz", null, "sha512-plugin-1.0.0"), null, null, null));

        Map<String, String> deps = new LinkedHashMap<>();
        deps.put("plugin", "^1.0.0");
        deps.put("framework", "^1.0.0");
        String lock = new NpmLockWriter().write(new NpmGraphBuilder(registry).build(singletonMap("", app(deps))), 3);

        // The provider is flagged `peer: true` even though it is also a top-level dependency.
        assertThat(lock).contains("\"node_modules/framework\": {\n" +
                "      \"version\": \"1.0.0\",\n" +
                "      \"resolved\": \"https://r/framework/-/framework-1.0.0.tgz\",\n" +
                "      \"integrity\": \"sha512-framework-1.0.0\",\n" +
                "      \"license\": \"MIT\",\n" +
                "      \"peer\": true\n" +
                "    }");
        // The declarer records peerDependencies and peerDependenciesMeta verbatim (objects, after the scalars).
        assertThat(lock).contains("\"node_modules/plugin\": {\n" +
                "      \"version\": \"1.0.0\",\n" +
                "      \"resolved\": \"https://r/plugin/-/plugin-1.0.0.tgz\",\n" +
                "      \"integrity\": \"sha512-plugin-1.0.0\",\n" +
                "      \"license\": \"MIT\",\n" +
                "      \"peerDependencies\": {\n" +
                "        \"framework\": \"^1.0.0\",\n" +
                "        \"types\": \">=1.0.0\"\n" +
                "      },\n" +
                "      \"peerDependenciesMeta\": {\n" +
                "        \"types\": {\n" +
                "          \"optional\": true\n" +
                "        }\n" +
                "      }\n" +
                "    }");
    }

    @Test
    void optionalPeerProviderNotFlagged() {
        // widget declares theme as an OPTIONAL peer; even though theme is present as a top-level dep, an optional
        // peer confers no `peer: true` on its provider (unlike the non-optional peer above).
        FakeRegistry registry = new FakeRegistry()
                .add("theme", "1.0.0", emptyMap(), "MIT", null);
        ObjectNode meta = JsonNodeFactory.instance.objectNode();
        meta.putObject("theme").put("optional", true);
        registry.versionsByName.computeIfAbsent("widget", k -> new TreeSet<>()).add("1.0.0");
        registry.manifests.put("widget@1.0.0", new VersionManifest("widget", "1.0.0", TextNode.valueOf("MIT"), "MIT",
                null, null, singletonMap("theme", "^1.0.0"), meta, null, null, null, null, null, null, null, null,
                null, null, new VersionManifest.Dist("https://r/widget/-/widget-1.0.0.tgz", null, "sha512-widget"),
                null, null, null));

        Map<String, String> deps = new LinkedHashMap<>();
        deps.put("widget", "^1.0.0");
        deps.put("theme", "^1.0.0");
        String lock = new NpmLockWriter().write(new NpmGraphBuilder(registry).build(singletonMap("", app(deps))), 3);

        assertThat(lock).contains("\"node_modules/theme\": {");
        assertThat(lock).doesNotContain("\"peer\": true");
    }

    @Test
    void binBearingManifestDefers() {
        VersionManifest withBin = new VersionManifest("cli", "1.0.0", TextNode.valueOf("MIT"), "MIT",
                null, null, null, null, TextNode.valueOf("cli.js"), null, null, null, null, null, null, null, null,
                null, new VersionManifest.Dist("https://r/cli/-/cli-1.0.0.tgz", null, "sha512-cli"), null, null, null);
        ResolutionGraph graph = new ResolutionGraph(
                Collections.singletonList(new ResolutionGraph.Importer("", "app", "1.0.0",
                        singletonMap("dependencies", singletonMap("cli", "^1.0.0")), singletonMap("cli", "1.0.0"))),
                singletonMap("cli@1.0.0", new ResolvedNode(withBin, emptyMap())));

        assertThatExceptionOfType(EngineFailure.class)
                .isThrownBy(() -> new NpmLockWriter().write(graph, 3));
    }

    @Test
    void workspaceImporterDefers() {
        ResolutionGraph graph = new ResolutionGraph(
                Collections.singletonList(new ResolutionGraph.Importer("packages/app", "app", "1.0.0",
                        emptyMap(), emptyMap())),
                emptyMap());
        assertThatExceptionOfType(EngineFailure.class)
                .isThrownBy(() -> new NpmLockWriter().write(graph, 3));
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
