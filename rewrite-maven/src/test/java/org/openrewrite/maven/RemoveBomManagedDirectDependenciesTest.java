/*
 * Copyright 2026 the original author or authors.
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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class RemoveBomManagedDirectDependenciesTest implements RewriteTest {

    @DocumentExample
    @Test
    void removeDependencyWithDifferentMajorVersion() {
        rewriteRun(
          spec -> spec.recipe(new RemoveBomManagedDirectDependencies(
              "org.springframework.boot", "*-dependencies", "*", "*")),
          pomXml(
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>test</artifactId>
                  <version>1.0-SNAPSHOT</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>org.springframework.boot</groupId>
                              <artifactId>spring-boot-dependencies</artifactId>
                              <version>3.0.0</version>
                              <type>pom</type>
                              <scope>import</scope>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>org.apache.tomcat.embed</groupId>
                          <artifactId>tomcat-embed-core</artifactId>
                          <version>9.0.50</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>test</artifactId>
                  <version>1.0-SNAPSHOT</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>org.springframework.boot</groupId>
                              <artifactId>spring-boot-dependencies</artifactId>
                              <version>3.0.0</version>
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
    void keepDependencyWithSameMajorVersion() {
        rewriteRun(
          spec -> spec.recipe(new RemoveBomManagedDirectDependencies(
              "org.springframework.boot", "*-dependencies", "*", "*")),
          pomXml(
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>test</artifactId>
                  <version>1.0-SNAPSHOT</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>org.springframework.boot</groupId>
                              <artifactId>spring-boot-dependencies</artifactId>
                              <version>3.0.0</version>
                              <type>pom</type>
                              <scope>import</scope>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>org.apache.tomcat.embed</groupId>
                          <artifactId>tomcat-embed-core</artifactId>
                          <version>10.0.20</version>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }

    /**
     * Dependencies with exclusions are kept because removing them would lose the exclusion configuration,
     * potentially pulling in unwanted transitive dependencies from the BOM-managed version.
     */
    @Test
    void keepDependencyWithExclusions() {
        rewriteRun(
          spec -> spec.recipe(new RemoveBomManagedDirectDependencies(
              "org.springframework.boot", "*-dependencies", "*", "*")),
          pomXml(
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>test</artifactId>
                  <version>1.0-SNAPSHOT</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>org.springframework.boot</groupId>
                              <artifactId>spring-boot-dependencies</artifactId>
                              <version>3.0.0</version>
                              <type>pom</type>
                              <scope>import</scope>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>org.apache.tomcat.embed</groupId>
                          <artifactId>tomcat-embed-core</artifactId>
                          <version>9.0.50</version>
                          <exclusions>
                              <exclusion>
                                  <groupId>org.apache.tomcat</groupId>
                                  <artifactId>tomcat-annotations-api</artifactId>
                              </exclusion>
                          </exclusions>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }

    /**
     * Recipe is looking for {@code com.example:*-bom} but we have {@code org.springframework.boot:spring-boot-dependencies},
     * so no dependencies should be removed.
     */
    @Test
    void keepDependencyWhenBomPatternDoesNotMatch() {
        rewriteRun(
          spec -> spec.recipe(new RemoveBomManagedDirectDependencies(
              "com.example", "*-bom", "*", "*")),
          pomXml(
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>test</artifactId>
                  <version>1.0-SNAPSHOT</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>org.springframework.boot</groupId>
                              <artifactId>spring-boot-dependencies</artifactId>
                              <version>3.0.0</version>
                              <type>pom</type>
                              <scope>import</scope>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>org.apache.tomcat.embed</groupId>
                          <artifactId>tomcat-embed-core</artifactId>
                          <version>9.0.50</version>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void keepDependencyWithoutExplicitVersion() {
        rewriteRun(
          spec -> spec.recipe(new RemoveBomManagedDirectDependencies(
              "org.springframework.boot", "*-dependencies", "*", "*")),
          pomXml(
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>test</artifactId>
                  <version>1.0-SNAPSHOT</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>org.springframework.boot</groupId>
                              <artifactId>spring-boot-dependencies</artifactId>
                              <version>3.0.0</version>
                              <type>pom</type>
                              <scope>import</scope>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>org.apache.tomcat.embed</groupId>
                          <artifactId>tomcat-embed-core</artifactId>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void multiModuleProjectWithBomInParent() {
        rewriteRun(
          spec -> spec.recipe(new RemoveBomManagedDirectDependencies(
              "org.springframework.boot", "*-dependencies", "*", "*")),
          pomXml(
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1.0-SNAPSHOT</version>
                  <packaging>pom</packaging>
                  <modules>
                      <module>child</module>
                  </modules>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>org.springframework.boot</groupId>
                              <artifactId>spring-boot-dependencies</artifactId>
                              <version>3.0.0</version>
                              <type>pom</type>
                              <scope>import</scope>
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
                          <groupId>org.example</groupId>
                          <artifactId>parent</artifactId>
                          <version>1.0-SNAPSHOT</version>
                      </parent>
                      <artifactId>child</artifactId>
                      <dependencies>
                          <dependency>
                              <groupId>org.apache.tomcat.embed</groupId>
                              <artifactId>tomcat-embed-core</artifactId>
                              <version>9.0.50</version>
                          </dependency>
                      </dependencies>
                  </project>
                  """,
              """
                  <project>
                      <parent>
                          <groupId>org.example</groupId>
                          <artifactId>parent</artifactId>
                          <version>1.0-SNAPSHOT</version>
                      </parent>
                      <artifactId>child</artifactId>
                  </project>
                  """
            )
          )
        );
    }

    @Test
    void filterByDependencyGroupPattern() {
        rewriteRun(
          spec -> spec.recipe(new RemoveBomManagedDirectDependencies(
              "org.springframework.boot", "*-dependencies", "org.apache.tomcat.*", "*")),
          pomXml(
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>test</artifactId>
                  <version>1.0-SNAPSHOT</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>org.springframework.boot</groupId>
                              <artifactId>spring-boot-dependencies</artifactId>
                              <version>3.0.0</version>
                              <type>pom</type>
                              <scope>import</scope>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>org.apache.tomcat.embed</groupId>
                          <artifactId>tomcat-embed-core</artifactId>
                          <version>9.0.50</version>
                      </dependency>
                      <dependency>
                          <groupId>com.fasterxml.jackson.core</groupId>
                          <artifactId>jackson-core</artifactId>
                          <version>2.10.0</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>test</artifactId>
                  <version>1.0-SNAPSHOT</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>org.springframework.boot</groupId>
                              <artifactId>spring-boot-dependencies</artifactId>
                              <version>3.0.0</version>
                              <type>pom</type>
                              <scope>import</scope>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>com.fasterxml.jackson.core</groupId>
                          <artifactId>jackson-core</artifactId>
                          <version>2.10.0</version>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }
}
