dependencies {
    api(project(":rewrite-xml"))

    api("com.fasterxml.jackson.core:jackson-annotations:2.11.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.11.3")

    testImplementation(project(":rewrite-test"))
}
