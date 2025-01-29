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
