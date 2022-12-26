plugins {
    id("org.openrewrite.build.language-library")
}

val compiler = javaToolchains.compilerFor {
    languageVersion.set(JavaLanguageVersion.of(8))
}

val tools = compiler.get().metadata.installationPath.file("lib/tools.jar")

dependencies {
    compileOnly(files(tools))
    compileOnly("org.slf4j:slf4j-api:1.7.+")

    implementation(project(":rewrite-java"))
    implementation("org.ow2.asm:asm:latest.release")

    implementation("io.micrometer:micrometer-core:1.9.+")

    testImplementation("org.junit.jupiter:junit-jupiter-api:latest.release")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:latest.release")

    testImplementation(project(":rewrite-test"))
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

val testTask = tasks.register<Test>("compatibilityTest") {
    description = "Test parser compatibility."
    group = "verification"
    useJUnitPlatform {
        excludeTags("java11", "java17")
    }
    jvmArgs = listOf("-XX:+UnlockDiagnosticVMOptions", "-XX:+ShowHiddenFrames")
    val tckSourceSet = project(":rewrite-java-tck").sourceSets.getByName("main")
    testClassesDirs = tckSourceSet.output.classesDirs
    classpath = tckSourceSet.runtimeClasspath
            .plus(sourceSets.getByName("test").runtimeClasspath)
            .plus(sourceSets.getByName("main").output.classesDirs)
    shouldRunAfter(tasks.test)
}
tasks.test {
    dependsOn(testTask)
}
