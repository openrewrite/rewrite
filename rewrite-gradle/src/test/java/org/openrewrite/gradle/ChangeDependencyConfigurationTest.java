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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;

class ChangeDependencyConfigurationTest implements RewriteTest {
    @DocumentExample
    @Test
    void changeConfiguration() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyConfiguration("org.openrewrite", "*", "implementation", "")),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
                
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  api 'org.openrewrite:rewrite-gradle:latest.release'
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
                  implementation 'org.openrewrite:rewrite-gradle:latest.release'
              }
              """
          )
        );
    }

    @Test
    void worksWithEmptyStringConfig() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyConfiguration("org.openrewrite", "*", "implementation", "")),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
                
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  api 'org.openrewrite:rewrite-gradle:latest.release'
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
                  implementation 'org.openrewrite:rewrite-gradle:latest.release'
              }
              """
          )
        );
    }

    @ParameterizedTest
    @CsvSource(value = {"org.openrewrite:rewrite-core", "*:*"}, delimiterString = ":")
    void changeStringStyleDependency(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyConfiguration(group, artifact, "implementation", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  api 'org.openrewrite:rewrite-core:latest.release'
                  api "org.openrewrite:rewrite-core:latest.release"
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
                  implementation 'org.openrewrite:rewrite-core:latest.release'
                  implementation "org.openrewrite:rewrite-core:latest.release"
              }
              """
          )
        );
    }

    @ParameterizedTest
    @CsvSource(value = {"org.openrewrite:rewrite-core", "*:*"}, delimiterString = ":")
    void changeMapStyleDependency(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyConfiguration(group, artifact, "implementation", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release'
                  api group: "org.openrewrite", name: "rewrite-core", version: "latest.release"
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
                  implementation group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release'
                  implementation group: "org.openrewrite", name: "rewrite-core", version: "latest.release"
              }
              """
          )
        );
    }

    @ParameterizedTest
    @CsvSource(value = {"org.openrewrite:rewrite-core", "*:*"}, delimiterString = ":")
    void changeGStringStyleDependency(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyConfiguration(group, artifact, "implementation", null)),
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
                  api "org.openrewrite:rewrite-core:${version}"
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
                  implementation "org.openrewrite:rewrite-core:${version}"
              }
              """
          )
        );
    }

    @ParameterizedTest
    @CsvSource(value = {"*:a", "*:*"}, delimiterString = ":")
    void worksForProjectDependencies(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyConfiguration(group, artifact, "implementation", null)),
          buildGradle(
            """
              dependencies {
                  compile project(":a")
              }
              """,
            """
              dependencies {
                  implementation project(":a")
              }
              """
          )
        );
    }

    @Test
    void onlyChangeSpecificDependency() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyConfiguration("org.openrewrite", "rewrite-core", "implementation", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release'
                  testImplementation group: "org.openrewrite", name: "rewrite-test", version: "latest.release"
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
                  implementation group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release'
                  testImplementation group: "org.openrewrite", name: "rewrite-test", version: "latest.release"
              }
              """
          )
        );
    }
}
