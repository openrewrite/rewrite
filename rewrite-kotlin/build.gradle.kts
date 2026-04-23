import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.openrewrite.build.language-library")
    kotlin("jvm") version "2.2.21"
}

val kotlinVersion = "2.3.20"

dependencies {
    compileOnly(project(":rewrite-core"))
    compileOnly(project(":rewrite-test"))

    implementation(project(":rewrite-java"))

    implementation(kotlin("compiler-embeddable", kotlinVersion))
    implementation(kotlin("reflect", kotlinVersion))
    implementation(kotlin("stdlib", kotlinVersion))

    testImplementation("org.junit-pioneer:junit-pioneer:latest.release")
    testImplementation(project(":rewrite-test"))
    testRuntimeOnly(project(":rewrite-java-21"))
    testRuntimeOnly("org.antlr:antlr4-runtime:4.13.2")
    testRuntimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")

    testImplementation("com.github.ajalt.clikt:clikt:3.5.0")
    testImplementation("com.squareup:javapoet:1.13.0")
    testImplementation("com.google.testing.compile:compile-testing:0.+")
}

configurations.matching { it.name == "kotlinBouncyCastleConfiguration" }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.bouncycastle") {
            useVersion("1.84")
            because("CVE-2026-3505, CVE-2026-5598, CVE-2026-5588, CVE-2026-0636")
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileTestKotlin") {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}
