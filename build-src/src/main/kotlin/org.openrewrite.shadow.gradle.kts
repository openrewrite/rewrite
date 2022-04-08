import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("org.openrewrite.java-library")
    id("com.github.johnrengelman.shadow")
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.withType<ShadowJar>() {
    configurations = listOf(project.configurations.compileClasspath.get())
    archiveClassifier.set(null as String?)
}
