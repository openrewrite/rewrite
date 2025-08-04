/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.gradle;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.*;

class AddJUnitPlatformLauncherTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .beforeRecipe(withToolingApi())
          .parser(JavaParser.fromJavaVersion().classpath("junit-jupiter-api"))
          .recipeFromResources("org.openrewrite.gradle.AddJUnitPlatformLauncher");
    }

    @DocumentExample
    @Test
    void addJUnitPlatformLauncher() {
        rewriteRun(
          mavenProject("project",
            srcTestJava(
              java(
                """
                  import org.junit.jupiter.api.Test;
                  public class A {
                      @Test
                      void foo() {
                      }
                  }
                  """
              )
            ),
            buildGradle(
              """
                plugins {
                    id "java-library"
                }

                repositories {
                    mavenCentral()
                }
                """,
              spec -> spec.after(buildGradle -> {
                  assertThat(buildGradle).contains("testRuntimeOnly \"org.junit.platform:junit-platform-launcher:");
                  return buildGradle;
              })
            )
          )
        );
    }
}
