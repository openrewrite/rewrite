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
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class AddPluginDependencyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddPluginDependency("org.openrewrite.maven", "rewrite-maven-plugin",
          "org.openrewrite.recipe", "rewrite-spring", "1.0.0"));
    }

    @DocumentExample
    @Test
    void addWithNoExistingDependencies() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0</version>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0</version>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
                              <dependencies>
                                  <dependency>
                                      <groupId>org.openrewrite.recipe</groupId>
                                      <artifactId>rewrite-spring</artifactId>
                                      <version>1.0.0</version>
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
    void addWithExistingDependency() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0</version>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
                              <dependencies>
                                  <dependency>
                                      <groupId>org.foo</groupId>
                                      <artifactId>bar</artifactId>
                                      <version>1.0.0</version>
                                  </dependency>
                                  <dependency>
                                      <groupId>org.openrewrite.recipe</groupId>
                                      <artifactId>rewrite-spring</artifactId>
                                      <version>0.1.0</version>
                                  </dependency>
                              </dependencies>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0</version>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
                              <dependencies>
                                  <dependency>
                                      <groupId>org.foo</groupId>
                                      <artifactId>bar</artifactId>
                                      <version>1.0.0</version>
                                  </dependency>
                                  <dependency>
                                      <groupId>org.openrewrite.recipe</groupId>
                                      <artifactId>rewrite-spring</artifactId>
                                      <version>1.0.0</version>
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
    void updateOnlyTheVersionOfAnExistingDependency() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0</version>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
                              <dependencies>
                                  <dependency>
                                      <groupId>org.openrewrite.recipe</groupId>
                                      <artifactId>rewrite-spring</artifactId>
                                      <!-- recipes for the 4.x plugin line -->
                                      <version>0.1.0</version>
                                      <classifier>tests</classifier>
                                      <exclusions>
                                          <exclusion>
                                              <groupId>org.slf4j</groupId>
                                              <artifactId>slf4j-api</artifactId>
                                          </exclusion>
                                      </exclusions>
                                  </dependency>
                              </dependencies>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0</version>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
                              <dependencies>
                                  <dependency>
                                      <groupId>org.openrewrite.recipe</groupId>
                                      <artifactId>rewrite-spring</artifactId>
                                      <!-- recipes for the 4.x plugin line -->
                                      <version>1.0.0</version>
                                      <classifier>tests</classifier>
                                      <exclusions>
                                          <exclusion>
                                              <groupId>org.slf4j</groupId>
                                              <artifactId>slf4j-api</artifactId>
                                          </exclusion>
                                      </exclusions>
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
    void replaceVersionNoPropertyOfThisPomControlsWithLiteral() {
        rewriteRun(
          mavenProject("parent",
            pomXml(
              """
                <project>
                    <groupId>org.example</groupId>
                    <artifactId>parent</artifactId>
                    <version>1.0</version>
                    <packaging>pom</packaging>
                    <modules>
                        <module>child</module>
                    </modules>
                    <properties>
                        <rewrite-spring.version>0.1.0</rewrite-spring.version>
                    </properties>
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
                          <version>1.0</version>
                      </parent>
                      <artifactId>child</artifactId>
                      <build>
                          <plugins>
                              <plugin>
                                  <groupId>org.openrewrite.maven</groupId>
                                  <artifactId>rewrite-maven-plugin</artifactId>
                                  <version>4.1.5</version>
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
                      <parent>
                          <groupId>org.example</groupId>
                          <artifactId>parent</artifactId>
                          <version>1.0</version>
                      </parent>
                      <artifactId>child</artifactId>
                      <build>
                          <plugins>
                              <plugin>
                                  <groupId>org.openrewrite.maven</groupId>
                                  <artifactId>rewrite-maven-plugin</artifactId>
                                  <version>4.1.5</version>
                                  <dependencies>
                                      <dependency>
                                          <groupId>org.openrewrite.recipe</groupId>
                                          <artifactId>rewrite-spring</artifactId>
                                          <version>1.0.0</version>
                                      </dependency>
                                  </dependencies>
                              </plugin>
                          </plugins>
                      </build>
                  </project>
                  """
              )
            )
          )
        );

        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>0.9</version>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
                              <dependencies>
                                  <dependency>
                                      <groupId>org.openrewrite.recipe</groupId>
                                      <artifactId>rewrite-spring</artifactId>
                                      <version>${undefined.version}</version>
                                  </dependency>
                              </dependencies>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>0.9</version>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
                              <dependencies>
                                  <dependency>
                                      <groupId>org.openrewrite.recipe</groupId>
                                      <artifactId>rewrite-spring</artifactId>
                                      <version>1.0.0</version>
                                  </dependency>
                              </dependencies>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """
          )
        );

        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>0.9</version>

                  <properties>
                      <major>0</major>
                      <minor>1</minor>
                  </properties>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
                              <dependencies>
                                  <dependency>
                                      <groupId>org.openrewrite.recipe</groupId>
                                      <artifactId>rewrite-spring</artifactId>
                                      <version>${major}.${minor}</version>
                                  </dependency>
                              </dependencies>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>0.9</version>

                  <properties>
                      <major>0</major>
                      <minor>1</minor>
                  </properties>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
                              <dependencies>
                                  <dependency>
                                      <groupId>org.openrewrite.recipe</groupId>
                                      <artifactId>rewrite-spring</artifactId>
                                      <version>1.0.0</version>
                                  </dependency>
                              </dependencies>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """
          )
        );

        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>0.9</version>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
                              <dependencies>
                                  <dependency>
                                      <groupId>org.openrewrite.recipe</groupId>
                                      <artifactId>rewrite-spring</artifactId>
                                      <version>${project.version}</version>
                                  </dependency>
                              </dependencies>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>0.9</version>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
                              <dependencies>
                                  <dependency>
                                      <groupId>org.openrewrite.recipe</groupId>
                                      <artifactId>rewrite-spring</artifactId>
                                      <version>1.0.0</version>
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
    void changeVersionPropertyThisPomOverridesFromInRepoParent() {
        rewriteRun(
          mavenProject("parent",
            pomXml(
              """
                <project>
                    <groupId>org.example</groupId>
                    <artifactId>parent</artifactId>
                    <version>1.0</version>
                    <packaging>pom</packaging>
                    <modules>
                        <module>child</module>
                    </modules>
                    <properties>
                        <rewrite-spring.version>0.1.0</rewrite-spring.version>
                    </properties>
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
                          <version>1.0</version>
                      </parent>
                      <artifactId>child</artifactId>
                      <properties>
                          <rewrite-spring.version>0.2.0</rewrite-spring.version>
                      </properties>
                      <build>
                          <plugins>
                              <plugin>
                                  <groupId>org.openrewrite.maven</groupId>
                                  <artifactId>rewrite-maven-plugin</artifactId>
                                  <version>4.1.5</version>
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
                      <parent>
                          <groupId>org.example</groupId>
                          <artifactId>parent</artifactId>
                          <version>1.0</version>
                      </parent>
                      <artifactId>child</artifactId>
                      <properties>
                          <rewrite-spring.version>1.0.0</rewrite-spring.version>
                      </properties>
                      <build>
                          <plugins>
                              <plugin>
                                  <groupId>org.openrewrite.maven</groupId>
                                  <artifactId>rewrite-maven-plugin</artifactId>
                                  <version>4.1.5</version>
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
            )
          )
        );
    }

    @Test
    void addVersionToExistingDependencyWithoutOne() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0</version>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
                              <dependencies>
                                  <dependency>
                                      <groupId>org.openrewrite.recipe</groupId>
                                      <artifactId>rewrite-spring</artifactId>
                                      <classifier>tests</classifier>
                                  </dependency>
                              </dependencies>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0</version>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
                              <dependencies>
                                  <dependency>
                                      <groupId>org.openrewrite.recipe</groupId>
                                      <artifactId>rewrite-spring</artifactId>
                                      <version>1.0.0</version>
                                      <classifier>tests</classifier>
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
    void keepSameOrNewerExistingDependencyVersion() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0</version>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
                              <dependencies>
                                  <dependency>
                                      <groupId>org.openrewrite.recipe</groupId>
                                      <artifactId>rewrite-spring</artifactId>
                                      <version>1.0.1</version>
                                  </dependency>
                              </dependencies>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """
          )
        );

        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0</version>

                  <properties>
                      <rewrite-spring.version>1.0.0</rewrite-spring.version>
                  </properties>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
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
    void upgradeExistingDependencyToRequestedPropertyVersion() {
        rewriteRun(
          spec -> spec.recipe(new AddPluginDependency("org.openrewrite.maven", "rewrite-maven-plugin",
            "org.openrewrite.recipe", "rewrite-spring", "${rewrite-spring.version}")),
          pomXml(
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0</version>

                  <properties>
                      <rewrite-spring.version>2.0.0</rewrite-spring.version>
                  </properties>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
                              <dependencies>
                                  <dependency>
                                      <groupId>org.openrewrite.recipe</groupId>
                                      <artifactId>rewrite-spring</artifactId>
                                      <version>1.0.1</version>
                                  </dependency>
                              </dependencies>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0</version>

                  <properties>
                      <rewrite-spring.version>2.0.0</rewrite-spring.version>
                  </properties>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
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

        rewriteRun(
          spec -> spec.recipe(new AddPluginDependency("org.openrewrite.maven", "rewrite-maven-plugin",
            "org.openrewrite.recipe", "rewrite-spring", "${rewrite-spring.version}")),
          pomXml(
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0</version>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
                              <dependencies>
                                  <dependency>
                                      <groupId>org.openrewrite.recipe</groupId>
                                      <artifactId>rewrite-spring</artifactId>
                                      <version>1.0.1</version>
                                  </dependency>
                              </dependencies>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0</version>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
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

        rewriteRun(
          spec -> spec.recipe(new AddPluginDependency("org.openrewrite.maven", "rewrite-maven-plugin",
            "org.openrewrite.recipe", "rewrite-spring", "${rewrite-spring.version}")),
          pomXml(
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0</version>

                  <properties>
                      <old.version>0.1.0</old.version>
                      <rewrite-spring.version>2.0.0</rewrite-spring.version>
                  </properties>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
                              <dependencies>
                                  <dependency>
                                      <groupId>org.openrewrite.recipe</groupId>
                                      <artifactId>rewrite-spring</artifactId>
                                      <version>${old.version}</version>
                                  </dependency>
                              </dependencies>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0</version>

                  <properties>
                      <old.version>0.1.0</old.version>
                      <rewrite-spring.version>2.0.0</rewrite-spring.version>
                  </properties>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
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
    void removePluginVersion() {
        rewriteRun(
          spec -> spec.recipe(new AddPluginDependency("org.openrewrite.maven", "rewrite-maven-plugin",
            "org.openrewrite.recipe", "rewrite-spring", null)),
          pomXml(
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0</version>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
                              <dependencies>
                                  <dependency>
                                      <groupId>org.openrewrite.recipe</groupId>
                                      <artifactId>rewrite-spring</artifactId>
                                      <version>0.1.0</version>
                                      <classifier>tests</classifier>
                                  </dependency>
                              </dependencies>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0</version>

                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.openrewrite.maven</groupId>
                              <artifactId>rewrite-maven-plugin</artifactId>
                              <version>4.1.5</version>
                              <dependencies>
                                  <dependency>
                                      <groupId>org.openrewrite.recipe</groupId>
                                      <artifactId>rewrite-spring</artifactId>
                                      <classifier>tests</classifier>
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
    void impliedGroupId() {
        rewriteRun(
          spec -> spec.recipe(new AddPluginDependency(
            "org.apache.maven.plugins", "maven-surefire-plugin",
            "org.openrewrite.recipe", "rewrite-spring", "1.0.0")),
          pomXml(
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0</version>
                  <build>
                      <plugins>
                          <plugin>
                              <artifactId>maven-surefire-plugin</artifactId>
                              <version>2.20.1</version>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            """
              <project>
                  <groupId>org.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0</version>
                  <build>
                      <plugins>
                          <plugin>
                              <artifactId>maven-surefire-plugin</artifactId>
                              <version>2.20.1</version>
                              <dependencies>
                                  <dependency>
                                      <groupId>org.openrewrite.recipe</groupId>
                                      <artifactId>rewrite-spring</artifactId>
                                      <version>1.0.0</version>
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

}
