plugins {
    id("org.openrewrite.build.language-library")
}

// As per: https://docs.gradle.org/current/userguide/java_testing.html#sec:configuring_java_integration_tests
sourceSets {
    create("intTest") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}
val intTestImplementation by configurations.getting {
    extendsFrom(configurations.implementation.get())
}
val intTestRuntimeOnly by configurations.getting
configurations["intTestRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())

dependencies {
    api(project(":rewrite-java"))

    implementation("org.codehaus.groovy:groovy:latest.release")

    compileOnly(project(":rewrite-test"))
    compileOnly("org.slf4j:slf4j-api:1.7.+")

    api("io.micrometer:micrometer-core:1.9.+")

    api("org.jetbrains:annotations:latest.release")

    api("com.fasterxml.jackson.core:jackson-annotations")

    testImplementation(project(":rewrite-test"))
    testImplementation(project(":rewrite-java-test"))
    testImplementation("org.junit-pioneer:junit-pioneer:2.0.0")
    testRuntimeOnly("org.codehaus.groovy:groovy-all:latest.release")
    testRuntimeOnly(project(":rewrite-java-17"))

    intTestImplementation(project(":rewrite-test"))
    intTestImplementation("org.junit.jupiter:junit-jupiter:latest.release")
    intTestImplementation("org.codehaus.groovy:groovy:[2.5,2.6)")
    intTestRuntimeOnly("org.codehaus.groovy:groovy-all:[2.5,2.6)")
    intTestRuntimeOnly(project(":rewrite-java-17"))
    intTestRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// As per: https://docs.gradle.org/current/userguide/java_testing.html#sec:configuring_java_integration_tests
val integrationTest = task<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"

    testClassesDirs = sourceSets["intTest"].output.classesDirs
    classpath = sourceSets["intTest"].runtimeClasspath
    shouldRunAfter("test")

    useJUnitPlatform()

    testLogging {
        events("passed")
    }
}
tasks.check { dependsOn(integrationTest) }
