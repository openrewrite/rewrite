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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

/**
 * The contract for what {@link UpgradeDependencyVersion} may and may not do to a {@code <properties>}
 * entry that more than one dependency resolves its version through.
 * <p>
 * Every shape gets a matched pair, and the pairing is the point. The equivalent Gradle fix
 * (<a href="https://github.com/openrewrite/rewrite/pull/7491">#7491</a>) shipped with three tests, all
 * asserting that a shared variable is <em>not</em> bumped when that would break a sibling, and none
 * asserting that it still <em>is</em> bumped when it would not. The gate it introduced was therefore free
 * to be far stricter than intended — it detached every shared variable that had any untargeted
 * neighbour — and that went unnoticed until
 * <a href="https://github.com/openrewrite/rewrite/pull/7694">#7694</a> ten days later. Nesting the two
 * directions together makes shipping only half of a gate visible at a glance.
 * <p>
 * The coordinates are load-bearing. Since 2.20 {@code jackson-annotations} publishes bare minor versions
 * while every other Jackson 2.x artifact publishes three-part versions, so {@code 2.21} is a real release
 * of {@code jackson-annotations} and of nothing else. {@code 2.15.2} is published for all of them.
 */
@Issue("https://github.com/openrewrite/rewrite/issues/8656")
class UpgradeDependencyVersionSharedPropertyTest implements RewriteTest {

    /**
     * Published for {@code jackson-annotations}, and for no other Jackson 2.x artifact.
     */
    private static final String UNSHARED = "2.21";

    /**
     * Published for every Jackson 2.x artifact.
     */
    private static final String SHARED = "2.15.2";

    private static UpgradeDependencyVersion upgradeAnnotationsTo(String version) {
        return new UpgradeDependencyVersion("com.fasterxml.jackson.core", "jackson-annotations", version, null, true, null);
    }

    @Nested
    class PlainDependency {

        @Test
        void decouplesTargetWhenNeighbourLacksTheNewVersion() {
            rewriteRun(
              spec -> spec.recipe(upgradeAnnotationsTo(UNSHARED)),
              pomXml(
                """
                  <project>
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-app</artifactId>
                      <version>1</version>
                      <properties>
                          <jackson.version>2.10.5</jackson.version>
                      </properties>
                      <dependencies>
                          <dependency>
                              <groupId>com.fasterxml.jackson.core</groupId>
                              <artifactId>jackson-annotations</artifactId>
                              <version>${jackson.version}</version>
                          </dependency>
                          <dependency>
                              <groupId>com.fasterxml.jackson.core</groupId>
                              <artifactId>jackson-core</artifactId>
                              <version>${jackson.version}</version>
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
                          <jackson.version>2.10.5</jackson.version>
                      </properties>
                      <dependencies>
                          <dependency>
                              <groupId>com.fasterxml.jackson.core</groupId>
                              <artifactId>jackson-annotations</artifactId>
                              <version>2.21</version>
                          </dependency>
                          <dependency>
                              <groupId>com.fasterxml.jackson.core</groupId>
                              <artifactId>jackson-core</artifactId>
                              <version>${jackson.version}</version>
                          </dependency>
                      </dependencies>
                  </project>
                  """
              )
            );
        }

        @Test
        void bumpsPropertyWhenNeighbourPublishesTheNewVersion() {
            rewriteRun(
              spec -> spec.recipe(upgradeAnnotationsTo(SHARED)),
              pomXml(
                """
                  <project>
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-app</artifactId>
                      <version>1</version>
                      <properties>
                          <jackson.version>2.10.5</jackson.version>
                      </properties>
                      <dependencies>
                          <dependency>
                              <groupId>com.fasterxml.jackson.core</groupId>
                              <artifactId>jackson-annotations</artifactId>
                              <version>${jackson.version}</version>
                          </dependency>
                          <dependency>
                              <groupId>com.fasterxml.jackson.core</groupId>
                              <artifactId>jackson-core</artifactId>
                              <version>${jackson.version}</version>
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
                          <jackson.version>2.15.2</jackson.version>
                      </properties>
                      <dependencies>
                          <dependency>
                              <groupId>com.fasterxml.jackson.core</groupId>
                              <artifactId>jackson-annotations</artifactId>
                              <version>${jackson.version}</version>
                          </dependency>
                          <dependency>
                              <groupId>com.fasterxml.jackson.core</groupId>
                              <artifactId>jackson-core</artifactId>
                              <version>${jackson.version}</version>
                          </dependency>
                      </dependencies>
                  </project>
                  """
              )
            );
        }
    }

