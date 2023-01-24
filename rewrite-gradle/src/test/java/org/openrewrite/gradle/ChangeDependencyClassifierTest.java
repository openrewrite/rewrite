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

class ChangeDependencyClassifierTest implements RewriteTest {
    @Test
    void worksWithEmptyStringConfig() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyClassifier("org.openrewrite", "*", "classified", "")),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
                
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  api 'org.openrewrite:rewrite-gradle:latest.release:javadoc'
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
                  api 'org.openrewrite:rewrite-gradle:latest.release:classified'
              }
              """
          )
        );
    }

    @ParameterizedTest
    @CsvSource(value = {"org.openrewrite:rewrite-core", "*:*"}, delimiterString = ":")
    void findDependency(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyClassifier(group, artifact, "classified", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  api 'org.openrewrite:rewrite-core:latest.release:javadoc'
                  api "org.openrewrite:rewrite-core:latest.release:javadoc"
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
                  api 'org.openrewrite:rewrite-core:latest.release:classified'
                  api "org.openrewrite:rewrite-core:latest.release:classified"
              }
              """
          )
        );
    }

    @ParameterizedTest
    @CsvSource(value = {"org.openrewrite:rewrite-core", "*:*"}, delimiterString = ":")
    void findMapStyleDependency(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyClassifier(group, artifact, "classified", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
                
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', classifier: 'javadoc'
                  api group: "org.openrewrite", name: "rewrite-core", version: "latest.release", classifier: "javadoc"
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
                  api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', classifier: 'classified'
                  api group: "org.openrewrite", name: "rewrite-core", version: "latest.release", classifier: "classified"
              }
              """
          )
        );
    }

    @ParameterizedTest
    @CsvSource(value = {"org.openrewrite:rewrite-core", "*:*"}, delimiterString = ":")
    void worksWithoutVersion(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyClassifier(group, artifact, "classified", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
                
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  api group: 'org.openrewrite', name: 'rewrite-core', classifier: 'javadoc'
                  api group: "org.openrewrite", name: "rewrite-core", classifier: "javadoc"
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
                  api group: 'org.openrewrite', name: 'rewrite-core', classifier: 'classified'
                  api group: "org.openrewrite", name: "rewrite-core", classifier: "classified"
              }
              """
          )
        );
    }

    @ParameterizedTest
    @CsvSource(value = {"org.openrewrite:rewrite-core", "*:*"}, delimiterString = ":")
    void worksWithClassifier(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyClassifier(group, artifact, "classified", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
                
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  api 'org.openrewrite:rewrite-core:latest.release:javadoc'
                  api "org.openrewrite:rewrite-core:latest.release:javadoc"
                  api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', classifier: 'javadoc'
                  api group: "org.openrewrite", name: "rewrite-core", version: "latest.release", classifier: "javadoc"
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
                  api 'org.openrewrite:rewrite-core:latest.release:classified'
                  api "org.openrewrite:rewrite-core:latest.release:classified"
                  api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', classifier: 'classified'
                  api group: "org.openrewrite", name: "rewrite-core", version: "latest.release", classifier: "classified"
              }
              """
          )
        );
    }

    @ParameterizedTest
    @CsvSource(value = {"org.openrewrite:rewrite-core", "*:*"}, delimiterString = ":")
    void worksWithExt(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyClassifier(group, artifact, "classified", null)),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
                
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  api 'org.openrewrite:rewrite-core:latest.release:javadoc@jar'
                  api "org.openrewrite:rewrite-core:latest.release:javadoc@jar"
                  api group: 'org.openrewrite', name: 'rewrite-core', classifier: 'javadoc'
                  api group: "org.openrewrite", name: "rewrite-core", classifier: "javadoc"
                  api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', classifier: 'javadoc'
                  api group: "org.openrewrite", name: "rewrite-core", version: "latest.release", classifier: "javadoc"
                  api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', classifier: 'javadoc', ext: 'jar'
                  api group: "org.openrewrite", name: "rewrite-core", version: "latest.release", classifier: "javadoc", ext: "jar"
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
                  api 'org.openrewrite:rewrite-core:latest.release:classified@jar'
                  api "org.openrewrite:rewrite-core:latest.release:classified@jar"
                  api group: 'org.openrewrite', name: 'rewrite-core', classifier: 'classified'
                  api group: "org.openrewrite", name: "rewrite-core", classifier: "classified"
                  api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', classifier: 'classified'
                  api group: "org.openrewrite", name: "rewrite-core", version: "latest.release", classifier: "classified"
                  api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', classifier: 'classified', ext: 'jar'
                  api group: "org.openrewrite", name: "rewrite-core", version: "latest.release", classifier: "classified", ext: "jar"
              }
              """
          )
        );
    }
}
