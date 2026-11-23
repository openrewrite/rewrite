/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
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
import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.toml.Assertions.toml;

class UpgradeCompileSdkVersionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpgradeCompileSdkVersion(35));
    }

    // ---- Groovy DSL ----

    @DocumentExample
    @Test
    void groovyLiteralInt() {
        rewriteRun(
                buildGradle(
                        """
                        plugins {
                            id 'com.android.application'
                        }

                        android {
                            compileSdk 34
                        }
                        """,
                        """
                        plugins {
                            id 'com.android.application'
                        }

                        android {
                            compileSdk 35
                        }
                        """
                )
        );
    }

    @Test
    void groovyLiteralIntAssignment() {
        rewriteRun(
                buildGradle(
                        """
                        plugins {
                            id 'com.android.application'
                        }

                        android {
                            compileSdk = 34
                        }
                        """,
                        """
                        plugins {
                            id 'com.android.application'
                        }

                        android {
                            compileSdk = 35
                        }
                        """
                )
        );
    }

    @Test
    void groovyStringForm() {
        rewriteRun(
                buildGradle(
                        """
                        plugins {
                            id 'com.android.application'
                        }

                        android {
                            compileSdkVersion 'android-34'
                        }
                        """,
                        """
                        plugins {
                            id 'com.android.application'
                        }

                        android {
                            compileSdkVersion 'android-35'
                        }
                        """
                )
        );
    }

    @Test
    void groovyExtraProperty() {
        rewriteRun(
                buildGradle(
                        """
                        plugins {
                            id 'com.android.application'
                        }

                        ext.compileSdkVersion = 34

                        android {
                            compileSdk = compileSdkVersion
                        }
                        """,
                        """
                        plugins {
                            id 'com.android.application'
                        }

                        ext.compileSdkVersion = 35

                        android {
                            compileSdk = compileSdkVersion
                        }
                        """
                )
        );
    }

    @Test
    void groovyVersionCatalog() {
        rewriteRun(
                buildGradle(
                        """
                        plugins {
                            id 'com.android.application'
                        }

                        android {
                            compileSdk = libs.versions.compileSdk.get().toInteger()
                        }
                        """
                ),
                toml(
                        """
                        [versions]
                        compileSdk = "34"
                        """,
                        """
                        [versions]
                        compileSdk = "35"
                        """,
                        spec -> spec.path("gradle/libs.versions.toml")
                )
        );
    }

    @Test
    void groovyGradleProperties() {
        rewriteRun(
                buildGradle(
                        """
                        plugins {
                            id 'com.android.application'
                        }

                        android {
                            compileSdk = providers.gradleProperty("compileSdk").get().toInteger()
                        }
                        """
                ),
                properties(
                        "compileSdk=34\n",
                        "compileSdk=35\n",
                        spec -> spec.path("gradle.properties")
                )
        );
    }

    // ---- Kotlin DSL ----

    @Test
    void kotlinLiteralInt() {
        rewriteRun(
                buildGradleKts(
                        """
                        plugins {
                            id("com.android.application")
                        }

                        android {
                            compileSdk = 34
                        }
                        """,
                        """
                        plugins {
                            id("com.android.application")
                        }

                        android {
                            compileSdk = 35
                        }
                        """
                )
        );
    }

    @Test
    void kotlinStringForm() {
        rewriteRun(
                buildGradleKts(
                        """
                        plugins {
                            id("com.android.application")
                        }

                        android {
                            compileSdkVersion("android-34")
                        }
                        """,
                        """
                        plugins {
                            id("com.android.application")
                        }

                        android {
                            compileSdkVersion("android-35")
                        }
                        """
                )
        );
    }

    @Test
    void kotlinExtraProperty() {
        rewriteRun(
                buildGradleKts(
                        """
                        plugins {
                            id("com.android.application")
                        }

                        val compileSdkVersion by extra(34)

                        android {
                            compileSdk = compileSdkVersion
                        }
                        """,
                        """
                        plugins {
                            id("com.android.application")
                        }

                        val compileSdkVersion by extra(35)

                        android {
                            compileSdk = compileSdkVersion
                        }
                        """
                )
        );
    }

    @Test
    void kotlinVersionCatalog() {
        rewriteRun(
                buildGradleKts(
                        """
                        plugins {
                            id("com.android.application")
                        }

                        android {
                            compileSdk = libs.versions.compileSdk.get().toInt()
                        }
                        """
                ),
                toml(
                        """
                        [versions]
                        compileSdk = "34"
                        """,
                        """
                        [versions]
                        compileSdk = "35"
                        """,
                        spec -> spec.path("gradle/libs.versions.toml")
                )
        );
    }

    @Test
    void kotlinGradleProperties() {
        rewriteRun(
                buildGradleKts(
                        """
                        plugins {
                            id("com.android.application")
                        }

                        android {
                            compileSdk = providers.gradleProperty("compileSdk").get().toInt()
                        }
                        """
                ),
                properties(
                        "compileSdk=34\n",
                        "compileSdk=35\n",
                        spec -> spec.path("gradle.properties")
                )
        );
    }

    // ---- Extras ----

    @Test
    void alreadyAtTargetIsNoOp() {
        rewriteRun(
                buildGradle(
                        """
                        plugins {
                            id 'com.android.application'
                        }

                        android {
                            compileSdk = 35
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
                            id 'com.android.application'
                        }

                        android {
                            compileSdk = 36
                        }
                        """
                )
        );
    }
}
