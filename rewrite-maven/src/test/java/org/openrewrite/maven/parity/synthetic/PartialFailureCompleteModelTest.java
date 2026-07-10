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
package org.openrewrite.maven.parity.synthetic;

import org.junit.jupiter.api.Test;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Scope;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Corpus shape observed on jetty-server-11.0.19: a download failure in one scope must not
 * discard the model. The complete model is the contract — the marker stays present with every
 * resolvable scope populated, and the failing scope's error is surfaced.
 */
class PartialFailureCompleteModelTest {

    @Test
    void unresolvableTestScopedDirectDependencyKeepsCompleteModel() {
        try (MockMavenRepo repo = new MockMavenRepo()) {
            repo.serve(MockMavenRepo.pomPath("org.parity", "a", "1.0", "1.0"),
              MockMavenRepo.pomXml("org.parity", "a", "1.0"));
            // org.parity:missing is never served: pom GET and jar HEAD both 404

            SyntheticHarness.Resolution resolution = SyntheticHarness.resolve(
              SyntheticHarness.rootPom(
                SyntheticHarness.repositories("mock=" + repo.url()) +
                """
                <dependencies>
                    <dependency><groupId>org.parity</groupId><artifactId>a</artifactId><version>1.0</version></dependency>
                    <dependency><groupId>org.parity</groupId><artifactId>missing</artifactId><version>1.0</version><scope>test</scope></dependency>
                </dependencies>
                """),
              ctx -> {
              });

            assertThat(resolution.failed()).isFalse();
            MavenResolutionResult marker = resolution.marker();
            assertThat(marker.findDependencies("org.parity", "a", Scope.Compile)).isNotEmpty();
            assertThat(marker.getDependencies()).containsKeys(Scope.Compile, Scope.Runtime, Scope.Provided);
            assertThat(marker.getDependencies()).doesNotContainKey(Scope.Test);
            assertThat(resolution.errors())
              .anySatisfy(t -> assertThat(t).isInstanceOf(MavenDownloadingException.class)
                .hasMessageContaining("org.parity:missing:1.0"));
        }
    }

    @Test
    void allScopesFailingStillKeepsTheMarker() {
        try (MockMavenRepo repo = new MockMavenRepo()) {
            SyntheticHarness.Resolution resolution = SyntheticHarness.resolve(
              SyntheticHarness.rootPom(
                SyntheticHarness.repositories("mock=" + repo.url()) +
                SyntheticHarness.dependencies("org.parity:missing:1.0")),
              ctx -> {
              });

            assertThat(resolution.failed()).isFalse();
            assertThat(resolution.errored()).isTrue();
            assertThat(resolution.marker().getDependencies()).isEmpty();
        }
    }
}
