/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.maven.parity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.maven.cache.InMemoryMavenPomCache;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.*;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.util.Collections.newSetFromMap;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the {@code ==} contracts of the resolution model. These are engine-independent and must
 * survive the resolution engine swap unchanged.
 */
class IdentityContractsTest {

    static List<String> fixtures() {
        return ParityHarness.fixtureNames();
    }

    @ParameterizedTest
    @MethodSource("fixtures")
    void requestedDependencyLookupByReference(String fixture) {
        MavenResolutionResult mrr = ParityHarness.resolve(fixture).getMarker();
        List<Dependency> requested = mrr.getPom().getRequestedDependencies();
        for (Dependency dependency : requested) {
            if (isShadowedByGaDeduplication(dependency, requested)) {
                continue; // pinned separately in classifierShadowedByGaDeduplication
            }
            assertThat(mrr.getResolvedDependency(dependency))
              .as("getResolvedDependency must find %s by reference equality on `requested`", dependency)
              .isNotNull();
        }
        // and the resolved node's requested field is the declaration instance, not a copy
        for (List<ResolvedDependency> scope : mrr.getDependencies().values()) {
            for (ResolvedDependency d : scope) {
                if (d.getDepth() == 0) {
                    assertThat(requested.stream().anyMatch(r -> r == d.getRequested()))
                      .as("depth-0 node %s must thread the requestedDependencies instance", d.getGav())
                      .isTrue();
                }
            }
        }
    }

    /**
     * The legacy reference engine (which {@link ParityHarness} pins) deduplicates direct
     * dependencies by groupId:artifactId only ("last declaration wins"), so a direct dependency
     * differing only by classifier is shadowed and unresolvable by reference.
     * {@link #classifierVariantsBothResolveOnMavenEngine} pins the Maven-engine counterpart.
     */
    @Test
    void classifierShadowedByGaDeduplication() {
        MavenResolutionResult mrr = ParityHarness.resolve("classifiers").getMarker();
        List<Dependency> requested = mrr.getPom().getRequestedDependencies();
        assertThat(requested).hasSize(2);

        Dependency plain = requested.stream().filter(d -> d.getClassifier() == null).findFirst().orElseThrow();
        Dependency tests = requested.stream().filter(d -> "tests".equals(d.getClassifier())).findFirst().orElseThrow();

        assertThat(mrr.getResolvedDependency(plain)).isNull();
        ResolvedDependency resolved = mrr.getResolvedDependency(tests);
        assertThat(resolved).isNotNull();
        assertThat(resolved.getClassifier()).isEqualTo("tests");
        assertThat(resolved.getVersion()).isEqualTo("1.1");
    }

    /**
     * Maven keys direct-dependency conflicts on g:a:classifier:type, so two declarations
     * differing only by classifier both resolve on the (default) Maven engine.
     */
    @Test
    void classifierVariantsBothResolveOnMavenEngine() {
        MavenResolutionResult mrr = resolveDefaultEngine("classifiers");
        List<Dependency> requested = mrr.getPom().getRequestedDependencies();
        assertThat(requested).hasSize(2);

        Dependency plain = requested.stream().filter(d -> d.getClassifier() == null).findFirst().orElseThrow();
        Dependency tests = requested.stream().filter(d -> "tests".equals(d.getClassifier())).findFirst().orElseThrow();

        ResolvedDependency resolvedPlain = mrr.getResolvedDependency(plain);
        assertThat(resolvedPlain).isNotNull();
        assertThat(resolvedPlain.getClassifier()).isNull();
        assertThat(resolvedPlain.getVersion()).isEqualTo("1.0");
        ResolvedDependency resolvedTests = mrr.getResolvedDependency(tests);
        assertThat(resolvedTests).isNotNull();
        assertThat(resolvedTests.getClassifier()).isEqualTo("tests");
        assertThat(resolvedTests.getVersion()).isEqualTo("1.1");
    }

    /**
     * Maven follows {@code <distributionManagement><relocation>}, so on the (default) Maven
     * engine the declaration on the relocated coordinate resolves the relocation TARGET's g:a
     * and the original declaration has no resolved node by reference.
     */
    @Test
    void relocatedCoordinateResolvesTargetOnMavenEngine() {
        MavenResolutionResult mrr = resolveDefaultEngine("relocation");
        List<Dependency> requested = mrr.getPom().getRequestedDependencies();
        assertThat(requested).hasSize(1);
        assertThat(requested.getFirst().getArtifactId()).isEqualTo("oldg");

        assertThat(mrr.getResolvedDependency(requested.getFirst())).isNull();
        assertThat(mrr.findDependencies("org.parity", "newg", Scope.Compile))
          .singleElement()
          .satisfies(d -> assertThat(d.getVersion()).isEqualTo("1.0"));
    }

