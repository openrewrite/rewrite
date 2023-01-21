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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;

class ChangeDependencyExtensionTest implements RewriteTest {

    @Test
    void worksWithEmptyStringConfig() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyExtension("org.openrewrite", "*", "war", "")),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
                
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  api 'org.openrewrite:rewrite-gradle:latest.integration@jar'
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
                  api 'org.openrewrite:rewrite-gradle:latest.integration@war'
              }
              """
          )
        );
    }

    @ParameterizedTest
    @CsvSource(value = {"org.openrewrite:rewrite-core", "*:*"}, delimiterString = ":")
    void findDependency(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyExtension(group, artifact, "war", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
                
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  api 'org.openrewrite:rewrite-core:latest.release@jar'
                  api "org.openrewrite:rewrite-core:latest.release@jar"
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
                  api 'org.openrewrite:rewrite-core:latest.release@war'
                  api "org.openrewrite:rewrite-core:latest.release@war"
              }
              """
          )
        );
    }

    @ParameterizedTest
    @CsvSource(value = {"org.openrewrite:rewrite-core", "*:*"}, delimiterString = ":")
    void findMapStyleDependency(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyExtension(group, artifact, "war", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
                
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', ext: 'jar'
                  api group: "org.openrewrite", name: "rewrite-core", version: "latest.release", ext: "jar"
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
                  api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', ext: 'war'
                  api group: "org.openrewrite", name: "rewrite-core", version: "latest.release", ext: "war"
              }
              """
          )
        );
    }

    @ParameterizedTest
    @CsvSource(value = {"org.openrewrite:rewrite-core", "*:*"}, delimiterString = ":")
    void worksWithoutVersion(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyExtension(group, artifact, "war", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
                
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  api 'org.openrewrite:rewrite-core@jar'
                  api "org.openrewrite:rewrite-core@jar"
                  api group: 'org.openrewrite', name: 'rewrite-core', ext: 'jar'
                  api group: "org.openrewrite", name: "rewrite-core", ext: "jar"
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
                  api 'org.openrewrite:rewrite-core@war'
                  api "org.openrewrite:rewrite-core@war"
                  api group: 'org.openrewrite', name: 'rewrite-core', ext: 'war'
                  api group: "org.openrewrite", name: "rewrite-core", ext: "war"
              }
              """
          )
        );
    }

    @ParameterizedTest
    @CsvSource(value = {"org.eclipse.jetty:jetty-servlet", "*:*"}, delimiterString = ":")
    void worksWithClassifier(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyExtension(group, artifact, "war", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
                
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  api 'org.eclipse.jetty:jetty-servlet:9.4.50.v20221201:tests@jar'
                  api "org.eclipse.jetty:jetty-servlet:9.4.50.v20221201:tests@jar"
                  api group: 'org.eclipse.jetty', name: 'jetty-servlet', version: '9.4.50.v20221201', classifier: 'tests', ext: 'jar'
                  api group: "org.eclipse.jetty", name: "jetty-servlet", version: "9.4.50.v20221201", classifier: "tests", ext: "jar"
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
                  api 'org.eclipse.jetty:jetty-servlet:9.4.50.v20221201:tests@war'
                  api "org.eclipse.jetty:jetty-servlet:9.4.50.v20221201:tests@war"
                  api group: 'org.eclipse.jetty', name: 'jetty-servlet', version: '9.4.50.v20221201', classifier: 'tests', ext: 'war'
                  api group: "org.eclipse.jetty", name: "jetty-servlet", version: "9.4.50.v20221201", classifier: "tests", ext: "war"
              }
              """
          )
        );
    }
}
