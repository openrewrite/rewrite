dependencies {
    api(project(":rewrite-core"))

    api("com.fasterxml.jackson.core:jackson-annotations:latest.release")

    implementation("org.yaml:snakeyaml:latest.release")

    testImplementation(project(":rewrite-test"))
}
