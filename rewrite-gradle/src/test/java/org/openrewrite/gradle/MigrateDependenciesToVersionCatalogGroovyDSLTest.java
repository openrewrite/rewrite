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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.toml.Assertions.toml;

    class MigrateDependenciesToVersionCatalogGroovyDSLTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateDependenciesToVersionCatalog());
    }

    @Test
    @DocumentExample
    void migrateStringNotationDependencies() {
        rewriteRun(
            buildGradle(
                """
                    plugins {
                        id 'java'
                    }

                    dependencies {
                        implementation 'org.springframework:spring-core:5.3.0'
                        testImplementation 'junit:junit:4.13.2'
                        runtimeOnly 'com.h2database:h2:1.4.200'
                    }
                    """,
                """
                    plugins {
                        id 'java'
                    }

                    dependencies {
                        implementation libs.springCore
                        testImplementation libs.junit
                        runtimeOnly libs.h2
                    }
                    """
            ),
            toml(
                doesNotExist(),
                """
                    [versions]
                    spring-core = "5.3.0"
                    junit = "4.13.2"
                    h2 = "1.4.200"

                    [libraries]
                    spring-core = { group = "org.springframework", name = "spring-core", version.ref = "spring-core" }
                    junit = { group = "junit", name = "junit", version.ref = "junit" }
                    h2 = { group = "com.h2database", name = "h2", version.ref = "h2" }
                    """,
                spec -> spec.path("gradle/libs.versions.toml")
            )
        );
    }

    @Test
    void migrateMapNotationDependencies() {
        rewriteRun(
            buildGradle(
                """
                    plugins {
                        id 'java'
                    }

                    dependencies {
                        implementation group: 'org.apache.commons', name: 'commons-lang3', version: '3.12.0'
                        testImplementation group: 'org.mockito', name: 'mockito-core', version: '4.6.1'
                    }
                    """,
                """
                    plugins {
                        id 'java'
                    }

                    dependencies {
                        implementation libs.commonsLang3
                        testImplementation libs.mockitoCore
                    }
                    """
            ),
            toml(
                doesNotExist(),
                """
                    [versions]
                    commons-lang3 = "3.12.0"
                    mockito-core = "4.6.1"

                    [libraries]
                    commons-lang3 = { group = "org.apache.commons", name = "commons-lang3", version.ref = "commons-lang3" }
                    mockito-core = { group = "org.mockito", name = "mockito-core", version.ref = "mockito-core" }
                    """,
                spec -> spec.path("gradle/libs.versions.toml")
            )
        );
    }

    @Test
    void migrateMultiProjectDependencies() {
        rewriteRun(
            buildGradle(
                """
                    subprojects {
                        apply plugin: 'java'

                        dependencies {
                            implementation 'org.slf4j:slf4j-api:1.7.36'
                            implementation 'com.fasterxml.jackson.core:jackson-databind:2.13.3'
                        }
                    }
                    """,
                """
                    subprojects {
                        apply plugin: 'java'

                        dependencies {
                            implementation libs.slf4jApi
                            implementation libs.jacksonDatabind
                        }
                    }
                    """
            ),
            toml(
                doesNotExist(),
                """
                    [versions]
                    slf4j-api = "1.7.36"
                    jackson-databind = "2.13.3"

                    [libraries]
                    slf4j-api = { group = "org.slf4j", name = "slf4j-api", version.ref = "slf4j-api" }
                    jackson-databind = { group = "com.fasterxml.jackson.core", name = "jackson-databind", version.ref = "jackson-databind" }
                    """,
                spec -> spec.path("gradle/libs.versions.toml")
            )
        );
    }

    @Test
    void doNotMigrateProjectDependencies() {
        rewriteRun(
            buildGradle(
                """
                    plugins {
                        id 'java'
                    }

                    dependencies {
                        implementation project(':core')
                        implementation 'org.springframework:spring-core:5.3.0'
                        testImplementation project(':test-utils')
                    }
                    """,
                """
                    plugins {
                        id 'java'
                    }

                    dependencies {
                        implementation project(':core')
                        implementation libs.springCore
                        testImplementation project(':test-utils')
                    }
                    """
            ),
            toml(
                doesNotExist(),
                """
                    [versions]
                    spring-core = "5.3.0"

                    [libraries]
                    spring-core = { group = "org.springframework", name = "spring-core", version.ref = "spring-core" }
                    """,
                spec -> spec.path("gradle/libs.versions.toml")
            )
        );
    }

    @Test
    void handleMixedNotations() {
        rewriteRun(
            buildGradle(
                """
                    plugins {
                        id 'java'
                    }

                    dependencies {
                        implementation 'org.springframework:spring-core:5.3.0'
                        implementation group: 'org.springframework', name: 'spring-context', version: '5.3.0'
                        implementation('org.springframework:spring-web:5.3.0') {
                            exclude group: 'commons-logging'
                        }
                    }
                    """,
                """
                    plugins {
                        id 'java'
                    }

                    dependencies {
                        implementation libs.springCore
                        implementation libs.springContext
                        implementation(libs.springWeb) {
                            exclude group: 'commons-logging'
                        }
                    }
                    """
            ),
            toml(
                doesNotExist(),
                """
                    [versions]
                    spring-core = "5.3.0"
                    spring-context = "5.3.0"
                    spring-web = "5.3.0"

                    [libraries]
                    spring-core = { group = "org.springframework", name = "spring-core", version.ref = "spring-core" }
                    spring-context = { group = "org.springframework", name = "spring-context", version.ref = "spring-context" }
                    spring-web = { group = "org.springframework", name = "spring-web", version.ref = "spring-web" }
                    """,
                spec -> spec.path("gradle/libs.versions.toml")
            )
        );
    }

    /**
     * When a version catalog already exists, the recipe will not modify it.
     * This is the current behavior - merging is not yet implemented.
     */
    @Test
    void whenVersionCatalogAlreadyExists() {
        rewriteRun(
            toml(
                """
                    [versions]
                    guava-version = "31.0-jre"

                    [libraries]
                    guava = { group = "com.google.guava", name = "guava", version.ref = "guava-version" }
                    """,
                // The existing catalog remains unchanged
                spec -> spec.path("gradle/libs.versions.toml")
            ),
            buildGradle(
                """
                    plugins {
                        id 'java'
                    }

                    dependencies {
                        implementation 'org.springframework:spring-core:5.3.0'
                        testImplementation 'junit:junit:4.13.2'
                    }
                    """,
                """
                    plugins {
                        id 'java'
                    }

                    dependencies {
                        implementation libs.springCore
                        testImplementation libs.junit
                    }
                    """
            )
        );
    }
}