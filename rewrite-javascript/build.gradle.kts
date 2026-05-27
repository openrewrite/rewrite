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

extensions.configure<LicenseExtension> {
    header = file("${rootProject.projectDir}/gradle/msalLicenseHeader.txt")
    exclude("**/rewrite-javascript-version.txt")
//    includePatterns.addAll(
//        listOf("**/*.ts")
//    )
}
