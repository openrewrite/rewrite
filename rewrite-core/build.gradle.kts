import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("org.openrewrite.java-library")
    id("org.openrewrite.maven-publish")
    id("org.openrewrite.shadow")
}

dependencies {
    implementation("org.projectlombok:lombok:1.18.24")
    compileOnly("org.eclipse.jgit:org.eclipse.jgit:5.13.+")
    testImplementation("org.eclipse.jgit:org.eclipse.jgit:5.13.+")

    implementation("de.danielbechler:java-object-diff:latest.release")

    implementation("net.bytebuddy:byte-buddy:latest.release")

    api("com.fasterxml.jackson.core:jackson-databind:latest.release")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-smile:latest.release")
    api("com.fasterxml.jackson.module:jackson-module-parameter-names:latest.release")
    implementation("net.java.dev.jna:jna-platform:latest.release")

    // Pinning okhttp while waiting on 5.0.0
    // https://github.com/openrewrite/rewrite/issues/1479
    compileOnly("com.squareup.okhttp3:okhttp:4.9.3")

    implementation("org.apache.commons:commons-compress:latest.release")

    implementation("io.micrometer:micrometer-core:1.+")
    implementation("io.github.classgraph:classgraph:latest.release")
    implementation("org.yaml:snakeyaml:latest.release")

    testImplementation(project(":rewrite-test"))

    testRuntimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin:latest.release")
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
