import com.github.gradle.node.NodeExtension
import com.github.gradle.node.npm.task.NpmTask
import nl.javadude.gradle.plugins.license.LicenseExtension
import org.eclipse.jgit.api.Git
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    id("org.openrewrite.build.language-library")
    id("org.openrewrite.build.moderne-source-available-license")
    id("com.netflix.nebula.integtest-standalone")
    id("com.github.node-gradle.node") version "latest.release"
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
    testImplementation(project(":rewrite-yaml"))
    testImplementation("io.moderne:jsonrpc:latest.integration")
    testRuntimeOnly(project(":rewrite-java-21"))

    integTestImplementation(project(":rewrite-json"))
    integTestImplementation(project(":rewrite-java-tck"))
    integTestImplementation("org.junit.platform:junit-platform-suite-api:latest.release")
    integTestRuntimeOnly("org.junit.platform:junit-platform-suite-engine:latest.release")
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


extensions.configure<NodeExtension> {
    workDir.set(projectDir.resolve("rewrite"))
    npmWorkDir.set(projectDir.resolve("rewrite"))
    nodeProjectDir.set(projectDir.resolve("rewrite"))
}

val datedSnapshotVersion by extra {
    if (System.getenv("CI") != null) {
        project.version.toString().replace(
            "SNAPSHOT",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        )
    } else {
        project.version.toString()
    }
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

    args = listOf("run", "ci:test")
}

tasks.named("check") {
    dependsOn(npmTest)
}


val npmRunBuild = tasks.register<NpmTask>("npmBuild") {
    inputs.files(npmInstall)
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(file("rewrite/package.json"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(fileTree("rewrite/src"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(file("rewrite/dist/src/"))

    args = listOf("run", "build")
}
tasks.named("assemble") {
    dependsOn(npmRunBuild)
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

val npmPack = tasks.register<Tar>("npmPack") {
    from(npmRunBuild) {
        into("package/dist/src/")
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

val npmInitTemp = tasks.register<NpmTask>("npmInitTemp") {
    val tempInstallDir = layout.buildDirectory.dir("tmp/npmInstall").get().asFile
    args.set(listOf("init", "-y"))
    workingDir.set(tempInstallDir)

    doFirst {
        if (tempInstallDir.exists()) {
            tempInstallDir.deleteRecursively()
        }
        tempInstallDir.mkdirs()
    }
}

val npmInstallTemp = tasks.register<NpmTask>("npmInstallTemp") {
    val tempInstallDir = layout.buildDirectory.file("tmp/npmInstall")
    inputs.files(npmPack)
        .withPathSensitivity(PathSensitivity.RELATIVE)
    dependsOn(npmInitTemp)

    // Use a provider to defer evaluation until execution time
    args = provider { listOf("install", npmPack.get().archiveFile.get().asFile.absolutePath, "--omit=dev") }
    workingDir.set(tempInstallDir)
}

sourceSets {
    main {
        resources {
            srcDir("src/main/generated-resources")
        }
    }
}

afterEvaluate {
    tasks.named("licenseMain") {
        dependsOn(createProductionPackage)
    }
}

tasks.named<Jar>("sourcesJar") {
    dependsOn("createProductionPackage")
    exclude("production-package.zip")
}

tasks.named("processResources") {
    dependsOn(createProductionPackage)
}

// Creates a production-ready package; writing it to `src/main/generated-resources` so that it will be included by IDEA
val createProductionPackage by tasks.register<Zip>("createProductionPackage") {
    // Configure the tar output
    archiveFileName.set("production-package.zip")
    destinationDirectory.set(layout.projectDirectory.dir("src/main/generated-resources"))

    dependsOn(npmInstallTemp)

    // Reference the actual directory where npm packages are installed
    from(layout.buildDirectory.dir("tmp/npmInstall")) {
        // Optionally exclude unnecessary files like package-lock.json, etc.
        // exclude("package-lock.json", ".npmrc")
    }
}

// Include production-ready package into jar
tasks.named<Jar>("jar") {
    from(createProductionPackage)
}

tasks.named("integrationTest") {
    dependsOn(npmRunBuild)
}

// This task creates a `.npmrc` file with the given token, so that the `npm publish` succeeds
// For local development the user would typically have a `~/.npmrc` file with the token in it
val setupNpmrc = tasks.register("setupNpmrc") {
    doLast {
        if (project.hasProperty("nodeAuthToken")) {
            val npmrcFile = file("rewrite/.npmrc")
            npmrcFile.writeText("//registry.npmjs.org/:_authToken=${project.property("nodeAuthToken")}\n")
        }
    }
}

// Implicitly `--tag latest` if not specified
val npmPublish = tasks.register<NpmTask>("npmPublish") {
    inputs.files(npmPack)
        .withPathSensitivity(PathSensitivity.RELATIVE)
    dependsOn(setupNpmrc)

    args = provider { listOf("publish", npmPack.get().archiveFile.get().asFile.absolutePath) }
    if (!project.hasProperty("releasing")) {
        args.addAll("--tag", "next")
    }

    workingDir.set(file("rewrite"))
}

tasks.named("publish") {
    dependsOn(npmPublish)
}

extensions.configure<LicenseExtension> {
    header = file("${rootProject.projectDir}/gradle/msalLicenseHeader.txt")
//    includePatterns.addAll(
//        listOf("**/*.ts")
//    )
}
