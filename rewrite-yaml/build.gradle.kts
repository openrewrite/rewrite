dependencies {
    api(project(":rewrite-xml"))

    api("com.fasterxml.jackson.core:jackson-annotations:2.11.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.11.3")

    implementation("org.yaml:snakeyaml:latest.release")

    testImplementation(project(":rewrite-test"))
}
