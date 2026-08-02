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

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.internal.lock.EngineFailure;
import org.openrewrite.javascript.internal.registry.VersionManifest;

import java.util.*;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for the npm clean-closure graph builder, driven by an in-memory {@link Registry} (no network).
 */
class NpmGraphBuilderTest {

    @Test
    void resolvesACleanTwoLevelClosure() {
        FakeRegistry registry = new FakeRegistry()
                .add("a", "1.0.0", singletonMap("b", "^2.0.0"))
                .add("a", "1.2.0", singletonMap("b", "^2.0.0"))
                .add("b", "2.1.0", emptyMap())
                .add("b", "2.3.0", emptyMap());

        ResolutionGraph graph = new NpmGraphBuilder(registry)
                .build(singletonMap("", "{\"dependencies\":{\"a\":\"^1.0.0\"}}"));

        // a resolves to the highest satisfying ^1.0.0 (1.2.0); its edge to b resolves to the highest ^2.0.0 (2.3.0)
        assertThat(graph.getNodes()).containsOnlyKeys("a@1.2.0", "b@2.3.0");
        assertThat(graph.node("a", "1.2.0").getResolvedEdges()).containsExactly(Map.entry("b", "2.3.0"));
        assertThat(graph.node("b", "2.3.0").getResolvedEdges()).isEmpty();

        assertThat(graph.getImporters()).singleElement().satisfies(imp -> {
            assertThat(imp.getDir()).isEmpty();
            assertThat(imp.getResolved()).containsExactly(Map.entry("a", "1.2.0"));
        });
    }

    @Test
    void dedupesASharedTransitiveToOneVersion() {
        FakeRegistry registry = new FakeRegistry()
                .add("a", "1.0.0", singletonMap("shared", "^1.0.0"))
                .add("c", "1.0.0", singletonMap("shared", "^1.2.0"))
                .add("shared", "1.5.0", emptyMap());

        ResolutionGraph graph = new NpmGraphBuilder(registry)
                .build(singletonMap("", "{\"dependencies\":{\"a\":\"^1.0.0\",\"c\":\"^1.0.0\"}}"));

        assertThat(graph.getNodes()).containsOnlyKeys("a@1.0.0", "c@1.0.0", "shared@1.5.0");
    }

    @Test
    void resolvesATransitiveForkToTwoVersions() {
        // a needs b@^1 and c needs b@^2 (disjoint), neither b directly declared: npm keeps both versions (a
        // transitive fork). The builder no longer defers — it carries the fork for the writer to place.
        FakeRegistry registry = new FakeRegistry()
                .add("a", "1.0.0", singletonMap("b", "^1.0.0"))
                .add("c", "1.0.0", singletonMap("b", "^2.0.0"))
                .add("b", "1.9.0", emptyMap())
                .add("b", "2.0.0", emptyMap());

        ResolutionGraph graph = new NpmGraphBuilder(registry)
                .build(singletonMap("", "{\"dependencies\":{\"a\":\"^1.0.0\",\"c\":\"^1.0.0\"}}"));

        assertThat(graph.getNodes()).containsOnlyKeys("a@1.0.0", "c@1.0.0", "b@1.9.0", "b@2.0.0");
        assertThat(graph.node("a", "1.0.0").getResolvedEdges()).containsExactly(Map.entry("b", "1.9.0"));
        assertThat(graph.node("c", "1.0.0").getResolvedEdges()).containsExactly(Map.entry("b", "2.0.0"));
    }

