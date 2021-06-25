import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val compiler = javaToolchains.compilerFor {
    languageVersion.set(JavaLanguageVersion.of(8))
}

val maybeExe = if(getCurrentOperatingSystem().isWindows) {
    ".exe"
} else {
    ""
}
val javac = compiler.get().metadata.installationPath.file("bin/javac${maybeExe}")
val tools = compiler.get().metadata.installationPath.file("lib/tools.jar")

dependencies {
    compileOnly(files(tools))
    compileOnly("org.slf4j:slf4j-api:1.7.+")

    implementation(project(":rewrite-java"))
    implementation("org.ow2.asm:asm:latest.release")

    implementation("io.micrometer:micrometer-core:latest.release")

    testImplementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    testImplementation("org.junit.jupiter:junit-jupiter-api:latest.release")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:latest.release")

    testImplementation(project(":rewrite-test"))
    testRuntimeOnly("ch.qos.logback:logback-classic:1.0.13")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jdkHome = compiler.get().metadata.installationPath.asFile.absolutePath
        jvmTarget = "1.8"
    }
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_1_8.toString()
    targetCompatibility = JavaVersion.VERSION_1_8.toString()
    options.isFork = true
    options.forkOptions.executable = javac.toString()
    options.compilerArgs.clear() // remove `--release 8` set in root gradle build
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
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
