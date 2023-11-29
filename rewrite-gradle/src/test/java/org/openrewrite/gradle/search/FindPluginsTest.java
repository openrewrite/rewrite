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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;

class FindPluginsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindPlugins("com.jfrog.bintray"));
    }

    @DocumentExample
    @Test
    void findPlugin() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'com.jfrog.bintray'
                  id 'com.jfrog.bintray' version '1.8.5'
              }
              """,
            """
              plugins {
                  /*~~>*/id 'com.jfrog.bintray'
                  /*~~>*/id 'com.jfrog.bintray' version '1.8.5'
              }
              """,
            spec -> spec.beforeRecipe(cu -> assertThat(FindPlugins.find(cu, "com.jfrog.bintray"))
              .isNotEmpty()
              .anySatisfy(p -> {
                  assertThat(p.getPluginId()).isEqualTo("com.jfrog.bintray");
                  assertThat(p.getVersion()).isEqualTo("1.8.5");
              }))
          )
        );
    }
}
