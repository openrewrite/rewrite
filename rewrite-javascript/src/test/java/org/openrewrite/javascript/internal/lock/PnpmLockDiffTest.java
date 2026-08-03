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
import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.internal.lock.LockEditSet.PackageEdit;
import org.openrewrite.javascript.internal.registry.VersionManifest;

import java.util.*;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for the pnpm graph-to-lock diff's decisions — what is untouched, what forks, and which peer shapes
 * defer. The end-to-end byte contract lives in {@code PnpmResolveAndPatchLockRegenTest}.
 */
class PnpmLockDiffTest {

    @Test
    void retainedClosureProducesNoEdits() {
        FakeRegistry registry = new FakeRegistry()
                .add("is-odd", "3.0.1", singletonMap("is-number", "^6.0.0"))
                .add("is-number", "6.0.0", emptyMap());
        String lock = "lockfileVersion: '9.0'\n" +
                "\nimporters:\n" +
                "\n  .:\n    dependencies:\n      is-odd:\n        specifier: ^3.0.1\n        version: 3.0.1\n" +
                "\npackages:\n" +
                "\n  is-number@6.0.0:\n    resolution: {integrity: sha512-n}\n" +
                "\n  is-odd@3.0.1:\n    resolution: {integrity: sha512-o}\n" +
                "\nsnapshots:\n" +
                "\n  is-number@6.0.0: {}\n" +
                "\n  is-odd@3.0.1:\n    dependencies:\n      is-number: 6.0.0\n";

        ResolutionGraph graph = new NpmGraphBuilder(registry).build(singletonMap("",
                "{\"name\":\"t\",\"version\":\"1.0.0\",\"dependencies\":{\"is-odd\":\"^3.0.1\"}}"));
        assertThat(PnpmLockDiff.diff(graph, lock)).isEmpty();
    }

    @Test
    void declaredReResolutionBesideRetainedVersionIsAContentFork() {
        FakeRegistry registry = new FakeRegistry()
                .add("is-odd", "3.0.1", singletonMap("is-number", "^6.0.0"))
                .add("is-number", "6.0.0", emptyMap())
                .add("is-number", "7.0.0", emptyMap());
        String lock = "lockfileVersion: '9.0'\n" +
                "\nimporters:\n" +
                "\n  .:\n    dependencies:\n      is-number:\n        specifier: ^6.0.0\n        version: 6.0.0\n" +
                "      is-odd:\n        specifier: ^3.0.1\n        version: 3.0.1\n" +
                "\npackages:\n" +
                "\n  is-number@6.0.0:\n    resolution: {integrity: sha512-n}\n" +
                "\n  is-odd@3.0.1:\n    resolution: {integrity: sha512-o}\n" +
                "\nsnapshots:\n" +
                "\n  is-number@6.0.0: {}\n" +
                "\n  is-odd@3.0.1:\n    dependencies:\n      is-number: 6.0.0\n";

        ResolutionGraph graph = new NpmGraphBuilder(registry).build(singletonMap("",
                "{\"name\":\"t\",\"version\":\"1.0.0\",\"dependencies\":{\"is-number\":\"^7.0.0\",\"is-odd\":\"^3.0.1\"}}"));
        List<PackageEdit> edits = PnpmLockDiff.diff(graph, lock);

        assertThat(edits).singleElement().satisfies(edit -> {
            assertThat(edit.getKind()).isEqualTo(PackageEdit.Kind.CONTENT_FORK);
            assertThat(edit.getName()).isEqualTo("is-number");
            assertThat(edit.getNewVersion()).isEqualTo("7.0.0");
        });
    }

    @Test
    void transitivePeerDefers() {
        // wrapper depends on has-peer but neither declares nor provides react: pnpm would record
        // transitivePeerDependencies on wrapper, which the diff cannot express.
        FakeRegistry registry = new FakeRegistry()
                .add("react", "19.0.0", emptyMap())
                .add("wrapper", "1.0.0", singletonMap("has-peer", "^1.0.0"));
        registry.addWithPeers("has-peer", "1.0.0", emptyMap(), singletonMap("react", ">=17"));

        ResolutionGraph graph = new NpmGraphBuilder(registry).build(singletonMap("",
                "{\"name\":\"t\",\"version\":\"1.0.0\",\"dependencies\":{\"react\":\"19.0.0\",\"wrapper\":\"^1.0.0\"}}"));
        assertThatExceptionOfType(EngineFailure.class)
                .isThrownBy(() -> PnpmLockDiff.diff(graph, ROOT_ONLY_LOCK))
                .withMessageContaining("transitive peer");
    }

    @Test
    void peerDependenciesMetaDefers() {
        // An optional peer reshapes the suffix and can leak into transitivePeerDependencies; a fresh node
        // declaring peerDependenciesMeta defers.
        FakeRegistry registry = new FakeRegistry().add("react", "19.0.0", emptyMap());
        ObjectNode meta = JsonNodeFactory.instance.objectNode();
        meta.putObject("react").put("optional", true);
        registry.put("has-meta", "1.0.0", emptyMap(), singletonMap("react", ">=17"), meta);

        ResolutionGraph graph = new NpmGraphBuilder(registry).build(singletonMap("",
                "{\"name\":\"t\",\"version\":\"1.0.0\",\"dependencies\":{\"react\":\"19.0.0\",\"has-meta\":\"^1.0.0\"}}"));
        assertThatExceptionOfType(EngineFailure.class)
                .isThrownBy(() -> PnpmLockDiff.diff(graph, ROOT_ONLY_LOCK))
                .withMessageContaining("peerDependenciesMeta");
    }

    private static final String ROOT_ONLY_LOCK = "lockfileVersion: '9.0'\n" +
            "\nsettings:\n  autoInstallPeers: true\n  excludeLinksFromLockfile: false\n" +
            "\nimporters:\n" +
            "\n  .: {}\n";

    private static final class FakeRegistry implements Registry {
        final Map<String, Set<String>> versionsByName = new HashMap<>();
        final Map<String, VersionManifest> manifests = new HashMap<>();

        FakeRegistry add(String name, String version, Map<String, String> deps) {
            return put(name, version, deps, null, null);
        }

        FakeRegistry addWithPeers(String name, String version, Map<String, String> deps, Map<String, String> peers) {
            return put(name, version, deps, peers, null);
        }

        FakeRegistry put(String name, String version, Map<String, String> deps,
                         Map<String, String> peers, ObjectNode peerMeta) {
            versionsByName.computeIfAbsent(name, k -> new TreeSet<>()).add(version);
            VersionManifest.Dist dist = new VersionManifest.Dist(
                    "https://registry.npmjs.org/" + name + "/-/" + name + "-" + version + ".tgz",
                    null, "sha512-" + name + version);
            manifests.put(name + "@" + version, new VersionManifest(name, version, null, null,
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
