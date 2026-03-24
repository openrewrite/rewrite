@file:Suppress("UnstableApiUsage")

import com.gradle.develocity.agent.gradle.test.ImportJUnitXmlReports
import com.gradle.develocity.agent.gradle.test.JUnitXmlDialect
import com.hierynomus.gradle.license.tasks.LicenseCheck
import com.hierynomus.gradle.license.tasks.LicenseFormat
import nl.javadude.gradle.plugins.license.LicenseExtension
import java.time.Instant
import java.time.ZoneOffset
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

    inputs.files(fileTree(csharpDir.resolve("OpenRewrite")) { exclude("**/bin/**", "**/obj/**") })
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(fileTree(csharpDir.resolve("OpenRewrite.Tool")) { exclude("**/bin/**", "**/obj/**") })
        .withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(csharpDir.resolve("OpenRewrite/bin"))
    outputs.dir(csharpDir.resolve("OpenRewrite.Tool/bin"))

    doFirst {
        logger.lifecycle("Building C# projects in ${csharpDir}")
    }
}

val junitXmlFile = csharpDir.resolve("build/test-results/xunit/junit.xml")

val csharpTest by tasks.registering(Exec::class) {
    group = "csharp"
    description = "Run C# xunit tests"
    dependsOn(csharpBuild)

    workingDir = csharpDir
    commandLine(
        findDotnet(), "test", "--no-build", "--verbosity", "normal",
        "--logger", "junit;LogFilePath=${junitXmlFile.absolutePath}"
    )

    inputs.files(fileTree(csharpDir.resolve("OpenRewrite")) { exclude("**/bin/**", "**/obj/**") })
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(fileTree(csharpDir.resolve("OpenRewrite.Tool")) { exclude("**/bin/**", "**/obj/**") })
        .withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.files(junitXmlFile)
    outputs.cacheIf { true }

    doFirst {
        logger.lifecycle("Running C# tests in ${csharpDir}")
    }
}

ImportJUnitXmlReports.register(tasks, csharpTest, JUnitXmlDialect.GENERIC)

tasks.named("check") {
    dependsOn(csharpTest)
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
        if (!project.hasProperty("includeSlow")) {
            excludeTags("slow")
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
// CI builds use timestamped pre-release: 8.73.0-snapshot.20260110143252
// Local builds use stable suffix that sorts higher: 8.73.0-zlocal
// Releases use clean version: 8.73.0
val nugetVersion: String = if (System.getenv("CI") != null) {
    project.version.toString().replace(
        "-SNAPSHOT",
        "-snapshot.${Instant.now().atZone(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))}"
    )
} else {
    project.version.toString().replace("-SNAPSHOT", "-zlocal")
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

    // Track only source files — exclude bin/obj build outputs to avoid stale up-to-date checks
    inputs.files(fileTree(csharpDir.resolve("OpenRewrite")) { exclude("**/bin/**", "**/obj/**") })
    inputs.files(fileTree(csharpDir.resolve("OpenRewrite.Tool")) { exclude("**/bin/**", "**/obj/**") })
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
    description = "Pack and install C# NuGet packages into local NuGet cache"

    dependsOn(csharpPack)

    doLast {
        val localFeed = file("${System.getProperty("user.home")}/.nuget/local-feed")
        localFeed.mkdirs()

        val dotnet = findDotnet()
        val distDir = csharpDir.resolve("dist").absolutePath

        fun run(vararg args: String, dir: File? = null, ignoreExitCode: Boolean = false): String {
            val pb = ProcessBuilder(*args)
                .redirectErrorStream(true)
            if (dir != null) pb.directory(dir)
            val proc = pb.start()
            val output = proc.inputStream.bufferedReader().readText()
            val exitCode = proc.waitFor()
            if (exitCode != 0 && !ignoreExitCode) {
                throw GradleException("Command failed (exit $exitCode): ${args.joinToString(" ")}\n$output")
            }
            return output
        }

        // Find NuGet global-packages cache path
        val localsOutput = run(dotnet, "nuget", "locals", "global-packages", "--list")
        val cachePath = localsOutput.trim().substringAfter("global-packages: ")

        // Clear stale entries from NuGet caches for each package
        csharpDir.resolve("dist").listFiles()
            ?.filter { it.name.endsWith(".nupkg") }
            ?.forEach { nupkg ->
                val nameWithoutExt = nupkg.name.removeSuffix(".nupkg")
                val packageId = nameWithoutExt.removeSuffix(".$nugetVersion")

                // Clear the specific version from global packages cache
                val packageCacheDir = file("$cachePath/${packageId.lowercase()}/$nugetVersion")
                if (packageCacheDir.exists()) {
                    logger.lifecycle("Clearing cached: ${packageCacheDir.absolutePath}")
                    packageCacheDir.deleteRecursively()
                }

                nupkg.copyTo(localFeed.resolve(nupkg.name), overwrite = true)
                logger.lifecycle("Published $packageId@$nugetVersion to ${localFeed.absolutePath}")
            }

        // Create a temp project with PackageDownload entries and restore to populate the NuGet cache
        val tempDir = temporaryDir
        tempDir.deleteRecursively()
        tempDir.mkdirs()

        val packageDownloads = csharpDir.resolve("dist").listFiles()
            ?.filter { it.name.endsWith(".nupkg") }
            ?.joinToString("\n") { nupkg ->
                val nameWithoutExt = nupkg.name.removeSuffix(".nupkg")
                val packageId = nameWithoutExt.removeSuffix(".$nugetVersion")
                logger.lifecycle("Installing $packageId@$nugetVersion into NuGet cache from $distDir")
                """    <PackageDownload Include="$packageId" Version="[$nugetVersion]" />"""
            } ?: ""

        val tempCsproj = tempDir.resolve("Temp.csproj")
        tempCsproj.writeText("""
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net10.0</TargetFramework>
              </PropertyGroup>
              <ItemGroup>
            $packageDownloads
              </ItemGroup>
            </Project>
        """.trimIndent())

        val restoreOutput = run(
            dotnet, "restore",
            "--source", distDir,
            "--force",
            dir = tempDir,
            ignoreExitCode = true
        )
        logger.lifecycle(restoreOutput)
    }
}

// Wire publishToMavenLocal to also publish the NuGet packages locally
tasks.named("publishToMavenLocal") {
    dependsOn(csharpPublishLocal)
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
    source = fileTree(csharpDir) { include("**/*.cs"); exclude("**/obj/**", "**/bin/**") }
    header = file("${rootProject.projectDir}/gradle/msalLicenseHeader.txt")
    mapping("cs", "SLASHSTAR_STYLE")
    ext["year"] = Calendar.getInstance().get(Calendar.YEAR)
}

val licenseFormatCsharp by tasks.registering(LicenseFormat::class) {
    group = "license"
    description = "Apply license headers to C# files"
    source = fileTree(csharpDir) { include("**/*.cs"); exclude("**/obj/**", "**/bin/**") }
    header = file("${rootProject.projectDir}/gradle/msalLicenseHeader.txt")
    mapping("cs", "SLASHSTAR_STYLE")
    ext["year"] = Calendar.getInstance().get(Calendar.YEAR)
}

tasks.named("licenseMain") { dependsOn(licenseCsharp) }
tasks.named("licenseFormatMain") { dependsOn(licenseFormatCsharp) }
