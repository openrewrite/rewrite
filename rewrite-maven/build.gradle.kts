import nl.javadude.gradle.plugins.license.LicenseExtension

plugins {
    id("org.openrewrite.build.language-library")
}

dependencies {
    api(project(":rewrite-xml"))
    api("org.jetbrains:annotations:latest.release")

    api("com.fasterxml.jackson.core:jackson-annotations")

    implementation(project(":rewrite-core"))
    compileOnly(project(":rewrite-test"))

    // Caffeine 2.x works with Java 8, Caffeine 3.x is Java 11 only.
    implementation("com.github.ben-manes.caffeine:caffeine:2.+")

    implementation("org.antlr:antlr4-runtime:4.11.1")
    implementation("dev.failsafe:failsafe:latest.release")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-smile")
    implementation("com.fasterxml.jackson.module:jackson-module-jaxb-annotations")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")

    // needed by AddDependency
    implementation(project(":rewrite-java"))

    compileOnly("guru.nidi:graphviz-java:latest.release")

    compileOnly("org.rocksdb:rocksdbjni:latest.release")
    compileOnly(project(":rewrite-yaml"))
    implementation(project(":rewrite-properties"))

    implementation("io.micrometer:micrometer-core:1.9.+")

    implementation("org.apache.commons:commons-text:latest.release")

    testImplementation(project(":rewrite-test"))
    testImplementation("com.squareup.okhttp3:mockwebserver:4.+")
    testImplementation("com.squareup.okio:okio-jvm:3.0.0")
    testImplementation("org.mapdb:mapdb:latest.release")
    testImplementation("guru.nidi:graphviz-java:latest.release")

    testRuntimeOnly("org.mapdb:mapdb:latest.release")
    testRuntimeOnly(project(":rewrite-java-17"))
    testRuntimeOnly("org.rocksdb:rocksdbjni:latest.release")
}

tasks.register<JavaExec>("generateAntlrSources") {
    mainClass.set("org.antlr.v4.Tool")

    args = listOf(
            "-o", "src/main/java/org/openrewrite/maven/internal/grammar",
            "-package", "org.openrewrite.maven.internal.grammar",
            "-visitor"
    ) + fileTree("src/main/antlr").matching { include("**/*.g4") }.map { it.path }

    classpath = sourceSets["main"].runtimeClasspath
}

tasks.withType<Javadoc> {
    // generated ANTLR sources violate doclint
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")

    // TODO
    // openrewrite/rewrite/rewrite-maven/src/main/java/org/openrewrite/maven/AddDependency.java:29: error: cannot find symbol
    // @AllArgsConstructor(onConstructor_=@JsonCreator)
    //                     ^
    //   symbol:   method onConstructor_()
    //   location: @interface AllArgsConstructor
    // 1 error
    exclude("**/VersionRangeParser**", "**/AddDependency**")
}

configure<LicenseExtension> {
    excludePatterns.add("**/unresolvable.txt")
}
