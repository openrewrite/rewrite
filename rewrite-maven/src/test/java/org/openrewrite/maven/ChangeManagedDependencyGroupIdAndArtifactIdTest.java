/*
 * Copyright 2022 the original author or authors.
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
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class ChangeManagedDependencyGroupIdAndArtifactIdTest implements RewriteTest {

    @DocumentExample
    @Test
    void changeManagedDependencyGroupIdAndArtifactId() {
        rewriteRun(
          spec -> spec.recipe(new ChangeManagedDependencyGroupIdAndArtifactId(
            "javax.activation",
            "javax.activation-api",
            "jakarta.activation",
            "jakarta.activation-api",
            "2.1.0"
          )),
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
                              <groupId>javax.activation</groupId>
                              <artifactId>javax.activation-api</artifactId>
                              <version>1.2.0</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
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
                              <groupId>jakarta.activation</groupId>
                              <artifactId>jakarta.activation-api</artifactId>
                              <version>2.1.0</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-java-dependencies/issues/55")
    @Test
    void requireNewGroupIdOrNewArtifactIdToBeDifferentFromBefore() {
        assertThatExceptionOfType(AssertionError.class)
          .isThrownBy(() -> rewriteRun(
            spec -> spec.recipe(new ChangeManagedDependencyGroupIdAndArtifactId(
              "javax.activation",
              "javax.activation-api",
              "javax.activation",
              "javax.activation-api",
              null
            ))
          )).withMessageContaining("newGroupId OR newArtifactId must be different from before");
    }

    @Test
    void changeManagedDependencyWithDynamicVersion() {
        rewriteRun(
          spec -> spec.recipe(new ChangeManagedDependencyGroupIdAndArtifactId(
            "javax.activation",
            "javax.activation-api",
            "jakarta.activation",
            "jakarta.activation-api",
            "2.1.x"
          )),
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
                              <groupId>javax.activation</groupId>
                              <artifactId>javax.activation-api</artifactId>
                              <version>1.2.0</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """,
            spec -> spec.after(pom -> {
                return assertThat(pom).containsPattern("<version>2.1.(\\d+)</version>").actual();
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4514")
    @Test
    void removeOldDependencyIfNewAlreadyExists() {
        rewriteRun(
          spec -> spec.recipe(new ChangeManagedDependencyGroupIdAndArtifactId(
            "javax.activation",
            "javax.activation-api",
            "jakarta.activation",
            "jakarta.activation-api",
            "2.1.0"
          )),
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
                              <groupId>javax.activation</groupId>
                              <artifactId>javax.activation-api</artifactId>
                              <version>1.2.0</version>
                          </dependency>
                          <dependency>
                              <groupId>jakarta.activation</groupId>
                              <artifactId>jakarta.activation-api</artifactId>
                              <version>2.1.0</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
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
                              <groupId>jakarta.activation</groupId>
                              <artifactId>jakarta.activation-api</artifactId>
                              <version>2.1.0</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4514")
    @Test
    void removeOldDependencyVersionWithPattern() {
        rewriteRun(
          spec -> spec.recipe(new ChangeManagedDependencyGroupIdAndArtifactId(
            "jakarta.activation",
            "jakarta.activation-api",
            "com.google.guava",
            "guava",
            "32.0.X",
            "-jre"
          )),
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
                             <groupId>com.google.guava</groupId>
                             <artifactId>guava</artifactId>
                             <version>32.0.1-jre</version>
                          </dependency>
                          <dependency>
                              <groupId>jakarta.activation</groupId>
                              <artifactId>jakarta.activation-api</artifactId>
                              <version>2.1.0</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>""",
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                             <groupId>com.google.guava</groupId>
                             <artifactId>guava</artifactId>
                             <version>32.0.1-jre</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>"""
          )
        );
    }

    @Test
    void changeManagedDependencyWithPropertyName() {
        rewriteRun(
          spec -> spec.recipe(new ChangeManagedDependencyGroupIdAndArtifactId(
            "javax.activation",
            "javax.activation-api",
            "jakarta.activation",
            "jakarta.acti${partial.arti}-api",
            "2.1.0"
          )),
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <properties>
                    <partial.arti>vation</partial.arti>
                  </properties>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>javax.activation</groupId>
                              <artifactId>javax.activation-api</artifactId>
                              <version>1.2.0</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """,
              """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <properties>
                    <partial.arti>vation</partial.arti>
                  </properties>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>jakarta.activation</groupId>
                              <artifactId>jakarta.acti${partial.arti}-api</artifactId>
                              <version>2.1.0</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void changeManagedDependencyWithPropertyVersionUpdatesProperty() {
        rewriteRun(
          spec -> spec.recipe(new ChangeManagedDependencyGroupIdAndArtifactId(
            "javax.activation",
            "javax.activation-api",
            "jakarta.activation",
            "jakarta.activation-api",
            "latest.patch"
          )),
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <properties>
                      <version.property>1.2.0</version.property>
                  </properties>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>javax.activation</groupId>
                              <artifactId>javax.activation-api</artifactId>
                              <version>${version.property}</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """,
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <properties>
                      <version.property>1.2.2</version.property>
                  </properties>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>jakarta.activation</groupId>
                              <artifactId>jakarta.activation-api</artifactId>
                              <version>${version.property}</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void changeManagedDependencyWithPropertyVersionAlreadyInUseLeavesPropertyAndInlines() {
        rewriteRun(
          spec -> spec.recipe(new ChangeManagedDependencyGroupIdAndArtifactId(
            "javax.activation",
            "javax.activation-api",
            "jakarta.activation",
            "jakarta.activation-api",
            "latest.patch"
          )),
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <properties>
                      <version.property>1.2.0</version.property>
                  </properties>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>javax.activation</groupId>
                              <artifactId>javax.activation-api</artifactId>
                              <version>${version.property}</version>
                          </dependency>
                          <dependency>
                              <groupId>org.openrewrite.recipe</groupId>
                              <artifactId>rewrite-recipe-bom</artifactId>
                              <version>${version.property}</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """,
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <properties>
                      <version.property>1.2.0</version.property>
                  </properties>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>jakarta.activation</groupId>
                              <artifactId>jakarta.activation-api</artifactId>
                              <version>1.2.2</version>
                          </dependency>
                          <dependency>
                              <groupId>org.openrewrite.recipe</groupId>
                              <artifactId>rewrite-recipe-bom</artifactId>
                              <version>${version.property}</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void doesNotIntroducePropertyForVersion() {
        rewriteRun(
          spec -> spec.recipe(
            new ChangeManagedDependencyGroupIdAndArtifactId(
              "com.fasterxml.jackson.core",
              "jackson-core",
              "org.apache.commons",
              "commons-csv",
              "1.14.1",
              null
            )
          ),
          //language=xml
          pomXml(
            """
              <project>
                <groupId>com.example</groupId>
                <artifactId>project</artifactId>
                <version>1</version>
                <parent>
                  <groupId>com.fasterxml.jackson</groupId>
                  <artifactId>jackson-bom</artifactId>
                  <version>2.20.0</version>
                </parent>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.fasterxml.jackson.core</groupId>
                      <artifactId>jackson-core</artifactId>
                      <version>${jackson.version.core}</version>
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
            """
              <project>
                <groupId>com.example</groupId>
                <artifactId>project</artifactId>
                <version>1</version>
                <parent>
                  <groupId>com.fasterxml.jackson</groupId>
                  <artifactId>jackson-bom</artifactId>
                  <version>2.20.0</version>
                </parent>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.apache.commons</groupId>
                      <artifactId>commons-csv</artifactId>
                      <version>1.14.1</version>
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
              """
          )
        );
    }

    @Test
    void latestPatchMangedDependency() {
        rewriteRun(
          spec -> spec.recipe(new ChangeManagedDependencyGroupIdAndArtifactId(
            "javax.activation",
            "javax.activation-api",
            "jakarta.activation",
            "jakarta.activation-api",
            "latest.patch",
            null
          )),
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
                              <groupId>javax.activation</groupId>
                              <artifactId>javax.activation-api</artifactId>
                              <version>1.2.0</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
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
                              <groupId>jakarta.activation</groupId>
                              <artifactId>jakarta.activation-api</artifactId>
                              <version>1.2.2</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void changeProfileManagedDependencyGroupIdAndArtifactId() {
        rewriteRun(
          spec -> spec.recipe(new ChangeManagedDependencyGroupIdAndArtifactId(
            "javax.activation",
            "javax.activation-api",
            "jakarta.activation",
            "jakarta.activation-api",
            "2.1.0"
          )),
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <profiles>
                    <profile>
                      <dependencyManagement>
                          <dependencies>
                              <dependency>
                                  <groupId>javax.activation</groupId>
                                  <artifactId>javax.activation-api</artifactId>
                                  <version>1.2.0</version>
                              </dependency>
                          </dependencies>
                      </dependencyManagement>
                    </profile>
                  </profiles>
              </project>
              """,
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <profiles>
                    <profile>
                      <dependencyManagement>
                          <dependencies>
                              <dependency>
                                  <groupId>jakarta.activation</groupId>
                                  <artifactId>jakarta.activation-api</artifactId>
                                  <version>2.1.0</version>
                              </dependency>
                          </dependencies>
                      </dependencyManagement>
                    </profile>
                  </profiles>
              </project>
              """
          )
        );
    }

    @Test
    void doesNotMakeChangeWhenChangingBomImportManagedDependency() {
        rewriteRun(
          spec -> spec.recipe(new ChangeManagedDependencyGroupIdAndArtifactId(
            "org.springframework.cloud",
            "spring-cloud-starter-sleuth",
            "io.micrometer",
            "micrometer-tracking-bridge-brave",
            "1.0.12",
            null
          )),
          //language=xml
          pomXml(
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>sample</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>org.springframework.cloud</groupId>
                              <artifactId>spring-cloud-dependencies</artifactId>
                              <version>2021.0.0</version>
                              <type>pom</type>
                              <scope>import</scope>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>org.springframework.cloud</groupId>
                          <artifactId>spring-cloud-starter-sleuth</artifactId>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void shouldNotChangeManagedDependencyWithImplicitlyDefinedVersionProperty() {
        rewriteRun(
          spec -> spec.recipe(new ChangeManagedDependencyGroupIdAndArtifactId(
            "org.junit.jupiter", "junit-jupiter-api",
            "org.junit.jupiter", "junit-jupiter-engine",
            "5.x", null)),
          pomXml(
            """
              <project>
                  <groupId>com.mycompany</groupId>
                  <artifactId>my-app</artifactId>
                  <version>5.7.2</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>org.junit.jupiter</groupId>
                              <artifactId>junit-jupiter-api</artifactId>
                              <version>${project.version}</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/1522")
    @Test
    void childModuleSeesParentManagedDependencyChange() {
        // When ChangeManagedDependencyGroupIdAndArtifactId modifies a managed dependency in
        // a parent POM (A), the UpdateMavenModel should correctly propagate those changes
        // to child modules (B) that depend on the managed dependency. The bug is that
        // UpdateMavenModel downloads the parent POM from the repository instead of reusing
        // the already-resolved (modified) parent POM.
        rewriteRun(
          spec -> spec.recipe(new ChangeManagedDependencyGroupIdAndArtifactId(
            "javax.activation",
            "javax.activation-api",
            "jakarta.activation",
            "jakarta.activation-api",
            "2.1.0"
          )),
          pomXml(
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>parent-project</artifactId>
                  <version>1</version>
                  <packaging>pom</packaging>
                  <modules>
                      <module>child</module>
                  </modules>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>javax.activation</groupId>
                              <artifactId>javax.activation-api</artifactId>
                              <version>1.2.0</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """,
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>parent-project</artifactId>
                  <version>1</version>
                  <packaging>pom</packaging>
                  <modules>
                      <module>child</module>
                  </modules>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>jakarta.activation</groupId>
                              <artifactId>jakarta.activation-api</artifactId>
                              <version>2.1.0</version>
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
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>parent-project</artifactId>
                        <version>1</version>
                    </parent>
                    <artifactId>child</artifactId>
                    <dependencies>
                        <dependency>
                            <groupId>javax.activation</groupId>
                            <artifactId>javax.activation-api</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """,
              """
                <project>
                    <parent>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>parent-project</artifactId>
                        <version>1</version>
                    </parent>
                    <artifactId>child</artifactId>
                    <dependencies>
                        <dependency>
                            <groupId>jakarta.activation</groupId>
                            <artifactId>jakarta.activation-api</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """
            )
          )
        );
    }
}
