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
package org.openrewrite.gradle.plugins;

import org.junit.jupiter.api.Test;
import org.openrewrite.Tree;
import org.openrewrite.gradle.marker.GradleBuildscript;
import org.openrewrite.gradle.marker.GradlePluginDescriptor;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.marker.GradleSettings;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.*;

class ChangePluginDslTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangePlugin("nebula.optional-base", "com.netflix.nebula.optional-base", null));
    }

    @Test
    void kotlinBuildVersionless() {
        rewriteRun(
          buildGradleKts(
            """
              plugins { id("nebula.optional-base") }
              """,
            """
              plugins { id("com.netflix.nebula.optional-base") }
              """,
            spec -> spec.markers(GradleProject.builder()
                .id(Tree.randomId())
                .plugins(singletonList(new GradlePluginDescriptor("legacy.Implementation", "nebula.optional-base")))
                .buildscript(GradleBuildscript.builder().build())
                .build())
              .afterRecipe(cu -> assertThat(cu.getMarkers().findFirst(GradleProject.class)).hasValueSatisfying(marker ->
                assertThat(marker.getPlugins()).singleElement().satisfies(plugin -> {
                    assertThat(plugin.getId()).isEqualTo("com.netflix.nebula.optional-base");
                    assertThat(plugin.getFullyQualifiedClassName()).isEqualTo("unknown");
                })))
          )
        );
    }

    @Test
    void kotlinBuildVersion() {
        rewriteRun(
          buildGradleKts(
            """
              plugins { id("nebula.optional-base") version "7.0.0" }
              """,
            """
              plugins { id("com.netflix.nebula.optional-base") version "7.0.0" }
              """,
            spec -> spec.markers(GradleProject.builder()
                .id(Tree.randomId())
                .plugins(singletonList(new GradlePluginDescriptor("legacy.Implementation", "nebula.optional-base")))
                .buildscript(GradleBuildscript.builder().build())
                .build())
              .afterRecipe(cu -> assertThat(cu.getMarkers().findFirst(GradleProject.class)).hasValueSatisfying(marker ->
                assertThat(marker.getPlugins()).singleElement().satisfies(plugin -> {
                    assertThat(plugin.getId()).isEqualTo("com.netflix.nebula.optional-base");
                    assertThat(plugin.getFullyQualifiedClassName()).isEqualTo("unknown");
                })))
          )
        );
    }

    @Test
    void kotlinSettingsVersionless() {
        rewriteRun(
          settingsGradleKts(
            """
              plugins { id("nebula.optional-base") }
              """,
            """
              plugins { id("com.netflix.nebula.optional-base") }
              """,
            spec -> spec.markers(GradleSettings.builder()
                .id(Tree.randomId())
                .plugins(singletonList(new GradlePluginDescriptor("legacy.Implementation", "nebula.optional-base")))
                .buildscript(GradleBuildscript.builder().build())
                .build())
              .afterRecipe(cu -> assertThat(cu.getMarkers().findFirst(GradleSettings.class)).hasValueSatisfying(marker ->
                assertThat(marker.getPlugins()).singleElement().satisfies(plugin -> {
                    assertThat(plugin.getId()).isEqualTo("com.netflix.nebula.optional-base");
                    assertThat(plugin.getFullyQualifiedClassName()).isEqualTo("unknown");
                })))
          )
        );
    }

    @Test
    void kotlinSettingsVersion() {
        rewriteRun(
          settingsGradleKts(
            """
              plugins { id("nebula.optional-base") version "7.0.0" }
              """,
            """
              plugins { id("com.netflix.nebula.optional-base") version "7.0.0" }
              """,
            spec -> spec.markers(GradleSettings.builder()
                .id(Tree.randomId())
                .plugins(singletonList(new GradlePluginDescriptor("legacy.Implementation", "nebula.optional-base")))
                .buildscript(GradleBuildscript.builder().build())
                .build())
              .afterRecipe(cu -> assertThat(cu.getMarkers().findFirst(GradleSettings.class)).hasValueSatisfying(marker ->
                assertThat(marker.getPlugins()).singleElement().satisfies(plugin -> {
                    assertThat(plugin.getId()).isEqualTo("com.netflix.nebula.optional-base");
                    assertThat(plugin.getFullyQualifiedClassName()).isEqualTo("unknown");
                })))
          )
        );
    }

    @Test
    void groovyBuildVersionless() {
        rewriteRun(
          buildGradle(
            """
              plugins { id 'nebula.optional-base' }
              """,
            """
              plugins { id 'com.netflix.nebula.optional-base' }
              """,
            spec -> spec.markers(GradleProject.builder()
                .id(Tree.randomId())
                .plugins(singletonList(new GradlePluginDescriptor("legacy.Implementation", "nebula.optional-base")))
                .buildscript(GradleBuildscript.builder().build())
                .build())
              .afterRecipe(cu -> assertThat(cu.getMarkers().findFirst(GradleProject.class)).hasValueSatisfying(marker ->
                assertThat(marker.getPlugins()).singleElement().satisfies(plugin -> {
                    assertThat(plugin.getId()).isEqualTo("com.netflix.nebula.optional-base");
                    assertThat(plugin.getFullyQualifiedClassName()).isEqualTo("unknown");
                })))
          )
        );
    }

    @Test
    void groovyBuildVersion() {
        rewriteRun(
          buildGradle(
            """
              plugins { id 'nebula.optional-base' version "7.0.0" }
              """,
            """
              plugins { id 'com.netflix.nebula.optional-base' version "7.0.0" }
              """,
            spec -> spec.markers(GradleProject.builder()
                .id(Tree.randomId())
                .plugins(singletonList(new GradlePluginDescriptor("legacy.Implementation", "nebula.optional-base")))
                .buildscript(GradleBuildscript.builder().build())
                .build())
              .afterRecipe(cu -> assertThat(cu.getMarkers().findFirst(GradleProject.class)).hasValueSatisfying(marker ->
                assertThat(marker.getPlugins()).singleElement().satisfies(plugin -> {
                    assertThat(plugin.getId()).isEqualTo("com.netflix.nebula.optional-base");
                    assertThat(plugin.getFullyQualifiedClassName()).isEqualTo("unknown");
                })))
          )
        );
    }

    @Test
    void groovySettingsVersionless() {
        rewriteRun(
          settingsGradle(
            """
              plugins { id 'nebula.optional-base' }
              """,
            """
              plugins { id 'com.netflix.nebula.optional-base' }
              """,
            spec -> spec.markers(GradleSettings.builder()
                .id(Tree.randomId())
                .plugins(singletonList(new GradlePluginDescriptor("legacy.Implementation", "nebula.optional-base")))
                .buildscript(GradleBuildscript.builder().build())
                .build())
              .afterRecipe(cu -> assertThat(cu.getMarkers().findFirst(GradleSettings.class)).hasValueSatisfying(marker ->
                assertThat(marker.getPlugins()).singleElement().satisfies(plugin -> {
                    assertThat(plugin.getId()).isEqualTo("com.netflix.nebula.optional-base");
                    assertThat(plugin.getFullyQualifiedClassName()).isEqualTo("unknown");
                })))
          )
        );
    }

    @Test
    void groovySettingsVersion() {
        rewriteRun(
          settingsGradle(
            """
              plugins { id 'nebula.optional-base' version "7.0.0" }
              """,
            """
              plugins { id 'com.netflix.nebula.optional-base' version "7.0.0" }
              """,
            spec -> spec.markers(GradleSettings.builder()
                .id(Tree.randomId())
                .plugins(singletonList(new GradlePluginDescriptor("legacy.Implementation", "nebula.optional-base")))
                .buildscript(GradleBuildscript.builder().build())
                .build())
              .afterRecipe(cu -> assertThat(cu.getMarkers().findFirst(GradleSettings.class)).hasValueSatisfying(marker ->
                assertThat(marker.getPlugins()).singleElement().satisfies(plugin -> {
                    assertThat(plugin.getId()).isEqualTo("com.netflix.nebula.optional-base");
                    assertThat(plugin.getFullyQualifiedClassName()).isEqualTo("unknown");
                })))
          )
        );
    }

    @Test
    void kotlinBuildVersionChange() {
        rewriteRun(
          spec -> spec.recipe(new ChangePlugin("nebula.optional-base", "com.netflix.nebula.optional-base", "9.0.0")),
          buildGradleKts(
            """
              plugins { id("nebula.optional-base") version "7.0.0" }
              """,
            """
              plugins { id("com.netflix.nebula.optional-base") version "9.0.0" }
              """,
            spec -> spec.markers(GradleProject.builder()
                .id(Tree.randomId())
                .plugins(singletonList(new GradlePluginDescriptor("legacy.Implementation", "nebula.optional-base")))
                .buildscript(GradleBuildscript.builder().build())
                .build())
              .afterRecipe(cu -> assertThat(cu.getMarkers().findFirst(GradleProject.class)).hasValueSatisfying(marker ->
                assertThat(marker.getPlugins()).singleElement().satisfies(plugin -> {
                    assertThat(plugin.getId()).isEqualTo("com.netflix.nebula.optional-base");
                    assertThat(plugin.getFullyQualifiedClassName()).isEqualTo("unknown");
                })))
          )
        );
    }

    @Test
    void groovySettingsVersionChange() {
        rewriteRun(
          spec -> spec.recipe(new ChangePlugin("nebula.optional-base", "com.netflix.nebula.optional-base", "9.0.0")),
          settingsGradle(
            """
              plugins { id 'nebula.optional-base' version "7.0.0" }
              """,
            """
              plugins { id 'com.netflix.nebula.optional-base' version "9.0.0" }
              """,
            spec -> spec.markers(GradleSettings.builder()
                .id(Tree.randomId())
                .plugins(singletonList(new GradlePluginDescriptor("legacy.Implementation", "nebula.optional-base")))
                .buildscript(GradleBuildscript.builder().build())
                .build())
              .afterRecipe(cu -> assertThat(cu.getMarkers().findFirst(GradleSettings.class)).hasValueSatisfying(marker ->
                assertThat(marker.getPlugins()).singleElement().satisfies(plugin -> {
                    assertThat(plugin.getId()).isEqualTo("com.netflix.nebula.optional-base");
                    assertThat(plugin.getFullyQualifiedClassName()).isEqualTo("unknown");
                })))
          )
        );
    }

    @Test
    void groovyApplyPlugin() {
        rewriteRun(
          buildGradle(
            """
              apply plugin: 'nebula.optional-base'
              """,
            """
              apply plugin: 'com.netflix.nebula.optional-base'
              """,
            spec -> spec.markers(GradleProject.builder()
                .id(Tree.randomId())
                .plugins(singletonList(new GradlePluginDescriptor("legacy.Implementation", "nebula.optional-base")))
                .buildscript(GradleBuildscript.builder().build())
                .build())
              .afterRecipe(cu -> assertThat(cu.getMarkers().findFirst(GradleProject.class)).hasValueSatisfying(marker ->
                assertThat(marker.getPlugins()).singleElement().satisfies(plugin -> {
                    assertThat(plugin.getId()).isEqualTo("com.netflix.nebula.optional-base");
                    assertThat(plugin.getFullyQualifiedClassName()).isEqualTo("unknown");
                })))
          )
        );
    }
}