    @Nested
    class ManagedDependency {

        @Test
        void decouplesTargetWhenNeighbourLacksTheNewVersion() {
            rewriteRun(
              spec -> spec.recipe(upgradeAnnotationsTo(UNSHARED)),
              pomXml(
                """
                  <project>
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-app</artifactId>
                      <version>1</version>
                      <properties>
                          <jackson.version>2.10.5</jackson.version>
                      </properties>
                      <dependencyManagement>
                          <dependencies>
                              <dependency>
                                  <groupId>com.fasterxml.jackson.core</groupId>
                                  <artifactId>jackson-annotations</artifactId>
                                  <version>${jackson.version}</version>
                              </dependency>
                              <dependency>
                                  <groupId>com.fasterxml.jackson.core</groupId>
                                  <artifactId>jackson-core</artifactId>
                                  <version>${jackson.version}</version>
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
                          <jackson.version>2.10.5</jackson.version>
                      </properties>
                      <dependencyManagement>
                          <dependencies>
                              <dependency>
                                  <groupId>com.fasterxml.jackson.core</groupId>
                                  <artifactId>jackson-annotations</artifactId>
                                  <version>2.21</version>
                              </dependency>
                              <dependency>
                                  <groupId>com.fasterxml.jackson.core</groupId>
                                  <artifactId>jackson-core</artifactId>
                                  <version>${jackson.version}</version>
                              </dependency>
                          </dependencies>
                      </dependencyManagement>
                  </project>
                  """
              )
            );
        }

        @Test
        void bumpsPropertyWhenNeighbourPublishesTheNewVersion() {
            rewriteRun(
              spec -> spec.recipe(upgradeAnnotationsTo(SHARED)),
              pomXml(
                """
                  <project>
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-app</artifactId>
                      <version>1</version>
                      <properties>
                          <jackson.version>2.10.5</jackson.version>
                      </properties>
                      <dependencyManagement>
                          <dependencies>
                              <dependency>
                                  <groupId>com.fasterxml.jackson.core</groupId>
                                  <artifactId>jackson-annotations</artifactId>
                                  <version>${jackson.version}</version>
                              </dependency>
                              <dependency>
                                  <groupId>com.fasterxml.jackson.core</groupId>
                                  <artifactId>jackson-core</artifactId>
                                  <version>${jackson.version}</version>
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
                          <jackson.version>2.15.2</jackson.version>
                      </properties>
                      <dependencyManagement>
                          <dependencies>
                              <dependency>
                                  <groupId>com.fasterxml.jackson.core</groupId>
                                  <artifactId>jackson-annotations</artifactId>
                                  <version>${jackson.version}</version>
                              </dependency>
                              <dependency>
                                  <groupId>com.fasterxml.jackson.core</groupId>
                                  <artifactId>jackson-core</artifactId>
                                  <version>${jackson.version}</version>
                              </dependency>
                          </dependencies>
                      </dependencyManagement>
                  </project>
                  """
              )
            );
        }
    }

    /**
     * The shape with the widest blast radius: over-decoupling here does not write one literal, it writes
     * an explicit version into every consuming module.
     */
    @Nested
    class PropertyInLocalParent {

