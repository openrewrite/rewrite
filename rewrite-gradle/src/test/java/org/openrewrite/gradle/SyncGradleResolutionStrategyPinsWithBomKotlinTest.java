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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

class SyncGradleResolutionStrategyPinsWithBomKotlinTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
          .recipe(new SyncGradleResolutionStrategyPinsWithBom());
    }

    @Test
    void upgradesPinWhenBomProvidesStrictlyHigher() {
        rewriteRun(
          buildGradleKts(
            """
              plugins { id("java") }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency {
                      if (requested.group == "com.fasterxml.jackson.core" && requested.name == "jackson-databind") {
                          useVersion("2.12.5")
                      }
                  }
              }
              dependencies {
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("com.fasterxml.jackson.core:jackson-databind")
              }
              """,
            """
              plugins { id("java") }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency {
                      if (requested.group == "com.fasterxml.jackson.core" && requested.name == "jackson-databind") {
                          useVersion("2.17.2")
                      }
                  }
              }
              dependencies {
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("com.fasterxml.jackson.core:jackson-databind")
              }
              """
          )
        );
    }

    @Test
    void leavesPinAloneWhenAboveBom() {
        rewriteRun(
          buildGradleKts(
            """
              plugins { id("java") }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency {
                      if (requested.group == "com.fasterxml.jackson.core" && requested.name == "jackson-databind") {
                          useVersion("2.20.0")
                      }
                  }
              }
              dependencies {
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("com.fasterxml.jackson.core:jackson-databind")
              }
              """
          )
        );
    }

    // Kotlin `in listOf(...)` input, all share one target → rewrite the useVersion literal, keep listOf intact.
    @Test
    void upgradesInListOfWhenAllArtifactsShareOneBomVersion() {
        rewriteRun(
          buildGradleKts(
            """
              plugins { id("java") }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency {
                      if (requested.group == "com.fasterxml.jackson.core" && requested.name in listOf("jackson-databind", "jackson-core")) {
                          useVersion("2.12.5")
                      }
                  }
              }
              dependencies {
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("com.fasterxml.jackson.core:jackson-databind")
                  implementation("com.fasterxml.jackson.core:jackson-core")
              }
              """,
            """
              plugins { id("java") }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency {
                      if (requested.group == "com.fasterxml.jackson.core" && requested.name in listOf("jackson-databind", "jackson-core")) {
                          useVersion("2.17.2")
                      }
                  }
              }
              dependencies {
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("com.fasterxml.jackson.core:jackson-databind")
                  implementation("com.fasterxml.jackson.core:jackson-core")
              }
              """
          )
        );
    }

    // Kotlin `in listOf(...)` input, divergent targets → split into filtered listOf(...) groups + collapses.
    @Test
    void splitsInListOfWhenBomDivergesFromPinForSomeArtifacts() {
        rewriteRun(
          buildGradleKts(
            """
              plugins { id("java") }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency {
                      if (requested.group == "com.fasterxml.jackson.core" && requested.name in listOf("jackson-databind", "jackson-core", "jackson-fictional")) {
                          useVersion("2.15.0")
                      }
                  }
              }
              dependencies {
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("com.fasterxml.jackson.core:jackson-databind")
                  implementation("com.fasterxml.jackson.core:jackson-core")
              }
              """,
            """
              plugins { id("java") }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency {
                      if (requested.group == "com.fasterxml.jackson.core" && requested.name in listOf("jackson-databind", "jackson-core")) {
                          useVersion("2.17.2")
                      } else if (requested.group == "com.fasterxml.jackson.core" && requested.name == "jackson-fictional") {
                          useVersion("2.15.0")
                      }
                  }
              }
              dependencies {
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("com.fasterxml.jackson.core:jackson-databind")
                  implementation("com.fasterxml.jackson.core:jackson-core")
              }
              """
          )
        );
    }

    // Kotlin `||` chain input, all share one target.
    @Test
    void upgradesOrChainWhenAllArtifactsShareOneBomVersion() {
        rewriteRun(
          buildGradleKts(
            """
              plugins { id("java") }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency {
                      if (requested.group == "com.fasterxml.jackson.core" && (requested.name == "jackson-databind" || requested.name == "jackson-core")) {
                          useVersion("2.12.5")
                      }
                  }
              }
              dependencies {
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("com.fasterxml.jackson.core:jackson-databind")
                  implementation("com.fasterxml.jackson.core:jackson-core")
              }
              """,
            """
              plugins { id("java") }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency {
                      if (requested.group == "com.fasterxml.jackson.core" && (requested.name == "jackson-databind" || requested.name == "jackson-core")) {
                          useVersion("2.17.2")
                      }
                  }
              }
              dependencies {
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("com.fasterxml.jackson.core:jackson-databind")
                  implementation("com.fasterxml.jackson.core:jackson-core")
              }
              """
          )
        );
    }

    @Test
    void skipsBranchWithBecauseClause() {
        rewriteRun(
          buildGradleKts(
            """
              plugins { id("java") }
              repositories { mavenCentral() }
              configurations.all {
                  resolutionStrategy.eachDependency {
                      if (requested.group == "com.fasterxml.jackson.core" && requested.name == "jackson-databind") {
                          useVersion("2.12.5")
                          because("some reason")
                      }
                  }
              }
              dependencies {
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("com.fasterxml.jackson.core:jackson-databind")
              }
              """
          )
        );
    }
}
