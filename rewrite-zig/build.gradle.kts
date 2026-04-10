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
    exclude("**/Z.java")
}

val zigBuild = tasks.register<Exec>("zigBuild") {
    workingDir = file("rewrite")

    // Use zig build-exe directly since `zig build` has linker issues on
    // macOS Tahoe (26.x) with Zig 0.15.x where the build runner itself
    // fails to link. Specifying an explicit macOS target range works around this.
    val dst = layout.buildDirectory.file("rewrite-zig-rpc").get().asFile
    val isMacOS = System.getProperty("os.name").lowercase().contains("mac")
    val isArm = System.getProperty("os.arch") == "aarch64"
    if (isMacOS && isArm) {
        commandLine(
            "zig", "build-exe",
            "src/main.zig",
            "-target", "aarch64-macos.15.0...26.4",
            "-lc",
            "-O", "ReleaseSafe",
            "--name", "rewrite-zig-rpc"
        )
    } else if (isMacOS) {
        commandLine(
            "zig", "build-exe",
            "src/main.zig",
            "-target", "x86_64-macos.13.0...26.4",
            "-lc",
            "-O", "ReleaseSafe",
            "--name", "rewrite-zig-rpc"
        )
    } else {
        commandLine(
            "zig", "build-exe",
            "src/main.zig",
            "-lc",
            "-O", "ReleaseSafe",
            "--name", "rewrite-zig-rpc"
        )
    }

    inputs.files(fileTree("rewrite") {
        include("**/*.zig")
        include("build.zig")
        include("build.zig.zon")
    }).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.file(layout.buildDirectory.file("rewrite-zig-rpc"))

    doLast {
        val src = file("rewrite/rewrite-zig-rpc")
        dst.parentFile.mkdirs()
        src.copyTo(dst, overwrite = true)
        dst.setExecutable(true)
        // Clean up the output in the working directory
        src.delete()
    }
}

val zigTest = tasks.register<Exec>("zigTest") {
    group = "verification"
    description = "Run Zig native tests"

    workingDir = file("rewrite")

    // Use zig build-obj -ftest to run tests, similar to zigBuild workaround
    // for macOS linker issues with zig 0.15.x
    val isMacOS = System.getProperty("os.name").lowercase().contains("mac")
    val isArm = System.getProperty("os.arch") == "aarch64"
    if (isMacOS && isArm) {
        commandLine(
            "zig", "test",
            "src/main.zig",
            "-target", "aarch64-macos.15.0...26.4",
            "-lc"
        )
    } else if (isMacOS) {
        commandLine(
            "zig", "test",
            "src/main.zig",
            "-target", "x86_64-macos.13.0...26.4",
            "-lc"
        )
    } else {
        commandLine("zig", "test", "src/main.zig", "-lc")
    }

    inputs.files(fileTree("rewrite") {
        include("**/*.zig")
    }).withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.cacheIf { true }
}

tasks.named("check") {
    dependsOn(zigTest)
}

testing {
    suites {
        register<JvmTestSuite>("integTest") {
            useJUnitJupiter()

            targets {
                all {
                    testTask.configure {
                        dependsOn(zigBuild)
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
