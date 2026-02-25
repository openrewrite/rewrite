@file:Suppress("UnstableApiUsage")

import com.gradle.develocity.agent.gradle.test.ImportJUnitXmlReports
import com.gradle.develocity.agent.gradle.test.JUnitXmlDialect
import nl.javadude.gradle.plugins.license.LicenseExtension
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
    api(project(":rewrite-toml"))

    api("org.jetbrains:annotations:latest.release")
    api("com.fasterxml.jackson.core:jackson-annotations")

    implementation("io.moderne:jsonrpc:latest.integration")
    implementation(project(":rewrite-maven"))

    compileOnly(project(":rewrite-test"))

    testImplementation(project(":rewrite-test"))
    testImplementation("io.moderne:jsonrpc:latest.integration")
    testRuntimeOnly(project(":rewrite-java-21"))
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
    exclude("**/Py.java")
}

// Python-specific build tasks
val pythonDir = projectDir.resolve("rewrite")
val venvDir = pythonDir.resolve(".venv")
val isWindows = System.getProperty("os.name").lowercase().contains("windows")
val pythonExe = if (isWindows) venvDir.resolve("Scripts/python.exe") else venvDir.resolve("bin/python")
val pipExe = if (isWindows) venvDir.resolve("Scripts/pip.exe") else venvDir.resolve("bin/pip")

// Find system Python
fun findPython(): String {
    val candidates = if (isWindows) {
        listOf("python", "python3", "py")
    } else {
        listOf("python3", "python")
    }
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
    throw GradleException("Python 3 not found. Please install Python 3.10+ and ensure it's on your PATH.")
}

val pythonSetupVenv by tasks.registering(Exec::class) {
    group = "python"
    description = "Create Python virtual environment"

    onlyIf { !venvDir.exists() }

    workingDir = pythonDir
    commandLine(findPython(), "-m", "venv", ".venv")

    doFirst {
        logger.lifecycle("Creating Python virtual environment in ${venvDir}")
    }
}

val pythonUpgradePip by tasks.registering(Exec::class) {
    group = "python"
    description = "Upgrade pip in virtual environment"

    dependsOn(pythonSetupVenv)
    onlyIf { venvDir.exists() }

    workingDir = pythonDir
    commandLine(pythonExe.absolutePath, "-m", "pip", "install", "--upgrade", "pip")

    doFirst {
        logger.lifecycle("Upgrading pip in virtual environment")
    }
}

val pythonInstall by tasks.registering(Exec::class) {
    group = "python"
    description = "Install Python package in development mode"

    dependsOn(pythonUpgradePip)

    workingDir = pythonDir
    commandLine(pipExe.absolutePath, "install", "-e", ".[dev]")

    // Re-run if pyproject.toml changes
    inputs.file(pythonDir.resolve("pyproject.toml"))

    doFirst {
        logger.lifecycle("Installing Python package with pip")
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

        register<JvmTestSuite>("py2CompatibilityTest") {
            useJUnitJupiter()

            dependencies {
                implementation(project())
                implementation(project(":rewrite-test"))
                implementation(project(":rewrite-java-21"))
                implementation("org.assertj:assertj-core:latest.release")
                implementation("io.moderne:jsonrpc:latest.integration")
            }

            targets {
                all {
                    testTask.configure {
                        // Include the main test classes so common tests run with the Python 2 parser
                        testClassesDirs += sourceSets["test"].output.classesDirs
                        classpath += sourceSets["test"].runtimeClasspath

                        systemProperty("rewrite.python.version", "2")

                        useJUnitPlatform {
                            excludeTags("python3")
                        }

                        shouldRunAfter(tasks.named("test"))
                    }
                }
            }
        }
    }
}

