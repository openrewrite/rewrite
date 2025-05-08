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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.*;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

class LockDependencyVersionsTest implements RewriteTest {

    @Test
    @DocumentExample
    void createGradleLockFile() {
        rewriteRun(spec ->
            spec.beforeRecipe(withToolingApi())
              .recipe(new LockDependencyVersions()),
          buildGradle(
            """
                    plugins {
                        id 'java'
                    }
                    repositories {
                        mavenCentral()
                    }
                    dependencies {
                        implementation 'com.fasterxml.jackson.core:jackson-core:2.15.3'
                        implementation 'com.fasterxml.jackson.core:jackson-annotations:2.15.4'
                        implementation 'com.fasterxml.jackson.core:jackson-databind:2.15.4'
                    }
                    """
          ),
          lockFile(
            null,
            """
                    # This is a Gradle generated file for dependency locking.
                    # Manual edits can break the build and are not advised.
                    # This file is expected to be part of source control.
                    com.fasterxml.jackson.core:jackson-annotations:2.15.4=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                    com.fasterxml.jackson.core:jackson-core:2.15.4=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                    com.fasterxml.jackson.core:jackson-databind:2.15.4=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                    com.fasterxml.jackson:jackson-bom:2.15.4=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                    empty=annotationProcessor,testAnnotationProcessor
                    """
          )
        );
    }

    @Test
    void updateGradleLock() {
        rewriteRun(spec ->
                spec.beforeRecipe(withToolingApi())
                        .recipe(new LockDependencyVersions()),
                buildGradle(
                        """
                                plugins {
                                    id 'java'
                                }
                                repositories {
                                    mavenCentral()
                                }
                                dependencies {
                                    implementation 'com.fasterxml.jackson.core:jackson-core:2.15.3'
                                    implementation 'com.fasterxml.jackson.core:jackson-annotations:2.15.4'
                                    implementation 'com.fasterxml.jackson.core:jackson-databind:2.15.4'
                                }
                                """
                ),
                lockFile(
                        """
                                # This is a Gradle generated file for dependency locking.
                                # Manual edits can break the build and are not advised.
                                # This file is expected to be part of source control.
                                com.fasterxml.jackson.core:jackson-annotations:2.15.3=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                                com.fasterxml.jackson.core:jackson-core:2.15.3=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                                com.fasterxml.jackson.core:jackson-databind:2.15.3=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                                com.fasterxml.jackson:jackson-bom:2.15.3=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                                empty=annotationProcessor,testAnnotationProcessor
                                
                                """,
                        """
                                # This is a Gradle generated file for dependency locking.
                                # Manual edits can break the build and are not advised.
                                # This file is expected to be part of source control.
                                com.fasterxml.jackson.core:jackson-annotations:2.15.4=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                                com.fasterxml.jackson.core:jackson-core:2.15.4=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                                com.fasterxml.jackson.core:jackson-databind:2.15.4=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                                com.fasterxml.jackson:jackson-bom:2.15.4=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                                empty=annotationProcessor,testAnnotationProcessor
                                """
                )
        );
    }

