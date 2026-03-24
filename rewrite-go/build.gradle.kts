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
    workingDir = file("rewrite")
    commandLine("go", "build", "-o", "${layout.buildDirectory.get().asFile}/rewrite-go-rpc", "./cmd/rpc")

    inputs.files(fileTree("rewrite") {
        include("**/*.go")
        include("go.mod")
        include("go.sum")
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
