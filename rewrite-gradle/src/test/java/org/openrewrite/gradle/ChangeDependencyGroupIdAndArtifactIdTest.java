package org.openrewrite.gradle;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.java;

public class ChangeDependencyGroupIdAndArtifactIdTest implements RewriteTest {

    @Test
    void changeDependencyGroupIdAndArtifactId() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyGroupIdAndArtifactId(
            "javax.activation",
            "javax.activation-api",
            "jakarta.activation",
            "jakarta.activation-api",
            "1.2.2",
            null,
            null
          )).beforeRecipe(withToolingApi()),
          buildGradle("""
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation 'javax.activation:javax.activation-api:1.2.0'
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
                  implementation 'jakarta.activation:jakarta.activation-api:1.2.2'
              }
              """)
        );
    }
}