    @Test
    void autoInstallsMissingLeafPeerTopLevel() {
        // react is a published leaf; has-peer peer-depends on it and root does not, so npm auto-installs react.
        FakeRegistry registry = new FakeRegistry().add("react", "18.2.0", emptyMap());
        registry.versionsByName.computeIfAbsent("has-peer", k -> new TreeSet<>()).add("1.0.0");
        registry.manifests.put("has-peer@1.0.0", vm("has-peer", "1.0.0", emptyMap(), singletonMap("react", ">=17")));

        ResolutionGraph graph = new NpmGraphBuilder(registry, true)
                .build(singletonMap("", "{\"dependencies\":{\"has-peer\":\"^1.0.0\"}}"));

        assertThat(graph.getNodes()).containsOnlyKeys("has-peer@1.0.0", "react@18.2.0");
        // The auto-installed peer carries no dev/optional flag in an all-prod closure.
        assertThat(graph.node("react", "18.2.0").isDev()).isFalse();
        assertThat(graph.node("react", "18.2.0").isOptional()).isFalse();
        assertThat(graph.node("react", "18.2.0").isDevOptional()).isFalse();
    }

    @Test
    void missingNonOptionalPeerDefersWhenAutoInstallDisabled() {
        // The shared builder (pnpm/bun/yarn) keeps the classic deferral: a missing non-optional peer fails loud.
        FakeRegistry registry = new FakeRegistry().add("react", "18.2.0", emptyMap());
        registry.versionsByName.computeIfAbsent("has-peer", k -> new TreeSet<>()).add("1.0.0");
        registry.manifests.put("has-peer@1.0.0", vm("has-peer", "1.0.0", emptyMap(), singletonMap("react", ">=17")));

        assertThatExceptionOfType(EngineFailure.class).isThrownBy(() ->
                new NpmGraphBuilder(registry).build(singletonMap("", "{\"dependencies\":{\"has-peer\":\"^1.0.0\"}}")));
    }

    @Test
    void peerAutoInstallWithNoSatisfyingVersionDefers() {
        // has-peer needs react, but the registry publishes no react version to auto-install — defer.
        FakeRegistry registry = new FakeRegistry();
        registry.versionsByName.computeIfAbsent("has-peer", k -> new TreeSet<>()).add("1.0.0");
        registry.manifests.put("has-peer@1.0.0", vm("has-peer", "1.0.0", emptyMap(), singletonMap("react", ">=17")));

        assertThatExceptionOfType(EngineFailure.class).isThrownBy(() ->
                new NpmGraphBuilder(registry, true).build(singletonMap("", "{\"dependencies\":{\"has-peer\":\"^1.0.0\"}}")));
    }

    @Test
    void autoInstalledPeerWithOwnDepsDefers() {
        // the missing peer would itself pull a dependency; only a pure-leaf auto-install is reproduced.
        FakeRegistry registry = new FakeRegistry()
                .add("react", "18.2.0", singletonMap("loose-envify", "^1.0.0"))
                .add("loose-envify", "1.4.0", emptyMap());
        registry.versionsByName.computeIfAbsent("has-peer", k -> new TreeSet<>()).add("1.0.0");
        registry.manifests.put("has-peer@1.0.0", vm("has-peer", "1.0.0", emptyMap(), singletonMap("react", ">=17")));

        assertThatExceptionOfType(EngineFailure.class).isThrownBy(() ->
                new NpmGraphBuilder(registry, true).build(singletonMap("", "{\"dependencies\":{\"has-peer\":\"^1.0.0\"}}")));
    }

    @Test
    void peerAutoInstallIntoDevClosureDefers() {
        // has-peer is a dev dependency, so its auto-installed peer would inherit dev-ness; a non-prod closure defers.
        FakeRegistry registry = new FakeRegistry().add("react", "18.2.0", emptyMap());
        registry.versionsByName.computeIfAbsent("has-peer", k -> new TreeSet<>()).add("1.0.0");
        registry.manifests.put("has-peer@1.0.0", vm("has-peer", "1.0.0", emptyMap(), singletonMap("react", ">=17")));

        assertThatExceptionOfType(EngineFailure.class).isThrownBy(() ->
                new NpmGraphBuilder(registry, true).build(singletonMap("", "{\"devDependencies\":{\"has-peer\":\"^1.0.0\"}}")));
    }

