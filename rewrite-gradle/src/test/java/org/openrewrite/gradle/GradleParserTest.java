package org.openrewrite.gradle;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.settingsGradle;

public class GradleParserTest implements RewriteTest {

    @Test
    void buildAndSettingsGradle() {
        rewriteRun(
          buildGradle(
            """
              plugins { id 'java'}
              """
          ),
          settingsGradle(
            """
              plugins {
                  id 'com.gradle.enterprise' version '3.11.3'
              }
              
              gradleEnterprise {
                  server = 'https://enterprise-samples.gradle.com'
              
                  buildScan {
                      publishAlways()
                  }
              }
              """
          )
        );
    }
}
