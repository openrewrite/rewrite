@file:Suppress("UnstableApiUsage")

import com.github.gradle.node.NodeExtension
import com.github.gradle.node.npm.task.NpmInstallTask
import com.github.gradle.node.npm.task.NpmTask
import nl.javadude.gradle.plugins.license.LicenseExtension
import java.nio.file.Files
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

plugins {
    id("org.openrewrite.build.language-library")
    id("org.openrewrite.build.moderne-source-available-license")
    id("com.github.node-gradle.node") version "latest.release"
    id("jvm-test-suite")
    id("publishing")
}

normalization {
    runtimeClasspath {
        ignore("META-INF/rewrite-javascript-version.txt")
    }
}

dependencies {
    api(project(":rewrite-core"))
    api(project(":rewrite-java"))
    api(project(":rewrite-json"))
    implementation(project(":rewrite-yaml"))
    implementation("org.yaml:snakeyaml:latest.release")

    api("org.jetbrains:annotations:latest.release")
    api("com.fasterxml.jackson.core:jackson-annotations")

    implementation("io.moderne:jsonrpc:latest.release")

    compileOnly(project(":rewrite-test"))

    testImplementation(project(":rewrite-test"))
    testImplementation("io.moderne:jsonrpc:latest.integration")
    testRuntimeOnly(project(":rewrite-java-21"))
    // For `:rewrite-javascript:generateTestClasspath` — bundles org.openrewrite.maven.rpc.JavaRewriteRpc,
    // the main class spawned by JavaRpcTestServer in test/rpc/.
    testRuntimeOnly(project(":rewrite-maven"))
}

tasks.withType<Javadoc>().configureEach {
    // generated ANTLR sources violate doclint
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")

    // Items besides JavaParser due to lombok error which looks similar to this:
    //     /openrewrite/rewrite/rewrite-javascript/src/main/java/org/openrewrite/javascript/tree/JS.java:4239: error: cannot find symbol
    // @AllArgsConstructor(onConstructor_=@JsonCreator)
    //                     ^
    //   symbol:   method onConstructor_()
    //   location: @interface AllArgsConstructor
    // 1 error
    exclude("**/JS.java")
}

// Override the defaults because the JavaScript code is one directory down (rewrite/).
extensions.configure<NodeExtension> {
    workDir.set(projectDir.resolve("rewrite"))
    npmWorkDir.set(projectDir.resolve("rewrite"))
    nodeProjectDir.set(projectDir.resolve("rewrite"))
}

// Generate a timestamped version for CI builds, or use the regular version for local development
// Uses git commit timestamp for determinism (same commit always produces same version)
fun gitCommitTimestamp(): String {
    val process = ProcessBuilder("git", "log", "-1", "--format=%ct")
        .directory(rootProject.projectDir)
        .redirectErrorStream(true)
        .start()
    val timestamp = process.inputStream.bufferedReader().readText().trim()
    process.waitFor()
    return Instant.ofEpochSecond(timestamp.toLong())
        .atZone(ZoneOffset.UTC)
        .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
}

// `-PnpmPublishVersion=<v>` (set by the npm-publish workflow on the tag-triggered path)
// pins the version verbatim, so e.g. tag `v8.83.0` publishes `8.83.0`. Otherwise CI builds
// substitute `SNAPSHOT` for the git commit timestamp, and local builds use the raw version.
val datedSnapshotVersion = when {
    project.hasProperty("npmPublishVersion") -> project.property("npmPublishVersion").toString()
    System.getenv("CI") != null -> project.version.toString().replace("SNAPSHOT", gitCommitTimestamp())
    else -> project.version.toString()
}

// Helper function to extract version from the JAR if it exists
fun extractVersionFromJar(): String? {
    val jarTask = tasks.named("jar", Jar::class).get()
    val jarFile = jarTask.archiveFile.get().asFile
    if (!jarFile.exists()) return null

    return zipTree(jarFile).matching {
        include("META-INF/rewrite-javascript-version.txt")
    }.singleFile.readText().trim()
}

