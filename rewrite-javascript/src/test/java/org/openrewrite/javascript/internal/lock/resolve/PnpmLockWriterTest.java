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
 * Pure serializer tests: a {@link ResolutionGraph} built from an in-memory {@link Registry} (no network) is written
 * and byte-compared against the exact {@code pnpm-lock.yaml} v9 shape pnpm produces — the flat content-addressed
 * closure and a directly-declared fork (both versions kept side by side, no nesting) — plus the fail-loud boundary.
 */
class PnpmLockWriterTest {

    @Test
    void flatClosure() {
        FakeRegistry registry = new FakeRegistry()
                .add("a", "1.0.0", singletonMap("b", "^1.0.0"), singletonMap("node", ">=8"))
                .add("b", "1.0.0", emptyMap(), null);

        ResolutionGraph graph = new NpmGraphBuilder(registry)
                .build(singletonMap("", app(singletonMap("a", "^1.0.0"))));

        assertThat(new PnpmLockWriter().write(graph)).isEqualTo(
                "lockfileVersion: '9.0'\n" +
                "\n" +
                "settings:\n" +
                "  autoInstallPeers: true\n" +
                "  excludeLinksFromLockfile: false\n" +
                "\n" +
                "importers:\n" +
                "\n" +
                "  .:\n" +
                "    dependencies:\n" +
                "      a:\n" +
                "        specifier: ^1.0.0\n" +
                "        version: 1.0.0\n" +
                "\n" +
                "packages:\n" +
                "\n" +
                "  a@1.0.0:\n" +
                "    resolution: {integrity: sha512-a-1.0.0}\n" +
                "    engines: {node: '>=8'}\n" +
                "\n" +
                "  b@1.0.0:\n" +
                "    resolution: {integrity: sha512-b-1.0.0}\n" +
                "\n" +
                "snapshots:\n" +
                "\n" +
                "  a@1.0.0:\n" +
                "    dependencies:\n" +
                "      b: 1.0.0\n" +
                "\n" +
                "  b@1.0.0: {}\n");
    }

    @Test
    void directlyDeclaredForkKeepsBothVersions() {
        // root directly depends on shared@2.0.0 and on parent, whose shared@1.0.0 is kept alongside — no nesting.
        FakeRegistry registry = new FakeRegistry()
                .add("parent", "1.0.0", singletonMap("shared", "1.0.0"), null)
                .add("shared", "1.0.0", emptyMap(), null)
                .add("shared", "2.0.0", emptyMap(), null);

        Map<String, String> deps = new LinkedHashMap<>();
        deps.put("parent", "^1.0.0");
        deps.put("shared", "2.0.0");
        ResolutionGraph graph = new NpmGraphBuilder(registry).build(singletonMap("", app(deps)));

        assertThat(new PnpmLockWriter().write(graph)).isEqualTo(
                "lockfileVersion: '9.0'\n" +
                "\n" +
                "settings:\n" +
                "  autoInstallPeers: true\n" +
                "  excludeLinksFromLockfile: false\n" +
                "\n" +
                "importers:\n" +
                "\n" +
                "  .:\n" +
                "    dependencies:\n" +
                "      parent:\n" +
                "        specifier: ^1.0.0\n" +
                "        version: 1.0.0\n" +
                "      shared:\n" +
                "        specifier: 2.0.0\n" +
                "        version: 2.0.0\n" +
                "\n" +
                "packages:\n" +
                "\n" +
                "  parent@1.0.0:\n" +
                "    resolution: {integrity: sha512-parent-1.0.0}\n" +
                "\n" +
                "  shared@1.0.0:\n" +
                "    resolution: {integrity: sha512-shared-1.0.0}\n" +
                "\n" +
                "  shared@2.0.0:\n" +
                "    resolution: {integrity: sha512-shared-2.0.0}\n" +
                "\n" +
                "snapshots:\n" +
                "\n" +
                "  parent@1.0.0:\n" +
                "    dependencies:\n" +
                "      shared: 1.0.0\n" +
                "\n" +
                "  shared@1.0.0: {}\n" +
                "\n" +
                "  shared@2.0.0: {}\n");
    }

