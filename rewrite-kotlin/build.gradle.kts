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

    testImplementation("org.jetbrains.kotlin:kotlin-metadata-jvm:2.1.0") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
    }

    testImplementation("org.junit-pioneer:junit-pioneer:latest.release")
    testImplementation(project(":rewrite-test"))
    testRuntimeOnly(project(":rewrite-java-21"))
    testRuntimeOnly("org.antlr:antlr4-runtime:4.13.2")
    testRuntimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")

    testImplementation("com.github.ajalt.clikt:clikt:3.5.0")
    testImplementation("com.squareup:javapoet:1.13.0")
    testImplementation("com.google.testing.compile:compile-testing:0.+")

    // Kotlin libraries for KotlinDeprecationRecipeGenerator
    // Pin to versions compiled with Kotlin 1.9 metadata (compatible with parser's Kotlin 1.9.25 compiler)
    testRuntimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    testRuntimeOnly("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.3")
}

recipeDependencies {
    // Kotlin libraries with @Deprecated(replaceWith=ReplaceWith(...)) annotations
    // Use the JVM variant artifact names since Kotlin multiplatform resolves to these
    // Pin to versions compiled with Kotlin 1.9 metadata (compatible with parser's Kotlin 1.9.25 compiler)
    testParserClasspath("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.8.1")
    testParserClasspath("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.6.3")
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
    val generateKotlinDeprecatedReplaceWithRecipes by registering(JavaExec::class) {
        group = "generate"
        description = "Generate recipes from Kotlin `@Deprecated` annotations using `ReplaceWith`."
        mainClass = "org.openrewrite.kotlin.replace.KotlinDeprecatedRecipeGenerator"
        classpath = sourceSets.getByName("test").runtimeClasspath
        args(
            "org.jetbrains.kotlinx:kotlinx-coroutines-core",
            "org.jetbrains.kotlinx:kotlinx-serialization-core"
        )
        finalizedBy("licenseFormat")
    }
}
