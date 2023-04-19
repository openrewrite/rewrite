plugins {
    id("org.openrewrite.build.language-library")
}

dependencies {
    api(project(":rewrite-core"))
    api(project(":rewrite-java"))

    implementation("org.eclipse.jdt:org.eclipse.jdt.core:latest.release")
    implementation("org.apache.logging.log4j:log4j-api:latest.release")
    implementation("org.apache.logging.log4j:log4j-iostreams:latest.release")

    testRuntimeOnly("org.apache.logging.log4j:log4j-slf4j-impl:latest.release")

    testImplementation(project(":rewrite-test"))
}
