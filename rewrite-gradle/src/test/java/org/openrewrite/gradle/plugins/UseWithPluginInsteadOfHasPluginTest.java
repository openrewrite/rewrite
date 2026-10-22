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
package org.openrewrite.gradle.plugins;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;

class UseWithPluginInsteadOfHasPluginTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseWithPluginInsteadOfHasPlugin());
    }

    @DocumentExample
    @Test
    void groovyDsl() {
        rewriteRun(
          buildGradle(
            """
              if (plugins.hasPlugin('java')) {
                  java {
                      withSourcesJar()
                  }
              }
              """,
            """
              pluginManager.withPlugin('java') {
                  java {
                      withSourcesJar()
                  }
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
              if (plugins.hasPlugin("java")) {
                  tasks.named("jar") {
                      enabled = false
                  }
              }
              """,
            """
              pluginManager.withPlugin("java") {
                  tasks.named("jar") {
                      enabled = false
                  }
              }
              """
          )
        );
    }

    @Test
    void qualifiedPluginManager() {
        rewriteRun(
          buildGradle(
            """
              subprojects {
                  if (project.pluginManager.hasPlugin("maven-publish")) {
                      publishing {
                          repositories {
                              mavenLocal()
                          }
                      }
                  }
              }
              """,
            """
              subprojects {
                  project.pluginManager.withPlugin("maven-publish") {
                      publishing {
                          repositories {
                              mavenLocal()
                          }
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void keepElseBranch() {
        rewriteRun(
          buildGradle(
            """
              if (plugins.hasPlugin('java')) {
                  println 'java'
              } else {
                  println 'not java'
              }
              """
          )
        );
    }

    @Test
    void keepNegatedCondition() {
        rewriteRun(
          buildGradle(
            """
              if (!plugins.hasPlugin('java')) {
                  println 'not java'
              }
              """
          )
        );
    }

    @Test
    void keepPluginClassCheck() {
        rewriteRun(
          buildGradle(
            """
              if (plugins.hasPlugin(JavaPlugin)) {
                  println 'java'
              }
              """
          )
        );
    }

    @Test
    void keepBodyReferringToShadowedName() {
        rewriteRun(
          buildGradle(
            """
              subprojects {
                  if (plugins.hasPlugin('java')) {
                      println "configuring ${name}"
                      it.version = '1.0'
                  }
              }
              """
          )
        );
    }

    @Test
    void keepBodyAssigningScriptVariable() {
        rewriteRun(
          buildGradle(
            """
              def isJava = false
              if (plugins.hasPlugin('java')) {
                  isJava = true
              }
              println isJava
              """
          )
        );
    }

    @Test
    void keepBodyThatReturns() {
        rewriteRun(
          buildGradle(
            """
              if (plugins.hasPlugin('java')) {
                  return
              }
              println 'not java'
              """
          )
        );
    }

    @Test
    void keepConditionUsedAsExpression() {
        rewriteRun(
          buildGradleKts(
            """
              val kind = if (plugins.hasPlugin("java")) {
                  "jvm"
              } else {
                  "other"
              }
              """
          )
        );
    }
}
