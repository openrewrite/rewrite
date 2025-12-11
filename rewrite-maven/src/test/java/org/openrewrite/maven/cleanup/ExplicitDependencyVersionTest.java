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
package org.openrewrite.maven.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.Assertions.pomXml;

class ExplicitDependencyVersionTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ExplicitDependencyVersion());
    }

    @DocumentExample
    @Test
    void shouldReplaceLatestWithExplicitVersion() {
        rewriteRun(
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
                    <version>LATEST</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            spec -> spec.after(actual ->
              assertThat(actual)
                .doesNotContain("LATEST")
                .containsPattern("<version>\\d+\\.\\d+[^<]*</version>")
                .actual())
          )
        );
    }

    @Test
    void shouldReplaceReleaseWithExplicitVersion() {
        rewriteRun(
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
                    <version>RELEASE</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            spec -> spec.after(actual ->
              assertThat(actual)
                .doesNotContain("RELEASE")
                .containsPattern("<version>\\d+\\.\\d+[^<]*</version>")
                .actual())
          )
        );
    }

    @Test
    void shouldNotChangeExplicitVersion() {
        rewriteRun(
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
                    <version>31.1-jre</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void shouldReplaceLatestInDependencyManagement() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>LATEST</version>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """,
            spec -> spec.after(actual ->
              assertThat(actual)
                .doesNotContain("LATEST")
                .containsPattern("<version>\\d+\\.\\d+[^<]*</version>")
                .actual())
          )
        );
    }

    @Test
    void shouldReplaceMultipleDependenciesWithLatestOrRelease() {
        rewriteRun(
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
                    <version>LATEST</version>
                  </dependency>
                  <dependency>
                    <groupId>org.projectlombok</groupId>
                    <artifactId>lombok</artifactId>
                    <version>RELEASE</version>
                  </dependency>
                  <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter-api</artifactId>
                    <version>5.9.0</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            spec -> spec.after(actual ->
              assertThat(actual)
                .doesNotContain("LATEST")
                .doesNotContain("RELEASE")
                .contains("<version>5.9.0</version>")
                .actual())
          )
        );
    }

    @Test
    void shouldReplaceLatestWhenArtifactIdUsesProperty() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <properties>
                  <guava.artifactId>guava</guava.artifactId>
                </properties>
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>${guava.artifactId}</artifactId>
                    <version>LATEST</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            spec -> spec.after(actual ->
              assertThat(actual)
                .doesNotContain("LATEST")
                .contains("${guava.artifactId}")
                .containsPattern("<version>\\d+\\.\\d+[^<]*</version>")
                .actual())
          )
        );
    }
}
