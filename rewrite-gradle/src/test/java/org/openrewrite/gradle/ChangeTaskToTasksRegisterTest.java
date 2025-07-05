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
import static org.openrewrite.gradle.Assertions.buildGradleKts;

class ChangeTaskToTasksRegisterTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeTaskToTasksRegister());
    }

    @DocumentExample
    @Test
    void groovyTaskWithClosure() {
        rewriteRun(
          buildGradle(
            """
              task taskName(type: Copy) {
                  from 'src/main/resources'
                  into 'build/generated-resources'
              }
              """,
            """
              tasks.register("taskName", Copy) {
                  from 'src/main/resources'
                  into 'build/generated-resources'
              }
              """
          )
        );
    }

    @Test
    void groovyTaskWithoutClosure() {
        rewriteRun(
          buildGradle(
            """
              task taskName(type: Copy)
              """,
            """
              tasks.register("taskName", Copy)
              """
          )
        );
    }

    @Test
    void groovyTaskWithoutType() {
        rewriteRun(
          buildGradle(
            """
              task taskName {
                  doLast {
                      println "simple print"
                  }
              }
              """,
            """
              tasks.register("taskName") {
                  doLast {
                      println "simple print"
                  }
              }
              """
          )
        );
    }

    @Test
    void alreadyGroovyTasks() {
        rewriteRun(
          buildGradle(
            """
              tasks.register("taskName", Copy)
              """
          )
        );
    }

    @Test
    void groovyTaskWithImport() {
        rewriteRun(
          buildGradle(
            """
              import org.gradle.api.tasks.Delete
              
              task taskName(type: Delete) {
                  description = 'Deletes the build directory.'
                  delete rootProject.buildDir
              }
              """,
            """
              import org.gradle.api.tasks.Delete
              
              tasks.register("taskName", Delete) {
                  description = 'Deletes the build directory.'
                  delete rootProject.buildDir
              }
              """
          )
        );
    }

    @Test
    void groovyTaskWithFullyQualifiedType() {
        rewriteRun(
          buildGradle(
            """
              task taskName(type: org.gradle.api.tasks.Copy) {
                  from 'src/main/resources'
                  into 'build/generated-resources'
              }
              """,
            """
              tasks.register("taskName", org.gradle.api.tasks.Copy) {
                  from 'src/main/resources'
                  into 'build/generated-resources'
              }
              """
          )
        );
    }

    @Test
    void groovyTaskWithLocalClassType() {
        rewriteRun(
          buildGradle(
            """
              class MyCustomTaskType extends DefaultTask {
              }
              
              task taskName(type: MyCustomTaskType) {
                  group = 'custom'
              }
              """,
            """
              class MyCustomTaskType extends DefaultTask {
              }
              
              tasks.register("taskName", MyCustomTaskType) {
                  group = 'custom'
              }
              """
          )
        );
    }

    @Test
    void groovyTaskWithSelect() {
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
              """
          )
        );
    }

    @Test
    void groovyTaskWithCustomPluginConfiguration() {
        rewriteRun(
          buildGradle(
            """
              customPluginConfiguration.task taskName(type: Copy) {
                  from 'src/main/resources'
                  into 'build/generated-resources'
              }
              """
          )
        );
    }

    @Test
    void groovyTaskWithProject() {
        rewriteRun(
          buildGradle(
            """
              project.task taskName {
                   doLast {
                       println "Running 'This works' task in root project"
                   }
              }
              """,
            """
              project.tasks.register("taskName") {
                   doLast {
                       println "Running 'This works' task in root project"
                   }
              }
              """
          )
        );
    }

    @Test
    void groovyTaskInSubprojectsWithIt() {
        rewriteRun(
          buildGradle(
            """
              subprojects {
                  it.task subTask {
                      doLast {
                          println "Running in subproject"
                      }
                  }
              }
              """,
            """
              subprojects {
                  it.tasks.register("subTask") {
                      doLast {
                          println "Running in subproject"
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void groovyTaskInSubprojectsImplicitThis() {
        rewriteRun(
          buildGradle(
            """
              subprojects {
                  task subTask {
                      doLast {
                          println "Running in subproject"
                      }
                  }
              }
              """,
            """
              subprojects {
                  tasks.register("subTask") {
                      doLast {
                          println "Running in subproject"
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void groovyTaskInSubprojectsWithNamedParameter() {
        rewriteRun(
          buildGradle(
            """
              subprojects { p ->
                  p.task subTask {
                      doLast {
                          println "Running in subproject"
                      }
                  }
              }
              """,
            """
              subprojects { p ->
                  p.tasks.register("subTask") {
                      doLast {
                          println "Running in subproject"
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void groovyTaskWithCommentsAndBlankLine() {
        rewriteRun(
          buildGradle(
            """
              task taskName(type: Exec) {
                  group = "verification"
                  description = "Runs tests and checks."
              
                  // comments
                  commandLine 'echo', "Formatted"
              }
              """,
            """
              tasks.register("taskName", Exec) {
                  group = "verification"
                  description = "Runs tests and checks."
              
                  // comments
                  commandLine 'echo', "Formatted"
              }
              """
          )
        );
    }

    @Test
    void kotlinTaskWithClosure() {
        rewriteRun(
          buildGradleKts(
            """
              task<Copy>("taskName") {
                  from("src")
                  into("dest")
              }
              """,
            """
              tasks.register<Copy>("taskName") {
                  from("src")
                  into("dest")
              }
              """
          )
        );
    }

    @Test
    void kotlinTaskWithoutClosure() {
        rewriteRun(
          buildGradleKts(
            """
              task<Copy>("taskName")
              """,
            """
              tasks.register<Copy>("taskName")
              """
          )
        );
    }

    @Test
    void kotlinTaskWithoutType() {
        rewriteRun(
          buildGradleKts(
            """
              task("taskName") {
                  // simple task
              }
              """,
            """
              tasks.register("taskName") {
                  // simple task
              }
              """
          )
        );
    }

    @Test
    void alreadyKotlinTasks() {
        rewriteRun(
          buildGradleKts(
            """
              tasks.register<Copy>("taskName") {
                    from("src")
                    into("dest")
              }
              """
          )
        );
    }

    @Test
    void kotlinTaskWithCustomPluginConfiguration() {
        rewriteRun(
          buildGradleKts(
            """
              customPluginConfiguration.task<Copy>("taskName") {
                    from("src")
                    into("dest")
              }
              """
          )
        );
    }

    @Test
    void kotlinTaskWithProject() {
        rewriteRun(
          buildGradleKts(
            """
              project.task<Copy>("taskName") {
                   from("src")
                   into("dest")
              }
              """,
            """
              project.tasks.register<Copy>("taskName") {
                   from("src")
                   into("dest")
              }
              """
          )
        );
    }

    @Test
    void kotlinTaskInSubprojectsImplicitThis() {
        rewriteRun(
          buildGradleKts(
            """
              subprojects {
                  task("subTask") {
                      doLast {
                          println("Running in subproject")
                      }
                  }
              }
              """,
            """
              subprojects {
                  tasks.register("subTask") {
                      doLast {
                          println("Running in subproject")
                      }
                  }
              }
              """
          )
        );
    }
}