    // Legacy's dedup key is groupId:artifactId only; the harness pins the legacy reference engine
    private static boolean isShadowedByGaDeduplication(Dependency dependency, List<Dependency> requested) {
        for (int i = requested.size() - 1; i >= 0; i--) {
            Dependency other = requested.get(i);
            if (Objects.equals(other.getGroupId(), dependency.getGroupId()) &&
                    other.getArtifactId().equals(dependency.getArtifactId())) {
                return other != dependency;
            }
        }
        return false;
    }

    // Mirrors ParityHarness.resolve but resolves through the ambient default (Maven) engine
    private static MavenResolutionResult resolveDefaultEngine(String fixture) {
        Path fixtureDir;
        try {
            fixtureDir = Paths.get(requireNonNull(IdentityContractsTest.class.getResource("/parity/fixtures/" + fixture)).toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        Path repoDir = fixtureDir.resolve("repo");
        MavenExecutionContextView ctx = MavenExecutionContextView.view(new InMemoryExecutionContext(t -> {
            throw new RuntimeException(t);
        }));
        ctx.setPomCache(new InMemoryMavenPomCache());
        ctx.setRepositories(singletonList(MavenRepository.builder()
          .id("fixture")
          .uri(repoDir.toUri().toString())
          .knownToExist(true)
          .build()));
        ctx.setAddCentralRepository(false);
        ctx.setAddLocalRepository(false);
        SourceFile doc = MavenParser.builder()
          .property("parity.repo.url", repoDir.toUri().toString())
          .build()
          .parseInputs(singletonList(Parser.Input.fromFile(fixtureDir.resolve("pom.xml"))), fixtureDir, ctx)
          .findFirst()
          .orElseThrow(() -> new IllegalStateException("Fixture " + fixture + " did not parse"));
        return doc.getMarkers().findFirst(MavenResolutionResult.class)
          .orElseThrow(() -> new IllegalStateException("Fixture " + fixture + " resolved without a MavenResolutionResult marker"));
    }

    @Test
    void managedDependencyLookupByReferenceForDefinedAndImported() {
        MavenResolutionResult mrr = ParityHarness.resolve("bom-import-single").getMarker();
        List<ManagedDependency> declarations = mrr.getPom().getRequested().getDependencyManagement();
        assertThat(declarations).hasSize(2);

        ManagedDependency imported = declarations.stream().filter(d -> d instanceof ManagedDependency.Imported).findFirst().orElseThrow();
        ManagedDependency defined = declarations.stream().filter(d -> d instanceof ManagedDependency.Defined).findFirst().orElseThrow();

        assertThat(mrr.getResolvedManagedDependency(defined)).isNotNull();
        ResolvedManagedDependency viaBom = mrr.getResolvedManagedDependency(imported);
        assertThat(viaBom).isNotNull();
        assertThat(viaBom.getRequestedBom()).isSameAs(imported);
        assertThat(viaBom.getBomGav()).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("fixtures")
    void managedDependencyThreading(String fixture) {
        MavenResolutionResult mrr = ParityHarness.resolve(fixture).getMarker();
        for (ResolvedManagedDependency dm : mrr.getPom().getDependencyManagement()) {
            assertThat(mrr.getResolvedManagedDependency(dm.getRequested())).isNotNull();
            if (dm.getRequestedBom() != null) {
                assertThat(mrr.getResolvedManagedDependency(dm.getRequestedBom())).isNotNull();
            }
        }
    }

    @ParameterizedTest
    @MethodSource("fixtures")
    void resolveWithoutChangeReturnsSameInstance(String fixture) throws MavenDownloadingException {
        ParityHarness.Resolution resolution = ParityHarness.resolve(fixture);
        MavenResolutionResult mrr = resolution.getMarker();
        MavenPomDownloader downloader = new MavenPomDownloader(mrr.getProjectPoms(), resolution.getCtx());
        assertThat(mrr.getPom().resolve(resolution.getCtx(), downloader))
          .as("resolve() with no effective change must return the same instance")
          .isSameAs(mrr.getPom());
    }

    @ParameterizedTest
    @MethodSource("fixtures")
    void flatScopeListsAndNestedGraphShareInstances(String fixture) {
        MavenResolutionResult mrr = ParityHarness.resolve(fixture).getMarker();
        for (Map.Entry<Scope, List<ResolvedDependency>> scope : mrr.getDependencies().entrySet()) {
            Set<ResolvedDependency> flat = newSetFromMap(new IdentityHashMap<>());
            flat.addAll(scope.getValue());
            for (ResolvedDependency d : scope.getValue()) {
                for (ResolvedDependency child : d.getDependencies()) {
                    assertThat(flat.contains(child))
                      .as("child %s of %s in scope %s must be the same instance as a flat list entry",
                        child.getGav(), d.getGav(), scope.getKey())
                      .isTrue();
                }
            }
        }
    }
}
