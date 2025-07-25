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

import java.util.Collections;

import static org.openrewrite.gradle.Assertions.*;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

class LockDependencyVersionsTest implements RewriteTest {

    private static String EMPTY_LOCK_FILE = """
              # This is a Gradle generated file for dependency locking.
              # Manual edits can break the build and are not advised.
              # This file is expected to be part of source control.
              empty=
              """;
    private static String JACKSON_LOCKFILE = """
              # This is a Gradle generated file for dependency locking.
              # Manual edits can break the build and are not advised.
              # This file is expected to be part of source control.
              com.fasterxml.jackson.core:jackson-annotations:1.0=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              com.fasterxml.jackson.core:jackson-core:1.0=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              com.fasterxml.jackson.core:jackson-databind:1.0=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              com.fasterxml.jackson:jackson-bom:1.0=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              empty=annotationProcessor,testAnnotationProcessor
              """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(RewriteTest.toRecipe(() -> new UpdateDependencyLock(Collections.emptySet())));
    }

    @DocumentExample
    @Test
    void updateGradleLock() {
        rewriteRun(spec ->
            spec.beforeRecipe(withToolingApi()),
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
          lockfile(
            JACKSON_LOCKFILE,
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
    void preserveNewLineEOFIfPresent() {
        rewriteRun(spec ->
            spec.beforeRecipe(withToolingApi()),
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation 'org.apache.tomcat.embed:tomcat-embed-core:10.0.27'
                  implementation 'org.apache.tomcat:tomcat-annotations-api:10.0.0-M1'
              }
              """
          ),
          lockfile(
            """
              # This is a Gradle generated file for dependency locking.
              # Manual edits can break the build and are not advised.
              # This file is expected to be part of source control.
              org.apache.tomcat.embed:tomcat-embed-core:10.0.27=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              org.apache.tomcat:tomcat-annotations-api:10.0.27=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              empty=annotationProcessor,testAnnotationProcessor
              
              """
          )
        );
    }

    @Test
    void onlyUpdateExistingConfigurations() {
        rewriteRun(spec ->
            spec.beforeRecipe(withToolingApi()),
          buildGradleKts(
            """
              plugins {
                  java
              }
              repositories {
                  mavenCentral()
              }
              
              val asciidoclet by configurations.creating
              
              dependencies {
                  implementation("org.apache.tomcat.embed:tomcat-embed-core:10.0.27")
                  implementation("org.apache.tomcat:tomcat-annotations-api:10.0.27")
                  asciidoclet("org.asciidoctor:asciidoclet:1.+")
              }
              """
          ),
          lockfile(
            """
              # This is a Gradle generated file for dependency locking.
              # Manual edits can break the build and are not advised.
              # This file is expected to be part of source control.
              org.apache.tomcat.embed:tomcat-embed-core:10.0.0-M1=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              org.apache.tomcat:tomcat-annotations-api:10.0.0-M1=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              org.jacoco:org.jacoco.agent:0.8.12=jacocoAgent,jacocoAnt
              org.jacoco:org.jacoco.ant:0.8.12=jacocoAnt
              empty=annotationProcessor,testAnnotationProcessor
              """,
            """
              # This is a Gradle generated file for dependency locking.
              # Manual edits can break the build and are not advised.
              # This file is expected to be part of source control.
              org.apache.tomcat.embed:tomcat-embed-core:10.0.27=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              org.apache.tomcat:tomcat-annotations-api:10.0.27=compileClasspath,runtimeClasspath,testCompileClasspath,testRuntimeClasspath
              org.jacoco:org.jacoco.agent:0.8.12=jacocoAgent,jacocoAnt
              org.jacoco:org.jacoco.ant:0.8.12=jacocoAnt
              empty=annotationProcessor,testAnnotationProcessor
              """
          )
        );
    }

    @Test
    void multimodule() {
        rewriteRun(spec ->
            spec.beforeRecipe(withToolingApi()),
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
          lockfile(
            EMPTY_LOCK_FILE
          ),
          lockfile(
            JACKSON_LOCKFILE,
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
          lockfile(
            JACKSON_LOCKFILE,
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
            spec.beforeRecipe(withToolingApi()),
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
          lockfile(
            EMPTY_LOCK_FILE
          ),
          lockfile(
            JACKSON_LOCKFILE,
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
          lockfile(
            JACKSON_LOCKFILE,
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
