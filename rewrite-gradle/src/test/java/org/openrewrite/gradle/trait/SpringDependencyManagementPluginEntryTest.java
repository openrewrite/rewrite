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
package org.openrewrite.gradle.trait;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.semver.DependencyMatcher;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.Objects;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

class SpringDependencyManagementPluginEntryTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        String oldGroupId = "javax.validation";
        String oldArtifactId = "validation-api";
        DependencyMatcher matcher = Objects.requireNonNull(DependencyMatcher.build(oldGroupId + ":" + oldArtifactId).getValue());
        spec
          .beforeRecipe(withToolingApi())
          .recipe(RewriteTest.toRecipe(() ->
            new SpringDependencyManagementPluginEntry.Matcher().groupId(oldGroupId).artifactId(oldArtifactId).asVisitor((dep, ctx) -> dep.withGroupArtifactVersion(
              matcher, "jakarta.validation", "jakarta.validation-api", "3.0.x", null, null, ctx).getTree()
            )));
    }

    @DocumentExample
    @Test
    void dependencyPluginManagedDependencies() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'org.springframework.boot' version '3.5.0'
                  id 'io.spring.dependency-management' version '1.1.7'
              }

              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  dependencies {
                      dependency 'javax.validation:validation-api:2.0.1.Final'
                      dependency group: 'javax.validation', name: 'validation-api', version: '2.0.1.Final'
                      dependencySet('javax.validation:2.0.1.Final') {
                          entry 'validation-api'
                      }
                      dependencySet(group:'javax.validation', version: '2.0.1.Final') {
                          entry 'validation-api'
                      }
                  }
              }
              """,
            """
              plugins {
                  id 'java'
                  id 'org.springframework.boot' version '3.5.0'
                  id 'io.spring.dependency-management' version '1.1.7'
              }

              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  dependencies {
                      dependency 'jakarta.validation:jakarta.validation-api:3.0.2'
                      dependency group: 'jakarta.validation', name: 'jakarta.validation-api', version: '3.0.2'
                      dependencySet('jakarta.validation:3.0.2') {
                          entry 'jakarta.validation-api'
                      }
                      dependencySet(group:'jakarta.validation', version: '3.0.2') {
                          entry 'jakarta.validation-api'
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void kotlinDependencyPluginManagedDependencies() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  java
                  id("org.springframework.boot") version "3.5.0"
                  id("io.spring.dependency-management") version "1.1.7"
              }
              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  dependencies {
                      dependency("javax.validation:validation-api:2.0.1.Final")
                      dependency(mapOf("group" to "javax.validation", "name" to "validation-api", "version" to "2.0.1.Final"))
                      dependencySet("javax.validation:2.0.1.Final") {
                          entry("validation-api")
                      }
                      dependencySet(mapOf("group" to "javax.validation", "version" to "2.0.1.Final")) {
                          entry("validation-api")
                      }
                  }
              }
              """,
            """
              plugins {
                  java
                  id("org.springframework.boot") version "3.5.0"
                  id("io.spring.dependency-management") version "1.1.7"
              }
              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  dependencies {
                      dependency("jakarta.validation:jakarta.validation-api:3.0.2")
                      dependency(mapOf("group" to "jakarta.validation", "name" to "jakarta.validation-api", "version" to "3.0.2"))
                      dependencySet("jakarta.validation:3.0.2") {
                          entry("jakarta.validation-api")
                      }
                      dependencySet(mapOf("group" to "jakarta.validation", "version" to "3.0.2")) {
                          entry("jakarta.validation-api")
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void makeChangesInDependencyManagementImports() {
        String oldGroupId = "io.moderne.recipe";
        String oldArtifactId = "moderne-recipe-bom";
        DependencyMatcher matcher = Objects.requireNonNull(DependencyMatcher.build(oldGroupId + ":" + oldArtifactId).getValue());
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new SpringDependencyManagementPluginEntry.Matcher().groupId(oldGroupId).artifactId(oldArtifactId).asVisitor((dep, ctx) -> dep.withGroupArtifactVersion(
              matcher, "org.openrewrite", "rewrite-core", "8.44.1", null, null, ctx).getTree()
            ))),
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'io.spring.dependency-management' version '1.1.7'
              }
              repositories {
                  mavenCentral()
              }

              dependencies {
                implementation platform("com.google.guava:guava:29.0-jre")
              }
              dependencyManagement {
                  imports {
                      mavenBom "io.moderne.recipe:moderne-recipe-bom:0.13.0"
                  }
              }
              """,
            """
              plugins {
                  id 'java'
                  id 'io.spring.dependency-management' version '1.1.7'
              }
              repositories {
                  mavenCentral()
              }

              dependencies {
                implementation platform("com.google.guava:guava:29.0-jre")
              }
              dependencyManagement {
                  imports {
                      mavenBom "org.openrewrite:rewrite-core:8.44.1"
                  }
              }
              """
          )
        );
    }

    @Test
    void makeChangesInKotlinDependencyManagementImports() {
        String oldGroupId = "io.moderne.recipe";
        String oldArtifactId = "moderne-recipe-bom";
        DependencyMatcher matcher = Objects.requireNonNull(DependencyMatcher.build(oldGroupId + ":" + oldArtifactId).getValue());
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new SpringDependencyManagementPluginEntry.Matcher().groupId(oldGroupId).artifactId(oldArtifactId).asVisitor((dep, ctx) -> dep.withGroupArtifactVersion(
              matcher, "org.openrewrite", "rewrite-core", "8.44.1", null, null, ctx).getTree()
            ))),
          buildGradleKts(
            """
              plugins {
                  java
                  id("org.springframework.boot") version "3.5.0"
                  id("io.spring.dependency-management") version "1.1.7"
              }
              repositories {
                  mavenCentral()
              }

              dependencies {
                implementation(platform("com.google.guava:guava:29.0-jre"))
              }
              dependencyManagement {
                  imports {
                      mavenBom("io.moderne.recipe:moderne-recipe-bom:0.13.0")
                  }
              }
              """,
            """
              plugins {
                  java
                  id("org.springframework.boot") version "3.5.0"
                  id("io.spring.dependency-management") version "1.1.7"
              }
              repositories {
                  mavenCentral()
              }

              dependencies {
                implementation(platform("com.google.guava:guava:29.0-jre"))
              }
              dependencyManagement {
                  imports {
                      mavenBom("org.openrewrite:rewrite-core:8.44.1")
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotChangeDependencySetWhenMatcherDoesNotMatchAllManagedArtifacts() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'org.springframework.boot' version '3.5.0'
                  id 'io.spring.dependency-management' version '1.1.7'
              }

              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  dependencies {
                      dependency 'javax.validation:validation-api:2.0.1.Final'
                      dependency group: 'javax.validation', name: 'validation-api', version: '2.0.1.Final'
                      dependencySet('javax.validation:2.0.1.Final') {
                          entry 'validation-api'
                          entry 'com.springsource.javax.validation'
                      }
                      dependencySet(group:'javax.validation', version: '2.0.1.Final') {
                          entry 'validation-api'
                          entry 'com.springsource.javax.validation'
                      }
                  }
              }
              """,
            """
              plugins {
                  id 'java'
                  id 'org.springframework.boot' version '3.5.0'
                  id 'io.spring.dependency-management' version '1.1.7'
              }

              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  dependencies {
                      dependency 'jakarta.validation:jakarta.validation-api:3.0.2'
                      dependency group: 'jakarta.validation', name: 'jakarta.validation-api', version: '3.0.2'
                      dependencySet('javax.validation:2.0.1.Final') {
                          entry 'validation-api'
                          entry 'com.springsource.javax.validation'
                      }
                      dependencySet(group:'javax.validation', version: '2.0.1.Final') {
                          entry 'validation-api'
                          entry 'com.springsource.javax.validation'
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotChangeDependencySetKotlinWhenMatcherDoesNotMatchAllManagedArtifacts() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  java
                  id("org.springframework.boot") version "3.5.0"
                  id("io.spring.dependency-management") version "1.1.7"
              }
              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  dependencies {
                      dependency("javax.validation:validation-api:2.0.1.Final")
                      dependency(mapOf("group" to "javax.validation", "name" to "validation-api", "version" to "2.0.1.Final"))
                      dependencySet("javax.validation:2.0.1.Final") {
                          entry("validation-api")
                          entry("com.springsource.javax.validation")
                      }
                      dependencySet(mapOf("group" to "javax.validation", "version" to "2.0.1.Final")) {
                          entry("validation-api")
                          entry("com.springsource.javax.validation")
                      }
                  }
              }
              """,
            """
              plugins {
                  java
                  id("org.springframework.boot") version "3.5.0"
                  id("io.spring.dependency-management") version "1.1.7"
              }
              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  dependencies {
                      dependency("jakarta.validation:jakarta.validation-api:3.0.2")
                      dependency(mapOf("group" to "jakarta.validation", "name" to "jakarta.validation-api", "version" to "3.0.2"))
                      dependencySet("javax.validation:2.0.1.Final") {
                          entry("validation-api")
                          entry("com.springsource.javax.validation")
                      }
                      dependencySet(mapOf("group" to "javax.validation", "version" to "2.0.1.Final")) {
                          entry("validation-api")
                          entry("com.springsource.javax.validation")
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void changeGroupOnly() {
        String oldGroupId = "javax.validation";
        String oldArtifactId = "validation-api";
        DependencyMatcher matcher = Objects.requireNonNull(DependencyMatcher.build(oldGroupId + ":" + oldArtifactId).getValue());
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new SpringDependencyManagementPluginEntry.Matcher().groupId(oldGroupId).artifactId(oldArtifactId).asVisitor((dep, ctx) -> dep.withGroupArtifactVersion(
              matcher, "jakarta.validation", null, null, null, null, ctx).getTree()
            ))),
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'org.springframework.boot' version '3.5.0'
                  id 'io.spring.dependency-management' version '1.1.7'
              }

              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  dependencies {
                      dependency 'javax.validation:validation-api:2.0.1.Final'
                      dependency group: 'javax.validation', name: 'validation-api', version: '2.0.1.Final'
                      dependencySet('javax.validation:2.0.1.Final') {
                          entry 'validation-api'
                      }
                      dependencySet(group:'javax.validation', version: '2.0.1.Final') {
                          entry 'validation-api'
                      }
                  }
              }
              """,
            """
              plugins {
                  id 'java'
                  id 'org.springframework.boot' version '3.5.0'
                  id 'io.spring.dependency-management' version '1.1.7'
              }

              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  dependencies {
                      dependency 'jakarta.validation:validation-api:2.0.1.Final'
                      dependency group: 'jakarta.validation', name: 'validation-api', version: '2.0.1.Final'
                      dependencySet('jakarta.validation:2.0.1.Final') {
                          entry 'validation-api'
                      }
                      dependencySet(group:'jakarta.validation', version: '2.0.1.Final') {
                          entry 'validation-api'
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void changeGroupOnlyKotlin() {
        String oldGroupId = "javax.validation";
        String oldArtifactId = "validation-api";
        DependencyMatcher matcher = Objects.requireNonNull(DependencyMatcher.build(oldGroupId + ":" + oldArtifactId).getValue());
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new SpringDependencyManagementPluginEntry.Matcher().groupId(oldGroupId).artifactId(oldArtifactId).asVisitor((dep, ctx) -> dep.withGroupArtifactVersion(
              matcher, "jakarta.validation", null, null, null, null, ctx).getTree()
            ))),
          buildGradleKts(
            """
              plugins {
                  java
                  id("org.springframework.boot") version "3.5.0"
                  id("io.spring.dependency-management") version "1.1.7"
              }
              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  dependencies {
                      dependency("javax.validation:validation-api:2.0.1.Final")
                      dependency(mapOf("group" to "javax.validation", "name" to "validation-api", "version" to "2.0.1.Final"))
                      dependencySet("javax.validation:2.0.1.Final") {
                          entry("validation-api")
                      }
                      dependencySet(mapOf("group" to "javax.validation", "version" to "2.0.1.Final")) {
                          entry("validation-api")
                      }
                  }
              }
              """,
            """
              plugins {
                  java
                  id("org.springframework.boot") version "3.5.0"
                  id("io.spring.dependency-management") version "1.1.7"
              }
              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  dependencies {
                      dependency("jakarta.validation:validation-api:2.0.1.Final")
                      dependency(mapOf("group" to "jakarta.validation", "name" to "validation-api", "version" to "2.0.1.Final"))
                      dependencySet("jakarta.validation:2.0.1.Final") {
                          entry("validation-api")
                      }
                      dependencySet(mapOf("group" to "jakarta.validation", "version" to "2.0.1.Final")) {
                          entry("validation-api")
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void changeArtifactOnly() {
        String oldGroupId = "javax.validation";
        String oldArtifactId = "validation-api";
        DependencyMatcher matcher = Objects.requireNonNull(DependencyMatcher.build(oldGroupId + ":" + oldArtifactId).getValue());
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new SpringDependencyManagementPluginEntry.Matcher().groupId(oldGroupId).artifactId(oldArtifactId).asVisitor((dep, ctx) -> dep.withGroupArtifactVersion(
              matcher, null, "jakarta.validation-api", null, null, null, ctx).getTree()
            ))),
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'org.springframework.boot' version '3.5.0'
                  id 'io.spring.dependency-management' version '1.1.7'
              }

              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  dependencies {
                      dependency 'javax.validation:validation-api:2.0.1.Final'
                      dependency group: 'javax.validation', name: 'validation-api', version: '2.0.1.Final'
                      dependencySet('javax.validation:2.0.1.Final') {
                          entry 'validation-api'
                      }
                      dependencySet(group:'javax.validation', version: '2.0.1.Final') {
                          entry 'validation-api'
                      }
                  }
              }
              """,
            """
              plugins {
                  id 'java'
                  id 'org.springframework.boot' version '3.5.0'
                  id 'io.spring.dependency-management' version '1.1.7'
              }

              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  dependencies {
                      dependency 'javax.validation:jakarta.validation-api:2.0.1.Final'
                      dependency group: 'javax.validation', name: 'jakarta.validation-api', version: '2.0.1.Final'
                      dependencySet('javax.validation:2.0.1.Final') {
                          entry 'jakarta.validation-api'
                      }
                      dependencySet(group:'javax.validation', version: '2.0.1.Final') {
                          entry 'jakarta.validation-api'
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void changeArtifactOnlyKotlin() {
        String oldGroupId = "javax.validation";
        String oldArtifactId = "validation-api";
        DependencyMatcher matcher = Objects.requireNonNull(DependencyMatcher.build(oldGroupId + ":" + oldArtifactId).getValue());
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new SpringDependencyManagementPluginEntry.Matcher().groupId(oldGroupId).artifactId(oldArtifactId).asVisitor((dep, ctx) -> dep.withGroupArtifactVersion(
              matcher, null, "jakarta.validation-api", null, null, null, ctx).getTree()
            ))),
          buildGradleKts(
            """
              plugins {
                  java
                  id("org.springframework.boot") version "3.5.0"
                  id("io.spring.dependency-management") version "1.1.7"
              }
              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  dependencies {
                      dependency("javax.validation:validation-api:2.0.1.Final")
                      dependency(mapOf("group" to "javax.validation", "name" to "validation-api", "version" to "2.0.1.Final"))
                      dependencySet("javax.validation:2.0.1.Final") {
                          entry("validation-api")
                      }
                      dependencySet(mapOf("group" to "javax.validation", "version" to "2.0.1.Final")) {
                          entry("validation-api")
                      }
                  }
              }
              """,
            """
              plugins {
                  java
                  id("org.springframework.boot") version "3.5.0"
                  id("io.spring.dependency-management") version "1.1.7"
              }
              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  dependencies {
                      dependency("javax.validation:jakarta.validation-api:2.0.1.Final")
                      dependency(mapOf("group" to "javax.validation", "name" to "jakarta.validation-api", "version" to "2.0.1.Final"))
                      dependencySet("javax.validation:2.0.1.Final") {
                          entry("jakarta.validation-api")
                      }
                      dependencySet(mapOf("group" to "javax.validation", "version" to "2.0.1.Final")) {
                          entry("jakarta.validation-api")
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void changeVersionOnly() {
        String oldGroupId = "javax.validation";
        String oldArtifactId = "validation-api";
        DependencyMatcher matcher = Objects.requireNonNull(DependencyMatcher.build(oldGroupId + ":" + oldArtifactId).getValue());
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new SpringDependencyManagementPluginEntry.Matcher().groupId(oldGroupId).artifactId(oldArtifactId).asVisitor((dep, ctx) -> dep.withGroupArtifactVersion(
              matcher, null, null, "2.0.0.Final", null, null, ctx).getTree()
            ))),
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'org.springframework.boot' version '3.5.0'
                  id 'io.spring.dependency-management' version '1.1.7'
              }

              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  dependencies {
                      dependency 'javax.validation:validation-api:2.0.1.Final'
                      dependency group: 'javax.validation', name: 'validation-api', version: '2.0.1.Final'
                      dependencySet('javax.validation:2.0.1.Final') {
                          entry 'validation-api'
                      }
                      dependencySet(group:'javax.validation', version: '2.0.1.Final') {
                          entry 'validation-api'
                      }
                  }
              }
              """,
            """
              plugins {
                  id 'java'
                  id 'org.springframework.boot' version '3.5.0'
                  id 'io.spring.dependency-management' version '1.1.7'
              }

              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  dependencies {
                      dependency 'javax.validation:validation-api:2.0.0.Final'
                      dependency group: 'javax.validation', name: 'validation-api', version: '2.0.0.Final'
                      dependencySet('javax.validation:2.0.0.Final') {
                          entry 'validation-api'
                      }
                      dependencySet(group:'javax.validation', version: '2.0.0.Final') {
                          entry 'validation-api'
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void changeVersionOnlyKotlin() {
        String oldGroupId = "javax.validation";
        String oldArtifactId = "validation-api";
        DependencyMatcher matcher = Objects.requireNonNull(DependencyMatcher.build(oldGroupId + ":" + oldArtifactId).getValue());
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new SpringDependencyManagementPluginEntry.Matcher().groupId(oldGroupId).artifactId(oldArtifactId).asVisitor((dep, ctx) -> dep.withGroupArtifactVersion(
              matcher, null, null, "2.0.0.Final", null, null, ctx).getTree()
            ))),
          buildGradleKts(
            """
              plugins {
                  java
                  id("org.springframework.boot") version "3.5.0"
                  id("io.spring.dependency-management") version "1.1.7"
              }
              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  dependencies {
                      dependency("javax.validation:validation-api:2.0.1.Final")
                      dependency(mapOf("group" to "javax.validation", "name" to "validation-api", "version" to "2.0.1.Final"))
                      dependencySet("javax.validation:2.0.1.Final") {
                          entry("validation-api")
                      }
                      dependencySet(mapOf("group" to "javax.validation", "version" to "2.0.1.Final")) {
                          entry("validation-api")
                      }
                  }
              }
              """,
            """
              plugins {
                  java
                  id("org.springframework.boot") version "3.5.0"
                  id("io.spring.dependency-management") version "1.1.7"
              }
              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  dependencies {
                      dependency("javax.validation:validation-api:2.0.0.Final")
                      dependency(mapOf("group" to "javax.validation", "name" to "validation-api", "version" to "2.0.0.Final"))
                      dependencySet("javax.validation:2.0.0.Final") {
                          entry("validation-api")
                      }
                      dependencySet(mapOf("group" to "javax.validation", "version" to "2.0.0.Final")) {
                          entry("validation-api")
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void changeDependencySetWhenMatcherDoesMatchAllManagedArtifacts() {
        String oldGroupId = "javax.validation";
        String oldArtifactId = "*";
        DependencyMatcher matcher = Objects.requireNonNull(DependencyMatcher.build(oldGroupId + ":" + oldArtifactId).getValue());
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
              new SpringDependencyManagementPluginEntry.Matcher().groupId(oldGroupId).artifactId(oldArtifactId).asVisitor((dep, ctx) -> dep.withGroupArtifactVersion(
                matcher, "jakarta.validation", null, null, null, null, ctx).getTree()
              ))),
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'org.springframework.boot' version '3.5.0'
                  id 'io.spring.dependency-management' version '1.1.7'
              }

              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  dependencies {
                      dependencySet('javax.validation:2.0.1.Final') {
                          entry 'validation-api'
                          entry 'com.springsource.javax.validation'
                      }
                      dependencySet(group:'javax.validation', version: '2.0.1.Final') {
                          entry 'validation-api'
                          entry 'com.springsource.javax.validation'
                      }
                  }
              }
              """,
            """
              plugins {
                  id 'java'
                  id 'org.springframework.boot' version '3.5.0'
                  id 'io.spring.dependency-management' version '1.1.7'
              }

              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  dependencies {
                      dependencySet('jakarta.validation:2.0.1.Final') {
                          entry 'validation-api'
                          entry 'com.springsource.javax.validation'
                      }
                      dependencySet(group:'jakarta.validation', version: '2.0.1.Final') {
                          entry 'validation-api'
                          entry 'com.springsource.javax.validation'
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void changeDependencySetKotlinWhenMatcherDoesMatchAllManagedArtifacts() {
        String oldGroupId = "javax.validation";
        String oldArtifactId = "*";
        DependencyMatcher matcher = Objects.requireNonNull(DependencyMatcher.build(oldGroupId + ":" + oldArtifactId).getValue());
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() ->
            new SpringDependencyManagementPluginEntry.Matcher().groupId(oldGroupId).artifactId(oldArtifactId).asVisitor((dep, ctx) -> dep.withGroupArtifactVersion(
              matcher, "jakarta.validation", null, null, null, null, ctx).getTree()
            ))),
          buildGradleKts(
            """
              plugins {
                  java
                  id("org.springframework.boot") version "3.5.0"
                  id("io.spring.dependency-management") version "1.1.7"
              }
              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  dependencies {
                      dependencySet("javax.validation:2.0.1.Final") {
                          entry("validation-api")
                          entry("com.springsource.javax.validation")
                      }
                      dependencySet(mapOf("group" to "javax.validation", "version" to "2.0.1.Final")) {
                          entry("validation-api")
                          entry("com.springsource.javax.validation")
                      }
                  }
              }
              """,
            """
              plugins {
                  java
                  id("org.springframework.boot") version "3.5.0"
                  id("io.spring.dependency-management") version "1.1.7"
              }
              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  dependencies {
                      dependencySet("jakarta.validation:2.0.1.Final") {
                          entry("validation-api")
                          entry("com.springsource.javax.validation")
                      }
                      dependencySet(mapOf("group" to "jakarta.validation", "version" to "2.0.1.Final")) {
                          entry("validation-api")
                          entry("com.springsource.javax.validation")
                      }
                  }
              }
              """
          )
        );
    }
}
