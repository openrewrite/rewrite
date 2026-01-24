import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.openrewrite.build.language-library")
    kotlin("jvm") version "2.2.21"
}

val kotlinVersion = "1.9.25"

dependencies {
    compileOnly(project(":rewrite-core"))
    compileOnly(project(":rewrite-test"))

    implementation(project(":rewrite-java"))

    implementation(kotlin("compiler-embeddable", kotlinVersion))
    implementation(kotlin("stdlib", kotlinVersion))

    testImplementation("org.jetbrains.kotlin:kotlin-metadata-jvm:2.1.0")

    testImplementation("org.junit-pioneer:junit-pioneer:latest.release")
    testImplementation(project(":rewrite-test"))
    testRuntimeOnly(project(":rewrite-java-21"))
    testRuntimeOnly("org.antlr:antlr4-runtime:4.13.2")
    testRuntimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")

    testImplementation("com.github.ajalt.clikt:clikt:3.5.0")
    testImplementation("com.squareup:javapoet:1.13.0")
    testImplementation("com.google.testing.compile:compile-testing:0.+")

    // Kotlin libraries for KotlinDeprecationRecipeGenerator
    testRuntimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.+")
    testRuntimeOnly("org.jetbrains.kotlinx:kotlinx-serialization-core:1.+")
}

recipeDependencies {
    // Kotlin libraries with @Deprecated(replaceWith=ReplaceWith(...)) annotations
    testParserClasspath("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.+")
    testParserClasspath("org.jetbrains.kotlinx:kotlinx-serialization-core:1.+")
}


java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        apiVersion = KotlinVersion.KOTLIN_1_9
        languageVersion = KotlinVersion.KOTLIN_1_9
        jvmTarget.set(if (name.contains("Test")) JvmTarget.JVM_21 else JvmTarget.JVM_1_8)
    }
}

tasks {
    val generateKotlinDeprecationRecipes by registering(JavaExec::class) {
        group = "generate"
        description = "Generate recipes from Kotlin @Deprecated annotations with ReplaceWith."
        mainClass = "org.openrewrite.kotlin.replace.KotlinDeprecationRecipeGenerator"
        classpath = sourceSets.getByName("test").runtimeClasspath
        args("arrow-core", "kotlinx-coroutines-core", "kotlinx-serialization-core")
        finalizedBy("licenseFormat")
    }
}