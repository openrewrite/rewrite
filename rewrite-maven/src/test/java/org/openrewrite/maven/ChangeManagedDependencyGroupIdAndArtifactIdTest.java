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

    @Test
    @Issue("https://github.com/openrewrite/rewrite-java-dependencies/issues/55")
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
                assertThat(pom).containsPattern("<version>2.1.(\\d+)</version>");
                return pom;
            })
          )
        );
    }

    @Test
    void latestPatch() {
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
}
