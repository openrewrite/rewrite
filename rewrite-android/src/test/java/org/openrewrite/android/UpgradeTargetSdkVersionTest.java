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

class UpgradeTargetSdkVersionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpgradeTargetSdkVersion(34, null));
    }

    // ---- Groovy DSL ----

    @DocumentExample
    @Test
    void groovyLiteralIntAssignment() {
        rewriteRun(
                buildGradle(
                        """
                        android {
                            defaultConfig {
                                targetSdk = 33
                            }
                        }
                        """,
                        """
                        android {
                            defaultConfig {
                                targetSdk = 34
                            }
                        }
                        """
                )
        );
    }

    @Test
    void groovyLiteralIntMethod() {
        rewriteRun(
                buildGradle(
                        """
                        android {
                            defaultConfig {
                                targetSdk 33
                            }
                        }
                        """,
                        """
                        android {
                            defaultConfig {
                                targetSdk 34
                            }
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
                        android {
                            defaultConfig {
                                targetSdkVersion 'android-33'
                            }
                        }
                        """,
                        """
                        android {
                            defaultConfig {
                                targetSdkVersion 'android-34'
                            }
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
                        ext.targetSdkVersion = 33

                        android {
                            defaultConfig {
                                targetSdk = targetSdkVersion
                            }
                        }
                        """,
                        """
                        ext.targetSdkVersion = 34

                        android {
                            defaultConfig {
                                targetSdk = targetSdkVersion
                            }
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
                        android {
                            defaultConfig {
                                targetSdk = libs.versions.targetSdk.get().toInteger()
                            }
                        }
                        """
                ),
                toml(
                        """
                        [versions]
                        targetSdk = "33"
                        """,
                        """
                        [versions]
                        targetSdk = "34"
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
                        android {
                            defaultConfig {
                                targetSdk = providers.gradleProperty("targetSdk").get().toInteger()
                            }
                        }
                        """
                ),
                properties(
                        "targetSdk=33\n",
                        "targetSdk=34\n",
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
                        android {
                            defaultConfig {
                                targetSdk = 33
                            }
                        }
                        """,
                        """
                        android {
                            defaultConfig {
                                targetSdk = 34
                            }
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
                        android {
                            defaultConfig {
                                targetSdkVersion("android-33")
                            }
                        }
                        """,
                        """
                        android {
                            defaultConfig {
                                targetSdkVersion("android-34")
                            }
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
                        val targetSdkVersion by extra(33)

                        android {
                            defaultConfig {
                                targetSdk = targetSdkVersion
                            }
                        }
                        """,
                        """
                        val targetSdkVersion by extra(34)

                        android {
                            defaultConfig {
                                targetSdk = targetSdkVersion
                            }
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
                        android {
                            defaultConfig {
                                targetSdk = libs.versions.targetSdk.get().toInt()
                            }
                        }
                        """
                ),
                toml(
                        """
                        [versions]
                        targetSdk = "33"
                        """,
                        """
                        [versions]
                        targetSdk = "34"
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
                        android {
                            defaultConfig {
                                targetSdk = providers.gradleProperty("targetSdk").get().toInt()
                            }
                        }
                        """
                ),
                properties(
                        "targetSdk=33\n",
                        "targetSdk=34\n",
                        spec -> spec.path("gradle.properties")
                )
        );
    }

    // ---- Floor & no-op behaviour ----

    @Test
    void minSdkFloorRefusesOvershoot() {
        // floor=33, requested=34 -> refuses to bump
        rewriteRun(
                spec -> spec.recipe(new UpgradeTargetSdkVersion(34, 33)),
                buildGradle(
                        """
                        android {
                            defaultConfig {
                                targetSdk = 30
                            }
                        }
                        """
                )
        );
    }

    @Test
    void minSdkFloorAllowsBumpWithinFloor() {
        // floor=34, requested=34 -> within floor, bumps from 30 to 34
        rewriteRun(
                spec -> spec.recipe(new UpgradeTargetSdkVersion(34, 34)),
                buildGradle(
                        """
                        android {
                            defaultConfig {
                                targetSdk = 30
                            }
                        }
                        """,
                        """
                        android {
                            defaultConfig {
                                targetSdk = 34
                            }
                        }
                        """
                )
        );
    }

    @Test
    void alreadyAtTargetIsNoOp() {
        rewriteRun(
                buildGradle(
                        """
                        android {
                            defaultConfig {
                                targetSdk = 34
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
                        android {
                            defaultConfig {
                                targetSdk = 35
                            }
                        }
                        """
                )
        );
    }
}
