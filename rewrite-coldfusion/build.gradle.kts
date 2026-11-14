plugins {
    id("org.openrewrite.build.language-library")
    id("org.openrewrite.build.moderne-source-available-license")
}

val antlrGeneration = configurations.create("antlrGeneration") {
    extendsFrom(configurations.implementation.get())
}

tasks.register<JavaExec>("generateAntlrSources") {
    mainClass.set("org.antlr.v4.Tool")

    args = listOf(
        "-o", "src/main/java/org/openrewrite/coldfusion/internal/grammar",
        "-package", "org.openrewrite.coldfusion.internal.grammar",
        "-visitor"
    ) + fileTree("src/main/antlr").matching { include("**/*.g4") }.map { it.path }

    classpath = antlrGeneration

    finalizedBy("licenseFormat")
}

dependencies {
    api(project(":rewrite-core"))
    api(project(":rewrite-java"))

    compileOnly(project(":rewrite-test"))

    api("org.jetbrains:annotations:latest.release")
    implementation("org.antlr:antlr4-runtime:4.13.2")

    antlrGeneration("org.antlr:antlr4:4.13.2") {
        exclude(group = "com.ibm.icu", module = "icu4j")
    }

    testImplementation(project(":rewrite-test"))
    testImplementation("org.assertj:assertj-core:latest.release")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly(project(":rewrite-java-21"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// The opt-in `ColdFusionCorpusTest` and `GrammarProfileTest` need these forwarded from the Gradle JVM
// to the test JVM.
tasks.withType<Test>().configureEach {
    for (property in listOf("coldfusion.corpus.dir", "coldfusion.corpus.report", "coldfusion.corpus.detail",
            "coldfusion.corpus.context", "coldfusion.corpus.limit", "coldfusion.corpus.mappings",
            "coldfusion.profile.report", "coldfusion.profile.limit")) {
        System.getProperty(property)?.let { systemProperty(property, it) }
    }
}

// `org.openrewrite.build.moderne-source-available-license` only rewrites the published POM and jar
// manifest; the `license*` tasks still stamp the Apache header from `gradle/licenseHeader.txt`
// unless the extension is pointed elsewhere.
configure<nl.javadude.gradle.plugins.license.LicenseExtension> {
    header = file("${rootProject.projectDir}/gradle/msalLicenseHeader.txt")
    excludePatterns.addAll(listOf("**/*.cfm", "**/*.cfc", "**/*.cfml"))
}
