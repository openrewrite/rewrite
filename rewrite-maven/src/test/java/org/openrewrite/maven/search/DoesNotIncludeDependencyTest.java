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

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.Recipe;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class DoesNotIncludeDependencyTest implements RewriteTest {
    private static final String marker = "<!--~~>-->";
    @Language("xml")
    private static final String missingDependencyTemplate = """
      <project>
        <modelVersion>4.0.0</modelVersion>
        <groupId>org.sample</groupId>
        <artifactId>b</artifactId>
        <version>1.0.0</version>
      </project>
      """;

    private Recipe defaultRecipeWithOnlyDirectAndScope(@Nullable Boolean onlyDirect, @Nullable String scope) {
        return new DoesNotIncludeDependency("org.springframework", "spring-beans", onlyDirect, scope);
    }

    private String wrappedScope(String scope) {
        return "\n<scope>" + scope + "</scope>";
    }

    @Nested
    class DirectDependencyPresent {
        //language=xml
        private static final String directDependencyTemplate = """
          <project>
            <modelVersion>4.0.0</modelVersion>
            <groupId>org.sample</groupId>
            <artifactId>sample</artifactId>
            <version>1.0.0</version>
            <dependencies>
              <dependency>
                <groupId>org.springframework</groupId>
                <artifactId>spring-beans</artifactId>
                <version>6.0.0</version>%s
              </dependency>
            </dependencies>
          </project>
          """;

        @Nested
        class OnlyDirectNull {
            @Test
            void withoutScopeOrDesiredScopeSpecifiedNotMarked() {
                rewriteRun(
                  spec -> spec.recipe(defaultRecipeWithOnlyDirectAndScope(null, null)),
                  pomXml(String.format(directDependencyTemplate, ""))
                );
            }

            @ParameterizedTest
            @ValueSource(strings = {"test", "provided", "compile", "runtime"})
            void withScopeButDesiredScopeNotSpecifiedNotMarked(String scope) {
                rewriteRun(
                  spec -> spec.recipe(defaultRecipeWithOnlyDirectAndScope(null, null)),
                  pomXml(String.format(directDependencyTemplate, wrappedScope(scope)))
                );
            }

            @ParameterizedTest
            @CsvSource({"test,compile"})
            @CsvSource({"provided,compile", "provided,test"})
            void withScopeNotInDesiredScopeMarked(String existingScope, String desiredScope) {
                rewriteRun(
                  spec -> spec.recipe(defaultRecipeWithOnlyDirectAndScope(null, desiredScope)),
                  pomXml(
                    String.format(directDependencyTemplate, wrappedScope(existingScope)),
                    String.format(marker + directDependencyTemplate, wrappedScope(existingScope))
                  )
                );
            }

            @ParameterizedTest
            @CsvSource({"compile,compile", "compile,test"})
            @CsvSource({"runtime,compile", "runtime,test",})
            @CsvSource({"test,test"})
            void withScopeInDesiredScopeNotMarked(String existingScope, String desiredScope) {
                rewriteRun(
                  spec -> spec.recipe(defaultRecipeWithOnlyDirectAndScope(null, desiredScope)),
                  pomXml(String.format(directDependencyTemplate, wrappedScope(existingScope)))
                );
            }

            @Test
            void multimoduleMarksOnlyCorrectModule() {
                rewriteRun(
                  spec -> spec.recipe(defaultRecipeWithOnlyDirectAndScope(null, null)),
                  pomXml(
                    String.format(directDependencyTemplate, ""),
                    spec -> spec.path("a/pom.xml")
                  ),
                  pomXml(
                    missingDependencyTemplate,
                    marker + missingDependencyTemplate,
                    spec -> spec.path("b/pom.xml")
                  ),
                  pomXml(
                    missingDependencyTemplate,
                    marker + missingDependencyTemplate
                  )
                );
            }
        }

        @Nested
        class OnlyDirectTrue {
            @Test
            void withoutScopeOrDesiredScopeSpecifiedNotMarked() {
                rewriteRun(
                  spec -> spec.recipe(defaultRecipeWithOnlyDirectAndScope(true, null)),
                  pomXml(String.format(directDependencyTemplate, ""))
                );
            }

            @ParameterizedTest
            @ValueSource(strings = {"test", "provided", "compile", "runtime"})
            void withScopeButDesiredScopeNotSpecifiedNotMarked(String scope) {
                rewriteRun(
                  spec -> spec.recipe(defaultRecipeWithOnlyDirectAndScope(true, null)),
                  pomXml(String.format(directDependencyTemplate, wrappedScope(scope)))
                );
            }

            @ParameterizedTest
            @CsvSource({"test,compile"})
            @CsvSource({"provided,compile", "provided,test"})
            void withScopeNotInDesiredScopeMarked(String existingScope, String desiredScope) {
                rewriteRun(
                  spec -> spec.recipe(defaultRecipeWithOnlyDirectAndScope(true, desiredScope)),
                  pomXml(
                    String.format(directDependencyTemplate, wrappedScope(existingScope)),
                    String.format(marker + directDependencyTemplate, wrappedScope(existingScope))
                  )
                );
            }

            @ParameterizedTest
            @CsvSource({"compile,compile", "compile,test"})
            @CsvSource({"runtime,compile", "runtime,test",})
            @CsvSource({"test,test"})
            void withScopeInDesiredScopeNotMarked(String existingScope, String desiredScope) {
                rewriteRun(
                  spec -> spec.recipe(defaultRecipeWithOnlyDirectAndScope(true, desiredScope)),
                  pomXml(String.format(directDependencyTemplate, wrappedScope(existingScope)))
                );
            }

            @Test
            void multimoduleMarksOnlyCorrectModule() {
                rewriteRun(
                  spec -> spec.recipe(defaultRecipeWithOnlyDirectAndScope(true, null)),
                  pomXml(
                    String.format(directDependencyTemplate, ""),
                    spec -> spec.path("a/pom.xml")
                  ),
                  pomXml(
                    missingDependencyTemplate,
                    marker + missingDependencyTemplate,
                    spec -> spec.path("b/pom.xml")
                  ),
                  pomXml(
                    missingDependencyTemplate,
                    marker + missingDependencyTemplate
                  )
                );
            }
        }

        @Nested
        class OnlyDirectFalse {
            @Test
            void withoutScopeOrDesiredScopeSpecifiedNotMarked() {
                rewriteRun(
                  spec -> spec.recipe(defaultRecipeWithOnlyDirectAndScope(false, null)),
                  pomXml(String.format(directDependencyTemplate, ""))
                );
            }

            @ParameterizedTest
            @ValueSource(strings = {"test", "provided", "compile", "runtime"})
            void withScopeButDesiredScopeNotSpecifiedNotMarked(String scope) {
                rewriteRun(
                  spec -> spec.recipe(defaultRecipeWithOnlyDirectAndScope(false, null)),
                  pomXml(String.format(directDependencyTemplate, wrappedScope(scope)))
                );
            }

            @ParameterizedTest
            @CsvSource({"test,compile"})
            @CsvSource({"provided,compile", "provided,test"})
            void withScopeNotInDesiredScopeMarked(String existingScope, String desiredScope) {
                rewriteRun(
                  spec -> spec.recipe(defaultRecipeWithOnlyDirectAndScope(false, desiredScope)),
                  pomXml(
                    String.format(directDependencyTemplate, wrappedScope(existingScope)),
                    String.format(marker + directDependencyTemplate, wrappedScope(existingScope))
                  )
                );
            }

            @ParameterizedTest
            @CsvSource({"compile,compile", "compile,test"})
            @CsvSource({"runtime,compile", "runtime,test",})
            @CsvSource({"test,test"})
            void withScopeInDesiredScopeNotMarked(String existingScope, String desiredScope) {
                rewriteRun(
                  spec -> spec.recipe(defaultRecipeWithOnlyDirectAndScope(false, desiredScope)),
                  pomXml(String.format(directDependencyTemplate, wrappedScope(existingScope)))
                );
            }

            @Test
            void multimoduleMarksOnlyCorrectModule() {
                rewriteRun(
                  spec -> spec.recipe(defaultRecipeWithOnlyDirectAndScope(false, null)),
                  pomXml(
                    String.format(directDependencyTemplate, ""),
                    spec -> spec.path("a/pom.xml")
                  ),
                  pomXml(
                    missingDependencyTemplate,
                    marker + missingDependencyTemplate,
                    spec -> spec.path("b/pom.xml")
                  ),
                  pomXml(
                    missingDependencyTemplate,
                    marker + missingDependencyTemplate
                  )
                );
            }
        }
    }

    @Nested
    class TransitiveDependencyPresent {
        //language=xml
        private static final String transitiveDependencyTemplate = """
          <project>
            <modelVersion>4.0.0</modelVersion>
            <groupId>org.sample</groupId>
            <artifactId>sample</artifactId>
            <version>1.0.0</version>
            <dependencies>
              <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-actuator</artifactId>
                <version>3.0.0</version>%s
              </dependency>
            </dependencies>
          </project>
          """;

        @Nested
        class OnlyDirectNull {
            @Test
            void withoutScopeOrDesiredScopeSpecifiedNotMarked() {
                rewriteRun(
                  spec -> spec.recipe(defaultRecipeWithOnlyDirectAndScope(null, null)),
                  pomXml(String.format(transitiveDependencyTemplate, ""))
                );
            }

            @ParameterizedTest
            @ValueSource(strings = {"test", "provided", "compile", "runtime"})
            void withScopeButDesiredScopeNotSpecifiedNotMarked(String scope) {
                rewriteRun(
                  spec -> spec.recipe(defaultRecipeWithOnlyDirectAndScope(null, null)),
                  pomXml(String.format(transitiveDependencyTemplate, wrappedScope(scope)))
                );
            }

            @ParameterizedTest
            @CsvSource({"test,compile"})
            @CsvSource({"provided,compile", "provided,test"})
            void withScopeNotInDesiredScopeMarked(String existingScope, String desiredScope) {
                rewriteRun(
                  spec -> spec.recipe(defaultRecipeWithOnlyDirectAndScope(null, desiredScope)),
                  pomXml(
                    String.format(transitiveDependencyTemplate, wrappedScope(existingScope)),
                    String.format(marker + transitiveDependencyTemplate, wrappedScope(existingScope))
                  )
                );
            }

            @ParameterizedTest
            @CsvSource({"compile,compile", "compile,test"})
            @CsvSource({"runtime,compile", "runtime,test",})
            @CsvSource({"test,test"})
            void withScopeInDesiredScopeNotMarked(String existingScope, String desiredScope) {
                rewriteRun(
                  spec -> spec.recipe(defaultRecipeWithOnlyDirectAndScope(null, desiredScope)),
                  pomXml(String.format(transitiveDependencyTemplate, wrappedScope(existingScope)))
                );
            }

            @Test
            void multimoduleMarksOnlyCorrectModule() {
                rewriteRun(
                  spec -> spec.recipe(defaultRecipeWithOnlyDirectAndScope(null, null)),
                  pomXml(
                    String.format(transitiveDependencyTemplate, ""),
                    spec -> spec.path("a/pom.xml")
                  ),
                  pomXml(
                    missingDependencyTemplate,
                    marker + missingDependencyTemplate,
                    spec -> spec.path("b/pom.xml")
                  ),
                  pomXml(
                    missingDependencyTemplate,
                    marker + missingDependencyTemplate
                  )
                );
            }
        }

        @Nested
        class OnlyDirectTrue {
            @DocumentExample
            @Test
            void withoutScopeOrDesiredScopeSpecifiedMarked() {
                rewriteRun(
                  spec -> spec.recipe(defaultRecipeWithOnlyDirectAndScope(true, null)),
                  pomXml(
                    String.format(transitiveDependencyTemplate, ""),
                    String.format(marker + transitiveDependencyTemplate, "")
                  )
                );
            }

            @ParameterizedTest
            @ValueSource(strings = {"test", "provided", "compile", "runtime"})
            void withScopeButDesiredScopeNotSpecifiedMarked(String scope) {
                rewriteRun(
                  spec -> spec.recipe(defaultRecipeWithOnlyDirectAndScope(true, null)),
                  pomXml(
                    String.format(transitiveDependencyTemplate, wrappedScope(scope)),
                    String.format(marker + transitiveDependencyTemplate, wrappedScope(scope))
                  )
                );
            }

            @ParameterizedTest
            @CsvSource({"test,compile"})
            @CsvSource({"provided,compile", "provided,test"})
            void withScopeNotInDesiredScopeMarked(String existingScope, String desiredScope) {
                rewriteRun(
                  spec -> spec.recipe(defaultRecipeWithOnlyDirectAndScope(true, desiredScope)),
                  pomXml(
                    String.format(transitiveDependencyTemplate, wrappedScope(existingScope)),
                    String.format(marker + transitiveDependencyTemplate, wrappedScope(existingScope))
                  )
                );
            }

            @ParameterizedTest
            @CsvSource({"compile,compile", "compile,test"})
            @CsvSource({"runtime,compile", "runtime,test",})
            @CsvSource({"test,test"})
            void withScopeInDesiredScopeMarked(String existingScope, String desiredScope) {
                rewriteRun(
                  spec -> spec.recipe(defaultRecipeWithOnlyDirectAndScope(true, desiredScope)),
                  pomXml(
                    String.format(transitiveDependencyTemplate, wrappedScope(existingScope)),
                    String.format(marker + transitiveDependencyTemplate, wrappedScope(existingScope))
                  )
                );
            }

            @Test
            void multimoduleMarksOnlyCorrectModule() {
                rewriteRun(
                  spec -> spec.recipe(defaultRecipeWithOnlyDirectAndScope(true, null)),
                  pomXml(
                    String.format(transitiveDependencyTemplate, ""),
                    spec -> spec.path("a/pom.xml")
                  ),
                  pomXml(
                    missingDependencyTemplate,
                    marker + missingDependencyTemplate,
                    spec -> spec.path("b/pom.xml")
                  ),
                  pomXml(
                    missingDependencyTemplate,
                    marker + missingDependencyTemplate
                  )
                );
            }
        }

        @Nested
        class OnlyDirectFalse {
            @Test
            void withoutScopeOrDesiredScopeSpecifiedNotMarked() {
                rewriteRun(
                  spec -> spec.recipe(defaultRecipeWithOnlyDirectAndScope(false, null)),
                  pomXml(String.format(transitiveDependencyTemplate, ""))
                );
            }

            @ParameterizedTest
            @ValueSource(strings = {"test", "provided", "compile", "runtime"})
            void withScopeButDesiredScopeNotSpecifiedNotMarked(String scope) {
                rewriteRun(
                  spec -> spec.recipe(defaultRecipeWithOnlyDirectAndScope(false, null)),
                  pomXml(String.format(transitiveDependencyTemplate, wrappedScope(scope)))
                );
            }

            @ParameterizedTest
            @CsvSource({"test,compile"})
            @CsvSource({"provided,compile", "provided,test"})
            void withScopeNotInDesiredScopeMarked(String existingScope, String desiredScope) {
                rewriteRun(
                  spec -> spec.recipe(defaultRecipeWithOnlyDirectAndScope(false, desiredScope)),
                  pomXml(
                    String.format(transitiveDependencyTemplate, wrappedScope(existingScope)),
                    String.format(marker + transitiveDependencyTemplate, wrappedScope(existingScope))
                  )
                );
            }

            @ParameterizedTest
            @CsvSource({"compile,compile", "compile,test"})
            @CsvSource({"runtime,compile", "runtime,test",})
            @CsvSource({"test,test"})
            void withScopeInDesiredScopeNotMarked(String existingScope, String desiredScope) {
                rewriteRun(
                  spec -> spec.recipe(defaultRecipeWithOnlyDirectAndScope(false, desiredScope)),
                  pomXml(String.format(transitiveDependencyTemplate, wrappedScope(existingScope)))
                );
            }

            @Test
            void multimoduleMarksOnlyCorrectModule() {
                rewriteRun(
                  spec -> spec.recipe(defaultRecipeWithOnlyDirectAndScope(false, null)),
                  pomXml(
                    String.format(transitiveDependencyTemplate, ""),
                    spec -> spec.path("a/pom.xml")
                  ),
                  pomXml(
                    missingDependencyTemplate,
                    marker + missingDependencyTemplate,
                    spec -> spec.path("b/pom.xml")
                  ),
                  pomXml(
                    missingDependencyTemplate,
                    marker + missingDependencyTemplate
                  )
                );
            }
        }
    }
}
