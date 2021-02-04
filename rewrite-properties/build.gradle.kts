dependencies {
    api(project(":rewrite-core"))

    api("com.fasterxml.jackson.core:jackson-annotations:latest.release")

    testImplementation(project(":rewrite-test"))
}
