/*
 * Copyright 2021 the original author or authors.
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
import org.openrewrite.gradle.plugins.UpgradePluginVersion;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;

class UpgradePluginVersionTest implements RewriteTest {

    @Test
    void upgradePlugin() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion("com.jfrog.bintray", "latest.patch", null)),
          buildGradle(
            """
              plugins {
                  id 'com.jfrog.bintray' version '1.7.1'
                  id 'com.github.johnrengelman.shadow' version '6.1.0'
              }
              """,
            """
              plugins {
                  id 'com.jfrog.bintray' version '1.7.3'
                  id 'com.github.johnrengelman.shadow' version '6.1.0'
              }
              """
          )
        );
    }

    @Test
    void upgradePluginGlob() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion("com.jfrog.*", "1.8.X", null)),
          buildGradle(
            """
              plugins {
                  id 'com.jfrog.bintray' version '1.8.2'
              }
              """,
            """
              plugins {
                  id 'com.jfrog.bintray' version '1.8.5'
              }
              """
          )
        );
    }

    @Test
    void exactVersionDoesNotHaveToBeResolvable() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion("org.openrewrite.rewrite", "999.0", null)),
          buildGradle(
            """
              plugins {
                  id 'org.openrewrite.rewrite' version '5.34.0'
              }
              """,
            """
              plugins {
                  id 'org.openrewrite.rewrite' version '999.0'
              }
              """
          )
        );
    }
}