    @Test
    void multimodule() {
        rewriteRun(spec ->
                spec.beforeRecipe(withToolingApi())
                        .recipe(new LockDependencyVersions()),
                settingsGradle(
                        """
                                rootProject.name = 'test'
                                include 'module1'
                                include 'module2'
                                """
                ),
                buildGradle(
                        """
                                subprojects {
                                    apply plugin: 'java'
                                
                                    repositories {
                                        mavenCentral()
                                    }
                                }
                                """
                ),
                buildGradle(
                        """
                                dependencies {
                                    implementation 'com.fasterxml.jackson.core:jackson-core:2.15.3'
                                    implementation 'com.fasterxml.jackson.core:jackson-annotations:2.15.4'
                                    implementation 'com.fasterxml.jackson.core:jackson-databind:2.15.4'
                                }
                                """,
                        spec -> spec.path("module1/build.gradle")
                ), buildGradle(
                        """
                                dependencies {
                                    implementation 'com.fasterxml.jackson.core:jackson-core:2.15.3'
                                    implementation 'com.fasterxml.jackson.core:jackson-annotations:2.15.3'
                                    implementation 'com.fasterxml.jackson.core:jackson-databind:2.15.3'
                                }
                                """,
                        spec -> spec.path("module2/build.gradle")
                ),
                lockFile(
                        """
                                # This is a Gradle generated file for dependency locking.
                                # Manual edits can break the build and are not advised.
                                # This file is expected to be part of source control.
                                empty=
                                """
                ),
                lockFile(
                        """
                                # This is a Gradle generated file for dependency locking.
                                # Manual edits can break the build and are not advised.
                                # This file is expected to be part of source control.
                                com.fasterxml.jackson.core:jackson-annotations:2.15.3=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                                com.fasterxml.jackson.core:jackson-core:2.15.3=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                                com.fasterxml.jackson.core:jackson-databind:2.15.3=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                                com.fasterxml.jackson:jackson-bom:2.15.3=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                                empty=annotationProcessor,testAnnotationProcessor
                                
                                """,
                        """
                                # This is a Gradle generated file for dependency locking.
                                # Manual edits can break the build and are not advised.
                                # This file is expected to be part of source control.
                                com.fasterxml.jackson.core:jackson-annotations:2.15.4=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                                com.fasterxml.jackson.core:jackson-core:2.15.4=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                                com.fasterxml.jackson.core:jackson-databind:2.15.4=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                                com.fasterxml.jackson:jackson-bom:2.15.4=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                                empty=annotationProcessor,testAnnotationProcessor
                                """,
                        spec -> spec.path("module1/gradle.lockfile")
                ),
                lockFile(
                        """
                                # This is a Gradle generated file for dependency locking.
                                # Manual edits can break the build and are not advised.
                                # This file is expected to be part of source control.
                                com.fasterxml.jackson.core:jackson-annotations:2.15.2=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                                com.fasterxml.jackson.core:jackson-core:2.15.2=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                                com.fasterxml.jackson.core:jackson-databind:2.15.2=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                                com.fasterxml.jackson:jackson-bom:2.15.2=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                                empty=annotationProcessor,testAnnotationProcessor
                                
                                """,
                        """
                                # This is a Gradle generated file for dependency locking.
                                # Manual edits can break the build and are not advised.
                                # This file is expected to be part of source control.
                                com.fasterxml.jackson.core:jackson-annotations:2.15.3=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                                com.fasterxml.jackson.core:jackson-core:2.15.3=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                                com.fasterxml.jackson.core:jackson-databind:2.15.3=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                                com.fasterxml.jackson:jackson-bom:2.15.3=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                                empty=annotationProcessor,testAnnotationProcessor
                                """,
                        spec -> spec.path("module2/gradle.lockfile")
                )
        );
    }

    @Test
    void createMultiModuleLockFile() {
        rewriteRun(spec ->
            spec.beforeRecipe(withToolingApi())
              .recipe(new LockDependencyVersions()),
          settingsGradle(
            """
                    rootProject.name = 'test'
                    include 'module1'
                    include 'module2'
                    """
          ),
          buildGradle(
            """
                    subprojects {
                        apply plugin: 'java'
                    
                        repositories {
                            mavenCentral()
                        }
                    }
                    """
          ),
          buildGradle(
            """
                    dependencies {
                        implementation 'com.fasterxml.jackson.core:jackson-core:2.15.3'
                        implementation 'com.fasterxml.jackson.core:jackson-annotations:2.15.4'
                        implementation 'com.fasterxml.jackson.core:jackson-databind:2.15.4'
                    }
                    """,
            spec -> spec.path("module1/build.gradle")
          ), buildGradle(
            """
                    dependencies {
                        implementation 'com.fasterxml.jackson.core:jackson-core:2.15.3'
                        implementation 'com.fasterxml.jackson.core:jackson-annotations:2.15.3'
                        implementation 'com.fasterxml.jackson.core:jackson-databind:2.15.3'
                    }
                    """,
            spec -> spec.path("module2/build.gradle")
          ),
          lockFile(
            null,
            """
                    # This is a Gradle generated file for dependency locking.
                    # Manual edits can break the build and are not advised.
                    # This file is expected to be part of source control.
                    empty=
                    """
          ),
          lockFile(
            null,
            """
                    # This is a Gradle generated file for dependency locking.
                    # Manual edits can break the build and are not advised.
                    # This file is expected to be part of source control.
                    com.fasterxml.jackson.core:jackson-annotations:2.15.4=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                    com.fasterxml.jackson.core:jackson-core:2.15.4=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                    com.fasterxml.jackson.core:jackson-databind:2.15.4=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                    com.fasterxml.jackson:jackson-bom:2.15.4=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                    empty=annotationProcessor,testAnnotationProcessor
                    """,
            spec -> spec.path("module1/gradle.lockfile")
          ),
          lockFile(
            null,
            """
                    # This is a Gradle generated file for dependency locking.
                    # Manual edits can break the build and are not advised.
                    # This file is expected to be part of source control.
                    com.fasterxml.jackson.core:jackson-annotations:2.15.3=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                    com.fasterxml.jackson.core:jackson-core:2.15.3=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                    com.fasterxml.jackson.core:jackson-databind:2.15.3=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                    com.fasterxml.jackson:jackson-bom:2.15.3=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
                    empty=annotationProcessor,testAnnotationProcessor
                    """,
            spec -> spec.path("module2/gradle.lockfile")
          )
        );
    }
}
