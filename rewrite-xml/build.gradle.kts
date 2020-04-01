// run manually with -x compileKotlin when you need to regenerate
tasks.register<JavaExec>("generateAntlrSources") {
    main = "org.antlr.v4.Tool"

    args = listOf(
            "-o", "src/main/java/org/openrewrite/xml/internal/grammar",
            "-package", "org.openrewrite.xml.internal.grammar",
            "-visitor"
    ) + fileTree("src/main/antlr").matching { include("**/*.g4") }.map { it.path }

    classpath = sourceSets["main"].runtimeClasspath
}

dependencies {
    api(project(":rewrite-core"))

    implementation("org.antlr:antlr4:4.8-1")

    api("com.fasterxml.jackson.core:jackson-annotations:latest.release")
    implementation("com.fasterxml.jackson.core:jackson-databind:latest.release")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-smile:latest.release")
}
