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
package org.openrewrite.maven.search;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.maven.table.DependenciesInUse;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.Assertions.pomXml;

class DependencyInsightTest implements RewriteTest {

    @DocumentExample
    @Test
    void findDependency() {
        rewriteRun(
          spec -> spec.recipe(new DependencyInsight("*guava*", "*", "compile", null, null)),
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
                      <version>29.0-jre</version>
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
                  <!--~~>--><dependency>
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

    @Issue("https://github.com/openrewrite/rewrite/issues/1418")
    @Test
    void doesNotMatchTestScope() {
        rewriteRun(
          spec -> spec.recipe(new DependencyInsight("*guava*", "*", "compile", null, null)),
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
                      <version>29.0-jre</version>
                      <scope>test</scope>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void findDependencyTransitively() {
        rewriteRun(
          spec -> spec.recipe(new DependencyInsight("*", "*simpleclient*", "compile", null, null)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                      <groupId>io.micrometer</groupId>
                      <artifactId>micrometer-registry-prometheus</artifactId>
                      <version>1.6.3</version>
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
                  <!--~~(io.prometheus:simpleclient_common:0.9.0)~~>--><dependency>
                      <groupId>io.micrometer</groupId>
                      <artifactId>micrometer-registry-prometheus</artifactId>
                      <version>1.6.3</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void onlyDirect() {
        rewriteRun(
          spec -> spec.recipe(new DependencyInsight("*", "*simpleclient*", "compile", null, true)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                      <groupId>io.micrometer</groupId>
                      <artifactId>micrometer-registry-prometheus</artifactId>
                      <version>1.6.3</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }


    @Test
    void versionSelector() {
        rewriteRun(
          spec -> spec.recipe(new DependencyInsight("org.openrewrite", "*", "compile", "8.0.0", true)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                      <groupId>org.openrewrite</groupId>
                      <artifactId>rewrite-java</artifactId>
                      <version>8.0.0</version>
                  </dependency>
                  <dependency>
                      <groupId>org.openrewrite</groupId>
                      <artifactId>rewrite-yaml</artifactId>
                      <version>7.0.0</version>
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
                  <!--~~>--><dependency>
                      <groupId>org.openrewrite</groupId>
                      <artifactId>rewrite-java</artifactId>
                      <version>8.0.0</version>
                  </dependency>
                  <dependency>
                      <groupId>org.openrewrite</groupId>
                      <artifactId>rewrite-yaml</artifactId>
                      <version>7.0.0</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "6.1.5", // exact
      "6.1.1-6.1.15", // hyphenated
      "[6.1.1,6.1.6)", "[6.1.1,6.1.5]", "[6.1.5,6.1.15]", "(6.1.4,6.1.15]", // full range
      "6.1.X", // X range
      "~6.1.0", "~6.1", // tilde range
    })
    void versionPatterns(String versionPattern) {
        rewriteRun(
          recipeSpec -> recipeSpec.recipe(new DependencyInsight("org.springframework", "*", null, versionPattern, null)),
            //language=xml
            pomXml(
              """
                <project>
                  <groupId>com.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0.0</version>
    
                  <dependencies>
                    <dependency>
                      <groupId>org.springframework</groupId>
                      <artifactId>spring-core</artifactId>
                      <version>6.1.5</version>
                    </dependency>
                    <dependency>
                      <groupId>org.springframework</groupId>
                      <artifactId>spring-aop</artifactId>
                      <version>6.2.2</version>
                    </dependency>
                  </dependencies>
                </project>
                """,
              """
                <project>
                  <groupId>com.example</groupId>
                  <artifactId>foo</artifactId>
                  <version>1.0.0</version>
    
                  <dependencies>
                    <!--~~>--><dependency>
                      <groupId>org.springframework</groupId>
                      <artifactId>spring-core</artifactId>
                      <version>6.1.5</version>
                    </dependency>
                    <dependency>
                      <groupId>org.springframework</groupId>
                      <artifactId>spring-aop</artifactId>
                      <version>6.2.2</version>
                    </dependency>
                  </dependencies>
                </project>
                """
            )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/6151")
    @Test
    void findTwoDependenciesAndTheirDataTableRows() {
        rewriteRun(
          spec -> spec
            .dataTable(DependenciesInUse.Row.class, rows -> assertThat(rows).hasSize(2))
            .recipes(
            new DependencyInsight("*", "guava", "compile", null, null),
            new DependencyInsight("*", "lombok", "compile", null, null)
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
                      <version>29.0-jre</version>
                  </dependency>
                  <dependency>
                      <groupId>org.projectlombok</groupId>
                      <artifactId>lombok</artifactId>
                      <version>1.18.42</version>
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
                  <!--~~>--><dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>29.0-jre</version>
                  </dependency>
                  <!--~~>--><dependency>
                      <groupId>org.projectlombok</groupId>
                      <artifactId>lombok</artifactId>
                      <version>1.18.42</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }
}
