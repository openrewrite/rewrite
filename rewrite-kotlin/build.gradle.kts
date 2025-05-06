import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.openrewrite.build.language-library")
    kotlin("jvm") version "1.9.25"
}

val kotlinVersion = "1.9.25"

dependencies {
    compileOnly(project(":rewrite-core"))
    compileOnly(project(":rewrite-test"))
//    compileOnly("com.google.code.findbugs:jsr305:latest.release")

    implementation(project(":rewrite-java"))

    implementation(platform(kotlin("bom", kotlinVersion)))
    implementation(kotlin("compiler-embeddable"))
//    implementation("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:${kotlinVersion}")

    implementation(kotlin("stdlib"))

    testImplementation("org.assertj:assertj-core:latest.release")
    testImplementation("org.junit.jupiter:junit-jupiter-api:latest.release")
    testImplementation("org.junit.jupiter:junit-jupiter-params:latest.release")
    testImplementation("org.junit-pioneer:junit-pioneer:latest.release")
    testImplementation(project(":rewrite-test"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:latest.release")
    testRuntimeOnly(project(":rewrite-java-17"))
    testRuntimeOnly("org.antlr:antlr4-runtime:4.13.2")

    testImplementation("com.github.ajalt.clikt:clikt:3.5.0")
    testImplementation("com.squareup:javapoet:1.13.0")
    testImplementation("com.google.testing.compile:compile-testing:0.+")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
    }
}
