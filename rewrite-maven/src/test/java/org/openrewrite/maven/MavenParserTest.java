/*
 * Copyright 2020 the original author or authors.
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

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.*;
import org.openrewrite.maven.http.OkHttpSender;
import org.openrewrite.maven.internal.MavenParsingException;
import org.openrewrite.maven.tree.*;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;
import org.openrewrite.tree.ParseError;

import java.net.InetAddress;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static org.assertj.core.api.Assertions.*;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class MavenParserTest implements RewriteTest {

    @Test
    void rangeVersion() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>[4.11]</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void prerequisites() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <prerequisites>
                    <maven>3.0</maven>
                </prerequisites>
                <dependencies>
                    <dependency>
                      <groupId>org.apache.maven.reporting</groupId>
                      <artifactId>maven-reporting-api</artifactId>
                      <version>${project.prerequisites.maven}</version>
                    </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void skipDependencyResolution() {
        rewriteRun(
          spec -> spec.parser(MavenParser.builder().skipDependencyResolution(true)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>foo</groupId>
                    <artifactId>bar</artifactId>
                    <version>42</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2603")
    @Test
    void repositoryWithPropertyPlaceholder() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencies>
                      <dependency>
                          <groupId>org.eclipse.persistence</groupId>
                          <artifactId>org.eclipse.persistence.moxy</artifactId>
                          <version>4.0.0</version>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void repositoryWithPropertyPlaceholders() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-project</artifactId>
                  <version>1</version>
                  <properties>
                        <my.artifact.repo.url>https://my.artifact.repo.com</my.artifact.repo.url>
                  </properties>
                  <repositories>
                      <repository>
                          <id>my-artifact-repo</id>
                          <url>${my.artifact.repo.url}</url>
                      </repository>
                  </repositories>
              </project>
              """,
            spec -> spec.afterRecipe(p ->
              assertThat(p.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom().getRepositories())
                .map(MavenRepository::getUri)
                .describedAs("Property placeholder in repository URL resolved")
                .singleElement()
                .isEqualTo("https://my.artifact.repo.com"))
          )
        );
    }

    @Test
    void repositoryWithPropertyFromParent() {
        rewriteRun(
          mavenProject("parent", pomXml(
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-parent</artifactId>
                  <version>1</version>
                  <properties>
                        <my.artifact.repo.url>https://my.artifact.repo.com</my.artifact.repo.url>
                  </properties>
              </project>
              """
          )),
          mavenProject("child", pomXml(
            """
              <project>
                  <parent>
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-parent</artifactId>
                      <version>1</version>
                  </parent>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-child</artifactId>
                  <version>1</version>
                  <repositories>
                      <repository>
                          <id>my-artifact-repo</id>
                          <url>${my.artifact.repo.url}</url>
                      </repository>
                  </repositories>
              </project>
              """,
            spec -> spec.afterRecipe(p ->
              assertThat(p.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom().getRepositories())
                .map(MavenRepository::getUri)
                .describedAs("Property placeholder in repository URL resolved")
                .singleElement()
                .isEqualTo("https://my.artifact.repo.com"))
          ))
        );
    }

    @Test
    void invalidRange() {
        assertThatExceptionOfType(MavenParsingException.class).isThrownBy(() ->
          rewriteRun(
            // Counter to what Maven does most of the time, the last range "wins" when the same dependency
            // is defined twice with a range.
            pomXml(
              """
                <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>

                  <dependencies>
                    <dependency>
                      <groupId>junit</groupId>
                      <artifactId>junit</artifactId>
                      <version>[88.7,90.9)</version>
                    </dependency>
                  </dependencies>
                </project>
                """
            )
          )
        ).withMessage("Could not resolve version for [GroupArtifact(groupId=junit, artifactId=junit)] matching version requirements RangeSet={[88.7,90.9)}");
    }

    @Test
    void differentRangeVersionInDependency() {
        rewriteRun(
          mavenProject("dep",
            pomXml(
              """
                    <project>
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-dep</artifactId>
                      <version>1</version>
                      <dependencies>
                        <dependency>
                          <groupId>junit</groupId>
                          <artifactId>junit</artifactId>
                          <version>[4.5,4.9]</version>
                        </dependency>
                      </dependencies>
                    </project>
                """,
              spec -> spec.afterRecipe(p -> {
                  var results = p.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                  var dependency = results.findDependencies("junit", "junit", Scope.Compile).getFirst();
                  assertThat(dependency.getVersion()).isEqualTo("4.9");
              })
            )
          ),
          mavenProject("app",
            pomXml(
              """
                    <project>
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-app</artifactId>
                      <version>1</version>
                      <dependencies>
                        <dependency>
                          <groupId>com.mycompany.app</groupId>
                          <artifactId>my-dep</artifactId>
                          <version>1</version>
                        </dependency>
                        <dependency>
                          <groupId>junit</groupId>
                          <artifactId>junit</artifactId>
                          <version>[4.5,4.9]</version>
                        </dependency>
                      </dependencies>
                    </project>
                """,
              spec -> spec.afterRecipe(p -> {
                  var results = p.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                  var dependency = results.findDependencies("junit", "junit", Scope.Compile).getFirst();
                  assertThat(dependency.getVersion()).isEqualTo("4.9");
              })
            )
          )
        );
    }

    @Test
    void differentRangeVersionInParent() {
        rewriteRun(
          mavenProject("parent",
            pomXml(
              """
                    <project>
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-parent</artifactId>
                      <version>1</version>
                      <dependencies>
                        <dependency>
                          <groupId>junit</groupId>
                          <artifactId>junit</artifactId>
                          <version>[4.5,4.6]</version>
                        </dependency>
                      </dependencies>
                    </project>
                """,
              spec -> spec.afterRecipe(p -> {
                  var results = p.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                  var dependency = results.findDependencies("junit", "junit", Scope.Compile).getFirst();
                  assertThat(dependency.getVersion()).isEqualTo("4.6");
              })
            )
          ),
          mavenProject("app",
            pomXml(
              """
                    <project>
                      <modelVersion>4.0.0</modelVersion>
                      <parent>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-parent</artifactId>
                        <version>1</version>
                        <relativePath>..</relativePath>
                      </parent>
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-app</artifactId>
                      <version>1</version>
                      <dependencies>
                        <dependency>
                          <groupId>junit</groupId>
                          <artifactId>junit</artifactId>
                          <version>[4.8,4.9]</version>
                        </dependency>
                      </dependencies>
                    </project>
                """,
              spec -> spec.afterRecipe(p -> {
                  var results = p.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                  var dependency = results.findDependencies("junit", "junit", Scope.Compile).getFirst();
                  assertThat(dependency.getVersion()).isEqualTo("4.9");
              })
            )
          )
        );
    }


    @Test
    void transitiveDependencyVersionDeterminedByBom() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>app</artifactId>
                  <version>0.0.1</version>
                  <dependencies>
                      <dependency>
                          <groupId>org.neo4j</groupId>
                          <artifactId>neo4j-ogm-core</artifactId>
                          <version>3.2.21</version>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void parentVersionRange() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.managed.test</groupId>
                  <artifactId>a</artifactId>
                  <version>1.0.0</version>
                  <parent>
                      <groupId>com.fasterxml.jackson</groupId>
                      <artifactId>jackson-parent</artifactId>
                      <version>[2.9.1,2.10.0)</version>
                  </parent>
                  <dependencies>
                      <dependency>
                          <groupId>junit</groupId>
                          <artifactId>junit</artifactId>
                          <version>4.11</version>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void guava25() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>app</artifactId>
                  <version>0.0.1</version>
                  <dependencies>
                      <dependency>
                          <groupId>com.google.guava</groupId>
                          <artifactId>guava</artifactId>
                          <version>25.0-android</version>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void rewriteCircleci() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>app</artifactId>
                  <version>0.0.1</version>
                  <dependencies>
                      <dependency>
                          <groupId>org.openrewrite.recipe</groupId>
                          <artifactId>rewrite-circleci</artifactId>
                          <version>1.1.0</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.beforeRecipe(pomXml ->
              assertThat(pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow()
                .getDependencies().get(Scope.Runtime).stream().map(ResolvedDependency::getArtifactId))
                .contains("rewrite-yaml")
            )
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1085")
    @Test
    void parseDependencyManagementWithNoVersion() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>com.google.guava</groupId>
                                <artifactId>guava</artifactId>
                                <exclusions>
                                    <exclusion>
                                        <groupId>org.springframework</groupId>
                                        <artifactId>spring-core</artifactId>
                                    </exclusion>
                                </exclusions>
                            </dependency>
                        </dependencies>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>14.0</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.afterRecipe(pomXml ->
              assertThat(pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow()
                .findDependencies("com.google.guava", "guava", null).getFirst().getVersion())
                .isEqualTo("14.0")
            )
          )
        );
    }

    @Test
    void parseMergeExclusions() {
        rewriteRun(
          mavenProject("my-dep",
            pomXml(
              """
                    <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-dep</artifactId>
                        <version>1</version>
                        <dependencies>
                            <dependency>
                              <groupId>com.google.guava</groupId>
                              <artifactId>guava</artifactId>
                              <version>14.0</version>
                            </dependency>
                            <dependency>
                              <groupId>org.slf4j</groupId>
                              <artifactId>slf4j-api</artifactId>
                              <version>1.7.20</version>
                            </dependency>
                        </dependencies>
                    </project>
                """
            )),
          mavenProject("my-app",
            pomXml(
              """
                    <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                        <dependencyManagement>
                              <dependencies>
                                  <dependency>
                                      <groupId>com.mycompany.app</groupId>
                                      <artifactId>my-dep</artifactId>
                                      <exclusions>
                                          <exclusion>
                                              <groupId>com.google.guava</groupId>
                                              <artifactId>guava</artifactId>
                                          </exclusion>
                                      </exclusions>
                                  </dependency>
                              </dependencies>
                        </dependencyManagement>
                        <dependencies>
                            <dependency>
                              <groupId>com.mycompany.app</groupId>
                              <artifactId>my-dep</artifactId>
                              <version>1</version>
                              <exclusions>
                                  <exclusion>
                                      <groupId>org.slf4j</groupId>
                                      <artifactId>slf4j-api</artifactId>
                                  </exclusion>
                              </exclusions>
                            </dependency>
                        </dependencies>
                    </project>
                """,
              spec -> spec.afterRecipe(pomXml ->
                //With one exclusion in the dependency and one in the managed dependency, both transitive dependencies
                //should be excluded.
                assertThat(pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow()
                  .getDependencies().get(Scope.Compile).size()).isEqualTo(1)
              )
            )
          )
        );
    }

    @Test
    void repositoryWithPropertyPlaceHolders() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                  <properties>
                      <repo-id>coolId</repo-id>
                      <repo-url>https://repository.apache.org/content/repositories/snapshots</repo-url>
                  </properties>
                  <repositories>
                      <repository>
                        <id>${repo-id}</id>
                        <url>${repo-url}</url>
                      </repository>
                  </repositories>
              </project>
              """,
            spec -> spec.afterRecipe(pomXml -> {
                assertThat(pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom()
                  .getRepositories().getFirst().getId())
                  .isEqualTo("coolId");
                assertThat(pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom()
                  .getRepositories().getFirst().getUri())
                  .isEqualTo("https://repository.apache.org/content/repositories/snapshots");
            })
          )
        );
    }

    @SuppressWarnings("CheckDtdRefs")
    @Test
    void parse() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>

                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <packaging>pom</packaging>
                  <developers>
                      <developer>
                          <name>Trygve Laugst&oslash;l</name>
                      </developer>
                  </developers>

                  <dependencies>
                    <dependency>
                      <groupId>org.junit.jupiter</groupId>
                      <artifactId>junit-jupiter-api</artifactId>
                      <version>5.7.0</version>
                      <scope>test</scope>
                    </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.afterRecipe(pomXml -> {
                assertThat(pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getDependencies()
                  .get(Scope.Test).getFirst().getLicenses().getFirst().getType())
                  .isEqualTo(License.Type.Eclipse);
                assertThat(pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getDependencies()
                  .get(Scope.Test).getFirst().getType())
                  .isEqualTo("jar");
                assertThat(pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom().getPackaging())
                  .isEqualTo("pom");
            })
          )
        );
    }

    // example from https://repo1.maven.org/maven2/org/openid4java/openid4java-parent/0.9.6/openid4java-parent-0.9.6.pom
    @Test
    void emptyArtifactPolicy() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <repositories>
                      <repository>
                          <id>alchim.snapshots</id>
                          <name>Achim Repository Snapshots</name>
                          <url>http://alchim.sf.net/download/snapshots</url>
                          <snapshots/>
                      </repository>
                  </repositories>
              </project>
              """
          )
        );
    }

    @Test
    void handlesRepositories() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>

                  <groupId>org.openrewrite.maven</groupId>
                  <artifactId>single-project</artifactId>
                  <version>0.1.0-SNAPSHOT</version>

                  <dependencies>
                      <dependency>
                          <groupId>com.google.guava</groupId>
                          <artifactId>guava</artifactId>
                          <version>29.0-jre</version>
                      </dependency>
                  </dependencies>

                  <repositories>
                      <repository>
                          <id>jcenter</id>
                          <name>JCenter</name>
                          <url>https://jcenter.bintray.com/</url>
                      </repository>
                  </repositories>
              </project>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/198")
    @Test
    void handlesPropertiesInDependencyScope() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>

                  <groupId>org.openrewrite.maven</groupId>
                  <artifactId>single-project</artifactId>
                  <version>0.1.0-SNAPSHOT</version>

                  <properties>
                      <dependency.scope>compile</dependency.scope>
                  </properties>

                  <dependencies>
                      <dependency>
                          <groupId>com.google.guava</groupId>
                          <artifactId>guava</artifactId>
                          <version>29.0-jre</version>
                          <scope>${dependency.scope}</scope>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.afterRecipe(pomXml ->
              assertThat(pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getDependencies()
                .get(Scope.Compile))
                .hasSize(7)
            )
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/199")
    @Test
    void continueOnInvalidScope() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>org.openrewrite.maven</groupId>
                  <artifactId>single-project</artifactId>
                  <version>0.1.0-SNAPSHOT</version>
                  <dependencies>
                      <dependency>
                          <groupId>com.google.guava</groupId>
                          <artifactId>guava</artifactId>
                          <version>29.0-jre</version>
                          <scope>${dependency.scope}</scope>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/135")
    @Test
    void selfRecursiveParent() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>

                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>

                  <parent>
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-app</artifactId>
                      <version>1</version>
                  </parent>
              </project>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/135")
    @Test
    void selfRecursiveDependency() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>

                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>

                  <dependencies>
                      <dependency>
                          <groupId>com.mycompany.app</groupId>
                          <artifactId>my-app</artifactId>
                          <version>1</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.afterRecipe(pomXml ->
              // Maven itself would respond to this pom with a fatal error.
              // So long as we don't produce an AST with cycles it's OK
              assertThat(pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getDependencies().get(Scope.Compile)).hasSize(1)
            )
          )
        );
    }

    @Test
    void managedDependenciesInParentInfluenceTransitives() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.foo</groupId>
                  <artifactId>parent</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>org.glassfish.jaxb</groupId>
                              <artifactId>jaxb-runtime</artifactId>
                              <version>2.3.3</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          ),
          mavenProject("app",
            pomXml(
              """
                    <project>
                        <parent>
                            <groupId>com.foo</groupId>
                            <artifactId>parent</artifactId>
                            <version>1</version>
                        </parent>
                        <groupId>com.foo</groupId>
                        <artifactId>app</artifactId>
                        <dependencies>
                            <dependency>
                                <groupId>org.hibernate</groupId>
                                <artifactId>hibernate-core</artifactId>
                                <version>5.4.28.Final</version>
                            </dependency>
                        </dependencies>
                    </project>
                """,
              spec -> spec.afterRecipe(pomXml ->
                assertThat(pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getDependencies().get(Scope.Compile)
                  .stream().map(dep -> dep.getArtifactId() + ":" + dep.getVersion()))
                  .contains("jaxb-runtime:2.3.3")
              )
            )
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/323")
    @Test
    void inheritScopeFromDependencyManagement() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                      <dependencies>
                         <dependency>
                              <groupId>org.junit.jupiter</groupId>
                              <artifactId>junit-jupiter</artifactId>
                              <version>5.7.1</version>
                              <scope>test</scope>
                          </dependency>
                          <dependency>
                              <groupId>com.google.guava</groupId>
                              <artifactId>guava</artifactId>
                              <version>29.0-jre</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>org.junit.jupiter</groupId>
                          <artifactId>junit-jupiter</artifactId>
                      </dependency>
                      <dependency>
                          <groupId>com.google.guava</groupId>
                          <artifactId>guava</artifactId>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.afterRecipe(pomXml -> {
                assertThat(pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getDependencies()
                  .get(Scope.Test).stream().map(ResolvedDependency::getArtifactId))
                  .contains("junit-jupiter", "guava");
                assertThat(pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getDependencies()
                  .get(Scope.Compile).stream().map(ResolvedDependency::getArtifactId))
                  .doesNotContain("junit-jupiter");
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/323")
    @Test
    void dependencyScopeTakesPrecedenceOverDependencyManagementScope() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                      <dependencies>
                         <dependency>
                              <groupId>org.junit.jupiter</groupId>
                              <artifactId>junit-jupiter</artifactId>
                              <version>5.7.1</version>
                              <scope>test</scope>
                          </dependency>
                          <dependency>
                              <groupId>com.google.guava</groupId>
                              <artifactId>guava</artifactId>
                              <version>29.0-jre</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>org.junit.jupiter</groupId>
                          <artifactId>junit-jupiter</artifactId>
                          <scope>compile</scope>
                      </dependency>
                      <dependency>
                          <groupId>com.google.guava</groupId>
                          <artifactId>guava</artifactId>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.afterRecipe(pomXml ->
              assertThat(pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getDependencies()
                .get(Scope.Compile).stream()
                .filter(dep -> dep.getDepth() == 0)
                .map(ResolvedDependency::getArtifactId))
                .containsExactlyInAnyOrder("junit-jupiter", "guava")
            )
          )
        );
    }

    @Test
    void mirrorsAndAuth() throws Exception {
        // Set up a web server that returns 401 to any request without an Authorization header corresponding to specific credentials
        // Exceptions in the console output are due to MavenPomDownloader attempting to access via https first before falling back to http
        var username = "admin";
        var password = "password";
        try (MockWebServer mockRepo = new MockWebServer()) {
            // TLS server setup based on https://github.com/square/okhttp/blob/master/okhttp-tls/README.md
            String localhost = InetAddress.getByName("localhost").getCanonicalHostName();
            HeldCertificate localhostCertificate = new HeldCertificate.Builder()
              .addSubjectAlternativeName(localhost)
              .build();
            HandshakeCertificates serverCertificates = new HandshakeCertificates.Builder()
              .heldCertificate(localhostCertificate)
              .build();
            mockRepo.useHttps(serverCertificates.sslSocketFactory(), false);

            mockRepo.setDispatcher(new Dispatcher() {
                @Override
                public MockResponse dispatch(RecordedRequest request) {
                    MockResponse resp = new MockResponse();
                    if (!Objects.equals(
                      request.getHeader("Authorization"),
                      "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes()))) {
                        return resp.setResponseCode(401);
                    } else {
                        if (!"HEAD".equalsIgnoreCase(request.getMethod())) {
                            //language=xml
                            resp.setBody("""
                              <project>
                                <modelVersion>4.0.0</modelVersion>

                                <groupId>com.foo</groupId>
                                <artifactId>bar</artifactId>
                                <version>1.0.0</version>

                              </project>
                              """
                            );
                        }
                        return resp.setResponseCode(200);
                    }
                }
            });

            mockRepo.start();
            var mavenCtx = MavenExecutionContextView.view(new InMemoryExecutionContext(t -> {
                throw new RuntimeException(t);
            }));
            var settings = MavenSettings.parse(Parser.Input.fromString(Path.of("settings.xml"),
              //language=xml
              """
                <settings>
                    <mirrors>
                        <mirror>
                            <mirrorOf>*</mirrorOf>
                            <name>repo</name>
                            <url>http://%s:%d</url>
                            <id>repo</id>
                        </mirror>
                    </mirrors>
                    <servers>
                        <server>
                            <id>repo</id>
                            <username>%s</username>
                            <password>%s</password>
                        </server>
                    </servers>
                </settings>
                """.formatted(mockRepo.getHostName(), mockRepo.getPort(), username, password)
            ), mavenCtx);
            mavenCtx.setMavenSettings(settings);

            // TLS client setup (just make it trust the self-signed certificate)
            HandshakeCertificates clientCertificates = new HandshakeCertificates.Builder()
              .addTrustedCertificate(localhostCertificate.certificate())
              .build();
            OkHttpClient client = new OkHttpClient.Builder()
              .sslSocketFactory(clientCertificates.sslSocketFactory(), clientCertificates.trustManager())
              .build();
            var ctx = new HttpSenderExecutionContextView(mavenCtx).setHttpSender(new OkHttpSender(client));

            var maven = MavenParser.builder().build().parse(
              ctx,
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                                 <groupId>org.openrewrite.test</groupId>
                    <artifactId>foo</artifactId>
                    <version>0.1.0-SNAPSHOT</version>
                                 <dependencies>
                        <dependency>
                            <groupId>com.foo</groupId>
                            <artifactId>bar</artifactId>
                            <version>1.0.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """
            ).findFirst().orElseThrow(() -> new IllegalArgumentException("Could not parse as XML"));

            assertThat(mockRepo.getRequestCount())
              .as("The mock repository received no requests. Applying mirrors is probably broken")
              .isGreaterThan(0);

            assertThat(maven.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getDependencies().get(Scope.Compile))
              .hasSize(1)
              .matches(deps -> "com.foo".equals(deps.getFirst().getGroupId()) &&
                "bar".equals(deps.getFirst().getArtifactId()));
            mockRepo.shutdown();
        }
    }

    // a depends on d. The version number is a property specified in a's parent, b
    // b gets part of the version number for d from a property specified in b's parent, c
    @Issue("https://github.com/openrewrite/rewrite/issues/95")
    @Test
    void recursivePropertyFromParentPoms() {
        rewriteRun(
          mavenProject("a",
            pomXml(
              """
                    <project>
                        <parent>
                            <groupId>org.openrewrite.maven</groupId>
                            <artifactId>b</artifactId>
                            <version>0.1.0-SNAPSHOT</version>
                            <relativePath />
                        </parent>
                        <artifactId>a</artifactId>
                        <dependencies>
                            <dependency>
                                <groupId>org.openrewrite.maven</groupId>
                                <artifactId>d</artifactId>
                                <version>${d.version}</version>
                            </dependency>
                        </dependencies>
                    </project>
                """,
              spec -> spec.afterRecipe(pomXml ->
                assertThat(pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getDependencies()
                  .get(Scope.Compile).getFirst().getVersion())
                  .isEqualTo("0.1.0-SNAPSHOT")
              )
            ),
            mavenProject("b",
              pomXml(
                """
                      <project>
                          <parent>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>c</artifactId>
                              <version>0.1.0-SNAPSHOT</version>
                              <relativePath />
                          </parent>
                          <artifactId>b</artifactId>
                          <packaging>pom</packaging>
                          <properties>
                              <d.version>0.1.0${d.version.snapshot}</d.version>
                          </properties>
                      </project>
                  """
              )
            ),
            mavenProject("c",
              pomXml(
                """
                      <project>
                          <artifactId>c</artifactId>
                          <groupId>org.openrewrite.maven</groupId>
                          <version>0.1.0-SNAPSHOT</version>
                          <packaging>pom</packaging>
                          <properties>
                              <d.version.snapshot>-SNAPSHOT</d.version.snapshot>
                          </properties>
                      </project>
                  """
              )
            ),
            mavenProject("d",
              pomXml(
                """
                      <project>
                          <groupId>org.openrewrite.maven</groupId>
                          <artifactId>d</artifactId>
                          <version>0.1.0-SNAPSHOT</version>
                          <properties>
                              <maven.compiler.source>1.8</maven.compiler.source>
                              <maven.compiler.target>1.8</maven.compiler.target>
                          </properties>
                      </project>
                  """
              )
            )
          )
        );
    }

    @Test
    void nestedParentWithDownloadedParent() {
        rewriteRun(
          mavenProject("root",
            pomXml(
              """
                    <project>
                        <parent>
                            <groupId>org.apache</groupId>
                            <artifactId>apache</artifactId>
                            <version>16</version>
                            <relativePath/>
                        </parent>
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>parent</artifactId>
                        <version>0.1.0-SNAPSHOT</version>
                    </project>
                """,
              spec -> spec.path("parent/pom.xml")
            ),
            pomXml(
              """
                    <project>
                        <parent>
                            <groupId>org.openrewrite.maven</groupId>
                            <artifactId>parent</artifactId>
                            <version>0.1.0-SNAPSHOT</version>
                            <relativePath>parent/pom.xml</relativePath>
                        </parent>
                        <artifactId>root</artifactId>
                    </project>
                """,
              spec -> spec.path("pom.xml")
            )
          )
        );
    }

    // a depends on d without specifying version number. a's parent is b
    // b imports c into its dependencyManagement section
    // c's dependencyManagement specifies the version of d to use
    // So if all goes well a will have the version of d from c's dependencyManagement
    @Issue("https://github.com/openrewrite/rewrite/issues/124")
    @Test
    void indirectBomImportedFromParent() {
        rewriteRun(
          mavenProject("a",
            pomXml(
              """
                    <project>
                        <parent>
                            <groupId>org.openrewrite.maven</groupId>
                            <artifactId>b</artifactId>
                            <version>0.1.0-SNAPSHOT</version>
                            <relativePath />
                        </parent>
                        <artifactId>a</artifactId>
                        <dependencies>
                            <dependency>
                                <groupId>org.openrewrite.maven</groupId>
                                <artifactId>d</artifactId>
                            </dependency>
                        </dependencies>
                    </project>
                """,
              spec -> spec.afterRecipe(pomXml ->
                assertThat(pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getDependencies()
                  .get(Scope.Compile).getFirst().getVersion())
                  .isEqualTo("0.1.0-SNAPSHOT")
              )
            )
          ),
          mavenProject("b",
            pomXml(
              """
                    <project>
                        <modelVersion>4.0.0</modelVersion>

                        <artifactId>b</artifactId>
                        <groupId>org.openrewrite.maven</groupId>
                        <version>0.1.0-SNAPSHOT</version>
                        <packaging>pom</packaging>

                        <properties>
                            <maven.compiler.source>1.8</maven.compiler.source>
                            <maven.compiler.target>1.8</maven.compiler.target>
                        </properties>

                        <dependencyManagement>
                            <dependencies>
                                <dependency>
                                    <groupId>org.openrewrite.maven</groupId>
                                    <artifactId>c</artifactId>
                                    <version>0.1.0-SNAPSHOT</version>
                                    <type>pom</type>
                                    <scope>import</scope>
                                </dependency>
                            </dependencies>
                        </dependencyManagement>
                    </project>
                """
            )
          ),
          mavenProject("c",
            pomXml(
              """
                    <project>
                        <modelVersion>4.0.0</modelVersion>

                        <artifactId>c</artifactId>
                        <groupId>org.openrewrite.maven</groupId>
                        <version>0.1.0-SNAPSHOT</version>
                        <packaging>pom</packaging>

                        <dependencyManagement>
                            <dependencies>
                                <dependency>
                                    <groupId>org.openrewrite.maven</groupId>
                                    <artifactId>d</artifactId>
                                    <version>0.1.0-SNAPSHOT</version>
                                </dependency>
                            </dependencies>
                        </dependencyManagement>
                    </project>
                """
            )
          ),
          mavenProject("d",
            pomXml(
              """
                    <project>
                        <modelVersion>4.0.0</modelVersion>

                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>d</artifactId>
                        <version>0.1.0-SNAPSHOT</version>

                        <properties>
                            <maven.compiler.source>1.8</maven.compiler.source>
                            <maven.compiler.target>1.8</maven.compiler.target>
                        </properties>
                    </project>
                """
            )
          )
        );
    }

    @Nested
    @SuppressWarnings("LanguageMismatch")
    class Profiles {

        //language=xml
        private final String parent = """
          <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.openrewrite.maven</groupId>
              <artifactId>parent</artifactId>
              <version>0.1.0-SNAPSHOT</version>
              <packaging>pom</packaging>
              <profiles>
                  <profile>
                      <id>active-profile-1</id>
                      <activation>
                          <activeByDefault>true</activeByDefault>
                      </activation>
                      <properties>
                          <d.version>2.0.9</d.version>
                      </properties>
                      <dependencies>
                          <dependency>
                              <groupId>org.slf4j</groupId>
                              <artifactId>slf4j-api</artifactId>
                              <version>${d.version}</version>
                          </dependency>
                      </dependencies>
                  </profile>
                  <profile>
                      <id>active-profile-2</id>
                      <activation>
                          <activeByDefault>true</activeByDefault>
                      </activation>
                      <properties>
                          <e.version>2.11.0</e.version>
                      </properties>
                      <dependencies>
                          <dependency>
                              <groupId>commons-io</groupId>
                              <artifactId>commons-io</artifactId>
                              <version>${e.version}</version>
                          </dependency>
                      </dependencies>
                  </profile>
              </profiles>
          </project>
          """;

        //language=xml
        private final String child = """
          <project>
              <parent>
                  <groupId>org.openrewrite.maven</groupId>
                  <artifactId>parent</artifactId>
                  <version>0.1.0-SNAPSHOT</version>
                  <relativePath />
              </parent>
              <groupId>org.openrewrite.maven</groupId>
              <artifactId>a</artifactId>
          </project>
          """;


        @DisplayName("activeByDefault=true profiles from a POM should be active " +
          "unless there is another active profile _from the same POM file_")
        @Issue("https://github.com/openrewrite/rewrite/issues/4269")
        @Test
        void activeByDefaultWithoutPomLocalActiveProfile() {
            rewriteRun(
              mavenProject("c",
                pomXml(
                  parent
                )
              ),
              pomXml(
                child, spec -> spec.afterRecipe(pomXml -> {
                    Map<String, List<ResolvedDependency>> deps =
                      pomXml.getMarkers()
                        .findFirst(MavenResolutionResult.class)
                        .orElseThrow()
                        .getDependencies()
                        .get(Scope.Compile)
                        .stream()
                        .collect(groupingBy(ResolvedDependency::getArtifactId));

                    assertThat(deps)
                      .hasEntrySatisfying("slf4j-api", rds -> assertThat(rds)
                        .singleElement().extracting(ResolvedDependency::getVersion).isEqualTo("2.0.9"))
                      .hasEntrySatisfying("commons-io", rds -> assertThat(rds)
                        .singleElement().extracting(ResolvedDependency::getVersion).isEqualTo("2.11.0"));
                })
              )
            );
        }

        @DisplayName("activeByDefault=true profiles from a POM should not be active" +
          " if there is another active profile _from the same POM file_")
        @Issue("https://github.com/openrewrite/rewrite/issues/4269")
        @Test
        void activeByDefaultWithPomLocalActiveProfile() {
            rewriteRun(
              recipeSpec -> recipeSpec
                .executionContext(MavenExecutionContextView.view(new InMemoryExecutionContext()))
                .parser(MavenParser.builder().activeProfiles("active-profile-1")),
              mavenProject("c",
                pomXml(
                  parent
                )
              ),
              pomXml(
                child, spec -> spec.afterRecipe(pomXml -> {
                      Map<String, List<ResolvedDependency>> deps =
                        pomXml.getMarkers()
                          .findFirst(MavenResolutionResult.class)
                          .orElseThrow()
                          .getDependencies()
                          .get(Scope.Compile)
                          .stream()
                          .collect(groupingBy(ResolvedDependency::getArtifactId));

                      assertThat(deps)
                        .hasEntrySatisfying("slf4j-api", rds -> assertThat(rds)
                          .singleElement().extracting(ResolvedDependency::getVersion).isEqualTo("2.0.9"))
                        .doesNotContainKey("commons-io");
                  }
                )
              )
            );
        }

        @Test
        void settingsActiveProfiles() {
            var mavenCtx = MavenExecutionContextView.view(new InMemoryExecutionContext(t -> {
                throw new RuntimeException(t);
            }));
            var settings = MavenSettings.parse(Parser.Input.fromString(Path.of("settings.xml"),
              //language=xml
              """
                <settings>
                  <activeProfiles>
                      <activeProfile>foo</activeProfile>
                  </activeProfiles>
                </settings>
                """
            ), mavenCtx);
            mavenCtx.setMavenSettings(settings);

            rewriteRun(
              recipeSpec -> recipeSpec.executionContext(mavenCtx),
              pomXml(
                """
                  <project>
                      <groupId>some.group</groupId>
                      <artifactId>some.artifact</artifactId>
                      <version>1-SNAPSHOT</version>
                      <dependencies>
                          <dependency>
                              <groupId>commons-io</groupId>
                              <artifactId>commons-io</artifactId>
                          </dependency>
                      </dependencies>
                      <profiles>
                          <profile>
                              <id>foo</id>
                              <properties>
                                  <commons-io.version>2.11.0</commons-io.version>
                              </properties>
                              <dependencyManagement>
                                  <dependencies>
                                      <dependency>
                                          <groupId>commons-io</groupId>
                                          <artifactId>commons-io</artifactId>
                                          <version>${commons-io.version}</version>
                                      </dependency>
                                  </dependencies>
                              </dependencyManagement>
                          </profile>
                      </profiles>
                  </project>
                  """,
                spec -> spec.afterRecipe(pomXml -> {
                      Map<String, List<ResolvedDependency>> deps =
                        pomXml.getMarkers()
                          .findFirst(MavenResolutionResult.class)
                          .orElseThrow()
                          .getDependencies()
                          .get(Scope.Compile)
                          .stream()
                          .collect(groupingBy(ResolvedDependency::getArtifactId));

                      assertThat(deps)
                        .hasEntrySatisfying("commons-io", rds -> assertThat(rds)
                          .singleElement().extracting(ResolvedDependency::getVersion).isEqualTo("2.11.0"));
                  }
                )
              )
            );
        }
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/378")
    @Test
    void parseNotInProfileActivation() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>org.openrewrite.maven</groupId>
                  <artifactId>test</artifactId>
                  <version>0.1.0-SNAPSHOT</version>
                  <profiles>
                      <profile>
                        <id>repo-incode-work</id>
                        <properties>
                          <name>!skip.repo-incode-work</name>
                        </properties>
                      </profile>
                  </profiles>
              </project>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1427")
    @Test
    void parseEmptyActivationTag() {
        //noinspection DataFlowIssue
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>org.openrewrite.maven</groupId>
                  <artifactId>test</artifactId>
                  <version>0.1.0-SNAPSHOT</version>
                  <profiles>
                      <profile>
                        <id>repo-incode-work</id>
                        <properties>
                          <name>!skip.repo-incode-work</name>
                        </properties>
                        <activation/>
                      </profile>
                  </profiles>
              </project>
              """,
            spec -> spec.afterRecipe(pomXml ->
              assertThat(pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow()
                .getPom().getRequested().getProfiles().getFirst().getActivation())
                .isNull()
            )
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1427")
    @SuppressWarnings("CheckTagEmptyBody")
    @Test
    void parseEmptyValueActivationTag() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>org.openrewrite.maven</groupId>
                  <artifactId>test</artifactId>
                  <version>0.1.0-SNAPSHOT</version>
                  <profiles>
                      <profile>
                        <id>repo-incode-work</id>
                        <properties>
                          <name>!skip.repo-incode-work</name>
                        </properties>
                        <activation></activation>
                      </profile>
                  </profiles>
              </project>
              """,
            spec -> spec.afterRecipe(pomXml -> {
                ProfileActivation activation = pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow()
                  .getPom().getRequested().getProfiles().getFirst().getActivation();
                assertThat(activation).isNotNull();
                assertThat(activation.getActiveByDefault()).isNull();
                assertThat(activation.getJdk()).isNull();
                //noinspection DataFlowIssue
                assertThat(activation.getProperty()).isNull();
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1427")
    @Test
    void parseWithActivationTag() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>org.openrewrite.maven</groupId>
                  <artifactId>test</artifactId>
                  <version>0.1.0-SNAPSHOT</version>
                  <profiles>
                      <profile>
                        <id>repo-incode-work</id>
                        <properties>
                          <name>!skip.repo-incode-work</name>
                        </properties>
                        <activation>
                          <activeByDefault>true</activeByDefault>
                        </activation>
                      </profile>
                  </profiles>
              </project>
              """,
            spec -> spec.afterRecipe(pomXml -> {
                ProfileActivation activation = pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow()
                  .getPom().getRequested().getProfiles().getFirst().getActivation();
                assertThat(activation).isNotNull();
                assertThat(activation.getActiveByDefault()).isTrue();
            })
          )
        );
    }

    @Test
    void parentPomProfileProperty() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>org.openrewrite.maven</groupId>
                  <artifactId>multi-module-project-parent</artifactId>
                  <version>0.1.0-SNAPSHOT</version>
                  <packaging>pom</packaging>
                  <modules>
                      <module>a</module>
                  </modules>
                  <profiles>
                      <profile>
                        <id>appserverConfig-dev-2</id>
                        <activation>
                          <activeByDefault>true</activeByDefault>
                        </activation>
                        <properties>
                          <guava.version>29.0-jre</guava.version>
                        </properties>
                      </profile>
                  </profiles>
              </project>
              """
          ),
          mavenProject("a",
            pomXml(
              """
                    <project>
                        <parent>
                            <groupId>org.openrewrite.maven</groupId>
                            <artifactId>multi-module-project-parent</artifactId>
                            <version>0.1.0-SNAPSHOT</version>
                        </parent>
                        <artifactId>a</artifactId>
                        <dependencies>
                            <dependency>
                                <groupId>com.google.guava</groupId>
                                <artifactId>guava</artifactId>
                                <version>${guava.version}</version>
                            </dependency>
                        </dependencies>
                    </project>
                """,
              spec -> spec.afterRecipe(pomXml ->
                assertThat(pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getDependencies().get(Scope.Compile))
                  .hasSize(7)
                  .matches(deps -> "guava".equals(deps.getFirst().getArtifactId()) &&
                    "29.0-jre".equals(deps.getFirst().getVersion()))
              )
            )
          )
        );
    }

    // a depends on b
    // a-parent manages version of d to 0.1
    // b depends on d without specifying version
    // b-parent manages version of d to 0.2
    // Therefore the version of b that wins is 0.1
    @Disabled
    @Issue("https://github.com/openrewrite/rewrite/issues/376")
    @Test
    void dependencyManagementPropagatesToDependencies() {
        rewriteRun(
          mavenProject("a-parent",
            pomXml(
              """
                    <project>
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>d</artifactId>
                        <version>0.1.0-SNAPSHOT</version>
                    </project>
                """
            ),
            pomXml(
              """
                    <project>
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>a-parent</artifactId>
                        <version>0.1.0-SNAPSHOT</version>
                        <packaging>pom</packaging>

                        <dependencyManagement>
                            <dependencies>
                                <dependency>
                                    <groupId>org.openrewrite.maven</groupId>
                                    <artifactId>d</artifactId>
                                    <version>0.1.0-SNAPSHOT</version>
                                </dependency>
                            </dependencies>
                        </dependencyManagement>
                    </project>
                """
            ),
            mavenProject("a",
              pomXml(
                """
                      <project>
                          <parent>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>a-parent</artifactId>
                              <version>0.1.0-SNAPSHOT</version>
                              <relativePath />
                          </parent>

                          <artifactId>a</artifactId>

                          <dependencies>
                              <dependency>
                                  <groupId>org.openrewrite.maven</groupId>
                                  <artifactId>b</artifactId>
                                  <version>0.1.0-SNAPSHOT</version>
                              </dependency>
                          </dependencies>
                      </project>
                  """,
                spec -> spec.afterRecipe(pomXml -> {
                    var compileDependencies = pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow()
                      .getDependencies().get(Scope.Compile);
                    assertThat(compileDependencies).hasSize(2);
                    assertThat(compileDependencies).anyMatch(it -> "b".equals(it.getArtifactId()) &&
                      "0.1.0-SNAPSHOT".equals(it.getVersion()));
                    assertThat(compileDependencies).anyMatch(it -> "d".equals(it.getArtifactId()) &&
                      "0.1.0-SNAPSHOT".equals(it.getVersion()));
                })
              ),
              mavenProject("b-parent",
                pomXml(
                  """
                        <project>
                            <groupId>org.openrewrite.maven</groupId>
                            <artifactId>d</artifactId>
                            <version>0.2.0-SNAPSHOT</version>
                        </project>
                    """
                ),
                pomXml(
                  """
                        <project>
                            <groupId>org.openrewrite.maven</groupId>
                            <artifactId>b-parent</artifactId>
                            <version>0.1.0-SNAPSHOT</version>
                            <packaging>pom</packaging>
                            <dependencyManagement>
                                <dependencies>
                                    <dependency>
                                        <groupId>org.openrewrite.maven</groupId>
                                        <artifactId>d</artifactId>
                                        <version>0.2.0-SNAPSHOT</version>
                                    </dependency>
                                </dependencies>
                            </dependencyManagement>
                        </project>
                    """
                ),
                mavenProject("b",
                  pomXml(
                    """
                          <project>
                              <parent>
                                  <groupId>org.openrewrite.maven</groupId>
                                  <artifactId>b-parent</artifactId>
                                  <version>0.1.0-SNAPSHOT</version>
                                  <relativePath />
                              </parent>

                              <artifactId>b</artifactId>

                              <dependencies>
                                  <dependency>
                                      <groupId>org.openrewrite.maven</groupId>
                                      <artifactId>d</artifactId>
                                  </dependency>
                              </dependencies>
                          </project>
                      """
                  )
                )
              )
            )
          )
        );
    }

    // a has a managed dependency on junit:junit:4.11
    // a has a dependency defined for junit:junit (version is managed to 4.11)
    // ------------------------
    // b has a managed dependency on junit:junit (with an exclusion on hamcrest, but does NOT define version)
    // b has a dependency on a
    // b does NOT have a direct dependency on junit.
    //
    // b -> a -> junit
    //
    // Resolve dependencies on b should include junit:junit:4.11 but NOT hamcrest.
    @Issue("https://github.com/openrewrite/rewrite/issues/1422")
    @Test
    void managedDependencyInTransitiveAndPom() {
        rewriteRun(
          mavenProject("a",
            pomXml(
              """
                    <project>
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.managed.test</groupId>
                        <artifactId>a</artifactId>
                        <version>1.0.0</version>
                        <packaging>jar</packaging>

                        <dependencyManagement>
                            <dependencies>
                                <dependency>
                                    <groupId>junit</groupId>
                                    <artifactId>junit</artifactId>
                                    <version>4.11</version>
                                </dependency>
                            </dependencies>
                        </dependencyManagement>

                        <dependencies>
                            <dependency>
                                <groupId>junit</groupId>
                                <artifactId>junit</artifactId>
                            </dependency>
                        </dependencies>
                    </project>
                """
            )
          ),
          mavenProject("b",
            pomXml(
              """
                    <project>
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.managed.test</groupId>
                        <artifactId>b</artifactId>
                        <version>1.0.0</version>
                        <packaging>jar</packaging>
                        <dependencyManagement>
                            <dependencies>
                                <dependency>
                                    <groupId>junit</groupId>
                                    <artifactId>junit</artifactId>
                                    <exclusions>
                                        <exclusion>
                                            <groupId>org.hamcrest</groupId>
                                            <artifactId>hamcrest-core</artifactId>
                                        </exclusion>
                                    </exclusions>
                                </dependency>
                            </dependencies>
                        </dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>com.managed.test</groupId>
                                <artifactId>a</artifactId>
                                <version>1.0.0</version>
                            </dependency>
                        </dependencies>
                    </project>
                """,
              spec -> spec.afterRecipe(pomXml -> {
                  var compileDependencies = pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow()
                    .getDependencies().get(Scope.Compile);
                  assertThat(compileDependencies).anyMatch(it -> "junit".equals(it.getArtifactId()) &&
                    "4.11".equals(it.getVersion()));
                  assertThat(compileDependencies).noneMatch(it -> "hamcrest-core".equals(it.getArtifactId()));
              })
            )
          )
        );
    }

    @Test
    void profileNoJdkActivation() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>

                  <profiles>
                      <profile>
                          <id>old-jdk</id>
                          <activation>
                              <jdk>1.5</jdk>
                          </activation>
                          <dependencies>
                              <dependency>
                                    <groupId>junit</groupId>
                                    <artifactId>junit</artifactId>
                                    <version>4.11</version>
                              </dependency>
                          </dependencies>
                      </profile>
                  </profiles>
              </project>
              """,
            spec -> spec.afterRecipe(pomXml ->
              assertThat(pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow()
                .getDependencies().get(Scope.Compile))
                .isEmpty()
            )
          )
        );
    }

    @Test
    void profileJdkSoftVersionActivation() {
        rewriteRun(
          pomXml(
            """
                  <project>
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-app</artifactId>
                      <version>1</version>
                      <profiles>
                          <profile>
                              <id>old-jdk</id>
                              <activation>
                                  <jdk>%s</jdk>
                              </activation>
                              <dependencies>
                                  <dependency>
                                        <groupId>junit</groupId>
                                        <artifactId>junit</artifactId>
                                        <version>4.11</version>
                                  </dependency>
                              </dependencies>
                          </profile>
                      </profiles>
                  </project>
              """.formatted(System.getProperty("java.version")),
            spec -> spec.afterRecipe(pomXml ->
              assertThat(pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getDependencies().get(Scope.Compile))
                .hasSize(2)
            )
          )
        );
    }

    @Test
    void cannotWidenScopeOfTransitiveDependency() {
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
                      <groupId>org.hamcrest</groupId>
                      <artifactId>hamcrest</artifactId>
                      <version>2.1</version>
                      <scope>compile</scope>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>org.apache.logging.log4j</groupId>
                    <artifactId>log4j-to-slf4j</artifactId>
                    <version>2.17.2</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            spec -> spec.afterRecipe(pomXml -> {
                MavenResolutionResult result = pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                List<ResolvedDependency> foundDependencies = result.findDependencies("org.hamcrest", "hamcrest", null);
                assertThat(foundDependencies).hasSize(0);
            })
          )
        );
    }

    @Test
    void cannotWidenScopeOfImplicitTransitiveDependency() {
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
                      <groupId>org.junit.vintage</groupId>
                      <artifactId>junit-vintage-engine</artifactId>
                      <version>5.7.2</version>
                      <scope>compile</scope>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>org.apache.logging.log4j</groupId>
                    <artifactId>log4j-to-slf4j</artifactId>
                    <version>2.17.2</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            spec -> spec.afterRecipe(pomXml -> {
                MavenResolutionResult result = pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                List<ResolvedDependency> foundDependencies = result.findDependencies("org.junit.vintage", "junit-vintage-engine", null);
                assertThat(foundDependencies).hasSize(0);
            })
          )
        );
    }

    @Test
    void canNarrowScopeOfImplicitTransitiveDependency() {
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
                      <groupId>org.apache.logging.log4j</groupId>
                      <artifactId>log4j-api</artifactId>
                      <version>2.17.2</version>
                      <scope>test</scope>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>org.apache.logging.log4j</groupId>
                    <artifactId>log4j-to-slf4j</artifactId>
                    <version>2.17.2</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            spec -> spec.afterRecipe(pomXml -> {
                MavenResolutionResult result = pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                List<ResolvedDependency> foundDependencies = result.findDependencies("org.apache.logging.log4j", "log4j-api", null);
                assertThat(foundDependencies).hasSize(0);
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2049")
    @Test
    void ciFriendlyVersionWithoutExplicitProperty() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>net.sample</groupId>
                <artifactId>sample</artifactId>
                <version>${revision}</version>
                <packaging>pom</packaging>
              </project>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2049")
    @Test
    void ciFriendlyVersionWithParent() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>net.sample</groupId>
                <artifactId>sample</artifactId>
                <version>${revision}</version>
                <packaging>pom</packaging>

                <modules>
                  <module>sample-rest</module>
                </modules>

              </project>
              """,
            spec -> spec.path("pom.xml")),
          pomXml(
            """
              <project>

                <modelVersion>4.0.0</modelVersion>
                <artifactId>sample-rest</artifactId>
                <packaging>jar</packaging>

                <parent>
                  <groupId>net.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>${revision}</version>
                  <relativePath>../pom.xml</relativePath>
                </parent>
              </project>
              """,
            spec -> spec.path("rest/pom.xml"))
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2049")
    @Test
    void canConnectProjectPomsWhenUsingCiFriendlyVersions() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>net.sample</groupId>
                <artifactId>sample</artifactId>
                <version>${revision}</version>
                <packaging>pom</packaging>

                <modules>
                  <module>sample-parent</module>
                  <module>sample-app</module>
                  <module>sample-rest</module>
                  <module>sample-web</module>
                </modules>

                <properties>
                  <revision>0.0.0-SNAPSHOT</revision>
                </properties>
              </project>
              """,
            spec -> spec.path("pom.xml")),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <artifactId>sample-parent</artifactId>
                <groupId>net.sample</groupId>
                <version>${revision}</version>
                <packaging>pom</packaging>

                <properties>
                  <revision>0.0.0-SNAPSHOT</revision>
                </properties>

                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>net.sample</groupId>
                      <artifactId>sample-web</artifactId>
                      <version>${project.version}</version>
                    </dependency>
                    <dependency>
                      <groupId>net.sample</groupId>
                      <artifactId>sample-rest</artifactId>
                      <version>${project.version}</version>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """,
            spec -> spec.path("parent/pom.xml")),
          pomXml(
            """
              <project>

                <modelVersion>4.0.0</modelVersion>
                <artifactId>sample-app</artifactId>
                <packaging>jar</packaging>

                <parent>
                  <groupId>net.sample</groupId>
                  <artifactId>sample-parent</artifactId>
                  <version>${revision}</version>
                  <relativePath>../parent/pom.xml</relativePath>
                </parent>

                <dependencies>
                  <dependency>
                    <groupId>net.sample</groupId>
                    <artifactId>sample-rest</artifactId>
                  </dependency>

                  <dependency>
                    <groupId>net.sample</groupId>
                    <artifactId>sample-web</artifactId>
                  </dependency>
                </dependencies>
              </project>
              """,
            spec -> spec.path("app/pom.xml")),
          pomXml(
            """
              <project>

                <modelVersion>4.0.0</modelVersion>
                <artifactId>sample-rest</artifactId>
                <packaging>jar</packaging>

                <parent>
                  <groupId>net.sample</groupId>
                  <artifactId>sample-parent</artifactId>
                  <version>${revision}</version>
                  <relativePath>../parent/pom.xml</relativePath>
                </parent>
              </project>
              """,
            spec -> spec.path("rest/pom.xml")),
          pomXml(
            """
              <project>

                <modelVersion>4.0.0</modelVersion>
                <artifactId>sample-web</artifactId>
                <packaging>jar</packaging>

                <parent>
                  <groupId>net.sample</groupId>
                  <artifactId>sample-parent</artifactId>
                  <version>${revision}</version>
                  <relativePath>../parent/pom.xml</relativePath>
                </parent>
              </project>
              """,
            spec -> spec.path("web/pom.xml"))
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2049")
    @Test
    void ciFriendlyVersionsStillWorkAfterUpdateMavenModel() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("junit", "junit", "4.1", null, null, null)),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>net.sample</groupId>
                <artifactId>sample</artifactId>
                <version>${revision}</version>
                <packaging>pom</packaging>

                <modules>
                  <module>sample-parent</module>
                  <module>sample-app</module>
                  <module>sample-rest</module>
                  <module>sample-web</module>
                </modules>

                <properties>
                  <revision>0.0.0-SNAPSHOT</revision>
                </properties>
              </project>
              """,
            spec -> spec.path("pom.xml")),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <artifactId>sample-parent</artifactId>
                <groupId>net.sample</groupId>
                <version>${revision}</version>
                <packaging>pom</packaging>

                <properties>
                  <revision>0.0.0-SNAPSHOT</revision>
                </properties>

                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>net.sample</groupId>
                      <artifactId>sample-web</artifactId>
                      <version>${project.version}</version>
                    </dependency>
                    <dependency>
                      <groupId>net.sample</groupId>
                      <artifactId>sample-rest</artifactId>
                      <version>${project.version}</version>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """,
            spec -> spec.path("parent/pom.xml")),
          pomXml(
            """
              <project>

                <modelVersion>4.0.0</modelVersion>
                <artifactId>sample-app</artifactId>
                <packaging>jar</packaging>

                <parent>
                  <groupId>net.sample</groupId>
                  <artifactId>sample-parent</artifactId>
                  <version>${revision}</version>
                  <relativePath>../parent/pom.xml</relativePath>
                </parent>

                <dependencies>
                  <dependency>
                    <groupId>net.sample</groupId>
                    <artifactId>sample-rest</artifactId>
                  </dependency>

                  <dependency>
                    <groupId>net.sample</groupId>
                    <artifactId>sample-web</artifactId>
                  </dependency>

                  <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.0</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>

                <modelVersion>4.0.0</modelVersion>
                <artifactId>sample-app</artifactId>
                <packaging>jar</packaging>

                <parent>
                  <groupId>net.sample</groupId>
                  <artifactId>sample-parent</artifactId>
                  <version>${revision}</version>
                  <relativePath>../parent/pom.xml</relativePath>
                </parent>

                <dependencies>
                  <dependency>
                    <groupId>net.sample</groupId>
                    <artifactId>sample-rest</artifactId>
                  </dependency>

                  <dependency>
                    <groupId>net.sample</groupId>
                    <artifactId>sample-web</artifactId>
                  </dependency>

                  <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.1</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            spec -> spec.path("app/pom.xml")),
          pomXml(
            """
              <project>

                <modelVersion>4.0.0</modelVersion>
                <artifactId>sample-rest</artifactId>
                <packaging>jar</packaging>

                <parent>
                  <groupId>net.sample</groupId>
                  <artifactId>sample-parent</artifactId>
                  <version>${revision}</version>
                  <relativePath>../parent/pom.xml</relativePath>
                </parent>
              </project>
              """,
            spec -> spec.path("rest/pom.xml")),
          pomXml(
            """
              <project>

                <modelVersion>4.0.0</modelVersion>
                <artifactId>sample-web</artifactId>
                <packaging>jar</packaging>

                <parent>
                  <groupId>net.sample</groupId>
                  <artifactId>sample-parent</artifactId>
                  <version>${revision}</version>
                  <relativePath>../parent/pom.xml</relativePath>
                </parent>
              </project>
              """,
            spec -> spec.path("web/pom.xml"))
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2373")
    @Test
    void multipleCiFriendlyVersionPlaceholders() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>bogus.example</groupId>
                <artifactId>parent</artifactId>
                <version>${revision}${changelist}</version>
                <packaging>pom</packaging>

                <modules>
                  <module>sub</module>
                </modules>

                <properties>
                  <revision>99999.0</revision>
                  <changelist>-SNAPSHOT</changelist>
                </properties>
              </project>
              """
          ),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <parent>
                  <groupId>bogus.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>${revision}${changelist}</version>
                </parent>

                <artifactId>sub</artifactId>
              </project>
              """,
            spec -> spec.path("sub/pom.xml"))
        );
    }

    @Test
    void optionalDependencies() {
        rewriteRun(
          mavenProject("a",
            pomXml("""
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample.optional</groupId>
                <artifactId>a</artifactId>
                <version>1.0.0</version>
              </project>
              """
            )),
          mavenProject("b",
            pomXml("""
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample.optional</groupId>
                <artifactId>b</artifactId>
                <version>1.0.0</version>
                <dependencies>
                  <dependency>
                    <groupId>org.sample.optional</groupId>
                    <artifactId>a</artifactId>
                    <version>1.0.0</version>
                    <optional>true</optional>
                  </dependency>
                  <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.12</version>
                  </dependency>
                </dependencies>
              </project>
              """
            )
          ),
          mavenProject("c",
            pomXml("""
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample.optional</groupId>
                  <artifactId>c</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                    <dependency>
                      <groupId>org.sample.optional</groupId>
                      <artifactId>b</artifactId>
                      <version>1.0.0</version>
                    </dependency>
                  </dependencies>
                </project>
                """,
              spec -> spec.afterRecipe(afterDoc -> assertThat(afterDoc.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow()
                .getDependencies().get(Scope.Compile))
                .noneMatch(dep -> "a".equals(dep.getGav().getArtifactId()))
                .anyMatch(dep -> "b".equals(dep.getGav().getArtifactId()))
                .anyMatch(dep -> "junit".equals(dep.getGav().getArtifactId())))
            ))
        );
    }

    @Test
    void malformedPom() {
        //language=xml
        String validXml = """
          <project>
            <groupId>com.mycompany.app</groupId>
            <artifactId>my-app</artifactId>
            <version>1</version>

            <dependencies>
              <dependency>
                <groupId>junit</groupId>
                <artifactId>junit</artifactId>
                <version>4.11</version>
              </dependency>
            </dependencies>
          </project>
          """;
        // I'm tired of the IDE warning me about the XML being malformed, so break it programmatically
        String malformedXml = validXml.replace("  </dependencies>", "");
        //noinspection LanguageMismatch
        assertThat(MavenParser.builder().build().parse(malformedXml))
          .singleElement()
          .isInstanceOf(ParseError.class);
    }

    @Test
    void plugins() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>org.openrewrite.maven</groupId>
                  <artifactId>a</artifactId>
                  <version>0.1.0-SNAPSHOT</version>

                  <build>
                      <plugins>
                          <plugin>
                              <artifactId>maven-compiler-plugin</artifactId>
                              <version>3.11.0</version>
                              <configuration>
                                  <release>11</release>
                              </configuration>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            spec -> spec.afterRecipe(pomXml -> {
                  Plugin plugin = pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom().getPlugins().getFirst();
                  assertThat(plugin.getArtifactId()).isEqualTo("maven-compiler-plugin");
                  assertThat(plugin.getConfiguration()).isNotNull();
              }
            )
          )
        );
    }

    @Test
    void pluginWithoutConfig() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>org.openrewrite.maven</groupId>
                  <artifactId>a</artifactId>
                  <version>0.1.0-SNAPSHOT</version>

                  <build>
                      <plugins>
                          <plugin>
                              <artifactId>maven-compiler-plugin</artifactId>
                              <version>3.11.0</version>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            spec -> spec.afterRecipe(pomXml -> {
                  Plugin plugin = pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom().getPlugins().getFirst();
                  assertThat(plugin.getArtifactId()).isEqualTo("maven-compiler-plugin");
                  assertThat(plugin.getConfiguration()).isNull();
              }
            )
          )
        );
    }

    @Test
    void pluginsFromParent() {
        rewriteRun(
          mavenProject("a",
            pomXml(
              """
                  <project>
                      <parent>
                          <groupId>org.openrewrite.maven</groupId>
                          <artifactId>b</artifactId>
                          <version>0.1.0-SNAPSHOT</version>
                          <relativePath />
                      </parent>
                      <artifactId>a</artifactId>
                  </project>
                """,
              spec -> spec.afterRecipe(pomXml ->
                assertThat(pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom().getPlugins()
                  .getFirst().getArtifactId())
                  .isEqualTo("maven-compiler-plugin")
              )
            )
          ),
          mavenProject("b",
            pomXml(
              """
                    <project>
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>b</artifactId>
                        <version>0.1.0-SNAPSHOT</version>

                        <packaging>pom</packaging>

                        <build>
                          <plugins>
                              <plugin>
                                  <artifactId>maven-compiler-plugin</artifactId>
                                  <version>3.11.0</version>
                                  <configuration>
                                      <release>11</release>
                                  </configuration>
                              </plugin>
                          </plugins>
                      </build>
                    </project>
                """
            )
          )
        );
    }

    @Test
    void pluginManagement() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>org.openrewrite.maven</groupId>
                  <artifactId>a</artifactId>
                  <version>0.1.0-SNAPSHOT</version>

                  <build>
                    <pluginManagement>
                      <plugins>
                          <plugin>
                              <artifactId>maven-compiler-plugin</artifactId>
                              <version>3.11.0</version>
                              <configuration>
                                  <release>11</release>
                              </configuration>
                          </plugin>
                      </plugins>
                     </pluginManagement>
                  </build>
              </project>
              """,
            spec -> spec.afterRecipe(pomXml ->
              assertThat(pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom().getPluginManagement()
                .getFirst().getArtifactId())
                .isEqualTo("maven-compiler-plugin")
            )
          )
        );
    }

    @Test
    void pluginManagementFromParent() {
        rewriteRun(
          mavenProject("a",
            pomXml(
              """
                  <project>
                      <parent>
                          <groupId>org.openrewrite.maven</groupId>
                          <artifactId>b</artifactId>
                          <version>0.1.0-SNAPSHOT</version>
                          <relativePath />
                      </parent>
                      <artifactId>a</artifactId>
                  </project>
                """,
              spec -> spec.afterRecipe(pomXml ->
                assertThat(pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom().getPluginManagement()
                  .getFirst().getArtifactId())
                  .isEqualTo("maven-compiler-plugin")
              )
            )
          ),
          mavenProject("b",
            pomXml(
              """
                    <project>
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>b</artifactId>
                        <version>0.1.0-SNAPSHOT</version>

                        <packaging>pom</packaging>

                        <build>
                        <pluginManagement>
                            <plugins>
                                <plugin>
                                    <artifactId>maven-compiler-plugin</artifactId>
                                    <version>3.11.0</version>
                                    <configuration>
                                        <release>11</release>
                                    </configuration>
                                </plugin>
                            </plugins>
                        </pluginManagement>
                      </build>
                    </project>
                """
            )
          )
        );
    }

    @Test
    void notInheritedPluginFromParent() {
        rewriteRun(
          mavenProject("a",
            pomXml(
              """
                  <project>
                      <parent>
                          <groupId>org.openrewrite.maven</groupId>
                          <artifactId>b</artifactId>
                          <version>0.1.0-SNAPSHOT</version>
                          <relativePath />
                      </parent>
                      <artifactId>a</artifactId>
                  </project>
                """,
              spec -> spec.afterRecipe(pomXml ->
                assertThat(pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom().getPlugins())
                  .isEmpty()
              )
            )
          ),
          mavenProject("b",
            pomXml(
              """
                    <project>
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>b</artifactId>
                        <version>0.1.0-SNAPSHOT</version>

                        <packaging>pom</packaging>

                        <build>
                          <plugins>
                              <plugin>
                                  <inherited>false</inherited>
                                  <artifactId>maven-compiler-plugin</artifactId>
                                  <version>3.11.0</version>
                                  <configuration>
                                      <release>11</release>
                                  </configuration>
                              </plugin>
                          </plugins>
                        </build>
                    </project>
                """
            )
          )
        );
    }

    @Test
    void mergePluginConfig() {
        rewriteRun(
          mavenProject("b",
            pomXml(
              """
                    <project>
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>b</artifactId>
                        <version>0.1.0-SNAPSHOT</version>

                        <packaging>pom</packaging>

                        <build>
                          <plugins>
                              <plugin>
                                  <artifactId>maven-compiler-plugin</artifactId>
                                  <version>3.11.0</version>
                                  <configuration>
                                      <source>11</source>
                                      <target>11</target>
                                  </configuration>
                              </plugin>
                          </plugins>
                        </build>
                    </project>
                """
            )
          ),
          mavenProject("a",
            pomXml(
              """
                  <project>
                      <parent>
                          <groupId>org.openrewrite.maven</groupId>
                          <artifactId>b</artifactId>
                          <version>0.1.0-SNAPSHOT</version>
                          <relativePath />
                      </parent>
                      <artifactId>a</artifactId>

                      <build>
                          <plugins>
                              <plugin>
                                  <artifactId>maven-compiler-plugin</artifactId>
                                  <version>3.11.0</version>
                                  <configuration>
                                      <target>17</target>
                                  </configuration>
                              </plugin>
                          </plugins>
                        </build>
                  </project>
                """,
              spec -> spec.afterRecipe(pomXml ->
                assertThat(pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom().getPlugins().getFirst().getConfiguration())
                  .hasSize(2)
                  .anyMatch(elem -> "11".equals(elem.asText()))
                  .anyMatch(elem -> "17".equals(elem.asText()))
              )
            )
          )
        );
    }

    @Test
    void mergePluginConfigListOverride() {
        rewriteRun(
          mavenProject("b",
            pomXml(
              """
                    <project>
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>b</artifactId>
                        <version>0.1.0-SNAPSHOT</version>

                        <packaging>pom</packaging>

                        <build>
                          <plugins>
                            <plugin>
                                <artifactId>maven-resources-plugin</artifactId>
                                <configuration>
                                    <resources>
                                        <resource>
                                            <directory>parent-resources</directory>
                                        </resource>
                                    </resources>
                                </configuration>
                            </plugin>
                          </plugins>
                        </build>
                    </project>
                """
            )
          ),
          mavenProject("a",
            pomXml(
              """
                  <project>
                      <parent>
                          <groupId>org.openrewrite.maven</groupId>
                          <artifactId>b</artifactId>
                          <version>0.1.0-SNAPSHOT</version>
                          <relativePath />
                      </parent>
                      <artifactId>a</artifactId>

                      <build>
                          <plugins>
                            <plugin>
                                <artifactId>maven-resources-plugin</artifactId>
                                <configuration>
                                    <resources>
                                        <resource>
                                            <directory>child-a-resources</directory>
                                        </resource>
                                    </resources>
                                </configuration>
                            </plugin>
                          </plugins>
                        </build>
                  </project>
                """,
              spec -> spec.afterRecipe(pomXml ->
                assertThat(pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom().getPlugins().getFirst().getConfiguration())
                  .hasSize(1)
                  .isEqualTo(JsonNodeFactory.instance.objectNode()
                    .set("resources", JsonNodeFactory.instance.objectNode()
                      .set("resource", JsonNodeFactory.instance.objectNode()
                        .set("directory", JsonNodeFactory.instance.textNode("child-a-resources")))))
              )
            )
          )
        );
    }

    @Test
    void mergePluginConfigListAppend() {
        rewriteRun(
          mavenProject("b",
            pomXml(
              """
                    <project>
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>b</artifactId>
                        <version>0.1.0-SNAPSHOT</version>
                        <packaging>pom</packaging>
                        <build>
                          <plugins>
                            <plugin>
                                <artifactId>maven-resources-plugin</artifactId>
                                <configuration>
                                    <resources combine.children="append">
                                        <resource>
                                            <directory>parent-resources</directory>
                                        </resource>
                                    </resources>
                                </configuration>
                            </plugin>
                          </plugins>
                        </build>
                    </project>
                """
            )
          ),
          mavenProject("a",
            pomXml(
              """
                  <project>
                      <parent>
                          <groupId>org.openrewrite.maven</groupId>
                          <artifactId>b</artifactId>
                          <version>0.1.0-SNAPSHOT</version>
                          <relativePath />
                      </parent>
                      <artifactId>a</artifactId>
                      <build>
                          <plugins>
                            <plugin>
                                <artifactId>maven-resources-plugin</artifactId>
                                <configuration>
                                    <resources>
                                        <resource>
                                            <directory>child-a-resources</directory>
                                        </resource>
                                    </resources>
                                </configuration>
                            </plugin>
                          </plugins>
                        </build>
                  </project>
                """,
              spec -> spec.afterRecipe(pomXml ->
                assertThat(pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom().getPlugins().getFirst().getConfiguration())
                  .hasSize(1)
                  .isEqualTo(JsonNodeFactory.instance.objectNode()
                    .set("resources", JsonNodeFactory.instance.objectNode()
                      .<ObjectNode>set("combine.children", JsonNodeFactory.instance.textNode("append"))
                      .set("resource", JsonNodeFactory.instance.arrayNode()
                        .add(JsonNodeFactory.instance.objectNode().set("directory", JsonNodeFactory.instance.textNode("parent-resources")))
                        .add(JsonNodeFactory.instance.objectNode().set("directory", JsonNodeFactory.instance.textNode("child-a-resources")))
                      )))
              )
            )
          )
        );
    }

    @Test
    void mergePluginConfigListAppendOverride() {
        rewriteRun(
          mavenProject("b",
            pomXml(
              """
                    <project>
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>b</artifactId>
                        <version>0.1.0-SNAPSHOT</version>
                        <packaging>pom</packaging>
                        <build>
                          <plugins>
                            <plugin>
                                <artifactId>maven-resources-plugin</artifactId>
                                <configuration>
                                    <resources combine.children="append">
                                        <resource>
                                            <directory>parent-resources</directory>
                                        </resource>
                                    </resources>
                                </configuration>
                            </plugin>
                          </plugins>
                        </build>
                    </project>
                """
            )
          ),
          mavenProject("a",
            pomXml(
              """
                  <project>
                      <parent>
                          <groupId>org.openrewrite.maven</groupId>
                          <artifactId>b</artifactId>
                          <version>0.1.0-SNAPSHOT</version>
                          <relativePath />
                      </parent>
                      <artifactId>a</artifactId>
                      <build>
                          <plugins>
                            <plugin>
                                <artifactId>maven-resources-plugin</artifactId>
                                <configuration>
                                    <resources combine.self="override">
                                        <resource>
                                            <directory>child-a-resources</directory>
                                        </resource>
                                    </resources>
                                </configuration>
                            </plugin>
                          </plugins>
                        </build>
                  </project>
                """,
              spec -> spec.afterRecipe(pomXml ->
                assertThat(pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom().getPlugins().getFirst().getConfiguration())
                  .hasSize(1)
                  .isEqualTo(JsonNodeFactory.instance.objectNode()
                    .set("resources", JsonNodeFactory.instance.objectNode()
                      .<ObjectNode>set("combine.self", JsonNodeFactory.instance.textNode("override"))
                      .set("resource", JsonNodeFactory.instance.objectNode()
                        .set("directory", JsonNodeFactory.instance.textNode("child-a-resources")))))
              )
            )
          )
        );
    }


    @Issue("https://github.com/openrewrite/rewrite/issues/3811")
    @Test
    void escapedA() {
        rewriteRun(
          spec -> spec.recipe(new AddManagedDependency("ch.qos.logback", "logback-classic", "1.4.14", null, null, null, null, null, null, null)),
          //language=xml
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                    <groupId>org.slf4j</groupId>
                    <artifactId>slf4j-&#0097;pi</artifactId>
                    <version>2.0.9</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>ch.qos.logback</groupId>
                      <artifactId>logback-classic</artifactId>
                      <version>1.4.14</version>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>org.slf4j</groupId>
                    <artifactId>slf4j-&#0097;pi</artifactId>
                    <version>2.0.9</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void transitiveDependencyManagement() {
        rewriteRun(
          mavenProject("depends-on-guava",
            pomXml("""
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.example</groupId>
                    <artifactId>depends-on-guava</artifactId>
                    <version>0.0.1</version>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>29.0-jre</version>
                        </dependency>
                    </dependencies>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>com.google.guava</groupId>
                                <artifactId>guava</artifactId>
                                <version>30.0-jre</version>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
                """,
              spec -> spec.afterRecipe(pom -> {
                  //noinspection OptionalGetWithoutIsPresent
                  List<ResolvedDependency> guava = pom.getMarkers().findFirst(MavenResolutionResult.class)
                    .map(mrr -> mrr.findDependencies("com.google.guava", "guava", Scope.Compile))
                    .get();

                  assertThat(guava)
                    .singleElement()
                    .as("Dependency management cannot override the version of a direct dependency")
                    .matches(it -> "29.0-jre".equals(it.getVersion()));
              })
            )),
          mavenProject("transitively-depends-on-guava",
            pomXml("""
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.example</groupId>
                    <artifactId>transitively-depends-on-guava</artifactId>
                    <version>0.0.1</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.example</groupId>
                            <artifactId>depends-on-guava</artifactId>
                            <version>0.0.1</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.afterRecipe(pom -> {
                  //noinspection OptionalGetWithoutIsPresent
                  List<ResolvedDependency> guava = pom.getMarkers().findFirst(MavenResolutionResult.class)
                    .map(mrr -> mrr.findDependencies("com.google.guava", "guava", Scope.Compile))
                    .get();

                  assertThat(guava)
                    .singleElement()
                    .as("The dependency management of dependency does not override the versions of its own direct dependencies")
                    .matches(it -> "29.0-jre".equals(it.getVersion()));
              })
            )
          )
        );
    }

    @Test
    void runtimeClasspathOnly() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>cache-2</artifactId>
                <version>0.0.1-SNAPSHOT</version>
                <dependencies>
                  <dependency>
                    <groupId>org.glassfish.jaxb</groupId>
                    <artifactId>jaxb-runtime</artifactId>
                    <version>4.0.5</version>
                    <scope>runtime</scope>
                  </dependency>
                </dependencies>
              </project>
              """,
            spec -> spec.afterRecipe(pomXml -> {
                MavenResolutionResult resolution = pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                assertThat(resolution.findDependencies("org.glassfish.jaxb", "jaxb-runtime", Scope.Runtime)).isNotEmpty();
                assertThat(resolution.findDependencies("org.glassfish.jaxb", "jaxb-runtime", Scope.Compile)).isEmpty();
                assertThat(resolution.findDependencies("jakarta.xml.bind", "jakarta.xml.bind-api", Scope.Compile)).isEmpty();
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4093")
    @Test
    void circularImportDependency() {
        rewriteRun(
          mavenProject("root",
            pomXml(
              """
                <project>
                  <groupId>com.example</groupId>
                  <artifactId>circular-example-parent</artifactId>
                  <version>0.0.1-SNAPSHOT</version>
                  <modules>
                    <module>circular-example-child</module>
                  </modules>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>circular-example-child</artifactId>
                        <version>0.0.1-SNAPSHOT</version>
                        <scope>import</scope>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                </project>
                """,
              spec -> spec.afterRecipe(pomXml -> {
                  MavenResolutionResult resolution = pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                  GroupArtifactVersion gav = resolution.getPom().getDependencyManagement().getFirst().getGav();
                  assertThat(gav.getGroupId()).isEqualTo("junit");
                  assertThat(gav.getArtifactId()).isEqualTo("junit");
                  assertThat(gav.getVersion()).isEqualTo("4.13.2");
              })
            ),
            mavenProject("circular-example-child",
              pomXml(
                """
                  <project>
                    <parent>
                      <groupId>com.example</groupId>
                      <artifactId>circular-example-parent</artifactId>
                      <version>0.0.1-SNAPSHOT</version>
                    </parent>
                    <artifactId>circular-example-child</artifactId>
                    <version>0.0.1-SNAPSHOT</version>
                    <dependencyManagement>
                      <dependencies>
                        <dependency>
                          <groupId>junit</groupId>
                          <artifactId>junit</artifactId>
                          <version>4.13.2</version>
                        </dependency>
                      </dependencies>
                    </dependencyManagement>
                  </project>
                  """,
                spec -> spec.afterRecipe(pomXml -> {
                    MavenResolutionResult resolution = pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                    GroupArtifactVersion gav = resolution.getPom().getDependencyManagement().getFirst().getGav();
                    assertThat(gav.getGroupId()).isEqualTo("junit");
                    assertThat(gav.getArtifactId()).isEqualTo("junit");
                    assertThat(gav.getVersion()).isEqualTo("4.13.2");
                })
              )
            )
          )
        );
    }

    @Test
    void circularMavenProperty() {
        rewriteRun(
          mavenProject("root",
            pomXml(
              """
                <project>
                    <groupId>example</groupId>
                    <artifactId>example-root</artifactId>
                    <packaging>pom</packaging>
                    <version>1.0.0</version>

                    <properties>
                        <project.version>1.0.1</project.version>
                    </properties>

                    <modules>
                        <module>example-child</module>
                    </modules>
                </project>
                """,
              spec -> spec.afterRecipe(pomXml -> assertThat(
                pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow()
                  .getPom().getProperties().get("project.version"))
                .isEqualTo("1.0.1"))
            ),
            mavenProject("circular-example-child",
              pomXml(
                """
                  <project>
                      <parent>
                          <groupId>example</groupId>
                          <artifactId>example-root</artifactId>
                          <version>1.0.0</version>
                      </parent>
                      <artifactId>example-child</artifactId>
                      <version>${project.version}</version>
                  </project>
                  """,
                spec -> spec.afterRecipe(pomXml -> assertThat(
                  pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow()
                    .getPom().getVersion())
                  .isEqualTo("1.0.1"))
              )
            )
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4319")
    @Test
    void multiModulePropertyVersionShouldAddModules() {
        rewriteRun(
          mavenProject("root",
            pomXml(
              """
                <project>
                    <groupId>example</groupId>
                    <artifactId>example-root</artifactId>
                    <packaging>pom</packaging>
                    <version>${revision}</version>
                    <properties>
                        <revision>1.0.1</revision>
                    </properties>
                    <modules>
                        <module>example-child</module>
                    </modules>
                </project>
                """,
              spec -> spec.afterRecipe(pomXml -> assertThat(
                pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow()
                  .getModules()).isNotEmpty())
            ),
            mavenProject("example-child",
              pomXml(
                """
                  <project>
                      <parent>
                          <groupId>example</groupId>
                          <artifactId>example-root</artifactId>
                          <version>${revision}</version>
                      </parent>
                      <artifactId>example-child</artifactId>
                  </project>
                  """
              )
            )
          )
        );
    }

    @Test
    void childDependencyDefinitionShouldTakePrecedence() {
        rewriteRun(
          mavenProject("parent",
            pomXml(
              """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>parent</artifactId>
                    <version>1</version>
                    <modules>
                        <module>child</module>
                    </modules>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>31.1-jre</version>
                        </dependency>
                    </dependencies>
                </project>
                """
            ),
            mavenProject("child",
              pomXml(
                """
                  <project>
                      <parent>
                          <groupId>com.mycompany.app</groupId>
                          <artifactId>parent</artifactId>
                          <version>1</version>
                          <relativePath>../pom.xml</relativePath>
                      </parent>
                      <artifactId>child</artifactId>
                      <dependencies>
                          <dependency>
                              <groupId>com.google.guava</groupId>
                              <artifactId>guava</artifactId>
                              <version>31.1-jre</version>
                              <scope>test</scope>
                          </dependency>
                      </dependencies>
                  </project>
                  """,
                spec -> spec.afterRecipe(pomXml -> {
                      MavenResolutionResult res = pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                      assertThat(res.getDependencies().get(Scope.Compile)).isEmpty();
                      assertThat(res.getDependencies().get(Scope.Runtime)).isEmpty();
                      assertThat(res.getDependencies().get(Scope.Provided)).isEmpty();
                      assertThat(res.getDependencies().get(Scope.Test)).isNotEmpty().anyMatch(dep -> "com.google.guava".equals(dep.getGroupId()) && "guava".equals(dep.getArtifactId()));
                  }
                )
              )
            )
          )
        );
    }

    @Test
    void missingDependencyIsExcluded() {
        rewriteRun(
          // Suppress test framework's usual error handler because we expect an error resolving coherence
          spec -> spec.executionContext(new InMemoryExecutionContext(throwable -> {
            }))
            .typeValidationOptions(TypeValidation.none()),
          mavenProject("a",
            pomXml("""
                <project>
                    <groupId>com.test</groupId>
                    <artifactId>a</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>com.test</groupId>
                            <artifactId>b</artifactId>
                            <version>1.0.0</version>
                            <exclusions>
                                  <exclusion>
                                      <groupId>com.oracle.coherence</groupId>
                                      <artifactId>coherence</artifactId>
                                  </exclusion>
                            </exclusions>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.afterRecipe(pom -> {
                  assertThat(pom.getMarkers().findFirst(ParseExceptionResult.class)).isEmpty();
                  assertThat(pom.getMarkers().findFirst(MavenResolutionResult.class))
                    .isNotEmpty()
                    .get()
                    .as("A's dependency on B should be represented within the maven resolution result")
                    .matches(mrr -> !mrr.findDependencies("com.test", "b", Scope.Compile).isEmpty())
                    .as("The excluded dependency should not appear within the maven resolution result")
                    .matches(mrr -> mrr.findDependencies("com.oracle.coherence", "coherence", Scope.Compile).isEmpty());
              })
            )),
          mavenProject("b",
            pomXml("""
              <project>
                  <groupId>com.test</groupId>
                  <artifactId>b</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                      <dependency>
                          <!-- this is not actually available in maven central -->
                          <groupId>com.oracle.coherence</groupId>
                          <artifactId>coherence</artifactId>
                          <version>12.1.3.0.0</version>
                      </dependency>
                  </dependencies>
              </project>
              """
            )
          )
        );
    }

    @Test
    void exclusionsAffectTransitiveDependencies() {
        rewriteRun(
          // Suppress test framework's usual error handler because we expect an error resolving coherence
          spec -> spec.executionContext(new InMemoryExecutionContext(throwable -> {
            }))
            .typeValidationOptions(TypeValidation.none()),
          mavenProject("x",
            pomXml("""
                <project>
                    <groupId>com.test</groupId>
                    <artifactId>x</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>com.test</groupId>
                            <artifactId>y</artifactId>
                            <version>1.0.0</version>
                            <exclusions>
                                  <exclusion>
                                      <groupId>com.oracle.coherence</groupId>
                                      <artifactId>coherence</artifactId>
                                  </exclusion>
                            </exclusions>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.afterRecipe(pom -> {
                  assertThat(pom.getMarkers().findFirst(ParseExceptionResult.class)).isEmpty();
                  assertThat(pom.getMarkers().findFirst(MavenResolutionResult.class))
                    .isNotEmpty()
                    .get()
                    .as("X's dependency on Y should be represented within the maven resolution result")
                    .matches(mrr -> !mrr.findDependencies("com.test", "y", Scope.Compile).isEmpty())
                    .as("The excluded dependency should not appear within the maven resolution result")
                    .matches(mrr -> mrr.findDependencies("com.oracle.coherence", "coherence", Scope.Compile).isEmpty());
              })
            )),
          mavenProject("y",
            pomXml("""
              <project>
                  <groupId>com.test</groupId>
                  <artifactId>y</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                  <dependency>
                      <groupId>com.test</groupId>
                      <artifactId>z</artifactId>
                      <version>1.0.0</version>
                  </dependency>
                  </dependencies>
              </project>
              """
            )
          ),
          mavenProject("z",
            pomXml("""
              <project>
                  <groupId>com.test</groupId>
                  <artifactId>z</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                  <dependency>
                      <!-- this is not actually available in maven central -->
                      <groupId>com.oracle.coherence</groupId>
                      <artifactId>coherence</artifactId>
                      <version>12.1.3.0.0</version>
                  </dependency>
                  </dependencies>
              </project>
              """
            )
          )
        );
    }

    @Test
    void systemPropertyTakesPrecedence() {
        System.setProperty("hatversion", "2.3.0");
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>parent</artifactId>
                <version>1.0-SNAPSHOT</version>
                <packaging>pom</packaging>
                <name>parent</name>
                <url>http://www.example.com</url>
                <properties>
                  <hatversion>SYSTEM_PROPERTY_SHOULD_OVERRIDE_THIS</hatversion>
                </properties>
                <dependencies>
                    <dependency>
                        <groupId>org.springframework.hateoas</groupId>
                        <artifactId>spring-hateoas</artifactId>
                        <version>${hatversion}</version>
                    </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void propertyFromMavenConfig() {
        rewriteRun(
          spec -> spec.parser(MavenParser.builder().property("revision", "1.0.0")),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>parent</artifactId>
                <version>${revision}</version>
              </project>
              """,
            spec -> spec.afterRecipe(p -> {
                  var results = p.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                  assertThat(results.getPom().getVersion()).isEqualTo("${revision}");
                  assertThat(results.getPom().getProperties().get("revision")).isEqualTo("1.0.0");
              }
            )
          )
        );
    }

    @Test
    void propertyFromMavenConfigFromParentPomCanBeUsedInChild() {
        rewriteRun(
          spec -> spec.parser(MavenParser.builder().property("revision", "1.0.0")),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>parent</artifactId>
                <version>${revision}</version>
              </project>
              """
          ),
          mavenProject("child",
            pomXml(
              //language=xml
              """
                <project>
                  <parent>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>parent</artifactId>
                    <version>${revision}</version>
                  </parent>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>child</artifactId>
                  <version>${revision}</version>
                </project>
                """,
              spec -> spec.afterRecipe(p -> {
                  var results = p.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                  assertThat(results.getPom().getVersion()).isEqualTo("${revision}");
                  assertThat(results.getPom().getProperties().get("revision")).isEqualTo("1.0.0");
                  assert results.getParent() != null;
                  assertThat(results.getParent().getPom().getVersion()).isEqualTo("${revision}");
                  assertThat(results.getParent().getPom().getProperties().get("revision")).isEqualTo("1.0.0");
              })
            )
          )
        );
    }

    @Test
    void profilesFromMavenConfig() {
        rewriteRun(
          spec -> spec.parser(MavenParser.builder().activeProfiles("a", "b", "c")),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>parent</artifactId>
                <version>1.0.0</version>
              </project>
              """,
            spec -> spec.afterRecipe(p -> {
                  var results = p.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                  assertThat(results.getPom().getActiveProfiles()).contains("a", "b", "c");
              }
            )
          )
        );
    }

    /**
     * Maven successfully resolves this pom, but warns that transitive dependencies are invalid. e.g.:
     * [WARNING] The POM for com.sun.xml.stream.buffer:streambuffer:jar:0.6 is invalid, transitive dependencies (if any) will not be available, enable debug logging for more details
     * [WARNING] The POM for org.jvnet.staxex:stax-ex:jar:1.0 is invalid, transitive dependencies (if any) will not be available, enable debug logging for more details
     * [WARNING] The POM for com.sun.xsom:xsom:jar:20070323 is invalid, transitive dependencies (if any) will not be available, enable debug logging for more details
     * Looking inside streambuffer's pom it is missing version numbers for its "activation:activation" dependency.
     * Maven lists these as the dependencies of this pom and we should match its behavior:
     * org.jvnet.jax-ws-commons:jaxws-json:jar:1.1:compile
     * +- com.sun.xml.ws:jaxws-rt:jar:2.1.2-alpha-20070426:compile
     * |  +- javax.xml.ws:jaxws-api:jar:2.1:compile
     * |  +- com.sun.xml.messaging.saaj:saaj-impl:jar:1.3:compile
     * |  |  \- javax.xml.soap:saaj-api:jar:1.3:compile
     * |  +- com.sun.xml.stream:sjsxp:jar:1.0:compile
     * |  |  \- javax.xml.stream:stax-api:jar:1.0:compile
     * |  \- org.jvnet.staxex:stax-ex:jar:1.0:compile
     * +- com.sun.xml.bind:jaxb-impl:jar:2.1.3:compile
     * |  \- javax.xml.bind:jaxb-api:jar:2.1:compile
     * |     \- javax.activation:activation:jar:1.1:compile
     * +- com.sun.xml.stream.buffer:streambuffer:jar:0.6:compile
     * +- com.sun.xsom:xsom:jar:20070323:compile
     * +- org.codehaus.jettison:jettison:jar:1.0-beta-1:compile
     * |  +- junit:junit:jar:3.8.1:compile
     * |  \- stax:stax-api:jar:1.0.1:compile
     * +- velocity:velocity:jar:1.5:compile
     * |  +- commons-collections:commons-collections:jar:3.1:compile
     * |  +- commons-lang:commons-lang:jar:2.1:compile
     * |  \- oro:oro:jar:2.0.8:compile
     * \- com.sun.xml:relaxngDatatype:jar:1.0:compile
     */
    @Test
    void invalidTransitives() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>app</artifactId>
                <version>1.0.0</version>
                  <dependencies>
                    <dependency>
                      <groupId>org.jvnet.jax-ws-commons</groupId>
                      <artifactId>jaxws-json</artifactId>
                      <version>1.1</version>
                    </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void invalidDirect() {
        assertThatThrownBy(() -> rewriteRun(
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>app</artifactId>
                <version>1.0.0</version>
                  <dependencies>
                    <dependency>
                      <groupId>org.jvnet.jax-ws-commons</groupId>
                      <artifactId>jaxws-json</artifactId>
                    </dependency>
                  </dependencies>
              </project>
              """
          )
        )).isInstanceOf(AssertionError.class)
          .cause()
          .isInstanceOf(MavenDownloadingException.class);
    }

    @Test
    void wildcardExclusion() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>app</artifactId>
                <version>1.0.0</version>
                <dependencies>
                  <dependency>
                      <groupId>ch.qos.logback</groupId>
                      <artifactId>logback-classic</artifactId>
                      <version>1.3.11</version>
                      <exclusions>
                          <exclusion>
                              <groupId>*</groupId>
                              <artifactId>*</artifactId>
                          </exclusion>
                      </exclusions>
                  </dependency>
                </dependencies>
              </project>
              """,
            spec -> spec.afterRecipe(pom -> {
                MavenResolutionResult resolution = pom.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                assertThat(resolution.findDependencies("ch.qos.logback", "logback-core", Scope.Compile)).isEmpty();
            })
          ));
    }

    @Test
    void parentNearerThanBom() {
        rewriteRun(
          pomXml(
            //language=xml
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>org.openrewrite</groupId>
                <artifactId>sam-parent</artifactId>
                <version>1.0.0</version>
                <packaging>pom</packaging>

                <dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>org.openrewrite</groupId>
                          <artifactId>rewrite-core</artifactId>
                          <version>8.0.0</version>
                      </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """
          ),
          mavenProject("sam-bom",
            pomXml(
              //language=xml
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>

                  <groupId>org.openrewrite</groupId>
                  <artifactId>sam-bom</artifactId>
                  <version>1.0.0</version>
                  <packaging>pom</packaging>

                  <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>org.openrewrite</groupId>
                            <artifactId>rewrite-core</artifactId>
                            <version>7.0.0</version>
                        </dependency>
                    </dependencies>
                  </dependencyManagement>
                </project>
                """)),
          mavenProject("sam",
            pomXml(
              //language=xml
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>

                  <groupId>org.openrewrite</groupId>
                  <artifactId>sam</artifactId>
                  <version>1.0.0</version>

                  <parent>
                      <groupId>org.openrewrite</groupId>
                      <artifactId>sam-parent</artifactId>
                      <version>1.0.0</version>
                  </parent>

                  <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>org.openrewrite</groupId>
                            <artifactId>sam-bom</artifactId>
                            <version>1.0.0</version>
                            <type>pom</type>
                            <scope>import</scope>
                        </dependency>
                    </dependencies>
                  </dependencyManagement>

                  <dependencies>
                    <dependency>
                        <groupId>org.openrewrite</groupId>
                        <artifactId>rewrite-core</artifactId>
                    </dependency>
                  </dependencies>
                </project>
                """, spec -> spec.afterRecipe(pom -> {
                  MavenResolutionResult resolution = pom.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                  assertThat(resolution.findDependencies("org.openrewrite", "rewrite-core", Scope.Compile))
                    .singleElement()
                    .extracting(r -> r.getGav().getVersion())
                    .as("The parent says 8.0.0, the bom says 7.0.0, Maven says the parent is nearer.")
                    .isEqualTo("8.0.0");
              })))
        );
    }

    @Test
    void jaxbRuntime() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.mycompany</groupId>
                <artifactId>my-jaxb</artifactId>
                <version>1.0-SNAPSHOT</version>

                <dependencies>
                  <dependency>
                      <groupId>org.glassfish.jaxb</groupId>
                      <artifactId>jaxb-runtime</artifactId>
                      <version>2.3.9</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            spec -> spec.afterRecipe(pom -> {
                  MavenResolutionResult mrr = pom.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                  assertThat(mrr.getDependencies().get(Scope.Runtime))
                    .map(ResolvedDependency::getGav)
                    .map(ResolvedGroupArtifactVersion::asGroupArtifactVersion)
                    .as("At one point this test failed with no version number found for jakarta.xml.bind-api because ResolvedPom was not considering classifiers as significant for dependency management")
                    .containsExactlyInAnyOrder(
                      new GroupArtifactVersion("org.glassfish.jaxb", "jaxb-runtime", "2.3.9"),
                      new GroupArtifactVersion("jakarta.xml.bind", "jakarta.xml.bind-api", "2.3.3"),
                      new GroupArtifactVersion("org.glassfish.jaxb", "txw2", "2.3.9"),
                      new GroupArtifactVersion("com.sun.istack", "istack-commons-runtime", "3.0.12"),
                      new GroupArtifactVersion("com.sun.activation", "jakarta.activation", "1.2.2")
                    );
              }
            )
          )
        );
    }

    @CsvSource({"2.15.0,2.13.0", "2.13.0,2.15.0", "2.13.0,2.13.0"})
    @ParameterizedTest
    void lastListedDependencyIsUsed(String firstVersion, String secondVersion) {
        rewriteRun(
          pomXml(
            //language=xml
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>com.fasterxml.jackson.core</groupId>
                    <artifactId>jackson-core</artifactId>
                    <version>%s</version>
                  </dependency>
                  <dependency>
                    <groupId>com.fasterxml.jackson.core</groupId>
                    <artifactId>jackson-core</artifactId>
                    <version>%s</version>
                  </dependency>
                </dependencies>
              </project>
              """.formatted(firstVersion, secondVersion),
            spec -> spec.afterRecipe(pom -> {
                MavenResolutionResult resolution = pom.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                assertThat(resolution.findDependencies("com.fasterxml.jackson.core", "jackson-core", Scope.Compile))
                  .hasSize(1)
                  .extracting(ResolvedDependency::getGav)
                  .extracting(ResolvedGroupArtifactVersion::getVersion)
                  .containsExactly(secondVersion);
            })
          )
        );
    }

    @CsvSource({"2.15.0,runtime,2.13.0,test", "2.13.0,runtime,2.15.0,test", "2.13.0,runtime,2.13.0,test", "2.15.0,compile,2.13.0,test", "2.13.0,compile,2.15.0,test", "2.13.0,compile,2.13.0,test"})
    @ParameterizedTest
    void lastListedDependencyIsUsedForScope(String firstVersion, String firstScope, String secondVersion, String secondScope) {
        rewriteRun(
          pomXml(
            //language=xml
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>com.fasterxml.jackson.core</groupId>
                    <artifactId>jackson-core</artifactId>
                    <version>%s</version>
                    <scope>%s</scope>
                  </dependency>
                  <dependency>
                    <groupId>com.fasterxml.jackson.core</groupId>
                    <artifactId>jackson-core</artifactId>
                    <version>%s</version>
                    <scope>%s</scope>
                  </dependency>
                </dependencies>
              </project>
              """.formatted(firstVersion, firstScope, secondVersion, secondScope),
            spec -> spec.afterRecipe(pom -> {
                MavenResolutionResult resolution = pom.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                assertThat(resolution.findDependencies("com.fasterxml.jackson.core", "jackson-core", Scope.fromName(firstScope)))
                  .hasSize(1)
                  .extracting(ResolvedDependency::getGav)
                  .extracting(ResolvedGroupArtifactVersion::getVersion)
                  .containsExactly(firstVersion);
                assertThat(resolution.findDependencies("com.fasterxml.jackson.core", "jackson-core", Scope.fromName(secondScope)))
                  .hasSize(1)
                  .extracting(ResolvedDependency::getGav)
                  .extracting(ResolvedGroupArtifactVersion::getVersion)
                  .containsExactly(secondVersion);
            })
          )
        );
    }

    @CsvSource({"2.15.0,2.13.0", "2.13.0,2.15.0", "2.13.0,2.13.0"})
    @ParameterizedTest
    void lastListedDependencyIsUsedForTransitiveScope(String firstVersion, String secondVersion) {
        rewriteRun(
          pomXml(
            //language=xml
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>com.fasterxml.jackson.core</groupId>
                    <artifactId>jackson-core</artifactId>
                    <version>%s</version>
                    <scope>test</scope>
                  </dependency>
                  <dependency>
                    <groupId>com.fasterxml.jackson.core</groupId>
                    <artifactId>jackson-core</artifactId>
                    <version>%s</version>
                    <scope>compile</scope>
                  </dependency>
                </dependencies>
              </project>
              """.formatted(firstVersion, secondVersion),
            spec -> spec.afterRecipe(pom -> {
                MavenResolutionResult resolution = pom.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                assertThat(resolution.findDependencies("com.fasterxml.jackson.core", "jackson-core", Scope.Test))
                  .hasSize(1)
                  .extracting(ResolvedDependency::getGav)
                  .extracting(ResolvedGroupArtifactVersion::getVersion)
                  .containsExactly(secondVersion);
                assertThat(resolution.findDependencies("com.fasterxml.jackson.core", "jackson-core", Scope.Compile))
                  .hasSize(1)
                  .extracting(ResolvedDependency::getGav)
                  .extracting(ResolvedGroupArtifactVersion::getVersion)
                  .containsExactly(secondVersion);
            })
          )
        );
    }

    @MethodSource("jacksonDependencies")
    @ParameterizedTest
    void firstDeclarationWinsForEqualDistanceOfTransitiveDependencies(String dependencies, String resolvedTransitiveVersion) {
        rewriteRun(
          pomXml(
            //language=xml
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  %s
                </dependencies>
              </project>
              """.formatted(dependencies),
            spec -> spec.afterRecipe(pom -> {
                MavenResolutionResult resolution = pom.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                assertThat(resolution.findDependencies("com.fasterxml.jackson.core", "jackson-databind", Scope.Compile))
                  .hasSize(1)
                  .extracting(ResolvedDependency::getGav)
                  .extracting(ResolvedGroupArtifactVersion::getVersion)
                  .containsExactly(resolvedTransitiveVersion);
            })
          )
        );
    }

    private static Stream<Arguments> jacksonDependencies() {
        return Stream.of(
          Arguments.of(
            """
            <dependency>
                <groupId>com.fasterxml.jackson.dataformat</groupId>
                <artifactId>jackson-dataformat-xml</artifactId>
                <version>2.18.0</version>
            </dependency>
            <dependency>
                <groupId>com.fasterxml.jackson.datatype</groupId>
                <artifactId>jackson-datatype-jsr310</artifactId>
                <version>2.15.3</version>
            </dependency>
            """,
            "2.18.0"),
          Arguments.of(
            """
            <dependency>
                <groupId>com.fasterxml.jackson.datatype</groupId>
                <artifactId>jackson-datatype-jsr310</artifactId>
                <version>2.15.3</version>
            </dependency>
            <dependency>
                <groupId>com.fasterxml.jackson.dataformat</groupId>
                <artifactId>jackson-dataformat-xml</artifactId>
                <version>2.18.0</version>
            </dependency>
            """,
            "2.15.3")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"latest", "release"})
    void latestOrReleaseVersionInDependencyManagement(String version) {
        rewriteRun(
          pomXml(
            //language=xml
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencyManagement>
                  <dependencies>
                     <dependency>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-dependencies</artifactId>
                      <version>%s</version>
                      <type>pom</type>
                      <scope>import</scope>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter</artifactId>
                  </dependency>
                </dependencies>
              </project>
              """.formatted(version)
          )
        );
    }

    @Disabled
    @Test
    void weirdImpalaPom() {
        // Maven says:
        // The POM for Impala:ImpalaJDBC42:jar:2.6.26.1031 is invalid, transitive dependencies (if any) will not be available, enable debug logging for more details
        // But does not fail the build as it does not actually have any transitive dependencies
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>

                  <groupId>test</groupId>
                  <artifactId>impala-test</artifactId>
                  <version>1.0.0-SNAPSHOT</version>
                  <packaging>jar</packaging>

                  <name>Impala test</name>
                  <description>The POM for Impala:ImpalaJDBC42:jar:2.6.26.1031 is invalid, transitive dependencies (if any) will not be available, enable debug logging for more details</description>

                  <dependencies>
                      <dependency>
                          <groupId>Impala</groupId>
                          <artifactId>ImpalaJDBC42</artifactId>
                          <version>2.6.26.1031</version>
                      </dependency>
                  </dependencies>

                  <repositories>
                      <repository>
                          <id>cloudera</id>
                          <name>cloudera</name>
                          <url>https://repository.cloudera.com/repository/cloudera-repos/</url>
                      </repository>
                  </repositories>
              </project>
              """
          )
        );
    }

    @Nested
    class DependencyManagement {
        // https://repo1.maven.org/maven2/org/apache/maven/plugins/maven-site-plugin/3.9.1/
        @Issue("https://github.com/openrewrite/rewrite/issues/5402")
        @Test
        void simple() {
            rewriteRun(
              mavenProject(
                "my-bom",
                pomXml(
                  """
                    <project>
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-bom</artifactId>
                      <version>1</version>
                      <dependencyManagement>
                        <dependencies>
                          <dependency>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-site-plugin</artifactId>
                            <version>3.9.1</version>
                          </dependency>
                        </dependencies>
                      </dependencyManagement>
                    </project>
                    """
                ),
                mavenProject(
                  "my-app",
                  pomXml(
                    """
                      <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                        <dependencyManagement>
                          <dependencies>
                            <dependency>
                              <groupId>com.mycompany.app</groupId>
                              <artifactId>my-bom</artifactId>
                              <version>1</version>
                              <scope>import</scope>
                              <type>pom</type>
                            </dependency>
                          </dependencies>
                        </dependencyManagement>
                        <dependencies>
                          <dependency>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-site-plugin</artifactId>
                          </dependency>
                        </dependencies>
                      </project>
                      """,
                    spec -> spec.afterRecipe(pom -> {
                        MavenResolutionResult resolution = pom.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                        List<ResolvedDependency> resolvedDependencies = resolution.getDependencies().get(Scope.Compile);
                        assertThat(resolvedDependencies).satisfiesOnlyOnce(dep -> {
                            assertThat(dep.getGav().getGroupId()).isEqualTo("org.apache.maven.plugins");
                            assertThat(dep.getGav().getArtifactId()).isEqualTo("maven-site-plugin");
                            assertThat(dep.getGav().getVersion()).isEqualTo("3.9.1");
                        });
                    })
                  )
                )
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite/issues/5402")
        @Test
        void withType() {
            rewriteRun(
              mavenProject(
                "my-bom",
                pomXml(
                  """
                    <project>
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-bom</artifactId>
                      <version>1</version>
                      <dependencyManagement>
                        <dependencies>
                          <dependency>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-site-plugin</artifactId>
                            <version>3.9.1</version>
                            <classifier>source-release</classifier>
                            <type>zip</type>
                          </dependency>
                        </dependencies>
                      </dependencyManagement>
                    </project>
                    """
                ),
                mavenProject(
                  "my-app",
                  pomXml(
                    """
                      <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                        <dependencyManagement>
                          <dependencies>
                            <dependency>
                              <groupId>com.mycompany.app</groupId>
                              <artifactId>my-bom</artifactId>
                              <version>1</version>
                              <scope>import</scope>
                              <type>pom</type>
                            </dependency>
                          </dependencies>
                        </dependencyManagement>
                        <dependencies>
                          <dependency>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-site-plugin</artifactId>
                            <classifier>source-release</classifier>
                            <type>zip</type>
                          </dependency>
                        </dependencies>
                      </project>
                      """,
                    spec -> spec.afterRecipe(pom -> {
                        MavenResolutionResult resolution = pom.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                        List<ResolvedDependency> resolvedDependencies = resolution.getDependencies().get(Scope.Compile);
                        assertThat(resolvedDependencies).satisfiesOnlyOnce(dep -> {
                            assertThat(dep.getGav().getGroupId()).isEqualTo("org.apache.maven.plugins");
                            assertThat(dep.getGav().getArtifactId()).isEqualTo("maven-site-plugin");
                            assertThat(dep.getGav().getVersion()).isEqualTo("3.9.1");
                        });
                    })
                  )
                )
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite/issues/5402")
        @Test
        void twoDependencyManagementEntries() {
            rewriteRun(
              mavenProject(
                "my-bom",
                pomXml(
                  """
                    <project>
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-bom</artifactId>
                      <version>1</version>
                      <dependencyManagement>
                        <dependencies>
                          <dependency>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-site-plugin</artifactId>
                            <version>3.9.1</version>
                          </dependency>
                          <dependency>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-site-plugin</artifactId>
                            <version>3.9.1</version>
                            <classifier>source-release</classifier>
                            <type>zip</type>
                          </dependency>
                        </dependencies>
                      </dependencyManagement>
                    </project>
                    """
                ),
                mavenProject(
                  "my-app",
                  pomXml(
                    """
                      <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                        <dependencyManagement>
                          <dependencies>
                            <dependency>
                              <groupId>com.mycompany.app</groupId>
                              <artifactId>my-bom</artifactId>
                              <version>1</version>
                              <scope>import</scope>
                              <type>pom</type>
                            </dependency>
                          </dependencies>
                        </dependencyManagement>
                        <dependencies>
                          <dependency>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-site-plugin</artifactId>
                          </dependency>
                        </dependencies>
                      </project>
                      """
                  )
                )
              )
            );
        }


        @Issue("https://github.com/openrewrite/rewrite/issues/5402")
        @Test
        void twoDependencyManagementEntries_dependencyWithType() {
            rewriteRun(
              mavenProject(
                "my-bom",
                pomXml(
                  """
                    <project>
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-bom</artifactId>
                      <version>1</version>
                      <dependencyManagement>
                        <dependencies>
                          <dependency>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-site-plugin</artifactId>
                            <version>3.9.1</version>
                          </dependency>
                          <dependency>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-site-plugin</artifactId>
                            <version>3.9.1</version>
                            <classifier>source-release</classifier>
                            <type>zip</type>
                          </dependency>
                        </dependencies>
                      </dependencyManagement>
                    </project>
                    """
                ),
                mavenProject(
                  "my-app",
                  pomXml(
                    """
                      <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                        <dependencyManagement>
                          <dependencies>
                            <dependency>
                              <groupId>com.mycompany.app</groupId>
                              <artifactId>my-bom</artifactId>
                              <version>1</version>
                              <scope>import</scope>
                              <type>pom</type>
                            </dependency>
                          </dependencies>
                        </dependencyManagement>
                        <dependencies>
                          <dependency>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-site-plugin</artifactId>
                            <classifier>source-release</classifier>
                            <type>zip</type>
                          </dependency>
                        </dependencies>
                      </project>
                      """
                  )
                )
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite/issues/5402")
        @Test
        void twoDependencyManagementEntries_twoDependencies() {
            rewriteRun(
              mavenProject(
                "my-bom",
                pomXml(
                  """
                    <project>
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-bom</artifactId>
                      <version>1</version>
                      <dependencyManagement>
                        <dependencies>
                          <dependency>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-site-plugin</artifactId>
                            <version>3.9.1</version>
                          </dependency>
                          <dependency>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-site-plugin</artifactId>
                            <version>3.9.1</version>
                            <classifier>source-release</classifier>
                            <type>zip</type>
                          </dependency>
                        </dependencies>
                      </dependencyManagement>
                    </project>
                    """
                ),
                mavenProject(
                  "my-app",
                  pomXml(
                    """
                      <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                        <dependencyManagement>
                          <dependencies>
                            <dependency>
                              <groupId>com.mycompany.app</groupId>
                              <artifactId>my-bom</artifactId>
                              <version>1</version>
                              <scope>import</scope>
                              <type>pom</type>
                            </dependency>
                          </dependencies>
                        </dependencyManagement>
                        <dependencies>
                          <dependency>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-site-plugin</artifactId>
                          </dependency>
                          <dependency>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-site-plugin</artifactId>
                            <classifier>source-release</classifier>
                            <type>zip</type>
                          </dependency>
                        </dependencies>
                      </project>
                      """
                  )
                )
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite/issues/5402")
        @Test
        void allDependencyManagementEntryVariants_allDependencyVariants() {
            rewriteRun(
              mavenProject(
                "my-bom",
                pomXml(
                  """
                    <project>
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-bom</artifactId>
                      <version>1</version>
                      <dependencyManagement>
                         <dependencies>
                             <dependency>
                                 <groupId>io.flux-capacitor</groupId>
                                 <artifactId>common</artifactId>
                                 <version>0.1148.0</version>
                             </dependency>
                             <dependency>
                                 <groupId>io.flux-capacitor</groupId>
                                 <artifactId>common</artifactId>
                                 <version>0.1148.0</version>
                                 <type>jar</type>
                             </dependency>
                             <dependency>
                                 <groupId>io.flux-capacitor</groupId>
                                 <artifactId>common</artifactId>
                                 <version>0.1148.0</version>
                                 <scope>compile</scope>
                             </dependency>
                             <dependency>
                                 <groupId>io.flux-capacitor</groupId>
                                 <artifactId>common</artifactId>
                                 <version>0.1148.0</version>
                                 <type>jar</type>
                                 <scope>compile</scope>
                             </dependency>
                             <dependency>
                                 <groupId>io.flux-capacitor</groupId>
                                 <artifactId>common</artifactId>
                                 <version>0.1148.0</version>
                                 <type>pom</type>
                             </dependency>
                             <dependency>
                                 <groupId>io.flux-capacitor</groupId>
                                 <artifactId>common</artifactId>
                                 <version>0.1148.0</version>
                                 <classifier>sources</classifier>
                             </dependency>
                             <dependency>
                                 <groupId>io.flux-capacitor</groupId>
                                 <artifactId>common</artifactId>
                                 <version>0.1148.0</version>
                                 <classifier>javadoc</classifier>
                             </dependency>
                             <dependency>
                                 <groupId>io.flux-capacitor</groupId>
                                 <artifactId>common</artifactId>
                                 <version>0.1148.0</version>
                                 <type>test-jar</type>
                                 <scope>test</scope>
                             </dependency>
                         </dependencies>
                      </dependencyManagement>
                    </project>
                    """
                ),
                mavenProject(
                  "my-app",
                  pomXml(
                    """
                      <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                        <dependencyManagement>
                          <dependencies>
                            <dependency>
                              <groupId>com.mycompany.app</groupId>
                              <artifactId>my-bom</artifactId>
                              <version>1</version>
                              <scope>import</scope>
                              <type>pom</type>
                            </dependency>
                          </dependencies>
                        </dependencyManagement>
                        <dependencies>
                           <dependency>
                             <groupId>io.flux-capacitor</groupId>
                             <artifactId>common</artifactId>
                           </dependency>
                           <dependency>
                             <groupId>io.flux-capacitor</groupId>
                             <artifactId>common</artifactId>
                             <type>jar</type>
                           </dependency>
                           <dependency>
                             <groupId>io.flux-capacitor</groupId>
                             <artifactId>common</artifactId>
                             <scope>compile</scope>
                           </dependency>
                           <dependency>
                             <groupId>io.flux-capacitor</groupId>
                             <artifactId>common</artifactId>
                             <type>jar</type>
                             <scope>compile</scope>
                           </dependency>
                           <dependency>
                             <groupId>io.flux-capacitor</groupId>
                             <artifactId>common</artifactId>
                             <type>pom</type>
                           </dependency>
                           <dependency>
                             <groupId>io.flux-capacitor</groupId>
                             <artifactId>common</artifactId>
                             <classifier>sources</classifier>
                           </dependency>
                           <dependency>
                             <groupId>io.flux-capacitor</groupId>
                             <artifactId>common</artifactId>
                             <classifier>javadoc</classifier>
                           </dependency>
                           <dependency>
                             <groupId>io.flux-capacitor</groupId>
                             <artifactId>common</artifactId>
                             <type>test-jar</type>
                             <scope>test</scope>
                           </dependency>
                        </dependencies>
                      </project>
                      """
                  )
                )
              )
            );
        }

    }
}
