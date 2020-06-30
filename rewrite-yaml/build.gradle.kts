dependencies {
    api(project(":rewrite-xml"))

    api("com.fasterxml.jackson.core:jackson-annotations:latest.release")
    implementation("com.fasterxml.jackson.core:jackson-databind:latest.release")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-smile:latest.release")

    implementation("org.yaml:snakeyaml:latest.release")
}
