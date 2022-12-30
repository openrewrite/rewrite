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
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class UpgradePluginVersionTest implements RewriteTest {

    @Test
    void upgradeToExactVersion() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion(
            "io.quarkus",
            "quarkus-maven-plugin",
            "1.13.5.Final",
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

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/565")
    void handlesPropertyResolution() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion(
            "io.quarkus",
            "quarkus-maven-plugin",
            "1.13.5.Final",
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

    @Test
    @Issue("Should be changed/removed when this recipe supports dynamic version resolution")
    void ignorePluginWithoutExplicitVersionDeclared() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion(
            "io.quarkus",
            "quarkus-maven-plugin",
            "1.13.5.Final",
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

    @Test
    @Disabled
    void trustParent() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion(
            "org.openrewrite.maven",
            "rewrite-maven-plugin",
            "4.2.x",
            null,
            true
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

    @Test
    @Disabled
    void upgradePluginInParent() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion(
            "org.openrewrite.maven",
            "rewrite-maven-plugin",
            "4.2.3",
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
}
