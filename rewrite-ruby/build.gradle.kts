plugins {
    id("org.openrewrite.build.language-library")
}

dependencies {
    api(project(":rewrite-core"))
    api("com.fasterxml.jackson.core:jackson-annotations")
    api(project(":rewrite-java"))

    compileOnly(project(":rewrite-test"))

    implementation("io.micrometer:micrometer-core:1.9.+")
    implementation("org.jruby:jruby-complete:latest.release")

    testImplementation(project(":rewrite-test"))
}
