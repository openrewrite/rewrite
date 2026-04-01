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
package org.openrewrite.gradle.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.settingsGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.*;

class ModuleHasPluginTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ModuleHasPlugin("org.openrewrite.rewrite", "org.openrewrite.gradle.RewritePlugin"));
    }

    @DocumentExample
    @Test
    void findPlugin() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()),
          mavenProject("multi-project-build",
            settingsGradle("""
              include 'project-applies-openrewrite-plugin'
              include 'other-project'
              """),
            mavenProject("project-applies-openrewrite-plugin",
              buildGradle(
                """
                  plugins {
                      id 'java'
                      id 'org.openrewrite.rewrite' version '6.18.0'
                  }
                  """,
                """
                  /*~~(Module has plugin: org.openrewrite.rewrite)~~>*/plugins {
                      id 'java'
                      id 'org.openrewrite.rewrite' version '6.18.0'
                  }
                  """
              ),
              srcMainJava(
                java("""
                    class A {}
                    """,
                  """
                    /*~~(Module has plugin: org.openrewrite.rewrite)~~>*/class A {}
                    """)
              )
            ),
            mavenProject("other-project",
              buildGradle(
                """
                  plugins {
                      id 'java'
                  }
                  """
              ),
              srcMainJava(
                java("""
                  class B {}
                  """)
              )
            )
          )
        );
    }
}
