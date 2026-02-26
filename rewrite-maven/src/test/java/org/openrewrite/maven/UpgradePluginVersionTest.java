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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class UpgradePluginVersionTest implements RewriteTest {

    @DocumentExample
    @Test
    void upgradeToExactVersion() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion(
            "io.quarkus",
            "quarkus-maven-plugin",
            "1.13.5.Final",
            null,
            null,
            null
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <build>
                  <plugins>
                    <plugin>
                      <groupId>io.quarkus</groupId>
                      <artifactId>quarkus-maven-plugin</artifactId>
                      <version>1.13.3.Final</version>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <build>
                  <plugins>
                    <plugin>
                      <groupId>io.quarkus</groupId>
                      <artifactId>quarkus-maven-plugin</artifactId>
                      <version>1.13.5.Final</version>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/565")
    @Test
    void handlesPropertyResolution() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion(
            "io.quarkus",
            "quarkus-maven-plugin",
            "1.13.5.Final",
            null,
            null,
            null
          )),
          pomXml(
            """
              <project>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <properties>
                  <quarkus-plugin.group-id>io.quarkus</quarkus-plugin.group-id>
                  <quarkus-plugin.artifact-id>quarkus-maven-plugin</quarkus-plugin.artifact-id>
                  <quarkus-plugin.version>1.13.3.Final</quarkus-plugin.version>
                </properties>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>${quarkus-plugin.group-id}</groupId>
                      <artifactId>${quarkus-plugin.artifact-id}</artifactId>
                      <version>${quarkus-plugin.version}</version>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """,
            """
              <project>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <properties>
                  <quarkus-plugin.group-id>io.quarkus</quarkus-plugin.group-id>
                  <quarkus-plugin.artifact-id>quarkus-maven-plugin</quarkus-plugin.artifact-id>
                  <quarkus-plugin.version>1.13.5.Final</quarkus-plugin.version>
                </properties>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>${quarkus-plugin.group-id}</groupId>
                      <artifactId>${quarkus-plugin.artifact-id}</artifactId>
                      <version>${quarkus-plugin.version}</version>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """
          )
        );
    }

    @Nested
    class PluginRepos {

        @Issue("https://github.com/openrewrite/rewrite/issues/5065")
        @Test
        void update() {
            rewriteRun(
              spec -> spec.recipe(new UpgradePluginVersion(
                "io.quarkus",
                "quarkus-maven-plugin",
                "3.15.3.redhat-00002",
                null,
                null,
                null
              )),
              pomXml(
                """
                  <project>
                    <groupId>org.openrewrite.example</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <build>
                      <plugins>
                        <plugin>
                          <groupId>io.quarkus</groupId>
                          <artifactId>quarkus-maven-plugin</artifactId>
                          <version>3.14.0</version>
                         </plugin>
                      </plugins>
                    </build>
                    <pluginRepositories>
                      <pluginRepository>
                        <id>redhat-ga</id>
                        <url>https://maven.repository.redhat.com/ga/</url>
                      </pluginRepository>
                    </pluginRepositories>
                  </project>
                  """,
                """
                  <project>
                    <groupId>org.openrewrite.example</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <build>
                      <plugins>
                        <plugin>
                          <groupId>io.quarkus</groupId>
                          <artifactId>quarkus-maven-plugin</artifactId>
                          <version>3.15.3.redhat-00002</version>
                         </plugin>
                      </plugins>
                    </build>
                    <pluginRepositories>
                      <pluginRepository>
                        <id>redhat-ga</id>
                        <url>https://maven.repository.redhat.com/ga/</url>
                      </pluginRepository>
                    </pluginRepositories>
                  </project>
                  """
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite/issues/5065")
        @Test
        void repoUnreachable() {
            rewriteRun(
              spec -> spec.recipe(new UpgradePluginVersion(
                "io.quarkus",
                "quarkus-maven-plugin",
                "3.15.3.redhat-00002",
                null,
                null,
                null
              )),
              pomXml(
                """
                  <project>
                    <groupId>org.openrewrite.example</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <build>
                      <plugins>
                        <plugin>
                          <groupId>io.quarkus</groupId>
                          <artifactId>quarkus-maven-plugin</artifactId>
                          <version>3.14.0</version>
                         </plugin>
                      </plugins>
                    </build>
                    <pluginRepositories>
                      <pluginRepository>
                        <id>unreachable</id>
                        <url>https://nexus.client.com/repository/maven-public</url>
                      </pluginRepository>
                    </pluginRepositories>
                  </project>
                  """
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite/issues/5065")
        @Test
        void noNewerVersion() {
            rewriteRun(
              spec -> spec.recipe(new UpgradePluginVersion(
                "io.quarkus",
                "quarkus-maven-plugin",
                "3.15.3.redhat-00002",
                null,
                null,
                null
              )),
              pomXml(
                """
                  <project>
                    <groupId>org.openrewrite.example</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <build>
                      <plugins>
                        <plugin>
                          <groupId>io.quarkus</groupId>
                          <artifactId>quarkus-maven-plugin</artifactId>
                          <version>3.14.0</version>
                         </plugin>
                      </plugins>
                    </build>
                    <pluginRepositories>
                      <pluginRepository>
                        <id>maven-mirror1</id>
                        <url>https://repo1.maven.org/maven2/</url>
                      </pluginRepository>
                    </pluginRepositories>
                  </project>
                  """
              )
            );
        }
    }

    @Issue("Should be changed/removed when this recipe supports dynamic version resolution")
    @Test
    void ignorePluginWithoutExplicitVersionDeclared() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion(
            "io.quarkus",
            "quarkus-maven-plugin",
            "1.13.5.Final",
            null,
            null,
            null
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <build>
                  <plugins>
                    <plugin>
                      <groupId>io.quarkus</groupId>
                      <artifactId>quarkus-maven-plugin</artifactId>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """
          )
        );
    }

    @Test
    void upgradeVersionDynamicallyUsingPattern() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion(
            "org.openrewrite.maven",
            "rewrite-maven-plugin",
            "~4.2",
            null,
            null,
            null
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.openrewrite.maven</groupId>
                      <artifactId>rewrite-maven-plugin</artifactId>
                      <version>4.2.0</version>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.openrewrite.maven</groupId>
                      <artifactId>rewrite-maven-plugin</artifactId>
                      <version>4.2.3</version>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """
          )
        );
    }

    @Test
    void upgradeVersionIgnoringParent() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion(
            "org.openrewrite.maven",
            "rewrite-maven-plugin",
            "4.2.x",
            null,
            null,
            null
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <packaging>pom</packaging>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app-bom</artifactId>
                <version>1</version>

                <build>
                  <pluginManagement>
                    <plugins>
                      <plugin>
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>rewrite-maven-plugin</artifactId>
                        <version>4.2.2</version>
                      </plugin>
                    </plugins>
                  </pluginManagement>
                </build>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <packaging>pom</packaging>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app-bom</artifactId>
                <version>1</version>

                <build>
                  <pluginManagement>
                    <plugins>
                      <plugin>
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>rewrite-maven-plugin</artifactId>
                        <version>4.2.3</version>
                      </plugin>
                    </plugins>
                  </pluginManagement>
                </build>
              </project>
              """
          ),
          mavenProject("server",
            pomXml(
              """
                    <project>
                      <parent>
                        <groupId>org.openrewrite.example</groupId>
                        <artifactId>my-app-bom</artifactId>
                        <version>1</version>
                      </parent>
                      <groupId>org.openrewrite.example</groupId>
                      <artifactId>my-app-server</artifactId>
                      <version>1</version>
                      <build>
                        <plugins>
                          <plugin>
                            <groupId>org.openrewrite.maven</groupId>
                            <artifactId>rewrite-maven-plugin</artifactId>
                            <version>4.2.1</version>
                          </plugin>
                        </plugins>
                      </build>
                    </project>
                """,
              """
                    <project>
                      <parent>
                        <groupId>org.openrewrite.example</groupId>
                        <artifactId>my-app-bom</artifactId>
                        <version>1</version>
                      </parent>
                      <groupId>org.openrewrite.example</groupId>
                      <artifactId>my-app-server</artifactId>
                      <version>1</version>
                      <build>
                        <plugins>
                          <plugin>
                            <groupId>org.openrewrite.maven</groupId>
                            <artifactId>rewrite-maven-plugin</artifactId>
                            <version>4.2.3</version>
                          </plugin>
                        </plugins>
                      </build>
                    </project>
                """
            )
          )
        );
    }

    @Disabled
    @Test
    void trustParent() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion(
            "org.openrewrite.maven",
            "rewrite-maven-plugin",
            "4.2.x",
            null,
            true,
            null
          )),
          pomXml(
            """
              <project>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app-bom</artifactId>
                <version>1</version>
                <build>
                  <pluginManagement>
                    <plugins>
                      <plugin>
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>rewrite-maven-plugin</artifactId>
                        <version>4.2.2</version>
                      </plugin>
                    </plugins>
                  </pluginManagement>
                </build>
              </project>
              """
          ),
          mavenProject("server",
            pomXml(
              """
                    <project>
                      <modelVersion>4.0.0</modelVersion>

                      <parent>
                        <groupId>org.openrewrite.example</groupId>
                        <artifactId>my-app-bom</artifactId>
                        <version>1</version>
                      </parent>

                      <groupId>org.openrewrite.example</groupId>
                      <artifactId>my-app-server</artifactId>
                      <version>1</version>

                      <build>
                        <plugins>
                          <plugin>
                            <groupId>org.openrewrite.maven</groupId>
                            <artifactId>rewrite-maven-plugin</artifactId>
                            <version>4.2.1</version>
                          </plugin>
                        </plugins>
                      </build>
                    </project>
                """,
              """
                    <project>
                      <modelVersion>4.0.0</modelVersion>

                      <parent>
                        <groupId>org.openrewrite.example</groupId>
                        <artifactId>my-app-bom</artifactId>
                        <version>1</version>
                      </parent>

                      <groupId>org.openrewrite.example</groupId>
                      <artifactId>my-app-server</artifactId>
                      <version>1</version>

                      <build>
                        <plugins>
                          <plugin>
                            <groupId>org.openrewrite.maven</groupId>
                            <artifactId>rewrite-maven-plugin</artifactId>
                            <version>4.2.2</version>
                          </plugin>
                        </plugins>
                      </build>
                    </project>
                """
            )
          )
        );
    }

    @Disabled
    @Test
    void upgradePluginInParent() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion(
            "org.openrewrite.maven",
            "rewrite-maven-plugin",
            "4.2.3",
            null,
            null,
            null
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <packaging>pom</packaging>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app-bom</artifactId>
                <version>1</version>

                <properties>
                  <rewrite-maven-plugin.version>4.2.2</rewrite-maven-plugin.version>
                </properties>
              </project>
              """
          ),
          mavenProject("server",
            pomXml(
              """
                    <project>
                      <modelVersion>4.0.0</modelVersion>

                      <parent>
                        <groupId>org.openrewrite.example</groupId>
                        <artifactId>my-app-bom</artifactId>
                        <version>1</version>
                      </parent>

                      <groupId>org.openrewrite.example</groupId>
                      <artifactId>my-app-server</artifactId>
                      <version>1</version>

                      <build>
                        <plugins>
                          <plugin>
                            <groupId>org.openrewrite.maven</groupId>
                            <artifactId>rewrite-maven-plugin</artifactId>
                            <version>${rewrite-maven-plugin.version}</version>
                          </plugin>
                        </plugins>
                      </build>
                    </project>
                """,
              """
                    <project>
                      <modelVersion>4.0.0</modelVersion>

                      <packaging>pom</packaging>
                      <groupId>org.openrewrite.example</groupId>
                      <artifactId>my-app-bom</artifactId>
                      <version>1</version>

                      <properties>
                        <rewrite-maven-plugin.version>4.2.3</rewrite-maven-plugin.version>
                      </properties>
                    </project>
                """
            )
          )
        );
    }

    @Test
    void upgradeMultiplePluginsWithDifferentVersions() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion(
            "*",
            "*",
            "2.4.x",
            null,
            null,
            null
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <build>
                  <plugins>
                    <plugin>
                      <groupId>io.quarkus</groupId>
                      <artifactId>quarkus-maven-plugin</artifactId>
                      <version>1.13.3.Final</version>
                    </plugin>
                    <plugin>
                      <groupId>org.openrewrite.maven</groupId>
                      <artifactId>rewrite-maven-plugin</artifactId>
                      <version>1.0.0</version>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <build>
                  <plugins>
                    <plugin>
                      <groupId>io.quarkus</groupId>
                      <artifactId>quarkus-maven-plugin</artifactId>
                      <version>2.4.2.Final</version>
                    </plugin>
                    <plugin>
                      <groupId>org.openrewrite.maven</groupId>
                      <artifactId>rewrite-maven-plugin</artifactId>
                      <version>2.4.13</version>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """
          )
        );
    }

    @Test
    void upgradePluginsVersionOnProperties() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion(
            "*",
            "*",
            "2.4.x",
            null,
            null,
            null
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <properties>
                  <quarkus-maven-plugin.version>1.13.3.Final</quarkus-maven-plugin.version>
                  <rewrite-maven-plugin.version>1.0.0</rewrite-maven-plugin.version>
                </properties>

                <build>
                  <plugins>
                    <plugin>
                      <groupId>io.quarkus</groupId>
                      <artifactId>quarkus-maven-plugin</artifactId>
                      <version>${quarkus-maven-plugin.version}</version>
                    </plugin>
                    <plugin>
                      <groupId>org.openrewrite.maven</groupId>
                      <artifactId>rewrite-maven-plugin</artifactId>
                      <version>${rewrite-maven-plugin.version}</version>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <properties>
                  <quarkus-maven-plugin.version>2.4.2.Final</quarkus-maven-plugin.version>
                  <rewrite-maven-plugin.version>2.4.13</rewrite-maven-plugin.version>
                </properties>

                <build>
                  <plugins>
                    <plugin>
                      <groupId>io.quarkus</groupId>
                      <artifactId>quarkus-maven-plugin</artifactId>
                      <version>${quarkus-maven-plugin.version}</version>
                    </plugin>
                    <plugin>
                      <groupId>org.openrewrite.maven</groupId>
                      <artifactId>rewrite-maven-plugin</artifactId>
                      <version>${rewrite-maven-plugin.version}</version>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """
          )
        );
    }

    @Test
    void doNotBumpToPreReleaseUnintentionally() {
        rewriteRun(
          spec -> spec.recipes(
            new UpgradePluginVersion(
              "org.apache.maven.plugins",
              "maven-failsafe-plugin",
              "3.1.x",
              null,
              null,
              null
            ),
            new UpgradePluginVersion(
              "org.apache.maven.plugins",
              "maven-checkstyle-plugin",
              "3.6.x",
              null,
              null,
              null
            )
          ),
          //language=xml
          pomXml(
            """
              <project>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <properties>
                  <version.maven.plugin.failsafe>3.0.0-M8</version.maven.plugin.failsafe>
                  <version.maven.plugin.checkstyle>3.1.2</version.maven.plugin.checkstyle>
                </properties>

                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-failsafe-plugin</artifactId>
                      <version>${version.maven.plugin.failsafe}</version>
                    </plugin>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-checkstyle-plugin</artifactId>
                      <version>${version.maven.plugin.checkstyle}</version>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """,
            spec -> spec.after(actual ->
              assertThat(actual)
                .containsPattern("<version.maven.plugin.failsafe>3.1.\\d+</version.maven.plugin.failsafe>")
                .containsPattern("<version.maven.plugin.checkstyle>3.6.\\d+</version.maven.plugin.checkstyle>")
                .actual()
              )
          )
        );
    }

    @Test
    void shouldAddVersionInOrder() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion(
            "org.apache.maven.plugins",
            "maven-compiler-plugin",
            "3.11.0",
            null,
            null,
            true
          )),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-compiler-plugin</artifactId>
                      <configuration>
                        <source>1.8</source>
                        <target>1.8</target>
                      </configuration>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-compiler-plugin</artifactId>
                      <version>3.11.0</version>
                      <configuration>
                        <source>1.8</source>
                        <target>1.8</target>
                      </configuration>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """
          )
        );
    }

    @Test
    void defaultPluginGroupId() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion(
            "org.apache.maven.plugins",
            "maven-compiler-plugin",
            "3.11.0",
            null,
            null,
            false
          )),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <build>
                  <plugins>
                    <plugin>
                      <artifactId>maven-compiler-plugin</artifactId>
                      <version>3.10.0</version>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <build>
                  <plugins>
                    <plugin>
                      <artifactId>maven-compiler-plugin</artifactId>
                      <version>3.11.0</version>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/992")
    @Test
    void shouldNotAddVersionWhenManagedByParent() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion(
            "org.apache.maven.plugins",
            "maven-compiler-plugin",
            "3.11.0",
            null,
            null,
            true
          )),
          pomXml(
            """
              <project>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app-parent</artifactId>
                <version>1</version>
                <packaging>pom</packaging>
                <build>
                  <pluginManagement>
                    <plugins>
                      <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-compiler-plugin</artifactId>
                        <version>3.8.1</version>
                      </plugin>
                    </plugins>
                  </pluginManagement>
                </build>
              </project>
              """,
            """
              <project>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app-parent</artifactId>
                <version>1</version>
                <packaging>pom</packaging>
                <build>
                  <pluginManagement>
                    <plugins>
                      <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-compiler-plugin</artifactId>
                        <version>3.11.0</version>
                      </plugin>
                    </plugins>
                  </pluginManagement>
                </build>
              </project>
              """
          ),
          mavenProject("child",
            pomXml(
              """
                <project>
                  <parent>
                    <groupId>org.openrewrite.example</groupId>
                    <artifactId>my-app-parent</artifactId>
                    <version>1</version>
                  </parent>
                  <artifactId>my-app-child</artifactId>
                  <build>
                    <plugins>
                      <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-compiler-plugin</artifactId>
                        <configuration>
                          <source>1.8</source>
                          <target>1.8</target>
                        </configuration>
                      </plugin>
                    </plugins>
                  </build>
                </project>
                """
              // No changes expected — version is managed by parent
            )
          )
        );
    }
}
