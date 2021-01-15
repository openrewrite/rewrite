dependencies {
    api(project(":rewrite-core"))

    api("com.fasterxml.jackson.core:jackson-annotations:latest.release")
    implementation("com.fasterxml.jackson.core:jackson-databind:latest.release")

    implementation("org.yaml:snakeyaml:latest.release")

    testImplementation(project(":rewrite-test"))
}
