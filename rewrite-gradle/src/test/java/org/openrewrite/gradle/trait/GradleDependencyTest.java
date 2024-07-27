/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.gradle.trait;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.trait.Traits.gradleDependency;

class GradleDependencyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(RewriteTest.toRecipe(() -> gradleDependency().asVisitor(dep ->
          SearchResult.found(dep.getTree(), dep.getResolvedDependency().toString()))));
    }

    @Test
    @DocumentExample
    void dependency() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java-library"
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation project(':api')
                  implementation "commons-lang:commons-lang5:${commonsLangVersion}"

                  implementation "commons-lang:commons-lang3"
                  implementation "commons-lang:commons-lang:2.6"
                  implementation group: "commons-lang", name: "commons-lang", version: "2.6"
              }
              """,
            """
              plugins {
                  id "java-library"
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  /*~~(::api)~~>*/implementation project(':api')
                  /*~~(commons-lang:commons-lang5:${commonsLangVersion})~~>*/implementation "commons-lang:commons-lang5:${commonsLangVersion}"
              
                  /*~~(commons-lang:commons-lang3)~~>*/implementation "commons-lang:commons-lang3"
                  /*~~(commons-lang:commons-lang:2.6)~~>*/implementation "commons-lang:commons-lang:2.6"
                  /*~~(commons-lang:commons-lang:2.6)~~>*/implementation group: "commons-lang", name: "commons-lang", version: "2.6"
              }
              """
          )

        );
    }
}