    @Test
    void satisfiedPeerResolvesWithNoExtraNode() {
        // react (a top-level dep) satisfies has-peer's react peer; the peer is a constraint already met.
        FakeRegistry registry = new FakeRegistry().add("react", "18.2.0", emptyMap());
        registry.versionsByName.computeIfAbsent("has-peer", k -> new TreeSet<>()).add("1.0.0");
        registry.manifests.put("has-peer@1.0.0", vm("has-peer", "1.0.0", emptyMap(), singletonMap("react", ">=17")));

        ResolutionGraph graph = new NpmGraphBuilder(registry)
                .build(singletonMap("", "{\"dependencies\":{\"has-peer\":\"^1.0.0\",\"react\":\"^18.0.0\"}}"));

        assertThat(graph.getNodes()).containsOnlyKeys("has-peer@1.0.0", "react@18.2.0");
    }

    @Test
    void autoInstallsRootPeerTopLevel() {
        // The root is a library declaring peerDependencies.react (a leaf) with no other deps; npm auto-installs it.
        FakeRegistry registry = new FakeRegistry().add("react", "18.2.0", emptyMap());

        ResolutionGraph graph = new NpmGraphBuilder(registry, true)
                .build(singletonMap("", "{\"name\":\"lib\",\"peerDependencies\":{\"react\":\">=17\"}}"));

        assertThat(graph.getNodes()).containsOnlyKeys("react@18.2.0");
        // The auto-installed root peer carries no dev/optional flag, and the root entry mirrors the scope verbatim.
        assertThat(graph.node("react", "18.2.0").isDev()).isFalse();
        assertThat(graph.node("react", "18.2.0").isOptional()).isFalse();
        assertThat(graph.getImporters()).singleElement().satisfies(imp ->
                assertThat(imp.getDeclared()).containsExactly(Map.entry("peerDependencies", singletonMap("react", ">=17"))));
    }

    @Test
    void rootPeerDefersWhenAutoInstallDisabled() {
        // The shared builder (pnpm/bun/yarn) keeps the classic deferral even for a library root's own peer.
        FakeRegistry registry = new FakeRegistry().add("react", "18.2.0", emptyMap());

        assertThatExceptionOfType(EngineFailure.class).isThrownBy(() ->
                new NpmGraphBuilder(registry).build(singletonMap("", "{\"peerDependencies\":{\"react\":\">=17\"}}")));
    }

    @Test
    void rootPeerIntoDevClosureDefers() {
        // A root peer alongside a devDependency is not an all-prod closure, so the auto-install defers.
        FakeRegistry registry = new FakeRegistry().add("react", "18.2.0", emptyMap()).add("dev", "1.0.0", emptyMap());

        assertThatExceptionOfType(EngineFailure.class).isThrownBy(() -> new NpmGraphBuilder(registry, true)
                .build(singletonMap("", "{\"devDependencies\":{\"dev\":\"^1.0.0\"},\"peerDependencies\":{\"react\":\">=17\"}}")));
    }

    @Test
    void satisfiedRootPeerResolvesWithNoExtraNode() {
        // The root declares react both as a dependency and a peer; the peer is a constraint already met (no node added).
        FakeRegistry registry = new FakeRegistry().add("react", "18.2.0", emptyMap());

        ResolutionGraph graph = new NpmGraphBuilder(registry, true).build(singletonMap("",
                "{\"dependencies\":{\"react\":\"^18.0.0\"},\"peerDependencies\":{\"react\":\">=17\"}}"));

        assertThat(graph.getNodes()).containsOnlyKeys("react@18.2.0");
    }

    @Test
    void optionalPeerAbsentResolves() {
        // has-peer's react peer is marked optional, so its absence is satisfied — the closure still resolves.
        FakeRegistry registry = new FakeRegistry();
        registry.versionsByName.computeIfAbsent("has-peer", k -> new TreeSet<>()).add("1.0.0");
        registry.manifests.put("has-peer@1.0.0", vmWithOptionalPeer("has-peer", "1.0.0", "react", ">=17"));

        ResolutionGraph graph = new NpmGraphBuilder(registry)
                .build(singletonMap("", "{\"dependencies\":{\"has-peer\":\"^1.0.0\"}}"));

        assertThat(graph.getNodes()).containsOnlyKeys("has-peer@1.0.0");
    }

