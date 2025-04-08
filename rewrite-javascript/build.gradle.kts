import com.github.gradle.node.task.NodeTask
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

    compileOnly(project(":rewrite-test"))

    testImplementation(project(":rewrite-test"))
    testImplementation(project(":rewrite-yaml"))
    testImplementation("io.moderne:jsonrpc:latest.release")
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

// ----------- snapshot build ---------------------
// To later install this snapshot: npm install @openrewrite/rewrite@next

val npmVersionNext = tasks.register("npmVersionNext", NodeTask::class) {
    args.set(
        listOf(
            "version",
            project.version.toString().replace(
                "SNAPSHOT",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd.HHmmss"))
            )
        )
    )
}
val npmPublishNext = tasks.register("npmPublishNext", NodeTask::class) {
    args.set(listOf("--tag", "next"))
}

// ----------- release build ---------------------
open class RestorePackageJson : DefaultTask() {
    override fun dependsOn(vararg paths: Any?): Task {
        return super.dependsOn(*paths)
    }

    @TaskAction
    fun restore() {
        val git = Git.open(project.rootDir)
        git.checkout()
            .addPath("${project.projectDir}/package.json")
            .addPath("${project.projectDir}/package-lock.json")
            .call()
    }
}

val restorePackageJson = tasks.register("restorePackageJson", RestorePackageJson::class)
val npmVersion = tasks.register("npmVersion", NodeTask::class) {
    args.set(listOf("version", project.version.toString()))
}
// Implicitly `--tag latest` if not specified
val npmPublish = tasks.named("npm_publish")

// ------------ NPM publish, choosing tag based on release status ------------------------
val npmRunBuild = tasks.named("npm_run_build")
tasks.named("build") {
    dependsOn(npmRunBuild)
}

val npmPublishProcess = tasks.register("npmPublish") {
    dependsOn(tasks.named("build"))
    if (project.hasProperty("releasing")) {
        dependsOn(
            npmVersion,
            npmPublish
        )
    } else {
        dependsOn(
            npmVersionNext,
            npmPublishNext
        )
    }
    dependsOn(restorePackageJson)
}

project.rootProject.tasks.named("final") {
    dependsOn(npmPublishProcess)
}

project.rootProject.tasks.named("snapshot") {
    dependsOn(npmPublishProcess)
}
