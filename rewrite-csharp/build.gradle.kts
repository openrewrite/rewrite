@file:Suppress("UnstableApiUsage")

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    id("org.openrewrite.build.language-library")
    id("org.openrewrite.build.moderne-source-available-license")
    id("jvm-test-suite")
    id("publishing")
}

dependencies {
    api(project(":rewrite-core"))
    api(project(":rewrite-java"))

    api("org.jetbrains:annotations:latest.release")
    api("com.fasterxml.jackson.core:jackson-annotations")

    implementation("io.moderne:jsonrpc:latest.integration")

    compileOnly(project(":rewrite-test"))
    compileOnly(project(":rewrite-xml"))

    testImplementation(project(":rewrite-test"))
    testImplementation(project(":rewrite-xml"))
    testImplementation("io.moderne:jsonrpc:latest.integration")
    testRuntimeOnly(project(":rewrite-java-21"))
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
    exclude("**/Cs.java")
}

// C#-specific build tasks
val csharpDir = projectDir.resolve("csharp")

// Find dotnet executable
fun findDotnet(): String {
    val candidates = listOf("dotnet")
    for (cmd in candidates) {
        try {
            val process = ProcessBuilder(cmd, "--version")
                .redirectErrorStream(true)
                .start()
            if (process.waitFor() == 0) {
                return cmd
            }
        } catch (e: Exception) {
            // Command not found, try next
        }
    }
    throw GradleException(".NET SDK not found. Please install .NET 8.0+ SDK and ensure 'dotnet' is on your PATH.")
}

val csharpBuild by tasks.registering(Exec::class) {
    group = "csharp"
    description = "Build C# projects"

    workingDir = csharpDir
    commandLine(findDotnet(), "build")

    doFirst {
        logger.lifecycle("Building C# projects in ${csharpDir}")
    }
}

testing {
    suites {
        register<JvmTestSuite>("integTest") {
            useJUnitJupiter()

            dependencies {
                implementation(project())
                implementation(project(":rewrite-java-21"))
                implementation(project(":rewrite-test"))
                implementation("org.assertj:assertj-core:latest.release")
                implementation("org.junit.platform:junit-platform-suite-api")
                runtimeOnly("org.junit.platform:junit-platform-suite-engine")
            }
        }
    }
}

// Run tests serially to avoid issues with concurrent C# RPC processes
tasks.withType<Test> {
    // Ensure C# is built before running tests
    dependsOn(csharpBuild)

    maxParallelForks = 1
    maxHeapSize = "8g"
    // Exclude working-set tests by default:
    //   -PincludeWorkingSet      → include individual solution tests
    //   -PincludeWorkingSetFull  → include the full sweep test too
    useJUnitPlatform {
        if (!project.hasProperty("includeWorkingSet")) {
            excludeTags("workingSet")
        }
        if (!project.hasProperty("includeWorkingSetFull")) {
            excludeTags("workingSet-full")
        }
    }
    // Add timeout to identify hanging tests
    systemProperty("junit.jupiter.execution.timeout.default", "30s")
    // Show test names as they run
    testLogging {
        events("started", "passed", "failed", "skipped")
        showStandardStreams = true
    }
}

// ============================================
// NuGet Publishing Tasks
// ============================================

// Generate a NuGet-compatible version for CI builds
// Snapshots use pre-release suffix: 8.73.0-snapshot.20260110143252
// Releases use clean version: 8.73.0
val nugetVersion: String = if (System.getenv("CI") != null) {
    project.version.toString().replace(
        "-SNAPSHOT",
        "-snapshot.${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))}"
    )
} else {
    project.version.toString().replace("-SNAPSHOT", "-dev")
}

// Task to pack the C# project as a NuGet tool package
// Version is injected via /p:Version so the .csproj is never modified
val csharpPack by tasks.registering(Exec::class) {
    group = "csharp"
    description = "Pack C# project as NuGet package"

    workingDir = csharpDir
    commandLine(
        findDotnet(), "pack",
        "--configuration", "Release",
        "--output", "dist",
        "/p:Version=$nugetVersion"
    )

    inputs.dir(csharpDir.resolve("OpenRewrite"))
    inputs.property("version", nugetVersion)
    outputs.dir(csharpDir.resolve("dist"))

    doFirst {
        csharpDir.resolve("dist").deleteRecursively()
        logger.lifecycle("Packing C# NuGet package (version: $nugetVersion)")
    }
}

// Task to publish NuGet package
val csharpPublish by tasks.registering(Exec::class) {
    group = "csharp"
    description = "Publish C# NuGet package"

    dependsOn(csharpPack)

    workingDir = csharpDir
    commandLine(
        findDotnet(), "nuget", "push",
        "dist/*.nupkg",
        "--source", "https://api.nuget.org/v3/index.json",
        "--api-key", project.findProperty("nugetApiKey")?.toString() ?: ""
    )

    doFirst {
        if (!project.hasProperty("nugetApiKey")) {
            throw GradleException("nugetApiKey property is required for NuGet publishing")
        }
        logger.lifecycle("Publishing C# NuGet package (version: $nugetVersion)")
    }
}

// Wire into the main publish task
tasks.named("publish") {
    dependsOn(csharpPublish)
}
