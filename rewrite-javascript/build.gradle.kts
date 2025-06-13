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

    npmCommand = listOf("test")
}

tasks.named("check") {
    dependsOn(npmTest)
}

val npmVersion = tasks.register<NpmTask>("npmVersion") {
    val datedSnapshotVersion = project.version.toString().replace(
        "SNAPSHOT",
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd.HHmmss"))
    )
    inputs.property("version", datedSnapshotVersion)
    outputs.file(file("package.json"))
    args = listOf("version", datedSnapshotVersion)
}

val npmRunBuild = tasks.register<NpmTask>("npmBuild") {
    inputs.files(npmInstall)
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(fileTree("rewrite") {
        include("*.json")
    }).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(fileTree("rewrite/src"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(npmVersion)
        .withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(file("rewrite/dist/"))
    npmCommand = listOf("run", "build")
}
tasks.named("assemble") {
    dependsOn(npmRunBuild)
}

tasks.named("integrationTest") {
    dependsOn(npmRunBuild)
}

// This task creates a `.npmrc` file with the given token, so that the `npm publish` succeeds
// For local development the user would typically have a `~/.npmrc` file with the token in it
val setupNpmrc = tasks.register("setupNpmrc") {
    inputs.property("nodeAuthToken", project.findProperty("nodeAuthToken"))
    outputs.file(file("rewrite/.npmrc"))
    doLast {
        if (project.hasProperty("nodeAuthToken")) {
            val npmrcFile = file("rewrite/.npmrc")
            npmrcFile.writeText("//registry.npmjs.org/:_authToken=${project.property("nodeAuthToken")}\n")
        }
    }
}

// Implicitly `--tag latest` if not specified
val npmPublish = tasks.register<NpmTask>("npmPublish") {
    if (!project.hasProperty("releasing")) {
        args = listOf("publish", "--tag", "next")
    }
    dependsOn(setupNpmrc, npmRunBuild)
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

val restorePackageJson = tasks.register<RestorePackageJson>("restorePackageJson")

tasks.named("publish") {
    dependsOn(npmPublish)
    finalizedBy(restorePackageJson)
}

extensions.configure<LicenseExtension> {
    header = file("${rootProject.projectDir}/gradle/msalLicenseHeader.txt")
//    includePatterns.addAll(
//        listOf("**/*.ts")
//    )
}
