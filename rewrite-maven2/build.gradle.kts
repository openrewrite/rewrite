plugins {
    id("nebula.integtest") version "7.0.9" apply false
}

apply(plugin = "nebula.integtest-standalone")

val integTestImplementation = configurations.getByName("integTestImplementation")

dependencies {
    api(project(":rewrite-xml"))

    api("com.fasterxml.jackson.core:jackson-annotations:latest.release")

    implementation("org.antlr:antlr4:4.8-1")

    implementation("com.fasterxml.jackson.core:jackson-databind:latest.release")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:latest.release")

    implementation("com.koloboke:koloboke-api-jdk8:latest.release")
    implementation("com.koloboke:koloboke-impl-jdk8:latest.release")

    implementation("org.slf4j:slf4j-api:1.7.+")

    compileOnly("org.mapdb:mapdb:latest.release")

    implementation("io.micrometer:micrometer-core:latest.release")

    implementation("com.squareup.okhttp3:okhttp:latest.release")

    implementation("org.apache.commons:commons-text:latest.release")

    integTestImplementation("org.eclipse.aether:aether-api:latest.release")
    integTestImplementation("org.eclipse.aether:aether-spi:latest.release")
    integTestImplementation("org.eclipse.aether:aether-util:latest.release")
    integTestImplementation("org.eclipse.aether:aether-connector-basic:latest.release")
    integTestImplementation("org.eclipse.aether:aether-transport-file:latest.release")
    integTestImplementation("org.eclipse.aether:aether-transport-http:latest.release")
    integTestImplementation("org.apache.maven:maven-aether-provider:latest.release")
    integTestImplementation("org.apache.maven:maven-core:latest.release")

    integTestImplementation("io.micrometer:micrometer-registry-prometheus:latest.release")

    testImplementation("ch.qos.logback:logback-classic:1.0.13")

    testImplementation(project(":rewrite-test"))

    testRuntimeOnly("org.mapdb:mapdb:latest.release")
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

    exclude("**/VersionRangeParser**")
}
