/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.maven.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class FindManagedDependencyTest implements RewriteTest {
    @DocumentExample
    @Test
    void simple() {
        rewriteRun(spec -> spec.recipe(new FindManagedDependency("jakarta.activation", "jakarta.activation-api", null, null)),
          pomXml(
                """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>jakarta.activation</groupId>
                      <artifactId>jakarta.activation-api</artifactId>
                      <version>2.1.2</version>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <dependencyManagement>
                  <dependencies>
                    <!--~~>--><dependency>
                      <groupId>jakarta.activation</groupId>
                      <artifactId>jakarta.activation-api</artifactId>
                      <version>2.1.2</version>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void version() {
        rewriteRun(spec -> spec.recipe(new FindManagedDependency("jakarta.activation", "jakarta.activation-api", "2.1.2", null)),
          pomXml(
                """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>sample</artifactId>
              <version>1.0.0</version>
              <dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>jakarta.activation</groupId>
                    <artifactId>jakarta.activation-api</artifactId>
                    <version>2.1.2</version>
                  </dependency>
                </dependencies>
              </dependencyManagement>
            </project>
            """,
            """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>sample</artifactId>
              <version>1.0.0</version>
              <dependencyManagement>
                <dependencies>
                  <!--~~>--><dependency>
                    <groupId>jakarta.activation</groupId>
                    <artifactId>jakarta.activation-api</artifactId>
                    <version>2.1.2</version>
                  </dependency>
                </dependencies>
              </dependencyManagement>
            </project>
            """
          )
        );
    }

    @Test
    void versionRange() {
        rewriteRun(spec -> spec.recipe(new FindManagedDependency("jakarta.activation", "jakarta.activation-api", "^2", null)),
          pomXml(
                """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>sample</artifactId>
              <version>1.0.0</version>
              <dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>jakarta.activation</groupId>
                    <artifactId>jakarta.activation-api</artifactId>
                    <version>2.1.2</version>
                  </dependency>
                </dependencies>
              </dependencyManagement>
            </project>
            """,
            """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>sample</artifactId>
              <version>1.0.0</version>
              <dependencyManagement>
                <dependencies>
                  <!--~~>--><dependency>
                    <groupId>jakarta.activation</groupId>
                    <artifactId>jakarta.activation-api</artifactId>
                    <version>2.1.2</version>
                  </dependency>
                </dependencies>
              </dependencyManagement>
            </project>
            """
          )
        );
    }

    @Test
    void wrongVersion() {
        rewriteRun(spec -> spec.recipe(new FindManagedDependency("jakarta.activation", "jakarta.activation-api", "1.0.0", null)),
          pomXml(
                """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>sample</artifactId>
              <version>1.0.0</version>
              <dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>jakarta.activation</groupId>
                    <artifactId>jakarta.activation-api</artifactId>
                    <version>2.1.2</version>
                  </dependency>
                </dependencies>
              </dependencyManagement>
            </project>
            """
          )
        );
    }

    @Test
    void wrongVersionRange() {
        rewriteRun(spec -> spec.recipe(new FindManagedDependency("jakarta.activation", "jakarta.activation-api", "^1", null)),
          pomXml(
                """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>sample</artifactId>
              <version>1.0.0</version>
              <dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>jakarta.activation</groupId>
                    <artifactId>jakarta.activation-api</artifactId>
                    <version>2.1.2</version>
                  </dependency>
                </dependencies>
              </dependencyManagement>
            </project>
            """
          )
        );
    }

    @Test
    void versionInProperties() {
        rewriteRun(spec -> spec.recipe(new FindManagedDependency("jakarta.activation", "jakarta.activation-api", "2.1.2", null)),
          pomXml(
            """
        <project>
          <modelVersion>4.0.0</modelVersion>
          <groupId>org.sample</groupId>
          <artifactId>sample</artifactId>
          <version>1.0.0</version>
          <properties>
              <activation.version>2.1.2</activation.version>
          </properties>
          <dependencyManagement>
            <dependencies>
              <dependency>
                <groupId>jakarta.activation</groupId>
                <artifactId>jakarta.activation-api</artifactId>
                <version>${activation.version}</version>
              </dependency>
            </dependencies>
          </dependencyManagement>
        </project>
        """,
            """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>sample</artifactId>
              <version>1.0.0</version>
              <properties>
                  <activation.version>2.1.2</activation.version>
              </properties>
              <dependencyManagement>
                <dependencies>
                  <!--~~>--><dependency>
                    <groupId>jakarta.activation</groupId>
                    <artifactId>jakarta.activation-api</artifactId>
                    <version>${activation.version}</version>
                  </dependency>
                </dependencies>
              </dependencyManagement>
            </project>
            """
          )
        );
    }

    @Test
    void wrongVersionInProperties() {
        rewriteRun(spec -> spec.recipe(new FindManagedDependency("jakarta.activation", "jakarta.activation-api", "1.0.0", null)),
          pomXml(
            """
        <project>
          <modelVersion>4.0.0</modelVersion>
          <groupId>org.sample</groupId>
          <artifactId>sample</artifactId>
          <version>1.0.0</version>
          <properties>
              <activation.version>2.1.2</activation.version>
          </properties>
          <dependencyManagement>
            <dependencies>
              <dependency>
                <groupId>jakarta.activation</groupId>
                <artifactId>jakarta.activation-api</artifactId>
                <version>${activation.version}</version>
              </dependency>
            </dependencies>
          </dependencyManagement>
        </project>
        """
          )
        );
    }

    @Test
    void versionFromProjectParentVersion() {
        rewriteRun(spec -> spec.recipe(new FindManagedDependency("jakarta.activation", "jakarta.activation-api", "1.0.0", null)),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>parent</artifactId>
                <version>1.0.0</version>
                <packaging>pom</packaging>
              </project>
              """,
            SourceSpec::skip
          ),
          mavenProject("child",
            pomXml(
              """
                <project>
                  <parent>
                    <groupId>org.sample</groupId>
                    <artifactId>parent</artifactId>
                    <version>1.0.0</version>
                  </parent>
                  <artifactId>child</artifactId>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>jakarta.activation</groupId>
                        <artifactId>jakarta.activation-api</artifactId>
                        <version>${project.parent.version}</version>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                </project>
                """,
              """
                <project>
                  <parent>
                    <groupId>org.sample</groupId>
                    <artifactId>parent</artifactId>
                    <version>1.0.0</version>
                  </parent>
                  <artifactId>child</artifactId>
                  <dependencyManagement>
                    <dependencies>
                      <!--~~>--><dependency>
                        <groupId>jakarta.activation</groupId>
                        <artifactId>jakarta.activation-api</artifactId>
                        <version>${project.parent.version}</version>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                </project>
                """
            )
          )
        );
    }

    @Test
    void bomImportVersionFromProjectParentVersion() {
        rewriteRun(spec -> spec.recipe(new FindManagedDependency("org.junit", "junit-bom", "5.12.2", null)),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>parent</artifactId>
                <version>5.12.2</version>
                <packaging>pom</packaging>
              </project>
              """,
            SourceSpec::skip
          ),
          mavenProject("child",
            pomXml(
              """
                <project>
                  <parent>
                    <groupId>org.sample</groupId>
                    <artifactId>parent</artifactId>
                    <version>5.12.2</version>
                  </parent>
                  <artifactId>child</artifactId>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>org.junit</groupId>
                        <artifactId>junit-bom</artifactId>
                        <version>${project.parent.version}</version>
                        <type>pom</type>
                        <scope>import</scope>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                </project>
                """,
              """
                <project>
                  <parent>
                    <groupId>org.sample</groupId>
                    <artifactId>parent</artifactId>
                    <version>5.12.2</version>
                  </parent>
                  <artifactId>child</artifactId>
                  <dependencyManagement>
                    <dependencies>
                      <!--~~>--><dependency>
                        <groupId>org.junit</groupId>
                        <artifactId>junit-bom</artifactId>
                        <version>${project.parent.version}</version>
                        <type>pom</type>
                        <scope>import</scope>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                </project>
                """
            )
          )
        );
    }
}
