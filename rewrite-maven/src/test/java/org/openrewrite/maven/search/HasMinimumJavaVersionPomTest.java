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
package org.openrewrite.maven.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;
import static org.openrewrite.maven.Assertions.pomXml;

class HasMinimumJavaVersionPomTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromYaml(
          """
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.maven.GatedOnJava17
            description: Test.
            preconditions:
              - org.openrewrite.java.search.HasMinimumJavaVersion:
                  version: 17
            recipeList:
              - org.openrewrite.maven.ChangePropertyValue:
                  key: junit.version
                  newValue: 6.0.0
            """,
          "org.openrewrite.maven.GatedOnJava17"
        );
    }

    @Test
    void pomPassesWhenRepositoryMeetsMinimum() {
        rewriteRun(
          java(
            """
              class A {
              }
              """,
            spec -> spec.markers(javaVersion(17))
          ),
          pomXml(
            """
              <project>
                <groupId>com.example</groupId>
                <artifactId>demo</artifactId>
                <version>1.0.0</version>
                <properties>
                  <junit.version>5.13.0</junit.version>
                </properties>
              </project>
              """,
            """
              <project>
                <groupId>com.example</groupId>
                <artifactId>demo</artifactId>
                <version>1.0.0</version>
                <properties>
                  <junit.version>6.0.0</junit.version>
                </properties>
              </project>
              """
          )
        );
    }

    @Test
    void pomBlockedWhenRepositoryBelowMinimum() {
        rewriteRun(
          java(
            """
              class A {
              }
              """,
            spec -> spec.markers(javaVersion(11))
          ),
          pomXml(
            """
              <project>
                <groupId>com.example</groupId>
                <artifactId>demo</artifactId>
                <version>1.0.0</version>
                <properties>
                  <junit.version>5.13.0</junit.version>
                </properties>
              </project>
              """
          )
        );
    }
}
