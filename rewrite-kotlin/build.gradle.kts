import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.openrewrite.build.language-library")
    kotlin("jvm") version "2.2.21"
}

val kotlinVersion = "2.2.0"

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
    testRuntimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    testRuntimeOnly("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.0")
}

recipeDependencies {
    // Kotlin libraries with @Deprecated(replaceWith=ReplaceWith(...)) annotations
    // Use the JVM variant artifact names since Kotlin multiplatform resolves to these
    testParserClasspath("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.2")
    testParserClasspath("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.8.0")
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

tasks.withType<Test> {
    maxHeapSize = "6g"
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(21)
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
