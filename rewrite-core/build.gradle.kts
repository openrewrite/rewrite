dependencies {
    api("javax.inject:javax.inject:1")

    implementation("org.eclipse.jgit:org.eclipse.jgit:latest.release")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.11.3")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-smile:2.11.3")

    implementation("com.koloboke:koloboke-api-jdk8:latest.release")
    implementation("com.koloboke:koloboke-impl-jdk8:latest.release")

    implementation("io.github.classgraph:classgraph:latest.release")

    implementation("org.yaml:snakeyaml:latest.release")

    api("io.micrometer:micrometer-core:latest.release")
    api("org.kohsuke:github-api:latest.release")
    api("com.google.code.findbugs:jsr305:latest.release")
}
