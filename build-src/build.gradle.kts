plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(platform(kotlin("bom")))
    implementation(kotlin("reflect"))
    implementation(kotlin("script-runtime"))
    implementation(kotlin("gradle-plugin"))
    implementation("org.gradle:test-retry-gradle-plugin:1.2.1")
    implementation("com.gradle:gradle-enterprise-gradle-plugin:3.10.1")
    implementation("org.owasp:dependency-check-gradle:latest.release")
    implementation("gradle.plugin.com.hierynomus.gradle.plugins:license-gradle-plugin:0.16.1")
    implementation("com.github.jk1:gradle-license-report:2.0")
    implementation("com.netflix.nebula:gradle-contacts-plugin:6.0.0")
    implementation("com.netflix.nebula:gradle-info-plugin:11.3.3")
    implementation("com.netflix.nebula:nebula-release-plugin:16.0.0")
    implementation("com.netflix.nebula:nebula-publishing-plugin:18.4.0")
    implementation("com.netflix.nebula:nebula-project-plugin:9.6.3")
    implementation("io.github.gradle-nexus:publish-plugin:1.0.0")
    implementation("gradle.plugin.com.github.johnrengelman:shadow:7.1.2")
    implementation("org.openrewrite:plugin:latest.release")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

kotlin {
    jvmToolchain {
        this as JavaToolchainSpec
        languageVersion.set(JavaLanguageVersion.of("11"))
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "11"
    }
}

tasks.named<Test>("test").configure {
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
    useJUnitPlatform()
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
}
