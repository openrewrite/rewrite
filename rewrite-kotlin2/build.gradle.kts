import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.openrewrite.build.language-library")
    kotlin("jvm") version "1.9.25"
}

val kotlinVersion = "2.0.0"  // Kotlin 2.0 for K2 compiler support

dependencies {
    compileOnly(project(":rewrite-core"))
    compileOnly(project(":rewrite-test"))

    implementation(project(":rewrite-java"))

    implementation(platform(kotlin("bom", kotlinVersion)))
    implementation(kotlin("compiler-embeddable", kotlinVersion))
    implementation(kotlin("stdlib"))

    testImplementation("org.junit-pioneer:junit-pioneer:latest.release")
    testImplementation(project(":rewrite-test"))
    testRuntimeOnly(project(":rewrite-java-21"))
    testRuntimeOnly("org.antlr:antlr4-runtime:4.13.2")
    testRuntimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")

    testImplementation("com.github.ajalt.clikt:clikt:3.5.0")
    testImplementation("com.squareup:javapoet:1.13.0")
    testImplementation("com.google.testing.compile:compile-testing:0.+")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = if (name.contains("Test")) "21" else "1.8"
}