/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package org.openrewrite.gradle;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

@SuppressWarnings("GroovyAssignabilityCheck")
class SyncGradleResolutionStrategyPinsWithBomGroovyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
          .recipe(new SyncGradleResolutionStrategyPinsWithBom());
    }

    @DocumentExample
    @Test
    void upgradesPinWhenBomProvidesStrictlyHigher() {
        rewriteRun(
          buildGradle(
            """
              plugins { id 'java' }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency { details ->
                      if (details.requested.group == 'com.fasterxml.jackson.core' && details.requested.name == 'jackson-databind') {
                          details.useVersion '2.12.5'
                      }
                  }
              }
              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'com.fasterxml.jackson.core:jackson-databind'
              }
              """,
            """
              plugins { id 'java' }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency { details ->
                      if (details.requested.group == 'com.fasterxml.jackson.core' && details.requested.name == 'jackson-databind') {
                          details.useVersion '2.17.2'
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
    void leavesPinAloneWhenAboveBom() {
        rewriteRun(
          buildGradle(
            """
              plugins { id 'java' }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency { details ->
                      if (details.requested.group == 'com.fasterxml.jackson.core' && details.requested.name == 'jackson-databind') {
                          details.useVersion '2.20.0'
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
    void leavesPinAloneWhenEqualToBom() {
        rewriteRun(
          buildGradle(
            """
              plugins { id 'java' }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency { details ->
                      if (details.requested.group == 'com.fasterxml.jackson.core' && details.requested.name == 'jackson-databind') {
                          details.useVersion '2.17.2'
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
    void skipsBranchWithBecauseClause() {
        // A `because` clause is a human-authored signal — we always leave those alone (drop recipe's job).
        rewriteRun(
          buildGradle(
            """
              plugins { id 'java' }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency { details ->
                      if (details.requested.group == 'com.fasterxml.jackson.core' && details.requested.name == 'jackson-databind') {
                          details.useVersion '2.12.5'
                          details.because 'some reason'
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
    void leavesPinAloneWhenNoBomEntry() {
        // Guava isn't in Spring Boot BOM, so there's no managed version to compare against → leave alone.
        rewriteRun(
          buildGradle(
            """
              plugins { id 'java' }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency { details ->
                      if (details.requested.group == 'com.google.guava' && details.requested.name == 'guava') {
                          details.useVersion '32.1.3-jre'
                      }
                  }
              }
              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'com.google.guava:guava'
              }
              """
          )
        );
    }

    @Test
    void upgradesInListWhenAllArtifactsShareOneBomVersion() {
        // Spring Framework artifacts move in lockstep; all three resolve to 6.1.12 under boot 3.3.3.
        // Should just rewrite the literal, keeping the `in [...]` list intact.
        rewriteRun(
          buildGradle(
            """
              plugins { id 'java' }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency { details ->
                      if (details.requested.group == 'org.springframework' && details.requested.name in ['spring-web', 'spring-webmvc', 'spring-core']) {
                          details.useVersion '6.1.0'
                      }
                  }
              }
              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'org.springframework:spring-web'
                  implementation 'org.springframework:spring-webmvc'
                  implementation 'org.springframework:spring-core'
              }
              """,
            """
              plugins { id 'java' }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency { details ->
                      if (details.requested.group == 'org.springframework' && details.requested.name in ['spring-web', 'spring-webmvc', 'spring-core']) {
                          details.useVersion '6.1.12'
                      }
                  }
              }
              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'org.springframework:spring-web'
                  implementation 'org.springframework:spring-webmvc'
                  implementation 'org.springframework:spring-core'
              }
              """
          )
        );
    }

    @org.junit.jupiter.api.Disabled("Gradle tooling API seems to return an empty marker when the fixture contains a multi-branch resolution rule; same limitation as RemoveRedundantSecurityResolutionRules#removeMultipleRulesFromElseIfChain")
    @Test
    void preservesExistingElseIfChain() {
        rewriteRun(
          buildGradle(
            """
              plugins { id 'java' }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency { details ->
                      if (details.requested.group == 'com.fasterxml.jackson.core' && details.requested.name == 'jackson-databind') {
                          details.useVersion '2.12.5'
                      } else if (details.requested.group == 'org.apache.commons' && details.requested.name == 'commons-lang3') {
                          details.useVersion '3.20'
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
              plugins { id 'java' }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency { details ->
                      if (details.requested.group == 'com.fasterxml.jackson.core' && details.requested.name == 'jackson-databind') {
                          details.useVersion '2.17.2'
                      } else if (details.requested.group == 'org.apache.commons' && details.requested.name == 'commons-lang3') {
                          details.useVersion '3.20'
                      }
                  }
              }
              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'com.fasterxml.jackson.core:jackson-databind'
                  implementation 'org.apache.commons:commons-lang3'
              }
              """
          )
        );
    }

    // Divergent targets: of two listed artifacts, one is BOM-managed above the pin and the other has no BOM entry.
    // Expected: split into two branches — the first keeps a filtered `in [...]` would collapse to `==` since only
    // one artifact lands in each group here.
    @Test
    void splitsListWhenBomDivergesFromPinForSomeArtifacts() {
        rewriteRun(
          buildGradle(
            """
              plugins { id 'java' }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency { details ->
                      if (details.requested.group == 'com.fasterxml.jackson.core' && details.requested.name in ['jackson-databind', 'jackson-fictional']) {
                          details.useVersion '2.15.0'
                      }
                  }
              }
              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'com.fasterxml.jackson.core:jackson-databind'
              }
              """,
            """
              plugins { id 'java' }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency { details ->
                      if (details.requested.group == 'com.fasterxml.jackson.core' && details.requested.name == 'jackson-databind') {
                          details.useVersion '2.17.2'
                      } else if (details.requested.group == 'com.fasterxml.jackson.core' && details.requested.name == 'jackson-fictional') {
                          details.useVersion '2.15.0'
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

    // Divergent targets with grouping: three artifacts, two share one BOM-managed version (same group), one is unmanaged.
    // Expected: the two sharing a target keep a filtered `in [...]`; the third collapses to `==`.
    @Test
    void groupsArtifactsSharingTheSameTargetVersion() {
        rewriteRun(
          buildGradle(
            """
              plugins { id 'java' }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency { details ->
                      if (details.requested.group == 'com.fasterxml.jackson.core' && details.requested.name in ['jackson-databind', 'jackson-core', 'jackson-fictional']) {
                          details.useVersion '2.15.0'
                      }
                  }
              }
              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'com.fasterxml.jackson.core:jackson-databind'
                  implementation 'com.fasterxml.jackson.core:jackson-core'
              }
              """,
            """
              plugins { id 'java' }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency { details ->
                      if (details.requested.group == 'com.fasterxml.jackson.core' && details.requested.name in ['jackson-databind', 'jackson-core']) {
                          details.useVersion '2.17.2'
                      } else if (details.requested.group == 'com.fasterxml.jackson.core' && details.requested.name == 'jackson-fictional') {
                          details.useVersion '2.15.0'
                      }
                  }
              }
              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'com.fasterxml.jackson.core:jackson-databind'
                  implementation 'com.fasterxml.jackson.core:jackson-core'
              }
              """
          )
        );
    }

    // 4 artifacts, 2 share one target and 2 share another → should emit TWO `in [...]` branches
    // (not four `==` branches). Preserves each group's original list formatting.
    @Test
    void groupsMultipleArtifactsOnEachSideOfDivergence() {
        rewriteRun(
          buildGradle(
            """
              plugins { id 'java' }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency { details ->
                      if (details.requested.group == 'com.fasterxml.jackson.core' && details.requested.name in ['jackson-databind', 'jackson-core', 'jackson-fictional-a', 'jackson-fictional-b']) {
                          details.useVersion '2.15.0'
                      }
                  }
              }
              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'com.fasterxml.jackson.core:jackson-databind'
                  implementation 'com.fasterxml.jackson.core:jackson-core'
              }
              """,
            """
              plugins { id 'java' }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency { details ->
                      if (details.requested.group == 'com.fasterxml.jackson.core' && details.requested.name in ['jackson-databind', 'jackson-core']) {
                          details.useVersion '2.17.2'
                      } else if (details.requested.group == 'com.fasterxml.jackson.core' && details.requested.name in ['jackson-fictional-a', 'jackson-fictional-b']) {
                          details.useVersion '2.15.0'
                      }
                  }
              }
              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'com.fasterxml.jackson.core:jackson-databind'
                  implementation 'com.fasterxml.jackson.core:jackson-core'
              }
              """
          )
        );
    }

    // `||` chain input, all leaves share one target → rewrite just the useVersion literal, keep the chain.
    @Test
    void upgradesOrChainWhenAllArtifactsShareOneBomVersion() {
        rewriteRun(
          buildGradle(
            """
              plugins { id 'java' }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency { details ->
                      if (details.requested.group == 'com.fasterxml.jackson.core' && (details.requested.name == 'jackson-databind' || details.requested.name == 'jackson-core')) {
                          details.useVersion '2.12.5'
                      }
                  }
              }
              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'com.fasterxml.jackson.core:jackson-databind'
                  implementation 'com.fasterxml.jackson.core:jackson-core'
              }
              """,
            """
              plugins { id 'java' }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency { details ->
                      if (details.requested.group == 'com.fasterxml.jackson.core' && (details.requested.name == 'jackson-databind' || details.requested.name == 'jackson-core')) {
                          details.useVersion '2.17.2'
                      }
                  }
              }
              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'com.fasterxml.jackson.core:jackson-databind'
                  implementation 'com.fasterxml.jackson.core:jackson-core'
              }
              """
          )
        );
    }

    // `||` chain input, divergent targets → split into filtered `||` sub-chains and/or `==` collapses.
    @Test
    void splitsOrChainWhenBomDivergesFromPinForSomeArtifacts() {
        rewriteRun(
          buildGradle(
            """
              plugins { id 'java' }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency { details ->
                      if (details.requested.group == 'com.fasterxml.jackson.core' && (details.requested.name == 'jackson-databind' || details.requested.name == 'jackson-core' || details.requested.name == 'jackson-fictional')) {
                          details.useVersion '2.15.0'
                      }
                  }
              }
              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'com.fasterxml.jackson.core:jackson-databind'
                  implementation 'com.fasterxml.jackson.core:jackson-core'
              }
              """,
            """
              plugins { id 'java' }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency { details ->
                      if (details.requested.group == 'com.fasterxml.jackson.core' && (details.requested.name == 'jackson-databind' || details.requested.name == 'jackson-core')) {
                          details.useVersion '2.17.2'
                      } else if (details.requested.group == 'com.fasterxml.jackson.core' && details.requested.name == 'jackson-fictional') {
                          details.useVersion '2.15.0'
                      }
                  }
              }
              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'com.fasterxml.jackson.core:jackson-databind'
                  implementation 'com.fasterxml.jackson.core:jackson-core'
              }
              """
          )
        );
    }

    @Test
    void doesNothingWhenNoResolutionStrategyBlock() {
        rewriteRun(
          buildGradle(
            """
              plugins { id 'java' }
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
