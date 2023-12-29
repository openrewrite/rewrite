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
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class DependencyInsightTest implements RewriteTest {

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
}
