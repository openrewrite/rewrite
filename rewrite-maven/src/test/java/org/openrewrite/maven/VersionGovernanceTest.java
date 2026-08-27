/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class VersionGovernanceTest implements RewriteTest {

    private static final GroupArtifact JACKSON_CORE = new GroupArtifact("com.fasterxml.jackson.core", "jackson-core");

    @Test
    void localManagedEntryIsGovernedBySelf() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>1.0.0</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>com.fasterxml.jackson.core</groupId>
                              <artifactId>jackson-core</artifactId>
                              <version>2.12.5</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """,
            spec -> spec.afterRecipe(doc -> {
                MavenResolutionResult mrr = doc.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                VersionGovernance governance = VersionGovernance.of(mrr, JACKSON_CORE);
                assertThat(governance).isNotNull();
                assertThat(governance.getKind()).isEqualTo(VersionGovernance.Kind.LOCAL_MANAGED);
                assertThat(governance.getManagedVersion()).isEqualTo("2.12.5");
                assertThat(governance.getBomGav()).isNull();
                assertThat(governance.getGoverningPom().getArtifactId()).isEqualTo("demo");
            })
          )
        );
    }

    @Test
    void managedEntryInParentIsGovernedByTheParent() {
        rewriteRun(
          mavenProject("parent",
            pomXml(
              """
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>parent</artifactId>
                    <version>1.0.0</version>
                    <packaging>pom</packaging>
                    <modules>
                        <module>child</module>
                    </modules>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>com.fasterxml.jackson.core</groupId>
                                <artifactId>jackson-core</artifactId>
                                <version>2.12.5</version>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
                """
            ),
            mavenProject("child",
              pomXml(
                """
                  <project>
                      <parent>
                          <groupId>com.example</groupId>
                          <artifactId>parent</artifactId>
                          <version>1.0.0</version>
                      </parent>
                      <artifactId>child</artifactId>
                  </project>
                  """,
                spec -> spec.afterRecipe(doc -> {
                    MavenResolutionResult mrr = doc.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                    VersionGovernance governance = VersionGovernance.of(mrr, JACKSON_CORE);
                    assertThat(governance).isNotNull();
                    assertThat(governance.getKind()).isEqualTo(VersionGovernance.Kind.LOCAL_MANAGED);
                    assertThat(governance.getGoverningPom().getArtifactId()).isEqualTo("parent");
                })
              )
            )
          )
        );
    }

    @Test
    void unmanagedCoordinateHasNoGovernance() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>1.0.0</version>
              </project>
              """,
            spec -> spec.afterRecipe(doc -> {
                MavenResolutionResult mrr = doc.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                assertThat(VersionGovernance.of(mrr, JACKSON_CORE)).isNull();
            })
          )
        );
    }
}
