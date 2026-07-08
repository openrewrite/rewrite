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

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.maven.parity.ResolutionSnapshot;
import org.openrewrite.maven.parity.SnapshotNormalizer;
import org.openrewrite.maven.tree.MavenResolutionResult;

import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.parity.synthetic.MockMavenRepo.pomPath;
import static org.openrewrite.maven.parity.synthetic.MockMavenRepo.pomXml;
import static org.openrewrite.maven.parity.synthetic.SyntheticHarness.dependencies;
import static org.openrewrite.maven.parity.synthetic.SyntheticHarness.rootPom;

/**
 * Pins the additive snapshot coverage fields: per-node {@code licenses}, resolution-level
 * {@code parentGav}/{@code moduleGavs} (flattened, never live references), and pom-level
 * {@code pluginRepositories} and {@code subprojects}.
 */
class SnapshotCoverageTest {
    private static final String G = "org.parity.synthetic";

    @Test
    void dependencyLicensesCaptured() {
        try (MockMavenRepo repo = new MockMavenRepo()) {
            repo.serve(pomPath(G, "licensed", "1.0", "1.0"), pomXml(G, "licensed", "1.0", """
              <licenses>
                  <license>
                      <name>The Apache Software License, Version 2.0</name>
                  </license>
              </licenses>
              """));

            SyntheticHarness.Resolution resolution = SyntheticHarness.resolve(
              rootPom(dependencies(G + ":licensed:1.0")),
              ctx -> ctx.setRepositories(List.of(repo.repo("mock"))));

            JsonNode licenses = resolution.snapshot().getJson().at("/scopes/Compile/0/licenses");
            assertThat(licenses).hasSize(1);
            assertThat(licenses.get(0).asText()).isEqualTo("The Apache Software License, Version 2.0|Apache2");
        }
    }

    @Test
    void parentAndModuleGavsFlattened() {
        SyntheticHarness.Session session = new SyntheticHarness.Session(ctx -> {
        });
        List<SourceFile> parsed = MavenParser.builder().build().parseInputs(List.of(
          Parser.Input.fromString(Paths.get("pom.xml"),
            //language=xml
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.parity.synthetic</groupId>
                  <artifactId>parent</artifactId>
                  <version>1.0</version>
                  <packaging>pom</packaging>
                  <modules>
                      <module>child</module>
                  </modules>
              </project>
              """),
          Parser.Input.fromString(Paths.get("child/pom.xml"),
            //language=xml
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                      <groupId>org.parity.synthetic</groupId>
                      <artifactId>parent</artifactId>
                      <version>1.0</version>
                  </parent>
                  <artifactId>child</artifactId>
              </project>
              """)), null, session.ctx).toList();

        assertThat(parsed).hasSize(2);
        ResolutionSnapshot parent = snapshot(session, byArtifactId(parsed, "parent"));
        ResolutionSnapshot child = snapshot(session, byArtifactId(parsed, "child"));

        assertThat(parent.getJson().get("parentGav").isNull()).isTrue();
        assertThat(parent.getJson().get("moduleGavs")).hasSize(1);
        assertThat(parent.getJson().get("moduleGavs").get(0).asText()).isEqualTo(G + ":child:1.0");
        assertThat(parent.getJson().at("/pom/subprojects/0").asText()).isEqualTo("child");

        assertThat(child.getJson().get("parentGav").asText()).isEqualTo(G + ":parent:1.0");
        assertThat(child.getJson().get("moduleGavs")).isEmpty();
        assertThat(child.getJson().get("pom").get("subprojects")).isEmpty();
    }

    @Test
    void pluginRepositoriesCaptured() {
        SyntheticHarness.Resolution resolution = SyntheticHarness.resolve(
          rootPom("""
            <pluginRepositories>
                <pluginRepository>
                    <id>plugin-repo</id>
                    <url>https://plugins.example/maven</url>
                </pluginRepository>
            </pluginRepositories>
            """),
          ctx -> {
          });

        JsonNode pluginRepositories = resolution.snapshot().getJson().at("/pom/pluginRepositories");
        assertThat(pluginRepositories).hasSize(1);
        assertThat(pluginRepositories.get(0).get("id").asText()).isEqualTo("plugin-repo");
        assertThat(pluginRepositories.get(0).get("uri").asText()).isEqualTo("https://plugins.example/maven");
    }

    private static SourceFile byArtifactId(List<SourceFile> parsed, String artifactId) {
        return parsed.stream()
                .filter(s -> s.getMarkers().findFirst(MavenResolutionResult.class)
                        .map(m -> artifactId.equals(m.getPom().getArtifactId()))
                        .orElse(false))
                .findFirst()
                .orElseThrow();
    }

    private static ResolutionSnapshot snapshot(SyntheticHarness.Session session, SourceFile sourceFile) {
        MavenResolutionResult marker = sourceFile.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
        return ResolutionSnapshot.of(marker, session.errors, session.listener.getEvents(), new SnapshotNormalizer(), session.ctx);
    }
}
