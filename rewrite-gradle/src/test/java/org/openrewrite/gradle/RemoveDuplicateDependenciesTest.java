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
import static org.openrewrite.gradle.Assertions.settingsGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

class RemoveDuplicateDependenciesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
          .recipe(new RemoveDuplicateDependencies());
    }

    @DocumentExample
    @Test
    void removeDuplicateDependency() {
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
                  implementation "com.google.guava:guava:29.0-jre"
                  testImplementation "junit:junit:4.13"
                  implementation "com.google.guava:guava:29.0-jre"
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
                  implementation "com.google.guava:guava:29.0-jre"
                  testImplementation "junit:junit:4.13"
              }
              """
          )
        );
    }

    @Test
    void removeDuplicateWithDifferentNotation() {
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
                  implementation group: "com.google.guava", name: "guava", version: "29.0-jre"
                  implementation(group: 'com.google.guava', version: '29.0-jre', name: 'guava')
                  implementation 'junit:junit:4.13'
                  implementation("junit:junit:4.13")
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
                  implementation group: "com.google.guava", name: "guava", version: "29.0-jre"
                  implementation 'junit:junit:4.13'
              }
              """
          )
        );
    }

    @Test
    void removeDuplicatePlatformDependency() {
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
                  implementation platform("org.springframework.boot:spring-boot-dependencies:2.7.18")
                  implementation platform("org.springframework.boot:spring-boot-dependencies:2.7.18")
                  implementation "org.springframework.boot:spring-boot-dependencies:2.7.18"
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
                  implementation platform("org.springframework.boot:spring-boot-dependencies:2.7.18")
                  implementation "org.springframework.boot:spring-boot-dependencies:2.7.18"
              }
              """
          )
        );
    }

    @Test
    void removeDuplicateKotlinDsl() {
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
                  implementation("com.google.guava:guava:29.0-jre")
                  testImplementation("junit:junit:4.13")
                  implementation("com.google.guava:guava:29.0-jre")
                  testImplementation(group = "junit", name = "junit", version = "4.13")
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
                  implementation("com.google.guava:guava:29.0-jre")
                  testImplementation("junit:junit:4.13")
              }
              """
          )
        );
    }

    @Test
    void keepDependenciesInDifferentConfigurations() {
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
                  api "com.google.guava:guava:29.0-jre"
                  implementation "com.google.guava:guava:29.0-jre"
                  testImplementation "com.google.guava:guava:29.0-jre"
              }
              """
          )
        );
    }

    @Test
    void keepDependenciesWithDifferentVersions() {
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
                  implementation "com.google.guava:guava:29.0-jre"
                  implementation "com.google.guava:guava:30.1-jre"
              }
              """
          )
        );
    }

    @Test
    void removeDuplicateDynamicVersion() {
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
                  implementation "com.google.guava:guava:+"
                  implementation 'com.google.guava:guava:+'
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
                  implementation "com.google.guava:guava:+"
              }
              """
          )
        );
    }

    @Test
    void keepDynamicAndFixedVersions() {
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
                  implementation "com.google.guava:guava:29.+"
                  implementation "com.google.guava:guava:29.0-jre"
              }
              """
          )
        );
    }

    @Test
    void keepDependenciesWithDifferentClassifiers() {
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
                  implementation "io.netty:netty-transport-native-epoll:4.1.100.Final:linux-x86_64"
                  implementation "io.netty:netty-transport-native-epoll:4.1.100.Final:linux-aarch_64"
              }
              """
          )
        );
    }

    @Test
    void keepDependenciesWithDifferentClosures() {
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
                  implementation("com.google.guava:guava:29.0-jre")
                  implementation("com.google.guava:guava:29.0-jre") {
                      exclude group: "com.google.code.findbugs"
                  }
              }
              """
          )
        );
    }

    @Test
    void keepConstraintsAndSeparateDependencyBlocks() {
        rewriteRun(
          buildGradle(
            """
              buildscript {
                  repositories {
                      mavenCentral()
                  }
                  dependencies {
                      classpath "com.google.guava:guava:29.0-jre"
                  }
              }

              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  constraints {
                      implementation "com.google.guava:guava:29.0-jre"
                  }
                  implementation "com.google.guava:guava:29.0-jre"
              }
              """
          )
        );
    }

    @Test
    void removeDuplicateProjectDependency() {
        rewriteRun(
          settingsGradle("include 'core'"),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
              """,
            spec -> spec.path("core/build.gradle")
          ),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              dependencies {
                  implementation project(":core")
                  implementation project(':core')
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              dependencies {
                  implementation project(":core")
              }
              """
          )
        );
    }

    @Test
    void keepDependenciesWhenVariableValueChanges() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              def dependency = 'com.google.guava:guava:29.0-jre'
              dependencies {
                  implementation dependency
                  dependency = 'junit:junit:4.13'
                  implementation dependency
              }
              """
          )
        );
    }

    @Test
    void keepDependenciesWhenInterpolatedValueChanges() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              def v = '29.0-jre'
              dependencies {
                  implementation "com.google.guava:guava:$v"
                  v = '30.1-jre'
                  implementation "com.google.guava:guava:$v"
              }
              """
          )
        );
    }

    @Test
    void keepDependenciesWhoseArgumentsCallMethods() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              def deps = ['com.google.guava:guava:29.0-jre', 'junit:junit:4.13'].iterator()
              dependencies {
                  implementation deps.next()
                  implementation deps.next()
              }
              """
          )
        );
    }

    @Test
    void keepDependenciesWhoseStringsDifferOnlyInWhitespace() {
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
                  implementation fileTree(dir: 'libs', include: ['a b.jar'])
                  implementation fileTree(dir: 'libs', include: ['a  b.jar'])
              }
              """
          )
        );
    }

    @Test
    void retainCommentsAndBlankLinesOfRemovedDuplicate() {
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
                  implementation "com.google.guava:guava:29.0-jre" // core

                  // testing
                  implementation "com.google.guava:guava:29.0-jre" // duplicate
                  testImplementation "junit:junit:4.13"
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
                  implementation "com.google.guava:guava:29.0-jre" // core

                  // testing
                  testImplementation "junit:junit:4.13"
              }
              """
          )
        );
    }

    @Test
    void removeLastDuplicateWithTrailingComment() {
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
                  implementation "com.google.guava:guava:29.0-jre"
                  implementation "com.google.guava:guava:29.0-jre" // duplicate
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
                  implementation "com.google.guava:guava:29.0-jre"
              }
              """
          )
        );
    }

    @Test
    void removeDuplicateAcrossStringAndMapNotation() {
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
                  implementation 'com.google.guava:guava:29.0-jre'
                  implementation group: 'com.google.guava', name: 'guava', version: '29.0-jre'
                  implementation([group: 'com.google.guava', name: 'guava', version: '29.0-jre'])
                  implementation 'group': 'com.google.guava', 'version': '29.0-jre', 'name': 'guava'
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
                  implementation 'com.google.guava:guava:29.0-jre'
              }
              """
          )
        );
    }

    @Test
    void removeDuplicateAcrossNotationsWithClassifierAndExtension() {
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
                  implementation group: 'io.netty', name: 'netty-transport-native-epoll', version: '4.1.100.Final', classifier: 'linux-x86_64', ext: 'jar'
                  implementation 'io.netty:netty-transport-native-epoll:4.1.100.Final:linux-x86_64@jar'
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
                  implementation group: 'io.netty', name: 'netty-transport-native-epoll', version: '4.1.100.Final', classifier: 'linux-x86_64', ext: 'jar'
              }
              """
          )
        );
    }

    @Test
    void removeDuplicatePlatformAcrossNotations() {
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
                  implementation platform('org.springframework.boot:spring-boot-dependencies:2.7.18')
                  implementation platform(group: 'org.springframework.boot', name: 'spring-boot-dependencies', version: '2.7.18')
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
                  implementation platform('org.springframework.boot:spring-boot-dependencies:2.7.18')
              }
              """
          )
        );
    }

    @Test
    void removeDuplicateAcrossNotationsKotlinDsl() {
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
                  implementation("com.google.guava:guava:29.0-jre")
                  implementation(group = "com.google.guava", name = "guava", version = "29.0-jre")
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
                  implementation("com.google.guava:guava:29.0-jre")
              }
              """
          )
        );
    }

    @Test
    void keepMapNotationWithClassifierAndStringNotationWithout() {
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
                  implementation 'io.netty:netty-transport-native-epoll:4.1.100.Final'
                  implementation group: 'io.netty', name: 'netty-transport-native-epoll', version: '4.1.100.Final', classifier: 'linux-x86_64'
              }
              """
          )
        );
    }

    @Test
    void keepMapNotationWithAdditionalKeys() {
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
                  implementation 'com.google.guava:guava:29.0-jre'
                  implementation group: 'com.google.guava', name: 'guava', version: '29.0-jre', transitive: false
              }
              """
          )
        );
    }

    @Test
    void keepMapNotationWithVariableValue() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              def guavaVersion = '30.1-jre'
              dependencies {
                  implementation 'com.google.guava:guava:29.0-jre'
                  implementation group: 'com.google.guava', name: 'guava', version: guavaVersion
              }
              """
          )
        );
    }

    @Test
    void keepMapNotationWhoseValueContainsSeparator() {
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
                  implementation 'com.google.guava:guava:29.0-jre:tests'
                  implementation group: 'com.google.guava', name: 'guava:29.0-jre', version: 'tests'
              }
              """
          )
        );
    }
}
