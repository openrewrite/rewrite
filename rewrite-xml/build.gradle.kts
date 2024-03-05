plugins {
    id("org.openrewrite.build.language-library")
}

val antlrGeneration by configurations.creating {
    extendsFrom(configurations.implementation.get())
}

tasks.register<JavaExec>("generateAntlrSources") {
    mainClass.set("org.antlr.v4.Tool")

    args = listOf(
            "-o", "src/main/java/org/openrewrite/xml/internal/grammar",
            "-package", "org.openrewrite.xml.internal.grammar",
            "-visitor"
    ) + fileTree("src/main/antlr").matching { include("**/*.g4") }.map { it.path }

    classpath = antlrGeneration
}

dependencies {
    api(project(":rewrite-core"))
    api("org.jetbrains:annotations:latest.release")
    api("com.fasterxml.jackson.core:jackson-annotations")

    compileOnly(project(":rewrite-test"))

    antlrGeneration("org.antlr:antlr4:4.11.1")
    implementation("org.antlr:antlr4-runtime:4.11.1")
    implementation("io.micrometer:micrometer-core:1.9.+")
    implementation("org.apache.commons:commons-text:1.11.+")

    testImplementation(project(":rewrite-test"))
    testImplementation(project(":rewrite-maven"))
}
