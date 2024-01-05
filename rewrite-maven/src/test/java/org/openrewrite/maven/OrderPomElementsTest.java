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

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class OrderPomElementsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new OrderPomElements());
    }

    @Test
    void validOrderNoChange() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>

                  <groupId>my.org.project</groupId>
                  <artifactId>my-project</artifactId>
                  <version>4.3.0</version>
              
                  <name>Some Project</name>
                  <description>Some project desc</description>
              
                  <properties>
                  </properties>
              
                  <dependencyManagement>
                      <dependencies>
                      </dependencies>
                  </dependencyManagement>
              
                  <dependencies>
                  </dependencies>

                  <repositories>
                  </repositories>
              
                  <pluginRepositories>
                  </pluginRepositories>
              
                  <build>
                  </build>
              </project>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1228")
    @Test
    void updateOrder() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <parent>
                      <groupId>jpl</groupId>
                      <artifactId>jpl</artifactId>
                      <version>7.4.0</version>
                  </parent>
                  <!-- modelVersion1 -->
              
                  <!-- modelVersion2 -->
                  <modelVersion>4.0.0</modelVersion>
                  <artifactId>my-project</artifactId>
                  <groupId>my.org.project</groupId>
                  <version>4.3.0</version>
                  <properties>
                  </properties>
                  <description>Some project desc</description>
                  <name>Some Project</name>
                  <dependencies>
                      <dependency>
                          <!-- artifact content
                              comment -->
                          <artifactId>my-project</artifactId>
                          <scope>test</scope>
                          <!-- group content -->
                          <groupId>my.org.project</groupId>
                          <!-- version content -->
                          <version>4.3.0</version>
                      </dependency>
                  </dependencies>
                  <dependencyManagement>
                      <dependency>
                          <type>pom</type>
                          <version>2</version>
                          <groupId>my.org.project</groupId>
                          <scope>import</scope>
                          <artifactId>my-project-thing</artifactId>
                      </dependency>
                  </dependencyManagement>
                  <repositories>
                  </repositories>
                  <pluginRepositories>
                  </pluginRepositories>
                  <build>
                  </build>
              </project>
              """,
            """
              <project>
                  <!-- modelVersion1 -->
              
                  <!-- modelVersion2 -->
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                      <groupId>jpl</groupId>
                      <artifactId>jpl</artifactId>
                      <version>7.4.0</version>
                  </parent>
                  <groupId>my.org.project</groupId>
                  <artifactId>my-project</artifactId>
                  <version>4.3.0</version>
                  <name>Some Project</name>
                  <description>Some project desc</description>
                  <properties>
                  </properties>
                  <dependencyManagement>
                      <dependency>
                          <groupId>my.org.project</groupId>
                          <artifactId>my-project-thing</artifactId>
                          <version>2</version>
                          <type>pom</type>
                          <scope>import</scope>
                      </dependency>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                          <!-- group content -->
                          <groupId>my.org.project</groupId>
                          <!-- artifact content
                              comment -->
                          <artifactId>my-project</artifactId>
                          <!-- version content -->
                          <version>4.3.0</version>
                          <scope>test</scope>
                      </dependency>
                  </dependencies>
                  <repositories>
                  </repositories>
                  <pluginRepositories>
                  </pluginRepositories>
                  <build>
                  </build>
              </project>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1230")
    @Test
    void updateOrderCorrectNewLines() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <parent>
                      <groupId>jpl</groupId>
                      <artifactId>jpl</artifactId>
                      <version>7.4.0</version>
                  </parent>
              
                  <!-- model version comment -->
              
                  <!-- model version comment 2 -->
                  <modelVersion>4.0.0</modelVersion>
              
                  <artifactId>my-project</artifactId>
                  <groupId>my.org.project</groupId>
                  <version>4.3.0</version>
              
                  <properties>
                  </properties>
              
                  <description>Some project desc</description>
                  <name>Some Project</name>
              
                  <dependencies>
                      <dependency>
                          <!-- artifact content
                              comment -->
                          <artifactId>my-project</artifactId>
                          <!-- group content -->
                          <groupId>my.org.project</groupId>
                          <!-- version content -->
                          <version>4.3.0</version>
                      </dependency>
                  </dependencies>
                  <dependencyManagement>
                      <dependency>
                          <version>2</version>
                          <groupId>my.org.project</groupId>
                          <artifactId>my-project-thing</artifactId>
                      </dependency>
                  </dependencyManagement>
              
                  <repositories>
                  </repositories>
                  <pluginRepositories>
                  </pluginRepositories>
              
                  <build>
                  </build>
              </project>
              """,
            """
              <project>
                  <!-- model version comment -->
              
                  <!-- model version comment 2 -->
                  <modelVersion>4.0.0</modelVersion>
              
                  <parent>
                      <groupId>jpl</groupId>
                      <artifactId>jpl</artifactId>
                      <version>7.4.0</version>
                  </parent>
              
                  <groupId>my.org.project</groupId>
                  <artifactId>my-project</artifactId>
                  <version>4.3.0</version>
              
                  <name>Some Project</name>
              
                  <description>Some project desc</description>
                  <properties>
                  </properties>
              
                  <dependencyManagement>
                      <dependency>
                          <groupId>my.org.project</groupId>
                          <artifactId>my-project-thing</artifactId>
                          <version>2</version>
                      </dependency>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                          <!-- group content -->
                          <groupId>my.org.project</groupId>
                          <!-- artifact content
                              comment -->
                          <artifactId>my-project</artifactId>
                          <!-- version content -->
                          <version>4.3.0</version>
                      </dependency>
                  </dependencies>
              
                  <repositories>
                  </repositories>
                  <pluginRepositories>
                  </pluginRepositories>
              
                  <build>
                  </build>
              </project>
              """
          )
        );
    }
}