    @Test
    void devAndOptionalScopesAndOptionalMarking() {
        // p (prod) and d (dev) carry no snapshot flag; o and its child oc (optional-reachable) get `optional: true` —
        // oc as a leaf, o after its dependencies block. The importer emits all three scopes in canonical order.
        FakeRegistry registry = new FakeRegistry()
                .add("p", "1.0.0", emptyMap(), null)
                .add("d", "1.0.0", emptyMap(), null)
                .add("o", "1.0.0", singletonMap("oc", "^1.0.0"), null)
                .add("oc", "1.0.0", emptyMap(), null);

        String root = "{\"name\":\"app\",\"version\":\"1.0.0\"," +
                "\"dependencies\":{\"p\":\"^1.0.0\"}," +
                "\"devDependencies\":{\"d\":\"^1.0.0\"}," +
                "\"optionalDependencies\":{\"o\":\"^1.0.0\"}}";
        ResolutionGraph graph = new NpmGraphBuilder(registry).build(singletonMap("", root));

        assertThat(new PnpmLockWriter().write(graph)).isEqualTo(
                "lockfileVersion: '9.0'\n" +
                "\n" +
                "settings:\n" +
                "  autoInstallPeers: true\n" +
                "  excludeLinksFromLockfile: false\n" +
                "\n" +
                "importers:\n" +
                "\n" +
                "  .:\n" +
                "    dependencies:\n" +
                "      p:\n" +
                "        specifier: ^1.0.0\n" +
                "        version: 1.0.0\n" +
                "    devDependencies:\n" +
                "      d:\n" +
                "        specifier: ^1.0.0\n" +
                "        version: 1.0.0\n" +
                "    optionalDependencies:\n" +
                "      o:\n" +
                "        specifier: ^1.0.0\n" +
                "        version: 1.0.0\n" +
                "\n" +
                "packages:\n" +
                "\n" +
                "  d@1.0.0:\n" +
                "    resolution: {integrity: sha512-d-1.0.0}\n" +
                "\n" +
                "  o@1.0.0:\n" +
                "    resolution: {integrity: sha512-o-1.0.0}\n" +
                "\n" +
                "  oc@1.0.0:\n" +
                "    resolution: {integrity: sha512-oc-1.0.0}\n" +
                "\n" +
                "  p@1.0.0:\n" +
                "    resolution: {integrity: sha512-p-1.0.0}\n" +
                "\n" +
                "snapshots:\n" +
                "\n" +
                "  d@1.0.0: {}\n" +
                "\n" +
                "  o@1.0.0:\n" +
                "    dependencies:\n" +
                "      oc: 1.0.0\n" +
                "    optional: true\n" +
                "\n" +
                "  oc@1.0.0:\n" +
                "    optional: true\n" +
                "\n" +
                "  p@1.0.0: {}\n");
    }

    @Test
    void satisfiedPeerMaterializesAsSnapshotDep() {
        // host peers prov, satisfied by the directly-declared prov: pnpm suffixes the consumer snapshot key with
        // (prov@1.0.0), materializes prov as a snapshot dependency, and records the raw peerDependencies verbatim.
        FakeRegistry registry = new FakeRegistry()
                .addPeer("host", "1.0.0", emptyMap(), singletonMap("prov", "^1.0.0"), null)
                .add("prov", "1.0.0", emptyMap(), null);

        Map<String, String> deps = new LinkedHashMap<>();
        deps.put("host", "^1.0.0");
        deps.put("prov", "^1.0.0");
        ResolutionGraph graph = new NpmGraphBuilder(registry).build(singletonMap("", app(deps)));

        assertThat(new PnpmLockWriter().write(graph)).isEqualTo(
                "lockfileVersion: '9.0'\n" +
                "\n" +
                "settings:\n" +
                "  autoInstallPeers: true\n" +
                "  excludeLinksFromLockfile: false\n" +
                "\n" +
                "importers:\n" +
                "\n" +
                "  .:\n" +
                "    dependencies:\n" +
                "      host:\n" +
                "        specifier: ^1.0.0\n" +
                "        version: 1.0.0(prov@1.0.0)\n" +
                "      prov:\n" +
                "        specifier: ^1.0.0\n" +
                "        version: 1.0.0\n" +
                "\n" +
                "packages:\n" +
                "\n" +
                "  host@1.0.0:\n" +
                "    resolution: {integrity: sha512-host-1.0.0}\n" +
                "    peerDependencies:\n" +
                "      prov: ^1.0.0\n" +
                "\n" +
                "  prov@1.0.0:\n" +
                "    resolution: {integrity: sha512-prov-1.0.0}\n" +
                "\n" +
                "snapshots:\n" +
                "\n" +
                "  host@1.0.0(prov@1.0.0):\n" +
                "    dependencies:\n" +
                "      prov: 1.0.0\n" +
                "\n" +
                "  prov@1.0.0: {}\n");
    }

