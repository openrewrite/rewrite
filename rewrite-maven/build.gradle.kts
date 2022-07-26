import nl.javadude.gradle.plugins.license.LicenseExtension

plugins {
    id("org.openrewrite.java-library")
    id("org.openrewrite.maven-publish")
}

apply(plugin = "nebula.integtest-standalone")

val integTestImplementation = configurations.getByName("integTestImplementation")

dependencies {
    api(project(":rewrite-xml"))
    api("org.jetbrains:annotations:latest.release")

    api("com.fasterxml.jackson.core:jackson-annotations:latest.release")

    // Caffeine 2.x works with Java 8, Caffeine 3.x is Java 11 only.
    implementation("com.github.ben-manes.caffeine:caffeine:2.+")

    implementation("org.antlr:antlr4:4.9.+")
    // FIXME: switch to `latest.release`
    // when https://github.com/resilience4j/resilience4j/issues/1472 is resolved
    implementation("io.github.resilience4j:resilience4j-retry:1.7.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:latest.release")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-smile:latest.release")
    implementation("com.fasterxml.jackson.module:jackson-module-jaxb-annotations:latest.release")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:latest.release")

    implementation("org.slf4j:slf4j-api:1.7.+")

    // needed by AddDependency
    implementation(project(":rewrite-java"))

    implementation("guru.nidi:graphviz-java:latest.release")

    compileOnly("org.rocksdb:rocksdbjni:latest.release")
    compileOnly(project(":rewrite-yaml"))
    compileOnly(project(":rewrite-properties"))

    implementation("io.micrometer:micrometer-core:1.9.+")

    implementation("org.apache.commons:commons-text:latest.release")

    integTestImplementation("org.eclipse.aether:aether-api:latest.release")
    integTestImplementation("org.eclipse.aether:aether-spi:latest.release")
    integTestImplementation("org.eclipse.aether:aether-util:latest.release")
    integTestImplementation("org.eclipse.aether:aether-connector-basic:latest.release")
    integTestImplementation("org.eclipse.aether:aether-transport-file:latest.release")
    integTestImplementation("org.eclipse.aether:aether-transport-http:latest.release")
    integTestImplementation("org.apache.maven:maven-aether-provider:latest.release")
    integTestImplementation("org.apache.maven:maven-core:latest.release")
    integTestImplementation("io.micrometer:micrometer-registry-prometheus:1.+")
    integTestImplementation("org.rocksdb:rocksdbjni:latest.release")

    integTestImplementation(project(":rewrite-java-11"))
    integTestImplementation(project(":rewrite-properties"))
    integTestImplementation(project(":rewrite-xml"))
    integTestImplementation(project(":rewrite-yaml"))

    testImplementation(project(":rewrite-test"))
    testImplementation("ch.qos.logback:logback-classic:1.2.11")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.+")
    testImplementation("org.mapdb:mapdb:latest.release")

    testRuntimeOnly("org.mapdb:mapdb:latest.release")
    testRuntimeOnly(project(":rewrite-java-11"))
    testRuntimeOnly("org.rocksdb:rocksdbjni:latest.release")
    testRuntimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin:latest.release")
}

tasks.register<JavaExec>("generateAntlrSources") {
    main = "org.antlr.v4.Tool"

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
