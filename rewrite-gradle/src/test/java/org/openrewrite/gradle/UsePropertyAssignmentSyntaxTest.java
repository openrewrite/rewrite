/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
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
import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.gradle.Assertions.settingsGradle;

class UsePropertyAssignmentSyntaxTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UsePropertyAssignmentSyntax("description"));
    }

    @DocumentExample
    @Test
    void spaceSyntaxConvertedToAssignment() {
        rewriteRun(
          buildGradle(
            """
              tasks.register('runLogic', JavaExec) {
                  description 'My precious logic'
                  classpath sourceSets.main.runtimeClasspath
                  mainClass = 'my.org.logic.Logic'
              }
              """,
            """
              tasks.register('runLogic', JavaExec) {
                  description = 'My precious logic'
                  classpath sourceSets.main.runtimeClasspath
                  mainClass = 'my.org.logic.Logic'
              }
              """
          )
        );
    }

    @Test
    void parenthesizedSyntaxConvertedToAssignment() {
        rewriteRun(
          buildGradle(
            """
              tasks.register('runLogic', JavaExec) {
                  description('My precious logic')
              }
              """,
            """
              tasks.register('runLogic', JavaExec) {
                  description = 'My precious logic'
              }
              """
          )
        );
    }

    @Test
    void alreadyAssignmentSyntaxUnchanged() {
        rewriteRun(
          buildGradle(
            """
              tasks.register('runLogic', JavaExec) {
                  description = 'My precious logic'
              }
              """
          )
        );
    }

    @Test
    void differentMethodNameUnchanged() {
        rewriteRun(
          buildGradle(
            """
              tasks.register('runLogic', JavaExec) {
                  classpath sourceSets.main.runtimeClasspath
              }
              """
          )
        );
    }

    @Test
    void nestedInClosure() {
        rewriteRun(
          buildGradle(
            """
              jar {
                  description 'Build the jar'
              }
              """,
            """
              jar {
                  description = 'Build the jar'
              }
              """
          )
        );
    }

    @Test
    void doubleQuotedString() {
        rewriteRun(
          buildGradle(
            """
              jar {
                  description "Build the jar"
              }
              """,
            """
              jar {
                  description = "Build the jar"
              }
              """
          )
        );
    }

    @Test
    void multipleOccurrences() {
        rewriteRun(
          buildGradle(
            """
              tasks.register('a', JavaExec) {
                  description 'First'
              }
              tasks.register('b', JavaExec) {
                  description 'Second'
              }
              """,
            """
              tasks.register('a', JavaExec) {
                  description = 'First'
              }
              tasks.register('b', JavaExec) {
                  description = 'Second'
              }
              """
          )
        );
    }

    @Test
    void kotlinDslMethodCallUnchanged() {
        rewriteRun(
          buildGradleKts(
            """
              tasks.register<JavaExec>("runLogic") {
                  description("My precious logic")
              }
              """
          )
        );
    }

    @Test
    void pluginVersionUnchangedInPluginsBlock() {
        rewriteRun(
          spec -> spec.recipe(new UsePropertyAssignmentSyntax("version")),
          settingsGradle(
            """
              plugins {
                  id 'com.gradle.develocity' version '3.19'
              }
              """
          )
        );
    }

    @Test
    void pluginVersionUnchangedInBuildGradlePluginsBlock() {
        rewriteRun(
          spec -> spec.recipe(new UsePropertyAssignmentSyntax("version")),
          buildGradle(
            """
              plugins {
                  id 'com.example.plugin' version '1.0'
              }
              """
          )
        );
    }

    @Test
    void versionOutsidePluginsBlockConverted() {
        rewriteRun(
          spec -> spec.recipe(new UsePropertyAssignmentSyntax("version")),
          buildGradle(
            """
              version '1.0-SNAPSHOT'
              """,
            """
              version = '1.0-SNAPSHOT'
              """
          )
        );
    }

    @Test
    void noArgMethodCallUnchanged() {
        rewriteRun(
          buildGradle(
            """
              tasks.register('runLogic', JavaExec) {
                  description()
              }
              """
          )
        );
    }
}
