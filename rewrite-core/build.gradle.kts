plugins {
    id("org.openrewrite.build.language-library")
}

dependencies {
    api("org.openrewrite.tools:jgit:latest.release")
    implementation("org.openrewrite.tools:java-object-diff:latest.release")
    implementation("io.quarkus.gizmo:gizmo:1.0.+")
    api("com.fasterxml.jackson.core:jackson-core")
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-smile")
    api("com.fasterxml.jackson.module:jackson-module-parameter-names")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("net.java.dev.jna:jna-platform:latest.release")

    api("org.jspecify:jspecify:latest.release")

    implementation("org.apache.commons:commons-lang3:latest.release")

    implementation("io.micrometer:micrometer-core:1.9.+")
    implementation("io.github.classgraph:classgraph:latest.release")
    implementation("org.yaml:snakeyaml:latest.release")

    implementation("io.moderne:jsonrpc:latest.integration")
    implementation("org.objenesis:objenesis:latest.release")

    testImplementation("org.assertj:assertj-core:latest.release")
    testImplementation(project(":rewrite-test"))
}
