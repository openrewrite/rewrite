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
package org.openrewrite.maven.cleanup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class ManagedDependencyRequiresVersionTest implements RewriteTest {
    @Issue("https://github.com/openrewrite/rewrite/issues/1084")
    @Test
    void dependencyManagementDependencyRequiresVersion() {
        rewriteRun(
          spec -> spec.recipe(new DependencyManagementDependencyRequiresVersion()),
          pomXml(
            """
              <project>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0-SNAPSHOT</version>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.fasterxml.jackson.core</groupId>
                      <artifactId>jackson-core</artifactId>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """,
            """
              <project>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0-SNAPSHOT</version>
              </project>
              """
          )
        );
    }

    @Test
    void managedScopeAndExclusionsFromParentManagedVersion() {
        rewriteRun(
          spec -> spec.recipe(new DependencyManagementDependencyRequiresVersion()),
          mavenProject("parent",
            pomXml(
              """
                <project>
                  <groupId>com.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1</version>
                  <packaging>pom</packaging>
                  <modules>
                    <module>child</module>
                  </modules>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>33.4.8-jre</version>
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
                      <groupId>com.example</groupId>
                      <artifactId>parent</artifactId>
                      <version>1</version>
                    </parent>
                    <artifactId>child</artifactId>
                    <dependencyManagement>
                      <dependencies>
                        <dependency>
                          <groupId>com.google.guava</groupId>
                          <artifactId>guava</artifactId>
                          <scope>runtime</scope>
                          <exclusions>
                            <exclusion>
                              <groupId>com.google.code.findbugs</groupId>
                              <artifactId>jsr305</artifactId>
                            </exclusion>
                          </exclusions>
                        </dependency>
                      </dependencies>
                    </dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>33.4.8-jre</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """
              )
            )
          )
        );
    }

    @Test
    void coordinatesOnlyEntryHidesParentManagedVersion() {
        rewriteRun(
          spec -> spec.recipe(new DependencyManagementDependencyRequiresVersion()),
          mavenProject("parent",
            pomXml(
              """
                <project>
                  <groupId>com.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1</version>
                  <packaging>pom</packaging>
                  <modules>
                    <module>child</module>
                  </modules>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>33.4.8-jre</version>
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
                      <groupId>com.example</groupId>
                      <artifactId>parent</artifactId>
                      <version>1</version>
                    </parent>
                    <artifactId>child</artifactId>
                    <dependencyManagement>
                      <dependencies>
                        <dependency>
                          <groupId>com.google.guava</groupId>
                          <artifactId>guava</artifactId>
                        </dependency>
                      </dependencies>
                    </dependencyManagement>
                  </project>
                  """
              )
            )
          )
        );
    }

    @Test
    void coordinatesOnlyEntryWhenTheParentManagesNothingForIt() {
        rewriteRun(
          spec -> spec.recipe(new DependencyManagementDependencyRequiresVersion()),
          mavenProject("parent",
            pomXml(
              """
                <project>
                  <groupId>com.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1</version>
                  <packaging>pom</packaging>
                  <modules>
                    <module>child</module>
                  </modules>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>com.fasterxml.jackson.core</groupId>
                        <artifactId>jackson-core</artifactId>
                        <version>2.19.0</version>
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
                      <groupId>com.example</groupId>
                      <artifactId>parent</artifactId>
                      <version>1</version>
                    </parent>
                    <artifactId>child</artifactId>
                    <dependencyManagement>
                      <dependencies>
                        <dependency>
                          <groupId>com.google.guava</groupId>
                          <artifactId>guava</artifactId>
                        </dependency>
                      </dependencies>
                    </dependencyManagement>
                  </project>
                  """,
                """
                  <project>
                    <parent>
                      <groupId>com.example</groupId>
                      <artifactId>parent</artifactId>
                      <version>1</version>
                    </parent>
                    <artifactId>child</artifactId>
                  </project>
                  """
              )
            )
          )
        );
    }

    @Test
    void coordinatesOnlyEntryMayHideAnImportedBom() {
        rewriteRun(
          spec -> spec.recipe(new DependencyManagementDependencyRequiresVersion()),
          mavenProject("bom",
            pomXml(
              """
                <project>
                  <groupId>com.example</groupId>
                  <artifactId>bom</artifactId>
                  <version>1</version>
                  <packaging>pom</packaging>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>33.4.8-jre</version>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                </project>
                """
            )
          ),
          mavenProject("app",
            pomXml(
              """
                <project>
                  <groupId>com.example</groupId>
                  <artifactId>app</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>bom</artifactId>
                        <version>1</version>
                        <type>pom</type>
                        <scope>import</scope>
                      </dependency>
                      <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
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
    void coordinatesOnlyEntryWhenTheParentManagesInAnInactiveProfile() {
        rewriteRun(
          spec -> spec.recipe(new DependencyManagementDependencyRequiresVersion()),
          mavenProject("parent",
            pomXml(
              """
                <project>
                  <groupId>com.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1</version>
                  <packaging>pom</packaging>
                  <modules>
                    <module>child</module>
                  </modules>
                  <profiles>
                    <profile>
                      <id>pinned</id>
                      <dependencyManagement>
                        <dependencies>
                          <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>33.4.8-jre</version>
                          </dependency>
                        </dependencies>
                      </dependencyManagement>
                    </profile>
                  </profiles>
                </project>
                """
            ),
            mavenProject("child",
              pomXml(
                """
                  <project>
                    <parent>
                      <groupId>com.example</groupId>
                      <artifactId>parent</artifactId>
                      <version>1</version>
                    </parent>
                    <artifactId>child</artifactId>
                    <dependencyManagement>
                      <dependencies>
                        <dependency>
                          <groupId>com.google.guava</groupId>
                          <artifactId>guava</artifactId>
                        </dependency>
                      </dependencies>
                    </dependencyManagement>
                  </project>
                  """
              )
            )
          )
        );
    }

    @Test
    void coordinatesOnlyEntryInAPomAnotherModuleDeclaresAsItsParent() {
        rewriteRun(
          spec -> spec.recipe(new DependencyManagementDependencyRequiresVersion()),
          mavenProject("parent",
            pomXml(
              """
                <project>
                  <groupId>com.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
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
                      <groupId>com.example</groupId>
                      <artifactId>parent</artifactId>
                      <version>1</version>
                    </parent>
                    <artifactId>child</artifactId>
                  </project>
                  """
              )
            )
          )
        );
    }

    // Without a guard against a cyclic ancestry the walk below never terminates, so fail rather than hang.
    @Timeout(30)
    @Test
    void coordinatesOnlyEntryInAPomWithACyclicAncestry() {
        rewriteRun(
          spec -> spec.recipe(new DependencyManagementDependencyRequiresVersion()),
          mavenProject("a",
            pomXml(
              """
                <project>
                  <parent>
                    <groupId>com.example</groupId>
                    <artifactId>b</artifactId>
                    <version>1</version>
                  </parent>
                  <artifactId>a</artifactId>
                </project>
                """
            )
          ),
          mavenProject("b",
            pomXml(
              """
                <project>
                  <parent>
                    <groupId>com.example</groupId>
                    <artifactId>a</artifactId>
                    <version>1</version>
                  </parent>
                  <artifactId>b</artifactId>
                </project>
                """
            )
          ),
          mavenProject("leaf",
            pomXml(
              """
                <project>
                  <parent>
                    <groupId>com.example</groupId>
                    <artifactId>a</artifactId>
                    <version>1</version>
                  </parent>
                  <artifactId>leaf</artifactId>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
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
    void coordinatesOnlyEntryBesideASiblingWithAnUnresolvableProperty() {
        rewriteRun(
          spec -> spec.recipe(new DependencyManagementDependencyRequiresVersion()),
          pomXml(
            """
              <project>
                <groupId>com.example</groupId>
                <artifactId>app</artifactId>
                <version>1</version>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                    </dependency>
                    <dependency>
                      <groupId>${guava.group}</groupId>
                      <artifactId>guava</artifactId>
                      <version>33.4.8-jre</version>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void coordinatesOnlyEntryInAPomThatOthersInheritFromOrImport() {
        rewriteRun(
          spec -> spec.recipe(new DependencyManagementDependencyRequiresVersion()),
          pomXml(
            """
              <project>
                <groupId>com.example</groupId>
                <artifactId>parent</artifactId>
                <version>1</version>
                <packaging>pom</packaging>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void coordinatesOnlyEntryWithAnUnresolvableProperty() {
        rewriteRun(
          spec -> spec.recipe(new DependencyManagementDependencyRequiresVersion()),
          pomXml(
            """
              <project>
                <groupId>com.example</groupId>
                <artifactId>app</artifactId>
                <version>1</version>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>${guava.group}</groupId>
                      <artifactId>guava</artifactId>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void coordinatesOnlyEntryWithAResolvableProperty() {
        rewriteRun(
          spec -> spec.recipe(new DependencyManagementDependencyRequiresVersion()),
          pomXml(
            """
              <project>
                <groupId>com.example</groupId>
                <artifactId>app</artifactId>
                <version>1</version>
                <properties>
                  <guava.group>com.google.guava</guava.group>
                </properties>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>${guava.group}</groupId>
                      <artifactId>guava</artifactId>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """,
            """
              <project>
                <groupId>com.example</groupId>
                <artifactId>app</artifactId>
                <version>1</version>
                <properties>
                  <guava.group>com.google.guava</guava.group>
                </properties>
              </project>
              """
          )
        );
    }

    @Test
    void coordinatesOnlyEntryBesideADuplicateInTheSamePom() {
        rewriteRun(
          spec -> spec.recipe(new DependencyManagementDependencyRequiresVersion()),
          pomXml(
            """
              <project>
                <groupId>com.example</groupId>
                <artifactId>app</artifactId>
                <version>1</version>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                    </dependency>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>33.4.8-jre</version>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void managedScope() {
        rewriteRun(
          spec -> spec.recipe(new DependencyManagementDependencyRequiresVersion()),
          pomXml(
            """
              <project>
                <groupId>com.example</groupId>
                <artifactId>app</artifactId>
                <version>1</version>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <scope>runtime</scope>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>33.4.8-jre</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void managedExclusions() {
        rewriteRun(
          spec -> spec.recipe(new DependencyManagementDependencyRequiresVersion()),
          pomXml(
            """
              <project>
                <groupId>com.example</groupId>
                <artifactId>app</artifactId>
                <version>1</version>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <exclusions>
                        <exclusion>
                          <groupId>com.google.code.findbugs</groupId>
                          <artifactId>jsr305</artifactId>
                        </exclusion>
                      </exclusions>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>33.4.8-jre</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void managedOptional() {
        rewriteRun(
          spec -> spec.recipe(new DependencyManagementDependencyRequiresVersion()),
          pomXml(
            """
              <project>
                <groupId>com.example</groupId>
                <artifactId>app</artifactId>
                <version>1</version>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <optional>true</optional>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>33.4.8-jre</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void declaresMoreThanCoordinates() {
        rewriteRun(
          spec -> spec.recipe(new DependencyManagementDependencyRequiresVersion()),
          pomXml(
            """
              <project>
                <groupId>com.example</groupId>
                <artifactId>app</artifactId>
                <version>1</version>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <type>test-jar</type>
                      <classifier>tests</classifier>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void incompleteCoordinates() {
        rewriteRun(
          spec -> spec.recipe(new DependencyManagementDependencyRequiresVersion()),
          pomXml(
            """
              <project>
                <groupId>com.example</groupId>
                <artifactId>app</artifactId>
                <version>1</version>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void removesOnlyTheEntryThatManagesNothing() {
        rewriteRun(
          spec -> spec.recipe(new DependencyManagementDependencyRequiresVersion()),
          pomXml(
            """
              <project>
                <groupId>com.example</groupId>
                <artifactId>app</artifactId>
                <version>1</version>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <scope>runtime</scope>
                    </dependency>
                    <dependency>
                      <groupId>com.fasterxml.jackson.core</groupId>
                      <artifactId>jackson-core</artifactId>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>33.4.8-jre</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <groupId>com.example</groupId>
                <artifactId>app</artifactId>
                <version>1</version>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <scope>runtime</scope>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>33.4.8-jre</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void propertyVersion() {
        rewriteRun(
          spec -> spec.recipe(new DependencyManagementDependencyRequiresVersion()),
          pomXml(
            """
              <project>
                <groupId>com.example</groupId>
                <artifactId>app</artifactId>
                <version>1</version>
                <properties>
                  <guava.version>33.4.8-jre</guava.version>
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
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }
}
