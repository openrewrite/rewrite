plugins {
    id("org.openrewrite.build.language-library")
}

val antlrGeneration by configurations.creating {
    extendsFrom(configurations.implementation.get())
}

tasks.register<JavaExec>("generateAntlrSources") {
    mainClass.set("org.antlr.v4.Tool")

    args = listOf(
        "-o", "src/main/java/org/openrewrite/bash/internal/grammar",
        "-package", "org.openrewrite.bash.internal.grammar",
        "-visitor"
    ) + fileTree("src/main/antlr").matching { include("**/*.g4") }.map { it.path }

    classpath = antlrGeneration

    finalizedBy("licenseFormat")
}

dependencies {
    implementation(project(":rewrite-core"))
    implementation("org.antlr:antlr4-runtime:4.13.2")
    implementation("io.micrometer:micrometer-core:1.9.+")

    antlrGeneration("org.antlr:antlr4:4.13.2"){
        exclude(group = "com.ibm.icu", module = "icu4j")
    }

    compileOnly(project(":rewrite-test"))

    testImplementation(project(":rewrite-test"))
}
