import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.github.johnrengelman.shadow")
    id("nebula.maven-shadow-publish")
}

dependencies {
    compileOnly("org.eclipse.jgit:org.eclipse.jgit:5.13.+")
    testImplementation("org.eclipse.jgit:org.eclipse.jgit:5.13.+")

    implementation("de.danielbechler:java-object-diff:latest.release")

    api("com.fasterxml.jackson.core:jackson-databind:2.13.2")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-smile:2.13.2")
    api("com.fasterxml.jackson.module:jackson-module-parameter-names:2.13.2")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.2")

    implementation("net.java.dev.jna:jna-platform:latest.release")

    // Pinning okhttp while waiting on 5.0.0
    // https://github.com/openrewrite/rewrite/issues/1479
    compileOnly("com.squareup.okhttp3:okhttp:4.9.3")

    implementation("org.graalvm.sdk:graal-sdk:latest.release")
    testImplementation("org.graalvm.sdk:graal-sdk:latest.release")
    implementation("commons-io:commons-io:latest.release")
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
