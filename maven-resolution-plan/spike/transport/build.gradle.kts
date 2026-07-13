plugins {
    java
}

repositories {
    mavenCentral()
}

java {
    // Prove the whole spike compiles at the Java 8 floor (claim 1).
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
}

dependencies {
    // Single dependency that transitively pulls the entire no-DI resolver 2.x stack
    // (resolver api/util/spi/impl/connector-basic/transport-file/transport-apache/named-locks)
    // + Maven 3.9 provider + model-builder. This is "Stack A" from a7-embeddability.md.
    implementation("org.apache.maven.resolver:maven-resolver-supplier-mvn3:2.0.20")

    // OpenRewrite HttpSender abstraction that remote traffic is routed through (claim 2).
    implementation("org.openrewrite:rewrite-core:latest.release")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // Keep the resolver quiet; test-only so it stays off the scanned runtime classpath (claim 1b).
    testRuntimeOnly("org.slf4j:slf4j-nop:2.0.16")
}

tasks.test {
    useJUnitPlatform()
    // Hand the production (main) runtime classpath to the bytecode-version scan (claim 1b).
    systemProperty("spike.runtimeClasspath", configurations.runtimeClasspath.get().asPath)
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = true
    }
}
