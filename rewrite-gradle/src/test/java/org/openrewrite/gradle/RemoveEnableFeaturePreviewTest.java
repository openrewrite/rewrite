package org.openrewrite.gradle;

import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

public class RemoveEnableFeaturePreviewTest implements RewriteTest {

    @Test
    void testRemoveEnableFeaturePreviewMethodRecipe_singleQuotes() {
        //language=gradle
        rewriteRun(
          spec -> spec.recipe(new RemoveEnableFeaturePreview("ONE_LOCKFILE_PER_PROJECT")),
          Assertions.settingsGradle(
            """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                    }
                }
                              
                rootProject.name = 'merge-service'
                enableFeaturePreview('ONE_LOCKFILE_PER_PROJECT')
              """,
            """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                    }
                }
                              
                rootProject.name = 'merge-service'
              """,
            spec -> spec.path(Paths.get("settings.gradle"))
          )
        );
    }

    @Test
    void testRemoveEnableFeaturePreviewMethodRecipe_doubleQuotes() {
        //language=gradle
        rewriteRun(
          spec -> spec.recipe(new RemoveEnableFeaturePreview("ONE_LOCKFILE_PER_PROJECT")),
          Assertions.settingsGradle(
            """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                    }
                }
                              
                rootProject.name = 'merge-service'
                enableFeaturePreview("ONE_LOCKFILE_PER_PROJECT")
                              
                include 'service'
              """,
            """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                    }
                }
                              
                rootProject.name = 'merge-service'
                              
                include 'service'
              """,
            spec -> spec.path(Paths.get("settings.gradle"))
          )
        );
    }

    @Test
    void testRemoveEnableFeaturePreviewMethodRecipe_noChange() {
        //language=gradle
        rewriteRun(
          spec -> spec.recipe(new RemoveEnableFeaturePreview("ONE_LOCKFILE_PER_PROJECT")),
          Assertions.settingsGradle(
            """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                    }
                }
                
                enableFeaturePreview("DIFFERENT_FEATURE")
                              
                rootProject.name = 'merge-service'
                              
                include 'service'
              """,
            spec -> spec.path(Paths.get("settings.gradle"))
          )
        );
    }

    @Test
    void testRemoveEnableFeaturePreviewMethodRecipe_noChangeNullArgument() {
        //language=gradle
        rewriteRun(
          spec -> spec.recipe(new RemoveEnableFeaturePreview("ONE_LOCKFILE_PER_PROJECT")),
          Assertions.settingsGradle(
            """
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                    }
                }
                
                enableFeaturePreview()
                              
                rootProject.name = 'merge-service'
                              
                include 'service'
              """,
            spec -> spec.path(Paths.get("settings.gradle"))
          )
        );
    }

}