    @Test
    void nestedPeerSuffixOrdersByReference() {
        // top peers base + mid, and mid itself peers base: the mid reference nests (base@1.0.0) and the two peer
        // suffixes on top are ordered by their rendered reference. top's regular dep lib merges into the snapshot deps.
        FakeRegistry registry = new FakeRegistry()
                .add("base", "1.0.0", emptyMap(), null)
                .add("lib", "1.0.0", emptyMap(), null)
                .addPeer("mid", "1.0.0", emptyMap(), singletonMap("base", "^1.0.0"), null)
                // peers passed unsorted (mid before base) to prove the writer sorts the peerDependencies block
                .addPeer("top", "1.0.0", singletonMap("lib", "^1.0.0"), peers("mid", "^1.0.0", "base", "^1.0.0"), null);

        Map<String, String> deps = new LinkedHashMap<>();
        deps.put("base", "^1.0.0");
        deps.put("mid", "^1.0.0");
        deps.put("top", "^1.0.0");
        ResolutionGraph graph = new NpmGraphBuilder(registry).build(singletonMap("", app(deps)));

        assertThat(new PnpmLockWriter().write(graph)).isEqualTo(
                "lockfileVersion: '9.0'\n" +
                "\n" +
                "settings:\n" +
                "  autoInstallPeers: true\n" +
                "  excludeLinksFromLockfile: false\n" +
                "\n" +
                "importers:\n" +
                "\n" +
                "  .:\n" +
                "    dependencies:\n" +
                "      base:\n" +
                "        specifier: ^1.0.0\n" +
                "        version: 1.0.0\n" +
                "      mid:\n" +
                "        specifier: ^1.0.0\n" +
                "        version: 1.0.0(base@1.0.0)\n" +
                "      top:\n" +
                "        specifier: ^1.0.0\n" +
                "        version: 1.0.0(base@1.0.0)(mid@1.0.0(base@1.0.0))\n" +
                "\n" +
                "packages:\n" +
                "\n" +
                "  base@1.0.0:\n" +
                "    resolution: {integrity: sha512-base-1.0.0}\n" +
                "\n" +
                "  lib@1.0.0:\n" +
                "    resolution: {integrity: sha512-lib-1.0.0}\n" +
                "\n" +
                "  mid@1.0.0:\n" +
                "    resolution: {integrity: sha512-mid-1.0.0}\n" +
                "    peerDependencies:\n" +
                "      base: ^1.0.0\n" +
                "\n" +
                "  top@1.0.0:\n" +
                "    resolution: {integrity: sha512-top-1.0.0}\n" +
                "    peerDependencies:\n" +
                "      base: ^1.0.0\n" +
                "      mid: ^1.0.0\n" +
                "\n" +
                "snapshots:\n" +
                "\n" +
                "  base@1.0.0: {}\n" +
                "\n" +
                "  lib@1.0.0: {}\n" +
                "\n" +
                "  mid@1.0.0(base@1.0.0):\n" +
                "    dependencies:\n" +
                "      base: 1.0.0\n" +
                "\n" +
                "  top@1.0.0(base@1.0.0)(mid@1.0.0(base@1.0.0)):\n" +
                "    dependencies:\n" +
                "      base: 1.0.0\n" +
                "      lib: 1.0.0\n" +
                "      mid: 1.0.0(base@1.0.0)\n");
    }

