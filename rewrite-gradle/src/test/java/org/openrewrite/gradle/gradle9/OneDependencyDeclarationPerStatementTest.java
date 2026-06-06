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
package org.openrewrite.gradle.gradle9;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;

class OneDependencyDeclarationPerStatementTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new OneDependencyDeclarationPerStatement());
    }

    @DocumentExample
    @Test
    void splitsMultipleStringLiteralCoordinates() {
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
                  implementation 'com.google.guava:guava:30.0-jre', 'org.apache.commons:commons-lang3:3.12.0'
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
                  implementation 'com.google.guava:guava:30.0-jre'
                  implementation 'org.apache.commons:commons-lang3:3.12.0'
              }
              """
          )
        );
    }

    @Test
    void splitsWhenSecondCoordinateUsesVersionVariable() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              ext {
                  guavaVersion = '30.0-jre'
              }

              dependencies {
                  implementation 'org.apache.commons:commons-lang3:3.12.0', 'com.google.guava:guava:' + guavaVersion
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              ext {
                  guavaVersion = '30.0-jre'
              }

              dependencies {
                  implementation 'org.apache.commons:commons-lang3:3.12.0'
                  implementation 'com.google.guava:guava:' + guavaVersion
              }
              """
          )
        );
    }

    @Test
    void leavesSingleCoordinateAlone() {
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
                  implementation 'com.google.guava:guava:30.0-jre'
              }
              """
          )
        );
    }

    @Test
    void leavesMultiComponentPositionalFormAlone() {
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
                  implementation 'com.google.guava', 'guava', '30.0-jre'
              }
              """
          )
        );
    }

    @Test
    void leavesCallWithTrailingClosureAlone() {
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
                  implementation('com.google.guava:guava:30.0-jre') {
                      exclude group: 'com.google.code.findbugs'
                  }
              }
              """
          )
        );
    }

    @Test
    void splitsAcrossDifferentConfigurations() {
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
                  implementation 'com.google.guava:guava:30.0-jre', 'org.apache.commons:commons-lang3:3.12.0'
                  testImplementation 'junit:junit:4.13.2', 'org.mockito:mockito-core:5.0.0'
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
                  implementation 'com.google.guava:guava:30.0-jre'
                  implementation 'org.apache.commons:commons-lang3:3.12.0'
                  testImplementation 'junit:junit:4.13.2'
                  testImplementation 'org.mockito:mockito-core:5.0.0'
              }
              """
          )
        );
    }

    @Test
    void preservesLineCommentOnFirstSplitOnly() {
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
                  // bump these together later
                  implementation 'com.google.guava:guava:30.0-jre', 'org.apache.commons:commons-lang3:3.12.0'
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
                  // bump these together later
                  implementation 'com.google.guava:guava:30.0-jre'
                  implementation 'org.apache.commons:commons-lang3:3.12.0'
              }
              """
          )
        );
    }

    @Test
    void doesNotTouchKotlinDsl() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation("com.google.guava:guava:30.0-jre")
              }
              """
          )
        );
    }
}
