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

class ChangeDependencyArtifactIdTest implements RewriteTest {

    @Test
    void worksWithEmptyStringConfig() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyArtifactId("org.springframework.boot", "spring-boot-starter", "new-starter", "")),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation 'org.springframework.boot:spring-boot-starter:2.5.4'
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
                  implementation 'org.springframework.boot:new-starter:2.5.4'
              }
              """
          )
        );
    }

    @ParameterizedTest
    @CsvSource(value = {"org.openrewrite:rewrite-core", "*:*"}, delimiterString = ":")
    void findDependency(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyArtifactId(group, artifact, "dewrite-core", null)),
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
                  api 'org.openrewrite:dewrite-core:latest.release'
                  api "org.openrewrite:dewrite-core:latest.release"
              }
              """
          )
        );
    }

    @ParameterizedTest
    @CsvSource(value = {"org.openrewrite:rewrite-core", "*:*"}, delimiterString = ":")
    void findMapStyleDependency(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyArtifactId(group, artifact, "dewrite-core", null)),
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
                  api group: 'org.openrewrite', name: 'dewrite-core', version: 'latest.release'
                  api group: "org.openrewrite", name: "dewrite-core", version: "latest.release"
              }
              """
          )
        );
    }

    @ParameterizedTest
    @CsvSource(value ={"org.openrewrite:rewrite-core", "*:*"}, delimiterString = ":")
    void worksWithoutVersion(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyArtifactId(group, artifact, "dewrite-core", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
                
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  api 'org.openrewrite:rewrite-core'
                  api "org.openrewrite:rewrite-core"
                  api group: 'org.openrewrite', name: 'dewrite-core'
                  api group: "org.openrewrite", name: "dewrite-core"
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
                  api 'org.openrewrite:dewrite-core'
                  api "org.openrewrite:dewrite-core"
                  api group: 'org.openrewrite', name: 'dewrite-core'
                  api group: "org.openrewrite", name: "dewrite-core"
              }
              """
          )
        );
    }

    @ParameterizedTest
    @CsvSource(value ={"org.openrewrite:rewrite-core", "*:*"}, delimiterString = ":")
    void worksWithClassifier(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyArtifactId(group, artifact, "dewrite-core", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
                
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  api 'org.openrewrite:rewrite-core:latest.release:tests'
                  api "org.openrewrite:rewrite-core:latest.release:tests"
                  api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', classifier: 'tests'
                  api group: "org.openrewrite", name: "rewrite-core", version: "latest.release", classifier: "tests"
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
                  api 'org.openrewrite:dewrite-core:latest.release:tests'
                  api "org.openrewrite:dewrite-core:latest.release:tests"
                  api group: 'org.openrewrite', name: 'dewrite-core', version: 'latest.release', classifier: 'tests'
                  api group: "org.openrewrite", name: "dewrite-core", version: "latest.release", classifier: "tests"
              }
              """
          )
        );
    }

    @ParameterizedTest
    @CsvSource(value ={"org.eclipse.jetty:jetty-servlet", "*:*"}, delimiterString = ":")
    void worksWithExt(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyArtifactId(group, artifact, "jetty-servlet-tests", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
                
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  api 'org.eclipse.jetty:jetty-servlet@jar'
                  api "org.eclipse.jetty:jetty-servlet@jar"
                  api 'org.eclipse.jetty:jetty-servlet:9.4.50.v20221201@jar'
                  api "org.eclipse.jetty:jetty-servlet:9.4.50.v20221201@jar"
                  api 'org.eclipse.jetty:jetty-servlet:9.4.50.v20221201:tests@jar'
                  api "org.eclipse.jetty:jetty-servlet:9.4.50.v20221201:tests@jar"
                  api group: 'org.eclipse.jetty', name: 'jetty-servlet', ext: 'jar'
                  api group: "org.eclipse.jetty", name: "jetty-servlet", ext: "jar"
                  api group: 'org.eclipse.jetty', name: 'jetty-servlet', version: '9.4.50.v20221201', ext: 'jar'
                  api group: "org.eclipse.jetty", name: "jetty-servlet", version: "9.4.50.v20221201", ext: "jar"
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
                  api 'org.eclipse.jetty:jetty-servlet-tests@jar'
                  api "org.eclipse.jetty:jetty-servlet-tests@jar"
                  api 'org.eclipse.jetty:jetty-servlet-tests:9.4.50.v20221201@jar'
                  api "org.eclipse.jetty:jetty-servlet-tests:9.4.50.v20221201@jar"
                  api 'org.eclipse.jetty:jetty-servlet-tests:9.4.50.v20221201:tests@jar'
                  api "org.eclipse.jetty:jetty-servlet-tests:9.4.50.v20221201:tests@jar"
                  api group: 'org.eclipse.jetty', name: 'jetty-servlet-tests', ext: 'jar'
                  api group: "org.eclipse.jetty", name: "jetty-servlet-tests", ext: "jar"
                  api group: 'org.eclipse.jetty', name: 'jetty-servlet-tests', version: '9.4.50.v20221201', ext: 'jar'
                  api group: "org.eclipse.jetty", name: "jetty-servlet-tests", version: "9.4.50.v20221201", ext: "jar"
                  api group: 'org.eclipse.jetty', name: 'jetty-servlet-tests', version: '9.4.50.v20221201', classifier: 'tests', ext: 'jar'
                  api group: "org.eclipse.jetty", name: "jetty-servlet-tests", version: "9.4.50.v20221201", classifier: "tests", ext: "jar"
              }
              """
          )
        );
    }
}
