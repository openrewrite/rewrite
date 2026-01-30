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
package org.openrewrite.groovy;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.SourceFile;
import org.openrewrite.groovy.format.GStringCurlyBraces;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.SourceSpecs.text;

class PlainTextAsGroovyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new PlainTextAsGroovy("**/Jenkinsfile*"));
    }

    @DocumentExample
    @Test
    void parsePlainTextAsGroovyThenApplyGroovyRecipe() {
        rewriteRun(
          spec -> spec.recipes(
            new PlainTextAsGroovy("**/Jenkinsfile*"),
            new GStringCurlyBraces()
          ),
          text(
            """
              def name = "world"
              println "Hello, $name!"
              """,
            """
              def name = "world"
              println "Hello, ${name}!"
              """,
            spec -> spec.path("Jenkinsfile")
          )
        );
    }

    @Test
    void convertsToGroovyCompilationUnit() {
        rewriteRun(
          spec -> spec.afterRecipe(run -> {
              SourceFile result = run.getChangeset().getAllResults().getFirst().getAfter();
              assertThat(result).isInstanceOf(G.CompilationUnit.class);
          }).recipes(
            new PlainTextAsGroovy("**/Jenkinsfile*"),
            new GStringCurlyBraces()
          ),
          text(
            """
              def name = "world"
              println "Hello, $name!"
              """,
            """
              def name = "world"
              println "Hello, ${name}!"
              """,
            spec -> spec.path("Jenkinsfile")
          )
        );
    }

    @Test
    void doesNotMatchWhenPatternDoesNotMatch() {
        rewriteRun(
          text(
            """
              def name = "world"
              println "Hello, $name!"
              """,
            spec -> spec.path("build.gradle")
          )
        );
    }

    @Test
    void parsesJenkinsfileSyntax() {
        rewriteRun(
          spec -> spec.recipes(
            new PlainTextAsGroovy("**/Jenkinsfile*"),
            new GStringCurlyBraces()
          ),
          text(
            """
              def msg = "Building..."
              pipeline {
                  agent any
                  stages {
                      stage('Build') {
                          steps {
                              echo "$msg"
                          }
                      }
                  }
              }
              """,
            """
              def msg = "Building..."
              pipeline {
                  agent any
                  stages {
                      stage('Build') {
                          steps {
                              echo "${msg}"
                          }
                      }
                  }
              }
              """,
            spec -> spec.path("Jenkinsfile")
          )
        );
    }
}
