dependencies {
    api(project(":rewrite-core"))
    api("org.jetbrains:annotations:latest.release")
    api("com.fasterxml.jackson.core:jackson-annotations:2.12.+")

    implementation("org.yaml:snakeyaml:latest.release")
    implementation("io.micrometer:micrometer-core:latest.release")

    testImplementation(project(":rewrite-test"))
}
