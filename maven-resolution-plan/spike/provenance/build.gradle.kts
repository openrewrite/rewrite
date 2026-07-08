plugins {
    java
}

repositories {
    mavenCentral()
}

dependencies {
    // Brings maven-resolver 2.0.x + maven-resolver-provider + maven-model-builder 3.9.x transitively.
    testImplementation("org.apache.maven.resolver:maven-resolver-supplier-mvn3:2.0.20")

    testImplementation(platform("org.junit:junit-bom:5.10.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.13")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
