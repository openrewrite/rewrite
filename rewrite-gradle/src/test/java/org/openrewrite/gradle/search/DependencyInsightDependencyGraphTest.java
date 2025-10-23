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
package org.openrewrite.gradle.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.maven.table.DependenciesInUse;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

class DependencyInsightDependencyGraphTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi());
    }

    @DocumentExample
    @Test
    void directDependencyGraph() {
        rewriteRun(
          spec -> spec
            .recipe(new DependencyInsight("com.google.guava", "guava", null, "compileClasspath"))
            .dataTable(DependenciesInUse.Row.class, rows -> {
                assertThat(rows).hasSize(1);
                DependenciesInUse.Row row = rows.getFirst();
                assertThat(row.getDependencyGraph()).isEqualTo(
                  """
                    com.google.guava:guava:29.0-jre
                    \\--- compileClasspath
                    """.strip());
                assertThat(row.getDepth()).isEqualTo(0);
            }),
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'com.google.guava:guava:29.0-jre'
              }
              """,
            """
              plugins {
                  id 'java'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  /*~~(com.google.guava:guava:29.0-jre)~~>*/implementation 'com.google.guava:guava:29.0-jre'
              }
              """
          )
        );
    }

    @Test
    void transitiveDependencyGraph() {
        rewriteRun(
          spec -> spec
            .recipe(new DependencyInsight("io.prometheus", "simpleclient_common", null, "compileClasspath"))
            .dataTable(DependenciesInUse.Row.class, rows -> {
                assertThat(rows).hasSize(1);
                DependenciesInUse.Row row = rows.getFirst();
                assertThat(row.getDependencyGraph()).isEqualTo(
                  """
                    io.prometheus:simpleclient_common:0.9.0
                    \\--- io.micrometer:micrometer-registry-prometheus:1.6.3
                         \\--- compileClasspath
                    """.strip());
                assertThat(row.getDepth()).isEqualTo(1);
            }),
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'io.micrometer:micrometer-registry-prometheus:1.6.3'
              }
              """,
            """
              plugins {
                  id 'java'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  /*~~(io.prometheus:simpleclient_common:0.9.0)~~>*/implementation 'io.micrometer:micrometer-registry-prometheus:1.6.3'
              }
              """
          )
        );
    }

    @Test
    void deepTransitiveDependencyGraph() {
        rewriteRun(
          spec -> spec
            .recipe(new DependencyInsight("io.netty", "netty-codec-dns", null, "compileClasspath"))
            .dataTable(DependenciesInUse.Row.class, rows -> {
                assertThat(rows).hasSize(1);
                DependenciesInUse.Row row = rows.getFirst();
                assertThat(row.getDependencyGraph()).isEqualTo(
                  """
                    io.netty:netty-codec-dns:4.1.89.Final
                    \\--- io.netty:netty-resolver-dns:4.1.89.Final
                         \\--- io.vertx:vertx-core:3.9.16
                              \\--- io.vertx:vertx-lang-kotlin:3.9.16
                                   \\--- compileClasspath
                    """.strip());
                assertThat(row.getDepth()).isEqualTo(3);
            }),
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'io.vertx:vertx-lang-kotlin:3.9.16'
              }
              """,
            """
              plugins {
                  id 'java'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  /*~~(io.netty:netty-codec-dns:4.1.89.Final)~~>*/implementation 'io.vertx:vertx-lang-kotlin:3.9.16'
              }
              """
          )
        );
    }

    @Test
    void dependencyGraphWithCustomConfiguration() {
        rewriteRun(
          spec -> spec
            .recipe(new DependencyInsight("junit", "junit", null, "testCompileClasspath"))
            .dataTable(DependenciesInUse.Row.class, rows -> {
                assertThat(rows).hasSize(1);
                DependenciesInUse.Row row = rows.get(0);
                assertThat(row.getDependencyGraph()).isEqualTo(
                  """
                    junit:junit:4.13
                    \\--- testCompileClasspath
                    """.strip());
                assertThat(row.getDepth()).isEqualTo(0);
            }),
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  testImplementation 'junit:junit:4.13'
              }
              """,
            """
              plugins {
                  id 'java'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  /*~~(junit:junit:4.13)~~>*/testImplementation 'junit:junit:4.13'
              }
              """
          )
        );
    }

    @Test
    void dependencyGraphForDuplicateTransitiveWithSameGroupIdArtifactIdVersion() {
        rewriteRun(
          spec -> spec
            .recipe(new DependencyInsight("jakarta.annotation", "jakarta.annotation-api", null, "compileClasspath"))
            .dataTable(DependenciesInUse.Row.class, rows -> {
                //assertThat(rows).hasSize(1);
                DependenciesInUse.Row row1 = rows.getFirst();
                assertThat(row1.getDependencyGraph()).isEqualTo(
                  """
                    jakarta.annotation:jakarta.annotation-api:1.3.5
                    \\--- org.glassfish.jersey.core:jersey-common:2.35
                         \\--- org.glassfish.jersey.core:jersey-client:2.35
                              \\--- org.glassfish.jersey.core:jersey-server:2.35
                                   \\--- compileClasspath
                    """.strip());
                assertThat(row1.getDepth()).isEqualTo(3);
                DependenciesInUse.Row row2 = rows.getFirst();
                assertThat(row2.getDependencyGraph()).isEqualTo(
                  """
                    jakarta.annotation:jakarta.annotation-api:1.3.5
                    \\--- org.glassfish.jersey.core:jersey-common:2.35
                         \\--- org.glassfish.jersey.core:jersey-client:2.35
                              \\--- org.glassfish.jersey.core:jersey-server:2.35
                                   \\--- compileClasspath
                    """.strip());
                assertThat(row2.getDepth()).isEqualTo(3);
            }),
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'org.glassfish.jersey.core:jersey-server:2.35'
              }
              """,
            """
              plugins {
                  id 'java'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  /*~~(jakarta.annotation:jakarta.annotation-api:1.3.5)~~>*/implementation 'org.glassfish.jersey.core:jersey-server:2.35'
              }
              """
          )
        );
    }
}
