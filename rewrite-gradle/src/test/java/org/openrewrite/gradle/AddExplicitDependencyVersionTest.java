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
import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

class AddExplicitDependencyVersionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi());
    }

    @DocumentExample
    @Test
    void addVersionToStringNotationFromBom() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new AddExplicitDependencyVersion("org.apache.commons", "commons-lang3"))),
          buildGradle(
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("org.apache.commons:commons-lang3")
              }
              """,
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("org.apache.commons:commons-lang3:3.14.0")
              }
              """
          )
        );
    }

    @Test
    void addVersionToMapNotationFromBom() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new AddExplicitDependencyVersion("org.apache.commons", "commons-lang3"))),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation group: 'org.apache.commons', name: 'commons-lang3'
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation group: 'org.apache.commons', name: 'commons-lang3', version: '3.14.0'
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenVersionAlreadyDeclared() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new AddExplicitDependencyVersion("org.apache.commons", "commons-lang3"))),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'org.apache.commons:commons-lang3:3.12.0'
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenVersionAlreadyDeclaredInMapNotation() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new AddExplicitDependencyVersion("org.apache.commons", "commons-lang3"))),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation group: 'org.apache.commons', name: 'commons-lang3', version: '3.12.0'
              }
              """
          )
        );
    }

    @Test
    void addVersionWithGlobPattern() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new AddExplicitDependencyVersion("com.fasterxml.jackson.core", "*"))),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'com.fasterxml.jackson.core:jackson-core'
                  implementation 'com.fasterxml.jackson.core:jackson-databind'
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'com.fasterxml.jackson.core:jackson-core:2.17.2'
                  implementation 'com.fasterxml.jackson.core:jackson-databind:2.17.2'
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenDependencyNotResolved() {
        // When the dependency can't be resolved (no version and no BOM management), no change should be made
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new AddExplicitDependencyVersion("com.example", "nonexistent"))),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation 'com.example:nonexistent'
              }
              """
          )
        );
    }

    @Test
    void addVersionToDoubleQuotedString() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new AddExplicitDependencyVersion("org.apache.commons", "commons-lang3"))),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation "org.apache.commons:commons-lang3"
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation "org.apache.commons:commons-lang3:3.14.0"
              }
              """
          )
        );
    }

    @Test
    void addVersionWithParentheses() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new AddExplicitDependencyVersion("org.apache.commons", "commons-lang3"))),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation('org.apache.commons:commons-lang3')
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation('org.apache.commons:commons-lang3:3.14.0')
              }
              """
          )
        );
    }

    @Test
    void noChangeForNonMatchingDependency() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new AddExplicitDependencyVersion("org.apache.commons", "commons-lang3"))),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'com.fasterxml.jackson.core:jackson-core'
              }
              """
          )
        );
    }

    @Test
    void kotlinDslAddVersionToStringNotation() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new AddExplicitDependencyVersion("org.apache.commons", "commons-lang3"))),
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("org.apache.commons:commons-lang3")
              }
              """,
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("org.apache.commons:commons-lang3:3.14.0")
              }
              """
          )
        );
    }

    @Test
    void kotlinDslAddVersionToMapNotation() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new AddExplicitDependencyVersion("org.apache.commons", "commons-lang3"))),
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation(group = "org.apache.commons", name = "commons-lang3")
              }
              """,
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation(group = "org.apache.commons", name = "commons-lang3", version = "3.14.0")
              }
              """
          )
        );
    }

    @Test
    void kotlinDslNoChangeWhenVersionAlreadyDeclared() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new AddExplicitDependencyVersion("org.apache.commons", "commons-lang3"))),
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("org.apache.commons:commons-lang3:3.12.0")
              }
              """
          )
        );
    }

    @Test
    void enforcedPlatform() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new AddExplicitDependencyVersion("org.apache.commons", "commons-lang3"))),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation enforcedPlatform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'org.apache.commons:commons-lang3'
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation enforcedPlatform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation 'org.apache.commons:commons-lang3:3.14.0'
              }
              """
          )
        );
    }

    @Test
    void addVersionToMapLiteralNotation() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new AddExplicitDependencyVersion("org.apache.commons", "commons-lang3"))),
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation([group: 'org.apache.commons', name: 'commons-lang3'])
              }
              """,
            """
              plugins {
                  id 'java-library'
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation platform('org.springframework.boot:spring-boot-dependencies:3.3.3')
                  implementation([group: 'org.apache.commons', name: 'commons-lang3', version: '3.14.0'])
              }
              """
          )
        );
    }

    @Test
    void addVersionWithSpringDependencyManagementPlugin() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new AddExplicitDependencyVersion("org.apache.commons", "commons-lang3"))),
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'io.spring.dependency-management' version '1.1.6'
              }

              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  imports {
                      mavenBom 'org.springframework.boot:spring-boot-dependencies:3.3.3'
                  }
              }

              dependencies {
                  implementation 'org.apache.commons:commons-lang3'
              }
              """,
            """
              plugins {
                  id 'java'
                  id 'io.spring.dependency-management' version '1.1.6'
              }

              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  imports {
                      mavenBom 'org.springframework.boot:spring-boot-dependencies:3.3.3'
                  }
              }

              dependencies {
                  implementation 'org.apache.commons:commons-lang3:3.14.0'
              }
              """
          )
        );
    }

    @Test
    void addVersionWithSpringDependencyManagementPluginMapNotation() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new AddExplicitDependencyVersion("org.apache.commons", "commons-lang3"))),
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'io.spring.dependency-management' version '1.1.6'
              }

              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  imports {
                      mavenBom 'org.springframework.boot:spring-boot-dependencies:3.3.3'
                  }
              }

              dependencies {
                  implementation group: 'org.apache.commons', name: 'commons-lang3'
              }
              """,
            """
              plugins {
                  id 'java'
                  id 'io.spring.dependency-management' version '1.1.6'
              }

              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  imports {
                      mavenBom 'org.springframework.boot:spring-boot-dependencies:3.3.3'
                  }
              }

              dependencies {
                  implementation group: 'org.apache.commons', name: 'commons-lang3', version: '3.14.0'
              }
              """
          )
        );
    }

    @Test
    void addVersionWithSpringDependencyManagementPluginMultipleDependencies() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new AddExplicitDependencyVersion("com.fasterxml.jackson.core", "*"))),
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'io.spring.dependency-management' version '1.1.6'
              }

              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  imports {
                      mavenBom 'org.springframework.boot:spring-boot-dependencies:3.3.3'
                  }
              }

              dependencies {
                  implementation 'com.fasterxml.jackson.core:jackson-core'
                  implementation 'com.fasterxml.jackson.core:jackson-databind'
              }
              """,
            """
              plugins {
                  id 'java'
                  id 'io.spring.dependency-management' version '1.1.6'
              }

              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  imports {
                      mavenBom 'org.springframework.boot:spring-boot-dependencies:3.3.3'
                  }
              }

              dependencies {
                  implementation 'com.fasterxml.jackson.core:jackson-core:2.17.2'
                  implementation 'com.fasterxml.jackson.core:jackson-databind:2.17.2'
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenVersionAlreadyDeclaredWithSpringDependencyManagementPlugin() {
        rewriteRun(
          spec -> spec.recipe(RewriteTest.toRecipe(() -> new AddExplicitDependencyVersion("org.apache.commons", "commons-lang3"))),
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'io.spring.dependency-management' version '1.1.6'
              }

              repositories {
                  mavenCentral()
              }

              dependencyManagement {
                  imports {
                      mavenBom 'org.springframework.boot:spring-boot-dependencies:3.3.3'
                  }
              }

              dependencies {
                  implementation 'org.apache.commons:commons-lang3:3.12.0'
              }
              """
          )
        );
    }
}
