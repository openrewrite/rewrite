dependencies {
    api(project(":rewrite-core"))
    api("org.jetbrains:annotations:latest.release")

    api("com.fasterxml.jackson.core:jackson-annotations:latest.release")

    testImplementation(project(":rewrite-test"))
}
