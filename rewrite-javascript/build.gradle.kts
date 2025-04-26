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
    testRuntimeOnly(project(":rewrite-java-17"))

    integTestImplementation(project(":rewrite-json"))
}

extensions.configure<NodeExtension> {
    npmWorkDir.set(projectDir.resolve("rewrite"))
}

val npmTest = tasks.named("npm_test")
tasks.check {
    dependsOn(
        tasks.named("npmInstall"),
        npmTest
    )
}

tasks.named("integrationTest") {
    dependsOn(tasks.named("npmInstall"))
}

val npmRunBuild = tasks.named("npm_run_build")
tasks.named("build") {
    dependsOn(npmRunBuild)
}

val npmVersion = tasks.register("npmVersion", NpmTask::class) {
    args.set(
        listOf(
            "version", project.version.toString().replace(
                "SNAPSHOT",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd.HHmmss"))
            )
        )
    )
}

// Implicitly `--tag latest` if not specified
val npmPublish = tasks.named<NpmTask>("npm_publish") {
    if (!project.hasProperty("releasing")) {
        args.set(listOf("--tag", "next"))
    }
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
