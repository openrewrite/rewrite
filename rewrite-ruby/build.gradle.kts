plugins {
    id("org.openrewrite.build.language-library")
    id("org.openrewrite.build.moderne-source-available-license")
}

dependencies {
    // Targeting Java 21 makes Gradle select rewrite-java's shadowed variant, which declares no
    // transitive dependencies, so rewrite-core has to be requested directly (as rewrite-java-21 does).
    api(project(":rewrite-core"))
    api(project(":rewrite-java"))

    // Consumed for its Prism transitive (jruby-prism -> prism-parser-api/prism-parser-wasm), which
    // produces the AST the parser is written against; no JRuby runtime is started. Pinned rather
    // than `latest.release` because it is what fixes the Prism version, and because the JRuby major
    // line sets this module's Java floor, so a silent major bump would move the toolchain.
    implementation("org.jruby:jruby-base:10.1.1.0")

    compileOnly(project(":rewrite-test"))
    compileOnly("org.slf4j:slf4j-api:1.7.+")

    api("org.jetbrains:annotations:latest.release")

    api("com.fasterxml.jackson.core:jackson-annotations")

    testImplementation(project(":rewrite-test"))
    testImplementation(project(":rewrite-java-test"))
    testImplementation("org.assertj:assertj-core:latest.release")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly(project(":rewrite-java-21"))
}

// JRuby 10 ships class file version 65 and cannot be loaded below Java 21, so this module has no
// Java 8 consumer to protect. The toolchain is already 21 by convention; the override that matters
// is dropping the `--release 8` that `org.openrewrite.build.java-base` puts on `compileJava`.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// `RubyCorpusTest` is opt-in and needs these forwarded from the Gradle JVM to the test JVM.
tasks.withType<Test>().configureEach {
    for (property in listOf("ruby.corpus.dir", "ruby.corpus.report", "ruby.corpus.detail")) {
        System.getProperty(property)?.let { systemProperty(property, it) }
    }
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_21.toString()
    targetCompatibility = JavaVersion.VERSION_21.toString()

    options.release.set(null as Int?) // remove `--release 8` set in `org.openrewrite.build.java-base`
}

// `org.openrewrite.build.moderne-source-available-license` only rewrites the published POM and jar
// manifest; the `license*` tasks still stamp the Apache header from `gradle/licenseHeader.txt`
// unless the extension is pointed elsewhere.
configure<nl.javadude.gradle.plugins.license.LicenseExtension> {
    header = file("${rootProject.projectDir}/gradle/msalLicenseHeader.txt")

    // The license plugin maps `.rb` to a `#` header style by default, so any Ruby fixture landing in
    // a source set would be rewritten and break round-trip assertions.
    excludePatterns.addAll(listOf("**/*.rb"))
}

// Lombok's `onConstructor_=@JsonCreator` on the LST model confuses the javadoc tool.
tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
    exclude("**/Rb.java")
}
