dependencies {
    api("javax.inject:javax.inject:1")

    implementation("org.eclipse.jgit:org.eclipse.jgit:latest.release")
    implementation("com.fasterxml.jackson.core:jackson-databind:latest.release")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-smile:latest.release")

    implementation("io.github.classgraph:classgraph:latest.release")

    implementation("org.yaml:snakeyaml:latest.release")

    api("io.micrometer:micrometer-core:latest.release")
    api("org.kohsuke:github-api:latest.release")
    api("com.google.code.findbugs:jsr305:latest.release")
}