val npmVersion = tasks.register<NpmTask>("npmVersion") {
    val versionDir = layout.buildDirectory.file("tmp/npmVersion")
    inputs.file("rewrite/package.json")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(versionDir)

    doFirst {
        copy {
            from("rewrite/package.json")
            into(versionDir)
        }
    }

    // Use version from JAR if available (second Gradle invocation), otherwise use generated version
    val versionToUse = provider { extractVersionFromJar() ?: datedSnapshotVersion }
    args = listOf("version", "--no-git-tag-version", versionToUse.get())
    workingDir = versionDir
}

val npmInstall = tasks.named<NpmInstallTask>("npmInstall") {
    // Every install would otherwise print an audit report, fetched from the registry, and a funding appeal
    args.addAll("--no-audit", "--no-fund")
}

// What npmInstall installed, without the caches vitest and vite keep in node_modules and rewrite on every test run
val nodeModules = fileTree("rewrite/node_modules") { exclude(".vite-temp/**", ".vite/**", ".cache/**") }

// What tsc type-checks and vitest runs, none of it from the Java side
fun Task.typeScriptInputs() {
    dependsOn(npmInstall)
    // Without the configuration cache Gradle runs one task of a project at a time. These wait for compileJava rather
    // than hold it up, since rewrite-kotlin's compilation, and rewrite-gradle's tests after it, need its classes.
    mustRunAfter(tasks.named("compileJava"))
    inputs.files(nodeModules)
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(fileTree("rewrite") {
        include("*.json")
        include("vitest.config.mts")
    }).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(fileTree("rewrite/src"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(fileTree("rewrite/test"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(fileTree("rewrite/fixtures"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

val npmTypecheck = tasks.register<NpmTask>("npmTypecheck") {
    typeScriptInputs()
    // tsc emits nothing, so this file records that the inputs type-checked
    val passed = layout.buildDirectory.file("npmTypecheck/passed")
    outputs.file(passed)
    outputs.cacheIf { true }
    args = listOf("run", "--silent", "typecheck")
    doLast {
        passed.get().asFile.writeText("")
    }
}

// Runs one of the projects vitest.config.mts divides the tests into
fun NpmTask.vitest(vitestProject: String, junitXml: String) {
    typeScriptInputs()
    mustRunAfter(npmTypecheck)
    // test/rpc/server.test.ts, for one, starts the built server
    dependsOn("npmBuild")
    inputs.files(fileTree("src/integTest/resources/printer-parity"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.file("rewrite/$junitXml")
    outputs.cacheIf { true }

    // vitest prints only failing tests and the summary, or with -PverboseTests every test (see vitest.config.mts)
    if (project.hasProperty("verboseTests")) {
        environment.put("VERBOSE_TESTS", "1")
    } else {
        environment.put("QUIET_TESTS", "1")
    }

    // npm exec prints no banner, and unlike `npm run --silent` leaves the loglevel of every npm a test runs alone.
    // --no fails the task when vitest is missing from node_modules, where npm would otherwise download the latest one.
    args = listOf("exec", "--no", "--", "vitest", "run", "--project", vitestProject, "--outputFile.junit=$junitXml")
}

val npmTestTypeScript = tasks.register<NpmTask>("npmTestTypeScript") {
    vitest("typescript", "build/test-results/vitest/junit.xml")
}

// The classpath the tests of the java project start org.openrewrite.maven.rpc.JavaRewriteRpc on, which
// generateTestClasspath writes to test-classpath.txt
val javaRpcClasspath = files(configurations.named("runtimeClasspath"), configurations.named("testRuntimeClasspath"),
    tasks.named("compileJava"), tasks.named("processResources"))

val npmTestJava = tasks.register<NpmTask>("npmTestJava") {
    vitest("java", "build/test-results/vitest-java/junit.xml")
    // Generating the server's classpath here means devs running `./gradlew :rewrite-javascript:npmTest` get these
    // tests for free — devs running `npx vitest` directly still need to run :generateTestClasspath once.
    dependsOn(tasks.named("generateTestClasspath"))
    inputs.files(tasks.named("generateTestClasspath").map { it.outputs.files })
        .withNormalizer(ClasspathNormalizer::class)
    // the tests run against the Java classes that file lists, not only the list itself
    inputs.files(javaRpcClasspath)
        .withNormalizer(ClasspathNormalizer::class)
    // test/javascript/completeness.test.ts compares the TypeScript trees and visitors with the Java ones
    inputs.files(fileTree("src/main/java"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

val npmTest = tasks.register("npmTest") {
    description = "Type-checks the TypeScript and runs its tests."
    dependsOn(npmTypecheck, npmTestTypeScript, npmTestJava)
}

tasks.named("check") {
    dependsOn(npmTest)
}

val npmBuild = tasks.register<NpmTask>("npmBuild") {
    dependsOn(npmInstall)
    inputs.files(nodeModules)
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files("rewrite/package.json", "rewrite/tsconfig.json", "rewrite/tsconfig.build.json")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(fileTree("rewrite/src"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(file("rewrite/dist/"))

    // --silent drops the banner npm prints for each script. The postbuild's npm link sets its own loglevel, so a
    // failure to link still says why.
    args = listOf("run", "--silent", "build")
}

tasks.named("assemble") {
    dependsOn(npmBuild)
}

// The version the jar embeds. On CI it changes with every commit, which would rerun npmBuild if that wrote it.
val generateVersionTxt = tasks.register("generateVersionTxt") {
    val versionTxt = file("src/main/resources/META-INF/rewrite-javascript-version.txt")
    val version = datedSnapshotVersion
    inputs.property("version", version)
    outputs.file(versionTxt)
    doLast {
        versionTxt.writeText(version)
    }
}

// Because each of these sees rewrite-javascript-version.txt as an input
listOf("sourcesJar", "processResources", "licenseMain").forEach {
    tasks.named(it) {
        dependsOn(generateVersionTxt)
    }
}

val npmPack = tasks.register<Tar>("npmPack") {
    from("rewrite/src") {
        into("package/src")
    }
    from(npmBuild) {
        into("package/dist/")
    }
    // Published packages carry the version the jar embeds as dist/rewrite-javascript-version.txt
    from(generateVersionTxt) {
        into("package/dist/")
    }
    from(npmVersion) {
        into("package")
    }
    from("rewrite/README.md") {
        into("package")
    }

    archiveBaseName = "openrewrite-rewrite"
    // Use version from JAR if available (second Gradle invocation), otherwise use generated version
    archiveVersion = provider { extractVersionFromJar() ?: datedSnapshotVersion }.get()
    compression = Compression.GZIP
    archiveExtension = "tgz"
    destinationDirectory = layout.buildDirectory.dir("distributions")
}

val npmFixturesBuild = tasks.register<NpmTask>("npmFixturesBuild") {
    dependsOn(npmInstall)
    inputs.files(nodeModules)
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(file("rewrite/package.json"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(npmBuild)
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(fileTree("rewrite/fixtures"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(file("rewrite/dist-fixtures/"))

    args = listOf("run", "--silent", "build:fixtures")
}

testing {
    suites {
        register<JvmTestSuite>("integTest") {
            useJUnitJupiter()

            targets {
                all {
                    testTask.configure {
                        dependsOn(npmBuild, npmFixturesBuild)
                    }
                }
            }

            dependencies {
                implementation(project())
                implementation(project(":rewrite-java-21"))
                implementation(project(":rewrite-test"))
                implementation(project(":rewrite-json"))
                implementation(project(":rewrite-java-tck"))
                implementation(project(":rewrite-yaml"))
                implementation("org.assertj:assertj-core:latest.release")
                implementation("org.junit.platform:junit-platform-suite-api")
                runtimeOnly("org.junit.platform:junit-platform-suite-engine")
            }
        }
    }
}

// The tarball is built whenever we publish, regardless of where it is going: the npmjs push is not
// a Gradle task at all (see npm-publish.yml) and so has no credential gate to pair this with.
tasks.named("publish") {
    dependsOn(npmPack)
}

// The npmjs push is performed directly by `.github/workflows/npm-publish.yml` (which runs
// `npm publish <tgz>` against its own `npmPack` invocation). The workflow owns version selection
// (via `-PnpmPublishVersion=<v>`), dist-tag selection (`latest` vs `next`), and the
// duplicate-publish guard. The dedicated workflow filename is also what the package's npm Trusted
// Publisher (OIDC) record matches against. CI/release workflows still publish to Sonatype, PyPI,
// NuGet as before.
//
// npm publishing is decoupled from the Maven snapshot publish (it runs in a separate
// `workflow_run` triggered by `ci`, because npm Trusted Publisher binds to a single workflow
// filename), so the `ci` run must hand off the exact version string to publish. That string is NOT
// the Maven snapshot coordinate (`8.86.0-<yyyyMMdd>.<HHmmss>-<buildNumber>`, a deploy-time
// timestamp): it MUST be the value baked into the jar's
// `META-INF/rewrite-javascript-version.txt` (= `datedSnapshotVersion`, a git-commit-time
// timestamp). A downstream consumer reads that embedded value and feeds it verbatim to
// `npx --package=@openrewrite/rewrite@<version>` when spawning the JS RPC server (see
// JavaScriptRewriteRpc). If npm carries the deploy timestamp instead, it diverges from the
// embedded value by however long elapses between commit and deploy, so the consumer asks npm for a
// version that was never published and the RPC process dies on startup.
//
// So we record `datedSnapshotVersion` itself, and assert it matches the jar that was just built.
// The `ci` run uploads the recorded version as an artifact and `npm-publish.yml` pins npm to it.
val recordPublishedSnapshotVersion = tasks.register("recordPublishedSnapshotVersion") {
    description = "Records the npm snapshot version (identical to the jar's embedded version.txt) for npm-publish to mirror."
    val baseVersion = project.version.toString()
    val recordedVersion = datedSnapshotVersion
    val versionFile = layout.buildDirectory.file("npm/publishedVersion.txt")
    onlyIf { baseVersion.endsWith("-SNAPSHOT") }
    doLast {
        // `extractVersionFromJar()` returns null only if the jar is absent; this task is
        // `finalizedBy` the `publish` that builds it, so in practice it is always present here.
        val embedded = extractVersionFromJar()
        check(embedded == null || embedded == recordedVersion) {
            "npm publish version ($recordedVersion) must equal the jar's embedded " +
                    "META-INF/rewrite-javascript-version.txt ($embedded); a downstream consumer derives the " +
                    "`npx --package=@openrewrite/rewrite@<version>` coordinate from the embedded value."
        }
        val out = versionFile.get().asFile
        out.parentFile.mkdirs()
        out.writeText(recordedVersion)
        logger.lifecycle("Recorded published snapshot version for npm: $recordedVersion")
    }
}

tasks.named("publish") {
    finalizedBy(recordPublishedSnapshotVersion)
}

// Null when the property is absent OR blank: the release workflow sets
// ORG_GRADLE_PROJECT_cgpPublishToken unconditionally, so a deleted secret still defines it as "".
fun cgpPublishToken(): String? =
    project.findProperty("cgpPublishToken")?.toString()?.takeIf { it.isNotBlank() }

val npmPublishCgp = tasks.register("npmPublishCgp") {
    group = "javascript"
    description = "Publish the npm tarball to the Code Genome Project"

    dependsOn(npmPack)

    doLast {
        val token = cgpPublishToken()
            ?: throw GradleException("cgpPublishToken property is required for Code Genome Project publishing")
        // The auth key is the registry URL with the scheme stripped, trailing slash included. If
        // the two ever disagree npm quietly sends no credential and the publish 401s.
        val npmrc = temporaryDir.resolve("npmrc")
        npmrc.writeText("//artifacts.codegenomeproject.org/npm/:_authToken=$token\n")

        val tarball = npmPack.get().archiveFile.get().asFile
        // SemVer: a prerelease version goes to `next`, so neither a snapshot nor an -rc. can claim
        // `latest`. Read off the tarball's own version, which is the one being published.
        val version = npmPack.get().archiveVersion.get()
        val distTag = if (version.substringBefore('+').contains('-')) "next" else "latest"

        val builder = ProcessBuilder(
            if (System.getProperty("os.name").lowercase().contains("windows")) "npm.cmd" else "npm",
            "publish", tarball.absolutePath,
            "--registry=https://artifacts.codegenomeproject.org/npm/",
            "--tag", distTag
        ).directory(projectDir).redirectErrorStream(true)
        builder.environment()["npm_config_userconfig"] = npmrc.absolutePath
        val process = builder.start()
        val output = process.inputStream.bufferedReader().readText()
        val exit = process.waitFor()
        logger.lifecycle(output)
        // npm has no --skip-duplicate, and a re-run of the same commit resolves to the same version,
        // so the 409 the repository answers on a version it already holds is tolerated here.
        if (exit != 0 && !output.contains("E409")) {
            throw GradleException("Publishing ${tarball.name} to the Code Genome Project failed (exit $exit)")
        }
        logger.lifecycle("Published ${tarball.name} to the Code Genome Project (dist-tag: $distTag)")
    }
}

if (cgpPublishToken() != null) {
    tasks.named("publish") {
        dependsOn(npmPublishCgp)
    }
}

// ============================================
// JavaScript Test Support Tasks
// ============================================

// Task to generate classpath file for Java RPC server testing (consumed by TS tests
// in rewrite-javascript/rewrite/test/rpc/ that spawn org.openrewrite.maven.rpc.JavaRewriteRpc).
val generateTestClasspath = tasks.register("generateTestClasspath") {
    group = "javascript"
    description = "Generate classpath file for Java RPC server (used by TypeScript tests)"

    val outputFile = projectDir.resolve("rewrite/test-classpath.txt")
    outputs.file(outputFile)

    inputs.files(configurations["runtimeClasspath"])
        .withNormalizer(ClasspathNormalizer::class)
    inputs.files(configurations["testRuntimeClasspath"])
        .withNormalizer(ClasspathNormalizer::class)
    inputs.files(tasks.named("compileJava").map { it.outputs.files })
    inputs.files(tasks.named("processResources").map { it.outputs.files })

    dependsOn(tasks.named("testClasses"))
    dependsOn(tasks.named("jar"))

    doLast {
        outputFile.writeText(javaRpcClasspath.joinToString(File.pathSeparator) { it.absolutePath })
        logger.info("Generated test classpath to ${outputFile.absolutePath}")
    }
}

extensions.configure<LicenseExtension> {
    header = file("${rootProject.projectDir}/gradle/msalLicenseHeader.txt")
    exclude("**/rewrite-javascript-version.txt")
    // Test fixtures without a file extension, which the check has no header style for
    excludes(listOf(
        "**/npm-arborist/*-cases", "**/npm-arborist/*-dumps", "**/npm-arborist/*-shapes", "**/npm-arborist/*-tables",
        "**/semver/fixtures-*", "**/semver/release-differences", "**/bun/hosted-git-info-valid", "**/bun/ini-cases",
        "**/npm-resolver/gh-acme-private", "**/npm-resolver/jsr-*"
    ))
//    includePatterns.addAll(
//        listOf("**/*.ts")
//    )
}

// The lock regeneration test fixtures (recorded package-manager output, ~37000 files including binary tarballs)
// live in openrewrite/rewrite-javascript-lock-fixtures. A change that needs new fixtures moves the pin with it.
// Gradle reruns this task when a fixture goes missing, and the rerun extracts the downloaded archive again.
val lockFixturesCommit = "8533f9639291673cad34703ed76178eb46bc72cf"
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
            uri("https://codeload.github.com/openrewrite/rewrite-javascript-lock-fixtures/tar.gz/$lockFixturesCommit")
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
