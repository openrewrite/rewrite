@file:Suppress("UnstableApiUsage")

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
    commandLine(pipExe.absolutePath, "install", "-e", ".")

    // Re-run if pyproject.toml changes
    inputs.file(pythonDir.resolve("pyproject.toml"))
    outputs.file(venvDir.resolve("pyvenv.cfg"))

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
    }
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
