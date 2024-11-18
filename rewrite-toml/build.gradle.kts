plugins {
    id("org.openrewrite.build.language-library")
    id("antlr")
}

tasks.register<JavaExec>("generateAntlrSources") {
    mainClass.set("org.antlr.v4.Tool")

    args = listOf(
        "-o", "src/main/java/org/openrewrite/toml/internal/grammar",
        "-package", "org.openrewrite.toml.internal.grammar",
        "-visitor"
    ) + fileTree("src/main/antlr").matching { include("**/*.g4") }.map { it.path }

    classpath = sourceSets["main"].runtimeClasspath + configurations["antlr"]
}

dependencies {
    antlr("org.antlr:antlr4:4.11.1")

    api(project(":rewrite-core"))
    api("org.jetbrains:annotations:latest.release")
    api("com.fasterxml.jackson.core:jackson-annotations")

    compileOnly(project(":rewrite-test"))

    implementation("org.antlr:antlr4-runtime:4.11.1")
    implementation("io.micrometer:micrometer-core:1.9.+")

    testImplementation(project(":rewrite-test"))
}
