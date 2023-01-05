/*
 * Copyright 2022 the original author or authors.
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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;

class AddBuildPluginTest implements RewriteTest {
    @Test
    void addPluginWithoutVersionToNewBlock() {
        rewriteRun(
          spec -> spec.recipe(new AddBuildPlugin("java-library", null, null)),
          buildGradle(
            "",
            """
              plugins {
                  id 'java-library'
              }
              """
          )
        );
    }

    @Test
    void addPluginWithoutVersionToExistingBlock() {
        rewriteRun(
          spec -> spec.recipe(new AddBuildPlugin("java-library", null, null)),
          buildGradle(
            """
              plugins {
                  id "java"
              }
              """,
            """
              plugins {
                  id "java"
                  id "java-library"
              }
              """
          )
        );
    }

    @Test
    void addPluginWithVersionToNewBlock() {
        rewriteRun(
          spec -> spec.recipe(new AddBuildPlugin("com.jfrog.bintray", "1.0", null)),
          buildGradle(
            "",
            """
              plugins {
                  id 'com.jfrog.bintray' version '1.0'
              }
              """
          )
        );
    }

    @Test
    void addPluginWithVersionToExistingBlock() {
        rewriteRun(
          spec -> spec.recipe(new AddBuildPlugin("com.jfrog.bintray", "1.0", null)),
          buildGradle(
            """
              plugins {
                  id "java"
              }
              """,
            """
              plugins {
                  id "java"
                  id "com.jfrog.bintray" version "1.0"
              }
              """
          )
        );
    }
}
