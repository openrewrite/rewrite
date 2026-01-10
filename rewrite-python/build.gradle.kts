@file:Suppress("UnstableApiUsage")

plugins {
    id("org.openrewrite.build.language-library")
    id("org.openrewrite.build.moderne-source-available-license")
    id("jvm-test-suite")
    id("publishing")
}

dependencies {
    api(project(":rewrite-core"))
    api(project(":rewrite-java"))

    api("org.jetbrains:annotations:latest.release")
    api("com.fasterxml.jackson.core:jackson-annotations")

    implementation("io.moderne:jsonrpc:latest.integration")

    compileOnly(project(":rewrite-test"))

    testImplementation(project(":rewrite-test"))
    testImplementation("io.moderne:jsonrpc:latest.integration")
    testRuntimeOnly(project(":rewrite-java-21"))
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
    exclude("**/Py.java")
}

// Python-specific build tasks will be added here
// Similar to rewrite-javascript's npm tasks

testing {
    suites {
        register<JvmTestSuite>("integTest") {
            useJUnitJupiter()

            dependencies {
                implementation(project())
                implementation(project(":rewrite-java-21"))
                implementation(project(":rewrite-test"))
                implementation("org.assertj:assertj-core:latest.release")
                implementation("org.junit.platform:junit-platform-suite-api")
                runtimeOnly("org.junit.platform:junit-platform-suite-engine")
            }
        }
    }
}

// Note: Python IDE support is configured via the standalone module at:
// .idea/modules/rewrite-python-src/rewrite-python-src.iml
// This is separate from Gradle because IntelliJ's Gradle integration doesn't support Python source roots.
