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
import org.openrewrite.maven.internal.ResolutionEngineSelector;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Scope;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.parity.synthetic.MockMavenRepo.pomPath;
import static org.openrewrite.maven.parity.synthetic.MockMavenRepo.pomXml;
import static org.openrewrite.maven.parity.synthetic.SyntheticHarness.dependencies;
import static org.openrewrite.maven.parity.synthetic.SyntheticHarness.rootPom;

/**
 * A {@code RELEASE}/{@code LATEST} metaversion declared as a direct dependency's version resolves through
 * merged {@code maven-metadata.xml} before collection (the engine's {@code EngineDependencyCollector} seeds the concrete
 * version so the descriptor read no longer requests {@code artifact-LATEST.pom}). Pins the engine (MAVEN mode) resolving
 * {@code RELEASE} to {@code <release>} and {@code LATEST} to {@code <latest>}, hermetically.
 */
class MetaversionResolutionTest {
    private static final String G = "org.parity.synthetic";
    private static final String METADATA_PATH = "/org/parity/synthetic/meta/maven-metadata.xml";

    //language=xml
    private static final String METADATA = """
      <metadata>
          <groupId>org.parity.synthetic</groupId>
          <artifactId>meta</artifactId>
          <versioning>
              <latest>2.0</latest>
              <release>1.5</release>
              <versions>
                  <version>1.5</version>
                  <version>2.0</version>
              </versions>
              <lastUpdated>20260101010101</lastUpdated>
          </versioning>
      </metadata>
      """;

    private static ResolvedDependency single(SyntheticHarness.Resolution resolution) {
        List<ResolvedDependency> compile = resolution.marker().getDependencies().get(Scope.Compile);
        assertThat(compile).hasSize(1);
        return compile.get(0);
    }

    @Test
    void releaseMetaversionResolvesToReleaseVersion() {
        try (MockMavenRepo repo = new MockMavenRepo()) {
            repo.serve(METADATA_PATH, METADATA);
            repo.serve(pomPath(G, "meta", "1.5", "1.5"), pomXml(G, "meta", "1.5"));

            SyntheticHarness.Resolution resolution = SyntheticHarness.resolve(
              rootPom(dependencies(G + ":meta:RELEASE")),
              ctx -> {
                  ctx.putMessage(ResolutionEngineSelector.ENGINE_KEY, "maven");
                  ctx.setRepositories(List.of(repo.repo("central")));
              });

            assertThat(single(resolution).getGav().getVersion()).isEqualTo("1.5");
        }
    }

    @Test
    void latestMetaversionResolvesToLatestVersion() {
        try (MockMavenRepo repo = new MockMavenRepo()) {
            repo.serve(METADATA_PATH, METADATA);
            repo.serve(pomPath(G, "meta", "2.0", "2.0"), pomXml(G, "meta", "2.0"));

            SyntheticHarness.Resolution resolution = SyntheticHarness.resolve(
              rootPom(dependencies(G + ":meta:LATEST")),
              ctx -> {
                  ctx.putMessage(ResolutionEngineSelector.ENGINE_KEY, "maven");
                  ctx.setRepositories(List.of(repo.repo("central")));
              });

            assertThat(single(resolution).getGav().getVersion()).isEqualTo("2.0");
        }
    }
}
