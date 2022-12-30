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

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class ManageDependenciesTest implements RewriteTest {

    @Test
    void createDependencyManagementWithDependencyWhenNoneExists() {
        rewriteRun(
          spec -> spec.recipe(new ManageDependencies(
            "org.junit.jupiter",
            "*",
            false, false)),
          pomXml(
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencies>
                      <dependency>
                          <groupId>org.junit.jupiter</groupId>
                          <artifactId>junit-jupiter-api</artifactId>
                          <version>5.6.2</version>
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
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>org.junit.jupiter</groupId>
                              <artifactId>junit-jupiter-api</artifactId>
                              <version>5.6.2</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>org.junit.jupiter</groupId>
                          <artifactId>junit-jupiter-api</artifactId>
                          <scope>test</scope>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void deferToDependencyManagementWhenDependencyIsAlreadyManaged() {
        rewriteRun(
          spec -> spec.recipe(new ManageDependencies(
            "junit",
            "junit",
            false, false)),
          pomXml(
            """
              <project>
              <groupId>com.othercompany.app</groupId>
              <artifactId>my-parent-app</artifactId>
              <version>1</version>
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
              """
          ),
          mavenProject("my-app",
            pomXml(
              """
                    <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                        <parent>
                            <groupId>com.othercompany.app</groupId>
                            <artifactId>my-parent-app</artifactId>
                            <version>1</version>
                        </parent>
                        <dependencies>
                            <dependency>
                                <groupId>junit</groupId>
                                <artifactId>junit</artifactId>
                                <version>4.13.2</version>
                            </dependency>
                        </dependencies>
                    </project>
                """,
              """
                    <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                        <parent>
                            <groupId>com.othercompany.app</groupId>
                            <artifactId>my-parent-app</artifactId>
                            <version>1</version>
                        </parent>
                        <dependencies>
                            <dependency>
                                <groupId>junit</groupId>
                                <artifactId>junit</artifactId>
                            </dependency>
                        </dependencies>
                    </project>
                """
            )
          )
        );
    }

    @Test
    void manageToSpecefiedVersion() {
        rewriteRun(
          spec -> spec.recipe(new ManageDependencies(
            "junit",
            "junit",
            false, false)),
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
                          <version>4.13.2</version>
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
                              <groupId>junit</groupId>
                              <artifactId>junit</artifactId>
                              <version>4.13.2</version>
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
        );
    }

    @Test
    void addedToTheRootPom() {
        rewriteRun(
          spec -> spec.recipe(new ManageDependencies("junit", "junit", true, false)),
          pomXml(
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>project</artifactId>
                  <version>1</version>
                  <modules>
                      <module>core</module>
                      <module>api</module>
                      <module>service</module>
                  </modules>
              </project>
              """,
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>project</artifactId>
                  <version>1</version>
                  <modules>
                      <module>core</module>
                      <module>api</module>
                      <module>service</module>
                  </modules>
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
              """
          ),
          mavenProject("api",
            pomXml(
              """
                    <project>
                        <parent>
                            <groupId>com.mycompany.app</groupId>
                            <artifactId>project</artifactId>
                            <version>1</version>
                        </parent>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>api</artifactId>
                        <version>1</version>
                    </project>
                """
            )
          ),
          mavenProject("service",
            pomXml(
              """
                    <project>
                        <parent>
                            <groupId>com.mycompany.app</groupId>
                            <artifactId>api</artifactId>
                            <version>1</version>
                        </parent>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>service</artifactId>
                        <version>1</version>
                        <dependencies>
                            <dependency>
                                <groupId>junit</groupId>
                                <artifactId>junit</artifactId>
                                <version>4.13.2</version>
                            </dependency>
                        </dependencies>
                    </project>
                """,
              """
                    <project>
                        <parent>
                            <groupId>com.mycompany.app</groupId>
                            <artifactId>api</artifactId>
                            <version>1</version>
                        </parent>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>service</artifactId>
                        <version>1</version>
                        <dependencies>
                            <dependency>
                                <groupId>junit</groupId>
                                <artifactId>junit</artifactId>
                            </dependency>
                        </dependencies>
                    </project>
                """
            ),
            mavenProject("core",
              pomXml(
                """
                      <project>
                          <groupId>com.mycompany.app</groupId>
                          <artifactId>core</artifactId>
                          <version>1</version>
                      </project>
                  """
              )
            )
          )
        );
    }

    @Test
    void externalParent() {
        rewriteRun(
          spec -> spec.recipe(new ManageDependencies(
            "org.apache.logging.log4j",
            "log4j-*",
            true, false)),
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
                      <version>2.9.1</version>
                  </parent>
              </project>
              """,
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.managed.test</groupId>
                  <artifactId>a</artifactId>
                  <version>1.0.0</version>
                  <parent>
                      <groupId>com.fasterxml.jackson</groupId>
                      <artifactId>jackson-parent</artifactId>
                      <version>2.9.1</version>
                  </parent>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>org.apache.logging.log4j</groupId>
                              <artifactId>log4j-api</artifactId>
                              <version>2.17.2</version>
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
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.managed.test</groupId>
                        <artifactId>b</artifactId>
                        <version>1.0.0</version>
                        <parent>
                            <groupId>com.managed.test</groupId>
                            <artifactId>a</artifactId>
                            <version>1.0.0</version>
                        </parent>
                        <dependencies>
                            <dependency>
                                <groupId>junit</groupId>
                                <artifactId>junit</artifactId>
                                <version>4.11</version>
                            </dependency>
                            <dependency>
                                <groupId>org.apache.logging.log4j</groupId>
                                <artifactId>log4j-api</artifactId>
                                <version>2.17.2</version>
                            </dependency>
                        </dependencies>
                    </project>
                """,
              """
                    <project>
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>com.managed.test</groupId>
                        <artifactId>b</artifactId>
                        <version>1.0.0</version>
                        <parent>
                            <groupId>com.managed.test</groupId>
                            <artifactId>a</artifactId>
                            <version>1.0.0</version>
                        </parent>
                        <dependencies>
                            <dependency>
                                <groupId>junit</groupId>
                                <artifactId>junit</artifactId>
                                <version>4.11</version>
                            </dependency>
                            <dependency>
                                <groupId>org.apache.logging.log4j</groupId>
                                <artifactId>log4j-api</artifactId>
                            </dependency>
                        </dependencies>
                    </project>
                """
            )
          )
        );
    }

    @Test
    void externalManagedDependencyOverride() {
        rewriteRun(
          spec -> spec.recipe(new ManageDependencies(
            "junit",
            "junit",
            true, false)),
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
                      <version>2.9.1</version>
                  </parent>
                  <dependencies>
                      <dependency>
                          <groupId>junit</groupId>
                          <artifactId>junit</artifactId>
                          <version>4.11</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.managed.test</groupId>
                  <artifactId>a</artifactId>
                  <version>1.0.0</version>
                  <parent>
                      <groupId>com.fasterxml.jackson</groupId>
                      <artifactId>jackson-parent</artifactId>
                      <version>2.9.1</version>
                  </parent>
                  <dependencies>
                      <dependency>
                          <groupId>junit</groupId>
                          <artifactId>junit</artifactId>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }
}
