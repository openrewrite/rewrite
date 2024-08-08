/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.gradle.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Tree;
import org.openrewrite.gradle.marker.GradlePluginDescriptor;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.test.SourceSpecs.text;

class FindPluginsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindPlugins("org.openrewrite.rewrite"));
    }

    @DocumentExample
    @Test
    void findPlugin() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()),
          buildGradle(
            """
              plugins {
                  id 'org.openrewrite.rewrite' version '6.18.0'
              }
              """,
            """
              plugins {
                  /*~~>*/id 'org.openrewrite.rewrite' version '6.18.0'
              }
              """,
            spec -> spec
              .beforeRecipe(cu -> assertThat(FindPlugins.find(cu, "org.openrewrite.rewrite"))
              .isNotEmpty()
              .anySatisfy(p -> {
                  assertThat(p.getPluginId()).isEqualTo("org.openrewrite.rewrite");
                  assertThat(p.getVersion()).isEqualTo("6.18.0");
              }))
          )
        );
    }

    @Test
    void findPluginFromGradleProjectMarker() {
        rewriteRun(
          text(
            "stand-in for a kotlin gradle script",
            "~~>stand-in for a kotlin gradle script",
            spec -> spec.markers(new GradleProject(
              Tree.randomId(),
              "group",
              "name",
              "version",
              "path",
              Collections.singletonList(new GradlePluginDescriptor("org.openrewrite.gradle.GradlePlugin", "org.openrewrite.rewrite")),
              Collections.emptyList(),
              Collections.emptyList(),
              Collections.emptyMap())
            )
          )
        );
    }
}
