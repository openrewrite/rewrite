/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.maven.tree;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.test.RewriteTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class ResolvedPomTest implements RewriteTest {

    @Test
    void resolveDependencyWithPlaceholderClassifier() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>org.example</groupId>
                <artifactId>foo-parent</artifactId>
                <version>1</version>
                <properties>
                  <netty.version>4.1.101.Final</netty.version>
                  <netty-transport-native-epoll-classifier>linux-x86_64</netty-transport-native-epoll-classifier>
                </properties>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>io.netty</groupId>
                      <artifactId>netty-transport-native-epoll</artifactId>
                      <classifier>${netty-transport-native-epoll-classifier}</classifier>
                      <version>${netty.version}</version>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """,
            spec -> spec.path("pom.xml")
          ),
          pomXml(
            """
              <project>
                <groupId>org.example</groupId>
                <artifactId>foo</artifactId>
                <version>1</version>
                <parent>
                  <groupId>org.example</groupId>
                  <artifactId>foo-parent</artifactId>
                  <version>1</version>
                </parent>
                <dependencies>
                  <dependency>
                    <groupId>io.netty</groupId>
                    <artifactId>netty-transport-native-epoll</artifactId>
                    <classifier>${netty-transport-native-epoll-classifier}</classifier>
                  </dependency>
                </dependencies>
              </project>
              """,
            spec -> spec.path("foo/pom.xml")
          ),
          pomXml(
            """
              <project>
                <groupId>org.example</groupId>
                <artifactId>bar</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                    <groupId>org.example</groupId>
                    <artifactId>foo</artifactId>
                    <version>1</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            spec -> spec.path("bar/pom.xml")
          )
        );
    }

    @Test
    void projectVersion() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>3.17.0</version>
              </project>
              """
          ),
          mavenProject(
            "app",
            pomXml(
              """
                <project>
                    <groupId>org.example</groupId>
                    <artifactId>app</artifactId>
                    <version>${project.version}</version>
                    <parent>
                        <groupId>org.example</groupId>
                        <artifactId>parent</artifactId>
                        <version>3.17.0</version>
                        <relativeParent>../pom.xml</relativeParent>
                    </parent>
                    <dependencies>
                        <dependency>
                            <groupId>org.apache.commons</groupId>
                            <artifactId>commons-lang3</artifactId>
                            <version>${project.version}</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec
                .afterRecipe(doc -> {
                    List<ResolvedDependency> deps = doc.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getDependencies()
                      .get(Scope.Compile);
                    assertThat(deps).hasSize(1);
                    assertThat(deps.getFirst().getVersion()).isEqualTo("3.17.0");
                })
            )
          )
        );
    }

    @Test
    void dependencyWithCircularProjectVersionReference() {
        assertThatThrownBy(() -> rewriteRun(
            pomXml(
              """
                <project>
                    <groupId>org.example</groupId>
                    <artifactId>app</artifactId>
                    <version>${project.version}</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.apache.commons</groupId>
                            <artifactId>commons-lang3</artifactId>
                            <version>${project.version}</version>
                        </dependency>
                    </dependencies>
                </project>
                """
            )
          )
        )
          .cause()
          .isInstanceOf(MavenDownloadingException.class)
          .hasMessageContaining("org.apache.commons:commons-lang3:error.circular.project.version");
    }

    @Test
    void resolveExecutionsFromDifferentParents() {
        String grandFather = """
          <project>
            <modelVersion>4.0.0</modelVersion>
            <groupId>org.example</groupId>
            <artifactId>grandfather</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <packaging>pom</packaging>
            <modules>
              <module>father</module>
            </modules>
            <build>
              <pluginManagement>
                <plugins>
                  <plugin>
                    <artifactId>maven-enforcer-plugin</artifactId>
                    <dependencies>
                      <dependency>
                        <groupId>org.codehaus.mojo</groupId>
                        <artifactId>extra-enforcer-rules</artifactId>
                        <version>1.7.0</version>
                      </dependency>
                    </dependencies>
                    <executions>
                      <execution>
                        <id>grandfather-execution-id</id>
                        <goals>
                          <goal>enforce</goal>
                        </goals>
                        <configuration>
                          <rules>
                            <requireProperty>
                              <property>test</property>
                              <message>Missing maven property test</message>
                              <regex>.+</regex>
                              <regexMessage>Missing maven property test</regexMessage>
                            </requireProperty>
                            <requirePropertyDiverges>
                              <property>test</property>
                              <regex>SHOULD_BE_OVERRIDDEN</regex>
                            </requirePropertyDiverges>
                          </rules>
                        </configuration>
                      </execution>
                    </executions>
                  </plugin>
                </plugins>
              </pluginManagement>
            </build>
          </project>
          """;
        String father = """
          <project>
            <modelVersion>4.0.0</modelVersion>
            <parent>
              <groupId>org.example</groupId>
              <artifactId>grandfather</artifactId>
              <version>1.0.0-SNAPSHOT</version>
            </parent>
            <artifactId>father</artifactId>
            <packaging>pom</packaging>
            <modules>
              <module>child</module>
            </modules>
            <build>
              <pluginManagement>
                <plugins>
                  <plugin>
                    <artifactId>maven-enforcer-plugin</artifactId>
                    <executions>
                      <execution>
                        <id>father-execution-id</id>
                        <goals>
                          <goal>enforce</goal>
                        </goals>
                        <phase>process-sources</phase>
                        <configuration>
                          <rules>
                            <RestrictImports>
                              <reason>Avoid accidental usage of shaded symbols</reason>
                              <bannedImports>
                                <bannedImport>**.shaded.**</bannedImport>
                                <bannedImport>**.repackaged.**</bannedImport>
                                <bannedImport>**.vendor.**</bannedImport>
                              </bannedImports>
                            </RestrictImports>
                          </rules>
                        </configuration>
                      </execution>
                      <execution>
                        <id>grandfather-execution-id</id>
                        <goals>
                          <goal>clean</goal>
                        </goals>
                        <phase>override-by-father</phase>
                      </execution>
                    </executions>
                  </plugin>
                </plugins>
              </pluginManagement>
            </build>
          </project>
          """;
        String child = """
          <project>
            <modelVersion>4.0.0</modelVersion>
            <parent>
              <groupId>org.example</groupId>
              <artifactId>father</artifactId>
              <version>1.0.0-SNAPSHOT</version>
            </parent>
            <artifactId>child</artifactId>
          </project>
          """;
        rewriteRun(
          pomXml(grandFather, spec -> spec.path("pom.xml")),
          pomXml(father, spec -> spec.path("father/pom.xml")),
          pomXml(child, spec -> spec.path("father/child/pom.xml").afterRecipe(doc -> {
                List<Plugin> pluginManagement = doc.getMarkers().findFirst(MavenResolutionResult.class)
                  .orElseThrow().getPom().getPluginManagement();
                assertThat(pluginManagement).hasSize(1);
                Plugin plugin = pluginManagement.getFirst();
                assertThat(plugin).extracting(Plugin::getArtifactId).isEqualTo("maven-enforcer-plugin");
                ObjectMapper objectMapper = new ObjectMapper();
                assertThat(plugin.getExecutions())
                  .hasSize(2)
                  .containsExactlyInAnyOrder(
                    new Plugin.Execution(
                      "father-execution-id",
                      List.of("enforce"),
                      "process-sources",
                      null,
                      objectMapper.readTree(
                        //language=json
                        """
                          {
                              "rules": {
                                  "RestrictImports": {
                                      "reason": "Avoid accidental usage of shaded symbols",
                                      "bannedImports": {
                                          "bannedImport": [
                                              "**.shaded.**",
                                              "**.repackaged.**",
                                              "**.vendor.**"
                                          ]
                                      }
                                  }
                              }
                          }
                          """
                      )
                    ),
                    new Plugin.Execution(
                      "grandfather-execution-id",
                      List.of("enforce", "clean"),
                      "override-by-father",
                      null,
                      objectMapper.readTree(
                        //language=json
                        """
                          {
                              "rules": {
                                  "requireProperty": {
                                      "property": "test",
                                      "message": "Missing maven property test",
                                      "regex": ".+",
                                      "regexMessage": "Missing maven property test"
                                  },
                                  "requirePropertyDiverges": {
                                      "property": "test",
                                      "regex": "SHOULD_BE_OVERRIDDEN"
                                  }
                              }
                          }
                          """
                      )
                    )
                  );
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4687")
    @Nested
    class TolerateMissingPom {

        @Language("xml")
        private static final String POM_WITH_DEPENDENCY = """
          <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>foo</groupId>
              <artifactId>bar</artifactId>
              <version>1.0-SNAPSHOT</version>
              <dependencies>
                  <dependency>
                      <groupId>com.some</groupId>
                      <artifactId>some-artifact</artifactId>
                      <version>1</version>
                  </dependency>
              </dependencies>
          </project>
          """;

        @TempDir
        Path localRepository;
        @TempDir
        Path localRepository2;

        @Test
        void singleRepositoryContainingJar() throws Exception {
            MavenRepository mavenLocal = createMavenRepository(localRepository, "local");
            createJarFile(localRepository);

            List<List<@Nullable Object>> downloadErrorArgs = new ArrayList<>();
            MavenExecutionContextView ctx = MavenExecutionContextView.view(new InMemoryExecutionContext(Throwable::printStackTrace));
            ctx.setRepositories(List.of(mavenLocal));
            ctx.setResolutionListener(new ResolutionEventListener() {
                @Override
                public void downloadError(GroupArtifactVersion gav, List<String> attemptedUris, @Nullable Pom containing) {
                    List<Object> list = new ArrayList<>();
                    list.add(gav);
                    list.add(attemptedUris);
                    list.add(containing);
                    downloadErrorArgs.add(list);
                }
            });
            rewriteRun(
              spec -> spec.executionContext(ctx),
              pomXml(POM_WITH_DEPENDENCY)
            );
            assertThat(downloadErrorArgs).hasSize(1);
        }

        @Test
        void twoRepositoriesSecondContainingJar() throws Exception {
            MavenRepository mavenLocal = createMavenRepository(localRepository, "local");
            MavenRepository mavenLocal2 = createMavenRepository(localRepository2, "local2");
            createJarFile(localRepository2);

            List<List<@Nullable Object>> downloadErrorArgs = new ArrayList<>();
            MavenExecutionContextView ctx = MavenExecutionContextView.view(new InMemoryExecutionContext(Throwable::printStackTrace));
            ctx.setRepositories(List.of(mavenLocal, mavenLocal2));
            ctx.setResolutionListener(new ResolutionEventListener() {
                @Override
                public void downloadError(GroupArtifactVersion gav, List<String> attemptedUris, @Nullable Pom containing) {
                    List<Object> list = new ArrayList<>();
                    list.add(gav);
                    list.add(attemptedUris);
                    list.add(containing);
                    downloadErrorArgs.add(list);
                }
            });
            rewriteRun(
              spec -> spec.executionContext(ctx),
              pomXml(POM_WITH_DEPENDENCY)
            );
            assertThat(downloadErrorArgs).hasSize(1);
        }

    }

    @Nested
    class DependencyManagement {

        @Issue("https://github.com/openrewrite/rewrite-maven-plugin/issues/862")
        @Test
        void resolveVersionFromParentDependencyManagement(@TempDir Path localRepository) throws Exception {
            MavenRepository mavenLocal = createMavenRepository(localRepository, "local");
            createJarFile(localRepository);
            createJarFile(localRepository, "org.openrewrite.test", "lib", "1.0");

            List<List<@Nullable Object>> downloadErrorArgs = new ArrayList<>();
            MavenExecutionContextView ctx = MavenExecutionContextView.view(new InMemoryExecutionContext(Throwable::printStackTrace));
            ctx.setRepositories(List.of(mavenLocal));
            ctx.setResolutionListener(new ResolutionEventListener() {
                @Override
                public void downloadError(GroupArtifactVersion gav, List<String> attemptedUris, @Nullable Pom containing) {
                    List<Object> list = new ArrayList<>();
                    list.add(gav);
                    list.add(attemptedUris);
                    list.add(containing);
                    downloadErrorArgs.add(list);
                }
            });

            String father = """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.example</groupId>
                <artifactId>father</artifactId>
                <version>1.0.0-SNAPSHOT</version>
                <packaging>pom</packaging>
                <modules>
                  <module>childA</module>
                  <module>childB</module>
                </modules>
                <dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>com.some</groupId>
                          <artifactId>some-artifact</artifactId>
                          <version>1</version>
                      </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """;
            String childA = """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <parent>
                  <groupId>org.example</groupId>
                  <artifactId>father</artifactId>
                  <version>1.0.0-SNAPSHOT</version>
                </parent>
                <artifactId>childA</artifactId>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>org.openrewrite.test</groupId>
                            <artifactId>lib</artifactId>
                            <version>1.0</version>
                            <type>pom</type>
                            <scope>import</scope>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
                <dependencies>
                    <dependency>
                      <groupId>com.some</groupId>
                      <artifactId>some-artifact</artifactId>
                    </dependency>
                </dependencies>
              </project>
              """;
            String childB = """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <parent>
                  <groupId>org.example</groupId>
                  <artifactId>father</artifactId>
                  <version>1.0.0-SNAPSHOT</version>
                </parent>
                <artifactId>childB</artifactId>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>com.some</groupId>
                            <artifactId>some-artifact</artifactId>
                            <scope>compile</scope>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
              </project>
              """;

            rewriteRun(
              spec -> spec.executionContext(ctx),
              pomXml(father, spec -> spec.path("pom.xml")),
              pomXml(childA, spec -> spec.path("childA/pom.xml")),
              pomXml(childB, spec -> spec.path("childB/pom.xml").afterRecipe(doc -> {
                    ResolvedPom pom = doc.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom();
                    String version = pom.getManagedVersion("com.some", "some-artifact", null, null);
                    // Assert that version is not null!
                    assertThat(version).isEqualTo("1");
                })
              )
            );
        }
    }

    @Test
    void ignoreScopeInDependencyManagement(@TempDir Path localRepository) throws Exception {
        MavenRepository mavenLocal = createMavenRepository(localRepository, "local");
        createJarFile(localRepository, "com.some", "some-artifact", "1");

        MavenExecutionContextView ctx = MavenExecutionContextView.view(new InMemoryExecutionContext())
          .setRepositories(List.of(mavenLocal));

        rewriteRun(
          spec -> spec.executionContext(ctx),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.example</groupId>
                <artifactId>parent</artifactId>
                <version>1.0.0-SNAPSHOT</version>
                <packaging>pom</packaging>
                <dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>com.some</groupId>
                          <artifactId>some-artifact</artifactId>
                          <version>1</version>
                          <scope>test</scope>
                      </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """,
            spec -> spec.path("pom.xml")),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <parent>
                  <groupId>org.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1.0.0-SNAPSHOT</version>
                  <relativePath>../pom.xml</relativePath>
                </parent>
                <artifactId>child</artifactId>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>com.some</groupId>
                            <artifactId>some-artifact</artifactId>
                            <version>1</version>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>com.some</groupId>
                    <artifactId>some-artifact</artifactId>
                  </dependency>
                </dependencies>
              </project>
              """,
            spec -> spec.path("child/pom.xml")
              .afterRecipe(doc -> {
                  ResolvedPom pom = doc.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom();
                  assertThat(pom.getDependencyManagement()).hasSize(1);
              })
          )
        );
    }

    @Test
    void firstUniqueManagedDependencyWins() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1.0.0-SNAPSHOT</version>
                  <packaging>pom</packaging>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>com.fasterxml.jackson</groupId>
                              <artifactId>jackson-bom</artifactId>
                              <version>2.16.1</version>
                              <type>pom</type>
                              <scope>import</scope>
                          </dependency>
                          <dependency>
                              <groupId>com.fasterxml.jackson</groupId>
                              <artifactId>jackson-bom</artifactId>
                              <version>2.18.1</version>
                              <type>pom</type>
                              <scope>import</scope>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>com.fasterxml.jackson.core</groupId>
                          <artifactId>jackson-core</artifactId>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec
              .afterRecipe(doc -> {
                  ResolvedPom pom = doc.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom();
                  assertThat(pom.getManagedVersion("com.fasterxml.jackson.core", "jackson-core", null, null)).isEqualTo("2.16.1");
              })
          )
        );
    }

    @Test
    void circularProjectVersionReference() {
        // Test case where a property is defined as project.version=${project.version}
        // This creates a circular reference when resolving ${project.version}
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.example</groupId>
                <artifactId>parent-project</artifactId>
                <version>1.0.0-SNAPSHOT</version>
                <packaging>pom</packaging>
              </project>
              """,
            spec -> spec.path("pom.xml")
          ),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <parent>
                  <groupId>org.example</groupId>
                  <artifactId>parent-project</artifactId>
                  <version>1.0.0-SNAPSHOT</version>
                  <relativePath>../pom.xml</relativePath>
                </parent>
                <artifactId>child-project</artifactId>
                <properties>
                  <!-- This creates a circular reference! -->
                  <project.version>${project.version}</project.version>
                </properties>
              </project>
              """,
            spec -> spec.path("child/pom.xml")
              .afterRecipe(doc -> {
                  // This should not throw a StackOverflowError
                  ResolvedPom pom = doc.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom();
                  // When project.version property shadows the built-in one,
                  // it should still resolve to the actual version
                  String resolvedVersion = pom.getValue("${project.version}");
                  assertThat(resolvedVersion).isEqualTo("1.0.0-SNAPSHOT");
              })
          )
        );
    }

    @Test
    void circularProjectVersionInDependency() {
        // Test case where a property shadows project.version and is used in a dependency
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.example</groupId>
                <artifactId>parent-project</artifactId>
                <version>1.0.0-SNAPSHOT</version>
                <packaging>pom</packaging>
              </project>
              """,
            spec -> spec.path("pom.xml")
          ),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <parent>
                  <groupId>org.example</groupId>
                  <artifactId>parent-project</artifactId>
                  <version>1.0.0-SNAPSHOT</version>
                  <relativePath>../pom.xml</relativePath>
                </parent>
                <artifactId>child-project</artifactId>
                <properties>
                  <!-- This creates a circular reference! -->
                  <project.version>${project.version}</project.version>
                  <my.version>${project.version}</my.version>
                </properties>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.example</groupId>
                      <artifactId>example-lib</artifactId>
                      <version>${my.version}</version>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """,
            spec -> spec.path("child/pom.xml")
              .afterRecipe(doc -> {
                  // This should not throw a StackOverflowError during dependency resolution
                  ResolvedPom pom = doc.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom();
                  // The managed version should resolve correctly
                  String managedVersion = pom.getManagedVersion("com.example", "example-lib", null, null);
                  assertThat(managedVersion).isEqualTo("1.0.0-SNAPSHOT");
              })
          )
        );
    }

    private static void createJarFile(Path localRepository1) throws IOException {
        createJarFile(localRepository1, "com/some", "some-artifact", "1");
    }

    private static void createJarFile(Path localRepository, String groupId, String artifactId, String version) throws IOException {
        Path localJar = localRepository.resolve("%s/%s/%s/%s-%s.jar".formatted(
          groupId.replace('.', '/'), artifactId, version, artifactId, version));
        assertThat(localJar.getParent().toFile().mkdirs()).isTrue();
        Files.writeString(localJar, "some content not to be empty");
    }

    @Test
    void siblingDependencyWithTgzPackagingAndSnapshot() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>api-parent</artifactId>
                  <version>1.0.0-SNAPSHOT</version>
                  <packaging>pom</packaging>

                  <modules>
                      <module>api-definitions</module>
                      <module>api-codegen</module>
                  </modules>
              </project>
              """
          ),
          mavenProject(
            "api-definitions",
            pomXml("""
                <project>
                    <artifactId>api-definitions</artifactId>
                    <packaging>tgz</packaging>

                    <parent>
                        <groupId>org.example</groupId>
                        <artifactId>api-parent</artifactId>
                        <version>1.0.0-SNAPSHOT</version>
                        <relativePath>../pom.xml</relativePath>
                    </parent>
                </project>
                """,
              spec -> spec.path("api-definitions/pom.xml")
            )
          ),
          mavenProject(
            "api-codegen",
            pomXml("""
                <project>
                    <artifactId>api-codegen</artifactId>

                    <parent>
                        <groupId>org.example</groupId>
                        <artifactId>api-parent</artifactId>
                        <version>1.0.0-SNAPSHOT</version>
                        <relativePath>../pom.xml</relativePath>
                    </parent>

                    <dependencies>
                        <dependency>
                            <groupId>org.example</groupId>
                            <artifactId>api-definitions</artifactId>
                            <version>${project.version}</version>
                            <type>tgz</type>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.path("api-codegen/pom.xml").afterRecipe(pom -> {
                  List<ResolvedDependency> deps = pom.getMarkers().findFirst(MavenResolutionResult.class)
                    .orElseThrow().getDependencies().get(Scope.Compile);
                  assertThat(deps).hasSize(1);
                  assertThat(deps.getFirst().getArtifactId()).isEqualTo("api-definitions");
                  assertThat(deps.getFirst().getVersion()).isEqualTo("1.0.0-SNAPSHOT");
                  assertThat(deps.getFirst().getType()).isEqualTo("tgz");
              })
            )
          )
        );
    }

    private static MavenRepository createMavenRepository(Path localRepository, String name) {
        return MavenRepository.builder()
          .id(name)
          .uri(localRepository.toUri().toString())
          .snapshots(false)
          .knownToExist(true)
          .build();
    }
}