    @Test
    void transitivePeerDefers() {
        // wrap depends on consumer, which peers base; wrap does not declare base — pnpm would record base under
        // wrap's transitivePeerDependencies, which the writer does not yet reproduce, so it defers.
        FakeRegistry registry = new FakeRegistry()
                .add("base", "1.0.0", emptyMap(), null)
                .addPeer("consumer", "1.0.0", emptyMap(), singletonMap("base", "^1.0.0"), null)
                .add("wrap", "1.0.0", singletonMap("consumer", "^1.0.0"), null);

        Map<String, String> deps = new LinkedHashMap<>();
        deps.put("base", "^1.0.0");
        deps.put("wrap", "^1.0.0");
        ResolutionGraph graph = new NpmGraphBuilder(registry).build(singletonMap("", app(deps)));

        assertThatExceptionOfType(EngineFailure.class)
                .isThrownBy(() -> new PnpmLockWriter().write(graph));
    }

    @Test
    void optionalPeerMetaDefers() {
        // host's peer prov is marked optional; a present optional peer materializes under a separate snapshot
        // optionalDependencies block (and absent ones can leak transitively), so the meta surface defers.
        JsonNode meta = JsonNodeFactory.instance.objectNode()
                .set("prov", JsonNodeFactory.instance.objectNode().put("optional", true));
        FakeRegistry registry = new FakeRegistry()
                .addPeer("host", "1.0.0", emptyMap(), singletonMap("prov", "^1.0.0"), meta)
                .add("prov", "1.0.0", emptyMap(), null);

        Map<String, String> deps = new LinkedHashMap<>();
        deps.put("host", "^1.0.0");
        deps.put("prov", "^1.0.0");
        ResolutionGraph graph = new NpmGraphBuilder(registry).build(singletonMap("", app(deps)));

        assertThatExceptionOfType(EngineFailure.class)
                .isThrownBy(() -> new PnpmLockWriter().write(graph));
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
                .isThrownBy(() -> new PnpmLockWriter().write(graph));
    }

    @Test
    void workspaceImporterDefers() {
        ResolutionGraph graph = new ResolutionGraph(
                Collections.singletonList(new ResolutionGraph.Importer("packages/app", "app", "1.0.0",
                        emptyMap(), emptyMap())),
                emptyMap());
        assertThatExceptionOfType(EngineFailure.class)
                .isThrownBy(() -> new PnpmLockWriter().write(graph));
    }

    // --- in-memory registry + manifest builders ---------------------------

    private static Map<String, String> peers(String n1, String r1, String n2, String r2) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(n1, r1);
        map.put(n2, r2);
        return map;
    }

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

        FakeRegistry add(String name, String version, Map<String, String> deps, @Nullable Map<String, String> engines) {
            versionsByName.computeIfAbsent(name, k -> new TreeSet<>()).add(version);
            JsonNode lic = JsonNodeFactory.instance.textNode("MIT");
            VersionManifest.Dist dist = new VersionManifest.Dist(
                    "https://r/" + name + "/-/" + name + "-" + version + ".tgz", null, "sha512-" + name + "-" + version);
            manifests.put(name + "@" + version, new VersionManifest(name, version, lic, "MIT",
                    deps.isEmpty() ? null : deps, null, null, null, null, engines, null, null, null, null, null,
                    null, null, null, dist, null, null, null));
            return this;
        }

        FakeRegistry addPeer(String name, String version, Map<String, String> deps,
                             Map<String, String> peers, @Nullable JsonNode peerMeta) {
            versionsByName.computeIfAbsent(name, k -> new TreeSet<>()).add(version);
            JsonNode lic = JsonNodeFactory.instance.textNode("MIT");
            VersionManifest.Dist dist = new VersionManifest.Dist(
                    "https://r/" + name + "/-/" + name + "-" + version + ".tgz", null, "sha512-" + name + "-" + version);
            manifests.put(name + "@" + version, new VersionManifest(name, version, lic, "MIT",
                    deps.isEmpty() ? null : deps, null, peers, peerMeta, null, null, null, null, null, null,
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
