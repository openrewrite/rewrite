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
package org.openrewrite.gradle;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

class RemoveInheritedDependenciesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
                .recipe(new RemoveInheritedDependencies());
    }

    /**
     * `testImplementation` extends `implementation`, so the test configuration already has the dependency.
     */
    @DocumentExample("Drop a test declaration the main one already supplies")
    @Test
    void removesTestImplementationAlreadyOnImplementation() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'com.google.guava:guava:33.0.0-jre'
                  testImplementation 'com.google.guava:guava:33.0.0-jre'
              }
              """,
            """
              plugins {
                  id 'java'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'com.google.guava:guava:33.0.0-jre'
              }
              """
          )
        );
    }

    /**
     * In `java-library` `implementation` extends `api`, so the `api` declaration is the one that reaches further and
     * the `implementation` one adds nothing.
     */
    @Test
    void keepsTheWiderApiDeclaration() {
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
                  api 'com.google.guava:guava:33.0.0-jre'
                  implementation 'com.google.guava:guava:33.0.0-jre'
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
                  api 'com.google.guava:guava:33.0.0-jre'
              }
              """
          )
        );
    }

    /**
     * Removing the narrower declaration would change which version resolves, which is a different change than
     * removing a redundant one.
     */
    @Test
    void noChangeWhenVersionsDiffer() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'com.google.guava:guava:33.0.0-jre'
                  testImplementation 'com.google.guava:guava:32.1.3-jre'
              }
              """
          )
        );
    }

    /**
     * `compileOnly` does not extend `implementation`; a dependency wanted at compile time only and also at runtime is
     * two separate intentions, not a duplicate.
     */
    @Test
    void noChangeForUnrelatedConfigurations() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'com.google.guava:guava:33.0.0-jre'
                  compileOnly 'com.google.guava:guava:33.0.0-jre'
              }
              """
          )
        );
    }

    @Test
    void noChangeForDistinctCoordinates() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'com.google.guava:guava:33.0.0-jre'
                  testImplementation 'org.assertj:assertj-core:3.25.3'
              }
              """
          )
        );
    }
}
