import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.openrewrite.build.language-library")
    kotlin("jvm") version "2.2.10"
}

val kotlinVersion = "2.2.10"

dependencies {
    compileOnly(project(":rewrite-core"))
    compileOnly(project(":rewrite-test"))

    implementation(project(":rewrite-java"))

    implementation(platform(kotlin("bom", kotlinVersion)))
    implementation(kotlin("compiler-embeddable"))
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

kotlin {
    compilerOptions {
        jvmTarget.set(if (name.contains("Test")) JvmTarget.JVM_21 else JvmTarget.JVM_1_8)
    }
}
