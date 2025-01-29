package org.openrewrite.gradle;

import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

public class GradleSetup extends RewriteGradleTest {
    private static Boolean alreadySetUp = false;

    // This is a one-time initialization of Gradle dependencies for the tests. It wouldn't be needed
    // if we didn't run (some of) the tests in offline mode.
    public static final void makeSureArtifactsAreDownloaded() {
        synchronized(GradleSetup.class) {
            if (alreadySetUp) {
                return;
            }
            RewriteTest fakeTest = new RewriteTest() {};
            fakeTest.rewriteRun(
              recipeSpec -> recipeSpec.beforeRecipe(withToolingApi()),
              buildGradle(
                """
                  repositories {
                   mavenCentral()
                  }
                """
            ));
            alreadySetUp = true;
        }
    }
}
