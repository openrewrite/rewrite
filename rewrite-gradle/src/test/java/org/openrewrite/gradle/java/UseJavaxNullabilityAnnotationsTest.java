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
package org.openrewrite.gradle.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.settingsGradle;
import static org.openrewrite.java.Assertions.*;

/**
 * <Beschreibung>
 * <br>
 *
 * @author askrock
 */
class UseJavaxNullabilityAnnotationsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder()
          .scanRuntimeClasspath("org.openrewrite.gradle.java")
          .build()
          .activateRecipes("org.openrewrite.gradle.java.UseJavaxNullabilityAnnotations"));
    }

    @Test
    void addsGradleDependencyIfNecessary() {
        rewriteRun(mavenProject("nullability",
          settingsGradle(
            """
              rootProject.name = "nullability"
              """
          ),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }
                          
              dependencies {
                  implementation "javax.annotation:javax.annotation-api:1"
              }
              """
          ),
          srcMainJava(java("""
            import org.openrewrite.internal.lang.NonNull;
                            
            class Test {
              @NonNull
              String nonNullVariable = "";
            }
            """, """
            import javax.annotation.Nonnull;
                            
            class Test {
              @Nonnull
              String nonNullVariable = "";
            }
            """)
          )
        ));
    }

    @Test
    void doesNotAddGradleDependencyIfUnnecessary() {
        rewriteRun(mavenProject("nullability",
          settingsGradle(
            """
              rootProject.name = "nullability"
              """
          ),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }
              """
          ),
          srcMainJava(java("""
            class Test {
              String nonNullVariable = "";
            }
            """)
          )
        ));
    }
}