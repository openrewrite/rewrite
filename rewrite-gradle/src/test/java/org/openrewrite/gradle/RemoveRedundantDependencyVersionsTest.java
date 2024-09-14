package org.openrewrite.gradle;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

class RemoveRedundantDependencyVersionsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
          .recipe(new RemoveRedundantDependencyVersions(null, null, null, null));
    }

    @DocumentExample
    @Test
    void platform() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java"
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("org.apache.commons:commons-lang3:3.14.0")
                  implementation(group: "org.apache.commons", name: "commons-lang3", version: "3.14.0")
              }
              """,
            """
              plugins {
                  id "java"
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("org.apache.commons:commons-lang3")
                  implementation(group: "org.apache.commons", name: "commons-lang3")
              }
              """
          )
        );
    }
}