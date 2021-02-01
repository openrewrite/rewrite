dependencies {
    api(project(":rewrite-core"))

    api("com.fasterxml.jackson.core:jackson-annotations:latest.release")
    implementation("com.fasterxml.jackson.core:jackson-databind:latest.release")
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names:latest.release")

    testImplementation(project(":rewrite-test"))
}
