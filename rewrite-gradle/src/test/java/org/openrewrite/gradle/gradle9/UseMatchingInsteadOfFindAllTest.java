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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;

class UseMatchingInsteadOfFindAllTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseMatchingInsteadOfFindAll());
    }

    @DocumentExample
    @Test
    void tasksFindAll() {
        rewriteRun(
          buildGradle(
            """
              def checkTasks = tasks.findAll { it.name.startsWith("check") }
              """,
            """
              def checkTasks = tasks.matching { it.name.startsWith("check") }
              """
          )
        );
    }

    @Test
    void configurationsFindAll() {
        rewriteRun(
          buildGradle(
            """
              def resolvable = configurations.findAll { it.canBeResolved }
              """,
            """
              def resolvable = configurations.matching { it.canBeResolved }
              """
          )
        );
    }

    @Test
    void sourceSetsFindAll() {
        rewriteRun(
          buildGradle(
            """
              def mains = sourceSets.findAll { it.name.contains("main") }
              """,
            """
              def mains = sourceSets.matching { it.name.contains("main") }
              """
          )
        );
    }

    @Test
    void chainedThroughWithType() {
        rewriteRun(
          buildGradle(
            """
              def slowTests = tasks.withType(Test).findAll { it.name.contains("integration") }
              """,
            """
              def slowTests = tasks.withType(Test).matching { it.name.contains("integration") }
              """
          )
        );
    }

    @Test
    void projectTasksFieldAccess() {
        rewriteRun(
          buildGradle(
            """
              def checkTasks = project.tasks.findAll { it.name.startsWith("check") }
              """,
            """
              def checkTasks = project.tasks.matching { it.name.startsWith("check") }
              """
          )
        );
    }

    @Nested
    class NoChange {

        @Test
        void subprojectsLeftAlone() {
            // `subprojects` is a Set<Project>, not a DomainObjectCollection
            rewriteRun(
              buildGradle(
                """
                  def withTests = subprojects.findAll { it.file("src/test").exists() }
                  """
              )
            );
        }

        @Test
        void unknownReceiverLeftAlone() {
            rewriteRun(
              buildGradle(
                """
                  def evens = [1, 2, 3, 4].findAll { it % 2 == 0 }
                  """
              )
            );
        }

        @Test
        void findAllWithoutClosureLeftAlone() {
            rewriteRun(
              buildGradle(
                """
                  def all = tasks.findAll()
                  """
              )
            );
        }

        @Test
        void alreadyUsingMatching() {
            rewriteRun(
              buildGradle(
                """
                  def checkTasks = tasks.matching { it.name.startsWith("check") }
                  """
              )
            );
        }

        @Test
        void kotlinDslFilterLeftAlone() {
            rewriteRun(
              buildGradleKts(
                """
                  val checkTasks = tasks.filter { it.name.startsWith("check") }
                  """
              )
            );
        }
    }
}
