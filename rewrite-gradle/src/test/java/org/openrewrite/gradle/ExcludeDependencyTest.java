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
    void attachesExcludeToDeclarationThatTransitivelyIncludesTarget() {
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

              dependencies {
                  implementation('org.apache.httpcomponents:httpclient:4.5.13') {
                      exclude group: 'commons-logging', module: 'commons-logging'
                  }
              }
              """
          )
        );
    }

    @Test
    void appendsExcludeToExistingClosure() {
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
                  implementation('org.apache.httpcomponents:httpclient:4.5.13') {
                      exclude group: 'org.apache.httpcomponents', module: 'httpcore'
                  }
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation('org.apache.httpcomponents:httpclient:4.5.13') {
                      exclude group: 'org.apache.httpcomponents', module: 'httpcore'
                      exclude group: 'commons-logging', module: 'commons-logging'
                  }
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

              dependencies {
                  implementation('org.apache.httpcomponents:httpclient:4.5.13') {
                      exclude group: 'commons-logging', module: 'commons-logging'
                  }
              }
              """
          )
        );
    }

    @Test
    void idempotentWhenGroupOnlyExcludeCoversTarget() {
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
                  implementation('org.apache.httpcomponents:httpclient:4.5.13') {
                      exclude group: 'commons-logging'
                  }
              }
              """
          )
        );
    }

    @Test
    void idempotentWhenModuleOnlyExcludeCoversTarget() {
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
                  implementation('org.apache.httpcomponents:httpclient:4.5.13') {
                      exclude module: 'commons-logging'
                  }
              }
              """
          )
        );
    }

    @Test
    void idempotentWhenTransitiveFalse() {
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
                  implementation('org.apache.httpcomponents:httpclient:4.5.13') {
                      transitive = false
                  }
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
    void noOpOnDirectDeclarationOfExcludedTarget() {
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
                  implementation 'commons-logging:commons-logging:1.2'
              }
              """
          )
        );
    }

    @Test
    void attachesToOtherDeclarationsEvenWhenTargetAlsoDeclaredDirectly() {
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
                  implementation 'commons-logging:commons-logging:1.2'
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

              dependencies {
                  implementation 'commons-logging:commons-logging:1.2'
                  implementation('org.apache.httpcomponents:httpclient:4.5.13') {
                      exclude group: 'commons-logging', module: 'commons-logging'
                  }
              }
              """
          )
        );
    }

    @Test
    void configurationFilterLimitsWhichDeclarationsAreTouched() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeDependency("commons-logging", "commons-logging", "api")),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  api 'org.apache.httpcomponents:httpclient:4.5.13'
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

              dependencies {
                  api('org.apache.httpcomponents:httpclient:4.5.13') {
                      exclude group: 'commons-logging', module: 'commons-logging'
                  }
                  implementation 'org.apache.httpcomponents:httpclient:4.5.13'
              }
              """
          )
        );
    }

    @Test
    void noOpInBuildscriptClasspath() {
        rewriteRun(
          buildGradle(
            """
              buildscript {
                  repositories {
                      mavenCentral()
                  }
                  dependencies {
                      classpath 'org.apache.httpcomponents:httpclient:4.5.13'
                  }
              }

              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }
              """
          )
        );
    }

    @Test
    void mapNotationDeclarationGroovy() {
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
                  implementation group: 'org.apache.httpcomponents', name: 'httpclient', version: '4.5.13'
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(group: 'org.apache.httpcomponents', name: 'httpclient', version: '4.5.13') {
                      exclude group: 'commons-logging', module: 'commons-logging'
                  }
              }
              """
          )
        );
    }

    // ------------- Kotlin DSL -------------

    @Test
    void kotlinDslAttachesExcludeToDeclaration() {
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

              dependencies {
                  implementation("org.apache.httpcomponents:httpclient:4.5.13") {
                      exclude(group = "commons-logging", module = "commons-logging")
                  }
              }
              """
          )
        );
    }

    @Test
    void kotlinDslAppendsExcludeToExistingClosure() {
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
                  implementation("org.apache.httpcomponents:httpclient:4.5.13") {
                      exclude(group = "org.apache.httpcomponents", module = "httpcore")
                  }
              }
              """,
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation("org.apache.httpcomponents:httpclient:4.5.13") {
                      exclude(group = "org.apache.httpcomponents", module = "httpcore")
                      exclude(group = "commons-logging", module = "commons-logging")
                  }
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

              dependencies {
                  implementation("org.apache.httpcomponents:httpclient:4.5.13") {
                      exclude(group = "commons-logging", module = "commons-logging")
                  }
              }
              """
          )
        );
    }

    @Test
    void kotlinDslIdempotentWhenIsTransitiveFalse() {
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
                  implementation("org.apache.httpcomponents:httpclient:4.5.13") {
                      isTransitive = false
                  }
              }
              """
          )
        );
    }

    @Test
    void kotlinDslConfigurationFilter() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeDependency("commons-logging", "commons-logging", "api")),
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  api("org.apache.httpcomponents:httpclient:4.5.13")
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

              dependencies {
                  api("org.apache.httpcomponents:httpclient:4.5.13") {
                      exclude(group = "commons-logging", module = "commons-logging")
                  }
                  implementation("org.apache.httpcomponents:httpclient:4.5.13")
              }
              """
          )
        );
    }
}
