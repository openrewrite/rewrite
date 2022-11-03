package org.openrewrite.gradle;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.settingsGradle;

public class UpgradePluginVersion2Test implements RewriteTest {

    @Test
    void upgradeGradleSettingsPlugin() {
        rewriteRun(
          spec -> spec.recipe(new UpgradePluginVersion("com.gradle.enterprise", "3.x", null)),
          settingsGradle(
            """
              plugins {
                  id 'com.gradle.enterprise' version '3.10.0'
              }
              """,
            """
              plugins {
                  id 'com.gradle.enterprise' version '3.11.3'
              }
              """
          )
        );
    }
}
