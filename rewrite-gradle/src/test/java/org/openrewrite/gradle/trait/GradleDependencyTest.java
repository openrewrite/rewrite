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
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.gradle.trait.Traits.gradleDependency;

class GradleDependencyTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .beforeRecipe(withToolingApi())
          .recipe(RewriteTest.toRecipe(() -> gradleDependency().asVisitor(dep ->
            SearchResult.found(dep.getTree(), dep.getResolvedDependency().getGav().toString()))));
    }

    @Test
    void literal() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java"
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation "com.google.guava:guava:28.2-jre"
              }
              """,
            """
              plugins {
                  id "java"
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  /*~~(com.google.guava:guava:28.2-jre)~~>*/implementation "com.google.guava:guava:28.2-jre"
              }
              """
          )
        );
    }

    @Test
    void groovyString() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java"
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  def version = "28.2-jre"
                  implementation "com.google.guava:guava:${version}"
              }
              """,
            """
              plugins {
                  id "java"
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  def version = "28.2-jre"
                  /*~~(com.google.guava:guava:28.2-jre)~~>*/implementation "com.google.guava:guava:${version}"
              }
              """
          )
        );
    }

    @Test
    void groovyMapEntry() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java"
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation group: "com.google.guava", name: "guava", version: "28.2-jre"
              }
              """,
            """
              plugins {
                  id "java"
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  /*~~(com.google.guava:guava:28.2-jre)~~>*/implementation group: "com.google.guava", name: "guava", version: "28.2-jre"
              }
              """
          )
        );
    }

    @Test
    void platform() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java"
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation(platform("com.google.guava:guava:28.2-jre"))
              }
              """,
            """
              plugins {
                  id "java"
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  /*~~(com.google.guava:guava:28.2-jre)~~>*/implementation(platform("com.google.guava:guava:28.2-jre"))
              }
              """
          )
        );
    }

    @Test
    void enforcedPlatform() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java"
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation(enforcedPlatform("com.google.guava:guava:28.2-jre"))
              }
              """,
            """
              plugins {
                  id "java"
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  /*~~(com.google.guava:guava:28.2-jre)~~>*/implementation(enforcedPlatform("com.google.guava:guava:28.2-jre"))
              }
              """
          )
        );
    }
}
