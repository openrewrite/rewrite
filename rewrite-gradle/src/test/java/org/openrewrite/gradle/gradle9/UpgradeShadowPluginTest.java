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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.gradle.Assertions.settingsGradleKts;

class UpgradeShadowPluginTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpgradeShadowPlugin());
    }

    @Test
    void originalGraphqlAnonymizerBuildScript() throws IOException {
        String original;
        try (InputStream in = requireNonNull(getClass().getResourceAsStream("/org/openrewrite/gradle/gradle9/shadow/build.gradle.kts"))) {
            original = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        rewriteRun(buildGradleKts(original,
          original.replace("com.github.johnrengelman.shadow", "com.gradleup.shadow").replace("6.1.0", "9.0.0")));
    }

    @Test
    void preserveVersionSelectors() {
        rewriteRun(buildGradleKts(
          "plugins { id(\"com.gradleup.shadow\") version \"9.+\" }"));
    }

    @Test
    void renameLegacyIdWithoutDowngrading() {
        rewriteRun(buildGradleKts(
          "plugins { id(\"com.github.johnrengelman.shadow\") version \"10.0.0\" }",
          "plugins { id(\"com.gradleup.shadow\") version \"10.0.0\" }"));
    }

    @Test
    void preserveExplicitUnresolvedReceiver() {
        rewriteRun(buildGradleKts(
          "plugins { custom.id(\"com.github.johnrengelman.shadow\").version(\"6.1.0\") }"));
    }

    @Test
    void preserveExplicitNonStringDsl() {
        rewriteRun(buildGradleKts(
          """
          class CustomPluginDsl {
              fun id(value: Int): CustomPluginDsl = this
              fun version(value: Int): CustomPluginDsl = this
          }
          val custom = CustomPluginDsl()
          plugins { custom.id(123).version(456) }
          """));
    }

    @Test
    void preserveExplicitTypedReceiver() {
        rewriteRun(buildGradleKts(
          """
          class CustomPluginDsl {
              fun id(value: String): CustomPluginDsl = this
              fun version(value: String): CustomPluginDsl = this
          }
          val custom = CustomPluginDsl()
          plugins { custom.id("com.github.johnrengelman.shadow").version("6.1.0") }
          """));
    }

    @Test
    void preserveExplicitGroovyReceiver() {
        rewriteRun(buildGradle(
          "plugins { custom.id('com.github.johnrengelman.shadow').version('6.1.0') }"));
    }

    @Test
    void legacyKotlin() {
        rewriteRun(buildGradleKts(
          "plugins { id(\"com.github.johnrengelman.shadow\") version \"6.1.0\" }",
          "plugins { id(\"com.gradleup.shadow\") version \"9.0.0\" }"));
    }

    @Test
    void legacyGroovy() {
        rewriteRun(buildGradle(
          "plugins { id 'com.github.johnrengelman.shadow' version '8.1.1' }",
          "plugins { id 'com.gradleup.shadow' version '9.0.0' }"));
    }

    @Test
    void maintainedNamespace() {
        rewriteRun(buildGradleKts(
          "plugins { id(\"com.gradleup.shadow\") version \"8.3.0\" apply false }",
          "plugins { id(\"com.gradleup.shadow\") version \"9.0.0\" apply false }"));
    }

    @Test
    void pluginManagement() {
        rewriteRun(settingsGradleKts(
          "pluginManagement { plugins { id(\"com.github.johnrengelman.shadow\") version \"7.1.2\" } }",
          "pluginManagement { plugins { id(\"com.gradleup.shadow\") version \"9.0.0\" } }"));
    }

    @Test
    void preserveNewerAndUnrelatedPlugins() {
        rewriteRun(buildGradleKts(
          """
          plugins {
              id("com.gradleup.shadow") version "9.0.0"
              id("com.gradleup.shadow") version "9.3.0"
              id("com.gradleup.shadow") version "10.0.0"
              id("other.shadow") version "1.0.0"
          }
          """));
    }

    @Test
    void preserveDynamicAndVersionlessDeclarations() {
        rewriteRun(buildGradleKts(
          """
          val shadowVersion = "6.1.0"
          plugins {
              id("com.github.johnrengelman.shadow") version shadowVersion
              id("com.gradleup.shadow")
          }
          """));
    }

    @Test
    void preserveNonPluginCalls() {
        rewriteRun(buildGradle(
          """
          dependencies { implementation 'com.github.johnrengelman.shadow:shadow:6.1.0' }
          custom { id('com.github.johnrengelman.shadow').version('6.1.0') }
          """));
    }
}
