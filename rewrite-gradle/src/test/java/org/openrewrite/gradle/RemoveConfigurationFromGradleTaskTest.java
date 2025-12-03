/*
 * Copyright 2025 the original author or authors.
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
import static org.openrewrite.gradle.Assertions.buildGradleKts;

class RemoveConfigurationFromGradleTaskTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveConfigurationFromGradleTask(
          "bootJar",
          "loaderImplementation = org.springframework.boot.loader.tools.LoaderImplementation.CLASSIC"
        ));
    }

    @DocumentExample
    @Test
    void removeLoaderImplementationFromBootJarTask() {
        rewriteRun(
          //language=gradle
          buildGradle(
            """
              plugins {
                  id 'org.springframework.boot' version '4.0.0'
              }
              
              bootJar {
                  loaderImplementation = 'org.springframework.boot.loader.tools.LoaderImplementation.CLASSIC'
              }
              """,
            """
              plugins {
                  id 'org.springframework.boot' version '4.0.0'
              }
              
              bootJar {
              }
              """
          )
        );
    }

    @Test
    void removeLoaderImplementationFromBootJarTaskUsingGlob() {
        rewriteRun(
          spec -> spec.recipe(new RemoveConfigurationFromGradleTask("bootJar", "loaderImplementation*")),
          //language=gradle
          buildGradle(
            """
              plugins {
                  id 'org.springframework.boot' version '4.0.0'
              }
              
              bootJar {
                  loaderImplementation = 'org.springframework.boot.loader.tools.LoaderImplementation.CLASSIC'
              }
              """,
            """
              plugins {
                  id 'org.springframework.boot' version '4.0.0'
              }
              
              bootJar {
              }
              """
          )
        );
    }

    @Test
    void removeLoaderImplementationWithOtherConfigurations() {
        rewriteRun(
          //language=gradle
          buildGradle(
            """
              plugins {
                  id 'org.springframework.boot' version '4.0.0'
              }
              
              bootJar {
                  enabled = true
                  loaderImplementation = org.springframework.boot.loader.tools.LoaderImplementation.CLASSIC
                  archiveFileName = 'app.jar'
              }
              """,
            """
              plugins {
                  id 'org.springframework.boot' version '4.0.0'
              }
              
              bootJar {
                  enabled = true
                  archiveFileName = 'app.jar'
              }
              """
          )
        );
    }

    @Test
    void removeLoaderImplementationFromTasksNamed() {
        rewriteRun(
          //language=gradle
          buildGradle(
            """
              plugins {
                  id 'org.springframework.boot' version '4.0.0'
              }
              
              tasks.named('bootJar') {
                  loaderImplementation = org.springframework.boot.loader.tools.LoaderImplementation.CLASSIC
              }
              """,
            """
              plugins {
                  id 'org.springframework.boot' version '4.0.0'
              }
              
              tasks.named('bootJar') {
              }
              """
          )
        );
    }

    @Test
    void removeLoaderImplementationFromTasksNamedWithOtherConfigurations() {
        rewriteRun(
          //language=gradle
          buildGradle(
            """
              plugins {
                  id 'org.springframework.boot' version '4.0.0'
              }
              
              tasks.named('bootJar') {
                  enabled = true
                  loaderImplementation = org.springframework.boot.loader.tools.LoaderImplementation.CLASSIC
                  archiveFileName = 'app.jar'
              }
              """,
            """
              plugins {
                  id 'org.springframework.boot' version '4.0.0'
              }
              
              tasks.named('bootJar') {
                  enabled = true
                  archiveFileName = 'app.jar'
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenConfigurationNotPresent() {
        rewriteRun(
          //language=gradle
          buildGradle(
            """
              plugins {
                  id 'org.springframework.boot' version '4.0.0'
              }
              
              bootJar {
                  enabled = true
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenDifferentLoaderImplementation() {
        rewriteRun(
          //language=gradle
          buildGradle(
            """
              plugins {
                  id 'org.springframework.boot' version '4.0.0'
              }
              
              bootJar {
                  loaderImplementation = 'org.springframework.boot.loader.tools.LoaderImplementation.DEFAULT'
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenDifferentTask() {
        rewriteRun(
          //language=gradle
          buildGradle(
            """
              plugins {
                  id 'org.springframework.boot' version '4.0.0'
              }
              
              bootWar {
                  loaderImplementation = 'org.springframework.boot.loader.tools.LoaderImplementation.CLASSIC'
              }
              """
          )
        );
    }

    @Test
    void handleMultipleSpacesAndFormatting() {
        rewriteRun(
          //language=gradle
          buildGradle(
            """
              plugins {
                  id 'org.springframework.boot' version '4.0.0'
              }
              
              bootJar {
                  loaderImplementation   =   'org.springframework.boot.loader.tools.LoaderImplementation.CLASSIC'
              }
              """,
            """
              plugins {
                  id 'org.springframework.boot' version '4.0.0'
              }
              
              bootJar {
              }
              """
          )
        );
    }

    @Test
    void removeLoaderImplementationFromBootJarTaskKotlin() {
        rewriteRun(
          //language=kotlin
          buildGradleKts(
            """
              plugins {
                  id("org.springframework.boot") version "4.0.0"
              }
              
              tasks.bootJar {
                  loaderImplementation = 'org.springframework.boot.loader.tools.LoaderImplementation.CLASSIC'
              }
              """,
            """
              plugins {
                  id("org.springframework.boot") version "4.0.0"
              }
              
              tasks.bootJar {
              }
              """
          )
        );
    }

    @Test
    void removeLoaderImplementationWithOtherConfigurationsKotlin() {
        rewriteRun(
          //language=kotlin
          buildGradleKts(
            """
              plugins {
                  id("org.springframework.boot") version "4.0.0"
              }
              
              tasks.bootJar {
                  enabled = true
                  loaderImplementation = 'org.springframework.boot.loader.tools.LoaderImplementation.CLASSIC'
                  archiveFileName.set("app.jar")
              }
              """,
            """
              plugins {
                  id("org.springframework.boot") version "4.0.0"
              }
              
              tasks.bootJar {
                  enabled = true
                  archiveFileName.set("app.jar")
              }
              """
          )
        );
    }

    @Test
    void removeLoaderImplementationFromTasksNamedKotlin() {
        rewriteRun(
          //language=kotlin
          buildGradleKts(
            """
              plugins {
                  id("org.springframework.boot") version "4.0.0"
              }
              
              tasks.named("bootJar") {
                  loaderImplementation = 'org.springframework.boot.loader.tools.LoaderImplementation.CLASSIC'
              }
              """,
            """
              plugins {
                  id("org.springframework.boot") version "4.0.0"
              }
              
              tasks.named("bootJar") {
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenConfigurationNotPresentKotlin() {
        rewriteRun(
          //language=kotlin
          buildGradleKts(
            """
              plugins {
                  id("org.springframework.boot") version "4.0.0"
              }
              
              tasks.bootJar {
                  enabled = true
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenDifferentLoaderImplementationKotlin() {
        rewriteRun(
          //language=kotlin
          buildGradleKts(
            """
              plugins {
                  id("org.springframework.boot") version "4.0.0"
              }
              
              tasks.bootJar {
                  loaderImplementation = 'org.springframework.boot.loader.tools.LoaderImplementation.DEFAULT'
              }
              """
          )
        );
    }
}
