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

tasks.withType<Javadoc> {
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

val npmTest = tasks.named("npm_test")
npmTest.configure {
    inputs.files(fileTree("rewrite") {
        include("*.json")
        include("jest.config.js")
    })
    inputs.files(fileTree("rewrite/src"))
    inputs.files(fileTree("rewrite/test"))
    outputs.files("rewrite/build/test-results/jest/junit.xml")

    dependsOn(tasks.named("npmInstall"))
}

tasks.register<Test>("npmTestReporting") {
    description = "Makes Jest test results visible to Develocity"

    // Don't run any JVM tests
    enabled = true
    testClassesDirs = files()
    classpath = files()

    // Configure where to find the Jest test results
    reports.junitXml.outputLocation.set(file("rewrite/build/test-results/jest"))

    // Always run
    outputs.upToDateWhen { false }

    // This runs after npmTest completes
    dependsOn(npmTest)
}

tasks.check {
    dependsOn(
        tasks.named("npmInstall"),
        tasks.named("npmTestReporting"),
    )
}


val npmRunBuild = tasks.named("npm_run_build")
tasks.named("build") {
    dependsOn(npmRunBuild)
}

val npmPack = tasks.register<NpmTask>("npmPack") {
    dependsOn(npmRunBuild, npmVersion)
    finalizedBy(npmResetVersion)

    val tempPackDir = layout.buildDirectory.dir("tmp/npm-pack").get().asFile

    // Delete existing tgz files before packing
    doFirst {
        if (tempPackDir.exists()) {
            tempPackDir.listFiles { _, name -> name.endsWith(".tgz") }?.forEach { file ->
                file.delete()
            }
        }
        tempPackDir.mkdirs()
    }

    args.set(listOf(
        "pack",
        "--pack-destination=${tempPackDir.absolutePath}"
    ))
    workingDir.set(file("rewrite"))

    inputs.files(fileTree("rewrite/dist"))
    inputs.files("rewrite/package.json", "rewrite/package-lock.json")
    outputs.files(fileTree(tempPackDir) {
        include("*.tgz")
    })
}

val npmInitTemp by tasks.registering(NpmTask::class) {
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

val npmInstallTemp by tasks.registering(NpmTask::class) {
    dependsOn(npmInitTemp, npmPack)
    val tempInstallDir = layout.buildDirectory.dir("tmp/npmInstall").get().asFile
    workingDir.set(tempInstallDir)

    // Use a provider to defer evaluation until execution time
    args.set(provider {
        val tgzFile = npmPack.get().outputs.files.singleFile
        listOf("install", tgzFile.absolutePath, "--omit=dev")
    })
}

sourceSets {
    main {
        resources {
            srcDir("src/main/generated-resources")
        }
    }
}

tasks.named<Jar>("sourcesJar") {
    dependsOn("createProductionPackage")
    exclude("production-package.zip")
}

// Creates a production-ready package; writing it to `src/main/generated-resources` so that it will be included by IDEA
val createProductionPackage by tasks.register<Zip>("createProductionPackage") {
    dependsOn(npmInstallTemp)

    // Configure the tar output
    archiveFileName.set("production-package.zip")
    destinationDirectory.set(layout.projectDirectory.dir("src/main/generated-resources"))

    from(layout.buildDirectory.dir("tmp/npmInstall")) {
        // Include everything from the temp install directory
        include("**/*")
    }
}

// Update processResources to depend on the new task instead
tasks.named("processResources") {
    dependsOn(createProductionPackage)
}

// npm pack --pack-destination=out/production rewrite/
// npm install --no-save out/production/openrewrite-rewrite-0.tgz

tasks.named("integrationTest") {
    dependsOn(npmRunBuild)
}

val npmVersion = tasks.register("npmVersion", NpmTask::class) {
    val versionProperty = "npmVersionGenerated"

    args.set(provider {
        // Check if we already generated a version for this build
        val existingVersion = project.extensions.findByName(versionProperty) as String?
        if (existingVersion != null) {
            listOf("version", "--no-git-tag-version", existingVersion)
        } else {
            val generatedVersion = project.version.toString().replace(
                "SNAPSHOT",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
            )
            // Store the generated version for reuse
            project.extensions.add(versionProperty, generatedVersion)
            listOf("version", "--no-git-tag-version", generatedVersion)
        }
    })

    workingDir.set(file("rewrite"))
}

val npmResetVersion = tasks.register<NpmTask>("npmResetVersion") {
    args.set(listOf("version", "0.0.0", "--no-git-tag-version"))
    workingDir.set(file("rewrite"))
}

// This task creates a `.npmrc` file with the given token, so that the `npm publish` succeeds
// For local development the user would typically have a `~/.npmrc` file with the token in it
tasks.register("setupNpmrc") {
    doLast {
        if (project.hasProperty("nodeAuthToken")) {
            val npmrcFile = file("rewrite/.npmrc")
            npmrcFile.writeText("//registry.npmjs.org/:_authToken=${project.property("nodeAuthToken")}\n")
        }
    }
}

// Implicitly `--tag latest` if not specified
val npmPublish = tasks.named<NpmTask>("npm_publish") {
    dependsOn(tasks.named("setupNpmrc"), npmRunBuild, npmVersion)
    finalizedBy(npmResetVersion)

    args.set(provider {
        buildList {
            add("--dry-run")
            if (!project.hasProperty("releasing")) {
                add("--tag")
                add("next")
            }
        }
    })

    workingDir.set(file("rewrite"))
}

open class RestorePackageJson : DefaultTask() {
    override fun dependsOn(vararg paths: Any?): Task {
        return super.dependsOn(*paths)
    }

    @TaskAction
    fun restore() {
        val git = Git.open(project.rootDir)
        git.checkout()
            .addPath("${project.projectDir.relativeTo(project.rootDir).path}/rewrite/package.json")
            .addPath("${project.projectDir.relativeTo(project.rootDir).path}/rewrite/package-lock.json")
            .call()
    }
}

val restorePackageJson = tasks.register("restorePackageJson", RestorePackageJson::class)

// To later install a snapshot: npm install @openrewrite/rewrite@next
val npmPublishProcess = tasks.register("npmPublish") {
    dependsOn(
        tasks.named("build"),
        npmVersion,
        npmPublish,
        restorePackageJson
    )
}

listOf("final", "snapshot").forEach { phase ->
    project.rootProject.tasks.named(phase) {
        dependsOn(npmPublishProcess)
    }
}

extensions.configure<LicenseExtension> {
    header = file("${rootProject.projectDir}/gradle/msalLicenseHeader.txt")
//    includePatterns.addAll(
//        listOf("**/*.ts")
//    )
}
