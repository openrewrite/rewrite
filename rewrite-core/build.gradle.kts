plugins {
    id("org.openrewrite.build.language-library")
}

dependencies {
    api("org.openrewrite.tools:jgit:latest.release")
    implementation("org.openrewrite.tools:java-object-diff:latest.release")
    implementation("io.quarkus.gizmo:gizmo:1.0.+")
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-smile")
    api("com.fasterxml.jackson.module:jackson-module-parameter-names")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("net.java.dev.jna:jna-platform:latest.release")

    // Pinning okhttp while waiting on 5.0.0
    // https://github.com/openrewrite/rewrite/issues/1479
    compileOnly("com.squareup.okhttp3:okhttp:4.9.3")

    implementation("org.apache.commons:commons-compress:latest.release")

    implementation("io.micrometer:micrometer-core:1.9.+")
    implementation("io.github.classgraph:classgraph:latest.release")
    implementation("org.yaml:snakeyaml:latest.release")

    testImplementation(project(":rewrite-test"))
}
