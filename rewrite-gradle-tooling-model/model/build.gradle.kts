plugins {
    id("java-library")
    id("org.openrewrite.build.metadata")
    id("org.openrewrite.build.publish")
    id("org.openrewrite.build.java8-text-blocks")
}

// This project's tests to work correctly a corresponding version of :plugin must be published to maven local
// This is because src/main/resources/init.gradle expects the plugin to be present there
// And a cross-project task dependency won't work if the projects are evaluated independently
// This is all messy and non-idiomatic from Gradle's standpoint, so some better way would be ideal
evaluationDependsOn(":rewrite-gradle-tooling-model:plugin")

// It is intentional that these are declared explicitly
// org.openrewrite.gradle.RewriteDependencyRepositoriesPlugin in rewrite-build-gradle-plugin disallows snapshot repositories during release builds
// Uniquely amongst our repositories, this repository is supposed to be able to release built against rewrite-core snapshots
repositories {
    mavenLocal()
    maven {
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
    }
    mavenCentral()
}

dependencies {
    implementation(gradleApi())

    // NOTE: this is latest.integration because we need to be able to release
    // rewrite-gradle-tooling-model BEFORE rewrite but also need to depend on
    // changes to the ABI of rewrite-maven.
    compileOnly(project(":rewrite-core"))
    compileOnly(project(":rewrite-maven"))

    // These are for org.openrewrite.gradle.toolingapi.Assertions
    compileOnly(project(":rewrite-test"))
    compileOnly(project(":rewrite-gradle"))
    compileOnly(project(":rewrite-groovy"))
    compileOnly(project(":rewrite-kotlin"))
    compileOnly(project(":rewrite-properties"))
    compileOnly(project(":rewrite-toml"))

    compileOnly("org.projectlombok:lombok:latest.release")
    testCompileOnly("org.projectlombok:lombok:latest.release")
    annotationProcessor("org.projectlombok:lombok:latest.release")
    testAnnotationProcessor("org.projectlombok:lombok:latest.release")

    testImplementation(project(":rewrite-test"))
    testImplementation(project(":rewrite-gradle")) {
        exclude(group = "org.openrewrite.gradle.tooling")
    }
    testImplementation(project(":rewrite-core"))
    testImplementation(project(":rewrite-maven"))
    testImplementation(platform("com.fasterxml.jackson:jackson-bom:2.17.+"))
    testImplementation("com.fasterxml.jackson.core:jackson-core")
    testImplementation("com.fasterxml.jackson.core:jackson-databind")
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-smile")
    // last version which supports java 8, which testGradle4 runs on
    testImplementation("org.assertj:assertj-core:3.+")
    testImplementation(platform("org.junit:junit-bom:5.13.3"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.named<JavaCompile>("compileJava").configure {
    options.release.set(8)
    options.compilerArgs.add("-parameters")
    options.encoding = "UTF-8"
}

val testGradle4 = tasks.register<Test>("testGradle4") {
    systemProperty("org.openrewrite.test.gradleVersion", "4.10")
    systemProperty("jarLocationForTest", tasks.named<Jar>("jar").get().archiveFile.get().asFile.absolutePath)
    // Gradle 4 predates support for Java 11
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(8))
    })
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.named("check").configure {
    dependsOn(testGradle4)
}

tasks.withType<Test>().configureEach {
    dependsOn(
        tasks.named("publishToMavenLocal"),
        project.rootProject.childProjects["rewrite-gradle-tooling-model"]!!.childProjects["model"]!!.tasks.named("publishToMavenLocal")
    )
}
