plugins {
    id("org.openrewrite.build.language-library")
}

val antlrGeneration by configurations.creating {
    extendsFrom(configurations.implementation.get())
}

tasks.register<JavaExec>("generateAntlrSources") {
    mainClass.set("org.antlr.v4.Tool")

    args = listOf(
        "-o", "src/main/java/org/openrewrite/toml/internal/grammar",
        "-package", "org.openrewrite.toml.internal.grammar",
        "-visitor"
    ) + fileTree("src/main/antlr").matching { include("**/*.g4") }.map { it.path }

    classpath = antlrGeneration
}

dependencies {
    implementation(project(":rewrite-core"))
    implementation("org.antlr:antlr4-runtime:4.11.1")
    implementation("io.micrometer:micrometer-core:1.9.+")

    antlrGeneration("org.antlr:antlr4:4.11.1")

    compileOnly(project(":rewrite-test"))

    testImplementation(project(":rewrite-test"))
}
