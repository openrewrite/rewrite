@file:Suppress("UnstableApiUsage")

import com.github.gradle.node.NodeExtension
import com.github.gradle.node.npm.task.NpmTask
import com.gradle.develocity.agent.gradle.test.ImportJUnitXmlReports
import com.gradle.develocity.agent.gradle.test.JUnitXmlDialect
import nl.javadude.gradle.plugins.license.LicenseExtension
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    id("org.openrewrite.build.language-library")
    id("org.openrewrite.build.moderne-source-available-license")
    id("com.github.node-gradle.node") version "latest.release"
    id("jvm-test-suite")
    id("publishing")
}

dependencies {
    api(project(":rewrite-core"))
    api(project(":rewrite-java"))
    api(project(":rewrite-json"))

    api("org.jetbrains:annotations:latest.release")
    api("com.fasterxml.jackson.core:jackson-annotations")

    implementation("io.moderne:jsonrpc:latest.integration")

    compileOnly(project(":rewrite-test"))

    testImplementation(project(":rewrite-test"))
    testImplementation(project(":rewrite-yaml"))
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

// Determine the npm package version. Priority:
// 1. Gradle property `npmVersion` (set by CI workflows to pass the version across jobs)
// 2. version.txt on disk (written by a prior `build` invocation in the same job)
// 3. Generate a fresh timestamped version from project.version
val versionTxt = file("src/main/resources/META-INF/rewrite-javascript-version.txt")
val datedSnapshotVersion: String = if (project.hasProperty("npmVersion")) {
    project.property("npmVersion").toString()
} else if (System.getenv("CI") != null) {
    versionTxt.takeIf { it.exists() }?.readText()?.trim()?.takeIf { it.isNotEmpty() }
        ?: project.version.toString().replace(
            "SNAPSHOT",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        )
} else {
    project.version.toString()
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

    args = listOf("version", "--no-git-tag-version", datedSnapshotVersion)
    workingDir = versionDir
}

val npmInstall = tasks.named("npmInstall")

val npmTest = tasks.register<NpmTask>("npmTest") {
    inputs.files(npmInstall)
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(fileTree("rewrite") {
        include("*.json")
        include("jest.config.js")
    }).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(fileTree("rewrite/src"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(fileTree("rewrite/test"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.files("rewrite/build/test-results/jest/junit.xml")
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
    archiveVersion = datedSnapshotVersion
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

// npm publishing is handled by a dedicated workflow (npm-publish.yml) using OIDC trusted publishing.
// This task is invoked directly by that workflow, not wired into the main publish task.
val npmPublish = tasks.register<NpmTask>("npmPublish") {
    inputs.files(npmPack)
        .withPathSensitivity(PathSensitivity.RELATIVE)

    args = provider { listOf("publish", npmPack.get().archiveFile.get().asFile.absolutePath, "--provenance", "--access", "public") }
    if (!project.hasProperty("releasing")) {
        args.addAll("--tag", "next")
    }

    workingDir.set(file("rewrite"))
}

extensions.configure<LicenseExtension> {
    header = file("${rootProject.projectDir}/gradle/msalLicenseHeader.txt")
    exclude("**/rewrite-javascript-version.txt")
//    includePatterns.addAll(
//        listOf("**/*.ts")
//    )
}
