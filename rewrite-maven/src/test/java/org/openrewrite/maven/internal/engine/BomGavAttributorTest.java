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
import org.openrewrite.maven.tree.ResolvedManagedDependency;
import org.openrewrite.maven.tree.ResolvedPom;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The membership-stamping {@code bomGav} reconstruction (SPIKE-RESULTS §4b). A single-level import is attributable
 * straight from the imported BOM; the load-bearing case is the multi-level BOM, where the directly-imported BOM
 * inherits its management from a parent BOM — {@code bomGav} must still be the directly-imported one (as rewrite
 * reports), not the defining parent {@code InputLocation} points at.
 */
class BomGavAttributorTest {

    @Test
    void singleLevelBomIsAttributedToTheImportedBom() {
        ResolvedPom pom = EngineFixtureHarness.resolve("bom-import-single").getPom();
        ResolvedManagedDependency managedA = managed(pom, "managed-a");
        assertThat(managedA.getBomGav()).isNotNull();
        assertThat(managedA.getBomGav().getArtifactId()).isEqualTo("bom");
        assertThat(managed(pom, "managed-b").getBomGav().getArtifactId()).isEqualTo("bom");
    }

    @Test
    void multiLevelBomIsAttributedToTheDirectlyImportedBomNotTheDefiningParent() {
        ResolvedPom pom = EngineFixtureHarness.resolve("bom-import-multi").getPom();
        ResolvedManagedDependency managedY = managed(pom, "managed-y");
        assertThat(managedY.getBomGav()).isNotNull();
        assertThat(managedY.getBomGav().getArtifactId())
                .as("multi-level import must stamp the directly-imported BOM, not its defining parent")
                .isEqualTo("bom-outer");
    }

    private static ResolvedManagedDependency managed(ResolvedPom pom, String artifactId) {
        return pom.getDependencyManagement().stream()
                .filter(d -> artifactId.equals(d.getArtifactId()))
                .findFirst().orElseThrow(() -> new AssertionError("managed dependency not found: " + artifactId));
    }
}
