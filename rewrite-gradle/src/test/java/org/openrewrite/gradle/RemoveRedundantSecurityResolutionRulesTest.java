/*
 * Copyright 2025 the original author or authors.
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
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

@SuppressWarnings("GroovyAssignabilityCheck")
class RemoveRedundantSecurityResolutionRulesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
          .recipe(new RemoveRedundantSecurityResolutionRules(null));
    }

    /**
     * Removes the resolution rule because the Spring Boot BOM (3.3.3) manages jackson-databind
     * to version 2.17.2, which is newer than the pinned version 2.12.5. Since the managed version
     * already addresses the CVE (indicated by the "CVE-2024-BAD" reason), the manual version
     * constraint is redundant and can be safely removed.
     */
    @DocumentExample
    @Test
    void removeRedundantCveRule() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency { details ->
                      if (details.requested.group == 'com.fasterxml.jackson.core' && details.requested.name == 'jackson-databind') {
                          details.useVersion('2.12.5')
                          details.because('CVE-2024-BAD')
                      }
                  }
              }
              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'com.fasterxml.jackson.core:jackson-databind'
              }
              """,
            """
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }
              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'com.fasterxml.jackson.core:jackson-databind'
              }
              """
          )
        );
    }

    /**
     * Keeps the resolution rule because the pinned version (2.20.0) is higher than the version
     * managed by the Spring Boot BOM (2.17.2). The CVE fix requires a version that the BOM
     * does not yet provide, so the manual constraint is still necessary to ensure the
     * vulnerability is addressed.
     */
    @Test
    void keepRuleWhenManagedVersionIsLower() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency { details ->
                      if (details.requested.group == 'com.fasterxml.jackson.core' && details.requested.name == 'jackson-databind') {
                          details.useVersion('2.20.0')
                          details.because('CVE-2099-FUTURE')
                      }
                  }
              }
              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'com.fasterxml.jackson.core:jackson-databind'
              }
              """
          )
        );
    }

    @Test
    void keepRuleWithoutSecurityReason() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency { details ->
                      if (details.requested.group == 'com.fasterxml.jackson.core' && details.requested.name == 'jackson-databind') {
                          details.useVersion('2.12.5')
                          details.because('We need this specific version for compatibility')
                      }
                  }
              }
              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'com.fasterxml.jackson.core:jackson-databind'
              }
              """
          )
        );
    }

    @Test
    void removeRuleWithCustomPattern() {
        rewriteRun(
          spec -> spec.recipe(new RemoveRedundantSecurityResolutionRules("vulnerability")),
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency { details ->
                      if (details.requested.group == 'com.fasterxml.jackson.core' && details.requested.name == 'jackson-databind') {
                          details.useVersion('2.12.5')
                          details.because('Security vulnerability fix')
                      }
                  }
              }
              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'com.fasterxml.jackson.core:jackson-databind'
              }
              """,
            """
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }
              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'com.fasterxml.jackson.core:jackson-databind'
              }
              """
          )
        );
    }

    @Test
    void removeMultipleRulesFromElseIfChain() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency { details ->
                      if (details.requested.group == 'com.fasterxml.jackson.core' && details.requested.name == 'jackson-databind') {
                          details.useVersion('2.12.5')
                          details.because('CVE-2024-OLD')
                      } else if (details.requested.group == 'org.apache.commons' && details.requested.name == 'commons-lang3') {
                          details.useVersion('3.10.0')
                          details.because('GHSA-abcd-efgh-ijkl')
                      }
                  }
              }
              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'com.fasterxml.jackson.core:jackson-databind'
                  implementation 'org.apache.commons:commons-lang3'
              }
              """,
            """
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }
              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'com.fasterxml.jackson.core:jackson-databind'
                  implementation 'org.apache.commons:commons-lang3'
              }
              """
          )
        );
    }

    @Test
    void keepBuildscriptRuleWhenNoPlatformInBuildscript() {
        rewriteRun(
          buildGradle(
            """
              buildscript {
                  repositories { mavenCentral() }
                  configurations.all {
                      resolutionStrategy.eachDependency { details ->
                          if (details.requested.group == 'com.fasterxml.jackson.core' && details.requested.name == 'jackson-databind') {
                              details.useVersion('2.12.5')
                              details.because('CVE-2024-BAD')
                          }
                      }
                  }
              }
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }
              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'com.fasterxml.jackson.core:jackson-databind'
              }
              """
          )
        );
    }

    @Test
    void keepRuleWithoutBecauseClause() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency { details ->
                      if (details.requested.group == 'com.fasterxml.jackson.core' && details.requested.name == 'jackson-databind') {
                          details.useVersion('2.12.5')
                      }
                  }
              }
              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'com.fasterxml.jackson.core:jackson-databind'
              }
              """
          )
        );
    }

    @Test
    void keepRuleWhenNoPlatformPresent() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency { details ->
                      if (details.requested.group == 'com.fasterxml.jackson.core' && details.requested.name == 'jackson-databind') {
                          details.useVersion('2.12.5')
                          details.because('CVE-2024-BAD')
                      }
                  }
              }
              dependencies {
                  implementation 'com.fasterxml.jackson.core:jackson-databind:2.17.0'
              }
              """
          )
        );
    }

    @Test
    void removeRuleButKeepCustomConfiguration() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }
              configurations {
                  custom
                  all {
                      resolutionStrategy.eachDependency { details ->
                          if (details.requested.group == 'com.fasterxml.jackson.core' && details.requested.name == 'jackson-databind') {
                              details.useVersion('2.12.5')
                              details.because('CVE-2024-BAD')
                          }
                      }
                  }
              }
              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'com.fasterxml.jackson.core:jackson-databind'
              }
              """,
            // Note: There's a trailing blank line inside configurations {} due to whitespace handling
            // when removing the all {} block. This is a known cosmetic issue.
            """
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }
              configurations {
                  custom
                 \s
              }
              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'com.fasterxml.jackson.core:jackson-databind'
              }
              """
          )
        );
    }

    /**
     * Verifies that a configurations block with only custom configuration declarations
     * (no resolution strategy) is left unchanged by the recipe.
     */
    @Test
    void keepConfigurationsBlockWithoutResolutionStrategy() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }
              configurations {
                  customOne
                  customTwo
              }
              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'com.fasterxml.jackson.core:jackson-databind'
              }
              """
          )
        );
    }

    /**
     * Removes the resolution rule defined on a specific configuration ({@code compileClasspath}) rather than
     * {@code configurations.all}. The recipe should handle resolution strategies on any resolvable configuration.
     */
    @Test
    void removeRedundantRuleOnSpecificConfiguration() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }
              configurations.compileClasspath {
                  resolutionStrategy.eachDependency { details ->
                      if (details.requested.group == 'com.fasterxml.jackson.core' && details.requested.name == 'jackson-databind') {
                          details.useVersion('2.12.5')
                          details.because('CVE-2024-BAD')
                      }
                  }
              }
              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'com.fasterxml.jackson.core:jackson-databind'
              }
              """,
            """
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }
              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'com.fasterxml.jackson.core:jackson-databind'
              }
              """
          )
        );
    }

    @Test
    void removeRedundantRuleOnNestedConfiguration() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }
              configurations {
                  compileClasspath {
                      resolutionStrategy {
                          eachDependency { details ->
                              if (details.requested.group == 'com.fasterxml.jackson.core' && details.requested.name == 'jackson-databind') {
                                  details.useVersion('2.12.5')
                                  details.because('CVE-2024-BAD')
                              }
                          }
                      }
                  }
              }
              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'com.fasterxml.jackson.core:jackson-databind'
              }
              """,
            """
              plugins {
                  id 'java'
              }
              repositories { mavenCentral() }
              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'com.fasterxml.jackson.core:jackson-databind'
              }
              """
          )
        );
    }
}
