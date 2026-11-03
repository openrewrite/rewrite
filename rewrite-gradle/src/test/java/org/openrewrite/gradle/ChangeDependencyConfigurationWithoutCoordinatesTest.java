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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;

class ChangeDependencyConfigurationWithoutCoordinatesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeDependencyConfiguration("*", "*", "testImplementation", "testCompile"));
    }

    @DocumentExample
    @Test
    void files() {
        rewriteRun(
          buildGradle(
            """
              dependencies {
                  testCompile files('x.jar')
              }
              """,
            """
              dependencies {
                  testImplementation files('x.jar')
              }
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "fileTree('libs')",
      "gradleTestKit()",
      "gradleApi()",
      "localGroovy()",
      "v",
      "libs.foo",
      "sourceSets.main.output",
      "configurations.foo",
      "files('x.jar'), files('y.jar')"
    })
    void groovyNotation(String notation) {
        rewriteRun(
          buildGradle(
            """
              def v = 'a:b:1'
              dependencies {
                  testCompile %s
              }
              """.formatted(notation),
            """
              def v = 'a:b:1'
              dependencies {
                  testImplementation %s
              }
              """.formatted(notation)
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"files(\"x.jar\")", "fileTree(\"libs\")", "gradleTestKit()", "v", "libs.foo"})
    void kotlinNotation(String notation) {
        rewriteRun(
          buildGradleKts(
            """
              val v = "a:b:1"
              dependencies {
                  testCompile(%s)
              }
              """.formatted(notation),
            """
              val v = "a:b:1"
              dependencies {
                  testImplementation(%s)
              }
              """.formatted(notation)
          )
        );
    }

    @Test
    void withinSubprojectsAndBuildscript() {
        rewriteRun(
          buildGradle(
            """
              buildscript {
                  dependencies {
                      testCompile files('x.jar')
                  }
              }
              subprojects {
                  dependencies {
                      testCompile files('x.jar')
                  }
              }
              """,
            """
              buildscript {
                  dependencies {
                      testImplementation files('x.jar')
                  }
              }
              subprojects {
                  dependencies {
                      testImplementation files('x.jar')
                  }
              }
              """
          )
        );
    }

    @Test
    void specificCoordinatesCannotMatch() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyConfiguration("org.example", "*", "testImplementation", "testCompile")),
          buildGradle(
            """
              dependencies {
                  testCompile files('x.jar')
              }
              """
          )
        );
    }

    @Test
    void wildcardArtifactOfAGroupCannotMatch() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyConfiguration("*", "guava", "testImplementation", "testCompile")),
          buildGradle(
            """
              dependencies {
                  testCompile files('x.jar')
              }
              """
          )
        );
    }

    @Test
    void noNamedConfigurationCannotMatch() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyConfiguration("*", "*", "testImplementation", null)),
          buildGradle(
            """
              dependencies {
                  testCompile files('x.jar')
              }
              """
          )
        );
    }

    @Test
    void otherConfigurationIsUnchanged() {
        rewriteRun(
          buildGradle(
            """
              dependencies {
                  compile files('x.jar')
              }
              """
          )
        );
    }

    @Test
    void outsideDependenciesIsUnchanged() {
        rewriteRun(
          buildGradle(
            """
              def testCompile(Object o) { o }
              testCompile files('x.jar')
              """
          )
        );
    }

    @Test
    void constraintsAreUnchanged() {
        rewriteRun(
          buildGradle(
            """
              dependencies {
                  constraints {
                      testCompile v
                  }
              }
              """
          )
        );
    }

    @Test
    void anotherReceiverIsUnchanged() {
        rewriteRun(
          buildGradle(
            """
              dependencies {
                  helper.testCompile files('x.jar')
              }
              """
          )
        );
    }
}
