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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;

class ChangeDependencyConfigurationTest implements RewriteTest {
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
    void findDependency(String group, String artifact) {
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
    void findMapStyleDependency(String group, String artifact) {
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

    @Disabled
    @ParameterizedTest
    @CsvSource(value = {"org.openrewrite:rewrite-core", "*:*"}, delimiterString = ":")
    void withoutVersionShouldNotChange(String group, String artifact) {
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
                  api 'org.openrewrite:rewrite-core'
                  api "org.openrewrite:rewrite-core"
                  api group: 'org.openrewrite', name: 'rewrite-core'
                  api group: "org.openrewrite", name: "rewrite-core"
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
                  implementation 'org.openrewrite:rewrite-core'
                  implementation "org.openrewrite:rewrite-core"
                  implementation group: 'org.openrewrite', name: 'rewrite-core'
                  implementation group: "org.openrewrite", name: "rewrite-core"
              }
              """
          )
        );
    }

    @ParameterizedTest
    @CsvSource(value = {"org.eclipse.jetty:jetty-servlet", "*:*"}, delimiterString = ":")
    void worksWithClassifier(String group, String artifact) {
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
                  api 'org.eclipse.jetty:jetty-servlet:9.4.9.v20180320:tests'
                  api "org.eclipse.jetty:jetty-servlet:9.4.9.v20180320:tests"
                  api group: 'org.eclipse.jetty', name: 'jetty-servlet', version: '9.4.9.v20180320', classifier: 'tests'
                  api group: "org.eclipse.jetty", name: "jetty-servlet", version: "9.4.9.v20180320", classifier: "tests"
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
                  implementation 'org.eclipse.jetty:jetty-servlet:9.4.9.v20180320:tests'
                  implementation "org.eclipse.jetty:jetty-servlet:9.4.9.v20180320:tests"
                  implementation group: 'org.eclipse.jetty', name: 'jetty-servlet', version: '9.4.9.v20180320', classifier: 'tests'
                  implementation group: "org.eclipse.jetty", name: "jetty-servlet", version: "9.4.9.v20180320", classifier: "tests"
              }
              """
          )
        );
    }

    @ParameterizedTest
    @CsvSource(value = {"org.eclipse.jetty:jetty-servlet", "*:*"}, delimiterString = ":")
    void worksWithExt(String group, String artifact) {
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
                  api 'org.eclipse.jetty:jetty-servlet@jar'
                  api "org.eclipse.jetty:jetty-servlet@jar"
                  api 'org.eclipse.jetty:jetty-servlet:9.4.9.v20180320@jar'
                  api "org.eclipse.jetty:jetty-servlet:9.4.9.v20180320@jar"
                  api 'org.eclipse.jetty:jetty-servlet:9.4.9.v20180320:tests@jar'
                  api "org.eclipse.jetty:jetty-servlet:9.4.9.v20180320:tests@jar"
                  api group: 'org.eclipse.jetty', name: 'jetty-servlet', ext: 'jar'
                  api group: "org.eclipse.jetty", name: "jetty-servlet", ext: "jar"
                  api group: 'org.eclipse.jetty', name: 'jetty-servlet', version: '9.4.9.v20180320', ext: 'jar'
                  api group: "org.eclipse.jetty", name: "jetty-servlet", version: "9.4.9.v20180320", ext: "jar"
                  api group: 'org.eclipse.jetty', name: 'jetty-servlet', version: '9.4.9.v20180320', classifier: 'tests', ext: 'jar'
                  api group: "org.eclipse.jetty", name: "jetty-servlet", version: "9.4.9.v20180320", classifier: "tests", ext: "jar"
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
                  implementation 'org.eclipse.jetty:jetty-servlet@jar'
                  implementation "org.eclipse.jetty:jetty-servlet@jar"
                  implementation 'org.eclipse.jetty:jetty-servlet:9.4.9.v20180320@jar'
                  implementation "org.eclipse.jetty:jetty-servlet:9.4.9.v20180320@jar"
                  implementation 'org.eclipse.jetty:jetty-servlet:9.4.9.v20180320:tests@jar'
                  implementation "org.eclipse.jetty:jetty-servlet:9.4.9.v20180320:tests@jar"
                  implementation group: 'org.eclipse.jetty', name: 'jetty-servlet', ext: 'jar'
                  implementation group: "org.eclipse.jetty", name: "jetty-servlet", ext: "jar"
                  implementation group: 'org.eclipse.jetty', name: 'jetty-servlet', version: '9.4.9.v20180320', ext: 'jar'
                  implementation group: "org.eclipse.jetty", name: "jetty-servlet", version: "9.4.9.v20180320", ext: "jar"
                  implementation group: 'org.eclipse.jetty', name: 'jetty-servlet', version: '9.4.9.v20180320', classifier: 'tests', ext: 'jar'
                  implementation group: "org.eclipse.jetty", name: "jetty-servlet", version: "9.4.9.v20180320", classifier: "tests", ext: "jar"
              }
              """
          )
        );
    }
}
