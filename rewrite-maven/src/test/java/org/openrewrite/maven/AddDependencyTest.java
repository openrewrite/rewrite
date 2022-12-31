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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.Issue;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

class AddDependencyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
          .classpath("junit-jupiter-api", "guava", "jackson-databind", "jackson-core"));
    }

    @SuppressWarnings("UnstableApiUsage")
    @Language("java")
    private final String usingGuavaIntMath = """
          import com.google.common.math.IntMath;
          public class A {
              boolean getMap() {
                  return IntMath.isPrime(5);
              }
          }
      """;

    @Test
    void dontAddDuplicateIfUpdateModelOnPriorRecipeCycleFailed() {
        rewriteRun(
          spec -> spec
            .recipe(addDependency("doesnotexist:doesnotexist:1", "com.google.common.math.IntMath")),
          mavenProject("project",
            srcMainJava(
              java(usingGuavaIntMath)
            ),
            pomXml(
              """
                    <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                    </project>
                """,
              """
                    <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                        <dependencies>
                            <!--~~(Unable to download POM. Tried repositories:
                    https://repo.maven.apache.org/maven2: HTTP 404)~~>--><dependency>
                                <groupId>doesnotexist</groupId>
                                <artifactId>doesnotexist</artifactId>
                                <version>1</version>
                            </dependency>
                        </dependencies>
                    </project>
                """
            )
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"com.google.common.math.*", "com.google.common.math.IntMath"})
    void onlyIfUsingTestScope(String onlyIfUsing) {
        rewriteRun(
          spec -> spec.recipe(
            addDependency("com.google.guava:guava:29.0-jre", onlyIfUsing)),
          mavenProject("project",
            srcTestJava(
              java(usingGuavaIntMath)
            ),
            pomXml(
              """
                    <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                    </project>
                """,
              """
                    <project>
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
                        </dependencies>
                    </project>
                """
            )
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"com.google.common.math.*", "com.google.common.math.IntMath"})
    void onlyIfUsingCompileScope(String onlyIfUsing) {
        rewriteRun(
          spec -> spec.recipe(addDependency("com.google.guava:guava:29.0-jre", onlyIfUsing)),
          mavenProject("project",
            srcMainJava(
              java(usingGuavaIntMath)
            ),
            pomXml(
              """
                    <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                    </project>
                """,
              """
                    <project>
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
          )
        );
    }

    @Test
    void addDependencyWithClassifier() {
        AddDependency addDep = new AddDependency(
          "io.netty", "netty-tcnative-boringssl-static", "2.0.54.Final", null, "compile", true,
          "com.google.common.math.IntMath", null, "linux-x86_64", false, null
        );
        rewriteRun(
          spec -> spec.recipe(addDep),
          mavenProject(
            "project",
            srcMainJava(
              java(usingGuavaIntMath)
            ),
            pomXml(
              """
                    <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                    </project>
                """,
              """
                    <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                        <dependencies>
                            <dependency>
                                <groupId>io.netty</groupId>
                                <artifactId>netty-tcnative-boringssl-static</artifactId>
                                <version>2.0.54.Final</version>
                                <classifier>linux-x86_64</classifier>
                            </dependency>
                        </dependencies>
                    </project>
                """
            )
          )
        );
    }

    @Test
    void notUsingType() {
        rewriteRun(
          spec -> spec.recipe(addDependency(
            "com.google.guava:guava:29.0-jre",
            "com.google.common.collect.ImmutableMap"
          )),
          mavenProject(
            "project",
            srcMainJava(
              java(usingGuavaIntMath)
            ),
            pomXml(
              """
                    <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                    </project>
                """
            )
          )
        );
    }

    @Test
    void addInOrder() {
        rewriteRun(
          spec -> spec.recipe(addDependency("com.google.guava:guava:29.0-jre", "com.google.common.math.IntMath")),
          mavenProject(
            "project",
            srcMainJava(
              java(usingGuavaIntMath)
            ),
            pomXml(
              """
                    <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                        <dependencies>
                            <dependency>
                                <groupId>commons-lang</groupId>
                                <artifactId>commons-lang</artifactId>
                                <version>1.0</version>
                            </dependency>
                        </dependencies>
                    </project>
                """,
              """
                    <project>
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
                                <groupId>commons-lang</groupId>
                                <artifactId>commons-lang</artifactId>
                                <version>1.0</version>
                            </dependency>
                        </dependencies>
                    </project>
                """
            )
          )
        );
    }

    @Test
    void doNotAddBecauseAlreadyTransitive() {
        rewriteRun(
          spec -> spec.recipe(addDependency("org.junit.jupiter:junit-jupiter-api:5.x", "org.junit.jupiter.api.*")),
          mavenProject(
            "project",
            srcTestJava(
              java(
                """
                      class MyTest {
                          @org.junit.jupiter.api.Test
                          void test() {}
                      }
                  """
              )
            ),
            pomXml(
              """
                    <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                        <dependencies>
                            <dependency>
                                <groupId>org.junit.jupiter</groupId>
                                <artifactId>junit-jupiter-engine</artifactId>
                                <version>5.7.1</version>
                            </dependency>
                        </dependencies>
                    </project>
                """
            )
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"com.google.common.math.*", "com.google.common.math.IntMath"})
    void semverSelector(String onlyIfUsing) {
        rewriteRun(
          spec -> spec.recipe(new AddDependency(
            "com.google.guava", "guava", "29.x", "-jre",
            null, false, onlyIfUsing, null, null, false, null
          )),
          mavenProject(
            "project",
            srcMainJava(
              java(usingGuavaIntMath)
            ),
            pomXml(
              """
                    <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                    </project>
                """,
              """
                    <project>
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
          )
        );
    }

    @Test
    void addTestDependenciesAfterCompile() {
        rewriteRun(
          spec -> spec.recipe(addDependency("com.google.guava:guava:29.0-jre", "com.google.common.math.IntMath")),
          mavenProject(
            "project",
            srcTestJava(
              java(usingGuavaIntMath)
            ),
            pomXml(
              """
                    <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                        <dependencies>
                            <dependency>
                                <groupId>commons-lang</groupId>
                                <artifactId>commons-lang</artifactId>
                                <version>1.0</version>
                            </dependency>
                        </dependencies>
                    </project>
                """,
              """
                    <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                        <dependencies>
                            <dependency>
                                <groupId>commons-lang</groupId>
                                <artifactId>commons-lang</artifactId>
                                <version>1.0</version>
                            </dependency>
                            <dependency>
                                <groupId>com.google.guava</groupId>
                                <artifactId>guava</artifactId>
                                <version>29.0-jre</version>
                                <scope>test</scope>
                            </dependency>
                        </dependencies>
                    </project>
                """
            )
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1392")
    @Test
    void preserveNonTagContent() {
        rewriteRun(
          spec -> spec.recipe(addDependency("com.google.guava:guava:29.0-jre", "com.google.common.math.IntMath")),
          mavenProject(
            "project",
            srcTestJava(
              java(usingGuavaIntMath)
            ),
            pomXml(
              """
                    <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                        <!-- comment 1 -->
                        <?processing instruction1?>
                        <dependencies>
                            <!-- comment 2 -->
                            <?processing instruction2?>
                            <dependency>
                                <groupId>commons-lang</groupId>
                                <artifactId>commons-lang</artifactId>
                                <version>1.0</version>
                            </dependency>
                            <!-- comment 3 -->
                            <?processing instruction3?>
                        </dependencies>
                    </project>
                """,
              """
                    <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                        <!-- comment 1 -->
                        <?processing instruction1?>
                        <dependencies>
                            <!-- comment 2 -->
                            <?processing instruction2?>
                            <dependency>
                                <groupId>commons-lang</groupId>
                                <artifactId>commons-lang</artifactId>
                                <version>1.0</version>
                            </dependency>
                            <dependency>
                                <groupId>com.google.guava</groupId>
                                <artifactId>guava</artifactId>
                                <version>29.0-jre</version>
                                <scope>test</scope>
                            </dependency>
                            <!-- comment 3 -->
                            <?processing instruction3?>
                        </dependencies>
                    </project>
                """
            )
          )
        );
    }

    @Test
    void addDependencyDoesntAddWhenExistingDependency() {
        rewriteRun(
          spec -> spec.recipe(addDependency("com.google.guava:guava:29.0-jre", "com.google.common.math.IntMath")),
          mavenProject(
            "project",
            srcMainJava(
              java(usingGuavaIntMath)
            ),
            pomXml(
              """
                    <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                        <dependencies>
                            <dependency>
                                <groupId>com.google.guava</groupId>
                                <artifactId>guava</artifactId>
                                <version>28.0-jre</version>
                            </dependency>
                        </dependencies>
                    </project>
                """
            )
          )
        );
    }

    @Test
    void useManaged() {
        rewriteRun(
          spec -> spec.recipe(addDependency("com.google.guava:guava:29.0-jre", "com.google.common.math.IntMath")),
          pomXml(
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-parent</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>com.google.guava</groupId>
                              <artifactId>guava</artifactId>
                              <version>28.0-jre</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          ),
          mavenProject(
            "server",
            srcMainJava(
              java(usingGuavaIntMath)
            ),
            pomXml(
              """
                    <project>
                        <parent>
                            <groupId>com.mycompany.app</groupId>
                            <artifactId>my-parent</artifactId>
                            <version>1</version>
                        </parent>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                    </project>
                """,
              """
                    <project>
                        <parent>
                            <groupId>com.mycompany.app</groupId>
                            <artifactId>my-parent</artifactId>
                            <version>1</version>
                        </parent>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                        <dependencies>
                            <dependency>
                                <groupId>com.google.guava</groupId>
                                <artifactId>guava</artifactId>
                            </dependency>
                        </dependencies>
                    </project>
                """
            )
          )
        );
    }

    @Test
    void useRequestedVersionInUseByOtherMembersOfTheFamily() {
        rewriteRun(
          spec -> spec.recipe(new AddDependency(
            "com.fasterxml.jackson.module", "jackson-module-afterburner", "2.10.5",
            null, null, false, "com.fasterxml.jackson.databind.*",
            null, null, null, "com.fasterxml.*"
          )),
          mavenProject(
            "project",
            srcMainJava(
              java(
                """
                      public class A {
                          com.fasterxml.jackson.databind.ObjectMapper mapper;
                      }
                  """
              )
            ),
            pomXml(
              """
                    <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                        <properties>
                            <jackson.version>2.12.0</jackson.version>
                        </properties>
                        <dependencies>
                            <dependency>
                                <groupId>com.fasterxml.jackson.core</groupId>
                                <artifactId>jackson-databind</artifactId>
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
                            <jackson.version>2.12.0</jackson.version>
                        </properties>
                        <dependencies>
                            <dependency>
                                <groupId>com.fasterxml.jackson.core</groupId>
                                <artifactId>jackson-databind</artifactId>
                                <version>${jackson.version}</version>
                            </dependency>
                            <dependency>
                                <groupId>com.fasterxml.jackson.module</groupId>
                                <artifactId>jackson-module-afterburner</artifactId>
                                <version>${jackson.version}</version>
                            </dependency>
                        </dependencies>
                    </project>
                """
            )
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1443")
    @Test
    void addTransitiveDependencyAsDirect() {
        rewriteRun(
          spec -> spec.recipe(addDependency(
            "com.fasterxml.jackson.core:jackson-core:2.12.0",
            "com.fasterxml.jackson.core.*"
          )),
          mavenProject(
            "project",
            srcMainJava(
              java(
                """
                      public class A {
                          com.fasterxml.jackson.core.Versioned v;
                      }
                  """
              )
            ),
            pomXml(
              """
                    <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                        <dependencies>
                            <dependency>
                                <groupId>com.fasterxml.jackson.core</groupId>
                                <artifactId>jackson-databind</artifactId>
                                <version>2.12.0</version>
                            </dependency>
                        </dependencies>
                    </project>
                """,
              """
                    <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                        <dependencies>
                            <dependency>
                                <groupId>com.fasterxml.jackson.core</groupId>
                                <artifactId>jackson-core</artifactId>
                                <version>2.12.0</version>
                            </dependency>
                            <dependency>
                                <groupId>com.fasterxml.jackson.core</groupId>
                                <artifactId>jackson-databind</artifactId>
                                <version>2.12.0</version>
                            </dependency>
                        </dependencies>
                    </project>
                """
            )
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2255")
    @ParameterizedTest
    @ValueSource(strings = {"provided", "runtime", "test"})
    void addScopedDependency(String scope) {
        rewriteRun(
          spec -> spec.recipe(addDependency(
            "com.fasterxml.jackson.core:jackson-core:2.12.0",
            "com.fasterxml.jackson.core.*",
            scope
          )),
          mavenProject(
            "project",
            srcMainJava(
              java(
                """
                      public class A {
                          com.fasterxml.jackson.core.Versioned v;
                      }
                  """
              )
            ),
            pomXml(
              """
                    <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                    </project>
                """,
              """
                    <project>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>my-app</artifactId>
                        <version>1</version>
                        <dependencies>
                            <dependency>
                                <groupId>com.fasterxml.jackson.core</groupId>
                                <artifactId>jackson-core</artifactId>
                                <version>2.12.0</version>
                                <scope>%s</scope>
                            </dependency>
                        </dependencies>
                    </project>
                """.formatted(scope)
            )
          )
        );
    }

    @Test
    void addDependencyToProjectsThatNeedIt() {
        rewriteRun(
          spec -> spec.recipe(addDependency("com.google.guava:guava:29.0-jre", "com.google.common.math.IntMath", "compile")),
          mavenProject("root",
            pomXml(
              """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>root</artifactId>
                    <version>1</version>
                    <modules>
                        <module>project1</module>
                        <module>project2</module>
                    </modules>
                </project>
                """
            )
          ),
          mavenProject("project1",
            srcMainJava(
              java(usingGuavaIntMath)
            ),
            pomXml(
              """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>project1</artifactId>
                    <version>1</version>
                    <parent>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>root</artifactId>
                        <version>1</version>
                    </parent>
                </project>
                """,
              """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>project1</artifactId>
                    <version>1</version>
                    <parent>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>root</artifactId>
                        <version>1</version>
                    </parent>
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
          ),
          mavenProject("project2",
            pomXml(
              """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>project2</artifactId>
                    <version>1</version>
                    <parent>
                        <groupId>com.mycompany.app</groupId>
                        <artifactId>root</artifactId>
                        <version>1</version>
                    </parent>
                </project>
                """
            )
          )
        );
    }

    private AddDependency addDependency(String gav, String onlyIfUsing) {
        return addDependency(gav, onlyIfUsing, null);
    }

    private AddDependency addDependency(String gav, String onlyIfUsing, @Nullable String scope) {
        String[] gavParts = gav.split(":");
        return new AddDependency(
          gavParts[0], gavParts[1], gavParts[2], null, scope, true,
          onlyIfUsing, null, null, false, null
        );
    }
}
