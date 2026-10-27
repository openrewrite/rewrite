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
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;

class UsePluginsBlockTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        // A hand-built marker rather than withToolingApi(): AddBuildPlugin only needs the marker's presence for a
        // versionless plugin, and a test whose script names a plugin that does not resolve cannot run real Gradle.
        spec.recipe(new UsePluginsBlock())
                .allSources(source -> source.markers(GradleProject.builder()
                        .name("project")
                        .path(":")
                        .build()));
    }

    @DocumentExample("Apply a core plugin through the plugins block")
    @Test
    void convertsCorePluginApply() {
        rewriteRun(
          buildGradle(
            """
              apply plugin: 'java'

              repositories {
                  mavenCentral()
              }
              """,
            """
              plugins {
                  id 'java'
              }

              repositories {
                  mavenCentral()
              }
              """
          )
        );
    }

    @Test
    void addsToAnExistingPluginsBlock() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              apply plugin: 'maven-publish'
              """,
            """
              plugins {
                  id 'java'
                  id 'maven-publish'
              }
              """
          )
        );
    }

    @Test
    void convertsKotlinDslApply() {
        rewriteRun(
          buildGradleKts(
            """
              apply(plugin = "java")

              repositories {
                  mavenCentral()
              }
              """,
            """
              plugins {
                  id("java")
              }

              repositories {
                  mavenCentral()
              }
              """
          )
        );
    }

    /**
     * A third-party plugin in a `plugins` block needs a version, or a `buildscript` classpath entry that the block
     * cannot see. Rewriting it without resolving that would stop the build configuring, so it is left alone.
     */
    @Test
    void noChangeForThirdPartyPlugin() {
        rewriteRun(
          buildGradle(
            """
              apply plugin: 'com.example.notcore'
              """
          )
        );
    }

    /**
     * A `plugins` block applies only to the script it appears in, so an apply inside `subprojects` or `allprojects`
     * has no equivalent there.
     */
    @Test
    void noChangeInsideSubprojects() {
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

    /**
     * `plugins` names a plugin by id; a class reference has none to write.
     */
    @Test
    void noChangeForClassReference() {
        rewriteRun(
          buildGradle(
            """
              apply plugin: org.gradle.api.plugins.JavaPlugin
              """
          )
        );
    }
}
