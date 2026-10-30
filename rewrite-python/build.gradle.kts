@file:Suppress("UnstableApiUsage")

import nl.javadude.gradle.plugins.license.LicenseExtension
import java.nio.file.Files
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

plugins {
    id("org.openrewrite.build.language-library")
    id("org.openrewrite.build.moderne-source-available-license")
    id("jvm-test-suite")
    id("publishing")
}

normalization {
    runtimeClasspath {
        ignore("META-INF/rewrite-python-version.txt")
    }
}

dependencies {
    api(project(":rewrite-core"))
    api(project(":rewrite-java"))
    api(project(":rewrite-toml"))
    implementation(project(":rewrite-json"))

    api("org.jetbrains:annotations:latest.release")
    api("com.fasterxml.jackson.core:jackson-annotations")

    implementation("io.moderne:jsonrpc:latest.release")
    implementation(project(":rewrite-maven"))

    compileOnly(project(":rewrite-test"))

    testImplementation(project(":rewrite-test"))
    testImplementation("io.moderne:jsonrpc:latest.integration")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.+")
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

// The floor the package's requires-python declares. An interpreter below it still
// yields a venv, and the rejection surfaces out of the editable install as a
// requires-python error naming the package rather than the interpreter behind it.
val minimumPython = 3 to 12

// `major to minor` for an interpreter, or null when it cannot be run or does not say.
// stderr is kept off the pipe: an interpreter that greets on it (a deprecation notice,
// sitecustomize output) is still one whose version answer on stdout is good.
fun pythonVersion(exe: String): Pair<Int, Int>? = try {
    val process = ProcessBuilder(exe, "-c", "import sys; print('%d.%d' % sys.version_info[:2])")
        .redirectError(ProcessBuilder.Redirect.DISCARD)
        .start()
    val printed = process.inputStream.bufferedReader().readText().trim()
    if (process.waitFor() != 0) null else printed.split('.').map { it.toInt() }.let { it[0] to it[1] }
} catch (e: Exception) {
    null
}

fun Pair<Int, Int>.meetsMinimum() =
    first > minimumPython.first || (first == minimumPython.first && second >= minimumPython.second)

fun Pair<Int, Int>.display() = "${first}.${second}"

fun findPython(): String {
    val candidates = if (isWindows) {
        listOf("python", "python3", "py")
    } else {
        listOf("python3", "python")
    }
    val rejected = mutableListOf<String>()
    for (cmd in candidates) {
        val version = pythonVersion(cmd) ?: continue
        if (version.meetsMinimum()) {
            return cmd
        }
        rejected.add("$cmd is ${version.display()}")
    }
    val found = if (rejected.isEmpty()) "" else " (found ${rejected.joinToString(", ")})"
    throw GradleException(
        "Python ${minimumPython.display()}+ not found$found. " +
            "Please install Python ${minimumPython.display()} or newer and ensure it's on your PATH."
    )
}

val pythonSetupVenv = tasks.register<Exec>("pythonSetupVenv") {
    group = "python"
    description = "Create Python virtual environment"

    onlyIf { !venvDir.exists() }

    workingDir = pythonDir

    doFirst {
        // Resolved in the task action, not at configuration time, so a project that
        // already has a venv configures on any interpreter.
        commandLine(findPython(), "-m", "venv", ".venv")
        logger.lifecycle("Creating Python virtual environment in ${venvDir}")
    }
}

val pythonUpgradePip = tasks.register<Exec>("pythonUpgradePip") {
    group = "python"
    description = "Upgrade pip in virtual environment"

    dependsOn(pythonSetupVenv)
    onlyIf { venvDir.exists() }

    workingDir = pythonDir
    commandLine(pythonExe.absolutePath, "-m", "pip", "install", "-q", "--disable-pip-version-check", "--upgrade", "pip")

    // Once per venv, as the marker goes with the venv, or with an upgrade that failed
    val upgraded = venvDir.resolve(".pip-upgraded")
    outputs.file(upgraded)

    doFirst {
        upgraded.delete()
        logger.info("Upgrading pip in virtual environment")
    }
    doLast {
        upgraded.writeText("")
    }
}

val pythonInstall = tasks.register<Exec>("pythonInstall") {
    group = "python"
    description = "Install Python package in development mode"

    dependsOn(pythonUpgradePip)

    workingDir = pythonDir
    commandLine(pipExe.absolutePath, "install", "-q", "--disable-pip-version-check", "-e", ".[dev]")

    // The editable install reads the sources in place, so only a change to pyproject.toml, or a missing marker
    // (a new venv, or an install that failed), redoes it
    inputs.file(pythonDir.resolve("pyproject.toml"))
    val installed = venvDir.resolve(".openrewrite-installed")
    outputs.file(installed)

    doFirst {
        installed.delete()
        // An existing venv is reused as-is, so the interpreter inside one is checked
        // here as well as at creation.
        val version = pythonVersion(pythonExe.absolutePath)
        if (version == null || !version.meetsMinimum()) {
            throw GradleException(
                "The virtual environment at $venvDir runs ${version?.display() ?: "an unknown Python"}, " +
                    "below ${minimumPython.display()}. Delete it and re-run to have it rebuilt."
            )
        }
        logger.info("Installing Python package with pip")
    }
    doLast {
        installed.writeText("")
    }
}

testing {
    suites {
        register<JvmTestSuite>("integTest") {
            useJUnitJupiter()

            dependencies {
                implementation(project())
                implementation(project(":rewrite-java-21"))
                implementation(project(":rewrite-json"))
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
                        // The dependency and lock file machinery tested under internal never starts a parser,
                        // so here its tests would only repeat the test task's run of them
                        filter {
                            excludeTestsMatching("org.openrewrite.python.internal.*")
                        }

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

// The classpath of the Java RPC peer that the pytest tests marked `requires_java_rpc` spawn, which
// generateTestClasspath writes to test-classpath.txt
val javaRpcClasspath = files(
    configurations["runtimeClasspath"],
    configurations["testRuntimeClasspath"],
    sourceSets["main"].output
)

// The tests marked `requires_java_rpc` run in a task of their own, the only one whose inputs include the Java side
fun registerPytest(name: String, description: String, markers: String, junitXml: String) =
    tasks.register<Exec>(name) {
        group = "verification"
        this.description = description

        dependsOn(pythonInstall)

        workingDir = pythonDir
        // Use relative path for python executable to avoid absolute paths in cache key
        val relativePythonExe = if (isWindows) ".venv/Scripts/python.exe" else ".venv/bin/python"
        val pytestArgs = mutableListOf(relativePythonExe, "-m", "pytest", "tests/", "-m", markers,
            "--junitxml=$junitXml")
        // -PverboseTests restores per-test output, where otherwise passing tests are only a dot each
        pytestArgs.add(if (project.hasProperty("verboseTests")) "-v" else "-q")
        commandLine(pytestArgs)

        inputs.files(fileTree(pythonDir.resolve("src")) { exclude("**/__pycache__/**") })
            .withPathSensitivity(PathSensitivity.RELATIVE)
        inputs.files(fileTree(pythonDir.resolve("tests")) { exclude("**/__pycache__/**") })
            .withPathSensitivity(PathSensitivity.RELATIVE)
        inputs.file(pythonDir.resolve("pyproject.toml"))
            .withPathSensitivity(PathSensitivity.RELATIVE)
        outputs.file(pythonDir.resolve(junitXml))
        outputs.cacheIf { true }
    }

val pytestTest = registerPytest(
    "pytestTest", "Run the Python pytest tests that need no Java RPC peer",
    "not requires_java_rpc", "build/test-results/pytest/junit.xml"
)

val pytestJavaRpcTest = registerPytest(
    "pytestJavaRpcTest", "Run the Python pytest tests marked requires_java_rpc, which spawn a Java RPC peer",
    "requires_java_rpc", "build/test-results/pytestJavaRpc/junit.xml"
)
pytestJavaRpcTest.configure {
    // The tests find the peer's classpath in test-classpath.txt. Devs invoking pytest directly still need
    // :generateTestClasspath once.
    dependsOn(tasks.named("generateTestClasspath"))
    // The jars rather than test-classpath.txt, which lists their paths, so a Java-side change re-runs the tests
    inputs.files(javaRpcClasspath)
        .withNormalizer(ClasspathNormalizer::class)
}

tasks.named("check") {
    dependsOn(testing.suites.named("py2CompatibilityTest"))
    dependsOn(pytestTest, pytestJavaRpcTest)
}

// Each test JVM starts Python RPC processes of its own, so tests in different forks share no RPC state
tasks.withType<Test> {
    // Ensure Python venv is set up before running tests
    dependsOn(pythonInstall)

    // A fork costs a Python process besides its JVM, so there are at most four
    maxParallelForks = maxParallelForks.coerceAtMost(4)
    // Add timeout to identify hanging tests - tests that hang will fail with timeout
    systemProperty("junit.jupiter.execution.timeout.default", "30s")
    // The Python RPC processes log to a file of this task's own, which each run starts over. Tracing every RPC
    // message there, hundreds of MB a run, is for -PverboseTests.
    val rpcLog = "build/python-rpc-$name.log"
    systemProperty("rewrite.python.rpcLog", rpcLog)
    if (project.hasProperty("verboseTests")) {
        systemProperty("rewrite.python.traceRpcMessages", "true")
    }
    // A listener, as a task action from this script would re-run the tests whenever the script's code changes
    val rpcLogFile = file(rpcLog)
    addTestListener(object : TestListener {
        override fun beforeSuite(suite: TestDescriptor) {
            if (suite.parent == null) {
                rpcLogFile.delete()
            }
        }
    })
    testLogging {
        // -PverboseTests shows every test plus its stdout/stderr (for diagnosing RPC hangs)
        if (project.hasProperty("verboseTests")) {
            events("started", "passed", "failed", "skipped")
            showStandardStreams = true
        } else {
            events("failed")
        }
    }
}

// integTest includes a throughput measurement, which tests in other forks would disturb
tasks.named<Test>("integTest") {
    maxParallelForks = 1
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
// The timestamp is the commit's, so the two Gradle invocations of a CI build, `build` and then
// `snapshot publish`, compute the same version for the JAR and the published pip package. Nothing is
// read back from version.txt: the file is git-ignored, so a working tree kept from one build to the
// next, as a CI worker keeps its checkouts, still holds the version an earlier commit wrote there.
fun gitCommitTimestamp(): String {
    val process = ProcessBuilder("git", "log", "-1", "--format=%ct")
        .directory(rootProject.projectDir)
        .redirectErrorStream(true)
        .start()
    val timestamp = process.inputStream.bufferedReader().readText().trim()
    process.waitFor()
    return Instant.ofEpochSecond(timestamp.toLong())
        .atZone(ZoneOffset.UTC)
        .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
}

val pythonVersion: String = if (System.getenv("CI") != null) {
    project.version.toString().replace("-SNAPSHOT", ".dev${gitCommitTimestamp()}")
} else {
    project.version.toString().replace("-SNAPSHOT", ".dev0")
}

// Write rewrite-python-version.txt resource so PythonRewriteRpc can pin the pip package version
val generateVersionTxt = tasks.register("generateVersionTxt") {
    group = "python"
    description = "Generate META-INF/rewrite-python-version.txt for RPC version pinning"

    val versionTxt = file("src/main/resources/META-INF/rewrite-python-version.txt")
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
val pythonUpdateVersion = tasks.register("pythonUpdateVersion") {
    group = "python"
    description = "Update version in pyproject.toml"

    dependsOn(pythonSetupVenv)

    val pyprojectFile = pythonDir.resolve("pyproject.toml")
    inputs.property("version", pythonVersion)
    outputs.file(pyprojectFile)

    doLast {
        val content = pyprojectFile.readText()
        // The [project] table's own key alone, at a line's start: unanchored, the pattern also matched the end of
        // ruff's target-version and ty's python-version, which the release then set to rewrite's version.
        val updated = content.replace(
            Regex("""(?m)^version\s*=\s*"[^"]*""""),
            """version = "$pythonVersion""""
        )
        pyprojectFile.writeText(updated)
        logger.lifecycle("Updated pyproject.toml version to $pythonVersion")
    }
}

// pythonInstall and the pytest tasks read pyproject.toml, which pythonUpdateVersion rewrites when a publish or a
// release builds the distribution in the same build as the tests: Gradle refuses to run a task that reads another's
// output in no declared order, and they should see the version it sets.
listOf(pythonInstall, pytestTest, pytestJavaRpcTest).forEach { reader ->
    reader.configure { mustRunAfter(pythonUpdateVersion) }
}

// Task to install build dependencies
val pythonInstallBuildDeps = tasks.register<Exec>("pythonInstallBuildDeps") {
    group = "python"
    description = "Install Python build and publish dependencies"

    dependsOn(pythonUpgradePip)

    workingDir = pythonDir
    commandLine(pipExe.absolutePath, "install", "-q", "--disable-pip-version-check", "build>=1.0.0", "twine>=5.0.0")

    // Once per venv and set of requirements, as the marker goes with the venv, or with an install that failed
    val installed = venvDir.resolve(".build-deps-installed")
    outputs.file(installed)

    doFirst {
        installed.delete()
        logger.info("Installing Python build dependencies (build, twine)")
    }
    doLast {
        installed.writeText("")
    }
}

// Task to build Python distribution (wheel + sdist)
val pythonBuild = tasks.register<Exec>("pythonBuild") {
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

// Null when the property is absent OR blank: the release workflow sets ORG_GRADLE_PROJECT_pypiToken
// unconditionally, so a deleted secret still defines it as "" and a hasProperty check would be
// true — the push would then run with no credential and fail at twine.
fun pypiToken(): String? = project.findProperty("pypiToken")?.toString()?.takeIf { it.isNotBlank() }

// Task to create .pypirc for authentication
val setupPypirc = tasks.register("setupPypirc") {
    group = "python"
    description = "Create .pypirc file for PyPI authentication"

    doLast {
        val token = pypiToken()
        if (token != null) {
            val pypirc = pythonDir.resolve(".pypirc")
            pypirc.writeText("""
                [pypi]
                username = __token__
                password = $token
            """.trimIndent())
            logger.lifecycle("Created .pypirc for PyPI authentication")
        } else {
            logger.warn("No pypiToken property found, skipping .pypirc creation")
        }
    }
}

// Task to publish to PyPI
val pythonPublish = tasks.register<Exec>("pythonPublish") {
    group = "python"
    description = "Publish Python package to PyPI"

    dependsOn(pythonBuild, setupPypirc)

    workingDir = pythonDir
    commandLine(
        pythonExe.absolutePath, "-m", "twine", "upload",
        "--config-file", ".pypirc",
        "--skip-existing",
        "dist/*"
    )

    doFirst {
        logger.lifecycle("Publishing Python package to PyPI (version: $pythonVersion)")
    }
}

// Null when the property is absent OR blank, for the same reason pypiToken() is.
fun cgpPublishToken(): String? =
    project.findProperty("cgpPublishToken")?.toString()?.takeIf { it.isNotBlank() }

// Task to publish to the Code Genome Project
val pythonPublishCgp = tasks.register("pythonPublishCgp") {
    group = "python"
    description = "Publish Python package to the Code Genome Project"

    dependsOn(pythonBuild)

    doLast {
        val token = cgpPublishToken()
            ?: throw GradleException("cgpPublishToken property is required for Code Genome Project publishing")
        // A named repository rather than --repository-url, which twine resolves *instead of* the
        // config file (utils._config_from_repository_url) and so would force the token onto argv.
        val pypirc = temporaryDir.resolve("pypirc")
        pypirc.writeText("""
            [distutils]
            index-servers = codegenome

            [codegenome]
            repository = https://artifacts.codegenomeproject.org/pypi/
            username = __token__
            password = $token
        """.trimIndent())

        // One twine per file, because twine abandons the run on the first failure and a file that
        // is already published must not stop the others. --skip-existing is not available: twine 7
        // refuses it for any non-PyPI repository before making a request (see
        // Settings.verify_feature_capability), so the 409 a re-run of this build earns is tolerated
        // here instead.
        val alreadyPublished = Regex("""HTTPError: 409\b""")
        val dists = pythonDir.resolve("dist").listFiles()?.filter { it.isFile }?.sorted().orEmpty()
        if (dists.isEmpty()) {
            throw GradleException("No distributions to publish in ${pythonDir.resolve("dist")}")
        }
        for (dist in dists) {
            val builder = ProcessBuilder(
                pythonExe.absolutePath, "-m", "twine", "upload",
                "--config-file", pypirc.absolutePath,
                "--repository", "codegenome",
                dist.absolutePath
            ).directory(pythonDir).redirectErrorStream(true)
            builder.environment()["COLUMNS"] = "200"   // twine word-wraps its errors to this width
            val process = builder.start()
            val output = process.inputStream.bufferedReader().readText()
            val exit = process.waitFor()
            logger.lifecycle(output)
            if (exit != 0 && !alreadyPublished.containsMatchIn(output)) {
                throw GradleException("Publishing ${dist.name} to the Code Genome Project failed (exit $exit)")
            }
        }
        logger.lifecycle("Published ${dists.size} distribution(s) to the Code Genome Project (version: $pythonVersion)")
    }
}

// The distributable is built whenever we publish, regardless of where it is going.
tasks.named("publish") {
    dependsOn(pythonBuild)
}

// Only the pushes are gated, each on its own credential, so retiring a destination is deleting one
// block and one secret.
if (pypiToken() != null) {
    tasks.named("publish") {
        dependsOn(pythonPublish)
    }
}

if (cgpPublishToken() != null) {
    tasks.named("publish") {
        dependsOn(pythonPublishCgp)
    }
}

// ============================================
// Python Test Support Tasks
// ============================================

// Task to generate classpath file for Java RPC server testing
val generateTestClasspath = tasks.register("generateTestClasspath") {
    group = "python"
    description = "Generate classpath file for Java RPC server (used by Python tests)"

    val outputFile = pythonDir.resolve("test-classpath.txt")
    outputs.file(outputFile)

    inputs.files(javaRpcClasspath)
        .withNormalizer(ClasspathNormalizer::class)

    // Depend on jar tasks to ensure jars exist
    dependsOn(tasks.named("testClasses"))
    dependsOn(tasks.named("jar"))

    doLast {
        outputFile.writeText(javaRpcClasspath.asPath)
        logger.info("Generated test classpath to ${outputFile.absolutePath}")
    }
}

// Task to print test classpath to stdout (useful for setting env vars)
val printTestClasspath = tasks.register("printTestClasspath") {
    group = "python"
    description = "Print the test classpath (for use with REWRITE_PYTHON_CLASSPATH env var)"

    dependsOn(tasks.named("testClasses"))

    doLast {
        println(javaRpcClasspath.asPath)
    }
}

extensions.configure<LicenseExtension> {
    exclude("**/rewrite-python-version.txt")
    // uv's own marker test cases, kept as JSON data
    exclude("**/marker/uv-*-tree-rows.json")
    // A package index's and a package's recorded responses, formats with no comment syntax
    exclude("**/index/pep691-listing-json")
    exclude("**/metadata/*-METADATA")
}

// The lock regeneration test fixtures (recorded package-manager output) live in
// openrewrite/rewrite-python-lock-fixtures. A change that needs new fixtures moves the pin with it. Gradle reruns
// this task when a fixture goes missing, and the rerun extracts the downloaded archive again.
val lockFixturesCommit = "c3231458118bb94a0cab845bc39a268b2e93e2a9"
val lockFixturesDir = layout.buildDirectory.dir("lock-fixtures")
val syncLockFixtures = tasks.register("syncLockFixtures") {
    inputs.property("commit", lockFixturesCommit)
    outputs.dir(lockFixturesDir)
    doLast {
        val archiveDir = layout.buildDirectory.dir("tmp/lock-fixtures-archive").get().asFile
        val archive = archiveDir.resolve("$lockFixturesCommit.tar.gz")
        if (!archive.exists()) {
            delete(archiveDir)
            archiveDir.mkdirs()
            val download = archiveDir.resolve("$lockFixturesCommit.tar.gz.part")
            uri("https://codeload.github.com/openrewrite/rewrite-python-lock-fixtures/tar.gz/$lockFixturesCommit")
                .toURL().openStream().use { input -> download.outputStream().use { input.copyTo(it) } }
            Files.move(download.toPath(), archive.toPath())
        }
        delete(lockFixturesDir)
        try {
            copy {
                from(tarTree(resources.gzip(archive))) {
                    include("*/lock/**")
                    eachFile { path = path.substringAfter('/') }
                    includeEmptyDirs = false
                }
                into(lockFixturesDir)
            }
        } catch (e: Exception) {
            archive.delete()
            throw e
        }
    }
}
tasks.named<ProcessResources>("processTestResources") {
    dependsOn(syncLockFixtures)
    from(lockFixturesDir)
}
