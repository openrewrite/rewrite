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
        compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
        runtimeClasspath += sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath
    }
}

dependencies {
    api(project(":rewrite-core"))
    api("org.jetbrains:annotations:latest.release")
    api("com.fasterxml.jackson.core:jackson-annotations:latest.release")

    implementation("org.antlr:antlr4:4.9.+")
    implementation("io.micrometer:micrometer-core:1.+")

    testImplementation(project(":rewrite-test"))
}
