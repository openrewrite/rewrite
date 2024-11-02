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
import org.junit.jupiter.api.Test;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.ParseExceptionResult;
import org.openrewrite.Parser;
import org.openrewrite.ipc.http.OkHttpSender;
import org.openrewrite.maven.internal.MavenParsingException;
import org.openrewrite.maven.tree.*;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;
import org.openrewrite.tree.ParseError;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/2603")
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
                  var dependency = results.findDependencies("junit", "junit", Scope.Compile).get(0);
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
                  var dependency = results.findDependencies("junit", "junit", Scope.Compile).get(0);
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
                  var dependency = results.findDependencies("junit", "junit", Scope.Compile).get(0);
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
                  var dependency = results.findDependencies("junit", "junit", Scope.Compile).get(0);
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
                .findDependencies("com.google.guava", "guava", null).get(0).getVersion())
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
                  .getRepositories().get(0).getId())
                  .isEqualTo("coolId");
                assertThat(pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom()
                  .getRepositories().get(0).getUri())
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
                  .get(Scope.Test).get(0).getLicenses().get(0).getType())
                  .isEqualTo(License.Type.Eclipse);
                assertThat(pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getDependencies()
                  .get(Scope.Test).get(0).getType())
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
    void mirrorsAndAuth() throws IOException {
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
            var settings = MavenSettings.parse(Parser.Input.fromString(Paths.get("settings.xml"),
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
              .matches(deps -> deps.get(0).getGroupId().equals("com.foo") &&
                               deps.get(0).getArtifactId().equals("bar"));
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
                  .get(Scope.Compile).get(0).getVersion())
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
                  .get(Scope.Compile).get(0).getVersion())
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
                .getPom().getRequested().getProfiles().get(0).getActivation())
                .isNull()
            )
          )
        );
    }

    @SuppressWarnings("CheckTagEmptyBody")
    @Issue("https://github.com/openrewrite/rewrite/issues/1427")
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
                  .getPom().getRequested().getProfiles().get(0).getActivation();
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
                  .getPom().getRequested().getProfiles().get(0).getActivation();
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
                  .matches(deps -> deps.get(0).getArtifactId().equals("guava") &&
                                   deps.get(0).getVersion().equals("29.0-jre"))
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
                    assertThat(compileDependencies).anyMatch(it -> it.getArtifactId().equals("b") &&
                                                                   it.getVersion().equals("0.1.0-SNAPSHOT"));
                    assertThat(compileDependencies).anyMatch(it -> it.getArtifactId().equals("d") &&
                                                                   it.getVersion().equals("0.1.0-SNAPSHOT"));
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
                  assertThat(compileDependencies).anyMatch(it -> it.getArtifactId().equals("junit") &&
                                                                 it.getVersion().equals("4.11"));
                  assertThat(compileDependencies).noneMatch(it -> it.getArtifactId().equals("hamcrest-core"));
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

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/2049")
    void ciFriendlyVersionWithoutExplicitProperty() {
        rewriteRun(
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
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

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/2049")
    void ciFriendlyVersionWithParent() {
        rewriteRun(
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>net.sample</groupId>
                <artifactId>sample</artifactId>
                <version>${revision}</version>
                <packaging>pom</packaging>
              
                <modules>
                  <module>sample-rest</module>
                </modules>
              
              </project>
              """, spec -> spec.path("pom.xml")),
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
              
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
              """, spec -> spec.path("rest/pom.xml"))
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/2049")
    void canConnectProjectPomsWhenUsingCiFriendlyVersions() {
        rewriteRun(
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
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
              """, spec -> spec.path("pom.xml")),
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
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
              """, spec -> spec.path("parent/pom.xml")),
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
              
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
              """, spec -> spec.path("app/pom.xml")),
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
              
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
              """, spec -> spec.path("rest/pom.xml")),
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
              
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
              """, spec -> spec.path("web/pom.xml"))
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/2049")
    void ciFriendlyVersionsStillWorkAfterUpdateMavenModel() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("junit", "junit", "4.1", null, null, null)),
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
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
              """, spec -> spec.path("pom.xml")),
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
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
              """, spec -> spec.path("parent/pom.xml")),
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
              
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
              """, """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
              
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
              """, spec -> spec.path("app/pom.xml")),
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
              
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
              """, spec -> spec.path("rest/pom.xml")),
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
              
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
              """, spec -> spec.path("web/pom.xml"))
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/2373")
    void multipleCiFriendlyVersionPlaceholders() {
        rewriteRun(
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
              
                <parent>
                  <groupId>bogus.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>${revision}${changelist}</version>
                </parent>
              
                <artifactId>sub</artifactId>
              </project>
              """, spec -> spec.path("sub/pom.xml"))
        );
    }

    @Test
    void optionalDependencies() {
        rewriteRun(
          mavenProject("a",
            pomXml("""
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample.optional</groupId>
                <artifactId>a</artifactId>
                <version>1.0.0</version>
              </project>
              """
            )),
          mavenProject("b",
            pomXml("""
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
                .noneMatch(dep -> dep.getGav().getArtifactId().equals("a"))
                .anyMatch(dep -> dep.getGav().getArtifactId().equals("b"))
                .anyMatch(dep -> dep.getGav().getArtifactId().equals("junit")))
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
                  Plugin plugin = pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom().getPlugins().get(0);
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
                  Plugin plugin = pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom().getPlugins().get(0);
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
                  .get(0).getArtifactId())
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
                .get(0).getArtifactId())
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
                  .get(0).getArtifactId())
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
                assertThat(pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom().getPlugins().get(0).getConfiguration())
                  .hasSize(2)
                  .anyMatch(elem -> elem.asText().equals("11"))
                  .anyMatch(elem -> elem.asText().equals("17"))
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
                assertThat(pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom().getPlugins().get(0).getConfiguration())
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
                assertThat(pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom().getPlugins().get(0).getConfiguration())
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
                assertThat(pomXml.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom().getPlugins().get(0).getConfiguration())
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

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/4093")
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
                  GroupArtifactVersion gav = resolution.getPom().getDependencyManagement().get(0).getGav();
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
                    GroupArtifactVersion gav = resolution.getPom().getDependencyManagement().get(0).getGav();
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

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/4319")
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
                      assertThat(res.getDependencies().get(Scope.Test)).isNotEmpty().anyMatch(dep -> dep.getGroupId().equals("com.google.guava") && dep.getArtifactId().equals("guava"));
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
}
