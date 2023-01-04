package org.openrewrite.gradle.plugins;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;

class AddBuildPluginTest implements RewriteTest {
    @Test
    void addPluginWithoutVersionToNewBlock() {
        rewriteRun(
          spec -> spec.recipe(new AddBuildPlugin("java-library", null, null)),
          buildGradle(
            "",
            """
              plugins {
                  id 'java-library'
              }
              """
          )
        );
    }

    @Test
    void addPluginWithoutVersionToExistingBlock() {
        rewriteRun(
          spec -> spec.recipe(new AddBuildPlugin("java-library", null, null)),
          buildGradle(
            """
              plugins {
                  id "java"
              }
              """,
            """
              plugins {
                  id "java"
                  id "java-library"
              }
              """
          )
        );
    }

    @Test
    void addPluginWithVersionToNewBlock() {
        rewriteRun(
          spec -> spec.recipe(new AddBuildPlugin("com.jfrog.bintray", "1.0", null)),
          buildGradle(
            "",
            """
              plugins {
                  id 'com.jfrog.bintray' version '1.0'
              }
              """
          )
        );
    }

    @Test
    void addPluginWithVersionToExistingBlock() {
        rewriteRun(
          spec -> spec.recipe(new AddBuildPlugin("com.jfrog.bintray", "1.0", null)),
          buildGradle(
            """
              plugins {
                  id "java"
              }
              """,
            """
              plugins {
                  id "java"
                  id 'com.jfrog.bintray' version '1.0'
              }
              """
          )
        );
    }
}
