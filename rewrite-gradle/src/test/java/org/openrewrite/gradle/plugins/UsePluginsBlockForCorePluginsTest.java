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
package org.openrewrite.gradle.plugins;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;

class UsePluginsBlockForCorePluginsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UsePluginsBlockForCorePlugins());
    }

    @DocumentExample
    @Test
    void createPluginsBlock() {
        rewriteRun(
          buildGradle(
            """
              apply plugin: 'java-library'
              apply plugin: 'maven-publish'

              group = 'com.example'
              """,
            """
              plugins {
                  id 'java-library'
                  id 'maven-publish'
              }

              group = 'com.example'
              """
          )
        );
    }

    @Test
    void addToExistingPluginsBlock() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              apply plugin: 'jacoco'

              group = 'com.example'
              """,
            """
              plugins {
                  id 'java'
                  id 'jacoco'
              }

              group = 'com.example'
              """
          )
        );
    }

    @Test
    void dropApplyOfPluginAlreadyInPluginsBlock() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              apply plugin: 'java'
              """,
            """
              plugins {
                  id 'java'
              }
              """
          )
        );
    }

    @Test
    void afterBuildscriptBlock() {
        rewriteRun(
          buildGradle(
            """
              buildscript {
                  repositories {
                      mavenCentral()
                  }
              }

              apply plugin: 'java'
              """,
            """
              buildscript {
                  repositories {
                      mavenCentral()
                  }
              }

              plugins {
                  id 'java'
              }
              """
          )
        );
    }

    @Test
    void keepCommunityPlugins() {
        rewriteRun(
          buildGradle(
            """
              apply plugin: 'com.github.johnrengelman.shadow'
              """
          )
        );
    }

    @Test
    void keepPluginsAppliedToOtherProjects() {
        rewriteRun(
          buildGradle(
            """
              subprojects {
                  apply plugin: 'java'
              }
              """
          )
        );
    }

    @Test
    void keepScriptPlugins() {
        rewriteRun(
          buildGradle(
            """
              apply plugin: 'java'
              """,
            spec -> spec.path("gradle/java-conventions.gradle")
          )
        );
    }

    @Test
    void kotlinDsl() {
        rewriteRun(
          buildGradleKts(
            """
              apply(plugin = "java-library")

              group = "com.example"
              """,
            """
              plugins {
                  id("java-library")
              }

              group = "com.example"
              """
          )
        );
    }

    @Test
    void kotlinDslPluginAlreadyDeclaredWithAccessor() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  java
              }

              apply(plugin = "java")
              """,
            """
              plugins {
                  java
              }
              """
          )
        );
    }
}
