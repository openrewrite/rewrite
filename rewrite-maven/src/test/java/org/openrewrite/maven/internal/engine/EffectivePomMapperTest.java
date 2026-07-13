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
package org.openrewrite.maven.internal.engine;

import org.junit.jupiter.api.Test;
import org.openrewrite.maven.internal.engine.EngineFixtureHarness.EngineResolution;
import org.openrewrite.maven.tree.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reference-identity, property-overlay, and no-change contracts of {@link EffectivePomMapper} output (study a3 §2),
 * driven through the real engine over the hermetic fixtures.
 */
class EffectivePomMapperTest {

    // a3 §2.2: getResolvedManagedDependency matches on `requested` OR `requestedBom` by reference (==).
    @Test
    void managedDependencyReferenceIdentity() {
        EngineResolution resolution = EngineFixtureHarness.resolve("bom-import-single");
        Pom requested = resolution.getRequested();
        MavenResolutionResult mrr = wrap(resolution);

        ManagedDependency importedBom = requested.getDependencyManagement().stream()
                .filter(d -> d instanceof ManagedDependency.Imported).findFirst().orElseThrow(AssertionError::new);
        ManagedDependency definedC = requested.getDependencyManagement().stream()
                .filter(d -> d instanceof ManagedDependency.Defined).findFirst().orElseThrow(AssertionError::new);

        ResolvedManagedDependency viaBom = mrr.getResolvedManagedDependency(importedBom);
        assertThat(viaBom).isNotNull();
        assertThat(viaBom.getRequestedBom()).isSameAs(importedBom);
        assertThat(viaBom.getBomGav()).isNotNull();

        ResolvedManagedDependency defined = mrr.getResolvedManagedDependency(definedC);
        assertThat(defined).isNotNull();
        assertThat(defined.getRequested()).isSameAs(definedC);
        assertThat(defined.getRequestedBom()).isNull();
    }

    // a3 §6: every declared managed dependency (Defined and the BOM Imported) resolves by reference across fixtures.
    @Test
    void managedDependencyThreadingHolds() {
        for (String fixture : List.of("bom-import-single", "bom-import-multi", "parent-chain", "classifiers")) {
            EngineResolution resolution = EngineFixtureHarness.resolve(fixture);
            MavenResolutionResult mrr = wrap(resolution);
            for (ResolvedManagedDependency dm : mrr.getPom().getDependencyManagement()) {
                assertThat(mrr.getResolvedManagedDependency(dm.getRequested()))
                        .as("%s: requested reference lookup", fixture).isNotNull();
                if (dm.getRequestedBom() != null) {
                    assertThat(mrr.getResolvedManagedDependency(dm.getRequestedBom()))
                            .as("%s: requestedBom reference lookup", fixture).isNotNull();
                }
            }
        }
    }

    // a3 §2.1: depth-0 requestedDependencies thread the exact declaring Pom.dependencies instances.
    @Test
    void requestedDependenciesThreadDeclaringInstances() {
        EngineResolution resolution = EngineFixtureHarness.resolve("exclusions");
        List<Dependency> declared = resolution.getRequested().getDependencies();
        List<Dependency> requestedDependencies = resolution.getPom().getRequestedDependencies();
        assertThat(declared).isNotEmpty();
        for (Dependency dependency : declared) {
            assertThat(requestedDependencies.stream().anyMatch(d -> d == dependency))
                    .as("requestedDependencies must thread the declared instance %s", dependency).isTrue();
        }
    }

    // DESIGN §4.1: parser/ctx-injected properties are overlaid and stay visible via getProperties()/getValue().
    @Test
    void injectedPropertiesAreVisible() {
        EngineResolution resolution = EngineFixtureHarness.resolve("parent-chain");
        String repoUri = resolution.getInjectedProperties().get("parity.repo.url");
        assertThat(resolution.getPom().getProperties()).containsEntry("parity.repo.url", repoUri);
        assertThat(resolution.getPom().getValue("${parity.repo.url}")).isEqualTo(repoUri);
        // and it does not clobber a lineage-declared property
        assertThat(resolution.getPom().getProperties()).containsEntry("leaf.version", "1.0");
    }

    // DESIGN §4.1: raw-lineage properties are NOT interpolated (a chained placeholder stays literal; getValue resolves it).
    @Test
    void propertiesAreRawNotInterpolated() {
        EngineResolution resolution = EngineFixtureHarness.resolve("property-indirection");
        assertThat(resolution.getPom().getProperties()).containsEntry("dep.version", "${dep.version.actual}");
        assertThat(resolution.getPom().getProperties()).containsEntry("dep.version.actual", "2.0");
        assertThat(resolution.getPom().getValue("${dep.version}")).isEqualTo("2.0");
    }

    // DESIGN §4.1: resolve()-style no-change identity — equal fields return the previous instance, a change returns fresh.
    @Test
    void sameIfUnchangedIdentity() {
        ResolvedPom pom = EngineFixtureHarness.resolve("bom-import-single").getPom();
        ResolvedPom remapped = EngineFixtureHarness.resolve("bom-import-single").getPom();
        assertThat(EffectivePomMapper.sameIfUnchanged(pom, remapped)).isSameAs(pom);

        ResolvedPom different = EngineFixtureHarness.resolve("classifiers").getPom();
        assertThat(EffectivePomMapper.sameIfUnchanged(pom, different)).isSameAs(different);
    }

    private static MavenResolutionResult wrap(EngineResolution resolution) {
        return new MavenResolutionResult(UUID.randomUUID(), null, resolution.getPom(), emptyList(), null,
                new LinkedHashMap<>(), null, resolution.getActiveProfiles(), resolution.getInjectedProperties());
    }
}
