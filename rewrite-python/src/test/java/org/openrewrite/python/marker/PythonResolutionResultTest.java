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
package org.openrewrite.python.marker;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class PythonResolutionResultTest {

    @Test
    void getResolvedDependencyByName() {
        ResolvedDependency requests = new ResolvedDependency(
                "requests", "2.31.0", "https://pypi.org/simple", null);

        PythonResolutionResult marker = new PythonResolutionResult(
                UUID.randomUUID(),
                "test-project",
                "1.0.0",
                null,
                null,
                "pyproject.toml",
                ">=3.10",
                null,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.singletonList(requests),
                null,
                null
        );

        assertThat(marker.getResolvedDependency("requests")).isEqualTo(requests);
        assertThat(marker.getResolvedDependency("nonexistent")).isNull();
    }

    @Test
    void getResolvedDependencyNormalizesName() {
        ResolvedDependency dep = new ResolvedDependency(
                "my-cool-package", "1.0.0", null, null);

        PythonResolutionResult marker = new PythonResolutionResult(
                UUID.randomUUID(),
                "test",
                "1.0.0",
                null,
                null,
                "pyproject.toml",
                null,
                null,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.singletonList(dep),
                null,
                null
        );

        // Dashes, underscores, and dots are normalized
        assertThat(marker.getResolvedDependency("my-cool-package")).isEqualTo(dep);
        assertThat(marker.getResolvedDependency("my_cool_package")).isEqualTo(dep);
        assertThat(marker.getResolvedDependency("My-Cool-Package")).isEqualTo(dep);
    }

    @Test
    void normalizeNameHandlesVariants() {
        assertThat(PythonResolutionResult.normalizeName("my-package")).isEqualTo("my_package");
        assertThat(PythonResolutionResult.normalizeName("my.package")).isEqualTo("my_package");
        assertThat(PythonResolutionResult.normalizeName("my_package")).isEqualTo("my_package");
        assertThat(PythonResolutionResult.normalizeName("MY-PACKAGE")).isEqualTo("my_package");
    }

    @Test
    void fieldAccess() {
        List<Dependency> buildRequires = Arrays.asList(
                new Dependency("hatchling", null, null, null, null));
        List<Dependency> deps = Arrays.asList(
                new Dependency("requests", ">=2.28.0", null, null, null),
                new Dependency("click", ">=8.0", null, null, null));

        Map<String, List<Dependency>> optDeps = new LinkedHashMap<>();
        optDeps.put("dev", Collections.singletonList(
                new Dependency("pytest", ">=7.0", null, null, null)));

        Map<String, List<Dependency>> depGroups = new LinkedHashMap<>();
        depGroups.put("test", Collections.singletonList(
                new Dependency("coverage", ">=7.0", null, null, null)));

        PythonResolutionResult marker = new PythonResolutionResult(
                UUID.randomUUID(),
                "my-project",
                "1.0.0",
                "A description",
                "MIT",
                "pyproject.toml",
                ">=3.10",
                "hatchling.build",
                buildRequires,
                deps,
                optDeps,
                depGroups,
                Collections.emptyList(),
                PackageManager.Uv,
                null
        );

        assertThat(marker.getName()).isEqualTo("my-project");
        assertThat(marker.getVersion()).isEqualTo("1.0.0");
        assertThat(marker.getDescription()).isEqualTo("A description");
        assertThat(marker.getLicense()).isEqualTo("MIT");
        assertThat(marker.getRequiresPython()).isEqualTo(">=3.10");
        assertThat(marker.getBuildBackend()).isEqualTo("hatchling.build");
        assertThat(marker.getBuildRequires()).hasSize(1);
        assertThat(marker.getDependencies()).hasSize(2);
        assertThat(marker.getOptionalDependencies()).containsKey("dev");
        assertThat(marker.getDependencyGroups()).containsKey("test");
        assertThat(marker.getDependencyGroups().get("test")).hasSize(1);
        assertThat(marker.getPackageManager()).isEqualTo(PackageManager.Uv);
    }
}
