/*
 * Copyright 2021 the original author or authors.
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

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class UpgradeDependencyVersionTest implements RewriteTest {

    @DocumentExample
    @Test
    void doNotOverrideImplicitProperty() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("io.dropwizard.metrics", "metrics-annotation", "4.2.9", null,
            true, null)),
          pomXml(
            """
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>explicit-deps-app</artifactId>
                  <version>0.0.1-SNAPSHOT</version>
                  <name>explicit-deps-app</name>
                  <description>explicit-deps-app</description>
                  <properties>
                      <java.version>17</java.version>
                      <maven.compiler.source>17</maven.compiler.source>
                      <maven.compiler.target>17</maven.compiler.target>
                  </properties>
                  <repositories>
                      <repository>
                          <id>spring-milestone</id>
                          <url>https://repo.spring.io/milestone</url>
                          <snapshots>
                              <enabled>false</enabled>
                          </snapshots>
                      </repository>
                  </repositories>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>org.springframework.boot</groupId>
                              <artifactId>spring-boot-dependencies</artifactId>
                              <version>2.4.0</version>
                              <type>pom</type>
                              <scope>import</scope>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>io.dropwizard.metrics</groupId>
                          <artifactId>metrics-annotation</artifactId>
                      </dependency>
                  </dependencies>
              </project>
              """,
            """
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>explicit-deps-app</artifactId>
                  <version>0.0.1-SNAPSHOT</version>
                  <name>explicit-deps-app</name>
                  <description>explicit-deps-app</description>
                  <properties>
                      <java.version>17</java.version>
                      <maven.compiler.source>17</maven.compiler.source>
                      <maven.compiler.target>17</maven.compiler.target>
                  </properties>
                  <repositories>
                      <repository>
                          <id>spring-milestone</id>
                          <url>https://repo.spring.io/milestone</url>
                          <snapshots>
                              <enabled>false</enabled>
                          </snapshots>
                      </repository>
                  </repositories>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>org.springframework.boot</groupId>
                              <artifactId>spring-boot-dependencies</artifactId>
                              <version>2.4.0</version>
                              <type>pom</type>
                              <scope>import</scope>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>io.dropwizard.metrics</groupId>
                          <artifactId>metrics-annotation</artifactId>
                          <version>4.2.9</version>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void updateManagedDependencyVersion() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("org.junit.jupiter", "junit-jupiter-api", "5.7.2", null,
            null, null)),
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
                              <artifactId>junit-jupiter-api</artifactId>
                              <version>5.6.2</version>
                              <scope>test</scope>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
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
                              <groupId>org.junit.jupiter</groupId>
                              <artifactId>junit-jupiter-api</artifactId>
                              <version>5.7.2</version>
                              <scope>test</scope>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void forceUpgradeNonSemverVersion() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("org.springframework.cloud", "spring-cloud-dependencies", "2022.0.2", null,
            false, null)),
          pomXml(
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>org.springframework.cloud</groupId>
                              <artifactId>spring-cloud-dependencies</artifactId>
                              <version>Camden.SR5</version>
                              <type>pom</type>
                              <scope>import</scope>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
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
                              <groupId>org.springframework.cloud</groupId>
                              <artifactId>spring-cloud-dependencies</artifactId>
                              <version>2022.0.2</version>
                              <type>pom</type>
                              <scope>import</scope>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }

    @ParameterizedTest
    @CsvSource(value = {"com.google.guava:guava", "*:*"}, delimiter = ':')
    void upgradeVersion(String groupId, String artifactId) {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion(groupId, artifactId, "latest.patch", null, null, null)),
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
                    <version>13.0</version>
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
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>13.0.1</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void overrideManagedDependency() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.google.guava", "guava", "14.0", "", true, null)),
          pomXml(
            """
              <project>
                  <groupId>com.mycompany</groupId>
                  <artifactId>my-parent</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>com.google.guava</groupId>
                              <artifactId>guava</artifactId>
                              <version>13.0</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """,
            SourceSpec::skip
          ),
          mavenProject("my-child",
            pomXml(
              """
                <project>
                    <parent>
                        <groupId>com.mycompany</groupId>
                        <artifactId>my-parent</artifactId>
                        <version>1</version>
                    </parent>
                    <groupId>com.mycompany</groupId>
                    <artifactId>my-child</artifactId>
                    <version>1</version>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """,
              """
                    <project>
                        <parent>
                            <groupId>com.mycompany</groupId>
                            <artifactId>my-parent</artifactId>
                            <version>1</version>
                        </parent>
                        <groupId>com.mycompany</groupId>
                        <artifactId>my-child</artifactId>
                        <version>1</version>
                        <dependencies>
                            <dependency>
                                <groupId>com.google.guava</groupId>
                                <artifactId>guava</artifactId>
                                <version>14.0</version>
                            </dependency>
                        </dependencies>
                    </project>
                """
            )
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/739")
    void upgradeVersionWithGroupIdAndArtifactIdDefinedAsProperty() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("io.quarkus", "quarkus-universe-bom", "1.13.7.Final", null,
            null, null)),
          pomXml(
            """
              <project>
                  <groupId>org.openrewrite.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <properties>
                      <quarkus.platform.artifact-id>quarkus-universe-bom</quarkus.platform.artifact-id>
                      <quarkus.platform.group-id>io.quarkus</quarkus.platform.group-id>
                      <quarkus.platform.version>1.11.7.Final</quarkus.platform.version>
                  </properties>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>${quarkus.platform.group-id}</groupId>
                              <artifactId>${quarkus.platform.artifact-id}</artifactId>
                              <version>${quarkus.platform.version}</version>
                              <type>pom</type>
                              <scope>import</scope>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """,
            """
              <project>
                  <groupId>org.openrewrite.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <properties>
                      <quarkus.platform.artifact-id>quarkus-universe-bom</quarkus.platform.artifact-id>
                      <quarkus.platform.group-id>io.quarkus</quarkus.platform.group-id>
                      <quarkus.platform.version>1.13.7.Final</quarkus.platform.version>
                  </properties>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>${quarkus.platform.group-id}</groupId>
                              <artifactId>${quarkus.platform.artifact-id}</artifactId>
                              <version>${quarkus.platform.version}</version>
                              <type>pom</type>
                              <scope>import</scope>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void upgradeVersionSuccessively() {
        rewriteRun(
          spec -> spec.recipes(
            new UpgradeDependencyVersion("com.google.guava", "*", "28.x", "-jre", null, null),
            new UpgradeDependencyVersion("com.google.guava", "*", "29.x", "-jre", null, null)
          ),
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
                    <version>27.0-jre</version>
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
                  <dependency>
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
    @Issue("https://github.com/openrewrite/rewrite/issues/565")
    void propertiesInDependencyGroupIdAndArtifactId() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.google.guava", "*", "latest.patch", null, null, null)),
          pomXml(
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <properties>
                      <dependency.group-id>com.google.guava</dependency.group-id>
                      <dependency.artifact-id>guava</dependency.artifact-id>
                      <dependency.version>13.0</dependency.version>
                  </properties>
                  <dependencies>
                      <dependency>
                          <groupId>${dependency.group-id}</groupId>
                          <artifactId>${dependency.artifact-id}</artifactId>
                          <version>${dependency.version}</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <properties>
                      <dependency.group-id>com.google.guava</dependency.group-id>
                      <dependency.artifact-id>guava</dependency.artifact-id>
                      <dependency.version>13.0.1</dependency.version>
                  </properties>
                  <dependencies>
                      <dependency>
                          <groupId>${dependency.group-id}</groupId>
                          <artifactId>${dependency.artifact-id}</artifactId>
                          <version>${dependency.version}</version>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void upgradeGuava() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.google.guava", "*", "25-28", "-android", null, null)),
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
                    <version>25.0-android</version>
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
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>28.0-android</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void upgradePluginDependencies() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("org.openrewrite.recipe", "rewrite-spring", "5.0.6", "", null, null)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.openrewrite.maven</groupId>
                      <artifactId>rewrite-maven-plugin</artifactId>
                      <version>5.4.1</version>
                      <dependencies>
                        <dependency>
                          <groupId>org.openrewrite.recipe</groupId>
                          <artifactId>rewrite-spring</artifactId>
                          <version>5.0.5</version>
                        </dependency>
                      </dependencies>
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
                      <groupId>org.openrewrite.maven</groupId>
                      <artifactId>rewrite-maven-plugin</artifactId>
                      <version>5.4.1</version>
                      <dependencies>
                        <dependency>
                          <groupId>org.openrewrite.recipe</groupId>
                          <artifactId>rewrite-spring</artifactId>
                          <version>5.0.6</version>
                        </dependency>
                      </dependencies>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """
          )
        );
    }

    @Test
    void upgradePluginDependenciesOnProperty() {
        rewriteRun(
          // Using latest.patch to validate the version property resolution, since it's needed for matching the valid patch.
          spec -> spec.recipe(new UpgradeDependencyVersion("org.openrewrite.recipe", "rewrite-spring", "latest.patch", "", null, null)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                
                <properties>
                    <rewrite-spring.version>4.33.0</rewrite-spring.version>
                </properties>
                
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.openrewrite.maven</groupId>
                      <artifactId>rewrite-maven-plugin</artifactId>
                      <version>5.4.1</version>
                      <dependencies>
                        <dependency>
                          <groupId>org.openrewrite.recipe</groupId>
                          <artifactId>rewrite-spring</artifactId>
                          <version>${rewrite-spring.version}</version>
                        </dependency>
                      </dependencies>
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
                
                <properties>
                    <rewrite-spring.version>4.33.2</rewrite-spring.version>
                </properties>
                
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.openrewrite.maven</groupId>
                      <artifactId>rewrite-maven-plugin</artifactId>
                      <version>5.4.1</version>
                      <dependencies>
                        <dependency>
                          <groupId>org.openrewrite.recipe</groupId>
                          <artifactId>rewrite-spring</artifactId>
                          <version>${rewrite-spring.version}</version>
                        </dependency>
                      </dependencies>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1334")
    void upgradeGuavaWithExplicitBlankVersionPattern() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.google.guava", "*", "latest.release", "", null, null)),
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
                    <version>22.0</version>
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
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>23.0</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void upgradeManagedInParent() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.google.guava", "*", "25-28", "-jre", false, null)),
          pomXml(
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <properties>
                      <guava.version>25.0-jre</guava.version>
                  </properties>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>com.google.guava</groupId>
                              <artifactId>guava</artifactId>
                              <version>${guava.version}</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """,
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <properties>
                      <guava.version>28.0-jre</guava.version>
                  </properties>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>com.google.guava</groupId>
                              <artifactId>guava</artifactId>
                              <version>${guava.version}</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          ),
          mavenProject("my-app-server",
            pomXml(
              """
                    <project>
                        <parent>
                            <groupId>com.mycompany.app</groupId>
                            <artifactId>my-app</artifactId>
                            <version>1</version>
                        </parent>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app-server</artifactId>
                        <version>1</version>
                        <dependencies>
                            <dependency>
                                <groupId>com.google.guava</groupId>
                                <artifactId>guava</artifactId>
                            </dependency>
                        </dependencies>
                    </project>
                """
            )
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/891")
    void upgradeDependencyOnlyTargetsSpecificDependencyProperty() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("com.google.guava", "*", "25-28", "-jre", null, null)),
          pomXml(
            """
              <project>
                  <groupId>org.openrewrite.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <properties>
                      <guava.version>25.0-jre</guava.version>
                      <spring.version>5.3.9</spring.version>
                      <spring.artifact-id>spring-jdbc</spring.artifact-id>
                  </properties>
                  <dependencies>
                      <dependency>
                          <groupId>com.google.guava</groupId>
                          <artifactId>guava</artifactId>
                          <version>${guava.version}</version>
                      </dependency>
                      <dependency>
                          <groupId>org.springframework</groupId>
                          <artifactId>${spring.artifact-id}</artifactId>
                          <version>${spring.version}</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            """
              <project>
                  <groupId>org.openrewrite.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <properties>
                      <guava.version>28.0-jre</guava.version>
                      <spring.version>5.3.9</spring.version>
                      <spring.artifact-id>spring-jdbc</spring.artifact-id>
                  </properties>
                  <dependencies>
                      <dependency>
                          <groupId>com.google.guava</groupId>
                          <artifactId>guava</artifactId>
                          <version>${guava.version}</version>
                      </dependency>
                      <dependency>
                          <groupId>org.springframework</groupId>
                          <artifactId>${spring.artifact-id}</artifactId>
                          <version>${spring.version}</version>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void upgradeBomImport() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("io.micronaut", "micronaut-bom", "3.0.0-M5", null, null, null)),
          pomXml(
            """
              <project>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app-server</artifactId>
                <version>1</version>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>io.micronaut</groupId>
                      <artifactId>micronaut-bom</artifactId>
                      <version>2.5.10</version>
                      <type>pom</type>
                      <scope>import</scope>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """,
            """
              <project>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app-server</artifactId>
                <version>1</version>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>io.micronaut</groupId>
                      <artifactId>micronaut-bom</artifactId>
                      <version>3.0.0-M5</version>
                      <type>pom</type>
                      <scope>import</scope>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void upgradeAllManagedDependenciesToPatchReleases() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("*", "*", "latest.patch", null, null, null)),
          pomXml(
            """
              <project>
                  <groupId>org.openrewrite.example</groupId>
                  <artifactId>my-app-server</artifactId>
                  <version>1</version>
                  <properties>
                      <micronaut.version>2.5.10</micronaut.version>
                  </properties>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>io.micronaut</groupId>
                              <artifactId>micronaut-bom</artifactId>
                              <version>${micronaut.version}</version>
                              <type>pom</type>
                              <scope>import</scope>
                          </dependency>
                          <dependency>
                              <groupId>javax.servlet</groupId>
                              <artifactId>javax.servlet-api</artifactId>
                              <version>4.0.0</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """,
            """
              <project>
                  <groupId>org.openrewrite.example</groupId>
                  <artifactId>my-app-server</artifactId>
                  <version>1</version>
                  <properties>
                      <micronaut.version>2.5.13</micronaut.version>
                  </properties>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>io.micronaut</groupId>
                              <artifactId>micronaut-bom</artifactId>
                              <version>${micronaut.version}</version>
                              <type>pom</type>
                              <scope>import</scope>
                          </dependency>
                          <dependency>
                              <groupId>javax.servlet</groupId>
                              <artifactId>javax.servlet-api</artifactId>
                              <version>4.0.1</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void upgradeAllDependenciesToPatchReleases() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("*", "*", "latest.patch", null, null, null)),
          pomXml(
            """
              <project>
                  <groupId>org.openrewrite.example</groupId>
                  <artifactId>my-app-server</artifactId>
                  <version>1</version>
                  <properties>
                      <guava.version>28.0-jre</guava.version>
                      <spring.version>5.3.4</spring.version>
                      <spring.artifact-id>spring-jdbc</spring.artifact-id>
                  </properties>
                  <dependencies>
                      <dependency>
                          <groupId>com.google.guava</groupId>
                          <artifactId>guava</artifactId>
                          <version>${guava.version}</version>
                      </dependency>
                      <dependency>
                          <groupId>org.springframework</groupId>
                          <artifactId>${spring.artifact-id}</artifactId>
                          <version>${spring.version}</version>
                      </dependency>
                      <dependency>
                          <groupId>com.fasterxml.jackson.core</groupId>
                          <artifactId>jackson-core</artifactId>
                          <version>2.13.1</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.after(after -> {
                Matcher matcher = Pattern.compile("<spring\\.version>(.+)</spring\\.version>").matcher(after);
                assertTrue(matcher.find());
                String springVersion = matcher.group(1);
                assertNotEquals("5.3.4", springVersion);
                return """
                  <project>
                      <groupId>org.openrewrite.example</groupId>
                      <artifactId>my-app-server</artifactId>
                      <version>1</version>
                      <properties>
                          <guava.version>28.0-jre</guava.version>
                          <spring.version>%s</spring.version>
                          <spring.artifact-id>spring-jdbc</spring.artifact-id>
                      </properties>
                      <dependencies>
                          <dependency>
                              <groupId>com.google.guava</groupId>
                              <artifactId>guava</artifactId>
                              <version>${guava.version}</version>
                          </dependency>
                          <dependency>
                              <groupId>org.springframework</groupId>
                              <artifactId>${spring.artifact-id}</artifactId>
                              <version>${spring.version}</version>
                          </dependency>
                          <dependency>
                              <groupId>com.fasterxml.jackson.core</groupId>
                              <artifactId>jackson-core</artifactId>
                              <version>2.13.5</version>
                          </dependency>
                      </dependencies>
                  </project>
                  """.formatted(springVersion);
            })
          )
        );
    }

    @Test
    void dependencyManagementResolvedFromProperty() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("io.micronaut", "micronaut-bom", "3.0.0-M5", null, null, null)),
          pomXml(
            """
              <project>
                <groupId>org.openrewrite</groupId>
                <artifactId>my-app-server</artifactId>
                <version>1.0.0</version>
                <properties>
                  <micronaut.version>2.5.11</micronaut.version>
                </properties>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>io.micronaut</groupId>
                      <artifactId>micronaut-bom</artifactId>
                      <version>${micronaut.version}</version>
                      <type>pom</type>
                      <scope>import</scope>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """,
            """
              <project>
                <groupId>org.openrewrite</groupId>
                <artifactId>my-app-server</artifactId>
                <version>1.0.0</version>
                <properties>
                  <micronaut.version>3.0.0-M5</micronaut.version>
                </properties>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>io.micronaut</groupId>
                      <artifactId>micronaut-bom</artifactId>
                      <version>${micronaut.version}</version>
                      <type>pom</type>
                      <scope>import</scope>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void upgradeToExactVersion() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("org.thymeleaf", "thymeleaf-spring5", "3.0.12.RELEASE", null,
            null, null)),
          pomXml(
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencies>
                      <dependency>
                          <groupId>org.thymeleaf</groupId>
                          <artifactId>thymeleaf-spring5</artifactId>
                          <version>3.0.8.RELEASE</version>
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
                      <dependency>
                          <groupId>org.thymeleaf</groupId>
                          <artifactId>thymeleaf-spring5</artifactId>
                          <version>3.0.12.RELEASE</version>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1248")
    @Test
    void updateWithExactVersionRange() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("junit", "junit", "4.13", null, false, null)),
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
              """,
            """            
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.13</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void deriveFromNexus() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("*", "*", "latest.patch", null, null, null)),
          pomXml(
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>net.sourceforge.saxon</groupId>
                              <artifactId>saxon</artifactId>
                              <version>9.1.0.8</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void deriveFromNexusUpgrade() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("*", "*", "latest.patch", null, null, null)),
          pomXml(
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>net.sourceforge.orbroker</groupId>
                              <artifactId>orbroker</artifactId>
                              <version>2.0.3</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
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
                              <groupId>net.sourceforge.orbroker</groupId>
                              <artifactId>orbroker</artifactId>
                              <version>2.0.4</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void noManagedVersion() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("*", "*", "latest.patch", null, null, null)),
          pomXml(
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>net.sourceforge.orbroker</groupId>
                              <artifactId>orbroker</artifactId>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void removesRedundantExplicitVersionsMatchingOldImport() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("org.junit", "junit-bom", "5.9.1", null, true, null)),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.junit</groupId>
                      <artifactId>junit-bom</artifactId>
                      <version>5.9.0</version>
                      <type>pom</type>
                      <scope>import</scope>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
                
                <dependencies>
                  <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter-api</artifactId>
                    <version>5.9.0</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.junit</groupId>
                      <artifactId>junit-bom</artifactId>
                      <version>5.9.1</version>
                      <type>pom</type>
                      <scope>import</scope>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
                
                <dependencies>
                  <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter-api</artifactId>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void removesRedundantExplicitVersionsMatchingNewImport() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("org.junit", "junit-bom", "5.9.1", null, true, null)),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.junit</groupId>
                      <artifactId>junit-bom</artifactId>
                      <version>5.9.0</version>
                      <type>pom</type>
                      <scope>import</scope>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
                
                <dependencies>
                  <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter-api</artifactId>
                    <version>5.9.1</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.junit</groupId>
                      <artifactId>junit-bom</artifactId>
                      <version>5.9.1</version>
                      <type>pom</type>
                      <scope>import</scope>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
                
                <dependencies>
                  <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter-api</artifactId>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void keepsRedundantExplicitVersionsNotMatchingOldOrNewImport() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("org.junit", "junit-bom", "5.9.1", null, true, null)),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.junit</groupId>
                      <artifactId>junit-bom</artifactId>
                      <version>5.9.0</version>
                      <type>pom</type>
                      <scope>import</scope>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
                
                <dependencies>
                  <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter-api</artifactId>
                    <version>5.8.0</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.junit</groupId>
                      <artifactId>junit-bom</artifactId>
                      <version>5.9.1</version>
                      <type>pom</type>
                      <scope>import</scope>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
                
                <dependencies>
                  <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter-api</artifactId>
                    <version>5.8.0</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Nested
    @Issue("https://github.com/openrewrite/rewrite/issues/2418")
    class RetainVersions {
        @DocumentExample
        @Test
        void dependencyWithExplicitVersionRemovedFromDepMgmt() {
            rewriteRun(spec -> spec.recipe(new UpgradeDependencyVersion("org.springframework.cloud", "spring-cloud-config-dependencies", "3.1.4", null, true, Collections.singletonList("com.jcraft:jsch"))),
              pomXml(
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    
                    <dependencyManagement>
                      <dependencies>
                        <dependency>
                          <groupId>org.springframework.cloud</groupId>
                          <artifactId>spring-cloud-config-dependencies</artifactId>
                          <version>3.1.2</version>
                          <type>pom</type>
                          <scope>import</scope>
                        </dependency>
                      </dependencies>
                    </dependencyManagement>
                    
                    <dependencies>
                      <dependency>
                        <groupId>com.jcraft</groupId>
                        <artifactId>jsch</artifactId>
                        <version>0.1.55</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """,
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    
                    <dependencyManagement>
                      <dependencies>
                        <dependency>
                          <groupId>org.springframework.cloud</groupId>
                          <artifactId>spring-cloud-config-dependencies</artifactId>
                          <version>3.1.4</version>
                          <type>pom</type>
                          <scope>import</scope>
                        </dependency>
                      </dependencies>
                    </dependencyManagement>
                    
                    <dependencies>
                      <dependency>
                        <groupId>com.jcraft</groupId>
                        <artifactId>jsch</artifactId>
                        <version>0.1.55</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """
              )
            );
        }

        @Test
        void dependencyWithoutExplicitVersionRemovedFromDepMgmt() {
            rewriteRun(spec -> spec.recipe(new UpgradeDependencyVersion("org.springframework.cloud", "spring-cloud-config-dependencies", "3.1.4", null, true, Collections.singletonList("com.jcraft:jsch"))),
              pomXml(
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    
                    <dependencyManagement>
                      <dependencies>
                        <dependency>
                          <groupId>org.springframework.cloud</groupId>
                          <artifactId>spring-cloud-config-dependencies</artifactId>
                          <version>3.1.2</version>
                          <type>pom</type>
                          <scope>import</scope>
                        </dependency>
                      </dependencies>
                    </dependencyManagement>
                    
                    <dependencies>
                      <dependency>
                        <groupId>com.jcraft</groupId>
                        <artifactId>jsch</artifactId>
                      </dependency>
                    </dependencies>
                  </project>
                  """,
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    
                    <dependencyManagement>
                      <dependencies>
                        <dependency>
                          <groupId>org.springframework.cloud</groupId>
                          <artifactId>spring-cloud-config-dependencies</artifactId>
                          <version>3.1.4</version>
                          <type>pom</type>
                          <scope>import</scope>
                        </dependency>
                      </dependencies>
                    </dependencyManagement>
                    
                    <dependencies>
                      <dependency>
                        <groupId>com.jcraft</groupId>
                        <artifactId>jsch</artifactId>
                        <version>0.1.55</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """
              )
            );
        }

        @Test
        void dependencyWithoutExplicitVersionRemovedFromDepMgmtRetainSpecificVersion() {
            rewriteRun(spec -> spec.recipe(new UpgradeDependencyVersion("org.springframework.cloud", "spring-cloud-config-dependencies", "3.1.4", null, true, Collections.singletonList("com.jcraft:jsch:0.1.50"))),
              pomXml(
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    
                    <dependencyManagement>
                      <dependencies>
                        <dependency>
                          <groupId>org.springframework.cloud</groupId>
                          <artifactId>spring-cloud-config-dependencies</artifactId>
                          <version>3.1.2</version>
                          <type>pom</type>
                          <scope>import</scope>
                        </dependency>
                      </dependencies>
                    </dependencyManagement>
                    
                    <dependencies>
                      <dependency>
                        <groupId>com.jcraft</groupId>
                        <artifactId>jsch</artifactId>
                      </dependency>
                    </dependencies>
                  </project>
                  """,
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    
                    <dependencyManagement>
                      <dependencies>
                        <dependency>
                          <groupId>org.springframework.cloud</groupId>
                          <artifactId>spring-cloud-config-dependencies</artifactId>
                          <version>3.1.4</version>
                          <type>pom</type>
                          <scope>import</scope>
                        </dependency>
                      </dependencies>
                    </dependencyManagement>
                    
                    <dependencies>
                      <dependency>
                        <groupId>com.jcraft</groupId>
                        <artifactId>jsch</artifactId>
                        <version>0.1.50</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """
              )
            );
        }

        @Test
        void multipleRetainVersions() {
            rewriteRun(spec -> spec.recipe(
                new UpgradeDependencyVersion("org.springframework.cloud", "spring-cloud-dependencies", "2021.0.5", null,
                  true,
                  Lists.newArrayList("com.jcraft:jsch", "org.springframework.cloud:spring-cloud-schema-registry-*:1.1.1"))),
              pomXml(
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    
                    <dependencyManagement>
                      <dependencies>
                        <dependency>
                          <groupId>org.springframework.cloud</groupId>
                          <artifactId>spring-cloud-dependencies</artifactId>
                          <version>2020.0.1</version>
                          <type>pom</type>
                          <scope>import</scope>
                        </dependency>
                      </dependencies>
                    </dependencyManagement>
                    
                    <dependencies>
                      <dependency>
                        <groupId>com.jcraft</groupId>
                        <artifactId>jsch</artifactId>
                      </dependency>
                      <dependency>
                        <groupId>org.springframework.cloud</groupId>
                        <artifactId>spring-cloud-schema-registry-core</artifactId>
                      </dependency>
                    </dependencies>
                  </project>
                  """,
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    
                    <dependencyManagement>
                      <dependencies>
                        <dependency>
                          <groupId>org.springframework.cloud</groupId>
                          <artifactId>spring-cloud-dependencies</artifactId>
                          <version>2021.0.5</version>
                          <type>pom</type>
                          <scope>import</scope>
                        </dependency>
                      </dependencies>
                    </dependencyManagement>
                    
                    <dependencies>
                      <dependency>
                        <groupId>com.jcraft</groupId>
                        <artifactId>jsch</artifactId>
                        <version>0.1.55</version>
                      </dependency>
                      <dependency>
                        <groupId>org.springframework.cloud</groupId>
                        <artifactId>spring-cloud-schema-registry-core</artifactId>
                        <version>1.1.1</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """
              )
            );
        }
    }
}
