import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-base`
    kotlin("jvm")
    id("org.openrewrite.base")
    id("org.gradle.test-retry")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

configurations.all {
    exclude("com.google.errorprone", "*")
    resolutionStrategy.cacheDynamicVersionsFor(0, "seconds")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

tasks.named<JavaCompile>("compileJava") {
    options.release.set(8)
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
    options.isFork = true
}

tasks.named<Test>("test").configure {
    retry {
        maxRetries.set(4)
    }
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
    useJUnitPlatform {
        excludeTags("debug")
    }
    jvmArgs = listOf(
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:+ShowHiddenFrames"
    )
    testLogging {
        showExceptions = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showCauses = true
        showStackTraces = true
    }

    val releasing = project.hasProperty("releasing")
    logger.info("This ${if (releasing) "is" else "is not"} a release build")

    val nightly = System.getenv("GITHUB_WORKFLOW") == "nightly-ci"
    logger.info("This ${if (nightly) "is" else "is not"} a nightly build")
}
