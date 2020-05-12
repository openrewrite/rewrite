import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import nebula.plugin.contacts.Contact
import nebula.plugin.contacts.ContactsExtension

buildscript {
    repositories {
        jcenter()
        gradlePluginPortal()
    }

    dependencies {
        classpath("io.spring.gradle:spring-release-plugin:0.20.1")

        constraints {
            classpath("org.jfrog.buildinfo:build-info-extractor-gradle:4.13.0") {
                because("Need recent version for Gradle 6+ compatibility")
            }
        }
    }
}

plugins {
    id("io.spring.release") version "0.20.1"
    id("org.jetbrains.kotlin.jvm") version "1.3.71" apply false
}

allprojects {
    apply(plugin = "io.spring.license")

    group = "org.openrewrite"
    description = "Eliminate tech-debt. Automatically."
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "nebula.maven-resolved-dependencies")
    apply(plugin = "io.spring.publishing")

    repositories {
        mavenCentral()
    }

    dependencies {
        "compileOnly"("org.projectlombok:lombok:latest.release")
        "annotationProcessor"("org.projectlombok:lombok:latest.release")

        "testImplementation"("org.junit.jupiter:junit-jupiter-api:latest.release")
        "testImplementation"("org.junit.jupiter:junit-jupiter-params:latest.release")
        "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:latest.release")

        "testImplementation"("org.jetbrains.kotlin:kotlin-reflect")
        "testImplementation"("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

        "testImplementation"("org.assertj:assertj-core:latest.release")

        "testRuntimeOnly"("ch.qos.logback:logback-classic:1.0.13")
    }

    configure<ContactsExtension> {
        val j = Contact("jkschneider@gmail.com")
        j.role("maintainer")
        j.moniker("Jonathan Schneider")

        people["jkschneider@gmail.com"] = j
    }

    tasks.named<Test>("test") {
        useJUnitPlatform()
        jvmArgs = listOf("-XX:+UnlockDiagnosticVMOptions", "-XX:+ShowHiddenFrames")
    }

    tasks.withType(KotlinCompile::class.java).configureEach {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }

    configurations.all {
        resolutionStrategy.cacheDynamicVersionsFor(0, "seconds")
    }

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

defaultTasks("build")
