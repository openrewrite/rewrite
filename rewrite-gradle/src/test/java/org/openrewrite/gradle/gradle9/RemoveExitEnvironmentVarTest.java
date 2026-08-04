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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;

class RemoveExitEnvironmentVarTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveExitEnvironmentVar());
    }

    @DocumentExample
    @Test
    void removeFromStartScriptsTask() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'application'
              }

              startScripts {
                  exitEnvironmentVar = 'MY_APP_EXIT_CONST'
                  applicationName = 'myApp'
              }
              """,
            """
              plugins {
                  id 'application'
              }

              startScripts {
                  applicationName = 'myApp'
              }
              """
          )
        );
    }

    @Test
    void removeEmptiedConfigurationBlock() {
        rewriteRun(
          buildGradle(
            """
              startScripts {
                  exitEnvironmentVar = 'MY_APP_EXIT_CONST'
              }
              """,
            ""
          )
        );
    }

    @Test
    void withTypeTaskConfiguration() {
        rewriteRun(
          buildGradle(
            """
              tasks.withType(CreateStartScripts) {
                  defaultJvmOpts = ['-Xmx512m']
                  exitEnvironmentVar = 'MY_APP_EXIT_CONST'
              }
              """,
            """
              tasks.withType(CreateStartScripts) {
                  defaultJvmOpts = ['-Xmx512m']
              }
              """
          )
        );
    }

    @Test
    void customlyNamedStartScriptsTask() {
        rewriteRun(
          buildGradle(
            """
              tasks.register('adminStartScripts', CreateStartScripts) {
                  exitEnvironmentVar = 'MY_APP_EXIT_CONST'
                  mainClass = 'com.example.Admin'
              }
              """,
            """
              tasks.register('adminStartScripts', CreateStartScripts) {
                  mainClass = 'com.example.Admin'
              }
              """
          )
        );
    }

    @Test
    void setterInvocation() {
        rewriteRun(
          buildGradle(
            """
              startScripts {
                  setExitEnvironmentVar('MY_APP_EXIT_CONST')
                  applicationName = 'myApp'
              }
              """,
            """
              startScripts {
                  applicationName = 'myApp'
              }
              """
          )
        );
    }

    @Test
    void kotlinDsl() {
        rewriteRun(
          buildGradleKts(
            """
              tasks.startScripts {
                  exitEnvironmentVar = "MY_APP_EXIT_CONST"
                  applicationName = "myApp"
              }
              """,
            """
              tasks.startScripts {
                  applicationName = "myApp"
              }
              """
          )
        );
    }

    @Test
    void doesNotTouchUnrelatedExitEnvironmentVar() {
        rewriteRun(
          buildGradle(
            """
              myExtension {
                  exitEnvironmentVar = 'MY_APP_EXIT_CONST'
              }
              """
          )
        );
    }
}
