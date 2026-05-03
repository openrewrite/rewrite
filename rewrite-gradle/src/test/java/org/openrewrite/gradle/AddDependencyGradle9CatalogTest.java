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
package org.openrewrite.gradle;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.toml.Assertions.toml;

@Issue("https://github.com/openrewrite/rewrite/issues/7548")
class AddDependencyGradle9CatalogTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        // version=null avoids triggering MavenPomDownloader; the bug was about the
        // recipe not making any change at all when libs.versions.toml is present on
        // Gradle 9, so the version itself is incidental.
        spec.recipe(new AddDependency(
                "org.projectlombok", "lombok", null,
                null, "compileOnly", null,
                null, null, null, Boolean.TRUE
        ));
    }

    @Test
    void gradle9_catalogPresent_addDependencyShouldEditBuildKts() {
        rewriteRun(
                spec -> spec.beforeRecipe(withToolingApi("9.0.0")),
                toml(
                        """
                        [versions]
                        lombok = "1.18.30"

                        [libraries]
                        lombok = { module = "org.projectlombok:lombok", version.ref = "lombok" }
                        """,
                        spec -> spec.path("gradle/libs.versions.toml")
                ),
                buildGradleKts(
                        """
                        plugins {
                            java
                        }
                        repositories {
                            mavenCentral()
                        }
                        """,
                        """
                        plugins {
                            java
                        }
                        repositories {
                            mavenCentral()
                        }

                        dependencies {
                            compileOnly("org.projectlombok:lombok")
                        }
                        """
                )
        );
    }

    @Test
    void gradle8_catalogPresent_addDependencyEditsBuildKts() {
        rewriteRun(
                spec -> spec.beforeRecipe(withToolingApi("8.14.3")),
                toml(
                        """
                        [versions]
                        lombok = "1.18.30"

                        [libraries]
                        lombok = { module = "org.projectlombok:lombok", version.ref = "lombok" }
                        """,
                        spec -> spec.path("gradle/libs.versions.toml")
                ),
                buildGradleKts(
                        """
                        plugins {
                            java
                        }
                        repositories {
                            mavenCentral()
                        }
                        """,
                        """
                        plugins {
                            java
                        }
                        repositories {
                            mavenCentral()
                        }

                        dependencies {
                            compileOnly("org.projectlombok:lombok")
                        }
                        """
                )
        );
    }
}
