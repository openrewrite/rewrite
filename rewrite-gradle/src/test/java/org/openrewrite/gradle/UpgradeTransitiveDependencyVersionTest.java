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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

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
    @DocumentExample
    void IncludedDefaultConfigurationsReceiveRuntimeConstraints() {
        rewriteRun(
          spec -> spec
            .recipe(new UpgradeTransitiveDependencyVersion(
              "org.apache.commons", "commons-lang3", "3.14.0", null, null, List.of("implementation", "runtimeOnly"))),
          buildGradle(
            """
              plugins {
                  id 'info.solidsoft.pitest' version '1.15.0'
                  id 'java'
              }
              repositories { mavenCentral() }
              dependencies {
                  compileOnly 'org.apache.activemq:artemis-jakarta-server:2.28.0'
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
                      implementation('org.apache.commons:commons-lang3:3.14.0')
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
                  deploy 'org.openrewrite:rewrite-java:7.0.0'
              
                  constraints {
                  }
              }
              """,
            """
              plugins { id 'ear' }
              repositories { mavenCentral() }
              configurations.earlib.extendsFrom configurations.deploy
              dependencies {
                  deploy 'org.openrewrite:rewrite-java:7.0.0'
              
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

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/4228")
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
}
