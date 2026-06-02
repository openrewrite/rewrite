plugins {
    id("org.openrewrite.build.language-library")
}

tasks.withType<Javadoc>().configureEach {
    exclude("**/AddProperty**")
}

dependencies {
    api(project(":rewrite-core"))
    api("org.jetbrains:annotations:latest.release")
    api("com.fasterxml.jackson.core:jackson-annotations")

    compileOnly(project(":rewrite-test"))

    implementation("io.micrometer:micrometer-core:1.9.+")

    testImplementation(project(":rewrite-test"))
}
