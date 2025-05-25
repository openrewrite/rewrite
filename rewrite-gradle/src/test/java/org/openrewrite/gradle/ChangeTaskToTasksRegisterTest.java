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

class ChangeTaskToTasksRegisterTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeTaskToTasksRegister());
    }

    @DocumentExample
    @Test
    void basicTaskToTasksRegister() {
        rewriteRun(
          buildGradle(
            """
            task myCopyTask(type: Copy)
            """,
            """
            tasks.register("myCopyTask", Copy)
            """
          )
        );
    }

    @Test
    void taskNameAsLiteralString() {
        rewriteRun(
          buildGradle(
            """
            task "literalTask"(type: Jar) {
                archiveFileName = "my-app.jar"
            }
            """,
            """
            tasks.register("literalTask", Jar) {
                archiveFileName = "my-app.jar"
            }
            """
          )
        );
    }

    @Test
    void shouldNotChangeExistingTasksRegister() {
        rewriteRun(
          buildGradle(
            """
            tasks.register("alreadyLazy", Copy) {
            }
            """
          )
        );
    }

    @Test
    void shouldNotChangeTaskWithoutType() {
        rewriteRun(
          buildGradle(
            """
            task "simpleStringArgument"
            """
          )
        );
    }

    @Test
    void taskWithConfigurationClosure() {
        rewriteRun(
          buildGradle(
            """
            import org.gradle.api.tasks.Delete
            
            task closureTask(type: Delete) {
                description = 'Deletes the build directory.'
                delete rootProject.buildDir
            }
            """,
            """
            import org.gradle.api.tasks.Delete
            
            tasks.register("closureTask", Delete) {
                description = 'Deletes the build directory.'
                delete rootProject.buildDir
            }
            """
          )
        );
    }

    @Test
    void taskTypeIsFullyQualified() {
        rewriteRun(
          buildGradle(
            """
            task fullQualifiedTask(type: org.gradle.api.tasks.Copy) {
                from 'src/main/resources'
                into 'build/generated-resources'
            }
            """,
            """
            tasks.register("fullQualifiedTask", org.gradle.api.tasks.Copy) {
                from 'src/main/resources'
                into 'build/generated-resources'
            }
            """
          )
        );
    }

    @Test
    void taskTypeIsSimpleNameWithLocalClass() {
        rewriteRun(
          buildGradle(
            """
            class MyCustomTaskType extends DefaultTask {
            }
            
            task custom(type: MyCustomTaskType) {
                group = 'custom'
            }
            """,
            """
            class MyCustomTaskType extends DefaultTask {
            }
            
            tasks.register("custom", MyCustomTaskType) {
                group = 'custom'
            }
            """
          )
        );
    }

    @Test
    void shouldNotChangeOtherMethodsNamedTask() {
        rewriteRun(
          buildGradle(
            """
            class TaskHelper {
                void task(Map params, Closure cl) { /* ... */ }
            }
            def helper = new TaskHelper()
            helper.task(type: Copy) {
                // This is not a standard Gradle task definition
            }
            
            task realGradleTask(type: Delete)
            """,
            """
            class TaskHelper {
                void task(Map params, Closure cl) { /* ... */ }
            }
            def helper = new TaskHelper()
            helper.task(type: Copy) {
                // This is not a standard Gradle task definition
            }
            
            tasks.register("realGradleTask", Delete)
            """
          )
        );
    }

    @Test
    void preserveFormatting() {
        rewriteRun(
          buildGradle(
            """
            task "format preservation"(type: Exec) {
                group = "verification"
                description = "Runs tests and checks."
            
                // comments
                commandLine 'echo', "Formatted"
            }
            """,
            """
            tasks.register("format preservation", Exec) {
                group = "verification"
                description = "Runs tests and checks."
            
                // comments
                commandLine 'echo', "Formatted"
            }
            """
          )
        );
    }
}
