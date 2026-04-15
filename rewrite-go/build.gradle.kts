@file:Suppress("UnstableApiUsage")

import com.gradle.develocity.agent.gradle.test.ImportJUnitXmlReports
import com.gradle.develocity.agent.gradle.test.JUnitXmlDialect

plugins {
    id("org.openrewrite.build.language-library")
    id("org.openrewrite.build.moderne-source-available-license")
    id("jvm-test-suite")
}

dependencies {
    api(project(":rewrite-core"))
    api(project(":rewrite-java"))

    api("org.jetbrains:annotations:latest.release")
    api("com.fasterxml.jackson.core:jackson-annotations")

    implementation("io.moderne:jsonrpc:latest.integration")

    compileOnly(project(":rewrite-test"))

    testImplementation(project(":rewrite-test"))
    testImplementation("io.moderne:jsonrpc:latest.integration")
    testRuntimeOnly(project(":rewrite-java-21"))
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
    exclude("**/G.java")
}

val goBuild = tasks.register<Exec>("goBuild") {
    workingDir = projectDir
    // Use relative path to avoid absolute paths in cache key (Exec args are cache inputs)
    commandLine("go", "build", "-o", layout.buildDirectory.file("rewrite-go-rpc").get().asFile.relativeTo(projectDir).path, "./cmd/rpc")

    inputs.files(fileTree(projectDir) {
        include("**/*.go")
        include("go.mod")
        include("go.sum")
        exclude("build/**")
    }).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.file(layout.buildDirectory.file("rewrite-go-rpc"))
}

testing {
    suites {
        register<JvmTestSuite>("integTest") {
            useJUnitJupiter()

            targets {
                all {
                    testTask.configure {
                        dependsOn(goBuild)
                    }
                }
            }

            dependencies {
                implementation(project())
                implementation(project(":rewrite-java-21"))
                implementation(project(":rewrite-test"))
                implementation("org.assertj:assertj-core:latest.release")
            }
        }
    }
}

// ============================================
// Go Test Support Tasks
// ============================================

// Configuration to collect classpath for the Java RPC server
val javaRpcClasspath by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    extendsFrom(configurations.getByName("runtimeClasspath"))
}

dependencies {
    javaRpcClasspath(project(":rewrite-maven"))
}

// Task to generate classpath file for Java RPC server testing from Go
val generateTestClasspath by tasks.registering {
    group = "golang"
    description = "Generate classpath file for Java RPC server (used by Go tests)"

    val outputFile = file("test-classpath.txt")
    outputs.file(outputFile)

    dependsOn(tasks.named("compileJava"))

    doLast {
        val classpath = (
            javaRpcClasspath.files +
            tasks.named("compileJava").get().outputs.files +
            tasks.named("processResources").get().outputs.files
        ).distinctBy { it.absolutePath }
         .joinToString(File.pathSeparator) { it.absolutePath }
        outputFile.writeText(classpath)
        logger.lifecycle("Generated test classpath to ${outputFile.absolutePath}")
    }
}

val junitXmlFile = file("build/test-results/gotest/junit.xml")

val goTest = tasks.register<Exec>("goTest") {
    group = "verification"
    description = "Run Go tests"

    workingDir = projectDir
    commandLine("go", "run", "gotest.tools/gotestsum@latest",
        "--junitfile", junitXmlFile.relativeTo(projectDir).path,
        "--format", "standard-verbose",
        "--", "-count=1", "./test/...")

    dependsOn(generateTestClasspath)

    inputs.files(fileTree(projectDir) {
        include("**/*.go")
        include("go.mod")
        include("go.sum")
        exclude("build/**")
    }).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(file("test-classpath.txt"))
    outputs.file(junitXmlFile)
    outputs.cacheIf { true }
}

ImportJUnitXmlReports.register(tasks, goTest, JUnitXmlDialect.GENERIC)

tasks.named("check") {
    dependsOn(goTest)
}
