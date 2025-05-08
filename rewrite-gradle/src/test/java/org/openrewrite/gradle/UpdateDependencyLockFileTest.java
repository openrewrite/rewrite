package org.openrewrite.gradle;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.*;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.test.SourceSpecs.text;

class UpdateDependencyLockFileTest implements RewriteTest {

    @Test
    @DocumentExample
    void calculateGradleLock() {
        rewriteRun(spec ->
                spec.beforeRecipe(withToolingApi())
                        .recipe(new UpgradeDependencyVersion("some-group", "and-never-matching-artifact", null, null)),
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
                        .recipe(new UpgradeDependencyVersion("some-group", "and-never-matching-artifact", null, null)),
                settingsGradle(
                        """
                                rootProject.name = 'test'
                                include 'module1'
                                include 'module2'
                                """, spec -> spec.path("settings.gradle")
                ),
                buildGradle(
                        """
                                subprojects {
                                    apply plugin: 'java'
                                
                                    repositories {
                                        mavenCentral()
                                    }
                                }
                                """, spec -> spec.path("build.gradle")
                ),
                buildGradle(
                        """
                                dependencies {
                                    implementation 'com.fasterxml.jackson.core:jackson-core:2.15.3'
                                    implementation 'com.fasterxml.jackson.core:jackson-annotations:2.15.4'
                                    implementation 'com.fasterxml.jackson.core:jackson-databind:2.15.4'
                                }
                                """, spec -> spec.path("module1/build.gradle")
                ), buildGradle(
                        """
                                dependencies {
                                    implementation 'com.fasterxml.jackson.core:jackson-core:2.15.3'
                                    implementation 'com.fasterxml.jackson.core:jackson-annotations:2.15.3'
                                    implementation 'com.fasterxml.jackson.core:jackson-databind:2.15.3'
                                }
                                """, spec -> spec.path("module2/build.gradle")
                ),
                text(
                        """
                                # This is a Gradle generated file for dependency locking.
                                # Manual edits can break the build and are not advised.
                                # This file is expected to be part of source control.
                                empty=
                                """, spec -> spec.path("gradle.lockfile")
                ),
                text(
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
                                """, spec -> spec.path("module1/gradle.lockfile")
                ),
                text(
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
                                """, spec -> spec.path("module2/gradle.lockfile")
                )
        );
    }
}