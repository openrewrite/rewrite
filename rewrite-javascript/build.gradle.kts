@file:Suppress("UnstableApiUsage")

import com.github.gradle.node.NodeExtension
import com.github.gradle.node.npm.task.NpmTask
import com.gradle.develocity.agent.gradle.test.ImportJUnitXmlReports
import com.gradle.develocity.agent.gradle.test.JUnitXmlDialect
import nl.javadude.gradle.plugins.license.LicenseExtension
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

    implementation("io.moderne:jsonrpc:latest.integration")

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

val npmInstall = tasks.named("npmInstall")

val npmTest = tasks.register<NpmTask>("npmTest") {
    dependsOn(npmInstall)
    // RPC integration tests under test/rpc/ need the classpath of org.openrewrite.maven.rpc.JavaRewriteRpc.
    // Generating it before npmTest means devs running `./gradlew :rewrite-javascript:test` get the RPC
    // tests for free — devs running `npx vitest` directly still need to run :generateTestClasspath once.
    dependsOn(tasks.named("generateTestClasspath"))
    inputs.files(fileTree("rewrite/node_modules") { exclude(".vite-temp/**", ".vite/**", ".cache/**") })
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(fileTree("rewrite") {
        include("*.json")
        include("vitest.config.mts")
    }).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(fileTree("rewrite/src"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(fileTree("rewrite/test"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(tasks.named("generateTestClasspath").map { it.outputs.files })
        .withNormalizer(ClasspathNormalizer::class)
    outputs.files("rewrite/build/test-results/vitest/junit.xml")
    outputs.cacheIf { true }

    args = listOf("run", "ci:test")
}

ImportJUnitXmlReports.register(tasks, npmTest, JUnitXmlDialect.GENERIC)

tasks.named("check") {
    dependsOn(npmTest)
}

val npmBuild = tasks.register<NpmTask>("npmBuild") {
    inputs.files(npmInstall)
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(file("rewrite/package.json"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(fileTree("rewrite/src"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(file("rewrite/dist/"))

    val versionTxt = file("src/main/resources/META-INF/rewrite-javascript-version.txt")
    outputs.file(versionTxt)
    doLast {
        versionTxt.writeText(datedSnapshotVersion)
    }

    args = listOf("run", "build")
}

// Because each of these sees rewrite-javascript-version.txt as an input
listOf("sourcesJar", "processResources", "licenseMain", "assemble").forEach {
    tasks.named(it) {
        dependsOn(npmBuild)
    }
}

val npmPack = tasks.register<Tar>("npmPack") {
    from("rewrite/src") {
        into("package/src")
    }
    from(npmBuild) {
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
    inputs.files(npmInstall)
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(file("rewrite/package.json"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(npmBuild)
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(fileTree("rewrite/fixtures"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(file("rewrite/dist-fixtures/"))

    args = listOf("run", "build:fixtures")
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

// npm publishing is performed directly by `.github/workflows/npm-publish.yml` (which runs
// `npm publish <tgz>` against the artifact produced by the `npmPack` task above). The workflow
// owns version selection (via `-PnpmPublishVersion=<v>`), dist-tag selection (`latest` vs
// `next`), and the duplicate-publish guard. The dedicated workflow filename is also what the
// package's npm Trusted Publisher (OIDC) record matches against. CI/release workflows still
// publish to Sonatype, PyPI, NuGet as before.
//
// npm publishing is decoupled from the Maven snapshot publish (it runs in a separate
// `workflow_run` triggered by `ci`, because npm Trusted Publisher binds to a single workflow
// filename). To still give every Maven snapshot a 1:1 npm counterpart carrying the identical
// version reference, this task records the exact unique snapshot version that Gradle assigned to
// `org.openrewrite:rewrite-javascript` (e.g. `8.86.0-20260625.164513-55`). Gradle's maven-publish
// computes that version client-side and stages the very `maven-metadata.xml` it uploads under
// `build/tmp/publish<Pub>PublicationTo<Repo>Repository/snapshot-maven-metadata.xml`; we read the
// `<value>` straight from that local file — no network read-back. The `ci` run uploads the
// recorded version as an artifact and `npm-publish.yml` pins the npm version to it.
val recordPublishedSnapshotVersion = tasks.register("recordPublishedSnapshotVersion") {
    description = "Records the unique Sonatype snapshot version of rewrite-javascript for npm-publish to mirror."
    val baseVersion = project.version.toString()
    val buildDirectory = layout.buildDirectory
    val versionFile = buildDirectory.file("npm/publishedVersion.txt")
    onlyIf { baseVersion.endsWith("-SNAPSHOT") }
    doLast {
        val staged = buildDirectory.dir("tmp").get().asFile.walkTopDown()
            .firstOrNull { it.name == "snapshot-maven-metadata.xml" }
        val resolved = staged?.readText()
            ?.let { Regex("<value>([^<]+)</value>").find(it)?.groupValues?.get(1) }
        if (resolved == null) {
            logger.warn("Could not find the staged snapshot metadata under build/tmp; " +
                    "npm-publish will fall back to its default version derivation.")
            return@doLast
        }
        val out = versionFile.get().asFile
        out.parentFile.mkdirs()
        out.writeText(resolved)
        logger.lifecycle("Recorded published snapshot version for npm: $resolved")
    }
}

tasks.named("publish") {
    finalizedBy(recordPublishedSnapshotVersion)
}

// ============================================
// JavaScript Test Support Tasks
// ============================================

// Task to generate classpath file for Java RPC server testing (consumed by TS tests
// in rewrite-javascript/rewrite/test/rpc/ that spawn org.openrewrite.maven.rpc.JavaRewriteRpc).
val generateTestClasspath by tasks.registering {
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

extensions.configure<LicenseExtension> {
    header = file("${rootProject.projectDir}/gradle/msalLicenseHeader.txt")
    exclude("**/rewrite-javascript-version.txt")
//    includePatterns.addAll(
//        listOf("**/*.ts")
//    )
}
