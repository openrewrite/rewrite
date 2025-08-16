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

    antlrGeneration("org.antlr:antlr4:4.13.2"){
        exclude(group = "com.ibm.icu", module = "icu4j")
    }
    implementation("org.antlr:antlr4-runtime:4.13.2")
    implementation("io.micrometer:micrometer-core:1.9.+")
    implementation("org.apache.commons:commons-text:1.11.+")

    testImplementation(project(":rewrite-test"))
}

//Javadoc compiler will complain about the use of the internal types.
tasks.withType<Javadoc>().configureEach {
    exclude(
        "**/Xml**"
    )
}