    @Test
    void classifiesDevAndOptionalReachability() {
        FakeRegistry registry = new FakeRegistry()
                .add("prod", "1.0.0", singletonMap("pt", "^1.0.0"))
                .add("pt", "1.0.0", emptyMap())
                .add("dv", "1.0.0", singletonMap("dt", "^1.0.0"))
                .add("dt", "1.0.0", emptyMap())
                .add("opt", "1.0.0", singletonMap("ot", "^1.0.0"))
                .add("ot", "1.0.0", emptyMap());

        ResolutionGraph graph = new NpmGraphBuilder(registry).build(singletonMap("",
                "{\"dependencies\":{\"prod\":\"^1.0.0\"},\"devDependencies\":{\"dv\":\"^1.0.0\"}," +
                        "\"optionalDependencies\":{\"opt\":\"^1.0.0\"}}"));

        // prod and its transitive carry no flag; the dev subtree is dev; the optional subtree is optional.
        assertThat(graph.node("prod", "1.0.0").isDev()).isFalse();
        assertThat(graph.node("pt", "1.0.0").isOptional()).isFalse();
        assertThat(graph.node("dv", "1.0.0").isDev()).isTrue();
        assertThat(graph.node("dt", "1.0.0").isDev()).isTrue();
        assertThat(graph.node("dt", "1.0.0").isOptional()).isFalse();
        assertThat(graph.node("opt", "1.0.0").isOptional()).isTrue();
        assertThat(graph.node("ot", "1.0.0").isOptional()).isTrue();
        assertThat(graph.node("ot", "1.0.0").isDev()).isFalse();
    }

    @Test
    void classifiesDevOptionalOverlapAsDevOptional() {
        // shared is a dev dependency and also reached through an optional dependency: neither purely dev nor
        // purely optional, so npm marks it devOptional.
        FakeRegistry registry = new FakeRegistry()
                .add("shared", "1.0.0", emptyMap())
                .add("opt", "1.0.0", singletonMap("shared", "^1.0.0"));

        ResolutionGraph graph = new NpmGraphBuilder(registry).build(singletonMap("",
                "{\"devDependencies\":{\"shared\":\"^1.0.0\"},\"optionalDependencies\":{\"opt\":\"^1.0.0\"}}"));

        assertThat(graph.node("shared", "1.0.0").isDev()).isFalse();
        assertThat(graph.node("shared", "1.0.0").isOptional()).isFalse();
        assertThat(graph.node("shared", "1.0.0").isDevOptional()).isTrue();
        assertThat(graph.node("opt", "1.0.0").isOptional()).isTrue();
    }

    @Test
    void resolvesTransitiveOptionalDependencies() {
        // a prod package declaring optionalDependencies resolves and places them; the optional transitive is
        // optional-flagged, the prod one is not.
        FakeRegistry registry = new FakeRegistry()
                .addWithOptional("pkg", "1.0.0", singletonMap("dep", "^1.0.0"), singletonMap("native", "^1.0.0"))
                .add("dep", "1.0.0", emptyMap())
                .add("native", "1.0.0", emptyMap());

        ResolutionGraph graph = new NpmGraphBuilder(registry)
                .build(singletonMap("", "{\"dependencies\":{\"pkg\":\"^1.0.0\"}}"));

        assertThat(graph.getNodes()).containsKeys("pkg@1.0.0", "dep@1.0.0", "native@1.0.0");
        assertThat(graph.node("native", "1.0.0").isOptional()).isTrue();
        assertThat(graph.node("dep", "1.0.0").isOptional()).isFalse();
    }

