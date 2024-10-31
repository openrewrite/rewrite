@file:Suppress("UnstableApiUsage")

plugins {
    id("org.openrewrite.build.language-library")
}

// Create the lombok configuration and ensure it extends compileOnly
val lombok = configurations.create("lombok")
configurations.named("compileOnly").configure {
    extendsFrom(lombok)
}

val unpackedAndRenamedLombokDir = layout.buildDirectory.dir("lombok").get().asFile

// Lombok hides its internal classes with a ".SCL.lombok" extension, so we have to undo that to compile with them
tasks.register("unpackAndRenameLombok") {
    inputs.files(configurations.getByName("lombok"))
    outputs.dir(unpackedAndRenamedLombokDir)
    doFirst {
        mkdir(unpackedAndRenamedLombokDir)
        unpackedAndRenamedLombokDir.listFiles()?.forEach { it.delete() }
    }
    doLast {
        copy {
            from(zipTree(configurations.getByName("lombok").singleFile))
            into(unpackedAndRenamedLombokDir)
            include("lombok/**/*")
        }
        copy {
            from(zipTree(configurations.getByName("lombok").singleFile))
            into(unpackedAndRenamedLombokDir)
            include("SCL.lombok/**/*")
            eachFile {
                // Drop the first segment and rename the file
                val newSegments = relativePath.segments.drop(1).toTypedArray()
                if (newSegments.isNotEmpty() && newSegments.last().endsWith(".SCL.lombok")) {
                    newSegments[newSegments.size - 1] = newSegments.last().replace(".SCL.lombok", ".class")
                }
                relativePath = RelativePath(true, *newSegments)
            }
        }
    }
}

tasks.named("compileJava") {
    dependsOn("unpackAndRenameLombok")
}

val compiler = javaToolchains.compilerFor {
    languageVersion.set(JavaLanguageVersion.of(8))
}

val tools = compiler.get().metadata.installationPath.file("lib/tools.jar")

dependencies {
    implementation("org.jspecify:jspecify:latest.release")
    runtimeOnly("org.projectlombok:lombok:latest.release")

    // Add lombok dependency to the newly created lombok configuration
    lombok("org.projectlombok:lombok:latest.release")
    compileOnly(files(tools))
    compileOnly(files(unpackedAndRenamedLombokDir))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.isFork = true
    options.release.set(null as? Int?) // remove `--release 8` set in `org.openrewrite.java-base`
}

tasks.withType<Test>().configureEach {
    jvmArgs = listOf("-XX:+UnlockDiagnosticVMOptions", "-XX:+ShowHiddenFrames")
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(8))
    })
}

tasks.withType<Javadoc>().configureEach {
    executable = javaToolchains.javadocToolFor {
        languageVersion.set(JavaLanguageVersion.of(8))
    }.get().executablePath.toString()
}
