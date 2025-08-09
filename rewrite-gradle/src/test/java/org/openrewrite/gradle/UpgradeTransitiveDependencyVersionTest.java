/*
 * Copyright 2024 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.*;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.gradle.Assertions.*;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.test.RewriteTest.toRecipe;

class UpgradeTransitiveDependencyVersionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .beforeRecipe(withToolingApi())
          .recipe(new UpgradeTransitiveDependencyVersion(
            "com.fasterxml*", "jackson-core", "2.12.5", null, "CVE-2024-BAD", null));
    }

    @DocumentExample
    @Test
    void addConstraint() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                id 'java'
              }
              repositories { mavenCentral() }

              dependencies {
                  implementation 'org.openrewrite:rewrite-java:7.0.0'
              }
              """,
            """
              plugins {
                id 'java'
              }
              repositories { mavenCentral() }

              dependencies {
                  constraints {
                      implementation('com.fasterxml.jackson.core:jackson-core:2.12.5') {
                          because 'CVE-2024-BAD'
                      }
                  }

                  implementation 'org.openrewrite:rewrite-java:7.0.0'
              }
              """
          )
        );
    }

    @Test
    void addConstraintForDependenciesDeclaredInMultipleConfigurationsThatExtendFromDifferentResolvableConfigurations() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }

              dependencies {
                  compileOnly 'org.openrewrite:rewrite-java:7.0.0'
                  runtimeOnly 'org.openrewrite:rewrite-java:7.0.0'
              }
              """,
            """
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }

              dependencies {
                  constraints {
                      runtimeOnly('com.fasterxml.jackson.core:jackson-core:2.12.5') {
                          because 'CVE-2024-BAD'
                      }
                      compileOnly('com.fasterxml.jackson.core:jackson-core:2.12.5') {
                          because 'CVE-2024-BAD'
                      }
                  }

                  compileOnly 'org.openrewrite:rewrite-java:7.0.0'
                  runtimeOnly 'org.openrewrite:rewrite-java:7.0.0'
              }
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"compileOnly", "implementation", "runtimeOnly", "testCompileOnly", "testImplementation", "testRuntimeOnly"})
    void updatesExistingConstraints(String configurationName) {
        rewriteRun(spec ->
            spec.recipe(new UpgradeTransitiveDependencyVersion(
              "com.thoughtworks.xstream", "xstream", "1.4.21", null, null, null)),
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }

              dependencies {
                  constraints {
                      %1$s 'com.thoughtworks.xstream:xstream:1.4.17'
                  }
              }

              dependencies {
                  %1$s 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client:4.2.0'
              }
              """.formatted(configurationName),
            """
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }

              dependencies {
                  constraints {
                      %1$s 'com.thoughtworks.xstream:xstream:1.4.21'
                  }
              }

              dependencies {
                  %1$s 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client:4.2.0'
              }
              """.formatted(configurationName)
          )
        );
    }

    @Test
    void includedDefaultConfigurationsReceiveRuntimeConstraints() {
        rewriteRun(
          spec -> spec
            .recipe(new UpgradeTransitiveDependencyVersion(
              "org.apache.commons", "commons-lang3", "3.14.0", null, null, List.of("compileOnly", "runtimeOnly"))),
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }
              dependencies {
                  compileOnly 'org.apache.activemq:artemis-jakarta-server:2.28.0'
              }
              """,
            """
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }
              dependencies {
                  constraints {
                      compileOnly('org.apache.commons:commons-lang3:3.14.0')
                  }

                  compileOnly 'org.apache.activemq:artemis-jakarta-server:2.28.0'
              }
              """
          )
        );
    }

    @Test
    void customConfiguration() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                id 'java'
              }
              configurations.create("foo")
              repositories { mavenCentral() }

              dependencies {
                  foo 'org.openrewrite:rewrite-java:7.0.0'
              }
              """,
            """
              plugins {
                id 'java'
              }
              configurations.create("foo")
              repositories { mavenCentral() }

              dependencies {
                  constraints {
                      foo('com.fasterxml.jackson.core:jackson-core:2.12.5') {
                          because 'CVE-2024-BAD'
                      }
                  }

                  foo 'org.openrewrite:rewrite-java:7.0.0'
              }
              """
          )
        );
    }

    @Test
    void customNonTransitiveConfigurationCannotAddConstraint() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                id 'java'
              }
              configurations {
                  foo {
                      transitive = false
                  }
              }
              repositories { mavenCentral() }

              dependencies {
                  foo 'org.openrewrite:rewrite-java:7.0.0'
              }
              """
          )
        );
    }

    @Test
    void addConstraintAddsSameArtifactsInSameConfigurationAsSingleConstraint() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                id 'java'
              }
              repositories { mavenCentral() }

              dependencies {
                  implementation 'org.openrewrite:rewrite-java:7.0.0'
                  implementation 'com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.8.9'
              }
              """,
            """
              plugins {
                id 'java'
              }
              repositories { mavenCentral() }

              dependencies {
                  constraints {
                      implementation('com.fasterxml.jackson.core:jackson-core:2.12.5') {
                          because 'CVE-2024-BAD'
                      }
                  }

                  implementation 'org.openrewrite:rewrite-java:7.0.0'
                  implementation 'com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.8.9'
              }
              """
          )
        );
    }

    @Test
    void addConstraintAddsSameArtifactsInDifferentConfigurationAsSingleConstraint() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                id 'java'
              }
              repositories { mavenCentral() }

              dependencies {
                  implementation 'org.openrewrite:rewrite-java:7.0.0'
                  runtimeOnly 'com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.8.9'
              }
              """,
            """
              plugins {
                id 'java'
              }
              repositories { mavenCentral() }

              dependencies {
                  constraints {
                      implementation('com.fasterxml.jackson.core:jackson-core:2.12.5') {
                          because 'CVE-2024-BAD'
                      }
                  }

                  implementation 'org.openrewrite:rewrite-java:7.0.0'
                  runtimeOnly 'com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.8.9'
              }
              """
          )
        );
    }

    @Test
    void addConstraintAddsUnrelatedConfigurationsForSameArtifact() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                id 'java'
                id 'ear'
              }
              repositories { mavenCentral() }

              dependencies {
                  implementation 'org.openrewrite:rewrite-java:7.0.0'
                  runtimeOnly 'com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.8.9'
                  earlib 'com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.8.9'
              }
              """,
            """
              plugins {
                id 'java'
                id 'ear'
              }
              repositories { mavenCentral() }

              dependencies {
                  constraints {
                      earlib('com.fasterxml.jackson.core:jackson-core:2.12.5') {
                          because 'CVE-2024-BAD'
                      }
                      implementation('com.fasterxml.jackson.core:jackson-core:2.12.5') {
                          because 'CVE-2024-BAD'
                      }
                  }

                  implementation 'org.openrewrite:rewrite-java:7.0.0'
                  runtimeOnly 'com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.8.9'
                  earlib 'com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.8.9'
              }
              """
          )
        );
    }

    @Test
    void updateConstraint() {
        rewriteRun(
          buildGradle(
            """
              plugins { id 'java' }
              repositories { mavenCentral() }

              dependencies {
                  constraints {
                      implementation('com.fasterxml.jackson.core:jackson-core:2.12.0') {
                          because 'some reason'
                      }
                  }

                  implementation 'org.openrewrite:rewrite-java:7.0.0'
              }
              """,
            """
              plugins { id 'java' }
              repositories { mavenCentral() }

              dependencies {
                  constraints {
                      implementation('com.fasterxml.jackson.core:jackson-core:2.12.5') {
                          because 'CVE-2024-BAD'
                      }
                  }

                  implementation 'org.openrewrite:rewrite-java:7.0.0'
              }
              """
          )
        );
    }

    @Disabled("String interpolation with inline properties is not yet supported")
    @Test
    void updateConstraintGStringInterpolation() {
        rewriteRun(
          buildGradle(
            """
              plugins { id 'java' }
              repositories { mavenCentral() }

              def jacksonVersion = "2.12.0"

              dependencies {
                  constraints {
                      implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion") {
                          because 'some reason'
                      }
                  }

                  implementation 'org.openrewrite:rewrite-java:7.0.0'
              }
              """,
            """
              plugins { id 'java' }
              repositories { mavenCentral() }

              def jacksonVersion = "2.12.5"

              dependencies {
                  constraints {
                      implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion") {
                          because 'CVE-2024-BAD'
                      }
                  }

                  implementation 'org.openrewrite:rewrite-java:7.0.0'
              }
              """
          )
        );
    }

    @Test
    void updateConstraintInPropertiesFile() {
        rewriteRun(
          properties(
            """
              jacksonVersion=2.12.0
              """,
            """
              jacksonVersion=2.12.5
              """,
            spec -> spec.path("gradle.properties")
          ),
          buildGradle(
            """
              plugins { id 'java' }
              repositories { mavenCentral() }

              dependencies {
                  constraints {
                      implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion") {
                          because 'some reason'
                      }
                  }

                  implementation 'org.openrewrite:rewrite-java:7.0.0'
              }
              """,
            """
              plugins { id 'java' }
              repositories { mavenCentral() }

              dependencies {
                  constraints {
                      implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion") {
                          because 'CVE-2024-BAD'
                      }
                  }

                  implementation 'org.openrewrite:rewrite-java:7.0.0'
              }
              """
          )
        );
    }

    @Test
    void updateConstraintInPropertiesFileMapNotation() {
        rewriteRun(
          properties(
            """
              jacksonVersion=2.12.0
              """,
            """
              jacksonVersion=2.12.5
              """,
            spec -> spec.path("gradle.properties")
          ),
          buildGradle(
            """
              plugins { id 'java' }
              repositories { mavenCentral() }

              dependencies {
                  constraints {
                      implementation group: "com.fasterxml.jackson.core", name: "jackson-core", version: jacksonVersion, {
                          because 'some reason'
                      }
                  }

                  implementation 'org.openrewrite:rewrite-java:7.0.0'
              }
              """,
            """
              plugins { id 'java' }
              repositories { mavenCentral() }

              dependencies {
                  constraints {
                      implementation group: "com.fasterxml.jackson.core", name: "jackson-core", version: jacksonVersion, {
                          because 'CVE-2024-BAD'
                      }
                  }

                  implementation 'org.openrewrite:rewrite-java:7.0.0'
              }
              """
          )
        );
    }

    @Test
    void kotlinDslUpdateConstraintInPropertiesFile() {
        rewriteRun(
          properties(
            """
              jacksonVersion=2.12.0
              """,
            """
              jacksonVersion=2.12.5
              """,
            spec -> spec.path("gradle.properties")
          ),
          buildGradleKts(
            """
              plugins {
                `java-library`
              }
              repositories { mavenCentral() }

              val jacksonVersion: String by project
              dependencies {
                  constraints {
                      implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion") {
                          because("some reason")
                      }
                  }

                  implementation("org.openrewrite:rewrite-java:7.0.0")
              }
              """,
            """
              plugins {
                `java-library`
              }
              repositories { mavenCentral() }

              val jacksonVersion: String by project
              dependencies {
                  constraints {
                      implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion") {
                          because("CVE-2024-BAD")
                      }
                  }

                  implementation("org.openrewrite:rewrite-java:7.0.0")
              }
              """
          )
        );
    }

    @Test
    void updateConstraintAddingBecause() {
        rewriteRun(
          buildGradle(
            """
              plugins { id 'java' }
              repositories { mavenCentral() }

              dependencies {
                  constraints {
                      implementation("org.openrewrite:rewrite-core:7.0.0")
                      implementation('com.fasterxml.jackson.core:jackson-core:2.12.0')
                      implementation("org.openrewrite:rewrite-xml:7.0.0")
                  }

                  implementation 'org.openrewrite:rewrite-java:7.0.0'
              }
              """,
            """
              plugins { id 'java' }
              repositories { mavenCentral() }

              dependencies {
                  constraints {
                      implementation("org.openrewrite:rewrite-core:7.0.0")
                      implementation('com.fasterxml.jackson.core:jackson-core:2.12.5') {
                          because 'CVE-2024-BAD'
                      }
                      implementation("org.openrewrite:rewrite-xml:7.0.0")
                  }

                  implementation 'org.openrewrite:rewrite-java:7.0.0'
              }
              """
          )
        );
    }

    @Test
    void addConstraintToExistingConstraintsBlock() {
        rewriteRun(
          buildGradle(
            """
              plugins { id 'java' }
              repositories { mavenCentral() }

              dependencies {
                  implementation 'org.openrewrite:rewrite-java:7.0.0'

                  constraints {
                  }
              }
              """,
            """
              plugins { id 'java' }
              repositories { mavenCentral() }

              dependencies {
                  implementation 'org.openrewrite:rewrite-java:7.0.0'

                  constraints {
                      implementation('com.fasterxml.jackson.core:jackson-core:2.12.5') {
                          because 'CVE-2024-BAD'
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void addConstraintToConfigurationNotExtendingAnything() {
        rewriteRun(
          buildGradle(
            """
              plugins { id 'ear' }
              repositories { mavenCentral() }

              dependencies {
                  earlib 'org.openrewrite:rewrite-java:7.0.0'

                  constraints {
                  }
              }
              """,
            """
              plugins { id 'ear' }
              repositories { mavenCentral() }

              dependencies {
                  earlib 'org.openrewrite:rewrite-java:7.0.0'

                  constraints {
                      earlib('com.fasterxml.jackson.core:jackson-core:2.12.5') {
                          because 'CVE-2024-BAD'
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void removeOtherVersionConstraint() {
        rewriteRun(
          buildGradle(
            """
              plugins { id 'java' }
              repositories { mavenCentral() }

              dependencies {
                  implementation 'org.openrewrite:rewrite-java:7.0.0'

                  constraints {
                      implementation('com.fasterxml.jackson.core:jackson-core') {
                          because 'security'
                          version {
                              strictly('2.12.0')
                          }
                      }
                  }
              }
              """,
            """
              plugins { id 'java' }
              repositories { mavenCentral() }

              dependencies {
                  implementation 'org.openrewrite:rewrite-java:7.0.0'

                  constraints {
                      implementation('com.fasterxml.jackson.core:jackson-core:2.12.5') {
                          because 'CVE-2024-BAD'
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void addConstraintToNonTransitiveExtendingTransitiveConfiguration() {
        rewriteRun(
          buildGradle(
            """
              plugins { id 'ear' }
              repositories { mavenCentral() }
              configurations.earlib.extendsFrom configurations.deploy
              dependencies {
                  earlib 'org.openrewrite:rewrite-java:7.0.0'

                  constraints {
                  }
              }
              """,
            """
              plugins { id 'ear' }
              repositories { mavenCentral() }
              configurations.earlib.extendsFrom configurations.deploy
              dependencies {
                  earlib 'org.openrewrite:rewrite-java:7.0.0'

                  constraints {
                      earlib('com.fasterxml.jackson.core:jackson-core:2.12.5') {
                          because 'CVE-2024-BAD'
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void constraintDoesNotGetAddedToNonTransitiveNonExtendingConfiguration() {
        rewriteRun(
          buildGradle(
            """
              plugins { id 'ear' }
              repositories { mavenCentral() }
              dependencies {
                  deploy 'org.openrewrite:rewrite-java:7.0.0'

                  constraints {
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4228")
    @Test
    void constraintDoesNotGetAddedInsideConstraint() {
        rewriteRun(
          spec -> spec
            .recipe(new UpgradeTransitiveDependencyVersion("com.fasterxml.jackson.core", "jackson-core", "2.12.5", null, "CVE-2024-BAD", null)),
          //language=groovy
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation 'org.openrewrite:rewrite-java:7.0.0'

                  constraints {
                      implementation("org.apache.logging.log4j:log4j-core") {
                          version {
                              strictly("2.17.0")
                          }
                          because 'security'
                      }
                  }
              }
              """,
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation 'org.openrewrite:rewrite-java:7.0.0'

                  constraints {
                      implementation('com.fasterxml.jackson.core:jackson-core:2.12.5') {
                          because 'CVE-2024-BAD'
                      }
                      implementation("org.apache.logging.log4j:log4j-core") {
                          version {
                              strictly("2.17.0")
                          }
                          because 'security'
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void includedConfigurationsReceiveOnlyConfiguredConstraints() {
        rewriteRun(
          spec -> spec
            .recipe(new UpgradeTransitiveDependencyVersion(
              "org.apache.commons", "commons-lang3", "3.14.0", null, null, List.of("pitest"))),
          buildGradle(
            """
              plugins {
                  id 'info.solidsoft.pitest' version '1.15.0'
                  id 'java'
              }
              repositories { mavenCentral() }
              dependencies {
                  testImplementation 'org.apache.activemq:artemis-jakarta-server:2.28.0'
              }
              """,
            """
              plugins {
                  id 'info.solidsoft.pitest' version '1.15.0'
                  id 'java'
              }
              repositories { mavenCentral() }
              dependencies {
                  constraints {
                      pitest('org.apache.commons:commons-lang3:3.14.0')
                  }

                  testImplementation 'org.apache.activemq:artemis-jakarta-server:2.28.0'
              }
              """
          )
        );
    }

    @Test
    void noIncludedConfigurationsReceiveAllConstraints() {
        rewriteRun(
          spec -> spec
            .recipe(new UpgradeTransitiveDependencyVersion(
              "org.apache.commons", "commons-lang3", "3.14.0", null, null, null)),
          buildGradle(
            """
              plugins {
                  id 'info.solidsoft.pitest' version '1.15.0'
                  id 'java'
              }
              repositories { mavenCentral() }
              dependencies {
                  testImplementation 'org.apache.activemq:artemis-jakarta-server:2.28.0'
              }
              """,
            """
              plugins {
                  id 'info.solidsoft.pitest' version '1.15.0'
                  id 'java'
              }
              repositories { mavenCentral() }
              dependencies {
                  constraints {
                      pitest('org.apache.commons:commons-lang3:3.14.0')
                      testImplementation('org.apache.commons:commons-lang3:3.14.0')
                  }

                  testImplementation 'org.apache.activemq:artemis-jakarta-server:2.28.0'
              }
              """
          )
        );
    }

    @Test
    void useResolutionStrategyWhenSpringDependencyManagementPluginIsPresent() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'io.spring.dependency-management' version '1.1.5'
              }
              repositories { mavenCentral() }

              dependencies {

                  implementation 'org.openrewrite:rewrite-java:7.0.0'
              }
              """,
            """
              plugins {
                  id 'java'
                  id 'io.spring.dependency-management' version '1.1.5'
              }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency { details ->
                      if (details.requested.group == 'com.fasterxml.jackson.core' && details.requested.name == 'jackson-core') {
                          details.useVersion('2.12.5')
                          details.because('CVE-2024-BAD')
                      }
                  }
              }

              dependencies {

                  implementation 'org.openrewrite:rewrite-java:7.0.0'
              }
              """
          )
        );
    }

    @Test
    void noChangesIfDependencyIsAlsoPresentOnProject() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                id 'java'
              }
              repositories { mavenCentral() }

              dependencies {
                  implementation('com.fasterxml.jackson.core:jackson-core:2.12.0')
                  implementation('com.fasterxml.jackson.core:jackson-databind:2.12.0')
              }
              """
          )
        );
    }

    @Test
    void kotlinDslAddConstraint() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                `java-library`
              }
              repositories { mavenCentral() }

              dependencies {
                  implementation("org.openrewrite:rewrite-java:7.0.0")
              }
              """,
            """
              plugins {
                `java-library`
              }
              repositories { mavenCentral() }

              dependencies {
                  constraints {
                      implementation("com.fasterxml.jackson.core:jackson-core:2.12.5") {
                          because("CVE-2024-BAD")
                      }
                  }

                  implementation("org.openrewrite:rewrite-java:7.0.0")
              }
              """
          )
        );
    }

    @Test
    void kotlinDslUpdateConstraint() {
        rewriteRun(
          buildGradleKts(
            """
              plugins { id("java") }
              repositories { mavenCentral() }

              dependencies {
                  constraints {
                      implementation("com.fasterxml.jackson.core:jackson-core:2.12.0") {
                          because("some reason")
                      }
                  }

                  implementation("org.openrewrite:rewrite-java:7.0.0")
              }
              """,
            """
              plugins { id("java") }
              repositories { mavenCentral() }

              dependencies {
                  constraints {
                      implementation("com.fasterxml.jackson.core:jackson-core:2.12.5") {
                          because("CVE-2024-BAD")
                      }
                  }

                  implementation("org.openrewrite:rewrite-java:7.0.0")
              }
              """
          )
        );
    }

    @Test
    void kotlinDslUseResolutionStrategyWhenSpringDependencyManagementPluginIsPresent() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  `java-library`
                  id("io.spring.dependency-management") version "1.1.5"
              }
              repositories { mavenCentral() }

              dependencies {

                  implementation("org.openrewrite:rewrite-java:7.0.0")
              }
              """,
            """
              plugins {
                  `java-library`
                  id("io.spring.dependency-management") version "1.1.5"
              }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency { details ->
                      if (details.requested.group == "com.fasterxml.jackson.core" && details.requested.name == "jackson-core") {
                          details.useVersion("2.12.5")
                          details.because("CVE-2024-BAD")
                      }
                  }
              }

              dependencies {

                  implementation("org.openrewrite:rewrite-java:7.0.0")
              }
              """
          )
        );
    }

    @Test
    void canHandleNullScannedAccumulator() {
        UpgradeTransitiveDependencyVersion updateClassgraph = new UpgradeTransitiveDependencyVersion("io.github.classgraph", "classgraph", "4.8.112", null, null, null);
        UpgradeTransitiveDependencyVersion updateJackson = new UpgradeTransitiveDependencyVersion("com.fasterxml*", "jackson-core", "2.12.5", null, "CVE-2024-BAD", null);
        rewriteRun(
          spec -> spec.recipe(new ScanningAccumulatedUpgradeRecipe(updateClassgraph, updateJackson)),
          buildGradle(
            """
              plugins {
                id 'java'
              }
              repositories { mavenCentral() }

              dependencies {
                  implementation 'org.openrewrite:rewrite-java:7.0.0'
              }
              """,
            """
              plugins {
                id 'java'
              }
              repositories { mavenCentral() }

              dependencies {
                  constraints {
                      implementation('com.fasterxml.jackson.core:jackson-core:2.12.5') {
                          because 'CVE-2024-BAD'
                      }
                  }

                  implementation 'org.openrewrite:rewrite-java:7.0.0'
              }
              """
          )
        );
    }

    @Test
    void canUseAnyWildcardForMultipleMatchingArtifactIds() {
        rewriteRun(spec ->
            spec.recipe(new UpgradeTransitiveDependencyVersion("org.apache.tomcat.embed", "*", "10.1.42", null, null, null)),
          buildGradle(
            """
              plugins {
                id 'java'
              }
              repositories { mavenCentral() }

              dependencies {
                  implementation 'org.springframework.boot:spring-boot-starter-tomcat:3.3.12'
              }
              """,
            """
              plugins {
                id 'java'
              }
              repositories { mavenCentral() }

              dependencies {
                  constraints {
                      implementation('org.apache.tomcat.embed:tomcat-embed-core:10.1.42')
                      implementation('org.apache.tomcat.embed:tomcat-embed-el:10.1.42')
                      implementation('org.apache.tomcat.embed:tomcat-embed-websocket:10.1.42')
                  }

                  implementation 'org.springframework.boot:spring-boot-starter-tomcat:3.3.12'
              }
              """
          )
        );
    }

    @Test
    void doesNotAddRedundantConstraintWhenImplementationAlreadyHasIt() {
        rewriteRun(
          spec -> spec
            .recipe(new UpgradeTransitiveDependencyVersion(
              "com.fasterxml.jackson.core", "jackson-databind", "2.12.5", null, null, List.of("testImplementation"))),
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }

              dependencies {
                  constraints {
                      implementation 'com.fasterxml.jackson.core:jackson-databind:2.12.5'
                  }

                  implementation 'org.openrewrite:rewrite-java:7.0.0'
                  testImplementation 'org.junit.jupiter:junit-jupiter:5.9.0'
              }
              """
          )
        );
    }

    @Test
    void replacesTestImplementationConstraintWithImplementation() {
        rewriteRun(
          spec -> spec
            .recipe(new UpgradeTransitiveDependencyVersion(
              "com.fasterxml.jackson.core", "jackson-databind", "2.12.5", null, null, null)),
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }

              dependencies {
                  constraints {
                      testImplementation 'com.fasterxml.jackson.core:jackson-databind:2.12.5'
                  }

                  implementation 'org.openrewrite:rewrite-java:7.0.0'
                  testImplementation 'org.junit.jupiter:junit-jupiter:5.9.0'
              }
              """,
            """
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }

              dependencies {
                  constraints {
                      implementation('com.fasterxml.jackson.core:jackson-databind:2.12.5')
                  }

                  implementation 'org.openrewrite:rewrite-java:7.0.0'
                  testImplementation 'org.junit.jupiter:junit-jupiter:5.9.0'
              }
              """
          )
        );
    }

    @Test
    void updateConstraintForVersionInSettings() {
        rewriteRun(
          buildGradle(
            """
              plugins { id 'java' }
              repositories { mavenCentral() }

              dependencies {
                  constraints {
                      implementation("com.fasterxml.jackson.core:jackson-core:${gradle.jacksonVersion}") {
                          because 'some reason'
                      }
                  }

                  implementation 'org.openrewrite:rewrite-java:7.0.0'
              }
              """,
            """
              plugins { id 'java' }
              repositories { mavenCentral() }

              dependencies {
                  constraints {
                      implementation("com.fasterxml.jackson.core:jackson-core:${gradle.jacksonVersion}") {
                          because 'CVE-2024-BAD'
                      }
                  }

                  implementation 'org.openrewrite:rewrite-java:7.0.0'
              }
              """
          ),
          settingsGradle(
            """
              gradle.ext {
                  jacksonVersion = '2.12.0'
              }
              """,
            """
              gradle.ext {
                  jacksonVersion = '2.12.5'
              }
              """
          )
        );
    }

    @Test
    void noChangesIfDependencyIsAlsoPresentOnProjectForVersionInSettings() {
        rewriteRun(
          buildGradle(
            """
              plugins { id 'java' }
              repositories { mavenCentral() }

              dependencies {
                  implementation "com.fasterxml.jackson.core:jackson-core:${gradle.jacksonVersion}"
                  implementation "com.fasterxml.jackson.core:jackson-databind:${gradle.jacksonVersion}"
              }
              """
          ),
          settingsGradle(
            """
              gradle.ext {
                  jacksonVersion = '2.12.0'
              }
              """
          )
        );
    }

    @Test
    void upgradeDependencyVersionBeforeApplyingTheUpgradeTransitiveDependencyVersion() {
        rewriteRun(
          // We use an imperative recipe rather than a declarative recipeList, because in a declarative recipeList, all recipes perform their scanning phase first and only then perform their editing phase.
          // In this test case, we want the UpgradeTransitiveDependencyVersion recipe to scan and edit based on the output of the first recipe’s changes,
          // to reflect a recipe downstream that uses this kind of setup.
          spec -> spec.recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              @Override
              public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                  if (tree instanceof G.CompilationUnit) {
                      // A recipe that updates an existing dependency in the Gradle build file.
                      // That dependency itself declares the one targeted by UpgradeTransitiveDependencyVersion as a transitive dependency.
                      // In this case, "org.openrewrite:rewrite-java" has "com.fasterxml.jackson.core:jackson-databind" as a transitive dependency.
                      UpgradeDependencyVersion upgrade = new UpgradeDependencyVersion("org.openrewrite", "rewrite-java", "8.0.0", null);
                      UpgradeDependencyVersion.DependencyVersionState acc = upgrade.getInitialValue(ctx);
                      upgrade.getScanner(acc).visit(tree, ctx);
                      tree = upgrade.getVisitor(acc).visit(tree, ctx);

                      // Use the changed tree as input for a UpgradeTransitiveDependencyVersion run
                      UpgradeTransitiveDependencyVersion upgradeTransitive = new UpgradeTransitiveDependencyVersion("com.fasterxml.jackson.core", "jackson-databind", "2.15.1", null, null, null);
                      UpgradeTransitiveDependencyVersion.DependencyVersionState accTransitive2 = upgradeTransitive.getInitialValue(ctx);
                      upgradeTransitive.getScanner(accTransitive2).visit(tree, ctx);
                      tree = upgradeTransitive.getVisitor(accTransitive2).visit(tree, ctx);
                  }
                  return super.visit(tree, ctx);
              }
          })),
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }

              dependencies {
                  constraints {
                      implementation 'com.fasterxml.jackson.core:jackson-databind:2.12.0'
                  }
                  implementation 'org.openrewrite:rewrite-java:7.0.0'
              }
              """,
            """
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }

              dependencies {
                  constraints {
                      implementation 'com.fasterxml.jackson.core:jackson-databind:2.15.1'
                  }
                  implementation 'org.openrewrite:rewrite-java:8.0.0'
              }
              """
          )
        );
    }

    @EqualsAndHashCode(callSuper = false)
    @Value
    public static class ScanningAccumulatedUpgradeRecipe extends ScanningRecipe<UpgradeTransitiveDependencyVersion.DependencyVersionState> {
        @Override
        public String getDisplayName() {
            return "Accumulation-scanned recipe";
        }

        @Override
        public String getDescription() {
            return "Some recipes hava loop to determine all updates and add them to the scanner. This cycle/recipe only can update for the provided dependency.";
        }

        private final UpgradeTransitiveDependencyVersion scanAlsoFor;
        private final UpgradeTransitiveDependencyVersion upgradeDependency;

        @Override
        public UpgradeTransitiveDependencyVersion.DependencyVersionState getInitialValue(ExecutionContext ctx) {
            return new UpgradeTransitiveDependencyVersion.DependencyVersionState();
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getScanner(UpgradeTransitiveDependencyVersion.DependencyVersionState acc) {
            return new TreeVisitor<>() {
                @Override
                public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                    if (tree instanceof SourceFile) {
                        tree = scanAlsoFor.getScanner(acc).visit(tree, ctx);
                        tree = upgradeDependency.getScanner(acc).visit(tree, ctx);
                    }
                    return tree;
                }
            };
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor(UpgradeTransitiveDependencyVersion.DependencyVersionState acc) {
            return upgradeDependency.getVisitor(acc);
        }
    }
}
