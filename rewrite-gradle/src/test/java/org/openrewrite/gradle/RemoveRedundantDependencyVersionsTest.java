/*
 * Copyright 2024 the original author or authors.
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

@SuppressWarnings("GroovyAssignabilityCheck")
class RemoveRedundantDependencyVersionsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
          .recipe(new RemoveRedundantDependencyVersions(null, null, null));
    }

    @DocumentExample
    @Test
    void literal() {
        rewriteRun(
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
                  implementation("org.apache.commons:commons-lang3:3.14.0")
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
                  implementation("org.apache.commons:commons-lang3")
              }
              """
          )
        );
    }

    @Test
    void mapEntry() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(platform(group: "org.springframework.boot", name: "spring-boot-dependencies", version: "3.3.3"))
                  implementation(group: "org.apache.commons", name: "commons-lang3", version: "3.14.0")
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
                  implementation(platform(group: "org.springframework.boot", name: "spring-boot-dependencies", version: "3.3.3"))
                  implementation(group: "org.apache.commons", name: "commons-lang3")
              }
              """
          )
        );
    }

    @Test
    void mapLiteral() {
        rewriteRun(
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
                  implementation([group: "org.apache.commons", name: "commons-lang3", version: "3.14.0"])
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
                  implementation([group: "org.apache.commons", name: "commons-lang3"])
              }
              """
          )
        );
    }

    @Test
    void enforcedPlatform() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(enforcedPlatform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("org.apache.commons:commons-lang3:3.14.0")
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
                  implementation(enforcedPlatform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("org.apache.commons:commons-lang3")
              }
              """
          )
        );
    }

    @Test
    void platformUsingMapEntry() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(enforcedPlatform(group: "org.springframework.boot", name: "spring-boot-dependencies", version: "3.3.3"))
                  implementation("org.apache.commons:commons-lang3:3.14.0")
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
                  implementation(enforcedPlatform(group: "org.springframework.boot", name: "spring-boot-dependencies", version: "3.3.3"))
                  implementation("org.apache.commons:commons-lang3")
              }
              """
          )
        );
    }

    @Test
    void freestandingScript() {
        rewriteRun(
          buildGradle(
            """
              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("org.apache.commons:commons-lang3:3.14.0")
              }
              """,
            """
              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("org.apache.commons:commons-lang3")
              }
              """,
            spec -> spec.path("dependencies.gradle")
          ),
          buildGradle(
            """
              plugins {
                  id("java")
              }

              apply from: 'dependencies.gradle'
              """
          )
        );
    }

    @Test
    void removeUnneededConstraint() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  constraints {
                      implementation('org.springframework:spring-core:6.2.1') {
                          because 'Gradle is resolving 6.2.2 already, this constraint has no effect and can be removed'
                      }
                  }
                  implementation 'org.springframework.boot:spring-boot:3.4.2'
              }
              """,
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation 'org.springframework.boot:spring-boot:3.4.2'
              }
              """
          )
        );
    }

    @Test
    void keepStrictConstraint() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'java'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  constraints {
                      implementation('org.springframework:spring-core:6.2.1!!') {
                          because 'The !! forces the usage of 6.2.1'
                      }
                  }
                  implementation 'org.springframework.boot:spring-boot:3.4.2'
              }
              """
          )
        );
    }

    @Test
    void removeDirectDependencyWithLowerVersionNumberIfDependencyIsLoadedTransitivelyWithHigherVersionNumber() {
        rewriteRun(
          spec -> spec.recipe(new RemoveRedundantDependencyVersions(null, null, RemoveRedundantDependencyVersions.Comparator.GTE)),
          buildGradle(
            """
              plugins {
                  id "java-library"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  // BOM `spring-boot-dependencies:3.3.3` describes `spring-webmvc:6.1.12`
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("org.springframework.boot:spring-boot-starter-web-services:3.3.3")
                  implementation("org.springframework:spring-webmvc:6.1.11")
              }
              """,
            """
              plugins {
                  id "java-library"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  // BOM `spring-boot-dependencies:3.3.3` describes `spring-webmvc:6.1.12`
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("org.springframework.boot:spring-boot-starter-web-services")
              }
              """
          )
        );
    }

    @Test
    void removeDirectDependencyIfDependencyIsLoadedTransitivelyWithSameVersion() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java-library"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  // BOM `spring-boot-dependencies:3.3.3` describes `spring-webmvc:6.1.12`
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("org.springframework.boot:spring-boot-starter-web-services:3.3.3")
                  implementation("org.springframework:spring-webmvc:6.1.12")
              }
              """,
            """
              plugins {
                  id "java-library"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  // BOM `spring-boot-dependencies:3.3.3` describes `spring-webmvc:6.1.12`
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("org.springframework.boot:spring-boot-starter-web-services")
              }
              """
          )
        );
    }

    @Test
    void keepDirectDependencyWithHigherVersionNumberIfDependencyIsLoadedTransitivelyWithLowerVersionNumber() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java-library"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  // Without explicit spring-webmvc:6.2.8 gradle would resolve 6.2.7
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("org.springframework.boot:spring-boot-starter-web-services:3.3.3")
                  implementation("org.springframework:spring-webmvc:6.2.8")
              }
              """,
            """
              plugins {
                  id "java-library"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  // Without explicit spring-webmvc:6.2.8 gradle would resolve 6.2.7
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("org.springframework.boot:spring-boot-starter-web-services")
                  implementation("org.springframework:spring-webmvc:6.2.8")
              }
              """
          )
        );
    }

    @Test
    void handleSeveralPlatformDependencies() {
        rewriteRun(
          spec -> spec.recipe(new RemoveRedundantDependencyVersions(null, null, RemoveRedundantDependencyVersions.Comparator.GTE)),
          buildGradle(
            """
              plugins {
                  id "java-library"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                   implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                   implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2024.0.0"))
                   implementation("org.springframework.boot:spring-boot-starter-web-services")
                   implementation("org.springframework.cloud:spring-cloud-starter-config")
                   implementation("org.springframework:spring-webmvc:6.1.10")
                   implementation("org.springframework.boot:spring-boot-starter-actuator:3.3.3")
                   implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
                   implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client:4.1.0")
                   implementation("org.springframework.cloud:spring-cloud-starter-openfeign:4.1.1")
                   implementation("org.springframework.cloud:spring-cloud-starter-gateway:4.1.2")
              }
              """,
            """
              plugins {
                  id "java-library"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                   implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                   implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2024.0.0"))
                   implementation("org.springframework.boot:spring-boot-starter-web-services")
                   implementation("org.springframework.cloud:spring-cloud-starter-config")
                   implementation("org.springframework.boot:spring-boot-starter-actuator")
                   implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
                   implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
                   implementation("org.springframework.cloud:spring-cloud-starter-gateway")
              }
              """
          )
        );
    }

    @Test
    void transitiveConfiguration() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java-library"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  api(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("org.apache.commons:commons-lang3:3.14.0")
              }
              """,
            """
              plugins {
                  id "java-library"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  api(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
                  implementation("org.apache.commons:commons-lang3")
              }
              """
          )
        );
    }

    @Test
    void unmanagedDependency() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation("org.apache.commons:commons-lang3:3.14.0")

                  testImplementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.3"))
              }
              """
          )
        );
    }

    @Test
    void removeUnmanagedDependencyIfDependencyIsLoadedTransitivelyIsExactlyTheSame() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation('org.flywaydb:flyway-sqlserver:10.10.0')
                  runtimeOnly('org.flywaydb:flyway-core:10.10.0')
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
                  implementation('org.flywaydb:flyway-sqlserver:10.10.0')
              }
              """
          )
        );
    }

    @Test
    void keepUnmanagedDependencyIfDependencyIsLoadedTransitivelyIsDifferentVersion() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation('org.flywaydb:flyway-sqlserver:10.10.0')
                  runtimeOnly('org.flywaydb:flyway-core:10.11.0')
              }
              """
          )
        );
    }

    @Test
    void keepUnmanagedDirectDependencyIfDependencyIsLoadedTransitivelyHasNoVersion() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation('org.flywaydb:flyway-sqlserver')
                  runtimeOnly('org.flywaydb:flyway-core')
              }
              """
          )
        );
    }

    @Test
    void kotlin() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  constraints {
                      implementation("org.springframework:spring-core:6.2.1") {
                          because("Gradle is resolving 6.2.2 already, this constraint has no effect and can be removed")
                      }
                  }
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.4.2"))
                  implementation("org.springframework.boot:spring-boot:3.4.2")
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
                  implementation(platform("org.springframework.boot:spring-boot-dependencies:3.4.2"))
                  implementation("org.springframework.boot:spring-boot")
              }
              """
          )
        );
    }

    @Test
    void differentScope() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  `java-library`
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  constraints {
                      testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0") {
                          because("Some reason")
                      }
                  }
                  testImplementation(platform("org.junit:junit-bom:5.10.1"))
                  testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
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
                  testImplementation(platform("org.junit:junit-bom:5.10.1"))
                  testImplementation("org.junit.jupiter:junit-jupiter")
              }
              """
          )
        );
    }

    @Test
    void removeRepeatedDependency() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation("org.openrewrite:rewrite-core:8.56.0")
                  implementation("org.openrewrite:rewrite-core:8.55.0")
                  implementation("org.openrewrite:rewrite-core:8.56.0")
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
                  implementation("org.openrewrite:rewrite-core:8.56.0")
              }
              """
          )
        );
    }

    @Test
    void webfluxGTE() {
        rewriteRun(
          spec -> spec.recipe(new RemoveRedundantDependencyVersions(null, null, RemoveRedundantDependencyVersions.Comparator.GTE)),
          buildGradle(
            """
              plugins {
                  id "java"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  // spring-cloud-starter-gateway-server-webflux:4.30 depends on reactor-netty-http:1.2.6
                  implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux:4.3.0")
                  implementation("io.projectreactor.netty:reactor-netty-http:1.1.13")
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
                  // spring-cloud-starter-gateway-server-webflux:4.30 depends on reactor-netty-http:1.2.6
                  implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux:4.3.0")
              }
              """
          )
        );
    }
}
