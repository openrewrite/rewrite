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
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class UseLoggingGetLoggerTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        // Two passes that change the tree: the field is declared first, then the calls are pointed at it.
        spec.recipe(new UseLoggingGetLogger())
                .parser(JavaParser.fromJavaVersion().classpath("gradle-api"))
                .cycles(2)
                .expectedCyclesThatMakeChanges(2);
    }

    @DocumentExample("Give a plugin a logger of its own")
    @Test
    void replacesProjectLoggerInAPlugin() {
        rewriteRun(
          java(
            """
              import org.gradle.api.Plugin;
              import org.gradle.api.Project;

              public class MyPlugin implements Plugin<Project> {
                  @Override
                  public void apply(Project project) {
                      project.getLogger().lifecycle("applying");
                  }
              }
              """,
            """
              import org.gradle.api.Plugin;
              import org.gradle.api.Project;
              import org.gradle.api.logging.Logger;
              import org.gradle.api.logging.Logging;

              public class MyPlugin implements Plugin<Project> {
                  private static final Logger LOGGER = Logging.getLogger(MyPlugin.class);
                  @Override
                  public void apply(Project project) {
                      LOGGER.lifecycle("applying");
                  }
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenAlreadyUsingLoggingGetLogger() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(0),
          java(
            """
              import org.gradle.api.Plugin;
              import org.gradle.api.Project;
              import org.gradle.api.logging.Logger;
              import org.gradle.api.logging.Logging;

              public class MyPlugin implements Plugin<Project> {
                  private static final Logger LOGGER = Logging.getLogger(MyPlugin.class);
                  @Override
                  public void apply(Project project) {
                      LOGGER.lifecycle("applying");
                  }
              }
              """
          )
        );
    }

    /**
     * The guard is that the enclosing class is a `Plugin`; build logic that is not one belongs to other catalog
     * entries. Written as a plain class because `DefaultTask`'s inherited members do not resolve with only
     * `gradle-api` on the parser classpath.
     */
    @Test
    void noChangeOutsideAPlugin() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(0),
          java(
            """
              import org.gradle.api.Project;

              public class Helper {
                  void report(Project project) {
                      project.getLogger().lifecycle("reporting");
                  }
              }
              """
          )
        );
    }

    /**
     * Adding a second field of this name would not compile, so the class is left alone entirely.
     */
    @Test
    void noChangeWhenANonLoggerFieldTakesTheName() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(0),
          java(
            """
              import org.gradle.api.Plugin;
              import org.gradle.api.Project;

              public class MyPlugin implements Plugin<Project> {
                  private static final String LOGGER = "not a logger";

                  @Override
                  public void apply(Project project) {
                      project.getLogger().lifecycle(LOGGER);
                  }
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenThePluginDoesNotLog() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(0),
          java(
            """
              import org.gradle.api.Plugin;
              import org.gradle.api.Project;

              public class MyPlugin implements Plugin<Project> {
                  @Override
                  public void apply(Project project) {
                      project.getTasks().register("hello");
                  }
              }
              """
          )
        );
    }
}
