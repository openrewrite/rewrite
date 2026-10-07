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
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

class ExcludeDependencyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
          .recipe(new ExcludeDependency("commons-logging", "commons-logging", null));
    }

    @DocumentExample
    @Test
    void addsConfigurationsAllBlockWhenNoneExists() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'org.apache.httpcomponents:httpclient:4.5.13'
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              configurations.all {
                  exclude group: 'commons-logging', module: 'commons-logging'
              }

              dependencies {
                  implementation 'org.apache.httpcomponents:httpclient:4.5.13'
              }
              """
          )
        );
    }

    @Test
    void mergesIntoExistingConfigurationsAllBlock() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              configurations.all {
                  exclude group: 'org.apache.httpcomponents', module: 'httpcore'
              }

              dependencies {
                  implementation 'org.apache.httpcomponents:httpclient:4.5.13'
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              configurations.all {
                  exclude group: 'org.apache.httpcomponents', module: 'httpcore'
                  exclude group: 'commons-logging', module: 'commons-logging'
              }

              dependencies {
                  implementation 'org.apache.httpcomponents:httpclient:4.5.13'
              }
              """
          )
        );
    }

    @Test
    void idempotentWhenExcludeAlreadyPresent() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              configurations.all {
                  exclude group: 'commons-logging', module: 'commons-logging'
              }

              dependencies {
                  implementation 'org.apache.httpcomponents:httpclient:4.5.13'
              }
              """
          )
        );
    }

    @Test
    void noOpWhenTargetModuleNotTransitivelyPresent() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'com.google.guava:guava:30.1.1-jre'
              }
              """
          )
        );
    }

    @Test
    void expandsGlobsAgainstTheResolvedDependencyGraph() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeDependency("commons-*", "commons-*", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'org.apache.httpcomponents:httpclient:4.5.13'
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              configurations.all {
                  exclude group: 'commons-logging', module: 'commons-logging'
                  exclude group: 'commons-codec', module: 'commons-codec'
              }

              dependencies {
                  implementation 'org.apache.httpcomponents:httpclient:4.5.13'
              }
              """
          )
        );
    }

    @Test
    void bareGroupWildcardEmitsModuleOnlyExclude() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeDependency("*", "commons-logging", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'org.apache.httpcomponents:httpclient:4.5.13'
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              configurations.all {
                  exclude module: 'commons-logging'
              }

              dependencies {
                  implementation 'org.apache.httpcomponents:httpclient:4.5.13'
              }
              """
          )
        );
    }

    @Test
    void bareArtifactWildcardEmitsGroupOnlyExclude() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeDependency("commons-logging", "*", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'org.apache.httpcomponents:httpclient:4.5.13'
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              configurations.all {
                  exclude group: 'commons-logging'
              }

              dependencies {
                  implementation 'org.apache.httpcomponents:httpclient:4.5.13'
              }
              """
          )
        );
    }

    @Test
    void bareGroupAndArtifactWildcardIsNoOp() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeDependency("*", "*", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'org.apache.httpcomponents:httpclient:4.5.13'
              }
              """
          )
        );
    }

    @Test
    void existingGroupOnlyExcludeCoversSpecificTarget() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              configurations.all {
                  exclude group: 'commons-logging'
              }

              dependencies {
                  implementation 'org.apache.httpcomponents:httpclient:4.5.13'
              }
              """
          )
        );
    }

    @Test
    void kotlinDslBareGroupWildcardEmitsModuleOnlyExclude() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeDependency("*", "commons-logging", null)),
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation("org.apache.httpcomponents:httpclient:4.5.13")
              }
              """,
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              configurations.all {
                  exclude(module = "commons-logging")
              }

              dependencies {
                  implementation("org.apache.httpcomponents:httpclient:4.5.13")
              }
              """
          )
        );
    }

    @Test
    void kotlinDslAddsConfigurationsAllBlockWhenNoneExists() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation("org.apache.httpcomponents:httpclient:4.5.13")
              }
              """,
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              configurations.all {
                  exclude(group = "commons-logging", module = "commons-logging")
              }

              dependencies {
                  implementation("org.apache.httpcomponents:httpclient:4.5.13")
              }
              """
          )
        );
    }

    @Test
    void kotlinDslMergesIntoExistingConfigurationsAllBlock() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              configurations.all {
                  exclude(group = "org.apache.httpcomponents", module = "httpcore")
              }

              dependencies {
                  implementation("org.apache.httpcomponents:httpclient:4.5.13")
              }
              """,
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              configurations.all {
                  exclude(group = "org.apache.httpcomponents", module = "httpcore")
                  exclude(group = "commons-logging", module = "commons-logging")
              }

              dependencies {
                  implementation("org.apache.httpcomponents:httpclient:4.5.13")
              }
              """
          )
        );
    }

    @Test
    void kotlinDslIdempotentWhenExcludeAlreadyPresent() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              configurations.all {
                  exclude(group = "commons-logging", module = "commons-logging")
              }

              dependencies {
                  implementation("org.apache.httpcomponents:httpclient:4.5.13")
              }
              """
          )
        );
    }

    @Test
    void kotlinDslUsesNamedForSpecificConfiguration() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeDependency("commons-logging", "commons-logging", "runtimeClasspath")),
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation("org.apache.httpcomponents:httpclient:4.5.13")
              }
              """,
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              configurations.named("runtimeClasspath") {
                  exclude(group = "commons-logging", module = "commons-logging")
              }

              dependencies {
                  implementation("org.apache.httpcomponents:httpclient:4.5.13")
              }
              """
          )
        );
    }

    @Test
    void addsNamedConfigurationBlockWhenConfigurationSpecified() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeDependency("commons-logging", "commons-logging", "runtimeClasspath")),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'org.apache.httpcomponents:httpclient:4.5.13'
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              configurations.runtimeClasspath {
                  exclude group: 'commons-logging', module: 'commons-logging'
              }

              dependencies {
                  implementation 'org.apache.httpcomponents:httpclient:4.5.13'
              }
              """
          )
        );
    }
}
