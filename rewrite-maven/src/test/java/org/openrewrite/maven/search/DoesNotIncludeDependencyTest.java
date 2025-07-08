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
        void dependencyPresentFailsApplicability() {
            rewriteRun(
              spec -> spec.recipe(new DoesNotIncludeDependency("org.springframework", "spring-beans", null, null)),
              pomXml(
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
                  """,
                spec -> spec.afterRecipe(doc -> assertThat(doc.getMarkers().getMarkers()).noneMatch(marker -> marker instanceof SearchResult))
              )
            );
        }

        @Test
        void dependencyPresentTransitivelyFailsApplicability() {
            rewriteRun(
              spec -> spec.recipe(new DoesNotIncludeDependency("org.springframework", "spring-beans", null, null)),
              pomXml(
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
                spec -> spec.afterRecipe(doc -> assertThat(doc.getMarkers().getMarkers()).noneMatch(marker -> marker instanceof SearchResult))
              )
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {"test", "provided", "compile", "runtime"})
        void dependencyPresentTransitivelyWithSpecificScopeFailsApplicability(String scope) {
            rewriteRun(
              spec -> spec.recipe(new DoesNotIncludeDependency("org.springframework", "spring-beans", null, null)),
              pomXml(
                String.format("""
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
                  """, scope),
                spec -> spec.afterRecipe(doc -> assertThat(doc.getMarkers().getMarkers()).noneMatch(marker -> marker instanceof SearchResult))
              )
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {"test", "provided", "compile", "runtime"})
        void dependencyPresentWithSpecificScopeFailsApplicability(String scope) {
            rewriteRun(
              spec -> spec.recipe(new DoesNotIncludeDependency("org.springframework", "spring-beans", null, null)),
              pomXml(
                String.format("""
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
                spec -> spec.afterRecipe(doc -> assertThat(doc.getMarkers().getMarkers()).noneMatch(marker -> marker instanceof SearchResult))
              )
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {"test", "provided"})
        void dependencyPresentButNotInSpecifiedCompileScopePassesApplicability(String scope) {
            rewriteRun(
              spec -> spec.recipe((new DoesNotIncludeDependency("org.springframework", "spring-beans", null, "compile"))),
              pomXml(
                String.format("""
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
                  <!--~~>--><?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
        void dependencyPresentSpecifiedCompileScopeFailsApplicability(String scope) {
            rewriteRun(
              spec -> spec.recipe(new DoesNotIncludeDependency("org.springframework", "spring-beans", null, "compile")),
              pomXml(
                String.format("""
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
                spec -> spec.afterRecipe(doc -> assertThat(doc.getMarkers().getMarkers()).noneMatch(marker -> marker instanceof SearchResult))
              )
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {"provided"})
        void dependencyPresentButNotInSpecifiedTestScopePassesApplicability(String scope) {
            rewriteRun(
              spec -> spec.recipe(new DoesNotIncludeDependency("org.springframework", "spring-beans", null, "test")),
              pomXml(
                String.format("""
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
                  <!--~~>--><?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
        void dependencyPresentSpecifiedTestScopeFailsApplicability(String scope) {
            rewriteRun(
              spec -> spec.recipe(new DoesNotIncludeDependency("org.springframework", "spring-beans", null, "test")),
              pomXml(
                String.format("""
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
                spec -> spec.afterRecipe(doc -> assertThat(doc.getMarkers().getMarkers()).noneMatch(marker -> marker instanceof SearchResult))
              )
            );
        }
    }

    @Nested
    class DontCheckTransitive {

        @DocumentExample
        @Test
        void dependencyPresentTransitivelyPassesApplicability() {
            rewriteRun(
              spec -> spec.recipe(new DoesNotIncludeDependency("org.springframework", "spring-beans", true, null)),
              pomXml(
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
                  <!--~~>--><?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
        void dependencyPresentSpecificScopeFailsApplicability(String scope) {
            rewriteRun(
              spec -> spec.recipe(new DoesNotIncludeDependency("org.springframework", "spring-beans", true, null)),
              pomXml(
                String.format("""
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
                spec -> spec.afterRecipe(doc -> assertThat(doc.getMarkers().getMarkers()).noneMatch(marker -> marker instanceof SearchResult))
              )
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {"test", "provided"})
        void dependencyPresentButNotInSpecifiedCompileScopePassesApplicability(String scope) {
            rewriteRun(
              spec -> spec.recipe(new DoesNotIncludeDependency("org.springframework", "spring-beans", true, "compile")),
              pomXml(
                String.format("""
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
                  <!--~~>--><?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
        void dependencyPresentSpecifiedCompileScopeFailsApplicability(String scope) {
            rewriteRun(
              spec -> spec.recipe(new DoesNotIncludeDependency("org.springframework", "spring-beans", true, "compile")),
              pomXml(
                String.format("""
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
                spec -> spec.afterRecipe(doc -> assertThat(doc.getMarkers().getMarkers()).noneMatch(marker -> marker instanceof SearchResult))
              )
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {"provided"})
        void dependencyPresentButNotInSpecifiedTestScopePassesApplicability(String scope) {
            rewriteRun(
              spec -> spec.recipe(new DoesNotIncludeDependency("org.springframework", "spring-beans", true, "test")),
              pomXml(
                String.format("""
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
                  <!--~~>--><?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
        void dependencyPresentSpecifiedTestScopeFailsApplicability(String scope) {
            rewriteRun(
              spec -> spec.recipe(new DoesNotIncludeDependency("org.springframework", "spring-beans", true, "test")),
              pomXml(
                String.format("""
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
                spec -> spec.afterRecipe(doc -> assertThat(doc.getMarkers().getMarkers()).noneMatch(marker -> marker instanceof SearchResult))
              )
            );
        }
    }

    @Test
    void multimodule() {
        rewriteRun(
          spec -> spec.recipe(new DoesNotIncludeDependency("org.springframework", "spring-beans", null, null)),
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>b</artifactId>
                <version>1.0.0</version>
              </project>
              """,
            """
              <!--~~>--><?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
    void dependencyNotPresentPassesApplicability() {
        rewriteRun(
          spec -> spec.recipe(new DoesNotIncludeDependency("org.springframework", "spring-beans", null, null)),
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
              </project>
              """,
            """
              <!--~~>--><?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
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
