/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.android;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.gradle.Assertions.settingsGradle;
import static org.openrewrite.gradle.Assertions.settingsGradleKts;

class UpgradeAndroidGradlePluginVersionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpgradeAndroidGradlePluginVersion("8.5.0", null));
    }

    @DocumentExample
    @Test
    void groovyPluginsBlockIdVersion() {
        rewriteRun(
                buildGradle(
                        """
                        plugins {
                            id 'com.android.application' version '8.0.0'
                        }
                        """,
                        """
                        plugins {
                            id 'com.android.application' version '8.5.0'
                        }
                        """
                )
        );
    }

    @Test
    void kotlinPluginsBlockIdVersion() {
        rewriteRun(
                buildGradleKts(
                        """
                        plugins {
                            id("com.android.application") version "8.0.0"
                        }
                        """,
                        """
                        plugins {
                            id("com.android.application") version "8.5.0"
                        }
                        """
                )
        );
    }

    @Test
    void groovyLibraryPluginsBlock() {
        rewriteRun(
                buildGradle(
                        """
                        plugins {
                            id 'com.android.library' version '8.0.0'
                        }
                        """,
                        """
                        plugins {
                            id 'com.android.library' version '8.5.0'
                        }
                        """
                )
        );
    }

    @Test
    void pluginsBlockInSettingsGradle() {
        rewriteRun(
                settingsGradle(
                        """
                        pluginManagement {
                            plugins {
                                id 'com.android.application' version '8.0.0'
                            }
                        }
                        """,
                        """
                        pluginManagement {
                            plugins {
                                id 'com.android.application' version '8.5.0'
                            }
                        }
                        """
                )
        );
    }

    @Test
    void pluginsBlockInSettingsGradleKts() {
        rewriteRun(
                settingsGradleKts(
                        """
                        pluginManagement {
                            plugins {
                                id("com.android.library") version "8.0.0"
                            }
                        }
                        """,
                        """
                        pluginManagement {
                            plugins {
                                id("com.android.library") version "8.5.0"
                            }
                        }
                        """
                )
        );
    }

    @Test
    void doesNotDowngrade() {
        rewriteRun(
                buildGradle(
                        """
                        plugins {
                            id 'com.android.application' version '9.0.0'
                        }
                        """
                )
        );
    }

    @Test
    void ignoresNonAgpPlugin() {
        rewriteRun(
                buildGradle(
                        """
                        plugins {
                            id 'java-library' version '1.0.0'
                        }
                        """
                )
        );
    }

    @Test
    void buildscriptClasspathFormIsHandledByCompositionRegression() {
        // The classpath form is delegated to rewrite-gradle:UpgradeDependencyVersion via
        // getRecipeList(). This regression test asserts the composition is wired up by
        // checking that the recipe's recipe list contains UpgradeDependencyVersion with
        // the expected groupId/artifactId/newVersion.
        UpgradeAndroidGradlePluginVersion recipe = new UpgradeAndroidGradlePluginVersion("8.5.0", null);
        org.assertj.core.api.Assertions.assertThat(recipe.getRecipeList())
                .singleElement()
                .isInstanceOfSatisfying(org.openrewrite.gradle.UpgradeDependencyVersion.class, udv -> {
                    org.assertj.core.api.Assertions.assertThat(udv.getGroupId()).isEqualTo("com.android.tools.build");
                    org.assertj.core.api.Assertions.assertThat(udv.getArtifactId()).isEqualTo("gradle");
                    org.assertj.core.api.Assertions.assertThat(udv.getNewVersion()).isEqualTo("8.5.0");
                });
    }
}
