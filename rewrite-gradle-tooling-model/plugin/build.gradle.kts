plugins {
    id("org.openrewrite.build.language-library")
    id("org.openrewrite.build.java8-text-blocks")
}

group = "org.openrewrite.gradle.tooling"
description = "A model for extracting semantic information out of Gradle build files necessary for refactoring them."

dependencies {
    implementation(project(":rewrite-gradle-tooling-model:model"))
    implementation(project(":rewrite-gradle"))
    implementation(gradleApi())
}

tasks.withType<Test>().configureEach {
    dependsOn(tasks.named("publishToMavenLocal"))
}

tasks.named<JavaCompile>("compileJava").configure {
    options.release.set(8)
}
