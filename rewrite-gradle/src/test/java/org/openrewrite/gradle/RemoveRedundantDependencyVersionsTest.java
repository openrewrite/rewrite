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
    void supportSpringBootDependenciesPlugin() {
        rewriteRun(
          spec -> spec.recipe(new RemoveRedundantDependencyVersions(null, null, RemoveRedundantDependencyVersions.Comparator.GTE)),
          buildGradle(
            """
              plugins {
                  id "java"
                  id("org.springframework.boot") version "3.5.6"
                  id("io.spring.dependency-management") version "1.1.7"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation('org.springframework.boot:spring-boot')
                  implementation("org.apache.commons:commons-lang3:3.14.0")
                  implementation("org.openrewrite:rewrite-core:8.62.0")
                  implementation("javax.validation:validation-api:2.0.0.Final")
                  implementation("org.openrewrite.recipe:rewrite-quarkus:2.25.1")
              }
              
              dependencyManagement {
                  dependencies {
                      dependency("javax.validation:validation-api:2.0.1.Final")
                      dependencySet('org.openrewrite:8.62.2') {
                          entry 'rewrite-core'
                      }
                  }
                  imports {
                      mavenBom "org.openrewrite.recipe:rewrite-recipe-bom:3.14.1"
                  }
              }
              """,
            """
              plugins {
                  id "java"
                  id("org.springframework.boot") version "3.5.6"
                  id("io.spring.dependency-management") version "1.1.7"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation('org.springframework.boot:spring-boot')
                  implementation("org.apache.commons:commons-lang3")
                  implementation("org.openrewrite:rewrite-core")
                  implementation("javax.validation:validation-api")
                  implementation("org.openrewrite.recipe:rewrite-quarkus")
              }
              
              dependencyManagement {
                  dependencies {
                      dependency("javax.validation:validation-api:2.0.1.Final")
                      dependencySet('org.openrewrite:8.62.2') {
                          entry 'rewrite-core'
                      }
                  }
                  imports {
                      mavenBom "org.openrewrite.recipe:rewrite-recipe-bom:3.14.1"
                  }
              }
              """
          )
        );
    }

    @Test
    void complexExample() {
        rewriteRun(
          spec -> spec.recipe(new RemoveRedundantDependencyVersions(null, null, RemoveRedundantDependencyVersions.Comparator.GTE)),
          buildGradle(
            """
            group = 'org.openrewrite'
            
            buildscript {
                ext {
                    springBootVersion = '3.5.0'
                }
                repositories {
                    mavenCentral()
                }
                dependencies {
                    classpath("org.springframework.boot:spring-boot-gradle-plugin:${springBootVersion}")
                }
            }
            
            apply plugin: 'java'
            apply plugin: 'jacoco'
            apply plugin: 'maven-publish'
            apply plugin: 'org.springframework.boot'
            apply plugin: 'io.spring.dependency-management'
            
            repositories {
                mavenCentral()
            }
            
            test {
                useJUnitPlatform()
            }
            
            dependencies {
                // Core dependencies
                implementation('org.springframework.boot:spring-boot-configuration-processor')
                implementation('org.springframework.boot:spring-boot-starter-webflux')
                implementation('org.springframework.boot:spring-boot-starter-actuator')
                implementation('org.springframework.boot:spring-boot')
                implementation('org.springframework.boot:spring-boot-autoconfigure')
                implementation('org.springframework.boot:spring-boot-starter-logging')
                implementation('org.springframework.cloud:spring-cloud-starter-gateway-server-webflux:4.3.0')
                implementation('io.projectreactor.netty:reactor-netty-http:1.1.13')    //CVE-2023-34062
                implementation('io.projectreactor:reactor-core')
                implementation('org.springframework:spring-web:6.2.8') //CVE-2024-38809
                implementation('org.springframework:spring-context')
                implementation('org.springframework:spring-test')
                implementation('org.springframework.security:spring-security-crypto:6.3.9') //CVE-2024-38827 CVE-2025-22228
            
                // Netty
                implementation('io.netty:netty-codec-http:4.1.108.Final') // CVE-2024-29025
                implementation('io.netty:netty-common:4.1.118.Final') //CVE-2025-25193
                implementation('io.netty:netty-handler:4.1.118.Final') //CVE-2025-24970
            
                // Logback
                implementation('ch.qos.logback:logback-core:1.5.13') //CVE-2024-12798
                implementation('ch.qos.logback:logback-classic:1.5.13') //CVE-2024-12798 //CVE-2024-12801
                implementation('net.logstash.logback:logstash-logback-encoder:8.1')
            
                // Jackson
                implementation('com.fasterxml.jackson.core:jackson-databind:2.19.0')
            
                // Nimbus JOSE JWT
                implementation('com.nimbusds:nimbus-jose-jwt:10.3')
                implementation('com.nimbusds:nimbus-jose-jwt:9.37.2') // CVE-2024-29025
            
                // Jakarta Servlet
                implementation('jakarta.servlet:jakarta.servlet-api')
            
                // Misc
                implementation('org.owasp.encoder:encoder:1.3.1')
                implementation('org.codehaus.janino:janino:3.1.12')
                implementation('commons-validator:commons-validator:1.9.0')
                implementation('commons-beanutils:commons-beanutils:1.11.0') //CVE-2025-48734
                implementation('org.apache.commons:commons-lang3:3.17.0')
                implementation('io.micrometer:context-propagation:1.1.3')
                implementation('org.bouncycastle:bcprov-jdk18on:1.78') //CVE-2024-29857
            
                // Lombok
                implementation('org.projectlombok:lombok:1.18.38')
                compileOnly('org.projectlombok:lombok')
                annotationProcessor('org.projectlombok:lombok:1.18.38')
                annotationProcessor('org.projectlombok:lombok')
            
                // Chaos Monkey
                implementation('de.codecentric:chaos-monkey-spring-boot:3.2.2')
                implementation('org.springframework:spring-aspects')
                implementation('org.springframework.boot:spring-boot-actuator-autoconfigure')
            
                // Test dependencies
                testImplementation('org.springframework.boot:spring-boot-starter-test')
                testImplementation('org.wiremock:wiremock-standalone:3.13.0')
                testImplementation('io.projectreactor:reactor-test:3.7.6')
                testImplementation('org.hamcrest:hamcrest:3.0')
                testImplementation('org.springframework.boot:spring-boot-starter-web')
            }
            """,
            """
            group = 'org.openrewrite'
            
            buildscript {
                ext {
                    springBootVersion = '3.5.0'
                }
                repositories {
                    mavenCentral()
                }
                dependencies {
                    classpath("org.springframework.boot:spring-boot-gradle-plugin:${springBootVersion}")
                }
            }
            
            apply plugin: 'java'
            apply plugin: 'jacoco'
            apply plugin: 'maven-publish'
            apply plugin: 'org.springframework.boot'
            apply plugin: 'io.spring.dependency-management'
            
            repositories {
                mavenCentral()
            }
            
            test {
                useJUnitPlatform()
            }
            
            dependencies {
                // Core dependencies
                implementation('org.springframework.boot:spring-boot-starter-actuator')
                implementation('org.springframework.cloud:spring-cloud-starter-gateway-server-webflux:4.3.0')
                implementation('org.springframework:spring-web:6.2.8') //CVE-2024-38809
            
                // Netty
            
                // Logback
                implementation('net.logstash.logback:logstash-logback-encoder:8.1')
            
                // Jackson
            
                // Nimbus JOSE JWT
                implementation('com.nimbusds:nimbus-jose-jwt:10.3')
            
                // Jakarta Servlet
                implementation('jakarta.servlet:jakarta.servlet-api')
            
                // Misc
                implementation('org.owasp.encoder:encoder:1.3.1')
                implementation('org.codehaus.janino:janino')
                implementation('commons-validator:commons-validator:1.9.0')
                implementation('commons-beanutils:commons-beanutils:1.11.0') //CVE-2025-48734
                implementation('org.apache.commons:commons-lang3')
                implementation('io.micrometer:context-propagation')
            
                // Lombok
                implementation('org.projectlombok:lombok')
                compileOnly('org.projectlombok:lombok')
                annotationProcessor('org.projectlombok:lombok')
            
                // Chaos Monkey
                implementation('de.codecentric:chaos-monkey-spring-boot:3.2.2')
            
                // Test dependencies
                testImplementation('org.springframework.boot:spring-boot-starter-test')
                testImplementation('org.wiremock:wiremock-standalone:3.13.0')
                testImplementation('io.projectreactor:reactor-test')
                testImplementation('org.springframework.boot:spring-boot-starter-web')
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
