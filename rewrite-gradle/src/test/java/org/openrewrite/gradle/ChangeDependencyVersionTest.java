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

class ChangeDependencyVersionTest implements RewriteTest {

    @Test
    void worksWithEmptyStringConfig() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyVersion("org.springframework.boot", "*", "2.5.5", null, "")),
          buildGradle(
            """
              dependencies {
                  rewrite 'org.openrewrite:rewrite-gradle:latest.integration'
                  implementation 'org.springframework.cloud:spring-cloud-starter-sleuth:3.0.3'
                  implementation 'org.springframework.integration:spring-integration-ftp:5.5.1'
                  implementation 'org.springframework.boot:spring-boot-starter:2.5.4'
                  implementation 'commons-lang:commons-lang:2.6'
                  testImplementation 'org.springframework.boot:spring-boot-starter-test'
              }
              """,
            """
              dependencies {
                  rewrite 'org.openrewrite:rewrite-gradle:latest.integration'
                  implementation 'org.springframework.cloud:spring-cloud-starter-sleuth:3.0.3'
                  implementation 'org.springframework.integration:spring-integration-ftp:5.5.1'
                  implementation 'org.springframework.boot:spring-boot-starter:2.5.5'
                  implementation 'commons-lang:commons-lang:2.6'
                  testImplementation 'org.springframework.boot:spring-boot-starter-test'
              }
              """
          )
        );
    }

    @ParameterizedTest
    @CsvSource(value = {"org.openrewrite:rewrite-core", "*:*"}, delimiterString = ":")
    void findDependency(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyVersion(group, artifact, "latest.integration", null, null)),
          buildGradle(
            """
              dependencies {
                  api 'org.openrewrite:rewrite-core:latest.release'
                  api "org.openrewrite:rewrite-core:latest.release"
              }
              """,
            """
              dependencies {
                  api 'org.openrewrite:rewrite-core:latest.integration'
                  api "org.openrewrite:rewrite-core:latest.integration"
              }
              """
          )
        );
    }

    @ParameterizedTest
    @CsvSource(value = {"org.openrewrite:rewrite-core", "*:*"}, delimiterString = ":")
    void findMapStyleDependency(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyVersion(group, artifact, "latest.integration", null, null)),
          buildGradle(
            """
              dependencies {
                  api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release'
                  api group: "org.openrewrite", name: "rewrite-core", version: "latest.release"
              }
              """,
            """
              dependencies {
                  api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.integration'
                  api group: "org.openrewrite", name: "rewrite-core", version: "latest.integration"
              }
              """
          )
        );
    }

    @ParameterizedTest
    @CsvSource(value = {"org.openrewrite:rewrite-core", "*:*"}, delimiterString = ":")
    void withoutVersionShouldNotChange(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyVersion(group, artifact, "latest.integration", null, null)),
          buildGradle(
            """
              dependencies {
                  api 'org.openrewrite:rewrite-core'
                  api "org.openrewrite:rewrite-core"
                  api group: 'org.openrewrite', name: 'rewrite-core'
                  api group: "org.openrewrite", name: "rewrite-core"
              }
              """
          )
        );
    }

    @ParameterizedTest
    @CsvSource(value = {"org.openrewrite:rewrite-core", "*:*"}, delimiterString = ":")
    void worksWithClassifier(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyVersion(group, artifact, "latest.integration", null, null)),
          buildGradle(
            """
              dependencies {
                  api 'org.openrewrite:rewrite-core:latest.release:classifier'
                  api "org.openrewrite:rewrite-core:latest.release:classifier"
                  api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', classifier: 'classifier'
                  api group: "org.openrewrite", name: "rewrite-core", version: "latest.release", classifier: "classifier"
              }
              """,
            """
              dependencies {
                  api 'org.openrewrite:rewrite-core:latest.integration:classifier'
                  api "org.openrewrite:rewrite-core:latest.integration:classifier"
                  api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.integration', classifier: 'classifier'
                  api group: "org.openrewrite", name: "rewrite-core", version: "latest.integration", classifier: "classifier"
              }
              """
          )
        );
    }

    @ParameterizedTest
    @CsvSource(value = {"org.openrewrite:rewrite-core", "*:*"}, delimiterString = ":")
    void worksWithExt(String group, String artifact) {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyVersion(group, artifact, "latest.integration", null, null)),
          buildGradle(
            """
              dependencies {
                  api 'org.openrewrite:rewrite-core@ext'
                  api "org.openrewrite:rewrite-core@ext"
                  api 'org.openrewrite:rewrite-core:latest.release@ext'
                  api "org.openrewrite:rewrite-core:latest.release@ext"
                  api 'org.openrewrite:rewrite-core:latest.release:classifier@ext'
                  api "org.openrewrite:rewrite-core:latest.release:classifier@ext"
                  api group: 'org.openrewrite', name: 'rewrite-core', extension: 'ext'
                  api group: "org.openrewrite", name: "rewrite-core", extension: "ext"
                  api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', extension: 'ext'
                  api group: "org.openrewrite", name: "rewrite-core", version: "latest.release", extension: "ext"
                  api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release', classifier: 'classifier', extension: 'ext'
                  api group: "org.openrewrite", name: "rewrite-core", version: "latest.release", classifier: "classifier", extension: "ext"
              }
              """,
            """
              dependencies {
                  api 'org.openrewrite:rewrite-core@ext'
                  api "org.openrewrite:rewrite-core@ext"
                  api 'org.openrewrite:rewrite-core:latest.integration@ext'
                  api "org.openrewrite:rewrite-core:latest.integration@ext"
                  api 'org.openrewrite:rewrite-core:latest.integration:classifier@ext'
                  api "org.openrewrite:rewrite-core:latest.integration:classifier@ext"
                  api group: 'org.openrewrite', name: 'rewrite-core', extension: 'ext'
                  api group: "org.openrewrite", name: "rewrite-core", extension: "ext"
                  api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.integration', extension: 'ext'
                  api group: "org.openrewrite", name: "rewrite-core", version: "latest.integration", extension: "ext"
                  api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.integration', classifier: 'classifier', extension: 'ext'
                  api group: "org.openrewrite", name: "rewrite-core", version: "latest.integration", classifier: "classifier", extension: "ext"
              }
              """
          )
        );
    }
}
