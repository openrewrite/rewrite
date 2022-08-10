plugins {
    id("org.openrewrite.java-library")
    id("org.openrewrite.maven-publish")
}

// run manually with -x compileKotlin when you need to regenerate
tasks.register<JavaExec>("generateAntlrSources") {
    main = "org.antlr.v4.Tool"

    args = listOf(
        "-o", "src/main/java/org/openrewrite/cobol/internal/grammar",
        "-package", "org.openrewrite.cobol.internal.grammar",
        "-visitor"
    ) + fileTree("src/main/antlr").matching { include("**/*.g4") }.map { it.path }

    classpath = sourceSets["main"].runtimeClasspath
}

sourceSets {
    create("model") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

val modelImplementation by configurations.getting {
    extendsFrom(configurations.implementation.get())
}

configurations["modelRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())

dependencies {
    api(project(":rewrite-core"))
    api("org.jetbrains:annotations:latest.release")
    api("com.fasterxml.jackson.core:jackson-annotations:latest.release")

    compileOnly(project(":rewrite-test"))

    implementation("org.antlr:antlr4:4.9.+")
    implementation("io.micrometer:micrometer-core:1.+")

    modelImplementation(project(":rewrite-java-11"))
    "modelAnnotationProcessor"("org.projectlombok:lombok:latest.release")
    "modelCompileOnly"("org.projectlombok:lombok:latest.release")
    modelImplementation("ch.qos.logback:logback-classic:latest.release")

    testImplementation(project(":rewrite-test"))
    testImplementation("io.github.classgraph:classgraph:latest.release")
}
