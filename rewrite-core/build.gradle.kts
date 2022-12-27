import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("org.openrewrite.build.language-library")
    id("org.openrewrite.build.shadow")
}

dependencies {
    compileOnly("org.eclipse.jgit:org.eclipse.jgit:5.13.+")

    implementation("de.danielbechler:java-object-diff:latest.release")
    implementation("org.apache.ant:ant:latest.release")

    implementation("io.quarkus.gizmo:gizmo:1.0.+")

    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-smile")
    api("com.fasterxml.jackson.module:jackson-module-parameter-names")
    implementation("net.java.dev.jna:jna-platform:latest.release")

    // Pinning okhttp while waiting on 5.0.0
    // https://github.com/openrewrite/rewrite/issues/1479
    compileOnly("com.squareup.okhttp3:okhttp:4.9.3")

    implementation("org.apache.commons:commons-compress:latest.release")

    implementation("io.micrometer:micrometer-core:1.9.+")
    implementation("io.github.classgraph:classgraph:latest.release")
    implementation("org.yaml:snakeyaml:latest.release")

    testImplementation(project(":rewrite-test"))
    testImplementation("org.eclipse.jgit:org.eclipse.jgit:5.13.+")
}

tasks.withType<ShadowJar> {
    dependencies {
        include(dependency("org.eclipse.jgit:"))
    }
    relocate("org.eclipse.jgit", "org.openrewrite.shaded.jgit")
    metaInf {
        from("$rootDir/LICENSE")
        from("$rootDir/NOTICE")
    }
}

tasks.named<Test>("test").configure {
    classpath = files(tasks.named<ShadowJar>("shadowJar"), sourceSets.test.get().output, configurations.testRuntimeClasspath)
}
