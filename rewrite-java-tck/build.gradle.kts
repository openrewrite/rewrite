plugins {
    id("org.openrewrite.build.language-library")
    id("org.openrewrite.build.java8-text-blocks")
    // Prevent cache misses due to metadata differences inside the JAR
    id("org.gradlex.reproducible-builds") version "1.0"
}

dependencies {
    // TCK classes target Java 8 bytecode and are loaded by the rewrite-java-8 test launcher.
    // JUnit 6 is compiled with `--release 17`, so we pin to JUnit 5 here to stay loadable on Java 8.
    implementation(enforcedPlatform("org.junit:junit-bom:5.14.0"))
    implementation("org.assertj:assertj-core:latest.release")
    implementation(project(":rewrite-java"))
    implementation(project(":rewrite-java-test"))
    implementation(project(":rewrite-test"))

    if (System.getProperty("idea.active") != null ||
            System.getProperty("idea.sync.active") != null) {
        // so we can run tests in the IDE with the IntelliJ IDEA runner
        runtimeOnly(project(":rewrite-java-21"))
    }
}

configurations.all {
    resolutionStrategy {
        eachDependency {
            if (requested.group == "org.assertj" && requested.name == "assertj-core") {
                useVersion("3.+") // Pin to latest 3.+ version as AssertJ 4 requires Java 17
            }
        }
    }
}

infoBroker {
    // Prevent cache misses due to unstable attributes, e.g. "Build-Date"
    includedManifestProperties = listOf(
        "Module-Owner",
        "Module-Email",
    )
}

tasks.withType<Javadoc>().configureEach {
    isFailOnError = false
    exclude("org/openrewrite/java/**")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(null as? Int?) // remove `--release 8` set in `org.openrewrite.java-base`
}

configurations.all {
    if (isCanBeConsumed) {
        attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8)
    }
}
