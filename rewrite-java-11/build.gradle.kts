import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem

plugins {
    id("nebula.integtest") version "7.0.9" apply false
}

apply(plugin = "nebula.integtest-standalone")

val integTestImplementation = configurations.getByName("integTestImplementation")

dependencies {
    api(project(":rewrite-core"))
    api(project(":rewrite-java"))

    compileOnly("org.slf4j:slf4j-api:1.7.+")

    implementation("io.micrometer:micrometer-core:latest.release")
    implementation("io.github.classgraph:classgraph:latest.release")
    implementation("org.ow2.asm:asm:latest.release")

    testImplementation(project(":rewrite-test"))

    integTestImplementation("io.micrometer:micrometer-registry-prometheus:latest.release")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

val compiler = javaToolchains.compilerFor {
    languageVersion.set(JavaLanguageVersion.of(11))
}

val maybeExe = if(getCurrentOperatingSystem().isWindows) {
    ".exe"
} else {
    ""
}
val javac = compiler.get().metadata.installationPath.file("bin/javac${maybeExe}")
tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile>().configureEach {
    kotlinOptions.jdkHome = compiler.get().metadata.installationPath.asFile.absolutePath
}
tasks.named<JavaCompile>("compileJava") {
    sourceCompatibility = JavaVersion.VERSION_11.toString()
    targetCompatibility = JavaVersion.VERSION_11.toString()

    options.compilerArgs.clear() // remove `--release 8` set in root gradle build
    options.compilerArgs.addAll(listOf(
            "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED"
    ))
}
tasks.withType<JavaCompile>().configureEach {
    options.isFork = true
    options.forkOptions.executable = javac.toString()
}
tasks.withType<Test>().configureEach {
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(11))
    })
}

tasks.withType<Javadoc> {
    exclude("**/Java11Parser**")
}
