import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow")
    id("nebula.maven-shadow-publish")
}

dependencies {
    compileOnly("org.eclipse.jgit:org.eclipse.jgit:latest.release")
    testImplementation("org.eclipse.jgit:org.eclipse.jgit:latest.release")

    api("com.fasterxml.jackson.core:jackson-databind:2.12.+")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-smile:2.12.+")
    api("com.fasterxml.jackson.module:jackson-module-parameter-names:2.12.+")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.+")

    implementation("org.graalvm.sdk:graal-sdk:latest.release")
    testImplementation("org.graalvm.sdk:graal-sdk:latest.release")
    implementation("org.apache.commons:commons-io:latest.release")
    implementation("org.apache.commons:commons-compress:latest.release")

    implementation("io.micrometer:micrometer-core:latest.release")
    implementation("io.github.classgraph:classgraph:latest.release")
    implementation("org.yaml:snakeyaml:latest.release")
}

tasks.withType<ShadowJar> {
    configurations = listOf(project.configurations.compileClasspath.get())
    archiveClassifier.set(null as String?)
    dependencies {
        include(dependency("org.eclipse.jgit:"))
    }
    relocate("org.eclipse.jgit", "org.openrewrite.shaded.jgit")
    metaInf {
        from("$rootDir/LICENSE")
        from("$rootDir/NOTICE")
    }
}

tasks.named("jar") {
    enabled = false
    dependsOn(tasks.named("shadowJar"))
}
