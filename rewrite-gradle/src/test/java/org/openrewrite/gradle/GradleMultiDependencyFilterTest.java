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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;

class GradleMultiDependencyFilterTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpgradeDependencyVersion("com.fasterxml.jackson.core", "jackson-*", "2.17.0", null));
    }

    @DocumentExample
    @Test
    void upgradesOnlyMatchingDependenciesInVarargs() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                id 'java-library'
              }

              repositories {
                mavenCentral()
              }

              dependencies {
                implementation(
                  'com.fasterxml.jackson.core:jackson-databind:2.11.0',
                  'com.google.guava:guava:29.0-jre',
                  'com.fasterxml.jackson.core:jackson-core:2.11.0')
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
                implementation(
                  'com.fasterxml.jackson.core:jackson-databind:2.17.0',
                  'com.google.guava:guava:29.0-jre',
                  'com.fasterxml.jackson.core:jackson-core:2.17.0')
              }
              """
          )
        );
    }

    @Test
    void doesNotChangeNonMatchingVarargs() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("org.springframework", "*", "6.0.0", null)),
          buildGradle(
            """
              dependencies {
                implementation(
                  'com.fasterxml.jackson.core:jackson-databind:2.11.0',
                  'com.google.guava:guava:29.0-jre')
              }
              """
          )
        );
    }
}