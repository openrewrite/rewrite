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
import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.toml.Assertions.toml;

class UpgradeMinSdkVersionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpgradeMinSdkVersion(24));
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
                                minSdk = 21
                            }
                        }
                        """,
                        """
                        android {
                            defaultConfig {
                                minSdk = 24
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
                                minSdk 21
                            }
                        }
                        """,
                        """
                        android {
                            defaultConfig {
                                minSdk 24
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
                                minSdkVersion 'android-21'
                            }
                        }
                        """,
                        """
                        android {
                            defaultConfig {
                                minSdkVersion 'android-24'
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
                        ext.minSdkVersion = 21

                        android {
                            defaultConfig {
                                minSdk = minSdkVersion
                            }
                        }
                        """,
                        """
                        ext.minSdkVersion = 24

                        android {
                            defaultConfig {
                                minSdk = minSdkVersion
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
                                minSdk = libs.versions.minSdk.get().toInteger()
                            }
                        }
                        """
                ),
                toml(
                        """
                        [versions]
                        minSdk = "21"
                        """,
                        """
                        [versions]
                        minSdk = "24"
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
                                minSdk = providers.gradleProperty("minSdk").get().toInteger()
                            }
                        }
                        """
                ),
                properties(
                        "minSdk=21\n",
                        "minSdk=24\n",
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
                                minSdk = 21
                            }
                        }
                        """,
                        """
                        android {
                            defaultConfig {
                                minSdk = 24
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
                                minSdkVersion("android-21")
                            }
                        }
                        """,
                        """
                        android {
                            defaultConfig {
                                minSdkVersion("android-24")
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
                        val minSdkVersion by extra(21)

                        android {
                            defaultConfig {
                                minSdk = minSdkVersion
                            }
                        }
                        """,
                        """
                        val minSdkVersion by extra(24)

                        android {
                            defaultConfig {
                                minSdk = minSdkVersion
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
                                minSdk = libs.versions.minSdk.get().toInt()
                            }
                        }
                        """
                ),
                toml(
                        """
                        [versions]
                        minSdk = "21"
                        """,
                        """
                        [versions]
                        minSdk = "24"
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
                                minSdk = providers.gradleProperty("minSdk").get().toInt()
                            }
                        }
                        """
                ),
                properties(
                        "minSdk=21\n",
                        "minSdk=24\n",
                        spec -> spec.path("gradle.properties")
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
                                minSdk = 24
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
                                minSdk = 26
                            }
                        }
                        """
                )
        );
    }
}
