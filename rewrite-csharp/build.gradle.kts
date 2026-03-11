@file:Suppress("UnstableApiUsage")

import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat
import nl.javadude.gradle.plugins.license.LicenseExtension
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

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
    throw GradleException(".NET SDK not found. Please install .NET 10.0+ SDK and ensure 'dotnet' is on your PATH.")
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
// NuGet Version & Version File
// ============================================

// Generate a NuGet-compatible version
// Snapshots use pre-release suffix with timestamp: 8.73.0-snapshot.20260110143252
// Releases use clean version: 8.73.0
// Read from version.txt first (written by a prior `build` invocation) to ensure the
// version published to NuGet matches what's baked into the JAR.
val nugetVersionTxt = file("src/main/resources/META-INF/rewrite-csharp-version.txt")
val nugetVersion: String = if (System.getenv("CI") != null) {
    nugetVersionTxt.takeIf { it.exists() }?.readText()?.trim()?.takeIf { it.isNotEmpty() }
        ?: project.version.toString().replace(
            "-SNAPSHOT",
            "-snapshot.${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))}"
        )
} else {
    project.version.toString().replace(
        "-SNAPSHOT",
        "-snapshot.${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))}"
    )
}

val generateVersionTxt by tasks.registering {
    group = "csharp"
    description = "Generate META-INF/rewrite-csharp-version.txt for RPC version pinning"

    val versionTxt = file("src/main/resources/META-INF/rewrite-csharp-version.txt")
    inputs.property("version", nugetVersion)
    outputs.file(versionTxt)

    doLast {
        versionTxt.parentFile.mkdirs()
        versionTxt.writeText(nugetVersion)
    }
}

listOf("sourcesJar", "processResources", "licenseMain", "assemble").forEach {
    tasks.named(it) {
        dependsOn(generateVersionTxt)
    }
}

// ============================================
// NuGet Publishing Tasks
// ============================================

// Task to pack C# projects as NuGet packages
// Packs both the SDK library and the tool; version is injected via /p:Version
val csharpPack by tasks.registering(Exec::class) {
    group = "csharp"
    description = "Pack C# projects as NuGet packages"

    workingDir = csharpDir
    commandLine(
        findDotnet(), "pack",
        "--configuration", "Release",
        "--output", "dist",
        "/p:Version=$nugetVersion"
    )

    inputs.dir(csharpDir.resolve("OpenRewrite"))
    inputs.dir(csharpDir.resolve("OpenRewrite.Tool"))
    inputs.property("version", nugetVersion)
    outputs.dir(csharpDir.resolve("dist"))

    doFirst {
        csharpDir.resolve("dist").deleteRecursively()
        logger.lifecycle("Packing C# NuGet packages (version: $nugetVersion)")
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

// Task to publish the C# SDK library to a local NuGet feed for cross-repo development.
// Usage: ./gradlew :rewrite-csharp:csharpPublishLocal
val csharpPublishLocal by tasks.registering {
    group = "csharp"
    description = "Pack and publish C# SDK library to local NuGet feed (~/.nuget/local-feed/)"

    dependsOn(csharpPack)

    val localFeed = file("${System.getProperty("user.home")}/.nuget/local-feed")

    doLast {
        localFeed.mkdirs()
        csharpDir.resolve("dist").listFiles()
            ?.filter { it.name.endsWith(".nupkg") }
            ?.forEach { nupkg ->
                val target = localFeed.resolve(nupkg.name)
                nupkg.copyTo(target, overwrite = true)
                logger.lifecycle("Published ${nupkg.name} to ${localFeed.absolutePath}")
            }
    }
}

// Wire into the main publish task only when the NuGet API key is available
if (project.hasProperty("nugetApiKey")) {
    tasks.named("publish") {
        dependsOn(csharpPublish)
    }
}

// ============================================
// License Header Configuration
// ============================================

extensions.configure<LicenseExtension> {
    header = file("${rootProject.projectDir}/gradle/msalLicenseHeader.txt")
}

val licenseCsharp by tasks.registering(LicenseCheck::class) {
    group = "license"
    description = "Check license headers on C# files"
    source = fileTree(csharpDir) { include("**/*.cs") }
    header = file("${rootProject.projectDir}/gradle/msalLicenseHeader.txt")
    mapping("cs", "SLASHSTAR_STYLE")
    ext["year"] = Calendar.getInstance().get(Calendar.YEAR)
}

val licenseFormatCsharp by tasks.registering(LicenseFormat::class) {
    group = "license"
    description = "Apply license headers to C# files"
    source = fileTree(csharpDir) { include("**/*.cs") }
    header = file("${rootProject.projectDir}/gradle/msalLicenseHeader.txt")
    mapping("cs", "SLASHSTAR_STYLE")
    ext["year"] = Calendar.getInstance().get(Calendar.YEAR)
}

tasks.named("licenseMain") { dependsOn(licenseCsharp) }
tasks.named("licenseFormatMain") { dependsOn(licenseFormatCsharp) }