val pytestTest by tasks.registering(Exec::class) {
    group = "verification"
    description = "Run Python pytest tests"

    dependsOn(pythonInstall)

    workingDir = pythonDir
    commandLine(pythonExe.absolutePath, "-m", "pytest", "tests/", "-v",
        "--junitxml=build/test-results/pytest/junit.xml")

    inputs.dir(pythonDir.resolve("src"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.dir(pythonDir.resolve("tests"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(pythonDir.resolve("pyproject.toml"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.file(pythonDir.resolve("build/test-results/pytest/junit.xml"))
    outputs.cacheIf { true }
}

ImportJUnitXmlReports.register(tasks, pytestTest, JUnitXmlDialect.GENERIC)

tasks.named("check") {
    dependsOn(testing.suites.named("py2CompatibilityTest"))
    dependsOn(pytestTest)
}

// Run tests serially to avoid issues with concurrent Python RPC processes
// The Python RPC server uses ThreadLocal, but test state can interfere
// when multiple tests run rapidly on the same thread
tasks.withType<Test> {
    // Ensure Python venv is set up before running tests
    dependsOn(pythonInstall)

    maxParallelForks = 1
    // Add timeout to identify hanging tests - tests that hang will fail with timeout
    systemProperty("junit.jupiter.execution.timeout.default", "30s")
    // Show test names as they run
    testLogging {
        events("started", "passed", "failed", "skipped")
        showStandardStreams = true
    }
}

// Note: Python IDE support is configured via the standalone module at:
// .idea/modules/rewrite-python-src/rewrite-python-src.iml
// This is separate from Gradle because IntelliJ's Gradle integration doesn't support Python source roots.

// ============================================
// Version Resource (for RPC version pinning)
// ============================================

// Generate a PEP 440 compliant version for CI builds
// Snapshots use .dev suffix: 8.71.0.dev20260112145318
// Releases use clean version: 8.71.0
val pythonVersion: String = if (System.getenv("CI") != null) {
    project.version.toString().replace(
        "-SNAPSHOT",
        ".dev${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))}"
    )
} else {
    project.version.toString().replace("-SNAPSHOT", ".dev0")
}

// Write version.txt resource so PythonRewriteRpc can pin the pip package version
val generateVersionTxt by tasks.registering {
    group = "python"
    description = "Generate META-INF/version.txt for RPC version pinning"

    val versionTxt = file("src/main/resources/META-INF/version.txt")
    inputs.property("version", pythonVersion)
    outputs.file(versionTxt)

    doLast {
        versionTxt.parentFile.mkdirs()
        versionTxt.writeText(pythonVersion)
    }
}

listOf("sourcesJar", "processResources", "licenseMain", "assemble").forEach {
    tasks.named(it) {
        dependsOn(generateVersionTxt)
    }
}

// ============================================
// Python Publishing Tasks (PyPI)
// ============================================

// Task to update version in pyproject.toml
val pythonUpdateVersion by tasks.registering {
    group = "python"
    description = "Update version in pyproject.toml"

    dependsOn(pythonSetupVenv)

    val pyprojectFile = pythonDir.resolve("pyproject.toml")
    inputs.property("version", pythonVersion)
    outputs.file(pyprojectFile)

    doLast {
        val content = pyprojectFile.readText()
        val updated = content.replace(
            Regex("""version\s*=\s*"[^"]*""""),
            """version = "$pythonVersion""""
        )
        pyprojectFile.writeText(updated)
        logger.lifecycle("Updated pyproject.toml version to $pythonVersion")
    }
}

// Task to install build dependencies
val pythonInstallBuildDeps by tasks.registering(Exec::class) {
    group = "python"
    description = "Install Python build and publish dependencies"

    dependsOn(pythonUpgradePip)

    workingDir = pythonDir
    commandLine(pipExe.absolutePath, "install", "build>=1.0.0", "twine>=5.0.0")

    doFirst {
        logger.lifecycle("Installing Python build dependencies (build, twine)")
    }
}

// Task to build Python distribution (wheel + sdist)
val pythonBuild by tasks.registering(Exec::class) {
    group = "python"
    description = "Build Python distribution packages"

    dependsOn(pythonUpdateVersion, pythonInstallBuildDeps)

    workingDir = pythonDir
    commandLine(pythonExe.absolutePath, "-m", "build")

    inputs.dir(pythonDir.resolve("src"))
    inputs.file(pythonDir.resolve("pyproject.toml"))
    outputs.dir(pythonDir.resolve("dist"))

    doFirst {
        // Clean previous builds
        pythonDir.resolve("dist").deleteRecursively()
        logger.lifecycle("Building Python distribution packages")
    }
}

// Task to create .pypirc for authentication
val setupPypirc by tasks.registering {
    group = "python"
    description = "Create .pypirc file for PyPI authentication"

    doLast {
        if (project.hasProperty("pypiToken")) {
            val pypirc = pythonDir.resolve(".pypirc")
            pypirc.writeText("""
                [pypi]
                username = __token__
                password = ${project.property("pypiToken")}
            """.trimIndent())
            logger.lifecycle("Created .pypirc for PyPI authentication")
        } else {
            logger.warn("No pypiToken property found, skipping .pypirc creation")
        }
    }
}

// Task to publish to PyPI
val pythonPublish by tasks.registering(Exec::class) {
    group = "python"
    description = "Publish Python package to PyPI"

    dependsOn(pythonBuild, setupPypirc)

    workingDir = pythonDir
    commandLine(
        pythonExe.absolutePath, "-m", "twine", "upload",
        "--config-file", ".pypirc",
        "dist/*"
    )

    doFirst {
        logger.lifecycle("Publishing Python package to PyPI (version: $pythonVersion)")
    }
}

// Wire into the main publish task
tasks.named("publish") {
    dependsOn(pythonPublish)
}

// ============================================
// Python Test Support Tasks
// ============================================

// Task to generate classpath file for Java RPC server testing
val generateTestClasspath by tasks.registering {
    group = "python"
    description = "Generate classpath file for Java RPC server (used by Python tests)"

    val outputFile = pythonDir.resolve("test-classpath.txt")
    outputs.file(outputFile)

    // Depend on jar tasks to ensure jars exist
    dependsOn(tasks.named("testClasses"))
    dependsOn(tasks.named("jar"))

    doLast {
        // Combine compile and test runtime classpaths to get all dependencies
        val classpath = (
            configurations.getByName("runtimeClasspath").files +
            configurations.getByName("testRuntimeClasspath").files +
            tasks.named("compileJava").get().outputs.files +
            tasks.named("processResources").get().outputs.files
        ).distinctBy { it.absolutePath }
         .joinToString(File.pathSeparator) { it.absolutePath }
        outputFile.writeText(classpath)
        logger.lifecycle("Generated test classpath to ${outputFile.absolutePath}")


    }
}

// Task to print test classpath to stdout (useful for setting env vars)
val printTestClasspath by tasks.registering {
    group = "python"
    description = "Print the test classpath (for use with REWRITE_PYTHON_CLASSPATH env var)"

    dependsOn(tasks.named("testClasses"))

    doLast {
        val classpath = (
            configurations.getByName("runtimeClasspath").files +
            configurations.getByName("testRuntimeClasspath").files +
            tasks.named("compileJava").get().outputs.files +
            tasks.named("processResources").get().outputs.files
        ).distinctBy { it.absolutePath }
         .joinToString(File.pathSeparator) { it.absolutePath }
        println(classpath)
    }
}

extensions.configure<LicenseExtension> {
    exclude("**/version.txt")
}

