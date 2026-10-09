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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class RemoveDuplicateDependenciesTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveDuplicateDependencies());
    }

    @DocumentExample
    @Test
    void removeSingleDuplicate() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                  <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.13.1</version>
                    <scope>test</scope>
                  </dependency>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
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

                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                  <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.13.1</version>
                    <scope>test</scope>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void notApplicable() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
          )
        );
    }

    @Test
    void preservesComments() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <!-- comment 1 -->
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                  <!-- comment 2 -->
                  <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.13.1</version>
                    <scope>test</scope>
                  </dependency>
                  <!-- comment 3 -->
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
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

                <dependencies>
                  <!-- comment 1 -->
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                  <!-- comment 2 -->
                  <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.13.1</version>
                    <scope>test</scope>
                  </dependency>
                  <!-- comment 3 -->
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void removeMultipleDuplicates() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                  <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.13.1</version>
                    <scope>test</scope>
                  </dependency>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
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

                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                  <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.13.1</version>
                    <scope>test</scope>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void keepLastDeclarationWhenVersionsDiffer() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>com.google.inject</groupId>
                    <artifactId>guice</artifactId>
                    <version>4.2.1</version>
                  </dependency>
                  <dependency>
                    <groupId>com.google.inject</groupId>
                    <artifactId>guice</artifactId>
                    <version>4.2.2</version>
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

                <dependencies>
                  <dependency>
                    <groupId>com.google.inject</groupId>
                    <artifactId>guice</artifactId>
                    <version>4.2.2</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void keepDependencyWithClassifier() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>com.google.inject</groupId>
                    <artifactId>guice</artifactId>
                    <version>4.2.2</version>
                  </dependency>
                  <dependency>
                    <groupId>com.google.inject</groupId>
                    <artifactId>guice</artifactId>
                    <version>4.2.2</version>
                    <classifier>no_aop</classifier>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void keepDependencyWithType() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                    <type>war</type>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void keepDependencyManagementWithType() {
        rewriteRun(
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
              	  <groupId>com.acme</groupId>
              	  <artifactId>example-dependency</artifactId>
                    <version>1.0.0</version>
              	</dependency>
              	<dependency>
                    <groupId>com.acme</groupId>
                    <artifactId>example-dependency</artifactId>
                    <version>1.0.0</version>
                    <type>test-jar</type>
              	</dependency>
                </dependencies>
              </dependencyManagement>
               </project>
              """
          )
        );
    }

    @Test
    void removeDuplicateWithDifferentScope() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                    <scope>compile</scope>
                  </dependency>
                  <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.13.2</version>
                    <scope>test</scope>
                  </dependency>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                    <scope>test</scope>
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

                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                    <scope>test</scope>
                  </dependency>
                  <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>4.13.2</version>
                    <scope>test</scope>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void keepExclusionsOfLastDeclaration() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                    <exclusions>
                      <exclusion>
                        <groupId>com.google.code.findbugs</groupId>
                        <artifactId>jsr305</artifactId>
                      </exclusion>
                    </exclusions>
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

                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                    <exclusions>
                      <exclusion>
                        <groupId>com.google.code.findbugs</groupId>
                        <artifactId>jsr305</artifactId>
                      </exclusion>
                    </exclusions>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void keepFirstManagedDeclarationWhenVersionsDiffer() {
        rewriteRun(
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
                      <version>29.0-jre</version>
                    </dependency>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>30.1-jre</version>
                      <scope>test</scope>
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
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>29.0-jre</version>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void removeManagedDuplicateWithDifferentVersionProperties() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <properties>
                  <guava.version>29.0-jre</guava.version>
                  <google.version>29.0-jre</google.version>
                </properties>

                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>${guava.version}</version>
                    </dependency>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>${google.version}</version>
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
                  <guava.version>29.0-jre</guava.version>
                  <google.version>29.0-jre</google.version>
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
          )
        );
    }

    @Test
    void keepDeclarationWhosePropertyResolvesToADuplicate() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <properties>
                  <extra.artifact>guava</extra.artifact>
                </properties>

                <profiles>
                  <profile>
                    <id>collections</id>
                    <properties>
                      <extra.artifact>guava-testlib</extra.artifact>
                    </properties>
                  </profile>
                </profiles>

                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>${extra.artifact}</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
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
    void removeDuplicatesInProfile() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <profiles>
                  <profile>
                    <id>extra</id>
                    <dependencies>
                      <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>29.0-jre</version>
                      </dependency>
                      <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>30.1-jre</version>
                      </dependency>
                    </dependencies>
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
                    <id>extra</id>
                    <dependencies>
                      <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>30.1-jre</version>
                      </dependency>
                    </dependencies>
                  </profile>
                </profiles>
              </project>
              """
          )
        );
    }

    @Test
    void removeManagedDuplicatesInProfile() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <profiles>
                  <profile>
                    <id>extra</id>
                    <dependencyManagement>
                      <dependencies>
                        <dependency>
                          <groupId>com.google.guava</groupId>
                          <artifactId>guava</artifactId>
                          <version>29.0-jre</version>
                        </dependency>
                        <dependency>
                          <groupId>com.google.guava</groupId>
                          <artifactId>guava</artifactId>
                          <version>30.1-jre</version>
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
                    <id>extra</id>
                    <dependencyManagement>
                      <dependencies>
                        <dependency>
                          <groupId>com.google.guava</groupId>
                          <artifactId>guava</artifactId>
                          <version>30.1-jre</version>
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
    void keepDependencyRedeclaredInProfile() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

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

                <profiles>
                  <profile>
                    <id>extra</id>
                    <dependencies>
                      <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>30.1-jre</version>
                      </dependency>
                    </dependencies>
                  </profile>
                </profiles>
              </project>
              """
          )
        );
    }

    @Test
    void keepDuplicateDependenciesOfPlugin() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-dependency-plugin</artifactId>
                      <version>3.8.1</version>
                      <dependencies>
                          <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>29.0-jre</version>
                          </dependency>
                          <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>30.1-jre</version>
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
    void removeDuplicatedDependencyWithImportScope() {
        rewriteRun(
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
                              <groupId>org.apache.logging.log4j</groupId>
                              <artifactId>log4j-bom</artifactId>
                              <version>2.24.0</version>
                              <scope>import</scope>
                              <type>pom</type>
                          </dependency>
                          <dependency>
                              <groupId>org.apache.logging.log4j</groupId>
                              <artifactId>log4j-bom</artifactId>
                              <version>2.24.0</version>
                              <scope>import</scope>
                              <type>pom</type>
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
                              <groupId>org.apache.logging.log4j</groupId>
                              <artifactId>log4j-bom</artifactId>
                              <version>2.24.0</version>
                              <scope>import</scope>
                              <type>pom</type>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void removeDependencyWithDefaultType() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                    <type>jar</type>
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

    @Issue("https://github.com/openrewrite/rewrite/issues/3832")
    @Test
    void retainDuplicateManagedDependenciesWithDifferentClassifier() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <properties>
                  <version.chromedriver>94.0.4606.61</version.chromedriver>
                </properties>

                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>selenium-tools</groupId>
                      <artifactId>chromedriver</artifactId>
                      <classifier>w64</classifier>
                      <version>${version.chromedriver}</version>
                      <type>tar.gz</type>
                    </dependency>
                    <dependency>
                      <groupId>selenium-tools</groupId>
                      <artifactId>chromedriver</artifactId>
                      <classifier>x64</classifier>
                      <version>${version.chromedriver}</version>
                      <type>tar.gz</type>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4868")
    @Test
    void retainWithAndWithoutClassifier() {
        rewriteRun(
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
                              <groupId>com.adobe.aio.cloudmanager</groupId>
                              <artifactId>aio-lib-cloudmanager</artifactId>
                              <classifier>java8</classifier>
                              <version>2.0.0</version>
                          </dependency>
                          <dependency>
                              <groupId>com.adobe.aio.cloudmanager</groupId>
                              <artifactId>aio-lib-cloudmanager</artifactId>
                              <version>2.0.0</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }
}
