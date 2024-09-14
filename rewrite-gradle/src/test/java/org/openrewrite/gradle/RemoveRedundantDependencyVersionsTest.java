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
package org.openrewrite.gradle;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

class RemoveRedundantDependencyVersionsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
          .recipe(new RemoveRedundantDependencyVersions(null, null, null, null));
    }

    @DocumentExample
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
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("org.apache.commons:commons-lang3:3.14.0")
                  implementation(group: "org.apache.commons", name: "commons-lang3", version: "3.14.0")
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
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("org.apache.commons:commons-lang3")
                  implementation(group: "org.apache.commons", name: "commons-lang3")
              }
              """
          )
        );
    }
}
