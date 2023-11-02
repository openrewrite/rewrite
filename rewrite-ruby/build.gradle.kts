plugins {
    id("org.openrewrite.build.language-library")
}

dependencies {
    api(project(":rewrite-core"))
    api("com.fasterxml.jackson.core:jackson-annotations")

    compileOnly(project(":rewrite-test"))

    implementation("io.micrometer:micrometer-core:1.9.+")

    testImplementation(project(":rewrite-test"))
}
