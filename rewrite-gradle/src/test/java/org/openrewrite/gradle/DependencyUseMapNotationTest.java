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
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

class DependencyUseMapNotationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
          .recipe(new DependencyUseMapNotation());
    }

    @DocumentExample
    @Test
    void basicString() {
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
                  api('org.openrewrite:rewrite-core:latest.release')
                  implementation "org.openrewrite:rewrite-core:latest.release"
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
                  api(group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release')
                  implementation group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release'
              }
              """
          )
        );
    }

    @Test
    void kotlinSupport() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              val version = "latest.release"

              dependencies {
                  api("org.openrewrite:rewrite-core:latest.release:sources")
                  implementation("org.openrewrite:rewrite-core:$version")
              }
              """,
              """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              val version = "latest.release"

              dependencies {
                  api(group = "org.openrewrite", name = "rewrite-core", version = "latest.release", classifier = "sources")
                  implementation(group = "org.openrewrite", name = "rewrite-core", version = version)
              }
              """
          )
        );
    }

    @Test
    void withGString() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              def version = "latest.release"
              dependencies {
                  api("org.openrewrite:rewrite-core:$version")
                  implementation "org.openrewrite:rewrite-gradle:$version"
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              def version = "latest.release"
              dependencies {
                  api(group: 'org.openrewrite', name: 'rewrite-core', version: version)
                  implementation group: 'org.openrewrite', name: 'rewrite-gradle', version: version
              }
              """
          )
        );
    }

    @Test
    void withExclusion() {
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
                  api("org.openrewrite:rewrite-core:latest.release") {
                      exclude group: "org.openrewrite", module: "rewrite-gradle"
                  }
                  implementation "org.openrewrite:rewrite-gradle:latest.release", {
                      exclude group: "org.openrewrite", module: "rewrite-core"
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
                  api(group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release') {
                      exclude group: "org.openrewrite", module: "rewrite-gradle"
                  }
                  implementation group: 'org.openrewrite', name: 'rewrite-gradle', version: 'latest.release', {
                      exclude group: "org.openrewrite", module: "rewrite-core"
                  }
              }
              """
          )
        );
    }

    @Test
    void withGStringAndExclusion() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              def version = "latest.release"
              dependencies {
                  api("org.openrewrite:rewrite-core:$version") {
                      exclude group: "org.openrewrite", module: "rewrite-gradle"
                  }
                  implementation "org.openrewrite:rewrite-gradle:$version", {
                      exclude group: "org.openrewrite", module: "rewrite-core"
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

              def version = "latest.release"
              dependencies {
                  api(group: 'org.openrewrite', name: 'rewrite-core', version: version) {
                      exclude group: "org.openrewrite", module: "rewrite-gradle"
                  }
                  implementation group: 'org.openrewrite', name: 'rewrite-gradle', version: version, {
                      exclude group: "org.openrewrite", module: "rewrite-core"
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2805")
    @Test
    void withoutVersion() {
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
                implementation(platform("org.openrewrite.recipe:rewrite-recipe-bom:latest.release"))
                implementation "org.openrewrite.recipe:rewrite-logging-frameworks"
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
                implementation(platform("org.openrewrite.recipe:rewrite-recipe-bom:latest.release"))
                implementation group: 'org.openrewrite.recipe', name: 'rewrite-logging-frameworks'
              }
              """
          )
        );
    }

    @Test
    void withClassifierAndExtension() {
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
                implementation "org.openrewrite:rewrite-core:latest.release:tests@jar"
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
                implementation group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', classifier: 'tests', ext: 'jar'
              }
              """
          )
        );
    }

    @Test
    void worksWithDependencyDefinedInJvmTestSuite() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java-library"
                  id 'jvm-test-suite'
              }

              repositories {
                  mavenCentral()
              }

              testing {
                  suites {
                      test {
                          dependencies {
                              implementation('org.openrewrite:rewrite-core:latest.release')
                          }
                      }
                  }
              }
              """,
            """
              plugins {
                  id "java-library"
                  id 'jvm-test-suite'
              }

              repositories {
                  mavenCentral()
              }

              testing {
                  suites {
                      test {
                          dependencies {
                              implementation(group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release')
                          }
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void worksWithDependencyDefinedInBuildScript() {
        rewriteRun(
          buildGradle(
            """
              buildscript {
                  repositories {
                      gradlePluginPortal()
                  }
                  dependencies {
                      classpath 'org.openrewrite:rewrite-core:latest.release'
                  }
              }
              """,
            """
              buildscript {
                  repositories {
                      gradlePluginPortal()
                  }
                  dependencies {
                      classpath group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release'
                  }
              }
              """
          )
        );
    }

    @Test
    void dependenciesBlockInFreestandingScript() {
        rewriteRun(
          buildGradle(
            """
              repositories {
                  mavenLocal()
                  mavenCentral()
                  maven {
                     url = uri("https://central.sonatype.com/repository/maven-snapshots")
                  }
              }
              dependencies {
                  implementation("org.openrewrite:rewrite-core:latest.release")
              }
              """,
            """
              repositories {
                  mavenLocal()
                  mavenCentral()
                  maven {
                     url = uri("https://central.sonatype.com/repository/maven-snapshots")
                  }
              }
              dependencies {
                  implementation(group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release')
              }
              """,
            spec -> spec.path("dependencies.gradle")
          ),
          buildGradle(
            """
              plugins {
                  id("java")
              }
              apply from: 'dependencies.gradle'
              """
          )
        );
    }
}
