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
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.gradle.Assertions.*;
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

    @Test
    void addConstraintUpdatesLockfile() {
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
          ),
          lockfile(
            """
              # This is a Gradle generated file for dependency locking.
              # Manual edits can break the build and are not advised.
              # This file is expected to be part of source control.
              com.fasterxml.jackson.core:jackson-annotations:2.12.2=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              com.fasterxml.jackson.core:jackson-core:2.12.2=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              com.fasterxml.jackson.core:jackson-databind:2.12.2=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              com.fasterxml.jackson.dataformat:jackson-dataformat-smile:2.12.2=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              com.fasterxml.jackson.module:jackson-module-kotlin:2.12.2=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              com.fasterxml.jackson.module:jackson-module-parameter-names:2.12.2=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              com.fasterxml.jackson:jackson-bom:2.12.2=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              com.ibm.icu:icu4j:61.1=runtimeClasspath,testRuntimeClasspath
              commons-lang:commons-lang:2.6=runtimeClasspath,testRuntimeClasspath
              io.github.classgraph:classgraph:4.8.102=runtimeClasspath,testRuntimeClasspath
              io.micrometer:micrometer-core:1.6.5=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              org.abego.treelayout:org.abego.treelayout.core:1.0.3=runtimeClasspath,testRuntimeClasspath
              org.antlr:ST4:4.3=runtimeClasspath,testRuntimeClasspath
              org.antlr:antlr-runtime:3.5.2=runtimeClasspath,testRuntimeClasspath
              org.antlr:antlr4-runtime:4.8-1=runtimeClasspath,testRuntimeClasspath
              org.antlr:antlr4:4.8-1=runtimeClasspath,testRuntimeClasspath
              org.glassfish:javax.json:1.0.4=runtimeClasspath,testRuntimeClasspath
              org.hdrhistogram:HdrHistogram:2.1.12=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              org.jetbrains.kotlin:kotlin-reflect:1.4.21=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              org.jetbrains.kotlin:kotlin-stdlib-common:1.4.31=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.4.31=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.4.31=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              org.jetbrains.kotlin:kotlin-stdlib:1.4.31=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              org.jetbrains:annotations:20.1.0=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              org.latencyutils:LatencyUtils:2.0.3=runtimeClasspath,testRuntimeClasspath
              org.openrewrite:rewrite-core:7.0.0=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              org.openrewrite:rewrite-java:7.0.0=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              org.ow2.asm:asm-analysis:9.1=runtimeClasspath,testRuntimeClasspath
              org.ow2.asm:asm-tree:9.1=runtimeClasspath,testRuntimeClasspath
              org.ow2.asm:asm-util:9.1=runtimeClasspath,testRuntimeClasspath
              org.ow2.asm:asm:9.1=runtimeClasspath,testRuntimeClasspath
              org.yaml:snakeyaml:1.28=runtimeClasspath,testRuntimeClasspath
              empty=annotationProcessor,testAnnotationProcessor
              """,
            """
              # This is a Gradle generated file for dependency locking.
              # Manual edits can break the build and are not advised.
              # This file is expected to be part of source control.
              com.fasterxml.jackson.core:jackson-annotations:2.12.5=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              com.fasterxml.jackson.core:jackson-core:2.12.5=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              com.fasterxml.jackson.core:jackson-databind:2.12.5=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              com.fasterxml.jackson.dataformat:jackson-dataformat-smile:2.12.5=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              com.fasterxml.jackson.module:jackson-module-kotlin:2.12.5=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              com.fasterxml.jackson.module:jackson-module-parameter-names:2.12.5=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              com.fasterxml.jackson:jackson-bom:2.12.5=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              com.ibm.icu:icu4j:61.1=runtimeClasspath,testRuntimeClasspath
              commons-lang:commons-lang:2.6=runtimeClasspath,testRuntimeClasspath
              io.github.classgraph:classgraph:4.8.102=runtimeClasspath,testRuntimeClasspath
              io.micrometer:micrometer-core:1.6.5=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              org.abego.treelayout:org.abego.treelayout.core:1.0.3=runtimeClasspath,testRuntimeClasspath
              org.antlr:ST4:4.3=runtimeClasspath,testRuntimeClasspath
              org.antlr:antlr-runtime:3.5.2=runtimeClasspath,testRuntimeClasspath
              org.antlr:antlr4-runtime:4.8-1=runtimeClasspath,testRuntimeClasspath
              org.antlr:antlr4:4.8-1=runtimeClasspath,testRuntimeClasspath
              org.glassfish:javax.json:1.0.4=runtimeClasspath,testRuntimeClasspath
              org.hdrhistogram:HdrHistogram:2.1.12=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              org.jetbrains.kotlin:kotlin-reflect:1.4.21=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              org.jetbrains.kotlin:kotlin-stdlib-common:1.4.31=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.4.31=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.4.31=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              org.jetbrains.kotlin:kotlin-stdlib:1.4.31=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              org.jetbrains:annotations:20.1.0=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              org.latencyutils:LatencyUtils:2.0.3=runtimeClasspath,testRuntimeClasspath
              org.openrewrite:rewrite-core:7.0.0=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              org.openrewrite:rewrite-java:7.0.0=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              org.ow2.asm:asm-analysis:9.1=runtimeClasspath,testRuntimeClasspath
              org.ow2.asm:asm-tree:9.1=runtimeClasspath,testRuntimeClasspath
              org.ow2.asm:asm-util:9.1=runtimeClasspath,testRuntimeClasspath
              org.ow2.asm:asm:9.1=runtimeClasspath,testRuntimeClasspath
              org.yaml:snakeyaml:1.28=runtimeClasspath,testRuntimeClasspath
              empty=annotationProcessor,testAnnotationProcessor
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

    @Value
    @EqualsAndHashCode(callSuper = false)
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
