dependencies {
    implementation("org.eclipse.jgit:org.eclipse.jgit:latest.release")
    implementation("com.fasterxml.jackson.core:jackson-databind:latest.release")

    implementation("com.koloboke:koloboke-api-jdk8:latest.release")
    implementation("com.koloboke:koloboke-impl-jdk8:latest.release")

    implementation("io.github.classgraph:classgraph:latest.release")

    implementation("io.micrometer:micrometer-core:latest.release")

    implementation("org.yaml:snakeyaml:latest.release")

    api("org.eclipse.microprofile.config:microprofile-config-api:latest.release")
    api("org.kohsuke:github-api:latest.release")
    api("com.google.code.findbugs:jsr305:latest.release")

    testRuntimeOnly("org.microbean:microbean-microprofile-config:latest.release")
}