    @Test
    void resolvesAnImporterAliasToRealPackage() {
        // react-is-18 aliases the real react-is; the node is keyed by the alias name but carries react-is's manifest.
        FakeRegistry registry = new FakeRegistry().add("react-is", "18.3.1", emptyMap());

        ResolutionGraph graph = new NpmGraphBuilder(registry)
                .build(singletonMap("", "{\"dependencies\":{\"react-is-18\":\"npm:react-is@^18.3.1\"}}"));

        assertThat(graph.getNodes()).containsOnlyKeys("react-is-18@18.3.1");
        assertThat(graph.node("react-is-18", "18.3.1").getName()).isEqualTo("react-is");
        assertThat(graph.getImporters()).singleElement().satisfies(imp ->
                assertThat(imp.getResolved()).containsExactly(Map.entry("react-is-18", "18.3.1")));
    }

    @Test
    void resolvesAnAliasCleanSubClosureUnderRealNames() {
        // mydebug aliases debug; debug's own dependency ms resolves under its own (real) name and identity.
        FakeRegistry registry = new FakeRegistry()
                .add("debug", "2.6.9", singletonMap("ms", "2.0.0"))
                .add("ms", "2.0.0", emptyMap());

        ResolutionGraph graph = new NpmGraphBuilder(registry)
                .build(singletonMap("", "{\"dependencies\":{\"mydebug\":\"npm:debug@2.6.9\"}}"));

        assertThat(graph.getNodes()).containsOnlyKeys("mydebug@2.6.9", "ms@2.0.0");
        assertThat(graph.node("mydebug", "2.6.9").getName()).isEqualTo("debug");
        assertThat(graph.node("mydebug", "2.6.9").getResolvedEdges()).containsExactly(Map.entry("ms", "2.0.0"));
    }

    @Test
    void defersAliasForkingWithUnaliasedCopy() {
        // react-is is both a normal dependency and an alias target: the alias would fork a non-aliased copy — defer.
        FakeRegistry registry = new FakeRegistry().add("react-is", "18.3.1", emptyMap());

        assertThatExceptionOfType(EngineFailure.class).isThrownBy(() ->
                new NpmGraphBuilder(registry).build(singletonMap("",
                        "{\"dependencies\":{\"react-is\":\"^18.3.1\",\"react-is-18\":\"npm:react-is@^18.3.1\"}}")));
    }

    @Test
    void defersAliasWithNonRegistryTarget() {
        // A git/file/url alias target is not a registry range; defer rather than treat it as one.
        FakeRegistry registry = new FakeRegistry();

        assertThatExceptionOfType(EngineFailure.class).isThrownBy(() ->
                new NpmGraphBuilder(registry).build(singletonMap("",
                        "{\"dependencies\":{\"dep\":\"npm:foo@github:owner/foo\"}}")));
    }

    // --- in-memory registry -------------------------------------------------

    private static final class FakeRegistry implements Registry {
        final Map<String, Set<String>> versionsByName = new HashMap<>();
        final Map<String, VersionManifest> manifests = new HashMap<>();

        FakeRegistry add(String name, String version, Map<String, String> deps) {
            versionsByName.computeIfAbsent(name, k -> new TreeSet<>()).add(version);
            manifests.put(name + "@" + version, vm(name, version, deps, null));
            return this;
        }

        FakeRegistry addWithOptional(String name, String version, Map<String, String> deps,
                                     Map<String, String> optionalDeps) {
            versionsByName.computeIfAbsent(name, k -> new TreeSet<>()).add(version);
            manifests.put(name + "@" + version, new VersionManifest(name, version, null, null,
                    deps.isEmpty() ? null : deps, optionalDeps.isEmpty() ? null : optionalDeps, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null, null, null));
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

    private static VersionManifest vm(String name, String version, Map<String, String> deps) {
        return vm(name, version, deps, null);
    }

    private static VersionManifest vm(String name, String version, Map<String, String> deps, Map<String, String> peers) {
        return new VersionManifest(name, version, null, null, deps, null, peers, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null);
    }

    private static VersionManifest vmWithOptionalPeer(String name, String version, String peer, String range) {
        ObjectNode meta = JsonNodeFactory.instance.objectNode();
        meta.putObject(peer).put("optional", true);
        return new VersionManifest(name, version, null, null, emptyMap(), null, singletonMap(peer, range), meta,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