        @Test
        void decouplesInTheConsumingModuleAndLeavesTheParentPropertyAlone() {
            rewriteRun(
              spec -> spec.recipe(upgradeAnnotationsTo(UNSHARED)),
              pomXml(
                """
                  <project>
                      <groupId>com.mycompany</groupId>
                      <artifactId>my-parent</artifactId>
                      <version>1</version>
                      <packaging>pom</packaging>
                      <properties>
                          <jackson.version>2.10.5</jackson.version>
                      </properties>
                      <modules>
                          <module>annotations-module</module>
                          <module>core-module</module>
                      </modules>
                  </project>
                  """
              ),
              mavenProject("annotations-module",
                pomXml(
                  """
                    <project>
                        <parent>
                            <groupId>com.mycompany</groupId>
                            <artifactId>my-parent</artifactId>
                            <version>1</version>
                        </parent>
                        <artifactId>annotations-module</artifactId>
                        <dependencies>
                            <dependency>
                                <groupId>com.fasterxml.jackson.core</groupId>
                                <artifactId>jackson-annotations</artifactId>
                                <version>${jackson.version}</version>
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
                        <artifactId>annotations-module</artifactId>
                        <dependencies>
                            <dependency>
                                <groupId>com.fasterxml.jackson.core</groupId>
                                <artifactId>jackson-annotations</artifactId>
                                <version>2.21</version>
                            </dependency>
                        </dependencies>
                    </project>
                    """
                )
              ),
              mavenProject("core-module",
                pomXml(
                  """
                    <project>
                        <parent>
                            <groupId>com.mycompany</groupId>
                            <artifactId>my-parent</artifactId>
                            <version>1</version>
                        </parent>
                        <artifactId>core-module</artifactId>
                        <dependencies>
                            <dependency>
                                <groupId>com.fasterxml.jackson.core</groupId>
                                <artifactId>jackson-core</artifactId>
                                <version>${jackson.version}</version>
                            </dependency>
                        </dependencies>
                    </project>
                    """
                )
              )
            );
        }

        @Test
        void bumpsTheParentPropertyAndLeavesEveryModuleOnIt() {
            rewriteRun(
              spec -> spec.recipe(upgradeAnnotationsTo(SHARED)),
              pomXml(
                """
                  <project>
                      <groupId>com.mycompany</groupId>
                      <artifactId>my-parent</artifactId>
                      <version>1</version>
                      <packaging>pom</packaging>
                      <properties>
                          <jackson.version>2.10.5</jackson.version>
                      </properties>
                      <modules>
                          <module>annotations-module</module>
                          <module>core-module</module>
                      </modules>
                  </project>
                  """,
                """
                  <project>
                      <groupId>com.mycompany</groupId>
                      <artifactId>my-parent</artifactId>
                      <version>1</version>
                      <packaging>pom</packaging>
                      <properties>
                          <jackson.version>2.15.2</jackson.version>
                      </properties>
                      <modules>
                          <module>annotations-module</module>
                          <module>core-module</module>
                      </modules>
                  </project>
                  """
              ),
              mavenProject("annotations-module",
                pomXml(
                  """
                    <project>
                        <parent>
                            <groupId>com.mycompany</groupId>
                            <artifactId>my-parent</artifactId>
                            <version>1</version>
                        </parent>
                        <artifactId>annotations-module</artifactId>
                        <dependencies>
                            <dependency>
                                <groupId>com.fasterxml.jackson.core</groupId>
                                <artifactId>jackson-annotations</artifactId>
                                <version>${jackson.version}</version>
                            </dependency>
                        </dependencies>
                    </project>
                    """
                )
              ),
              mavenProject("core-module",
                pomXml(
                  """
                    <project>
                        <parent>
                            <groupId>com.mycompany</groupId>
                            <artifactId>my-parent</artifactId>
                            <version>1</version>
                        </parent>
                        <artifactId>core-module</artifactId>
                        <dependencies>
                            <dependency>
                                <groupId>com.fasterxml.jackson.core</groupId>
                                <artifactId>jackson-core</artifactId>
                                <version>${jackson.version}</version>
                            </dependency>
                        </dependencies>
                    </project>
                    """
                )
              )
            );
        }
    }

    @Nested
    class PluginDependency {

