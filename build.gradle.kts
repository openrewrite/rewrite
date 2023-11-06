import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.openrewrite.build.recipe-library") version "latest.release"
    kotlin("jvm") version "1.9.0"
}
group = "org.openrewrite"
description = "Rewrite Kotlin"

val latest = if (project.hasProperty("releasing")) {
    "latest.release"
} else {
    "latest.integration"
}

val kotlinVersion = "1.9.0"

dependencies {
    annotationProcessor("org.projectlombok:lombok:latest.release")

    compileOnly("org.openrewrite:rewrite-core")
    compileOnly("org.openrewrite:rewrite-test")
    compileOnly("org.projectlombok:lombok:latest.release")
    compileOnly("com.google.code.findbugs:jsr305:latest.release")

    implementation(platform("org.openrewrite:rewrite-bom:${latest}"))
    implementation("org.openrewrite:rewrite-java")

    implementation(platform(kotlin("bom", kotlinVersion)))
    implementation(kotlin("compiler-embeddable"))
//    implementation("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:${kotlinVersion}")

    implementation(kotlin("stdlib"))

    testImplementation("org.assertj:assertj-core:latest.release")
    testImplementation("org.junit.jupiter:junit-jupiter-api:latest.release")
    testImplementation("org.junit.jupiter:junit-jupiter-params:latest.release")
    testImplementation("org.junit-pioneer:junit-pioneer:2.0.0")
    testImplementation("org.openrewrite:rewrite-test")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:latest.release")
    testRuntimeOnly("org.openrewrite:rewrite-java-17")
    testRuntimeOnly("com.squareup.misk:misk-prometheus:latest.release")
    testRuntimeOnly("com.squareup.misk:misk-metrics:latest.release")

    testImplementation("com.github.ajalt.clikt:clikt:3.5.0")
    testImplementation("com.squareup:javapoet:1.13.0")
    testImplementation("com.google.testing.compile:compile-testing:0.+")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
