import org.eclipse.jgit.api.Git

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

//tasks.named("snapshot") {
// npm run build
//}

// snapshot
// npm version ${project.version.replace('SNAPSHOT', System.currentTimeMillis())}
// npm publish --tag next

// to later install this snapshot: npm install @openrewrite/rewrite@next

//tasks.named("final") {
// npm version ${project.version}
// npm publish
// `--tag latest` is implicitly the case if not specified
// git reset package.json package.lock
//}

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

class NpmPublish : DefaultTask() {
    override fun dependsOn(vararg paths: Any?): Task {
        return super.dependsOn(*paths)
    }

    @TaskAction
    fun publish() {
        val git = Git.open(project.rootDir)
    }
}