        @Test
        void decouplesTargetWhenNeighbourLacksTheNewVersion() {
            rewriteRun(
              spec -> spec.recipe(upgradeAnnotationsTo(UNSHARED)),
              pomXml(
                """
                  <project>
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-app</artifactId>
                      <version>1</version>
                      <properties>
                          <jackson.version>2.10.5</jackson.version>
                      </properties>
                      <dependencies>
                          <dependency>
                              <groupId>com.fasterxml.jackson.core</groupId>
                              <artifactId>jackson-core</artifactId>
                              <version>${jackson.version}</version>
                          </dependency>
                      </dependencies>
                      <build>
                          <plugins>
                              <plugin>
                                  <groupId>org.openrewrite.maven</groupId>
                                  <artifactId>rewrite-maven-plugin</artifactId>
                                  <version>5.4.1</version>
                                  <dependencies>
                                      <dependency>
                                          <groupId>com.fasterxml.jackson.core</groupId>
                                          <artifactId>jackson-annotations</artifactId>
                                          <version>${jackson.version}</version>
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
                          <jackson.version>2.10.5</jackson.version>
                      </properties>
                      <dependencies>
                          <dependency>
                              <groupId>com.fasterxml.jackson.core</groupId>
                              <artifactId>jackson-core</artifactId>
                              <version>${jackson.version}</version>
                          </dependency>
                      </dependencies>
                      <build>
                          <plugins>
                              <plugin>
                                  <groupId>org.openrewrite.maven</groupId>
                                  <artifactId>rewrite-maven-plugin</artifactId>
                                  <version>5.4.1</version>
                                  <dependencies>
                                      <dependency>
                                          <groupId>com.fasterxml.jackson.core</groupId>
                                          <artifactId>jackson-annotations</artifactId>
                                          <version>2.21</version>
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
        void bumpsPropertyWhenNeighbourPublishesTheNewVersion() {
            rewriteRun(
              spec -> spec.recipe(upgradeAnnotationsTo(SHARED)),
              pomXml(
                """
                  <project>
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-app</artifactId>
                      <version>1</version>
                      <properties>
                          <jackson.version>2.10.5</jackson.version>
                      </properties>
                      <dependencies>
                          <dependency>
                              <groupId>com.fasterxml.jackson.core</groupId>
                              <artifactId>jackson-core</artifactId>
                              <version>${jackson.version}</version>
                          </dependency>
                      </dependencies>
                      <build>
                          <plugins>
                              <plugin>
                                  <groupId>org.openrewrite.maven</groupId>
                                  <artifactId>rewrite-maven-plugin</artifactId>
                                  <version>5.4.1</version>
                                  <dependencies>
                                      <dependency>
                                          <groupId>com.fasterxml.jackson.core</groupId>
                                          <artifactId>jackson-annotations</artifactId>
                                          <version>${jackson.version}</version>
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
                          <jackson.version>2.15.2</jackson.version>
                      </properties>
                      <dependencies>
                          <dependency>
                              <groupId>com.fasterxml.jackson.core</groupId>
                              <artifactId>jackson-core</artifactId>
                              <version>${jackson.version}</version>
                          </dependency>
                      </dependencies>
                      <build>
                          <plugins>
                              <plugin>
                                  <groupId>org.openrewrite.maven</groupId>
                                  <artifactId>rewrite-maven-plugin</artifactId>
                                  <version>5.4.1</version>
                                  <dependencies>
                                      <dependency>
                                          <groupId>com.fasterxml.jackson.core</groupId>
                                          <artifactId>jackson-annotations</artifactId>
                                          <version>${jackson.version}</version>
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

    @Nested
    class AnnotationProcessorPath {

        @Test
        void decouplesTargetWhenNeighbourLacksTheNewVersion() {
            rewriteRun(
              spec -> spec.recipe(upgradeAnnotationsTo(UNSHARED)),
              pomXml(
                """
                  <project>
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-app</artifactId>
                      <version>1</version>
                      <properties>
                          <jackson.version>2.10.5</jackson.version>
                      </properties>
                      <dependencies>
                          <dependency>
                              <groupId>com.fasterxml.jackson.core</groupId>
                              <artifactId>jackson-core</artifactId>
                              <version>${jackson.version}</version>
                          </dependency>
                      </dependencies>
                      <build>
                          <plugins>
                              <plugin>
                                  <groupId>org.apache.maven.plugins</groupId>
                                  <artifactId>maven-compiler-plugin</artifactId>
                                  <version>3.8.1</version>
                                  <configuration>
                                      <annotationProcessorPaths>
                                          <path>
                                              <groupId>com.fasterxml.jackson.core</groupId>
                                              <artifactId>jackson-annotations</artifactId>
                                              <version>${jackson.version}</version>
                                          </path>
                                      </annotationProcessorPaths>
                                  </configuration>
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
                          <jackson.version>2.10.5</jackson.version>
                      </properties>
                      <dependencies>
                          <dependency>
                              <groupId>com.fasterxml.jackson.core</groupId>
                              <artifactId>jackson-core</artifactId>
                              <version>${jackson.version}</version>
                          </dependency>
                      </dependencies>
                      <build>
                          <plugins>
                              <plugin>
                                  <groupId>org.apache.maven.plugins</groupId>
                                  <artifactId>maven-compiler-plugin</artifactId>
                                  <version>3.8.1</version>
                                  <configuration>
                                      <annotationProcessorPaths>
                                          <path>
                                              <groupId>com.fasterxml.jackson.core</groupId>
                                              <artifactId>jackson-annotations</artifactId>
                                              <version>2.21</version>
                                          </path>
                                      </annotationProcessorPaths>
                                  </configuration>
                              </plugin>
                          </plugins>
                      </build>
                  </project>
                  """
              )
            );
        }

        @Test
        void bumpsPropertyWhenNeighbourPublishesTheNewVersion() {
            rewriteRun(
              spec -> spec.recipe(upgradeAnnotationsTo(SHARED)),
              pomXml(
                """
                  <project>
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-app</artifactId>
                      <version>1</version>
                      <properties>
                          <jackson.version>2.10.5</jackson.version>
                      </properties>
                      <dependencies>
                          <dependency>
                              <groupId>com.fasterxml.jackson.core</groupId>
                              <artifactId>jackson-core</artifactId>
                              <version>${jackson.version}</version>
                          </dependency>
                      </dependencies>
                      <build>
                          <plugins>
                              <plugin>
                                  <groupId>org.apache.maven.plugins</groupId>
                                  <artifactId>maven-compiler-plugin</artifactId>
                                  <version>3.8.1</version>
                                  <configuration>
                                      <annotationProcessorPaths>
                                          <path>
                                              <groupId>com.fasterxml.jackson.core</groupId>
                                              <artifactId>jackson-annotations</artifactId>
                                              <version>${jackson.version}</version>
                                          </path>
                                      </annotationProcessorPaths>
                                  </configuration>
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
                          <jackson.version>2.15.2</jackson.version>
                      </properties>
                      <dependencies>
                          <dependency>
                              <groupId>com.fasterxml.jackson.core</groupId>
                              <artifactId>jackson-core</artifactId>
                              <version>${jackson.version}</version>
                          </dependency>
                      </dependencies>
                      <build>
                          <plugins>
                              <plugin>
                                  <groupId>org.apache.maven.plugins</groupId>
                                  <artifactId>maven-compiler-plugin</artifactId>
                                  <version>3.8.1</version>
                                  <configuration>
                                      <annotationProcessorPaths>
                                          <path>
                                              <groupId>com.fasterxml.jackson.core</groupId>
                                              <artifactId>jackson-annotations</artifactId>
                                              <version>${jackson.version}</version>
                                          </path>
                                      </annotationProcessorPaths>
                                  </configuration>
                              </plugin>
                          </plugins>
                      </build>
                  </project>
                  """
              )
            );
        }
    }

    /**
     * The floor. A gate that fires here is over-correcting no matter how it is implemented, so these
     * must hold on {@code main} and after any fix.
     */
    @Nested
    class NotShared {

        @Test
        void bumpsPropertyWithASingleConsumerEvenToAnUnsharedVersion() {
            rewriteRun(
              spec -> spec.recipe(upgradeAnnotationsTo(UNSHARED)),
              pomXml(
                """
                  <project>
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-app</artifactId>
                      <version>1</version>
                      <properties>
                          <jackson.version>2.10.5</jackson.version>
                      </properties>
                      <dependencies>
                          <dependency>
                              <groupId>com.fasterxml.jackson.core</groupId>
                              <artifactId>jackson-annotations</artifactId>
                              <version>${jackson.version}</version>
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
                          <jackson.version>2.21</jackson.version>
                      </properties>
                      <dependencies>
                          <dependency>
                              <groupId>com.fasterxml.jackson.core</groupId>
                              <artifactId>jackson-annotations</artifactId>
                              <version>${jackson.version}</version>
                          </dependency>
                      </dependencies>
                  </project>
                  """
              )
            );
        }

    }

    /**
     * Property identity is (declaring POM, name), not the name alone. Two modules that happen to use the
     * same property name for different artifact sets must not constrain one another.
     */
    @Nested
    class UnrelatedModules {

        @Test
        void sameNamedPropertyInAnUnrelatedModuleDoesNotBlockTheBump() {
            rewriteRun(
              spec -> spec.recipe(upgradeAnnotationsTo(UNSHARED)),
              mavenProject("clean-module",
                pomXml(
                  """
                    <project>
                        <groupId>com.mycompany</groupId>
                        <artifactId>clean-module</artifactId>
                        <version>1</version>
                        <properties>
                            <jackson.version>2.10.5</jackson.version>
                        </properties>
                        <dependencies>
                            <dependency>
                                <groupId>com.fasterxml.jackson.core</groupId>
                                <artifactId>jackson-annotations</artifactId>
                                <version>${jackson.version}</version>
                            </dependency>
                        </dependencies>
                    </project>
                    """,
                  """
                    <project>
                        <groupId>com.mycompany</groupId>
                        <artifactId>clean-module</artifactId>
                        <version>1</version>
                        <properties>
                            <jackson.version>2.21</jackson.version>
                        </properties>
                        <dependencies>
                            <dependency>
                                <groupId>com.fasterxml.jackson.core</groupId>
                                <artifactId>jackson-annotations</artifactId>
                                <version>${jackson.version}</version>
                            </dependency>
                        </dependencies>
                    </project>
                    """
                )
              ),
              mavenProject("shared-module",
                pomXml(
                  """
                    <project>
                        <groupId>com.mycompany</groupId>
                        <artifactId>shared-module</artifactId>
                        <version>1</version>
                        <properties>
                            <jackson.version>2.10.5</jackson.version>
                        </properties>
                        <dependencies>
                            <dependency>
                                <groupId>com.fasterxml.jackson.core</groupId>
                                <artifactId>jackson-annotations</artifactId>
                                <version>${jackson.version}</version>
                            </dependency>
                            <dependency>
                                <groupId>com.fasterxml.jackson.core</groupId>
                                <artifactId>jackson-core</artifactId>
                                <version>${jackson.version}</version>
                            </dependency>
                        </dependencies>
                    </project>
                    """,
                  """
                    <project>
                        <groupId>com.mycompany</groupId>
                        <artifactId>shared-module</artifactId>
                        <version>1</version>
                        <properties>
                            <jackson.version>2.10.5</jackson.version>
                        </properties>
                        <dependencies>
                            <dependency>
                                <groupId>com.fasterxml.jackson.core</groupId>
                                <artifactId>jackson-annotations</artifactId>
                                <version>2.21</version>
                            </dependency>
                            <dependency>
                                <groupId>com.fasterxml.jackson.core</groupId>
                                <artifactId>jackson-core</artifactId>
                                <version>${jackson.version}</version>
                            </dependency>
                        </dependencies>
                    </project>
                    """
                )
              )
            );
        }
    }

    /**
     * {@code ${project.version}} and its siblings are intentional links to the project's own coordinates,
     * never versions to raise. {@code MavenVisitor#changeChildTagValue} refuses to touch them; a decouple
     * path that writes the version tag directly must refuse too, or it silently severs the link.
     */
    @Nested
    class ImplicitProperty {

        @Test
        void leavesAnImplicitProjectVersionLinkAlone() {
            rewriteRun(
              spec -> spec.recipe(upgradeAnnotationsTo(UNSHARED)),
              pomXml(
                """
                  <project>
                      <groupId>com.fasterxml.jackson.core</groupId>
                      <artifactId>my-app</artifactId>
                      <version>2.10.5</version>
                      <dependencies>
                          <dependency>
                              <groupId>com.fasterxml.jackson.core</groupId>
                              <artifactId>jackson-annotations</artifactId>
                              <version>${project.version}</version>
                          </dependency>
                          <dependency>
                              <groupId>com.fasterxml.jackson.core</groupId>
                              <artifactId>jackson-core</artifactId>
                              <version>${project.version}</version>
                          </dependency>
                      </dependencies>
                  </project>
                  """
              )
            );
        }
    }
}
