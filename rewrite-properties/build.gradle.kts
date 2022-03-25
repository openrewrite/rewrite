dependencies {
    api(project(":rewrite-core"))
    api("org.jetbrains:annotations:latest.release")
    api("com.fasterxml.jackson.core:jackson-annotations:2.13.2")

    implementation("io.micrometer:micrometer-core:latest.release")

    testImplementation(project(":rewrite-test"))
}
