/*
 * Copyright 2023 the original author or authors.
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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.Assertions.pomXml;

class DoesNotIncludeDependencyTest implements RewriteTest {

    @Nested
    class CheckTransitive {

        @Test
        void dependencyPresentNotMarked() {
            rewriteRun(
              spec -> spec.recipe(new DoesNotIncludeDependency("org.springframework", "spring-beans", null, null)),
              pomXml(
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                      <dependency>
                        <groupId>org.springframework</groupId>
                        <artifactId>spring-beans</artifactId>
                        <version>6.0.0</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """
              )
            );
        }

        @Test
        void dependencyPresentTransitivelyNotMarked() {
            rewriteRun(
              spec -> spec.recipe(new DoesNotIncludeDependency("org.springframework", "spring-beans", null, null)),
              pomXml(
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                      <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-actuator</artifactId>
                        <version>3.0.0</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """
              )
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {"test", "provided", "compile", "runtime"})
        void dependencyPresentTransitivelyWithSpecificScopeNotMarked(String scope) {
            rewriteRun(
              spec -> spec.recipe(new DoesNotIncludeDependency("org.springframework", "spring-beans", null, null)),
              pomXml(
                String.format("""
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                      <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-actuator</artifactId>
                        <version>3.0.0</version>
                        <scope>%s</scope>
                      </dependency>
                    </dependencies>
                  </project>
                  """, scope)
              )
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {"test", "provided", "compile", "runtime"})
        void dependencyPresentWithSpecificScopeNotMarked(String scope) {
            rewriteRun(
              spec -> spec.recipe(new DoesNotIncludeDependency("org.springframework", "spring-beans", null, null)),
              pomXml(
                String.format("""
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                      <dependency>
                        <groupId>org.springframework</groupId>
                        <artifactId>spring-beans</artifactId>
                        <version>6.0.0</version>
                        <scope>%s</scope>
                      </dependency>
                    </dependencies>
                  </project>
                  """, scope)
              )
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {"test", "provided"})
        void dependencyPresentButNotInSpecifiedCompileScopeMarked(String scope) {
            rewriteRun(
              spec -> spec.recipe((new DoesNotIncludeDependency("org.springframework", "spring-beans", null, "compile"))),
              pomXml(
                String.format("""
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                      <dependency>
                        <groupId>org.springframework</groupId>
                        <artifactId>spring-beans</artifactId>
                        <version>6.0.0</version>
                        <scope>%s</scope>
                      </dependency>
                    </dependencies>
                  </project>
                  """, scope),
                String.format("""
                  <!--~~>--><project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                      <dependency>
                        <groupId>org.springframework</groupId>
                        <artifactId>spring-beans</artifactId>
                        <version>6.0.0</version>
                        <scope>%s</scope>
                      </dependency>
                    </dependencies>
                  </project>
                  """, scope)
              )
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {"compile", "runtime"})
        void dependencyPresentSpecifiedCompileScopeNotMarked(String scope) {
            rewriteRun(
              spec -> spec.recipe(new DoesNotIncludeDependency("org.springframework", "spring-beans", null, "compile")),
              pomXml(
                String.format("""
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                      <dependency>
                        <groupId>org.springframework</groupId>
                        <artifactId>spring-beans</artifactId>
                        <version>6.0.0</version>
                        <scope>%s</scope>
                      </dependency>
                    </dependencies>
                  </project>
                  """, scope)
              )
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {"provided"})
        void dependencyPresentButNotInSpecifiedTestScopeMarked(String scope) {
            rewriteRun(
              spec -> spec.recipe(new DoesNotIncludeDependency("org.springframework", "spring-beans", null, "test")),
              pomXml(
                String.format("""
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                      <dependency>
                        <groupId>org.springframework</groupId>
                        <artifactId>spring-beans</artifactId>
                        <version>6.0.0</version>
                        <scope>%s</scope>
                      </dependency>
                    </dependencies>
                  </project>
                  """, scope),
                String.format("""
                  <!--~~>--><project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                      <dependency>
                        <groupId>org.springframework</groupId>
                        <artifactId>spring-beans</artifactId>
                        <version>6.0.0</version>
                        <scope>%s</scope>
                      </dependency>
                    </dependencies>
                  </project>
                  """, scope)
              )
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {"compile", "runtime", "test"})
        void dependencyPresentSpecifiedTestScopeNotMarked(String scope) {
            rewriteRun(
              spec -> spec.recipe(new DoesNotIncludeDependency("org.springframework", "spring-beans", null, "test")),
              pomXml(
                String.format("""
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                      <dependency>
                        <groupId>org.springframework</groupId>
                        <artifactId>spring-beans</artifactId>
                        <version>6.0.0</version>
                        <scope>%s</scope>
                      </dependency>
                    </dependencies>
                  </project>
                  """, scope)
              )
            );
        }
    }

    @Nested
    class DontCheckTransitive {

        @DocumentExample
        @Test
        void dependencyPresentTransitivelyMarked() {
            rewriteRun(
              spec -> spec.recipe(new DoesNotIncludeDependency("org.springframework", "spring-beans", true, null)),
              pomXml(
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                      <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-actuator</artifactId>
                        <version>3.0.0</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """,
                """
                  <!--~~>--><project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                      <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-actuator</artifactId>
                        <version>3.0.0</version>
                      </dependency>
                    </dependencies>
                  </project>
                  """
              )
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {"test", "provided", "compile", "runtime"})
        void dependencyPresentSpecificScopeNotMarked(String scope) {
            rewriteRun(
              spec -> spec.recipe(new DoesNotIncludeDependency("org.springframework", "spring-beans", true, null)),
              pomXml(
                String.format("""
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                      <dependency>
                        <groupId>org.springframework</groupId>
                        <artifactId>spring-beans</artifactId>
                        <version>6.0.0</version>
                        <scope>%s</scope>
                      </dependency>
                    </dependencies>
                  </project>
                  """, scope)
              )
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {"test", "provided"})
        void dependencyPresentButNotInSpecifiedCompileScopeMarked(String scope) {
            rewriteRun(
              spec -> spec.recipe(new DoesNotIncludeDependency("org.springframework", "spring-beans", true, "compile")),
              pomXml(
                String.format("""
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                      <dependency>
                        <groupId>org.springframework</groupId>
                        <artifactId>spring-beans</artifactId>
                        <version>6.0.0</version>
                        <scope>%s</scope>
                      </dependency>
                    </dependencies>
                  </project>
                  """, scope),
                String.format("""
                  <!--~~>--><project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                      <dependency>
                        <groupId>org.springframework</groupId>
                        <artifactId>spring-beans</artifactId>
                        <version>6.0.0</version>
                        <scope>%s</scope>
                      </dependency>
                    </dependencies>
                  </project>
                  """, scope)
              )
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {"compile", "runtime"})
        void dependencyPresentSpecifiedCompileScopeNotMarked(String scope) {
            rewriteRun(
              spec -> spec.recipe(new DoesNotIncludeDependency("org.springframework", "spring-beans", true, "compile")),
              pomXml(
                String.format("""
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                      <dependency>
                        <groupId>org.springframework</groupId>
                        <artifactId>spring-beans</artifactId>
                        <version>6.0.0</version>
                        <scope>%s</scope>
                      </dependency>
                    </dependencies>
                  </project>
                  """, scope)
              )
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {"provided"})
        void dependencyPresentButNotInSpecifiedTestScopeMarked(String scope) {
            rewriteRun(
              spec -> spec.recipe(new DoesNotIncludeDependency("org.springframework", "spring-beans", true, "test")),
              pomXml(
                String.format("""
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                      <dependency>
                        <groupId>org.springframework</groupId>
                        <artifactId>spring-beans</artifactId>
                        <version>6.0.0</version>
                        <scope>%s</scope>
                      </dependency>
                    </dependencies>
                  </project>
                  """, scope),
                String.format("""
                  <!--~~>--><project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                      <dependency>
                        <groupId>org.springframework</groupId>
                        <artifactId>spring-beans</artifactId>
                        <version>6.0.0</version>
                        <scope>%s</scope>
                      </dependency>
                    </dependencies>
                  </project>
                  """, scope)
              )
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {"compile", "runtime", "test"})
        void dependencyPresentSpecifiedTestScopeNotMarked(String scope) {
            rewriteRun(
              spec -> spec.recipe(new DoesNotIncludeDependency("org.springframework", "spring-beans", true, "test")),
              pomXml(
                String.format("""
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                      <dependency>
                        <groupId>org.springframework</groupId>
                        <artifactId>spring-beans</artifactId>
                        <version>6.0.0</version>
                        <scope>%s</scope>
                      </dependency>
                    </dependencies>
                  </project>
                  """, scope)
              )
            );
        }
    }

    @Test
    void multimoduleMarksOnlyCorrectModule() {
        rewriteRun(
          spec -> spec.recipe(new DoesNotIncludeDependency("org.springframework", "spring-beans", null, null)),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>a</artifactId>
                <version>1.0.0</version>
                <dependencies>
                  <dependency>
                    <groupId>org.springframework</groupId>
                    <artifactId>spring-beans</artifactId>
                    <version>6.0.0</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            spec -> spec.path("a/pom.xml")
              .afterRecipe(doc -> assertThat(doc.getMarkers().getMarkers()).noneMatch(marker -> marker instanceof SearchResult))
          ),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>b</artifactId>
                <version>1.0.0</version>
              </project>
              """,
            """
              <!--~~>--><project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>b</artifactId>
                <version>1.0.0</version>
              </project>
              """,
            spec -> spec.path("b/pom.xml")
          )
        );
    }

    @Test
    void dependencyNotPresentMarked() {
        rewriteRun(
          spec -> spec.recipe(new DoesNotIncludeDependency("org.springframework", "spring-beans", null, null)),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
              </project>
              """,
            """
              <!--~~>--><project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
              </project>
              """
          )
        );
    }
}
