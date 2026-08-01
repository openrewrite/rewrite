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
    void incompatibleForkDefers() {
        FakeRegistry registry = new FakeRegistry()
                .add("a", "1.0.0", singletonMap("b", "^1.0.0"))
                .add("c", "1.0.0", singletonMap("b", "^2.0.0"))
                .add("b", "1.9.0", emptyMap())
                .add("b", "2.0.0", emptyMap());

        assertThatExceptionOfType(EngineFailure.class).isThrownBy(() ->
                new NpmGraphBuilder(registry).build(singletonMap("", "{\"dependencies\":{\"a\":\"^1.0.0\",\"c\":\"^1.0.0\"}}")));
    }

    @Test
    void peerDeclaringPackageDefers() {
        FakeRegistry registry = new FakeRegistry();
        registry.versionsByName.computeIfAbsent("has-peer", k -> new TreeSet<>()).add("1.0.0");
        registry.manifests.put("has-peer@1.0.0", vm("has-peer", "1.0.0", emptyMap(), singletonMap("react", ">=17")));

        assertThatExceptionOfType(EngineFailure.class).isThrownBy(() ->
                new NpmGraphBuilder(registry).build(singletonMap("", "{\"dependencies\":{\"has-peer\":\"^1.0.0\"}}")));
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
}
