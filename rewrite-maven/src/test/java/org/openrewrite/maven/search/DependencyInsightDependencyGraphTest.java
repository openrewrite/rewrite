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
package org.openrewrite.maven.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.maven.table.DependenciesInUse;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.Assertions.pomXml;

class DependencyInsightDependencyGraphTest implements RewriteTest {

    @DocumentExample
    @Test
    void directDependencyGraph() {
        rewriteRun(
          spec -> spec
            .dataTable(DependenciesInUse.Row.class, rows -> {
                assertThat(rows).hasSize(1);
                DependenciesInUse.Row row = rows.getFirst();
                assertThat(row.getDependencyGraph()).isEqualTo(
                  """
                    com.google.guava:guava:29.0-jre
                    \\--- compile
                    """.strip());
                assertThat(row.getDepth()).isEqualTo(0);
            })
            .recipe(new DependencyInsight("com.google.guava", "guava", null, null, null)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>29.0-jre</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <!--~~>--><dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>29.0-jre</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void transitiveDependencyGraph() {
        rewriteRun(
          spec -> spec
            .dataTable(DependenciesInUse.Row.class, rows -> {
                assertThat(rows).hasSize(1);
                DependenciesInUse.Row row = rows.getFirst();
                assertThat(row.getDependencyGraph()).isEqualTo(
                  """
                    io.prometheus:simpleclient_common:0.9.0
                    \\--- io.micrometer:micrometer-registry-prometheus:1.6.3
                         \\--- compile
                    """.strip());
                assertThat(row.getDepth()).isEqualTo(1);
            })
            .recipe(new DependencyInsight("io.prometheus", "simpleclient_common", null, null, null)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                      <groupId>io.micrometer</groupId>
                      <artifactId>micrometer-registry-prometheus</artifactId>
                      <version>1.6.3</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <!--~~(io.prometheus:simpleclient_common:0.9.0)~~>--><dependency>
                      <groupId>io.micrometer</groupId>
                      <artifactId>micrometer-registry-prometheus</artifactId>
                      <version>1.6.3</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void deepTransitiveDependencyGraph() {
        rewriteRun(
          spec -> spec
            .dataTable(DependenciesInUse.Row.class, rows -> {
                assertThat(rows).hasSize(1);
                DependenciesInUse.Row row = rows.getFirst();
                assertThat(row.getDependencyGraph()).isEqualTo(
                  """
                    io.netty:netty-codec-dns:4.1.89.Final
                    \\--- io.netty:netty-resolver-dns:4.1.89.Final
                         \\--- io.vertx:vertx-core:3.9.16
                              \\--- io.vertx:vertx-lang-kotlin:3.9.16
                                   \\--- compile
                    """.strip());
                assertThat(row.getDepth()).isEqualTo(3);
                assertThat(row.getVersion()).isEqualTo("4.1.89.Final");
            })
            .recipe(new DependencyInsight("io.netty", "netty-codec-dns", null, null, null)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                      <groupId>io.vertx</groupId>
                      <artifactId>vertx-lang-kotlin</artifactId>
                      <version>3.9.16</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <!--~~(io.netty:netty-codec-dns:4.1.89.Final)~~>--><dependency>
                      <groupId>io.vertx</groupId>
                      <artifactId>vertx-lang-kotlin</artifactId>
                      <version>3.9.16</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    /**
     * Maven deduplicates dependencies and shows only the direct path.
     * <p>
     * jakarta.annotation-api appears as a direct dependency of jersey-server (depth 1)
     * even though jersey-common (also a dependency of jersey-server) also depends on it.
     */
    @Test
    void dependencyGraphForDuplicateTransitiveWithSameGroupIdArtifactIdVersion() {
        rewriteRun(
          spec -> spec
            .dataTable(DependenciesInUse.Row.class, rows -> {
                assertThat(rows).hasSize(1);
                DependenciesInUse.Row row = rows.getFirst();
                assertThat(row.getDependencyGraph()).isEqualTo(
                  """
                    jakarta.annotation:jakarta.annotation-api:1.3.5
                    \\--- org.glassfish.jersey.core:jersey-server:2.35
                         \\--- compile
                    """.strip());
                assertThat(row.getDepth()).isEqualTo(1);
            })
            .recipe(new DependencyInsight("jakarta.annotation", "jakarta.annotation-api", null, null, null)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                      <groupId>org.glassfish.jersey.core</groupId>
                      <artifactId>jersey-server</artifactId>
                      <version>2.35</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <!--~~(jakarta.annotation:jakarta.annotation-api:1.3.5)~~>--><dependency>
                      <groupId>org.glassfish.jersey.core</groupId>
                      <artifactId>jersey-server</artifactId>
                      <version>2.35</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void dependencyGraphWithNullScopeSearchesAllScopes() {
        rewriteRun(
          spec -> spec
            .dataTable(DependenciesInUse.Row.class, rows -> {
                assertThat(rows).hasSize(2);
                DependenciesInUse.Row row1 = rows.get(0);
                // When scope is null, all scopes are searched
                // Dependencies without an explicit scope default to compile
                assertThat(row1.getDependencyGraph()).isEqualTo(
                  """
                    com.google.guava:guava:29.0-jre
                    \\--- compile
                    """.strip());
                assertThat(row1.getDepth()).isEqualTo(0);
                DependenciesInUse.Row row2 = rows.get(1);
                assertThat(row2.getDependencyGraph()).isEqualTo(
                  """
                    com.google.guava:guava:29.0-jre
                    \\--- compile
                    """.strip());
                assertThat(row2.getDepth()).isEqualTo(0);
            })
            .recipe(new DependencyInsight("com.google.guava", "guava", null, null, null)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>29.0-jre</version>
                  </dependency>
                  <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>29.0-jre</version>
                      <scope>test</scope>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <!--~~>--><dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>29.0-jre</version>
                  </dependency>
                  <!--~~>--><dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>29.0-jre</version>
                      <scope>test</scope>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }
}
