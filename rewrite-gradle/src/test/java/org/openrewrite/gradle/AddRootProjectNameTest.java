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
import org.openrewrite.groovy.tree.G;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import java.util.function.Consumer;

import static org.openrewrite.gradle.Assertions.*;

class AddRootProjectNameTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddRootProjectName(null));
    }

    /**
     * The name is Gradle's own, read from the `GradleProject` marker, so writing it changes nothing about how the
     * build resolves — it pins what the directory name was already supplying.
     */
    private static Consumer<SourceSpec<G.CompilationUnit>> named(String name) {
        return spec -> spec.markers(GradleProject.builder()
                .name(name)
                .path(":")
                .build());
    }

    @DocumentExample("Pin the root project name that the directory name was supplying")
    @Test
    void addsRootProjectName() {
        rewriteRun(
          buildGradle("plugins { id 'java' }", named("my-service")),
          settingsGradle(
            "",
            "rootProject.name = 'my-service'\n"
          )
        );
    }

    @Test
    void addsRootProjectNameToKotlinDsl() {
        rewriteRun(
          buildGradle("plugins { id 'java' }", named("my-service")),
          settingsGradleKts(
            "",
            "rootProject.name = \"my-service\"\n"
          )
        );
    }

    @Test
    void insertsAfterPluginManagement() {
        rewriteRun(
          buildGradle("plugins { id 'java' }", named("my-service")),
          settingsGradle(
            """
              pluginManagement {
                  repositories {
                      gradlePluginPortal()
                  }
              }
              """,
            """
              pluginManagement {
                  repositories {
                      gradlePluginPortal()
                  }
              }

              rootProject.name = 'my-service'
              """
          )
        );
    }

    @Test
    void noChangeWhenAlreadyNamed() {
        rewriteRun(
          buildGradle("plugins { id 'java' }", named("my-service")),
          settingsGradle("rootProject.name = 'something-else'\n")
        );
    }

    /**
     * Without the marker there is no way to know the name Gradle is inferring, and guessing one would rename the
     * project rather than pin it.
     */
    @Test
    void noChangeWhenNameIsUnknown() {
        rewriteRun(
          buildGradle("plugins { id 'java' }"),
          settingsGradle("")
        );
    }

    @Test
    void explicitOptionWinsOverTheMarker() {
        rewriteRun(
          spec -> spec.recipe(new AddRootProjectName("chosen-name")),
          buildGradle("plugins { id 'java' }", named("my-service")),
          settingsGradle(
            "",
            "rootProject.name = 'chosen-name'\n"
          )
        );
    }
}
