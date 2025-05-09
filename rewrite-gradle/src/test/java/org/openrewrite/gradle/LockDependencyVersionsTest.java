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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;

import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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

    //TODO for now I have this recipe here to test the calculation of the lockfile is correct.
    //     I will later move it to production recipes when it also generates the  necessary build.gradle and build.gradle.kts changes.
    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class LockDependencyVersions extends ScanningRecipe<LockDependencyVersions.Accumulator> {
        private static final String[] EMPTY = new String[0];

        @Override
        public String getDisplayName() {
            return "Create or update a gradle.lockfile";
        }

        @Override
        public String getDescription() {
            //language=markdown
            return "Using dynamic dependency versions (e.g., 1.+ or [1.0,2.0)) can cause builds to break unexpectedly because the exact version of a dependency that gets resolved can change over time.\n" +
              "To ensure reproducible builds, it’s necessary to lock versions of dependencies and their transitive dependencies. " +
              "This guarantees that a build with the same inputs will always resolve to the same module versions, a process known as dependency locking.\n" +
              "This recipe creates a `gradle.lockfile` in the root of every gradle module in the project.\n" +
              "The `gradle.lockfile` contains the resolved versions of all dependencies and their transitive dependencies, ensuring that the same versions are used across different builds.\n" +
              "The dependencies used for locking are the ones that are resolved during the BUILD time of your LST. \n" +
              "If you want to update the lockfile after impacting dependencies using a recipe, either you first need to build a new LST or use the `UpdateDependencyLockFileVisitor`.";
        }

        @Value
        static class Accumulator {
            Set<String> lockfiles = new HashSet<>();
            Set<GradleProject> gradleProjects = new HashSet<>();
        }

        @Override
        public Accumulator getInitialValue(ExecutionContext ctx) {
            return new Accumulator();
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
            return new TreeVisitor<Tree, ExecutionContext>() {
                @Override
                public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                    return (sourceFile instanceof G.CompilationUnit && sourceFile.getSourcePath().toString().endsWith(".gradle")) ||
                      (sourceFile instanceof K.CompilationUnit && sourceFile.getSourcePath().toString().endsWith(".gradle.kts")) ||
                      (sourceFile instanceof PlainText && sourceFile.getSourcePath().endsWith("gradle.lockfile"));
                }

                @Override
                public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                    if (tree instanceof PlainText && ((PlainText) tree).getSourcePath().endsWith("gradle.lockfile")) {
                        acc.getLockfiles().add(((PlainText) tree).getSourcePath().toString());
                    } else if (tree instanceof JavaSourceFile) {
                        tree.getMarkers().findFirst(GradleProject.class).ifPresent(acc.gradleProjects::add);
                    }
                    return super.visit(tree, ctx);
                }
            };
        }

        @Override
        public Collection<? extends SourceFile> generate(Accumulator acc, ExecutionContext ctx) {
            return acc.gradleProjects.stream()
              .flatMap(project -> {
                  String path = project.getPath();
                  if (path.startsWith(":")) {
                      path = path.substring(1);
                  }
                  if (!path.isEmpty()) {
                      path += "/";
                  }
                  path = path.replaceAll(":", "/") + "gradle.lockfile";
                  String finalPath = path;
                  return PlainTextParser.builder().build()
                    .parse("# This is a Gradle generated file for dependency locking.\n" +
                      "# Manual edits can break the build and are not advised.\n" +
                      "# This file is expected to be part of source control.\n" +
                      "empty=")
                    .map(brandNewFile -> (SourceFile) brandNewFile
                      .withSourcePath(Paths.get(finalPath))
                      .withMarkers(brandNewFile.getMarkers().add(project))
                    )
                    .filter(brandNewFile -> !acc.lockfiles.contains(brandNewFile.getSourcePath().toString()));
              }).collect(Collectors.toList());
        }

        @Override
        public UpdateDependencyLock getVisitor(Accumulator acc) {
            return new UpdateDependencyLock();
        }
    }
}
